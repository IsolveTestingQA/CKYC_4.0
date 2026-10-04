/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.TestRunner;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.UtilityFiles.ExecutionLogger;
import Com.Ckyc_4_0.utils.RetryContext;
import io.cucumber.testng.PickleWrapper;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.List;
import java.util.Set;

/**
 * Retries a failed Cucumber scenario once (count configurable), but ONLY for read-safe /
 * single-actor modules: Login, Dashboard, Search, Masters (State/District/Pincode/Hierarchy).
 *
 * User Management ({@code @UserManagement}) and Concurrent Session ({@code @ConcurrentSession})
 * scenarios are never auto-retried: a Maker-Checker chain that fails partway through, or a
 * second live browser session, is not safe to blindly redo from the top — see project rules
 * in {@code .cursor/rules/user-management-test-plan.mdc}.
 *
 * Eligibility is decided by scenario TAG, not by which runner class executed it, so it works
 * the same whether a scenario runs via its own module runner or as part of the combined
 * {@code TestCKYCAllRunner}.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

	private static final Set<String> NEVER_RETRY_TAGS = Set.of("@UserManagement", "@ConcurrentSession");

	private int attempts = 0;

	@Override
	public boolean retry(ITestResult result) {
		if (!ConfigReader.retryFailedScenarios()) {
			return false;
		}
		int maxRetries = ConfigReader.maxScenarioRetries();
		if (attempts >= maxRetries) {
			return false;
		}
		List<String> tags = tagsOf(result);
		if (tags == null) {
			// Could not identify the scenario's tags from ITestResult — safest default is no retry.
			return false;
		}
		for (String tag : tags) {
			if (NEVER_RETRY_TAGS.contains(tag)) {
				return false;
			}
		}

		attempts++;
		int attemptNumber = attempts + 1;
		ExecutionLogger.warn("Retrying scenario (attempt " + attemptNumber + "/" + (maxRetries + 1) + "): "
				+ nameOf(result));
		RetryContext.markNextAttempt(attemptNumber);
		return true;
	}

	private List<String> tagsOf(ITestResult result) {
		for (Object param : result.getParameters()) {
			if (param instanceof PickleWrapper pickleWrapper) {
				return pickleWrapper.getPickle().getTags();
			}
		}
		return null;
	}

	private String nameOf(ITestResult result) {
		for (Object param : result.getParameters()) {
			if (param instanceof PickleWrapper pickleWrapper) {
				return pickleWrapper.getPickle().getName();
			}
		}
		return result.getName();
	}
}
