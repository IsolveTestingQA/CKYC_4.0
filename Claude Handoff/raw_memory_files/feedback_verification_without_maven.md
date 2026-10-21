---
name: feedback-verification-without-maven
description: "How to verify Java changes to CKYC_4.0 when Maven isn't on this machine's PATH — confirmed working approach the user accepted across multiple sessions."
metadata:
  node_type: memory
  type: feedback
  originSessionId: a543b4ab-b8d0-4a52-8519-273b221ffdab
  modified: 2026-09-30T14:43:04.122Z
---

This sandbox has no `mvn` on PATH (confirmed 2026-09-29), but the user's own machine does and they run the suite themselves (IntelliJ or `mvn test`). Claude cannot execute the Cucumber/TestNG integration itself here.

**Confirmed working verification approach** (used successfully across several rounds of framework changes — reporting/retry/screenshot work in late Sept 2026): compile the changed/new `.java` files directly with `javac`, using a classpath built from `target/classes` + `target/test-classes` + a filtered subset of jars under `~/.m2/repository` (filter by groupId keywords: selenium, cucumber, poi, allure, extentreports, slf4j, log4j, aspectj, testng, commons-io, jackson, org.json, velocity — the *unfiltered* full repo scan produces a classpath string too long for `javac`'s argv even via `cygpath -wp`, so always filter). Paths must be converted to Windows form with `cygpath -w`/`cygpath -wp` since git-bash's own POSIX-style paths are unusable by native `javac.exe`. Pass the classpath as a normal argv string (not a javac `@argfile` — argfiles silently failed to pick up an embedded `-cp "...;...;..."` value in testing on 2026-09-29, even well under the OS argv length limit; root cause not fully diagnosed, just avoid argfiles for this).

**Why this matters / how to apply**: this lets Claude catch real compile errors (missing imports, wrong API signatures — e.g. this caught the correct `io.cucumber.testng.PickleWrapper.getPickle().getTags()` signature before writing `RetryAnalyzer`) without needing Maven. But it is *not* a substitute for running the actual suite — always tell the user plainly that runtime/integration behavior (report generation, retry firing correctly, screenshots landing right) still needs to be verified by them with a real run, and ask for specific things to check back. The user has consistently accepted this two-step approach (compile-verify here, runtime-verify by them) without pushback — don't skip the "please verify" step by overclaiming confidence just because `javac` was clean.

**Lesson from a real miss**: an auto-open-report feature was compile-clean but silently didn't work at runtime, because it was placed *after* an unguarded `driver.quit()` call in a teardown method — an exception there aborted the method before reaching the new code, with nothing logged. Since Claude can't runtime-test here, when adding a "final step" to any teardown/shutdown-style method, proactively audit for unguarded prior calls that could throw and silently skip everything after them (order defensively: risky teardown last, wrapped in try/catch) — don't wait for the user to report it broken.
