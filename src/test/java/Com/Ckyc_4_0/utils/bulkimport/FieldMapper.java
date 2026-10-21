/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.bulkimport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links the columns of the uploaded CSV to the fields of the Customer Information screen and compares them.
 *
 * Nothing is hard-coded: the links are SUGGESTED from the real data of one customer (a CSV value equals a value on the
 * screen, a name is the join of prefix + first + last, a code decodes to the description shown, and the label looks like
 * the column / spec label) and then FROZEN in a mapping file that the tester reviews. A frozen mapping is looked up by
 * the label read from the page, so a re-ordered screen still works; a label that disappeared, a new label and a changed
 * field count are reported. The same rules decide the first draft and every later comparison.
 */
public final class FieldMapper {

	public static final String MATCH = "MATCH";
	public static final String MISMATCH = "MISMATCH";
	public static final String UNVERIFIED = "UNVERIFIED";
	public static final String MASKED = "MASKED";
	public static final String UI_EMPTY = "UI EMPTY";
	public static final String NOT_ON_UI = "NOT ON SCREEN";
	public static final String INFO = "INFO";

	/** One field read from the Customer Information screen. */
	public record UiField(String tab, String section, String label, String value, boolean masked, boolean revealed,
			String xpath) {
		public boolean empty() {
			String v = value == null ? "" : value.trim();
			return v.isEmpty() || v.equals("—") || v.equals("-");
		}
	}

	/** A link between a screen field and one or more CSV columns. rule = EQ | JOIN | DECODE | IGNORE. */
	public record Link(String tab, String uiLabel, List<String> csvCols, String rule, String confidence, String note) {
	}

	public record Decision(String tab, String uiLabel, String csvCols, String csvValue, String uiValue, String rule,
			String status, String note, String xpath) {
		public boolean bug() {
			return status.equals(MISMATCH) || status.equals(UI_EMPTY) || status.equals(NOT_ON_UI);
		}
	}

	/** code -> descriptions learned from real data (and from a frozen file). */
	public static final class CodeBook {
		private final Map<String, Map<String, Set<String>>> map = new TreeMap<>();

		public void learn(String column, String code, String ui) {
			map.computeIfAbsent(column.toUpperCase(Locale.ROOT), k -> new TreeMap<>())
					.computeIfAbsent(code.trim(), k -> new LinkedHashSet<>()).add(ui.trim());
		}

		Set<String> known(String column, String code) {
			Map<String, Set<String>> m = map.get(column.toUpperCase(Locale.ROOT));
			Set<String> s = m == null ? null : m.get(code.trim());
			return s == null ? Set.of() : s;
		}

		public int size() {
			return map.values().stream().mapToInt(Map::size).sum();
		}

		public void save(Path file) throws IOException {
			StringBuilder sb = new StringBuilder("csv_column|code|ui_value\n");
			map.forEach((col, codes) -> codes.forEach((code, uis) -> uis.forEach(u -> sb.append(col).append('|').append(code).append('|').append(u).append('\n'))));
			Files.createDirectories(file.getParent());
			Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
		}

