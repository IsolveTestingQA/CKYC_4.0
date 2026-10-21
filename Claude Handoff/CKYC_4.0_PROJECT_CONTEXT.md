# CKYC 4.0 Automation - Project Context / Handoff (for a new Claude chat)

HOW TO USE: In a new chat for this project, say:
"Read 'Claude Handoff/CKYC_4.0_PROJECT_CONTEXT.md' in the project folder and continue from it."

Project: D:\IntelliJ_Projects\CKYC_4.0  (Selenium 4 + Cucumber 7 + TestNG 7, Java 21, Maven, Windows)
GitHub: https://github.com/IsolveTestingQA/CKYC_4.0  (branch main, first commit done 2026-10-04)
Owner/author: Aravindhan (author header added to all .java and .feature files)
App under test: https://iflowckyc.isolveapps.in  (user logs in manually; Claude never enters credentials)
Credentials: src/test/resources/config.properties is git-ignored; config.properties.example is committed.

## Current status (as of 2026-10-05)
DONE
- Framework hardening: Total Time Taken + Summary sheet in Excel, screenshots (success + failure, one folder per date, TC-ID in name),
  scenario-level retry (TestNG; EXCLUDES @UserManagement and @ConcurrentSession), "[Attempt N]" tags, Allure history kept,
  Extent + Allure auto-open (static report via jwebserver, NOT mvn allure:serve), toast-capture polling fix.
- Java validation engine (src/test/java/Com/Ckyc_4_0/validation) ported from CKYC_Cosmos_Validation_Spec and verified vs the Python reference;
  Kotak bank values in config.properties; extra rule DATE-04 (KYC date must be after declaration date).
- Live exploration (Claude in Chrome): Bulk Data Import, Monitor Automation, Dashboard "Data pushed from FI".
- Git repo created and pushed.

PENDING / NEXT
1. Re-walk manually up to Dashboard In-Progress (user was about to do this), then continue In-Progress -> Success polling (30s refresh).
   Blocker seen: IFLOW PROCESS "Not Responding" on Monitor Automation = server-side (dev/ops) job, don't chase from UI.
2. Capture the <input type="file"> locator under the Bulk Data Import "New Record" dropzone (not yet captured).
3. Upload design (user-approved): read CSV in Java first (header row -> name->value map) -> sendKeys file path to input[type=file]
   -> verify file shows in queue -> Create Record -> toast -> Monitor Automation -> Dashboard -> Customer Information reconciliation.
4. At In-Progress stage, reconcile CSV vs Dashboard on Name, Customer ID, BRID.
5. Open questions awaiting user: real Kotak customer-ref regex per persona; BRID single value vs branch lookup; meaning of REMARKS
   codes ERR/DUP/DLI/RFI; correct Minor-detection signal (Customer RP Ref populated vs RP row exists - Job #241 contradicted the stated rule).
6. DEFERRED by user (do not start until asked): a customizable HTML page shown before launching/logging in Chrome, to choose
   modules, positive/negative, and the cycle - after all modules are complete.
7. Optional: author header for pom.xml / .ps1 / properties (offered, not decided).

Below: the full memory files exactly as saved.


---

# MEMORY FILE: project_ckyc_automation_overview.md

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


---

# MEMORY FILE: user_role_and_style.md

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


---

# MEMORY FILE: project_customer_information_module.md

---
name: project-customer-information-module
description: "New CKYC 4.0 automation module in progress — bulk CSV import, iFlow pipeline monitoring, dashboard reconciliation, and Customer Information field verification. Not yet started in code; live UI exploration blocked pending Claude in Chrome setup."
metadata:
  node_type: memory
  type: project
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-10-01T04:21:54.632Z
---

New module the user wants automated next, on top of the existing CKYC_4.0 Selenium/Cucumber/TestNG suite (see [[project-ckyc-automation-overview]]). As of 2026-09-30, **no code has been written for this yet** — still in the flow-confirmation / live-exploration phase.

## Confirmed end-to-end flow (user-approved, 2026-09-29/30)

1. **Pick a CSV** — before navigating anywhere, choose either "from existing test-data folder" (show a list, user selects) or "direct file" (a given path).
2. **Upload via the Bulk Data Import module** — correction (2026-10-01, confirmed via live Claude-in-Chrome exploration): there **is** a separate "Bulk Data Import" module after all (`/bulk/upload`, its own sidebar item), distinct from Customer Information and Customer Status. See [[project-bulk-import-live-findings]] for its full confirmed screen structure. (Superseded the original 2026-09-30 note below this line, which had assumed the Import CSV control lived directly on the Customer Information screen — not what was found live.)
3. **Read the result toast** — green = accepted; red = likely "filename already exists" → must bump the series number and retry.
4. **Check Monitor Automation page** (not yet explored) — verify the iFlow pipeline is running; click Start/Restart if not; "Scheduled" is an acceptable state, no action needed.
5. **Dashboard → In-Progress** — capture the Customer IDs shown there into memory.
6. **Wait/poll** until the batch resolves into Success / Duplicate / Failed counts.
7. **Take the Success list's Customer IDs** (subset of step 5's IDs).
8. **Reconcile**: for each successful Customer ID, open it in Customer Information and compare on-screen fields against the original CSV row (matched by name / CKYC ref — exact join key still TBD). Log any mismatch.

