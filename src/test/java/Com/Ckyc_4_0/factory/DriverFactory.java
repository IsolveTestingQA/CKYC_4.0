/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.factory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LoggingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Browser / driver factory — initializes Chrome, Firefox, or Edge with automation-safe options.
 * Suppresses Google Password Manager save / breach-check bubbles via prefs and feature flags.
 */
public final class DriverFactory {

	private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);

	private DriverFactory() {
	}

	private static void cleanOldChromeProfiles() {
		try {
			File targetDir = new File(System.getProperty("user.dir"), "target");
			if (!targetDir.exists()) return;
			File[] oldProfiles = targetDir.listFiles(
					f -> f.isDirectory() && f.getName().startsWith("chrome-automation-profile-"));
			if (oldProfiles != null) {
				for (File dir : oldProfiles) {
					deleteRecursive(dir);
				}
				if (oldProfiles.length > 0) {
					logger.info("Cleaned {} old Chrome automation profiles", oldProfiles.length);
				}
			}
		} catch (Exception e) {
			logger.debug("Old Chrome profile cleanup skipped: {}", e.getMessage());
		}
	}

	private static void deleteRecursive(File file) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) deleteRecursive(child);
			}
		}
		file.delete();
	}

	public static WebDriver createDriver(String browserName, boolean headless, String downloadPath) {
		String browser = browserName == null ? "chrome" : browserName.toLowerCase().trim();
		return switch (browser) {
		case "chrome" -> setupChrome(headless, downloadPath);
		case "firefox" -> setupFirefox(headless, downloadPath);
		case "edge" -> setupEdge(headless, downloadPath);
		default -> throw new IllegalArgumentException("Unsupported browser: " + browserName);
		};
	}

	public static WebDriver setupChrome(boolean headless, String downloadPath) {
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--disable-infobars", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
		options.addArguments("--disable-notifications");
		options.addArguments("--disable-save-password-bubble");
		options.addArguments("--disable-password-manager-reauthentication");
		options.addArguments(
				"--disable-features=PasswordManagerOnboarding,PasswordLeakDetection,PasswordCheck,"
						+ "PasswordManagerUi,AutofillServerCommunication,SafeBrowsingEnhancedProtection");
		options.addArguments("--password-store=basic");
		if (headless) {
			options.addArguments("--headless=new");
		}

		// Fresh profile each run; delete old profiles first to save disk space
		cleanOldChromeProfiles();
		String profileDir = System.getProperty("user.dir") + File.separator + "target" + File.separator
				+ "chrome-automation-profile-" + System.currentTimeMillis();
		new File(profileDir).mkdirs();
		options.addArguments("--user-data-dir=" + profileDir);

		Map<String, Object> prefs = buildChromiumPasswordPrefs(downloadPath);
		options.setExperimentalOption("prefs", prefs);
		options.setExperimentalOption("excludeSwitches", List.of("enable-automation", "enable-logging"));

		LoggingPreferences logPrefs = new LoggingPreferences();
		options.setCapability("goog:loggingPrefs", logPrefs);
		logger.info("Chrome launched with Password Manager / leak-detection disabled (profile={})", profileDir);
		return new ChromeDriver(options);
	}

	public static WebDriver setupFirefox(boolean headless, String downloadPath) {
		WebDriverManager.firefoxdriver().setup();
		FirefoxOptions options = new FirefoxOptions();
		if (headless) {
			options.addArguments("-headless");
		}
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", downloadPath);
		profile.setPreference("signon.rememberSignons", false);
		profile.setPreference("signon.autofillForms", false);
		options.setProfile(profile);
		return new FirefoxDriver(options);
	}

	public static WebDriver setupEdge(boolean headless, String downloadPath) {
		WebDriverManager.edgedriver().setup();
		EdgeOptions options = new EdgeOptions();
		options.addArguments("--disable-gpu", "--no-sandbox", "--disable-save-password-bubble");
		options.addArguments(
				"--disable-features=PasswordManagerOnboarding,PasswordLeakDetection,PasswordCheck");
		if (headless) {
			options.addArguments("--headless=new");
		}
		Map<String, Object> prefs = buildChromiumPasswordPrefs(downloadPath);
		options.setExperimentalOption("prefs", prefs);
		return new EdgeDriver(options);
	}

	private static Map<String, Object> buildChromiumPasswordPrefs(String downloadPath) {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("profile.default_content_settings.popups", 0);
		prefs.put("download.default_directory", downloadPath);
		prefs.put("credentials_enable_service", false);
		prefs.put("profile.password_manager_enabled", false);
		prefs.put("profile.password_manager_leak_detection", false);
		prefs.put("profile.password_manager_auto_signin", false);
		prefs.put("signin.allowed_on_next_startup", false);
		prefs.put("safebrowsing.enabled", false);
		prefs.put("safebrowsing.disable_download_protection", true);
		prefs.put("profile.default_content_setting_values.notifications", 2);
		prefs.put("autofill.profile_enabled", false);
		prefs.put("autofill.credit_card_enabled", false);
		return prefs;
	}
}
