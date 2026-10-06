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
import Com.Ckyc_4_0.utils.dvs.DvsScreenshot;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

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
		String field = row.field();
		if (field.toLowerCase(Locale.ROOT).endsWith(" tab")) {
			executeTabRoundTrip(row, field.substring(0, field.length() - 4).trim());
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
			DvsNavigation.reloadAndReopen(row.moduleCode(), row.customerId().isEmpty() ? customerId : row.customerId());
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
