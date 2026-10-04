/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dashboard;

import Com.Ckyc_4_0.pages.dashboard.DashboardPage;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.CurrentDataManager;
import Com.Ckyc_4_0.UtilityFiles.DashboardCountStore;
import Com.Ckyc_4_0.UtilityFiles.DashboardDataExcelReport;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.utils.DashboardFilterDiscovery;
import Com.Ckyc_4_0.utils.DashboardFilterDiscovery.DiscoveredFilter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardFunctionality extends BaseClass {

	private static final Logger logger = LoggerFactory.getLogger(DashboardFunctionality.class);
	private static final Pattern MISMATCH_ARIA_PATTERN = Pattern.compile("Count mismatch of (\\d+)");
	private static final Pattern MISMATCH_TEXT_PATTERN = Pattern.compile("=\\s*([+-]?\\d+)");

	private final int waitSeconds = ConfigReader.getExplicitWait();

	private static final AtomicBoolean dataCollectionInitialized = new AtomicBoolean(false);

	private String activeDateFilter = "today";
	private String activeConstitutionFilter = "Individual";
	private String activeApplicationFilter = "";
	private String activeFromDate = "";
	private String activeToDate = "";

	/** Last selected filter objects — used to re-verify UI stays on the same combo after modal/refresh. */
	private DiscoveredFilter activeDateFilterObj;
	private DiscoveredFilter activeConstitutionFilterObj;
	private DiscoveredFilter activeApplicationFilterObj;

	private List<DiscoveredFilter> discoveredDateFilters = List.of();
	private List<DiscoveredFilter> discoveredConstitutionFilters = List.of();
	private List<DiscoveredFilter> discoveredApplicationFilters = List.of();

	public void verifyDashboardIsDisplayed() {
		waitUntilUrlContains(ConfigReader.getDashboardUrlPath(), ConfigReader.getLoginWait());
		PomElementManager.findVisible(DashboardPage.DATE_FILTER_GROUP, waitSeconds);
		waitForPageFullyLoaded();
		logger.info("Dashboard loaded — URL: {}", getDriver().getCurrentUrl());
	}

	/**
	 * Reads filters from live UI.
	 * Constitution empty → default Individual (continue, do not fail).
	 * Date empty → default Today.
	 * Application (NEW/UPDATE) optional — only if present.
	 */
	public void discoverAvailableFiltersFromUi() {
		waitForPageFullyLoaded();
		discoveredDateFilters = DashboardFilterDiscovery.resolveDateFiltersOrDefault(getDriver());
		discoveredConstitutionFilters = DashboardFilterDiscovery.resolveConstitutionFiltersOrDefault(getDriver());
		discoveredApplicationFilters = DashboardFilterDiscovery.discoverApplicationTypeFilters(getDriver());

		ExtentReportManager.logInfo("Discovered date filters: " + describeFilters(discoveredDateFilters));
		ExtentReportManager.logInfo(
				"Discovered constitution filters: " + describeFilters(discoveredConstitutionFilters));
		if (discoveredApplicationFilters.isEmpty()) {
			ExtentReportManager.logInfo("Application type filter (NEW/UPDATE) not present — skipped");
		} else {
			ExtentReportManager.logInfo(
					"Discovered application filters: " + describeFilters(discoveredApplicationFilters));
		}

		int appFactor = Math.max(1, discoveredApplicationFilters.size());
		logger.info("Available filter matrix: {} date × {} constitution × {} application = {} combinations",
				discoveredDateFilters.size(), discoveredConstitutionFilters.size(), appFactor,
				discoveredDateFilters.size() * discoveredConstitutionFilters.size() * appFactor);
	}

	public List<DiscoveredFilter> getDiscoveredDateFilters() {
		return discoveredDateFilters;
	}

	public List<DiscoveredFilter> getDiscoveredConstitutionFilters() {
		return discoveredConstitutionFilters;
	}

	public List<DiscoveredFilter> getDiscoveredApplicationFilters() {
		return discoveredApplicationFilters;
	}

	/**
	 * Execution order (only filters present on UI are used):
	 * <pre>
	 *   Application: NEW → UPDATE
	 *     Date: Today → Weekly → Monthly → Overdue
	 *       Constitution: ALL → Individual → Legal Entity
	 *         → read all summary cards + sub-items for that combo
	 * </pre>
	 * Example: Today+NEW+ALL → Today+NEW+Individual → Today+NEW+Legal →
	 * Weekly+NEW+ALL → … → Overdue+NEW+Legal → then same dates/constitutions for UPDATE.
	 */
	public void collectCountsForAllFilterCombinations() {
		prepareDashboardDataCollection();
		if (discoveredDateFilters.isEmpty() || discoveredConstitutionFilters.isEmpty()) {
			discoverAvailableFiltersFromUi();
		}

		List<DiscoveredFilter> dates = orderDateFilters(discoveredDateFilters);
		List<DiscoveredFilter> constitutions = orderConstitutionFilters(discoveredConstitutionFilters);
		List<DiscoveredFilter> applications = orderApplicationFilters(discoveredApplicationFilters);

		List<DiscoveredFilter> appLoop = new ArrayList<>();
		if (applications.isEmpty()) {
			appLoop.add(null); // no NEW/UPDATE on UI — still run date × constitution
		} else {
			appLoop.addAll(applications);
		}

		ExtentReportManager.logInfo("Filter execution order | Application(outer): "
				+ describeFilters(appLoop.stream().filter(f -> f != null).toList())
				+ " | Date: " + describeFilters(dates)
				+ " | Constitution(inner): " + describeFilters(constitutions));

		for (DiscoveredFilter applicationFilter : appLoop) {
			if (applicationFilter != null) {
				selectApplicationTypeFilter(applicationFilter);
			} else {
				activeApplicationFilter = "";
				activeApplicationFilterObj = null;
			}

			for (DiscoveredFilter dateFilter : dates) {
				selectDateRangeFilter(dateFilter);

				for (DiscoveredFilter constitutionFilter : constitutions) {
					selectConstitutionFilter(constitutionFilter);

					String comboKey = buildComboKey(dateFilter.key(), constitutionFilter.key(),
							applicationFilter == null ? null : applicationFilter.key());
					logActiveFilterCombination(comboKey);
					try {
						assertActiveFiltersStillSelected(comboKey);
						collectCountsForActiveFilters();
						saveDashboardDataForActiveFilters();
						if (ConfigReader.screenshotOnEachFilter()) {
							Hooks.captureMilestone("Dashboard feature | filter=" + comboKey
									+ " | summary cards + A–G sub-items captured");
						}
					} catch (Throwable t) {
						logger.error("Dashboard count collection failed for {} — continuing", comboKey, t);
						SoftAssertManager.recordFailure("Collect counts | filter=" + comboKey, t);
						Hooks.captureMilestone("Dashboard feature FAIL | filter=" + comboKey, false);
					}
				}
			}
		}
	}

	/** NEW (01) before UPDATE (03); unknown values keep discovery order after known ones. */
	private List<DiscoveredFilter> orderApplicationFilters(List<DiscoveredFilter> filters) {
		return sortByPreferredOrder(filters, List.of("01", "new", "03", "update"), true);
	}

	/** Today → Weekly → Monthly → Overdue/aged. */
	private List<DiscoveredFilter> orderDateFilters(List<DiscoveredFilter> filters) {
		return sortByPreferredOrder(filters,
				List.of("today", "weekly", "monthly", "aged", "overdue"), true);
	}

	/** ALL → Individual → Legal Entity (HTML values 0, 1, 2). */
	private List<DiscoveredFilter> orderConstitutionFilters(List<DiscoveredFilter> filters) {
		return sortByPreferredOrder(filters,
				List.of("0", "all", "1", "individual", "2", "legal entity", "legalentity", "legal"), true);
	}

	private List<DiscoveredFilter> sortByPreferredOrder(List<DiscoveredFilter> filters,
			List<String> preferredTokens, boolean preferValueAttr) {
		if (filters == null || filters.isEmpty()) {
			return List.of();
		}
		List<DiscoveredFilter> sorted = new ArrayList<>(filters);
		sorted.sort((a, b) -> Integer.compare(filterRank(a, preferredTokens, preferValueAttr),
				filterRank(b, preferredTokens, preferValueAttr)));
		return sorted;
	}

	private int filterRank(DiscoveredFilter filter, List<String> preferredTokens, boolean preferValueAttr) {
		String value = filter.valueAttr() == null ? "" : filter.valueAttr().trim().toLowerCase();
		String text = filter.displayText() == null ? "" : filter.displayText().trim().toLowerCase();
		String key = filter.key() == null ? "" : filter.key().trim().toLowerCase();
		for (int i = 0; i < preferredTokens.size(); i++) {
			String token = preferredTokens.get(i);
			if (preferValueAttr && value.equals(token)) {
				return i;
			}
			if (text.equals(token) || key.equals(token) || text.contains(token) || key.contains(token)) {
				return i;
			}
		}
		return preferredTokens.size() + 50;
	}

	private String buildComboKey(String dateKey, String constitutionKey, String applicationKey) {
		if (applicationKey == null || applicationKey.isBlank()) {
			return dateKey + "/" + constitutionKey;
		}
		return dateKey + "/" + constitutionKey + "/" + applicationKey;
	}

	private void logActiveFilterCombination(String combo) {
		DashboardCountStore.recordCombination(activeDateFilter, storeConstitutionKey());
		String step = "Active filter combination";
		Hooks.logStep(step, combo, "Collecting summary, sub-items, formulas", true);
		ExtentReportManager.logInfo(step + ": " + combo);
		logger.info("=== Dashboard filter: {} ===", combo);
	}

	public void collectCountsForActiveFilters() {
		// Wait until card totals catch up with sub-cards after filter change (avoids stale 572 vs 1134)
		waitUntilPrimaryCardMatchesSubCards();

		collectAndStoreSummaryCardCounts();
		Map<String, Integer> subItemCounts = collectAndStoreSubItemCounts();

		// Fresh card totals for formula (UI may finish refreshing while sub-cards/modals ran)
		Map<String, Integer> summaryCounts = refreshSummaryCardCountsForFormula();

		verifySummaryCardFormulas(summaryCounts, subItemCounts);
		verifyOverallMismatchFormula(summaryCounts);
	}

	/** Re-reads summary h4 values for formula check without duplicating every Excel summary row. */
	private Map<String, Integer> refreshSummaryCardCountsForFormula() {
		Map<String, Integer> summaryCounts = new LinkedHashMap<>();
		String filter = activeDateFilter + "/" + storeConstitutionKey();
		List<String> parts = new ArrayList<>();
		for (DashboardPage.SummaryCard card : DashboardPage.SUMMARY_CARDS) {
			int count = PomElementManager.getCountValue(card.totalLocator(), waitSeconds);
			summaryCounts.put(card.key(), count);
			String storeKey = DashboardCountStore.buildKey(activeDateFilter, storeConstitutionKey(), card.key());
			DashboardCountStore.put(storeKey, count);
			parts.add(card.uiTitle() + "=" + count);
		}
		Hooks.logStep("Re-read summary cards before formula | filter=" + filter,
				"Fresh summary totals for formula compare",
				String.join(" | ", parts), true);
		return summaryCounts;
	}

	/** Saves title-based UI data to Current Data JSON + Dashboard Excel for active filters. */
	public void saveDashboardDataForActiveFilters() {
		String constitutionKey = storeConstitutionKey();
		DashboardCountStore.recordCombination(activeDateFilter, constitutionKey);
		List<DashboardCountStore.DashboardDataRow> rows = DashboardCountStore.getDataRowsForCombination(
				activeDateFilter, constitutionKey);
		CurrentDataManager.saveDashboardSnapshot(activeDateFilter, constitutionKey, activeFromDate, activeToDate,
				rows);
		DashboardDataExcelReport.getInstance().appendRows(rows);
		ExtentReportManager.logInfo("Dashboard UI data saved | filter=" + activeDateFilter + "/" + constitutionKey
				+ " | rows=" + rows.size() + " | folder=" + CurrentDataManager.getRunFolderPath());
		logger.info("Saved {} dashboard rows for {}/{} to Current Data + Excel", rows.size(), activeDateFilter,
				constitutionKey);
	}

	public void prepareDashboardDataCollection() {
		if (dataCollectionInitialized.compareAndSet(false, true)) {
			DashboardCountStore.clear();
			logger.info("Dashboard data collection initialized (store cleared once per test run)");
		}
	}

	public static void resetDataCollectionFlag() {
		dataCollectionInitialized.set(false);
	}

	public void selectDateRangeFilter(String filterValue) {
		if (discoveredDateFilters.isEmpty()) {
			discoveredDateFilters = DashboardFilterDiscovery.resolveDateFiltersOrDefault(getDriver());
		}
		DiscoveredFilter match = DashboardFilterDiscovery.findByKeyOrText(discoveredDateFilters, filterValue);
		if (match == null) {
			throw new AssertionError("Date filter '" + filterValue + "' is not available on this client UI. Available: "
					+ describeFilters(discoveredDateFilters));
		}
		selectDateRangeFilter(match);
	}

	public void selectDateRangeFilter(DiscoveredFilter filter) {
		activeDateFilterObj = filter;
		if (filter.synthetic()) {
			activeDateFilter = filter.key();
			logger.info("Date filter default applied (no UI control): {}", filter.displayText());
			try {
				activeFromDate = PomElementManager.getAttribute(DashboardPage.FROM_DATE_INPUT, "value", waitSeconds);
				activeToDate = PomElementManager.getAttribute(DashboardPage.TO_DATE_INPUT, "value", waitSeconds);
			} catch (Exception e) {
				activeFromDate = "";
				activeToDate = "";
			}
			return;
		}
		By locator = resolveDateLocator(filter);
		if (!PomElementManager.isSelectedToggle(locator, waitSeconds)) {
			PomElementManager.click(locator, waitSeconds);
			waitForDashboardDataRefresh();
		}
		activeDateFilter = filter.key();
		activeFromDate = PomElementManager.getAttribute(DashboardPage.FROM_DATE_INPUT, "value", waitSeconds);
		activeToDate = PomElementManager.getAttribute(DashboardPage.TO_DATE_INPUT, "value", waitSeconds);
		logger.info("Date filter '{}' selected (UI: {}) — From: {} | To: {}", filter.key(), filter.displayText(),
				activeFromDate, activeToDate);
	}

	public void selectConstitutionFilter(String filterValue) {
		if (discoveredConstitutionFilters.isEmpty()) {
			discoveredConstitutionFilters = DashboardFilterDiscovery.resolveConstitutionFiltersOrDefault(getDriver());
		}
		DiscoveredFilter match = DashboardFilterDiscovery.findByKeyOrText(discoveredConstitutionFilters, filterValue);
		if (match == null) {
			// Requested filter not on UI — fall back to default Individual (do not fail)
			logger.warn("Constitution '{}' not on UI — using default Individual", filterValue);
			match = new DiscoveredFilter("Individual", "Individual", "1", true);
		}
		selectConstitutionFilter(match);
	}

	public void selectConstitutionFilter(DiscoveredFilter filter) {
		activeConstitutionFilterObj = filter;
		if (filter.synthetic()) {
			activeConstitutionFilter = filter.key();
			ExtentReportManager.logInfo(
					"Constitution filter UI not available — continuing with default: " + filter.displayText());
			logger.info("Constitution default applied (no UI control): {}", filter.displayText());
			return;
		}
		By locator = resolveConstitutionLocator(filter);
		if (locator == null || !PomElementManager.isPresent(locator)) {
			activeConstitutionFilter = filter.key();
			logger.warn("Constitution button not clickable for '{}' — continuing with key={}", filter.displayText(),
					filter.key());
			ExtentReportManager.logInfo("Constitution '" + filter.displayText()
					+ "' not clickable — continuing with default/logical value: " + filter.key());
			return;
		}
		if (!PomElementManager.isSelectedToggle(locator, waitSeconds)) {
			PomElementManager.click(locator, waitSeconds);
			waitForDashboardDataRefresh();
		}
		activeConstitutionFilter = filter.key();
		logger.info("Constitution filter '{}' selected (UI label: {}, value={})", filter.key(), filter.displayText(),
				filter.valueAttr());
	}

	public void selectApplicationTypeFilter(DiscoveredFilter filter) {
		activeApplicationFilterObj = filter;
		By locator = resolveApplicationLocator(filter);
		if (locator == null || !PomElementManager.isPresent(locator)) {
			logger.warn("Application type '{}' not found — skipping", filter.displayText());
			activeApplicationFilterObj = null;
			return;
		}
		if (!PomElementManager.isSelectedToggle(locator, waitSeconds)) {
			PomElementManager.click(locator, waitSeconds);
			waitForDashboardDataRefresh();
		}
		activeApplicationFilter = filter.key();
		logger.info("Application type '{}' selected (value={})", filter.displayText(), filter.valueAttr());
	}

	private String storeConstitutionKey() {
		if (activeApplicationFilter == null || activeApplicationFilter.isBlank()) {
			return activeConstitutionFilter;
		}
		return activeConstitutionFilter + "_" + activeApplicationFilter;
	}

	private String describeFilters(List<DiscoveredFilter> filters) {
		List<String> parts = new ArrayList<>();
		for (DiscoveredFilter f : filters) {
			String extra = f.valueAttr().isEmpty() ? "" : " [value=" + f.valueAttr() + "]";
			if (f.synthetic()) {
				extra += " [default]";
			}
			parts.add(f.displayText() + extra);
		}
		return parts.toString();
	}

	public void clickRefreshDashboard() {
		PomElementManager.click(DashboardPage.REFRESH_BUTTON, waitSeconds);
		waitForDashboardDataRefresh();
		logger.info("Dashboard refreshed manually");
	}

	public Map<String, Integer> collectAndStoreSummaryCardCounts() {
		Map<String, Integer> summaryCounts = new LinkedHashMap<>();
		for (DashboardPage.SummaryCard card : DashboardPage.SUMMARY_CARDS) {
			int count = readAndStoreSummary(card);
			summaryCounts.put(card.key(), count);
		}
		return summaryCounts;
	}

	public Map<String, Integer> collectAndStoreSubItemCounts() {
		Map<String, Integer> subItemCounts = new LinkedHashMap<>();
		for (DashboardPage.DashboardSubItem item : DashboardPage.ALL_SUB_ITEMS) {
			int count = readAndStoreSubItem(item);
			subItemCounts.put(item.code(), count);
			if (DashboardPage.isCountOnlyNavigationItem(item.code())) {
				logCountOnlySkip(item, count);
			} else if (count > 0) {
				openDetailModalIfPresent(item, count);
			} else {
				String filter = activeDateFilter + "/" + storeConstitutionKey();
				String scenario = "Sub-card: " + item.code() + " " + item.ariaLabel()
						+ " | under " + item.cardGroup() + " | filter=" + filter;
				String step = "Sub-card click skipped | " + item.ariaLabel() + " (" + item.code() + ")"
						+ " | under " + item.cardGroup() + " | filter=" + filter;
				Hooks.logStepWithScenario(scenario, step, "Click report only when sub-card count > 0",
						"Sub-card name: " + item.ariaLabel() + " | dashboard count = 0 — no report open", true);
			}
		}
		return subItemCounts;
	}

	public void verifySummaryCardFormulas(Map<String, Integer> summaryCounts, Map<String, Integer> subItemCounts) {
		for (DashboardPage.SummaryCard card : DashboardPage.SUMMARY_CARDS) {
			String[] codes = card.formulaLabel().replace("+", " ").split("\\s+");
			String breakdown = buildSubCardBreakdown(codes, subItemCounts);
			int subSum = sum(subItemCounts, codes);
			verifyFormula(card.uiTitle() + " (" + card.formulaLabel() + ")", summaryCounts.get(card.key()), subSum,
					breakdown);
		}
	}

	/** e.g. A1 Success=0 + A2 Failed=0 + A3 In-Progress=1134 + A4 Duplicate=0 */
	private String buildSubCardBreakdown(String[] codes, Map<String, Integer> subItemCounts) {
		List<String> parts = new ArrayList<>();
		for (String code : codes) {
			String title = code;
			for (DashboardPage.DashboardSubItem item : DashboardPage.ALL_SUB_ITEMS) {
				if (item.code().equalsIgnoreCase(code)) {
					title = item.code() + " " + item.ariaLabel();
					break;
				}
			}
			parts.add(title + "=" + subItemCounts.getOrDefault(code, 0));
		}
		return String.join(" + ", parts);
	}

	/**
	 * Overall dashboard rule: A − (B+C+D+E+F) = mismatch delta shown in the banner.
	 * G (Reverse Update to CBS) is excluded from this cross-card check.
	 */
	public void verifyOverallMismatchFormula(Map<String, Integer> summaryCounts) {
		int a = summaryCounts.getOrDefault("DATA_PUSHED_FI", 0);
		int b = summaryCounts.getOrDefault("IFLOW_IN_PROGRESS", 0);
		int c = summaryCounts.getOrDefault("IFLOW_REJECTIONS", 0);
		int d = summaryCounts.getOrDefault("CERSAI_IN_PROGRESS", 0);
		int e = summaryCounts.getOrDefault("CERSAI_REJECTIONS", 0);
		int f = summaryCounts.getOrDefault("CERSAI_SUCCESS", 0);
		int downstreamSum = b + c + d + e + f;
		int calculatedMismatch = a - downstreamSum;

		String storeKey = DashboardCountStore.buildKey(activeDateFilter, storeConstitutionKey(), "MISMATCH_DELTA");
		DashboardCountStore.put(storeKey, calculatedMismatch);
		recordDataRow("MISMATCH", "Overall Dashboard", "A-(B+C+D+E+F)", "Count mismatch delta", calculatedMismatch,
				null);

		String filterLabel = activeDateFilter + "/" + storeConstitutionKey();
		String breakdown = "A=" + a + "(DataPushedFI), B=" + b + "(iFlowIP), C=" + c + "(iFlowRej), D=" + d
				+ "(CERSAI_IP), E=" + e + "(CERSAI_Rej), F=" + f + "(CERSAI_OK)";
		String step = "Overall formula | A1-(B+C+D+E+F) | filter=" + filterLabel;
		String detail = breakdown + " | calculated=" + calculatedMismatch;

		// Read the UI formula text: "A1 − (B+C+D+E+F) = 27"
		Integer uiFormulaValue = readUiFormulaValue();
		if (uiFormulaValue != null) {
			detail += " | UI formula text value=" + uiFormulaValue;
		}

		Integer uiMismatch = readDisplayedMismatchDelta();
		detail += " | uiBanner=" + (uiMismatch == null ? "not shown" : uiMismatch);

		boolean pass;
		String actualResult;

		if (calculatedMismatch == 0) {
			pass = true;
			actualResult = "PASS | No mismatch (delta=0). " + detail;
			if (uiFormulaValue != null && uiFormulaValue != 0) {
				pass = false;
				actualResult = "FAIL | Calculated=0 but UI formula shows " + uiFormulaValue + ". " + detail;
			}
		} else {
			pass = false;
			actualResult = "FAIL | Mismatch detected: A1-(B+C+D+E+F) = " + calculatedMismatch + ". " + detail;
			if (uiFormulaValue != null && uiFormulaValue != calculatedMismatch) {
				actualResult += " | BUG: UI formula value " + uiFormulaValue + " != calculated " + calculatedMismatch;
			}
		}

		Hooks.logStep(step, "A1-(B+C+D+E+F) should equal 0 (no mismatch)", actualResult, pass);
		ExtentReportManager.logInfo(step + " | " + actualResult);
		logger.info("{} | {}", step, actualResult);

		if (uiMismatch != null) {
			String bannerStep = "UI mismatch banner | filter=" + filterLabel;
			boolean bannerMatch = uiMismatch == Math.abs(calculatedMismatch);
			Hooks.logStep(bannerStep,
					"UI banner should match |calculated|=" + Math.abs(calculatedMismatch),
					"UI banner=" + uiMismatch + " | calculated=" + calculatedMismatch,
					bannerMatch);
			if (!bannerMatch) {
				SoftAssertManager.recordFailure(bannerStep,
						new AssertionError(bannerStep + " — UI banner " + uiMismatch + " != |" + calculatedMismatch + "|"));
			}
		}

		logCardLevelMismatchBadge(calculatedMismatch);
	}

	/**
	 * MCP-verified: reads "A1 − (B+C+D+E+F) = 27" from the Typography.caption span
	 * next to the Search button. Returns the integer value after '=', or null if not found.
	 */
	private Integer readUiFormulaValue() {
		try {
			List<WebElement> elements = getDriver().findElements(DashboardPage.FORMULA_TEXT);
			if (elements.isEmpty()) {
				logger.debug("UI formula text element not found");
				return null;
			}
			String text = elements.get(0).getText();
			if (text == null || text.isBlank()) {
				return null;
			}
			logger.info("UI formula text: '{}'", text);
			Matcher m = Pattern.compile("=\\s*([+-]?\\d+)").matcher(text);
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
			return null;
		} catch (Exception ex) {
			logger.debug("Could not read UI formula text: {}", ex.getMessage());
			return null;
		}
	}

	public String getActiveDateFilter() {
		return activeDateFilter;
	}

	public String getActiveConstitutionFilter() {
		return activeConstitutionFilter;
	}

	private int readAndStoreSummary(DashboardPage.SummaryCard card) {
		int count = PomElementManager.getCountValue(card.totalLocator(), waitSeconds);
		String storeKey = DashboardCountStore.buildKey(activeDateFilter, storeConstitutionKey(), card.key());
		DashboardCountStore.put(storeKey, count);
		recordDataRow("SUMMARY", card.uiTitle(), card.key(), card.uiTitle(), count, card.formulaLabel());

		String filter = activeDateFilter + "/" + storeConstitutionKey();
		String scenario = "Summary card: " + card.uiTitle() + " | filter=" + filter;
		String step = "Summary card | " + card.uiTitle() + " | formula " + card.formulaLabel()
				+ " | filter=" + filter;
		Hooks.logStepWithScenario(scenario, step, "Summary card count >= 0",
				"Summary card (" + card.uiTitle() + ") count = " + count, true);
		ExtentReportManager.logInfo(step + " = " + count);
		logger.info("{} = {}", step, count);
		return count;
	}

	private int readAndStoreSubItem(DashboardPage.DashboardSubItem item) {
		By countLocator = DashboardPage.subItemCount(item.code(), item.ariaLabel());
		String filter = activeDateFilter + "/" + storeConstitutionKey();
		String scenario = "Sub-card: " + item.code() + " " + item.ariaLabel()
				+ " | under " + item.cardGroup() + " | filter=" + filter;
		String step = "Sub-card count | " + item.ariaLabel() + " (" + item.code() + ")"
				+ " | under Summary card: " + item.cardGroup()
				+ " | filter=" + filter;

		// Soft-skip missing tiles (e.g. B5 Image Compression Queue not rendered for this env).
		if (!PomElementManager.isPresent(countLocator)) {
			int count = 0;
			String storeKey = DashboardCountStore.buildKey(activeDateFilter, storeConstitutionKey(), item.code());
			DashboardCountStore.put(storeKey, count);
			recordDataRow("SUB_ITEM", item.cardGroup(), item.code(), item.ariaLabel(), count, null);
			String actual = "Sub-card name: " + item.ariaLabel() + " | code: " + item.code()
					+ " | not present on dashboard — stored count = 0 (soft skip)";
			Hooks.logStepWithScenario(scenario, step + " | soft-skip missing",
					"Sub-card visible or soft-skip when absent", actual, true);
			ExtentReportManager.logInfo(step + " | soft-skip missing = 0");
			logger.warn("{} | soft-skip missing element — stored 0", step);
			return count;
		}

		int count = PomElementManager.getCountValue(countLocator, waitSeconds);
		String storeKey = DashboardCountStore.buildKey(activeDateFilter, storeConstitutionKey(), item.code());
		DashboardCountStore.put(storeKey, count);
		recordDataRow("SUB_ITEM", item.cardGroup(), item.code(), item.ariaLabel(), count, null);

		Hooks.logStepWithScenario(scenario, step, "Sub-card dashboard count >= 0",
				"Sub-card name: " + item.ariaLabel() + " | code: " + item.code() + " | dashboard count = " + count,
				true);
		ExtentReportManager.logInfo(step + " = " + count);
		logger.info("{} = {}", step, count);
		return count;
	}

	private void recordDataRow(String section, String cardGroup, String code, String title, int count, String formula) {
		DashboardCountStore.recordDataRow(new DashboardCountStore.DashboardDataRow(
				activeDateFilter,
				storeConstitutionKey(),
				activeFromDate,
				activeToDate,
				section,
				cardGroup,
				code,
				title,
				count,
				formula,
				DashboardDataExcelReport.nowTimestamp()));
	}

	private Integer readDisplayedMismatchDelta() {
		if (!PomElementManager.isPresent(DashboardPage.OVERALL_MISMATCH_BANNER)) {
			return null;
		}
		try {
			WebElement banner = getDriver().findElement(DashboardPage.OVERALL_MISMATCH_BANNER);
			String ariaLabel = banner.getAttribute("aria-label");
			if (ariaLabel != null) {
				Matcher ariaMatcher = MISMATCH_ARIA_PATTERN.matcher(ariaLabel);
				if (ariaMatcher.find()) {
					return Integer.parseInt(ariaMatcher.group(1));
				}
			}
			String text = banner.getText();
			Matcher textMatcher = MISMATCH_TEXT_PATTERN.matcher(text);
			if (textMatcher.find()) {
				return Math.abs(PomElementManager.parseCountText(textMatcher.group(1), "mismatch banner text"));
			}
		} catch (Exception e) {
			logger.warn("Could not read mismatch banner: {}", e.getMessage());
		}
		return null;
	}

	private void logCardLevelMismatchBadge(int calculatedMismatch) {
		if (!PomElementManager.isPresent(DashboardPage.DATA_PUSHED_FI_MISMATCH_BADGE)) {
			return;
		}
		try {
			String badgeText = PomElementManager.getText(DashboardPage.DATA_PUSHED_FI_MISMATCH_BADGE, waitSeconds);
			int badgeValue = PomElementManager.parseCountText(badgeText, "FI card mismatch badge");
			String step = "FI card mismatch badge | filter=" + activeDateFilter + "/" + activeConstitutionFilter;
			String expected = String.valueOf(Math.abs(calculatedMismatch));
			boolean passed = badgeValue == Math.abs(calculatedMismatch);
			Hooks.logStep(step, "Badge shows |A-(B+C+D+E+F)| (" + expected + ")", String.valueOf(badgeValue), passed);
			if (!passed) {
				Assert.fail(step + " — badge " + badgeValue + " != expected " + expected);
			}
		} catch (Exception e) {
			logger.debug("FI card mismatch badge check skipped: {}", e.getMessage());
		}
	}

	private void logCountOnlySkip(DashboardPage.DashboardSubItem item, int count) {
		String filter = activeDateFilter + "/" + storeConstitutionKey();
		String scenario = "Sub-card: " + item.code() + " " + item.ariaLabel()
				+ " | under " + item.cardGroup() + " | filter=" + filter;
		String step = "Sub-card click skipped | " + item.ariaLabel() + " (" + item.code() + ")"
				+ " | under " + item.cardGroup()
				+ " | navigates to another page | filter=" + filter;
		String actual = "Sub-card name: " + item.ariaLabel() + " | dashboard count = " + count
				+ " — no report-page compare (navigation item)";
		Hooks.logStepWithScenario(scenario, step, "Read sub-card count only (no click)", actual, true);
		ExtentReportManager.logInfo(step + " | " + actual);
		logger.info("{} | {}", step, actual);
	}

	/**
	 * Clicks sub-card (Success / Failed / In-Progress / Duplicate / …), opens report modal,
	 * compares dashboard count vs modal badge "N records" first; table row xpath is fallback only.
	 */
	private void openDetailModalIfPresent(DashboardPage.DashboardSubItem item, int dashboardCount) {
		String filter = activeDateFilter + "/" + storeConstitutionKey();
		String subCardName = item.ariaLabel();
		String subCard = item.code() + " " + subCardName;
		String scenario = "Sub-card: " + subCard + " | under " + item.cardGroup() + " | filter=" + filter;
		String step = "Sub-card report compare | " + subCardName + " (" + item.code() + ")"
				+ " | under Summary card: " + item.cardGroup()
				+ " | filter=" + filter;
		try {
			PomElementManager.click(DashboardPage.subItemRow(item.code(), item.ariaLabel()), waitSeconds);
			waitForPageFullyLoaded();
			PomElementManager.findVisible(DashboardPage.DETAIL_MODAL, waitSeconds);

			ModalCountResult report = readModalRecordCount(dashboardCount);
			boolean matched = report.count() == dashboardCount;
			String expected = "Sub-card '" + subCardName + "' report count = dashboard count (" + dashboardCount + ")";
			String actual = "Sub-card name: " + subCardName
					+ " | code: " + item.code()
					+ " | under: " + item.cardGroup()
					+ " | Dashboard count = " + dashboardCount
					+ " | Report count = " + report.count()
					+ " | Source: " + report.source()
					+ " | Compare: " + (matched ? "MATCH" : "MISMATCH");

			Hooks.logStepWithScenario(scenario, step, expected, actual, matched);
			ExtentReportManager.logInfo(step + " | " + actual);
			logger.info("{} | {}", step, actual);

			if (!matched) {
				SoftAssertManager.recordFailure(step,
						new AssertionError(expected + " but report showed " + report.count()
								+ " via " + report.source()));
			}

			closeDetailModalIfOpen();
			ensureActiveFiltersAfterModal();
		} catch (Exception e) {
			String error = e.getMessage() != null ? e.getMessage() : e.toString();
			Hooks.logStepWithScenario(scenario, step,
					"Open report for sub-card " + subCardName + " and compare count " + dashboardCount,
					"Failed: " + error, false);
			logger.warn("Could not open/compare report for sub-card {} {}: {}", item.code(), item.ariaLabel(), error);
			SoftAssertManager.recordFailure(step, e);
			ensureActiveFiltersAfterModal();
		}
	}

	private record ModalCountResult(int count, String source) {
	}

	/**
	 * 1) Prefer popup badge text like "1134 records"
	 * 2) Else fallback: count table rows by xpath (may be wrong if paginated — share table HTML if needed)
	 */
	private ModalCountResult readModalRecordCount(int dashboardCountHint) {
		long deadline = System.currentTimeMillis() + (ConfigReader.getLoginWait() * 1000L);
		Integer badgeCount = null;
		while (System.currentTimeMillis() < deadline) {
			try {
				for (WebElement el : getDriver().findElements(DashboardPage.DETAIL_MODAL_RECORD_COUNT)) {
					if (!el.isDisplayed()) {
						continue;
					}
					String text = el.getText();
					if (text != null && text.toLowerCase().contains("record")) {
						int parsed = PomElementManager.parseCountText(text, "modal records badge");
						if (parsed > 0 || dashboardCountHint == 0) {
							badgeCount = parsed;
							if (parsed == dashboardCountHint || parsed > 0) {
								return new ModalCountResult(parsed, "popup badge ('N records')");
							}
						}
					}
				}
			} catch (Exception ignored) {
				// keep polling
			}
			if (badgeCount != null) {
				return new ModalCountResult(badgeCount, "popup badge ('N records')");
			}
			int rows = 0;
			for (By locator : DashboardPage.DETAIL_MODAL_ROW_LOCATORS) {
				rows = Math.max(rows, getDriver().findElements(locator).size());
			}
			if (rows > 0 && rows == dashboardCountHint) {
				return new ModalCountResult(rows, "table row xpath (fallback)");
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		if (badgeCount != null) {
			return new ModalCountResult(badgeCount, "popup badge ('N records')");
		}
		int rows = waitAndCountModalRows();
		return new ModalCountResult(rows, "table row xpath (fallback)");
	}

	private int waitAndCountModalRows() {
		int timeoutMs = ConfigReader.getLoginWait() * 1000;
		long deadline = System.currentTimeMillis() + timeoutMs;
		int lastCount = 0;
		while (System.currentTimeMillis() < deadline) {
			for (By locator : DashboardPage.DETAIL_MODAL_ROW_LOCATORS) {
				lastCount = getDriver().findElements(locator).size();
				if (lastCount > 0) {
					return lastCount;
				}
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return lastCount;
	}

	/**
	 * After closing a detail modal, stay on the SAME filter combo.
	 * Only re-clicks a toggle if UI deselected it (avoids unnecessary refresh / jumping to ALL).
	 */
	private void ensureActiveFiltersAfterModal() {
		String combo = buildComboKey(activeDateFilter, activeConstitutionFilter,
				activeApplicationFilter == null || activeApplicationFilter.isBlank() ? null : activeApplicationFilter);
		try {
			assertActiveFiltersStillSelected(combo);
		} catch (AssertionError e) {
			logger.warn("Filters drifted after modal — restoring combo {}: {}", combo, e.getMessage());
			if (activeDateFilterObj != null) {
				selectDateRangeFilter(activeDateFilterObj);
			} else {
				selectDateRangeFilter(activeDateFilter);
			}
			if (activeConstitutionFilterObj != null) {
				selectConstitutionFilter(activeConstitutionFilterObj);
			} else {
				selectConstitutionFilter(activeConstitutionFilter);
			}
			if (activeApplicationFilterObj != null) {
				selectApplicationTypeFilter(activeApplicationFilterObj);
			}
			assertActiveFiltersStillSelected(combo);
		}
	}

	/**
	 * Confirms dashboard is still on the intended date / constitution / application filters
	 * (aria-pressed=true) before collecting or continuing. Does not click Refresh.
	 */
	private void assertActiveFiltersStillSelected(String comboKey) {
		List<String> problems = new ArrayList<>();

		if (activeDateFilterObj != null && !activeDateFilterObj.synthetic()) {
			By dateLoc = resolveDateLocator(activeDateFilterObj);
			if (!PomElementManager.isSelectedToggle(dateLoc, waitSeconds)) {
				problems.add("Date '" + activeDateFilterObj.displayText() + "' is not selected");
			}
		}
		if (activeConstitutionFilterObj != null && !activeConstitutionFilterObj.synthetic()) {
			By constLoc = resolveConstitutionLocator(activeConstitutionFilterObj);
			if (constLoc != null && PomElementManager.isPresent(constLoc)
					&& !PomElementManager.isSelectedToggle(constLoc, waitSeconds)) {
				problems.add("Constitution '" + activeConstitutionFilterObj.displayText() + "' is not selected");
			}
		}
		if (activeApplicationFilterObj != null) {
			By appLoc = resolveApplicationLocator(activeApplicationFilterObj);
			if (appLoc != null && PomElementManager.isPresent(appLoc)
					&& !PomElementManager.isSelectedToggle(appLoc, waitSeconds)) {
				problems.add("Application '" + activeApplicationFilterObj.displayText() + "' is not selected");
			}
		}

		if (!problems.isEmpty()) {
			throw new AssertionError("Not on expected filter combo [" + comboKey + "]: " + problems
					+ " — counts would be wrong if we continued");
		}

		PomElementManager.findVisible(DashboardPage.DATE_FILTER_GROUP, waitSeconds);
		Hooks.logStep("Verify still on filter combo", comboKey, "UI toggles still selected for " + comboKey, true);
		logger.info("Confirmed still on filter combo: {}", comboKey);
	}

	private By resolveDateLocator(DiscoveredFilter filter) {
		By locator = filter.locator(DashboardPage.DATE_FILTER_BUTTONS);
		if (!PomElementManager.isPresent(locator) && filter.valueAttr() != null && !filter.valueAttr().isBlank()) {
			locator = DashboardPage.dateFilterButton(filter.valueAttr());
		}
		return locator;
	}

	private By resolveConstitutionLocator(DiscoveredFilter filter) {
		By locator = filter.locator(DashboardPage.CONSTITUTION_FILTER_BUTTONS);
		if (!PomElementManager.isPresent(locator)) {
			locator = DashboardPage.constitutionFilterButton(filter.valueAttr());
		}
		if (!PomElementManager.isPresent(locator)) {
			locator = DashboardPage.constitutionFilterButton(filter.displayText());
		}
		return PomElementManager.isPresent(locator) ? locator : null;
	}

	private By resolveApplicationLocator(DiscoveredFilter filter) {
		By locator = filter.locator(DashboardPage.APPLICATION_TYPE_FILTER_BUTTONS);
		if (!PomElementManager.isPresent(locator) && filter.valueAttr() != null && !filter.valueAttr().isBlank()) {
			locator = By.xpath("//div[@aria-label='Application type filter']//button[@value='" + filter.valueAttr()
					+ "']");
		}
		return PomElementManager.isPresent(locator) ? locator : null;
	}

	private void closeDetailModalIfOpen() {
		try {
			if (getDriver().findElements(DashboardPage.DETAIL_MODAL).isEmpty()) {
				return;
			}
			List<By> closeCandidates = List.of(DashboardPage.DETAIL_MODAL_CLOSE,
					By.xpath("//div[@role='dialog']//button[.//*[@data-testid='CloseIcon']]"),
					By.xpath("//div[@role='dialog']//button[@aria-label='Close']"));
			for (By closeBtn : closeCandidates) {
				if (!getDriver().findElements(closeBtn).isEmpty()) {
					PomElementManager.click(closeBtn, waitSeconds);
					waitUntilInvisible(DashboardPage.DETAIL_MODAL, waitSeconds);
					return;
				}
			}
			getDriver().switchTo().activeElement().sendKeys(org.openqa.selenium.Keys.ESCAPE);
			waitUntilInvisible(DashboardPage.DETAIL_MODAL, 3);
		} catch (Exception e) {
			logger.debug("Modal close skipped: {}", e.getMessage());
		}
	}

	private void verifyFormula(String label, Integer cardTotal, int subItemSum, String subCardBreakdown) {
		int total = cardTotal == null ? 0 : cardTotal;
		String filter = activeDateFilter + "/" + storeConstitutionKey();
		String scenario = "Formula: " + label + " | sub-cards: " + subCardBreakdown + " | filter=" + filter;
		String step = "Formula check | " + label + " | filter=" + filter
				+ " | sub-cards: " + subCardBreakdown;
		String expected = "Summary card total = sub-card sum (" + subItemSum + ") | " + subCardBreakdown;
		String actual = "Summary card total = " + total + " | sub-cards: " + subCardBreakdown;
		try {
			Assert.assertEquals(total, subItemSum,
					label + " — card total " + total + " != sub-item sum " + subItemSum
							+ " [" + subCardBreakdown + "]");
			Hooks.logStepWithScenario(scenario, step, expected, actual, true);
		} catch (AssertionError e) {
			Hooks.logStepWithScenario(scenario, step, expected, actual, false);
			throw e;
		}
	}

	private int sum(Map<String, Integer> counts, String... codes) {
		int sum = 0;
		for (String code : codes) {
			sum += counts.getOrDefault(code, 0);
		}
		return sum;
	}

	private void waitForDashboardDataRefresh() {
		waitForLoaderToDisappear();
		int settleMs = ConfigReader.getFilterRefreshWaitMs();
		if (settleMs > 0) {
			try {
				Thread.sleep(settleMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * After filter change, summary h4 can lag behind sub-cards (e.g. card still 572 while A3 already 1134).
	 * Poll until Data pushed from FI card == A1+A2+A3+A4, or values stop changing.
	 */
	private void waitUntilPrimaryCardMatchesSubCards() {
		waitForDashboardDataRefresh();
		long deadline = System.currentTimeMillis() + Math.max(ConfigReader.getExplicitWait(), 8) * 1000L;
		Integer lastCard = null;
		Integer lastSum = null;
		int stableRounds = 0;

		while (System.currentTimeMillis() < deadline) {
			try {
				int card = PomElementManager.getCountValue(DashboardPage.DATA_PUSHED_FI_TOTAL, waitSeconds);
				int sum = 0;
				for (DashboardPage.DashboardSubItem item : DashboardPage.ALL_SUB_ITEMS) {
					if (item.code().startsWith("A")) {
						sum += PomElementManager.getCountValue(
								DashboardPage.subItemCount(item.code(), item.ariaLabel()), waitSeconds);
					}
				}
				if (card == sum) {
					logger.info("Dashboard counts consistent after filter — FI card={} equals A1..A4 sum={}", card, sum);
					// small extra settle so other cards catch up too
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return;
				}
				if (lastCard != null && lastCard == card && lastSum != null && lastSum == sum) {
					stableRounds++;
				} else {
					stableRounds = 0;
				}
				lastCard = card;
				lastSum = sum;
				if (stableRounds >= 4) {
					logger.warn("Counts stable but FI card {} != A1..A4 sum {} — continuing (will formula-check)",
							card, sum);
					return;
				}
			} catch (Exception e) {
				logger.debug("Count consistency wait: {}", e.getMessage());
			}
			try {
				Thread.sleep(350);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		logger.warn("Timed out waiting for FI card to match A1..A4 — continuing with re-read before formula");
	}
}
