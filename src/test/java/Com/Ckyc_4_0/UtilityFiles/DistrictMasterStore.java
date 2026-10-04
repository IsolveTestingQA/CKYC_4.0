/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory District Master grid snapshot: name → record. */
public final class DistrictMasterStore {

	public record DistrictRecord(String state, String name, boolean active, int serialNo) {
	}

	private static final Map<String, DistrictRecord> BY_NAME = new LinkedHashMap<>();

	private DistrictMasterStore() {
	}

	public static void clear() {
		BY_NAME.clear();
	}

	public static void put(DistrictRecord record) {
		if (record == null || record.name() == null || record.name().isBlank()) {
			return;
		}
		BY_NAME.put(record.name().trim().toUpperCase(), record);
	}

	public static Map<String, DistrictRecord> asMap() {
		return Collections.unmodifiableMap(BY_NAME);
	}

	public static List<DistrictRecord> values() {
		return new ArrayList<>(BY_NAME.values());
	}

	public static int size() {
		return BY_NAME.size();
	}
}
