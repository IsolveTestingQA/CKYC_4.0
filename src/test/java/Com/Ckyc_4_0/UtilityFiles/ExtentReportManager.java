/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public final class ExtentReportManager {

	private static final Logger logger = LoggerFactory.getLogger(ExtentReportManager.class);
	private static ExtentReports extentReports;
	private static String currentReportPath;
	private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

	private ExtentReportManager() {
	}

	public static synchronized ExtentReports getReporter() {
		if (extentReports == null) {
			currentReportPath = ReportNamingHelper.extentReportPath(ConfigReader.getExtentReportPath());
			File parent = new File(currentReportPath).getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			ExtentSparkReporter spark = new ExtentSparkReporter(currentReportPath);
			spark.config().setDocumentTitle("CKYC 4.0 Automation Report");
			spark.config().setReportName("iFlowCKYC 4.0 Execution Report");
			spark.config().setTheme(Theme.STANDARD);

			extentReports = new ExtentReports();
			extentReports.attachReporter(spark);
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
		String displayName = (description != null && !description.isBlank())
				? description + " → " + testName
				: testName;
		ExtentTest test = getReporter().createTest(displayName);
		if (description != null && !description.isBlank()) {
			test.assignCategory(description);
		}
		ExtentTest infoNode = test.createNode("Scenario Details");
		infoNode.info("<b>Feature:</b> " + (description == null ? "N/A" : description));
		infoNode.info("<b>Scenario:</b> " + testName);
		infoNode.info("<b>Started:</b> " + new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").format(new java.util.Date()));
		extentTest.set(test);
		return test;
	}

	public static ExtentTest getTest() {
		return extentTest.get();
	}

	public static void logInfo(String message) {
		ExtentTest test = getTest();
		if (test != null) {
			ExtentTest node = test.createNode("ℹ " + truncate(message, 80));
			node.info(message);
		}
		logger.info(message);
	}

	public static void logPass(String message) {
		ExtentTest test = getTest();
		if (test != null) {
			ExtentTest node = test.createNode("✔ " + truncate(message, 80));
			node.pass(message);
		}
		logger.info("PASS: {}", message);
	}

	public static void logFail(String message) {
		ExtentTest test = getTest();
		if (test != null) {
			ExtentTest node = test.createNode("✘ " + truncate(message, 80));
			node.fail(message);
		}
		logger.error("FAIL: {}", message);
	}

	private static String truncate(String text, int maxLen) {
		if (text == null) return "";
		int pipeIdx = text.indexOf('|');
		String title = pipeIdx > 0 ? text.substring(0, pipeIdx).trim() : text;
		return title.length() > maxLen ? title.substring(0, maxLen) + "…" : title;
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
		}
	}

	static void attachScreenshotBase64(ExtentTest test, String message, String screenshotPath, boolean success) {
		if (test == null || screenshotPath == null) {
			return;
		}
		try {
			byte[] bytes = Files.readAllBytes(new File(screenshotPath).toPath());
			String base64 = Base64.getEncoder().encodeToString(bytes);
			var media = MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build();
			if (success) {
				test.pass(message, media);
			} else {
				test.fail(message, media);
			}
		} catch (Exception e) {
			if (success) {
				test.pass(message + " | Screenshot: " + screenshotPath);
			} else {
				test.fail(message + " | Screenshot: " + screenshotPath);
			}
		}
	}
}
