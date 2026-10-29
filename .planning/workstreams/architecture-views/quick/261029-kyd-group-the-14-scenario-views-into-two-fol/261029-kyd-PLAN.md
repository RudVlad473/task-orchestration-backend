---
quick_id: 261029-kyd
description: Group the 14 scenario views into two folders
files_modified:
  - docs/architecture/sync.mjs
  - docs/architecture/model/views.c4
  - docs/architecture/model/scenarios-app.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/drills.tsv
  - docs/architecture/drill.fish
  - docs/architecture/drill-output.txt
  - scripts/pre-commit-likec4-selftest.py
  - docs/DIAGRAM_CONVENTIONS.md
---

<objective>
The 14 scenario views sit in one flat folder `5 Scenarios`. Split it into "5a Application behaviour" (8 views) and "5b Platform and delivery" (6 views) and hold the split with the folder rule from phase 3.1 plan 02 (`checkViewFolders`, `VIEW_FOLDERS` in sync.mjs). The 21 view ids do not change.
</objective>

<approaches>
Two alternatives were considered besides the chosen one.

| Approach | Pros / Cons | Why picked |
|----------|-------------|------------|
| A. Two folders by participant set (chosen) | One rule, checkable from the view body; 8 and 6 views; matches existing source files | Cheapest rule that separates business from infrastructure |
| B. Four folders by capability (Authentication 4, API errors 3, Audit trail 1, Platform 6) | Finer; reads like a feature list | A one-view folder, and every new scenario needs a category argument |
| C. Leave one folder, only move source between files | No navigation change, no drill | The sidebar still shows 14 mixed items, which is what the user dislikes |

Trade-offs: the folder rule gains one more entry per folder, so `checkViewFolders` and its drills must change in the same commit (state invalidation risk: a view moved without the map update fails `sync.mjs check`, which is the intended loud failure). No runtime, memory or security cost; the leak gate applies to any added model line.
</objective>

<tasks>
1. Read every scenario body and list its participants. Confirm the rule: application behaviour when every participant sits inside the kanban system (API, database, session, activity log); platform when it touches CI, the registry, the cluster, the edge, backup or the site pipeline. If any scenario does not fit, STOP and report which one and why, changing nothing.
2. Regroup the views in the model (folder names "5a Application behaviour", "5b Platform and delivery"), keeping all 21 view ids and the `order` sequence unique. Move the three signin views from views.c4 into scenarios-app.c4 only if the folder rule allows it without changing a view id.
3. Update `VIEW_FOLDERS`/`checkViewFolders` in sync.mjs. Add a drill row (a platform view moved into the application folder, and the reverse if cheap) that is red on the new rule and was green on the old script, record before and after output, update drill.fish and drill-output.txt, and add a self-test case in scripts/pre-commit-likec4-selftest.py if the existing folder cases do not cover it.
4. State the heuristic in docs/DIAGRAM_CONVENTIONS.md next to the viewpoint-folder rule.
5. Verify, reading each output: `node docs/architecture/sync.mjs check`; `python3 scripts/pre-commit-likec4-selftest.py`; the drill harness output equals drill-output.txt except for the intended new lines; a likec4 site build shows both folder labels in the built bundle (delete out/site afterwards); the 21 view ids equal those at the start; gitleaks and the comment lint on the staged diff.
6. Commit on main with a conventional message, never push. The pre-commit hook takes about 4 minutes; use a generous timeout and `< /dev/null`.
</tasks>
