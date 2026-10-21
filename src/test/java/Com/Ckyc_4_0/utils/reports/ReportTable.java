/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One report as read from the screen: its headers exactly as shown (dynamic - nothing is hard-coded) and every row
 * of every page, plus the count the screen declared for it. Used for the dashboard sub-card popups and for the
 * Reports module, so the two can be compared.
 */
public final class ReportTable {

	public static final String DASHBOARD = "DASHBOARD";
	public static final String REPORT = "REPORT";

	private final String source;
	private final String category;
	private final String reportType;
	private final String application;
	private final String constitution;
	private final List<String> headers = new ArrayList<>();
	private final List<List<String>> rows = new ArrayList<>();
	private Integer declaredCount;
	private String declaredFrom = "";
	private String notExecuted = "";
	private String code = "";
	private Integer cardCount;
	private String dateRange = "";
	private boolean countOnly;

	public ReportTable(String source, String category, String reportType, String application, String constitution) {
		this.source = source;
		this.category = category == null ? "" : category.trim();
		this.reportType = reportType == null ? "" : reportType.trim();
		this.application = application == null ? "" : application.trim();
		this.constitution = constitution == null ? "" : constitution.trim();
	}

	public String source() {
		return source;
	}

	public String category() {
		return category;
	}

	public String reportType() {
		return reportType;
	}

	public String application() {
		return application;
	}

	public String constitution() {
		return constitution;
	}

	public List<String> headers() {
		return headers;
	}

	public List<List<String>> rows() {
		return rows;
	}

	public Integer declaredCount() {
		return declaredCount;
	}

	public String declaredFrom() {
		return declaredFrom;
	}

	/** Sub-card code on the dashboard (A1, B3 ...), blank for Reports-module tables. */
	public String code() {
		return code;
	}

	public ReportTable code(String c) {
		this.code = c == null ? "" : c;
		return this;
	}

	/** True for a dashboard sub-card that opens another page: only its count is checked, it is never clicked. */
	public boolean isCountOnly() {
		return countOnly;
	}

	public ReportTable countOnly() {
		this.countOnly = true;
		return this;
	}

	/** From / To dates the screen showed when this table was read. */
	public String dateRange() {
		return dateRange;
	}

	public ReportTable dateRange(String range) {
		this.dateRange = range == null ? "" : range;
		return this;
	}

	/** Count printed on the dashboard sub-card itself (dashboard tables only). */
	public Integer cardCount() {
		return cardCount;
	}

	public ReportTable cardCount(int c) {
		this.cardCount = c;
		return this;
	}

	public ReportTable declared(int count, String from) {
		this.declaredCount = count;
		this.declaredFrom = from == null ? "" : from;
		return this;
	}

	/** Marks the table as not read (element missing / exception). The reason goes to the Actual result. */
	public ReportTable notExecuted(String reason) {
		this.notExecuted = reason == null ? "" : reason;
		return this;
	}

	public boolean isNotExecuted() {
		return !notExecuted.isEmpty();
	}

	public String notExecutedReason() {
		return notExecuted;
	}

	/**
	 * Index of the customer id column ("CUSTOMER ID", "Customer Ref No" ...), or -1. "CKYC REFERENCE NO" is not a
	 * customer key. Headers are dynamic, so the column is found by name, never by position.
	 */
	public int keyColumnIndex() {
		return keyIndex(headers);
	}

	public static int keyIndex(List<String> headers) {
		String[] prefs = { "customer id", "customer reference number", "customer reference no", "customer ref number",
				"customer ref no", "customer ref", "cust id", "customer number", "customer no" };
		for (String p : prefs) {
			for (int i = 0; i < headers.size(); i++) {
				if (norm(headers.get(i)).equals(p)) {
					return i;
				}
			}
		}
		for (int i = 0; i < headers.size(); i++) {
			String n = norm(headers.get(i));
			if (n.contains("customer") && !n.contains("name") && (n.contains("id") || n.contains("ref") || n.contains("no"))) {
				return i;
			}
		}
		return -1;
	}

	/** Title used for sheets and logs. */
	public String title() {
		String t = (code.isEmpty() ? "" : code + " ") + (reportType.isEmpty() ? category : reportType);
		return t.trim();
	}

	/** Key used to pair a dashboard table with a Reports-module table: normalised category + type. */
	public static String norm(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}
}
