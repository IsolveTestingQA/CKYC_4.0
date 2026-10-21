# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@BulkImport
Feature: Bulk Data Import to Customer Information
  As a CKYC tester
  I want to upload a CSV, follow the iFlow processing and check the data on Customer Information
  So that every successful customer id is inward and its details equal the CSV

  Background:
    Given user is logged in and on CKYC dashboard

  Scenario: Upload CSV, monitor iFlow, wait on the Dashboard and compare Customer Information
    When user opens Bulk Data Import and uploads the CSV chosen in the upload pop-up
    And the uploaded job is read and the successful customer ids are kept
    And user checks Monitor Automation and restarts the IFLOW PROCESS
    And user waits on the Dashboard until every uploaded customer id reaches Success, Failed or Duplicate
    Then Customer Information matches the CSV for the successful customer ids
    And the Bulk Import result workbook is published
