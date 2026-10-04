/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.pages.account.AccountMenuPage;
import Com.Ckyc_4_0.pages.login.LoginPage;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * End-of-suite completion prompt (headed) and logout before browser quit.
 */
public final class SuiteCompletionHelper {

	private static final Logger logger = LoggerFactory.getLogger(SuiteCompletionHelper.class);

	public enum CompletionChoice {
		STAY, LOGOUT, TIMEOUT_LOGOUT, AUTO_HEADLESS, SKIPPED
	}

	private SuiteCompletionHelper() {
	}

	/**
	 * Headed/manual: show Stay / Logout for up to {@link ConfigReader#getSuiteCompletionWaitSeconds()}.
	 * Headless/CI: logout immediately without waiting.
	 */
	public static CompletionChoice resolveCompletionChoice(WebDriver driver, String runnerName) {
		if (driver == null) {
			return CompletionChoice.SKIPPED;
		}
		if (isHeadlessOrUnattended()) {
			ExecutionLogger.info("Suite completion | headless/unattended — auto-logout | runner=" + runnerName);
			performLogout(driver, "auto-headless");
			return CompletionChoice.AUTO_HEADLESS;
		}
		return showCompletionPrompt(driver, runnerName);
	}

	public static void performLogout(WebDriver driver, String reason) {
		if (driver == null) {
			return;
		}
		removePrompt(driver);
		try {
			if (isLoginPage(driver)) {
				ExecutionLogger.info("Suite logout skipped — already on login page | reason=" + reason);
				captureCompletionScreenshot(driver, "Suite logout skipped (login page) | " + reason);
				return;
			}
			openAccountMenu(driver);
			clickLogout(driver);
			waitForLoginPage(driver);
			captureCompletionScreenshot(driver, "Suite logout | " + reason);
			ExecutionLogger.pass("Suite logout completed | reason=" + reason);
			ExtentReportManager.logInfo("Suite logout completed | reason=" + reason);
		} catch (Exception e) {
			logger.warn("Suite logout failed (browser will still quit): {}", e.getMessage());
			ExecutionLogger.warn("Suite logout failed: " + e.getMessage());
			captureCompletionScreenshot(driver, "Suite logout failed | " + reason);
		}
	}

