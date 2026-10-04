"""Renders the Markdown field tables (02/03/04) and the generated parts of 01 from ckyc_cosmos_field_spec.json."""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent  # the spec folder
S = json.loads((OUT / "ckyc_cosmos_field_spec.json").read_text())
FMT = S["formats"]


def esc(x):
    return str(x if x is not None else "").replace("|", "\\|").replace("\n", " ")


def allowed(c):
    parts = []
    if c["enum"]:
        en = [e.replace("CONFIG:", "config.") for e in c["enum"]]
        parts.append(", ".join(en) if len(", ".join(en)) < 90 else ", ".join(en[:14]) + " ...")
    f = c["format"]
    if f:
        if f.startswith("BY_"):
            parts.append({"BY_POI_TYPE": "format by POI_TYPE (doc_map)", "BY_POA_TYPE": "format by POA_TYPE (doc_map)",
                          "BY_COMM_IDTYPE": "format by COMM_ADDRESS_IDTYPE", "BY_LE_POI_TYPE": "02: CIN/LLPIN, 03: REG_CERT/GSTIN",
                          "BY_LE_POA_TYPE": "06: CIN/LLPIN, 07: REG_CERT/GSTIN"}[f])
        else:
            parts.append(f"`{f}`")
    return "; ".join(parts)


STATUS = {"M": "**M**", "CM": "CM", "O": "O", "BLANK_FOR_NEW": "BLANK (new)", "SYSTEM_BLANK": "SYSTEM (blank)"}


def table(p, title, intro):
    per = S["personas"][p]
    L = [f"# {title}", "", intro, "",
         f"Columns: **{per['column_count']}** · file prefix `{per['file_prefix']}DDMMYYYY_NN.csv` · pipe `|` delimited · UTF-8 (BOM).",
         "", "Status: **M** mandatory · CM conditional mandatory (see *When*) · O optional · BLANK (new) = must be blank for a NEW upload · SYSTEM = system column, keep blank.",
         "Pack fill = what the Cosmos positive pack does (ALWAYS / BLANK / condition). Formats: `01_VALIDATION_RULES.md` section 10 (regex, examples). Document codes: section 5.", ""]
    block = None
    for c in per["columns"]:
        b = c["block"]
        if b != block:
            block = b
            L += ["", f"## {'Related person block' if b == 'RP' else ('Entity block' if b == 'ENTITY' else 'Applicant block')}"
                  + (" (blank on 03_Minor_Without_RP except CUST_ID)" if p == "MIN" and b == "RP" else "")
                  + (" (blank on LE continuation rows)" if p == "LE" and b == "ENTITY" else ""), "",
                  "| # | Column | Field | Status | When (CM) | Max | Allowed values / format | Pack fill | DB table · CERSAI record | Notes |",
                  "|---|---|---|---|---|---|---|---|---|---|"]
        rec = c["cersai_record"]
        rec = {"20": "Detail 20", "30": "Detail 30", "40": "Detail 40", "70": "Detail 70"}.get(rec, rec)
        mx = esc(c["max_len"] or "")
        if c.get("max_len_db"):
            mx += f" (DB {c['max_len_db']})"
        L.append(f"| {c['col']} | `{c['name']}` | {esc(c['label'])} | {STATUS[c['status']]} | {esc(c['when'] or '')} | {mx} | {esc(allowed(c))} | {esc(c['pack_fill'])} | {c['db_table']} · {rec} | {esc(c['notes'])} |")
    return "\n".join(L) + "\n"


(OUT / "02_FIELD_SPEC_INDIVIDUAL.md").write_text(table("IND", "Individual (IND) - field spec, 99 columns",
    "Folder `01_Individual`. ACC_TYPE 01, MINOR N, CONST_TYPE 01, age >= 18. No related-person columns in this header."))
(OUT / "03_FIELD_SPEC_MINOR.md").write_text(table("MIN", "Minor - field spec, 198 columns",
    "Folders `02_Minor_With_RP` (guardian REL_TYPE 1 on the same row) and `03_Minor_Without_RP` (RP block blank). ACC_TYPE 05, MINOR Y, age < 18. "
    "Columns 1-99 are the Individual columns (Minor comm-address headers use the short form COMM_Add_Line1 ... COMM_Add_Pin); columns 100-198 are the related-person block."))
(OUT / "04_FIELD_SPEC_LEGAL_ENTITY.md").write_text(table("LE", "Legal Entity (multi-RP) - field spec, 189 columns",
    "Folder `04_Legal_Entity_Multi_RP`. Row 1 = entity + RP1; each further RP = a continuation row with columns 1-91 blank, linked by CUST_ID. "
    "NUM_OF_REL_PER = number of rows for that CUST_ID. No entity photograph."))

# generated appendix for the rule book
A = ["", "## 9. Cross-field rule catalogue (machine copy: `cross_field_rules` in the JSON)", "",
     "| Rule | Personas | Severity | Rule | Where to verify | Negative example | Source |", "|---|---|---|---|---|---|---|"]
for r in S["cross_field_rules"]:
    A.append(f"| **{r['id']}** | {', '.join(r['personas'])} | {r['severity']} | {esc(r['rule'])} | {esc(r['verify'])} | {esc(r['negative'])} | {esc(r['source'])} |")
A += ["", "## 10. Value formats (machine copy: `formats` in the JSON)", "",
      "| Format | Regex / master | Meaning | Valid example | Invalid example |", "|---|---|---|---|---|"]
for k, f in FMT.items():
    rx = f.get("regex") or ("master " + f.get("master", ""))
    if f.get("checksum"):
        rx += f" + {f['checksum']} checksum"
    A.append(f"| `{k}` | `{esc(rx)}` | {esc(f['desc'])} | {esc(f.get('ok', ''))} | {esc(f.get('bad', ''))} |")
(OUT / "_appendix.md").write_text("\n".join(A) + "\n")
print("rendered")
