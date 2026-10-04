/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.masters.pincode;

import Com.Ckyc_4_0.functionality.masters.district.DistrictMasterFunctionality;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.MasterTestContext;
import Com.Ckyc_4_0.UtilityFiles.PincodeMasterStore;
import Com.Ckyc_4_0.UtilityFiles.PincodeMasterStore.PincodeRecord;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.pages.masters.pincode.PincodeMasterPage;
import Com.Ckyc_4_0.utils.MasterFieldRules;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.PopupHandler;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pincode Master — CRUD only on a newly created unique pincode linked to the test State/District.
 */
public class PincodeMasterFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(PincodeMasterFunctionality.class);
	private final int waitSeconds = ConfigReader.getExplicitWait();
	private final Random random = new Random();
	private final DistrictMasterFunctionality districtMaster = new DistrictMasterFunctionality();

	public void openPincodeMasterFromMenu() {
		MasterUiHelper.openMastersSubmodule("Pincode Master", PincodeMasterPage.PAGE_HEADING, waitSeconds);
		StepLog.info("Pincode Master | Navigate", "Open Masters → Pincode Master", "Pincode Master visible",
				"Heading Pincode Master", "menu=Pincode Master");
		Hooks.captureMilestone("Pincode Master page opened");
	}

	public void verifyPincodeMasterPageLoaded() {
		PomElementManager.findVisible(PincodeMasterPage.PAGE_HEADING, waitSeconds);
		PomElementManager.findVisible(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
		int total = MasterUiHelper.readChip(PincodeMasterPage.TOTAL_CHIP, waitSeconds);
		StepLog.info("Pincode Master | Page load", "Verify Pincode Master loaded",
				"Heading + search + chips visible", "Loaded | Total Pincodes=" + total, "total=" + total);
	}

	public void verifyBadges() {
		int total = MasterUiHelper.readChip(PincodeMasterPage.TOTAL_CHIP, waitSeconds);
		int active = MasterUiHelper.readChip(PincodeMasterPage.ACTIVE_CHIP, waitSeconds);
		int districts = MasterUiHelper.readChip(PincodeMasterPage.DISTRICTS_COVERED_CHIP, waitSeconds);
		int states = MasterUiHelper.readChip(PincodeMasterPage.STATES_COVERED_CHIP, waitSeconds);
		boolean pass = total >= 0 && active >= 0 && total >= active;
		StepLog.check("Pincode Master | Badges",
				"Total Pincodes >= Active (Inactive chip not shown on this page)",
				"Total >= Active; coverage chips readable",
				"Total=" + total + " Active=" + active + " impliedInactive=" + (total - active)
						+ " DistrictsCovered=" + districts + " StatesCovered=" + states,
				pass, "Total=" + total + "; Active=" + active);
	}

	public void ensureTestSafeStateAndDistrict() {
		if (MasterTestContext.createdDistrictName() != null && MasterTestContext.createdStateName() != null) {
			StepLog.info("Pincode Master | Test-safe geography", "Reuse created TEST state/district",
					"State and District available",
					"state=" + MasterTestContext.createdStateName() + " district="
							+ MasterTestContext.createdDistrictName(),
					"reuse=true");
			return;
		}
		districtMaster.openDistrictMasterFromMenu();
		districtMaster.ensureTestSafeState();
		districtMaster.createUniqueTestDistrict();
		openPincodeMasterFromMenu();
	}

	public void loadVisiblePincodesIntoMap() {
		PincodeMasterStore.clear();
		MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
		// One XPath reads every row currently rendered; do not paginate the 18k master.
		readVisiblePincodeRows();
		MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
		boolean pass = PincodeMasterStore.size() > 0;
		StepLog.check("Pincode Master | Load visible map | count=" + PincodeMasterStore.size(),
				"Store visible pincode rows only (do not paginate 18k)", "Visible map size > 0",
				"Stored " + PincodeMasterStore.size() + " visible pincodes", pass,
				"mapSize=" + PincodeMasterStore.size() + "; currentPageOnly=true; fullLoad=false");
	}

	/** Randomly verify the State filter using up to two States from visible pincode rows. */
	public void verifyTwoRandomStateFilters() {
		List<String> states = PincodeMasterStore.values().stream()
				.map(PincodeRecord::state).filter(s -> s != null && !s.isBlank()).distinct()
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		Collections.shuffle(states, random);
		int samples = Math.min(2, states.size());
		if (samples == 0) {
			throw new AssertionError("No State values available in visible Pincode rows");
		}
		for (int i = 0; i < samples; i++) {
			String state = states.get(i);
			MasterUiHelper.selectFilterOption(PincodeMasterPage.STATE_FILTER, state, waitSeconds);
			List<WebElement> rows = getDriver().findElements(MasterUiHelper.GRID_DATA_ROWS);
			boolean correct = !rows.isEmpty() && rows.stream().allMatch(row -> {
				List<WebElement> cells = row.findElements(By.xpath(".//div[@role='gridcell']"));
				return cells.size() > 3 && cells.get(3).getText().toUpperCase()
						.contains(state.toUpperCase());
			});
			StepLog.check("Pincode Master | State filter sample " + (i + 1),
					"Select random State filter", "Only pincodes for selected State are shown",
					"selectedState='" + state + "' visibleRows=" + rows.size() + " correct=" + correct,
					correct, "sample=" + (i + 1) + "/" + samples + "; state=" + state);
		}
		MasterUiHelper.selectFilterOption(PincodeMasterPage.STATE_FILTER, "All States", waitSeconds);
	}

	private void readVisiblePincodeRows() {
		for (WebElement row : getDriver().findElements(MasterUiHelper.GRID_DATA_ROWS)) {
			try {
				List<WebElement> cells = row.findElements(By.xpath(".//div[@role='gridcell']"));
				if (cells.size() < 4) {
					continue;
				}
				String pin = cells.get(1).getText().trim();
				String district = cells.get(2).getText().trim();
				String state = firstNonBlankLine(cells.get(3).getText());
				boolean active = false;
				for (WebElement cb : row.findElements(By.cssSelector("input[type='checkbox']"))) {
					if (cb.isSelected() || "true".equalsIgnoreCase(cb.getAttribute("aria-checked"))) {
						active = true;
						break;
					}
				}
				if (!pin.isBlank()) {
					PincodeMasterStore.put(new PincodeRecord(pin, district, state, active));
				}
			} catch (Exception e) {
				logger.debug("Skip pincode row: {}", e.getMessage());
			}
		}
	}

	public void randomSearchFromMapWithRetry() {
		List<PincodeRecord> pool = new ArrayList<>(PincodeMasterStore.values());
		if (pool.isEmpty()) {
			loadVisiblePincodesIntoMap();
			pool = new ArrayList<>(PincodeMasterStore.values());
		}
		Collections.shuffle(pool, random);
		int maxAttempts = Math.min(2, pool.size());
		boolean passed = maxAttempts > 0;
		String last = "";
		for (int i = 0; i < maxAttempts; i++) {
			PincodeRecord pick = pool.get(i);
			StepLog.info("Pincode Master | Random search input " + (i + 1),
					"Enter randomly selected existing pincode", "Search receives test data",
					"Typing query='" + pick.pincode() + "'",
					"sample=" + (i + 1) + "/" + maxAttempts + "; query=" + pick.pincode()
							+ "; district=" + pick.district() + "; state=" + pick.state());
			MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pick.pincode(), waitSeconds);
			boolean found = MasterUiHelper.rowContains(pick.pincode());
			last = "Attempt " + (i + 1) + " query='" + pick.pincode() + "' found=" + found;
			StepLog.check("Pincode Master | Random search | " + pick.pincode(),
					"Search randomly selected visible pincode", "Grid shows expected pincode", last, found,
					"query=" + pick.pincode() + "; district=" + pick.district());
			passed = passed && found;
			MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
		}
		if (!passed) {
			SoftAssertManager.recordFailure("Pincode random search", new AssertionError(last));
		}
	}

	public void runFieldValidations() {
		openCreatePanel();
		MasterUiHelper.clickJs(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
		boolean districtDisabled = false;
		try {
			WebElement district = getDriver().findElement(PincodeMasterPage.DISTRICT_COMBO);
			districtDisabled = "true".equalsIgnoreCase(district.getAttribute("aria-disabled"))
					|| !district.isEnabled();
		} catch (Exception ignored) {
			districtDisabled = true;
		}
		StepLog.check("Pincode Master | District depends on State",
				"District combobox disabled until State is selected",
				"District disabled before State selection", "districtDisabled=" + districtDisabled, districtDisabled,
				"dependency=State→District");

		preparePincodeNegativeTrial("Non-numeric");
		MasterUiHelper.typeInto(PincodeMasterPage.PINCODE_INPUT, "12AB", waitSeconds);
		String letters = MasterUiHelper.readValue(PincodeMasterPage.PINCODE_INPUT, waitSeconds);
		boolean lettersBlocked = !letters.equals("12AB")
				|| !MasterUiHelper.isButtonEnabled(PincodeMasterPage.CREATE_BUTTON);
		StepLog.check("Pincode Master | Non-numeric", "Pincode must be numeric 6-digit",
				"Letters rejected or Create disabled",
				MasterFieldRules.describeLengthTrial("Pincode", "12AB", letters), lettersBlocked,
				"entered=12AB; accepted=" + letters);

		preparePincodeNegativeTrial("Length 5");
		MasterUiHelper.typeInto(PincodeMasterPage.PINCODE_INPUT, "12345", waitSeconds);
		String shortPin = MasterUiHelper.readValue(PincodeMasterPage.PINCODE_INPUT, waitSeconds);
		boolean shortBlocked = !MasterFieldRules.isValidPincode(shortPin)
				|| !MasterUiHelper.isButtonEnabled(PincodeMasterPage.CREATE_BUTTON);
		StepLog.check("Pincode Master | Length 5", "Pincode must be exactly 6 digits",
				"Length 5 must not enable Create",
				MasterFieldRules.describeLengthTrial("Pincode", "12345", shortPin), shortBlocked,
				"entered=12345; accepted=" + shortPin);

		String over = MasterFieldRules.overLengthPincodeSample();
		preparePincodeNegativeTrial("Length max 6");
		MasterUiHelper.typeInto(PincodeMasterPage.PINCODE_INPUT, over, waitSeconds);
		String acceptedOver = MasterUiHelper.readValue(PincodeMasterPage.PINCODE_INPUT, waitSeconds);
		boolean lenOk = acceptedOver.length() <= MasterFieldRules.PINCODE_LENGTH;
		StepLog.check("Pincode Master | Length max 6", "Over-length pincode must trim/block",
				"Accepted length <= 6",
				MasterFieldRules.describeLengthTrial("Pincode", over, acceptedOver), lenOk,
				"enteredLen=" + over.length() + "; acceptedLen=" + acceptedOver.length());

		try {
			MasterUiHelper.clickJs(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
		} catch (Exception e) {
			StepLog.check("Pincode Master | Clear", "Clear form", "Cleared", e.getMessage(), false, "action=Clear");
		}
	}

	private void preparePincodeNegativeTrial(String trial) {
		MasterUiHelper.clickJs(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
		String stateName = MasterTestContext.createdStateName();
		String stateCode = MasterTestContext.createdStateCode();
		String districtName = MasterTestContext.createdDistrictName();
		List<String> candidates = DistrictMasterFunctionality.testStateCandidates(stateName, stateCode);
		String testData = "trial=" + trial + "; stateName=" + stateName + "; stateCode=" + stateCode
				+ "; candidates=" + candidates + "; district=" + districtName;
		try {
			MasterUiHelper.selectMuiOption(PincodeMasterPage.STATE_COMBO, candidates, waitSeconds);
			String selectedState = getDriver().findElement(PincodeMasterPage.STATE_COMBO).getText().trim();
			boolean stateConfirmed = candidates.stream()
					.filter(v -> v != null && !v.isBlank())
					.anyMatch(v -> selectedState.equalsIgnoreCase(v) || selectedState.contains(v));
			if (!stateConfirmed) {
				throw new IllegalStateException("State dropdown did not retain a candidate; selected='"
						+ selectedState + "'");
			}

			MasterUiHelper.selectMuiOption(PincodeMasterPage.DISTRICT_COMBO, districtName, waitSeconds);
			String selectedDistrict = getDriver().findElement(PincodeMasterPage.DISTRICT_COMBO).getText().trim();
			boolean districtConfirmed = districtName != null
					&& (selectedDistrict.equalsIgnoreCase(districtName)
							|| selectedDistrict.contains(districtName));
			StepLog.info("Pincode Master | Negative trial prerequisites",
					"Reselect State and District before entering each invalid Pincode",
					"State and District dropdown selections confirmed",
					"trial='" + trial + "' selectedState='" + selectedState
							+ "' selectedDistrict='" + selectedDistrict + "' confirmed="
							+ (stateConfirmed && districtConfirmed),
					testData + "; selectedState=" + selectedState
							+ "; selectedDistrict=" + selectedDistrict);
			if (!districtConfirmed) {
				throw new IllegalStateException("District dropdown did not retain '" + districtName
						+ "'; selected='" + selectedDistrict + "'");
			}
		} catch (Exception e) {
			throw new PopupHandler.ModuleBlockerException("Pincode Master",
					"Cannot execute negative Pincode trial because State/District selection was reset or not retained: "
							+ trial,
					testData + "; dropdownFailure=" + e.getMessage());
		}
	}

	/** Banking negative: an existing State+District+Pincode must be rejected. */
	public void verifyExistingPincodeDuplicateRejected() {
		if (PincodeMasterStore.size() == 0) {
			loadVisiblePincodesIntoMap();
		}
		PincodeRecord existing = PincodeMasterStore.values().stream()
				.filter(r -> MasterFieldRules.isValidPincode(r.pincode())
						&& r.state() != null && !r.state().isBlank()
						&& r.district() != null && !r.district().isBlank())
				.findFirst()
				.orElseThrow(() -> new AssertionError("No valid existing Pincode available for duplicate test"));
		openCreatePanel();
		MasterUiHelper.selectAutocomplete(PincodeMasterPage.STATE_COMBO, existing.state(), waitSeconds);
		MasterUiHelper.selectAutocomplete(PincodeMasterPage.DISTRICT_COMBO, existing.district(), waitSeconds);
		MasterUiHelper.typeSlowly(PincodeMasterPage.PINCODE_INPUT, existing.pincode(), waitSeconds);
		boolean enabled = MasterUiHelper.isButtonEnabled(PincodeMasterPage.CREATE_BUTTON);
		String toast = "";
		if (enabled) {
			MasterUiHelper.clickJs(PincodeMasterPage.CREATE_BUTTON, waitSeconds);
			toast = ToastHandler.waitAndRead(getDriver(), 3000);
		}
		boolean rejected = enabled && isDuplicateMessage(toast);
		StepLog.check("Pincode Master | Duplicate existing Pincode",
				"Submit existing State+District+Pincode combination",
				"Creation rejected with already-exists/duplicate toaster",
				"createEnabled=" + enabled + " toast='" + toast + "'", rejected,
				"pincode=" + existing.pincode() + "; state=" + existing.state()
						+ "; district=" + existing.district());
		ToastHandler.dismissIfPresent(getDriver());
		if (MasterUiHelper.isVisible(PincodeMasterPage.CLEAR_BUTTON, 1)) {
			MasterUiHelper.clickJs(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
		}
	}

	public void createUniqueTestPincode() {
		ensureTestSafeStateAndDistrict();
		if (!MasterUiHelper.isVisible(PincodeMasterPage.PAGE_HEADING, 2)) {
			openPincodeMasterFromMenu();
		}
		String stateName = MasterTestContext.createdStateName();
		String districtName = MasterTestContext.createdDistrictName();
		StringBuilder attempts = new StringBuilder();
		for (int attempt = 1; attempt <= 3; attempt++) {
			String pin = nextUnusedPincode();
			openCreatePanel();
			MasterUiHelper.clickJs(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
			List<String> stateCandidates = DistrictMasterFunctionality.testStateCandidates(
					stateName, MasterTestContext.createdStateCode());
			String selectionData = "attempt=" + attempt + "/3; selectedState=" + stateName
					+ "; selectedStateCode=" + MasterTestContext.createdStateCode()
					+ "; candidates=" + stateCandidates
					+ "; selectedDistrict=" + districtName + "; pincodeEntered=" + pin;
			try {
				MasterUiHelper.selectMuiOption(PincodeMasterPage.STATE_COMBO, stateCandidates, waitSeconds);
				String selectedState = getDriver().findElement(PincodeMasterPage.STATE_COMBO).getText().trim();
				boolean stateConfirmed = stateCandidates.stream()
						.filter(v -> v != null && !v.isBlank())
						.anyMatch(v -> selectedState.equalsIgnoreCase(v) || selectedState.contains(v));
				if (!stateConfirmed) {
					throw new IllegalStateException("State dropdown selection not retained; selected='"
							+ selectedState + "'");
				}
				MasterUiHelper.selectMuiOption(PincodeMasterPage.DISTRICT_COMBO, districtName, waitSeconds);
				String selectedDistrict = getDriver().findElement(PincodeMasterPage.DISTRICT_COMBO).getText().trim();
				if (districtName == null || !(selectedDistrict.equalsIgnoreCase(districtName)
						|| selectedDistrict.contains(districtName))) {
					throw new IllegalStateException("District dropdown selection not retained; expected='"
							+ districtName + "' selected='" + selectedDistrict + "'");
				}
			} catch (Exception e) {
				String failedData = selectionData + "; dropdownFailure=" + e.getMessage();
				appendAttempt(attempts, failedData);
				StepLog.info("Pincode Master | State/District dropdown candidate failed",
						"Select the successful State and District created earlier in this run",
						"Exact failed dropdown data is recorded before retry",
						"Selection failed; retrying another Pincode candidate | " + e.getMessage(),
						failedData);
				closeCreatePanelIfVisible();
				continue;
			}
			MasterUiHelper.typeSlowly(PincodeMasterPage.PINCODE_INPUT, pin, waitSeconds);
			String acceptedPin = MasterUiHelper.readValue(PincodeMasterPage.PINCODE_INPUT, waitSeconds);
			boolean enabled = MasterUiHelper.isButtonEnabled(PincodeMasterPage.CREATE_BUTTON);
			String testData = "attempt=" + attempt + "/3; pinEntered=" + pin
					+ "; pinAccepted=" + acceptedPin + "; state=" + stateName
					+ "; district=" + districtName + "; createEnabled=" + enabled;
			appendAttempt(attempts, testData);
			if (!enabled) {
				StepLog.info("Pincode Master | Positive candidate disabled",
						"Create dedicated 6-digit TEST pincode",
						"Retry another valid Pincode candidate",
						"Create disabled; retrying", testData);
				closeCreatePanelIfVisible();
				continue;
			}
			MasterUiHelper.clickJs(PincodeMasterPage.CREATE_BUTTON, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2500);
			if (isDuplicateMessage(toast)) {
				StepLog.info("Pincode Master | Positive candidate collision",
						"Backend rejected generated pincode as duplicate",
						"Search it and retry with another random pincode",
						"toast='" + toast + "'; retrying", testData);
				ToastHandler.dismissIfPresent(getDriver());
				closeCreatePanelIfVisible();
				MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, acceptedPin, waitSeconds);
				MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
				continue;
			}
			boolean created = toast.toLowerCase().contains("created successfully")
					|| toast.toLowerCase().contains("success");
			waitForCreatePanelToClose();
			MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, acceptedPin, waitSeconds);
			boolean found = MasterUiHelper.rowContains(acceptedPin);
			if (created && found) {
				MasterTestContext.setCreatedPincode(acceptedPin, districtName);
				PincodeMasterStore.put(new PincodeRecord(acceptedPin, districtName, stateName, true));
				StepLog.check("Pincode Master | Create unique TEST pincode",
						"Create unique pincode linked to last-created TEST State/District",
						"Success toast and new Pincode row both confirmed",
						"createdByToast=true; found=true; toast='" + toast + "'",
						true, testData + "; toast=" + toast);
				MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
				return;
			}
			StepLog.info("Pincode Master | Positive candidate not persisted",
					"Create valid Pincode and verify persistence",
					"Success toast and matching grid row",
					"createdByToast=" + created + "; found=" + found + "; toast='" + toast
							+ "'; retrying",
					testData + "; toast=" + toast);
			MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
		}
		throw new PopupHandler.ModuleBlockerException("Pincode Master",
				"Valid Pincode data was tried under the run's successful State/District, but no Pincode was saved.",
				attempts.toString());
	}

	private static void appendAttempt(StringBuilder attempts, String detail) {
		if (attempts.length() > 0) {
			attempts.append(" || ");
		}
		attempts.append(detail);
	}

	public void editOnlyCreatedTestPincode() {
		String pin = MasterTestContext.createdPincode();
		if (pin == null) {
			StepLog.info("Pincode Master | Edit TEST pincode", "Skipped", "Edit created only", "Skipped",
					"safety=existing untouched");
			return;
		}
		MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
		By edit = MasterUiHelper.editButton(pin);
		if (getDriver().findElements(edit).isEmpty()) {
			StepLog.check("Pincode Master | Edit TEST pincode", "Open Edit for created pincode",
					"Edit button on created row", "Not found", false, "pin=" + pin);
			return;
		}
		MasterUiHelper.clickJs(edit, waitSeconds);
		MasterUiHelper.sleep(400);
		if (!getDriver().findElements(PincodeMasterPage.SAVE_BUTTON).isEmpty()) {
			MasterUiHelper.clickJs(PincodeMasterPage.SAVE_BUTTON, waitSeconds);
		} else {
			MasterUiHelper.clickSaveIfPresent(waitSeconds);
		}
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
		boolean found = MasterUiHelper.rowContains(pin);
		StepLog.check("Pincode Master | Edit TEST pincode", "Open/save Edit only on created pincode",
				"Created pincode still present after Save", "found=" + found + " toast='" + toast + "'", found,
				"pin=" + pin);
		MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void toggleOnlyCreatedTestPincode() {
		String pin = MasterTestContext.createdPincode();
		if (pin == null) {
			StepLog.info("Pincode Master | Toggle TEST pincode", "Skipped", "Toggle created only", "Skipped", "");
			return;
		}
		MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
		By toggle = MasterUiHelper.toggleFor(pin);
		if (getDriver().findElements(toggle).isEmpty()) {
			StepLog.check("Pincode Master | Toggle TEST pincode", "Toggle created pincode",
					"Checkbox on created row", "Not found", false, "pin=" + pin);
			return;
		}
		boolean before = isChecked(toggle);
		int activeBefore = MasterUiHelper.readActiveChip();
		MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(600);
		boolean after = isChecked(toggle);
		int activeAfter = MasterUiHelper.readActiveChip();
		boolean changed = before != after || activeBefore != activeAfter;
		StepLog.check("Pincode Master | Toggle TEST pincode", "Toggle Active on created pincode only",
				"Checkbox or Active chip changes",
				"checked " + before + "→" + after + " Active " + activeBefore + "→" + activeAfter
						+ " toast='" + toast + "'",
				changed, "pin=" + pin);
		MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void searchCreatedTestPincode() {
		String pin = MasterTestContext.createdPincode();
		if (pin == null) {
			StepLog.info("Pincode Master | Search TEST pincode", "Skipped", "Search created", "Skipped", "");
			return;
		}
		MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
		boolean found = MasterUiHelper.rowContains(pin);
		StepLog.check("Pincode Master | Search TEST pincode", "Search newly created pincode",
				"Grid shows created pincode", "found=" + found, found, "query=" + pin);
		MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void cleanupCreatedTestPincode() {
		String pin = MasterTestContext.createdPincode();
		if (pin == null) {
			StepLog.info("Pincode Master | Cleanup", "Nothing to clean", "Delete or deactivate created only",
					"Skipped", "");
			return;
		}
		MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
		if (MasterUiHelper.deleteAvailable()) {
			MasterUiHelper.clickDeleteIfPresent(pin, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2000);
			MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
			boolean gone = !MasterUiHelper.rowContains(pin);
			StepLog.check("Pincode Master | Cleanup", "Delete created TEST pincode", "Row removed",
					"deleted=" + gone + " toast='" + toast + "'", gone, "pin=" + pin);
		} else {
			By toggle = MasterUiHelper.toggleFor(pin);
			if (!getDriver().findElements(toggle).isEmpty() && isChecked(toggle)) {
				MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
				ToastHandler.waitAndRead(getDriver(), 1500);
			}
			boolean inactive = getDriver().findElements(toggle).isEmpty() || !isChecked(toggle);
			StepLog.info("Pincode Master | Cleanup", "Delete not in UI — deactivate created pincode",
					"Created pincode left inactive", "deleteAvailable=false | inactive=" + inactive + " | pin=" + pin,
					"cleanup=deactivate");
		}
		MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
		MasterTestContext.clearCreatedPincode();
	}

	private void openCreatePanel() {
		if (MasterUiHelper.isVisible(PincodeMasterPage.CREATE_HEADING, 1)
				&& MasterUiHelper.isVisible(PincodeMasterPage.PINCODE_INPUT, 1)
				&& MasterUiHelper.isVisible(PincodeMasterPage.CLEAR_BUTTON, 1)) {
			return;
		}
		MasterUiHelper.clickJs(PincodeMasterPage.NEW_PINCODE_BUTTON, waitSeconds);
		PomElementManager.findVisible(PincodeMasterPage.CREATE_HEADING, waitSeconds);
		PomElementManager.findVisible(PincodeMasterPage.PINCODE_INPUT, waitSeconds);
		PomElementManager.findClickable(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
	}

	private String nextUnusedPincode() {
		for (int i = 0; i < 20; i++) {
			String pin = MasterTestContext.uniqueTestPincode();
			MasterUiHelper.search(PincodeMasterPage.SEARCH_INPUT, pin, waitSeconds);
			boolean exists = MasterUiHelper.rowContains(pin)
					|| PincodeMasterStore.values().stream()
							.anyMatch(r -> r.pincode().equals(pin));
			MasterUiHelper.resetSearch(PincodeMasterPage.SEARCH_INPUT, waitSeconds);
			if (!exists) {
				StepLog.info("Pincode Master | Positive uniqueness pre-check",
						"Search generated pincode before Create",
						"Generated pincode does not already exist",
						"exists=false; candidate='" + pin + "'", "candidate=" + pin);
				return pin;
			}
			MasterUiHelper.sleep(20);
		}
		throw new AssertionError("Could not generate an unused six-digit TEST pincode");
	}

	/**
	 * The Create Pincode panel is permanently rendered beside the grid, so there is
	 * nothing to wait for after submitting — only let the grid settle.
	 */
	private void waitForCreatePanelToClose() {
		MasterUiHelper.sleep(800);
	}

	private void closeCreatePanelIfVisible() {
		if (MasterUiHelper.isVisible(PincodeMasterPage.CLEAR_BUTTON, 1)) {
			MasterUiHelper.clickJs(PincodeMasterPage.CLEAR_BUTTON, waitSeconds);
			MasterUiHelper.sleep(400);
		}
	}

	private static boolean isDuplicateMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("already") || value.contains("exist") || value.contains("duplicate");
	}

	private boolean isChecked(By locator) {
		try {
			WebElement cb = getDriver().findElement(locator);
			return cb.isSelected() || "true".equalsIgnoreCase(cb.getAttribute("checked"))
					|| "true".equalsIgnoreCase(cb.getAttribute("aria-checked"));
		} catch (Exception e) {
			return false;
		}
	}

	private static String firstNonBlankLine(String text) {
		for (String line : (text == null ? "" : text).split("\\R")) {
			if (!line.isBlank()) {
				return line.trim();
			}
		}
		return "";
	}
}
