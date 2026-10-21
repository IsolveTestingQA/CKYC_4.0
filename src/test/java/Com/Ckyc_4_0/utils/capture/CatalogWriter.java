/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.capture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;

/**
 * Turns a crawl into things a script writer uses: the catalog (JSON), a locator properties file, a readable overview, the
 * list of elements that are NEW (not in the code), the list of existing locators that matched nothing (stale?), and a Page
 * class skeleton per module with ready-to-paste {@code By} constants.
 */
final class CatalogWriter {

	/** One page or one state of a page (a tab, an opened dialog). */
	record PageState(String module, String kind, JSONObject snap) {
	}

	private final Path dir;
	private final List<PageState> states;
	private final List<LocatorHealth.Existing> existing;
	private final int[] bestCounts;
	private final Map<Integer, Set<String>> foundOn;

	CatalogWriter(Path dir, List<PageState> states, List<LocatorHealth.Existing> existing, int[] bestCounts, Map<Integer, Set<String>> foundOn) {
		this.dir = dir;
		this.states = states;
		this.existing = existing;
		this.bestCounts = bestCounts;
		this.foundOn = foundOn;
	}

	void writeAll() throws IOException {
		Files.createDirectories(dir);
		writeJson();
		writeProperties();
		writeOverview();
		writeNewAndStale();
		writePageClasses();
	}

	/** Side-navigation items repeat on every page: they are kept once, on a "Navigation" page. */
	private static boolean isNav(JSONObject it) {
		String b = it.optString("best");
		return it.optBoolean("inNav") || b.contains("data-tour-label") || b.contains("tour-nav-") || b.contains("[@role='button'][normalize-space()=");
	}

	private static boolean usable(JSONObject it) {
		String kind = it.optString("bestKind");
		return !it.optString("best").isBlank() && !kind.equals("position") && !kind.equals("none");
	}

	private void writeJson() throws IOException {
		JSONArray pages = new JSONArray();
		for (PageState p : states) {
			JSONObject o = new JSONObject().put("module", p.module()).put("kind", p.kind()).put("path", p.snap().optString("path"))
					.put("title", p.snap().optString("title")).put("items", p.snap().optJSONArray("items"))
					.put("fileInputs", p.snap().optJSONArray("fileInputs"));
			pages.put(o);
		}
		JSONObject root = new JSONObject().put("generated", ReportNamingHelper.nowFormatted()).put("testedBy", ConfigReader.testedBy())
				.put("pages", pages);
		Files.writeString(dir.resolve("catalog.json"), root.toString(1), StandardCharsets.UTF_8);
	}

	private void writeProperties() throws IOException {
		StringBuilder sb = new StringBuilder("# Locators found by the crawler - key = Module_Label, value = best locator. unique=false needs a look.\n");
		Set<String> keys = new HashSet<>();
		Set<String> seen = new HashSet<>();
		for (PageState p : states) {
			JSONArray items = p.snap().optJSONArray("items");
			for (int i = 0; items != null && i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				if (!usable(it) || !seen.add(p.module() + "|" + it.optString("best"))) {
					continue;
				}
				String key = key(p.module()) + "_" + key(label(it));
				String base = key;
				int n = 2;
				while (!keys.add(key)) {
					key = base + "_" + n++;
				}
				sb.append("# ").append(p.kind()).append(" | kind=").append(it.optString("bestKind")).append(" unique=").append(it.optBoolean("unique"))
						.append(it.optBoolean("masked") ? " MASKED" : "").append('\n');
				sb.append(key).append('=').append(it.optString("best")).append('\n');
			}
		}
		Files.writeString(dir.resolve("locators_catalog.properties"), sb.toString(), StandardCharsets.UTF_8);
	}

