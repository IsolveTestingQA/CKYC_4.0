# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Login @Positive
Feature: Login Page
  As a CKYC user
  I want to sign in with valid credentials
  So that I can access the CKYC dashboard

  @Smoke
  Scenario: Successful login with valid credentials in API mode
    Given user is on CKYC login page
    When user selects runtime mode from config
    And user enters valid credentials from config
    And user clicks Sign In button
    Then user should be logged in successfully
