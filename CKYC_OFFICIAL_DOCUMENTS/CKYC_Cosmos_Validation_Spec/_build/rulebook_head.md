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
