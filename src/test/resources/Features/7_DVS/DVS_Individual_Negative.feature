# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@DVS @DVS_IND_NEG
Feature: DVS 2.0 Individual negative suite

  Scenario: DVS 2.0 Individual negative scenarios on the live record
    Given DVS test data and locators are loaded
    And user opens DVS 2.0 from the side navigation
    When DVS run is executed in mode "IND_NEGATIVE"
    Then DVS result file is published with no failures
