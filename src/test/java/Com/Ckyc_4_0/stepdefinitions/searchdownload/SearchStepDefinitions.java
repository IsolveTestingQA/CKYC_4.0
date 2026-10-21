/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.searchdownload;

import Com.Ckyc_4_0.functionality.searchdownload.SearchFunctionality;
import Com.Ckyc_4_0.functionality.searchdownload.SearchInteractiveFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class SearchStepDefinitions {

	private final SearchFunctionality search = new SearchFunctionality();
	private final SearchInteractiveFunctionality interactive = new SearchInteractiveFunctionality();
	private SearchInteractiveFunctionality.Summary interactiveSummary;

	@When("user opens Search from Search And Download menu")
	public void userOpensSearchFromSearchAndDownloadMenu() {
		Hooks.runStep("Open Search And Download → Search", "Search page opens",
				search::openSearchFromMenu, true, true);
	}

	@Then("Search page should be displayed")
	public void searchPageShouldBeDisplayed() {
		Hooks.runStep("Verify Search page controls", "Search controls visible",
				search::verifySearchPageLoaded, true, true);
	}

	@And("user verifies Search requires a document type before input")
	public void userVerifiesSearchRequiresDocumentTypeBeforeInput() {
		Hooks.runStep("Verify Document Type prerequisite", "Number and Search initially disabled",
				search::verifyPrerequisites, false);
	}

	@And("user validates PAN Search input rules")
	public void userValidatesPanSearchInputRules() {
		Hooks.runStep("Validate PAN Search negatives", "Invalid PAN values keep Search disabled",
				search::runPanNegativeValidations, true, true);
	}

	@And("user searches the configured PAN, validates all CKYC fields, and saves download identifiers")
	public void userSearchesConfiguredPanValidatesFieldsAndSavesIdentifiers() {
		Hooks.runStep("Search configured PAN",
				"All result fields populated, CKYC Number masked, Reference ID saved for Download",
				search::searchPanAndRequireRecord, true, true);
	}

	@And("user validates mobile Search input rules")
	public void userValidatesMobileSearchInputRules() {
		Hooks.runStep("Validate mobile Search negatives", "Invalid mobile values keep Search disabled",
				search::runMobileNegativeValidations, true, true);
	}

	@And("user searches the configured mobile and records the outcome")
	public void userSearchesConfiguredMobileAndRecordsOutcome() {
		Hooks.runStep("Search configured mobile", "Record or explicit no-record/error response",
				search::searchMobileAndRecordOutcome, false);
	}

	@And("user clears Search and verifies the reset state")
	public void userClearsSearchAndVerifiesResetState() {
		Hooks.runStep("Clear Search", "Search controls and previous result reset",
				search::clearAndVerifyReset, false);
	}

	@And("user runs the interactive Search session")
	public void userRunsTheInteractiveSearchSession() {
		Hooks.runStep("Interactive Search session",
				"Operator chooses the Document Type and number in the popups; every search is recorded; Close ends the session",
				() -> interactiveSummary = interactive.runSession());
	}

	@Then("the Search results report is published")
	public void theSearchResultsReportIsPublished() {
		Hooks.runStep("Search results report", "Excel report written; no search with a FAIL verdict", () -> {
			if (interactiveSummary == null) {
				throw new AssertionError("The interactive Search session did not run");
			}
			String text = "Report " + interactiveSummary.file() + " | searches=" + interactiveSummary.searches()
					+ " PASS=" + interactiveSummary.pass() + " FAIL=" + interactiveSummary.fail()
					+ " closedByOperator=" + interactiveSummary.closedByOperator();
			Hooks.logStep("Search results report", "No FAIL verdicts", text, interactiveSummary.fail() == 0);
			if (interactiveSummary.fail() > 0) {
				throw new AssertionError(text);
			}
		});
	}
}
