/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.pages.reports;

import org.openqa.selenium.By;

/**
 * Reports module locators (/reports). Verified on the live page 2026-10-11: two MUI autocompletes (Report Category,
 * Report Type), NEW / UPDATE and ALL / Individual / Legal entity toggles, a Run button whose aria-label is
 * "Run &lt;report type&gt;", a results table with a "N records" chip and a rows-per-page / next-page pager.
 * "Others" reports are different layouts (e.g. a role matrix) and may have no filters or Run button.
 */
public final class ReportsPage {

	private ReportsPage() {
	}

	public static final By MAIN = By.xpath("//main");

	/** The n-th (1 = category, 2 = report type) autocomplete input. */
	public static By combo(int n) {
		return By.xpath("(//main//input[@role='combobox'])[" + n + "]");
	}

	public static By comboOpenButton(int n) {
		return By.xpath("((//main//div[contains(@class,'MuiAutocomplete-root')])[" + n
				+ "]//button[@title='Open' or @aria-label='Open'])[1]");
	}

	public static final By OPTIONS = By.xpath("//ul[@role='listbox']//li[@role='option']");
	public static final By LISTBOX = By.xpath("//ul[@role='listbox']");

	public static By option(String text) {
		return By.xpath("//ul[@role='listbox']//li[@role='option'][normalize-space()=" + quote(text) + "]");
	}

	/** NEW / UPDATE and ALL / Individual / Legal entity toggle buttons (matched by visible text, any case). */
	public static By toggle(String text) {
		return By.xpath("//main//button[translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')="
				+ quote(text.toUpperCase()) + "]");
	}

	public static final By RUN_BUTTON = By.xpath("//main//button[starts-with(@aria-label,'Run ')]");
	public static final By FROM_DATE = By.xpath("//main//label[contains(.,'From Date')]/following::input[1]");
	public static final By TO_DATE = By.xpath("//main//label[contains(.,'To Date')]/following::input[1]");
	public static final By DATE_INPUTS = By.xpath("//main//input[contains(@class,'MuiPickersInputBase') or @placeholder='DD-MMM-YYYY' "
			+ "or contains(@aria-label,'Date')]");
	public static final By TABLE = By.xpath("//main//table");
	public static final By ROWS_PER_PAGE = By.xpath(
			"//main//*[normalize-space()='Rows per page:']/following::div[@role='combobox'][1]");
	public static final By NEXT_PAGE = By.xpath("//main//button[@aria-label='Go to next page']");
	public static final By REFRESH = By.xpath("//main//button[@aria-label='Refresh report data']");

	private static String quote(String s) {
		return s.contains("'") ? "concat('" + s.replace("'", "',\"'\",'") + "')" : "'" + s + "'";
	}
}
