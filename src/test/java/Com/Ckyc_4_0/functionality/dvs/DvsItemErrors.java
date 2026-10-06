/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsFindings;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsScreenshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** E1-E3: Item Error List capture, entry to field mapping, validation-dialog count checks. */
public final class DvsItemErrors {

	public record Panel(List<String> iflow, List<String> ckyc, List<String> checker, String raw) {
		public boolean isEmpty() {
			return iflow.isEmpty() && ckyc.isEmpty() && checker.isEmpty();
		}

		public List<String> all() {
			List<String> a = new ArrayList<>(iflow);
			a.addAll(ckyc);
			a.addAll(checker);
			return a;
		}

		public String summary() {
			return "IFLOW[" + iflow.size() + "]=" + iflow + " CKYC[" + ckyc.size() + "]=" + ckyc + " CHECKER["
					+ checker.size() + "]=" + checker;
		}
	}

	private static final DvsLocators L = DvsLocators.get();
	private static final Pattern DIALOG_COUNT = Pattern.compile("(\\d+)\\s+Validation\\s+Errors?\\s+Found", Pattern.CASE_INSENSITIVE);
	private static Map<String, String> errorMap;

	private DvsItemErrors() {
	}

	/** E1: reads the Item Error List panel (three columns). Empty panel when it is not on screen. */
	public static Panel read() {
		List<String> iflow = new ArrayList<>();
		List<String> ckyc = new ArrayList<>();
		List<String> checker = new ArrayList<>();
		String raw = "";
		try {
			List<WebElement> panels = BaseClass.getDriver().findElements(L.by("COM_ERR_panel"));
			if (!panels.isEmpty()) {
				raw = panels.get(0).getText();
				parse(raw, iflow, ckyc, checker);
			}
		} catch (RuntimeException e) {
			raw = "";
		}
		Panel p = new Panel(iflow, ckyc, checker, raw);
		DvsStepLog.step("read-item-error-list").items(p.summary()).result(DvsStepLog.INFO).log();
		return p;
	}

	static void parse(String raw, List<String> iflow, List<String> ckyc, List<String> checker) {
		List<String> current = null;
		for (String line : raw.split("\\R")) {
			String t = line.trim();
			if (t.isEmpty()) {
				continue;
			}
			String up = t.toUpperCase(Locale.ROOT);
			if (up.startsWith("IFLOW REVERT")) {
				current = iflow;
			} else if (up.startsWith("CKYC REVERT")) {
				current = ckyc;
			} else if (up.startsWith("CHECKER REVERT")) {
				current = checker;
			} else if (up.startsWith("ITEM ERROR LIST") || t.matches("\\d+")) {
				continue;
			} else if (current != null) {
				current.add(t);
			}
		}
	}

	/**
	 * E2: each entry must map to a field (DVS_ErrorListMap.properties), the field must exist and show the error.
	 * @return findings text, one per problem (also written to the Bugs sheet)
	 */
	public static List<String> mapAndVerify(Panel panel, String tcId, String customerId) {
		List<String> problems = new ArrayList<>();
		for (String entry : panel.all()) {
			String id = mapEntry(entry);
			if (id == null) {
				problems.add(add(tcId, customerId, "", "Error list entry not mapped to a field: '" + entry + "'"));
				continue;
			}
			try {
				DvsFieldActions.ensureVisible(id);
				boolean showsError = !DvsFieldActions.errorText(id).isEmpty() || invalidFlag(id);
				if (!showsError) {
					problems.add(add(tcId, customerId, id, "Entry '" + entry + "' is listed but the field shows no error"
							+ " (value '" + DvsFieldActions.read(id) + "') - stale iFlow error"));
				}
			} catch (RuntimeException e) {
				problems.add(add(tcId, customerId, id, "Entry '" + entry + "' maps to " + id + " but the field is not on screen: "
						+ DvsFieldActions.firstLine(e)));
			}
		}
		return problems;
	}

	/** E3: dialog count = tab badge total = inline error count; errors grouped under "Other" are a UI bug. */
	public static List<String> checkDialogCounts(String tcId, String customerId) {
		List<String> problems = new ArrayList<>();
		String dialog = DvsFieldActions.dialogText();
		Matcher m = DIALOG_COUNT.matcher(dialog);
		if (!m.find()) {
			return problems;
		}
		int dialogCount = Integer.parseInt(m.group(1));
		int badges = 0;
		for (WebElement b : BaseClass.getDriver().findElements(L.by("COM_ERR_tabBadge"))) {
			String t = b.getText().trim();
			if (t.matches("\\d+")) {
				badges += Integer.parseInt(t);
			}
		}
		int inline = 0;
		for (WebElement e : BaseClass.getDriver().findElements(L.by("COM_ERR_inline"))) {
			if (!e.getText().trim().isEmpty()) {
				inline++;
			}
		}
		if (dialogCount != badges) {
			problems.add(add(tcId, customerId, "", "Validation dialog says " + dialogCount + " but tab badges total " + badges));
		}
		if (dialogCount != inline) {
			problems.add(add(tcId, customerId, "", "Validation dialog says " + dialogCount + " but " + inline + " inline errors shown"));
		}
		for (String line : dialog.split("\\R")) {
			if (line.trim().equalsIgnoreCase("Other")) {
				problems.add(add(tcId, customerId, "", "Errors are grouped under 'Other' instead of their own tab (UI bug)"));
				break;
			}
		}
		DvsStepLog.step("check-validation-counts").dialog(dialog)
				.actual("dialog=" + dialogCount + " badges=" + badges + " inline=" + inline)
				.result(problems.isEmpty() ? DvsStepLog.PASS : DvsStepLog.FAIL).log();
		return problems;
	}

	static String mapEntry(String entry) {
		String e = entry.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> m : map().entrySet()) {
			if (e.contains(m.getKey())) {
				return m.getValue();
			}
		}
		return null;
	}

	private static boolean invalidFlag(String id) {
		List<WebElement> els = BaseClass.getDriver().findElements(L.by(id));
		return !els.isEmpty() && "true".equals(els.get(0).getAttribute("aria-invalid"));
	}

	private static String add(String tcId, String customerId, String field, String detail) {
		DvsFindings.add(DvsFindings.BUG, tcId, customerId, "", field, detail, DvsScreenshot.capture(tcId.isEmpty() ? "DVS" : tcId, "item-errors"));
		return detail;
	}

	private static synchronized Map<String, String> map() {
		if (errorMap == null) {
			errorMap = new LinkedHashMap<>();
			try (InputStream in = DvsItemErrors.class.getResourceAsStream("/DVS/DVS_ErrorListMap.properties")) {
				if (in != null) {
					java.io.BufferedReader br = new java.io.BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
					String line;
					while ((line = br.readLine()) != null) {
						String t = line.trim();
						int eq = t.lastIndexOf('=');
						if (t.isEmpty() || t.startsWith("#") || eq < 0) {
							continue;
						}
						errorMap.put(t.substring(0, eq).trim().toLowerCase(Locale.ROOT), t.substring(eq + 1).trim());
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException("Cannot load DVS/DVS_ErrorListMap.properties", e);
			}
		}
		return errorMap;
	}
}