## CSV format (from 3 sample files the user shared and Claude read on 2026-09-30)

- **Location**: external test-data repo, not inside the CKYC_4.0 project — see [[reference-ckyc-test-data-location]].
- **Delimiter**: pipe (`|`), not comma.
- **Filename convention**: `CKYC_DATA_<IN|LE>_<DDMMYYYY>_<series>` — `IN` = Individual (with or without Minor/RP), `LE` = Legal Entity (with RP = authorized signatory). Date **must be today's date**. Series number must be unused or the app rejects the file (red toast).
- **Schema differs by file/type — headers must be read dynamically, never hardcoded by position**:
  - Plain Individual (e.g. `..._31.csv`): ~100 columns, ends at `EMAIL_ID2`, no `REL_PER_*` columns, `ACC_TYPE=01`, `MINOR=N`.
  - Individual + Minor with guardian/RP (e.g. `..._33.csv`, user noted "series ending 33 = individual with minor" — this is their own pack-numbering convention, not a CKYC standard): same base columns as plain Individual **plus** ~90 extra `REL_PER_*` columns appended after `EMAIL_ID2` for the guardian. `ACC_TYPE=05`, `MINOR=Y`.
  - Legal Entity (e.g. `..._35.csv`): entirely different header set — starts with `BRID` (not `APPL_TYPE`), entity fields (`APPL_FULL_NAME`, `DATE_OF_INC`, `PLACE_OF_INC`, `COUNTRY_OF_INC`, `TIN`, `PAN_OR_FORM60`), plus its own `REL_PER_*` block for the authorized signatory.
- **Required reading strategy**: parse row 1 into column names at runtime, build a name→value map per data row (schema-agnostic). Comparison/reconciliation logic must look up fields **by column name**, never by fixed index — confirmed necessary by the actual header differences observed above, not just a hypothetical.

## Customer Information screen (confirmed via a screenshot the user shared, 2026-09-30)

URL: `iflowckyc.isolveapps.in/settings/customer-information`. Layout: left = `Customers (N)` list with search box; right = detail panel with tabs **Personal Details / Contact / Address / Identity Proof / KYC Verification**. Personal Details alone has 25 fields (CONSTITUTION TYPE, ACCOUNT TYPE, CKYC NO, CMCUSTOMERFULLNAME, PAN, DOB OR INC, GENDER, MARITAL STATUS, NATIONALITY, CMFATHERSPOUSEFULLNAME, CMMOTHERFULLNAME, PLACE/COUNTRY OF INCORPORATION, TIN fields, RESIDENTIAL STATUS, DIS FLAG/TYPE/PERCENT, ...).

Verified against real data: customer `KMX10H0009` (Sachin Mishra) on screen matches row 10 of the plain-Individual sample CSV. One confirmed transformation for the reconciliation logic to handle: CSV `DOB=1974-10-04` (ISO `YYYY-MM-DD`) renders on screen as `10-04-1974` (`DD-MM-YYYY`) — **must normalize date formats before comparing, never compare raw strings**.

