# Individual (IND) - field spec, 99 columns

Folder `01_Individual`. ACC_TYPE 01, MINOR N, CONST_TYPE 01, age >= 18. No related-person columns in this header.

Columns: **99** · file prefix `CKYC_DATA_IN_DDMMYYYY_NN.csv` · pipe `|` delimited · UTF-8 (BOM).

Status: **M** mandatory · CM conditional mandatory (see *When*) · O optional · BLANK (new) = must be blank for a NEW upload · SYSTEM = system column, keep blank.
Pack fill = what the Cosmos positive pack does (ALWAYS / BLANK / condition). Formats: `01_VALIDATION_RULES.md` section 10 (regex, examples). Document codes: section 5.


## Applicant block

| # | Column | Field | Status | When (CM) | Max | Allowed values / format | Pack fill | DB table · CERSAI record | Notes |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `APPL_TYPE` | Application type | **M** |  | 2 | 01 | ALWAYS | CKYC_FIN_DATA · Detail 20 | 01 = NEW. UPDATE (03) goes in a separate file. |
| 2 | `BRID` | Branch code | **M** |  | 10 | config.BRID; `ALNUM` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Branch code registered with CERSAI under the region. Never the region code. |
| 3 | `CONST_TYPE` | Constitution type | **M** |  | 2 | 01 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Individual = 01 (iFlow static value; CERSAI master shows '1'). |
| 4 | `ACC_TYPE` | Account type | **M** |  | 2 | 01, 04 | ALWAYS | CKYC_FIN_DATA · Detail 20 | IND 01 (04 = OTP e-KYC, POI must be H). Minor = 05. |
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
| 20 | `MARITAL_STATUS` | Marital status | O |  | 2 | M, S | ALWAYS | CKYC_FIN_DATA · iFlow only | Not in CERSAI V1.3 upload (iFlow: Blank). Pack fills M/S; Minor = S. |
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
| 39 | `PERM_ADD_PROOF` | Permanent address proof (POA master) | **M** |  | 2 | 01, 02, 03, 04, 05, 08, 09, 10, 16 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Must equal POA_TYPE. Deemed 11-15 NOT allowed for permanent address. |
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
| 61 | `MINOR` | Minor flag | **M** |  | 1 | N | ALWAYS | CKYC_FIN_DATA · Detail 20 | IND = N, Minor = Y. |
| 62 | `CUSTOMER_REFERENCE_NUMBER` | Customer reference number (FI ref) | **M** |  | 14 | `ALNUM` | ALWAYS | CKYC_FIN_DATA · Detail 20 | Unique across all packs. Image folder name. Pack COSI/COSM + 8 digits. |
| 63 | `PHOTO_FILE_NAME` | Photo image file | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA · Detail 70 | Pack = Photo.jpg (doc code 02). |
| 64 | `POI_FILE_NAME` | POI image file | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA · Detail 70 | Must be the image of POI_TYPE (see DOC_MAP). |
| 65 | `POI_CATEGORY` | POI category (Identity Code name) | **M** |  | 50 |  | ALWAYS | CKYC_FIN_DATA · Detail 30 | Text name of POI_TYPE from Identity Code master (see DOC_MAP.poi_category). |
| 66 | `POI_TYPE` | POI type (Identity Code) | **M** |  | 2 | A, B, D, E, F, G, H, I, J | ALWAYS | CKYC_FIN_DATA · Detail 30 | Pack uses A/B/D/E/F. C, Z, S01, S02 are download-only - never upload. |
| 67 | `POI_NUMBER` | POI number | **M** |  | 20 | format by POI_TYPE (doc_map) | ALWAYS | CKYC_FIN_DATA · Detail 30 | Format depends on POI_TYPE (DOC_MAP.number_format). |
| 68 | `POI_EXPIRY_DATE` | POI expiry date | O |  | 10 | `DATE` | POI_TYPE in ('A','D') | CKYC_FIN_DATA · Detail 20 | Pack: filled (future date) for Passport/DL only; blank otherwise. |
| 69 | `POA_FILE_NAME` | POA image file | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA · Detail 70 | Same file as POI_FILE_NAME when the same document is POI and POA (POIA). |
| 70 | `POA_CATEGORY` | POA category (POA master name) | **M** |  | 50 |  | ALWAYS | CKYC_FIN_DATA · Detail 20 | Text name of POA_TYPE (DOC_MAP.poa_category). |
| 71 | `POA_TYPE` | POA type (POA master) | **M** |  | 2 | 01, 02, 03, 04, 05, 08, 09, 10, 16 | ALWAYS | CKYC_FIN_DATA · Detail 20 |  |
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
| 86 | `COMM_ADDRESS_IDTYPE` | Correspondence address proof type | CM | PERM_TO_COMM_FLG == 'N' | 2 | 01, 02, 03, 04, 05, 08, 09, 10, 11, 12, 13, 14, 15, 16 | ALWAYS | CKYC_FIN_DATA · Detail 20 | Pack/user rule: flag Y = POA_TYPE; flag N = deemed proof 11/12/13/14 only (never an OVD). Minor: 11/12 only. |
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
