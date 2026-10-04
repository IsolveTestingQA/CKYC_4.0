"""Builds ckyc_cosmos_field_spec.json - the machine-readable CKYC/CERSAI V1.3 field spec
for the Cosmos iFlow bulk CSV (Individual 99 cols, Minor 198 cols, Legal Entity 189 cols).

Sources (see README): CERSAI Bulk File Structure V1.3 (Detail 20/30/40/70 + masters),
iFlow CKYC Data Format for Individuals V1.3, CKYC_Field_Length_Dropdown_Guide_V1.3_FIXED,
iSolve KYC Data Mapping Guide (Legal), Cosmos 30-Sep-2026 packs (live convention) and user rules.
"""
import json
from pathlib import Path

HERE = Path(__file__).parent
OUT = HERE.parent  # the spec folder
HDR = {"IND": HERE / "hdr_01_Individual.txt", "MIN": HERE / "hdr_02_Minor_With_RP.txt",
       "LE": HERE / "hdr_04_Legal_Entity_Multi_RP.txt"}

# ---------------------------------------------------------------- constants (edit per bank / env)
CONFIG = {
    "bank": "COSMOS CO OP BANK LTD", "ORG_CODE": "IN0308", "ORG_NAME": "COSMOS CO OP BANK LTD", "BRID": "001",
    "image_naming_mode": "DOCUMENT",
    "csv_delimiter": "|", "csv_encoding": "utf-8-sig (UTF-8 with BOM)", "date_format": "DD-MM-YYYY",
    "file_name_pattern": {"IND_MIN": r"^CKYC_DATA_IN_\d{8}_\d{2,}\.csv$", "LE": r"^CKYC_DATA_LE_\d{8}_\d{2,}\.csv$"},
    "customer_ref_pattern": {"IND": r"^COSI\d{8}$", "MIN": r"^COSM\d{8}$", "LE": r"^COSL\d{8}$", "RP": r"^COSR\d{8}$"},
    "email_domain": "cosmos.in",
    "pack_dates": {"DECL_DATETIME": "02-09-2026", "KYC_VERIFY_DATETIME": "03-09-2026"},
    "image_root_folder": "Common_Images/<CUSTOMER_REFERENCE_NUMBER>/",
    "pack_folders": {"purposes": ["01_POSITIVE", "02_DVS", "03_IMAGE_PROCESSING"],
                     "categories": ["01_Individual", "02_Minor_With_RP", "03_Minor_Without_RP", "04_Legal_Entity_Multi_RP"]},
}

