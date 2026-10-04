/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.concurrentsession;

import Com.Ckyc_4_0.functionality.concurrentsession.ConcurrentSessionFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ConcurrentSessionStepDefinitions {
	private final ConcurrentSessionFunctionality concurrent = new ConcurrentSessionFunctionality();

	@When("user opens a second browser and logs in with the same credentials")
	public void openSecondBrowser() {
		Hooks.runStep("Open second browser with same credentials",
				"Second browser login submitted",
				concurrent::openSecondBrowserAndLogin, true, true);
	}

	@Then("the Active session detected dialog should appear in the second browser")
	public void verifyDialog() {
		Hooks.runStep("Verify Active session detected dialog",
				"Dialog appears in second browser",
				concurrent::verifyActiveSessionDialog, true, false);
	}

	@And("user captures split-screen screenshot of both browsers")
	public void splitScreen() {
		Hooks.runStep("Capture split-screen screenshot",
				"Both browsers captured side-by-side",
				concurrent::captureSplitScreenshot, true, false);
	}

	@And("user clicks Log out other session and continue in the second browser")
	public void clickLogoutOther() {
		Hooks.runStep("Click Log out other session & continue",
				"Dialog handled in second browser",
				concurrent::clickLogoutOtherSessionAndContinue, true, false);
	}

	@Then("the second browser should reach the dashboard")
	public void secondBrowserDashboard() {
		Hooks.runStep("Second browser reaches dashboard",
				"Dashboard loaded after session takeover",
				concurrent::verifySecondBrowserReachesDashboard, true, false);
	}

	@And("the first browser session should be invalidated")
	public void firstBrowserInvalidated() {
		Hooks.runStep("First browser session invalidated",
				"First browser redirects to login after takeover",
				concurrent::verifyFirstBrowserSessionInvalidated, true, false);
	}

	@And("both browser sessions are cleaned up")
	public void cleanup() {
		Hooks.runStep("Cleanup both browser sessions",
				"Second browser closed, primary restored",
				concurrent::cleanupBothSessions, true, false);
	}
}
