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
 * DVS 2.0 Individual negative suite (IND_NEGATIVE). Login runs first (credentials from config), DVS 2.0 is opened from the side
 * navigation, then every scenario of DVS/IND_Scenarios.csv of this type is run on the configured
 * customer: value, Save, Submit to Checker, read the answer, restore the record. The customer comes from
 * dvs.cust.individual in config.properties. No scenario retry listener on purpose.
 */
@CucumberOptions(
		features = { "src/test/resources/Features/1_Login", "src/test/resources/Features/7_DVS" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-dvsindneg.json",
				"html:target/cucumber-reports/dvsindneg/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Login or @DVS_IND_NEG",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestDVSIndividualNegativeRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestDVSIndividualNegativeRunner", "DVS Individual negative", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestDVSIndividualNegativeRunner");
	}
}
