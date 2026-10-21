/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.searchdownload;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus;
import Com.Ckyc_4_0.UtilityFiles.ScreenshotManager;
import Com.Ckyc_4_0.UtilityFiles.SearchResultStore;
import Com.Ckyc_4_0.functionality.reports.SessionLost;
import Com.Ckyc_4_0.pages.searchdownload.SearchPage;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interactive Search session (Search And Download &gt; Search): the operator picks the Document Type from a popup that
 * lists the application's own options, types the number in a popup that forces the format, and every search is
 * recorded - outcome, message, result fields, wait time, screenshot - in an Excel report. Negative values can be
 * typed (Negative test switch) or run as an automatic set; the application's own error messages are captured.
 * Close ends the session (the feature stops); Clear resets the page and the popup.
 */
public class SearchInteractiveFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(SearchInteractiveFunctionality.class);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	public record Summary(Path file, int searches, int pass, int fail, int info, boolean closedByOperator) {
	}

	/** How one document type is typed: popup mask, length and the format check. */
	private record Rule(String name, int max, String hint) {
		boolean valid(String v) {
			return switch (name) {
				case "PAN" -> v.matches("[A-Z]{5}[0-9]{4}[A-Z]");
				case "DIGITS" -> v.matches("[0-9]{" + max + "}");
				default -> !v.isBlank();
			};
		}
	}

	private final SearchFunctionality base = new SearchFunctionality();
	private final SearchResultsExcel excel = new SearchResultsExcel();
	private final List<String> history = new ArrayList<>();
	private final int waitSeconds = ConfigReader.getExplicitWait();
	private final int resultTimeoutSeconds = Integer.parseInt(ConfigReader.get("search.resultTimeoutSeconds", "90"));
	private final boolean includeNames = Boolean.parseBoolean(ConfigReader.get("search.report.includeNames", "false"));
	private int pass;
	private int fail;
	private int info;

	// ------------------------------------------------------------------ session

	public Summary runSession() {
		boolean closed = false;
		try {
			List<String> types = readDocumentTypes();
			excel.meta("Tested by", ConfigReader.testedBy());
			excel.meta("Started", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
			excel.meta("Document types in the dropdown", String.valueOf(types));
			excel.meta("Result wait limit", resultTimeoutSeconds + " s");
			if (types.isEmpty()) {
				throw new AssertionError("The Document Type dropdown shows no options (element "
						+ SearchPage.DOCUMENT_TYPE_OPTIONS + ")");
			}
			if (!SearchInteractivePrompt.enabled()) {
				runConfigured(types);
			} else {
				closed = interactive(types);
			}
		} catch (RuntimeException e) {
			if (SessionLost.is(e)) {
				excel.meta("STOPPED", "Browser closed during the session - results so far are saved");
				logger.error("Search session stopped: browser closed", e);
			} else {
				excel.meta("STOPPED", e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
				excel.save();
				throw e;
			}
		} finally {
			excel.meta("Closed by operator", String.valueOf(closed));
			excel.save();
			try {
				SearchInteractivePrompt.remove(getDriver());
			} catch (RuntimeException ignored) {
				// browser gone
			}
		}
		return new Summary(excel.file(), excel.count(), pass, fail, info, closed);
	}

	/** Operator loop. @return true when the operator pressed Close. */
	private boolean interactive(List<String> types) {
		String type = null;
		boolean negative = false;
		String last = "";
		while (true) {
			if (type == null) {
				SearchInteractivePrompt.Answer a = SearchInteractivePrompt.chooseType(types, history);
				if (a.action() == SearchInteractivePrompt.Action.CLOSE) {
					return true;
				}
				if (a.action() != SearchInteractivePrompt.Action.CHOSEN) {
					continue;
				}
				type = a.value();
				selectType(type);
				last = "";
			}
			Rule rule = ruleFor(type);
			SearchInteractivePrompt.Answer a = SearchInteractivePrompt.enterNumber(type, rule.name(), rule.max(), rule.hint(),
					last, negative, history);
			switch (a.action()) {
				case CLOSE -> {
					return true;
				}
				case CLEAR -> {
					clearPage();
					type = null;
					last = "";
					negative = false;
				}
				case CHANGE_TYPE -> type = null;
				case SEARCH -> {
					last = a.value();
					negative = a.negativeMode();
					search(type, rule, a.value(), a.negativeMode() ? "NEGATIVE (typed by operator)" : "POSITIVE (typed by operator)");
				}
				case NEGATIVES -> runAutomaticNegatives(type, rule);
				default -> {
					// CHOSEN is not an answer to the number popup
				}
			}
		}
	}

	/** No operator (headless / popups off): one search with the configured value of the first matching type. */
	private void runConfigured(List<String> types) {
		String wanted = ConfigReader.get("search.interactive.type", "PAN");
		String type = types.stream().filter(t -> t.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT)))
				.findFirst().orElse(types.get(0));
		selectType(type);
		String value = type.toLowerCase(Locale.ROOT).contains("pan") ? SearchTestData.get("pan") : SearchTestData.get("mobile");
		excel.meta("Mode", "No popups (headless / interactiveBlockerPrompt=false): configured value used");
		search(type, ruleFor(type), value, "POSITIVE (configured value)");
	}

	// ------------------------------------------------------------------ page actions

	private List<String> readDocumentTypes() {
		WebDriver d = getDriver();
		WebElement combo = Com.Ckyc_4_0.UtilityFiles.PomElementManager.findVisible(SearchPage.DOCUMENT_TYPE_COMBOBOX, waitSeconds);
		((JavascriptExecutor) d).executeScript("arguments[0].dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));", combo);
		MasterUiHelper.sleep(600);
		List<String> out = new ArrayList<>();
		for (WebElement o : d.findElements(SearchPage.DOCUMENT_TYPE_OPTIONS)) {
			String t = o.getText().trim();
			if (!t.isEmpty() && !out.contains(t)) {
				out.add(t);
			}
		}
		try {
			d.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
		} catch (RuntimeException ignored) {
			// list already closed
		}
		MasterUiHelper.sleep(300);
		return out;
	}

	private void selectType(String type) {
		MasterUiHelper.selectMuiOption(SearchPage.DOCUMENT_TYPE_COMBOBOX, type, waitSeconds);
		String selected = base.textOf(SearchPage.DOCUMENT_TYPE_COMBOBOX);
		boolean enabled = !base.isDisabled(SearchPage.DOCUMENT_NUMBER_INPUT);
		StepLog.check("Search | Select document type", "Select '" + type + "' from the dropdown",
				"Selection persists and Document Number is enabled",
				"selected='" + selected + "'; documentNumberEnabled=" + enabled, selected.contains(type) && enabled,
				"documentType=" + type);
		if (!selected.contains(type) || !enabled) {
			history.add("Document type '" + type + "' could not be selected (selected='" + selected + "', number enabled=" + enabled + ")  FAIL");
			fail++;
		}
	}

	private void clearPage() {
		base.clearAndVerifyReset();
		history.add("Clear pressed - page reset");
	}

	private Rule ruleFor(String type) {
		String lower = type.toLowerCase(Locale.ROOT);
		int uiMax = 0;
		try {
			String m = getDriver().findElement(SearchPage.DOCUMENT_NUMBER_INPUT).getAttribute("maxLength");
			uiMax = m == null || m.isBlank() ? 0 : Integer.parseInt(m.trim());
		} catch (RuntimeException ignored) {
			// no limit published by the field
		}
		if (lower.contains("pan")) {
			return new Rule("PAN", 10, "AAAAA9999A  (5 letters, 4 digits, 1 letter - e.g. BNIPA5626L)");
		}
		if (lower.contains("mobile")) {
			int max = uiMax > 0 ? uiMax : 10;
			return new Rule("DIGITS", max, max + " digits");
		}
		int max = uiMax > 0 ? uiMax : 30;
		return new Rule("ALNUM", max, "letters and digits, up to " + max);
	}

	// ------------------------------------------------------------------ one search

	private void search(String type, Rule rule, String value, String mode) {
		WebDriver d = getDriver();
		long t0 = System.currentTimeMillis();
		Map<String, String> row = new LinkedHashMap<>();
		row.put("Time", LocalDateTime.now().format(TIME));
		row.put("Mode", mode);
		row.put("Document type", type);
		row.put("Entered", value);
		try {
			base.enterDocument(value);
			String accepted = base.readDocument();
			boolean enabled = MasterUiHelper.isButtonEnabled(SearchPage.SEARCH_BUTTON);
			boolean formatValid = rule.valid(accepted.toUpperCase(Locale.ROOT));
			boolean negativeMode = mode.startsWith("NEGATIVE") || mode.startsWith("AUTO");
			row.put("Accepted by the field", accepted);
			row.put("Format valid", formatValid ? "yes" : "no");
			row.put("Search enabled", enabled ? "yes" : "no");
			List<String> issues = new ArrayList<>();
			String outcome;
			String message = fieldError();
			String verdict;

			if (!enabled) {
				outcome = "BLOCKED BY THE FIELD - Search stays disabled";
				if (formatValid) {
					verdict = "FAIL";
					issues.add("DEFECT: a valid-format value kept Search disabled");
				} else {
					verdict = "PASS";
				}
				row.put("Wait (s)", "0");
			} else {
				String before = signature();
				base.clickSearch();
				SearchFunctionality.Response r = awaitResponse(before);
				row.put("Wait (s)", String.format(Locale.ROOT, "%.1f", r.seconds()));
				message = r.message().isEmpty() ? fieldError() : r.message();
				if (r.record()) {
					outcome = "RECORD FOUND";
					readFields(row, issues);
					if (negativeMode && !formatValid) {
						verdict = "FAIL";
						issues.add("VALIDATION DEFECT: an invalid value returned a record");
					} else {
						verdict = issues.isEmpty() ? "PASS" : "FAIL";
					}
					persistForDownload(type, value, row);
				} else if (r.timedOut()) {
					outcome = "NO RESPONSE within " + Math.round(r.seconds()) + " s";
					verdict = "FAIL";
					issues.add("DEFECT: the search gave no result and no message");
				} else if (SearchFunctionality.containsAny(message, "token api", "server error", "internal server",
						"something went wrong", "service unavailable", "timed out", "timeout", "network error",
						"status code 5")) {
					// the application's back end failed - this is not an answer to the value that was searched
					outcome = "BACKEND / API ERROR - the search could not complete";
					verdict = "FAIL";
					issues.add("APP DEFECT: the search was not completed, the application answered '" + message + "'");
				} else if (SearchFunctionality.containsAny(message, "no record", "not found", "no data", "does not exist")) {
					outcome = "NO RECORD FOUND";
					verdict = "PASS";
				} else if (SearchFunctionality.containsAny(r.alertSeverity() + " " + message, "error/red", "invalid", "error",
						"failed", "failure", "required", "please")) {
					outcome = "ERROR MESSAGE SHOWN";
					if (formatValid && !negativeMode) {
						verdict = "FAIL";
						issues.add("DEFECT: a valid-format value was answered with an error message");
					} else {
						verdict = "PASS";
					}
				} else if (SearchFunctionality.containsAny(message, "success")) {
					outcome = "SUCCESS MESSAGE WITHOUT A RESULT";
					verdict = "FAIL";
					issues.add("APP DEFECT: the application says Success but shows no CKYC result");
				} else {
					outcome = "NO MEANINGFUL RESPONSE";
					verdict = "FAIL";
					issues.add("DEFECT: no record, no recognisable message");
				}
			}
			row.put("Outcome", outcome);
			row.put("Application message", message);
			row.put("Issues found", issues.isEmpty() ? "none" : String.join(" | ", issues));
			row.put("Verdict", verdict);
			String banner = "Search | " + type + " | " + (value.isEmpty() ? "(blank)" : value) + " | " + outcome + " | " + verdict;
			row.put("Screenshot", evidence(banner, "PASS".equals(verdict)));
			record(row, verdict, type, value, outcome);
			StepLog.check("Search | " + type + " | " + mode, "Search '" + value + "'",
					"Meaningful response; invalid values are blocked or answered with a message",
					"accepted='" + accepted + "'; enabled=" + enabled + "; outcome=" + outcome + "; message='" + message
							+ "'; waited=" + row.get("Wait (s)") + "s; issues=" + row.get("Issues found"),
					"PASS".equals(verdict), "document=" + type + "; value=" + value);
		} catch (RuntimeException e) {
			if (SessionLost.is(e)) {
				throw new SessionLost("Browser closed during a Search", e);
			}
			row.put("Outcome", "AUTOMATION ERROR");
			row.put("Application message", firstLine(e.getMessage()));
			row.put("Issues found", e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
			row.put("Verdict", "FAIL");
			row.put("Wait (s)", String.format(Locale.ROOT, "%.1f", (System.currentTimeMillis() - t0) / 1000.0));
			try {
				row.put("Screenshot", evidence("Search | " + type + " | " + value + " | AUTOMATION ERROR", false));
			} catch (RuntimeException ignored) {
				// no picture
			}
			record(row, "FAIL", type, value, "AUTOMATION ERROR");
		}
	}

	/** Waits for the application's answer (search can take time): see SearchFunctionality#awaitResponse. */
	private SearchFunctionality.Response awaitResponse(String beforeSignature) {
		return base.awaitResponse(beforeSignature);
	}

	private String signature() {
		return base.signature();
	}

	private String fieldError() {
		return base.fieldError();
	}

	private void readFields(Map<String, String> row, List<String> issues) {
		String[][] map = { { "CKYC Number", "CKYC Number" }, { "CKYC Reference ID", "CKYC Reference ID" },
				{ "Account Type", "Account Type" }, { "KYC Date", "KYC Date" }, { "Updated Date", "Updated Date" },
				{ "Full Name", "Full Name" }, { "Father / Spouse Full Name", "Father / Spouse Full Name" }, { "Age", "Age" },
				{ "Mobile Number", "Mobile Number" } };
		List<String> missing = new ArrayList<>();
		for (String[] f : map) {
			String v = base.readResultValue(f[0]);
			if (!SearchFunctionality.hasValue(v)) {
				missing.add(f[0]);
			}
			boolean name = f[0].equals("Full Name") || f[0].startsWith("Father");
			row.put(f[1], name && !includeNames && SearchFunctionality.hasValue(v) ? "<present - hidden in report>" : v);
		}
		String masked = base.readResultValue("CKYC Number");
		if (SearchFunctionality.hasValue(masked) && !masked.matches("[X*]+[0-9]{4}")) {
			issues.add("DEFECT: CKYC Number is not masked to the last four digits ('" + masked + "')");
		}
		if (!missing.isEmpty()) {
			issues.add("Result field(s) empty: " + missing);
		}
		boolean photo = base.isCustomerPhotoFilePresent();
		row.put("Customer photo", photo ? "image loaded" : "no image file");
		if (!photo) {
			issues.add("APP DEFECT: Customer Photo file not present");
		}
	}

	private void persistForDownload(String type, String value, Map<String, String> row) {
		String masked = row.getOrDefault("CKYC Number", "");
		String ref = row.getOrDefault("CKYC Reference ID", "");
		if (SearchFunctionality.hasValue(masked) && SearchFunctionality.hasValue(ref) && masked.matches("[X*]+[0-9]{4}")) {
			boolean pan = type.toLowerCase(Locale.ROOT).contains("pan");
			SearchResultStore.save(masked, ref, pan ? value : "", pan ? "" : value);
		}
	}

	private String evidence(String banner, boolean ok) {
		ScreenshotFocus.set(SearchPage.RESULT_REGION, SearchPage.ALERT, SearchPage.FIELD_ERROR);
		ScreenshotFocus.banner(banner);
		try {
			String path = ScreenshotManager.captureAndAttach(getDriver(), null, banner, ok);
			return path == null ? "" : path;
		} finally {
			ScreenshotFocus.release(getDriver());
			ScreenshotFocus.clear();
		}
	}

	private void record(Map<String, String> row, String verdict, String type, String value, String outcome) {
		excel.add(row);
		switch (verdict) {
			case "PASS" -> pass++;
			case "FAIL" -> fail++;
			default -> info++;
		}
		history.add("#" + excel.count() + "  " + type + "  " + (value.isEmpty() ? "(blank)" : value) + "  ->  " + outcome + "  ("
				+ row.getOrDefault("Wait (s)", "?") + " s)  " + verdict);
		if (history.size() > 8) {
			history.remove(0);
		}
	}

	// ------------------------------------------------------------------ automatic negatives

	private void runAutomaticNegatives(String type, Rule rule) {
		List<String> values = new ArrayList<>();
		switch (rule.name()) {
			case "PAN" -> values.addAll(List.of("", "ABCDE1234", "abcde1234f", "ABCDE12X4F", "ABCDE123$F", "ABCDE1234FA",
					"ABCDEFGHIJ", "1234567890", "ABCDE 234F", "ZZZZZ9999Z"));
			case "DIGITS" -> values.addAll(List.of("", "123456789", "12345678901", "ABCDEFGHIJ", "98765@3210", "1234567890",
					"0000000000"));
			default -> values.addAll(List.of("", "!@#$%^&*", "A".repeat(60), "0000000000"));
		}
		for (String v : values) {
			search(type, rule, v, "AUTO NEGATIVE");
			try {
				base.enterDocument("");
			} catch (RuntimeException ignored) {
				// next search enters its own value
			}
		}
	}

	private static String firstLine(String s) {
		if (s == null) {
			return "";
		}
		int nl = s.indexOf('\n');
		String l = nl > 0 ? s.substring(0, nl) : s;
		return l.length() > 300 ? l.substring(0, 300) : l;
	}
}
