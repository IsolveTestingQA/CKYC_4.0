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
 * Module runner: Login → Search And Download.
 * Search executes now; Download has a separate deferred feature file.
 */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/4_SearchAndDownload"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-search-download.json",
				"html:target/cucumber-reports/search-download/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive and not @Deferred",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestSearchAndDownloadRunner extends AbstractTestNGCucumberTests {

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestSearchAndDownloadRunner",
				"Login → Search And Download (Search now; Download deferred)");
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestSearchAndDownloadRunner");
	}
}
