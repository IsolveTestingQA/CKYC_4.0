/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

/** Run-only identifiers; never authorizes mutations of pre-existing users or roles. */
public final class UserManagementStore {
	private static String loginName;
	private static String roleName;
	private static String roleCode;
	private static String currentUser;
	private static boolean userApproved;
	private static boolean roleApproved;
	private static String lastPendingAction;
	private static String lastReviewedBy;
	private static String lastReviewDecision;
	/** Survives scenario clear so Users Positive can assign the role created in Roles Positive. */
	private static String approvedRoleName;
	private static String approvedRoleCode;

	private UserManagementStore() {}

	public static String uniqueLogin() {
		return "testauto" + (System.currentTimeMillis() % 100_000_000L);
	}

	public static String uniqueRole() {
		return "TEST AUTO ROLE " + Long.toString(System.currentTimeMillis() % 1_000_000L, 36).toUpperCase();
	}

	public static String uniqueRoleCode() {
		return "TA" + (System.currentTimeMillis() % 1_000_000L);
	}

	public static void setLoginName(String value) { loginName = value; }
	public static String loginName() { return loginName; }
	public static void setRoleName(String value) { roleName = value; }
	public static String roleName() { return roleName; }
	public static void setRoleCode(String value) { roleCode = value; }
	public static String roleCode() { return roleCode; }
	public static void setCurrentUser(String value) { currentUser = value; }
	public static String currentUser() { return currentUser; }
	public static void setUserApproved(boolean value) { userApproved = value; }
	public static boolean userApproved() { return userApproved; }
	public static void setRoleApproved(boolean value) { roleApproved = value; }
	public static boolean roleApproved() { return roleApproved; }
	public static void setLastPendingAction(String value) { lastPendingAction = value; }
	public static String lastPendingAction() { return lastPendingAction; }
	public static void setLastReviewedBy(String value) { lastReviewedBy = value; }
	public static String lastReviewedBy() { return lastReviewedBy; }
	public static void setLastReviewDecision(String value) { lastReviewDecision = value; }
	public static String lastReviewDecision() { return lastReviewDecision; }
	public static void setApprovedRoleName(String value) { approvedRoleName = value; }
	public static String approvedRoleName() { return approvedRoleName; }
	public static void setApprovedRoleCode(String value) { approvedRoleCode = value; }
	public static String approvedRoleCode() { return approvedRoleCode; }

	public static void clear() {
		loginName = null;
		roleName = null;
		roleCode = null;
		currentUser = null;
		userApproved = false;
		roleApproved = false;
		lastPendingAction = null;
		lastReviewedBy = null;
		lastReviewDecision = null;
	}
}
