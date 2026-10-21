/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsFindings;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsRow;
import Com.Ckyc_4_0.utils.dvs.DvsScenarioLoader;
import Com.Ckyc_4_0.utils.dvs.DvsScreenshot;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;
import Com.Ckyc_4_0.utils.dvs.DvsTypedValues;

import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriverException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes ONE data row. A row is run only when its Field resolves to a locator and its Input_Value is a typed
 * value. Anything else is reported NOT RUN with the reason - never guessed.
 * Extra checks per row: T1/T2 tab-switch persistence, Submit rows with R2 Checker recovery, E1-E3, S1, step log.
 */
public final class DvsRowExecutor {

	/** Thrown when the browser / session is gone: the engine stops the run. */
	public static final class RunAbort extends RuntimeException {
		public RunAbort(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final DvsLocators L = DvsLocators.get();
	private static final Pattern NEGATIVE = Pattern.compile(
			"^(rejected|blocked|submit blocked|error|not accepted|cannot|must be|invalid|warning)", Pattern.CASE_INSENSITIVE);
	private static final Pattern POSITIVE = Pattern.compile(
			"^(accepted|allowed|saved|valid|retained|selectable|no error)", Pattern.CASE_INSENSITIVE);
	private static final Pattern DATE_TOKEN = Pattern.compile("^(today|yesterday|tomorrow)([+-]\\d+)?$", Pattern.CASE_INSENSITIVE);
	private static final Map<String, String> TAB_LABELS = Map.of(
			"personal", "Personal Details",
			"poi", "Identity / POI",
			"contact", "Contact & Address",
			"address", "Contact & Address",
			"related person", "Related Persons");

	private final boolean dryRun;
	private final Set<String> bugLocked;
	private String customerId = "";
	private final Set<String> touched = new LinkedHashSet<>();
	private boolean customerBlocked;
	private String blockedReason = "";
	private DvsSnapshot lastPositive = new DvsSnapshot();

	public DvsRowExecutor(boolean dryRun, Set<String> bugLocked) {
		this.dryRun = dryRun;
		this.bugLocked = bugLocked;
	}

	public void customer(String id) {
		this.customerId = id;
	}

	public void lastPositive(DvsSnapshot snapshot) {
		this.lastPositive = snapshot;
	}

	/** True after an R3 situation: the customer did not come back to the Maker queue. */
	public boolean customerBlocked() {
		return customerBlocked;
	}

	public String blockedReason() {
		return blockedReason;
	}

	/** Locator ids touched since the last call (field + connected field), then cleared. */
	public Set<String> drainTouched() {
		Set<String> copy = new LinkedHashSet<>(touched);
		touched.clear();
		return copy;
	}

	public void execute(DvsRow row) {
		DvsStepLog.tc(row.tcId());
		if (DvsScenarioLoader.SHEET_IND.equals(row.sheet()) || DvsScenarioLoader.SHEET_LE.equals(row.sheet())) {
			executeSuite(row);
			return;
		}
		String field = row.field();
		if (field.toLowerCase(Locale.ROOT).endsWith(" tab")) {
			executeTabRoundTrip(row, field.substring(0, field.length() - 4).trim());
			return;
		}
		List<DvsTypedValues.Pair> typed = DvsTypedValues.get(row.tcId());
		if (!typed.isEmpty()) {
			executeTyped(row, typed);
			return;
		}
		DvsLocators.Resolution res = L.resolveField(row.moduleCode(), field);
		if (!res.ok()) {
			row.result(DvsRow.NOT_RUN, res.reason());
			return;
		}
		row.locatorId(res.id());
		String raw = row.inputValue();
		boolean openList = raw.toLowerCase(Locale.ROOT).startsWith("open") && "dropdown".equals(L.kind(res.id()));
		if (!openList && descriptive(raw)) {
			row.result(DvsRow.NOT_RUN, "Input_Value is a description, not a typed value: '" + raw
					+ "' (needs structured test data or a FieldMap entry)");
			return;
		}
		String value = openList ? raw : token(raw);
		if (dryRun) {
			row.result(DvsRow.PLANNED, (openList ? "would list options of " : "would set ") + res.id()
					+ (openList ? "" : " = '" + value + "'") + flags(row));
			return;
		}
		try {
			if (openList) {
				List<String> options = DvsFieldActions.optionCodes(res.id());
				row.result(DvsRow.CAPTURED, "Options: " + String.join(" ; ", options));
				return;
			}
			runLiteral(row, res.id(), value);
		} catch (RunAbort e) {
			throw e;
		} catch (WebDriverException e) {
			if (sessionGone(e)) {
				throw new RunAbort("Browser session lost: " + firstLine(e), e);
			}
			fail(row, "Exception: " + firstLine(e));
		} catch (RuntimeException e) {
			fail(row, "Exception: " + firstLine(e));
		}
		verdictStep(row);
	}

	// ---------------------------------------------------------------- Individual / Legal Entity suite rows

	private Set<String> baselineLines = new LinkedHashSet<>();
	private Map<String, Integer> baselineBadges = new LinkedHashMap<>();
	private Map<String, String> suiteMakerFull = new LinkedHashMap<>();

	/**
	 * Before the first suite row: the tab badges and the validation messages the record ALREADY has (one Submit
	 * with no edits), so every row is judged only on what its own value adds. If that Submit goes through (a record
	 * with no errors at all) it is sent back to Maker and the suite stops - a clean live record is never submit-tested.
	 */
	public boolean captureSuiteBaseline(String moduleCode) {
		String prefix = "LE".equals(moduleCode) ? "LE" : "IND";
		suiteMakerFull = DvsRestoreManager.captureFull(prefix);
		DvsFieldActions.dismissDialogIfOpen();
		String toast = DvsNavigation.submit();
		String dialog = DvsFieldActions.dialogText();
		if (DvsCheckerFlow.submitSucceeded(toast)) {
			DvsCheckerFlow.Outcome out = DvsCheckerFlow.recover("SUITE-BASELINE", moduleCode, customerId,
					"baseline Submit with no edits (record had no validation errors)", suiteMakerFull, lastPositive);
			customerBlocked = true;
			blockedReason = "Record " + customerId + " has no validation errors, so a Submit would be accepted: the suite did not "
					+ "run on it (reverted=" + out.reverted() + ", backInMaker=" + out.backInMaker() + "). Use a record with open errors.";
			DvsStepLog.step("suite-baseline").field(customerId).actual(blockedReason).result(DvsStepLog.FAIL).log();
			return false;
		}
		baselineLines = new LinkedHashSet<>();
		for (String s : DvsFieldActions.validationLines(dialog)) {
			baselineLines.add(s.toLowerCase(Locale.ROOT));
		}
		baselineBadges = DvsFieldActions.tabBadges();
		String shot = DvsScreenshot.capture("SUITE-BASELINE", "baseline-submit");
		DvsFieldActions.closeDialog();
		DvsStepLog.step("suite-baseline").field(customerId)
				.actual(baselineLines.size() + " existing validation messages, tab badges " + baselineBadges)
				.toast(toast).dialog(oneLine(dialog)).shot(shot).result(DvsStepLog.INFO).log();
		return true;
	}

	/**
	 * One scenario: open the tab, enter the value, blur (inline message), Save (temporary save), enter it again if
	 * Save dropped it, Submit to Checker (the real validation answer: toast, error dialog, tab badge, inline message),
	 * screenshot, close the dialog. The engine restores the record afterwards.
	 */
	private void executeSuite(DvsRow row) {
		String id = row.get("Locator_ID");
		String tab = row.get("Tab");
		String expect = row.get("Expect").toUpperCase(Locale.ROOT);
		String contains = row.get("Error_Contains");
		if (!L.has(id)) {
			row.result(DvsRow.NOT_RUN, "Unknown locator " + id + " in the scenario file");
			return;
		}
		row.locatorId(id);
		String value = token(row.inputValue());
		String kind = L.kind(id);
		if (dryRun) {
			row.result(DvsRow.PLANNED, "would open '" + tab + "', set " + id + " = '" + abbreviate(value) + "', Save, Submit to Checker, expect "
					+ expect + (contains.isEmpty() ? "" : " containing '" + contains + "'") + ", then restore");
			return;
		}
		try {
			DvsFieldActions.dismissDialogIfOpen();
			DvsFieldActions.openTab(tab);
			touched.add(id);
			String before = DvsFieldActions.read(id);
			for (String pair : row.get("Setup").split(";")) {
				int eq = pair.indexOf('=');
				if (eq > 0 && L.has(pair.substring(0, eq).trim())) {
					String sid = pair.substring(0, eq).trim();
					touched.add(sid);
					DvsFieldActions.set(sid, token(pair.substring(eq + 1).trim()));
					DvsFieldActions.blur(sid);
				}
			}
			DvsFieldActions.set(id, value);
			DvsFieldActions.blur(id);
			String typed = DvsFieldActions.read(id);
			String inline = DvsFieldActions.errorText(id);

			String saveToast = DvsNavigation.save();
			String afterSave = DvsFieldActions.read(id);
			String savedNote = "";
			if (!DvsFieldActions.sameValue(kind, typed, afterSave)) {
				savedNote = " | Save changed the field: typed '" + abbreviate(typed) + "', now '" + abbreviate(afterSave) + "' - entered again before Submit";
				DvsFieldActions.set(id, value);
				DvsFieldActions.blur(id);
			}

			String submitToast = DvsNavigation.submit();
			String dialog = DvsFieldActions.dialogText();
			boolean submitted = DvsCheckerFlow.submitSucceeded(submitToast);
			List<String> newLines = new java.util.ArrayList<>();
			for (String s : DvsFieldActions.validationLines(dialog)) {
				if (!baselineLines.contains(s.toLowerCase(Locale.ROOT))) {
					newLines.add(s);
				}
			}
			Map<String, Integer> badges = DvsFieldActions.tabBadges();
			int badgeUp = badges.getOrDefault(tab, 0) - baselineBadges.getOrDefault(tab, 0);
			String inlineAfter = DvsFieldActions.errorText(id);
			String shot = DvsScreenshot.capture(row.tcId(), "after-submit");
			row.screenshot(shot);
			DvsFieldActions.closeDialog();

			if (submitted) {
				suiteSubmitted(row, id, value, submitToast);
				verdictStep(row);
				return;
			}

			List<String> msgs = new java.util.ArrayList<>();
			for (String m : List.of(inline, inlineAfter)) {
				if (!m.isEmpty() && !msgs.contains(m)) {
					msgs.add(m);
				}
			}
			msgs.addAll(newLines);
			boolean match = contains.isEmpty() ? !msgs.isEmpty() || badgeUp > 0
					: msgs.stream().anyMatch(m -> m.toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT)));
			String actual = "typed '" + abbreviate(typed) + "' | inline: " + (inline.isEmpty() ? "-" : inline)
					+ " | Save toast: " + (saveToast.isEmpty() ? "-" : saveToast)
					+ " | Submit toast: " + (submitToast.isEmpty() ? "-" : submitToast)
					+ " | new errors: " + (newLines.isEmpty() ? "none" : String.join("; ", newLines))
					+ " | tab badge " + (badgeUp == 0 ? "unchanged" : (badgeUp > 0 ? "+" : "") + badgeUp) + savedNote;
			int max = parseMax(L.meta(id));
			switch (expect) {
				case "ERROR" -> {
					if (match) {
						row.result(DvsRow.PASS, actual);
					} else {
						suiteBug(row, id, value, "Invalid value accepted: " + id + " = '" + abbreviate(value) + "' (expected an error"
								+ (contains.isEmpty() ? "" : " containing '" + contains + "'") + ")", actual);
					}
				}
				case "OK" -> {
					if (!msgs.isEmpty() || badgeUp > 0) {
						suiteBug(row, id, value, "Valid value rejected: " + id + " = '" + abbreviate(value) + "'", actual);
					} else if (!value.isEmpty() && !"dropdown".equals(kind) && !DvsFieldActions.sameValue(kind, value, typed)) {
						fail(row, "Value changed after entry (typed '" + abbreviate(value) + "', field shows '" + abbreviate(typed) + "') | " + actual);
					} else {
						row.result(DvsRow.PASS, actual);
					}
				}
				case "LIMIT" -> {
					if (max > 0 && typed.length() <= max) {
						row.result(DvsRow.PASS, actual + " | field limited to " + max + " characters (entered " + value.length() + ")");
					} else if (!msgs.isEmpty()) {
						row.result(DvsRow.PASS, actual);
					} else {
						suiteBug(row, id, value, "Over-length value kept: " + typed.length() + " characters in " + id + " (max " + max + ")", actual);
					}
				}
				default -> row.result(DvsRow.CAPTURED, actual + " (behaviour recorded - review)");
			}
			DvsStepLog.step("suite-row").field(id).before(before).entered(abbreviate(value)).after(abbreviate(typed))
					.expected(expect + (contains.isEmpty() ? "" : " " + contains)).actual(actual).inline(inline)
					.toast(submitToast).dialog(oneLine(dialog)).shot(shot)
					.result(DvsRow.FAIL.equals(row.status()) ? DvsStepLog.FAIL : DvsStepLog.PASS).log();
		} catch (RunAbort e) {
			throw e;
		} catch (WebDriverException e) {
			if (sessionGone(e)) {
				throw new RunAbort("Browser session lost: " + firstLine(e), e);
			}
			fail(row, "Exception: " + firstLine(e));
		} catch (RuntimeException e) {
			fail(row, "Exception: " + firstLine(e));
		}
		verdictStep(row);
	}

