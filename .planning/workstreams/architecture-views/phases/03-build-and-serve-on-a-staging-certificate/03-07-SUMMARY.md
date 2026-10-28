---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 07
subsystem: infra
tags: [likec4, scenarios, flux, github-actions, phase-gate, docs]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: plan 06's live site and records, plan 02's push procedure, plan 04's manifests and Flux wiring, DECISION Q6 1
provides:
  - dynamic view publish-the-site (5 participants, 8 titled steps), each step cited to a workflow or manifest line in 03-SCENARIO-EVIDENCE.md
  - a flush push of plan 06's planning records and a model-only push by explicit SHA, both observed end to end
  - the new view rendering on the live site after a second Flux bump that started no site run
  - DIAGRAM_CONVENTIONS, INFRA_ARCHITECTURE and INFRA_RUNBOOK describing the site
  - 03-07-gate.txt mapping ROADMAP Phase 3's five criteria to record lines
affects: [phase-04]

actuals:
  tokens: 8000
  tasks: 3
  commits: 6
plan_head_before: 3f86bb3107827dc2c4ef1b911dee105b2fd34843
plan_head_after: 503d4caefaec4af76717168a1752072958a0c209

tech-stack:
  added: []
  patterns:
    - "flush the planning-only commits first so a one-commit push has an empty range, then push the one commit by explicit SHA"
    - "waiting on Flux with `kubectl wait --for=jsonpath` on the ImagePolicy and the Deployment spec instead of a sleep loop"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-SCENARIO-EVIDENCE.md
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-07-model-only-push.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-07-gate.txt
  modified:
    - docs/architecture/model/scenarios-platform.c4
    - docs/DIAGRAM_CONVENTIONS.md
    - docs/INFRA_ARCHITECTURE.md
    - docs/INFRA_RUNBOOK.md

key-decisions:
  - "DECISION Q6 1 (03-DECISIONS.txt:7) authorized the flush push and the model-only push; both were made by explicit SHA and nothing else was pushed"
  - "PHASE STATUS leaves the src-only push pending: DECISION Q4 1 made none and origin/main holds no src-only commit"

requirements-completed: [SCEN-08, PUB-01]

coverage:
  - id: D1
    description: "publish-the-site exports with 5 participants and 8 titled steps, and 03-SCENARIO-EVIDENCE.md has 8 rows for them"
    requirement: "SCEN-08"
    verification:
      - kind: integration
        ref: "export counting check printed `publish-the-site scenarios 5 8`; the awk row count printed 8; every step line ends in a quote"
        status: pass
    human_judgment: false
  - id: D2
    description: "A push whose range was exactly the model file ran the site workflow and not deploy.yml, its Flux bump started no site run, and the new view renders live"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-07-model-only-push.txt lines 10 to 20: MODEL-ONLY COMMITS, MODEL-ONLY DIFF, SITE RUN 40774467920, DEPLOY RUNS 0, FLUX BUMP, ROLLED, CONTROL flux-bump, DEEPLINK svg_nodes=162"
        status: pass
    human_judgment: false
  - id: D3
    description: "The flush push of planning-only commits started neither workflow"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-07-model-only-push.txt:6 CONTROL flush push, witness Secret Scan run 40773919680"
        status: pass
    human_judgment: false
  - id: D4
    description: "All local gates exit 0 and each of ROADMAP criteria 1 to 5 is mapped to record lines"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-07-gate.txt: 8 GATE lines exit=0, 5 CRITERION blocks, 9 PAIR or AFTER-ONLY labels, one PHASE STATUS line"
        status: pass
    human_judgment: false

duration: 23min
completed: 2026-10-28
status: complete
---

# Phase 3 Plan 07: Draw the publish scenario, push it alone, close the phase Summary

**publish-the-site is drawn from the files as built with each of its eight steps cited, a push whose range was exactly that one model file rebuilt and rolled the site and rendered the new view live with no run for its Flux bump, and the phase gate maps every criterion to a record line with one clause, the src-only push, still pending.**

## Performance

- **Duration:** about 23 minutes
- **Completed:** 2026-10-28
- **Tasks:** 3 of 3
- **Files:** 3 records created, 4 files modified

## Commits