## Current blocker (as of 2026-09-30)

Live DOM exploration is blocked: the user's corporate Chrome has a DevTools-restriction policy, and the "Claude in Chrome" extension is not yet installed/signed in on their machine. The in-app Browser pane (`mcp__Claude_Browser__*`) was offered as a no-setup alternative (a separate, isolated profile — the corporate DevTools policy likely doesn't apply to it, and `read_page` doesn't use the visible F12 panel anyway), but the user explicitly asked to **wait and continue tomorrow using Claude in Chrome instead**, since that would let Claude see their actual already-logged-in session rather than requiring a fresh login in a separate pane.

**How to apply**: next session, before writing any feature/page-object code for this module, first help the user get Claude in Chrome connected (`https://chromewebstore.google.com/detail/fcoeoabgfenejglbffodgkkbkcdhcgfn`, sign in with the same account), then resume live exploration of: Customer Information's Import CSV flow, Monitor Automation page, Dashboard In-Progress/Success/Failed cards, and the full field-by-field Personal/Contact/Address/Identity Proof/KYC Verification tabs — none of these have real locators captured yet.

**Status update 2026-10-01**: Claude in Chrome is now connected and live exploration is underway — see [[project-bulk-import-live-findings]] for Bulk Data Import screen details and Dashboard "Data pushed from FI" card confirmation. Monitor Automation page also explored: 14 processes, IFLOW PROCESS is the key one but was "Not Responding" even after Start→Restart — user confirmed getting it healthy is a **server-side (dev/ops) responsibility**, not something to chase from the UI/automation side.

## Confirmed upload-step automation design (user-approved, 2026-10-01)

For the Bulk Data Import step specifically, the user confirmed this exact sequence (not a workaround — this is the only way Selenium can do file upload, since it cannot drive a native OS file-picker dialog at all):

1. **Read the source CSV from disk first**, in Java, before touching the browser — dynamic header-row parsing into a `List<Map<String,String>>` (column name → value), per the schema-agnostic approach already confirmed above. This gives the automation the ground-truth row data in memory for every later comparison step (Dashboard reconciliation, Customer Information field check, the validation engine), rather than needing to re-derive it from the UI afterward.
2. **Upload via Selenium**: locate the underlying `<input type="file">` element (present in the DOM even though the visible UI shows a styled dropzone) and call `sendKeys(absoluteFilePath)` directly on it — no OS dialog ever opens, this is the actual automated upload action.
3. **Verify before committing**: confirm the file appears correctly in the upload queue/file list (correct filename, correct format badge) before clicking "Create Record".
4. Then proceed into the already-confirmed flow: read the toast → Monitor Automation check → Dashboard In-Progress polling → Success Customer IDs → Customer Information reconciliation.

**Not yet captured**: the actual `<input type="file">` locator under the dropzone — only the visual dropzone has been seen live so far, not its underlying DOM. Needs a live Claude-in-Chrome `read_page`/DOM inspection pass before this step can be coded.


---

# MEMORY FILE: project_bulk_import_live_findings.md

---
name: project-bulk-import-live-findings
description: "Live-exploration findings from the real Bulk Data Import module (iFlowCKYC, Kotak env) on 2026-10-01 — screen structure, Customer ID generation behavior, and open questions. Feeds the Customer Information reconciliation flow."
metadata:
  node_type: memory
  type: project
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-10-01T04:14:36.243Z
---

Captured via Claude in Chrome against the real `https://iflowckyc.isolveapps.in/bulk/upload` screen (logged in as "Automation aravindh"), 2026-10-01. See [[project-customer-information-module]] for the overall flow this feeds into.

## Screen structure (confirmed, not a separate "Bulk Data Import" module assumption — it's real)

