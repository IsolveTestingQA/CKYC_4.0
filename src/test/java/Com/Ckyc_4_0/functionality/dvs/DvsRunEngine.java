/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsExcelReader;
import Com.Ckyc_4_0.utils.dvs.DvsResultWriter;
import Com.Ckyc_4_0.utils.dvs.DvsRow;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;

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
 * result workbook. With dvs.dryRun=true nothing touches the browser: it only plans and reports what is runnable.
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

	public record Outcome(Path resultFile, int total, int pass, int fail, int blocked, int captured, int notRun, int planned) {
	}

	private static final Logger logger = LoggerFactory.getLogger(DvsRunEngine.class);
	private static final List<String> POSITIVE_SHEETS = List.of("POSITIVE");
	private static final List<String> NEGATIVE_SHEETS = List.of("NEGATIVE", "BOUNDARY", "DEPENDENCY", "DOCUMENT_MAPPING", "IMAGE_MAPPING");
	private static final List<String> REGRESSION_SHEETS = List.of("REGRESSION");
	private static final List<String> MODULE_ORDER = List.of("IND", "MIN", "LE");

	private final Map<String, String> info = new LinkedHashMap<>();
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

		info.put("Started", start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
		info.put("Filter", filter.describe());
		info.put("Data file", data.toAbsolutePath().toString());
		info.put("Dry run", String.valueOf(dry));
		info.put("Restore mode", DvsConfig.get("restore.mode", "GROUP_END"));
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
		}
		blockRemaining(all, abortReason.isEmpty() ? "Not reached" : "Run stopped: " + abortReason);

		LocalDateTime end = LocalDateTime.now();
		info.put("Ended", end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
		info.put("Total time taken", format(Duration.between(start, end)));
		if (!abortReason.isEmpty()) {
			info.put("STOPPED", abortReason);
		}
		Path file = DvsResultWriter.write(mode.name(), info, all);
		logger.info("DVS result file: {}", file);
		return summarize(file, all);
	}

	private void runModule(Mode mode, String module, List<DvsRow> pos, List<DvsRow> neg, boolean dry) {
		if (pos.isEmpty() && neg.isEmpty()) {
			return;
		}
		String cust = customerFor(module, pos, neg);
		String prefix = "LE".equals(module) ? "LE" : "IND";
		info.put("Customer " + module, cust);
		DvsRowExecutor exec = new DvsRowExecutor(dry);
		exec.customer(cust);
		if (!dry) {
			DvsNavigation.selectModule(module);
			DvsNavigation.openCustomer(cust);
		}

		boolean p0Failed = false;
		DvsSnapshot snapshot = new DvsSnapshot();
		for (DvsRow row : pos) {
			exec.execute(row);
			exec.drainTouched();
			p0Failed |= DvsRow.FAIL.equals(row.status()) && row.priorityRank() == 0;
		}
		if (!pos.isEmpty() && !dry && !p0Failed) {
			snapshot = DvsRestoreManager.capture(prefix);
			snapshot.save(cust);
			info.put("Snapshot " + module, snapshot.values().size() + " fields saved");
		}

		if (neg.isEmpty()) {
			return;
		}
		if (mode == Mode.COMBINED && p0Failed) {
			for (DvsRow r : neg) {
				r.result(DvsRow.BLOCKED, "Positive P0 failed for " + module + " - negatives not run on a broken baseline");
			}
			return;
		}
		if (mode != Mode.COMBINED && !dry) {
			snapshot = DvsSnapshot.load(cust);
			if (snapshot.isEmpty()) {
				snapshot = DvsRestoreManager.capture(prefix);
				info.put("Snapshot " + module, "no saved last-positive file; current record values used as baseline");
			}
		}
		runGroups(neg, exec, snapshot, module, cust, dry);
	}

	private void runGroups(List<DvsRow> neg, DvsRowExecutor exec, DvsSnapshot snapshot, String module, String cust, boolean dry) {
		boolean rowMode = "ROW".equalsIgnoreCase(DvsConfig.get("restore.mode", "GROUP_END"));
		for (List<DvsRow> group : orderedGroups(neg)) {
			Set<String> groupTouched = new LinkedHashSet<>();
			for (int i = 0; i < group.size(); i++) {
				DvsRow row = group.get(i);
				exec.execute(row);
				Set<String> t = exec.drainTouched();
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
			if (!dry && !rowMode && !groupTouched.isEmpty() && !snapshot.isEmpty()) {
				if (!DvsRestoreManager.restoreGroupEnd(snapshot, groupTouched, module, cust)) {
					abort("Record " + cust + " could not be restored to last positive data after group "
							+ group.get(0).groupId() + " (irrecoverable)");
					return;
				}
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

	private static String customerFor(String module, List<DvsRow> pos, List<DvsRow> neg) {
		for (List<DvsRow> list : List.of(pos, neg)) {
			for (DvsRow r : list) {
				if (!r.customerId().isEmpty()) {
					return r.customerId();
				}
			}
		}
		String key = "LE".equals(module) ? "cust.le" : "MIN".equals(module) ? "cust.minor" : "cust.individual";
		return DvsConfig.get(key, "");
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

	private static Outcome summarize(Path file, List<DvsRow> rows) {
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
		return new Outcome(file, rows.size(), c[0], c[1], c[2], c[3], c[5], c[4]);
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
