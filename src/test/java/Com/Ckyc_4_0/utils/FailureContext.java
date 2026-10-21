/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns "something was not found" into plain words for the on-page decision popup: which feature / scenario / step was
 * running, which Functionality method and Page class were involved, and which element (locator) was missing - instead
 * of a raw stack trace.
 */
public final class FailureContext {

	/** One row of the popup table. */
	public record Row(String label, String value, boolean mono) {
	}

	private static volatile String feature = "";
	private static volatile String scenario = "";
	private static volatile String step = "";
	private static volatile String expected = "";

	private static final Pattern JSON_SELECTOR = Pattern
			.compile("\"method\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"selector\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern BY_LOCATOR = Pattern
			.compile("(By\\.[A-Za-z]+:\\s*[^\\r\\n]*?)(?=\\s*\\(tried for|\\s*\\(\\d|\\s*\\r?\\n|\\s*$)");
	private static final Pattern LOCATED_BY = Pattern.compile("located by\\s+([^\\r\\n]*?)(?=\\s*\\(tried for|\\s*\\r?\\n|\\s*$)");

	private FailureContext() {
	}

	/** Called by the step runner so a popup raised from deep inside a helper still knows where it is. */
	public static void setRunContext(String featureName, String scenarioName, String stepName, String expectedText) {
		feature = nz(featureName);
		scenario = nz(scenarioName);
		step = nz(stepName);
		expected = nz(expectedText);
	}

	public static void setScenario(String featureName, String scenarioName) {
		feature = nz(featureName);
		scenario = nz(scenarioName);
		step = "";
		expected = "";
	}

	public static String currentFeature() {
		return feature;
	}

	/** Rows for the popup, in reading order. Empty values are dropped by the popup. */
	public static List<Row> describe(Throwable error, String pageUrl, String extraNote) {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("Feature", feature, false));
		rows.add(new Row("Scenario", scenario, false));
		rows.add(new Row("Step being run", step, false));
		rows.add(new Row("Expected", expected, false));

		StackTraceElement func = firstFrame(error, ".functionality.");
		StackTraceElement def = firstFrame(error, ".stepdefinitions.");
		StackTraceElement page = firstFrame(error, ".pages.");
		if (func == null && def == null && page == null) {
			func = firstOwnFrame(error);
		}
		if (func != null) {
			rows.add(new Row("Functionality", simple(func.getClassName()) + "  >  " + words(func.getMethodName())
					+ "   [" + simple(func.getClassName()) + "." + func.getMethodName() + "()  line " + func.getLineNumber()
					+ "]", false));
		}
		if (def != null) {
			rows.add(new Row("Step definition", simple(def.getClassName()) + "." + def.getMethodName() + "()", false));
		}
		if (page != null) {
			rows.add(new Row("Page object", simple(page.getClassName()), false));
		}
		rows.add(new Row("Element / locator", locator(error), true));
		rows.add(new Row("What happened", shortReason(error), false));
		if (extraNote != null && !extraNote.isBlank()) {
			rows.add(new Row("Note", extraNote, false));
		}
		rows.add(new Row("Page open now", nz(pageUrl), true));
		return rows;
	}

	/** The locator the framework was looking for, as written in the Selenium message; never a stack trace. */
	public static String locator(Throwable error) {
		for (Throwable t = error; t != null; t = t.getCause()) {
			String m = t.getMessage();
			if (m == null) {
				continue;
			}
			Matcher j = JSON_SELECTOR.matcher(m);
			if (j.find()) {
				return j.group(1) + ": " + j.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
			}
			Matcher b = BY_LOCATOR.matcher(m);
			if (b.find()) {
				return b.group(1).trim();
			}
			Matcher l = LOCATED_BY.matcher(m);
			if (l.find()) {
				return l.group(1).trim();
			}
		}
		return "not named in the error - see the Page class above";
	}

	private static String shortReason(Throwable error) {
		if (error == null) {
			return "";
		}
		Throwable root = error;
		String msg = null;
		for (Throwable t = error; t != null; t = t.getCause()) {
			if (t.getMessage() != null && !t.getMessage().isBlank()) {
				msg = t.getMessage();
				root = t;
				break;
			}
		}
		String first = msg == null ? "" : msg.strip();
		int cut = first.indexOf("\n");
		if (cut > 0) {
			first = first.substring(0, cut).strip();
		}
		first = first.replaceAll("\\s*\\(Session info:.*$", "");
		if (first.length() > 260) {
			first = first.substring(0, 260) + "...";
		}
		return root.getClass().getSimpleName() + (first.isEmpty() ? "" : ": " + first);
	}

	private static StackTraceElement firstFrame(Throwable error, String packagePart) {
		for (Throwable t = error; t != null; t = t.getCause()) {
			for (StackTraceElement f : t.getStackTrace()) {
				if (f.getClassName().contains("Com.Ckyc_4_0") && f.getClassName().contains(packagePart)) {
					return f;
				}
			}
		}
		return null;
	}

	private static StackTraceElement firstOwnFrame(Throwable error) {
		for (Throwable t = error; t != null; t = t.getCause()) {
			for (StackTraceElement f : t.getStackTrace()) {
				String c = f.getClassName();
				if (c.contains("Com.Ckyc_4_0") && !c.endsWith(".Hooks") && !c.endsWith(".PopupHandler")
						&& !c.endsWith(".FailureContext")) {
					return f;
				}
			}
		}
		return null;
	}

	private static String simple(String className) {
		int dot = className.lastIndexOf('.');
		String s = dot >= 0 ? className.substring(dot + 1) : className;
		int dollar = s.indexOf('$');
		return dollar > 0 ? s.substring(0, dollar) : s;
	}

	/** verifyLoginPageIsDisplayed -> "verify Login Page Is Displayed". */
	private static String words(String method) {
		if (method == null) {
			return "";
		}
		return method.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').replaceAll("^lambda\\$", "step ");
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
