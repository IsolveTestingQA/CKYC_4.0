/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.capture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;

/**
 * Watches what the tester does by hand in the application and saves it: every click, field, file, toast and dialog with
 * the best locator candidates, a screen inventory at every module switch (and on demand), and the tester's notes. A pop-up
 * appears in the page each time the tester switches module from the navigation bar. Nothing is clicked or typed by this
 * class, typed values are never stored (only their length) and password fields are never recorded.
 *
 * Output (under {@code Locator Capture/<date>/<run>/}): capture.json (everything), locators_draft.properties (one line per
 * element: module_label=xpath), capture_report.md (readable, module by module).
 */
public final class LocatorRecorder {

	private static final Logger logger = LoggerFactory.getLogger(LocatorRecorder.class);

	private final WebDriver driver;
	private final Path dir;
	private final List<JSONObject> events = new ArrayList<>();
	private String script;
	private JSONObject carryState = new JSONObject();
	private long lastWrite;

	public LocatorRecorder(WebDriver driver) {
		this.driver = driver;
		this.dir = Paths.get(ReportNamingHelper.reportOutputRoot()).getParent().resolve("Locator Capture")
				.resolve(ReportNamingHelper.runDate()).resolve(ReportNamingHelper.runTimestamp());
	}

	public Path folder() {
		return dir;
	}

