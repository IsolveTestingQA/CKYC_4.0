/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.masters.hierarchy;

import org.openqa.selenium.By;

/**
 * Hierarchy Master locators — captured from live UI (Enterprise Console /ckyc/hierarchy).
 * Tabs: FI Master, Region, CPC Master, Branch.
 */
public final class HierarchyMasterPage {

	private HierarchyMasterPage() {
	}

	public static final By PAGE_HEADING = By.xpath("//h6[normalize-space()='Hierarchy Master']");
	public static final By REFRESH_PAGE = By.xpath("//button[@aria-label='Refresh page']");
	public static final By RESET_FILTERS = By.xpath("//button[normalize-space()='Reset Filters']");
	public static final By NEW_RECORD_BUTTON = By.xpath(
			"//button[normalize-space()='New Record' or @aria-label='New Record'"
					+ " or contains(@aria-label,'New Record')]"
					+ " | //button[normalize-space()='Reset Filters']/following-sibling::button[1]");
	public static final By CREATE_RECORD_BUTTON = By.xpath("//button[normalize-space()='Create Record']");
	public static final By CLEAR_BUTTON = By.xpath("//button[normalize-space()='Clear']");
	public static final By SAVE_OR_UPDATE_BUTTON = By.xpath(
			"//button[normalize-space()='Save' or normalize-space()='Update' or normalize-space()='Save Record'"
					+ " or normalize-space()='Update Record']");
	public static final By CANCEL_OR_CLOSE = By.xpath(
			"//h6[contains(normalize-space(),'Create') or contains(normalize-space(),'Edit')]"
					+ "/ancestor::div[contains(@class,'MuiDrawer-root') or contains(@class,'MuiPaper-root')][1]"
					+ "//button[normalize-space()='Cancel' or @aria-label='Close' or .//*[@data-testid='CloseIcon']]"
					+ " | //h6[contains(normalize-space(),'Create') or contains(normalize-space(),'Edit')]"
					+ "/following::button[.//*[@data-testid='CloseIcon']][1]");
	public static final By ACTIVE_CHECKBOX = By.xpath(
			"//h6[contains(normalize-space(),'Create') or contains(normalize-space(),'Edit')]"
					+ "/following::input[@type='checkbox'][@aria-label='Active' or following-sibling::*[contains(.,'Active')]][1]"
					+ " | //label[contains(normalize-space(.),'Active')]//input[@type='checkbox']");

	public static final By GRID = By.xpath("//div[@role='grid']");
	public static final By GRID_DATA_ROWS = By.xpath(
			"//div[@role='grid']//div[@role='rowgroup']//div[@role='row'][.//div[@role='gridcell']]");

	public static final By CHIP_FI = By.xpath("(//*[contains(normalize-space(.),'FI Institutions')])[1]");
	public static final By CHIP_REGION = By.xpath("(//*[contains(normalize-space(.),'Regions') and not(contains(.,'Region Code'))])[1]");
	public static final By CHIP_CPC = By.xpath("(//*[contains(normalize-space(.),'CPC Centres')])[1]");
	public static final By CHIP_BRANCH = By.xpath("(//*[contains(normalize-space(.),'Branches') and not(contains(.,'Branch Code'))])[1]");

	// ---------- FI Master ----------
	public static final By TABLIST = By.xpath("//div[@role='tablist']");
	public static final By TAB_FI_MASTER = By.xpath(
			"//*[@role='tablist']//*[@role='tab'][contains(.,'FI Master')]"
					+ " | //*[@role='tab'][contains(.,'FI Master')]");
	public static final By SEARCH_FI = By.xpath(
			"//input[@aria-label='Search fi master…' or contains(@placeholder,'Search fi master')"
					+ " | //input[contains(@aria-label,'Search fi master')]");
	public static final By CREATE_FI_TITLE = By.xpath("//h6[normalize-space()='Create FI Master']");
	public static final By EDIT_FI_TITLE = By.xpath("//h6[normalize-space()='Edit FI Master']");
	public static final By FI_NAME_INPUT = By.xpath(
			"//input[@aria-label='FI Name *' or contains(@aria-label,'FI Name')]");
	public static final By FI_CODE_INPUT = By.xpath(
			"//input[@aria-label='FI Code *' or contains(@aria-label,'FI Code')]");

