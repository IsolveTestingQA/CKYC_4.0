/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.searchdownload;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * The HTML popups of the interactive Search session, drawn on top of the application page:
 * (1) choose the Document Type - the options are the ones the application's own dropdown shows;
 * (2) type the document number - the field forces the format while you type (PAN = 5 letters, 4 digits, 1 letter,
 *     upper case), with buttons Search, Run automatic negatives, Clear and Close, and a "Negative test" switch that
 *     sends exactly what is typed so the application's own error messages can be seen.
 * The popup is removed while the automation works on the page and shown again with the result of the last search.
 */
public final class SearchInteractivePrompt {

	public enum Action { CHOSEN, SEARCH, NEGATIVES, CLEAR, CLOSE, CHANGE_TYPE }

	public record Answer(Action action, String value, boolean negativeMode) {
	}

	private static final Logger logger = LoggerFactory.getLogger(SearchInteractivePrompt.class);
	private static final String ID = "ckyc-search-prompt";

	private SearchInteractivePrompt() {
	}

	/** Popups need a visible browser and an operator; otherwise the session falls back to the configured values. */
	public static boolean enabled() {
		return ConfigReader.interactiveBlockerPrompt() && !Boolean.parseBoolean(ConfigReader.get("headless", "false"));
	}

	/** Step 1: pick one of the application's own Document Type options. */
	public static Answer chooseType(List<String> options, List<String> history) {
		StringBuilder opts = new StringBuilder("[");
		for (int i = 0; i < options.size(); i++) {
			opts.append(i == 0 ? "" : ",").append(quote(options.get(i)));
		}
		opts.append(']');
		return show("{\"stage\":\"choose\",\"options\":" + opts + ",\"history\":" + array(history) + "}");
	}

	/**
	 * Step 2: enter the number.
	 * @param rule PAN, DIGITS or ALNUM (how the field controls typing)
	 */
	public static Answer enterNumber(String docType, String rule, int maxLength, String hint, String lastValue,
			boolean negativeMode, List<String> history) {
		return show("{\"stage\":\"enter\",\"docType\":" + quote(docType) + ",\"rule\":" + quote(rule) + ",\"max\":" + maxLength
				+ ",\"hint\":" + quote(hint) + ",\"last\":" + quote(lastValue == null ? "" : lastValue)
				+ ",\"negative\":" + negativeMode + ",\"history\":" + array(history) + "}");
	}

	@SuppressWarnings("unchecked")
	private static Answer show(String configJson) {
		WebDriver driver = BaseClass.getDriver();
		int timeoutMinutes = Math.max(1, Integer.parseInt(ConfigReader.get("search.prompt.timeoutMinutes", "30")));
		try {
			((JavascriptExecutor) driver).executeScript("window.__ckycSearch = null; var old = document.getElementById('" + ID
					+ "'); if (old) { old.remove(); }" + BUILD_JS, configJson);
		} catch (RuntimeException e) {
			logger.warn("Could not show the Search popup: {}", e.getMessage());
			throw e;
		}
		long deadline = System.currentTimeMillis() + timeoutMinutes * 60_000L;
		while (System.currentTimeMillis() < deadline) {
			Object o = ((JavascriptExecutor) driver).executeScript("return window.__ckycSearch;");
			if (o instanceof Map) {
				Map<String, Object> m = (Map<String, Object>) o;
				String action = String.valueOf(m.get("action"));
				String value = m.get("value") == null ? "" : String.valueOf(m.get("value"));
				boolean neg = Boolean.TRUE.equals(m.get("negative"));
				remove(driver);
				return new Answer(switch (action) {
					case "chosen" -> Action.CHOSEN;
					case "search" -> Action.SEARCH;
					case "negatives" -> Action.NEGATIVES;
					case "clear" -> Action.CLEAR;
					case "change" -> Action.CHANGE_TYPE;
					default -> Action.CLOSE;
				}, value, neg);
			}
			sleep(300);
		}
		remove(driver);
		logger.warn("Search popup not answered for {} minutes - closing the session", timeoutMinutes);
		return new Answer(Action.CLOSE, "", false);
	}

