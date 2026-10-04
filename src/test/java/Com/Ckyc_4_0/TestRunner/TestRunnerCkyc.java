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
 * @deprecated Prefer {@link TestCKYCAllRunner}. Alias kept for older IDE run configurations.
 */
@Deprecated
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/2_Dashboard",
				"src/test/resources/Features/3_Masters"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber.json",
				"html:target/cucumber-reports/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestRunnerCkyc extends AbstractTestNGCucumberTests {

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestRunnerCkyc(deprecated→TestCKYCAllRunner)",
				"Login → Dashboard → Masters");
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestRunnerCkyc");
	}
}
