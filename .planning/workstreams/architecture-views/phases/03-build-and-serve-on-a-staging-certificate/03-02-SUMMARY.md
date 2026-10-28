---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 02
subsystem: infra
tags: [github-actions, docker-hub, environments, deployment-branch-policy, likec4, ci]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: plan 01's architecture-site.yml, site Dockerfile and invariant I12
provides:
  - production and staging deploy only from main (custom branch policy `main`), read back and shown refusing a branch run
  - the first site image main-1-5d44dac in the public rudenkovladimir/kanban-board-backend-architecture repository, probed after a pull
  - the push procedure by explicit SHA, used once, with the commit classes recorded
  - red-then-green pull-request proof that a drifted model fails before publish, with publish skipped both times
  - 03-DECISIONS.txt holding Q1 to Q4 and Q7 for plans 03 to 07
affects: [03-03, 03-04, 03-05, 03-06, 03-07]

actuals:
  tokens: 3200
  tasks: 3
  commits: 5
plan_head_before: 4d8baa42788744386c4c3ab43d586e89abb808c8
plan_head_after: 8b56cfe1428f2d8b279d36ea3e08945abfa8151e

tech-stack:
  added: []
  patterns:
    - "environment deployment-branch policy as the fence for an environment secret, because a branch run executes the branch's own workflow text"
    - "push by `${SHA}:refs/heads/main` after classifying every commit in the range"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-DECISIONS.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-02-publish.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-02-pr-proof.txt
  modified: []

key-decisions:
  - "Q7 1 applied: production and staging restricted to the branch `main` through gh api before the first push"
  - "The green proof restores the model line and adds one comment line, because a pure revert leaves the pull request's net diff empty and starts no path-filtered run"
  - "The Flux merge beneath the push was a --no-ff merge (5d44dac), keeping every cited SHA valid"

requirements-completed: [PUB-01]

coverage:
  - id: D1
    description: "Production and staging deployment-branch policy is exactly `main` and a branch's own workflow naming either environment is refused before a step runs"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-02-publish.txt ENV_POLICY lines; 03-02-pr-proof.txt ENV_PROBE lines (run 40767150747, both token_step=absent)"
        status: pass
    human_judgment: false
  - id: D2
    description: "origin/main builds and publishes main-1-5d44dac; the published image serves / with 200 and the likec4-root marker"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-02-publish.txt RUN architecture-site 40765087077, TAG, DIGEST and PUBLISHED PROBE lines; Docker Hub tags API lists the tag"
        status: pass
    human_judgment: false
  - id: D3
    description: "A drifted model fails the pull-request build at `Verify the model matches the code` with publish skipped, and the restored model passes with publish skipped"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-02-pr-proof.txt RED run 40766223979 and GREEN run 40766992838"
        status: pass
    human_judgment: false
  - id: D4
    description: "A Flux bump commit starts no architecture-site run"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-02-publish.txt CONTROL flux-bump 673238f (one bump observed, witness Invariant Checks)"
        status: pass
    human_judgment: false

duration: 35min
completed: 2026-10-28
status: complete
---

# Phase 3 Plan 02: Publish the first site image and fence the registry token Summary

**Production and staging now deploy only from main, origin/main published `main-1-5d44dac` through the new workflow, and a drifted model was shown failing the pull-request build before publish while a branch's own workflow was refused both environments.**

## Performance

