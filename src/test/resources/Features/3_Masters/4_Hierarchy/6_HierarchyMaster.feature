# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@Masters @HierarchyMaster @Positive
Feature: Masters - Hierarchy Master
  As a CKYC admin
  I want Hierarchy Master tabs (FI, Region, CPC, Branch) validated with safe CRUD
  So that hierarchy data can be tested without changing existing master rows

  Background:
    Given user is logged in and on CKYC dashboard

  @Smoke @HierarchyFI
  Scenario: FI Master tab — summary, validation, safe CRUD and search
    When user opens Hierarchy Master from Masters menu
    Then Hierarchy Master page should be displayed
    And user verifies Hierarchy Master summary chips are visible
    And user runs FI Master tab validation and safe CRUD

  @Smoke @HierarchyCPC
  Scenario: CPC Master tab — validation, safe CRUD and search
    When user opens Hierarchy Master from Masters menu
    Then Hierarchy Master page should be displayed
    And user runs CPC Master tab validation and safe CRUD

  @Smoke @HierarchyRegion
  Scenario: Region tab — validation, safe CRUD and search
    When user opens Hierarchy Master from Masters menu
    Then Hierarchy Master page should be displayed
    And user runs Region tab validation and safe CRUD

  @Smoke @HierarchyBranch
  Scenario: Branch tab — validation, safe CRUD and search
    When user opens Hierarchy Master from Masters menu
    Then Hierarchy Master page should be displayed
    And user runs Branch tab validation and safe CRUD
