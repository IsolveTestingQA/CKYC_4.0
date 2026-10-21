/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.reports.ReportsPage;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.MasterUiHelper;
import Com.Ckyc_4_0.utils.reports.ReportTable;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Reports module (/reports): pick a category and a report type, set NEW / UPDATE and ALL / Individual / Legal
 * entity, Search, and read the whole result (all pages). Every failure becomes a NOT EXECUTED table that names the
 * element or exception - a missing element never stops the run.
 */
public final class ReportsFunctionality {

	private static final Logger logger = LoggerFactory.getLogger(ReportsFunctionality.class);

	private static final String SIGNATURE_JS = "var m=document.querySelector('main'); if(!m) return '|||';"
			+ "var t=m.querySelector('table'); var rows=t?t.querySelectorAll('tbody tr'):[];"
			+ "var txt=m.innerText||'';"
			+ "var chip=(txt.match(/\\d[\\d,]*\\s+records?/i)||[''])[0]; var show=(txt.match(/Showing[^\\n]*/)||[''])[0];"
			+ "return chip+'|'+show+'|'+rows.length+'|'+(rows[0]?(rows[0].innerText||'').slice(0,80):'');";

	private static String dateRange = "";

	private ReportsFunctionality() {
	}

	private static WebDriver driver() {
		return BaseClass.getDriver();
	}

	/** Opens the Reports module from the side nav (no direct URL) and closes the drawer. */
	public static void openModule() {
		WebDriver d = driver();
		String url = d.getCurrentUrl();
		if (url == null || !url.contains("/reports")) {
			MasterUiHelper.ensureSideNavOpen(d, 8);
			MasterUiHelper.clickJs(SideNavPage.REPORTS, 8);
			new WebDriverWait(d, Duration.ofSeconds(20)).until(ExpectedConditions.urlContains("/reports"));
		}
		new WebDriverWait(d, Duration.ofSeconds(20)).until(ExpectedConditions.presenceOfElementLocated(ReportsPage.combo(1)));
		MasterUiHelper.ensureSideNavClosed(d, 5);
		sleep(400);
	}

	public static List<String> categories() {
		return optionsOf(1);
	}

	public static List<String> types(String category) {
		select(1, category);
		return optionsOf(2);
	}

	/**
	 * Evidence for "dashboard shows it, the Reports module does not list it": opens the Report Type list of the
	 * category (or the Report Category list when the category itself is missing), with the open list brought into
	 * view, and takes the screenshot. Returns the screenshot file path ("" when it could not be taken).
	 */
	public static String captureListEvidence(String category, String description) {
		try {
			openModule();
			List<String> cats = categories();
			int combo = 2;
			if (cats.contains(category)) {
				if (!currentValue(1).equals(category)) {
					select(1, category);
				}
			} else {
				combo = 1;
			}
			open(combo);
			Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.set(ReportsPage.LISTBOX);
			Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.banner(description);
			try {
				String path = Com.Ckyc_4_0.UtilityFiles.ScreenshotManager.captureAndAttach(driver(), null, description, false);
				return path == null ? "" : path;
			} finally {
				Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.release(driver());
				Com.Ckyc_4_0.UtilityFiles.ScreenshotFocus.clear();
				closeList(combo);
			}
		} catch (RuntimeException e) {
			logger.warn("Dropdown evidence could not be captured: {}", e.getMessage());
			return "";
		}
	}

	/** From / To dates the Reports page shows (default = weekly). */
	public static String dateRange() {
		return dateRange;
	}

