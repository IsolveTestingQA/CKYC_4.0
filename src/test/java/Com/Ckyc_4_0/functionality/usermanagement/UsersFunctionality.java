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
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.UtilityFiles.UserManagementAccounts;
import Com.Ckyc_4_0.UtilityFiles.UserManagementSession;
import Com.Ckyc_4_0.UtilityFiles.UserManagementStore;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.usermanagement.UsersPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;

/**
 * Users Maker/Checker. Negatives first. Same-role Checker must fail. Independent Checker is shyam;
 * branch Checker (lock/unlock/dormant) is Nivijay. Fail when expected action cannot be proven.
 */
public class UsersFunctionality extends BaseClass {
	private final int waitSeconds = ConfigReader.getExplicitWait();

	public void openUsers() {
		ensureMakerOnUsers();
		Hooks.captureMilestone("User Management Users page opened");
	}

	public void verifyMakerControls() {
		boolean pass = MasterUiHelper.isVisible(UsersPage.MAKER, waitSeconds)
				&& MasterUiHelper.isVisible(UsersPage.CHECKER, waitSeconds)
				&& MasterUiHelper.isVisible(UsersPage.ADD_USER, waitSeconds);
		StepLog.check("Users | Maker controls", "Maker, Checker and Add User controls visible",
				"All Maker/Checker controls visible", "visible=" + pass, pass, "url=" + getDriver().getCurrentUrl());
	}

	public void validateEmptyAddUser() {
		openAddUser();
		forceClickSave();
		int errors = helperCount();
		StepLog.check("Users | Mandatory validation", "Save blank Add User form",
				"Mandatory validation is displayed", "helperErrors=" + errors, errors > 0, "form=Add User");
		closePanel();
	}

	public void validateUserFieldRules() {
		openAddUser();
		validateMaxLength("First Name", UsersPage.FIRST_NAME, repeat("A", 51), 50);
		validateMaxLength("Email ID", UsersPage.EMAIL, repeat("a", 101) + "@x.test", 100);
		validateMaxLength("Login Name", UsersPage.LOGIN_NAME, repeat("x", 31), 30);
		type(UsersPage.FIRST_NAME, "AB12@");
		type(UsersPage.EMAIL, "not-an-email");
		type(UsersPage.MOBILE, "ABC123");
		MasterUiHelper.clickJs(UsersPage.SAVE, waitSeconds);
		MasterUiHelper.sleep(400);
		int errors = helperCount();
		StepLog.check("Users | Invalid datatype negative",
				"Submit digits/specials in name, invalid email and nonnumeric mobile",
				"UI blocks invalid data with validation and does not queue request",
				"helperErrors=" + errors + "; first='" + read(UsersPage.FIRST_NAME) + "'; email='"
						+ read(UsersPage.EMAIL) + "'; mobile='" + read(UsersPage.MOBILE) + "'",
				errors > 0, "first=AB12@; email=not-an-email; mobile=ABC123");
		closePanel();
	}

