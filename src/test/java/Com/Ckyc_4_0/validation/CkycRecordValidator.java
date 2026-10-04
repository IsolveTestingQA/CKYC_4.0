/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.validation;

import Com.Ckyc_4_0.validation.CkycValidationSpec.ColumnSpec;
import Com.Ckyc_4_0.validation.CkycValidationSpec.DeemedEntry;
import Com.Ckyc_4_0.validation.CkycValidationSpec.DocEntry;
import Com.Ckyc_4_0.validation.CkycValidationSpec.FormatSpec;
import Com.Ckyc_4_0.validation.CkycValidationSpec.LeConstitutionRule;
import Com.Ckyc_4_0.validation.CkycValidationSpec.PersonaSpec;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Single-record CKYC/CERSAI V1.3 field validator — Java port of {@code ckyc_validate.py}'s
 * {@code check_fields} / {@code applicant_rules} / {@code rp_rules} / {@code entity_rules} /
 * {@code check_record} (see CKYC_OFFICIAL_DOCUMENTS/CKYC_Cosmos_Validation_Spec). Same rule ids
 * and messages as the Python reference, so a finding here means the same thing there.
 *
 * <p>Deliberately scoped to "is this one CSV record valid" — the pack-level checks in the Python
 * reference that need a whole extracted pack folder (image files on disk, cross-record
 * uniqueness, DVS/IMAGE_PROCESSING expectations, filename/header structural checks) are not
 * ported here; add them separately if/when that context is available.
 *
 * <p>Not thread-safe for concurrent calls to {@link #validateRecord} on the same instance — each
 * call uses a fresh, unshared issue list internally, so separate instances (or sequential calls)
 * are fine; this project's test execution is sequential throughout (see SoftAssertManager /
 * MasterTestContext for the same assumption elsewhere in the codebase).
 */
public final class CkycRecordValidator {

	private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	private static final String[] PERM = { "PERM_ADD_LINE1", "PERM_ADD_LINE2", "PERM_ADD_LINE3", "PERM_ADD_CITY",
			"PERM_ADD_DIST", "PERM_ADD_STATE", "PERM_ADD_COUNTRY", "PERM_ADD_PIN" };
	private static final String[] COMM_IND = { "COMM_Add_Line1", "COMM_Add_Line2", "COMM_Add_Line3", "COMM_Add_City",
			"COMM_Add_Dist", "COMM_Add_State", "COMM_Add_Country", "COMM_Add_Pin" };
	private static final String[] COMM_LE = { "COMM_ADD_LINE1", "COMM_ADD_LINE2", "COMM_ADD_LINE3", "COMM_ADD_CITY",
			"COMM_ADD_DIST", "COMM_ADD_STATE", "COMM_ADD_COUNTRY", "COMM_ADD_PIN" };
	private static final String[] RPP = { "REL_PER_ADD_LINE1", "REL_PER_ADD_LINE2", "REL_PER_ADD_LINE3",
			"REL_PER_ADD_CITY", "REL_PER_ADD_DISTRICT", "REL_PER_ADD_STATE", "REL_PER_ADD_COUNTRY",
			"REL_PER_ADD_PIN" };
	private static final String[] RPC = { "REL_PER_CURR_ADD_LINE1", "REL_PER_CURR_ADD_LINE2",
			"REL_PER_CURR_ADD_LINE3", "REL_PER_CURR_ADD_CITY", "REL_PER_CURR_ADD_DISTRICT",
			"REL_PER_CURR_ADD_STATE", "REL_PER_CURR_ADD_COUNTRY", "REL_PER_CURR_ADD_PIN" };

	private final CkycValidationSpec spec;

	public CkycRecordValidator(CkycValidationSpec spec) {
		this.spec = spec;
	}

	public CkycRecordValidator() {
		this(CkycValidationSpec.getDefault());
	}

	// ------------------------------------------------------------------ entry point
	/**
	 * @param persona           "IND" | "MIN" | "LE"
	 * @param applicantRow      the applicant/entity CSV row (column name -> value) — for LE, this
	 *                          is row 1 (entity + related-person-1 combined)
	 * @param relatedPersonRows for LE, the <b>continuation</b> rows only — RP2, RP3, ... — never
	 *                          including {@code applicantRow} itself (its entity columns are blank
	 *                          on these rows; that is what gets checked here). Ignored for MIN
	 *                          (its RP1 data lives in {@code applicantRow} itself, not a separate
	 *                          row) and for IND (no RP block). Pass an empty list when there are no
	 *                          continuation rows.
	 * @param withRp            true for LE always, and for Minor only when this record actually
	 *                          carries guardian/RP data (category "02_Minor_With_RP")
	 * @param positivePack      false only for a DVS-pack record (relaxes PACK-severity findings)
	 * @param uploadDate        the date this record is/will be uploaded (today, normally)
	 */
	public List<ValidationIssue> validateRecord(String persona, Map<String, String> applicantRow,
			List<Map<String, String>> relatedPersonRows, boolean withRp, boolean positivePack,
			LocalDate uploadDate) {
		List<ValidationIssue> out = new ArrayList<>();
		PersonaSpec personaSpec = spec.persona(persona);
		Map<String, String> skipBlock = withRp ? Map.of() : Map.of("RP", "minor without RP");

		checkFields(out, applicantRow, personaSpec.columns(), skipBlock, positivePack, applicantRow);
		List<Map<String, String>> leAllRows = new ArrayList<>();
		leAllRows.add(applicantRow);
		leAllRows.addAll(relatedPersonRows);

		String cust = applicantRow.getOrDefault("CUSTOMER_REFERENCE_NUMBER", "");
		if ("MIN".equals(persona) && !withRp
				&& !cust.equals(applicantRow.getOrDefault("CUST_ID", ""))) {
			out.add(new ValidationIssue("ERROR", "CUST-01", "CUST_ID", applicantRow.getOrDefault("CUST_ID", ""),
					"must equal CUSTOMER_REFERENCE_NUMBER"));
		}
		if ("MIN".equals(persona) && withRp && !"1".equals(applicantRow.getOrDefault("REL_TYPE", ""))) {
			out.add(new ValidationIssue("ERROR", "MINRP-01", "REL_TYPE", applicantRow.getOrDefault("REL_TYPE", ""),
					"Minor with RP needs REL_TYPE 1 (Guardian)"));
		}
		String custPattern = spec.customerRefPattern(persona);
		if (custPattern != null && !Pattern.matches(custPattern, cust == null ? "" : cust)) {
			out.add(new ValidationIssue("WARN", "UNIQ-01", "CUSTOMER_REFERENCE_NUMBER", cust,
					"pack pattern " + custPattern));
		}

		if ("LE".equals(persona)) {
			entityRules(out, applicantRow, leAllRows, uploadDate);
			for (Map<String, String> row : relatedPersonRows) { // continuation rows only — never row 1
				checkFields(out, row, personaSpec.columns(), Map.of("ENTITY", "LE continuation row"), positivePack,
						row);
			}
		} else {
			applicantRules(out, applicantRow, persona, uploadDate);
			if ("MIN".equals(persona) && withRp
					&& !applicantRow.getOrDefault("PERM_ADD_PIN", "").equals(applicantRow.getOrDefault("REL_PER_ADD_PIN", ""))) {
				out.add(new ValidationIssue("WARN", "ADDR-04", "REL_PER_ADD_PIN",
						applicantRow.getOrDefault("REL_PER_ADD_PIN", ""),
						"guardian lives with the minor (same permanent address)"));
			}
		}

		if (withRp) {
			int n = 1;
			List<Map<String, String>> rpRowsForLoop = "LE".equals(persona) ? leAllRows : List.of(applicantRow);
			for (Map<String, String> row : rpRowsForLoop) {
				rpRules(out, row, n, persona, cust, uploadDate);
				n++;
			}
		}

		return out;
	}

