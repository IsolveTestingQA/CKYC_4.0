/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.functionality.reports.DashboardLive;
import Com.Ckyc_4_0.functionality.reports.DashboardReportReader;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.reports.ReportTable;

/**
 * Dashboard > Data pushed from FI (A1 Success, A2 Failed, A3 In-Progress, A4 Duplicate). In-Progress is looked at first;
 * every 30 seconds the page is refreshed and the four lists are read again, until every uploaded customer id has reached
 * Success, Failed or Duplicate - at most 7 minutes. A timer card in the page shows the progress the whole time. After the
 * time an expected id that is in none of the four lists is MISSING (bug), and one still In-Progress is a bug too.
 */
public final class InwardPollFunctionality {

	private static final Logger logger = LoggerFactory.getLogger(InwardPollFunctionality.class);
	private static final String SC = "Dashboard | ";
	private static final String GROUP = "Data pushed from FI";

	private static boolean settled(String bucket) {
		return bucket != null && !bucket.toLowerCase(Locale.ROOT).contains("progress");
	}

	public void waitUntilSettled() {
		if (BulkImportContext.cancelled || BulkImportContext.expectedIds.isEmpty()) {
			return;
		}
		List<String> apps = Arrays.asList(ConfigReader.get("bulkimport.apps", "NEW,UPDATE").split("\\s*,\\s*"));
		long start = System.currentTimeMillis();
		long limitMs = BiPopups.waitMinutes() * 60_000L;
		long tickMs = Integer.parseInt(ConfigReader.get("bulkimport.pollSeconds", "30")) * 1000L;
		boolean stopped = false;
		int tick = 0;
		while (true) {
			tick++;
			long tickStart = System.currentTimeMillis();
			card(start, limitMs, "Reading the four lists (check " + tick + ")...", tickMs, 0);
			readAll(apps, start);
			BulkImportContext.Poll p = counts();
			BulkImportContext.polls.add(p);
			logger.info("Dashboard poll {} | in-progress={} success={} failed={} duplicate={} unseen={}", tick, p.inProgress(), p.success(),
					p.failed(), p.duplicate(), p.unseen());
			if (allSettled()) {
				break;
			}
			if (System.currentTimeMillis() - start >= limitMs) {
				break;
			}
			while (System.currentTimeMillis() - tickStart < tickMs) {
				long left = tickMs - (System.currentTimeMillis() - tickStart);
				card(start, limitMs, "Next refresh in " + (left / 1000) + " s", tickMs, left);
				if (BiPopups.stopRequested()) {
					stopped = true;
					break;
				}
				if (System.currentTimeMillis() - start >= limitMs) {
					break;
				}
				BiSupport.sleep(1000);
			}
			if (stopped) {
				break;
			}
			try {
				BiSupport.driver().navigate().refresh();
				BiSupport.sleep(1500);
			} catch (RuntimeException e) {
				logger.warn("Dashboard refresh failed: {}", e.getMessage());
			}
		}
		BiPopups.removeWaitCard();
		finish(start, stopped);
	}

	private void card(long start, long limitMs, String headline, long tickMs, long leftMs) {
		BulkImportContext.Poll p = counts();
		JSONArray lines = new JSONArray(List.of(headline,
				"Expected ids: " + BulkImportContext.expectedIds.size() + "   Not seen yet: " + p.unseen(),
				"In-Progress " + p.inProgress() + "  |  Success " + p.success() + "  |  Duplicate " + p.duplicate() + "  |  Failed " + p.failed(),
				"Refresh every " + (tickMs / 1000) + " s, up to " + (limitMs / 60000) + " min"));
		BiPopups.waitCard(new JSONObject().put("startMs", start).put("limitMs", limitMs).put("lines", lines));
	}

