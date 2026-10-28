---
phase: quick-261028-opl
plan: 01
subsystem: architecture-views
tags: [likec4, kube-prometheus-stack, monitoring-view, error-401, docs]
requires:
  - phase: 02-model-completion
    provides: the monitoring deployment view, the error-401 view, the 401 post-fix re-measure (2fbe8ff)
provides:
  - six Prometheus scrape edges in the monitoring view, each tied to a chart values line
  - the 401 path stated as observed at 7bccf71, with its limits
  - an addendum answering human item 1 in 02-VERIFICATION.md, without an override
affects: [02-verification re-run, phase 3 publication]
actuals:
  tokens: 6770
  tasks: 3
  commits: 3
plan_head_before: 2fbe8ffbfbecd4546e99c3088073bf22c5e6091e
plan_head_after: e7a3f914d41cfaea50d69ff24a58ab3d32bc3705
tech-stack:
  added: []
  patterns: ["a drawn chart default is tied to a quoted values line plus the byte count and sha256 of the file read"]
key-files:
  created:
    - .planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt
  modified:
    - docs/architecture/model/monitoring.c4
    - docs/architecture/model/scenarios-app.c4
    - docs/architecture/drill-output.txt
    - docs/DIAGRAM_CONVENTIONS.md
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-04-401-measure.txt
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-VERIFICATION.md
key-decisions:
  - "Draw every default monitor the repo leaves on that LikeC4 can draw (six edges), not only the four the scope named"
  - "State the Prometheus self-scrape instead of drawing it: LikeC4 rejects a self-relationship"
  - "No new sync.mjs rule and no new drill"
requirements-completed: [PHYS-03, SCEN-02, STD-01]
duration: 22min
completed: 2026-10-28
status: complete
---

# Phase quick-261028-opl Plan 01: Phase 02 final gap pass Summary

**The monitoring view draws Prometheus edges to the kubelet with cAdvisor, the API server, CoreDNS, kube-state-metrics, the operator and Grafana, each tied to a quoted kube-prometheus-stack 91.5.2 values line, and the error-401 text now says the absence of SQL was observed at 7bccf71, with the capture-window limit named.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-10-28T17:22Z
- **Completed:** 2026-10-28T17:44Z
- **Tasks:** 3
- **Files modified:** 8 (one created)
- **BASE (`git rev-parse --short HEAD` before Task 1):** 2fbe8ff

## Task Commits

1. **Task 1: draw the chart-default scrape targets** - `1d6c40e` (docs)
2. **Task 2: the 401 path as observed at 7bccf71** - `6b6385d` (docs)
3. **Task 3: sizes and the 02-VERIFICATION.md addendum** - `e7a3f91` (docs)

All three used `--no-verify`, allowed by the project CLAUDE.md for paths under docs/ and .planning/. gitleaks 8.30.1 was run by hand over the five changed paths before the last commit (and before Tasks 1 and 2): "no leaks found" each time. The plan metadata commit is left to the orchestrator.

## The values download

- Command: `gh api -H 'Accept: application/vnd.github.raw' 'repos/prometheus-community/helm-charts/contents/charts/kube-prometheus-stack/values.yaml?ref=kube-prometheus-stack-91.5.2'`, at 2026-10-28T17:23Z.
- Measured: 218683 bytes, sha256 7d089fcf435e6904cda5ad246602e1c61005d08c68e3a9ab82770c249e395081. Both equal the planning figures.
- No line of the plan's context table differed from the file. The repo side was re-read too (kube-prometheus-stack.yaml lines 1, 13, 25, 37-38, 45-52, 83-84, 86-137, 160-187, 169-187; loki.yaml 99-103; the k3s disable list names only metrics-server): all as the plan stated.
- Read only with `sed -n` and `rg -n` from a fresh directory under the gitignored docs/architecture/out, then trashed.

## The six drawn edges

| Target | Edge title | Values line behind it |
|---|---|---|
| kubelet (with cAdvisor) | scrape kubelet, cAdvisor and probe metrics over HTTPS | 1838-1849, `https: true` 1900, `cAdvisor: true` 1944 |
| Kubernetes API server | scrape API server metrics | 1735-1736, 1748-1749 |
| CoreDNS | scrape DNS metrics on port 9153 | 2229-2233, 2242-2243 |
| kube-state-metrics | scrape cluster object state metrics | 2835-2836, 2849-2853 |
| Prometheus Operator | scrape operator metrics | 2981-2982, 3525-3528 |
| Grafana | scrape Grafana server metrics | 1696-1700 |

CoreDNS and Grafana go beyond the four targets the scope named. The reason: the full values file turns both monitors on (`coreDns.enabled: true`, `grafana.serviceMonitor.enabled: true`), the repo changes neither, and the criterion "an edge to each thing they scrape" would still fail on two drawable targets. The orchestrator accepted this.

