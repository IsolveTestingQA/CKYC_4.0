/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsFindings;
import Com.Ckyc_4_0.utils.dvs.DvsImageNameParser;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsScreenshot;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A1-A3: read-only audit of the open record before any scenario (no edits).
 * A1 empty mandatory fields and max-length breaks, A2 CSV to DVS compare (only when a CSV map is configured),
 * A3 image names and document label against the selected POI / POA.
 */
public final class DvsDataAudit {

	private static final DvsLocators L = DvsLocators.get();

	private DvsDataAudit() {
	}

	public static boolean enabled() {
		return DvsConfig.getBool("data.audit", true);
	}

	/** @return number of findings added */
	public static int run(String moduleCode, String customerId) {
		String prefix = "LE".equals(moduleCode) ? "LE" : "IND";
		int before = DvsFindings.all().size();
		Map<String, String> values = new LinkedHashMap<>();
		for (String tab : DvsFieldActions.recordTabNames()) {
			DvsFieldActions.openTab(tab);
			for (String id : L.idsForTab(prefix, tab)) {
				if (!DvsLocators.isInput(L.kind(id)) || !DvsFieldActions.present(id)) {
					continue;
				}
				String v = DvsFieldActions.read(id);
				values.put(id, v);
				auditField(customerId, tab, id, v);
			}
		}
		auditCsv(customerId, values);
		auditImages(moduleCode, customerId, values);
		int added = DvsFindings.all().size() - before;
		DvsStepLog.step("data-audit").field(customerId).actual(values.size() + " fields read, " + added + " findings")
				.result(added == 0 ? DvsStepLog.PASS : DvsStepLog.INFO).log();
		return added;
	}

	private static void auditField(String customerId, String tab, String id, String value) {
		DvsLocators.Meta m = L.meta(id);
		if (m == null) {
			return;
		}
		String mand = m.mandatory() == null ? "" : m.mandatory().trim();
		boolean empty = value == null || value.isBlank() || "false".equals(value) && "checkbox".equals(m.kind());
		if (empty && "M".equals(mand) && !"checkbox".equals(m.kind())) {
			DvsFindings.add(DvsFindings.DATA_AUDIT, "", customerId, tab, id, "Mandatory field is empty (source not known)",
					DvsScreenshot.capture("AUDIT_" + customerId, "empty-" + id));
		} else if (empty && mand.startsWith("M") && !"M".equals(mand) && !"checkbox".equals(m.kind())) {
			DvsFindings.add(DvsFindings.DATA_AUDIT, "", customerId, tab, id,
					"Conditional-mandatory (" + mand + ") field is empty - check whether the condition applies", "");
		}
		int max = parseInt(m.maxLen());
		if (max > 0 && value != null && value.length() > max) {
			DvsFindings.add(DvsFindings.DATA_AUDIT, "", customerId, tab, id,
					"Value length " + value.length() + " exceeds max " + max + ": '" + value + "'", "");
		}
	}

