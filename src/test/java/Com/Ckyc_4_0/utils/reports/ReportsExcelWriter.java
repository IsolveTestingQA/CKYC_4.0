/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.reports;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Workbook for the dashboard report data and the Reports-module comparison. Everything is keyed by the customer id
 * column, which is found by name because the columns change per process.
 *
 * Sheets: SUMMARY, COMPARE (one line per dashboard sub-card vs report, with links to both tables' JSON), DIFFS (one
 * line per difference, naming the customer and column), TABLES (index of every table read, with JSON link),
 * BY_CUSTOMER (one line per customer per report: all that customer's headers and values), ALL_VALUES (every
 * header / value with its customer id) and one sheet per report with its own dynamic headers (D_ = dashboard
 * sub-card, R_ = Reports module).
 *
 * Whatever does not fit a cell (over 32,000 characters) or the ALL_VALUES sheet is written to a JSON file in the
 * "json" folder next to the workbook, and the cell holds a clickable link to it. Every table is also saved whole as JSON.
 */
public final class ReportsExcelWriter {

	private static final int CELL_LIMIT = 32_000;
	private static final int ALL_VALUES_LIMIT = 1_000_000;

	private final Path file;
	private final Path jsonDir;
	private final XSSFWorkbook wb = new XSSFWorkbook();
	private final CreationHelper helper = wb.getCreationHelper();
	private final CellStyle head;
	private final CellStyle pass;
	private final CellStyle fail;
	private final CellStyle skip;
	private final CellStyle link;
	private final Sheet summary;
	private final Sheet compare;
	private final Sheet diffs;
	private final Sheet tablesIndex;
	private final Sheet columnDiffs;
	private int columnRow = 1;
	private final Sheet byCustomer;
	private final Sheet allValues;
	private final Map<String, String> info = new LinkedHashMap<>();
	private final Map<String, Integer> statusCount = new LinkedHashMap<>();
	private final List<String> usedNames = new ArrayList<>();
	private final List<String> usedJson = new ArrayList<>();
	private final Map<ReportTable, String> jsonOf = new IdentityHashMap<>();
	private int compareRow = 1;
	private int diffRow = 1;
	private int indexRow = 1;
	private int customerRow = 1;
	private int allRow = 1;
	private int jsonFiles;
	private boolean allTruncated;
	private boolean customerTruncated;
	private final List<ReportsHtmlReport.Case> cases = new ArrayList<>();
	private int caseSeq;

	public ReportsExcelWriter(Path file) {
		this.file = file.toAbsolutePath();
		this.jsonDir = this.file.getParent().resolve("json");
		head = style(IndexedColors.GREY_25_PERCENT, true);
		pass = style(IndexedColors.LIGHT_GREEN, false);
		fail = style(IndexedColors.ROSE, false);
		skip = style(IndexedColors.LIGHT_YELLOW, false);
		link = wb.createCellStyle();
		Font lf = wb.createFont();
		lf.setUnderline(Font.U_SINGLE);
		lf.setColor(IndexedColors.BLUE.getIndex());
		link.setFont(lf);
		summary = wb.createSheet("SUMMARY");
		compare = wb.createSheet("COMPARE");
		diffs = wb.createSheet("DIFFS");
		columnDiffs = wb.createSheet("COLUMN_DIFFS");
		tablesIndex = wb.createSheet("TABLES");
		byCustomer = wb.createSheet("BY_CUSTOMER");
		allValues = wb.createSheet("ALL_VALUES");
		header(compare, "Application", "Constitution", "Dashboard sub-card", "Report category", "Report type", "Dashboard count",
				"Report count", "Rows read (dashboard / report)", "Status", "Actual result", "Notes", "Dashboard JSON",
				"Report JSON", "Evidence screenshot", "Dashboard date range", "Report date range");
		header(diffs, "Application", "Constitution", "Dashboard sub-card", "Report type", "Kind", "Customer ID", "Column",
				"Dashboard value", "Report value");
		header(columnDiffs, "Application", "Constitution", "Dashboard sub-card", "Report category", "Report type", "Where the extra column is",
				"Column", "Counted as");
		header(tablesIndex, "Source", "Application", "Constitution", "Category", "Report type", "Code", "Declared count",
				"Rows read", "Customer id column", "Status", "Sheet", "JSON", "Date range");
		header(byCustomer, "Customer ID", "Source", "Application", "Constitution", "Category", "Report type", "Code", "Details");
		header(allValues, "Source", "Application", "Constitution", "Category", "Report type", "Code", "Customer ID", "Row No",
				"Header", "Value");
	}

