/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.capture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;
import Com.Ckyc_4_0.utils.MasterUiHelper;

/**
 * Finds the locators of the whole application by itself, the way the tester did by hand: after login it walks the side
 * navigation (every module and sub-module, groups are expanded), takes a screen inventory of every page, and on each page
 * opens only SAFE things - tabs, "New / Add" panels (then closes them) and one Dashboard sub-card list - taking an inventory
 * of each state. It never types, never submits, and never clicks anything that saves, deletes, approves, restarts, uploads,
 * downloads or logs out (word list below + {@code crawl.blockWords}). It also checks every locator the code already has
 * against each page, so new elements and stale locators come out of the same run.
 */
public final class LocatorCrawler {

	private static final Logger logger = LoggerFactory.getLogger(LocatorCrawler.class);

	private static final Pattern BLOCKED = Pattern.compile("(?i)\\b(save|submit|delete|remove|approve|revert|reject|restart|start|stop|lock|unlock|"
			+ "import|upload|logout|log out|sign out|confirm|yes|send|download|export|clear|reset|activate|deactivate|dormant|create|update|"
			+ "apply|sync|publish|cancel|run|enable|disable|merge|reverse)\\b");

	private final WebDriver driver;
	private final Path dir;
	private final List<CatalogWriter.PageState> states = new ArrayList<>();
	private final List<LocatorHealth.Existing> existing;
	private final Map<Integer, Set<String>> foundOn = new HashMap<>();
	private final Set<String> visited = new LinkedHashSet<>();
	private final Pattern extraBlocked;
	private String script;
	private String lastSig = "";
	private final int maxPages = Integer.parseInt(ConfigReader.get("crawl.maxPages", "40"));
	private final int maxClicks = Integer.parseInt(ConfigReader.get("crawl.maxClicksPerPage", "10"));
	private final Set<String> skipModules = new HashSet<>();

	public LocatorCrawler(WebDriver driver) {
		this.driver = driver;
		this.dir = Paths.get(ReportNamingHelper.reportOutputRoot()).getParent().resolve("Locator Capture").resolve(ReportNamingHelper.runDate())
				.resolve(ReportNamingHelper.runTimestamp() + "_crawl");
		this.existing = LocatorHealth.load();
		String extra = ConfigReader.get("crawl.blockWords", "");
		this.extraBlocked = extra.isBlank() ? null : Pattern.compile("(?i)\\b(" + extra.replace(",", "|") + ")\\b");
		for (String s : ConfigReader.get("crawl.skipModules", "").split(",")) {
			if (!s.isBlank()) {
				skipModules.add(s.trim().toLowerCase(Locale.ROOT));
			}
		}
	}

	public Path folder() {
		return dir;
	}

	// ------------------------------------------------------------------ run

	public void run() {
		try {
			Files.createDirectories(dir);
			script = loadScript();
		} catch (IOException e) {
			throw new IllegalStateException("Locator crawler cannot start: " + e.getMessage(), e);
		}
		logger.info("Locator crawl started - {} existing locators to check. Folder: {}", existing.size(), dir);
		try {
			snapshot("Home", "page");
			for (int pass = 1; pass <= 4 && visited.size() < maxPages; pass++) {
				int before = visited.size();
				for (NavItem n : navItems()) {
					if (visited.size() >= maxPages) {
						break;
					}
					if (visited.contains(n.label) || skipModules.contains(n.label.toLowerCase(Locale.ROOT))) {
						continue;
					}
					visit(n);
				}
				if (visited.size() == before) {
					break;
				}
			}
		} catch (org.openqa.selenium.WebDriverException e) {
			logger.warn("Crawl stopped: {}", e.getMessage());
		}
		try {
			new CatalogWriter(dir, states, existing, null, foundOn).writeAll();
		} catch (IOException e) {
			logger.warn("Catalog could not be written: {}", e.getMessage());
		}
		print();
	}

	private record NavItem(String label, boolean group) {
	}

	private List<NavItem> navItems() {
		ensureNavOpen();
		String js = "var res=[],seen={};function add(l,g){l=(l||'').replace(/\\s+/g,' ').trim();if(!l||seen[l]){return;}seen[l]=1;res.push({label:l,group:g});}"
				+ "document.querySelectorAll('[data-tour-label],[data-nav-label]').forEach(function(e){add(e.getAttribute('data-tour-label')||e.getAttribute('data-nav-label'),false);});"
				+ "document.querySelectorAll('[id^=\"nav-group-\"]').forEach(function(e){add((e.innerText||'').split('\\n')[0],true);});"
				+ "document.querySelectorAll('ul.MuiList-root [role=button]').forEach(function(e){add((e.innerText||'').split('\\n')[0],false);});"
				+ "return JSON.stringify(res);";
		List<NavItem> out = new ArrayList<>();
		try {
			JSONArray a = new JSONArray(String.valueOf(((JavascriptExecutor) driver).executeScript(js)));
			for (int i = 0; i < a.length(); i++) {
				out.add(new NavItem(a.getJSONObject(i).getString("label"), a.getJSONObject(i).getBoolean("group")));
			}
		} catch (RuntimeException e) {
			logger.warn("Navigation items could not be listed: {}", e.getMessage());
		}
		return out;
	}

