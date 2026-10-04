/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

/**
 * Holds uniquely created TEST_ master records for this run.
 * Existing masters must never be edited or deleted — only these created ids.
 */
public final class MasterTestContext {

	private static String createdStateCode;
	private static String createdStateName;
	private static String createdDistrictName;
	private static String createdDistrictStateCode;
	private static String createdPincode;
	private static String createdPincodeDistrict;

	private static String createdFiCode;
	private static String createdFiName;
	private static String createdCpcCode;
	private static String createdCpcCity;
	private static String createdRegionCode;
	private static String createdRegionName;
	private static String createdBranchCode;
	private static String createdBranchName;

	private MasterTestContext() {
	}

	public static void clear() {
		createdStateCode = null;
		createdStateName = null;
		createdDistrictName = null;
		createdDistrictStateCode = null;
		createdPincode = null;
		createdPincodeDistrict = null;
		createdFiCode = null;
		createdFiName = null;
		createdCpcCode = null;
		createdCpcCity = null;
		createdRegionCode = null;
		createdRegionName = null;
		createdBranchCode = null;
		createdBranchName = null;
	}

	public static String uniqueToken() {
		return Long.toString(System.currentTimeMillis() % 100000, 36).toUpperCase();
	}

	/** Letters-only token for name fields (no digits). */
	public static String uniqueLetters(int length) {
		long n = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder();
		int len = Math.max(1, length);
		for (int i = 0; i < len; i++) {
			sb.append((char) ('A' + (int) ((n / Math.pow(26, i)) % 26)));
		}
		return sb.toString();
	}

	public static String uniqueTestStateName() {
		return "TEST AUTO STATE " + uniqueLetters(4);
	}

	public static String uniqueTestDistrictName() {
		return "TEST AUTO DISTRICT " + uniqueLetters(4);
	}

	public static String uniqueTestPincode() {
		long seed = (System.currentTimeMillis() % 90000) + 900000;
		return String.format("%06d", seed);
	}

	public static void setCreatedState(String code, String name) {
		createdStateCode = code;
		createdStateName = name;
	}

	/** Clears only State context so a later module can create a fresh active TEST state. */
	public static void clearCreatedState() {
		createdStateCode = null;
		createdStateName = null;
	}

	public static String createdStateCode() {
		return createdStateCode;
	}

	public static String createdStateName() {
		return createdStateName;
	}

	public static void setCreatedDistrict(String name, String stateCode) {
		createdDistrictName = name;
		createdDistrictStateCode = stateCode;
	}

	public static String createdDistrictName() {
		return createdDistrictName;
	}

	public static String createdDistrictStateCode() {
		return createdDistrictStateCode;
	}

	public static void setCreatedPincode(String pincode, String district) {
		createdPincode = pincode;
		createdPincodeDistrict = district;
	}

	/** Clears only Pincode context after leaf cleanup. */
	public static void clearCreatedPincode() {
		createdPincode = null;
		createdPincodeDistrict = null;
	}

	public static String createdPincode() {
		return createdPincode;
	}

	public static String createdPincodeDistrict() {
		return createdPincodeDistrict;
	}

	public static String uniqueTestFiName() {
		return "TEST AUTO FI " + uniqueLetters(4);
	}

	public static String uniqueTestFiCode() {
		long n = (System.currentTimeMillis() % 900000L) + 100000L;
		return "IN" + n;
	}

	public static String uniqueTestRegionName() {
		return "TEST AUTO REGION " + uniqueLetters(4);
	}

	public static String uniqueTestRegionCode() {
		return "RG" + uniqueLetters(4);
	}

	public static String uniqueTestCpcCode() {
		return "CPC" + (System.currentTimeMillis() % 90000L + 10000L);
	}

	public static String uniqueTestCpcCity() {
		return "TEST AUTO CITY " + uniqueLetters(3);
	}

	public static String uniqueTestBranchName() {
		return "TEST AUTO BRANCH " + uniqueLetters(4);
	}

	public static String uniqueTestBranchCode() {
		return "BR" + (System.currentTimeMillis() % 90000L + 10000L);
	}

	public static void setCreatedFi(String code, String name) {
		createdFiCode = code;
		createdFiName = name;
	}

	public static String createdFiCode() {
		return createdFiCode;
	}

	public static String createdFiName() {
		return createdFiName;
	}

	public static void setCreatedCpc(String code, String city) {
		createdCpcCode = code;
		createdCpcCity = city;
	}

	public static String createdCpcCode() {
		return createdCpcCode;
	}

	public static String createdCpcCity() {
		return createdCpcCity;
	}

	public static void setCreatedRegion(String code, String name) {
		createdRegionCode = code;
		createdRegionName = name;
	}

	public static String createdRegionCode() {
		return createdRegionCode;
	}

	public static String createdRegionName() {
		return createdRegionName;
	}

	public static void setCreatedBranch(String code, String name) {
		createdBranchCode = code;
		createdBranchName = name;
	}

	public static String createdBranchCode() {
		return createdBranchCode;
	}

	public static String createdBranchName() {
		return createdBranchName;
	}
}
