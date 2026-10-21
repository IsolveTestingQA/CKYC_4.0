/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.reports;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** Small JSON writer (no library): enough for report tables and oversized cell values. */
public final class JsonText {

	private JsonText() {
	}

	public static String quote(String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
				}
			}
		}
		return sb.append('"').toString();
	}

	/** Header names made unique for JSON object keys ("Remarks", "Remarks (2)"). */
	public static List<String> uniqueKeys(List<String> headers) {
		Set<String> seen = new LinkedHashSet<>();
		java.util.ArrayList<String> out = new java.util.ArrayList<>();
		for (String h : headers) {
			String k = h == null || h.isBlank() ? "(column)" : h;
			String key = k;
			int n = 2;
			while (!seen.add(key)) {
				key = k + " (" + n++ + ")";
			}
			out.add(key);
		}
		return out;
	}

	public static String table(ReportTable t) {
		List<String> keys = uniqueKeys(t.headers());
		int keyIdx = t.keyColumnIndex();
		StringBuilder sb = new StringBuilder(1024);
		sb.append("{\n");
		sb.append("  \"source\": ").append(quote(t.source())).append(",\n");
		sb.append("  \"application\": ").append(quote(t.application())).append(",\n");
		sb.append("  \"constitution\": ").append(quote(t.constitution())).append(",\n");
		sb.append("  \"category\": ").append(quote(t.category())).append(",\n");
		sb.append("  \"reportType\": ").append(quote(t.reportType())).append(",\n");
		sb.append("  \"code\": ").append(quote(t.code())).append(",\n");
		sb.append("  \"declaredCount\": ").append(t.declaredCount() == null ? "null" : t.declaredCount()).append(",\n");
		sb.append("  \"declaredFrom\": ").append(quote(t.declaredFrom())).append(",\n");
		sb.append("  \"cardCount\": ").append(t.cardCount() == null ? "null" : t.cardCount()).append(",\n");
		sb.append("  \"notExecuted\": ").append(t.isNotExecuted() ? quote(t.notExecutedReason()) : "null").append(",\n");
		sb.append("  \"customerKeyColumn\": ").append(keyIdx < 0 ? "null" : quote(t.headers().get(keyIdx))).append(",\n");
		sb.append("  \"headers\": [");
		for (int i = 0; i < t.headers().size(); i++) {
			sb.append(i == 0 ? "" : ", ").append(quote(t.headers().get(i)));
		}
		sb.append("],\n");
		sb.append("  \"rowCount\": ").append(t.rows().size()).append(",\n");
		sb.append("  \"rows\": [\n");
		for (int r = 0; r < t.rows().size(); r++) {
			List<String> row = t.rows().get(r);
			sb.append("    {");
			for (int c = 0; c < keys.size(); c++) {
				sb.append(c == 0 ? "" : ", ").append(quote(keys.get(c))).append(": ")
						.append(quote(c < row.size() ? row.get(c) : ""));
			}
			sb.append(r + 1 < t.rows().size() ? "},\n" : "}\n");
		}
		sb.append("  ]\n}\n");
		return sb.toString();
	}

	/** A value too large for one Excel cell. */
	public static String largeValue(String context, String value) {
		return "{\n  \"context\": " + quote(context) + ",\n  \"length\": " + (value == null ? 0 : value.length())
				+ ",\n  \"value\": " + quote(value) + "\n}\n";
	}
}
