/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;
import Com.Ckyc_4_0.utils.bulkimport.FieldMapper;

/** The result workbook of one Bulk Import run: summary, ids, polls, comparison, bugs and the draft mapping. */
final class BulkImportWorkbook {

	private static final Logger logger = LoggerFactory.getLogger(BulkImportWorkbook.class);

	private BulkImportWorkbook() {
	}

	static Path write() {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Font bold = wb.createFont();
			bold.setBold(true);
			CellStyle head = wb.createCellStyle();
			head.setFont(bold);
			head.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
			head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			CellStyle bad = wb.createCellStyle();
			bad.setFillForegroundColor(IndexedColors.ROSE.getIndex());
			bad.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			CellStyle good = wb.createCellStyle();
			good.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			good.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			Sheet s = wb.createSheet("SUMMARY");
			String[][] kv = { { "Tested by", ConfigReader.testedBy() }, { "Started", BulkImportContext.startedAt },
					{ "Total time taken", ReportNamingHelper.elapsedFormatted() }, { "File", BulkImportContext.fileName },
					{ "Persona", BulkImportContext.persona }, { "Job", String.valueOf(BulkImportContext.jobNo) },
					{ "Upload", BulkImportContext.manualUpload ? "by hand (tester)" : "automation" }, { "Upload toasts", BulkImportContext.uploadToasts },
					{ "Absent columns warning", BulkImportContext.absentColumnsWarning }, { "Total rows", String.valueOf(BulkImportContext.total) },
					{ "SUCCESS", String.valueOf(BulkImportContext.successCount) }, { "FAILED", String.valueOf(BulkImportContext.failedCount) },
					{ "Expected customer ids", String.valueOf(BulkImportContext.expectedIds.size()) },
					{ "IFLOW status before", BulkImportContext.iflowStatusBefore }, { "IFLOW status after", BulkImportContext.iflowStatusAfter },
					{ "Customers compared", String.valueOf(BulkImportContext.decisions.size()) }, { "Mapping", BulkImportContext.mappingNote },
					{ "Draft mapping file", BulkImportContext.draftMappingFile }, { "Bugs", String.valueOf(BulkImportContext.bugs.size()) } };
			header(s, head, "Item", "Value");
			for (String[] r : kv) {
				row(s, r[0], r[1]);
			}
			s.setColumnWidth(0, 9000);
			s.setColumnWidth(1, 22000);

			Sheet ids = wb.createSheet("IDS");
			header(ids, head, "Customer ref", "Name (Bulk Import)", "Final list on Dashboard", "Seen after (s)", "Region");
			for (String id : BulkImportContext.expectedIds) {
				String b = BulkImportContext.bucket.get(id);
				Row r = row(ids, id, BulkImportContext.bucketName.getOrDefault(id, ""), b == null ? "MISSING" : b,
						String.valueOf(BulkImportContext.seenAfterMs.getOrDefault(id, 0L) / 1000), BulkImportContext.bucketRegion.getOrDefault(id, ""));
				r.getCell(2).setCellStyle(b == null || b.toLowerCase().contains("fail") || b.toLowerCase().contains("progress") ? bad : good);
			}

			Sheet polls = wb.createSheet("POLLS");
			header(polls, head, "Time", "In-Progress", "Success", "Failed", "Duplicate", "Not seen");
			for (BulkImportContext.Poll p : BulkImportContext.polls) {
				row(polls, p.time(), String.valueOf(p.inProgress()), String.valueOf(p.success()), String.valueOf(p.failed()), String.valueOf(p.duplicate()),
						String.valueOf(p.unseen()));
			}

			Sheet cmp = wb.createSheet("COMPARE");
			header(cmp, head, "Customer", "Tab", "Screen label", "CSV column(s)", "CSV value", "Screen value", "Rule", "Status", "Note", "XPath");
			for (Map.Entry<String, List<FieldMapper.Decision>> e : BulkImportContext.decisions.entrySet()) {
				for (FieldMapper.Decision d : e.getValue()) {
					Row r = row(cmp, e.getKey(), d.tab(), d.uiLabel(), d.csvCols(), d.csvValue(), d.uiValue(), d.rule(), d.status(), d.note(), d.xpath());
					if (d.bug()) {
						r.getCell(7).setCellStyle(bad);
					} else if (d.status().equals(FieldMapper.MATCH)) {
						r.getCell(7).setCellStyle(good);
					}
				}
			}
			for (int c : new int[] { 0, 1, 2, 3, 4, 5, 8 }) {
				cmp.setColumnWidth(c, 7000);
			}

			Sheet bugs = wb.createSheet("BUGS");
			header(bugs, head, "Area", "Id", "Bug", "Remarks");
			for (BulkImportContext.Bug b : BulkImportContext.bugs) {
				row(bugs, b.area(), b.id(), b.what(), b.remarks());
			}
			bugs.setColumnWidth(2, 14000);
			bugs.setColumnWidth(3, 16000);

			Sheet map = wb.createSheet("MAPPING");
			header(map, head, "CSV column(s)", "Tab", "Screen label", "Rule", "Confidence", "Note");
			for (FieldMapper.Link l : BulkImportContext.usedLinks) {
				row(map, String.join("+", l.csvCols()), l.tab(), l.uiLabel(), l.rule(), l.confidence(), l.note());
			}

			Sheet failed = wb.createSheet("FAILED_ROWS");
			header(failed, head, "Record", "Status", "Customer ref", "Name", "Remarks");
			for (BulkImportContext.InsertedRow f : BulkImportContext.failedRows) {
				row(failed, f.record(), f.status(), f.customerRef(), f.name(), f.remarks());
			}

			Path dir = Paths.get(ReportNamingHelper.reportOutputRoot(), "Bulk Import", ReportNamingHelper.runDate());
			Files.createDirectories(dir);
			Path file = dir.resolve("BulkImport_" + ReportNamingHelper.runTimestamp() + ".xlsx");
			try (OutputStream out = Files.newOutputStream(file)) {
				wb.write(out);
			}
			logger.info("Bulk Import workbook: {}", file.toAbsolutePath().toUri());
			return file;
		} catch (IOException | RuntimeException e) {
			logger.warn("Bulk Import workbook could not be written: {}", e.getMessage());
			return null;
		}
	}

	private static void header(Sheet s, CellStyle style, String... names) {
		Row r = s.createRow(0);
		for (int i = 0; i < names.length; i++) {
			Cell c = r.createCell(i);
			c.setCellValue(names[i]);
			c.setCellStyle(style);
		}
	}

	private static Row row(Sheet s, String... values) {
		Row r = s.createRow(s.getLastRowNum() + 1);
		for (int i = 0; i < values.length; i++) {
			String v = values[i] == null ? "" : values[i];
			r.createCell(i).setCellValue(v.length() > 32000 ? v.substring(0, 32000) : v);
		}
		return r;
	}
}
