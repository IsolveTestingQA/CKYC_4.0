/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Soft failures — record and continue; assertAll() at scenario end.
 * Also holds interactive-run control flags (skip scenario / skip feature / abort).
 */
public final class SoftAssertManager {

	private static final Logger logger = LoggerFactory.getLogger(SoftAssertManager.class);
	private static final ThreadLocal<List<Throwable>> FAILURES = ThreadLocal.withInitial(ArrayList::new);
	private static final AtomicBoolean SKIP_SCENARIO = new AtomicBoolean(false);
	private static final AtomicBoolean SKIP_FEATURE = new AtomicBoolean(false);
	private static final AtomicBoolean ABORT_RUN = new AtomicBoolean(false);
	private static final AtomicReference<String> SKIP_REASON = new AtomicReference<>("");
	private static final AtomicReference<String> SKIP_FEATURE_NAME = new AtomicReference<>("");

	private SoftAssertManager() {
	}

	public static void recordFailure(String context, Throwable t) {
		Throwable wrapped = new AssertionError(context + " => " + (t != null ? t.getMessage() : "unknown"), t);
		FAILURES.get().add(wrapped);
		logger.error("Recorded soft failure (execution continues): {}", context, t);
	}

	public static void assertAll() {
		List<Throwable> list = FAILURES.get();
		if (list == null || list.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder("One or more steps failed (all steps were still executed):\n");
		for (Throwable t : list) {
			sb.append("- ").append(t.getMessage()).append("\n");
		}
		AssertionError error = new AssertionError(sb.toString().trim());
		list.forEach(error::addSuppressed);
		throw error;
	}

	public static boolean hasFailures() {
		List<Throwable> list = FAILURES.get();
		return list != null && !list.isEmpty();
	}

	public static List<Throwable> getFailures() {
		List<Throwable> list = FAILURES.get();
		return list == null ? List.of() : Collections.unmodifiableList(list);
	}

	public static void clearFailures() {
		FAILURES.get().clear();
	}

	public static void markSkipScenario(String reason) {
		SKIP_SCENARIO.set(true);
		SKIP_REASON.set(reason == null ? "" : reason);
		logger.warn("User chose SKIP SCENARIO: {}", reason);
	}

	public static void markSkipFeature(String featureName, String reason) {
		SKIP_FEATURE.set(true);
		SKIP_FEATURE_NAME.set(featureName == null ? "" : featureName);
		SKIP_REASON.set(reason == null ? "" : reason);
		logger.warn("User chose SKIP FEATURE [{}]: {}", featureName, reason);
	}

	public static void markAbortRun(String reason) {
		ABORT_RUN.set(true);
		SKIP_REASON.set(reason == null ? "" : reason);
		logger.warn("User chose STOP / ABORT RUN: {}", reason);
	}

	public static boolean shouldSkipScenario() {
		return SKIP_SCENARIO.get();
	}

	public static boolean shouldSkipFeature(String currentFeatureName) {
		if (!SKIP_FEATURE.get()) {
			return false;
		}
		String locked = SKIP_FEATURE_NAME.get();
		return locked != null && !locked.isBlank() && locked.equalsIgnoreCase(currentFeatureName);
	}

	public static boolean shouldAbortRun() {
		return ABORT_RUN.get();
	}

	public static String skipReason() {
		return SKIP_REASON.get();
	}

	/** Alias used by Hooks when aborting the run. */
	public static String abortReason() {
		return skipReason();
	}

	/** Alias used by Hooks when skipping the rest of a feature. */
	public static String skipFeatureReason() {
		return skipReason();
	}

	/** Alias used by Hooks when skipping the current scenario. */
	public static String skipScenarioReason() {
		return skipReason();
	}

	public static void clearScenarioSkip() {
		SKIP_SCENARIO.set(false);
	}

	public static void clearFeatureSkip() {
		SKIP_FEATURE.set(false);
		SKIP_FEATURE_NAME.set("");
	}

	public static void clearAllFlags() {
		SKIP_SCENARIO.set(false);
		SKIP_FEATURE.set(false);
		ABORT_RUN.set(false);
		SKIP_REASON.set("");
		SKIP_FEATURE_NAME.set("");
	}

	/** Clears soft failures + scenario skip. Keeps feature-skip / abort for following scenarios. */
	public static void clear() {
		clearFailures();
		clearScenarioSkip();
	}
}
