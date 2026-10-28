---
phase: 02-model-completion
verified: 2026-10-28T17:05:00Z
status: human_needed
score: 4/5 must-haves verified
covered_files:
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-01-PLAN.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-01-SUMMARY.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-02-PLAN.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-02-SUMMARY.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-03-PLAN.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-03-SUMMARY.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-04-PLAN.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-04-SUMMARY.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-05-PLAN.md"
  - ".planning/workstreams/architecture-views/phases/02-model-completion/02-05-SUMMARY.md"
  - "docs/DIAGRAM_CONVENTIONS.md"
  - "docs/architecture/drill-output.txt"
  - "docs/architecture/drill.fish"
  - "docs/architecture/model/delivery.c4"
  - "docs/architecture/model/deployment.c4"
  - "docs/architecture/model/edge.c4"
  - "docs/architecture/model/generated.c4"
  - "docs/architecture/model/model.c4"
  - "docs/architecture/model/monitoring.c4"
  - "docs/architecture/model/scenarios-app.c4"
  - "docs/architecture/model/scenarios-platform.c4"
  - "docs/architecture/model/spec.c4"
  - "docs/architecture/model/views.c4"
  - "docs/architecture/sync.mjs"
  - "scripts/pre-commit-likec4-selftest.py"
  - "src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java"
  - "src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java"
covered_digest: "v3:sha256:9dfe9efef5448c7402c2bd100dd9ede26da6d1e8a294d8671b1ca0d1be8ca9ef"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 3/5
  gaps_closed:
    - "Criterion 5 / STD-01: standard overclaimed evidence coverage and Mermaid parity (WR-04, WR-08)"
    - "Criterion 3: inbound-packet-path step 3 and the hostfilter element omitted ICMP (WR-06)"
    - "Phase goal: staging ClusterIssuer described as the issuer every certificate starts on (WR-05)"
    - "Criterion 3: error-401 'no SQL' was unmeasured; the measured defect was fixed in 17a94cb and pinned by a test"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Decide whether the monitoring view may omit the kube-prometheus-stack chart-default scrape targets (kubelet with cAdvisor, kube-state-metrics, API server, operator)"
    expected: "Either the user accepts the narrower scope and an override is recorded in this file's frontmatter, or the four targets get edges in the monitoring view (then criterion 1 is re-verified). The orchestrator's assumption that the narrower scope is acceptable is NOT yet a user decision."
    why_human: "The criterion says 'an edge to each thing they scrape'. Only the user can accept a narrower reading (an override needs accepted_by), and the live target list cannot be read from the repo."
---

# Phase 2: Model Completion Verification Report

**Phase Goal:** Every runtime, delivery and physical fact that matters appears in the Physical, Process, Logical and Scenarios views. Each new fact is tied to a check or labelled as authored, and the LikeC4 standard describes what is drawn.
**Verified:** 2026-10-28T17:05:00Z
**Status:** human_needed
**Re-verification:** Yes, second pass after gap closure (first pass: `gaps_found`, 3/5)

## Verdict in one paragraph

All four first-pass gaps are closed and each fix is visible in the files, not just in the SUMMARYs. The one thing that still stops a clean `passed` is a scope question on criterion 1: the monitoring view states plainly that it leaves out the chart-default scrape targets, but the criterion's words are "an edge to each thing they scrape", so only the user can accept that narrower reading (or order the edges). That is pending the user's confirmation; it is not decided. Criterion 3's 401 claim is satisfied with a stated caveat: the "no session row, no cookie" part is proven by a test I re-ran green, the "no SQL statement" part is inferred and is labelled as inferred everywhere it appears.

## First-pass gaps (kept as history) and how each closed

