# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Masters @DistrictMaster @Positive
Feature: Masters - District Master
  As a CKYC admin
  I want District Master list, search, field validations and safe CRUD
  So that districts stay tied to a test-safe State without changing existing districts

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: District Master badges, map, field rules, safe CRUD and cleanup
    When user opens District Master from Masters menu
    Then District Master page should be displayed
    And user verifies District Master badges Total equals Active plus Inactive
    And user ensures a test-safe State exists for District create
    And user loads districts into memory map
    And user verifies an existing District cannot be created again
    And user validates District Name field rules with test data
    And user verifies District State filter for two random States
    And user randomly searches two districts from the map
    And user creates a unique TEST district record
    And user edits only the newly created district
    And user toggles only the newly created district
    And user searches the newly created district
