/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;
import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;

/**
 * The pop-ups of the Bulk Data Import flow, drawn inside the application page: the upload pop-up (drag and drop or browse),
 * the retry pop-up (Retry / choose another file / "I did it by hand" / Cancel) and the waiting card with the live timer.
 */
final class BiPopups {

	/** What the tester answered in the upload pop-up. */
	record FileAnswer(String action, String name, Path saved) {
	}

	private BiPopups() {
	}

	private static WebDriver driver() {
		return BaseClass.getDriver();
	}

	private static final String HELPERS = "function el(t,c,x){var e=document.createElement(t);if(c){e.style.cssText=c;}if(x!=null){e.textContent=x;}return e;}"
			+ "function btn(label,css,fn){var b=el('button','border:0;border-radius:8px;padding:8px 15px;font-weight:600;cursor:pointer;'+css,label);b.type='button';b.onclick=fn;return b;}";

	/** Upload pop-up: returns upload (file saved under Current Data/BulkImport) or cancel. Waits for the tester. */
	static FileAnswer askFile(String title, String hint) {
		JSONObject cfg = new JSONObject().put("title", title).put("hint", hint);
		String js = "var cfg=" + cfg + ";window.__ckycBi=null;var old=document.getElementById('ckyc-bi-popup');if(old){old.remove();}" + HELPERS
				+ "var w=el('div','position:fixed;inset:0;z-index:2147483647;background:rgba(15,23,42,.6);display:flex;align-items:center;justify-content:center;font-family:Segoe UI,Arial,sans-serif;');w.id='ckyc-bi-popup';"
				+ "var box=el('div','width:min(620px,94vw);background:#fff;border-radius:14px;box-shadow:0 20px 50px rgba(0,0,0,.35);overflow:hidden;');"
				+ "box.appendChild(el('div','background:#1d4ed8;color:#fff;padding:14px 18px;font-size:16px;font-weight:700;',cfg.title));"
				+ "var body=el('div','padding:16px 18px;');body.appendChild(el('div','font-size:13px;color:#475569;margin-bottom:10px;',cfg.hint));"
				+ "var drop=el('div','border:2px dashed #94a3b8;border-radius:12px;padding:34px 12px;text-align:center;color:#334155;cursor:pointer;background:#f8fafc;','Drag and drop the CSV file here, or click to browse');"
				+ "var inp=document.createElement('input');inp.type='file';inp.accept='.csv,text/csv';inp.style.display='none';"
				+ "var info=el('div','margin-top:10px;font-size:13px;min-height:36px;color:#0f172a;','No file chosen yet.');var chosen=null;"
				+ "function take(f){if(!f){return;}chosen=f;var r=new FileReader();r.onload=function(){var t=String(r.result||'');var h=t.split(/\\r?\\n/)[0]||'';"
				+ "var d=h.indexOf('|')>=0?'|':(h.indexOf(',')>=0?',':'\\t');var rows=t.split(/\\r?\\n/).filter(function(x){return x.trim();}).length-1;"
				+ "info.textContent=f.name+'  -  '+f.size+' bytes  -  '+h.split(d).length+' columns, '+rows+' rows (delimiter '+(d==='\\t'?'tab':d)+')';up.disabled=false;up.style.opacity='1';};r.readAsText(f.slice(0,2000000));}"
				+ "drop.onclick=function(){inp.click();};inp.onchange=function(){take(inp.files[0]);};"
				+ "drop.ondragover=function(e){e.preventDefault();drop.style.background='#e0f2fe';};drop.ondragleave=function(){drop.style.background='#f8fafc';};"
				+ "drop.ondrop=function(e){e.preventDefault();drop.style.background='#f8fafc';if(e.dataTransfer.files&&e.dataTransfer.files.length){take(e.dataTransfer.files[0]);}};"
				+ "var row=el('div','display:flex;gap:10px;justify-content:flex-end;margin-top:16px;');"
				+ "var up=btn('Upload this file','background:#16a34a;color:#fff;opacity:.5;',function(){if(!chosen){return;}var fr=new FileReader();fr.onload=function(){window.__ckycBi={action:'upload',name:chosen.name,b64:String(fr.result).split(',')[1]};};fr.readAsDataURL(chosen);up.disabled=true;up.textContent='Reading...';});up.disabled=true;"
				+ "var cancel=btn('Cancel','background:#fef2f2;color:#b91c1c;border:1px solid #b91c1c;',function(){window.__ckycBi={action:'cancel'};});"
				+ "row.appendChild(up);row.appendChild(cancel);body.appendChild(drop);body.appendChild(inp);body.appendChild(info);body.appendChild(row);box.appendChild(body);w.appendChild(box);document.documentElement.appendChild(w);";
		JSONObject a = waitAnswer(js, 30);
		remove();
		if (a == null || !"upload".equals(a.optString("action"))) {
			return new FileAnswer("cancel", "", null);
		}
		try {
			String day = ReportNamingHelper.runDate();
			Path dir = Paths.get(ReportNamingHelper.reportOutputRoot()).getParent().resolve("Current Data").resolve("BulkImport").resolve(day);
			Files.createDirectories(dir);
			String name = a.getString("name").replaceAll("[\\\\/:*?\"<>|]", "_");
			Path f = dir.resolve(name);
			Files.write(f, Base64.getDecoder().decode(a.getString("b64")));
			return new FileAnswer("upload", name, f);
		} catch (Exception e) {
			throw new IllegalStateException("The chosen file could not be saved: " + e.getMessage(), e);
		}
	}

