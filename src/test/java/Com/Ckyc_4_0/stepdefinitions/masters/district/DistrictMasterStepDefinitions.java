/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.masters.district;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;

import Com.Ckyc_4_0.functionality.masters.district.DistrictMasterFunctionality;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class DistrictMasterStepDefinitions {

	private final DistrictMasterFunctionality districtMaster = new DistrictMasterFunctionality();

	@When("user opens District Master from Masters menu")
	public void userOpensDistrictMasterFromMastersMenu() {
		Hooks.runStep("Open Masters → District Master", "District Master page opens",
				districtMaster::openDistrictMasterFromMenu, true, true);
	}

	@Then("District Master page should be displayed")
	public void districtMasterPageShouldBeDisplayed() {
		Hooks.runStep("Verify District Master displayed", "Heading and search visible",
				districtMaster::verifyDistrictMasterPageLoaded, true);
	}

	@And("user verifies District Master badges Total equals Active plus Inactive")
	public void userVerifiesDistrictMasterBadges() {
		Hooks.runStep("Verify Total = Active + Inactive", "Badge arithmetic holds",
				districtMaster::verifyBadges, false);
	}

	@And("user ensures a test-safe State exists for District create")
	public void userEnsuresATestSafeStateExistsForDistrictCreate() {
		Hooks.runStep("Ensure TEST State for District", "Created or reused TEST state",
				districtMaster::ensureTestSafeState, false);
	}

	@And("user loads districts into memory map")
	public void userLoadsDistrictsIntoMemoryMap() {
		Hooks.runStep("Load District Master grid into Map", "Rows stored by name",
				districtMaster::loadDistrictsIntoMap, false);
	}

	@And("user verifies District State filter for two random States")
	public void userVerifiesDistrictStateFilterForTwoRandomStates() {
		Hooks.runStep("Verify District State filter with two random States",
				"Every visible District belongs to the selected State",
				districtMaster::verifyTwoRandomStateFilters, true, true);
	}

	@And("user randomly searches two districts from the map")
	public void userRandomlySearchesTwoDistrictsFromTheMap() {
		Hooks.runStep("Search two random existing Districts",
				"Both selected Districts appear in the grid",
				districtMaster::randomSearchFromMapWithRetry, false);
	}

	@And("user validates District Name field rules with test data")
	public void userValidatesDistrictNameFieldRulesWithTestData() {
		Hooks.runStep("Validate District Name length, datatype, double-space",
				"Invalid rejected; max 50; details in Excel",
				districtMaster::runFieldValidations, true);
	}

	@And("user verifies an existing District cannot be created again")
	public void userVerifiesExistingDistrictCannotBeCreatedAgain() {
		Hooks.runStep("Reject duplicate existing District",
				"Already-exists/duplicate toaster is shown",
				districtMaster::verifyExistingDistrictDuplicateRejected, true, true);
	}

	@And("user creates a unique TEST district record")
	public void userCreatesAUniqueTestDistrictRecord() {
		Hooks.runStep("Create unique TEST district under test-safe State", "Only the new district is created",
				districtMaster::createUniqueTestDistrict, true, true);
	}

	@And("user edits only the newly created district")
	public void userEditsOnlyTheNewlyCreatedDistrict() {
		Hooks.runStep("Edit created TEST district only", "Existing rows untouched",
				districtMaster::editOnlyCreatedTestDistrict, false);
	}

	@And("user toggles only the newly created district")
	public void userTogglesOnlyTheNewlyCreatedDistrict() {
		Hooks.runStep("Toggle created TEST district only", "Existing rows untouched",
				districtMaster::toggleOnlyCreatedTestDistrict, false);
	}

	@And("user searches the newly created district")
	public void userSearchesTheNewlyCreatedDistrict() {
		Hooks.runStep("Search created TEST district", "Created row found",
				districtMaster::searchCreatedTestDistrict, false);
	}

	@And("user deletes the newly created district if delete is available otherwise deactivates it")
	public void userCleansUpCreatedDistrict() {
		Hooks.runStep("Cleanup created TEST district", "Delete if present else deactivate created only",
				districtMaster::cleanupCreatedTestDistrict, false);
	}
}
