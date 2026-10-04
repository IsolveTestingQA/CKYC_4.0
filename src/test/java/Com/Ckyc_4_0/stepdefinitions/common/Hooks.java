/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.common;



import Com.Ckyc_4_0.UtilityFiles.BaseClass;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;

import Com.Ckyc_4_0.UtilityFiles.ExcelReport;

import Com.Ckyc_4_0.UtilityFiles.DashboardDataExcelReport;

import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;

import Com.Ckyc_4_0.UtilityFiles.ScreenshotManager;

import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.UtilityFiles.UserManagementStore;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.PopupHandler;
import Com.Ckyc_4_0.utils.ToastHandler;
import Com.Ckyc_4_0.utils.PopupHandler.AbortRunException;
import Com.Ckyc_4_0.utils.PopupHandler.BlockerChoice;
import Com.Ckyc_4_0.utils.PopupHandler.ModuleBlockerException;
import Com.Ckyc_4_0.utils.RetryContext;
import Com.Ckyc_4_0.utils.ScenarioPageGuard;

import io.cucumber.java.After;

import io.cucumber.java.Before;

import io.cucumber.java.BeforeStep;

import io.cucumber.java.Scenario;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.testng.SkipException;



import java.io.File;



import static Com.Ckyc_4_0.UtilityFiles.BaseClass.getDriver;

import static Com.Ckyc_4_0.UtilityFiles.BaseClass.initialization;

import static Com.Ckyc_4_0.UtilityFiles.BaseClass.quitBrowser;



public class Hooks extends BaseClass {



	private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

	private static final ExcelReport reporter = ExcelReport.getInstance();

	private static final ThreadLocal<Scenario> currentScenario = new ThreadLocal<>();



	private static class HooksHolder {

		private static long stepStartTime;

		private static String featureName;

		private static String scenarioName;

		private static String currentStepDescription;

	}



	@Before(order = 0)

	public void ensureBrowserStarted() {

		if (getDriver() == null) {

			synchronized (Hooks.class) {

				if (getDriver() == null) {

					initialization();

				}

			}

		}

	}



	@Before(order = 1)

	public void beforeScenario(Scenario scenario) {

		currentScenario.set(scenario);

		int attempt = RetryContext.consumeAttempt();
		HooksHolder.scenarioName = attempt > 1 ? scenario.getName() + " [Attempt " + attempt + "]" : scenario.getName();

		HooksHolder.featureName = getFeatureName(scenario);

		if (SoftAssertManager.shouldAbortRun()) {
			throw new SkipException("Run aborted: " + SoftAssertManager.abortReason());
		}
		if (SoftAssertManager.shouldSkipFeature(HooksHolder.featureName)) {
			throw new SkipException("Feature skipped: " + SoftAssertManager.skipFeatureReason());
		}

		SoftAssertManager.clear();

		if (scenario.getSourceTagNames().contains("@UserManagement")) {
			UserManagementStore.clear();
		}

		ExtentReportManager.startTest(HooksHolder.scenarioName, HooksHolder.featureName);

		logger.info("=== Starting Scenario: {} ({}) ===", HooksHolder.scenarioName, HooksHolder.featureName);
		ExecutionLogger.info("=== Starting Scenario: " + HooksHolder.scenarioName
				+ " (" + HooksHolder.featureName + ") ===");

		// After skip / previous scenario: open side-nav only if module names are not visible.
		try {
			if (getDriver() != null) {
				String url = getDriver().getCurrentUrl();
				if (url != null && !url.toLowerCase().contains("/login")) {
					MasterUiHelper.ensureSideNavOpen(getDriver(), 8);
					ExecutionLogger.info("Pre-scenario nav check | url=" + url + " | side-nav prepared");
				}
			}
		} catch (Exception e) {
			logger.debug("Pre-scenario side-nav open check skipped: {}", e.getMessage());
			ExecutionLogger.warn("Pre-scenario nav check skipped: " + e.getMessage());
		}

	}



	@BeforeStep

	public void beforeStep() {

		HooksHolder.stepStartTime = System.currentTimeMillis();

	}



	@After

