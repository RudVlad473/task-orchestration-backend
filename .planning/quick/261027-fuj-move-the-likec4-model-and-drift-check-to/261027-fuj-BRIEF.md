# Brief: move the LikeC4 model and drift check into docs/, wire the check into the hook and CI

Decided with the user on 2026-10-27 (the user said "go" after this was proposed in chat; the Gradle
question was settled as "scripts own invariants, Gradle owns JVM work"). The previous task,
`261027-f8q`, wrote the standard into `docs/DIAGRAM_CONVENTIONS.md` (section `## LikeC4 standard`) and
listed "moving the model out of `.planning/spikes/`" as outside that change. This task is that move,
plus the wiring.

## Goal in two sentences

The LikeC4 model, its drift script and its tooling live in `docs/architecture/` instead of a spike
folder, and a code change that makes the model stale is refused at commit time and in CI. Nothing in
the Java build changes.

## What moves (inputs)

| Today | Role |
|---|---|
| `.planning/spikes/006-likec4-model-of-this-app/model/*.c4` (spec, model, deployment, views, generated) | the model; `generated.c4` is written by the script |
| `.planning/spikes/006-likec4-model-of-this-app/likec4.fish` | wrapper: `start|validate|png|site|check|sync` |
| `.planning/spikes/007-likec4-code-sync-seam/sync.mjs` | drift script: `generate` and `check [--root DIR]` |
| `.planning/spikes/007-likec4-code-sync-seam/drill.fish` and `drill-output.txt` | the drift drills (proof that the check discriminates) |
| `.planning/spikes/005-likec4-toolchain-and-views/package.json`, `pnpm-lock.yaml`, `pnpm-workspace.yaml` | likec4 1.60.0 pinned; pnpm; PNG export additionally needs Playwright Chromium revision 1248 (`chromium-headless-shell`) |
| `.planning/spikes/005-.../smoke/smoke.c4` | a smoke model; stays as spike evidence, does not need to move |

`sync.mjs` currently imports likec4 through a relative path into spike 005's `node_modules` and
computes the repo root and model path from its own location. Both must be rewritten for the new home;
once it sits beside a `package.json`, a normal `import('likec4')` resolves.

## Constraints

1. **Location:** `docs/architecture/`, not `docs/diagrams/` (the Mermaid guard `scripts/verify-diagrams.py`
   governs that folder). Verify what the guard flags before choosing; if `docs/architecture/` is also
   scanned, say so and adapt. Use `git mv` so history follows.
2. **Tooling manifest:** `docs/architecture/package.json` is developer tooling, separate from anything
   shipped. It carries its own `.gitignore` (`node_modules/`, `out/`) because `node_modules/` is NOT
   ignored repo-wide. The repo has no root `package.json`.
