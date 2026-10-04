/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Opens the HTML report(s) in the default browser right after a run finishes — controlled by
 * {@code openExtentReportAfterRun} / {@code openAllureReportAfterRun} in config.properties.
 *
 * Extent is a real standalone HTML file — it just opens.
 *
 * Allure only ships raw JSON "results"; turning that into a page needs the {@code allure}
 * command-line tool to generate a static HTML report. That static report is then served over
 * a tiny local HTTP server (JDK's built-in {@code jwebserver}) instead of {@code mvn allure:serve}
 * — {@code allure:serve} ties the report to a live Maven process in a terminal (closing the
 * terminal kills the report); a detached {@code jwebserver} keeps serving a proper, timestamped,
 * named report folder independently, so it stays open whether you launched it automatically here
 * or ran {@code open-allure.ps1} yourself later.
 */
public final class ReportOpener {

	private static final Logger logger = LoggerFactory.getLogger(ReportOpener.class);

	private ReportOpener() {
	}

	public static void openReportsIfConfigured() {
		if (ConfigReader.openExtentReportAfterRun()) {
			openFile(ExtentReportManager.getCurrentReportPath(), "Extent");
		}
		if (ConfigReader.openAllureReportAfterRun()) {
			openAllureReport();
		}
	}

	/** Opens a local file in the default browser. Windows `cmd /c start` first (most reliable
	 *  on this project's target OS), {@link Desktop#browse} as a fallback. */
	static void openFile(String path, String label) {
		if (path == null || path.isBlank()) {
			logger.warn("{} report path is unknown — nothing to open (was the report ever created?)", label);
			return;
		}
		File file = new File(path);
		if (!file.exists()) {
			logger.warn("{} report not found to open: {}", label, path);
			return;
		}
		if (openWithWindowsShell(file)) {
			logger.info("Opened {} report: {}", label, path);
			return;
		}
		if (openWithDesktop(file)) {
			logger.info("Opened {} report: {}", label, path);
			return;
		}
		logger.warn("Could not auto-open {} report — open it manually: {}", label, path);
	}

	private static boolean openWithWindowsShell(File file) {
		try {
			// "" is a required dummy window-title arg for `start` when the path may contain spaces.
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath());
			pb.redirectErrorStream(true);
			Process process = pb.start();
			process.waitFor(5, TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			logger.debug("Windows shell open failed, will try Desktop: {}", e.getMessage());
			return false;
		}
	}

	private static boolean openWithDesktop(File file) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(file.toURI());
				return true;
			}
		} catch (Exception e) {
			logger.debug("Desktop browse failed: {}", e.getMessage());
		}
		return false;
	}

	private static void openAllureReport() {
		try {
			String resultsDir = System.getProperty("user.dir") + File.separator + "target" + File.separator
					+ "allure-results";
			if (!new File(resultsDir).exists()) {
				logger.debug("No target/allure-results yet — skipping Allure auto-open");
				return;
			}
			String reportDir = ReportNamingHelper.allureReportDir();

			boolean generated = generateWithAllureCli(resultsDir, reportDir) || generateWithMaven(reportDir);
			if (!generated) {
				logger.info("Could not auto-generate the Allure report (no `allure` CLI and no `mvn` reachable) "
						+ "— run open-allure.ps1 to view it.");
				return;
			}

			int port = findFreePort();
			if (port <= 0) {
				logger.warn("No free port found for the Allure report server — open manually: {}", reportDir);
				return;
			}
			String jwebserver = System.getProperty("java.home") + File.separator + "bin" + File.separator
					+ "jwebserver.exe";
			if (!new File(jwebserver).exists()) {
				jwebserver = "jwebserver"; // fall back to PATH resolution
			}
			ProcessBuilder serve = new ProcessBuilder(jwebserver, "-b", "127.0.0.1", "-p", String.valueOf(port),
					"-d", reportDir);
			serve.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			serve.redirectError(ProcessBuilder.Redirect.DISCARD);
			serve.start(); // detached — deliberately not waited on; keeps serving after this JVM exits

			Thread.sleep(600); // give the tiny server a moment to bind before we open the browser
			String url = "http://127.0.0.1:" + port + "/";
			if (!openUrlWithWindowsShell(url)) {
				try {
					Desktop.getDesktop().browse(new java.net.URI(url));
				} catch (Exception e) {
					logger.warn("Allure report generated ({}) but could not auto-open — visit {}", reportDir, url);
					return;
				}
			}
			logger.info("Opened Allure report: {} (serving from {})", url, reportDir);
		} catch (Exception e) {
			logger.warn("Allure auto-open skipped: {}", e.getMessage());
		}
	}

	/** Preferred path: a standalone `allure` executable on PATH. */
	private static boolean generateWithAllureCli(String resultsDir, String reportDir) {
		String allureCli = findAllureCli();
		if (allureCli == null) {
			return false;
		}
		boolean ok = runProcess(new String[] { allureCli, "generate", resultsDir, "-o", reportDir, "--clean" }, 60);
		if (ok) {
			logger.debug("Allure report generated via standalone CLI: {}", reportDir);
		}
		return ok;
	}

	/**
	 * Fallback: this project's own `allure-maven` plugin already knows how to fetch and drive the
	 * Allure commandline tool (its {@code resultsDirectory} is configured in pom.xml as
	 * target/allure-results, which is exactly what this run just populated). Running just the
	 * `report` goal (not `serve`) generates a static site with no blocking server, to its default
	 * location {@code target/site/allure-maven-plugin}, which is then copied into our own
	 * timestamped {@code reportDir} so it is named/dated like every other report and never
	 * overwritten. First run downloads the Allure commandline distribution, so this can take a
	 * while (up to the timeout below) — subsequent runs are faster.
	 */
	private static boolean generateWithMaven(String reportDir) {
		String mvn = findMavenExecutable();
		if (mvn == null) {
			return false;
		}
		boolean ok = runProcess(new String[] { mvn, "-q", "allure:report" }, 180);
		if (!ok) {
			return false;
		}
		File defaultOut = new File(System.getProperty("user.dir"), "target" + File.separator + "site"
				+ File.separator + "allure-maven-plugin");
		if (!defaultOut.exists()) {
			logger.warn("mvn allure:report ran but the expected output folder was not found: {}", defaultOut);
			return false;
		}
		try {
			FileUtils.copyDirectory(defaultOut, new File(reportDir));
			logger.debug("Allure report generated via mvn allure:report, copied to: {}", reportDir);
			return true;
		} catch (IOException e) {
			logger.warn("Could not copy generated Allure report into {}: {}", reportDir, e.getMessage());
			return false;
		}
	}

	private static boolean runProcess(String[] cmd, int timeoutSeconds) {
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(new File(System.getProperty("user.dir")));
			pb.redirectErrorStream(true);
			pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			Process process = pb.start();
			boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
			return finished && process.exitValue() == 0;
		} catch (Exception e) {
			logger.debug("Process {} failed: {}", String.join(" ", cmd), e.getMessage());
			return false;
		}
	}

	private static String findMavenExecutable() {
		for (String var : new String[] { "MAVEN_HOME", "M2_HOME" }) {
			String home = System.getenv(var);
			if (home == null) {
				continue;
			}
			for (String candidate : new String[] { "mvn.cmd", "mvn.bat", "mvn" }) {
				File f = new File(home, "bin" + File.separator + candidate);
				if (f.isFile()) {
					return f.getAbsolutePath();
				}
			}
		}
		String path = System.getenv("PATH");
		if (path == null) {
			return null;
		}
		for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
			for (String candidate : new String[] { "mvn.cmd", "mvn.bat", "mvn" }) {
				File f = new File(dir, candidate);
				if (f.isFile()) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}

	private static boolean openUrlWithWindowsShell(String url) {
		try {
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", url);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			process.waitFor(5, TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (Exception e) {
			return -1;
		}
	}

	private static String findAllureCli() {
		String path = System.getenv("PATH");
		if (path == null) {
			return null;
		}
		String[] candidates = { "allure.bat", "allure.cmd", "allure" };
		for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
			for (String candidate : candidates) {
				File f = new File(dir, candidate);
				if (f.isFile()) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}
}
