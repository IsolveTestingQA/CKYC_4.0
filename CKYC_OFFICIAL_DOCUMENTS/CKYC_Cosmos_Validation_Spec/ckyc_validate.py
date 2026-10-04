#!/usr/bin/env python3
"""
CKYC / CERSAI V1.3 - Cosmos iFlow bulk-CSV reference validator
==============================================================
Driven entirely by ckyc_cosmos_field_spec.json + masters/*.csv (same folder).

Usage
  python ckyc_validate.py <pack_folder> [<pack_folder> ...] [--out report_dir] [--upload-date DD-MM-YYYY]

  <pack_folder> = folder that contains 01_POSITIVE / 02_DVS / 03_IMAGE_PROCESSING / Common_Images
                  (e.g. the extracted COSMOS_Pack1_30Sep2026). Several packs -> uniqueness is checked across all.

Output
  <out>/issues.csv          one line per finding (severity, rule, file, line, customer, column, value, message)
  <out>/record_results.csv  one line per record: PASS / FAIL + DVS expected-defect match
  <out>/summary.json        counts per pack / purpose / category
  console                   short summary

Expected result for a correct Cosmos pack
  01_POSITIVE, 03_IMAGE_PROCESSING : 0 ERROR (images in IP judged against IMAGE_STATUS_SUMMARY.csv)
  02_DVS                           : every record FAIL, and the SUMMARY.csv DVS_DEFECT_FIELD is among its error columns
Severity: ERROR = CERSAI / iFlow / user hard rule, WARN = Cosmos pack convention, PACK = positive-pack completeness
Python 3.8+, standard library only.
"""
import argparse, csv, json, re, sys
from collections import defaultdict, Counter
from datetime import date, datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = json.loads((HERE / "ckyc_cosmos_field_spec.json").read_text(encoding="utf-8"))
CFG = SPEC["config"]
FMT = SPEC["formats"]
DOC = SPEC["doc_map"]
POI2DOC = {d["poi_code"]: d for d in DOC}
POA2DOC = {d["poa_code"]: d for d in DOC}
IMG2POA = {d["image_file"]: d["poa_code"] for d in DOC}
RPCOL = {d["poa_code"]: d["rp_id_column"] for d in DOC}
LEDOC = {"02": SPEC["le_doc_map"][0], "06": SPEC["le_doc_map"][0], "03": SPEC["le_doc_map"][1], "07": SPEC["le_doc_map"][1]}
DEEMED = {d["code"]: d for d in SPEC["deemed_map"]}
POA_NAME = {**SPEC["masters"]["POA_IND"], **SPEC["masters"]["POA_LE"]}
GST = SPEC["gst_state_code"]
RP_ID_COLS = [d["rp_id_column"] for d in DOC]


