/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/** "Last positive" field values of one customer: Locator_ID to value. Negatives restore to this. */
public final class DvsSnapshot {

	private final Map<String, String> values = new LinkedHashMap<>();
	/** Full Maker snapshot (C1): key tab|Locator_ID, RP|row-n, IMG|n - used for the Maker to Checker compare. */
	private final Map<String, String> full = new LinkedHashMap<>();

	public Map<String, String> full() {
		return full;
	}

	public Map<String, String> values() {
		return values;
	}

	public void put(String locatorId, String value) {
		values.put(locatorId, value == null ? "" : value);
	}

	public String get(String locatorId) {
		return values.get(locatorId);
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public static Path fileFor(String customerId) {
		return Paths.get(DvsResultWriter.resultsRoot(), "LastPositive_" + customerId + ".json");
	}

	public void save(String customerId) {
		try {
			Path p = fileFor(customerId);
			Files.createDirectories(p.getParent());
			Files.writeString(p, new JSONObject(values).toString(2), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot save last-positive snapshot for " + customerId, e);
		}
	}

	public static DvsSnapshot load(String customerId) {
		DvsSnapshot s = new DvsSnapshot();
		Path p = fileFor(customerId);
		if (!Files.exists(p)) {
			return s;
		}
		try {
			JSONObject o = new JSONObject(Files.readString(p, StandardCharsets.UTF_8));
			for (String k : o.keySet()) {
				s.values.put(k, o.optString(k, ""));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read last-positive snapshot for " + customerId, e);
		}
		return s;
	}
}
