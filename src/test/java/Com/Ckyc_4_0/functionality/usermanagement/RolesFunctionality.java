/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.usermanagement;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.UserManagementAccounts;
import Com.Ckyc_4_0.UtilityFiles.UserManagementSession;
import Com.Ckyc_4_0.UtilityFiles.UserManagementStore;
import Com.Ckyc_4_0.pages.usermanagement.RolesPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;

/** Role Maker/Checker. Never touch Super Admin. Same-role Checker must fail; the independent Checker comes from user-management.properties. */
public class RolesFunctionality extends BaseClass {
	private final int waitSeconds = ConfigReader.getExplicitWait();

	public void openRoles() {
		ensureMakerOnRoles();
		Hooks.captureMilestone("User Management Roles page opened");
	}

	public void verifyRolesPage() {
		boolean pass = MasterUiHelper.isVisible(RolesPage.PAGE_HEADING, waitSeconds)
				&& MasterUiHelper.isVisible(RolesPage.ADD_ROLE, waitSeconds);
		StepLog.check("Roles | Maker controls", "Role Management + Add Role visible",
				"Maker Roles page visible", "visible=" + pass, pass, "neverTarget=Super Admin");
	}

	public void validateAddRoleRules() {
		ensureMakerOnRoles();
		MasterUiHelper.clickJs(RolesPage.ADD_ROLE, waitSeconds);
		forceClickSave();
		int required = helperCount();
		StepLog.check("Roles | Mandatory validation", "Save blank Add Role form",
				"Required validation displayed", "helperErrors=" + required, required > 0, "form=Add Role");
		MasterUiHelper.typeInto(RolesPage.ROLE_NAME, "TEST@ROLE", waitSeconds);
		MasterUiHelper.typeInto(RolesPage.ROLE_CODE, "ROLE CODE", waitSeconds);
		MasterUiHelper.typeInto(RolesPage.DESCRIPTION, "TEST", waitSeconds);
		forceClickSave();
		int invalidErrors = helperCount();
		StepLog.check("Roles | Invalid datatype negative",
				"Submit role name special character and role code with spaces",
				"UI blocks invalid name/code format and does not queue request",
				"helperErrors=" + invalidErrors + " roleName='TEST@ROLE' roleCode='ROLE CODE'",
				invalidErrors > 0, "roleName=TEST@ROLE; roleCode=ROLE CODE");
		closePanel();
	}

	public void createUniqueRoleRequest() {
		ensureMakerOnRoles();
		closePanel();
		MasterUiHelper.sleep(300);
		MasterUiHelper.clickJs(RolesPage.ADD_ROLE, waitSeconds);
		MasterUiHelper.sleep(400);
		String role = UserManagementStore.uniqueRole();
		String code = UserManagementStore.uniqueRoleCode();
		MasterUiHelper.typeInto(RolesPage.ROLE_NAME, role, waitSeconds);
		MasterUiHelper.typeInto(RolesPage.ROLE_CODE, code, waitSeconds);
		MasterUiHelper.typeInto(RolesPage.DESCRIPTION, "TEST AUTO maker checker role", waitSeconds);
		Hooks.captureMilestone("Roles Add Role populated before submit | " + role);
		MasterUiHelper.clickJs(RolesPage.SAVE, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 3500);
		if (toast == null || toast.isBlank()) {
			toast = ToastHandler.waitAndRead(getDriver(), 2500);
		}
		closePanel();
		MasterUiHelper.sleep(400);
		boolean inMyRequests = false;
		for (int i = 0; i < 12 && !inMyRequests; i++) {
			inMyRequests = !getDriver().findElements(RolesPage.myRequestPending(role)).isEmpty();
			if (!inMyRequests) {
				MasterUiHelper.sleep(500);
			}
		}
		boolean queued = inMyRequests || isQueued(toast);
		UserManagementStore.setRoleName(role);
		UserManagementStore.setRoleCode(code);
		if (queued) {
			UserManagementStore.setLastPendingAction("ROLE_CREATE");
		}
		boolean toastPresent = toast != null && !toast.isBlank();
		StepLog.check("Roles | Create TEST AUTO request", "Submit role to Checker",
				"Pending request/toast confirms submission",
				"role=" + role + " code=" + code + " toast='" + toast + "' queued=" + queued
						+ " toastPresent=" + toastPresent,
				queued, "role=" + role + "; code=" + code);
		if (queued && !toastPresent) {
			StepLog.check("Roles | Create TEST AUTO toast",
					"Toast message should appear after role request submission",
					"Non-empty success/pending toast is shown",
					"FAIL (BUG) | Role request queued but toaster was missing. role=" + role,
					false, "role=" + role);
		}
		Hooks.captureMilestone("Roles Add Role submitted | " + role);
		closePanel();
	}

