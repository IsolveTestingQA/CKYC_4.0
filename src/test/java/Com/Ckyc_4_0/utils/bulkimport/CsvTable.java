/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.bulkimport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A delimited file read with its own header row: every row is a column-name to value map, so nothing depends on column
 * positions (the Individual, Minor and Legal Entity files all have different headers). The delimiter (| , or tab) is
 * detected from the header line.
 */
public final class CsvTable {

	private final Path file;
	private final char delimiter;
	private final List<String> headers;
	private final List<Map<String, String>> rows;

	private CsvTable(Path file, char delimiter, List<String> headers, List<Map<String, String>> rows) {
		this.file = file;
		this.delimiter = delimiter;
		this.headers = headers;
		this.rows = rows;
	}

	public static CsvTable read(Path file) throws IOException {
		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		while (!lines.isEmpty() && lines.get(0).replace("﻿", "").isBlank()) {
			lines.remove(0);
		}
		if (lines.isEmpty()) {
			throw new IOException("Empty file: " + file);
		}
		String head = lines.get(0).replace("﻿", "");
		char d = detect(head);
		List<String> headers = new ArrayList<>();
		for (String h : split(head, d)) {
			headers.add(h.trim());
		}
		List<Map<String, String>> rows = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			if (lines.get(i).isBlank()) {
				continue;
			}
			List<String> cells = split(lines.get(i), d);
			Map<String, String> row = new LinkedHashMap<>();
			for (int c = 0; c < headers.size(); c++) {
				row.put(headers.get(c), c < cells.size() ? cells.get(c).trim() : "");
			}
			rows.add(row);
		}
		return new CsvTable(file, d, headers, rows);
	}

	public Path file() {
		return file;
	}

	public char delimiter() {
		return delimiter;
	}

	public List<String> headers() {
		return headers;
	}

	public List<Map<String, String>> rows() {
		return rows;
	}

	/** The header as written in the file for a name given in any case, or null. */
	public String header(String name) {
		for (String h : headers) {
			if (h.equalsIgnoreCase(name)) {
				return h;
			}
		}
		return null;
	}

	/** First header containing every given word (case-insensitive, ignoring _ and spaces), or null. */
	public String headerContaining(String... words) {
		for (String h : headers) {
			String k = h.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
			boolean all = true;
			for (String w : words) {
				all &= k.contains(w.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", ""));
			}
			if (all) {
				return h;
			}
		}
		return null;
	}

	/** The column whose values contain most of the given ids (used to find the customer reference column by data). */
	public String columnHolding(java.util.Collection<String> ids) {
		String best = null;
		int bestHits = 0;
		for (String h : headers) {
			int hits = 0;
			for (Map<String, String> r : rows) {
				if (ids.contains(r.get(h))) {
					hits++;
				}
			}
			if (hits > bestHits) {
				bestHits = hits;
				best = h;
			}
		}
		return best;
	}

	public Map<String, String> rowWhere(String column, String value) {
		for (Map<String, String> r : rows) {
			if (value != null && value.equalsIgnoreCase(r.get(column))) {
				return r;
			}
		}
		return null;
	}

	private static char detect(String head) {
		int pipe = count(head, '|');
		int comma = count(head, ',');
		int tab = count(head, '\t');
		if (pipe >= comma && pipe >= tab && pipe > 0) {
			return '|';
		}
		return tab > comma ? '\t' : ',';
	}

	private static int count(String s, char c) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				n++;
			}
		}
		return n;
	}

	/** Quote-aware split. */
	static List<String> split(String line, char d) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (ch == '"') {
				if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					cur.append('"');
					i++;
				} else {
					quoted = !quoted;
				}
			} else if (ch == d && !quoted) {
				out.add(cur.toString());
				cur.setLength(0);
			} else {
				cur.append(ch);
			}
		}
		out.add(cur.toString());
		return out;
	}
}