	public void afterScenario(Scenario scenario) {

		long durationMs = System.currentTimeMillis() - HooksHolder.stepStartTime;

		boolean softFailed = SoftAssertManager.hasFailures();
		boolean skippedOrAborted = SoftAssertManager.shouldSkipScenario()
				|| SoftAssertManager.shouldAbortRun()
				|| SoftAssertManager.shouldSkipFeature(HooksHolder.featureName);

		try {
			if (!skippedOrAborted && getDriver() != null) {
				ScenarioPageGuard.verifyExpectedEndpoint(
						getDriver(), HooksHolder.featureName, HooksHolder.scenarioName);
				softFailed = SoftAssertManager.hasFailures();
			}
			if (skippedOrAborted) {
				// User chose Continue wait / Skip / Stop — do not hard-fail leftover soft asserts
				SoftAssertManager.clearFailures();
				logger.info("Scenario ended via skip/abort — soft failures cleared: {}", scenario.getName());
			} else if (softFailed) {
				String desc = "Scenario failed - final state | " + scenario.getName();
				ScreenshotManager.captureAndAttach(getDriver(), scenario, desc, false);
				SoftAssertManager.assertAll();
			}
		} catch (AssertionError e) {
			logger.error("Scenario completed with failures (all steps were executed): {}", scenario.getName(), e);
			if (!softFailed) {
				ScreenshotManager.captureAndAttach(getDriver(), scenario,
						"Scenario assertion failed | " + scenario.getName(), false);
			}
			throw e;
		} finally {
			recoverBrowserForNextScenario(scenario);

			String status;
			if (SoftAssertManager.shouldAbortRun()) {
				status = "Aborted";
			} else if (skippedOrAborted) {
				status = "Skipped";
			} else if (scenario.isFailed() || softFailed) {
				status = "Failed";
			} else {
				status = "Passed";
			}

			if ("Failed".equals(status) || "Aborted".equals(status)) {
				ExtentReportManager.logFail("Scenario " + status.toLowerCase() + ": " + scenario.getName());
			} else if ("Skipped".equals(status)) {
				ExtentReportManager.logInfo("Scenario skipped: " + scenario.getName()
						+ " — " + SoftAssertManager.skipReason());
			} else {
				ExtentReportManager.logPass("Scenario passed: " + scenario.getName());
			}

			reporter.logStep(HooksHolder.featureName, HooksHolder.scenarioName, "Scenario Summary",
					"All steps executed", status, status, HooksHolder.stepStartTime,
					"Failed".equals(status) ? "One or more steps failed"
							: ("Skipped".equals(status) || "Aborted".equals(status))
									? SoftAssertManager.skipReason()
									: "-");

			logger.info("=== Scenario {}: {} | Duration: {} ms ===", status, scenario.getName(), durationMs);
			if ("Failed".equals(status) || "Aborted".equals(status)) {
				ExecutionLogger.fail("Scenario " + status + ": " + scenario.getName());
			} else if ("Skipped".equals(status)) {
				ExecutionLogger.warn("Scenario skipped: " + scenario.getName());
			} else {
				ExecutionLogger.pass("Scenario passed: " + scenario.getName());
			}
			currentScenario.remove();
		}

	}



	/**

	 * Runs a step; captures screenshot on failure. Set captureOnSuccess=true for module-entry milestones (e.g. dashboard).

	 */

	public static void runStep(String stepDescription, String expected, Runnable action) {

		runStep(stepDescription, expected, action, false, false);

	}



	/** @param captureOnSuccess screenshot on pass; @param mandatory true = Missing-UI interactive prompt allowed */
	public static void runStep(String stepDescription, String expected, Runnable action, boolean captureOnSuccess) {
		runStep(stepDescription, expected, action, captureOnSuccess, false);
	}

