/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the DVS test-data workbook. Read-only: results go to a new file per run (DvsResultWriter). */
public final class DvsExcelReader {

	private static final DataFormatter FORMAT = new DataFormatter();

	private DvsExcelReader() {
	}

	public static Map<String, String> readConfig(Path file) {
		Map<String, String> out = new LinkedHashMap<>();
		try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
			Sheet sheet = wb.getSheet("CONFIG");
			if (sheet == null) {
				return out;
			}
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row r = sheet.getRow(i);
				if (r == null) {
					continue;
				}
				String k = text(r.getCell(0));
				if (!k.isEmpty()) {
					out.put(k, text(r.getCell(1)));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read DVS test data: " + file, e);
		}
		return out;
	}

	public static List<DvsRow> readSheet(Path file, String sheetName) {
		List<DvsRow> rows = new ArrayList<>();
		try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
			Sheet sheet = wb.getSheet(sheetName);
			if (sheet == null) {
				return rows;
			}
			Row header = sheet.getRow(0);
			if (header == null) {
				return rows;
			}
			int cols = header.getLastCellNum();
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row r = sheet.getRow(i);
				if (r == null) {
					continue;
				}
				Map<String, String> m = new LinkedHashMap<>();
				for (int c = 0; c < cols; c++) {
					String name = text(header.getCell(c));
					if (!name.isEmpty()) {
						m.put(name, text(r.getCell(c)));
					}
				}
				if (!m.getOrDefault("TC_ID", "").isEmpty()) {
					rows.add(new DvsRow(sheetName, m));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read DVS test data: " + file, e);
		}
		return rows;
	}

	private static String text(Cell cell) {
		return cell == null ? "" : FORMAT.formatCellValue(cell).trim();
	}
}
