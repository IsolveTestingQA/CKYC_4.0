/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import Com.Ckyc_4_0.pages.dashboard.DashboardPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Discovers date / constitution / application filter buttons from the live dashboard DOM.
 * Uses only what the client UI shows. If constitution group is empty → default Individual.
 * HTML reference: constitution values 0=ALL, 1=Individual, 2=Legal entity; app 01=NEW, 03=UPDATE.
 */
public final class DashboardFilterDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(DashboardFilterDiscovery.class);

	private static final Set<String> DATE_VALUES = Set.of("today", "weekly", "monthly", "aged");
	private static final Set<String> DATE_LABELS = Set.of("today", "weekly", "monthly", "overdue");
	private static final Set<String> APP_VALUES = Set.of("01", "03");
	private static final Set<String> APP_LABELS = Set.of("new", "update");

	/** Default when client UI has no constitution filter controls. */
	public static final DiscoveredFilter DEFAULT_CONSTITUTION_INDIVIDUAL = new DiscoveredFilter("Individual",
			"Individual", "1");

	/** Default when client UI has no date shortcut controls. */
	public static final DiscoveredFilter DEFAULT_DATE_TODAY = new DiscoveredFilter("today", "Today", "today");

	private DashboardFilterDiscovery() {
	}

	/**
	 * @param key         report/store key — prefers visible UI text (e.g. Individual, not "1")
	 * @param displayText visible button label
	 * @param valueAttr   HTML value (e.g. 0/1/2) used for stable click locator
	 * @param synthetic   true when invented as default (no UI button to click)
	 */
	public record DiscoveredFilter(String key, String displayText, String valueAttr, boolean synthetic) {

		public DiscoveredFilter(String key, String displayText, String valueAttr) {
			this(key, displayText, valueAttr, false);
		}

		public By locator(By groupButtonLocator) {
			if (valueAttr != null && !valueAttr.isBlank()) {
				return By.xpath(groupRootXpath(groupButtonLocator) + "//button[@value='" + escape(valueAttr) + "']");
			}
			String upper = displayText == null ? "" : displayText.trim().toUpperCase(Locale.ENGLISH);
			return By.xpath(groupRootXpath(groupButtonLocator) + "//button[translate(normalize-space(.),"
					+ "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='" + escape(upper) + "']");
		}
	}

	public static List<DiscoveredFilter> discoverDateFilters(WebDriver driver) {
		return discoverFromGroup(driver, DashboardPage.DATE_FILTER_BUTTONS, "date");
	}

	public static List<DiscoveredFilter> discoverConstitutionFilters(WebDriver driver) {
		List<DiscoveredFilter> found = discoverFromGroup(driver, DashboardPage.CONSTITUTION_FILTER_BUTTONS,
				"constitution");
		if (!found.isEmpty()) {
			return found;
		}
		found = discoverLooseConstitutionButtons(driver);
		return found;
	}

	public static List<DiscoveredFilter> discoverApplicationTypeFilters(WebDriver driver) {
		return discoverFromGroup(driver, DashboardPage.APPLICATION_TYPE_FILTER_BUTTONS, "application");
	}

	/**
	 * If nothing discovered → return default Individual (do not fail).
	 * If UI has options → return only those options.
	 */
	public static List<DiscoveredFilter> resolveConstitutionFiltersOrDefault(WebDriver driver) {
		List<DiscoveredFilter> found = discoverConstitutionFilters(driver);
		if (!found.isEmpty()) {
			return found;
		}
		logger.warn("No constitution filters on UI — continuing with default: Individual");
		return List.of(new DiscoveredFilter("Individual", "Individual", "1", true));
	}

	public static List<DiscoveredFilter> resolveDateFiltersOrDefault(WebDriver driver) {
		List<DiscoveredFilter> found = discoverDateFilters(driver);
		if (!found.isEmpty()) {
			return found;
		}
		logger.warn("No date filters on UI — continuing with default: Today");
		return List.of(new DiscoveredFilter("today", "Today", "today", true));
	}

	private static List<DiscoveredFilter> discoverFromGroup(WebDriver driver, By buttonsLocator, String type) {
		List<DiscoveredFilter> result = new ArrayList<>();
		Map<String, DiscoveredFilter> unique = new LinkedHashMap<>();
		if (driver == null) {
			return result;
		}
		try {
			for (WebElement button : driver.findElements(buttonsLocator)) {
				try {
					if (!button.isDisplayed() || !button.isEnabled()) {
						continue;
					}
					String text = safe(button.getText());
					String value = safe(button.getAttribute("value"));
					if (text.isEmpty() && value.isEmpty()) {
						continue;
					}
					// Prefer visible label as report key (Individual), keep value for click (1)
					String key = !text.isEmpty() ? text : value;
					DiscoveredFilter filter = new DiscoveredFilter(key, text.isEmpty() ? value : text, value, false);
					unique.putIfAbsent(key.toLowerCase(Locale.ENGLISH), filter);
				} catch (Exception ignored) {
					// stale — skip
				}
			}
		} catch (Exception e) {
			logger.warn("Could not discover {} filters: {}", type, e.getMessage());
		}
		result.addAll(unique.values());
		logger.info("Discovered {} {} filter(s) from UI: {}", result.size(), type, summarize(result));
		return result;
	}

	private static List<DiscoveredFilter> discoverLooseConstitutionButtons(WebDriver driver) {
		List<DiscoveredFilter> result = new ArrayList<>();
		Map<String, DiscoveredFilter> unique = new LinkedHashMap<>();
		try {
			List<WebElement> buttons = driver.findElements(
					By.xpath("//button[(contains(@class,'MuiToggleButton') or @aria-pressed) and normalize-space()]"));
			for (WebElement button : buttons) {
				if (!button.isDisplayed()) {
					continue;
				}
				String text = safe(button.getText());
				String value = safe(button.getAttribute("value"));
				String lowerVal = value.toLowerCase(Locale.ENGLISH);
				String lowerText = text.toLowerCase(Locale.ENGLISH);
				if (DATE_VALUES.contains(lowerVal) || DATE_LABELS.contains(lowerText)) {
					continue;
				}
				if (APP_VALUES.contains(lowerVal) || APP_LABELS.contains(lowerText)) {
					continue;
				}
				if (text.isEmpty() && value.isEmpty()) {
					continue;
				}
				String key = !text.isEmpty() ? text : value;
				unique.putIfAbsent(key.toLowerCase(Locale.ENGLISH),
						new DiscoveredFilter(key, text.isEmpty() ? value : text, value, false));
			}
		} catch (Exception e) {
			logger.warn("Loose constitution discovery failed: {}", e.getMessage());
		}
		result.addAll(unique.values());
		logger.info("Discovered {} constitution filter(s) via fallback: {}", result.size(), summarize(result));
		return result;
	}

	public static DiscoveredFilter findByKeyOrText(List<DiscoveredFilter> filters, String requested) {
		if (requested == null || requested.isBlank() || filters == null) {
			return null;
		}
		String want = requested.trim().toLowerCase(Locale.ENGLISH);
		for (DiscoveredFilter f : filters) {
			if (want.equals(f.key().toLowerCase(Locale.ENGLISH))
					|| want.equals(f.displayText().toLowerCase(Locale.ENGLISH))
					|| want.equals(safe(f.valueAttr()).toLowerCase(Locale.ENGLISH))) {
				return f;
			}
		}
		for (DiscoveredFilter f : filters) {
			String display = f.displayText().toLowerCase(Locale.ENGLISH);
			String key = f.key().toLowerCase(Locale.ENGLISH);
			if (display.contains(want) || want.contains(display) || key.contains(want) || want.contains(key)) {
				return f;
			}
		}
		return null;
	}

	private static String summarize(List<DiscoveredFilter> filters) {
		List<String> parts = new ArrayList<>();
		for (DiscoveredFilter f : filters) {
			String extra = f.valueAttr().isEmpty() ? "" : " [value=" + f.valueAttr() + "]";
			if (f.synthetic()) {
				extra += " [default]";
			}
			parts.add(f.displayText() + extra);
		}
		return parts.toString();
	}

	private static String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("'", "").replace("\"", "");
	}

	private static String groupRootXpath(By groupButtonLocator) {
		String raw = groupButtonLocator.toString();
		if (raw.startsWith("By.xpath: ")) {
			String xp = raw.substring("By.xpath: ".length());
			int idx = xp.lastIndexOf("//button");
			if (idx > 0) {
				return xp.substring(0, idx);
			}
			return xp;
		}
		return "//div[@aria-label='Constitution type filter']";
	}

	public static void logPageFilterSnapshot(WebDriver driver) {
		if (driver == null) {
			return;
		}
		logger.info("Filter discovery on URL: {}", driver.getCurrentUrl());
		discoverDateFilters(driver);
		discoverConstitutionFilters(driver);
		discoverApplicationTypeFilters(driver);
	}
}
