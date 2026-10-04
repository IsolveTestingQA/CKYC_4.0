/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.masters.hierarchy;

import Com.Ckyc_4_0.functionality.masters.hierarchy.HierarchyMasterFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class HierarchyMasterStepDefinitions {

	private final HierarchyMasterFunctionality hierarchy = new HierarchyMasterFunctionality();

	@When("user opens Hierarchy Master from Masters menu")
	public void userOpensHierarchyMasterFromMastersMenu() {
		Hooks.runStep("Open Masters → Hierarchy Master", "Hierarchy Master page opens",
				hierarchy::openHierarchyMasterFromMenu, true, true);
	}

	@Then("Hierarchy Master page should be displayed")
	public void hierarchyMasterPageShouldBeDisplayed() {
		Hooks.runStep("Verify Hierarchy Master displayed", "Heading and tabs visible",
				hierarchy::verifyHierarchyMasterPageLoaded, true, false);
	}

	@And("user verifies Hierarchy Master summary chips are visible")
	public void userVerifiesHierarchyMasterSummaryChipsAreVisible() {
		Hooks.runStep("Verify Hierarchy summary chips", "FI/Region/CPC/Branch chips readable",
				hierarchy::verifySummaryChips, false);
	}

	@And("user runs FI Master tab validation and safe CRUD")
	public void userRunsFiMasterTabValidationAndSafeCrud() {
		Hooks.runStep("FI Master tab — validation and safe CRUD",
				"Mandatory/duplicate/create/edit/toggle/search on TEST AUTO FI only",
				hierarchy::runFiMasterTabFlow, true, false);
	}

	@And("user runs CPC Master tab validation and safe CRUD")
	public void userRunsCpcMasterTabValidationAndSafeCrud() {
		Hooks.runStep("CPC Master tab — validation and safe CRUD",
				"Mandatory/duplicate/create/edit/toggle/search on TEST AUTO CPC only",
				hierarchy::runCpcMasterTabFlow, true, false);
	}

	@And("user runs Region tab validation and safe CRUD")
	public void userRunsRegionTabValidationAndSafeCrud() {
		Hooks.runStep("Region tab — validation and safe CRUD",
				"Mandatory/duplicate/create/edit/toggle/search on TEST AUTO Region only",
				hierarchy::runRegionTabFlow, true, false);
	}

	@And("user runs Branch tab validation and safe CRUD")
	public void userRunsBranchTabValidationAndSafeCrud() {
		Hooks.runStep("Branch tab — validation and safe CRUD",
				"Mandatory/duplicate/create/edit/toggle/search on TEST AUTO Branch only",
				hierarchy::runBranchTabFlow, true, false);
	}
}
