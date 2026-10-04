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
 * Dedicated User Management runner.
 * Roles Positive runs first so the approved TEST AUTO role can be assigned on user create.
 * Comment out any {@code features} line below to skip that feature file.
 * Note: @UserManagement scenarios are never auto-retried by {@link RetryAnalyzer} (Maker-Checker
 * chains are not safe to blindly redo) — the listener is still attached for consistency.
 */
@Listeners(RetryAnnotationTransformer.class)
@CucumberOptions(
		features = {
				"src/test/resources/Features/1_Login",
				"src/test/resources/Features/5_UserManagement/2_Roles/8_01_RolesPositive.feature",
				"src/test/resources/Features/5_UserManagement/1_Users/7_01_UsersPositive.feature",
				"src/test/resources/Features/5_UserManagement/2_Roles/8_02_RolesNegative.feature",
				"src/test/resources/Features/5_UserManagement/1_Users/7_02_UsersNegative.feature"
		},
		glue = { "Com.Ckyc_4_0.stepdefinitions" },
		plugin = { "pretty", "json:target/cucumber/cucumber-user-management.json",
				"html:target/cucumber-reports/user-management/index.html",
				"io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" },
		tags = "@UserManagement",
		monochrome = true, dryRun = false, publish = false)
public class TestUserManagementRunner extends AbstractTestNGCucumberTests {
	@BeforeSuite(alwaysRun = true)
	public void setUpSuite() {
		RunnerSuiteSupport.beforeSuite("TestUserManagementRunner",
				"Login → Roles Positive → Users Positive → Roles Negative → Users Negative");
	}
	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		RunnerSuiteSupport.afterSuite("TestUserManagementRunner");
	}
}
