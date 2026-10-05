/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsSnapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Captures the last-positive snapshot and re-applies it after a group of negatives. */
public final class DvsRestoreManager {

	private static final Logger logger = LoggerFactory.getLogger(DvsRestoreManager.class);
	private static final DvsLocators L = DvsLocators.get();

	private DvsRestoreManager() {
	}

	/** Reads every input visible on every tab of the module (IND or LE) into a snapshot. */
	public static DvsSnapshot capture(String modulePrefix) {
		DvsSnapshot snap = new DvsSnapshot();
		Set<String> tabs = new LinkedHashSet<>();
		for (String id : L.allIds()) {
			DvsLocators.Meta m = L.meta(id);
			if (m != null && id.startsWith(modulePrefix + "_") && DvsLocators.isInput(m.kind())) {
				tabs.add(m.tab());
			}
		}
		for (String tab : tabs) {
			try {
				DvsFieldActions.openTab(tab);
			} catch (RuntimeException e) {
				logger.warn("Snapshot: cannot open tab {}: {}", tab, e.getMessage());
				continue;
			}
			for (String id : L.idsForTab(modulePrefix, tab)) {
				if (DvsLocators.isInput(L.kind(id)) && DvsFieldActions.present(id)) {
					try {
						snap.put(id, DvsFieldActions.read(id));
					} catch (RuntimeException e) {
						logger.debug("Snapshot: skip {}: {}", id, e.getMessage());
					}
				}
			}
		}
		return snap;
	}

	/** Re-applies snapshot values to the touched fields only (fields already equal are left alone). */
	public static void restore(DvsSnapshot snap, Set<String> touchedIds) {
		for (String id : touchedIds) {
			String want = snap.get(id);
			if (want == null) {
				continue;
			}
			try {
				if (!want.equals(DvsFieldActions.read(id))) {
					DvsFieldActions.set(id, want);
					DvsFieldActions.blur(id);
				}
			} catch (RuntimeException e) {
				logger.warn("Restore failed for {}: {}", id, e.getMessage());
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
				if (!want.equals(DvsFieldActions.read(id)) || !DvsFieldActions.errorText(id).isEmpty()) {
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
				return true;
			}
			logger.warn("Restore attempt {}/{} left differences: {}", attempt, retries, bad);
		}
		return false;
	}
}
