---
gsd_state_version: "1.0"
milestone: v1.5
milestone_name: Architecture Views
current_phase: 4
current_phase_name: Leak Review and Production Go-Live
current_plan: Not started
status: planning
stopped_at: Phase 4 context gathered
last_updated: "2026-10-29T14:26:24.390Z"
last_activity: 2026-10-29
last_activity_desc: Phase 3.1 complete, transitioned to Phase 4
state_head: 568bf818f1466ce84caf23f1cb2d2bf2c6e00370
progress:
  total_phases: 5
  completed_phases: 3
  total_plans: 19
  completed_plans: 19
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-30; v1.5 is not yet recorded there)
Workstream files: `.planning/workstreams/architecture-views/` (REQUIREMENTS.md, ROADMAP.md, research/)

**Core value:** A reader of the public repository can see how the system is built from a model that cannot drift from the code, not from screenshots.
**Current focus:** Phase 3.1 — Model description pass (INSERTED)

## Current Position

Phase: 4 — Leak Review and Production Go-Live
Current Plan: Not started
Total Plans in Phase: 3
Status: Ready to plan
Last activity: 2026-10-29 — Phase 3.1 complete, transitioned to Phase 4

Progress: [████████░░] 75%

## Performance Metrics

**Velocity:**
- Total plans completed: 19
- Average duration: -
- Total execution time: -

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 3 | - | - |
| 02 | 5 | - | - |
| 03 | 8 | - | - |
| 3.1 | 3 | - | - |

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
| Phase 03 P07 | 23min | 3 tasks | 7 files |
| Phase 03 P08 | 10 min | 2 tasks | 4 files |
| Phase 03.1 P01 | 25 min | 2 tasks | 9 files |
| Phase 03.1 P02 | 25 min | 3 tasks | 10 files |
| Phase 03.1 P03 | 65 min | 2 tasks | 8 files |

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
- [Phase 03]: 03-07: Q6 1 authorized the flush push and the model-only push, both made by explicit SHA; the src-only push stays pending under Q4 1
- [Phase 03]: 03-08: step 4 and the view description say 'registry token', never the secret identifier — Keeps Secret names out of the public model; Phase 4 leak scan lists them as targets
- [Phase 03]: 03-08: no standing regression test for the model prose fix — No gate reads prose; a sync.mjs rule would hold only workflow-name lists and key on wording. Guard would need a sync.mjs rule reading push: true, a drill row, regenerated drill-output.txt and a hook selftest case
- [Phase 03.1]: Definition-form noun phrases pass the description sentence rule; noun lists, colon lists and semicolon-joined clauses are defects
- [Phase 03.1]: Generated elements get description text from the generator (extend cannot carry one at likec4 1.60.0)
- [Phase 03.1]: VIEW_FOLDERS repeats the views block labels and is kept in step by hand; sync.mjs check fails until they agree
- [Phase 03.1]: Relationships stay label-only (D-02): held at phase end by a verify gate, not a sync.mjs rule, since D-05 limits the script to presence and final period
- [Phase 03.1]: The drill before-record runs the BASE script on the final model with generated.c4 re-rendered by it, so the old script does not call the file stale

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
| 261029-kyd | Split the 14 scenario views into 5a Application behaviour and 5b Platform and delivery, enforced by checkViewFolders | 2026-10-29 | 74106f2 | [261029-kyd-group-the-14-scenario-views-into-two-fol](./quick/261029-kyd-group-the-14-scenario-views-into-two-fol/) |

### Roadmap Evolution

- Phase 03.1 inserted after Phase 3: Model description pass: audit descriptions for completeness, standard wording and grouping before the Phase 4 leak review

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-10-29T14:26:23.358Z
Stopped at: Phase 4 context gathered
Resume file: .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-CONTEXT.md
