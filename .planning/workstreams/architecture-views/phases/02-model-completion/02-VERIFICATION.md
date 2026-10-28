---
phase: 02-model-completion
verified: 2026-10-28T17:53:13Z
status: passed
score: 5/5 must-haves verified
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
covered_digest: "v3:sha256:7d91df776530f4128b317a268d5ddc01c83a2f7e286d70e4f29e04b7bfe6bd2c"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 4/5
  gaps_closed:
    - "Criterion 1 / PHYS-03: monitoring view omitted the chart-default scrape targets (the user chose to draw them; six edges now exist and each rests on a values line I re-read)"
    - "Criterion 3: 'no SQL statement after the 401 fix' was an inference; it is now an observation at 7bccf71, with its capture-window limits named"
  gaps_remaining: []
  regressions: []
---

# Phase 2: Model Completion Verification Report

**Phase Goal:** Every runtime, delivery and physical fact that matters appears in the Physical, Process, Logical and Scenarios views. Each new fact is tied to a check or labelled as authored, and the LikeC4 standard describes what is drawn.
**Verified:** 2026-10-28T17:53:13Z
**Status:** passed
**Re-verification:** Yes, third pass (pass 1 `gaps_found` 3/5, pass 2 `human_needed` 4/5, pass 3 `passed` 5/5)

## Verdict in one paragraph

`passed`, with two stated caveats that do not contradict any criterion. Criterion 1 now draws an edge to every scrape target that the repo's monitors and the pinned chart's values say Prometheus has; I re-read the chart values file myself and the lines behind each new edge say what the edge titles say. What remains undrawn is the Prometheus self-scrape (LikeC4 refuses a self-link) and monitors the repo turns off (not scraped, so nothing is owed), and the live target list is not read. Criterion 3's "401 with no SQL step" is drawn, pinned by a test I re-ran green, and observed live at 7bccf71 by a recorded run whose own capture-sanity check I read; I did not reproduce that live run, and its limits are listed below. No human item is needed to close the phase; two optional, non-gating live checks are given with their commands.

## History kept visible

### Pass 1 gaps (`gaps_found`, 3/5), all closed in pass 2 and still closed

| First-pass gap | Fix commit | Still closed this run |
| -------------- | ---------- | --------------------- |
| Criterion 5 / STD-01: Scenarios row claimed every step cited in the evidence file and Mermaid parity for `error-status-split` and `signup` (WR-04, WR-08) | `ee9f9be`, `3ea3414` | Yes. Row 113 of `docs/DIAGRAM_CONVENTIONS.md` says "the ten Phase 2 scenario views" and points the three `signin-*` views at `261027-if9-T4-SUMMARY.md` at 8b7e379 |
| Criterion 3: `inbound-packet-path` step 3 and `hostfilter` said only TCP is admitted (WR-06, ICMP) | `4d13696` | Yes. Untouched since; `git diff 7bccf71 HEAD --stat` lists no `edge.c4` or `scenarios-platform.c4` change |
| Phase goal: staging ClusterIssuer described as the issuer every certificate starts on (WR-05) | `4d13696` | Yes. Untouched since |
| Criterion 3: "401 with no SQL step" unmeasured | `17a94cb`, `3ea3414` | Yes, and strengthened below |

### Pass 2 (`human_needed`, 4/5): what it left open and how it closed

| Pass-2 open item | Closed by | This run |
| ---------------- | --------- | -------- |
| Human item 1: may the monitoring view omit kubelet, kube-state-metrics, API server, operator? The user answered "draw them" (addendum in the pass-2 report) | `1d6c40e`, `6b6385d`, `e7a3f91` | Drawn. See "Criterion 1" below. No override was written, none is needed |
| "No SQL statement after the fix" inferred, not observed | `2fbe8ff`, `6b6385d` | Observed at 7bccf71 in the recorded run. See "Criterion 3" below |
| Info: "Of those four defaults" ambiguous referent; "line 1735" off by one in a citation | `1d6c40e` | Both phrases are gone from `docs/DIAGRAM_CONVENTIONS.md` (`rg` for "those four" and "1735" finds nothing). The new text cites no values line numbers in the document; the line-by-line record is in `261028-opl-chart-values.txt` |

## Re-run evidence (this run, stdin redirected, nothing copied)

