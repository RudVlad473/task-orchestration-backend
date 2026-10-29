---
phase: 04-leak-review-and-production-go-live
plan: 03
subsystem: infra
tags: [leak-scan, ci, flux, github-actions, owner-review, approval-gate]

requires:
  - phase: 04-leak-review-and-production-go-live
    provides: leak-scan.mjs, its workflow steps and the drill (plans 01 and 02)
provides:
  - the scan and the rewritten model pushed to origin/main (c3c366a), CI green, tag main-8-c3c366a published
  - Flux rollout of main-8-c3c366a onto the staging site (overlay bump ea1855b)
  - the workflow step seen red on a planted IP and green after the restore (draft PR 22, closed, branch deleted)
  - the owner's recorded approval of tag main-8-c3c366a (APPROVED_TAG) and of the secret-word target
affects: [04-04, 04-05]

plan_head_before: 9c596db0b273c6db88e145300d1821df1eb071d5
plan_head_after: 9354a94064455223028a919fff564b6923669c5d

actuals:
  tokens: 930
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Push main first, then prove the CI step on a draft pull request whose diff is the single planted line"
    - "The reviewed artifact is the live tag: APPROVED_TAG records tag, overlay newTag and pod image, which plan 04 compares before the flip"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-DECISIONS.txt
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-03-push.txt
  modified: []

key-decisions:
  - "Q1 1 push and prove, reply \"Q1 1\""
  - "Q2 approve of tag main-8-c3c366a, reply \"Q2 approve\", which also confirms the secret-word target kept (hits=0) and rules on the six excluded user-name forms"

requirements-completed: [PUB-05]

coverage:
  - id: D1
    description: "The scan step runs before the push step in the real workflow: green on main's build (13 targets at 0), red on a planted 10.20.30.40 with publish skipped, green after the restore"
    requirement: PUB-05
    verification:
      - kind: integration
        ref: "04-03-push.txt SITE RUN 40923112448, RED run=40925203236, GREEN run=40925366776, CLOSED pr=22"
        status: pass
    human_judgment: false
  - id: D2
    description: "The owner reviewed the CI scan report and the live staging site and approved tag main-8-c3c366a; the approval is recorded with the tag, overlay and pod values"
    requirement: PUB-05
    verification: []
    human_judgment: true
    rationale: "Only the owner can judge what is sensitive (D-06); no script can"

duration: about 25min of executor work (Tasks 1 and 2; owner wait times excluded; start time not recorded to the second)
completed: 2026-10-29
status: complete
---

# Phase 4 Plan 03: Push, CI proof and owner approval Summary

**The leak scan and rewritten model are on origin/main, CI published and Flux rolled the scanned tag `main-8-c3c366a` onto staging, the scan step was seen red then green on a draft pull request, and the owner approved that exact tag.**

## Performance

- **Duration:** about 25 min of executor work (start time was not recorded; estimated from the first fetch to the push record commit). Waiting for the owner is excluded.
- **Completed:** 2026-10-29
- **Tasks:** 3 (Tasks 1 and 2 by the executor, Task 3 by the orchestrator relaying the owner)
- **Files:** 2 created (`04-DECISIONS.txt`, `04-03-push.txt`)

## Decisions, verbatim

- Q1: `DECISION Q1: 1 push and prove; user reply: "Q1 1"`
- Q2: `DECISION Q2: approve tag=main-8-c3c366a; user reply: "Q2 approve"`
- `DECISION secret-word kept hits=0; confirmed by Q2 approve`
- `APPROVED_TAG main-8-c3c366a overlay=main-8-c3c366a pod=main-8-c3c366a run=40923112448 at 2026-10-29T18:22:28Z`

## Accomplishments

