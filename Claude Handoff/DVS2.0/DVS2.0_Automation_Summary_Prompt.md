# DVS 2.0 Automation – Summary & Prompt for the CKYC_4.0 project chat

Paste this into the automation project chat together with these files from `D:\CKYC_7.0_TestData\OCT_05\`:
`DVS2.0_Automation_Design_Pack.xlsx`, `DVS_Automation_TestData.xlsx`, `DVS2.0_Locator_Map.xlsx`, `dvs_locators.properties`.

---

## Task
Add a **DVS 2.0 module** to `D:\IntelliJ_Projects\CKYC_4.0`. Rules:
- **Additive only.** Do not change any existing file.
- New code goes in `pages/dvs`, `functionality/dvs`, `stepdefinitions/dvs`, `utils/dvs`, `Features/7_DVS` and `TestRunner`. The full class list is in sheet `12_Architecture`.
- **Login is manual.** Never type credentials. Use admin access for both Maker and Checker toggles.
- **Locators:** read only from `dvs_locators.properties` (key = Locator_ID). No hard-coded XPaths.
  - MUI dropdown: `//input[@name='X']/preceding-sibling::div[@role='combobox']`
  - Option: `li[data-value=code]`
- **Test data:** `DVS_Automation_TestData.xlsx`.
  - Sheets: CONFIG, IMAGE_NAME_RULES, DOC_MASTER, POSITIVE, NEGATIVE, BOUNDARY, DEPENDENCY, DOCUMENT_MAPPING, IMAGE_MAPPING, REGRESSION.
  - Columns: TC_ID … Screenshot_Path, plus Group_ID, Group_Seq, Restore_Point.

## Persona detection (Individual / Minor / Legal Entity)
- Decide the persona from the record, not only from the ID prefix.
  - **Legal Entity:** the `Legal Entity` toggle is active (the header reads "DVS 2.0 Legal Entity — Maker/Checker").
  - **Individual vs Minor:** use the Account Type text and the helper line under it.
    - "05 - MINOR" with "CKYC KYC Type M — Minor" plus the Minor checkbox ticked (disabled) = **Minor**.
    - "01 - NORMAL" (KYC Type N), "02 - SMALL" or "04 - OTP E-KYC" = **Individual**.
- **IMPORTANT:** the hidden dropdown value is NOT the CKYC code. For example, Account Type "05 - MINOR" has hidden `value=2`, and the options' data-value are 1, 2, 0, 4. Always read and select by the visible text prefix ("05 - "), never by `@value` or `data-value`, for Account Type and similar masters.
- Run Minor-only scenarios (DOB / age, guardian / RP tab) only when the persona is Minor. Run LE scenarios only in the LE toggle.

## Run modes (sheet 08c_Run_Modes)
- `POSITIVE_ONLY`: runs the positive rows and saves a "last positive snapshot" of the customer.
- `NEGATIVE_ONLY`: runs the negatives and restores from the last snapshot.
- `COMBINED`:
  1. Run positive first.
  2. Run negatives group-wise (one connected-field family per Group_ID).
  3. At GROUP_END, apply the last positive data, Save, reload and verify.
- `REGRESSION`: re-runs the bug list only.
- Filters: module, priority, group, TC_ID.
- Every run writes a new result file (Summary + Failed sheets) plus `Screenshot/<TC_ID>/`.

## Per-row flow (negative / boundary / dependency)
1. Open the customer (search by ID or prefix), go to the tab, and read the baseline value.
2. Enter Input_Value, and Connected_Value if given.
3. Trigger the check: blur, then Save or Submit as the row says.
4. Capture all of the following:
   - inline helper text
   - toast
   - "N Validation Errors Found" dialog (section + messages)
   - tab badge counts
   - screenshot
   - updateCustomerDetailsV2 request and response (CDP)
5. Compare with Expected_Result and mark the row PASS or FAIL.
6. Stay on the same row until there is a clear verdict. If the page reloads, a dialog stays stuck, or the session refreshes, re-open the same customer, tab and field (maximum 2 retries).
7. Restore using the group rule: at GROUP_END apply the last positive data. If a row blocks the screen, restore only that field.
8. Stop only on a browser crash, session expiry, or an irrecoverable record.