| First-pass gap | Fix commit | Check this run | Result |
| -------------- | ---------- | -------------- | ------ |
| Gap 1, criterion 5 / STD-01: Scenarios row said every step is cited in the evidence file, but `signin-ok`, `signin-bad-credentials`, `signin-ceiling` have no section; Mermaid parity claimed for `error-status-split` and `signup` | `ee9f9be`, `3ea3414` | Read `docs/DIAGRAM_CONVENTIONS.md` rows and "Outside this change". The Scenarios row now says "the ten Phase 2 scenario views" and points the three `signin-*` views at `261027-if9-T4-SUMMARY.md` at `8b7e379` (file exists, commit exists, 8 `signin` mentions). The Mermaid text now says no view draws the 400 `VALIDATION_FAILED` section and `signup-ok` draws only the success path. I read `docs/diagrams/scenarios/signup.mmd` and `rg` the model for `VALIDATION_FAILED`, `400`, `DELETE`, `rollback`: the Mermaid has the validation, taken-email, lost-race and `DELETE user (rollback)` branches, the model draws none | CLOSED |
| Gap 2, criterion 3: `inbound-packet-path` step 3 and `hostfilter` said only TCP is admitted; `k3s-host-firewall.sh:63` is `-p icmp -j RETURN` | `4d13696` | Read `infra/vm/k3s-host-firewall.sh:61-66`. `scenarios-platform.c4:37` now says "established traffic, new TCP on an allow-listed port and ICMP return from the chain, any other new connection is dropped"; `edge.c4:31` says "only as TCP on an allow-listed port or as ICMP, and drops every other new connection"; evidence row 3 cites lines 61, 62, 63 and 64 with the ICMP quote | CLOSED |
| Gap 3, phase goal: staging issuer "every certificate starts on" | `4d13696` | `rg issuerRef -A3 k8s/overlays k8s/platform/edge`: all three Certificates name `letsencrypt-production`; `rg letsencrypt-staging k8s` hits only `clusterissuers.yaml`. `edge.c4:10` now reads "defined in the repo but referenced by no Certificate today. The prod, nonprod and monitoring Certificates all name the production issuer" | CLOSED |
| Unrun verify, criterion 3: "401 with no SQL step" unmeasured | `17a94cb` (app), `3ea3414` (text) | See "Criterion 3 and the 401 path" below | CLOSED with a stated caveat |

## Re-run evidence (this run, stdin redirected, nothing copied)