- URL `/bulk/upload`, sidebar label "Bulk Data Import". Also separate sidebar items **Customer Status** and **Customer Information** (not yet explored) — unclear which one shows the In-Progress/Success/Failed dashboard cards described in the original flow; still need to check.
- History table columns: FILE | TYPE | TOTAL ROWS | SUCCESS | FAILED | UPLOADED | STATUS. TYPE badge (`INDIVIDUAL`/`LEGAL ENTITY`) is auto-classified from the filename prefix (`IN_`/`LE_`) — no manual type picker.
- "New Record" opens a right-side panel: drag-and-drop CSV dropzone (`Accepts: CSV`), two template downloads (**Individual Template**, **Legal Entity Template** — no separate Minor template, confirming Minor rides the Individual/`IN` format), a file queue list, "Create Record" (disabled until a file is queued), "Clear".
- Clicking a row's **SUCCESS** count opens a modal "Inserted Rows — Job #N" listing every inserted record: RECORD (Main Applicant / Related Person) | STATUS | CKYC REF | CUSTOMER REF | CUSTOMER RP REF | NAME | REMARKS | CREATED AT, plus a footer "N main applicants · N related persons · N inserted".
- From/To Date filters use a real MUI calendar picker — typing directly into the date textbox does nothing (read-only), must click the calendar icon and navigate months.
- Filename casing is looser than the written spec assumes: a real accepted file was `CKYC_data_IN_30092026_0009.csv` (lowercase `data`) — processed fine, 3/3 success. Don't assume the CSV reference spec's case-sensitive filename regex matches live behavior.

## Customer ID generation — confirmed inconsistent, cause only partially confirmed

