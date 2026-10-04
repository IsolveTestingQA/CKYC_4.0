# CKYC / CERSAI V1.3 – Cosmos bulk CSV validation rule book

**Bank:** Cosmos Co-op Bank · ORG_CODE `IN0308` · ORG_NAME `COSMOS CO OP BANK LTD` · BRID `001` · UAT
**Scope:** iFlow bulk CSV for Individual (99 columns), Minor (198 columns) and Legal Entity with multiple related persons (189 columns). It covers every header, its allowed values, the CERSAI masters, the POI / POA / ID-number rules, where to verify each value and what DVS and Image Processing should produce.
**Machine copy:** `ckyc_cosmos_field_spec.json` holds everything in this book as data. `ckyc_validate.py` runs it against a pack.
**Version:** 1.0 · 30-09-2026 · checked against the Cosmos 30-Sep-2026 packs 1-3.

---

## 1. Sources and priority (highest first)

| # | Source | Used for |
|---|---|---|
| 1 | User rules given for the Cosmos packs (Sep 2026) | Comm-proof rule (flag N → deemed proof), RP current-proof ID rule, image naming, LE continuation rows, "fill every field", uniqueness |
| 2 | Cosmos 30-Sep-2026 packs 1-3 (live convention) | Code values actually used, pack structure, formats of generated numbers |
| 3 | `iFlow CKYC Data Format for Individuals V1.3` | Individual CSV column names, iFlow M/CM/O, iFlow lengths |
| 4 | `Bulk File Structure_Version 1.3.xlsx` (CERSAI) | Detail 20/30/40/70 validations and error messages, all masters, Pincode master (w.e.f. 24-Apr-2026) |
| 5 | `CKYC_Field_Length_Dropdown_Guide_V1.3_FIXED.xlsx` | DB column names and lengths, DB tables CKYC_FIN_DATA / _RP / _LE / _LE_RP |
| 6 | `iSolve_KYC_Data_Mapping_Guide_Legal` | LE and RP M/CM/O, iSolve staging DB types |
| 7 | Cosmos team mail `File_Upload_Name_Format.txt` | File naming CKYC_DATA_IN/LE_DDMMYYYY_NN |

Where the sources disagree, section 8 lists the conflict and the value this spec uses.

## 2. File-level rules

| Item | Rule |
|---|---|
| File name | `CKYC_DATA_IN_DDMMYYYY_NN.csv` for Individual and Minor, `CKYC_DATA_LE_DDMMYYYY_NN.csv` for Legal Entity. NN runs on across all packs of the day (30-Sep set: IN 01-270, LE 01-90). |
| Format | Pipe `\|` delimited, UTF-8 with BOM, first row is the header, empty value = nothing between pipes (never NULL, "-" or a space). |
| Header | Exact column names and order from `personas.<IND/MIN/LE>.header` in the JSON. IND = 99, Minor = 198, LE = 189 columns. |
| Mixing | Never mix Individual and LE in one file. Never mix NEW (APPL_TYPE 01) and UPDATE (03). All current packs are NEW. |
| Dates | `DD-MM-YYYY` everywhere. Not in the future and not the upload date (DOB, DOI, DECL, KYC). Pack dates: DECL_DATETIME `02-09-2026`, KYC_VERIFY_DATETIME `03-09-2026`. |
| Hierarchy | ORG_CODE = `IN0308`, ORG_NAME = `COSMOS CO OP BANK LTD`, BRID = `001`. REL_PER_ORG_CODE / REL_PER_ORG_NAME = the same, even in DVS. |
| Uniqueness | CUSTOMER_REFERENCE_NUMBER, REL_PER_CUST_ID, PAN, every ID number, DIN, CIN/LLPIN, GSTIN, deemed-proof numbers, mobile and e-mail must be unique per person across all packs. |

## 3. Pack structure and expected result per folder

```
COSMOS_Pack<N>_30Sep2026/
├── 01_POSITIVE/            01_Individual · 02_Minor_With_RP · 03_Minor_Without_RP · 04_Legal_Entity_Multi_RP
├── 02_DVS/                 (same 4 sub-folders)
├── 03_IMAGE_PROCESSING/    (same 4 sub-folders)
├── Common_Images/<CUSTOMER_REFERENCE_NUMBER>/   one folder per customer (all 3 purposes)
├── SUMMARY.csv             one line per record (+ DVS_DEFECT_FIELD, DVS_DEFECT, VALID_VALUE, DVS_VALUE for DVS)
└── IMAGE_STATUS_SUMMARY.csv  one line per person: PHOTO/POI/POA file + AVAILABLE / MISSING
```

Each folder holds 10 CSVs. IND and Minor CSVs have 20 records each. LE CSVs have 20 companies × 2 RPs = 40 rows.

| Folder | CSV data | Images | Expected automation result |
|---|---|---|---|
| 01_POSITIVE | All rules pass | All files present | Record accepted, no DVS error, pushed to CKYC |
| 02_DVS | Exactly one deliberate data defect per record (SUMMARY.csv `DVS_DEFECT_FIELD`). RPs with flag N also carry a wrong-format current-address proof ID. | All files present | Record held in DVS with an error on the defect field. Nothing else in the record is wrong. |
| 03_IMAGE_PROCESSING | All rules pass (same as positive) | Photo / POI / POA each AVAILABLE or MISSING, for applicant and RPs, as listed in IMAGE_STATUS_SUMMARY.csv (8 combinations for persons, 4 for LE entity) | Record routed to the image-upload / image-processing queue for the MISSING files only |

DVS defect catalogue (rotates per record; `SUMMARY.csv` names the field). The column in brackets shows who gets the defect: all personas unless marked.

- **Identity numbers:** invalid POI number (special characters), invalid POA number, invalid PAN format, invalid POI type code `Z`.
- **Address:** PIN not in the master (`999999`).
- **KYC block:** KYC verifier name blank, declaration place blank, KYC date in the wrong format (`2026/09/03`).
- **Individual and Minor:** applicant first name blank, first name with special characters, invalid prefix `XYZ`, DOB in the future, gender `X`, 9-digit mobile.
- **Minor only:** MINOR=`N` for a person under 18, Voter ID (`B`) used as POI.
- **Legal Entity only:** entity name blank, invalid GSTIN, constitution `Z9`, date of incorporation in the future, an Individual POI code (`E`) used.
- **Records with an RP:** RP first name blank, REL_TYPE `99`, RP under 18, invalid RP UID.
- **LE directors only:** 7-digit DIN.

## 4. Masters – quick reference (full lists in `masters/*.csv`)

**Constitution type** (`CONST_TYPE`): Individual = `01`. LE: A Sole Proprietorship · B Partnership Firm · C HUF · D Private Ltd · E Public Ltd · F Society · G AOP/BOI · H Trust · I Liquidator · J LLP · K Artificial Liability Partnership · L Public Sector Bank · M Govt Dept · N Section 8 · O Artificial Juridical Person · P Intl Org/Embassy · Q Not Categorized · R Others (CONST_TYPE_OTHERS mandatory) · S FPI. **Cosmos packs use D, E, J, B, H.**

