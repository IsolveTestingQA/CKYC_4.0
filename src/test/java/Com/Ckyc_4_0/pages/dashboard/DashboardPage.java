/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.dashboard;

import org.openqa.selenium.By;

import java.util.List;
import java.util.Set;

/** Stable XPath locators for CKYC Enterprise Console — Dashboard. */
public final class DashboardPage {

	private DashboardPage() {
	}

	// -------------------------- Date range filters --------------------------
	public static final By FROM_DATE_INPUT = By
			.xpath("//fieldset//legend/span[normalize-space()='From']/ancestor::div[contains(@class,'MuiFormControl')]//input");
	public static final By TO_DATE_INPUT = By
			.xpath("//fieldset//legend/span[normalize-space()='To']/ancestor::div[contains(@class,'MuiFormControl')]//input");
	public static final By FROM_DATE_PICKER_BUTTON = By.xpath("//button[@aria-label='Open From picker']");
	public static final By TO_DATE_PICKER_BUTTON = By.xpath("//button[@aria-label='Open To picker']");

	public static final By DATE_FILTER_TODAY = By
			.xpath("//div[@aria-label='Date range shortcut']//button[@value='today']");
	public static final By DATE_FILTER_WEEKLY = By
			.xpath("//div[@aria-label='Date range shortcut']//button[@value='weekly']");
	public static final By DATE_FILTER_MONTHLY = By
			.xpath("//div[@aria-label='Date range shortcut']//button[@value='monthly']");
	public static final By DATE_FILTER_PENDING_7_DAYS = By
			.xpath("//div[@aria-label='Date range shortcut']//button[@value='aged']");

	/** All date shortcut buttons — discovered dynamically at runtime (no hardcoded list required). */
	public static final By DATE_FILTER_GROUP = By.xpath("//div[@aria-label='Date range shortcut']");
	public static final By DATE_FILTER_BUTTONS = By.xpath("//div[@aria-label='Date range shortcut']//button");

	// -------------------------- Constitution type filters --------------------------
	/** Group container — children discovered from live UI (HTML values: 0=ALL, 1=Individual, 2=Legal entity). */
	public static final By CONSTITUTION_FILTER_GROUP = By.xpath("//div[@aria-label='Constitution type filter']");
	public static final By CONSTITUTION_FILTER_BUTTONS = By
			.xpath("//div[@aria-label='Constitution type filter']//button");

	// -------------------------- Application type filters (NEW / UPDATE) --------------------------
	public static final By APPLICATION_TYPE_FILTER_GROUP = By.xpath("//div[@aria-label='Application type filter']");
	public static final By APPLICATION_TYPE_FILTER_BUTTONS = By
			.xpath("//div[@aria-label='Application type filter']//button");

	// -------------------------- Dashboard actions --------------------------
	public static final By AUTO_REFRESH_TOGGLE = By.xpath("//input[@aria-label='Toggle dashboard auto-refresh']");
	public static final By REFRESH_BUTTON = By.xpath("//button[@aria-label='Refresh dashboard now']");

