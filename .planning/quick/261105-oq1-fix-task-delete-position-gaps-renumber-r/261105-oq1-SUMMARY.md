---
phase: quick-261105-oq1
plan: 01
subsystem: tasks
tags: [positions, ordering, bulk-update, jpa, tdd]
requires: []
provides:
  - TaskService.deleteById closes the position gap with one bulk shift
affects: [TaskService, learning docs 00/01/04]
tech-stack:
  added: []
  patterns: [renumber-after-delete via TaskRepository.shiftPositions, as ColumnService.deleteById]
key-files:
  created:
    - .planning/quick/261105-oq1-fix-task-delete-position-gaps-renumber-r/261105-oq1-red.txt
    - .planning/quick/261105-oq1-fix-task-delete-position-gaps-renumber-r/261105-oq1-green.txt
    - .planning/quick/261105-oq1-fix-task-delete-position-gaps-renumber-r/261105-oq1-mutation.txt
  modified:
    - src/main/java/com/vrudenko/kanban_board/service/TaskService.java
    - src/test/java/com/vrudenko/kanban_board/service/TaskServiceTest.java
    - docs/learning/00-README.md
    - docs/learning/01-domain-model-and-schema.md
    - docs/learning/04-service-layer-and-access-control.md
    - .planning/todos/pending/2026-11-01-task-delete-tests-read-no-state-back-and-miss-the-rejection-cases.md
decisions:
  - "Renumber after delete with one bulk UPDATE (user's choice), reusing TaskRepository.shiftPositions"
metrics:
  tasks: 3
  files: 9
status: complete
commits: 1
plan_head_before: 9288815e524b50262136a8fa309922b4c820d44e
plan_head_after: ab4158e7cbd126813fc47bb5a559ac57d650130c
actuals:
  tasks: 3
  commits: 1
completed: 2026-11-05
---

# Phase quick-261105-oq1 Plan 01: Task delete position gap Summary

A task delete now shifts every later task in its column down by one in a single bulk UPDATE, so
the sibling-count probe in `TaskService.save` can no longer hand a new task a position another task
holds.

## What changed

- `TaskService.deleteById` captures `deletedPosition` from the ownership-verified task and, right
  after `taskRepository.deleteById`, calls
  `taskRepository.shiftPositions(deletedColumnId, -1, deletedPosition + 1, Integer.MAX_VALUE)`.
  The column id is the verified task's, not a path variable. No new repository query, no flush, no
  version bump. The Javadoc records the decision.
- `TaskServiceTest.DeleteByIdTest` holds two tests: contiguity after a middle delete plus a
  create, and a query-count guard (a lone task versus the first of eight tasks, both without
  subtasks).
- Learning docs 00, 01 and 04 no longer describe the gap as a live defect; the sibling todo records
  the decision.

## Verification (both failing directions checked)

- Contiguity test on the UNFIXED code: failed with `AssertionFailedError`, positions
  `[0, 1, 2, 4, 5, 6, 7, 7]` instead of `[0..7]`; every other `TaskServiceTest` suite passed.
  Evidence: `261105-oq1-red.txt` (HEAD 9288815). After the fix: passes, `261105-oq1-green.txt`.
- Query-count test against a temporary per-row renumber mutant: failed with
  `expected: 4L but was: 11L` while the contiguity test still passed. After restoring the single
  shift: both pass. Evidence: `261105-oq1-mutation.txt`. The test would also pass on the unfixed
  code (no renumber, equal counts), which is why the mutant was needed to show it can fail.
- `scripts/verify-comments.py check` on both Java files: OK. `spotlessApply`: clean.
- `ActivityLogConsumerE2ETest` (kafka-tagged, deletes tasks): 18 tests, 0 failures.
- The pre-commit hook (gitleaks, comment lint, spotlessCheck, fastTest) ran on the commit and
  passed.

## Subtask finding

No bug and no change: `V5__add_position_subtask_version_theme_board_name_uniqueness.sql` adds
`position` to `tasks` and `columns` only, `SubtaskEntity` has no position field,
`TaskEntity.subtasks` is `@OrderBy("id")`, and `SubtaskService.save` sets no position, so a
subtask delete cannot leave a position gap.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Learning doc 04 Known gaps item 3 also described the gap**
- **Found during:** Task 3
- **Issue:** The plan listed 04's "Caution" bullet and "How we test it" for edit, but 04's own
  "Known gaps and open items" item 3 ("Task delete leaves a position hole") also stated the gap as
  live, contradicting the plan's truth "the learning docs no longer describe the task-delete gap as
  a live defect".
- **Fix:** Removed item 3 and renumbered items 4-12 as 3-11. No other chapter links to those item
  numbers (checked with `rg`).
- **Files modified:** docs/learning/04-service-layer-and-access-control.md

**2. [Plan correction] Known gaps numbering in doc 01**
- **Issue:** The plan said to renumber items 2-5 as 1-4; the file holds items 1-10.
- **Fix:** Removed item 1 and renumbered 2-10 as 1-9.
- **Files modified:** docs/learning/01-domain-model-and-schema.md

**3. [Attribution] Commit trailer**
- The plan asked for `Co-Authored-By: Claude Opus 5.5`; the session's attribution instruction names
  `Claude Sonnet 5.5`, so the commit carries that trailer.

**4. [Todo count] "0 of 7"**
- With one row deleted and one marked COVERED, the sibling todo now reads "1 of 6 required cases
  fully covered".

## Not edited, by instruction

- `docs/wiki/learning/` copies still describe the gap (`.claude/CLAUDE.md`: originals stay
  authoritative until the cleanup deletes them). Whoever runs that cleanup inherits the stale text.
- The source todo `2026-11-01-task-delete-leaves-position-gaps-...` was not moved or edited.

## Open questions (not done, surfaced)

1. **Existing stored gaps.** The fix stops new gaps but does not repair columns that already hold
   gaps or ties from earlier deletes; in such a column the next create can still duplicate a
   position. Repair would be a one-shot Flyway migration renumbering each column by
   `(position, id)`; it changes production data on deploy, so it was not planned.
2. **Concurrent mutation race (pre-existing, T-06-19).** A create reading the count while a delete is
   uncommitted can still leave a gap or tie. Closing it needs a per-column lock in every mutation.

## Known Stubs

None.

## Threat Flags

None. The shift's column predicate uses the id read from the ownership-verified task (T-oq1-01),
and the shift runs inside `deleteById`'s single transaction (T-oq1-02).

## Cleanup

After the commit, `docker ps -a` showed one postgres:16 Testcontainers container and its ryuk
reaper still up, held by the live Gradle daemon (ryuk was present, so they were not orphaned).
`./gradlew --stop` removed both; a second `docker ps -a --filter label=org.testcontainers` was
empty. The Gradle daemon is stopped.

## Self-Check: PASSED

- Commit ab4158e exists on main (`git log`), one commit since plan_head_before; the pre-commit hook
  (gitleaks, comment lint, spotlessCheck, fastTest) completed with BUILD SUCCESSFUL in 3m 5s.
- The three evidence files, TaskService.java, TaskServiceTest.java, three learning docs and the
  sibling todo are all in the commit (`git show --stat`).
- Unrelated untracked files (`.planning/quick/261024-*`, `docs/architecture/record-demo.mjs`) and
  PLAN.md/SUMMARY.md stay uncommitted.