## Bug handling – invalid value accepted (sheet 08d, rows R1–R5)
- **R1 – invalid value accepted, no error:** log a BUG and do NOT click Submit for that row. Continue.
- **R2 – invalid value accepted and Submit to Checker succeeds.** This flow was verified live on 06-10 on LE02100008 (Legal Entity) and MN02100119 (Individual / Minor). The evidence is in `DVS2.0_Checker_Revert_Evidence.zip`.
  1. **Detect the submit.** The toast "Application submitted successfully and moved to Checker." appears, the record closes ("No Record Loaded"), and the Maker queue no longer lists it.
  2. **Log the BUG** "invalid value submitted to checker".
  3. **Open the record as Checker.** Click `//button[@aria-label='Checker role']`, keep the same DVS Individual / Legal Entity toggle, type the customer ID in the queue search, and open the row.
     - The Checker header has `Approve` and `Revert` buttons, and all fields are read-only (disabled).
     - The Checker view has NO Item Error List.
  4. **Verify all tabs.** Click every record tab: Personal/Entity, Identity / POI, Contact & Address, Related Persons, KYC Verification.
     - Each click shows a toast: "CUSTOMER DETAILS / CUSTOMER POI DETAILS / CUSTOMER POA DETAILS / RP DETAILS / CUSTOMER KYV DETAILS Tab Updated".
     - If any tab is not visited, Confirm Revert fails with the toast "Some tabs are not verified Please verify all tabs".
     - While visiting, read every field and check that it equals the Maker value (the invalid value must be present).
  5. **Revert.** Click `//button[normalize-space()='Revert']`.
     - A dialog opens: "Revert Record" (Individual) or "Revert Legal Entity Record" (LE), with the text "You are reverting CKYC record <ID> … notify the Maker to re-process".
     - Type into the textarea "Revert Reason *" (no name attribute, max 300 characters, counter "n / 300"): `AUTO-REVERT <TC_ID>: <field>=<value> accepted (bug)`.
     - "Confirm Revert" is disabled until the reason is typed. Click it. The toast "Reverted Successfully" appears and the record leaves the Checker queue.
     - Dialog locators: `//div[@role='dialog']//textarea[not(@aria-hidden)]` and `//div[@role='dialog']//button[normalize-space()='Confirm Revert']`.
  6. **Back to Maker.** Click `//button[@aria-label='Maker role']`, search the customer and open it.
     - The queue row shows the remark in orange: "↩ <reason>".
     - The Item Error List → CHECKER REVERT shows the same reason.
     - Status is "Draft" again, and all values are unchanged (verified: Employee CKYC ID entered before submit was still there).
  7. **Keep the bug value** (`keep.bug.value=true`), mark the field BUG_LOCKED, and continue with the next field or scenario.
- **R3 – customer not back in the Maker queue after revert:** retry once. If it's still missing, mark that customer's remaining rows BLOCKED and move to the next customer.
- **R4 – a later row whose Connected_Field is BUG_LOCKED:** run it with the bug value in place and note that in Actual Result. Setting `keep.bug.value=false` restores the last positive value instead.

## Step logs (MOST IMPORTANT – L1–L3)
- **L1 – log EVERY action as one Step_Log row.** Actions include click, type, select, tab switch, Save, Submit, Revert, reload, restore, retry and compare. Each row has:
  - **Identity:** Run_ID, Timestamp (IST, milliseconds), TC_ID, Customer_ID, Persona, Role (Maker / Checker), Tab, Field (Locator_ID), Action.
  - **Values:** Value_Before, Value_Entered, Value_After (read back from the screen), Expected, Actual.
  - **App messages:** Inline_Error, Toast_Text, Dialog_Text, Item_Error_List (iFlow / CKYC / Checker).
  - **Evidence and outcome:** API status plus a response snippet, Screenshot_Path, Step_Result (PASS / FAIL / INFO / RETRY / RESTORE), Duration_ms.
- **L2 – where logs go:**
  - a Step_Log sheet in the run's result xlsx, next to Summary and Failed,
  - a plain-text `Logs/<Run_ID>.log`,
  - Extent and Allure steps through the existing `Hooks.runStep()`.

  Each Failed row links to its Step_Log rows and screenshots.
- **L3 – never skip logging:**
  - On an exception, log the message, retry number, restore action and final state.
  - On a crash, flush the log before stopping.
  - Log the checker-revert recovery and the Maker→Checker comparison field by field.

## Maker → Checker strict comparison (C1–C4), on EVERY Submit
This applies to positive submits as well as bug-accepted submits.
- **C1 – Maker snapshot before Submit:** visit every tab and store every field, keyed `tab|field` (RP fields `RPn|field`). Include:
  - visible dropdown text,
  - checkbox and radio state,
  - dates,
  - masked values, revealed with the eye icon,
  - every Related Person row and its form fields,
  - the image list (file name, document label, n/N),
  - Mode of KYC and the declaration flag.
- **C2 – Checker strict compare:** open the record in Checker, visit every tab, read the same fields and compare one by one. Each of these is a BUG "Maker→Checker mismatch":
  - a missing field,
  - blank in Checker where Maker had a value,
  - a different value, code or text,
  - a different checkbox or radio state,
  - a missing or extra RP row,
  - a different image list.

  Log the tab, field, Maker value and Checker value, with a screenshot of both.
