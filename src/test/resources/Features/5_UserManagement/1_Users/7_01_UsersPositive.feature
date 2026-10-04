# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@UserManagement @Users @UsersPositive @Positive
Feature: User Management - Users Maker Checker (Positive)
  Accounts come from TestData/user-management.properties so they can be updated without rewriting steps.
  Maker = config username (auto). Independent Checker = shyam. Branch Checker = Nivijay.
  Password default = Welcome@123. Only TEST AUTO records are created or changed.
  Run Roles Positive first: Add User assigns the approved TEST AUTO role created in that feature.

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: Users positive maker-checker flow with approved and reverted detail verification
    When user opens User Management Users
    Then Users Maker controls should be displayed
    And user submits a unique TEST AUTO user request
    And independent Checker shyam approves the TEST AUTO user request
    And checker verifies the last processed TEST AUTO user request details
    And user logs in as the approved TEST AUTO user to verify access
    And user searches the TEST AUTO user
    And user edits only the TEST AUTO user
    And independent Checker shyam approves the TEST AUTO edit request
    And checker verifies the last processed TEST AUTO user request details
    And user submits a TEST AUTO edit request for revert validation
    And independent Checker shyam reverts the TEST AUTO edit request
    And checker verifies the last processed TEST AUTO user request details
    And user locks only the TEST AUTO user
    And branch Checker Nivijay approves the TEST AUTO lock request
    And checker verifies the last processed TEST AUTO user request details
    And user unlocks only the TEST AUTO user
    And branch Checker Nivijay approves the TEST AUTO unlock request
    And checker verifies the last processed TEST AUTO user request details
    And user sets Dormant on only the TEST AUTO user
    And branch Checker Nivijay approves the TEST AUTO dormant request
    And checker verifies the last processed TEST AUTO user request details
    And user verifies TEST AUTO user exit flag and login status after dormant
    And maker session is restored on Users
