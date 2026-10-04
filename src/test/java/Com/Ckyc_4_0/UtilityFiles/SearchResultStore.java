/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

/**
 * Search-to-Download handoff. Keeps the latest successful identifiers in memory
 * and under Current Data/Search without writing unmasked customer details.
 */
public final class SearchResultStore {

	private static final Logger logger = LoggerFactory.getLogger(SearchResultStore.class);
	private static volatile SearchResult latest;

	private SearchResultStore() {
	}

	public record SearchResult(String maskedCkycNumber, String ckycReferenceId,
			String searchedPan, String searchedMobile, String capturedAt) {
	}

	public static synchronized void save(String maskedCkycNumber, String ckycReferenceId,
			String searchedPan, String searchedMobile) {
		String capturedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date());
		latest = new SearchResult(maskedCkycNumber, ckycReferenceId, searchedPan, searchedMobile, capturedAt);

		Properties data = new Properties();
		data.setProperty("maskedCkycNumber", safe(maskedCkycNumber));
		data.setProperty("ckycReferenceId", safe(ckycReferenceId));
		// Alias retained for the future Download feature terminology.
		data.setProperty("ckycReferenceIdentifier", safe(ckycReferenceId));
		data.setProperty("searchedPan", safe(searchedPan));
		data.setProperty("searchedMobile", safe(searchedMobile));
		data.setProperty("capturedAt", capturedAt);

		File root = resolveCurrentDataRoot();
		File runDir = new File(root, "Search" + File.separator + "run_" + ReportNamingHelper.runTimestamp());
		File latestDir = new File(root, "Search" + File.separator + "latest");
		write(data, new File(runDir, "search-result.properties").toPath());
		write(data, new File(latestDir, "search-result.properties").toPath());
	}

	public static SearchResult latest() {
		return latest;
	}

	public static String latestFilePath() {
		return new File(resolveCurrentDataRoot(),
				"Search" + File.separator + "latest" + File.separator + "search-result.properties")
				.getAbsolutePath();
	}

	private static File resolveCurrentDataRoot() {
		String configured = ConfigReader.getCurrentDataPath();
		File root = new File(configured);
		return root.isAbsolute() ? root : new File(System.getProperty("user.dir"), configured);
	}

	private static void write(Properties data, Path path) {
		try {
			Files.createDirectories(path.getParent());
			try (FileOutputStream output = new FileOutputStream(path.toFile())) {
				data.store(new java.io.OutputStreamWriter(output, StandardCharsets.UTF_8),
						"CKYC Search result for future Download automation");
			}
			logger.info("Search-to-Download data saved: {}", path.toAbsolutePath());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to persist Search result at " + path, e);
		}
	}

	private static String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
