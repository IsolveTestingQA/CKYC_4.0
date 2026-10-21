---
name: reference-ckyc-test-data-location
description: "Where CKYC bulk-upload CSV test-data packs live on disk, and their naming/series convention — needed for the Customer Information / bulk import automation."
metadata:
  node_type: memory
  type: reference
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T14:42:30.422Z
---

Bulk CSV test-data files for CKYC live **outside** the CKYC_4.0 project repo, in a separate external folder tree:

```
D:\CKYC_Test_Data_4.0\CKYC_LE_Test_Data\03_TEST_DATA\Kotak\DEV\<Pack folder>\
    01_INDIVIDUAL\CKYC_DATA_IN_<DDMMYYYY>_<series>.csv
    02_MINOR\CKYC_DATA_IN_<DDMMYYYY>_<series>.csv
    03_LEGAL_ENTITY\CKYC_DATA_LE_<DDMMYYYY>_<series>.csv
```

Example pack seen 2026-09-30: `Sep_10_2026_Pack6_DVS_Common_Images`, containing `CKYC_DATA_IN_10092026_31.csv` (plain Individual), `CKYC_DATA_IN_10092026_33.csv` (Individual + Minor with guardian/RP), `CKYC_DATA_LE_10092026_35.csv` (Legal Entity with authorized signatory).

Filename rule (app-enforced, not just convention): `CKYC_DATA_<IN|LE>_<DDMMYYYY>_<series>` — the date segment must equal **today's date** when uploading, and the series number must not already exist for that date/type or the CKYC app rejects the upload (red toast, duplicate-file error).

See [[project-customer-information-module]] for how this feeds the bulk-import automation flow.