# ---------------------------------------------------------------- value formats
FORMATS = {
    "CODE2": {"regex": r"^\d{2}$", "desc": "2-digit code"},
    "NAME": {"regex": r"^[A-Za-z]+( [A-Za-z]+)*$", "desc": "Letters only, single space between words. No digits/special characters (CERSAI allows single quote ' in father/spouse name only).", "ok": "RAJESH KUMAR", "bad": "RAJ@SH"},
    "PLACE": {"regex": r"^[A-Za-z]+( [A-Za-z]+)*$", "desc": "Plain place/branch name: letters, single spaces, no digits, no double space.", "ok": "NEW DELHI", "bad": "CHN02"},
    "ENTITY_NAME": {"regex": r"^[A-Za-z0-9&().'-]+( [A-Za-z0-9&().'-]+)*$", "desc": "Entity name: letters/digits and & ( ) . ' - with single spaces.", "ok": "KAVERI TEXTILES PRIVATE LIMITED", "bad": ""},
    "ADDRESS_LINE": {"regex": r"^[A-Za-z0-9]+( [A-Za-z0-9]+)*$", "desc": "Address line: letters/digits, single spaces, no special characters (iFlow rule). Max 55.", "ok": "NO 183 CHURCH STREET", "bad": "NO#183, CHURCH ST"},
    "CITY": {"regex": r"^[A-Za-z]+( [A-Za-z]+)*$", "desc": "City/Town/Village: letters and single spaces. Max 50.", "ok": "CHENNAI", "bad": "CHENNAI-2"},
    "DATE": {"regex": r"^(0[1-9]|[12]\d|3[01])-(0[1-9]|1[0-2])-(19|20)\d{2}$", "desc": "DD-MM-YYYY and a real calendar date.", "ok": "02-09-2026", "bad": "2026/09/02"},
    "PIN": {"regex": r"^[1-9]\d{5}$", "desc": "6 digits, must exist in CERSAI Pincode master; district + state must match that PIN.", "ok": "600002", "bad": "60000A"},
    "STATE": {"master": "state_master.csv", "desc": "2-char CERSAI State Master code (TN, MH, KA ...).", "ok": "TN", "bad": "TAMILNADU"},
    "COUNTRY": {"master": "country_code_iso3166.csv", "desc": "ISO 3166 2-char code. India = IN.", "ok": "IN", "bad": "IND"},
    "PAN": {"regex": r"^[A-Z]{5}\d{4}[A-Z]$", "desc": "PAN = 5 letters + 4 digits + 1 letter. 4th letter = holder type (P person, C company, F firm/LLP, T trust, H HUF, A AOP, B BOI, G govt, J AJP, L local authority).", "ok": "ABCPK1234L", "bad": "12345ABCD1"},
    "PAN_OR_FORM60": {"regex": r"^([A-Z]{5}\d{4}[A-Z]|FORM60)$", "desc": "Either a valid PAN or the literal text FORM60.", "ok": "FORM60", "bad": "FORM 60"},
    "AADHAAR": {"regex": r"^[2-9]\d{11}$", "checksum": "verhoeff", "desc": "12 digits, first digit 2-9, Verhoeff checksum valid. No X/x/*, no spaces, no masking in the CSV (images show it masked XXXX XXXX 1234). CERSAI stores only last 4.", "ok": "234567890124", "bad": "XXXXXXXX1234"},
    "PASSPORT": {"regex": r"^[A-Z]\d{7}$", "desc": "Indian passport: 1 letter + 7 digits (CERSAI max 20).", "ok": "M2364382", "bad": "M23@4382"},
    "VOTER_EPIC": {"regex": r"^[A-Z]{3}\d{7}$", "desc": "Voter ID (EPIC): 3 letters + 7 digits (CERSAI max 20).", "ok": "SKT2102993", "bad": "SK2102993"},
    "DRIVING_LICENCE": {"regex": r"^[A-Z]{2}\d{2}(19|20)\d{2}\d{7}$", "desc": "DL: state code(2) + RTO(2) + issue year(4) + 7 digits = 15 chars (CERSAI max 20).", "ok": "TN7820171327481", "bad": "TN78@0171327481"},
    "NREGA": {"regex": r"^[A-Z]{2}\d{14}$", "desc": "NREGA job card (pack format): state code(2) + 14 digits. CERSAI max 40 (applicant) / 20 (RP column).", "ok": "TN17260081128167", "bad": "TN17@60081128167"},
    "NPR": {"regex": r"^[A-Z0-9]{1,20}$", "desc": "NPR letter reference - format NOT confirmed by CERSAI (max 20 alphanumeric). Not used in current packs.", "ok": "", "bad": ""},
    "LAST4_AADHAAR": {"regex": r"^\d{4}$", "desc": "Exactly 4 digits = last 4 digits of the person's Aadhaar (E-KYC / Offline Aadhaar).", "ok": "1234", "bad": "12#X"},
    "DIN": {"regex": r"^\d{8}$", "desc": "Director Identification Number: exactly 8 digits.", "ok": "04363701", "bad": "4363701"},
    "CIN": {"regex": r"^[LU]\d{5}[A-Z]{2}(19|20)\d{2}(PLC|PTC)\d{6}$", "desc": "CIN (21): L/U + 5-digit industry + state(2) + year(4) + PLC/PTC + 6 digits. Public Ltd (E) = L..PLC, Private Ltd (D) = U..PTC.", "ok": "U92727TN2008PTC863802", "bad": "U9272TN2008PTC863802"},
    "LLPIN": {"regex": r"^[A-Z]{3}-?\d{4}$", "desc": "LLP Identification Number: 3 letters + 4 digits (AAA-1234 / AAA1234).", "ok": "ABC1234", "bad": "AB12345"},
    "GSTIN": {"regex": r"^\d{2}[A-Z]{5}\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", "checksum": "gstin", "desc": "GSTIN (15): GST state code(2) + PAN(10) + entity no + Z + checksum. Chars 3-12 must equal PAN_OR_FORM60, first 2 = GST code of PERM_ADD_STATE.", "ok": "33PLOCS9829E5ZS", "bad": "33PLOCS9829E5@@"},
    "REG_CERT": {"regex": r"^(REG[A-Z]{2}(19|20)\d{2}\d{6}|\d{2}[A-Z]{5}\d{4}[A-Z][1-9A-Z]Z[0-9A-Z])$", "desc": "Registration Certificate number: pack format REG + state(2) + year(4) + 6 digits (firms/trusts) OR the GSTIN (companies/LLPs).", "ok": "REGTN2015335843", "bad": "REG-TN-2015"},
    "DEEMED_11": {"regex": r"^EB\d{12}$", "desc": "Utility bill consumer/bill no (pack format EB + 12 digits).", "ok": "EB123456789012", "bad": ""},
    "DEEMED_12": {"regex": r"^PTR\d{12}$", "desc": "Property / municipal tax receipt no (pack format PTR + 12 digits).", "ok": "PTR123456789012", "bad": ""},
    "DEEMED_13": {"regex": r"^PPO\d{12}$", "desc": "Pension Payment Order no (pack format PPO + 12 digits).", "ok": "PPO123456789012", "bad": ""},
    "DEEMED_14": {"regex": r"^ALT\d{12}$", "desc": "Allotment letter no (pack format ALT + 12 digits).", "ok": "ALT123456789012", "bad": ""},
    "MOBILE_IN": {"regex": r"^[6-9]\d{9}$", "desc": "Indian mobile: 10 digits starting 6-9 (when ISD = 91). Else up to 20 digits.", "ok": "9876543210", "bad": "987654321"},
    "ISD": {"regex": r"^\d{1,3}$", "desc": "ISD code digits only (India = 91), no + sign.", "ok": "91", "bad": "+91"},
    "STD": {"regex": r"^\d{2,4}$", "desc": "STD code 2-4 digits, no leading 0.", "ok": "44", "bad": "044-"},
    "LANDLINE": {"regex": r"^\d{6,8}$", "desc": "Landline number 6-8 digits (max 10).", "ok": "22002532", "bad": "2200-2532"},
    "EMAIL": {"regex": r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$", "desc": "Valid e-mail.", "ok": "cosi30090001@cosmos.in", "bad": "cosi30090001@"},
    "IMAGE_FILE": {"regex": r"^[A-Za-z0-9_]+\.(jpg|jpeg|pdf|tif|tiff|JPG|JPEG|PDF|TIF|TIFF)$", "desc": "File name only (no folder), extension jpg/jpeg/pdf/tif/tiff; must exist in Common_Images/<CUSTOMER_REFERENCE_NUMBER>/ unless the image-processing summary says MISSING.", "ok": "Aadhar.jpg", "bad": "C:\\img\\Aadhar.jpg"},
    "CKYC_NO_IND": {"regex": r"^[1-6]\d{13}$", "desc": "14-digit CKYC number, first digit 1-6 = Individual (UPDATE only)."},
    "CKYC_NO_LE": {"regex": r"^[7-9]\d{13}$", "desc": "14-digit CKYC number, first digit 7-9 = Legal Entity (UPDATE only)."},
    "UDID": {"regex": r"^[A-Z]{2}\d{16}$", "desc": "UDID: 2 letters + 16 digits."},
    "PERCENT": {"regex": r"^(100|[1-9]?\d)$", "desc": "0-100."},
    "ALNUM": {"regex": r"^[A-Za-z0-9]+$", "desc": "Letters/digits only."},
    "DESIGNATION": {"regex": r"^[A-Za-z]+( [A-Za-z]+)*$", "desc": "Designation text (letters/single spaces).", "ok": "BRANCH HEAD", "bad": "99999"},
    "NUM": {"regex": r"^\d+$", "desc": "Digits only."},
}

# ---------------------------------------------------------------- code lists (masters)
PREFIX = ["MR", "MRS", "MS", "MISS", "DR"]
MASTERS = {
    "APPL_TYPE": {"01": "New", "03": "Update (separate file, never mixed with 01)"},
    "ACC_TYPE": {"01": "Normal", "04": "OTP based E-KYC (POI must be H)", "05": "Minor", "02": "Small - NOT allowed for new upload", "03": "Simplified - NOT allowed for new upload"},
    "GENDER": {"M": "Male", "F": "Female", "T": "Transgender"},
    "FORS_FLG": {"01": "Father name given", "02": "Spouse name given"},
    "RES_STATUS": {"01": "Resident Individual", "02": "Non Resident Indian", "03": "Foreign National", "04": "Person of Indian Origin"},
    "DIS_FLAG": {"0": "Not differently abled", "1": "Differently abled (then DIS_TYPE, DIS_PERCENT, DIS_UDID_NUMBER mandatory)"},
    "YN": {"Y": "Yes", "N": "No"},
    "COMM_ADDRESS_TYPE": {"01": "Resident/Business", "02": "Residential", "03": "Business", "04": "Registered Office", "05": "Unspecified"},
    "TYPE_OF_DOC_IND": {"01": "Certified copies", "02": "E-KYC data from UIDAI", "03": "Offline verification data", "04": "Digital KYC", "05": "Equivalent e-document", "06": "Video based KYC"},
    "TYPE_OF_DOC_LE": {"01": "Certified copies", "02": "Equivalent e-document"},
    "UPDATE_FLAG": {"01": "Yes - section updated", "02": "No"},
    "ADD_DEL_REL_PER": {"01": "Add related person", "02": "Delete related person (UPDATE only)"},
    "MARITAL_STATUS": {"M": "Married", "S": "Single (pack convention; not a CERSAI upload field)"},
    "IDENTITY_CODE_IND": {"A": "Passport", "B": "Voter ID", "D": "Driving Licence", "E": "Proof of Possession of Aadhaar", "F": "NREGA Job Card", "G": "National Population Register Letter", "H": "E-KYC Authentication", "I": "Offline Verification of Aadhaar", "J": "Foreign govt / embassy document (RES_STATUS 03 only)"},
    "IDENTITY_CODE_IND_NOT_ALLOWED": {"C": "PAN - download only", "Z": "Others - download only", "S01": "Simplified - download only", "S02": "Simplified - download only"},
    "IDENTITY_CODE_LE": {"01": "OVD of person authorised to transact (no ID no.)", "02": "Certificate of Incorporation/Formation (ID no. yes, 60)", "03": "Registration Certificate (ID no. yes, 60)", "04": "MOA & AOA", "05": "Partnership Deed", "06": "Trust Deed", "07": "Board/Managing Committee Resolution", "08": "Power of Attorney", "09": "Activity Proof 1 (Sole Prop)", "10": "Activity Proof 2 (Sole Prop)"},
    "POA_IND": {"01": "Proof of Possession of Aadhaar", "02": "Passport", "03": "Driving License", "04": "Voters Identity Card", "05": "NREGA Job Card", "08": "National Population Register Letter", "09": "E-KYC Authentication", "10": "Offline verification of Aadhaar", "16": "Foreign govt / embassy document"},
    "POA_DEEMED_CURRENT": {"11": "Utility Bill (<= 2 months old)", "12": "Property or Municipal Tax Receipt", "13": "Pension Payment Order", "14": "Letter of Allotment of Accommodation", "15": "Self Declaration (only when POI&A is Aadhaar)"},
    "POA_LE": {"06": "Certificate of Incorporation/Formation", "07": "Registration Certificate", "99": "Others (PERM_ADD_PROOF_DESC mandatory)"},
    "CONST_TYPE_LE": {"A": "Sole Proprietorship", "B": "Partnership Firm", "C": "HUF", "D": "Private Limited Company", "E": "Public Limited Company", "F": "Society", "G": "AOP/BOI", "H": "Trust", "I": "Liquidator", "J": "Limited Liability Partnership", "K": "Artificial Liability Partnership", "L": "Public Sector Banks", "M": "Central/State Govt Dept/Agency", "N": "Section 8 Company", "O": "Artificial Juridical Person", "P": "International Org/Embassy", "Q": "Not Categorized", "R": "Others (CONST_TYPE_OTHERS mandatory)", "S": "Foreign Portfolio Investors"},
    "REL_TYPE": {"1": "Guardian of minor", "2": "Assignee", "3": "Authorised Representative", "4": "Director (DIN mandatory)", "5": "Promoter", "6": "Karta", "7": "Trustee", "8": "Partner", "9": "Proprietor", "10": "Court Appointed Official", "11": "Beneficiary", "12": "Authorised Signatory", "13": "Beneficial Owner", "14": "Power of Attorney holder", "15": "Other (REL_TYPE_OTHERS mandatory)"},
    "DOCUMENT_MASTER_IMAGE": {"02": "Photograph", "04": "Aadhaar (first 8 digits masked on image)", "05": "Passport", "06": "Driving License", "07": "Voters Identity Card", "08": "NREGA Job Card", "09": "Signature (optional)", "35": "NPR Letter", "36": "E-KYC Authentication", "37": "Offline verification of Aadhaar", "18": "LE - OVD of authorised person", "19": "LE - Certificate of Incorporation/Formation", "20": "LE - Registration Certificate", "21": "LE - MOA/AOA", "22": "LE - Partnership Deed", "23": "LE - Trust Deed", "24": "LE - Board Resolution", "25": "LE - Power of Attorney", "26": "LE - Activity Proof 1", "27": "LE - Activity Proof 2", "98": "LE - Other", "97": "Death certificate"},
}

# ---------------------------------------------------------------- document cross-map (the heart of POI / POA / ID checks)
DOC_MAP = [
    # doc, POI code, POI_CATEGORY text, POA code, POA_CATEGORY text, number format, RP ID column, image (DOCUMENT mode), CERSAI image doc code, minor allowed
    {"doc": "AADHAAR", "poi_code": "E", "poi_category": "Proof of Possession of Aadhaar", "poa_code": "01", "poa_category": "Proof of Possession of Aadhaar",
     "number_format": "AADHAAR", "rp_id_column": "REL_PER_UID", "image_file": "Aadhar.jpg", "image_doc_code": "04", "minor_allowed": True, "expiry": "blank"},
    {"doc": "PASSPORT", "poi_code": "A", "poi_category": "Passport", "poa_code": "02", "poa_category": "Passport",
     "number_format": "PASSPORT", "rp_id_column": "REL_PER_PASSPORT", "image_file": "Passport.jpg", "image_doc_code": "05", "minor_allowed": True, "expiry": "mandatory future date (pack)"},
    {"doc": "DRIVING_LICENCE", "poi_code": "D", "poi_category": "Driving Licence", "poa_code": "03", "poa_category": "Driving License",
     "number_format": "DRIVING_LICENCE", "rp_id_column": "REL_PER_DRIVING", "image_file": "DrivingLicense.jpg", "image_doc_code": "06", "minor_allowed": False, "expiry": "mandatory future date (pack)"},
    {"doc": "VOTER_ID", "poi_code": "B", "poi_category": "Voter ID", "poa_code": "04", "poa_category": "Voters Identity Card",
     "number_format": "VOTER_EPIC", "rp_id_column": "REL_PER_VOTER", "image_file": "VoterID.jpg", "image_doc_code": "07", "minor_allowed": False, "expiry": "blank"},
    {"doc": "NREGA", "poi_code": "F", "poi_category": "NREGA Job Card", "poa_code": "05", "poa_category": "NREGA Job Card",
     "number_format": "NREGA", "rp_id_column": "REL_PER_NREGA", "image_file": "NREGA.jpg", "image_doc_code": "08", "minor_allowed": True, "expiry": "blank"},
    {"doc": "NPR_LETTER", "poi_code": "G", "poi_category": "National Population Register Letter", "poa_code": "08", "poa_category": "National Population Register Letter",
     "number_format": "NPR", "rp_id_column": "REL_PER_POPULATION_LET", "image_file": "NPR.jpg", "image_doc_code": "35", "minor_allowed": True, "expiry": "blank", "note": "not used in current packs"},
    {"doc": "EKYC", "poi_code": "H", "poi_category": "E-KYC Authentication", "poa_code": "09", "poa_category": "E-KYC Authentication",
     "number_format": "LAST4_AADHAAR", "rp_id_column": "REL_PER_EKYC", "image_file": "EKYC.jpg", "image_doc_code": "36", "minor_allowed": True, "expiry": "blank", "note": "RP current-address proof only in current packs"},
    {"doc": "OFFLINE_AADHAAR", "poi_code": "I", "poi_category": "Offline Verification of Aadhaar", "poa_code": "10", "poa_category": "Offline verification of Aadhaar",
     "number_format": "LAST4_AADHAAR", "rp_id_column": "REL_PER_OFFLINE_UID", "image_file": "OfflineAadhaar.jpg", "image_doc_code": "37", "minor_allowed": True, "expiry": "blank", "note": "RP current-address proof only in current packs"},
]
LE_DOC_MAP = [
    {"doc": "CERTIFICATE_OF_INCORPORATION", "poi_code": "02", "poa_code": "06", "category": "Certificate of Incorporation/Formation",
     "number_format": "CIN (D/E) or LLPIN (J)", "image_file": "IncorporationCertificate.jpg", "image_doc_code": "19", "constitutions": ["D", "E", "J"]},
    {"doc": "REGISTRATION_CERTIFICATE", "poi_code": "03", "poa_code": "07", "category": "Registration Certificate",
     "number_format": "REG_CERT (B/H) or GSTIN (D/E/J)", "image_file": "RegistrationCertificate.jpg", "image_doc_code": "20", "constitutions": ["B", "D", "E", "H", "J"]},
]
DEEMED_MAP = [
    {"code": "11", "category": "Utility Bill", "number_format": "DEEMED_11", "age_rule": "any"},
    {"code": "12", "category": "Property or Municipal Tax Receipt", "number_format": "DEEMED_12", "age_rule": "any"},
    {"code": "13", "category": "Pension Payment Order", "number_format": "DEEMED_13", "age_rule": "applicant age >= 58 (pack rule); never for Minor"},
    {"code": "14", "category": "Letter of Allotment of Accommodation", "number_format": "DEEMED_14", "age_rule": "adults only; never for Minor"},
]
LE_CONST_RULES = {
    "D": {"name_suffix": "PRIVATE LIMITED", "pan_4th": "C", "rel_type": "4", "coi": "CIN (U...PTC...)"},
    "E": {"name_suffix": "LIMITED", "pan_4th": "C", "rel_type": "4", "coi": "CIN (L...PLC...)"},
    "J": {"name_suffix": "LLP", "pan_4th": "F", "rel_type": "8", "coi": "LLPIN"},
    "B": {"name_suffix": "AND COMPANY", "pan_4th": "F", "rel_type": "8", "coi": "none - Registration Certificate only"},
    "H": {"name_suffix": "TRUST", "pan_4th": "T", "rel_type": "7", "coi": "none - Registration Certificate only"},
}
GST_STATE = {"JK": "01", "HP": "02", "PB": "03", "CH": "04", "UK": "05", "UA": "05", "HR": "06", "DL": "07", "RJ": "08", "UP": "09",
             "BR": "10", "SK": "11", "AR": "12", "NL": "13", "MN": "14", "MZ": "15", "TR": "16", "ML": "17", "AS": "18", "WB": "19",
             "JH": "20", "OD": "21", "OR": "21", "CG": "22", "MP": "23", "GJ": "24", "DD": "26", "DN": "26", "MH": "27", "KA": "29",
             "GA": "30", "LD": "31", "KL": "32", "TN": "33", "PY": "34", "AN": "35", "TG": "36", "TS": "36", "AP": "37", "LA": "38"}


# ---------------------------------------------------------------- field definitions
def F(label, status, maxlen=None, fmt=None, enum=None, when=None, pack="ALWAYS", rec="20", notes="", sec=""):
    return {"label": label, "status": status, "when": when, "max_len": maxlen, "format": fmt, "enum": enum,
            "pack_fill": pack, "cersai_record": rec, "notes": notes, "section": sec}


NEWBLANK = "BLANK_FOR_NEW"
SYS = "SYSTEM_BLANK"
FLAG_N = "PERM_TO_COMM_FLG == 'N'"
RP_N = "REL_PER_SAMEASPERM_ADD_FLAG == 'N'"

APPLICANT = {  # Individual / Minor applicant block (cols 1-99)
    "APPL_TYPE": F("Application type", "M", 2, None, ["01"], sec="Application", notes="01 = NEW. UPDATE (03) goes in a separate file."),
    "BRID": F("Branch code", "M", 10, "ALNUM", ["CONFIG:BRID"], sec="Hierarchy", notes="Branch code registered with CERSAI under the region. Never the region code."),
    "CONST_TYPE": F("Constitution type", "M", 2, None, ["01"], sec="Application", notes="Individual = 01 (iFlow static value; CERSAI master shows '1')."),
    "ACC_TYPE": F("Account type", "M", 2, None, ["01", "04"], sec="Application", notes="IND 01 (04 = OTP e-KYC, POI must be H). Minor = 05."),
    "CKYC_REF_NUM": F("CKYC number", NEWBLANK, 14, "CKYC_NO_IND", when="APPL_TYPE == '03'", pack="BLANK", sec="Application", notes="Blank for NEW. Mandatory for UPDATE (14 digits, starts 1-6)."),
    "APPL_NAME_PREFIX": F("Applicant name prefix", "M", 5, None, PREFIX, sec="Name"),
    "APPL_FIRST_NAME": F("Applicant first name", "M", 50, "NAME", sec="Name"),
    "APPL_MIDDLE_NAME": F("Applicant middle name", "O", 50, "NAME", sec="Name"),
    "APPL_LAST_NAME": F("Applicant last name", "M", 50, "NAME", sec="Name", notes="CERSAI: mandatory if constitution 01 (iFlow sheet marks optional - keep filled)."),
    "FORS_FLG": F("Father/Spouse flag", "CM", 2, None, ["01", "02"], when="FATHERORSPOUSE_FIRST_NAME != ''", sec="Name", notes="01 father, 02 spouse. Pack: always filled; 02 only for married women (prefix MRS)."),
    "FATHERORSPOUSE_NAME_PREFIX": F("Father/Spouse prefix", "CM", 5, None, PREFIX, when="FORS_FLG != ''", sec="Name"),
    "FATHERORSPOUSE_FIRST_NAME": F("Father/Spouse first name", "CM", 50, "NAME", when="FORS_FLG != ''", sec="Name"),
    "FATHERORSPOUSE_MIDDLE_NAME": F("Father/Spouse middle name", "O", 50, "NAME", sec="Name"),
    "FATHERORSPOUSE_LAST_NAME": F("Father/Spouse last name", "O", 50, "NAME", sec="Name"),
    "MOTHER_NAME_PREFIX": F("Mother name prefix", "CM", 5, None, PREFIX, when="MOTHER_FIRST_NAME != ''", sec="Name"),
    "MOTHER_FIRST_NAME": F("Mother first name", "CM", 50, "NAME", when="FORS_FLG == ''", sec="Name", notes="At least one of father / spouse / mother name is mandatory. Pack: always filled."),
    "MOTHER_MIDDLE_NAME": F("Mother middle name", "O", 50, "NAME", sec="Name"),
    "MOTHER_LAST_NAME": F("Mother last name", "O", 50, "NAME", sec="Name"),
    "GENDER": F("Gender", "M", 1, None, ["M", "F", "T"], sec="Personal"),
    "MARITAL_STATUS": F("Marital status", "O", 2, None, ["M", "S"], sec="Personal", rec="iFlow only", notes="Not in CERSAI V1.3 upload (iFlow: Blank). Pack fills M/S; Minor = S."),
    "NATIONALITY": F("Nationality", "O", 2, "COUNTRY", sec="Personal", rec="iFlow only", notes="iFlow: Blank. Pack = IN."),
    "OCC_TYPE": F("Occupation type", "O", 4, "CODE2", sec="Personal", rec="iFlow only", notes="iFlow: Blank, no CERSAI master in V1.3 upload. Pack = 02."),
    "DOB": F("Date of birth", "M", 10, "DATE", sec="Personal", notes="Not future, not equal to upload date. IND age >= 18; Minor age < 18 (at DECL_DATETIME)."),
    "RES_STATUS": F("Residential status", "M", 2, None, list(MASTERS["RES_STATUS"]), sec="Personal", notes="Pack = 01."),
    "DIS_FLAG": F("Differently abled flag", "M", 1, None, ["0", "1"], sec="Personal", notes="Pack = 0."),
    "DIS_TYPE": F("Type of impairment", "CM", 2, None, ["MASTER:type_of_impairment.csv"], when="DIS_FLAG == '1'", pack="DIS_FLAG == '1'", sec="Personal"),
    "DIS_PERCENT": F("Impairment %", "CM", 3, "PERCENT", when="DIS_FLAG == '1'", pack="DIS_FLAG == '1'", sec="Personal"),
    "DIS_UDID_NUMBER": F("UDID number", "CM", 18, "UDID", when="DIS_FLAG == '1'", pack="DIS_FLAG == '1'", sec="Personal"),
    "OTHER_JURI_FLG": F("Tax resident outside India flag", "O", 2, "CODE2", sec="Personal", rec="iFlow only", notes="iFlow: Blank. Pack = 01."),
    "PERM_ADD_TYPE": F("Permanent address type", "O", 2, None, pack="BLANK", sec="Permanent address", notes="Cosmos rule: always BLANK (never 'PER')."),
    "PERM_ADD_LINE1": F("Permanent address line 1", "M", 55, "ADDRESS_LINE", sec="Permanent address"),
    "PERM_ADD_LINE2": F("Permanent address line 2", "O", 55, "ADDRESS_LINE", sec="Permanent address"),
    "PERM_ADD_LINE3": F("Permanent address line 3", "O", 55, "ADDRESS_LINE", sec="Permanent address"),
    "PERM_ADD_CITY": F("Permanent city", "M", 50, "CITY", sec="Permanent address"),
    "PERM_ADD_DIST": F("Permanent district", "M", 50, None, ["MASTER:pincode_master.csv (by PIN)"], sec="Permanent address", notes="Must equal the district of PERM_ADD_PIN in the Pincode master (case-insensitive)."),
    "PERM_ADD_STATE": F("Permanent state", "M", 2, "STATE", sec="Permanent address", notes="Must equal the state of PERM_ADD_PIN."),
    "PERM_ADD_COUNTRY": F("Permanent country", "M", 2, None, ["IN"], sec="Permanent address"),
    "PERM_ADD_PIN": F("Permanent PIN", "M", 6, "PIN", sec="Permanent address"),
    "PERM_ADD_PROOF": F("Permanent address proof (POA master)", "M", 2, None, ["01", "02", "03", "04", "05", "08", "09", "10", "16"], sec="Permanent address", notes="Must equal POA_TYPE. Deemed 11-15 NOT allowed for permanent address."),
    "PERM_ADD_PROOF_DESC": F("Permanent address proof description", "O", 75, None, sec="Permanent address", notes="iFlow: Blank. Pack = POA master name of PERM_ADD_PROOF."),
    "PERM_TO_COMM_FLG": F("Permanent = correspondence flag", "M", 1, None, ["Y", "N"], sec="Permanent address"),
    "COMM_ADDRESS_TYPE": F("Correspondence address type", "CM", 2, None, list(MASTERS["COMM_ADDRESS_TYPE"]), when=FLAG_N, pack=FLAG_N, sec="Correspondence address", notes="Blank when flag Y (pack)."),
    "COMM_Add_Line1": F("Correspondence line 1", "CM", 55, "ADDRESS_LINE", when=FLAG_N, sec="Correspondence address", notes="Flag Y: copy of PERM line 1. Flag N: a different address."),
    "COMM_Add_Line2": F("Correspondence line 2", "O", 55, "ADDRESS_LINE", sec="Correspondence address"),
    "COMM_Add_Line3": F("Correspondence line 3", "O", 55, "ADDRESS_LINE", sec="Correspondence address"),
    "COMM_Add_City": F("Correspondence city", "CM", 50, "CITY", when=FLAG_N, sec="Correspondence address"),
    "COMM_Add_Dist": F("Correspondence district", "CM", 50, None, ["MASTER:pincode_master.csv (by PIN)"], when=FLAG_N, sec="Correspondence address"),
    "COMM_Add_State": F("Correspondence state", "CM", 2, "STATE", when=FLAG_N, sec="Correspondence address"),
    "COMM_Add_Country": F("Correspondence country", "CM", 2, None, ["IN"], when=FLAG_N, sec="Correspondence address"),
    "COMM_Add_Pin": F("Correspondence PIN", "CM", 6, "PIN", when=FLAG_N, sec="Correspondence address"),
    "DECL_DATETIME": F("Declaration date", "M", 10, "DATE", sec="Declaration / KYC", notes="Not future. Pack = 02-09-2026."),
    "DECL_PLACE": F("Declaration place", "M", 50, "PLACE", sec="Declaration / KYC"),
    "KYC_VERIFY_DATETIME": F("KYC verification date", "M", 10, "DATE", sec="Declaration / KYC", notes="Not future. Pack = 03-09-2026."),
    "TYPE_OF_DOC": F("Type of document submitted", "M", 2, None, list(MASTERS["TYPE_OF_DOC_IND"]), sec="Declaration / KYC", notes="Pack = 01."),
    "KYC_VERIFY_NAME": F("KYC verifier name", "M", 150, "NAME", sec="Declaration / KYC"),
    "KYC_VERIFY_DESGN": F("KYC verifier designation", "M", 50, "DESIGNATION", sec="Declaration / KYC"),
    "KYC_VERIFY_BRANCH": F("KYC verifier branch", "M", 50, "PLACE", sec="Declaration / KYC"),
    "KYC_VERIFY_EMP_CODE": F("KYC verifier employee code", "M", 50, "ALNUM", sec="Declaration / KYC"),
    "ORG_NAME": F("Organisation name", "M", 150, None, ["CONFIG:ORG_NAME"], sec="Hierarchy"),
    "ORG_CODE": F("Organisation (FI) code", "M", 7, None, ["CONFIG:ORG_CODE"], sec="Hierarchy"),
    "MINOR": F("Minor flag", "M", 1, None, ["N"], sec="Application", notes="IND = N, Minor = Y."),
    "CUSTOMER_REFERENCE_NUMBER": F("Customer reference number (FI ref)", "M", 14, "ALNUM", sec="Application", notes="Unique across all packs. Image folder name. Pack COSI/COSM + 8 digits."),
    "PHOTO_FILE_NAME": F("Photo image file", "M", 50, "IMAGE_FILE", sec="Images", rec="70", notes="Pack = Photo.jpg (doc code 02)."),
    "POI_FILE_NAME": F("POI image file", "M", 50, "IMAGE_FILE", sec="Identity (POI)", rec="70", notes="Must be the image of POI_TYPE (see DOC_MAP)."),
    "POI_CATEGORY": F("POI category (Identity Code name)", "M", 50, None, sec="Identity (POI)", rec="30", notes="Text name of POI_TYPE from Identity Code master (see DOC_MAP.poi_category)."),
    "POI_TYPE": F("POI type (Identity Code)", "M", 2, None, ["A", "B", "D", "E", "F", "G", "H", "I", "J"], sec="Identity (POI)", rec="30", notes="Pack uses A/B/D/E/F. C, Z, S01, S02 are download-only - never upload."),
    "POI_NUMBER": F("POI number", "M", 20, "BY_POI_TYPE", sec="Identity (POI)", rec="30", notes="Format depends on POI_TYPE (DOC_MAP.number_format)."),
    "POI_EXPIRY_DATE": F("POI expiry date", "O", 10, "DATE", pack="POI_TYPE in ('A','D')", sec="Identity (POI)", notes="Pack: filled (future date) for Passport/DL only; blank otherwise."),
    "POA_FILE_NAME": F("POA image file", "M", 50, "IMAGE_FILE", sec="Address proof (POA)", rec="70", notes="Same file as POI_FILE_NAME when the same document is POI and POA (POIA)."),
    "POA_CATEGORY": F("POA category (POA master name)", "M", 50, None, sec="Address proof (POA)", notes="Text name of POA_TYPE (DOC_MAP.poa_category)."),
    "POA_TYPE": F("POA type (POA master)", "M", 2, None, ["01", "02", "03", "04", "05", "08", "09", "10", "16"], sec="Address proof (POA)"),
    "POA_NUMBER": F("POA number", "M", 20, "BY_POA_TYPE", sec="Address proof (POA)", rec="30"),
    "POA_EXPIRY_DATE": F("POA expiry date", "O", 10, "DATE", pack="POA_TYPE in ('02','03')", sec="Address proof (POA)"),
    "PAN_OR_FORM60": F("PAN or FORM60", "M", 10, "PAN_OR_FORM60", sec="Personal", notes="IND pack: PAN, 4th letter P. Minor: PAN or FORM60."),
    "COMM_ADDRESS_CATEGORY": F("Correspondence address proof category", "CM", 50, None, when=FLAG_N, sec="Correspondence address", notes="Pack always fills. Flag Y = POA_CATEGORY. Flag N = deemed proof name (11-14)."),
    "COMM_ADDRESS_IDTYPE": F("Correspondence address proof type", "CM", 2, None, ["01", "02", "03", "04", "05", "08", "09", "10", "11", "12", "13", "14", "15", "16"], when=FLAG_N, sec="Correspondence address",
                             notes="Pack/user rule: flag Y = POA_TYPE; flag N = deemed proof 11/12/13/14 only (never an OVD). Minor: 11/12 only."),
    "COMM_ADDRESS_NUMBER": F("Correspondence address proof number", "CM", 20, "BY_COMM_IDTYPE", when=FLAG_N, sec="Correspondence address", notes="Flag Y = POA_NUMBER. Flag N = unique bill/receipt no (EB/PTR/PPO/ALT + 12 digits)."),
    "RESIDENCE_TELEPHONE_NO_STD_CODE": F("Residence STD", "O", 4, "STD", sec="Contact", notes="STD and number both or neither."),
    "RESIDENCE_TELEPHONE_NO": F("Residence tel", "O", 10, "LANDLINE", sec="Contact"),
    "OFFICE_TELEPHONE_NO_STD_CODE": F("Office STD", "O", 4, "STD", sec="Contact"),
    "OFFICE_TELEPHONE_NO": F("Office tel", "O", 10, "LANDLINE", sec="Contact"),
    "MOBILE_NO_ISD_CODE": F("Mobile ISD", "O", 3, "ISD", sec="Contact"),
    "MOBILE_NO": F("Mobile", "O", 10, "MOBILE_IN", sec="Contact", notes="ISD 91 -> exactly 10 digits starting 6-9."),
    "FAX_NO_STD_CODE": F("Fax STD", "O", 4, "STD", sec="Contact"),
    "FAX_NO": F("Fax", "O", 10, "LANDLINE", sec="Contact"),
    "EMAIL_ID": F("Email", "O", 50, "EMAIL", sec="Contact"),
    "MOBILE_NO2_ISD_CODE2": F("Mobile 2 ISD", "O", 3, "ISD", sec="Contact"),
    "MOBILE_NO2": F("Mobile 2", "O", 10, "MOBILE_IN", sec="Contact"),
    "EMAIL_ID2": F("Email 2", "O", 50, "EMAIL", sec="Contact"),
}
for fl in ["CUST_NAME_UPDT_FLG", "PER_DET_UPDT_FLG", "ADD_DET_UPDT_FLG", "CONT_DET_UPDT_FLG", "REM_UPDT_FLG", "KYC_VERIFY_UPDT_FLG",
           "ID_DET_UPDT_FLG", "REL_PER_UPDT_FLG", "CONT_PER_UPDT_FLG", "IMG_UPDT_FLG", "IMG_DET_UPDT_FLG"]:
    APPLICANT[fl] = F(fl.replace("_", " ").title(), NEWBLANK, 2, None, ["01", "02"], when="APPL_TYPE == '03'", pack="BLANK",
                      sec="Update flags", notes="Must be BLANK for NEW (01). UPDATE: each 01/02.")

RP = {  # Related-person block (Minor guardian and LE directors/partners/trustees)
    "DECEASED_DATE": F("Deceased date", NEWBLANK, 10, "DATE", pack="BLANK", sec="Other", notes="Keep blank for upload."),
    "REL_TYPE": F("Relationship type", "M", 2, None, list(MASTERS["REL_TYPE"]), sec="Related person", rec="40", notes="Minor = 1 (Guardian). LE: D/E -> 4 Director, B/J -> 8 Partner, H -> 7 Trustee."),
    "REL_TYPE_OTHERS": F("Relationship others text", "CM", 150, None, when="REL_TYPE == '15'", pack="REL_TYPE == '15'", sec="Related person", rec="40"),
    "ADD_DEL_REL_PER": F("Add/Delete RP", "M", 2, None, ["01"], sec="Related person", rec="40", notes="01 = add (NEW)."),
    "REL_PER_KYC_NUM": F("RP CKYC number", "O", 14, "CKYC_NO_IND", pack="BLANK", sec="Related person", rec="40", notes="If given, only RP name is further mandatory."),
    "REL_PER_NAME_PREFIX": F("RP prefix", "M", 5, None, PREFIX, sec="RP name", rec="40"),
    "REL_PER_FIRST_NAME": F("RP first name", "M", 50, "NAME", sec="RP name", rec="40"),
    "REL_PER_MIDDLE_NAME": F("RP middle name", "O", 50, "NAME", sec="RP name", rec="40"),
    "REL_PER_LAST_NAME": F("RP last name", "O", 50, "NAME", sec="RP name", rec="40", notes="Pack: always filled."),
    "REL_PER_MAIDEN_PREFIX": F("RP maiden prefix", "O", 5, None, PREFIX, pack="REL_PER_FATHERORSPOUSE == '02'", sec="RP name", rec="40"),
    "REL_PER_MAIDEN_FIRST_NAME": F("RP maiden first", "O", 50, "NAME", pack="REL_PER_FATHERORSPOUSE == '02'", sec="RP name", rec="40"),
    "REL_PER_MAIDEN_MIDDLE_NAME": F("RP maiden middle", "O", 50, "NAME", pack="REL_PER_FATHERORSPOUSE == '02'", sec="RP name", rec="40"),
    "REL_PER_MAIDEN_LAST_NAME": F("RP maiden last", "O", 50, "NAME", pack="REL_PER_FATHERORSPOUSE == '02'", sec="RP name", rec="40"),
    "REL_PER_FATHERORSPOUSE": F("RP father/spouse flag", "CM", 2, None, ["01", "02"], when="REL_PER_FATHERORSPOUSE_FIRST_NAME != ''", sec="RP name", rec="40"),
    "REL_PER_FATHERORSPOUSE_NAME_PREFIX": F("RP father/spouse prefix", "CM", 5, None, PREFIX, when="REL_PER_FATHERORSPOUSE != ''", sec="RP name", rec="40"),
    "REL_PER_FATHERORSPOUSE_FIRST_NAME": F("RP father/spouse first", "CM", 50, "NAME", when="REL_PER_FATHERORSPOUSE != ''", sec="RP name", rec="40"),
    "REL_PER_FATHERORSPOUSE_MIDDLE_NAME": F("RP father/spouse middle", "O", 50, "NAME", sec="RP name", rec="40"),
    "REL_PER_FATHERORSPOUSE_LAST_NAME": F("RP father/spouse last", "O", 50, "NAME", sec="RP name", rec="40"),
    "REL_PER_MOTHER_NAME_PREFIX": F("RP mother prefix", "CM", 5, None, PREFIX, when="REL_PER_MOTHER_FIRST_NAME != ''", sec="RP name", rec="40", notes="Pack/user rule: RP mother name always filled."),
    "REL_PER_MOTHER_FIRST_NAME": F("RP mother first", "CM", 50, "NAME", when="REL_PER_FATHERORSPOUSE == ''", sec="RP name", rec="40"),
    "REL_PER_MOTHER_MIDDLE_NAME": F("RP mother middle", "O", 50, "NAME", sec="RP name", rec="40"),
    "REL_PER_MOTHER_LAST_NAME": F("RP mother last", "O", 50, "NAME", sec="RP name", rec="40"),
    "REL_PER_DOB": F("RP date of birth", "M", 10, "DATE", sec="RP personal", rec="40", notes="Not future; RP age >= 18."),
    "REL_PER_GENDER": F("RP gender", "M", 1, None, ["M", "F", "T"], sec="RP personal", rec="40"),
    "REL_PER_MARITAL_STATUS": F("RP marital status", "O", 2, None, ["M", "S"], sec="RP personal", rec="iFlow only"),
    "REL_PER_NATIONALITY": F("RP nationality", "M", 2, "COUNTRY", sec="RP personal", rec="40"),
    "REL_PER_RES_STATUS": F("RP residential status", "M", 2, None, list(MASTERS["RES_STATUS"]), sec="RP personal", rec="40"),
    "REL_PER_OCC_TYPE": F("RP occupation", "O", 4, "CODE2", sec="RP personal", rec="iFlow only"),
    "REL_PER_OTHER_JURI_FLG": F("RP other jurisdiction flag", "O", 2, "CODE2", sec="RP personal", rec="iFlow only"),
    "REL_PER_JURI_RES_COUNTRY": F("RP jurisdiction country", "O", 2, "COUNTRY", pack="BLANK", sec="RP personal", rec="iFlow only"),
    "REL_PER_JURI_TIN": F("RP jurisdiction TIN", "O", 20, None, pack="BLANK", sec="RP personal", rec="iFlow only"),
    "REL_PER_BIRTH_COUNTRY": F("RP country of birth", "O", 2, "COUNTRY", sec="RP personal", rec="iFlow only"),
    "REL_PER_BIRTH_CITY": F("RP city of birth", "O", 50, "CITY", sec="RP personal", rec="iFlow only"),
    "REL_PER_PANORFORM60": F("RP PAN or FORM60", "M", 10, "PAN_OR_FORM60", sec="RP identity", rec="40", notes="Pack: PAN with 4th letter P."),
    "REL_PER_UID": F("RP Aadhaar (UID)", "CM", 12, "AADHAAR", when="no_other_rp_id()", sec="RP identity", rec="40", notes="Any ONE of the 8 RP ID columns is mandatory. Pack: UID always filled."),
    "REL_PER_VOTER": F("RP Voter ID", "CM", 20, "VOTER_EPIC", when="rp_doc_used('04')", pack="rp_doc_used('04')", sec="RP identity", rec="40"),
    "REL_PER_NREGA": F("RP NREGA", "CM", 20, "NREGA", when="rp_doc_used('05')", pack="rp_doc_used('05')", sec="RP identity", rec="40"),
    "REL_PER_PASSPORT": F("RP Passport", "CM", 20, "PASSPORT", when="rp_doc_used('02')", pack="rp_doc_used('02')", sec="RP identity", rec="40"),
    "REL_PER_DRIVING": F("RP Driving licence", "CM", 20, "DRIVING_LICENCE", when="rp_doc_used('03')", pack="rp_doc_used('03')", sec="RP identity", rec="40"),
    "REL_PER_POPULATION_LET": F("RP NPR letter", "CM", 20, "NPR", when="rp_doc_used('08')", pack="rp_doc_used('08')", sec="RP identity", rec="40"),
    "REL_PER_EKYC": F("RP E-KYC (last 4 of UID)", "CM", 4, "LAST4_AADHAAR", when="rp_doc_used('09')", pack="rp_doc_used('09')", sec="RP identity", rec="40"),
    "REL_PER_OFFLINE_UID": F("RP Offline Aadhaar (last 4 of UID)", "CM", 4, "LAST4_AADHAAR", when="rp_doc_used('10')", pack="rp_doc_used('10')", sec="RP identity", rec="40"),
    "REL_PER_PHOTO_NAME": F("RP photo image", "M", 50, "IMAGE_FILE", sec="RP images", rec="40", notes="Pack: RP1_Photo.jpg / RP2_Photo.jpg."),
    "REL_PER_POI_NAME": F("RP POI image", "M", 50, "IMAGE_FILE", sec="RP images", rec="40", notes="RP{n}_<doc>.jpg of the RP's identity document."),
    "REL_PER_PER_ADDRESS_NAME": F("RP permanent-address proof image", "CM", 50, "IMAGE_FILE", when="REL_PER_ADD_PROF != ''", sec="RP images", rec="40", notes="RP{n}_<doc>.jpg of REL_PER_ADD_PROF document."),
    "REL_PER_ADD_LINE1": F("RP permanent line 1", "M", 55, "ADDRESS_LINE", sec="RP permanent address", rec="40"),
    "REL_PER_ADD_LINE2": F("RP permanent line 2", "O", 55, "ADDRESS_LINE", sec="RP permanent address", rec="40"),
    "REL_PER_ADD_LINE3": F("RP permanent line 3", "O", 55, "ADDRESS_LINE", sec="RP permanent address", rec="40"),
    "REL_PER_ADD_CITY": F("RP permanent city", "M", 50, "CITY", sec="RP permanent address", rec="40"),
    "REL_PER_ADD_DISTRICT": F("RP permanent district", "M", 50, None, ["MASTER:pincode_master.csv (by PIN)"], sec="RP permanent address", rec="40"),
    "REL_PER_ADD_PIN": F("RP permanent PIN", "M", 6, "PIN", sec="RP permanent address", rec="40"),
    "REL_PER_ADD_STATE": F("RP permanent state", "M", 2, "STATE", sec="RP permanent address", rec="40"),
    "REL_PER_ADD_COUNTRY": F("RP permanent country", "M", 2, None, ["IN"], sec="RP permanent address", rec="40"),
    "REL_PER_ADD_PROF": F("RP permanent address proof (POA master)", "M", 2, None, ["01", "02", "03", "04", "05", "08", "09", "10"], sec="RP permanent address", rec="40", notes="Individual documents only (no simplified / LE codes). Its number must be in the matching RP ID column."),
    "REL_PER_SAMEASPERM_ADD_FLAG": F("RP current = permanent flag", "M", 1, None, ["Y", "N"], sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_LINE1": F("RP current line 1", "CM", 55, "ADDRESS_LINE", when=RP_N, sec="RP current address", rec="40", notes="Flag Y: copy of permanent. Flag N: a different address."),
    "REL_PER_CURR_ADD_LINE2": F("RP current line 2", "O", 55, "ADDRESS_LINE", sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_LINE3": F("RP current line 3", "O", 55, "ADDRESS_LINE", sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_CITY": F("RP current city", "CM", 50, "CITY", when=RP_N, sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_DISTRICT": F("RP current district", "CM", 50, None, ["MASTER:pincode_master.csv (by PIN)"], when=RP_N, sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_PIN": F("RP current PIN", "CM", 6, "PIN", when=RP_N, sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_STATE": F("RP current state", "CM", 2, "STATE", when=RP_N, sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_COUNTRY": F("RP current country", "CM", 3, None, ["IN"], when=RP_N, sec="RP current address", rec="40"),
    "REL_PER_CURR_ADD_PROOF": F("RP current address proof (POA master)", "CM", 2, None, ["01", "02", "03", "04", "05", "08", "09", "10"], when=RP_N, sec="RP current address", rec="40",
                                notes="Flag N: a document DIFFERENT from REL_PER_ADD_PROF and its ID number MUST be in the matching RP ID column (01 UID, 02 PASSPORT, 03 DRIVING, 04 VOTER, 05 NREGA, 08 POPULATION_LET, 09 EKYC, 10 OFFLINE_UID). Flag Y: = REL_PER_ADD_PROF."),
    "REL_PER_TEL_STDCODE": F("RP res STD", "O", 4, "STD", sec="RP contact", rec="40"),
    "REL_PER_TEL_NO": F("RP res tel", "O", 10, "LANDLINE", sec="RP contact", rec="40"),
    "REL_PER_OFFTEL_STDCODE": F("RP office STD", "O", 4, "STD", sec="RP contact", rec="40"),
    "REL_PER_OFFTEL_NO": F("RP office tel", "O", 10, "LANDLINE", sec="RP contact", rec="40"),
    "REL_PER_MOBILE_CODE": F("RP mobile ISD", "O", 3, "ISD", sec="RP contact", rec="40"),
    "REL_PER_MOBILE_NO": F("RP mobile", "O", 10, "MOBILE_IN", sec="RP contact", rec="40"),
    "REL_PER_EMAILID": F("RP email", "O", 100, "EMAIL", sec="RP contact", rec="40"),
    "REL_PER_DECL_DATETIME": F("RP declaration date", "M", 10, "DATE", sec="RP KYC", rec="40"),
    "REL_PER_DECL_PLACE": F("RP declaration place", "M", 50, "PLACE", sec="RP KYC", rec="40"),
    "REL_PER_KYC_VERIFY_DATETIME": F("RP KYC verification date", "M", 10, "DATE", sec="RP KYC", rec="40"),
    "REL_PER_TYPE_OF_DOC": F("RP type of document", "M", 2, None, list(MASTERS["TYPE_OF_DOC_IND"]), sec="RP KYC", rec="40"),
    "REL_PER_KYC_VERIFY_NAME": F("RP KYC verifier name", "M", 150, "NAME", sec="RP KYC", rec="40"),
    "REL_PER_KYC_VERIFY_DESGN": F("RP KYC verifier designation", "M", 50, "DESIGNATION", sec="RP KYC", rec="40"),
    "REL_PER_KYC_VERIFY_BRANCH": F("RP KYC verifier branch", "M", 50, "PLACE", sec="RP KYC", rec="40"),
    "REL_PER_KYC_VERIFY_EMP_CODE": F("RP KYC verifier emp code", "M", 50, "ALNUM", sec="RP KYC", rec="40"),
    "REL_PER_ORG_NAME": F("RP organisation name", "M", 150, None, ["CONFIG:ORG_NAME"], sec="RP KYC", rec="40"),
    "REL_PER_ORG_CODE": F("RP organisation code", "M", 20, None, ["CONFIG:ORG_CODE"], sec="RP KYC", rec="40", notes="Always = ORG_CODE, even in DVS."),
    "REL_PER_DIN_NUMBER": F("RP DIN", "CM", 8, "DIN", when="REL_TYPE == '4'", pack="REL_TYPE == '4'", sec="Related person", rec="40", notes="Mandatory for Director (4); blank otherwise."),
    "FIN_RP_CREATED_TIME": F("System: RP created time", SYS, None, pack="BLANK", sec="System", rec="system"),
    "CUST_ID": F("Parent customer id", "M", 50, "ALNUM", sec="Link", rec="system", notes="= CUSTOMER_REFERENCE_NUMBER of the minor / legal entity. On LE continuation rows it is the only link to the entity."),
    "REL_PER_CUST_ID": F("RP customer id", "M", 50, "ALNUM", sec="Link", rec="system", notes="Unique per RP. Pack COSR + 8 digits."),
    "REL_PER_DIS_FLAG": F("RP differently abled flag", "M", 1, None, ["0", "1"], sec="RP personal", rec="40"),
    "REL_PER_DIS_TYPE": F("RP impairment type", "CM", 2, None, ["MASTER:type_of_impairment.csv"], when="REL_PER_DIS_FLAG == '1'", pack="REL_PER_DIS_FLAG == '1'", sec="RP personal", rec="40"),
    "REL_PER_DIS_PERCENT": F("RP impairment %", "CM", 3, "PERCENT", when="REL_PER_DIS_FLAG == '1'", pack="REL_PER_DIS_FLAG == '1'", sec="RP personal", rec="40"),
    "REL_PER_DIS_UDID_NUMBER": F("RP UDID", "CM", 18, "UDID", when="REL_PER_DIS_FLAG == '1'", pack="REL_PER_DIS_FLAG == '1'", sec="RP personal", rec="40"),
}
for s in ["REL_PER_CONST", "REL_PER_APPLICATION_DOC_ID", "REL_PER_PHOTO_DOC_ID", "REL_PER_POI_DOC_ID", "FIN_FIN_ID", "FIN_API_RP_ID",
          "REL_PER_POI_DCM_CODE", "REL_PER_PHOTO_DCM_CODE", "REL_PER_APPLICATION_DCM_CODE", "REL_PER_CURR_ADDRESS_NAME"]:
    RP[s] = F("System field: " + s, SYS, None, pack="BLANK", sec="System", rec="system", notes="System-populated - keep blank in upload CSV.")

LE = {
    "BRID": APPLICANT["BRID"],
    "COMM_ADDRESS_IDTYPE": F("Local address proof type", "CM", 2, None, ["06", "07", "99"], when=FLAG_N, sec="Local address", notes="LE: only 06/07/99 (deemed proofs are Individual-only). Pack = POA_TYPE."),
    "COMM_ADDRESS_NUMBER": F("Local address proof number", "CM", 60, "BY_COMM_IDTYPE", when=FLAG_N, sec="Local address", notes="Pack = POA_NUMBER. iFlow / iSolve DB length 20 - a 21-char CIN exceeds it (see max_len_db)."),
    "CONST_TYPE": F("Constitution type", "M", 2, None, list(MASTERS["CONST_TYPE_LE"]), sec="Entity", notes="Pack uses D, E, J, B, H."),
    "CONST_TYPE_OTHERS": F("Constitution others text", "CM", 20, None, when="CONST_TYPE == 'R'", pack="CONST_TYPE == 'R'", sec="Entity"),
    "ACC_HOLDER_TYPE_FLAG": F("Account holder type flag", "O", 1, None, pack="BLANK", sec="Entity", rec="iFlow only", notes="Not used for LE - blank."),
    "ACC_HOLDER_TYPE": F("Account holder type", "O", 5, None, pack="BLANK", sec="Entity", rec="iFlow only", notes="Not used for LE - blank."),
    "ACC_TYPE": F("Account type", "O", 2, None, pack="BLANK", sec="Entity", notes="Blank for LE."),
    "CKYC_REF_NUM": F("CKYC number", NEWBLANK, 14, "CKYC_NO_LE", when="False", pack="BLANK", sec="Entity", notes="Blank for NEW. UPDATE: 14 digits starting 7-9."),
    "APPL_FULL_NAME": F("Entity name", "M", 150, "ENTITY_NAME", sec="Entity", notes="Unique across packs. Suffix matches constitution (PRIVATE LIMITED / LIMITED / LLP / AND COMPANY / TRUST)."),
    "DATE_OF_INC": F("Date of incorporation", "M", 10, "DATE", sec="Entity", notes="Past date, not the upload date."),
    "PLACE_OF_INC": F("Place of incorporation", "M", 50, "PLACE", sec="Entity"),
    "DATE_OF_COMMENCEMENT": F("Date of commencement", "M", 10, "DATE", sec="Entity", notes=">= DATE_OF_INC, not future."),
    "COUNTRY_OF_INC": F("Country of incorporation", "M", 2, None, ["IN"], sec="Entity"),
    "TIN": F("TIN / GSTIN", "O", 20, "GSTIN", sec="Entity", notes="Pack: GSTIN with embedded PAN = PAN_OR_FORM60 and state code = PERM_ADD_STATE."),
    "TIN_COUNTRY": F("TIN country", "CM", 2, None, ["IN"], when="TIN != ''", sec="Entity"),
    "PAN_OR_FORM60": F("Entity PAN or FORM60", "M", 10, "PAN_OR_FORM60", sec="Entity", notes="4th letter by constitution: D/E -> C, B/J -> F, H -> T."),
    "PERM_ADD_LINE1": APPLICANT["PERM_ADD_LINE1"], "PERM_ADD_LINE2": APPLICANT["PERM_ADD_LINE2"], "PERM_ADD_LINE3": APPLICANT["PERM_ADD_LINE3"],
    "PERM_ADD_CITY": APPLICANT["PERM_ADD_CITY"], "PERM_ADD_DIST": APPLICANT["PERM_ADD_DIST"], "PERM_ADD_STATE": APPLICANT["PERM_ADD_STATE"],
    "PERM_ADD_COUNTRY": APPLICANT["PERM_ADD_COUNTRY"], "PERM_ADD_PIN": APPLICANT["PERM_ADD_PIN"],
    "PERM_ADD_PROOF": F("Registered address proof (POA master)", "M", 2, None, ["06", "07", "99"], sec="Registered address", notes="Must equal POA_TYPE."),
    "PERM_ADD_PROOF_DESC": F("Registered address proof description", "CM", 75, None, when="PERM_ADD_PROOF == '99'", sec="Registered address", notes="Mandatory for 99. Pack fills the master name."),
    "PERM_TO_COMM_FLG": F("Registered = local address flag", "M", 1, None, ["Y", "N"], sec="Registered address"),
    "COMM_ADD_LINE1": F("Local address line 1", "CM", 55, "ADDRESS_LINE", when=FLAG_N, sec="Local address", notes="Indian local address only."),
    "COMM_ADD_LINE2": F("Local address line 2", "O", 55, "ADDRESS_LINE", sec="Local address"),
    "COMM_ADD_LINE3": F("Local address line 3", "O", 55, "ADDRESS_LINE", sec="Local address"),
    "COMM_ADD_CITY": F("Local city", "CM", 50, "CITY", when=FLAG_N, sec="Local address"),
    "COMM_ADD_DIST": F("Local district", "CM", 50, None, ["MASTER:pincode_master.csv (by PIN)"], when=FLAG_N, sec="Local address"),
    "COMM_ADD_STATE": F("Local state", "CM", 2, "STATE", when=FLAG_N, sec="Local address"),
    "COMM_ADD_COUNTRY": F("Local country", "CM", 3, None, ["IN"], when=FLAG_N, sec="Local address"),
    "COMM_ADD_PIN": F("Local PIN", "CM", 6, "PIN", when=FLAG_N, sec="Local address"),
    "RES_TEL_CODE": F("Res STD (LE block)", "O", 4, "STD", sec="Contact", notes="Pack = RESIDENCE_TELEPHONE_NO_STD_CODE."),
    "RES_TEL_NUM": F("Res tel (LE block)", "O", 10, "LANDLINE", sec="Contact"),
    "OFF_TEL_CODE": F("Office STD (LE block)", "O", 4, "STD", sec="Contact"),
    "OFF_TEL_NUM": F("Office tel (LE block)", "O", 10, "LANDLINE", sec="Contact"),
    "MOB_ISD_CODE": F("Mobile ISD (LE block)", "O", 3, "ISD", sec="Contact"),
    "MOB_NUM": F("Mobile (LE block)", "O", 10, "MOBILE_IN", sec="Contact"),
    "FAX_STD_CODE": F("Fax STD (LE block)", "O", 4, "STD", sec="Contact"),
    "FAX_NUM": F("Fax (LE block)", "O", 10, "LANDLINE", sec="Contact"),
    "REMARKS": F("Remarks", "O", 300, None, sec="Other"),
    "DECL_DATETIME": APPLICANT["DECL_DATETIME"], "DECL_PLACE": APPLICANT["DECL_PLACE"], "KYC_VERIFY_DATETIME": APPLICANT["KYC_VERIFY_DATETIME"],
    "TYPE_OF_DOC": F("Type of document submitted", "M", 2, None, list(MASTERS["TYPE_OF_DOC_LE"]), sec="Declaration / KYC"),
    "KYC_VERIFY_NAME": APPLICANT["KYC_VERIFY_NAME"], "KYC_VERIFY_DESGN": APPLICANT["KYC_VERIFY_DESGN"],
    "KYC_VERIFY_BRANCH": APPLICANT["KYC_VERIFY_BRANCH"], "KYC_VERIFY_EMP_CODE": APPLICANT["KYC_VERIFY_EMP_CODE"],
    "ORG_NAME": APPLICANT["ORG_NAME"], "ORG_CODE": APPLICANT["ORG_CODE"],
    "NUM_OF_ID_DET": F("Number of identity details", "O", 2, "NUM", sec="Counts", notes="Pack = 1."),
    "NUM_OF_REL_PER": F("Number of related persons", "M", 3, "NUM", sec="Counts", notes="= number of rows with this CUST_ID (pack = 2)."),
    "NUM_OF_IMG": F("Number of images", "M", 2, "NUM", sec="Counts", notes="Pack = distinct entity document images: 01 (POIA) or 02 (POI != POA)."),
    "CUSTOMER_REFERENCE_NUMBER": F("Customer reference number", "M", 14, "ALNUM", sec="Entity", notes="Unique. Pack COSL + 8 digits."),
    "POI_FILE_NAME": F("Entity POI image", "M", 50, "IMAGE_FILE", sec="Identity (POI)", rec="70"),
    "POI_TYPE": F("Entity POI type (LE Identity Code)", "M", 2, None, list(MASTERS["IDENTITY_CODE_LE"]), sec="Identity (POI)", rec="30", notes="Pack 02 (COI) or 03 (Reg Cert). Never an Individual code (A-J)."),
    "POI_NUMBER": F("Entity POI number", "CM", 60, "BY_LE_POI_TYPE", when="POI_TYPE in ('02','03')", sec="Identity (POI)", rec="30", notes="CERSAI Identity Code 02/03 size 60. iSolve mapping guide shows VARCHAR2(20) - a 21-char CIN exceeds it (see max_len_db)."),
    "POI_EXPIRY_DATE": F("Entity POI expiry", "O", 10, "DATE", pack="BLANK", sec="Identity (POI)"),
    "POA_FILE_NAME": F("Entity POA image", "M", 50, "IMAGE_FILE", sec="Address proof (POA)", rec="70"),
    "POA_TYPE": F("Entity POA type", "M", 2, None, ["06", "07", "99"], sec="Address proof (POA)"),
    "POA_NUMBER": F("Entity POA number", "M", 60, "BY_LE_POA_TYPE", sec="Address proof (POA)", notes="CERSAI size 60; iSolve DB VARCHAR2(20) (see max_len_db)."),
    "POA_EXPIRY_DATE": F("Entity POA expiry", "O", 10, "DATE", pack="BLANK", sec="Address proof (POA)"),
    "COMM_ADDRESS_CATEGORY": F("Local address proof category", "CM", 50, None, when=FLAG_N, sec="Local address", notes="Pack = POA master name of POA_TYPE."),
    "MOBILE_NO2_ISD_CODE": F("Mobile 2 ISD (LE block)", "O", 3, "ISD", sec="Contact"),
}
for k in ["CUST_NAME_UPDT_FLG", "PER_DET_UPDT_FLG", "ADD_DET_UPDT_FLG", "CONT_DET_UPDT_FLG", "REM_UPDT_FLG", "KYC_VERIFY_UPDT_FLG",
          "ID_DET_UPDT_FLG", "REL_PER_UPDT_FLG", "CONT_PER_UPDT_FLG", "IMG_DET_UPDT_FLG",
          "RESIDENCE_TELEPHONE_NO_STD_CODE", "RESIDENCE_TELEPHONE_NO", "OFFICE_TELEPHONE_NO_STD_CODE", "OFFICE_TELEPHONE_NO",
          "MOBILE_NO_ISD_CODE", "MOBILE_NO", "FAX_NO_STD_CODE", "FAX_NO", "EMAIL_ID", "MOBILE_NO2_ISD_CODE2", "MOBILE_NO2", "EMAIL_ID2"]:
    LE[k] = dict(APPLICANT[k])

DB = {"IND": ("CKYC_FIN_DATA", None), "MIN": ("CKYC_FIN_DATA", "CKYC_FIN_DATA_RP"), "LE": ("CKYC_FIN_DATA_LE", "CKYC_FIN_DATA_LE_RP")}
REC = {"20": "CERSAI Detail(20) KYC record", "30": "CERSAI Detail(30) Identity record", "40": "CERSAI Detail(40) Related person record",
       "70": "CERSAI Detail(70) Image record", "iFlow only": "iFlow/iSolve CSV only (not in CERSAI V1.3 upload)", "system": "iSolve system/link column"}


def where(name, spec, persona, is_rp):
    tbl = DB[persona][1 if is_rp else 0]
    w = [f"CSV column {name}", f"DB {tbl}.{name}", REC.get(spec["cersai_record"], spec["cersai_record"])]
    if spec["format"] == "IMAGE_FILE":
        w.append("File Common_Images/<CUSTOMER_REFERENCE_NUMBER>/<value> + IMAGE_STATUS_SUMMARY.csv (IP folder)")
    if name in ("POI_NUMBER", "POA_NUMBER", "POI_TYPE", "POA_TYPE", "PAN_OR_FORM60") or name.startswith("REL_PER_UID") or name in [d["rp_id_column"] for d in DOC_MAP]:
        w.append("Number printed on the matching document image; SUMMARY.csv (POI/POA columns)")
    if name.endswith("_PIN") or name.endswith("_DIST") or name.endswith("_DISTRICT") or name.endswith("_STATE"):
        w.append("masters/pincode_master.csv")
    w.append("DVS Maker screen (field-level error) / CERSAI response file error code")
    return w


def build():
    personas = {}
    for p in ("IND", "MIN", "LE"):
        hdr = HDR[p].read_text().strip().split("|")
        cols = []
        rp_start = hdr.index("REL_TYPE") if "REL_TYPE" in hdr else len(hdr)
        for i, name in enumerate(hdr):
            is_rp = i >= rp_start or (p == "MIN" and name == "DECEASED_DATE")
            src = (RP if is_rp else (LE if p == "LE" else APPLICANT))
            if name not in src:
                raise SystemExit(f"{p}: no definition for {name}")
            s = json.loads(json.dumps(src[name]))
            if p == "MIN" and not is_rp:
                if name == "ACC_TYPE":
                    s["enum"] = ["05"]; s["notes"] = "Minor = 05."
                if name == "MINOR":
                    s["enum"] = ["Y"]; s["notes"] = "Minor = Y (age < 18)."
                if name == "POI_TYPE":
                    s["enum"] = ["A", "E", "F", "G", "H", "I"]; s["notes"] = "Minor: never Voter ID (B) or Driving Licence (D). Pack uses A/E/F."
                if name in ("POA_TYPE", "PERM_ADD_PROOF"):
                    s["enum"] = ["01", "02", "05", "08", "09", "10"]; s["notes"] = (s["notes"] + " Minor: never 03 (DL) or 04 (Voter).").strip()
                if name == "COMM_ADDRESS_IDTYPE":
                    s["enum"] = ["01", "02", "05", "08", "09", "10", "11", "12"]
                if name == "MARITAL_STATUS":
                    s["enum"] = ["S"]
            if p == "MIN" and is_rp and name != "DECEASED_DATE":
                s["minor_without_rp"] = "BLANK" if name != "CUST_ID" else "ALWAYS"
                if name == "REL_TYPE":
                    s["enum"] = ["1"]; s["notes"] = "Minor with RP: 1 (Guardian). Minor without RP: blank."
            if p == "LE" and not is_rp:
                s["le_continuation_row"] = "BLANK"
            if p == "LE" and is_rp and name == "REL_TYPE":
                s["enum"] = [str(x) for x in range(4, 16)]
            if p == "LE" and not is_rp and name in ("POI_NUMBER", "POA_NUMBER", "COMM_ADDRESS_NUMBER"):
                s["max_len_db"] = 20
            s.update({"col": i + 1, "name": name, "block": "RP" if is_rp else ("ENTITY" if p == "LE" else "APPLICANT"),
                      "db_table": DB[p][1 if is_rp else 0], "where_to_verify": where(name, s, p, is_rp)})
            cols.append(s)
        personas[p] = {"file_prefix": "CKYC_DATA_LE_" if p == "LE" else "CKYC_DATA_IN_", "column_count": len(hdr),
                       "header": "|".join(hdr), "columns": cols}
    spec = {
        "spec_name": "CKYC / CERSAI V1.3 - Cosmos iFlow bulk CSV validation spec",
        "version": "1.0 (30-09-2026)",
        "status_legend": {"M": "Mandatory - must be filled", "CM": "Conditional mandatory - mandatory when `when` is true",
                          "O": "Optional - may be blank, format still checked when filled",
                          "BLANK_FOR_NEW": "Must be blank for NEW (APPL_TYPE 01) upload", "SYSTEM_BLANK": "System field - keep blank"},
        "pack_fill_legend": {"ALWAYS": "Cosmos positive pack fills it on every row (user rule: fill every field a NEW record can carry)",
                             "BLANK": "Cosmos pack keeps it blank", "<expression>": "Filled when the expression is true, blank otherwise"},
        "condition_syntax": "Python-style expression over column names (string values). Helpers: no_other_rp_id() - true when none of the other 7 RP ID columns is filled; rp_doc_used(code) - true when REL_PER_ADD_PROF / REL_PER_CURR_ADD_PROOF / RP POI image uses that POA code.",
        "config": CONFIG, "formats": FORMATS, "masters": MASTERS, "doc_map": DOC_MAP, "le_doc_map": LE_DOC_MAP,
        "deemed_map": DEEMED_MAP, "le_constitution_rules": LE_CONST_RULES, "gst_state_code": GST_STATE,
        "number_format_by_code": {
            "POI_TYPE": {d["poi_code"]: d["number_format"] for d in DOC_MAP},
            "POA_TYPE": {d["poa_code"]: d["number_format"] for d in DOC_MAP},
            "COMM_ADDRESS_IDTYPE": {**{d["poa_code"]: d["number_format"] for d in DOC_MAP}, **{d["code"]: d["number_format"] for d in DEEMED_MAP},
                                    "06": "CIN_OR_LLPIN", "07": "REG_CERT"},
            "LE_POI_TYPE": {"02": "CIN_OR_LLPIN", "03": "REG_CERT"}, "LE_POA_TYPE": {"06": "CIN_OR_LLPIN", "07": "REG_CERT", "99": "ANY"},
        },
        "personas": personas,
    }
    spec["cross_field_rules"] = json.loads((HERE / "rules.json").read_text())
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "ckyc_cosmos_field_spec.json").write_text(json.dumps(spec, indent=1, ensure_ascii=False))
    print({p: len(v["columns"]) for p, v in personas.items()}, "rules", len(spec["cross_field_rules"]))


if __name__ == "__main__":
    build()
