/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.masters.district;

import Com.Ckyc_4_0.functionality.masters.state.StateMasterFunctionality;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.DistrictMasterStore;
import Com.Ckyc_4_0.UtilityFiles.DistrictMasterStore.DistrictRecord;
import Com.Ckyc_4_0.UtilityFiles.MasterTestContext;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.pages.masters.district.DistrictMasterPage;
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
 * District Master — CRUD only on a newly created TEST district linked to the test-safe State.
 */
public class DistrictMasterFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(DistrictMasterFunctionality.class);
	private final int waitSeconds = ConfigReader.getExplicitWait();
	private final Random random = new Random();
	private final StateMasterFunctionality stateMaster = new StateMasterFunctionality();

	public void openDistrictMasterFromMenu() {
		MasterUiHelper.openMastersSubmodule("District Master", DistrictMasterPage.PAGE_HEADING, waitSeconds);
		StepLog.info("District Master | Navigate", "Open Masters → District Master", "District Master visible",
				"Heading District Master", "menu=District Master");
		Hooks.captureMilestone("District Master page opened");
	}

	public void verifyDistrictMasterPageLoaded() {
		PomElementManager.findVisible(DistrictMasterPage.PAGE_HEADING, waitSeconds);
		PomElementManager.findVisible(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
		int total = MasterUiHelper.readChip(DistrictMasterPage.TOTAL_CHIP, waitSeconds);
		StepLog.info("District Master | Page load", "Verify District Master loaded",
				"Heading + search + chips visible", "Loaded | Total Districts=" + total, "total=" + total);
	}

	public void verifyBadges() {
		int total = MasterUiHelper.readChip(DistrictMasterPage.TOTAL_CHIP, waitSeconds);
		int active = MasterUiHelper.readChip(DistrictMasterPage.ACTIVE_CHIP, waitSeconds);
		int inactive = MasterUiHelper.readChip(DistrictMasterPage.INACTIVE_CHIP, waitSeconds);
		boolean pass = total >= 0 && active >= 0 && inactive >= 0 && total == active + inactive;
		StepLog.check("District Master | Badges", "Total Districts equals Active + Inactive",
				"Total = Active + Inactive",
				"Total=" + total + " Active=" + active + " Inactive=" + inactive, pass,
				"Total=" + total + "; Active=" + active + "; Inactive=" + inactive);
	}

	public void ensureTestSafeState() {
		if (MasterTestContext.createdStateCode() != null && MasterTestContext.createdStateName() != null) {
			StepLog.info("District Master | Test-safe State", "Reuse created TEST state",
					"State available for district create",
					"code=" + MasterTestContext.createdStateCode() + " name=" + MasterTestContext.createdStateName(),
					"reuse=true");
			return;
		}
		stateMaster.openStateMasterFromMenu();
		stateMaster.loadAllStatesIntoMap();
		stateMaster.createUniqueTestState();
		openDistrictMasterFromMenu();
	}

	public void loadDistrictsIntoMap() {
		DistrictMasterStore.clear();
		MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
		// UI smoke coverage: one XPath reads every row currently rendered on this page.
		// Do not change rows-per-page or paginate the complete production-like master.
		readVisibleDistrictRows();
		MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
		boolean pass = DistrictMasterStore.size() > 0;
		StepLog.check("District Master | Load map | count=" + DistrictMasterStore.size(),
				"Store all currently visible district rows into map", "Visible map size > 0",
				"Stored " + DistrictMasterStore.size() + " visible districts", pass,
				"mapSize=" + DistrictMasterStore.size() + "; currentPageOnly=true");
	}

	/** Randomly verify the State filter using up to two States present on the current page. */
	public void verifyTwoRandomStateFilters() {
		List<String> states = DistrictMasterStore.values().stream()
				.map(DistrictRecord::state)
				.filter(s -> s != null && MasterFieldRules.isValidName(s))
				.distinct()
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		Collections.shuffle(states, random);
		int samples = Math.min(2, states.size());
		if (samples == 0) {
			throw new AssertionError("No State values available in visible District rows");
		}
		for (int i = 0; i < samples; i++) {
			String state = states.get(i);
			MasterUiHelper.selectFilterOption(DistrictMasterPage.STATE_FILTER, state, waitSeconds);
			List<WebElement> rows = getDriver().findElements(MasterUiHelper.GRID_DATA_ROWS);
			boolean correct = !rows.isEmpty() && rows.stream().allMatch(row -> {
				List<WebElement> cells = row.findElements(By.xpath(".//div[@role='gridcell']"));
				return cells.size() > 1 && cells.get(1).getText().toUpperCase()
						.contains(state.toUpperCase());
			});
			StepLog.check("District Master | State filter sample " + (i + 1),
					"Select random State filter", "Only districts for selected State are shown",
					"selectedState='" + state + "' visibleRows=" + rows.size() + " correct=" + correct,
					correct, "sample=" + (i + 1) + "/" + samples + "; state=" + state);
		}
		MasterUiHelper.selectFilterOption(DistrictMasterPage.STATE_FILTER, "All States", waitSeconds);
	}

	private void readVisibleDistrictRows() {
		for (WebElement row : getDriver().findElements(MasterUiHelper.GRID_DATA_ROWS)) {
			try {
				List<WebElement> cells = row.findElements(By.xpath(".//div[@role='gridcell']"));
				if (cells.size() < 3) {
					continue;
				}
				String sno = cells.get(0).getText().trim();
				String state = lastNonBlankLine(cells.get(1).getText());
				String name = cells.get(2).getText().trim();
				boolean active = false;
				for (WebElement cb : row.findElements(By.cssSelector("input[type='checkbox']"))) {
					if (cb.isSelected() || "true".equalsIgnoreCase(cb.getAttribute("aria-checked"))) {
						active = true;
						break;
					}
				}
				if (!name.isBlank()) {
					DistrictMasterStore.put(new DistrictRecord(state, name, active, parseInt(sno)));
				}
			} catch (Exception e) {
				logger.debug("Skip district row: {}", e.getMessage());
			}
		}
	}

	public void randomSearchFromMapWithRetry() {
		List<DistrictRecord> pool = DistrictMasterStore.values().stream()
				.filter(r -> MasterFieldRules.isValidName(r.state())
						&& MasterFieldRules.isValidName(r.name()))
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		if (pool.isEmpty()) {
			loadDistrictsIntoMap();
			pool = DistrictMasterStore.values().stream()
					.filter(r -> MasterFieldRules.isValidName(r.state())
							&& MasterFieldRules.isValidName(r.name()))
					.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		}
		Collections.shuffle(pool, random);
		int maxAttempts = Math.min(2, pool.size());
		boolean passed = maxAttempts > 0;
		String last = "";
		for (int i = 0; i < maxAttempts; i++) {
			DistrictRecord pick = pool.get(i);
			StepLog.info("District Master | Random search input " + (i + 1),
					"Enter randomly selected District name", "Search receives test data",
					"Typing query='" + pick.name() + "'",
					"sample=" + (i + 1) + "/" + maxAttempts + "; query=" + pick.name()
							+ "; state=" + pick.state());
			MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, pick.name(), waitSeconds);
			boolean found = MasterUiHelper.rowContains(pick.name());
			last = "Attempt " + (i + 1) + " query='" + pick.name() + "' found=" + found;
			StepLog.check("District Master | Random search | " + pick.name(),
					"Search randomly selected district from map", "Grid shows expected district", last, found,
					"query=" + pick.name() + "; state=" + pick.state());
			passed = passed && found;
			MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
		}
		if (!passed) {
			SoftAssertManager.recordFailure("District random search", new AssertionError(last));
		}
	}

	public void runFieldValidations() {
		openCreatePanel();
		prepareDistrictNegativeTrial("Double space");
		MasterUiHelper.typeInto(DistrictMasterPage.DISTRICT_NAME_INPUT, "TAMIL  NADU", waitSeconds);
		String dbl = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		boolean dblBlocked = !MasterFieldRules.hasContinuousDoubleSpace(dbl)
				|| !MasterUiHelper.isButtonEnabled(DistrictMasterPage.CREATE_BUTTON);
		StepLog.check("District Master | Double space", "District Name must not keep double spaces usable",
				"Sanitized or Create disabled",
				MasterFieldRules.describeLengthTrial("District Name", "TAMIL  NADU", dbl), dblBlocked,
				"entered='TAMIL  NADU'; accepted='" + dbl + "'");

		prepareDistrictNegativeTrial("Special character");
		MasterUiHelper.typeInto(DistrictMasterPage.DISTRICT_NAME_INPUT, "CHENNAI@", waitSeconds);
		String spec = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		boolean specialRejected = !spec.contains("@");
		StepLog.check("District Master | Special character", "Letters-only district name",
				"Special character must not remain in the field",
				MasterFieldRules.describeLengthTrial("District Name", "CHENNAI@", spec),
				specialRejected, "entered=CHENNAI@; accepted=" + spec);

		prepareDistrictNegativeTrial("Mixed numeric");
		MasterUiHelper.typeInto(DistrictMasterPage.DISTRICT_NAME_INPUT, "CHENNAI1", waitSeconds);
		String mixedNumeric = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		boolean mixedNumericRejected = mixedNumeric.chars().noneMatch(Character::isDigit);
		StepLog.check("District Master | Mixed numeric", "District Name must contain letters only",
				"Digits must not remain in the field",
				MasterFieldRules.describeLengthTrial("District Name", "CHENNAI1", mixedNumeric),
				mixedNumericRejected, "entered=CHENNAI1; accepted=" + mixedNumeric);

		prepareDistrictNegativeTrial("Numeric-only");
		MasterUiHelper.typeInto(DistrictMasterPage.DISTRICT_NAME_INPUT, "123456", waitSeconds);
		String numericOnly = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		boolean numericOnlyRejected = numericOnly.chars().noneMatch(Character::isDigit);
		StepLog.check("District Master | Numeric-only", "Numeric-only District Name is invalid",
				"No numeric character may remain in a letters-only field",
				MasterFieldRules.describeLengthTrial("District Name", "123456", numericOnly),
				numericOnlyRejected, "entered=123456; accepted=" + numericOnly);

		String over = MasterFieldRules.overLengthNameSample();
		prepareDistrictNegativeTrial("Name max 50");
		MasterUiHelper.typeInto(DistrictMasterPage.DISTRICT_NAME_INPUT, over, waitSeconds);
		String acceptedOver = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		boolean lenOk = acceptedOver.length() <= MasterFieldRules.NAME_MAX_LENGTH;
		StepLog.check("District Master | Name max 50", "Name longer than 50 must trim/block (html max may be 100)",
				"Accepted length <= 50",
				MasterFieldRules.describeLengthTrial("District Name", over, acceptedOver), lenOk,
				"enteredLen=" + over.length() + "; acceptedLen=" + acceptedOver.length());

		try {
			MasterUiHelper.clickJs(DistrictMasterPage.CLEAR_BUTTON, waitSeconds);
		} catch (Exception e) {
			StepLog.check("District Master | Clear", "Clear form", "Fields cleared", e.getMessage(), false,
					"action=Clear");
		}
	}

	private void prepareDistrictNegativeTrial(String trial) {
		MasterUiHelper.clickJs(DistrictMasterPage.CLEAR_BUTTON, waitSeconds);
		String stateName = MasterTestContext.createdStateName();
		String stateCode = MasterTestContext.createdStateCode();
		List<String> candidates = testStateCandidates(stateName, stateCode);
		String testData = "trial=" + trial + "; stateName=" + stateName
				+ "; stateCode=" + stateCode + "; candidates=" + candidates;
		try {
			MasterUiHelper.selectMuiOption(DistrictMasterPage.STATE_COMBO, candidates, waitSeconds);
			String selected = getDriver().findElement(DistrictMasterPage.STATE_COMBO).getText().trim();
			boolean confirmed = candidates.stream()
					.filter(v -> v != null && !v.isBlank())
					.anyMatch(v -> selected.equalsIgnoreCase(v) || selected.contains(v));
			StepLog.info("District Master | Negative trial prerequisite",
					"Reselect State before entering each invalid District Name",
					"State dropdown selection confirmed",
					"trial='" + trial + "' selectedState='" + selected + "' confirmed=" + confirmed,
					testData + "; selectedState=" + selected);
			if (!confirmed) {
				throw new IllegalStateException("State dropdown did not retain a candidate; selected='" + selected + "'");
			}
		} catch (Exception e) {
			throw new PopupHandler.ModuleBlockerException("District Master",
					"Cannot execute negative District trial because State selection was reset or not retained: "
							+ trial,
					testData + "; dropdownFailure=" + e.getMessage());
		}
	}

	/** Banking negative: an existing State+District combination must be rejected. */
	public void verifyExistingDistrictDuplicateRejected() {
		if (DistrictMasterStore.size() == 0) {
			loadDistrictsIntoMap();
		}
		DistrictRecord existing = DistrictMasterStore.values().stream()
				.filter(r -> r.state() != null && !r.state().isBlank()
						&& MasterFieldRules.isValidName(r.name()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No valid existing District available for duplicate test"));
		openCreatePanel();
		MasterUiHelper.selectAutocomplete(DistrictMasterPage.STATE_COMBO, existing.state(), waitSeconds);
		MasterUiHelper.typeSlowly(DistrictMasterPage.DISTRICT_NAME_INPUT, existing.name(), waitSeconds);
		boolean enabled = MasterUiHelper.isButtonEnabled(DistrictMasterPage.CREATE_BUTTON);
		String toast = "";
		if (enabled) {
			MasterUiHelper.clickJs(DistrictMasterPage.CREATE_BUTTON, waitSeconds);
			toast = ToastHandler.waitAndRead(getDriver(), 3000);
		}
		boolean rejected = enabled && isDuplicateMessage(toast);
		StepLog.check("District Master | Duplicate existing District",
				"Submit existing State+District combination",
				"Creation rejected with already-exists/duplicate toaster",
				"createEnabled=" + enabled + " toast='" + toast + "'", rejected,
				"state=" + existing.state() + "; district=" + existing.name());
		ToastHandler.dismissIfPresent(getDriver());
		if (MasterUiHelper.isVisible(DistrictMasterPage.CLEAR_BUTTON, 1)) {
			MasterUiHelper.clickJs(DistrictMasterPage.CLEAR_BUTTON, waitSeconds);
		}
	}

	public void createUniqueTestDistrict() {
		ensureTestSafeState();
		if (!MasterUiHelper.isVisible(DistrictMasterPage.PAGE_HEADING, 2)) {
			openDistrictMasterFromMenu();
		}
		String stateName = MasterTestContext.createdStateName();
		String stateCode = MasterTestContext.createdStateCode();
		StringBuilder attempts = new StringBuilder();
		for (int attempt = 1; attempt <= 3; attempt++) {
			String districtName = nextUnusedDistrictName();
			openCreatePanel();
			List<String> stateCandidates = testStateCandidates(stateName, stateCode);
			String selectionData = "attempt=" + attempt + "/3; selectedStateName=" + stateName
					+ "; selectedStateCode=" + stateCode + "; candidates=" + stateCandidates
					+ "; districtEntered=" + districtName;
			try {
				MasterUiHelper.selectMuiOption(DistrictMasterPage.STATE_COMBO, stateCandidates, waitSeconds);
				String selectedState = getDriver().findElement(DistrictMasterPage.STATE_COMBO).getText().trim();
				boolean confirmed = stateCandidates.stream()
						.filter(v -> v != null && !v.isBlank())
						.anyMatch(v -> selectedState.equalsIgnoreCase(v) || selectedState.contains(v));
				if (!confirmed) {
					throw new IllegalStateException("State dropdown selection not retained; selected='"
							+ selectedState + "'");
				}
			} catch (Exception e) {
				String failedData = selectionData + "; dropdownFailure=" + e.getMessage();
				appendAttempt(attempts, failedData);
				StepLog.info("District Master | State dropdown candidate failed",
						"Select the successful State created earlier in this run",
						"Exact failed State candidate is recorded before retry",
						"Selection failed; retrying another District candidate | " + e.getMessage(),
						failedData);
				closeCreatePanelIfVisible();
				continue;
			}
			MasterUiHelper.typeSlowly(DistrictMasterPage.DISTRICT_NAME_INPUT, districtName, waitSeconds);
			String accepted = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
			boolean enabled = MasterUiHelper.isButtonEnabled(DistrictMasterPage.CREATE_BUTTON);
			String testData = "attempt=" + attempt + "/3; state=" + stateName + " (" + stateCode
					+ "); nameEntered=" + districtName + "; nameAccepted=" + accepted
					+ "; createEnabled=" + enabled;
			appendAttempt(attempts, testData);
			if (!enabled) {
				StepLog.info("District Master | Positive candidate disabled",
						"Create dedicated TEST district under last-created State",
						"Retry another valid District candidate",
						"Create disabled; retrying", testData);
				closeCreatePanelIfVisible();
				continue;
			}
			MasterUiHelper.clickJs(DistrictMasterPage.CREATE_BUTTON, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2500);
			if (isDuplicateMessage(toast)) {
				StepLog.info("District Master | Positive candidate collision",
						"Backend rejected generated name as duplicate",
						"Search it and retry with another random name",
						"toast='" + toast + "'; retrying", testData);
				ToastHandler.dismissIfPresent(getDriver());
				closeCreatePanelIfVisible();
				MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, accepted, waitSeconds);
				MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
				continue;
			}
			boolean created = toast.toLowerCase().contains("created successfully")
					|| toast.toLowerCase().contains("success");
			waitForCreatePanelToClose();
			MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, accepted, waitSeconds);
			boolean found = MasterUiHelper.rowContains(accepted);
			if (created && found) {
				MasterTestContext.setCreatedDistrict(accepted, stateCode);
				DistrictMasterStore.put(new DistrictRecord(stateName, accepted, true, 0));
				StepLog.check("District Master | Create unique TEST district",
						"Create TEST district linked to last-created TEST State",
						"Success toast and new District row both confirmed",
						"createdByToast=true; found=true; toast='" + toast + "'",
						true, testData + "; toast=" + toast);
				MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
				return;
			}
			StepLog.info("District Master | Positive candidate not persisted",
					"Create valid District and verify persistence",
					"Success toast and matching grid row",
					"createdByToast=" + created + "; found=" + found + "; toast='" + toast
							+ "'; retrying",
					testData + "; toast=" + toast);
			MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
		}
		throw new PopupHandler.ModuleBlockerException("District Master",
				"Valid District data was tried under the run's successful State, but no District was saved.",
				attempts.toString());
	}

	private static void appendAttempt(StringBuilder attempts, String detail) {
		if (attempts.length() > 0) {
			attempts.append(" || ");
		}
		attempts.append(detail);
	}

	public void editOnlyCreatedTestDistrict() {
		String name = MasterTestContext.createdDistrictName();
		if (name == null) {
			StepLog.info("District Master | Edit TEST district", "Skipped — no created district",
					"Edit created record only", "Skipped", "safety=existing untouched");
			return;
		}
		MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, name, waitSeconds);
		By edit = MasterUiHelper.editButton(name);
		if (getDriver().findElements(edit).isEmpty()) {
			StepLog.check("District Master | Edit TEST district", "Open Edit for created district",
					"Edit button visible", "Not found for " + name, false, "name=" + name);
			return;
		}
		MasterUiHelper.clickJs(edit, waitSeconds);
		String edited = name.endsWith(" EDIT") ? name : name + " EDIT";
		MasterUiHelper.typeInto(DistrictMasterPage.DISTRICT_NAME_INPUT, edited, waitSeconds);
		String accepted = MasterUiHelper.readValue(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		if (!getDriver().findElements(DistrictMasterPage.SAVE_BUTTON).isEmpty()) {
			MasterUiHelper.clickJs(DistrictMasterPage.SAVE_BUTTON, waitSeconds);
		} else {
			MasterUiHelper.clickSaveIfPresent(waitSeconds);
		}
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(700);
		MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, accepted, waitSeconds);
		boolean found = MasterUiHelper.rowContains(accepted) || MasterUiHelper.rowContains(edited);
		if (found) {
			MasterTestContext.setCreatedDistrict(accepted.isBlank() ? edited : accepted,
					MasterTestContext.createdDistrictStateCode());
		}
		StepLog.check("District Master | Edit TEST district", "Edit only the newly created district",
				"Grid shows updated TEST name", "found=" + found + " toast='" + toast + "'", found,
				"old=" + name + "; new=" + edited);
		MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void toggleOnlyCreatedTestDistrict() {
		String name = MasterTestContext.createdDistrictName();
		if (name == null) {
			StepLog.info("District Master | Toggle TEST district", "Skipped", "Toggle created only", "Skipped", "");
			return;
		}
		MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, name, waitSeconds);
		int activeBefore = MasterUiHelper.readActiveChip();
		boolean checkedBefore = MasterUiHelper.isToggleChecked(name);
		By toggle = MasterUiHelper.toggleFor(name);
		if (getDriver().findElements(toggle).isEmpty()) {
			StepLog.check("District Master | Toggle TEST district", "Toggle created district",
					"Toggle present", "Not found", false, "name=" + name);
			return;
		}
		MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(600);
		boolean checkedAfter = MasterUiHelper.isToggleChecked(name);
		int activeAfter = MasterUiHelper.readActiveChip();
		boolean changed = checkedBefore != checkedAfter || activeBefore != activeAfter;
		if (checkedAfter != checkedBefore) {
			MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
			ToastHandler.waitAndRead(getDriver(), 1500);
			MasterUiHelper.sleep(500);
		}
		boolean restored = MasterUiHelper.isToggleChecked(name);
		StepLog.check("District Master | Toggle TEST district", "Toggle Active on created district only",
				"Checkbox changes and is restored for dependent Pincode flow",
				"checked " + checkedBefore + "→" + checkedAfter + " Active " + activeBefore + "→" + activeAfter
						+ " restored=" + restored + " toast='" + toast + "'",
				changed && restored == checkedBefore, "name=" + name);
		MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void searchCreatedTestDistrict() {
		String name = MasterTestContext.createdDistrictName();
		if (name == null) {
			StepLog.info("District Master | Search TEST district", "Skipped", "Search created", "Skipped", "");
			return;
		}
		MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, name, waitSeconds);
		boolean found = MasterUiHelper.rowContains(name);
		StepLog.check("District Master | Search TEST district", "Search newly created district",
				"Grid shows created row", "found=" + found, found, "query=" + name);
		MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void cleanupCreatedTestDistrict() {
		String name = MasterTestContext.createdDistrictName();
		if (name == null) {
			StepLog.info("District Master | Cleanup", "Nothing to clean", "Delete or deactivate created only",
					"Skipped", "");
			return;
		}
		MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, name, waitSeconds);
		if (MasterUiHelper.deleteAvailable()) {
			MasterUiHelper.clickDeleteIfPresent(name, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2000);
			MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, name, waitSeconds);
			boolean gone = !MasterUiHelper.rowContains(name);
			StepLog.check("District Master | Cleanup", "Delete created TEST district", "Row removed",
					"deleted=" + gone + " toast='" + toast + "'", gone, "name=" + name);
		} else {
			if (MasterUiHelper.isToggleChecked(name)) {
				MasterUiHelper.clickHiddenInput(MasterUiHelper.toggleFor(name), waitSeconds);
				ToastHandler.waitAndRead(getDriver(), 1500);
			}
			StepLog.info("District Master | Cleanup", "Delete not in UI — deactivate created district",
					"Created district left inactive",
					"deleteAvailable=false | inactive=" + !MasterUiHelper.isToggleChecked(name) + " | name=" + name,
					"cleanup=deactivate");
		}
		MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
		MasterTestContext.setCreatedDistrict(null, null);
	}

	private void openCreatePanel() {
		if (MasterUiHelper.isVisible(DistrictMasterPage.CREATE_HEADING, 1)
				&& MasterUiHelper.isVisible(DistrictMasterPage.DISTRICT_NAME_INPUT, 1)
				&& MasterUiHelper.isVisible(DistrictMasterPage.CLEAR_BUTTON, 1)) {
			return;
		}
		MasterUiHelper.clickJs(DistrictMasterPage.NEW_DISTRICT_BUTTON, waitSeconds);
		PomElementManager.findVisible(DistrictMasterPage.CREATE_HEADING, waitSeconds);
		PomElementManager.findVisible(DistrictMasterPage.DISTRICT_NAME_INPUT, waitSeconds);
		PomElementManager.findClickable(DistrictMasterPage.CLEAR_BUTTON, waitSeconds);
	}

	private String nextUnusedDistrictName() {
		for (int i = 0; i < 20; i++) {
			String candidate = MasterTestContext.uniqueTestDistrictName();
			MasterUiHelper.search(DistrictMasterPage.SEARCH_INPUT, candidate, waitSeconds);
			boolean exists = MasterUiHelper.rowContains(candidate)
					|| DistrictMasterStore.values().stream()
							.anyMatch(r -> r.name().equalsIgnoreCase(candidate));
			MasterUiHelper.resetSearch(DistrictMasterPage.SEARCH_INPUT, waitSeconds);
			if (!exists) {
				StepLog.info("District Master | Positive uniqueness pre-check",
						"Search generated District before Create",
						"Generated name does not already exist",
						"exists=false; candidate='" + candidate + "'", "candidate=" + candidate);
				return candidate;
			}
			MasterUiHelper.sleep(20);
		}
		throw new AssertionError("Could not generate an unused TEST District name");
	}

	/**
	 * Names the created TEST state may appear under: the possibly-renamed name, the
	 * original name before an " EDIT" suffix, and finally the code.
	 */
	public static List<String> testStateCandidates(String stateName, String stateCode) {
		List<String> candidates = new ArrayList<>();
		if (stateName != null && !stateName.isBlank()) {
			candidates.add(stateName);
			int suffix = stateName.lastIndexOf(" EDIT");
			if (suffix > 0) {
				candidates.add(stateName.substring(0, suffix).trim());
			}
		}
		if (stateCode != null && !stateCode.isBlank()) {
			candidates.add(stateCode);
		}
		return candidates;
	}

	/**
	 * The Create District panel is permanently rendered beside the grid, so there is
	 * nothing to wait for after submitting — only let the grid settle.
	 */
	private void waitForCreatePanelToClose() {
		MasterUiHelper.sleep(800);
	}

	private void closeCreatePanelIfVisible() {
		if (MasterUiHelper.isVisible(DistrictMasterPage.CLEAR_BUTTON, 1)) {
			MasterUiHelper.clickJs(DistrictMasterPage.CLEAR_BUTTON, waitSeconds);
			MasterUiHelper.sleep(400);
		}
	}

	private static boolean isDuplicateMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("already") || value.contains("exist") || value.contains("duplicate");
	}

	private int parseInt(String s) {
		try {
			return Integer.parseInt(s.replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return 0;
		}
	}

	private static String lastNonBlankLine(String text) {
		String[] lines = (text == null ? "" : text).split("\\R");
		for (int i = lines.length - 1; i >= 0; i--) {
			if (!lines[i].isBlank()) {
				return lines[i].trim();
			}
		}
		return "";
	}
}