**Related person type** (`REL_TYPE`): 1 Guardian of minor · 2 Assignee · 3 Authorised Representative · 4 Director (DIN mandatory) · 5 Promoter · 6 Karta · 7 Trustee · 8 Partner · 9 Proprietor · 10 Court Appointed Official · 11 Beneficiary · 12 Authorised Signatory · 13 Beneficial Owner · 14 PoA holder · 15 Other (REL_TYPE_OTHERS mandatory). **Pack: Minor 1; LE D/E → 4, B/J → 8, H → 7.**

**Identity Code – POI_TYPE**

| Code | Document | For | Max | ID no. | Upload? |
|---|---|---|---|---|---|
| A | Passport | IND | 20 | Yes | Yes |
| B | Voter ID | IND (not Minor) | 20 | Yes | Yes |
| C | PAN | IND/LE | 10 | – | **No** (download only) |
| D | Driving Licence | IND (not Minor) | 20 | Yes | Yes |
| E | Proof of Possession of Aadhaar | IND | 12 | Yes (CERSAI keeps last 4) | Yes |
| F | NREGA Job Card | IND | 40 | Yes | Yes |
| G | NPR Letter | IND | 20 | Yes | Yes |
| H | E-KYC Authentication | IND | 12 (last 4 kept) | Yes | Yes (ACC_TYPE 04 needs H) |
| I | Offline Verification of Aadhaar | IND | 12 (last 4 kept) | Yes | Yes |
| J | Foreign govt / embassy document | IND, RES_STATUS 03 | 20 | – | Yes |
| Z, S01, S02 | Others / Simplified | IND | – | – | **No** |
| 01 | OVD of authorised person | LE | – | No | Yes |
| 02 | Certificate of Incorporation / Formation | LE | 60 | Yes | Yes |
| 03 | Registration Certificate | LE | 60 | Yes | Yes |
| 04-10 | MOA/AOA, Partnership Deed, Trust Deed, Board Resolution, PoA, Activity Proof 1/2 | LE | – | No | Yes |

**Proof of Address master – POA_TYPE / PERM_ADD_PROOF / COMM_ADDRESS_IDTYPE / REL_PER_ADD_PROF / REL_PER_CURR_ADD_PROOF**

| Code | Document | For | Notes |
|---|---|---|---|
| 01 | Proof of Possession of Aadhaar | IND | |
| 02 | Passport | IND | |
| 03 | Driving License | IND (not Minor) | |
| 04 | Voters Identity Card | IND (not Minor) | |
| 05 | NREGA Job Card | IND | |
| 06 | Certificate of Incorporation / Formation | LE | |
| 07 | Registration Certificate | LE | |
| 08 | NPR Letter | IND | |
| 09 | E-KYC Authentication | IND | last 4 digits |
| 10 | Offline verification of Aadhaar | IND | last 4 digits |
| 11 | Utility bill (≤ 2 months old) | IND | Deemed – current address only, no ID/image required |
| 12 | Property / Municipal Tax receipt | IND | Deemed – current address only |
| 13 | Pension / family pension payment order | IND | Deemed – current address only (pack: age ≥ 58) |
| 14 | Letter of allotment of accommodation | IND | Deemed – current address only |
| 15 | Self declaration | IND | Current address only, when POI&A is Aadhaar |
| 16 | Foreign govt / embassy document | IND | RES_STATUS 03, image mandatory |
| 99 | Others | LE | PERM_ADD_PROOF_DESC mandatory |
| S01-S06 | Simplified | IND | **Not allowed for upload** |

**Document master – image codes (CERSAI Detail 70)**

| Code | Image | Code | Image |
|---|---|---|---|
| 02 | Photograph | 18 | LE OVD of authorised person |
| 04 | Aadhaar (first 8 digits masked on the image) | 19 | Certificate of Incorporation |
| 05 | Passport | 20 | Registration Certificate |
| 06 | Driving License | 21-27 | MOA/AOA, Partnership Deed, Trust Deed, Board Resolution, PoA, Activity Proof 1/2 |
| 07 | Voters Identity Card | 98 | LE Other |
| 08 | NREGA Job Card | 09 | Signature (optional) |
| 35 / 36 / 37 | NPR / E-KYC / Offline Aadhaar | 97 | Death certificate |

**Other code lists**

| Field | Values |
|---|---|
| GENDER | M, F, T |
| RES_STATUS | 01 Resident · 02 NRI · 03 Foreign National · 04 PIO (pack: 01) |
| ACC_TYPE | 01 Normal · 04 OTP e-KYC · 05 Minor (02/03 not allowed for new upload). LE: blank |
| FORS_FLG / REL_PER_FATHERORSPOUSE | 01 Father · 02 Spouse |
| COMM_ADDRESS_TYPE | 01 Resident/Business · 02 Residential · 03 Business · 04 Registered Office · 05 Unspecified (only when flag N) |
| TYPE_OF_DOC | IND/RP: 01 Certified copies · 02 e-KYC UIDAI · 03 Offline verification · 04 Digital KYC · 05 Equivalent e-document · 06 V-CIP. LE: 01, 02. Pack: 01 |
| DIS_FLAG | 0 / 1 (1 → DIS_TYPE from `type_of_impairment.csv`, DIS_PERCENT, DIS_UDID_NUMBER mandatory). Pack: 0 |
| ADD_DEL_REL_PER | 01 Add (NEW) · 02 Delete (UPDATE only) |
| Update flags (…_UPDT_FLG) | Blank for NEW. UPDATE: 01 / 02 each |
| Name prefix | MR, MRS, MS (pack). MISS, DR also accepted by this spec |
| State | `masters/state_master.csv` (TN, MH, KA, KL, DL, TG, RJ, UP, WB …) |
| PIN / District | `masters/pincode_master.csv` – PIN must exist; district and state must be that PIN's |

## 5. The document cross-map (POI / POA / images / RP ID column)

This one table drives most checks. The same document always has the same codes, category text, number format, image name and RP ID column.

