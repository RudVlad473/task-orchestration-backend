---
phase: quick-261027-fuj
verified: 2026-10-27T11:45:00Z
status: passed
score: 10/10 must-haves verified
covered_files: [docs/architecture/sync.mjs, .githooks/pre-commit, .github/workflows/invariant-checks.yml, scripts/pre-commit-likec4-selftest.py, docs/architecture/package.json, docs/architecture/pnpm-workspace.yaml, docs/architecture/.gitignore]
behavior_unverified: 0
gaps: []
human_verification: []
---

# Quick 261027-fuj Verification Report

**Goal:** Move the LikeC4 model and drift check out of `.planning/spikes` into `docs/architecture/`, and wire the drift check into `.githooks/pre-commit` and `invariant-checks.yml`, so a code change that makes the model stale is refused at commit time and in CI. Nothing in the Java build changes.
**HEAD verified:** de30039 (the single feature commit). No Gradle, no real hook run, nothing committed or staged by this verification.
**Status:** passed. The one thing not verifiable here is the GitHub Actions job itself (see Not verified).

## Observable truths

| # | Truth | Command run | Output | Status |
|---|---|---|---|---|
| 1 | docs/architecture tracks the 5 model files, sync.mjs, likec4.fish, drill.fish, drill-output.txt, package.json, lockfile, workspace yaml, .gitignore | `git ls-files docs/architecture` | 13 paths: .gitignore, drill-output.txt, drill.fish, likec4.fish, model/{deployment,generated,model,spec,views}.c4, package.json, pnpm-lock.yaml, pnpm-workspace.yaml, sync.mjs | VERIFIED |
| 2 | Spikes 005/006/007 hold only READMEs plus 005 smoke.c4, no untracked or ignored residue | `git ls-files` on the three spike dirs; `git status --porcelain --ignored --untracked-files=all` on them | 4 tracked files (005 README.md, 005 smoke/smoke.c4, 006 README.md, 007 README.md); status output empty | VERIFIED |
| 3 | `sync.mjs check` is OK / exit 0, and the drill reproduces the recorded output | `node docs/architecture/sync.mjs check`; `fish docs/architecture/drill.fish` then `diff docs/architecture/drill-output.txt` | `OK (17 elements, 6 tables, 2 topics, 4 workloads)`, exit 0. Drill exit 0, diff exit 0: baseline OK, D1 to D4 each exit 1 with a DRIFT line (stale generated.c4, topic kanban.audit, Deployment/worker, deleted postgres manifest), D5 renamed class exit 0 (not caught) | VERIFIED |
| 4 | A frozen install works from a clean checkout (and the install command is what the failure prints) | `git archive HEAD docs/architecture k8s src/main` into a scratch dir under the task dir, `sync.mjs check` before install, `pnpm --dir docs/architecture install --frozen-lockfile`, `check` again, `likec4.fish validate` | Before install: `likec4 is not installed for docs/architecture. Run: pnpm --dir docs/architecture install`, exit 3. Install exit 0 (pnpm v11.20.0, "+ likec4 1.60.0"). After: `OK (17 elements, ...)` exit 0. validate: `Valid (5 files)` exit 0 | VERIFIED |
| 5 | Hook step: path-gated, after the instruction budget block, before `Checking formatting...`, fail closed, install command printed | `git diff HEAD~1 HEAD -- .githooks/pre-commit`; `commit-hook.log` grep | Insertion at old line 181, between the budget `fi` and `echo "Checking formatting..."`. Trigger list is one `git diff --cached --name-only --no-renames --` over the 5 pathspecs, and a failed `git diff` exits 1. Missing node: prints `pnpm --dir docs/architecture install`, exit 1. Exit 1 from sync: drift refusal, exit 1. Any other code (incl. 3): prints exit code, `node --version`, the install command, exit 1. `node ... < /dev/null`. Unrelated commits skip the block. Log: line 14 banner, 15 `OK (...)`, 16 `Checking formatting...`, 70 `exit=0` | VERIFIED |
| 6 | Self-test 11/11 against the wired hook, 10 of 11 failing against the unwired hook, only the unrelated-path control passing | `python3 scripts/pre-commit-likec4-selftest.py`; same with `--hook` set to `git show HEAD~1:.githooks/pre-commit` | Green: tests 01 to 11 PASS, `11/11 passed`, exit 0, two `PROBE ok ...: likec4 is not resolvable from the scratch repo` lines (cases 9 and 11). Red (HEAD~1 hook): tests 01 to 10 FAIL (commits succeeded; 06 to 08 "the drift step did not run"), test_11 PASS, `1/11 passed`, exit 1, probes ok. `ls out` afterwards: `fuzz` only, both runs. The filter-mutant run was not repeated; its recorded evidence file was not re-executed here | VERIFIED (mutant direction only read, not re-run) |
| 7 | CI job: contents read, pnpm from packageManager, exact Node, frozen install, self-test, check; both paths lists admit `docs/architecture/**` right after `!docs/**`; additions only | `git diff --numstat HEAD~1 HEAD`; PyYAML parse of the workflow | numstat `50 0 .githooks/pre-commit`, `35 0 .github/workflows/invariant-checks.yml`. Push and pull_request paths both: `**, .*/**, !docs/**, docs/architecture/**, !**/*.md, !.planning/**, .claude/**, ...` so Markdown under docs/architecture is still excluded. Jobs: comment-lint, instruction-budget, public-dashboards, k8s-manifests-valid, k8s-invariants, pre-commit-secret-scan, likec4-model-drift. New job: `permissions: {contents: read}`, checkout@v5, `pnpm/action-setup@v6` with `package_json_file: docs/architecture/package.json`, setup-node@v5 `'24.19.0'`, frozen install, self-test, `node docs/architecture/sync.mjs check` | VERIFIED |
| 8 | No path-shaped reference to the old spike locations outside `.planning/` | `rg -n 'spikes/00[567]\|00[567]-likec4'` over README.md, docs, .claude/CLAUDE.md, scripts, .githooks, .github, k8s | No hits, rg exit 1; second `rg -l` over docs/scripts/hook/workflows/README also empty. Prose-only "spike 006/007" history mentions remain in docs/DIAGRAM_CONVENTIONS.md (lines 87, 126, 128, 155, 162, 190) as provenance, not commands | VERIFIED |
| 9 | verify-diagrams output unchanged; instruction budget unchanged; comment lint clean | `python3 scripts/verify-diagrams.py check`; `verify-instruction-budget.py`; `verify-comments.py check` | Guard: the same single `flowchart-rules docs/learning/kafka-architecture-overview.md:11` violation, exit 1, matching the committed baseline file's tail. Budget: `OK: 4886 bytes within a ceiling of 5100.` Comments: `OK: 330 files, 1280 comment blocks` | VERIFIED |
| 10 | The feature commit ran the real hook to exit 0 with the drift step before formatting; nothing in the Java build changed | `commit-hook.log` grep; `git show --stat HEAD` | Log order as in row 5, `exit=0`. The 39 changed files include no `src/`, `build.gradle` or Gradle file (hook, workflow, docs, spikes, planning evidence, scripts only) | VERIFIED |

