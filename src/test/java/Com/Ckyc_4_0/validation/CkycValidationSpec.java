/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.validation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code ckyc_cosmos_field_spec.json} (+ the CERSAI masters CSVs) from
 * {@code src/test/resources/CkycValidation/} and exposes it in a form {@link CkycRecordValidator}
 * can use directly. Mirrors the module-level globals at the top of {@code ckyc_validate.py} (see
 * CKYC_OFFICIAL_DOCUMENTS/CKYC_Cosmos_Validation_Spec) — same source of truth, same field names.
 *
 * <p><b>The {@code config} block (ORG_CODE, ORG_NAME, customer-reference-number patterns, ...) in
 * the bundled JSON is Cosmos Co-op Bank's values.</b> The rule engine itself (formats, checksums,
 * cross-field logic) is CERSAI-standard and bank-agnostic; only {@code config} needs to change to
 * validate a different bank's data — see {@link #withConfigOverride(Map)}.
 */
public final class CkycValidationSpec {

	private static final String RESOURCE_ROOT = "CkycValidation/";
	private static volatile CkycValidationSpec DEFAULT_INSTANCE;

	public record ColumnSpec(String name, String status, String when, Integer maxLen, Integer maxLenDb,
			String format, List<String> enumValues, String packFill, String block) {
	}

	public record PersonaSpec(String header, int columnCount, List<ColumnSpec> columns) {
	}

	public record DocEntry(String doc, String poiCode, String poiCategory, String poaCode, String poaCategory,
			String numberFormat, String rpIdColumn, String imageFile, boolean minorAllowed,
			List<String> constitutions) {
	}

	public record FormatSpec(String regex, String checksum, String master, String desc) {
	}

	public record DeemedEntry(String code, String category) {
	}

	public record LeConstitutionRule(String nameSuffix, String pan4th, String relType) {
	}

	private final JSONObject root;
	private final Map<String, String> config;
	private final Map<String, FormatSpec> formats = new HashMap<>();
	private final List<DocEntry> docMap = new ArrayList<>();
	private final Map<String, DocEntry> poiToDoc = new HashMap<>();
	private final Map<String, DocEntry> poaToDoc = new HashMap<>();
	private final Map<String, String> imageToPoaCode = new HashMap<>();
	private final Map<String, String> poaCodeToRpIdColumn = new HashMap<>();
	private final Set<String> rpIdColumns = new LinkedHashSet<>();
	private final Map<String, DocEntry> leCodeToDoc = new HashMap<>(); // poi/poa code (02/06/03/07) -> le_doc_map entry
	private final Map<String, DeemedEntry> deemedMap = new HashMap<>();
	private final Map<String, String> poaName = new HashMap<>(); // code -> category name (IND + LE)
	private final Map<String, String> gstStateCode = new HashMap<>();
	private final Map<String, LeConstitutionRule> leConstitutionRules = new HashMap<>();
	private final Map<String, Map<String, String>> numberFormatByCode = new HashMap<>();
	private final Map<String, PersonaSpec> personas = new HashMap<>();

	private final Map<String, Map<String, String>> pincodeMaster = new HashMap<>(); // PIN -> {DISTRICT, STATE_CODE}
	private final Set<String> stateCodes = new HashSet<>();
	private final Set<String> countryCodes = new HashSet<>();
	private final Set<String> impairmentCodes = new HashSet<>();

	private CkycValidationSpec(JSONObject root, Map<String, String> configOverride) {
		this.root = root;
		this.config = configOverride != null ? configOverride : flattenConfig(root.getJSONObject("config"));
		loadFormats();
		loadDocMaps();
		loadDeemedMap();
		loadPoaNames();
		loadGstStateCode();
		loadLeConstitutionRules();
		loadNumberFormatByCode();
		loadPersonas();
		loadCsvMasters();
	}

	/** Default instance — Cosmos Co-op Bank config, bundled in resources. */
	public static CkycValidationSpec getDefault() {
		if (DEFAULT_INSTANCE == null) {
			synchronized (CkycValidationSpec.class) {
				if (DEFAULT_INSTANCE == null) {
					DEFAULT_INSTANCE = new CkycValidationSpec(loadJson("ckyc_cosmos_field_spec.json"), null);
				}
			}
		}
		return DEFAULT_INSTANCE;
	}