- **C3 – read-only:** every Checker field must be disabled. Re-read after visiting all tabs; nothing may change (tab clicks trigger "Tab Updated" saves).
- **C4 – after Revert:** reopen the record as Maker. Values must equal the C1 snapshot, nothing lost or changed. After Approve, the values stay the same and the status changes.

## Tab-switch persistence (T1–T3)
- **T1 – after every changed field:**
  1. Switch to another tab (this autosaves), come back and read the field. It must equal the entered value.
  2. Reload the record and read the field again.
  3. If the old value shows, log BUG "not updated". If the value is changed or trimmed, log BUG "altered".
- **T2 – invalid value plus tab switch:** if the invalid value is silently saved (still there after reload), log BUG "invalid data autosaved".
- **T3 – before Submit:** visit every tab and re-read all fields. After Submit or a checker revert, the Checker view must equal the Maker values tab by tab.

## Item Error List (E1–E3)
- **E1 – capture:** on every open, Save, Submit and revert, read the "ITEM ERROR LIST · n ISSUE(S) FOUND" panel. Capture its three columns (IFLOW REVERT, CKYC REVERT, CHECKER REVERT) and store each entry.
- **E2 – map:** link each entry to a field (mapping table, e.g. "Account Type" → accountType, "city" → city). Then verify that:
  - the field exists,
  - the field shows the error highlight or text,
  - the value named in the entry is the value in the field.

  An entry with no field, or a field with no error, is a BUG "error list not mapped".
- **E3 – counts:** the dialog count must equal the tab badge total, which must equal the number of inline errors. Errors grouped under "Other" instead of their own tab are a UI bug.

## Record data audit, before any scenario (A1–A3)
- **A1 – pre-scan:** read every field on every tab without editing anything. A mandatory or conditional-mandatory field that is empty, or a value that breaks a format rule, is a DATA_AUDIT BUG. Log the field, value and rule.
- **A2 – compare with the CSV row from the test pack:**
  - CSV has a value but DVS is empty or different: BUG "CSV→DVS mapping".
  - CSV is empty and the field is mandatory: BUG "mandatory missing accepted at upload".
  - Source not known: log it as DATA_AUDIT, source unknown (still a bug).
- **A3 – images:** check name pattern (IMAGE_NAME_RULES), document label, and that the image matches the selected POI/POA. For example, IN03100002 has POI = Driving Licence but its image is "Passport" – that is a BUG.

## Layout notes for locators (verified 06-10)
- **Submit to Checker button position:**
  - Individual: in the record header row.
  - Legal Entity: a second header row, because the long entity name wraps.
  - Always locate it with `//button[normalize-space()='Submit to Checker']`, never by coordinates.
- **Server-side errors come only as a toast, not in the validation dialog.** Example: IN03100002 Submit gave the red toast "Invalid value in Current Address City field" (correspondence: Argentina, city BRUNO AIRES, PIN blank). Always capture `[role=alert]` text after Save and Submit.
- **Queue search:** click the search box, type the ID, then open the row whose ID text matches exactly.

## Already verified live on 05/06-10 (use as expected behaviour)
- Submit with a mandatory field blank shows the toast "Please fix N field(s)" and the dialog "N Validation Errors Found". The record stays with the Maker.
- Reloading the record discards unsaved edits. Tab switching autosaves.
- Name fields cap at 33 characters.
- Choosing POI D enables DL expiry and the RTO flag and disables the others.
- With certified copy, e-document and DigiLocker all unticked, Submit is blocked.
- Open bugs:
  - Employee fields are mandatory for every Mode of KYC (should be Face to Face with RE official only).
  - Declaration unticked is not validated.
  - Mother First Name is marked ** but not validated.
  - Citizenship list shows the garbled entry "Ã…land Islands".
  - Checker revert reason is saved even when Confirm Revert fails ("tabs not verified"). The next revert APPENDS the text with no separator, e.g. "…(LE)AUTO-REVERT TEST…". The same pattern is seen in the IN03100002 old remark.
  - Records submit successfully while the Item Error List still shows iFlow revert errors:
    - LE02100008: 4 date errors, Date of Commencement empty.
    - MN02100119: 12 errors, including "KYC Verification Name field is empty" while Emp. Name = VIKRAM MEHTA, so the iFlow error list is stale or not mapped.
  - Checker view says "Draft" and calls a "Tab Updated" save on every tab click in a read-only view. Toast typo: "CUSTOMER KYV DETAILS".
  - LE02100008 was submitted with "Declaration provided" unticked, so the declaration is not validated for LE either.
  - LE KYC Verification tab has no Mode of KYC section; confirm whether it is required for LE.
  - Data audit examples:
    - IN03100002: permanent city VISAKHAPATNAM with district TIRUCHIRAPPALLI / Tamil Nadu (city–district mismatch); correspondence PIN blank for a foreign address; POI = DL but image = Passport.
    - LE02100008: Employee CKYC ID was empty (filled with 50000000001234 for this test).
