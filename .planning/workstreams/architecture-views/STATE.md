---
gsd_state_version: "1.0"
milestone: v1.5
milestone_name: Architecture Views
current_phase: 3
current_phase_name: Build and Serve on a Staging Certificate
status: planning
stopped_at: Completed 03-06-PLAN.md
last_updated: "2026-10-28T21:38:56.567Z"
last_activity: 2026-10-28
last_activity_desc: Phase 02 complete, transitioned to Phase 3
state_head: c789fed96d7f75a5817da6aa5090f9da6c0905c7
progress:
  total_phases: 4
  completed_phases: 2
  total_plans: 15
  completed_plans: 14
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-30; v1.5 is not yet recorded there)
Workstream files: `.planning/workstreams/architecture-views/` (REQUIREMENTS.md, ROADMAP.md, research/)

**Core value:** A reader of the public repository can see how the system is built from a model that cannot drift from the code, not from screenshots.
**Current focus:** Phase 02 — Model Completion

## Current Position

Phase: 3 — Build and Serve on a Staging Certificate
Plan: Not started
Status: Ready to plan
Last activity: 2026-10-28 — Phase 02 complete, transitioned to Phase 3

Progress: [█████░░░░░] 50%

## Performance Metrics

**Velocity:**
- Total plans completed: 8
- Average duration: -
- Total execution time: -

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 3 | - | - |
| 02 | 5 | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 25min | 3 tasks | 8 files |
| Phase 01 P02 | 30 min | 3 tasks | 4 files |
| Phase 01 P03 | 40min | 3 tasks | 10 files |
| Phase 02 P01 | n/a | 3 tasks | 8 files |
| Phase 02 P02 | not measured | 3 tasks | 9 files |
| Phase 02 P03 | not measured | 3 tasks | 9 files |
| Phase 02 P04 | not measured | 3 tasks | 3 files |
| Phase 02 P05 | not measured | 3 tasks | 5 files |
| Phase 03 P01 | 8 min | 2 tasks | 9 files |
| Phase 03 P02 | 35 min | 3 tasks | 3 files |
| Phase 03 P03 | 70min | 2 tasks | 4 files |
| Phase 03 P04 | 40min | 2 tasks | 17 files |
| Phase 03 P05 | 12min | 2 tasks | 2 files |
| Phase 03 P06 | 25min | 3 tasks | 2 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Phase numbering restarts at 1 in this workstream. Order is foundations and gates, model completion, build and serve on staging, then leak review and the production flip. Nothing is public on a production certificate before the leak review.
- [Roadmap]: SCEN-08 is in Phase 3 so the publish scenario is drawn as built. GATE-01 is in Phase 2 so each reader lands with the elements it forces. PUB-03, PUB-04 and PUB-06 are in Phase 4 because each completes only after the production flip.
- [Scope]: Mermaid migration and the retirement of `docs/diagrams/` are out of scope. Docker Hub tag cleanup for the site repository is deferred to v2 (V2-05).
- [Defaults accepted]: Monitoring, cert-manager and Flux appear in the Physical view. The error-status scenario shows client-visible discriminators only. URLs are history-style. `robots.txt` keeps `Disallow: /`.
- [Phase 01]: Accepted the pnpm yaml peer re-key in the lockfile (option A); verified by suffix-only pairing instead of zero deletions
- [Phase 01]: Placement: HelmRelease lands in spec.targetNamespace, Flux source objects land nowhere, outer namespaced kustomization wins over a nested one
- [Phase 01]: Hook trigger is the single pathspec k8s so overlays and monitoring typos are refused locally (A2)
- [Phase 01]: K8S_DIRS guard exits 4 for any of six missing k8s trees (A3)
- [Phase 02]: production include uses per-namespace .** plus explicit kube-system: the .* form drew relationship-less instances and silenced drill D13
- [Phase 02]: 02-02: encryption in the backup flow is the label of the edge to the bucket, not its own node
- [Phase 02]: Flux controllers: read the single file gotk-components.yaml, not the directory, so a rename is not masked by the patch file
- [Phase 02]: 02-04: error-401 follows the not-measured branch (port 5433 held by another project Postgres); no SQL step, claim stated as authored and unmeasured
- [Phase 02]: Hook self-test scratch repos copy .github/workflows and infra/vm; test_08's injected element is placed and drawn instead of relaxing the workload-instance rule
- [Phase 03]: I12 scope statement lives in its docstring entry: the comment lint caps prose before the first Known holes marker at 8 lines
- [Phase 03]: 03-02: the green proof restores the model line and adds a comment, because a pure revert leaves the pull request's net diff empty and starts no path-filtered run
- [Phase 03]: 03-02: production and staging deploy only from main (Q7 1); push refspecs under zsh need ${SHA}:refs/heads/main, since $SHA:r is a modifier
- [Phase 03]: 03-03: adopt requests.memory=16Mi limits.memory=32Mi for the architecture site (lowest passing rung 16Mi, 8Mi OOMKilled on the VPS)
- [Phase 03]: 03-03: both ClusterIssuers set serviceType ClusterIP (Q2 1), applied live, live Certificates unchanged
- [Phase 03]: 03-04: the production view includes netcup.k3s.architecture explicitly, because LikeC4 collapses a single-instance namespace into its parent
- [Phase 03]: Plan 06: Q5 1 and Q6 1 authorized; the site went live on a staging certificate from one push of 8 commits (b1aa8c5), Flux bump fe3466b rolled main-4-b1aa8c5 with no site run

### Pending Todos

Shared project todos are in `.planning/todos/pending/`. None has been filed for this workstream yet.

### Blockers/Concerns

- [Phase 3]: Open Question 1 in ROADMAP.md. PUB-06 wants history URLs and no 200 HTML for a missing asset, but SWS 2.44.1 with `page-fallback` was measured returning 200 HTML. Settle the server choice by measurement before Phase 3 is planned.
- [Phase 3]: Open Question 3. The DuckDNS record and a public Docker Hub repository are operator actions outside the repo, and both are needed before the first push and the Certificate commit.
- [Phase 2]: Open Question 5. A render spike on host-level elements is needed before SCEN-05 is planned.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 261027-r62 | Fix stale two-checks sentence in DIAGRAM_CONVENTIONS.md (phase 01 gap closure) | 2026-10-27 | 53c0415 | [261027-r62-fix-stale-two-checks-still-proposed-sent](./quick/261027-r62-fix-stale-two-checks-still-proposed-sent/) |
| 261028-m6k | Stop anonymous 401 requests from creating spring_session rows via request cache | 2026-10-28 | 17a94cb | [261028-m6k-stop-anonymous-401-requests-from-creatin](./quick/261028-m6k-stop-anonymous-401-requests-from-creatin/) |
| 261028-n5v | Close phase 02 verification gaps: error-401 measured-then-fixed, WR-04 WR-05 WR-06 WR-08, monitoring scope wording | 2026-10-28 | ee9f9be | [261028-n5v-close-phase-02-verification-gaps-error-4](./quick/261028-n5v-close-phase-02-verification-gaps-error-4/) |
| 261028-opl | Phase 02 final gap pass: draw kube-prometheus-stack scrape targets in monitoring view, error-401 observed after fix | 2026-10-28 | e7a3f91 | [261028-opl-phase-02-final-gap-pass-draw-kube-promet](./quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/) |

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-10-28T21:38:56.150Z
Stopped at: Completed 03-06-PLAN.md
Resume file: None
