/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.UtilityFiles;

import Com.Ckyc_4_0.factory.DriverFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Month;
import java.util.*;

public class BaseClass {

	protected static final Logger logger = LoggerFactory.getLogger(BaseClass.class);
	protected static WebDriver driver;
	public static Properties properties = new Properties();
	protected static final String DOWNLOAD_PATH = System.getProperty("user.dir") + File.separator
			+ "getFilesDownloaded";

	static {
		loadConfig();
	}

	// -------------------------- Configuration Loader --------------------------
	public static void loadConfig() {
		try {
			String primaryPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test"
					+ File.separator + "resources" + File.separator + "config.properties";

			String fallbackPath = System.getProperty("user.dir") + File.separator + "Configuration" + File.separator
					+ "config.properties";

			File primaryFile = new File(primaryPath);
			File fallbackFile = new File(fallbackPath);

			// Prefer: load resources config first (if present), then overlay Configuration/config.properties (if present)
			// so that project-level overrides take precedence without requiring deletion of the resources file.
			boolean loadedAny = false;
			if (primaryFile.exists()) {
				try (FileInputStream fip = new FileInputStream(primaryFile)) {
					properties.load(fip);
					loadedAny = true;
					logger.info("Loaded base configuration from: {}", primaryFile.getAbsolutePath());
				}
			}
			if (fallbackFile.exists()) {
				try (FileInputStream fip = new FileInputStream(fallbackFile)) {
					properties.load(fip);
					loadedAny = true;
					logger.info("Loaded override configuration from: {}", fallbackFile.getAbsolutePath());
				}
			}
			if (!loadedAny) {
				throw new IOException("Configuration file not found in expected paths:\n" + "1) " + primaryPath + "\n"
						+ "2) " + fallbackPath);
			}

			// Validate required fields
			checkRequiredProperty("url");
			checkRequiredProperty("browser");
			checkRequiredProperty("username");
			checkRequiredProperty("password");
			checkRequiredProperty("runtimeMode");

		} catch (IOException e) {
			throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
		}
	}

