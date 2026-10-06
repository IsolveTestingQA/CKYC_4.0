/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsFindings;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsScreenshot;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rule R2 - an invalid value was accepted AND Submit to Checker succeeded. The record is opened as Checker, all tabs
 * are visited, Maker and Checker values are compared (C1-C3), the record is reverted, and the Maker values are
 * verified again (C4). Needs checker.recovery=ON (default ON).
 */
public final class DvsCheckerFlow {

	public record Outcome(boolean reverted, boolean backInMaker, List<String> notes) {
	}

	private static final DvsLocators L = DvsLocators.get();
	private static final int REASON_MAX = 300;

	private DvsCheckerFlow() {
	}

	public static boolean enabled() {
		return DvsConfig.getBool("checker.recovery", true);
	}

	public static boolean submitSucceeded(String toast) {
		return toast != null && toast.toLowerCase(Locale.ROOT).contains("submitted successfully");
	}

	/**
	 * @param makerFull   C1 snapshot taken before Submit (key tab|Locator_ID)
	 * @param lastPositive last positive data, to verify the Maker record after revert
	 */
	public static Outcome recover(String tcId, String moduleCode, String customerId, String fieldDescription,
			Map<String, String> makerFull, DvsSnapshot lastPositive) {
		List<String> notes = new ArrayList<>();
		String prefix = "LE".equals(moduleCode) ? "LE" : "IND";
		String reason = trim("AUTO-REVERT " + tcId + ": " + fieldDescription + " accepted (bug)");

		DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", fieldDescription,
				"Invalid value submitted to checker", DvsScreenshot.capture(tcId, "submitted"));

		DvsNavigation.selectChecker();
		DvsNavigation.selectModule(moduleCode);
		DvsNavigation.openCustomer(customerId);

		List<String> tabs = visitAllTabs(notes);
		compareWithMaker(tcId, customerId, prefix, makerFull, notes);

		List<String> editable = DvsRestoreManager.editableFields(prefix);
		if (!editable.isEmpty()) {
			String detail = "C3: fields editable in Checker: " + editable;
			DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", "", detail, DvsScreenshot.capture(tcId, "checker-editable"));
			notes.add(detail);
		}

		boolean reverted = revert(tcId, customerId, reason, tabs, notes);

