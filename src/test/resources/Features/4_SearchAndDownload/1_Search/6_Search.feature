# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@SearchAndDownload @Search @Positive
Feature: Search And Download - Search
  As a CKYC operations user
  I want to search an existing CKYC record by PAN or mobile number
  So that valid searches return a meaningful response and invalid input is blocked

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: Validate PAN and mobile document search behavior
    When user opens Search from Search And Download menu
    Then Search page should be displayed
    And user verifies Search requires a document type before input
    And user validates PAN Search input rules
    And user searches the configured PAN, validates all CKYC fields, and saves download identifiers
    And user validates mobile Search input rules
    And user searches the configured mobile and records the outcome
    And user clears Search and verifies the reset state