Three different ID shapes observed across real uploads, all under the same "Individual" type:
- `KMX10H0001`...`KMX10H0010` (Job #242, `CKYC_DATA_IN_30092026_31.csv`)
- `COSNRI031`...`COSNRI080` (Job #243, today's `CKYC_DATA_IN_01102026_01.csv`, 50 main / 0 RP)
- `CUST100001`, `CUST100002` (Job #241, `CKYC_data_IN_30092026_0009.csv`, 2 main / 1 RP)

Legal Entity (Job #240, `CKYC_DATA_LE_29092026_24.csv`) used yet another scheme: `COSL24090231`...`COSL24090240` for entities, `COSR24091061`...`COSR24091080` for their related persons — this one DOES match the "Cosmos" reference spec's `customer_ref_pattern` (`^COSL\d{8}$` / `^COSR\d{8}$`) exactly, even though this is the Kotak-branded live environment. **This means the "COS" prefix convention is very likely an iFlowCKYC platform-wide convention, not literally specific to "Cosmos Co-op Bank" as the spec's filename implied** — worth re-reading [[project-ckyc-validation-engine]]'s "Cosmos vs Kotak" section with this in mind; the customer-ref-pattern placeholder there may need to become `^COS[A-Z]\d{8}$`-shaped rather than left fully permissive, at least for Legal Entity/RP. Not yet confirmed for Individual/Minor, since none of the 3 Individual examples above match that shape at all.

**Working (unconfirmed) hypothesis**: Customer Ref = whatever the CSV's own `CUSTOMER_REFERENCE_NUMBER` column contained, used as-is, when non-blank; the system auto-generates its own sequential code only when that column was blank in the source CSV. This would explain all 3 Individual examples (different because each source CSV's `CUSTOMER_REFERENCE_NUMBER` column differed) without needing 3 separate rules. **Not verified** — would need the original CSVs for jobs #241/#243 to check their `CUSTOMER_REFERENCE_NUMBER` values directly. Asked the user to confirm 2026-10-01, answer pending.

## Minor-detection rule — user's stated rule vs. one observed exception

User's stated rule (2026-10-01): in an Individual-type file's Job details modal, a Main Applicant row that has a **populated "Customer RP Ref"** indicates that applicant is a Minor (the RP being the guardian); for Legal Entity, each entity can have **multiple** related persons, each getting their own RP customer ID.

**Observed exception**: Job #241 (confirmed to actually contain 1 Minor + 1 guardian, per "2 main applicants · 1 related person"): the guardian's "Related Person" row shares the **same** Customer Ref as its parent (`CUST100001` on both rows) but its own **"Customer RP Ref" column is blank** (`—`), not populated — unlike Legal Entity, where related persons reliably get a distinct `COSR...` value in that same column.

So for Individual/Minor the real signal may be **"a Related Person row exists for this Customer Ref at all"**, not **"Customer RP Ref column is non-blank"** — those are different conditions, and the modal mixes Main + Related Person rows together (tagged by the RECORD column, not by a per-parent RP count), so detecting "this Main Applicant is a Minor" means scanning for a Related Person row with a matching Customer Ref immediately after it, not checking the RP-ref column on the Main row itself. **Asked the user to confirm 2026-10-01, answer pending** — do not implement Minor-detection logic until this is resolved, since the stated rule and the observed data disagree on which column actually carries the signal.

## REMARKS column — meaning unknown, asked user

Three different short codes seen, always alongside STATUS = "Inserted" (success), so these are not error messages:
- LE file (Job #240): every Main Applicant row = `ERR`, every Related Person row = blank.
- IND file (Job #242): mix of `DUP` and `DLI` across Main Applicant rows.
- IND file (Job #243, today, 0 RP): every row = `RFI`.
- IND file (Job #241, has 1 Minor): Main rows = `RFI`, the Related Person row = blank.

Pattern so far: Related Person rows always show blank REMARKS; only Main Applicant rows carry a code, and the code seems tied to the **file/job as a whole** (every Main row in a given job shares the same code) rather than varying per individual record within a job. Meaning unconfirmed — asked the user 2026-10-01, answer pending.

## CKYC REF column

Blank (`—`) on every row seen so far, across all jobs/personas — consistent with "not yet CERSAI-submitted," matching the flow's expectation that CKYC Ref only appears after the Monitor Automation/iFlow processing step (not yet explored live).

## Dashboard "Data pushed from FI" card — confirmed live, 2026-10-01

- URL `/dashboard`. Top strip has 7 big cards (Data Pushed From FI, iFlow In Progress, iFlow Rejections, CERSAI In Progress, CERSAI Rejections, CERSAI Success, Reverse Update to CBS), each a roll-up of sub-cards below (e.g. Data Pushed From FI = A1 Success + A2 Failed + A3 In-Progress + A4 Duplicate). Has From/To date filters, Today/Weekly/Monthly/Overdue toggle, NEW/UPDATE toggle, All/Individual/Legal Entity toggle.
- Clicking a sub-card count (e.g. A3 In-Progress) opens a modal listing every record: S.NO | REGION | BRANCH | DATE | CUSTOMER ID | CUSTOMER NAME, with an **Export CSV** button (not used — downloading needs explicit user permission per policy, didn't ask since not needed yet).
- Confirmed for today's upload (Job #243, `CKYC_DATA_IN_01102026_01.csv`, 50 Individual records, Customer Refs `COSNRI031`–`COSNRI080`): Dashboard showed **A3 In-Progress = 50, A1 Success = 0, A2 Failed = 0, A4 Duplicate = 0**, and the modal's listed Customer IDs (COSNRI033, 036, 039...078, 054, 057, 060, 063, 066, 069, 072, 075) matched expected names from the batch. Matches exactly — no records missing at this stage.
- **Checked ~20s apart twice, no change**: still 50/50 In-Progress both times, and notably **all 8 "iFlow In Progress" sub-queues (B1 Data Validation Queue ... B8) showed 0** — meaning the records are sitting in the FI-pushed bucket but had not yet been picked up by the iFlow processing pipeline at all.
- This lines up with Monitor Automation: IFLOW PROCESS still showed **"Not Responding"** even after Start → Restart (see below). **User confirmed 2026-10-01: getting IFLOW PROCESS actually healthy/running is a server-side responsibility (dev/ops team), not something fixable from the UI or worth retrying repeatedly from our end** — so an "In-Progress" batch stuck at 0% iFlow pickup is expected/normal when the server-side process isn't up, not a UI or data bug to chase.

## Reconciliation checkpoint confirmed so far (CSV → Dashboard In-Progress stage)

Per user, 2026-10-01: at the CSV → Dashboard In-Progress stage, the validation/reconciliation that matters is checking **Name, Customer ID, and BRID** match between the source CSV and what the Dashboard shows. (This is the first of several reconciliation checkpoints in the overall flow — see [[project-customer-information-module]] for the full Customer-Information-page reconciliation that happens later, once records reach Success.) Session paused here 2026-10-01 pending the server-side iFlow process coming up; will continue from Dashboard polling once that's resolved.


---

# MEMORY FILE: project_ckyc_validation_engine.md

---
name: project-ckyc-validation-engine
description: "Java port of the CKYC/CERSAI bulk-CSV field validator (Com.Ckyc_4_0.validation package) — verified against the Python reference, but still Cosmos-bank-configured, not Kotak. Gates whether a CSV record is valid before it's used for Customer Information reconciliation."
metadata:
  node_type: memory
  type: project
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T15:01:47.532Z
---

Built 2026-09-30/10-01: a full Java port of the CERSAI V1.3 bulk-CSV record validator, so the Customer Information reconciliation flow (see [[project-customer-information-module]]) can check "is this CSV row even valid" before comparing it against the UI and reporting a mismatch.

## Where it lives

- Source: `src/test/java/Com/Ckyc_4_0/validation/` — `CkycValidationSpec` (loads the JSON rule spec + CERSAI masters), `CkycRecordValidator` (the engine: field-level + cross-field checks, one method per persona mirroring the Python), `SpecExpr` (hand-written evaluator for the spec's Python-style `when`/`pack_fill` condition strings — no `and`/`or` compounds appear in the current spec, but the parser supports them), `Checksums` (Verhoeff for Aadhaar, GSTIN mod-36), `ValidationIssue` (severity/rule/column/value/message).
- Reference material: `CKYC_OFFICIAL_DOCUMENTS/CKYC_Cosmos_Validation_Spec/` (untouched, original) — `ckyc_validate.py` is the Python reference implementation this was ported from; `ckyc_cosmos_field_spec.json` is the actual rule data, copied verbatim into `src/test/resources/CkycValidation/` (+ its `masters/*.csv`) so the Java code loads the *same* source of truth, not a hand-transcribed copy.

## Verified, not just compiled

Cross-checked the Java port against the Python reference by running both on 3 real rows from the Kotak sample CSVs (Individual, Minor+Guardian, Legal Entity+RP1) and diffing every finding (rule id, column, value, message) — after two real bugs found and fixed this way:
1. The LE "continuation row must be blank" check was being re-applied to row 1 itself (row 1 = entity + RP1 combined) instead of only rows 2+, producing dozens of false `FIELD-BLANK` errors.
2. A missing `LE-03` constitution-applicability check (`constitution X has no <document>`) that sits right next to the `IMG-03` check in the Python — copied the one, missed its neighbor.

After both fixes: 32/32, 57/57, and 68/68 issues matched byte-for-byte across the three test records. This is a real verification, not a "the diff is empty because I didn't test enough" situation — treat the engine as trustworthy for the rule logic itself.

**Not ported** (out of scope for "is this one CSV record valid," which was the actual ask): image-file-existence checks (need a real pack folder on disk), cross-record uniqueness (PAN/mobile/email duplicated across records — needs the whole pack), and file/header-structural checks (needs the raw file, not a parsed row). Add these later if the flow needs them — the Python reference (`image_rules`, `uniq_add`, and the `main()` driver in `ckyc_validate.py`) has the logic to port from.

## The open blocker: Cosmos config vs. Kotak data

**The bundled JSON spec's `config` block is Cosmos Co-op Bank's values** (`ORG_CODE=IN0308`, `ORG_NAME=COSMOS CO OP BANK LTD`, customer-ref-number patterns `^COSI\d{8}$` / `COSM` / `COSL` / `COSR`). The actual sample data (and presumably the real Customer Information module target) is **Kotak Mahindra Prime** (`ORG_CODE=IN4098`, `ORG_NAME=KOTAK MAHINDRA PRIME LTD`, customer IDs like `KMX10H0009` — confirmed not matching any Cosmos pattern). Running the engine as-is against Kotak data produces real-looking but **false** `HIER-01` (org code/name) and `UNIQ-01` (customer-ref pattern) findings — not real defects, just a bank-config mismatch.

The rule *engine* itself (formats, checksums, cross-field logic — CERSAI-standard) is bank-agnostic and does not need to change. `CkycValidationSpec.withConfigOverride(Map<String,String>)` exists specifically so a different bank's config can be swapped in without touching the engine.

**Still needed from the user before this can validate real Kotak data without false positives** (do not guess these):
- Kotak's actual customer-reference-number regex pattern per persona (IND/MIN/LE/RP) — only a handful of `KMX10H0009`-style samples seen so far, not enough to infer the exact pattern confidently.
- Whether Kotak's BRID is validated as one fixed value (like Cosmos's `"001"`) or must be a valid code from a branch master list — the sample data shows a varying branch code (`CHN02`), which doesn't fit the Cosmos-style single-fixed-BRID assumption `hier()` currently makes.
- Whether the `pack_dates` concept (fixed DECL_DATETIME/KYC_VERIFY_DATETIME across a whole generated test pack) applies to Kotak's real data at all, or only to Cosmos's own generated test packs.

See [[project-customer-information-module]] for how this plugs into the reconciliation flow, and [[reference-ckyc-test-data-location]] for where the sample CSVs live.


---

# MEMORY FILE: reference_ckyc_test_data_location.md

---
name: reference-ckyc-test-data-location
description: "Where CKYC bulk-upload CSV test-data packs live on disk, and their naming/series convention — needed for the Customer Information / bulk import automation."
metadata:
  node_type: memory
  type: reference
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T14:42:30.422Z
---

Bulk CSV test-data files for CKYC live **outside** the CKYC_4.0 project repo, in a separate external folder tree:

```
D:\CKYC_Test_Data_4.0\CKYC_LE_Test_Data\03_TEST_DATA\Kotak\DEV\<Pack folder>\
    01_INDIVIDUAL\CKYC_DATA_IN_<DDMMYYYY>_<series>.csv
    02_MINOR\CKYC_DATA_IN_<DDMMYYYY>_<series>.csv
    03_LEGAL_ENTITY\CKYC_DATA_LE_<DDMMYYYY>_<series>.csv
```

Example pack seen 2026-09-30: `Sep_10_2026_Pack6_DVS_Common_Images`, containing `CKYC_DATA_IN_10092026_31.csv` (plain Individual), `CKYC_DATA_IN_10092026_33.csv` (Individual + Minor with guardian/RP), `CKYC_DATA_LE_10092026_35.csv` (Legal Entity with authorized signatory).

Filename rule (app-enforced, not just convention): `CKYC_DATA_<IN|LE>_<DDMMYYYY>_<series>` — the date segment must equal **today's date** when uploading, and the series number must not already exist for that date/type or the CKYC app rejects the upload (red toast, duplicate-file error).

See [[project-customer-information-module]] for how this feeds the bulk-import automation flow.


---

# MEMORY FILE: feedback_verification_without_maven.md

---
name: feedback-verification-without-maven
description: "How to verify Java changes to CKYC_4.0 when Maven isn't on this machine's PATH — confirmed working approach the user accepted across multiple sessions."
metadata:
  node_type: memory
  type: feedback
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T14:43:04.122Z
---

This sandbox has no `mvn` on PATH (confirmed 2026-09-29), but the user's own machine does and they run the suite themselves (IntelliJ or `mvn test`). Claude cannot execute the Cucumber/TestNG integration itself here.

**Confirmed working verification approach** (used successfully across several rounds of framework changes — reporting/retry/screenshot work in late Sept 2026): compile the changed/new `.java` files directly with `javac`, using a classpath built from `target/classes` + `target/test-classes` + a filtered subset of jars under `~/.m2/repository` (filter by groupId keywords: selenium, cucumber, poi, allure, extentreports, slf4j, log4j, aspectj, testng, commons-io, jackson, org.json, velocity — the *unfiltered* full repo scan produces a classpath string too long for `javac`'s argv even via `cygpath -wp`, so always filter). Paths must be converted to Windows form with `cygpath -w`/`cygpath -wp` since git-bash's own POSIX-style paths are unusable by native `javac.exe`. Pass the classpath as a normal argv string (not a javac `@argfile` — argfiles silently failed to pick up an embedded `-cp "...;...;..."` value in testing on 2026-09-29, even well under the OS argv length limit; root cause not fully diagnosed, just avoid argfiles for this).

**Why this matters / how to apply**: this lets Claude catch real compile errors (missing imports, wrong API signatures — e.g. this caught the correct `io.cucumber.testng.PickleWrapper.getPickle().getTags()` signature before writing `RetryAnalyzer`) without needing Maven. But it is *not* a substitute for running the actual suite — always tell the user plainly that runtime/integration behavior (report generation, retry firing correctly, screenshots landing right) still needs to be verified by them with a real run, and ask for specific things to check back. The user has consistently accepted this two-step approach (compile-verify here, runtime-verify by them) without pushback — don't skip the "please verify" step by overclaiming confidence just because `javac` was clean.

**Lesson from a real miss**: an auto-open-report feature was compile-clean but silently didn't work at runtime, because it was placed *after* an unguarded `driver.quit()` call in a teardown method — an exception there aborted the method before reaching the new code, with nothing logged. Since Claude can't runtime-test here, when adding a "final step" to any teardown/shutdown-style method, proactively audit for unguarded prior calls that could throw and silently skip everything after them (order defensively: risky teardown last, wrapped in try/catch) — don't wait for the user to report it broken.


---

# ADDENDUM 2026-10-05: DVS 2.0 automation (see memory file project_dvs_automation.md content below)

---
name: project-dvs-automation
description: "DVS 2.0 automation added to CKYC_4.0 on branch feature/dvs-2.0 (Option A, 2026-10-05) - what exists, what is NOT runnable yet, how to run/dry-run."
metadata:
  type: project
---

Design pack from another chat lives in `D:\CKYC_7.0_TestData\OCT_05\` (Design_Pack, Automation_TestData.xlsx, Locator_Map.xlsx, dvs_locators.properties). User chose **Option A**: Claude writes the DVS code into CKYC_4.0, new files only, on git branch `feature/dvs-2.0` (uncommitted at time of writing). Login stays manual; no credentials typed.

**Built (compile-verified with javac only, never run against the live app):** `utils/dvs` (DvsRow, DvsConfig, DvsExcelReader, DvsLocators, DvsImageNameParser, DvsSnapshot, DvsResultWriter, DvsScreenshot, DvsDryRun), `functionality/dvs` (DvsFieldActions, DvsNavigation, DvsRestoreManager, DvsRowExecutor, DvsRunEngine), `stepdefinitions/dvs/DvsSteps`, 4 runners `TestDVS{Positive,Negative,Combined,Regression}Runner` (no retry listener on purpose), `Features/7_DVS/*.feature`, resources `DVS/` + `TestData/DVS/`. Only existing file touched: `config.properties.example` (comments appended).

**Key finding:** the test-data workbook is a descriptive catalog, not machine-typed values. Offline dry run (COMBINED, 594 rows): only 42 plannable; 404 need Field->Locator mapping (`DVS/DVS_FieldMap.properties`, worklist in `DVS/FieldMap_Worklist.csv` with guessed suggestions), 84 are Image rows (image-panel check not built; DvsImageNameParser exists and was sanity-tested), 63 have descriptive Input_Value. Rows that cannot run are reported NOT RUN with a reason, never guessed.

**Unconfirmed assumptions in code:** queue row xpath `(//*[normalize-space(text())='{ID}'])[1]` (dvs.queue.rowXpath), date format dd-MM-yyyy (dvs.dateFormat), tab-label map (Attestation unmapped), group end = last row of Group_ID (Restore_Point column ignored), expected-result classification by first word (Rejected/Blocked => needs error; Accepted/Allowed => no error; else CAPTURED).

**How to apply:** next step is for the user to run DvsDryRun / a real positive run and report failures; then fill FieldMap, build image-panel checks, DB validation (off until Q-08 answered), checker flow (LATER).