| Document | POI_TYPE | POI_CATEGORY | POA code | POA_CATEGORY / PERM_ADD_PROOF_DESC | Number format | Image (DOCUMENT naming) | Image doc code | RP ID column | Minor? | Expiry |
|---|---|---|---|---|---|---|---|---|---|---|
| Aadhaar | E | Proof of Possession of Aadhaar | 01 | Proof of Possession of Aadhaar | `AADHAAR` 12 digits, Verhoeff | Aadhar.jpg | 04 | REL_PER_UID | Yes | blank |
| Passport | A | Passport | 02 | Passport | `PASSPORT` A1234567 | Passport.jpg | 05 | REL_PER_PASSPORT | Yes | future date |
| Driving Licence | D | Driving Licence | 03 | Driving License | `DRIVING_LICENCE` TN7820171327481 | DrivingLicense.jpg | 06 | REL_PER_DRIVING | **No** | future date |
| Voter ID | B | Voter ID | 04 | Voters Identity Card | `VOTER_EPIC` ABC1234567 | VoterID.jpg | 07 | REL_PER_VOTER | **No** | blank |
| NREGA | F | NREGA Job Card | 05 | NREGA Job Card | `NREGA` TN + 14 digits | NREGA.jpg | 08 | REL_PER_NREGA | Yes | blank |
| NPR letter | G | National Population Register Letter | 08 | National Population Register Letter | `NPR` (format not confirmed) | – | 35 | REL_PER_POPULATION_LET | Yes | blank |
| E-KYC | H | E-KYC Authentication | 09 | E-KYC Authentication | `LAST4_AADHAAR` | – | 36 | REL_PER_EKYC | Yes | blank |
| Offline Aadhaar | I | Offline Verification of Aadhaar | 10 | Offline verification of Aadhaar | `LAST4_AADHAAR` | – | 37 | REL_PER_OFFLINE_UID | Yes | blank |
| LE Certificate of Incorporation | 02 | – | 06 | Certificate of Incorporation/Formation | CIN (D/E), LLPIN (J) | IncorporationCertificate.jpg | 19 | – | – | blank |
| LE Registration Certificate | 03 | – | 07 | Registration Certificate | REG… (B/H) or GSTIN (D/E/J) | RegistrationCertificate.jpg | 20 | – | – | blank |

- **POIA (the same document is POI and POA):** POI_NUMBER = POA_NUMBER, and POI_FILE_NAME = POA_FILE_NAME (one image). If the documents differ, both the numbers and the file names differ.
- **RP images:** prefix `RP1_` or `RP2_`, for example RP1_Photo.jpg, RP1_Passport.jpg, RP2_VoterID.jpg. All RP images sit in the parent customer's folder.
- **LEGACY image naming** (Kotak/Repco and older Cosmos packs): `{CUST}_{DocCode}_{Photo|POI|POA|POIA}_{01}.jpg`, with `RP1_` in front for RPs. This spec uses DOCUMENT naming (`config.image_naming_mode`).

**Correspondence / current address proof (Individual and Minor), when PERM_TO_COMM_FLG = N – deemed proofs only**

| COMM_ADDRESS_IDTYPE | COMM_ADDRESS_CATEGORY | COMM_ADDRESS_NUMBER | Who |
|---|---|---|---|
| 11 | Utility Bill | `EB` + 12 digits | anyone |
| 12 | Property or Municipal Tax Receipt | `PTR` + 12 digits | anyone |
| 13 | Pension Payment Order | `PPO` + 12 digits | adults aged 58 or over |
| 14 | Letter of Allotment of Accommodation | `ALT` + 12 digits | adults |

- When the flag is Y, the three COMM_ADDRESS_* columns repeat the POA: same type, same number, same category.
- A minor with flag N uses 11 or 12 only.
- **Legal Entity:** COMM_ADDRESS_IDTYPE is 06, 07 or 99, and equals POA_TYPE. COMM_ADDRESS_NUMBER equals POA_NUMBER. COMM_ADDRESS_CATEGORY is the POA master name.

**LE constitution rules**

| CONST_TYPE | Name ends with | PAN 4th letter | RP type | COI number | Reg Cert number |
|---|---|---|---|---|---|
| D Private Ltd | PRIVATE LIMITED | C | 4 Director + DIN | CIN `U#####SSYYYYPTC######` | GSTIN |
| E Public Ltd | LIMITED | C | 4 Director + DIN | CIN `L#####SSYYYYPLC######` | GSTIN |
| J LLP | LLP | F | 8 Partner | LLPIN `AAA1234` | GSTIN |
| B Partnership | AND COMPANY | F | 8 Partner | – | `REG` + state + year + 6 digits |
| H Trust | TRUST | T | 7 Trustee | – | `REG` + state + year + 6 digits |

GSTIN (TIN column):

- characters 1-2 are the GST state code of PERM_ADD_STATE (TN 33, MH 27, KA 29, KL 32, DL 07, TG 36, RJ 08, UP 09, WB 19);
- characters 3-12 are the entity PAN;
- the check digit must be valid;
- TIN_COUNTRY is IN.

## 6. Where to verify each ID number

| What | CSV column(s) | Format rule | Also check | DB (iSolve) | CERSAI record |
|---|---|---|---|---|---|
| Applicant PAN | PAN_OR_FORM60 (IND col 84 / LE col 39) | `PAN_OR_FORM60`; IND 4th letter P; LE by constitution | PAN on nothing else (PAN is not an upload image) | CKYC_FIN_DATA / _LE | Detail 20 f53 |
| Applicant POI | POI_TYPE + POI_NUMBER + POI_CATEGORY + POI_FILE_NAME (cols 64-68) | by POI_TYPE (section 5) | Number printed on the POI image (Aadhaar image shows XXXX XXXX + last 4) | CKYC_FIN_DATA | Detail 30 (type + number), Detail 70 (image) |
| Applicant POA | POA_TYPE + POA_NUMBER + POA_CATEGORY + POA_FILE_NAME + PERM_ADD_PROOF (+ _DESC) | by POA_TYPE | POA image; PERM_ADD_PROOF = POA_TYPE | CKYC_FIN_DATA | Detail 20 f69, Detail 30, Detail 70 |
| Correspondence proof | COMM_ADDRESS_IDTYPE / _NUMBER / _CATEGORY (IND cols 85-87; LE cols 12, 13, 90) | Y → = POA; N → deemed 11-14 (IND/Minor) | No image for deemed proofs | CKYC_FIN_DATA / _LE | Detail 20 f81 |
| RP PAN | REL_PER_PANORFORM60 | PAN, 4th letter P | – | CKYC_FIN_DATA_RP / _LE_RP | Detail 40 f34 |
| RP identity numbers | REL_PER_UID, _VOTER, _NREGA, _PASSPORT, _DRIVING, _POPULATION_LET, _EKYC, _OFFLINE_UID (Minor cols 134-141, LE cols 125-132) | at least one filled; the documents of REL_PER_ADD_PROF, of the RP POI image and (flag N) of REL_PER_CURR_ADD_PROOF each have their number in the matching column | RP1_/RP2_ document images | *_RP | Detail 40 f35-43 |
| RP permanent proof | REL_PER_ADD_PROF + REL_PER_PER_ADDRESS_NAME | POA master individual codes; image = RP{n}_<doc> | – | *_RP | Detail 40 f55 / f45 |
| RP current proof (flag N) | REL_PER_CURR_ADD_PROOF (Minor col 163, LE col 154) + its RP ID column | different document from the permanent proof; its number in the matching RP ID column | – | *_RP | Detail 40 f65 |
| DIN | REL_PER_DIN_NUMBER | 8 digits, only when REL_TYPE = 4 | – | CKYC_FIN_DATA_LE_RP | Detail 40 f85 |
| CIN / LLPIN / Reg no | POI_NUMBER / POA_NUMBER (LE) | section 5 LE table | Certificate image | CKYC_FIN_DATA_LE | Detail 30 |
| GSTIN | TIN | `GSTIN` + embedded PAN + state code | – | CKYC_FIN_DATA_LE | Detail 20 f51 |
| Images | *_FILE_NAME / REL_PER_*_NAME | DOCUMENT naming | `Common_Images/<CUST>/` + IMAGE_STATUS_SUMMARY.csv | – | Detail 70 |

