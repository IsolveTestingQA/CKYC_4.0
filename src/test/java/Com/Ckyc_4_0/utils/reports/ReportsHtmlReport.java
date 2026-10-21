/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.reports;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Self-contained HTML report (no internet, no libraries) for the Reports vs Dashboard comparison: KPI tiles, an
 * "at a glance" matrix (every dashboard sub-card against NEW / UPDATE x ALL / Individual / Legal entity), filterable
 * cases with expected (dashboard) next to actual (Reports module), the differences per customer and column, the
 * screenshots and links to the full JSON data. Template: /Reports/report_template.html.
 */
public final class ReportsHtmlReport {

	/** One screenshot: label shown on the page and the file path (absolute or relative to the HTML file). */
	public record Evidence(String label, String path) {
	}

	/** One comparison case, as shown on the page. */
	public record Case(String id, String app, String con, String category, String card, String reportType, String dashReport,
			String status, String summary, String dashCount, String repCount, String dashRows, String repRows,
			String dashRange, String repRange, List<String> dashHeaders, List<String> repHeaders, List<String> notes,
			List<ReportComparer.Diff> diffs, List<Evidence> evidence, String dashJson, String repJson) {
	}

	private ReportsHtmlReport() {
	}

	public static void write(Path html, String workbookName, String generated, Map<String, String> info, String abort,
			List<Case> cases, Map<String, String> dashRanges, Map<String, String> repRanges) throws IOException {
		String template;
		try (InputStream in = ReportsHtmlReport.class.getResourceAsStream("/Reports/report_template.html")) {
			if (in == null) {
				throw new IOException("Reports/report_template.html not found on the classpath");
			}
			template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		Files.createDirectories(html.toAbsolutePath().getParent());
		Files.writeString(html, template.replace("__DATA__", data(workbookName, generated, info, abort, cases, dashRanges, repRanges)),
				StandardCharsets.UTF_8);
	}

	static String data(String workbookName, String generated, Map<String, String> info, String abort, List<Case> cases,
			Map<String, String> dashRanges, Map<String, String> repRanges) {
		Set<String> apps = new LinkedHashSet<>();
		Set<String> cons = new LinkedHashSet<>();
		Map<String, String[]> combos = new LinkedHashMap<>();
		for (Case c : cases) {
			if (!c.app().isEmpty() && !c.app().equals("n/a")) {
				apps.add(c.app());
			}
			if (!c.con().isEmpty() && !c.con().equals("n/a")) {
				cons.add(c.con());
			}
		}
		for (String a : apps) {
			for (String c : cons) {
				combos.put(a + " / " + c, new String[] { a, c });
			}
		}
		StringBuilder sb = new StringBuilder(8192);
		sb.append("{\"testedBy\":").append(JsonText.quote(info.getOrDefault("Tested by", "Aravindhan")));
		sb.append(",\"generated\":").append(JsonText.quote(generated));
		sb.append(",\"workbook\":").append(JsonText.quote(workbookName));
		sb.append(",\"abort\":").append(JsonText.quote(abort == null ? "" : abort));
		sb.append(",\"apps\":").append(array(apps));
		sb.append(",\"cons\":").append(array(cons));
		sb.append(",\"combos\":[");
		boolean first = true;
		for (Map.Entry<String, String[]> e : combos.entrySet()) {
			sb.append(first ? "" : ",").append("{\"app\":").append(JsonText.quote(e.getValue()[0]))
					.append(",\"con\":").append(JsonText.quote(e.getValue()[1]))
					.append(",\"dashRange\":").append(JsonText.quote(dashRanges.getOrDefault(e.getKey(), "")))
					.append(",\"repRange\":").append(JsonText.quote(repRanges.getOrDefault(e.getKey(), ""))).append('}');
			first = false;
		}
		sb.append("],\"info\":[");
		first = true;
		for (Map.Entry<String, String> e : info.entrySet()) {
			sb.append(first ? "" : ",").append('[').append(JsonText.quote(e.getKey())).append(',')
					.append(JsonText.quote(e.getValue())).append(']');
			first = false;
		}
		sb.append("],\"cases\":[");
		first = true;
		for (Case c : cases) {
			sb.append(first ? "" : ",\n");
			first = false;
			sb.append("{\"id\":").append(JsonText.quote(c.id()));
			sb.append(",\"app\":").append(JsonText.quote(c.app()));
			sb.append(",\"con\":").append(JsonText.quote(c.con()));
			sb.append(",\"category\":").append(JsonText.quote(c.category()));
			sb.append(",\"card\":").append(JsonText.quote(c.card()));
			sb.append(",\"reportType\":").append(JsonText.quote(c.reportType()));
			sb.append(",\"dashReport\":").append(JsonText.quote(c.dashReport()));
			sb.append(",\"status\":").append(JsonText.quote(c.status()));
			sb.append(",\"summary\":").append(JsonText.quote(c.summary()));
			sb.append(",\"dashCount\":").append(JsonText.quote(c.dashCount()));
			sb.append(",\"repCount\":").append(JsonText.quote(c.repCount()));
			sb.append(",\"dashRows\":").append(JsonText.quote(c.dashRows()));
			sb.append(",\"repRows\":").append(JsonText.quote(c.repRows()));
			sb.append(",\"dashRange\":").append(JsonText.quote(c.dashRange()));
			sb.append(",\"repRange\":").append(JsonText.quote(c.repRange()));
			sb.append(",\"dashHeaders\":").append(array(c.dashHeaders()));
			sb.append(",\"repHeaders\":").append(array(c.repHeaders()));
			sb.append(",\"notes\":").append(array(c.notes()));
			sb.append(",\"dashJson\":").append(JsonText.quote(c.dashJson() == null ? "" : c.dashJson()));
			sb.append(",\"repJson\":").append(JsonText.quote(c.repJson() == null ? "" : c.repJson()));
			sb.append(",\"diffs\":[");
			List<ReportComparer.Diff> ds = c.diffs();
			for (int i = 0; i < ds.size(); i++) {
				ReportComparer.Diff d = ds.get(i);
				sb.append(i == 0 ? "" : ",").append("{\"kind\":").append(JsonText.quote(d.kind()))
						.append(",\"customer\":").append(JsonText.quote(d.customer()))
						.append(",\"column\":").append(JsonText.quote(d.column()))
						.append(",\"dashboard\":").append(JsonText.quote(shorten(d.dashboard())))
						.append(",\"report\":").append(JsonText.quote(shorten(d.report()))).append('}');
			}
			sb.append("],\"evidence\":[");
			List<Evidence> ev = c.evidence();
			for (int i = 0; i < ev.size(); i++) {
				sb.append(i == 0 ? "" : ",").append("{\"label\":").append(JsonText.quote(ev.get(i).label()))
						.append(",\"href\":").append(JsonText.quote(ev.get(i).path().replace('\\', '/'))).append('}');
			}
			sb.append("]}");
		}
		sb.append("]}");
		// the data sits inside a <script>: nothing in it may close the tag
		return sb.toString().replace("</", "<\\/");
	}

	private static String array(Iterable<String> items) {
		List<String> l = new ArrayList<>();
		for (String s : items) {
			l.add(JsonText.quote(s));
		}
		return "[" + String.join(",", l) + "]";
	}

	private static String shorten(String s) {
		return s != null && s.length() > 300 ? s.substring(0, 300) + "... (" + s.length() + " chars - see JSON)" : s;
	}
}
