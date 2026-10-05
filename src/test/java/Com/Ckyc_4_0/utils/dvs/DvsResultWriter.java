/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Every run writes its own new workbook (never overwrites): Summary, Results, Failed, Unmapped. */
public final class DvsResultWriter {

	private static final String[] STATUSES = { DvsRow.PASS, DvsRow.FAIL, DvsRow.BLOCKED, DvsRow.CAPTURED, DvsRow.PLANNED, DvsRow.NOT_RUN };

	private DvsResultWriter() {
	}

	public static String resultsRoot() {
		return new File(System.getProperty("user.dir"), "Report Output" + File.separator + "DVS Results").getAbsolutePath();
	}

	public static Path write(String mode, Map<String, String> info, List<DvsRow> rows) {
		LocalDateTime now = LocalDateTime.now();
		String day = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String ts = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		Path file = Paths.get(resultsRoot(), day, "DVS_" + ts + "_" + mode + ".xlsx");
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			CellStyle bold = wb.createCellStyle();
			Font f = wb.createFont();
			f.setBold(true);
			bold.setFont(f);

			summary(wb, bold, mode, info, rows);
			results(wb, bold, "Results", rows, false);
			results(wb, bold, "Failed", rows, true);
			unmapped(wb, bold, rows);

			Files.createDirectories(file.getParent());
			try (OutputStream out = Files.newOutputStream(file)) {
				wb.write(out);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot write DVS result file " + file, e);
		}
		return file;
	}

	private static void summary(XSSFWorkbook wb, CellStyle bold, String mode, Map<String, String> info, List<DvsRow> rows) {
		Sheet s = wb.createSheet("Summary");
		int r = 0;
		r = kv(s, bold, r, "DVS 2.0 automation result", "");
		r = kv(s, null, r, "Run mode", mode);
		for (Map.Entry<String, String> e : info.entrySet()) {
			r = kv(s, null, r, e.getKey(), e.getValue());
		}
		r++;
		Map<String, int[]> byModule = new TreeMap<>();
		Map<String, int[]> byPriority = new TreeMap<>();
		int[] grand = new int[STATUSES.length + 1];
		for (DvsRow row : rows) {
			int idx = statusIndex(row.status());
			int[] m = bucketOf(byModule, row.module());
			int[] p = bucketOf(byPriority, row.priority());
			for (int[] b : new int[][] { m, p, grand }) {
				b[0]++;
				b[1 + idx]++;
			}
		}
		r = header(s, bold, r, "By module");
		for (Map.Entry<String, int[]> e : byModule.entrySet()) {
			r = counts(s, r, e.getKey(), e.getValue());
		}
		r = counts(s, r, "ALL MODULES", grand);
		r++;
		r = header(s, bold, r, "By priority");
		for (Map.Entry<String, int[]> e : byPriority.entrySet()) {
			r = counts(s, r, e.getKey(), e.getValue());
		}
		s.setColumnWidth(0, 9000);
		s.setColumnWidth(1, 9000);
		for (int c = 2; c <= STATUSES.length + 1; c++) {
			s.setColumnWidth(c, 4200);
		}
	}

	private static int[] bucketOf(Map<String, int[]> map, String key) {
		return map.computeIfAbsent(key.isEmpty() ? "-" : key, k -> new int[STATUSES.length + 1]);
	}

	private static int statusIndex(String status) {
		for (int i = 0; i < STATUSES.length; i++) {
			if (STATUSES[i].equals(status)) {
				return i;
			}
		}
		return STATUSES.length - 1;
	}

	private static int kv(Sheet s, CellStyle style, int r, String k, String v) {
		Row row = s.createRow(r);
		row.createCell(0).setCellValue(k);
		row.createCell(1).setCellValue(v);
		if (style != null) {
			row.getCell(0).setCellStyle(style);
		}
		return r + 1;
	}

	private static int header(Sheet s, CellStyle bold, int r, String first) {
		Row row = s.createRow(r);
		row.createCell(0).setCellValue(first);
		row.createCell(1).setCellValue("Total");
		for (int i = 0; i < STATUSES.length; i++) {
			row.createCell(2 + i).setCellValue(STATUSES[i]);
		}
		for (int i = 0; i < STATUSES.length + 2; i++) {
			row.getCell(i).setCellStyle(bold);
		}
		return r + 1;
	}

	private static int counts(Sheet s, int r, String label, int[] c) {
		Row row = s.createRow(r);
		row.createCell(0).setCellValue(label);
		for (int i = 0; i < c.length; i++) {
			row.createCell(1 + i).setCellValue(c[i]);
		}
		return r + 1;
	}

	private static void results(XSSFWorkbook wb, CellStyle bold, String name, List<DvsRow> rows, boolean failedOnly) {
		Sheet s = wb.createSheet(name);
		String[] cols = { "TC_ID", "Sheet", "Module", "Customer_ID", "Priority", "Scenario", "Field", "Input_Value",
				"Expected_Result", "Status", "Actual_Result", "Locator_ID", "Screenshot_Path", "Group_ID" };
		Row h = s.createRow(0);
		for (int i = 0; i < cols.length; i++) {
			h.createCell(i).setCellValue(cols[i]);
			h.getCell(i).setCellStyle(bold);
		}
		int r = 1;
		for (DvsRow row : rows) {
			if (failedOnly && !DvsRow.FAIL.equals(row.status())) {
				continue;
			}
			Row x = s.createRow(r++);
			String[] v = { row.tcId(), row.sheet(), row.module(), row.customerId(), row.priority(), row.get("Scenario"),
					row.field(), row.inputValue(), row.expectedResult(), row.status(), row.actual(), row.locatorId(),
					row.screenshot(), row.groupId() };
			for (int i = 0; i < v.length; i++) {
				x.createCell(i).setCellValue(cap(v[i]));
			}
		}
		s.createFreezePane(0, 1);
		for (int i = 0; i < cols.length; i++) {
			s.setColumnWidth(i, i == 5 || i == 8 || i == 10 ? 12000 : 5000);
		}
	}

	private static void unmapped(XSSFWorkbook wb, CellStyle bold, List<DvsRow> rows) {
		Sheet s = wb.createSheet("Unmapped");
		Row h = s.createRow(0);
		String[] cols = { "TC_ID", "Module", "Field", "Input_Value", "Why it was not run" };
		for (int i = 0; i < cols.length; i++) {
			h.createCell(i).setCellValue(cols[i]);
			h.getCell(i).setCellStyle(bold);
		}
		int r = 1;
		for (DvsRow row : rows) {
			if (!DvsRow.NOT_RUN.equals(row.status())) {
				continue;
			}
			Row x = s.createRow(r++);
			x.createCell(0).setCellValue(row.tcId());
			x.createCell(1).setCellValue(row.module());
			x.createCell(2).setCellValue(cap(row.field()));
			x.createCell(3).setCellValue(cap(row.inputValue()));
			x.createCell(4).setCellValue(cap(row.actual()));
		}
		s.setColumnWidth(0, 5000);
		s.setColumnWidth(2, 7000);
		s.setColumnWidth(3, 9000);
		s.setColumnWidth(4, 16000);
		s.createFreezePane(0, 1);
	}

	private static String cap(String v) {
		if (v == null) {
			return "";
		}
		return v.length() > 32000 ? v.substring(0, 32000) : v;
	}
}
