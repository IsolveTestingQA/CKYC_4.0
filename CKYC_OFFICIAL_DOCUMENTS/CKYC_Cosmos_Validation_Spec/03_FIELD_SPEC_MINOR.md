# Minor - field spec, 198 columns

Folders `02_Minor_With_RP` (guardian REL_TYPE 1 on the same row) and `03_Minor_Without_RP` (RP block blank). ACC_TYPE 05, MINOR Y, age < 18. Columns 1-99 are the Individual columns (Minor comm-address headers use the short form COMM_Add_Line1 ... COMM_Add_Pin); columns 100-198 are the related-person block.

Columns: **198** · file prefix `CKYC_DATA_IN_DDMMYYYY_NN.csv` · pipe `|` delimited · UTF-8 (BOM).

Status: **M** mandatory · CM conditional mandatory (see *When*) · O optional · BLANK (new) = must be blank for a NEW upload · SYSTEM = system column, keep blank.
Pack fill = what the Cosmos positive pack does (ALWAYS / BLANK / condition). Formats: `01_VALIDATION_RULES.md` section 10 (regex, examples). Document codes: section 5.


## Applicant block

| # | Column | Field | Status | When (CM) | Max | Allowed values / format | Pack fill | DB table · CERSAI record | Notes |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `APPL_TYPE` | Application type | **M** |  | 2 | 01 | ALWAYS | CKYC_FIN_DATA · Detail 20 | 01 = NEW. UPDATE (03) goes in a separate file. |
| 2 | `BRID` | Branch code | **M** |  | 10 | config.BRID; `ALNUM` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Branch code registered with CERSAI under the region. Never the region code. |
| 3 | `CONST_TYPE` | Constitution type | **M** |  | 2 | 01 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Individual = 01 (iFlow static value; CERSAI master shows '1'). |
| 4 | `ACC_TYPE` | Account type | **M** |  | 2 | 05 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Minor = 05. |
| 5 | `CKYC_REF_NUM` | CKYC number | BLANK (new) | APPL_TYPE == '03' | 14 | `CKYC_NO_IND` | BLANK | CKYC_FIN_DATA · Detail 20 | Blank for NEW. Mandatory for UPDATE (14 digits, starts 1-6). |
| 6 | `APPL_NAME_PREFIX` | Applicant name prefix | **M** |  | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 7 | `APPL_FIRST_NAME` | Applicant first name | **M** |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 8 | `APPL_MIDDLE_NAME` | Applicant middle name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 9 | `APPL_LAST_NAME` | Applicant last name | **M** |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 | CERSAI: mandatory if constitution 01 (iFlow sheet marks optional - keep filled). |
| 10 | `FORS_FLG` | Father/Spouse flag | CM | FATHERORSPOUSE_FIRST_NAME != '' | 2 | 01, 02 | ALWAYS | CKYC_FIN_DATA · Detail 20 | 01 father, 02 spouse. Pack: always filled; 02 only for married women (prefix MRS). |
| 11 | `FATHERORSPOUSE_NAME_PREFIX` | Father/Spouse prefix | CM | FORS_FLG != '' | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 12 | `FATHERORSPOUSE_FIRST_NAME` | Father/Spouse first name | CM | FORS_FLG != '' | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 13 | `FATHERORSPOUSE_MIDDLE_NAME` | Father/Spouse middle name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 14 | `FATHERORSPOUSE_LAST_NAME` | Father/Spouse last name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 15 | `MOTHER_NAME_PREFIX` | Mother name prefix | CM | MOTHER_FIRST_NAME != '' | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 16 | `MOTHER_FIRST_NAME` | Mother first name | CM | FORS_FLG == '' | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 | At least one of father / spouse / mother name is mandatory. Pack: always filled. |
| 17 | `MOTHER_MIDDLE_NAME` | Mother middle name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 18 | `MOTHER_LAST_NAME` | Mother last name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 19 | `GENDER` | Gender | **M** |  | 1 | M, F, T | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 20 | `MARITAL_STATUS` | Marital status | O |  | 2 | S | ALWAYS | CKYC_FIN_DATA · iFlow only | Not in CERSAI V1.3 upload (iFlow: Blank). Pack fills M/S; Minor = S. |
| 21 | `NATIONALITY` | Nationality | O |  | 2 | `COUNTRY` | ALWAYS | CKYC_FIN_DATA · iFlow only | iFlow: Blank. Pack = IN. |
| 22 | `OCC_TYPE` | Occupation type | O |  | 4 | `CODE2` | ALWAYS | CKYC_FIN_DATA · iFlow only | iFlow: Blank, no CERSAI master in V1.3 upload. Pack = 02. |
| 23 | `DOB` | Date of birth | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Not future, not equal to upload date. IND age >= 18; Minor age < 18 (at DECL_DATETIME). |
| 24 | `RES_STATUS` | Residential status | **M** |  | 2 | 01, 02, 03, 04 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Pack = 01. |
| 25 | `DIS_FLAG` | Differently abled flag | **M** |  | 1 | 0, 1 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Pack = 0. |
| 26 | `DIS_TYPE` | Type of impairment | CM | DIS_FLAG == '1' | 2 | MASTER:type_of_impairment.csv | DIS_FLAG == '1' | CKYC_FIN_DATA · Detail 20 |  |
| 27 | `DIS_PERCENT` | Impairment % | CM | DIS_FLAG == '1' | 3 | `PERCENT` | DIS_FLAG == '1' | CKYC_FIN_DATA · Detail 20 |  |
| 28 | `DIS_UDID_NUMBER` | UDID number | CM | DIS_FLAG == '1' | 18 | `UDID` | DIS_FLAG == '1' | CKYC_FIN_DATA · Detail 20 |  |
| 29 | `OTHER_JURI_FLG` | Tax resident outside India flag | O |  | 2 | `CODE2` | ALWAYS | CKYC_FIN_DATA · iFlow only | iFlow: Blank. Pack = 01. |
| 30 | `PERM_ADD_TYPE` | Permanent address type | O |  | 2 |  | BLANK | CKYC_FIN_DATA · Detail 20 | Cosmos rule: always BLANK (never 'PER'). |
| 31 | `PERM_ADD_LINE1` | Permanent address line 1 | **M** |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 32 | `PERM_ADD_LINE2` | Permanent address line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 33 | `PERM_ADD_LINE3` | Permanent address line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 34 | `PERM_ADD_CITY` | Permanent city | **M** |  | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 35 | `PERM_ADD_DIST` | Permanent district | **M** |  | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA · Detail 20 | Must equal the district of PERM_ADD_PIN in the Pincode master (case-insensitive). |
| 36 | `PERM_ADD_STATE` | Permanent state | **M** |  | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Must equal the state of PERM_ADD_PIN. |
| 37 | `PERM_ADD_COUNTRY` | Permanent country | **M** |  | 2 | IN | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 38 | `PERM_ADD_PIN` | Permanent PIN | **M** |  | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 39 | `PERM_ADD_PROOF` | Permanent address proof (POA master) | **M** |  | 2 | 01, 02, 05, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Must equal POA_TYPE. Deemed 11-15 NOT allowed for permanent address. Minor: never 03 (DL) or 04 (Voter). |
| 40 | `PERM_ADD_PROOF_DESC` | Permanent address proof description | O |  | 75 |  | ALWAYS | CKYC_FIN_DATA · Detail 20 | iFlow: Blank. Pack = POA master name of PERM_ADD_PROOF. |
| 41 | `PERM_TO_COMM_FLG` | Permanent = correspondence flag | **M** |  | 1 | Y, N | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 42 | `COMM_ADDRESS_TYPE` | Correspondence address type | CM | PERM_TO_COMM_FLG == 'N' | 2 | 01, 02, 03, 04, 05 | PERM_TO_COMM_FLG == 'N' | CKYC_FIN_DATA · Detail 20 | Blank when flag Y (pack). |
| 43 | `COMM_Add_Line1` | Correspondence line 1 | CM | PERM_TO_COMM_FLG == 'N' | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Flag Y: copy of PERM line 1. Flag N: a different address. |
| 44 | `COMM_Add_Line2` | Correspondence line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 45 | `COMM_Add_Line3` | Correspondence line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 46 | `COMM_Add_City` | Correspondence city | CM | PERM_TO_COMM_FLG == 'N' | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 47 | `COMM_Add_Dist` | Correspondence district | CM | PERM_TO_COMM_FLG == 'N' | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 48 | `COMM_Add_State` | Correspondence state | CM | PERM_TO_COMM_FLG == 'N' | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 49 | `COMM_Add_Country` | Correspondence country | CM | PERM_TO_COMM_FLG == 'N' | 2 | IN | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 50 | `COMM_Add_Pin` | Correspondence PIN | CM | PERM_TO_COMM_FLG == 'N' | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 51 | `DECL_DATETIME` | Declaration date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Not future. Pack = 02-09-2026. |
| 52 | `DECL_PLACE` | Declaration place | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 53 | `KYC_VERIFY_DATETIME` | KYC verification date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Not future. Pack = 03-09-2026. |
| 54 | `TYPE_OF_DOC` | Type of document submitted | **M** |  | 2 | 01, 02, 03, 04, 05, 06 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Pack = 01. |
| 55 | `KYC_VERIFY_NAME` | KYC verifier name | **M** |  | 150 | `NAME` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 56 | `KYC_VERIFY_DESGN` | KYC verifier designation | **M** |  | 50 | `DESIGNATION` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 57 | `KYC_VERIFY_BRANCH` | KYC verifier branch | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 58 | `KYC_VERIFY_EMP_CODE` | KYC verifier employee code | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 59 | `ORG_NAME` | Organisation name | **M** |  | 150 | config.ORG_NAME | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 60 | `ORG_CODE` | Organisation (FI) code | **M** |  | 7 | config.ORG_CODE | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 61 | `MINOR` | Minor flag | **M** |  | 1 | Y | ALWAYS | CKYC_FIN_DATA · Detail 20 | Minor = Y (age < 18). |
| 62 | `CUSTOMER_REFERENCE_NUMBER` | Customer reference number (FI ref) | **M** |  | 14 | `ALNUM` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Unique across all packs. Image folder name. Pack COSI/COSM + 8 digits. |
| 63 | `PHOTO_FILE_NAME` | Photo image file | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA · Detail 70 | Pack = Photo.jpg (doc code 02). |
| 64 | `POI_FILE_NAME` | POI image file | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA · Detail 70 | Must be the image of POI_TYPE (see DOC_MAP). |
| 65 | `POI_CATEGORY` | POI category (Identity Code name) | **M** |  | 50 |  | ALWAYS | CKYC_FIN_DATA · Detail 30 | Text name of POI_TYPE from Identity Code master (see DOC_MAP.poi_category). |
| 66 | `POI_TYPE` | POI type (Identity Code) | **M** |  | 2 | A, E, F, G, H, I | ALWAYS | CKYC_FIN_DATA · Detail 30 | Minor: never Voter ID (B) or Driving Licence (D). Pack uses A/E/F. |
| 67 | `POI_NUMBER` | POI number | **M** |  | 20 | format by POI_TYPE (doc_map) | ALWAYS | CKYC_FIN_DATA · Detail 30 | Format depends on POI_TYPE (DOC_MAP.number_format). |
| 68 | `POI_EXPIRY_DATE` | POI expiry date | O |  | 10 | `DATE` | POI_TYPE in ('A','D') | CKYC_FIN_DATA · Detail 20 | Pack: filled (future date) for Passport/DL only; blank otherwise. |
| 69 | `POA_FILE_NAME` | POA image file | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA · Detail 70 | Same file as POI_FILE_NAME when the same document is POI and POA (POIA). |
| 70 | `POA_CATEGORY` | POA category (POA master name) | **M** |  | 50 |  | ALWAYS | CKYC_FIN_DATA · Detail 20 | Text name of POA_TYPE (DOC_MAP.poa_category). |
| 71 | `POA_TYPE` | POA type (POA master) | **M** |  | 2 | 01, 02, 05, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Minor: never 03 (DL) or 04 (Voter). |
| 72 | `POA_NUMBER` | POA number | **M** |  | 20 | format by POA_TYPE (doc_map) | ALWAYS | CKYC_FIN_DATA · Detail 30 |  |
| 73 | `POA_EXPIRY_DATE` | POA expiry date | O |  | 10 | `DATE` | POA_TYPE in ('02','03') | CKYC_FIN_DATA · Detail 20 |  |
| 74 | `CUST_NAME_UPDT_FLG` | Cust Name Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 75 | `PER_DET_UPDT_FLG` | Per Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 76 | `ADD_DET_UPDT_FLG` | Add Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 77 | `CONT_DET_UPDT_FLG` | Cont Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 78 | `REM_UPDT_FLG` | Rem Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 79 | `KYC_VERIFY_UPDT_FLG` | Kyc Verify Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 80 | `ID_DET_UPDT_FLG` | Id Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 81 | `REL_PER_UPDT_FLG` | Rel Per Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 82 | `CONT_PER_UPDT_FLG` | Cont Per Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 83 | `IMG_UPDT_FLG` | Img Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 84 | `PAN_OR_FORM60` | PAN or FORM60 | **M** |  | 10 | `PAN_OR_FORM60` | ALWAYS | CKYC_FIN_DATA · Detail 20 | IND pack: PAN, 4th letter P. Minor: PAN or FORM60. |
| 85 | `COMM_ADDRESS_CATEGORY` | Correspondence address proof category | CM | PERM_TO_COMM_FLG == 'N' | 50 |  | ALWAYS | CKYC_FIN_DATA · Detail 20 | Pack always fills. Flag Y = POA_CATEGORY. Flag N = deemed proof name (11-14). |
| 86 | `COMM_ADDRESS_IDTYPE` | Correspondence address proof type | CM | PERM_TO_COMM_FLG == 'N' | 2 | 01, 02, 05, 08, 09, 10, 11, 12 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Pack/user rule: flag Y = POA_TYPE; flag N = deemed proof 11/12/13/14 only (never an OVD). Minor: 11/12 only. |
| 87 | `COMM_ADDRESS_NUMBER` | Correspondence address proof number | CM | PERM_TO_COMM_FLG == 'N' | 20 | format by COMM_ADDRESS_IDTYPE | ALWAYS | CKYC_FIN_DATA · Detail 20 | Flag Y = POA_NUMBER. Flag N = unique bill/receipt no (EB/PTR/PPO/ALT + 12 digits). |
| 88 | `RESIDENCE_TELEPHONE_NO_STD_CODE` | Residence STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA · Detail 20 | STD and number both or neither. |
| 89 | `RESIDENCE_TELEPHONE_NO` | Residence tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 90 | `OFFICE_TELEPHONE_NO_STD_CODE` | Office STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 91 | `OFFICE_TELEPHONE_NO` | Office tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 92 | `MOBILE_NO_ISD_CODE` | Mobile ISD | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 93 | `MOBILE_NO` | Mobile | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA · Detail 20 | ISD 91 -> exactly 10 digits starting 6-9. |
| 94 | `FAX_NO_STD_CODE` | Fax STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 95 | `FAX_NO` | Fax | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 96 | `EMAIL_ID` | Email | O |  | 50 | `EMAIL` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 97 | `MOBILE_NO2_ISD_CODE2` | Mobile 2 ISD | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 98 | `MOBILE_NO2` | Mobile 2 | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
| 99 | `EMAIL_ID2` | Email 2 | O |  | 50 | `EMAIL` | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |

