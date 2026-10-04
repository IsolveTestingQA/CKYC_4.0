/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.account;

import org.openqa.selenium.By;

/**
 * Top-right account avatar menu — logout flow after suite completion.
 */
public final class AccountMenuPage {

	private AccountMenuPage() {
	}

	/**
	 * Avatar button — always the last button in the header bar, sibling after
	 * "Expand quick options". Works for any user (auto→"Automation", shyam→"Shyamsundar N", etc.).
	 * MCP-verified: button[aria-label="<display name>"] with child MuiAvatar div.
	 */
	public static final By AVATAR_BUTTON = By.xpath(
			"//button[contains(@class,'MuiIconButton')][.//*[contains(@class,'MuiAvatar')]]"
					+ " | //button[contains(@aria-label,'Expand quick options')]/following-sibling::button[1]");

	/** Logout menuitem inside avatar dropdown menu. MCP-verified: [role=menuitem] with text "Logout". */
	public static final By LOGOUT_MENU_ITEM = By.xpath(
			"//*[@role='menuitem'][contains(normalize-space(.),'Logout')]"
					+ " | //li[@role='menuitem'][contains(normalize-space(.),'Logout')]");
}