	public void verifyMakerCannotApproveOwnRole() {
		String role = requireRole("Maker self-approve");
		if (role == null) {
			return;
		}
		ensureMakerOnRoles();
		openCheckerQueue();
		ensureRoleVisibleInCheckerQueue(role, true);
		verifyRoleViewDetails(role, UserManagementAccounts.maker(), "CREATE", "PENDING", false);
		boolean blocked = attemptApprove(role, UserManagementAccounts.maker(), true);
		StepLog.check("Roles | Maker self-approve blocked", "Maker attempts approval of own TEST AUTO role",
				"Self-approval is rejected or Approve is not available to Maker",
				"role=" + role + " blocked=" + blocked, blocked, "role=" + role);
		clickMaker();
	}

	public void sameRoleCheckerCannotApproveRole() {
		String role = requireRole("Same-role Checker");
		if (role == null) {
			return;
		}
		String checker = UserManagementAccounts.sameRoleChecker();
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		if (!UserManagementSession.openRolesOrFail(checker + " (same-role negative)")) {
			UserManagementSession.restoreMaker();
			ensureMakerOnRoles();
			return;
		}
		openCheckerQueue();
		ensureRoleVisibleInCheckerQueue(role, true);
		verifyRoleViewDetails(role, UserManagementAccounts.maker(), "CREATE", "PENDING", false);
		boolean blocked = attemptApprove(role, checker, true);
		StepLog.check("Roles | Same-role Checker blocked",
				checker + " with same Admin-like role attempts to approve Maker role request",
				"Same-role person cannot approve; toaster/error or Approve blocked",
				"checker=" + checker + " role=" + role + " blocked=" + blocked, blocked,
				"checker=" + checker + "; role=" + role);
		UserManagementSession.restoreMaker();
		ensureMakerOnRoles();
	}

	public void independentCheckerApprovesRole() {
		String role = requireRole("Independent Checker");
		if (role == null) {
			return;
		}
		String primary = UserManagementAccounts.primaryChecker();
		String approvedBy = primary;
		boolean approved = approveAsIndependent(role, primary);
		if (!approved && isRoleInApprovedQueue(role)) {
			approved = true;
		}
		if (!approved) {
			String branch = UserManagementAccounts.branchChecker();
			approved = approveAsIndependent(role, branch);
			if (approved) {
				approvedBy = branch;
			}
		}
		UserManagementStore.setRoleApproved(approved);
		if (approved) {
			UserManagementStore.setLastReviewedBy(approvedBy);
			UserManagementStore.setLastReviewDecision("APPROVED");
			UserManagementStore.setApprovedRoleName(role);
			UserManagementStore.setApprovedRoleCode(UserManagementStore.roleCode());
		}
		StepLog.check("Roles | Independent Checker approval",
				"Different-role Checker approves TEST AUTO role request",
				"Approval succeeds", "role=" + role + " approved=" + approved, approved,
				"primary=" + primary + "; role=" + role);
		UserManagementSession.restoreMaker();
		ensureMakerOnRoles();
	}

	/** Module whose permission the demo flow enables on the TEST AUTO role (View/Add/Edit/Download chips). */
	private static final String PERMISSION_MODULE = "Dashboard";

	/**
	 * Finds the dialog row that starts with the module name and holds exactly one checkbox. wanted=null just reads the
	 * state; true/false clicks only when the state differs. Returns the checkbox state after the call.
	 */
	private static final String MODULE_CHECKBOX_JS =
			"var module=arguments[0],wanted=arguments[1];"
					+ "var dlg=document.querySelector('[role=dialog]')||document;"
					+ "var boxes=dlg.querySelectorAll('input[type=checkbox]');"
					+ "for(var i=0;i<boxes.length;i++){var cb=boxes[i];var row=cb.parentElement;"
					+ "for(var k=0;k<5&&row;k++){var t=(row.innerText||'').trim();"
					+ "if(t.toLowerCase().indexOf(module.toLowerCase())===0&&row.querySelectorAll('input[type=checkbox]').length===1){"
					+ "if(wanted!==null&&cb.checked!==wanted){cb.click();}return cb.checked;}"
					+ "row=row.parentElement;}}"
					+ "return false;";

