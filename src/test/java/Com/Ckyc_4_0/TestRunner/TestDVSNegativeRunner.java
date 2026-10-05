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
 * DVS negative run. Sign in manually in the opened browser; the run waits for the DVS shell.
 * No scenario retry listener on purpose: a retry would repeat the whole data run.
 */
@CucumberOptions(
		features = { "src/test/resources/Features/7_DVS" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-dvsnegative.json",
				"html:target/cucumber-reports/dvsnegative/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@DVS_NEG",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestDVSNegativeRunner extends AbstractTestNGCucumberTests {

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestDVSNegativeRunner", "DVS negative");
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestDVSNegativeRunner");
	}
}