	// ------------------------------------------------------------------ formats / enum
	boolean fmtOk(String fname, String v) {
		if (fname == null || fname.isEmpty() || "ANY".equals(fname)) {
			return true;
		}
		if ("CIN_OR_LLPIN".equals(fname)) {
			return fmtOk("CIN", v) || fmtOk("LLPIN", v);
		}
		FormatSpec f = spec.format(fname);
		if (f == null) {
			return true;
		}
		if (f.master() != null) {
			return f.master().startsWith("state") ? spec.isValidStateCode(v) : spec.isValidCountryCode(v);
		}
		if (v == null || !Pattern.matches(f.regex(), v)) {
			return false;
		}
		if ("verhoeff".equals(f.checksum())) {
			return Checksums.verhoeff(v);
		}
		if ("gstin".equals(f.checksum())) {
			return Checksums.gstinOk(v);
		}
		if ("REG_CERT".equals(fname) && !v.isEmpty() && Character.isDigit(v.charAt(0))) {
			return Checksums.gstinOk(v);
		}
		return true;
	}

	private String resolveFormat(ColumnSpec c, Map<String, String> r) {
		String f = c.format();
		if (f == null) {
			return null;
		}
		return switch (f) {
		case "BY_POI_TYPE" -> spec.numberFormatByCode("POI_TYPE", r.getOrDefault("POI_TYPE", ""));
		case "BY_POA_TYPE" -> spec.numberFormatByCode("POA_TYPE", r.getOrDefault("POA_TYPE", ""));
		case "BY_COMM_IDTYPE" -> spec.numberFormatByCode("COMM_ADDRESS_IDTYPE", r.getOrDefault("COMM_ADDRESS_IDTYPE", ""));
		case "BY_LE_POI_TYPE" -> spec.numberFormatByCode("LE_POI_TYPE", r.getOrDefault("POI_TYPE", ""));
		case "BY_LE_POA_TYPE" -> spec.numberFormatByCode("LE_POA_TYPE", r.getOrDefault("POA_TYPE", ""));
		default -> f;
		};
	}

	private boolean enumOk(ColumnSpec c, String v) {
		if (c.enumValues() == null || c.enumValues().isEmpty()) {
			return true;
		}
		for (String e : c.enumValues()) {
			if (e.startsWith("CONFIG:")) {
				if (v.equals(spec.config(e.substring(7)))) {
					return true;
				}
			} else if (e.startsWith("MASTER:")) {
				if (e.contains("type_of_impairment")) {
					if (spec.isValidImpairmentCode(v)) {
						return true;
					}
				} else {
					return true; // district masters are checked by ADDR-01 (pin_check)
				}
			} else if (v.equals(e)) {
				return true;
			}
		}
		return false;
	}

	private String cond(String expr, Map<String, String> r) {
		if (expr == null || expr.isBlank()) {
			return "False";
		}
		try {
			boolean v = SpecExpr.eval(expr, r, args -> callSpecFunction(args[0], args[1], r));
			return v ? "True" : "False";
		} catch (Exception e) {
			return "False";
		}
	}

	private boolean condBool(String expr, Map<String, String> r) {
		return "True".equals(cond(expr, r));
	}

	private boolean callSpecFunction(String name, String arg, Map<String, String> r) {
		if ("no_other_rp_id".equals(name)) {
			for (String col : spec.rpIdColumns()) {
				if (!"REL_PER_UID".equals(col) && !r.getOrDefault(col, "").isEmpty()) {
					return false;
				}
			}
			return true;
		}
		if ("rp_doc_used".equals(name)) {
			return rpDocCodes(r).contains(arg);
		}
		throw new IllegalArgumentException("Unknown spec function: " + name);
	}

	private java.util.Set<String> rpDocCodes(Map<String, String> r) {
		java.util.Set<String> codes = new java.util.HashSet<>();
		String a = r.getOrDefault("REL_PER_ADD_PROF", "");
		String b = r.getOrDefault("REL_PER_CURR_ADD_PROOF", "");
		if (!a.isEmpty()) {
			codes.add(a);
		}
		if (!b.isEmpty()) {
			codes.add(b);
		}
		String img = r.getOrDefault("REL_PER_POI_NAME", "").replaceFirst("^RP\\d+_", "");
		String code = spec.poaCodeForImage(img);
		if (code != null) {
			codes.add(code);
		}
		return codes;
	}

