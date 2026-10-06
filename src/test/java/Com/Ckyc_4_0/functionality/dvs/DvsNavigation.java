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

	/**
	 * Opens the DVS URL and waits for the DVS shell. Sign-in is manual: the automation never types credentials,
	 * it only waits up to dvs.loginWaitSeconds for the person to sign in.
	 */
	public static void ensureSession() {
		if (shellVisible()) {
			return;
		}
		String url = DvsConfig.get("dvs.url", "");
		if (url.isBlank()) {
			throw new IllegalStateException("dvs.url is not configured (config.properties or CONFIG sheet)");
		}
		driver().get(url);
		int wait = DvsConfig.getInt("dvs.loginWaitSeconds", 240);
		logger.info("Waiting up to {}s for manual sign-in and the DVS shell at {}", wait, url);
		DvsStepLog.step("wait-manual-sign-in").actual("waiting up to " + wait + "s at " + url).log();
		long end = System.currentTimeMillis() + wait * 1000L;
		while (System.currentTimeMillis() < end) {
			if (shellVisible()) {
				DvsStepLog.step("wait-manual-sign-in").actual("DVS shell visible").result(DvsStepLog.PASS).log();
				return;
			}
			if (!driver().getCurrentUrl().toLowerCase().contains("dvs")) {
				driver().get(url);
			}
			DvsFieldActions.sleep(2000);
		}
		throw new IllegalStateException("DVS shell not visible after " + wait + "s - sign in manually and re-run");
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
		wait(15).until(ExpectedConditions.elementToBeClickable(row)).click();
		DvsFieldActions.sleep(1200);
		logger.info("Opened DVS record {} as {}", customerId, role);
		DvsStepLog.step("open-customer").field(customerId).actual("opened as " + role).result(DvsStepLog.PASS).log();
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

	public static void save() {
		DvsFieldActions.clickBy("COM_SHELL_Save");
		DvsFieldActions.sleep(DvsConfig.getInt("dvs.saveWaitMs", 1500));
		DvsStepLog.step("save").toast(DvsFieldActions.toastText()).result(DvsStepLog.PASS).log();
	}

	/** Clicks Submit to Checker and returns the toast shown (success or server error). */
	public static String submit() {
		DvsFieldActions.clickBy("COM_SHELL_Submit_to_Checker");
		String toast = DvsFieldActions.waitForToast(null, DvsConfig.getInt("dvs.submitToastSeconds", 6));
		DvsStepLog.step("submit").toast(toast).dialog(DvsFieldActions.dialogText()).result(DvsStepLog.INFO).log();
		return toast;
	}

	public static void reloadAndReopen(String moduleCode, String customerId) {
		driver().navigate().refresh();
		DvsFieldActions.sleep(1500);
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
