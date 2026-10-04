/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

/**
 * Bridges TestNG's retry decision — made by {@code RetryAnalyzer} right after a scenario
 * fails, outside Cucumber's own lifecycle — to the next {@code Hooks.beforeScenario()} call,
 * so Excel/Extent can tag the retried run "[Attempt 2]" and it is visible next to "Attempt 1".
 *
 * Single-threaded, sequential execution only (testng.xml {@code preserve-order="true"}) — same
 * assumption already relied on by {@link Com.Ckyc_4_0.UtilityFiles.MasterTestContext} and
 * {@link Com.Ckyc_4_0.UtilityFiles.SoftAssertManager}.
 */
public final class RetryContext {

	private static int nextAttempt = 1;

	private RetryContext() {
	}

	/** Called by RetryAnalyzer right before TestNG re-invokes the scenario. */
	public static synchronized void markNextAttempt(int attemptNumber) {
		nextAttempt = attemptNumber;
	}

	/** Consumed once per scenario start; resets to 1 so the following scenario is unaffected. */
	public static synchronized int consumeAttempt() {
		int attempt = nextAttempt;
		nextAttempt = 1;
		return attempt;
	}
}
