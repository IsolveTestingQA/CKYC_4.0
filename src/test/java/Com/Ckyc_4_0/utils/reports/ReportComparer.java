/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.reports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compares the dashboard report of a sub-card with the same report in the Reports module.
 * PASS when the counts agree, every customer is on both sides and every shared column holds the same value.
 * FAIL on any count difference, missing / extra customer, or value difference (the diff names the customer and the
 * column). NOT EXECUTED when either side could not be read (the reason is the screen's own exception text).
 * Columns are matched BY HEADER NAME, so a new or re-ordered column never breaks the comparison.
 */
public final class ReportComparer {

	public static final String PASS = "PASS";
	public static final String FAIL = "FAIL";
	/** A dashboard sub-card whose report type is not in the Reports module list - a product defect. */
	public static final String BUG = "BUG";
	public static final String NOT_EXECUTED = "NOT EXECUTED";
	public static final String STORED_ONLY = "STORED ONLY";

	public record Diff(String kind, String customer, String column, String dashboard, String report) {
	}

	public record Result(String status, String summary, List<Diff> diffs, List<String> notes) {
	}

	private static final int MAX_DIFFS = 500;

	private ReportComparer() {
	}

	public static Result compare(ReportTable dash, ReportTable rep) {
		return compare(dash, rep, true);
	}

	/** @param columnMismatchIsFail a column on one side only (headers are dynamic) is a difference when true */
	public static Result compare(ReportTable dash, ReportTable rep, boolean columnMismatchIsFail) {
		List<String> notes = new ArrayList<>();
		if (dash == null || rep == null) {
			return new Result(NOT_EXECUTED, (dash == null ? "Dashboard report" : "Reports-module report") + " was not read", List.of(), notes);
		}
		if (dash.isNotExecuted() || rep.isNotExecuted()) {
			String why = (dash.isNotExecuted() ? "Dashboard: " + dash.notExecutedReason() : "")
					+ (dash.isNotExecuted() && rep.isNotExecuted() ? " | " : "")
					+ (rep.isNotExecuted() ? "Reports module: " + rep.notExecutedReason() : "");
			return new Result(NOT_EXECUTED, why, List.of(), notes);
		}
		List<Diff> diffs = new ArrayList<>();

		if (dash.isCountOnly()) {
			// the sub-card opens another page: only its number is compared with the report's record count
			Integer dCount = dash.declaredCount();
			Integer rCount = rep.declaredCount();
			if (rCount == null) {
				return new Result(NOT_EXECUTED, "Reports module did not show a record count for this report - sub-card number "
						+ dCount + " could not be checked", List.of(), notes);
			}
			if (!rCount.equals(dCount)) {
				add(diffs, new Diff("COUNT", "", "sub-card number vs report record count", String.valueOf(dCount),
						rCount + " (" + rep.declaredFrom() + ")"));
			}
			if (rCount != rep.rows().size()) {
				add(diffs, new Diff("ROWS-READ", "", "report rows read", "declared " + rCount, String.valueOf(rep.rows().size())));
			}
			notes.add("Count check only - the sub-card opens another page and is not clicked");
			return diffs.isEmpty()
					? new Result(PASS, "Count check only: sub-card " + dCount + " = report " + rCount, diffs, notes)
					: new Result(FAIL, diffs.size() + " difference(s): " + firstDiffs(diffs), diffs, notes);
		}

		int dRows = dash.rows().size();
		int rRows = rep.rows().size();
		if (dash.cardCount() != null && dash.declaredCount() != null && !dash.cardCount().equals(dash.declaredCount())) {
			add(diffs, new Diff("CARD-VS-POPUP", "", "sub-card count vs popup count", String.valueOf(dash.cardCount()),
					String.valueOf(dash.declaredCount())));
		}
		Integer dDecl = dash.declaredCount();
		Integer rDecl = rep.declaredCount();
		if (dDecl != null && rDecl != null && !dDecl.equals(rDecl)) {
			add(diffs, new Diff("COUNT", "", "declared count", dDecl + " (" + dash.declaredFrom() + ")", rDecl + " (" + rep.declaredFrom() + ")"));
		}
		if (dDecl != null && dDecl != dRows) {
			add(diffs, new Diff("ROWS-READ", "", "dashboard rows read", String.valueOf(dRows), "declared " + dDecl));
		}
		if (rDecl != null && rDecl != rRows) {
			add(diffs, new Diff("ROWS-READ", "", "report rows read", "declared " + rDecl, String.valueOf(rRows)));
		}
		if (dDecl == null && dRows != rRows) {
			add(diffs, new Diff("COUNT", "", "rows read", String.valueOf(dRows), String.valueOf(rRows)));
		}

		Map<String, Integer> dIdx = indexByNorm(dash.headers());
		Map<String, Integer> rIdx = indexByNorm(rep.headers());
		List<String> common = new ArrayList<>();
		for (String h : dash.headers()) {
			String n = norm(h);
			if (!isSerial(n) && rIdx.containsKey(n) && !common.contains(n)) {
				common.add(n);
			}
		}
		for (String h : dash.headers()) {
			if (!isSerial(norm(h)) && !rIdx.containsKey(norm(h))) {
				notes.add("Column only on the dashboard: " + h);
				add(diffs, new Diff("COLUMN-ONLY-ON-DASHBOARD", "", h, "present", "missing in the Reports module"));
			}
		}
		for (String h : rep.headers()) {
			if (!isSerial(norm(h)) && !dIdx.containsKey(norm(h))) {
				notes.add("Column only in the Reports module: " + h);
				add(diffs, new Diff("COLUMN-ONLY-IN-REPORT", "", h, "missing on the dashboard", "present"));
			}
		}
		String keyNorm = keyColumn(common);
		if (keyNorm == null && (dRows > 0 || rRows > 0)) {
			notes.add("No customer id column found - rows compared as whole rows on the shared columns");
		}

		Map<String, List<List<String>>> dMap = group(dash, dIdx, common, keyNorm);
		Map<String, List<List<String>>> rMap = group(rep, rIdx, common, keyNorm);
		Set<String> keys = new LinkedHashSet<>(dMap.keySet());
		keys.addAll(rMap.keySet());
		for (String key : keys) {
			List<List<String>> d = dMap.getOrDefault(key, List.of());
			List<List<String>> r = rMap.getOrDefault(key, List.of());
			String label = keyNorm == null ? "row" : key;
			if (d.isEmpty()) {
				add(diffs, new Diff("MISSING-ON-DASHBOARD", label, "", "-", "present in report (" + r.size() + ")"));
				continue;
			}
			if (r.isEmpty()) {
				add(diffs, new Diff("MISSING-IN-REPORT", label, "", "present on dashboard (" + d.size() + ")", "-"));
				continue;
			}
			if (d.size() != r.size()) {
				add(diffs, new Diff("DUPLICATE-COUNT", label, "occurrences", String.valueOf(d.size()), String.valueOf(r.size())));
			}
			for (int i = 0; i < Math.min(d.size(), r.size()); i++) {
				for (int c = 0; c < common.size(); c++) {
					String dv = d.get(i).get(c);
					String rv = r.get(i).get(c);
					if (!dv.equals(rv)) {
						add(diffs, new Diff("VALUE", label, headerOf(dash, common.get(c)), dv, rv));
					}
				}
			}
		}

		long columnDiffs = diffs.stream().filter(d -> d.kind().startsWith("COLUMN-")).count();
		boolean failing = diffs.stream().anyMatch(d -> !d.kind().startsWith("COLUMN-") || columnMismatchIsFail);
		String status = failing ? FAIL : PASS;
		String summary = failing
				? diffs.size() + " difference(s): " + firstDiffs(diffs)
				: "Counts and all " + dRows + " rows match on " + common.size() + " shared columns"
						+ (columnDiffs > 0 ? " | " + columnDiffs + " column(s) on one side only - listed in COLUMN_DIFFS" : "");
		return new Result(status, summary, diffs, notes);
	}

	private static void add(List<Diff> diffs, Diff d) {
		if (diffs.size() < MAX_DIFFS) {
			diffs.add(d);
		}
	}

	private static String firstDiffs(List<Diff> diffs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(3, diffs.size()); i++) {
			Diff d = diffs.get(i);
			if (i > 0) {
				sb.append("; ");
			}
			sb.append(d.kind());
			if (!d.customer().isEmpty()) {
				sb.append(' ').append(d.customer());
			}
			if (!d.column().isEmpty()) {
				sb.append(" [").append(d.column()).append(']');
			}
			sb.append(": dashboard '").append(d.dashboard()).append("' vs report '").append(d.report()).append('\'');
		}
		return sb.toString();
	}

	/** Rows keyed by customer id (or by whole row when there is no id column), values in the order of {@code common}. */
	private static Map<String, List<List<String>>> group(ReportTable t, Map<String, Integer> idx, List<String> common, String keyNorm) {
		Map<String, List<List<String>>> map = new LinkedHashMap<>();
		for (List<String> row : t.rows()) {
			List<String> vals = new ArrayList<>();
			for (String n : common) {
				int i = idx.get(n);
				vals.add(i < row.size() ? clean(row.get(i)) : "");
			}
			String key = keyNorm == null ? String.join("\u0001", vals) : vals.get(common.indexOf(keyNorm)).toUpperCase(Locale.ROOT);
			map.computeIfAbsent(key, k -> new ArrayList<>()).add(vals);
		}
		return map;
	}

	private static String keyColumn(List<String> common) {
		for (String pref : List.of("customer id", "customer reference number", "customer ref no", "cust id", "customer no", "customer number")) {
			if (common.contains(pref)) {
				return pref;
			}
		}
		for (String n : common) {
			if (n.contains("customer") && n.contains("id")) {
				return n;
			}
		}
		return null;
	}

	private static String headerOf(ReportTable t, String normName) {
		for (String h : t.headers()) {
			if (norm(h).equals(normName)) {
				return h;
			}
		}
		return normName;
	}

	private static Map<String, Integer> indexByNorm(List<String> headers) {
		Map<String, Integer> idx = new LinkedHashMap<>();
		for (int i = 0; i < headers.size(); i++) {
			idx.putIfAbsent(norm(headers.get(i)), i);
		}
		return idx;
	}

	private static boolean isSerial(String n) {
		return n.equals("s no") || n.equals("sno") || n.equals("sr no") || n.equals("sl no") || n.equals("serial no") || n.equals("no") || n.isEmpty();
	}

	static String norm(String s) {
		return ReportTable.norm(s);
	}

	static String clean(String s) {
		return s == null ? "" : s.replace(' ', ' ').trim().replaceAll("\\s+", " ");
	}
}
