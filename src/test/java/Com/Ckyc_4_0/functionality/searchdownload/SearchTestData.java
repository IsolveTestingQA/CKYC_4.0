/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.searchdownload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads read-only Search fixtures kept separately from runtime credentials. */
final class SearchTestData {

	private static final Properties DATA = load();

	private SearchTestData() {
	}

	static String get(String key) {
		String value = DATA.getProperty(key, "").trim();
		if (value.isEmpty()) {
			throw new IllegalStateException("Missing Search test-data key: " + key
					+ " in TestData/search-temp.properties");
		}
		return value;
	}

	private static Properties load() {
		Properties properties = new Properties();
		try (InputStream input = SearchTestData.class.getClassLoader()
				.getResourceAsStream("TestData/search-temp.properties")) {
			if (input == null) {
				throw new IllegalStateException("Search test-data file not found: TestData/search-temp.properties");
			}
			properties.load(input);
			return properties;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load Search test data", e);
		}
	}
}