	// -------------------------- Required Property Checker
	// --------------------------
	private static void checkRequiredProperty(String key) {
		String value = properties.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(
					"Required property '" + key + "' is missing or empty in config.properties. Please check the file.");
		}
		if ("password".equalsIgnoreCase(key)) {
			logger.info("Property '{}' is configured (masked)", key);
		} else {
			logger.info("Property '{}' = '{}'", key, value.trim());
		}
	}

	private static WebDriverWait fastWait(int timeoutSeconds) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
		wait.pollingEvery(Duration.ofMillis(250));
		return wait;
	}

	public static WebElement waitUntilVisible(By locator, int timeoutSeconds) {
		return fastWait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	public static WebElement waitUntilClickable(By locator, int timeoutSeconds) {
		return fastWait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(locator));
	}

	public static boolean waitUntilInvisible(By locator, int timeoutSeconds) {
		return fastWait(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
	}

	public static void waitUntilUrlContains(String urlPart, int timeoutSeconds) {
		fastWait(timeoutSeconds).until(d -> {
			String url = d.getCurrentUrl();
			return url != null && url.contains(urlPart);
		});
		logger.info("URL contains '{}': {}", urlPart, driver.getCurrentUrl());
	}

	public static void typeIntoField(By locator, String value, int timeoutSeconds) {
		WebElement element = waitUntilClickable(locator, timeoutSeconds);
		clearInputField(element);
		element.sendKeys(value);
		ensureInputValue(element, value);
		logger.info("Entered value into field (cleared first): {}", maskIfPassword(locator, value));
	}

	/**
	 * MUI/React inputs often ignore Selenium {@code clear()}. Use Ctrl+A+Delete, then JS reset.
	 */
	public static void clearInputField(WebElement element) {
		if (element == null) {
			return;
		}
		try {
			element.click();
		} catch (Exception ignored) {
			// continue with keyboard/JS clear
		}
		try {
			element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
			element.sendKeys(Keys.DELETE);
			element.sendKeys(Keys.BACK_SPACE);
		} catch (Exception e) {
			logger.debug("Keyboard clear failed: {}", e.getMessage());
		}
		try {
			((JavascriptExecutor) driver).executeScript(
					"arguments[0].focus();"
							+ "arguments[0].value='';"
							+ "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
							+ "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
					element);
		} catch (Exception e) {
			logger.debug("JS clear failed: {}", e.getMessage());
		}
		try {
			element.clear();
		} catch (Exception ignored) {
			// already cleared via Ctrl+A / JS
		}
	}

	/** Re-type if MUI still kept old/appended text. */
	private static void ensureInputValue(WebElement element, String expected) {
		if (expected == null) {
			return;
		}
		String actual = element.getAttribute("value");
		if (expected.equals(actual)) {
			return;
		}
		logger.warn("Input value mismatch after type (actual='{}', expected='{}') — resetting", actual, expected);
		clearInputField(element);
		try {
			((JavascriptExecutor) driver).executeScript(
					"arguments[0].focus();"
							+ "arguments[0].value=arguments[1];"
							+ "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
							+ "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
					element, expected);
		} catch (Exception e) {
			element.sendKeys(expected);
		}
		actual = element.getAttribute("value");
		if (!expected.equals(actual)) {
			clearInputField(element);
			element.sendKeys(expected);
		}
	}

	private static String maskIfPassword(By locator, String value) {
		String loc = locator == null ? "" : locator.toString().toLowerCase();
		if (loc.contains("password")) {
			return "********";
		}
		return value;
	}

	public static void clickElement(By locator, int timeoutSeconds) {
		WebElement element = waitUntilClickable(locator, timeoutSeconds);
		clickBtn(element);
	}

	// -------------------------- Property Getter --------------------------
	public static String getProperty(String key, String defaultValue) {
		String value = properties.getProperty(key, defaultValue);
		if (value != null) {
			return value.trim();
		}
		return defaultValue;
	}

	// -------------------------- WebDriver
	// Initialization----------------------------------
	public static void initialization() {
		if (isExistingSessionAlive()) {
			logger.info("Reusing existing WebDriver session — skip second browser launch");
			return;
		}
		if (driver != null) {
			quitBrowser();
		}

		String browserName = getProperty("browser", "chrome").toLowerCase();
		String baseUrl = getProperty("url", "");
		boolean headless = Boolean.parseBoolean(getProperty("headless", "false"));
		boolean maximize = Boolean.parseBoolean(getProperty("maximizeWindow", "true"));
		boolean deleteCookies = Boolean.parseBoolean(getProperty("deleteCookies", "true"));
		int implicitWait = Integer.parseInt(getProperty("implicitWait", "10"));
		int pageLoadTimeout = Integer.parseInt(getProperty("pageLoadTimeout", "30"));

		logger.info("Launching browser: {}", browserName);
		driver = DriverFactory.createDriver(browserName, headless, DOWNLOAD_PATH);

		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));

		if (maximize)
			driver.manage().window().maximize();
		if (deleteCookies)
			driver.manage().deleteAllCookies();

		if (baseUrl.isEmpty()) {
			throw new IllegalArgumentException("Base URL is not specified in config.properties");
		}

		logger.info("Navigating to: {}", baseUrl);
		driver.get(baseUrl);
	}

	public static WebDriver getDriver() {
		return driver;
	}

	/** True when the static driver still has a live browser session. */
	public static boolean isExistingSessionAlive() {
		if (driver == null) {
			return false;
		}
		try {
			driver.getWindowHandles();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// -------------------------- Browser Setup Methods (delegates to DriverFactory) --------------------------
	public static WebDriver setupChrome(boolean headless) {
		return DriverFactory.setupChrome(headless, DOWNLOAD_PATH);
	}

	public static WebDriver setupFirefox(boolean headless) {
		return DriverFactory.setupFirefox(headless, DOWNLOAD_PATH);
	}

	public static WebDriver setupEdge(boolean headless) {
		return DriverFactory.setupEdge(headless, DOWNLOAD_PATH);
	}

	// -------------------------- Selenium Utilities --------------------------
	public static void clickBtn(WebElement element) {
		try {
			element.click();
		} catch (Exception e) {
			logger.warn("Standard click failed, attempting JS click: {}", e.getMessage());
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
		}
	}

	public static void sendValues(WebElement element, String value) {

		if (value == null || value.trim().isEmpty()) {
			logger.warn("Input value is null or empty. Skipping sendKeys for element: {}", element);
			return;
		}

		try {
			clearInputField(element);
			element.sendKeys(value);
			ensureInputValue(element, value);
			logger.info("Entered value: {}", value);
		} catch (Exception e) {
			logger.error("Failed to enter value: {}", value, e);
			throw e;
		}
	}

	public static void clearText(WebElement element) {
		if (element != null) {
			clearInputField(element);
		}
	}

	public static void closeBrowser() {
		if (driver != null)
			driver.close();
	}

	public static void quitBrowser() {
		if (driver != null) {
			driver.quit();
			driver = null;
			logger.info("Browser session closed.");
		}
	}

	public String getText(WebElement element) {
		String value = "";
		try {
			if (elementVisibilityOf(element)) {
				value = element.getText().trim();
				logger.info("Fetched text: " + value);
			}
		} catch (Exception e) {
			logger.error("Failed to fetch text from element", e);
			throw e;
		}
		return value;
	}

	// -------------------------- Wait Utilities --------------------------
	public boolean elementVisibilityOf(WebElement element) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.visibilityOf(element));
			logger.info("Element is visible: {}", element);
			return true;
		} catch (Exception e) {
			logger.error("Element not visible within timeout: {}", e.getMessage());
			return false;
		}
	}

	public boolean isElementPresent(WebElement element) {
		try {
			return element.isDisplayed();
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public WebElement waitForPresenceOfElement(By locator, int timeoutInSeconds) {
		return new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds))
				.until(ExpectedConditions.presenceOfElementLocated(locator));
	}

	public WebElement waitForVisibilityOfElement(By locator, int timeoutInSeconds) {
		return new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds))
				.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	public WebElement waitForElementToBeClickable(By locator, int timeoutInSeconds) {
		return new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds))
				.until(ExpectedConditions.elementToBeClickable(locator));
	}

	// -------------------------- Select Class Utilities --------------------------
	public static void selectByVisibleText(WebElement dropdownElement, String visibleText) {

		if (visibleText == null || visibleText.trim().isEmpty()) {
			logger.warn("Dropdown visible text is null or empty. Selection skipped.");
			return;
		}

		try {
			new Select(dropdownElement).selectByVisibleText(visibleText);
			logger.info("Selected option by visible text: {}", visibleText);
		} catch (Exception e) {
			logger.error("Failed to select dropdown option: {}", visibleText, e);
			throw e;
		}
	}

	public static void selectByValue(WebElement dropdownElement, String value) {
		new Select(dropdownElement).selectByValue(value);
		logger.info("Selected option by value: {}", value);
	}

	public static void selectByIndex(WebElement dropdownElement, int index) {
		new Select(dropdownElement).selectByIndex(index);
		logger.info("Selected option by index: {}", index);
	}

	public static String getSelectedOption(WebElement dropdownElement) {
		String selected = new Select(dropdownElement).getFirstSelectedOption().getText();
		logger.info("Currently selected option: {}", selected);
		return selected;
	}

	public static List<String> getAllDropdownOptions(WebElement dropdownElement) {
		Select select = new Select(dropdownElement);
		List<String> options = new ArrayList<>();
		for (WebElement option : select.getOptions())
			options.add(option.getText().trim());
		logger.info("Dropdown options: {}", options);
		return options;
	}

	// -------------------------- JavaScript Utilities --------------------------
	public static void scrollIntoView(WebElement element) {
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
		logger.info("Scrolled to element: {}", element);
	}

	public static void highlightElement(WebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].style.border='3px solid red'", element);
	}

	public static void scrollBy(int x, int y) {
		((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
	}

	public static void scrollToBottom() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
	}

	// -------------------------- Frame and Window Handling
	// --------------------------
	public static void switchToFrame(WebElement frameElement) {
		driver.switchTo().frame(frameElement);
		logger.info("Switched to frame: {}", frameElement);
	}

	public static void switchToDefaultContent() {
		driver.switchTo().defaultContent();
		logger.info("Switched to default content");
	}

	public static void switchToWindow(String windowHandle) {
		driver.switchTo().window(windowHandle);
		logger.info("Switched to window: {}", windowHandle);
	}

	// -------------------------- Screenshot Utility --------------------------
	public static String captureScreenshot(String fileName) {
		try {
			File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			String path = System.getProperty("user.dir") + File.separator + "reports" + File.separator + fileName
					+ ".png";
			FileHandler.copy(src, new File(path));
			logger.info("Screenshot captured: {}", path);
			return path;
		} catch (IOException e) {
			logger.error("Screenshot capture failed: {}", e.getMessage());
			return null;
		}
	}

	// -------------------------- Actions Utilities --------------------------
	public static void hoverOverElement(WebElement element) {
		new Actions(driver).moveToElement(element).perform();
		logger.info("Hovered over element: {}", element);
	}

	public static void doubleClick(WebElement element) {
		new Actions(driver).doubleClick(element).perform();
		logger.info("Double clicked element: {}", element);
	}

	public static void rightClick(WebElement element) {
		new Actions(driver).contextClick(element).perform();
		logger.info("Right clicked element: {}", element);
	}

	public void writeReferenceNumberToSingleFile(String referenceNumber) {
		String relativePath = "/src/test/resources/Referencenumber.txt";
		String filePath = System.getProperty("user.dir") + relativePath;

		try {
			String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

			String content = "Reference Number: " + referenceNumber.trim() + System.lineSeparator() + "Generated On: "
					+ timestamp;

			File file = new File(filePath);
			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			java.nio.file.Files.write(java.nio.file.Paths.get(filePath),
					content.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

			logger.info("Reference Number updated in file: {}", file.getAbsolutePath());

		} catch (Exception e) {
			logger.error("Failed to update Referencenumber.txt: {}", e.getMessage(), e);
		}
	}

	public void selectDate1(WebElement dateField, String dateString) throws InterruptedException {
		try {
			if (dateString == null || dateString.trim().isEmpty()) {
				throw new RuntimeException("Date string is empty");
			}

			// Expected formats: dd-MM-yyyy or d-M-yyyy etc.
			String[] parts = dateString.split("-");
			if (parts.length != 3) {
				throw new RuntimeException("Invalid date format: " + dateString);
			}

			String day = parts[0].trim();
			int monthNum = Integer.parseInt(parts[1].trim());
			String monthFull = getMonthName(monthNum); // "July"
			String monthShort = monthFull.substring(0, 3); // "Jul"
			String monthNumeric = String.valueOf(monthNum); // "7" or "07" (we'll try numeric too)
			String year = parts[2].trim();

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

			// 1) Open the datepicker
			wait.until(ExpectedConditions.elementToBeClickable(dateField));
			safeClick(dateField);
			logger.info("Clicked DOB field to open datepicker");

			// 2) Try to open year/decade view using multiple strategies (re-locate each
			// attempt)
			boolean yearViewOpened = false;
			String[] yearOpenXPaths = new String[] { "//button[@class='current']",
					"(//button[contains(@class,'current')])[2]", "//button[contains(@class,'ui-datepicker-current')]",
					"//thead//button[contains(@class,'current') or contains(@class,'ui-datepicker-current')]",
					"//div[contains(@class,'datepicker')]//button[contains(@class,'current')]" };

			for (String xpath : yearOpenXPaths) {
				try {
					WebElement el = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
					safeClick(el);
					// sometimes require another click to go deeper
					try {
						Thread.sleep(150);
					} catch (InterruptedException ignored) {
					}
					safeClick(wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath))));
					yearViewOpened = true;
					logger.info("Opened year view using xpath: {}", xpath);
					break;
				} catch (Exception ex) {
					// try next strategy
				}
			}

			// If still not opened: try clicking header cell or title area
			if (!yearViewOpened) {
				List<By> headerLocators = Arrays.asList(
						By.xpath("//div[contains(@class,'datepicker') or contains(@class,'calendar')]//thead"),
						By.xpath(
								"//div[contains(@class,'datepicker') or contains(@class,'calendar')]//button[contains(@class,'title') or contains(@class,'header')]"),
						By.xpath(
								"//div[contains(@class,'datepicker') or contains(@class,'calendar')]//div[contains(@class,'header')]"));
				for (By b : headerLocators) {
					try {
						WebElement header = wait.until(ExpectedConditions.elementToBeClickable(b));
						safeClick(header);
						Thread.sleep(150);
						safeClick(header);
						yearViewOpened = true;
						logger.info("Opened year view by clicking header locator: {}", b);
						break;
					} catch (Exception ignored) {
					}
				}
			}

			// 3) If year view still not opened, continue but attempt to locate year
			// directly (some pickers show years list from start)
			if (!yearViewOpened) {
				logger.info("Year view not explicitly opened; will try to find year directly in the DOM.");
			}

			// 4) Navigate the 16-year grid or the year list until target year is visible &
			// clickable
			boolean yearSelected = false;
			int navAttempts = 0;
			while (!yearSelected && navAttempts < 60) { // safety cap
				navAttempts++;

				// try to find the year element visible now (support several patterns)
				List<By> yearXPaths = Arrays.asList(
						By.xpath("//td[@role='gridcell']//span[normalize-space(text())='" + year + "']"),
						By.xpath("//div[contains(@class,'years')]//button[normalize-space()='" + year + "']"),
						By.xpath("//div[contains(@class,'year') and normalize-space()='" + year + "']"),
						By.xpath("//td//a[normalize-space()='" + year + "']"),
						By.xpath("//span[normalize-space()='" + year + "']"));

				for (By yBy : yearXPaths) {
					try {
						List<WebElement> cand = driver.findElements(yBy);
						if (!cand.isEmpty()) {
							for (WebElement c : cand) {
								try {
									if (c.isDisplayed()) {
										safeClick(c);
										yearSelected = true;
										logger.info("Selected year '{}' using locator: {}", year, yBy);
										break;
									}
								} catch (Exception e) {
									// stale or not clickable — ignore and continue
								}
							}
							if (yearSelected)
								break;
						}
					} catch (Exception ex) {
						// ignore and continue trying other xpaths
					}
				}

				if (yearSelected)
					break;

				// if not found, attempt to detect range element (like "2000 - 2015") and
				// navigate
				try {
					WebElement rangeEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
							"//button[contains(@class,'current')]/span[contains(text(),'-')] | //div[contains(@class,'range') or contains(@class,'years-range')]//span[contains(text(),'-')]")));
					String txt = rangeEl.getText().trim().replace("\u2013", "-").replace("\u2014", "-");
					String[] rangeParts = txt.split("-");
					if (rangeParts.length >= 2) {
						int start = Integer.parseInt(rangeParts[0].trim());
						int end = Integer.parseInt(rangeParts[1].trim());
						int targetYear = Integer.parseInt(year);
						if (targetYear < start) {
							// click previous
							try {
								WebElement prev = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
										"//button[@class='previous' or contains(@class,'prev') or contains(@aria-label,'Previous')]")));
								safeClick(prev);
								logger.info("Clicked previous to go to earlier range");
							} catch (Exception ex) {
								// alternative prev locator
								try {
									safeClick(wait.until(ExpectedConditions
											.elementToBeClickable(By.xpath("//button[contains(@class,'prev')]"))));
								} catch (Exception ignore) {
								}
							}
						} else if (targetYear > end) {
							// click next
							try {
								WebElement next = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
										"//button[@class='next' or contains(@class,'next') or contains(@aria-label,'Next')]")));
								safeClick(next);
								logger.info("Clicked next to go to later range");
							} catch (Exception ex) {
								try {
									safeClick(wait.until(ExpectedConditions
											.elementToBeClickable(By.xpath("//button[contains(@class,'next')]"))));
								} catch (Exception ignore) {
								}
							}
						} else {
							// within range but year element not found — try small scroll/wait
							try {
								Thread.sleep(200);
							} catch (InterruptedException ignored) {
							}
						}
					} else {
						// fallback nav if range not parseable
						try {
							safeClick(wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
									"//button[contains(@class,'next') or contains(@class,'ui-datepicker-next')]"))));
						} catch (Exception ignore) {
						}
					}
				} catch (Exception e) {
					// range element not found — try clicking prev/next heuristically
					try {
						safeClick(wait.until(ExpectedConditions.elementToBeClickable(By
								.xpath("//button[contains(@class,'next') or contains(@class,'ui-datepicker-next')]"))));
					} catch (Exception ignore) {
					}
				}

				// small pause between nav attempts
				try {
					Thread.sleep(120);
				} catch (InterruptedException ignored) {
				}
			} // end year nav loop

			if (!yearSelected) {
				logger.warn(
						"Couldn't select year '{}' after navigation attempts; continuing to try month/day selection",
						year);
			}

			// 5) Select MONTH — attempt several locator patterns (full name, short,
			// numeric)
			boolean monthSelected = false;
			List<String> monthCandidates = Arrays.asList(monthFull, monthShort, monthNumeric);

			for (String mc : monthCandidates) {
				List<By> monthXPaths = Arrays.asList(
						By.xpath("//td[@role='gridcell']//span[normalize-space(text())='" + mc + "']"),
						By.xpath("//div[contains(@class,'months')]//button[normalize-space()='" + mc + "']"),
						By.xpath("//div[contains(@class,'month') and normalize-space()='" + mc + "']"),
						By.xpath("//td//a[normalize-space()='" + mc + "']"));
				for (By mBy : monthXPaths) {
					try {
						WebElement mEl = wait.until(ExpectedConditions.elementToBeClickable(mBy));
						safeClick(mEl);
						monthSelected = true;
						logger.info("Selected month '{}' using locator: {}", mc, mBy);
						break;
					} catch (Exception ex) {
						// try next
					}
				}
				if (monthSelected)
					break;
			}

			if (!monthSelected) {
				logger.warn("Month not selected via primary xpaths; will try to continue to day selection anyway.");
			}

			// 6) Select DAY — try several locators: span, button, anchor, contains leading
			// zeros
			boolean daySelected = false;
			String dayNoLeading = String.valueOf(Integer.parseInt(day)); // e.g., "07" -> "7"
			List<String> dayCandidates = Arrays.asList(day, dayNoLeading);

			List<By> dayPatterns = Arrays.asList(By.xpath("//td[@role='gridcell']//span[normalize-space(text())='%s']"),
					By.xpath("//td[@role='gridcell']//a[normalize-space(text())='%s']"),
					By.xpath("//td[contains(@class,'day')]//span[normalize-space(text())='%s']"),
					By.xpath("//td//button[normalize-space(text())='%s']"),
					By.xpath("//td//div[normalize-space()='%s']"));

			for (String dc : dayCandidates) {
				for (By pat : dayPatterns) {
					String xpath = String.format(pat.toString().replace("By.xpath: ", ""), dc).replace("\\", "");
					// Build By again
					By by = By.xpath(xpath);
					try {
						WebElement dEl = wait.until(ExpectedConditions.elementToBeClickable(by));
						safeClick(dEl);
						daySelected = true;
						logger.info("Selected day '{}' using xpath: {}", dc, xpath);
						break;
					} catch (Exception ex) {
						// try next pattern
					}
				}
				if (daySelected)
					break;
			}

			if (!daySelected) {
				// final attempt: click any visible day with matching text (loose search)
				try {
					List<WebElement> candidates = driver
							.findElements(By.xpath("//td//*[normalize-space(text())='" + day + "']"));
					for (WebElement cand : candidates) {
						try {
							if (cand.isDisplayed()) {
								safeClick(cand);
								daySelected = true;
								logger.info("Selected day '{}' using fallback candidate", day);
								break;
							}
						} catch (Exception ignore) {
						}
					}
				} catch (Exception ignore) {
				}
			}

			if (!daySelected) {
				throw new RuntimeException("Failed to locate/click day '" + day + "' in datepicker");
			}

			logger.info("Successfully selected date: {}", dateString);

		} catch (Exception e) {
			logger.error("Failed to select date '{}': {}", dateString, e.getMessage(), e);
			Assert.fail("Failed to select date - " + e.getMessage());
		}
	}

	/**
	 * Helper: tries regular click, retries, then JS click as fallback. Works with
	 * either WebElement or By (overloaded).
	 */
	private void safeClick(WebElement el) throws StaleElementReferenceException, ElementClickInterceptedException {
		try {
			// try normal click
			el.click();
			return;
		} catch (WebDriverException e) {
			// try re-finding element if it has an id or unique attributes?
			try {
				// JS click fallback
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
				return;
			} catch (Exception jsEx) {
				throw new RuntimeException("safeClick failed on element: " + jsEx.getMessage(), jsEx);
			}
		}
	}

	public void safeClick(By by) {
		WebElement el = (new WebDriverWait(driver, Duration.ofSeconds(5)))
				.until(ExpectedConditions.elementToBeClickable(by));
		safeClick(el);
	}

	private String getMonthName(int monthNumber) {
		if (monthNumber < 1 || monthNumber > 12) {
			throw new IllegalArgumentException("Invalid month number: " + monthNumber);
		}
		String name = Month.of(monthNumber).name(); // e.g., JULY
		return name.substring(0, 1) + name.substring(1).toLowerCase(); // July
	}

	public void selectDate(WebElement dateField, String dateString) {
		try {
			String[] parts = dateString.split("-");
			String day = parts[0];
			int monthNum = Integer.parseInt(parts[1]);
			String year = parts[2];
			String month = getMonthName(monthNum);

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

			// open date picker
			wait.until(ExpectedConditions.elementToBeClickable(dateField)).click();

			// click header twice → jumps to year grid immediately
			WebElement header = wait
					.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class,'current')]")));
			header.click(); // first click → month view
			Thread.sleep(50);
			header.click(); // second click → year grid (decade mode)

			// Jump directly to decade block
			int targetYear = Integer.parseInt(year);

			while (true) {
				List<WebElement> years = driver.findElements(By.xpath("//td[@role='gridcell']//span"));

				// read first and last displayed year
				int first = Integer.parseInt(years.get(0).getText());
				int last = Integer.parseInt(years.get(years.size() - 1).getText());

				if (targetYear < first) {
					driver.findElement(By.xpath("//button[@class='previous']")).click();
				} else if (targetYear > last) {
					driver.findElement(By.xpath("//button[@class='next']")).click();
				} else {
					// target year visible
					driver.findElement(By.xpath("//td[@role='gridcell']//span[text()='" + year + "']")).click();
					break;
				}
			}

			// select month
			wait.until(ExpectedConditions
					.elementToBeClickable(By.xpath("//td[@role='gridcell']//span[text()='" + month + "']"))).click();

			// select day
			wait.until(ExpectedConditions
					.elementToBeClickable(By.xpath("//td[@role='gridcell']//span[text()='" + day + "']"))).click();

		} catch (Exception e) {
			Assert.fail("Fast DOB selection failed: " + e.getMessage());
		}
	}

	public void selectDate_NGXBootstrap(WebElement dobField, String dobString) {
		try {
			// Expected format dd-MM-yyyy
			String[] parts = dobString.split("-");
			if (parts.length != 3)
				throw new RuntimeException("Invalid DOB format");

			String day = parts[0];
			int month = Integer.parseInt(parts[1]); // 1–12
			int year = Integer.parseInt(parts[2]);

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

			// Open date picker
			wait.until(ExpectedConditions.elementToBeClickable(dobField)).click();

			// CLICK MONTH/YEAR HEADER → opens Month/Year selection instantly
			WebElement headerBtn = wait
					.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.current")));
			headerBtn.click(); // shows months of current year
			headerBtn.click(); // shows years grid (faster navigation)

			// 1) SELECT YEAR DIRECTLY (no loops)
			WebElement yearBtn = wait.until(
					ExpectedConditions.elementToBeClickable(By.xpath("//span[normalize-space()='" + year + "']")));
			yearBtn.click();

			// 2) SELECT MONTH DIRECTLY
			String monthName = Month.of(month).name().substring(0, 1)
					+ Month.of(month).name().substring(1).toLowerCase();

			WebElement monthBtn = wait.until(
					ExpectedConditions.elementToBeClickable(By.xpath("//span[normalize-space()='" + monthName + "']")));
			monthBtn.click();

			// 3) SELECT DAY DIRECTLY
			WebElement dayBtn = wait.until(ExpectedConditions
					.elementToBeClickable(By.xpath("//td[@role='gridcell']//span[normalize-space()='" + day + "']")));
			dayBtn.click();

			logger.info("DOB selected fast: " + dobString);

		} catch (Exception e) {
			Assert.fail("FAST DOB selection failed: " + e.getMessage());
		}
	}

	private By loader = By.id("loading_data");

	public void clickButton(WebElement element) {

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

		try {

			// 1️⃣ Wait until element clickable
			wait.until(ExpectedConditions.elementToBeClickable(element));

			// 2️⃣ Click using normal click first
			element.click();

		} catch (ElementClickInterceptedException e) {

			// 3️⃣ Fallback → JS click if intercepted
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].click();", element);
		}

		// 4️⃣ Wait for loader to disappear (if appears)
		wait.until(driver -> {
			try {
				WebElement loading = driver.findElement(loader);
				return loading.getCssValue("display").equals("none");
			} catch (NoSuchElementException ex) {
				return true; // If loader not present, continue
			}
		});

	}

	public void clearAndType(WebElement element, String value) {

		try {

			elementVisibilityOf(element);
			element.clear();
			element.sendKeys(value);

			logger.info("Entered value: " + value);

		} catch (Exception e) {

			logger.error("Failed to enter value in element", e);
			throw e;
		}
	}

	public String getFirstSelectedOption(WebElement dropdown) {

		try {

			Select select = new Select(dropdown);
			return select.getFirstSelectedOption().getText().trim();

		} catch (Exception e) {

			logger.error("Unable to get selected option from dropdown", e);
			throw e;
		}
	}

	public void waitForPageLoad() {
		waitForPageFullyLoaded();
	}

	/** Wait until DOM ready and loader gone — use before screenshots. */
	public static void waitForPageFullyLoaded() {
		if (driver == null) {
			return;
		}
		int timeout = Integer.parseInt(getProperty("explicitWait", "10"));
		try {
			fastWait(Math.min(timeout, 8)).until(webDriver -> "complete".equals(
					((JavascriptExecutor) webDriver).executeScript("return document.readyState")));
		} catch (Exception e) {
			logger.debug("document.readyState wait skipped: {}", e.getMessage());
		}
		waitForLoaderGoneQuick();
	}

	public void waitForLoaderToDisappear() {
		waitForLoaderGoneQuick();
	}

	/** Short loader wait — does not burn full explicit timeout when loader is absent. */
	private static void waitForLoaderGoneQuick() {
		if (driver == null) {
			return;
		}
		try {
			fastWait(2).until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading_data")));
		} catch (Exception e) {
			logger.debug("Loader wait skipped or already hidden");
		}
	}
}
