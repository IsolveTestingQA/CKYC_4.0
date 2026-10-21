# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Reports
Feature: Reports module vs Dashboard (Weekly)
  As a CKYC admin user
  I want every dashboard sub-card report to match the same report in the Reports module
  So that counts, customer ids and their details are proven consistent for NEW / UPDATE and ALL / Individual / Legal entity

  Background:
    Given user is logged in and on CKYC dashboard

  @Reports_Compare
  Scenario: Weekly dashboard reports match the Reports module and every report is stored
    When weekly dashboard reports and Reports module data are collected and compared
    Then reports comparison workbook is published with no mismatches
