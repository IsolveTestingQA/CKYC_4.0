/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.dvs;

import Com.Ckyc_4_0.functionality.dvs.DvsNavigation;
import Com.Ckyc_4_0.functionality.dvs.DvsRunEngine;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.nio.file.Files;

public class DvsSteps {

	private DvsRunEngine.Outcome outcome;

	@Given("DVS test data and locators are loaded")
	public void dvsTestDataAndLocatorsAreLoaded() {
		Hooks.runStep("Load DVS test data and locators", "Workbook found, locators loaded", () -> {
			if (!Files.exists(DvsConfig.testDataPath())) {
				throw new AssertionError("DVS test data not found: " + DvsConfig.testDataPath().toAbsolutePath());
			}
			if (DvsLocators.get().allIds().isEmpty()) {
				throw new AssertionError("No DVS locators loaded");
			}
		});
	}

	@Given("DVS session is ready after manual sign-in")
	public void dvsSessionIsReadyAfterManualSignIn() {
		Hooks.runStep("Wait for manual sign-in and DVS shell", "DVS shell visible",
				DvsNavigation::ensureSession, true, true);
	}

	@When("DVS run is executed in mode {string}")
	public void dvsRunIsExecutedInMode(String mode) {
		Hooks.runStep("Run DVS 2.0 automation: " + mode, "Run completes and result workbook is written", () -> {
			DvsRunEngine.Mode m = DvsRunEngine.Mode.valueOf(mode.trim().toUpperCase());
			outcome = new DvsRunEngine().run(m, DvsRunEngine.filterFromConfig());
		});
	}

	@Then("DVS result file is published with no failures")
	public void dvsResultFileIsPublishedWithNoFailures() {
		Hooks.runStep("Check DVS result summary", "No FAIL rows and the run was not stopped", () -> {
			if (outcome == null) {
				throw new AssertionError("DVS run did not produce an outcome");
			}
			String summary = "Result file " + outcome.resultFile() + " | total=" + outcome.total() + " pass=" + outcome.pass()
					+ " fail=" + outcome.fail() + " blocked=" + outcome.blocked() + " captured=" + outcome.captured()
					+ " notRun=" + outcome.notRun();
			if (outcome.fail() > 0 || outcome.blocked() > 0) {
				throw new AssertionError(summary);
			}
		});
	}
}