	/**
	 * Same rule engine, a different bank's {@code config} (ORG_CODE, ORG_NAME, and
	 * {@code customer_ref.<PERSONA>} regex keys — see {@link #customerRefPattern(String)}).
	 * Use this once the target bank's actual values are known; do not guess them.
	 */
	public static CkycValidationSpec withConfigOverride(Map<String, String> configOverride) {
		return new CkycValidationSpec(loadJson("ckyc_cosmos_field_spec.json"), configOverride);
	}

	/**
	 * Same rule engine, config read from {@code config.properties} (ORG_CODE/ORG_NAME/BRID
	 * confirmed from the real Kotak sample data; the customer-reference-number patterns are still
	 * placeholders — see {@code ckycCustomerRefPattern*} in config.properties and
	 * {@link Com.Ckyc_4_0.UtilityFiles.ConfigReader#ckycCustomerRefPattern(String)}).
	 */
	public static CkycValidationSpec forConfiguredBank() {
		Map<String, String> override = new HashMap<>();
		override.put("ORG_CODE", Com.Ckyc_4_0.UtilityFiles.ConfigReader.ckycOrgCode());
		override.put("ORG_NAME", Com.Ckyc_4_0.UtilityFiles.ConfigReader.ckycOrgName());
		override.put("BRID", Com.Ckyc_4_0.UtilityFiles.ConfigReader.ckycBrid());
		for (String persona : new String[] { "IND", "MIN", "LE", "RP" }) {
			override.put("customer_ref." + persona, Com.Ckyc_4_0.UtilityFiles.ConfigReader.ckycCustomerRefPattern(persona));
		}
		return withConfigOverride(override);
	}

	private static JSONObject loadJson(String resourceName) {
		try (InputStream in = CkycValidationSpec.class.getClassLoader()
				.getResourceAsStream(RESOURCE_ROOT + resourceName)) {
			if (in == null) {
				throw new IllegalStateException("Missing resource: " + RESOURCE_ROOT + resourceName);
			}
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return new JSONObject(text);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load " + resourceName, e);
		}
	}

	private static Map<String, String> flattenConfig(JSONObject cfg) {
		Map<String, String> out = new HashMap<>();
		for (String key : cfg.keySet()) {
			Object v = cfg.get(key);
			if (v instanceof String s) {
				out.put(key, s);
			} else if (v instanceof JSONObject nested && "customer_ref_pattern".equals(key)) {
				for (String p : nested.keySet()) {
					out.put("customer_ref." + p, nested.getString(p));
				}
			}
			// other nested blocks (pack_folders, file_name_pattern, pack_dates) are not needed
			// for single-record field validation and are intentionally not flattened here.
		}
		return out;
	}

	// ------------------------------------------------------------------ loaders
	private void loadFormats() {
		JSONObject f = root.getJSONObject("formats");
		for (String name : f.keySet()) {
			JSONObject spec = f.getJSONObject(name);
			formats.put(name, new FormatSpec(spec.optString("regex", null), spec.optString("checksum", null),
					spec.optString("master", null), spec.optString("desc", null)));
		}
	}

