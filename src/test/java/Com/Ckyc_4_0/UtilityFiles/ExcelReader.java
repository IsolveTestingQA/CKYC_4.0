/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExcelReader {

	// ------------------- SHEET DETAILS -------------------

	public String getSheetName(int index, String filePath) throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = WorkbookFactory.create(fis)) {
			return workbook.getSheetName(index);
		}
	}

	public int getSheetCount(String filePath) throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = WorkbookFactory.create(fis)) {
			return workbook.getNumberOfSheets();
		}
	}

	public int totalRowCount(String sheetName, String filePath) throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheet(sheetName);
			return sheet.getLastRowNum();
		}
	}

	public int totalRowCountWithHeader(String sheetName, String filePath) throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheet(sheetName);
			return sheet.getLastRowNum() + 1;
		}
	}

	// ------------------- READ SINGLE CELL -------------------

	public static String readCell(String filePath, int sheetIndex, int rowNum, int colNum) throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheetAt(sheetIndex);
			Row row = sheet.getRow(rowNum);
			if (row == null)
				return "";
			return getCellValueAsString(row.getCell(colNum));
		}
	}

	// ------------------- READ ONE ROW AS KEY-VALUE -------------------

	public HashMap<String, String> readRowData(String filePath, String sheetName, int rowNum) throws Exception {
		HashMap<String, String> values = new HashMap<>();

		try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			XSSFSheet sheet = workbook.getSheet(sheetName);
			Row headerRow = sheet.getRow(0);
			Row dataRow = sheet.getRow(rowNum);

			if (dataRow == null)
				throw new Exception("Row " + rowNum + " is empty or missing.");

			int lastCol = headerRow.getLastCellNum();
			for (int col = 0; col < lastCol; col++) {
				String key = getCellValueAsString(headerRow.getCell(col));
				String value = getCellValueAsString(dataRow.getCell(col));
				values.put(key, value);
			}
		}
		return values;
	}

	// ------------------- READ MULTIPLE ROW DATA -------------------

	public HashMap<String, String> readMultipleData(String filePath, String sheetName, int rowNum) throws Exception {
		HashMap<String, String> values = new HashMap<>();

		try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			XSSFSheet sheet = workbook.getSheet(sheetName);
			Row headerRow = sheet.getRow(0);
			if (headerRow == null)
				throw new Exception("Header row is missing in sheet: " + sheetName);

			Row dataRow = sheet.getRow(rowNum);
			if (dataRow == null)
				throw new Exception("Row " + rowNum + " is empty or missing.");

			int lastColumn = headerRow.getLastCellNum();
			for (int col = 0; col < lastColumn; col++) {
				String key = getCellValueAsString(headerRow.getCell(col));
				String value = getCellValueAsString(dataRow.getCell(col));
				values.put(key, value);
			}
		}
		return values;
	}

	// ------------------- READ ALL DATA FROM SHEET -------------------

	public List<HashMap<String, String>> readAllData(String filePath, String sheetName) throws IOException {
		List<HashMap<String, String>> allData = new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			XSSFSheet sheet = workbook.getSheet(sheetName);
			Row headerRow = sheet.getRow(0);

			if (headerRow == null)
				throw new IOException("Header row missing in sheet: " + sheetName);

			int lastRow = sheet.getLastRowNum();
			int lastCol = headerRow.getLastCellNum();

			for (int i = 1; i <= lastRow; i++) {
				Row dataRow = sheet.getRow(i);
				if (dataRow == null)
					continue;

				HashMap<String, String> rowData = new HashMap<>();
				for (int col = 0; col < lastCol; col++) {
					String key = getCellValueAsString(headerRow.getCell(col));
					String value = getCellValueAsString(dataRow.getCell(col));
					rowData.put(key, value);
				}
				allData.add(rowData);
			}
		}
		return allData;
	}

	// ------------------- WRITE/UPDATE A SINGLE CELL -------------------

	public static void updateCell(String filePath, String sheetName, int rowNum, int colNum, String value)
			throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheet(sheetName);
			Row row = sheet.getRow(rowNum);
			if (row == null)
				row = sheet.createRow(rowNum);
			Cell cell = row.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			cell.setCellValue(value);

			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				workbook.write(fos);
			}
		}
	}

	// ------------------- ENSURE REPORT HEADERS EXIST -------------------

	public static void ensureReportColumns(String filePath, String sheetName) throws IOException {
		String[] requiredCols = { "Feature", "Scenario", "Step", "Expected Result", "Actual Result", "Status",
				"Duration (mins)", "Timestamp", "Error" };

		try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null)
				sheet = workbook.createSheet(sheetName);

			Row header = sheet.getRow(0);
			if (header == null)
				header = sheet.createRow(0);

			List<String> existingCols = new ArrayList<>();
			for (int i = 0; i < header.getLastCellNum(); i++) {
				if (header.getCell(i) != null)
					existingCols.add(header.getCell(i).getStringCellValue().trim());
			}

			int cellIndex = header.getLastCellNum() == -1 ? 0 : header.getLastCellNum();
			for (String col : requiredCols) {
				if (!existingCols.contains(col)) {
					header.createCell(cellIndex++).setCellValue(col);
				}
			}

			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				workbook.write(fos);
			}
		}
	}

	// ------------------- CONVERT DURATION (seconds → minutes) -------------------

	public static void convertDurationToMinutes(String filePath, String sheetName, int durationColIndex)
			throws IOException {
		try (FileInputStream fis = new FileInputStream(filePath); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheet(sheetName);
			int lastRow = sheet.getLastRowNum();

			for (int i = 1; i <= lastRow; i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;
				Cell cell = row.getCell(durationColIndex);
				if (cell != null && cell.getCellType() == CellType.NUMERIC) {
					double seconds = cell.getNumericCellValue();
					double minutes = seconds / 60.0;
					cell.setCellValue(String.format("%.2f", minutes));
				}
			}

			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				workbook.write(fos);
			}
		}
	}

	// ------------------- PREPARE FOR CUCUMBER REPORT -------------------

	public static void prepareCucumberExcel(String filePath, String sheetName) throws IOException {
		ensureReportColumns(filePath, sheetName);
		System.out.println("✅ Excel verified/updated for Cucumber report structure.");
	}

	// ------------------- HELPER: CELL VALUE TO STRING -------------------

	private static String getCellValueAsString(Cell cell) {
		if (cell == null)
			return "";

		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue().trim();

		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
			} else {
				BigDecimal bd = new BigDecimal(cell.getNumericCellValue());
				return bd.stripTrailingZeros().toPlainString();
			}

		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());

		case FORMULA:
			try {
				switch (cell.getCachedFormulaResultType()) {
				case STRING:
					return cell.getStringCellValue().trim();
				case NUMERIC:
					BigDecimal bd = new BigDecimal(cell.getNumericCellValue());
					return bd.stripTrailingZeros().toPlainString();
				case BOOLEAN:
					return String.valueOf(cell.getBooleanCellValue());
				default:
					return "";
				}
			} catch (Exception e) {
				return cell.getCellFormula().trim();
			}

		default:
			return "";
		}
	}
}
