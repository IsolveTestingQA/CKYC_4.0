/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.recording;

import java.io.File;
import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;

/**
 * Screen recording of one run. OFF by default. It is switched on by (highest priority first):
 * the Test Control Panel toggle, the runner class constant {@code RECORD_SCREEN = true}, or {@code recording.enabled=true}.
 * The file is {@code Report Output/Screen Recordings/<date>/<runner>_<date_time>.avi}.
 * Config (all optional): recording.fps (5), recording.maxWidth (1280), recording.quality (0.6).
 */
public final class RunRecording {

	private static final Logger logger = LoggerFactory.getLogger(RunRecording.class);

	private static ScreenRecorder recorder;
	private static File lastFile;
	private static String runnerName = "CKYC";
	private static boolean hookInstalled;

	private RunRecording() {
	}

	/** Runner constant OR config key - the panel, when shown, overrides this afterwards. */
	public static boolean requestedByRunnerOrConfig(boolean runnerFlag) {
		return runnerFlag || Boolean.parseBoolean(ConfigReader.get("recording.enabled", "false"));
	}

	public static synchronized boolean isRecording() {
		return recorder != null;
	}

	public static synchronized File lastFile() {
		return lastFile;
	}

	/** Starts recording the automation browser (call after the browser is open). Safe to call twice. */
	public static synchronized void start(String runner) {
		if (recorder != null) {
			return;
		}
		try {
			runnerName = runner == null || runner.isBlank() ? "CKYC" : runner.replaceAll("[^A-Za-z0-9_-]", "_");
			File dir = new File(ReportNamingHelper.reportOutputRoot() + File.separator + "Screen Recordings"
					+ File.separator + ReportNamingHelper.runDate());
			File file = new File(dir, runnerName + "_" + ReportNamingHelper.runTimestamp() + ".avi");
			int fps = Integer.parseInt(ConfigReader.get("recording.fps", "5"));
			int maxWidth = Integer.parseInt(ConfigReader.get("recording.maxWidth", "1280"));
			float quality = Float.parseFloat(ConfigReader.get("recording.quality", "0.6"));
			recorder = new ScreenRecorder(file, RunRecording::currentTarget, fps, maxWidth, quality, runnerName);
			lastFile = file;
			recorder.start();
			installShutdownHook();
		} catch (RuntimeException e) {
			recorder = null;
			logger.warn("Screen recording could not be started: {}", e.toString());
		}
	}

	/** Stops and keeps the file. Returns the file, or null when nothing was recorded. */
	public static synchronized File stop() {
		if (recorder == null) {
			return null;
		}
		ScreenRecorder r = recorder;
		recorder = null;
		try {
			File f = r.stop();
			lastFile = f;
			return f;
		} catch (RuntimeException e) {
			logger.warn("Screen recording could not be finished cleanly: {}", e.toString());
			return null;
		}
	}

	/** Stops and deletes (the tester switched the toggle off in the panel). */
	public static synchronized void discard() {
		File f = lastFile;
		stop();
		if (f != null) {
			try {
				f.delete();
			} catch (RuntimeException ignored) {
				// nothing to keep
			}
		}
		lastFile = null;
		logger.info("Screen recording switched off in the Test Control Panel - file discarded");
	}

	private static void installShutdownHook() {
		if (hookInstalled) {
			return;
		}
		hookInstalled = true;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				stop(); // JVM ending (Stop button, crash): still produce a playable file
			} catch (RuntimeException ignored) {
				// ending anyway
			}
		}, "ckyc-recording-finalize"));
	}

	/** The DevTools address of the automation browser and the id of its current window; null when no browser. */
	@SuppressWarnings("unchecked")
	private static ScreenRecorder.Target currentTarget() {
		WebDriver driver = BaseClass.getDriver();
		if (driver == null || !(driver instanceof HasCapabilities)) {
			return null;
		}
		try {
			Capabilities caps = ((HasCapabilities) driver).getCapabilities();
			Object options = caps.getCapability("goog:chromeOptions");
			if (options == null) {
				options = caps.getCapability("ms:edgeOptions");
			}
			if (!(options instanceof Map)) {
				return null;
			}
			Object address = ((Map<String, Object>) options).get("debuggerAddress");
			if (address == null) {
				return null;
			}
			String id = null;
			try {
				id = driver.getWindowHandle().replace("CDwindow-", "");
			} catch (RuntimeException ignored) {
				// no current window yet - the first page is used
			}
			return new ScreenRecorder.Target(String.valueOf(address), id);
		} catch (RuntimeException e) {
			return null;
		}
	}
}