| Command | Result |
| ------- | ------ |
| `node docs/architecture/sync.mjs check < /dev/null` | `OK (56 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)`, exit 0 |
| `python3 scripts/pre-commit-likec4-selftest.py < /dev/null` | `16/16 passed`, exit 0 |
| `fish docs/architecture/drill.fish < /dev/null 2>/dev/null \| cmp - docs/architecture/drill-output.txt` | `cmp` exit 0, byte-identical |
| `likec4 export json` on `docs/architecture/model` (to a temp dir, inspected, then trashed) | 20 views; 3 `deployment` views, all tagged `physical`: `monitoring` (11 edges), `production` (17), `delivery` (8); 0 edges in any view with a `[...]` label; 13 views tagged `scenarios` with 3 to 5 nodes each (`signup-ok` 5, `error-401` 3, `error-403` 5, `error-409` 4, `activity-log-row` 5, `push-to-image` 4, `image-to-rollout` 5, `inbound-packet-path` 5, `ingress-to-pod` 3, `nightly-backup` 3, `signin-ok` 5, `signin-bad-credentials` 4, `signin-ceiling` 5); every scenario edge has a non-empty title |
| `./gradlew test --tests 'com.vrudenko.kanban_board.security.AuthorizationGatingTest$NoSessionSweep'` | Test report: `tests="30" skipped="0" failures="0" errors="0"`; the 8 `shouldCreateNoSessionAndSetNoCookie_whenNoSessionIsPresent` cases (GET and POST `/boards` x Accept `application/json`, `*/*`, browser navigation, none) all present and passing. The Gradle build exit was 1 only because `jacocoTestCoverageVerification` fails when one class runs alone (coverage 0.29 against 0.90), which is not a test failure. Docker containers started by the run were gone afterwards |
| Debt markers `TBD\|FIXME\|XXX\|TODO\|HACK\|PLACEHOLDER` over `sync.mjs`, `drill.fish`, `model/`, `DIAGRAM_CONVENTIONS.md`, the self-test | no hits |
| `rg 'excluded, as in' docs/DIAGRAM_CONVENTIONS.md` | no match (exit 1) |

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1 | Three `#physical` deployment views; delivery view (GitHub, runner, Docker Hub, Flux, image path); monitoring view with an edge to each thing it scrapes; cert-manager with both ClusterIssuers, edge routing, host filter external; view-membership check green; no `[...]` label | VERIFIED for everything except the scrape-completeness clause; that clause is PENDING THE USER'S DECISION (human item 1) | Export: `monitoring`, `production`, `delivery`, all `physical`, 0 `[...]` labels; `check` exit 0. `production` has `issuer-staging`, `issuer-production`, `certmanager`, `hostfilter` (kind `external`). `monitoring.c4` draws `prometheus -> node-exporter, postgres-exporter, certmanager, kanban.management, redpanda`; `loki.yaml:102` has `serviceMonitor.enabled: false`, so no Loki scrape edge is owed. NOT drawn: the chart-default targets named in the view description. The criterion's literal words ("an edge to each thing they scrape") would require them, so this is not a clean pass |
| 2 | `process` shows Schema Registry, `register-schemas` init job, management port, backup flow Postgres to encryption to off-cluster bucket; `data` has notes for `activity_log` and Avro subjects; each `metadata.source` is a file `check` confirms | VERIFIED | Unchanged since first pass; `sync.mjs`, `process`/`data` model files untouched by the four fix commits (`git show --stat` of `17a94cb`, `3ea3414`, `4d13696`, `ee9f9be` lists no such file); `check` exit 0 re-run this pass |
| 3 | Scenario views for signup, error 401/403/409, push-to-image, image-to-rollout, inbound-packet-path, ingress-to-pod, activity-log row, nightly backup; every step titled; at most 5 participants; evidence file cites the line behind each step | VERIFIED, with one stated caveat on the 401 no-SQL claim | All ten Phase 2 views present, `scenarios`, 3 to 5 nodes, no untitled edge. Evidence row for `inbound-packet-path` step 3 now cites lines 61 to 64 including 63, and the quoted line matches the script I read. `error-401`: see below |
| 4 | `sync.mjs` reads workloads, namespaces, hosts from the k8s YAML incl. `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`; `check` exit 0; three breaks each exit 1 and are drilled; unmodified copy exits 0 | VERIFIED | `sync.mjs` and `drill.fish` untouched by the fix commits; `check` exit 0 and the drill replay is byte-identical to `drill-output.txt` this run |
| 5 | Physical row names the deployment views as built and no longer says monitoring is excluded; Scenarios row describes scenarios as built incl. internal activity-log and delivery steps; "excluded, as in `production-host.mmd`" absent | VERIFIED | `rg 'excluded, as in'` exit 1. Physical row names `production`, `delivery`, `monitoring` and now also says the chart default scrape targets are not drawn. Scenarios row names the activity-log and delivery internal steps and is now scoped to "the ten Phase 2 scenario views". Both overclaims from the first pass are gone (see gap table) |

**Score:** 4/5 truths verified; criterion 1 verified except one clause awaiting the user's decision.

### Criterion 3 and the 401 path: my judgment

What the ROADMAP asks: a scenario view for error 401 as one outcome "with client-visible discriminators only (401 with no SQL step ...)". The view exists, is tagged `scenarios`, has 3 participants and 4 titled edges, and draws no SQL step. So the letter is met. The harder question is whether the drawn claim is true after the fix.

What is measured, what is tested, what is inferred:

