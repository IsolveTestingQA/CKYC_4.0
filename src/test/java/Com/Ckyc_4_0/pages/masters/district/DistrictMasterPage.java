/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.masters.district;

import org.openqa.selenium.By;

/**
 * District Master locators — captured from live UI via MCP (/ckyc/district).
 */
public final class DistrictMasterPage {

	private DistrictMasterPage() {
	}

	public static final By PAGE_HEADING = By.xpath("//h6[normalize-space()='District Master']");
	public static final By TOTAL_CHIP = By.xpath("//span[contains(normalize-space(),'Total Districts')]");
	public static final By ACTIVE_CHIP = By.xpath(
			"//span[contains(normalize-space(),'Active') and not(contains(.,'Inactive')) and not(contains(.,'Total'))]");
	public static final By INACTIVE_CHIP = By.xpath("//span[contains(normalize-space(),'Inactive')]");
	public static final By STATES_COVERED_CHIP = By.xpath("//span[contains(normalize-space(),'States Covered')]");
	public static final By SEARCH_INPUT = By.xpath("//input[@aria-label='Search districts']");
	/** Verified live: filter and create-form selects carry stable aria-labelledby ids. */
	public static final By STATE_FILTER = By.cssSelector("div[role='combobox'][aria-labelledby='state-filter-label']");
	public static final By NEW_DISTRICT_BUTTON = By.xpath("//button[contains(.,'New District')]");
	public static final By CREATE_HEADING = By.xpath("//h6[normalize-space()='Create District' or normalize-space()='Edit District']");
	public static final By STATE_COMBO = By.cssSelector("div[role='combobox'][aria-labelledby='form-state-label']");
	public static final By DISTRICT_NAME_INPUT = By.xpath("//input[@aria-label='District name' or @placeholder='e.g. CHENNAI']");
	public static final By ACTIVE_CHECKBOX = By.xpath(
			"//h6[contains(.,'District')]/following::input[@type='checkbox'][1] | //input[@aria-label='Active']");
	public static final By CREATE_BUTTON = By.xpath("//button[normalize-space()='Create District']");
	public static final By CLEAR_BUTTON = By.xpath("//button[normalize-space()='Clear']");
	public static final By SAVE_BUTTON = By.xpath(
			"//button[normalize-space()='Save District' or normalize-space()='Update District' or normalize-space()='Save']");
}

