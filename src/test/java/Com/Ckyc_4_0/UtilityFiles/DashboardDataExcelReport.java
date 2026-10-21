/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard UI data Excel — one row per title (summary card or sub-item) per filter combination.
 */
public final class DashboardDataExcelReport {

	private static final Logger logger = LoggerFactory.getLogger(DashboardDataExcelReport.class);
	private static final DashboardDataExcelReport INSTANCE = new DashboardDataExcelReport();

	private XSSFWorkbook workbook;
	private Sheet sheet;
	private int rowNum;
	private String excelPath;
	private CellStyle headerStyle;

	private DashboardDataExcelReport() {
		excelPath = ReportNamingHelper.dashboardDataExcelPath(ConfigReader.getExcelReportPath());
		initWorkbook();
	}

	public static DashboardDataExcelReport getInstance() {
		return INSTANCE;
	}

	public String getExcelPath() {
		return excelPath;
	}

	private void initWorkbook() {
		try {
			workbook = new XSSFWorkbook();
			sheet = workbook.createSheet("Dashboard UI Data");
			rowNum = 0;

			headerStyle = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBold(true);
			headerStyle.setFont(font);

			Row header = sheet.createRow(rowNum++);
			String[] columns = { "Run Timestamp", "Date Filter", "Constitution Filter", "From Date", "To Date",
					"Section", "Card Group", "Code", "Title (UI)", "Count", "Formula", "Captured At" };
			for (int i = 0; i < columns.length; i++) {
				Cell cell = header.createCell(i);
				cell.setCellValue(columns[i]);
				cell.setCellStyle(headerStyle);
			}
		} catch (Exception e) {
			logger.error("Dashboard Excel init failed: {}", e.getMessage(), e);
		}
	}

	public synchronized void appendRows(List<DashboardCountStore.DashboardDataRow> rows) {
		if (workbook == null || sheet == null) {
			initWorkbook();
		}
		if (rows == null || rows.isEmpty()) {
			return;
		}

		String runTs = ReportNamingHelper.runTimestamp();
		for (DashboardCountStore.DashboardDataRow row : rows) {
			Row excelRow = sheet.createRow(rowNum++);
			excelRow.createCell(0).setCellValue(runTs);
			excelRow.createCell(1).setCellValue(row.dateFilter());
			excelRow.createCell(2).setCellValue(row.constitutionFilter());
			excelRow.createCell(3).setCellValue(nullToEmpty(row.fromDate()));
			excelRow.createCell(4).setCellValue(nullToEmpty(row.toDate()));
			excelRow.createCell(5).setCellValue(row.section());
			excelRow.createCell(6).setCellValue(row.cardGroup());
			excelRow.createCell(7).setCellValue(row.code());
			excelRow.createCell(8).setCellValue(row.title());
			excelRow.createCell(9).setCellValue(row.count());
			excelRow.createCell(10).setCellValue(nullToEmpty(row.formula()));
			excelRow.createCell(11).setCellValue(nullToEmpty(row.capturedAt()));
		}
	}

	// ---------------------------------------------------------------- popup report tables (dynamic headers)

	private static final int REPORT_FIXED_COLS = 6;
	private static final String[] REPORT_FIXED = { "Run Timestamp", "Date Filter", "Constitution", "Application", "Code", "Sub-card" };

	private static final class ReportSheet {
		final Sheet sheet;
		final List<String> headers = new ArrayList<>();
		int nextRow = 1;

		ReportSheet(Sheet sheet) {
			this.sheet = sheet;
		}
	}

	private final Map<String, ReportSheet> reportSheets = new LinkedHashMap<>();
	private Sheet valuesSheet;
	private int valuesRow = 1;

