# DVS 2.0 + Image Upload - automation flows and resume notes

Author: Aravindhan (Tested by) | Written: 2026-10-13 | Branch: `feature/dvs-2.0`

## 1. Status - why this is parked

- DVS 2.0 and the Image Upload module are **not frozen** yet. Live automation tuning of both is **paused until both are frozen**.
- Everything built is **kept as is** - nothing is deleted or rewritten. The proper locators (XPaths) are already in the repo.
- When both modules are frozen the flows below are executed in the order of section 6.
- Work continues on the other modules in the meantime (see the module list in the project memory).

## 2. What is kept (do not delete)

| Item | Where |
|---|---|
| Locators (261) + kinds | `src/test/resources/DVS/dvs_locators.properties`, `dvs_locator_kinds.csv`, `dvs_locators_extra.properties` (nav, step-log, audit, checker revert) |
| Field map / worklist | `DVS/DVS_FieldMap.properties`, `FieldMap_Worklist.csv`, `DVS_CsvMap.properties`, `DVS_ErrorListMap.properties` |
| Typed values for descriptive rows | `DVS/DVS_TypedValues.properties` (TC_ID = LOCATOR \| value ; ...) |
| Suite scenarios | `DVS/IND_Scenarios.csv` (88: 24 pos / 64 neg), `DVS/LE_Scenarios.csv` (61: 15 pos / 46 neg) |
| Test data workbook (design) | `Claude Handoff/DVS2.0/DVS_Automation_TestData_v2_06-10.xlsx`, `DVS2.0_Locator_Map.xlsx`, `DVS2.0_Automation_Design_Pack.xlsx` |
| Change request 06-10 | `Claude Handoff/DVS2.0/DVS2.0_Change_Request_06-10.md` |
| Engine | `functionality/dvs/*` (DvsRunEngine, DvsRowExecutor, DvsNavigation, DvsFieldActions, DvsRestoreManager, DvsCheckerFlow, DvsItemErrors, DvsDataAudit, DvsStuckPrompt) and `utils/dvs/*` |
| Runners + features | `TestDVSIndividual{Positive,Negative}Runner`, `TestDVSLegal{Positive,Negative}Runner`, `TestDVS{Positive,Negative,Combined,Regression}Runner`; `Features/7_DVS/*.feature` |
| Panel / tags | `@DVS_IND_POS @DVS_IND_NEG @DVS_LE_POS @DVS_LE_NEG @DVS_POS @DVS_NEG @DVS_COMBINED @DVS_REG` |

## 3. DVS flows to automate / re-run

### 3.1 Navigation flow (all DVS runs)
1. `1_Login` scenario first (the panel adds `@Login` automatically for DVS) - login stays from `config.properties`.
2. Side navigation: click **DVS 2.0** (`div#tour-nav-dvs-2.0`, `data-tour-label="DVS 2.0"`) -> `/dvs-2`. "DVS" and "DVS 2.0" are separate nav items. Direct URL only if `dvs.allowDirectUrl=true` (nav bar first - standing rule).
3. Choose the role button **Maker role** (Checker role only in the checker flow) and the case type **DVS Individual** or **Legal Entity**.
4. **Queue** tab -> search box (`Search customer name or ID`) -> open the customer from the configured list `dvs.cust.individual` / `dvs.cust.le` (first one found in the Maker queue is used). Never navigate with the Customer_ID of a workbook row - always the run's configured customer.
5. Record view tabs: Personal Details, Identity / POI, Contact & Address, Related Persons, KYC Verification (tab names carry a badge, so match with *contains*).

### 3.2 Suite row flow (Individual / Legal, Positive and Negative are separate runners)
1. Snapshot the record (every field of every tab) and run a **baseline Submit** to learn the standing errors.
2. For each scenario row: set the field value -> blur -> **Save** (temporary save only, "Application Saved Successfully") -> if Save dropped the value, enter it again -> **Submit to Checker** -> read toast ("Please fix N field(s)"), dialog ("N Validation Errors Found", grouped by section + "Other"), red count badge on the tab, inline "<Field> is required" -> screenshot -> close the dialog.
3. **Restore after EVERY row** and verify the restore; stop the module if the restore cannot be verified (live product - never leave modified data).
4. Verdict: Positive = value accepted and no new validation error for that field; Negative = Submit blocked with the expected message. The real validation answer comes from Submit, not from Save.
5. Result workbook (+ HTML) with Step_Log sheet, Bugs sheet and `logs/<Run_ID>.log`.

