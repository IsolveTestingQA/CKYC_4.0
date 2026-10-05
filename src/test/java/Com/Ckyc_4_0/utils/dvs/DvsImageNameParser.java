/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Image file-name rules (sheet IMAGE_NAME_RULES, R1-R7). Parser order: photo -> related person -> main ->
 * LE entity -> deviation -> invalid. Extension is compared case-insensitively.
 */
public final class DvsImageNameParser {

	public enum Verdict { VALID, DEVIATION, INVALID }

	public record Result(Verdict verdict, String rule, String docMaster, String type, String page, String rpNumber,
			String message) {
	}

	private static final int CI = Pattern.CASE_INSENSITIVE;
	private static final Pattern RP_PHOTO = Pattern.compile("^RP(\\d+)-([A-Z0-9]+)_02_Photo\\.jpg$", CI);
	private static final Pattern PHOTO = Pattern.compile("^([A-Z0-9]+)_02_Photo\\.jpg$", CI);
	private static final Pattern RP_DOC = Pattern.compile("^RP(\\d+)-([A-Z0-9]+)_(\\d{2})_(POI|POA|POIA)_(\\d{2})\\.jpg$", CI);
	private static final Pattern MAIN_DOC = Pattern.compile("^([A-Z0-9]+)_(\\d{2})_(POI|POA|POIA)_(\\d{2})\\.jpg$", CI);
	private static final Pattern LE_CURRENT = Pattern.compile("^ID-(\\d{2})\\.jpg$", CI);
	private static final Pattern NO_PAGE = Pattern.compile("^([A-Z0-9]+)_(\\d{2})_(POI|POA|POIA)\\.jpg$", CI);

	private DvsImageNameParser() {
	}

	/**
	 * @param moduleCode IND / MIN / LE
	 * @param leRule     CURRENT or FUTURE (config image.rule.le), only used for LE
	 */
	public static Result parse(String fileName, String customerId, String moduleCode, String leRule) {
		String name = fileName == null ? "" : fileName.trim();
		boolean le = "LE".equalsIgnoreCase(moduleCode);
		boolean leCurrent = le && !"FUTURE".equalsIgnoreCase(leRule);
		Matcher m;

		if ((m = RP_PHOTO.matcher(name)).matches()) {
			return custCheck(m.group(2), customerId,
					new Result(Verdict.VALID, "R4", "02", "Photo", "", m.group(1), "RP photo"));
		}
		if ((m = PHOTO.matcher(name)).matches()) {
			return custCheck(m.group(1), customerId,
					new Result(Verdict.VALID, "R2", "02", "Photo", "", "", "Photo"));
		}
		if ((m = RP_DOC.matcher(name)).matches()) {
			return custCheck(m.group(2), customerId, new Result(Verdict.VALID, "R3", m.group(3),
					m.group(4).toUpperCase(), m.group(5), m.group(1), "Related person document"));
		}
		if ((m = MAIN_DOC.matcher(name)).matches()) {
			Result r = new Result(Verdict.VALID, le ? "R6" : "R1", m.group(2), m.group(3).toUpperCase(), m.group(4), "",
					le ? "LE entity document (FUTURE style)" : "Main customer document");
			if (leCurrent) {
				r = new Result(Verdict.DEVIATION, "R6", r.docMaster(), r.type(), r.page(), "",
						"FUTURE LE style used while image.rule.le=CURRENT");
			}
			return custCheck(m.group(1), customerId, r);
		}
		if ((m = LE_CURRENT.matcher(name)).matches()) {
			if (le && leCurrent) {
				return new Result(Verdict.VALID, "R5", m.group(1), "", "", "", "LE entity document (CURRENT style)");
			}
			return new Result(Verdict.INVALID, "R5", m.group(1), "", "", "",
					le ? "CURRENT LE style used while image.rule.le=FUTURE" : "ID-nn style is only valid for Legal Entity");
		}
		if ((m = NO_PAGE.matcher(name)).matches()) {
			return custCheck(m.group(1), customerId, new Result(Verdict.DEVIATION, "R7", m.group(2),
					m.group(3).toUpperCase(), "", "", "Page number _01/_02 missing (defect Q-03)"));
		}
		return new Result(Verdict.INVALID, "", "", "", "", "", "File name matches no image naming rule");
	}

	private static Result custCheck(String cust, String expectedCust, Result ok) {
		if (expectedCust != null && !expectedCust.isBlank() && !cust.equalsIgnoreCase(expectedCust.trim())) {
			return new Result(Verdict.INVALID, ok.rule(), ok.docMaster(), ok.type(), ok.page(), ok.rpNumber(),
					"Customer ID in file name (" + cust + ") differs from record " + expectedCust);
		}
		return ok;
	}
}
