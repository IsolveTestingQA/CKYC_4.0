/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class CucumberReport {

	private static final Logger logger = LoggerFactory.getLogger(CucumberReport.class);

	private CucumberReport() {
	}

	private static volatile String lastIndex = "";

	/** The Cucumber HTML report's start page of the last generated report ("" when none was generated). */
	public static String lastIndex() {
		return lastIndex;
	}

	/**
	 * The runner's own JSON when the name is known, otherwise the newest Cucumber JSON of this run - so every
	 * runner (Login, Dashboard, Reports, Search, DVS ...) gets its Cucumber HTML report, not only four of them.
	 */
	private static File pickJson(String runnerName) {
		String n = runnerName == null ? "" : runnerName.toLowerCase();
		String known = n.contains("searchanddownload") ? "cucumber-search-download.json"
				: n.contains("searchinteractive") ? "cucumber-searchinteractive.json"
				: n.contains("search") ? "cucumber-search.json"
				: n.contains("masters") ? "cucumber-masters.json"
				: n.contains("dashboard") ? "cucumber-dashboard.json"
				: n.contains("controlpanel") ? "cucumber-controlpanel.json"
				: n.contains("reports") ? "cucumber-reports.json"
				: n.contains("login") ? "cucumber-login.json" : "cucumber.json";
		File dir = new File("target/cucumber");
		File exact = new File(dir, known);
		if (exact.exists() && exact.length() > 0L) {
			return exact;
		}
		File[] all = dir.listFiles((d, name) -> name.endsWith(".json"));
		File newest = null;
		if (all != null) {
			for (File f : all) {
				if (f.length() > 0L && (newest == null || f.lastModified() > newest.lastModified())) {
					newest = f;
				}
			}
		}
		return newest != null ? newest : exact;
	}

	public static void generateJVMReports(String runnerName) {
		File jsonReport = pickJson(runnerName);
		if (!jsonReport.exists() || jsonReport.length() == 0L) {
			logger.warn("Cucumber JSON report not found at {}. Skipping JVM report generation.", jsonReport.getAbsolutePath());
			return;
		}

		File reportOutputDir = new File(ReportNamingHelper.cucumberJvmReportPath());
		if (!reportOutputDir.exists()) {
			reportOutputDir.mkdirs();
		}
		String safeRunner = runnerName == null ? "CKYC"
				: runnerName.replaceAll("[^A-Za-z0-9_-]", "_");
		File archivedJson = new File(reportOutputDir,
				"Cucumber_" + safeRunner + "_" + ReportNamingHelper.runTimestamp() + ".json");
		try {
			Files.copy(jsonReport.toPath(), archivedJson.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			logger.warn("Could not archive Cucumber JSON into Report Output: {}", e.getMessage());
		}
		Configuration configuration = new Configuration(reportOutputDir, "iFlowCKYC 4.0 - Automation");
		configuration.addPresentationModes(net.masterthought.cucumber.presentation.PresentationMode.EXPAND_ALL_STEPS);
		configuration.setBuildNumber(ReportNamingHelper.runTimestamp());
		configuration.addClassifications("Tested By", ConfigReader.testedBy());
		configuration.addClassifications("Application", "iFlowCKYC 4.0.0");
		configuration.addClassifications("Environment", ConfigReader.getUrl());
		configuration.addClassifications("Browser", ConfigReader.get("browser", "chrome"));
		configuration.addClassifications("Runner", runnerName == null ? "" : runnerName);
		configuration.addClassifications("What was tested", AdvancedRunReport.selectionText());
		configuration.addClassifications("Run date", ReportNamingHelper.runDate());
		configuration.addClassifications("Total time taken", ReportNamingHelper.elapsedFormatted());

		List<String> jsonFiles = new ArrayList<>();
		jsonFiles.add(jsonReport.getAbsolutePath());

		ReportBuilder reportBuilder = new ReportBuilder(jsonFiles, configuration);
		reportBuilder.generateReports();
		File index = new File(reportOutputDir, "cucumber-html-reports" + File.separator + "overview-features.html");
		lastIndex = index.exists() ? index.getAbsolutePath() : "";
		logger.info("Cucumber JVM report generated at {} (open: {})", reportOutputDir.getAbsolutePath(), lastIndex);
	}
}
