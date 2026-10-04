/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.UtilityFiles.PomElementManager;
import Com.Ckyc_4_0.pages.SideNavPage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Masters UI helpers. Navigation uses the collapsible left side-nav
 * ({@link SideNavPage}): MenuOpen → Masters expand → submodule.
 */
public final class MasterUiHelper {

	public static final By GRID_DATA_ROWS =
			By.xpath("//*[@role='grid']//*[@role='rowgroup']//*[@role='row'][.//*[@role='gridcell']]"
					+ " | //table//tbody/tr[td]");
	public static final By NEXT_PAGE =
			By.xpath("//button[@aria-label='Go to next page' and not(@disabled)]");
	public static final By PREV_PAGE =
			By.xpath("//button[@aria-label='Go to previous page' and not(@disabled)]");
	public static final By TOAST =
			By.xpath("//*[contains(@class,'Toastify') or @role='alert' or contains(@class,'MuiAlert')]");
	public static final By CREATE_BUTTON =
			By.xpath("//button[normalize-space()='Create' and not(@disabled)]");
	public static final By UPDATE_BUTTON =
			By.xpath("//button[normalize-space()='Update' and not(@disabled)]");
	public static final By CLEAR_BUTTON =
			By.xpath("//button[normalize-space()='Clear']");

	private static final Pattern CHIP_NUMBER = Pattern.compile("(\\d+)");

	private MasterUiHelper() {
	}

	/** Ensure left side-nav is expanded (MenuOpen), then open Masters → submodule, then close nav. */
	public static void openMastersSubmodule(String buttonText, By heading, int waitSeconds) {
		WebDriver driver = BaseClass.getDriver();
		if (driver == null) {
			throw new IllegalStateException("WebDriver is null");
		}
		try {
			openMastersSubmoduleOnce(driver, buttonText, heading, waitSeconds);
		} catch (RuntimeException first) {
			if (!refreshAndPrepareNav(driver, waitSeconds)) {
				throw first;
			}
			openMastersSubmoduleOnce(driver, buttonText, heading, waitSeconds);
		}
	}

	private static void openMastersSubmoduleOnce(WebDriver driver, String buttonText, By heading, int waitSeconds) {
		if (isVisible(heading, 2)) {
			ensureSideNavClosed(driver, Math.min(waitSeconds, 5));
			return;
		}
		ensureSideNavOpen(driver, waitSeconds);
		openMastersGroup(driver, waitSeconds);
		By submodule = SideNavPage.mastersSubmodule(buttonText);
		ExecutionLogger.info("Masters navigation | click submodule=" + buttonText
				+ " | locator=" + submodule);
		clickJs(submodule, waitSeconds);
		PomElementManager.findVisible(heading, waitSeconds);
		ExecutionLogger.pass("Masters navigation landed | submodule=" + buttonText
				+ " | url=" + driver.getCurrentUrl());
		ensureSideNavClosed(driver, Math.min(waitSeconds, 5));
	}

