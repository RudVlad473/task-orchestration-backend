---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 05
subsystem: docs-tooling
tags: [drills, sync-check, workload, host, placement, architecture-site]
status: complete

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: plan 04's site manifests (Deployment architecture-site, IngressRoute host, overlay namespace) and model element archsite
provides:
  - drills D25, D26 and D27 as rows of docs/architecture/drills.tsv, run after D24 by the unchanged run_drills
  - drill-output.txt regenerated against the committed tree (62 elements, 18 workloads), with D25 to D27 sections
  - a shown no-regression: every earlier drill's heading and exit code is unchanged by plan 04's site commit
affects: [03-06, 03-07]

actuals:
  tokens: 2600
  tasks: 2
  commits: 2
plan_head_before: 03ca15a96985381fe85b0b1489a475e10e051c89
plan_head_after: 0728e518deb4764289e15e07f7eb8d7013daaee1

tech-stack:
  added: []
  patterns:
    - "a new drill is one tab-separated sed row in drills.tsv; drill.fish holds no per-drill code"

key-files:
  created: []
  modified:
    - docs/architecture/drills.tsv
    - docs/architecture/drill-output.txt

key-decisions:
  - "drill.fish was not edited: the three drills are data rows read by run_drills"
  - "Both commits used --no-verify, which CLAUDE.md permits for docs and config-only commits; sync.mjs check, verify-comments.py check and the likec4 hook selftest were run by hand instead"

requirements-completed: []  # SCEN-08 is the publish scenario view (plan 07); PUB-02 stays Pending until plan 06

coverage:
  - id: D1
    description: "The workload check catches the site Deployment renamed on a scratch copy"
    requirement: "SCEN-08"
    verification:
      - kind: integration
        ref: "drill-output.txt section D25: exit=1 with two problem lines naming Deployment/architecture-sitex and Deployment/architecture-site"
        status: pass
    human_judgment: false
  - id: D2
    description: "The host check catches the architecture host changed in its IngressRoute"
    requirement: "SCEN-08"
    verification:
      - kind: integration
        ref: "drill-output.txt section D26: exit=1 with the routed-but-unclaimed and claimed-but-unrouted host pair"
        status: pass
    human_judgment: false
  - id: D3
    description: "The placement check catches the site overlay moved to another namespace"
    requirement: "SCEN-08"
    verification:
      - kind: integration
        ref: "drill-output.txt section D27: exit=1 with a placement line naming archsite"
        status: pass
    human_judgment: false
---

# Phase 3 Plan 05: Drills D25 to D27 Summary

Three sed rows in drills.tsv show that the existing workload, host and placement rules each catch a change to the architecture site's own fact, and a fresh replay shows no earlier drill's outcome moved.

## State detection

The Expected state held and no STOP bullet fired. Measured at execution time on HEAD 03ca15a:

- `git status --short` on drill.fish, drills.tsv and drill-output.txt printed nothing, and `git diff --cached --name-only` printed nothing.
- `git ls-files` listed drills.tsv and not sed-drills.tsv; `test -e docs/architecture/sed-drills.tsv` exited 1.
- `rg -c '^run_drills \$here/drills\.tsv$' drill.fish` printed 1 and `rg -c sed_drills drill.fish` printed nothing.
- The header was the five names drill, title, kind, file, value; the last data row was D24 and no D25 to D27 row existed; no row lacked five fields.
- drill.fish and drills.tsv were last touched by 0397d50. drill-output.txt was last touched by 1d6c40e, an ancestor of plan 04's commit 8e531d0.
- `node docs/architecture/sync.mjs check` exited 0 (62 elements, 18 workloads) and `python3 scripts/verify-comments.py check` exited 0 (340 files, 1307 comment blocks).

## Commits

| Task | Commit | Paths |
|---|---|---|
| 1 (tracer): D25 | 5377803 | docs/architecture/drills.tsv, docs/architecture/drill-output.txt |
| 2: D26 and D27 | 0728e51 | docs/architecture/drills.tsv, docs/architecture/drill-output.txt |

