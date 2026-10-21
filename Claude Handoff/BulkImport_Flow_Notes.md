# Bulk Data Import -> Monitor Automation -> Dashboard -> Customer Information

Author: Aravindhan (Tested by) | 2026-10-18 | Runner: `TestBulkImportRunner` (tag `@BulkImport`, also a module in the Test Control Panel)

## Flow (as built)
1. **Bulk Data Import** (nav bar). Pop-up in the page: drag and drop (or browse) the CSV; it is saved under `Current Data/BulkImport/<date>/` with its own name, queued with `sendKeys` on the hidden `input[type=file]`, **Create Record**, toasts read (`Upload accepted (Job #n)`, `WARNING: n column(s) absent ...`). Failure -> pop-up: **Retry** / **Choose another file** / **I uploaded it by hand** / **Cancel**, until it succeeds or is cancelled.
2. The job row is found by file name (waits while the status is processing). SUCCESS count -> "Inserted Rows" list: **Main Applicant** customer refs = the expected ids; **Related Person** rows are kept. FAILED count -> list read, each failed record becomes a **bug with its remarks**. A Legal Entity (`_LE_`) file without any related person is a bug. IN files may carry related persons (minor guardian; no minor flag anywhere).
3. **Monitor Automation** (Settings > Monitor Automation): whatever the status of IFLOW PROCESS is, **Restart** (or Start); refresh every 10 s, at most 6 times, until Not Responding goes. Still Not Responding = finding (bug), flow continues.
4. **Dashboard > Data pushed from FI**: In-Progress first, then Success / Failed / Duplicate; NEW and UPDATE application types; every 30 s refresh and re-read, max 7 minutes, **timer card in the page** (Stop waiting button). After the time: an id in none of the four lists = MISSING (bug); still In-Progress = bug; Failed = bug; Duplicate = reported. The customer name on the Dashboard is compared with the CSV.
5. **Customer Information**: random Success ids (`bulkimport.compareCount`, default 3): search by id, read every tab (tabs found from the PERSONAL DETAILS tab's siblings), each section's `N fields` header vs the labels read, masked values: eye icon clicked and re-read. Compared with the CSV row through the **field mapping**.

## Field mapping (freeze workflow)
- No frozen file -> **learning mode**: the mapping is suggested from the first customer's real values (value equality, name = prefix+first+last, code -> description from the spec masters / learned, label looks like the column or the spec label) and saved to `Current Data/BulkImport/mapping/FieldMap_<IN|LE>.draft.csv` + `CodeMap_<..>.draft.csv`.
- Review the draft (confidence HIGH / MEDIUM / LOW), fix wrong links, add `IGNORE` rows for CSV columns that are not shown on screen, then copy to `src/test/resources/CustomerInfo/FieldMap_IN.csv` and `CodeMap_IN.csv` = **frozen**.
- Frozen run: screen labels are looked up by name (so a re-ordered screen works); a mapped label that disappeared, a new unmapped label and a changed section field count are reported; codes not in the code book are UNVERIFIED (learned), a known code with a different text is a bug.
- Mandatory columns (spec status M) with a value that is shown nowhere = bug.

## Output
`Report Output/Bulk Import/<date>/BulkImport_<time>.xlsx`: SUMMARY, IDS, POLLS, COMPARE, BUGS, MAPPING, FAILED_ROWS; plus the Advanced / Extent / Cucumber reports (every check is a step).

## Not done yet
- Legal Entity Customer Information tabs (related-person sections) were not captured: LE mapping is learned on the first LE run.
- Bulk Data Import 2.0 (separate module) not started.
- Live run of the whole flow (offline checks only: compile, mapping on the captured Individual customer, page-reader JS in jsdom).
