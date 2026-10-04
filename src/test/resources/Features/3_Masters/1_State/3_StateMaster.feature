# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Masters @StateMaster @Positive
Feature: Masters - State Master
  As a CKYC admin
  I want State Master list, search, field validations and safe CRUD
  So that banking KYC master data stays CERSAI-compliant without changing existing states

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: State Master badges, map, field rules, safe CRUD and random search
    When user opens State Master from Masters menu
    Then State Master page should be displayed
    And user verifies State Master badges Total equals Active plus Inactive
    And user loads all states into memory map
    And user clears Create State form and verifies fields empty
    And user validates State Code and State Name field rules with test data
    And user verifies an existing State cannot be created again
    And user creates a unique TEST state record
    And user edits only the newly created state
    And user toggles only the newly created state
    And user searches the newly created state
    And user randomly searches a state from the map with retry
