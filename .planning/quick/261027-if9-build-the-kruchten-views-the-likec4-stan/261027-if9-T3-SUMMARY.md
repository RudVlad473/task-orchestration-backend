# Quick 261027-if9, Task 3 summary: the development view, generated from package imports

Commit: `8b7e379` on main (not pushed), made through the real `.githooks/pre-commit`. Its log (`261027-if9-T3-commit.log`)
ends `exit=0` and shows the LikeC4 banner, then `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges,
2 topics, 4 workloads)` and the cycle line, before `Checking formatting...`. `git show --stat` lists exactly the eight paths of
the task. `spotlessCheck` and `fastTest` were both UP-TO-DATE (no Java changed). Testcontainers: `docker ps -a` lists the same
8 containers as before this run (newest 2026-10-26), none new. Nothing else was staged.

## What changed

- `docs/architecture/sync.mjs`: `packages()` reads the top-level folders under `src/main/java/com/vrudenko/kanban_board` and the
  import edges with `^import (static )?com\.vrudenko\.kanban_board\.([a-z_]+)` (no `i` flag, as pkg.fish does), with no edge for
  a file's own package. `render()` writes `extend kanban { pkg_{name} = component ... #generated ... }` and one
  `-[imports]->` edge per pair, tagged generated, no label. Header line 2 names the import statements. The tally gains
  `{n} packages, {m} package edges`. A Tarjan pass finds the strongly connected components of two or more packages, and `check`
  prints `package cycles (reported, not gated): ...` after the tally on both outcomes (groups joined by ` | `); it adds no
  problem, so it cannot change the exit code. The stale message now reads `(the migrations or the package imports changed)`.
- `docs/architecture/model/spec.c4`: relationship kind `imports` (dotted, vee head, slate, a notation line); the parser accepted it.
- `docs/architecture/model/views.c4`: `process` lists `kanban.api, kanban.publisher, kanban.consumer`; new
  `view development of kanban` tagged `#development` with `include kanban.* where kind is component`. The first form the plan
  names was accepted by the parser, so the fallback (include all, exclude the three) was not needed.
- `docs/architecture/model/generated.c4`: 14 package components and 38 import edges.
- `docs/architecture/drill.fish`: `fresh` copies the whole `kanban_board` tree in one `cp`; drills D8 and D8b.
- `scripts/pre-commit-likec4-selftest.py`: `test_14_added_package_import_is_refused` (pathspec commit of KafkaTopics.java);
  docstring says eight drift refusals.
- `.githooks/pre-commit`: "Known holes" now records that a Java import change is caught in CI's likec4-model-drift job only,
  and why; the refusal's second line says "when a migration or a package import changed".
- `docs/architecture/drill-output.txt`: regenerated; every change is named in the green file.

## Evidence (all in the quick folder)

- `261027-if9-T3-red.txt`: before the rule, D8 and D8b `exit=0`, `FAIL: test_14` with 13/14 passed, no `package cycles` line
  (`rg -c` exit 1), 0 `-[imports]->` lines in generated.c4 against pkg.fish's 38.
- `261027-if9-T3-green.txt`: cross-check (generated edge set equals a fresh pkg.fish run, 38 = 38, diff exit 0, 14 components,
  14 folders; side-by-side list), check timings, `free -h`, the gate (`GATE PASS`, 14/14), and the drill-output notes. D8 and
  D8b now `exit=1` stale; D8b's cycle line lists `exception, handler` beside the 8-package group; D5 still `exit=0`.
- `261027-if9-T3-png.txt`: sizes, the five traces, the two other layouts, the verdict.
- `261027-if9-T3-commit.log`: the hook run.
- `scratch/pkg-now.txt` (pkg.fish at the T2 tree, identical to f8q's `pkg-result.txt`, `git status` of the f8q folder empty),
  `scratch/pkg-after.txt`, `scratch/gen-edges.txt`, `scratch/pkg-edges.txt`, `scratch/lay-lr/` and `scratch/lay-tb/` (the two
  other renders).

## The cycle line (the hard equality)

On the real tree the line lists exactly one group: `base, config, dto, entity, mapper, repository, security, service`. That is
the required 8 packages, no more groups, no fewer members. Not BLOCKED. I also confirmed it by hand from pkg-now.txt's edges
(config -> security -> service -> config; entity <-> base, dto -> entity and config -> dto tie in dto, base, entity; service ->
mapper -> dto and service -> repository -> entity pull in mapper and repository).

## Timing of `check`

Before (T2 tree): 925, 935, 927 ms, median 927. After: 910, 958, 1039 ms, median 958. Ratio 1.03, far under the 2x limit.

## Readability verdict (from renders I looked at)

Default layout, 8950x5272 (natural 4475x2636): meets the plan's rule (14 labels legible, five traces unambiguous: activitylog ->
constant, service -> repository, service -> config, service -> mapper, service -> exception). Honest caveats: the first trace
runs beside two other long arcs into constant; the three arcs at the left side of entity (base, dto) and the eight arrows into
constant are only distinguishable by their tiny arrowheads; the picture does not show the 8-package cycle as a group. I viewed
every PNG downscaled, never at natural size, and had no crop tool, so "legible at natural size" is inferred from the downscaled
view. Layouts tried: `autoLayout LeftRight 120 110` (worse: long looping arcs, 7792x8938) and `autoLayout TopBottom 200 160`
(same topology as the default, 12500x6552, edges drew dotted). Default kept. Details in `261027-if9-T3-png.txt`.

## Unexercised or unverified

- The root-class exclusion (an import of `KanbanBoardApplication` gives no edge) is unexercised by any fixture: no source file
  imports a root-level class today, so neither the pkg.fish cross-check nor any drill touches it.
- Why the `imports` edges drew dotted in the `-f development` render with spacing but looked solid in the first full render: not
  established.
- A package-edge change under src/main/java is not a hook trigger, so the hook cannot refuse it; I did not run CI's
  likec4-model-drift job.
- The self-test case 14 exercises a trigger path (KafkaTopics.java); a Java file outside the trigger paths was not committed
  through the hook, which is the recorded hole.

## Deviations

- The comment-length advisory fired on sync.mjs blocks of 11 to 14 lines; the comment lint `verify-comments.py` does not cover
  `.mjs` and passed.
- One note on D8: adding `constant -> mapper` puts `constant` into the cycle group (constant joins through mapper). The plan only
  predicted D8b's group; D8's group is recorded in the green notes.
- No deviation from the plan's steps otherwise. The plan's cwd and sentinel guards for worktrees do not apply (sequential on main).

## Left for later tasks

Tasks 4 to 6. The scratch scripts (`gate.fish`, `commit.fish`, `wait.fish`, `red.fish`, `render.fish`, `regen-drill.fish`,
`t3-*.fish`, `pkgcheck.fish`, `layout.fish`) and `docs/architecture/out` stay for them. PLAN, SUMMARY and STATE were not committed.
The untracked `.planning/quick/261024-*` files and the evidence files were left alone.
