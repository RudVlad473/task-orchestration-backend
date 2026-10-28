---
phase: 02-model-completion
plan: 05
subsystem: architecture-views
tags: [likec4, dynamic-views, scenarios, evidence, standard, phase-gate]

requires:
  - phase: 02-model-completion
    provides: plans 02-01 to 02-04 (rules, delivery and edge elements, app scenarios, the 401 verdict)
provides:
  - scenarios-platform.c4 with dynamic views push-to-image, image-to-rollout, inbound-packet-path, ingress-to-pod and nightly-backup
  - 02-SCENARIO-EVIDENCE.md with ten cited view sections, one row per step
  - docs/DIAGRAM_CONVENTIONS.md describing the Phase 2 kinds, views, checks and natural sizes as built
  - the hook self-test (16 cases) passing again
affects: [phase-3, phase-4]

actuals:
  tokens: 7276  # chars/4 over the added lines of cfe40a7..717629c (29105 chars)
  tasks: 3
  commits: 4

key-files:
  created:
    - docs/architecture/model/scenarios-platform.c4
  modified:
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md
    - docs/DIAGRAM_CONVENTIONS.md
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-REVIEW-DISPOSITION.md
    - scripts/pre-commit-likec4-selftest.py

key-decisions:
  - "The standard states no section or case counts for the drills and the self-test, so the next drill cannot make it stale"
  - "Natural sizes of the Phase 2 views are recorded without a verdict, as the plan chose"
  - "The self-test's scratch repos copy .github/workflows and infra/vm, and test_08's injected element is placed and drawn rather than the assertion being relaxed"

requirements-completed: [SCEN-03, SCEN-04, SCEN-05, SCEN-07, STD-01]

duration: not measured
completed: 2026-10-28
status: complete
plan_head_before: cfe40a7db34a4827fa65cee21e6762c27de9be25
plan_head_after: 717629c9a9ac501f40fa61562467004602e6508c
---

# Phase 02 Plan 05: Delivery, inbound-path and backup scenarios drawn; the standard describes Phase 2 as built Summary

**Five platform dynamic views on the delivery and edge elements, each step cited or marked authored, the standard amended to what was built, and the phase gate green including the hook self-test at 16/16 after a fixture repair outside this plan's file list.**

## Commits

| Task | Commit | What |
|---|---|---|
| 1 (tracer) | 94b04dc | scenarios-platform.c4 with push-to-image, evidence section |
| 2 | e9194a7 | image-to-rollout, inbound-packet-path, ingress-to-pod, nightly-backup and their evidence sections |
| deviation | 110ed32 | hook self-test fixture repair (see below) |
| 3 | 717629c | DIAGRAM_CONVENTIONS.md amended, WR-03 and IN-03 marked fixed, WR-04 left open |

`commits: 4` is measured: `git rev-list --count cfe40a7..HEAD` before this SUMMARY commit. No per-plan ledger file existed for 02-05, so the base is the HEAD that preceded Task 1.

## Steps to evidence rows

From `likec4 export json --skip-layout` (edges) and the evidence file (rows), on the final tree.

| View | Participants | Steps | Evidence rows |
|---|---|---|---|
| push-to-image | 4 | 5 | 5 |
| image-to-rollout | 5 | 6 | 6 |
| inbound-packet-path | 5 | 5 | 5 |
| ingress-to-pod | 3 | 5 | 5 |
| nightly-backup | 3 | 5 | 5 |