| Command | Result |
| ------- | ------ |
| `node docs/architecture/sync.mjs check < /dev/null` | `OK (61 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)`, exit 0 |
| `python3 scripts/pre-commit-likec4-selftest.py < /dev/null` | `16/16 passed`, exit 0 (exit read separately from the output) |
| `fish docs/architecture/drill.fish < /dev/null 2>/dev/null \| cmp - docs/architecture/drill-output.txt` | `cmp` exit 0, byte-identical. `git diff 7bccf71 HEAD -- docs/architecture/drill-output.txt` changes only the element count (56 to 61, 57 to 62); no drill changed its exit code |
| `likec4 export json` (temp dir under `/tmp`, trashed after) | 20 views. 3 `deployment` views, all tagged `physical`: `monitoring` 17 edges, `production` 17, `delivery` 8. 0 edge labels read `[...]`. 13 views tagged `scenarios`, 3 to 5 nodes each (`signup-ok` 5, `error-401` 3, `error-403` 5, `error-409` 4, `activity-log-row` 5, `push-to-image` 4, `image-to-rollout` 5, `inbound-packet-path` 5, `ingress-to-pod` 3, `nightly-backup` 3, `signin-ok` 5, `signin-bad-credentials` 4, `signin-ceiling` 5); every scenario edge has a non-empty label |
| `./gradlew test --tests 'com.vrudenko.kanban_board.security.AuthorizationGatingTest$NoSessionSweep' < /dev/null` | Report `tests="30" skipped="0" failures="0" errors="0"`. The build exit was 1 only because `jacocoTestCoverageVerification` fails when one class runs alone (coverage 0.29 against 0.90), not a test failure. No Testcontainers or Ryuk container was left behind. `git diff --stat 7bccf71 HEAD` lists no file under `src/`, so this is the same code the live run used |
| `rg 'excluded, as in' docs/DIAGRAM_CONVENTIONS.md` | no match |
| Debt markers `TBD\|FIXME\|XXX\|TODO\|HACK\|PLACEHOLDER` over `sync.mjs`, `drill.fish`, `model/`, `DIAGRAM_CONVENTIONS.md`, the self-test | no hits |

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1 | Three `#physical` deployment views; delivery view; monitoring view with an edge to each thing it scrapes; cert-manager with both ClusterIssuers, edge routing, host filter external; view-membership check green; no `[...]` label | VERIFIED, with stated scope limits (Info) | Export: `monitoring`, `production`, `delivery`, all `physical`, 0 `[...]` labels, `check` exit 0. `production` nodes include `issuer-staging`, `issuer-production`, `certmanager`, `hostfilter`, `traefik`, `svclb`. `delivery` nodes include `github`, `runner`, `ci`, `docker-hub`, `dockerhub`, `flux`, `repo`. `monitoring` edges listed below |
| 2 | `process` shows Schema Registry, `register-schemas` init job, management port, backup flow Postgres to encryption to off-cluster bucket; `data` has notes for `activity_log` and Avro subjects; each `metadata.source` is a file `check` confirms | VERIFIED | `process` nodes include `registry`, `init: register-schemas`, `management`, `backup`, `b2: Off-cluster backup bucket`. `data` has `activity_log_note` (source `V3__add_activity_log.sql`) and `avro_subjects` (source `src/main/avro`); `check` exit 0 confirms sources exist |
| 3 | Scenario views for signup, error 401/403/409, push-to-image, image-to-rollout, inbound-packet-path, ingress-to-pod, activity-log row, nightly backup; every step titled; at most 5 participants; evidence file cites the line behind each step | VERIFIED, with caveat on the extent of the 401 observation | All ten Phase 2 views present, tagged `scenarios`, 3 to 5 nodes, no untitled edge. Evidence file line citations into `02-04-401-measure.txt` (heading 161, rows 191-199, burst 207, signin 212, result 223, limits 230-243) match the file |
| 4 | `sync.mjs` reads workloads, namespaces, hosts incl. `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`; `check` exit 0; three breaks each exit 1 and are drilled; unmodified copy exits 0 | VERIFIED | `check` exit 0; drill replay byte-identical. Recorded in `drill-output.txt`: D20 HelmRelease rename exit 1, D22 Flux controller rename exit 1, D24 changed host exit 1, D21 changed namespace exit 1, D23 ClusterIssuer rename exit 1; baseline exit 0 |
| 5 | Physical row names the deployment views as built and no longer says monitoring is excluded; Scenarios row describes scenarios as built incl. internal activity-log and delivery steps; "excluded, as in `production-host.mmd`" absent | VERIFIED | Row 112 names `production`, `delivery`, `monitoring` and lists the monitoring targets as now drawn, with the self-scrape stated as not drawn. Row 113 names the activity-log and delivery internal steps. `rg 'excluded, as in'` finds nothing |