- **Measured live, before the fix** (`02-04-401-measure.txt`, host commit `82775fa`, verdict b): a cookie-less `GET`/`POST /api/boards` whose Accept named `application/json` (rows A, C, F) got a 401 with no cookie, no `spring_session` row and **no statement at all**; a bare wildcard, browser navigation or no Accept header (rows B, D, E) wrote `INSERT SPRING_SESSION` and `INSERT SPRING_SESSION_ATTRIBUTES` and set `JSESSIONID`.
- **Fixed** (`17a94cb`): I read the diff. `SecurityConfiguration.java:129` is `http.requestCache(cache -> cache.requestCache(new NullRequestCache()));`, and `rg -n "requestCache|NullRequestCache" src/main/java` finds only that line and the import.
- **Tested, observed this run**: `AuthorizationGatingTest$NoSessionSweep` passes all 8 sweep cases against the fixed code (0 failures in the report I just generated). The red direction is recorded in `261028-m6k-red.txt` (6 of the 8 cases fail on the unfixed code, each with `newSessionRows=1, setCookieNames=[JSESSIONID]`, and the two JSON cases pass as the control). I did not re-run the red direction myself because that needs the app code reverted, which this pass may not do; I read the recorded output and it matches the measurement rows B, D, E.
- **Inferred, not observed**: that no SQL *statement* is sent after the fix. The test counts new `SPRING_SESSION` primary ids and `Set-Cookie` names in process (MockMvc), not statements, and no live re-measure followed the fix. The inference rests on three premises, each checked: (1) the only statements measured on the unfixed code were the session INSERTs, (2) the fix removes the cache that triggered them and the test shows no session is created for any of the 4 Accept variants, (3) the requests that never reached the saving path (A, C, F) issued zero statements live, so a refused anonymous request that creates no session follows that zero-statement path.

Does that satisfy criterion 3 as worded? Yes. The criterion requires a view with no SQL step and an evidence file that cites the line behind each step; both hold, and the claim behind the view is now backed by a green behavioral test of its observable consequence plus a live baseline, not by presence checks alone. I do not round the statement-level claim up to measured: `error-401`'s description, `DIAGRAM_CONVENTIONS.md` note (b2), the table row and `02-SCENARIO-EVIDENCE.md` all say "inferred from the code, not observed", and I checked that the wording is identical in substance in all four places. That labelling is what the phase goal ("tied to a check or labelled as authored") asks for.

