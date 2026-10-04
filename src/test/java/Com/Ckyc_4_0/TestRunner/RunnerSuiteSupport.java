/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.CucumberReport;
import Com.Ckyc_4_0.UtilityFiles.DashboardDataExcelReport;
import Com.Ckyc_4_0.UtilityFiles.ExcelReport;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.SuiteCompletionHelper;
import Com.Ckyc_4_0.utils.SuiteCompletionHelper.CompletionChoice;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Shared suite bootstrap for module runners (browser, Extent, timestamped execution log).
 */
final class RunnerSuiteSupport {

	private static final Logger logger = LoggerFactory.getLogger(RunnerSuiteSupport.class);

	/** Ensures Allure results directory is clean before each run — no stale data from prior modules. */
	private static void cleanDirectory(String dirPath) {
		try {
			File dir = new File(dirPath);
			if (dir.exists()) {
				File[] files = dir.listFiles();
				if (files != null) {
					for (File f : files) {
						if (f.isFile()) f.delete();
					}
				}
			} else {
				dir.mkdirs();
			}
		} catch (Exception e) {
			logger.warn("Could not clean allure dir {}: {}", dirPath, e.getMessage());
		}
	}

	/** Copies current run's allure results to target/allure-results so mvn allure:serve shows only this run. */
	private static void copyAllureToTarget() {
		try {
			File src = new File(ReportNamingHelper.allureResultsPath());
			File dest = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + "allure-results");
			dest.mkdirs();
			File[] files = src.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isFile()) {
						Files.copy(f.toPath(), new File(dest, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
				logger.info("Copied {} allure result files to target/allure-results", files.length);
			}
		} catch (IOException e) {
			logger.warn("Could not copy allure results to target: {}", e.getMessage());
		}
	}

	/** Where `allure:report`/`allure:serve` writes its generated HTML (allure-maven plugin default). */
	private static String targetAllureResultsDir() {
		return System.getProperty("user.dir") + File.separator + "target" + File.separator + "allure-results";
	}

	private static String targetAllureReportHistoryDir() {
		return System.getProperty("user.dir") + File.separator + "target" + File.separator + "site"
				+ File.separator + "allure-maven-plugin" + File.separator + "history";
	}

	/**
	 * Cleans target/allure-results for the new run, but first carries the previously-generated
	 * report's "history" folder forward into it — that's what feeds Allure's trend/history graph
	 * (pass/fail over time). Without this, every run's Allure view is an isolated snapshot with
	 * no trend. Only works once a report has been generated at least once (mvn allure:serve /
	 * open-allure.ps1) — before that there is nothing to carry forward, which is fine.
	 */
	private static void preserveAllureHistoryThenClean() {
		String resultsPath = targetAllureResultsDir();
		File historySource = new File(targetAllureReportHistoryDir());
		File resultsHistory = new File(resultsPath, "history");
		File backup = null;
		try {
			if (historySource.exists()) {
				backup = new File(System.getProperty("java.io.tmpdir"), "ckyc-allure-history-" + System.currentTimeMillis());
				FileUtils.copyDirectory(historySource, backup);
			} else if (resultsHistory.exists()) {
				backup = new File(System.getProperty("java.io.tmpdir"), "ckyc-allure-history-" + System.currentTimeMillis());
				FileUtils.copyDirectory(resultsHistory, backup);
			}
		} catch (IOException e) {
			logger.warn("Could not read prior Allure history: {}", e.getMessage());
			backup = null;
		}

		cleanDirectory(resultsPath);

		if (backup != null) {
			try {
				FileUtils.copyDirectory(backup, resultsHistory);
				FileUtils.deleteDirectory(backup);
				logger.info("Allure history carried forward for trend graph");
			} catch (IOException e) {
				logger.warn("Could not restore Allure history: {}", e.getMessage());
			}
		}
	}

	private RunnerSuiteSupport() {
	}

	static void beforeSuite(String runnerName, String modulesDescription) {
		try {
			ReportNamingHelper.initializeRun(runnerName);
			String allureDir = ReportNamingHelper.allureResultsPath();
			cleanDirectory(allureDir);
			// Also refresh default target/allure-results so mvn allure:serve shows only current run,
			// while keeping the trend history from previously generated reports.
			preserveAllureHistoryThenClean();
			System.setProperty("allure.results.directory", allureDir);
			ExecutionLogger.start(runnerName);
			logger.info("Starting suite | runner={} | {}", runnerName, modulesDescription);
			ExecutionLogger.info("Starting suite | runner=" + runnerName + " | " + modulesDescription);
			BaseClass.initialization();
			ExtentReportManager.getReporter();
			logger.info("Browser launched | Password Manager disabled via DriverFactory");
			ExecutionLogger.info("Browser launched successfully");
		} catch (Exception e) {
			logger.error("Suite initialization failed: {}", e.getMessage(), e);
			ExecutionLogger.exception("Suite initialization failed", e);
		}
	}

	static void afterSuite(String runnerName) {
		try {
			logger.info("Generating execution reports | runner={}", runnerName);
			ExecutionLogger.info("Generating reports | runner=" + runnerName);
			ExcelReport.getInstance().saveExcel();
			DashboardDataExcelReport.getInstance().save();
			ExtentReportManager.flush();
			CucumberReport.generateJVMReports(runnerName);
			copyAllureToTarget();
			logger.info("Extent report: {}", ExtentReportManager.getCurrentReportPath());
			logger.info("Excel report: {}", ExcelReport.getInstance().getExcelPath());
			logger.info("Dashboard data Excel: {}", DashboardDataExcelReport.getInstance().getExcelPath());
			logger.info("Allure results: {}", ReportNamingHelper.allureResultsPath());
			logger.info("Allure (target): target/allure-results (run: mvn allure:serve)");
			logger.info("Execution text log: {}", ExecutionLogger.getTextLogPath());
			logger.info("Execution HTML log: {}", ExecutionLogger.getHtmlLogPath());
			ExecutionLogger.info("Extent: " + ExtentReportManager.getCurrentReportPath());
			ExecutionLogger.info("Excel: " + ExcelReport.getInstance().getExcelPath());
			ExecutionLogger.info("Allure: " + ReportNamingHelper.allureResultsPath());
		} catch (Exception e) {
			logger.error("Report generation failed.", e);
			ExecutionLogger.exception("Report generation failed", e);
		} finally {
			Hooks.shutdown();
			WebDriver driver = BaseClass.getDriver();
			try {
				CompletionChoice choice = SuiteCompletionHelper.resolveCompletionChoice(driver, runnerName);
				logger.info("Suite completion choice | runner={} | choice={}", runnerName, choice);
				ExecutionLogger.info("Suite completion choice | runner=" + runnerName + " | choice=" + choice);
			} catch (Exception e) {
				logger.warn("Suite completion/logout sequence failed: {}", e.getMessage());
				ExecutionLogger.warn("Suite completion/logout failed: " + e.getMessage());
			} finally {
				BaseClass.quitBrowser();
			}
			logger.info("Test execution completed | runner={}", runnerName);
		}
	}
}
