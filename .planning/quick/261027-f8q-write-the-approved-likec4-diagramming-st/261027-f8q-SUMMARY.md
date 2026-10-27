---
phase: quick-261027-f8q
plan: 01
subsystem: docs
tags: [likec4, diagrams, conventions, documentation]
requires: []
provides:
  - "docs/DIAGRAM_CONVENTIONS.md: LikeC4 standard section (entities and rules, viewpoints, checks, verification outcomes V1 to V6, undefined and out-of-scope items)"
  - "Committed verification evidence under the quick directory's verify/ folder, plus verify/png-dimensions.txt"
affects: [docs/DIAGRAM_CONVENTIONS.md]
key-files:
  created:
    - .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/261027-f8q-base-sha.txt
    - .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/261027-f8q-guard-before.txt
    - .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/261027-f8q-required-terms.txt
    - .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/png-dimensions.txt
  modified:
    - docs/DIAGRAM_CONVENTIONS.md
decisions:
  - "Pure insertion of a LikeC4 standard section before Layout and naming; every Mermaid section left byte-identical"
  - "Scenario layout (variant sequence versus autoLayout TopBottom) reported with measurements, not chosen"
metrics:
  duration: "about 20 minutes"
  completed: 2026-10-27
status: complete
actuals:
  tokens: 5000
  tasks: 2
  commits: 2
plan_head_before: 474bd74311d5e107d03f51a2c604ccd824759023
plan_head_after: 3abab0ad51aeaf574c95062ec666e023087faf49
---

# Phase quick-261027-f8q Plan 01: LikeC4 diagramming standard Summary

A `## LikeC4 standard` section in `docs/DIAGRAM_CONVENTIONS.md` records the approved design and the six verification outcomes (V1 to V6) with the brief's verdicts, as a pure insertion before `## Layout and naming`.

## What was done

- Task 1 (tracer): took the baseline (base sha, guard output), recorded all 16 PNG dimensions as text in `verify/png-dimensions.txt`, inserted the section opening, and committed it with the verification evidence folder.
- Task 2: wrote five `###` subsections: Entities and rules, Viewpoints, Checks, Verification outcomes (2026-10-27), and Undefined, not adopted, and outside this change. Committed with the required-terms file.

All done-list gates held. `status: complete`.

## Commits

| Task | SHA | Subject |
|---|---|---|
| 1 | 1ca02f9 | docs(quick-261027-f8q): open the LikeC4 standard section and commit its evidence |
| 2 | 3abab0a | docs(quick-261027-f8q): write the LikeC4 standard with its verification outcomes |

`commits: 2` is measured with `git rev-list --count 474bd74..HEAD`. Nothing was pushed (`main...origin/main [ahead 15]`, which includes 13 earlier commits).

## Gitleaks scans (pinned image `ghcr.io/gitleaks/gitleaks:v8.30.1@sha256:c00b6bd0...`, stdin shape, `--exit-code 2`)

| Commit | Bytes scanned | Exit code |
|---|---|---|
| 1ca02f9 | about 34.10 KB | 0 |
| 3abab0a | about 12.06 KB | 0 |

Both scans ran over the whole staged diff. Before each scan I read `git diff --cached --name-only` and it held exactly the plan's paths (see "Staged foreign paths").

## Guard before and after

Baseline (`261027-f8q-guard-before.txt`, taken before any doc edit) and the output after both commits are identical:

```
FAIL flowchart-rules docs/learning/kafka-architecture-overview.md:11 inline diagram must open with the shared init line
1 violation(s); 11 diagram(s), 68 doc(s) scanned
exit=1
```

The baseline matched the planning-time measurement. The `docs/learning/kafka-architecture-overview.md:11` violation is pre-existing and untouched.

## Number trace (verify line 6)

Output: `1254`, the only number in the section that is in neither the brief nor `verify/*.txt`.

Derivation: `verify/png-dimensions.txt` lists the verify workspace's `process` view (which carries an extra experimental node) at 2508 x 3072. 2508 divided by 2 (`deviceScaleFactor` 2) is 1254. The brief's 1233 is spike 006's render (2466 / 2), also in `png-dimensions.txt`. The doc states both and their sources.

