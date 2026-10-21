/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.model.Media;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extent (Spark) report, organised the way a reader works through it:
 * <ul>
 * <li>one TEST per scenario, named "[Module &rsaquo; Sub-module] Scenario", with the module / feature / tags as
 *     categories, the tester ({@link ConfigReader#testedBy()}) as author and the browser as device;</li>
 * <li>inside it ONE NODE PER STEP ("Step 3 - ...") holding a table (TC ID, expected, actual, test data, toast,
 *     error) and that step's screenshot, with its own pass / fail status;</li>
 * <li>at the end a step-by-step summary table of the scenario.</li>
 * </ul>
 * Low-level narrative lines are plain log rows of the scenario (no node each), so the list stays short.
 */
public final class ExtentReportManager {

	private static final Logger logger = LoggerFactory.getLogger(ExtentReportManager.class);
	private static ExtentReports extentReports;
	private static String currentReportPath;
	private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
	private static final ThreadLocal<Integer> stepNo = ThreadLocal.withInitial(() -> 0);
	private static final ThreadLocal<List<String[]>> steps = ThreadLocal.withInitial(ArrayList::new);
	private static final ThreadLocal<List<Object[]>> pendingMedia = ThreadLocal.withInitial(ArrayList::new);

	private ExtentReportManager() {
	}

	public static synchronized ExtentReports getReporter() {
		if (extentReports == null) {
			currentReportPath = ReportNamingHelper.extentReportPath(ConfigReader.getExtentReportPath());
			File parent = new File(currentReportPath).getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			String tester = ConfigReader.testedBy();

			ExtentSparkReporter spark = new ExtentSparkReporter(currentReportPath);
			spark.config().setDocumentTitle("iFlowCKYC 4.0 Automation Report - Tested by " + tester);
			spark.config().setReportName("iFlowCKYC 4.0 Execution Report  |  Tested by " + tester);
			spark.config().setTheme(Theme.STANDARD);
			spark.config().setEncoding("utf-8");
			spark.config().setTimelineEnabled(true);
			spark.config().setTimeStampFormat("dd-MMM-yyyy hh:mm:ss a");
			spark.config().enableOfflineMode(true); // works without internet (no CDN)
			spark.config().thumbnailForBase64(true); // screenshots as small thumbnails, click to enlarge
			spark.viewConfigurer().viewOrder().as(new ViewName[] { ViewName.DASHBOARD, ViewName.TEST, ViewName.CATEGORY,
					ViewName.AUTHOR, ViewName.DEVICE, ViewName.EXCEPTION, ViewName.LOG }).apply();

			extentReports = new ExtentReports();
			extentReports.attachReporter(spark);
			extentReports.setSystemInfo("Tested By", tester);
			extentReports.setSystemInfo("Application", "iFlowCKYC 4.0.0");
			extentReports.setSystemInfo("Product", "CKYC Enterprise Console");
			extentReports.setSystemInfo("Environment", ConfigReader.getUrl());
			extentReports.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
			extentReports.setSystemInfo("Runtime Mode", ConfigReader.getRuntimeMode());
			extentReports.setSystemInfo("Runner", ReportNamingHelper.runFolderName());
			extentReports.setSystemInfo("Run Date", ReportNamingHelper.runDate());
			extentReports.setSystemInfo("Run Timestamp", ReportNamingHelper.runTimestamp());
			extentReports.setSystemInfo("Report File", currentReportPath);
			logger.info("Extent report file (new run, not overwritten): {}", currentReportPath);
		}
		return extentReports;
	}

	public static String getCurrentReportPath() {
		return currentReportPath;
	}

	public static ExtentTest startTest(String testName, String description) {
		return startTest(testName, description, "", List.of());
	}

	/** One test per scenario: module / feature / tags as categories, tester as author, browser as device. */
	public static ExtentTest startTest(String testName, String feature, String uri, List<String> tags) {
		flushPending();
		String[] mod = AdvancedRunReport.moduleOf(uri, feature);
		String module = mod[0];
		String sub = mod[1];
		String display = "[" + module + (sub.isEmpty() ? "" : " › " + sub) + "] " + testName;
		String desc = "Feature: " + (feature == null ? "N/A" : feature) + "  |  Module: " + module
				+ (sub.isEmpty() ? "" : " › " + sub) + "  |  Tested by: " + ConfigReader.testedBy();
		ExtentTest test = getReporter().createTest(display, desc);
		Set<String> cats = new LinkedHashSet<>();
		cats.add(module);
		if (feature != null && !feature.isBlank()) {
			cats.add(feature);
		}
		if (tags != null) {
			for (String t : tags) {
				if (t != null && !t.isBlank()) {
					cats.add(t);
				}
			}
		}
		test.assignCategory(cats.toArray(new String[0]));
		test.assignAuthor(ConfigReader.testedBy());
		test.assignDevice(ConfigReader.get("browser", "chrome"));
		String when = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").format(new java.util.Date());
		test.info(MarkupHelper.createTable(new String[][] {
				{ "Tested by", ConfigReader.testedBy() },
				{ "Module", module + (sub.isEmpty() ? "" : " › " + sub) },
				{ "Feature", feature == null ? "N/A" : feature },
				{ "Scenario", testName },
				{ "Tags", tags == null || tags.isEmpty() ? "-" : String.join(", ", tags) },
				{ "Started", when },
				{ "Browser", ConfigReader.get("browser", "chrome") },
				{ "Environment", ConfigReader.getUrl() } }));
		extentTest.set(test);
		stepNo.set(0);
		steps.get().clear();
		return test;
	}

	public static ExtentTest getTest() {
		return extentTest.get();
	}

	// ------------------------------------------------------------------ steps

	/**
	 * One executed step as ONE node: details table + the screenshot taken for it + its own status. Called from the
	 * single step-logging funnel, so every module is covered.
	 */
	public static void recordStep(String tcId, String step, String expected, String actual, String status, String error,
			String testData, String toast, String screenshotFiles) {
		ExtentTest test = getTest();
		if (test == null || "Scenario Summary".equals(step)) {
			return;
		}
		int n = stepNo.get() + 1;
		stepNo.set(n);
		String st = status == null ? "" : status;
		boolean failed = st.equalsIgnoreCase("failed");
		boolean skipped = st.equalsIgnoreCase("skipped") || st.equalsIgnoreCase("aborted");
		ExtentTest node = test.createNode((failed ? "✘ " : skipped ? "○ " : "✔ ") + "Step " + n + " - "
				+ shorten(step, 110));
		List<String[]> rows = new ArrayList<>();
		rows.add(new String[] { "Step", nz(step) });
		if (!nz(tcId).isEmpty()) {
			rows.add(new String[] { "TC ID", tcId });
		}
		rows.add(new String[] { "Expected result", nz(expected) });
		rows.add(new String[] { "Actual result", nz(actual) });
		if (!blank(testData)) {
			rows.add(new String[] { "Test data", testData });
		}
		if (!blank(toast)) {
			rows.add(new String[] { "Toast / message", toast });
		}
		if (failed && !blank(error)) {
			rows.add(new String[] { "Error", error });
		}
		rows.add(new String[] { "Tested by", ConfigReader.testedBy() });
		node.info(MarkupHelper.createTable(rows.toArray(new String[0][])));

		List<Object[]> media = pendingMedia.get();
		boolean attached = false;
		for (Object[] m : media) {
			attached |= logMedia(node, (String) m[0], (byte[]) m[1], (Boolean) m[2]);
		}
		media.clear();
		if (!attached && !blank(screenshotFiles)) {
			for (String f : screenshotFiles.split("[,;|]")) {
				File file = new File(ReportNamingHelper.screenshotDir(), f.trim());
				if (file.exists()) {
					try {
						attached |= logMedia(node, f.trim(), Files.readAllBytes(file.toPath()), !failed);
					} catch (Exception e) {
						logger.debug("Screenshot {} could not be read for Extent: {}", f, e.getMessage());
					}
				}
			}
		}
		if (failed) {
			node.fail("Step failed" + (blank(error) ? "" : ": " + error));
		} else if (skipped) {
			node.skip("Step " + st.toLowerCase());
		} else {
			node.pass("Step passed");
		}
		steps.get().add(new String[] { String.valueOf(n), nz(step), failed ? "FAILED" : skipped ? "SKIPPED" : "PASSED",
				nz(expected), nz(actual) });
	}

	/** A screenshot taken during a step: kept until the step is recorded, then attached to that step's node. */
	public static void addMedia(String title, byte[] png, boolean success) {
		if (png == null) {
			return;
		}
		pendingMedia.get().add(new Object[] { title, png, success });
	}

	private static void flushPending() {
		ExtentTest test = getTest();
		List<Object[]> media = pendingMedia.get();
		if (test != null) {
			for (Object[] m : media) {
				logMedia(test, (String) m[0], (byte[]) m[1], (Boolean) m[2]);
			}
		}
		media.clear();
	}

	private static boolean logMedia(ExtentTest target, String title, byte[] png, boolean success) {
		try {
			Media media = MediaEntityBuilder.createScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(png), title)
					.build();
			if (success) {
				target.pass(title, media);
			} else {
				target.fail(title, media);
			}
			return true;
		} catch (Exception e) {
			logger.debug("Extent screenshot attach failed for '{}': {}", title, e.getMessage());
			return false;
		}
	}

	/** Closes the scenario: leftover screenshots, the step-by-step summary table and the scenario verdict. */
	public static void endTest(String status, String reason) {
		ExtentTest test = getTest();
		if (test == null) {
			return;
		}
		flushPending();
		List<String[]> list = steps.get();
		if (!list.isEmpty()) {
			String[][] table = new String[list.size() + 1][];
			table[0] = new String[] { "#", "Step", "Result", "Expected result", "Actual result" };
			for (int i = 0; i < list.size(); i++) {
				table[i + 1] = list.get(i);
			}
			test.info(MarkupHelper.createTable(table));
		}
		String tested = " | Tested by " + ConfigReader.testedBy();
		if ("Failed".equalsIgnoreCase(status) || "Aborted".equalsIgnoreCase(status)) {
			test.fail("Scenario " + status.toLowerCase() + (blank(reason) ? "" : ": " + reason) + tested);
		} else if ("Skipped".equalsIgnoreCase(status)) {
			test.skip("Scenario skipped" + (blank(reason) ? "" : ": " + reason) + tested);
		} else {
			test.pass("Scenario passed" + tested);
		}
		list.clear();
		stepNo.set(0);
	}

	// ------------------------------------------------------------------ narrative lines

	/** Narrative line of the scenario (no node). Steps are shown by {@link #recordStep}, so those lines are skipped. */
	public static void logInfo(String message) {
		ExtentTest test = getTest();
		if (test != null && !duplicatesStep(message)) {
			test.info(message);
		}
		logger.info(message);
	}

	public static void logPass(String message) {
		ExtentTest test = getTest();
		if (test != null && !duplicatesStep(message)) {
			test.pass(message);
		}
		logger.info("PASS: {}", message);
	}

	public static void logFail(String message) {
		ExtentTest test = getTest();
		if (test != null && !duplicatesStep(message)) {
			test.fail(message);
		}
		logger.error("FAIL: {}", message);
	}

	private static boolean duplicatesStep(String message) {
		return message != null && message.contains(" | Expected: ") && message.contains(" | Actual: ");
	}

	public static void flush() {
		if (extentReports != null) {
			extentReports.flush();
			logger.info("Extent report saved: {}", currentReportPath);
		}
	}

	/** Records total run duration in the Extent system-info panel. Call before {@link #flush()}. */
	public static void setTotalDuration(String elapsed) {
		if (extentReports != null) {
			extentReports.setSystemInfo("Total Time Taken", elapsed);
			extentReports.setSystemInfo("Test execution time", ReportNamingHelper.executionElapsedFormatted());
			extentReports.setSystemInfo("Started", ReportNamingHelper.startedFormatted());
			String video = Com.Ckyc_4_0.utils.recording.RunFinale.recordingPath();
			if (!video.isEmpty()) {
				extentReports.setSystemInfo("Screen recording", video);
			}
			extentReports.setSystemInfo("What was tested", AdvancedRunReport.selectionText());
		}
	}

	static void attachScreenshotBase64(ExtentTest test, String message, String screenshotPath, boolean success) {
		if (test == null || screenshotPath == null) {
			return;
		}
		try {
			byte[] bytes = Files.readAllBytes(new File(screenshotPath).toPath());
			if (!logMedia(test, message, bytes, success)) {
				throw new IllegalStateException("not attached");
			}
		} catch (Exception e) {
			if (success) {
				test.pass(message + " | Screenshot: " + screenshotPath);
			} else {
				test.fail(message + " | Screenshot: " + screenshotPath);
			}
		}
	}

	private static String shorten(String text, int max) {
		String t = text == null ? "" : text.trim();
		int pipe = t.indexOf(" | ");
		if (pipe > 0) {
			t = t.substring(0, pipe);
		}
		return t.length() > max ? t.substring(0, max) + "…" : t;
	}

	private static boolean blank(String s) {
		return s == null || s.isBlank() || "-".equals(s.trim());
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
