---
phase: 02-model-completion
plan: 04
subsystem: architecture-views
tags: [likec4, dynamic-views, scenarios, evidence, 401-measurement]

requires:
  - phase: 02-model-completion
    provides: plan 02-01 participant cap and viewpoint tag rules checked by sync.mjs; plan 02-03 committed model the views draw on
provides:
  - scenarios-app.c4 with dynamic views signup-ok, error-401, error-403, error-409 and activity-log-row
  - 02-SCENARIO-EVIDENCE.md with one cited section per view and one row per step
  - 02-04-401-measure.txt recording that the cookie-less 401 SQL was not measured, with the reason
affects: [02-05]

actuals:
  tokens: 5537  # chars/4 over the three files written (22148 chars)
  tasks: 3
  commits: 3

key-files:
  created:
    - docs/architecture/model/scenarios-app.c4
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-04-401-measure.txt
  modified: []

key-decisions:
  - "error-401 follows the not-measured branch: no SQL step, and the description says the no-SQL claim is authored and unmeasured"
  - "error-409 draws only postgres.table_tasks; the column, board and user reads are named in the step title"
  - "activity-log-row opens with a kanban.api to kanban.publisher step titled for the committed task update, so client stays out of the 5-participant cap"

requirements-completed: [SCEN-01, SCEN-02, SCEN-06]

duration: not measured
completed: 2026-10-28
status: complete
plan_head_before: e2f2860b2ecee78b88a89ff24b5017121f9d88d5
plan_head_after: 226b45fd63d0663d6916bde6f598db92997c0002
---

# Phase 02 Plan 04: Signup, error-status and activity-log scenarios drawn, each step cited Summary

**Five dynamic views (signup-ok, error-401, error-403, error-409, activity-log-row) on existing elements, each step tied to a quoted file:line in the evidence file; the 401 view claims no SQL as authored and unmeasured, because the live measurement could not run.**

## Commits

| Task | Commit | What |
|---|---|---|
| 1 (tracer) | 62f5f17 | scenarios-app.c4 with signup-ok, 02-SCENARIO-EVIDENCE.md with its section |
| 2 | 6d358fd | 02-04-401-measure.txt, `VERDICT: not measured` |
| 3 | not reached | checkpoint applies only on VERDICT b or c; see below |
| 4 | 226b45f | error-401, error-403, error-409, activity-log-row and their evidence sections |

`commits: 3` is measured: `git rev-list --count e2f2860..HEAD` before this SUMMARY commit.

## The 401 measurement was not run

- Verdict: `VERDICT: not measured`. There is no `WRITES` line (it is written only on b or c) and no `DECISION` line.
- Reason: port 5433 was held by the `datastore-postgres` container (postgres:17, compose project `datastore`, working directory `/home/andre/dev/custom-ai-skills/shared/datastore`), not by this repo's compose Postgres. Task 2's precondition says to record `not measured` in that case and never stop a service the task did not start. Nothing was started, changed or stopped: no compose service, no bootRun, no statement logging, no request.
- What this means: whether a request with no session cookie creates a `spring_session` row, receives a `Set-Cookie`, or issues SQL is still unknown. Planning found a concrete way the "no SQL" claim could be false: the main chain uses `SessionCreationPolicy.IF_REQUIRED` (SecurityConfiguration.java:127) with no request-cache setting, and Spring Security's default request cache may save an unauthenticated request in a new session unless the client asks for JSON. That mechanism is training knowledge and was not checked against the Spring Security source.
- Task 3 checkpoint: `Task 3 checkpoint not reached: VERDICT not measured`. Criterion 3 of Phase 2 ("401 with no SQL step") holds as written: the view has no SQL step. Its description states that the absence is authored and unmeasured. ROADMAP.md is unchanged.
- To close the gap: free port 5433 (stop the datastore) or point the app at a Postgres on another port, then re-run Task 2's method. If the new verdict is b or c, the Task 3 decision applies and the error-401 view needs redrawing.

## Steps to evidence rows, per view

Exported with `likec4 export json --skip-layout`, counted per the plan; rows counted with the awk from the plan context.

| View | Participants | Steps | Evidence rows |
|---|---|---|---|
| signup-ok | 5 | 9 | 9 |
| error-401 | 3 | 4 | 4 |
| error-403 | 5 | 6 | 6 |
| error-409 | 4 | 5 | 5 |
| activity-log-row | 5 | 5 | 5 |

All five are tagged `scenarios`. `grep -c '^## view '` on the evidence file prints 5. `node docs/architecture/sync.mjs check` exits 0 (56 elements). Every step line ends in a quoted title (the plan's `rg -v` check prints nothing). A throwaway script checked that each backticked quote fragment in the evidence file is a substring of the files its row cites: 97 of 98 fragments matched; the one miss was the word `production` inside an authored row, not a quote. No cookie value or `DB_PASS=` is in either evidence file.

## Deviations from Plan

None - plan executed as written. The not-measured branch is the plan's own path for a held port 5433, so Task 2's record, Task 3's skip and Task 4's not-measured wording all follow it.

## Findings for the user

- No security-relevant finding was established: the cookie-less 401 behaviour is unmeasured, so the possible anonymous-session-row finding stays open as a question, not a result.
- Line cites in the evidence file are read from the code. The SQL statement order and counts in error-403, error-409 and signup-ok are not observed on the wire; the rows say so where it matters.
- Step 1 of `activity-log-row` stands in for the HTTP request (a sixth participant would break the cap), as the plan chose.

## Requirements

SCEN-01, SCEN-02 and SCEN-06 are marked complete. SCEN-02 is complete as drawn (client-visible discriminators for 401, 403 and 409, with the 401 view claiming no SQL only as authored); the unmeasured 401 SQL is carried above and in `.planning/WINDOWS.md` as an `unrun-verify` entry. Nothing is left unmarked.

## Known Stubs

None.

## Threat Flags

None. The views and evidence add no endpoint, auth path or schema; T-02-09 held (no cookie value or credential in any evidence file), T-02-10 held (the 401 view does not claim more than the record shows), T-02-11 held (the held port was left alone).

## Self-Check: PASSED

- scenarios-app.c4, 02-SCENARIO-EVIDENCE.md and 02-04-401-measure.txt exist; the commits 62f5f17, 6d358fd and 226b45f are ancestors of HEAD.
- `docs/architecture/out` was trashed after verification; port 8080 had no listener at any point; no container was started or left behind by this plan.