	// ------------------------------------------------------------------ field-level checks
	private void checkFields(List<ValidationIssue> out, Map<String, String> r, List<ColumnSpec> cols,
			Map<String, String> skipBlock, boolean positive, Map<String, String> rForPackFillCond) {
		for (ColumnSpec c : cols) {
			String name = c.name();
			String v = r.getOrDefault(name, "");
			if (skipBlock.containsKey(c.block())) {
				if (!v.isEmpty() && !"CUST_ID".equals(name)) {
					out.add(new ValidationIssue("ERROR", "FIELD-BLANK", name, v,
							"must be blank on this row type (" + skipBlock.get(c.block()) + ")"));
				}
				continue;
			}
			String st = c.status();
			if ("BLANK_FOR_NEW".equals(st) || "SYSTEM_BLANK".equals(st)) {
				if (!v.isEmpty()) {
					out.add(new ValidationIssue("ERROR", "FIELD-BLANK", name, v, st + ": must be blank for NEW upload"));
				}
				continue;
			}
			if (v.isEmpty()) {
				if ("M".equals(st)) {
					out.add(new ValidationIssue("ERROR", "FIELD-M", name, "", "mandatory field is blank"));
				} else if ("CM".equals(st) && condBool(c.when(), r)) {
					out.add(new ValidationIssue("ERROR", "FIELD-CM", name, "",
							"conditional mandatory (" + c.when() + ") is blank"));
				} else if (positive) {
					String pf = c.packFill();
					if ("ALWAYS".equals(pf) || (pf != null && !"BLANK".equals(pf) && condBool(pf, rForPackFillCond))) {
						out.add(new ValidationIssue("PACK", "FIELD-PACK", name, "",
								"positive pack fills this field on every applicable row"));
					}
				}
				continue;
			}
			if (positive && "BLANK".equals(c.packFill())) {
				out.add(new ValidationIssue("WARN", "FIELD-PACK", name, v, "Cosmos pack keeps this field blank"));
			}
			if (c.maxLen() != null && v.length() > c.maxLen()) {
				out.add(new ValidationIssue("ERROR", "FIELD-LEN", name, v,
						"length " + v.length() + " > max " + c.maxLen()));
			}
			if (c.maxLenDb() != null && v.length() > c.maxLenDb()) {
				out.add(new ValidationIssue("WARN", "DB-LEN", name, v, "length " + v.length()
						+ " > iSolve DB column length " + c.maxLenDb() + " (CERSAI allows " + c.maxLen()
						+ ") - confirm with dev team"));
			}
			String f = resolveFormat(c, r);
			if (f != null && !fmtOk(f, v)) {
				FormatSpec fs = spec.format(f);
				String desc = fs != null && fs.desc() != null ? fs.desc() : f;
				out.add(new ValidationIssue("ERROR", "FIELD-FMT", name, v, "not a valid " + f + ": " + desc));
			}
			if (!enumOk(c, v)) {
				out.add(new ValidationIssue("ERROR", "FIELD-ENUM", name, v,
						"value not in allowed list " + pyListRepr(c.enumValues())));
			}
		}
	}

