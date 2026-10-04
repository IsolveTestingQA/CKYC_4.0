/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.usermanagement;

import org.openqa.selenium.By;

/** Live-captured Users Maker/Checker locators at /admin/users. */
public final class UsersPage {
	private UsersPage() {}

	public static final By PAGE_HEADING = By.xpath(
			"//*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6 or self::div][normalize-space()='User Management']");
	public static final By MAKER = By.xpath("//button[@aria-label='Maker mode']");
	public static final By CHECKER = By.xpath("//button[@aria-label='Checker mode']");
	public static final By ADD_USER = By.xpath("//button[starts-with(@aria-label,'Create a new user')]");
	public static final By CLOSE_PANEL = By.xpath("//button[@aria-label='Close panel']");
	public static final By SAVE = By.xpath("//button[normalize-space()='Save' or normalize-space()='Update']");
	public static final By SEARCH = By.xpath("//input[@placeholder='Search name / login / email...']");
	public static final By FIRST_NAME = By.xpath("//label[normalize-space()='First Name *']/following::input[1]");
	public static final By MIDDLE_NAME = By.xpath("//label[normalize-space()='Middle Name']/following::input[1]");
	public static final By LAST_NAME = By.xpath("//label[normalize-space()='Last Name']/following::input[1]");
	public static final By DESIGNATION = By.xpath("//label[normalize-space()='Designation *']/following::input[1]");
	public static final By EMAIL = By.xpath("//label[normalize-space()='Email ID *']/following::input[1]");
	public static final By MOBILE = By.xpath("//label[contains(normalize-space(),'Mobile')]/following::input[1]");
	public static final By LOGIN_NAME = By.xpath("//label[normalize-space()='Login Name *']/following::input[1]");
	public static final By SOL_COMBO = By.xpath("//*[contains(normalize-space(),'SOL/REG/CPC ID *')]/following::div[@role='combobox'][1]");
	public static final By ROLE_COMBO = By.xpath("//*[normalize-space()='Role *']/following::div[@role='combobox'][1]");
	public static final By ACTIVE_STATUS = By.xpath("//*[normalize-space()='Status']/following::input[@type='checkbox'][1]");
	public static final By DORMANT_OPTION = By.xpath(
			"//*[self::button or self::li or @role='option' or @role='menuitem' or @role='radio' or self::span or self::p]"
					+ "[normalize-space()='Dormant' or contains(normalize-space(),'Dormant')]");
	public static final By STATUS_COMBO = By.xpath("//*[normalize-space()='Status' or normalize-space()='Status *']/following::div[@role='combobox'][1]");
	public static final By CHECKER_QUEUE = By.xpath(
			"//*[contains(normalize-space(),'USER REQUEST QUEUE') or contains(normalize-space(),'User Approvals') or contains(normalize-space(),'MY REQUESTS')]");
	public static final By PENDING_TAB = By.xpath(
			"//button[contains(normalize-space(),'Pending')] | //*[@role='tab'][contains(normalize-space(),'Pending')]");
	public static final By APPROVED_TAB = By.xpath(
			"//button[contains(normalize-space(),'Approved')] | //*[@role='tab'][contains(normalize-space(),'Approved')]");
	public static final By REVERTED_TAB = By.xpath(
			"//button[contains(normalize-space(),'Reverted')] | //*[@role='tab'][contains(normalize-space(),'Reverted')]");
	public static final By CONFIRM = By.xpath(
			"//button[normalize-space()='Confirm' or normalize-space()='Yes' or normalize-space()='OK' or normalize-space()='Approve']");
	public static final By ALERT = By.xpath("//*[@role='alert' or contains(@class,'MuiAlert') or contains(@class,'Toastify')]");
	public static final By NEXT_PAGE = By.xpath(
			"//button[@aria-label='Go to next page' or contains(@aria-label,'Next page')]");
	public static final By ROWS_PER_PAGE = By.xpath(
			"//*[(@role='combobox') and (contains(@aria-label,'Rows per page') or contains(normalize-space(.),'Rows per page'))][1]");

	public static By row(String login) {
		return By.xpath("//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]");
	}

	/**
	 * MCP-verified: Approve button has NO aria-label — the parent div has aria-label="Approve".
	 * Structure: div[aria-label="Approve"] > button (MuiIconButton)
	 */
	public static By approveFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//*[@aria-label='Approve']//button"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[@aria-label='Approve'])[1]");
	}

	/** MCP-verified: parent div has aria-label="Revert to maker", button inside has no label. */
	public static By revertFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//*[contains(@aria-label,'Revert')]//button"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[contains(@aria-label,'Revert')])[1]");
	}

	/** MCP-verified: parent div has aria-label="Edit", button inside. */
	public static By editFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//*[@aria-label='Edit']//button"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[contains(@aria-label,'Edit')])[1]");
	}

	/** MCP-verified: parent div has aria-label="Lock Account", button has aria-label="Lock Account". */
	public static By lockFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//*[contains(@aria-label,'Lock')]//button"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[contains(@aria-label,'Lock')])[1]");
	}

	/** MCP-verified: similar pattern — aria-label on parent or button. */
	public static By unlockFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//*[contains(@aria-label,'Unlock')]//button"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[contains(@aria-label,'Unlock')])[1]");
	}

	/** MCP-verified: View details button has aria-label="View details". */
	public static By viewDetailsFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[@aria-label='View details'])[1]");
	}

	/** MCP-verified: Deactivate tooltip parent or button. */
	public static By deactivateFor(String login) {
		return By.xpath("(//*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//*[@aria-label='Deactivate' or @aria-label='Activate']//button"
				+ " | //*[self::tr or @role='row'][contains(normalize-space(.),\"" + safe(login) + "\")]"
				+ "//button[contains(@aria-label,'Deactivate') or contains(@aria-label,'Activate')])[1]");
	}

	private static String safe(String value) {
		return value == null ? "" : value.replace("\"", "");
	}
}
