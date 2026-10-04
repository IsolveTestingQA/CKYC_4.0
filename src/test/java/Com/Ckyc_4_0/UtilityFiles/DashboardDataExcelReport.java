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
import java.util.List;
import java.util.Locale;

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
