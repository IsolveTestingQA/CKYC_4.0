# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@DVS @DVS_IND_POS
Feature: DVS 2.0 Individual positive suite

  Scenario: DVS 2.0 Individual positive scenarios on the live record
    Given DVS test data and locators are loaded
    And user opens DVS 2.0 from the side navigation
    When DVS run is executed in mode "IND_POSITIVE"
    Then DVS result file is published with no failures