	public void info(String key, String value) {
		info.put(key, value);
	}

	// ------------------------------------------------------------------ tables

	private static final class SheetState {
		final Sheet sheet;
		final List<String> headers = new ArrayList<>();
		int nextRow = 1;

		SheetState(Sheet sheet) {
			this.sheet = sheet;
		}
	}

	private static final int FIXED_COLS = 4;
	private final Map<String, SheetState> tableSheets = new LinkedHashMap<>();

	/**
	 * Adds the table to its report's sheet (one sheet per sub-card / report type, every filter combination appended
	 * with Application / Constitution columns), to TABLES, BY_CUSTOMER and ALL_VALUES, and saves it whole as JSON.
	 * A header that did not exist yet becomes a new column, so a new report column needs no code change.
	 */
	public void addTable(ReportTable t) {
		String prefix = ReportTable.DASHBOARD.equals(t.source()) ? "D_" : "R_";
		String base = prefix + (t.code().isEmpty() ? "" : t.code() + "_")
				+ (t.reportType().isEmpty() ? t.category() : t.reportType());
		SheetState st = tableSheets.get(base.toLowerCase(Locale.ROOT));
		if (st == null) {
			st = new SheetState(wb.createSheet(uniqueName(base)));
			tableSheets.put(base.toLowerCase(Locale.ROOT), st);
			header(st.sheet, "Application", "Constitution", "Category", "Report type");
		}
		String jsonName = writeJson(jsonName(t), JsonText.table(t));
		jsonOf.put(t, jsonName);
		int keyIdx = t.keyColumnIndex();

		Row ix = tablesIndex.createRow(indexRow++);
		String[] iv = { t.source(), t.application(), t.constitution(), t.category(), t.reportType(), t.code(),
				t.declaredCount() == null ? "" : String.valueOf(t.declaredCount()), String.valueOf(t.rows().size()),
				keyIdx < 0 ? (t.rows().isEmpty() ? "" : "NOT FOUND") : t.headers().get(keyIdx),
				t.isNotExecuted() ? "NOT EXECUTED: " + t.notExecutedReason() : "OK", st.sheet.getSheetName() };
		for (int c = 0; c < iv.length; c++) {
			ix.createCell(c).setCellValue(cut(iv[c]));
		}
		linkCell(ix.createCell(iv.length), jsonName, "View JSON");
		ix.createCell(iv.length + 1).setCellValue(t.dateRange());

		if (t.isNotExecuted()) {
			Row r = st.sheet.createRow(st.nextRow++);
			fixedCols(r, t);
			r.createCell(FIXED_COLS).setCellValue("NOT EXECUTED: " + cut(t.notExecutedReason()));
			return;
		}
		if (t.isCountOnly()) {
			Row r = st.sheet.createRow(st.nextRow++);
			fixedCols(r, t);
			r.createCell(FIXED_COLS).setCellValue("COUNT ONLY: sub-card number " + t.declaredCount()
					+ " (this sub-card opens another page - not clicked, no rows)");
			return;
		}
		int[] col = new int[t.headers().size()];
		for (int c = 0; c < t.headers().size(); c++) {
			String h = t.headers().get(c);
			int idx = st.headers.indexOf(h);
			if (idx < 0) {
				st.headers.add(h);
				idx = st.headers.size() - 1;
				Row hr = st.sheet.getRow(0);
				hr.createCell(FIXED_COLS + idx).setCellValue(h);
				hr.getCell(FIXED_COLS + idx).setCellStyle(head);
				st.sheet.setColumnWidth(FIXED_COLS + idx, Math.min(40, Math.max(14, h.length() + 4)) * 256);
			}
			col[c] = FIXED_COLS + idx;
		}
		if (t.rows().isEmpty()) {
			Row r = st.sheet.createRow(st.nextRow++);
			fixedCols(r, t);
			r.createCell(FIXED_COLS).setCellValue("0 records");
		}
		int rowNo = 1;
		for (List<String> values : t.rows()) {
			Row r = st.sheet.createRow(st.nextRow++);
			fixedCols(r, t);
			String customer = keyIdx >= 0 && keyIdx < values.size() ? values.get(keyIdx) : "";
			StringBuilder details = new StringBuilder();
			for (int c = 0; c < t.headers().size(); c++) {
				String v = c < values.size() ? values.get(c) : "";
				setText(r.createCell(col[c]), v, t.reportType() + " row " + rowNo + " / " + t.headers().get(c));
				addValue(t, customer, rowNo, t.headers().get(c), v);
				if (c > 0) {
					details.append(" | ");
				}
				details.append(t.headers().get(c)).append(": ").append(v);
			}
			addCustomer(t, customer, rowNo, details.toString());
			rowNo++;
		}
	}

