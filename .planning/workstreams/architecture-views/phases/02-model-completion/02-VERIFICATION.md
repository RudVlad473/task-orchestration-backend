---
phase: 02-model-completion
verified: 2026-10-28T15:50:00Z
status: gaps_found
score: 3/5 must-haves verified
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
covered_digest: "v3:sha256:21d341227b3890fe3088e02e41d758bc9a1c296fa35df68f2bb4526ae8139583"
behavior_unverified: 1
overrides_applied: 0
gaps:
  - truth: "Criterion 5 / STD-01: the Scenarios row of docs/DIAGRAM_CONVENTIONS.md describes the scenarios as built (plan 02-05 prohibition: MUST NOT describe in the standard anything not built)"
    status: partial
    reason: "Two statements in the standard describe something that was not built (code review WR-04 and WR-08). The Scenarios row says every step is cited in 02-SCENARIO-EVIDENCE.md, but the Scenarios viewpoint has 12 views and the evidence file has no section for signin-ok, signin-bad-credentials or signin-ceiling. The 'Outside this change' list says the Mermaid error-status-split counterpart is now built as error-401, error-403 and error-409, but the Mermaid diagram also draws a 400 VALIDATION_FAILED branch that no LikeC4 view draws, and the signup inventory row says the Mermaid diagram shows a rollback that signup-ok does not draw."
    artifacts:
      - path: "docs/DIAGRAM_CONVENTIONS.md"
        issue: "line 113 (Scenarios row, provenance cell) overclaims evidence coverage; lines 379-382 overclaim Mermaid parity"
    missing:
      - "Narrow the Scenarios provenance sentence to 'the Phase 2 scenario views', or add signin-ok, signin-bad-credentials and signin-ceiling sections to 02-SCENARIO-EVIDENCE.md"
      - "State that no 400 view is drawn and signup-ok draws no rollback, or build those views"
  - truth: "Criterion 3: the phase evidence file cites the code or manifest line behind each step (inbound-packet-path step 3 and the hostfilter element description)"
    status: partial
    reason: "The step reads 'any other new connection is dropped' and edge.c4 says the host filter 'admits new TCP connections only on allow-listed ports'. infra/vm/k3s-host-firewall.sh:63 is '-A ${CHAIN} -p icmp -j RETURN', placed before the NEW DROP at line 64, so new ICMP is admitted. The evidence row cites lines 61, 62 and 64 and skips 63, the one line that contradicts the sentence (code review WR-06). Read directly this run."
    artifacts:
      - path: "docs/architecture/model/scenarios-platform.c4"
        issue: "step 3 of inbound-packet-path states the chain drops every other new connection"
      - path: "docs/architecture/model/edge.c4"
        issue: "hostfilter description says only TCP on allow-listed ports is admitted"
      - path: ".planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md"
        issue: "inbound-packet-path row 3 omits line 63"
    missing:
      - "Reword the step and the element to include ICMP, and add line 63 to the evidence row"
  - truth: "Phase goal: each new fact is tied to a check or labelled as authored (cert-manager staging issuer)"
    status: partial
    reason: "edge.c4 describes certmanager.staging as the 'Issuer that every certificate starts on'. All three Certificates (k8s/overlays/prod/certificate.yaml:11, k8s/overlays/nonprod/certificate.yaml:11, k8s/platform/edge/certificate-monitoring.yaml:13) reference letsencrypt-production, and none references staging. The sentence was copied from the stale header comment of k8s/platform/edge/clusterissuers.yaml. The check proves only that the ClusterIssuer exists, so it stays green while the description is false (code review WR-05)."
    artifacts:
      - path: "docs/architecture/model/edge.c4"
        issue: "staging description contradicts the manifests; the staging -> acme edge implies an issuer in use"
    missing:
      - "Reword to 'Staging issuer, defined but referenced by no Certificate today', or drop the claim"
behavior_unverified_items:
  - truth: "Criterion 3: error-401 is a 401 with no SQL step (ROADMAP wording: '401 with no SQL step')"
    test: "With the app on a Postgres this repo owns (not the shared datastore on 5433), send GET /api/boards with no Cookie header twice, once with 'Accept: application/json' and once with no Accept header (curl default */*), with Postgres log_statement = 'all'; record status, whether Set-Cookie came back, the spring_session row count before and after, and the statements logged"
    expected: "401 UNAUTHENTICATED, no Set-Cookie, no spring_session row, no SQL, for both requests. If the default-Accept request creates a row, error-401 must draw an INSERT into spring_session for that client and criterion 3 is contradicted for it"
    why_human: "Needs a live app and a Postgres on a free port; plan 02-04 could not run it (VERDICT: not measured, port 5433 held by datastore-postgres). Presence checks cannot see it: SecurityConfiguration.java:127 sets IF_REQUIRED and no request cache is configured, and Spring Security's default request cache can save an unauthenticated GET in a new session (stated from knowledge, not verified in the Spring source this run)"