- **Push.** `PUSHED c3c366a908afbb61a43eab100d2c26414cc8ad2c` at 17:37:31Z by explicit SHA, no force. The range was 16 commits (15 plus the step 2 merge), across `.githooks`, `.github`, `.planning` (19 paths), `docs` (8) and `scripts`. Classes: (a) the plan 01 and 02 commits `aaa549b 1301b50 1830879 06ca6f8 7695303` and their records `fdc3510 6b17f41 9c596db`; (b) planning-only `28c74bb fc88613 3a93134 4e2a7a1 e896b5a 04657f7 568bf81`; (c) the merge `c3c366a`, beneath which only fluxcdbot bumps `4582896` and `ab8bdf1` sat. Nothing fell in no class.
- **CI.** Architecture Site run 40923112448 succeeded with `Verify the leak scan catches each target` and `Scan the build for leaks` both successful and published `main-8-c3c366a`. Invariant Checks (40923112279, including the LikeC4 drift job), Secret Scan, Dependency Submission and deploy.yml (40923112196) all succeeded. The CI scan report listed 13 targets at hits=0, the four public hostnames, `ROBOTS disallow=yes` and `OK targets=13`. There was no CI-only hit.
- **Flux.** Bump `ea1855b` set the architecture overlay to `main-8-c3c366a`; the staging pod ran `rudenkovladimir/kanban-board-backend-architecture:main-8-c3c366a` with 0 restarts. deploy.yml also rolled kanban-prod and kanban-nonprod to `main-162-c3c366a` (bump `efeb3fd`), both 1/1. `OTHERS prod=200 nonprod=200 monitoring=302`.
- **Proof on draft PR 22.** Plant commit `6f45bb7`: run 40925203236, build failure at `Scan the build for leaks` (`TARGET ipv4 hits=7`, `LEAK targets=ipv4`), publish skipped. Restore commit `6a81aec` (line restored plus a comment line): run 40925366776, build success, publish skipped. PR 22 is closed, the branch is gone from origin (`git ls-remote` printed nothing).
- **Review (Task 3).** The orchestrator presented Q2, re-reading the overlay `newTag` on freshly fetched origin/main and comparing it with the SITE RUN tag; the pod image in `APPROVED_TAG` is my earlier read from the Flux rollout polling (17:4x Z), not a second read at approval time. The owner replied `Q2 approve` and the orchestrator recorded it in `9354a94`.

## Task Commits

1. **Task 1: record DECISION Q1** - `28c74bb` (docs)
2. **Task 2: push and push record** - merge `c3c366a`, push record `e81c839` (docs)
3. **Task 3: DECISION Q2, secret-word, APPROVED_TAG** - `9354a94` (docs, by the orchestrator)

The two proof commits `6f45bb7` and `6a81aec` live only on the deleted `leak-scan-proof` branch and are not part of main.

**Plan metadata:** committed after this file (docs: complete plan).

## Deviations from Plan

None to the plan's outcomes. Two small notes:

- My first push command failed before sending anything: zsh read `$S:refs` as a history modifier and the refspec was malformed. I re-ran it with `${S}:refs/heads/main`.
- `OTHERS` for prod and nonprod was read at `/api/healthz` (the `uptime-check.yml` URLs). `GET /` answers 404 on both when measured after the rollout; I did not compare that with a pre-push reading.

## Issues Encountered

- `gh pr close --delete-branch` also deleted the local `leak-scan-proof` branch and switched to main, so my later `git branch -D` found nothing. The result is the intended one.
- The tool hook refuses long and heredoc Bash commands; scratch scripts went to a `/tmp` directory that was removed with `trash-put`.

## Not verified

- What the owner looked at in the review: I did not see the Q2 exchange, only its recorded outcome.
- The pod image in `APPROVED_TAG` was not re-read at approval time. Plan 04 compares tag, overlay and pod again before the flip.
- Local main is 1 commit ahead of origin/main (the push record) plus these close-out commits, and 2 behind it (the Flux bumps `ea1855b`, `efeb3fd`); plan 04's push procedure takes those. Nothing was pushed after `c3c366a`.
- `actuals.tokens` counts only the two files created (about 3.7 KB); the proof commits on the deleted branch are excluded.

## Known Stubs

None.

## Threat Flags

None. The closed PR 22, its logs and the made-up address 10.20.30.40 stay public by design (T-04-12, accepted).

## User Setup Required

None.

## Next Phase Readiness

- Plan 04 may flip only `main-8-c3c366a`; any later site build voids the approval (D-07). Between this plan and the flip only `k8s/**`, `docs/*.md`, `README.md` and `uptime-check.yml` may change.

---
*Phase: 04-leak-review-and-production-go-live*
*Completed: 2026-10-29*

## Self-Check: PASSED

Checked: `04-DECISIONS.txt` and `04-03-push.txt` exist; commits `28c74bb`, `c3c366a`, `e81c839` and `9354a94` are ancestors of HEAD; `c3c366a` is an ancestor of origin/main; `git branch --list leak-scan-proof` prints nothing.
