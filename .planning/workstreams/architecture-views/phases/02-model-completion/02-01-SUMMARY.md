---
phase: 02-model-completion
plan: 01
subsystem: architecture-views
tags: [likec4, sync-check, drills, deployment-views]

requires:
  - phase: 01-foundations-edge-fix-and-honesty-gates
    provides: sync.mjs check, drill.fish harness, drill-output.txt recording
provides:
  - checkEdgeLabels, checkParticipants, checkDeployedWorkloads rules in sync.mjs
  - one protocol-level title per merging instance pair, so production exports no `[...]` edge
  - spec kinds deploymentNode provider, element external, element note, job notation "Run-to-completion job"
  - production with a per-namespace include list that new monitoring and Flux instances cannot leak into
  - drill harness MODEL override and drills D17, D17b, D18, D19, recorded before and after
affects: [02-02, 02-03, 02-04, 02-05, STD-01 amendment]

actuals:
  tokens: 6469
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "A deployment edge merges every logical relationship on one instance pair: one shared title per pair, view-level `include <rel> with { title }` to restore specifics in element views"
    - "Not-caught-before evidence: run the harness with SYNC and MODEL pointing at the commit before the rule"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-01-label-red.txt
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-drills-before.txt
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/model/model.c4
    - docs/architecture/model/views.c4
    - docs/architecture/model/spec.c4
    - docs/architecture/drill.fish
    - docs/architecture/drill-output.txt

key-decisions:
  - "production includes `netcup.k3s.<ns>.**` plus an explicit `netcup.k3s.kube-system`, not `netcup.k3s.<ns>.*`: the `.*` form draws relationship-less instances and silenced Phase 1 drill D13"
  - "WR-03 folded in (checkDeployedWorkloads); WR-04 left open, every element this phase adds anchors by workload"

requirements-completed: []  # PHYS-02/03/04 are in the plan's requirements, but the plan states it covers only their "no [...] label" half; plans 02-02 and 02-03 complete them, so none is marked complete here

duration: not measured (continuation after a decision checkpoint)
completed: 2026-10-28
status: complete
plan_head_before: 71637bd91e52b3c55fd668a29b16d90a61c9b65e
plan_head_after: 4e665f2a3ecadb555d5bb75ae14db6fe6d450749
---

# Phase 02 Plan 01: Label, participant and workload rules, plus a narrowed production view Summary

**sync.mjs now fails on a merged `[...]` deployment-edge label, a scenario with more than five participants and a workload element with no instance, each proven by a drill recorded exit 0 against the bc06665 script and model and exit 1 against the current ones; `production` exports no `[...]` edge.**

## Commits

| Task | Commit | What |
|---|---|---|
| 1 (tracer) | 3eb342c | checkEdgeLabels, five titles, four `process` overrides, 02-01-label-red.txt |
| 2 | 8efe2ca | spec kinds, explicit `production` include list (the `.*` form, revised below) |
| 2 fix | 99ce074 | `production` include revised to `.**` plus explicit kube-system after the D13 regression |
| 3 | 4e665f2 | checkParticipants, checkDeployedWorkloads, MODEL override, D17/D17b/D18/D19, re-recorded drill-output.txt, 02-drills-before.txt |

`commits: 4` is measured: `git rev-list --count 71637bd..HEAD`.

## Red lines (Task 1, 02-01-label-red.txt)

Exit 1 with four `has merged label '[...]'` lines in view `production`: nonprod.kanban -> data.postgres, nonprod.kanban -> nonprod.redpanda, prod.kanban -> data.postgres, prod.kanban -> prod.redpanda. After the title fix: exit 0, OK tally unchanged (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads).

## Final-state measurements

- Export: `production 0` (one deployment view, zero `[...]` edges); `process` edge labels sorted-equal to the pre-change export; `production` has 13 nodes.
- Leak gate over docs/architecture/model prints nothing (it printed model.c4:91 at HEAD before Task 1).
- Exit-code invariant against bc06665 prints 0.
- `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` exits 0 (run after the final re-recording).
- 02-drills-before.txt: D17, D17b, D18, D19 each exit=0. sync-before.mjs is byte-identical to `git show bc06665:docs/architecture/sync.mjs`, and the five model-before files have the same blob hashes as bc06665's.
- drill-output.txt: D17 exit=1 (two merged-label lines, nonprod and prod kanban -> postgres), D17b exit=0, D18 exit=1 (`dynamic view signin-ok has 6 participants, more than 5`), D19 exit=1 (backup claims CronJob/postgres-backup, no instance). 31 sections in drill.fish and in the recording.
- D16 gains `instance netcup.k3s.flux-system.certmgr appears in no deployment view` (still exit 1). D10 gains a validation line (`Could not resolve reference ... 'cert-manager'`) (still exit 1). D13 stays exit 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Task 2's per-namespace `.*` include silenced Phase 1 drill D13**
- **Found during:** Task 3 exit-code invariant (printed 1, must be 0)
- **Issue:** `netcup.k3s.<ns>.*` draws instances that have no relationship, so `checkViewMembership` no longer reported an unlinked instance and D13 flipped exit 1 -> exit 0. The plan's expected result "same 13 nodes" held, so the plan's verify command did not see it.
- **Fix:** After a user decision at a checkpoint (option 1), `production` now lists `netcup.k3s.kube-system, netcup.k3s.kube-system.**, netcup.k3s.prod.**, ...nonprod.**, ...data.**, ...cert-manager.**`. Measured on the real model before commit: same 13 node ids compared by id, D13 exit 1, D16 and D10 changes unchanged, leak gate clean. A scratch probe earlier showed plain `netcup.k3s.<ns>.**` also drops the kube-system namespace node, hence the explicit entry.
- **Files modified:** docs/architecture/model/views.c4
- **Commit:** 99ce074
- No drill or rule was edited to satisfy the invariant.

**2. [Plan weakness, not a code bug] Task 2's verify compared node sets weakly**
- The plan's own verify prints `13 same` using a sorted key join, which does compare ids; the weakness was elsewhere: it counts nodes, not drawn-versus-reported behaviour, so it could not see D13. Plans 02 to 05 should run the exit-code invariant after any change to a deployment-view include, not only at the end of the phase. In this continuation the node comparison was done by id set and printed `13 13 IDSET-SAME`, with missing and extra both empty.

## Not verified

- A monitoring-namespace instance was not probed directly. The leak property was checked through D16 (an instance in flux-system is reported as drawn in no view); `monitoring` is not named in the include list, so it follows the same path, but that is inference, not a measurement.
- Task 1's tracer expansion gate and the first two commits were made by the previous executor; this continuation re-ran their checks on the final tree (export, process labels, leak gate, drill replay) but did not re-run the pre-commit hook for them.
- Duration was not measured across the checkpoint.

## Known Stubs

None.

## Threat Flags

None. T-02-01 mitigated (backup title no longer names an in-cluster name; leak gate clean), T-02-02 mitigated (drills red before, green after), T-02-03 mitigated (`git status --short -- docs/architecture` shows only the intended edits before each commit).

## Self-Check: PASSED

Commits 3eb342c, 8efe2ca, 99ce074, 4e665f2 are ancestors of HEAD; 02-01-label-red.txt and 02-drills-before.txt exist.
