/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.masters.state;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.MasterTestContext;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.UtilityFiles.StateMasterStore;
import Com.Ckyc_4_0.UtilityFiles.StateMasterStore.StateRecord;
import Com.Ckyc_4_0.pages.masters.state.StateMasterPage;
import Com.Ckyc_4_0.utils.MasterFieldRules;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.PopupHandler;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * State Master flows — map grid, badges, field validations, and CRUD only on freshly created TEST states.
 * Existing master rows are never edited or deleted.
 */
public class StateMasterFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(StateMasterFunctionality.class);
	private final int waitSeconds = ConfigReader.getExplicitWait();
	private final Random random = new Random();

	public void openStateMasterFromMenu() {
		MasterUiHelper.openMastersSubmodule("State Master", StateMasterPage.PAGE_HEADING, waitSeconds);
		StepLog.info("State Master | Navigate", "Open Masters → State Master", "State Master page visible",
				"URL/page heading State Master", "menu=State Master");
		Hooks.captureMilestone("State Master page opened");
	}

	public void verifyStateMasterPageLoaded() {
		PomElementManager.findVisible(StateMasterPage.PAGE_HEADING, waitSeconds);
		PomElementManager.findVisible(StateMasterPage.SEARCH_INPUT, waitSeconds);
		int total = MasterUiHelper.readChip(StateMasterPage.TOTAL_STATES_CHIP, waitSeconds);
		StepLog.info("State Master | Page load", "Verify State Master loaded",
				"Heading + search + summary chips visible", "Loaded | Total States=" + total, "total=" + total);
	}

	public void verifyBadges() {
		int total = MasterUiHelper.readChip(StateMasterPage.TOTAL_STATES_CHIP, waitSeconds);
		int active = MasterUiHelper.readChip(StateMasterPage.ACTIVE_CHIP, waitSeconds);
		int inactive = MasterUiHelper.readChip(StateMasterPage.INACTIVE_CHIP, waitSeconds);
		boolean pass = total >= 0 && active >= 0 && inactive >= 0 && total == active + inactive;
		StepLog.check("State Master | Badges", "Total States equals Active + Inactive",
				"Total = Active + Inactive",
				"Total=" + total + " Active=" + active + " Inactive=" + inactive, pass,
				"Total=" + total + "; Active=" + active + "; Inactive=" + inactive);
	}

	/** Reads all pages into StateMasterStore map (code → name/active). */
	public void loadAllStatesIntoMap() {
		StateMasterStore.clear();
		resetSearch();
		int guard = 0;
		while (guard++ < 50) {
			List<WebElement> rows = getDriver().findElements(StateMasterPage.GRID_DATA_ROWS);
			int idx = 1;
			for (WebElement row : rows) {
				try {
					List<WebElement> cells = row.findElements(By.xpath(".//div[@role='gridcell']"));
					if (cells.size() < 4) {
						continue;
					}
					String sno = cells.get(0).getText().trim();
					String code = cells.get(1).getText().trim();
					String name = cells.get(2).getText().trim();
					boolean active = !row.findElements(By.cssSelector("input[type='checkbox'][checked], input[aria-checked='true']"))
							.isEmpty()
							|| row.findElements(By.xpath(".//input[@type='checkbox' and @checked]")).size() > 0
							|| isCheckboxChecked(row);
					if (code.isBlank()) {
						continue;
					}
					int serial = parseIntSafe(sno, idx);
					StateMasterStore.put(new StateRecord(code, name, active, serial));
					idx++;
				} catch (Exception e) {
					logger.debug("Skip row: {}", e.getMessage());
				}
			}
			if (!goNextPageIfEnabled()) {
				break;
			}
		}
		resetSearch();
		String scenario = "State Master | Load map | count=" + StateMasterStore.size();
		Hooks.logStepWithScenario(scenario,
				"Store all state rows into Map<code,record>",
				"Map size > 0",
				"Stored " + StateMasterStore.size() + " states | sampleKeys="
						+ StateMasterStore.asMap().keySet().stream().limit(8).toList(),
				StateMasterStore.size() > 0);
		List<StateRecord> invalidStates = StateMasterStore.asMap().values().stream()
				.filter(r -> !MasterFieldRules.isValidStateCode(r.code())
						|| !MasterFieldRules.isValidName(r.name()))
				.toList();
		StepLog.check("State Master | Existing data quality",
				"Validate loaded State codes and names against banking datatype rules",
				"Every code is exactly 2 capital letters and every name is letters with single spaces",
				"invalidCount=" + invalidStates.size() + "; invalidRows="
						+ invalidStates.stream().limit(10).toList(),
				invalidStates.isEmpty(),
				"invalid rows are logged as application data bugs and excluded from random selection");
		ExtentReportManager.logInfo("StateMasterStore size=" + StateMasterStore.size());
		if (StateMasterStore.size() == 0) {
			throw new AssertionError("State Master map is empty — no rows read");
		}
	}

	/**
	 * Pick random states from map, search; if not found try another (up to 5 attempts).
	 */
	public void randomSearchFromMapWithRetry() {
		List<StateRecord> pool = StateMasterStore.asMap().values().stream()
				.filter(r -> MasterFieldRules.isValidStateCode(r.code())
						&& MasterFieldRules.isValidName(r.name()))
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		if (pool.isEmpty()) {
			loadAllStatesIntoMap();
			pool = StateMasterStore.asMap().values().stream()
					.filter(r -> MasterFieldRules.isValidStateCode(r.code())
							&& MasterFieldRules.isValidName(r.name()))
					.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		}
		Collections.shuffle(pool, random);
		int maxAttempts = Math.min(5, pool.size());
		boolean passed = false;
		String lastDetail = "";
		for (int i = 0; i < maxAttempts; i++) {
			StateRecord pick = pool.get(i);
			String query = random.nextBoolean() ? pick.code() : pick.name();
			String testData = "attempt=" + (i + 1) + "/" + maxAttempts
					+ "; query='" + query + "'; expectedCode='" + pick.code()
					+ "'; expectedName='" + pick.name() + "'";
			// Persist the random input before typing, even if the UI fails afterward.
			StepLog.info("State Master | Random search input | " + pick.code(),
					"Enter randomly selected state code/name in Search",
					"Search input receives selected test data",
					"Typing query='" + query + "'", testData);
			search(query);
			boolean found = !getDriver().findElements(StateMasterPage.rowContainingText(pick.code())).isEmpty()
					|| !getDriver().findElements(StateMasterPage.rowContainingText(pick.name())).isEmpty();
			lastDetail = "Attempt " + (i + 1) + "/" + maxAttempts
					+ " | TestData query='" + query + "'"
					+ " | Expected code=" + pick.code() + " name=" + pick.name()
					+ " | FoundInGrid=" + found;
			StepLog.check(
					"State Master | Random search | " + pick.code() + " " + pick.name(),
					"Search randomly selected state from map",
					"Grid shows expected code/name for query",
					lastDetail, found, testData);
			if (found) {
				passed = true;
				break;
			}
			logger.warn("Search miss — trying another state. {}", lastDetail);
		}
		resetSearch();
		if (!passed) {
			SoftAssertManager.recordFailure("Random search from State Master map",
					new AssertionError("All " + maxAttempts + " random search attempts failed. Last: " + lastDetail));
		}
	}

	public void openCreatePanelIfNeeded() {
		if (PopupHandler.isUnexpectedApplicationPage(getDriver())) {
			throw new IllegalStateException("Cannot open Create State panel: "
					+ PopupHandler.describeUnexpectedPage(getDriver()));
		}
		if (!MasterUiHelper.isVisible(StateMasterPage.PAGE_HEADING, 2)) {
			openStateMasterFromMenu();
		}
		if (MasterUiHelper.isVisible(StateMasterPage.CREATE_PANEL_TITLE, 1)
				&& MasterUiHelper.isVisible(StateMasterPage.STATE_CODE_INPUT, 1)
				&& MasterUiHelper.isVisible(StateMasterPage.CLEAR_BUTTON, 1)) {
			ExecutionLogger.info("State Master create panel already visible");
			return;
		}
		ExecutionLogger.info("State Master | click New State and wait for Create State drawer");
		WebElement newState = PomElementManager.findClickable(StateMasterPage.NEW_STATE_BUTTON, waitSeconds);
		((org.openqa.selenium.JavascriptExecutor) getDriver())
				.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", newState);
		PomElementManager.findVisible(StateMasterPage.CREATE_PANEL_TITLE, waitSeconds);
		PomElementManager.findVisible(StateMasterPage.STATE_CODE_INPUT, waitSeconds);
		PomElementManager.findClickable(StateMasterPage.CLEAR_BUTTON, waitSeconds);
		ExecutionLogger.pass("State Master | Create State drawer visible | State Code, State Name and Clear ready");
	}

	public void clearCreateFormAndVerifyEmpty() {
		openCreatePanelIfNeeded();
		PomElementManager.click(StateMasterPage.CLEAR_BUTTON, waitSeconds);
		String code = readInput(StateMasterPage.STATE_CODE_INPUT);
		String name = readInput(StateMasterPage.STATE_NAME_INPUT);
		boolean empty = (code == null || code.isBlank()) && (name == null || name.isBlank());
		Hooks.logStepWithScenario("State Master | Clear Create form",
				"Click Clear and verify fields empty",
				"State Code and State Name blank",
				"TestData action=Clear | code='" + code + "' name='" + name + "' | empty=" + empty, empty);
		if (!empty) {
			SoftAssertManager.recordFailure("Clear Create form",
					new AssertionError("Fields not cleared: code=" + code + " name=" + name));
		}
	}

	/** Max length, datatype, double-space, specials — banking rules. */
	public void runCreateFieldValidations() {
		openCreatePanelIfNeeded();
		clearCreateFormAndVerifyEmpty();

		// State Code: exactly 2 capitals
		assertFieldTrial("State Code", StateMasterPage.STATE_CODE_INPUT, "T", false,
				"length 1 (below 2) must not be valid code");
		assertFieldTrial("State Code", StateMasterPage.STATE_CODE_INPUT, "T1", false,
				"digit not allowed in 2-letter code");
		assertFieldTrial("State Code", StateMasterPage.STATE_CODE_INPUT, "tn", false,
				"lowercase must not pass as 2 capital letters");
		assertFieldTrial("State Code", StateMasterPage.STATE_CODE_INPUT, "TNN", false,
				"length 3 over max 2");
		assertFieldTrial("State Code", StateMasterPage.STATE_CODE_INPUT, "T@", false,
				"special character must be rejected");
		String okCode = "ZZ";
		typeInto(StateMasterPage.STATE_CODE_INPUT, okCode);
		String acceptedCode = readInput(StateMasterPage.STATE_CODE_INPUT);
		boolean codeOk = MasterFieldRules.isValidStateCode(acceptedCode);
		Hooks.logStepWithScenario("State Master | State Code valid trial",
				"Enter valid 2 capital letters",
				"Accepted value matches 2 A-Z",
				MasterFieldRules.describeLengthTrial("State Code", okCode, acceptedCode)
						+ " | ruleValid=" + codeOk,
				codeOk);

		// State Name: letters + single spaces, max 50, no double space
		assertNameTrial("AB", true);
		assertNameTrial("TAMIL  NADU", false); // double space
		assertNameTrial("TAMIL@NADU", false);
		assertNameTrial("STATE123", false);
		assertNameTrial("123456", false); // numeric-only State Name is an application bug
		String over = MasterFieldRules.overLengthNameSample();
		typeInto(StateMasterPage.STATE_NAME_INPUT, over);
		String acceptedOver = readInput(StateMasterPage.STATE_NAME_INPUT);
		int acceptedLen = acceptedOver == null ? 0 : acceptedOver.length();
		boolean blockedOrTrimmed = acceptedLen <= MasterFieldRules.NAME_MAX_LENGTH;
		Hooks.logStepWithScenario("State Master | State Name max length " + MasterFieldRules.NAME_MAX_LENGTH,
				"Enter name longer than max 50",
				"UI rejects or trims to max " + MasterFieldRules.NAME_MAX_LENGTH,
				MasterFieldRules.describeLengthTrial("State Name", over, acceptedOver)
						+ " | maxAllowed=" + MasterFieldRules.NAME_MAX_LENGTH
						+ " | PassIfTrimOrBlock=" + blockedOrTrimmed,
				blockedOrTrimmed);
		if (!blockedOrTrimmed) {
			SoftAssertManager.recordFailure("State Name max length",
					new AssertionError("Accepted length " + acceptedLen + " > max "
							+ MasterFieldRules.NAME_MAX_LENGTH));
		}

		PomElementManager.click(StateMasterPage.CLEAR_BUTTON, waitSeconds);
	}

	/** Banking negative: an existing State must be rejected with a duplicate toaster. */
	public void verifyExistingStateDuplicateRejected() {
		if (StateMasterStore.size() == 0) {
			loadAllStatesIntoMap();
		}
		StateRecord existing = StateMasterStore.asMap().values().stream()
				.filter(r -> MasterFieldRules.isValidStateCode(r.code())
						&& MasterFieldRules.isValidName(r.name()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No valid existing State available for duplicate test"));
		openCreatePanelIfNeeded();
		MasterUiHelper.clickJs(StateMasterPage.CLEAR_BUTTON, waitSeconds);
		MasterUiHelper.typeSlowly(StateMasterPage.STATE_CODE_INPUT, existing.code(), waitSeconds);
		MasterUiHelper.typeSlowly(StateMasterPage.STATE_NAME_INPUT, existing.name(), waitSeconds);
		boolean enabled = MasterUiHelper.isButtonEnabled(StateMasterPage.CREATE_STATE_BUTTON);
		String toast = "";
		if (enabled) {
			MasterUiHelper.clickJs(StateMasterPage.CREATE_STATE_BUTTON, waitSeconds);
			toast = ToastHandler.waitAndRead(getDriver(), 3000);
		}
		boolean rejected = enabled && isDuplicateMessage(toast);
		StepLog.check("State Master | Duplicate existing State",
				"Submit an existing State code/name",
				"Creation rejected with already-exists/duplicate toaster",
				"createEnabled=" + enabled + " toast='" + toast + "'", rejected,
				"existingCode=" + existing.code() + "; existingName=" + existing.name());
		ToastHandler.dismissIfPresent(getDriver());
		if (MasterUiHelper.isVisible(StateMasterPage.CLEAR_BUTTON, 1)) {
			MasterUiHelper.clickJs(StateMasterPage.CLEAR_BUTTON, waitSeconds);
		}
	}

	/**
	 * Creates one fresh TEST state (unused 2-letter code + TEST AUTO name). Never touches existing rows.
	 */
	public void createUniqueTestState() {
		if (MasterTestContext.createdStateCode() != null && MasterTestContext.createdStateName() != null) {
			StepLog.info("State Master | Create unique TEST state", "Reuse already created TEST state this run",
					"TEST state available",
					"code=" + MasterTestContext.createdStateCode() + " name=" + MasterTestContext.createdStateName(),
					"reuse=true");
			return;
		}
		if (StateMasterStore.size() == 0) {
			loadAllStatesIntoMap();
		}
		Set<String> attemptedCodes = new HashSet<>();
		StringBuilder attempts = new StringBuilder();
		for (int attempt = 1; attempt <= 3; attempt++) {
			openCreatePanelIfNeeded();
			MasterUiHelper.clickJs(StateMasterPage.CLEAR_BUTTON, waitSeconds);
			String code = pickUnusedStateCode(attemptedCodes);
			attemptedCodes.add(code);
			String name = MasterTestContext.uniqueTestStateName();
			if (!MasterUiHelper.setReactInputValue(StateMasterPage.STATE_CODE_INPUT, code, waitSeconds)) {
				MasterUiHelper.typeSlowly(StateMasterPage.STATE_CODE_INPUT, code, waitSeconds);
			}
			MasterUiHelper.typeInto(StateMasterPage.STATE_NAME_INPUT, name, waitSeconds);
			String acceptedCode = MasterUiHelper.readValue(StateMasterPage.STATE_CODE_INPUT, waitSeconds);
			String acceptedName = MasterUiHelper.readValue(StateMasterPage.STATE_NAME_INPUT, waitSeconds);
			boolean enabled = MasterUiHelper.isButtonEnabled(StateMasterPage.CREATE_STATE_BUTTON);
			String testData = "attempt=" + attempt + "/3; codeEntered=" + code
					+ "; codeAccepted=" + acceptedCode + "; nameEntered=" + name
					+ "; nameAccepted=" + acceptedName + "; createEnabled=" + enabled;
			if (attempts.length() > 0) {
				attempts.append(" || ");
			}
			attempts.append(testData);

			if (!enabled || !MasterFieldRules.isValidStateCode(acceptedCode)
					|| !MasterFieldRules.isValidName(acceptedName)) {
				StepLog.info("State Master | Positive candidate rejected before submit",
						"Try another valid positive State candidate",
						"At least one valid candidate must become creatable",
						"Candidate not usable; retrying", testData);
				continue;
			}

			MasterUiHelper.clickJs(StateMasterPage.CREATE_STATE_BUTTON, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2500);
			if (isDuplicateMessage(toast)) {
				StepLog.info("State Master | Positive candidate collision",
						"Backend rejected generated State as duplicate",
						"Duplicate is valid negative behavior; retry another positive candidate",
						"toast='" + toast + "'; retrying", testData + "; toast=" + toast);
				ToastHandler.dismissIfPresent(getDriver());
				continue;
			}

			boolean created = toast.toLowerCase().contains("created successfully")
					|| toast.toLowerCase().contains("success");
			waitForCreatePanelToClose();
			MasterUiHelper.clickJs(StateMasterPage.REFRESH_PAGE, waitSeconds);
			PomElementManager.findVisible(StateMasterPage.SEARCH_INPUT, waitSeconds);
			MasterUiHelper.sleep(700);
			MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, acceptedCode, waitSeconds);
			boolean found = MasterUiHelper.rowContains(acceptedCode)
					&& MasterUiHelper.rowContains(acceptedName);
			MasterUiHelper.resetSearch(StateMasterPage.SEARCH_INPUT, waitSeconds);

			if (created && found) {
				MasterTestContext.setCreatedState(acceptedCode, acceptedName);
				StateMasterStore.put(new StateRecord(acceptedCode, acceptedName, true, 0));
				StepLog.check("State Master | Create unique TEST state",
						"Create one fresh TEST state after negative validations",
						"Success toast and new State row both confirmed",
						"createdByToast=true; found=true; toast='" + toast + "'",
						true, testData + "; toast=" + toast);
				return;
			}
			StepLog.info("State Master | Positive candidate not persisted",
					"Create valid State and verify persistence",
					"Success toast and matching grid row",
					"createdByToast=" + created + "; found=" + found + "; toast='" + toast
							+ "'; retrying another candidate",
					testData + "; toast=" + toast);
		}
		throw new PopupHandler.ModuleBlockerException("State Master",
				"Valid State data was tried but no State was successfully saved and verified.",
				attempts.toString());
	}

	/** Optional second TEST state — create only; existing masters still untouched. */
	public void createSecondUniqueTestStateIfNeeded() {
		String firstCode = MasterTestContext.createdStateCode();
		String firstName = MasterTestContext.createdStateName();
		if (firstCode == null) {
			createUniqueTestState();
			return;
		}
		openCreatePanelIfNeeded();
		String code = pickUnusedStateCode();
		String name = MasterTestContext.uniqueTestStateName();
		if (!MasterUiHelper.setReactInputValue(StateMasterPage.STATE_CODE_INPUT, code, waitSeconds)) {
			MasterUiHelper.typeSlowly(StateMasterPage.STATE_CODE_INPUT, code, waitSeconds);
		}
		MasterUiHelper.typeInto(StateMasterPage.STATE_NAME_INPUT, name, waitSeconds);
		String acceptedCode = MasterUiHelper.readValue(StateMasterPage.STATE_CODE_INPUT, waitSeconds);
		String acceptedName = MasterUiHelper.readValue(StateMasterPage.STATE_NAME_INPUT, waitSeconds);
		boolean enabled = MasterUiHelper.isButtonEnabled(StateMasterPage.CREATE_STATE_BUTTON);
		String testData = "secondCode=" + acceptedCode + "; secondName=" + acceptedName
				+ "; primaryKept=" + firstCode + "/" + firstName;
		if (!enabled) {
			StepLog.info("State Master | Create second TEST state", "Second create skipped (Create disabled)",
					"Optional second TEST row", "skipped | " + testData, testData);
			return;
		}
		MasterUiHelper.clickJs(StateMasterPage.CREATE_STATE_BUTTON, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, acceptedCode, waitSeconds);
		boolean found = MasterUiHelper.rowContains(acceptedCode);
		StepLog.check("State Master | Create second TEST state", "Create optional second TEST state",
				"Second TEST row appears (primary still used for edit/toggle)",
				"found=" + found + " toast='" + toast + "' | " + testData, found, testData);
		MasterTestContext.setCreatedState(firstCode, firstName);
		MasterUiHelper.resetSearch(StateMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void editOnlyCreatedTestState() {
		String code = MasterTestContext.createdStateCode();
		String name = MasterTestContext.createdStateName();
		if (code == null || name == null) {
			StepLog.info("State Master | Edit TEST state", "Skipped — no created TEST state",
					"Edit created record only", "Skipped", "safety=existing untouched");
			return;
		}
		MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, code, waitSeconds);
		By edit = MasterUiHelper.editButton(name);
		if (getDriver().findElements(edit).isEmpty()) {
			edit = MasterUiHelper.editButton(code);
		}
		if (getDriver().findElements(edit).isEmpty()) {
			StepLog.check("State Master | Edit TEST state", "Open Edit for created TEST state",
					"Edit button visible", "Not found for " + code + "/" + name, false,
					"code=" + code + "; name=" + name);
			return;
		}
		MasterUiHelper.clickJs(edit, waitSeconds);
		PomElementManager.findVisible(StateMasterPage.EDIT_PANEL_TITLE, waitSeconds);
		String edited = name.endsWith(" EDIT") ? name : name + " EDIT";
		if (edited.length() > MasterFieldRules.NAME_MAX_LENGTH) {
			edited = edited.substring(0, MasterFieldRules.NAME_MAX_LENGTH);
		}
		MasterUiHelper.typeInto(StateMasterPage.STATE_NAME_INPUT, edited, waitSeconds);
		String accepted = MasterUiHelper.readValue(StateMasterPage.STATE_NAME_INPUT, waitSeconds);
		if (!getDriver().findElements(StateMasterPage.SAVE_OR_UPDATE_BUTTON).isEmpty()) {
			MasterUiHelper.clickJs(StateMasterPage.SAVE_OR_UPDATE_BUTTON, waitSeconds);
		} else {
			MasterUiHelper.clickSaveIfPresent(waitSeconds);
		}
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(700);
		MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, code, waitSeconds);
		/*
		 * Only adopt the new name once the grid actually shows it. Matching on the code
		 * alone would store a rename that never persisted, and every later State
		 * dropdown lookup in District/Pincode would then search for a missing option.
		 */
		String expected = accepted.isBlank() ? edited : accepted;
		boolean renamed = MasterUiHelper.rowContains(expected);
		boolean found = renamed || MasterUiHelper.rowContains(code);
		if (renamed) {
			MasterTestContext.setCreatedState(code, expected);
		}
		StepLog.check("State Master | Edit TEST state", "Edit only the newly created TEST state",
				"Grid shows updated TEST name",
				"renamed=" + renamed + " found=" + found + " toast='" + toast
						+ "' nameInContext=" + MasterTestContext.createdStateName(),
				renamed, "code=" + code + "; old=" + name + "; new=" + edited);
		MasterUiHelper.resetSearch(StateMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void toggleOnlyCreatedTestState() {
		String code = MasterTestContext.createdStateCode();
		String name = MasterTestContext.createdStateName();
		if (code == null || name == null) {
			StepLog.info("State Master | Toggle TEST state", "Skipped", "Toggle created only", "Skipped", "");
			return;
		}
		MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, code, waitSeconds);
		int activeBefore = MasterUiHelper.readActiveChip();
		boolean checkedBefore = MasterUiHelper.isToggleChecked(name);
		if (!checkedBefore) {
			checkedBefore = MasterUiHelper.isToggleChecked(code);
		}
		By toggle = MasterUiHelper.toggleFor(name);
		if (getDriver().findElements(toggle).isEmpty()) {
			toggle = MasterUiHelper.toggleFor(code);
		}
		if (getDriver().findElements(toggle).isEmpty()) {
			StepLog.check("State Master | Toggle TEST state", "Toggle created TEST state",
					"Toggle present", "Not found", false, "code=" + code + "; name=" + name);
			return;
		}
		MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(600);
		boolean checkedAfter = MasterUiHelper.isToggleChecked(name);
		if (!checkedAfter) {
			checkedAfter = MasterUiHelper.isToggleChecked(code);
		}
		int activeAfter = MasterUiHelper.readActiveChip();
		boolean changed = checkedBefore != checkedAfter || activeBefore != activeAfter;
		if (checkedAfter != checkedBefore) {
			MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
			ToastHandler.waitAndRead(getDriver(), 1500);
			MasterUiHelper.sleep(500);
		}
		boolean restored = MasterUiHelper.isToggleChecked(name);
		if (!restored) {
			restored = MasterUiHelper.isToggleChecked(code);
		}
		StepLog.check("State Master | Toggle TEST state", "Toggle Active on created TEST state only",
				"Checkbox changes and is restored for dependent District/Pincode flow",
				"checked " + checkedBefore + "→" + checkedAfter + " Active " + activeBefore + "→" + activeAfter
						+ " restored=" + restored + " toast='" + toast + "'",
				changed && restored == checkedBefore, "code=" + code + "; name=" + name);
		MasterUiHelper.resetSearch(StateMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void searchCreatedTestState() {
		String code = MasterTestContext.createdStateCode();
		String name = MasterTestContext.createdStateName();
		if (code == null) {
			StepLog.info("State Master | Search TEST state", "Skipped", "Search created", "Skipped", "");
			return;
		}
		MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, code, waitSeconds);
		boolean found = MasterUiHelper.rowContains(code)
				|| (name != null && MasterUiHelper.rowContains(name));
		StepLog.check("State Master | Search TEST state", "Search newly created TEST state",
				"Grid shows created row", "found=" + found, found, "query=" + code + "; name=" + name);
		MasterUiHelper.resetSearch(StateMasterPage.SEARCH_INPUT, waitSeconds);
	}

	public void cleanupCreatedTestState() {
		String code = MasterTestContext.createdStateCode();
		String name = MasterTestContext.createdStateName();
		if (code == null) {
			StepLog.info("State Master | Cleanup", "Nothing to clean", "Delete or deactivate created only",
					"Skipped", "");
			return;
		}
		MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, code, waitSeconds);
		if (MasterUiHelper.deleteAvailable()) {
			MasterUiHelper.clickDeleteIfPresent(name != null ? name : code, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2000);
			MasterUiHelper.search(StateMasterPage.SEARCH_INPUT, code, waitSeconds);
			boolean gone = !MasterUiHelper.rowContains(code);
			StepLog.check("State Master | Cleanup", "Delete created TEST state", "Row removed",
					"deleted=" + gone + " toast='" + toast + "'", gone, "code=" + code + "; name=" + name);
		} else {
			By toggle = MasterUiHelper.toggleFor(name != null ? name : code);
			if (getDriver().findElements(toggle).isEmpty()) {
				toggle = MasterUiHelper.toggleFor(code);
			}
			String toggleKey = name != null ? name : code;
			if (!getDriver().findElements(toggle).isEmpty() && MasterUiHelper.isToggleChecked(toggleKey)) {
				MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
				ToastHandler.waitAndRead(getDriver(), 1500);
			}
			boolean inactive = !MasterUiHelper.isToggleChecked(toggleKey)
					&& !MasterUiHelper.isToggleChecked(code);
			StepLog.info("State Master | Cleanup", "Delete not in UI — deactivate created TEST state only",
					"Created state left inactive; existing masters untouched",
					"deleteAvailable=false | inactive=" + inactive + " | code=" + code + " | name=" + name,
					"cleanup=deactivate");
		}
		MasterUiHelper.resetSearch(StateMasterPage.SEARCH_INPUT, waitSeconds);
		// Free context so District/Pincode can create a fresh active TEST geography
		MasterTestContext.clearCreatedState();
	}

	private String pickUnusedStateCode() {
		return pickUnusedStateCode(Collections.emptySet());
	}

	private String pickUnusedStateCode(Set<String> excluded) {
		for (int i = 0; i < 40; i++) {
			String code = MasterTestContext.uniqueLetters(2);
			if (MasterFieldRules.isValidStateCode(code) && !StateMasterStore.asMap().containsKey(code)
					&& !excluded.contains(code)) {
				return code;
			}
			MasterUiHelper.sleep(15);
		}
		for (char a = 'Z'; a >= 'A'; a--) {
			for (char b = 'Z'; b >= 'A'; b--) {
				String candidate = "" + a + b;
				if (!StateMasterStore.asMap().containsKey(candidate) && !excluded.contains(candidate)) {
					return candidate;
				}
			}
		}
		return "QX";
	}

	private void assertNameTrial(String entered, boolean expectAcceptableByRule) {
		typeInto(StateMasterPage.STATE_NAME_INPUT, entered);
		String accepted = readInput(StateMasterPage.STATE_NAME_INPUT);
		boolean ruleOk = MasterFieldRules.isValidName(accepted) && !MasterFieldRules.hasContinuousDoubleSpace(accepted);
		// If rule says invalid, accepting same invalid string = potential bug when saved;
		// for input-level we check: double space / specials should not remain if UI sanitizes,
		// OR Create stays disabled.
		boolean createEnabled = isCreateEnabled();
		boolean pass;
		String verdict;
		if (!expectAcceptableByRule) {
			/*
			 * Invalid name data may remain visible only when Create is disabled. If the
			 * browser changes the value, the accepted result itself must be a valid
			 * letters-only name; removing '@' but retaining digits is still a bug.
			 */
			boolean sanitizedToValid = !entered.equals(accepted) && ruleOk;
			pass = !createEnabled || sanitizedToValid;
			verdict = pass ? "Invalid input blocked/sanitized" : "BUG: invalid input still usable";
		} else {
			pass = ruleOk;
			verdict = pass ? "Valid name accepted" : "Valid name rejected unexpectedly";
		}
		Hooks.logStepWithScenario("State Master | State Name trial | '" + entered + "'",
				"Datatype/space validation for State Name",
				expectAcceptableByRule ? "Valid letters+single spaces within max 50"
						: "Invalid pattern must not enable Create / must sanitize",
				MasterFieldRules.describeLengthTrial("State Name", entered, accepted)
						+ " | CreateEnabled=" + createEnabled
						+ " | Verdict=" + verdict,
				pass);
		if (!pass) {
			SoftAssertManager.recordFailure("State Name trial " + entered, new AssertionError(verdict));
		}
	}

	private void assertFieldTrial(String field, By locator, String entered, boolean expectRuleValid, String why) {
		typeInto(locator, entered);
		String accepted = readInput(locator);
		boolean ruleValid = "State Code".equals(field)
				? MasterFieldRules.isValidStateCode(accepted)
				: MasterFieldRules.isValidName(accepted);
		boolean createEnabled = isCreateEnabled();
		boolean pass = expectRuleValid ? ruleValid : (!ruleValid || !createEnabled || !entered.equals(accepted));
		Hooks.logStepWithScenario("State Master | " + field + " trial | '" + entered + "'",
				why,
				expectRuleValid ? "Value valid per banking rule" : "Invalid value must not pass as-is with Create enabled",
				MasterFieldRules.describeLengthTrial(field, entered, accepted)
						+ " | ruleValid=" + ruleValid
						+ " | CreateEnabled=" + createEnabled
						+ " | Pass=" + pass,
				pass);
		if (!pass) {
			SoftAssertManager.recordFailure(field + " trial " + entered,
					new AssertionError(why + " | " + MasterFieldRules.describeLengthTrial(field, entered, accepted)));
		}
	}

	private void search(String query) {
		WebElement box = PomElementManager.findVisible(StateMasterPage.SEARCH_INPUT, waitSeconds);
		String value = query == null ? "" : query;
		ExecutionLogger.info("State Master search | typing TestData query='" + value + "'");
		((org.openqa.selenium.JavascriptExecutor) getDriver()).executeScript(
				"arguments[0].scrollIntoView({block:'center'});"
						+ "arguments[0].style.outline='3px solid #f59e0b';",
				box);
		box.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		box.sendKeys(Keys.DELETE);
		for (char c : value.toCharArray()) {
			box.sendKeys(String.valueOf(c));
			MasterUiHelper.sleep(75);
		}
		try {
			Thread.sleep(value.isEmpty() ? 400 : 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			try {
				((org.openqa.selenium.JavascriptExecutor) getDriver())
						.executeScript("arguments[0].style.outline='';", box);
			} catch (Exception ignored) {
				// React may replace the input after filtering.
			}
		}
	}

	private void resetSearch() {
		if (!getDriver().findElements(StateMasterPage.RESET_FILTERS).isEmpty()) {
			PomElementManager.click(StateMasterPage.RESET_FILTERS, waitSeconds);
		} else {
			search("");
		}
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean goNextPageIfEnabled() {
		List<WebElement> next = getDriver().findElements(StateMasterPage.NEXT_PAGE);
		if (next.isEmpty()) {
			return false;
		}
		WebElement btn = next.get(0);
		if (!btn.isEnabled() || "true".equalsIgnoreCase(btn.getAttribute("disabled"))) {
			return false;
		}
		try {
			btn.click();
			Thread.sleep(500);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void typeInto(By locator, String text) {
		WebElement el = PomElementManager.findVisible(locator, waitSeconds);
		el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		el.sendKeys(Keys.DELETE);
		if (text != null && !text.isEmpty()) {
			el.sendKeys(text);
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String readInput(By locator) {
		try {
			return PomElementManager.getAttribute(locator, "value", waitSeconds);
		} catch (Exception e) {
			return "";
		}
	}

	private boolean isCreateEnabled() {
		try {
			WebElement btn = getDriver().findElement(StateMasterPage.CREATE_STATE_BUTTON);
			return btn.isEnabled() && !"true".equalsIgnoreCase(btn.getAttribute("disabled"));
		} catch (Exception e) {
			return false;
		}
	}

	private void waitForCreatePanelToClose() {
		long deadline = System.currentTimeMillis() + 5000L;
		while (System.currentTimeMillis() < deadline
				&& (MasterUiHelper.isVisible(StateMasterPage.CREATE_PANEL_TITLE, 1)
						|| !getDriver().findElements(By.cssSelector(
								".MuiBackdrop-root:not([style*='opacity: 0']), .MuiModal-backdrop:not([style*='opacity: 0'])"))
								.isEmpty())) {
			MasterUiHelper.sleep(200);
		}
		MasterUiHelper.sleep(500);
	}

	private static boolean isDuplicateMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("already") || value.contains("exist") || value.contains("duplicate");
	}

	private boolean isCheckboxChecked(WebElement row) {
		try {
			for (WebElement cb : row.findElements(By.cssSelector("input[type='checkbox']"))) {
				if (cb.isSelected() || "true".equalsIgnoreCase(cb.getAttribute("checked"))
						|| "true".equalsIgnoreCase(cb.getAttribute("aria-checked"))) {
					return true;
				}
			}
		} catch (Exception ignored) {
			// default true if unknown
		}
		return true;
	}

	private String safeText(By locator) {
		try {
			return PomElementManager.getText(locator, 3);
		} catch (Exception e) {
			return "";
		}
	}

	private int parseIntSafe(String s, int fallback) {
		try {
			return Integer.parseInt(s.replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return fallback;
		}
	}
}
