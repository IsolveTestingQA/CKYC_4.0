/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Type / select / read / error-text actions on DVS fields, addressed by Locator_ID (never by position).
 * Dropdowns are selected and read by their VISIBLE text ("05 - MINOR"): the hidden input value is an internal id
 * (05 - MINOR is stored as 2) and the option data-value is not the CKYC code either.
 */
public final class DvsFieldActions {

	private static final DvsLocators L = DvsLocators.get();

	private DvsFieldActions() {
	}

	private static WebDriver driver() {
		return BaseClass.getDriver();
	}

	private static int waitSeconds() {
		return DvsConfig.getInt("dvs.explicitWait", 8);
	}

	private static WebDriverWait waiter() {
		return new WebDriverWait(driver(), Duration.ofSeconds(waitSeconds()));
	}

	public static boolean present(String id) {
		return !driver().findElements(L.by(id)).isEmpty();
	}

	/** "05 - MINOR" -> "05"; text without " - " is returned unchanged. */
	public static String codeOf(String visibleText) {
		if (visibleText == null) {
			return "";
		}
		String t = visibleText.trim();
		int i = t.indexOf(" - ");
		return (i > 0 ? t.substring(0, i) : t).trim();
	}

	/** Compares two values of a field; dropdowns compare on the code before " - ". */
	public static boolean sameValue(String kind, String a, String b) {
		String x = a == null ? "" : a.trim();
		String y = b == null ? "" : b.trim();
		if ("dropdown".equals(kind)) {
			return codeOf(x).equalsIgnoreCase(codeOf(y));
		}
		return x.equals(y);
	}

	/** Opens the tab that owns the locator when the element is not on screen yet. */
	public static void ensureVisible(String id) {
		if (present(id)) {
			return;
		}
		DvsLocators.Meta meta = L.meta(id);
		if (meta != null) {
			openTab(meta.tab());
		}
		waiter().until(ExpectedConditions.presenceOfElementLocated(L.by(id)));
	}

	public static void openTab(String tab) {
		if (tab == null || tab.isBlank() || "Shell".equalsIgnoreCase(tab) || "Image panel".equalsIgnoreCase(tab)) {
			return;
		}
		long t0 = System.currentTimeMillis();
		By by = By.xpath("//button[@role='tab'][normalize-space()='" + tab + "']");
		WebElement el = waiter().until(ExpectedConditions.elementToBeClickable(by));
		boolean switched = !"true".equals(el.getAttribute("aria-selected"));
		if (switched) {
			click(el);
			sleep(400);
		}
		DvsStepLog.tab(tab);
		DvsStepLog.step("tab-switch").actual(switched ? "opened tab " + tab : "tab already open").toast(toastText())
				.result(DvsStepLog.PASS).log();
	}

	/** Visible tab names of the open record (Queue and Image are not record tabs). */
	public static List<String> recordTabNames() {
		List<String> names = new ArrayList<>();
		for (WebElement t : driver().findElements(L.by("COM_TAB_any"))) {
			String text = t.getText().trim();
			String first = text.contains("\n") ? text.substring(0, text.indexOf('\n')).trim() : text;
			String lower = first.toLowerCase(Locale.ROOT);
			if (!first.isEmpty() && !lower.contains("queue") && !lower.equals("image") && !names.contains(first)) {
				names.add(first);
			}
		}
		return names;
	}

	public static void set(String id, String value) {
		long t0 = System.currentTimeMillis();
		String before = "";
		try {
			ensureVisible(id);
			before = read(id);
			String kind = L.kind(id);
			WebElement el = driver().findElement(L.by(id));
			scrollTo(el);
			switch (kind) {
				case "dropdown" -> selectOption(el, value);
				case "autocomplete" -> pickAutocomplete(el, value);
				case "checkbox" -> setCheckbox(el, truthy(value));
				case "radio" -> {
					if (!el.isSelected()) {
						click(el);
					}
				}
				default -> typeText(el, value);
			}
			DvsStepLog.step("set-" + kind).field(id).before(before).entered(value).result(DvsStepLog.PASS).log();
		} catch (RuntimeException e) {
			DvsStepLog.step("set-" + L.kind(id)).field(id).before(before).entered(value)
					.actual("Exception: " + firstLine(e)).result(DvsStepLog.FAIL).log();
			throw e;
		}
	}

