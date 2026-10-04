/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.usermanagement;

import Com.Ckyc_4_0.functionality.usermanagement.RolesFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class RolesStepDefinitions {
	private final RolesFunctionality roles = new RolesFunctionality();

	@When("user opens User Management Roles")
	public void openRoles() {
		Hooks.runStep("Open User Management → Roles", "Roles Maker page opens", roles::openRoles, true, true);
	}

	@Then("Roles Maker controls should be displayed")
	public void verifyRoles() {
		Hooks.runStep("Verify Roles Maker controls", "Role Management and Add Role visible",
				roles::verifyRolesPage, true, false);
	}

	@And("user validates mandatory Add Role fields")
	public void mandatory() {
		Hooks.runStep("Validate Add Role mandatory fields", "Blank save shows required messages",
				roles::validateAddRoleRules, true, false);
	}

	@And("user submits a unique TEST AUTO role request")
	public void create() {
		Hooks.runStep("Create unique TEST AUTO role request", "Role is submitted to Checker",
				roles::createUniqueRoleRequest, true, false);
	}

	@And("maker attempts to approve the same TEST AUTO role request")
	public void selfApprove() {
		Hooks.runStep("Reject maker self-approval of role", "Maker cannot approve own role request",
				roles::verifyMakerCannotApproveOwnRole, true, false);
	}

	@And("same-role admin Checker is blocked from approving the TEST AUTO role request")
	public void sameRoleBlocked() {
		Hooks.runStep("Reject same-role Checker approval of role",
				"Admin-role Checker cannot approve an Admin-role Maker role request",
				roles::sameRoleCheckerCannotApproveRole, true, false);
	}

	@And("independent Checker shyam approves the TEST AUTO role request")
	public void independentApproval() {
		Hooks.runStep("Independent Checker shyam approves TEST AUTO role",
				"Different-role Checker approves successfully",
				roles::independentCheckerApprovesRole, true, false);
	}

	@And("permissions are evaluated only for a TEST AUTO role")
	public void permissions() {
		Hooks.runStep("Evaluate parent child permission rule on TEST AUTO role",
				"Never modify Super Admin; parent checks with child",
				roles::inspectPermissionParentChildRule, true, false);
	}

	@And("maker session is restored on Roles")
	public void restoreMaker() {
		Hooks.runStep("Restore Maker session on Roles", "auto is back on /admin/roles",
				roles::restoreMakerForNextModule, true, false);
	}
}