	// ------------------------------------------------------------------ shared helpers
	/** Renders a string list the way Python's reference validator does, e.g. ['A', 'B'] — the
	 * reference validator's messages are quoted verbatim in test_cases.csv, so match its format. */
	private static String pyListRepr(List<String> values) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append('\'').append(values.get(i)).append('\'');
		}
		return sb.append(']').toString();
	}

	private LocalDate pdate(String v) {
		if (v == null || v.isEmpty()) {
			return null;
		}
		FormatSpec dateFmt = spec.format("DATE");
		if (dateFmt == null || !Pattern.matches(dateFmt.regex(), v)) {
			return null;
		}
		try {
			return LocalDate.parse(v, DMY);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private int age(LocalDate dob, LocalDate ref) {
		int a = ref.getYear() - dob.getYear();
		if (ref.getMonthValue() < dob.getMonthValue()
				|| (ref.getMonthValue() == dob.getMonthValue() && ref.getDayOfMonth() < dob.getDayOfMonth())) {
			a--;
		}
		return a;
	}

	/**
	 * DATE-04 (project rule, not in the original spec): the KYC verification date must be after
	 * the declaration date — a record can't be "KYC verified" before the customer even declared.
	 * Applied to Individual/Minor applicants, Legal Entity, and every Related Person block.
	 */
	private void kycAfterDeclCheck(List<ValidationIssue> out, Map<String, String> r, String declCol, String kycCol) {
		LocalDate decl = pdate(r.getOrDefault(declCol, ""));
		LocalDate kyc = pdate(r.getOrDefault(kycCol, ""));
		if (decl != null && kyc != null && !kyc.isAfter(decl)) {
			out.add(new ValidationIssue("ERROR", "DATE-04", kycCol, r.getOrDefault(kycCol, ""),
					kycCol + " must be after " + declCol + " (" + r.getOrDefault(declCol, "") + ")"));
		}
	}

	private void datesCheck(List<ValidationIssue> out, Map<String, String> r, List<String> cols, LocalDate upload) {
		for (String col : cols) {
			String v = r.getOrDefault(col, "");
			LocalDate d = pdate(v);
			if (!v.isEmpty() && d != null) {
				if (d.isAfter(upload)) {
					out.add(new ValidationIssue("ERROR", "DATE-01", col, v, "future date"));
				} else if (d.isEqual(upload)) {
					out.add(new ValidationIssue("ERROR", "DATE-01", col, v, "must not equal the upload date"));
				}
			}
		}
	}

	private void pinCheck(List<ValidationIssue> out, Map<String, String> r, String[] keys) {
		String dist = r.getOrDefault(keys[4], "");
		String st = r.getOrDefault(keys[5], "");
		String pin = r.getOrDefault(keys[7], "");
		if (pin.isEmpty()) {
			return;
		}
		Map<String, String> m = spec.pincodeMaster(pin);
		if (m == null) {
			out.add(new ValidationIssue("ERROR", "ADDR-01", keys[7], pin, "PIN not in CERSAI pincode master"));
			return;
		}
		if (!dist.isEmpty() && !dist.equalsIgnoreCase(m.get("DISTRICT"))) {
			out.add(new ValidationIssue("ERROR", "ADDR-01", keys[4], dist,
					"district of PIN " + pin + " is " + m.get("DISTRICT")));
		}
		if (!st.isEmpty() && !st.equals(m.get("STATE_CODE"))) {
			out.add(new ValidationIssue("ERROR", "ADDR-01", keys[5], st,
					"state of PIN " + pin + " is " + m.get("STATE_CODE")));
		}
	}

	private void sameOrDiff(List<ValidationIssue> out, Map<String, String> r, String flag, String[] a, String[] b,
			String rule, String label) {
		String[] av = addrBlock(r, a);
		String[] bv = addrBlock(r, b);
		if ("Y".equals(flag) && !java.util.Arrays.equals(av, bv)) {
			List<String> bad = new ArrayList<>();
			for (int i = 0; i < a.length; i++) {
				if (!av[i].equals(bv[i])) {
					bad.add(b[i]);
				}
			}
			out.add(new ValidationIssue("ERROR", rule, String.join(",", bad), "",
					label + " flag Y but address is not an exact copy"));
		}
		if ("N".equals(flag) && av[0].equals(bv[0]) && av[7].equals(bv[7])) {
			out.add(new ValidationIssue("ERROR", rule, b[0], bv[0], label + " flag N but address equals the permanent address"));
		}
	}

	private String[] addrBlock(Map<String, String> r, String[] keys) {
		String[] out = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			out[i] = r.getOrDefault(keys[i], "");
		}
		return out;
	}

	private void contactRules(List<ValidationIssue> out, Map<String, String> r, String[][] pairs) {
		for (String[] pair : pairs) {
			String a = pair[0], b = pair[1];
			boolean hasA = !r.getOrDefault(a, "").isEmpty();
			boolean hasB = !r.getOrDefault(b, "").isEmpty();
			if (hasA != hasB) {
				String col = hasA ? b : a;
				out.add(new ValidationIssue("ERROR", "CONTACT-01", col, r.getOrDefault(a, r.getOrDefault(b, "")),
						a + " and " + b + " must be filled together"));
			}
		}
	}

	private void hier(List<ValidationIssue> out, Map<String, String> r, String codeCol, String nameCol) {
		String code = r.getOrDefault(codeCol, "");
		String name = r.getOrDefault(nameCol, "");
		String cfgOrgCode = spec.config("ORG_CODE");
		String cfgOrgName = spec.config("ORG_NAME");
		if (!code.isEmpty() && !code.equals(cfgOrgCode)) {
			out.add(new ValidationIssue("ERROR", "HIER-01", codeCol, code, "must be " + cfgOrgCode));
		}
		if (!name.isEmpty() && !name.equals(cfgOrgName)) {
			out.add(new ValidationIssue("ERROR", "HIER-01", nameCol, name, "must be " + cfgOrgName));
		}
	}

	private String[] commFor(String persona) {
		return switch (persona) {
		case "LE" -> COMM_LE;
		default -> COMM_IND; // IND and MIN share the same correspondence-address column names
		};
	}

	// ------------------------------------------------------------------ applicant (IND / MIN)
	private void applicantRules(List<ValidationIssue> out, Map<String, String> r, String persona, LocalDate upload) {
		String fl = r.getOrDefault("PERM_TO_COMM_FLG", "");
		if (r.getOrDefault("FATHERORSPOUSE_FIRST_NAME", "").isEmpty() && r.getOrDefault("MOTHER_FIRST_NAME", "").isEmpty()) {
			out.add(new ValidationIssue("ERROR", "NAME-01", "MOTHER_FIRST_NAME", "", "none of father/spouse/mother name given"));
		}
		String g = r.getOrDefault("GENDER", "");
		String p = r.getOrDefault("APPL_NAME_PREFIX", "");
		if (("MR".equals(p) && "F".equals(g)) || (("MRS".equals(p) || "MS".equals(p) || "MISS".equals(p)) && "M".equals(g))) {
			out.add(new ValidationIssue("WARN", "NAME-02", "APPL_NAME_PREFIX", p, "prefix does not match gender " + g));
		}
		if ("02".equals(r.getOrDefault("FORS_FLG", "")) && !"MRS".equals(p)) {
			out.add(new ValidationIssue("WARN", "NAME-02", "FORS_FLG", "02", "spouse name used for a non-married prefix"));
		}

		datesCheck(out, r, List.of("DOB", "DECL_DATETIME", "KYC_VERIFY_DATETIME"), upload);
		LocalDate dob = pdate(r.getOrDefault("DOB", ""));
		LocalDate decl = pdate(r.getOrDefault("DECL_DATETIME", ""));
		LocalDate kyc = pdate(r.getOrDefault("KYC_VERIFY_DATETIME", ""));
		if (dob != null && decl != null) {
			int a = age(dob, decl);
			if (!dob.isBefore(decl)) {
				out.add(new ValidationIssue("ERROR", "DATE-01", "DOB", r.get("DOB"), "DOB not before declaration date"));
			}
			if ("IND".equals(persona) && a < 18) {
				out.add(new ValidationIssue("ERROR", "AGE-01", "DOB", r.get("DOB"), "Individual aged " + a + " (<18)"));
			}
			if ("MIN".equals(persona) && a >= 18) {
				out.add(new ValidationIssue("ERROR", "AGE-02", "DOB", r.get("DOB"), "Minor aged " + a + " (>=18)"));
			}
		}
		kycAfterDeclCheck(out, r, "DECL_DATETIME", "KYC_VERIFY_DATETIME");
		for (String[] pair : new String[][] { { "POI_EXPIRY_DATE", r.getOrDefault("POI_TYPE", "") },
				{ "POA_EXPIRY_DATE", r.getOrDefault("POA_TYPE", "") } }) {
			String c = pair[0];
			String code = pair[1];
			boolean expiryApplicable = "POI_EXPIRY_DATE".equals(c) ? ("A".equals(code) || "D".equals(code))
					: ("02".equals(code) || "03".equals(code));
			String v = r.getOrDefault(c, "");
			LocalDate vd = pdate(v);
			if (expiryApplicable && !v.isEmpty() && kyc != null && vd != null && !vd.isAfter(kyc)) {
				out.add(new ValidationIssue("WARN", "DATE-02", c, v, "expiry not after KYC date"));
			}
			if (!expiryApplicable && !v.isEmpty()) {
				out.add(new ValidationIssue("WARN", "DATE-02", c, v, "expiry only for Passport / Driving Licence"));
			}
		}

		String[] commCols = commFor(persona);
		pinCheck(out, r, PERM);
		pinCheck(out, r, commCols);
		sameOrDiff(out, r, fl, PERM, commCols, "ADDR-02", "PERM_TO_COMM_FLG");
		if ("Y".equals(fl) && !r.getOrDefault("COMM_ADDRESS_TYPE", "").isEmpty()) {
			out.add(new ValidationIssue("WARN", "ADDR-02", "COMM_ADDRESS_TYPE", r.get("COMM_ADDRESS_TYPE"),
					"pack keeps COMM_ADDRESS_TYPE blank when flag Y"));
		}

		DocEntry pd = spec.docByPoiCode(r.getOrDefault("POI_TYPE", ""));
		DocEntry ad = spec.docByPoaCode(r.getOrDefault("POA_TYPE", ""));
		if (pd != null) {
			if (!r.getOrDefault("POI_CATEGORY", "").isEmpty() && !r.get("POI_CATEGORY").equals(pd.poiCategory())) {
				out.add(new ValidationIssue("ERROR", "POI-01", "POI_CATEGORY", r.get("POI_CATEGORY"),
						"POI_TYPE " + r.get("POI_TYPE") + " category must be '" + pd.poiCategory() + "'"));
			}
			if (!r.getOrDefault("POI_FILE_NAME", "").isEmpty() && !r.get("POI_FILE_NAME").equals(pd.imageFile())) {
				out.add(new ValidationIssue("ERROR", "IMG-03", "POI_FILE_NAME", r.get("POI_FILE_NAME"),
						"POI_TYPE " + r.get("POI_TYPE") + " image must be " + pd.imageFile()));
			}
			if ("MIN".equals(persona) && !pd.minorAllowed()) {
				out.add(new ValidationIssue("ERROR", "MINDOC-01", "POI_TYPE", r.get("POI_TYPE"),
						"Voter ID / Driving Licence not allowed for a minor"));
			}
		}
		if (ad != null) {
			if (!r.getOrDefault("POA_CATEGORY", "").isEmpty() && !r.get("POA_CATEGORY").equals(ad.poaCategory())) {
				out.add(new ValidationIssue("ERROR", "POA-01", "POA_CATEGORY", r.get("POA_CATEGORY"),
						"POA_TYPE " + r.get("POA_TYPE") + " category must be '" + ad.poaCategory() + "'"));
			}
			if (!r.getOrDefault("POA_FILE_NAME", "").isEmpty() && !r.get("POA_FILE_NAME").equals(ad.imageFile())) {
				out.add(new ValidationIssue("ERROR", "IMG-03", "POA_FILE_NAME", r.get("POA_FILE_NAME"),
						"POA_TYPE " + r.get("POA_TYPE") + " image must be " + ad.imageFile()));
			}
			if ("MIN".equals(persona) && !ad.minorAllowed()) {
				out.add(new ValidationIssue("ERROR", "MINDOC-01", "POA_TYPE", r.get("POA_TYPE"),
						"Voter ID / Driving Licence not allowed for a minor"));
			}
		}
		if (!r.getOrDefault("PERM_ADD_PROOF", "").equals(r.getOrDefault("POA_TYPE", ""))) {
			out.add(new ValidationIssue("ERROR", "POA-01", "PERM_ADD_PROOF", r.getOrDefault("PERM_ADD_PROOF", ""),
					"must equal POA_TYPE " + r.getOrDefault("POA_TYPE", "")));
		}
		if (!r.getOrDefault("PERM_ADD_PROOF_DESC", "").isEmpty() && ad != null
				&& !r.get("PERM_ADD_PROOF_DESC").equals(ad.poaCategory())) {
			out.add(new ValidationIssue("WARN", "POA-01", "PERM_ADD_PROOF_DESC", r.get("PERM_ADD_PROOF_DESC"),
					"should be '" + ad.poaCategory() + "'"));
		}
		if (pd != null && ad != null) {
			boolean sameDoc = pd.doc().equals(ad.doc());
			if (sameDoc && (!r.getOrDefault("POI_NUMBER", "").equals(r.getOrDefault("POA_NUMBER", ""))
					|| !r.getOrDefault("POI_FILE_NAME", "").equals(r.getOrDefault("POA_FILE_NAME", "")))) {
				out.add(new ValidationIssue("ERROR", "POIA-01", "POA_NUMBER", r.getOrDefault("POA_NUMBER", ""),
						"same document for POI and POA -> same number and same image"));
			}
			if (!sameDoc && (r.getOrDefault("POI_FILE_NAME", "").equals(r.getOrDefault("POA_FILE_NAME", ""))
					|| r.getOrDefault("POI_NUMBER", "").equals(r.getOrDefault("POA_NUMBER", "")))) {
				out.add(new ValidationIssue("ERROR", "POIA-01", "POA_FILE_NAME", r.getOrDefault("POA_FILE_NAME", ""),
						"different documents must have different number and image"));
			}
		}

		String it = r.getOrDefault("COMM_ADDRESS_IDTYPE", "");
		String catg = r.getOrDefault("COMM_ADDRESS_CATEGORY", "");
		if ("Y".equals(fl)) {
			boolean matches = it.equals(r.getOrDefault("POA_TYPE", ""))
					&& r.getOrDefault("COMM_ADDRESS_NUMBER", "").equals(r.getOrDefault("POA_NUMBER", ""))
					&& catg.equals(r.getOrDefault("POA_CATEGORY", ""));
			if (!matches) {
				out.add(new ValidationIssue("ERROR", "COMM-01", "COMM_ADDRESS_IDTYPE", it,
						"flag Y: correspondence proof must be the POA document (type, number, category)"));
			}
		}
		if ("N".equals(fl)) {
			if (!spec.isDeemed(it)) {
				out.add(new ValidationIssue("ERROR", "COMM-01", "COMM_ADDRESS_IDTYPE", it,
						"flag N: must be a deemed proof 11/12/13/14, never an OVD"));
			} else {
				DeemedEntry d = spec.deemed(it);
				if (!catg.equals(d.category())) {
					out.add(new ValidationIssue("ERROR", "COMM-01", "COMM_ADDRESS_CATEGORY", catg,
							"must be '" + d.category() + "'"));
				}
				if ("MIN".equals(persona) && !"11".equals(it) && !"12".equals(it)) {
					out.add(new ValidationIssue("ERROR", "COMM-01", "COMM_ADDRESS_IDTYPE", it, "minor: only 11 / 12"));
				}
				if ("13".equals(it) && dob != null && decl != null && age(dob, decl) < 58) {
					out.add(new ValidationIssue("WARN", "COMM-01", "COMM_ADDRESS_IDTYPE", it,
							"Pension Payment Order only for age >= 58"));
				}
			}
		}

		String pan = r.getOrDefault("PAN_OR_FORM60", "");
		FormatSpec panFmt = spec.format("PAN");
		if (panFmt != null && Pattern.matches(panFmt.regex(), pan)) {
			if (pan.length() > 3 && pan.charAt(3) != 'P') {
				out.add(new ValidationIssue("ERROR", "PAN-01", "PAN_OR_FORM60", pan, "individual PAN 4th letter must be P"));
			}
			String lastName = r.getOrDefault("APPL_LAST_NAME", "");
			if (!lastName.isEmpty() && pan.length() > 4 && pan.charAt(4) != lastName.charAt(0)) {
				out.add(new ValidationIssue("WARN", "PAN-02", "PAN_OR_FORM60", pan, "5th letter should be the last-name initial"));
			}
		}
		if ("IND".equals(persona) && "FORM60".equals(pan)) {
			out.add(new ValidationIssue("WARN", "PAN-01", "PAN_OR_FORM60", pan, "Individual positive pack uses PAN"));
		}

		contactRules(out, r, new String[][] { { "RESIDENCE_TELEPHONE_NO_STD_CODE", "RESIDENCE_TELEPHONE_NO" },
				{ "OFFICE_TELEPHONE_NO_STD_CODE", "OFFICE_TELEPHONE_NO" }, { "FAX_NO_STD_CODE", "FAX_NO" },
				{ "MOBILE_NO_ISD_CODE", "MOBILE_NO" }, { "MOBILE_NO2_ISD_CODE2", "MOBILE_NO2" } });
		hier(out, r, "ORG_CODE", "ORG_NAME");
	}

	// ------------------------------------------------------------------ related person
	private void rpRules(List<ValidationIssue> out, Map<String, String> r, int n, String persona, String parent,
			LocalDate upload) {
		String flag = r.getOrDefault("REL_PER_SAMEASPERM_ADD_FLAG", "");
		datesCheck(out, r, List.of("REL_PER_DOB", "REL_PER_DECL_DATETIME", "REL_PER_KYC_VERIFY_DATETIME"), upload);
		LocalDate dob = pdate(r.getOrDefault("REL_PER_DOB", ""));
		LocalDate decl = pdate(r.getOrDefault("REL_PER_DECL_DATETIME", ""));
		if (dob != null && decl != null && age(dob, decl) < 18) {
			out.add(new ValidationIssue("ERROR", "AGE-03", "REL_PER_DOB", r.get("REL_PER_DOB"),
					"related person aged " + age(dob, decl) + " (<18)"));
		}
		kycAfterDeclCheck(out, r, "REL_PER_DECL_DATETIME", "REL_PER_KYC_VERIFY_DATETIME");
		if (r.getOrDefault("REL_PER_MOTHER_FIRST_NAME", "").isEmpty()) {
			out.add(new ValidationIssue("WARN", "NAME-03", "REL_PER_MOTHER_FIRST_NAME", "",
					"RP mother name always filled (user rule)"));
		}
		String g = r.getOrDefault("REL_PER_GENDER", "");
		String p = r.getOrDefault("REL_PER_NAME_PREFIX", "");
		if (("MR".equals(p) && "F".equals(g)) || (("MRS".equals(p) || "MS".equals(p) || "MISS".equals(p)) && "M".equals(g))) {
			out.add(new ValidationIssue("WARN", "NAME-02", "REL_PER_NAME_PREFIX", p, "prefix does not match gender " + g));
		}
		pinCheck(out, r, RPP);
		pinCheck(out, r, RPC);
		sameOrDiff(out, r, flag, RPP, RPC, "ADDR-03", "REL_PER_SAMEASPERM_ADD_FLAG");
		String perm = r.getOrDefault("REL_PER_ADD_PROF", "");
		String curr = r.getOrDefault("REL_PER_CURR_ADD_PROOF", "");
		if ("Y".equals(flag) && !curr.isEmpty() && !curr.equals(perm)) {
			out.add(new ValidationIssue("ERROR", "ADDR-03", "REL_PER_CURR_ADD_PROOF", curr, "flag Y: must equal REL_PER_ADD_PROF"));
		}

		boolean anyRpId = false;
		for (String col : spec.rpIdColumns()) {
			if (!r.getOrDefault(col, "").isEmpty()) {
				anyRpId = true;
				break;
			}
		}
		if (!anyRpId) {
			out.add(new ValidationIssue("ERROR", "RPID-01", "REL_PER_UID", "",
					"no RP identity number in any of the 8 RP ID columns"));
		}
		String poiImg = r.getOrDefault("REL_PER_POI_NAME", "").replaceFirst("^RP\\d+_", "");
		String poiCode = spec.poaCodeForImage(poiImg);
		poiCode = poiCode == null ? "" : poiCode;
		Map<String, String> checks = new LinkedHashMap<>();
		checks.put("REL_PER_ADD_PROF", perm);
		checks.put("RP POI image", poiCode);
		checks.put("REL_PER_CURR_ADD_PROOF", "N".equals(flag) ? curr : "");
		for (Map.Entry<String, String> e : checks.entrySet()) {
			String code = e.getValue();
			if (code != null && !code.isEmpty()) {
				String col = spec.rpIdColumnForPoaCode(code);
				if (col != null && r.getOrDefault(col, "").isEmpty()) {
					out.add(new ValidationIssue("ERROR", "RPID-02", col, "", e.getKey() + " = " + code + " but " + col + " is blank"));
				}
			}
		}
		if ("N".equals(flag) && !curr.isEmpty() && (curr.equals(perm) || curr.equals(poiCode))) {
			out.add(new ValidationIssue("WARN", "RPID-03", "REL_PER_CURR_ADD_PROOF", curr,
					"different current address should use a different proof document"));
		}
		for (String c : new String[] { "REL_PER_EKYC", "REL_PER_OFFLINE_UID" }) {
			String v = r.getOrDefault(c, "");
			String uid = r.getOrDefault("REL_PER_UID", "");
			if (!v.isEmpty() && !uid.isEmpty() && Pattern.matches("\\d{4}", v) && !v.equals(uid.substring(Math.max(0, uid.length() - 4)))) {
				out.add(new ValidationIssue("ERROR", "RPID-04", c, v, "must be the last 4 digits of REL_PER_UID"));
			}
		}

		String pre = "RP" + n + "_";
		if (!r.getOrDefault("REL_PER_PHOTO_NAME", "").isEmpty() && !r.get("REL_PER_PHOTO_NAME").equals(pre + "Photo.jpg")) {
			out.add(new ValidationIssue("ERROR", "RPIMG-01", "REL_PER_PHOTO_NAME", r.get("REL_PER_PHOTO_NAME"), "expected " + pre + "Photo.jpg"));
		}
		if (!r.getOrDefault("REL_PER_POI_NAME", "").isEmpty() && !r.get("REL_PER_POI_NAME").startsWith(pre)) {
			out.add(new ValidationIssue("ERROR", "RPIMG-01", "REL_PER_POI_NAME", r.get("REL_PER_POI_NAME"), "expected prefix " + pre));
		}
		DocEntry permDoc = spec.docByPoaCode(perm);
		if (permDoc != null && !r.getOrDefault("REL_PER_PER_ADDRESS_NAME", "").isEmpty()
				&& !r.get("REL_PER_PER_ADDRESS_NAME").equals(pre + permDoc.imageFile())) {
			out.add(new ValidationIssue("ERROR", "RPIMG-01", "REL_PER_PER_ADDRESS_NAME", r.get("REL_PER_PER_ADDRESS_NAME"),
					"REL_PER_ADD_PROF " + perm + " -> " + pre + permDoc.imageFile()));
		}
		String pan = r.getOrDefault("REL_PER_PANORFORM60", "");
		FormatSpec panFmt = spec.format("PAN");
		if (panFmt != null && Pattern.matches(panFmt.regex(), pan)) {
			if (pan.length() > 3 && pan.charAt(3) != 'P') {
				out.add(new ValidationIssue("ERROR", "PAN-01", "REL_PER_PANORFORM60", pan, "RP PAN 4th letter must be P"));
			}
			String lastName = r.getOrDefault("REL_PER_LAST_NAME", "");
			if (!lastName.isEmpty() && pan.length() > 4 && pan.charAt(4) != lastName.charAt(0)) {
				out.add(new ValidationIssue("WARN", "PAN-02", "REL_PER_PANORFORM60", pan, "5th letter should be the last-name initial"));
			}
		}
		if (!parent.equals(r.getOrDefault("CUST_ID", ""))) {
			out.add(new ValidationIssue("ERROR", "CUST-01", "CUST_ID", r.getOrDefault("CUST_ID", ""),
					"must be the parent CUSTOMER_REFERENCE_NUMBER " + parent));
		}
		if ("4".equals(r.getOrDefault("REL_TYPE", "")) && r.getOrDefault("REL_PER_DIN_NUMBER", "").isEmpty()) {
			out.add(new ValidationIssue("ERROR", "LE-02", "REL_PER_DIN_NUMBER", "", "Director needs DIN"));
		}
		if (!"4".equals(r.getOrDefault("REL_TYPE", "")) && !r.getOrDefault("REL_PER_DIN_NUMBER", "").isEmpty()) {
			out.add(new ValidationIssue("WARN", "LE-02", "REL_PER_DIN_NUMBER", r.get("REL_PER_DIN_NUMBER"), "DIN only for Director"));
		}
		contactRules(out, r, new String[][] { { "REL_PER_TEL_STDCODE", "REL_PER_TEL_NO" },
				{ "REL_PER_OFFTEL_STDCODE", "REL_PER_OFFTEL_NO" }, { "REL_PER_MOBILE_CODE", "REL_PER_MOBILE_NO" } });
		hier(out, r, "REL_PER_ORG_CODE", "REL_PER_ORG_NAME");
	}

	// ------------------------------------------------------------------ legal entity
	private void entityRules(List<ValidationIssue> out, Map<String, String> r, List<Map<String, String>> rows,
			LocalDate upload) {
		String fl = r.getOrDefault("PERM_TO_COMM_FLG", "");
		String ct = r.getOrDefault("CONST_TYPE", "");
		LeConstitutionRule rule = spec.leConstitutionRule(ct);
		datesCheck(out, r, List.of("DATE_OF_INC", "DATE_OF_COMMENCEMENT", "DECL_DATETIME", "KYC_VERIFY_DATETIME"), upload);
		LocalDate di = pdate(r.getOrDefault("DATE_OF_INC", ""));
		LocalDate dc = pdate(r.getOrDefault("DATE_OF_COMMENCEMENT", ""));
		if (di != null && dc != null && dc.isBefore(di)) {
			out.add(new ValidationIssue("ERROR", "DATE-03", "DATE_OF_COMMENCEMENT", r.get("DATE_OF_COMMENCEMENT"), "before DATE_OF_INC"));
		}
		kycAfterDeclCheck(out, r, "DECL_DATETIME", "KYC_VERIFY_DATETIME");
		pinCheck(out, r, PERM);
		pinCheck(out, r, COMM_LE);
		sameOrDiff(out, r, fl, PERM, COMM_LE, "ADDR-02", "PERM_TO_COMM_FLG");

		String pan = r.getOrDefault("PAN_OR_FORM60", "");
		FormatSpec panFmt = spec.format("PAN");
		if (rule != null && panFmt != null && Pattern.matches(panFmt.regex(), pan)) {
			if (pan.length() > 3 && !String.valueOf(pan.charAt(3)).equals(rule.pan4th())) {
				out.add(new ValidationIssue("ERROR", "PAN-01", "PAN_OR_FORM60", pan,
						"constitution " + ct + " PAN 4th letter must be " + rule.pan4th()));
			}
			String fullName = r.getOrDefault("APPL_FULL_NAME", "");
			if (pan.length() > 4 && !fullName.isEmpty() && pan.charAt(4) != fullName.charAt(0)) {
				out.add(new ValidationIssue("WARN", "PAN-02", "PAN_OR_FORM60", pan, "5th letter should be the entity-name initial"));
			}
		}
		if (rule != null && !r.getOrDefault("APPL_FULL_NAME", "").endsWith(rule.nameSuffix())) {
			out.add(new ValidationIssue("WARN", "LE-05", "APPL_FULL_NAME", r.getOrDefault("APPL_FULL_NAME", ""),
					"constitution " + ct + " name ends with '" + rule.nameSuffix() + "'"));
		}
		String tin = r.getOrDefault("TIN", "");
		FormatSpec gstinFmt = spec.format("GSTIN");
		if (!tin.isEmpty() && gstinFmt != null && Pattern.matches(gstinFmt.regex(), tin)) {
			if (tin.length() >= 12 && !tin.substring(2, 12).equals(pan)) {
				out.add(new ValidationIssue("ERROR", "LE-04", "TIN", tin, "GSTIN chars 3-12 must equal PAN_OR_FORM60"));
			}
			String wantGst = spec.gstStateCode(r.getOrDefault("PERM_ADD_STATE", ""));
			if (wantGst != null && tin.length() >= 2 && !tin.substring(0, 2).equals(wantGst)) {
				out.add(new ValidationIssue("ERROR", "LE-04", "TIN", tin,
						"GSTIN state code must be " + wantGst + " for " + r.getOrDefault("PERM_ADD_STATE", "")));
			}
		}
		if (!r.getOrDefault("PERM_ADD_PROOF", "").equals(r.getOrDefault("POA_TYPE", ""))) {
			out.add(new ValidationIssue("ERROR", "LE-03", "PERM_ADD_PROOF", r.getOrDefault("PERM_ADD_PROOF", ""),
					"must equal POA_TYPE " + r.getOrDefault("POA_TYPE", "")));
		}
		for (String[] triple : new String[][] { { "POI_TYPE", r.getOrDefault("POI_TYPE", ""), "POI_FILE_NAME" },
				{ "POA_TYPE", r.getOrDefault("POA_TYPE", ""), "POA_FILE_NAME" } }) {
			String col = triple[0], code = triple[1], fcol = triple[2];
			DocEntry d = spec.leDocByCode(code);
			if (d != null && !r.getOrDefault(fcol, "").equals(d.imageFile())) {
				out.add(new ValidationIssue("ERROR", "IMG-03", fcol, r.getOrDefault(fcol, ""), col + " " + code + " image must be " + d.imageFile()));
			}
			if (d != null && !d.constitutions().contains(ct)) {
				out.add(new ValidationIssue("ERROR", "LE-03", col, code, "constitution " + ct + " has no " + d.doc()));
			}
		}
		for (String[] pair : new String[][] { { "POI_TYPE", r.getOrDefault("POI_TYPE", ""), "POI_NUMBER" },
				{ "POA_TYPE", r.getOrDefault("POA_TYPE", ""), "POA_NUMBER" } }) {
			String code = pair[1], ncol = pair[2];
			DocEntry d = spec.leDocByCode(code);
			String v = r.getOrDefault(ncol, "");
			if (d != null && !v.isEmpty() && "CERTIFICATE_OF_INCORPORATION".equals(d.doc())) {
				String want = "J".equals(ct) ? "LLPIN" : "CIN";
				if (!fmtOk(want, v)) {
					out.add(new ValidationIssue("ERROR", "LE-03", ncol, v, "constitution " + ct + ": COI number must be " + want));
				} else if ("CIN".equals(want)) {
					boolean isPublic = "E".equals(ct);
					boolean startsL = v.startsWith("L");
					boolean hasPlc = v.contains("PLC");
					if (isPublic != startsL || isPublic != hasPlc) {
						out.add(new ValidationIssue("ERROR", "LE-03", ncol, v, "Public Ltd = L..PLC, Private Ltd = U..PTC"));
					}
				}
			}
		}
		DocEntry poiDoc = spec.leDocByCode(r.getOrDefault("POI_TYPE", ""));
		DocEntry poaDoc = spec.leDocByCode(r.getOrDefault("POA_TYPE", ""));
		String poiDocName = poiDoc == null ? null : poiDoc.doc();
		String poaDocName = poaDoc == null ? null : poaDoc.doc();
		if (poiDocName != null && poiDocName.equals(poaDocName)) {
			if (!r.getOrDefault("POI_NUMBER", "").equals(r.getOrDefault("POA_NUMBER", ""))
					|| !r.getOrDefault("POI_FILE_NAME", "").equals(r.getOrDefault("POA_FILE_NAME", ""))) {
				out.add(new ValidationIssue("ERROR", "POIA-01", "POA_NUMBER", r.getOrDefault("POA_NUMBER", ""), "same LE document -> same number and image"));
			}
		} else if (r.getOrDefault("POI_FILE_NAME", "").equals(r.getOrDefault("POA_FILE_NAME", ""))) {
			out.add(new ValidationIssue("ERROR", "POIA-01", "POA_FILE_NAME", r.getOrDefault("POA_FILE_NAME", ""), "different LE documents need different images"));
		}

		String commIdType = r.getOrDefault("COMM_ADDRESS_IDTYPE", "");
		if (!commIdType.isEmpty() && !commIdType.equals("06") && !commIdType.equals("07") && !commIdType.equals("99")) {
			out.add(new ValidationIssue("ERROR", "COMM-02", "COMM_ADDRESS_IDTYPE", commIdType, "LE: only 06 / 07 / 99"));
		}
		if (!(commIdType.equals(r.getOrDefault("POA_TYPE", "")) && r.getOrDefault("COMM_ADDRESS_NUMBER", "").equals(r.getOrDefault("POA_NUMBER", "")))) {
			out.add(new ValidationIssue("WARN", "COMM-02", "COMM_ADDRESS_NUMBER", r.getOrDefault("COMM_ADDRESS_NUMBER", ""), "pack: local proof = POA document"));
		}
		String commCat = r.getOrDefault("COMM_ADDRESS_CATEGORY", "");
		String wantCat = spec.poaCategoryName(commIdType);
		if (!commCat.isEmpty() && !commCat.equals(wantCat)) {
			out.add(new ValidationIssue("ERROR", "COMM-02", "COMM_ADDRESS_CATEGORY", commCat,
					"must be '" + (wantCat == null ? "?" : wantCat) + "'"));
		}

		String numRp = r.getOrDefault("NUM_OF_REL_PER", "");
		String rowsCount = String.valueOf(rows.size());
		if (!numRp.equals(rowsCount) && !numRp.replaceFirst("^0+", "").equals(rowsCount)) {
			out.add(new ValidationIssue("ERROR", "LE-01", "NUM_OF_REL_PER", numRp, rows.size() + " RP rows found for this CUST_ID"));
		}
		String wantImg = r.getOrDefault("POI_FILE_NAME", "").equals(r.getOrDefault("POA_FILE_NAME", "")) ? "01" : "02";
		if (!wantImg.equals(r.getOrDefault("NUM_OF_IMG", ""))) {
			out.add(new ValidationIssue("WARN", "LE-05", "NUM_OF_IMG", r.getOrDefault("NUM_OF_IMG", ""), "pack convention " + wantImg));
		}
		for (String[] pair : new String[][] { { "RES_TEL_CODE", "RESIDENCE_TELEPHONE_NO_STD_CODE" },
				{ "RES_TEL_NUM", "RESIDENCE_TELEPHONE_NO" }, { "OFF_TEL_CODE", "OFFICE_TELEPHONE_NO_STD_CODE" },
				{ "OFF_TEL_NUM", "OFFICE_TELEPHONE_NO" }, { "MOB_ISD_CODE", "MOBILE_NO_ISD_CODE" },
				{ "MOB_NUM", "MOBILE_NO" }, { "FAX_STD_CODE", "FAX_NO_STD_CODE" }, { "FAX_NUM", "FAX_NO" } }) {
			String a = pair[0], b = pair[1];
			if (!r.getOrDefault(a, "").equals(r.getOrDefault(b, ""))) {
				out.add(new ValidationIssue("WARN", "LE-05", a, r.getOrDefault(a, ""), "should equal " + b + " (" + r.getOrDefault(b, "") + ")"));
			}
		}
		contactRules(out, r, new String[][] { { "RESIDENCE_TELEPHONE_NO_STD_CODE", "RESIDENCE_TELEPHONE_NO" },
				{ "OFFICE_TELEPHONE_NO_STD_CODE", "OFFICE_TELEPHONE_NO" }, { "FAX_NO_STD_CODE", "FAX_NO" },
				{ "MOBILE_NO_ISD_CODE", "MOBILE_NO" }, { "MOBILE_NO2_ISD_CODE2", "MOBILE_NO2" } });
		hier(out, r, "ORG_CODE", "ORG_NAME");

		String wantRelType = rule == null ? null : rule.relType();
		if (wantRelType != null) {
			for (Map<String, String> rr : rows) {
				String relType = rr.getOrDefault("REL_TYPE", "");
				if (!relType.isEmpty() && !relType.equals(wantRelType)) {
					out.add(new ValidationIssue("WARN", "LE-02", "REL_TYPE", relType, "constitution " + ct + " -> REL_TYPE " + wantRelType));
				}
			}
		}
	}
}
