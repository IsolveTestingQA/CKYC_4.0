/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.dashboard;

import Com.Ckyc_4_0.stepdefinitions.common.Hooks;

import Com.Ckyc_4_0.functionality.dashboard.DashboardFunctionality;
import Com.Ckyc_4_0.functionality.login.LoginFunctionality;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.DashboardCountStore;
import Com.Ckyc_4_0.UtilityFiles.DashboardDataExcelReport;
import Com.Ckyc_4_0.UtilityFiles.ExtentReportManager;
import Com.Ckyc_4_0.utils.DashboardFilterDiscovery.DiscoveredFilter;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;

import static Com.Ckyc_4_0.UtilityFiles.BaseClass.getDriver;

public class DashboardStepDefinitions {

	private final LoginFunctionality loginFunctionality = new LoginFunctionality();
	private final DashboardFunctionality dashboardFunctionality = new DashboardFunctionality();

	private Map<String, Integer> lastSummaryCounts;
	private Map<String, Integer> lastSubItemCounts;

	@Given("user is logged in and on CKYC dashboard")
	public void userIsLoggedInAndOnCkycDashboard() {
		String dashboardPath = ConfigReader.getDashboardUrlPath();
		String currentUrl = getDriver() != null ? getDriver().getCurrentUrl() : "";
		// Already authenticated on app (dashboard or any /ckyc/* page e.g. location/masters) — never re-login
		boolean alreadyInApp = currentUrl != null
				&& !currentUrl.toLowerCase().contains("/login")
				&& (currentUrl.contains(dashboardPath) || currentUrl.contains("/ckyc/")
						|| currentUrl.contains("/admin/"));
		if (alreadyInApp) {
			Hooks.runStep("Resume session (skip re-login)", "Dashboard filter bar visible", () -> {
				if (!currentUrl.contains(dashboardPath)) {
					Com.Ckyc_4_0.utils.MasterUiHelper.goToDashboard();
				}
				dashboardFunctionality.verifyDashboardIsDisplayed();
			}, true, true);
			return;
		}
		Hooks.runStep("Navigate to CKYC login page", "Username field visible",
				loginFunctionality::verifyLoginPageIsDisplayed, true, true);
		Hooks.runStep("Select runtime mode", ConfigReader.getRuntimeMode(),
				() -> loginFunctionality.selectRuntimeMode(ConfigReader.getRuntimeMode()), false, true);
		Hooks.runStep("Enter credentials", "Valid username/password from config",
				loginFunctionality::enterCredentialsFromConfig, false, true);
		Hooks.runStep("Click Sign In", "Submit login form", loginFunctionality::clickSignIn, false, true);
		Hooks.runStep("Verify dashboard loaded (Active session / password prompts if shown)",
				"URL contains " + dashboardPath, () -> {
					loginFunctionality.verifyUserLoggedInSuccessfully();
					dashboardFunctionality.verifyDashboardIsDisplayed();
				}, true, true);
	}

	@Given("dashboard data collection is initialized")
	public void dashboardDataCollectionIsInitialized() {
		Hooks.runStep("Initialize dashboard data store", "Store cleared once per test run",
				dashboardFunctionality::prepareDashboardDataCollection);
	}

	@Given("user is on CKYC dashboard")
	public void userIsOnCkycDashboard() {
		Hooks.runStep("Verify dashboard page", "Dashboard filter bar visible", () -> {
			dashboardFunctionality.verifyDashboardIsDisplayed();
		}, true, true);
	}

	@When("user discovers available dashboard filters from UI")
	public void userDiscoversAvailableDashboardFiltersFromUi() {
		Hooks.runStep("Discover date + constitution filters from live UI",
				"Only filters visible for this client are used",
				dashboardFunctionality::discoverAvailableFiltersFromUi, false);
	}

	@When("user selects {string} date range filter on dashboard")
	public void userSelectsDateRangeFilterOnDashboard(String filterValue) {
		Hooks.runStep("Select date range filter (must be available on UI)", filterValue,
				() -> dashboardFunctionality.selectDateRangeFilter(filterValue));
	}

	@When("user selects {string} constitution filter on dashboard")
	public void userSelectsConstitutionFilterOnDashboard(String filterValue) {
		Hooks.runStep("Select constitution filter (must be available on UI)", filterValue,
				() -> dashboardFunctionality.selectConstitutionFilter(filterValue));
	}

	@When("user refreshes dashboard data")
	public void userRefreshesDashboardData() {
		Hooks.runStep("Refresh dashboard", "Dashboard data reloaded",
				dashboardFunctionality::clickRefreshDashboard);
	}

	@Then("user collects and stores dashboard data for all available filter combinations")
	public void userCollectsAndStoresDashboardDataForAllAvailableFilterCombinations() {
		int dates = dashboardFunctionality.getDiscoveredDateFilters().size();
		int constitutions = dashboardFunctionality.getDiscoveredConstitutionFilters().size();
		int apps = Math.max(1, dashboardFunctionality.getDiscoveredApplicationFilters().size());
		// captureOnSuccess=false — avoid multi-second screenshot after long collection
		Hooks.runStep("Collect counts for discovered filters only",
				dates + " date × " + constitutions + " constitution × " + apps + " application = "
						+ (dates * constitutions * apps) + " combinations",
				() -> dashboardFunctionality.collectCountsForAllFilterCombinations(), false);
	}

