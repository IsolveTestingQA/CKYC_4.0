/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory store for dashboard UI data — keyed counts plus title-based rows for reports / Current Data.
 */
public final class DashboardCountStore {

	private static final Map<String, Integer> counts = new LinkedHashMap<>();
	private static final Set<String> processedCombinations = new LinkedHashSet<>();
	private static final List<DashboardDataRow> dataRows = new ArrayList<>();

	private DashboardCountStore() {
	}

	public record DashboardDataRow(
			String dateFilter,
			String constitutionFilter,
			String fromDate,
			String toDate,
			String section,
			String cardGroup,
			String code,
			String title,
			int count,
			String formula,
			String capturedAt) {
	}

	public static void put(String key, int value) {
		counts.put(key, value);
	}

	public static Integer get(String key) {
		return counts.get(key);
	}

	public static Map<String, Integer> getAll() {
		return Collections.unmodifiableMap(counts);
	}

	public static List<DashboardDataRow> getDataRows() {
		return Collections.unmodifiableList(dataRows);
	}

	public static List<DashboardDataRow> getDataRowsForCombination(String dateFilter, String constitutionFilter) {
		return dataRows.stream()
				.filter(r -> r.dateFilter().equals(dateFilter) && r.constitutionFilter().equals(constitutionFilter))
				.toList();
	}

	public static void recordDataRow(DashboardDataRow row) {
		dataRows.add(row);
	}

	public static void recordCombination(String dateFilter, String constitutionFilter) {
		processedCombinations.add(dateFilter + "/" + constitutionFilter);
	}

	public static Set<String> getProcessedCombinations() {
		return Collections.unmodifiableSet(processedCombinations);
	}

	public static int getProcessedCombinationCount() {
		return processedCombinations.size();
	}

	public static List<String> findMissingCombinations(List<String> dateFilters, List<String> constitutionFilters) {
		List<String> missing = new ArrayList<>();
		for (String date : dateFilters) {
			for (String constitution : constitutionFilters) {
				String combo = date + "/" + constitution;
				if (!processedCombinations.contains(combo)) {
					missing.add(combo);
				}
			}
		}
		return missing;
	}

	public static void clear() {
		counts.clear();
		processedCombinations.clear();
		dataRows.clear();
	}

	public static String buildKey(String dateFilter, String constitutionFilter, String itemKey) {
		return dateFilter + "_" + constitutionFilter + "_" + itemKey;
	}
}