---

# Phase 2: Model Completion Verification Report

**Phase Goal:** Every runtime, delivery and physical fact that matters appears in the Physical, Process, Logical and Scenarios views. Each new fact is tied to a check or labelled as authored, and the LikeC4 standard describes what is drawn.
**Verified:** 2026-10-28T15:50:00Z
**Status:** gaps_found
**Re-verification:** No, initial verification

## Verdict in one paragraph

The structure is built and the gate holds: three `#physical` deployment views, all required process, data and scenario views, the readers and the drills, and the standard's rewritten rows. I re-ran the checks and they are green. The goal is only partly achieved: three authored statements are contradicted by the repo (staging issuer, host-filter ICMP, evidence/Mermaid-parity claims in the standard), and criterion 3's "401 with no SQL step" is drawn and honestly labelled but unmeasured, so I do not count it verified.

## Re-run evidence (this run, stdin redirected)

| Command | Result |
| ------- | ------ |
| `node docs/architecture/sync.mjs check` | `OK (56 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)`, exit 0 |
| `python3 scripts/pre-commit-likec4-selftest.py < /dev/null` | `16/16 passed` |
| `fish docs/architecture/drill.fish < /dev/null 2>/dev/null` compared with `docs/architecture/drill-output.txt` | `cmp` exit 0, byte-identical, 182 lines |
| `likec4 export json` (to a temp file, then inspected) | 3 deployment views tagged `physical`; no edge label `[...]` in any view; 13 `scenarios` dynamic views, max 5 participants each |

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1 | Three `#physical` deployment views (`production`, `delivery`, `monitoring`); delivery shows GitHub, runner, Docker Hub, Flux and the image path; monitoring has an edge per scraped thing; cert-manager with both ClusterIssuers, edge routing, host filter as external; view-membership check green; no `[...]` label | VERIFIED (warning: see human item 2) | Export: `monitoring`, `production`, `delivery`, all `physical`. `delivery` edges: repo -> runner.ci -> docker-hub, flux -> repo, flux -> docker-hub, flux -> prod/nonprod kanban, kanban -> docker-hub. `production` has `issuer-staging`, `issuer-production`, `certmanager`, `hostfilter` (kind `external`), `firewall`, `svclb`, traefik routes. No `[...]` label printed by my inspection of every view. `check` exit 0. Scrape-completeness caveat: only repo-evidenced targets are drawn |
| 2 | `process` shows Schema Registry, `register-schemas` init job, management port, backup flow Postgres -> encryption -> off-cluster bucket; `data` has notes for `activity_log` and Avro subjects; each note's `metadata.source` is a file `check` confirms | VERIFIED | `process` nodes `redpanda.registry`, `kanban.init` (`register-schemas`), `kanban.management`, `backup`, `b2`; edges `backup -> postgres` "pg_dump, custom format", `backup -> b2` "age-encrypted dump, S3 put", `kanban.init -> redpanda.registry`, `prometheus -> kanban.management`. `data` notes `activity_log_note` (source `V3__add_activity_log.sql`) and `avro_subjects` (source `src/main/avro`); source loop in `check` exits 0, so the paths exist |
| 3 | Scenario views for signup; error 401 (no SQL) / 403 (after ownership SELECTs) / 409 (version check); push-to-image; image-to-rollout; inbound-packet-path; ingress-to-pod; activity-log row; nightly backup; every step titled; at most 5 participants; evidence file cites the line behind each step | PRESENT_BEHAVIOR_UNVERIFIED for the 401 claim, plus a partial gap | All ten views exist, tagged `scenarios`, 3 to 5 participants, every edge titled. I spot-checked 12 cited lines (`ActivityLogRecorder.java:42,47`, `UserService.java:75`, `TaskService.java:173-176`, `deploy.yml:90,156`, `image-automation.yaml:30-36`, `postgres-backup.sh:56-66`, `k3s-host-firewall.sh:61-64`) and the quotes match. Gaps: the 401 no-SQL claim is unmeasured; inbound step 3 is contradicted by cited script line 63 |
| 4 | `sync.mjs` reads workloads, namespaces and hosts from the k8s YAML incl. `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`; `check` exits 0; three breaks each exit 1 and are drilled; unmodified copy exits 0 | VERIFIED | `WORKLOAD_DIRS` (sync.mjs:236) includes the three trees, kinds include `HelmRelease` and `ClusterIssuer`; `routedHosts`/`checkHosts` at :249-270. Drill replay byte-identical: baseline exit=0; D20 (rename HelmRelease loki) exit=1; D21 (namespace change) exit=1; D22 (rename Flux controller) exit=1; D23 (rename ClusterIssuer) exit=1; D24 (change claimed host) exit=1; controls D16b, D17b, D13b exit=0 |
| 5 | `DIAGRAM_CONVENTIONS.md` Physical row names the deployment views as built, no longer excludes the monitoring stack; Scenarios row describes scenarios as built incl. internal steps of the activity-log and delivery scenarios; the phrase "excluded, as in `production-host.mmd`" is absent | FAILED (partial) | `rg 'excluded, as in'` finds nothing (exit 1). Physical row (line 112) names `production`, `delivery`, `monitoring` and lists monitoring as built. Scenarios row (line 113) names the activity-log and delivery internal steps. But the standard overclaims in two places (WR-04, WR-08, see gap 1), against the plan 02-05 prohibition "MUST NOT describe in the standard anything not built" |

