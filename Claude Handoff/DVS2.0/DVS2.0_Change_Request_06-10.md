# DVS 2.0 – Change Request 06-10-2026 (paste into the CKYC_4.0 automation chat)

Your DVS module already exists:
- `functionality/dvs/*`: DvsRunEngine, DvsRowExecutor, DvsFieldActions, DvsNavigation, DvsRestoreManager
- `utils/dvs/*`: DvsLocators, DvsExcelReader, DvsResultWriter, DvsSnapshot, DvsImageNameParser, DvsConfig, DvsScreenshot, DvsDryRun
- `stepdefinitions/dvs/DvsSteps`, `Features/7_DVS/*.feature`, and the 4 `TestDVS*Runner` classes

**Extend that module. Do not rewrite it, and do not touch any non-DVS file.**

Read these first. They are in `Claude Handoff/DVS2.0/`:
- `DVS2.0_Automation_Summary_Prompt.md`: the full rules, R/C/T/E/A/P/S/L.
- `DVS2.0_Automation_Design_Pack.xlsx`: sheets `08d_Recovery_and_Checks` and `16_Image_Name_Rules`.
- `DVS2.0_Checker_Revert_Evidence.zip`: 17 screenshots of the live Maker → Checker → Revert flow from 06-10.
- `DVS_Automation_TestData_v2_06-10.xlsx`: the newer test data.
  - Compared with the current `src/test/resources/TestData/DVS/DVS_Automation_TestData.xlsx`, it adds columns Group_ID, Group_Seq and Restore_Point at the END of each data sheet, plus sheets IMAGE_NAME_RULES and STEP_LOG_TEMPLATE, plus new CONFIG keys.
  - Copy it over the old file only after DvsExcelReader reads columns by header name.

## 1. FIX FIRST – dropdown value is NOT the CKYC code (verified live)
MUI hidden input values are internal IDs, not the codes shown on screen. Example: Account Type shows "05 - MINOR" but its hidden `value` is `2`, and the option `data-value`s are 1, 2, 0, 4.
- `DvsFieldActions` line ~220 selects by `//li[@data-value='<value>']`, and lines ~99 and ~109 read the hidden `value`. Change both:
  - **Select** by visible text prefix: `//ul[@role='listbox']//li[starts-with(normalize-space(),'<code> - ') or normalize-space()='<text>']`
  - **Read** the visible text: `//input[@name='X']/preceding-sibling::div[@role='combobox']`, then `getText()`. Compare on the code prefix before " - ".
- Keep a `data-value` fallback only where the visible text equals the data-value, e.g. Gender MALE/FEMALE, Prefix Mr/Ms.

## 2. Persona detection (new: `utils/dvs/DvsPersona.java`)
- **LE:** the Legal Entity toggle is active (header "DVS 2.0 Legal Entity — Maker/Checker").
- **Minor:** Account Type text starts with "05 - MINOR", the helper reads "CKYC KYC Type M — Minor", and the Minor checkbox is ticked and disabled.
- **Individual:** anything else ("01 - NORMAL", "02 - SMALL", "04 - OTP E-KYC").
- `DvsRowExecutor` skips Minor-only rows when the persona is not Minor, and LE rows unless the module is LE. Log the reason as NOT RUN.

## 3. Checker revert recovery – rule R2 (new: `functionality/dvs/DvsCheckerFlow.java`)
Trigger: an invalid value was accepted AND Submit succeeded. The success toast is exactly "Application submitted successfully and moved to Checker.", and the record closes.
1. Log the BUG.
2. Click `//button[@aria-label='Checker role']` (keep the Individual / LE toggle). Search the ID in `//input[contains(@placeholder,'Search Customer')]` and open the row whose ID text matches exactly.
3. Click EVERY record tab: `//button[@role='tab']`, excluding Queue and Image.
   - Each click shows a toast "… Tab Updated".
   - If a tab is skipped, Confirm Revert gives the toast "Some tabs are not verified Please verify all tabs". That revert does NOT happen, but the reason text still gets stored (an app bug).
4. Run the Maker → Checker compare (section 4) while visiting the tabs.
5. Click `//button[normalize-space()='Revert']`. A dialog opens: "Revert Record" or "Revert Legal Entity Record".
   - Type into `//div[@role='dialog']//textarea[not(@aria-hidden='true')]` (max 300 characters): `AUTO-REVERT <TC_ID>: <field>=<value> accepted (bug)`.
   - Click `//div[@role='dialog']//button[normalize-space()='Confirm Revert']`. It is disabled until text is typed. Expect the toast "Reverted Successfully".
6. Click `//button[@aria-label='Maker role']`, search and open the record. Check that:
   - the queue row and Item Error List → CHECKER REVERT contain the reason,
   - status is "Draft",
   - values equal the Maker snapshot.
7. Keep the bug value (`keep.bug.value=true`), mark the field BUG_LOCKED in the run state, and continue.
- If the record doesn't come back to Maker, retry once. Then mark that customer's remaining rows BLOCKED (R3).

