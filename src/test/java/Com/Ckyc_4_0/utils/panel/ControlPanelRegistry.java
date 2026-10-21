/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.panel;

import java.util.List;
import java.util.Map;

/**
 * The modules the Test Control Panel offers, and the Cucumber tags that select their scenarios. A new module or
 * sub-module needs ONE entry here (and its feature file tagged with the same tag).
 *
 * pos / neg = the tag that selects the positive / negative scenarios of that part (null = none exists).
 */
public final class ControlPanelRegistry {

	public record Sub(String key, String title, String pos, String neg) {
	}

	/** A group of tick-boxes that feeds a config key (comma separated), e.g. reports.apps. */
	public record Group(String key, String label, String configKey, List<String> options, List<String> defaults,
			Map<String, List<String>> quick) {
	}

	/** Label -> ticked options, in the order the quick-pick buttons are shown. */
	private static Map<String, List<String>> quick(Object... labelAndOptions) {
		Map<String, List<String>> m = new java.util.LinkedHashMap<>();
		for (int i = 0; i + 1 < labelAndOptions.length; i += 2) {
			@SuppressWarnings("unchecked")
			List<String> opts = (List<String>) labelAndOptions[i + 1];
			m.put(String.valueOf(labelAndOptions[i]), opts);
		}
		return m;
	}

	/** A text box that feeds a config key, e.g. dvs.cust.individual. */
	public record Text(String key, String label, String configKey, String fallback) {
	}

	public record Module(String key, String title, boolean needsLoginFirst, List<Sub> subs, List<Group> groups,
			List<Text> texts) {
	}

	public static final String NOTHING_TAG = "@ControlPanelNothingSelected";

	private static final List<Module> MODULES = List.of(
			new Module("login", "Login", false,
					List.of(new Sub("login", "Login", "@Login", null)), List.of(), List.of()),
			new Module("dashboard", "Dashboard", false,
					List.of(new Sub("dashboard", "Dashboard counts, filters and reports", "@Dashboard", null)), List.of(), List.of()),
			new Module("masters", "Masters", false,
					List.of(new Sub("state", "State", "@StateMaster", null),
							new Sub("district", "District", "@DistrictMaster", null),
							new Sub("pincode", "Pincode", "@PincodeMaster", null),
							new Sub("hierarchy", "Hierarchy", "@HierarchyMaster", null)),
					List.of(), List.of()),
			new Module("search", "Search And Download", false,
					List.of(new Sub("search", "Search - configured values", "@Search", null),
							new Sub("interactive", "Search - interactive (popups ask for type and number)", "@SearchInteractive",
									null)),
					List.of(), List.of()),
			new Module("usermgmt", "User Management", false,
					List.of(new Sub("users", "Users", "@UsersPositive", "@UsersNegative"),
							new Sub("roles", "Roles", "@RolesPositive", "@RolesNegative")),
					List.of(), List.of()),
			new Module("concurrent", "Concurrent session", false,
					List.of(new Sub("concurrent", "Second login logs the first out", "@ConcurrentSession", null)), List.of(),
					List.of()),
			new Module("dvs", "DVS 2.0", true,
					List.of(new Sub("individual", "Individual", "@DVS_IND_POS", "@DVS_IND_NEG"),
							new Sub("legal", "Legal Entity", "@DVS_LE_POS", "@DVS_LE_NEG")),
					List.of(),
					List.of(new Text("custInd", "Individual customer id", "dvs.cust.individual", "IN05100004"),
							new Text("custLe", "Legal entity customer id", "dvs.cust.le", "LE02100010"))),
			new Module("dvswb", "DVS 2.0 - master test-data workbook", true,
					List.of(new Sub("positive", "Positive data", "@DVS_POS", null),
							new Sub("negative", "Negative data (grouped, record restored)", "@DVS_NEG", null),
							new Sub("combined", "Combined (positive then negatives)", "@DVS_COMBINED", null),
							new Sub("regression", "Regression", "@DVS_REG", null)),
					List.of(), List.of()),
			new Module("reports", "Reports compare (Weekly)", false,
					List.of(new Sub("compare", "Dashboard popups vs Reports module", "@Reports", null)),
					List.of(new Group("apps", "Application", "reports.apps", List.of("NEW", "UPDATE"), List.of("NEW", "UPDATE"),
									quick("NEW only", List.of("NEW"), "UPDATE only", List.of("UPDATE"), "Both NEW + UPDATE",
											List.of("NEW", "UPDATE"))),
							new Group("cons", "Constitution", "reports.constitutions",
									List.of("ALL", "Individual", "Legal entity"), List.of("ALL", "Individual", "Legal entity"),
									quick("Individual only", List.of("Individual"), "Legal entity only", List.of("Legal entity"),
											"Both Individual + Legal entity", List.of("Individual", "Legal entity"),
											"ALL only", List.of("ALL"), "Everything (ALL + Individual + Legal entity)",
											List.of("ALL", "Individual", "Legal entity")))),
					List.of()),
			new Module("bulkimport", "Bulk Import to Customer Information (uploads a CSV - live data)", false,
					List.of(new Sub("flow", "Upload CSV, monitor iFlow, Dashboard wait, Customer Information compare", "@BulkImport", null)),
					List.of(),
					List.of(new Text("cnt", "Customers to compare", "bulkimport.compareCount", "3"))));

	private ControlPanelRegistry() {
	}

	public static List<Module> modules() {
		return MODULES;
	}

	public static Module module(String key) {
		for (Module m : MODULES) {
			if (m.key().equals(key)) {
				return m;
			}
		}
		return null;
	}

	/** Tags of scenarios that must run for the selected modules / sub-modules in the chosen kind of testing. */
	public static String tagExpression(String mode, Map<String, List<String>> selected, List<String> notes) {
		java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>();
		boolean needLogin = false;
		boolean loginChosen = false;
		boolean wantPos = !"NEGATIVE".equals(mode);
		boolean wantNeg = !"POSITIVE".equals(mode);
		for (Map.Entry<String, List<String>> e : selected.entrySet()) {
			Module m = module(e.getKey());
			if (m == null) {
				continue;
			}
			if (m.key().equals("login")) {
				loginChosen = true;
			}
			needLogin |= m.needsLoginFirst();
			for (Sub s : m.subs()) {
				if (!e.getValue().contains(s.key())) {
					continue;
				}
				boolean added = false;
				if (wantPos && s.pos() != null) {
					tags.add(s.pos());
					added = true;
				}
				if (wantNeg && s.neg() != null) {
					tags.add(s.neg());
					added = true;
				}
				if (!added) {
					notes.add(m.title() + " - " + s.title() + ": no "
							+ ("NEGATIVE".equals(mode) ? "negative" : "positive") + " scenarios exist, nothing to run");
				}
			}
		}
		if (needLogin && !loginChosen && !tags.isEmpty()) {
			tags.add("@Login"); // DVS opens its pages from the session the Login scenario creates
		}
		return tags.isEmpty() ? NOTHING_TAG : String.join(" or ", tags);
	}
}
