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
	public static final By ADD_ROLE = By.xpath("//button[starts-with(@aria-label,'Create a new role')]");
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

	public static By card(String roleName) {
		return By.xpath("//*[contains(@class,'MuiCard') or contains(@class,'MuiPaper') or self::article or @role='row']"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")]"
				+ "[not(contains(normalize-space(.),'Super Admin'))]");
	}

	/** MCP-verified: Approve button has no aria-label; parent div has aria-label="Approve". */
	public static By approveFor(String roleName) {
		return By.xpath("(//*[contains(@class,'MuiCard') or contains(@class,'MuiPaper') or @role='row' or self::tr]"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")][not(contains(normalize-space(.),'Super Admin'))]"
				+ "//*[@aria-label='Approve']//button"
				+ " | //*[contains(@class,'MuiCard') or contains(@class,'MuiPaper') or @role='row' or self::tr]"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")][not(contains(normalize-space(.),'Super Admin'))]"
				+ "//button[@aria-label='Approve'])[1]");
	}

	public static By configureFor(String roleName) {
		return By.xpath("(//*[contains(@class,'MuiCard') or contains(@class,'MuiPaper') or self::article]"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")][not(contains(normalize-space(.),'Super Admin'))]"
				+ "//button[@aria-label='Configure Permissions' or contains(@aria-label,'Permissions')])[1]");
	}

	public static By viewDetailsFor(String roleName) {
		return By.xpath("(//*[contains(@class,'MuiCard') or contains(@class,'MuiPaper') or @role='row' or self::tr]"
				+ "[contains(normalize-space(.),\"" + safe(roleName) + "\")][not(contains(normalize-space(.),'Super Admin'))]"
				+ "//button[@aria-label='View details' or contains(@aria-label,'View details')])[1]");
	}

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