	private void fixedCols(Row r, ReportTable t) {
		r.createCell(0).setCellValue(t.application());
		r.createCell(1).setCellValue(t.constitution());
		r.createCell(2).setCellValue(t.category());
		r.createCell(3).setCellValue(t.reportType());
	}

	private void addValue(ReportTable t, String customer, int rowNo, String header, String value) {
		if (allRow >= ALL_VALUES_LIMIT) {
			allTruncated = true;
			return;
		}
		Row r = allValues.createRow(allRow++);
		String[] v = { t.source(), t.application(), t.constitution(), t.category(), t.reportType(), t.code(), customer,
				String.valueOf(rowNo), header };
		for (int c = 0; c < v.length; c++) {
			r.createCell(c).setCellValue(cut(v[c]));
		}
		setText(r.createCell(v.length), value, t.reportType() + " row " + rowNo + " / " + header);
	}

	private void addCustomer(ReportTable t, String customer, int rowNo, String details) {
		if (customerRow >= ALL_VALUES_LIMIT) {
			customerTruncated = true;
			return;
		}
		Row r = byCustomer.createRow(customerRow++);
		String[] v = { customer.isEmpty() ? "(row " + rowNo + " - no customer id column)" : customer, t.source(),
				t.application(), t.constitution(), t.category(), t.reportType(), t.code() };
		for (int c = 0; c < v.length; c++) {
			r.createCell(c).setCellValue(cut(v[c]));
		}
		setText(r.createCell(v.length), details, t.reportType() + " customer " + customer);
	}

	// ------------------------------------------------------------------ comparison

	/** A screenshot belonging to a case (label shown in the HTML report, absolute file path). */
	public record Evidence(String label, String path) {
	}

	public void addComparison(String subCard, ReportTable dash, ReportTable rep, ReportComparer.Result res) {
		addComparison(subCard, dash, rep, res, List.of());
	}

	public void addComparison(String subCard, ReportTable dash, ReportTable rep, ReportComparer.Result res, String evidence) {
		addComparison(subCard, dash, rep, res,
				evidence == null || evidence.isEmpty() ? List.<Evidence>of() : List.of(new Evidence(evidence, "")));
	}

