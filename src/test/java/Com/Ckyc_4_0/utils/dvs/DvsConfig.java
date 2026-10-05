/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Lookup order: -D system property, config.properties, CONFIG sheet of the DVS test-data workbook, default.
 * Existing ConfigReader is only read, never changed.
 */
public final class DvsConfig {

	public static final String TEST_DATA_PATH_KEY = "dvs.testdata.path";
	private static final String DEFAULT_TEST_DATA = "src/test/resources/TestData/DVS/DVS_Automation_TestData.xlsx";

	private static Map<String, String> sheetConfig;

	private DvsConfig() {
	}

	public static synchronized String get(String key, String defaultValue) {
		String v = System.getProperty(key);
		if (v != null && !v.isBlank()) {
			return v.trim();
		}
		try {
			v = ConfigReader.get(key, "");
			if (v != null && !v.isBlank()) {
				return v.trim();
			}
		} catch (RuntimeException | LinkageError ignored) {
			// config.properties key missing - fall through to sheet value
		}
		v = sheet().get(key);
		return v == null || v.isBlank() ? defaultValue : v.trim();
	}

	public static int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(get(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static boolean getBool(String key, boolean defaultValue) {
		String v = get(key, String.valueOf(defaultValue));
		return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("yes");
	}

	public static Path testDataPath() {
		return Paths.get(get(TEST_DATA_PATH_KEY, DEFAULT_TEST_DATA));
	}

	public static synchronized void reload() {
		sheetConfig = null;
	}

	private static Map<String, String> sheet() {
		if (sheetConfig == null) {
			Path p = testDataPath();
			sheetConfig = Files.exists(p) ? DvsExcelReader.readConfig(p) : Map.of();
		}
		return sheetConfig;
	}
}
