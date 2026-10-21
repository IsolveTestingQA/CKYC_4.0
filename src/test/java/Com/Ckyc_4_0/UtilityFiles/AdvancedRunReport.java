/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import Com.Ckyc_4_0.utils.reports.JsonText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The advanced end-of-run HTML report for ALL modules of the iFlowCKYC automation. Every scenario, every step with
 * Expected / Actual / result / test data / toast and its screenshot is collected as the run goes (hooked at the
 * scenario start / end, at the single step-logging funnel and at screenshot capture), and one self-contained page is
 * written when the run ends. "Tested by" is printed from {@link ConfigReader#testedBy()}.
 */
public final class AdvancedRunReport {

	private static final Logger logger = LoggerFactory.getLogger(AdvancedRunReport.class);
	private static final AdvancedRunReport INSTANCE = new AdvancedRunReport();
	private static final String SUMMARY_STEP = "Scenario Summary";

	private static final class Step {
		int no;
		String tc = "", desc = "", expected = "", actual = "", status = "", error = "", data = "", toast = "", time = "";
		final List<String[]> shots = new ArrayList<>();
	}

	private static final class Scen {
		String feature = "", module = "", sub = "", name = "", uri = "", status = "Running", reason = "";
		final List<String> tags = new ArrayList<>();
		long startMs;
		long endMs;
		final List<Step> steps = new ArrayList<>();
		final List<String[]> shots = new ArrayList<>();
	}

	private final List<Scen> scenarios = new ArrayList<>();
	private Scen current;
	/** Screenshots taken since the last logged step: they belong to the next test case that is logged. */
	private final List<String[]> pending = new ArrayList<>();
	private String writtenTo = "";

	private AdvancedRunReport() {
	}

	public static AdvancedRunReport get() {
		return INSTANCE;
	}

	public String writtenTo() {
		return writtenTo;
	}

	// ------------------------------------------------------------------ collection

	public synchronized void startScenario(String feature, String name, List<String> tags, String uri) {
		Scen s = new Scen();
		s.feature = feature == null ? "" : feature;
		s.name = name == null ? "" : name;
		s.uri = uri == null ? "" : uri;
		if (tags != null) {
			s.tags.addAll(tags);
		}
		String[] mod = moduleOf(s.uri, s.feature);
		s.module = mod[0];
		s.sub = mod[1];
		s.startMs = System.currentTimeMillis();
		scenarios.add(s);
		current = s;
		pending.clear();
	}

	public synchronized void endScenario(String status, String reason) {
		if (current == null) {
			return;
		}
		current.status = status == null ? "Passed" : status;
		current.reason = reason == null ? "" : reason;
		current.endMs = System.currentTimeMillis();
		current = null;
		pending.clear();
	}

	/** One step as written to the Excel report (called from {@code ExcelReport.logStepDetailed}). */
	public synchronized void step(String feature, String scenario, String tcId, String desc, String expected, String actual,
			String status, String error, String testData, String toast, String screenshotFiles) {
		if (SUMMARY_STEP.equals(desc)) {
			return;
		}
		Scen s = current;
		if (s == null) {
			s = new Scen();
			s.feature = feature == null ? "General" : feature;
			s.name = scenario == null ? "General" : scenario;
			String[] mod = moduleOf("", s.feature);
			s.module = mod[0];
			s.sub = mod[1];
			s.startMs = System.currentTimeMillis();
			s.endMs = s.startMs;
			s.status = "Passed";
			scenarios.add(s);
		}
		Step st = new Step();
		st.no = s.steps.size() + 1;
		st.tc = nz(tcId);
		st.desc = nz(desc);
		st.expected = nz(expected);
		st.actual = nz(actual);
		st.status = nz(status);
		st.error = "-".equals(error) ? "" : nz(error);
		st.data = "-".equals(testData) ? "" : nz(testData);
		st.toast = "-".equals(toast) ? "" : nz(toast);
		st.time = new SimpleDateFormat("HH:mm:ss").format(new Date());
		if (screenshotFiles != null && !screenshotFiles.isBlank() && !"-".equals(screenshotFiles)) {
			for (String f : screenshotFiles.split("[,;|]")) {
				String name = f.trim();
				if (!name.isEmpty()) {
					st.shots.add(new String[] { name, "failed".equalsIgnoreCase(status) ? "Failure evidence" : "Evidence", "" });
				}
			}
		}
		for (String[] pe : pending) {
			String fileName = new java.io.File(pe[1]).getName();
			boolean already = false;
			for (String[] have : st.shots) {
				if (new java.io.File(have[0]).getName().equals(fileName)) {
					already = true;
					break;
				}
			}
			if (!already) {
				st.shots.add(new String[] { pe[1], pe[0].isBlank() ? "Evidence" : pe[0], "" });
			}
		}
		pending.clear();
		s.steps.add(st);
	}

	/** A screenshot taken while a scenario is running (milestones, failure pictures, evidence). */
	public synchronized void screenshot(String title, String path, boolean ok) {
		if (path == null || path.isBlank()) {
			return;
		}
		Scen s = current;
		if (s == null && !scenarios.isEmpty()) {
			s = scenarios.get(scenarios.size() - 1);
		}
		if (s != null) {
			s.shots.add(new String[] { nz(title), path, ok ? "1" : "0" });
			pending.add(new String[] { nz(title), path });
		}
	}

	// ------------------------------------------------------------------ output

	public synchronized Path write() {
		try {
			Path dir = Paths.get(ReportNamingHelper.reportOutputRoot(), "Advanced Report", ReportNamingHelper.runDate(),
					ReportNamingHelper.runFolderName());
			Files.createDirectories(dir);
			Path html = dir.resolve("CKYC_Advanced_Report_" + ReportNamingHelper.runTimestamp() + ".html");
			String template;
			try (InputStream in = AdvancedRunReport.class.getResourceAsStream("/Reports/advanced_report_template.html")) {
				if (in == null) {
					throw new IOException("Reports/advanced_report_template.html not found on the classpath");
				}
				template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			Files.writeString(html, template.replace("__DATA__", data(html)), StandardCharsets.UTF_8);
			writtenTo = html.toAbsolutePath().toString();
			logger.info("Advanced HTML report: {}", writtenTo);
			return html;
		} catch (IOException | RuntimeException e) {
			logger.warn("Advanced HTML report could not be written: {}", e.getMessage());
			return null;
		}
	}

	private String data(Path html) {
		boolean embed = Boolean.parseBoolean(ConfigReader.get("advancedReport.embedScreenshots", "false"));
		StringBuilder sb = new StringBuilder(16384);
		sb.append("{\"title\":").append(JsonText.quote("iFlowCKYC 4.0 - Automation Execution Report"));
		sb.append(",\"testedBy\":").append(JsonText.quote(ConfigReader.testedBy()));
		sb.append(",\"generated\":").append(JsonText.quote(new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").format(new Date())));
		sb.append(",\"duration\":").append(JsonText.quote(ReportNamingHelper.elapsedFormatted()));
		sb.append(",\"runner\":").append(JsonText.quote(ReportNamingHelper.runFolderName()));
		sb.append(",\"execDuration\":").append(JsonText.quote(ReportNamingHelper.executionElapsedFormatted()));
		sb.append(",\"started\":").append(JsonText.quote(ReportNamingHelper.startedFormatted()));
		sb.append(",\"final\":").append(finalBlock(html));
		sb.append(",\"files\":").append(filesBlock(html));
		sb.append(",\"env\":[");
		String[][] env = { { "Application", "iFlowCKYC 4.0.0" }, { "URL", ConfigReader.getUrl() },
				{ "Browser", ConfigReader.get("browser", "chrome") }, { "Runtime mode", ConfigReader.getRuntimeMode() },
				{ "What was tested", selectionText() },
				{ "Run date", ReportNamingHelper.runDate() }, { "Run id", ReportNamingHelper.runTimestamp() },
				{ "Operating system", System.getProperty("os.name", "") }, { "Java", System.getProperty("java.version", "") } };
		for (int i = 0; i < env.length; i++) {
			sb.append(i == 0 ? "" : ",").append('[').append(JsonText.quote(env[i][0])).append(',')
					.append(JsonText.quote(env[i][1])).append(']');
		}
		sb.append("],\"scenarios\":[");
		int id = 0;
		for (Scen s : scenarios) {
			sb.append(id == 0 ? "" : ",\n");
			id++;
			long dur = (s.endMs > 0 ? s.endMs : System.currentTimeMillis()) - s.startMs;
			sb.append("{\"id\":\"S").append(id).append('"');
			sb.append(",\"module\":").append(JsonText.quote(s.module));
			sb.append(",\"sub\":").append(JsonText.quote(s.sub));
			sb.append(",\"feature\":").append(JsonText.quote(s.feature));
			sb.append(",\"name\":").append(JsonText.quote(s.name));
			sb.append(",\"status\":").append(JsonText.quote(s.status));
			sb.append(",\"reason\":").append(JsonText.quote(s.reason));
			sb.append(",\"ms\":").append(Math.max(0, dur));
			sb.append(",\"start\":").append(JsonText.quote(s.startMs == 0 ? "" : new SimpleDateFormat("HH:mm:ss").format(new Date(s.startMs))));
			sb.append(",\"tags\":[");
			for (int i = 0; i < s.tags.size(); i++) {
				sb.append(i == 0 ? "" : ",").append(JsonText.quote(s.tags.get(i)));
			}
			sb.append("],\"steps\":[");
			for (int i = 0; i < s.steps.size(); i++) {
				Step st = s.steps.get(i);
				sb.append(i == 0 ? "" : ",");
				sb.append("{\"no\":").append(st.no).append(",\"tc\":").append(JsonText.quote(st.tc))
						.append(",\"desc\":").append(JsonText.quote(st.desc))
						.append(",\"exp\":").append(JsonText.quote(cut(st.expected)))
						.append(",\"act\":").append(JsonText.quote(cut(st.actual)))
						.append(",\"status\":").append(JsonText.quote(st.status))
						.append(",\"err\":").append(JsonText.quote(cut(st.error)))
						.append(",\"data\":").append(JsonText.quote(cut(st.data)))
						.append(",\"toast\":").append(JsonText.quote(cut(st.toast)))
						.append(",\"time\":").append(JsonText.quote(st.time)).append(",\"shots\":[");
				boolean first = true;
				for (String[] sh : st.shots) {
					java.io.File shotFile = new java.io.File(sh[0]);
						if (!shotFile.isAbsolute()) {
							shotFile = new java.io.File(ReportNamingHelper.screenshotDir(), sh[0]);
						}
						String href = href(html, shotFile.toPath(), embed);
					if (href.isEmpty()) {
						continue;
					}
					sb.append(first ? "" : ",").append("{\"t\":").append(JsonText.quote(sh[1])).append(",\"h\":")
							.append(JsonText.quote(href)).append('}');
					first = false;
				}
				sb.append("]}");
			}
			sb.append("],\"shots\":[");
			boolean first = true;
			for (String[] sh : s.shots) {
				String href = href(html, Paths.get(sh[1]), embed);
				if (href.isEmpty()) {
					continue;
				}
				sb.append(first ? "" : ",").append("{\"t\":").append(JsonText.quote(sh[0])).append(",\"h\":")
						.append(JsonText.quote(href)).append(",\"ok\":").append("1".equals(sh[2])).append('}');
				first = false;
			}
			sb.append("]}");
		}
		sb.append("]}");
		return sb.toString().replace("</", "<\\/");
	}

	/** The picture of the browser window after completion (embedded), the full-screen picture and the video (links). */
	private static String finalBlock(Path html) {
		String shot = Com.Ckyc_4_0.utils.recording.RunFinale.finalWindowShot();
		String screen = Com.Ckyc_4_0.utils.recording.RunFinale.finalScreenShot();
		String video = Com.Ckyc_4_0.utils.recording.RunFinale.recordingPath();
		return "{\"shot\":" + JsonText.quote(shot.isEmpty() ? "" : href(html, Paths.get(shot), true))
				+ ",\"screen\":" + JsonText.quote(screen.isEmpty() ? "" : href(html, Paths.get(screen), false))
				+ ",\"recording\":" + JsonText.quote(video.isEmpty() ? "" : href(html, Paths.get(video), false)) + "}";
	}

	/** Links to every result file of the run: [label, relative link, shown name]. */
	private static String filesBlock(Path html) {
		List<String[]> rows = new ArrayList<>();
		rows.add(new String[] { "Excel execution report", safePath(() -> ExcelReport.getInstance().getExcelPath()) });
		rows.add(new String[] { "Dashboard data Excel", safePath(() -> DashboardDataExcelReport.getInstance().getExcelPath()) });
		rows.add(new String[] { "Extent report", safePath(ExtentReportManager::getCurrentReportPath) });
		rows.add(new String[] { "Cucumber report", safePath(CucumberReport::lastIndex) });
		rows.add(new String[] { "Execution log (HTML)", safePath(ExecutionLogger::getHtmlLogPath) });
		rows.add(new String[] { "Execution log (text)", safePath(ExecutionLogger::getTextLogPath) });
		rows.add(new String[] { "Screenshots folder", ReportNamingHelper.screenshotDir() });
		rows.add(new String[] { "Screen recording", Com.Ckyc_4_0.utils.recording.RunFinale.recordingPath() });
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		for (String[] r : rows) {
			if (r[1] == null || r[1].isBlank() || !Files.exists(Paths.get(r[1]))) {
				continue;
			}
			String name = Paths.get(r[1]).getFileName() == null ? r[1] : Paths.get(r[1]).getFileName().toString();
			sb.append(first ? "" : ",").append('[').append(JsonText.quote(r[0])).append(',')
					.append(JsonText.quote(href(html, Paths.get(r[1]), false))).append(',').append(JsonText.quote(name)).append(']');
			first = false;
		}
		return sb.append(']').toString();
	}

	private static String safePath(java.util.function.Supplier<String> s) {
		try {
			String v = s.get();
			return v == null ? "" : v;
		} catch (RuntimeException e) {
			return "";
		}
	}

	/** Relative link from the report to the screenshot (or an embedded data URI); "" when the file is missing. */
	private static String href(Path html, Path file, boolean embed) {
		try {
			if (!Files.exists(file)) {
				return "";
			}
			if (embed) {
				return "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file));
			}
			return html.getParent().toAbsolutePath().relativize(file.toAbsolutePath()).toString().replace('\\', '/');
		} catch (IOException | RuntimeException e) {
			return file.toUri().toString();
		}
	}

	/** [module, sub-module] from the feature path: .../Features/4_SearchAndDownload/1_Search/x.feature. */
	static String[] moduleOf(String uri, String feature) {
		String path = uri == null ? "" : uri.replace('\\', '/');
		int i = path.indexOf("/Features/");
		if (i >= 0) {
			String[] parts = path.substring(i + "/Features/".length()).split("/");
			String module = pretty(parts.length > 1 ? parts[0] : feature);
			String sub = parts.length > 2 ? pretty(parts[1]) : "";
			return new String[] { module, sub };
		}
		return new String[] { pretty(feature), "" };
	}

	private static String pretty(String raw) {
		String s = raw == null ? "" : raw.replaceAll("\\.feature$", "").replaceAll("^[0-9]+_", "").replace('_', ' ');
		s = s.replaceAll("(?<=[a-z])(?=[A-Z])", " ").trim();
		return s.isEmpty() ? "General" : s;
	}

	private static String cut(String s) {
		return s != null && s.length() > 4000 ? s.substring(0, 4000) + " ... (" + s.length() + " chars)" : nz(s);
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	/** The Test Control Panel choice of this run, or a note that the runner's own defaults applied. */
	static String selectionText() {
		Com.Ckyc_4_0.utils.panel.RunSelection sel = Com.Ckyc_4_0.utils.panel.RunSelection.current();
		return sel == null ? "Runner defaults (Test Control Panel not used)" : sel.summary();
	}

	/** Used by the self-test only. */
	static String lower(String s) {
		return s.toLowerCase(Locale.ROOT);
	}
}
