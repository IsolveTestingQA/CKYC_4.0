/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.dashboard.DashboardPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.reports.ReportTable;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Dashboard side of the comparison: Weekly + application type + constitution, the live sub-cards, and each
 * sub-card's report popup read completely (headers as shown, every row).
 */
public final class DashboardReportReader {

	private static final Logger logger = LoggerFactory.getLogger(DashboardReportReader.class);
	private static final By APP_GROUP = DashboardPage.APPLICATION_TYPE_FILTER_GROUP;
	private static final By CON_GROUP = DashboardPage.CONSTITUTION_FILTER_GROUP;
	private static final By POPUP_ROWS_PER_PAGE = By.xpath(".//*[normalize-space()='Rows per page:']/following::div[@role='combobox'][1]");
	private static final By POPUP_NEXT = By.xpath(".//button[@aria-label='Go to next page']");
	private static final By POPUP_CLOSE = By.xpath("//div[@role='dialog']//button[@aria-label='Close report dialog' "
			+ "or contains(@aria-label,'Close')]");

	private DashboardReportReader() {
	}

	private static WebDriver driver() {
		return BaseClass.getDriver();
	}

	public static void openDashboard() {
		WebDriver d = driver();
		String url = d.getCurrentUrl();
		if (url == null || !url.contains("/dashboard")) {
			MasterUiHelper.ensureSideNavOpen(d, 8);
			MasterUiHelper.clickJs(SideNavPage.DASHBOARD, 8);
			new WebDriverWait(d, Duration.ofSeconds(20)).until(ExpectedConditions.urlContains("/dashboard"));
		}
		new WebDriverWait(d, Duration.ofSeconds(20))
				.until(ExpectedConditions.presenceOfElementLocated(DashboardPage.DATE_FILTER_GROUP));
		MasterUiHelper.ensureSideNavClosed(d, 5);
		sleep(400);
	}

	/** Weekly + the application type + the constitution (each only clicked when not already selected). */
	public static void setFilters(String application, String constitution) {
		clickIfNotSelected(DashboardPage.DATE_FILTER_WEEKLY);
		clickIfNotSelected(toggleIn(APP_GROUP, application));
		clickIfNotSelected(toggleIn(CON_GROUP, constitution));
		waitForCountsToSettle();
	}

	public static String dateRange() {
		try {
			String from = driver().findElement(DashboardPage.FROM_DATE_INPUT).getAttribute("value");
			String to = driver().findElement(DashboardPage.TO_DATE_INPUT).getAttribute("value");
			return from + " to " + to;
		} catch (RuntimeException e) {
			return "";
		}
	}

	public static List<DashboardLive.SubCard> subCards() {
		return DashboardLive.read(driver());
	}

	/**
	 * Opens the sub-card's report popup and reads it completely. Zero-count sub-cards are not opened (nothing to
	 * read); navigation items and any problem come back as a NOT EXECUTED table with the reason.
	 */
	public static ReportTable readPopup(DashboardLive.SubCard sc, String application, String constitution) {
		WebDriver d = driver();
		ReportTable fallback = new ReportTable(ReportTable.DASHBOARD, sc.group(), sc.label(), application, constitution)
				.code(sc.code()).cardCount(sc.count()).dateRange(dateRange());
		if (DashboardLive.navigationOnly(sc.label())) {
			// opens another page: never clicked - only the number on the card is used for the count check
			return fallback.countOnly().declared(sc.count(), "dashboard sub-card number (opens another page - not clicked)");
		}
		if (sc.count() == 0) {
			return fallback.declared(0, "dashboard sub-card count");
		}
		String urlBefore;
		try {
			urlBefore = d.getCurrentUrl();
		} catch (RuntimeException e) {
			throw new SessionLost("Browser closed before sub-card " + sc.code() + " " + sc.label() + " could be opened", e);
		}
		try {
			WebElement li = d.findElement(DashboardPage.subItemRow(sc.code(), sc.label()));
			((JavascriptExecutor) d).executeScript("arguments[0].scrollIntoView({block:'center'});arguments[0].click();", li);
			WebElement dialog;
			try {
				dialog = new WebDriverWait(d, Duration.ofSeconds(10))
						.until(ExpectedConditions.visibilityOfElementLocated(DashboardPage.DETAIL_MODAL));
			} catch (RuntimeException e) {
				String now = d.getCurrentUrl();
				if (now != null && !now.equals(urlBefore)) {
					d.navigate().back();
					openDashboard();
					return fallback.notExecuted("Sub-card '" + sc.label() + "' navigated to " + now + " instead of a popup");
				}
				return fallback.notExecuted("Report popup did not open for sub-card " + sc.code() + " " + sc.label()
						+ " (element " + DashboardPage.DETAIL_MODAL + " not visible)");
			}
			sleep(700);
			String name = popupReportName(dialog);
			ReportTable t = new ReportTable(ReportTable.DASHBOARD, sc.group(), name.isEmpty() ? sc.label() : name,
					application, constitution).code(sc.code()).cardCount(sc.count()).dateRange(fallback.dateRange());
			ReportTableReader.readAllPages(d, DashboardPage.DETAIL_MODAL, t, POPUP_ROWS_PER_PAGE, POPUP_NEXT);
			closePopup();
			logger.info("Dashboard popup read | {} {} | {} {} | rows={} declared={}", sc.code(), sc.label(), application,
					constitution, t.rows().size(), t.declaredCount());
			return t;
		} catch (RuntimeException e) {
			if (SessionLost.is(e)) {
				throw new SessionLost("Browser closed while reading dashboard sub-card " + sc.code() + " " + sc.label(), e);
			}
			String why = e.getClass().getSimpleName() + ": " + firstLine(e.getMessage());
			logger.warn("Dashboard popup not executed | {} {} | {}", sc.code(), sc.label(), why);
			try {
				Hooks.captureMilestone("Dashboard popup NOT EXECUTED | " + sc.code() + " " + sc.label() + " | " + why, false);
			} catch (RuntimeException ignored) {
				// screenshot is best effort
			}
			try {
				closePopup();
			} catch (RuntimeException ignored) {
				// nothing open
			}
			return fallback.notExecuted(why);
		}
	}

