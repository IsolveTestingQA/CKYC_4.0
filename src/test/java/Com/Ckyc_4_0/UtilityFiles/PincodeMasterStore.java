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

/** Visible-page Pincode Master snapshot (full 18k load is not used). */
public final class PincodeMasterStore {

	public record PincodeRecord(String pincode, String district, String state, boolean active) {
	}

	private static final Map<String, PincodeRecord> BY_PIN = new LinkedHashMap<>();

	private PincodeMasterStore() {
	}

	public static void clear() {
		BY_PIN.clear();
	}

	public static void put(PincodeRecord record) {
		if (record == null || record.pincode() == null || record.pincode().isBlank()) {
			return;
		}
		BY_PIN.put(record.pincode().trim(), record);
	}

	public static Map<String, PincodeRecord> asMap() {
		return Collections.unmodifiableMap(BY_PIN);
	}

	public static List<PincodeRecord> values() {
		return new ArrayList<>(BY_PIN.values());
	}

	public static int size() {
		return BY_PIN.size();
	}
}