	/**
	 * @param mandatory only mandatory/blocker steps may show the interactive Missing-UI popup;
	 *                  soft/optional failures just soft-assert (SweetAlert blockers still use Stop/Retry).
	 */
	public static void runStep(String stepDescription, String expected, Runnable action,
			boolean captureOnSuccess, boolean mandatory) {
		HooksHolder.currentStepDescription = stepDescription;
		Scenario scenario = currentScenario.get();
		ExecutionLogger.info("STEP START | " + stepDescription + " | Expected: " + expected
				+ (mandatory ? " | mandatory=true" : ""));
		if (SoftAssertManager.shouldAbortRun()) {
			throw new SkipException("Run aborted: " + SoftAssertManager.abortReason());
		}
		if (SoftAssertManager.shouldSkipScenario()) {
			throw new SkipException("Scenario skipped: " + SoftAssertManager.skipScenarioReason());
		}
		try {
			PopupHandler.runWithBlockerRetry(getDriver(), action);
			markStepPassed(stepDescription, expected, captureOnSuccess, scenario);
			ExecutionLogger.pass(stepDescription + " | Actual: " + expected);
		} catch (AbortRunException abort) {
			ExecutionLogger.exception("Abort on step: " + stepDescription, abort);
			handleAbortAndStop(abort);
		} catch (ModuleBlockerException blocker) {
			String details = "Mandatory positive creation failed after valid retries."
					+ System.lineSeparator() + "Module: " + blocker.module()
					+ System.lineSeparator() + "Expected: " + expected
					+ System.lineSeparator() + "Actual: " + blocker.getMessage()
					+ System.lineSeparator() + "TestData: " + blocker.testData();
			ExecutionLogger.fail("MODULE BLOCKER | " + details.replace(System.lineSeparator(), " | "));
			logStepToExcel(HooksHolder.scenarioName, stepDescription, expected, blocker.getMessage(), false,
					blocker.testData(), ToastHandler.waitAndRead(getDriver(), 2000));
			ScreenshotManager.captureAndAttach(getDriver(), scenario,
					"MODULE BLOCKER | " + blocker.module() + " | " + blocker.getMessage(), false);

			BlockerChoice choice = PopupHandler.promptModuleBlocker(getDriver(),
					blocker.module() + " positive creation blocker", details);
			if (choice == BlockerChoice.CONTINUE_WAIT || choice == BlockerChoice.RETRY) {
				try {
					PopupHandler.runWithBlockerRetry(getDriver(), action);
					markStepPassed(stepDescription, expected, captureOnSuccess, scenario);
					ExecutionLogger.pass(stepDescription + " | recovered after module-blocker retry");
					return;
				} catch (ModuleBlockerException retryBlocker) {
					String reason = retryBlocker.module() + " still blocked after retry | "
							+ retryBlocker.getMessage() + " | TestData: " + retryBlocker.testData();
					SoftAssertManager.markSkipFeature(HooksHolder.featureName, reason);
					throw new SkipException("Stop module: " + reason);
				}
			}
			String reason = blocker.module() + " positive creation blocked | "
					+ blocker.getMessage() + " | TestData: " + blocker.testData();
			SoftAssertManager.markSkipFeature(HooksHolder.featureName, reason);
			throw new SkipException("Stop module: " + reason);
		} catch (Throwable t) {
			if (t instanceof AbortRunException) {
				ExecutionLogger.exception("Abort on step: " + stepDescription, t);
				handleAbortAndStop((AbortRunException) t);
			}
			if (t.getCause() instanceof AbortRunException) {
				ExecutionLogger.exception("Abort on step: " + stepDescription, t.getCause());
				handleAbortAndStop((AbortRunException) t.getCause());
			}
			Throwable working = t;
			boolean unexpectedPage = PopupHandler.isUnexpectedApplicationPage(getDriver());
			if (unexpectedPage) {
				ExecutionLogger.fail("Unexpected/server page detected | "
						+ PopupHandler.describeUnexpectedPage(getDriver())
						+ " | interrupted step=" + stepDescription);
			} else {
				try {
					ExecutionLogger.warn("Step failed — safe refresh/retry check | " + stepDescription
							+ " | " + (t.getMessage() == null ? t.toString() : t.getMessage()));
					if (MasterUiHelper.refreshAndPrepareNav(getDriver(), 8)) {
						PopupHandler.runWithBlockerRetry(getDriver(), action);
						markStepPassed(stepDescription, expected, captureOnSuccess, scenario);
						ExecutionLogger.pass(stepDescription + " | recovered after refresh retry");
						return;
					}
				} catch (Throwable retryEx) {
					working = retryEx;
					ExecutionLogger.warn("Refresh retry still failed | " + stepDescription);
				}
			}
			/*
			 * A 500/foreign endpoint is always a blocker. Missing normal UI prompts only
			 * when the step is explicitly mandatory.
			 */
			if ((unexpectedPage || mandatory) && ConfigReader.interactiveBlockerPrompt()
					&& (unexpectedPage || PopupHandler.looksLikeMissingExpectedUi(working))) {
				try {
					String recovery = unexpectedPage
							? "Unexpected/server page detected: "
									+ PopupHandler.describeUnexpectedPage(getDriver()) + System.lineSeparator()
									+ "Please go to Login, sign in, return to the expected module page, then click Continue."
									+ System.lineSeparator()
							: "";
					String details = recovery + "Expected: " + expected + System.lineSeparator()
							+ (working.getMessage() != null ? working.getMessage() : working.toString());
					BlockerChoice choice = PopupHandler.promptMissingExpectedUi(
							getDriver(),
							unexpectedPage ? "Server error / unexpected page — " + stepDescription : stepDescription,
							details);
					PopupHandler.applyMissingUiChoice(choice, HooksHolder.featureName, stepDescription);
					if (choice == BlockerChoice.CONTINUE_WAIT) {
						PopupHandler.runWithBlockerRetry(getDriver(), action);
						markStepPassed(stepDescription, expected, captureOnSuccess, scenario);
						ExecutionLogger.pass(stepDescription + " | recovered after Continue");
						return;
					}
					if (choice == BlockerChoice.SKIP_SCENARIO || choice == BlockerChoice.SKIP_FEATURE) {
						ExecutionLogger.warn(choice + ": " + stepDescription);
						throw new SkipException(choice + ": " + stepDescription);
					}
					if (choice == BlockerChoice.STOP) {
						handleAbortAndStop(new AbortRunException("Stop chosen for: " + stepDescription));
					}
				} catch (AbortRunException abort) {
					handleAbortAndStop(abort);
				} catch (SkipException skip) {
					throw skip;
				} catch (Throwable retryFail) {
					working = retryFail;
				}
			}
			String errorMsg = working.getMessage() != null ? working.getMessage() : working.toString();
			ExecutionLogger.fail(stepDescription + " | Expected: " + expected + " | Actual: " + errorMsg);
			ExecutionLogger.exception(stepDescription, working);
			logStepToExcel(stepDescription, expected, errorMsg, false);
			SoftAssertManager.recordFailure(stepDescription, working);
			ScreenshotManager.captureAndAttach(getDriver(), scenario, stepDescription + " | " + errorMsg, false);
		}
	}

