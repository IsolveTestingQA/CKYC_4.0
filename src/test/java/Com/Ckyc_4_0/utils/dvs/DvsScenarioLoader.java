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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structured scenario files for the Individual and Legal Entity suites (DVS/IND_Scenarios.csv, DVS/LE_Scenarios.csv).
 * Columns: TC_ID, Group_ID, Priority, Type (POSITIVE / NEGATIVE), Tab, Locator_ID, Input_Value, Expect
 * (OK / ERROR / LIMIT / CAPTURE), Error_Contains, Setup (LOCATOR=value;LOCATOR=value), Scenario, Source_TC.
 * Each line becomes one {@link DvsRow} so the normal result workbook, step log and filters apply.
 */
public final class DvsScenarioLoader {

	public static final String SHEET_IND = "IND_SUITE";
	public static final String SHEET_LE = "LE_SUITE";

	private DvsScenarioLoader() {
	}

	/** @param type POSITIVE or NEGATIVE (blank = both) */
	public static List<DvsRow> load(String resource, String module, String type) {
		List<DvsRow> out = new ArrayList<>();
		try (InputStream in = DvsScenarioLoader.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Scenario file not found on the classpath: " + resource);
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			List<String> header = null;
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				List<String> cells = split(line);
				if (header == null) {
					header = cells;
					continue;
				}
				Map<String, String> v = new LinkedHashMap<>();
				for (int i = 0; i < header.size(); i++) {
					v.put(header.get(i), i < cells.size() ? cells.get(i) : "");
				}
				if (!type.isBlank() && !type.equalsIgnoreCase(v.getOrDefault("Type", ""))) {
					continue;
				}
				v.put("Module", module);
				v.put("Role", "Maker");
				v.put("Field", v.getOrDefault("Locator_ID", ""));
				String expect = v.getOrDefault("Expect", "");
				String contains = v.getOrDefault("Error_Contains", "");
				v.put("Expected_Result", expect + (contains.isEmpty() ? "" : " (" + contains + ")"));
				out.add(new DvsRow(module.toLowerCase(Locale.ROOT).startsWith("legal") ? SHEET_LE : SHEET_IND, v));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read " + resource, e);
		}
		return out;
	}

	/** RFC 4180 style split: quoted cells may contain commas and doubled quotes. */
	static List<String> split(String line) {
		List<String> cells = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (quoted) {
				if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					cur.append('"');
					i++;
				} else if (c == '"') {
					quoted = false;
				} else {
					cur.append(c);
				}
			} else if (c == '"') {
				quoted = true;
			} else if (c == ',') {
				cells.add(cur.toString());
				cur.setLength(0);
			} else {
				cur.append(c);
			}
		}
		cells.add(cur.toString());
		return cells;
	}
}
