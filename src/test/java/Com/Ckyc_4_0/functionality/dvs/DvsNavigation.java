/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;

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

/** DVS shell navigation: session check, role / module toggle, queue search, open record, save, reload. */
public final class DvsNavigation {

	private static final Logger logger = LoggerFactory.getLogger(DvsNavigation.class);
	private static final DvsLocators L = DvsLocators.get();

	private DvsNavigation() {
	}

	private static WebDriver driver() {
		return BaseClass.getDriver();
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
		long end = System.currentTimeMillis() + wait * 1000L;
		while (System.currentTimeMillis() < end) {
			if (shellVisible()) {
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
		clickIfPresent("COM_SHELL_Maker_role");
	}

	/** IND and MIN use the DVS Individual toggle; LE uses Legal Entity. */
	public static void selectModule(String moduleCode) {
		clickIfPresent("LE".equals(moduleCode) ? "COM_SHELL_Legal_Entity" : "COM_SHELL_DVS_Individual");
	}

	public static void openCustomer(String customerId) {
		if (customerId == null || customerId.isBlank()) {
			throw new IllegalStateException("No Customer_ID for this module (set cust.* in CONFIG)");
		}
		clickIfPresent("COM_SHELL_Queue");
		By search = L.by("COM_SHELL_dvsQueueSearch");
		WebElement box = wait(10).until(ExpectedConditions.elementToBeClickable(search));
		box.click();
		box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		box.sendKeys(customerId, Keys.ENTER);
		DvsFieldActions.sleep(800);
		String template = DvsConfig.get("dvs.queue.rowXpath", "(//*[normalize-space(text())='{ID}'])[1]");
		By row = By.xpath(template.replace("{ID}", customerId));
		wait(15).until(ExpectedConditions.elementToBeClickable(row)).click();
		DvsFieldActions.sleep(1200);
		logger.info("Opened DVS record {}", customerId);
	}

	public static void save() {
		clickRequired("COM_SHELL_Save");
		DvsFieldActions.sleep(DvsConfig.getInt("dvs.saveWaitMs", 1500));
	}

	public static void reloadAndReopen(String moduleCode, String customerId) {
		driver().navigate().refresh();
		DvsFieldActions.sleep(1500);
		ensureSession();
		selectMaker();
		selectModule(moduleCode);
		openCustomer(customerId);
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

	private static void clickRequired(String id) {
		wait(10).until(ExpectedConditions.elementToBeClickable(L.by(id))).click();
	}
}
