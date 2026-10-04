# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Dashboard @Positive
Feature: CKYC Dashboard
  As a CKYC admin user
  I want to verify dashboard counts for filters available on the client UI
  So that summary cards and sub-items are stored dynamically without hardcoded filter names

  Background:
    Given user is logged in and on CKYC dashboard
    And dashboard data collection is initialized

  @Smoke
  Scenario: Verify and store dashboard UI data for all available filters
    When user discovers available dashboard filters from UI
    Then user collects and stores dashboard data for all available filter combinations
    And dashboard counts are stored for all discovered filter combinations
    And user exports dashboard data report to Excel
