---
phase: quick-261028-n5v
verified: 2026-10-28T16:45:00Z
status: human_needed
score: 9/11 must-haves verified
covered_files:
  - ".planning/workstreams/architecture-views/quick/261028-n5v-close-phase-02-verification-gaps-error-4/261028-n5v-PLAN.md"
  - ".planning/workstreams/architecture-views/quick/261028-n5v-close-phase-02-verification-gaps-error-4/261028-n5v-SUMMARY.md"
  - "docs/DIAGRAM_CONVENTIONS.md"
  - "docs/architecture/model/edge.c4"
  - "docs/architecture/model/monitoring.c4"
  - "docs/architecture/model/scenarios-app.c4"
  - "docs/architecture/model/scenarios-platform.c4"
covered_digest: "v3:sha256:4d2e13d5cd92f1e61f8dbeddc77b4323976a7bc884dffe9cfa3d657c31471591"
behavior_unverified: 1
overrides_applied: 0
behavior_unverified_items:
  - truth: "ROADMAP Phase 2 criterion 3: error-401 is a 401 with no SQL step, after the fix in 17a94cb"
    test: "With the app on a Postgres this repo owns, send GET and POST /api/boards with no Cookie header and with Accept */* and no Accept, with log_statement = 'all'; record Set-Cookie, the spring_session row count before and after, and every statement logged"
    expected: "401 UNAUTHENTICATED, no Set-Cookie, no spring_session row, no statement logged"
    why_human: "The only post-fix evidence is the in-process MockMvc test AuthorizationGatingTest.NoSessionSweep, which counts new SPRING_SESSION primary ids and Set-Cookie names. It counts no statements, and no live run followed the fix. The text states this and labels the absence of SQL 'inferred from the code', which is honest, but the invariant itself is unobserved."
human_verification:
  - test: "Decide the monitoring scope: should the monitoring view draw the kube-prometheus-stack default scrape targets (kubelet with cAdvisor, kube-state-metrics, API server, operator self-monitor)?"
    expected: "Either confirm the narrower reading (the view keeps its drawn scope and the text says what is not drawn), or ask for the targets to be drawn as a separate change"
    why_human: "ROADMAP criterion 1 says 'A monitoring view shows the monitoring nodes, with an edge to each thing they scrape'. The task kept the drawn scope and stated the gap, on the orchestrator's assumption, not on a user answer. The user has not answered."
---

# Quick task 261028-n5v: Verification Report

**Task goal:** Close the phase 02 verification gaps in the architecture model and standard (text and drawing only): error-401 stated as measured-then-fixed, WR-04, WR-05, WR-06, WR-08, and the monitoring scope wording.
**Verified:** 2026-10-28T16:45Z, on HEAD ee9f9be (commits 3ea3414, 4d13696, ee9f9be on top of ff741e4)
**Status:** human_needed
**Re-verification:** No, initial verification