	private void ensureNavOpen() {
		try {
			MasterUiHelper.ensureSideNavOpen(driver, 5);
		} catch (RuntimeException ignored) {
			// already open or not collapsible
		}
	}

	private static String q(String s) {
		return s.contains("'") ? "concat('" + s.replace("'", "',\"'\",'") + "')" : "'" + s + "'";
	}

	private void visit(NavItem n) {
		visited.add(n.label);
		String xp = "//*[@data-tour-label=" + q(n.label) + " or @data-nav-label=" + q(n.label) + " or (@role='button' and normalize-space()=" + q(n.label) + ")]";
		List<WebElement> els = driver.findElements(By.xpath(xp));
		WebElement target = null;
		for (WebElement e : els) {
			try {
				if (e.isDisplayed()) {
					target = e;
					break;
				}
			} catch (RuntimeException ignored) {
				// stale
			}
		}
		if (target == null) {
			return;
		}
		logger.info("Crawl > {}", n.label);
		click(target);
		sleep(1800);
		String sig = signature();
		if (sig.equals(lastSig) || n.group) {
			return; // a group opened its children: the next pass lists them
		}
		lastSig = sig;
		snapshot(n.label, "page");
		explore(n.label);
	}

	private String signature() {
		try {
			return driver.getCurrentUrl() + "|" + ((JavascriptExecutor) driver).executeScript("return document.querySelectorAll('main button, main input').length;");
		} catch (RuntimeException e) {
			return "";
		}
	}

	// ------------------------------------------------------------------ explore one page safely

	private void explore(String module) {
		int clicks = 0;
		for (String tab : labels(TAB_JS)) {
			if (clicks++ >= maxClicks) {
				break;
			}
			if (clickByText("//main//button[normalize-space()=%s]|//*[@role='tab'][normalize-space()=%s]", tab)) {
				sleep(1000);
				snapshot(module, "tab: " + tab);
			}
		}
		for (String opener : labels(OPENER_JS)) {
			if (clicks++ >= maxClicks) {
				break;
			}
			if (clickByText("//main//button[normalize-space()=%s]", opener)) {
				sleep(1300);
				if (dialogOpen()) {
					snapshot(module, "opened: " + opener);
					closeDialog(module);
				}
			}
		}
		if (signature().contains("/dashboard")) {
			String cards = "var out=[];document.querySelectorAll('li').forEach(function(li){var s=li.querySelector('span'),p=li.querySelector('p[aria-label]');"
					+ "if(s&&p&&/^[A-Z]\\d+$/.test((s.innerText||'').trim())){out.push((s.innerText||'').trim());}});return JSON.stringify(out);";
			try {
				JSONArray a = new JSONArray(String.valueOf(((JavascriptExecutor) driver).executeScript(cards)));
				String code = a.length() == 0 ? "" : a.getString(0);
				if (!code.isBlank()) {
					List<WebElement> li = driver.findElements(By.xpath("//li[.//span[normalize-space()=" + q(code) + "]]"));
					if (!li.isEmpty()) {
						click(li.get(0));
						sleep(1500);
						if (dialogOpen()) {
							snapshot(module, "opened: sub-card " + code);
							closeDialog(module);
						}
					}
				}
			} catch (RuntimeException e) {
				logger.debug("Dashboard sub-card skipped: {}", e.getMessage());
			}
		}
	}

	private static final String TAB_JS = "var out=[];var tabs=document.querySelectorAll('main [role=tab]');"
			+ "if(tabs.length){tabs.forEach(function(t){out.push((t.innerText||'').trim());});}"
			+ "else{document.querySelectorAll('main div,main nav,main ul').forEach(function(p){var b=[].filter.call(p.children,function(c){return c.tagName==='BUTTON';});"
			+ "if(b.length>=2&&b.every(function(x){return /^[A-Z][A-Z &\\/\\-]{2,}$/.test((x.innerText||'').trim());})){b.forEach(function(x){out.push((x.innerText||'').trim());});}});}"
			+ "return JSON.stringify(out.filter(function(x,i){return x&&out.indexOf(x)===i;}));";