	/** After a failed upload: "retry" | "choose" | "manual" | "cancel". */
	static String askRetry(String reason, String fileName) {
		JSONObject cfg = new JSONObject().put("reason", reason).put("file", fileName);
		String js = "var cfg=" + cfg + ";window.__ckycBi=null;var old=document.getElementById('ckyc-bi-popup');if(old){old.remove();}" + HELPERS
				+ "var w=el('div','position:fixed;inset:0;z-index:2147483647;background:rgba(15,23,42,.6);display:flex;align-items:center;justify-content:center;font-family:Segoe UI,Arial,sans-serif;');w.id='ckyc-bi-popup';"
				+ "var box=el('div','width:min(600px,94vw);background:#fff;border-radius:14px;box-shadow:0 20px 50px rgba(0,0,0,.35);overflow:hidden;');"
				+ "box.appendChild(el('div','background:#b91c1c;color:#fff;padding:14px 18px;font-size:16px;font-weight:700;','Upload did not succeed'));"
				+ "var body=el('div','padding:16px 18px;');body.appendChild(el('div','font-size:14px;color:#0f172a;margin-bottom:8px;','File: '+cfg.file));"
				+ "body.appendChild(el('div','font-size:13px;color:#475569;white-space:pre-wrap;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;max-height:150px;overflow:auto;',cfg.reason));"
				+ "var help=el('div','font-size:12px;color:#334155;line-height:1.6;margin:12px 0;');"
				+ "[['Retry','upload the same file again (the page is reloaded first).'],['Choose another file','open the upload pop-up again, for example after changing the series number.'],"
				+ "['I uploaded it by hand','you upload and submit in the application yourself, then click this - the automation reads the newest upload.'],['Cancel','stop this scenario.']]"
				+ ".forEach(function(x){var d=el('div');d.appendChild(el('b',null,x[0]+' - '));d.appendChild(document.createTextNode(x[1]));help.appendChild(d);});body.appendChild(help);"
				+ "var row=el('div','display:flex;gap:8px;justify-content:flex-end;flex-wrap:wrap;');"
				+ "row.appendChild(btn('Retry','background:#1d4ed8;color:#fff;',function(){window.__ckycBi={action:'retry'};}));"
				+ "row.appendChild(btn('Choose another file','background:#e2e8f0;color:#0f172a;',function(){window.__ckycBi={action:'choose'};}));"
				+ "row.appendChild(btn('I uploaded it by hand','background:#e2e8f0;color:#0f172a;',function(){window.__ckycBi={action:'manual'};}));"
				+ "row.appendChild(btn('Cancel','background:#fef2f2;color:#b91c1c;border:1px solid #b91c1c;',function(){window.__ckycBi={action:'cancel'};}));"
				+ "body.appendChild(row);box.appendChild(body);w.appendChild(box);document.documentElement.appendChild(w);";
		JSONObject a = waitAnswer(js, 30);
		remove();
		return a == null ? "cancel" : a.optString("action", "cancel");
	}

