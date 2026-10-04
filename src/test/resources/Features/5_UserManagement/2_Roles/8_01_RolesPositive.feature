# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@UserManagement @Roles @RolesPositive @Positive
Feature: User Management - Roles Maker Checker (Positive)
  Accounts come from TestData/user-management.properties so they can be updated without rewriting steps.
  Maker = config username (auto). Independent Checker = shyam. Super Admin is never opened or edited.
  Runs before Users Positive so the approved TEST AUTO role can be selected on Add User.

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: Roles positive maker-checker approval and TEST AUTO permissions
    When user opens User Management Roles
    Then Roles Maker controls should be displayed
    And user submits a unique TEST AUTO role request
    And independent Checker shyam approves the TEST AUTO role request
    And permissions are evaluated only for a TEST AUTO role
    And maker session is restored on Roles
