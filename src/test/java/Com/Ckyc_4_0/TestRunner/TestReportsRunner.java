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
 * Reports module only: signs in (Background step), then compares the Weekly dashboard reports with the Reports module for NEW / UPDATE x ALL / Individual / Legal entity and stores every report with its dynamic headers. Result: Report Output/Reports Compare/&lt;date&gt;/Reports_Compare_&lt;time&gt;.xlsx.
 */
@CucumberOptions(
		features = { "src/test/resources/Features/8_Reports" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-reports.json",
				"html:target/cucumber-reports/reports/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@Reports",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestReportsRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestReportsRunner", "Reports compare", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestReportsRunner");
	}
}
