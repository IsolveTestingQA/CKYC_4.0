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
 * Full suite — Login first, then Dashboard, Masters, and Search And Download > Search.
 * Folder prefixes (1_/2_/3_/4_) drive Cucumber's lexical execution order.
 * Default entry in {@code testng.xml}.
 */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/2_Dashboard",
				"src/test/resources/Features/3_Masters",
				"src/test/resources/Features/4_SearchAndDownload",
				"src/test/resources/Features/5_UserManagement"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber.json",
				"html:target/cucumber-reports/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Positive and not @Deferred",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestCKYCAllRunner extends AbstractTestNGCucumberTests {

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestCKYCAllRunner",
				"Login → Dashboard → Masters → Search And Download → User Management (Users, Roles)");
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestCKYCAllRunner");
	}
}
