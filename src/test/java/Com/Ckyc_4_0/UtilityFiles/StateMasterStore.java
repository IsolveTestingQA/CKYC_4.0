/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory State Master grid snapshot: code → record. */
public final class StateMasterStore {

	public record StateRecord(String code, String name, boolean active, int serialNo) {
	}

	private static final Map<String, StateRecord> BY_CODE = new LinkedHashMap<>();

	private StateMasterStore() {
	}

	public static void clear() {
		BY_CODE.clear();
	}

	public static void put(StateRecord record) {
		if (record == null || record.code() == null || record.code().isBlank()) {
			return;
		}
		BY_CODE.put(record.code().trim().toUpperCase(), record);
	}

	public static Map<String, StateRecord> asMap() {
		return Collections.unmodifiableMap(BY_CODE);
	}

	public static int size() {
		return BY_CODE.size();
	}

	public static StateRecord get(String code) {
		if (code == null) {
			return null;
		}
		return BY_CODE.get(code.trim().toUpperCase());
	}
}