**Score:** 3/5 truths verified (criteria 1, 2, 4); criterion 3 has 1 present-but-behavior-unverified claim and 1 partial gap; criterion 5 has a partial gap.

### Criterion 3 and the unmeasured 401 path: my reading

The letter of criterion 3 is met: an `error-401` view exists, tagged `scenarios`, with 3 participants and no SQL step. The fact behind it is not established. Plan 02-04 recorded `VERDICT: not measured`, the view description says the absence of SQL is "authored, not measured", and the plan's own truth allows "or states why it was not measured". So the plan complied with itself, and the plan prohibition ("MUST NOT draw or describe the 401 path as SQL-free beyond what the measurement shows") is met because the description hedges.

That makes the phase partially achieved on criterion 3, and not verified. The risk is real, not hypothetical: `SecurityConfiguration.java:127` sets `SessionCreationPolicy.IF_REQUIRED`, no `requestCache` is configured anywhere in `src/main/java`, and the request goes through `ExceptionTranslationFilter` before the entry point. If the default request cache saves the unauthenticated GET for a client sending `Accept: */*`, a `spring_session` row is inserted and the 401 is not SQL-free for that client. I believe the default matcher ignores only JSON-accepting clients, but I did not verify that against the Spring Security source, so treat it as a hypothesis the measurement should settle. I did not round this up. It is an open `unrun-verify` entry (id 10) in `.planning/WINDOWS.md`. The roadmap says the discriminator is "401 with no SQL step", so if the measurement shows an INSERT, criterion 3 is contradicted for that client and the view must change.

I did not run the measurement myself: it needs a live app and a free Postgres port, and this verification forbids starting servers or touching the shared datastore.

### Code review findings weighed against the contract

| Finding | Contradicts a contract item? | Disposition here |
| ------- | ---------------------------- | ---------------- |
| WR-04 (evidence claim vs three `signin-*` views) | Yes: plan 02-05 prohibition on describing the standard beyond what is built; criterion 5 "as built" | Gap 1 |
| WR-08 (Mermaid parity overstated) | Yes: same prohibition | Gap 1 |
| WR-06 (host filter drops "any other new connection", ICMP admitted at line 63) | Yes: criterion 3 "cites the line behind each step" is not honoured when the cited block's line 63 contradicts the step | Gap 2 |
| WR-05 (staging "every certificate starts on", all Certificates use production) | Yes: phase goal "tied to a check or labelled as authored"; the sentence is neither checked nor true | Gap 3 |
| WR-01 (metadata-key allowlist is not per surface; `source [...]` throws) | No roadmap criterion or plan truth is contradicted; defends the rules' meaning, current model passes them honestly | Warning, not a gap |
| WR-07 (`routedHosts` reads only the first `Host(` argument, ignores `HostRegexp`) | No: every current `IngressRoute` uses one literal per `Host(`, so criterion 4 holds on the real tree (I read all `Host(` lines in `k8s/overlays` and `k8s/monitoring`). Latent vacuous pass | Warning |
| WR-02 (drill.fish has no machine oracle) | No: the recorded output is byte-identical and shows the expected exits; the weakness is future drift | Warning |
| WR-03, WR-09, IN-01 to IN-05 | WR-09 (public Grafana path not drawn) is not in any criterion or truth; others are hygiene | Notes |

