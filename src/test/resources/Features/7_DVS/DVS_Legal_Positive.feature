# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@DVS @DVS_LE_POS
Feature: DVS 2.0 Legal Entity positive suite

  Scenario: DVS 2.0 Legal Entity positive scenarios on the live record
    Given DVS test data and locators are loaded
    And user opens DVS 2.0 from the side navigation
    When DVS run is executed in mode "LE_POSITIVE"
    Then DVS result file is published with no failures
