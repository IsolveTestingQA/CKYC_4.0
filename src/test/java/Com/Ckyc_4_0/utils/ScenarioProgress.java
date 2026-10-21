/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;

/**
 * "What is being checked now and what is still left" - a small toast on the page for a few seconds at the start of every
 * feature step: step n of N, the step running now, and the next steps still to come. It never takes a click
 * (pointer-events: none), fades by itself, and any problem showing it is ignored.
 * Switch off with progressToast=false in config.properties; length with progressToastSeconds (default 4).
 */
public final class ScenarioProgress {

	private static final Logger logger = LoggerFactory.getLogger(ScenarioProgress.class);
	private static final Pattern STEP = Pattern.compile("^(Given|When|Then|And|But|\\*)\\s+(.*)$");
	private static final int NEXT_SHOWN = 4;

	private static final ThreadLocal<State> STATE = new ThreadLocal<>();

	private static final class State {
		String scenario;
		List<String> steps = new ArrayList<>();
		int index;
	}

	private ScenarioProgress() {
	}

	/** Called when a scenario starts: reads its step list (Background first) from the feature file. */
	public static void start(String scenarioName, URI uri, int line) {
		State st = new State();
		st.scenario = scenarioName;
		try {
			st.steps = readSteps(uri, line);
		} catch (RuntimeException | IOException e) {
			logger.debug("Scenario step list not read: {}", e.getMessage());
		}
		STATE.set(st);
	}

	public static void end() {
		STATE.remove();
	}

	/** Called before every feature step. */
	public static void stepStarting(WebDriver driver) {
		State st = STATE.get();
		if (st == null) {
			return;
		}
		st.index++;
		if (!ConfigReader.get("progressToast", "true").equalsIgnoreCase("true") || driver == null) {
			return;
		}
		try {
			int total = Math.max(st.steps.size(), st.index);
			String now = st.index <= st.steps.size() ? st.steps.get(st.index - 1) : "";
			StringBuilder next = new StringBuilder();
			int shown = 0;
			for (int i = st.index; i < st.steps.size() && shown < NEXT_SHOWN; i++, shown++) {
				next.append("<div style='opacity:.9'>&#9675; ").append(esc(st.steps.get(i))).append("</div>");
			}
			int more = Math.max(0, st.steps.size() - st.index - shown);
			if (more > 0) {
				next.append("<div style='opacity:.7'>&hellip; +").append(more).append(" more</div>");
			}
			int left = Math.max(0, total - st.index);
			boolean failing = SoftAssertManager.hasFailures();
			int pct = total == 0 ? 0 : (int) Math.round(100.0 * (st.index - 1) / total);
			String html = "<div style='font-weight:600;margin-bottom:3px'>" + (failing ? "&#9888; " : "&#9654; ")
					+ "Step " + st.index + " of " + total + " &middot; " + esc(shorten(st.scenario, 70)) + "</div>"
					+ "<div style='color:#fde68a;margin-bottom:5px'>Checking now: " + esc(now) + "</div>"
					+ "<div style='height:5px;background:#374151;border-radius:3px;margin-bottom:6px'>"
					+ "<div style='height:5px;width:" + pct + "%;background:" + (failing ? "#f87171" : "#22c55e")
					+ ";border-radius:3px'></div></div>"
					+ "<div style='opacity:.8;margin-bottom:2px'>Still to check (" + left + "):</div>" + next
					+ (failing ? "<div style='color:#fca5a5;margin-top:4px'>Failures recorded so far in this scenario</div>" : "");
			int seconds = 4;
			try {
				seconds = Integer.parseInt(ConfigReader.get("progressToastSeconds", "4").trim());
			} catch (NumberFormatException ignored) {
				// keep default
			}
			((JavascriptExecutor) driver).executeScript(TOAST_JS, html, seconds * 1000, failing ? "#ef4444" : "#22c55e");
		} catch (RuntimeException e) {
			logger.debug("Progress toast skipped: {}", e.getMessage());
		}
	}

	private static final String TOAST_JS =
			"var old=document.getElementById('ckyc-progress-toast');if(old)old.remove();"
					+ "var el=document.createElement('div');el.id='ckyc-progress-toast';"
					+ "el.style.cssText='position:fixed;left:16px;bottom:16px;z-index:2147483646;max-width:440px;"
					+ "pointer-events:none;font:13px/1.4 Segoe UI,Arial,sans-serif;color:#fff;background:rgba(17,24,39,.95);"
					+ "border-radius:10px;padding:10px 14px;box-shadow:0 6px 24px rgba(0,0,0,.35);border-left:5px solid '+arguments[2];"
					+ "el.innerHTML=arguments[0];(document.body||document.documentElement).appendChild(el);"
					+ "setTimeout(function(){el.style.transition='opacity .4s';el.style.opacity='0';"
					+ "setTimeout(function(){if(el.parentNode)el.parentNode.removeChild(el);},450);},arguments[1]);";

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String shorten(String s, int max) {
		return s == null ? "" : s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	/** Background steps first, then the steps of the scenario that starts at {@code line}. */
	static List<String> readSteps(URI uri, int line) throws IOException {
		List<String> all = readLines(uri);
		List<String> background = new ArrayList<>();
		List<String> scenario = new ArrayList<>();
		String mode = "";
		for (int i = 0; i < all.size(); i++) {
			String t = all.get(i).trim();
			if (t.startsWith("Background:")) {
				mode = "bg";
			} else if (t.startsWith("Scenario:") || t.startsWith("Scenario Outline:")) {
				mode = (i + 1 == line) ? "sc" : "";
			} else if (t.startsWith("Examples:") || t.startsWith("Rule:")) {
				mode = "";
			} else {
				Matcher m = STEP.matcher(t);
				if (m.matches()) {
					if ("bg".equals(mode)) {
						background.add(m.group(2));
					} else if ("sc".equals(mode)) {
						scenario.add(m.group(2));
					}
				}
			}
		}
		List<String> out = new ArrayList<>(background);
		out.addAll(scenario);
		return out;
	}

	private static List<String> readLines(URI uri) throws IOException {
		String text = uri.toString();
		String path = text.startsWith("classpath:") ? text.substring("classpath:".length()) : text;
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
			if (in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
			}
		}
		Path disk = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", path);
		if (Files.exists(disk)) {
			return Files.readAllLines(disk, StandardCharsets.UTF_8);
		}
		return new ArrayList<>();
	}
}