### 3.3 Round-trip rows (workbook runs - Positive / Negative / Combined / Regression)
Read values -> Save -> reopen the same customer (the panel shows "No Record Loaded" after Save/Submit, so the customer must be searched again) -> compare. Tab-switch persistence checks T1/T2.

### 3.4 Checker flow (later)
`DvsCheckerFlow`: R2 revert recovery and C1-C4 compare (Maker submits -> Checker role opens the same customer -> compare -> revert). Config `checker.recovery` (default ON). Evidence zip: `DVS2.0_Checker_Revert_Evidence.zip`.

### 3.5 Item Error List / data audit
- `DvsItemErrors` E1-E3, S1: parse the Item Error List panel (text parsing unverified).
- `DvsDataAudit` A1-A3: A2 only when `dvs.audit.csv` + `DVS_CsvMap.properties` are filled.

### 3.6 Image panel (84 image rows) - NOT BUILT
Image panel checks (zoom in/out, rotate, reset, revert to image, caption n/N). `DvsImageNameParser` exists and was sanity-tested; the panel check itself is still to build once the image UI is frozen.

## 4. Image Upload module - flows to prepare when frozen

Not automated yet. Known touch points:
- Dashboard sub-card **Image Scan/Upload Queue** is a navigation-only card (count check only, never click) - in the dashboard and reports compare.
- **Image Availablitity Queue** and **Image Compression Queue** open a report popup (full compare already handled).
- When frozen: find the module in the nav bar (nav bar first, endpoint only as fallback), capture locators with the nav click, then write feature + runner + `ControlPanelRegistry` entry (tag the feature, one registry line).

## 5. Live facts to remember (verified in the app)
- One browser tab / one session only. Never "log out other session" for the user; close the extra iFlow tab only when the one-tab rule blocks (standing OK).
- Save = temporary save; Submit to Checker = real validation. IN05100004 always has ~6 standing errors ("Other") + 8 iFlow revert issues, so Submit is always blocked there (safe). Dates dd/mm/yyyy; inputs have maxlength.
- Dropdowns: select/read by visible text prefix (hidden value is not the code).
- The built-in browser pane is blocked by the app ("Developer tools are not permitted"); do not drive Search / DVS through MCP - the Selenium runners are the test.
- "Token API Failed" on Search after ~16 s is a back-end defect, not the debugger.

## 6. Resume checklist (when DVS and Image Upload are frozen)
1. Pull the frozen build; re-check the tab names and the queue row xpath (`dvs.queue.rowXpath`), date format (`dvs.dateFormat`), Attestation tab mapping.
2. Dry run (`DvsDryRun`) -> confirm counts: plannable / NOT RUN / image rows.
3. Fill `DVS_FieldMap.properties` from `FieldMap_Worklist.csv` (only locators confirmed live) and structured `DVS_TypedValues.properties`.
4. First live run: **TestDVSIndividualPositiveRunner** -> review workbook -> tune `IND_Scenarios.csv` Expect columns (CAPTURE rows and wrong guesses are expected first time).
5. Then Individual Negative, Legal Positive, Legal Negative (LE record never inspected live).
6. Build the image panel checks and the Image Upload module; add checker flow (`checker.recovery`) and DB validation (off until Q-08 is answered).
7. Run all from the **Test Control Panel** (order: Login, Dashboard, Masters, Search, Roles, Users, DVS, Reports, Concurrent session).

## 7. Open items / unverified guesses
- Item Error List text parsing, eye-button reveal for masked fields (`dvs.maskEyeXpath` empty), image caption n/N.
- `checker.recovery` default ON instead of CONFIG `checker.flow=LATER`.
- Submit is only clicked for rows whose Expected_Result mentions submit + block/reject (workbook runs).
- Expected-result classification by first word (Rejected/Blocked => needs error; Accepted/Allowed => no error; else CAPTURED).
- 404 of 594 workbook rows need a Field -> Locator mapping; 63 have descriptive Input_Value.
