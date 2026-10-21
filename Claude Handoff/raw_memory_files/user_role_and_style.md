---
name: user-role-and-style
description: "Who the user is on the CKYC_4.0 project, how they communicate, and how they like to collaborate on this automation framework."
metadata:
  node_type: memory
  type: user
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T14:42:49.079Z
---

QA automation engineer (login shown in the app as "Automation aravindh") working on the CKYC 4.0 Selenium/Cucumber/TestNG suite for a banking KYC product (Kotak Mahindra Prime, iFlowCKYC 4.0.0 Enterprise Console). Windows machine, works in IntelliJ, has Maven locally (Claude's sandbox does not — see [[feedback-verification-without-maven]]). Deeply familiar with the CKYC/CERSAI domain (constitution types, maker-checker, bulk file formats) but relies on Claude for the Java/Selenium engineering.

**Communication style**: writes in an informal, often all-caps, run-on shorthand style with inconsistent punctuation and frequent "ok" as a sentence-ender/confirmation marker (e.g. "and then screenshot column create in report ok"). Needs careful parsing — a single message often bundles multiple distinct asks, confirmations of prior recommendations, and new requirements together. When re-reading such a message, look for repeated "and then" as a step separator and "ok"/"am i right" as places the user is checking understanding, not just narrating.

**Working style**:
- Multi-day, iterative: regularly says "we'll continue tomorrow" / "continue further" — treat each session as picking up an ongoing thread, not a one-shot task. Check project memory at the start of a new session on this repo.
- Wants safety-conscious, incremental changes on a real production-adjacent test framework — confirms scope (e.g. "only safe ones" for retry-eligible modules) before large changes, and expects the same caution back (ask before broad retrofits).
- For any *new* module (locators, flows), insists on live UI exploration before code is written — never wants locators guessed from a screenshot or CSV alone. See [[project-customer-information-module]] for the current example of this in progress.
