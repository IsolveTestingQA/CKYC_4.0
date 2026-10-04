/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.searchdownload;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SearchResultStore;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.searchdownload.SearchPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only document search coverage for Search And Download → Search.
 * Actual Excel/Extent rows include entered vs accepted values, button state,
 * toast text, result presence, and noted validation issues. Customer PII from
 * the response body is not copied into reports.
 */
public class SearchFunctionality extends BaseClass {

	private static final String PAN = readFixture("pan");
	private static final String MOBILE = readFixture("mobile");
	private final int waitSeconds = ConfigReader.getExplicitWait();

	public void openSearchFromMenu() {
		WebDriver driver = getDriver();
		MasterUiHelper.ensureSideNavOpen(driver, waitSeconds);
		if (!MasterUiHelper.isVisible(SideNavPage.SEARCH_AND_DOWNLOAD_GROUP, 1)) {
			MasterUiHelper.clickJs(SideNavPage.SEARCH_AND_DOWNLOAD, waitSeconds);
			PomElementManager.findVisible(SideNavPage.SEARCH_AND_DOWNLOAD_GROUP, waitSeconds);
		}
		MasterUiHelper.clickJs(SideNavPage.SEARCH, waitSeconds);
		PomElementManager.findVisible(SearchPage.DOCUMENT_TYPE_COMBOBOX, waitSeconds);
		MasterUiHelper.ensureSideNavClosed(driver, Math.min(waitSeconds, 5));
		String url = driver.getCurrentUrl();
		StepLog.info("Search | Navigate", "Open Search And Download → Search",
				"Search page opens with Document Type controls",
				"url=" + url + "; landedOnSearch=" + (url != null && url.contains("/search-download/search")),
				"module=Search And Download > Search");
		Hooks.captureMilestone("Search page opened");
	}

	public void verifySearchPageLoaded() {
		PomElementManager.findVisible(SearchPage.DOCUMENT_TYPE_COMBOBOX, waitSeconds);
		PomElementManager.findVisible(SearchPage.DOCUMENT_NUMBER_INPUT, waitSeconds);
		PomElementManager.findVisible(SearchPage.SEARCH_BUTTON, waitSeconds);
		boolean clearVisible = MasterUiHelper.isVisible(SearchPage.CLEAR_BUTTON, 2);
		StepLog.check("Search | Page load", "Verify Search controls",
				"Document Type, Document Number, Search and Clear visible",
				"documentType=visible; documentNumber=visible; search=visible; clearVisible=" + clearVisible,
				clearVisible, "page=Search");
		Hooks.captureMilestone("Search controls visible");
	}