	// ---------- Region ----------
	public static final By TAB_REGION = By.xpath(
			"//*[@role='tablist']//*[@role='tab'][contains(.,'Region') and not(contains(.,'Branch'))]"
					+ " | //*[@role='tab'][contains(.,'Region') and not(contains(.,'Branch'))]");
	public static final By SEARCH_REGION = By.xpath(
			"//input[@aria-label='Search region…' or contains(@placeholder,'Search region')]");
	public static final By CREATE_REGION_TITLE = By.xpath("//h6[normalize-space()='Create Region']");
	public static final By EDIT_REGION_TITLE = By.xpath("//h6[normalize-space()='Edit Region']");
	public static final By REGION_CODE_INPUT = By.xpath("//input[contains(@aria-label,'Region Code')]");
	public static final By REGION_NAME_INPUT = By.xpath("//input[contains(@aria-label,'Region Name')]");
	public static final By REGION_FI_COMBO = By.xpath(
			"//label[contains(normalize-space(.),'Financial Institution')]/following::div[@role='combobox'][1]"
					+ " | //*[contains(normalize-space(.),'Financial Institution')]/following::div[@role='combobox'][1]");
	public static final By REGION_CPC_COMBO = By.xpath(
			"//label[contains(normalize-space(.),'CPC')]/following::div[@role='combobox'][1]"
					+ " | //*[contains(normalize-space(.),'CPC (optional)')]/following::div[@role='combobox'][1]");

	// ---------- CPC Master ----------
	public static final By TAB_CPC = By.xpath(
			"//*[@role='tablist']//*[@role='tab'][contains(.,'CPC Master')]"
					+ " | //*[@role='tab'][contains(.,'CPC Master')]");
	public static final By SEARCH_CPC = By.xpath(
			"//input[@aria-label='Search cpc master…' or contains(@placeholder,'Search cpc master')]");
	public static final By CREATE_CPC_TITLE = By.xpath("//h6[normalize-space()='Create CPC Master']");
	public static final By EDIT_CPC_TITLE = By.xpath("//h6[normalize-space()='Edit CPC Master']");
	public static final By CPC_CODE_INPUT = By.xpath("//input[contains(@aria-label,'CPC Code')]");
	public static final By CPC_CITY_INPUT = By.xpath("//input[contains(@aria-label,'City')]");

	// ---------- Branch ----------
	public static final By TAB_BRANCH = By.xpath(
			"//*[@role='tablist']//*[@role='tab'][contains(.,'Branch')]"
					+ " | //*[@role='tab'][contains(.,'Branch')]");
	public static final By SEARCH_BRANCH = By.xpath(
			"//input[@aria-label='Search branch…' or contains(@placeholder,'Search branch')]");
	public static final By CREATE_BRANCH_TITLE = By.xpath("//h6[normalize-space()='Create Branch']");
	public static final By EDIT_BRANCH_TITLE = By.xpath("//h6[normalize-space()='Edit Branch']");
	public static final By BRANCH_CODE_INPUT = By.xpath("//input[contains(@aria-label,'Branch Code')]");
	public static final By BRANCH_NAME_INPUT = By.xpath("//input[contains(@aria-label,'Branch Name')]");
	public static final By BRANCH_REGION_COMBO = By.xpath(
			"//label[contains(normalize-space(.),'Region') and not(contains(.,'Branch'))]"
					+ "/following::div[@role='combobox'][1]"
					+ " | //*[contains(normalize-space(.),'Region *')]/following::div[@role='combobox'][1]");
	public static final By BRANCH_CPC_COMBO = By.xpath(
			"//label[contains(normalize-space(.),'CPC')]/following::div[@role='combobox'][1]"
					+ " | //*[contains(normalize-space(.),'CPC (optional)')]/following::div[@role='combobox'][1]");

	public static final By REQUIRED_HELPER = By.xpath("//p[contains(@class,'MuiFormHelperText') and normalize-space()='Required']");
}
