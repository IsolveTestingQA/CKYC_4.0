/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Com.Ckyc_4_0.utils.bulkimport.CsvTable;
import Com.Ckyc_4_0.utils.bulkimport.FieldMapper;

/** Everything one Bulk Import run learns, handed from step to step and written to the result workbook at the end. */
public final class BulkImportContext {

	public record InsertedRow(String record, String status, String ckycRef, String customerRef, String rpRef, String name,
			String remarks, String createdAt) {
		public boolean mainApplicant() {
			return !record.toLowerCase().contains("related");
		}
	}

	public record Bug(String area, String id, String what, String remarks) {
	}

	public record Poll(String time, int inProgress, int success, int failed, int duplicate, int unseen) {
	}

	public static Path csvFile;
	public static CsvTable csv;
	public static String persona = "IND"; // IND / LE (file name IN / LE; Minor rides the IN file)
	public static String fileName = "";
	public static int jobNo;
	public static boolean manualUpload;
	public static boolean cancelled;
	public static int total;
	public static int successCount;
	public static int failedCount;
	public static String absentColumnsWarning = "";
	public static String uploadToasts = "";
	public static String iflowStatusBefore = "";
	public static String iflowStatusAfter = "";
	public static String startedAt = "";
	public static String idColumn = "";

	public static final List<InsertedRow> inserted = new ArrayList<>();
	public static final List<InsertedRow> failedRows = new ArrayList<>();
	public static final Set<String> expectedIds = new LinkedHashSet<>();
	public static final Map<String, String> bucket = new LinkedHashMap<>();
	public static final Map<String, String> bucketName = new LinkedHashMap<>();
	public static final Map<String, String> bucketRegion = new LinkedHashMap<>();
	public static final Map<String, Long> seenAfterMs = new LinkedHashMap<>();
	public static final List<Poll> polls = new ArrayList<>();
	public static final List<Bug> bugs = new ArrayList<>();
	public static final Map<String, List<FieldMapper.Decision>> decisions = new LinkedHashMap<>();
	public static final List<FieldMapper.Link> usedLinks = new ArrayList<>();
	public static final Map<String, Integer> sectionCounts = new LinkedHashMap<>();
	public static String mappingNote = "";
	public static String draftMappingFile = "";

	private BulkImportContext() {
	}

	public static void reset() {
		csvFile = null;
		csv = null;
		persona = "IND";
		fileName = "";
		jobNo = 0;
		manualUpload = false;
		cancelled = false;
		total = 0;
		successCount = 0;
		failedCount = 0;
		absentColumnsWarning = "";
		uploadToasts = "";
		iflowStatusBefore = "";
		iflowStatusAfter = "";
		idColumn = "";
		startedAt = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").format(new java.util.Date());
		inserted.clear();
		failedRows.clear();
		expectedIds.clear();
		bucket.clear();
		bucketName.clear();
		bucketRegion.clear();
		seenAfterMs.clear();
		polls.clear();
		bugs.clear();
		decisions.clear();
		usedLinks.clear();
		sectionCounts.clear();
		mappingNote = "";
		draftMappingFile = "";
	}

	public static void bug(String area, String id, String what, String remarks) {
		bugs.add(new Bug(area, id == null ? "" : id, what, remarks == null ? "" : remarks));
	}
}
