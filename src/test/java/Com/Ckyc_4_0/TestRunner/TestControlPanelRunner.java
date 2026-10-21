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
 * THE one runner: opens the browser on the login page, shows the Test Control Panel, and then executes only what was
 * ticked there (type of testing, modules, options). The panel sets the Cucumber tag filter before Cucumber lists the
 * scenarios, so the tags below are only the default used when no operator is present (headless /
 * interactiveBlockerPrompt=false): every positive scenario that is not deferred.
 */
@CucumberOptions(
		features = { "src/test/resources/Features" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-controlpanel.json",
				"html:target/cucumber-reports/controlpanel/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive and not @Deferred",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestControlPanelRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		// the common suite start opens the browser and shows the Test Control Panel
		RunnerSuiteSupport.beforeSuite("TestControlPanelRunner", "Test Control Panel - modules chosen at run time", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestControlPanelRunner");
	}
}
