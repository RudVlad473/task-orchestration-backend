---
phase: quick-261028-s6i
plan: 01
subsystem: architecture-views drill harness
tags: [fish, tsv, drills, refactor]
requires: [quick-261028-rlq]
provides: ["docs/architecture/drills.tsv: every drill as rows", "run_drills: the one loop that runs them"]
affects: [phase-03 plan 05 (drill rows for D25 to D27)]
tech-stack:
  added: []
  patterns: ["data table plus a kind switch as allowlist, fields passed as variable expansions, no eval"]
key-files:
  created: [docs/architecture/drills.tsv]
  modified: [docs/architecture/drill.fish, docs/DIAGRAM_CONVENTIONS.md]
  deleted: [docs/architecture/sed-drills.tsv]
decisions:
  - "D5 pinned to BoardController.java and D8b to AppAccessDeniedException.java instead of a find at run time"
  - "lonely_into takes the value unlinked in place of an argument-less call"
  - "certmgr_into, lonely_into and labeldrill stay as kinds, not expanded into write and sed rows"
requirements: [GATE-01]
status: complete
actuals:
  tokens: 3000
  tasks: 3
  commits: 2
plan_head_before: edc959dca79039c6481058d976d876c185b52e51
plan_head_after: 0397d50aae6e95b8e60d294af5ccaa481f434526
duration: about 1 hour
completed: 2026-10-28
---

# Phase quick-261028-s6i Plan 01: drill.fish as one TSV and one loop Summary

All 36 drills of docs/architecture/drill.fish are now 39 rows of docs/architecture/drills.tsv, run by one
`run_drills $here/drills.tsv` call, and the replay is byte-identical to docs/architecture/drill-output.txt.

## What changed

- `docs/architecture/drills.tsv`: header `drill title kind file value` and 39 rows. Kinds: none 1, write 7, sed 21, delete 3, mv 1, certmgr_into 2, lonely_into 2, labeldrill 2. D11 (2 rows) and D12b (3 rows) are consecutive rows with the same drill and title.
- `docs/architecture/drill.fish` keeps `fresh`, `run`, the three helpers and `run_drills`, then two top-level commands: `run_drills $here/drills.tsv` and `trash-put $scratch`. The `sed_drills` function, its 6 calls and the 21 written-out blocks are gone.
- `docs/architecture/sed-drills.tsv` is removed (`git rm`); `rg 'sed-drills|sed_drills'` over docs/architecture, scripts, .githooks, .github and docs finds nothing.
- `docs/DIAGRAM_CONVENTIONS.md`: one sentence in the Drills bullet naming drills.tsv and run_drills.
- `run_drills` reads the table whole by command substitution before the loop, passes each field as one argument from a variable (no eval, no re-parse), runs sed with `--sandbox`, and exits 2 for a missing or empty table, a row that is not 5 non-empty fields, an unknown kind, or a drill whose rows are not one consecutive block.

## Commits

