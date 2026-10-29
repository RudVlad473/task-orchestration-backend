---
phase: 04-leak-review-and-production-go-live
plan: 02
subsystem: infra
tags: [leak-scan, drill, pre-commit, selftest, fish, likec4]

requires:
  - phase: 04-leak-review-and-production-go-live
    provides: leak-scan.mjs and its 81 fixture tests (plan 01)
provides:
  - drill kind `scan` and helper `scan_build` in docs/architecture/drill.fish
  - drills D30 (planted IP) and D30b (untouched control) and the regenerated drill-output.txt
  - a pre-commit step that runs the leak scan's fixture tests inside the LikeC4 block
  - self-test case 20, seen failing without the step and passing with it
affects: [04-03, 04-04, 04-05]

plan_head_before: 6b17f41b1d65be4917399166fc028601a36346bd
plan_head_after: 7695303a99bcb4241f7f1a466f11fceb8f846bd2

actuals:
  tokens: 2900
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "A drill row that builds the scratch model and records only the scan's exit and last line, so the record stays byte-stable"
    - "A hook step that holds a gate script's logic through its fixture tests, with the model-is-clean claim left to CI"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-02-proof.txt
  modified:
    - docs/architecture/drill.fish
    - docs/architecture/drills.tsv
    - docs/architecture/drill-output.txt
    - .githooks/pre-commit
    - scripts/pre-commit-likec4-selftest.py

key-decisions:
  - "The hook runs the scan's fixture tests, not a site build; the current model's cleanliness stays with CI's scan step (stated in the hook comment)"
  - "The drill prints the scan's exit and last line only, never file names or byte counts, because LikeC4 builds are not byte-reproducible"

requirements-completed: [PUB-05]

coverage:
  - id: D1
    description: "Drill D30 plants an IP literal in the acme description; the scan exits 1 naming ipv4 while sync exits 0; control D30b scans clean"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "fish docs/architecture/drill.fish; blocks == D30 and == D30b (control) in docs/architecture/drill-output.txt; 04-02-proof.txt DRILL lines"
        status: pass
    human_judgment: false
  - id: D2
    description: "The pre-commit hook refuses a commit that removes the ipv4 row of the scan; self-test case 20 proves the step is what refuses it"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "python3 scripts/pre-commit-likec4-selftest.py (20/20 passed); same script with --hook pointed at a copy without the step (19/20, only case 20 fails); 04-02-proof.txt SELFTEST lines"
        status: pass
    human_judgment: false

duration: about 30min
completed: 2026-10-29
status: complete
---

# Phase 4 Plan 02: Drill and hook case for the leak scan Summary

**A `scan` drill kind builds a scratch model with a planted IP and records the scan catching what sync.mjs misses, and the pre-commit hook now runs the scan's fixture tests, with self-test case 20 seen red without the step and green with it.**

## Performance

- **Duration:** about 30 min (start time was not recorded; estimated from the first edit to the last commit, most of it two hook runs and the drill run)
- **Completed:** 2026-10-29T16:14Z
- **Tasks:** 2
- **Files modified:** 6 (1 created, 5 modified)

## Accomplishments

- `drill.fish` has kind `scan` and `scan_build`, which builds `$scratch/model` with the installed likec4 (`--use-dot=false`), scans the output and prints the scan's exit and last line. A missing binary exits 3; a failed build prints its exit and the last five log lines.
- The regenerated `drill-output.txt` differs from the old one by 12 appended lines and zero removed lines (`git diff -U0` on the commit, `grep -c '^-[^-]'` printed 0). The block count (49) equals the distinct drill and title pairs in `drills.tsv`.
- The hook step runs `node --test docs/architecture/site/leak-scan.test.mjs < /dev/null` after the drift check and refuses with `The leak scan's fixture tests failed`.
- Case 20 removes the ipv4 row of `leak-scan.mjs` in a scratch repo and expects the refusal naming ipv4.

## Verbatim drill blocks (from drill-output.txt)

