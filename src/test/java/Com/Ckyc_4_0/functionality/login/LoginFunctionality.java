/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.login;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.pages.login.LoginPage;
import Com.Ckyc_4_0.utils.PopupHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Login actions. Dashboard URL is verified only after Sign In (and optional session dialog) completes.
 */
public class LoginFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(LoginFunctionality.class);
	private final int waitSeconds = ConfigReader.getExplicitWait();

	public void verifyLoginPageIsDisplayed() {
		waitUntilVisible(LoginPage.USERNAME_INPUT, waitSeconds);
		logger.info("Login page ready (username field visible)");
	}

	public void selectRuntimeMode(String mode) {
		String normalized = mode == null ? "" : mode.trim().toUpperCase();
		switch (normalized) {
		case "API" -> {
			if (!isRuntimeModeSelected(LoginPage.RUNTIME_API_BUTTON)) {
				clickElement(LoginPage.RUNTIME_API_BUTTON, waitSeconds);
			}
		}
		case "MOCK" -> {
			if (!isRuntimeModeSelected(LoginPage.RUNTIME_MOCK_BUTTON)) {
				clickElement(LoginPage.RUNTIME_MOCK_BUTTON, waitSeconds);
			}
		}
		default -> throw new IllegalArgumentException("Unsupported runtime mode: " + mode + ". Use API or MOCK.");
		}
		logger.info("Runtime mode: {}", normalized);
	}

	public void enterCredentialsFromConfig() {
		typeIntoField(LoginPage.USERNAME_INPUT, ConfigReader.getUsername(), waitSeconds);
		typeIntoField(LoginPage.PASSWORD_INPUT, ConfigReader.getPassword(), waitSeconds);
		assertCredentialsNotDuplicated();
		logger.info("Credentials entered for user: {}", ConfigReader.getUsername());
	}

	public void clickSignIn() {
		clickElement(LoginPage.SIGN_IN_BUTTON, waitSeconds);
		logger.info("Sign In clicked");
	}

	/**
	 * After Sign In step:
	 * 1) Handle Active session dialog if shown (no Escape on login)
	 * 2) If still on login → clear, re-enter credentials, Sign In again once
	 * 3) Only then assert URL contains dashboard
	 * 4) Dismiss Google password bubble only on dashboard
	 */
	public void verifyUserLoggedInSuccessfully() {
		String dashboardPath = ConfigReader.getDashboardUrlPath();
		int loginWait = ConfigReader.getLoginWait();

		boolean sessionHandled = waitForSessionDialogOrDashboard(dashboardPath, loginWait);
		logOverlayResult(sessionHandled);

		// After session logout continue (or failed first Sign In), still on /login → Sign In again
		if (!isOnDashboard(dashboardPath) && isLoginFormVisible()) {
			ExtentReportManager.logInfo(
					"Still on login after Sign In / session continue — clearing fields, re-entering, Sign In again");
			logger.info("Re-login from login page (hard clear + credentials + Sign In)");
			enterCredentialsFromConfig();
			clickSignIn();
			sessionHandled = waitForSessionDialogOrDashboard(dashboardPath, loginWait) || sessionHandled;
			if (sessionHandled) {
				ExtentReportManager.logInfo("Active session handled during Sign In flow");
			}
		}

		// Dashboard URL check ONLY after Sign In flow completes
		waitUntilUrlContains(dashboardPath, loginWait);
		PopupHandler.dismissBrowserPasswordPromptsIfPresent(getDriver());

		String currentUrl = getDriver().getCurrentUrl();
		Assert.assertTrue(currentUrl != null && currentUrl.contains(dashboardPath),
				"Expected dashboard URL containing '" + dashboardPath + "' but was: " + currentUrl);

		// One more pass after dashboard confirmed — Chrome breach / Password Manager may appear late
		PopupHandler.dismissBrowserPasswordPromptsIfPresent(getDriver());
		logger.info("Login success — dashboard module URL: {}", currentUrl);
		ExtentReportManager.logInfo("Post-login: Chrome / Google Password Manager prompts closed if shown");
	}

	/**
	 * Poll for Active session dialog OR dashboard. Does not send Escape.
	 * If session continue leaves us on login, returns so caller can Sign In again.
	 */
	private boolean waitForSessionDialogOrDashboard(String dashboardPath, int timeoutSeconds) {
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
		boolean sessionHandled = false;
		long settleAfterSessionMs = 800;
		long sessionHandledAt = 0;

		while (System.currentTimeMillis() < deadline) {
			if (isOnDashboard(dashboardPath)) {
				return sessionHandled;
			}

			if (!sessionHandled && PopupHandler.isActiveSessionDialogVisible(getDriver())) {
				sessionHandled = PopupHandler.handleActiveSessionIfPresent(getDriver(), 5);
				sessionHandledAt = System.currentTimeMillis();
				ExtentReportManager.logInfo("Clicked 'Log out other session & continue'");
				continue;
			}

			// After session continue: give UI a moment; if still login, exit so we can Sign In again
			if (sessionHandled && isLoginFormVisible()) {
				if (System.currentTimeMillis() - sessionHandledAt >= settleAfterSessionMs) {
					logger.info("After session continue — still on login; will Sign In again");
					return true;
				}
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		if (isOnDashboard(dashboardPath)) {
			return sessionHandled;
		}
		if (sessionHandled && isLoginFormVisible()) {
			return true;
		}
		logger.warn("No dashboard yet after {}s. URL: {}", timeoutSeconds, safeUrl());
		return sessionHandled;
	}

	private boolean isOnDashboard(String dashboardPath) {
		String url = safeUrl();
		return url != null && url.contains(dashboardPath);
	}

	private boolean isLoginFormVisible() {
		try {
			if (PopupHandler.isActiveSessionDialogVisible(getDriver())) {
				return false;
			}
			WebElement user = getDriver().findElement(LoginPage.USERNAME_INPUT);
			WebElement signIn = getDriver().findElement(LoginPage.SIGN_IN_BUTTON);
			return user.isDisplayed() && signIn.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	private String safeUrl() {
		try {
			return getDriver().getCurrentUrl();
		} catch (Exception e) {
			return "";
		}
	}

	private void assertCredentialsNotDuplicated() {
		String expectedUser = ConfigReader.getUsername();
		String expectedPass = ConfigReader.getPassword();
		String actualUser = getDriver().findElement(LoginPage.USERNAME_INPUT).getAttribute("value");
		String actualPass = getDriver().findElement(LoginPage.PASSWORD_INPUT).getAttribute("value");
		Assert.assertEquals(actualUser, expectedUser,
				"Username field has unexpected/appended value. Actual='" + actualUser + "'");
		Assert.assertEquals(actualPass, expectedPass,
				"Password field has unexpected/appended value (length="
						+ (actualPass == null ? 0 : actualPass.length()) + ")");
	}

	private void logOverlayResult(boolean sessionHandled) {
		if (sessionHandled) {
			ExtentReportManager.logInfo(
					"Active session detected — clicked 'Log out other session & continue'");
		} else {
			ExtentReportManager.logInfo("Active session dialog not shown after Sign In");
		}
	}

	private boolean isRuntimeModeSelected(By modeButton) {
		try {
			WebElement button = getDriver().findElement(modeButton);
			return "true".equalsIgnoreCase(button.getAttribute("aria-pressed"));
		} catch (Exception e) {
			return false;
		}
	}
}
