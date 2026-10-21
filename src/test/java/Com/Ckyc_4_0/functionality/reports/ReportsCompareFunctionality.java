/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.reports.ReportComparer;
import Com.Ckyc_4_0.utils.reports.ReportTable;
import Com.Ckyc_4_0.utils.reports.ReportsExcelWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Weekly dashboard reports vs the Reports module, for NEW / UPDATE x ALL / Individual / Legal entity.
 *
 * For each application type x constitution:
 * 1. DASHBOARD: set to Weekly (only), that application type and constitution, then every sub-card popup is read
 *    completely (headers as shown, every row).
 * 2. REPORTS module, one report at a time exactly like a user: choose the category, choose the report type, set
 *    NEW / UPDATE and ALL / Individual / Legal entity (the date stays the default weekly range), click Search, then
 *    read the whole result (all pages) with its own dynamic headers. Others have no filters / Search - read once.
 * 3. COMPARE each dashboard sub-card with its report: counts first, then every remaining column per customer id.
 *    PASS / FAIL / NOT EXECUTED (element missing - the Actual result names it). A sub-card whose report is NOT in the
 *    Reports dropdown under its category is a BUG (its own status, counted apart from FAIL) with a screenshot of
 *    the sub-card and of the list. Nothing is compared for it - there is no report to compare with.
 *    Report types with no dashboard sub-card are STORED ONLY (never a bug; all of Others).
 *
 * The workbook is saved after every combination and always written - also when the browser is closed half way
 * (the remaining work is then reported as NOT EXECUTED with the reason).
 *
 * config.properties (all optional): reports.apps=NEW,UPDATE  reports.constitutions=ALL,Individual,Legal entity
 * reports.categories=&lt;comma separated, blank = all&gt;  reports.columnMismatchIsFail=true
 */
public final class ReportsCompareFunctionality {

	private static final Logger logger = LoggerFactory.getLogger(ReportsCompareFunctionality.class);

	public record Outcome(Path file, int pass, int fail, int bug, int notExecuted, int storedOnly, int tables,
			String abortReason) {
	}

	/** One dashboard sub-card with the popup table that was read. */
	private record DashItem(DashboardLive.SubCard sub, ReportTable table) {
	}

	private ReportsExcelWriter writer;
	private int pass;
	private int fail;
	private int bug;
	private int notExecuted;
	private int storedOnly;
	private int tables;
	private String abortReason = "";

