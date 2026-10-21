/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A screenshot normally shows only the part of the page that happens to be on screen. For a FAIL the failing place
 * (a summary card, a count badge, a field) must be in the picture: the caller sets the element(s) with {@link #set},
 * and just before the capture {@link #bringIntoView} scrolls the first one that exists into the middle of the
 * viewport (page down / page up as a fallback for nested scrollers) and outlines it in red so it is easy to spot.
 * The focus is per thread and is cleared by the caller ({@link #clear}) as soon as the failure is logged.
 */
public final class ScreenshotFocus {

	private static final Logger logger = LoggerFactory.getLogger(ScreenshotFocus.class);
	private static final ThreadLocal<List<By>> FOCUS = ThreadLocal.withInitial(ArrayList::new);
	private static final String OUTLINE_ATTR = "data-shot-focus";
	private static final ThreadLocal<String> BANNER = new ThreadLocal<>();
	private static final String BANNER_ID = "ckyc-shot-banner";

	private ScreenshotFocus() {
	}

	/** Elements that must be visible in the next screenshot; the first one found is used. */
	public static void set(By... locators) {
		List<By> list = FOCUS.get();
		list.clear();
		for (By b : locators) {
			if (b != null) {
				list.add(b);
			}
		}
	}

	public static void clear() {
		FOCUS.get().clear();
		BANNER.remove();
	}

	/** A line of text (e.g. "NEW / ALL | Weekly | BUG ...") printed across the top of the next screenshot. */
	public static void banner(String text) {
		BANNER.set(text);
	}

	public static boolean isSet() {
		return !FOCUS.get().isEmpty();
	}

	/** Scrolls the focus element into view and outlines it. No-op when nothing is set. */
	public static void bringIntoView(WebDriver driver) {
		if (driver == null) {
			return;
		}
		String banner = BANNER.get();
		if (banner != null && !banner.isBlank()) {
			try {
				((JavascriptExecutor) driver).executeScript("var o=document.getElementById('" + BANNER_ID + "');if(o){o.remove();}"
						+ "var d=document.createElement('div');d.id='" + BANNER_ID + "';d.textContent=arguments[0];"
						+ "d.style.cssText='position:fixed;top:0;left:0;right:0;z-index:2147483647;background:#b42318;color:#fff;"
						+ "font:600 14px Segoe UI,Arial,sans-serif;padding:6px 12px;text-align:center;';"
						+ "document.body.appendChild(d);", banner);
			} catch (RuntimeException e) {
				logger.debug("Screenshot banner skipped: {}", e.getMessage());
			}
		}
		if (FOCUS.get().isEmpty()) {
			return;
		}
		try {
			WebElement target = null;
			for (By by : FOCUS.get()) {
				List<WebElement> found = driver.findElements(by);
				if (!found.isEmpty()) {
					target = found.get(0);
					break;
				}
			}
			if (target == null) {
				logger.info("Screenshot focus: element not on the page - screenshot taken as is");
				return;
			}
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'center'});", target);
			pause(250);
			if (!fullyVisible(js, target)) {
				pageThrough(driver, js, target);
			}
			js.executeScript("var e=arguments[0]; e.setAttribute('" + OUTLINE_ATTR + "', e.style.outline||'none');"
					+ "e.style.outline='3px solid #e53935'; e.style.outlineOffset='2px';", target);
			pause(150);
			logger.info("Screenshot focus: failing element brought into view (visible={})", fullyVisible(js, target));
		} catch (RuntimeException e) {
			logger.warn("Screenshot focus skipped: {}", e.getMessage());
		}
	}

	/** Removes the outline added by {@link #bringIntoView} (the page keeps its scroll position). */
	public static void release(WebDriver driver) {
		if (driver == null) {
			return;
		}
		try {
			((JavascriptExecutor) driver).executeScript("var b=document.getElementById('" + BANNER_ID + "');if(b){b.remove();}");
		} catch (RuntimeException ignored) {
			// page already navigated away
		}
		try {
			((JavascriptExecutor) driver).executeScript("var l=document.querySelectorAll('[" + OUTLINE_ATTR + "]');"
					+ "for(var i=0;i<l.length;i++){var o=l[i].getAttribute('" + OUTLINE_ATTR + "');"
					+ "l[i].style.outline=(o==='none'?'':o);l[i].removeAttribute('" + OUTLINE_ATTR + "');}");
		} catch (RuntimeException ignored) {
			// page already navigated away
		}
	}

	private static boolean fullyVisible(JavascriptExecutor js, WebElement e) {
		Object r = js.executeScript("var r=arguments[0].getBoundingClientRect();"
				+ "var h=window.innerHeight||document.documentElement.clientHeight;"
				+ "var w=window.innerWidth||document.documentElement.clientWidth;"
				+ "return r.width>0&&r.height>0&&r.top>=0&&r.left>=0&&r.bottom<=h&&r.right<=w;", e);
		return Boolean.TRUE.equals(r);
	}

	/** Fallback: start at the top and page down (then back up) until the element is inside the viewport. */
	private static void pageThrough(WebDriver driver, JavascriptExecutor js, WebElement target) {
		js.executeScript("window.scrollTo(0,0);");
		for (int i = 0; i < 15; i++) {
			if (fullyVisible(js, target)) {
				return;
			}
			try {
				driver.findElement(By.tagName("body")).sendKeys(Keys.PAGE_DOWN);
			} catch (RuntimeException e) {
				js.executeScript("window.scrollBy(0, window.innerHeight*0.8);");
			}
			pause(200);
			js.executeScript("arguments[0].scrollIntoView({block:'nearest',inline:'nearest'});", target);
		}
		for (int i = 0; i < 15 && !fullyVisible(js, target); i++) {
			js.executeScript("window.scrollBy(0, -window.innerHeight*0.8);");
			pause(200);
		}
	}

	private static void pause(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
