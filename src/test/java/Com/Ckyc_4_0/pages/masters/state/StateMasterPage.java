/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.masters.state;

import org.openqa.selenium.By;

/**
 * State Master locators — captured from live UI via MCP (Enterprise Console /ckyc/location).
 */
public final class StateMasterPage {

	private StateMasterPage() {
	}

	public static final By PAGE_HEADING = By.xpath("//h6[normalize-space()='State Master']");
	public static final By TOTAL_STATES_CHIP = By.xpath(
			"(//span[contains(normalize-space(.),'Total States')]"
					+ " | //*[@role='status' and contains(normalize-space(.),'Total States')])[1]");
	public static final By ACTIVE_CHIP = By.xpath(
			"(//span[contains(normalize-space(.),'Active') and not(contains(normalize-space(.),'Inactive'))]"
					+ " | //*[@role='status' and contains(normalize-space(.),'Active')"
					+ " and not(contains(normalize-space(.),'Inactive'))])[1]");
	public static final By INACTIVE_CHIP = By.xpath(
			"(//span[contains(normalize-space(.),'Inactive')]"
					+ " | //*[@role='status' and contains(normalize-space(.),'Inactive')])[1]");
	public static final By SEARCH_INPUT = By.xpath("//input[@aria-label='Search states' or @placeholder='Search states by code or name...']"
			+ " | //label[contains(.,'Search states')]/following::input[1]");
	public static final By RESET_FILTERS = By.xpath("//button[normalize-space()='Reset Filters']");
	public static final By REFRESH_PAGE = By.xpath("//button[@aria-label='Refresh page']");
	public static final By NEW_STATE_BUTTON = By.xpath(
			"//button[normalize-space()='New State' or normalize-space()='Create New State'"
					+ " or @aria-label='New State' or @aria-label='Create New State'"
					+ " or .//*[@data-testid='AddIcon']]");

	public static final By GRID = By.xpath("//div[@role='grid']");
	public static final By GRID_DATA_ROWS = By.xpath(
			"//div[@role='grid']//div[@role='rowgroup']//div[@role='row']");
	public static final By NEXT_PAGE = By.xpath("//button[@aria-label='Go to next page']");
	public static final By PREV_PAGE = By.xpath("//button[@aria-label='Go to previous page']");
	public static final By PAGINATION_INFO = By.xpath("//p[contains(normalize-space(),'of')]");

	public static final By CREATE_PANEL_TITLE = By.xpath("//h6[normalize-space()='Create State']");
	public static final By EDIT_PANEL_TITLE = By.xpath("//h6[normalize-space()='Edit State']");
	public static final By STATE_CODE_INPUT = By.xpath("//input[@aria-label='State code' or @placeholder='e.g. TN']");
	public static final By STATE_NAME_INPUT = By.xpath("//input[@aria-label='State name' or @placeholder='e.g. TAMIL NADU']");
	public static final By ACTIVE_CHECKBOX = By.xpath("//input[@aria-label='Active' or @type='checkbox'][@aria-label='Active']"
			+ " | //label[.//text()[contains(.,'Active')]]//input[@type='checkbox']");
	public static final By CREATE_STATE_BUTTON = By.xpath("//button[normalize-space()='Create State']");
	public static final By CLEAR_BUTTON = By.xpath("//button[normalize-space()='Clear']");
	/** Edit panel primary action — may be icon-only Save next to Clear. */
	public static final By SAVE_OR_UPDATE_BUTTON = By.xpath(
			"//button[normalize-space()='Save' or normalize-space()='Update' or normalize-space()='Save State'"
					+ " or normalize-space()='Update State']"
					+ " | //h6[normalize-space()='Edit State']/following::button[not(normalize-space()='Clear')"
					+ " and not(contains(@aria-label,'Cancel'))][1]");
	public static final By CODE_HINT = By.xpath("//p[contains(.,'Two capital letters')]");
	public static final By NAME_HINT = By.xpath("//p[contains(.,'Letters and single spaces')]");

	public static By gridCellCode(int rowIndex1Based) {
		return By.xpath("(//div[@role='grid']//div[@role='rowgroup']//div[@role='row'])[" + rowIndex1Based
				+ "]//div[@role='gridcell'][2]//p");
	}

	public static By gridCellName(int rowIndex1Based) {
		return By.xpath("(//div[@role='grid']//div[@role='rowgroup']//div[@role='row'])[" + rowIndex1Based
				+ "]//div[@role='gridcell'][3]//p");
	}

	public static By rowContainingText(String text) {
		String t = text == null ? "" : text.replace("'", "");
		return By.xpath("//div[@role='grid']//div[@role='rowgroup']//div[@role='row'][.//p[contains(normalize-space(),'"
				+ t + "')]]");
	}
}
