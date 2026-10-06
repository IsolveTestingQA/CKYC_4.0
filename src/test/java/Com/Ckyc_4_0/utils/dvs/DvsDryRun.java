/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import Com.Ckyc_4_0.functionality.dvs.DvsRunEngine;

/**
 * Offline check (no browser): plans every row of every mode and writes the result workbooks.
 * Run this class from IntelliJ to see how many rows are runnable and which are not mapped yet.
 */
public final class DvsDryRun {

	private DvsDryRun() {
	}

	public static void main(String[] args) {
		System.setProperty("dvs.dryRun", "true");
		DvsConfig.reload();
		System.out.println("Checks: data.audit=" + DvsConfig.get("data.audit", "ON") + ", tabswitch.check="
				+ DvsConfig.get("tabswitch.check", "ON") + ", checker.recovery=" + DvsConfig.get("checker.recovery", "ON")
				+ ", keep.bug.value=" + DvsConfig.get("keep.bug.value", "true"));
		System.out.println("Customers: individual=" + DvsConfig.list("dvs.cust.individual") + " minor="
				+ DvsConfig.list("dvs.cust.minor") + " le=" + DvsConfig.list("dvs.cust.le"));
		for (DvsRunEngine.Mode mode : DvsRunEngine.Mode.values()) {
			DvsRunEngine.Outcome o = new DvsRunEngine().run(mode, DvsRunEngine.filterFromConfig());
			System.out.println(mode + ": total=" + o.total() + " planned=" + o.planned() + " notRun=" + o.notRun()
					+ " -> " + o.resultFile());
		}
	}
}
