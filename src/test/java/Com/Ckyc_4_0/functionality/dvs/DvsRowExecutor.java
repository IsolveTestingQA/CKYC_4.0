/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsRow;
import Com.Ckyc_4_0.utils.dvs.DvsScreenshot;

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
 * Executes ONE data row. A row is run only when its Field resolves to a locator and its Input_Value is a
 * typed value. Anything else is reported NOT RUN with the reason - never guessed.
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
	private String customerId = "";
	private final Set<String> touched = new LinkedHashSet<>();

	public DvsRowExecutor(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public void customer(String id) {
		this.customerId = id;
	}

	/** Locator ids touched since the last call (field + connected field), then cleared. */
	public Set<String> drainTouched() {
		Set<String> copy = new LinkedHashSet<>(touched);
		touched.clear();
		return copy;
	}

	public void execute(DvsRow row) {
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
					+ (openList ? "" : " = '" + value + "'"));
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
	}

	private void runLiteral(DvsRow row, String id, String value) {
		touched.add(id);
		String connId = "";
		DvsLocators.Resolution conn = L.resolveField(row.moduleCode(), row.connectedField());
		if (conn.ok() && !row.connectedValue().isEmpty() && !descriptive(row.connectedValue())) {
			connId = conn.id();
			touched.add(connId);
			DvsFieldActions.set(connId, token(row.connectedValue()));
			DvsFieldActions.blur(connId);
		}
		DvsFieldActions.set(id, value);
		DvsFieldActions.blur(id);
		String error = DvsFieldActions.errorText(id);
		String back = DvsFieldActions.read(id);
		String actual = (error.isEmpty() ? "No validation message" : "Message: " + error) + " | value now '" + back + "'";
		String expected = row.expectedResult();

		if (NEGATIVE.matcher(expected).find()) {
			if (error.isEmpty()) {
				fail(row, "Expected rejection but " + actual);
			} else {
				row.result(DvsRow.PASS, actual);
			}
		} else if (POSITIVE.matcher(expected).find()) {
			if (!error.isEmpty()) {
				fail(row, "Expected acceptance but " + actual);
			} else if (!value.isEmpty() && !"dropdown".equals(L.kind(id)) && !value.equals(back)
					&& !"checkbox".equals(L.kind(id))) {
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
				if (!e.getValue().equals(now)) {
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
}
