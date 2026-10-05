/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.dvs;

import Com.Ckyc_4_0.UtilityFiles.BaseClass;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Screenshot/&lt;TC_ID&gt;/&lt;time&gt;_&lt;label&gt;.png (root from CONFIG screenshot.root). */
public final class DvsScreenshot {

	private DvsScreenshot() {
	}

	/** @return saved path, or "" when no browser / capture failed (never throws). */
	public static String capture(String tcId, String label) {
		try {
			WebDriver driver = BaseClass.getDriver();
			if (!(driver instanceof TakesScreenshot shot)) {
				return "";
			}
			String root = DvsConfig.get("screenshot.root", "Screenshot/");
			String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss_SSS"));
			Path file = Paths.get(root, tcId, time + "_" + label.replaceAll("[^A-Za-z0-9_-]", "_") + ".png");
			Files.createDirectories(file.getParent());
			Files.write(file, shot.getScreenshotAs(OutputType.BYTES));
			return file.toString().replace('\\', '/');
		} catch (IOException | RuntimeException e) {
			return "";
		}
	}
}
