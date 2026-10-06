/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Last-positive snapshot + restore (group end), and the full Maker snapshot / Maker-to-Checker compare (C1-C4).
 * Dropdown values are compared on the visible code prefix, never on the hidden value.
 */
public final class DvsRestoreManager {

	private static final Logger logger = LoggerFactory.getLogger(DvsRestoreManager.class);
	private static final DvsLocators L = DvsLocators.get();

	private DvsRestoreManager() {
	}

	/** Reads every input visible on every record tab of the module (IND or LE) into a last-positive snapshot. */
	public static DvsSnapshot capture(String modulePrefix) {
		DvsSnapshot snap = new DvsSnapshot();
		Map<String, String> full = captureFull(modulePrefix);
		snap.full().putAll(full);
		for (Map.Entry<String, String> e : full.entrySet()) {
			int bar = e.getKey().indexOf('|');
			String id = e.getKey().substring(bar + 1);
			if (L.has(id)) {
				snap.put(id, e.getValue());
			}
		}
		DvsStepLog.step("snapshot").actual(snap.values().size() + " fields captured for " + modulePrefix).result(DvsStepLog.INFO).log();
		return snap;
	}

	/**
	 * C1: every tab, every input (dropdown visible text, checkbox / radio state, dates, masked values), every RP
	 * table row and the image list. Keys: tab|Locator_ID, RP|row-n, IMG|n.
	 */
	public static Map<String, String> captureFull(String modulePrefix) {
		Map<String, String> out = new LinkedHashMap<>();
		List<String> tabs = DvsFieldActions.recordTabNames();
		if (tabs.isEmpty()) {
			tabs = new ArrayList<>(tabsOf(modulePrefix));
		}
		String eyeXpath = DvsConfig.get("dvs.maskEyeXpath", "");
		for (String tab : tabs) {
			try {
				DvsFieldActions.openTab(tab);
			} catch (RuntimeException e) {
				logger.warn("Snapshot: cannot open tab {}: {}", tab, e.getMessage());
				continue;
			}
			for (String id : L.idsForTab(modulePrefix, tab)) {
				String kind = L.kind(id);
				if (!DvsLocators.isInput(kind) || !DvsFieldActions.present(id)) {
					continue;
				}
				try {
					if ("masked-text".equals(kind) && !eyeXpath.isBlank()) {
						revealMasked(id, eyeXpath);
					}
					out.put(tab + "|" + id, DvsFieldActions.read(id));
				} catch (RuntimeException e) {
					logger.debug("Snapshot: skip {}: {}", id, e.getMessage());
				}
			}
			List<WebElement> rows = BaseClass.getDriver().findElements(L.by("COM_RP_rows"));
			int n = 0;
			for (WebElement r : rows) {
				String text = r.getText().replace('\n', ' ').trim();
				if (!text.isEmpty()) {
					out.put("RP|" + tab + "|row-" + (++n), text);
				}
			}
		}
		out.putAll(captureImages());
		return out;
	}

	/** Image list: file name + document label (alt text) in screen order. */
	public static Map<String, String> captureImages() {
		Map<String, String> out = new LinkedHashMap<>();
		List<WebElement> thumbs = BaseClass.getDriver().findElements(L.by("COM_IMG_Thumbnail"));
		if (thumbs.isEmpty()) {
			try {
				DvsFieldActions.clickBy("COM_SHELL_Image");
				thumbs = BaseClass.getDriver().findElements(L.by("COM_IMG_Thumbnail"));
			} catch (RuntimeException ignored) {
				// no Image tab on this screen
			}
		}
		int n = 0;
		for (WebElement t : thumbs) {
			String alt = t.getAttribute("alt");
			out.put("IMG|" + (++n), alt == null ? "" : alt.trim());
		}
		out.put("IMG|count", String.valueOf(n));
		return out;
	}

	/** C2: field-by-field compare of a Maker snapshot with what the Checker shows. */
	public static List<String> compareFull(Map<String, String> maker, Map<String, String> checker) {
		List<String> diffs = new ArrayList<>();
		for (Map.Entry<String, String> e : maker.entrySet()) {
			String key = e.getKey();
			if (!checker.containsKey(key)) {
				diffs.add(key + ": missing in Checker (Maker='" + e.getValue() + "')");
				continue;
			}
			String m = e.getValue();
			String c = checker.get(key);
			String id = key.substring(key.lastIndexOf('|') + 1);
			String kind = L.has(id) ? L.kind(id) : "";
			if (!DvsFieldActions.sameValue(kind, m, c)) {
				String why = c.isBlank() ? "blank in Checker" : m.isBlank() ? "blank in Maker" : "different";
				diffs.add(key + ": " + why + " (Maker='" + m + "', Checker='" + c + "')");
			}
		}
		for (String key : checker.keySet()) {
			if (!maker.containsKey(key) && (key.startsWith("RP|") || key.startsWith("IMG|"))) {
				diffs.add(key + ": extra in Checker ('" + checker.get(key) + "')");
			}
		}
		return diffs;
	}

