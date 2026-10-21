/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import Com.Ckyc_4_0.pages.login.LoginPage;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.SoftAssertManager;
import Com.Ckyc_4_0.constants.FrameworkConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Handles optional overlays:
 * - Active session MUI dialog → "Log out other session & continue"
 * - Chrome / Google Password Manager bubbles after login → Close / Escape
 * - App blockers that stop the next step → interactive Stop / Retry (or auto-dismiss)
 * Never sends Escape on the login page (that cancels the Active session dialog).
 */
public final class PopupHandler {

	private static final Logger logger = LoggerFactory.getLogger(PopupHandler.class);

	public enum BlockerChoice {
		RETRY, STOP, NONE, SKIP_SCENARIO, SKIP_FEATURE, CONTINUE_WAIT
	}

	/** Thrown when user chooses Stop — Hooks flushes reports and quits the browser. */
	public static final class AbortRunException extends RuntimeException {
		public AbortRunException(String message) {
			super(message);
		}
	}

	/**
	 * A mandatory positive business transaction could not be completed after all
	 * valid candidates were tried. This stops only the current module/feature.
	 */
	public static final class ModuleBlockerException extends RuntimeException {
		private final String module;
		private final String testData;

		public ModuleBlockerException(String module, String message, String testData) {
			super(message);
			this.module = module == null ? "Current module" : module;
			this.testData = testData == null ? "" : testData;
		}

		public String module() {
			return module;
		}

		public String testData() {
			return testData;
		}
	}

	/** SweetAlert2 / app blocker containers. */
	private static final By SWEET_ALERT = By.cssSelector(
			".swal2-container, .swal2-popup, .swal2-shown, div.swal2-modal");

	private static final By GENERIC_BLOCKER_DIALOG = By.xpath(
			"//*[@role='alertdialog' or (@role='dialog' and contains(@class,'swal')) "
					+ "or contains(@class,'swal2-popup')]"
					+ "[not(@aria-labelledby='session-active-dialog-title')]"
					+ "[not(.//span[normalize-space()='Active session detected'])]"
					+ "[not(@id='ckyc-blocker-prompt')]");

	private static final By BLOCKER_DISMISS_BUTTON = By.xpath(
			"//div[contains(@class,'swal2-popup') or contains(@class,'swal2-container') "
					+ "or @role='alertdialog' or contains(@class,'swal')]"
					+ "[not(@id='ckyc-blocker-prompt')]"
					+ "//button[normalize-space()='OK' or normalize-space()='Ok' or normalize-space()='Confirm' "
					+ "or normalize-space()='Yes' or normalize-space()='Close' or normalize-space()='Continue' "
					+ "or contains(@class,'swal2-confirm')]");

	/** Page-level password / breach UI text if Chrome ever injects into DOM. */
	private static final By PASSWORD_UI_TEXT = By.xpath(
			"//*[contains(.,'Check your saved passwords') or contains(.,'Google Password Manager') "
					+ "or contains(.,'found in a data breach') or contains(.,'Save password') "
					+ "or contains(.,'password you just used')]");

	private static final By PASSWORD_UI_CLOSE = By.xpath(
			"//button[normalize-space()='Close' or normalize-space()='Not now' or normalize-space()='No thanks' "
					+ "or normalize-space()='Never' or normalize-space()='Cancel']"
					+ " | //*[@role='dialog' or @role='alertdialog']"
					+ "//button[contains(.,'Close') or contains(.,'Not now') or contains(.,'No thanks')]");

	private PopupHandler() {
	}

	public static boolean handlePostLoginOverlays(WebDriver driver) {
		return handleActiveSessionIfPresent(driver, FrameworkConstants.OPTIONAL_DIALOG_WAIT_SECONDS);
	}

