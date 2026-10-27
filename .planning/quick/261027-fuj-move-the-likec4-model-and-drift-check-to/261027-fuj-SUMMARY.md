---
phase: quick-261027-fuj
plan: 01
subsystem: tooling
tags: [likec4, pre-commit, github-actions, drift-check, pnpm]
requires: []
provides:
  - LikeC4 model, drift script, drills and pinned tooling under docs/architecture
  - pre-commit step that refuses a stale model (fail closed, prints the install command)
  - likec4-model-drift job in invariant-checks.yml
  - scripts/pre-commit-likec4-selftest.py (11 cases, run against the real hook)
affects: [docs/DIAGRAM_CONVENTIONS.md, docs/LOCAL_DEV.md, README.md, .planning/spikes]
status: complete
done_list_exception: "Plan Task 3 done-list line 6 (`docker ps -a --filter label=org.testcontainers`) prints 8 names, not nothing. All 8 are older than this run (25 hours to 4 weeks, all Exited), so none was left by it and none was removed. The orchestrator judged the intent of the line (this run leaves no container) to hold and set status complete. See Verify block."
actuals:
  tokens: 16286
  tasks: 3
  commits: 1
plan_head_before: 81d56224017ec883453258a32075bd9b334a8f86
plan_head_after: de300399b6077a9abaff1c2954ecbe535c6bde10
tech-stack:
  added: []
  patterns:
    - "fail-closed path-gated hook step"
    - "self-test that runs the real hook in scratch repos and probes that tooling is unresolvable"
key-files:
  created:
    - docs/architecture/.gitignore
    - docs/architecture/pnpm-workspace.yaml
    - scripts/pre-commit-likec4-selftest.py
  modified:
    - .githooks/pre-commit
    - .github/workflows/invariant-checks.yml
    - docs/architecture/sync.mjs
    - docs/architecture/likec4.fish
    - docs/architecture/drill.fish
    - docs/architecture/package.json
    - docs/DIAGRAM_CONVENTIONS.md
    - docs/LOCAL_DEV.md
    - README.md
key-decisions:
  - "docs/architecture is the model's home; the hook step runs `node docs/architecture/sync.mjs check` and fails closed."
  - "allowBuilds esbuild false, because an undecided entry makes pnpm 11 exit 1 on a frozen install."
  - "CI uses pnpm/action-setup@v6 by tag with the pnpm version read from package.json's packageManager."
requirements-completed: [261027-fuj]
---

# Quick 261027-fuj: LikeC4 model moved to docs/architecture, drift gated at commit and in CI

**The LikeC4 model, drift script, drills and pinned likec4 1.60.0 now live in docs/architecture; a stale model is refused by a path-gated, fail-closed pre-commit step and by a new CI job, proven by an 11-case self-test seen red, green and red-on-control.**

