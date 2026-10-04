/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.validation;

/**
 * One finding from {@link CkycRecordValidator} — mirrors a row of {@code issues.csv} produced by
 * the Python reference validator. {@code severity}: ERROR (CERSAI/iFlow/user hard rule), WARN
 * (pack convention / advisory), PACK (positive pack normally fills this field, informational).
 */
public record ValidationIssue(String severity, String rule, String column, String value, String message) {

	public boolean isError() {
		return "ERROR".equals(severity);
	}

	@Override
	public String toString() {
		return severity + " | " + rule + " | " + column + "='" + value + "' | " + message;
	}
}