	/**
	 * Stores one popup report completely: its own sheet per sub-card (every filter combination appended, headers as
	 * shown - a new column simply becomes a new column here) and one "Report_Values" sheet holding every value as
	 * Filter / Code / Sub-card / Row / Header / Value.
	 */
	public synchronized void addReportTable(String dateFilter, String constitution, String application, String code,
			String subCard, List<String> headers, List<List<String>> rows, String note) {
		if (workbook == null || sheet == null) {
			initWorkbook();
		}
		if (workbook == null) {
			return;
		}
		String runTs = ReportNamingHelper.runTimestamp();
		String name = ("R_" + code + "_" + subCard).replaceAll("[\\\\/?*\\[\\]:]", "_");
		if (name.length() > 31) {
			name = name.substring(0, 31);
		}
		ReportSheet rs = reportSheets.get(name);
		if (rs == null) {
			rs = new ReportSheet(workbook.createSheet(name));
			Row h = rs.sheet.createRow(0);
			for (int i = 0; i < REPORT_FIXED.length; i++) {
				Cell c = h.createCell(i);
				c.setCellValue(REPORT_FIXED[i]);
				c.setCellStyle(headerStyle);
				rs.sheet.setColumnWidth(i, 20 * 256);
			}
			reportSheets.put(name, rs);
		}
		int[] col = new int[headers.size()];
		for (int c = 0; c < headers.size(); c++) {
			int idx = rs.headers.indexOf(headers.get(c));
			if (idx < 0) {
				rs.headers.add(headers.get(c));
				idx = rs.headers.size() - 1;
				Cell hc = rs.sheet.getRow(0).createCell(REPORT_FIXED_COLS + idx);
				hc.setCellValue(headers.get(c));
				hc.setCellStyle(headerStyle);
				rs.sheet.setColumnWidth(REPORT_FIXED_COLS + idx, 24 * 256);
			}
			col[c] = REPORT_FIXED_COLS + idx;
		}
		if (valuesSheet == null) {
			valuesSheet = workbook.createSheet("Report_Values");
			String[] vh = { "Run Timestamp", "Date Filter", "Constitution", "Application", "Code", "Sub-card", "Customer ID", "Row No",
					"Header", "Value" };
			Row r = valuesSheet.createRow(0);
			for (int i = 0; i < vh.length; i++) {
				Cell c = r.createCell(i);
				c.setCellValue(vh[i]);
				c.setCellStyle(headerStyle);
				valuesSheet.setColumnWidth(i, 22 * 256);
			}
		}
		if (rows.isEmpty()) {
			Row r = rs.sheet.createRow(rs.nextRow++);
			fixed(r, runTs, dateFilter, constitution, application, code, subCard);
			r.createCell(REPORT_FIXED_COLS).setCellValue(note == null || note.isBlank() ? "0 records" : note);
			return;
		}
		int keyIdx = Com.Ckyc_4_0.utils.reports.ReportTable.keyIndex(headers);
		int rowNo = 1;
		for (List<String> values : rows) {
			Row r = rs.sheet.createRow(rs.nextRow++);
			fixed(r, runTs, dateFilter, constitution, application, code, subCard);
			String customer = keyIdx >= 0 && keyIdx < values.size() ? values.get(keyIdx) : "";
			for (int c = 0; c < headers.size(); c++) {
				String v = c < values.size() ? values.get(c) : "";
				setText(r.createCell(col[c]), v, code + " " + subCard + " row " + rowNo + " / " + headers.get(c));
				if (valuesRow < 1_000_000) {
					Row vr = valuesSheet.createRow(valuesRow++);
					fixed(vr, runTs, dateFilter, constitution, application, code, subCard);
					vr.createCell(6).setCellValue(customer);
					vr.createCell(7).setCellValue(rowNo);
					vr.createCell(8).setCellValue(headers.get(c));
					setText(vr.createCell(9), v, code + " " + subCard + " row " + rowNo + " / " + headers.get(c));
				}
			}
			rowNo++;
		}
	}

	private int jsonCount;

	/** Text into a cell; over 32,000 characters it is saved as JSON next to the workbook and the cell links to it. */
	private void setText(Cell cell, String value, String context) {
		String v = value == null ? "" : value;
		if (v.length() <= 32_000) {
			cell.setCellValue(v);
			return;
		}
		try {
			File dir = new File(new File(excelPath).getAbsoluteFile().getParentFile(), "json");
			dir.mkdirs();
			String name = "dashboard_cell_" + (++jsonCount) + ".json";
			java.nio.file.Files.writeString(new File(dir, name).toPath(),
					Com.Ckyc_4_0.utils.reports.JsonText.largeValue(context, v), java.nio.charset.StandardCharsets.UTF_8);
			cell.setCellValue("Too large for a cell (" + v.length() + " chars) - click to view JSON");
			org.apache.poi.ss.usermodel.Hyperlink h = workbook.getCreationHelper()
					.createHyperlink(org.apache.poi.common.usermodel.HyperlinkType.FILE);
			h.setAddress("json/" + name);
			cell.setHyperlink(h);
		} catch (IOException e) {
			cell.setCellValue(v.substring(0, 32_000));
			logger.warn("Large cell could not be written as JSON: {}", e.getMessage());
		}
	}

	private static void fixed(Row r, String ts, String date, String con, String app, String code, String sub) {
		String[] v = { ts, date, con, app, code, sub };
		for (int i = 0; i < v.length; i++) {
			r.createCell(i).setCellValue(v[i] == null ? "" : v[i]);
		}
	}

	public synchronized void save() {
		if (workbook == null || sheet == null) {
			return;
		}
		if (rowNum <= 1) {
			logger.info("Dashboard data Excel skipped — no data rows (header only)");
			return;
		}
		try {
			File file = new File(excelPath);
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			for (int i = 0; i < 12; i++) {
				sheet.autoSizeColumn(i);
			}
			try (FileOutputStream fos = new FileOutputStream(file)) {
				workbook.write(fos);
			}
			logger.info("Dashboard data Excel saved: {}", file.getAbsolutePath());
		} catch (IOException e) {
			logger.error("Failed to save dashboard data Excel: {}", e.getMessage(), e);
		}
	}

	public synchronized void close() {
		if (workbook != null) {
			try {
				workbook.close();
			} catch (IOException e) {
				logger.debug("Workbook close: {}", e.getMessage());
			}
			workbook = null;
			sheet = null;
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public static String nowTimestamp() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date());
	}
}
