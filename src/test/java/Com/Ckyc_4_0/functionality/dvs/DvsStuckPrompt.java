/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.utils.dvs.DvsConfig;
import Com.Ckyc_4_0.utils.dvs.DvsStepLog;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Popup shown on the page when a DVS row / group is stuck: Retry, Skip and continue, or Stop. It expires after
 * dvs.stuckPromptSeconds (default and maximum 60) and then SKIPS the row and continues - it never stops the run
 * on its own. In headless / unattended runs (headless=true or interactiveBlockerPrompt=false) no popup is shown
 * and the answer is SKIP at once.
 */
public final class DvsStuckPrompt {

	public enum Choice { RETRY, SKIP, STOP }

	private static final Logger logger = LoggerFactory.getLogger(DvsStuckPrompt.class);
	private static final int MAX_SECONDS = 60;

	private DvsStuckPrompt() {
	}

	public static int seconds() {
		return Math.min(MAX_SECONDS, Math.max(10, DvsConfig.getInt("dvs.stuckPromptSeconds", MAX_SECONDS)));
	}

	public static boolean enabled() {
		return DvsConfig.getBool("dvs.stuckPrompt", true) && ConfigReader.interactiveBlockerPrompt()
				&& !Boolean.parseBoolean(ConfigReader.get("headless", "false"));
	}

	public static Choice ask(String title, String detail) {
		long t0 = System.currentTimeMillis();
		Choice result = askInternal(title, detail);
		DvsStepLog.step("stuck-popup").actual(title + " -> " + result + (result == Choice.SKIP && !enabled() ? " (popup disabled)" : ""))
				.expected("Retry / Skip / Stop, expires in " + seconds() + "s with Skip").result(DvsStepLog.INFO).log();
		logger.warn("Stuck popup '{}' answered {} after {} ms", title, result, System.currentTimeMillis() - t0);
		return result;
	}

	private static Choice askInternal(String title, String detail) {
		WebDriver driver = BaseClass.getDriver();
		if (driver == null || !enabled()) {
			return Choice.SKIP;
		}
		int wait = seconds();
		String script = "window.__dvsStuckChoice = null;"
				+ "var old = document.getElementById('dvs-stuck-prompt'); if (old) { old.remove(); }"
				+ "var wrap = document.createElement('div'); wrap.id = 'dvs-stuck-prompt';"
				+ "wrap.setAttribute('role','alertdialog');"
				+ "wrap.style.cssText = 'position:fixed;inset:0;z-index:2147483647;display:flex;align-items:center;"
				+ "justify-content:center;background:rgba(15,23,42,0.55);font-family:Segoe UI,Arial,sans-serif;';"
				+ "var box = document.createElement('div');"
				+ "box.style.cssText = 'width:min(520px,92vw);background:#fff;border-radius:14px;overflow:hidden;"
				+ "box-shadow:0 20px 50px rgba(0,0,0,.28);border:1px solid #e2e8f0;';"
				+ "var head = document.createElement('div');"
				+ "head.style.cssText = 'background:#b45309;color:#fff;padding:14px 18px;font-size:16px;font-weight:700;';"
				+ "head.textContent = 'DVS automation - scenario stuck';"
				+ "var body = document.createElement('div'); body.style.cssText = 'padding:18px;';"
				+ "var t = document.createElement('div');"
				+ "t.style.cssText = 'font-size:15px;font-weight:600;color:#0f172a;margin-bottom:8px;'; t.textContent = arguments[0];"
				+ "var d = document.createElement('div');"
				+ "d.style.cssText = 'font-size:13px;color:#475569;line-height:1.45;max-height:160px;overflow:auto;white-space:pre-wrap;';"
				+ "d.textContent = arguments[1];"
				+ "var cd = document.createElement('div'); cd.style.cssText = 'font-size:12px;color:#b45309;margin-top:12px;font-weight:600;';"
				+ "var left = arguments[2]; cd.textContent = 'No answer in ' + left + 's = Skip and continue with the next scenario';"
				+ "var row = document.createElement('div');"
				+ "row.style.cssText = 'display:flex;gap:10px;justify-content:flex-end;margin-top:16px;';"
				+ "function mk(label, val, css) { var b = document.createElement('button'); b.type = 'button'; b.textContent = label;"
				+ "b.style.cssText = 'padding:9px 16px;border-radius:8px;font-weight:600;cursor:pointer;' + css;"
				+ "b.onclick = function(){ window.__dvsStuckChoice = val; }; return b; }"
				+ "row.appendChild(mk('Stop run','stop','border:1px solid #b91c1c;background:#fef2f2;color:#b91c1c;'));"
				+ "row.appendChild(mk('Retry','retry','border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;'));"
				+ "row.appendChild(mk('Skip and continue','skip','border:none;background:#1d4ed8;color:#fff;'));"
				+ "body.appendChild(t); body.appendChild(d); body.appendChild(cd); body.appendChild(row);"
				+ "box.appendChild(head); box.appendChild(body); wrap.appendChild(box); document.documentElement.appendChild(wrap);"
				+ "window.__dvsStuckTimer = setInterval(function(){ left--; if (left >= 0) { cd.textContent = 'No answer in ' + left"
				+ " + 's = Skip and continue with the next scenario'; } }, 1000);";
		try {
			((JavascriptExecutor) driver).executeScript(script, title == null ? "" : title, detail == null ? "" : detail, wait);
		} catch (RuntimeException e) {
			logger.warn("Could not show the stuck popup: {}", e.getMessage());
			return Choice.SKIP;
		}
		long deadline = System.currentTimeMillis() + wait * 1000L;
		Choice choice = Choice.SKIP;
		while (System.currentTimeMillis() < deadline) {
			try {
				Object c = ((JavascriptExecutor) driver).executeScript("return window.__dvsStuckChoice;");
				if ("retry".equals(c)) {
					choice = Choice.RETRY;
					break;
				}
				if ("stop".equals(c)) {
					choice = Choice.STOP;
					break;
				}
				if ("skip".equals(c)) {
					break;
				}
			} catch (RuntimeException ignored) {
				// page may be navigating - keep waiting until the deadline
			}
			DvsFieldActions.sleep(300);
		}
		remove(driver);
		return choice;
	}

	private static void remove(WebDriver driver) {
		try {
			((JavascriptExecutor) driver).executeScript("if (window.__dvsStuckTimer) { clearInterval(window.__dvsStuckTimer); }"
					+ "var el = document.getElementById('dvs-stuck-prompt'); if (el) { el.remove(); } window.__dvsStuckChoice = null;");
		} catch (RuntimeException ignored) {
			// page already gone
		}
	}
}
