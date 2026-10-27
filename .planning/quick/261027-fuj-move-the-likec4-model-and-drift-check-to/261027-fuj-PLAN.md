---
phase: quick-261027-fuj
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - docs/architecture/.gitignore
  - docs/architecture/package.json
  - docs/architecture/pnpm-lock.yaml
  - docs/architecture/pnpm-workspace.yaml
  - docs/architecture/sync.mjs
  - docs/architecture/likec4.fish
  - docs/architecture/drill.fish
  - docs/architecture/drill-output.txt
  - docs/architecture/model/spec.c4
  - docs/architecture/model/model.c4
  - docs/architecture/model/deployment.c4
  - docs/architecture/model/views.c4
  - docs/architecture/model/generated.c4
  - .planning/spikes/005-likec4-toolchain-and-views/.gitignore
  - .planning/spikes/005-likec4-toolchain-and-views/package.json
  - .planning/spikes/005-likec4-toolchain-and-views/pnpm-lock.yaml
  - .planning/spikes/005-likec4-toolchain-and-views/pnpm-workspace.yaml
  - .planning/spikes/005-likec4-toolchain-and-views/README.md
  - .planning/spikes/006-likec4-model-of-this-app/.gitignore
  - .planning/spikes/006-likec4-model-of-this-app/likec4.fish
  - .planning/spikes/006-likec4-model-of-this-app/model/spec.c4
  - .planning/spikes/006-likec4-model-of-this-app/model/model.c4
  - .planning/spikes/006-likec4-model-of-this-app/model/deployment.c4
  - .planning/spikes/006-likec4-model-of-this-app/model/views.c4
  - .planning/spikes/006-likec4-model-of-this-app/model/generated.c4
  - .planning/spikes/006-likec4-model-of-this-app/README.md
  - .planning/spikes/007-likec4-code-sync-seam/.gitignore
  - .planning/spikes/007-likec4-code-sync-seam/sync.mjs
  - .planning/spikes/007-likec4-code-sync-seam/drill.fish
  - .planning/spikes/007-likec4-code-sync-seam/drill-output.txt
  - .planning/spikes/007-likec4-code-sync-seam/README.md
  - .planning/spikes/MANIFEST.md
  - .planning/spikes/CONVENTIONS.md
  - .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/lc4.fish
  - docs/DIAGRAM_CONVENTIONS.md
  - docs/LOCAL_DEV.md
  - README.md
  - .githooks/pre-commit
  - .github/workflows/invariant-checks.yml
  - scripts/pre-commit-likec4-selftest.py
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-base-sha.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-baseline.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-after.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-install.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-drill-rerun.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-system-node.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-unwired.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-green.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-filter-mutant.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-gitleaks-selftest.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-paths-filter.txt
  - .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-commit-hook.log
autonomous: true
requirements: [261027-fuj]

estimate:
  tokens: 185000
  raw_tokens: 185000
  tasks: 3
  confidence: low
  per_task:
    task_1: 55000
    task_2: 70000
    task_3: 60000

must_haves:
  truths:
    - "docs/architecture/ tracks the five model/*.c4 files, sync.mjs, likec4.fish, drill.fish, drill-output.txt, package.json, pnpm-lock.yaml, pnpm-workspace.yaml and a .gitignore (node_modules/, out/), all moved with git mv where they existed before."
    - "The three spike folders track only README.md files plus spike 005's smoke/smoke.c4, and hold no untracked or ignored residue (node_modules/, out/)."
    - "`node docs/architecture/sync.mjs check` prints `OK (17 elements, 6 tables, 2 topics, 4 workloads)` and exits 0 on the real tree, and `fish docs/architecture/drill.fish` reproduces docs/architecture/drill-output.txt byte for byte."
    - "`pnpm --dir docs/architecture install --frozen-lockfile` exits 0. With the spike's committed pnpm-workspace.yaml it exits 1 (ERR_PNPM_IGNORED_BUILDS, measured while planning)."
    - "A commit that stages a path under src/main/resources/db/migration, k8s/base, k8s/data or docs/architecture, or KafkaTopics.java, runs the drift step after the gitleaks scan and before spotlessCheck. Drift refuses the commit. A missing Node or a missing install refuses it and prints `pnpm --dir docs/architecture install`. A commit that stages none of those paths never runs the step."
    - "scripts/pre-commit-likec4-selftest.py passes 11 of 11 against the wired hook. It fails 10 of 11 against the unwired hook, where only the unrelated-path control passes. It fails exactly the unrelated-path case against a filter mutant. Every scratch repo it builds without tooling is first probed by running that repo's own sync.mjs, which must exit 3 (likec4 not resolvable from any ancestor); any other result exits the self-test with 2 as an environment error."
    - "invariant-checks.yml has a likec4-model-drift job: contents read, pnpm from package.json's packageManager, an exact Node version, a frozen-lockfile install, the self-test, then the check. Both paths lists admit docs/architecture/**. git diff --numstat shows 0 deleted lines in the workflow and in the hook."
    - "Outside .planning/, no path-shaped reference to the old spike locations remains. Every hit left inside .planning/ is a spike README pointer or history line, or a dated record of a finished task, and the SUMMARY lists each one."
    - "`python3 scripts/verify-diagrams.py check` output and exit code are identical to the baseline taken before any edit. The instruction budget still reads 4886 of 5100 bytes."
    - "The feature commit ran the real .githooks/pre-commit to exit 0, and its log shows the drift step's OK line before `Checking formatting...`."
  artifacts:
    - path: docs/architecture/sync.mjs
      provides: "Drift script at its new home: repo root and model dir from its own location, bare import('likec4'), exit 3 with the install command when likec4 is missing"
    - path: docs/architecture/pnpm-workspace.yaml
      provides: "allowBuilds esbuild false, without which a frozen install exits 1 under pnpm 11"
    - path: docs/architecture/package.json
      provides: "Developer-tooling manifest: likec4 1.60.0 and packageManager pnpm@11.20.0, the single pnpm version source for CI"
    - path: .githooks/pre-commit
      provides: "The LikeC4 drift step, path-gated, fail-closed, between the instruction budget and spotlessCheck"
    - path: .github/workflows/invariant-checks.yml
      provides: "likec4-model-drift job and docs/architecture/** re-included in both paths lists"
    - path: scripts/pre-commit-likec4-selftest.py
      provides: "Runs the real hook in scratch repos under the repo root's ignored out/: 5 refusals, 3 matching passes, 2 missing-tooling refusals, 1 unrelated-path control, plus a probe that each repo built without tooling cannot resolve likec4"
    - path: docs/DIAGRAM_CONVENTIONS.md
      provides: "LikeC4 standard names docs/architecture as the model's home and says where the check is wired"
  key_links:
    - from: ".githooks/pre-commit"
      to: "docs/architecture/sync.mjs"
      via: "`git diff --cached --name-only --no-renames -- <five pathspecs>` non-empty, then `node docs/architecture/sync.mjs check < /dev/null`"
    - from: "docs/architecture/sync.mjs"
      to: "docs/architecture/node_modules/likec4"
      via: "bare import('likec4') resolved beside package.json; ERR_MODULE_NOT_FOUND turns into exit 3 and the install command"
    - from: ".github/workflows/invariant-checks.yml (likec4-model-drift)"
      to: "docs/architecture/package.json and pnpm-lock.yaml"
      via: "pnpm/action-setup@v6 package_json_file, then `pnpm --dir docs/architecture install --frozen-lockfile`"
    - from: ".github/workflows/invariant-checks.yml (on.push.paths, on.pull_request.paths)"
      to: "docs/architecture/**"
      via: "a positive entry placed right after `!docs/**`, so `!**/*.md` still excludes Markdown there"
    - from: "scripts/pre-commit-likec4-selftest.py"
      to: ".githooks/pre-commit"
      via: "--hook copy installed through core.hooksPath in scratch repos under the repo root's ignored out/, where no ancestor holds node_modules (asserted per no-tooling repo by the sync.mjs probe)"
---

<objective>
Move the LikeC4 model, its drift script, its drills and its pinned tooling from spikes 005-007 into
docs/architecture/, so that their old home is gone. Then make a stale model a refused commit (pre-commit
hook step) and a red CI job (invariant-checks). Nothing in the Java build changes.

Purpose: the brief's goal in two sentences, decided with the user on 2026-10-27. The model lives with
the docs instead of in a spike folder, and a code change that makes it stale cannot land unnoticed. The
user acts on this when a commit touching migrations, Kafka topics, k8s workloads or the model is refused
with a DRIFT list, or with the install command.

Output: the moved and rewritten tooling, one hook step, one CI job, one self-test proved red and green,
updated docs and spike pointers, and one feature commit made through the real hook. Nothing is pushed.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@docs/SESSION_LESSONS.md
@.planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-BRIEF.md
@.githooks/pre-commit
@.github/workflows/invariant-checks.yml
@scripts/pre-commit-gitleaks-selftest.py
@docs/DIAGRAM_CONVENTIONS.md
@.planning/spikes/007-likec4-code-sync-seam/sync.mjs
@.planning/spikes/007-likec4-code-sync-seam/drill.fish

The brief is authoritative, and every task traces to one of its constraints (C1 to C8 below). Q is
shorthand for `.planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to`; write the full
path in every command. Every command runs from the checkout root. Run one short command per Bash call,
because the shell hook refuses calls over about 300 characters, any `cd` and `rm`. For anything longer,
write a fish script under `Q/scratch/` and run it by path. Use `trash-put`, never `rm`.

Measured while planning (HEAD 81d5622, 2026-10-27):

- `node .planning/spikes/007-likec4-code-sync-seam/sync.mjs check` prints
  `OK (17 elements, 6 tables, 2 topics, 4 workloads)`, exit 0. The baseline is green.
