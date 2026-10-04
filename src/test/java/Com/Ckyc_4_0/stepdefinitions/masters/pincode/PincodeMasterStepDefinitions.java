/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.masters.pincode;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;

import Com.Ckyc_4_0.functionality.masters.pincode.PincodeMasterFunctionality;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class PincodeMasterStepDefinitions {

	private final PincodeMasterFunctionality pincodeMaster = new PincodeMasterFunctionality();

	@When("user opens Pincode Master from Masters menu")
	public void userOpensPincodeMasterFromMastersMenu() {
		Hooks.runStep("Open Masters → Pincode Master", "Pincode Master page opens",
				pincodeMaster::openPincodeMasterFromMenu, true, true);
	}

	@Then("Pincode Master page should be displayed")
	public void pincodeMasterPageShouldBeDisplayed() {
		Hooks.runStep("Verify Pincode Master displayed", "Heading and search visible",
				pincodeMaster::verifyPincodeMasterPageLoaded, true);
	}

	@And("user verifies Pincode Master badges Total is at least Active")
	public void userVerifiesPincodeMasterBadges() {
		Hooks.runStep("Verify Total Pincodes >= Active", "Badge arithmetic holds",
				pincodeMaster::verifyBadges, false);
	}

	@And("user ensures a test-safe State and District exist for Pincode create")
	public void userEnsuresTestSafeGeography() {
		Hooks.runStep("Ensure TEST State and District for Pincode", "Created or reused TEST geography",
				pincodeMaster::ensureTestSafeStateAndDistrict, false);
	}

	@And("user loads visible pincodes into memory map")
	public void userLoadsVisiblePincodesIntoMemoryMap() {
		Hooks.runStep("Load visible Pincode rows into Map", "Current visible page stored (not all 18k)",
				pincodeMaster::loadVisiblePincodesIntoMap, false);
	}

	@And("user verifies Pincode State filter for two random States")
	public void userVerifiesPincodeStateFilterForTwoRandomStates() {
		Hooks.runStep("Verify Pincode State filter with two random States",
				"Every visible Pincode belongs to the selected State",
				pincodeMaster::verifyTwoRandomStateFilters, true, true);
	}

	@And("user randomly searches two pincodes from the map")
	public void userRandomlySearchesTwoPincodesFromTheMap() {
		Hooks.runStep("Search two random existing Pincodes",
				"Both selected Pincodes appear in the grid",
				pincodeMaster::randomSearchFromMapWithRetry, false);
	}

	@And("user validates Pincode field rules with test data")
	public void userValidatesPincodeFieldRulesWithTestData() {
		Hooks.runStep("Validate pincode numeric 6-digit and State→District dependency",
				"Invalid rejected; district disabled until state selected",
				pincodeMaster::runFieldValidations, true);
	}

	@And("user verifies an existing Pincode cannot be created again")
	public void userVerifiesExistingPincodeCannotBeCreatedAgain() {
		Hooks.runStep("Reject duplicate existing Pincode",
				"Already-exists/duplicate toaster is shown",
				pincodeMaster::verifyExistingPincodeDuplicateRejected, true, true);
	}

	@And("user creates a unique TEST pincode record")
	public void userCreatesAUniqueTestPincodeRecord() {
		Hooks.runStep("Create unique TEST pincode under test-safe District", "Only the new pincode is created",
				pincodeMaster::createUniqueTestPincode, true, true);
	}

	@And("user edits only the newly created pincode")
	public void userEditsOnlyTheNewlyCreatedPincode() {
		Hooks.runStep("Edit created TEST pincode only", "Existing rows untouched",
				pincodeMaster::editOnlyCreatedTestPincode, false);
	}

	@And("user toggles only the newly created pincode")
	public void userTogglesOnlyTheNewlyCreatedPincode() {
		Hooks.runStep("Toggle created TEST pincode only", "Existing rows untouched",
				pincodeMaster::toggleOnlyCreatedTestPincode, false);
	}

	@And("user searches the newly created pincode")
	public void userSearchesTheNewlyCreatedPincode() {
		Hooks.runStep("Search created TEST pincode", "Created row found",
				pincodeMaster::searchCreatedTestPincode, false);
	}

	@And("user deletes the newly created pincode if delete is available otherwise deactivates it")
	public void userCleansUpCreatedPincode() {
		Hooks.runStep("Cleanup created TEST pincode", "Delete if present else deactivate created only",
				pincodeMaster::cleanupCreatedTestPincode, false);
	}
}
