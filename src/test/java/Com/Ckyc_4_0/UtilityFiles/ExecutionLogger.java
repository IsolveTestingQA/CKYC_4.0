/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-run execution log: timestamped plaintext + HTML (failures in red/bold).
 * Complements Extent / Allure / Excel / screenshots — does not replace them.
 */
public final class ExecutionLogger {

	private static final DateTimeFormatter LINE_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private static final AtomicBoolean STARTED = new AtomicBoolean(false);
	private static Path textLog;
	private static Path htmlLog;
	private static BufferedWriter textOut;
	private static BufferedWriter htmlOut;

	private ExecutionLogger() {
	}

	public static synchronized void start(String runnerName) {
		if (STARTED.get()) {
			info("Suite already logging | runner=" + runnerName);
			return;
		}
		try {
			Path dir = Paths.get(ReportNamingHelper.executionLogDir());
			Files.createDirectories(dir);
			// Align with Extent/Excel run stamp from ReportNamingHelper
			String stamp = ReportNamingHelper.runTimestamp();
			textLog = dir.resolve("ckyc-run_" + stamp + ".txt");
			htmlLog = dir.resolve("ckyc-run_" + stamp + ".html");
			textOut = Files.newBufferedWriter(textLog, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			htmlOut = new BufferedWriter(new OutputStreamWriter(
					Files.newOutputStream(htmlLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
					StandardCharsets.UTF_8));
			htmlOut.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>"
					+ "<title>CKYC run " + escape(stamp) + "</title>"
					+ "<style>body{font-family:Consolas,monospace;background:#111;color:#eee;padding:16px}"
					+ ".info{color:#ccc}.pass{color:#7CFC00}.warn{color:#FFD700}"
					+ ".fail,.exception{color:#ff4d4d;font-weight:bold}"
					+ "pre{white-space:pre-wrap;margin:2px 0}</style></head><body>");
			htmlOut.write("<h2>CKYC execution log — " + escape(stamp) + "</h2>");
			STARTED.set(true);
			info("=== RUN START | runner=" + runnerName + " | text=" + textLog.toAbsolutePath()
					+ " | html=" + htmlLog.toAbsolutePath() + " ===");
		} catch (IOException e) {
			STARTED.set(false);
			System.err.println("ExecutionLogger failed to start: " + e.getMessage());
		}
	}

	public static void info(String message) {
		write("INFO", message, "info");
	}

	public static void pass(String message) {
		write("PASS", message, "pass");
	}

	public static void warn(String message) {
		write("WARN", message, "warn");
	}

	public static void fail(String message) {
		write("FAILED", "***** FAILED ***** " + message, "fail");
	}

	public static void exception(String message, Throwable t) {
		String detail = message;
		if (t != null) {
			detail += " | " + t.getClass().getSimpleName() + ": "
					+ (t.getMessage() == null ? t.toString() : t.getMessage());
		}
		write("EXCEPTION", "***** EXCEPTION ***** " + detail, "exception");
	}

	public static synchronized void close() {
		if (!STARTED.get()) {
			return;
		}
		try {
			info("=== RUN END ===");
			if (htmlOut != null) {
				htmlOut.write("</body></html>");
				htmlOut.flush();
				htmlOut.close();
			}
			if (textOut != null) {
				textOut.flush();
				textOut.close();
			}
		} catch (IOException ignored) {
		} finally {
			STARTED.set(false);
			textOut = null;
			htmlOut = null;
		}
	}

	public static String getTextLogPath() {
		return textLog == null ? "" : textLog.toAbsolutePath().toString();
	}

	public static String getHtmlLogPath() {
		return htmlLog == null ? "" : htmlLog.toAbsolutePath().toString();
	}

	private static synchronized void write(String level, String message, String cssClass) {
		if (!STARTED.get()) {
			return;
		}
		String ts = LocalDateTime.now().format(LINE_TS);
		String line = ts + " [" + level + "] " + message;
		try {
			if (textOut != null) {
				textOut.write(line);
				textOut.newLine();
				textOut.flush();
			}
			if (htmlOut != null) {
				htmlOut.write("<pre class=\"" + cssClass + "\">" + escape(line) + "</pre>");
				htmlOut.flush();
			}
		} catch (IOException e) {
			System.err.println("ExecutionLogger write failed: " + e.getMessage());
		}
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