	private void suiteBug(DvsRow row, String id, String value, String detail, String actual) {
		fail(row, "BUG - " + detail + " | " + actual);
		DvsFindings.add(DvsFindings.BUG, row.tcId(), customerId, row.get("Tab"), id, detail, row.screenshot());
	}

	/** The Submit went through although the scenario value should not be accepted on this record: send it back to Maker. */
	private void suiteSubmitted(DvsRow row, String id, String value, String toast) {
		String detail = "Record submitted to Checker with " + id + " = '" + abbreviate(value) + "'";
		row.result(DvsRow.FAIL, "BUG - " + detail + " | Toast: " + toast);
		DvsFindings.add(DvsFindings.BUG, row.tcId(), customerId, row.get("Tab"), id, detail, row.screenshot());
		if (!DvsCheckerFlow.enabled()) {
			customerBlocked = true;
			blockedReason = "Record " + customerId + " was submitted to Checker and not reverted (checker.recovery=OFF)";
			return;
		}
		DvsCheckerFlow.Outcome out = DvsCheckerFlow.recover(row.tcId(), row.moduleCode(), customerId, id + "=" + value,
				suiteMakerFull, lastPositive);
		row.result(DvsRow.FAIL, row.actual() + " | recovery: reverted=" + out.reverted() + ", backInMaker=" + out.backInMaker());
		if (!out.backInMaker()) {
			customerBlocked = true;
			blockedReason = "R3: " + customerId + " did not return to the Maker queue after revert";
		}
		touched.add(id);
	}

