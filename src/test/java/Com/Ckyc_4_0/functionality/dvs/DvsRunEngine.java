/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsExcelReader;
import Com.Ckyc_4_0.utils.dvs.DvsFindings;
import Com.Ckyc_4_0.utils.dvs.DvsPersona;
import Com.Ckyc_4_0.utils.dvs.DvsResultWriter;
import Com.Ckyc_4_0.utils.dvs.DvsRow;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Run modes: POSITIVE_ONLY, NEGATIVE_ONLY, COMBINED (positive, snapshot, negatives group by group, last positive
 * data applied at every group end), REGRESSION. Filters: module / priority / group / TC. Every run writes its own
 * result workbook, a Step_Log sheet and logs/&lt;Run_ID&gt;.log. With dvs.dryRun=true nothing touches the browser.
 * Customer ids come from config (dvs.cust.individual / dvs.cust.minor / dvs.cust.le, comma separated): the first one
 * found in the Maker queue is used.
 */
public final class DvsRunEngine {

	public enum Mode { POSITIVE_ONLY, NEGATIVE_ONLY, COMBINED, REGRESSION }

	public record Filter(Set<String> modules, Set<String> priorities, String groupGlob, String tc) {
		public static Filter none() {
			return new Filter(Set.of(), Set.of(), "", "");
		}

		String describe() {
			return "module=" + (modules.isEmpty() ? "all" : modules) + ", priority=" + (priorities.isEmpty() ? "all" : priorities)
					+ ", group=" + (groupGlob.isEmpty() ? "all" : groupGlob) + ", tc=" + (tc.isEmpty() ? "all" : tc);
		}
	}

	public record Outcome(Path resultFile, int total, int pass, int fail, int blocked, int captured, int notRun, int planned,
			int bugs) {
	}

	private static final Logger logger = LoggerFactory.getLogger(DvsRunEngine.class);
	private static final List<String> POSITIVE_SHEETS = List.of("POSITIVE");
	private static final List<String> NEGATIVE_SHEETS = List.of("NEGATIVE", "BOUNDARY", "DEPENDENCY", "DOCUMENT_MAPPING", "IMAGE_MAPPING");
	private static final List<String> REGRESSION_SHEETS = List.of("REGRESSION");
	private static final List<String> MODULE_ORDER = List.of("IND", "MIN", "LE");

	private final Map<String, String> info = new LinkedHashMap<>();
	private final Set<String> bugLocked = new LinkedHashSet<>();
	private boolean aborted;
	private String abortReason = "";