	public static void remove(WebDriver driver) {
		try {
			((JavascriptExecutor) driver).executeScript("var el = document.getElementById('" + ID + "'); if (el) { el.remove(); }"
					+ "window.__ckycSearch = null;");
		} catch (RuntimeException ignored) {
			// page already gone
		}
	}

	private static String array(List<String> items) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < items.size(); i++) {
			sb.append(i == 0 ? "" : ",").append(quote(items.get(i)));
		}
		return sb.append(']').toString();
	}

	private static String quote(String s) {
		return Com.Ckyc_4_0.utils.reports.JsonText.quote(s == null ? "" : s);
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** The popup: plain DOM, no libraries; arguments[0] is the JSON configuration. */
	private static final String BUILD_JS = """
			var cfg = JSON.parse(arguments[0]);
			var wrap = document.createElement('div'); wrap.id = 'ckyc-search-prompt';
			wrap.style.cssText = 'position:fixed;inset:0;z-index:2147483647;display:flex;align-items:center;justify-content:center;'
			  + 'background:rgba(15,23,42,.55);font-family:Segoe UI,Arial,sans-serif;';
			var box = document.createElement('div');
			box.style.cssText = 'width:min(640px,94vw);max-height:92vh;overflow:auto;background:#fff;border-radius:14px;'
			  + 'box-shadow:0 20px 50px rgba(0,0,0,.3);border:1px solid #e2e8f0;';
			var head = document.createElement('div');
			head.style.cssText = 'background:#1d4ed8;color:#fff;padding:14px 18px;font-size:16px;font-weight:700;';
			head.textContent = 'Search automation - ' + (cfg.stage === 'choose' ? 'choose the Document Type' : 'enter the ' + cfg.docType);
			var body = document.createElement('div'); body.style.cssText = 'padding:16px 18px;';
			function done(action, value, negative) { window.__ckycSearch = {action: action, value: value || '', negative: !!negative}; }
			function btn(label, css, fn) { var b = document.createElement('button'); b.type = 'button'; b.textContent = label;
			  b.style.cssText = 'padding:9px 14px;border-radius:8px;font-weight:600;cursor:pointer;font-size:14px;' + css; b.onclick = fn; return b; }
			// results so far
			if (cfg.history && cfg.history.length) {
			  var h = document.createElement('div');
			  h.style.cssText = 'font-size:12px;color:#334155;background:#f1f5f9;border-radius:8px;padding:8px 10px;margin-bottom:12px;max-height:130px;overflow:auto;';
			  h.innerHTML = '<b>Results so far</b><br>' + cfg.history.map(function(x){ var bad = x.indexOf('FAIL') >= 0;
			    return '<span style="color:' + (bad ? '#b91c1c' : '#166534') + '">' + x.replace(/&/g,'&amp;').replace(/</g,'&lt;') + '</span>'; }).join('<br>');
			  body.appendChild(h);
			}
			if (cfg.stage === 'choose') {
			  var p = document.createElement('div'); p.style.cssText = 'font-size:14px;color:#0f172a;margin-bottom:10px;';
			  p.textContent = 'These are the options of the Document Type dropdown on the Search page. Choose one:';
			  body.appendChild(p);
			  var list = document.createElement('div'); list.style.cssText = 'display:grid;gap:8px;';
			  cfg.options.forEach(function(o) {
			    list.appendChild(btn(o, 'border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;text-align:left;', function(){ done('chosen', o, false); }));
			  });
			  body.appendChild(list);
			  var row = document.createElement('div'); row.style.cssText = 'display:flex;justify-content:flex-end;margin-top:14px;';
			  row.appendChild(btn('Close - stop the Search module', 'border:1px solid #b91c1c;background:#fef2f2;color:#b91c1c;', function(){ done('close'); }));
			  body.appendChild(row);
			} else {
			  var info = document.createElement('div'); info.style.cssText = 'font-size:13px;color:#475569;margin-bottom:8px;';
			  info.innerHTML = 'Format: <b>' + cfg.hint + '</b> &nbsp;|&nbsp; the field corrects what you type (upper case, only the allowed character at each position).';
			  body.appendChild(info);
			  var inp = document.createElement('input'); inp.type = 'text'; inp.autocomplete = 'off'; inp.spellcheck = false;
			  inp.value = cfg.last || '';
			  inp.style.cssText = 'width:100%;box-sizing:border-box;font-size:22px;letter-spacing:3px;padding:10px 12px;border:2px solid #94a3b8;'
			    + 'border-radius:10px;font-family:Consolas,monospace;text-transform:uppercase;';
			  body.appendChild(inp);
			  var state = document.createElement('div'); state.style.cssText = 'font-size:13px;margin:8px 0 2px;font-weight:600;';
			  body.appendChild(state);
			  var neg = document.createElement('label'); neg.style.cssText = 'display:flex;gap:8px;align-items:center;font-size:13px;color:#7c2d12;margin-top:8px;';
			  var cb = document.createElement('input'); cb.type = 'checkbox'; cb.checked = !!cfg.negative;
			  neg.appendChild(cb); neg.appendChild(document.createTextNode('Negative test - send exactly what I type (no format control), to see the application error message'));
			  body.appendChild(neg);
			  function mask(raw) {
			    var out = '';
			    for (var i = 0; i < raw.length && out.length < cfg.max; i++) {
			      var c = raw.charAt(i).toUpperCase(); var p = out.length;
			      var letter = c >= 'A' && c <= 'Z', digit = c >= '0' && c <= '9';
			      if (cfg.rule === 'PAN') { var needLetter = p < 5 || p === 9; if (needLetter ? letter : digit) { out += c; } }
			      else if (cfg.rule === 'DIGITS') { if (digit) { out += c; } }
			      else { if (letter || digit) { out += c; } }
			    }
			    return out;
			  }
			  function validComplete(v) {
			    if (cfg.rule === 'PAN') { return v.length === 10; }
			    if (cfg.rule === 'DIGITS') { return v.length === cfg.max; }
			    return v.length >= 1;
			  }
			  var searchBtn, autoBtn;
			  function refresh() {
			    var v = inp.value;
			    if (!cb.checked) { v = mask(v); if (v !== inp.value) { inp.value = v; } }
			    var ok = validComplete(v);
			    if (cb.checked) {
			      state.style.color = '#b45309'; state.textContent = v.length + ' character(s) - NEGATIVE test: sent as typed';
			      searchBtn.disabled = v.length === 0 && false;
			    } else {
			      state.style.color = ok ? '#166534' : '#b45309';
			      state.textContent = v.length + ' / ' + cfg.max + (ok ? '  - complete, ready to search' : '  - keep typing');
			      searchBtn.disabled = !ok;
			    }
			    searchBtn.style.opacity = searchBtn.disabled ? '.5' : '1';
			  }
			  var row2 = document.createElement('div'); row2.style.cssText = 'display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end;margin-top:16px;';
			  var closeBtn = btn('Close - stop the Search module', 'border:1px solid #b91c1c;background:#fef2f2;color:#b91c1c;', function(){ done('close'); });
			  var clearBtn = btn('Clear (reset everything)', 'border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;', function(){ done('clear'); });
			  var changeBtn = btn('Change type', 'border:1px solid #cbd5e1;background:#f8fafc;color:#0f172a;', function(){ done('change'); });
			  autoBtn = btn('Run automatic negatives', 'border:1px solid #b45309;background:#fffbeb;color:#92400e;', function(){ done('negatives', '', true); });
			  searchBtn = btn('Search', 'border:none;background:#1d4ed8;color:#fff;', function(){ if (!searchBtn.disabled) { done('search', inp.value, cb.checked); } });
			  [closeBtn, clearBtn, changeBtn, autoBtn, searchBtn].forEach(function(b){ row2.appendChild(b); });
			  body.appendChild(row2);
			  inp.addEventListener('input', refresh); cb.addEventListener('change', refresh);
			  inp.addEventListener('keydown', function(e){ if (e.key === 'Enter' && !searchBtn.disabled) { done('search', inp.value, cb.checked); } });
			  setTimeout(function(){ refresh(); inp.focus(); }, 0);
			}
			box.appendChild(head); box.appendChild(body); wrap.appendChild(box); document.documentElement.appendChild(wrap);
			""";
}
