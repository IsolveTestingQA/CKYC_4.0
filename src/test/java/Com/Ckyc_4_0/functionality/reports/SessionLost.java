/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import java.util.Locale;

/** The browser window / driver session is gone: the run must stop and keep what it already read. */
public final class SessionLost extends RuntimeException {

	public SessionLost(String message, Throwable cause) {
		super(message, cause);
	}

	public static boolean is(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			String m = c.getMessage() == null ? "" : c.getMessage().toLowerCase(Locale.ROOT);
			if (c instanceof org.openqa.selenium.NoSuchSessionException || c instanceof org.openqa.selenium.NoSuchWindowException
					|| m.contains("invalid session id") || m.contains("no such window") || m.contains("disconnected")
					|| m.contains("target window already closed") || m.contains("session deleted")) {
				return true;
			}
			if (c.getCause() == c) {
				break;
			}
		}
		return false;
	}
}