**Score:** 5/5 truths verified. `behavior_unverified`: 0.

### Criterion 1: does the monitoring view now satisfy "an edge to each thing they scrape"?

What I checked rather than took from the record:

- **Edges in the export** (`monitoring`, 17 edges). Prometheus to: Grafana, node-exporter, postgres-exporter, kube-state-metrics, the Prometheus Operator, cert-manager, `kanban` (prod and nonprod), Redpanda (prod and nonprod), the kubelet (with cAdvisor), the API server, CoreDNS. Query and push edges: Grafana to Prometheus and Loki, Alloy to Loki, postgres-exporter to Postgres. Compared with 7bccf71, exactly six scrape edges and six nodes (`kube-state-metrics`, `prometheus-operator`, `kubelet`, `kube-apiserver`, `kube-system`, `coredns`) were added; nothing was removed.
- **Repo-declared monitors** (read `k8s/`): the `redpanda` and `app` PodMonitors (`k8s/monitoring/configs/podmonitors.yaml`), the cert-manager `servicemonitor` (`cert-manager.yaml:46`), postgres-exporter `serviceMonitor` (`postgres-exporter.yaml:62`), node-exporter via the chart, Loki `serviceMonitor` off (`loki.yaml:102`). Each drawn pre-existing edge has its monitor; the one disabled monitor has no edge. There is no `additionalScrapeConfigs` or `scrape_configs` anywhere under `k8s/`, so the `prometheus.io/scrape` annotations on the Flux controllers in `gotk-components.yaml` are not selected by anything and no Flux scrape edge is owed. (Prometheus Operator does not honour those annotations without such a config; that is my knowledge of the operator, not something I read in this repo.)
- **Chart values, read by me** (`gh api` of `charts/kube-prometheus-stack/values.yaml` at tag `kube-prometheus-stack-91.5.2`; 218683 bytes, sha256 `7d089fcf...5081`, equal to the figures in `261028-opl-chart-values.txt`; pinned version equals `kube-prometheus-stack.yaml:25`). I read these lines: `kubernetesServiceMonitors.enabled: true` (1730-1731); `kubeApiServer.enabled: true` (1735-1736) and `serviceMonitor.enabled: true` (1748-1749); `kubelet.enabled: true` (1838-1839), `serviceMonitor.enabled` (1846-1847), `https: true` (1900), `probes: true` (1929), `resource: false` (1934), `cAdvisor: true` (1944); `coreDns.enabled: true` with `port: 9153` (2229-2233) and `serviceMonitor.enabled: true` (2242-2243); `kubeStateMetrics.enabled: true` (2835-2836) and `kube-state-metrics.prometheus.monitor.enabled: true` (2849-2853); `prometheusOperator.enabled: true` (2981-2982), `tls.enabled: true` (2997-2998) and `serviceMonitor.selfMonitor: true` (3525-3528); Grafana `serviceMonitor.enabled: true` (1696-1700, under the `grafana:` key at 1394); Prometheus `serviceMonitor.selfMonitor: true` (4341-4344, under `prometheus:` at 3783). Every cited line says what the record says. The repo's own `kube-prometheus-stack.yaml` lines 37-38 and 45-52 turn off Alertmanager, controller-manager, scheduler, etcd and kube-proxy, as the view and the standard state.
- **Edge titles name a protocol or port only where a values line states it.** "over HTTPS" rests on 1900, "port 9153" on 2233, "probe metrics" on 1929. The API-server, operator, kube-state-metrics and Grafana titles name none, although 2998 shows operator TLS is on; that is an understatement, not an overclaim.
- **Authored elements are labelled.** `kubelet`, `kube-apiserver` and `coredns` descriptions open with "Authored" and say no manifest declares them. Their `source` is `infra/vm/k3s/config.yaml`, which exists (`check` exit 0), and whose `disable:` list names only `metrics-server`, which is what keeps CoreDNS on. The standard (row 112, Checks paragraph at lines 244-261) lists them as "not placement-checked" and says a chart bump can leave the drawn set stale without failing.

