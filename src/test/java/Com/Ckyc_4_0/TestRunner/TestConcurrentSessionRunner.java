/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Dedicated runner for Concurrent Session (multi-browser login) test.
 * Note: @ConcurrentSession scenarios are never auto-retried by {@link RetryAnalyzer} (a second
 * live browser session is not safe to blindly redo) — the listener is attached for consistency.
 */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = { "src/test/resources/Features/1_Login", "src/test/resources/Features/6_ConcurrentSession" },
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = { "pretty", "json:target/cucumber/cucumber-concurrent-session.json",
				"html:target/cucumber-reports/concurrent-session/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" },
		tags = "@ConcurrentSession",
		monochrome = true, dryRun = false, publish = false)
public class TestConcurrentSessionRunner extends CkycCucumberTests {

	/** true = record the browser window of THIS runner from the moment it opens (default off; the Test Control Panel toggle overrides). */
	private static final boolean RECORD_SCREEN = false;
	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestConcurrentSessionRunner", "Login → Concurrent Session Detection", RECORD_SCREEN);
	}
	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestConcurrentSessionRunner");
	}
}