Every reworded claim was re-read against the file it cites. SUMMARY.md was used only as a map. All the text-level must-haves hold. Two items cannot be closed from the repo: the post-fix absence of SQL was never observed, and the monitoring scope waits on the user.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | error-401 view description and evidence section state the pre-fix measurement, the 17a94cb fix, the test with red 6 of 8 and green 0, and the limits (no live re-measurement, in-process MockMvc, /boards only, rows and cookie names not statements, no-SQL inferred) | VERIFIED | `scenarios-app.c4` diff carries every element. The evidence file has the provenance block. Rows A to F re-read in `02-04-401-measure.txt` lines 74-79: A, C, F no cookie, no row, `none`; B, D, E JSESSIONID, one row each (lines 75, 77, 78). The no-statement-for-A/C/F premise is at line 83, limits at 125-128, verdict at 134. The red file says `tests="30" failures="6"` (8 new cases, 22 old), the green file `failures="0"` at line 15. `AuthorizationGatingTest.java`: `NoSessionSweep` at 382, `anonymousRefusalCases` at 354, the test at 412, with four Accept variants (json, wildcard, browser navigation, none) times GET and POST /boards. `rg -c 'inferred from the code'`: scenarios-app.c4 1, evidence 2, measure record 1, DIAGRAM_CONVENTIONS.md 3 (note b2, Scenarios row, error-401 row). The wording never claims a live re-measurement. |
| 2 | error-401 keeps 3 participants, 4 steps, no SQL step; evidence keeps 4 numbered rows | VERIFIED | Export of the model: `error-401 scenarios 3 4 0`. Steps-to-rows: `error-401 4 4`. The 13 dynamic-view lines match the plan baseline exactly. |
| 3 | Every SecurityConfiguration.java cite in the evidence file is the line that holds its quote at HEAD | VERIFIED | `sed -n` on the source: 109 `auth.anyRequest().authenticated();`; 117-119 `http.exceptionHandling(` / `handling ->` / `handling.authenticationEntryPoint(...)`; 129 `http.requestCache(cache -> cache.requestCache(new NullRequestCache()));`; 138 `session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);`; 174-175 `SpringSessionBackedSessionRegistry reads the live...` / `count, so its ceiling...`; 197-199 `received read 1...` / `connection, until...` / `flushed, strictly after...`. Line 129 sits inside `securityFilterChain` (declared at 85), the main chain. |
| 4 | `02-04-401-measure.txt` ends with a dated note naming 17a94cb; 0 deleted lines; one `VERDICT:` line | VERIFIED | `git diff --numstat ff741e4 HEAD` on the file: `24 0`. `grep -c '^VERDICT:'` prints 1, no `DECISION:` line added. The note is headed `=== Follow-up (2026-10-28, quick task 261028-n5v): fixed in 17a94cb ===`. Loopback IP count stays 2. |
| 5 | edge.c4 describes staging as referenced by no Certificate and production as named by all three | VERIFIED | `rg -n 'letsencrypt-' k8s/`: the only `issuerRef` lines are `overlays/prod/certificate.yaml:11`, `overlays/nonprod/certificate.yaml:11`, `platform/edge/certificate-monitoring.yaml:13`, all `letsencrypt-production`. No Certificate names staging. Exactly three files hold `kind: Certificate`. The `certmanager.staging -> acme` edge is kept, as planned. |
| 6 | hostfilter, inbound-packet-path step 3 and its evidence row say ICMP returns from the chain and cite line 63 | VERIFIED | `infra/vm/k3s-host-firewall.sh:63` is `-A ${CHAIN} -p icmp -j RETURN`, ahead of the NEW DROP at 64. edge.c4 and the step title both name ICMP. Evidence row 3 cites 61, 62, 63, 64 with the matching verbatim quote. The step title in the evidence file equals the title in the .c4 file. |
| 7 | The monitoring view description and, in the standard, the Physical row, the Checks paragraph and the `monitoring` row say the chart default scrape targets are not drawn | VERIFIED (text) | All four sites carry it. The repo values file turns off exactly kubeControllerManager, kubeScheduler, kubeEtcd and kubeProxy (`kube-prometheus-stack.yaml` lines 45 to 52, as cited). I fetched the chart values at tag `kube-prometheus-stack-91.5.2` (6203 lines): `kubelet.enabled: true` (1838-1839), `kubelet` `cAdvisor: true` (1944), `kubeApiServer.enabled: true` (key at 1735, `enabled` at 1736), `kubeStateMetrics.enabled: true` (2835-2836), `prometheusOperator.enabled: true` (2981-2982), `selfMonitor: true` present. The text says only the API server was read and the other three are chart documented defaults. That understates what is true, it never overstates. Whether this is the right scope is item 11. |
| 8 | Standard: Scenarios row limited to the ten Phase 2 views and the three signin views said to have no section; Mermaid bullet names 400 VALIDATION_FAILED and the signup branches as not drawn; note (b2), Scenarios row and error-401 row say measured then fixed | VERIFIED | Steps-to-rows run now: ten equal pairs, `signin-ok`, `signin-bad-credentials`, `signin-ceiling` print `none`. `error-status-split.mmd:22` is `Note over C,H: 400 -- body fails validation` and no LikeC4 view draws it. `signup.mmd`: validation alt at 9, email taken at 13, lost race at 17, authentication-fails alt at 23 with `DELETE user (rollback)` at 24, none of which `signup-ok` (9 steps, success path) draws. Commit 8b7e379 exists and the if9 T4 summary lists the lines per class, not per step. `unmeasured`/`not measured`/`did not run` no longer occurs in the standard, except the unrelated `**Unmeasured.**` heading about layout. |
| 9 | Gate on the final tree | VERIFIED (re-run parts) | Re-run by me: `sync.mjs check` exit 0, `OK (56 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)`. `scripts/pre-commit-likec4-selftest.py` exit 0, `16/16 passed`. Export to /tmp: physical `delivery,monitoring,production 0`. Scenarios: the 13 baseline lines. Steps to rows: ten equal pairs, three `none`. L1, L2, L3 print nothing; L4 prints 2. Not re-run by me: the drill replay (about 2 minutes, which the task did not ask for). The commit diff touches only description strings, one step title and prose, so no element, view or edge moved, which is what the drills mutate. Scope: `git diff --stat ff741e4..HEAD` over k8s, infra, src, scripts, sync.mjs, drill.fish, views.c4 and ROADMAP.md prints nothing; exactly the 7 planned files changed. |
| 10 | ROADMAP criterion 3: error-401 is a 401 with no SQL step, after the fix | PRESENT_BEHAVIOR_UNVERIFIED | The view still draws no SQL step, and the code (line 129) replaces the only measured statement source. But no statement-level observation exists after the fix; the test counts rows and cookies. Routed to human verification. |
| 11 | ROADMAP criterion 1: the monitoring view shows an edge to each thing the monitoring nodes scrape | UNCERTAIN | Default kubelet, kube-state-metrics, API server and operator targets are scraped (chart defaults, confirmed above) and not drawn. The text now says so; the decision to keep it undrawn is the orchestrator's assumption pending the user. WARNING, not a blocker, because the text no longer misstates the model. |

