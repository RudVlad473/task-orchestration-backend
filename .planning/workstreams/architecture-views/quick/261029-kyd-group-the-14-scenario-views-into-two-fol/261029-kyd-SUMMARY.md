---
quick_id: 261029-kyd
description: Group the 14 scenario views into two folders
status: complete
commits: 1
plan_head_before: 73e3c22
plan_head_after: the commit that adds this file (a commit cannot name its own hash)
key-files:
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/model/scenarios-app.c4
    - docs/architecture/model/scenarios-platform.c4
    - docs/architecture/model/views.c4
    - docs/architecture/drills.tsv
    - docs/architecture/drill-output.txt
    - scripts/pre-commit-likec4-selftest.py
    - docs/DIAGRAM_CONVENTIONS.md
---

# Quick 261029-kyd: two scenario folders held by the folder rule

The 14 scenario views now sit in `5a Application behaviour` (8) and `5b Platform and delivery` (6), and `checkViewFolders` derives the expected folder of a scenario from the participants its body draws, so a view in the wrong folder is refused.

## Task 1: participants, read from the bodies

Rule used: a scenario is application behaviour when every participant is `client`, `traefik`, `kanban` or a child of it, `postgres` or a child, or `redpanda` or a child. Any other participant makes it a platform view. All 14 fit; none needed a stop.

| View | Participants (read from the body) | Folder |
|------|-----------------------------------|--------|
| signup-ok | client, traefik, kanban.api, postgres.table_users, postgres.spring_session | 5a |
| signin-ok | same five | 5a |
| signin-bad-credentials | client, traefik, kanban.api, postgres.table_users | 5a |
| signin-ceiling | same five as signin-ok | 5a |
| error-401 | client, traefik, kanban.api | 5a |
| error-403 | client, traefik, kanban.api, postgres.table_users, postgres.table_boards | 5a |
| error-409 | client, traefik, kanban.api, postgres.table_tasks | 5a |
| activity-log-row | kanban.api, kanban.publisher, redpanda.activity, kanban.consumer, postgres.table_activity_log | 5a |
| push-to-image | repo, ci, dockerhub, postgres | 5b |
| image-to-rollout | flux, dockerhub, repo, kanban.init, kanban.api | 5b |
| publish-the-site | repo, ci, dockerhub, flux, archsite | 5b |
| inbound-packet-path | client, firewall, hostfilter, svclb, traefik | 5b |
| ingress-to-pod | svclb, traefik, kanban.api | 5b |
| nightly-backup | backup, postgres, b2 | 5b |

Two judgment calls the rule absorbs rather than avoids:

- `traefik` and `client` appear in both folders, so the test cannot be "touches the edge". Traefik is plumbing of the application path; the platform marker is `svclb`, `firewall` or `hostfilter` (the host and provider edge).
- `activity-log-row` draws `redpanda`, which is a separate system, but it is the activity-log pipeline the plan names as application behaviour. `redpanda` is therefore in the allow-list. `kanban.init` is not special-cased: its only scenario, `image-to-rollout`, also draws `flux`.

The rule is an allow-list on purpose: a participant not named in `APP_PARTICIPANTS` sends the view to the platform folder, and a view drawn there by mistake is reported by the check instead of passing quietly.

## What changed

- `sync.mjs`: `VIEW_FOLDERS` loses `scenarios`; new `SCENARIO_FOLDERS`, `APP_PARTICIPANTS` and `scenarioFolder(view)`; `checkViewFolders` calls it for views tagged `#scenarios`. The message format is unchanged.
- Model: the three signin views moved from `views.c4` into `scenarios-app.c4` (the folder rule allowed it; the `views '5 Scenarios'` block in `views.c4` is gone). The two files carry the new labels. View ids and `order` values are unchanged.
- `drills.tsv`: D29 retargeted to the app file with a label-agnostic sed; D18 retargeted from `views.c4` to `scenarios-app.c4` because its anchor line moved (see below); new D29c and D29d.
- `scripts/pre-commit-likec4-selftest.py`: `test_18` retargeted to the new label; new `test_19` (a platform view in the application folder is refused). The hook self-test count is now 19.
- `docs/DIAGRAM_CONVENTIONS.md`: the viewpoint-folder rule names both folders and states the participant heuristic.

## Verification, each output read

| Check | Result |
|-------|--------|
| `node docs/architecture/sync.mjs check` | `OK (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads)`, exit 0 |
| View ids | 21 before and 21 after, `diff` of the sorted id lists empty |
| `python3 scripts/pre-commit-likec4-selftest.py` | `19/19 passed` |
| `likec4 validate` | Valid (10 files) |
| Drill harness, current script and model, diffed against the old `drill-output.txt` | only the intended lines differ (D29 wording and its 8 view lines, new D29c and D29d); then recorded as the new `drill-output.txt` |
| Site build, from a clean `out/site` | built, exit 0; `likec4-views.js` holds `5a Application behaviour` 8 times and `5b Platform and delivery` 6 times, matching the view counts; `5 Scenarios` is absent from the bundle; `out/site` deleted afterwards |

New drills, both ways, both outputs read:

| Drill | Old script + HEAD model (before) | New script + new model (after) |
|-------|----------------------------------|--------------------------------|
| D29c, a scenario that draws CI sits in the application folder | `exit=0`, `OK (62 elements ...)` | `exit=1`, `view probe-platform is in folder '5a Application behaviour', expected '5b Platform and delivery'` |
| D29d, a scenario that draws only the app sits in the platform folder | `exit=0`, `OK (62 elements ...)` | `exit=1`, `view probe-app is in folder '5b Platform and delivery', expected '5a Application behaviour'` |

The before half used the sync.mjs and the model of HEAD (73e3c22) with the new drill table. The edit is an appended probe view placed before the last line of the target file, so it applies identically to the old and new model; on the old one it lands in `5 Scenarios` and the old rule has nothing to say. That the sed applied in the before half is shown by the after half failing on the same view.

## Deviations from Plan

**1. [Rule 1 - Bug] Drill D18 silently stopped catching anything.** First harness run after the move showed D18 (a scenario view gets a sixth participant) at `exit=0` where the recorded output had `exit=1`. Its sed anchors on the `signin-ok` response line, which now lives in `scenarios-app.c4`, so the edit hit no line in `views.c4`. Retargeted the row to `model/scenarios-app.c4`; the rerun reproduces the recorded D18 output exactly. This is the failure the harness exists to show, and it is why the drill output was diffed, not only run.

**2. [Scope] Stale `out/site` removed before the build check.** A previous build was in `docs/architecture/out/site` (ignored), and a rebuild over it left the old `5 Scenarios` label in a stale hashed asset, which would have made the label check read both labels. The directory was trashed and rebuilt clean for the numbers above.

## Not run

- The self-test was not run against the old `sync.mjs`; the old-versus-new proof for the rule is the drill pair above, which uses the same edit shape. `test_19` was therefore not shown to fail on the old script.
- The commit's pre-commit hook (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest) is the last gate and runs at commit time; its result is reported in the final message, not here.

## Known Stubs

None.

## Self-Check: PASSED

Files verified present and sorted ids equal: see the verification table. `git diff --stat` before commit listed exactly the eight files above.
