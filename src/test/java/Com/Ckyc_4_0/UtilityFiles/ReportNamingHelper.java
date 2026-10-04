/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Unique report file names per run: date + time with AM/PM (never overwrites previous runs).
 */
public final class ReportNamingHelper {

	private static final String TIMESTAMP_PATTERN = "dd_MMM_yyyy_hh_mm_ss_a";
	private static final String DATE_PATTERN = "dd_MMM_yyyy";
	private static final String SHORT_DATE_PATTERN = "ddMMM";
	/** One timestamp per JVM run — Extent, Excel, screenshots stay aligned. */
	private static final String RUN_TIMESTAMP = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.ENGLISH)
			.format(new Date());
	private static final String RUN_DATE = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH)
			.format(new Date());
	/** Short token (e.g. 29Sep) used as a filename prefix so screenshots sort/search by date. */
	private static final String SHORT_DATE = new SimpleDateFormat(SHORT_DATE_PATTERN, Locale.ENGLISH)
			.format(new Date());
	/** Suite start time — used to compute "Total Time Taken" for the Excel summary sheet. */
	private static final long START_MILLIS = System.currentTimeMillis();
	private static volatile String runnerName = "CKYC";

	private ReportNamingHelper() {
	}

	public static String runTimestamp() {
		return RUN_TIMESTAMP;
	}

	public static void initializeRun(String name) {
		if (name != null && !name.isBlank()) {
			runnerName = name.replaceAll("[^A-Za-z0-9_-]", "_");
		}
	}

	public static String runDate() {
		return RUN_DATE;
	}

	public static String shortDateToken() {
		return SHORT_DATE;
	}

	/** Elapsed time since this JVM run started, formatted as "Xh Ym Zs" for the Excel Summary sheet. */
	public static String elapsedFormatted() {
		long totalSeconds = Math.max(0, System.currentTimeMillis() - START_MILLIS) / 1000;
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		StringBuilder sb = new StringBuilder();
		if (hours > 0) {
			sb.append(hours).append("h ");
		}
		if (hours > 0 || minutes > 0) {
			sb.append(minutes).append("m ");
		}
		sb.append(seconds).append("s");
		return sb.toString();
	}

	public static String runFolderName() {
		return runnerName + "_" + runTimestamp();
	}

	public static String extentReportPath(String configuredPath) {
		String baseDir = reportTypeRunDir("Extent");
		return baseDir + File.separator + "CKYC_ExtentReport_" + runTimestamp() + ".html";
	}

	public static String excelReportPath(String configuredPath) {
		String baseDir = reportTypeRunDir("Excel");
		return baseDir + File.separator + "TestExecutionReport_" + runTimestamp() + ".xlsx";
	}

	public static String dashboardDataExcelPath(String configuredExcelDir) {
		String baseDir = reportTypeRunDir("Excel");
		return baseDir + File.separator + "DashboardDataReport_" + runTimestamp() + ".xlsx";
	}

	public static String allureResultsPath() {
		return reportTypeRunDir("Allure Results");
	}

	/** Timestamped, never-overwritten folder for the generated (static) Allure HTML report. */
	public static String allureReportDir() {
		return reportTypeRunDir("Allure Report");
	}

	public static String cucumberJvmReportPath() {
		return reportTypeRunDir("Cucumber JVM Reports");
	}

	public static String executionLogDir() {
		return reportTypeRunDir("Execution Logs");
	}

	/** One shared folder per calendar date (not per runner) so every screenshot for the day is in one place. */
	public static String screenshotDir() {
		return reportOutputRoot() + File.separator + "Screenshots" + File.separator + RUN_DATE;
	}

	public static String reportOutputRoot() {
		return new File(System.getProperty("user.dir"), "Report Output").getAbsolutePath();
	}

	public static String currentDataRunFolder() {
		String base = System.getProperty("user.dir");
		String root = ConfigReader.getCurrentDataPath();
		File dir = new File(root.startsWith(File.separator) || root.contains(":") ? root : base + File.separator + root);
		return dir.getAbsolutePath() + File.separator + "Dashboard" + File.separator + "run_" + runTimestamp();
	}

	private static String reportTypeRunDir(String reportType) {
		return reportOutputRoot() + File.separator + reportType + File.separator
				+ RUN_DATE + File.separator + runFolderName();
	}
}