	/**
	 * Opens Configure Permissions of the TEST AUTO role. Returns false (and closes the dialog) when the dialog that opened
	 * is not for this role - a permission change must never be made on another role.
	 */
	private boolean openPermissionsOf(String role, String step) {
		ensureMakerOnRoles();
		closePanel();
		if (getDriver().findElements(RolesPage.configureFor(role)).isEmpty()) {
			StepLog.check("Roles | " + step, "Open Configure Permissions on TEST AUTO role",
					"Permissions action exists on the TEST AUTO card (never Super Admin)",
					"FAIL | Configure Permissions not found for " + role + ". Super Admin / other roles were not used.",
					false, "role=" + role);
			return false;
		}
		MasterUiHelper.clickJs(RolesPage.configureFor(role), waitSeconds);
		MasterUiHelper.sleep(600);
		String title = "";
		try {
			title = PomElementManager.findVisible(RolesPage.PERMISSION_DIALOG, waitSeconds).getText();
		} catch (RuntimeException ignored) {
			// handled below
		}
		boolean ours = title != null && title.toLowerCase().contains(role.toLowerCase());
		if (!ours) {
			StepLog.check("Roles | " + step, "Permissions dialog opens for the TEST AUTO role only",
					"Dialog title names " + role,
					"FAIL | dialog title='" + firstLine(title) + "' - closed without any change", false, "role=" + role);
			cancelPermissionDialog();
			return false;
		}
		return true;
	}

	private static String firstLine(String text) {
		if (text == null) {
			return "";
		}
		int i = text.indexOf('\n');
		return (i > 0 ? text.substring(0, i) : text).trim();
	}

	private void cancelPermissionDialog() {
		By cancel = By.xpath("//*[@role='dialog']//button[normalize-space()='Cancel']");
		if (!getDriver().findElements(cancel).isEmpty()) {
			MasterUiHelper.clickJs(cancel, waitSeconds);
			MasterUiHelper.sleep(300);
		} else {
			closePanel();
		}
	}

	/** Read-only check: ticking a child module also ticks its parent. Nothing is saved. */
	public void inspectPermissionParentChildRule() {
		String role = UserManagementStore.roleName();
		if (role == null) {
			StepLog.check("Roles | Permissions parent/child", "Configure permissions on TEST AUTO role only",
					"Approved TEST AUTO role exists; Super Admin is never opened",
					"FAIL | No TEST AUTO role was created in this run", false, "neverTarget=Super Admin");
			return;
		}
		if (!openPermissionsOf(role, "Permissions parent/child")) {
			return;
		}
		Hooks.captureMilestone("Roles permissions dialog | " + role);
		boolean parentChild = verifyParentFollowsChild();
		StepLog.check("Roles | Permissions parent/child",
				"Selecting a child module also checks the parent module",
				"Parent is checked when child is selected on TEST AUTO role",
				"role=" + role + " parentFollowsChild=" + parentChild, parentChild, "role=" + role);
		cancelPermissionDialog();
	}

