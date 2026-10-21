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
 * DVS 2.0 Legal Entity positive suite (LE_POSITIVE). Login runs first (credentials from config), DVS 2.0 is opened from the side
 * navigation, then every scenario of DVS/LE_Scenarios.csv of this type is run on the configured
 * customer: value, Save, Submit to Checker, read the answer, restore the record. The customer comes from
 * dvs.cust.le in config.properties. No scenario retry listener on purpose.
 */
@CucumberOptions(
		features = { "src/test/resources/Features/1_Login", "src/test/resources/Features/7_DVS" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-dvslepos.json",
				"html:target/cucumber-reports/dvslepos/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Login or @DVS_LE_POS",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestDVSLegalPositiveRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestDVSLegalPositiveRunner", "DVS Legal Entity positive", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestDVSLegalPositiveRunner");
	}
}