3. **A migration ends when its old home is gone** (the user's standing rule): after the move no live
   copy remains under `.planning/spikes/`. Each spike README that named a moved file says in its header
   what replaced it, and a search for the old paths ends with every hit either gone or a pointer to the
   replacement. Update `docs/DIAGRAM_CONVENTIONS.md` where it says the model lives in spike 006 or that
   `sync.mjs` is spike 007's, and `.planning/spikes/MANIFEST.md` and `CONVENTIONS.md` where they name
   moved files. The instruction-file budget is 4886 of 5100 bytes (`scripts/verify-instruction-budget.py`);
   touch `.claude/CLAUDE.md` only if it already names a moved path, and do not exceed the budget.
4. **Pre-commit hook** (`.githooks/pre-commit`, a `/bin/sh` script that runs gitleaks, then the comment
   lint, the instruction budget, spotless and `fastTest`, about 4 minutes in total): add a step AFTER the
   gitleaks scan and BEFORE the Gradle steps, because cheap checks should fail in seconds. It runs only when
   a staged path touches `src/main/resources/db/migration`, `src/main/java/**/constant/KafkaTopics.java`,
   `k8s/base`, `k8s/data` or `docs/architecture`.
   - **No silent false clean.** If Node or the installed tooling is missing, the step FAILS and prints the
     exact install command (`pnpm --dir docs/architecture install`). It does not skip. The user's standing
     lesson: a check that quietly does not run looks identical to a clean run.
   - **Stated cost** (must appear in the plan's trade-offs): Node and pnpm become a commit prerequisite
     beside Docker, and every checkout and git worktree needs its own install.
5. **CI** (`.github/workflows/invariant-checks.yml`): add the same check, with Node and pnpm set up. READ
   that workflow's header comments first: its trigger is an ordered `paths` list that currently ends in
   `"!docs/**"`, which would exclude `docs/architecture/`, and its comments explain why the list is ordered
   rather than `paths-ignore`. Keep every existing guarantee. Pin actions the way the sibling workflows do
   (look at how they reference actions). The scan must install with the lockfile as written
   (`--frozen-lockfile`), not resolve versions fresh.
6. **Proof, red then green.** Add a self-test in the style of `scripts/pre-commit-gitleaks-selftest.py`
   (read it first) or the smallest equivalent that runs the REAL hook against a scratch checkout, covering:
   (a) a staged migration, topic or workload change WITHOUT the matching model change is refused;
   (b) the same change WITH the regenerated or updated model passes;
   (c) missing tooling fails loudly with the install command.
   The plan and SUMMARY state which direction each case was checked (against the unwired hook it must pass
   wrongly or not run; against the wired hook it must refuse). Scratch checkouts live inside the repo,
   under the task directory or an ignored folder, and are deleted afterwards with `trash-put`.
7. **Do not** touch the Java build or add a Gradle task. Do not move PNG or site rendering into the hook
   (slow, needs a Playwright browser). Keep the regex table extraction in the gate; the scratch-Postgres
   foreign-key route stays a documented option and is not wired.
8. **Out of scope:** migrating any Mermaid diagram, retiring `verify-diagrams.py`, the scenario-layout
   choice (sequence versus TopBottom), and the three unbuilt checks listed in the standard.

## Hazards measured earlier in this session

- `python3 scripts/verify-diagrams.py check` already exits 1 at HEAD with one violation
  (`flowchart-rules`, `docs/learning/kafka-architecture-overview.md:11`); it is pre-existing and
  unrelated. Gate on output identical to a baseline taken before any edit.
- The guard's dangling-reference rule only fires on diagram-folder paths ending in `.mmd`, `.png` or
  `.svg`; it will not catch a wrong `.c4` path, so verify moved paths by command, not by trusting it.
- On this machine hooks refuse a Bash call over about 300 characters, any `cd`, and `rm` (use
  `trash-put`). Write a script file for anything longer and run it by path.
- `git commit --no-verify` is allowed for docs and planning-only commits (`.claude/CLAUDE.md`), but THIS
  change edits the hook and CI, so its feature commit must run the real hook. The hook takes about 4
  minutes: run it in the background with a bounded wait (no new output for about 3x the expected time
  means hung; kill by PID). Never use `gsd_run query commit` for that commit: its 30-second limit kills the
  hook mid-run and leaves an orphaned hook process (seen earlier today, PID 98879).
- Shared index: other quick tasks' untracked files sit in this tree (261024-nvj, 261024-q91). Stage named
  paths only and read `git diff --cached --name-only` before every commit.
- Measured in spike 007: `sync.mjs check` runs in 1.1 s and needs `likec4` for `computedModel()` (which
  is async in 1.60.0); `generate` rewrites `generated.c4` from the migrations.
- A first `pnpm install` of likec4 took 78 s cold on this machine. pnpm reported
  `ERR_PNPM_IGNORED_BUILDS: esbuild`; `validate` and `build` still worked.

## Acceptance (what the verifier should check against the real tree)

- `docs/architecture/` holds the model, `sync.mjs`, `likec4.fish`, the drills, `package.json`, the lockfile
  and a `.gitignore`; `.planning/spikes/00{5,6,7}` hold no live copy of a moved file.
- `node docs/architecture/sync.mjs check` exits 0 on the real repo, and `drill.fish` reproduces the
  recorded drill output (baseline OK, four drifts caught, renamed class not caught).
- The hook step and the CI job exist, in the places and with the triggers above, and the self-test has
  been run in both directions.
- The existing hook steps and CI guarantees are unchanged (diff shows additions only, except where the
  paths list had to change).
- `verify-diagrams.py` output unchanged from its baseline; instruction budget still within its ceiling.
