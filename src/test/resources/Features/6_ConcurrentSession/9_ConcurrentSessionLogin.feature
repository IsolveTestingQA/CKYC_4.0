# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@ConcurrentSession @Smoke
Feature: Concurrent Session Detection
  Validates that the CKYC application detects and handles concurrent (multi-browser) login
  attempts correctly. Opens a second browser with the same credentials while the first is
  still active. The "Active session detected" dialog must appear and its options must work.

  Background:
    Given user is logged in and on CKYC dashboard

  Scenario: Active session dialog appears when same user logs in from second browser
    When user opens a second browser and logs in with the same credentials
    Then the Active session detected dialog should appear in the second browser
    And user captures split-screen screenshot of both browsers
    And user clicks Log out other session and continue in the second browser
    Then the second browser should reach the dashboard
    And the first browser session should be invalidated
    And both browser sessions are cleaned up
