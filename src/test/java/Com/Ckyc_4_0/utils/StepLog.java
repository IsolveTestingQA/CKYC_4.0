/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;

/**
 * Logs a Masters check to Excel (test data, toast, named failure screenshot) and records soft failures.
 */
public final class StepLog {

	private StepLog() {
	}

	public static void check(String scenario, String step, String expected, String actual, boolean passed,
			String testData) {
		// Poll briefly — a MUI snackbar can auto-dismiss within 2-3s; an instant read can miss it.
		String toast = ToastHandler.waitAndRead(Com.Ckyc_4_0.UtilityFiles.BaseClass.getDriver(), 2000);
		Hooks.logStepWithDetails(scenario, step, expected, actual, passed, testData, toast);
		String detail = scenario + " | " + step + " | Expected: " + expected
				+ " | Actual: " + actual + " | TestData: " + testData
				+ (toast == null || toast.isBlank() ? "" : " | Toast: " + toast);
		if (passed) {
			ExecutionLogger.pass(detail);
		} else {
			ExecutionLogger.fail(detail);
		}
		if (!passed) {
			SoftAssertManager.recordFailure(step, new AssertionError(actual));
		}
	}

	public static void info(String scenario, String step, String expected, String actual, String testData) {
		Hooks.logStepWithDetails(scenario, step, expected, actual, true, testData, "");
		ExecutionLogger.info(scenario + " | " + step + " | Expected: " + expected
				+ " | Actual: " + actual + " | TestData: " + testData);
	}
}