	private void writeOverview() throws IOException {
		Map<String, List<PageState>> byModule = new LinkedHashMap<>();
		for (PageState p : states) {
			byModule.computeIfAbsent(p.module(), k -> new ArrayList<>()).add(p);
		}
		StringBuilder sb = new StringBuilder("# Crawl overview\n\nTested by ").append(ConfigReader.testedBy()).append(" - ")
				.append(ReportNamingHelper.nowFormatted()).append("\n\n| Module | URL | States | Controls | Unique locators | File inputs |\n|---|---|---|---|---|---|\n");
		for (Map.Entry<String, List<PageState>> m : byModule.entrySet()) {
			int controls = 0;
			int unique = 0;
			int files = 0;
			Set<String> kinds = new LinkedHashSet<>();
			for (PageState p : m.getValue()) {
				JSONArray items = p.snap().optJSONArray("items");
				controls += items == null ? 0 : items.length();
				for (int i = 0; items != null && i < items.length(); i++) {
					if (items.getJSONObject(i).optBoolean("unique")) {
						unique++;
					}
				}
				files += p.snap().optJSONArray("fileInputs") == null ? 0 : p.snap().optJSONArray("fileInputs").length();
				kinds.add(p.kind());
			}
			sb.append("| ").append(m.getKey()).append(" | `").append(m.getValue().get(0).snap().optString("path")).append("` | ")
					.append(String.join(", ", kinds)).append(" | ").append(controls).append(" | ").append(unique).append(" | ").append(files).append(" |\n");
		}
		Files.writeString(dir.resolve("pages_overview.md"), sb.toString(), StandardCharsets.UTF_8);
	}

	private void writeNewAndStale() throws IOException {
		Set<String> known = new HashSet<>();
		for (LocatorHealth.Existing e : existing) {
			known.add(LocatorHealth.norm(e.expr()));
		}
		StringBuilder neu = new StringBuilder("# New elements - on the page but not in the code\n\n");
		Set<String> printed = new HashSet<>();
		int count = 0;
		String last = "";
		for (PageState p : states) {
			JSONArray items = p.snap().optJSONArray("items");
			for (int i = 0; items != null && i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				if (!usable(it) || !it.optBoolean("unique") || known.contains(LocatorHealth.norm(it.optString("best")))
						|| !printed.add(p.module() + "|" + it.optString("best"))) {
					continue;
				}
				if (!p.module().equals(last)) {
					neu.append("\n## ").append(p.module()).append("\n\n");
					last = p.module();
				}
				neu.append("- **").append(label(it)).append("** (").append(it.optString("tag")).append(", ").append(p.kind()).append(") `")
						.append(it.optString("best")).append("`\n");
				count++;
			}
		}
		neu.insert(0, "");
		Files.writeString(dir.resolve("new_elements.md"), neu.append("\nTotal new unique elements: ").append(count).append('\n').toString(),
				StandardCharsets.UTF_8);

		StringBuilder stale = new StringBuilder("# Existing locators that matched nothing on any crawled page or state\n\n"
				+ "They may be fine (they can exist only after an action, a search or inside a pop-up the crawler does not open) - or the "
				+ "application changed. Check these first when a run gets stuck.\n\n");
		String cls = "";
		int n = 0;
		for (int i = 0; i < existing.size(); i++) {
			boolean found = foundOn.containsKey(i) && !foundOn.get(i).isEmpty();
			if (found) {
				continue;
			}
			LocatorHealth.Existing e = existing.get(i);
			String c = e.id().substring(0, e.id().indexOf('.'));
			if (!c.equals(cls)) {
				stale.append("\n## ").append(c).append("\n\n");
				cls = c;
			}
			stale.append("- `").append(e.id()).append("` ").append(e.kind()).append(" `").append(e.expr(), 0, Math.min(160, e.expr().length())).append("`\n");
			n++;
		}
		stale.append("\nExisting locators checked: ").append(existing.size()).append("; matched somewhere: ").append(existing.size() - n)
				.append("; matched nothing: ").append(n).append('\n');
		Files.writeString(dir.resolve("stale_locators.md"), stale.toString(), StandardCharsets.UTF_8);
	}

