/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.dvs;

import Com.Ckyc_4_0.functionality.dvs.DvsNavigation;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.functionality.dvs.DvsRunEngine;
import Com.Ckyc_4_0.functionality.dvs.DvsStuckPrompt;
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

	@Given("user opens DVS 2.0 from the side navigation")
	public void userOpensDvsFromTheSideNavigation() {
		Hooks.runStep("Open DVS 2.0 from the side navigation (after login)", "DVS shell visible", () -> {
			for (int attempt = 1; ; attempt++) {
				try {
					DvsNavigation.ensureSession();
					return;
				} catch (RuntimeException e) {
					DvsStuckPrompt.Choice c = attempt >= 3 ? DvsStuckPrompt.Choice.SKIP
							: DvsStuckPrompt.ask("DVS 2.0 could not be opened from the side navigation", e.getMessage());
					if (c == DvsStuckPrompt.Choice.RETRY) {
						continue;
					}
					if (c == DvsStuckPrompt.Choice.STOP) {
						SoftAssertManager.markAbortRun("Stopped from the DVS popup: " + e.getMessage());
					} else {
						SoftAssertManager.markSkipScenario("DVS 2.0 not opened (skipped): " + e.getMessage());
					}
					return;
				}
			}
		}, true, true);
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
					+ " notRun=" + outcome.notRun() + " bugsFound=" + outcome.bugs();
			if (outcome.fail() > 0 || outcome.blocked() > 0) {
				throw new AssertionError(summary);
			}
		});
	}
}
