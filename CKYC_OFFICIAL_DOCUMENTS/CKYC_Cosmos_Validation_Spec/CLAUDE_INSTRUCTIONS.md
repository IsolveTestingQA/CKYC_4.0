# Instructions for Claude (CKYC Cosmos automation validation)

You are helping automate validation testing of CKYC / CERSAI V1.3 bulk uploads for **Cosmos Co-op Bank**:

| Setting | Value |
|---|---|
| ORG_CODE | IN0308 |
| ORG_NAME | COSMOS CO OP BANK LTD |
| BRID | 001 |
| Upload channel | iFlow bulk CSV, pipe-delimited |

Use the files in this folder as the only source of truth.

## Read in this order

1. `01_VALIDATION_RULES.md` sections 2-8: file rules, pack structure, masters, the document cross-map, where to verify, persona rules, conflicts.
2. The field spec for the persona in question:
   - `02_FIELD_SPEC_INDIVIDUAL.md` (99 columns);
   - `03_FIELD_SPEC_MINOR.md` (198 columns);
   - `04_FIELD_SPEC_LEGAL_ENTITY.md` (189 columns).
3. For code, load `ckyc_cosmos_field_spec.json`. Never hard-code a rule that already exists there.

## How to decide whether a value is valid

For each column, check in this order:

1. `status`:
   - **M**: must be filled.
   - **CM**: must be filled when `when` is true. `when` is a Python-style expression over column names, and all values are strings.
   - **O**: may be blank.
   - **BLANK_FOR_NEW** and **SYSTEM_BLANK**: must be blank.
2. If filled: `max_len`, then `format` (regex and checksum from `formats`), then `enum`.
   - `CONFIG:x` means the value must equal `config.x`.
   - `MASTER:file` means the value is looked up in `masters/`.
3. Numbers whose format depends on a type code (`BY_POI_TYPE`, `BY_POA_TYPE`, `BY_COMM_IDTYPE`, `BY_LE_POI_TYPE`, `BY_LE_POA_TYPE`) take their format from `number_format_by_code`.
4. Then apply every rule in `cross_field_rules` for that persona. ERROR is a hard failure; WARN is a Cosmos pack convention.

## Rules people most often get wrong

- **Same document everywhere.** POI_TYPE, POI_CATEGORY, POI_NUMBER and POI_FILE_NAME must describe one document. The same goes for POA and PERM_ADD_PROOF. The mapping is `doc_map`. Example: E → "Proof of Possession of Aadhaar" → 12-digit Verhoeff number → Aadhar.jpg → RP column REL_PER_UID.
- **POIA.** When POI and POA are the same document, the number and the image are the same. When they are different documents, both differ.
- **PERM_TO_COMM_FLG = Y.** The correspondence address is an exact copy of the permanent one, and COMM_ADDRESS_IDTYPE / _NUMBER / _CATEGORY equal the POA.
- **PERM_TO_COMM_FLG = N.** The address is different. The proof is a deemed proof only (11 Utility Bill, 12 Property Tax, 13 Pension Payment Order at age 58 or over, 14 Allotment Letter), never an OVD. A minor uses 11 or 12 only. A Legal Entity always uses 06/07/99, equal to POA_TYPE.
- **RP identity numbers.** The number of every document the RP uses goes in the matching RP ID column, and at least one column must be filled. That covers the REL_PER_ADD_PROF document, the RP POI image document and, when the flag is N, the REL_PER_CURR_ADD_PROOF document. The mapping is 01 UID, 02 PASSPORT, 03 DRIVING, 04 VOTER, 05 NREGA, 08 POPULATION_LET, 09 EKYC, 10 OFFLINE_UID.
  - EKYC and OFFLINE_UID hold the last 4 digits of the UID.
  - When the flag is N, the current proof is a different document from the permanent proof.
- **Minor.** ACC_TYPE 05, MINOR Y, age under 18.
  - Never uses Voter ID (B/04) or Driving Licence (D/03).
  - Minor With RP: guardian REL_TYPE 1, aged 18 or over, on the same row.
  - Minor Without RP: the whole RP block is blank except CUST_ID.
- **Legal Entity.**
  - Row 1 = entity + RP1. Each continuation row has columns 1-91 blank and CUST_ID set to the entity.
  - No entity photo.
  - POI is 02 or 03. POA is 06 or 07, equal to PERM_ADD_PROOF.
  - REL_TYPE by constitution: D/E → 4 with an 8-digit DIN; B/J → 8; H → 7.
  - The GSTIN embeds the entity PAN and the GST state code.
- **Images.** They live in `Common_Images/<CUSTOMER_REFERENCE_NUMBER>/` under DOCUMENT names (Photo.jpg, Aadhar.jpg, Passport.jpg, DrivingLicense.jpg, VoterID.jpg, NREGA.jpg, IncorporationCertificate.jpg, RegistrationCertificate.jpg), with RP1_/RP2_ in front for RPs.
  - POSITIVE and DVS: every file is present.
  - IMAGE_PROCESSING: a file is present exactly when IMAGE_STATUS_SUMMARY.csv says AVAILABLE.
- **Uniqueness.** Customer ids, PAN, every ID number, mobile and e-mail are unique per person across all packs.
- **Dates.** DD-MM-YYYY; not in the future; not the upload date.

## Expected outcomes

| Folder | Expected outcome |
|---|---|
| 01_POSITIVE | Accepted, no DVS error |
| 02_DVS | Held in DVS with an error on the field named in SUMMARY.csv `DVS_DEFECT_FIELD` |
| 03_IMAGE_PROCESSING | Data accepted; routed to the image queue for the MISSING files only |

## When you write automation code

- Reuse `ckyc_validate.py` (functions `check_record`, `fmt_ok`, `verhoeff`, `gstin_ok`, `pin_check`), or port the same logic. Keep rule ids (for example `RPID-02` or `COMM-01`) in assertion messages.
- Drive UI or DB assertions from `test_cases.csv`, which gives the precondition, the action, the expected result and the rule id.
- DB tables:
  - CKYC_FIN_DATA (Individual / Minor applicant);
  - CKYC_FIN_DATA_RP (Minor guardian);
  - CKYC_FIN_DATA_LE (entity);
  - CKYC_FIN_DATA_LE_RP (LE related persons).
  Column names equal the CSV header names. Each field's `where_to_verify` lists the CSV column, the DB table, the CERSAI record (Detail 20/30/40/70), the image and the master.
- If a rule is unclear or sources conflict, check `01_VALIDATION_RULES.md` section 8 before assuming anything. Say which conflict applies; do not silently pick a side.
