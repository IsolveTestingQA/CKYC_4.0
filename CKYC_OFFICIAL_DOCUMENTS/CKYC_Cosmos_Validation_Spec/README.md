# CKYC Cosmos Validation Spec – start here

A complete, machine-readable spec of the Cosmos CKYC 4.0 bulk CSV. It covers Individual, Minor and Legal Entity with multiple related persons. It includes every header, its allowed values, the CERSAI V1.3 masters, the POI / POA / ID-number rules, where to verify each value, and a runnable validator plus a test-case catalog for automation.

## Files

| File | For | What it is |
|---|---|---|
| `CLAUDE_INSTRUCTIONS.md` | Claude / AI agents | Paste or attach this first in any automation chat or project. It tells the agent how to use everything below. |
| `01_VALIDATION_RULES.md` | People + Claude | The rule book: file rules, pack structure, masters, document cross-map, where to verify each ID, persona rules, conflicts and findings, the full cross-field rule catalogue and the value formats |
| `02_FIELD_SPEC_INDIVIDUAL.md` | People + Claude | All 99 Individual columns: status, condition, length, allowed values, pack fill, DB table, notes |
| `03_FIELD_SPEC_MINOR.md` | People + Claude | All 198 Minor columns (applicant + guardian block) |
| `04_FIELD_SPEC_LEGAL_ENTITY.md` | People + Claude | All 189 LE columns (entity block + RP block, continuation rows) |
| `ckyc_cosmos_field_spec.json` | Code | The single source of truth: config, formats (regex), masters, doc_map, per-persona column specs, cross_field_rules |
| `field_spec_all_personas.csv` | Excel / Java / Selenium | The same column specs as a flat table (486 rows) |
| `masters/*.csv` | Code | CERSAI masters exported from Bulk File Structure V1.3: pincode (18,234), district, state, country, constitution, RP type, identity code, POA master, document master, impairment |
| `ckyc_validate.py` | Code | Reference validator for a whole pack (CSV + images + DVS/IP expectations) |
| `make_test_cases.py` | Code | Builds `test_cases.csv` from the spec and self-checks every case against a real positive record |
| `test_cases.csv` | Automation | 898 test cases (positive controls, mandatory, conditional, format, allowed values, length, cross-field), each with the expected rule |

## Run the validator

Requirements: Python 3.8 or later, standard library only.

```
# extract the pack zips first, then:
python ckyc_validate.py "D:\...\COSMOS_Pack1_30Sep2026" "D:\...\COSMOS_Pack2_30Sep2026" "D:\...\COSMOS_Pack3_30Sep2026" --out report --upload-date 30-09-2026
```

Outputs:

- `report/issues.csv`: one line per finding, with severity, rule, pack, folder, file, line, customer, RP, column, value and message.
- `report/record_results.csv`: PASS or FAIL per record. For DVS records it also shows whether the expected defect from SUMMARY.csv was caught.
- `report/summary.json`: counts.

Severity levels:

- **ERROR**: a CERSAI, iFlow or user hard rule.
- **WARN**: a Cosmos pack convention, or the `DB-LEN` length conflict.
- **PACK**: a field the positive pack normally fills was left blank.

A correct pack gives these results:

- 01_POSITIVE and 03_IMAGE_PROCESSING: every record PASS.
- 02_DVS: every record FAIL, with `dvs_expected_caught = YES`.

The exit code is 1 if any record has an unexpected outcome.

## Regenerate the test cases

```
python make_test_cases.py "D:\...\COSMOS_Pack1_30Sep2026" --out test_cases.csv
```

## Results on the Cosmos 30-Sep-2026 packs 1-3 (7,200 records)

| Folder | Records | Result |
|---|---|---|
| 01_POSITIVE | 2,400 | 2,400 PASS |
| 02_DVS | 2,400 | 2,400 FAIL; the planted defect was caught for 2,400 of 2,400 |
| 03_IMAGE_PROCESSING | 2,400 | 2,400 PASS; images match IMAGE_STATUS_SUMMARY |

The run also produced findings to review; see `01_VALIDATION_RULES.md` section 8:

- two duplicate secondary e-mails (EMAIL_ID2) in Pack 1;
- 780 LE records carry a 21-character CIN, which is longer than the iSolve DB column (20).

## Changing bank or environment

Edit `config` in `ckyc_cosmos_field_spec.json`: ORG_CODE, ORG_NAME, BRID, customer_ref_pattern, image_naming_mode and pack_dates. The validator reads the config and every field-level rule from the JSON. Cross-field rules are coded in `ckyc_validate.py` under the same rule ids as `cross_field_rules`. FILE-03 (series continuity) is documented but not checked automatically. Rules that are specific to Cosmos packs are tagged WARN, and their `source` field says so.