	/**
	 * When the current page is unusable after skip/failure: refresh once, re-check nav, keep URL memory.
	 * @return true if refresh completed and nav was prepared
	 */
	public static boolean refreshAndPrepareNav(WebDriver driver, int waitSeconds) {
		if (driver == null) {
			return false;
		}
		try {
			String before = driver.getCurrentUrl();
			/*
			 * React client routes such as /ckyc/location are not server-refresh safe in
			 * this deployment: a direct refresh is answered by IIS with HTTP 500.
			 * Preserve the page and let the mandatory/unexpected-page prompt guide recovery.
			 */
			if (before != null && before.contains("/ckyc/")) {
				ExecutionLogger.warn("Recovery refresh skipped for React deep route | url=" + before);
				return false;
			}
			if (PopupHandler.isUnexpectedApplicationPage(driver)) {
				ExecutionLogger.warn("Recovery refresh skipped for server/unexpected page | "
						+ PopupHandler.describeUnexpectedPage(driver));
				return false;
			}
			driver.navigate().refresh();
			try {
				Thread.sleep(800L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			ensureSideNavOpen(driver, waitSeconds);
			String after = driver.getCurrentUrl();
			return after != null && (before == null || after.contains("/") || after.equals(before));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Before every scenario (and before nav clicks): if module names are not visible,
	 * click ChevronRight / MenuOpen. If Dashboard or Masters already visible, do nothing.
	 */
	public static void ensureSideNavOpen(WebDriver driver, int waitSeconds) {
		if (driver == null) {
			return;
		}
		List<WebElement> openBtns = driver.findElements(SideNavPage.MENU_OPEN);
		if (!openBtns.isEmpty() && openBtns.get(0).isDisplayed()) {
			ExecutionLogger.info("Side-nav collapsed | click open icon");
			clickJs(SideNavPage.MENU_OPEN, Math.min(waitSeconds, 8));
			try {
				new WebDriverWait(driver, Duration.ofSeconds(Math.min(waitSeconds, 8)))
						.until(ExpectedConditions.or(
								ExpectedConditions.visibilityOfElementLocated(SideNavPage.DASHBOARD),
								ExpectedConditions.visibilityOfElementLocated(SideNavPage.MASTERS)));
			} catch (Exception ignored) {
				// continue; caller may still find Masters
			}
			return;
		}
		if (isVisible(SideNavPage.DASHBOARD, 1) || isVisible(SideNavPage.MASTERS, 1)) {
			return;
		}
	}

	/**
	 * After landing on the scenario page: click ChevronLeft to close the drawer for full visibility.
	 * No-op if modules are already hidden / close button absent.
	 */
	public static void ensureSideNavClosed(WebDriver driver, int waitSeconds) {
		if (driver == null) {
			return;
		}
		if (!isVisible(SideNavPage.DASHBOARD, 1) && !isVisible(SideNavPage.MASTERS, 1)) {
			return;
		}
		List<WebElement> closeBtns = driver.findElements(SideNavPage.MENU_CLOSE);
		if (closeBtns.isEmpty() || !closeBtns.get(0).isDisplayed()) {
			return;
		}
		try {
			clickJs(SideNavPage.MENU_CLOSE, Math.min(waitSeconds, 5));
			new WebDriverWait(driver, Duration.ofSeconds(Math.min(waitSeconds, 5)))
					.until(ExpectedConditions.visibilityOfElementLocated(SideNavPage.MENU_OPEN));
		} catch (Exception ignored) {
			// page usable either way
		}
	}

	/**
	 * Expand the Masters group. The submodule list ({@code #nav-group-masters}) only
	 * exists while the drawer is open, so the drawer state is settled first.
	 */
	public static void openMastersGroup(WebDriver driver, int waitSeconds) {
		ensureSideNavOpen(driver, waitSeconds);
		if (isVisible(SideNavPage.MASTERS_GROUP, 1)) {
			ExecutionLogger.info("Masters navigation | dropdown already open | id=nav-group-masters");
			return;
		}
		WebElement masters = PomElementManager.findClickable(SideNavPage.MASTERS, waitSeconds);
		ExecutionLogger.info("Masters navigation | click parent Masters | aria-expanded="
				+ masters.getAttribute("aria-expanded"));
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", masters);
		PomElementManager.findVisible(SideNavPage.MASTERS_GROUP, waitSeconds);
		ExecutionLogger.info("Masters navigation | dropdown visible | id=nav-group-masters");
	}

	/** Navigate to Dashboard via side-nav, then close drawer for full-width view. */
	public static void goToDashboard() {
		WebDriver driver = BaseClass.getDriver();
		if (driver == null) {
			return;
		}
		ensureSideNavOpen(driver, 8);
		clickJs(SideNavPage.DASHBOARD, 8);
		try {
			new WebDriverWait(driver, Duration.ofSeconds(10))
					.until(ExpectedConditions.urlContains("/dashboard"));
		} catch (Exception ignored) {
		}
		ensureSideNavClosed(driver, 5);
	}

	public static void clickJs(By locator, int waitSeconds) {
		WebElement el = PomElementManager.findClickable(locator, waitSeconds);
		((JavascriptExecutor) BaseClass.getDriver()).executeScript("arguments[0].click();", el);
	}

	/**
	 * Click an element that is present but never reported visible — MUI Switch and
	 * Checkbox render a zero-opacity input that {@code elementToBeClickable} rejects.
	 */
	public static void clickHiddenInput(By locator, int waitSeconds) {
		WebDriver driver = BaseClass.getDriver();
		WebElement el = new WebDriverWait(driver, Duration.ofSeconds(Math.max(waitSeconds, 3)))
				.until(ExpectedConditions.presenceOfElementLocated(locator));
		((JavascriptExecutor) driver).executeScript(
				"arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", el);
	}

	public static void typeInto(By locator, String value, int waitSeconds) {
		WebElement el = PomElementManager.findVisible(locator, waitSeconds);
		el.click();
		el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		if (value != null && !value.isEmpty()) {
			el.sendKeys(value);
		}
	}

	public static String readValue(By locator, int waitSeconds) {
		WebElement el = PomElementManager.findVisible(locator, waitSeconds);
		String v = el.getAttribute("value");
		return v == null ? "" : v.trim();
	}

	public static int readChip(By chipRoot, int waitSeconds) {
		WebElement root = PomElementManager.findVisible(chipRoot, waitSeconds);
		Matcher m = CHIP_NUMBER.matcher(root.getText() == null ? "" : root.getText());
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	/** Prefer Active chip when Total chip is ambiguous (multiple similar labels). */
	public static int readActiveChip(int waitSeconds) {
		By activePrefer = By.xpath(
				"(//*[contains(@class,'MuiChip') or self::span or self::div]"
						+ "[contains(normalize-space(.),'Active') "
						+ "and not(contains(normalize-space(.),'Inactive'))])[1]");
		List<WebElement> els = BaseClass.getDriver().findElements(activePrefer);
		if (!els.isEmpty() && els.get(0).isDisplayed()) {
			Matcher m = CHIP_NUMBER.matcher(els.get(0).getText() == null ? "" : els.get(0).getText());
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		}
		return readChip(By.xpath(
				"(//*[contains(normalize-space(.),'Active') and not(contains(normalize-space(.),'Inactive'))])[1]"),
				waitSeconds);
	}

	public static void search(By searchBox, String term, int waitSeconds) {
		typeInto(searchBox, term, waitSeconds);
		PomElementManager.findVisible(searchBox, waitSeconds).sendKeys(Keys.ENTER);
		try {
			Thread.sleep(700L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void clearSearch(By searchBox, int waitSeconds) {
		typeInto(searchBox, "", waitSeconds);
		PomElementManager.findVisible(searchBox, waitSeconds).sendKeys(Keys.ENTER);
		try {
			Thread.sleep(500L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static boolean isVisible(By locator, int waitSeconds) {
		try {
			PomElementManager.findVisible(locator, waitSeconds);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String readToastIfAny(int waitSeconds) {
		try {
			WebElement t = PomElementManager.findVisible(TOAST, waitSeconds);
			return t.getText() == null ? "" : t.getText().trim();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Open a MUI Select and pick an option by its exact visible text.
	 *
	 * <p>Verified against the live app: these controls are {@code div[role=combobox]},
	 * so {@code sendKeys} is not interactable and a native click is swallowed by the
	 * DataGrid header overlay. MUI opens the listbox from <b>mousedown</b> only — a
	 * synthetic {@code click()} never opens it.
	 */
	public static void selectMuiOption(By combobox, String optionText, int waitSeconds) {
		selectMuiOption(combobox, Collections.singletonList(optionText), waitSeconds);
	}

	/**
	 * Pick the first candidate present in the dropdown. Candidates allow a record to be
	 * matched by its edited name, its original name, or its code when an in-app rename
	 * did not persist.
	 */
	public static void selectMuiOption(By combobox, List<String> candidates, int waitSeconds) {
		WebDriver driver = BaseClass.getDriver();
		RuntimeException last = null;
		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				openMuiSelect(combobox, waitSeconds);
				for (String candidate : candidates) {
					if (candidate == null || candidate.isBlank()) {
						continue;
					}
					String safe = candidate.replace("\"", "");
					List<WebElement> found = driver.findElements(
							By.xpath("(//*[@role='option'][normalize-space(.)=\"" + safe + "\"]"
									+ " | //*[@role='option'][contains(normalize-space(.),\"" + safe + "\")])[1]"));
					if (!found.isEmpty()) {
						((JavascriptExecutor) driver).executeScript(
								"arguments[0].scrollIntoView({block:'center'}); arguments[0].click();",
								found.get(0));
						waitForOverlayToClear(driver, waitSeconds);
						ExecutionLogger.info("MUI select | chose '" + candidate + "' | attempt=" + attempt);
						return;
					}
				}
				last = new NoSuchElementException("No dropdown option matched " + candidates
						+ " | available=" + describeOptions(driver));
			} catch (RuntimeException e) {
				last = e;
			}
			closeAnyOpenMenu(driver);
		}
		throw last;
	}

	/** Option texts currently rendered — logged so a missing value is diagnosable. */
	private static String describeOptions(WebDriver driver) {
		List<WebElement> options = driver.findElements(By.cssSelector("[role='option']"));
		StringBuilder sb = new StringBuilder("[" + options.size() + "] ");
		for (int i = 0; i < Math.min(options.size(), 60); i++) {
			try {
				sb.append('\'').append(options.get(i).getText().trim()).append("' ");
			} catch (Exception ignored) {
				// list rebuilt mid-read; partial listing is still useful
			}
		}
		return sb.toString().trim();
	}

	/** Dispatch the mousedown MUI listens to, then wait for the listbox to render. */
	private static void openMuiSelect(By combobox, int waitSeconds) {
		WebDriver driver = BaseClass.getDriver();
		WebElement el = PomElementManager.findVisible(combobox, waitSeconds);
		((JavascriptExecutor) driver).executeScript(
				"arguments[0].scrollIntoView({block:'center'});"
						+ "arguments[0].dispatchEvent(new MouseEvent('mousedown',"
						+ "{bubbles:true,cancelable:true,buttons:1}));",
				el);
		new WebDriverWait(driver, Duration.ofSeconds(Math.max(waitSeconds, 5)))
				.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[role='option']")));
	}

	private static void closeAnyOpenMenu(WebDriver driver) {
		try {
			new Actions(driver).sendKeys(Keys.ESCAPE).perform();
		} catch (Exception ignored) {
			// menu may already be gone
		}
		waitForOverlayToClear(driver, 3);
	}

	private static void waitForOverlayToClear(WebDriver driver, int waitSeconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(Math.min(Math.max(waitSeconds, 2), 5))).until(
					ExpectedConditions.invisibilityOfElementLocated(
							By.cssSelector(".MuiBackdrop-root, .MuiModal-backdrop")));
		} catch (Exception ignored) {
			sleep(300);
		}
		sleep(300);
	}

	public static void selectAutocomplete(By input, String optionText, int waitSeconds) {
		selectMuiOption(input, optionText, waitSeconds);
	}

	/** Select a visible option from a MUI filter combobox and wait for its backdrop to close. */
	public static void selectFilterOption(By combobox, String optionText, int waitSeconds) {
		selectMuiOption(combobox, optionText, waitSeconds);
		sleep(700);
	}

	public static String uniqueCode(String prefix, int maxLen) {
		String raw = (prefix == null ? "T" : prefix) + System.currentTimeMillis();
		String alnum = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
		return alnum.length() <= maxLen ? alnum : alnum.substring(0, maxLen);
	}

	public static String uniqueName(String prefix, int maxLen) {
		String raw = (prefix == null ? "TEST" : prefix) + " " + System.currentTimeMillis();
		return raw.length() <= maxLen ? raw : raw.substring(0, maxLen);
	}

	// ---------- Chip / badge helpers (label-based) ----------

	/** Read first integer from a chip/badge whose visible text contains {@code labelPart}. */
	public static int readChip(String labelPart) {
		By chip = By.xpath(
				"(//*[contains(@class,'MuiChip') or self::span or self::div]"
						+ "[contains(normalize-space(.),\"" + esc(labelPart) + "\")])[1]");
		List<WebElement> els = BaseClass.getDriver().findElements(chip);
		if (els.isEmpty()) {
			chip = By.xpath("(//*[contains(normalize-space(.),\"" + esc(labelPart) + "\")])[1]");
			els = BaseClass.getDriver().findElements(chip);
		}
		if (els.isEmpty()) {
			return -1;
		}
		Matcher m = CHIP_NUMBER.matcher(els.get(0).getText() == null ? "" : els.get(0).getText());
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	/** Prefer Active chip when Total chip is ambiguous. */
	public static int readActiveChip() {
		return readActiveChip(8);
	}

	// ---------- Search / pagination / grid ----------

	public static void resetSearch(By searchBox, int waitSeconds) {
		clearSearch(searchBox, waitSeconds);
	}

	public static void trySetRowsPerPage(String rows) {
		WebDriver driver = BaseClass.getDriver();
		By select = By.xpath(
				"//div[contains(@class,'MuiTablePagination')]//div[@role='combobox']"
						+ " | //div[contains(@class,'MuiTablePagination')]//select");
		List<WebElement> sels = driver.findElements(select);
		if (sels.isEmpty()) {
			return;
		}
		try {
			WebElement el = sels.get(0);
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
			el.click();
			sleep(300);
			By option = By.xpath("//li[@role='option' and normalize-space()='" + esc(rows) + "']"
					+ " | //option[normalize-space()='" + esc(rows) + "']");
			List<WebElement> opts = driver.findElements(option);
			if (!opts.isEmpty()) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", opts.get(0));
				sleep(500);
			}
		} catch (Exception ignored) {
		}
	}

	public static boolean goNextPageIfEnabled() {
		List<WebElement> next = BaseClass.getDriver().findElements(NEXT_PAGE);
		if (next.isEmpty() || !next.get(0).isEnabled()) {
			return false;
		}
		try {
			((JavascriptExecutor) BaseClass.getDriver()).executeScript("arguments[0].click();", next.get(0));
			sleep(600);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean rowContains(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		By row = By.xpath("//*[@role='grid']//*[@role='rowgroup']//*[@role='row']"
				+ "[.//*[@role='gridcell'][contains(normalize-space(.),\"" + esc(text) + "\")]]"
				+ " | //table//tbody/tr[td][contains(normalize-space(.),\"" + esc(text) + "\")]");
		return !BaseClass.getDriver().findElements(row).isEmpty();
	}

	public static boolean isButtonEnabled(By locator) {
		List<WebElement> els = BaseClass.getDriver().findElements(locator);
		if (els.isEmpty()) {
			return false;
		}
		WebElement el = els.get(0);
		String disabled = el.getAttribute("disabled");
		String aria = el.getAttribute("aria-disabled");
		return el.isEnabled()
				&& (disabled == null || disabled.isBlank() || "false".equalsIgnoreCase(disabled))
				&& (aria == null || !"true".equalsIgnoreCase(aria));
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(Math.max(0L, millis));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Convenience overload used by some masters (treats value as milliseconds). */
	public static void sleep(int millis) {
		sleep((long) millis);
	}

	// ---------- Row actions (edit / toggle / delete) ----------

	public static By editButton(String rowKey) {
		return By.xpath("(//*[@role='grid']//*[@role='rowgroup']//*[@role='row']"
				+ "[.//*[@role='gridcell'][contains(normalize-space(.),\"" + esc(rowKey) + "\")]]"
				+ "//*[@role='gridcell'][last()]//button"
				+ " | //table//tbody/tr[td][contains(normalize-space(.),\"" + esc(rowKey) + "\")]"
				+ "//button[@aria-label='Edit' or contains(@aria-label,'Edit')"
				+ " or .//*[@data-testid='EditIcon']])[1]");
	}

	public static By toggleFor(String rowKey) {
		return By.xpath("(//*[@role='grid']//*[@role='rowgroup']//*[@role='row']"
				+ "[.//*[@role='gridcell'][contains(normalize-space(.),\"" + esc(rowKey) + "\")]]"
				+ "//input[@type='checkbox']"
				+ " | //table//tbody/tr[td][contains(normalize-space(.),\"" + esc(rowKey) + "\")]"
				+ "//input[@type='checkbox'])[1]");
	}

	public static boolean isToggleChecked(String rowKey) {
		By toggle = toggleFor(rowKey);
		List<WebElement> els = BaseClass.getDriver().findElements(toggle);
		if (els.isEmpty()) {
			return false;
		}
		WebElement el = els.get(0);
		if ("input".equalsIgnoreCase(el.getTagName())) {
			return el.isSelected();
		}
		String checked = el.getAttribute("checked");
		String aria = el.getAttribute("aria-checked");
		return "true".equalsIgnoreCase(checked) || "true".equalsIgnoreCase(aria) || el.isSelected();
	}

	public static boolean deleteAvailable() {
		By del = By.xpath("//table[.//th][1]//tbody/tr[td]//button[@aria-label='Delete' "
				+ "or contains(@aria-label,'Delete') or .//*[@data-testid='DeleteIcon']]");
		return !BaseClass.getDriver().findElements(del).isEmpty();
	}

	public static void clickDeleteIfPresent(String rowKey, int waitSeconds) {
		By del = By.xpath("//table[.//th][1]//tbody/tr[td][contains(.,\"" + esc(rowKey) + "\")]"
				+ "//button[@aria-label='Delete' or contains(@aria-label,'Delete') "
				+ "or .//*[@data-testid='DeleteIcon']][1]");
		List<WebElement> els = BaseClass.getDriver().findElements(del);
		if (els.isEmpty()) {
			return;
		}
		clickJs(del, waitSeconds);
		sleep(300);
		By confirm = By.xpath("//button[normalize-space()='Delete' or normalize-space()='Confirm' "
				+ "or normalize-space()='Yes' or @aria-label='Confirm delete']");
		List<WebElement> conf = BaseClass.getDriver().findElements(confirm);
		if (!conf.isEmpty()) {
			((JavascriptExecutor) BaseClass.getDriver()).executeScript("arguments[0].click();", conf.get(0));
		}
	}

	public static void clickSaveIfPresent(int waitSeconds) {
		By save = By.xpath("//button[normalize-space()='Save' or normalize-space()='Update' "
				+ "or @aria-label='Save' or @aria-label='Update'][not(@disabled)]");
		if (!BaseClass.getDriver().findElements(save).isEmpty()) {
			clickJs(save, waitSeconds);
		}
	}

	/** Set React-controlled input value via native setter + input event. */
	public static boolean setReactInputValue(By locator, String value, int waitSeconds) {
		try {
			WebElement el = PomElementManager.findVisible(locator, waitSeconds);
			JavascriptExecutor js = (JavascriptExecutor) BaseClass.getDriver();
			js.executeScript(
					"const el=arguments[0], v=arguments[1];"
							+ "const proto=Object.getPrototypeOf(el);"
							+ "const desc=Object.getOwnPropertyDescriptor(proto,'value')"
							+ "||Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value');"
							+ "if(desc&&desc.set){desc.set.call(el,v);}else{el.value=v;}"
							+ "el.dispatchEvent(new Event('input',{bubbles:true}));"
							+ "el.dispatchEvent(new Event('change',{bubbles:true}));",
					el, value == null ? "" : value);
			String actual = el.getAttribute("value");
			return actual != null && actual.equals(value);
		} catch (Exception e) {
			return false;
		}
	}

	public static void typeSlowly(By locator, String value, int waitSeconds) {
		WebElement el = PomElementManager.findVisible(locator, waitSeconds);
		el.click();
		el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		if (value == null || value.isEmpty()) {
			return;
		}
		for (char c : value.toCharArray()) {
			el.sendKeys(String.valueOf(c));
			sleep(25);
		}
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\"", "");
	}
}