	/** A2 runs only when dvs.audit.csv and DVS/DVS_CsvMap.properties are both filled - no mapping is assumed. */
	private static void auditCsv(String customerId, Map<String, String> values) {
		String csv = DvsConfig.get("dvs.audit.csv", "");
		Map<String, String> map = csvMap();
		if (csv.isBlank() || map.isEmpty()) {
			DvsStepLog.step("audit-csv").actual("skipped: dvs.audit.csv or DVS_CsvMap.properties not configured")
					.result(DvsStepLog.INFO).log();
			return;
		}
		Path p = Paths.get(csv);
		if (!Files.exists(p)) {
			DvsStepLog.step("audit-csv").actual("CSV not found: " + p).result(DvsStepLog.INFO).log();
			return;
		}
		String keyColumn = DvsConfig.get("dvs.audit.csv.keyColumn", "CUSTOMER_REFERENCE_NUMBER");
		try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
			String[] header = br.readLine().split("\\|", -1);
			int keyIdx = -1;
			for (int i = 0; i < header.length; i++) {
				if (header[i].trim().equalsIgnoreCase(keyColumn)) {
					keyIdx = i;
				}
			}
			String line;
			while (keyIdx >= 0 && (line = br.readLine()) != null) {
				String[] cells = line.split("\\|", -1);
				if (cells.length <= keyIdx || !cells[keyIdx].trim().equalsIgnoreCase(customerId)) {
					continue;
				}
				for (Map.Entry<String, String> e : map.entrySet()) {
					for (int i = 0; i < header.length && i < cells.length; i++) {
						if (!header[i].trim().equalsIgnoreCase(e.getKey())) {
							continue;
						}
						String csvVal = cells[i].trim();
						String dvsVal = values.getOrDefault(e.getValue(), "");
						if (!csvVal.isEmpty() && !csvVal.equalsIgnoreCase(DvsFieldActions.codeOf(dvsVal)) && !csvVal.equalsIgnoreCase(dvsVal)) {
							DvsFindings.add(DvsFindings.BUG, "", customerId, "", e.getValue(),
									"CSV->DVS mapping: CSV " + e.getKey() + "='" + csvVal + "' but DVS shows '" + dvsVal + "'", "");
						} else if (csvVal.isEmpty() && "M".equals(L.meta(e.getValue()) == null ? "" : L.meta(e.getValue()).mandatory())) {
							DvsFindings.add(DvsFindings.BUG, "", customerId, "", e.getValue(),
									"Mandatory field empty in CSV and accepted at upload (" + e.getKey() + ")", "");
						}
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			DvsStepLog.step("audit-csv").actual("CSV compare failed: " + e.getMessage()).result(DvsStepLog.FAIL).log();
		}
	}

	/** A3: name pattern per IMAGE_NAME_RULES; image document must match the selected POI / POA document. */
	private static void auditImages(String moduleCode, String customerId, Map<String, String> values) {
		String leRule = DvsConfig.get("image.rule.le", "CURRENT");
		Map<String, String> alts = DvsRestoreManager.captureImages();
		for (Map.Entry<String, String> e : alts.entrySet()) {
			if (e.getKey().equals("IMG|count")) {
				continue;
			}
			String alt = e.getValue();
			int dash = alt.lastIndexOf(" - ");
			String file = dash > 0 ? alt.substring(0, dash).trim() : alt;
			String label = dash > 0 ? alt.substring(dash + 3).trim() : "";
			DvsImageNameParser.Result r = DvsImageNameParser.parse(file, customerId, moduleCode, leRule);
			if (r.verdict() != DvsImageNameParser.Verdict.VALID) {
				DvsFindings.add(DvsFindings.DATA_AUDIT, "", customerId, "Image", file,
						r.verdict() + " image name: " + r.message(), "");
			}
			String poi = DvsFieldActions.codeOf(values.getOrDefault("IND_POI_poiType", values.getOrDefault("LE_POI_poiType", "")));
			String docForPoi = docNameForPoiCode(poi);
			if (!label.isEmpty() && "POI".equalsIgnoreCase(r.type()) && !docForPoi.isEmpty()
					&& !label.toLowerCase(Locale.ROOT).contains(docForPoi.toLowerCase(Locale.ROOT))
					&& !docForPoi.toLowerCase(Locale.ROOT).contains(label.toLowerCase(Locale.ROOT))) {
				DvsFindings.add(DvsFindings.BUG, "", customerId, "Image", file,
						"Selected POI is '" + docForPoi + "' but the image label is '" + label + "'", "");
			}
		}
	}

	private static String docNameForPoiCode(String poiCode) {
		if (poiCode.isEmpty()) {
			return "";
		}
		try {
			for (Map<String, String> r : Com.Ckyc_4_0.utils.dvs.DvsExcelReader.readTable(DvsConfig.testDataPath(), "DOC_MASTER")) {
				if (poiCode.equalsIgnoreCase(r.getOrDefault("POI code", ""))) {
					return r.getOrDefault("Document", "");
				}
			}
		} catch (RuntimeException ignored) {
			// workbook not readable - label check is skipped
		}
		return "";
	}

	private static Map<String, String> csvMap() {
		Map<String, String> m = new LinkedHashMap<>();
		try (InputStream in = DvsDataAudit.class.getResourceAsStream("/DVS/DVS_CsvMap.properties")) {
			if (in == null) {
				return m;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				String t = line.trim();
				int eq = t.indexOf('=');
				if (!t.isEmpty() && !t.startsWith("#") && eq > 0) {
					m.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
				}
			}
		} catch (IOException e) {
			return m;
		}
		return m;
	}

	private static int parseInt(String s) {
		try {
			return Integer.parseInt(s == null ? "" : s.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