Also checked beyond the plan:
- `docs/architecture/.gitignore` contains `node_modules/` and `out/`; `git check-ignore -v` confirms both resolve to its lines 1 and 2.
- `package.json` carries `packageManager: pnpm@11.20.0` and `likec4: 1.60.0`; `pnpm-workspace.yaml` sets `allowBuilds: esbuild: false`, so the frozen install exits 0.
- `sync.mjs` loads likec4 first in `check()`, so missing tooling wins over drift, and exit 3 prints the install command (self-test case 9 passes on a stale model without tooling).
- Docs that tell a reader to run a command (README.md 207-209, docs/LOCAL_DEV.md 35-36, docs/DIAGRAM_CONVENTIONS.md 49-51 and 132-133) all name `docs/architecture` paths.

## Anti-patterns and gaps

No blocker. Notes the SUMMARY mostly records, plus two it does not:

- WARNING-level, known and recorded in the hook header: sync.mjs reads the working tree, not the index, so a regenerated but unstaged generated.c4 passes the hook; CI covers it. Triggers exclude `k8s/platform/traefik` and three Java packages the model cites; CI covers those.
- Not in the SUMMARY: running the drills (`drill.fish`) creates an ignored `docs/architecture/out/` (empty here). Harmless, ignored by the folder's own `.gitignore`.
- Not in the SUMMARY: the self-test's no-node case (test_10) also hides `python3` from the hook, so the comment lint and budget print their "unavailable" warnings in that case. That is the existing hook behavior and does not affect the assertion.
- `.claude/CLAUDE.md` still lists the hook's steps without the new one, and `docs/learning/09-build-quality-and-ci.md` and the wiki copy lag. All are follow-ups recorded in the SUMMARY and were outside the brief.

## Not verified

- The `likec4-model-drift` GitHub Actions job has never run (no push; actionlint not installed). I verified it by YAML parse and by running its steps' commands locally from a clean archive. Its first real run will also be the first test of `pnpm/action-setup@v6` with `package_json_file` and of the self-test on a runner without `trash-put` (it falls back to `shutil.rmtree`).
- The Java tests were not re-run by the commit's hook (`spotlessCheck` and `fastTest` were UP-TO-DATE); irrelevant to this change, which touches no Java.
- Pushing this commit will start deploy.yml (hook, workflow and scripts paths are not in its paths-ignore); nothing was pushed.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
