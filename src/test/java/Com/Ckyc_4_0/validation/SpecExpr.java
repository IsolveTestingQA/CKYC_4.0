/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Tiny evaluator for the "Python-style expression over column names" used in the spec's
 * {@code when} (conditional-mandatory) and {@code pack_fill} fields — see CLAUDE_INSTRUCTIONS.md
 * in CKYC_OFFICIAL_DOCUMENTS/CKYC_Cosmos_Validation_Spec. Java has no {@code eval()}, so this is a
 * small hand-written recursive-descent parser for the grammar actually used by the spec:
 *
 * <pre>
 *   expr       := orExpr
 *   orExpr     := andExpr ("or" andExpr)*
 *   andExpr    := notExpr ("and" notExpr)*
 *   notExpr    := "not" notExpr | comparison
 *   comparison := atom (("==" | "!=" | "in") atom)?
 *   atom       := STRING | "True" | "False" | "(" expr ")" | tuple | IDENT ["(" [STRING] ")"]
 *   tuple      := "(" STRING ("," STRING)* ")"
 * </pre>
 *
 * A bare {@code IDENT} not followed by "(" is a column reference: true when
 * {@code row.get(IDENT)} is non-blank (mirrors Python truthiness for a string). {@code IDENT(...)}
 * calls into the supplied {@code functions} registry (e.g. {@code no_other_rp_id()},
 * {@code rp_doc_used('02')}).
 */
public final class SpecExpr {

	private SpecExpr() {
	}

	public static boolean eval(String expr, Map<String, String> row, Function<String[], Boolean> functions) {
		if (expr == null || expr.isBlank()) {
			return false;
		}
		List<String> tokens = tokenize(expr);
		Parser p = new Parser(tokens, row, functions);
		boolean result = p.parseOr();
		if (p.pos != tokens.size()) {
			throw new IllegalArgumentException("Unexpected trailing tokens in expr: " + expr);
		}
		return result;
	}

	// ---------------------------------------------------------------- tokenizer
	private static List<String> tokenize(String expr) {
		List<String> tokens = new ArrayList<>();
		int i = 0;
		int n = expr.length();
		while (i < n) {
			char ch = expr.charAt(i);
			if (Character.isWhitespace(ch)) {
				i++;
				continue;
			}
			if (ch == '\'' || ch == '"') {
				char quote = ch;
				int j = i + 1;
				StringBuilder sb = new StringBuilder();
				while (j < n && expr.charAt(j) != quote) {
					sb.append(expr.charAt(j));
					j++;
				}
				tokens.add("'" + sb + "'");
				i = j + 1;
				continue;
			}
			if (ch == '(' || ch == ')' || ch == ',') {
				tokens.add(String.valueOf(ch));
				i++;
				continue;
			}
			if (ch == '=' && i + 1 < n && expr.charAt(i + 1) == '=') {
				tokens.add("==");
				i += 2;
				continue;
			}
			if (ch == '!' && i + 1 < n && expr.charAt(i + 1) == '=') {
				tokens.add("!=");
				i += 2;
				continue;
			}
			if (Character.isLetterOrDigit(ch) || ch == '_') {
				int j = i;
				while (j < n && (Character.isLetterOrDigit(expr.charAt(j)) || expr.charAt(j) == '_')) {
					j++;
				}
				tokens.add(expr.substring(i, j));
				i = j;
				continue;
			}
			throw new IllegalArgumentException("Unexpected character '" + ch + "' in expr: " + expr);
		}
		return tokens;
	}

	private static boolean isStringLiteral(String t) {
		return t.length() >= 2 && t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\'';
	}

	private static String unquote(String t) {
		return t.substring(1, t.length() - 1);
	}

	// ---------------------------------------------------------------- recursive-descent parser
	private static final class Parser {
		final List<String> tokens;
		final Map<String, String> row;
		final Function<String[], Boolean> functions;
		int pos = 0;

		Parser(List<String> tokens, Map<String, String> row, Function<String[], Boolean> functions) {
			this.tokens = tokens;
			this.row = row;
			this.functions = functions;
		}

		String peek() {
			return pos < tokens.size() ? tokens.get(pos) : null;
		}

		String next() {
			return tokens.get(pos++);
		}

		boolean parseOr() {
			boolean v = parseAnd();
			while ("or".equals(peek())) {
				next();
				boolean rhs = parseAnd();
				v = v || rhs;
			}
			return v;
		}

		boolean parseAnd() {
			boolean v = parseNot();
			while ("and".equals(peek())) {
				next();
				boolean rhs = parseNot();
				v = v && rhs;
			}
			return v;
		}

		boolean parseNot() {
			if ("not".equals(peek())) {
				next();
				return !parseNot();
			}
			return parseComparison();
		}

		boolean parseComparison() {
			String left = parseAtomRaw();
			String op = peek();
			if ("==".equals(op) || "!=".equals(op)) {
				next();
				String right = parseAtomRaw();
				boolean eq = left.equals(right);
				return "==".equals(op) == eq;
			}
			if ("in".equals(op)) {
				next();
				List<String> tuple = parseTuple();
				return tuple.contains(left);
			}
			// bare atom used as a boolean (column truthiness, True/False, or a function call)
			return !"".equals(left) && !"False".equals(left);
		}

		/** Parses one atom and returns its string value (column value, literal, or function-call result as "True"/"False"). */
		String parseAtomRaw() {
			String t = next();
			if (isStringLiteral(t)) {
				return unquote(t);
			}
			if ("True".equals(t)) {
				return "True";
			}
			if ("False".equals(t)) {
				return "False";
			}
			if ("(".equals(t)) {
				boolean v = parseOr();
				expect(")");
				return v ? "True" : "False";
			}
			// identifier: either a function call IDENT(arg?) or a column reference
			if ("(".equals(peek())) {
				next();
				String arg = null;
				if (!")".equals(peek())) {
					String a = next();
					arg = isStringLiteral(a) ? unquote(a) : a;
				}
				expect(")");
				boolean result = Boolean.TRUE.equals(functions.apply(new String[] { t, arg }));
				return result ? "True" : "False";
			}
			String v = row.get(t);
			return v == null ? "" : v;
		}

		List<String> parseTuple() {
			expect("(");
			List<String> values = new ArrayList<>();
			if (!")".equals(peek())) {
				values.add(unquote(next()));
				while (",".equals(peek())) {
					next();
					values.add(unquote(next()));
				}
			}
			expect(")");
			return values;
		}

		void expect(String tok) {
			String t = next();
			if (!tok.equals(t)) {
				throw new IllegalArgumentException("Expected '" + tok + "' but got '" + t + "'");
			}
		}
	}
}