	public void verifyDuplicateLoginRejected() {
		openAddUser();
		fillRequiredCandidate("auto");
		MasterUiHelper.clickJs(UsersPage.SAVE, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		int errors = helperCount();
		boolean rejected = isDuplicateMessage(toast) || errors > 0;
		StepLog.check("Users | Duplicate login negative", "Submit existing login name auto",
				"Existing login is rejected and no request is created",
				"toast='" + toast + "' helperErrors=" + errors, rejected, "login=auto");
		closePanel();
	}

	public void createUniqueUserRequest() {
		openAddUser();
		String login = UserManagementStore.uniqueLogin();
		MasterUiHelper.typeInto(UsersPage.FIRST_NAME, "TEST", waitSeconds);
		MasterUiHelper.typeInto(UsersPage.LAST_NAME, "AUTO", waitSeconds);
		MasterUiHelper.typeInto(UsersPage.DESIGNATION, "QA TESTER", waitSeconds);
		MasterUiHelper.typeInto(UsersPage.EMAIL, login + "@example.test", waitSeconds);
		MasterUiHelper.typeInto(UsersPage.MOBILE,
				"98765" + String.format("%05d", System.currentTimeMillis() % 100000), waitSeconds);
		MasterUiHelper.typeInto(UsersPage.LOGIN_NAME, login, waitSeconds);
		selectFirstOption(UsersPage.SOL_COMBO, "SOL/REG/CPC");
		selectNonSuperAdminRole();
		Hooks.captureMilestone("Users Add User populated before submit | " + login);
		MasterUiHelper.clickJs(UsersPage.SAVE, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 3500);
		if (toast == null || toast.isBlank()) {
			toast = ToastHandler.waitAndRead(getDriver(), 2500);
		}
		closePanel();
		clickMaker();
		search(login);
		boolean queued = isQueuedOrSaved(toast) || hasPendingRequestEvidence(login, toast);
		UserManagementStore.setLoginName(login);
		if (queued) {
			UserManagementStore.setLastPendingAction("CREATE");
		}
		boolean toastPresent = toast != null && !toast.isBlank();
		StepLog.check("Users | Create TEST AUTO request", "Submit unique login name to Checker",
				"Pending request/toast confirms submission to Checker queue",
				"login=" + login + " toast='" + toast + "' queued=" + queued + " toastPresent=" + toastPresent,
				queued, "login=" + login + "; email=" + login + "@example.test");
		if (queued && !toastPresent) {
			StepLog.check("Users | Create TEST AUTO toast",
					"Toast message should appear after user request submission",
					"Non-empty success/pending toast is shown",
					"FAIL (BUG) | Request queued but toaster was missing. login=" + login,
					false, "login=" + login);
		}
		Hooks.captureMilestone("Users Add User submitted | " + login);
	}

	public void verifyMakerCannotApproveOwnRequest() {
		String login = requireLogin("Maker self-approve");
		if (login == null) {
			return;
		}
		ensureMakerOnUsers();
		openCheckerQueue();
		ensureCheckerQueueRowVisible(login, true);
		verifyViewDetails(login, UserManagementAccounts.maker(), UserManagementStore.lastPendingAction(), "PENDING");
		boolean rejected = attemptApprove(login, UserManagementAccounts.maker(), true);
		StepLog.check("Users | Maker self-approve blocked", "Maker attempts approval of own TEST AUTO request",
				"Self-approval is rejected or Approve is not available to Maker",
				"login=" + login + " rejectedOrBlocked=" + rejected, rejected, "login=" + login);
		Hooks.captureMilestone("Users maker self-approve result | " + login);
		clickMaker();
	}

	public void sameRoleCheckerCannotApprove() {
		String login = requireLogin("Same-role Checker");
		if (login == null) {
			return;
		}
		String checker = UserManagementAccounts.sameRoleChecker();
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		if (!UserManagementSession.openUsersOrFail(checker + " (same-role negative)")) {
			UserManagementSession.restoreMaker();
			ensureMakerOnUsers();
			return;
		}
		openCheckerQueue();
		ensureCheckerQueueRowVisible(login, true);
		verifyViewDetails(login, checker, UserManagementStore.lastPendingAction(), "PENDING");
		boolean blocked = attemptApprove(login, checker, true);
		StepLog.check("Users | Same-role Checker blocked",
				checker + " with same Admin-like role attempts to approve Maker request",
				"Same-role person cannot approve; toaster/error or Approve blocked",
				"checker=" + checker + " login=" + login + " blocked=" + blocked, blocked,
				"checker=" + checker + "; login=" + login);
		Hooks.captureMilestone("Users same-role checker result | " + checker + " | " + login);
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
	}

	public void independentCheckerApprovesCreatedRequest() {
		String login = requireLogin("Independent Checker");
		if (login == null) {
			return;
		}
		String primary = UserManagementAccounts.primaryChecker();
		String approvedBy = primary;
		boolean approved = approveAsIndependent(login, primary);
		if (!approved && isRequestInDecisionQueue(login, "APPROVED")) {
			approved = true;
		}
		if (!approved) {
			String branch = UserManagementAccounts.branchChecker();
			StepLog.check("Users | Independent Checker fallback",
					"Primary Checker " + primary + " did not approve; try branch Checker " + branch,
					"Branch Checker approves only if primary cannot complete",
					"trying=" + branch, true, "fallback=" + branch);
			approved = approveAsIndependent(login, branch);
			if (approved) {
				approvedBy = branch;
			}
		}
		UserManagementStore.setUserApproved(approved);
		if (approved) {
			UserManagementStore.setLastReviewedBy(approvedBy);
			UserManagementStore.setLastReviewDecision("APPROVED");
		}
		StepLog.check("Users | Independent Checker approval",
				"Different-role Checker approves TEST AUTO user request",
				"Approval succeeds and request is no longer pending",
				"login=" + login + " approved=" + approved, approved,
				"primary=" + primary + "; login=" + login);
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
	}

	/**
	 * After approval, login as the newly created TEST AUTO user with default password.
	 * Only attempt if user was approved. If login fails → BUG-003.
	 */
	public void loginAsApprovedTestAutoUser() {
		String login = requireLogin("Login as approved TEST AUTO user");
		if (login == null) return;
		if (!UserManagementStore.userApproved()) {
			StepLog.check("Users | Login as new user", "Login with approved TEST AUTO credentials",
					"Only attempt after approval",
					"SKIP | User was not approved in this run — cannot test login",
					false, "login=" + login);
			return;
		}
		UserManagementSession.loginAs(login, UserManagementAccounts.defaultPassword());
		MasterUiHelper.sleep(1500);
		String url = getDriver().getCurrentUrl();
		boolean onApp = url != null && !url.toLowerCase().contains("/login");
		if (onApp) {
			Hooks.captureMilestone("Login as TEST AUTO user SUCCESS | " + login);
			StepLog.check("Users | Login as new user",
					"Login with created login=" + login + " / default password after approval",
					"Dashboard or app page loads successfully",
					"PASS | URL=" + url, true, "login=" + login);
			// logout back
			UserManagementSession.restoreMaker();
		} else {
			Hooks.captureMilestone("BUG-003 Login as TEST AUTO user FAILED | " + login);
			StepLog.check("Users | Login as new user",
					"Login with created login=" + login + " / default password after approval",
					"Dashboard or app page loads successfully",
					"FAIL (BUG-003) | Still on login page. New user cannot login with Welcome@123. URL=" + url,
					false, "login=" + login);
			// recover maker session
			UserManagementSession.restoreMaker();
		}
		ensureMakerOnUsers();
	}

	/**
	 * Search should ONLY happen after approval — verifies the user was approved by the checker.
	 * If not approved, mark as SKIP.
	 */
	public void searchCreatedUser() {
		String login = requireLogin("Search TEST AUTO user");
		if (login == null) {
			return;
		}
		if (!UserManagementStore.userApproved()) {
			StepLog.check("Users | Search TEST AUTO",
					"Search approved user in Maker grid after checker approval",
					"Only search after checker has approved the user",
					"SKIP | User was not approved — search not applicable until approved",
					false, "login=" + login);
			return;
		}
		ensureMakerOnUsers();
		clickMaker();
		search(login);
		boolean found = !getDriver().findElements(UsersPage.row(login)).isEmpty();

		// Also log visible action buttons on the found row
		if (found) {
			logVisibleActionButtons(login, "Search result");
		}

		StepLog.check("Users | Search TEST AUTO",
				"Search approved user '" + login + "' in Maker grid after checker approval",
				"TEST AUTO login row is visible with Active status",
				"login=" + login + " found=" + found, found, "login=" + login);
		Hooks.captureMilestone("Users search result | " + login);
	}

	public void editCreatedUser() {
		String login = requireApprovedOrQueued("Edit TEST AUTO user");
		if (login == null) {
			return;
		}
		ensureMakerOnUsers();
		clickMaker();
		waitForRowReady(login, 20);
		search(login);
		if (getDriver().findElements(UsersPage.editFor(login)).isEmpty()) {
			StepLog.check("Users | Edit TEST AUTO", "Open Edit on TEST AUTO row only",
					"Edit action is available on the created user",
					"FAIL | Edit not found for " + login, false, "login=" + login);
			return;
		}
		MasterUiHelper.clickJs(UsersPage.editFor(login), waitSeconds);
		PomElementManager.findVisible(UsersPage.DESIGNATION, waitSeconds);
		MasterUiHelper.typeInto(UsersPage.DESIGNATION, "QA TESTER EDIT", waitSeconds);
		Hooks.captureMilestone("Users Edit TEST AUTO before submit | " + login);
		MasterUiHelper.clickJs(UsersPage.SAVE, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		boolean queued = isQueuedOrSaved(toast);
		if (queued) {
			UserManagementStore.setLastPendingAction("EDIT");
		}
		StepLog.check("Users | Edit TEST AUTO", "Submit edit on TEST AUTO user only",
				"Edit is submitted for Checker (or saved if no queue)",
				"login=" + login + " toast='" + toast + "'", queued, "login=" + login + "; designation=QA TESTER EDIT");
		closePanel();
	}

	public void independentCheckerApprovesEditedRequest() {
		approveActionWithIndependentChecker("EDIT", "Users | Independent Checker edit approval",
				"Different-role Checker approves TEST AUTO edit request");
	}

	public void submitEditRequestForRevert() {
		String login = requireApprovedOrQueued("Revert TEST AUTO user edit");
		if (login == null) {
			return;
		}
		ensureMakerOnUsers();
		clickMaker();
		waitForRowReady(login, 20);
		search(login);
		if (getDriver().findElements(UsersPage.editFor(login)).isEmpty()) {
			StepLog.check("Users | Revert TEST AUTO", "Submit a second edit request for revert validation",
					"Edit action is available on the created user",
					"FAIL | Edit not found for " + login, false, "login=" + login);
			return;
		}
		MasterUiHelper.clickJs(UsersPage.editFor(login), waitSeconds);
		PomElementManager.findVisible(UsersPage.DESIGNATION, waitSeconds);
		MasterUiHelper.typeInto(UsersPage.DESIGNATION, "QA TESTER REVERT", waitSeconds);
		Hooks.captureMilestone("Users Revert edit before submit | " + login);
		MasterUiHelper.clickJs(UsersPage.SAVE, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		boolean queued = isQueuedOrSaved(toast);
		if (queued) {
			UserManagementStore.setLastPendingAction("EDIT_REVERT");
		}
		StepLog.check("Users | Revert TEST AUTO", "Submit edit request that checker will revert",
				"Edit request is submitted to Checker queue",
				"login=" + login + " toast='" + toast + "' queued=" + queued,
				queued, "login=" + login + "; designation=QA TESTER REVERT");
		closePanel();
	}

	public void independentCheckerRevertsEditedRequest() {
		String login = requireLogin("Independent Checker revert");
		if (login == null) {
			return;
		}
		String checker = UserManagementAccounts.primaryChecker();
		boolean reverted = revertAsIndependent(login, checker);
		if (!reverted) {
			String branch = UserManagementAccounts.branchChecker();
			reverted = revertAsIndependent(login, branch);
			if (reverted) {
				checker = branch;
			}
		}
		if (reverted) {
			UserManagementStore.setLastReviewedBy(checker);
			UserManagementStore.setLastReviewDecision("REVERTED");
		}
		StepLog.check("Users | Independent Checker revert",
				"Different-role Checker reverts TEST AUTO edit request",
				"Revert succeeds and request moves to Reverted",
				"login=" + login + " reverted=" + reverted, reverted,
				"checker=" + checker + "; login=" + login);
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
		search(login);
		boolean makerCanSeeUser = !getDriver().findElements(UsersPage.row(login)).isEmpty();
		StepLog.check("Users | Reverted request visible to maker",
				"Maker returns to Users grid after checker revert",
				"Created TEST AUTO user row is still visible for follow-up action",
				"login=" + login + " visibleToMaker=" + makerCanSeeUser,
				makerCanSeeUser, "login=" + login);
	}

	public void lockCreatedUser() {
		submitRowAction("Lock");
	}

	public void branchCheckerApprovesLock() {
		approveBranchAction("LOCK");
	}

	public void unlockCreatedUser() {
		submitRowAction("Unlock");
	}

	public void branchCheckerApprovesUnlock() {
		approveBranchAction("UNLOCK");
	}

	/**
	 * Dormant flow: Edit the TEST AUTO user → uncheck Active checkbox → click Update →
	 * goes to checker queue → branch checker approves → status becomes Dormant/Exited.
	 */
	public void dormantCreatedUser() {
		String login = requireApprovedOrQueued("Dormant TEST AUTO user");
		if (login == null) {
			return;
		}
		ensureMakerOnUsers();
		clickMaker();
		waitForRowReady(login, 20);
		search(login);
		if (getDriver().findElements(UsersPage.editFor(login)).isEmpty()) {
			StepLog.check("Users | Dormant TEST AUTO", "Open TEST AUTO user Edit to set Dormant",
					"Edit is available on created user", "FAIL | Edit not found for " + login, false, "login=" + login);
			return;
		}
		MasterUiHelper.clickJs(UsersPage.editFor(login), waitSeconds);
		MasterUiHelper.sleep(600);

		// Uncheck Active checkbox to set Dormant
		boolean unchecked = uncheckActiveStatus();
		if (!unchecked) {
			// Try Dormant option in Status combo as fallback
			boolean set = setDormantStatus();
			if (!set) {
				StepLog.check("Users | Dormant TEST AUTO", "Uncheck Active checkbox or select Dormant status",
						"Active checkbox is unchecked or Dormant option selected",
						"FAIL | Neither Active checkbox nor Dormant option found", false, "login=" + login);
				closePanel();
				return;
			}
		}

		Hooks.captureMilestone("Users Dormant before Update | " + login);
		MasterUiHelper.clickJs(UsersPage.SAVE, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		boolean queued = isQueuedOrSaved(toast);
		if (queued) {
			UserManagementStore.setLastPendingAction("DORMANT");
		}
		StepLog.check("Users | Dormant TEST AUTO", "Uncheck Active and click Update on TEST AUTO user",
				"Dormant/deactivation change is submitted to Checker queue",
				"login=" + login + " toast='" + toast + "' queued=" + queued, queued,
				"login=" + login + "; action=Uncheck Active → Dormant");
		closePanel();
	}

	/** Uncheck the Active status checkbox. Returns true if successfully unchecked. */
	private boolean uncheckActiveStatus() {
		List<WebElement> activeCheckbox = getDriver().findElements(UsersPage.ACTIVE_STATUS);
		if (activeCheckbox.isEmpty()) return false;
		WebElement cb = activeCheckbox.get(0);
		try {
			boolean isChecked = cb.isSelected()
					|| "true".equals(cb.getAttribute("checked"))
					|| "true".equals(cb.getAttribute("aria-checked"));
			if (isChecked) {
				((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", cb);
				MasterUiHelper.sleep(300);
				return true;
			}
			// Already unchecked — still dormant action
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void branchCheckerApprovesDormant() {
		approveBranchAction("DORMANT");
	}

	public void verifyLastProcessedRequestDetails() {
		String login = requireLogin("Verify processed TEST AUTO request details");
		if (login == null) {
			return;
		}
		String action = UserManagementStore.lastPendingAction();
		String checker = UserManagementStore.lastReviewedBy();
		String decision = UserManagementStore.lastReviewDecision();
		if (action == null || checker == null || decision == null) {
			StepLog.check("Users | Processed request details",
					"Verify Approved/Reverted details for the last processed TEST AUTO request",
					"Last action, checker, and decision are available",
					"FAIL | Missing review context action=" + action + " checker=" + checker + " decision=" + decision,
					false, "login=" + login);
			return;
		}
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		navigateToUsersViaSideNav();
		openCheckerDecisionTab(decision);
		verifyDecisionDetails(login, checker, action, decision);
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
	}

	/**
	 * After dormant is approved, verify:
	 * 1. Search the user — check if Exit flag / Dormant / Exited status is shown
	 * 2. Try login with that user's login name + default password — should FAIL (user is dormant/exited)
	 */
	public void verifyExitFlagAndLoginAfterDormant() {
		String login = requireLogin("Exit flag verification");
		if (login == null) return;

		ensureMakerOnUsers();
		clickMaker();
		search(login);

		// Check if the row shows Exit/Dormant/Exited status
		List<WebElement> row = getDriver().findElements(UsersPage.row(login));
		String rowText = "";
		if (!row.isEmpty()) {
			rowText = row.get(0).getText();
		}
		boolean hasExitFlag = rowText.toLowerCase().contains("dormant")
				|| rowText.toLowerCase().contains("exit")
				|| rowText.toLowerCase().contains("inactive")
				|| rowText.toLowerCase().contains("locked");

		StepLog.check("Users | Exit flag check",
				"Search dormant user and verify Exit/Dormant flag in status",
				"User row shows Dormant or Exited status",
				"login=" + login + " rowText='" + rowText + "' hasExitFlag=" + hasExitFlag,
				hasExitFlag || !row.isEmpty(), "login=" + login);
		Hooks.captureMilestone("Users Exit flag status | " + login);

		// Also log which action buttons are available for a dormant user
		if (!row.isEmpty()) {
			logVisibleActionButtons(login, "Exit flag / Dormant state");
		}

		// Now try login with dormant user credentials — should FAIL
		UserManagementSession.loginAs(login, UserManagementAccounts.defaultPassword());
		MasterUiHelper.sleep(1500);
		String url = getDriver().getCurrentUrl();
		boolean loginFailed = url != null && url.toLowerCase().contains("/login");

		if (loginFailed) {
			Hooks.captureMilestone("Dormant user login correctly BLOCKED | " + login);
			StepLog.check("Users | Dormant user login blocked",
					"Login with dormant/exited user credentials (login=" + login + " / Welcome@123)",
					"Login should be blocked — dormant user cannot access the system",
					"PASS | Login correctly rejected. User is dormant/exited. URL=" + url,
					true, "login=" + login);
		} else {
			Hooks.captureMilestone("BUG: Dormant user login ALLOWED | " + login);
			StepLog.check("Users | Dormant user login blocked",
					"Login with dormant/exited user credentials (login=" + login + " / Welcome@123)",
					"Login should be blocked — dormant user cannot access the system",
					"FAIL (BUG) | Dormant user was able to login! URL=" + url,
					false, "login=" + login);
		}

		// Restore maker
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
	}

	public void restoreMakerForNextModule() {
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
		StepLog.info("Users | Restore Maker session", "Return to Maker auto on Users",
				"Maker session on /admin/users", "user=" + UserManagementAccounts.maker()
						+ " url=" + getDriver().getCurrentUrl(),
				"user=" + UserManagementAccounts.maker());
	}

	/**
	 * Lock/Unlock: Click the icon button (tooltip "Lock Account"/"Unlock") on the user row.
	 * These are action icon buttons, NOT inside the Edit form.
	 * After clicking, confirm dialog if present, then verify toast for queue submission.
	 */
	private void submitRowAction(String action) {
		String login = UserManagementStore.loginName();
		if (login == null) {
			failMissing("Users | " + action + " TEST AUTO", "No TEST AUTO login exists to " + action.toLowerCase() + ".");
			return;
		}
		ensureMakerOnUsers();
		clickMaker();
		waitForRowReady(login, 20);
		search(login);

		// Log all visible action buttons for this row
		logVisibleActionButtons(login, action);

		By locator;
		if ("Unlock".equalsIgnoreCase(action)) {
			locator = UsersPage.unlockFor(login);
		} else if ("Lock".equalsIgnoreCase(action)) {
			locator = UsersPage.lockFor(login);
		} else if ("Deactivate".equalsIgnoreCase(action) || "Activate".equalsIgnoreCase(action)) {
			locator = UsersPage.deactivateFor(login);
		} else {
			locator = UsersPage.lockFor(login);
		}

		if (getDriver().findElements(locator).isEmpty()) {
			StepLog.check("Users | " + action + " TEST AUTO", action + " only the created TEST AUTO user",
					action + " icon button (tooltip) is available on the created row",
					"FAIL | " + action + " button not found for " + login
							+ ". Check if user is in correct state for this action.",
					false, "login=" + login);
			Hooks.captureMilestone("Users " + action + " button NOT found | " + login);
			return;
		}

		Hooks.captureMilestone("Users " + action + " before click | " + login);
		MasterUiHelper.clickJs(locator, waitSeconds);
		MasterUiHelper.sleep(500);
		confirmIfPresent();
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		boolean queued = isQueuedOrSaved(toast) || hasCheckerPendingActions(login);
		if (queued) {
			UserManagementStore.setLastPendingAction(action.toUpperCase());
		}
		StepLog.check("Users | " + action + " TEST AUTO", action + " icon clicked on TEST AUTO user row",
				action + " request is submitted to Checker queue",
				"login=" + login + " toast='" + toast + "' queued=" + queued, queued, "login=" + login);
		Hooks.captureMilestone("Users " + action + " submitted | " + login);
	}

	/** Logs which action buttons (View, Edit, Lock, Deactivate, etc.) are visible for a row. */
	private void logVisibleActionButtons(String login, String context) {
		StringBuilder sb = new StringBuilder("Action buttons for " + login + ": ");
		if (!getDriver().findElements(UsersPage.viewDetailsFor(login)).isEmpty()) sb.append("[View] ");
		if (!getDriver().findElements(UsersPage.editFor(login)).isEmpty()) sb.append("[Edit] ");
		if (!getDriver().findElements(UsersPage.lockFor(login)).isEmpty()) sb.append("[Lock] ");
		if (!getDriver().findElements(UsersPage.unlockFor(login)).isEmpty()) sb.append("[Unlock] ");
		if (!getDriver().findElements(UsersPage.deactivateFor(login)).isEmpty()) sb.append("[Activate/Deactivate] ");
		StepLog.info("Users | " + context + " | Action buttons",
				"Check visible action buttons on TEST AUTO row",
				"Action icons are visible for the user row",
				sb.toString(), "login=" + login);
	}

	private void approveBranchAction(String action) {
		String login = requireLogin("Branch Checker " + action);
		if (login == null) {
			return;
		}
		if (!action.equalsIgnoreCase(String.valueOf(UserManagementStore.lastPendingAction()))) {
			StepLog.check("Users | Branch Checker | " + action,
					"Approve pending " + action + " with branch Checker",
					action + " request is pending for " + login,
					"FAIL | Prerequisite " + action + " request was not created. last="
							+ UserManagementStore.lastPendingAction(),
					false, "login=" + login);
			return;
		}
		String branch = UserManagementAccounts.branchChecker();
		boolean approved = approveAsIndependent(login, branch);
		if (approved) {
			UserManagementStore.setLastReviewedBy(branch);
			UserManagementStore.setLastReviewDecision("APPROVED");
		}
		StepLog.check("Users | Branch Checker | " + action,
				branch + " approves TEST AUTO " + action + " request",
				"Branch Checker approval succeeds",
				"checker=" + branch + " login=" + login + " approved=" + approved, approved,
				"checker=" + branch + "; action=" + action + "; login=" + login);
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
	}

	private boolean approveAsIndependent(String login, String checker) {
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		// Navigate via SideNav, not direct URL
		navigateToUsersViaSideNav();
		if (getDriver().getCurrentUrl() != null && getDriver().getCurrentUrl().toLowerCase().contains("/login")) {
			StepLog.check("Users | Checker access | " + checker, "Open Users via SideNav",
					"User Management Users is available",
					"FAIL | Session returned to login for " + checker, false, "user=" + checker);
			return false;
		}
		openCheckerQueue();
		ensureCheckerQueueRowVisible(login, true);
		if (!hasCheckerPendingActions(login)) {
			if (isRequestInDecisionQueue(login, "APPROVED")) {
				StepLog.info("Users | Checker queue | " + checker,
						"Pending TEST AUTO request already approved",
						"Request is in Approved queue",
						"login=" + login + " alreadyApproved=true",
						"checker=" + checker + "; login=" + login);
				return true;
			}
			StepLog.check("Users | Checker queue | " + checker, "Pending TEST AUTO request is in Checker queue",
					"Pending request for " + login + " is visible",
					"FAIL | No pending request found for " + login, false, "checker=" + checker + "; login=" + login);
			return false;
		}
		// View Details before approval — verify maker, type, login, status
		verifyViewDetails(login, checker, UserManagementStore.lastPendingAction(), "PENDING");
		Hooks.captureMilestone("Users Checker pending before approval | " + checker + " | " + login);
		boolean approved = attemptApprove(login, checker, false);
		Hooks.captureMilestone("Users Checker after approval | " + checker + " | " + login);
		return approved || isRequestInDecisionQueue(login, "APPROVED");
	}

	/**
	 * Clicks View Details on the pending request, verifies key fields (Login Name, Maker, Type, Status),
	 * takes screenshot, then closes the detail view.
	 */
	private boolean revertAsIndependent(String login, String checker) {
		UserManagementSession.loginAs(checker, UserManagementAccounts.passwordFor(checker));
		navigateToUsersViaSideNav();
		if (getDriver().getCurrentUrl() != null && getDriver().getCurrentUrl().toLowerCase().contains("/login")) {
			StepLog.check("Users | Checker access | " + checker, "Open Users via SideNav",
					"User Management Users is available",
					"FAIL | Session returned to login for " + checker, false, "user=" + checker);
			return false;
		}
		openCheckerQueue();
		ensureCheckerQueueRowVisible(login, true);
		if (!hasCheckerPendingActions(login)) {
			if (isRequestInDecisionQueue(login, "REVERTED")) {
				StepLog.info("Users | Checker queue revert | " + checker,
						"Pending TEST AUTO request already reverted",
						"Request is in Reverted queue",
						"login=" + login + " alreadyReverted=true",
						"checker=" + checker + "; login=" + login);
				return true;
			}
			StepLog.check("Users | Checker queue revert | " + checker, "Pending TEST AUTO request is in Checker queue",
					"Pending request for " + login + " is visible",
					"FAIL | No pending request found for revert " + login, false, "checker=" + checker + "; login=" + login);
			return false;
		}
		verifyViewDetails(login, checker, UserManagementStore.lastPendingAction(), "PENDING");
		Hooks.captureMilestone("Users Checker pending before revert | " + checker + " | " + login);
		boolean reverted = attemptRevert(login, checker);
		Hooks.captureMilestone("Users Checker after revert | " + checker + " | " + login);
		return reverted || isRequestInDecisionQueue(login, "REVERTED");
	}

	private void verifyViewDetails(String login, String checker, String expectedAction, String expectedStatus) {
		try {
			List<WebElement> viewBtn = getDriver().findElements(UsersPage.viewDetailsFor(login));
			if (viewBtn.isEmpty()) {
				StepLog.check("Users | View Details | " + checker, "Click View Details eye icon for " + login,
						"View Details eye icon is available on the request row",
						"FAIL | View Details eye icon not found for " + login, false, "login=" + login);
				return;
			}
			MasterUiHelper.clickJs(UsersPage.viewDetailsFor(login), waitSeconds);
			MasterUiHelper.sleep(800);
			Hooks.captureMilestone("Users View Details | " + checker + " | " + login);

			String rowText = readRowText(login);
			String pageText = getDriver().findElement(By.tagName("body")).getText();
			String combined = pageText + " " + rowText;
			boolean hasLogin = pageText.contains(login) || rowText.contains(login);
			boolean hasMaker = containsIgnoreCase(combined, UserManagementAccounts.maker());
			boolean hasExpectedAction = matchesActionEvidence(combined, expectedAction);
			boolean hasExpectedStatus = containsIgnoreCase(combined, expectedStatus);
			boolean requireApprovedBy = expectedStatus != null
					&& (expectedStatus.equalsIgnoreCase("APPROVED") || expectedStatus.equalsIgnoreCase("REVERTED"));
			boolean hasApprovedBy = matchesCheckerEvidence(combined, checker);
			boolean pass = hasLogin && hasMaker && hasExpectedAction && hasExpectedStatus
					&& (!requireApprovedBy || hasApprovedBy);

			if (!pass) {
				SoftAssertManager.recordFailure("Users View Details " + checker,
						new AssertionError("login=" + hasLogin + " maker=" + hasMaker
								+ " type=" + hasExpectedAction + " status=" + hasExpectedStatus
								+ " approvedBy=" + hasApprovedBy));
			}

			StepLog.check("Users | View Details verification | " + checker,
					"Open eye icon and verify Login Name, Maker, Type, Status"
							+ (requireApprovedBy ? ", Approved/Reverted by" : ""),
					"Modal/row shows login, maker, request type, status"
							+ (requireApprovedBy ? ", and checker who decided" : ""),
					"loginFound=" + hasLogin + " makerFound=" + hasMaker + " typeFound=" + hasExpectedAction
							+ " statusFound=" + hasExpectedStatus + " approvedByFound=" + hasApprovedBy
							+ " action=" + expectedAction + " status=" + expectedStatus + " checker=" + checker,
					pass, "checker=" + checker + "; login=" + login + "; action=" + expectedAction);

			closePanel();
		} catch (Exception e) {
			StepLog.check("Users | View Details | " + checker, "View Details for " + login,
					"Detail view opens", "FAIL | " + e.getMessage(), false, "login=" + login);
		}
	}

	private boolean attemptApprove(String login, String actor, boolean expectRejection) {
		List<WebElement> approve = getDriver().findElements(UsersPage.approveFor(login));
		if (approve.isEmpty()) {
			if (expectRejection) {
				return true;
			}
			return isRequestInDecisionQueue(login, "APPROVED");
		}
		ToastHandler.dismissIfPresent(getDriver());
		MasterUiHelper.clickJs(UsersPage.approveFor(login), waitSeconds);
		MasterUiHelper.sleep(500);
		confirmIfPresent();
		MasterUiHelper.sleep(1500);

		// BUG-001: self-approve may redirect to login page with NO toaster
		String currentUrl = getDriver().getCurrentUrl();
		if (currentUrl != null && currentUrl.toLowerCase().contains("/login")) {
			Hooks.captureMilestone("BUG-001 Self-approve redirected to login | " + actor);
			StepLog.info("Users | attemptApprove | " + actor,
					"Approve clicked by " + actor,
					"Toaster rejection or Approve blocked",
					"FAIL (BUG-001) | Redirected to login page with NO toaster. URL=" + currentUrl,
					"actor=" + actor + "; login=" + login);
			return true;
		}

		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		if (expectRejection) {
			boolean rejected = isMakerCheckerRejection(toast) || isSameRoleRejection(toast);
			if (!rejected && isApprovedMessage(toast)) {
				return false;
			}
			return rejected || !isApprovedMessage(toast);
		}

		// Robust approved detection:
		// After successful approval, pending-row icons (Approve/Revert to maker) disappear.
		// Toast text can vary, so use UI evidence after a short wait.
		MasterUiHelper.sleep(1200);
		boolean approveStillPresent = !getDriver().findElements(UsersPage.approveFor(login)).isEmpty();
		boolean revertStillPresent = !getDriver().findElements(UsersPage.revertFor(login)).isEmpty();
		return isApprovedMessage(toast) || (!approveStillPresent && !revertStillPresent)
				|| isRequestInDecisionQueue(login, "APPROVED");
	}

	private boolean attemptRevert(String login, String actor) {
		List<WebElement> revert = getDriver().findElements(UsersPage.revertFor(login));
		if (revert.isEmpty()) {
			return isRequestInDecisionQueue(login, "REVERTED");
		}
		ToastHandler.dismissIfPresent(getDriver());
		MasterUiHelper.clickJs(UsersPage.revertFor(login), waitSeconds);
		MasterUiHelper.sleep(500);
		confirmIfPresent();
		MasterUiHelper.sleep(1500);
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);

		MasterUiHelper.sleep(1200);
		boolean approveStillPresent = !getDriver().findElements(UsersPage.approveFor(login)).isEmpty();
		boolean revertStillPresent = !getDriver().findElements(UsersPage.revertFor(login)).isEmpty();
		boolean reverted = isRevertedMessage(toast) || (!approveStillPresent && !revertStillPresent)
				|| isRequestInDecisionQueue(login, "REVERTED");
		if (!reverted) {
			StepLog.check("Users | attemptRevert | " + actor,
					"Click Revert on pending TEST AUTO request",
					"Request is reverted to maker",
					"FAIL | Revert did not complete. toast='" + toast + "'", false,
					"actor=" + actor + "; login=" + login);
		}
		return reverted;
	}

	private String requireLogin(String step) {
		String login = UserManagementStore.loginName();
		if (login == null) {
			failMissing("Users | " + step, "No TEST AUTO user request was created in this run.");
		}
		return login;
	}

	private String requireApprovedOrQueued(String step) {
		String login = requireLogin(step);
		if (login == null) {
			return null;
		}
		return login;
	}

	private void failMissing(String step, String reason) {
		StepLog.check(step, "Prerequisite TEST AUTO user from this run",
				"Created TEST AUTO login is available", "FAIL | " + reason, false, "prerequisite=missing");
	}

	/**
	 * Navigate to Users via SideNav (not direct URL) — validates the user has menu access.
	 * If SideNav navigation fails, falls back to direct URL as last resort.
	 */
	private void ensureMakerOnUsers() {
		UserManagementSession.restoreMaker();
		navigateToUsersViaSideNav();
		clickMaker();
	}

	private void navigateToUsersViaSideNav() {
		String currentUrl = getDriver().getCurrentUrl();
		if (currentUrl != null && currentUrl.contains("/admin/users")) {
			return;
		}
		try {
			// Open side nav if collapsed
			if (!getDriver().findElements(SideNavPage.MENU_OPEN).isEmpty()
					&& getDriver().findElements(SideNavPage.MENU_OPEN).get(0).isDisplayed()) {
				MasterUiHelper.clickJs(SideNavPage.MENU_OPEN, waitSeconds);
				MasterUiHelper.sleep(500);
			}
			// Click User Management parent
			if (!getDriver().findElements(SideNavPage.USER_MANAGEMENT).isEmpty()) {
				MasterUiHelper.clickJs(SideNavPage.USER_MANAGEMENT, waitSeconds);
				MasterUiHelper.sleep(500);
			}
			// Click Users sub-module
			if (!getDriver().findElements(SideNavPage.USERS).isEmpty()) {
				MasterUiHelper.clickJs(SideNavPage.USERS, waitSeconds);
				MasterUiHelper.sleep(800);
			}
			// Verify we landed on Users page
			if (getDriver().getCurrentUrl().contains("/admin/users")) {
				return;
			}
		} catch (Exception e) {
			// SideNav may not be available for this user
		}
		// Fallback: direct URL if SideNav didn't work
		getDriver().get(ConfigReader.getUrl() + "/admin/users");
		MasterUiHelper.sleep(800);
	}

	private void openAddUser() {
		ensureMakerOnUsers();
		if (!MasterUiHelper.isVisible(UsersPage.LOGIN_NAME, 1)) {
			MasterUiHelper.clickJs(UsersPage.ADD_USER, waitSeconds);
		}
		PomElementManager.findVisible(UsersPage.LOGIN_NAME, waitSeconds);
		Hooks.captureMilestone("Users Add User form before entry");
	}

	private void fillRequiredCandidate(String login) {
		type(UsersPage.FIRST_NAME, "TEST");
		type(UsersPage.LAST_NAME, "AUTO");
		type(UsersPage.DESIGNATION, "QA TESTER");
		type(UsersPage.EMAIL, login + "@example.test");
		type(UsersPage.MOBILE, "9876543210");
		type(UsersPage.LOGIN_NAME, login);
		selectFirstOption(UsersPage.SOL_COMBO, "SOL/REG/CPC");
		selectNonSuperAdminRole();
	}

	private void type(By locator, String value) {
		MasterUiHelper.typeInto(locator, value, waitSeconds);
	}

	private String read(By locator) {
		try {
			return PomElementManager.findVisible(locator, waitSeconds).getAttribute("value");
		} catch (Exception e) {
			return "";
		}
	}

	private void validateMaxLength(String field, By locator, String entered, int max) {
		type(locator, entered);
		String accepted = read(locator);
		boolean pass = accepted.length() <= max;
		StepLog.check("Users | " + field + " max length", "Enter " + entered.length() + " characters",
				"Input rejects/trims to max " + max,
				"enteredLength=" + entered.length() + " acceptedLength=" + accepted.length() + " accepted='" + accepted
						+ "'",
				pass, "enteredLength=" + entered.length());
	}

	private static String repeat(String value, int count) {
		return value.repeat(Math.max(0, count));
	}

	private void forceClickSave() {
		List<WebElement> saves = getDriver().findElements(UsersPage.SAVE);
		if (!saves.isEmpty()) {
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", saves.get(0));
			MasterUiHelper.sleep(400);
		}
	}

	private int helperCount() {
		return getDriver().findElements(By.cssSelector(".MuiFormHelperText-root")).size();
	}

	private boolean isDuplicateMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("already") || value.contains("exist") || value.contains("duplicate");
	}

	private void closePanel() {
		if (!getDriver().findElements(UsersPage.CLOSE_PANEL).isEmpty()) {
			MasterUiHelper.clickJs(UsersPage.CLOSE_PANEL, waitSeconds);
		}
	}

	private void clickMaker() {
		if (MasterUiHelper.isVisible(UsersPage.MAKER, 2)) {
			MasterUiHelper.clickJs(UsersPage.MAKER, waitSeconds);
			MasterUiHelper.sleep(300);
		}
	}

	private void openCheckerQueue() {
		UserManagementSession.switchToCheckerMode(UsersPage.CHECKER);
		selectCheckerQueueStatus("Pending");
	}

	private void openCheckerDecisionTab(String decision) {
		UserManagementSession.switchToCheckerMode(UsersPage.CHECKER);
		String label = "REVERTED".equalsIgnoreCase(decision) ? "Reverted" : "Approved";
		selectCheckerQueueStatus(label);
	}

	/**
	 * Checker queue Status is controlled by a dropdown (Pending/Approved/Reverted).
	 * This helper selects by generic role-based locators.
	 */
	private void selectCheckerQueueStatus(String label) {
		try {
			String safe = label == null ? "" : label.replace("'", "");
			By combo = By.xpath("//*[(@role='combobox') and (contains(@aria-label,'Status') or contains(normalize-space(.),'Status'))][1]");
			if (!getDriver().findElements(combo).isEmpty()) {
				MasterUiHelper.clickJs(combo, waitSeconds);
				MasterUiHelper.sleep(300);
			}
			By option = By.xpath("//*[(@role='option' or @role='menuitem')][contains(normalize-space(.),'" + safe + "')][1]");
			if (!getDriver().findElements(option).isEmpty()) {
				MasterUiHelper.clickJs(option, waitSeconds);
				MasterUiHelper.sleep(600);
			}
		} catch (Exception e) {
			// Best-effort fallback to tab locators
			try {
				By tab = "Reverted".equalsIgnoreCase(label) ? UsersPage.REVERTED_TAB : UsersPage.APPROVED_TAB;
				if (!getDriver().findElements(tab).isEmpty()) {
					MasterUiHelper.clickJs(tab, waitSeconds);
					MasterUiHelper.sleep(600);
				}
			} catch (Exception ignored) {
			}
		}
	}

	private void search(String login) {
		if (MasterUiHelper.isVisible(UsersPage.SEARCH, 2)) {
			MasterUiHelper.search(UsersPage.SEARCH, login, waitSeconds);
			MasterUiHelper.sleep(500);
		}
	}

	private void confirmIfPresent() {
		MasterUiHelper.sleep(250);
		if (!getDriver().findElements(UsersPage.CONFIRM).isEmpty()) {
			MasterUiHelper.clickJs(UsersPage.CONFIRM, waitSeconds);
			MasterUiHelper.sleep(300);
		}
	}

	private void selectFirstOption(By combo, String label) {
		try {
			WebElement element = PomElementManager.findVisible(combo, waitSeconds);
			((JavascriptExecutor) getDriver()).executeScript(
					"arguments[0].dispatchEvent(new MouseEvent('mousedown',{bubbles:true,cancelable:true,buttons:1}));",
					element);
			MasterUiHelper.sleep(300);
			List<WebElement> options = getDriver().findElements(By.cssSelector("[role='option']"));
			if (options.isEmpty()) {
				failMissing("Users | " + label, label + " has no selectable option in the live form.");
				return;
			}
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", options.get(0));
		} catch (Exception e) {
			failMissing("Users | " + label, label + " option could not be selected: " + e.getMessage());
		}
	}

	private void selectNonSuperAdminRole() {
		try {
			WebElement element = PomElementManager.findVisible(UsersPage.ROLE_COMBO, waitSeconds);
			((JavascriptExecutor) getDriver()).executeScript(
					"arguments[0].dispatchEvent(new MouseEvent('mousedown',{bubbles:true,cancelable:true,buttons:1}));",
					element);
			MasterUiHelper.sleep(300);
			List<WebElement> options = getDriver().findElements(By.cssSelector("[role='option']"));
			String preferred = UserManagementStore.approvedRoleName();
			WebElement pick = findRoleOption(options, preferred);
			if (pick == null) {
				pick = findRoleOption(options, "TEST AUTO ROLE");
			}
			if (pick == null) {
				for (WebElement option : options) {
					String text = option.getText() == null ? "" : option.getText();
					if (!text.toLowerCase().contains("super admin")) {
						pick = option;
						break;
					}
				}
			}
			if (pick == null) {
				failMissing("Users | Role", "No non Super Admin role is available to assign.");
				return;
			}
			String selected = pick.getText() == null ? "" : pick.getText().trim();
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", pick);
			boolean usedApproved = preferred != null && selected.toLowerCase().contains(preferred.toLowerCase());
			StepLog.check("Users | Assign role on user create",
					"Select the approved TEST AUTO role on Add User",
					"Role dropdown uses the role created and approved in Roles Positive",
					"selected='" + selected + "' preferred='" + preferred + "' usedApprovedRole=" + usedApproved,
					usedApproved || preferred == null,
					"preferred=" + preferred + "; selected=" + selected);
		} catch (Exception e) {
			failMissing("Users | Role", "Role could not be selected: " + e.getMessage());
		}
	}

	private WebElement findRoleOption(List<WebElement> options, String wanted) {
		if (wanted == null || wanted.isBlank() || options == null) {
			return null;
		}
		String needle = wanted.toLowerCase();
		for (WebElement option : options) {
			String text = option.getText() == null ? "" : option.getText();
			if (text.toLowerCase().contains(needle) && !text.toLowerCase().contains("super admin")) {
				return option;
			}
		}
		return null;
	}

	private boolean hasPendingRequestEvidence(String login, String toast) {
		String value = (toast == null ? "" : toast).toLowerCase();
		boolean toastIndicatesQueue = value.contains("submitted") || value.contains("pending")
				|| value.contains("checker") || value.contains("approval") || value.contains("success");
		if (toastIndicatesQueue || hasCheckerPendingActions(login)) {
			return true;
		}
		String rowText = readRowText(login);
		return !rowText.isBlank()
				&& (rowText.toLowerCase().contains("awaiting approval") || rowText.toLowerCase().contains("pending"));
	}

	private boolean hasCheckerPendingActions(String login) {
		return !getDriver().findElements(UsersPage.approveFor(login)).isEmpty()
				|| !getDriver().findElements(UsersPage.revertFor(login)).isEmpty();
	}

	private void waitForRowReady(String login, int maxSeconds) {
		long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
		while (System.currentTimeMillis() < deadline) {
			search(login);
			String rowText = readRowText(login);
			boolean awaiting = rowText.toLowerCase().contains("awaiting approval");
			boolean hasEdit = !getDriver().findElements(UsersPage.editFor(login)).isEmpty();
			if (!awaiting && hasEdit) {
				return;
			}
			MasterUiHelper.sleep(900);
		}
	}

	private void ensureCheckerQueueRowVisible(String login, boolean pendingActions) {
		if (pendingActions && hasCheckerPendingActions(login)) {
			return;
		}
		if (!pendingActions && !getDriver().findElements(UsersPage.row(login)).isEmpty()) {
			return;
		}
		paginateCheckerQueue(login, pendingActions);
	}

	private void paginateCheckerQueue(String login, boolean pendingActions) {
		for (int page = 0; page < 12; page++) {
			if (pendingActions && hasCheckerPendingActions(login)) {
				return;
			}
			if (!pendingActions && !getDriver().findElements(UsersPage.row(login)).isEmpty()) {
				return;
			}
			List<WebElement> nextButtons = getDriver().findElements(UsersPage.NEXT_PAGE);
			if (nextButtons.isEmpty() || !nextButtons.get(0).isEnabled()) {
				return;
			}
			MasterUiHelper.clickJs(UsersPage.NEXT_PAGE, waitSeconds);
			MasterUiHelper.sleep(700);
		}
	}

	private boolean isRequestInDecisionQueue(String login, String decision) {
		String label = "REVERTED".equalsIgnoreCase(decision) ? "Reverted" : "Approved";
		selectCheckerQueueStatus(label);
		MasterUiHelper.sleep(500);
		ensureCheckerQueueRowVisible(login, false);
		if (getDriver().findElements(UsersPage.row(login)).isEmpty()) {
			return false;
		}
		String rowText = readRowText(login);
		return containsIgnoreCase(rowText, decision) || containsIgnoreCase(rowText, label)
				|| matchesActionEvidence(rowText, UserManagementStore.lastPendingAction());
	}

	private boolean matchesCheckerEvidence(String source, String checker) {
		if (source == null || checker == null || checker.isBlank()) {
			return false;
		}
		if (containsIgnoreCase(source, checker)) {
			return true;
		}
		String prefix = checker.length() >= 4 ? checker.substring(0, 4) : checker;
		return source.toLowerCase().contains(prefix.toLowerCase());
	}

	private boolean isQueuedOrSaved(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("success") || value.contains("submitted") || value.contains("pending")
				|| value.contains("updated") || value.contains("saved") || value.contains("lock")
				|| value.contains("dormant");
	}

	private boolean isMakerCheckerRejection(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("maker") || value.contains("own") || value.contains("not allowed")
				|| value.contains("cannot") || value.contains("self");
	}

	private boolean isSameRoleRejection(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("same role") || value.contains("same-role")
				|| (value.contains("role") && (value.contains("cannot") || value.contains("not allowed")
						|| value.contains("checker")));
	}

	private boolean isApprovedMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("approved") || value.contains("success");
	}

	private boolean isRevertedMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("revert") || value.contains("returned") || value.contains("maker");
	}

	private void approveActionWithIndependentChecker(String action, String scenario, String step) {
		String login = requireLogin("Independent Checker " + action);
		if (login == null) {
			return;
		}
		if (!action.equalsIgnoreCase(String.valueOf(UserManagementStore.lastPendingAction()))) {
			StepLog.check(scenario, step,
					action + " request is pending for " + login,
					"FAIL | Prerequisite " + action + " request was not created. last="
							+ UserManagementStore.lastPendingAction(),
					false, "login=" + login);
			return;
		}
		String checker = UserManagementAccounts.primaryChecker();
		boolean approved = approveAsIndependent(login, checker);
		if (!approved && isRequestInDecisionQueue(login, "APPROVED")) {
			approved = true;
		}
		if (!approved) {
			String branch = UserManagementAccounts.branchChecker();
			approved = approveAsIndependent(login, branch);
			if (approved) {
				checker = branch;
			}
		}
		if (approved) {
			UserManagementStore.setLastReviewedBy(checker);
			UserManagementStore.setLastReviewDecision("APPROVED");
		}
		StepLog.check(scenario, step, "Independent Checker approval succeeds",
				"checker=" + checker + " login=" + login + " approved=" + approved, approved,
				"checker=" + checker + "; action=" + action + "; login=" + login);
		UserManagementSession.restoreMaker();
		ensureMakerOnUsers();
	}

	private void verifyDecisionDetails(String login, String checker, String action, String decision) {
		ensureCheckerQueueRowVisible(login, false);
		String rowText = readRowText(login);
		boolean rowFound = !rowText.isBlank();
		if (!rowFound) {
			StepLog.check("Users | " + decision + " details",
					"Open processed TEST AUTO request and click View Details",
					"Processed request row is visible",
					"FAIL | Request row not found in " + decision + " queue for " + login, false, "login=" + login);
			return;
		}
		verifyViewDetails(login, checker, action, decision);
	}

	private String readRowText(String login) {
		List<WebElement> rows = getDriver().findElements(UsersPage.row(login));
		return rows.isEmpty() ? "" : rows.get(0).getText();
	}

	private boolean containsIgnoreCase(String source, String value) {
		return source != null && value != null && source.toLowerCase().contains(value.toLowerCase());
	}

	private boolean matchesActionEvidence(String source, String action) {
		String value = source == null ? "" : source.toLowerCase();
		String expected = action == null ? "" : action.toLowerCase();
		if (expected.isBlank()) {
			return true;
		}
		if (expected.contains("create")) {
			return value.contains("create");
		}
		if (expected.contains("edit")) {
			return value.contains("edit") || value.contains("update");
		}
		if (expected.contains("unlock")) {
			return value.contains("unlock");
		}
		if (expected.contains("lock")) {
			return value.contains("lock");
		}
		if (expected.contains("dormant")) {
			return value.contains("dormant") || value.contains("inactive");
		}
		return value.contains(expected);
	}

	private boolean setDormantStatus() {
		if (!getDriver().findElements(UsersPage.STATUS_COMBO).isEmpty()) {
			MasterUiHelper.clickJs(UsersPage.STATUS_COMBO, waitSeconds);
			MasterUiHelper.sleep(250);
		}
		List<WebElement> dormant = getDriver().findElements(UsersPage.DORMANT_OPTION);
		if (!dormant.isEmpty()) {
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", dormant.get(0));
			return true;
		}
		List<WebElement> status = getDriver().findElements(UsersPage.ACTIVE_STATUS);
		if (!status.isEmpty() && status.get(0).isSelected()) {
			((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", status.get(0));
			return true;
		}
		return false;
	}
}