| Item | SHA |
|---|---|
| Flush (Flux merge on top of plan 06's two planning commits) | `82a5dee6c12202cbfac7f6efaa42768e2b08d207`, pushed, commits=3 |
| Commit A, the model view | `a26694fa4388b8e2736a3bf5117a5bc80178c21c`, pushed alone |
| Commit B, evidence and push record | `e38c028`, local |
| Docs commit | `65a40d4`, local |
| Gate commit | `503d4ca`, local |

`commits: 6` in the frontmatter is the measured count of `git rev-list ${plan_head_before}..HEAD`; it includes fluxcdbot's `fe3466b`, which the Flux merge brought in. The plan's own commits are 5.

## What happened

- **Flush:** local main was 2 ahead and 1 behind. Both local commits were planning-only (`c789fed` live record; `3f86bb3` summary, roadmap, state, requirements), the one incoming commit was fluxcdbot's bump, so the push procedure's merge applied and `82a5dee` was pushed by `${SHA}:refs/heads/main`. Secret Scan run 40773919680 witnessed it; architecture-site and deploy.yml both listed 0 runs. After a fetch, `origin/main..HEAD` read 0.
- **Scenario:** `dynamic view publish-the-site` was added after `image-to-rollout`. Export: `publish-the-site scenarios 5 8`; `sync.mjs check` exit 0; drill replay `cmp` exit 0; leak words printed nothing; no untitled step. Commit A went through the full hook.
- **Model-only push:** before the push the live deep link read `nav_status=200 svg_nodes=11`. That is not the "0 nodes or a non-200" the plan expected: nginx answers the app shell for any path. The page text did discriminate ("The diagram publish-the-site does not exist or contains errors"). The pre-push check read `MODEL-ONLY COMMITS: a26694f` and `MODEL-ONLY DIFF: docs/architecture/model/scenarios-platform.c4`; pushed at 21:44:44Z.
- **Observed after it:** Architecture Site run 40774467920 (number 5, push event, build and publish success) published `main-5-a26694f`; deploy.yml 0 runs with Invariant Checks run 40774467949 as witness; fluxcdbot committed `6a82dde` at 21:48:11Z (2.6 minutes after the tag, only the overlay file); the pod rolled at 21:49:40Z, 0 restarts; `6a82dde` has Invariant Checks run 40774959670 and 0 site runs; the cold deep link read `nav_status=200 svg_nodes=162` and its text began with step 1 of the new view.
- **Docs:** natural sizes re-measured. Changed: `production` 3813 x 2566 (was 3461 x 2566), `delivery` 2342 x 1573 (was 1862 x 1556), `push-to-image` 1789 x 933 (was 1780 x 933, its model unedited, cause not investigated), `index` 911 x 1204 (was 836 x 1204, not investigated); new row `publish-the-site` 2360 x 1401. Every other row matched. Container count read live on 2026-10-28: 38 containers across 8 namespaces, `architecture` x1, `kanban-data` x4 (was 1 in the Plan 13-09 snapshot, cause not investigated). The Mermaid gap sentence and the runbook hostname line were added; no IP was added.
- **Gate:** 8 `GATE ... exit=0` lines, 5 `CRITERION` blocks, 9 labels (5 PAIR, 4 AFTER-ONLY, listed below), `PHASE STATUS` line.

## PAIR and AFTER-ONLY

PAIR: c2-issuer, c2-marker, c3-deeplink-browser, c3-deeplink-curl, c1-model-only (red is the k8s-only push control, green the model-only SITE RUN). AFTER-ONLY: c2-other-hosts, c3-missing-asset (as the plan says), c3-fsread (the dev-server control read `same_as_file=no`, its body being the file plus a source-map comment) and c5-publish-view (DEEPLINK_BEFORE read 200 and 11 nodes, so node count and status do not discriminate).

## PHASE STATUS

`PHASE STATUS: criteria 1 to 5 mapped; pending: src-only push (criterion 1, DECISION Q4 1)`

Pending: **the src-only push.** No commit on origin/main since plan 02's push changes only `src/` paths (checked: 21 commits). Same-mechanism controls observed instead: the k8s-only push, three Flux bump controls and the flush push. None is the literal case.

## Deviations from Plan

**1. [Plan expectation wrong] DEEPLINK_BEFORE did not fail**
- The plan assumed the live site would show no SVG nodes for an unknown view. The shell renders 11 SVG nodes with a "does not exist" message at status 200, so c5-publish-view is AFTER-ONLY, which the plan allows ("else AFTER-ONLY quoting its values"). Recorded as measured.

**2. [Rule 3 - Blocking] a commit command with an empty message**
- Found during: commit B. A `git commit -F -` with stdin on /dev/null ran the hook (it passed) and then aborted on the empty message; nothing was committed. Retried with `-m` messages. The two files stayed staged in between and were committed by path.

**3. [Note] size drift on views the phase did not edit**
- `push-to-image` and `index` measured differently from the recorded sizes. They are updated and flagged in the conventions doc; the cause is not investigated.

## Authentication gates

None.

## Known Stubs

None.

## Threat Flags

None. T-03-26: the leak-words command printed nothing and the evidence holds no token-shaped string. T-03-28: the flush range held only planning commits and the Flux merge, and the model-only range was one commit and one path, compared before the push.

## Unpushed commits for the user

Local main is 3 ahead of origin/main and 1 behind: `e38c028`, `65a40d4`, `503d4ca` plus this summary and the state commits, with Flux's bump `6a82dde` (overlay file only) on origin. The next push procedure takes the Flux merge first. `e38c028` is a planning-only commit and `65a40d4` touches only `docs/` outside `docs/architecture/`, so neither starts a site or deploy run.

## Self-Check: PASSED

Verified: 03-SCENARIO-EVIDENCE.md, 03-07-model-only-push.txt and 03-07-gate.txt exist; `a26694f`, `82a5dee`, `e38c028`, `65a40d4` and `503d4ca` are ancestors of HEAD; `a26694f` and `82a5dee` are ancestors of origin/main; `git diff --name-only 82a5dee..a26694f` prints only the model file; Docker Hub lists `main-5-a26694f`; the gate verify greps print 8, 0, 5, 9, 0, 1, 1.