	private static void markStepPassed(String stepDescription, String expected, boolean captureOnSuccess, Scenario scenario) {
		String screenshotFile = "";
		if (captureOnSuccess) {
			String tcId = reporter.peekNextTcId(HooksHolder.featureName);
			screenshotFile = ScreenshotManager.captureMilestoneForTc(getDriver(), scenario, tcId,
					stepDescription + " | Module entered successfully | " + expected);
		} else {
			ExtentReportManager.logInfo(stepDescription + " | Expected: " + expected + " | Actual: " + expected);
		}
		logStepToExcel(HooksHolder.scenarioName, stepDescription, expected, expected, true, "", "",
				screenshotFile.isBlank() ? null : screenshotFile);
	}

	/** Stop run: flush reports so far, close browser, abort remaining scenarios. */
	private static void handleAbortAndStop(AbortRunException abort) {
		logger.error("Run STOP requested: {}", abort.getMessage());
		SoftAssertManager.markAbortRun(abort.getMessage());
		try {
			shutdown();
		} catch (Exception e) {
			logger.warn("Report flush during abort failed: {}", e.getMessage());
		}
		try {
			quitBrowser();
		} catch (Exception e) {
			logger.warn("Browser quit during abort failed: {}", e.getMessage());
		}
		throw new SkipException("STOP — browser closed, reports generated: " + abort.getMessage());
	}



	/**
	 * Optional reset to login — only when {@code resetToLoginAfterScenario=true} in config.
	 * Default is false so the browser stays on the dashboard after a successful login.
	 */
	private void recoverBrowserForNextScenario(Scenario scenario) {
		if (!ConfigReader.resetToLoginAfterScenario()) {
			logger.debug("Skipping login URL reset — resetToLoginAfterScenario=false");
			return;
		}
		if (getDriver() == null) {
			return;
		}
		try {
			getDriver().get(ConfigReader.getUrl());
			logger.info("Browser reset to login URL for next scenario (previous scenario: {})", scenario.getName());
		} catch (Exception e) {
			logger.warn("Could not reset browser to login URL: {}", e.getMessage());
		}
	}



	public static void logStep(String stepDescription, String expected, String actual, boolean passed) {

		logStepToExcel(HooksHolder.scenarioName, stepDescription, expected, actual, passed);

		if (passed) {

			ExtentReportManager.logInfo(stepDescription + " | Expected: " + expected + " | Actual: " + actual);

		} else {

			ExtentReportManager.logFail(stepDescription + " | Expected: " + expected + " | Actual: " + actual);

		}

	}

	/**
	 * Excel Scenario column override (e.g. include sub-card name: In-Progress / Duplicate).
	 */
	public static void logStepWithScenario(String scenarioForExcel, String stepDescription, String expected,
			String actual, boolean passed) {
		String scenario = (scenarioForExcel == null || scenarioForExcel.isBlank())
				? HooksHolder.scenarioName
				: scenarioForExcel;
		logStepToExcel(scenario, stepDescription, expected, actual, passed, "", "");
		if (passed) {
			ExtentReportManager.logInfo(stepDescription + " | Expected: " + expected + " | Actual: " + actual);
		} else {
			ExtentReportManager.logFail(stepDescription + " | Expected: " + expected + " | Actual: " + actual);
		}
	}

