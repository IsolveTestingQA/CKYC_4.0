# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@DVS @DVS_NEG
Feature: DVS 2.0 negative run

  Scenario: DVS 2.0 negative validations
    Given DVS test data and locators are loaded
    And user opens DVS 2.0 from the side navigation
    When DVS run is executed in mode "NEGATIVE_ONLY"
    Then DVS result file is published with no failures
