/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.masters.pincode;

import org.openqa.selenium.By;

/**
 * Pincode Master locators — captured from live UI via MCP (/ckyc/pincode).
 */
public final class PincodeMasterPage {

	private PincodeMasterPage() {
	}

	public static final By PAGE_HEADING = By.xpath("//h6[normalize-space()='Pincode Master']");
	public static final By TOTAL_CHIP = By.xpath("//span[contains(normalize-space(),'Total Pincodes')]");
	public static final By ACTIVE_CHIP = By.xpath(
			"//span[contains(normalize-space(),'Active') and not(contains(.,'Inactive')) and not(contains(.,'Total'))]");
	public static final By DISTRICTS_COVERED_CHIP = By.xpath("//span[contains(normalize-space(),'Districts Covered')]");
	public static final By STATES_COVERED_CHIP = By.xpath("//span[contains(normalize-space(),'States Covered')]");
	/*
	 * Verified live: this screen's inputs carry no aria-label/placeholder in the DOM
	 * (the accessible name comes from a sibling <label>), so inputs are reached through
	 * their label and the selects through their stable aria-labelledby ids.
	 */
	public static final By SEARCH_INPUT = By.xpath(
			"//label[starts-with(normalize-space(.),'Search pincode')]/following::input[1]");
	public static final By STATE_FILTER = By.cssSelector("div[role='combobox'][aria-labelledby='pincode-state-filter']");
	public static final By NEW_PINCODE_BUTTON = By.xpath("//button[contains(.,'New Pincode')]");
	public static final By CREATE_HEADING = By.xpath("//h6[normalize-space()='Create Pincode' or normalize-space()='Edit Pincode']");
	public static final By PINCODE_INPUT = By.xpath(
			"//label[normalize-space()='Pincode *']/following::input[1]");
	public static final By STATE_COMBO = By.cssSelector("div[role='combobox'][aria-labelledby='pincode-form-state']");
	public static final By DISTRICT_COMBO = By.cssSelector("div[role='combobox'][aria-labelledby='pincode-form-district']");
	public static final By ACTIVE_CHECKBOX = By.xpath(
			"//h6[contains(.,'Pincode')]/following::input[@type='checkbox'][1] | //input[@aria-label='Active']");
	public static final By CREATE_BUTTON = By.xpath("//button[normalize-space()='Create Pincode']");
	public static final By CLEAR_BUTTON = By.xpath("//button[normalize-space()='Clear']");
	public static final By SAVE_BUTTON = By.xpath(
			"//button[normalize-space()='Save Pincode' or normalize-space()='Update Pincode' or normalize-space()='Save']");
}