```
== D30: an IP literal in an element description (acme)
  scan exit=1
  LEAK targets=ipv4
  exit=0
  OK (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads)
  package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service
== D30b (control): the untouched copy is built and scanned
  scan exit=0
  OK targets=13
  exit=0
  OK (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads)
  package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service
```

## Red and green (04-02-proof.txt)

```
SELFTEST without-step exit=1 failed=test_20_a_weakened_leak_scan_is_refused
SELFTEST with-step exit=0 passed=20/20
DRILL D30 scan_exit=1 sync_exit=0
DRILL D30b scan_exit=0 sync_exit=0
```

Direction checked: case 20 was run against a hook copy with the new step cut out (it fails: the weakened scan was committed, 19/20) and then against the real hook (it passes, 20/20). The drill's red direction is D30 itself: sync exit 0 with the plant, scan exit 1.

## Timings

- **Drill run:** the full `fish docs/architecture/drill.fish` took 1:49.6 wall (115 s user). I did not time a run before the change, so the added time is not measured separately; it holds two builds (D30 and D30b), against the plan's estimate of about a minute.
- **Hook step:** `node --test docs/architecture/site/leak-scan.test.mjs` alone takes 3.95 s wall (81 tests, 3.9 s reported), not the "about a second" the plan and the hook comment say. The hook comment I wrote avoids the number; the plan text is the only place it is wrong.
- **Not timed on one commit:** the Task 2 commit touched no LikeC4 trigger path (`.githooks`, `scripts` and `.planning` are not triggers), so the new step did not run in that commit's hook. The step ran for real only inside the 20 self-test cases.

## Task Commits

1. **Task 1: scan drill kind, D30 and D30b, regenerated record** - `06ca6f8` (test)
2. **Task 2: hook step, self-test case 20, proof file** - `7695303` (test)

**Plan metadata:** committed after this file (docs: complete plan).

## Decisions Made

- Follows the plan; the two choices are in the frontmatter. In `drills.tsv` the sed expression is `s|\(checks each hostname over HTTP\)\.'$|\1 from 10.20.30.40.'|`, which matched exactly one line of `edge.c4` (`grep -c` printed 1).

## Deviations from Plan

None - plan executed exactly as written.

Two small notes, neither a deviation: the docstring counts of the self-test were corrected to the real numbers (thirteen drift refusals, three passes, two missing-tooling refusals, one leak-scan refusal, one unrelated path; 20 in all), and the hook's falsifier comment now names case 20 as well.

## Issues Encountered

- The tool hook refuses long Bash commands; a commit with the full message inline was split into a message file under `out/` (gitignored, removed with `trash-put`) and `git commit -F`.

## Not verified

- No CI run: nothing was pushed. The architecture-site workflow's own run of the fixture tests and the self-test in invariant-checks are unverified here.
- The new hook step has not yet run in a real commit that triggers the LikeC4 block. The self-test covers it in scratch repos; the first model, k8s or `docs/architecture` commit will run it for real (about 4 s added).
- The drill's baseline time was not measured, so "added time" is an upper bound (the whole run) and not a difference.
- `--no-verify` bypasses the hook step; CI is the backstop (threat T-04-07).

## Known Stubs

None.

## Threat Flags

None. No new network, auth or file-access surface; the drill reads and builds a scratch copy under the gitignored `docs/architecture/out/`, which the harness removes (`ls docs/architecture/out` printed nothing after the run).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 03 owns the CI red/green and the owner review. Plans touching `docs/architecture` now run the 4 s fixture-test step in the hook.

---
*Phase: 04-leak-review-and-production-go-live*
*Completed: 2026-10-29*

## Self-Check: PASSED

Checked: `docs/architecture/drill.fish`, `drills.tsv`, `drill-output.txt`, `.githooks/pre-commit`, `scripts/pre-commit-likec4-selftest.py` and `04-02-proof.txt` exist; commits `06ca6f8` and `7695303` are ancestors of HEAD; `grep -c 'node --test docs/architecture/site/leak-scan.test.mjs < /dev/null' .githooks/pre-commit` is 1; `out/hook-without-leak-step` is gone.