Thirteen dynamic views are tagged `scenarios`, none above 5 participants. The evidence file has ten `## view ` sections (the plan's count for a 401 view that stays in).

## Natural sizes (PNG width x height / 2, fresh export on the final tree)

push-to-image 1780 x 933, image-to-rollout 2205 x 960, inbound-packet-path 2148 x 665, ingress-to-pod 1346 x 884, nightly-backup 1344 x 920. The other Phase 2 views and their sizes are in the standard's Phase 2 table; every value there was re-read from this export and matched. Recorded without a verdict, against no bound.

## Phase-gate outputs (final tree, after the self-test fix)

| Gate | Result |
|---|---|
| `node docs/architecture/sync.mjs check` | exit 0, `OK (56 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)` |
| `fish docs/architecture/drill.fish \| cmp - docs/architecture/drill-output.txt` | byte-identical (stderr discarded) |
| `python3 scripts/pre-commit-likec4-selftest.py` | 16/16 passed |
| physical views with `[...]` edge labels | `delivery,monitoring,production 0` |
| Phase 1 drill exit codes against `exits-p1.txt` | 0 differing lines |
| leak `rg` over `docs/architecture/model` | silent (exit 1, no match) |
| acceptance greps | `excluded, as in`, `no LikeC4 counterpart yet`, `internal calls`: 0 each; Physical row names `delivery`, `monitoring`, `production`; Scenarios row names `activity-log-row`; `external` 2, `provider` 1, `note` 4 mentions; WR-03 row matches `fixed in 02-01` |
| plan-specific greps | no `secrets.` in the evidence file; no age recipient, bucket host or IP in evidence or model; every `->` or `<-` step line ends in a quoted title |

`docs/architecture/out` was trashed after the gate. The hook's pre-commit run on the two Task 3 files and the fix passed (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocker] The hook self-test failed 3 of 16 cases (13/16), blocking the phase gate**
- **Found during:** Task 3 phase gate (previous executor), reproduced by the orchestrator and again here (13/16 on the committed tree before the fix).
- **Issue:** `scripts/pre-commit-likec4-selftest.py` copied only `src/main`, `k8s` and `docs/architecture` into each scratch repo. The `ci`, `dockerhub` and `hostfilter` elements added in Phase 2 cite `.github/workflows/deploy.yml` and `infra/vm/k3s-host-firewall.sh` as `source`, so the check's source-file rule refused the scratch repos for test_06 and test_07. test_08's injected element had a `workload` but no deployment instance, so it tripped the 02-01 workload-instance rule.
- **Fix:** `tracked_files()` now lists `.github/workflows` and `infra/vm` as well (one `TRACKED_PATHS` constant with a comment). That alone gave 15/16. test_08 then failed on two further rules the same fixture meets: placement (the manifest named no namespace) and view membership (an instance with no relationship is not drawn). Its manifest now sets `namespace: kanban-prod`, and its injected element has a relationship to `postgres` and an `instanceOf` in `netcup.k3s.prod`, so the `production` view draws it. No assertion was weakened and no case removed; test_08 still asserts that a deployment with a modelled workload passes the check.
- **Direction checked:** the self-test is the regression test. Unfixed script: 13/16 (test_06, test_07, test_08 fail), run on the committed tree before the edit. With only the pathspec change: 15/16 (test_08 fails on placement and view membership). Final script: 16/16. I did not run the fixed script against an older, pre-Phase-2 model, so "fails without the fix" rests on the 13/16 run, not on a bisect.
- **Files modified:** scripts/pre-commit-likec4-selftest.py (outside the plan's `files_modified`; authorized by the orchestrator).
- **Commit:** 110ed32. The CI job `likec4-model-drift` runs this script. I did not check CI.

### Observations left unchanged

- The standard's "Outside this change" section still says "migrating any of the seven Mermaid diagrams", while `docs/diagrams` holds 11 `.mmd` files (3 physical, 1 process, 7 scenarios). The plan did not ask for that sentence to change and I did not verify which seven were meant, so it is left as it was.

## Same narrow pathspec elsewhere (report only, not widened)

- `.githooks/pre-commit` lines 207 to 210: the staged-path trigger for the LikeC4 drift step covers `src/main/resources/db/migration`, `k8s`, `docs/architecture` and `KafkaTopics.java`. It does not cover `.github/workflows` or `infra/vm`, which `ci`, `dockerhub` and `hostfilter` now cite as sources. A commit that deletes or renames `deploy.yml` or the firewall script therefore passes the hook and is caught only by CI's `likec4-model-drift` job. The hook's "Known holes" comment names the same gap for `src/main/java` sources but not for these two paths. Not changed here.
- `scripts/verify-comments.py`, `verify-diagrams.py` and `verify-instruction-budget.py` call `git ls-files` over the whole repo, so they have no such pathspec.

## Open items stated plainly

- The 401 claim stays authored and unmeasured: `02-04-401-measure.txt` says `VERDICT: not measured` and the shared `datastore-postgres` was not stopped. The standard (note (b2)) and the `error-401` row say so. ROADMAP Phase 2 criterion 3 holds as written (no SQL step) but is not measured.
- Host-level steps (provider firewall, ServiceLB forward) rest on INFRA_ARCHITECTURE.md claims and are marked authored.
- CI was not checked for this plan or for the self-test fix.
- WR-04 stays open by the plan's instruction.

## Known Stubs

None.

## Threat Flags

None. The views and evidence add no endpoint, auth path or schema. T-02-12 held (the greps above are silent and gitleaks passed on each commit), T-02-13 held (the stale sentences are gone and the unchecked elements are listed by name).

## Self-Check: PASSED

- scenarios-platform.c4 and this file's siblings exist; commits 94b04dc, e9194a7, 110ed32 and 717629c are ancestors of HEAD.
- `git status --short -- docs/DIAGRAM_CONVENTIONS.md docs/architecture scripts` prints nothing; `docs/architecture/out` is gone.
