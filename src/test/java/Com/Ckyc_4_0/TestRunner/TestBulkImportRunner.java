/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * Bulk Data Import -> Monitor Automation -> Dashboard (Data pushed from FI) -> Customer Information.
 * An upload pop-up asks for the CSV; the run uploads it, follows the ids for up to 7 minutes and compares the successful
 * customers with the CSV. It creates data in the live application: it is never part of the "all positive" runners.
 */
@CucumberOptions(
		features = { "src/test/resources/Features/9_BulkImport" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = {
				"pretty",
				"json:target/cucumber/cucumber-bulkimport.json",
				"html:target/cucumber-reports/bulkimport/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
		},
		tags = "@BulkImport",
		monochrome = true,
		dryRun = false,
		publish = false)
public class TestBulkImportRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;

	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestBulkImportRunner", "Bulk Data Import -> Monitor Automation -> Dashboard -> Customer Information", RECORD_SCREEN);
	}

	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestBulkImportRunner");
	}
}
