# Quick 261027-if9, Task 6 summary: the LikeC4 standard brought in step with what is built

Commit: `106ed4a` on main (not pushed), path `docs/DIAGRAM_CONVENTIONS.md` only (`git show --stat`: 1 file changed, 98 insertions,
22 deletions). Made with `git commit --no-verify`, which the project CLAUDE.md allows for a docs-only commit; so there is no hook
log, no LikeC4 banner and no gitleaks run on the staged diff (the diff holds prose and numbers, no credential-shaped value).
`261027-if9-T6-commit.log` ends `exit=0`.

## What moved in the doc (section `## LikeC4 standard` only; the wiki copy is untouched)

- Entities and rules: Relationship names the structural kinds `fk` and `imports` (generated only; the one authored `fk` edge is
  the Spring Session pair). Viewpoint names the five tags, says `index` carries none, and says `check` refuses a view without
  exactly one tag and a viewpoint without a view.
- Viewpoints table: Logical and Development provenance cells rewritten. Notes (a) to (c) extended and a new note (d) for the
  development view: regex FK forms followed and not followed (DO block or function, renames, DROP TABLE not observed), the
  information_schema route kept documented and not wired, why a wrong password has no view, sequence adopted (`--seq`) with the
  T4 widths 1838, 1500 and 1838 recorded and not judged, the unexercised root-class exclusion, the 8-package cycle.
- Checks: new "Built in 261027-if9" bullet (viewpoint-tag rule, coverage rule, generated FK and package edges, cycle report);
  Drills bullet now says 12 of 12 drift drills are caught (D1 to D4, D6, D6b, D7, D7b, D7c, D8, D8b, D9), D6 to D9 seen red
  first, D5 the negative control, self-test 15/15 with 9 drift refusals; Wired bullet records that a Java import change is caught
  in CI only and why. "Every view has a viewpoint tag" is gone from the proposed list, which now holds the unknown-metadata-key and
  server-title checks.
- New section "Built in 261027-if9 (2026-10-27)" after the V6 list: one row per view (id, tag, provenance, natural size from the
  final render in `261027-if9-T5-png.txt`) and the `development` readability verdict with the three layouts tried. The V rows are
  unchanged.
- Undefined, not adopted, outside: the 38-edge and information_schema "Unmeasured" bullets replaced by the measured results; the
  "three unbuilt checks" line now names the two that remain. `rg -n 'every view has a viewpoint tag" check|three unbuilt checks|does
  not choose'` prints nothing (rg exit 1); the tag grep counts 9 matches, the `261027-if9` grep 14.

## Numbers and where each comes from

Checked again now: `node docs/architecture/sync.mjs check` prints `OK (33 elements, 6 tables, 4 foreign keys, 14 packages,
38 package edges, 2 topics, 4 workloads)` and the cycle line with the 8 packages, exit 0. The gate (check, drill diff against
drill-output.txt, self-test 15/15, validate, comment lint) ran before the doc edit and ended `GATE PASS`
(`261027-if9-T6-gate-pre.txt`). Drill counts come from `docs/architecture/drill-output.txt` (baseline `exit=0`, D5 `exit=0`, the
other twelve `exit=1`). The other figures are copied from the T2, T3, T4 and T5 evidence files named in the doc: 4 FKs equal to
`verify/fk-result.txt`, 38 edges equal to pkg.fish, check median 958 ms, the layout sizes, the PNG sizes.

## Checks run by hand (the hook did not run)

- Instruction-file budget: `OK: 4886 bytes within a ceiling of 5100` (exit 0).
- Comment lint: `OK: 330 files, 1280 comment blocks` inside the gate, before the edit; the doc is Markdown and the lint covers
  `.py` and the hook only.
- Wiki evidence check: `check_evidence.py docs` output before and after the edit is identical (`diff` exit 0): 0 fidelity
  suspects, 20 evidence errors, 51 unreferenced raw files. The plan said one "no Raw field" error per migrated article is the
  baseline; I recorded the totals as they are and did not investigate the 20.

## Deviations

- None to the plan's steps. `.githooks/pre-commit` was bypassed with `--no-verify` as the orchestrator's constraints allow.
- The "Drills" bullet says D6 to D9 were each seen red; D1 to D4 predate this change and I did not claim a red file for them.

## Not done or not verified

- Not re-run after the doc edit: the gate (the edit touches no file it reads).
- The development view's readability verdict is as weak as T3 recorded it: judged from downscaled images, never at natural size.
- markdownlint was not run on the doc; no such step exists in the hook.

## Cleanup (done after this summary was written)

- `trash-put` of `QDIR/scratch` and `docs/architecture/out`: exit 0. `find` for `likec4-selftest*` under docs/architecture
  (excluding node_modules) prints nothing. `docs/architecture/node_modules` was left for the hook.
- Testcontainers: 8 containers, newest created 2026-10-26, none from this run.
- `git status --short` shows only the pre-existing untracked 261024 files and the untracked quick folder (PLAN, SUMMARYs,
  evidence files) for the orchestrator to commit. The scratch scripts (gate.fish, commit.fish, T*-msg.txt and the rest) are
  gone with the folder; the evidence files they produced stay.