	// -------------------------- Summary card totals (icon-scoped for uniqueness) --------------------------
	public static final By DATA_PUSHED_FI_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='CloudUploadIcon']]//h4");
	public static final By IFLOW_IN_PROGRESS_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='AutorenewIcon']]//h4");
	public static final By IFLOW_REJECTIONS_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='CancelIcon']]//h4");
	public static final By CERSAI_IN_PROGRESS_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='PendingActionsIcon']]//h4");
	public static final By CERSAI_REJECTIONS_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='BlockIcon']]//h4");
	public static final By CERSAI_SUCCESS_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='CheckCircleIcon']]//h4");
	public static final By REVERSE_UPDATE_CBS_TOTAL = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='SyncAltIcon']]//h4");

	// -------------------------- Overall dashboard formula (A1 − (B+C+D+E+F) = N) --------------------------
	/**
	 * MCP-verified: formula text is a Typography.caption span next to the Search button.
	 * Example: "A1 − (B+C+D+E+F) = 27" or "A1 − (B+C+D+E+F) = 0"
	 * If the value is non-zero or negative → mismatch detected.
	 */
	public static final By FORMULA_TEXT = By.xpath(
			"//span[contains(@class,'MuiTypography')][contains(normalize-space(.),'A1') and contains(normalize-space(.),'=')]"
					+ " | //*[contains(normalize-space(.),'A1') and contains(normalize-space(.),'(B+C+D+E+F)') and contains(normalize-space(.),'=')]");

	public static final By OVERALL_MISMATCH_BANNER = By
			.xpath("//div[@role='status' and contains(@aria-label,'Count mismatch')]");
	public static final By DATA_PUSHED_FI_MISMATCH_BADGE = By.xpath(
			"//div[contains(@class,'MuiCard-root')][.//*[@data-testid='CloudUploadIcon']]//div[@role='status']");

	/** Search/refresh button next to the formula. MCP-verified: button[aria-label="Search"]. */
	public static final By SEARCH_REFRESH_BUTTON = By.xpath(
			"//button[@aria-label='Search']");

	/** Summary cards — UI title as shown on dashboard (locators stay in Java). */
	public record SummaryCard(String key, String uiTitle, String formulaLabel, By totalLocator) {
	}

	public static final List<SummaryCard> SUMMARY_CARDS = List.of(
			new SummaryCard("DATA_PUSHED_FI", "Data pushed from FI", "A1+A2+A3+A4", DATA_PUSHED_FI_TOTAL),
			new SummaryCard("IFLOW_IN_PROGRESS", "iFlow In Progress", "B1+B2+B3+B4+B5+B6+B7",
					IFLOW_IN_PROGRESS_TOTAL),
			new SummaryCard("IFLOW_REJECTIONS", "iFlow Rejections", "C1+C2", IFLOW_REJECTIONS_TOTAL),
			new SummaryCard("CERSAI_IN_PROGRESS", "CERSAI In Progress", "D1+D2+D3+D4+D5+D6", CERSAI_IN_PROGRESS_TOTAL),
			new SummaryCard("CERSAI_REJECTIONS", "CERSAI Rejections", "E1+E2+E3", CERSAI_REJECTIONS_TOTAL),
			new SummaryCard("CERSAI_SUCCESS", "CERSAI Success", "F1+F2", CERSAI_SUCCESS_TOTAL),
			new SummaryCard("REVERSE_UPDATE_CBS", "Reverse Update to CBS", "G1+G2", REVERSE_UPDATE_CBS_TOTAL));

	// -------------------------- Detail modal (customer list) --------------------------
	/** Customer detail modal — excludes Active session security dialog. */
	public static final By DETAIL_MODAL = By.xpath(
			"//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title') "
					+ "and not(.//span[normalize-space()='Active session detected'])]");
	public static final By DETAIL_MODAL_CLOSE = By.xpath(
			"//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title')]"
					+ "//button[contains(@aria-label,'Close') or @aria-label='close']");
	public static final By DETAIL_MODAL_TABLE_ROWS = By.xpath(
			"//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title')]"
					+ "//tbody/tr[not(contains(@class,'MuiTableRow-head'))]");

	/** Modal header badge e.g. "1134 records" — preferred over counting table rows (pagination). */
	public static final By DETAIL_MODAL_RECORD_COUNT = By.xpath(
			"//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title')]"
					+ "//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'records')]");

	public static final List<By> DETAIL_MODAL_ROW_LOCATORS = List.of(
			DETAIL_MODAL_TABLE_ROWS,
			By.xpath("//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title')]"
					+ "//tr[contains(@class,'MuiTableRow-root') and not(contains(@class,'MuiTableRow-head'))]"),
			By.xpath("//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title')]//table//tbody/tr"),
			By.xpath("//div[@role='dialog' and not(@aria-labelledby='session-active-dialog-title')]"
					+ "//*[@role='rowgroup']//*[@role='row']"));

	public record DashboardSubItem(String code, String ariaLabel, String cardGroup) {
	}

	/**
	 * These sub-items navigate to another page (not a report modal).
	 * Synced to Dashboard HTML — read count only, no click.
	 */
	public static final Set<String> COUNT_ONLY_NAVIGATION_ITEMS = Set.of(
			"B4", // Image Availablitity Queue
			"B5", // Image Compression Queue
			"B7", // Confirm Match Audit Queue
			"D6"  // Probable match pending in FI
	);

	public static boolean isCountOnlyNavigationItem(String code) {
		return COUNT_ONLY_NAVIGATION_ITEMS.contains(code);
	}

	/** Sub-items synced from live Dashboard HTML body (iFlow B1–B7 only; no B8/B9). */
	public static final List<DashboardSubItem> ALL_SUB_ITEMS = List.of(
			new DashboardSubItem("A1", "Success", "Data pushed from FI"),
			new DashboardSubItem("A2", "Failed", "Data pushed from FI"),
			new DashboardSubItem("A3", "In-Progress", "Data pushed from FI"),
			new DashboardSubItem("A4", "Duplicate", "Data pushed from FI"),
			new DashboardSubItem("B1", "Data Validation Queue", "iFlow In Progress"),
			new DashboardSubItem("B2", "Bulk Search", "iFlow In Progress"),
			new DashboardSubItem("B3", "Bulk Download", "iFlow In Progress"),
			new DashboardSubItem("B4", "Image Availablitity Queue", "iFlow In Progress"),
			new DashboardSubItem("B5", "Image Compression Queue", "iFlow In Progress"),
			new DashboardSubItem("B6", "Zip Upload", "iFlow In Progress"),
			new DashboardSubItem("B7", "Confirm Match Audit Queue", "iFlow In Progress"),
			new DashboardSubItem("C1", "Data Rejections", "iFlow Rejections"),
			new DashboardSubItem("C2", "Image Rejections", "iFlow Rejections"),
			new DashboardSubItem("D1", "Bulk Search", "CERSAI In Progress"),
			new DashboardSubItem("D2", "Bulk Download", "CERSAI In Progress"),
			new DashboardSubItem("D3", "Immediate Response", "CERSAI In Progress"),
			new DashboardSubItem("D4", "Periodic Response", "CERSAI In Progress"),
			new DashboardSubItem("D5", "Probable Match Pending with CERSAI", "CERSAI In Progress"),
			new DashboardSubItem("D6", "Probable match pending in FI", "CERSAI In Progress"),
			new DashboardSubItem("E1", "Immediate Response", "CERSAI Rejections"),
			new DashboardSubItem("E2", "Periodic Response", "CERSAI Rejections"),
			new DashboardSubItem("E3", "Bulk Download", "CERSAI Rejections"),
			new DashboardSubItem("F1", "Bulk Search", "CERSAI Success"),
			new DashboardSubItem("F2", "Zip Upload", "CERSAI Success"),
			new DashboardSubItem("G1", "Reverse Update to CBS Queue Pending", "Reverse Update to CBS"),
			new DashboardSubItem("G2", "Reverse Update to CBS Completed", "Reverse Update to CBS"));

	public static By subItemRow(String code, String ariaLabel) {
		return By.xpath("//li[.//span[normalize-space()='" + code + "'] and .//p[@aria-label='" + ariaLabel + "']]");
	}

	public static By subItemCount(String code, String ariaLabel) {
		return By.xpath("//li[.//span[normalize-space()='" + code
				+ "'] and .//p[@aria-label='" + ariaLabel + "']]/p[last()]");
	}

	public static By dateFilterButton(String value) {
		return By.xpath("//div[@aria-label='Date range shortcut']//button[@value='" + value + "']");
	}

	/**
	 * Dynamic constitution button locator from discovered key/text/value — no hardcoded All/Individual/Legal.
	 */
	public static By constitutionFilterButton(String keyOrText) {
		if (keyOrText == null || keyOrText.isBlank()) {
			throw new IllegalArgumentException("Constitution filter key/text is blank");
		}
		String raw = keyOrText.trim();
		String upper = raw.toUpperCase();
		return By.xpath("("
				+ "//div[@aria-label='Constitution type filter']//button["
				+ "@value='" + raw + "'"
				+ " or translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='" + upper + "'"
				+ " or translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='"
				+ upper + "'"
				+ "]"
				+ " | //button[(contains(@class,'MuiToggleButton') or @aria-pressed) and ("
				+ "@value='" + raw + "'"
				+ " or translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='"
				+ upper + "')]"
				+ ")[1]");
	}
}
