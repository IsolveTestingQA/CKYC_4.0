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
