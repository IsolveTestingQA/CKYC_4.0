/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.searchdownload;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One workbook per Search session: sheet "Searches" gets one line per search attempt (what was entered, what the
 * application accepted, how long it took, the outcome, the message, every result field and the screenshot as a
 * link). It is saved after every attempt, so closing the session or the browser never loses a result.
 */
final class SearchResultsExcel {

	static final String[] COLUMNS = { "No", "Time", "Mode", "Document type", "Entered", "Accepted by the field", "Format valid",
			"Search enabled", "Wait (s)", "Outcome", "Verdict", "Application message", "Issues found", "CKYC Number",
			"CKYC Reference ID", "Account Type", "KYC Date", "Updated Date", "Full Name", "Father / Spouse Full Name", "Age",
			"Mobile Number", "Customer photo", "Screenshot" };

	private final XSSFWorkbook wb = new XSSFWorkbook();
	private final Sheet sheet = wb.createSheet("Searches");
	private final Sheet summary = wb.createSheet("Summary");
	private final CreationHelper helper = wb.getCreationHelper();
	private final CellStyle head;
	private final CellStyle pass;
	private final CellStyle fail;
	private final CellStyle info;
	private final CellStyle link;
	private final Path file;
	private final Map<String, Integer> verdicts = new LinkedHashMap<>();
	private final Map<String, String> meta = new LinkedHashMap<>();
	private int rowNo = 1;

	SearchResultsExcel() {
		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		file = Paths.get("Report Output", "Search Results", LocalDate.now().toString(), "Search_Results_" + ts + ".xlsx")
				.toAbsolutePath();
		head = style(IndexedColors.GREY_25_PERCENT, true);
		pass = style(IndexedColors.LIGHT_GREEN, false);
		fail = style(IndexedColors.ROSE, false);
		info = style(IndexedColors.LIGHT_YELLOW, false);
		link = wb.createCellStyle();
		Font f = wb.createFont();
		f.setUnderline(Font.U_SINGLE);
		f.setColor(IndexedColors.BLUE.getIndex());
		link.setFont(f);
		Row h = sheet.createRow(0);
		for (int i = 0; i < COLUMNS.length; i++) {
			Cell c = h.createCell(i);
			c.setCellValue(COLUMNS[i]);
			c.setCellStyle(head);
			sheet.setColumnWidth(i, (i == 11 || i == 12 ? 48 : 20) * 256);
		}
		sheet.createFreezePane(0, 1);
	}

	Path file() {
		return file;
	}

	void meta(String key, String value) {
		meta.put(key, value);
	}

	/** @param values keyed by the COLUMNS names; "Screenshot" holds the image file path */
	void add(Map<String, String> values) {
		Row r = sheet.createRow(rowNo);
		for (int i = 0; i < COLUMNS.length; i++) {
			String col = COLUMNS[i];
			String v = col.equals("No") ? String.valueOf(rowNo) : values.getOrDefault(col, "");
			Cell c = r.createCell(i);
			if (col.equals("Screenshot") && !v.isBlank()) {
				c.setCellValue(Paths.get(v).getFileName().toString());
				Hyperlink h = helper.createHyperlink(HyperlinkType.FILE);
				h.setAddress(Paths.get(v).toUri().toString());
				c.setHyperlink(h);
				c.setCellStyle(link);
			} else {
				c.setCellValue(v.length() > 32000 ? v.substring(0, 32000) : v);
			}
			if (col.equals("Verdict")) {
				c.setCellStyle("PASS".equals(v) ? pass : "FAIL".equals(v) ? fail : info);
			}
		}
		verdicts.merge(values.getOrDefault("Verdict", ""), 1, Integer::sum);
		rowNo++;
		save();
	}

	int count() {
		return rowNo - 1;
	}

	Map<String, Integer> verdicts() {
		return verdicts;
	}

	void save() {
		try {
			Files.createDirectories(file.getParent());
			int r = 0;
			Row h = summary.getRow(0);
			if (h == null) {
				h = summary.createRow(0);
				h.createCell(0).setCellValue("Item");
				h.createCell(1).setCellValue("Value");
				h.getCell(0).setCellStyle(head);
				h.getCell(1).setCellStyle(head);
				summary.setColumnWidth(0, 34 * 256);
				summary.setColumnWidth(1, 90 * 256);
			}
			r = 1;
			for (Map.Entry<String, String> e : meta.entrySet()) {
				Row row = summary.getRow(r) != null ? summary.getRow(r) : summary.createRow(r);
				row.createCell(0).setCellValue(e.getKey());
				row.createCell(1).setCellValue(e.getValue());
				r++;
			}
			Row total = summary.getRow(r) != null ? summary.getRow(r) : summary.createRow(r);
			total.createCell(0).setCellValue("Searches done");
			total.createCell(1).setCellValue(count());
			r++;
			for (Map.Entry<String, Integer> e : verdicts.entrySet()) {
				Row row = summary.getRow(r) != null ? summary.getRow(r) : summary.createRow(r);
				row.createCell(0).setCellValue("Verdict " + (e.getKey().isEmpty() ? "(none)" : e.getKey()));
				row.createCell(1).setCellValue(e.getValue());
				r++;
			}
			try (OutputStream out = Files.newOutputStream(file)) {
				wb.write(out);
			}
		} catch (IOException e) {
			// the next attempt tries again
		}
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
