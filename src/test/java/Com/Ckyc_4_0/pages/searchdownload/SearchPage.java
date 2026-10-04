/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.searchdownload;

import org.openqa.selenium.By;

/**
 * Search And Download > Search locators captured from the live Enterprise Console.
 */
public final class SearchPage {

	private SearchPage() {
	}

	public static final By PAGE_HEADING = By.xpath(
			"//h6[normalize-space()='Search'] | //h5[normalize-space()='Search']"
					+ " | //nav//*[normalize-space()='Search']");
	public static final By DOCUMENT_TYPE_COMBOBOX = By.xpath(
			"//label[contains(normalize-space(),'Document Type')]/following::*[@role='combobox'][1]"
					+ " | //*[@role='combobox'][@aria-label='Document Type' or @name='documentType'][1]");
	public static final By DOCUMENT_NUMBER_INPUT = By.xpath(
			"//input[@placeholder='e.g. PZRPS7071C' or @placeholder='e.g. 9876543210'"
					+ " or @aria-label='Document Number' or @name='documentNumber']"
					+ " | //label[contains(normalize-space(),'Document Number')]/following::input[1]");
	public static final By SEARCH_BUTTON = By.xpath(
			"//button[normalize-space()='Search' or @aria-label='Search'][not(@title='Search')]");
	public static final By CLEAR_BUTTON = By.xpath("//button[normalize-space()='Clear' or @aria-label='Clear']");
	public static final By RESULT_REGION = By.xpath(
			"//*[@role='grid' or @role='table']"
					+ " | //*[contains(@class,'MuiCard-root')][.//*[contains(normalize-space(),'CKYC')]]"
					+ " | //*[contains(normalize-space(),'CKYC Ref') or contains(normalize-space(),'CKYC Identifier')]");
	public static final By RESULT_IDENTIFIER = By.xpath(
			"//*[contains(normalize-space(),'CKYC Ref') or contains(normalize-space(),'CKYC Identifier')"
					+ " or contains(normalize-space(),'Reference Number')]");
	public static final By ALERT = By.xpath("//*[@role='alert' or contains(@class,'MuiAlert-root')]");

	public static final By CUSTOMER_PHOTO_LABEL = By.xpath(
			"//*[normalize-space()='Customer Photo' or normalize-space()='CUSTOMER PHOTO']");
	public static final By CUSTOMER_PHOTO_SECTION = By.xpath(
			"(//*[normalize-space()='Customer Photo' or normalize-space()='CUSTOMER PHOTO'])[1]"
					+ "/ancestor::div[contains(@class,'MuiBox-root') or contains(@class,'MuiPaper') "
					+ "or contains(@class,'MuiCard')][1]");
	public static final By CUSTOMER_PHOTO_TYPE_CHIP = By.xpath(
			"(//*[normalize-space()='Customer Photo' or normalize-space()='CUSTOMER PHOTO'])[1]"
					+ "/ancestor::div[contains(@class,'MuiBox-root')][1]"
					+ "//*[contains(@class,'MuiChip-label')]");
	public static final By CUSTOMER_PHOTO_IMAGE = By.xpath(
			"(//*[normalize-space()='Customer Photo' or normalize-space()='CUSTOMER PHOTO'])[1]"
					+ "/ancestor::div[contains(@class,'MuiBox-root') or contains(@class,'MuiPaper')"
					+ " or contains(@class,'MuiCard')][2]//img"
					+ "[not(contains(@src,'logo')) and not(contains(@alt,'iFlow'))"
					+ " and not(contains(@alt,'iSolve'))]"
					+ " | (//*[normalize-space()='Customer Photo' or normalize-space()='CUSTOMER PHOTO'])[1]"
					+ "/following::img[1][not(contains(@src,'logo'))]");
	public static final By CUSTOMER_PHOTO_PLACEHOLDER_ICON = By.xpath(
			"(//*[normalize-space()='Customer Photo' or normalize-space()='CUSTOMER PHOTO'])[1]"
					+ "/ancestor::div[contains(@class,'MuiBox-root')][1]"
					+ "//*[@data-testid='ImageOutlinedIcon' or @data-testid='BrokenImageIcon'"
					+ " or @data-testid='PersonIcon' or @data-testid='NoPhotographyIcon']");
	public static final By PHOTO_PREVIEW_DIALOG = By.xpath(
			"//*[@role='dialog' or contains(@class,'MuiDialog-root') or contains(@class,'MuiModal-root')]"
					+ "[.//img or .//*[contains(normalize-space(),'Photo') or contains(normalize-space(),'Preview')]]");

	/** Value paired with an exact result-card label (for example CKYC Number). */
	public static By resultValue(String label) {
		String safe = label == null ? "" : label.replace("'", "");
		return By.xpath("(//*[normalize-space()='" + safe + "']"
				+ "/following-sibling::*[self::p or self::div or self::span][1]"
				+ " | //*[normalize-space()='" + safe + "']/following::*[self::p or self::div or self::span][1])[1]");
	}
}