	private static JSONObject waitAnswer(String script, int minutes) {
		WebDriver d = driver();
		((JavascriptExecutor) d).executeScript(script);
		long deadline = System.currentTimeMillis() + minutes * 60_000L;
		while (System.currentTimeMillis() < deadline) {
			try {
				Object o = ((JavascriptExecutor) d).executeScript("return window.__ckycBi ? JSON.stringify(window.__ckycBi) : null;");
				if (o != null) {
					return new JSONObject(String.valueOf(o));
				}
			} catch (org.openqa.selenium.WebDriverException e) {
				// page busy
			}
			sleep(400);
		}
		return null;
	}

	static void remove() {
		try {
			((JavascriptExecutor) driver()).executeScript(
					"var o=document.getElementById('ckyc-bi-popup');if(o){o.remove();}window.__ckycBi=null;");
		} catch (RuntimeException ignored) {
			// page changed
		}
	}

	// ------------------------------------------------------------------ waiting card (timer)

	/**
	 * Shows or refreshes the waiting card. The timer runs in the page by itself, so it keeps counting while the
	 * automation is reading the dashboard. state: startMs, limitMs, lines (text rows), nextRefreshMs.
	 */
	static void waitCard(JSONObject state) {
		String js = "var s=" + state + ";var id='ckyc-bi-wait';var c=document.getElementById(id);" + HELPERS
				+ "if(!c){c=el('div','position:fixed;left:14px;bottom:14px;z-index:2147483646;width:330px;background:#0b2a6f;color:#fff;border-radius:12px;padding:12px 14px;font:13px Segoe UI,Arial,sans-serif;box-shadow:0 10px 30px rgba(0,0,0,.4);');c.id=id;"
				+ "c.appendChild(el('div','font-weight:700;font-size:14px;margin-bottom:6px;','Waiting for iFlow - Data pushed from FI'));"
				+ "var t=el('div','font-size:26px;font-weight:700;letter-spacing:1px;margin:2px 0 6px;','00:00');t.id=id+'-t';c.appendChild(t);"
				+ "var l=el('div','line-height:1.5;white-space:pre-wrap;','');l.id=id+'-l';c.appendChild(l);"
				+ "var b=btn('Stop waiting','background:#fecaca;color:#7f1d1d;margin-top:8px;padding:5px 10px;',function(){window.__ckycBiStop=true;b.textContent='Stopping...';});c.appendChild(b);document.documentElement.appendChild(c);"
				+ "window.__ckycBiTimer=setInterval(function(){var st=window.__ckycBiState;if(!st){return;}var e=Math.max(0,Date.now()-st.startMs);var f=function(ms){var s=Math.floor(ms/1000);return ('0'+Math.floor(s/60)).slice(-2)+':'+('0'+(s%60)).slice(-2);};"
				+ "document.getElementById(id+'-t').textContent=f(e)+' / '+f(st.limitMs);},1000);}"
				+ "window.__ckycBiState=s;document.getElementById(id+'-l').textContent=(s.lines||[]).join('\\n');";
		try {
			((JavascriptExecutor) driver()).executeScript(js);
		} catch (RuntimeException ignored) {
			// page reloading
		}
	}

	static boolean stopRequested() {
		try {
			return Boolean.TRUE.equals(((JavascriptExecutor) driver()).executeScript("return !!window.__ckycBiStop;"));
		} catch (RuntimeException e) {
			return false;
		}
	}

	static void removeWaitCard() {
		try {
			((JavascriptExecutor) driver()).executeScript(
					"var c=document.getElementById('ckyc-bi-wait');if(c){c.remove();}if(window.__ckycBiTimer){clearInterval(window.__ckycBiTimer);}window.__ckycBiStop=false;");
		} catch (RuntimeException ignored) {
			// page changed
		}
	}

	static int waitMinutes() {
		return Math.max(1, Integer.parseInt(ConfigReader.get("bulkimport.waitMinutes", "7")));
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
