---
name: project-ckyc-automation-overview
description: What the CKYC_4.0 automation framework is and where the full architecture writeup lives — orientation for any new session on this repo.
metadata:
  node_type: memory
  type: project
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T14:43:16.195Z
---

CKYC_4.0 is a Selenium 4 + Cucumber (BDD) + TestNG hybrid automation suite (Java 21, Maven) testing the iFlowCKYC 4.0.0 Enterprise Console — a banking KYC product for Kotak Mahindra Prime. Modules already automated: Login, Dashboard, Masters (State/District/Pincode/Hierarchy), Search, User Management (Roles/Users, maker-checker), Concurrent Session. Reporting: Excel (per-module sheets + a Summary sheet with pass/fail totals and Total Time Taken), Extent HTML, Allure, a timestamped execution log, and TC-ID-named screenshots.

A full architecture/design writeup (layers, `Hooks.runStep()` recovery flow, reporting pipeline, module coverage diagrams) was produced as a Word document, `CKYC_4.0_Automation_Framework.docx`, delivered to the user around 2026-09-25 — read that (or ask the user for it) before re-deriving framework structure from scratch.

**Framework hardening done late Sept 2026** (see git history / recent session for specifics): scenario-level retry for safe modules only (Dashboard/Search/Masters/Login — never User Management or Concurrent Session, tag-based via `RetryAnalyzer`), Excel Summary sheet + Total Time Taken, date-based screenshot naming, auto-opening Extent/Allure reports after a run (`ReportOpener`), Allure trend-history retention across runs.

**Current focus (as of 2026-09-30)**: a new, not-yet-coded module — bulk CSV import → Customer Information reconciliation. See [[project-customer-information-module]] for the confirmed flow and CSV format details, and [[user-role-and-style]] for how this user likes to work.