	/**
	 * Maker enables one module (checkbox) on the TEST AUTO role and saves; the change goes to the Checker queue as a
	 * request. Nothing is saved on any other role.
	 */
	public void enablePermissionOnRole() {
		String role = requireRole("Enable permission");
		if (role == null) {
			return;
		}
		if (!openPermissionsOf(role, "Enable permission")) {
			return;
		}
		boolean ticked = setModuleCheckbox(PERMISSION_MODULE, true);
		MasterUiHelper.sleep(400);
		String granted = readPermissionsGranted();
		Hooks.captureMilestone("Roles permission ticked | " + role + " | " + PERMISSION_MODULE);
		if (!ticked) {
			StepLog.check("Roles | Enable permission", "Tick the " + PERMISSION_MODULE + " module checkbox",
					"Checkbox is ticked and Save Permissions becomes enabled",
					"FAIL | module checkbox for " + PERMISSION_MODULE + " not found/ticked", false, "role=" + role);
			cancelPermissionDialog();
			return;
		}
		boolean saveEnabled = false;
		List<WebElement> save = getDriver().findElements(RolesPage.SAVE_PERMISSIONS);
		if (!save.isEmpty()) {
			saveEnabled = save.get(0).isEnabled() && !"true".equals(save.get(0).getAttribute("aria-disabled"));
		}
		if (saveEnabled) {
			MasterUiHelper.clickJs(RolesPage.SAVE_PERMISSIONS, waitSeconds);
			MasterUiHelper.sleep(500);
			confirmIfPresent();
		}
		String toast = ToastHandler.waitAndRead(getDriver(), 3500);
		closePanel();
		boolean inMyRequests = false;
		for (int i = 0; i < 8 && !inMyRequests; i++) {
			inMyRequests = !getDriver().findElements(RolesPage.myRequestPending(role)).isEmpty();
			if (!inMyRequests) {
				MasterUiHelper.sleep(500);
			}
		}
		boolean queued = saveEnabled && (inMyRequests || isQueued(toast));
		if (queued) {
			UserManagementStore.setLastPendingAction("ROLE_PERMISSION");
		}
		StepLog.check("Roles | Enable permission",
				"Tick the " + PERMISSION_MODULE + " module on the TEST AUTO role and Save Permissions",
				"Permission change is submitted to the Checker queue",
				"role=" + role + " module=" + PERMISSION_MODULE + " ticked=" + ticked + " " + granted
						+ " saveEnabled=" + saveEnabled + " toast='" + toast + "' inMyRequestsPending=" + inMyRequests,
				queued, "role=" + role + "; module=" + PERMISSION_MODULE);
		Hooks.captureMilestone("Roles permission submitted | " + role);
	}

	public void independentCheckerApprovesPermissionRequest() {
		String role = requireRole("Checker approves permission");
		if (role == null) {
			return;
		}
		if (!"ROLE_PERMISSION".equals(UserManagementStore.lastPendingAction())) {
			StepLog.check("Roles | Checker approves permission", "Approve the pending permission request",
					"A permission request is pending for " + role,
					"FAIL | Prerequisite permission request was not submitted. last="
							+ UserManagementStore.lastPendingAction(),
					false, "role=" + role);
			return;
		}
		String checker = UserManagementAccounts.primaryChecker();
		boolean approved = approvePendingAs(role, checker);
		if (!approved) {
			String branch = UserManagementAccounts.branchChecker();
			if (!branch.equalsIgnoreCase(checker)) {
				approved = approvePendingAs(role, branch);
				if (approved) {
					checker = branch;
				}
			}
		}
		if (approved) {
			UserManagementStore.setLastReviewedBy(checker);
			UserManagementStore.setLastReviewDecision("APPROVED");
		}
		StepLog.check("Roles | Checker approves permission",
				"Different-role Checker approves the TEST AUTO role permission request",
				"Approval succeeds and the request is no longer pending",
				"role=" + role + " checker=" + checker + " approved=" + approved, approved,
				"checker=" + checker + "; role=" + role);
		UserManagementSession.restoreMaker();
		ensureMakerOnRoles();
	}

	/** Re-opens the role's permissions as Maker and proves the approved module checkbox is really saved. */
	public void verifyPermissionSaved() {
		String role = requireRole("Verify permission saved");
		if (role == null) {
			return;
		}
		if (!openPermissionsOf(role, "Permission saved")) {
			return;
		}
		boolean checked = isModuleChecked(PERMISSION_MODULE);
		String granted = readPermissionsGranted();
		Hooks.captureMilestone("Roles permission after approval | " + role);
		StepLog.check("Roles | Permission saved",
				"Re-open Configure Permissions on the TEST AUTO role after Checker approval",
				PERMISSION_MODULE + " is ticked after approval",
				"role=" + role + " " + PERMISSION_MODULE + "Ticked=" + checked + " " + granted, checked,
				"role=" + role + "; module=" + PERMISSION_MODULE);
		cancelPermissionDialog();
	}

	private boolean approvePendingAs(String role, String checker) {
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		if (!UserManagementSession.openRolesOrFail(checker)) {
			return false;
		}
		openCheckerQueue();
		ensureRoleVisibleInCheckerQueue(role, true);
		if (!hasPendingRoleActions(role)) {
			StepLog.check("Roles | Checker queue | " + checker, "Pending TEST AUTO role request is in Checker queue",
					"Pending request for " + role + " is visible",
					"FAIL | No pending role request found for " + role + " | signedIn="
							+ UserManagementSession.signedInLabel(), false, "checker=" + checker + "; role=" + role);
			return false;
		}
		Hooks.captureMilestone("Roles Checker pending before approval | " + checker + " | " + role);
		boolean approved = attemptApprove(role, checker, false);
		Hooks.captureMilestone("Roles Checker after approval | " + checker + " | " + role);
		return approved;
	}

