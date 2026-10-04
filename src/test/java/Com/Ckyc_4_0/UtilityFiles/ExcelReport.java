/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Execution Excel report — one colourful sheet per feature/module (Login first).
 * Columns include Test Data, Toast, and failure Screenshot filenames (TC_ID based).
 */
public class ExcelReport {

	private static final String[] COLUMNS = { "S.No", "TC ID", "Module", "Scenario", "Step", "Expected Result",
			"Actual Result", "Status", "Test Data", "Toast Message", "Screenshot", "Timestamp", "Error" };

	/** Character widths (POI uses 1/256th of a character width). */
	private static final int[] COLUMN_WIDTHS = {
			8 * 256, // S.No
			12 * 256, // TC ID
			14 * 256, // Module
			40 * 256, // Scenario
			50 * 256, // Step
			30 * 256, // Expected
			42 * 256, // Actual
			12 * 256, // Status
			36 * 256, // Test Data
			36 * 256, // Toast
			40 * 256, // Screenshot
			20 * 256, // Timestamp
			28 * 256 // Error
	};

	private static final byte[] NAVY = hex("1F4E79");
	private static final byte[] ALT_ROW = hex("DEEBF7");
	private static final byte[] PASS_BG = hex("C6EFCE");
	private static final byte[] FAIL_BG = hex("FFC7CE");
	private static final byte[] FAIL_ROW = hex("FCE4EC");
	private static final byte[] WHITE = hex("FFFFFF");
	private static final byte[] TITLE_BG = hex("2E75B6");

	private static XSSFWorkbook workbook;
	private static String excelPath;
	@SuppressWarnings("unused")
	private static String reportMode;
	private static final Map<String, SheetState> sheetsByFeature = new LinkedHashMap<>();
	private static final ExcelReport instance = new ExcelReport();

	private CellStyle headerStyle;
	private CellStyle titleStyle;
	private CellStyle dataStyle;
	private CellStyle altDataStyle;
	private CellStyle passStatusStyle;
	private CellStyle failStatusStyle;
	private CellStyle failRowStyle;
	private CellStyle errorStyle;

	private static final class SheetState {
		final Sheet sheet;
		final String moduleName;
		final String tcPrefix;
		int nextRow;
		int nextSno = 1;
		int nextTc = 1;
		int passCount = 0;
		int failCount = 0;
		int otherCount = 0;

		SheetState(Sheet sheet, String moduleName, String tcPrefix, int nextRow) {
			this.sheet = sheet;
			this.moduleName = moduleName;
			this.tcPrefix = tcPrefix;
			this.nextRow = nextRow;
		}
	}

