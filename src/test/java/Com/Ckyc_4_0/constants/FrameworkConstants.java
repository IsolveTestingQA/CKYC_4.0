/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.constants;

/**
 * Product-level CKYC 4.0.0 framework constants (timeouts, paths, labels).
 */
public final class FrameworkConstants {

	private FrameworkConstants() {
	}

	public static final String APPLICATION_NAME = "iFlowCKYC 4.0.0";
	public static final String PRODUCT_VERSION = "4.0.0";

	/** Short wait when optional dialogs may or may not appear after Sign In. */
	public static final int OPTIONAL_DIALOG_WAIT_SECONDS = 5;

	/** Wait for SweetAlert / blocker overlay to dismiss before retrying a step. */
	public static final int BLOCKER_WAIT_SECONDS = 8;

	/** How long to wait for human Stop/Retry on the interactive blocker dialog (seconds). */
	public static final int INTERACTIVE_BLOCKER_WAIT_SECONDS = 300;
	/** Seconds to wait after user chooses Continue for a manual UI fix. */
	public static final int MANUAL_FIX_WAIT_SECONDS = 60;

	public static final String SESSION_ACTIVE_DIALOG_TITLE_ID = "session-active-dialog-title";
	public static final String SESSION_CONTINUE_BUTTON_TEXT = "Log out other session";
}
