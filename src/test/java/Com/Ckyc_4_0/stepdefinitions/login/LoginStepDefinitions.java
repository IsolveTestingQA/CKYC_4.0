/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.login;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;

import Com.Ckyc_4_0.functionality.login.LoginFunctionality;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.BaseClass;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LoginStepDefinitions {

	private final LoginFunctionality loginFunctionality = new LoginFunctionality();
	private boolean authenticatedSession;

	@Given("user is on CKYC login page")
	public void userIsOnCkycLoginPage() {
		authenticatedSession = isAlreadyAuthenticated();
		if (authenticatedSession) {
			Hooks.runStep("Reuse authenticated CKYC session",
					"Current dashboard/master session remains available", () -> {
					}, false, false);
			return;
		}
		Hooks.runStep("Navigate to CKYC login page", "Username field visible",
				loginFunctionality::verifyLoginPageIsDisplayed, true, true);
	}

	@When("user selects runtime mode from config")
	public void userSelectsRuntimeModeFromConfig() {
		String runtimeMode = ConfigReader.getRuntimeMode();
		if (authenticatedSession) {
			logAuthenticatedSkip("Select runtime mode", runtimeMode);
			return;
		}
		Hooks.runStep("Select runtime mode", runtimeMode, () -> loginFunctionality.selectRuntimeMode(runtimeMode));
	}

	@When("user selects {string} runtime mode from config")
	public void userSelectsRuntimeModeFromConfig(String mode) {
		String runtimeMode = mode == null || mode.isBlank() ? ConfigReader.getRuntimeMode() : mode;
		if (authenticatedSession) {
			logAuthenticatedSkip("Select runtime mode", runtimeMode);
			return;
		}
		Hooks.runStep("Select runtime mode", runtimeMode, () -> loginFunctionality.selectRuntimeMode(runtimeMode));
	}

	@When("user enters valid credentials from config")
	public void userEntersValidCredentialsFromConfig() {
		if (authenticatedSession) {
			logAuthenticatedSkip("Enter credentials", "Already authenticated");
			return;
		}
		Hooks.runStep("Enter credentials", "Valid username/password from config",
				() -> loginFunctionality.enterCredentialsFromConfig());
	}

	@When("user clicks Sign In button")
	public void userClicksSignInButton() {
		if (authenticatedSession) {
			logAuthenticatedSkip("Click Sign In", "Already authenticated");
			return;
		}
		Hooks.runStep("Click Sign In", "Submit login form", () -> loginFunctionality.clickSignIn());
	}

	@Then("user should be logged in successfully")
	public void userShouldBeLoggedInSuccessfully() {
		if (authenticatedSession) {
			Hooks.runStep("Verify existing authenticated session",
					"URL remains on dashboard or CKYC module", () -> {
					}, true, false);
			return;
		}
		Hooks.runStep("Verify login success - handle Active session / password prompts if shown",
				"URL contains " + ConfigReader.getDashboardUrlPath(), () -> {
					loginFunctionality.verifyUserLoggedInSuccessfully();
				}, true, true);
	}

	private void logAuthenticatedSkip(String step, String expected) {
		Hooks.runStep(step + " (not required)", expected, () -> {
		}, false, false);
	}

	private boolean isAlreadyAuthenticated() {
		try {
			String url = BaseClass.getDriver() == null ? "" : BaseClass.getDriver().getCurrentUrl();
			return url != null
					&& !url.toLowerCase().contains("/login")
					&& (url.contains(ConfigReader.getDashboardUrlPath()) || url.contains("/ckyc/")
							|| url.contains("/admin/"));
		} catch (Exception ignored) {
			return false;
		}
	}
}