	public void verifyPrerequisites() {
		boolean numberDisabled = isDisabled(SearchPage.DOCUMENT_NUMBER_INPUT);
		boolean searchDisabled = !MasterUiHelper.isButtonEnabled(SearchPage.SEARCH_BUTTON);
		List<String> issues = new ArrayList<>();
		if (!numberDisabled) {
			issues.add("ISSUE: Document Number enabled before Document Type");
		}
		if (!searchDisabled) {
			issues.add("ISSUE: Search enabled before Document Type");
		}
		boolean pass = numberDisabled && searchDisabled;
		StepLog.check("Search | Required Document Type",
				"Verify controls before selecting a document type",
				"Document Number and Search are disabled",
				"documentNumberDisabled=" + numberDisabled + "; searchDisabled=" + searchDisabled
						+ "; issues=" + (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				pass, "documentType=blank");
		if (!pass) {
			SoftAssertManager.recordFailure("Search prerequisite state",
					new AssertionError(String.join(" | ", issues)));
		}
	}

	public void runPanNegativeValidations() {
		selectDocumentType("PAN");
		assertDocumentMaxLength("PAN", 10);
		runNegativeTrial("PAN blank", "", false);
		runNegativeTrial("PAN short", "ABCDE1234", false);
		runNegativeTrial("PAN lowercase", "abcde1234f", false);
		runNegativeTrial("PAN malformed", "ABCDE12X4F", false);
		runNegativeTrial("PAN special character", "ABCDE123$F", false);
		runNegativeTrial("PAN over length", "ABCDE1234FA", false);
		Hooks.captureMilestone("PAN negative validations completed");
		clearAndVerifyReset();
	}

	public void searchPanAndRequireRecord() {
		selectDocumentType("PAN");
		enterDocument(PAN);
		String accepted = readDocument();
		boolean enabled = MasterUiHelper.isButtonEnabled(SearchPage.SEARCH_BUTTON);
		boolean valid = accepted.matches("[A-Z]{5}[0-9]{4}[A-Z]");
		List<String> issues = new ArrayList<>();
		if (!valid) {
			issues.add("ISSUE: accepted PAN does not match 5A+4N+1A");
		}
		if (!enabled) {
			issues.add("ISSUE: Search remained disabled for valid PAN");
		}
		StepLog.check("Search | PAN positive input", "Enter configured PAN from TestData",
				"Valid PAN retained and Search enabled",
				"entered='" + PAN + "'; accepted='" + accepted + "'; enteredLength=" + PAN.length()
						+ "; acceptedLength=" + accepted.length() + "; validPattern=" + valid
						+ "; searchEnabled=" + enabled
						+ "; issues=" + (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				valid && enabled, "fixture=TestData/search-temp.properties; pan=" + PAN);
		Hooks.captureMilestone("PAN positive input ready");
		if (!valid || !enabled) {
			throw new AssertionError("PAN positive input is not searchable | " + String.join(" | ", issues));
		}
		submitAndRequireMeaningfulRecord("PAN", "pan=" + PAN);
	}

	public void runMobileNegativeValidations() {
		selectDocumentType("Mobile Number without ISD Code");
		assertDocumentMaxLength("Mobile", 10);
		runNegativeTrial("Mobile blank", "", false);
		runNegativeTrial("Mobile short", "987654321", false);
		runNegativeTrial("Mobile over length", "98765432101", false);
		runNegativeTrial("Mobile alphabetic", "ABCDEFGHIJ", false);
		runNegativeTrial("Mobile special character", "98765@3210", false);
		Hooks.captureMilestone("Mobile negative validations completed");
		clearAndVerifyReset();
	}

	public void searchMobileAndRecordOutcome() {
		selectDocumentType("Mobile Number without ISD Code");
		enterDocument(MOBILE);
		String accepted = readDocument();
		boolean enabled = MasterUiHelper.isButtonEnabled(SearchPage.SEARCH_BUTTON);
		boolean valid = accepted.matches("[6-9][0-9]{9}");
		List<String> issues = new ArrayList<>();
		if (!valid) {
			issues.add("ISSUE: accepted mobile is not a 10-digit Indian mobile");
		}
		if (!enabled) {
			issues.add("ISSUE: Search remained disabled for valid mobile");
		}
		StepLog.check("Search | Mobile positive input", "Enter configured mobile from TestData",
				"10-digit mobile retained and Search enabled",
				"entered='" + MOBILE + "'; accepted='" + accepted + "'; enteredLength=" + MOBILE.length()
						+ "; acceptedLength=" + accepted.length() + "; validPattern=" + valid
						+ "; searchEnabled=" + enabled
						+ "; issues=" + (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				valid && enabled, "fixture=TestData/search-temp.properties; mobile=" + MOBILE);
		if (!valid || !enabled) {
			throw new AssertionError("Mobile positive input is not searchable | " + String.join(" | ", issues));
		}

		clickSearch();
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		String alert = alertDescription();
		boolean recordVisible = resultIsMeaningful();
		if (recordVisible) {
			validateAndPersistResult("Mobile", "mobile=" + MOBILE);
			return;
		}
		String responseText = toast.isBlank() ? alert : toast;
		boolean explicitFailure = containsAny(responseText, "no record", "not found", "invalid", "error", "failed",
				"failure");
		boolean bareSuccess = containsAny(responseText, "success");
		if (bareSuccess) {
			issues.add("APP DEFECT: red Success alert with no CKYC result");
		} else if (explicitFailure) {
			issues.add("Positive mobile search returned an explicit failure/no-record response");
		} else {
			issues.add("APP DEFECT: no result and no meaningful response message");
		}
		StepLog.check("Search | Mobile positive result",
				"Submit configured mobile search",
				"Visible CKYC result with populated fields",
				"submittedMobile='" + MOBILE + "'; recordVisible=false; alert=" + alert
						+ "; toast='" + toast + "'; responseClass="
						+ (bareSuccess ? "red-bare-success" : explicitFailure ? "explicit-failure" : "empty-response")
						+ "; issues=" + String.join(" | ", issues),
				false, "mobile=" + MOBILE);
		Hooks.captureMilestone("Mobile search response evidence - " + responseText, false);
	}

	public void clearAndVerifyReset() {
		if (MasterUiHelper.isVisible(SearchPage.CLEAR_BUTTON, 2)) {
			MasterUiHelper.clickJs(SearchPage.CLEAR_BUTTON, waitSeconds);
		}
		MasterUiHelper.sleep(300);
		String selected = textOf(SearchPage.DOCUMENT_TYPE_COMBOBOX);
		String number = readDocument();
		boolean searchDisabled = !MasterUiHelper.isButtonEnabled(SearchPage.SEARCH_BUTTON);
		boolean resultGone = getDriver().findElements(SearchPage.RESULT_IDENTIFIER).isEmpty();
		List<String> issues = new ArrayList<>();
		boolean typeReset = selected.isBlank() || selected.toLowerCase(Locale.ROOT).contains("document type");
		if (!typeReset) {
			issues.add("ISSUE: Document Type not reset after Clear");
		}
		if (!number.isBlank()) {
			issues.add("ISSUE: Document Number not cleared");
		}
		if (!searchDisabled) {
			issues.add("ISSUE: Search still enabled after Clear");
		}
		if (!resultGone) {
			issues.add("ISSUE: previous result still visible after Clear");
		}
		boolean reset = typeReset && number.isBlank() && searchDisabled && resultGone;
		StepLog.check("Search | Clear reset", "Click Clear after search trials",
				"Document Type/Number reset, Search disabled, previous result removed",
				"selected='" + selected + "'; number='" + number + "'; numberLength=" + number.length()
						+ "; searchDisabled=" + searchDisabled + "; resultGone=" + resultGone
						+ "; issues=" + (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				reset, "action=Clear");
		Hooks.captureMilestone(reset ? "Search Clear reset verified" : "Search Clear reset issue", reset);
		if (!reset) {
			SoftAssertManager.recordFailure("Search Clear reset",
					new AssertionError(String.join(" | ", issues)));
		}
	}

	private void selectDocumentType(String type) {
		MasterUiHelper.selectMuiOption(SearchPage.DOCUMENT_TYPE_COMBOBOX, type, waitSeconds);
		String selected = textOf(SearchPage.DOCUMENT_TYPE_COMBOBOX);
		boolean numberEnabled = !isDisabled(SearchPage.DOCUMENT_NUMBER_INPUT);
		boolean persisted = selected.contains(type);
		List<String> issues = new ArrayList<>();
		if (!persisted) {
			issues.add("ISSUE: Document Type selection did not persist");
		}
		if (!numberEnabled) {
			issues.add("ISSUE: Document Number stayed disabled after type selection");
		}
		StepLog.check("Search | Select document type", "Select " + type,
				"Selected type persists and enables Document Number",
				"selected='" + selected + "'; documentNumberEnabled=" + numberEnabled
						+ "; placeholder='" + readAttribute(SearchPage.DOCUMENT_NUMBER_INPUT, "placeholder")
						+ "'; maxLength='" + readAttribute(SearchPage.DOCUMENT_NUMBER_INPUT, "maxLength")
						+ "'; issues=" + (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				persisted && numberEnabled, "documentType=" + type);
		if (!persisted) {
			throw new AssertionError("Document type selection did not persist: " + type);
		}
	}

	private void assertDocumentMaxLength(String label, int expectedMax) {
		String maxLength = readAttribute(SearchPage.DOCUMENT_NUMBER_INPUT, "maxLength");
		boolean pass = String.valueOf(expectedMax).equals(maxLength);
		StepLog.check("Search | " + label + " maxLength",
				"Document Number maxLength should match banking rule",
				"maxLength=" + expectedMax,
				"observedMaxLength='" + maxLength + "'; issues="
						+ (pass ? "none" : "APP DEFECT: maxLength=" + maxLength + " expected=" + expectedMax),
				pass, "documentType=" + label + "; expectedMax=" + expectedMax);
		if (!pass) {
			SoftAssertManager.recordFailure(label + " maxLength",
					new AssertionError("APP DEFECT: Document Number maxLength=" + maxLength
							+ " expected=" + expectedMax));
		}
	}

	private void runNegativeTrial(String name, String entered, boolean expectedEnabled) {
		enterDocument(entered);
		String accepted = readDocument();
		boolean enabled = MasterUiHelper.isButtonEnabled(SearchPage.SEARCH_BUTTON);
		boolean pass = enabled == expectedEnabled;
		List<String> issues = new ArrayList<>();
		if (!pass) {
			issues.add("VALIDATION DEFECT: invalid value made Search enabled");
		}
		if (!entered.equals(accepted) && !accepted.isBlank()) {
			issues.add("NOTE: UI changed entered value to '" + accepted + "'");
		}
		StepLog.check("Search | " + name, "Enter invalid document number",
				"Search remains disabled for invalid document number",
				"entered='" + entered + "'; accepted='" + accepted + "'; enteredLength=" + entered.length()
						+ "; acceptedLength=" + accepted.length() + "; searchEnabled=" + enabled
						+ "; issues=" + (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				pass, "input=" + entered);
		if (!pass) {
			SoftAssertManager.recordFailure(name,
					new AssertionError(String.join(" | ", issues) + " | accepted=" + accepted));
		}
	}

	private void submitAndRequireMeaningfulRecord(String type, String testData) {
		clickSearch();
		String toast = ToastHandler.waitAndRead(getDriver(), 3000);
		boolean recordVisible = resultIsMeaningful();
		if (!recordVisible) {
			String alert = alertDescription();
			String issue = containsAny(toast + " " + alert, "success")
					? "APP DEFECT: alert says Success but no CKYC result is visible"
					: "ISSUE: no meaningful CKYC result section/identifier visible";
			StepLog.check("Search | " + type + " positive result", "Submit valid " + type + " search",
					"Visible CKYC result with populated fields",
					"recordVisible=false; alert=" + alert + "; toast='" + toast + "'; issues=" + issue,
					false, testData);
			Hooks.captureMilestone(type + " search response without record", false);
			return;
		}
		validateAndPersistResult(type, testData);
	}

	private void validateAndPersistResult(String type, String testData) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("CKYC Number", readResultValue("CKYC Number"));
		fields.put("CKYC Reference ID", readResultValue("CKYC Reference ID"));
		fields.put("Account Type", readResultValue("Account Type"));
		fields.put("KYC Date", readResultValue("KYC Date"));
		fields.put("Updated Date", readResultValue("Updated Date"));
		fields.put("Full Name", readResultValue("Full Name"));
		fields.put("Father / Spouse Full Name", readResultValue("Father / Spouse Full Name"));
		fields.put("Age", readResultValue("Age"));
		fields.put("Mobile Number", readResultValue("Mobile Number"));

		String maskedCkyc = fields.get("CKYC Number");
		String referenceId = fields.get("CKYC Reference ID");
		boolean maskedCorrectly = maskedCkyc.matches("[X*]+[0-9]{4}");
		boolean referencePresent = hasValue(referenceId);
		List<String> missing = new ArrayList<>();

		for (Map.Entry<String, String> entry : fields.entrySet()) {
			String label = entry.getKey();
			String value = entry.getValue();
			boolean populated = hasValue(value);
			boolean pass = populated;
			String expected = "CKYC Number".equals(label)
					? "Partially masked CKYC number ending in four visible digits"
					: "Non-empty value (not dash/placeholder)";
			if ("CKYC Number".equals(label)) {
				pass = populated && maskedCorrectly;
			}
			if (!pass) {
				missing.add(label + (populated ? " (mask invalid)" : " (missing)"));
			}
			String reportValue = isNameField(label) ? (populated ? "<present; redacted from report>" : value) : value;
			StepLog.check("Search | " + type + " response field | " + label,
					"Validate positive response field " + label, expected,
					"label='" + label + "'; value='" + reportValue + "'; populated=" + populated
							+ ("CKYC Number".equals(label) ? "; partiallyMasked=" + maskedCorrectly : ""),
					pass, testData);
		}

		boolean photoOk = validateCustomerPhoto(type, testData, missing);

		boolean identifiersUsable = maskedCorrectly && referencePresent;
		if (identifiersUsable) {
			SearchResultStore.save(maskedCkyc, referenceId, PAN, MOBILE);
		}
		boolean allFieldsValid = missing.isEmpty() && identifiersUsable && photoOk;
		StepLog.check("Search | " + type + " complete response",
				"Validate all response fields, Customer Photo, and persist Search-to-Download identifiers",
				"Every displayed field has a value; photo file present with type; CKYC Number masked; Reference ID saved",
				"recordVisible=true; maskedCkycNumber='" + maskedCkyc + "'; ckycReferenceId='" + referenceId
						+ "'; allFieldsPopulated=" + missing.isEmpty() + "; missingOrInvalid=" + missing
						+ "; customerPhotoOk=" + photoOk + "; identifiersSaved=" + identifiersUsable + "; savedAt='"
						+ SearchResultStore.latestFilePath() + "'; alert=" + alertDescription(),
				allFieldsValid, testData);
		Hooks.captureMilestone(type + " positive CKYC response evidence", allFieldsValid);
	}

	/**
	 * Customer Photo must show a type chip (jpg/png/…) and an actual image file.
	 * Placeholder-only (ImageOutlinedIcon) with a type chip is logged as missing file.
	 */
	private boolean validateCustomerPhoto(String type, String testData, List<String> missing) {
		boolean sectionVisible = MasterUiHelper.isVisible(SearchPage.CUSTOMER_PHOTO_LABEL, 2);
		String photoType = textOf(SearchPage.CUSTOMER_PHOTO_TYPE_CHIP);
		boolean typePresent = hasValue(photoType)
				&& photoType.toLowerCase(Locale.ROOT).matches("jpe?g|png|gif|bmp|webp|tif{1,2}|pdf");
		boolean placeholderOnly = !getDriver().findElements(SearchPage.CUSTOMER_PHOTO_PLACEHOLDER_ICON).isEmpty()
				&& getDriver().findElements(SearchPage.CUSTOMER_PHOTO_IMAGE).isEmpty();
		boolean imagePresent = isCustomerPhotoFilePresent();
		boolean previewOpened = false;
		String clickNote = "not-clicked";

		if (sectionVisible) {
			previewOpened = tryOpenCustomerPhotoPreview();
			clickNote = previewOpened ? "preview-opened" : "clicked-no-preview";
			if (previewOpened) {
				imagePresent = imagePresent || isCustomerPhotoFilePresent()
						|| !getDriver().findElements(SearchPage.PHOTO_PREVIEW_DIALOG).isEmpty();
				dismissPhotoPreviewIfOpen();
			}
		}

		List<String> issues = new ArrayList<>();
		if (!sectionVisible) {
			issues.add("ISSUE: Customer Photo section not visible");
		}
		if (!typePresent) {
			issues.add("ISSUE: photo type chip missing/invalid (observed='" + photoType + "')");
		}
		if (!imagePresent) {
			issues.add(placeholderOnly
					? "APP DEFECT: photo type shown but only placeholder icon — photo file not present"
					: "ISSUE: Customer Photo file/image not present");
		}

		boolean pass = sectionVisible && typePresent && imagePresent;
		if (!pass) {
			missing.add("Customer Photo"
					+ (typePresent ? " type=" + photoType : " type=missing")
					+ (imagePresent ? "" : " (file missing)"));
		}

		StepLog.check("Search | " + type + " response field | Customer Photo",
				"Validate Customer Photo label, photo type, and actual photo file",
				"Customer Photo visible with valid type and loaded image file",
				"sectionVisible=" + sectionVisible + "; photoType='" + photoType + "'; typePresent=" + typePresent
						+ "; imagePresent=" + imagePresent + "; placeholderOnly=" + placeholderOnly
						+ "; clickOutcome=" + clickNote + "; issues="
						+ (issues.isEmpty() ? "none" : String.join(" | ", issues)),
				pass, testData);
		Hooks.captureMilestone(type + " Customer Photo evidence"
				+ (pass ? "" : " - file missing"), pass);
		return pass;
	}

	private boolean isCustomerPhotoFilePresent() {
		List<WebElement> images = getDriver().findElements(SearchPage.CUSTOMER_PHOTO_IMAGE);
		for (WebElement image : images) {
			try {
				if (!image.isDisplayed()) {
					continue;
				}
				String src = image.getAttribute("src");
				String current = image.getAttribute("currentSrc");
				String effective = (current == null || current.isBlank()) ? src : current;
				if (effective == null || effective.isBlank() || effective.startsWith("data:image/svg")) {
					continue;
				}
				Object loaded = ((org.openqa.selenium.JavascriptExecutor) getDriver()).executeScript(
						"const img=arguments[0];"
								+ "return !!(img && img.complete && img.naturalWidth > 0 && img.naturalHeight > 0);",
						image);
				if (Boolean.TRUE.equals(loaded)) {
					return true;
				}
			} catch (Exception ignored) {
				// try next candidate
			}
		}
		return false;
	}

	private boolean tryOpenCustomerPhotoPreview() {
		try {
			List<WebElement> targets = getDriver().findElements(SearchPage.CUSTOMER_PHOTO_SECTION);
			if (targets.isEmpty()) {
				targets = getDriver().findElements(SearchPage.CUSTOMER_PHOTO_LABEL);
			}
			if (targets.isEmpty()) {
				return false;
			}
			WebElement target = targets.get(0);
			((org.openqa.selenium.JavascriptExecutor) getDriver()).executeScript(
					"arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", target);
			MasterUiHelper.sleep(500);
			return MasterUiHelper.isVisible(SearchPage.PHOTO_PREVIEW_DIALOG, 1)
					|| !getDriver().findElements(SearchPage.CUSTOMER_PHOTO_IMAGE).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	private void dismissPhotoPreviewIfOpen() {
		try {
			if (MasterUiHelper.isVisible(SearchPage.PHOTO_PREVIEW_DIALOG, 1)) {
				getDriver().findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
				MasterUiHelper.sleep(200);
			}
		} catch (Exception ignored) {
			// preview may already be closed
		}
	}

	private void enterDocument(String value) {
		WebElement input = PomElementManager.findVisible(SearchPage.DOCUMENT_NUMBER_INPUT, waitSeconds);
		input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		if (value != null && !value.isBlank()) {
			input.sendKeys(value);
		}
		MasterUiHelper.sleep(250);
	}

	private void clickSearch() {
		MasterUiHelper.clickJs(SearchPage.SEARCH_BUTTON, waitSeconds);
		MasterUiHelper.sleep(700);
	}

	private String readDocument() {
		try {
			String value = PomElementManager.findVisible(SearchPage.DOCUMENT_NUMBER_INPUT, waitSeconds)
					.getAttribute("value");
			return value == null ? "" : value.trim();
		} catch (Exception ignored) {
			return "";
		}
	}

	private String readAttribute(By locator, String attribute) {
		try {
			String value = PomElementManager.findVisible(locator, waitSeconds).getAttribute(attribute);
			return value == null ? "" : value.trim();
		} catch (Exception ignored) {
			return "";
		}
	}

	private boolean isDisabled(By locator) {
		List<WebElement> elements = getDriver().findElements(locator);
		if (elements.isEmpty()) {
			return true;
		}
		WebElement element = elements.get(0);
		return !element.isEnabled() || "true".equalsIgnoreCase(element.getAttribute("disabled"))
				|| "true".equalsIgnoreCase(element.getAttribute("aria-disabled"));
	}

	private boolean resultIsMeaningful() {
		return hasValue(readResultValue("CKYC Number"))
				&& hasValue(readResultValue("CKYC Reference ID"));
	}

	private String textOf(By locator) {
		List<WebElement> elements = getDriver().findElements(locator);
		return elements.isEmpty() ? "" : elements.get(0).getText().trim();
	}

	private String readResultValue(String label) {
		return textOf(SearchPage.resultValue(label));
	}

	private String alertDescription() {
		List<WebElement> alerts = getDriver().findElements(SearchPage.ALERT);
		if (alerts.isEmpty()) {
			return "none";
		}
		WebElement alert = alerts.get(0);
		String text = alert.getText() == null ? "" : alert.getText().trim();
		String classes = alert.getAttribute("class");
		String severity = containsAny(classes, "colorerror", "standarderror") ? "error/red"
				: containsAny(classes, "colorsuccess", "standardsuccess") ? "success"
						: containsAny(classes, "colorwarning", "standardwarning") ? "warning" : "unknown";
		return "text='" + text + "'; severity=" + severity + "; class='" + classes + "'";
	}

	private static boolean hasValue(String value) {
		if (value == null) {
			return false;
		}
		String normalized = value.trim();
		return !normalized.isEmpty() && !normalized.equals("—") && !normalized.equals("-")
				&& !normalized.equalsIgnoreCase("N/A") && !normalized.equalsIgnoreCase("NA")
				&& !normalized.equalsIgnoreCase("null");
	}

	private static boolean isNameField(String label) {
		return "Full Name".equals(label) || "Father / Spouse Full Name".equals(label);
	}

	private static boolean containsAny(String value, String... candidates) {
		String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
		for (String candidate : candidates) {
			if (text.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private static String readFixture(String key) {
		return SearchTestData.get(key);
	}
}