## Other gate results

- Guard diff against baseline: empty (Task 1 and Task 2).
- Remove-the-section diff against base commit 474bd74: empty (also re-run after both commits), so no existing line changed.
- Required terms missing: none (all 32).
- Verification-table rows `V1` to `V6`: 6.
- `diagrams/`, `avro`, `avsc` in the section: none.
- Cited `verify/` paths: all 9 are tracked (`git ls-files --error-unmatch` exit 0). No render from an ignored `out/` folder is cited.
- `git diff --name-only 474bd74 -- docs/wiki docs/raw .claude`: empty.
- `verify-instruction-budget.py`: 4886 of 5100 bytes, OK.
- `git status --porcelain -- docs/`: empty.
- `png-dimensions.txt`: 16 lines, matching the planning-time sizes.

## Deviations from Plan

### Execution adjustments (no Rule 1 to 3 fixes needed)

1. **Working directory.** The shell started in `src/main/resources/db/migration`, not the checkout root, and the machine rules forbid `cd` in a Bash call. Every plan command that assumed the root was therefore run through small script files in `/tmp/f8q/` (guard, dimensions, verify, stage, scan, commit). `dims.sh` changes into the checkout root internally so `file` printed repo-relative paths, as the plan specifies for `png-dimensions.txt`. This is a use of `cd` inside a script file, not a Bash call, but it is worth flagging.
2. **Gitleaks scan scope.** The scan scripts pipe `git diff --cached` with no pathspec instead of the plan's explicit path list, because the path list exceeded the 300-character command limit. The index held exactly the plan's paths at both scans, so the two are equivalent.
3. **Subsection count.** The plan's Task 2 text says "four `###` subsections" but lists five items (the fifth being "Undefined, not adopted, and outside this change"). I wrote five, following the list.
4. **Protected branch.** HEAD is `main`. `.planning/config.json` sets `allow_default_branch_commits: true`, and the orchestrator ran this sequentially on the current branch, so the commits went to `main`.

## Auth gates

None.

## Known Stubs

None.

## Threat Flags

None. The change adds prose and committed text evidence only.

## Staged foreign paths

None. `git diff --cached --name-only` before each commit listed exactly the plan's paths (Task 1: BRIEF, base-sha, guard-before, the 16 non-ignored `verify/` files including `png-dimensions.txt`, and the doc; Task 2: the doc and the required-terms file). Untracked files from quick tasks 261024-nvj, 261024-q91 and this directory's PLAN.md and SUMMARY.md were left alone.

## Open questions

- **Scenario layout.** Both `variant sequence` (V1) and `autoLayout TopBottom` (V6) are adopted. Measured against the Mermaid-derived width bound, the sequence render (1599) fails and TopBottom (692) passes. The brief does not choose between them, so the user decides. The doc reports both and leaves the choice open.
- **Pre-existing guard violation.** `docs/learning/kafka-architecture-overview.md:11` lacks the shared init line. Todo candidate, not fixed here.
- **Wiki copy lag.** `docs/wiki/conventions/diagram-conventions.md` now lags the original. That is expected under `.claude/CLAUDE.md`'s "originals stay authoritative" rule and is left for the wiki cleanup.
- **Process width source (resolved by the orchestrator after execution).** The brief's `process 1233` is spike 006's render. The verify workspace's `process.png` (1254) is a different view, not a re-render: `verify/lc4/exp.c4` adds a `sub-el` component to `kanban`, and the `process` view includes `kanban.*`, so the PNG shows an extra node (read from the image). 1233 is the figure to use. The doc was corrected in a follow-up commit.

## Self-Check: PASSED

- `docs/DIAGRAM_CONVENTIONS.md`: FOUND, contains `## LikeC4 standard`.
- `261027-f8q-required-terms.txt`, `261027-f8q-base-sha.txt`, `261027-f8q-guard-before.txt`, `verify/png-dimensions.txt`: FOUND.
- Commits 1ca02f9 and 3abab0a are ancestors of HEAD.