## 4. Maker → Checker strict compare – C1–C4 (extend `DvsSnapshot` and `DvsRestoreManager`)
- **C1 – before EVERY Submit**, snapshot all tabs, keyed `tab|Locator_ID` (RP rows `RPn|field`). Include:
  - visible dropdown text, checkbox and radio state, dates,
  - masked values (click the eye button),
  - RP table rows,
  - the image list: `//img[contains(@alt,' - ')]/@alt` plus the "n / N" caption,
  - Mode of KYC and the declaration flag.
- **C2 – in Checker,** read the same fields and compare. Log each mismatch as BUG "Maker→Checker mismatch" with tab, field, Maker value, Checker value and both screenshots. This covers a missing field, blank vs value, different text, different checkbox or radio, an RP row missing or extra, and a different image list.
- **C3 – read-only:** every Checker field must be `disabled`. Re-read after all tab clicks; there must be no change.
- **C4 – after Revert,** Maker values equal the C1 snapshot.

## 5. Tab-switch persistence – T1–T3 (DvsRowExecutor)
- **T1 – after entering a value:** open another tab (this autosaves), come back and read the field. It must equal the entered value. Then `reloadAndReopen` and read again.
  - Old value shown: BUG "not updated".
  - Value changed or trimmed: BUG "altered".
- **T2:** an invalid value that survives the reload is a BUG "invalid data autosaved".
- **T3:** before Submit, visit all tabs once.

## 6. Item Error List and messages – E1–E3, S1
- **E1 – capture:** on every open, Save, Submit and revert, read `//*[contains(normalize-space(),'ITEM ERROR LIST')]` and its three columns (IFLOW REVERT, CKYC REVERT, CHECKER REVERT). Store the list items.
- **E2 – map entries to fields:** use a new `DVS/DVS_ErrorListMap.properties` (entry phrase → Locator_ID). For each mapped field, verify it shows an error and holds the value named in the entry.
  - An entry with no field is a BUG.
  - An entry still listed after the field was fixed is a BUG "stale iFlow error". Example: MN02100119 shows "KYC Verification Name field is empty" while Emp. Name = VIKRAM MEHTA.
- **E3 – counts:** the "N Validation Errors Found" dialog count (`//div[@role='dialog']`) must equal the tab badge total and the inline error count. Errors under "Other" are a UI bug.
- **S1 – toasts:** always store `[role=alert]` text. Server errors appear ONLY as a toast, e.g. "Invalid value in Current Address City field".
- Submitting while iFlow REVERT errors are still listed is a BUG. LE02100008 and MN02100119 both submitted that way.

## 7. Record data audit before any scenario – A1–A3 (new: `functionality/dvs/DvsDataAudit.java`)
- **A1:** read every field on every tab without editing. A mandatory field that is empty, or a value that breaks a format rule, is a DATA_AUDIT bug.
- **A2:** if the customer's CSV row exists in the test pack, compare:
  - CSV has a value but DVS is empty or different: mapping bug.
  - CSV is empty and the field is mandatory: "mandatory missing accepted at upload".
- **A3:** images – check the name pattern (IMAGE_NAME_RULES) and that the document label matches the selected POI/POA.
- Known examples:
  - IN03100002: POI = DL but the image is Passport; city VISAKHAPATNAM vs district TIRUCHIRAPPALLI; correspondence PIN blank for Argentina.
  - LE02100008: Employee CKYC ID was empty (now 50000000001234).

## 8. Step logs – L1–L3 (MOST IMPORTANT; reuse `utils/StepLog.java` and `Hooks.runStep()`)
- **L1 – one Step_Log row per action:** click, type, select, tab switch, Save, Submit, Revert, reload, restore, retry, compare. Columns are in sheet STEP_LOG_TEMPLATE:
  - Run_ID, Timestamp_IST (ms), TC_ID, Customer_ID, Persona, Role, Tab, Field_Locator_ID, Action
  - Value_Before, Value_Entered, Value_After, Expected, Actual
  - Inline_Error, Toast_Text, Dialog_Text, Item_Error_List
  - API_Status, Screenshot_Path, Step_Result, Duration_ms
- **L2 – outputs:** `DvsResultWriter` adds a Step_Log sheet next to Summary and Failed, plus `logs/DVS_<Run_ID>.log`. Each Failed row links to its Step_Log rows.
- **L3 – never skip:** log exceptions, retries and restores. Flush on crash.

## 9. Layout notes
- Find buttons by text, never by coordinates: "Submit to Checker" sits on a second header row for LE.
- Opening and switching tabs in Checker triggers "… Tab Updated" saves. This is expected; log it.
- Login stays manual. Never type credentials.

## Deliver
- Compile-clean code. Do not change any non-DVS file.
- Report every new or changed DVS file in a list.
- Keep `DvsDryRun` working, and add dry-run output for the new checks.
