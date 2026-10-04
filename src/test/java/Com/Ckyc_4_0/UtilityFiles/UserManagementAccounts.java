/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads {@code TestData/user-management.properties} so checker accounts can change
 * without rewriting scenarios. Maker remains {@link ConfigReader#getUsername()}.
 */
public final class UserManagementAccounts {
	private static final Properties PROPS = new Properties();

	static {
		File file = new File(System.getProperty("user.dir")
				+ File.separator + "src" + File.separator + "test" + File.separator + "resources"
				+ File.separator + "TestData" + File.separator + "user-management.properties");
		if (file.exists()) {
			try (FileInputStream in = new FileInputStream(file)) {
				PROPS.load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot load " + file.getAbsolutePath(), e);
			}
		}
	}

	private UserManagementAccounts() {}

	public static String maker() {
		return ConfigReader.getUsername();
	}

	public static String makerPassword() {
		return ConfigReader.getPassword();
	}

	/** Independent Checker for create / edit / role approval. */
	public static String primaryChecker() {
		return value("checker.primary", "shyam");
	}

	/** Branch Checker for lock / unlock / dormant only. */
	public static String branchChecker() {
		return value("checker.branch", "Nivijay");
	}

	/** Same-role negative Checker (Admin must not approve an Admin-role Maker request). */
	public static String sameRoleChecker() {
		return value("checker.sameRole", "admin");
	}

	public static String defaultPassword() {
		return value("password.default", "Welcome@123");
	}

	public static String adminPassword() {
		return value("password.adminFallback", "KMPL@123");
	}

	public static String passwordFor(String username) {
		if (username != null && username.equalsIgnoreCase(sameRoleChecker())) {
			return adminPassword();
		}
		if (username != null && username.equalsIgnoreCase("admin")) {
			return adminPassword();
		}
		return defaultPassword();
	}

	private static String value(String key, String fallback) {
		String raw = PROPS.getProperty(key, fallback);
		return raw == null || raw.isBlank() ? fallback : raw.trim();
	}
}
