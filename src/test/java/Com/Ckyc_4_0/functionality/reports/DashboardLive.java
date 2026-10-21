/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.reports;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sub-cards exactly as the dashboard shows them NOW (code, label, count). The list is read from the page, never
 * hard-coded: the dashboard gained B8, B9 and G3 and re-ordered B5-B7, which made a fixed list drift out of date
 * (the card total 107 was then compared with a sum of sub-cards that were not found).
 */
public final class DashboardLive {

	public record SubCard(String code, String label, String group, int count) {
	}

	/** Summary card each code letter belongs to (same grouping as the report categories). */
	private static final Map<String, String> GROUP_BY_LETTER = Map.of(
			"A", "Data pushed from FI",
			"B", "iFlow In Progress",
			"C", "iFlow Rejections",
			"D", "CERSAI In Progress",
			"E", "CERSAI Rejections",
			"F", "CERSAI Success",
			"G", "Reverse Update to CBS");

	/** Sub-cards that open another page instead of a report popup (matched by label, not by code). */
	private static final Set<String> NAVIGATION_LABELS = Set.of(
			"image scan/upload queue", "dvs queue", "confirm match audit queue", "probable match pending in fi");

	private static final String READ_JS = "var out=[];document.querySelectorAll('li').forEach(function(li){"
			+ "var s=li.querySelector('span'),p=li.querySelector('p[aria-label]');"
			+ "if(s&&p&&/^[A-Z]\\d+$/.test((s.innerText||'').trim())){var ps=li.querySelectorAll('p');"
			+ "out.push([(s.innerText||'').trim(),p.getAttribute('aria-label'),(ps[ps.length-1].innerText||'').trim()]);}});"
			+ "return out;";

	private DashboardLive() {
	}

	@SuppressWarnings("unchecked")
	public static List<SubCard> read(WebDriver driver) {
		List<SubCard> out = new ArrayList<>();
		Object o = ((JavascriptExecutor) driver).executeScript(READ_JS);
		if (!(o instanceof List)) {
			return out;
		}
		for (Object row : (List<Object>) o) {
			List<Object> r = (List<Object>) row;
			String code = String.valueOf(r.get(0));
			String label = String.valueOf(r.get(1));
			int count = 0;
			try {
				count = Integer.parseInt(String.valueOf(r.get(2)).replaceAll("[^0-9]", ""));
			} catch (NumberFormatException ignored) {
				// unreadable count stays 0 - the compare will show the real report count
			}
			out.add(new SubCard(code, label, groupOf(code), count));
		}
		return out;
	}

	public static String groupOf(String code) {
		return code == null || code.isEmpty() ? "" : GROUP_BY_LETTER.getOrDefault(code.substring(0, 1), "");
	}

	public static boolean navigationOnly(String label) {
		return label != null && NAVIGATION_LABELS.contains(
				label.trim().toLowerCase(Locale.ROOT).replaceAll("\\s*/\\s*", "/").replaceAll("\\s+", " "));
	}
}
