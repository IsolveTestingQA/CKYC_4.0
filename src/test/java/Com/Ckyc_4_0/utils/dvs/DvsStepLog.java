/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Step log (L1-L3): one row per action - click, type, select, tab switch, Save, Submit, Revert, reload,
 * restore, retry, compare. Rows are kept for the Step_Log sheet and appended to logs/&lt;Run_ID&gt;.log
 * immediately (flushed per row, so a crash loses nothing).
 */
public final class DvsStepLog {

	public static final String[] COLUMNS = { "Seq", "Run_ID", "Timestamp_IST", "TC_ID", "Customer_ID", "Persona", "Role", "Tab",
			"Field_Locator_ID", "Action", "Value_Before", "Value_Entered", "Value_After", "Expected", "Actual", "Inline_Error",
			"Toast_Text", "Dialog_Text", "Item_Error_List", "API_Status", "Screenshot_Path", "Step_Result", "Duration_ms" };

	public static final String PASS = "PASS";
	public static final String FAIL = "FAIL";
	public static final String INFO = "INFO";
	public static final String RETRY = "RETRY";
	public static final String RESTORE = "RESTORE";

	private static final DateTimeFormatter IST = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
	private static final List<String[]> ROWS = new ArrayList<>();

	private static String runId = "DVS_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
	private static BufferedWriter out;
	private static String tcId = "";
	private static String customerId = "";
	private static String persona = "";
	private static String role = "Maker";
	private static String tab = "";

	private DvsStepLog() {
	}

	public static synchronized void startRun(String mode) {
		close();
		ROWS.clear();
		runId = "DVS_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "_" + mode;
		tcId = "";
		customerId = "";
		persona = "";
		role = "Maker";
		tab = "";
		try {
			Path file = new File(System.getProperty("user.dir"), "logs" + File.separator + runId + ".log").toPath();
			Files.createDirectories(file.getParent());
			out = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			out.write(String.join(" | ", COLUMNS));
			out.newLine();
			out.flush();
		} catch (IOException e) {
			out = null;
		}
	}

	public static synchronized void close() {
		if (out != null) {
			try {
				out.flush();
				out.close();
			} catch (IOException ignored) {
				// log file already unusable
			}
			out = null;
		}
	}

	public static String runId() {
		return runId;
	}

	public static synchronized void context(String tc, String customer, String personaName, String roleName) {
		tcId = nz(tc);
		customerId = nz(customer);
		persona = nz(personaName);
		role = nz(roleName);
	}

	public static synchronized void tc(String tc) {
		tcId = nz(tc);
	}

	public static synchronized void persona(String p) {
		persona = nz(p);
	}

	public static synchronized void role(String r) {
		role = nz(r);
	}

	public static synchronized void tab(String t) {
		tab = nz(t);
	}

	public static synchronized int size() {
		return ROWS.size();
	}

	/** Text such as "#12-#19" for the rows added since mark (empty when none). */
	public static synchronized String rangeSince(int mark) {
		int from = mark + 1;
		int to = ROWS.size();
		if (to < from) {
			return "";
		}
		return from == to ? "#" + from : "#" + from + "-#" + to;
	}

	public static synchronized List<String[]> rows() {
		return new ArrayList<>(ROWS);
	}

	public static Step step(String action) {
		return new Step(action);
	}

	/** Fluent builder: DvsStepLog.step("type").field(id).entered(v).result(PASS).log(). */
	public static final class Step {
		private final long started = System.currentTimeMillis();
		private final String action;
		private String field = "";
		private String before = "";
		private String entered = "";
		private String after = "";
		private String expected = "";
		private String actual = "";
		private String inline = "";
		private String toast = "";
		private String dialog = "";
		private String items = "";
		private String api = "";
		private String shot = "";
		private String result = INFO;

		private Step(String action) {
			this.action = action;
		}

		public Step field(String v) {
			field = nz(v);
			return this;
		}

		public Step before(String v) {
			before = nz(v);
			return this;
		}

		public Step entered(String v) {
			entered = nz(v);
			return this;
		}

		public Step after(String v) {
			after = nz(v);
			return this;
		}

		public Step expected(String v) {
			expected = nz(v);
			return this;
		}

		public Step actual(String v) {
			actual = nz(v);
			return this;
		}

		public Step inline(String v) {
			inline = nz(v);
			return this;
		}

		public Step toast(String v) {
			toast = nz(v);
			return this;
		}

		public Step dialog(String v) {
			dialog = nz(v);
			return this;
		}

		public Step items(String v) {
			items = nz(v);
			return this;
		}

		public Step api(String v) {
			api = nz(v);
			return this;
		}

		public Step shot(String v) {
			shot = nz(v);
			return this;
		}

		public Step result(String v) {
			result = nz(v);
			return this;
		}

		public void log() {
			add(this);
		}
	}

	private static synchronized void add(Step s) {
		int seq = ROWS.size() + 1;
		String[] row = { String.valueOf(seq), runId, ZonedDateTime.now(ZONE).format(IST), tcId, customerId, persona, role, tab,
				s.field, s.action, s.before, s.entered, s.after, s.expected, s.actual, s.inline, s.toast, s.dialog, s.items,
				s.api, s.shot, s.result, String.valueOf(System.currentTimeMillis() - s.started) };
		ROWS.add(row);
		if (out != null) {
			try {
				out.write(String.join(" | ", flat(row)));
				out.newLine();
				out.flush();
			} catch (IOException ignored) {
				// keep running; rows stay in memory for the Step_Log sheet
			}
		}
	}

	private static String[] flat(String[] row) {
		String[] f = new String[row.length];
		for (int i = 0; i < row.length; i++) {
			f[i] = row[i].replace('\r', ' ').replace('\n', ' ');
		}
		return f;
	}

	private static String nz(String v) {
		return v == null ? "" : v;
	}
}
