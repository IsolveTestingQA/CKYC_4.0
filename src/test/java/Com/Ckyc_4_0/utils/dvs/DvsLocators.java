/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import org.openqa.selenium.By;

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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locator_ID -> XPath comes from DVS/dvs_locators.properties (edit one line when the UI changes).
 * Kind / Tab / Key come from DVS/dvs_locator_kinds.csv. Field display names are resolved through
 * DVS/DVS_FieldMap.properties first, then by exact locator key.
 */
public final class DvsLocators {

	public record Meta(String id, String module, String tab, String key, String kind) {
	}

	public record Resolution(String id, String reason) {
		public boolean ok() {
			return id != null;
		}
	}

	private static final Pattern NAME_ATTR = Pattern.compile("@name='([^']+)'");
	private static final DvsLocators INSTANCE = new DvsLocators();

	private final Map<String, String> xpaths = new LinkedHashMap<>();
	private final Map<String, Meta> meta = new LinkedHashMap<>();
	private final Map<String, String> fieldMap = new LinkedHashMap<>();

	private DvsLocators() {
		loadXpaths();
		loadKinds();
		loadFieldMap();
	}

	public static DvsLocators get() {
		return INSTANCE;
	}

	public boolean has(String id) {
		return xpaths.containsKey(id);
	}

	public String xpath(String id) {
		String x = xpaths.get(id);
		if (x == null) {
			throw new IllegalArgumentException("Unknown DVS locator id: " + id);
		}
		return x;
	}

	public By by(String id) {
		return By.xpath(xpath(id));
	}

	public Meta meta(String id) {
		return meta.get(id);
	}

	public String kind(String id) {
		Meta m = meta.get(id);
		return m == null ? "" : m.kind();
	}

	/** The MUI input name (e.g. dob) taken from the locator XPath, or "" when it has none. */
	public String inputName(String id) {
		Matcher m = NAME_ATTR.matcher(xpath(id));
		return m.find() ? m.group(1) : "";
	}

	public List<String> idsForTab(String modulePrefix, String tab) {
		List<String> out = new ArrayList<>();
		for (Meta m : meta.values()) {
			if (m.tab().equalsIgnoreCase(tab) && prefixOk(m.id(), modulePrefix)) {
				out.add(m.id());
			}
		}
		return out;
	}

	public List<String> allIds() {
		return new ArrayList<>(xpaths.keySet());
	}

	public static boolean isInput(String kind) {
		return switch (kind) {
			case "text", "masked-text", "textarea", "date", "dropdown", "autocomplete", "checkbox", "radio" -> true;
			default -> false;
		};
	}

	/** moduleCode = IND / MIN / LE (as DvsRow.moduleCode()). */
	public Resolution resolveField(String moduleCode, String field) {
		if (field == null || field.isBlank()) {
			return new Resolution(null, "Field is blank");
		}
		String f = field.trim().toLowerCase(Locale.ROOT);
		String mapped = fieldMap.get(moduleCode.toLowerCase(Locale.ROOT) + "|" + f);
		if (mapped == null) {
			mapped = fieldMap.get(prefixOf(moduleCode).toLowerCase(Locale.ROOT) + "|" + f);
		}
		if (mapped == null) {
			mapped = fieldMap.get("*|" + f);
		}
		if (mapped != null) {
			return xpaths.containsKey(mapped)
					? checkInput(mapped)
					: new Resolution(null, "FieldMap points to unknown locator " + mapped);
		}
		List<String> hits = new ArrayList<>();
		for (Meta m : meta.values()) {
			if (m.key().equalsIgnoreCase(field.trim()) && prefixOk(m.id(), prefixOf(moduleCode))) {
				hits.add(m.id());
			}
		}
		if (hits.size() == 1) {
			return checkInput(hits.get(0));
		}
		if (hits.isEmpty()) {
			return new Resolution(null, "No locator for Field '" + field + "' (add it to DVS_FieldMap.properties)");
		}
		return new Resolution(null, "Field '" + field + "' matches several locators " + hits + " (add it to DVS_FieldMap.properties)");
	}

	private Resolution checkInput(String id) {
		String kind = kind(id);
		if (!isInput(kind)) {
			return new Resolution(null, "Locator " + id + " is a " + kind + ", not an input field");
		}
		return new Resolution(id, "");
	}

	private static String prefixOf(String moduleCode) {
		return "LE".equals(moduleCode) ? "LE" : "IND";
	}

	private static boolean prefixOk(String id, String prefix) {
		return id.startsWith(prefix + "_") || id.startsWith("COM_");
	}

	private void loadXpaths() {
		Properties p = new Properties();
		try (InputStream in = stream("/DVS/dvs_locators.properties")) {
			p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load DVS/dvs_locators.properties", e);
		}
		for (String k : p.stringPropertyNames()) {
			xpaths.put(k, p.getProperty(k).trim());
		}
	}

	private void loadKinds() {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(stream("/DVS/dvs_locator_kinds.csv"), StandardCharsets.UTF_8))) {
			String line = br.readLine();
			while ((line = br.readLine()) != null) {
				List<String> c = splitCsv(line);
				if (c.size() >= 5) {
					meta.put(c.get(0), new Meta(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4)));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load DVS/dvs_locator_kinds.csv", e);
		}
	}

	private void loadFieldMap() {
		try (InputStream in = DvsLocators.class.getResourceAsStream("/DVS/DVS_FieldMap.properties")) {
			if (in == null) {
				return;
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
				String module = t.substring(0, bar).trim().toLowerCase(Locale.ROOT);
				module = module.startsWith("individual") ? "ind" : module.startsWith("legal") ? "le"
						: module.startsWith("minor") ? "min" : module;
				String field = t.substring(bar + 1, eq).trim().toLowerCase(Locale.ROOT);
				fieldMap.put(module + "|" + field, t.substring(eq + 1).trim());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load DVS/DVS_FieldMap.properties", e);
		}
	}

	private static InputStream stream(String path) {
		InputStream in = DvsLocators.class.getResourceAsStream(path);
		if (in == null) {
			throw new IllegalStateException("Missing resource " + path);
		}
		return in;
	}

	static List<String> splitCsv(String line) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (quoted) {
				if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					cur.append('"');
					i++;
				} else if (ch == '"') {
					quoted = false;
				} else {
					cur.append(ch);
				}
			} else if (ch == '"') {
				quoted = true;
			} else if (ch == ',') {
				out.add(cur.toString());
				cur.setLength(0);
			} else {
				cur.append(ch);
			}
		}
		out.add(cur.toString());
		return out;
	}
}
