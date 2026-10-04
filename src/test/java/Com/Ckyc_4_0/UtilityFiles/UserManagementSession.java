/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.account.AccountMenuPage;
import Com.Ckyc_4_0.pages.login.LoginPage;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.PopupHandler;
import Com.Ckyc_4_0.utils.StepLog;

/**
 * Reusable Maker ↔ Checker switch. One browser. Restores maker with config credentials.
 */
public final class UserManagementSession {
	private UserManagementSession() {}

	public static void restoreMaker() {
		loginAs(UserManagementAccounts.maker(), UserManagementAccounts.makerPassword());
	}

	public static void loginAs(String username, String password) {
		WebDriver driver = BaseClass.getDriver();
		if (driver == null) {
			throw new IllegalStateException("WebDriver is null");
		}
		String current = UserManagementStore.currentUser();
		if (!isLoginPage(driver)) {
			if (username != null && username.equalsIgnoreCase(current)) {
				return;
			}
			if (current == null && username != null
					&& username.equalsIgnoreCase(UserManagementAccounts.maker())) {
				UserManagementStore.setCurrentUser(username);
				return;
			}
		}
		if (!isLoginPage(driver)) {
			logoutQuietly(driver);
		}
		waitForLogin(driver);
		BaseClass.typeIntoField(LoginPage.USERNAME_INPUT, username, ConfigReader.getExplicitWait());
		BaseClass.typeIntoField(LoginPage.PASSWORD_INPUT, password, ConfigReader.getExplicitWait());
		MasterUiHelper.clickJs(LoginPage.SIGN_IN_BUTTON, ConfigReader.getExplicitWait());
		MasterUiHelper.sleep(800);
		PopupHandler.handleActiveSessionIfPresent(driver, ConfigReader.getLoginWait());
		if (isLoginPage(driver)) {
			BaseClass.typeIntoField(LoginPage.USERNAME_INPUT, username, ConfigReader.getExplicitWait());
			BaseClass.typeIntoField(LoginPage.PASSWORD_INPUT, password, ConfigReader.getExplicitWait());
			MasterUiHelper.clickJs(LoginPage.SIGN_IN_BUTTON, ConfigReader.getExplicitWait());
			PopupHandler.handleActiveSessionIfPresent(driver, ConfigReader.getLoginWait());
		}
		waitForApp(driver, username);
		PopupHandler.dismissBrowserPasswordPromptsIfPresent(driver);
		UserManagementStore.setCurrentUser(username);
	}

	public static boolean openUsersOrFail(String checkerLabel) {
		WebDriver driver = BaseClass.getDriver();
		// Try SideNav first
		navigateViaSideNav(driver, "Users");
		MasterUiHelper.sleep(900);
		// Fallback to direct URL if SideNav didn't reach /admin/users
		if (!safeUrl(driver).contains("/admin/users")) {
			driver.get(ConfigReader.getUrl() + "/admin/users");
			MasterUiHelper.sleep(900);
		}
		if (isLoginPage(driver)) {
			StepLog.check("Users | Checker access | " + checkerLabel, "Open /admin/users after login",
					"User Management Users is available",
					"FAIL | Session returned to login for " + checkerLabel, false, "user=" + checkerLabel);
			return false;
		}
		boolean onUsers = safeUrl(driver).contains("/admin/users");
		if (!onUsers) {
			StepLog.check("Users | Checker access | " + checkerLabel, "Open /admin/users after login",
					"User Management Users is available",
					"FAIL | " + checkerLabel + " has no User Management Users. URL=" + safeUrl(driver),
					false, "user=" + checkerLabel);
			return false;
		}
		return true;
	}

	public static boolean openRolesOrFail(String checkerLabel) {
		WebDriver driver = BaseClass.getDriver();
		navigateViaSideNav(driver, "Roles");
		MasterUiHelper.sleep(900);
		if (!safeUrl(driver).contains("/admin/roles")) {
			driver.get(ConfigReader.getUrl() + "/admin/roles");
			MasterUiHelper.sleep(900);
		}
		if (isLoginPage(driver) || !safeUrl(driver).contains("/admin/roles")) {
			StepLog.check("Roles | Checker access | " + checkerLabel, "Open /admin/roles after login",
					"User Management Roles is available",
					"FAIL | " + checkerLabel + " cannot open Roles. URL=" + safeUrl(driver),
					false, "user=" + checkerLabel);
			return false;
		}
		return true;
	}

	/**
	 * MCP-verified: Checker toggle sometimes needs a second click.
	 * Clicks Checker mode button and verifies aria-pressed="true", retries up to 3 times.
	 */
	public static void switchToCheckerMode(By checkerButton) {
		WebDriver driver = BaseClass.getDriver();
		for (int attempt = 0; attempt < 3; attempt++) {
			try {
				List<WebElement> btns = driver.findElements(checkerButton);
				if (!btns.isEmpty()) {
					String pressed = btns.get(0).getAttribute("aria-pressed");
					if ("true".equals(pressed)) return;
					((JavascriptExecutor) driver).executeScript("arguments[0].click();", btns.get(0));
					MasterUiHelper.sleep(1200);
					pressed = btns.get(0).getAttribute("aria-pressed");
					if ("true".equals(pressed)) return;
				}
			} catch (Exception ignored) {}
			MasterUiHelper.sleep(500);
		}
	}