The API server title names no protocol. No values line in the kubeApiServer block (1735-1749) states the scheme: its tlsConfig (1741-1747) sets TLS client fields, and the only "https" in that range is a URL in the comment at line 1739. Only the kubelet title says HTTPS, on line 1900.

G3c printed 13 Prometheus edges in `monitoring`, the 7 earlier ones unchanged. G3b printed `monitoring 25 17`, `production 24 17`, `delivery 14 8`; G3 printed `delivery,monitoring,production 0`.

### The 25 nodes of `monitoring`

netcup, netcup.k3s, netcup.k3s.cert-manager, netcup.k3s.cert-manager.certmanager, netcup.k3s.data, netcup.k3s.data.postgres, netcup.k3s.kube-apiserver, netcup.k3s.kube-system, netcup.k3s.kube-system.coredns, netcup.k3s.kubelet, netcup.k3s.monitoring, netcup.k3s.monitoring.alloy, .grafana, .kube-state-metrics, .loki, .node-exporter, .postgres-exporter, .prometheus, .prometheus-operator, netcup.k3s.nonprod, netcup.k3s.nonprod.kanban, netcup.k3s.nonprod.redpanda, netcup.k3s.prod, netcup.k3s.prod.kanban, netcup.k3s.prod.redpanda.

The 25th, the one that is neither an earlier node nor one of the five new instances (kubelet, kube-apiserver, coredns, kube-state-metrics, prometheus-operator), is the implicit ancestor `netcup.k3s.kube-system`, brought in by the CoreDNS instance. That is the expected one. (19 earlier + 5 new + 1 = 25.)

## Not drawn, and why

- Prometheus self-scrape (values 4341-4344, `selfMonitor: true`): stated in the record, the view description, the prometheus description and the standard. LikeC4 1.60 rejects a self-relationship. Measured again at execution on a scratch copy: `- model does not validate: Invalid parent-child relationship`, exit 1.
- controller-manager, scheduler, etcd, kube-proxy: the repo turns them off (kube-prometheus-stack.yaml 45-52). Alertmanager: off at 37-38.
- kubeDns, thanosRuler, windowsMonitoring: the chart leaves them off (2322-2323, 5515 and 5519, 367 and 369).
- Loki: its ServiceMonitor is off (loki.yaml 102-103).

## No rule, no drill

No sync.mjs rule and no drill were added. Each new element already falls under a drilled rule (checkDeployedWorkloads, checkPlacement, view membership, checkEdgeLabels, the `source` exists check), so "every new rule seen red first" is vacuous. G7r (diff of 518eb75's drill-output.txt against the re-recorded one, element counts normalized) printed nothing and exited 0; the tallies are `29 (61 elements` and `4 (62 elements`. `git diff --stat 2fbe8ff..HEAD` over k8s, src, scripts, sync.mjs, drill.fish, views.c4, deployment.c4, spec.c4 and ROADMAP.md printed nothing. The drill replay (G7) is byte-identical (run twice, after the re-record and on the final tree); the self-test passes 16/16.

## PNG sizes

Natural size is the PNG width and height divided by 2. Export: `fish docs/architecture/likec4.fish png`, 20 views in 3 min 2 s.

| Row | Old | New |
|---|---|---|
| `monitoring` | 3490 x 1574 | 6725 x 1276 |
| `production` | 3427 x 2566 | 3461 x 2566 |
| `process` | 3280 x 1536 | 3281 x 1536 |
| `inbound-packet-path` | 2148 x 665 | 2166 x 680 |

The `monitoring` change was expected. The other three were not, and none of their views was edited: they are reported, not explained (the cause was not investigated, and the standard says so). All other Phase 2 rows matched their PNGs. The new `monitoring` image is very wide (13450 px); it is recorded without a verdict, as the standard says. The PNGs were trashed.

## Measure-record lines cited (sed -n confirmed)

Numbers are after the one-line pointer insertion (planning numbers plus one). Each was printed with `sed -n` and shows the cited text:

- 161: `=== Post-fix re-measure (HEAD 7bccf71) ===`
- 191-199: rows A to I (191 is A, 199 is I)
- 207-210: the Burst paragraph (207 starts it, 210 ends with the content-type)
- 212-221: the Signin check (212 starts it, 221 ends it)
- 223-228: POST-FIX RESULT (223 starts it, 228 ends it)
- 230-243: the Unverified / limits list (230 is the heading, 243 ends the last item)
- statement-capture lines, Method: 183-184 ("The whole log was not re-read at the end, so a statement outside every request window is not ruled out")
- statement-capture lines, limits: 239-241 ("The statement capture is Postgres `log_statement=all` over a 1 second window after each request ...")

The post-fix observation covers capture windows only. The statements were captured in a 1-second window after each request, plus one log-tail read after rows A to I; the whole log was not re-read. So "no statement" is an observation of those windows, not a proof that no statement was ever sent. Every reader-facing site (view description, evidence section, closing paragraph, note (b2), the Scenarios row, the `error-401` row, the addendum) says so with the words "capture window".

## Gates changed from the n5v plan, and why

- G1: tally now `61 elements` (five added elements).
- G7: the drill record is re-recorded (the tally lines print the element count); G7r added to prove only counts changed.
- L2: scope widened to the record and the report; L4: expected count is 3 (2fbe8ff added a third loopback line); M1: base is 2fbe8ff and exactly one added line is required.
- I1 to I6: now require the inference label to be absent. I1 to I5 and the new V3 also require `capture window`, so every no-statement claim names the capture limit (plan revision).
- N1 to N6 and V1 to V3 as the plan listed them. The `monitoring` row N5 gate and the size gate (`3490 x 1574` absent) pass.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] error-401 view lacked the literal `82775fa`**
- **Found during:** Task 2, gate I1.
- **Issue:** the existing description said "Measured once before the fix" with no commit; I1 requires `82775fa`.
- **Fix:** changed it to "Measured once before the fix at 82775fa".
- **Files modified:** docs/architecture/model/scenarios-app.c4
- **Committed in:** 6b6385d