	/** C3: ids of fields on the open record that are still editable (should be empty in Checker). */
	public static List<String> editableFields(String modulePrefix) {
		List<String> editable = new ArrayList<>();
		for (String tab : DvsFieldActions.recordTabNames()) {
			DvsFieldActions.openTab(tab);
			for (String id : L.idsForTab(modulePrefix, tab)) {
				if (DvsLocators.isInput(L.kind(id)) && DvsFieldActions.present(id) && !DvsFieldActions.isReadOnly(id)) {
					editable.add(tab + "|" + id);
				}
			}
		}
		return editable;
	}

	/** Re-applies snapshot values to the touched fields only (fields already equal are left alone). */
	public static void restore(DvsSnapshot snap, Set<String> touchedIds) {
		for (String id : touchedIds) {
			String want = snap.get(id);
			if (want == null) {
				continue;
			}
			try {
				String now = DvsFieldActions.read(id);
				if (!DvsFieldActions.sameValue(L.kind(id), want, now)) {
					DvsFieldActions.set(id, "dropdown".equals(L.kind(id)) ? DvsFieldActions.codeOf(want) : want);
					DvsFieldActions.blur(id);
					DvsStepLog.step("restore").field(id).before(now).entered(want).result(DvsStepLog.RESTORE).log();
				}
			} catch (RuntimeException e) {
				logger.warn("Restore failed for {}: {}", id, e.getMessage());
				DvsStepLog.step("restore").field(id).entered(want).actual("Exception: " + DvsFieldActions.firstLine(e))
						.result(DvsStepLog.FAIL).log();
			}
		}
	}

	/** @return ids whose current value differs from the snapshot (empty = record equals last positive data). */
	public static List<String> mismatches(DvsSnapshot snap, Set<String> ids) {
		List<String> bad = new ArrayList<>();
		for (String id : ids) {
			String want = snap.get(id);
			if (want == null) {
				continue;
			}
			try {
				if (!DvsFieldActions.sameValue(L.kind(id), want, DvsFieldActions.read(id))
						|| !DvsFieldActions.errorText(id).isEmpty()) {
					bad.add(id);
				}
			} catch (RuntimeException e) {
				bad.add(id);
			}
		}
		return bad;
	}

	/** Group-end restore: apply snapshot, Save, reload, verify; retries per CONFIG restore.retries. */
	public static boolean restoreGroupEnd(DvsSnapshot snap, Set<String> touchedIds, String moduleCode, String customerId) {
		int retries = Math.max(1, DvsConfig.getInt("restore.retries", 2));
		for (int attempt = 1; attempt <= retries; attempt++) {
			DvsFieldActions.dismissDialogIfOpen();
			restore(snap, touchedIds);
			DvsNavigation.save();
			DvsNavigation.reloadAndReopen(moduleCode, customerId);
			List<String> bad = mismatches(snap, touchedIds);
			if (bad.isEmpty()) {
				DvsStepLog.step("restore-verify").actual("record equals last positive data (attempt " + attempt + ")")
						.result(DvsStepLog.PASS).log();
				return true;
			}
			logger.warn("Restore attempt {}/{} left differences: {}", attempt, retries, bad);
			DvsStepLog.step("restore-verify").actual("differences after attempt " + attempt + ": " + bad)
					.result(DvsStepLog.RETRY).log();
		}
		return false;
	}

	private static Set<String> tabsOf(String modulePrefix) {
		Set<String> tabs = new LinkedHashSet<>();
		for (String id : L.allIds()) {
			DvsLocators.Meta m = L.meta(id);
			if (m != null && id.startsWith(modulePrefix + "_") && DvsLocators.isInput(m.kind())) {
				tabs.add(m.tab());
			}
		}
		return tabs;
	}

	private static void revealMasked(String id, String eyeXpath) {
		List<WebElement> btn = BaseClass.getDriver().findElements(
				By.xpath("(" + L.xpath(id) + ")/ancestor::div[contains(@class,'MuiFormControl')][1]" + eyeXpath));
		if (!btn.isEmpty()) {
			try {
				btn.get(0).click();
			} catch (RuntimeException ignored) {
				// eye button not clickable - value stays masked
			}
		}
	}
}
