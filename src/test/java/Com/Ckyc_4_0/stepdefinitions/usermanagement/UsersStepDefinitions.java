/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.usermanagement;

import Com.Ckyc_4_0.functionality.usermanagement.UsersFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class UsersStepDefinitions {
	private final UsersFunctionality users = new UsersFunctionality();

	@When("user opens User Management Users")
	public void openUsers() {
		Hooks.runStep("Open User Management → Users", "Users Maker page opens", users::openUsers, true, true);
	}

	@Then("Users Maker controls should be displayed")
	public void verifyControls() {
		Hooks.runStep("Verify Users Maker controls", "Maker, Checker and Add User visible",
				users::verifyMakerControls, true, false);
	}

	@And("user validates mandatory Add User fields")
	public void mandatoryFields() {
		Hooks.runStep("Validate Add User mandatory fields", "Blank save shows required messages",
				users::validateEmptyAddUser, true, false);
	}

	@And("user validates Add User field length and datatype negatives")
	public void userValidatesFieldRules() {
		Hooks.runStep("Validate Add User field-level negatives",
				"Overlength and invalid datatype values are blocked or trimmed",
				users::validateUserFieldRules, true, false);
	}

	@And("user verifies an existing login name is rejected")
	public void duplicateLogin() {
		Hooks.runStep("Reject duplicate existing login", "Existing login auto cannot be submitted",
				users::verifyDuplicateLoginRejected, true, false);
	}

	@And("user submits a unique TEST AUTO user request")
	public void submitRequest() {
		Hooks.runStep("Create unique TEST AUTO user request", "Request is submitted to Checker",
				users::createUniqueUserRequest, true, false);
	}

	@And("maker attempts to approve the same TEST AUTO user request")
	public void selfApprove() {
		Hooks.runStep("Reject maker self-approval", "Maker cannot approve own request",
				users::verifyMakerCannotApproveOwnRequest, true, false);
	}

	@And("same-role admin Checker is blocked from approving the TEST AUTO user request")
	public void sameRoleBlocked() {
		Hooks.runStep("Reject same-role Checker approval",
				"Admin-role Checker cannot approve an Admin-role Maker request",
				users::sameRoleCheckerCannotApprove, true, false);
	}

	@And("independent Checker shyam approves the TEST AUTO user request")
	public void independentApproval() {
		Hooks.runStep("Independent Checker shyam approves TEST AUTO user",
				"Different-role Checker approves successfully",
				users::independentCheckerApprovesCreatedRequest, true, false);
	}

	@And("user logs in as the approved TEST AUTO user to verify access")
	public void loginAsNewUser() {
		Hooks.runStep("Login as approved TEST AUTO user", "New user can login after approval",
				users::loginAsApprovedTestAutoUser, true, false);
	}

	@And("user searches the TEST AUTO user")
	public void searchUser() {
		Hooks.runStep("Search TEST AUTO user", "Created login is found in Users grid",
				users::searchCreatedUser, true, false);
	}

	@And("user edits only the TEST AUTO user")
	public void editUser() {
		Hooks.runStep("Edit TEST AUTO user only", "Edit is submitted for Checker on created user only",
				users::editCreatedUser, true, false);
	}

	@And("independent Checker shyam approves the TEST AUTO edit request")
	public void independentApprovesEdit() {
		Hooks.runStep("Independent Checker shyam approves TEST AUTO edit",
				"Different-role Checker approves the submitted edit request",
				users::independentCheckerApprovesEditedRequest, true, false);
	}

	@And("user submits a TEST AUTO edit request for revert validation")
	public void submitRevertEdit() {
		Hooks.runStep("Submit TEST AUTO edit for revert validation",
				"Second edit request is submitted so Checker can revert it",
				users::submitEditRequestForRevert, true, false);
	}

	@And("independent Checker shyam reverts the TEST AUTO edit request")
	public void independentRevertsEdit() {
		Hooks.runStep("Independent Checker shyam reverts TEST AUTO edit",
				"Different-role Checker reverts the submitted edit request",
				users::independentCheckerRevertsEditedRequest, true, false);
	}

	@And("user locks only the TEST AUTO user")
	public void lockUser() {
		Hooks.runStep("Lock TEST AUTO user", "Lock request is submitted to Checker",
				users::lockCreatedUser, true, false);
	}

	@And("branch Checker Nivijay approves the TEST AUTO lock request")
	public void branchApprovesLock() {
		Hooks.runStep("Branch Checker Nivijay approves lock", "Lock is approved by branch Checker",
				users::branchCheckerApprovesLock, true, false);
	}

	@And("user unlocks only the TEST AUTO user")
	public void unlockUser() {
		Hooks.runStep("Unlock TEST AUTO user", "Unlock request is submitted to Checker",
				users::unlockCreatedUser, true, false);
	}

	@And("branch Checker Nivijay approves the TEST AUTO unlock request")
	public void branchApprovesUnlock() {
		Hooks.runStep("Branch Checker Nivijay approves unlock", "Unlock is approved by branch Checker",
				users::branchCheckerApprovesUnlock, true, false);
	}

	@And("user sets Dormant on only the TEST AUTO user")
	public void dormantUser() {
		Hooks.runStep("Set Dormant on TEST AUTO user", "Dormant request is submitted to Checker",
				users::dormantCreatedUser, true, false);
	}

	@And("branch Checker Nivijay approves the TEST AUTO dormant request")
	public void branchApprovesDormant() {
		Hooks.runStep("Branch Checker Nivijay approves dormant", "Dormant is approved by branch Checker",
				users::branchCheckerApprovesDormant, true, false);
	}

	@And("checker verifies the last processed TEST AUTO user request details")
	public void verifyProcessedDetails() {
		Hooks.runStep("Verify processed TEST AUTO user request details",
				"Maker, checker, type, status, and date are visible on the processed request",
				users::verifyLastProcessedRequestDetails, true, false);
	}

	@And("user verifies TEST AUTO user exit flag and login status after dormant")
	public void verifyExitFlag() {
		Hooks.runStep("Verify exit flag and dormant user login",
				"Dormant user cannot login, exit flag is visible",
				users::verifyExitFlagAndLoginAfterDormant, true, false);
	}

	@And("maker session is restored on Users")
	public void restoreMaker() {
		Hooks.runStep("Restore Maker session on Users", "auto is back on /admin/users",
				users::restoreMakerForNextModule, true, false);
	}
}
