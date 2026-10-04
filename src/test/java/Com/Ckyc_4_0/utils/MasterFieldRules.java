/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Banking / CKYC UI field validators — shared Masters rules (skill ckyc-cersai-rules).
 * Name max 50, address max 55, single spaces only, datatype-strict.
 */
public final class MasterFieldRules {

	public static final int NAME_MAX_LENGTH = 50;
	public static final int ADDRESS_MAX_LENGTH = 55;
	public static final int STATE_CODE_LENGTH = 2;
	public static final int PINCODE_LENGTH = 6;

	private static final Pattern STATE_CODE = Pattern.compile("^[A-Z]{2}$");
	private static final Pattern NAME_LETTERS_SINGLE_SPACE = Pattern.compile("^[A-Za-z]+( [A-Za-z]+)*$");
	private static final Pattern DOUBLE_SPACE = Pattern.compile(" {2,}");
	private static final Pattern PINCODE = Pattern.compile("^\\d{6}$");

	private MasterFieldRules() {
	}

	public static boolean isValidStateCode(String value) {
		return value != null && STATE_CODE.matcher(value.trim()).matches();
	}

	public static boolean isValidPincode(String value) {
		return value != null && PINCODE.matcher(value.trim()).matches();
	}

	public static boolean isValidName(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		String v = value.trim();
		if (v.length() > NAME_MAX_LENGTH) {
			return false;
		}
		if (DOUBLE_SPACE.matcher(value).find()) {
			return false;
		}
		return NAME_LETTERS_SINGLE_SPACE.matcher(v).matches();
	}

	public static boolean hasContinuousDoubleSpace(String value) {
		return value != null && DOUBLE_SPACE.matcher(value).find();
	}

	public static String describeLengthTrial(String field, String entered, String accepted) {
		int enteredLen = entered == null ? 0 : entered.length();
		int acceptedLen = accepted == null ? 0 : accepted.length();
		boolean trimmed = accepted != null && entered != null && !entered.equals(accepted);
		return "Field=" + field
				+ " | Entered='" + entered + "' (len=" + enteredLen + ")"
				+ " | Accepted='" + accepted + "' (len=" + acceptedLen + ")"
				+ " | TrimmedOrChanged=" + trimmed
				+ " | RejectedPart=" + rejectedPart(entered, accepted);
	}

	private static String rejectedPart(String entered, String accepted) {
		if (entered == null) {
			return "";
		}
		if (accepted == null || accepted.isEmpty()) {
			return entered;
		}
		if (entered.startsWith(accepted) && entered.length() > accepted.length()) {
			return entered.substring(accepted.length());
		}
		if (!entered.equals(accepted)) {
			return "(differs from entered)";
		}
		return "-";
	}

	public static String overLengthNameSample() {
		return "A".repeat(NAME_MAX_LENGTH + 5);
	}

	public static String overLengthPincodeSample() {
		return "9".repeat(PINCODE_LENGTH + 3);
	}

	public static String overLengthAddressSample() {
		return "A".repeat(ADDRESS_MAX_LENGTH + 5);
	}

	public static String normalizeLookup(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}
}