**Caveat, stated:** the statement-level absence after the fix is an inference. It is not a human gate because the artifact says so itself and the criterion does not require a statement count. If you want it observed, the settling command is the same method as `02-04-401-measure.txt`: run this repo's own compose Postgres on a free host port (the earlier run used project name `kbmeasure` and port `15433`, with a temp override file outside the repo), `./gradlew bootRun` with `DB_PORT=15433`, `ALTER SYSTEM SET log_statement='all'` plus `pg_reload_conf()`, then `curl -s -o /dev/null -D - http://localhost:8080/api/boards` with no Cookie header once with `-H 'Accept: application/json'`, once with `-H 'Accept: */*'` and once with `-H 'Accept:'`; expect 401, no `Set-Cookie`, `select count(*) from spring_session` unchanged, and no statement in `docker logs`. Do not stop the user's `datastore-postgres` on 5433. If any request still writes a row, `error-401` and criterion 3 are contradicted for that client.

### Criterion 1 and the monitoring scope: my judgment

The criterion says the monitoring view shows "an edge to each thing they scrape". The view now says, in its own description and in `DIAGRAM_CONVENTIONS.md` (Physical row, Checks paragraph, `monitoring` row), exactly what it leaves out: the chart-default scrape targets, "among them the kubelet with cAdvisor, kube-state-metrics, the API server and the operator". I read `k8s/monitoring/controllers/kube-prometheus-stack.yaml`: lines 45 to 52 turn off only `kubeControllerManager`, `kubeScheduler`, `kubeEtcd`, `kubeProxy` (the "lines 45 to 52" citation is exact). `k8s/monitoring/configs/dashboards/cadvisor.json` exists in the repo, so cAdvisor metrics are at least consumed here.

Is the criterion met by "repo-evidenced targets plus an honest statement"? By the literal words, no: a thing Prometheus scrapes is not drawn. The honest statement turns an inaccuracy into a labelled scope reduction, and that satisfies the phase goal's "labelled as authored". It does not turn the ROADMAP wording into something else, and a scope reduction of a ROADMAP criterion is what the override mechanism exists for, which needs a person's `accepted_by`. So I do not mark it VERIFIED and I do not mark it FAILED: it is pending the user's confirmation (human item 1). The orchestrator's assumption that the narrower reading is fine stays an assumption until then.

What the text says versus what is known (you asked me to check the text says exactly that and no more):

- The model description and the standard claim only: these four are chart-default targets that the values file leaves on; "only the API server was confirmed enabled in the chart values at the pinned version"; the rest are the chart's documented defaults, not read from this repo; the live target list is not read. That is a deliberate understatement, not an overstatement. Pinned version `91.5.2` matches `kube-prometheus-stack.yaml:25`.
- I did not re-read the upstream chart `values.yaml` myself (no network read this run). The quick task's own verifier (`261028-n5v-VERIFICATION.md` row 7) reports a fuller fetch at tag `kube-prometheus-stack-91.5.2` finding `kubelet.enabled`, `cAdvisor`, `kubeApiServer`, `kubeStateMetrics` and `prometheusOperator` all `true`, and that the cited line 1735 is the `kubeApiServer:` key with `enabled: true` at 1736 (off by one). Treat that as reported, not confirmed by me. If correct, the four omitted targets are real, which makes a "no" from the user a concrete four-edge task rather than an open-ended one.
- Minor wording defect (Info): `DIAGRAM_CONVENTIONS.md` Checks paragraph says "turns off only the controller-manager, scheduler, etcd and kube-proxy monitors (lines 45 to 52). Of those four defaults only the API server was read ...". "Those four" reads as the four turned off, but means the four not drawn (kubelet with cAdvisor, kube-state-metrics, API server, operator). `monitoring.c4` says "Of the four named", which is unambiguous. Not a gap against any criterion or plan truth.

### Code review findings weighed against the contract

| Finding | Contradicts a contract item? | Disposition now |
| ------- | ---------------------------- | --------------- |
| WR-04, WR-08 | Was yes | Fixed in `ee9f9be`, rechecked above |
| WR-06 | Was yes | Fixed in `4d13696`, rechecked above |
| WR-05 | Was yes | Fixed in `4d13696`, rechecked above |
| WR-01 (metadata-key allowlist not per surface) | No ROADMAP criterion or plan truth is contradicted | Warning, deliberately not fixed |
| WR-02 (`drill.fish` has no machine oracle) | No: the recorded output is byte-identical and shows the expected exits this run | Warning, deliberately not fixed |
| WR-07 (`routedHosts` reads only the first `Host(` argument) | No: criterion 4 holds on the real tree | Warning, latent vacuous pass |
| WR-09 (public Grafana path not drawn) and IN-01 to IN-05 | Not in any criterion or must_have | Notes |

None of WR-01, WR-02, WR-07, WR-09 contradicts a ROADMAP criterion or a plan must_have, so none is called a gap.

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `docs/architecture/sync.mjs` | edge-label, participant, workload, host and HelmRelease checks | VERIFIED | Unchanged since first pass; `check` exit 0 |
| `docs/architecture/model/{delivery,edge,monitoring,scenarios-app,scenarios-platform}.c4` | new elements, views, scenarios | VERIFIED | Parse and export; text corrected as in the gap table |
| `docs/architecture/drill.fish`, `drill-output.txt` | drills D17 to D24, replay identical | VERIFIED | `cmp` exit 0 |
| `02-SCENARIO-EVIDENCE.md` | section per Phase 2 view with file:line and quote | VERIFIED | Ten Phase 2 views; `inbound-packet-path` row 3 cites line 63; `error-401` section lists measured, fixed, pinned, not done, inferred |
| `02-04-401-measure.txt` | measurement or reason | VERIFIED | Superseded "not measured" kept as history; live measurement with verdict b; dated fixed-in-`17a94cb` note that says no re-measure was run |
| `docs/DIAGRAM_CONVENTIONS.md` | Physical and Scenarios rows amended to what is built | VERIFIED | Both rows and the "Outside this change" list match what is drawn |
| `SecurityConfiguration.java`, `AuthorizationGatingTest.java` | `NullRequestCache` on the main chain; sweep test | VERIFIED | Line 129; test lines 354, 382, 412; green this run |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `checkEdgeLabels` | LikeC4 computed deployment views | `view.edges()` label | WIRED | Untouched; `check` exit 0 |
| `drill.fish fresh()` | scratch model copy | `MODEL` override | WIRED | Replay identical |
| `sync.mjs` workload sets | `k8s/monitoring`, `k8s/platform`, `gotk-components.yaml` | `WORKLOAD_DIRS` | WIRED | Drills D20, D22, D23 recorded exit 1 |
| `sync.mjs checkHosts` | `IngressRoute` matches | `routedHosts` | WIRED, with the WR-07 limit | D24 recorded exit 1 |
| `securityFilterChain` | refused-request path | `http.requestCache(... NullRequestCache)` | WIRED | `ExceptionTranslationFilter` receives the configured cache; sweep test green |

### Data-Flow Trace (Level 4)

Not applicable to the model and the checker. For the app change: the request cache is configured on the only chain that returns the 401 for the protected routes, and the test drives the real filter chain against a Testcontainers Postgres built by the Flyway migrations, so `SPRING_SESSION` is the real table.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Model matches code | `node docs/architecture/sync.mjs check` | exit 0, 17 workloads | PASS |
| Hook self-test | `python3 scripts/pre-commit-likec4-selftest.py` | 16/16 | PASS |
| Drill replay | `drill.fish \| cmp` | identical | PASS |
| Export checks | `likec4 export json` | 3 physical, 0 `[...]`, 13 scenarios | PASS |
| Anonymous refusal leaves no session row and no cookie | the one named `NoSessionSweep` test class | 0 failures, 8 sweep cases present | PASS |
| Anonymous refusal sends no SQL statement after the fix | live curl with `log_statement=all` | not run | inferred, labelled as such; command above |

### Probe Execution

SKIPPED: the plans declare no `probe-*.sh` scripts.

### Requirements Coverage

All 14 IDs appear in a PLAN `requirements:` field and in REQUIREMENTS.md (lines 13 to 44 and the table at 82 to 99); none is orphaned. SCEN-08 maps to Phase 3 and is correctly absent.

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| PHYS-02 | 02-01, 02-03 | Delivery view | SATISFIED | `delivery` view, 8 edges |
| PHYS-03 | 02-01, 02-02 | Monitoring nodes and what they scrape | PENDING USER DECISION | Repo-evidenced targets drawn, chart-default targets stated as not drawn (human item 1) |
| PHYS-04 | 02-01, 02-02, 02-03 | cert-manager, issuers, edge routing, host filter external | SATISFIED | `production` nodes and edges; staging description now true |
| PROC-01 | 02-02 | Schema Registry, init job, management port, backup flow | SATISFIED | `process` view |
| LOGI-01 | 02-02 | `activity_log` and Avro notes tied to sources | SATISFIED | `data` notes with `source` |
| SCEN-01 | 02-04 | Signup | SATISFIED | `signup-ok` |
| SCEN-02 | 02-04 | Error status: 401 no SQL, 403, 409 | SATISFIED, with the stated caveat on statement-level absence | `error-401/403/409`; sweep test; measurement file |
| SCEN-03 | 02-05 | Push-to-image | SATISFIED | `push-to-image` |
| SCEN-04 | 02-05 | Image-to-rollout | SATISFIED | `image-to-rollout` |
| SCEN-05 | 02-05 | Inbound packet path, ingress-to-pod | SATISFIED | step 3 now includes ICMP, line 63 cited |
| SCEN-06 | 02-04 | Mutation to `activity_log` row | SATISFIED | `activity-log-row` |
| SCEN-07 | 02-05 | Nightly backup | SATISFIED | `nightly-backup` |
| GATE-01 | 02-02, 02-03 | `sync.mjs` reads k8s facts and fails on drift | SATISFIED | D20 to D24 |
| STD-01 | 02-05 | Physical and Scenarios rows match what is built | SATISFIED | overclaims removed |

REQUIREMENTS.md marks all 14 `[x]`; PHYS-03 stays marked complete while its scope question is open.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `docs/DIAGRAM_CONVENTIONS.md` | Checks paragraph, "Of those four defaults" | ambiguous referent (reads as the four disabled monitors) | Info | Reader could misread which four; `monitoring.c4` is unambiguous |
| `docs/DIAGRAM_CONVENTIONS.md` | Checks paragraph, "`values.yaml` line 1735" | per the quick-task verifier, 1735 is the `kubeApiServer:` key, `enabled` is 1736 | Info | Off by one in a citation |
| `k8s/platform/edge/clusterissuers.yaml` | 1 | header comment still says every Certificate starts on staging; all three use production | Info (outside this phase's files) | The model no longer copies it, but the manifest comment is stale and is the source of the original WR-05 error |
| `docs/architecture/sync.mjs` | 255 | first `Host(` argument only (WR-07) | Warning | Latent vacuous pass; criterion 4 holds on the real tree |
| `docs/architecture/drill.fish` | 36-40 | no oracle for expected exit (WR-02) | Warning | Future drift in drill targets reads as "not caught" |

Debt-marker scan: no hits. No new-scope blocker was found, so the `advisory:` list is empty.

### Advisory (New Scope, Unevidenced)

None.

### Human Verification Required

#### 1. Is "an edge to each thing they scrape" satisfied by repo-evidenced targets plus the stated omission?

**Test:** Choose one.
(a) Accept the narrower scope. Add this to the frontmatter of this file (or tell me to) and re-verify, which makes criterion 1 PASSED (override) and the status `passed`:

```yaml
overrides:
  - must_have: "A monitoring view shows the monitoring nodes, with an edge to each thing they scrape"
    reason: "The view draws the scrape targets the repo's monitor settings name and states in its description and in DIAGRAM_CONVENTIONS.md that the kube-prometheus-stack chart-default targets are not drawn"
    accepted_by: "<your name>"
    accepted_at: "<ISO timestamp>"
```

(b) Require the edges. Add prometheus edges to the kubelet with cAdvisor, kube-state-metrics, the API server and the operator in `monitoring.c4` (each needs an authored-or-checked label), then re-verify. To settle first whether all four are really enabled, either read the chart defaults at the pinned version (`helm show values prometheus-community/kube-prometheus-stack --version 91.5.2`) or, with cluster access, `kubectl -n monitoring get servicemonitors`.

**Expected:** (a) an override entry with a real `accepted_by`, or (b) four more edges and `check` still exit 0.
**Why human:** only the user can accept a narrower reading of a ROADMAP criterion, and the live target list is not in the repo. If the answer is (b), criterion 1 becomes a gap with a concrete fix (four edges) and the phase goes to `gaps_found` until they exist.

## Gaps Summary

No open gaps against a ROADMAP criterion or a plan must_have. All four first-pass items are closed and rechecked against the files. One decision remains with the user (criterion 1, monitoring scope), and one inference is carried openly (no SQL statement after the 401 fix, labelled in four places, with the live command above if you want it observed). The orchestrator's working assumption on monitoring scope is recorded here as an assumption, not as a decision.

---

_Verified: 2026-10-28T17:05:00Z_
_Verifier: Claude (gsd-verifier)_
