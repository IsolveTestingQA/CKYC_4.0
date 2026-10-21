/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.pages.bulkimport.BulkImportPages;
import Com.Ckyc_4_0.utils.MasterUiHelper;

/** Navigation (nav bar first, URL only as the fallback) and a small table reader shared by the Bulk Import steps. */
final class BiSupport {

	/** A table read as text: headers plus rows. */
	record Table(List<String> headers, List<List<String>> rows) {
		int col(String... words) {
			for (int i = 0; i < headers.size(); i++) {
				String h = headers.get(i).toLowerCase().replaceAll("[^a-z0-9]", "");
				boolean all = true;
				for (String w : words) {
					all &= h.contains(w.toLowerCase().replaceAll("[^a-z0-9]", ""));
				}
				if (all) {
					return i;
				}
			}
			return -1;
		}
	}

	private BiSupport() {
	}

	static WebDriver driver() {
		return BaseClass.getDriver();
	}

	static void sleep(long ms) {
		MasterUiHelper.sleep(ms);
	}

	/**
	 * Opens a module the way a user does: side navigation item first. {@code beforeClick} (for example the Settings group
	 * that holds Monitor Automation) is clicked only when the item itself is not visible. The URL is used only when the
	 * click did not land.
	 */
	static void openModule(By navItem, By beforeClick, String urlPart, String fallbackPath) {
		WebDriver d = driver();
		String now = safeUrl();
		if (now.contains(urlPart)) {
			return;
		}
		try {
			MasterUiHelper.ensureSideNavOpen(d, 8);
			if (beforeClick != null && !MasterUiHelper.isVisible(navItem, 1)) {
				MasterUiHelper.clickJs(beforeClick, 8);
				sleep(500);
			}
			MasterUiHelper.clickJs(navItem, 8);
			new WebDriverWait(d, Duration.ofSeconds(15)).until(ExpectedConditions.urlContains(urlPart));
		} catch (RuntimeException e) {
			d.get(ConfigReader.getUrl() + fallbackPath); // nav item missing or did not land: endpoint as the last resort
		}
		new WebDriverWait(d, Duration.ofSeconds(20)).until(ExpectedConditions.urlContains(urlPart));
		sleep(900);
	}

	static String safeUrl() {
		try {
			String u = driver().getCurrentUrl();
			return u == null ? "" : u;
		} catch (RuntimeException e) {
			return "";
		}
	}

	static void clickJs(WebElement e) {
		((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView({block:'center'});arguments[0].click();", e);
	}

	static boolean present(By by) {
		return !driver().findElements(by).isEmpty();
	}

	static String text(By by) {
		List<WebElement> l = driver().findElements(by);
		return l.isEmpty() ? "" : l.get(0).getText().trim();
	}

	static boolean waitFor(By by, int seconds) {
		try {
			new WebDriverWait(driver(), Duration.ofSeconds(seconds)).until(ExpectedConditions.presenceOfElementLocated(by));
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	static void closeDialog() {
		WebDriver d = driver();
		List<WebElement> close = d.findElements(BulkImportPages.DIALOG_CLOSE);
		if (!close.isEmpty()) {
			clickJs(close.get(0));
		} else {
			new Actions(d).sendKeys(Keys.ESCAPE).perform();
		}
		try {
			new WebDriverWait(d, Duration.ofSeconds(5)).until(ExpectedConditions.invisibilityOfElementLocated(BulkImportPages.DIALOG));
		} catch (RuntimeException ignored) {
			// already closed
		}
		sleep(300);
	}

	private static final String READ_TABLE = "var root=arguments[0];var t=root.querySelector('table');if(!t){return null;}"
			+ "var h=[].map.call(t.querySelectorAll('thead th'),function(x){return (x.innerText||'').replace(/\\s+/g,' ').trim();});"
			+ "var r=[].map.call(t.querySelectorAll('tbody tr'),function(tr){return [].map.call(tr.querySelectorAll('td'),function(td){return (td.innerText||'').replace(/\\s+/g,' ').trim();});});"
			+ "return JSON.stringify({h:h,r:r});";

	/** Reads the first table inside {@code scope} (all pages: the next-page button is clicked while it is enabled). */
	static Table readTable(By scope) {
		WebDriver d = driver();
		List<String> headers = new ArrayList<>();
		List<List<String>> rows = new ArrayList<>();
		String lastSig = "";
		for (int page = 1; page <= 60; page++) {
			WebElement root = d.findElement(scope);
			Object o = ((JavascriptExecutor) d).executeScript(READ_TABLE, root);
			if (o == null) {
				break;
			}
			JSONObject j = new JSONObject(String.valueOf(o));
			if (headers.isEmpty()) {
				JSONArray h = j.getJSONArray("h");
				for (int i = 0; i < h.length(); i++) {
					headers.add(h.getString(i));
				}
			}
			JSONArray r = j.getJSONArray("r");
			String sig = r.length() == 0 ? "" : r.toString();
			if (sig.equals(lastSig)) {
				break;
			}
			lastSig = sig;
			for (int i = 0; i < r.length(); i++) {
				JSONArray c = r.getJSONArray(i);
				List<String> row = new ArrayList<>();
				for (int k = 0; k < c.length(); k++) {
					row.add(c.getString(k));
				}
				rows.add(row);
			}
			List<WebElement> next = root.findElements(By.xpath(".//button[@aria-label='Go to next page']"));
			if (next.isEmpty() || !next.get(0).isEnabled()) {
				break;
			}
			clickJs(next.get(0));
			sleep(700);
		}
		return new Table(headers, rows);
	}
}
