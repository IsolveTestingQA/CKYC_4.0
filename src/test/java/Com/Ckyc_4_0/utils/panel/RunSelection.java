/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.panel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What was chosen in the Test Control Panel for this run (null / inactive when the panel was not used). */
public final class RunSelection {

	private static volatile RunSelection current;

	private final String mode;
	private final Map<String, List<String>> subs;
	private final Map<String, Map<String, List<String>>> groups;
	private final Map<String, Map<String, String>> texts;
	private final boolean stopOnFirstFailure;
	private final String tagExpression;
	private final List<String> notes;
	private final boolean stopped;
	private final String preset;

	RunSelection(String mode, Map<String, List<String>> subs, Map<String, Map<String, List<String>>> groups,
			Map<String, Map<String, String>> texts, boolean stopOnFirstFailure, String tagExpression, List<String> notes,
			boolean stopped, String preset) {
		this.mode = mode;
		this.subs = subs;
		this.groups = groups;
		this.texts = texts;
		this.stopOnFirstFailure = stopOnFirstFailure;
		this.tagExpression = tagExpression;
		this.notes = notes;
		this.stopped = stopped;
		this.preset = preset;
	}

	static void set(RunSelection s) {
		current = s;
	}

	/** The selection of this run, or null when the panel was not used. */
	public static RunSelection current() {
		return current;
	}

	public static boolean stopOnFirstFailure() {
		return current != null && current.stopOnFirstFailure;
	}

	public boolean stopped() {
		return stopped;
	}

	public String mode() {
		return mode;
	}

	public String tagExpression() {
		return tagExpression;
	}

	public List<String> notes() {
		return notes;
	}

	/** One line for reports: "Positive | Login; DVS 2.0 (Individual, Legal Entity); Reports compare (NEW, UPDATE ...)". */
	public String summary() {
		if (stopped) {
			return "Stopped from the control panel - nothing was run";
		}
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, List<String>> e : subs.entrySet()) {
			ControlPanelRegistry.Module m = ControlPanelRegistry.module(e.getKey());
			if (m == null) {
				continue;
			}
			StringBuilder sb = new StringBuilder(m.title());
			List<String> detail = new ArrayList<>();
			for (ControlPanelRegistry.Sub s : m.subs()) {
				if (e.getValue().contains(s.key()) && m.subs().size() > 1) {
					detail.add(s.title());
				}
			}
			Map<String, List<String>> g = groups.get(e.getKey());
			if (g != null) {
				for (List<String> v : g.values()) {
					detail.addAll(v);
				}
			}
			Map<String, String> t = texts.get(e.getKey());
			if (t != null) {
				for (String v : t.values()) {
					if (!v.isBlank()) {
						detail.add(v);
					}
				}
			}
			if (!detail.isEmpty()) {
				sb.append(" (").append(String.join(", ", detail)).append(')');
			}
			parts.add(sb.toString());
		}
		return label(mode) + " | " + (parts.isEmpty() ? "no module" : String.join("; ", parts))
				+ (stopOnFirstFailure ? " | stop at first failure" : "");
	}

	public String presetName() {
		return preset;
	}

	static String label(String mode) {
		return "NEGATIVE".equals(mode) ? "Negative" : "BOTH".equals(mode) ? "Positive + Negative" : "Positive";
	}

	static Map<String, List<String>> newSubs() {
		return new LinkedHashMap<>();
	}
}