- **pnpm 11.20.0 (local) exits 1 on a fresh `install --frozen-lockfile` with spike 005's committed
  pnpm-workspace.yaml** (`allowBuilds: esbuild: set this to true or false`), printing
  `[ERR_PNPM_IGNORED_BUILDS] Ignored build scripts: esbuild@0.28.2`. The packages are installed anyway.
  With `allowBuilds: esbuild: false` it exits 0. As committed, the CI install step would fail on its
  first run. Measured in scratch copies under Q, removed afterwards.
- **pnpm/action-setup added pnpm 11 support in v6.0.0** (release notes, `gh release view v6.0.0 -R
  pnpm/action-setup`). verify-rate-limit.yml's `@v4` installs pnpm 10, so the new job cannot copy that ref.
  v6 reads `packageManager` from the file named by `package_json_file`, and fails if a `version` input
  disagrees with it (context7 `/pnpm/action-setup`).
- Node: local `node` is v24.19.0 (nvm). `/usr/bin/node` is v22.22.1, below likec4 1.60.0's declared
  `engines.node >=22.22.3`, and is what a hook run without nvm on PATH would get. Whether likec4 works
  there is unmeasured, and Task 1 measures it.
- `actionlint` is not installed. `fish`, `trash-put` and pyyaml 6.0.3 are.
- `scripts/verify-diagrams.py` reads only files under `docs/diagrams/` plus tracked `*.md`. Any non-allowed
  file under `docs/diagrams/` trips `outside-view-folder`, so a `.c4` there would be flagged.
  `docs/architecture/` is not scanned, except for diagram-path tokens in Markdown. At HEAD the guard
  exits 1 with one pre-existing violation (`docs/learning/kafka-architecture-overview.md:11`), so the
  gate is "output identical to the baseline".
- `scripts/verify-comments.py` lints `.githooks/pre-commit`, `*.yml`, `*.yaml`, `*.py` and `.gitignore`
  files, so the new pnpm-workspace.yaml, pnpm-lock.yaml and .gitignore enter its scope once tracked. It
  does not lint `.mjs`, `.fish` or `.c4`. Rule 14 bans planning ids in comments: quick-task ids,
  `Phase N`, plan numbers, `D-NN`, `T-NN-NN`, artifact names such as `*-PLAN.md`.
