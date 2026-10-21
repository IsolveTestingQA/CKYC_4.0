/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import org.testng.annotations.Test;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.functionality.login.LoginFunctionality;
import Com.Ckyc_4_0.utils.capture.LocatorCrawler;

/**
 * Locator crawl (NOT a test of the application): logs in with the config.properties user, then walks the whole application
 * by itself and writes what it finds - Locator Capture/&lt;date&gt;/&lt;time&gt;_crawl/ : pages_overview.md, new_elements.md (on the
 * page but not in the code), stale_locators.md (in the code but not on any page), locators_catalog.properties, catalog.json and
 * generated_pages/*.java (ready-to-paste By constants). It only opens safe things (side navigation, tabs, New/Add panels,
 * one Dashboard list) and never types, saves, deletes, approves, uploads, downloads or restarts anything.
 * Run it from IntelliJ like any runner. Use it again after the application changes.
 */
public class TestLocatorCrawler {

	@Test
	public void crawlApplicationLocators() {
		BaseClass.initialization();
		try {
			LoginFunctionality login = new LoginFunctionality();
			login.verifyLoginPageIsDisplayed();
			login.selectRuntimeMode(Com.Ckyc_4_0.UtilityFiles.ConfigReader.getRuntimeMode());
			login.enterCredentialsFromConfig();
			login.clickSignIn();
			login.verifyUserLoggedInSuccessfully();
			new LocatorCrawler(BaseClass.getDriver()).run();
		} finally {
			try {
				BaseClass.quitBrowser();
			} catch (RuntimeException ignored) {
				// browser already closed
			}
		}
	}
}
