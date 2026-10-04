# Legal Entity (multi-RP) - field spec, 189 columns

Folder `04_Legal_Entity_Multi_RP`. Row 1 = entity + RP1; each further RP = a continuation row with columns 1-91 blank, linked by CUST_ID. NUM_OF_REL_PER = number of rows for that CUST_ID. No entity photograph.

Columns: **189** · file prefix `CKYC_DATA_LE_DDMMYYYY_NN.csv` · pipe `|` delimited · UTF-8 (BOM).

Status: **M** mandatory · CM conditional mandatory (see *When*) · O optional · BLANK (new) = must be blank for a NEW upload · SYSTEM = system column, keep blank.
Pack fill = what the Cosmos positive pack does (ALWAYS / BLANK / condition). Formats: `01_VALIDATION_RULES.md` section 10 (regex, examples). Document codes: section 5.


## Entity block (blank on LE continuation rows)

| # | Column | Field | Status | When (CM) | Max | Allowed values / format | Pack fill | DB table · CERSAI record | Notes |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `BRID` | Branch code | **M** |  | 10 | config.BRID; `ALNUM` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Branch code registered with CERSAI under the region. Never the region code. |
| 2 | `CUST_NAME_UPDT_FLG` | Cust Name Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 3 | `PER_DET_UPDT_FLG` | Per Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 4 | `ADD_DET_UPDT_FLG` | Add Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 5 | `CONT_DET_UPDT_FLG` | Cont Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 6 | `REM_UPDT_FLG` | Rem Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 7 | `KYC_VERIFY_UPDT_FLG` | Kyc Verify Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 8 | `ID_DET_UPDT_FLG` | Id Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 9 | `REL_PER_UPDT_FLG` | Rel Per Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 10 | `CONT_PER_UPDT_FLG` | Cont Per Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 11 | `IMG_DET_UPDT_FLG` | Img Det Updt Flg | BLANK (new) | APPL_TYPE == '03' | 2 | 01, 02 | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Must be BLANK for NEW (01). UPDATE: each 01/02. |
| 12 | `COMM_ADDRESS_IDTYPE` | Local address proof type | CM | PERM_TO_COMM_FLG == 'N' | 2 | 06, 07, 99 | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | LE: only 06/07/99 (deemed proofs are Individual-only). Pack = POA_TYPE. |
| 13 | `COMM_ADDRESS_NUMBER` | Local address proof number | CM | PERM_TO_COMM_FLG == 'N' | 60 (DB 20) | format by COMM_ADDRESS_IDTYPE | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack = POA_NUMBER. iFlow / iSolve DB length 20 - a 21-char CIN exceeds it (see max_len_db). |
| 14 | `RESIDENCE_TELEPHONE_NO_STD_CODE` | Residence STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | STD and number both or neither. |
| 15 | `RESIDENCE_TELEPHONE_NO` | Residence tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 16 | `OFFICE_TELEPHONE_NO_STD_CODE` | Office STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 17 | `OFFICE_TELEPHONE_NO` | Office tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 18 | `MOBILE_NO_ISD_CODE` | Mobile ISD | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 19 | `MOBILE_NO` | Mobile | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | ISD 91 -> exactly 10 digits starting 6-9. |
| 20 | `FAX_NO_STD_CODE` | Fax STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 21 | `FAX_NO` | Fax | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 22 | `EMAIL_ID` | Email | O |  | 50 | `EMAIL` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 23 | `MOBILE_NO2_ISD_CODE2` | Mobile 2 ISD | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 24 | `MOBILE_NO2` | Mobile 2 | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 25 | `EMAIL_ID2` | Email 2 | O |  | 50 | `EMAIL` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 26 | `CONST_TYPE` | Constitution type | **M** |  | 2 | A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack uses D, E, J, B, H. |
| 27 | `CONST_TYPE_OTHERS` | Constitution others text | CM | CONST_TYPE == 'R' | 20 |  | CONST_TYPE == 'R' | CKYC_FIN_DATA_LE · Detail 20 |  |
| 28 | `ACC_HOLDER_TYPE_FLAG` | Account holder type flag | O |  | 1 |  | BLANK | CKYC_FIN_DATA_LE · iFlow only | Not used for LE - blank. |
| 29 | `ACC_HOLDER_TYPE` | Account holder type | O |  | 5 |  | BLANK | CKYC_FIN_DATA_LE · iFlow only | Not used for LE - blank. |
| 30 | `ACC_TYPE` | Account type | O |  | 2 |  | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Blank for LE. |
| 31 | `CKYC_REF_NUM` | CKYC number | BLANK (new) | False | 14 | `CKYC_NO_LE` | BLANK | CKYC_FIN_DATA_LE · Detail 20 | Blank for NEW. UPDATE: 14 digits starting 7-9. |
| 32 | `APPL_FULL_NAME` | Entity name | **M** |  | 150 | `ENTITY_NAME` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Unique across packs. Suffix matches constitution (PRIVATE LIMITED / LIMITED / LLP / AND COMPANY / TRUST). |
| 33 | `DATE_OF_INC` | Date of incorporation | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Past date, not the upload date. |
| 34 | `PLACE_OF_INC` | Place of incorporation | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 35 | `DATE_OF_COMMENCEMENT` | Date of commencement | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | >= DATE_OF_INC, not future. |
| 36 | `COUNTRY_OF_INC` | Country of incorporation | **M** |  | 2 | IN | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 37 | `TIN` | TIN / GSTIN | O |  | 20 | `GSTIN` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack: GSTIN with embedded PAN = PAN_OR_FORM60 and state code = PERM_ADD_STATE. |
| 38 | `TIN_COUNTRY` | TIN country | CM | TIN != '' | 2 | IN | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 39 | `PAN_OR_FORM60` | Entity PAN or FORM60 | **M** |  | 10 | `PAN_OR_FORM60` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | 4th letter by constitution: D/E -> C, B/J -> F, H -> T. |
| 40 | `PERM_ADD_LINE1` | Permanent address line 1 | **M** |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 41 | `PERM_ADD_LINE2` | Permanent address line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 42 | `PERM_ADD_LINE3` | Permanent address line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 43 | `PERM_ADD_CITY` | Permanent city | **M** |  | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 44 | `PERM_ADD_DIST` | Permanent district | **M** |  | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Must equal the district of PERM_ADD_PIN in the Pincode master (case-insensitive). |
| 45 | `PERM_ADD_STATE` | Permanent state | **M** |  | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Must equal the state of PERM_ADD_PIN. |
| 46 | `PERM_ADD_COUNTRY` | Permanent country | **M** |  | 2 | IN | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 47 | `PERM_ADD_PIN` | Permanent PIN | **M** |  | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 48 | `PERM_ADD_PROOF` | Registered address proof (POA master) | **M** |  | 2 | 06, 07, 99 | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Must equal POA_TYPE. |
| 49 | `PERM_ADD_PROOF_DESC` | Registered address proof description | CM | PERM_ADD_PROOF == '99' | 75 |  | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Mandatory for 99. Pack fills the master name. |
| 50 | `PERM_TO_COMM_FLG` | Registered = local address flag | **M** |  | 1 | Y, N | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 51 | `COMM_ADD_LINE1` | Local address line 1 | CM | PERM_TO_COMM_FLG == 'N' | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Indian local address only. |
| 52 | `COMM_ADD_LINE2` | Local address line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 53 | `COMM_ADD_LINE3` | Local address line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 54 | `COMM_ADD_CITY` | Local city | CM | PERM_TO_COMM_FLG == 'N' | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 55 | `COMM_ADD_DIST` | Local district | CM | PERM_TO_COMM_FLG == 'N' | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 56 | `COMM_ADD_STATE` | Local state | CM | PERM_TO_COMM_FLG == 'N' | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 57 | `COMM_ADD_COUNTRY` | Local country | CM | PERM_TO_COMM_FLG == 'N' | 3 | IN | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 58 | `COMM_ADD_PIN` | Local PIN | CM | PERM_TO_COMM_FLG == 'N' | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 59 | `RES_TEL_CODE` | Res STD (LE block) | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack = RESIDENCE_TELEPHONE_NO_STD_CODE. |
| 60 | `RES_TEL_NUM` | Res tel (LE block) | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 61 | `OFF_TEL_CODE` | Office STD (LE block) | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 62 | `OFF_TEL_NUM` | Office tel (LE block) | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 63 | `MOB_ISD_CODE` | Mobile ISD (LE block) | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 64 | `MOB_NUM` | Mobile (LE block) | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 65 | `FAX_STD_CODE` | Fax STD (LE block) | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 66 | `FAX_NUM` | Fax (LE block) | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 67 | `REMARKS` | Remarks | O |  | 300 |  | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 68 | `DECL_DATETIME` | Declaration date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Not future. Pack = 02-09-2026. |
| 69 | `DECL_PLACE` | Declaration place | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 70 | `KYC_VERIFY_DATETIME` | KYC verification date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Not future. Pack = 03-09-2026. |
| 71 | `TYPE_OF_DOC` | Type of document submitted | **M** |  | 2 | 01, 02 | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 72 | `KYC_VERIFY_NAME` | KYC verifier name | **M** |  | 150 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 73 | `KYC_VERIFY_DESGN` | KYC verifier designation | **M** |  | 50 | `DESIGNATION` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 74 | `KYC_VERIFY_BRANCH` | KYC verifier branch | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 75 | `KYC_VERIFY_EMP_CODE` | KYC verifier employee code | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 76 | `ORG_NAME` | Organisation name | **M** |  | 150 | config.ORG_NAME | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 77 | `ORG_CODE` | Organisation (FI) code | **M** |  | 7 | config.ORG_CODE | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 78 | `NUM_OF_ID_DET` | Number of identity details | O |  | 2 | `NUM` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack = 1. |
| 79 | `NUM_OF_REL_PER` | Number of related persons | **M** |  | 3 | `NUM` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | = number of rows with this CUST_ID (pack = 2). |
| 80 | `NUM_OF_IMG` | Number of images | **M** |  | 2 | `NUM` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack = distinct entity document images: 01 (POIA) or 02 (POI != POA). |
| 81 | `CUSTOMER_REFERENCE_NUMBER` | Customer reference number | **M** |  | 14 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Unique. Pack COSL + 8 digits. |
| 82 | `POI_FILE_NAME` | Entity POI image | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 70 |  |
| 83 | `POI_TYPE` | Entity POI type (LE Identity Code) | **M** |  | 2 | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA_LE · Detail 30 | Pack 02 (COI) or 03 (Reg Cert). Never an Individual code (A-J). |
| 84 | `POI_NUMBER` | Entity POI number | CM | POI_TYPE in ('02','03') | 60 (DB 20) | 02: CIN/LLPIN, 03: REG_CERT/GSTIN | ALWAYS | CKYC_FIN_DATA_LE · Detail 30 | CERSAI Identity Code 02/03 size 60. iSolve mapping guide shows VARCHAR2(20) - a 21-char CIN exceeds it (see max_len_db). |
| 85 | `POI_EXPIRY_DATE` | Entity POI expiry | O |  | 10 | `DATE` | BLANK | CKYC_FIN_DATA_LE · Detail 20 |  |
| 86 | `POA_FILE_NAME` | Entity POA image | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_LE · Detail 70 |  |
| 87 | `POA_TYPE` | Entity POA type | **M** |  | 2 | 06, 07, 99 | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |
| 88 | `POA_NUMBER` | Entity POA number | **M** |  | 60 (DB 20) | 06: CIN/LLPIN, 07: REG_CERT/GSTIN | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | CERSAI size 60; iSolve DB VARCHAR2(20) (see max_len_db). |
| 89 | `POA_EXPIRY_DATE` | Entity POA expiry | O |  | 10 | `DATE` | BLANK | CKYC_FIN_DATA_LE · Detail 20 |  |
| 90 | `COMM_ADDRESS_CATEGORY` | Local address proof category | CM | PERM_TO_COMM_FLG == 'N' | 50 |  | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 | Pack = POA master name of POA_TYPE. |
| 91 | `MOBILE_NO2_ISD_CODE` | Mobile 2 ISD (LE block) | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA_LE · Detail 20 |  |

