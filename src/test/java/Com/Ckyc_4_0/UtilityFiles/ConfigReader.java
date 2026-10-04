/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

/**
 * Single entry point for all runtime configuration. Values come only from config.properties
 * loaded by {@link BaseClass#loadConfig()}.
 */
public final class ConfigReader {

	private ConfigReader() {
	}

	public static String get(String key) {
		return BaseClass.getProperty(key, "");
	}

	public static String get(String key, String defaultValue) {
		return BaseClass.getProperty(key, defaultValue);
	}

	public static String getUrl() {
		return get("url");
	}

	public static String getUsername() {
		return get("username");
	}

	public static String getPassword() {
		return get("password");
	}

	public static String getRuntimeMode() {
		return get("runtimeMode", "API");
	}

	public static int getExplicitWait() {
		return Integer.parseInt(get("explicitWait", "10"));
	}

	public static int getLoginWait() {
		return Integer.parseInt(get("loginWait", "20"));
	}

	public static String getDashboardUrlPath() {
		return get("dashboardUrl", "/ckyc/dashboard");
	}

	public static String getExcelReportPath() {
		return get("excelReportPath", "");
	}

	public static String getExtentReportPath() {
		return get("extentReportPath", "");
	}

	public static String getCurrentDataPath() {
		return get("currentDataPath", "Current Data");
	}

	public static int getScreenshotDelayBeforeMs() {
		return Integer.parseInt(get("screenshotDelayBeforeMs", "200"));
	}

	public static int getScreenshotDelayAfterMs() {
		return Integer.parseInt(get("screenshotDelayAfterMs", "0"));
	}

	public static boolean screenshotOnEachFilter() {
		return Boolean.parseBoolean(get("screenshotOnEachFilter", "false"));
	}

	public static int getFilterRefreshWaitMs() {
		return Integer.parseInt(get("filterRefreshWaitMs", "200"));
	}

	public static boolean resetToLoginAfterScenario() {
		return Boolean.parseBoolean(get("resetToLoginAfterScenario", "false"));
	}

	/**
	 * When true (default), a blocker that stops the next step shows an on-page warning with Stop / Retry
	 * so you can decide. Set false for CI / unattended runs (auto-dismiss + one retry only).
	 */
	public static boolean interactiveBlockerPrompt() {
		return Boolean.parseBoolean(get("interactiveBlockerPrompt", "true"));
	}

	public static int getInteractiveBlockerWaitSeconds() {
		return Integer.parseInt(get("interactiveBlockerWaitSeconds",
				String.valueOf(Com.Ckyc_4_0.constants.FrameworkConstants.INTERACTIVE_BLOCKER_WAIT_SECONDS)));
	}

	/** Seconds to wait for a manual UI fix after "Continue" on the missing-element prompt (default 60). */
	public static int getManualFixWaitSeconds() {
		return Integer.parseInt(get("manualFixWaitSeconds",
				String.valueOf(Com.Ckyc_4_0.constants.FrameworkConstants.MANUAL_FIX_WAIT_SECONDS)));
	}

	public static boolean isHeadless() {
		return Boolean.parseBoolean(get("headless", "false"));
	}

	/** Seconds to wait on suite completion prompt before auto-logout (default 120). */
	public static int getSuiteCompletionWaitSeconds() {
		return Integer.parseInt(get("suiteCompletionWaitSeconds", "120"));
	}

	/** Whether {@code RetryAnalyzer} may retry a failed scenario (still gated by tag exclusions). */
	public static boolean retryFailedScenarios() {
		return Boolean.parseBoolean(get("retryFailedScenarios", "true"));
	}

	/** Extra attempts allowed for an eligible failed scenario (default 1 — i.e. 2 attempts total). */
	public static int maxScenarioRetries() {
		return Integer.parseInt(get("maxScenarioRetries", "1"));
	}

	public static boolean openExtentReportAfterRun() {
		return Boolean.parseBoolean(get("openExtentReportAfterRun", "true"));
	}

	public static boolean openAllureReportAfterRun() {
		return Boolean.parseBoolean(get("openAllureReportAfterRun", "true"));
	}

	// -------------------------- CKYC/CERSAI bulk-CSV validation (bank config) --------------------------
	public static String ckycOrgCode() {
		return get("ckycOrgCode", "IN4098");
	}

	public static String ckycOrgName() {
		return get("ckycOrgName", "KOTAK MAHINDRA PRIME LTD");
	}

	public static String ckycBrid() {
		return get("ckycBrid", "CHN02");
	}

	public static String ckycCustomerRefPattern(String persona) {
		return get("ckycCustomerRefPattern" + capitalize(persona), "^.+$");
	}

	private static String capitalize(String persona) {
		String p = persona == null ? "" : persona.toLowerCase(java.util.Locale.ROOT);
		return p.isEmpty() ? "" : Character.toUpperCase(p.charAt(0)) + p.substring(1);
	}
}