	private ExcelReport() {
		loadConfig();
		initExcel();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			saveExcel();
			closeWorkbook();
		}));
	}

	public static ExcelReport getInstance() {
		return instance;
	}

	public String getExcelPath() {
		return excelPath;
	}

	private void loadConfig() {
		String base = System.getProperty("user.dir");
		File configFile = resolveConfigFile(base);
		if (configFile != null && configFile.exists()) {
			try (InputStream input = new FileInputStream(configFile)) {
				Properties prop = new Properties();
				prop.load(input);
				String configured = prop.getProperty("excelReportPath");
				reportMode = prop.getProperty("excelReportMode", "append").trim().toLowerCase(Locale.ROOT);
				excelPath = ReportNamingHelper.excelReportPath(configured);
				if (excelPath == null || excelPath.isEmpty()) {
					setDefaults();
				}
			} catch (Exception e) {
				System.err.println("⚠️ Warning: Failed to read config.properties. Using default paths.");
				setDefaults();
			}
		} else {
			System.out.println("⚠️ config.properties not found. Using default paths.");
			setDefaults();
		}
	}

	private static File resolveConfigFile(String base) {
		File[] candidates = new File[] {
				new File(base + File.separator + "src" + File.separator + "test" + File.separator + "resources"
						+ File.separator + "config.properties"),
				new File(base + File.separator + "Configuration" + File.separator + "config.properties"),
				new File(base + File.separator + "config.properties") };
		for (File f : candidates) {
			if (f.exists()) {
				return f;
			}
		}
		return null;
	}

	private void setDefaults() {
		excelPath = ReportNamingHelper.excelReportPath(null);
		reportMode = "append";
	}

	private synchronized void initExcel() {
		try {
			File file = new File(excelPath);
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			workbook = new XSSFWorkbook();
			sheetsByFeature.clear();
			createStyles();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createStyles() {
		XSSFFont headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		headerFont.setFontHeightInPoints((short) 11);

		headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(rgb(NAVY));
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setAlignment(HorizontalAlignment.CENTER);
		headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		headerStyle.setWrapText(true);
		applyThinBorders(headerStyle);

		XSSFFont titleFont = workbook.createFont();
		titleFont.setBold(true);
		titleFont.setColor(IndexedColors.WHITE.getIndex());
		titleFont.setFontHeightInPoints((short) 13);

		titleStyle = workbook.createCellStyle();
		titleStyle.setFont(titleFont);
		titleStyle.setFillForegroundColor(rgb(TITLE_BG));
		titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		titleStyle.setAlignment(HorizontalAlignment.LEFT);
		titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		dataStyle = baseDataStyle(WHITE);
		altDataStyle = baseDataStyle(ALT_ROW);
		failRowStyle = baseDataStyle(FAIL_ROW);

		passStatusStyle = statusStyle(PASS_BG, IndexedColors.DARK_GREEN.getIndex());
		failStatusStyle = statusStyle(FAIL_BG, IndexedColors.DARK_RED.getIndex());

		errorStyle = workbook.createCellStyle();
		errorStyle.cloneStyleFrom(dataStyle);
		XSSFFont errorFont = workbook.createFont();
		errorFont.setColor(IndexedColors.DARK_RED.getIndex());
		errorStyle.setFont(errorFont);
		errorStyle.setWrapText(true);
	}

	private CellStyle baseDataStyle(byte[] bg) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(rgb(bg));
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setWrapText(true);
		applyThinBorders(style);
		return style;
	}

	private CellStyle statusStyle(byte[] bg, short fontColor) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(rgb(bg));
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setWrapText(true);
		applyThinBorders(style);
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setColor(fontColor);
		style.setFont(font);
		return style;
	}

	private static void applyThinBorders(CellStyle style) {
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
	}

	private static XSSFColor rgb(byte[] rgb) {
		return new XSSFColor(rgb, null);
	}

	private static byte[] hex(String hex) {
		int v = Integer.parseInt(hex, 16);
		return new byte[] { (byte) ((v >> 16) & 0xFF), (byte) ((v >> 8) & 0xFF), (byte) (v & 0xFF) };
	}

	private synchronized SheetState getOrCreateSheet(String feature) {
		if (workbook == null) {
			initExcel();
		}
		String sheetName = sanitizeSheetName(feature);
		SheetState existing = sheetsByFeature.get(sheetName);
		if (existing != null) {
			return existing;
		}

		Sheet sheet = workbook.createSheet(sheetName);
		applyFixedColumnWidths(sheet);

		int rowIdx = 0;
		Row titleRow = sheet.createRow(rowIdx++);
		titleRow.setHeightInPoints(24);
		Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("CKYC 4.0 Execution Report — " + sheetName);
		titleCell.setCellStyle(titleStyle);
		for (int i = 1; i < COLUMNS.length; i++) {
			Cell c = titleRow.createCell(i);
			c.setCellStyle(titleStyle);
		}
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COLUMNS.length - 1));

		Row header = sheet.createRow(rowIdx++);
		header.setHeightInPoints(22);
		for (int i = 0; i < COLUMNS.length; i++) {
			Cell cell = header.createCell(i);
			cell.setCellValue(COLUMNS[i]);
			cell.setCellStyle(headerStyle);
		}
		sheet.createFreezePane(0, 2);

		SheetState state = new SheetState(sheet, sheetName, tcPrefixForModule(sheetName), rowIdx);
		sheetsByFeature.put(sheetName, state);
		reorderSheetsLoginFirst();
		return state;
	}

	/** Dashboard → DB, Login → LG, else first 2 letters of module name. */
	static String tcPrefixForModule(String featureOrModule) {
		String raw = featureOrModule == null ? "" : featureOrModule.trim().toLowerCase(Locale.ROOT);
		if (raw.contains("login")) {
			return "LG";
		}
		if (raw.contains("dashboard")) {
			return "DB";
		}
		String cleaned = raw.replaceAll("^\\d+_", "").replaceAll("[^a-z0-9]", "");
		if (cleaned.length() >= 2) {
			return cleaned.substring(0, 2).toUpperCase(Locale.ROOT);
		}
		return "TC";
	}

	private void applyFixedColumnWidths(Sheet sheet) {
		for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
			sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
		}
	}

	private void reorderSheetsLoginFirst() {
		if (workbook == null || workbook.getNumberOfSheets() <= 1) {
			return;
		}
		List<String> names = new ArrayList<>();
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			names.add(workbook.getSheetName(i));
		}
		names.sort(Comparator
				.comparing((String n) -> !n.equalsIgnoreCase("Summary"))
				.thenComparing((String n) -> !n.toLowerCase(Locale.ROOT).contains("login"))
				.thenComparing(String::compareToIgnoreCase));
		for (int i = 0; i < names.size(); i++) {
			workbook.setSheetOrder(names.get(i), i);
		}
	}

	static String sanitizeSheetName(String feature) {
		String raw = (feature == null || feature.isBlank()) ? "General" : feature.trim();
		String cleaned = raw.replaceAll("[\\\\/?*\\[\\]:]", "_");
		if (cleaned.length() > 31) {
			cleaned = cleaned.substring(0, 31);
		}
		return cleaned;
	}

	/** Peeks next TC ID without consuming (for failure screenshot naming before write). */
	public synchronized String peekNextTcId(String feature) {
		SheetState state = getOrCreateSheet(feature);
		return String.format(Locale.ROOT, "%s_%03d", state.tcPrefix, state.nextTc);
	}

	public synchronized void logStep(String feature, String scenario, String step, String expected, String actual,
			String status, long startTimeMillis, String error) {
		logStepDetailed(feature, scenario, step, expected, actual, status, error, "", "", "");
	}

	/**
	 * Full Masters/Dashboard row including test data, toaster text, and failure screenshot file names.
	 *
	 * @return TC ID written for this row (e.g. ST_012)
	 */
	public synchronized String logStepDetailed(String feature, String scenario, String step, String expected,
			String actual, String status, String error, String testData, String toastMessage, String screenshotFiles) {
		try {
			SheetState state = getOrCreateSheet(feature);
			Sheet sheet = state.sheet;
			int rowIndex = state.nextRow++;
			Row row = sheet.createRow(rowIndex);
			row.setHeightInPoints(-1);

			boolean failed = status != null && status.equalsIgnoreCase("failed");
			boolean evenDataRow = ((rowIndex - 2) % 2 == 0);
			CellStyle rowStyle = failed ? failRowStyle : (evenDataRow ? dataStyle : altDataStyle);

			int sno = state.nextSno++;
			String tcId = String.format(Locale.ROOT, "%s_%03d", state.tcPrefix, state.nextTc++);
			if (failed) {
				state.failCount++;
			} else if (status != null && (status.equalsIgnoreCase("Skipped") || status.equalsIgnoreCase("Aborted"))) {
				state.otherCount++;
			} else {
				state.passCount++;
			}

			String actualOut = actual == null ? "" : actual;
			if (toastMessage != null && !toastMessage.isBlank()) {
				actualOut = actualOut + (actualOut.isBlank() ? "" : " | ") + "Toast: " + toastMessage;
			}

			writeCell(row, 0, String.valueOf(sno), rowStyle);
			writeCell(row, 1, tcId, rowStyle);
			writeCell(row, 2, state.moduleName, rowStyle);
			writeCell(row, 3, scenario, rowStyle);
			writeCell(row, 4, step, rowStyle);
			writeCell(row, 5, expected, rowStyle);
			writeCell(row, 6, actualOut, rowStyle);

			Cell statusCell = row.createCell(7);
			statusCell.setCellValue(status == null ? "" : status);
			statusCell.setCellStyle(failed ? failStatusStyle : passStatusStyle);

			writeCell(row, 8, blankDash(testData), rowStyle);
			writeCell(row, 9, blankDash(toastMessage), rowStyle);
			writeCell(row, 10, blankDash(screenshotFiles), rowStyle);
			writeCell(row, 11, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), rowStyle);

			String errorText = (error == null || error.isEmpty()) ? "-" : error;
			Cell errorCell = row.createCell(12);
			errorCell.setCellValue(errorText);
			errorCell.setCellStyle((!failed || "-".equals(errorText)) ? rowStyle : errorStyle);
			return tcId;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String blankDash(String value) {
		return (value == null || value.isBlank()) ? "-" : value;
	}

	private static void writeCell(Row row, int col, String value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value == null ? "" : value);
		cell.setCellStyle(style);
	}

	/**
	 * Writes (or rewrites) a "Summary" sheet — Passed/Failed/Other per module, grand totals, and
	 * Total Time Taken for the run. Always placed first by {@link #reorderSheetsLoginFirst()}.
	 */
	public synchronized void writeSummarySheet(String totalTimeTaken) {
		if (workbook == null || sheetsByFeature.isEmpty()) {
			return;
		}
		try {
			int existingIdx = workbook.getSheetIndex("Summary");
			if (existingIdx >= 0) {
				workbook.removeSheetAt(existingIdx);
			}
			Sheet sheet = workbook.createSheet("Summary");
			String[] cols = { "Module", "Total Checks", "Passed", "Failed", "Other (Skipped/Aborted)" };
			int[] widths = { 24 * 256, 14 * 256, 12 * 256, 12 * 256, 24 * 256 };
			for (int i = 0; i < widths.length; i++) {
				sheet.setColumnWidth(i, widths[i]);
			}

			int rowIdx = 0;
			Row titleRow = sheet.createRow(rowIdx++);
			titleRow.setHeightInPoints(24);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue("CKYC 4.0 Execution Summary");
			titleCell.setCellStyle(titleStyle);
			for (int i = 1; i < cols.length; i++) {
				Cell c = titleRow.createCell(i);
				c.setCellStyle(titleStyle);
			}
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, cols.length - 1));

			Row header = sheet.createRow(rowIdx++);
			header.setHeightInPoints(22);
			for (int i = 0; i < cols.length; i++) {
				Cell cell = header.createCell(i);
				cell.setCellValue(cols[i]);
				cell.setCellStyle(headerStyle);
			}
			sheet.createFreezePane(0, 2);

			int firstDataRow = rowIdx;
			int grandTotal = 0;
			int grandPass = 0;
			int grandFail = 0;
			int grandOther = 0;
			for (SheetState state : sheetsByFeature.values()) {
				int total = state.passCount + state.failCount + state.otherCount;
				if (total == 0) {
					continue;
				}
				Row row = sheet.createRow(rowIdx++);
				boolean evenRow = ((rowIdx - firstDataRow) % 2 == 0);
				CellStyle rowStyle = evenRow ? altDataStyle : dataStyle;
				writeCell(row, 0, state.moduleName, rowStyle);
				writeCell(row, 1, String.valueOf(total), rowStyle);
				writeCell(row, 2, String.valueOf(state.passCount), rowStyle);
				writeCell(row, 3, String.valueOf(state.failCount), rowStyle);
				writeCell(row, 4, String.valueOf(state.otherCount), rowStyle);
				grandTotal += total;
				grandPass += state.passCount;
				grandFail += state.failCount;
				grandOther += state.otherCount;
			}

			Row totalsRow = sheet.createRow(rowIdx++);
			writeCell(totalsRow, 0, "GRAND TOTAL", headerStyle);
			writeCell(totalsRow, 1, String.valueOf(grandTotal), headerStyle);
			writeCell(totalsRow, 2, String.valueOf(grandPass), headerStyle);
			writeCell(totalsRow, 3, String.valueOf(grandFail), headerStyle);
			writeCell(totalsRow, 4, String.valueOf(grandOther), headerStyle);

			rowIdx++;
			Row timeRow = sheet.createRow(rowIdx++);
			timeRow.setHeightInPoints(22);
			Cell timeLabel = timeRow.createCell(0);
			timeLabel.setCellValue("Total Time Taken");
			timeLabel.setCellStyle(titleStyle);
			Cell timeValue = timeRow.createCell(1);
			timeValue.setCellValue(totalTimeTaken == null || totalTimeTaken.isBlank() ? "-" : totalTimeTaken);
			timeValue.setCellStyle(titleStyle);
			for (int i = 2; i < cols.length; i++) {
				timeRow.createCell(i).setCellStyle(titleStyle);
			}
			sheet.addMergedRegion(new CellRangeAddress(timeRow.getRowNum(), timeRow.getRowNum(), 1, cols.length - 1));
		} catch (Exception e) {
			System.err.println("⚠️ Failed to write Summary sheet: " + e.getMessage());
		}
	}

	public synchronized void saveExcel() {
		if (workbook == null || sheetsByFeature.isEmpty()) {
			return;
		}
		try {
			reorderSheetsLoginFirst();
			File file = new File(excelPath);
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			try (FileOutputStream fos = new FileOutputStream(file)) {
				workbook.write(fos);
			}
			System.out.println("Excel report saved (per-module sheets): " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void closeWorkbook() {
		if (workbook != null) {
			try {
				workbook.close();
				workbook = null;
				sheetsByFeature.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