	/** Records until the tester presses Finish in the page or closes the browser. */
	public void run() {
		try {
			Files.createDirectories(dir);
			script = loadScript();
		} catch (IOException e) {
			throw new IllegalStateException("Locator recorder cannot start: " + e.getMessage(), e);
		}
		long poll = Long.parseLong(ConfigReader.get("capture.pollMs", "800"));
		logger.info("Locator capture started - do your steps in the browser. Finish = the Finish button in the page. Folder: {}", dir);
		boolean finished = false;
		int deadSession = 0;
		while (!finished) {
			try {
				JavascriptExecutor js = (JavascriptExecutor) driver;
				Object installed = js.executeScript("return !!window.__ckycRecInstalled;");
				if (!Boolean.TRUE.equals(installed)) {
					js.executeScript("window.__ckycRecInit=" + carryState + ";" + script);
				}
				Object raw = js.executeScript("return window.__ckycRecDrain ? window.__ckycRecDrain() : null;");
				if (raw != null) {
					JSONObject state = new JSONObject(String.valueOf(raw));
					JSONArray ev = state.optJSONArray("events");
					for (int i = 0; ev != null && i < ev.length(); i++) {
						events.add(ev.getJSONObject(i));
					}
					carryState = new JSONObject().put("module", state.optString("module"))
							.put("modKey", state.optString("modKey")).put("counts", state.optJSONObject("counts"))
							.put("modCounts", state.optJSONObject("modCounts"));
					finished = state.optBoolean("finish");
					if (ev != null && ev.length() > 0) {
						saveSoon(false);
					}
				}
				deadSession = 0;
			} catch (org.openqa.selenium.WebDriverException e) {
				String m = String.valueOf(e.getMessage());
				if (m.contains("invalid session") || m.contains("no such window") || m.contains("not reachable")
						|| m.contains("disconnected") || ++deadSession > 12) {
					logger.info("Browser closed - capture finished");
					break;
				}
			}
			try {
				Thread.sleep(poll);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		saveSoon(true);
		print();
	}

	private static String loadScript() throws IOException {
		try (InputStream in = LocatorRecorder.class.getResourceAsStream("/Capture/locator_recorder.js")) {
			if (in == null) {
				throw new IOException("Capture/locator_recorder.js not found on the classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	// ------------------------------------------------------------------ output

	private void saveSoon(boolean force) {
		long now = System.currentTimeMillis();
		if (!force && now - lastWrite < 4000) {
			return;
		}
		lastWrite = now;
		try {
			JSONArray all = new JSONArray();
			events.forEach(all::put);
			Files.writeString(dir.resolve("capture.json"), all.toString(1), StandardCharsets.UTF_8);
			Files.writeString(dir.resolve("locators_draft.properties"), locatorsDraft(), StandardCharsets.UTF_8);
			Files.writeString(dir.resolve("capture_report.md"), report(), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			logger.warn("Locator capture could not be saved: {}", e.getMessage());
		}
	}

	private String locatorsDraft() {
		StringBuilder sb = new StringBuilder("# Draft locators captured by hand - review before use (unique=false means more than one match)\n");
		Set<String> seenXpath = new HashSet<>();
		Set<String> seenKey = new HashSet<>();
		for (JSONObject e : events) {
			String type = e.optString("type");
			if (!(type.equals("click") || type.equals("type") || type.equals("file"))) {
				continue;
			}
			addLocator(sb, e, seenXpath, seenKey);
			if (type.equals("snap")) {
				continue;
			}
		}
		for (JSONObject e : events) {
			if (!e.optString("type").equals("snap")) {
				continue;
			}
			JSONArray files = e.optJSONArray("fileInputs");
			for (int i = 0; files != null && i < files.length(); i++) {
				addLocator(sb, files.getJSONObject(i).put("module", e.optString("module")), seenXpath, seenKey);
			}
			JSONArray items = e.optJSONArray("items");
			for (int i = 0; items != null && i < items.length(); i++) {
				addLocator(sb, items.getJSONObject(i).put("module", e.optString("module")), seenXpath, seenKey);
			}
		}
		return sb.toString();
	}

	private static void addLocator(StringBuilder sb, JSONObject e, Set<String> seenXpath, Set<String> seenKey) {
		String xp = e.optString("best");
		if (xp.isBlank() || !seenXpath.add(e.optString("module") + "|" + xp)) {
			return;
		}
		String label = first(e.optString("ariaLabel"), e.optString("text"), e.optString("placeholder"), e.optString("label"),
				e.optString("name"), e.optString("id"), e.optString("tag"));
		String key = key(e.optString("module")) + "_" + key(label);
		String base = key;
		int n = 2;
		while (!seenKey.add(key)) {
			key = base + "_" + n++;
		}
		sb.append("# ").append(e.optString("module")).append(" | ").append(e.optString("section")).append(" | kind=")
				.append(e.optString("bestKind")).append(" unique=").append(e.optBoolean("unique")).append(" matches=")
				.append(e.optInt("matches")).append(e.optBoolean("masked") ? " MASKED" : "").append('\n');
		sb.append(key).append('=').append(xp).append('\n');
	}

	private static String first(String... values) {
		for (String v : values) {
			if (v != null && !v.isBlank()) {
				return v;
			}
		}
		return "element";
	}

	private static String key(String s) {
		String k = s == null ? "" : s.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return k.isEmpty() ? "x" : (k.length() > 40 ? k.substring(0, 40) : k);
	}

	private String report() {
		Map<String, List<JSONObject>> byModule = new LinkedHashMap<>();
		for (JSONObject e : events) {
			byModule.computeIfAbsent(e.optString("module", "-"), k -> new ArrayList<>()).add(e);
		}
		StringBuilder sb = new StringBuilder("# Locator capture\n\nTested by ").append(ConfigReader.testedBy())
				.append(" - ").append(ReportNamingHelper.nowFormatted()).append("\n\n");
		for (Map.Entry<String, List<JSONObject>> m : byModule.entrySet()) {
			sb.append("## ").append(m.getKey().isEmpty() ? "(start)" : m.getKey()).append("\n\n");
			for (JSONObject e : m.getValue()) {
				String t = e.optString("type");
				switch (t) {
				case "click" -> sb.append("- click **").append(label(e)).append("** `").append(e.optString("best")).append("`")
						.append(e.optBoolean("unique") ? "" : " (not unique)").append(e.optBoolean("nav") ? " [nav bar]" : "").append('\n');
				case "type" -> sb.append("- field **").append(label(e)).append("** `").append(e.optString("best")).append("` (")
						.append(e.optInt("valueLength")).append(" chars)").append(e.optBoolean("masked") ? " MASKED" : "").append('\n');
				case "file" -> sb.append("- FILE ").append(e.optJSONArray("files")).append(" via ").append(e.optString("via"))
						.append(" `").append(e.optString("best")).append("`\n");
				case "toast" -> sb.append("- TOAST: ").append(e.optString("message")).append('\n');
				case "dialog" -> sb.append("- dialog opened: ").append(e.optString("title")).append('\n');
				case "note" -> sb.append("- NOTE: ").append(e.optString("note")).append('\n');
				case "module" -> sb.append("- *moved from* ").append(e.optString("from")).append(" *to* ").append(e.optString("to"))
						.append(" (").append(e.optString("key")).append(")\n");
				case "snap" -> sb.append("- screen snapshot (").append(e.optString("kind")).append("): ")
						.append(e.optJSONArray("items") == null ? 0 : e.optJSONArray("items").length()).append(" elements, ")
						.append(e.optJSONArray("texts") == null ? 0 : e.optJSONArray("texts").length()).append(" texts, ")
						.append(e.optJSONArray("fileInputs") == null ? 0 : e.optJSONArray("fileInputs").length())
						.append(" file inputs\n");
				default -> {
				}
				}
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String label(JSONObject e) {
		return first(e.optString("ariaLabel"), e.optString("text"), e.optString("placeholder"), e.optString("label"),
				e.optString("name"), e.optString("id"), e.optString("tag"));
	}

	private void print() {
		StringBuilder sb = new StringBuilder(System.lineSeparator());
		sb.append("==================== LOCATOR CAPTURE SAVED ====================").append(System.lineSeparator());
		sb.append(" Events captured : ").append(events.size()).append(System.lineSeparator());
		for (String f : new String[] { "capture_report.md", "locators_draft.properties", "capture.json" }) {
			sb.append(String.format(" %-26s %s%n", f, dir.resolve(f).toAbsolutePath().toUri()));
		}
		sb.append("================================================================");
		logger.info(sb.toString());
	}
}
