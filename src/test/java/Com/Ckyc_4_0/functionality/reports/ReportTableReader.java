/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import Com.Ckyc_4_0.utils.reports.ReportTable;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a report table straight from the DOM: headers as shown (dynamic) and every row of every page.
 * Works for the Reports module (rows-per-page + next-page pager) and for the dashboard popup (one page).
 * The declared count comes from the "N records" chip, "No of Record(s): N" or "Showing a-b of N entries".
 */
public final class ReportTableReader {

	private static final Logger logger = LoggerFactory.getLogger(ReportTableReader.class);
	private static final Pattern CHIP = Pattern.compile("(\\d[\\d,]*)\\s+records?\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern NO_OF = Pattern.compile("No of Record\\(s\\):\\s*(\\d[\\d,]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SHOWING = Pattern.compile("Showing\\s+\\d[\\d,]*\\s*[\\u2013\\-]\\s*\\d[\\d,]*\\s+of\\s+(\\d[\\d,]*)",
			Pattern.CASE_INSENSITIVE);
	private static final int MAX_PAGES = 400;

	private static final String READ_TABLE_JS = "var s=arguments[0]; var t=s.querySelector('table'); if(!t) return null;"
			+ "var h=[].map.call(t.querySelectorAll('thead th'),function(x){return (x.innerText||'').trim();});"
			+ "var r=[].map.call(t.querySelectorAll('tbody tr'),function(tr){return [].map.call(tr.querySelectorAll('td'),"
			+ "function(c){return (c.innerText||'').trim();});});"
			+ "return {h:h,r:r};";

	private ReportTableReader() {
	}

	/** What the scope's table holds right now (current page only). */
	public record Page(List<String> headers, List<List<String>> rows) {
	}

	/** Reads the table inside {@code scope}; null when there is no table. */
	@SuppressWarnings("unchecked")
	public static Page readPage(WebDriver driver, WebElement scope) {
		Object o = ((JavascriptExecutor) driver).executeScript(READ_TABLE_JS, scope);
		if (!(o instanceof Map)) {
			return null;
		}
		Map<String, Object> m = (Map<String, Object>) o;
		List<String> headers = new ArrayList<>();
		for (Object h : (List<Object>) m.get("h")) {
			headers.add(String.valueOf(h));
		}
		List<List<String>> rows = new ArrayList<>();
		for (Object r : (List<Object>) m.get("r")) {
			List<String> cells = new ArrayList<>();
			for (Object c : (List<Object>) r) {
				cells.add(String.valueOf(c));
			}
			rows.add(cells);
		}
		return new Page(headers, rows);
	}

	/** "N records" / "No of Record(s): N" / "of N entries" from the scope's text, or null. */
	public static Integer declaredCount(WebElement scope, StringBuilder from) {
		String text;
		try {
			text = scope.getText();
		} catch (RuntimeException e) {
			return null;
		}
		Matcher m = CHIP.matcher(text);
		if (m.find()) {
			from.append("N records chip");
			return Integer.parseInt(m.group(1).replace(",", ""));
		}
		m = NO_OF.matcher(text);
		if (m.find()) {
			from.append("No of Record(s)");
			return Integer.parseInt(m.group(1).replace(",", ""));
		}
		m = SHOWING.matcher(text);
		if (m.find()) {
			from.append("Showing ... of N entries");
			return Integer.parseInt(m.group(1).replace(",", ""));
		}
		return null;
	}

	/**
	 * Reads the current table fully into {@code target}: raises rows per page when the pager has it, then walks the
	 * pages with the next-page button until it is disabled. A "No records found" placeholder row is not data.
	 */
	public static void readAllPages(WebDriver driver, By scopeBy, ReportTable target, By rowsPerPage, By nextPage) {
		WebElement scope = driver.findElement(scopeBy);
		StringBuilder from = new StringBuilder();
		Integer declared = declaredCount(scope, from);
		if (declared != null) {
			target.declared(declared, from.toString());
		}
		raiseRowsPerPage(driver, scopeBy, rowsPerPage);
		boolean headersTaken = false;
		String lastSignature = "";
		for (int page = 1; page <= MAX_PAGES; page++) {
			scope = driver.findElement(scopeBy); // the page re-renders between pages - never keep a stale element
			Page p = readPage(driver, scope);
			if (p == null) {
				break;
			}
			if (!headersTaken && !p.headers().isEmpty()) {
				target.headers().addAll(p.headers());
				headersTaken = true;
			}
			String signature = p.rows().isEmpty() ? "" : String.join("|", p.rows().get(0)) + "#" + p.rows().size();
			if (page > 1 && signature.equals(lastSignature)) {
				logger.warn("Pager did not move (same page read twice) - stopping at page {}", page);
				break;
			}
			lastSignature = signature;
			for (List<String> row : p.rows()) {
				if (isPlaceholder(row)) {
					continue;
				}
				target.rows().add(row);
			}
			List<WebElement> next = scope.findElements(nextPage);
			if (next.isEmpty() || !next.get(0).isEnabled() || "true".equals(next.get(0).getAttribute("disabled"))
					|| "true".equals(next.get(0).getAttribute("aria-disabled"))) {
				break;
			}
			String before = pagerText(driver, scopeBy);
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", next.get(0));
			waitForChange(driver, scopeBy, before);
		}
	}

	private static void raiseRowsPerPage(WebDriver driver, By scopeBy, By rowsPerPage) {
		try {
			List<WebElement> combos = driver.findElement(scopeBy).findElements(rowsPerPage);
			if (combos.isEmpty()) {
				return;
			}
			((JavascriptExecutor) driver).executeScript(
					"arguments[0].dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));", combos.get(0));
			sleep(400);
			WebElement best = null;
			int bestVal = -1;
			for (WebElement o : driver.findElements(By.xpath("//ul[@role='listbox']//li[@role='option']"))) {
				try {
					int v = Integer.parseInt(o.getText().trim());
					if (v > bestVal) {
						bestVal = v;
						best = o;
					}
				} catch (NumberFormatException ignored) {
					// non numeric option (e.g. All) - keep numeric maximum
				}
			}
			if (best != null) {
				String before = pagerText(driver, scopeBy);
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", best);
				waitForChange(driver, scopeBy, before);
				logger.info("Rows per page set to {}", bestVal);
			}
		} catch (RuntimeException e) {
			logger.warn("Could not raise rows per page: {}", e.getMessage());
		}
	}

	private static boolean isPlaceholder(List<String> row) {
		if (row.size() == 1) {
			String t = row.get(0).toLowerCase();
			return t.startsWith("no records found") || t.startsWith("no data") || t.startsWith("no record");
		}
		return false;
	}

	private static String pagerText(WebDriver driver, By scopeBy) {
		try {
			Matcher m = Pattern.compile("Showing[^\\n]*").matcher(driver.findElement(scopeBy).getText());
			return m.find() ? m.group() : "";
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static void waitForChange(WebDriver driver, By scopeBy, String before) {
		long end = System.currentTimeMillis() + 6000;
		while (System.currentTimeMillis() < end) {
			sleep(250);
			if (!pagerText(driver, scopeBy).equals(before)) {
				sleep(300);
				return;
			}
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
