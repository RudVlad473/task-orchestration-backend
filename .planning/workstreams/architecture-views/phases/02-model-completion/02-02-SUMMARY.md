---
phase: 02-model-completion
plan: 02
subsystem: architecture-views
tags: [likec4, sync-check, helmrelease, monitoring, process-view, drills]

requires:
  - phase: 02-model-completion
    provides: plan 02-01 label, participant and workload-instance rules; revised production include; MODEL drill override
provides:
  - sync.mjs reads HelmRelease workloads under k8s/platform and k8s/monitoring
  - monitoring.c4 with six monitoring elements, their instances and the `monitoring` Physical view
  - edge.c4 with the cert-manager controller element and instance
  - process view with Schema Registry, init job, management server, backup flow; data view with two notes
  - drills D20 and D21, recorded before and after
affects: [02-03, 02-04, 02-05]

actuals:
  tokens: 2800  # chars/4 over the diff of sync.mjs, the model folder and drill.fish (11095 chars); recordings excluded
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Chart-installed components: one element per runtime role, each carrying `workload 'HelmRelease/<name>'`; three elements share the kube-prometheus-stack claim"
    - "A deployment view that includes app instances only to show monitoring edges excludes the instance-pair edges with `exclude a <-> b` and `exclude a -> b`"

key-files:
  created:
    - docs/architecture/model/monitoring.c4
    - docs/architecture/model/edge.c4
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-02-red.txt
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/model/model.c4
    - docs/architecture/model/views.c4
    - docs/architecture/drill.fish
    - docs/architecture/drill-output.txt
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-drills-before.txt

key-decisions:
  - "Encryption in the backup flow is the label of the edge to the bucket, not a node of its own"
  - "The monitoring view's exclude list names the six app, broker and Postgres edges by instance pair, not by wildcard"

requirements-completed: [PHYS-03, PROC-01, LOGI-01]  # PHYS-04 (issuers, edge routing, host filter) and GATE-01 (Flux controllers, hosts) are finished by plan 03 and are left unmarked

duration: not measured
completed: 2026-10-28
status: complete
plan_head_before: b0ecd8a6b4c7e8b51c93255457c9ff7efc3b79a7
plan_head_after: 0d177d68e9a2a6f6febc236e8cf6a82180f65a3b
---

# Phase 02 Plan 02: HelmRelease workloads, the monitoring view, and Process and Logical additions Summary

**sync.mjs now compares HelmRelease Kind/name against element workload claims, seen red on the real model with five missing elements and green with them; the `monitoring` view draws eleven scrape and query edges, `process` gains the registry, init job, management server and backup flow, and D20 and D21 are exit 0 before and exit 1 after.**

## Commits

| Task | Commit | What |
|---|---|---|
| 1 (tracer) | 92bcab6 | HelmRelease in WORKLOAD_KINDS, k8s/monitoring/ in WORKLOAD_DIRS, monitoring.c4, edge.c4, 02-02-red.txt |
| 2 | 2cc5cb4 | management, init, registry, activity_log_note, avro_subjects, b2, registry and backup relationships, process and data view changes |
| 3 | 0d177d6 | Prometheus edges to the app and Redpanda, exclude list, drills D20 and D21, both recordings |

`commits: 3` is measured: `git rev-list --count b0ecd8a..HEAD`.

## Red lines (02-02-red.txt)

With the reader changed and no element added: `exit=1` and five lines, `k8s HelmRelease/<name> has no element carrying metadata workload 'HelmRelease/<name>'` for alloy, kube-prometheus-stack, loki, postgres-exporter and cert-manager (tally 33 elements, 9 workloads). After the elements: `OK (40 elements ... 9 workloads)`, `exit=0`. After Task 2 the tally is 46 elements.

## The eleven `monitoring` edges as exported

- prometheus -> node-exporter : scrape metrics
- prometheus -> postgres-exporter : scrape metrics
- grafana -> prometheus : PromQL queries
- grafana -> loki : LogQL queries
- alloy -> loki : push pod logs
- prometheus -> cert-manager (certmanager) : scrape metrics
- postgres-exporter -> data.postgres : read statistics over SQL
- prometheus -> prod.kanban, nonprod.kanban : scrape metrics
- prometheus -> prod.redpanda, nonprod.redpanda : scrape metrics

No `[...]` label; the deployment views are `monitoring,production` with 0 `[...]` edges. Before the exclude list the view also drew six app, broker and Postgres edges (kanban -> postgres, kanban <-> redpanda, in both namespaces); the four exclude predicates removed exactly those.

## `process` nodes as exported

b2, backup, client, kanban.api, kanban.consumer, kanban.init, kanban.management, kanban.publisher, postgres, prometheus, redpanda.activity, redpanda.dlt, redpanda.registry (13 nodes). The `data` view holds avro_subjects and postgres.activity_log_note beside the generated tables; check exits 0, so both notes' sources exist, and neither description carries a count.

## ROADMAP criterion 2: the backup flow

The exported `process` edges are `backup -> postgres : pg_dump, custom format` and `backup -> b2 : age-encrypted dump, S3 put`. Encryption is shown as the label of the edge to the bucket, not as its own node: it is one pipe stage inside postgres-backup.sh rather than a runtime element, and a child of `backup` would need a relationship between a parent and its child, which no measurement in this workstream has shown LikeC4 1.60 accepts.

## Drills D20 and D21

| Drill | Before (bc06665 script and model) | After (current) |
|---|---|---|
| D20 loki HelmRelease renamed | exit=0 | exit=1: `k8s HelmRelease/lokix has no element carrying metadata workload 'HelmRelease/lokix'` and `model claims workload HelmRelease/loki but k8s/ has no such manifest` |
| D21 loki moved to kanban-data | exit=0 | exit=1: `placement: loki is deployed in [monitoring] in the model, manifests place it in [kanban-data]` |

D17, D17b, D18 and D19 also read exit=0 in the re-recorded 02-drills-before.txt. Verified: exit-code invariant against bc06665 prints 0; D13b, D16b and D17b are exit=0; drill.fish and drill-output.txt each hold 33 sections; `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` exits 0 after the final recording; the leak gate over docs/architecture/model prints nothing. Tally in the recording reads 46 elements and 9 workloads in every section except four (D13, D13b, D16 and D16b), which read 47 because the drill adds an element to the model.

## Deviations from Plan

None - plan executed exactly as written. The exclude predicates were left to the executor by the plan; the first export without them showed the six extra edges and the second export with them matched the plan's eleven.

## Not verified

- The check proves each HelmRelease exists and lands in its namespace, not what the chart renders; Prometheus, Grafana and node-exporter share one claim, so renaming that HelmRelease reports one missing claim (accepted limit A11, T-02-05).
- The scrape edges come from PodMonitor, ServiceMonitor and chart settings read by hand; no live target was queried and sync.mjs does not check them.
- The two notes are authored; a later foreign key on activity_log would contradict its note without failing a check (plan trade-off, option C rejected).
- The `process` view now has 13 nodes; whether that width is readable is not judged here (plan 05 records it).
- Duration was not measured.

## Known Stubs

None.

## Threat Flags

None. T-02-04 mitigated (fresh one-sentence descriptions, leak gate prints nothing), T-02-05 accepted as stated above, T-02-SC no package installed.

## Self-Check: PASSED

Commits 92bcab6, 2cc5cb4 and 0d177d6 are ancestors of HEAD; monitoring.c4, edge.c4 and 02-02-red.txt exist.
