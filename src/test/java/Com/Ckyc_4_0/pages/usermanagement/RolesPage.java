/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.usermanagement;

import org.openqa.selenium.By;

/** Live-captured Roles Maker/Checker locators at /admin/roles. Never target Super Admin. */
public final class RolesPage {
	private RolesPage() {}

	public static final By PAGE_HEADING = By.xpath(
			"//*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6 or self::div][normalize-space()='Role Management']");
	public static final By MAKER = By.xpath("//button[@aria-label='Maker mode']");
	public static final By CHECKER = By.xpath("//button[@aria-label='Checker mode']");
	public static final By ADD_ROLE = By.xpath(
			"//button[starts-with(@aria-label,'Create a new role') or normalize-space()='Add Role']");
	public static final By ROLE_NAME = By.xpath("//label[normalize-space()='Role Name *']/following::input[1]");
	public static final By ROLE_CODE = By.xpath("//label[normalize-space()='Role Code *']/following::input[1]");
	public static final By DESCRIPTION = By.xpath(
			"//label[normalize-space()='Description *']/following::input[1]"
					+ " | //label[normalize-space()='Description *']/following::textarea[1]"
					+ " | //*[normalize-space()='Description *']/following::input[1]"
					+ " | //*[normalize-space()='Description *']/following::textarea[1]");
	public static final By SAVE = By.xpath("//button[normalize-space()='Save' or normalize-space()='Update']");
	public static final By CANCEL = By.xpath("//button[normalize-space()='Cancel']");
	public static final By CLOSE_PANEL = By.xpath("//button[@aria-label='Close panel' or @aria-label='Close']");
	public static final By CONFIGURE_PERMISSIONS = By.xpath("//button[@aria-label='Configure Permissions']");
	public static final By PERMISSION_DIALOG = By.xpath("//*[contains(normalize-space(),'Permissions —') or contains(normalize-space(),'Permissions -')]");
	public static final By SAVE_PERMISSIONS = By.xpath("//button[normalize-space()='Save Permissions']");
	public static final By PENDING_TAB = By.xpath(
			"//button[contains(normalize-space(),'Pending')] | //*[@role='tab'][contains(normalize-space(),'Pending')]");
	public static final By APPROVED_TAB = By.xpath(
			"//button[contains(normalize-space(),'Approved')] | //*[@role='tab'][contains(normalize-space(),'Approved')]");
	public static final By NEXT_PAGE = By.xpath(
			"//button[@aria-label='Go to next page' or contains(@aria-label,'Next page')]");
	public static final By CONFIRM = By.xpath(
			"//button[normalize-space()='Confirm' or normalize-space()='Yes' or normalize-space()='OK' or normalize-space()='Approve']");
	public static final By SUPER_ADMIN_CARD = By.xpath(
			"//*[contains(normalize-space(),'Super Admin')][ancestor::*[contains(@class,'MuiCard') or contains(@class,'MuiPaper') or self::div][1]]");

	/**
	 * Card / queue row of ONE role: the innermost element that contains the role name and exactly one of the given
	 * action controls. (A wrapper that holds many cards also contains the name but has many controls, so it never matches -
	 * this is what stops the script from acting on the first card/row of the page.)
	 */
	private static String rowWith(String roleName, String controlXpath) {
		return "//*[.//" + controlXpath + "][count(.//" + controlXpath + ")=1]"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")]"
				+ "[not(contains(normalize-space(.),'Super Admin'))]";
	}

	public static By card(String roleName) {
		return By.xpath("//*[.//button[@aria-label='Configure Permissions']]"
				+ "[count(.//button[@aria-label='Configure Permissions'])=1]"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")]"
				+ "[not(contains(normalize-space(.),'Super Admin'))]"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(roleName) + "\")]"
				+ "[not(contains(normalize-space(.),'Super Admin'))]");
	}

	/** MCP-verified: Approve button has no aria-label; parent div has aria-label="Approve". */
	public static By approveFor(String roleName) {
		return By.xpath("(" + rowWith(roleName, "*[@aria-label='Approve']") + "//*[@aria-label='Approve']//button"
				+ " | " + rowWith(roleName, "*[@aria-label='Approve']") + "//button[@aria-label='Approve'])[1]");
	}

	public static By configureFor(String roleName) {
		return By.xpath("(" + rowWith(roleName, "button[@aria-label='Configure Permissions']")
				+ "//button[@aria-label='Configure Permissions'])[1]");
	}

	public static By viewDetailsFor(String roleName) {
		return By.xpath("(" + rowWith(roleName, "*[@aria-label='View details']")
				+ "//button[@aria-label='View details' or contains(@aria-label,'View details')]"
				+ " | " + rowWith(roleName, "*[@aria-label='View details']")
				+ "//*[@aria-label='View details']//button)[1]");
	}

	/** Maker view: the request just submitted shows with status PENDING (case-insensitive). */
	public static By myRequestPending(String key) {
		return By.xpath("//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(key) + "\")]"
				+ "[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'pending')]");
	}

	/** Title of the open permissions dialog, e.g. "Permissions — Branch Checker". */
	public static final By PERMISSION_TITLE = By.xpath(
			"//*[@role='dialog']//*[contains(normalize-space(.),'Permissions') and (contains(normalize-space(.),'—') or contains(normalize-space(.),'-'))]"
					+ "[not(.//*[contains(normalize-space(.),'Permissions')])]");

	public static By childModuleCheckbox(String moduleName) {
		return By.xpath("(//*[normalize-space()=\"" + safe(moduleName) + "\"]/ancestor::*[contains(@class,'Mui') or self::tr or self::div][1]"
				+ "//input[@type='checkbox'])[1]");
	}

	public static By parentModuleCheckbox(String moduleName) {
		return By.xpath("(//*[normalize-space()=\"" + safe(moduleName) + "\"]/ancestor::*[contains(@class,'MuiAccordion') or contains(@class,'MuiPaper') or self::div][1]"
				+ "//input[@type='checkbox'])[1]");
	}

	private static String safe(String value) {
		return value == null ? "" : value.replace("\"", "");
	}
}
