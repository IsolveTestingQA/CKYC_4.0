/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/** DVS shell navigation: session check, role / module toggle, queue search, open record, save, submit, reload. */
public final class DvsNavigation {

	private static final Logger logger = LoggerFactory.getLogger(DvsNavigation.class);
	private static final DvsLocators L = DvsLocators.get();

	private static String role = "Maker";

	private DvsNavigation() {
	}

	private static WebDriver driver() {
		return BaseClass.getDriver();
	}

	public static String currentRole() {
		return role;
	}

	/** DVS 2.0 endpoint: dvs.url when set, else the app link + /dvs-2. Logged for reference; used only as a last resort. */
	public static String endpoint() {
		String configured = DvsConfig.get("dvs.url", "");
		if (!configured.isBlank()) {
			return configured;
		}
		String base = DvsConfig.get("url", "").trim().replaceAll("/+$", "");
		return base + "/dvs-2";
	}

	/**
	 * Makes sure the DVS shell is open: the app is opened on its exact link, the user is signed in by the Login
	 * feature, and DVS 2.0 is opened with the SIDE NAVIGATION locators. The endpoint (/dvs-2) is written to the log
	 * but is only hit as a LAST RESORT when the locators fail (dvs.allowDirectUrl=false disables that fallback).
	 */
	public static void ensureSession() {
		if (shellVisible()) {
			return;
		}
		String current = safeUrl();
		if (current.toLowerCase().contains("/login") || current.isBlank()) {
			throw new IllegalStateException("Not signed in (url=" + current + "). The 1_Login feature must run before DVS.");
		}
		int wait = DvsConfig.getInt("dvs.shellWaitSeconds", 20);
		logger.info("DVS 2.0 endpoint (reference / last-resort fallback only): {}", endpoint());
		DvsStepLog.step("open-dvs").actual("endpoint " + endpoint() + " - opening through the side navigation locators first")
				.result(DvsStepLog.INFO).log();
		try {
			openFromSidebar(wait);
		} catch (RuntimeException e) {
			if (!DvsConfig.getBool("dvs.allowDirectUrl", true)) {
				DvsStepLog.step("open-dvs-sidebar").actual("sidebar navigation failed: " + e.getMessage()).result(DvsStepLog.FAIL).log();
				throw new IllegalStateException("DVS 2.0 could not be opened from the side navigation: " + e.getMessage(), e);
			}
			String url = endpoint();
			logger.warn("Side navigation failed ({}) - LAST RESORT: opening the endpoint {}", e.getMessage(), url);
			DvsStepLog.step("open-dvs-endpoint").actual("LAST RESORT endpoint " + url + " after locators failed: " + e.getMessage())
					.result(DvsStepLog.RETRY).log();
			driver().get(url);
		}
		if (!waitForShell(wait)) {
			throw new IllegalStateException("DVS shell not visible after " + wait + "s (url=" + safeUrl() + ")");
		}
		DvsStepLog.step("open-dvs").actual("DVS shell visible at " + safeUrl()).result(DvsStepLog.PASS).log();
	}

	/**
	 * Side navigation (verified live): "DVS" and "DVS 2.0" are two separate items; DVS 2.0 is
	 * div#tour-nav-dvs-2.0 and opens /dvs-2 in one click. Never click the old "DVS" item.
	 */
	private static void openFromSidebar(int waitSeconds) {
		Com.Ckyc_4_0.utils.MasterUiHelper.ensureSideNavOpen(driver(), 8);
		WebElement item;
		try {
			item = wait(waitSeconds).until(ExpectedConditions.presenceOfElementLocated(L.by("COM_NAV_DVS2")));
		} catch (RuntimeException e) {
			// fallback: item whose own text is exactly "DVS 2.0"
			List<WebElement> byText = driver().findElements(By.xpath(
					"//*[(@role='button' or self::a or self::button or self::li)][normalize-space(.)='DVS 2.0']"));
			if (byText.isEmpty()) {
				String labels = sidebarLabels();
				DvsStepLog.step("sidebar-labels").actual("DVS 2.0 not found. Labels seen: " + labels).result(DvsStepLog.FAIL).log();
				throw new IllegalStateException("No 'DVS 2.0' item in the side navigation. Labels seen: " + labels);
			}
			item = byText.get(0);
		}
		jsClick(item);
		DvsStepLog.step("sidebar-click").field("COM_NAV_DVS2").actual("clicked DVS 2.0 in side navigation").result(DvsStepLog.PASS).log();
		DvsFieldActions.sleep(800);
	}

