---
phase: quick-261028-n5v
plan: 01
subsystem: architecture-views
tags: [likec4, docs, evidence, standard, 401, physical-view]
requires:
  - phase: 02-model-completion
    provides: the model, the evidence file and the standard that 02-VERIFICATION.md found overstated
provides:
  - error-401 view, evidence section, measurement record and standard stating the 401 path as measured before 17a94cb and fixed by it
  - issuer, host-filter and monitoring text matching the manifests, the script and the monitor settings
  - standard claims narrowed to what is drawn (Scenarios evidence, Mermaid parity, monitoring defaults)
affects: [architecture-views phase 2 re-verification, phase 3 publication]
actuals:
  tokens: 8600
  tasks: 3
  commits: 3
plan_head_before: ff741e4
plan_head_after: ee9f9bee97a41d43907bd4cacc4b1eb4ee853dc4
tech-stack:
  added: []
  patterns: [positional evidence cites re-read after the cited file changed]
key-files:
  created: []
  modified:
    - docs/architecture/model/scenarios-app.c4
    - docs/architecture/model/scenarios-platform.c4
    - docs/architecture/model/edge.c4
    - docs/architecture/model/monitoring.c4
    - docs/DIAGRAM_CONVENTIONS.md
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-04-401-measure.txt
key-decisions:
  - "ROADMAP.md Phase 2 criterion 3 left unchanged: its clause '401 with no SQL step' is still true of the view"
  - "Staging issuer edge kept; only the descriptions changed"
  - "Monitoring scope: narrower reading (keep the drawn scope, state what is not drawn), the orchestrator's assumption pending the user"
requirements-completed: [SCEN-02, SCEN-05, PHYS-03, PHYS-04, STD-01]
duration: not recorded
completed: 2026-10-28
status: complete
---

# Phase quick-261028-n5v Plan 01: Close phase 02 verification gaps Summary

**The error-401 view, its evidence, the measurement record and the standard now say the 401 path was measured before the fix and fixed in 17a94cb, with the issuer, host-filter, monitoring and standard claims narrowed to what the sources show.**

## Performance

- **Tasks:** 3 of 3
- **Files modified:** 7 (all under docs/ or .planning/)
- **Completed:** 2026-10-28T16:28Z (start time not recorded)
- **Base:** `BASE=ff741e4`

## Task Commits

1. **Task 1: the 401 path as measured, then fixed** - `3ea3414` (docs)
2. **Task 2: issuer, host-filter and monitoring text** - `4d13696` (docs)
3. **Task 3: the standard claims only what is drawn** - `ee9f9be` (docs)

All three used `git commit --no-verify`, allowed because every path is under docs/ or .planning/ (project CLAUDE.md). That skipped the hook's gitleaks and LikeC4 steps; the gates below replaced them. Gitleaks was not run by this task, and CI will run it on a push.

## What was changed

- **error-401 description** (`scenarios-app.c4`): still 3 participants, 4 steps, no SQL step. It now states the pre-fix measurement (JSON Accept clean; a bare wildcard or a browser navigation Accept saved the request in a new session, one row and a JSESSIONID per request), the fix in 17a94cb, the in-process test with its 6-of-8 red and 0-failure green, and the limits.
- **Evidence file**: the old verdict paragraph is replaced by a provenance block (bullets, not numbered rows, so steps still equal rows); five SecurityConfiguration.java cites re-read; inbound-packet-path row 3 gains the ICMP step title and the `k3s-host-firewall.sh:63` cite and quote.
- **02-04-401-measure.txt**: a dated follow-up section appended; 24 lines added, 0 deleted; one `VERDICT:` line still.
- **edge.c4**: staging issuer described as referenced by no Certificate, production issuer as the one all three Certificates name, host filter as admitting TCP on an allow-listed port or ICMP.
- **scenarios-platform.c4**: inbound-packet-path step 3 title names ICMP.
- **monitoring.c4 and DIAGRAM_CONVENTIONS.md**: the chart default scrape targets are stated as not drawn (Physical row, Checks paragraph, `monitoring` table row, view description).
- **DIAGRAM_CONVENTIONS.md**: Scenarios row limited to the ten Phase 2 views (the three signin views have no section); the Mermaid bullet names the 400 VALIDATION_FAILED section and the signup branches as not drawn; note (b2), the Scenarios row and the `error-401` row say measured before the fix, then fixed.

## Stated plainly

- **No live re-measurement was run after the fix.** Rows A to F in 02-04-401-measure.txt describe the code at 82775fa.
- **"No SQL after the fix" is inferred from the code, not observed.** The test (`AuthorizationGatingTest`, nested `NoSessionSweep`, 8 cases, GET and POST `/boards`) runs in process (MockMvc) and counts new `SPRING_SESSION` primary ids and Set-Cookie names. It counts no statements. The inference rests on three premises, cited in the evidence block: the only measured statements were the saved-request session INSERTs, the fix replaces that cache with one that saves nothing, and the requests the old cache did not save sent no statement. The label `inferred from the code` appears at each of the six sites (gate counts below).
- **The monitoring scope is the orchestrator's assumption, pending the user.** The text says what is drawn and what is not; it never says the user chose it. Human item 2 stays open. If the user wants the default targets drawn, that is a separate change.
- **Chart-default scrape-target read: partial.** I fetched `values.yaml` at tag `kube-prometheus-stack-91.5.2` through the web-access MCP. The tool returned the first 62,574 of 218,683 characters (`truncated: true`; a second fetch at rung 2 hit the same cap). Inside that part: `kubernetesServiceMonitors.enabled: true` at line 1730 and `kubeApiServer.enabled: true` at line 1735. Not read, because they lie past the cut: the kubelet (with cAdvisor), kube-state-metrics and the operator self-monitor defaults. The model description and the Checks paragraph say exactly this: only the API server was confirmed in the chart values; the other three are the chart documented defaults, not read from this repo. In this repo's values file I did read `sed -n '42,52p'` (only controller-manager, scheduler, etcd and kube-proxy are turned off, lines 45 to 52) and lines 169-187 (kube-state-metrics and the operator set resources only).