	public void addComparison(String subCard, ReportTable dash, ReportTable rep, ReportComparer.Result res,
			List<Evidence> evidenceList) {
		String evidence = evidenceList.stream().map(e -> e.path().isEmpty() ? e.label() : fileNameOf(e.path()))
				.collect(java.util.stream.Collectors.joining(" | "));
		addCase(subCard, dash, rep, res, evidenceList);
		ReportTable ref = dash != null ? dash : rep;
		String app = ref == null ? "" : ref.application();
		String con = ref == null ? "" : ref.constitution();
		Row r = compare.createRow(compareRow++);
		String[] v = { app, con, subCard, rep == null ? (dash == null ? "" : dash.category()) : rep.category(),
				rep == null ? (dash == null ? "" : dash.reportType()) : rep.reportType(),
				count(dash), count(rep),
				(dash == null || dash.isNotExecuted() ? "-" : String.valueOf(dash.rows().size())) + " / "
						+ (rep == null || rep.isNotExecuted() ? "-" : String.valueOf(rep.rows().size())),
				res.status(), res.summary(), String.join("; ", res.notes()) };
		for (int c = 0; c < v.length; c++) {
			setText(r.createCell(c), v[c], "compare " + subCard);
		}
		CellStyle st = ReportComparer.PASS.equals(res.status()) ? pass
				: ReportComparer.FAIL.equals(res.status()) || ReportComparer.BUG.equals(res.status()) ? fail : skip;
		r.getCell(8).setCellStyle(st);
		if (dash != null && jsonOf.get(dash) != null) {
			linkCell(r.createCell(11), jsonOf.get(dash), "Dashboard JSON");
		}
		if (rep != null && jsonOf.get(rep) != null) {
			linkCell(r.createCell(12), jsonOf.get(rep), "Report JSON");
		}
		r.createCell(13).setCellValue(evidence == null ? "" : evidence);
		r.createCell(14).setCellValue(dash == null ? "" : dash.dateRange());
		r.createCell(15).setCellValue(rep == null ? "" : rep.dateRange());
		statusCount.merge(res.status(), 1, Integer::sum);
		for (ReportComparer.Diff d : res.diffs()) {
			if (d.kind().startsWith("COLUMN-")) {
				Row cr = columnDiffs.createRow(columnRow++);
				String[] cv = { app, con, subCard, rep == null ? (dash == null ? "" : dash.category()) : rep.category(),
						rep == null ? (dash == null ? "" : dash.reportType()) : rep.reportType(),
						d.kind().equals("COLUMN-ONLY-IN-REPORT") ? "Only in the Reports module (not on the dashboard popup)"
								: "Only on the dashboard popup (not in the Reports module)",
						d.column(), ReportComparer.FAIL.equals(res.status()) ? "FAIL" : "Note only" };
				for (int c = 0; c < cv.length; c++) {
					cr.createCell(c).setCellValue(cut(cv[c]));
				}
				if (!ReportComparer.FAIL.equals(res.status())) {
					continue; // note-only: listed in COLUMN_DIFFS, not in the failures sheet
				}
			}
			Row dr = diffs.createRow(diffRow++);
			String[] dv = { app, con, subCard, rep == null ? "" : rep.reportType(), d.kind(), d.customer(), d.column() };
			for (int c = 0; c < dv.length; c++) {
				dr.createCell(c).setCellValue(cut(dv[c]));
			}
			setText(dr.createCell(7), d.dashboard(), "diff " + subCard + " " + d.customer() + " " + d.column());
			setText(dr.createCell(8), d.report(), "diff " + subCard + " " + d.customer() + " " + d.column());
		}
	}

