---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 08
subsystem: infra
tags: [likec4, architecture-model, github-actions, gap-closure, wr-05]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: publish-the-site dynamic view and 03-SCENARIO-EVIDENCE.md (plans 03-04, 03-07)
provides:
  - publish-the-site step 4 and description scoped to the site workflow's own production job and registry token
  - delivery view that names every image-pushing workflow (deploy.yml, backup-image.yml, architecture-site.yml) with no count
  - evidence row 4 re-cited with one verbatim line per clause across three workflows
  - 03-08-prose-proof.txt, the one-shot record of the source assertions and build gates
affects: [03.1-model-description-pass, 04 leak review]

actuals:
  tokens: 3264
  tasks: 2
  commits: 2
plan_head_before: 74e50d43fbee9387d0bedfe834f77e53e5848cd8
plan_head_after: 853bae1bab80d8670c5ae18c94b2ea0793d1020e

tech-stack:
  added: []
  patterns:
    - "Prose claims about workflows are proved once by an rg set diff against .github/workflows, recorded in a proof txt"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-08-prose-proof.txt
  modified:
    - docs/architecture/model/scenarios-platform.c4
    - docs/architecture/model/delivery.c4
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-SCENARIO-EVIDENCE.md

key-decisions:
  - "Token clause says 'registry token', never the secret's identifier, to keep Secret names out of the public model (Phase 4 leak scan targets them)"
  - "No standing regression test: no gate reads prose, and a sync.mjs prose rule would key on wording"

requirements-completed: [SCEN-08]

duration: ~10min (start time not captured at the first command; estimated from the first gitleaks timestamp)
completed: 2026-10-29
status: complete
---

# Phase 3 Plan 08: Publish-the-site prose made true (G-03-2 / WR-05) Summary

**Six model sentences no longer claim the site's publish job is the only production job or count the image-pushing workflows; each workflow-name claim is checked against `.github/workflows` and the rebuilt site carries the new wording and none of the old.**

## Performance

- **Duration:** ~10 min (estimate, see frontmatter)
- **Completed:** 2026-10-29
- **Tasks:** 2 of 2
- **Files:** 4 (3 modified, 1 created)

## Commits

| Task | Commit | Paths |
|------|--------|-------|
| 1 (tracer): step 4, evidence row 4, proof record | `d8599d4` | scenarios-platform.c4, 03-SCENARIO-EVIDENCE.md, 03-08-prose-proof.txt |
| 2: view description and delivery view | `853bae1` | scenarios-platform.c4, delivery.c4, 03-08-prose-proof.txt |

Both used `git commit --no-verify` with an explicit pathspec, as the plan and `.claude/CLAUDE.md` allow for docs-only commits. gitleaks and `sync.mjs check` were run by hand first. Nothing was pushed.

## Accomplishments

- **Step 4** (`scenarios-platform.c4:38`) now reads "the only job of this workflow in the production environment, whose registry token deploy.yml and backup-image.yml also use".
- **View description** (`scenarios-platform.c4:33`): "the publish job is the only job of this workflow that holds the registry token".
- **ci description** (`delivery.c4:7`) drops "Two workflows" and lists deploy.yml, backup-image.yml and architecture-site.yml with no count.
- **dockerhub description** (`:14`), **repo-to-ci title** (`:42`) and **ci-to-dockerhub title** (`:43`) now include backup-image.yml and the backup image.
- **Evidence row 4** carries the new step text and 9 cites with 9 verbatim quote groups; the section still has 8 rows; a dated sentence was added after the paragraph on line 5.

## Proof record (every line of 03-08-prose-proof.txt)

```
TREE 74e50d4
STEP4-ENV diff_exit=0
STEP4-TOKEN diff_exit=0
SITE-ENV count=1
SYNC task1 exit=0
BUILT-STEP4 new_files=3 old_files=0
DELIVERY diff_exit=0
DELIVERY-REL diff_exit=0
OLD-PHRASES model_lines=0
BUILT L38 new_files=3 old_files=0
BUILT L33 new_files=2 old_files=0
BUILT L7 new_files=4 old_files=0
BUILT L14 new_files=4 old_files=0
BUILT L42 new_files=5 old_files=0
BUILT L43 new_files=6 old_files=0
LEAKWORDS lines=0
SYNC task2 exit=0
```

The `BUILT` new_files counts equal the planning-time baselines (3, 2, 4, 4, 5, 6 for L38, L33, L7, L14, L42, L43). The record is my own report; the gates that re-run against the committed tree are the proof.

