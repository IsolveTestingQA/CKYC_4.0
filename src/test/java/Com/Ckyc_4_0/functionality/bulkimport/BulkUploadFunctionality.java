/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.bulkimport.BulkImportPages;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;
import Com.Ckyc_4_0.utils.bulkimport.CsvTable;

/**
 * Bulk Data Import: the upload pop-up (drag and drop), the upload itself, the retry loop (Retry / another file / done by
 * hand / Cancel until it succeeds), and reading the job - SUCCESS rows are the customer ids the rest of the flow expects,
 * FAILED rows are kept and raised as bugs with their remarks.
 */
public final class BulkUploadFunctionality {

	private static final Logger logger = LoggerFactory.getLogger(BulkUploadFunctionality.class);
	private static final String SC = "Bulk Import | ";
	private static final Pattern JOB = Pattern.compile("Job\\s*#\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern FILE_NAME = Pattern.compile("CKYC_DATA_(IN|LE)_(\\d{8})_(\\w+)\\.csv", Pattern.CASE_INSENSITIVE);

	private record UploadResult(boolean ok, String toasts, int job) {
	}

	private WebDriver d() {
		return BiSupport.driver();
	}

	// ------------------------------------------------------------------ open

	public void openBulkDataImport() {
		BiSupport.openModule(SideNavPage.BULK_DATA_IMPORT, null, "/bulk/upload", "/bulk/upload");
		boolean ok = BiSupport.waitFor(BulkImportPages.NEW_RECORD, 15);
		StepLog.check(SC + "Open", "Open Bulk Data Import from the navigation bar", "Bulk Data Import page with New Record",
				"url=" + BiSupport.safeUrl() + "; newRecordVisible=" + ok, ok, "module=Bulk Data Import");
		if (!ok) {
			throw new AssertionError("Bulk Data Import page did not open (New Record button missing)");
		}
		Hooks.captureMilestone("Bulk Data Import opened");
	}

	// ------------------------------------------------------------------ upload loop

	/** Upload pop-up -> upload -> retry pop-up until it succeeds or the tester cancels. */
	public void uploadWithPopups() {
		BulkImportContext.reset();
		Path file = null;
		String lastReason = "";
		while (true) {
			if (file == null) {
				BiPopups.FileAnswer a = BiPopups.askFile("Upload the CSV for Bulk Data Import",
						"The file name must be CKYC_DATA_<IN|LE>_<DDMMYYYY>_<series>.csv with today's date and an unused series number. "
								+ "IN = Individual (a Minor rides the IN file), LE = Legal Entity.");
				if (!"upload".equals(a.action())) {
					cancel("The tester cancelled in the upload pop-up");
					return;
				}
				file = a.saved();
				warnIfNameLooksWrong(a.name());
			}
			UploadResult r;
			try {
				r = tryUpload(file);
			} catch (RuntimeException e) {
				r = new UploadResult(false, "Automation error while uploading: " + e.getClass().getSimpleName() + " - " + e.getMessage(), 0);
			}
			if (r.ok()) {
				BulkImportContext.fileName = file.getFileName().toString();
				BulkImportContext.csvFile = file;
				BulkImportContext.jobNo = r.job();
				BulkImportContext.uploadToasts = r.toasts();
				captureAbsentColumns(r.toasts());
				loadCsv(file);
				StepLog.check(SC + "Upload", "Upload " + BulkImportContext.fileName + " and Create Record",
						"Toast 'Upload accepted (Job #n)'", "toast='" + r.toasts() + "'; job=" + r.job(), true, "file=" + BulkImportContext.fileName);
				Hooks.captureMilestone("Bulk upload accepted | job " + r.job());
				return;
			}
			lastReason = r.toasts().isBlank() ? "No 'Upload accepted' message appeared." : r.toasts();
			StepLog.check(SC + "Upload attempt", "Upload " + file.getFileName() + " and Create Record", "Toast 'Upload accepted (Job #n)'",
					"FAIL | " + lastReason, false, "file=" + file.getFileName());
			Hooks.captureMilestone("Bulk upload failed | " + lastReason, false);
			String choice = BiPopups.askRetry(lastReason, file.getFileName().toString());
			switch (choice) {
			case "retry" -> {
				// same file again
			}
			case "choose" -> file = null;
			case "manual" -> {
				manualUpload(file);
				return;
			}
			default -> {
				cancel("The tester cancelled after a failed upload: " + lastReason);
				return;
			}
			}
		}
	}

	private void cancel(String why) {
		BulkImportContext.cancelled = true;
		SoftAssertManager.markSkipScenario(why);
		StepLog.check(SC + "Upload", "Upload the CSV", "A file is uploaded", "SKIP | " + why, true, "cancelled=true");
		logger.warn(why);
	}

	private void warnIfNameLooksWrong(String name) {
		Matcher m = FILE_NAME.matcher(name);
		if (!m.matches()) {
			StepLog.check(SC + "File name", "File name follows CKYC_DATA_<IN|LE>_<DDMMYYYY>_<series>.csv", "Name matches the rule",
					"WARNING | '" + name + "' does not match - the application may reject it", true, "file=" + name);
			return;
		}
		String today = new java.text.SimpleDateFormat("ddMMyyyy").format(new java.util.Date());
		if (!m.group(2).equals(today)) {
			StepLog.check(SC + "File name", "File date is today's date", "Date segment = " + today,
					"WARNING | date segment is " + m.group(2) + " - the application may reject it", true, "file=" + name);
		}
	}

	private UploadResult tryUpload(Path file) {
		WebDriver d = d();
		openBulkDataImport();
		d.navigate().refresh(); // a clean file queue on every attempt
		BiSupport.waitFor(BulkImportPages.NEW_RECORD, 15);
		BiSupport.sleep(800);
		List<WebElement> nr = d.findElements(BulkImportPages.NEW_RECORD);
		if (!nr.isEmpty() && nr.get(0).isDisplayed()) {
			BiSupport.clickJs(nr.get(0));
			BiSupport.sleep(700);
		}
		if (!BiSupport.waitFor(BulkImportPages.FILE_INPUT, 10)) {
			return new UploadResult(false, "The file input was not found on the page", 0);
		}
		WebElement input = d.findElement(BulkImportPages.FILE_INPUT);
		((JavascriptExecutor) d).executeScript("var e=arguments[0];e.style.display='block';e.style.visibility='visible';e.style.opacity='1';e.style.width='1px';e.style.height='1px';", input);
		input.sendKeys(file.toAbsolutePath().toString());
		BiSupport.sleep(1200);
		List<WebElement> create = d.findElements(BulkImportPages.CREATE_RECORD);
		if (create.isEmpty()) {
			return new UploadResult(false, "Create Record button not found after choosing the file", 0);
		}
		long wait = System.currentTimeMillis() + 10_000;
		while (!create.get(0).isEnabled() && System.currentTimeMillis() < wait) {
			BiSupport.sleep(300);
		}
		if (!create.get(0).isEnabled()) {
			return new UploadResult(false, "Create Record stayed disabled - the file was not accepted into the queue", 0);
		}
		Hooks.captureMilestone("Bulk upload queued before Create Record | " + file.getFileName(), true);
		BiSupport.clickJs(create.get(0));
		Set<String> toasts = new LinkedHashSet<>();
		long end = System.currentTimeMillis() + 25_000;
		long extra = 0;
		int job = 0;
		boolean accepted = false;
		while (System.currentTimeMillis() < end) {
			String t = ToastHandler.readIfPresent(d);
			if (t != null && !t.isBlank()) {
				toasts.add(t.trim());
				String low = t.toLowerCase(Locale.ROOT);
				Matcher m = JOB.matcher(t);
				if (m.find()) {
					job = Integer.parseInt(m.group(1));
				}
				if (low.contains("upload accepted") || low.contains("accepted")) {
					accepted = true;
					if (extra == 0) {
						extra = System.currentTimeMillis() + 4000; // the warning toast about absent columns follows
					}
				} else if (!accepted && (low.contains("already") || low.contains("fail") || low.contains("invalid")
						|| low.contains("error") || low.contains("reject") || low.contains("duplicate"))) {
					break;
				}
			}
			if (accepted && System.currentTimeMillis() > extra) {
				break;
			}
			BiSupport.sleep(400);
		}
		return new UploadResult(accepted, String.join(" | ", toasts), job);
	}

	private void captureAbsentColumns(String toasts) {
		Matcher m = Pattern.compile("WARNING:[^|]*", Pattern.CASE_INSENSITIVE).matcher(toasts);
		if (m.find()) {
			BulkImportContext.absentColumnsWarning = m.group().trim();
			StepLog.check(SC + "Upload warning", "Read the warning toast", "Warning about absent columns is recorded",
					BulkImportContext.absentColumnsWarning, true, "job=" + BulkImportContext.jobNo);
		}
	}

	private void loadCsv(Path file) {
		try {
			BulkImportContext.csv = CsvTable.read(file);
			Matcher m = FILE_NAME.matcher(file.getFileName().toString());
			BulkImportContext.persona = m.matches() && m.group(1).equalsIgnoreCase("LE") ? "LE" : "IND";
			StepLog.check(SC + "CSV read", "Read the uploaded CSV with its own header row", "Headers and rows read dynamically",
					"columns=" + BulkImportContext.csv.headers().size() + "; rows=" + BulkImportContext.csv.rows().size()
							+ "; delimiter=" + BulkImportContext.csv.delimiter() + "; persona=" + BulkImportContext.persona, true,
					"file=" + file.getFileName());
		} catch (IOException e) {
			throw new IllegalStateException("The uploaded CSV could not be read: " + e.getMessage(), e);
		}
	}

	/** The tester uploaded and submitted in the application by hand: use the newest job. */
	private void manualUpload(Path file) {
		BulkImportContext.manualUpload = true;
		BulkImportContext.fileName = file == null ? "" : file.getFileName().toString();
		BulkImportContext.csvFile = file;
		if (file != null) {
			loadCsv(file);
		}
		StepLog.check(SC + "Upload", "Tester uploads and submits by hand", "The newest upload is used",
				"manual upload chosen; file=" + BulkImportContext.fileName, true, "manual=true");
	}

	// ------------------------------------------------------------------ read the job

	/** Finds the job row, waits until it is processed, keeps the SUCCESS customer refs and raises the FAILED ones as bugs. */
	public void readJobAndKeepSuccessIds() {
		if (BulkImportContext.cancelled) {
			return;
		}
		WebDriver d = d();
		openBulkDataImport();
		BiSupport.Table history = null;
		int rowIdx = -1;
		int successIdx = -1;
		int failedIdx = -1;
		int statusIdx = -1;
		int totalIdx = -1;
		List<String> cells = null;
		long end = System.currentTimeMillis() + 180_000;
		long lastRefresh = System.currentTimeMillis();
		while (System.currentTimeMillis() < end) {
			history = BiSupport.readTable(mainScope());
			successIdx = history.col("success");
			failedIdx = history.col("failed");
			statusIdx = history.col("status");
			totalIdx = history.col("total");
			rowIdx = -1;
			for (int i = 0; i < history.rows().size(); i++) {
				List<String> r = history.rows().get(i);
				boolean mine = !BulkImportContext.fileName.isBlank() ? r.stream().anyMatch(c -> c.equalsIgnoreCase(BulkImportContext.fileName)) : i == 0;
				if (mine) {
					rowIdx = i;
					cells = r;
					break;
				}
			}
			if (rowIdx >= 0 && statusIdx >= 0) {
				String st = cells.get(statusIdx).toLowerCase(Locale.ROOT);
				if (!(st.contains("process") || st.contains("pending") || st.contains("queue") || st.contains("running"))) {
					break;
				}
			} else if (rowIdx >= 0) {
				break;
			}
			if (System.currentTimeMillis() - lastRefresh > 15_000) {
				d.navigate().refresh();
				BiSupport.sleep(1500);
				lastRefresh = System.currentTimeMillis();
			}
			BiSupport.sleep(3000);
		}
		if (rowIdx < 0 || cells == null) {
			StepLog.check(SC + "Job row", "Find the uploaded file in the history list", "A row with " + BulkImportContext.fileName,
					"FAIL | row not found within 3 minutes", false, "file=" + BulkImportContext.fileName);
			throw new AssertionError("The uploaded file is not in the Bulk Data Import history: " + BulkImportContext.fileName);
		}
		BulkImportContext.total = toInt(totalIdx >= 0 ? cells.get(totalIdx) : "");
		BulkImportContext.successCount = toInt(successIdx >= 0 ? cells.get(successIdx) : "");
		BulkImportContext.failedCount = toInt(failedIdx >= 0 ? cells.get(failedIdx) : "");
		StepLog.check(SC + "Job result", "Read the job row", "Total, SUCCESS and FAILED counts",
				"row=" + cells + "; total=" + BulkImportContext.total + "; success=" + BulkImportContext.successCount + "; failed="
						+ BulkImportContext.failedCount, true, "job=" + BulkImportContext.jobNo);
		Hooks.captureMilestone("Bulk job row | " + BulkImportContext.fileName);
		String rowText = BulkImportContext.fileName.isBlank() ? "" : BulkImportContext.fileName;
		if (BulkImportContext.successCount > 0) {
			readRowsDialog(rowText, successIdx, BulkImportContext.successCount, true);
		}
		if (BulkImportContext.failedCount > 0) {
			readRowsDialog(rowText, failedIdx, BulkImportContext.failedCount, false);
		}
		classifyRecords();
	}

	private static org.openqa.selenium.By mainScope() {
		return org.openqa.selenium.By.xpath("//main");
	}

	private void readRowsDialog(String fileName, int countIdx, int count, boolean success) {
		WebDriver d = d();
		List<WebElement> rows = fileName.isBlank() ? d.findElements(org.openqa.selenium.By.xpath("(//table//tbody/tr)[1]"))
				: d.findElements(BulkImportPages.historyRow(fileName));
		if (rows.isEmpty()) {
			return;
		}
		List<WebElement> btn = rows.get(0).findElements(org.openqa.selenium.By.xpath(".//td[" + (countIdx + 1) + "]//button|.//button[normalize-space()='" + count + "']"));
		if (btn.isEmpty()) {
			StepLog.check(SC + (success ? "SUCCESS rows" : "FAILED rows"), "Open the " + (success ? "SUCCESS" : "FAILED") + " count", "Rows dialog opens",
					"FAIL | the count is not a button", false, "count=" + count);
			return;
		}
		BiSupport.clickJs(btn.get(0));
		if (!BiSupport.waitFor(BulkImportPages.DIALOG, 10)) {
			StepLog.check(SC + (success ? "SUCCESS rows" : "FAILED rows"), "Rows dialog", "Dialog opens", "FAIL | no dialog", false, "count=" + count);
			return;
		}
		BiSupport.sleep(900);
		BiSupport.Table t = BiSupport.readTable(BulkImportPages.DIALOG);
		Hooks.captureMilestone("Bulk " + (success ? "inserted" : "failed") + " rows | job " + BulkImportContext.jobNo);
		int iRec = t.col("record");
		int iSt = t.col("status");
		int iCk = t.col("ckyc", "ref");
		int iCu = t.col("customer", "ref");
		int iRp = t.col("rp");
		int iNm = t.col("name");
		int iRe = t.col("remark");
		int iCr = t.col("created");
		for (List<String> r : t.rows()) {
			BulkImportContext.InsertedRow row = new BulkImportContext.InsertedRow(at(r, iRec), at(r, iSt), at(r, iCk), at(r, iCu), at(r, iRp),
					at(r, iNm), at(r, iRe), at(r, iCr));
			(success ? BulkImportContext.inserted : BulkImportContext.failedRows).add(row);
		}
		BiSupport.closeDialog();
	}

	private static String at(List<String> r, int i) {
		return i >= 0 && i < r.size() ? r.get(i).replace("—", "").trim() : "";
	}

	private static int toInt(String s) {
		try {
			return Integer.parseInt(s.replaceAll("[^0-9]", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Expected ids = SUCCESS main applicants; Legal Entity must carry related persons; failed rows become bugs. */
	private void classifyRecords() {
		List<String> rp = new ArrayList<>();
		for (BulkImportContext.InsertedRow r : BulkImportContext.inserted) {
			if (r.mainApplicant()) {
				if (!r.customerRef().isBlank()) {
					BulkImportContext.expectedIds.add(r.customerRef());
					BulkImportContext.bucketName.put(r.customerRef(), r.name());
				}
			} else {
				rp.add(r.customerRef() + (r.rpRef().isBlank() ? "" : "/" + r.rpRef()));
			}
		}
		if (BulkImportContext.csv != null) {
			BulkImportContext.idColumn = BulkImportContext.csv.columnHolding(BulkImportContext.expectedIds) == null ? ""
					: BulkImportContext.csv.columnHolding(BulkImportContext.expectedIds);
		}
		boolean le = "LE".equals(BulkImportContext.persona);
		boolean hasRp = !rp.isEmpty();
		if (le && !hasRp && !BulkImportContext.inserted.isEmpty()) {
			BulkImportContext.bug("Bulk Data Import", BulkImportContext.fileName, "Legal Entity file inserted without any related person",
					"A Legal Entity record needs at least one related person (authorised signatory)");
			StepLog.check(SC + "Related person", "Legal Entity file has related person rows", "At least one Related Person row",
					"FAIL | none found among " + BulkImportContext.inserted.size() + " inserted rows", false, "persona=LE");
		} else if (hasRp) {
			StepLog.check(SC + "Related person", le ? "Legal Entity related persons" : "Individual file with related persons (minor guardian)",
					"Related person rows are listed with their own customer RP ref", "related persons=" + rp, true, "persona=" + BulkImportContext.persona);
		}
		StepLog.check(SC + "Expected ids", "Keep the SUCCESS customer ids", "Success main applicants are kept for the next steps",
				"expected=" + BulkImportContext.expectedIds.size() + " " + BulkImportContext.expectedIds + "; idColumnInCsv="
						+ BulkImportContext.idColumn, !BulkImportContext.expectedIds.isEmpty(), "job=" + BulkImportContext.jobNo);
		for (BulkImportContext.InsertedRow f : BulkImportContext.failedRows) {
			String what = "Record failed in Bulk Data Import: " + (f.customerRef().isBlank() ? f.name() : f.customerRef());
			BulkImportContext.bug("Bulk Data Import", f.customerRef().isBlank() ? f.name() : f.customerRef(), what, f.remarks());
			StepLog.check(SC + "Failed record", "Failed rows are kept as bugs", "No failed record", "FAIL | " + what + " | remarks=" + f.remarks(), false,
					"job=" + BulkImportContext.jobNo);
		}
		if (BulkImportContext.expectedIds.isEmpty()) {
			throw new AssertionError("No SUCCESS customer ids were found for job " + BulkImportContext.jobNo + " - nothing to follow on the Dashboard");
		}
	}
}
