/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.stepdefinitions.bulkimport;

import Com.Ckyc_4_0.functionality.bulkimport.BulkImportContext;
import Com.Ckyc_4_0.functionality.bulkimport.BulkImportReport;
import Com.Ckyc_4_0.functionality.bulkimport.BulkUploadFunctionality;
import Com.Ckyc_4_0.functionality.bulkimport.CustomerInfoCompareFunctionality;
import Com.Ckyc_4_0.functionality.bulkimport.InwardPollFunctionality;
import Com.Ckyc_4_0.functionality.bulkimport.MonitorAutomationFunctionality;
import Com.Ckyc_4_0.stepdefinitions.common.Hooks;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class BulkImportStepDefinitions {

	private final BulkUploadFunctionality upload = new BulkUploadFunctionality();
	private final MonitorAutomationFunctionality monitor = new MonitorAutomationFunctionality();
	private final InwardPollFunctionality inward = new InwardPollFunctionality();
	private final CustomerInfoCompareFunctionality customerInfo = new CustomerInfoCompareFunctionality();

	@When("user opens Bulk Data Import and uploads the CSV chosen in the upload pop-up")
	public void userUploadsTheCsv() {
		Hooks.runStep("Open Bulk Data Import and upload the CSV from the pop-up", "The file is accepted (Upload accepted, Job number)",
				upload::uploadWithPopups, false, true);
	}

	@And("the uploaded job is read and the successful customer ids are kept")
	public void theJobIsRead() {
		Hooks.runStep("Read the uploaded job: SUCCESS ids kept, FAILED rows raised as bugs", "SUCCESS customer refs are listed",
				upload::readJobAndKeepSuccessIds, true, true);
	}

	@And("user checks Monitor Automation and restarts the IFLOW PROCESS")
	public void monitorAutomation() {
		Hooks.runStep("Monitor Automation: restart IFLOW PROCESS and wait for it to respond", "Not Responding goes within about a minute",
				monitor::restartIflowProcess, true, true);
	}

	@And("user waits on the Dashboard until every uploaded customer id reaches Success, Failed or Duplicate")
	public void dashboardWait() {
		Hooks.runStep("Dashboard Data pushed from FI: refresh every 30 s for up to 7 minutes", "Every uploaded id is in a final list",
				inward::waitUntilSettled, true, true);
	}

	@Then("Customer Information matches the CSV for the successful customer ids")
	public void customerInformation() {
		Hooks.runStep("Customer Information: compare random Success customers with the CSV", "Mandatory fields are reflected and equal",
				customerInfo::compareRandomSuccessIds, true, true);
	}

	@And("the Bulk Import result workbook is published")
	public void workbook() {
		Hooks.runStep("Publish the Bulk Import result workbook", "Workbook written", BulkImportReport::publish, false, false);
	}

	/** The workbook is written even when the run stopped half way. */
	@After("@BulkImport")
	public void afterBulkImport() {
		BulkImportReport.publishIfNotYet();
		BulkImportContext.reset();
	}
}
