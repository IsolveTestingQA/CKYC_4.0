/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.masters.state;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;

import Com.Ckyc_4_0.functionality.masters.state.StateMasterFunctionality;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class StateMasterStepDefinitions {

	private final StateMasterFunctionality stateMaster = new StateMasterFunctionality();

	@When("user opens State Master from Masters menu")
	public void userOpensStateMasterFromMastersMenu() {
		Hooks.runStep("Open Masters → State Master", "State Master page opens",
				stateMaster::openStateMasterFromMenu, true, true);
	}

	@Then("State Master page should be displayed")
	public void stateMasterPageShouldBeDisplayed() {
		Hooks.runStep("Verify State Master displayed", "Heading and search visible",
				stateMaster::verifyStateMasterPageLoaded, true, true);
	}

	@And("user verifies State Master badges Total equals Active plus Inactive")
	public void userVerifiesStateMasterBadges() {
		Hooks.runStep("Verify Total = Active + Inactive", "Badge arithmetic holds",
				stateMaster::verifyBadges, false);
	}

	@And("user loads all states into memory map")
	public void userLoadsAllStatesIntoMemoryMap() {
		Hooks.runStep("Load State Master grid into Map", "All pages stored by code",
				stateMaster::loadAllStatesIntoMap, false, true);
	}

	@And("user randomly searches a state from the map with retry")
	public void userRandomlySearchesAStateFromTheMapWithRetry() {
		Hooks.runStep("Random search from map with retry (after CRUD)", "Expected row appears or alternate tried",
				stateMaster::randomSearchFromMapWithRetry, false, true);
	}

	@And("user clears Create State form and verifies fields empty")
	public void userClearsCreateStateFormAndVerifiesFieldsEmpty() {
		Hooks.runStep("Clear Create State form", "Code and Name empty after Clear",
				stateMaster::clearCreateFormAndVerifyEmpty, false, true);
	}

	@And("user validates State Code and State Name field rules with test data")
	public void userValidatesStateCodeAndStateNameFieldRulesWithTestData() {
		Hooks.runStep("Validate State Code/Name length, datatype, double-space",
				"Invalid rejected; max 50 name; code 2 capitals; details in Excel Actual",
				stateMaster::runCreateFieldValidations, true, true);
	}

	@And("user verifies an existing State cannot be created again")
	public void userVerifiesExistingStateCannotBeCreatedAgain() {
		Hooks.runStep("Reject duplicate existing State",
				"Already-exists/duplicate toaster is shown",
				stateMaster::verifyExistingStateDuplicateRejected, true, true);
	}

	@And("user creates a unique TEST state record")
	public void userCreatesAUniqueTestStateRecord() {
		Hooks.runStep("Create unique TEST state", "Only the new TEST state is created",
				stateMaster::createUniqueTestState, true, true);
	}

	@And("user creates a second unique TEST state record if possible")
	public void userCreatesASecondUniqueTestStateRecordIfPossible() {
		Hooks.runStep("Create second TEST state if possible", "Optional second TEST row; existing untouched",
				stateMaster::createSecondUniqueTestStateIfNeeded, false);
	}

	@And("user edits only the newly created state")
	public void userEditsOnlyTheNewlyCreatedState() {
		Hooks.runStep("Edit created TEST state only", "Existing rows untouched",
				stateMaster::editOnlyCreatedTestState, false);
	}

	@And("user toggles only the newly created state")
	public void userTogglesOnlyTheNewlyCreatedState() {
		Hooks.runStep("Toggle created TEST state only", "Existing rows untouched",
				stateMaster::toggleOnlyCreatedTestState, false);
	}

	@And("user searches the newly created state")
	public void userSearchesTheNewlyCreatedState() {
		Hooks.runStep("Search created TEST state", "Created row found",
				stateMaster::searchCreatedTestState, false);
	}

	@And("user deletes the newly created state if delete is available otherwise deactivates it")
	public void userDeletesOrDeactivatesNewlyCreatedState() {
		Hooks.runStep("Cleanup created TEST state", "Delete if present else deactivate created only",
				stateMaster::cleanupCreatedTestState, false);
	}
}
