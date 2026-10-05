/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsLocators;

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

/** Type / select / read / error-text actions on DVS fields, addressed by Locator_ID (never by position). */
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
		By by = By.xpath("//button[@role='tab'][normalize-space()='" + tab + "']");
		WebElement el = waiter().until(ExpectedConditions.elementToBeClickable(by));
		if (!"true".equals(el.getAttribute("aria-selected"))) {
			click(el);
			sleep(400);
		}
	}

	public static void set(String id, String value) {
		ensureVisible(id);
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
	}

	public static String read(String id) {
		ensureVisible(id);
		String kind = L.kind(id);
		if ("dropdown".equals(kind)) {
			String name = L.inputName(id);
			if (!name.isEmpty()) {
				List<WebElement> hidden = driver().findElements(By.xpath("//input[@name='" + name + "']"));
				if (!hidden.isEmpty()) {
					String v = hidden.get(0).getAttribute("value");
					return v == null ? "" : v;
				}
			}
			return driver().findElement(L.by(id)).getText().trim();
		}
		WebElement el = driver().findElement(L.by(id));
		if ("checkbox".equals(kind) || "radio".equals(kind)) {
			return String.valueOf(el.isSelected());
		}
		String v = el.getAttribute("value");
		return v == null ? "" : v;
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

	/** Option codes (data-value) of a dropdown, in screen order. */
	public static List<String> optionCodes(String id) {
		ensureVisible(id);
		WebElement el = driver().findElement(L.by(id));
		scrollTo(el);
		click(el);
		By options = By.xpath("//ul[@role='listbox']//li");
		waiter().until(ExpectedConditions.presenceOfElementLocated(options));
		List<String> out = new ArrayList<>();
		for (WebElement li : driver().findElements(options)) {
			String code = li.getAttribute("data-value");
			out.add((code == null ? "" : code) + " - " + li.getText().trim());
		}
		driver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
		sleep(300);
		return out;
	}

	/** True when a dialog is covering the screen (blocks typing into the next field). */
	public static boolean dialogOpen() {
		for (WebElement d : driver().findElements(By.xpath("//div[@role='dialog']"))) {
			if (d.isDisplayed()) {
				return true;
			}
		}
		return false;
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

	public static String toastText() {
		List<WebElement> t = driver().findElements(By.xpath("//div[contains(@class,'MuiSnackbar') or @role='alert']"));
		for (WebElement e : t) {
			String s = e.getText().trim();
			if (!s.isEmpty()) {
				return s;
			}
		}
		return "";
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
		By byCode = By.xpath("//ul[@role='listbox']//li[@data-value='" + value + "']");
		By byText = By.xpath("//ul[@role='listbox']//li[normalize-space()='" + value + "']");
		WebElement option;
		try {
			option = waiter().until(ExpectedConditions.elementToBeClickable(byCode));
		} catch (RuntimeException e) {
			option = waiter().until(ExpectedConditions.elementToBeClickable(byText));
		}
		click(option);
		sleep(200);
	}

	private static void pickAutocomplete(WebElement el, String value) {
		click(el);
		el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		el.sendKeys(value);
		By option = By.xpath("//ul[@role='listbox']//li[normalize-space()='" + value + "']");
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

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