## gitleaks (hand-run before each commit, `--staged`, `.gitleaks.toml`)

- Task 1: `scanned ~2407 bytes (2.41 KB) in 231ms`, `no leaks found`, exit 0.
- Task 2: `scanned ~1557 bytes (1.56 KB) in 205ms`, `no leaks found`, exit 0.

## Cited lines versus the plan's quotes

No cited line differed. `architecture-site.yml:50-53` and `:112`, `deploy.yml:92-94` and `backup-image.yml:36`, `:46` were re-read before the row was written and matched the plan's quotes exactly. The row 4 gate then re-read every cite against the file and printed `ROW4 OK`, both before the Task 1 commit and again on the final tree.

## Scope beyond the two lines the gap names

`scenarios-platform.c4:33` carried the same false exclusivity claim as line 38 in the same view, so it was fixed in the same wording. `delivery.c4:14`, `:42` and `:43` claim no exclusivity but named only deploy.yml and architecture-site.yml (or only the app and site images). That contradicted the rewritten ci description, since backup-image.yml also starts on a push to main and pushes the kanban-postgres-backup image. All four were fixed so the delivery view agrees with itself.

## Decision: no standing regression test

None was added. Basis: the user's global "Pin every bug fix" rule, read with its "never for what is meant to keep changing" exemption. No project file requires a test for prose.

- The bug lived in model prose. `sync.mjs check` validates LikeC4 syntax and drift rules over element metadata and topic/namespace titles, never a description or relationship title.
- A guard could hold only the workflow-name lists on `delivery.c4:7` and `:42`. The "only job of this workflow" claims on `scenarios-platform.c4:33` and `:38` are semantic and no string match holds them. Such a rule would also key on wording, so a rewording would fail it.
- Direction checked: every set diff, absence search, the row 4 gate and the build gate was red against HEAD at planning time (baselines recorded in the plan) and green after the edit (this run). I did not re-run the red direction myself; that half rests on the plan's planning-time record.
- **What a standing guard would need:** a `sync.mjs` rule that reads `push: true` from `.github/workflows/*.yml` and the `.yml` names from `delivery.c4:7` and `:42`, a drill row, a regenerated `drill-output.txt` and a hook selftest case.
- Residual risk accepted (T-03-32): a fourth image-pushing workflow, or another job moved into `production`, would make the text false again without failing anything.

## Deviations from Plan

None - plan executed exactly as written. One transient slip, not a deviation: my first Edit of line 33 used a lower-case "the drift check" and failed loudly on a missing anchor; I re-ran it with the file's actual capital "The", and the resulting line matches the plan's text exactly (verified with `sed -n 33p`).

## Not verified

- **Rendered size not re-measured.** `DIAGRAM_CONVENTIONS.md` records publish-the-site at 2360 x 1401 and delivery at 2342 x 1573 (both 2026-10-28). Step 4's label and the delivery `:42` and `:43` edge labels grew, so those rows predate the change. The legibility rule is undefined, so no verdict was possible either way.
- **The live staging site still serves the old sentences** until the user pushes. A push of these commits starts `architecture-site.yml` (model paths) and not `deploy.yml` (`docs/**` and `.planning/**` are in its `paths-ignore`). It costs one site run and one Flux bump, and it carries every local commit ahead of origin/main (13 at the time of this run, including these two). I did not push.
- The leak-words scan (`LEAKWORDS`) is a one-off for T-03-33; no verify repeats it.
- The drill replay was not re-run: no row of `drills.tsv` touches the six edited lines (plan's finding, not re-derived here).

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or trust-boundary file access; the model says "registry token" and no secret identifier or value was added.

## Issues Encountered

The pre-existing untracked files under `.planning/quick/` and `.planning/workstreams/architecture-views/phases/03.1-model-description-pass/` were left untouched, as instructed.

## Self-Check: PASSED

- Files: scenarios-platform.c4, delivery.c4, 03-SCENARIO-EVIDENCE.md and 03-08-prose-proof.txt exist.
- Commits: `d8599d4` and `853bae1` are ancestors of HEAD.
- `git show --stat` of each commit lists exactly its three paths; `git diff --numstat 853bae1^..853bae1 -- docs/architecture/model` printed `4 4 delivery.c4` and `1 1 scenarios-platform.c4`.
- `docs/architecture/site/out` absent; `git status --short -- docs/architecture` and the phase directory print nothing before this SUMMARY.