	public static boolean isActiveSessionDialogVisible(WebDriver driver) {
		if (driver == null) {
			return false;
		}
		try {
			for (WebElement el : driver.findElements(LoginPage.ACTIVE_SESSION_DIALOG)) {
				if (el.isDisplayed()) {
					return true;
				}
			}
			for (WebElement el : driver.findElements(LoginPage.LOGOUT_OTHER_SESSION_CONTINUE)) {
				if (el.isDisplayed()) {
					return true;
				}
			}
		} catch (Exception ignored) {
			// not visible
		}
		return false;
	}

	/**
	 * Clicks "Log out other session & continue" when the Active session dialog is shown.
	 */
	public static boolean handleActiveSessionIfPresent(WebDriver driver, int waitSeconds) {
		if (driver == null) {
			return false;
		}
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(waitSeconds, 1)));
			wait.pollingEvery(Duration.ofMillis(200));

			wait.until(ExpectedConditions.or(
					ExpectedConditions.visibilityOfElementLocated(LoginPage.ACTIVE_SESSION_DIALOG),
					ExpectedConditions.elementToBeClickable(LoginPage.LOGOUT_OTHER_SESSION_CONTINUE)));

			WebElement continueBtn = wait.until(
					ExpectedConditions.elementToBeClickable(LoginPage.LOGOUT_OTHER_SESSION_CONTINUE));
			String buttonText = safeText(continueBtn);
			clickSafely(driver, continueBtn);
			logger.info("Active session dialog handled — clicked: {}", buttonText);

			try {
				wait.until(ExpectedConditions.invisibilityOfElementLocated(LoginPage.ACTIVE_SESSION_DIALOG));
			} catch (TimeoutException ignored) {
				try {
					wait.until(ExpectedConditions.invisibilityOfElementLocated(LoginPage.LOGOUT_OTHER_SESSION_CONTINUE));
				} catch (TimeoutException ignored2) {
					logger.debug("Active session dialog still present after click (continuing)");
				}
			}
			return true;
		} catch (TimeoutException e) {
			logger.debug("Active session dialog not shown within {}s — continuing", waitSeconds);
			return false;
		} catch (Exception e) {
			logger.warn("Active session dialog handling skipped: {}", e.getMessage());
			return false;
		}
	}

	/** True when a SweetAlert / generic blocking dialog is visible (not Active session / our prompt). */
	public static boolean isBlockerVisible(WebDriver driver) {
		if (driver == null || isActiveSessionDialogVisible(driver)) {
			return false;
		}
		try {
			for (By locator : List.of(SWEET_ALERT, GENERIC_BLOCKER_DIALOG)) {
				for (WebElement el : driver.findElements(locator)) {
					if (el.isDisplayed() && !"ckyc-blocker-prompt".equals(el.getAttribute("id"))) {
						return true;
					}
				}
			}
		} catch (Exception ignored) {
			// not a blocker
		}
		return false;
	}

	/**
	 * If a SweetAlert / app blocker is shown: click OK/Confirm if available, else wait until gone.
	 */
	public static boolean handleBlockerIfPresent(WebDriver driver, int waitSeconds) {
		if (driver == null || !isBlockerVisible(driver)) {
			return false;
		}
		int waitSec = Math.max(waitSeconds, 1);
		logger.info("Blocker / SweetAlert detected — waiting up to {}s and dismissing if possible", waitSec);
		try {
			List<WebElement> buttons = driver.findElements(BLOCKER_DISMISS_BUTTON);
			boolean clicked = false;
			for (WebElement btn : buttons) {
				try {
					if (btn.isDisplayed() && btn.isEnabled()) {
						clickSafely(driver, btn);
						clicked = true;
						logger.info("Dismissed blocker via button: {}", safeText(btn));
						break;
					}
				} catch (Exception ignored) {
					// try next
				}
			}

			long deadline = System.currentTimeMillis() + (waitSec * 1000L);
			while (System.currentTimeMillis() < deadline) {
				if (!isBlockerVisible(driver)) {
					logger.info("Blocker cleared{}", clicked ? " after dismiss" : " after wait");
					return true;
				}
				sleepQuietly(200);
			}
			logger.warn("Blocker still visible after {}s", waitSec);
			return !isBlockerVisible(driver);
		} catch (Exception e) {
			logger.warn("Blocker handling skipped: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Runs the step. If something blocks the next action (app SweetAlert / click intercept):
	 * shows Stop / Retry on the page (when enabled), or auto-dismisses and retries once.
	 */
	public static void runWithBlockerRetry(WebDriver driver, Runnable action) {
		if (action == null) {
			return;
		}

		if (isBlockerVisible(driver)) {
			resolveBlockerWithUserOrAuto(driver,
					"A popup/blocker is open and may stop the next step.",
					"Close or wait for the app dialog if needed, then choose Retry — or Stop to fail this step.");
		}

		try {
			action.run();
		} catch (Throwable first) {
			boolean intercepted = first instanceof ElementClickInterceptedException
					|| (first.getMessage() != null && first.getMessage().toLowerCase().contains("intercepted"));
			boolean blockerNow = isBlockerVisible(driver);

			if (!blockerNow && !intercepted) {
				sneakyThrow(first);
			}
			if (intercepted && !blockerNow) {
				/*
				 * MUI menus/drawers keep a short-lived invisible backdrop during their
				 * closing transition. This is transient automation timing, not an
				 * application blocker and must not ask the user to Stop/Retry.
				 */
				logger.info("Transient click interception detected — closing/waiting for MUI backdrop, then retrying");
				try {
					new Actions(driver).sendKeys(Keys.ESCAPE).perform();
					new WebDriverWait(driver, Duration.ofSeconds(4)).until(
							ExpectedConditions.invisibilityOfElementLocated(
									By.cssSelector(".MuiBackdrop-root, .MuiModal-backdrop")));
				} catch (Exception ignored) {
					sleepQuietly(500);
				}
				try {
					action.run();
					logger.info("Automatic retry after transient backdrop succeeded");
					return;
				} catch (Throwable second) {
					sneakyThrow(second);
				}
			}

			String detail = first.getMessage() == null ? first.toString() : first.getMessage();
			logger.warn("Step blocked — offering Stop/Retry. Cause: {}", detail);

			BlockerChoice choice = resolveBlockerWithUserOrAuto(driver,
					"Step was blocked — next action could not continue.",
					detail);

			if (choice == BlockerChoice.STOP) {
				throw new IllegalStateException("Stopped by user after blocker: " + detail, first);
			}

			handleBlockerIfPresent(driver, FrameworkConstants.BLOCKER_WAIT_SECONDS);
			if (isActiveSessionDialogVisible(driver)) {
				handleActiveSessionIfPresent(driver, FrameworkConstants.OPTIONAL_DIALOG_WAIT_SECONDS);
			}

			try {
				action.run();
				logger.info("Retry after blocker succeeded");
			} catch (Throwable second) {
				sneakyThrow(second);
			}
		}
	}

	/**
	 * Interactive Stop/Retry when enabled; otherwise auto-dismiss. Returns the choice taken.
	 */
	private static BlockerChoice resolveBlockerWithUserOrAuto(WebDriver driver, String title, String detail) {
		if (shouldShowInteractivePrompt(driver)) {
			BlockerChoice choice = showInteractiveBlockerPrompt(driver, title, detail);
			removeInteractivePrompt(driver);
			if (choice == BlockerChoice.STOP) {
				throw new AbortRunException("Stopped by user: " + title + " - " + detail);
			}
			if (choice == BlockerChoice.SKIP_SCENARIO) {
				String reason = "Skipped (popup): " + title + " - " + detail;
				SoftAssertManager.markSkipScenario(reason);
				throw new org.testng.SkipException("Scenario skipped (popup): " + reason);
			}
			if (choice == BlockerChoice.SKIP_FEATURE) {
				String reason = "Feature skipped (popup): " + title + " - " + detail;
				SoftAssertManager.markSkipFeature(FailureContext.currentFeature(), reason);
				throw new org.testng.SkipException(reason);
			}
			handleBlockerIfPresent(driver, FrameworkConstants.BLOCKER_WAIT_SECONDS);
			return choice == BlockerChoice.NONE ? BlockerChoice.RETRY : choice;
		}
		handleBlockerIfPresent(driver, FrameworkConstants.BLOCKER_WAIT_SECONDS);
		return BlockerChoice.RETRY;
	}

	/** Every manual popup waits at most 60 seconds; no answer = skip the scenario and continue. */
	private static int popupWaitSeconds() {
		int configured = ConfigReader.getInteractiveBlockerWaitSeconds();
		return configured <= 0 ? 60 : Math.min(configured, 60);
	}

	private static boolean shouldShowInteractivePrompt(WebDriver driver) {
		if (!ConfigReader.interactiveBlockerPrompt() || driver == null) {
			return false;
		}
		// Unattended / headless runs should not wait for a human click
		if (Boolean.parseBoolean(ConfigReader.get("headless", "false"))) {
			return false;
		}
		return true;
	}

	/**
	 * Decision popup for "the next action cannot continue" (app dialog / click intercepted). Four answers, each explained
	 * on the popup: Retry, Skip this case, Skip this feature, Stop everything.
	 */
	public static BlockerChoice showInteractiveBlockerPrompt(WebDriver driver, String title, String detail) {
		if (driver == null) {
			return BlockerChoice.NONE;
		}
		String answer = askDecision(driver, "Automation blocker - the next step cannot continue", "#b45309", title, detail,
				null, "Retry",
				"close or fix the popup / page by hand first, then click Retry - the same step runs again.");
		if (answer == null) {
			logger.warn("Blocker popup: no answer in {}s - skipping this case and continuing", popupWaitSeconds());
			return BlockerChoice.SKIP_SCENARIO;
		}
		switch (answer) {
		case "continue":
			logger.info("User chose Retry on blocker popup");
			return BlockerChoice.RETRY;
		case "skip_feature":
			return BlockerChoice.SKIP_FEATURE;
		case "stop":
			return BlockerChoice.STOP;
		case "skip_scenario":
		default:
			return BlockerChoice.SKIP_SCENARIO;
		}
	}

	private static void removeInteractivePrompt(WebDriver driver) {
		try {
			((JavascriptExecutor) driver).executeScript(
					"var el=document.getElementById('ckyc-blocker-prompt'); if(el){el.remove();}"
							+ "var el2=document.getElementById('ckyc-missing-ui-prompt'); if(el2){el2.remove();}"
							+ " window.__ckycBlockerChoice=null;");
		} catch (Exception ignored) {
			// ignore
		}
	}

	/**
	 * When an expected page / button / field is missing: ONE popup with four answers - Continue (fix by hand, then the
	 * step runs again), Skip this case, Skip this feature, Stop everything. The popup names the feature, scenario,
	 * step, Functionality method, Page class and the missing element. No answer in 60 s = skip this case.
	 */
	public static BlockerChoice promptMissingExpectedUi(WebDriver driver, String whatIsMissing, String details) {
		return promptMissingExpectedUi(driver, whatIsMissing, details, null);
	}

	public static BlockerChoice promptMissingExpectedUi(WebDriver driver, String whatIsMissing, String details,
			Throwable cause) {
		if (!shouldShowInteractivePrompt(driver)) {
			return BlockerChoice.NONE;
		}
		BlockerChoice choice = showMissingUiPrompt(driver, whatIsMissing, details, cause);
		removeInteractivePrompt(driver);
		return choice;
	}

	/** Same four answers for a module that cannot be created (mandatory positive failed). */
	public static BlockerChoice promptModuleBlocker(WebDriver driver, String title, String details) {
		if (!shouldShowInteractivePrompt(driver)) {
			return BlockerChoice.SKIP_FEATURE;
		}
		BlockerChoice choice = showMissingUiPrompt(driver, title, details, null);
		removeInteractivePrompt(driver);
		return choice == BlockerChoice.NONE ? BlockerChoice.SKIP_FEATURE : choice;
	}

	private static BlockerChoice showMissingUiPrompt(WebDriver driver, String title, String detail, Throwable cause) {
		if (driver == null) {
			return BlockerChoice.NONE;
		}
		String answer = askDecision(driver, "Expected page / button / field not available", "#b91c1c", title, detail,
				cause, "Continue",
				"fix the page by hand (log in, open the right page, close a dialog) and then click Continue - the same "
						+ "step runs again right away.");
		if (answer == null) {
			logger.warn("Missing-UI popup: no answer in {}s - skipping this case and continuing", popupWaitSeconds());
			return BlockerChoice.SKIP_SCENARIO;
		}
		switch (answer) {
		case "continue":
			return BlockerChoice.CONTINUE_WAIT;
		case "skip_feature":
			return BlockerChoice.SKIP_FEATURE;
		case "stop":
			return BlockerChoice.STOP;
		case "skip_scenario":
		default:
			return BlockerChoice.SKIP_SCENARIO;
		}
	}

	/**
	 * Draws the decision popup on the page and waits for a click. Returns continue / skip_scenario / skip_feature / stop,
	 * or null when nobody answered within the wait.
	 */
	private static String askDecision(WebDriver driver, String header, String color, String title, String detail,
			Throwable cause, String continueLabel, String continueHelp) {
		int waitSec = popupWaitSeconds();
		String url = safeUrl(driver);
		org.json.JSONArray rows = new org.json.JSONArray();
		for (FailureContext.Row r : FailureContext.describe(cause, url, null)) {
			if (r.value() != null && !r.value().isBlank()) {
				rows.put(new org.json.JSONArray().put(r.label()).put(r.value()).put(r.mono()));
				logger.warn("Decision popup | {}: {}", r.label(), r.value());
			}
		}
		org.json.JSONObject cfg = new org.json.JSONObject();
		cfg.put("header", header);
		cfg.put("color", color);
		cfg.put("title", title == null ? "" : title);
		String cleanDetail = detail == null ? "" : detail;
		for (String noise : new String[] { "For documentation on this error", "Build info:", "System info:", "(Session info:" }) {
			int cut = cleanDetail.indexOf(noise);
			if (cut > 0) {
				cleanDetail = cleanDetail.substring(0, cut).strip();
			}
		}
		cfg.put("detail", cleanDetail);
		cfg.put("rows", rows);
		cfg.put("continueLabel", continueLabel);
		cfg.put("continueHelp", continueHelp);
		cfg.put("secs", waitSec);

		String script = "window.__ckycBlockerChoice = null; (function(cfg){"
				+ "['ckyc-blocker-prompt','ckyc-missing-ui-prompt'].forEach(function(i){var o=document.getElementById(i);if(o){o.remove();}});"
				+ "function el(t,c,x){var e=document.createElement(t);if(c){e.style.cssText=c;}if(x!=null){e.textContent=x;}return e;}"
				+ "var w=el('div','position:fixed;inset:0;z-index:2147483647;display:flex;align-items:center;justify-content:center;"
				+ "background:rgba(15,23,42,.6);font-family:Segoe UI,Arial,sans-serif;');"
				+ "w.id='ckyc-missing-ui-prompt';w.setAttribute('role','alertdialog');"
				+ "var box=el('div','width:min(680px,95vw);max-height:94vh;overflow:auto;background:#fff;border-radius:14px;"
				+ "box-shadow:0 20px 50px rgba(0,0,0,.3);border:1px solid #e2e8f0;');"
				+ "box.appendChild(el('div','background:'+cfg.color+';color:#fff;padding:14px 18px;font-size:16px;font-weight:700;','\\u26A0 '+cfg.header));"
				+ "var body=el('div','padding:16px 18px;');box.appendChild(body);"
				+ "body.appendChild(el('div','font-size:15px;font-weight:700;color:#0f172a;margin-bottom:10px;',cfg.title));"
				+ "var tb=el('table','width:100%;border-collapse:collapse;font-size:13px;margin-bottom:10px;');"
				+ "cfg.rows.forEach(function(p){var tr=document.createElement('tr');"
				+ "tr.appendChild(el('td','padding:5px 8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:600;white-space:nowrap;vertical-align:top;color:#334155;',p[0]));"
				+ "tr.appendChild(el('td','padding:5px 8px;border:1px solid #e2e8f0;word-break:break-word;color:#0f172a;'"
				+ "+(p[2]?'font-family:Consolas,monospace;font-size:12px;':''),p[1]));tb.appendChild(tr);});"
				+ "body.appendChild(tb);"
				+ "if(cfg.detail){body.appendChild(el('div','font-size:12px;color:#475569;line-height:1.45;max-height:120px;overflow:auto;"
				+ "white-space:pre-wrap;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;margin-bottom:10px;',cfg.detail));}"
				+ "var lg=el('div','font-size:12px;color:#334155;line-height:1.55;margin-bottom:12px;');"
				+ "[[cfg.continueLabel,cfg.continueHelp],"
				+ "['Skip this case','this scenario is marked skipped; the next scenario of the same feature runs.'],"
				+ "['Skip this feature','the rest of this feature file is skipped; the next feature / module you ticked still runs.'],"
				+ "['Stop everything','the WHOLE automation ends now - every remaining scenario, feature and module ticked in the "
				+ "Control Panel is cancelled; reports for what already ran are written and the browser closes.']]"
				+ ".forEach(function(x){var d=el('div');d.appendChild(el('b',null,x[0]+' - '));d.appendChild(document.createTextNode(x[1]));lg.appendChild(d);});"
				+ "body.appendChild(lg);"
				+ "var cd=el('div','font-size:12px;color:#b45309;margin-bottom:10px;','');body.appendChild(cd);"
				+ "var left=cfg.secs;function tick(){cd.textContent='No answer in '+left+'s = Skip this case and continue.';}tick();"
				+ "var iv=setInterval(function(){left--;if(left<0){clearInterval(iv);}else{tick();}},1000);"
				+ "var bar=el('div','display:flex;gap:8px;justify-content:flex-end;flex-wrap:wrap;');"
				+ "function btn(label,css,val){var b=el('button',css+'padding:9px 14px;border-radius:8px;font-weight:600;cursor:pointer;',label);"
				+ "b.type='button';b.onclick=function(){clearInterval(iv);window.__ckycBlockerChoice=val;};bar.appendChild(b);}"
				+ "btn(cfg.continueLabel,'border:none;background:#1d4ed8;color:#fff;','continue');"
				+ "btn('Skip this case','border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;','skip_scenario');"
				+ "btn('Skip this feature','border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;','skip_feature');"
				+ "btn('Stop everything','border:1px solid #b91c1c;background:#fef2f2;color:#b91c1c;','stop');"
				+ "body.appendChild(bar);w.appendChild(box);document.documentElement.appendChild(w);"
				+ "})(" + cfg + ");";

		try {
			((JavascriptExecutor) driver).executeScript(script);
			logger.info("Decision popup shown ({}) - waiting up to {}s", header, waitSec);
		} catch (Exception e) {
			logger.warn("Could not inject decision popup: {}", e.getMessage());
			return "skip_scenario";
		}

		long deadline = System.currentTimeMillis() + (waitSec * 1000L);
		while (System.currentTimeMillis() < deadline) {
			try {
				Object choice = ((JavascriptExecutor) driver).executeScript("return window.__ckycBlockerChoice;");
				if (choice != null) {
					return String.valueOf(choice);
				}
			} catch (Exception ignored) {
				// page may be navigating
			}
			sleepQuietly(300);
		}
		return null;
	}

	/** Apply SoftAssert skip/abort flags from a missing-UI choice; throws on STOP. */
	public static void applyMissingUiChoice(BlockerChoice choice, String featureName, String message) {
		if (choice == null || choice == BlockerChoice.NONE || choice == BlockerChoice.RETRY
				|| choice == BlockerChoice.CONTINUE_WAIT) {
			return;
		}
		String reason = message != null ? message : "missing expected UI";
		if (choice == BlockerChoice.SKIP_SCENARIO) {
			SoftAssertManager.markSkipScenario(reason);
			logger.warn("User skipped scenario: {}", reason);
			return;
		}
		if (choice == BlockerChoice.SKIP_FEATURE) {
			SoftAssertManager.markSkipFeature(featureName != null ? featureName : "unknown", reason);
			logger.warn("User skipped feature {}: {}", featureName, reason);
			return;
		}
		if (choice == BlockerChoice.STOP) {
			SoftAssertManager.markAbortRun(reason);
			throw new AbortRunException("Stopped by user (missing UI): " + reason);
		}
	}

	/** True when failure looks like a missing expected page / button / field. */
	public static boolean looksLikeMissingExpectedUi(Throwable t) {
		if (t == null) {
			return false;
		}
		Throwable cur = t;
		while (cur != null) {
			if (cur instanceof NoSuchElementException || cur instanceof TimeoutException) {
				return true;
			}
			String msg = cur.getMessage();
			if (msg != null) {
				String lower = msg.toLowerCase(Locale.ROOT);
				if (lower.contains("no such element") || lower.contains("unable to locate")
						|| lower.contains("waiting for visibility") || lower.contains("waiting for element")
						|| lower.contains("not found")) {
					return true;
				}
			}
			cur = cur.getCause();
		}
		return false;
	}

	/**
	 * Detect server-generated error documents or navigation outside the configured CKYC app.
	 * This is a blocker regardless of whether the current validation step is optional.
	 */
	public static boolean isUnexpectedApplicationPage(WebDriver driver) {
		if (driver == null) {
			return false;
		}
		try {
			String url = safeUrl(driver);
			String configured = ConfigReader.getUrl();
			String title = driver.getTitle() == null ? "" : driver.getTitle().toLowerCase(Locale.ROOT);
			String body = "";
			try {
				body = driver.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT);
			} catch (Exception ignored) {
				// title/url checks still apply
			}
			boolean serverError = title.contains("internal server error")
					|| title.contains("server error")
					|| title.startsWith("500")
					|| body.contains("500 - internal server error")
					|| body.contains("there is a problem with the resource you are looking for")
					|| body.contains("it cannot be displayed");
			boolean outsideApp = url == null || url.isBlank() || "about:blank".equalsIgnoreCase(url)
					|| (configured != null && !configured.isBlank() && !url.startsWith(configured));
			return serverError || outsideApp;
		} catch (Exception e) {
			return true;
		}
	}

	public static String describeUnexpectedPage(WebDriver driver) {
		if (driver == null) {
			return "Browser unavailable";
		}
		try {
			return "URL=" + safeUrl(driver) + " | title=" + driver.getTitle();
		} catch (Exception e) {
			return "Unable to read current page: " + e.getMessage();
		}
	}

	/**
	 * After successful login (dashboard only): close Chrome "Check your saved passwords" /
	 * Google Password Manager bubbles if they appear. Native Chrome UI is closed via Escape + Robot.
	 */
	public static void dismissBrowserPasswordPromptsIfPresent(WebDriver driver) {
		if (driver == null) {
			return;
		}
		String url = safeUrl(driver);
		if (url.toLowerCase().contains("/login")) {
			logger.debug("Skipping password-prompt dismiss on login page");
			return;
		}
		if (isActiveSessionDialogVisible(driver)) {
			logger.debug("Skipping password-prompt dismiss — Active session dialog is open");
			return;
		}

		logger.info("Post-login: dismissing Chrome / Google Password Manager UI if shown");
		boolean closedDom = clickPasswordUiCloseIfPresent(driver);

		// Native Chrome bubble is outside page DOM — Escape / Robot Close
		focusWindow(driver);
		sendEscape(driver);
		sleepQuietly(250);
		sendEscape(driver);
		sleepQuietly(250);
		sendNativeEscape();

		// Second pass for late-appearing breach dialog
		sleepQuietly(600);
		closedDom = clickPasswordUiCloseIfPresent(driver) || closedDom;
		sendEscape(driver);
		sendNativeEscape();

		if (closedDom) {
			logger.info("Dismissed page-level password / breach Close button");
		} else {
			logger.info("Sent Escape for Chrome password / breach bubble (if it was showing)");
		}
	}

	private static boolean clickPasswordUiCloseIfPresent(WebDriver driver) {
		try {
			boolean sawText = false;
			for (WebElement el : driver.findElements(PASSWORD_UI_TEXT)) {
				try {
					if (el.isDisplayed()) {
						sawText = true;
						break;
					}
				} catch (Exception ignored) {
					// continue
				}
			}
			for (WebElement btn : driver.findElements(PASSWORD_UI_CLOSE)) {
				try {
					if (btn.isDisplayed() && btn.isEnabled()) {
						clickSafely(driver, btn);
						logger.info("Clicked Close/Not now on password UI button: {}", safeText(btn));
						return true;
					}
				} catch (Exception ignored) {
					// try next
				}
			}
			if (sawText) {
				logger.debug("Password UI text detected but no Close button in DOM (likely native Chrome UI)");
			}
		} catch (Exception e) {
			logger.debug("Password UI Close scan skipped: {}", e.getMessage());
		}
		return false;
	}

	private static void focusWindow(WebDriver driver) {
		try {
			((JavascriptExecutor) driver).executeScript("window.focus();");
			driver.switchTo().window(driver.getWindowHandle());
		} catch (Exception ignored) {
			// ignore
		}
	}

	private static void sendEscape(WebDriver driver) {
		try {
			new Actions(driver).sendKeys(Keys.ESCAPE).perform();
		} catch (Exception e) {
			logger.debug("WebDriver Escape skipped: {}", e.getMessage());
		}
	}

	/** For native Chrome UI (Check your saved passwords) that Selenium cannot see in DOM. */
	private static void sendNativeEscape() {
		try {
			Robot robot = new Robot();
			robot.setAutoDelay(40);
			robot.keyPress(KeyEvent.VK_ESCAPE);
			robot.keyRelease(KeyEvent.VK_ESCAPE);
		} catch (Exception e) {
			logger.debug("Native Escape (Robot) skipped: {}", e.getMessage());
		}
	}

	private static void clickSafely(WebDriver driver, WebElement element) {
		try {
			element.click();
		} catch (Exception clickEx) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		}
	}

	private static String safeText(WebElement el) {
		try {
			String t = el.getText();
			return t == null ? "" : t.trim();
		} catch (Exception e) {
			return "";
		}
	}

	private static String safeUrl(WebDriver driver) {
		try {
			String u = driver.getCurrentUrl();
			return u == null ? "" : u;
		} catch (Exception e) {
			return "";
		}
	}

	private static String escapeJs(String raw) {
		return raw.replace("\\", "\\\\")
				.replace("`", "\\`")
				.replace("${", "\\${")
				.replace("</", "<\\/");
	}

	private static void sleepQuietly(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
		throw (E) t;
	}
}