def load_master(name, key, cols=None):
    with open(HERE / "masters" / name, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    return {r[key]: r for r in rows} if cols is None else rows


PIN = load_master("pincode_master.csv", "PIN")
STATES = set(load_master("state_master.csv", "CODE"))
COUNTRIES = set(load_master("country_code_iso3166.csv", "CODE"))
IMPAIR = set(load_master("type_of_impairment.csv", "CODE"))

# ---------------------------------------------------------------- checksums
_D = [[0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [1, 2, 3, 4, 0, 6, 7, 8, 9, 5], [2, 3, 4, 0, 1, 7, 8, 9, 5, 6], [3, 4, 0, 1, 2, 8, 9, 5, 6, 7],
      [4, 0, 1, 2, 3, 9, 5, 6, 7, 8], [5, 9, 8, 7, 6, 0, 4, 3, 2, 1], [6, 5, 9, 8, 7, 1, 0, 4, 3, 2], [7, 6, 5, 9, 8, 2, 1, 0, 4, 3],
      [8, 7, 6, 5, 9, 3, 2, 1, 0, 4], [9, 8, 7, 6, 5, 4, 3, 2, 1, 0]]
_P = [[0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [1, 5, 7, 6, 2, 8, 3, 0, 9, 4], [5, 8, 0, 3, 7, 9, 6, 1, 4, 2], [8, 9, 1, 6, 0, 4, 3, 5, 2, 7],
      [9, 4, 5, 3, 1, 2, 6, 8, 7, 0], [4, 2, 8, 6, 5, 7, 3, 9, 0, 1], [2, 7, 9, 3, 8, 0, 6, 4, 1, 5], [7, 0, 4, 6, 9, 1, 3, 2, 5, 8]]


def verhoeff(n):
    c = 0
    for i, ch in enumerate(reversed(n)):
        c = _D[c][_P[i % 8][int(ch)]]
    return c == 0


_G = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"


def gstin_ok(g):
    if len(g) != 15 or any(ch not in _G for ch in g):
        return False
    s = 0
    for i, ch in enumerate(g[:14]):
        v = _G.index(ch) * (1 if i % 2 == 0 else 2)
        s += v // 36 + v % 36
    return g[14] == _G[(36 - s % 36) % 36]


def pdate(v):
    try:
        return datetime.strptime(v, "%d-%m-%Y").date() if re.fullmatch(FMT["DATE"]["regex"], v or "") else None
    except ValueError:
        return None


def age(dob, ref):
    return ref.year - dob.year - ((ref.month, ref.day) < (dob.month, dob.day))


def fmt_ok(fname, v):
    """True / False for a named format (or composite CIN_OR_LLPIN / ANY)."""
    if fname in (None, "", "ANY"):
        return True
    if fname == "CIN_OR_LLPIN":
        return fmt_ok("CIN", v) or fmt_ok("LLPIN", v)
    f = FMT[fname]
    if "master" in f:
        return v in (STATES if f["master"].startswith("state") else COUNTRIES)
    if not re.fullmatch(f["regex"], v):
        return False
    if f.get("checksum") == "verhoeff":
        return verhoeff(v)
    if f.get("checksum") == "gstin":
        return gstin_ok(v)
    if fname == "REG_CERT" and v[:2].isdigit():
        return gstin_ok(v)
    return True


# ---------------------------------------------------------------- report
ISSUES = []


def issue(ctx, sev, rule, col, value, msg):
    ISSUES.append({"severity": sev, "rule": rule, "pack": ctx["pack"], "purpose": ctx["purpose"], "category": ctx["category"],
                   "file": ctx["file"], "line": ctx["line"], "customer": ctx["cust"], "rp_cust_id": ctx.get("rp", ""),
                   "column": col, "value": value, "message": msg})


# ---------------------------------------------------------------- field-level checks
def rp_doc_codes(r):
    codes = {r.get("REL_PER_ADD_PROF", ""), r.get("REL_PER_CURR_ADD_PROOF", "")}
    img = re.sub(r"^RP\d+_", "", r.get("REL_PER_POI_NAME", ""))
    codes.add(IMG2POA.get(img, ""))
    return codes - {""}


def ns(r):
    n = dict(r)
    n["no_other_rp_id"] = lambda: not any(r.get(c) for c in RP_ID_COLS if c != "REL_PER_UID")
    n["rp_doc_used"] = lambda code: code in rp_doc_codes(r)
    n["True"], n["False"] = True, False
    return n


def cond(expr, r):
    if not expr:
        return False
    try:
        return bool(eval(expr, {"__builtins__": {}}, ns(r)))
    except Exception:
        return False


def resolve_format(c, r, persona):
    f = c["format"]
    by = SPEC["number_format_by_code"]
    if f == "BY_POI_TYPE":
        return by["POI_TYPE"].get(r.get("POI_TYPE"), "ANY")
    if f == "BY_POA_TYPE":
        return by["POA_TYPE"].get(r.get("POA_TYPE"), "ANY")
    if f == "BY_COMM_IDTYPE":
        return by["COMM_ADDRESS_IDTYPE"].get(r.get("COMM_ADDRESS_IDTYPE"), "ANY")
    if f == "BY_LE_POI_TYPE":
        return by["LE_POI_TYPE"].get(r.get("POI_TYPE"), "ANY")
    if f == "BY_LE_POA_TYPE":
        return by["LE_POA_TYPE"].get(r.get("POA_TYPE"), "ANY")
    return f


def enum_ok(c, v):
    en = c.get("enum")
    if not en:
        return True
    for e in en:
        if e.startswith("CONFIG:"):
            if v == CFG[e[7:]]:
                return True
        elif e.startswith("MASTER:"):
            if "type_of_impairment" in e:
                return v in IMPAIR
            return True  # district masters are checked by ADDR-01
        elif v == e:
            return True
    return False


def check_fields(ctx, r, cols, skip_block, positive):
    for c in cols:
        name, v = c["name"], r.get(c["name"], "")
        if c["block"] in skip_block:
            if v and not (name == "CUST_ID"):
                issue(ctx, "ERROR", "FIELD-BLANK", name, v, "must be blank on this row type (" + skip_block[c["block"]] + ")")
            continue
        st = c["status"]
        if st in ("BLANK_FOR_NEW", "SYSTEM_BLANK"):
            if v:
                issue(ctx, "ERROR", "FIELD-BLANK", name, v, f"{st}: must be blank for NEW upload")
            continue
        if not v:
            if st == "M":
                issue(ctx, "ERROR", "FIELD-M", name, "", "mandatory field is blank")
            elif st == "CM" and cond(c["when"], r):
                issue(ctx, "ERROR", "FIELD-CM", name, "", f"conditional mandatory ({c['when']}) is blank")
            elif positive:
                pf = c["pack_fill"]
                if pf == "ALWAYS" or (pf not in ("BLANK",) and cond(pf, r)):
                    issue(ctx, "PACK", "FIELD-PACK", name, "", "positive pack fills this field on every applicable row")
            continue
        if positive and c["pack_fill"] == "BLANK":
            issue(ctx, "WARN", "FIELD-PACK", name, v, "Cosmos pack keeps this field blank")
        if c["max_len"] and len(v) > int(c["max_len"]):
            issue(ctx, "ERROR", "FIELD-LEN", name, v, f"length {len(v)} > max {c['max_len']}")
        if c.get("max_len_db") and len(v) > int(c["max_len_db"]):
            issue(ctx, "WARN", "DB-LEN", name, v, f"length {len(v)} > iSolve DB column length {c['max_len_db']} (CERSAI allows {c['max_len']}) - confirm with Cosmos dev")
        f = resolve_format(c, r, ctx["persona"])
        if f and not fmt_ok(f, v):
            issue(ctx, "ERROR", "FIELD-FMT", name, v, f"not a valid {f}: {FMT.get(f, {}).get('desc', f)}")
        if not enum_ok(c, v):
            issue(ctx, "ERROR", "FIELD-ENUM", name, v, f"value not in allowed list {c['enum']}")


# ---------------------------------------------------------------- cross-field rules
def addr_block(r, keys):
    return [r.get(k, "") for k in keys]


PERM = ["PERM_ADD_LINE1", "PERM_ADD_LINE2", "PERM_ADD_LINE3", "PERM_ADD_CITY", "PERM_ADD_DIST", "PERM_ADD_STATE", "PERM_ADD_COUNTRY", "PERM_ADD_PIN"]
COMM = {"IND": ["COMM_Add_Line1", "COMM_Add_Line2", "COMM_Add_Line3", "COMM_Add_City", "COMM_Add_Dist", "COMM_Add_State", "COMM_Add_Country", "COMM_Add_Pin"],
        "LE": ["COMM_ADD_LINE1", "COMM_ADD_LINE2", "COMM_ADD_LINE3", "COMM_ADD_CITY", "COMM_ADD_DIST", "COMM_ADD_STATE", "COMM_ADD_COUNTRY", "COMM_ADD_PIN"]}
COMM["MIN"] = COMM["IND"]
RPP = ["REL_PER_ADD_LINE1", "REL_PER_ADD_LINE2", "REL_PER_ADD_LINE3", "REL_PER_ADD_CITY", "REL_PER_ADD_DISTRICT", "REL_PER_ADD_STATE", "REL_PER_ADD_COUNTRY", "REL_PER_ADD_PIN"]
RPC = ["REL_PER_CURR_ADD_LINE1", "REL_PER_CURR_ADD_LINE2", "REL_PER_CURR_ADD_LINE3", "REL_PER_CURR_ADD_CITY", "REL_PER_CURR_ADD_DISTRICT", "REL_PER_CURR_ADD_STATE", "REL_PER_CURR_ADD_COUNTRY", "REL_PER_CURR_ADD_PIN"]


def pin_check(ctx, r, keys):
    dist, st, pin = r.get(keys[4], ""), r.get(keys[5], ""), r.get(keys[7], "")
    if not pin:
        return
    m = PIN.get(pin)
    if not m:
        issue(ctx, "ERROR", "ADDR-01", keys[7], pin, "PIN not in CERSAI pincode master")
        return
    if dist and dist.upper() != m["DISTRICT"].upper():
        issue(ctx, "ERROR", "ADDR-01", keys[4], dist, f"district of PIN {pin} is {m['DISTRICT']}")
    if st and st != m["STATE_CODE"]:
        issue(ctx, "ERROR", "ADDR-01", keys[5], st, f"state of PIN {pin} is {m['STATE_CODE']}")


def same_or_diff(ctx, r, flag, a, b, rule, label):
    A, B = addr_block(r, a), addr_block(r, b)
    if flag == "Y" and A != B:
        bad = [b[i] for i in range(len(a)) if A[i] != B[i]]
        issue(ctx, "ERROR", rule, ",".join(bad), "", f"{label} flag Y but address is not an exact copy")
    if flag == "N" and (A[0] == B[0] and A[7] == B[7]):
        issue(ctx, "ERROR", rule, b[0], B[0], f"{label} flag N but address equals the permanent address")


def dates_check(ctx, r, pairs, upload):
    for col in pairs:
        v = r.get(col, "")
        d = pdate(v)
        if v and d:
            if d > upload:
                issue(ctx, "ERROR", "DATE-01", col, v, "future date")
            elif d == upload:
                issue(ctx, "ERROR", "DATE-01", col, v, "must not equal the upload date")


def applicant_rules(ctx, r, persona, folder, upload):
    fl = r["PERM_TO_COMM_FLG"]
    # names
    if not (r["FATHERORSPOUSE_FIRST_NAME"] or r["MOTHER_FIRST_NAME"]):
        issue(ctx, "ERROR", "NAME-01", "MOTHER_FIRST_NAME", "", "none of father/spouse/mother name given")
    g, p = r["GENDER"], r["APPL_NAME_PREFIX"]
    if (p == "MR" and g == "F") or (p in ("MRS", "MS", "MISS") and g == "M"):
        issue(ctx, "WARN", "NAME-02", "APPL_NAME_PREFIX", p, f"prefix does not match gender {g}")
    if r["FORS_FLG"] == "02" and p != "MRS":
        issue(ctx, "WARN", "NAME-02", "FORS_FLG", "02", "spouse name used for a non-married prefix")
    # dates / age
    dates_check(ctx, r, ["DOB", "DECL_DATETIME", "KYC_VERIFY_DATETIME"], upload)
    dob, decl, kyc = pdate(r["DOB"]), pdate(r["DECL_DATETIME"]), pdate(r["KYC_VERIFY_DATETIME"])
    if dob and decl:
        a = age(dob, decl)
        if dob >= decl:
            issue(ctx, "ERROR", "DATE-01", "DOB", r["DOB"], "DOB not before declaration date")
        if persona == "IND" and a < 18:
            issue(ctx, "ERROR", "AGE-01", "DOB", r["DOB"], f"Individual aged {a} (<18)")
        if persona == "MIN":
            if a >= 18:
                issue(ctx, "ERROR", "AGE-02", "DOB", r["DOB"], f"Minor aged {a} (>=18)")
            if folder == "03_Minor_Without_RP" and not (11 <= a <= 17):
                issue(ctx, "WARN", "AGE-02", "DOB", r["DOB"], f"Minor without RP aged {a} (pack rule 11-17)")
    for c, code, codes in (("POI_EXPIRY_DATE", r["POI_TYPE"], ("A", "D")), ("POA_EXPIRY_DATE", r["POA_TYPE"], ("02", "03"))):
        v = r.get(c, "")
        if code in codes and v and kyc and pdate(v) and pdate(v) <= kyc:
            issue(ctx, "WARN", "DATE-02", c, v, "expiry not after KYC date")
        if code not in codes and v:
            issue(ctx, "WARN", "DATE-02", c, v, "expiry only for Passport / Driving Licence")
    # addresses
    pin_check(ctx, r, PERM)
    pin_check(ctx, r, COMM[persona])
    same_or_diff(ctx, r, fl, PERM, COMM[persona], "ADDR-02", "PERM_TO_COMM_FLG")
    if fl == "Y" and r["COMM_ADDRESS_TYPE"]:
        issue(ctx, "WARN", "ADDR-02", "COMM_ADDRESS_TYPE", r["COMM_ADDRESS_TYPE"], "pack keeps COMM_ADDRESS_TYPE blank when flag Y")
    # POI / POA
    pd, ad = POI2DOC.get(r["POI_TYPE"]), POA2DOC.get(r["POA_TYPE"])
    if pd:
        if r["POI_CATEGORY"] and r["POI_CATEGORY"] != pd["poi_category"]:
            issue(ctx, "ERROR", "POI-01", "POI_CATEGORY", r["POI_CATEGORY"], f"POI_TYPE {r['POI_TYPE']} category must be '{pd['poi_category']}'")
        if r["POI_FILE_NAME"] and r["POI_FILE_NAME"] != pd["image_file"]:
            issue(ctx, "ERROR", "IMG-03", "POI_FILE_NAME", r["POI_FILE_NAME"], f"POI_TYPE {r['POI_TYPE']} image must be {pd['image_file']}")
        if persona == "MIN" and not pd["minor_allowed"]:
            issue(ctx, "ERROR", "MINDOC-01", "POI_TYPE", r["POI_TYPE"], "Voter ID / Driving Licence not allowed for a minor")
    if ad:
        if r["POA_CATEGORY"] and r["POA_CATEGORY"] != ad["poa_category"]:
            issue(ctx, "ERROR", "POA-01", "POA_CATEGORY", r["POA_CATEGORY"], f"POA_TYPE {r['POA_TYPE']} category must be '{ad['poa_category']}'")
        if r["POA_FILE_NAME"] and r["POA_FILE_NAME"] != ad["image_file"]:
            issue(ctx, "ERROR", "IMG-03", "POA_FILE_NAME", r["POA_FILE_NAME"], f"POA_TYPE {r['POA_TYPE']} image must be {ad['image_file']}")
        if persona == "MIN" and not ad["minor_allowed"]:
            issue(ctx, "ERROR", "MINDOC-01", "POA_TYPE", r["POA_TYPE"], "Voter ID / Driving Licence not allowed for a minor")
    if r["PERM_ADD_PROOF"] != r["POA_TYPE"]:
        issue(ctx, "ERROR", "POA-01", "PERM_ADD_PROOF", r["PERM_ADD_PROOF"], f"must equal POA_TYPE {r['POA_TYPE']}")
    if r["PERM_ADD_PROOF_DESC"] and ad and r["PERM_ADD_PROOF_DESC"] != ad["poa_category"]:
        issue(ctx, "WARN", "POA-01", "PERM_ADD_PROOF_DESC", r["PERM_ADD_PROOF_DESC"], f"should be '{ad['poa_category']}'")
    if pd and ad:
        same_doc = pd["doc"] == ad["doc"]
        if same_doc and (r["POI_NUMBER"] != r["POA_NUMBER"] or r["POI_FILE_NAME"] != r["POA_FILE_NAME"]):
            issue(ctx, "ERROR", "POIA-01", "POA_NUMBER", r["POA_NUMBER"], "same document for POI and POA -> same number and same image")
        if not same_doc and (r["POI_FILE_NAME"] == r["POA_FILE_NAME"] or r["POI_NUMBER"] == r["POA_NUMBER"]):
            issue(ctx, "ERROR", "POIA-01", "POA_FILE_NAME", r["POA_FILE_NAME"], "different documents must have different number and image")
    # correspondence proof
    it, num, catg = r["COMM_ADDRESS_IDTYPE"], r["COMM_ADDRESS_NUMBER"], r["COMM_ADDRESS_CATEGORY"]
    if fl == "Y" and (it, num, catg) != (r["POA_TYPE"], r["POA_NUMBER"], r["POA_CATEGORY"]):
        issue(ctx, "ERROR", "COMM-01", "COMM_ADDRESS_IDTYPE", it, "flag Y: correspondence proof must be the POA document (type, number, category)")
    if fl == "N":
        if it not in DEEMED:
            issue(ctx, "ERROR", "COMM-01", "COMM_ADDRESS_IDTYPE", it, "flag N: must be a deemed proof 11/12/13/14, never an OVD")
        else:
            if catg != DEEMED[it]["category"]:
                issue(ctx, "ERROR", "COMM-01", "COMM_ADDRESS_CATEGORY", catg, f"must be '{DEEMED[it]['category']}'")
            if persona == "MIN" and it not in ("11", "12"):
                issue(ctx, "ERROR", "COMM-01", "COMM_ADDRESS_IDTYPE", it, "minor: only 11 / 12")
            if it == "13" and dob and decl and age(dob, decl) < 58:
                issue(ctx, "WARN", "COMM-01", "COMM_ADDRESS_IDTYPE", it, "Pension Payment Order only for age >= 58")
    # PAN
    pan = r["PAN_OR_FORM60"]
    if re.fullmatch(FMT["PAN"]["regex"], pan):
        if pan[3] != "P":
            issue(ctx, "ERROR", "PAN-01", "PAN_OR_FORM60", pan, "individual PAN 4th letter must be P")
        if r["APPL_LAST_NAME"] and pan[4] != r["APPL_LAST_NAME"][0]:
            issue(ctx, "WARN", "PAN-02", "PAN_OR_FORM60", pan, "5th letter should be the last-name initial")
    if persona == "IND" and pan == "FORM60":
        issue(ctx, "WARN", "PAN-01", "PAN_OR_FORM60", pan, "Individual positive pack uses PAN")
    contact_rules(ctx, r, [("RESIDENCE_TELEPHONE_NO_STD_CODE", "RESIDENCE_TELEPHONE_NO"), ("OFFICE_TELEPHONE_NO_STD_CODE", "OFFICE_TELEPHONE_NO"),
                           ("FAX_NO_STD_CODE", "FAX_NO"), ("MOBILE_NO_ISD_CODE", "MOBILE_NO"), ("MOBILE_NO2_ISD_CODE2", "MOBILE_NO2")])
    hier(ctx, r, "ORG_CODE", "ORG_NAME")


def contact_rules(ctx, r, pairs):
    for a, b in pairs:
        if bool(r.get(a)) != bool(r.get(b)):
            issue(ctx, "ERROR", "CONTACT-01", b if r.get(a) else a, r.get(a) or r.get(b), f"{a} and {b} must be filled together")


def hier(ctx, r, code_col, name_col):
    if r.get(code_col) and r[code_col] != CFG["ORG_CODE"]:
        issue(ctx, "ERROR", "HIER-01", code_col, r[code_col], f"must be {CFG['ORG_CODE']}")
    if r.get(name_col) and r[name_col] != CFG["ORG_NAME"]:
        issue(ctx, "ERROR", "HIER-01", name_col, r[name_col], f"must be {CFG['ORG_NAME']}")


def rp_rules(ctx, r, n, persona, parent, upload):
    flag = r["REL_PER_SAMEASPERM_ADD_FLAG"]
    dates_check(ctx, r, ["REL_PER_DOB", "REL_PER_DECL_DATETIME", "REL_PER_KYC_VERIFY_DATETIME"], upload)
    dob, decl = pdate(r["REL_PER_DOB"]), pdate(r["REL_PER_DECL_DATETIME"])
    if dob and decl and age(dob, decl) < 18:
        issue(ctx, "ERROR", "AGE-03", "REL_PER_DOB", r["REL_PER_DOB"], f"related person aged {age(dob, decl)} (<18)")
    if not r["REL_PER_MOTHER_FIRST_NAME"]:
        issue(ctx, "WARN", "NAME-03", "REL_PER_MOTHER_FIRST_NAME", "", "RP mother name always filled (user rule)")
    g, p = r["REL_PER_GENDER"], r["REL_PER_NAME_PREFIX"]
    if (p == "MR" and g == "F") or (p in ("MRS", "MS", "MISS") and g == "M"):
        issue(ctx, "WARN", "NAME-02", "REL_PER_NAME_PREFIX", p, f"prefix does not match gender {g}")
    pin_check(ctx, r, RPP)
    pin_check(ctx, r, RPC)
    same_or_diff(ctx, r, flag, RPP, RPC, "ADDR-03", "REL_PER_SAMEASPERM_ADD_FLAG")
    perm, curr = r["REL_PER_ADD_PROF"], r["REL_PER_CURR_ADD_PROOF"]
    if flag == "Y" and curr and curr != perm:
        issue(ctx, "ERROR", "ADDR-03", "REL_PER_CURR_ADD_PROOF", curr, "flag Y: must equal REL_PER_ADD_PROF")
    # RP ID columns
    if not any(r.get(c) for c in RP_ID_COLS):
        issue(ctx, "ERROR", "RPID-01", "REL_PER_UID", "", "no RP identity number in any of the 8 RP ID columns")
    poi_img = re.sub(r"^RP\d+_", "", r["REL_PER_POI_NAME"])
    poi_code = IMG2POA.get(poi_img, "")
    for label, code in (("REL_PER_ADD_PROF", perm), ("RP POI image", poi_code), ("REL_PER_CURR_ADD_PROOF", curr if flag == "N" else "")):
        if code and code in RPCOL and not r.get(RPCOL[code]):
            issue(ctx, "ERROR", "RPID-02", RPCOL[code], "", f"{label} = {code} but {RPCOL[code]} is blank")
    if flag == "N" and curr and (curr == perm or curr == poi_code):
        issue(ctx, "WARN", "RPID-03", "REL_PER_CURR_ADD_PROOF", curr, "different current address should use a different proof document")
    for c in ("REL_PER_EKYC", "REL_PER_OFFLINE_UID"):
        if r.get(c) and r.get("REL_PER_UID") and re.fullmatch(r"\d{4}", r[c]) and r[c] != r["REL_PER_UID"][-4:]:
            issue(ctx, "ERROR", "RPID-04", c, r[c], "must be the last 4 digits of REL_PER_UID")
    # images
    pre = f"RP{n}_"
    if r["REL_PER_PHOTO_NAME"] and r["REL_PER_PHOTO_NAME"] != pre + "Photo.jpg":
        issue(ctx, "ERROR", "RPIMG-01", "REL_PER_PHOTO_NAME", r["REL_PER_PHOTO_NAME"], f"expected {pre}Photo.jpg")
    if r["REL_PER_POI_NAME"] and not r["REL_PER_POI_NAME"].startswith(pre):
        issue(ctx, "ERROR", "RPIMG-01", "REL_PER_POI_NAME", r["REL_PER_POI_NAME"], f"expected prefix {pre}")
    if perm in POA2DOC and r["REL_PER_PER_ADDRESS_NAME"] and r["REL_PER_PER_ADDRESS_NAME"] != pre + POA2DOC[perm]["image_file"]:
        issue(ctx, "ERROR", "RPIMG-01", "REL_PER_PER_ADDRESS_NAME", r["REL_PER_PER_ADDRESS_NAME"], f"REL_PER_ADD_PROF {perm} -> {pre}{POA2DOC[perm]['image_file']}")
    pan = r["REL_PER_PANORFORM60"]
    if re.fullmatch(FMT["PAN"]["regex"], pan):
        if pan[3] != "P":
            issue(ctx, "ERROR", "PAN-01", "REL_PER_PANORFORM60", pan, "RP PAN 4th letter must be P")
        if r["REL_PER_LAST_NAME"] and pan[4] != r["REL_PER_LAST_NAME"][0]:
            issue(ctx, "WARN", "PAN-02", "REL_PER_PANORFORM60", pan, "5th letter should be the last-name initial")
    if r.get("CUST_ID") != parent:
        issue(ctx, "ERROR", "CUST-01", "CUST_ID", r.get("CUST_ID"), f"must be the parent CUSTOMER_REFERENCE_NUMBER {parent}")
    if r["REL_TYPE"] == "4" and not r["REL_PER_DIN_NUMBER"]:
        issue(ctx, "ERROR", "LE-02", "REL_PER_DIN_NUMBER", "", "Director needs DIN")
    if r["REL_TYPE"] != "4" and r["REL_PER_DIN_NUMBER"]:
        issue(ctx, "WARN", "LE-02", "REL_PER_DIN_NUMBER", r["REL_PER_DIN_NUMBER"], "DIN only for Director")
    contact_rules(ctx, r, [("REL_PER_TEL_STDCODE", "REL_PER_TEL_NO"), ("REL_PER_OFFTEL_STDCODE", "REL_PER_OFFTEL_NO"), ("REL_PER_MOBILE_CODE", "REL_PER_MOBILE_NO")])
    hier(ctx, r, "REL_PER_ORG_CODE", "REL_PER_ORG_NAME")


def entity_rules(ctx, r, rows, upload):
    fl = r["PERM_TO_COMM_FLG"]
    ct = r["CONST_TYPE"]
    rule = SPEC["le_constitution_rules"].get(ct)
    dates_check(ctx, r, ["DATE_OF_INC", "DATE_OF_COMMENCEMENT", "DECL_DATETIME", "KYC_VERIFY_DATETIME"], upload)
    di, dc = pdate(r["DATE_OF_INC"]), pdate(r["DATE_OF_COMMENCEMENT"])
    if di and dc and dc < di:
        issue(ctx, "ERROR", "DATE-03", "DATE_OF_COMMENCEMENT", r["DATE_OF_COMMENCEMENT"], "before DATE_OF_INC")
    pin_check(ctx, r, PERM)
    pin_check(ctx, r, COMM["LE"])
    same_or_diff(ctx, r, fl, PERM, COMM["LE"], "ADDR-02", "PERM_TO_COMM_FLG")
    pan = r["PAN_OR_FORM60"]
    if rule and re.fullmatch(FMT["PAN"]["regex"], pan):
        if pan[3] != rule["pan_4th"]:
            issue(ctx, "ERROR", "PAN-01", "PAN_OR_FORM60", pan, f"constitution {ct} PAN 4th letter must be {rule['pan_4th']}")
        if pan[4] != r["APPL_FULL_NAME"][:1]:
            issue(ctx, "WARN", "PAN-02", "PAN_OR_FORM60", pan, "5th letter should be the entity-name initial")
    if rule and not r["APPL_FULL_NAME"].endswith(rule["name_suffix"]):
        issue(ctx, "WARN", "LE-05", "APPL_FULL_NAME", r["APPL_FULL_NAME"], f"constitution {ct} name ends with '{rule['name_suffix']}'")
    tin = r["TIN"]
    if tin and re.fullmatch(FMT["GSTIN"]["regex"], tin):
        if tin[2:12] != pan:
            issue(ctx, "ERROR", "LE-04", "TIN", tin, "GSTIN chars 3-12 must equal PAN_OR_FORM60")
        if GST.get(r["PERM_ADD_STATE"]) and tin[:2] != GST[r["PERM_ADD_STATE"]]:
            issue(ctx, "ERROR", "LE-04", "TIN", tin, f"GSTIN state code must be {GST[r['PERM_ADD_STATE']]} for {r['PERM_ADD_STATE']}")
    # POI / POA
    if r["PERM_ADD_PROOF"] != r["POA_TYPE"]:
        issue(ctx, "ERROR", "LE-03", "PERM_ADD_PROOF", r["PERM_ADD_PROOF"], f"must equal POA_TYPE {r['POA_TYPE']}")
    for col, code, fcol in (("POI_TYPE", r["POI_TYPE"], "POI_FILE_NAME"), ("POA_TYPE", r["POA_TYPE"], "POA_FILE_NAME")):
        d = LEDOC.get(code)
        if d and r[fcol] != d["image_file"]:
            issue(ctx, "ERROR", "IMG-03", fcol, r[fcol], f"{col} {code} image must be {d['image_file']}")
        if d and ct not in d["constitutions"]:
            issue(ctx, "ERROR", "LE-03", col, code, f"constitution {ct} has no {d['doc']}")
    for col, code, ncol in (("POI_TYPE", r["POI_TYPE"], "POI_NUMBER"), ("POA_TYPE", r["POA_TYPE"], "POA_NUMBER")):
        d, v = LEDOC.get(code), r[ncol]
        if d and v and d["doc"] == "CERTIFICATE_OF_INCORPORATION":
            want = "LLPIN" if ct == "J" else "CIN"
            if not fmt_ok(want, v):
                issue(ctx, "ERROR", "LE-03", ncol, v, f"constitution {ct}: COI number must be {want}")
            elif want == "CIN" and ((ct == "E") != v.startswith("L") or (ct == "E") != ("PLC" in v)):
                issue(ctx, "ERROR", "LE-03", ncol, v, "Public Ltd = L..PLC, Private Ltd = U..PTC")
    if (LEDOC.get(r["POI_TYPE"]) or {}).get("doc") == (LEDOC.get(r["POA_TYPE"]) or {}).get("doc"):
        if r["POI_NUMBER"] != r["POA_NUMBER"] or r["POI_FILE_NAME"] != r["POA_FILE_NAME"]:
            issue(ctx, "ERROR", "POIA-01", "POA_NUMBER", r["POA_NUMBER"], "same LE document -> same number and image")
    elif r["POI_FILE_NAME"] == r["POA_FILE_NAME"]:
        issue(ctx, "ERROR", "POIA-01", "POA_FILE_NAME", r["POA_FILE_NAME"], "different LE documents need different images")
    # comm proof (LE)
    if r["COMM_ADDRESS_IDTYPE"] and r["COMM_ADDRESS_IDTYPE"] not in ("06", "07", "99"):
        issue(ctx, "ERROR", "COMM-02", "COMM_ADDRESS_IDTYPE", r["COMM_ADDRESS_IDTYPE"], "LE: only 06 / 07 / 99")
    if (r["COMM_ADDRESS_IDTYPE"], r["COMM_ADDRESS_NUMBER"]) != (r["POA_TYPE"], r["POA_NUMBER"]):
        issue(ctx, "WARN", "COMM-02", "COMM_ADDRESS_NUMBER", r["COMM_ADDRESS_NUMBER"], "pack: local proof = POA document")
    if r["COMM_ADDRESS_CATEGORY"] and r["COMM_ADDRESS_CATEGORY"] != POA_NAME.get(r["COMM_ADDRESS_IDTYPE"], ""):
        issue(ctx, "ERROR", "COMM-02", "COMM_ADDRESS_CATEGORY", r["COMM_ADDRESS_CATEGORY"], f"must be '{POA_NAME.get(r['COMM_ADDRESS_IDTYPE'], '?')}'")
    # counts / duplicates
    if r["NUM_OF_REL_PER"] != str(len(rows)) and r["NUM_OF_REL_PER"].lstrip("0") != str(len(rows)):
        issue(ctx, "ERROR", "LE-01", "NUM_OF_REL_PER", r["NUM_OF_REL_PER"], f"{len(rows)} RP rows found for this CUST_ID")
    want_img = "01" if r["POI_FILE_NAME"] == r["POA_FILE_NAME"] else "02"
    if r["NUM_OF_IMG"] != want_img:
        issue(ctx, "WARN", "LE-05", "NUM_OF_IMG", r["NUM_OF_IMG"], f"pack convention {want_img}")
    for a, b in (("RES_TEL_CODE", "RESIDENCE_TELEPHONE_NO_STD_CODE"), ("RES_TEL_NUM", "RESIDENCE_TELEPHONE_NO"), ("OFF_TEL_CODE", "OFFICE_TELEPHONE_NO_STD_CODE"),
                 ("OFF_TEL_NUM", "OFFICE_TELEPHONE_NO"), ("MOB_ISD_CODE", "MOBILE_NO_ISD_CODE"), ("MOB_NUM", "MOBILE_NO"), ("FAX_STD_CODE", "FAX_NO_STD_CODE"), ("FAX_NUM", "FAX_NO")):
        if r[a] != r[b]:
            issue(ctx, "WARN", "LE-05", a, r[a], f"should equal {b} ({r[b]})")
    contact_rules(ctx, r, [("RESIDENCE_TELEPHONE_NO_STD_CODE", "RESIDENCE_TELEPHONE_NO"), ("OFFICE_TELEPHONE_NO_STD_CODE", "OFFICE_TELEPHONE_NO"),
                           ("FAX_NO_STD_CODE", "FAX_NO"), ("MOBILE_NO_ISD_CODE", "MOBILE_NO"), ("MOBILE_NO2_ISD_CODE2", "MOBILE_NO2")])
    hier(ctx, r, "ORG_CODE", "ORG_NAME")
    for rr in rows:
        want = (rule or {}).get("rel_type")
        if want and rr["REL_TYPE"] and rr["REL_TYPE"] != want:
            issue(dict(ctx, rp=rr["REL_PER_CUST_ID"]), "WARN", "LE-02", "REL_TYPE", rr["REL_TYPE"], f"constitution {ct} -> REL_TYPE {want}")


# ---------------------------------------------------------------- images
def images_for(persona, rows):
    """-> list of (who, column, file)"""
    out = []
    r0 = rows[0]
    if persona != "LE":
        out += [("APPLICANT", c, r0[c]) for c in ("PHOTO_FILE_NAME", "POI_FILE_NAME", "POA_FILE_NAME") if r0.get(c)]
    else:
        out += [("ENTITY", c, r0[c]) for c in ("POI_FILE_NAME", "POA_FILE_NAME") if r0.get(c)]
    for i, r in enumerate(rows, 1):
        if r.get("REL_TYPE"):
            out += [(f"RP{i}", c, r[c]) for c in ("REL_PER_PHOTO_NAME", "REL_PER_POI_NAME", "REL_PER_PER_ADDRESS_NAME") if r.get(c)]
    return out


def image_rules(ctx, pack, persona, cust, rows, purpose, status):
    folder = pack / "Common_Images" / cust
    if not folder.is_dir():
        issue(ctx, "ERROR", "IMG-01", "CUSTOMER_REFERENCE_NUMBER", cust, "image folder Common_Images/<cust> missing")
        return
    files = {p.name for p in folder.iterdir() if p.is_file()}
    need = images_for(persona, rows)
    names = {f for _, _, f in need}
    if persona == "LE" and "Photo.jpg" in files:
        issue(ctx, "ERROR", "LE-06", "PHOTO", "Photo.jpg", "legal entity must not have a photograph")
    if purpose != "03_IMAGE_PROCESSING":
        for who, col, f in need:
            if f not in files:
                issue(ctx, "ERROR", "IMG-02", col, f, f"{who}: image named in CSV not found in folder")
        for f in sorted(files - names):
            issue(ctx, "ERROR", "IMG-02", "-", f, "extra file in customer folder (not referenced by the CSV)")
    else:
        st = status.get(cust, {})
        for who, col, f in need:
            s = st.get(f)
            if s is None:
                issue(ctx, "ERROR", "IMG-02", col, f, f"{who}: file not listed in IMAGE_STATUS_SUMMARY.csv")
            elif s == "AVAILABLE" and f not in files:
                issue(ctx, "ERROR", "IMG-02", col, f, f"{who}: status AVAILABLE but file missing")
            elif s == "MISSING" and f in files:
                issue(ctx, "ERROR", "IMG-02", col, f, f"{who}: status MISSING but file present")
        for f in sorted(files - names):
            issue(ctx, "ERROR", "IMG-02", "-", f, "extra file in customer folder")


def load_status(pack):
    st = defaultdict(dict)
    p = pack / "IMAGE_STATUS_SUMMARY.csv"
    if not p.exists():
        return st
    for r in csv.DictReader(open(p, encoding="utf-8-sig")):
        if r["PURPOSE"] != "03_IMAGE_PROCESSING":
            continue
        for fc, sc in (("PHOTO_FILE", "PHOTO_STATUS"), ("POI_FILE", "POI_STATUS"), ("POA_FILE", "POA_STATUS")):
            f, s = r[fc], r[sc]
            if s in ("AVAILABLE", "MISSING"):
                if st[r["CUSTOMER_REFERENCE_NUMBER"]].get(f) not in (None, s):
                    st[r["CUSTOMER_REFERENCE_NUMBER"]][f] = "CONFLICT"
                else:
                    st[r["CUSTOMER_REFERENCE_NUMBER"]][f] = s
    return st


def load_dvs(pack):
    d = {}
    p = pack / "SUMMARY.csv"
    if p.exists():
        for r in csv.DictReader(open(p, encoding="utf-8-sig")):
            if r["PURPOSE"] == "02_DVS":
                d[r["CUSTOMER_REFERENCE_NUMBER"]] = r["DVS_DEFECT_FIELD"]
    return d


# ---------------------------------------------------------------- record
def check_record(ctx, persona, cat, rr, positive, upload):
    """All data checks for one record (rr = [(line, row), ...]); images and cross-pack uniqueness excluded."""
    spec = SPEC["personas"][persona]
    cust = ctx["cust"]
    r0 = rr[0][1]
    with_rp = persona == "LE" or cat == "02_Minor_With_RP"
    skip = {} if with_rp else {"RP": "minor without RP"}
    check_fields(ctx, r0, spec["columns"], skip, positive)
    if persona == "MIN" and not with_rp and r0.get("CUST_ID") != cust:
        issue(ctx, "ERROR", "CUST-01", "CUST_ID", r0.get("CUST_ID"), "must equal CUSTOMER_REFERENCE_NUMBER")
    if persona == "MIN" and with_rp and r0.get("REL_TYPE") != "1":
        issue(ctx, "ERROR", "MINRP-01", "REL_TYPE", r0.get("REL_TYPE"), "Minor with RP needs REL_TYPE 1 (Guardian)")
    if not re.fullmatch(CFG["customer_ref_pattern"][persona], cust or ""):
        issue(ctx, "WARN", "UNIQ-01", "CUSTOMER_REFERENCE_NUMBER", cust, "pack pattern " + CFG["customer_ref_pattern"][persona])
    if persona == "LE":
        entity_rules(ctx, r0, [x[1] for x in rr], upload)
        for i, (ln, r) in enumerate(rr[1:], 2):
            c2 = dict(ctx, line=ln, rp=r["REL_PER_CUST_ID"])
            check_fields(c2, r, spec["columns"], {"ENTITY": "LE continuation row"}, positive)
    else:
        applicant_rules(ctx, r0, persona, cat, upload)
        if persona == "MIN" and with_rp and r0.get("PERM_ADD_PIN") != r0.get("REL_PER_ADD_PIN"):
            issue(ctx, "WARN", "ADDR-04", "REL_PER_ADD_PIN", r0.get("REL_PER_ADD_PIN"), "guardian lives with the minor (same permanent address)")
    if with_rp:
        for i, (ln, r) in enumerate(rr, 1):
            rp_rules(dict(ctx, line=ln, rp=r["REL_PER_CUST_ID"]), r, i, persona, cust, upload)


# ---------------------------------------------------------------- driver
UNIQ = defaultdict(set)      # value -> owners
UNIQ_WHERE = {}
UNIQ_COLS = {"PAN_OR_FORM60", "REL_PER_PANORFORM60", "POI_NUMBER", "POA_NUMBER", "COMM_ADDRESS_NUMBER", "REL_PER_UID", "REL_PER_VOTER",
             "REL_PER_NREGA", "REL_PER_PASSPORT", "REL_PER_DRIVING", "REL_PER_DIN_NUMBER", "TIN", "MOBILE_NO", "MOBILE_NO2", "EMAIL_ID",
             "EMAIL_ID2", "REL_PER_MOBILE_NO", "REL_PER_EMAILID"}


def uniq_add(ctx, owner, r, cols):
    for c in cols:
        v = r.get(c, "")
        if c in UNIQ_COLS and v and v != "FORM60" and "@#" not in v:
            UNIQ[v].add(owner)
            UNIQ_WHERE.setdefault(v, (c, ctx))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("packs", nargs="+")
    ap.add_argument("--out", default="validation_report")
    ap.add_argument("--upload-date", default=date.today().strftime("%d-%m-%Y"))
    a = ap.parse_args()
    upload = pdate(a.upload_date)
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    per = SPEC["personas"]
    rec_results, cust_seen = [], defaultdict(list)
    for pk in a.packs:
        pack = Path(pk)
        status, dvs = load_status(pack), load_dvs(pack)
        for purpose in CFG["pack_folders"]["purposes"]:
            for cat in CFG["pack_folders"]["categories"]:
                for f in sorted((pack / purpose / cat).glob("*.csv")):
                    persona = "LE" if cat.startswith("04") else ("MIN" if "Minor" in cat else "IND")
                    spec = per[persona]
                    base = {"pack": pack.name, "purpose": purpose, "category": cat, "file": f.name, "line": 1, "cust": "", "persona": persona}
                    raw = f.read_bytes()
                    text = raw.decode("utf-8-sig")
                    lines = text.split("\n")
                    if lines and lines[-1] == "":
                        lines = lines[:-1]
                    hdr = lines[0].rstrip("\r").split("|")
                    if not re.fullmatch(CFG["file_name_pattern"]["LE" if persona == "LE" else "IND_MIN"], f.name):
                        issue(base, "ERROR", "FILE-02", "-", f.name, "file name does not follow CKYC_DATA_IN/LE_DDMMYYYY_NN.csv")
                    if hdr != spec["header"].split("|"):
                        issue(base, "ERROR", "FILE-01", "-", f"{len(hdr)} cols", f"header differs from the {persona} spec header ({spec['column_count']} cols)")
                        continue
                    rows = []
                    for ln, l in enumerate(lines[1:], 2):
                        vals = l.rstrip("\r").split("|")
                        if len(vals) != len(hdr):
                            issue(dict(base, line=ln), "ERROR", "FILE-01", "-", f"{len(vals)} cols", "column count differs from header")
                            continue
                        rows.append((ln, dict(zip(hdr, vals))))
                    # group into records
                    records = []
                    if persona == "LE":
                        cur = None
                        for ln, r in rows:
                            if r["CUSTOMER_REFERENCE_NUMBER"]:
                                cur = [r["CUSTOMER_REFERENCE_NUMBER"], [(ln, r)]]; records.append(cur)
                            elif cur and r["CUST_ID"] == cur[0]:
                                cur[1].append((ln, r))
                            else:
                                issue(dict(base, line=ln, cust=r.get("CUST_ID", "")), "ERROR", "LE-01", "CUST_ID", r.get("CUST_ID", ""), "continuation row not linked to the entity above")
                    else:
                        records = [[r["CUSTOMER_REFERENCE_NUMBER"], [(ln, r)]] for ln, r in rows]
                    positive = purpose != "02_DVS"
                    for cust, rr in records:
                        n_before = len(ISSUES)
                        ctx = dict(base, line=rr[0][0], cust=cust)
                        cust_seen[cust].append(f"{pack.name}/{purpose}/{f.name}")
                        r0 = rr[0][1]
                        with_rp = persona == "LE" or cat == "02_Minor_With_RP"
                        check_record(ctx, persona, cat, rr, positive, upload)
                        owner = cust
                        uniq_add(ctx, owner, r0, [c for c in r0 if not c.startswith("REL_PER")])
                        for ln, r in rr:
                            if r.get("REL_TYPE"):
                                uniq_add(dict(ctx, line=ln, rp=r["REL_PER_CUST_ID"]), r["REL_PER_CUST_ID"], r, [c for c in r if c.startswith("REL_PER")])
                                cust_seen["RP:" + r["REL_PER_CUST_ID"]].append(f"{pack.name}/{purpose}/{f.name}")
                        image_rules(ctx, pack, persona, cust, [x[1] for x in rr], purpose, status)
                        mine = ISSUES[n_before:]
                        errs = [i for i in mine if i["severity"] == "ERROR"]
                        exp = dvs.get(cust, "") if purpose == "02_DVS" else ""
                        rec_results.append({"pack": pack.name, "purpose": purpose, "category": cat, "file": f.name, "customer": cust,
                                            "result": "FAIL" if errs else "PASS", "errors": len(errs),
                                            "warnings": sum(1 for i in mine if i["severity"] in ("WARN", "PACK")),
                                            "error_columns": ";".join(sorted({i["column"] for i in errs})),
                                            "dvs_expected_field": exp,
                                            "dvs_expected_caught": ("YES" if exp in {i["column"] for i in errs} else "NO") if exp else ""})
    for k, where in cust_seen.items():
        if len(where) > 1:
            issue({"pack": "-", "purpose": "-", "category": "-", "file": "; ".join(where), "line": "", "cust": k}, "ERROR", "UNIQ-01", "CUSTOMER_REFERENCE_NUMBER", k, "customer / RP id used more than once")
    for v, owners in UNIQ.items():
        if len(owners) > 1:
            c, ctx = UNIQ_WHERE[v]
            issue(ctx, "ERROR", "UNIQ-02", c, v, f"same value used by {len(owners)} different people: {sorted(owners)[:4]}")
    # write
    keys = ["severity", "rule", "pack", "purpose", "category", "file", "line", "customer", "rp_cust_id", "column", "value", "message"]
    with open(out / "issues.csv", "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, keys); w.writeheader(); w.writerows(ISSUES)
    with open(out / "record_results.csv", "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, list(rec_results[0])); w.writeheader(); w.writerows(rec_results)
    summ = defaultdict(lambda: Counter())
    for r in rec_results:
        k = f"{r['purpose']}/{r['category']}"
        summ[k]["records"] += 1; summ[k][r["result"]] += 1
        if r["dvs_expected_field"]:
            summ[k]["dvs_expected_caught_" + r["dvs_expected_caught"]] += 1
    sev = Counter((i["purpose"], i["severity"]) for i in ISSUES)
    rules = Counter((i["purpose"], i["severity"], i["rule"]) for i in ISSUES)
    res = {"records": {k: dict(v) for k, v in sorted(summ.items())}, "issues_by_purpose_severity": {f"{a}|{b}": n for (a, b), n in sorted(sev.items())},
           "issues_by_rule": {f"{a}|{b}|{c}": n for (a, b, c), n in sorted(rules.items())}}
    (out / "summary.json").write_text(json.dumps(res, indent=1))
    print(json.dumps(res["records"], indent=1))
    print("issues:", dict(sev))
    bad = [r for r in rec_results if (r["purpose"] != "02_DVS" and r["result"] == "FAIL") or (r["purpose"] == "02_DVS" and r["result"] == "PASS")]
    print("unexpected record outcomes:", len(bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