Each commit holds exactly those two paths (`git show --name-only` checked). Nothing was pushed: `git merge-base --is-ancestor HEAD origin/main` exits 1 after `git fetch origin`.

## Recorded sections (verbatim from drill-output.txt)

```
== D25: the site Deployment is renamed (architecture-site)
  exit=1
  DRIFT (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads):
  - k8s Deployment/architecture-sitex has no element carrying metadata workload 'Deployment/architecture-sitex'
  - model claims workload Deployment/architecture-site but k8s/ has no such manifest
  package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service
== D26: the architecture host is changed in its IngressRoute
  exit=1
  DRIFT (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads):
  - k8s IngressRoute routes host kanban-board-rud-vlad-473-architecturex.duckdns.org, which no element claims in metadata host
  - model claims host kanban-board-rud-vlad-473-architecture.duckdns.org but no IngressRoute routes it
  package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service
== D27: the site overlay's namespace changes (architecture to kanban-nonprod)
  exit=1
  DRIFT (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads):
  - placement: archsite is deployed in [architecture] in the model, manifests place it in [kanban-nonprod]
  package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service
```

## Checks

- Table check (`awk -F'\t' 'NR>1 && NF!=5'`): prints nothing, before and after each append.
- Header check (table's `== id: title` lines with repeats collapsed against drill-output.txt's `== ` headers): 0 differing lines after both regenerations.
- Exit-code comparison against the file as committed before plan 04 (72 `== ` and `exit=` lines saved): 0 removed or changed lines after both regenerations, so only the new sections were added.
- The only other lines that differ in drill-output.txt are the `OK (...)` and `DRIFT (...)` counts (61 to 62 elements, 17 to 18 workloads, and 62 to 63 in the two drills that add an element), which plan 04 changed. This is the stale-count effect the plan expected.
- `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` exits 0 after each commit.
- `python3 scripts/pre-commit-likec4-selftest.py`: 16/16 passed, exit 0.
- `git status --short -- k8s docs/architecture/model` printed nothing after each replay (the drills edited only the scratch copy). `git status --short -- k8s docs/architecture` prints nothing after the final commit.
- docs/architecture/out/exits-before-03-05.txt was removed with trash-put (the file only); out/ is gitignored.

## Deviations from Plan

None - plan executed exactly as written.

Two procedural notes, neither a deviation: the pre-commit hook guard that refuses a protected branch was not applied because the dispatch directs a sequential executor on main and the plan permits `--no-verify`; `state.advance-plan` was not applied because it refused with `plan_position_unreadable` (STATE.md's Current Position reads `Plan: Not started`, a shape plans 03-01 to 03-04 also left as it was); `state.record-metric`, `state.record-session` and `roadmap.update-plan-progress` ran, and `state.update-progress` reported itself skipped for an unscoped phase; and several single-line Bash checks were split into separate calls because a local hook rejects dense command lines.

## Known Stubs

None.

## Threat Flags

None. No file outside docs/architecture/drills.tsv and docs/architecture/drill-output.txt was edited, and no network endpoint, auth path or schema was added.

## Notes for plan 06 and plan 07

- D25 to D27 apply rules that already existed, so they have no earlier script to be red against (T-03-30). The red that shows the checks seeing the site is plan 04's 03-04-model-red.txt.
- The committed drill-output.txt now matches a fresh replay. Plan 06's Q5 push carries 5377803 and 0728e51 along with plan 04's commit.
- PUB-02 and SCEN-08 stay Pending in REQUIREMENTS.md.

## Self-Check: PASSED

- FOUND: docs/architecture/drills.tsv with rows D25, D26, D27 (one each, five fields)
- FOUND: docs/architecture/drill-output.txt with `== D25:` to `== D27:` as the last three headers
- FOUND: 5377803 and 0728e51 are ancestors of HEAD