	/**
	 * Masters/detailed Excel row: Test Data + Toast columns; on failure captures screenshot named by TC ID.
	 */
	public static void logStepWithDetails(String scenarioForExcel, String stepDescription, String expected,
			String actual, boolean passed, String testData, String toastMessage) {
		String scenario = (scenarioForExcel == null || scenarioForExcel.isBlank())
				? HooksHolder.scenarioName
				: scenarioForExcel;
		String toast = toastMessage == null ? "" : toastMessage;
		logStepToExcel(scenario, stepDescription, expected, actual, passed, testData == null ? "" : testData, toast);
		String line = stepDescription + " | Expected: " + expected + " | Actual: " + actual
				+ (testData == null || testData.isBlank() ? "" : " | TestData: " + testData)
				+ (toast.isBlank() ? "" : " | Toast: " + toast);
		if (passed) {
			ExtentReportManager.logInfo(line);
		} else {
			ExtentReportManager.logFail(line);
		}
	}

	/** Milestone screenshot for feature/module coverage in Extent, Allure, Cucumber, and disk. */
	public static void captureMilestone(String description) {
		captureMilestone(description, true);
	}

	public static void captureMilestone(String description, boolean success) {
		ScreenshotManager.captureAndAttach(getDriver(), currentScenario.get(), description, success);
	}

	private static void logStepToExcel(String stepDescription, String expected, String actual, boolean passed) {
		logStepToExcel(HooksHolder.scenarioName, stepDescription, expected, actual, passed, "", "");
	}

	private static void logStepToExcel(String scenarioName, String stepDescription, String expected, String actual,
			boolean passed) {
		logStepToExcel(scenarioName, stepDescription, expected, actual, passed, "", "");
	}

	private static void logStepToExcel(String scenarioName, String stepDescription, String expected, String actual,
			boolean passed, String testData, String toastMessage) {
		logStepToExcel(scenarioName, stepDescription, expected, actual, passed, testData, toastMessage, null);
	}

	/**
	 * @param screenshotOverride when non-null, used as-is for the Excel Screenshot column (a screenshot
	 *                           already captured by the caller, e.g. a pass-path milestone shot); when
	 *                           null, a failure screenshot is captured here as before.
	 */
	private static void logStepToExcel(String scenarioName, String stepDescription, String expected, String actual,
			boolean passed, String testData, String toastMessage, String screenshotOverride) {
		String status = passed ? "Passed" : "Failed";
		String error = passed ? "-" : "Step failed";
		String toast = toastMessage;
		if ((toast == null || toast.isBlank()) && !passed) {
			// Poll briefly — a MUI snackbar can auto-dismiss within 2-3s, so a single instant
			// read can easily miss it. waitAndRead catches it if it's still fading in/out.
			toast = Com.Ckyc_4_0.utils.ToastHandler.waitAndRead(getDriver(), 2000);
		}
		String screenshots = screenshotOverride;
		if (screenshots == null) {
			screenshots = "";
			if (!passed) {
				String tcId = reporter.peekNextTcId(HooksHolder.featureName);
				screenshots = ScreenshotManager.captureFailureForTc(getDriver(), currentScenario.get(), tcId,
						stepDescription);
			}
		}
		reporter.logStepDetailed(HooksHolder.featureName, scenarioName, stepDescription, expected, actual, status,
				error, testData, toast, screenshots);
	}



	private String getFeatureName(Scenario scenario) {

		try {

			String rawUri = scenario.getUri().toString();

			File file = new File(rawUri);

			return file.getName().replace(".feature", "");

		} catch (Exception e) {

			return "Unknown Feature";

		}

	}



	public static void shutdown() {

		String totalTimeTaken = Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper.elapsedFormatted();

		ExtentReportManager.setTotalDuration(totalTimeTaken);
		ExtentReportManager.flush();

		reporter.writeSummarySheet(totalTimeTaken);
		reporter.saveExcel();

		DashboardDataExcelReport.getInstance().save();

		ExecutionLogger.info("Reports flushed | Extent/Excel/DashboardData");
		ExecutionLogger.info("Total Time Taken: " + totalTimeTaken);
		ExecutionLogger.close();

		// Open reports BEFORE quitting the browser, and guard the quit — an already-dead/crashed
		// session throwing out of driver.quit() must never silently skip opening the reports.
		Com.Ckyc_4_0.UtilityFiles.ReportOpener.openReportsIfConfigured();

		try {
			quitBrowser();
		} catch (Exception e) {
			logger.warn("Browser quit failed (reports were already saved/opened): {}", e.getMessage());
		}

	}

}

