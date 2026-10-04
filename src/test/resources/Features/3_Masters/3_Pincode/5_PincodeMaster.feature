# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Masters @PincodeMaster @Positive
Feature: Masters - Pincode Master
  As a CKYC admin
  I want Pincode Master list, search, field validations and safe CRUD
  So that pincodes stay tied to test-safe State and District without changing existing pincodes

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: Pincode Master badges, map, field rules, safe CRUD and cleanup
    When user opens Pincode Master from Masters menu
    Then Pincode Master page should be displayed
    And user verifies Pincode Master badges Total is at least Active
    And user ensures a test-safe State and District exist for Pincode create
    And user loads visible pincodes into memory map
    And user verifies an existing Pincode cannot be created again
    And user validates Pincode field rules with test data
    And user verifies Pincode State filter for two random States
    And user randomly searches two pincodes from the map
    And user creates a unique TEST pincode record
    And user edits only the newly created pincode
    And user toggles only the newly created pincode
    And user searches the newly created pincode
