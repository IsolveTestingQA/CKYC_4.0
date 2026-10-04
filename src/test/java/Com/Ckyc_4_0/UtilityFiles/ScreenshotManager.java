/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.model.Media;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * Captures screenshots after page is fully loaded, with short delays so UI is visible in reports.
 */
public final class ScreenshotManager {

	private static final Logger logger = LoggerFactory.getLogger(ScreenshotManager.class);
	private ScreenshotManager() {
	}

	private static String screenshotDir() {
		return ReportNamingHelper.screenshotDir();
	}

	public static String captureAndAttach(WebDriver driver, Scenario scenario, String description, boolean success) {
		if (driver == null) {
			logger.warn("Screenshot skipped (no driver): {}", description);
			return null;
		}
		try {
			preparePageForScreenshot();

			byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
			String fileName = buildFileName(description, success);
			File dest = new File(screenshotDir(), fileName);
			FileUtils.forceMkdirParent(dest);
			FileUtils.writeByteArrayToFile(dest, screenshot);

			String statusPrefix = success ? "PASS" : "FAIL";
			String attachmentTitle = statusPrefix + " | " + description;

			attachToCucumber(scenario, screenshot, attachmentTitle);
			attachToAllure(screenshot, attachmentTitle);
			attachToExtent(attachmentTitle, screenshot, success);

			pauseAfterCapture();

			logger.info("Screenshot [{}]: {}", attachmentTitle, dest.getAbsolutePath());
			return dest.getAbsolutePath();
		} catch (IOException e) {
			logger.error("Screenshot capture failed for '{}': {}", description, e.getMessage(), e);
			Allure.addAttachment("Screenshot Error - " + description, e.getMessage());
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("Screenshot delay interrupted for '{}'", description);
			return null;
		} catch (RuntimeException e) {
			// Invalid / closed session must not fail the scenario hooks
			logger.warn("Screenshot skipped (driver unavailable) for '{}': {}", description, e.getMessage());
			return null;
		}
	}

	/** Always: brief DOM ready check, optional pause, then capture. */
	private static void preparePageForScreenshot() throws InterruptedException {
		BaseClass.waitForPageFullyLoaded();
		int beforeMs = ConfigReader.getScreenshotDelayBeforeMs();
		if (beforeMs > 0) {
			Thread.sleep(beforeMs);
		}
	}

	private static void pauseAfterCapture() throws InterruptedException {
		int afterMs = ConfigReader.getScreenshotDelayAfterMs();
		if (afterMs > 0) {
			Thread.sleep(afterMs);
		}
	}

	private static String buildFileName(String description, boolean success) {
		String time = new SimpleDateFormat("hh_mm_ss_a").format(new Date());
		String safe = description.replaceAll("[^a-zA-Z0-9-_]", "_");
		if (safe.length() > 80) {
			safe = safe.substring(0, 80);
		}
		return ReportNamingHelper.shortDateToken() + "_" + (success ? "PASS_" : "FAIL_") + safe + "_" + time + ".png";
	}

	/**
	 * Screenshot named by date + TC ID (e.g. 29Sep_ST_012_113205.png), for either a pass or a fail
	 * row. Retries once if capture fails. Returns the file name for the Excel Screenshot column
	 * (not the full path) — the file itself lives under the one shared per-date screenshot folder.
	 */
	public static String captureForTc(WebDriver driver, Scenario scenario, String tcId, String stepHint,
			boolean success) {
		if (driver == null || tcId == null || tcId.isBlank()) {
			return "";
		}
		String safeTc = tcId.replaceAll("[^a-zA-Z0-9-_]", "_");
		String hint = stepHint == null ? "" : stepHint.replaceAll("[^a-zA-Z0-9-_]", "_");
		if (hint.length() > 40) {
			hint = hint.substring(0, 40);
		}
		String time = new SimpleDateFormat("HHmmss").format(new Date());
		String base = ReportNamingHelper.shortDateToken() + "_" + safeTc + "_" + time;
		String fileName = base + (hint.isBlank() ? "" : "_" + hint) + ".png";
		IOException last = null;
		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				preparePageForScreenshot();
				byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
				File dest = new File(screenshotDir(), fileName);
				if (attempt > 1) {
					fileName = base + (hint.isBlank() ? "" : "_" + hint) + "_retry.png";
					dest = new File(screenshotDir(), fileName);
				}
				FileUtils.forceMkdirParent(dest);
				FileUtils.writeByteArrayToFile(dest, screenshot);
				String statusPrefix = success ? "PASS" : "FAIL";
				String title = statusPrefix + " | " + tcId + (stepHint == null || stepHint.isBlank() ? "" : " | " + stepHint);
				attachToCucumber(scenario, screenshot, title);
				attachToAllure(screenshot, title);
				attachToExtent(title, screenshot, success);
				pauseAfterCapture();
				logger.info("{} screenshot [{}]: {}", statusPrefix, title, dest.getAbsolutePath());
				return dest.getName();
			} catch (IOException e) {
				last = e;
				logger.warn("Screenshot attempt {} failed for {}: {}", attempt, tcId, e.getMessage());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("Screenshot interrupted for {}", tcId);
				return "";
			} catch (RuntimeException e) {
				logger.warn("Screenshot skipped (driver unavailable) for {}: {}", tcId, e.getMessage());
				return "";
			}
		}
		if (last != null) {
			Allure.addAttachment("Screenshot Error - " + tcId, last.getMessage());
		}
		return "";
	}

	/** Failure screenshot named by date + TC ID. Retries once if capture fails. */
	public static String captureFailureForTc(WebDriver driver, Scenario scenario, String tcId, String stepHint) {
		return captureForTc(driver, scenario, tcId, stepHint, false);
	}

	/** Pass/milestone screenshot named by date + TC ID, so it can also be recorded in the Excel Screenshot column. */
	public static String captureMilestoneForTc(WebDriver driver, Scenario scenario, String tcId, String stepHint) {
		return captureForTc(driver, scenario, tcId, stepHint, true);
	}

	private static void attachToCucumber(Scenario scenario, byte[] screenshot, String title) {
		if (scenario != null) {
			scenario.attach(screenshot, "image/png", title);
		}
	}

	private static void attachToAllure(byte[] screenshot, String title) {
		Allure.addAttachment(title, "image/png", new ByteArrayInputStream(screenshot), "png");
	}

	private static void attachToExtent(String title, byte[] screenshot, boolean success) {
		ExtentTest parent = ExtentReportManager.getTest();
		if (parent == null) {
			return;
		}
		ExtentTest stepNode = parent.createNode(title);
		try {
			String base64 = Base64.getEncoder().encodeToString(screenshot);
			Media media = MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build();
			if (success) {
				stepNode.pass("Screenshot captured (page fully loaded)", media);
			} else {
				stepNode.fail("Screenshot captured (page fully loaded)", media);
			}
		} catch (Exception e) {
			logger.warn("Extent base64 screenshot attach failed for '{}': {}", title, e.getMessage());
			if (success) {
				stepNode.pass("Screenshot saved under Report Output/Screenshots");
			} else {
				stepNode.fail("Screenshot saved under Report Output/Screenshots");
			}
		}
	}
}