	/**
	 * Runs one report: select category + type, set the filters (only when this report has them) and read everything.
	 * Never throws: a problem is returned as a NOT EXECUTED table with the exception text.
	 */
	public static ReportTable run(String category, String type, String application, String constitution) {
		WebDriver d = driver();
		String app = application;
		String con = constitution;
		try {
			if (!currentValue(1).equals(category)) {
				select(1, category);
			}
			if (!currentValue(2).equals(type)) {
				select(2, type);
			}
			boolean filters = !d.findElements(ReportsPage.toggle("NEW")).isEmpty();
			if (filters) {
				setToggle(application);
				setToggle(constitution);
			} else {
				app = "n/a";
				con = "n/a";
			}
			String before = signature();
			List<WebElement> run = d.findElements(ReportsPage.RUN_BUTTON);
			if (!run.isEmpty()) {
				((JavascriptExecutor) d).executeScript("arguments[0].click();", run.get(0));
			}
			waitForResults(before);
			readDateRange();
			ReportTable t = new ReportTable(ReportTable.REPORT, category, type, app, con).dateRange(dateRange);
			if (d.findElements(ReportsPage.TABLE).isEmpty()) {
				return t.notExecuted("No results table appeared after Search for '" + type + "' (element //main//table not found)");
			}
			ReportTableReader.readAllPages(d, ReportsPage.MAIN, t, ReportsPage.ROWS_PER_PAGE, ReportsPage.NEXT_PAGE);
			logger.info("Report read | {} / {} | {} {} | rows={} declared={}", category, type, app, con, t.rows().size(),
					t.declaredCount());
			return t;
		} catch (RuntimeException e) {
			if (SessionLost.is(e)) {
				throw new SessionLost("Browser closed while reading report '" + category + " / " + type + "'", e);
			}
			String why = e.getClass().getSimpleName() + ": " + firstLine(e.getMessage());
			logger.warn("Report not executed | {} / {} | {}", category, type, why);
			try {
				Hooks.captureMilestone("Reports NOT EXECUTED | " + category + " / " + type + " | " + why, false);
			} catch (RuntimeException ignored) {
				// screenshot is best effort
			}
			return new ReportTable(ReportTable.REPORT, category, type, app, con).notExecuted(why);
		}
	}

	// ------------------------------------------------------------------ helpers

	private static String currentValue(int n) {
		try {
			String v = driver().findElement(ReportsPage.combo(n)).getAttribute("value");
			return v == null ? "" : v.trim();
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static List<String> optionsOf(int n) {
		open(n);
		List<String> out = new ArrayList<>();
		for (WebElement o : driver().findElements(ReportsPage.OPTIONS)) {
			String t = o.getText().trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
		closeList(n);
		return out;
	}

	private static void open(int n) {
		WebDriver d = driver();
		WebElement btn = new WebDriverWait(d, Duration.ofSeconds(10))
				.until(ExpectedConditions.presenceOfElementLocated(ReportsPage.comboOpenButton(n)));
		((JavascriptExecutor) d).executeScript("arguments[0].click();", btn);
		new WebDriverWait(d, Duration.ofSeconds(10))
				.until(ExpectedConditions.presenceOfElementLocated(ReportsPage.OPTIONS));
	}

	private static void closeList(int n) {
		try {
			driver().findElement(ReportsPage.combo(n)).sendKeys(Keys.ESCAPE);
		} catch (RuntimeException ignored) {
			// list already closed
		}
		sleep(250);
	}

	private static void select(int n, String text) {
		open(n);
		WebElement o = new WebDriverWait(driver(), Duration.ofSeconds(10))
				.until(ExpectedConditions.presenceOfElementLocated(ReportsPage.option(text)));
		((JavascriptExecutor) driver()).executeScript("arguments[0].click();", o);
		sleep(800);
	}

	private static void setToggle(String text) {
		WebElement b = new WebDriverWait(driver(), Duration.ofSeconds(8))
				.until(ExpectedConditions.presenceOfElementLocated(ReportsPage.toggle(text)));
		if (!"true".equals(b.getAttribute("aria-pressed"))) {
			((JavascriptExecutor) driver()).executeScript("arguments[0].click();", b);
			sleep(500);
		}
	}

	private static String signature() {
		try {
			Object o = ((JavascriptExecutor) driver()).executeScript(SIGNATURE_JS);
			return o == null ? "|||" : o.toString();
		} catch (RuntimeException e) {
			return "|||";
		}
	}

	/** Waits until the result differs from the previous one and stays the same for two polls (or 3.5s if unchanged). */
	private static void waitForResults(String before) {
		long start = System.currentTimeMillis();
		long deadline = start + 30_000;
		String prev = null;
		int stable = 0;
		while (System.currentTimeMillis() < deadline) {
			sleep(400);
			String sig = signature();
			stable = sig.equals(prev) ? stable + 1 : 0;
			prev = sig;
			boolean changed = !sig.equals(before);
			if (!"|||".equals(sig) && stable >= 2 && (changed || System.currentTimeMillis() - start > 3500)) {
				return;
			}
		}
		logger.warn("Results did not settle within 30s - reading what is on the page");
	}

	private static void readDateRange() {
		try {
			Object o = ((JavascriptExecutor) driver()).executeScript(
					"var v=[].map.call(document.querySelectorAll('main input'),function(i){return i.value;})"
							+ ".filter(function(x){return /\\d{2}-\\w{3}-\\d{4}/.test(x);}); return v.join(' to ');");
			dateRange = o == null ? "" : o.toString();
		} catch (RuntimeException ignored) {
			dateRange = "";
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
