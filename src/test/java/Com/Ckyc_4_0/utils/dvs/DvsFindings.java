/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import java.util.ArrayList;
import java.util.List;

/** Bugs and audit findings found during a run (written to the Bugs sheet of the result workbook). */
public final class DvsFindings {

	public record Finding(String kind, String tcId, String customerId, String tab, String field, String detail, String screenshot) {
	}

	public static final String BUG = "BUG";
	public static final String DATA_AUDIT = "DATA_AUDIT";

	private static final List<Finding> ALL = new ArrayList<>();

	private DvsFindings() {
	}

	public static synchronized void clear() {
		ALL.clear();
	}

	public static synchronized void add(String kind, String tcId, String customerId, String tab, String field, String detail,
			String screenshot) {
		ALL.add(new Finding(kind, nz(tcId), nz(customerId), nz(tab), nz(field), nz(detail), nz(screenshot)));
		DvsStepLog.step("finding").field(field).actual(kind + ": " + detail).result(DvsStepLog.FAIL).shot(screenshot).log();
	}

	public static synchronized List<Finding> all() {
		return new ArrayList<>(ALL);
	}

	private static String nz(String v) {
		return v == null ? "" : v;
	}
}