	/** Saves what has been read so far (the workbook stays open) - a crash later never loses it. */
	public void checkpoint() {
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream out = Files.newOutputStream(file)) {
				wb.write(out);
			}
		} catch (IOException e) {
			// the final write reports problems
		}
	}

	private void addCase(String subCard, ReportTable dash, ReportTable rep, ReportComparer.Result res,
			List<Evidence> evidenceList) {
		ReportTable ref = dash != null ? dash : rep;
		String app = ref == null ? "" : ref.application();
		String con = ref == null ? "" : ref.constitution();
		String code = dash != null && !dash.code().isEmpty() ? dash.code() : "R" + (++caseSeq);
		String id = "RC-" + (app.toUpperCase(Locale.ROOT).startsWith("UPD") ? "UPD" : app.toUpperCase(Locale.ROOT).startsWith("NEW") ? "NEW" : "NA")
				+ "-" + (con.equalsIgnoreCase("ALL") ? "ALL" : con.toLowerCase(Locale.ROOT).startsWith("ind") ? "IND"
						: con.toLowerCase(Locale.ROOT).startsWith("legal") ? "LE" : "NA") + "-" + code;
		List<ReportsHtmlReport.Evidence> ev = new ArrayList<>();
		for (Evidence e : evidenceList) {
			if (!e.path().isEmpty()) {
				ev.add(new ReportsHtmlReport.Evidence(e.label(), relative(e.path())));
			}
		}
		cases.add(new ReportsHtmlReport.Case(id, app, con,
				ref == null ? "" : ref.category(), subCard,
				rep != null ? rep.reportType() : (dash != null ? dash.reportType() : ""),
				dash != null ? dash.reportType() : "",
				res.status(), res.summary(),
				dash == null ? "-" : dash.isNotExecuted() ? "n/e" : dash.declaredCount() == null ? "-" : String.valueOf(dash.declaredCount()),
				rep == null ? "-" : rep.isNotExecuted() ? "n/e" : rep.declaredCount() == null ? "-" : String.valueOf(rep.declaredCount()),
				dash == null ? "-" : dash.isCountOnly() ? "not opened (count only)" : String.valueOf(dash.rows().size()),
				rep == null ? "-" : String.valueOf(rep.rows().size()),
				dash == null ? "" : dash.dateRange(), rep == null ? "" : rep.dateRange(),
				dash == null ? List.of() : dash.headers(), rep == null ? List.of() : rep.headers(),
				res.notes(), res.diffs(), ev,
				dash == null ? "" : jsonOf.getOrDefault(dash, ""), rep == null ? "" : jsonOf.getOrDefault(rep, "")));
	}

	/** Path of a screenshot relative to the workbook / HTML folder, so the report can be moved with its folder. */
	private String relative(String path) {
		try {
			return file.getParent().relativize(java.nio.file.Paths.get(path).toAbsolutePath()).toString().replace('\\', '/');
		} catch (RuntimeException e) {
			return java.nio.file.Paths.get(path).toUri().toString();
		}
	}

	private static String fileNameOf(String path) {
		int i = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
		return i >= 0 ? path.substring(i + 1) : path;
	}

	public void write() throws IOException {
		header(summary, "Item", "Value");
		int r = 1;
		info.put("Written", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
		info.put("JSON files", jsonFiles + " in " + jsonDir);
		for (Map.Entry<String, String> e : info.entrySet()) {
			Row row = summary.createRow(r++);
			row.createCell(0).setCellValue(e.getKey());
			row.createCell(1).setCellValue(cut(e.getValue()));
		}
		for (Map.Entry<String, Integer> e : statusCount.entrySet()) {
			Row row = summary.createRow(r++);
			row.createCell(0).setCellValue("Comparisons " + e.getKey());
			row.createCell(1).setCellValue(e.getValue());
		}
		if (allTruncated || customerTruncated) {
			Row row = summary.createRow(r++);
			row.createCell(0).setCellValue("Sheet limit");
			row.createCell(1).setCellValue("ALL_VALUES / BY_CUSTOMER stopped at " + ALL_VALUES_LIMIT
					+ " lines - the TABLES sheet links every table's complete JSON");
		}
		for (Sheet s : List.of(summary, compare, diffs, tablesIndex, columnDiffs)) {
			for (int c = 0; c < 16; c++) {
				s.setColumnWidth(c, 26 * 256);
			}
		}
		byCustomer.setColumnWidth(0, 22 * 256);
		byCustomer.setColumnWidth(7, 80 * 256);
		Files.createDirectories(file.getParent());
		try (OutputStream out = Files.newOutputStream(file)) {
			wb.write(out);
		}
		wb.close();
		writeHtml();
	}

	private void writeHtml() {
		try {
			String name = file.getFileName().toString();
			Path html = file.getParent().resolve(name.replaceAll("\\.xlsx$", "") + ".html");
			Map<String, String> dashRanges = new LinkedHashMap<>();
			Map<String, String> repRanges = new LinkedHashMap<>();
			for (Map.Entry<String, String> e : info.entrySet()) {
				String k = e.getKey();
				if (k.startsWith("Dashboard date range (") && k.endsWith(")")) {
					dashRanges.put(k.substring("Dashboard date range (".length(), k.length() - 1), e.getValue());
				} else if (k.startsWith("Reports date range (") && k.endsWith(")")) {
					repRanges.put(k.substring("Reports date range (".length(), k.length() - 1), e.getValue());
				}
			}
			ReportsHtmlReport.write(html, name, info.getOrDefault("Written", ""), info, info.getOrDefault("STOPPED", ""),
					cases, dashRanges, repRanges);
		} catch (IOException | RuntimeException e) {
			// the workbook is the primary result - a problem here must not fail the run
		}
	}

	public Path file() {
		return file;
	}

	public Map<String, Integer> statusCount() {
		return statusCount;
	}

	// ------------------------------------------------------------------ helpers

	/** Text into a cell; over 32,000 characters it goes to a JSON file and the cell holds a link to it. */
	private void setText(Cell cell, String value, String context) {
		String v = value == null ? "" : value;
		if (v.length() <= CELL_LIMIT) {
			cell.setCellValue(v);
			return;
		}
		String name = writeJson(uniqueJson("cell_" + context), JsonText.largeValue(context, v));
		linkCell(cell, name, "Too large for a cell (" + v.length() + " chars) - click to view JSON");
	}

	private void linkCell(Cell cell, String jsonRelative, String text) {
		cell.setCellValue(text);
		Hyperlink h = helper.createHyperlink(HyperlinkType.FILE);
		h.setAddress(jsonRelative);
		cell.setHyperlink(h);
		cell.setCellStyle(link);
	}

	/** @return the path relative to the workbook ("json/name.json") */
	private String writeJson(String fileName, String content) {
		try {
			Files.createDirectories(jsonDir);
			Files.writeString(jsonDir.resolve(fileName), content, StandardCharsets.UTF_8);
			jsonFiles++;
		} catch (IOException e) {
			return "json/" + fileName + " (could not be written: " + e.getMessage() + ")";
		}
		return "json/" + fileName;
	}

	private String jsonName(ReportTable t) {
		return uniqueJson(t.source() + "_" + t.code() + "_" + t.reportType() + "_" + t.application() + "_" + t.constitution());
	}

	private String uniqueJson(String raw) {
		String s = raw.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("_+", "_");
		if (s.length() > 110) {
			s = s.substring(0, 110);
		}
		String name = s + ".json";
		int n = 2;
		while (usedJson.contains(name.toLowerCase(Locale.ROOT))) {
			name = s + "_" + n++ + ".json";
		}
		usedJson.add(name.toLowerCase(Locale.ROOT));
		return name;
	}

	private static String count(ReportTable t) {
		return t == null || t.isNotExecuted() || t.declaredCount() == null ? "-" : String.valueOf(t.declaredCount());
	}

	private String uniqueName(String raw) {
		String s = raw.replaceAll("[\\\\/?*\\[\\]:]", "_").replaceAll("\\s+", " ").trim();
		if (s.length() > 31) {
			s = s.substring(0, 31);
		}
		String name = s;
		int n = 2;
		while (usedNames.contains(name.toLowerCase(Locale.ROOT)) || wb.getSheet(name) != null) {
			String suffix = "~" + n++;
			name = s.substring(0, Math.min(s.length(), 31 - suffix.length())) + suffix;
		}
		usedNames.add(name.toLowerCase(Locale.ROOT));
		return name;
	}

	private static String cut(String s) {
		if (s == null) {
			return "";
		}
		return s.length() > CELL_LIMIT ? s.substring(0, CELL_LIMIT) : s;
	}

	private void header(Sheet s, String... cols) {
		Row r = s.createRow(0);
		for (int i = 0; i < cols.length; i++) {
			r.createCell(i).setCellValue(cols[i]);
			r.getCell(i).setCellStyle(head);
			s.setColumnWidth(i, Math.min(40, Math.max(14, cols[i].length() + 4)) * 256);
		}
		s.createFreezePane(0, 1);
	}

	private CellStyle style(IndexedColors color, boolean bold) {
		CellStyle st = wb.createCellStyle();
		st.setFillForegroundColor(color.getIndex());
		st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		if (bold) {
			Font f = wb.createFont();
			f.setBold(true);
			st.setFont(f);
		}
		return st;
	}
}