## Related person block (blank on 03_Minor_Without_RP except CUST_ID)

| # | Column | Field | Status | When (CM) | Max | Allowed values / format | Pack fill | DB table · CERSAI record | Notes |
|---|---|---|---|---|---|---|---|---|---|
| 100 | `DECEASED_DATE` | Deceased date | BLANK (new) |  | 10 | `DATE` | BLANK | CKYC_FIN_DATA_RP · Detail 20 | Keep blank for upload. |
| 101 | `REL_TYPE` | Relationship type | **M** |  | 2 | 1 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Minor with RP: 1 (Guardian). Minor without RP: blank. |
| 102 | `REL_TYPE_OTHERS` | Relationship others text | CM | REL_TYPE == '15' | 150 |  | REL_TYPE == '15' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 103 | `ADD_DEL_REL_PER` | Add/Delete RP | **M** |  | 2 | 01 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | 01 = add (NEW). |
| 104 | `REL_PER_KYC_NUM` | RP CKYC number | O |  | 14 | `CKYC_NO_IND` | BLANK | CKYC_FIN_DATA_RP · Detail 40 | If given, only RP name is further mandatory. |
| 105 | `REL_PER_NAME_PREFIX` | RP prefix | **M** |  | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 106 | `REL_PER_FIRST_NAME` | RP first name | **M** |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 107 | `REL_PER_MIDDLE_NAME` | RP middle name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 108 | `REL_PER_LAST_NAME` | RP last name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Pack: always filled. |
| 109 | `REL_PER_MAIDEN_PREFIX` | RP maiden prefix | O |  | 5 | MR, MRS, MS, MISS, DR | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 110 | `REL_PER_MAIDEN_FIRST_NAME` | RP maiden first | O |  | 50 | `NAME` | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 111 | `REL_PER_MAIDEN_MIDDLE_NAME` | RP maiden middle | O |  | 50 | `NAME` | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 112 | `REL_PER_MAIDEN_LAST_NAME` | RP maiden last | O |  | 50 | `NAME` | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 113 | `REL_PER_FATHERORSPOUSE` | RP father/spouse flag | CM | REL_PER_FATHERORSPOUSE_FIRST_NAME != '' | 2 | 01, 02 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 114 | `REL_PER_FATHERORSPOUSE_NAME_PREFIX` | RP father/spouse prefix | CM | REL_PER_FATHERORSPOUSE != '' | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 115 | `REL_PER_FATHERORSPOUSE_FIRST_NAME` | RP father/spouse first | CM | REL_PER_FATHERORSPOUSE != '' | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 116 | `REL_PER_FATHERORSPOUSE_MIDDLE_NAME` | RP father/spouse middle | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 117 | `REL_PER_FATHERORSPOUSE_LAST_NAME` | RP father/spouse last | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 118 | `REL_PER_MOTHER_NAME_PREFIX` | RP mother prefix | CM | REL_PER_MOTHER_FIRST_NAME != '' | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Pack/user rule: RP mother name always filled. |
| 119 | `REL_PER_MOTHER_FIRST_NAME` | RP mother first | CM | REL_PER_FATHERORSPOUSE == '' | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 120 | `REL_PER_MOTHER_MIDDLE_NAME` | RP mother middle | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 121 | `REL_PER_MOTHER_LAST_NAME` | RP mother last | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 122 | `REL_PER_DOB` | RP date of birth | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Not future; RP age >= 18. |
| 123 | `REL_PER_GENDER` | RP gender | **M** |  | 1 | M, F, T | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 124 | `REL_PER_MARITAL_STATUS` | RP marital status | O |  | 2 | M, S | ALWAYS | CKYC_FIN_DATA_RP · iFlow only |  |
| 125 | `REL_PER_NATIONALITY` | RP nationality | **M** |  | 2 | `COUNTRY` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 126 | `REL_PER_RES_STATUS` | RP residential status | **M** |  | 2 | 01, 02, 03, 04 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 127 | `REL_PER_OCC_TYPE` | RP occupation | O |  | 4 | `CODE2` | ALWAYS | CKYC_FIN_DATA_RP · iFlow only |  |
| 128 | `REL_PER_OTHER_JURI_FLG` | RP other jurisdiction flag | O |  | 2 | `CODE2` | ALWAYS | CKYC_FIN_DATA_RP · iFlow only |  |
| 129 | `REL_PER_JURI_RES_COUNTRY` | RP jurisdiction country | O |  | 2 | `COUNTRY` | BLANK | CKYC_FIN_DATA_RP · iFlow only |  |
| 130 | `REL_PER_JURI_TIN` | RP jurisdiction TIN | O |  | 20 |  | BLANK | CKYC_FIN_DATA_RP · iFlow only |  |
| 131 | `REL_PER_BIRTH_COUNTRY` | RP country of birth | O |  | 2 | `COUNTRY` | ALWAYS | CKYC_FIN_DATA_RP · iFlow only |  |
| 132 | `REL_PER_BIRTH_CITY` | RP city of birth | O |  | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_RP · iFlow only |  |
| 133 | `REL_PER_PANORFORM60` | RP PAN or FORM60 | **M** |  | 10 | `PAN_OR_FORM60` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Pack: PAN with 4th letter P. |
| 134 | `REL_PER_UID` | RP Aadhaar (UID) | CM | no_other_rp_id() | 12 | `AADHAAR` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Any ONE of the 8 RP ID columns is mandatory. Pack: UID always filled. |
| 135 | `REL_PER_VOTER` | RP Voter ID | CM | rp_doc_used('04') | 20 | `VOTER_EPIC` | rp_doc_used('04') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 136 | `REL_PER_NREGA` | RP NREGA | CM | rp_doc_used('05') | 20 | `NREGA` | rp_doc_used('05') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 137 | `REL_PER_PASSPORT` | RP Passport | CM | rp_doc_used('02') | 20 | `PASSPORT` | rp_doc_used('02') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 138 | `REL_PER_DRIVING` | RP Driving licence | CM | rp_doc_used('03') | 20 | `DRIVING_LICENCE` | rp_doc_used('03') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 139 | `REL_PER_POPULATION_LET` | RP NPR letter | CM | rp_doc_used('08') | 20 | `NPR` | rp_doc_used('08') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 140 | `REL_PER_EKYC` | RP E-KYC (last 4 of UID) | CM | rp_doc_used('09') | 4 | `LAST4_AADHAAR` | rp_doc_used('09') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 141 | `REL_PER_OFFLINE_UID` | RP Offline Aadhaar (last 4 of UID) | CM | rp_doc_used('10') | 4 | `LAST4_AADHAAR` | rp_doc_used('10') | CKYC_FIN_DATA_RP · Detail 40 |  |
| 142 | `REL_PER_PHOTO_NAME` | RP photo image | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Pack: RP1_Photo.jpg / RP2_Photo.jpg. |
| 143 | `REL_PER_POI_NAME` | RP POI image | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | RP{n}_<doc>.jpg of the RP's identity document. |
| 144 | `REL_PER_PER_ADDRESS_NAME` | RP permanent-address proof image | CM | REL_PER_ADD_PROF != '' | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | RP{n}_<doc>.jpg of REL_PER_ADD_PROF document. |
| 145 | `REL_PER_ADD_LINE1` | RP permanent line 1 | **M** |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 146 | `REL_PER_ADD_LINE2` | RP permanent line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 147 | `REL_PER_ADD_LINE3` | RP permanent line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 148 | `REL_PER_ADD_CITY` | RP permanent city | **M** |  | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 149 | `REL_PER_ADD_DISTRICT` | RP permanent district | **M** |  | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 150 | `REL_PER_ADD_PIN` | RP permanent PIN | **M** |  | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 151 | `REL_PER_ADD_STATE` | RP permanent state | **M** |  | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 152 | `REL_PER_ADD_COUNTRY` | RP permanent country | **M** |  | 2 | IN | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 153 | `REL_PER_ADD_PROF` | RP permanent address proof (POA master) | **M** |  | 2 | 01, 02, 03, 04, 05, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Individual documents only (no simplified / LE codes). Its number must be in the matching RP ID column. |
| 154 | `REL_PER_SAMEASPERM_ADD_FLAG` | RP current = permanent flag | **M** |  | 1 | Y, N | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 155 | `REL_PER_CURR_ADD_LINE1` | RP current line 1 | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Flag Y: copy of permanent. Flag N: a different address. |
| 156 | `REL_PER_CURR_ADD_LINE2` | RP current line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 157 | `REL_PER_CURR_ADD_LINE3` | RP current line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 158 | `REL_PER_CURR_ADD_CITY` | RP current city | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 159 | `REL_PER_CURR_ADD_DISTRICT` | RP current district | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 160 | `REL_PER_CURR_ADD_PIN` | RP current PIN | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 161 | `REL_PER_CURR_ADD_STATE` | RP current state | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 162 | `REL_PER_CURR_ADD_COUNTRY` | RP current country | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 3 | IN | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 163 | `REL_PER_CURR_ADD_PROOF` | RP current address proof (POA master) | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 2 | 01, 02, 03, 04, 05, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Flag N: a document DIFFERENT from REL_PER_ADD_PROF and its ID number MUST be in the matching RP ID column (01 UID, 02 PASSPORT, 03 DRIVING, 04 VOTER, 05 NREGA, 08 POPULATION_LET, 09 EKYC, 10 OFFLINE_UID). Flag Y: = REL_PER_ADD_PROF. |
| 164 | `REL_PER_TEL_STDCODE` | RP res STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 165 | `REL_PER_TEL_NO` | RP res tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 166 | `REL_PER_OFFTEL_STDCODE` | RP office STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 167 | `REL_PER_OFFTEL_NO` | RP office tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 168 | `REL_PER_MOBILE_CODE` | RP mobile ISD | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 169 | `REL_PER_MOBILE_NO` | RP mobile | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 170 | `REL_PER_EMAILID` | RP email | O |  | 100 | `EMAIL` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 171 | `REL_PER_DECL_DATETIME` | RP declaration date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 172 | `REL_PER_DECL_PLACE` | RP declaration place | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 173 | `REL_PER_KYC_VERIFY_DATETIME` | RP KYC verification date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 174 | `REL_PER_TYPE_OF_DOC` | RP type of document | **M** |  | 2 | 01, 02, 03, 04, 05, 06 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 175 | `REL_PER_KYC_VERIFY_NAME` | RP KYC verifier name | **M** |  | 150 | `NAME` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 176 | `REL_PER_KYC_VERIFY_DESGN` | RP KYC verifier designation | **M** |  | 50 | `DESIGNATION` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 177 | `REL_PER_KYC_VERIFY_BRANCH` | RP KYC verifier branch | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 178 | `REL_PER_KYC_VERIFY_EMP_CODE` | RP KYC verifier emp code | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 179 | `REL_PER_ORG_NAME` | RP organisation name | **M** |  | 150 | config.ORG_NAME | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 180 | `REL_PER_ORG_CODE` | RP organisation code | **M** |  | 20 | config.ORG_CODE | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 | Always = ORG_CODE, even in DVS. |
| 181 | `REL_PER_DIN_NUMBER` | RP DIN | CM | REL_TYPE == '4' | 8 | `DIN` | REL_TYPE == '4' | CKYC_FIN_DATA_RP · Detail 40 | Mandatory for Director (4); blank otherwise. |
| 182 | `FIN_RP_CREATED_TIME` | System: RP created time | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system |  |
| 183 | `CUST_ID` | Parent customer id | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_RP · system | = CUSTOMER_REFERENCE_NUMBER of the minor / legal entity. On LE continuation rows it is the only link to the entity. |
| 184 | `REL_PER_CUST_ID` | RP customer id | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_RP · system | Unique per RP. Pack COSR + 8 digits. |
| 185 | `REL_PER_CONST` | System field: REL_PER_CONST | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 186 | `REL_PER_APPLICATION_DOC_ID` | System field: REL_PER_APPLICATION_DOC_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 187 | `REL_PER_PHOTO_DOC_ID` | System field: REL_PER_PHOTO_DOC_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 188 | `REL_PER_POI_DOC_ID` | System field: REL_PER_POI_DOC_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 189 | `FIN_FIN_ID` | System field: FIN_FIN_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 190 | `FIN_API_RP_ID` | System field: FIN_API_RP_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 191 | `REL_PER_DIS_FLAG` | RP differently abled flag | **M** |  | 1 | 0, 1 | ALWAYS | CKYC_FIN_DATA_RP · Detail 40 |  |
| 192 | `REL_PER_DIS_TYPE` | RP impairment type | CM | REL_PER_DIS_FLAG == '1' | 2 | MASTER:type_of_impairment.csv | REL_PER_DIS_FLAG == '1' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 193 | `REL_PER_DIS_PERCENT` | RP impairment % | CM | REL_PER_DIS_FLAG == '1' | 3 | `PERCENT` | REL_PER_DIS_FLAG == '1' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 194 | `REL_PER_DIS_UDID_NUMBER` | RP UDID | CM | REL_PER_DIS_FLAG == '1' | 18 | `UDID` | REL_PER_DIS_FLAG == '1' | CKYC_FIN_DATA_RP · Detail 40 |  |
| 195 | `REL_PER_POI_DCM_CODE` | System field: REL_PER_POI_DCM_CODE | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 196 | `REL_PER_PHOTO_DCM_CODE` | System field: REL_PER_PHOTO_DCM_CODE | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 197 | `REL_PER_APPLICATION_DCM_CODE` | System field: REL_PER_APPLICATION_DCM_CODE | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
| 198 | `REL_PER_CURR_ADDRESS_NAME` | System field: REL_PER_CURR_ADDRESS_NAME | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_RP · system | System-populated - keep blank in upload CSV. |