	/** Visible texts of the side navigation, for the log when the DVS entry cannot be found. */
	private static String sidebarLabels() {
		try {
			Object o = ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
					"var out=[];document.querySelectorAll('nav a, nav button, nav [role=button], aside a, aside button,"
							+ " aside [role=button], [role=navigation] a, [role=navigation] [role=button], .MuiDrawer-root [role=button]')"
							+ ".forEach(function(e){var t=(e.innerText||e.getAttribute('aria-label')||'').trim();"
							+ "if(t&&out.indexOf(t)<0){out.push(t.replace(/\\s+/g,' '));}});return out.slice(0,40).join(' | ');");
			return o == null ? "(none)" : o.toString();
		} catch (RuntimeException e) {
			return "(could not read: " + e.getMessage() + ")";
		}
	}

	/** After loading the app link: wait until the app has left the blank / redirect state (dashboard or login). */
	private static void waitForAppPage(int seconds) {
		long end = System.currentTimeMillis() + seconds * 1000L;
		while (System.currentTimeMillis() < end) {
			String u = safeUrl().toLowerCase();
			if (u.contains("/dashboard") || u.contains("/login") || shellVisible()
					|| !driver().findElements(L.by("COM_NAV_DVS2")).isEmpty()) {
				return;
			}
			DvsFieldActions.sleep(500);
		}
	}

	private static boolean waitForShell(int seconds) {
		long end = System.currentTimeMillis() + seconds * 1000L;
		while (System.currentTimeMillis() < end) {
			if (shellVisible()) {
				return true;
			}
			DvsFieldActions.sleep(500);
		}
		return shellVisible();
	}

	private static void jsClick(WebElement el) {
		try {
			el.click();
		} catch (RuntimeException e) {
			((org.openqa.selenium.JavascriptExecutor) driver()).executeScript("arguments[0].click();", el);
		}
	}

	private static String safeUrl() {
		try {
			String u = driver().getCurrentUrl();
			return u == null ? "" : u;
		} catch (RuntimeException e) {
			return "";
		}
	}

	public static boolean shellVisible() {
		try {
			return !driver().findElements(L.by("COM_SHELL_DVS_Individual")).isEmpty();
		} catch (RuntimeException e) {
			return false;
		}
	}

	public static void selectMaker() {
		switchRole("Maker");
	}

	public static void selectChecker() {
		switchRole("Checker");
	}

	private static void switchRole(String wanted) {
		clickIfPresent("Checker".equals(wanted) ? "COM_SHELL_Checker_role" : "COM_SHELL_Maker_role");
		role = wanted;
		DvsStepLog.role(wanted);
		DvsStepLog.step("switch-role").actual(wanted).result(DvsStepLog.PASS).log();
	}

	/** IND and MIN use the DVS Individual toggle; LE uses Legal Entity. */
	public static void selectModule(String moduleCode) {
		clickIfPresent("LE".equals(moduleCode) ? "COM_SHELL_Legal_Entity" : "COM_SHELL_DVS_Individual");
		DvsStepLog.step("select-module").actual(moduleCode).result(DvsStepLog.PASS).log();
	}

	/** Opens the first of the given customer ids that is found in the queue of the current role; returns it. */
	public static String openFirstAvailable(List<String> ids) {
		RuntimeException last = null;
		for (String id : ids) {
			try {
				openCustomer(id);
				return id;
			} catch (RuntimeException e) {
				last = e;
				logger.warn("Customer {} not opened: {}", id, e.getMessage());
				DvsStepLog.step("open-customer").field(id).actual("not found / not opened: " + e.getMessage())
						.result(DvsStepLog.RETRY).log();
			}
		}
		throw new IllegalStateException("None of the configured customer ids could be opened " + ids
				+ (last == null ? "" : " - " + last.getMessage()));
	}

	public static void openCustomer(String customerId) {
		if (customerId == null || customerId.isBlank()) {
			throw new IllegalStateException("No customer id given (set dvs.cust.* in config.properties)");
		}
		searchQueue(customerId);
		By row = By.xpath(rowTemplate().replace("{ID}", customerId));
		// search result: the id shows as the first record; click it and the record (with its images) loads on the right
		wait(15).until(ExpectedConditions.presenceOfElementLocated(row));
		boolean loaded = false;
		for (int attempt = 1; attempt <= 3 && !loaded; attempt++) {
			List<WebElement> cards = driver().findElements(row);
			if (cards.isEmpty()) {
				DvsFieldActions.sleep(800);
				continue;
			}
			jsClick(cards.get(0));
			loaded = waitForRecordLoaded(10);
			if (!loaded) {
				DvsStepLog.step("open-customer").field(customerId).actual("clicked the record, right panel still empty (attempt "
						+ attempt + "/3)").result(DvsStepLog.RETRY).log();
			}
		}
		if (!loaded) {
			throw new IllegalStateException("Record " + customerId + " was clicked in the queue but the right panel did not load it");
		}
		logger.info("Opened DVS record {} as {}", customerId, role);
		DvsStepLog.step("open-customer").field(customerId).actual("opened as " + role).result(DvsStepLog.PASS).log();
	}

	/** The right panel has a record loaded when its tabs (Personal Details, Identity / POI ...) are on the page. */
	private static boolean waitForRecordLoaded(int seconds) {
		By recordTab = By.xpath("//button[@role='tab'][not(contains(normalize-space(),'Queue'))"
				+ " and not(starts-with(normalize-space(),'Image'))]");
		long end = System.currentTimeMillis() + seconds * 1000L;
		while (System.currentTimeMillis() < end) {
			try {
				if (!driver().findElements(recordTab).isEmpty()) {
					DvsFieldActions.sleep(500);
					return true;
				}
			} catch (RuntimeException ignored) {
				// page re-rendering - keep waiting
			}
			DvsFieldActions.sleep(400);
		}
		return false;
	}

	/** True when the customer row is listed in the current role's queue (does not open it). */
	public static boolean isInQueue(String customerId) {
		try {
			searchQueue(customerId);
			By row = By.xpath(rowTemplate().replace("{ID}", customerId));
			wait(6).until(ExpectedConditions.presenceOfElementLocated(row));
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static String rowTemplate() {
		return DvsConfig.get("dvs.queue.rowXpath", "(//*[normalize-space(text())='{ID}'])[1]");
	}

	private static void searchQueue(String customerId) {
		clickIfPresent("COM_SHELL_Queue");
		By search = L.by("Checker".equals(role) ? "COM_CHK_search" : "COM_SHELL_dvsQueueSearch");
		WebElement box = wait(10).until(ExpectedConditions.elementToBeClickable(search));
		box.click();
		box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		box.sendKeys(customerId, Keys.ENTER);
		DvsFieldActions.sleep(800);
	}

	/** Save is only a temporary save (the real validation answer comes from Submit). Returns the toast shown. */
	public static String save() {
		DvsFieldActions.clickBy("COM_SHELL_Save");
		String toast = DvsFieldActions.waitForToast(null, 3);
		DvsFieldActions.sleep(DvsConfig.getInt("dvs.saveWaitMs", 1500));
		DvsStepLog.step("save").toast(toast).result(DvsStepLog.PASS).log();
		return toast;
	}

	/** Clicks Submit to Checker and returns the toast shown (success or server error). */
	public static String submit() {
		DvsFieldActions.clickBy("COM_SHELL_Submit_to_Checker");
		String toast = DvsFieldActions.waitForToast(null, DvsConfig.getInt("dvs.submitToastSeconds", 6));
		DvsStepLog.step("submit").toast(toast).dialog(DvsFieldActions.dialogText()).result(DvsStepLog.INFO).log();
		return toast;
	}

	public static void reloadAndReopen(String moduleCode, String customerId) {
		// no browser refresh on /dvs-2 (a React route - the server can answer a reload with HTTP 500): load the exact app
		// link again, the session is kept, then reopen DVS 2.0 through the locators (endpoint only as last resort)
		String appLink = DvsConfig.get("url", "");
		if (!appLink.isBlank()) {
			logger.info("Reload: loading the app link {} then DVS 2.0 through the side navigation", appLink);
			driver().get(appLink);
			waitForAppPage(15);
		} else {
			driver().navigate().refresh();
		}
		DvsFieldActions.sleep(1000);
		ensureSession();
		switchRole(role);
		selectModule(moduleCode);
		openCustomer(customerId);
		DvsStepLog.step("reload").field(customerId).actual("reloaded and reopened").result(DvsStepLog.PASS).log();
	}

	private static WebDriverWait wait(int seconds) {
		return new WebDriverWait(driver(), Duration.ofSeconds(seconds));
	}

	private static void clickIfPresent(String id) {
		List<WebElement> els = driver().findElements(L.by(id));
		if (!els.isEmpty()) {
			try {
				els.get(0).click();
				DvsFieldActions.sleep(500);
			} catch (RuntimeException e) {
				logger.warn("Could not click {}: {}", id, e.getMessage());
			}
		}
	}
}