	@Then("user collects dashboard counts for all filter combinations")
	public void userCollectsDashboardCountsForAllFilterCombinations() {
		Hooks.runStep("Collect counts for all available filters",
				"Discovered from UI at runtime",
				() -> {
					dashboardFunctionality.discoverAvailableFiltersFromUi();
					dashboardFunctionality.collectCountsForAllFilterCombinations();
				});
	}

	@Then("user collects and logs all dashboard summary card counts")
	public void userCollectsAndLogsAllDashboardSummaryCardCounts() {
		Hooks.runStep("Collect summary card counts",
				"All 7 summary cards read by UI title and stored",
				() -> lastSummaryCounts = dashboardFunctionality.collectAndStoreSummaryCardCounts(), true);
	}

	@And("user collects and logs all dashboard sub-item counts")
	public void userCollectsAndLogsAllDashboardSubItemCounts() {
		Hooks.runStep("Collect sub-item counts",
				"All A–G sub-items read by UI title (modal when count > 0)",
				() -> lastSubItemCounts = dashboardFunctionality.collectAndStoreSubItemCounts());
	}

	@Then("user verifies dashboard card totals match sub-item sums")
	public void userVerifiesDashboardCardTotalsMatchSubItemSums() {
		Hooks.runStep("Verify card formulas",
				"Each card total equals sum of its sub-items",
				() -> dashboardFunctionality.verifySummaryCardFormulas(lastSummaryCounts, lastSubItemCounts));
	}

	@And("user saves dashboard UI data for filter {string} and {string}")
	public void userSavesDashboardUiDataForFilter(String dateFilter, String constitutionFilter) {
		Hooks.runStep("Save dashboard UI data to Current Data + Excel",
				"Title-based rows for " + dateFilter + "/" + constitutionFilter,
				() -> dashboardFunctionality.saveDashboardDataForActiveFilters(), true);
	}

	@And("user exports dashboard data report to Excel")
	public void userExportsDashboardDataReportToExcel() {
		Hooks.runStep("Export dashboard data Excel report",
				"All discovered filter combinations in DashboardDataReport sheet",
				() -> {
					DashboardDataExcelReport.getInstance().save();
					ExtentReportManager.logInfo("Dashboard data Excel: "
							+ DashboardDataExcelReport.getInstance().getExcelPath());
				});
	}

	@Then("dashboard counts are stored for filter {string} and {string}")
	public void dashboardCountsAreStoredForFilter(String dateFilter, String constitutionFilter) {
		Hooks.runStep("Confirm counts stored",
				"Counts keyed by " + dateFilter + "/" + constitutionFilter,
				() -> assertCountsStoredForFilter(dateFilter, constitutionFilter));
	}

	@And("dashboard counts are stored for all discovered filter combinations")
	public void dashboardCountsAreStoredForAllDiscoveredFilterCombinations() {
		Hooks.runStep("Confirm discovered filter combinations stored",
				"Count equals discovered date × constitution (× application if present)",
				() -> {
					var dates = dashboardFunctionality.getDiscoveredDateFilters();
					var constitutions = dashboardFunctionality.getDiscoveredConstitutionFilters();
					var applications = dashboardFunctionality.getDiscoveredApplicationFilters();
					int appFactor = Math.max(1, applications.size());
					int expected = dates.size() * constitutions.size() * appFactor;
					int processed = DashboardCountStore.getProcessedCombinationCount();
					if (DashboardCountStore.getAll().isEmpty()) {
						throw new AssertionError("No dashboard counts were stored");
					}
					if (processed < expected) {
						throw new AssertionError("Expected " + expected + " discovered filter combinations but processed "
								+ processed + ". Discovered dates=" + keys(dates) + ", constitutions="
								+ keys(constitutions) + ", applications=" + keys(applications) + ", processed="
								+ DashboardCountStore.getProcessedCombinations());
					}
					ExtentReportManager.logInfo("Stored combinations: " + DashboardCountStore.getProcessedCombinations());
				});
	}

	@And("dashboard counts are stored for all filter combinations")
	public void dashboardCountsAreStoredForAllFilterCombinations() {
		dashboardCountsAreStoredForAllDiscoveredFilterCombinations();
	}

	private void assertCountsStoredForFilter(String dateFilter, String constitutionFilter) {
		String prefix = dateFilter + "_" + constitutionFilter + "_";
		boolean found = DashboardCountStore.getAll().keySet().stream().anyMatch(k -> k.startsWith(prefix));
		if (!found) {
			throw new AssertionError("No counts stored for " + dateFilter + "/" + constitutionFilter);
		}
		if (DashboardCountStore.getDataRowsForCombination(dateFilter, constitutionFilter).isEmpty()) {
			throw new AssertionError("No title-based UI rows stored for " + dateFilter + "/" + constitutionFilter);
		}
	}

	private static String keys(java.util.List<DiscoveredFilter> filters) {
		return filters.stream().map(DiscoveredFilter::key).toList().toString();
	}
}
