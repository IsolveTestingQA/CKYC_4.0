/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.panel;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Control Panel: right after the browser has opened on the login page the operator sees a popup, ticks the type
 * of testing (Positive / Negative / Both), the modules and their options, and presses Start Test. The choices are
 * turned into a Cucumber tag filter plus the config values the runners already read, so the ONE runner that is
 * running executes only what was ticked. "Stop" runs nothing. Without an operator (headless /
 * interactiveBlockerPrompt=false) the panel is skipped and the runner's own defaults apply.
 *
 * Presets and the last choice are kept in "Control Panel/presets.json" (next to the project, not in the code).
 */
public final class ControlPanel {

	private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);
	private static final String ID = "ckyc-control-panel";

	private ControlPanel() {
	}

	public static boolean enabled() {
		return ConfigReader.interactiveBlockerPrompt() && !Boolean.parseBoolean(ConfigReader.get("headless", "false"));
	}

	/** Shows the panel, waits for the answer and applies it. Never throws - on any problem the defaults apply. */
	public static void chooseAndApply() {
		chooseAndApply("");
	}

	/**
	 * @param runnerName the runner class that started the suite: the panel opens pre-ticked with that runner's own
	 *                   module(s), so pressing Start without changes runs what that runner always ran.
	 */
	public static void chooseAndApply(String runnerName) {
		try {
			if (!enabled()) {
				logger.info("Control panel skipped (headless or interactiveBlockerPrompt=false) - runner defaults apply");
				return;
			}
			WebDriver driver = BaseClass.getDriver();
			if (driver == null) {
				logger.warn("Control panel skipped - the browser is not open");
				return;
			}
			JSONObject cfg = panelConfig();
			JSONObject defaults = defaultsFor(runnerName);
			cfg.put("defaults", defaults == null ? JSONObject.NULL : defaults);
			cfg.put("runner", runnerName == null ? "" : runnerName);
			// the toggle shows ON when the runner class / config already started the recording
			cfg.put("record", Com.Ckyc_4_0.utils.recording.RunRecording.isRecording());
			JSONObject answer = ask(driver, cfg);
			applyRecording(answer, runnerName);
			RunSelection sel = apply(answer);
			RunSelection.set(sel);
			logger.info("Control panel | {} | tags: {}", sel.summary(), sel.tagExpression());
		} catch (RuntimeException e) {
			logger.error("Control panel failed - runner defaults apply: {}", e.getMessage(), e);
		}
	}

	/** The panel's Screen recording toggle wins over the runner constant / config: on = start now, off = discard. */
	private static void applyRecording(JSONObject answer, String runnerName) {
		try {
			boolean stopped = "stop".equals(answer.optString("action"));
			JSONObject opts = answer.optJSONObject("opts");
			boolean want = !stopped && opts != null && opts.optBoolean("record", false);
			boolean on = Com.Ckyc_4_0.utils.recording.RunRecording.isRecording();
			if (want && !on) {
				Com.Ckyc_4_0.utils.recording.RunRecording.start(runnerName);
			} else if (!want && on) {
				Com.Ckyc_4_0.utils.recording.RunRecording.discard();
			}
		} catch (RuntimeException e) {
			logger.warn("Screen recording switch from the panel failed: {}", e.getMessage());
		}
	}

	// ------------------------------------------------------------------ panel data

	private static JSONObject panelConfig() {
		JSONObject cfg = new JSONObject();
		cfg.put("tester", ConfigReader.testedBy());
		cfg.put("url", ConfigReader.getUrl());
		JSONArray mods = new JSONArray();
		for (ControlPanelRegistry.Module m : ControlPanelRegistry.modules()) {
			JSONObject mj = new JSONObject().put("key", m.key()).put("title", m.title());
			JSONArray subs = new JSONArray();
			for (ControlPanelRegistry.Sub s : m.subs()) {
				subs.put(new JSONObject().put("key", s.key()).put("title", s.title()).put("pos", s.pos() != null)
						.put("neg", s.neg() != null));
			}
			mj.put("subs", subs);
			JSONArray groups = new JSONArray();
			for (ControlPanelRegistry.Group g : m.groups()) {
				JSONArray quick = new JSONArray();
				for (Map.Entry<String, List<String>> q : g.quick().entrySet()) {
					quick.put(new JSONObject().put("label", q.getKey()).put("options", new JSONArray(q.getValue())));
				}
				groups.put(new JSONObject().put("key", g.key()).put("label", g.label())
						.put("options", new JSONArray(g.options())).put("defaults", new JSONArray(g.defaults()))
						.put("quick", quick));
			}
			mj.put("groups", groups);
			JSONArray texts = new JSONArray();
			for (ControlPanelRegistry.Text t : m.texts()) {
				String current = ConfigReader.get(t.configKey(), t.fallback());
				texts.put(new JSONObject().put("key", t.key()).put("label", t.label()).put("def", current));
			}
			mj.put("texts", texts);
			mods.put(mj);
		}
		cfg.put("modules", mods);
		JSONObject store = readStore();
		cfg.put("last", store.has("last") ? store.getJSONObject("last") : JSONObject.NULL);
		JSONArray presets = new JSONArray();
		JSONObject ps = store.optJSONObject("presets");
		if (ps != null) {
			for (String name : ps.keySet()) {
				presets.put(new JSONObject().put("name", name).put("sel", ps.getJSONObject(name)));
			}
		}
		cfg.put("presets", presets);
		return cfg;
	}

	// ------------------------------------------------------------------ popup

	private static JSONObject ask(WebDriver driver, JSONObject cfg) {
		int timeoutMinutes = Math.max(1, Integer.parseInt(ConfigReader.get("panel.timeoutMinutes", "60")));
		((JavascriptExecutor) driver).executeScript("window.__ckycPanel = null; var old = document.getElementById('" + ID
				+ "'); if (old) { old.remove(); }" + panelJs(), cfg.toString());
		long deadline = System.currentTimeMillis() + timeoutMinutes * 60_000L;
		while (System.currentTimeMillis() < deadline) {
			Object o = ((JavascriptExecutor) driver).executeScript("return window.__ckycPanel;");
			if (o instanceof String && !((String) o).isBlank()) {
				removePanel(driver);
				return new JSONObject((String) o);
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		removePanel(driver);
		logger.warn("Control panel not answered for {} minutes - nothing will run", timeoutMinutes);
		return new JSONObject().put("action", "stop");
	}

	private static void removePanel(WebDriver driver) {
		try {
			((JavascriptExecutor) driver).executeScript("var e = document.getElementById('" + ID + "'); if (e) { e.remove(); }"
					+ "window.__ckycPanel = null;");
		} catch (RuntimeException ignored) {
			// page gone
		}
	}

	private static String panelJs() {
		try (InputStream in = ControlPanel.class.getResourceAsStream("/Reports/control_panel.js")) {
			if (in == null) {
				throw new IllegalStateException("Reports/control_panel.js not found on the classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read the control panel script", e);
		}
	}

	// ------------------------------------------------------------------ apply

	static RunSelection apply(JSONObject a) {
		if (!"start".equals(a.optString("action"))) {
			System.setProperty("cucumber.filter.tags", ControlPanelRegistry.NOTHING_TAG);
			return new RunSelection("POSITIVE", new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), false,
					ControlPanelRegistry.NOTHING_TAG, List.of(), true, "");
		}
		String mode = a.optString("mode", "POSITIVE");
		Map<String, List<String>> subs = new LinkedHashMap<>();
		Map<String, Map<String, List<String>>> groups = new LinkedHashMap<>();
		Map<String, Map<String, String>> texts = new LinkedHashMap<>();
		JSONObject mods = a.optJSONObject("modules");
		if (mods != null) {
			for (String key : mods.keySet()) {
				ControlPanelRegistry.Module m = ControlPanelRegistry.module(key);
				if (m == null) {
					continue;
				}
				JSONObject mo = mods.getJSONObject(key);
				List<String> s = new ArrayList<>();
				JSONArray sa = mo.optJSONArray("subs");
				if (sa != null) {
					for (int i = 0; i < sa.length(); i++) {
						s.add(sa.getString(i));
					}
				}
				subs.put(key, s);
				JSONObject go = mo.optJSONObject("groups");
				if (go != null) {
					Map<String, List<String>> gm = new LinkedHashMap<>();
					for (ControlPanelRegistry.Group g : m.groups()) {
						JSONArray ga = go.optJSONArray(g.key());
						List<String> vals = new ArrayList<>();
						if (ga != null) {
							for (int i = 0; i < ga.length(); i++) {
								vals.add(ga.getString(i));
							}
						}
						if (!vals.isEmpty()) {
							gm.put(g.key(), vals);
							System.setProperty("ckyc." + g.configKey(), String.join(",", vals));
						}
					}
					groups.put(key, gm);
				}
				JSONObject to = mo.optJSONObject("texts");
				if (to != null) {
					Map<String, String> tm = new LinkedHashMap<>();
					for (ControlPanelRegistry.Text t : m.texts()) {
						String v = to.optString(t.key(), "").trim();
						if (!v.isEmpty()) {
							tm.put(t.key(), v);
							System.setProperty(t.configKey(), v);
							System.setProperty("ckyc." + t.configKey(), v);
						}
					}
					texts.put(key, tm);
				}
			}
		}
		List<String> notes = new ArrayList<>();
		String tags = ControlPanelRegistry.tagExpression(mode, subs, notes);
		System.setProperty("cucumber.filter.tags", tags);
		// the panel can add modules the running runner would not normally load: look at every feature, the tags decide
		System.setProperty("cucumber.features", "src/test/resources/Features");

		JSONObject opts = a.optJSONObject("opts");
		boolean advanced = opts == null || opts.optBoolean("advanced", true);
		boolean extentCucumber = opts == null || opts.optBoolean("extentCucumber", true);
		boolean stopFirst = opts != null && opts.optBoolean("stopFirst", false);
		System.setProperty("ckyc.openAdvancedReportAfterRun", String.valueOf(advanced));
		System.setProperty("ckyc.openExtentReportAfterRun", String.valueOf(extentCucumber));
		System.setProperty("ckyc.openCucumberReportAfterRun", String.valueOf(extentCucumber));

		String preset = a.optString("preset", "").trim();
		saveStore(a, preset);
		return new RunSelection(mode, subs, groups, texts, stopFirst, tags, notes, false, preset);
	}

	// ------------------------------------------------------------------ runner defaults

	private static JSONObject pick(String mode, String... moduleAndSubs) {
		JSONObject modules = new JSONObject();
		for (String spec : moduleAndSubs) {
			String[] p = spec.split(":");
			JSONArray subs = new JSONArray();
			if (p.length > 1) {
				for (String s : p[1].split(",")) {
					subs.put(s);
				}
			}
			modules.put(p[0], new JSONObject().put("subs", subs).put("groups", new JSONObject()).put("texts", new JSONObject()));
		}
		return new JSONObject().put("mode", mode).put("modules", modules);
	}

	/** What each existing runner always ran - used to pre-tick the panel. Null = no default (last run is used). */
	static JSONObject defaultsFor(String runnerName) {
		String n = runnerName == null ? "" : runnerName;
		if (n.contains("TestControlPanelRunner") || n.isEmpty()) {
			return null;
		}
		if (n.contains("TestLoginRunner")) {
			return pick("POSITIVE", "login:login");
		}
		if (n.contains("TestDashboardRunner")) {
			return pick("POSITIVE", "login:login", "dashboard:dashboard");
		}
		if (n.contains("TestMastersRunner")) {
			return pick("POSITIVE", "login:login", "masters:state,district,pincode,hierarchy");
		}
		if (n.contains("TestSearchInteractiveRunner")) {
			return pick("POSITIVE", "search:interactive");
		}
		if (n.contains("TestSearchAndDownloadRunner") || n.contains("TestSearchRunner")) {
			return pick("POSITIVE", "login:login", "search:search");
		}
		if (n.contains("TestUserManagementRunner")) {
			return pick("BOTH", "login:login", "usermgmt:users,roles");
		}
		if (n.contains("TestConcurrentSessionRunner")) {
			return pick("POSITIVE", "concurrent:concurrent");
		}
		if (n.contains("TestDVSIndividualPositiveRunner")) {
			return pick("POSITIVE", "dvs:individual");
		}
		if (n.contains("TestDVSIndividualNegativeRunner")) {
			return pick("NEGATIVE", "dvs:individual");
		}
		if (n.contains("TestDVSLegalPositiveRunner")) {
			return pick("POSITIVE", "dvs:legal");
		}
		if (n.contains("TestDVSLegalNegativeRunner")) {
			return pick("NEGATIVE", "dvs:legal");
		}
		if (n.contains("TestDVSCombinedRunner")) {
			return pick("POSITIVE", "dvswb:combined");
		}
		if (n.contains("TestDVSRegressionRunner")) {
			return pick("POSITIVE", "dvswb:regression");
		}
		if (n.contains("TestDVSNegativeRunner")) {
			return pick("POSITIVE", "dvswb:negative");
		}
		if (n.contains("TestDVSPositiveRunner")) {
			return pick("POSITIVE", "dvswb:positive");
		}
		if (n.contains("TestReportsRunner")) {
			return pick("POSITIVE", "reports:compare");
		}
		if (n.contains("TestBulkImportRunner")) {
			return pick("POSITIVE", "bulkimport:flow");
		}
		if (n.contains("TestCKYCAllRunner") || n.contains("TestRunnerCkyc")) {
			return pick("POSITIVE", "login:login", "dashboard:dashboard", "masters:state,district,pincode,hierarchy",
					"search:search", "usermgmt:users,roles");
		}
		return null;
	}

	// ------------------------------------------------------------------ presets file

	private static Path storeFile() {
		return Paths.get(System.getProperty("user.dir"), "Control Panel", "presets.json");
	}

	private static JSONObject readStore() {
		try {
			Path f = storeFile();
			if (Files.exists(f)) {
				return new JSONObject(Files.readString(f, StandardCharsets.UTF_8));
			}
		} catch (IOException | RuntimeException e) {
			logger.warn("Presets file could not be read: {}", e.getMessage());
		}
		return new JSONObject();
	}

	private static void saveStore(JSONObject answer, String presetName) {
		try {
			JSONObject store = readStore();
			store.put("last", answer);
			if (!presetName.isEmpty()) {
				JSONObject ps = store.optJSONObject("presets");
				if (ps == null) {
					ps = new JSONObject();
				}
				ps.put(presetName, answer);
				store.put("presets", ps);
			}
			Files.createDirectories(storeFile().getParent());
			Files.writeString(storeFile(), store.toString(2), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			logger.warn("Presets file could not be written: {}", e.getMessage());
		}
	}
}