All 14 findings stay `open` in `02-REVIEW-DISPOSITION.md`; none was fixed after review.

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `docs/architecture/sync.mjs` | `checkEdgeLabels`, `checkParticipants`, `checkDeployedWorkloads`, host and HelmRelease readers | VERIFIED | Present at :249-270, :457-480, :236; called from `check()` at :603-627 |
| `docs/architecture/model/{delivery,edge,monitoring,scenarios-app,scenarios-platform}.c4` | new elements, views, scenarios | VERIFIED | All exist, parse and export |
| `docs/architecture/model/spec.c4` | `deploymentNode provider`, `element external`, `element note` | VERIFIED | Kinds `provider`, `external`, `note` appear in the exported elements |
| `docs/architecture/drill.fish`, `drill-output.txt` | drills D17 to D24, replay identical | VERIFIED | `cmp` exit 0 |
| `02-SCENARIO-EVIDENCE.md` | section per Phase 2 view with file:line and quote | VERIFIED for the ten Phase 2 views; no section for the three `signin-*` views | Row 3 of inbound-packet-path omits line 63 |
| `02-04-401-measure.txt` | measurement or reason it did not run | VERIFIED as an honest record | `VERDICT: not measured` |
| `docs/DIAGRAM_CONVENTIONS.md` | Physical and Scenarios rows amended | PARTIAL | Rows amended; two overclaims remain |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `checkEdgeLabels` | LikeC4 computed deployment views | `view.edges()` label, `has merged label` | WIRED | `sync.mjs:457-462`, called at :625; D17 exit=1 |
| `views.c4` process view | `model.c4` relationships | `include ... with { title }` | WIRED | Process view labels exported intact (12 edges) |
| `drill.fish fresh()` | scratch model copy | `MODEL` override | WIRED | `drill-output.txt` replays identically |
| `sync.mjs` workload sets | `k8s/monitoring`, `k8s/platform`, `gotk-components.yaml` | `WORKLOAD_DIRS` | WIRED | D20, D22, D23 exit=1 |
| `sync.mjs` `checkHosts` | `IngressRoute` matches | `routedHosts` | WIRED, with the WR-07 limit | D24 exit=1 |

### Data-Flow Trace (Level 4)

Not applicable: the artifacts are a documentation model and a checker, not a rendered data path. The model-to-code tie is the `check` run above.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Model matches code | `node docs/architecture/sync.mjs check` | exit 0, 17 workloads | PASS |
| Hook self-test | `python3 scripts/pre-commit-likec4-selftest.py < /dev/null` | 16/16 | PASS |
| Drill replay | `fish docs/architecture/drill.fish ... | cmp` | identical | PASS |
| No `[...]` label, 3 physical views, participant cap | `likec4 export json` + inspection | as stated above | PASS |
| Cookie-less 401 issues no SQL | needs live app | not run | SKIP, routed to human |

### Probe Execution

SKIPPED: the plans declare no `probe-*.sh` scripts.

### Requirements Coverage

All 14 IDs appear in a PLAN `requirements:` field and in REQUIREMENTS.md; none is orphaned. SCEN-08 maps to Phase 3 and is correctly absent.

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| PHYS-02 | 02-01, 02-03 | Delivery view | SATISFIED | `delivery` view, 8 edges |
| PHYS-03 | 02-01, 02-02 | Monitoring view with scrape edges | SATISFIED, scope narrowed | Repo-evidenced targets only (human item 2) |
| PHYS-04 | 02-01, 02-02, 02-03 | cert-manager, issuers, edge routing, host filter external | SATISFIED structurally; staging description false (gap 3) | `production` nodes and edges |
| PROC-01 | 02-02 | Schema Registry, init job, management port, backup flow | SATISFIED | `process` view |
| LOGI-01 | 02-02 | `activity_log` and Avro notes tied to sources | SATISFIED | `data` view notes with `source` |
| SCEN-01 | 02-04 | Signup | SATISFIED | `signup-ok` |
| SCEN-02 | 02-04 | Error status: 401 no SQL, 403, 409 | PARTIAL: 403 and 409 satisfied; 401 no-SQL unmeasured | `error-401/403/409`, `02-04-401-measure.txt` |
| SCEN-03 | 02-05 | Push-to-image | SATISFIED | `push-to-image` |
| SCEN-04 | 02-05 | Image-to-rollout | SATISFIED | `image-to-rollout` |
| SCEN-05 | 02-05 | Inbound packet path, ingress-to-pod | SATISFIED structurally; inbound step 3 inaccurate (gap 2) | `inbound-packet-path`, `ingress-to-pod` |
| SCEN-06 | 02-04 | Mutation to `activity_log` row | SATISFIED | `activity-log-row` |
| SCEN-07 | 02-05 | Nightly backup | SATISFIED | `nightly-backup` |
| GATE-01 | 02-02, 02-03 | `sync.mjs` reads k8s facts and fails on drift | SATISFIED | D20 to D24 |
| STD-01 | 02-05 | Physical and Scenarios rows amended to match what is built | PARTIAL | Gap 1 |