## 7. Persona rules in one place

**Individual (01_Individual)**

- Codes: ACC_TYPE 01, MINOR N, CONST_TYPE 01, age 18 or over.
- Documents: the POI and POA pool is Aadhaar, Passport, DL, Voter ID and NREGA. The pack alternates POIA (one document) and split POI/POA.
- Cosmos rules: PAN only (no FORM60); PERM_ADD_TYPE is always blank; the RP columns are not in the header.
- Address flag: roughly 60% Y and 40% N. Y means COMM_* is an exact copy of PERM_* and COMM_ADDRESS_TYPE is blank. N means a different address, COMM_ADDRESS_TYPE 01-03, and a deemed proof.

**Minor with RP (02_Minor_With_RP)**

- Minor: ACC_TYPE 05, MINOR Y, age 1-17. The documents never include DL or Voter ID. PAN or FORM60.
- Guardian on the same row: REL_TYPE 1, ADD_DEL_REL_PER 01, aged 18 or over, and is the father or mother (same surname). The guardian's permanent address is the minor's permanent address.
- Guardian's IDs: REL_PER_UID is always filled, plus the POI and POA document numbers in their RP columns.
- Guardian's current address when REL_PER_SAMEASPERM_ADD_FLAG = N: a different address, and REL_PER_CURR_ADD_PROOF is a different document with its ID in the matching column.
- Images: RP1_Photo.jpg, RP1_<POI doc>.jpg, RP1_<POA doc>.jpg.

**Minor without RP (03_Minor_Without_RP)**

- Age 11-17 (user rule). Every RP column is blank except CUST_ID, which equals CUSTOMER_REFERENCE_NUMBER.
- See the open question in section 8.

**Legal Entity multi-RP (04_Legal_Entity_Multi_RP)**

- 20 companies per CSV, each with 2 RPs:
  - row 1 = entity + RP1;
  - row 2 = RP2 continuation, with columns 1-91 blank and CUST_ID set to the entity.
- NUM_OF_REL_PER = 2. NUM_OF_ID_DET = 1. NUM_OF_IMG = 01 for POIA, otherwise 02.
- No entity photograph. The entity POI is 02 or 03; the POA is 06 or 07 and equals PERM_ADD_PROOF.
- RP type follows the constitution table in section 5. A Director needs a DIN.
- ACC_TYPE, ACC_HOLDER_TYPE and ACC_HOLDER_TYPE_FLAG are blank.

**Every RP (guardian, director, partner, trustee)**

- Mother name is always filled.
- DIS_FLAG 0. TYPE_OF_DOC 01. KYC block and ORG fields filled.
- REL_PER_CURR_ADD_* is a copy of the permanent address when the flag is Y.
- System columns are blank: FIN_RP_CREATED_TIME, REL_PER_CONST, the *_DOC_ID columns, FIN_FIN_ID, FIN_API_RP_ID, the *_DCM_CODE columns and REL_PER_CURR_ADDRESS_NAME.

## 8. Conflicts between sources, open questions and findings on the 30-Sep packs