		DvsNavigation.selectMaker();
		DvsNavigation.selectModule(moduleCode);
		boolean back = DvsNavigation.isInQueue(customerId);
		if (!back) {
			DvsStepLog.step("r3-retry").field(customerId).actual("not in Maker queue after revert - retry once")
					.result(DvsStepLog.RETRY).log();
			DvsFieldActions.sleep(1500);
			back = DvsNavigation.isInQueue(customerId);
		}
		if (!back) {
			notes.add("R3: customer " + customerId + " not back in Maker queue after revert");
			DvsStepLog.step("r3-blocked").field(customerId).actual(notes.get(notes.size() - 1)).result(DvsStepLog.FAIL).log();
			return new Outcome(reverted, false, notes);
		}
		DvsNavigation.openCustomer(customerId);
		verifyMakerAfterRevert(tcId, customerId, prefix, reason, makerFull, notes);
		return new Outcome(reverted, true, notes);
	}

	/** Clicks every record tab (each shows a "... Tab Updated" toast) and returns the tab names visited. */
	public static List<String> visitAllTabs(List<String> notes) {
		List<String> visited = new ArrayList<>();
		for (String tab : DvsFieldActions.recordTabNames()) {
			DvsFieldActions.openTab(tab);
			String toast = DvsFieldActions.waitForToast("Updated", 3);
			visited.add(tab);
			notes.add("tab " + tab + ": " + (toast.isEmpty() ? "no toast" : toast));
		}
		return visited;
	}

	private static void compareWithMaker(String tcId, String customerId, String prefix, Map<String, String> maker,
			List<String> notes) {
		if (maker == null || maker.isEmpty()) {
			notes.add("C1 Maker snapshot missing - compare skipped");
			return;
		}
		Map<String, String> checker = DvsRestoreManager.captureFull(prefix);
		List<String> diffs = DvsRestoreManager.compareFull(maker, checker);
		DvsStepLog.step("compare-maker-checker").actual(diffs.isEmpty() ? "identical (" + maker.size() + " items)" : diffs.size() + " differences")
				.result(diffs.isEmpty() ? DvsStepLog.PASS : DvsStepLog.FAIL).log();
		for (String d : diffs) {
			DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", d.substring(0, Math.max(0, d.indexOf(':'))),
					"Maker->Checker mismatch: " + d, DvsScreenshot.capture(tcId, "mismatch"));
			notes.add("mismatch " + d);
		}
	}

	private static boolean revert(String tcId, String customerId, String reason, List<String> tabs, List<String> notes) {
		for (int attempt = 1; attempt <= 2; attempt++) {
			DvsFieldActions.clickBy("COM_CHK_Revert");
			DvsFieldActions.sleep(600);
			DvsFieldActions.typeInto("COM_CHK_reasonText", reason);
			DvsFieldActions.clickBy("COM_CHK_confirmRevert");
			String toast = DvsFieldActions.waitForToast("", 4);
			DvsStepLog.step("revert").field(customerId).entered(reason).toast(toast).dialog(DvsFieldActions.dialogText())
					.result(toast.toLowerCase(Locale.ROOT).contains("reverted successfully") ? DvsStepLog.PASS : DvsStepLog.FAIL).log();
			if (toast.toLowerCase(Locale.ROOT).contains("reverted successfully")) {
				return true;
			}
			if (toast.toLowerCase(Locale.ROOT).contains("tabs are not verified")) {
				DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", "Revert reason",
						"Revert reason saved although Confirm Revert failed (tabs not verified)", DvsScreenshot.capture(tcId, "revert-blocked"));
				notes.add("revert blocked: tabs not verified, visiting all tabs again");
				DvsFieldActions.dismissDialogIfOpen();
				visitAllTabs(notes);
				continue;
			}
			notes.add("revert toast: " + toast);
			DvsFieldActions.dismissDialogIfOpen();
		}
		return false;
	}

	private static void verifyMakerAfterRevert(String tcId, String customerId, String prefix, String reason,
			Map<String, String> maker, List<String> notes) {
		DvsItemErrors.Panel panel = DvsItemErrors.read();
		boolean listed = false;
		for (String entry : panel.checker()) {
			if (entry.contains(reason) || entry.contains("AUTO-REVERT " + tcId)) {
				listed = true;
			}
		}
		if (!listed) {
			String d = "Revert reason not shown under CHECKER REVERT in the Maker Item Error List";
			DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", "Item Error List", d, DvsScreenshot.capture(tcId, "revert-reason"));
			notes.add(d);
		}
		if (BaseClass.getDriver().findElements(L.by("COM_STATUS_draft")).isEmpty()) {
			String d = "Status 'Draft' not shown after revert";
			DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", "Status", d, DvsScreenshot.capture(tcId, "status"));
			notes.add(d);
		}
		if (maker != null && !maker.isEmpty()) {
			Map<String, String> now = DvsRestoreManager.captureFull(prefix);
			List<String> diffs = DvsRestoreManager.compareFull(maker, now);
			DvsStepLog.step("c4-maker-after-revert").actual(diffs.isEmpty() ? "values equal the C1 snapshot" : diffs.size() + " differences")
					.result(diffs.isEmpty() ? DvsStepLog.PASS : DvsStepLog.FAIL).log();
			for (String d : diffs) {
				DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", "", "After revert Maker value changed: " + d,
						DvsScreenshot.capture(tcId, "c4"));
				notes.add("C4 " + d);
			}
		}
	}

	private static String trim(String s) {
		return s.length() <= REASON_MAX ? s : s.substring(0, REASON_MAX);
	}
}
