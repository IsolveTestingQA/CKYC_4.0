/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.masters.hierarchy;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.HierarchyMasterStore;
import Com.Ckyc_4_0.UtilityFiles.HierarchyMasterStore.HierarchyRecord;
import Com.Ckyc_4_0.UtilityFiles.HierarchyMasterStore.Tab;
import Com.Ckyc_4_0.UtilityFiles.MasterTestContext;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.pages.masters.hierarchy.HierarchyMasterPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Hierarchy Master — FI, Region, CPC, Branch tabs. Safe CRUD only on TEST AUTO records.
 */
public class HierarchyMasterFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(HierarchyMasterFunctionality.class);
	private final int waitSeconds = ConfigReader.getExplicitWait();
	private final Random random = new Random();

	public void openHierarchyMasterFromMenu() {
		MasterUiHelper.openMastersSubmodule("Hierarchy Master", HierarchyMasterPage.PAGE_HEADING, waitSeconds);
		StepLog.info("Hierarchy Master | Navigate", "Open Masters → Hierarchy Master",
				"Hierarchy Master page visible", "URL/page heading Hierarchy Master", "menu=Hierarchy Master");
		Hooks.captureMilestone("Hierarchy Master page opened");
	}

	public void verifyHierarchyMasterPageLoaded() {
		PomElementManager.findVisible(HierarchyMasterPage.PAGE_HEADING, waitSeconds);
		boolean tabsVisible = MasterUiHelper.isVisible(HierarchyMasterPage.TABLIST, 2)
				|| MasterUiHelper.isVisible(HierarchyMasterPage.TAB_FI_MASTER, 2)
				|| MasterUiHelper.isVisible(HierarchyMasterPage.SEARCH_FI, 2);
		if (!tabsVisible) {
			PomElementManager.findVisible(HierarchyMasterPage.TAB_FI_MASTER, waitSeconds);
		}
		StepLog.info("Hierarchy Master | Page load", "Verify Hierarchy Master loaded",
				"Heading + tabs/search visible", "Loaded | tabsVisible=" + tabsVisible, "tabs=FI,Region,CPC,Branch");
	}

	public void verifySummaryChips() {
		int fi = MasterUiHelper.readChip(HierarchyMasterPage.CHIP_FI, waitSeconds);
		int region = MasterUiHelper.readChip(HierarchyMasterPage.CHIP_REGION, waitSeconds);
		int cpc = MasterUiHelper.readChip(HierarchyMasterPage.CHIP_CPC, waitSeconds);
		int branch = MasterUiHelper.readChip(HierarchyMasterPage.CHIP_BRANCH, waitSeconds);
		boolean pass = fi >= 0 && region >= 0 && cpc >= 0 && branch >= 0;
		StepLog.check("Hierarchy Master | Summary chips",
				"FI/Region/CPC/Branch counts visible",
				"All four summary chips readable",
				"FI=" + fi + " Region=" + region + " CPC=" + cpc + " Branch=" + branch,
				pass, "chips=4");
	}

	// ---------- FI Master ----------

	public void runFiMasterTabFlow() {
		selectTab(HierarchyMasterPage.TAB_FI_MASTER, "FI Master");
		loadGridIntoStore(Tab.FI, 2, 3);
		runMandatoryValidation(Tab.FI);
		verifyDuplicateExisting(Tab.FI);
		createUniqueTestRecord(Tab.FI);
		editCreatedTestRecord(Tab.FI);
		toggleCreatedTestRecord(Tab.FI);
		searchCreatedTestRecord(Tab.FI);
		randomSearchFromStore(Tab.FI);
	}

	public void runCpcMasterTabFlow() {
		selectTab(HierarchyMasterPage.TAB_CPC, "CPC Master");
		loadGridIntoStore(Tab.CPC, 2, 3);
		runMandatoryValidation(Tab.CPC);
		verifyDuplicateExisting(Tab.CPC);
		createUniqueTestRecord(Tab.CPC);
		editCreatedTestRecord(Tab.CPC);
		toggleCreatedTestRecord(Tab.CPC);
		searchCreatedTestRecord(Tab.CPC);
		randomSearchFromStore(Tab.CPC);
	}

	public void runRegionTabFlow() {
		selectTab(HierarchyMasterPage.TAB_REGION, "Region");
		loadGridIntoStore(Tab.REGION, 2, 3);
		runMandatoryValidation(Tab.REGION);
		verifyDuplicateExisting(Tab.REGION);
		createUniqueTestRecord(Tab.REGION);
		editCreatedTestRecord(Tab.REGION);
		toggleCreatedTestRecord(Tab.REGION);
		searchCreatedTestRecord(Tab.REGION);
		randomSearchFromStore(Tab.REGION);
	}

	public void runBranchTabFlow() {
		selectTab(HierarchyMasterPage.TAB_BRANCH, "Branch");
		loadGridIntoStore(Tab.BRANCH, 2, 3);
		runMandatoryValidation(Tab.BRANCH);
		verifyDuplicateExisting(Tab.BRANCH);
		createUniqueTestRecord(Tab.BRANCH);
		editCreatedTestRecord(Tab.BRANCH);
		toggleCreatedTestRecord(Tab.BRANCH);
		searchCreatedTestRecord(Tab.BRANCH);
		randomSearchFromStore(Tab.BRANCH);
	}

	private void selectTab(By tabLocator, String label) {
		MasterUiHelper.clickJs(tabLocator, waitSeconds);
		MasterUiHelper.sleep(800);
		StepLog.info("Hierarchy Master | Tab", "Select " + label + " tab", label + " grid visible",
				"Tab selected", "tab=" + label);
	}

	private void loadGridIntoStore(Tab tab, int codeCellIndex, int nameCellIndex) {
		HierarchyMasterStore.clear(tab);
		resetFilters();
		int guard = 0;
		while (guard++ < 50) {
			for (WebElement row : getDriver().findElements(HierarchyMasterPage.GRID_DATA_ROWS)) {
				try {
					List<WebElement> cells = row.findElements(By.xpath(".//div[@role='gridcell']"));
					if (cells.size() < codeCellIndex + 1) {
						continue;
					}
					String code = cells.get(codeCellIndex).getText().trim();
					String name = cells.size() > nameCellIndex ? cells.get(nameCellIndex).getText().trim() : "";
					if (code.isBlank()) {
						continue;
					}
					boolean active = MasterUiHelper.isToggleChecked(code) || MasterUiHelper.isToggleChecked(name);
					HierarchyMasterStore.put(new HierarchyRecord(tab, code, name, "", active));
				} catch (Exception e) {
					logger.debug("Skip hierarchy row: {}", e.getMessage());
				}
			}
			if (!MasterUiHelper.goNextPageIfEnabled()) {
				break;
			}
		}
		resetFilters();
		int count = HierarchyMasterStore.size(tab);
		StepLog.check("Hierarchy Master | Load map | " + tab,
				"Store grid rows for " + tab,
				"Map size > 0",
				"Stored " + count + " rows",
				count > 0, "tab=" + tab);
	}

	private void runMandatoryValidation(Tab tab) {
		openCreatePanel(tab);
		MasterUiHelper.clickJs(HierarchyMasterPage.CLEAR_BUTTON, waitSeconds);
		MasterUiHelper.clickJs(HierarchyMasterPage.CREATE_RECORD_BUTTON, waitSeconds);
		MasterUiHelper.sleep(500);
		int requiredCount = getDriver().findElements(HierarchyMasterPage.REQUIRED_HELPER).size();
		boolean pass = requiredCount >= minRequiredFields(tab);
		StepLog.check("Hierarchy Master | Mandatory | " + tab,
				"Submit empty create form",
				"Required helper shown for mandatory fields",
				"requiredHelpers=" + requiredCount + " minExpected=" + minRequiredFields(tab),
				pass, "tab=" + tab);
		if (tab == Tab.FI) {
			validateFiFieldTrialsInOpenPanel();
		} else if (tab == Tab.CPC) {
			validateCpcFieldTrialsInOpenPanel();
		}
		closeCreatePanelIfOpen();
	}

	private void validateFiFieldTrialsInOpenPanel() {
		try {
			MasterUiHelper.clickJs(HierarchyMasterPage.CLEAR_BUTTON, waitSeconds);
			MasterUiHelper.typeInto(HierarchyMasterPage.FI_NAME_INPUT, "ab", waitSeconds);
			MasterUiHelper.typeInto(HierarchyMasterPage.FI_CODE_INPUT, "xx", waitSeconds);
			String name = MasterUiHelper.readValue(HierarchyMasterPage.FI_NAME_INPUT, waitSeconds);
			String code = MasterUiHelper.readValue(HierarchyMasterPage.FI_CODE_INPUT, waitSeconds);
			boolean createEnabled = MasterUiHelper.isButtonEnabled(HierarchyMasterPage.CREATE_RECORD_BUTTON);
			StepLog.check("Hierarchy Master | FI field trial",
					"Enter short/invalid FI values",
					"Values captured; Create state logged",
					"name='" + name + "' code='" + code + "' createEnabled=" + createEnabled,
					true, "UI behavior recorded");
		} catch (Exception e) {
			StepLog.check("Hierarchy Master | FI field trial",
					"Enter short/invalid FI values",
					"Trial attempted",
					"Could not complete trial: " + e.getMessage(), false, "UI behavior recorded");
			SoftAssertManager.recordFailure("FI field trial", new AssertionError(e.getMessage()));
		}
	}

	private void validateCpcFieldTrialsInOpenPanel() {
		try {
			MasterUiHelper.clickJs(HierarchyMasterPage.CLEAR_BUTTON, waitSeconds);
			MasterUiHelper.typeInto(HierarchyMasterPage.CPC_CODE_INPUT, "!", waitSeconds);
			MasterUiHelper.typeInto(HierarchyMasterPage.CPC_CITY_INPUT, "123", waitSeconds);
			String code = MasterUiHelper.readValue(HierarchyMasterPage.CPC_CODE_INPUT, waitSeconds);
			String city = MasterUiHelper.readValue(HierarchyMasterPage.CPC_CITY_INPUT, waitSeconds);
			StepLog.check("Hierarchy Master | CPC field trial",
					"Enter special/numeric city trial",
					"Accepted values logged for UI review",
					"code='" + code + "' city='" + city + "'",
					true, "UI behavior recorded");
		} catch (Exception e) {
			StepLog.check("Hierarchy Master | CPC field trial",
					"Enter special/numeric city trial",
					"Trial attempted",
					"Could not complete trial: " + e.getMessage(), false, "UI behavior recorded");
			SoftAssertManager.recordFailure("CPC field trial", new AssertionError(e.getMessage()));
		}
	}

	private void verifyDuplicateExisting(Tab tab) {
		HierarchyRecord existing = HierarchyMasterStore.first(tab);
		if (existing == null) {
			loadGridIntoStore(tab, codeCellIndex(tab), nameCellIndex(tab));
			existing = HierarchyMasterStore.first(tab);
		}
		if (existing == null) {
			StepLog.info("Hierarchy Master | Duplicate | " + tab, "No existing row for duplicate test",
					"Skip duplicate", "Skipped", "tab=" + tab);
			return;
		}
		openCreatePanel(tab);
		fillCreateForm(tab, existing.code(), existing.name(), true);
		boolean enabled = MasterUiHelper.isButtonEnabled(HierarchyMasterPage.CREATE_RECORD_BUTTON);
		String toast = "";
		if (enabled) {
			MasterUiHelper.clickJs(HierarchyMasterPage.CREATE_RECORD_BUTTON, waitSeconds);
			toast = ToastHandler.waitAndRead(getDriver(), 3000);
		}
		boolean rejected = enabled && isDuplicateMessage(toast);
		StepLog.check("Hierarchy Master | Duplicate | " + tab,
				"Submit existing " + tab + " code/name",
				"Duplicate rejected with toast",
				"createEnabled=" + enabled + " toast='" + toast + "'",
				rejected || !enabled,
				"existingCode=" + existing.code());
		ToastHandler.dismissIfPresent(getDriver());
		closeCreatePanelIfOpen();
	}

	private void createUniqueTestRecord(Tab tab) {
		if (alreadyCreated(tab)) {
			StepLog.info("Hierarchy Master | Create | " + tab, "Reuse created TEST record",
					"TEST record available", describeCreated(tab), "reuse=true");
			return;
		}
		Set<String> attempted = new HashSet<>();
		StringBuilder attempts = new StringBuilder();
		for (int attempt = 1; attempt <= 3; attempt++) {
			openCreatePanel(tab);
			MasterUiHelper.clickJs(HierarchyMasterPage.CLEAR_BUTTON, waitSeconds);
			String code = pickUniqueCode(tab, attempted);
			String name = pickUniqueName(tab);
			attempted.add(code);
			fillCreateForm(tab, code, name, true);
			String acceptedCode = readPrimaryValue(tab);
			String acceptedName = readSecondaryValue(tab);
			boolean enabled = MasterUiHelper.isButtonEnabled(HierarchyMasterPage.CREATE_RECORD_BUTTON);
			String testData = "attempt=" + attempt + "; code=" + code + "; acceptedCode=" + acceptedCode
					+ "; name=" + name + "; acceptedName=" + acceptedName + "; enabled=" + enabled;
			appendAttempt(attempts, testData);
			if (!enabled) {
				continue;
			}
			MasterUiHelper.clickJs(HierarchyMasterPage.CREATE_RECORD_BUTTON, waitSeconds);
			String toast = ToastHandler.waitAndRead(getDriver(), 2500);
			if (isDuplicateMessage(toast)) {
				ToastHandler.dismissIfPresent(getDriver());
				continue;
			}
			waitForCreatePanelToClose(tab);
			MasterUiHelper.clickJs(HierarchyMasterPage.REFRESH_PAGE, waitSeconds);
			MasterUiHelper.sleep(700);
			search(tab, acceptedCode.isBlank() ? code : acceptedCode);
			boolean found = MasterUiHelper.rowContains(acceptedCode) || MasterUiHelper.rowContains(acceptedName);
			resetSearch(tab);
			if (found) {
				storeCreated(tab, acceptedCode.isBlank() ? code : acceptedCode,
						acceptedName.isBlank() ? name : acceptedName);
				HierarchyMasterStore.put(new HierarchyRecord(tab,
						acceptedCode.isBlank() ? code : acceptedCode,
						acceptedName.isBlank() ? name : acceptedName, "", true));
				StepLog.check("Hierarchy Master | Create TEST | " + tab,
						"Create unique TEST AUTO record",
						"Row appears in grid",
						"found=true toast='" + toast + "'", true, testData);
				return;
			}
		}
		throw new PopupHandler.ModuleBlockerException("Hierarchy Master " + tab,
				"Could not create and verify TEST record after retries.", attempts.toString());
	}

	private void editCreatedTestRecord(Tab tab) {
		String code = createdCode(tab);
		String name = createdName(tab);
		if (code == null) {
			StepLog.info("Hierarchy Master | Edit | " + tab, "Skipped — no TEST record", "Edit created only",
					"Skipped", "tab=" + tab);
			return;
		}
		search(tab, code);
		By edit = MasterUiHelper.editButton(name != null ? name : code);
		if (getDriver().findElements(edit).isEmpty()) {
			edit = MasterUiHelper.editButton(code);
		}
		if (getDriver().findElements(edit).isEmpty()) {
			StepLog.check("Hierarchy Master | Edit | " + tab, "Open Edit for TEST record",
					"Edit button visible", "Not found for " + code, false, "code=" + code);
			return;
		}
		MasterUiHelper.clickJs(edit, waitSeconds);
		MasterUiHelper.sleep(600);
		String edited = buildEditedName(name);
		By nameInput = nameInput(tab);
		if (nameInput != null) {
			MasterUiHelper.typeInto(nameInput, edited, waitSeconds);
		}
		if (!getDriver().findElements(HierarchyMasterPage.SAVE_OR_UPDATE_BUTTON).isEmpty()) {
			MasterUiHelper.clickJs(HierarchyMasterPage.SAVE_OR_UPDATE_BUTTON, waitSeconds);
		} else {
			MasterUiHelper.clickSaveIfPresent(waitSeconds);
		}
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(700);
		search(tab, code);
		String expected = edited;
		boolean renamed = MasterUiHelper.rowContains(expected);
		if (renamed) {
			updateCreatedName(tab, expected);
		}
		StepLog.check("Hierarchy Master | Edit | " + tab,
				"Edit only newly created TEST record",
				"Grid shows updated name",
				"renamed=" + renamed + " toast='" + toast + "'", renamed,
				"code=" + code + "; newName=" + expected);
		resetSearch(tab);
	}

	private void toggleCreatedTestRecord(Tab tab) {
		String code = createdCode(tab);
		String name = createdName(tab);
		if (code == null) {
			StepLog.info("Hierarchy Master | Toggle | " + tab, "Skipped", "Toggle created only", "Skipped", "");
			return;
		}
		search(tab, code);
		String rowKey = name != null ? name : code;
		boolean checkedBefore = MasterUiHelper.isToggleChecked(rowKey);
		if (!checkedBefore) {
			checkedBefore = MasterUiHelper.isToggleChecked(code);
		}
		By toggle = MasterUiHelper.toggleFor(rowKey);
		if (getDriver().findElements(toggle).isEmpty()) {
			toggle = MasterUiHelper.toggleFor(code);
		}
		if (getDriver().findElements(toggle).isEmpty()) {
			StepLog.check("Hierarchy Master | Toggle | " + tab, "Toggle TEST record",
					"Toggle present", "Not found", false, "code=" + code);
			return;
		}
		MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
		String toast = ToastHandler.waitAndRead(getDriver(), 2000);
		MasterUiHelper.sleep(600);
		boolean checkedAfter = MasterUiHelper.isToggleChecked(rowKey);
		if (!checkedAfter) {
			checkedAfter = MasterUiHelper.isToggleChecked(code);
		}
		boolean changed = checkedBefore != checkedAfter;
		if (checkedAfter != checkedBefore) {
			MasterUiHelper.clickHiddenInput(toggle, waitSeconds);
			ToastHandler.waitAndRead(getDriver(), 1500);
			MasterUiHelper.sleep(500);
		}
		boolean restored = MasterUiHelper.isToggleChecked(rowKey) || MasterUiHelper.isToggleChecked(code);
		StepLog.check("Hierarchy Master | Toggle | " + tab,
				"Active → Inactive → restore Active on TEST record",
				"Toggle changes and restores",
				"checked " + checkedBefore + "→" + checkedAfter + " restored=" + restored
						+ " toast='" + toast + "'",
				changed && restored == checkedBefore, "code=" + code);
		resetSearch(tab);
	}

	private void searchCreatedTestRecord(Tab tab) {
		String code = createdCode(tab);
		String name = createdName(tab);
		if (code == null) {
			StepLog.info("Hierarchy Master | Search TEST | " + tab, "Skipped", "Search created", "Skipped", "");
			return;
		}
		search(tab, code);
		boolean found = MasterUiHelper.rowContains(code) || (name != null && MasterUiHelper.rowContains(name));
		StepLog.check("Hierarchy Master | Search TEST | " + tab,
				"Search newly created TEST record",
				"Row found in grid",
				"found=" + found, found, "query=" + code);
		resetSearch(tab);
	}

	private void randomSearchFromStore(Tab tab) {
		List<HierarchyRecord> pool = new ArrayList<>(HierarchyMasterStore.forTab(tab).values());
		if (pool.isEmpty()) {
			loadGridIntoStore(tab, codeCellIndex(tab), nameCellIndex(tab));
			pool = new ArrayList<>(HierarchyMasterStore.forTab(tab).values());
		}
		Collections.shuffle(pool, random);
		int maxAttempts = Math.min(3, pool.size());
		boolean passed = false;
		String lastDetail = "";
		for (int i = 0; i < maxAttempts; i++) {
			HierarchyRecord pick = pool.get(i);
			String query = random.nextBoolean() ? pick.code() : pick.name();
			if (query == null || query.isBlank()) {
				query = pick.code();
			}
			search(tab, query);
			boolean found = MasterUiHelper.rowContains(pick.code())
					|| (pick.name() != null && MasterUiHelper.rowContains(pick.name()));
			lastDetail = "query='" + query + "' code=" + pick.code() + " found=" + found;
			StepLog.check("Hierarchy Master | Random search | " + tab,
					"Search random row from map",
					"Expected row visible",
					lastDetail, found, "attempt=" + (i + 1));
			if (found) {
				passed = true;
				break;
			}
		}
		resetSearch(tab);
		if (!passed && maxAttempts > 0) {
			SoftAssertManager.recordFailure("Hierarchy random search " + tab,
					new AssertionError("Random search failed. Last: " + lastDetail));
		}
	}

	private void openCreatePanel(Tab tab) {
		if (!MasterUiHelper.isVisible(createTitle(tab), 1)) {
			MasterUiHelper.clickJs(HierarchyMasterPage.NEW_RECORD_BUTTON, waitSeconds);
		}
		PomElementManager.findVisible(createTitle(tab), waitSeconds);
	}

	private void closeCreatePanelIfOpen() {
		if (!MasterUiHelper.isVisible(HierarchyMasterPage.CLEAR_BUTTON, 1)) {
			return;
		}
		List<WebElement> closeBtns = getDriver().findElements(HierarchyMasterPage.CANCEL_OR_CLOSE);
		if (!closeBtns.isEmpty()) {
			MasterUiHelper.clickJs(HierarchyMasterPage.CANCEL_OR_CLOSE, waitSeconds);
		} else {
			try {
				getDriver().switchTo().activeElement().sendKeys(org.openqa.selenium.Keys.ESCAPE);
			} catch (Exception ignored) {
			}
		}
		MasterUiHelper.sleep(500);
	}

	private void fillCreateForm(Tab tab, String code, String name, boolean active) {
		switch (tab) {
			case FI -> {
				MasterUiHelper.typeInto(HierarchyMasterPage.FI_CODE_INPUT, code, waitSeconds);
				MasterUiHelper.typeInto(HierarchyMasterPage.FI_NAME_INPUT, name, waitSeconds);
			}
			case CPC -> {
				MasterUiHelper.typeInto(HierarchyMasterPage.CPC_CODE_INPUT, code, waitSeconds);
				MasterUiHelper.typeInto(HierarchyMasterPage.CPC_CITY_INPUT, name, waitSeconds);
			}
			case REGION -> {
				MasterUiHelper.typeInto(HierarchyMasterPage.REGION_CODE_INPUT, code, waitSeconds);
				MasterUiHelper.typeInto(HierarchyMasterPage.REGION_NAME_INPUT, name, waitSeconds);
				selectFirstAvailableFi();
			}
			case BRANCH -> {
				MasterUiHelper.typeInto(HierarchyMasterPage.BRANCH_CODE_INPUT, code, waitSeconds);
				MasterUiHelper.typeInto(HierarchyMasterPage.BRANCH_NAME_INPUT, name, waitSeconds);
				selectCreatedOrFirstRegion();
				selectCreatedCpcIfAvailable();
			}
			default -> throw new IllegalArgumentException("Unknown tab " + tab);
		}
		setActiveIfNeeded(active);
	}

	private void selectFirstAvailableFi() {
		String fi = MasterTestContext.createdFiName();
		if (fi == null) {
			HierarchyRecord fiRec = HierarchyMasterStore.first(Tab.FI);
			fi = fiRec != null ? fiRec.name() : null;
		}
		if (fi != null && !fi.isBlank()) {
			try {
				MasterUiHelper.selectMuiOption(HierarchyMasterPage.REGION_FI_COMBO,
						List.of(fi, MasterTestContext.createdFiCode()), waitSeconds);
			} catch (Exception e) {
				ExecutionLogger.warn("Region FI select failed: " + e.getMessage());
			}
		}
	}

	private void selectCreatedOrFirstRegion() {
		String region = MasterTestContext.createdRegionName();
		if (region == null) {
			HierarchyRecord rec = HierarchyMasterStore.first(Tab.REGION);
			region = rec != null ? rec.name() : null;
		}
		if (region != null && !region.isBlank()) {
			try {
				MasterUiHelper.selectMuiOption(HierarchyMasterPage.BRANCH_REGION_COMBO,
						List.of(region, MasterTestContext.createdRegionCode()), waitSeconds);
			} catch (Exception e) {
				ExecutionLogger.warn("Branch Region select failed: " + e.getMessage());
			}
		}
	}

	private void selectCreatedCpcIfAvailable() {
		String cpc = MasterTestContext.createdCpcCode();
		if (cpc == null) {
			HierarchyRecord rec = HierarchyMasterStore.first(Tab.CPC);
			cpc = rec != null ? rec.code() : null;
		}
		if (cpc != null && !cpc.isBlank()) {
			try {
				MasterUiHelper.selectMuiOption(HierarchyMasterPage.BRANCH_CPC_COMBO,
						List.of(cpc, MasterTestContext.createdCpcCity()), waitSeconds);
			} catch (Exception ignored) {
				// CPC is optional on Branch
			}
		}
	}

	private void setActiveIfNeeded(boolean active) {
		if (!active) {
			try {
				MasterUiHelper.clickHiddenInput(HierarchyMasterPage.ACTIVE_CHECKBOX, waitSeconds);
			} catch (Exception ignored) {
			}
		}
	}

	private void search(Tab tab, String query) {
		MasterUiHelper.search(searchBox(tab), query == null ? "" : query, waitSeconds);
	}

	private void resetSearch(Tab tab) {
		if (!getDriver().findElements(HierarchyMasterPage.RESET_FILTERS).isEmpty()) {
			MasterUiHelper.clickJs(HierarchyMasterPage.RESET_FILTERS, waitSeconds);
		} else {
			MasterUiHelper.resetSearch(searchBox(tab), waitSeconds);
		}
		MasterUiHelper.sleep(400);
	}

	private void resetFilters() {
		if (!getDriver().findElements(HierarchyMasterPage.RESET_FILTERS).isEmpty()) {
			MasterUiHelper.clickJs(HierarchyMasterPage.RESET_FILTERS, waitSeconds);
			MasterUiHelper.sleep(400);
		}
	}

	private void waitForCreatePanelToClose(Tab tab) {
		long deadline = System.currentTimeMillis() + 5000L;
		while (System.currentTimeMillis() < deadline
				&& MasterUiHelper.isVisible(createTitle(tab), 1)) {
			MasterUiHelper.sleep(200);
		}
		MasterUiHelper.sleep(500);
	}

	private static boolean isDuplicateMessage(String toast) {
		String value = toast == null ? "" : toast.toLowerCase();
		return value.contains("already") || value.contains("exist") || value.contains("duplicate");
	}

	private static int minRequiredFields(Tab tab) {
		return switch (tab) {
			case FI, CPC, REGION, BRANCH -> 1;
		};
	}

	private static int codeCellIndex(Tab tab) {
		return 1;
	}

	private static int nameCellIndex(Tab tab) {
		return switch (tab) {
			case FI, REGION, BRANCH -> 2;
			case CPC -> 2;
		};
	}

	private static By searchBox(Tab tab) {
		return switch (tab) {
			case FI -> HierarchyMasterPage.SEARCH_FI;
			case REGION -> HierarchyMasterPage.SEARCH_REGION;
			case CPC -> HierarchyMasterPage.SEARCH_CPC;
			case BRANCH -> HierarchyMasterPage.SEARCH_BRANCH;
		};
	}

	private static By createTitle(Tab tab) {
		return switch (tab) {
			case FI -> HierarchyMasterPage.CREATE_FI_TITLE;
			case REGION -> HierarchyMasterPage.CREATE_REGION_TITLE;
			case CPC -> HierarchyMasterPage.CREATE_CPC_TITLE;
			case BRANCH -> HierarchyMasterPage.CREATE_BRANCH_TITLE;
		};
	}

	private static By nameInput(Tab tab) {
		return switch (tab) {
			case FI -> HierarchyMasterPage.FI_NAME_INPUT;
			case REGION -> HierarchyMasterPage.REGION_NAME_INPUT;
			case CPC -> HierarchyMasterPage.CPC_CITY_INPUT;
			case BRANCH -> HierarchyMasterPage.BRANCH_NAME_INPUT;
		};
	}

	private String pickUniqueCode(Tab tab, Set<String> excluded) {
		for (int i = 0; i < 5; i++) {
			String code = switch (tab) {
				case FI -> MasterTestContext.uniqueTestFiCode();
				case REGION -> MasterTestContext.uniqueTestRegionCode();
				case CPC -> MasterTestContext.uniqueTestCpcCode();
				case BRANCH -> MasterTestContext.uniqueTestBranchCode();
			};
			if (!excluded.contains(code) && !codeExistsInStore(tab, code)) {
				return code;
			}
		}
		return "T" + System.currentTimeMillis() % 100000;
	}

	private String pickUniqueName(Tab tab) {
		return switch (tab) {
			case FI -> MasterTestContext.uniqueTestFiName();
			case REGION -> MasterTestContext.uniqueTestRegionName();
			case CPC -> MasterTestContext.uniqueTestCpcCity();
			case BRANCH -> MasterTestContext.uniqueTestBranchName();
		};
	}

	private boolean codeExistsInStore(Tab tab, String code) {
		return HierarchyMasterStore.forTab(tab).values().stream()
				.anyMatch(r -> r.code().equalsIgnoreCase(code));
	}

	private String readPrimaryValue(Tab tab) {
		return switch (tab) {
			case FI -> MasterUiHelper.readValue(HierarchyMasterPage.FI_CODE_INPUT, waitSeconds);
			case REGION -> MasterUiHelper.readValue(HierarchyMasterPage.REGION_CODE_INPUT, waitSeconds);
			case CPC -> MasterUiHelper.readValue(HierarchyMasterPage.CPC_CODE_INPUT, waitSeconds);
			case BRANCH -> MasterUiHelper.readValue(HierarchyMasterPage.BRANCH_CODE_INPUT, waitSeconds);
		};
	}

	private String readSecondaryValue(Tab tab) {
		return switch (tab) {
			case FI -> MasterUiHelper.readValue(HierarchyMasterPage.FI_NAME_INPUT, waitSeconds);
			case REGION -> MasterUiHelper.readValue(HierarchyMasterPage.REGION_NAME_INPUT, waitSeconds);
			case CPC -> MasterUiHelper.readValue(HierarchyMasterPage.CPC_CITY_INPUT, waitSeconds);
			case BRANCH -> MasterUiHelper.readValue(HierarchyMasterPage.BRANCH_NAME_INPUT, waitSeconds);
		};
	}

	private static boolean alreadyCreated(Tab tab) {
		return switch (tab) {
			case FI -> MasterTestContext.createdFiCode() != null;
			case REGION -> MasterTestContext.createdRegionCode() != null;
			case CPC -> MasterTestContext.createdCpcCode() != null;
			case BRANCH -> MasterTestContext.createdBranchCode() != null;
		};
	}

	private static String createdCode(Tab tab) {
		return switch (tab) {
			case FI -> MasterTestContext.createdFiCode();
			case REGION -> MasterTestContext.createdRegionCode();
			case CPC -> MasterTestContext.createdCpcCode();
			case BRANCH -> MasterTestContext.createdBranchCode();
		};
	}

	private static String createdName(Tab tab) {
		return switch (tab) {
			case FI -> MasterTestContext.createdFiName();
			case REGION -> MasterTestContext.createdRegionName();
			case CPC -> MasterTestContext.createdCpcCity();
			case BRANCH -> MasterTestContext.createdBranchName();
		};
	}

	private static void storeCreated(Tab tab, String code, String name) {
		switch (tab) {
			case FI -> MasterTestContext.setCreatedFi(code, name);
			case REGION -> MasterTestContext.setCreatedRegion(code, name);
			case CPC -> MasterTestContext.setCreatedCpc(code, name);
			case BRANCH -> MasterTestContext.setCreatedBranch(code, name);
		}
	}

	private static void updateCreatedName(Tab tab, String name) {
		switch (tab) {
			case FI -> MasterTestContext.setCreatedFi(MasterTestContext.createdFiCode(), name);
			case REGION -> MasterTestContext.setCreatedRegion(MasterTestContext.createdRegionCode(), name);
			case CPC -> MasterTestContext.setCreatedCpc(MasterTestContext.createdCpcCode(), name);
			case BRANCH -> MasterTestContext.setCreatedBranch(MasterTestContext.createdBranchCode(), name);
		}
	}

	private static String describeCreated(Tab tab) {
		return "code=" + createdCode(tab) + " name=" + createdName(tab);
	}

	private static String buildEditedName(String name) {
		if (name == null || name.isBlank()) {
			return "TEST AUTO EDIT";
		}
		String edited = name.endsWith(" EDIT") ? name : name + " EDIT";
		return edited.length() > 80 ? edited.substring(0, 80) : edited;
	}

	private static void appendAttempt(StringBuilder sb, String detail) {
		if (sb.length() > 0) {
			sb.append(" || ");
		}
		sb.append(detail);
	}
}