| # | Topic | What the sources say | This spec uses | Action |
|---|---|---|---|---|
| C1 | Image file-name length | iFlow sheet: PHOTO/POI/POA_FILE_NAME max **10**. CERSAI Detail 40/70 and the length guide: **50** | 50 (names like IncorporationCertificate.jpg are 28 chars) | Confirm with the Cosmos dev team that 10 is a typo |
| C2 | POI_CATEGORY / POA_CATEGORY / COMM_ADDRESS_CATEGORY | iFlow: max 20, "master". Length guide / iSolve DB: COMM_ADDRESS_CATEGORY VARCHAR2(**2**) (a code). Cosmos team sample + packs: full **text** (up to 36 chars, e.g. "Property or Municipal Tax Receipt") | Text, max 50 | Confirm text vs code with Cosmos dev |
| C3 | LE POI/POA/COMM number length | CERSAI Identity Code 02/03: **60**. iSolve DB / iFlow: VARCHAR2(**20**). A CIN is 21 chars | 60, plus WARN `DB-LEN` when > 20 | In packs 1-3, 780 of 1,800 LE records (210 positive, 210 DVS, 360 image processing) carry a 21-char CIN in POI_NUMBER / POA_NUMBER / COMM_ADDRESS_NUMBER. Confirm the DB column size before the UAT run |
| C4 | Aadhaar in CSV | CERSAI keeps only the last 4 digits (max 12). Packs send the full 12-digit Verhoeff number; images show it masked | Full 12 digits, Verhoeff valid, no X/*/spaces | – |
| C5 | DIS_FLAG values | CERSAI 0/1; iSolve RP guide says Y/N | 0/1 (the pack uses 0) | – |
| C6 | Fields iFlow marks "Blank" (MARITAL_STATUS, NATIONALITY, OCC_TYPE, OTHER_JURI_FLG, PERM_ADD_PROOF_DESC, RES_STATUS) | Not in the CERSAI upload, or marked blank in iFlow | Optional; the pack fills them (user rule "fill all fields"). OCC_TYPE / OTHER_JURI_FLG have no master in V1.3 | – |
| C7 | APPL_LAST_NAME | iFlow: optional. CERSAI: mandatory for constitution 01 | Mandatory | – |
| C8 | Minor without guardian | CERSAI checklist: ACC_TYPE 05 needs REL_TYPE 1 | Folder kept as a scenario (INFO rule MINRP-02) | Confirm the expected outcome (accept or DVS) with Cosmos |
| C9 | NUM_OF_IMG (LE) | CERSAI: number of Detail-70 image records | Pack: entity images only (01/02) | Confirm whether RP images count |
| C10 | NPR letter (08 / G) | No number format in CERSAI | Generic alphanumeric ≤ 20; not used in the packs | Share the format if 08 is needed |
| C11 | CERSAI image naming | CERSAI Detail 70: `<fi_reference_number>_<any>.<ext>` | Cosmos iFlow DOCUMENT names inside `Common_Images/<CUST>/` (the iFlow app builds the CERSAI names) | – |
| F1 | **Finding:** duplicate EMAIL_ID2 | Pack 1: `meena.natarajan.0354@gmail.com` (COSI30090354 and COSM30090354) and `bhuvana.annamalai.0770@gmail.com` (COSI30090770 and COSM30090770) | UNIQ-02 ERROR | Two secondary e-mails need a fix (optional field) |
| F2 | **Validator result on 30-Sep packs 1-3** | POSITIVE: 2,400/2,400 records PASS (only F1 is reported, as a cross-pack duplicate). IMAGE_PROCESSING: 2,400/2,400 PASS, images match IMAGE_STATUS_SUMMARY. DVS: 2,400/2,400 FAIL, and the planted SUMMARY.csv defect is caught for every record. 898 generated test cases self-check 898/898 | – | – |

## 9. Cross-field rule catalogue (machine copy: `cross_field_rules` in the JSON)

| Rule | Personas | Severity | Rule | Where to verify | Negative example | Source |
|---|---|---|---|---|---|---|
| **FILE-01** | IND, MIN, LE | ERROR | CSV is pipe (\|) delimited, UTF-8 (BOM allowed), first row = exact header of the persona (IND 99 / Minor 198 / LE 189 columns, same order), every data row has the same column count. | Open the CSV as text; count '\|' per row; compare header with personas.<P>.header | Row with 98 pipes -> 'Number of pipe separated records do not match' | CERSAI + Cosmos team |
| **FILE-02** | IND, MIN, LE | ERROR | File name CKYC_DATA_IN_DDMMYYYY_NN.csv for Individual + Minor, CKYC_DATA_LE_DDMMYYYY_NN.csv for Legal Entity. Never mix IN and LE rows, never mix NEW (01) and UPDATE (03) rows in one file. | File name regex + APPL_TYPE distinct values per file | CKYC_DATA_LE_30092026_01.csv containing an Individual row | Cosmos team mail (File_Upload_Name_Format.txt) |
| **FILE-03** | IND, MIN, LE | WARN | Series number NN is continuous across the packs of one day (IN 01..270, LE 01..90 for the 30-Sep set). Positive 20 records per IND/Minor CSV; LE 20 companies x 2 RP = 40 rows per CSV. | List CSV names across packs; count records | Pack 2 restarting at _01 | Cosmos pack convention |
| **HIER-01** | IND, MIN, LE | ERROR | ORG_CODE = config.ORG_CODE (IN0308), ORG_NAME = config.ORG_NAME, BRID = config.BRID (001). On every RP row REL_PER_ORG_CODE = ORG_CODE and REL_PER_ORG_NAME = ORG_NAME - also in DVS. | CSV + DB CKYC_FIN_DATA.ORG_CODE / *_RP.REL_PER_ORG_CODE | REL_PER_ORG_CODE blank | CERSAI + Cosmos hierarchy |
| **UNIQ-01** | IND, MIN, LE | ERROR | CUSTOMER_REFERENCE_NUMBER unique across ALL packs and folders; REL_PER_CUST_ID unique; each Common_Images folder name = one customer. | Collect all values across packs; duplicates = failure | Same COSI30090001 in Pack1 and Pack2 | CERSAI + user rule |
| **UNIQ-02** | IND, MIN, LE | ERROR | PAN (not FORM60), Aadhaar, Passport, Voter, DL, NREGA, DIN, CIN/LLPIN, GSTIN, deemed-proof numbers, mobile and e-mail are unique per person across all packs. The same number may repeat only inside one person's own record (POI = POA = COMM proof when it is the same document). | Map value -> set of owners (customer or RP id); >1 owner = failure | Two different customers with PAN ABCPK1234L | User rule (unique test data) |
| **NAME-01** | IND, MIN | ERROR | At least one of father / spouse / mother name is given. FORS_FLG 01 -> father's name in FATHERORSPOUSE_*; 02 -> spouse's name. When FORS_FLG is set, prefix + first name are mandatory. | CSV name columns | FORS_FLG=01 with FATHERORSPOUSE_FIRST_NAME blank | CERSAI Detail(20) 30-39 |
| **NAME-02** | IND, MIN, LE | WARN | Prefix matches gender: MR -> M; MRS/MS/MISS -> F. FORS_FLG 02 (spouse) only with MRS. For RP: REL_PER_FATHERORSPOUSE 02 only for female RP, and then maiden name is filled. | CSV | APPL_NAME_PREFIX=MR with GENDER=F | Cosmos pack convention |
| **NAME-03** | MIN, LE | WARN | RP mother name (prefix + first name) always filled on every RP row. | CSV REL_PER_MOTHER_* | Guardian row without mother name | User rule |
| **DATE-01** | IND, MIN, LE | ERROR | All dates DD-MM-YYYY and real calendar dates. DOB / DATE_OF_INC / DECL_DATETIME / KYC_VERIFY_DATETIME (and RP equivalents) are not future dates and not equal to the upload date. DOB < DECL_DATETIME. | CSV date columns vs upload date | DOB=15-10-2027; KYC_VERIFY_DATETIME=2026/09/03 | CERSAI Detail(20) 45/102/104, Detail(40) 23/75/77 |
| **DATE-02** | IND, MIN | WARN | POI/POA expiry date filled only for Passport (A/02) and Driving Licence (D/03) and must be later than KYC_VERIFY_DATETIME. Blank for Aadhaar, Voter, NREGA. | CSV POI_EXPIRY_DATE / POA_EXPIRY_DATE | Passport expiry 01-01-2020 | Cosmos pack convention |
| **DATE-03** | LE | ERROR | DATE_OF_COMMENCEMENT >= DATE_OF_INC and not future. | CSV | Commencement before incorporation | CERSAI Detail(20) 47 |
| **AGE-01** | IND | ERROR | Individual (ACC_TYPE 01, MINOR N): age at DECL_DATETIME >= 18. | DOB vs DECL_DATETIME | Individual aged 16 with MINOR=N | CERSAI account type rules |
| **AGE-02** | MIN | ERROR | Minor: ACC_TYPE 05, MINOR Y, age < 18 at DECL_DATETIME. Minor With RP pack: age 1-17. Minor Without RP pack: age 11-17 (user rule). | DOB vs DECL_DATETIME + folder | MINOR=N for a 12 year old | CERSAI account type 05 |
| **AGE-03** | MIN, LE | ERROR | Every related person (guardian, director, partner, trustee) is >= 18 at REL_PER_DECL_DATETIME. | REL_PER_DOB | REL_PER_DOB=10-05-2014 | CERSAI / RBI |
| **ADDR-01** | IND, MIN, LE | ERROR | Every address block (permanent, correspondence/local, RP permanent, RP current): PIN exists in masters/pincode_master.csv, district = master district of that PIN, state = master state of that PIN (case-insensitive). Country IN. | masters/pincode_master.csv | PERM_ADD_PIN=999999; PIN 600002 with state MH | CERSAI Pincode / District master |
| **ADDR-02** | IND, MIN, LE | ERROR | PERM_TO_COMM_FLG = Y -> correspondence/local address lines 1-3, city, district, state, country, PIN are an exact copy of the permanent address and COMM_ADDRESS_TYPE is blank. Flag = N -> a different address (line 1 or PIN differs) with all mandatory lines filled and (IND/Minor) COMM_ADDRESS_TYPE in 01-05. | Compare PERM_* vs COMM_* column by column | Flag Y but COMM_Add_Pin differs; flag N but COMM equals PERM | User rule + iFlow |
| **ADDR-03** | MIN, LE | ERROR | REL_PER_SAMEASPERM_ADD_FLAG = Y -> REL_PER_CURR_ADD_* is an exact copy of REL_PER_ADD_* and REL_PER_CURR_ADD_PROOF = REL_PER_ADD_PROF. Flag = N -> a different current address, all mandatory current lines filled, REL_PER_CURR_ADD_PROOF filled. | Compare REL_PER_ADD_* vs REL_PER_CURR_ADD_* | Flag N with current address = permanent | User rule + CERSAI Detail(40) 56-65 |
| **ADDR-04** | MIN | WARN | Guardian lives with the minor: REL_PER_ADD_LINE1..PIN/STATE = minor's PERM_ADD_LINE1..PIN/STATE. | CSV | Guardian permanent PIN different from minor's | Cosmos pack convention |
| **POI-01** | IND, MIN | ERROR | POI_TYPE, POI_CATEGORY, POI_NUMBER and POI_FILE_NAME describe the same document (doc_map): e.g. E -> 'Proof of Possession of Aadhaar' -> 12-digit Verhoeff Aadhaar -> Aadhar.jpg. POI_NUMBER format = number_format_by_code.POI_TYPE. | CSV + image text | POI_TYPE=A with POI_FILE_NAME=Aadhar.jpg; POI_TYPE=Z | CERSAI Identity Code + Document Master |
| **POA-01** | IND, MIN | ERROR | POA_TYPE, POA_CATEGORY, POA_NUMBER, POA_FILE_NAME describe the same document. PERM_ADD_PROOF = POA_TYPE. PERM_ADD_PROOF_DESC (if filled) = POA master name. | CSV | PERM_ADD_PROOF=02 but POA_TYPE=01 | CERSAI POA master + Document Master |
| **POIA-01** | IND, MIN, LE | ERROR | If POI and POA are the same document (E/01, A/02, D/03, B/04, F/05; LE 02/06, 03/07) then POI_NUMBER = POA_NUMBER and POI_FILE_NAME = POA_FILE_NAME (one image). If different documents then numbers and file names differ. | CSV + Common_Images folder | POI E + POA 01 with two different Aadhaar numbers | CERSAI image rules |
| **MINDOC-01** | MIN | ERROR | A minor never uses Voter ID (POI B / POA 04) or Driving Licence (POI D / POA 03) for POI, POA or correspondence proof. Minor pool = Aadhaar, Passport, NREGA. | POI_TYPE / POA_TYPE / COMM_ADDRESS_IDTYPE | Minor with POI_TYPE=B | User rule (18+ documents) |
| **PAN-01** | IND, MIN, LE | ERROR | PAN_OR_FORM60 / REL_PER_PANORFORM60 = valid PAN pattern or FORM60. Individual/RP PAN 4th letter P. LE 4th letter by constitution (D/E C, B/J F, H T). | CSV | 12345ABCD1 | CERSAI Detail(20) 53 / Detail(40) 34 |
| **PAN-02** | IND, MIN, LE | WARN | PAN 5th letter = first letter of the holder's last name (Individual/RP) or of the entity name (LE). | CSV | Last name SHAH with PAN ABCPK1234L | Cosmos pack convention (ITD structure) |
| **COMM-01** | IND, MIN | ERROR | COMM_ADDRESS_CATEGORY / COMM_ADDRESS_IDTYPE / COMM_ADDRESS_NUMBER always filled. Flag Y -> same document as POA (IDTYPE = POA_TYPE, NUMBER = POA_NUMBER, CATEGORY = POA_CATEGORY). Flag N -> NEVER an OVD: deemed proof 11 Utility Bill / 12 Property or Municipal Tax Receipt / 13 Pension Payment Order (age >= 58 only) / 14 Letter of Allotment of Accommodation, number EB/PTR/PPO/ALT + 12 digits, category = deemed_map name. Minor flag N -> 11 or 12 only. | CSV | Flag N with COMM_ADDRESS_IDTYPE=02 (Passport) | User rule 30-09-2026 |
| **COMM-02** | LE | ERROR | LE COMM_ADDRESS_IDTYPE in 06/07/99 only and (pack) = POA_TYPE; COMM_ADDRESS_NUMBER = POA_NUMBER; COMM_ADDRESS_CATEGORY = POA master name of POA_TYPE. | CSV cols 12, 13, 90 | LE with COMM_ADDRESS_IDTYPE=11 | CERSAI POA master (LE) + user rule |
| **RPID-01** | MIN, LE | ERROR | At least one RP ID column is filled (REL_PER_UID, _VOTER, _NREGA, _PASSPORT, _DRIVING, _POPULATION_LET, _EKYC, _OFFLINE_UID). Each filled column must match its format. | CSV + DB *_RP | All 8 RP ID columns blank | CERSAI Detail(40) 35-43 |
| **RPID-02** | MIN, LE | ERROR | The document of REL_PER_ADD_PROF, the document shown in REL_PER_POI_NAME and (flag N) the document of REL_PER_CURR_ADD_PROOF each have their number in the matching RP ID column (01 UID, 02 PASSPORT, 03 DRIVING, 04 VOTER, 05 NREGA, 08 POPULATION_LET, 09 EKYC, 10 OFFLINE_UID). | CSV | REL_PER_CURR_ADD_PROOF=03 with REL_PER_DRIVING blank | CERSAI Detail(40) 55/65 ('ID number needs to be provided in fields 35 to 43') |
| **RPID-03** | MIN, LE | WARN | RP flag N: REL_PER_CURR_ADD_PROOF is a different document from REL_PER_ADD_PROF and from the RP POI document (different address -> different proof). | CSV | Flag N with REL_PER_CURR_ADD_PROOF = REL_PER_ADD_PROF | User rule 30-09-2026 |
| **RPID-04** | MIN, LE | ERROR | REL_PER_EKYC / REL_PER_OFFLINE_UID = exactly 4 digits = last 4 digits of REL_PER_UID. | CSV | REL_PER_EKYC=12#X | CERSAI Identity Code H/I remarks |
| **RPIMG-01** | MIN, LE | ERROR | REL_PER_PHOTO_NAME = RP{n}_Photo.jpg; REL_PER_POI_NAME = RP{n}_<doc image> of the RP POI document; REL_PER_PER_ADDRESS_NAME = RP{n}_<doc image> of REL_PER_ADD_PROF. n = RP order inside the customer (1, 2). | CSV + Common_Images/<CUST_ID>/ | REL_PER_ADD_PROF=02 but REL_PER_PER_ADDRESS_NAME=RP1_Aadhar.jpg | CERSAI Detail(40) 44-46 + Cosmos naming |
| **MINRP-01** | MIN | ERROR | 02_Minor_With_RP: REL_TYPE=1, ADD_DEL_REL_PER=01, full guardian block. 03_Minor_Without_RP: every RP column (DECEASED_DATE..REL_PER_CURR_ADDRESS_NAME) blank except CUST_ID. | CSV | Minor with RP with REL_TYPE=4 | CERSAI + folder rule |
| **MINRP-02** | MIN | INFO | CERSAI expects ACC_TYPE 05 records to carry a Guardian (REL_TYPE 1). The 03_Minor_Without_RP folder is a deliberate scenario (minor 11-17 operating own account) - confirm the expected outcome with the Cosmos team. | Cosmos team |  | CERSAI quick checklist |
| **CUST-01** | MIN, LE | ERROR | CUST_ID = CUSTOMER_REFERENCE_NUMBER of the parent (minor / entity). LE continuation row: CUST_ID = the entity row's CUSTOMER_REFERENCE_NUMBER. | CSV | CUST_ID not found in any entity row | iSolve mapping guide |
| **LE-01** | LE | ERROR | Multi-RP: row 1 = entity + RP1; rows 2..N = continuation rows where every column before REL_TYPE is BLANK and only the RP block is filled, linked by CUST_ID. NUM_OF_REL_PER = number of rows with that CUST_ID. | CSV | Entity name repeated on RP2 row | User rule 29-09-2026 |
| **LE-02** | LE | ERROR | REL_TYPE by constitution: D/E -> 4 Director (REL_PER_DIN_NUMBER 8 digits mandatory), B/J -> 8 Partner, H -> 7 Trustee. DIN blank when REL_TYPE != 4. | CSV | Director with 7-digit DIN | CERSAI Related Person Type + Constitution rules |
| **LE-03** | LE | ERROR | POI_TYPE 02 (COI) or 03 (Registration Certificate) - never an Individual code. POA_TYPE 06/07/99 = PERM_ADD_PROOF. COI number: CIN for D/E (U..PTC for D, L..PLC for E), LLPIN for J; B/H have no COI (Registration Certificate only). Registration Certificate number: REG format (B/H) or GSTIN (D/E/J). | CSV + image | LE with POI_TYPE=E | CERSAI Identity Code (LE) + POA master (LE) |
| **LE-04** | LE | ERROR | TIN (GSTIN) checksum valid, chars 3-12 = PAN_OR_FORM60, first 2 digits = GST state code of PERM_ADD_STATE, TIN_COUNTRY = IN. | CSV | 33ABCDE1234F1Z@ | GST rules |
| **LE-05** | LE | WARN | LE contact duplicates are equal: RES_TEL_CODE = RESIDENCE_TELEPHONE_NO_STD_CODE, RES_TEL_NUM = RESIDENCE_TELEPHONE_NO, OFF_TEL_* = OFFICE_*, MOB_ISD_CODE/MOB_NUM = MOBILE_NO_ISD_CODE/MOBILE_NO, FAX_* = FAX_NO_*. NUM_OF_IMG = 01 when POI file = POA file else 02. Entity name suffix matches constitution. | CSV | MOB_NUM different from MOBILE_NO | Cosmos pack convention |
| **LE-06** | LE | ERROR | Legal entity never has a photograph: no Photo.jpg in the entity folder. Only RPs have RP{n}_Photo.jpg. | Common_Images/<CUST>/ | Photo.jpg present for COSL... | CERSAI image rules |
| **CONTACT-01** | IND, MIN, LE | ERROR | STD code and telephone number both filled or both blank (residence, office, fax). ISD code and mobile both filled or both blank; ISD 91 -> mobile 10 digits starting 6-9. | CSV | MOBILE_NO=987654321 (9 digits) | CERSAI Detail(20) 92-99 |
| **IMG-01** | IND, MIN, LE | ERROR | Every customer in every CSV (all 3 folders) has a folder Common_Images/<CUSTOMER_REFERENCE_NUMBER>/ (for LE the entity CUSTOMER_REFERENCE_NUMBER; RP images sit in the same folder). | Common_Images/ | COSM24090801 folder missing | User rule |
| **IMG-02** | IND, MIN, LE | ERROR | 01_POSITIVE and 02_DVS: every file named in PHOTO_FILE_NAME / POI_FILE_NAME / POA_FILE_NAME / REL_PER_PHOTO_NAME / REL_PER_POI_NAME / REL_PER_PER_ADDRESS_NAME exists in the customer folder, and the folder has no extra files. 03_IMAGE_PROCESSING: file exists exactly when IMAGE_STATUS_SUMMARY.csv says AVAILABLE and is absent when it says MISSING. | Common_Images + IMAGE_STATUS_SUMMARY.csv | Aadhar.jpg named in CSV but not in folder (positive) | CERSAI Detail(70) + user rule |
| **IMG-03** | IND, MIN, LE | ERROR | Image file name = document of its column: Photo.jpg (02), Aadhar.jpg (04), Passport.jpg (05), DrivingLicense.jpg (06), VoterID.jpg (07), NREGA.jpg (08), IncorporationCertificate.jpg (19), RegistrationCertificate.jpg (20); RP files prefixed RP1_/RP2_. LEGACY naming alternative: {CUST}_{DocCode}_{Photo\|POI\|POA\|POIA}_{page}.jpg. | CSV vs doc_map | POI_TYPE=D with POI_FILE_NAME=Passport.jpg | Cosmos DOCUMENT naming (user rule) / Document Master |
| **DVS-01** | IND, MIN, LE | INFO | 02_DVS: every record carries at least one deliberate defect; SUMMARY.csv column DVS_DEFECT_FIELD names it (plus RP current-address proof ID in wrong format for RP flag N rows). Automation must see the record rejected/held with an error on that field. Everything else in the record is valid. | SUMMARY.csv DVS_DEFECT_FIELD vs DVS Maker / CERSAI error |  | Pack design |
| **IP-01** | IND, MIN, LE | INFO | 03_IMAGE_PROCESSING: CSV data fully valid (same rules as positive); only images are partially missing (photo / POI / POA each AVAILABLE or MISSING, incl. RP images). Expected: record routed to image-upload / image-processing queue for the MISSING files only. | IMAGE_STATUS_SUMMARY.csv |  | Pack design |

## 10. Value formats (machine copy: `formats` in the JSON)

| Format | Regex / master | Meaning | Valid example | Invalid example |
|---|---|---|---|---|
| `CODE2` | `^\d{2}$` | 2-digit code |  |  |
| `NAME` | `^[A-Za-z]+( [A-Za-z]+)*$` | Letters only, single space between words. No digits/special characters (CERSAI allows single quote ' in father/spouse name only). | RAJESH KUMAR | RAJ@SH |
| `PLACE` | `^[A-Za-z]+( [A-Za-z]+)*$` | Plain place/branch name: letters, single spaces, no digits, no double space. | NEW DELHI | CHN02 |
| `ENTITY_NAME` | `^[A-Za-z0-9&().'-]+( [A-Za-z0-9&().'-]+)*$` | Entity name: letters/digits and & ( ) . ' - with single spaces. | KAVERI TEXTILES PRIVATE LIMITED |  |
| `ADDRESS_LINE` | `^[A-Za-z0-9]+( [A-Za-z0-9]+)*$` | Address line: letters/digits, single spaces, no special characters (iFlow rule). Max 55. | NO 183 CHURCH STREET | NO#183, CHURCH ST |
| `CITY` | `^[A-Za-z]+( [A-Za-z]+)*$` | City/Town/Village: letters and single spaces. Max 50. | CHENNAI | CHENNAI-2 |
| `DATE` | `^(0[1-9]\|[12]\d\|3[01])-(0[1-9]\|1[0-2])-(19\|20)\d{2}$` | DD-MM-YYYY and a real calendar date. | 02-09-2026 | 2026/09/02 |
| `PIN` | `^[1-9]\d{5}$` | 6 digits, must exist in CERSAI Pincode master; district + state must match that PIN. | 600002 | 60000A |
| `STATE` | `master state_master.csv` | 2-char CERSAI State Master code (TN, MH, KA ...). | TN | TAMILNADU |
| `COUNTRY` | `master country_code_iso3166.csv` | ISO 3166 2-char code. India = IN. | IN | IND |
| `PAN` | `^[A-Z]{5}\d{4}[A-Z]$` | PAN = 5 letters + 4 digits + 1 letter. 4th letter = holder type (P person, C company, F firm/LLP, T trust, H HUF, A AOP, B BOI, G govt, J AJP, L local authority). | ABCPK1234L | 12345ABCD1 |
| `PAN_OR_FORM60` | `^([A-Z]{5}\d{4}[A-Z]\|FORM60)$` | Either a valid PAN or the literal text FORM60. | FORM60 | FORM 60 |
| `AADHAAR` | `^[2-9]\d{11}$ + verhoeff checksum` | 12 digits, first digit 2-9, Verhoeff checksum valid. No X/x/*, no spaces, no masking in the CSV (images show it masked XXXX XXXX 1234). CERSAI stores only last 4. | 234567890124 | XXXXXXXX1234 |
| `PASSPORT` | `^[A-Z]\d{7}$` | Indian passport: 1 letter + 7 digits (CERSAI max 20). | M2364382 | M23@4382 |
| `VOTER_EPIC` | `^[A-Z]{3}\d{7}$` | Voter ID (EPIC): 3 letters + 7 digits (CERSAI max 20). | SKT2102993 | SK2102993 |
| `DRIVING_LICENCE` | `^[A-Z]{2}\d{2}(19\|20)\d{2}\d{7}$` | DL: state code(2) + RTO(2) + issue year(4) + 7 digits = 15 chars (CERSAI max 20). | TN7820171327481 | TN78@0171327481 |
| `NREGA` | `^[A-Z]{2}\d{14}$` | NREGA job card (pack format): state code(2) + 14 digits. CERSAI max 40 (applicant) / 20 (RP column). | TN17260081128167 | TN17@60081128167 |
| `NPR` | `^[A-Z0-9]{1,20}$` | NPR letter reference - format NOT confirmed by CERSAI (max 20 alphanumeric). Not used in current packs. |  |  |
| `LAST4_AADHAAR` | `^\d{4}$` | Exactly 4 digits = last 4 digits of the person's Aadhaar (E-KYC / Offline Aadhaar). | 1234 | 12#X |
| `DIN` | `^\d{8}$` | Director Identification Number: exactly 8 digits. | 04363701 | 4363701 |
| `CIN` | `^[LU]\d{5}[A-Z]{2}(19\|20)\d{2}(PLC\|PTC)\d{6}$` | CIN (21): L/U + 5-digit industry + state(2) + year(4) + PLC/PTC + 6 digits. Public Ltd (E) = L..PLC, Private Ltd (D) = U..PTC. | U92727TN2008PTC863802 | U9272TN2008PTC863802 |
| `LLPIN` | `^[A-Z]{3}-?\d{4}$` | LLP Identification Number: 3 letters + 4 digits (AAA-1234 / AAA1234). | ABC1234 | AB12345 |
| `GSTIN` | `^\d{2}[A-Z]{5}\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$ + gstin checksum` | GSTIN (15): GST state code(2) + PAN(10) + entity no + Z + checksum. Chars 3-12 must equal PAN_OR_FORM60, first 2 = GST code of PERM_ADD_STATE. | 33PLOCS9829E5ZS | 33PLOCS9829E5@@ |
| `REG_CERT` | `^(REG[A-Z]{2}(19\|20)\d{2}\d{6}\|\d{2}[A-Z]{5}\d{4}[A-Z][1-9A-Z]Z[0-9A-Z])$` | Registration Certificate number: pack format REG + state(2) + year(4) + 6 digits (firms/trusts) OR the GSTIN (companies/LLPs). | REGTN2015335843 | REG-TN-2015 |
| `DEEMED_11` | `^EB\d{12}$` | Utility bill consumer/bill no (pack format EB + 12 digits). | EB123456789012 |  |
| `DEEMED_12` | `^PTR\d{12}$` | Property / municipal tax receipt no (pack format PTR + 12 digits). | PTR123456789012 |  |
| `DEEMED_13` | `^PPO\d{12}$` | Pension Payment Order no (pack format PPO + 12 digits). | PPO123456789012 |  |
| `DEEMED_14` | `^ALT\d{12}$` | Allotment letter no (pack format ALT + 12 digits). | ALT123456789012 |  |
| `MOBILE_IN` | `^[6-9]\d{9}$` | Indian mobile: 10 digits starting 6-9 (when ISD = 91). Else up to 20 digits. | 9876543210 | 987654321 |
| `ISD` | `^\d{1,3}$` | ISD code digits only (India = 91), no + sign. | 91 | +91 |
| `STD` | `^\d{2,4}$` | STD code 2-4 digits, no leading 0. | 44 | 044- |
| `LANDLINE` | `^\d{6,8}$` | Landline number 6-8 digits (max 10). | 22002532 | 2200-2532 |
| `EMAIL` | `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | Valid e-mail. | cosi30090001@cosmos.in | cosi30090001@ |
| `IMAGE_FILE` | `^[A-Za-z0-9_]+\.(jpg\|jpeg\|pdf\|tif\|tiff\|JPG\|JPEG\|PDF\|TIF\|TIFF)$` | File name only (no folder), extension jpg/jpeg/pdf/tif/tiff; must exist in Common_Images/<CUSTOMER_REFERENCE_NUMBER>/ unless the image-processing summary says MISSING. | Aadhar.jpg | C:\img\Aadhar.jpg |
| `CKYC_NO_IND` | `^[1-6]\d{13}$` | 14-digit CKYC number, first digit 1-6 = Individual (UPDATE only). |  |  |
| `CKYC_NO_LE` | `^[7-9]\d{13}$` | 14-digit CKYC number, first digit 7-9 = Legal Entity (UPDATE only). |  |  |
| `UDID` | `^[A-Z]{2}\d{16}$` | UDID: 2 letters + 16 digits. |  |  |
| `PERCENT` | `^(100\|[1-9]?\d)$` | 0-100. |  |  |
| `ALNUM` | `^[A-Za-z0-9]+$` | Letters/digits only. |  |  |
| `DESIGNATION` | `^[A-Za-z]+( [A-Za-z]+)*$` | Designation text (letters/single spaces). | BRANCH HEAD | 99999 |
| `NUM` | `^\d+$` | Digits only. |  |  |
