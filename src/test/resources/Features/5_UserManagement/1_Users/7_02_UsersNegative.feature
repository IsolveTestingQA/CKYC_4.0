# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@UserManagement @Users @UsersNegative @Negative
Feature: User Management - Users Maker Checker (Negative)
  Accounts come from TestData/user-management.properties so they can be updated without rewriting steps.
  Maker = config username (auto). Same-role Checker = admin (negative: same role cannot approve).
  Seed users are never edited.

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke
  Scenario: Users negative validations and maker-checker rejection rules
    When user opens User Management Users
    Then Users Maker controls should be displayed
    And user validates mandatory Add User fields
    And user validates Add User field length and datatype negatives
    And user verifies an existing login name is rejected
    And user submits a unique TEST AUTO user request
    And maker attempts to approve the same TEST AUTO user request
    And same-role admin Checker is blocked from approving the TEST AUTO user request
    And maker session is restored on Users
