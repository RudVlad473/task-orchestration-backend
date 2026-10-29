---
phase: quick-261029-sra
plan: 01
subsystem: architecture-site
tags: [likec4, sync.mjs, development-view, folders, drills]
requires:
  - phase: architecture-views
    provides: sync.mjs checks, drills.tsv, the scenario folders from 261029-kyd
provides:
  - site folders ordered 1 Physical to 6 Development, held by checkFolderOrder
  - development view drawn as four layers with 12 edges, held by checkPackagesDrawn
affects: [docs/architecture, docs/DIAGRAM_CONVENTIONS.md, scripts/pre-commit-likec4-selftest.py]
tech-stack:
  added: []
  patterns: [view-only grouping over generated components, mutant copy of the checker to prove a rule bites]
key-files:
  created: []
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/model/views.c4
    - docs/architecture/model/monitoring.c4
    - docs/architecture/model/scenarios-app.c4
    - docs/architecture/model/scenarios-platform.c4
    - docs/architecture/drills.tsv
    - docs/architecture/drill-output.txt
    - scripts/pre-commit-likec4-selftest.py
    - docs/DIAGRAM_CONVENTIONS.md
decisions:
  - "Layering is done in the view only (groups, excludes, with-blocks); generated.c4 and element ids are untouched"
  - "security goes in Web and config in Application, with the evidence recorded in DIAGRAM_CONVENTIONS.md note (d)"
  - "The view description keeps the security reason and drops the config reason to stay under the 70-word View cap"
metrics:
  duration: not measured (no start time was recorded)
  completed: 2026-10-29
status: complete
actuals:
  tokens: 10400
  tasks: 3
  commits: 1
plan_head_before: bbc5d8d93ea552ece02cb38ee0357ab7d612c6d6
plan_head_after: 723ea3b1991a6c5934e91665c34c89ecd4eb9c4a
---

# Phase quick-261029-sra Plan 01: Reorder the architecture site and layer the development view Summary

The site's folders now read 1 Physical, 2 Process, 3 Logical, 4 Application behaviour, 5 Platform and delivery, 6 Development, and the development view draws 14 packages in four layer groups with 12 edges instead of 38.

## Outcome

One local commit, `723ea3b`, made through the full pre-commit hook. Nothing was pushed. generated.c4 and spec.c4 are byte-identical to HEAD (`git diff --quiet` exit 0).

The request said 42 edges; the export and `sync.mjs check` both say 38, so 38 is the before figure.

## Evidence per task

All outputs are in `261029-sra-evidence.txt` in this folder.

**Task 1 (tracer): folder order.**
- Red: the new sync.mjs on the HEAD model exits 1 with 19 folder lines plus the order line.
- Green: the new sync.mjs on the new model exits 0, same tally (62 elements) and the same cycle line.
- Mutant: a copy of sync.mjs with the physical and process entries of FOLDER_ORDER swapped exits 1 on the new model with the order line as the only problem, so the order rule and not the folder rule holds the order.
- Validate passes, the self-test prints 20/20, the comment lint passes.

**Task 2: layered development view.**
- Before (HEAD): 14 nodes, 38 edges, JSON bounds 4440 x 2580, PNG 8992 x 5272 (4496 x 2636 natural).
- After: 18 nodes (14 packages plus 4 group nodes, every package parented), 12 edges, JSON bounds 4000 x 978, PNG 8112 x 2068 (4056 x 1034 natural).
- Edges: 4 red solid upward edges (config to security, entity to config, base to entity, dto to entity), 1 amber layer skip (security to entity), 7 plain dotted. No edge targets the shared kernel, and none runs from controller, handler or security to repository.
- Each cycle's hops are listed with their generated.c4 line numbers in the evidence file.
- Coverage rule: with the handler dropped from the Web group, the new sync.mjs exits 1 ("view development does not draw package kanban.pkg_handler"); a copy of sync.mjs with only the checkPackagesDrawn call deleted exits 0 on the same model. Direction checked: fails with the rule, passes without it.

**Task 3: gate.**
- drill.fish output differs from the recorded drill-output.txt only on the planned lines (D9 label, new D9b block, D29, D29b, D29c, D29d); every other drill, the baseline, D30 and D30b are byte-identical. D29 also gains one order line because removing the application folder's label empties that folder.
- Site built with CI's command: exit 0, `likec4-root` present, leak scan exit 0 (`OK targets=13`), leak-scan fixture tests 81 of 81 pass.
- New labels in the bundle: 1 Physical 3, 2 Process 1, 3 Logical 1, 4 Application behaviour 8, 5 Platform and delivery 6, 6 Development 1. Old labels (3 Development, 5a, 5b, 4 Physical, 1 Logical): 0 each.

## What the rendered view looks like

I Read both PNGs, downscaled to 2000 px wide (the after image 4.06x, the before image 4.5x), never at natural size.

- Before: 14 boxes joined by long looping arcs; edges are told apart only by arrowheads, and eight arrows converge on constant. Unreadable as a dependency picture.
- After: all 14 package names, the four group titles and the five edge labels are legible. The Shared kernel group sits to the right of the stack. The dto and base red edges cross the Persistence group on the way to entity, the dto label straddles the border of the Shared kernel group, and the amber security to entity arc is the longest edge in the picture. The default layout needed no `autoLayout`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] View description shortened to the 70-word cap**
- **Found during:** Task 2
- **Issue:** The plan's description content (including the config placement reason) came to 84 words, over the 70-word View cap in DIAGRAM_CONVENTIONS.md.
- **Fix:** The description is now 69 words and keeps the security reason; the config reason lives in the layer key in DIAGRAM_CONVENTIONS.md note (d).
- **Files modified:** docs/architecture/model/views.c4
- **Commit:** 723ea3b

No other deviation. All nine files changed are in the plan's `files_modified`; the evidence file is committed with the planning commit.

## Not verified

- The left navigation panel was not opened. The order was observed on the landing page's card gallery (each card is titled `<folder> / <view title>`), which showed index, then 1 Physical x3, 2 Process, 3 Logical, 4 Application behaviour x8, 5 Platform and delivery x6, 6 Development x1. The panel's order is held by checkFolderOrder, which reads LikeC4's computed folder order.
- Natural-size legibility of the development view, and the LikeC4 text floor, which is undefined.
- The pre-commit hook's `fastTest` was UP-TO-DATE (no Java changed), so it did not run tests this time.
- The site was not published; a push rebuilds production through Flux and was left to the owner.

## Known Stubs

None.

## Threat Flags

None. The development view's title, description and edge labels name only packages, layers and LayeringArchTest, and the CI-command build passed the leak scan.

## Working tree state

Code commit `723ea3b` holds the nine repo files. `docs/architecture/out/*` and `docs/architecture/site/out` were removed. Untracked files left alone: `.planning/quick/261024-*` (10 text files). `milestone.lock` was not touched.

## Self-Check: PASSED

- Commit `723ea3b` is on main and an ancestor of HEAD.
- `git diff --quiet HEAD -- docs/architecture/model/generated.c4 docs/architecture/model/spec.c4` exits 0.
- Evidence file present in this folder.
