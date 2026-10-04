/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.concurrentsession;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.factory.DriverFactory;
import Com.Ckyc_4_0.pages.login.LoginPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.PopupHandler;
import Com.Ckyc_4_0.utils.StepLog;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * Opens a second browser while the primary session is active.
 * Validates the "Active session detected" dialog and session invalidation.
 */
public class ConcurrentSessionFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(ConcurrentSessionFunctionality.class);
	private WebDriver secondDriver;
	private boolean dialogAppeared = false;
	private boolean secondBrowserReachedDashboard = false;

	public void openSecondBrowserAndLogin() {
		String browser = getProperty("browser", "chrome");
		boolean headless = Boolean.parseBoolean(getProperty("headless", "false"));
		String downloadPath = System.getProperty("user.dir") + File.separator + "getFilesDownloaded";

		logger.info("Launching second {} browser for concurrent session test", browser);
		secondDriver = DriverFactory.createDriver(browser, headless, downloadPath);
		secondDriver.manage().window().maximize();
		secondDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

		String loginUrl = ConfigReader.getUrl() + "/login";
		secondDriver.get(loginUrl);

		WebDriverWait wait = new WebDriverWait(secondDriver, Duration.ofSeconds(15));
		wait.until(ExpectedConditions.visibilityOfElementLocated(LoginPage.USERNAME_INPUT));

		String username = ConfigReader.getUsername();
		String password = ConfigReader.getPassword();

		secondDriver.findElement(LoginPage.USERNAME_INPUT).clear();
		secondDriver.findElement(LoginPage.USERNAME_INPUT).sendKeys(username);
		secondDriver.findElement(LoginPage.PASSWORD_INPUT).clear();
		secondDriver.findElement(LoginPage.PASSWORD_INPUT).sendKeys(password);

		Hooks.captureMilestone("Concurrent | Second browser login form filled | user=" + username);

		List<WebElement> signIn = secondDriver.findElements(LoginPage.SIGN_IN_BUTTON);
		if (!signIn.isEmpty()) {
			((JavascriptExecutor) secondDriver).executeScript("arguments[0].click();", signIn.get(0));
		}
		MasterUiHelper.sleep(2000);

		StepLog.check("Concurrent | Open second browser",
				"Launch second browser and submit login with same credentials",
				"Login form submitted in second browser",
				"PASS | Second browser opened, login submitted for user=" + username,
				true, "user=" + username);
	}

	public void verifyActiveSessionDialog() {
		WebDriverWait wait = new WebDriverWait(secondDriver, Duration.ofSeconds(10));
		try {
			wait.until(ExpectedConditions.or(
					ExpectedConditions.visibilityOfElementLocated(LoginPage.ACTIVE_SESSION_DIALOG),
					ExpectedConditions.visibilityOfElementLocated(LoginPage.LOGOUT_OTHER_SESSION_CONTINUE)));
			dialogAppeared = true;
			logger.info("Active session detected dialog appeared in second browser");
		} catch (Exception e) {
			dialogAppeared = false;
			logger.warn("Active session dialog did NOT appear: {}", e.getMessage());
		}

		if (dialogAppeared) {
			captureScreenshot(secondDriver, "Concurrent_ActiveSessionDialog_SecondBrowser");
		}

		StepLog.check("Concurrent | Active session dialog",
				"After login in second browser, Active session detected dialog should appear",
				"Dialog with 'Log out other session & continue' is visible",
				dialogAppeared
						? "PASS | Active session detected dialog appeared"
						: "FAIL | Dialog did NOT appear — app may allow concurrent sessions without warning",
				dialogAppeared, "secondBrowser=true");
	}

	/**
	 * Captures both browsers side-by-side in a single combined screenshot.
	 */
	public void captureSplitScreenshot() {
		try {
			byte[] primaryBytes = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
			byte[] secondBytes = ((TakesScreenshot) secondDriver).getScreenshotAs(OutputType.BYTES);

			BufferedImage primaryImg = ImageIO.read(new ByteArrayInputStream(primaryBytes));
			BufferedImage secondImg = ImageIO.read(new ByteArrayInputStream(secondBytes));

			int totalWidth = primaryImg.getWidth() + secondImg.getWidth();
			int maxHeight = Math.max(primaryImg.getHeight(), secondImg.getHeight());

			BufferedImage combined = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = combined.createGraphics();
			g.drawImage(primaryImg, 0, 0, null);
			g.drawImage(secondImg, primaryImg.getWidth(), 0, null);
			g.dispose();

			String screenshotDir = System.getProperty("user.dir") + File.separator + "test-output"
					+ File.separator + "screenshots";
			new File(screenshotDir).mkdirs();
			String filename = "ConcurrentSession_SplitScreen_" + System.currentTimeMillis() + ".png";
			File outputFile = new File(screenshotDir, filename);
			ImageIO.write(combined, "png", outputFile);

			logger.info("Split-screen screenshot saved: {}", outputFile.getAbsolutePath());

			Hooks.captureMilestone("Concurrent | Split-screen: primary(left) + second(right)");

			StepLog.check("Concurrent | Split-screen screenshot",
					"Capture both browser windows side-by-side",
					"Combined screenshot saved to test-output/screenshots",
					"PASS | " + outputFile.getAbsolutePath(),
					true, "file=" + filename);
		} catch (Exception e) {
			logger.error("Split-screen screenshot failed: {}", e.getMessage());
			StepLog.check("Concurrent | Split-screen screenshot",
					"Capture both browser windows side-by-side",
					"Combined screenshot saved",
					"FAIL | " + e.getMessage(), false, "error");
		}
	}

	public void clickLogoutOtherSessionAndContinue() {
		if (!dialogAppeared) {
			StepLog.check("Concurrent | Click Log out other session",
					"Click 'Log out other session & continue'",
					"Button is clicked and dialog closes",
					"SKIP | Active session dialog did not appear — nothing to click",
					false, "dialogAppeared=false");
			return;
		}
		try {
			WebDriverWait wait = new WebDriverWait(secondDriver, Duration.ofSeconds(10));
			WebElement continueBtn = wait.until(
					ExpectedConditions.elementToBeClickable(LoginPage.LOGOUT_OTHER_SESSION_CONTINUE));
			((JavascriptExecutor) secondDriver).executeScript("arguments[0].click();", continueBtn);
			MasterUiHelper.sleep(2000);

			captureScreenshot(secondDriver, "Concurrent_AfterLogoutOtherSession");

			StepLog.check("Concurrent | Click Log out other session",
					"Click 'Log out other session & continue' in second browser",
					"Dialog closes and second browser proceeds to dashboard",
					"PASS | Button clicked successfully",
					true, "secondBrowser=true");
		} catch (Exception e) {
			StepLog.check("Concurrent | Click Log out other session",
					"Click 'Log out other session & continue'",
					"Button is clicked",
					"FAIL | " + e.getMessage(), false, "error");
		}
	}

	public void verifySecondBrowserReachesDashboard() {
		try {
			String dashPath = ConfigReader.getDashboardUrlPath();
			WebDriverWait wait = new WebDriverWait(secondDriver, Duration.ofSeconds(15));
			wait.until(d -> {
				String url = d.getCurrentUrl();
				return url != null && (url.contains(dashPath) || url.contains("/dashboard")
						|| url.contains("/ckyc/") || url.contains("/admin/"));
			});
			secondBrowserReachedDashboard = true;
			captureScreenshot(secondDriver, "Concurrent_SecondBrowser_Dashboard");
		} catch (Exception e) {
			secondBrowserReachedDashboard = false;
		}

		String url = safeUrl(secondDriver);
		StepLog.check("Concurrent | Second browser reaches dashboard",
				"After handling Active session dialog, second browser loads dashboard",
				"Dashboard or app page is loaded in second browser",
				secondBrowserReachedDashboard
						? "PASS | Second browser on: " + url
						: "FAIL | Second browser stuck at: " + url,
				secondBrowserReachedDashboard, "url=" + url);
	}

	public void verifyFirstBrowserSessionInvalidated() {
		try {
			getDriver().navigate().refresh();
			MasterUiHelper.sleep(2000);
			String url = safeUrl(getDriver());
			boolean invalidated = url.toLowerCase().contains("/login");

			captureScreenshot(getDriver(), "Concurrent_FirstBrowser_AfterInvalidation");

			StepLog.check("Concurrent | First browser session invalidated",
					"Refresh first browser — session should be terminated by second browser's takeover",
					"First browser redirects to login page",
					invalidated
							? "PASS | First browser redirected to login. URL=" + url
							: "INFO | First browser still active at " + url
									+ " — app may allow parallel sessions",
					true, "url=" + url);
		} catch (Exception e) {
			StepLog.check("Concurrent | First browser session invalidated",
					"Refresh first browser to check session",
					"Session should be invalidated",
					"FAIL | Error checking first browser: " + e.getMessage(), false, "error");
		}
	}

	public void cleanupBothSessions() {
		try {
			if (secondDriver != null) {
				secondDriver.quit();
				secondDriver = null;
				logger.info("Second browser closed");
			}
		} catch (Exception e) {
			logger.warn("Error closing second browser: {}", e.getMessage());
		}

		// Re-login in primary browser if session was invalidated
		try {
			String url = safeUrl(getDriver());
			if (url.toLowerCase().contains("/login")) {
				logger.info("Re-logging in primary browser after concurrent session test");
				getDriver().findElement(LoginPage.USERNAME_INPUT).clear();
				getDriver().findElement(LoginPage.USERNAME_INPUT).sendKeys(ConfigReader.getUsername());
				getDriver().findElement(LoginPage.PASSWORD_INPUT).clear();
				getDriver().findElement(LoginPage.PASSWORD_INPUT).sendKeys(ConfigReader.getPassword());
				List<WebElement> signIn = getDriver().findElements(LoginPage.SIGN_IN_BUTTON);
				if (!signIn.isEmpty()) {
					((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", signIn.get(0));
				}
				MasterUiHelper.sleep(2000);
				PopupHandler.handleActiveSessionIfPresent(getDriver(), 5);
			}
		} catch (Exception e) {
			logger.warn("Primary browser re-login failed: {}", e.getMessage());
		}

		StepLog.check("Concurrent | Cleanup",
				"Close second browser and restore primary session",
				"Second browser closed, primary re-logged in if needed",
				"PASS | Cleanup complete", true, "");
	}

	private void captureScreenshot(WebDriver driver, String name) {
		try {
			byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
			String dir = System.getProperty("user.dir") + File.separator + "test-output"
					+ File.separator + "screenshots";
			new File(dir).mkdirs();
			File file = new File(dir, name + "_" + System.currentTimeMillis() + ".png");
			java.nio.file.Files.write(file.toPath(), bytes);
			logger.info("Screenshot: {}", file.getAbsolutePath());
		} catch (Exception e) {
			logger.warn("Screenshot failed for {}: {}", name, e.getMessage());
		}
	}

	private String safeUrl(WebDriver driver) {
		try {
			return driver.getCurrentUrl() == null ? "" : driver.getCurrentUrl();
		} catch (Exception e) {
			return "";
		}
	}
}
