/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.validation;

/**
 * Checksum algorithms used by the CKYC/CERSAI field formats (Aadhaar = Verhoeff, GSTIN = its own
 * mod-36 check digit). Ported 1:1 from {@code ckyc_validate.py}'s {@code verhoeff}/{@code gstin_ok}
 * (see CKYC_OFFICIAL_DOCUMENTS/CKYC_Cosmos_Validation_Spec) — do not hand-tune the tables below.
 */
public final class Checksums {

	private static final int[][] D = {
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 1, 2, 3, 4, 0, 6, 7, 8, 9, 5 }, { 2, 3, 4, 0, 1, 7, 8, 9, 5, 6 },
			{ 3, 4, 0, 1, 2, 8, 9, 5, 6, 7 }, { 4, 0, 1, 2, 3, 9, 5, 6, 7, 8 }, { 5, 9, 8, 7, 6, 0, 4, 3, 2, 1 },
			{ 6, 5, 9, 8, 7, 1, 0, 4, 3, 2 }, { 7, 6, 5, 9, 8, 2, 1, 0, 4, 3 }, { 8, 7, 6, 5, 9, 3, 2, 1, 0, 4 },
			{ 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 } };
	private static final int[][] P = {
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 1, 5, 7, 6, 2, 8, 3, 0, 9, 4 }, { 5, 8, 0, 3, 7, 9, 6, 1, 4, 2 },
			{ 8, 9, 1, 6, 0, 4, 3, 5, 2, 7 }, { 9, 4, 5, 3, 1, 2, 6, 8, 7, 0 }, { 4, 2, 8, 6, 5, 7, 3, 9, 0, 1 },
			{ 2, 7, 9, 3, 8, 0, 6, 4, 1, 5 }, { 7, 0, 4, 6, 9, 1, 3, 2, 5, 8 } };

	private static final String G36 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private Checksums() {
	}

	/** Verhoeff check-digit algorithm (used for Aadhaar). {@code n} must be digits only. */
	public static boolean verhoeff(String n) {
		int c = 0;
		String reversed = new StringBuilder(n).reverse().toString();
		for (int i = 0; i < reversed.length(); i++) {
			int digit = reversed.charAt(i) - '0';
			c = D[c][P[i % 8][digit]];
		}
		return c == 0;
	}

	/** GSTIN mod-36 check digit (last of the 15 characters). */
	public static boolean gstinOk(String g) {
		if (g.length() != 15) {
			return false;
		}
		for (int i = 0; i < g.length(); i++) {
			if (G36.indexOf(g.charAt(i)) < 0) {
				return false;
			}
		}
		int s = 0;
		for (int i = 0; i < 14; i++) {
			int v = G36.indexOf(g.charAt(i)) * (i % 2 == 0 ? 1 : 2);
			s += v / 36 + v % 36;
		}
		char expected = G36.charAt((36 - s % 36) % 36);
		return g.charAt(14) == expected;
	}
}