**Score:** 9/11 truths verified (1 present, behavior-unverified; 1 uncertain pending the user)

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `docs/architecture/model/scenarios-app.c4` | VERIFIED | contains `17a94cb`, parses (`check` and export pass) |
| `02-SCENARIO-EVIDENCE.md` | VERIFIED | contains `k3s-host-firewall.sh:63`, provenance block in bullets, row counts equal step counts |
| `02-04-401-measure.txt` | VERIFIED | append-only, `17a94cb` present |
| `docs/architecture/model/edge.c4` | VERIFIED | contains ICMP; issuers match the manifests |
| `docs/architecture/model/monitoring.c4` | VERIFIED | contains `kubelet` |
| `docs/DIAGRAM_CONVENTIONS.md` | VERIFIED | contains `VALIDATION_FAILED` |

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| error-401 description | measure rows A to F and 17a94cb | WIRED | named in the description, cited with file:line in the evidence block, all lines re-read |
| inbound-packet-path step 3 | `k3s-host-firewall.sh:63` | WIRED | evidence row 3 |
| certmanager.staging description | the three issuerRef lines | WIRED | matches `rg -n 'letsencrypt-' k8s/` |
| Scenarios row | the ten `## view` sections | WIRED | steps-to-rows gate |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Model parses and ties out | `node docs/architecture/sync.mjs check < /dev/null` | exit 0, tally above | PASS |
| Check self-test | `python3 scripts/pre-commit-likec4-selftest.py < /dev/null` | `16/16 passed`, exit 0 | PASS |
| Post-fix test red and green | not re-run (Gradle with Testcontainers); read the committed 261028-m6k-red.txt and green.txt | 6 of 8 red, 0 failures green | NOT RE-RUN |

### Probe Execution

None declared. SKIPPED.

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| SCEN-02 | SATISFIED for the text; criterion 3 behavior item open | truths 1, 2, 10 |
| SCEN-05 | SATISFIED | truth 6 |
| PHYS-03 | NEEDS HUMAN | truths 7 and 11 |
| PHYS-04 | SATISFIED | truth 5 |
| STD-01 | SATISFIED | truth 8 |

### Anti-Patterns Found

None blocking. No `TBD`, `FIXME` or `XXX` was introduced. The leak gates are clean.

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `docs/DIAGRAM_CONVENTIONS.md` Checks paragraph | cites `values.yaml line 1735` for `kubeApiServer` enabled | Info | At tag 91.5.2 line 1735 is the `kubeApiServer:` key and `enabled: true` is line 1736. The block is correct, the line is off by one. |
| `docs/DIAGRAM_CONVENTIONS.md` Checks paragraph | wrap leaves a short orphan line (`target list is not read. Scenario`) | Info | Cosmetic. |
| `k8s/platform/edge/clusterissuers.yaml:1` | header still says "every Certificate starts on letsencrypt-staging" | Info | Out of scope (k8s file), now contradicts the three Certificates and the corrected edge.c4. Named in the SUMMARY. |
| `docs/DIAGRAM_CONVENTIONS.md` Phase 2 size column | `error-401` 1180 x 530 predates the longer description and step title | Info | Dated record, disclosed in the SUMMARY, not re-exported. |
| the three commits | `--no-verify` skipped gitleaks and the LikeC4 hook step | Info | Allowed for docs and planning paths by the project CLAUDE.md. My leak greps and `check` stand in for them. Gitleaks was not run by anyone. |

### Human Verification Required

#### 1. Post-fix absence of SQL on an anonymous 401

**Test:** Run the app on a Postgres this repo owns with `log_statement = 'all'`, send GET and POST `/api/boards` with no cookie, with `Accept: */*` and with no Accept header. Record Set-Cookie, `spring_session` rows before and after, and the statements logged.
**Expected:** 401 UNAUTHENTICATED, no Set-Cookie, no new row, no statement.
**Why human:** it needs a live app. The test in the repo counts rows and cookie names only.

#### 2. Monitoring scope

**Test:** Answer whether the monitoring view should draw the chart default scrape targets.
**Expected:** Narrower reading confirmed (nothing more to do), or a separate change to draw them.
**Why human:** only the user can supply it.

### Gaps Summary

No gap. The six items named in the task goal are closed in text, and each reworded sentence was checked against its source: the 401 path (measured before the fix, fixed in 17a94cb, limits stated, the no-SQL claim labelled as inferred at all six sites), WR-04, WR-05, WR-06, WR-08, and the monitoring wording. The status is `human_needed` because two things rest on a person: nobody has observed the post-fix statement count, and the monitoring scope is still the orchestrator's assumption.

---

_Verified: 2026-10-28T16:45Z_
_Verifier: Claude (gsd-verifier)_
