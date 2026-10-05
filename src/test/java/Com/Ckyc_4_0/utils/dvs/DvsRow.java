/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import java.util.LinkedHashMap;
import java.util.Map;

/** One test-data row (any DVS data sheet) plus its run result. */
public final class DvsRow {

	public static final String PASS = "PASS";
	public static final String FAIL = "FAIL";
	public static final String BLOCKED = "BLOCKED";
	public static final String NOT_RUN = "NOT RUN";
	public static final String CAPTURED = "CAPTURED";
	public static final String PLANNED = "PLANNED";

	private final String sheet;
	private final Map<String, String> values = new LinkedHashMap<>();

	private String status = "";
	private String actual = "";
	private String screenshot = "";
	private String locatorId = "";

	public DvsRow(String sheet, Map<String, String> values) {
		this.sheet = sheet;
		this.values.putAll(values);
	}

	public String sheet() {
		return sheet;
	}

	public String get(String column) {
		String v = values.get(column);
		return v == null ? "" : v.trim();
	}

	public Map<String, String> values() {
		return values;
	}

	public String tcId() {
		return get("TC_ID");
	}

	public String module() {
		return get("Module");
	}

	/** IND / MIN / LE */
	public String moduleCode() {
		String m = module().toLowerCase();
		if (m.startsWith("legal")) {
			return "LE";
		}
		if (m.startsWith("minor")) {
			return "MIN";
		}
		return "IND";
	}

	public String customerId() {
		return get("Customer_ID");
	}

	public String field() {
		return get("Field");
	}

	public String inputValue() {
		return get("Input_Value");
	}

	public String connectedField() {
		return get("Connected_Field");
	}

	public String connectedValue() {
		return get("Connected_Value");
	}

	public String expectedResult() {
		return get("Expected_Result");
	}

	public String priority() {
		String p = get("Priority");
		return p.isEmpty() ? "P2" : p;
	}

	public String groupId() {
		return get("Group_ID");
	}

	public int groupSeq() {
		try {
			return (int) Double.parseDouble(get("Group_Seq"));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String restorePoint() {
		return get("Restore_Point");
	}

	public String status() {
		return status;
	}

	public String actual() {
		return actual;
	}

	public String screenshot() {
		return screenshot;
	}

	public String locatorId() {
		return locatorId;
	}

	public void result(String status, String actual) {
		this.status = status;
		this.actual = actual == null ? "" : actual;
	}

	public void screenshot(String path) {
		this.screenshot = path == null ? "" : path;
	}

	public void locatorId(String id) {
		this.locatorId = id == null ? "" : id;
	}

	public int priorityRank() {
		String p = priority().toUpperCase();
		return p.startsWith("P0") ? 0 : p.startsWith("P1") ? 1 : 2;
	}
}
