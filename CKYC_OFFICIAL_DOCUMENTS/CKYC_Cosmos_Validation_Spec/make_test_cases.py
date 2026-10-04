#!/usr/bin/env python3
"""
Generates test_cases.csv (automation test-case catalog) from ckyc_cosmos_field_spec.json and
self-verifies every negative case: the mutation is applied to a real POSITIVE record and
ckyc_validate.check_record() must report the expected rule.

Usage: python make_test_cases.py <positive_pack_folder> [--out test_cases.csv] [--upload-date DD-MM-YYYY]
"""
import argparse, csv, sys
from pathlib import Path

import ckyc_validate as V

SPEC = V.SPEC
FOLDER = {"IND": "01_Individual", "MIN": "02_Minor_With_RP", "MIN0": "03_Minor_Without_RP", "LE": "04_Legal_Entity_Multi_RP"}


def load_records(pack, cat, persona):
    recs = []
    for f in sorted((pack / "01_POSITIVE" / cat).glob("*.csv")):
        lines = f.read_text(encoding="utf-8-sig").split("\n")
        hdr = lines[0].rstrip("\r").split("|")
        rows = [(i, dict(zip(hdr, l.rstrip("\r").split("|")))) for i, l in enumerate(lines[1:], 2) if l.strip()]
        if persona == "LE":
            cur = None
            for ln, r in rows:
                if r["CUSTOMER_REFERENCE_NUMBER"]:
                    cur = [(ln, r)]; recs.append(cur)
                else:
                    cur.append((ln, r))
        else:
            recs += [[x] for x in rows]
        if len(recs) > 60:
            break
    return recs


def pick(recs, pred):
    for rr in recs:
        try:
            if pred(rr):
                return rr
        except Exception:
            pass
    return recs[0]


def run(persona, cat, rr, upload):
    V.ISSUES.clear()
    ctx = {"pack": "TC", "purpose": "01_POSITIVE", "category": cat, "file": "-", "line": 1, "cust": rr[0][1]["CUSTOMER_REFERENCE_NUMBER"], "persona": persona}
    V.check_record(ctx, persona, cat, rr, True, upload)
    return [(i["severity"], i["rule"], i["column"]) for i in V.ISSUES]


def mutate(rr, row_index, changes):
    new = [(ln, dict(r)) for ln, r in rr]
    r = new[row_index][1]
    for k, v in changes.items():
        r[k] = v(r) if callable(v) else v
    return new


