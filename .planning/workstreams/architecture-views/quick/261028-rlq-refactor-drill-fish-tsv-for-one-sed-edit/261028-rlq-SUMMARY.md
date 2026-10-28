---
phase: quick-261028-rlq
plan: 01
subsystem: architecture-views
tags: [fish, drill-harness, sed, tsv, gate-01]
requires: []
provides:
  - docs/architecture/sed-drills.tsv: one row per one-sed drill (id, title, file, sed expression)
  - sed_drills runner in docs/architecture/drill.fish
affects: [docs/architecture/drill.fish]
tech-stack:
  added: []
  patterns: ["data rows read with string match + string split -m3, expression passed to sed --sandbox as one argument"]
key-files:
  created: [docs/architecture/sed-drills.tsv, .planning/workstreams/architecture-views/quick/261028-rlq-refactor-drill-fish-tsv-for-one-sed-edit/261028-rlq-mutation.txt]
  modified: [docs/architecture/drill.fish]
decisions:
  - "15 one-sed drills (D2, D6, D6b, D8, D9, D10, D10b, D12, D18 to D24) became TSV rows; 21 sections stay written out"
  - "sed_drills takes ids and is called where each run of one-sed drills stood, so drill order and output are unchanged"
  - "sed runs with --sandbox so a row cannot use the e, r or w commands"
status: complete
commits: 2
plan_head_before: 7321bd18d6540ef32bdb9a98c6d0cb2294263067
plan_head_after: 49f95d485412f7cdeb2b5443318e42f7a5b4010c
actuals:
  tokens: 2100
  tasks: 2
  commits: 2
metrics:
  duration: about 15 minutes
  completed: 2026-10-28
---

# Phase quick-261028-rlq Plan 01: drill.fish one-sed drills as sed-drills.tsv Summary

The 15 single-`sed -i` drills in `docs/architecture/drill.fish` are now 15 rows of `docs/architecture/sed-drills.tsv`, run by a `sed_drills` function through 6 calls placed where the blocks stood; the replay is byte-identical to `drill-output.txt`.

## Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Tracer: D18 end to end through the TSV and sed_drills | ed074df | docs/architecture/drill.fish, docs/architecture/sed-drills.tsv |
| 2 | Move the other 14 one-sed drills, update the header, prove the oracle reads the TSV | 49f95d4 | docs/architecture/drill.fish, docs/architecture/sed-drills.tsv |

The mutation record `261028-rlq-mutation.txt` is left uncommitted for the orchestrator's docs commit.

## Verification (all run, real output)

- Tracer (after Task 1): `fish -n` and `fish_indent --check` exit 0; TSV has `1 4 id` and `2 4 D18`; `sed_drills D99` prints `sed_drills: no row for D99 in sed-drills.tsv` and `exit=2` with stdin from /dev/null, no hang; replay `run=0`, `cmp=0`.
- After Task 2: `fish -n` and `fish_indent --check` exit 0; counts of `^echo "== `, `^sed -i`, `^sed_drills ` are 21, 6, 6; TSV `15 0`; the ids cut from the TSV equal the ids called in order (diff empty); header names the TSV; `python3 scripts/verify-comments.py check` printed `OK: 331 files, 1286 comment blocks`; replay `run=0`, `cmp=0` (about 69 s).
- Oracle reads the TSV: no-op mutation of the D24 row gave `cmp exit: 1`, diff confined to lines 178 to 181 (D24: mutant `exit=0` and `OK`, oracle `exit=1` and `DRIFT`). TSV restored with `git checkout`, `git status --porcelain -- docs/architecture` empty afterwards.
- `git diff --stat 7321bd1 -- docs/architecture` lists only `drill.fish` (98 lines changed) and `sed-drills.tsv` (16 added).
- Both commits ran the full `.githooks/pre-commit` (gitleaks, comment lint, spotless, fastTest) and passed.

## Deviations from Plan

None - plan executed exactly as written. The plan's pre-correction of the brief (five more one-sed drills than listed: D2, D8, D10, D18, D19) was followed as written.

Process note, not a deviation: two Bash calls were rejected by the dense-command hook and re-issued as shorter steps; no state was changed by the rejected calls.

## Known Stubs

None.

## Threat Flags

None. The plan's T-rlq-01 to T-rlq-03 mitigations are in place: `sed -i --sandbox -e`, no eval builtin, and a missing-id status check before `string split`.

## Cleanup

Scratch files `rlq-gen.fish`, `rlq-run.txt`, `rlq-mut.txt` were trashed; `docs/architecture/out/` is empty. Tracked changes outside this plan: `.planning/workstreams/architecture-views/ROADMAP.md` was already modified before this task and was left alone, as were the untracked phase 03 planner files.

## Self-Check: PASSED

- FOUND: docs/architecture/sed-drills.tsv, docs/architecture/drill.fish, 261028-rlq-mutation.txt
- FOUND commits: ed074df, 49f95d4 (both ancestors of HEAD)
