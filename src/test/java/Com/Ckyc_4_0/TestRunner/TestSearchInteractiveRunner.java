/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * Interactive Search session: signs in (Background step), opens Search And Download &gt; Search, then asks in popups for
 * the Document Type and the number. Close in the popup ends the run. Report: Report Output/Search Results/&lt;date&gt;/.
 * Not tagged @Positive on purpose - it needs an operator, so the unattended "all" runners never start it.
 */
@CucumberOptions(
		features = { "src/test/resources/Features/4_SearchAndDownload/1_Search" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-searchinteractive.json",
				"html:target/cucumber-reports/searchinteractive/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@SearchInteractive",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestSearchInteractiveRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestSearchInteractiveRunner", "Search And Download > Search (interactive)", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestSearchInteractiveRunner");
	}
}