	private void writePageClasses() throws IOException {
		Path out = dir.resolve("generated_pages");
		Files.createDirectories(out);
		Map<String, List<PageState>> byModule = new LinkedHashMap<>();
		JSONArray navItems = new JSONArray();
		Set<String> navSeen = new HashSet<>();
		for (PageState p : states) {
			JSONArray items = p.snap().optJSONArray("items");
			JSONArray keep = new JSONArray();
			for (int i = 0; items != null && i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				if (isNav(it)) {
					if (navSeen.add(it.optString("best"))) {
						navItems.put(it);
					}
				} else {
					keep.put(it);
				}
			}
			JSONObject copy = new JSONObject(p.snap().toString()).put("items", keep);
			byModule.computeIfAbsent(p.module(), k -> new ArrayList<>()).add(new PageState(p.module(), p.kind(), copy));
		}
		if (navItems.length() > 0) {
			Map<String, List<PageState>> withNav = new LinkedHashMap<>();
			withNav.put("Navigation", List.of(new PageState("Navigation", "side navigation", new JSONObject().put("items", navItems).put("path", ""))));
			withNav.putAll(byModule);
			byModule = withNav;
		}
		for (Map.Entry<String, List<PageState>> m : byModule.entrySet()) {
			String cls = className(m.getKey()) + "CapturedPage";
			StringBuilder sb = new StringBuilder();
			sb.append("package Com.Ckyc_4_0.pages.captured;\n\nimport org.openqa.selenium.By;\n\n/**\n * Generated by the locator crawler from the live \"")
					.append(m.getKey()).append("\" page (").append(m.getValue().get(0).snap().optString("path")).append("). Review, rename and move the\n")
					.append(" * constants you need into the real Page class. unique=false / positional locators are left as comments.\n */\n")
					.append("public final class ").append(cls).append(" {\n\n\tprivate ").append(cls).append("() {\n\t}\n\n");
			Set<String> names = new HashSet<>();
			Set<String> seen = new HashSet<>();
			for (PageState p : m.getValue()) {
				JSONArray items = p.snap().optJSONArray("items");
				sb.append("\t// ---- ").append(p.kind()).append('\n');
				for (int i = 0; items != null && i < items.length(); i++) {
					JSONObject it = items.getJSONObject(i);
					if (it.optString("best").isBlank() || !seen.add(it.optString("best"))) {
						continue;
					}
					String xp = it.optString("best").replace("\\", "\\\\").replace("\"", "\\\"");
					if (usable(it) && it.optBoolean("unique")) {
						String n = constant(it);
						String base = n;
						int k = 2;
						while (!names.add(n)) {
							n = base + "_" + k++;
						}
						sb.append("\tpublic static final By ").append(n).append(" = By.xpath(\"").append(xp).append("\");\n");
					} else {
						sb.append("\t// not unique or positional: ").append(label(it)).append(" -> ").append(xp, 0, Math.min(140, xp.length())).append('\n');
					}
				}
			}
			sb.append("}\n");
			Files.writeString(out.resolve(cls + ".java"), sb.toString(), StandardCharsets.UTF_8);
		}
	}

	// ------------------------------------------------------------------ names

	private static String label(JSONObject it) {
		for (String k : new String[] { "ariaLabel", "text", "placeholder", "label", "name", "id" }) {
			String v = it.optString(k);
			if (!v.isBlank()) {
				return v;
			}
		}
		return it.optString("tag", "element");
	}

	private static String key(String s) {
		String k = s == null ? "" : s.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return k.isEmpty() ? "x" : (k.length() > 40 ? k.substring(0, 40) : k);
	}

	private static String className(String module) {
		StringBuilder sb = new StringBuilder();
		for (String w : module.split("[^A-Za-z0-9]+")) {
			if (!w.isBlank()) {
				sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
			}
		}
		String c = sb.toString();
		return c.isEmpty() || Character.isDigit(c.charAt(0)) ? "Page" + c : c;
	}

	private static String constant(JSONObject it) {
		String base = key(label(it)).toUpperCase(Locale.ROOT);
		if (base.isEmpty() || Character.isDigit(base.charAt(0))) {
			base = "X_" + base;
		}
		String tag = it.optString("tag");
		String role = it.optString("role");
		String suffix = role.equals("tab") ? "_TAB" : tag.equals("button") ? "_BTN" : tag.equals("input") || tag.equals("textarea") ? "_INPUT"
				: tag.equals("select") ? "_SELECT" : tag.equals("th") ? "_HEADER" : tag.equals("a") ? "_LINK" : tag.equals("label") ? "_LABEL"
						: tag.matches("h[1-4]") ? "_HEADING" : "";
		return base + suffix;
	}
}