	private static int parseMax(DvsLocators.Meta m) {
		try {
			return m == null ? 0 : Integer.parseInt(m.maxLen().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String abbreviate(String s) {
		return s == null ? "" : s.length() <= 60 ? s : s.substring(0, 57) + "...(" + s.length() + " chars)";
	}

	/** Row with values from DVS_TypedValues.properties: earlier pairs are set first, the last pair is the judged field. */
	private void executeTyped(DvsRow row, List<DvsTypedValues.Pair> pairs) {
		for (DvsTypedValues.Pair p : pairs) {
			if (!L.has(p.locatorId())) {
				row.result(DvsRow.NOT_RUN, "DVS_TypedValues.properties points to unknown locator " + p.locatorId());
				return;
			}
		}
		DvsTypedValues.Pair main = pairs.get(pairs.size() - 1);
		row.locatorId(main.locatorId());
		String value = token(main.value());
		if (dryRun) {
			row.result(DvsRow.PLANNED, "would set " + pairs.stream().map(p -> p.locatorId() + " = '" + p.value() + "'")
					.collect(java.util.stream.Collectors.joining(", ")) + " [typed value from DVS_TypedValues]" + flags(row));
			return;
		}
		try {
			for (DvsTypedValues.Pair p : pairs.subList(0, pairs.size() - 1)) {
				touched.add(p.locatorId());
				DvsFieldActions.set(p.locatorId(), token(p.value()));
				DvsFieldActions.blur(p.locatorId());
			}
			runLiteral(row, main.locatorId(), value);
		} catch (RunAbort e) {
			throw e;
		} catch (WebDriverException e) {
			if (sessionGone(e)) {
				throw new RunAbort("Browser session lost: " + firstLine(e), e);
			}
			fail(row, "Exception: " + firstLine(e));
		} catch (RuntimeException e) {
			fail(row, "Exception: " + firstLine(e));
		}
		verdictStep(row);
	}

	/** What a dry run reports about the extra checks this row would get. */
	private String flags(DvsRow row) {
		StringBuilder sb = new StringBuilder();
		if (needsSubmit(row)) {
			sb.append(" [Submit to Checker + R2 recovery]");
		}
		if (DvsConfig.getBool("tabswitch.check", true)) {
			sb.append(" [tab-switch persistence T1/T2]");
		}
		return sb.toString();
	}

	private void runLiteral(DvsRow row, String id, String value) {
		touched.add(id);
		String kind = L.kind(id);
		String connId = "";
		DvsLocators.Resolution conn = L.resolveField(row.moduleCode(), row.connectedField());
		String lockNote = "";
		if (conn.ok() && !row.connectedValue().isEmpty() && !descriptive(row.connectedValue())) {
			connId = conn.id();
			touched.add(connId);
			if (bugLocked.contains(connId)) {
				lockNote = " | connected field " + connId + " has known bug value";
			} else {
				DvsFieldActions.set(connId, token(row.connectedValue()));
				DvsFieldActions.blur(connId);
			}
		}
		String baseline = DvsFieldActions.read(id);
		DvsFieldActions.set(id, value);
		DvsFieldActions.blur(id);
		String error = DvsFieldActions.errorText(id);
		String back = DvsFieldActions.read(id);
		String actual = (error.isEmpty() ? "No validation message" : "Message: " + error) + " | value now '" + back + "'" + lockNote;
		String expected = row.expectedResult();
		boolean expectReject = NEGATIVE.matcher(expected).find();

		if (expectReject) {
			if (error.isEmpty()) {
				acceptedBug(row, id, value, actual, expected);
			} else {
				row.result(DvsRow.PASS, actual);
			}
		} else if (POSITIVE.matcher(expected).find()) {
			if (!error.isEmpty()) {
				fail(row, "Expected acceptance but " + actual);
			} else if (!value.isEmpty() && !"dropdown".equals(kind) && !"checkbox".equals(kind) && !value.equals(back)) {
				fail(row, "Value changed after entry (typed '" + value + "', now '" + back + "')");
			} else {
				row.result(DvsRow.PASS, actual);
			}
		} else {
			row.result(DvsRow.CAPTURED, actual + " (expected result is descriptive - review)");
		}
		if (DvsRow.FAIL.equals(row.status()) || DvsRow.CAPTURED.equals(row.status())) {
			row.screenshot(DvsScreenshot.capture(row.tcId(), row.status()));
		}

		if (needsSubmit(row) && !customerBlocked) {
			submitFlow(row, id, value, error.isEmpty());
		}
		if (!error.isEmpty() || value.isEmpty() || !DvsConfig.getBool("tabswitch.check", true) || customerBlocked) {
			return;
		}
		persistenceCheck(row, id, value, baseline, expectReject);
	}

	/** R1: invalid value accepted with no error - log a BUG, do not Submit for that row (unless the row tests Submit). */
	private void acceptedBug(DvsRow row, String id, String value, String actual, String expected) {
		String detail = "Invalid value accepted: " + id + " = '" + value + "' (expected: " + expected + ")";
		fail(row, "BUG - " + detail + " | " + actual);
		DvsFindings.add(DvsFindings.BUG, row.tcId(), customerId, "", id, detail, row.screenshot());
	}

	/** Rows that test Submit: Expected_Result talks about Submit being blocked. */
	static boolean needsSubmit(DvsRow row) {
		String e = row.expectedResult().toLowerCase(Locale.ROOT);
		return e.contains("submit") && (e.contains("block") || e.contains("reject") || e.contains("not allowed"));
	}

	/** T3 + C1, Submit to Checker, E1-E3, S1; on an accepted invalid value R2 Checker revert recovery. */
	private void submitFlow(DvsRow row, String id, String value, boolean noInlineError) {
		String prefix = "LE".equals(row.moduleCode()) ? "LE" : "IND";
		DvsFieldActions.dismissDialogIfOpen();
		DvsItemErrors.Panel before = DvsItemErrors.read();
		DvsFieldActions.recordTabNames().forEach(DvsFieldActions::openTab);
		Map<String, String> makerFull = DvsRestoreManager.captureFull(prefix);

		String toast = DvsNavigation.submit();
		String dialog = DvsFieldActions.dialogText();
		boolean submitted = DvsCheckerFlow.submitSucceeded(toast);
		DvsItemErrors.Panel after = DvsItemErrors.read();

		if (!submitted) {
			DvsItemErrors.checkDialogCounts(row.tcId(), customerId);
			DvsItemErrors.mapAndVerify(after, row.tcId(), customerId);
			DvsFieldActions.dismissDialogIfOpen();
			String note = "Submit blocked as expected. Toast: " + toast + (dialog.isEmpty() ? "" : " | Dialog: " + oneLine(dialog));
			if (!DvsRow.FAIL.equals(row.status())) {
				row.result(DvsRow.PASS, row.actual() + " | " + note);
			} else {
				row.result(DvsRow.FAIL, row.actual() + " | " + note);
			}
			return;
		}

		String detail = "Invalid value submitted to checker: " + id + " = '" + value + "'";
		row.result(DvsRow.FAIL, "BUG - " + detail + " | Toast: " + toast);
		row.screenshot(DvsScreenshot.capture(row.tcId(), "submitted"));
		if (!before.iflow().isEmpty()) {
			DvsFindings.add(DvsFindings.BUG, row.tcId(), customerId, "", "Item Error List",
					"Record submitted while " + before.iflow().size() + " iFlow revert errors were still listed: " + before.iflow(),
					row.screenshot());
		}
		if (!DvsCheckerFlow.enabled()) {
			row.result(DvsRow.FAIL, row.actual() + " | checker.recovery=OFF: record left in Checker queue");
			customerBlocked = true;
			blockedReason = "Record " + customerId + " was submitted to Checker and not reverted (checker.recovery=OFF)";
			return;
		}
		DvsCheckerFlow.Outcome out = DvsCheckerFlow.recover(row.tcId(), row.moduleCode(), customerId,
				id + "=" + value, makerFull, lastPositive);
		row.result(DvsRow.FAIL, row.actual() + " | recovery: reverted=" + out.reverted() + ", backInMaker=" + out.backInMaker()
				+ (out.notes().isEmpty() ? "" : " | " + oneLine(String.join("; ", out.notes()))));
		if (!out.backInMaker()) {
			customerBlocked = true;
			blockedReason = "R3: " + customerId + " did not return to the Maker queue after revert";
			return;
		}
		if (DvsConfig.getBool("keep.bug.value", true)) {
			bugLocked.add(id);
			DvsStepLog.step("bug-locked").field(id).actual("kept bug value '" + value + "' (keep.bug.value=true)").result(DvsStepLog.INFO).log();
		} else {
			touched.add(id);
		}
	}

	/** T1 / T2: switch tab and come back, then reload and read again. */
	private void persistenceCheck(DvsRow row, String id, String value, String baseline, boolean invalidValue) {
		String kind = L.kind(id);
		DvsLocators.Meta meta = L.meta(id);
		String ownTab = meta == null ? "" : meta.tab();
		try {
			String other = "";
			for (String t : DvsFieldActions.recordTabNames()) {
				if (!t.equalsIgnoreCase(ownTab)) {
					other = t;
					break;
				}
			}
			if (!other.isEmpty()) {
				DvsFieldActions.openTab(other);
				DvsFieldActions.openTab(ownTab);
			}
			String afterSwitch = DvsFieldActions.read(id);
			DvsNavigation.reloadAndReopen(row.moduleCode(), customerId);
			DvsFieldActions.openTab(ownTab);
			String afterReload = DvsFieldActions.read(id);
			boolean sameSwitch = DvsFieldActions.sameValue(kind, value, afterSwitch);
			boolean sameReload = DvsFieldActions.sameValue(kind, value, afterReload);
			DvsStepLog.step("persistence-check").field(id).before(baseline).entered(value).after(afterReload)
					.actual("after tab switch '" + afterSwitch + "', after reload '" + afterReload + "'")
					.result(sameSwitch && sameReload ? DvsStepLog.PASS : DvsStepLog.FAIL).log();

			if (invalidValue && sameReload) {
				persistenceBug(row, id, "Invalid data autosaved: '" + value + "' is still there after reload");
			} else if (!invalidValue && (!sameSwitch || !sameReload)) {
				String shown = !sameSwitch ? afterSwitch : afterReload;
				boolean notUpdated = DvsFieldActions.sameValue(kind, baseline, shown);
				persistenceBug(row, id, (notUpdated ? "Not updated: old value '" + baseline + "' shown" : "Altered: typed '" + value
						+ "' but shows '" + shown + "'") + (sameSwitch ? " after reload" : " after tab switch"));
			}
		} catch (WebDriverException e) {
			if (sessionGone(e)) {
				throw new RunAbort("Browser session lost: " + firstLine(e), e);
			}
			DvsStepLog.step("persistence-check").field(id).actual("Exception: " + firstLine(e)).result(DvsStepLog.FAIL).log();
		}
	}

	private void persistenceBug(DvsRow row, String id, String text) {
		row.screenshot(DvsScreenshot.capture(row.tcId(), "persistence"));
		DvsFindings.add(DvsFindings.BUG, row.tcId(), customerId, "", id, text, row.screenshot());
		row.result(DvsRow.FAIL, row.actual() + " | " + text);
		touched.add(id);
	}

	/** Positive "X tab" rows: record the tab values, Save, reload, reopen, compare. */
	private void executeTabRoundTrip(DvsRow row, String label) {
		String tab = TAB_LABELS.get(label.toLowerCase(Locale.ROOT));
		if (tab == null) {
			row.result(DvsRow.NOT_RUN, "Tab '" + label + "' has no confirmed mapping to a DVS tab (add it to TAB_LABELS)");
			return;
		}
		String prefix = "LE".equals(row.moduleCode()) ? "LE" : "IND";
		List<String> ids = L.idsForTab(prefix, tab);
		if (ids.isEmpty()) {
			row.result(DvsRow.NOT_RUN, "No locators captured for tab '" + tab + "' in module " + prefix);
			return;
		}
		if (dryRun) {
			row.result(DvsRow.PLANNED, "would Save + reload + compare " + ids.size() + " locators of tab '" + tab + "'");
			return;
		}
		try {
			DvsFieldActions.openTab(tab);
			Map<String, String> before = new LinkedHashMap<>();
			for (String id : ids) {
				if (DvsLocators.isInput(L.kind(id)) && DvsFieldActions.present(id)) {
					before.put(id, DvsFieldActions.read(id));
				}
			}
			DvsNavigation.save();
			String toast = DvsFieldActions.toastText();
			// always the customer this run opened (config) - the workbook row id can belong to another persona
			DvsNavigation.reloadAndReopen(row.moduleCode(), customerId);
			DvsFieldActions.openTab(tab);
			StringBuilder diff = new StringBuilder();
			for (Map.Entry<String, String> e : before.entrySet()) {
				String now = DvsFieldActions.present(e.getKey()) ? DvsFieldActions.read(e.getKey()) : "<missing>";
				if (!DvsFieldActions.sameValue(L.kind(e.getKey()), e.getValue(), now)) {
					diff.append(e.getKey()).append(": '").append(e.getValue()).append("' -> '").append(now).append("'; ");
				}
			}
			if (diff.length() == 0) {
				row.result(DvsRow.PASS, before.size() + " fields identical after Save + reopen. Toast: " + toast);
			} else {
				fail(row, "Differences after reopen: " + diff);
			}
		} catch (WebDriverException e) {
			if (sessionGone(e)) {
				throw new RunAbort("Browser session lost: " + firstLine(e), e);
			}
			fail(row, "Exception: " + firstLine(e));
		}
		verdictStep(row);
	}

	private void verdictStep(DvsRow row) {
		String result = DvsRow.PASS.equals(row.status()) ? DvsStepLog.PASS
				: DvsRow.FAIL.equals(row.status()) ? DvsStepLog.FAIL : DvsStepLog.INFO;
		DvsStepLog.step("row-verdict").field(row.locatorId()).entered(row.inputValue()).expected(row.expectedResult())
				.actual(row.status() + ": " + row.actual()).shot(row.screenshot()).result(result).log();
	}

	private void fail(DvsRow row, String message) {
		row.result(DvsRow.FAIL, message);
		if (row.screenshot().isEmpty() && !dryRun) {
			row.screenshot(DvsScreenshot.capture(row.tcId(), "FAIL"));
		}
	}

	static boolean descriptive(String v) {
		if (v == null || v.isBlank()) {
			return true;
		}
		String s = v.trim();
		String l = s.toLowerCase(Locale.ROOT);
		if (l.equals("valid values") || l.startsWith("open") || l.equals("—") || l.equals("…")) {
			return true;
		}
		if (DATE_TOKEN.matcher(s).matches()) {
			return false;
		}
		return s.contains(",") || s.contains("+") || s.contains("→") || s.contains("…")
				|| s.split("\\s+").length > 3;
	}

	static String token(String v) {
		String s = v.trim();
		String l = s.toLowerCase(Locale.ROOT);
		if (l.equals("<blank>") || l.equals("blank") || l.equals("<empty>")) {
			return "";
		}
		Matcher m = DATE_TOKEN.matcher(s);
		if (m.matches()) {
			int base = m.group(1).equalsIgnoreCase("yesterday") ? -1 : m.group(1).equalsIgnoreCase("tomorrow") ? 1 : 0;
			int plus = m.group(2) == null ? 0 : Integer.parseInt(m.group(2).replace("+", ""));
			String fmt = DvsConfig.get("dvs.dateFormat", "dd-MM-yyyy");
			return LocalDate.now().plusDays(base + plus).format(DateTimeFormatter.ofPattern(fmt));
		}
		return s;
	}

	private static boolean sessionGone(WebDriverException e) {
		String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
		return e instanceof NoSuchSessionException || e instanceof NoSuchWindowException
				|| msg.contains("invalid session id") || msg.contains("no such window") || msg.contains("disconnected");
	}

	private static String firstLine(Throwable t) {
		String m = t.getMessage();
		if (m == null) {
			return t.getClass().getSimpleName();
		}
		int nl = m.indexOf('\n');
		return nl > 0 ? m.substring(0, nl) : m;
	}

	private static String oneLine(String s) {
		return s.replace('\r', ' ').replace('\n', ' ');
	}
}