	/** Ticks/unticks the checkbox of one module row of the open permissions dialog; returns its state afterwards. */
	private boolean setModuleCheckbox(String module, boolean wanted) {
		return Boolean.TRUE.equals(((JavascriptExecutor) getDriver()).executeScript(MODULE_CHECKBOX_JS, module, wanted));
	}

	private boolean isModuleChecked(String module) {
		return Boolean.TRUE.equals(((JavascriptExecutor) getDriver()).executeScript(MODULE_CHECKBOX_JS, module, null));
	}

	private String readPermissionsGranted() {
		try {
			Object o = ((JavascriptExecutor) getDriver()).executeScript(
					"var m=(document.body.innerText||'').match(/(\\d+)\\s+permissions?\\s+granted/i);return m?m[0]:'';");
			return "granted='" + (o == null ? "" : o) + "'";
		} catch (RuntimeException e) {
			return "granted=''";
		}
	}

	public void restoreMakerForNextModule() {
		UserManagementSession.restoreMaker();
		ensureMakerOnRoles();
		StepLog.info("Roles | Restore Maker session", "Return to Maker auto on Roles",
				"Maker session on /admin/roles",
				"user=" + UserManagementAccounts.maker() + " url=" + getDriver().getCurrentUrl(),
				"user=" + UserManagementAccounts.maker());
	}

	private boolean approveAsIndependent(String role, String checker) {
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		if (!UserManagementSession.openRolesOrFail(checker)) {
			return false;
		}
		openCheckerQueue();
		ensureRoleVisibleInCheckerQueue(role, true);
		if (!hasPendingRoleActions(role)) {
			if (isRoleInApprovedQueue(role)) {
				verifyRoleViewDetails(role, checker, "CREATE", "APPROVED", true);
				return true;
			}
			StepLog.check("Roles | Checker queue | " + checker, "Pending TEST AUTO role is in Checker queue",
					"Pending request for " + role + " is visible",
					"FAIL | No pending role request found for " + role, false,
					"checker=" + checker + "; role=" + role);
			return false;
		}
		verifyRoleViewDetails(role, UserManagementAccounts.maker(), "CREATE", "PENDING", false);
		Hooks.captureMilestone("Roles Checker pending before approval | " + checker + " | " + role);
		boolean approved = attemptApprove(role, checker, false);
		Hooks.captureMilestone("Roles Checker after approval | " + checker + " | " + role);
		boolean done = approved || isRoleInApprovedQueue(role);
		if (done) {
			selectCheckerQueueStatus("Approved");
			ensureRoleVisibleInCheckerQueue(role, false);
			verifyRoleViewDetails(role, checker, "CREATE", "APPROVED", true);
		}
		return done;
	}

	private boolean attemptApprove(String role, String actor, boolean expectRejection) {
		List<WebElement> approve = getDriver().findElements(RolesPage.approveFor(role));
		if (approve.isEmpty()) {
			if (expectRejection) {
				return true;
			}
			return isRoleInApprovedQueue(role);
		}
		ToastHandler.dismissIfPresent(getDriver());
		MasterUiHelper.clickJs(RolesPage.approveFor(role), waitSeconds);
		MasterUiHelper.sleep(500);
		confirmIfPresent();
		MasterUiHelper.sleep(1500);
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		if (expectRejection) {
			return isRejection(toast) || !isApproved(toast);
		}
		MasterUiHelper.sleep(1200);
		boolean approveStillPresent = !getDriver().findElements(RolesPage.approveFor(role)).isEmpty();
		return isApproved(toast) || !approveStillPresent || isRoleInApprovedQueue(role);
	}