- Task 1: f6e6efe `refactor(drill): run D12b to D24 from drills.tsv through run_drills`
- Task 2: 0397d50 `refactor(drill): run every drill from drills.tsv through one run_drills loop`
- Task 3 wrote only records under .planning (left for the orchestrator's docs commit) and removed its scratch files.

Both code commits went through the full `.githooks/pre-commit` (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest); neither used `--no-verify`.

## Choices this plan made

- **D5 and D8b are pinned.** D5 previously ran `find ... -path '*controller*' | head -1`, whose result depends on directory order; it is now an `mv` row from `src/main/java/com/vrudenko/kanban_board/controller/BoardController.java` to `RenamedController.java` in the same directory. D8b previously took `find ... | sort | head -1`; it is now a `sed` row on `src/main/java/com/vrudenko/kanban_board/exception/AppAccessDeniedException.java`. I re-ran both finds at execution time and they print exactly those files, so no substitution was needed. Consequence: if either file is renamed in the real tree, the replay prints the mv or sed error and cmp fails, where find would have silently picked another file.
- **lonely_into value.** D13 passes `unlinked` as the helper's one argument instead of calling it with none. Behaviour is identical because the helper tests only for `linked`; D13b passes `linked`.
- The three helpers stay as kinds (certmgr_into, lonely_into, labeldrill), so each control differs from its drill by one value and the helper comments stay next to the code.

## Verification (real output)

- `fish -n` and `fish_indent --check` on drill.fish: both exit 0.
- Table: header `drill,title,kind,file,value`, `39 0` (rows, rows with a field count other than 5), kind counts as listed above.
- The table's `== drill: title` lines, consecutive repeats collapsed, `diff` clean against the 36 `== ` headers of drill-output.txt (also clean for the D12b-on suffix after Task 1).
- Counts after Task 1 (`echo "== `, `sed -i`, `sed_drills `, `run_drills `): 13, 3, 5, 1.
- Guard script: 4 lines matching `exit=2 .*run_drills: ` (missing table, 4-field row, kind eval, drill X1 split by X2), no hang under stdin from /dev/null.
- `python3 scripts/verify-comments.py check`: `OK: 331 files, 1286 comment blocks`.
- Replay after Task 1: `cmp` exit 0. Replay after Task 2: script exit 0, `cmp` exit 0, 59 s.
- `git status --porcelain -- docs/architecture docs/DIAGRAM_CONVENTIONS.md` prints nothing; `git diff --stat edc959d -- docs/architecture` lists only drill.fish, drills.tsv and sed-drills.tsv.

## Mutation result (the oracle reads every new kind)

Kind set to none on D14c (write), D14 (delete), D16 (certmgr_into), D13 (lonely_into), D17 (labeldrill) and D12b's model/model.c4 row (continuation row), and D5 retargeted to the cited manifest k8s/data/postgres/postgres.yaml. Result: `cmp exit: 1` and exactly 7 changed sections: D5 (negative control), D12b, D13, D14, D14c, D16, D17. drills.tsv was restored from git afterwards (`git status` clean). Record: 261028-s6i-mutation.txt in this directory.

## Phase 3 plan lines naming the old table or runner (not edited)

Listed at execution time with `rg -n --sort path 'sed-drills|sed_drills'` over the phase 3 directory (the planner had already revised 03-05-PLAN.md to handle both shapes); verbatim from 261028-s6i-phase3-refs.txt:

```
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-01-PLAN.md:144:Commit protocol for every plan in this phase: precondition `docker info` exits 0, because the hook runs gitleaks in a pinned container and fastTest on Testcontainers. Run the full `.githooks/pre-commit` with a Bash timeout of 600000 ms; expect up to about five minutes per commit. `--no-verify` only where a task says so: CLAUDE.md permits it for docs, planning and config-only commits and never for k8s manifests or Java. Before staging, `git diff --cached --name-only` must print nothing: a path there belongs to another session mid-commit, so stop and report rather than commit around it (SESSION_LESSONS lesson 2). When this phase was first planned, quick task 261028-rlq had drill.fish and sed-drills.tsv staged; it has since committed them (ed074df, 49f95d4, edc959d) and the index was empty at revision time. At revision time a second quick task, 261028-s6i, had drill.fish and sed-drills.tsv modified and docs/architecture/drills.tsv untracked in the working tree (moving every drill into drills.tsv); no plan here edits or stages those files except plan 05, which works on whichever drill table is committed when it runs and stops if one is mid-flight. `git commit -- <paths>` keeps another session's unstaged work out of every commit. Then `git add` new files, confirm `git diff --cached --name-only` lists only the task's paths, and `git commit -m "<message saying why>" -- <paths>`. Never amend. Push only from a task whose precondition names a `DECISION` line in 03-DECISIONS.txt that authorizes it, using the push procedure in 03-02-PLAN.md. Leave the user's untracked `.planning/quick/261024-*` files and this phase's untracked 03-PATTERNS.md alone.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:9:  - docs/architecture/sed-drills.tsv
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:23:    - "Drill D25 (the site Deployment renamed) is a sed row of the drill table drill.fish runs when this plan executes (docs/architecture/drills.tsv once quick task 261028-s6i has landed; otherwise docs/architecture/sed-drills.tsv with its id on the sed_drills call that ends with D24), and the replay records it caught with `exit=1` and the workload pair: the renamed Deployment has no element, and the model's claimed workload Deployment/architecture-site has no manifest"
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:26:    - "`fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` exits 0 on the committed tree, the table passes its shape check (five fields per drills.tsv row, or TSV ids equal to the called ids for sed-drills.tsv), and `python3 scripts/pre-commit-likec4-selftest.py` exits 0"
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:46:Which drill table the three drills go into. At revision time the committed harness is quick task 261028-rlq's (sed-drills.tsv rows run by `sed_drills` calls, 49f95d4), and a second quick task, 261028-s6i, is uncommitted in the working tree: its PLAN.md moves every drill into docs/architecture/drills.tsv (columns drill, title, kind, file, value; one `run_drills` call) and deletes sed-drills.tsv and `sed_drills`.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:50:| Picked: rows in whichever table is committed when this plan runs, decided from git state: drills.tsv rows of kind `sed` if 261028-s6i has landed, else sed-drills.tsv rows plus their ids on the `sed_drills` call that ends with D24; a refactor found mid-flight stops the plan | Pro: matches the harness as it is at execution time; the drills are data in both shapes; no fish code is written. Con: two shapes to describe and verify; the executor decides from git state. | Plan ordering cannot guarantee which refactor state exists by wave 5. |
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:52:| Plan only the sed-drills.tsv shape | Con: 261028-s6i deletes sed-drills.tsv and `sed_drills`; once it lands, this plan would stop on its own precondition and block the phase. | Rejected. |
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:76:Output: two local commits holding the drill table (and, in the sed-drills.tsv shape, drill.fish) and drill-output.txt.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:81:- The drill table: three rows after D24's. drills.tsv shape: `D25`, title, `sed`, file, sed expression (five tab-separated fields). sed-drills.tsv shape: `D25`, title, file, sed expression (four fields), and ` D25 D26 D27` appended to the `sed_drills` call that ends with D24 in drill.fish.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:99:- First, `git status --short -- docs/architecture/drill.fish docs/architecture/drills.tsv docs/architecture/sed-drills.tsv docs/architecture/drill-output.txt` must print nothing, and `git diff --cached --name-only` must print nothing. Any line (an untracked drills.tsv included) means another session is mid-refactor: stop and report the lines; do not edit, stage or stash them.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:100:- drills.tsv shape: `git ls-files docs/architecture/drills.tsv` prints it, `git ls-files docs/architecture/sed-drills.tsv` prints nothing, and `grep -c '^run_drills ' docs/architecture/drill.fish` prints 1. Read drill.fish's header and run_drills, and the quick task's PLAN.md at .planning/workstreams/architecture-views/quick/261028-s6i-refactor-drill-fish-into-one-tsv-with-a-/, for the row rules (five non-empty fields, a `-` in a column a kind does not use, rows of one drill consecutive).
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:101:- sed-drills.tsv shape: `git ls-files docs/architecture/sed-drills.tsv` prints it, `git ls-files docs/architecture/drills.tsv` prints nothing, and `grep -c '^sed_drills D18 D19 D20 D21 D22 D23 D24$' docs/architecture/drill.fish` prints 1 (quick task 261028-rlq's committed shape; its PLAN.md at .planning/workstreams/architecture-views/quick/261028-rlq-refactor-drill-fish-tsv-for-one-sed-edit/ states the invariants).
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:106:- Table check, sed-drills.tsv shape (prints nothing and exits 0 when the TSV ids equal the called ids, in order; measured at revision time on the committed rlq tree: exit 0): `diff <(tail -n +2 docs/architecture/sed-drills.tsv | cut -f1) <(grep -E '^sed_drills ' docs/architecture/drill.fish | sed 's/^sed_drills //' | tr ' ' '\n')`.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:116:  <files>docs/architecture/drills.tsv or docs/architecture/sed-drills.tsv and docs/architecture/drill.fish (by shape), docs/architecture/drill-output.txt</files>
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:118:    - docs/architecture/drill.fish (header comment, fresh and run, and the loop that reads the table: run_drills or sed_drills)
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:119:    - the committed drill table (drills.tsv or sed-drills.tsv): header row and the D20 to D24 rows, for column order and quoting
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:132:    - sed-drills.tsv shape: `D25`, the title, the file, the expression (four fields); then in drill.fish append ` D25` to the call, so it reads `sed_drills D18 D19 D20 D21 D22 D23 D24 D25`. Leave its header comment as it is: it names the table's rule, not its rows.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:137:    Commit with `--no-verify` after the two checks from context, naming only the shape's paths (`git commit -m "<message>" -- <paths>`: drills.tsv and drill-output.txt, or sed-drills.tsv, drill.fish and drill-output.txt), message along the lines of `test(drill): D25 shows the workload check covers the architecture site's Deployment`. Keep docs/architecture/out/exits-before-03-05.txt for Task 2.
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:149:    <fails_when>non-zero exit, or the list is not exactly the shape's paths (drills.tsv and drill-output.txt; or sed-drills.tsv, drill.fish and drill-output.txt)</fails_when>
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:152:    - The shape's table check from context prints nothing (and, for sed-drills.tsv, exits 0).
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:162:  <files>docs/architecture/drills.tsv or docs/architecture/sed-drills.tsv and docs/architecture/drill.fish (by Task 1's shape), docs/architecture/drill-output.txt</files>
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:169:    With Edit, append two rows after D25's, in Task 1's shape and tab-separated as there (drills.tsv: drill, title, `sed`, file, expression; sed-drills.tsv: drill, title, file, expression):
.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md:172:    In the sed-drills.tsv shape only, append ` D26 D27` to the call, so it ends `D24 D25 D26 D27`. Run the shape's table check.
```

For the Phase 3 planner: the s6i refactor has landed (0397d50), so only the drills.tsv shape applies. A new one-sed drill is one row appended after the D24 row of docs/architecture/drills.tsv with the five tab-separated columns drill, title, `sed`, file, expression; there is no `sed_drills` call to extend and no sed-drills.tsv. The drill.fish header comment does not list drills, so it needs no edit for D25 to D27. Three plan-05 checks now take their drills.tsv branch: `git ls-files docs/architecture/sed-drills.tsv` prints nothing, `grep -c '^run_drills ' docs/architecture/drill.fish` prints 1, and every row has five non-empty fields.

## Deviations from Plan

None - plan executed exactly as written. One small note, not a deviation:

- The guard script runs the `run_drills` text through `psub` as the plan says, with a `fresh`/`run` stub child script; its "fields" case writes a genuine four-field row (`printf` with four `%s`), since padding a five-field format would have produced an empty fifth field and tested the other guard.

## Known Stubs

None.

## Threat Flags

None. No new network, auth or file-access surface; the table's fields only reach sed (`--sandbox`), printf, trash-put, mv and three named helpers.

## Self-Check: PASSED

- docs/architecture/drills.tsv, drill.fish and docs/DIAGRAM_CONVENTIONS.md exist; docs/architecture/sed-drills.tsv is gone.
- f6e6efe and 0397d50 are ancestors of HEAD (`git merge-base --is-ancestor`).
- `git rev-list --count edc959d..HEAD` printed 2.
- docs/architecture/out holds no s6i file.