	private void loadDocMaps() {
		JSONArray arr = root.getJSONArray("doc_map");
		for (int i = 0; i < arr.length(); i++) {
			JSONObject o = arr.getJSONObject(i);
			DocEntry d = new DocEntry(o.getString("doc"), o.optString("poi_code", ""), o.optString("poi_category", ""),
					o.optString("poa_code", ""), o.optString("poa_category", ""), o.optString("number_format", ""),
					o.optString("rp_id_column", ""), o.optString("image_file", ""), o.optBoolean("minor_allowed", false),
					List.of());
			docMap.add(d);
			if (!d.poiCode().isEmpty()) {
				poiToDoc.put(d.poiCode(), d);
			}
			if (!d.poaCode().isEmpty()) {
				poaToDoc.put(d.poaCode(), d);
				poaCodeToRpIdColumn.put(d.poaCode(), d.rpIdColumn());
			}
			if (!d.imageFile().isEmpty() && !d.poaCode().isEmpty()) {
				imageToPoaCode.put(d.imageFile(), d.poaCode());
			}
			if (!d.rpIdColumn().isEmpty()) {
				rpIdColumns.add(d.rpIdColumn());
			}
		}
		JSONArray le = root.getJSONArray("le_doc_map");
		for (int i = 0; i < le.length(); i++) {
			JSONObject o = le.getJSONObject(i);
			List<String> constitutions = new ArrayList<>();
			if (o.has("constitutions")) {
				JSONArray ca = o.getJSONArray("constitutions");
				for (int j = 0; j < ca.length(); j++) {
					constitutions.add(ca.getString(j));
				}
			}
			DocEntry d = new DocEntry(o.getString("doc"), o.optString("poi_code", ""), o.optString("category", ""),
					o.optString("poa_code", ""), o.optString("category", ""), o.optString("number_format", ""), "",
					o.optString("image_file", ""), false, constitutions);
			if (!d.poiCode().isEmpty()) {
				leCodeToDoc.put(d.poiCode(), d);
			}
			if (!d.poaCode().isEmpty()) {
				leCodeToDoc.put(d.poaCode(), d);
			}
		}
	}

	private void loadDeemedMap() {
		JSONArray arr = root.getJSONArray("deemed_map");
		for (int i = 0; i < arr.length(); i++) {
			JSONObject o = arr.getJSONObject(i);
			deemedMap.put(o.getString("code"), new DeemedEntry(o.getString("code"), o.optString("category", "")));
		}
	}

	private void loadPoaNames() {
		JSONObject masters = root.getJSONObject("masters");
		for (String key : new String[] { "POA_IND", "POA_LE" }) {
			if (!masters.has(key)) {
				continue;
			}
			JSONObject m = masters.getJSONObject(key);
			for (String code : m.keySet()) {
				poaName.put(code, m.getString(code));
			}
		}
	}

	private void loadGstStateCode() {
		JSONObject m = root.getJSONObject("gst_state_code");
		for (String code : m.keySet()) {
			gstStateCode.put(code, m.getString(code));
		}
	}

	private void loadLeConstitutionRules() {
		JSONObject m = root.getJSONObject("le_constitution_rules");
		for (String ct : m.keySet()) {
			JSONObject o = m.getJSONObject(ct);
			leConstitutionRules.put(ct, new LeConstitutionRule(o.optString("name_suffix", ""),
					o.optString("pan_4th", ""), o.optString("rel_type", "")));
		}
	}

	private void loadNumberFormatByCode() {
		JSONObject m = root.getJSONObject("number_format_by_code");
		for (String group : m.keySet()) {
			JSONObject g = m.getJSONObject(group);
			Map<String, String> inner = new HashMap<>();
			for (String code : g.keySet()) {
				inner.put(code, g.getString(code));
			}
			numberFormatByCode.put(group, inner);
		}
	}

	private void loadPersonas() {
		JSONObject p = root.getJSONObject("personas");
		for (String persona : p.keySet()) {
			JSONObject spec = p.getJSONObject(persona);
			List<ColumnSpec> cols = new ArrayList<>();
			JSONArray colsArr = spec.getJSONArray("columns");
			for (int i = 0; i < colsArr.length(); i++) {
				JSONObject c = colsArr.getJSONObject(i);
				List<String> enumValues = null;
				if (c.has("enum") && !c.isNull("enum")) {
					enumValues = new ArrayList<>();
					JSONArray ea = c.getJSONArray("enum");
					for (int j = 0; j < ea.length(); j++) {
						enumValues.add(ea.getString(j));
					}
				}
				cols.add(new ColumnSpec(c.getString("name"), c.getString("status"), c.optString("when", null),
						c.isNull("max_len") ? null : c.optInt("max_len"),
						c.has("max_len_db") && !c.isNull("max_len_db") ? c.optInt("max_len_db") : null,
						c.optString("format", null), enumValues, c.optString("pack_fill", null),
						c.optString("block", "APPLICANT")));
			}
			personas.put(persona, new PersonaSpec(spec.getString("header"), spec.getInt("column_count"), cols));
		}
	}