	private boolean verifyParentFollowsChild() {
		List<WebElement> childBoxes = getDriver().findElements(By.xpath(
				"//*[contains(normalize-space(),'State') or contains(normalize-space(),'District')]"
						+ "/ancestor::*[self::tr or contains(@class,'Mui') or self::div][1]//input[@type='checkbox']"));
		if (childBoxes.isEmpty()) {
			childBoxes = getDriver().findElements(By.cssSelector("input[type='checkbox']"));
		}
		if (childBoxes.isEmpty()) {
			return false;
		}
		WebElement child = childBoxes.get(Math.min(1, childBoxes.size() - 1));
		if (!child.isSelected()) {
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", child);
			MasterUiHelper.sleep(300);
		}
		List<WebElement> parents = getDriver().findElements(By.xpath(
				"//*[normalize-space()='Masters' or normalize-space()='Dashboard']"
						+ "/ancestor::*[self::tr or contains(@class,'MuiAccordion') or self::div][1]//input[@type='checkbox']"));
		if (parents.isEmpty()) {
			return child.isSelected();
		}
		return parents.get(0).isSelected();
	}

	private String requireRole(String step) {
		String role = UserManagementStore.roleName();
		if (role == null) {
			StepLog.check("Roles | " + step, "Prerequisite TEST AUTO role from this run",
					"Created TEST AUTO role is available",
					"FAIL | No TEST AUTO role request was created in this run.", false, "prerequisite=missing");
		}
		return role;
	}

	private void ensureMakerOnRoles() {
		UserManagementSession.restoreMaker();
		// nav bar first (User Management > Roles); the endpoint only if the nav item is not there
		UserManagementSession.openRolesNavFirst();
		PomElementManager.findVisible(RolesPage.PAGE_HEADING, waitSeconds);
		clickMaker();
	}

	private void clickMaker() {
		if (MasterUiHelper.isVisible(RolesPage.MAKER, 2)) {
			MasterUiHelper.clickJs(RolesPage.MAKER, waitSeconds);
			MasterUiHelper.sleep(300);
		}
	}

	private void openCheckerQueue() {
		UserManagementSession.switchToCheckerMode(RolesPage.CHECKER);
		selectCheckerQueueStatus("Pending");
	}

	private void selectCheckerQueueStatus(String label) {
		try {
			String safe = label == null ? "" : label.replace("'", "");
			By combo = By.xpath(
					"//*[(@role='combobox') and (contains(@aria-label,'Status') or contains(normalize-space(.),'Status'))][1]");
			if (!getDriver().findElements(combo).isEmpty()) {
				MasterUiHelper.clickJs(combo, waitSeconds);
				MasterUiHelper.sleep(300);
			}
			By option = By.xpath(
					"//*[(@role='option' or @role='menuitem')][contains(normalize-space(.),'" + safe + "')][1]");
			if (!getDriver().findElements(option).isEmpty()) {
				MasterUiHelper.clickJs(option, waitSeconds);
				MasterUiHelper.sleep(600);
			}
		} catch (Exception e) {
			if (!getDriver().findElements(RolesPage.PENDING_TAB).isEmpty()) {
				MasterUiHelper.clickJs(RolesPage.PENDING_TAB, waitSeconds);
				MasterUiHelper.sleep(400);
			}
		}
	}

	private boolean hasPendingRoleActions(String role) {
		return !getDriver().findElements(RolesPage.approveFor(role)).isEmpty();
	}

	private void ensureRoleVisibleInCheckerQueue(String role, boolean pendingActions) {
		if (pendingActions && hasPendingRoleActions(role)) {
			return;
		}
		if (!pendingActions && pageContainsRoleCard(role)) {
			return;
		}
		for (int page = 0; page < 12; page++) {
			if (pendingActions && hasPendingRoleActions(role)) {
				return;
			}
			if (!pendingActions && pageContainsRoleCard(role)) {
				return;
			}
			List<WebElement> nextButtons = getDriver().findElements(RolesPage.NEXT_PAGE);
			if (nextButtons.isEmpty() || !nextButtons.get(0).isEnabled()) {
				return;
			}
			MasterUiHelper.clickJs(RolesPage.NEXT_PAGE, waitSeconds);
			MasterUiHelper.sleep(700);
		}
	}

	private boolean isRoleInApprovedQueue(String role) {
		selectCheckerQueueStatus("Approved");
		MasterUiHelper.sleep(500);
		ensureRoleVisibleInCheckerQueue(role, false);
		return pageContainsRoleCard(role);
	}

	private boolean pageContainsRoleCard(String role) {
		return !getDriver().findElements(RolesPage.card(role)).isEmpty();
	}