		public static CodeBook load(Path file) {
			CodeBook b = new CodeBook();
			if (file == null || !Files.exists(file)) {
				return b;
			}
			try {
				for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
					String[] p = line.split("\\|", -1);
					if (p.length >= 3 && !p[0].equals("csv_column")) {
						b.learn(p[0], p[1], p[2]);
					}
				}
			} catch (IOException ignored) {
				// empty book
			}
			return b;
		}
	}

	private static final Pattern NAME_PART = Pattern.compile("^(.+?)_?(NAME_PREFIX|FIRST_NAME|MIDDLE_NAME|LAST_NAME)$", Pattern.CASE_INSENSITIVE);
	private static final List<String> NAME_ORDER = List.of("NAME_PREFIX", "FIRST_NAME", "MIDDLE_NAME", "LAST_NAME");
	private static final Pattern DEFAULT_IGNORE = Pattern.compile("(?i)^(.+_UPDT_FLG|.*FILE_NAME|APPL_TYPE|BRID|.*_IMG.*|IMG_.*)$");
	private static final Map<String, List<String>> SYN = new HashMap<>();
	private static final List<DateTimeFormatter> DATES = new ArrayList<>();

	static {
		SYN.put("res", List.of("residential", "residence"));
		SYN.put("acc", List.of("account"));
		SYN.put("const", List.of("constitution"));
		SYN.put("perm", List.of("permanent"));
		SYN.put("comm", List.of("correspondence"));
		SYN.put("add", List.of("address"));
		SYN.put("dist", List.of("district"));
		SYN.put("pin", List.of("pincode"));
		SYN.put("desgn", List.of("designation"));
		SYN.put("org", List.of("organisation", "organization"));
		SYN.put("appl", List.of("application"));
		SYN.put("decl", List.of("application", "declaration"));
		SYN.put("emp", List.of("employee"));
		SYN.put("fors", List.of("father", "spouse"));
		SYN.put("fatherorspouse", List.of("father", "spouse", "fatherspouse"));
		SYN.put("flg", List.of("flag"));
		SYN.put("poi", List.of("identity", "proof"));
		SYN.put("poa", List.of("address", "proof"));
		SYN.put("doc", List.of("document"));
		SYN.put("dt", List.of("date"));
		SYN.put("datetime", List.of("date"));
		SYN.put("cust", List.of("customer"));
		SYN.put("ref", List.of("reference"));
		SYN.put("email", List.of("mail"));
		SYN.put("mail", List.of("email"));
		SYN.put("verify", List.of("kyc"));
		SYN.put("dis", List.of("dis"));
		SYN.put("tin", List.of("tax", "identification"));
		SYN.put("fullname", List.of("name"));
		SYN.put("num", List.of("number", "no"));
		SYN.put("no", List.of("number", "num"));
		SYN.put("number", List.of("no", "num"));
		for (String p : new String[] { "dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MMM-yyyy", "dd.MM.yyyy", "ddMMyyyy" }) {
			DATES.add(DateTimeFormatter.ofPattern(p, Locale.ENGLISH));
		}
	}

	private final CkycSpec spec;
	private final CodeBook book;
	private final Set<String> ignore;

	public FieldMapper(CkycSpec spec, CodeBook book, Set<String> ignoreColumns) {
		this.spec = spec;
		this.book = book;
		this.ignore = new HashSet<>();
		if (ignoreColumns != null) {
			ignoreColumns.forEach(c -> this.ignore.add(c.toUpperCase(Locale.ROOT)));
		}
	}

	public CodeBook codeBook() {
		return book;
	}

	// ------------------------------------------------------------------ normalising

	public static String norm(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("​", "").replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
	}

	public static String normDate(String s) {
		String v = s == null ? "" : s.trim();
		if (v.length() < 8 || v.length() > 11) {
			return null;
		}
		for (DateTimeFormatter f : DATES) {
			try {
				return LocalDate.parse(v, f).toString();
			} catch (DateTimeParseException ignored) {
				// next pattern
			}
		}
		return null;
	}

	private static boolean sameValue(String a, String b) {
		String na = norm(a);
		String nb = norm(b);
		if (na.isEmpty() || nb.isEmpty()) {
			return false;
		}
		if (na.equals(nb)) {
			return true;
		}
		String da = normDate(a);
		return da != null && da.equals(normDate(b));
	}

	private boolean ignored(String col) {
		return ignore.contains(col.toUpperCase(Locale.ROOT)) || DEFAULT_IGNORE.matcher(col).matches();
	}

	// ------------------------------------------------------------------ name similarity

	static Set<String> tokens(String s) {
		Set<String> out = new LinkedHashSet<>();
		if (s == null) {
			return out;
		}
		String x = s.replaceAll("([a-z])([A-Z])", "$1 $2").replaceAll("[^A-Za-z0-9]+", " ").toLowerCase(Locale.ROOT).trim();
		for (String t : x.split(" ")) {
			if (!t.isBlank() && !t.equals("of") && !t.equals("the")) {
				out.add(t);
			}
		}
		return out;
	}

	private static Set<String> expand(Set<String> in) {
		Set<String> out = new LinkedHashSet<>(in);
		for (String t : in) {
			List<String> s = SYN.get(t);
			if (s != null) {
				out.addAll(s);
			}
		}
		return out;
	}

	private static String squash(String s) {
		return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
	}

	double nameScore(String uiLabel, String csvCol) {
		Set<String> raw = tokens(uiLabel);
		if (raw.isEmpty()) {
			return 0;
		}
		double byName = overlap(raw, tokens(csvCol));
		double byLabel = 0.85 * overlap(raw, tokens(spec.label(csvCol)));
		double score = Math.max(byName, byLabel);
		String su = squash(uiLabel);
		String sc = squash(csvCol);
		if (su.length() > 3 && sc.length() > 3 && (su.contains(sc) || sc.contains(su))) {
			score = Math.max(score, 0.8);
		}
		return score;
	}

	private static double overlap(Set<String> uiTokens, Set<String> colTokens) {
		if (colTokens.isEmpty()) {
			return 0;
		}
		Set<String> col = expand(colTokens);
		int hit = 0;
		for (String t : uiTokens) {
			boolean found = false;
			for (String x : expand(Set.of(t))) {
				found |= col.contains(x);
			}
			if (found) {
				hit++;
			}
		}
		return (double) hit / uiTokens.size();
	}

	// ------------------------------------------------------------------ suggest

	private static Map<String, List<String>> nameGroups(Iterable<String> headers) {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		for (String h : headers) {
			Matcher m = NAME_PART.matcher(h);
			if (m.matches()) {
				groups.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(h);
			}
		}
		return groups;
	}

	private static String joinName(List<String> cols, Map<String, String> csv) {
		List<String> parts = new ArrayList<>();
		for (String suffix : NAME_ORDER) {
			for (String c : cols) {
				if (c.toUpperCase(Locale.ROOT).endsWith(suffix)) {
					String v = csv.getOrDefault(c, "").trim();
					if (!v.isEmpty()) {
						parts.add(v);
					}
				}
			}
		}
		return String.join(" ", parts);
	}

	private record Cand(String rule, List<String> cols, double score, String note) {
	}

	/** Suggests a link for every screen field that has a recognisable CSV counterpart. */
	public List<Link> suggest(Map<String, String> csv, List<UiField> uiFields) {
		List<Link> links = new ArrayList<>();
		Map<String, List<String>> groups = nameGroups(csv.keySet());
		Set<String> done = new HashSet<>();
		for (UiField f : uiFields) {
			if (f.label() == null || f.label().isBlank() || !done.add(norm(f.tab()) + "|" + norm(f.label()))) {
				continue;
			}
			boolean hasValue = !f.empty() && !(f.masked() && !f.revealed());
			String uiVal = f.value();
			List<Cand> cands = new ArrayList<>();
			if (hasValue) {
				for (Map.Entry<String, List<String>> g : groups.entrySet()) {
					String joined = joinName(g.getValue(), csv);
					if (g.getValue().size() > 1 && !joined.isEmpty() && norm(joined).equals(norm(uiVal))) {
						cands.add(new Cand("JOIN", g.getValue(), 3.0 + nameScore(f.label(), g.getKey()), "prefix + first + middle + last"));
					}
				}
				for (Map.Entry<String, String> e : csv.entrySet()) {
					String col = e.getKey();
					if (e.getValue() == null || e.getValue().isBlank() || ignored(col)) {
						continue;
					}
					double ns = nameScore(f.label(), col);
					if (sameValue(e.getValue(), uiVal)) {
						double sc = 3.0 + ns;
						if (NAME_PART.matcher(col).matches() && !squash(f.label()).contains("name")) {
							sc -= 1.5; // a first / last name column is not the answer for a flag or a code
						}
						cands.add(new Cand("EQ", List.of(col), sc, ""));
					} else {
						String desc = spec.describe(col, e.getValue());
						boolean descEq = !desc.isEmpty() && norm(desc).equals(norm(uiVal));
						boolean codeLike = e.getValue().trim().length() <= 4 || !desc.isEmpty();
						if (codeLike && (ns >= 0.5 || (descEq && ns >= 0.25))) {
							double dsc = 1.5 + 1.5 * ns + (descEq ? 0.5 : 0);
							if (NAME_PART.matcher(col).matches() && !squash(f.label()).contains("name")) {
								dsc -= 0.5;
							}
							cands.add(new Cand("DECODE", List.of(col), dsc, descEq ? "spec master" : "learned"));
						} else if (!codeLike && ns >= 0.9) {
							cands.add(new Cand("EQ", List.of(col), 2.0 + ns, "same field by name, values differ"));
						}
					}
				}
			} else {
				for (Map.Entry<String, String> e : csv.entrySet()) {
					double ns = nameScore(f.label(), e.getKey());
					if (ns >= (f.masked() ? 0.6 : 0.85) && !e.getValue().isBlank() && !ignored(e.getKey())) {
						cands.add(new Cand("EQ", List.of(e.getKey()), 0.5 + ns, f.masked() ? "name only (masked)" : "name only (empty on screen)"));
					}
				}
			}
			if (cands.isEmpty()) {
				continue;
			}
			cands.sort((a, b) -> Double.compare(b.score(), a.score()));
			Cand best = cands.get(0);
			String conf = best.rule().equals("DECODE") ? (best.score() >= 3.0 ? "HIGH" : "MEDIUM")
					: best.score() >= 3.8 ? "HIGH" : best.score() >= 3.0 ? "MEDIUM" : "LOW";
			String note = best.note();
			if (cands.size() > 1 && Math.abs(cands.get(1).score() - best.score()) < 0.01
					&& !cands.get(1).cols().equals(best.cols())) {
				conf = "LOW";
				note = (note + " also matches " + String.join("+", cands.get(1).cols())).trim();
			}
			if (best.rule().equals("DECODE") && hasValue) {
				String code = csv.getOrDefault(best.cols().get(0), "");
				if (!code.isBlank()) {
					book.learn(best.cols().get(0), code, uiVal);
				}
			}
			links.add(new Link(f.tab(), f.label(), best.cols(), best.rule(), conf, note));
		}
		return links;
	}

	// ------------------------------------------------------------------ compare

	private String expected(Link l, Map<String, String> csv) {
		if (l.rule().equals("JOIN")) {
			return joinName(l.csvCols(), csv);
		}
		return csv.getOrDefault(l.csvCols().get(0), "");
	}

	/** Compares one customer: every screen field against its link, then the CSV columns nobody displays. */
	public List<Decision> compare(List<Link> links, Map<String, String> csv, List<UiField> uiFields) {
		List<Decision> out = new ArrayList<>();
		Map<String, Link> byLabel = new HashMap<>();
		for (Link l : links) {
			byLabel.putIfAbsent(norm(l.tab()) + "|" + norm(l.uiLabel()), l);
			byLabel.putIfAbsent("|" + norm(l.uiLabel()), l);
		}
		Set<String> seen = new HashSet<>();
		Set<String> covered = new HashSet<>();
		for (UiField f : uiFields) {
			Link l = byLabel.get(norm(f.tab()) + "|" + norm(f.label()));
			if (l == null) {
				l = byLabel.get("|" + norm(f.label()));
			}
			if (l != null) {
				seen.add(norm(l.uiLabel()));
			}
			if (l == null) {
				if (!f.empty()) {
					out.add(new Decision(f.tab(), f.label(), "", "", f.value(), "-", INFO,
							"screen field has no mapping (new field or not frozen yet)", f.xpath()));
				}
				continue;
			}
			if (l.rule().equals("IGNORE") || l.csvCols().isEmpty()) {
				continue;
			}
			l.csvCols().forEach(c -> covered.add(c.toUpperCase(Locale.ROOT)));
			out.add(decide(l, f, csv));
		}
		for (Link l : links) {
			if (!l.rule().equals("IGNORE") && !l.csvCols().isEmpty() && !seen.contains(norm(l.uiLabel()))) {
				out.add(new Decision(l.tab(), l.uiLabel(), String.join("+", l.csvCols()), expected(l, csv), "", l.rule(), NOT_ON_UI,
						"mapped field is not on the screen any more (label removed or renamed)", ""));
				l.csvCols().forEach(c -> covered.add(c.toUpperCase(Locale.ROOT)));
			}
		}
		for (Map.Entry<String, String> e : csv.entrySet()) {
			String col = e.getKey();
			if (e.getValue() == null || e.getValue().isBlank() || covered.contains(col.toUpperCase(Locale.ROOT)) || ignored(col)) {
				continue;
			}
			boolean mand = spec.mandatory(col);
			out.add(new Decision("", "", col, e.getValue(), "", "-", mand ? NOT_ON_UI : INFO,
					mand ? "mandatory CSV value is not shown on any Customer Information field" : "CSV value is not shown on screen (optional)", ""));
		}
		return out;
	}

	private Decision decide(Link l, UiField f, Map<String, String> csv) {
		String cols = String.join("+", l.csvCols());
		String exp = expected(l, csv);
		String ui = f.value() == null ? "" : f.value();
		if (f.masked() && !f.revealed()) {
			return new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MASKED, "value is masked and could not be revealed", f.xpath());
		}
		boolean csvEmpty = exp == null || exp.isBlank();
		if (csvEmpty) {
			return f.empty()
					? new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MATCH, "both empty", f.xpath())
					: new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MISMATCH, "CSV is empty but the screen shows a value", f.xpath());
		}
		if (f.empty()) {
			return new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), UI_EMPTY, "CSV has a value, the screen field is empty", f.xpath());
		}
		if (l.rule().equals("DECODE")) {
			String col = l.csvCols().get(0);
			Set<String> known = new LinkedHashSet<>(book.known(col, exp));
			String desc = spec.describe(col, exp);
			if (!desc.isEmpty()) {
				known.add(desc);
			}
			if (known.isEmpty()) {
				book.learn(col, exp, ui);
				return new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), UNVERIFIED, "new code '" + exp + "' = '" + ui + "' learned - freeze it after review", f.xpath());
			}
			for (String k : known) {
				if (norm(k).equals(norm(ui)) || norm(ui).startsWith(norm(k)) || norm(k).startsWith(norm(ui))) {
					return new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MATCH, "", f.xpath());
				}
			}
			return new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MISMATCH, "code '" + exp + "' should show " + known, f.xpath());
		}
		return sameValue(exp, ui)
				? new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MATCH, "", f.xpath())
				: new Decision(f.tab(), f.label(), cols, exp, ui, l.rule(), MISMATCH, "CSV '" + exp + "' but the screen shows '" + ui + "'", f.xpath());
	}

	// ------------------------------------------------------------------ mapping files

	public static void saveLinks(Path file, List<Link> links, Map<String, Integer> sectionCounts) throws IOException {
		StringBuilder sb = new StringBuilder("csv_columns|ui_tab|ui_label|rule|confidence|note\n");
		for (Link l : links) {
			sb.append(String.join("+", l.csvCols())).append('|').append(l.tab()).append('|').append(l.uiLabel()).append('|')
					.append(l.rule()).append('|').append(l.confidence()).append('|').append(l.note().replace('|', '/')).append('\n');
		}
		if (sectionCounts != null) {
			sectionCounts.forEach((s, n) -> sb.append("|").append(s).append("|").append(n).append("|COUNT|||\n"));
		}
		Files.createDirectories(file.getParent());
		Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
	}

	public static List<Link> loadLinks(Path file, Map<String, Integer> sectionCountsOut) {
		List<Link> out = new ArrayList<>();
		if (file == null || !Files.exists(file)) {
			return out;
		}
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				String[] p = line.split("\\|", -1);
				if (p.length < 6 || p[0].equals("csv_columns") && p[1].equals("ui_tab")) {
					continue;
				}
				if (p[3].equals("COUNT")) {
					if (sectionCountsOut != null) {
						sectionCountsOut.put(p[1], Integer.parseInt(p[2]));
					}
					continue;
				}
				List<String> cols = p[0].isBlank() ? List.of() : Arrays.asList(p[0].split("\\+"));
				out.add(new Link(p[1], p[2], cols, p[3], p[4], p[5]));
			}
		} catch (IOException | NumberFormatException ignored) {
			// partial mapping
		}
		return out;
	}
}
