/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.bulkimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * What the CKYC field spec (CkycValidation/ckyc_cosmos_field_spec.json) says about the CSV columns of a persona: the
 * human label, whether the column is mandatory (M), and the code -> description master (GENDER M = Male ...). It is only
 * a helper for the field mapping: the application's own labels and values stay the reference.
 */
public final class CkycSpec {

	public record Col(String name, String label, String status, String section) {
		public boolean mandatory() {
			return "M".equals(status);
		}
	}

	private final Map<String, Col> columns = new LinkedHashMap<>();
	private final JSONObject masters;
	private final String persona;

	private CkycSpec(String persona, JSONObject root) {
		this.persona = persona;
		this.masters = root.optJSONObject("masters") == null ? new JSONObject() : root.getJSONObject("masters");
		JSONObject p = root.optJSONObject("personas") == null ? null : root.getJSONObject("personas").optJSONObject(persona);
		JSONArray cols = p == null ? null : p.optJSONArray("columns");
		for (int i = 0; cols != null && i < cols.length(); i++) {
			JSONObject c = cols.getJSONObject(i);
			String n = c.optString("name");
			columns.put(n.toUpperCase(Locale.ROOT), new Col(n, c.optString("label"), c.optString("status"), c.optString("section")));
		}
	}

	/** @param persona IND, MIN or LE (from the file name: IN -> IND, LE -> LE) */
	public static CkycSpec load(String persona) {
		try (InputStream in = CkycSpec.class.getResourceAsStream("/CkycValidation/ckyc_cosmos_field_spec.json")) {
			if (in == null) {
				return new CkycSpec(persona, new JSONObject());
			}
			return new CkycSpec(persona, new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
		} catch (IOException | RuntimeException e) {
			return new CkycSpec(persona, new JSONObject());
		}
	}

	public String persona() {
		return persona;
	}

	public Col column(String name) {
		return name == null ? null : columns.get(name.toUpperCase(Locale.ROOT));
	}

	public String label(String column) {
		Col c = column(column);
		return c == null ? "" : c.label();
	}

	public boolean mandatory(String column) {
		Col c = column(column);
		return c != null && c.mandatory();
	}

	private static final Map<String, String[]> ALIAS = new HashMap<>();

	static {
		ALIAS.put("TYPE_OF_DOC", new String[] { "TYPE_OF_DOC_IND", "TYPE_OF_DOC_LE" });
		ALIAS.put("PERM_ADD_PROOF", new String[] { "POA_IND", "POA_LE" });
		ALIAS.put("POA_TYPE", new String[] { "POA_IND", "POA_LE" });
		ALIAS.put("POI_TYPE", new String[] { "IDENTITY_CODE_IND", "IDENTITY_CODE_LE" });
		ALIAS.put("CONST_TYPE", new String[] { "CONST_TYPE_IND", "CONST_TYPE_LE" });
		ALIAS.put("PERM_TO_COMM_FLG", new String[] { "YN", "YN" });
		ALIAS.put("MINOR", new String[] { "YN", "YN" });
	}

	/** Description of a code for a column from the spec masters, or "" when the spec has none. Text in brackets is cut. */
	public String describe(String column, String code) {
		if (column == null || code == null) {
			return "";
		}
		String key = column.toUpperCase(Locale.ROOT);
		String[] alias = ALIAS.get(key);
		String master = key;
		if (alias != null) {
			master = "LE".equals(persona) ? alias[1] : alias[0];
		}
		JSONObject m = masters.optJSONObject(master);
		if (m == null) {
			return "";
		}
		String d = m.optString(code.trim(), "");
		int cut = d.indexOf(" (");
		return (cut > 0 ? d.substring(0, cut) : d).trim();
	}
}