	/** Dropdown: the VISIBLE text. Checkbox / radio: "true" / "false". Others: the input value. */
	public static String read(String id) {
		ensureVisible(id);
		String kind = L.kind(id);
		WebElement el = driver().findElement(L.by(id));
		if ("dropdown".equals(kind)) {
			return el.getText().trim();
		}
		if ("checkbox".equals(kind) || "radio".equals(kind)) {
			return String.valueOf(el.isSelected());
		}
		String v = el.getAttribute("value");
		return v == null ? "" : v;
	}

	public static boolean isReadOnly(String id) {
		List<WebElement> els = driver().findElements(L.by(id));
		if (els.isEmpty()) {
			return true;
		}
		WebElement el = els.get(0);
		String kind = L.kind(id);
		if ("dropdown".equals(kind)) {
			String aria = el.getAttribute("aria-disabled");
			return "true".equals(aria);
		}
		return !el.isEnabled() || el.getAttribute("readonly") != null || el.getAttribute("disabled") != null;
	}

	/** Helper text under the field (MUI Mui-error), or "" when no validation message is shown. */
	public static String errorText(String id) {
		String name = L.inputName(id);
		By by;
		if (name.isEmpty()) {
			by = By.xpath("(" + L.xpath(id) + ")/ancestor::div[contains(@class,'MuiFormControl')][1]//p[contains(@class,'Mui-error')]");
		} else {
			String tag = "textarea".equals(L.kind(id)) ? "textarea" : "input";
			by = By.xpath("//" + tag + "[@name='" + name
					+ "']/ancestor::div[contains(@class,'MuiFormControl')][1]//p[contains(@class,'Mui-error')]");
		}
		List<WebElement> found = driver().findElements(by);
		StringBuilder sb = new StringBuilder();
		for (WebElement e : found) {
			String t = e.getText().trim();
			if (!t.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(" | ");
				}
				sb.append(t);
			}
		}
		return sb.toString();
	}

	public static void blur(String id) {
		List<WebElement> els = driver().findElements(L.by(id));
		if (!els.isEmpty() && !"dropdown".equals(L.kind(id))) {
			try {
				els.get(0).sendKeys(Keys.TAB);
			} catch (RuntimeException ignored) {
				// field not focusable (read-only / disabled) - nothing to blur
			}
		}
		sleep(DvsConfig.getInt("dvs.blurWaitMs", 500));
	}

	/** Option list of a dropdown as "visible text", in screen order. */
	public static List<String> optionCodes(String id) {
		ensureVisible(id);
		WebElement el = driver().findElement(L.by(id));
		scrollTo(el);
		click(el);
		By options = By.xpath("//ul[@role='listbox']//li");
		waiter().until(ExpectedConditions.presenceOfElementLocated(options));
		List<String> out = new ArrayList<>();
		for (WebElement li : driver().findElements(options)) {
			out.add(li.getText().trim());
		}
		driver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
		sleep(300);
		DvsStepLog.step("list-options").field(id).actual(String.join(" ; ", out)).result(DvsStepLog.INFO).log();
		return out;
	}

	/** True when a dialog is covering the screen (blocks typing into the next field). */
	public static boolean dialogOpen() {
		for (WebElement d : driver().findElements(L.by("COM_CHK_dialog"))) {
			if (d.isDisplayed()) {
				return true;
			}
		}
		return false;
	}

	public static String dialogText() {
		for (WebElement d : driver().findElements(L.by("COM_CHK_dialog"))) {
			if (d.isDisplayed()) {
				return d.getText().trim();
			}
		}
		return "";
	}

	public static void dismissDialog() {
		try {
			driver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
		} catch (RuntimeException ignored) {
			// nothing focused
		}
		sleep(300);
	}

	public static void dismissDialogIfOpen() {
		if (dialogOpen()) {
			dismissDialog();
		}
	}

	/** Text of every visible alert / snackbar (server errors arrive only as a toast). */
	public static String toastText() {
		StringBuilder sb = new StringBuilder();
		try {
			for (WebElement e : driver().findElements(L.by("COM_TOAST_alert"))) {
				String s = e.getText().trim();
				if (!s.isEmpty()) {
					if (sb.length() > 0) {
						sb.append(" | ");
					}
					sb.append(s);
				}
			}
		} catch (RuntimeException ignored) {
			// no browser / stale element
		}
		return sb.toString();
	}

	/** Waits briefly for a toast containing text (case-insensitive) and returns what was shown, or "". */
	public static String waitForToast(String containing, int seconds) {
		long end = System.currentTimeMillis() + seconds * 1000L;
		String last = "";
		while (System.currentTimeMillis() < end) {
			last = toastText();
			if (!last.isEmpty() && (containing == null || last.toLowerCase(Locale.ROOT).contains(containing.toLowerCase(Locale.ROOT)))) {
				return last;
			}
			sleep(250);
		}
		return last;
	}

	public static void clickBy(String locatorId) {
		long t0 = System.currentTimeMillis();
		try {
			WebElement el = waiter().until(ExpectedConditions.elementToBeClickable(L.by(locatorId)));
			click(el);
			DvsStepLog.step("click").field(locatorId).result(DvsStepLog.PASS).log();
		} catch (RuntimeException e) {
			DvsStepLog.step("click").field(locatorId).actual("Exception: " + firstLine(e)).result(DvsStepLog.FAIL).log();
			throw e;
		}
	}

	public static void typeInto(String locatorId, String text) {
		WebElement el = waiter().until(ExpectedConditions.elementToBeClickable(L.by(locatorId)));
		typeText(el, text);
		DvsStepLog.step("type").field(locatorId).entered(text).result(DvsStepLog.PASS).log();
	}

	private static void typeText(WebElement el, String value) {
		click(el);
		el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		if (!value.isEmpty()) {
			el.sendKeys(value);
		}
	}

	private static void setCheckbox(WebElement el, boolean wanted) {
		if (el.isSelected() != wanted) {
			click(el);
		}
	}

	private static void selectOption(WebElement combo, String value) {
		click(combo);
		String v = value.replace("'", "");
		By byText = By.xpath("//ul[@role='listbox']//li[starts-with(normalize-space(),'" + v + " - ') or normalize-space()='" + v + "']");
		By byData = By.xpath("//ul[@role='listbox']//li[@data-value='" + v + "']");
		WebElement option;
		try {
			option = waiter().until(ExpectedConditions.elementToBeClickable(byText));
		} catch (RuntimeException e) {
			option = waiter().until(ExpectedConditions.elementToBeClickable(byData));
		}
		click(option);
		sleep(200);
	}

	private static void pickAutocomplete(WebElement el, String value) {
		click(el);
		el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		el.sendKeys(value);
		By option = By.xpath("//ul[@role='listbox']//li[normalize-space()='" + value.replace("'", "") + "']");
		WebElement li = waiter().until(ExpectedConditions.elementToBeClickable(option));
		click(li);
		sleep(200);
	}

	private static boolean truthy(String v) {
		String s = v.trim().toLowerCase(Locale.ROOT);
		return s.equals("true") || s.equals("y") || s.equals("yes") || s.equals("1") || s.equals("checked");
	}

	private static void click(WebElement el) {
		try {
			el.click();
		} catch (RuntimeException e) {
			((JavascriptExecutor) driver()).executeScript("arguments[0].click();", el);
		}
	}

	private static void scrollTo(WebElement el) {
		((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
	}

	static String firstLine(Throwable t) {
		String m = t.getMessage();
		if (m == null) {
			return t.getClass().getSimpleName();
		}
		int nl = m.indexOf('\n');
		return nl > 0 ? m.substring(0, nl) : m;
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