	/** Screenshot with the sub-card scrolled into the middle of the screen and outlined. */
	public static String captureSubCardEvidence(DashboardLive.SubCard sc, String description) {
		try {
			Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.set(DashboardPage.subItemRow(sc.code(), sc.label()));
			Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.banner(description);
			String path = Com.Ckyc_4_0.UtilityFiles.ScreenshotManager.captureAndAttach(driver(), null, description, false);
			return path == null ? "" : path;
		} catch (RuntimeException e) {
			logger.warn("Sub-card evidence could not be captured: {}", e.getMessage());
			return "";
		} finally {
			Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.release(driver());
			Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.clear();
		}
	}

	/** Reads the popup that is open right now (all rows / pages) into {@code target}. */
	public static void readOpenPopup(ReportTable target) {
		ReportTableReader.readAllPages(driver(), DashboardPage.DETAIL_MODAL, target, POPUP_ROWS_PER_PAGE, POPUP_NEXT);
	}

	/** The popup repeats the report type name in a read-only combobox - the exact key used by the Reports module. */
	private static String popupReportName(WebElement dialog) {
		for (WebElement c : dialog.findElements(By.xpath(".//*[@role='combobox' or @aria-haspopup='listbox']"))) {
			String t = c.getText().trim();
			if (!t.isEmpty()) {
				return t;
			}
			String v = c.getAttribute("value");
			if (v != null && !v.isBlank()) {
				return v.trim();
			}
		}
		return "";
	}

	public static void closePopup() {
		WebDriver d = driver();
		List<WebElement> close = d.findElements(POPUP_CLOSE);
		if (!close.isEmpty()) {
			((JavascriptExecutor) d).executeScript("arguments[0].click();", close.get(0));
		}
		try {
			new WebDriverWait(d, Duration.ofSeconds(6)).until(ExpectedConditions.invisibilityOfElementLocated(DashboardPage.DETAIL_MODAL));
		} catch (RuntimeException ignored) {
			// already gone
		}
		sleep(300);
	}

	private static By toggleIn(By group, String text) {
		String up = text.toUpperCase();
		String groupXpath = group.toString().replace("By.xpath: ", "");
		return By.xpath(groupXpath + "//button[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='" + up + "']");
	}

	private static void clickIfNotSelected(By by) {
		WebElement b = new WebDriverWait(driver(), Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(by));
		String pressed = b.getAttribute("aria-pressed");
		String cls = b.getAttribute("class");
		boolean selected = "true".equals(pressed) || (cls != null && cls.contains("Mui-selected"));
		if (!selected) {
			((JavascriptExecutor) driver()).executeScript("arguments[0].click();", b);
			sleep(500);
		}
	}

	/** After a filter change the sub-card counts lag: wait until two reads 600 ms apart agree. */
	private static void waitForCountsToSettle() {
		sleep(Math.max(ConfigReader.getFilterRefreshWaitMs(), 1200));
		String last = "";
		for (int i = 0; i < 20; i++) {
			String now = DashboardLive.read(driver()).toString();
			if (now.equals(last) && !now.equals("[]")) {
				return;
			}
			last = now;
			sleep(600);
		}
	}

	private static String firstLine(String s) {
		if (s == null) {
			return "";
		}
		int nl = s.indexOf('\n');
		String l = nl > 0 ? s.substring(0, nl) : s;
		return l.length() > 300 ? l.substring(0, 300) : l;
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
