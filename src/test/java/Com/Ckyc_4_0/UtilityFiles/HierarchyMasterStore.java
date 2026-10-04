/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory Hierarchy Master grid snapshots per tab. */
public final class HierarchyMasterStore {

	public enum Tab {
		FI, REGION, CPC, BRANCH
	}

	public record HierarchyRecord(Tab tab, String code, String name, String secondary, boolean active) {
	}

	private static final Map<String, HierarchyRecord> BY_KEY = new LinkedHashMap<>();

	private HierarchyMasterStore() {
	}

	private static String key(Tab tab, String code) {
		return tab.name() + ":" + (code == null ? "" : code.trim().toUpperCase());
	}

	public static void clear(Tab tab) {
		BY_KEY.entrySet().removeIf(e -> e.getKey().startsWith(tab.name() + ":"));
	}

	public static void clearAll() {
		BY_KEY.clear();
	}

	public static void put(HierarchyRecord record) {
		if (record == null || record.code() == null || record.code().isBlank()) {
			return;
		}
		BY_KEY.put(key(record.tab(), record.code()), record);
	}

	public static Map<String, HierarchyRecord> forTab(Tab tab) {
		Map<String, HierarchyRecord> out = new LinkedHashMap<>();
		String prefix = tab.name() + ":";
		for (Map.Entry<String, HierarchyRecord> e : BY_KEY.entrySet()) {
			if (e.getKey().startsWith(prefix)) {
				out.put(e.getKey(), e.getValue());
			}
		}
		return Collections.unmodifiableMap(out);
	}

	public static int size(Tab tab) {
		return forTab(tab).size();
	}

	public static HierarchyRecord first(Tab tab) {
		return forTab(tab).values().stream().findFirst().orElse(null);
	}
}
