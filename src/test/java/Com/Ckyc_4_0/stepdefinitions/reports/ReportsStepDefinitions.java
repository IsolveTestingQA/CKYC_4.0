/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.reports;

import Com.Ckyc_4_0.functionality.reports.ReportsCompareFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ReportsStepDefinitions {

	private ReportsCompareFunctionality.Outcome outcome;

	@When("weekly dashboard reports and Reports module data are collected and compared")
	public void weeklyDashboardReportsAndReportsModuleDataAreCollectedAndCompared() {
		Hooks.runStep("Collect Weekly dashboard reports and Reports module data, compare and store",
				"Every dashboard sub-card report matches its Reports-module report; all reports stored with their headers",
				() -> outcome = new ReportsCompareFunctionality().run());
	}

	@Then("reports comparison workbook is published with no mismatches")
	public void reportsComparisonWorkbookIsPublishedWithNoMismatches() {
		Hooks.runStep("Check reports comparison summary", "No FAIL comparisons", () -> {
			if (outcome == null) {
				throw new AssertionError("Reports comparison did not run");
			}
			String summary = "Workbook " + outcome.file() + " | PASS=" + outcome.pass() + " FAIL=" + outcome.fail() + " BUG=" + outcome.bug()
					+ " NOT EXECUTED=" + outcome.notExecuted() + " STORED ONLY=" + outcome.storedOnly()
					+ " | tables stored=" + outcome.tables()
					+ (outcome.abortReason().isEmpty() ? "" : " | STOPPED: " + outcome.abortReason());
			boolean ok = outcome.fail() == 0 && outcome.bug() == 0 && outcome.abortReason().isEmpty();
			Hooks.logStep("Reports comparison result", "No FAIL / BUG comparisons, run completed", summary, ok);
			if (!ok) {
				throw new AssertionError(summary);
			}
		});
	}
}
