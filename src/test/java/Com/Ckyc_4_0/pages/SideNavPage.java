/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages;

import org.openqa.selenium.By;

/**
 * Left side navigation locators (Enterprise Console drawer).
 * Open the drawer first via {@link #MENU_OPEN} when collapsed.
 */
public final class SideNavPage {

	private SideNavPage() {
	}

	/** Open drawer when collapsed — live UI uses ChevronRightIcon (or MenuOpenIcon). */
	public static final By MENU_OPEN = By.xpath(
			"//button[.//*[@data-testid='ChevronRightIcon']]"
					+ " | //button[.//*[@data-testid='MenuOpenIcon']]"
					+ " | //button[contains(@class,'MuiIconButton') and"
					+ " (.//svg[@data-testid='ChevronRightIcon'] or .//svg[@data-testid='MenuOpenIcon'])]");

	/** Close drawer when open — live UI uses ChevronLeftIcon (collapses side nav). */
	public static final By MENU_CLOSE = By.xpath(
			"//button[.//*[@data-testid='ChevronLeftIcon']]"
					+ " | //button[.//*[@data-testid='MenuIcon']]"
					+ " | //button[contains(@class,'MuiIconButton') and"
					+ " (.//svg[@data-testid='ChevronLeftIcon'] or .//svg[@data-testid='MenuIcon'])]");

	public static final By NAV_LIST = By.cssSelector("ul.MuiList-root");

	public static final By DASHBOARD = navModule("dashboard", "Dashboard");
	public static final By MASTERS = navModule("masters", "Masters");
	public static final By MASTERS_GROUP = By.id("nav-group-masters");
	public static final By AUDIT = navModule("audit", "Audit");
	public static final By REPORTS = navModule("reports", "Reports");
	public static final By USER_MANAGEMENT = navModule("user-management", "User Management");
	public static final By USER_MANAGEMENT_GROUP = By.id("nav-group-user-management");
	public static final By CUSTOMER_STATUS = navModule("customer-status", "Customer Status");
	public static final By CUSTOMER_INFORMATION = navModule("customer-information", "Customer Information");
	public static final By BULK_DATA_IMPORT = navModule("bulk-data-import", "Bulk Data Import");
	public static final By CERSAI_CKYC_PROCESS =
			navModule("cersai-ckyc-2.0-process", "CERSAI CKYC 2.0 Process");
	public static final By SEARCH_AND_DOWNLOAD =
			navModule("search-and-download", "Search And Download");
	public static final By SEARCH_AND_DOWNLOAD_GROUP = By.id("nav-group-search-and-download");
	public static final By AI_EXTRACTION = navModule("ai-extraction", "AI Extraction");
	public static final By SETTINGS = navModule("settings", "Settings");

	/**
	 * Masters submodule under expanded {@code #nav-group-masters}.
	 * Live DOM has no per-item id — prefer role=button + span text inside the group.
	 */
	public static By mastersSubmodule(String label) {
		String safe = label == null ? "" : label.trim();
		return By.xpath("//*[@id='nav-group-masters']"
				+ "//*[@role='button'][.//span[normalize-space()=" + quote(safe) + "]]");
	}

	public static final By STATE_MASTER = mastersSubmodule("State Master");
	public static final By DISTRICT_MASTER = mastersSubmodule("District Master");
	public static final By PINCODE_MASTER = mastersSubmodule("Pincode Master");
	/** Locator only — workflow deferred until flow is provided. */
	public static final By PROOF_MASTER = mastersSubmodule("Proof Master");
	/** Locator only — workflow deferred until flow is provided. */
	public static final By HIERARCHY_MASTER = mastersSubmodule("Hierarchy Master");

	/** Search And Download submodule under expanded {@code #nav-group-search-and-download}. */
	public static By searchAndDownloadSubmodule(String label) {
		String safe = label == null ? "" : label.trim();
		return By.xpath("//*[@id='nav-group-search-and-download']"
				+ "//*[@role='button'][.//span[normalize-space()=" + quote(safe) + "]]"
				+ " | //*[@id='nav-group-search-and-download']//*[self::a or @role='link']"
				+ "[normalize-space(.)=" + quote(safe) + "]");
	}

	public static final By SEARCH = searchAndDownloadSubmodule("Search");

	/** Users and Roles under User Management; supports role buttons and direct links. */
	public static By userManagementSubmodule(String label) {
		String safe = label == null ? "" : label.trim();
		return By.xpath("//*[@id='nav-group-user-management']"
				+ "//*[@role='button'][.//span[normalize-space()=" + quote(safe) + "]]"
				+ " | //*[@id='nav-group-user-management']//*[self::a or @role='link']"
				+ "[normalize-space(.)=" + quote(safe) + "]");
	}

	public static final By USERS = userManagementSubmodule("Users");
	public static final By ROLES = userManagementSubmodule("Roles");

	private static String quote(String s) {
		return "'" + s.replace("'", "") + "'";
	}

	/** Live main-nav DOM: id=tour-nav-* and data-tour-label. */
	private static By navModule(String slug, String label) {
		return By.xpath("//*[@id=" + quote("tour-nav-" + slug)
				+ " or @data-tour-label=" + quote(label)
				+ " or @id=" + quote("nav-nav-" + slug)
				+ " or @data-nav-label=" + quote(label)
				+ " or (@role='button' and .//span[normalize-space()=" + quote(label) + "])]");
	}
}