	private BulkImportContext.Poll counts() {
		int ip = 0, ok = 0, fail = 0, dup = 0;
		for (String id : BulkImportContext.expectedIds) {
			String b = BulkImportContext.bucket.get(id);
			if (b == null) {
				continue;
			}
			String l = b.toLowerCase(Locale.ROOT);
			if (l.contains("progress")) {
				ip++;
			} else if (l.contains("success")) {
				ok++;
			} else if (l.contains("fail")) {
				fail++;
			} else if (l.contains("dup")) {
				dup++;
			}
		}
		int unseen = BulkImportContext.expectedIds.size() - ip - ok - fail - dup;
		return new BulkImportContext.Poll(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), ip, ok, fail, dup, unseen);
	}

	private boolean allSettled() {
		for (String id : BulkImportContext.expectedIds) {
			if (!settled(BulkImportContext.bucket.get(id))) {
				return false;
			}
		}
		return true;
	}

	/** In-Progress first, then the other lists, for every application type. */
	private void readAll(List<String> apps, long start) {
		for (String app : apps) {
			try {
				DashboardReportReader.openDashboard();
				DashboardReportReader.setFilters(app, "ALL");
				List<DashboardLive.SubCard> subs = new ArrayList<>();
				for (DashboardLive.SubCard sc : DashboardReportReader.subCards()) {
					if (GROUP.equalsIgnoreCase(sc.group())) {
						subs.add(sc);
					}
				}
				subs.sort((a, b) -> Boolean.compare(!a.label().toLowerCase(Locale.ROOT).contains("progress"),
						!b.label().toLowerCase(Locale.ROOT).contains("progress")));
				for (DashboardLive.SubCard sc : subs) {
					ReportTable t = DashboardReportReader.readPopup(sc, app, "ALL");
					if (t.isNotExecuted()) {
						logger.warn("Dashboard list {} {} not read: {}", sc.code(), sc.label(), t.notExecutedReason());
						continue;
					}
					keep(sc, t, start);
				}
			} catch (RuntimeException e) {
				if (Com.Ckyc_4_0.functionality.reports.SessionLost.is(e)) {
					throw e;
				}
				logger.warn("Dashboard read for {} failed: {}", app, e.getMessage());
			}
		}
	}

	private void keep(DashboardLive.SubCard sc, ReportTable t, long start) {
		int idCol = t.keyColumnIndex();
		if (idCol < 0) {
			idCol = ReportTable.keyIndex(t.headers());
		}
		int nameCol = indexOf(t.headers(), "customer name");
		int regionCol = indexOf(t.headers(), "region");
		for (List<String> row : t.rows()) {
			if (idCol < 0 || idCol >= row.size()) {
				continue;
			}
			String id = row.get(idCol).trim();
			if (!BulkImportContext.expectedIds.contains(id)) {
				continue;
			}
			String now = sc.label();
			String old = BulkImportContext.bucket.get(id);
			if (old != null && settled(old) && !settled(now)) {
				continue; // never go back from a final list to In-Progress
			}
			if (old == null || !old.equals(now)) {
				BulkImportContext.bucket.put(id, now);
				BulkImportContext.seenAfterMs.put(id, System.currentTimeMillis() - start);
			}
			if (nameCol >= 0 && nameCol < row.size()) {
				BulkImportContext.bucketName.put(id + "|dash", row.get(nameCol));
			}
			if (regionCol >= 0 && regionCol < row.size()) {
				BulkImportContext.bucketRegion.put(id, row.get(regionCol));
			}
		}
	}

	private static int indexOf(List<String> headers, String word) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i).toLowerCase(Locale.ROOT).contains(word)) {
				return i;
			}
		}
		return -1;
	}

	private void finish(long start, boolean stopped) {
		long secs = (System.currentTimeMillis() - start) / 1000;
		int missing = 0;
		int stuck = 0;
		for (String id : BulkImportContext.expectedIds) {
			String b = BulkImportContext.bucket.get(id);
			String name = BulkImportContext.bucketName.getOrDefault(id, "");
			if (b == null) {
				missing++;
				BulkImportContext.bug("Dashboard", id, "Uploaded customer id is in none of the four Data pushed from FI lists after " + secs + " s",
						"Success / Failed / In-Progress / Duplicate all checked for NEW and UPDATE" + (stopped ? " (tester stopped the wait)" : ""));
				StepLog.check(SC + "Inward", "Customer id " + id + " appears in Data pushed from FI", "Found in Success, Failed, In-Progress or Duplicate",
						"FAIL | MISSING after " + secs + " s | name=" + name, false, "id=" + id);
			} else if (!settled(b)) {
				stuck++;
				BulkImportContext.bug("Dashboard", id, "Customer id is still In-Progress after " + secs + " s", "iFlow did not finish within the wait");
				StepLog.check(SC + "Inward", "Customer id " + id + " leaves In-Progress", "Success, Failed or Duplicate",
						"FAIL | still " + b + " after " + secs + " s | name=" + name, false, "id=" + id);
			} else if (b.toLowerCase(Locale.ROOT).contains("fail")) {
				BulkImportContext.bug("Dashboard", id, "Customer id reached Failed in Data pushed from FI", "name=" + name);
				StepLog.check(SC + "Inward", "Customer id " + id + " reaches Success", "Success", "FAIL | Failed | name=" + name, false, "id=" + id);
			} else {
				StepLog.check(SC + "Inward", "Customer id " + id + " appears in Data pushed from FI", "Found in a final list",
						(b.toLowerCase(Locale.ROOT).contains("dup") ? "DUPLICATE (reported, not compared)" : "OK") + " | list=" + b + " | after "
								+ (BulkImportContext.seenAfterMs.getOrDefault(id, 0L) / 1000) + " s | name=" + name, true, "id=" + id);
			}
		}
		BulkImportContext.Poll p = counts();
		StepLog.check(SC + "Summary", "All uploaded customer ids are inward", "Every expected id is Success, Failed or Duplicate",
				"expected=" + BulkImportContext.expectedIds.size() + "; success=" + p.success() + "; failed=" + p.failed() + "; duplicate="
						+ p.duplicate() + "; in-progress=" + stuck + "; missing=" + missing + "; waited=" + secs + " s", missing == 0 && stuck == 0,
				"job=" + BulkImportContext.jobNo);
		Hooks.captureMilestone("Dashboard inward result | missing=" + missing + " inProgress=" + stuck, missing == 0 && stuck == 0);
		compareWithCsv();
	}

	/** Name check against the CSV for the ids that reached Success (the dashboard shows the customer name). */
	private void compareWithCsv() {
		if (BulkImportContext.csv == null || BulkImportContext.idColumn.isBlank()) {
			return;
		}
		for (String id : BulkImportContext.expectedIds) {
			String b = BulkImportContext.bucket.get(id);
			String dash = BulkImportContext.bucketName.get(id + "|dash");
			if (b == null || dash == null || dash.isBlank()) {
				continue;
			}
			Map<String, String> row = BulkImportContext.csv.rowWhere(BulkImportContext.idColumn, id);
			if (row == null) {
				continue;
			}
			String first = row.getOrDefault(BulkImportContext.csv.headerContaining("first", "name") == null ? "" : BulkImportContext.csv.headerContaining("first", "name"), "");
			String last = row.getOrDefault(BulkImportContext.csv.headerContaining("last", "name") == null ? "" : BulkImportContext.csv.headerContaining("last", "name"), "");
			String full = row.getOrDefault(BulkImportContext.csv.headerContaining("full", "name") == null ? "" : BulkImportContext.csv.headerContaining("full", "name"), "");
			String csvName = (full.isBlank() ? (first + " " + last) : full).trim();
			boolean ok = csvName.isBlank() || Com.Ckyc_4_0.utils.bulkimport.FieldMapper.norm(dash).contains(Com.Ckyc_4_0.utils.bulkimport.FieldMapper.norm(csvName))
					|| Com.Ckyc_4_0.utils.bulkimport.FieldMapper.norm(csvName).contains(Com.Ckyc_4_0.utils.bulkimport.FieldMapper.norm(dash));
			if (!ok) {
				BulkImportContext.bug("Dashboard", id, "Customer name on the Dashboard differs from the CSV", "csv='" + csvName + "' dashboard='" + dash + "'");
			}
			StepLog.check(SC + "Name", "Dashboard name of " + id + " equals the CSV name", "Same name", "csv='" + csvName + "'; dashboard='" + dash + "'", ok,
					"id=" + id);
		}
	}
}