- **Duration:** about 35 min for this continuation (Task 1's earlier stop is not counted)
- **Completed:** 2026-10-28
- **Tasks:** 3 of 3
- **Files modified:** 3 records created, 0 source files on main

## Accomplishments

- Wrote the five DECISION lines before anything left the machine (7e0b5b4): Q1 1, Q2 1, Q3 1, Q4 1 (from "go on with recommended"), Q7 1.
- Fenced the registry token: both environments read `null` with 0 rules before, and `protected_branches=false custom=true branches=main:branch` after.
- Pushed `5d44dac9ec0b7a060b0ff217ffb874cec90a8fa1` (99 commits: 98 authorized by Q1 plus the Flux merge) by explicit SHA. Architecture Site 40765087077, deploy 40765086761, Invariant Checks 40765086784, Secret Scan 40765086751 and Dependency Submission 40765086902 all concluded success, so the environment-gated deploy jobs passed on main under the new policy.
- Docker Hub lists `main-1-5d44dac`, digest `sha256:8a529ac02b862bb21d3a95648a963295de4b0da909b8e2a3ddb1875f18b6b5ef`; the pulled image answered `/` 200 with the marker and `/healthz` ok.
- Drift pull request #21: RED run 40766223979 (build failed at `Verify the model matches the code`, publish skipped; invariant-checks' LikeC4 job also failed), GREEN run 40766992838 (build success, publish skipped). Probe run 40767150747: `Branch "env-policy-probe" is not allowed to deploy to production due to environment protection rules.` for both, no steps started.
- Flux bump 673238f (observed after deploy.yml finished) has an Invariant Checks run and no Architecture Site run.

## Task Commits

1. **Task 1: decisions** - `7e0b5b4` (docs)
2. **Task 2: fence, push, publish record** - the push itself is `5d44dac` (the Flux merge); record in `cac105d` (docs)
3. **Task 3: pull-request proof record** - `8b56cfe` (docs)

The three throwaway-branch commits (drift 3eb592f, revert 9a985ee, restore 8ebbe93) and the probe commit 0d04755 were never on main; their branches are deleted locally and on origin and PR #21 is closed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] zsh read `$PUSH_SHA:r` as a modifier**
- **Found during:** Task 2, first push
- **Issue:** `git push origin "$PUSH_SHA:refs/heads/main"` became `...efs/heads/main`; git refused locally with "src refspec ... does not match any". Nothing reached origin.
- **Fix:** retried with `${PUSH_SHA}:refs/heads/main`. Plans 03, 06 and 07 reuse this push procedure verbatim and run under zsh, so they need the braces.
- **Files modified:** none

**2. [Plan expectation wrong] `run_number` is 1**
- The plan said the run number "will not be 1". `github.run_number` counts per workflow and architecture-site.yml had never run, so the tag is `main-1-5d44dac`. The ImagePolicy only needs the number monotonic.

**3. [Plan expectation wrong] the green step by pure revert starts no run**
- **Found during:** Task 3. After `git revert --no-edit` and a push, `gh pr view 21` reported `files=0`, Secret Scan (no paths filter) ran, and Architecture Site and Invariant Checks did not. The cause is inferred from `files=0`, not confirmed in GitHub's documentation: both path-filtered workflows appear to judge a pull_request by its net diff against the base.
- **Decision:** stopped and reported; the orchestrator chose restoring the model line plus a one-line comment in `docs/architecture/model/delivery.c4` (8ebbe93). Before pushing, `node docs/architecture/sync.mjs check` and `scripts/verify-comments.py check` both printed OK. That run's net diff touched the filtered path and the workflow ran. The workflow was not changed.
- **Consequence for later plans:** a drill that reverts a pull request's only change cannot observe a path-filtered workflow.

**4. [Note] the remote and `gh` use two repository names**
- `git remote` says `RudVlad473/task-orchestration-backend` and the plan commands say `RudVlad473/kanban-board-backend`. `gh repo view` returns the same ID (`R_kgDONi5k2g`) for both, so the fence and the push are on one repository.

## Authentication gates

None.

## Known Stubs

None.

## Threat Flags

None. The only new surface is the environment branch policy, which is the plan's own mitigation (T-03-31).

## Open items carried forward

- Local main is one commit behind origin/main (Flux bump 673238f); the next push procedure merges it.
- The probe run and drift pull-request runs stay in the Actions history; they hold no secret value.
- Two critical Dependabot alerts were reported by GitHub on the push; they are outside this plan.

## Self-Check: PASSED

Verified: the three record files exist and `TAG: main-1-5d44dac` matches the tag regex; `5d44dac`, `7e0b5b4`, `cac105d` and `8b56cfe` are ancestors of HEAD; the Docker Hub tags API lists `main-1-5d44dac`; `ls .github/workflows/env-policy-probe.yml` fails on main; `git ls-remote --heads origin site-drift-proof env-policy-probe` prints nothing; PR #21 reads CLOSED; the records hold no token-shaped string.