## Related person block

| # | Column | Field | Status | When (CM) | Max | Allowed values / format | Pack fill | DB table · CERSAI record | Notes |
|---|---|---|---|---|---|---|---|---|---|
| 92 | `REL_TYPE` | Relationship type | **M** |  | 2 | 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Minor = 1 (Guardian). LE: D/E -> 4 Director, B/J -> 8 Partner, H -> 7 Trustee. |
| 93 | `REL_TYPE_OTHERS` | Relationship others text | CM | REL_TYPE == '15' | 150 |  | REL_TYPE == '15' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 94 | `ADD_DEL_REL_PER` | Add/Delete RP | **M** |  | 2 | 01 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | 01 = add (NEW). |
| 95 | `REL_PER_KYC_NUM` | RP CKYC number | O |  | 14 | `CKYC_NO_IND` | BLANK | CKYC_FIN_DATA_LE_RP · Detail 40 | If given, only RP name is further mandatory. |
| 96 | `REL_PER_NAME_PREFIX` | RP prefix | **M** |  | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 97 | `REL_PER_FIRST_NAME` | RP first name | **M** |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 98 | `REL_PER_MIDDLE_NAME` | RP middle name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 99 | `REL_PER_LAST_NAME` | RP last name | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Pack: always filled. |
| 100 | `REL_PER_MAIDEN_PREFIX` | RP maiden prefix | O |  | 5 | MR, MRS, MS, MISS, DR | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 101 | `REL_PER_MAIDEN_FIRST_NAME` | RP maiden first | O |  | 50 | `NAME` | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 102 | `REL_PER_MAIDEN_MIDDLE_NAME` | RP maiden middle | O |  | 50 | `NAME` | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 103 | `REL_PER_MAIDEN_LAST_NAME` | RP maiden last | O |  | 50 | `NAME` | REL_PER_FATHERORSPOUSE == '02' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 104 | `REL_PER_FATHERORSPOUSE` | RP father/spouse flag | CM | REL_PER_FATHERORSPOUSE_FIRST_NAME != '' | 2 | 01, 02 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 105 | `REL_PER_FATHERORSPOUSE_NAME_PREFIX` | RP father/spouse prefix | CM | REL_PER_FATHERORSPOUSE != '' | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 106 | `REL_PER_FATHERORSPOUSE_FIRST_NAME` | RP father/spouse first | CM | REL_PER_FATHERORSPOUSE != '' | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 107 | `REL_PER_FATHERORSPOUSE_MIDDLE_NAME` | RP father/spouse middle | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 108 | `REL_PER_FATHERORSPOUSE_LAST_NAME` | RP father/spouse last | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 109 | `REL_PER_MOTHER_NAME_PREFIX` | RP mother prefix | CM | REL_PER_MOTHER_FIRST_NAME != '' | 5 | MR, MRS, MS, MISS, DR | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Pack/user rule: RP mother name always filled. |
| 110 | `REL_PER_MOTHER_FIRST_NAME` | RP mother first | CM | REL_PER_FATHERORSPOUSE == '' | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 111 | `REL_PER_MOTHER_MIDDLE_NAME` | RP mother middle | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 112 | `REL_PER_MOTHER_LAST_NAME` | RP mother last | O |  | 50 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 113 | `REL_PER_DOB` | RP date of birth | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Not future; RP age >= 18. |
| 114 | `REL_PER_GENDER` | RP gender | **M** |  | 1 | M, F, T | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 115 | `REL_PER_MARITAL_STATUS` | RP marital status | O |  | 2 | M, S | ALWAYS | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 116 | `REL_PER_NATIONALITY` | RP nationality | **M** |  | 2 | `COUNTRY` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 117 | `REL_PER_RES_STATUS` | RP residential status | **M** |  | 2 | 01, 02, 03, 04 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 118 | `REL_PER_OCC_TYPE` | RP occupation | O |  | 4 | `CODE2` | ALWAYS | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 119 | `REL_PER_OTHER_JURI_FLG` | RP other jurisdiction flag | O |  | 2 | `CODE2` | ALWAYS | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 120 | `REL_PER_JURI_RES_COUNTRY` | RP jurisdiction country | O |  | 2 | `COUNTRY` | BLANK | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 121 | `REL_PER_JURI_TIN` | RP jurisdiction TIN | O |  | 20 |  | BLANK | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 122 | `REL_PER_BIRTH_COUNTRY` | RP country of birth | O |  | 2 | `COUNTRY` | ALWAYS | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 123 | `REL_PER_BIRTH_CITY` | RP city of birth | O |  | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_LE_RP · iFlow only |  |
| 124 | `REL_PER_PANORFORM60` | RP PAN or FORM60 | **M** |  | 10 | `PAN_OR_FORM60` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Pack: PAN with 4th letter P. |
| 125 | `REL_PER_UID` | RP Aadhaar (UID) | CM | no_other_rp_id() | 12 | `AADHAAR` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Any ONE of the 8 RP ID columns is mandatory. Pack: UID always filled. |
| 126 | `REL_PER_VOTER` | RP Voter ID | CM | rp_doc_used('04') | 20 | `VOTER_EPIC` | rp_doc_used('04') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 127 | `REL_PER_NREGA` | RP NREGA | CM | rp_doc_used('05') | 20 | `NREGA` | rp_doc_used('05') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 128 | `REL_PER_PASSPORT` | RP Passport | CM | rp_doc_used('02') | 20 | `PASSPORT` | rp_doc_used('02') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 129 | `REL_PER_DRIVING` | RP Driving licence | CM | rp_doc_used('03') | 20 | `DRIVING_LICENCE` | rp_doc_used('03') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 130 | `REL_PER_POPULATION_LET` | RP NPR letter | CM | rp_doc_used('08') | 20 | `NPR` | rp_doc_used('08') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 131 | `REL_PER_EKYC` | RP E-KYC (last 4 of UID) | CM | rp_doc_used('09') | 4 | `LAST4_AADHAAR` | rp_doc_used('09') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 132 | `REL_PER_OFFLINE_UID` | RP Offline Aadhaar (last 4 of UID) | CM | rp_doc_used('10') | 4 | `LAST4_AADHAAR` | rp_doc_used('10') | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 133 | `REL_PER_PHOTO_NAME` | RP photo image | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Pack: RP1_Photo.jpg / RP2_Photo.jpg. |
| 134 | `REL_PER_POI_NAME` | RP POI image | **M** |  | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | RP{n}_<doc>.jpg of the RP's identity document. |
| 135 | `REL_PER_PER_ADDRESS_NAME` | RP permanent-address proof image | CM | REL_PER_ADD_PROF != '' | 50 | `IMAGE_FILE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | RP{n}_<doc>.jpg of REL_PER_ADD_PROF document. |
| 136 | `REL_PER_ADD_LINE1` | RP permanent line 1 | **M** |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 137 | `REL_PER_ADD_LINE2` | RP permanent line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 138 | `REL_PER_ADD_LINE3` | RP permanent line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 139 | `REL_PER_ADD_CITY` | RP permanent city | **M** |  | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 140 | `REL_PER_ADD_DISTRICT` | RP permanent district | **M** |  | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 141 | `REL_PER_ADD_PIN` | RP permanent PIN | **M** |  | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 142 | `REL_PER_ADD_STATE` | RP permanent state | **M** |  | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 143 | `REL_PER_ADD_COUNTRY` | RP permanent country | **M** |  | 2 | IN | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 144 | `REL_PER_ADD_PROF` | RP permanent address proof (POA master) | **M** |  | 2 | 01, 02, 03, 04, 05, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Individual documents only (no simplified / LE codes). Its number must be in the matching RP ID column. |
| 145 | `REL_PER_SAMEASPERM_ADD_FLAG` | RP current = permanent flag | **M** |  | 1 | Y, N | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 146 | `REL_PER_CURR_ADD_LINE1` | RP current line 1 | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Flag Y: copy of permanent. Flag N: a different address. |
| 147 | `REL_PER_CURR_ADD_LINE2` | RP current line 2 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 148 | `REL_PER_CURR_ADD_LINE3` | RP current line 3 | O |  | 55 | `ADDRESS_LINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 149 | `REL_PER_CURR_ADD_CITY` | RP current city | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 50 | `CITY` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 150 | `REL_PER_CURR_ADD_DISTRICT` | RP current district | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 50 | MASTER:pincode_master.csv (by PIN) | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 151 | `REL_PER_CURR_ADD_PIN` | RP current PIN | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 6 | `PIN` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 152 | `REL_PER_CURR_ADD_STATE` | RP current state | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 2 | `STATE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 153 | `REL_PER_CURR_ADD_COUNTRY` | RP current country | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 3 | IN | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 154 | `REL_PER_CURR_ADD_PROOF` | RP current address proof (POA master) | CM | REL_PER_SAMEASPERM_ADD_FLAG == 'N' | 2 | 01, 02, 03, 04, 05, 08, 09, 10 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Flag N: a document DIFFERENT from REL_PER_ADD_PROF and its ID number MUST be in the matching RP ID column (01 UID, 02 PASSPORT, 03 DRIVING, 04 VOTER, 05 NREGA, 08 POPULATION_LET, 09 EKYC, 10 OFFLINE_UID). Flag Y: = REL_PER_ADD_PROF. |
| 155 | `REL_PER_TEL_STDCODE` | RP res STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 156 | `REL_PER_TEL_NO` | RP res tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 157 | `REL_PER_OFFTEL_STDCODE` | RP office STD | O |  | 4 | `STD` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 158 | `REL_PER_OFFTEL_NO` | RP office tel | O |  | 10 | `LANDLINE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 159 | `REL_PER_MOBILE_CODE` | RP mobile ISD | O |  | 3 | `ISD` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 160 | `REL_PER_MOBILE_NO` | RP mobile | O |  | 10 | `MOBILE_IN` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 161 | `REL_PER_EMAILID` | RP email | O |  | 100 | `EMAIL` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 162 | `REL_PER_DECL_DATETIME` | RP declaration date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 163 | `REL_PER_DECL_PLACE` | RP declaration place | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 164 | `REL_PER_KYC_VERIFY_DATETIME` | RP KYC verification date | **M** |  | 10 | `DATE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 165 | `REL_PER_TYPE_OF_DOC` | RP type of document | **M** |  | 2 | 01, 02, 03, 04, 05, 06 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 166 | `REL_PER_KYC_VERIFY_NAME` | RP KYC verifier name | **M** |  | 150 | `NAME` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 167 | `REL_PER_KYC_VERIFY_DESGN` | RP KYC verifier designation | **M** |  | 50 | `DESIGNATION` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 168 | `REL_PER_KYC_VERIFY_BRANCH` | RP KYC verifier branch | **M** |  | 50 | `PLACE` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 169 | `REL_PER_KYC_VERIFY_EMP_CODE` | RP KYC verifier emp code | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 170 | `REL_PER_ORG_NAME` | RP organisation name | **M** |  | 150 | config.ORG_NAME | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 171 | `REL_PER_ORG_CODE` | RP organisation code | **M** |  | 20 | config.ORG_CODE | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 | Always = ORG_CODE, even in DVS. |
| 172 | `REL_PER_DIN_NUMBER` | RP DIN | CM | REL_TYPE == '4' | 8 | `DIN` | REL_TYPE == '4' | CKYC_FIN_DATA_LE_RP · Detail 40 | Mandatory for Director (4); blank otherwise. |
| 173 | `FIN_RP_CREATED_TIME` | System: RP created time | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system |  |
| 174 | `CUST_ID` | Parent customer id | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_LE_RP · system | = CUSTOMER_REFERENCE_NUMBER of the minor / legal entity. On LE continuation rows it is the only link to the entity. |
| 175 | `REL_PER_CUST_ID` | RP customer id | **M** |  | 50 | `ALNUM` | ALWAYS | CKYC_FIN_DATA_LE_RP · system | Unique per RP. Pack COSR + 8 digits. |
| 176 | `REL_PER_CONST` | System field: REL_PER_CONST | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 177 | `REL_PER_APPLICATION_DOC_ID` | System field: REL_PER_APPLICATION_DOC_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 178 | `REL_PER_PHOTO_DOC_ID` | System field: REL_PER_PHOTO_DOC_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 179 | `REL_PER_POI_DOC_ID` | System field: REL_PER_POI_DOC_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 180 | `FIN_FIN_ID` | System field: FIN_FIN_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 181 | `FIN_API_RP_ID` | System field: FIN_API_RP_ID | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 182 | `REL_PER_DIS_FLAG` | RP differently abled flag | **M** |  | 1 | 0, 1 | ALWAYS | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 183 | `REL_PER_DIS_TYPE` | RP impairment type | CM | REL_PER_DIS_FLAG == '1' | 2 | MASTER:type_of_impairment.csv | REL_PER_DIS_FLAG == '1' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 184 | `REL_PER_DIS_PERCENT` | RP impairment % | CM | REL_PER_DIS_FLAG == '1' | 3 | `PERCENT` | REL_PER_DIS_FLAG == '1' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 185 | `REL_PER_DIS_UDID_NUMBER` | RP UDID | CM | REL_PER_DIS_FLAG == '1' | 18 | `UDID` | REL_PER_DIS_FLAG == '1' | CKYC_FIN_DATA_LE_RP · Detail 40 |  |
| 186 | `REL_PER_POI_DCM_CODE` | System field: REL_PER_POI_DCM_CODE | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 187 | `REL_PER_PHOTO_DCM_CODE` | System field: REL_PER_PHOTO_DCM_CODE | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 188 | `REL_PER_APPLICATION_DCM_CODE` | System field: REL_PER_APPLICATION_DCM_CODE | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
| 189 | `REL_PER_CURR_ADDRESS_NAME` | System field: REL_PER_CURR_ADDRESS_NAME | SYSTEM (blank) |  |  |  | BLANK | CKYC_FIN_DATA_LE_RP · system | System-populated - keep blank in upload CSV. |