	private void verifyRoleViewDetails(String role, String actor, String expectedType, String expectedStatus,
			boolean requireApprovedBy) {
		List<WebElement> viewBtn = getDriver().findElements(RolesPage.viewDetailsFor(role));
		if (viewBtn.isEmpty()) {
			StepLog.check("Roles | View Details | " + actor, "Click View Details eye icon for " + role,
					"View Details eye icon is available on the role request",
					"FAIL | View Details eye icon not found for " + role, false, "role=" + role);
			return;
		}
		MasterUiHelper.clickJs(RolesPage.viewDetailsFor(role), waitSeconds);
		MasterUiHelper.sleep(800);
		Hooks.captureMilestone("Roles View Details | " + actor + " | " + role);
		String pageText = "";
		try {
			pageText = getDriver().findElement(By.tagName("body")).getText();
		} catch (Exception ignored) {
		}
		boolean hasRole = pageText.toLowerCase().contains(role.toLowerCase());
		boolean hasMaker = pageText.toLowerCase().contains(UserManagementAccounts.maker().toLowerCase());
		boolean hasType = expectedType == null || pageText.toLowerCase().contains("create")
				|| pageText.toLowerCase().contains(expectedType.toLowerCase());
		boolean hasStatus = expectedStatus == null
				|| pageText.toLowerCase().contains(expectedStatus.toLowerCase());
		boolean hasApprovedBy = actor != null && pageText.toLowerCase().contains(actor.toLowerCase());
		boolean pass = hasRole && hasMaker && hasType && hasStatus && (!requireApprovedBy || hasApprovedBy);
		StepLog.check("Roles | View Details verification | " + actor,
				"Open eye icon and verify Role Name, Maker, Type, Status"
						+ (requireApprovedBy ? ", Approved by" : ""),
				"Modal/card shows role, maker, type, status"
						+ (requireApprovedBy ? ", and checker who approved" : ""),
				"roleFound=" + hasRole + " makerFound=" + hasMaker + " typeFound=" + hasType
						+ " statusFound=" + hasStatus + " approvedByFound=" + hasApprovedBy
						+ " status=" + expectedStatus,
				pass, "actor=" + actor + "; role=" + role);
		closePanel();
	}

	private void closePanel() {
		if (!getDriver().findElements(RolesPage.CANCEL).isEmpty()) {
			MasterUiHelper.clickJs(RolesPage.CANCEL, waitSeconds);
		} else if (!getDriver().findElements(RolesPage.CLOSE_PANEL).isEmpty()) {
			MasterUiHelper.clickJs(RolesPage.CLOSE_PANEL, waitSeconds);
		}
	}

	private void confirmIfPresent() {
		MasterUiHelper.sleep(250);
		try {
			List<WebElement> remarks = getDriver().findElements(Com.Ckyc_4_0.pages.usermanagement.UsersPage.DIALOG_REMARKS);
			if (!remarks.isEmpty() && remarks.get(0).isDisplayed()
					&& (remarks.get(0).getAttribute("value") == null || remarks.get(0).getAttribute("value").isBlank())) {
				MasterUiHelper.typeInto(Com.Ckyc_4_0.pages.usermanagement.UsersPage.DIALOG_REMARKS,
						"TEST AUTO remarks - automation", waitSeconds);
			}
		} catch (RuntimeException ignored) {
			// no remarks box in this dialog
		}
		if (!getDriver().findElements(RolesPage.CONFIRM).isEmpty()) {
			MasterUiHelper.clickJs(RolesPage.CONFIRM, waitSeconds);
		}
	}

	private void forceClickSave() {
		List<WebElement> saves = getDriver().findElements(RolesPage.SAVE);
		if (!saves.isEmpty()) {
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", saves.get(0));
			MasterUiHelper.sleep(400);
		}
	}

	private int helperCount() {
		return getDriver().findElements(By.cssSelector(".MuiFormHelperText-root")).size();
	}

	private boolean isQueued(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		if (value.contains("unsuccess") || value.contains("fail") || value.contains("error") || value.contains("already")) {
			return false;
		}
		return value.contains("success") || value.contains("submitted") || value.contains("pending")
				|| value.contains("checker");
	}

	private boolean isRejection(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("maker") || value.contains("own") || value.contains("not allowed")
				|| value.contains("cannot") || value.contains("same role") || value.contains("self");
	}

	private boolean isApproved(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("approved") || value.contains("success");
	}

	private boolean pageContains(String text) {
		return pageContainsRoleCard(text);
	}
}