	private void loadCsvMasters() {
		for (Map<String, String> row : readCsvResource("masters/pincode_master.csv")) {
			pincodeMaster.put(row.get("PIN"), row);
		}
		for (Map<String, String> row : readCsvResource("masters/state_master.csv")) {
			stateCodes.add(row.get("CODE"));
		}
		for (Map<String, String> row : readCsvResource("masters/country_code_iso3166.csv")) {
			countryCodes.add(row.get("CODE"));
		}
		for (Map<String, String> row : readCsvResource("masters/type_of_impairment.csv")) {
			impairmentCodes.add(row.get("CODE"));
		}
	}

	private static List<Map<String, String>> readCsvResource(String relativePath) {
		List<Map<String, String>> rows = new ArrayList<>();
		try (InputStream in = CkycValidationSpec.class.getClassLoader()
				.getResourceAsStream(RESOURCE_ROOT + relativePath);
				BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String headerLine = br.readLine();
			if (headerLine == null) {
				return rows;
			}
			if (headerLine.startsWith("﻿")) {
				headerLine = headerLine.substring(1);
			}
			String[] headers = splitCsvLine(headerLine);
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] values = splitCsvLine(line);
				Map<String, String> row = new LinkedHashMap<>();
				for (int i = 0; i < headers.length && i < values.length; i++) {
					row.put(headers[i].trim(), values[i].trim());
				}
				rows.add(row);
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read " + relativePath, e);
		}
		return rows;
	}

	private static String[] splitCsvLine(String line) {
		// Simple comma split with basic double-quote support — sufficient for these masters files
		// (no embedded newlines; commas only appear inside quotes, if at all).
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '"') {
				inQuotes = !inQuotes;
			} else if (ch == ',' && !inQuotes) {
				out.add(cur.toString());
				cur.setLength(0);
			} else {
				cur.append(ch);
			}
		}
		out.add(cur.toString());
		return out.toArray(new String[0]);
	}

	// ------------------------------------------------------------------ accessors
	public String config(String key) {
		return config.get(key);
	}

	public String customerRefPattern(String persona) {
		return config.get("customer_ref." + persona);
	}

	public FormatSpec format(String name) {
		return formats.get(name);
	}

	public PersonaSpec persona(String code) {
		PersonaSpec p = personas.get(code);
		if (p == null) {
			throw new IllegalArgumentException("Unknown persona '" + code + "' — expected IND, MIN, or LE");
		}
		return p;
	}

	public DocEntry docByPoiCode(String code) {
		return poiToDoc.get(code);
	}

	public DocEntry docByPoaCode(String code) {
		return poaToDoc.get(code);
	}

	public String poaCodeForImage(String imageFile) {
		return imageToPoaCode.get(imageFile);
	}

	public String rpIdColumnForPoaCode(String poaCode) {
		return poaCodeToRpIdColumn.get(poaCode);
	}

	public Set<String> rpIdColumns() {
		return rpIdColumns;
	}

	public DocEntry leDocByCode(String code) {
		return leCodeToDoc.get(code);
	}

	public DeemedEntry deemed(String code) {
		return deemedMap.get(code);
	}

	public boolean isDeemed(String code) {
		return deemedMap.containsKey(code);
	}

	public String poaCategoryName(String code) {
		return poaName.get(code);
	}

	public String gstStateCode(String stateCode) {
		return gstStateCode.get(stateCode);
	}

	public LeConstitutionRule leConstitutionRule(String constType) {
		return leConstitutionRules.get(constType);
	}

	public String numberFormatByCode(String group, String code) {
		Map<String, String> inner = numberFormatByCode.get(group);
		return inner == null ? null : inner.getOrDefault(code, "ANY");
	}

	public Map<String, String> pincodeMaster(String pin) {
		return pincodeMaster.get(pin);
	}

	public boolean isValidStateCode(String code) {
		return stateCodes.contains(code);
	}

	public boolean isValidCountryCode(String code) {
		return countryCodes.contains(code);
	}

	public boolean isValidImpairmentCode(String code) {
		return impairmentCodes.contains(code);
	}
}