CROSS = [  # (persona key, row index, precondition, changes, expected rule, description)
    ("IND", 0, lambda r: r["PERM_TO_COMM_FLG"] == "Y", {"COMM_Add_Pin": lambda r: "600001" if r["PERM_ADD_PIN"] != "600001" else "600002"}, "ADDR-02", "Flag Y but correspondence PIN differs from permanent"),
    ("IND", 0, lambda r: r["PERM_TO_COMM_FLG"] == "N", {k: (lambda r, k=k: r[k.replace("COMM_Add_Line", "PERM_ADD_LINE").replace("COMM_Add_Pin", "PERM_ADD_PIN")]) for k in ("COMM_Add_Line1", "COMM_Add_Pin")}, "ADDR-02", "Flag N but correspondence address = permanent"),
    ("IND", 0, lambda r: True, {"PERM_ADD_PIN": "999999"}, "ADDR-01", "PIN not in pincode master"),
    ("IND", 0, lambda r: r["PERM_ADD_STATE"] == "TN", {"PERM_ADD_STATE": "MH"}, "ADDR-01", "State does not match PIN"),
    ("IND", 0, lambda r: True, {"PERM_ADD_DIST": "UNKNOWN DISTRICT"}, "ADDR-01", "District does not match PIN"),
    ("IND", 0, lambda r: r["POI_TYPE"] == "E", {"POI_FILE_NAME": "Passport.jpg"}, "IMG-03", "POI type Aadhaar but image name Passport.jpg"),
    ("IND", 0, lambda r: r["POI_TYPE"] == "E", {"POI_CATEGORY": "Passport"}, "POI-01", "POI_CATEGORY does not match POI_TYPE"),
    ("IND", 0, lambda r: True, {"PERM_ADD_PROOF": lambda r: "02" if r["POA_TYPE"] != "02" else "01"}, "POA-01", "PERM_ADD_PROOF different from POA_TYPE"),
    ("IND", 0, lambda r: r["POI_TYPE"] == "E" and r["POA_TYPE"] == "01", {"POA_NUMBER": "234567890124"}, "POIA-01", "Same document (Aadhaar) with two different numbers"),
    ("IND", 0, lambda r: r["PERM_TO_COMM_FLG"] == "N", {"COMM_ADDRESS_IDTYPE": "02", "COMM_ADDRESS_CATEGORY": "Passport"}, "COMM-01", "Flag N with an OVD (Passport) as correspondence proof"),
    ("IND", 0, lambda r: r["PERM_TO_COMM_FLG"] == "Y", {"COMM_ADDRESS_NUMBER": "EB123456789012"}, "COMM-01", "Flag Y but correspondence proof number differs from POA number"),
    ("IND", 0, lambda r: True, {"DOB": "15-10-2027"}, "DATE-01", "DOB in the future"),
    ("IND", 0, lambda r: True, {"DECL_DATETIME": "30-09-2026"}, "DATE-01", "Declaration date = upload date"),
    ("IND", 0, lambda r: True, {"DOB": "01-01-2012"}, "AGE-01", "Individual below 18"),
    ("IND", 0, lambda r: True, {"FATHERORSPOUSE_FIRST_NAME": "", "MOTHER_FIRST_NAME": "", "FORS_FLG": ""}, "NAME-01", "No father / spouse / mother name"),
    ("IND", 0, lambda r: True, {"PAN_OR_FORM60": lambda r: r["PAN_OR_FORM60"][:3] + "C" + r["PAN_OR_FORM60"][4:]}, "PAN-01", "Individual PAN with 4th letter C"),
    ("IND", 0, lambda r: True, {"MOBILE_NO": ""}, "CONTACT-01", "ISD code given but mobile blank"),
    ("IND", 0, lambda r: True, {"ORG_CODE": "IN9999"}, "HIER-01", "Wrong ORG_CODE"),
    ("MIN", 0, lambda r: True, {"DOB": "01-01-2000"}, "AGE-02", "Minor aged 18+"),
    ("MIN", 0, lambda r: True, {"POI_TYPE": "B", "POI_CATEGORY": "Voter ID", "POI_FILE_NAME": "VoterID.jpg", "POI_NUMBER": "ABC1234567"}, "MINDOC-01", "Minor with Voter ID"),
    ("MIN", 0, lambda r: r["PERM_TO_COMM_FLG"] == "N", {"COMM_ADDRESS_IDTYPE": "14", "COMM_ADDRESS_CATEGORY": "Letter of Allotment of Accommodation", "COMM_ADDRESS_NUMBER": "ALT123456789012"}, "COMM-01", "Minor correspondence proof 14"),
    ("MIN", 0, lambda r: True, {"REL_TYPE": "4"}, "MINRP-01", "Minor with RP where REL_TYPE is not 1"),
    ("MIN", 0, lambda r: True, {"REL_PER_DOB": "10-05-2014"}, "AGE-03", "Guardian below 18"),
    ("MIN", 0, lambda r: r["REL_PER_SAMEASPERM_ADD_FLAG"] == "N", {"REL_PER_CURR_ADD_PROOF": "03", "REL_PER_DRIVING": ""}, "RPID-02", "RP current-address proof 03 without REL_PER_DRIVING"),
    ("MIN", 0, lambda r: r["REL_PER_SAMEASPERM_ADD_FLAG"] == "N", {"REL_PER_CURR_ADD_PROOF": lambda r: r["REL_PER_ADD_PROF"]}, "RPID-03", "RP flag N with current proof = permanent proof"),
    ("MIN", 0, lambda r: r["REL_PER_SAMEASPERM_ADD_FLAG"] == "Y", {"REL_PER_CURR_ADD_PIN": lambda r: "600001" if r["REL_PER_ADD_PIN"] != "600001" else "600002"}, "ADDR-03", "RP flag Y but current PIN differs"),
    ("MIN", 0, lambda r: True, {c: "" for c in ["REL_PER_UID", "REL_PER_VOTER", "REL_PER_NREGA", "REL_PER_PASSPORT", "REL_PER_DRIVING", "REL_PER_POPULATION_LET", "REL_PER_EKYC", "REL_PER_OFFLINE_UID"]}, "RPID-01", "No RP identity number at all"),
    ("MIN", 0, lambda r: r["REL_PER_EKYC"] != "", {"REL_PER_EKYC": lambda r: str((int(r["REL_PER_UID"][-4:]) + 1) % 10000).zfill(4)}, "RPID-04", "E-KYC value not the last 4 of UID"),
    ("MIN", 0, lambda r: True, {"REL_PER_PHOTO_NAME": "RP2_Photo.jpg"}, "RPIMG-01", "Guardian photo named RP2_"),
    ("MIN", 0, lambda r: True, {"REL_PER_ORG_CODE": "IN9999"}, "HIER-01", "RP org code different from FI"),
    ("MIN", 0, lambda r: True, {"CUST_ID": "COSM99999999"}, "CUST-01", "CUST_ID not the minor's reference"),
    ("MIN0", 0, lambda r: True, {"REL_TYPE": "1"}, "FIELD-BLANK", "Minor without RP carrying RP data"),
    ("LE", 0, lambda r: True, {"REL_TYPE": "8"}, "LE-02", "Private Ltd with Partner instead of Director"),
    ("LE", 0, lambda r: r["REL_TYPE"] == "4", {"REL_PER_DIN_NUMBER": ""}, "LE-02", "Director without DIN"),
    ("LE", 0, lambda r: True, {"POI_TYPE": "E"}, "FIELD-ENUM", "Individual POI code on a legal entity"),
    ("LE", 0, lambda r: True, {"PERM_ADD_PROOF": "01", "POA_TYPE": "01"}, "FIELD-ENUM", "Individual POA code on a legal entity"),
    ("LE", 0, lambda r: True, {"TIN": lambda r: r["TIN"][:14] + ("A" if r["TIN"][14] != "A" else "B")}, "FIELD-FMT", "GSTIN checksum wrong"),
    ("LE", 0, lambda r: True, {"TIN": lambda r: r["TIN"][:2] + "ZZZZZ" + r["TIN"][7:]}, "LE-04", "GSTIN does not contain the entity PAN"),
    ("LE", 0, lambda r: True, {"DATE_OF_COMMENCEMENT": "01-01-1990", "DATE_OF_INC": "01-01-2000"}, "DATE-03", "Commencement before incorporation"),
    ("LE", 0, lambda r: True, {"NUM_OF_REL_PER": "3"}, "LE-01", "NUM_OF_REL_PER does not match RP rows"),
    ("LE", 1, lambda r: True, {"APPL_FULL_NAME": "DUPLICATE ENTITY NAME"}, "FIELD-BLANK", "Entity data repeated on continuation row"),
    ("LE", 0, lambda r: True, {"COMM_ADDRESS_IDTYPE": "11"}, "COMM-02", "Deemed proof on a legal entity"),
    ("LE", 0, lambda r: True, {"PAN_OR_FORM60": lambda r: r["PAN_OR_FORM60"][:3] + "P" + r["PAN_OR_FORM60"][4:]}, "PAN-01", "Company PAN with 4th letter P"),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("pack")
    ap.add_argument("--out", default="test_cases.csv")
    ap.add_argument("--upload-date", default="30-09-2026")
    a = ap.parse_args()
    upload = V.pdate(a.upload_date)
    pack = Path(a.pack)
    base = {k: load_records(pack, FOLDER[k], "LE" if k == "LE" else ("IND" if k == "IND" else "MIN")) for k in FOLDER}
    rules_text = {r["id"]: r["rule"] for r in SPEC["cross_field_rules"]}
    out, n = [], 0

    def add(key, typ, col, pre, action, exp_rule, desc, rr, row_idx, changes):
        nonlocal n
        persona = "LE" if key == "LE" else ("IND" if key == "IND" else "MIN")
        got = run(persona, FOLDER[key], mutate(rr, row_idx, changes), upload)
        ok = any(g[1] == exp_rule for g in got if g[0] in ("ERROR", "WARN"))
        n += 1
        out.append({"TC_ID": f"TC-{persona}-{n:04d}", "PERSONA": persona, "FOLDER": FOLDER[key], "TYPE": typ, "COLUMN": col,
                    "PRECONDITION": pre, "ACTION": action, "EXPECTED": "REJECT / DVS error on " + (col or "record"),
                    "EXPECTED_RULE": exp_rule, "RULE_TEXT": rules_text.get(exp_rule, desc), "DESCRIPTION": desc,
                    "SELF_CHECK": "PASS" if ok else "FAIL", "BASE_RECORD": rr[0][1]["CUSTOMER_REFERENCE_NUMBER"]})

    # positive controls
    for key in FOLDER:
        persona = "LE" if key == "LE" else ("IND" if key == "IND" else "MIN")
        rr = base[key][0]
        got = [g for g in run(persona, FOLDER[key], rr, upload) if g[0] == "ERROR"]
        n += 1
        out.append({"TC_ID": f"TC-{persona}-{n:04d}", "PERSONA": persona, "FOLDER": FOLDER[key], "TYPE": "POSITIVE_CONTROL", "COLUMN": "",
                    "PRECONDITION": "unchanged positive record", "ACTION": "upload as is", "EXPECTED": "ACCEPT (no DVS error, pushed to CKYC)",
                    "EXPECTED_RULE": "", "RULE_TEXT": "All field + cross-field rules pass", "DESCRIPTION": "Positive control",
                    "SELF_CHECK": "PASS" if not got else "FAIL", "BASE_RECORD": rr[0][1]["CUSTOMER_REFERENCE_NUMBER"]})
    # field-level
    for key in FOLDER:
        persona = "LE" if key == "LE" else ("IND" if key == "IND" else "MIN")
        with_rp = key in ("MIN", "LE")
        for c in SPEC["personas"][persona]["columns"]:
            name, st = c["name"], c["status"]
            if c["block"] == "RP" and not with_rp:
                continue
            if key == "MIN" and c["block"] != "RP":
                continue          # applicant block already covered by IND + MIN0
            if key == "MIN0" and c["block"] == "RP":
                continue
            row_idx = 0
            if st == "M":
                add(key, "MANDATORY_BLANK", name, "-", f"{name} = '' (blank)", "FIELD-M", f"{c['label']} is mandatory", base[key][0], row_idx, {name: ""})
            elif st == "CM" and c["when"]:
                rr = pick(base[key], lambda rr: V.cond(c["when"], rr[0][1]) and rr[0][1].get(name))
                if V.cond(c["when"], rr[0][1]) and rr[0][1].get(name):
                    add(key, "CONDITIONAL_BLANK", name, c["when"], f"{name} = '' (blank)", "FIELD-CM", f"{c['label']} mandatory when {c['when']}", rr, row_idx, {name: ""})
            elif st in ("BLANK_FOR_NEW", "SYSTEM_BLANK"):
                add(key, "MUST_BE_BLANK", name, "APPL_TYPE = 01", f"{name} = '01'", "FIELD-BLANK", f"{c['label']} must be blank for NEW", base[key][0], row_idx, {name: "01"})
                continue
            f = c["format"]
            if f and not f.startswith("BY_"):
                bad = V.FMT[f].get("bad") or "@#1"
                rr = pick(base[key], lambda rr: rr[0][1].get(name))
                add(key, "FORMAT", name, f"format {f}", f"{name} = '{bad}'", "FIELD-FMT", V.FMT[f]["desc"], rr, row_idx, {name: bad})
            elif f and f.startswith("BY_"):
                rr = pick(base[key], lambda rr: rr[0][1].get(name))
                add(key, "FORMAT_BY_TYPE", name, f"format by {f[3:]}", f"{name} = '12@#34'", "FIELD-FMT", "number must match its document type format", rr, row_idx, {name: "12@#34"})
            if c["enum"] and not any(str(e).startswith("MASTER:") for e in c["enum"]):
                rr = pick(base[key], lambda rr: rr[0][1].get(name))
                add(key, "ALLOWED_VALUES", name, f"allowed {c['enum'][:12]}", f"{name} = 'ZZ'", "FIELD-ENUM", "value outside allowed list", rr, row_idx, {name: "ZZ"})
            if c["max_len"] and (f in (None, "NAME", "ADDRESS_LINE", "CITY", "PLACE", "ENTITY_NAME", "DESIGNATION", "ALNUM")) and not c["enum"]:
                rr = pick(base[key], lambda rr: rr[0][1].get(name))
                add(key, "MAX_LENGTH", name, f"max {c['max_len']}", f"{name} = {int(c['max_len']) + 1} characters", "FIELD-LEN", f"max length {c['max_len']}", rr, row_idx, {name: "A" * (int(c["max_len"]) + 1)})
    # cross-field
    for key, idx, pre, changes, rule, desc in CROSS:
        rr = pick(base[key], lambda rr: len(rr) > idx and pre(rr[idx][1]))
        add(key, "CROSS_FIELD", ",".join(changes), "see description", "; ".join(f"{k} changed" for k in changes), rule, desc, rr, idx, changes)
    with open(a.out, "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, list(out[0])); w.writeheader(); w.writerows(out)
    bad = [o for o in out if o["SELF_CHECK"] != "PASS"]
    print(f"{len(out)} test cases written to {a.out}; self-check failures: {len(bad)}")
    for o in bad[:30]:
        print("  FAIL", o["TC_ID"], o["FOLDER"], o["COLUMN"], o["EXPECTED_RULE"], o["DESCRIPTION"])
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
