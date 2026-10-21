/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.functionality.bulkimport;

import java.util.List;
import java.util.Locale;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Com.Ckyc_4_0.UtilityFiles.ConfigReader;
import Com.Ckyc_4_0.pages.SideNavPage;
import Com.Ckyc_4_0.pages.bulkimport.BulkImportPages;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import Com.Ckyc_4_0.utils.StepLog;
import Com.Ckyc_4_0.utils.ToastHandler;

/**
 * Monitor Automation: whatever the status of the IFLOW PROCESS is (scheduled, in progress, idle ...) it is restarted
 * (or started), and the page is refreshed every 10 seconds, at most 6 times, until "Not Responding" goes. Then the flow
 * continues on the Dashboard. A process that stays "Not Responding" is a finding (the server team fixes it), not a reason
 * to stop.
 */
public final class MonitorAutomationFunctionality {

	private static final Logger logger = LoggerFactory.getLogger(MonitorAutomationFunctionality.class);
	private static final String SC = "Monitor Automation | ";

	private WebDriver d() {
		return BiSupport.driver();
	}

	private String process() {
		return ConfigReader.get("bulkimport.process", "IFLOW PROCESS");
	}

	public void openMonitorAutomation() {
		BiSupport.openModule(BulkImportPages_monitorItem(), SideNavPage.SETTINGS, "/monitor-automation", "/settings/monitor-automation");
		boolean ok = BiSupport.waitFor(BulkImportPages.processCard(process()), 20);
		StepLog.check(SC + "Open", "Open Settings > Monitor Automation from the navigation bar", "Process list with " + process(),
				"url=" + BiSupport.safeUrl() + "; processVisible=" + ok, ok, "process=" + process());
		if (!ok) {
			throw new AssertionError("Monitor Automation did not show the process " + process());
		}
		Hooks.captureMilestone("Monitor Automation opened");
	}

	private static org.openqa.selenium.By BulkImportPages_monitorItem() {
		return org.openqa.selenium.By.xpath("//*[@role='button'][normalize-space()='Monitor Automation']");
	}

	private String statusText() {
		List<WebElement> card = d().findElements(BulkImportPages.processCard(process()));
		return card.isEmpty() ? "" : card.get(0).getText().replaceAll("\\s+", " ").trim();
	}

	private static boolean notResponding(String s) {
		return s.toLowerCase(Locale.ROOT).contains("not responding");
	}

	/** Restart (or Start) the process, then refresh every 10 s (max 6 times) until it is no longer Not Responding. */
	public void restartIflowProcess() {
		openMonitorAutomation();
		String before = statusText();
		BulkImportContext.iflowStatusBefore = before;
		List<WebElement> card = d().findElements(BulkImportPages.processCard(process()));
		WebElement action = null;
		if (!card.isEmpty()) {
			List<WebElement> restart = card.get(0).findElements(org.openqa.selenium.By.xpath(".//button[normalize-space()='" + BulkImportPages.RESTART + "']"));
			List<WebElement> start = card.get(0).findElements(org.openqa.selenium.By.xpath(".//button[normalize-space()='" + BulkImportPages.START + "']"));
			action = !restart.isEmpty() ? restart.get(0) : start.isEmpty() ? null : start.get(0);
		}
		if (action == null) {
			StepLog.check(SC + "Restart", "Restart or start " + process(), "A Restart / Start button on the process card",
					"FAIL | no button; status='" + before + "'", false, "process=" + process());
			throw new AssertionError("No Restart/Start button for " + process() + " (status: " + before + ")");
		}
		String label = action.getText().trim();
		Hooks.captureMilestone("Monitor Automation before " + label + " | " + before);
		BiSupport.clickJs(action);
		String toast = "";
		long end = System.currentTimeMillis() + 8000;
		while (System.currentTimeMillis() < end && toast.isBlank()) {
			toast = ToastHandler.readIfPresent(d());
			BiSupport.sleep(300);
		}
		StepLog.check(SC + label, label + " " + process() + " whatever its status is", "Toast confirms the " + label.toLowerCase() + " was sent",
				"status before='" + before + "'; toast='" + toast + "'", !toast.isBlank(), "process=" + process());
		String status = statusText();
		int refreshes = 0;
		int max = Integer.parseInt(ConfigReader.get("bulkimport.iflowRefreshes", "6"));
		int gapSeconds = Integer.parseInt(ConfigReader.get("bulkimport.iflowRefreshSeconds", "10"));
		while (refreshes < max) {
			BiSupport.sleep(gapSeconds * 1000L);
			d().navigate().refresh();
			BiSupport.waitFor(BulkImportPages.processCard(process()), 20);
			BiSupport.sleep(1200);
			refreshes++;
			status = statusText();
			logger.info("{} refresh {}/{} | {}", process(), refreshes, max, status);
			if (!notResponding(status)) {
				break;
			}
		}
		BulkImportContext.iflowStatusAfter = status;
		Hooks.captureMilestone("Monitor Automation after " + refreshes + " refresh(es) | " + status, !notResponding(status));
		if (notResponding(status)) {
			BulkImportContext.bug("Monitor Automation", process(), process() + " is still Not Responding after " + max + " refreshes",
					"status='" + status + "' - server side; the flow continues on the Dashboard");
			StepLog.check(SC + "Status", process() + " leaves Not Responding after the restart", "Status is no longer Not Responding",
					"FAIL | still Not Responding after " + refreshes + " refreshes (" + (refreshes * gapSeconds) + " s); status='" + status + "'", false,
					"process=" + process());
		} else {
			StepLog.check(SC + "Status", process() + " leaves Not Responding after the restart", "Status is no longer Not Responding",
					"OK after " + refreshes + " refresh(es); status='" + status + "'", true, "process=" + process());
		}
	}
}
