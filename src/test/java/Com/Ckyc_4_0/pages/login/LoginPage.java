/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.login;

import org.openqa.selenium.By;

/**
 * Page Object locators — CKYC Login + post-login app dialogs.
 * Canonical page package (product framework layout).
 */
public final class LoginPage {

	private LoginPage() {
	}

	public static final By RUNTIME_API_BUTTON = By.xpath("//button[@value='API' and @aria-label='Use live API']");
	public static final By RUNTIME_MOCK_BUTTON = By.xpath("//button[@value='MOCK' and @aria-label='Use mock data']");

	public static final By USERNAME_INPUT = By.xpath("//input[@name='loginId']");
	public static final By PASSWORD_INPUT = By.xpath("//input[@name='password']");
	public static final By SIGN_IN_BUTTON = By.xpath(
			"//button[normalize-space()='Sign In']"
					+ " | //form//button[@type='submit']"
					+ " | //button[@aria-label='Sign In']");

	/**
	 * Active session detected — MUI dialog (aria-labelledby=session-active-dialog-title).
	 * Shows only sometimes after Sign In when another session is open.
	 */
	public static final By ACTIVE_SESSION_DIALOG = By.xpath(
			"//div[@role='dialog' and (@aria-labelledby='session-active-dialog-title' "
					+ "or @aria-describedby='session-active-dialog-desc' "
					+ "or .//span[normalize-space()='Active session detected'])]");

	/** Primary action on Active session dialog. */
	public static final By LOGOUT_OTHER_SESSION_CONTINUE = By.xpath(
			"//div[@role='dialog' and (@aria-labelledby='session-active-dialog-title' "
					+ "or .//span[normalize-space()='Active session detected'])]"
					+ "//button[contains(normalize-space(.),'Log out other session')]"
					+ " | //button[contains(normalize-space(.),'Log out other session & continue')]"
					+ " | //button[contains(normalize-space(.),'Log out other session')]");

	public static final By ACTIVE_SESSION_CANCEL = By.xpath(
			"//div[@role='dialog' and (@aria-labelledby='session-active-dialog-title' "
					+ "or .//span[normalize-space()='Active session detected'])]"
					+ "//button[normalize-space()='Cancel']");
}