The executor first wrote `status: incomplete` because of one literal done-list mismatch (Testcontainers listing, below). The orchestrator read it, judged that the substance holds (this run left no container: the hook's `fastTest` was UP-TO-DATE and started none, and all eight listed containers predate the run by at least 25 hours), and set `status: complete` with the exception recorded in the frontmatter.

## Orchestrator's account (not measured by the executors)

1. **Plan review.** The plan went through the plan checker twice. Pass 1 found one blocker (the missing-tooling self-test cases could not fail, because Node resolves `likec4` from ancestor folders; fixed by moving scratch repos under the root `out/` and adding a per-case probe) and two warnings. The planner revised once. Pass 2 found one new blocker (Task 3 precondition expected `exit=0` that Task 2 did not write), two warnings (cleanup used TemporaryDirectory instead of the brief's trash-put; missing tooling with a stale model printed no install command) and two info items. The workflow caps revisions at two, so the user was offered force-proceed, one more revision or abort, and chose force-proceed. The orchestrator then applied five fixes to the plan text itself: `exit=$?` appended to the green evidence; `check()` loads likec4 first; self-test case 9 became stale-model-plus-no-tooling; scratch cleanup uses trash-put when available and falls back to rmtree only in CI; baseline comparison is normalized by a shared filter; data-flow section trimmed to three sentences. Those five edits had no third independent plan review. The post-execution checks and the verifier are what test them.
2. **Execution split.** One executor per task (Tasks 1, 2, 3A, 3B), because the workflow's single-executor run did not fit the plan's size and the executor agent has no tool to wait safely for the 4-minute hook. The orchestrator re-checked Task 1 and Task 2 results itself before dispatching the next task: drift check OK from the new home, baseline equals after, spike folders hold only READMEs plus the smoke model, hook diff 50 additions and 0 deletions, self-test run again 11/11 green, red evidence 10 FAIL and 1 PASS control, mutant fails only the control.
3. **Feature commit hook run.** The orchestrator started the real hook in the background with a guard script (memory-pressure and 12-minute stall kill by PID). At start the machine had 4.8 GB available, 841 MB free, swap 1.6 of 2.0 GB used, memory pressure low. The hook ran all stages and exited 0 in well under the expected 4 minutes. `spotlessCheck` and `fastTest` both reported UP-TO-DATE because no Java or test input changed, so Gradle reused its earlier passing result and **the tests were NOT re-executed in this run.**
4. **Not verifiable here.** The GitHub Actions job `likec4-model-drift` itself (its first real run is on push). actionlint is not installed. Pushing the feature commit will run deploy.yml, because .githooks/, .github/ and scripts/ are not in its paths-ignore. Nothing was pushed.
5. **Closing commit.** The closing docs commit (PLAN, SUMMARY, BRIEF, commit-hook.log, STATE.md) is the orchestrator's. This executor made no commit and updated neither STATE.md nor ROADMAP.md. That commit has no Java, migration or k8s file, so `--no-verify` is allowed (.claude/CLAUDE.md), but only after a manual secret scan: read the pinned image with `rg -o -m1 "ghcr.io/gitleaks/[^']+" .githooks/pre-commit`, pipe `git diff --cached -- <those paths>` into `docker run --rm -i <image> stdin --redact --no-banner --verbose --exit-code 2`, and commit only on exit 0 (2 means a secret, any other code means the scan did not run).

## What was done (executor results, from the handoff file and evidence files)

- **Commit:** `de30039` ("feat(quick-261027-fuj): gate LikeC4 model drift at commit and in CI from docs/architecture"), the one feature commit, made by the orchestrator through the real `.githooks/pre-commit`. The log ends `exit=0`. 39 files changed, 801 insertions, 47 deletions. `git rev-list --count 81d5622..HEAD` is 1.
- **Log order (read this run):** line 14 `Checking the LikeC4 model against the code it describes...`, line 15 `OK (17 elements, 6 tables, 2 topics, 4 workloads)`, line 16 `Checking formatting...`.
- **Install path:** frozen install on the first try, exit 0, no lockfile refresh. "Done in 4.8s using pnpm v11.20.0" (warm; wall 4.90 s). pnpm-lock.yaml is byte-identical to the moved file. A second frozen install at verify time also exited 0.
- **Import-before-compare ordering:** the first `check` after the rewrite reported `generated.c4 is stale` (exit 1), proving the line 1 header is part of the comparison; after `generate`, `check` printed the OK line. Direction checked for the ordering fix (load likec4 first in `check()`): green only. The pre-fix form is the spike's committed script, whose paths cannot run from a scratch location, so a red run was not possible in Task 1. Task 2's green run is the actual proof: case 9 asserts the output does NOT contain `generated.c4 is stale` and passes.
- **System Node:** `/usr/bin/node` v22.22.1 runs `sync.mjs check` and prints the OK line, exit 0, despite likec4's declared `engines.node >=22.22.3`. Trade-off 4 resolved on the pass side. (The commit's hook run used node v24.19.0 per the mutant evidence; the system-Node run is the separate measurement.)
- **Residue trashed with trash-put:** spike 005 node_modules 138M, 005 out 5.7M, 006 out 7.0M, 007 out 20K. This run also trashed `docs/architecture/out/`. `docs/architecture/node_modules` was left in place.
- **Self-test, three directions:**
  - Red, unwired hook (HEAD's): `1/11 passed`, exit 1. FAIL: test_01 to test_10. The only PASS is test_11_unrelated_path_runs_no_drift_step.
  - Green, wired hook: `11/11 passed`, exit 0.
  - Red, filter mutant (the one `git diff --cached` pathspec list replaced by `.`): `10/11 passed`, exit 1. The single FAIL is test_11 (refused for the missing install, exit 3).
  - PROBE lines seen in all three direction files: `PROBE ok stale-no-tooling: likec4 is not resolvable from the scratch repo` (case 9) and `PROBE ok unrelated-path: likec4 is not resolvable from the scratch repo` (case 11).
  - Gitleaks self-test: `7/7 passed`.
- **Hook step:** 50 insertions, 0 deletions vs the base, mode 100755, between the instruction-budget block and `Checking formatting...`. Beyond the plan text, the trigger-list assignment itself fails closed if `git diff` fails, so an empty variable from a failed call cannot read as "no trigger".
- **CI:** `likec4-model-drift` job appended after `pre-commit-secret-scan`; permissions `contents: read`; `actions/checkout@v5`, `pnpm/action-setup@v6` (package_json_file, no version), `actions/setup-node@v5` with `'24.19.0'`, frozen install, self-test, check. `docs/architecture/**` re-included right after `!docs/**` in both paths lists. Numstat against the base: workflow 35 added 0 deleted, hook 50 added 0 deleted. Jobs now: comment-lint, instruction-budget, k8s-invariants, k8s-manifests-valid, likec4-model-drift, pre-commit-secret-scan, public-dashboards.
- **Paths-filter emulation:** `RESULT: PASS`. Push and pull_request lists identical in HEAD and the working tree; only `docs/architecture/model/model.c4`, `sync.mjs` and `pnpm-lock.yaml` flip excluded to included. The emulator is the executor's own model of GitHub's documented semantics, not GitHub's matcher.
- **Docs:** spike READMEs 005/006/007 got a pointer paragraph and new How-to-Run paths (Investigation Trail and Results untouched). CONVENTIONS.md and MANIFEST.md rewritten to docs/architecture. DIAGRAM_CONVENTIONS.md: model-home sentence, Checks line, a Wired bullet, and the "moving the model out of spikes" bullet deleted. LOCAL_DEV.md: one Prerequisites bullet. README.md: the gate sentence now names the six gates in hook order. The wiki copy was not edited.
- **Commit-paths comparison:** the 51 paths in the commit equal the 51 on the commit-paths list, with none outside it and none missing. No foreign staged path (nothing was staged before Task 1).

## Verify block, run this session against the real tree

| Line | Plan expects | Got | Verdict |
|---|---|---|---|
| 1 | `exit=0` | `exit=0` | holds |
| 2 | banner, OK line, `Checking formatting...` in order | lines 14, 15, 16 of the log | holds |
| 3 | subject plus exactly 12 `R` lines; renames are the 12 named files; rest A, D, M on the list | subject correct; 12 `R` lines; 51 of 51 paths on the list | count holds; pairing differs, see Notes |
| 4 | only `!! docs/architecture/node_modules/` | 7378 `!!` lines, every one under `docs/architecture/node_modules/`, nothing else | holds in substance; the command's `--untracked-files=all` lists files, not the folder |
| 5 | `scratch-exists=1` | `scratch-exists=0` at that moment, because `Q/scratch/` is trashed only after this SUMMARY | re-checked after the trash, see end of file |
| 6 | prints nothing | prints 8 names: recursing_blackwell, brave_mclaren, sad_fermi, tender_perlman, stoic_borg, strange_galois, naughty_perlman, loving_newton (7 postgres:16, 1 redpanda). Created 2026-10-26 10:50 (x2), 2026-10-16 (x3), 2026-10-05, 2026-09-25 (x2); all Exited; the commit was 2026-10-27 12:28 | **does not hold literally**; the youngest is about 25 hours older than the commit, so none was left by this run and none was removed |
| 7 | prints nothing (rg exit 1) | nothing, `rg-exit=1` | holds |
| 8 | deletions `0` for both | `50 0 .githooks/pre-commit`, `35 0 .github/workflows/invariant-checks.yml` | holds |
| 9 | six existing jobs plus likec4-model-drift | the seven names listed above | holds |
| 10 | three non-Markdown paths flip; lists identical | exactly those three flip; identical in both versions; `RESULT: PASS` | holds |
| 11 | `exit=0` | `exit=0` | holds |
| 12 | nothing | `diff-exit=0`, no output | holds |
| 13 | `actionlint-exit=1` | `actionlint-exit=1` (command -v found nothing) | holds; actionlint not run |
| 14 | nothing | nothing | holds |

Other done items: HEAD's subject is the feature commit and `main` is ahead of `origin/main` by 18, so the commit is local (no push was made by this executor). `git ls-files docs/architecture` lists 13. The three spikes list 4: 005 README.md and `smoke/smoke.c4`, 006 README.md, 007 README.md. `python3 scripts/verify-instruction-budget.py` prints `OK: 4886 bytes within a ceiling of 5100.` The Testcontainers listing is the one item that fails as worded.

## Notes on the mismatches

- **Line 3 pairing.** Git's similarity detection paired differently from the plan's list: the spike's 005 `.gitignore` shows as `R076` to `docs/architecture/.gitignore`; the spike's `drill-output.txt` shows as `R100` to this task's `261027-fuj-drill-rerun.txt` (the rerun is byte-identical to the recorded output by design, `drill-identical` confirmed); `docs/architecture/drill-output.txt` and `docs/architecture/pnpm-workspace.yaml` show as `A`, the latter with a `D` for the old spike file because its content was rewritten. History for those two files follows `git log --follow` less cleanly. The 12 `R` count matches; the set does not match the plan's list.
- **Line 6.** The plan's done wording assumes the host has no Testcontainers leftovers at all. It has eight, from earlier sessions, in line with the memory note "Testcontainers orphaned, no Ryuk". They are not this task's and were not touched.

## Old-path hits inside .planning (rg from the plan, node_modules excluded), classified

Run: `rg -n --hidden -g '!**/node_modules/**' '<old spike path pattern>' .planning`. Hits outside `Q/scratch/`:

| File | Hits | Class |
|---|---|---|
| spike 005 README.md | 1 | live reference: the validate command for the smoke folder that stays in the spike (not a stale pointer) |
| quick 261027-f8q BRIEF.md | 1 | dated record of a finished task |
| quick 261027-f8q PLAN.md | 5 | dated record of a finished task |
| quick 261027-f8q verify/lc4/generated.c4 | 1 | dated evidence (its header names the old script path) |
| quick 261027-f8q verify/png-dimensions.txt | 5 | dated evidence of rendered PNGs |
| quick 261027-fuj BRIEF.md | 6 | this task's own record of the old paths |
| quick 261027-fuj PLAN.md | 28 | this task's own record of the old paths |
| quick 261027-fuj commit-hook.log | 15 | history lines: git's rename and delete listing of the feature commit |

Total 62 outside the scratch folder: 0 stale references, 1 live reference, 61 dated records or history. The 12 scratch files that matched (the executors' own scripts and path lists) were trashed after this check. The f8q `verify/lc4.fish` was repointed to the new home; it no longer matches.

## Deviations from Plan

### Auto-fixed Issues

None under Rules 1 to 3 beyond what the handoff records below.

### Recorded by the executors

1. **Task 1:** spike 007's `out` held four ignored scripts (init2.sh, isolation2.sh, probe.sh, validate-plan.sh), not only init2.sh. All four went to the trash with trash-put (recoverable).
2. **Task 1:** scratch scripts `cd` to the repo root inside the script file, because the Bash cwd on this machine is `src/main/resources/db/migration`.
3. **Task 1:** verify line 5 showed `D` (not `R`) for the moved spike paths, because the pathspec is limited to the spike folders, so git could not pair them with their targets.
4. **Task 2:** the self-test's `remove_tree` falls back to `shutil.rmtree` when trash-put exists but exits non-zero (the plan said rmtree only where trash-put is absent). Not exercised: trash-put succeeded every run.
5. **Task 2:** the module docstring was restructured under a `Decisions:` marker because the comment lint's segregated-narration rule flagged 13 prose lines before any marker.
6. **Task 2:** the filter mutant was made with `cp` plus the Edit tool, not a make-mutant script; the diff against the hook shows only the pathspec lines changed.
7. **Task 2:** the hook's trigger-list assignment fails closed if `git diff` fails (beyond the plan text).
8. **Task 3A:** the spike 005 README follow-on line was also repointed to docs/architecture/likec4.fish.
9. **Difference from the brief's "deleted afterwards with trash-put":** the self-test's scratch repos under the root `out/` go to `trash-put` when it is on PATH, and are removed with `shutil.rmtree` only where it is absent, because the self-test also runs in CI, which has no trash-put. The executors' own scratch (`Q/scratch/`, `docs/architecture/out/`) went with trash-put.

## Known Stubs

None found in the files this plan created or modified.

## Threat Flags

None. The CI job and the self-test's nested scratch repos were already in the plan's threat model (T-fuj-01 to T-fuj-04).

## Not verified, and follow-ups

- **Not run here:** the GitHub Actions job `likec4-model-drift` was checked only by YAML parse, the paths-filter emulation and the action refs compared with the sibling jobs; its first real run is the user's push. actionlint is not installed. `fastTest` and `spotlessCheck` were UP-TO-DATE in the commit's hook run, so the Java tests were not re-executed.
- **Pushing runs deploy.yml.** .githooks/, .github/ and scripts/ are not in its paths-ignore, so a push of this commit starts a full deploy. Nothing was pushed.
- **Follow-ups:**
  - `.claude/CLAUDE.md` still lists the hook's steps without the new one (the brief allowed touching it only for a moved path; 214 bytes of budget remain).
  - `docs/learning/09-build-quality-and-ci.md` describes the hook with already-stale line anchors.
  - `docs/wiki/conventions/diagram-conventions.md` lags the original.
  - `scripts/verify-diagrams.py` still exits 1 on `docs/learning/kafka-architecture-overview.md:11` (pre-existing, unrelated; output equals the baseline).
  - The scenario-layout choice (sequence versus TopBottom) remains open.
  - Known holes recorded in the hook: a partly staged commit is judged by the working tree; `k8s/platform/traefik` and three Java packages the model cites are outside the five triggers (CI catches those). Every checkout and git worktree needs its own `pnpm --dir docs/architecture install` before a trigger-path commit.

## Self-Check

- Feature commit `de300399b6077a9abaff1c2954ecbe535c6bde10` is HEAD and an ancestor of HEAD: FOUND.
- `git ls-files docs/architecture` lists 13 paths and `scripts/pre-commit-likec4-selftest.py` is in the commit: FOUND.
- Evidence files read this run: the commit-hook log, selftest red/green/mutant, gitleaks self-test, install, system-node, paths-filter, drill rerun, residue sizes.
- After `Q/scratch/` was trashed (once this SUMMARY existed): `scratch-exists=1`, so verify line 5 now holds. `git status --short` shows only the other tasks' untracked `.planning/quick/261024-*` files plus this task's BRIEF, PLAN, SUMMARY and commit-hook.log, which are the orchestrator's closing commit.
- Self-Check result: PASSED for existence. One done-list line (Testcontainers listing) did not hold as worded and is recorded as an exception in the frontmatter.