	public static Mode modeFromConfig(Mode fallback) {
		String v = DvsConfig.get("dvs.mode", DvsConfig.get("run.mode", fallback.name()));
		try {
			return Mode.valueOf(v.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	public static Filter filterFromConfig() {
		return new Filter(csv(DvsConfig.get("dvs.module", "")), csv(DvsConfig.get("dvs.priority", "")),
				DvsConfig.get("dvs.group", ""), DvsConfig.get("dvs.tc", ""));
	}

	public Outcome run(Mode mode, Filter filter) {
		LocalDateTime start = LocalDateTime.now();
		boolean dry = DvsConfig.getBool("dvs.dryRun", false);
		Path data = DvsConfig.testDataPath();
		if (!Files.exists(data)) {
			throw new IllegalStateException("DVS test data not found: " + data.toAbsolutePath());
		}
		DvsStepLog.startRun(mode.name());
		DvsFindings.clear();

		List<DvsRow> positive = new ArrayList<>();
		List<DvsRow> negative = new ArrayList<>();
		switch (mode) {
			case POSITIVE_ONLY -> positive.addAll(load(data, POSITIVE_SHEETS, filter));
			case NEGATIVE_ONLY -> negative.addAll(load(data, NEGATIVE_SHEETS, filter));
			case COMBINED -> {
				positive.addAll(load(data, POSITIVE_SHEETS, filter));
				negative.addAll(load(data, NEGATIVE_SHEETS, filter));
			}
			case REGRESSION -> negative.addAll(load(data, REGRESSION_SHEETS, filter));
		}
		List<DvsRow> all = new ArrayList<>(positive);
		all.addAll(negative);

		info.put("Run_ID", DvsStepLog.runId());
		info.put("Started", start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
		info.put("Filter", filter.describe());
		info.put("Data file", data.toAbsolutePath().toString());
		info.put("Dry run", String.valueOf(dry));
		info.put("Restore mode", DvsConfig.get("restore.mode", "GROUP_END"));
		info.put("Checks", "data.audit=" + DvsConfig.get("data.audit", "ON") + ", tabswitch.check="
				+ DvsConfig.get("tabswitch.check", "ON") + ", checker.recovery=" + DvsConfig.get("checker.recovery", "ON")
				+ ", keep.bug.value=" + DvsConfig.get("keep.bug.value", "true"));
		info.put("Group boundary note", "Group end = last row of each Group_ID; the Restore_Point column is not used to decide it");

		try {
			if (!dry) {
				DvsNavigation.ensureSession();
				DvsNavigation.selectMaker();
			}
			for (String module : MODULE_ORDER) {
				if (aborted) {
					break;
				}
				runModule(mode, module, rows(positive, module), rows(negative, module), dry);
			}
		} catch (DvsRowExecutor.RunAbort e) {
			abort(e.getMessage());
		} catch (RuntimeException e) {
			abort("Run could not continue: " + e.getMessage());
			logger.error("DVS run stopped", e);
			DvsStepLog.step("run-stopped").actual(String.valueOf(e)).result(DvsStepLog.FAIL).log();
		}
		blockRemaining(all, abortReason.isEmpty() ? "Not reached" : "Run stopped: " + abortReason);

		LocalDateTime end = LocalDateTime.now();
		info.put("Ended", end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
		info.put("Total time taken", format(Duration.between(start, end)));
		info.put("Bugs / audit findings", String.valueOf(DvsFindings.all().size()) + " (see Bugs sheet)");
		info.put("Step log", "Step_Log sheet and logs/" + DvsStepLog.runId() + ".log (" + DvsStepLog.size() + " steps)");
		if (!abortReason.isEmpty()) {
			info.put("STOPPED", abortReason);
		}
		Path file = DvsResultWriter.write(mode.name(), info, all);
		DvsStepLog.close();
		logger.info("DVS result file: {}", file);
		return summarize(file, all, DvsFindings.all().size());
	}

	private void runModule(Mode mode, String module, List<DvsRow> pos, List<DvsRow> neg, boolean dry) {
		if (pos.isEmpty() && neg.isEmpty()) {
			return;
		}
		List<String> candidates = customersFor(module, pos, neg);
		String prefix = "LE".equals(module) ? "LE" : "IND";
		DvsRowExecutor exec = new DvsRowExecutor(dry, bugLocked);
		String cust = candidates.isEmpty() ? "" : candidates.get(0);
		DvsPersona persona = DvsPersona.UNKNOWN;
		if (!dry) {
			DvsNavigation.selectMaker();
			DvsNavigation.selectModule(module);
			try {
				cust = DvsNavigation.openFirstAvailable(candidates);
			} catch (RuntimeException e) {
				String why = "No customer could be opened for " + module + ": " + e.getMessage();
				info.put("Customer " + module, why);
				for (DvsRow r : concat(pos, neg)) {
					r.result(DvsRow.BLOCKED, why);
				}
				return;
			}
			persona = DvsPersona.detect();
		}
		info.put("Customer " + module, cust + " (candidates " + candidates + ")");
		info.put("Persona " + module, persona.name());
		exec.customer(cust);
		DvsStepLog.context("", cust, persona.name(), DvsNavigation.currentRole());
		DvsStepLog.step("persona").actual(persona.name()).result(DvsStepLog.INFO).log();

		gateByPersona(pos, persona, dry);
		gateByPersona(neg, persona, dry);

		if (!dry && DvsDataAudit.enabled()) {
			int added = DvsDataAudit.run(module, cust);
			info.put("Audit " + module, added + " findings (A1-A3)");
		}

		boolean p0Failed = false;
		DvsSnapshot snapshot = new DvsSnapshot();
		for (DvsRow row : pos) {
			if (!row.status().isEmpty()) {
				continue;
			}
			runRow(exec, row, module, cust, dry);
			if (exec.customerBlocked()) {
				blockCustomer(concat(pos, neg), exec.blockedReason());
				return;
			}
			p0Failed |= DvsRow.FAIL.equals(row.status()) && row.priorityRank() == 0;
		}
		if (!pos.isEmpty() && !dry && !p0Failed) {
			snapshot = DvsRestoreManager.capture(prefix);
			snapshot.save(cust);
			info.put("Snapshot " + module, snapshot.values().size() + " fields saved");
		}
		exec.lastPositive(snapshot);

		if (neg.isEmpty()) {
			return;
		}
		if (mode == Mode.COMBINED && p0Failed) {
			for (DvsRow r : neg) {
				if (r.status().isEmpty()) {
					r.result(DvsRow.BLOCKED, "Positive P0 failed for " + module + " - negatives not run on a broken baseline");
				}
			}
			return;
		}
		if (mode != Mode.COMBINED && !dry) {
			snapshot = DvsSnapshot.load(cust);
			if (snapshot.isEmpty()) {
				snapshot = DvsRestoreManager.capture(prefix);
				info.put("Snapshot " + module, "no saved last-positive file; current record values used as baseline");
			}
			exec.lastPositive(snapshot);
		}
		runGroups(neg, exec, snapshot, module, cust, dry);
	}

	private void runGroups(List<DvsRow> neg, DvsRowExecutor exec, DvsSnapshot snapshot, String module, String cust, boolean dry) {
		boolean rowMode = "ROW".equalsIgnoreCase(DvsConfig.get("restore.mode", "GROUP_END"));
		for (List<DvsRow> group : orderedGroups(neg)) {
			Set<String> groupTouched = new LinkedHashSet<>();
			for (DvsRow row : group) {
				if (!row.status().isEmpty()) {
					continue;
				}
				runRow(exec, row, module, cust, dry);
				if (exec.customerBlocked()) {
					blockCustomer(neg, exec.blockedReason());
					return;
				}
				Set<String> t = exec.drainTouched();
				t.removeAll(bugLocked);
				groupTouched.addAll(t);
				if (dry) {
					continue;
				}
				if (rowMode && !t.isEmpty()) {
					DvsRestoreManager.restore(snapshot, t);
				} else if (DvsFieldActions.dialogOpen()) {
					DvsFieldActions.dismissDialog();
					DvsRestoreManager.restore(snapshot, t);
				}
			}
			groupTouched.removeAll(bugLocked);
			if (!dry && !rowMode && !groupTouched.isEmpty() && !snapshot.isEmpty()) {
				if (!DvsRestoreManager.restoreGroupEnd(snapshot, groupTouched, module, cust)) {
					abort("Record " + cust + " could not be restored to last positive data after group "
							+ group.get(0).groupId() + " (irrecoverable)");
					return;
				}
			}
		}
	}

	/** One row: run, retry up to 2 times while the verdict is only an exception (R5), then log. */
	private void runRow(DvsRowExecutor exec, DvsRow row, String module, String cust, boolean dry) {
		int mark = DvsStepLog.size();
		DvsStepLog.tc(row.tcId());
		exec.execute(row);
		int tries = 0;
		while (!dry && DvsRow.FAIL.equals(row.status()) && row.actual().startsWith("Exception:") && tries < 2) {
			tries++;
			DvsStepLog.step("retry").field(row.locatorId()).actual("attempt " + tries + "/2 after: " + row.actual())
					.result(DvsStepLog.RETRY).log();
			try {
				DvsFieldActions.dismissDialogIfOpen();
				DvsNavigation.reloadAndReopen(module, cust);
			} catch (RuntimeException e) {
				DvsStepLog.step("retry").actual("re-open failed: " + e.getMessage()).result(DvsStepLog.FAIL).log();
				break;
			}
			row.result("", "");
			exec.execute(row);
		}
		row.stepRange(DvsStepLog.rangeSince(mark));
		extentLog(row, dry);
	}

	private static void extentLog(DvsRow row, boolean dry) {
		if (dry) {
			return;
		}
		String line = row.tcId() + " | " + row.status() + " | " + row.actual();
		try {
			if (DvsRow.FAIL.equals(row.status())) {
				ExtentReportManager.logFail(line);
			} else if (DvsRow.PASS.equals(row.status())) {
				ExtentReportManager.logPass(line);
			} else {
				ExtentReportManager.logInfo(line);
			}
		} catch (RuntimeException | LinkageError ignored) {
			// report not started (running outside a Cucumber scenario)
		}
	}

	/** Minor rows need a Minor record, LE rows need the Legal Entity toggle (persona from the record, not the id). */
	private static void gateByPersona(List<DvsRow> rows, DvsPersona persona, boolean dry) {
		if (dry || persona == DvsPersona.UNKNOWN) {
			return;
		}
		for (DvsRow r : rows) {
			if (!r.status().isEmpty()) {
				continue;
			}
			String code = r.moduleCode();
			if ("MIN".equals(code) && persona != DvsPersona.MINOR) {
				r.result(DvsRow.NOT_RUN, "Record persona is " + persona + ", not Minor - Minor-only row skipped");
			} else if ("LE".equals(code) && persona != DvsPersona.LEGAL_ENTITY) {
				r.result(DvsRow.NOT_RUN, "Record persona is " + persona + ", not Legal Entity - LE row skipped");
			} else if ("IND".equals(code) && persona == DvsPersona.LEGAL_ENTITY) {
				r.result(DvsRow.NOT_RUN, "Record is a Legal Entity - Individual row skipped");
			}
		}
	}

	private static void blockCustomer(List<DvsRow> rows, String reason) {
		for (DvsRow r : rows) {
			if (r.status().isEmpty()) {
				r.result(DvsRow.BLOCKED, reason);
			}
		}
	}

	/** Groups ordered P0 first (then first appearance); rows inside a group by Group_Seq. */
	static List<List<DvsRow>> orderedGroups(List<DvsRow> rows) {
		Map<String, List<DvsRow>> byGroup = new LinkedHashMap<>();
		int n = 0;
		for (DvsRow r : rows) {
			String g = r.groupId().isEmpty() ? "NOGROUP-" + (n++) : r.groupId();
			byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(r);
		}
		List<List<DvsRow>> groups = new ArrayList<>(byGroup.values());
		for (List<DvsRow> g : groups) {
			g.sort(Comparator.comparingInt(DvsRow::groupSeq));
		}
		groups.sort(Comparator.comparingInt((List<DvsRow> g) -> g.stream().mapToInt(DvsRow::priorityRank).min().orElse(2)));
		return groups;
	}

	private List<DvsRow> load(Path data, List<String> sheets, Filter f) {
		List<DvsRow> out = new ArrayList<>();
		Pattern glob = f.groupGlob().isEmpty() ? null
				: Pattern.compile("^" + Pattern.quote(f.groupGlob()).replace("*", "\\E.*\\Q") + "$", Pattern.CASE_INSENSITIVE);
		for (String sheet : sheets) {
			for (DvsRow r : DvsExcelReader.readSheet(data, sheet)) {
				if (!f.modules().isEmpty() && !f.modules().contains(r.moduleCode())) {
					continue;
				}
				if (!f.priorities().isEmpty() && !f.priorities().contains(r.priority().toUpperCase(Locale.ROOT))) {
					continue;
				}
				if (glob != null && !glob.matcher(r.groupId()).matches()) {
					continue;
				}
				if (!f.tc().isEmpty() && !f.tc().equalsIgnoreCase(r.tcId())) {
					continue;
				}
				out.add(r);
			}
		}
		return out;
	}

	private static List<DvsRow> rows(List<DvsRow> all, String module) {
		List<DvsRow> out = new ArrayList<>();
		for (DvsRow r : all) {
			if (r.moduleCode().equals(module)) {
				out.add(r);
			}
		}
		return out;
	}

	private static List<DvsRow> concat(List<DvsRow> a, List<DvsRow> b) {
		List<DvsRow> out = new ArrayList<>(a);
		out.addAll(b);
		return out;
	}

	/**
	 * Customer ids to try: config lists (dvs.cust.*), else the CONFIG sheet (cust.*), and only when both are empty the
	 * ids written in the data rows (those can belong to another persona, so they are never mixed in).
	 */
	static List<String> customersFor(String module, List<DvsRow> pos, List<DvsRow> neg) {
		String listKey = "LE".equals(module) ? "dvs.cust.le" : "MIN".equals(module) ? "dvs.cust.minor" : "dvs.cust.individual";
		String sheetKey = "LE".equals(module) ? "cust.le" : "MIN".equals(module) ? "cust.minor" : "cust.individual";
		Set<String> ids = new LinkedHashSet<>(DvsConfig.list(listKey));
		ids.addAll(DvsConfig.list(sheetKey));
		if (ids.isEmpty()) {
			for (List<DvsRow> list : List.of(pos, neg)) {
				for (DvsRow r : list) {
					if (!r.customerId().isEmpty()) {
						ids.add(r.customerId());
					}
				}
			}
		}
		return new ArrayList<>(ids);
	}

	private void abort(String reason) {
		if (!aborted) {
			aborted = true;
			abortReason = reason;
			logger.error("DVS run aborted: {}", reason);
		}
	}

	private static void blockRemaining(List<DvsRow> rows, String reason) {
		for (DvsRow r : rows) {
			if (r.status().isEmpty()) {
				r.result(DvsRow.BLOCKED, reason);
			}
		}
	}

	private static Outcome summarize(Path file, List<DvsRow> rows, int bugs) {
		int[] c = new int[6];
		for (DvsRow r : rows) {
			switch (r.status()) {
				case DvsRow.PASS -> c[0]++;
				case DvsRow.FAIL -> c[1]++;
				case DvsRow.BLOCKED -> c[2]++;
				case DvsRow.CAPTURED -> c[3]++;
				case DvsRow.PLANNED -> c[4]++;
				default -> c[5]++;
			}
		}
		return new Outcome(file, rows.size(), c[0], c[1], c[2], c[3], c[5], c[4], bugs);
	}

	private static Set<String> csv(String v) {
		Set<String> out = new LinkedHashSet<>();
		if (v != null) {
			Arrays.stream(v.split(",")).map(s -> s.trim().toUpperCase(Locale.ROOT)).filter(s -> !s.isEmpty()).forEach(out::add);
		}
		return out;
	}

	private static String format(Duration d) {
		return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
	}
}
