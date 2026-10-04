# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@UserManagement @Roles @RolesNegative @Negative
Feature: User Management - Roles Maker Checker (Negative)
  Accounts come from TestData/user-management.properties so they can be updated without rewriting steps.
  Maker = config username (auto). Same-role Checker = admin (negative). Super Admin is never touched.

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: Roles negative validations and maker-checker rejection rules
    When user opens User Management Roles
    Then Roles Maker controls should be displayed
    And user validates mandatory Add Role fields
    And user submits a unique TEST AUTO role request
    And maker attempts to approve the same TEST AUTO role request
    And same-role admin Checker is blocked from approving the TEST AUTO role request
    And maker session is restored on Roles
