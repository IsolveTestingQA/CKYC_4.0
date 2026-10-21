/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.panel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import io.cucumber.testng.Pickle;
import io.cucumber.testng.PickleWrapper;

/**
 * The order the Test Control Panel runs the chosen modules in. Cucumber alone would run feature files in folder-name
 * order (which put Concurrent session before DVS and Reports); here the order is the one a tester expects and the one the
 * data needs: Login first, then Dashboard, Masters, Search, User Management (Roles before Users, because Add User uses
 * the approved TEST AUTO role), DVS, Reports - and Concurrent session last, because it signs the first session out.
 * Inside a module Positive runs before Negative. Within one tag the original order (feature file, line) is kept.
 */
public final class ExecutionOrder {

	private static final List<String> TAG_ORDER = List.of("@Login", "@Dashboard", "@StateMaster", "@DistrictMaster",
			"@PincodeMaster", "@HierarchyMaster", "@Search", "@SearchInteractive", "@Download", "@RolesPositive",
			"@RolesNegative", "@UsersPositive", "@UsersNegative", "@DVS_IND_POS", "@DVS_IND_NEG", "@DVS_LE_POS",
			"@DVS_LE_NEG", "@DVS_POS", "@DVS_NEG", "@DVS_COMBINED", "@DVS_REG", "@Reports", "@BulkImport", "@ConcurrentSession");

	private ExecutionOrder() {
	}

	public static int rank(List<String> pickleTags) {
		int best = Integer.MAX_VALUE;
		for (String t : pickleTags) {
			int i = TAG_ORDER.indexOf(t);
			if (i >= 0 && i < best) {
				best = i;
			}
		}
		return best == Integer.MAX_VALUE ? TAG_ORDER.size() : best;
	}

	/** Rows are {PickleWrapper, FeatureWrapper} as produced by the Cucumber TestNG data provider. */
	public static Object[][] sort(Object[][] rows) {
		if (rows == null || rows.length < 2) {
			return rows;
		}
		List<Object[]> list = new ArrayList<>(Arrays.asList(rows));
		list.sort(Comparator.comparingInt(ExecutionOrder::rankOf)); // List.sort is stable
		return list.toArray(new Object[0][]);
	}

	private static int rankOf(Object[] row) {
		try {
			Pickle p = ((PickleWrapper) row[0]).getPickle();
			return rank(p.getTags());
		} catch (RuntimeException e) {
			return TAG_ORDER.size();
		}
	}

	/** Names of the modules in run order, for the panel summary / reports. */
	public static List<String> tagOrder() {
		return TAG_ORDER;
	}
}
