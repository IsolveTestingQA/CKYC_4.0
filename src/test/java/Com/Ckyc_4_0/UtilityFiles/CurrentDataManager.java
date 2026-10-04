/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Persists dashboard UI snapshots under {@code Current Data/Dashboard/} for later comparison.
 * One JSON file per filter combination per run.
 */
public final class CurrentDataManager {

	private static final Logger logger = LoggerFactory.getLogger(CurrentDataManager.class);
	private static final String RUN_FOLDER = ReportNamingHelper.currentDataRunFolder();

	private CurrentDataManager() {
	}

	public static String getRunFolderPath() {
		return RUN_FOLDER;
	}

	public static void saveDashboardSnapshot(String dateFilter, String constitutionFilter, String fromDate,
			String toDate, List<DashboardCountStore.DashboardDataRow> rows) {
		if (rows == null || rows.isEmpty()) {
			logger.warn("No dashboard rows to save for {}/{}", dateFilter, constitutionFilter);
			return;
		}

		try {
			File runDir = new File(RUN_FOLDER);
			if (!runDir.exists()) {
				runDir.mkdirs();
			}

			String safeName = dateFilter + "_" + constitutionFilter.replace(" ", "_") + ".json";
			File outFile = new File(runDir, safeName);

			JSONObject root = new JSONObject();
			root.put("product", "iFlowCKYC 4.0.0");
			root.put("module", "Dashboard");
			root.put("runTimestamp", ReportNamingHelper.runTimestamp());
			root.put("capturedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date()));
			root.put("dateFilter", dateFilter);
			root.put("constitutionFilter", constitutionFilter);
			root.put("fromDate", fromDate == null ? "" : fromDate);
			root.put("toDate", toDate == null ? "" : toDate);

			JSONArray summaryCards = new JSONArray();
			JSONArray subItems = new JSONArray();
			Integer mismatchDelta = null;

			for (DashboardCountStore.DashboardDataRow row : rows) {
				JSONObject item = new JSONObject();
				item.put("cardGroup", row.cardGroup());
				item.put("code", row.code());
				item.put("title", row.title());
				item.put("count", row.count());
				if (row.formula() != null && !row.formula().isBlank()) {
					item.put("formula", row.formula());
				}

				if ("SUMMARY".equals(row.section())) {
					summaryCards.put(item);
				} else if ("SUB_ITEM".equals(row.section())) {
					subItems.put(item);
				} else if ("MISMATCH".equals(row.section())) {
					mismatchDelta = row.count();
				}
			}

			root.put("summaryCards", summaryCards);
			root.put("subItems", subItems);
			if (mismatchDelta != null) {
				root.put("mismatchDelta", mismatchDelta);
			}

			try (FileWriter writer = new FileWriter(outFile, StandardCharsets.UTF_8)) {
				writer.write(root.toString(2));
			}

			updateLatestCopy(dateFilter, constitutionFilter, root);
			logger.info("Current Data saved: {}", outFile.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Failed to save Current Data for {}/{}: {}", dateFilter, constitutionFilter, e.getMessage(), e);
		}
	}

	/** Latest snapshot per filter combo — easy baseline for comparison. */
	private static void updateLatestCopy(String dateFilter, String constitutionFilter, JSONObject root)
			throws Exception {
		File latestDir = new File(ConfigReader.getCurrentDataPath() + File.separator + "Dashboard" + File.separator
				+ "latest");
		if (!latestDir.exists()) {
			latestDir.mkdirs();
		}
		String safeName = dateFilter + "_" + constitutionFilter.replace(" ", "_") + ".json";
		File latestFile = new File(latestDir, safeName);
		Files.writeString(latestFile.toPath(), root.toString(2), StandardCharsets.UTF_8);
	}
}