REQUIREMENTS.md marks all 14 `[x]` and "Complete". PHYS-04, SCEN-02, SCEN-05 and STD-01 are overstated by the gaps above.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `docs/architecture/model/edge.c4` | 9-15 | description contradicted by manifests | Blocker for gap 3 | False physical fact |
| `docs/architecture/model/scenarios-platform.c4` | 37 | "any other new connection is dropped" vs script line 63 | Warning | False step |
| `docs/DIAGRAM_CONVENTIONS.md` | 113, 379-382 | claims exceed what is built | Warning | Standard overclaims |
| `docs/architecture/sync.mjs` | 255 | first `Host(` argument only | Warning | Latent vacuous pass |
| `docs/architecture/drill.fish` | 36-40 | no oracle for expected exit | Warning | Drift in drill targets reads as "not caught" |

Debt-marker scan (`TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER`) over `sync.mjs`, `drill.fish`, `model/`, `DIAGRAM_CONVENTIONS.md` and the self-test: no hits. Leak scan over `docs/architecture/model` (in-cluster DNS, IP literal, age recipient, bucket host, key paths, Secret names): no hits.

### Human Verification Required

#### 1. Cookie-less 401 SQL measurement

**Test:** Run the app against a Postgres this repo owns on a free port (not the shared `datastore-postgres` on 5433), set `log_statement = 'all'`, and send `GET /api/boards` with no `Cookie` once with `Accept: application/json` and once with no `Accept` header. Record status, `Set-Cookie`, `spring_session` row count before and after, and the logged statements (steps in `02-04-401-measure.txt` "Method planned"; commands in `docs/LOCAL_DEV.md` "Inspecting live output").
**Expected:** 401 UNAUTHENTICATED, no `Set-Cookie`, no row, no SQL, for both. If either request writes `spring_session`, `error-401` must draw that INSERT for that client and its description must change. Then replace the `VERDICT` line in `02-04-401-measure.txt` and close `.planning/WINDOWS.md` entry 10.
**Why human:** needs a live app and a free Postgres port; the planned run was blocked because the only port-5433 listener is the user's datastore, which must not be stopped.

#### 2. Is "an edge to each thing they scrape" satisfied by repo-evidenced targets only?

**Test:** Decide whether `monitoring` may omit the chart-default scrape targets of kube-prometheus-stack (kubelet and cAdvisor, kube-state-metrics, API server, operator), which are not overridden in `k8s/monitoring/controllers/kube-prometheus-stack.yaml` (only controller-manager, scheduler, etcd and proxy are disabled) and are evidenced by `k8s/monitoring/configs/dashboards/cadvisor.json`. Settle it with `kubectl -n monitoring get servicemonitors` on the cluster, or accept the narrower reading with an override.
**Expected:** Either those targets get edges, or an override records "the monitoring view draws the repo-evidenced scrape targets" and the view description names the omitted defaults. Today the description says the edges come from the repo's monitor settings but does not say chart-default targets are left out.
**Why human:** the live target list cannot be read from the repo, and the criterion wording is ambiguous.

## Gaps Summary

Three gaps, all fixable in documentation or model text, none needing new checks:

1. **Standard overclaims (criterion 5, STD-01).** Narrow the Scenarios provenance sentence to the Phase 2 views (or add the three `signin-*` evidence sections), and state the Mermaid gaps (no 400 view, no signup rollback).
2. **Host-filter step and element (criterion 3).** Reword `inbound-packet-path` step 3 and the `hostfilter` description to include ICMP, and add `k3s-host-firewall.sh:63` to the evidence row.
3. **Staging issuer description (phase goal).** Say the staging issuer is defined and referenced by no Certificate today.

Plus the open `unrun-verify` for the 401 path, which keeps criterion 3 unproven until measured. The five checks (`check`, self-test, drill replay, export) are green, so the structural half of the phase is solid; what fails is the accuracy of a few authored sentences, which is exactly what this phase's goal ("tied to a check or labelled as authored") is about. Gap-closure plans via `/gsd-plan-phase --gaps` should be small.

---

_Verified: 2026-10-28T15:50:00Z_
_Verifier: Claude (gsd-verifier)_
