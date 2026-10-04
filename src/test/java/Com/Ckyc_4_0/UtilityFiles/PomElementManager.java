/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Re-fetches elements on each access to reduce stale-element failures in dynamic MUI pages.
 */
public final class PomElementManager {

	private static final Logger logger = LoggerFactory.getLogger(PomElementManager.class);
	private static final int MAX_RETRIES = 3;

	private PomElementManager() {
	}

	public static WebElement findVisible(By locator, int timeoutSeconds) {
		return withRetry(() -> BaseClass.waitUntilVisible(locator, timeoutSeconds));
	}

	public static WebElement findClickable(By locator, int timeoutSeconds) {
		return withRetry(() -> BaseClass.waitUntilClickable(locator, timeoutSeconds));
	}

	public static void click(By locator, int timeoutSeconds) {
		withRetryVoid(() -> BaseClass.clickElement(locator, timeoutSeconds));
	}

	/**
	 * Locator fallback chain: tries each candidate in order (primary first), returns on the
	 * first one that works. If the primary fails but a fallback saves it, logs a WARN so the
	 * team can fix the primary locator before it goes stale everywhere — the test itself does
	 * not fail. Opt-in: existing single-{@code By} page objects are unaffected; adopt this
	 * per-element only where a locator has actually proven to break (e.g. after a UI redesign),
	 * not as a blanket rewrite.
	 */
	public static WebElement findVisible(By[] candidates, int timeoutSeconds) {
		return firstThatWorks(candidates, i -> findVisible(candidates[i], timeoutSeconds));
	}

	/** Same fallback-chain behaviour as {@link #findVisible(By[], int)}, waiting for clickable. */
	public static WebElement findClickable(By[] candidates, int timeoutSeconds) {
		return firstThatWorks(candidates, i -> findClickable(candidates[i], timeoutSeconds));
	}

	/** Clicks the first candidate locator that resolves to a clickable element. */
	public static void click(By[] candidates, int timeoutSeconds) {
		findClickable(candidates, timeoutSeconds).click();
	}

	private static WebElement firstThatWorks(By[] candidates, Function<Integer, WebElement> attempt) {
		if (candidates == null || candidates.length == 0) {
			throw new IllegalArgumentException("At least one locator candidate is required");
		}
		RuntimeException last = null;
		for (int i = 0; i < candidates.length; i++) {
			try {
				WebElement element = attempt.apply(i);
				if (i > 0) {
					logger.warn("Primary locator failed — fallback #{} succeeded: {}", i, candidates[i]);
				}
				return element;
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw last;
	}

	public static String getText(By locator, int timeoutSeconds) {
		return withRetry(() -> findVisible(locator, timeoutSeconds).getText().trim());
	}

	public static String getAttribute(By locator, String attribute, int timeoutSeconds) {
		return withRetry(() -> {
			WebElement element = findVisible(locator, timeoutSeconds);
			String value = element.getAttribute(attribute);
			return value == null ? "" : value.trim();
		});
	}

	public static int getCountValue(By locator, int timeoutSeconds) {
		return parseCountText(getText(locator, timeoutSeconds), locator.toString());
	}

	/** Parses UI counts such as 2,550 or +29 into integers. */
	public static int parseCountText(String text, String source) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		String normalized = text.trim().replace(",", "");
		boolean negative = normalized.startsWith("-") || normalized.startsWith("−");
		String digitsOnly = normalized.replaceAll("[^0-9]", "");
		if (digitsOnly.isEmpty()) {
			logger.warn("Could not parse count '{}' from {}", text, source);
			return 0;
		}
		try {
			int value = Integer.parseInt(digitsOnly);
			return negative ? -value : value;
		} catch (NumberFormatException e) {
			logger.warn("Could not parse count '{}' from {}", text, source);
			return 0;
		}
	}

	public static boolean isPresent(By locator) {
		return BaseClass.getDriver() != null && !BaseClass.getDriver().findElements(locator).isEmpty();
	}

	public static boolean isSelectedToggle(By locator, int timeoutSeconds) {
		try {
			if (!isPresent(locator)) {
				return false;
			}
			WebElement element = BaseClass.getDriver().findElement(locator);
			return "true".equalsIgnoreCase(element.getAttribute("aria-pressed"));
		} catch (Exception e) {
			logger.debug("isSelectedToggle could not read aria-pressed for {}: {}", locator, e.getMessage());
			return false;
		}
	}

	private static <T> T withRetry(Supplier<T> action) {
		StaleElementReferenceException lastStale = null;
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				return action.get();
			} catch (StaleElementReferenceException e) {
				lastStale = e;
				logger.debug("Stale element on attempt {}/{} — re-fetching", attempt, MAX_RETRIES);
			}
		}
		throw lastStale != null ? lastStale : new RuntimeException("Action failed after retries");
	}

	private static void withRetryVoid(Runnable action) {
		withRetry(() -> {
			action.run();
			return null;
		});
	}

	public static <T> T withRetry(Function<Integer, T> action) {
		StaleElementReferenceException lastStale = null;
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				return action.apply(attempt);
			} catch (StaleElementReferenceException e) {
				lastStale = e;
				logger.debug("Stale element on attempt {}/{} — re-fetching", attempt, MAX_RETRIES);
			}
		}
		throw lastStale != null ? lastStale : new RuntimeException("Action failed after retries");
	}
}
