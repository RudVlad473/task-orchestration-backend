---
phase: quick-261024-lb6
plan: 01
subsystem: tooling
tags: [pre-commit, gitleaks, secret-scan, git-index, selftest]
requires: []
provides:
  - pre-commit secret scan that reads the index git commit records
  - scripts/pre-commit-gitleaks-selftest.py wired into invariant-checks.yml
affects: [.githooks/pre-commit, .github/workflows/invariant-checks.yml]
tech-stack:
  added: []
  patterns: [forward GIT_INDEX_FILE into the scanner container, stdin fallback outside the mount]
key-files:
  created: [scripts/pre-commit-gitleaks-selftest.py]
  modified: [.githooks/pre-commit, .github/workflows/invariant-checks.yml]
decisions:
  - "Fix A (forward GIT_INDEX_FILE translated under /repo; stdin shape outside the mount) over fix B (route every commit through stdin)"
metrics:
  duration: ~4 minutes
  completed: 2026-10-24
status: complete
commits: 1
plan_head_before: 2faa4195833ad9067cdcbb6b641400fc00b1ac15
plan_head_after: 4bb56c25a228555137f5e4113cc5a690be8708dc
actuals:
  tokens: 21000
  tasks: 3
  commits: 1
---

# Phase quick-261024-lb6 Plan 01: Pre-commit secret scan reads the recorded index Summary

The pre-commit gitleaks scan now forwards the index git commit is about to record (GIT_INDEX_FILE) into the scanner container, so pathspec and `-a` commits of unstaged edits are scanned on their real bytes instead of about 0.

## The commit

- SHA: 4bb56c2 (`fix(quick-261024-lb6): scan the index git commit records, not the default one`), on `main`, not pushed.
- Files: .githooks/pre-commit, scripts/pre-commit-gitleaks-selftest.py (new), .github/workflows/invariant-checks.yml. `git show --stat` lists exactly these three.
- Made by `git commit -F ... -- <three paths>` with the full hook and no `--no-verify`.
- The commit was itself scanned: its hook log shows `scanned ~12735 bytes (12.73 KB)` next to `No secrets found in staged diff.`
- Rescan of 2faa419..HEAD with the pinned image (`git --redact --verbose --log-opts=2faa419..HEAD`): exit 0, 1 commits scanned, `scanned ~12735 bytes`, no leaks found.
- First commit attempt (log kept in the scratch dir) was refused at the comment-policy gate: the selftest docstring had 10 prose lines before any marker (limit 8). The earlier `verify-comments.py check` run had passed because the file was untracked at that point. Docstring shortened, commit retried and passed. No hook gate was bypassed.
- In the successful run, Gradle reported spotlessCheck and `fastTest` as UP-TO-DATE (inputs unchanged since earlier runs), so `fastTest` was not re-executed in that final attempt.

## Unscanned commits (the hole this closes)

Before this fix, every `git commit -- <paths>` of unstaged edits scanned about 0 bytes and printed "no leaks found". Known affected:

- 3c08a62, c47fa48, c22827a: pathspec commits of unstaged edits.
- 2faa419: made with `--no-verify`, so not scanned at all.
- Every earlier `git commit -- <paths>` commit of unstaged edits, and (as found during planning) every earlier `git commit -a` or `-i` commit. These were not enumerated individually.

Rescans: the four commits of 6762827..HEAD were rescanned clean before this task (4 commits, about 65 KB). Older unpushed history is covered only when CI's full-history secret-scan.yml next runs on push.

## The choice: fix A over fix B

Measured during planning (throwaway repos, pinned image), where A and B differ, on a repo whose committed file already held a credential:

| Commit | Fix A | Fix B |
|---|---|---|
| edit the line next to the committed credential | allowed | REFUSED (context lines are scanned) |
| delete the committed credential | allowed | REFUSED (removed lines are scanned) |
| new leak: does the finding print a `File:` line | yes, `File: new.txt` | no File line at all |

