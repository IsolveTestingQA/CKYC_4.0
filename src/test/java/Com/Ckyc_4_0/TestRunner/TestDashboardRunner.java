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
 * Login first, then Dashboard module only.
 */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/2_Dashboard"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-dashboard.json",
				"html:target/cucumber-reports/dashboard/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestDashboardRunner extends AbstractTestNGCucumberTests {

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestDashboardRunner", "Login → Dashboard");
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestDashboardRunner");
	}
}