My judgment on the remainder. The disabled monitors are not scraped, so omitting them is accurate, not a gap. Prometheus scraping itself is the one real literal gap: "an edge to each thing they scrape" includes it. It is stated in the view description, in the `prometheus` element description and in `DIAGRAM_CONVENTIONS.md`, together with the reason (LikeC4 1.60 rejects a self-relationship; the record says it was measured). A self-edge conveys nothing the sentence does not, and the phase goal accepts "labelled" facts. I treat it as an honest, stated scope (Info), not a gap, and not a reason for an override. The Alloy chart defaults and the live target list are also unread and stated as such. If the user reads the criterion as requiring even the self-scrape, the fix is one extra authored element (for example "Prometheus (self-scrape)") with an edge; I do not think the criterion's intent needs it.

### Criterion 3: the 401 path

- **View.** `error-401` has 3 participants and 4 titled edges, no SQL step. `SecurityConfiguration.java:129` is still `http.requestCache(cache -> cache.requestCache(new NullRequestCache()));`, the only `NullRequestCache` use under `src/main/java`.
- **Test, re-run by me.** `NoSessionSweep`: 30 tests, 0 failures, including the GET and POST `/boards` sweep over four Accept variants. The red direction (6 of 8 failing on the unfixed code) is read from `261028-m6k-red.txt`, not re-run.
- **Live observation, read not reproduced.** `02-04-401-measure.txt`, section "Post-fix re-measure (HEAD 7bccf71)": 9 request combinations (GET and POST `/api/boards` against `application/json`, `*/*`, a browser navigation header, the axios default, and no Accept header) plus a 10-request burst, each 401, no `Set-Cookie`, `spring_session` 0 to 0, no logged statement; a real signin created one row with a principal. The record includes a capture-sanity check (18 harness-tagged lines logged for the first 9 requests), which makes "empty statement list" a real "nothing logged" and not a dead capture. I did not start the app and database myself (that needs a server and a state-changing run, outside what this pass may do), so this is a recorded observation by an earlier executor that I judged for internal consistency, not one I reproduced.
- **Limits, as the record names them.** One second of Postgres log per request plus one log-tail read; the whole log was not re-read; only `GET`/`POST /api/boards`; one machine, no proxy; no Redpanda; plain HTTP; cookie-less case only (no stale cookie).

Decision: **passed, with this caveat, not `human_needed`.** The criterion needs a view with no SQL step and a cited step list; both hold. The claim behind the view is now backed by three independent kinds of evidence: a pre-fix live measurement that reproduced the defect (rows B, D, E), a test I re-ran green whose red direction is recorded, and a post-fix live measurement. Every artifact that states the claim (view description, standard row 113 and note (b2), table row, evidence file) says "observed at 7bccf71" with the capture-window limits, and none says more. A residual risk remains outside what the criterion asks: a stale-cookie request, other methods or other routes, or a proxy-added header could still touch the database, and nothing in the artifacts claims otherwise.

