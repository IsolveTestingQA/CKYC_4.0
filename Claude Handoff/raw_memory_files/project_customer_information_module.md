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
