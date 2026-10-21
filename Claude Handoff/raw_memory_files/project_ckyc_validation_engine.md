---
name: project-ckyc-validation-engine
description: "Java port of the CKYC/CERSAI bulk-CSV field validator (Com.Ckyc_4_0.validation package) — verified against the Python reference, but still Cosmos-bank-configured, not Kotak. Gates whether a CSV record is valid before it's used for Customer Information reconciliation."
metadata:
  node_type: memory
  type: project
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T15:01:47.532Z
---

Built 2026-09-30/10-01: a full Java port of the CERSAI V1.3 bulk-CSV record validator, so the Customer Information reconciliation flow (see [[project-customer-information-module]]) can check "is this CSV row even valid" before comparing it against the UI and reporting a mismatch.

## Where it lives

- Source: `src/test/java/Com/Ckyc_4_0/validation/` — `CkycValidationSpec` (loads the JSON rule spec + CERSAI masters), `CkycRecordValidator` (the engine: field-level + cross-field checks, one method per persona mirroring the Python), `SpecExpr` (hand-written evaluator for the spec's Python-style `when`/`pack_fill` condition strings — no `and`/`or` compounds appear in the current spec, but the parser supports them), `Checksums` (Verhoeff for Aadhaar, GSTIN mod-36), `ValidationIssue` (severity/rule/column/value/message).
- Reference material: `CKYC_OFFICIAL_DOCUMENTS/CKYC_Cosmos_Validation_Spec/` (untouched, original) — `ckyc_validate.py` is the Python reference implementation this was ported from; `ckyc_cosmos_field_spec.json` is the actual rule data, copied verbatim into `src/test/resources/CkycValidation/` (+ its `masters/*.csv`) so the Java code loads the *same* source of truth, not a hand-transcribed copy.

## Verified, not just compiled

Cross-checked the Java port against the Python reference by running both on 3 real rows from the Kotak sample CSVs (Individual, Minor+Guardian, Legal Entity+RP1) and diffing every finding (rule id, column, value, message) — after two real bugs found and fixed this way:
1. The LE "continuation row must be blank" check was being re-applied to row 1 itself (row 1 = entity + RP1 combined) instead of only rows 2+, producing dozens of false `FIELD-BLANK` errors.
2. A missing `LE-03` constitution-applicability check (`constitution X has no <document>`) that sits right next to the `IMG-03` check in the Python — copied the one, missed its neighbor.

After both fixes: 32/32, 57/57, and 68/68 issues matched byte-for-byte across the three test records. This is a real verification, not a "the diff is empty because I didn't test enough" situation — treat the engine as trustworthy for the rule logic itself.

**Not ported** (out of scope for "is this one CSV record valid," which was the actual ask): image-file-existence checks (need a real pack folder on disk), cross-record uniqueness (PAN/mobile/email duplicated across records — needs the whole pack), and file/header-structural checks (needs the raw file, not a parsed row). Add these later if the flow needs them — the Python reference (`image_rules`, `uniq_add`, and the `main()` driver in `ckyc_validate.py`) has the logic to port from.

## The open blocker: Cosmos config vs. Kotak data

**The bundled JSON spec's `config` block is Cosmos Co-op Bank's values** (`ORG_CODE=IN0308`, `ORG_NAME=COSMOS CO OP BANK LTD`, customer-ref-number patterns `^COSI\d{8}$` / `COSM` / `COSL` / `COSR`). The actual sample data (and presumably the real Customer Information module target) is **Kotak Mahindra Prime** (`ORG_CODE=IN4098`, `ORG_NAME=KOTAK MAHINDRA PRIME LTD`, customer IDs like `KMX10H0009` — confirmed not matching any Cosmos pattern). Running the engine as-is against Kotak data produces real-looking but **false** `HIER-01` (org code/name) and `UNIQ-01` (customer-ref pattern) findings — not real defects, just a bank-config mismatch.

The rule *engine* itself (formats, checksums, cross-field logic — CERSAI-standard) is bank-agnostic and does not need to change. `CkycValidationSpec.withConfigOverride(Map<String,String>)` exists specifically so a different bank's config can be swapped in without touching the engine.

**Still needed from the user before this can validate real Kotak data without false positives** (do not guess these):
- Kotak's actual customer-reference-number regex pattern per persona (IND/MIN/LE/RP) — only a handful of `KMX10H0009`-style samples seen so far, not enough to infer the exact pattern confidently.
- Whether Kotak's BRID is validated as one fixed value (like Cosmos's `"001"`) or must be a valid code from a branch master list — the sample data shows a varying branch code (`CHN02`), which doesn't fit the Cosmos-style single-fixed-BRID assumption `hier()` currently makes.
- Whether the `pack_dates` concept (fixed DECL_DATETIME/KYC_VERIFY_DATETIME across a whole generated test pack) applies to Kotak's real data at all, or only to Cosmos's own generated test packs.

See [[project-customer-information-module]] for how this plugs into the reconciliation flow, and [[reference-ckyc-test-data-location]] for where the sample CSVs live.
