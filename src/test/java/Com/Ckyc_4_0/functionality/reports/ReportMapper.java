/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pairs a dashboard sub-card with its Reports-module report type. The dashboard popup shows the report type name
 * itself, which is used first. Without a popup (count 0) the sub-card label is matched to the report types of its
 * category by normalised name ("Data Validation Queue" = "Data Validation Q"). Known spelling differences between
 * the two screens live in /Reports/ReportMap.properties ("category|dashboard label = report type").
 */
public final class ReportMapper {

	private static final Map<String, String> OVERRIDES = load();

	private ReportMapper() {
	}

	/** @return the report type text from the category's list, or null when nothing matches */
	public static String match(String category, String dashboardLabel, List<String> typesOfCategory) {
		String forced = OVERRIDES.get(norm(category) + "|" + norm(dashboardLabel));
		if (forced != null) {
			for (String t : typesOfCategory) {
				if (norm(t).equals(norm(forced))) {
					return t;
				}
			}
		}
		String want = nameKey(dashboardLabel);
		for (String t : typesOfCategory) {
			if (nameKey(typeName(category, t)).equals(want)) {
				return t;
			}
		}
		return null;
	}

	/** "Bulk Search - In Progress in iFlow" -> "Bulk Search"; "Data pushed from FI - Success" -> "Success". */
	static String typeName(String category, String type) {
		int dash = type.indexOf(" - ");
		if (dash < 0) {
			return type;
		}
		return "Data pushed from FI".equalsIgnoreCase(category) ? type.substring(dash + 3) : type.substring(0, dash);
	}

	static String nameKey(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT).replace("queue", "q").replaceAll("[^a-z0-9]+", "");
	}

	static String norm(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}

	private static Map<String, String> load() {
		Map<String, String> m = new HashMap<>();
		// the two screens spell this one differently ("Probable" vs "Praboble")
		m.put(norm("CERSAI In Progress") + "|" + norm("Probable match pending in FI"),
				"Praboble Match Pending with FI - In Progress in CERSAI");
		try (InputStream in = ReportMapper.class.getResourceAsStream("/Reports/ReportMap.properties")) {
			if (in == null) {
				return m;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				String t = line.trim();
				int eq = t.lastIndexOf('=');
				int bar = t.indexOf('|');
				if (t.isEmpty() || t.startsWith("#") || eq < 0 || bar < 0 || bar > eq) {
					continue;
				}
				m.put(norm(t.substring(0, bar)) + "|" + norm(t.substring(bar + 1, eq)), t.substring(eq + 1).trim());
			}
		} catch (IOException e) {
			return m;
		}
		return m;
	}
}