	/**
	 * MCP-verified: Maker toggle — same retry pattern.
	 */
	public static void switchToMakerMode(By makerButton) {
		WebDriver driver = BaseClass.getDriver();
		for (int attempt = 0; attempt < 3; attempt++) {
			try {
				List<WebElement> btns = driver.findElements(makerButton);
				if (!btns.isEmpty()) {
					String pressed = btns.get(0).getAttribute("aria-pressed");
					if ("true".equals(pressed)) return;
					((JavascriptExecutor) driver).executeScript("arguments[0].click();", btns.get(0));
					MasterUiHelper.sleep(1200);
					pressed = btns.get(0).getAttribute("aria-pressed");
					if ("true".equals(pressed)) return;
				}
			} catch (Exception ignored) {}
			MasterUiHelper.sleep(500);
		}
	}

	/** Navigate to Users or Roles via SideNav click. */
	private static void navigateViaSideNav(WebDriver driver, String subModule) {
		try {
			// Open side nav if collapsed
			List<WebElement> menuOpen = driver.findElements(SideNavPage.MENU_OPEN);
			if (!menuOpen.isEmpty() && menuOpen.get(0).isDisplayed()) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuOpen.get(0));
				MasterUiHelper.sleep(500);
			}
			// Click User Management parent
			List<WebElement> umParent = driver.findElements(SideNavPage.USER_MANAGEMENT);
			if (!umParent.isEmpty()) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", umParent.get(0));
				MasterUiHelper.sleep(500);
			}
			// Click sub-module (Users or Roles)
			org.openqa.selenium.By subLoc = "Roles".equalsIgnoreCase(subModule)
					? SideNavPage.ROLES : SideNavPage.USERS;
			List<WebElement> sub = driver.findElements(subLoc);
			if (!sub.isEmpty()) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", sub.get(0));
				MasterUiHelper.sleep(800);
			}
		} catch (Exception e) {
			// SideNav may not be available — caller falls back to direct URL
		}
	}

	/**
	 * MCP-verified logout flow:
	 * 1. Click avatar button (MuiIconButton with MuiAvatar child — works for ANY user)
	 * 2. Wait for dropdown menu to appear
	 * 3. Click "Logout" menuitem
	 * 4. Wait for login page
	 * NEVER uses "Expand quick options" — that button is NOT needed.
	 */
	private static void logoutQuietly(WebDriver driver) {
		try {
			List<WebElement> avatars = driver.findElements(AccountMenuPage.AVATAR_BUTTON);
			if (avatars.isEmpty()) {
				avatars = driver.findElements(By.xpath(
						"//button[contains(@class,'MuiIconButton')][.//*[contains(@class,'MuiAvatar')]]"));
			}
			if (avatars.isEmpty()) {
				avatars = driver.findElements(By.xpath(
						"//button[contains(@aria-label,'Expand quick options')]/following-sibling::button[1]"));
			}
			if (!avatars.isEmpty()) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", avatars.get(0));
				MasterUiHelper.sleep(800);
			}
			List<WebElement> logout = driver.findElements(AccountMenuPage.LOGOUT_MENU_ITEM);
			if (logout.isEmpty()) {
				MasterUiHelper.sleep(500);
				logout = driver.findElements(By.xpath(
						"//*[@role='menuitem'][contains(normalize-space(.),'Logout')]"));
			}
			if (!logout.isEmpty()) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", logout.get(logout.size() - 1));
			}
			waitForLogin(driver);
			if (!isLoginPage(driver)) {
				driver.get(ConfigReader.getUrl() + "/login");
				waitForLogin(driver);
			}
		} catch (Exception e) {
			try {
				driver.get(ConfigReader.getUrl() + "/login");
				waitForLogin(driver);
			} catch (Exception e2) {
				driver.get(ConfigReader.getUrl());
				MasterUiHelper.sleep(1000);
			}
		}
	}

	private static void waitForLogin(WebDriver driver) {
		long deadline = System.currentTimeMillis() + 15000L;
		while (System.currentTimeMillis() < deadline) {
			if (isLoginPage(driver)) {
				return;
			}
			MasterUiHelper.sleep(300);
		}
	}

	private static void waitForApp(WebDriver driver, String username) {
		String dashboard = ConfigReader.getDashboardUrlPath();
		long deadline = System.currentTimeMillis() + (ConfigReader.getLoginWait() * 1000L);
		while (System.currentTimeMillis() < deadline) {
			if (!isLoginPage(driver)) {
				String url = safeUrl(driver);
				if (url.contains(dashboard) || url.contains("/ckyc/") || url.contains("/admin/")
						|| url.contains("/dashboard")) {
					return;
				}
			}
			PopupHandler.handleActiveSessionIfPresent(driver, 2);
			MasterUiHelper.sleep(300);
		}
		if (isLoginPage(driver)) {
			throw new IllegalStateException("Login failed for " + username + " — still on login page");
		}
	}

	private static boolean isLoginPage(WebDriver driver) {
		try {
			String url = driver.getCurrentUrl();
			if (url != null && url.toLowerCase().contains("/login")) {
				return true;
			}
			return !driver.findElements(LoginPage.USERNAME_INPUT).isEmpty()
					&& !driver.findElements(LoginPage.SIGN_IN_BUTTON).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	private static String safeUrl(WebDriver driver) {
		try {
			return driver.getCurrentUrl() == null ? "" : driver.getCurrentUrl();
		} catch (Exception e) {
			return "";
		}
	}
}
