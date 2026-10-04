# Maintaining the spec

The JSON is generated. To change a rule:

1. Edit `build_spec.py` (field definitions, formats, masters) or `rules.json` (cross-field rules).
2. Run `python build_spec.py`. This rewrites `../ckyc_cosmos_field_spec.json`.
3. Run `python render_md.py`. This rewrites the `../02_*`, `../03_*` and `../04_*` field tables and `../_appendix.md`.
4. Rebuild the rule book: concatenate `rulebook_head.md` and `../_appendix.md` into `../01_VALIDATION_RULES.md`, then delete `../_appendix.md`.
5. Re-run `../make_test_cases.py <positive pack>` and `../ckyc_validate.py <packs>`.

`hdr_*.txt` files hold the exact headers of the 30-Sep-2026 Cosmos packs.
