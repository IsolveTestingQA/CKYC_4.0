/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.bulkimport;

import org.openqa.selenium.By;

/**
 * Locators of the Bulk Data Import -> Monitor Automation -> Customer Information flow. Captured by hand with the Locator
 * Recorder on 18-Oct-2026 (aria-label / visible text / placeholder, no positional paths). Side-navigation items live in
 * {@code pages.SideNavPage}; the Dashboard sub-cards are read by {@code functionality.reports.DashboardReportReader}.
 */
public final class BulkImportPages {

	private BulkImportPages() {
	}

	private static String q(String s) {
		return s.contains("'") ? "concat('" + s.replace("'", "',\"'\",'") + "')" : "'" + s + "'";
	}

	// ---- Bulk Data Import (/bulk/upload)
	public static final By NEW_RECORD = By.xpath("//button[normalize-space()='New Record']");
	public static final By FILE_INPUT = By.xpath("//input[@type='file']");
	public static final By CREATE_RECORD = By.xpath("//button[normalize-space()='Create Record']");
	public static final By HISTORY_HEADERS = By.xpath("//table//thead//th");
	public static final By DIALOG = By.xpath("//div[@role='dialog']");
	public static final By DIALOG_CLOSE = By.xpath("//div[@role='dialog']//button[contains(@aria-label,'Close') or normalize-space()='Close']"
			+ "|//div[@role='dialog']//*[@data-testid='CloseIcon']/ancestor::button[1]|//div[@role='dialog']//*[@data-testid='CloseIcon']");
	public static final By NEXT_PAGE = By.xpath("//button[@aria-label='Go to next page']");

	public static By historyRow(String fileName) {
		return By.xpath("//table//tbody/tr[.//*[normalize-space()=" + q(fileName) + "]]");
	}

	// ---- Monitor Automation (/settings/monitor-automation)
	public static final By MONITOR_SEARCH = By.xpath("//input[@placeholder='Search processes…' or contains(@placeholder,'Search processes')]");

	/** The card of one process: the nearest block around its name that holds a Start / Restart button. */
	public static By processCard(String name) {
		return By.xpath("(//*[normalize-space()=" + q(name) + " and not(*)])[1]/ancestor::div[.//button[normalize-space()='Restart' or normalize-space()='Start']][1]");
	}

	public static final String RESTART = "Restart";
	public static final String START = "Start";

	// ---- Customer Information (/settings/customer-information)
	public static final By CI_ID_INPUT = By.xpath("//input[contains(@placeholder,'e.g.') and contains(@placeholder,',')]"
			+ "|//label[contains(normalize-space(),'Customer IDs')]/following::input[1]");
	public static final By CI_SEARCH = By.xpath("//button[normalize-space()='Search']");
	public static final By CI_CLEAR = By.xpath("//button[normalize-space()='Clear']");
	public static final By CI_RESULT_COUNT = By.xpath("//h6[contains(normalize-space(),'Customers (')]");

	/** The tab buttons: the siblings of the PERSONAL DETAILS tab, so a new tab is found without a code change. */
	public static final By CI_TABS = By.xpath("//button[normalize-space()='PERSONAL DETAILS']/parent::*/button");

	public static By customerCard(String customerId) {
		return By.xpath("//*[not(*) and contains(normalize-space(),'ID " + customerId + "')]");
	}
}
