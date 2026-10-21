/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed values for rows whose Input_Value in the workbook is only a description (DVS/DVS_TypedValues.properties).
 * One line per TC_ID: {@code TC_ID = LOCATOR | value ; LOCATOR | value}. The last pair is the field the row judges,
 * the earlier pairs are set first.
 */
public final class DvsTypedValues {

	public record Pair(String locatorId, String value) {
	}

	private static final Map<String, List<Pair>> BY_TC = load();

	private DvsTypedValues() {
	}

	/** @return the pairs for this TC_ID, or an empty list when the row has no override. */
	public static List<Pair> get(String tcId) {
		return BY_TC.getOrDefault(tcId == null ? "" : tcId.trim(), List.of());
	}

	private static Map<String, List<Pair>> load() {
		Map<String, List<Pair>> map = new HashMap<>();
		try (InputStream in = DvsTypedValues.class.getResourceAsStream("/DVS/DVS_TypedValues.properties")) {
			if (in == null) {
				return map;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				String t = line.trim();
				int eq = t.indexOf('=');
				if (t.isEmpty() || t.startsWith("#") || eq < 0) {
					continue;
				}
				List<Pair> pairs = new ArrayList<>();
				for (String part : t.substring(eq + 1).split(";")) {
					int bar = part.indexOf('|');
					if (bar > 0) {
						pairs.add(new Pair(part.substring(0, bar).trim(), part.substring(bar + 1).trim()));
					}
				}
				if (!pairs.isEmpty()) {
					map.put(t.substring(0, eq).trim(), pairs);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load DVS/DVS_TypedValues.properties", e);
		}
		return map;
	}
}
