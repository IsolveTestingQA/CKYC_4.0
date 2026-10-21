/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.recording;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.AdvancedRunReport;
import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.CucumberReport;
import Com.Ckyc_4_0.UtilityFiles.DashboardDataExcelReport;
import Com.Ckyc_4_0.UtilityFiles.ExcelReport;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;

/**
 * The end of a run, after the reports were generated: a completion toast in the browser, the final screenshot (page and
 * whole screen), the screen recording is finished, and one block in the console with clickable links to every result
 * (IntelliJ turns the {@code file:///} lines into links). Runs once; safe to call again.
 */
public final class RunFinale {

	private static final Logger logger = LoggerFactory.getLogger(RunFinale.class);

	private static boolean done;
	private static String finalWindowShot = "";
	private static String finalScreenShot = "";
	private static String recordingPath = "";

	private RunFinale() {
	}

	/** Toast + final screenshots + stop the recording. Call before the Advanced report is written. */
	public static synchronized void completeRun() {
		if (done) {
			return;
		}
		done = true;
		WebDriver driver = BaseClass.getDriver();
		String total = ReportNamingHelper.elapsedFormatted();
		String exec = ReportNamingHelper.executionElapsedFormatted();
		boolean recording = RunRecording.isRecording();
		String message = "✔ Test run completed  |  total time " + total + "  |  test execution " + exec
				+ "  |  reports saved in Report Output" + (recording ? "  |  screen recording saved" : "");
		if (driver != null) {
			try {
				toast(driver, message);
				Thread.sleep(3000); // the toast stays on screen (and in the recording) for a moment
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (RuntimeException e) {
				logger.debug("Completion toast skipped: {}", e.toString());
			}
			finalWindowShot = pageShot(driver);
			if (Boolean.parseBoolean(ConfigReader.get("finalScreenshot.fullScreen", "true"))) {
				finalScreenShot = fullScreenShot();
			}
		}
		File recorded = RunRecording.stop();
		recordingPath = recorded == null ? "" : recorded.getAbsolutePath();
		logger.info("Run finished | total time {} | test execution {}", total, exec);
	}

	public static String finalWindowShot() {
		return finalWindowShot;
	}

	public static String finalScreenShot() {
		return finalScreenShot;
	}

	public static String recordingPath() {
		return recordingPath;
	}

	// ------------------------------------------------------------------ console summary

	/** One block of clickable links. Call after every report exists (before they are opened). */
	public static void printSummary() {
		List<String[]> rows = new ArrayList<>();
		rows.add(new String[] { "Total time taken", ReportNamingHelper.elapsedFormatted() });
		rows.add(new String[] { "Test execution time", ReportNamingHelper.executionElapsedFormatted() });
		rows.add(new String[] { "Started", ReportNamingHelper.startedFormatted() });
		rows.add(new String[] { "Finished", ReportNamingHelper.nowFormatted() });
		StringBuilder sb = new StringBuilder(System.lineSeparator());
		sb.append("==================== RUN SUMMARY - Tested by ").append(ConfigReader.testedBy())
				.append(" ====================").append(System.lineSeparator());
		for (String[] r : rows) {
			sb.append(String.format(" %-22s: %s%n", r[0], r[1]));
		}
		sb.append(System.lineSeparator()).append(" Click a link to open it (Ctrl+click in the IntelliJ console):")
				.append(System.lineSeparator());
		link(sb, "Advanced report (HTML)", AdvancedRunReport.get().writtenTo());
		link(sb, "Excel execution report", safe(() -> ExcelReport.getInstance().getExcelPath()));
		link(sb, "Dashboard data Excel", safe(() -> DashboardDataExcelReport.getInstance().getExcelPath()));
		link(sb, "Extent report (HTML)", ExtentReportManager.getCurrentReportPath());
		link(sb, "Cucumber report (HTML)", CucumberReport.lastIndex());
		link(sb, "Execution log (HTML)", ExecutionLogger.getHtmlLogPath());
		link(sb, "Execution log (text)", ExecutionLogger.getTextLogPath());
		link(sb, "Screenshots folder", ReportNamingHelper.screenshotDir());
		link(sb, "Final window screenshot", finalWindowShot);
		link(sb, "Final full-screen shot", finalScreenShot);
		link(sb, "Screen recording (video)", recordingPath);
		sb.append("=====================================================================").append(System.lineSeparator());
		logger.info(sb.toString());
	}

	private static void link(StringBuilder sb, String label, String path) {
		if (path == null || path.isBlank()) {
			return;
		}
		Path p = Paths.get(path);
		if (!Files.exists(p)) {
			return;
		}
		sb.append(String.format(" %-24s %s%n", label, p.toAbsolutePath().toUri()));
	}

	private static String safe(java.util.function.Supplier<String> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			return "";
		}
	}

	// ------------------------------------------------------------------ screenshots

	private static void toast(WebDriver driver, String message) {
		String js = "var id='ckyc-final-toast';var o=document.getElementById(id);if(o){o.remove();}"
				+ "var d=document.createElement('div');d.id=id;d.textContent=" + JSONObject.quote(message) + ";"
				+ "d.style.cssText='position:fixed;left:50%;bottom:36px;transform:translateX(-50%);z-index:2147483647;"
				+ "background:#0b2a6f;color:#fff;padding:14px 22px;border-radius:12px;"
				+ "font:600 15px Segoe UI,Arial,sans-serif;box-shadow:0 8px 28px rgba(0,0,0,.35);max-width:92vw;"
				+ "text-align:center;line-height:1.4;';document.documentElement.appendChild(d);";
		((JavascriptExecutor) driver).executeScript(js);
	}

	private static String pageShot(WebDriver driver) {
		try {
			byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
			File dir = new File(ReportNamingHelper.screenshotDir());
			dir.mkdirs();
			File f = new File(dir, "Final_Window_" + ReportNamingHelper.runTimestamp() + ".png");
			Files.write(f.toPath(), png);
			return f.getAbsolutePath();
		} catch (Exception e) {
			logger.debug("Final window screenshot skipped: {}", e.toString());
			return "";
		}
	}

	/** Whole primary screen (browser window included); skipped on machines without a display. */
	private static String fullScreenShot() {
		try {
			if (GraphicsEnvironment.isHeadless() || Boolean.parseBoolean(ConfigReader.get("headless", "false"))) {
				return "";
			}
			Rectangle area = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			BufferedImage img = new Robot().createScreenCapture(area);
			File dir = new File(ReportNamingHelper.screenshotDir());
			dir.mkdirs();
			File f = new File(dir, "Final_Screen_" + ReportNamingHelper.runTimestamp() + ".png");
			ImageIO.write(img, "png", f);
			return f.getAbsolutePath();
		} catch (Exception | Error e) {
			logger.debug("Final full-screen screenshot skipped: {}", e.toString());
			return "";
		}
	}
}