	private static boolean isLoginPage(WebDriver driver) {
		try {
			String url = driver.getCurrentUrl();
			if (url != null && url.contains("/login")) {
				return true;
			}
			return !driver.findElements(LoginPage.USERNAME_INPUT).isEmpty()
					&& !driver.findElements(LoginPage.SIGN_IN_BUTTON).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	private static CompletionChoice showCompletionPrompt(WebDriver driver, String runnerName) {
		int waitSec = ConfigReader.getSuiteCompletionWaitSeconds();
		String safeRunner = escapeJs(runnerName == null ? "Test runner" : runnerName);
		String script = ""
				+ "window.__ckycCompletionChoice = null;"
				+ "var old = document.getElementById('ckyc-suite-completion-prompt');"
				+ "if (old) { old.remove(); }"
				+ "var wrap = document.createElement('div');"
				+ "wrap.id = 'ckyc-suite-completion-prompt';"
				+ "wrap.setAttribute('role','alertdialog');"
				+ "wrap.style.cssText = 'position:fixed;inset:0;z-index:2147483647;display:flex;"
				+ "align-items:center;justify-content:center;background:rgba(15,23,42,0.55);"
				+ "font-family:Segoe UI,Arial,sans-serif;';"
				+ "wrap.innerHTML = `"
				+ "<div style=\"width:min(520px,92vw);background:#fff;border-radius:14px;"
				+ "box-shadow:0 20px 50px rgba(0,0,0,.28);overflow:hidden;border:1px solid #e2e8f0;\">"
				+ "<div style=\"background:#1d4ed8;color:#fff;padding:14px 18px;font-size:16px;font-weight:700;\">"
				+ "✓ Test execution completed</div>"
				+ "<div style=\"padding:18px;\">"
				+ "<div style=\"font-size:15px;font-weight:600;color:#0f172a;margin-bottom:8px;\">"
				+ safeRunner + " finished</div>"
				+ "<div style=\"font-size:13px;color:#475569;line-height:1.45;\">"
				+ "All scenarios have run and reports were saved. Choose whether to logout before the browser closes."
				+ "</div>"
				+ "<div style=\"font-size:12px;color:#64748b;margin-top:12px;\">"
				+ "Auto-logout in <b>" + waitSec + " seconds</b> if no choice is made.</div>"
				+ "<div style=\"display:flex;gap:10px;justify-content:flex-end;margin-top:18px;\">"
				+ "<button id=\"ckyc-completion-stay\" type=\"button\" style=\"padding:9px 16px;border-radius:8px;"
				+ "border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;font-weight:600;cursor:pointer;\">"
				+ "Stay on this page</button>"
				+ "<button id=\"ckyc-completion-logout\" type=\"button\" style=\"padding:9px 16px;border-radius:8px;"
				+ "border:none;background:#b91c1c;color:#fff;font-weight:600;cursor:pointer;\">Logout</button>"
				+ "</div></div></div>`;"
				+ "document.documentElement.appendChild(wrap);"
				+ "document.getElementById('ckyc-completion-stay').onclick=function(){window.__ckycCompletionChoice='stay';};"
				+ "document.getElementById('ckyc-completion-logout').onclick=function(){window.__ckycCompletionChoice='logout';};";

		try {
			((JavascriptExecutor) driver).executeScript(script);
			captureCompletionScreenshot(driver, "Suite completion prompt shown");
			logger.info("Suite completion prompt shown — waiting up to {}s", waitSec);
			ExecutionLogger.info("Suite completion prompt shown | waitSec=" + waitSec);
		} catch (Exception e) {
			logger.warn("Could not show completion prompt: {}", e.getMessage());
			performLogout(driver, "prompt-failed-auto-logout");
			return CompletionChoice.TIMEOUT_LOGOUT;
		}

		long deadline = System.currentTimeMillis() + (waitSec * 1000L);
		while (System.currentTimeMillis() < deadline) {
			try {
				Object choice = ((JavascriptExecutor) driver).executeScript("return window.__ckycCompletionChoice;");
				if ("stay".equals(choice)) {
					removePrompt(driver);
					ExecutionLogger.info("Suite completion | user chose Stay on this page");
					ExtentReportManager.logInfo("Suite completion choice: Stay on this page");
					captureCompletionScreenshot(driver, "Suite completion | Stay on this page");
					return CompletionChoice.STAY;
				}
				if ("logout".equals(choice)) {
					removePrompt(driver);
					ExecutionLogger.info("Suite completion | user chose Logout");
					performLogout(driver, "user-logout");
					return CompletionChoice.LOGOUT;
				}
			} catch (Exception ignored) {
				// page may be navigating
			}
			sleepQuietly(300);
		}
		removePrompt(driver);
		ExecutionLogger.info("Suite completion | timeout — auto-logout");
		ExtentReportManager.logInfo("Suite completion choice: timeout auto-logout");
		performLogout(driver, "timeout-auto-logout");
		return CompletionChoice.TIMEOUT_LOGOUT;
	}

	private static void openAccountMenu(WebDriver driver) {
		removePrompt(driver);
		List<org.openqa.selenium.WebElement> avatars = driver.findElements(AccountMenuPage.AVATAR_BUTTON);
		if (avatars.isEmpty()) {
			// Fallback: header avatar initial letter button near Admin label
			avatars = driver.findElements(org.openqa.selenium.By.xpath(
					"//header//button[contains(@class,'MuiIconButton')][last()]"
							+ " | //header//button[normalize-space()='A' or .//*[normalize-space()='A']]"));
		}
		if (avatars.isEmpty()) {
			throw new IllegalStateException("Account avatar button not found");
		}
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();",
				avatars.get(0));
		MasterUiHelper.sleep(600);
	}

	private static void clickLogout(WebDriver driver) {
		PomElementManager.findClickable(AccountMenuPage.LOGOUT_MENU_ITEM, ConfigReader.getExplicitWait());
		((JavascriptExecutor) driver).executeScript(
				"arguments[0].click();",
				driver.findElement(AccountMenuPage.LOGOUT_MENU_ITEM));
		MasterUiHelper.sleep(1200);
	}

	private static void waitForLoginPage(WebDriver driver) {
		long deadline = System.currentTimeMillis() + 15000L;
		while (System.currentTimeMillis() < deadline) {
			try {
				String url = driver.getCurrentUrl();
				if (url != null && url.contains("/login")) {
					return;
				}
				if (!driver.findElements(LoginPage.USERNAME_INPUT).isEmpty()
						&& !driver.findElements(LoginPage.SIGN_IN_BUTTON).isEmpty()) {
					return;
				}
			} catch (Exception ignored) {
			}
			MasterUiHelper.sleep(400);
		}
		logger.warn("Login page not confirmed after logout — continuing to quit browser");
	}

	private static void captureCompletionScreenshot(WebDriver driver, String description) {
		try {
			if (driver == null) {
				return;
			}
			byte[] png = ((org.openqa.selenium.TakesScreenshot) driver)
					.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
			java.io.File dest = new java.io.File(
					Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper.screenshotDir(),
					"suite-completion-" + System.currentTimeMillis() + ".png");
			org.apache.commons.io.FileUtils.forceMkdirParent(dest);
			org.apache.commons.io.FileUtils.writeByteArrayToFile(dest, png);
			ExtentReportManager.logInfo(description + " | screenshot=" + dest.getAbsolutePath());
			ExecutionLogger.info(description + " | screenshot=" + dest.getAbsolutePath());
		} catch (Exception e) {
			ExtentReportManager.logInfo(description);
			logger.debug("Completion screenshot skipped: {}", e.getMessage());
		}
	}

	private static void removePrompt(WebDriver driver) {
		try {
			((JavascriptExecutor) driver).executeScript(
					"var el=document.getElementById('ckyc-suite-completion-prompt'); if(el){el.remove();}");
		} catch (Exception ignored) {
		}
	}

	private static boolean isHeadlessOrUnattended() {
		return Boolean.parseBoolean(ConfigReader.get("headless", "false"))
				|| !ConfigReader.interactiveBlockerPrompt();
	}

	private static String escapeJs(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$");
	}

	private static void sleepQuietly(long ms) {
		try {
			Thread.sleep(Math.max(0L, ms));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
