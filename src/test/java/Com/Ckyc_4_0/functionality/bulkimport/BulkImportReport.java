/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.nio.file.Path;

import Com.Ckyc_4_0.utils.StepLog;

/** Writes the Bulk Import workbook once per run. */
public final class BulkImportReport {

	private static boolean published;

	private BulkImportReport() {
	}

	public static synchronized void publish() {
		Path p = BulkImportWorkbook.write();
		published = true;
		int bugs = BulkImportContext.bugs.size();
		StepLog.check("Bulk Import | Result", "Publish the result workbook", "Workbook with ids, polls, comparison and bugs",
				(p == null ? "FAIL | workbook not written" : p.toAbsolutePath().toUri().toString()) + " | bugs=" + bugs, p != null,
				"job=" + BulkImportContext.jobNo);
	}

	public static synchronized void publishIfNotYet() {
		if (!published && BulkImportContext.csv != null) {
			BulkImportWorkbook.write();
		}
		published = false;
	}
}
