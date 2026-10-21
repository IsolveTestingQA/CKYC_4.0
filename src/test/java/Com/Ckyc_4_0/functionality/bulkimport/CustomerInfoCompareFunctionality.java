/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ReportNamingHelper;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.bulkimport.BulkImportPages;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.bulkimport.CkycSpec;
import Com.Ckyc_4_0.utils.bulkimport.FieldMapper;
import Com.Ckyc_4_0.utils.bulkimport.FieldMapper.Decision;
import Com.Ckyc_4_0.utils.bulkimport.FieldMapper.Link;
import Com.Ckyc_4_0.utils.bulkimport.FieldMapper.UiField;

/**
 * Customer Information: for random customer ids that reached Success, search the id, read every tab dynamically (labels,
 * values, the "N fields" count of each section; a masked value is revealed with its view icon and read), and compare it
 * with the uploaded CSV row through the field mapping. Missing mandatory values and values that differ are bugs. With no
 * frozen mapping the run is in learning mode: a draft mapping (and the code book) is suggested from the real data and
 * saved for review; with a frozen mapping the labels are looked up by name, so changed or added fields are reported.
 */
public final class CustomerInfoCompareFunctionality {

	private static final Logger logger = LoggerFactory.getLogger(CustomerInfoCompareFunctionality.class);
	private static final String SC = "Customer Information | ";
	private static final Pattern ID_LINE = Pattern.compile("ID\\s+(\\S+)\\s*[\\u00b7\\u2022.]\\s*(\\d+)");

	private static final String READ_JS = "var out=[];var hs=[].filter.call(document.querySelectorAll('span,p,h6,h5'),function(e){return !e.children.length&&/^\\s*\\S+\\s*\\u2014\\s*\\d+\\s*fields?\\s*$/i.test(e.textContent);});"
			+ "hs.forEach(function(h){var m=h.textContent.match(/^\\s*(\\S+)\\s*\\u2014\\s*(\\d+)/);var sec={name:m[1],declared:parseInt(m[2],10),fields:[]};var root=h.parentElement&&h.parentElement.parentElement;"
			+ "if(root){[].forEach.call(root.querySelectorAll('span'),function(s){if(s===h||s.children.length||s.closest('p')){return;}var b1=s.parentElement&&s.parentElement.parentElement;if(!b1||b1===root){return;}"
			+ "var p=null;for(var c=b1.firstElementChild;c;c=c.nextElementSibling){if(c.tagName==='P'){p=c;break;}}var label=s.textContent.replace(/\\u200b/g,'').trim();if(!label){return;}"
			+ "var val=p?(p.getAttribute('aria-label')||p.innerText||p.textContent||'').replace(/\\u200b/g,'').trim():'';var eye=!!b1.querySelector('[data-testid=\"VisibilityIcon\"]');"
			+ "sec.fields.push({label:label,value:val,masked:/[X*\\u2022]{3,}/.test(val)||eye,eye:eye});});}out.push(sec);});return JSON.stringify(out);";

	private static final String REVEAL_JS = "var label=arguments[0];var s=[].find.call(document.querySelectorAll('span'),function(x){return !x.children.length&&!x.closest('p')&&x.textContent.replace(/\\u200b/g,'').trim()===label;});"
			+ "if(!s){return false;}var b1=s.parentElement&&s.parentElement.parentElement;var eye=b1&&b1.querySelector('[data-testid=\"VisibilityIcon\"]');if(!eye){return false;}"
			+ "var t=eye.closest('button,[role=button]')||eye;t.click();return true;";

	private WebDriver d() {
		return BiSupport.driver();
	}

	public void openCustomerInformation() {
		BiSupport.openModule(SideNavPage.CUSTOMER_INFORMATION, null, "/customer-information", "/settings/customer-information");
		boolean ok = BiSupport.waitFor(BulkImportPages.CI_ID_INPUT, 15);
		StepLog.check(SC + "Open", "Open Customer Information from the navigation bar", "Customer Lookup with the Customer IDs box",
				"url=" + BiSupport.safeUrl() + "; idBoxVisible=" + ok, ok, "module=Customer Information");
		if (!ok) {
			throw new AssertionError("Customer Information did not open (Customer IDs box missing)");
		}
	}

