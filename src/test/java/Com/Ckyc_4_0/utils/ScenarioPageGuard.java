/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.pages.masters.district.DistrictMasterPage;
import Com.Ckyc_4_0.pages.masters.pincode.PincodeMasterPage;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.masters.state.StateMasterPage;

/**
 * After each scenario, confirm we are still on (or can reach) the expected module
 * endpoint for that feature — Dashboard / State / District / Pincode Master.
 */
public final class ScenarioPageGuard {

	private static final Logger logger = LoggerFactory.getLogger(ScenarioPageGuard.class);
	private ScenarioPageGuard() {
	}

	public static void verifyExpectedEndpoint(WebDriver driver, String featureName, String scenarioName) {
		if (driver == null || SoftAssertManager.shouldAbortRun() || SoftAssertManager.shouldSkipScenario()) {
			return;
		}
		String feature = featureName == null ? "" : featureName;
		String scenario = scenarioName == null ? "" : scenarioName;
		String key = (feature + " " + scenario).toLowerCase();

		try {
			if (key.contains("state")) {
				assertOnPage(driver, "State Master", StateMasterPage.PAGE_HEADING, "/state");
			} else if (key.contains("district")) {
				assertOnPage(driver, "District Master", DistrictMasterPage.PAGE_HEADING, "/district");
			} else if (key.contains("pincode") || key.contains("pin code")) {
				assertOnPage(driver, "Pincode Master", PincodeMasterPage.PAGE_HEADING, "/pincode");
			} else if (key.contains("user management") || key.contains("users")) {
				if (feature.toLowerCase().contains("role") && !feature.toLowerCase().contains("users")) {
					assertOnUrl(driver, "User Management Roles", "/admin/roles");
				} else {
					assertOnUrl(driver, "User Management Users", "/admin/users");
				}
			} else if (key.contains("role")) {
				assertOnUrl(driver, "User Management Roles", "/admin/roles");
			} else if (key.contains("dashboard") || key.contains("login")) {
				assertOnDashboardOrApp(driver);
			} else {
				logger.debug("No endpoint guard mapped for feature={}", featureName);
			}
		} catch (Throwable t) {
			SoftAssertManager.recordFailure(
					"Post-scenario page check | expected module for " + featureName, t);
			ExtentReportManager.logInfo("Post-scenario page check failed | " + featureName
					+ " | " + (t.getMessage() != null ? t.getMessage() : t.toString()));
		}
	}

	private static void assertOnDashboardOrApp(WebDriver driver) {
		String url = safeUrl(driver);
		if (url.contains("/dashboard") || url.contains("/ckyc/")) {
			ExtentReportManager.logInfo("Post-scenario page check | still in app | URL=" + url);
			return;
		}
		if (PomElementManager.isPresent(SideNavPage.DASHBOARD)
				|| PomElementManager.isPresent(SideNavPage.MASTERS)) {
			ExtentReportManager.logInfo("Post-scenario page check | side-nav markers present");
			return;
		}
		throw new AssertionError("Expected Dashboard / CKYC app after scenario; URL=" + url);
	}

	private static void assertOnPage(WebDriver driver, String label, By heading, String urlHint) {
		String url = safeUrl(driver);
		if (PomElementManager.isPresent(heading)) {
			ExtentReportManager.logInfo("Post-scenario page check | on " + label + " | URL=" + url);
			return;
		}
		if (urlHint != null && !urlHint.isBlank() && url.toLowerCase().contains(urlHint.toLowerCase())) {
			ExtentReportManager.logInfo("Post-scenario page check | URL matches " + label + " | URL=" + url);
			return;
		}
		throw new AssertionError("Expected " + label + " page after scenario; heading missing; URL=" + url);
	}

	private static void assertOnUrl(WebDriver driver, String label, String urlHint) {
		String url = safeUrl(driver);
		if (url.toLowerCase().contains(urlHint.toLowerCase())) {
			ExtentReportManager.logInfo("Post-scenario page check | on " + label + " | URL=" + url);
			return;
		}
		throw new AssertionError("Expected " + label + " page after scenario; URL=" + url);
	}

	private static String safeUrl(WebDriver driver) {
		try {
			return driver.getCurrentUrl() == null ? "" : driver.getCurrentUrl();
		} catch (Exception e) {
			return "";
		}
	}
}
