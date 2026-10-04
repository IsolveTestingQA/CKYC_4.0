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

/** Login first, then execute only Search And Download > Search. */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/4_SearchAndDownload/1_Search"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-search.json",
				"html:target/cucumber-reports/search/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestSearchRunner extends AbstractTestNGCucumberTests {

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestSearchRunner", "Login → Search And Download → Search");
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestSearchRunner");
	}
}