	private static final String OPENER_JS = "var out=[];document.querySelectorAll('main button').forEach(function(b){var t=(b.innerText||'').trim();"
			+ "if(/^(new|add)\\b/i.test(t)&&!b.closest('[role=dialog]')){out.push(t);}});return JSON.stringify(out.filter(function(x,i){return out.indexOf(x)===i;}));";

	private List<String> labels(String js) {
		List<String> out = new ArrayList<>();
		try {
			JSONArray a = new JSONArray(String.valueOf(((JavascriptExecutor) driver).executeScript(js)));
			for (int i = 0; i < a.length(); i++) {
				String t = a.getString(i);
				if (!blocked(t)) {
					out.add(t);
				}
			}
		} catch (RuntimeException e) {
			logger.debug("Labels not listed: {}", e.getMessage());
		}
		return out;
	}

	boolean blocked(String text) {
		return text == null || BLOCKED.matcher(text).find() || (extraBlocked != null && extraBlocked.matcher(text).find());
	}

	private boolean clickByText(String xpathTemplate, String text) {
		if (blocked(text)) {
			return false;
		}
		List<WebElement> l = driver.findElements(By.xpath(xpathTemplate.replace("%s", q(text))));
		for (WebElement e : l) {
			try {
				if (e.isDisplayed()) {
					click(e);
					return true;
				}
			} catch (RuntimeException ignored) {
				// try the next match
			}
		}
		return false;
	}

	private void click(WebElement e) {
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});arguments[0].click();", e);
	}

	private boolean dialogOpen() {
		return !driver.findElements(By.xpath("//div[@role='dialog']|//*[contains(@class,'MuiDrawer-paper')]")).isEmpty();
	}

	private void closeDialog(String module) {
		String js = "var d=document.querySelector('[role=dialog],.MuiDrawer-paper');if(!d){return false;}"
				+ "var c=d.querySelector('[data-testid=CloseIcon],[aria-label*=\"Close\"],[aria-label*=\"close\"]');if(c){(c.closest('button')||c).click();return true;}return false;";
		try {
			((JavascriptExecutor) driver).executeScript(js);
			sleep(600);
			if (dialogOpen()) {
				new Actions(driver).sendKeys(Keys.ESCAPE).perform();
				sleep(600);
			}
			if (dialogOpen()) {
				driver.navigate().refresh(); // could not close it: a clean page for the next module
				sleep(2000);
			}
		} catch (RuntimeException e) {
			logger.debug("Dialog of {} not closed cleanly: {}", module, e.getMessage());
		}
	}

	// ------------------------------------------------------------------ inventory

	private void snapshot(String module, String kind) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			if (!Boolean.TRUE.equals(js.executeScript("return !!window.__ckycRecInstalled;"))) {
				js.executeScript("window.__ckycRecInit={quiet:true};" + script);
			}
			Object raw = js.executeScript("return window.__ckycRecSnapshotNow('" + kind.replace("'", "") + "');");
			if (raw == null) {
				return;
			}
			JSONObject snap = new JSONObject(String.valueOf(raw));
			states.add(new CatalogWriter.PageState(module, kind, snap));
			if (!existing.isEmpty()) {
				int[] c = LocatorHealth.count(driver, existing);
				for (int i = 0; i < c.length; i++) {
					if (c[i] > 0) {
						foundOn.computeIfAbsent(i, k -> new LinkedHashSet<>()).add(module + " / " + kind);
					}
				}
			}
			logger.info("  captured {} | {} | {} controls", module, kind, snap.optJSONArray("items") == null ? 0 : snap.getJSONArray("items").length());
		} catch (RuntimeException e) {
			logger.warn("Inventory of {} / {} failed: {}", module, kind, e.getMessage());
		}
	}

	private static String loadScript() throws IOException {
		try (InputStream in = LocatorCrawler.class.getResourceAsStream("/Capture/locator_recorder.js")) {
			if (in == null) {
				throw new IOException("Capture/locator_recorder.js not found on the classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void sleep(long ms) {
		MasterUiHelper.sleep(ms);
	}

	private void print() {
		StringBuilder sb = new StringBuilder(System.lineSeparator());
		sb.append("==================== LOCATOR CRAWL SAVED ====================").append(System.lineSeparator());
		sb.append(" Modules visited : ").append(visited.size()).append("   States captured : ").append(states.size()).append(System.lineSeparator());
		for (String f : Arrays.asList("pages_overview.md", "new_elements.md", "stale_locators.md", "locators_catalog.properties", "catalog.json")) {
			sb.append(String.format(" %-30s %s%n", f, dir.resolve(f).toAbsolutePath().toUri()));
		}
		sb.append(String.format(" %-30s %s%n", "generated_pages (Java)", dir.resolve("generated_pages").toAbsolutePath().toUri()));
		sb.append("=============================================================");
		logger.info(sb.toString());
	}
}