**2. [Rule 1 - Bug] capitalised `Observed at 7bccf71` would have missed the gate literal**
- **Found during:** Task 2, while writing the view description.
- **Fix:** reworded to "it was observed at 7bccf71" before running any gate.
- **Committed in:** 6b6385d

**3. [Rule 3 - Blocking] intro sentence extended**
- **Found during:** Task 3. The plan expected only `monitoring` to change; three more rows changed, so the table-intro sentence names all four re-exported rows and says the last three were not edited and the cause was not investigated.
- **Committed in:** e7a3f91

**Total deviations:** 3 (two wording fixes, one expected-versus-measured size difference reported). **Impact:** none on scope.

## Record gate and what it proves

The Task 1 record gate (`rg -o ... | sort -u | wc -l` printing 9) is a presence check only: it proves the tag, the probe result and the monitor keys appear in the record, not that each cited line number and quote is right. The citation check rests on reading each line with `sed -n` against the downloaded file in Step 1. That was done row by row for every row of the context table (kubernetesServiceMonitors, kubeApiServer, kubelet, coreDns, kubeStateMetrics, prometheusOperator, grafana, nodeExporter, prometheus self, alertmanager, the four disabled kube* monitors, kubeDns, thanosRuler, windowsMonitoring). One refinement: the planning table gave "2102, 2563, 2431, 2718" for the four disabled kube* monitors, which are the key lines; the record now states the key lines and the `enabled: true` lines that follow them (2103, 2564, 2432, 2719). No row was left unchecked. The nodeExporter chart line (2867-2868) was checked; the repo's own node-exporter monitor setting (kube-prometheus-stack.yaml 160-162) was read through the Read tool, not through sed.

## What was not done

- The live target list was not read (no cluster access); the chart was not rendered (no helm binary), so ports and schemes appear only where a values line states them.
- The Alloy chart defaults were not fetched.
- ROADMAP.md is unchanged; STATE.md and this plan's other planning files are left to the orchestrator.
- Out of scope and untouched: WR-01, WR-02, WR-07, WR-09, the info findings, clusterissuers.yaml header, Java, the Mermaid diagrams.
- 02-VERIFICATION.md: only an appended addendum. No override, no `accepted_by`, frontmatter byte-identical (V2), status left for the verifier.

## Candidate tracked item

No check reads the chart defaults, so a kube-prometheus-stack version bump can leave the drawn set stale without failing. A real fix needs either a vendored values file or a hardcoded monitor list that mirrors the model; neither was built here (trade-off matrix, "Drift rule or drawing only").

## Threat Flags

None. No new endpoint, auth path or file access was added; the model text names no host, IP, credential or local measurement port (L1 to L4 clean, gitleaks clean).

## Known Stubs

None.

## Self-Check: PASSED

Created and modified files exist; commits 1d6c40e, 6b6385d and e7a3f91 are ancestors of HEAD; on the final tree G1, G3, G3b, G3c, G5, G7, G7r, G8, L2, L3, L4, L5, M1, V1 to V3, N1, N5 and the scope diff gave their "after" values. G4, G6, L1 and I1 to I7 were last run after the Task 2 edits; Task 3 touched only DIAGRAM_CONVENTIONS.md size cells and the table intro, and 02-VERIFICATION.md, so they were not re-run.
