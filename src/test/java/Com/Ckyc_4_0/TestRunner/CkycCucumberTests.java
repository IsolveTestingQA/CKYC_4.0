/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import org.testng.annotations.DataProvider;

import Com.Ckyc_4_0.utils.panel.ExecutionOrder;
import Com.Ckyc_4_0.utils.panel.RunSelection;
import io.cucumber.testng.AbstractTestNGCucumberTests;

/**
 * Base of every runner. When the Test Control Panel was used the chosen scenarios are run in the panel's module order
 * (Login, Dashboard, Masters, Search, User Management, DVS, Reports, Concurrent session; Positive before Negative)
 * instead of the folder-name order Cucumber would use. Without the panel nothing changes.
 */
public abstract class CkycCucumberTests extends AbstractTestNGCucumberTests {

	@Override
	@DataProvider(name = "scenarios")
	public Object[][] scenarios() {
		Object[][] rows = super.scenarios();
		if (RunSelection.current() == null) {
			return rows;
		}
		return ExecutionOrder.sort(rows);
	}
}