Both refused all five leak shapes (47 B vs 162 B scanned). B was rejected because it refuses deleting or editing beside a committed credential (blocking a leak's own remediation) and drops the File line; git also sets GIT_INDEX_FILE on every commit, so B would move every commit off the bind mount. Option C (stop using pathspec commits) was rejected in the brief.

## Test direction checks (both were run)

Selftest: scripts/pre-commit-gitleaks-selftest.py, seven cases, canary built at runtime, no credential-shaped literal in the source (grep exit 1).

- Red, against `git show 2faa419:.githooks/pre-commit` (copy in the scratch dir): run twice, before and after the fix was applied. Both times exit 1, `2/7 passed`. FAIL: unstaged_leak_pathspec, unstaged_clean_pathspec_scans_bytes ("the scan read 0 bytes, a false clean"), unstaged_leak_commit_all, nested_worktree_pathspec, index_outside_repo. PASS: staged_leak_plain_commit and out_of_tree_worktree_pathspec, as predicted (those shapes already worked).
- Case 7 matched its prediction: it failed on HEAD's hook (the commit succeeded because the container scanned the default index).
- Green, against the fixed hook: exit 0, `7/7 passed` (run twice, the second after the comment-only edits).
- Wiring: the `pre-commit-secret-scan` job in invariant-checks.yml runs the selftest. It has NOT run in CI, because nothing was pushed.

## Unset-case argv identity

With GIT_INDEX_FILE unset, the hook's docker argv (captured with a shim `docker`, 17 lines) is byte-identical between HEAD's hook and the fixed hook (`cmp` clean). With `GIT_INDEX_FILE=.git/index` the fixed hook's argv contains `GIT_INDEX_FILE=/repo/.git/index`, so the relative form resolves in the real checkout.

## Other checks

- `sh -n .githooks/pre-commit` clean; `.githooks/pre-commit` stays mode 100755; `scripts/verify-comments.py check` exits 0 (316 files).
- The finding is in the first Decisions block of the hook, 11 comment lines (limit 20).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Selftest docstring violated the comment-policy gate**
- **Found during:** Task 2 commit (first attempt refused).
- **Issue:** 10 prose lines before any marker; the gate allows 8. Missed earlier because the file was untracked then.
- **Fix:** Condensed the docstring to 7 prose lines.
- **Files modified:** scripts/pre-commit-gitleaks-selftest.py
- **Commit:** 4bb56c2 (single commit; the refused attempt created none)

**2. [Rule 1 - Bug] Hook comments made stale/over-long by the change**
- **Found during:** Task 2 verification (`verify-comments.py check` fail on .githooks/pre-commit:113 summary-first and segregated-narration).
- **Fix:** Rewrote the stdin branch lead comment to a two-line summary, and updated the stale comment that described the old `case "$GIT_TOPLEVEL"` patterns.
- **Commit:** 4bb56c2

**3. [Attribution] Commit trailer** — the plan asked for `Co-Authored-By: Claude Opus 5.5`; the commit carries `Co-Authored-By: Claude Sonnet 5.5`, matching the executing model and the session's attribution rule.

Otherwise the plan was executed as written.

## Learning queue

Gotcha recorded: /home/andre/.claude/learnings-inbox/20261024T133631788Z-a-pre-commit-hook-running-a-scanner-in-d.json

## Testcontainers leftovers

`docker ps -a --filter label=org.testcontainers=true` lists six Exited containers, all created 2026-09-25 to 2026-10-16, i.e. before this task started (2026-10-24T13:33:18Z). None was created by this task, so none was removed. No running Testcontainers containers, and no gitleaks-image containers remain.

## Known Stubs

None.

## Threat Flags

None. The only new input is the GIT_INDEX_FILE environment value, passed as one quoted docker argument.

## Not verified

- The CI job (nothing pushed).
- Individual enumeration of every earlier unscanned `-a`/`-i`/pathspec commit.
- `fastTest` was UP-TO-DATE in the final hook run rather than re-executed.

## Self-Check: PASSED

- scripts/pre-commit-gitleaks-selftest.py, .githooks/pre-commit, .github/workflows/invariant-checks.yml exist and are in commit 4bb56c2.
- 4bb56c2 is an ancestor of HEAD.
- The user's five files are unchanged: `git status --short` matches the pre-task snapshot except for this task's own entries, and none appears in the commit.
