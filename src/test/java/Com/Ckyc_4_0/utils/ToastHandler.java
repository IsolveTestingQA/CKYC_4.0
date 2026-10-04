/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads MUI snackbar / SweetAlert / role=alert toaster text so Excel Actual can show the exact message.
 */
public final class ToastHandler {

	private static final Logger logger = LoggerFactory.getLogger(ToastHandler.class);

	public static final By TOAST_CONTAINERS = By.cssSelector(
			"[role='alert'], .MuiSnackbar-root, .MuiAlert-root, .Toastify__toast, "
					+ ".notistack-Snackbar, [class*='Snackbar'], [class*='toast']");

	private ToastHandler() {
	}

	public static String readIfPresent(WebDriver driver) {
		if (driver == null) {
			return "";
		}
		List<String> messages = new ArrayList<>();
		try {
			for (WebElement el : driver.findElements(TOAST_CONTAINERS)) {
				try {
					if (!el.isDisplayed()) {
						continue;
					}
					String text = el.getText();
					if (text != null && !text.isBlank()) {
						messages.add(text.trim().replaceAll("\\s+", " "));
					}
				} catch (Exception ignored) {
					// stale
				}
			}
			if (messages.isEmpty()) {
				Object js = ((JavascriptExecutor) driver).executeScript(
						"var nodes=[...document.querySelectorAll(\"[role='alert'],.MuiSnackbar-root,.MuiAlert-message\")];"
								+ "return nodes.map(n=>n.innerText||'').filter(t=>t.trim()).join(' | ');");
				if (js != null && !js.toString().isBlank()) {
					messages.add(js.toString().trim());
				}
			}
		} catch (Exception e) {
			logger.debug("Toast scan skipped: {}", e.getMessage());
		}
		return String.join(" | ", messages);
	}

	public static String waitAndRead(WebDriver driver, int waitMs) {
		long deadline = System.currentTimeMillis() + Math.max(waitMs, 200);
		String last = "";
		while (System.currentTimeMillis() < deadline) {
			last = readIfPresent(driver);
			if (!last.isBlank()) {
				return last;
			}
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return last;
	}

	public static void dismissIfPresent(WebDriver driver) {
		if (driver == null) {
			return;
		}
		try {
			List<By> closes = List.of(
					By.cssSelector("[role='alert'] button[aria-label='Close']"),
					By.xpath("//*[@role='alert']//button[normalize-space()='Close' or contains(@aria-label,'Close')]"),
					By.cssSelector(".MuiSnackbar-root button"));
			for (By close : closes) {
				for (WebElement btn : driver.findElements(close)) {
					if (btn.isDisplayed()) {
						try {
							btn.click();
						} catch (Exception e) {
							((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
						}
						return;
					}
				}
			}
		} catch (Exception e) {
			logger.debug("Toast dismiss skipped: {}", e.getMessage());
		}
	}
}
