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

	public static void generateJVMReports(String runnerName) {
		String normalizedRunner = runnerName == null ? "" : runnerName.toLowerCase();
		String suffix = normalizedRunner.contains("searchanddownload")
				? "cucumber-search-download.json"
				: normalizedRunner.contains("search")
						? "cucumber-search.json"
				: normalizedRunner.contains("masters")
				? "cucumber-masters.json"
				: normalizedRunner.contains("dashboard")
						? "cucumber-dashboard.json"
						: "cucumber.json";
		File jsonReport = new File("target/cucumber", suffix);
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
		Configuration configuration = new Configuration(reportOutputDir, "CKYC 4.0");
		configuration.addPresentationModes(net.masterthought.cucumber.presentation.PresentationMode.EXPAND_ALL_STEPS);
		configuration.setBuildNumber("1.0-SNAPSHOT");

		List<String> jsonFiles = new ArrayList<>();
		jsonFiles.add(jsonReport.getAbsolutePath());

		ReportBuilder reportBuilder = new ReportBuilder(jsonFiles, configuration);
		reportBuilder.generateReports();
		logger.info("Cucumber JVM report generated at {}", reportOutputDir.getAbsolutePath());
	}
}