	// ------------------------------------------------------------------ main

	public void compareRandomSuccessIds() {
		if (BulkImportContext.cancelled || BulkImportContext.csv == null) {
			return;
		}
		List<String> ids = new ArrayList<>();
		for (String id : BulkImportContext.expectedIds) {
			String b = BulkImportContext.bucket.get(id);
			if (b != null && b.toLowerCase(Locale.ROOT).contains("success")) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			StepLog.check(SC + "Customers", "Pick Success customer ids", "At least one Success id", "FAIL | none of the uploaded ids reached Success",
					false, "job=" + BulkImportContext.jobNo);
			return;
		}
		Collections.shuffle(ids);
		int n = Math.min(ids.size(), Math.max(1, Integer.parseInt(ConfigReader.get("bulkimport.compareCount", "3"))));
		List<String> pick = ids.subList(0, n);
		StepLog.check(SC + "Customers", "Pick random Success customer ids", "Ids chosen", "picked " + n + " of " + ids.size() + ": " + pick, true,
				"job=" + BulkImportContext.jobNo);
		String code = "LE".equals(BulkImportContext.persona) ? "LE" : "IN";
		Path root = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "CustomerInfo");
		Path frozenMap = root.resolve("FieldMap_" + code + ".csv");
		Path frozenCodes = root.resolve("CodeMap_" + code + ".csv");
		Map<String, Integer> frozenCounts = new LinkedHashMap<>();
		List<Link> frozen = FieldMapper.loadLinks(frozenMap, frozenCounts);
		boolean learning = frozen.isEmpty();
		FieldMapper.CodeBook book = FieldMapper.CodeBook.load(frozenCodes);
		Set<String> ignore = new HashSet<>();
		for (Link l : frozen) {
			if (l.rule().equals("IGNORE")) {
				ignore.addAll(l.csvCols());
			}
		}
		FieldMapper fm = new FieldMapper(CkycSpec.load(BulkImportContext.persona), book, ignore);
		BulkImportContext.mappingNote = learning ? "LEARNING MODE - no frozen mapping (" + frozenMap.getFileName() + "); a draft was suggested from this run"
				: "Frozen mapping " + frozenMap.getFileName() + " (" + frozen.size() + " links)";
		StepLog.check(SC + "Mapping", "Use the field mapping", "Frozen mapping, or a draft in learning mode", BulkImportContext.mappingNote, true,
				"persona=" + BulkImportContext.persona);
		List<Link> links = frozen;
		openCustomerInformation();
		for (String id : pick) {
			try {
				Map<String, String> row = BulkImportContext.csv.rowWhere(BulkImportContext.idColumn, id);
				if (row == null) {
					StepLog.check(SC + id, "Find the CSV row of " + id, "Row found by " + BulkImportContext.idColumn, "FAIL | no CSV row", false, "id=" + id);
					continue;
				}
				search(id);
				List<UiField> ui = readAllTabs(id);
				if (ui.isEmpty()) {
					BulkImportContext.bug("Customer Information", id, "Customer details did not load or no field was read", "searched by customer id");
					StepLog.check(SC + id, "Read the customer details", "Fields are read from every tab", "FAIL | nothing read", false, "id=" + id);
					continue;
				}
				if (learning && links.isEmpty()) {
					links = fm.suggest(row, ui);
					BulkImportContext.usedLinks.addAll(links);
					saveDraft(code, links, book);
				}
				List<Decision> ds = fm.compare(links, row, ui);
				BulkImportContext.decisions.put(id, ds);
				report(id, ds);
				countCheck(id, frozenCounts, learning);
			} catch (RuntimeException e) {
				if (Com.Ckyc_4_0.functionality.reports.SessionLost.is(e)) {
					throw e;
				}
				StepLog.check(SC + id, "Compare Customer Information with the CSV", "Comparison completed",
						"FAIL | " + e.getClass().getSimpleName() + ": " + e.getMessage(), false, "id=" + id);
			}
		}
		if (learning) {
			saveDraft(code, links, book);
		}
	}

	private void saveDraft(String code, List<Link> links, FieldMapper.CodeBook book) {
		try {
			Path dir = Paths.get(ReportNamingHelper.reportOutputRoot()).getParent().resolve("Current Data").resolve("BulkImport").resolve("mapping");
			Path map = dir.resolve("FieldMap_" + code + ".draft.csv");
			FieldMapper.saveLinks(map, links, BulkImportContext.sectionCounts);
			book.save(dir.resolve("CodeMap_" + code + ".draft.csv"));
			BulkImportContext.draftMappingFile = map.toAbsolutePath().toString();
		} catch (java.io.IOException e) {
			logger.warn("Draft mapping could not be saved: {}", e.getMessage());
		}
	}

	// ------------------------------------------------------------------ page

	private void search(String id) {
		WebDriver d = d();
		openCustomerInformation();
		WebElement box = d.findElement(BulkImportPages.CI_ID_INPUT);
		box.click();
		box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
		box.sendKeys(id);
		BiSupport.sleep(300);
		BiSupport.clickJs(d.findElement(BulkImportPages.CI_SEARCH));
		long end = System.currentTimeMillis() + 25_000;
		while (System.currentTimeMillis() < end) {
			if (BiSupport.present(BulkImportPages.CI_TABS) && containsText(id)) {
				break;
			}
			BiSupport.sleep(500);
		}
		BiSupport.sleep(800);
		// several customers could be listed: open the one with this id
		List<WebElement> card = d.findElements(BulkImportPages.customerCard(id));
		if (!card.isEmpty()) {
			try {
				BiSupport.clickJs(card.get(0));
				BiSupport.sleep(800);
			} catch (RuntimeException ignored) {
				// the details were already open
			}
		}
		Hooks.captureMilestone("Customer Information search | " + id);
	}

	private boolean containsText(String s) {
		try {
			return d().findElement(org.openqa.selenium.By.tagName("body")).getText().contains(s);
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** Every tab: sections with their declared count, labels and values; masked values are revealed and re-read. */
	private List<UiField> readAllTabs(String id) {
		WebDriver d = d();
		List<UiField> out = new ArrayList<>();
		String body = "";
		try {
			body = d.findElement(org.openqa.selenium.By.tagName("body")).getText();
		} catch (RuntimeException ignored) {
			// no body text
		}
		Matcher m = ID_LINE.matcher(body);
		if (m.find()) {
			out.add(new UiField("(header)", "header", "Customer ID", m.group(1), false, false, ""));
			out.add(new UiField("(header)", "header", "CKYC No (card)", m.group(2), false, false, ""));
		}
		List<String> tabs = new ArrayList<>();
		for (WebElement t : d.findElements(BulkImportPages.CI_TABS)) {
			tabs.add(t.getText().trim());
		}
		if (tabs.isEmpty()) {
			tabs.add("(current)");
		}
		StepLog.check(SC + id + " tabs", "Read the tabs of " + id, "All tabs of the customer are read", "tabs=" + tabs, true, "id=" + id);
		for (String tab : tabs) {
			if (!tab.equals("(current)")) {
				List<WebElement> b = d.findElements(org.openqa.selenium.By.xpath("//button[normalize-space()=" + xq(tab) + "]"));
				if (b.isEmpty()) {
					continue;
				}
				BiSupport.clickJs(b.get(0));
				BiSupport.sleep(1000);
			}
			JSONArray sections = readSections();
			Set<String> revealed = new LinkedHashSet<>();
			for (int i = 0; i < sections.length(); i++) {
				JSONArray fs = sections.getJSONObject(i).getJSONArray("fields");
				for (int k = 0; k < fs.length(); k++) {
					JSONObject f = fs.getJSONObject(k);
					if (f.optBoolean("eye") && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(REVEAL_JS, f.getString("label")))) {
						revealed.add(f.getString("label"));
						BiSupport.sleep(500);
					}
				}
			}
			if (!revealed.isEmpty()) {
				sections = readSections(); // the revealed values are on the page now
			}
			for (int i = 0; i < sections.length(); i++) {
				JSONObject sec = sections.getJSONObject(i);
				String secName = sec.getString("name");
				JSONArray fs = sec.getJSONArray("fields");
				BulkImportContext.sectionCounts.put(secName, sec.getInt("declared"));
				for (int k = 0; k < fs.length(); k++) {
					JSONObject f = fs.getJSONObject(k);
					String label = f.getString("label");
					boolean wasRevealed = revealed.contains(label);
					boolean stillMasked = f.optBoolean("masked") && !(wasRevealed && !f.getString("value").matches(".*[X*\\u2022]{3,}.*"));
					out.add(new UiField(tab, secName, label, f.getString("value"), f.optBoolean("masked") && stillMasked, wasRevealed && !stillMasked,
							"//span[normalize-space()=" + xq(label) + "]/../../p"));
				}
				sectionRead.put(id + "|" + secName, new int[] { sec.getInt("declared"), fs.length() });
			}
		}
		Hooks.captureMilestone("Customer Information read | " + id + " | fields=" + out.size());
		return out;
	}

	private final Map<String, int[]> sectionRead = new LinkedHashMap<>();

	private JSONArray readSections() {
		Object o = ((JavascriptExecutor) d()).executeScript(READ_JS);
		return o == null ? new JSONArray() : new JSONArray(String.valueOf(o));
	}

	private static String xq(String s) {
		return s.contains("'") ? "concat('" + s.replace("'", "',\"'\",'") + "')" : "'" + s + "'";
	}

	// ------------------------------------------------------------------ reporting

	private void countCheck(String id, Map<String, Integer> frozen, boolean learning) {
		for (Map.Entry<String, int[]> e : sectionRead.entrySet()) {
			if (!e.getKey().startsWith(id + "|")) {
				continue;
			}
			String sec = e.getKey().substring(id.length() + 1);
			int declared = e.getValue()[0];
			int read = e.getValue()[1];
			boolean same = declared == read;
			StepLog.check(SC + id + " fields", "Section " + sec + " shows as many fields as it declares", declared + " fields",
					"declared=" + declared + "; read=" + read, same, "id=" + id + "; section=" + sec);
			if (!same) {
				BulkImportContext.bug("Customer Information", id, "Section " + sec + " declares " + declared + " fields but " + read + " were read", "");
			}
			Integer was = frozen.get(sec);
			if (!learning && was != null && was != declared) {
				BulkImportContext.bug("Customer Information", id, "Field count of " + sec + " changed: frozen " + was + ", now " + declared,
						declared > was ? "new field(s) added - review the mapping and freeze again" : "field(s) removed");
				StepLog.check(SC + id + " field count", "Field count of " + sec + " equals the frozen count", String.valueOf(was),
						"FAIL | now " + declared, false, "id=" + id + "; section=" + sec);
			}
		}
	}

	private void report(String id, List<Decision> ds) {
		int match = 0;
		int bugs = 0;
		int info = 0;
		for (Decision x : ds) {
			if (x.status().equals(FieldMapper.MATCH)) {
				match++;
				continue;
			}
			String what = x.tab() + " > " + (x.uiLabel().isBlank() ? x.csvCols() : x.uiLabel());
			if (x.bug()) {
				bugs++;
				BulkImportContext.bug("Customer Information", id, what + " - " + x.status(), "csv[" + x.csvCols() + "]='" + x.csvValue() + "' ui='" + x.uiValue() + "' " + x.note());
				StepLog.check(SC + id + " | " + what, "CSV " + x.csvCols() + " is reflected on the screen", "Same value",
						"FAIL | " + x.status() + " | csv='" + x.csvValue() + "' | ui='" + x.uiValue() + "' | " + x.note() + " | xpath=" + x.xpath(), false,
						"id=" + id + "; rule=" + x.rule());
			} else {
				info++;
				StepLog.check(SC + id + " | " + what, "CSV " + x.csvCols() + " is reflected on the screen", "Same value",
						x.status() + " | csv='" + x.csvValue() + "' | ui='" + x.uiValue() + "' | " + x.note(), true, "id=" + id + "; rule=" + x.rule());
			}
		}
		StepLog.check(SC + id + " summary", "Customer Information equals the CSV row", "Every mapped field matches",
				"match=" + match + "; bugs=" + bugs + "; info/unverified=" + info + "; fields compared=" + ds.size(), bugs == 0, "id=" + id);
	}
}