- `.dockerignore` excludes `docs`, so `docs/architecture/node_modules` (138 MB in spike 005's copy) never
  enters a Docker build context. Spotless targets `src/**/*.java` only. The wiki lint walks only
  docs/wiki and docs/raw.
- The old-path search (`spikes/00[567]|00[567]-likec4`) outside .planning hits only
  docs/DIAGRAM_CONVENTIONS.md lines 50 and 125. Inside .planning it hits the three spikes' files, this
  task's BRIEF, and four files of the finished task 261027-f8q: its BRIEF, its PLAN, verify/lc4.fish,
  verify/lc4/generated.c4 and verify/png-dimensions.txt. Of those, only verify/lc4.fish is runnable: it
  resolves the likec4 binary through spike 005's node_modules.
- Untracked residue in the spikes: `005/node_modules` (138 MB, ignored only by 005's own .gitignore),
  `005/out` (5.7 MB), `006/out` (7.0 MB) and `007/out`. The last holds the orchestrator's one-off
  `init2.sh` for this task's init, which is no longer needed.
- `.claude/CLAUDE.md` names no moved path, so it is not touched (C3). The instruction budget reads 4886 of
  5100 bytes.
- Five git worktrees live under `.claude/worktrees/`. Each needs its own install before it can commit a
  trigger-path change. Exclude them from repo-wide searches.
- deploy.yml's `paths-ignore` skips docs/**, **/*.md, .planning/** and k8s/**, and nothing else.
  Pushing the feature commit, which touches .githooks/, .github/ and scripts/, will therefore run a full
  deploy. This plan does not push.

Measured in revision (2026-10-27):

- The root `.gitignore` ignores `out/` at any depth (line 25). `git check-ignore -v` confirms it for a
  scratch path below the root `out/`. The root `out/` already exists and holds only api-fuzz output, in
  `out/fuzz/`. No `node_modules` exists at the repo root, `/home/andre/dev`, `/home/andre`, `/home` or
  `/`. This matters because Node's ESM resolver looks for `node_modules` in every ancestor folder of
  the importing file, so a scratch repo under `docs/architecture/out/` would resolve the real install.
- The repo's written action-pinning rule is deploy.yml lines 31-40. First-party GitHub and Docker
  actions stay trusted by tag. A digest pin is required for an action holding production SSH
  credentials (none remain), and for "a non-GitHub action in a job whose token can write", which is
  dependency-submission.yml's gradle/actions under `contents: write`. README.md line 218 states the
  SSH clause. The repo is not uniform: backup-image.yml SHA-pins checkout and docker/* in a
  `contents: read` job holding the Docker Hub token, with no recorded reason, while deploy.yml
  tag-trusts the same docker/* actions with the same kind of token. verify-rate-limit.yml tag-trusts
  `pnpm/action-setup@v4`, holds no secrets and has no permissions block.

Package legitimacy audit (the npm installs in Tasks 1-2; no new package name enters the repo):

| Package | Status | Evidence |
|---|---|---|
| likec4@1.60.0 | VERIFIED | `npm view`: repository github.com/likec4/likec4, homepage likec4.dev, MIT, first published 2023-09-21. Registry `dist.integrity` sha512-VwdH2D...2PhQ== equals the committed lockfile's line 456. |
| 103 transitive packages | pinned | Resolved by likec4's own manifests, not named by this plan. Every one is pinned by `integrity` in the lockfile committed with spike 005, so `--frozen-lockfile` refuses any byte change. |
| pnpm@11.20.0 | VERIFIED | Already the local package manager. In CI it is installed by pnpm/action-setup, the pnpm org's own action. |

No package is `[ASSUMED]` or `[SUS]`, so no legitimacy checkpoint is needed.
</context>

## Approaches considered (.claude/CLAUDE.md, before approval)

### The hook step (C4)

| Approach | Pros / Cons | Why Picked |
|---|---|---|
| A. Run `node docs/architecture/sync.mjs check` when a staged path matches; fail closed, printing the install command, if Node or the install is missing | Pro: one implementation shared by the hook, CI and the drills, so they cannot disagree. Validates the model itself (`likec4 validate` and the computed model's metadata), not only code facts. 1.1 s measured. Con: Node and pnpm become a commit prerequisite for matching commits, and every checkout and worktree needs its own install | Picked (the brief's decision) |
| B. A pure-shell grep check, no Node | Pro: no new prerequisite, nothing to install per worktree. Con: a second extraction implementation that can disagree with sync.mjs. It cannot run LikeC4's parser, so "model does not validate" and inherited `metadata workload` values are invisible to it. It would re-implement the model's semantics in regex | Rejected |
| C. CI only | Pro: zero local cost. Con: this repo commits straight to main (no branches), so CI on push reports drift after it has landed. A gate whose only power is a red badge on main is the post-merge failure the workflow header already rejects for deploy.yml | Rejected as the only gate; kept as the backstop |

### The proof (C6)

| Approach | Pros / Cons | Why Picked |
|---|---|---|
| 1. Run the real hook in scratch git repos, in the style of pre-commit-gitleaks-selftest.py | Pro: exercises what can actually break. That means the pathspec filter, the index git hands the hook for plain, pathspec, `-a` and deletion commits, the step's position and exit wiring, and the printed fix. It also runs in CI. `--hook PATH` makes the red direction one flag. Con: about 30-40 s and roughly 250 lines of Python | Picked |
| 2. Unit-test the step as a shell function sourced from a helper file | Pro: fast, no git. Con: the hook would have to be split into sourced files, which is a structural change to a file whose diff must be additions only. It never tests `GIT_INDEX_FILE` handling or ordering, so it covers something adjacent to the bug class | Rejected |

### Where the model lives (C1)

| Approach | Pros / Cons | Why Picked |
|---|---|---|
| i. `docs/architecture/` | Pro: beside the docs that will embed its views. Already excluded from the Docker context. Not scanned by verify-diagrams (measured). Con: CI's paths list must re-include it after `!docs/**`, which is one line in each list | Picked |
| ii. A new top-level folder (`architecture/`) | Pro: no paths-filter change. Con: `.dockerignore` is a denylist, so a 138 MB node_modules would enter local build contexts until a new line is added. It also separates the model from the docs that cite it | Rejected |
| iii. `docs/diagrams/` | Con: governed by verify-diagrams, whose `outside-view-folder` rule flags any `.c4` there. The brief forbids it | Rejected |

### Smaller choices, each with the alternative it beat

- **pnpm version source:** `"packageManager": "pnpm@11.20.0"` in docs/architecture/package.json, read by
  pnpm/action-setup through `package_json_file`. A `version:` input in YAML was rejected because it would
  be a second copy that can drift, and v6 fails on a mismatch anyway. Cost: pnpm on another machine
  self-switches to 11.20.0, a download.
- **esbuild:** `allowBuilds: esbuild: false`. `true` was rejected because no measured command needs
  esbuild's install script (validate and build worked without it in spike 005, and the check ran in
  spike 007), and running a dependency's install script in CI is attack surface.
- **Action refs:** `actions/checkout@v5` and `actions/setup-node@v5`, as every sibling job in
  invariant-checks.yml uses. `pnpm/action-setup@v6`, not `@v4`, because v4 predates pnpm 11 support
  (measured). Major-tag refs, not SHA pins.
  - The first draft justified this as "the repo reserves SHA pins for actions that carry secrets".
    Re-checked against deploy.yml lines 31-40 and README.md line 218, that summary was wrong.
    deploy.yml's docker/* actions handle the Docker Hub token and stay tag-trusted, so secrets are not
    the line.
  - The written rule SHA-pins a non-GitHub action only in a job whose token can write, or one holding
    production SSH credentials. pnpm/action-setup is non-GitHub. This job's token is `contents: read`,
    set at job level, and the job holds no secrets, so neither clause applies.
    verify-rate-limit.yml already tag-trusts the same action.
  - backup-image.yml's SHA pins carry no recorded reason, so they are not a rule to copy.
  - The step gets a comment recording the trade-off inline, as the README gate table does for
    tag-trusted actions.
  - Rejected alternative: a SHA pin. Dependabot's github-actions entry would maintain it, so it is
    cheap. But constraint C5 says to pin the way the sibling jobs do, and every job in this workflow uses
    a tag.
- **Node in CI:** exact `'24.19.0'`, the version the model was verified with. A floating `'24'` was
  rejected because the workflow header promises each job is a pure function of the commit.
- **Trigger matching:** one `git diff --cached --name-only --no-renames -- <pathspecs>` call, with the
  KafkaTopics.java entry as `:(glob)src/main/java/**/constant/KafkaTopics.java`, and a non-empty result
  means "run". A `while read` loop with `case` patterns was rejected: in POSIX sh it either runs in a
  pipeline subshell or needs a here-doc, and it re-implements path matching that git does natively.
  `--no-renames` makes a file moved out of a trigger root still show its old path.
- **Placement:** after the instruction-budget block, right before `echo "Checking formatting..."`. This
  is after gitleaks and before Gradle (C4), and puts every existing line in its place, so the diff is
  one contiguous insertion.
- **One feature commit:** the move and the wiring land together through one real-hook run, about 4
  minutes. That run also exercises the new step on the real tree, because the commit stages
  docs/architecture/. Per-task commits were rejected: the move without the wiring is a state nobody
  uses, and each commit costs another hook run.
- **Self-test scratch location:** constraint C6 puts scratch checkouts inside the repo, under the task
  directory or an ignored folder. The self-test creates them with `tempfile.mkdtemp` under
  the repo root's ignored `out/`.
  - Not under `docs/architecture/out/`, the first draft's choice. Node's ESM resolver walks every
    ancestor folder for `node_modules`, so a scratch sync.mjs there would load the real
    `docs/architecture/node_modules/likec4`. The no-tooling case would then commit instead of being
    refused, and only the green run would show it.
  - Not under Q: the self-test is permanent and runs in CI, where a task folder means nothing.
  - Not `/tmp`, the gitleaks self-test's choice, because the brief asks for in-repo.
  - The self-test does not trust the planning-time measurement that no ancestor of `out/` holds
    `node_modules`. Every scratch repo built without tooling is probed before its case runs (Task 2,
    step 1), so a stray install anywhere above it, on this machine or a CI runner, is an environment
    error (exit 2), never a false pass.
  - Cleanup follows the brief's wording "deleted afterwards with trash-put" wherever the command exists:
    the self-test removes its scratch repos with `trash-put` when it is on PATH and falls back to
    `shutil.rmtree` only where it is absent (CI has no trash-put). The self-test removes `out/` itself
    only if it created it, and only with `os.rmdir`, which refuses a non-empty folder, so `out/fuzz/` is
    never touched. The CI fallback is the one remaining difference from the brief, and the SUMMARY
    records it.
  - The executor's own ad-hoc scratch (mutant hook, filter emulator, commit script, the handoff file)
    lives in `Q/scratch/` and goes with trash-put.
- **Spike residue:** move what is tracked, trash the ignored install and render output, and `git rm` the
  three spike .gitignore files, since nothing in the spikes writes there any more. 005's smoke model and
  the READMEs stay as evidence (brief table, last row).

## Non-obvious trade-offs

1. **Stated cost (C4):** Node 22.22.3 or later and pnpm 11 become a commit prerequisite beside Docker,
   for commits touching the trigger paths. Every checkout and git worktree needs its own
   `pnpm --dir docs/architecture install`, which took 78 s cold in spike 005. pnpm hard-links from its
   global store, so the disk cost is mostly shared. The five live worktrees under `.claude/worktrees/`
   are refused on such a commit until installed. Unrelated commits are unaffected, and the self-test's
   unrelated-path case proves it.
2. **State invalidation, working tree versus index:** sync.mjs reads files from disk, as spotlessCheck
   and the comment lint do. A partly staged commit is therefore judged by the working tree. For example,
   a regenerated generated.c4 left unstaged passes the hook while the commit carries the stale file. CI
   checks the committed tree and catches it. Checking the index would mean a `git checkout-index` into a
   temp tree, plus cleanup on every exit path of a sh script. The hole is recorded in the hook's own
   Known holes.
3. **Trigger list versus the model's metadata sources:** the model also cites `k8s/platform/traefik` and
   three Java packages (controller, event, activitylog), which are outside the brief's five triggers.
   Renaming one passes the hook. CI runs the check on every push the workflow admits, so CI catches it.
   This is a Known hole, not a silent one.
4. **Fail closed versus an old Node:** a hook run whose PATH lacks nvm gets `/usr/bin/node` v22.22.1.
   If likec4 breaks there, the commit is refused with the exit code and `node --version` printed, and it
   is never skipped. Task 1 measures which outcome happens.
5. **Security, supply chain:** CI now installs 104 npm packages. Mitigations: `--frozen-lockfile`
   (integrity-pinned), a job-level `permissions: contents: read` (the workflow has no permissions block,
   so the default token would otherwise apply), no secrets in the job, and no dependency install scripts
   (esbuild false). The third-party pnpm/action-setup is tag-trusted, which is accepted (T-fuj-02).
6. **Security, the self-test's nested repos:** scratch repos sit inside the real work tree, under the
   root `out/`. A case directory missing its `.git` would let git discovery climb into the real repo,
   where `git add --all` would stage real files. Mitigated by `GIT_CEILING_DIRECTORIES` set to the
   self-test's workdir, by a GIT_*-free environment, and by asserting each repo's `--show-toplevel`
   before any add. Node has an equivalent climb and no ceiling setting: module resolution walks every
   ancestor for `node_modules`. The sync.mjs probe on each no-tooling repo is the guard for that one.
7. **CI cost:** re-including docs/architecture/** makes a model-only push run all seven invariant jobs,
   not just the new one. Each is a pure function of the commit, and they take a few runner minutes.
   deploy.yml still ignores docs/**.
8. **Performance:** the hook gains about 1.1 s plus Node startup on matching commits only, and nothing
   on others. Memory: one Node process of a few hundred MB, briefly, before the Gradle JVMs start.
9. **Not updated, by the brief's constraint:** `.claude/CLAUDE.md` lists the hook's steps and will not
   name the new one (C3 allows touching it only for a moved path). docs/learning/09-build-quality-and-ci.md
   describes the hook with line anchors that were already stale. Both go in the SUMMARY as follow-ups.
   The hook's own failure message carries the install command, so an agent that hits the step learns
   the fix there.

## Data flow (the core mechanism, three sentences)

On every commit the hook asks git, through the index of the commit being made, whether any staged path
falls under the model's fact sources. If one does, `node docs/architecture/sync.mjs check` loads likec4
first, then re-derives tables, topics and workloads from the working tree and compares them with the
model likec4 computes from docs/architecture/model, and exit 0 lets the commit continue to Gradle while
any other code refuses it with the fix printed. CI installs the same tooling from the lockfile and runs
the same script on every push and pull request its paths filter admits.

Tracer-first is not applied: the orchestrator fixed this three-task split, and the move has no
behaviour a user can observe until the hook step exists. Task 1's verify still runs the moved tooling end
to end (install, generate, check, drill).

## Execution model: one executor per task

The orchestrator runs Tasks 1, 2 and 3 in order. Each runs in a fresh executor that starts from this
PLAN and the files on disk, and nothing else. So every task:

- opens with step 0. It states what the task assumes already exists and gives one command that checks
  it. If the output differs, the executor stops and reports BLOCKED with that output. It never redoes or
  undoes an earlier task's work.
- ends at a boundary. Its verify block has been run and read against its `<done>`, its evidence files
  are written, and its facts for the SUMMARY are appended to `Q/scratch/handoff.txt` under a
  `== Task N` heading. Those facts are the ones no evidence file holds: residue sizes, paths someone
  else had staged, and deviations.
- leaves the index exactly as the next task's step 0 expects. Each task's last step says which changes
  it staged and which it left unstaged.

Tasks 1 and 2 never commit and never write the SUMMARY. Task 3 makes the one feature commit, then
writes the SUMMARY from the handoff file and the evidence files. The orchestrator makes the closing docs
commit. Work was rebalanced in revision so the three tasks are closer in size:

- Task 1 moves files and rewrites tooling.
- Task 2 is the hook and its red/green proof.
- Task 3 is the CI job, every Markdown edit and the commit.

Every Markdown edit now sits in one task, so docs/DIAGRAM_CONVENTIONS.md has a single owner. The
dependency order holds: Task 2's self-test needs Task 1's tracked docs/architecture files, and Task 3's
CI job runs Task 2's self-test.

<tasks>

<task type="auto">
  <name>Task 1: Move the model, sync.mjs, drills and tooling to docs/architecture, rewrite their paths, retire the spike residue (no hook, CI or Markdown change)</name>
  <files>docs/architecture/.gitignore, docs/architecture/package.json, docs/architecture/pnpm-lock.yaml, docs/architecture/pnpm-workspace.yaml, docs/architecture/sync.mjs, docs/architecture/likec4.fish, docs/architecture/drill.fish, docs/architecture/drill-output.txt, docs/architecture/model/spec.c4, docs/architecture/model/model.c4, docs/architecture/model/deployment.c4, docs/architecture/model/views.c4, docs/architecture/model/generated.c4, .planning/spikes/005-likec4-toolchain-and-views/.gitignore, .planning/spikes/006-likec4-model-of-this-app/.gitignore, .planning/spikes/007-likec4-code-sync-seam/.gitignore, .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/lc4.fish, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-base-sha.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-baseline.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-after.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-install.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-drill-rerun.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-system-node.txt</files>
  <action>
  Implements C1, C2 and C7, and the residue half of C3. Do not edit `.githooks/pre-commit`, any
  workflow, or any Markdown file in this task. The spike READMEs, MANIFEST.md, CONVENTIONS.md and
  docs/DIAGRAM_CONVENTIONS.md still name the old paths when this task ends, and Task 3 rewrites them.
  Estimate: about 55k tokens. This executor runs Task 1 alone (see "Execution model").

  0. **Precondition.** This task assumes HEAD is the planning base. The spikes still hold the tooling,
     git knows no file under docs/architecture/, and spike 005's install is present, because the
     baselines run through it. Check with one command:
     `git ls-files docs/architecture .planning/spikes/007-likec4-code-sync-seam/sync.mjs; ls .planning/spikes/005-likec4-toolchain-and-views/node_modules/.bin/likec4`
     It must print exactly two lines: the spike 007 sync.mjs path, then the likec4 bin path. Any
     docs/architecture path means an earlier run started the move. In that case, stop and report
     BLOCKED with the output of `git status --short -- docs/architecture .planning/spikes`.

  1. **Baselines, before any edit.** First read `git diff --cached --name-only`, which should be empty.
     If anything is already staged, it is someone else's work. Do not unstage it. Record it in the
     handoff (step 9), and rely on the pathspec commit in Task 3. `mkdir -p Q/scratch`. Write `git rev-parse HEAD` to
     `Q/261027-fuj-base-sha.txt`. Write `Q/scratch/baseline.fish`. For each item below it prints a fixed
     label line, the command's merged output, and `exit=<status>`, all into `Q/261027-fuj-baseline.txt`:
     - `== drift check`: `node .planning/spikes/007-likec4-code-sync-seam/sync.mjs check`
     - `== model validate`: `fish .planning/spikes/006-likec4-model-of-this-app/likec4.fish validate`
     - `== f8q evidence validate`: `fish .planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/lc4.fish lc4 validate`
     - `== smoke validate`: spike 005's `node_modules/.bin/likec4 validate` on its `smoke` folder
     - `== diagram guard`: `python3 scripts/verify-diagrams.py check`

     Labels name the item, never a path, so Task 1's after-file can be diffed against this one. likec4
     prints a wall-clock timestamp on every line and a duration on some, so a raw diff could never be
     empty. Write the filter once as `Q/scratch/norm.fish` and pipe every item's merged output through it,
     in both `baseline.fish` and `after.fish`, so the comparison is exact and not judged by eye:
     - strip ANSI colour codes;
     - delete every line that contains `workspace:` (they echo an absolute path);
     - strip a leading `HH:MM:SS.mmm ` timestamp;
     - strip a trailing duration such as ` 1.2s` or ` 813ms`.

     Read the file: the drift check must say OK with exit 0, or stop and report, because a move is only
     verifiable against a green baseline.

  2. **Move with history (C1).** `mkdir -p docs/architecture`, then `git mv` each of these:
     - spike 006's `model` folder to `docs/architecture/model`, and its `likec4.fish`;
     - spike 007's `sync.mjs`, `drill.fish` and `drill-output.txt`;
     - spike 005's `package.json`, `pnpm-lock.yaml` and `pnpm-workspace.yaml`.

     All of them land directly in `docs/architecture/`, except the model folder. Make no content edit
     before every move is done, so git sees pure renames.

  3. **Tooling manifest (C2).** Write a new `docs/architecture/.gitignore` with `node_modules/` and `out/`.
     The root `.gitignore` has `out/` but not `node_modules/`, and the brief wants both here so the folder
     does not depend on root rules. Keep the file comment-free, or use one plain-text line; the comment
     lint scans it. In package.json:
     - set `name` to `kanban-architecture-model`;
     - keep `private`, `type: module` and `likec4` exactly `1.60.0`;
     - add `"packageManager": "pnpm@11.20.0"`.

     Replace pnpm-workspace.yaml's placeholder with `allowBuilds:` / `esbuild: false`. Give it a comment
     of at most two plain-text lines: the esbuild install script is not needed, because validate, build
     and the drift check run without it, and an undecided entry makes pnpm 11 exit 1. Then run
     `pnpm --dir docs/architecture install --frozen-lockfile </dev/null`, timed with `time`, and save
     the output and exit code to `Q/261027-fuj-install.txt`.
     - If it fails with an outdated-lockfile error (packageManager can add lockfile metadata), run one
       plain `pnpm --dir docs/architecture install`.
     - Read `git diff docs/architecture/pnpm-lock.yaml`: likec4 must stay 1.60.0 with the same integrity,
       and only metadata may change.
     - Rerun the frozen install, which must exit 0. Record which path happened.

  4. **Rewrite sync.mjs for its new home.**
     - `repo` becomes `resolve(here, '../..')`, and `modelDir` becomes `join(here, 'model')`.
     - Delete the `likec4Entry` constant and the now-unused `pathToFileURL` import.
     - Load likec4 with a bare `await import('likec4')`, which resolves through `docs/architecture/node_modules`.
       Wrap it so that an error whose `code` is `ERR_MODULE_NOT_FOUND` prints
       `likec4 is not installed for docs/architecture. Run: pnpm --dir docs/architecture install` to
       stderr and exits 3. Re-throw any other error.
     - **Load likec4 FIRST in `check()`, before the generated-file comparison.** Today the comparison
       (`render()` against the committed `generated.c4`) runs before the import. Left that way, a stale
       model with missing tooling exits 1 and the hook shows a drift list with no install command, which
       breaks the brief's rule that a missing-tooling refusal always prints the install command. Moving
       the import block (with its exit-3 handling) to the top of `check()` makes missing tooling win over
       drift. `generate` does not import likec4 and keeps working without an install. Self-test case 9
       covers the combination (stale model and no tooling). Direction checked: green only for this
       ordering, because the pre-fix form is the spike's committed script, whose paths cannot run from a
       scratch location. State that limit in the handoff. The case 9 assertion that the output does NOT
       contain `generated.c4 is stale` fails by construction on a stale-first ordering.
     - The header usage lines name `node docs/architecture/sync.mjs generate|check [--root DIR]` and the
       exit codes: 0 clean, 1 drift, 2 usage, 3 tooling missing.
     - `render()`'s first line becomes `// GENERATED by docs/architecture/sync.mjs. Do not edit.`
     - Keep `--root` meaning "code facts only, model stays the real one", which drill.fish relies on.
     - No other behaviour changes.

     Run `node docs/architecture/sync.mjs check` once now. It must report `generated.c4 is stale`
     (exit 1), which proves the header is part of the comparison; record that. Then run `generate`, then
     `check`, which must print the OK line with exit 0. `git diff -M HEAD -- docs/architecture/model/generated.c4`
     must show only line 1 changed.

  5. **Rewrite the fish scripts.**
     - likec4.fish: `bin` is `$here/node_modules/.bin/likec4` and `sync` is `$here/sync.mjs`. Keep the
       same six subcommands and the `out/` targets. Before `start|validate|png|site`, if `$bin` is not
       executable, print the install command to stderr and exit 3.
     - drill.fish: `repo` is `(realpath $here/../..)` and `scratch` stays `$here/out/drill-root`. The
       header no longer says "spike 007". Keep each drill's edits exactly as they are.

  6. **Measure the system Node (trade-off 4).** Run
     `/usr/bin/node docs/architecture/sync.mjs check > Q/261027-fuj-system-node.txt 2>&1`, then append
     `exit=$?` and `/usr/bin/node --version`. Either outcome is acceptable. Record it in the handoff
     (step 9).

  7. **Repoint the one runnable reader, then retire the old home's residue (C3).**
     - In the finished task 261027-f8q, change only `verify/lc4.fish`'s `bin` line to resolve
       `$here/../../../../docs/architecture/node_modules/.bin/likec4`, and its comment to "the pinned
       likec4 in docs/architecture". It is the only runnable reader of the moved tooling, and step 8's
       after-file runs it. Its BRIEF, its PLAN, `verify/lc4/generated.c4` and
       `verify/png-dimensions.txt` are dated records, so leave them.
     - Then retire the spike residue, in this order:
       - record `du -sh` of 005's node_modules and of the three `out` folders;
       - trash-put 005's `node_modules`, then 005's, 006's and 007's `out` folders (007's holds the
         orchestrator's spent init2.sh);
       - only then `git rm` the three spike `.gitignore` files.

       The node_modules folder is ignored only by 005's own .gitignore, so trashing it first keeps
       138 MB from showing as untracked.

  8. **Stage new files so the tracked-file gates see them.** `git add` docs/architecture/.gitignore,
     plus every Q evidence file this task wrote, but not `Q/scratch/`. Write `Q/scratch/after.fish`,
     which writes `Q/261027-fuj-after.txt` with the same five labels in the same order, through the
     same `Q/scratch/norm.fish`. The commands are:
     - `node docs/architecture/sync.mjs check`;
     - `fish docs/architecture/likec4.fish validate`;
     - the repointed f8q lc4.fish;
     - `docs/architecture/node_modules/.bin/likec4 validate` on 005's smoke folder;
     - the diagram guard.

     Run it, and run the drill into `Q/261027-fuj-drill-rerun.txt`.

  9. **Hand off, then stop.**
     - Append a `== Task 1` section to `Q/scratch/handoff.txt` with these facts:
       - the install path taken (frozen first, or one lockfile refresh) and its `time` reading;
       - that the first `check` reported `generated.c4 is stale`;
       - the system-Node exit code and version;
       - the `du -sh` sizes trashed in step 7;
       - any path that was already staged before step 1;
       - every deviation from this task as written.
     - Run the verify block and read each line against `<done>`. Fix a failing line before stopping, or
       report BLOCKED with it.
     - Do not write the SUMMARY, and do not commit.
     - End state, which Task 2's step 0 checks:
       - staged: the renames, the three spike `.gitignore` removals, `docs/architecture/.gitignore`, and
         this task's Q evidence files;
       - unstaged: the content edits to the moved files and to f8q's lc4.fish.

       Task 2's self-test copies the files git knows, taking their working-tree content. So it needs
       the 13 docs/architecture paths in the index, which the renames put there, and it sees the
       unstaged edits. Task 3 commits by pathspec, which takes working-tree content either way.

  Rule 14 applies to the .gitignore and pnpm-workspace.yaml comments: plain text, and no planning ids.
  </action>
  <verify>
    <automated>
diff .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-baseline.txt .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-after.txt
fish docs/architecture/drill.fish > .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-drill-rerun.txt 2>&1; diff docs/architecture/drill-output.txt .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-drill-rerun.txt
pnpm --dir docs/architecture install --frozen-lockfile </dev/null >/dev/null 2>&1; echo "exit=$?"
git ls-files .planning/spikes/005-likec4-toolchain-and-views .planning/spikes/006-likec4-model-of-this-app .planning/spikes/007-likec4-code-sync-seam
git status --porcelain --ignored --untracked-files=all -- .planning/spikes/005-likec4-toolchain-and-views .planning/spikes/006-likec4-model-of-this-app .planning/spikes/007-likec4-code-sync-seam
git ls-files docs/architecture
python3 scripts/verify-comments.py check >/dev/null; echo "exit=$?"
python3 scripts/verify-comments-selftest.py >/dev/null; echo "exit=$?"
python3 scripts/verify-instruction-budget.py
    </automated>
  </verify>
  <done>
  - Line 1 prints nothing: the normalized after-file equals the normalized baseline. Any output at all
    is a regression to explain or fix, not to wave through. The drift check reads
    `OK (17 elements, 6 tables, 2 topics, 4 workloads)` with `exit=0` in both files.
  - Line 2 prints nothing: the drill reproduces its recorded output (baseline OK, D1 to D4 caught, D5
    not caught).
  - Line 3 prints `exit=0`.
  - Line 4 prints exactly four paths: 005's README.md and smoke/smoke.c4, 006's README.md and 007's
    README.md. It printed 19 at HEAD.
  - Line 5 prints only staged moves and removals: every line's first column is `R` or `D`. The READMEs
    are not modified yet, because Task 3 edits them. No line starts with `??` or `!!`, so there is no
    untracked or ignored residue in the spikes. At HEAD it printed `!!` lines for node_modules and out.
  - Line 6 lists 13 paths. Lines 7 and 8 print `exit=0`. Line 9's last line reads
    `OK: 4886 bytes within a ceiling of 5100.`
  - `Q/261027-fuj-install.txt` and `Q/261027-fuj-system-node.txt` exist. `Q/scratch/handoff.txt` has
    a `== Task 1` section stating the install path taken and the system-Node outcome.
  - `git log -1 --format=%H` still equals `Q/261027-fuj-base-sha.txt`, because this task made no commit.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Self-test first (red against the unwired hook), then the hook step (green), then a filter mutant (red on the control)</name>
  <files>scripts/pre-commit-likec4-selftest.py, .githooks/pre-commit, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-unwired.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-green.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-filter-mutant.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-gitleaks-selftest.txt</files>
  <behavior>
    - Refused, step ran, DRIFT names the cause [tooling installed]: a new migration creating a table, staged with `git add` and a plain commit, without regenerating.
    - Refused [tooling installed]: a new KafkaTopics constant, as a pathspec commit of an unstaged edit, without a model topic.
    - Refused [tooling installed]: a new k8s/base Deployment, `git add` then `git commit -a`, without a model element.
    - Refused [tooling installed]: `git rm` of the cited k8s/data/postgres/backup-cronjob.yaml.
    - Refused [tooling installed]: a model-only edit under docs/architecture that retitles the dlt topic.
    - Commits and prints the OK tally line [tooling installed]: each of the first three changes together with its model update (regenerated generated.c4, a model topic, an element carrying `metadata workload`).
    - Refused, printing `pnpm --dir docs/architecture install`: a model-consistent trigger change [no tooling, and likec4 proven unresolvable from the scratch repo first], and the same change [tooling installed, no `node` on PATH].
    - Commits without the step's banner [no tooling]: an unrelated path, with no install and no requirement of Node.
  </behavior>
  <action>
  Implements C4 and C6. Work in this order, because the order is the red-then-green proof. Do not edit
  `.github/workflows/invariant-checks.yml` or any Markdown file: they are Task 3's. Estimate: about
  70k tokens. This executor runs Task 2 alone (see "Execution model").

  0. **Precondition.** This task assumes Task 1 is done and nothing of Task 2 exists yet:
     - git knows the 13 docs/architecture paths;
     - the moved drift check passes;
     - the working-tree hook is HEAD's;
     - the self-test file does not exist.

     Check with one command:
     `git ls-files docs/architecture | wc -l; node docs/architecture/sync.mjs check; git diff --quiet HEAD -- .githooks/pre-commit; echo hook-at-head=$?; ls scripts/pre-commit-likec4-selftest.py`
     It must print `13`, then `OK (17 elements, 6 tables, 2 topics, 4 workloads)`, then
     `hook-at-head=0`, then an `ls` "No such file or directory" error. Any other output means Task 1 is
     unfinished or Task 2 already started. Stop and report BLOCKED with the output. Also read
     `Q/scratch/handoff.txt`'s `== Task 1` section, because its deviations may change what this task
     can assume.

  1. **Write the self-test first**, at `scripts/pre-commit-likec4-selftest.py`, in the shape of
     scripts/pre-commit-gitleaks-selftest.py:
     - module docstring (one-line summary, then what it proves, its exit codes and `--hook PATH`);
     - copy its `clean_env`, `run` and `git` helpers rather than importing them, because two self-tests
       sharing module globals would couple their runs;
     - `clean_env` additionally sets `GIT_CEILING_DIRECTORIES` to the self-test's workdir (trade-off 6);
     - a 120 s per-command timeout;
     - PASS/FAIL lines, an `N/M passed` total, and exit 0, 1, or 2 when the environment is unusable.

     It is in comment-lint scope: rule 14, no planning ids.

     - **Preflight** (exit 2 with a message): the hook exists; `node` and `git` are on PATH; the real
       repo's `docs/architecture/node_modules/likec4` exists, else print the install command.
     - **Workdir:** a `tempfile.mkdtemp` folder under the repo root's `out/`, removed in a `finally`
       block by one helper: it runs `trash-put <folder>` when `shutil.which('trash-put')` finds the
       command (local runs, as the brief requires), and falls back to `shutil.rmtree` only where the
       command is absent (CI). The root
       `.gitignore` ignores at line 25. Do not put it under `docs/architecture/out/`. Node resolves a
       bare `import('likec4')` by looking for `node_modules` in every ancestor folder of the importing
       file, so a scratch sync.mjs below docs/architecture would load the real install. The no-tooling
       case would then commit instead of being refused.
       - Note whether `out/` existed, then create it with exist_ok.
       - Use a prefix that starts with `likec4-selftest` and contains a space, as the gitleaks self-test
         does, so the hook's quoting is exercised.
       - After the workdir is removed, call `os.rmdir(out)` only if this run created `out/`.
         rmdir refuses a non-empty folder, so other tools' output, such as `out/fuzz/`, is never
         removed.
     - **make_repo(case, tooling)** builds `<workdir>/<case>/main`.
       - Copy every file listed by `git ls-files -z -- src/main k8s docs/architecture`, run in the real
         repo with a GIT_*-free environment, from the working tree. Copying tracked files only never
         touches an ignored `.env`, and node_modules and out are excluded for free.
       - Stub `scripts/verify-comments.py` and `scripts/verify-instruction-budget.py` to `raise SystemExit(0)`,
         and `gradlew` to `#!/bin/sh` `exit 0` (mode 755).
       - Add a `README.txt`, then `git init`, user config and gpgsign off.
       - Assert `git rev-parse --show-toplevel` equals the repo path, then `git add --all` and the initial
         commit.
       - Only then install the hook under test into `<case>/hooks/pre-commit` (mode 755) and set
         `core.hooksPath`.
       - When `tooling` is true, symlink the repo's `docs/architecture/node_modules` to the real install
         and add `/docs/architecture/node_modules` to `.git/info/exclude`. A dir-only `node_modules/`
         ignore pattern does not match a symlink.
       - **When `tooling` is false, probe before returning**, so the check runs before the case's
         hook run:
         - Run the scratch repo's own `node docs/architecture/sync.mjs check`, with cwd set to the
           scratch repo, stdin from `/dev/null`, the clean environment and the 120 s timeout. This
           attempts the real `import('likec4')` from the scratch repo's sync.mjs, through every
           ancestor folder Node would search.
         - Require exit 3, with `pnpm --dir docs/architecture install` in its stderr. Then print
           `PROBE ok <case>: likec4 is not resolvable from the scratch repo`.
         - Any other result means likec4 resolves from an ancestor of the scratch repo, or sync.mjs
           failed some other way. Print the probe's exit code and output, plus
           `environment: likec4 is resolvable from outside the scratch repo, so the no-tooling cases cannot discriminate`,
           and exit 2. Never count it as a PASS.

         The probe runs in both no-tooling repos (cases 9 and 11), in every direction: red, green,
         mutant and CI.
       - Build `<case>/bin` holding a `docker` stub (`#!/bin/sh`, `exit 0`). It isolates the step under
         test; the secret scan has its own self-test.
       - Every commit runs with `PATH=<case>/bin` prepended to the original PATH, except the no-node case.
     - **Assertions:**
       - refused: non-zero exit, the hook's drift-refusal text and the expected finding substring in the
         output, and HEAD unmoved;
       - committed with check: exit 0, the step's banner present, a line matching
         `^OK \(\d+ elements, \d+ tables, \d+ topics, \d+ workloads\)$`, and HEAD moved;
       - committed without step: exit 0, banner absent, and HEAD moved;
       - missing tooling: non-zero exit, `pnpm --dir docs/architecture install` in the output, and HEAD
         unmoved.

       Each case also asserts that its file edit changed the file's bytes, so a missed replace cannot
       pass as a drift.
     - **The 11 cases.** Each is named `test_...`, and each finding substring is sync.mjs's own wording.
       The bracket before each case is its `tooling` argument. Cases 1 to 8 run with tooling, so a
       refusal there comes from drift and never from a missing install:
       1. [tooling] a new `V9999__selftest.sql` creating `selftest_drift`, then `git add` and a plain
          commit: refused, `generated.c4 is stale`;
       2. [tooling] a `SELFTEST = "kanban.selftest"` constant inserted before KafkaTopics.java's
          closing brace, left unstaged, then `git commit -m x -- <that path>`: refused,
          `topic kanban.selftest is in KafkaTopics.java but not in the model`;
       3. [tooling] `k8s/base/selftest-worker.yaml` as a minimal Deployment named `selftest-worker`,
          `git add`, then `git commit -a`: refused, `k8s Deployment/selftest-worker has no element`;
       4. [tooling] `git rm k8s/data/postgres/backup-cronjob.yaml`, then a plain commit: refused,
          `model claims workload CronJob/postgres-backup`;
       5. [tooling] in docs/architecture/model/model.c4, the title `'kanban.activity.dlt'` replaced
          with `'kanban.activity.deadletter'`: refused,
          `topic kanban.activity.dlt is in KafkaTopics.java but not in the model`;
       6. [tooling] the first case's migration plus `node docs/architecture/sync.mjs generate` run in
          the scratch repo: committed with check;
       7. [tooling] the second case's constant plus a new `docs/architecture/model/selftest.c4` that
          extends `redpanda` with `selftest = topic 'kanban.selftest'`: committed with check;
       8. [tooling] the third case's manifest plus a model file declaring a `system` element with
          `metadata { workload 'Deployment/selftest-worker' }`: committed with check;
       9. [no tooling, probed] a new `V9999__selftest.sql` staged with no regeneration, so the model is
          stale AND the tooling is missing: missing tooling, meaning the output contains
          `pnpm --dir docs/architecture install` and the hook's not-run text, and does NOT contain
          `generated.c4 is stale` (the user must see the install command, not a drift list);
       10. [tooling, no node on PATH] a `// selftest` line appended to views.c4, with `PATH` set to `<case>/bin` only. That folder
           holds symlinks to the resolved `git`, `sh`, `dirname` and `basename`, plus the docker stub.
           Assert `shutil.which('node', path=<case>/bin)` is None, or exit 2 as an environment problem.
           Result: missing tooling, and the output contains `Node is not on PATH`;
       11. [no tooling, probed] `README.txt` edited: committed without step.

  2. **Red against the unwired hook.** Confirm with `git diff --quiet HEAD -- .githooks/pre-commit` that
     the working-tree hook is HEAD's. Run the self-test with the default `--hook` into
     `Q/261027-fuj-selftest-red-unwired.txt`, appending `exit=$?`. Expected: 10 FAIL lines and one PASS,
     the unrelated-path case.
     - The refusal and missing-tooling cases fail because the commit succeeds.
     - The three matching-update cases fail because no OK line is printed. The step never ran, which is
       the false-clean shape the brief forbids.

     If any case other than the unrelated-path one passes here, it does not discriminate: fix the test
     before going on.

  3. **Add the hook step**, as one contiguous insertion after the instruction-budget block's closing `fi`
     and before `echo "Checking formatting..."`. Change no existing line. In order, the step:
     - computes the staged trigger list with one
       `git diff --cached --name-only --no-renames -- src/main/resources/db/migration k8s/base k8s/data docs/architecture ':(glob)src/main/java/**/constant/KafkaTopics.java'`
       call. Line continuations are fine. git reads the GIT_INDEX_FILE it exported, so every commit
       shape is seen.
     - prints nothing at all when that list is empty.
     - otherwise echoes `Checking the LikeC4 model against the code it describes...`.
     - if `command -v node` fails, prints
       `Node is not on PATH, so the LikeC4 drift check cannot run. Install Node 22.22.3 or later, then run: pnpm --dir docs/architecture install`
       and `Commit refused rather than skipping the check.`, then exits 1.
     - otherwise runs `node docs/architecture/sync.mjs check < /dev/null` (SESSION_LESSONS lesson 3) and
       keeps its status.
     - on status 1, prints that it is LikeC4 model drift (see the DRIFT list above). The fix is to update
       docs/architecture/model, or to run `node docs/architecture/sync.mjs generate` when a migration
       changed the tables, then re-stage. The commit is refused. Exit 1.
     - on any other non-zero status, prints
       `LikeC4 drift check did not run to completion (exit N, <node --version>).`, then
       `If its tooling is not installed, run: pnpm --dir docs/architecture install`, then the
       refused-rather-than-skipped line. Exit 1.

     Its comment block follows rule 14 and the hook's existing style.
     - It opens with a one-line summary and a blank comment line.
     - `Decisions:` covers four points: why it fails closed (a check that quietly does not run looks
       like a clean one); why only these paths (unrelated commits need no Node); that `git diff --cached`
       honours GIT_INDEX_FILE; and placement after the cheap checks.
     - `Known holes:` covers two points: the working tree is read, not the index; and metadata sources
       outside the trigger paths (k8s/platform, the cited Java packages) are caught in CI only.
     - It closes with `Falsified if scripts/pre-commit-likec4-selftest.py passes with --hook pointed at a copy without this step.`

     Keep the file mode 100755.

  4. **Green.** Run the self-test into `Q/261027-fuj-selftest-green.txt`, then append `exit=$?` as a
     final line, exactly as step 2 does for the red file. The file's last two lines must be `11/11 passed`
     and `exit=0`, because Task 3's step 0 reads them with `tail -n 2`. Then run `python3 scripts/pre-commit-gitleaks-selftest.py` into `Q/261027-fuj-gitleaks-selftest.txt`.
     It needs Docker and must still pass 7 of 7. Its throwaway repos stage only non-trigger files, so a
     filter that over-triggers would fail it for lack of an install. That makes it a second, independent
     proof of the filter.

  5. **Red on the control (filter mutant).** Write `Q/scratch/make-mutant.fish`. It copies the hook to
     `Q/scratch/hook-filter-mutant` and replaces the pathspec arguments of that one `git diff --cached`
     call with the single pathspec `.`. It exits non-zero if the copy is byte-identical to the hook, or if
     the banner line is missing from it. Run the self-test with `--hook` set to that copy, into
     `Q/261027-fuj-selftest-red-filter-mutant.txt`. Expected: exactly one FAIL, the unrelated-path case,
     refused for the missing install. This proves the only case that passed against the unwired hook can
     fail.

  6. **Hand off, then stop.**
     - `git add scripts/pre-commit-likec4-selftest.py` and this task's four Q evidence files. Leave the
       hook edit unstaged.
     - Append a `== Task 2` section to `Q/scratch/handoff.txt` with these facts:
       - each direction's totals, and the names of its failing cases;
       - the gitleaks self-test total;
       - the PROBE lines seen;
       - every deviation from this task as written.
     - Run the verify block and read each line against `<done>`. Fix a failing line before stopping, or
       report BLOCKED with it.
     - Do not write the SUMMARY, and do not commit.
     - End state, which Task 3's step 0 checks: the hook step is in the working tree, the self-test and
       the four evidence files are staged, and `invariant-checks.yml` is still HEAD's.
  </action>
  <verify>
    <automated>
python3 scripts/pre-commit-likec4-selftest.py; echo "exit=$?"
rg -c '^FAIL' .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-unwired.txt
rg '^PASS' .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-unwired.txt
rg '^FAIL' .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-red-filter-mutant.txt
tail -1 .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-gitleaks-selftest.txt
git diff --numstat HEAD -- .githooks/pre-commit
git ls-files -s .githooks/pre-commit
python3 scripts/verify-comments.py check >/dev/null; echo "exit=$?"
rg -c '^PROBE ok' .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-green.txt
find out -maxdepth 1 -name 'likec4-selftest*'
    </automated>
  </verify>
  <done>
  - Line 1 prints 11 PASS lines, two `PROBE ok` lines, `11/11 passed` and `exit=0`.
  - Line 2 prints 10 and line 3 prints only the unrelated-path case: the unwired direction.
  - Line 4 prints exactly one FAIL, the unrelated-path case: the mutant direction.
  - Line 5 prints `7/7 passed`.
  - Line 6 shows a deletions column of `0` for the hook.
  - Line 7 starts with `100755`.
  - Line 8 prints `exit=0`.
  - Line 9 prints `2`: the probe ran in both no-tooling repos and found likec4 unresolvable. A `0`
    means the probe never ran, which is not a pass.
  - Line 10 prints nothing: every scratch repo was removed. The root `out/` still holds whatever it held
    before, such as `out/fuzz/`.
  - `git log -1 --format=%H` still equals `Q/261027-fuj-base-sha.txt`, because this task made no commit.
  </done>
</task>

<task type="auto">
  <name>Task 3: CI job, the docs that point at the new home and describe the gate, then the feature commit through the real hook, cleanup and the SUMMARY</name>
  <files>.github/workflows/invariant-checks.yml, .planning/spikes/005-likec4-toolchain-and-views/README.md, .planning/spikes/006-likec4-model-of-this-app/README.md, .planning/spikes/007-likec4-code-sync-seam/README.md, .planning/spikes/MANIFEST.md, .planning/spikes/CONVENTIONS.md, docs/DIAGRAM_CONVENTIONS.md, docs/LOCAL_DEV.md, README.md, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-paths-filter.txt, .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-commit-hook.log</files>
  <action>
  Implements C5, the prose half of C3, and the one feature commit. Estimate: about 60k tokens. This
  executor runs Task 3 alone (see "Execution model").

  0. **Precondition.** This task assumes Tasks 1 and 2 are done and the feature commit has not been
     made:
     - the green self-test evidence reads 11 of 11;
     - the hook carries the step;
     - the workflow is still HEAD's;
     - HEAD is the planning base.

     Check with one command:
     `tail -n 2 .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-selftest-green.txt; rg -c 'Checking the LikeC4 model' .githooks/pre-commit; git diff --quiet HEAD -- .github/workflows/invariant-checks.yml; echo wf-at-head=$?; git log -1 --format=%s`
     It must print `11/11 passed`, `exit=0`, `1`, `wf-at-head=0`, and a subject that does not start
     with `feat(quick-261027-fuj)`. Any other output means an earlier task is unfinished or this task
     already started. Stop and report BLOCKED with the output. Read `Q/scratch/handoff.txt` in full:
     its Task 1 and Task 2 sections feed the SUMMARY (step 10) and may record deviations this task must
     honour.

  1. **CI job (C5).** In `.github/workflows/invariant-checks.yml`:
     - insert `"docs/architecture/**"` immediately after `"!docs/**"` in both the push and the
       pull_request paths lists;
     - add one comment line above the push list's entries saying docs/architecture/ is re-included
       because the model there is checked against the code. Add a line rather than edit the existing
       comment, so the diff stays additions only.
     - append a job `likec4-model-drift` after `pre-commit-secret-scan`, with a `name:` stating the
       invariant and `runs-on: ubuntu-latest`;
     - give the job `permissions: contents: read`;
     - its steps, in order:
       - `actions/checkout@v5`;
       - `pnpm/action-setup@v6` with `package_json_file: docs/architecture/package.json` and no
         `version`. Add two comment lines. The first says v6 is the first major that installs pnpm 11,
         and the version is read from the packageManager field. The second says it is trusted by tag
         like the other actions here, because this job's token is read-only and the job holds no
         secrets. It follows deploy.yml's rule: a non-GitHub action is SHA-pinned only in a job whose
         token can write. See "Action refs" above.
       - `actions/setup-node@v5` with `node-version: '24.19.0'`;
       - `pnpm --dir docs/architecture install --frozen-lockfile`;
       - `python3 scripts/pre-commit-likec4-selftest.py`, commented as running ahead of the gate like
         every job above;
       - `node docs/architecture/sync.mjs check`.

     YAML comments are linted, so rule 14 applies. This job cannot run locally. Instead, write
     `Q/scratch/paths-filter.py`, a throwaway emulator of the ordered filter where the last match wins,
     `**` crosses `/` and `*` does not. It:
     - asserts that the push and pull_request lists are identical, in both HEAD's file (`git show`) and
       the working-tree file;
     - prints old and new verdicts for these paths, into `Q/261027-fuj-paths-filter.txt`:
       - docs/architecture/model/model.c4
       - docs/architecture/sync.mjs
       - docs/architecture/pnpm-lock.yaml
       - docs/architecture/README.md
       - docs/ARCHITECTURE.md
       - docs/diagrams/process/activity-pipeline.mmd
       - src/main/resources/db/migration/V1__init.sql
       - k8s/base/redpanda/redpanda.yaml
       - .planning/quick/x/y.txt
       - .claude/CLAUDE.md
       - .github/workflows/invariant-checks.yml

     Only the three non-Markdown docs/architecture paths may flip, from excluded to included.

  2. **Docs that point at the new home (C3).**
     - Each spike README (005, 006, 007) gets, right under its `# Spike` heading, a one-paragraph pointer:
       which of its files moved to `docs/architecture/` on 2026-10-27, and that the policy lives in
       docs/DIAGRAM_CONVENTIONS.md, section "LikeC4 standard".
     - Rewrite each README's How to Run commands to the new paths. 005's commands become
       `pnpm --dir docs/architecture install` and `docs/architecture/node_modules/.bin/likec4 validate`
       on 005's smoke folder. 006 uses `fish docs/architecture/likec4.fish ...`. 007 uses
       `node docs/architecture/sync.mjs ...` and `fish docs/architecture/drill.fish`.
     - Leave the Investigation Trail and Results sections as written. They are history.
     - CONVENTIONS.md: rewrite the "LikeC4 models" paragraph and the likec4 Tools paragraph to the
       docs/architecture paths, keeping the facts about Chromium 1248, async `computedModel()` and
       node_modules not being ignored repo-wide.
     - MANIFEST.md: add one line under the likec4-diagram-sync idea saying the model, sync.mjs and the
       tooling moved to docs/architecture/, and that spikes 005-007 keep their READMEs, and 005 its smoke
       model, as evidence. Leave the table rows unchanged.
     - docs/DIAGRAM_CONVENTIONS.md, section LikeC4 standard:
       - The sentence that places the model in a spike says instead that the model lives in
         `docs/architecture/model/`, driven by `docs/architecture/likec4.fish`
         (`start|validate|png|site|check|sync`), and that its pinned tooling installs once per checkout
         with `pnpm --dir docs/architecture install`.
       - The Checks section's first line says the drift script is `docs/architecture/sync.mjs`, built in
         spike 007.
       - Delete the "Outside this change" bullet about moving the model out of the spikes, because this
         task does it.
       - Keep every "spike 006" and "spike 007" attribution of a measurement as it is. Do not edit the
         wiki copy: originals are authoritative (.claude/CLAUDE.md).

  3. **Docs that describe the gate.**
     - docs/DIAGRAM_CONVENTIONS.md's Checks section gains a `**Wired:**` bullet. The hook runs
       `node docs/architecture/sync.mjs check` when a staged path is under the five triggers, and refuses
       when Node or the install is missing. invariant-checks' `likec4-model-drift` job runs the self-test
       and the check. A changed metadata source outside the triggers is caught in CI only.
     - docs/LOCAL_DEV.md's Prerequisites gains one bullet: Node 22.22.3 or later and pnpm 11, needed
       only to commit changes under the five triggers; run `pnpm --dir docs/architecture install` once
       per checkout and per git worktree.
     - README.md's pre-commit paragraph: rewrite its "four gates" sentence to list the gates the hook
       actually runs, in order:
       - gitleaks;
       - the comment policy;
       - the instruction budget, which the sentence already omits;
       - the LikeC4 drift check, only for the five trigger paths, which needs Node and the install;
       - spotlessCheck;
       - fastTest.

       "None of the four" becomes "None of them". Leave the CI table alone.

  4. **Gate before the commit.** A failure found here costs seconds, while the same failure found by
     the hook costs a 4-minute run. Run verify lines 7 and 9 to 13, plus
     `git diff --numstat HEAD -- .githooks/pre-commit .github/workflows/invariant-checks.yml`, which
     stands in for line 8 before HEAD moves. Also run `python3 scripts/verify-instruction-budget.py`.
     Each must already read as `<done>` says, and the budget must read 4886 of 5100. Fix anything that
     does not before step 5.

  The feature commit edits the hook and CI, so it runs the real `.githooks/pre-commit`: gitleaks, the
  comment lint, the budget, the new LikeC4 step, spotlessCheck and fastTest, about 4 minutes. Never use
  `gsd_run query commit` for it: that helper's 30 s limit kills the hook mid-run and orphans its process.
  Never use `--no-verify` for it. Docker must be running for gitleaks and the Testcontainers in fastTest.

  5. Read `free -h`. Write `Q/scratch/commit-paths.txt` with one path per line, built from this plan's
     files_modified. It includes the old spike paths of every moved and deleted file, because a pathspec
     commit needs both sides of a rename. It excludes `Q/261027-fuj-commit-hook.log`, the BRIEF and the
     PLAN, which the closing docs commit carries. `git add` any new file on the list that git does not
     know yet. Then read `git diff --cached --name-status -M` and compare it with the list. A staged path
     outside the list is someone else's: leave it staged, name it in the SUMMARY, and rely on the
     pathspec.

  6. Write `Q/scratch/commit-msg.txt`, a conventional message that says why. The subject is
     `feat(quick-261027-fuj): gate LikeC4 model drift at commit and in CI from docs/architecture`. The
     body explains three things: the model and tooling left the spike folders; a stale model is now
     refused at commit and in CI; and Node and pnpm are now a prerequisite for commits touching the five
     trigger paths. End it with the session's attribution trailer.

  7. Write `Q/scratch/commit.fish`. It writes its own PID to `Q/scratch/commit.pid`, then runs
     `git commit -F <msg> --pathspec-from-file=<paths> < /dev/null`, writing stdout and stderr to
     `Q/261027-fuj-commit-hook.log`, then appends `exit=$status`. Run it with `run_in_background`.
     The expected duration is about 4 minutes. Bound the wait by the log's mtime against `date`: if no new
     output appears for about 12 minutes, treat the commit as hung. Then kill the fish PID and its
     `git commit` and hook children by PID (`ps -o pid,ppid,args`), confirm `git log -1` did not move,
     and report BLOCKED with the log tail.

  8. **If fastTest fails** with "daemon has been stopped" under low free memory (SESSION_LESSONS lesson 9),
     do not retry with `--no-verify`, because that needs fresh user authorization for a feature commit.
     Report BLOCKED with the log tail and the `free -h` reading. If the LikeC4 step itself refuses, that
     is a real finding: fix it and recommit. Never bypass it.

  9. After `exit=0`:
     - read the log and confirm `Checking the LikeC4 model against the code it describes...` and the OK
       line appear before `Checking formatting...`;
     - read `git show -M --name-status --format=%s HEAD`;
     - run `docker ps -a --filter label=org.testcontainers --format '{{.Names}}'` and remove only
       containers this run left;
     - trash-put `docs/architecture/out/` if it is present. It holds only the drill's and likec4.fish's
       ignored scratch.
     - `find out -maxdepth 1 -name 'likec4-selftest*'` must print nothing. If a killed self-test run
       left a folder there, trash-put that folder alone, and never `out/` itself or `out/fuzz/`.
     - leave `docs/architecture/node_modules` in place, because the hook now needs it.

  10. **SUMMARY, then the last cleanup.**
      - Write `Q/261027-fuj-SUMMARY.md` as `<output>` specifies, from `Q/scratch/handoff.txt`, the
        evidence files and this task's own results.
      - For the old-path classification, run
        `rg -n --hidden -g '!**/node_modules/**' 'spikes/00[567]|00[567]-likec4' .planning`. Searching
        `.planning` alone keeps the worktrees under `.claude/worktrees/` out of the results. Classify
        every hit.
      - Only after the SUMMARY is written, trash-put `Q/scratch/`, which holds the handoff file. Do not
        commit the SUMMARY.
      - The closing docs commit (PLAN, SUMMARY, STATE, commit-hook.log) is the orchestrator's, not this
        executor's. The SUMMARY repeats this note for it. That commit has no Java, migration or k8s
        file, so `--no-verify` is allowed (.claude/CLAUDE.md), but only after a manual secret scan:
        - read the pinned image with `rg -o -m1 "ghcr.io/gitleaks/[^']+" .githooks/pre-commit`;
        - pipe `git diff --cached -- <those paths>` into
          `docker run --rm -i <image> stdin --redact --no-banner --verbose --exit-code 2`;
        - commit only on exit 0. On 2 it found a secret, and any other code means it did not run: stop
          and report either one.

      Do not push. Pushing the feature commit will run deploy.yml, because .githooks/, .github/ and
      scripts/ are not in its paths-ignore. Say so in the SUMMARY.
  </action>
  <verify>
    <automated>
tail -n 1 .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-commit-hook.log
rg -n 'Checking the LikeC4 model|^OK \(|Checking formatting' .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-commit-hook.log
git show -M --name-status --format=%s HEAD
git status --porcelain --ignored --untracked-files=all -- docs/architecture .githooks .github/workflows/invariant-checks.yml scripts/pre-commit-likec4-selftest.py docs/DIAGRAM_CONVENTIONS.md docs/LOCAL_DEV.md README.md
test -e .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/scratch; echo "scratch-exists=$?"
docker ps -a --filter label=org.testcontainers --format '{{.Names}}'
rg -n --hidden -g '!**/node_modules/**' 'spikes/00[567]|00[567]-likec4' docs scripts .github .githooks .claude/CLAUDE.md README.md
git diff --numstat "$(cat .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-base-sha.txt)" HEAD -- .githooks/pre-commit .github/workflows/invariant-checks.yml
python3 -c "import yaml;print(sorted(yaml.safe_load(open('.github/workflows/invariant-checks.yml'))['jobs']))"
cat .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-paths-filter.txt
python3 scripts/verify-comments.py check >/dev/null; echo "exit=$?"
diff <(sed -n '/^== diagram guard/,$p' .planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-baseline.txt) <(echo "== diagram guard"; python3 scripts/verify-diagrams.py check 2>&1; echo "exit=$?")
command -v actionlint && actionlint .github/workflows/invariant-checks.yml; echo "actionlint-exit=$?"
find out -maxdepth 1 -name 'likec4-selftest*'
    </automated>
  </verify>
  <done>
  - Line 1 prints `exit=0`.
  - Line 2 shows, in this order, the LikeC4 banner, `OK (17 elements, 6 tables, 2 topics, 4 workloads)`
    and `Checking formatting...`.
  - Line 3 prints the feature subject and exactly 12 lines starting with `R`. The renames are the five
    model files, sync.mjs, drill.fish, drill-output.txt, likec4.fish, package.json, pnpm-lock.yaml and
    pnpm-workspace.yaml. The rest are `A` (the new .gitignore, the self-test and evidence), `D` (the
    three spike .gitignore files) and `M` lines, all on the commit-paths list.
  - Line 4 prints only `!! docs/architecture/node_modules/`.
  - Line 5 prints `scratch-exists=1`.
  - Line 6 prints nothing.
  - Line 7 prints nothing (rg exit 1). At HEAD it printed docs/DIAGRAM_CONVENTIONS.md lines 50 and 125.
  - Line 8 shows a deletions column of `0` for both files.
  - Line 9 lists the six existing jobs plus `likec4-model-drift`.
  - Line 10 shows only the three non-Markdown docs/architecture paths flipping, and identical
    push/pull_request lists in both versions.
  - Line 11 prints `exit=0`, and line 12 prints nothing.
  - Line 13: actionlint is not installed (measured while planning), so it prints `actionlint-exit=1`.
    Record "actionlint not run". Do not pull an unpinned image to get it.
  - Line 14 prints nothing.
  - HEAD's subject is the feature commit, and nothing was pushed. `Q/261027-fuj-SUMMARY.md` exists and
    is uncommitted.
  - The SUMMARY says plainly that the GitHub Actions job itself was not executed. It was checked by YAML
    parse, the filter emulation and the action refs compared with the siblings, and its first real run
    is the user's push.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| npm registry to CI and dev machines | 104 third-party packages enter through `pnpm install` |
| third-party action to CI runner | pnpm/action-setup runs code in the job by mutable tag |
| scratch repos to the real work tree | the self-test runs git inside an ignored folder of the real repo |
| hook to the commit | the hook decides whether a stale model lands |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-fuj-01 | Tampering | likec4-model-drift job's `pnpm install` | medium | mitigate | `--frozen-lockfile` against integrity-pinned lockfile; job-level `permissions: contents: read`; no secrets; `allowBuilds: esbuild: false` so no dependency install script runs |
| T-fuj-02 | Elevation of privilege | `pnpm/action-setup@v6` by tag | medium | accept | Job holds a job-level `contents: read` token and no secrets. deploy.yml's written rule (lines 31-40) SHA-pins a non-GitHub action only in a job whose token can write, or one holding production SSH credentials, and neither applies. verify-rate-limit.yml already tag-trusts this action. The step's comment records the trade-off inline |
| T-fuj-03 | Repudiation | hook step silently skipped when Node or the install is missing (false clean) | high | mitigate | Step fails closed with the install command; self-test cases for no install and no Node prove refusal, and were seen to fail against the unwired hook. Each no-tooling scratch repo is probed first (its own sync.mjs must exit 3), so an install reachable from an ancestor folder cannot make the no-install case pass falsely; the self-test exits 2 instead |
| T-fuj-04 | Tampering | self-test git discovery climbing from a scratch dir into the real repo | high | mitigate | `GIT_CEILING_DIRECTORIES` set to the workdir; GIT_*-free env; `--show-toplevel` asserted equal to the scratch repo before any `git add` |
| T-fuj-05 | Information disclosure | closing docs commit with `--no-verify` skips gitleaks | medium | mitigate | Manual stdin scan with the hook's pinned image on exactly the staged paths; commit only on exit 0 |
| T-fuj-06 | Denial of service | commits to trigger paths blocked on machines or worktrees without Node/pnpm | low | accept | Stated cost (trade-off 1); unrelated commits unaffected (self-test control); hook and LOCAL_DEV.md print the fix |
| T-fuj-SC | Tampering | npm installs | high | mitigate | Package legitimacy audit in context: likec4@1.60.0 VERIFIED (registry integrity equals the lockfile), no new package names; no [ASSUMED]/[SUS] package, so no blocking checkpoint |
</threat_model>

<verification>
- Every brief acceptance line holds against the real tree:
  - docs/architecture holds the 13 tracked files, and the spikes hold no live copy (Task 1, lines 4-6);
  - `sync.mjs check` is OK and the drill reproduces its output (Task 1, lines 1-2);
  - the hook step exists, and the self-test was run in all three directions with both probes seen
    (Task 2, lines 1-4 and 9);
  - the CI job exists with the stated triggers (Task 3, lines 9-10);
  - the existing hook steps and CI guarantees are unchanged, with deletions 0 (Task 2 line 6 before the
    commit, Task 3 line 8 against the base SHA after it);
  - no old path remains outside .planning (Task 3, line 7);
  - the diagram guard output equals its baseline (Task 1 line 1, Task 3 line 12), and the budget is
    4886 of 5100 (Task 1 line 9, Task 3 step 4).
- The real hook ran to exit 0 on the feature commit and exercised the new step (Task 3, lines 1-3).
- Each task's step 0 passed before it changed anything, and Tasks 1 and 2 left HEAD at the base SHA.
- Could not verify here: the GitHub Actions job's execution, which first runs on the user's push, and
  actionlint, which is not installed.
</verification>

<success_criteria>
- `git ls-files docs/architecture` lists 13 files, and `git ls-files` for the three spikes lists 4.
- `node docs/architecture/sync.mjs check` exits 0 with the 17/6/2/4 tally.
- The self-test reads 11/11 on the wired hook, 10 FAIL on the unwired hook and 1 FAIL on the filter
  mutant, and the evidence files are committed.
- One feature commit, made through the real hook to exit 0, and no push.
</success_criteria>

<output>
Task 3's executor creates
`.planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/261027-fuj-SUMMARY.md` after the
feature commit (Task 3, step 10), from `Q/scratch/handoff.txt` and the evidence files. Tasks 1 and 2 do
not write it. The orchestrator commits it in the closing docs commit. It must state:

- the install path taken (frozen first, or one lockfile refresh) and the cold or warm install time;
- the system-Node outcome;
- each self-test direction with its counts and the case names that failed, and the two PROBE lines;
- the classified list of every remaining old-path hit inside .planning/;
- the residue sizes trashed;
- any path someone else had staged, and every deviation the handoff recorded;
- the one remaining difference from the brief's wording "deleted afterwards with trash-put": the
  self-test's scratch repos under the root `out/` go to `trash-put` when it is on PATH, and are removed
  with `shutil.rmtree` only where it is absent, because the self-test also runs in CI, which has no
  trash-put. The executors' own scratch (`Q/scratch/`, `docs/architecture/out/`) went with trash-put;
- the direction checked for the Task 1 import-before-compare ordering fix (green only, with the reason
  stated in Task 1 step 4);
- that the CI job and actionlint were not run here;
- that pushing triggers deploy.yml;
- the two follow-ups: .claude/CLAUDE.md's hook-step list, and docs/learning/09's stale hook section.
</output>
