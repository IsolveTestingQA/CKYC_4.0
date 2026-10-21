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
import org.testng.annotations.Listeners;

/**
 * Login first, then Masters only (State → District → Pincode → Hierarchy).
 */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/3_Masters"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-masters.json",
				"html:target/cucumber-reports/masters/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestMastersRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestMastersRunner",
				"Login → Masters (State, District, Pincode, Hierarchy) — Proof deferred", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestMastersRunner");
	}
}
