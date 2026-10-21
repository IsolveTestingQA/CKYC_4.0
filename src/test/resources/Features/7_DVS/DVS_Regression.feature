# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@DVS @DVS_REG
Feature: DVS 2.0 regression run

  Scenario: DVS 2.0 raised bug recheck
    Given DVS test data and locators are loaded
    And user opens DVS 2.0 from the side navigation
    When DVS run is executed in mode "REGRESSION"
    Then DVS result file is published with no failures