## SecurityConfiguration.java cites (old to new)

| Where in 02-SCENARIO-EVIDENCE.md | Old | New | Confirmed by `sed -n` |
|---|---|---|---|
| signup-ok row 6 | 163-164 | 174-175 | `SpringSessionBackedSessionRegistry reads the live...` / `count, so its ceiling...` |
| signup-ok row 7 | 186-188 | 197-199 | `received read 1. The new session row...` through `flushed, strictly after...` |
| error-401 row 1 | 108 | 109 | `auth.anyRequest().authenticated();` |
| error-401 row 3 | 116-118 | 117-119 | `http.exceptionHandling(` / `handling ->` / `handling.authenticationEntryPoint(...)` |
| error-401 closing paragraph | 127 | 138 | `session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);` |
| new (fix line) | none | 129 | `http.requestCache(cache -> cache.requestCache(new NullRequestCache()));` |

## Gate results on the final tree (HEAD ee9f9be)

| Gate | Result |
|---|---|
| G1 `sync.mjs check` | exit 0, `OK (56 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)` |
| G2 export | exit 0, `generated` printed |
| G3 physical | `delivery,monitoring,production 0` |
| G4 scenarios | the same 13 lines as the baseline (error-401 `3 4 0`, inbound-packet-path `5 5 0`) |
| G5 steps to rows | ten equal pairs (error-401 `4 4`, inbound-packet-path `5 5`), signin-ok, signin-bad-credentials, signin-ceiling `none` |
| G6 titles | nothing |
| G7 drill replay | `cmp` exit 0, byte-identical, not re-recorded |
| G8 self-test | `16/16 passed` |
| L1, L2, L3 | nothing printed, rg exit 1 (no match) |
| L4 | `2` (the two existing loopback lines) |
| M1 | `24 0`, exit 0 |
| `inferred from the code` | scenarios-app.c4: 1; error-401 evidence section: 2 lines; measure record: 1; note (b2): 1; Scenarios row: 1; `error-401` row: 1 |
| Scope | `git diff --stat ff741e4..HEAD -- k8s infra src scripts sync.mjs drill.fish views.c4` prints nothing; ROADMAP.md unchanged (no commit touches it); `docs/architecture/out/export.json` trashed |

Task acceptance criteria were each run and met; the one deviation from the stated expectations is the partial chart read above.

## Deviations from Plan

None to the work itself. Two execution notes:

- The chart-values fetch was truncated by the tool (see above), so the monitoring wording uses the plan's "documented defaults, not read from this repo" branch for three of the four named targets rather than citing line numbers for all four.
- I wrote the follow-up note in 02-04-401-measure.txt with an `Edit` on the final line (appending text); M1 confirmed 0 lines deleted.

## Observation (not acted on)

`261028-m6k-red.txt` line 31 says "the 6 failures are exactly the 02-04 rows B, D, E shape". Only three of the six failing cases (rows B, D, E: a bare wildcard on GET, a bare wildcard on POST, a browser navigation GET) were measured live; the other three (POST with a browser Accept, and GET and POST with no Accept header) were seen only in the test. That file is committed and was not edited.

## Not done

- No live re-measurement after the fix.
- No PNG re-export: the standard's Phase 2 size column (including 1180 x 530 for `error-401`) stays a dated record from 2026-10-28 and may no longer match the longer description and step title.
- The stale header comment of `k8s/platform/edge/clusterissuers.yaml` lines 1-2 ("every Certificate starts on letsencrypt-staging") is a k8s file and out of scope; it is now contradicted by the three Certificates.
- Out-of-scope findings WR-01, WR-02, WR-03, WR-07, WR-09 and IN-01 to IN-05 untouched; the `hostfilter -> svclb` edge label, the `monitoring` view title, the Mermaid inventory row for `signup` and the docs/wiki copies untouched.
- 02-REVIEW-DISPOSITION rows are left to the orchestrator; STATE.md, PLAN.md and this SUMMARY are not committed by me.
- Candidate tracked item (PATTERN_SELECTION step 4, no checker built): evidence cites are positional and 17a94cb staled five with no failing check. A cite-drift check would be a separate change.
- Gitleaks and the LikeC4 pre-commit step did not run (`--no-verify`); `sync.mjs check`, the self-test and the leak greps replaced them.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or schema change; cookie names only, no values, session ids or new IP literals were written (L1 to L4).

## Self-Check: PASSED

- Commits `3ea3414`, `4d13696`, `ee9f9be` are ancestors of HEAD.
- All seven modified files exist and `git status --short` on them is clean.