Optional, non-gating independent reproduction, if the user wants one: use this repo's compose Postgres under a private project name and a free host port (the record used project `kbmeasure2`, port `15433`, `SERVER_PORT=18080`, `MANAGEMENT_SERVER_PORT=19081`), set `log_statement=all`, run `./gradlew bootRun`, then `curl -s -o /dev/null -D - -H 'Accept: */*' http://localhost:18080/api/boards` with no Cookie header. Expect 401, no `Set-Cookie`, `select count(*) from spring_session` unchanged, and no statement in the Postgres log. Do not stop `datastore-postgres` on 5433. Any request that still writes a row would contradict `error-401` for that client.

### Four PNG sizes changed: does it matter?

No criterion mentions a PNG size, and the standard records the sizes "without a verdict against any bound". I compared the `likec4 export json` output at 7bccf71 with the working tree: `production` (24 nodes, 17 edges, bounds 3405 x 2510), `inbound-packet-path` (5 nodes, 5 edges, 3218 x 369), `delivery` and `data` have identical node sets, edge sets and bounds at both revisions; `process` has identical node and edge sets and a width that differs by 1 px (3224 vs 3225). So the model content of the three views the quick task did not edit did not change, and the recorded sizes (`monitoring` 6725 x 1276 is the export bounds 6669 x 1220 plus 56 each way, as is `production` 3461 x 2566) match the current export. Why the earlier-recorded sizes differed, I did not find; I did not compare PNG bytes. The one item worth noting for Phase 4's legibility rule: `monitoring` is now about 6.7k px wide against 1.2k high (5.5 : 1), which the standard records without a verdict.

### Code review findings weighed against the contract

| Finding | Contradicts a ROADMAP criterion or plan must_have? | Disposition |
| ------- | -------------------------------------------------- | ----------- |
| WR-04, WR-05, WR-06, WR-08 | Were yes | Fixed, rechecked above |
| WR-01 (metadata-key allowlist not per surface) | No | Warning, not fixed |
| WR-02 (`drill.fish` has no machine oracle) | No: replay byte-identical, expected exits present in the recorded output | Warning, not fixed |
| WR-07 (`routedHosts` reads only the first `Host(` argument) | No: criterion 4 holds on the real tree (D24 caught) | Warning, latent |
| WR-09 (public Grafana path not drawn) and IN-01 to IN-05 | No | Notes |

None of these is called a gap.

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `docs/architecture/sync.mjs` | edge-label, participant, workload, host and HelmRelease checks | VERIFIED | `check` exit 0; drills D17 to D24 record exit 1 |
| `docs/architecture/model/monitoring.c4` | monitoring nodes and scrape edges, six added | VERIFIED | Parses, exports 17 edges |
| `docs/architecture/model/{delivery,edge,scenarios-app,scenarios-platform}.c4` | delivery, issuers, scenarios | VERIFIED | Export checks above |
| `docs/architecture/drill.fish`, `drill-output.txt` | drills D17 to D24, replay identical | VERIFIED | `cmp` exit 0 |
| `02-SCENARIO-EVIDENCE.md` | a section per Phase 2 view with file:line and quote | VERIFIED | Line citations into the measure file match |
| `02-04-401-measure.txt` | measurement or reason | VERIFIED | Pre-fix verdict b kept, superseded-in-part note, post-fix section with rows A to I |
| `261028-opl-chart-values.txt` | row-by-row chart-value record | VERIFIED | Spot-read 17 of its cited line ranges against my own fetch; all match |
| `docs/DIAGRAM_CONVENTIONS.md` | Physical and Scenarios rows amended to what is built | VERIFIED | Rows 112 and 113, Checks paragraph, size table |
| `SecurityConfiguration.java`, `AuthorizationGatingTest.java` | `NullRequestCache`; sweep test | VERIFIED | Line 129; `NoSessionSweep` green |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `checkEdgeLabels` | LikeC4 computed deployment views | `view.edges()` label | WIRED | `check` exit 0; 0 `[...]` labels in the export |
| `drill.fish fresh()` | scratch model copy | `MODEL` override | WIRED | Replay identical |
| `sync.mjs` workload sets | `k8s/monitoring`, `k8s/platform`, `gotk-components.yaml` | `WORKLOAD_DIRS` | WIRED | D20, D22, D23 exit 1 |
| `sync.mjs checkHosts` | `IngressRoute` matches | `routedHosts` | WIRED, WR-07 limit | D24 exit 1 |
| `securityFilterChain` | refused-request path | `http.requestCache(... NullRequestCache)` | WIRED | Sweep test green |
| `monitoring.c4` new instances | `netcup.k3s`, `netcup.k3s.monitoring`, `netcup.k3s.kube-system` | `extend ... instanceOf` | WIRED | Appear as nodes in the `monitoring` export; `production` unchanged |

### Data-Flow Trace (Level 4)

Not applicable to the model and the checker. For the app change: the request cache is configured on the chain that returns the 401 for protected routes; the sweep test drives the real filter chain against a Testcontainers Postgres built by the Flyway migrations.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Model matches code | `node docs/architecture/sync.mjs check` | exit 0, 61 elements, 17 workloads | PASS |
| Hook self-test | `python3 scripts/pre-commit-likec4-selftest.py` | 16/16 | PASS |
| Drill replay | `drill.fish \| cmp` | identical | PASS |
| Export checks | `likec4 export json` | 3 physical, 0 `[...]`, 13 scenarios | PASS |
| Anonymous refusal leaves no session row and no cookie | `NoSessionSweep` class | 30 tests, 0 failures | PASS |
| Anonymous refusal sends no SQL statement | live curl with `log_statement=all` | recorded run read, not reproduced | PASS (recorded), optional reproduction above |

### Probe Execution

SKIPPED: the plans declare no `probe-*.sh` scripts.

### Requirements Coverage

All 14 IDs appear in a PLAN `requirements:` field (02-01: PHYS-02, 03, 04; 02-02: PHYS-03, 04, PROC-01, LOGI-01, GATE-01; 02-03: PHYS-02, 04, GATE-01; 02-04: SCEN-01, 02, 06; 02-05: SCEN-03, 04, 05, 07, STD-01) and in `REQUIREMENTS.md` (lines 13 to 44 and the table at 82 to 99). The table holds exactly 14 `Phase 2` rows, so none is orphaned. SCEN-08 maps to Phase 3 and is correctly absent.

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| PHYS-02 | 02-01, 02-03 | Delivery view | SATISFIED | `delivery`, 8 edges, 14 nodes |
| PHYS-03 | 02-01, 02-02 | Monitoring nodes and what they scrape | SATISFIED, stated limits | 17 edges; six chart-default edges re-read against the values file; self-scrape stated, not drawn |
| PHYS-04 | 02-01, 02-02, 02-03 | cert-manager, issuers, edge routing, host filter external | SATISFIED | `production` nodes and edges |
| PROC-01 | 02-02 | Schema Registry, init job, management port, backup flow | SATISFIED | `process` nodes |
| LOGI-01 | 02-02 | `activity_log` and Avro notes tied to sources | SATISFIED | `data` notes with `source` |
| SCEN-01 | 02-04 | Signup | SATISFIED | `signup-ok` |
| SCEN-02 | 02-04 | Error status: 401 no SQL, 403, 409 | SATISFIED, caveat above | `error-401/403/409`; sweep test; post-fix record |
| SCEN-03 | 02-05 | Push-to-image | SATISFIED | `push-to-image` |
| SCEN-04 | 02-05 | Image-to-rollout | SATISFIED | `image-to-rollout` |
| SCEN-05 | 02-05 | Inbound packet path, ingress-to-pod | SATISFIED | both views present; ICMP in step 3 |
| SCEN-06 | 02-04 | Mutation to `activity_log` row | SATISFIED | `activity-log-row` |
| SCEN-07 | 02-05 | Nightly backup | SATISFIED | `nightly-backup` |
| GATE-01 | 02-02, 02-03 | `sync.mjs` reads k8s facts and fails on drift | SATISFIED | D20 to D24 |
| STD-01 | 02-05 | Physical and Scenarios rows match what is built | SATISFIED | rows 112, 113 reflect the six new edges and the 401 observation |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `k8s/platform/edge/clusterissuers.yaml` | 1-2 | header comment still says every Certificate starts on `letsencrypt-staging`; all three Certificates name production (checked in pass 2) | Info (outside this phase's files) | The model no longer copies it; the comment is stale and is the source of the original WR-05 error |
| `docs/architecture/sync.mjs` | ~255 | first `Host(` argument only (WR-07) | Warning | Latent vacuous pass; criterion 4 holds on the real tree |
| `docs/architecture/drill.fish` | 36-40 | no oracle for the expected exit (WR-02) | Warning | Future drift in drill targets reads as "not caught" |
| `docs/architecture/model/monitoring.c4` | `kubelet`, `kube-apiserver`, `coredns` | authored, not placement-checked, not tied to a manifest | Info | Labelled "Authored" in three places; a live check would be needed to prove the targets are up |

Debt-marker scan: no hits. No new-scope blocker found, so the `advisory:` list is empty.

### Advisory (New Scope, Unevidenced)

None.

### Human Verification Required

None. Two optional checks that need a live cluster or a live run, neither a gate:

1. **Live scrape target list against the 17 edges** (needs cluster access; not in the repo). With `kubectl -n monitoring port-forward svc/kps-prometheus 9090:9090`, then `curl -s localhost:9090/api/v1/targets` and compare the `scrapePool` names with the drawn set (node-exporter, postgres-exporter, cert-manager, app, Redpanda, kubelet incl. cAdvisor and probes, API server, CoreDNS, kube-state-metrics, operator, Grafana, plus Prometheus itself). A target present live and not drawn would be a new gap against criterion 1; a drawn target that is down does not contradict "an edge to each thing they scrape". The Service name `kps-prometheus` follows from `fullnameOverride: kps` at `kube-prometheus-stack.yaml:~33`; I did not confirm it against a rendered chart.
2. **Independent reproduction of the post-fix 401 run** (command in the Criterion 3 section).

## Gaps Summary

No open gap against a ROADMAP criterion or a plan must_have. Pass 1's four gaps and pass 2's two open items are closed and rechecked against files and commands, not against SUMMARYs. The stated scope limits (Prometheus self-scrape undrawn, live target list unread, 401 observation limited to cookie-less `GET`/`POST /api/boards` on one machine without a proxy) are written in the artifacts at the point where they apply and are not rounded up anywhere I looked. WR-01, WR-02, WR-07, WR-09 and the info findings stay deliberately unfixed and contradict nothing.

---

_Verified: 2026-10-28T17:53:13Z_
_Verifier: Claude (gsd-verifier)_
