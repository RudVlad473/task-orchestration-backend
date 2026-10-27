# Quick 261027-if9, Task 1 summary: viewpoint tags and the untagged-view check

Commit: `6a549ee` on main (not pushed), made through the real `.githooks/pre-commit`. Its log
(`261027-if9-T1-commit.log`) ends `exit=0`, shows the LikeC4 banner and then `OK (17 elements, 6 tables,
2 topics, 4 workloads)` before `Checking formatting...`. `git show --stat` lists exactly the six paths
of the task. `spotlessCheck` and `fastTest` were both UP-TO-DATE (no Java changed), so the hook finished
well under the 4-minute worst case.

## What changed

- `docs/architecture/model/spec.c4`: five viewpoint tags `logical`, `process`, `development`,
  `physical`, `scenarios`.
- `docs/architecture/model/views.c4`: `#logical` on data, `#process` on process, `#physical` on
  production, `#scenarios` on signin; index carries none.
- `docs/architecture/sync.mjs`: a `VIEWPOINTS` constant and a check loop over `computed.views()` that
  skips index and reports `view {id} carries no viewpoint tag` or `view {id} carries {n} viewpoint tags`.
  New `--model DIR` option for both modes (default: the folder beside the script); a missing folder
  exits 2 with the usage line; the header comment documents it.
- `docs/architecture/drill.fish`: `fresh` copies the real model to `$scratch/model`, `run` passes
  `--root $scratch --model $scratch/model`, new drills D6 (tag stripped) and D6b (second tag), header
  names D5 as the negative control.
- `scripts/pre-commit-likec4-selftest.py`: OK_TALLY loosened to `^OK \(\d+ elements(, \d+ [a-z ]+)+\)$`,
  new `test_12_untagged_view_is_refused`, docstring count updated to six drift refusals.
- `docs/architecture/drill-output.txt`: regenerated; only the D6 and D6b sections were added.

## Evidence (all in the quick folder)

- `261027-if9-T1-baseline.txt`: gate at the starting HEAD, GATE PASS, self-test 11/11, empty drill diff.
- `261027-if9-T0-png.txt`: PNG baseline sizes.
- `261027-if9-T1-red.txt`: before the rule existed, D6 and D6b print `exit=0` (not caught), D5 also
  `exit=0`, self-test `FAIL: test_12_untagged_view_is_refused` with 11 PASS (`11/12 passed`, exit 1).
- `261027-if9-T1-green.txt`: after the rule, GATE PASS, `12/12 passed`; notes list the drill-output diff
  (only the D6 and D6b sections), `free -h`, and the PNG comparison. D6 reports
  `view process carries no viewpoint tag`, D6b `view process carries 2 viewpoint tags`.
- `261027-if9-T1-png.txt`: sizes identical to T0 for all five PNGs (data 1292x1912, index 1708x2408,
  process 2466x3072, production 4082x2482, signin 4970x524).
- `261027-if9-T1-commit.log`: the hook run.

## Deviations and what I could not verify

- Rule 3 note, no behaviour change: the plan's "`git status --short docs/architecture/model` prints
  nothing after a drill" literally cannot hold while the task's own edits to spec.c4 and views.c4 are
  uncommitted. I checked the equivalent: after the drills the status lists only those two files (model.c4
  and generated.c4 untouched) and `rg -c` of the four tag lines in views.c4 still prints 4, so the drills
  edited only the scratch copy.
- The PNG files were not opened with the Read tool. A tag does not change a layout, and every dimension
  matches the T0 baseline; byte equality was not compared because the T0 files were overwritten by the
  re-render.
- The red file shows D5 (negative control) at `exit=0` in both red and green, as intended.
- The plan's cwd and sentinel guards for worktrees do not apply (sequential run on main); the commit
  went to `main` as the orchestrator directed.
- Testcontainers: `docker ps -a --filter label=org.testcontainers` lists 8 containers, all created
  2026-09-25 to 2026-10-26, none during this run (the hook's `fastTest` was up to date, so none start).
  I left them alone.
- Memory at commit time: 8.7Gi total, 1.0Gi free, 4.5Gi available, swap 2.0Gi full. The hook finished
  without a daemon failure.
- The comment-length advisory fired on the `sync.mjs` header (12 lines); the comment lint gate
  (`verify-comments.py`) passed and does not cover `.mjs`.

## Left for later tasks

Tasks 2 to 6. Scratch scripts (`scratch/gate.fish`, `commit.fish`, `render.fish`, `red.fish`,
`baseline.fish`, `wait.fish`) stay in the quick folder for them; `docs/architecture/out` holds the T1
render and is trashed in Task 6.
