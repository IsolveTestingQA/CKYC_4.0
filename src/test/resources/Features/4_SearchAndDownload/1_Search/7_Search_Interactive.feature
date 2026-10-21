# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@SearchAndDownload @SearchInteractive
Feature: Search And Download - interactive Search
  As a CKYC operations user
  I want to choose the Document Type and type the number in a popup
  So that every search - valid and invalid - is recorded with its outcome, message, result fields and screenshot

  Background:
    Given user is logged in and on CKYC dashboard

  Scenario: Interactive Search session with report
    When user opens Search from Search And Download menu
    Then Search page should be displayed
    And user runs the interactive Search session
    Then the Search results report is published