	public Outcome run() {
		List<String> apps = csv(BaseClass.getProperty("reports.apps", "NEW,UPDATE"));
		List<String> cons = csv(BaseClass.getProperty("reports.constitutions", "ALL,Individual,Legal entity"));
		boolean columnFail = Boolean.parseBoolean(BaseClass.getProperty("reports.columnMismatchIsFail", "true"));
		Set<String> onlyCategories = csv(BaseClass.getProperty("reports.categories", "")).stream()
				.map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
		LocalDateTime start = LocalDateTime.now();
		Path file = Paths.get("Report Output", "Reports Compare", LocalDate.now().toString(),
				"Reports_Compare_" + start.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
		writer = new ReportsExcelWriter(file);
		writer.info("Tested by", Com.Ckyc_4_0.UtilityFiles.ConfigReader.testedBy());
		writer.info("Applications", String.valueOf(apps));
		writer.info("Constitutions", String.valueOf(cons));
		writer.info("Categories", onlyCategories.isEmpty() ? "all" : String.valueOf(onlyCategories));
		writer.info("Compare basis", "Dashboard Weekly only vs Reports module default (weekly) date range; counts, then "
				+ "every remaining column per customer id");
		writer.info("Column on one side only", columnFail ? "FAIL" : "note only");

		String where = "start";
		boolean othersDone = false;
		try {
			for (String app : apps) {
				for (String con : cons) {
					String combo = app + " / " + con;
					logger.info("=== Reports compare | {} ===", combo);

					// 1) DASHBOARD first - Weekly only
					where = "dashboard | " + combo;
					DashboardReportReader.openDashboard();
					DashboardReportReader.setFilters(app, con);
					writer.info("Dashboard date range (" + combo + ")", DashboardReportReader.dateRange());
					List<DashItem> dash = new ArrayList<>();
					List<DashboardLive.SubCard> subs = DashboardReportReader.subCards();
					if (subs.isEmpty()) {
						record("(dashboard)", null, null, new ReportComparer.Result(ReportComparer.NOT_EXECUTED,
								"No dashboard sub-cards found for " + combo + " (li with code span and aria-label not present)",
								List.of(), List.of()), combo, List.of());
					}
					for (DashboardLive.SubCard sc : subs) {
						if (!onlyCategories.isEmpty() && !onlyCategories.contains(sc.group().toLowerCase(Locale.ROOT))) {
							continue;
						}
						where = "dashboard popup " + sc.code() + " " + sc.label() + " | " + combo;
						ReportTable dt = DashboardReportReader.readPopup(sc, app, con);
						writer.addTable(dt);
						tables++;
						dash.add(new DashItem(sc, dt));
					}

					// 2) REPORTS module, one report at a time: category -> type -> NEW/UPDATE + constitution -> Search
					where = "reports module | " + combo;
					ReportsFunctionality.openModule();
					Map<String, List<String>> typesByCategory = new LinkedHashMap<>();
					Map<String, ReportTable> reports = new LinkedHashMap<>();
					for (String cat : ReportsFunctionality.categories()) {
						if (!onlyCategories.isEmpty() && !onlyCategories.contains(cat.toLowerCase(Locale.ROOT))) {
							continue;
						}
						List<String> types = ReportsFunctionality.types(cat);
						typesByCategory.put(cat, types);
						for (String type : types) {
							if (cat.equalsIgnoreCase("Others") && othersDone) {
								continue; // Others reports have no NEW / UPDATE / constitution filters - read once
							}
							where = "report " + cat + " / " + type + " | " + combo;
							ReportTable rt = ReportsFunctionality.run(cat, type, app, con);
							reports.put(key(cat, type), rt);
							writer.addTable(rt);
							tables++;
						}
					}
					othersDone = true;
					writer.info("Reports date range (" + combo + ")", ReportsFunctionality.dateRange());

					// 3) COMPARE
					where = "compare | " + combo;
					Map<String, Boolean> matched = new LinkedHashMap<>();
					List<DashItem> missing = new ArrayList<>();
					List<String> missingList = new ArrayList<>();
					for (DashItem it : dash) {
						DashboardLive.SubCard sc = it.sub();
						List<String> typesOfCategory = typesByCategory.getOrDefault(sc.group(), List.of());
						String type = typesOfCategory.contains(it.table().reportType()) ? it.table().reportType()
								: ReportMapper.match(sc.group(), sc.label(), typesOfCategory);
						String cardName = sc.code() + " " + sc.label() + " (" + sc.group() + ")";
						if (type == null) {
							missing.add(it);
							continue;
						}
						matched.put(key(sc.group(), type), true);
						ReportTable rt = reports.get(key(sc.group(), type));
						record(cardName, it.table(), rt, ReportComparer.compare(it.table(), rt, columnFail), combo, List.of());
					}
					// dashboard sub-cards that the Reports dropdown does not list under their category = BUG
					Map<String, String> listEvidence = new LinkedHashMap<>();
					for (DashItem it : missing) {
						DashboardLive.SubCard sc = it.sub();
						listEvidence.put(it.sub().code(), ReportsFunctionality.captureListEvidence(sc.group(),
								"BUG | Reports module list for '" + sc.group() + "' does not contain '" + sc.label() + "' | " + combo));
					}
					Map<String, String> cardEvidence = new LinkedHashMap<>();
					if (!missing.isEmpty()) {
						DashboardReportReader.openDashboard();
						DashboardReportReader.setFilters(app, con);
						for (DashItem it : missing) {
							DashboardLive.SubCard sc = it.sub();
							cardEvidence.put(sc.code(), DashboardReportReader.captureSubCardEvidence(sc, "BUG | dashboard sub-card "
									+ sc.code() + " " + sc.label() + " (" + sc.group() + ") has no report type in the Reports module | "
									+ combo));
						}
					}
					for (DashItem it : missing) {
						DashboardLive.SubCard sc = it.sub();
						List<String> listed = typesByCategory.getOrDefault(sc.group(), List.of());
						ReportComparer.Diff d = new ReportComparer.Diff("MISSING-REPORT-TYPE", "",
								"Report Type dropdown of '" + sc.group() + "'",
								"dashboard sub-card " + sc.code() + " '" + sc.label() + "' (count " + sc.count() + ")",
								"not listed - dropdown has: " + listed);
						ReportComparer.Result res = new ReportComparer.Result(ReportComparer.BUG,
								"BUG: dashboard sub-card '" + sc.label() + "' has no report type under category '" + sc.group()
										+ "' in the Reports module",
								List.of(d), List.of("Reports dropdown for '" + sc.group() + "' lists: " + listed));
						List<ReportsExcelWriter.Evidence> ev = new ArrayList<>();
						ev.add(new ReportsExcelWriter.Evidence("Dashboard: sub-card " + sc.code() + " '" + sc.label() + "' is shown",
								cardEvidence.getOrDefault(sc.code(), "")));
						ev.add(new ReportsExcelWriter.Evidence("Reports module: the '" + sc.group() + "' list does not contain it",
								listEvidence.getOrDefault(sc.code(), "")));
						record(sc.code() + " " + sc.label() + " (" + sc.group() + ")", it.table(), null, res, combo, ev);
					}
					for (Map.Entry<String, ReportTable> e : reports.entrySet()) {
						if (!matched.containsKey(e.getKey())) {
							ReportTable rt = e.getValue();
							ReportComparer.Result res = new ReportComparer.Result(
									rt.isNotExecuted() ? ReportComparer.NOT_EXECUTED : ReportComparer.STORED_ONLY,
									rt.isNotExecuted() ? "Reports module: " + rt.notExecutedReason()
											: "No dashboard sub-card for this report - " + rt.rows().size() + " rows stored",
									List.of(), List.of());
							record("(no dashboard sub-card)", null, rt, res, combo, List.of());
						}
					}
					writer.checkpoint(); // everything read so far is on disk, whatever happens next
				}
			}
		} catch (RuntimeException e) {
			abortReason = (SessionLost.is(e) ? "BROWSER CLOSED / session lost" : "Run stopped by " + e.getClass().getSimpleName())
					+ " at: " + where + " | " + firstLine(e.getMessage());
			logger.error("Reports compare stopped: {}", abortReason, e);
			notExecuted++;
			writer.addComparison("(run stopped)", null, null, new ReportComparer.Result(ReportComparer.NOT_EXECUTED,
					abortReason + " - everything read before this point is saved; the rest was not executed", List.of(),
					List.of()), List.of());
		}

		writer.info("Started", start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
		writer.info("Tables stored", String.valueOf(tables));
		if (!abortReason.isEmpty()) {
			writer.info("STOPPED", abortReason);
		}
		try {
			writer.write();
			logger.info("Reports compare workbook: {}", writer.file());
		} catch (java.io.IOException e) {
			throw new IllegalStateException("Could not write " + file + ": " + e.getMessage(), e);
		}
		return new Outcome(writer.file(), pass, fail, bug, notExecuted, storedOnly, tables, abortReason);
	}

	private void record(String card, ReportTable dash, ReportTable rep, ReportComparer.Result res, String combo,
			List<ReportsExcelWriter.Evidence> evidence) {
		writer.addComparison(card, dash, rep, res, evidence);
		switch (res.status()) {
			case ReportComparer.PASS -> pass++;
			case ReportComparer.FAIL -> fail++;
			case ReportComparer.BUG -> bug++;
			case ReportComparer.STORED_ONLY -> storedOnly++;
			default -> notExecuted++;
		}
		// the filters come FIRST: Extent cuts a title at 80 characters, and NEW / UPDATE + ALL / Individual / Legal
		// entity must always be visible in it
		String step = "[" + combo + "] " + card + " | Weekly compare";
		String expected = "Dashboard popup = Reports module: same count, then every column per customer id";
		String ev = evidence.stream().filter(e -> !e.path().isEmpty()).map(e -> fileName(e.path()))
				.collect(Collectors.joining(" | "));
		String actual = res.status() + " | " + res.summary()
				+ (res.notes().isEmpty() ? "" : " | " + String.join("; ", res.notes()))
				+ (ev.isEmpty() ? "" : " | Evidence: " + ev);
		boolean ok = !ReportComparer.FAIL.equals(res.status()) && !ReportComparer.BUG.equals(res.status());
		try {
			Hooks.logStepWithScenario(step, step, expected, actual, ok);
		} catch (RuntimeException e) {
			logger.debug("Step log skipped: {}", e.getMessage());
		}
		try {
			ExtentReportManager.logInfo(step + " | " + actual);
		} catch (RuntimeException | LinkageError ignored) {
			// report not started
		}
	}

	private static String fileName(String path) {
		if (path == null || path.isEmpty()) {
			return "(screenshot not taken)";
		}
		int i = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
		return i >= 0 ? path.substring(i + 1) : path;
	}

	private static String firstLine(String s) {
		if (s == null) {
			return "";
		}
		int nl = s.indexOf('\n');
		String l = nl > 0 ? s.substring(0, nl) : s;
		return l.length() > 300 ? l.substring(0, 300) : l;
	}

	private static String key(String category, String type) {
		return ReportTable.norm(category) + "|" + ReportTable.norm(type);
	}

	private static List<String> csv(String v) {
		return Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
	}
}
