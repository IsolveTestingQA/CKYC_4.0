/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;

/**
 * Persona of the open record. LE = the Legal Entity toggle is active (header "DVS 2.0 Legal Entity").
 * Minor = Account Type text starts with "05 - MINOR" (helper "CKYC KYC Type M") and the Minor box is ticked.
 * Anything else is Individual. Never decided by the customer-ID prefix alone.
 */
public enum DvsPersona {
	INDIVIDUAL, MINOR, LEGAL_ENTITY, UNKNOWN;

	/** Pure rule used by detect(); also easy to test without a browser. */
	public static DvsPersona fromSignals(boolean legalEntityHeader, String accountTypeText, boolean kycTypeMHelper,
			boolean minorChecked) {
		if (legalEntityHeader) {
			return LEGAL_ENTITY;
		}
		String t = accountTypeText == null ? "" : accountTypeText.trim().toUpperCase(Locale.ROOT);
		if (t.isEmpty()) {
			return UNKNOWN;
		}
		boolean minorText = t.startsWith("05 - MINOR") || t.startsWith("05-MINOR");
		if (minorText && (kycTypeMHelper || minorChecked)) {
			return MINOR;
		}
		return minorText ? MINOR : INDIVIDUAL;
	}

	public static DvsPersona detect() {
		WebDriver d = BaseClass.getDriver();
		DvsLocators l = DvsLocators.get();
		if (d == null) {
			return UNKNOWN;
		}
		try {
			boolean le = anyDisplayed(d.findElements(l.by("COM_HDR_LegalEntity")));
			String account = "";
			List<WebElement> acc = d.findElements(l.by("IND_PER_accountType"));
			if (!acc.isEmpty()) {
				account = acc.get(0).getText();
			}
			boolean helper = anyDisplayed(d.findElements(l.by("COM_HDR_KycTypeMinor")));
			boolean minorBox = false;
			List<WebElement> box = d.findElements(l.by("IND_PER_minorFlag"));
			if (!box.isEmpty()) {
				minorBox = box.get(0).isSelected();
			}
			return fromSignals(le, account, helper, minorBox);
		} catch (RuntimeException e) {
			return UNKNOWN;
		}
	}

	private static boolean anyDisplayed(List<WebElement> els) {
		for (WebElement e : els) {
			try {
				if (e.isDisplayed()) {
					return true;
				}
			} catch (RuntimeException ignored) {
				// stale element - treat as not shown
			}
		}
		return false;
	}
}
