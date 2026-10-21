/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import org.testng.annotations.Test;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.capture.LocatorRecorder;

/**
 * Locator capture session (NOT a test of the application): opens the browser on the login page, then only watches what
 * you do by hand - log in yourself, walk through the screens, click what the automation will have to click. A pop-up
 * appears each time you switch module from the navigation bar. Press Finish in the page (bottom-right) or close the
 * browser to save: Locator Capture/&lt;date&gt;/&lt;time&gt;/capture_report.md, locators_draft.properties, capture.json.
 * Run it from IntelliJ like any runner (right-click, Run). Nothing is typed or clicked for you.
 */
public class TestLocatorRecorder {

	@Test
	public void captureLocatorsByHand() {
		BaseClass.initialization();
		try {
			new LocatorRecorder(BaseClass.getDriver()).run();
		} finally {
			try {
				BaseClass.quitBrowser();
			} catch (RuntimeException ignored) {
				// browser already closed by the tester
			}
		}
	}
}
