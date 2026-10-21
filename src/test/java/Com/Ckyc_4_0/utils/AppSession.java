/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils;

import java.net.URI;

import org.openqa.selenium.WebDriver;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.pages.login.LoginPage;

/**
 * Answers "is the browser already signed in to the application?" for every feature's Background, so a feature that
 * starts after another one (Test Control Panel runs) resumes instead of hunting for a login page that is not there.
 */
public final class AppSession {

	private AppSession() {
	}

	public static boolean isInsideApp(WebDriver driver) {
		if (driver == null) {
			return false;
		}
		try {
			String url = driver.getCurrentUrl();
			if (url == null || url.isBlank()) {
				return false;
			}
			String lower = url.toLowerCase();
			if (lower.contains("/login")) {
				return false;
			}
			if (url.contains(ConfigReader.getDashboardUrlPath()) || url.contains("/ckyc/") || url.contains("/admin/")) {
				return true;
			}
			// Any other page of the application (for example /search-download/search, /reports): same host, a real
			// path, and no login form on screen.
			URI now = URI.create(url);
			URI app = URI.create(ConfigReader.getUrl());
			boolean sameHost = now.getHost() != null && now.getHost().equalsIgnoreCase(app.getHost());
			String path = now.getPath() == null ? "" : now.getPath();
			boolean realPage = !path.isEmpty() && !"/".equals(path);
			return sameHost && realPage && driver.findElements(LoginPage.USERNAME_INPUT).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}
}
