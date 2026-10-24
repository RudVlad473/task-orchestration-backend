---
phase: quick-261024-jvc
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - .env.nonprod.example
  - .github/dependabot.yml
  - k8s/flux-system/apps-prod.yaml
  - src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogCleanupIsolationTest.java
  - src/test/java/com/vrudenko/kanban_board/config/FlywaySchemaProvenanceTest.java
  - src/test/java/com/vrudenko/kanban_board/controller/BoardFullReadTest.java
  - src/main/java/com/vrudenko/kanban_board/config/RandFlakeGenerator.java
  - src/main/java/com/vrudenko/kanban_board/constant/MetricNames.java
  - src/main/java/com/vrudenko/kanban_board/dto/board_dto/UpdateBoardRequestDTO.java
  - src/main/java/com/vrudenko/kanban_board/dto/column_dto/ColumnFullResponseDTO.java
  - src/main/java/com/vrudenko/kanban_board/entity/UserEntity.java
  - src/main/java/com/vrudenko/kanban_board/mapper/ColumnFullMapper.java
  - src/main/java/com/vrudenko/kanban_board/mapper/TaskFullMapper.java
  - src/main/java/com/vrudenko/kanban_board/security/AuthenticationController.java
  - src/main/java/com/vrudenko/kanban_board/security/ProblemDetailAuthenticationEntryPoint.java
  - src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java
  - src/main/java/com/vrudenko/kanban_board/service/BoardService.java
  - src/main/java/com/vrudenko/kanban_board/service/ColumnService.java
  - src/main/java/com/vrudenko/kanban_board/service/SubtaskService.java
  - src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogConsumerE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/activitylog/HistoricalActivityEventReconstructor.java
  - src/test/java/com/vrudenko/kanban_board/activitylog/HistoricalSchemaRehearsalE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/activitylog/SchemaCompatibilityE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/activitylog/SchemaRegistryOutageE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/config/ComposedConstraintPropertyCustomizerTest.java
  - src/test/java/com/vrudenko/kanban_board/config/OpenApiParameterCompletenessTest.java
  - src/test/java/com/vrudenko/kanban_board/controller/BoardControllerTest.java
  - src/test/java/com/vrudenko/kanban_board/controller/ColumnControllerTest.java
  - src/test/java/com/vrudenko/kanban_board/controller/ThemePersistenceTest.java
  - src/test/java/com/vrudenko/kanban_board/dto/OptionalNotBlankTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/board/BoardCreationE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/column/ColumnLockingTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/observability/EagerMetricRegistrationTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/observability/ObservedAspectWiringTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/reset/ResetControllerE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/subtask/SubtaskLockingTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/task/TaskMoveTest.java
  - src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/security/AuthenticationTest.java
  - src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java
  - src/test/java/com/vrudenko/kanban_board/security/InjectionAttemptTest.java
  - src/test/java/com/vrudenko/kanban_board/security/ResetEndpointProfileGatingTest.java
  - src/test/java/com/vrudenko/kanban_board/security/SigninTimingEqualizationTest.java
  - src/test/java/com/vrudenko/kanban_board/service/BoardServiceTest.java
  - src/test/java/com/vrudenko/kanban_board/service/ColumnServiceTest.java
  - src/test/java/com/vrudenko/kanban_board/support/containers/AbstractPostgresContainerTest.java
  - src/test/java/com/vrudenko/kanban_board/support/fixtures/AbstractAppMockMvcTest.java
  - src/test/java/com/vrudenko/kanban_board/support/fixtures/AbstractAppTest.java
  - .github/workflows/api-fuzz.yml
  - .github/workflows/deploy.yml
  - .github/workflows/invariant-checks.yml
  - .github/workflows/security-scan.yml
  - .planning/milestones/v1.2-phases/05-infra-migration/scripts/verify-baseline-schema.sh
  - .planning/quick/260808-ku4-analyze-kanban-mock-up-pdf-and-produce-a/extract-mockup-text.py
  - .planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/migrate_docs.py
  - .planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/verify_migration.py
  - .planning/quick/261006-fby-ban-javadoc-html-tags-and-inline-tags-in/sweep-javadoc-markup.py
  - .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml
  - .serena/project.yml
  - infra/vm/k3s-host-firewall.sh
  - infra/vm/k3s/config.yaml
  - k8s/base/redpanda/redpanda.yaml
  - k8s/flux-system/data.yaml
  - k8s/monitoring/controllers/kube-prometheus-stack.yaml
  - k8s/monitoring/controllers/repositories.yaml
  - k8s/overlays/nonprod/ingressroute.yaml
  - k8s/platform/edge/ingressroute-monitoring.yaml
  - scripts/fuzz/run-fuzz-selftest.py
  - scripts/verify-comments-selftest.py
  - scripts/verify-diagrams.py
  - scripts/verify-k8s-invariants-selftest.py
  - scripts/verify-k8s-invariants.py
  - scripts/verify-k8s-manifests.sh
  - scripts/verify-postgres-init-quoting.sh
  - src/main/resources/application-test.properties
  - src/main/resources/application.properties
autonomous: true
requirements: [QUICK-261024-jvc]

estimate:
  tokens: 340000
  raw_tokens: 340000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "The six comments that stated something false now match the code: the Flyway range, the CI image tag format, the dependabot caps, the flat-repository ordering, the board id scheme, and the stale cutover note in apps-prod.yaml (gone)"
    - "Every one of the 195 applicable findings is applied or listed in not-applied.txt with a reason; `fish <quick-dir>/worklist.fish check 3` exits 0"
    - "The diff from base.sha is comments-only: verify-comments.py equiv passes on every changed in-scope file, and yamleq, bash -n and py_compile pass on the .serena/ and .planning/ files equiv does not cover"
    - "No bare arrange/act/assert marker is lost: equiv's marker-count check passes on every changed Java test file and `python3 scripts/verify-comments.py check` exits 0"
    - "Nothing that triggers a rollout changed: k8s/data/postgres/init/ is byte-identical to base.sha and no configMapGenerator input was edited"
    - "The user's files (.planning/STATE.md, AsyncConfig.java, KafkaConsumerConfig.java, ActivityTraceContinuityE2ETest.java, kanban-backup.agekey) are untouched and in no commit"
  artifacts:
    - path: ".planning/quick/261024-jvc-apply-the-198-review-comments-findings-t/261024-jvc-SUMMARY.md"
      provides: "per-task commit SHAs, applied counts, the not-applied and rollout-gated list with reasons, grading deviations, observations, and the final whole_tree.py verify counts"
    - path: ".planning/quick/261024-jvc-apply-the-198-review-comments-findings-t/worklist.jsonl"
      provides: "the 198-row snapshot (id, file, grading-time span, verdict, text, replacement, task, factual, skip) every step keys on; deleted in the final cleanup"
  key_links:
    - from: "worklist.jsonl id"
      to: "review-comments ledger.jsonl id"
      via: "check-applied.jq, run by worklist.fish check after whole_tree.py verify rescans"
      pattern: "still-graded|re-keyed"
    - from: "base.sha"
      to: "verify-comments.py equiv --base and worklist.fish yamleq"
      via: "one commit recorded before the first edit, so every gate compares against the same pre-change tree"
      pattern: "equiv --base"
    - from: "worklist.fish paths N"
      to: "git commit -- <paths>"
      via: "explicit pathspec, so the user's uncommitted files can never enter a commit"
      pattern: "git commit -F"
---

<objective>
Apply the 198 review-comments findings (80 deletes, 118 rewrites) to the repo's code comments, with
no code, config value or key change. The six comments that state something false come first.

Purpose: the comment ledger graded every comment in the tree; until the verdicts land, the repo still
carries six comments that state something false and about 190 that restate code, cite unresolvable
planning tokens or point at siblings. CODE_STYLE rule 14 says to remove or fix those.

Output: three commits (one per task), edited comments in 76 files, and 261024-jvc-SUMMARY.md listing
what was not applied and why, plus the final whole_tree.py verify counts.
</objective>

## Approaches considered (project CLAUDE.md GSD directive)

The approach was chosen by the orchestrator before planning; this records it, it does not reopen it.

| Approach | Pros / Cons | Why picked |
|----------|-------------|------------|
| A. One plan, three risk-grouped tasks, every edit made with the Edit tool against a worklist snapshot keyed by ledger id (chosen) | Pro: a missed match fails loudly at the Edit call; each comment is rewritten in its real frame and width; the executor reads the code before writing a replacement, which catches the 2 replacements that drop an AAA marker and the 1 grading error (I10) found while planning. Con: about 190 Edit calls and 3 pre-commit hook runs (about 12 minutes of hooks). | Only option that fails loudly per site and lets the replacement be checked against the code it describes. |
| B. A script that applies the ledger replacements by line number | Pro: fast, one pass. Con: line numbers drift as soon as one edit lands in a file; a replace string that misses writes the file back unchanged (silent no-op); it cannot re-frame bare-prose replacements into `/** */`, `//` or `#` blocks; it would apply the 2 marker-dropping replacements verbatim and fail equiv's marker count. | Rejected: silent failure mode on exactly the cases that need judgement. |
| C. One commit per file | Pro: finest-grained revert. Con: about 76 commits, each Java one paying the about-4-minute pre-commit hook (several hours of hooks) for comment-only changes. | Rejected: cost far above the revert granularity it buys. |

Non-obvious trade-offs:

- **State invalidation:** whole_tree.py verify rewrites the skill's ledger (refreshes line numbers, marks deleted comments gone, adds new text as pending). worklist.jsonl was therefore built once at planning time, before any verify, by joining findings to ledger rows on file and grading-time line (1:1 for all 198). Every later step keys on the row id, which is hash(file, text, anchor, occurrence) and survives line shifts. Never regenerate the worklist after a verify has run.
- **Re-key blind spot:** deleting a comment can change the anchor of the comment directly above it, which gives that comment a new id. An unapplied finding re-keyed this way shows as PENDING, not UNAPPLIED, in verify's output. check-applied.jq closes this by also matching pending rows on exact original text.
- **Formatter scope:** a bare `./gradlew spotlessApply` rewrites every Java file, including the user's in-progress AsyncConfig.java and KafkaConsumerConfig.java. Formatting is applied only through `-PspotlessIdeHook=<abs path>,<abs path>` (Spotless 7.0.2 formats exactly the listed files).
- **Rollout:** the configMapGenerator in k8s/data/postgres/init/ hashes its input files, so any byte change renames the ConfigMap and restarts the Postgres StatefulSet on the next Flux reconcile. All 3 findings there are left alone. Every other k8s/ finding is a YAML comment outside a block scalar in a plain manifest or HelmRelease, which kustomize and helm drop when parsing, so equiv's parsed-YAML comparison is the proof that no rendered object changes.
- **Performance:** each whole_tree.py verify rescans all 1233 comment units in seconds; the dominant cost is the three fastTest hook runs.

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@docs/CODE_STYLE.md
@docs/SESSION_LESSONS.md

Inputs (absolute, outside the repo; read-only for this plan):
- findings: /home/andre/.claude/skills/review-comments/state/kanban-board-backend-2c04b44c7802/findings.jsonl
- ledger: /home/andre/.claude/skills/review-comments/state/kanban-board-backend-2c04b44c7802/ledger.jsonl
- rubric, for judging a replacement that no longer fits: /home/andre/.claude/skills/review-comments/references/rubric.md

Do not read .planning/STATE.md beyond what the orchestrator already gave you; it holds the user's
uncommitted edits.
</context>

## Worklist helper (planning-time artifacts in this quick directory)

All paths below are relative to the repo root; `Q` stands for
`.planning/quick/261024-jvc-apply-the-198-review-comments-findings-t`. Shell variables do not persist
between Bash calls, so start each call that uses it with
`Q=.planning/quick/261024-jvc-apply-the-198-review-comments-findings-t;` and write `$Q` where this
plan writes `Q`.

- `Q/worklist.jsonl`: 198 rows, one per finding: id, file, line/end_line at grading time, verdict, rule, test (the grader's reason), text (original comment, normalized), replacement, task (1/2/3), factual (true on the six factual-error rows), skip (ROLLOUT_GATED or null). Counts: task 1 = 10 (6 factual), task 2 = 110, task 3 = 78 (3 of them ROLLOUT_GATED), so 195 to apply.
- `Q/worklist.fish` (read-only, run with `fish Q/worklist.fish ...`): `files N` (task N's files with counts), `paths N` (bare paths for equiv and commit), `show FILE` (every finding in FILE), `check N [--no-rescan]` (runs whole_tree.py verify, saves `Q/verify-taskN.log`, then exits non-zero while any finding of tasks 1..N is still in the tree), `yamleq PATH` (YAML documents at base.sha equal the working tree's).
- `Q/check-applied.jq`: the comparison `check` runs. `Q/not-applied.txt`: one `<id> <reason>` line per finding deliberately left in place (starts empty).
- Proven at planning time: `check 3 --no-rescan` on the unedited tree exits 1 with 195 MISS lines; listing one id in not-applied.txt drops exactly that line; `yamleq` prints SAME on an unchanged file and DIFFERENT (exit 1) against an older revision. A whole_tree.py verify on a throwaway copy of the state printed 784 UNAPPLIED (186 decision-record, 480 delete, 118 rewrite) and 0 PENDING before any edit.

Data flow, in three sentences: worklist.jsonl holds each finding's original text and replacement, keyed by ledger id. You change the files with Edit, matching on that text, never on a line number. whole_tree.py verify then rescans the tree into the ledger, and check-applied.jq reports every worklist id whose comment is still present.

## Shared rules (every task)

1. **Primary checkout only.** Work in /home/andre/dev/kanban-board-backend on branch main, never in a worktree: the review-comments state directory is keyed by a hash of that path, so verify run from elsewhere scans the wrong tree.
2. **Locate by text.** For each file: `fish Q/worklist.fish show <file>`, then `rg -n -F '<a distinctive phrase from TEXT>' <file>` for each finding to get its current span, then Read that span with a few lines of code around it. One Read per cluster of nearby findings, never the same range twice; read the whole file only when it is under about 200 lines. Finish a file completely before moving on.
3. **Frame the replacement.** TEXT is normalized (indentation stripped; Javadoc lines can show `*` at column 0). REPLACEMENT is usually bare prose, one line per paragraph with a blank line between paragraphs, unless it already starts with `//` or `#`. Write it back in the original comment's frame and indentation (`/** */` with ` * ` lines, `//`, `#`, or a Python docstring), with a blank comment line between paragraphs, wrapped to the width of the neighbouring comments (Java: at most 100 columns). Plain text per CODE_STYLE rule 14: identifiers by bare name, `- ` list items, no line starting with `@` other than a block tag.
4. **Check the replacement against the code.** Before writing a rewrite, read the code it describes and anything it names. If the replacement states something the code contradicts, write a true version that keeps the finding's intent. If no true version fits, leave the comment and add `<id> <reason>` to Q/not-applied.txt. A finding whose TEXT is already gone from the file goes into not-applied.txt too, as "text already gone".
5. **AAA markers (CODE_STYLE rule 5).** Never delete or reduce a bare `// arrange`, `// act` or `// assert` line; worklist.jsonl already excludes the 400 bare-marker deletes. When a finding's TEXT starts with a marker, the result starts with the same bare marker, lowercase. Narration after the marker that restates code is removed. A why the replacement keeps goes on its own comment line(s) directly under the bare marker. A combined marker such as `// act & assert` stays as written. Two replacements drop the marker entirely and must not be applied literally: SchemaRegistryOutageE2ETest (arrange, grading line 155) and BoardControllerTest (the capitalized assert, grading line 443). Write the bare marker, then the replacement's sentence on the next comment line.
6. **Cited ids.** Before removing an id the grader called a planning token, `rg -n -F '<id>' scripts/ src/ docs/ .github/ k8s/`. If it names a real artifact in the repo (an invariant id printed in a FAIL line, a test or doc heading), keep it, drop only the rest of what the finding targets, and record the deviation in SUMMARY.md. Known case: I10 is the IngressRoute redirect invariant in scripts/verify-k8s-invariants.py (its FAIL lines start with `I10:`), so in k8s/overlays/nonprod/ingressroute.yaml keep "(I10)", drop only the sibling comparison at the end of the line, and match the wording of the k8s/platform/edge/ingressroute-monitoring.yaml replacement.
7. **Deletes.** Remove the comment lines only. Do not leave two consecutive blank lines, and do not leave a blank line where a comment used to separate nothing. In YAML keep any `---` document separator.
8. **Hands off.** Never Edit, stage, format, read or commit .planning/STATE.md, src/main/java/com/vrudenko/kanban_board/config/AsyncConfig.java, src/main/java/com/vrudenko/kanban_board/config/KafkaConsumerConfig.java, src/test/java/com/vrudenko/kanban_board/activitylog/ActivityTraceContinuityE2ETest.java, or kanban-backup.agekey (an untracked key file). No worklist row targets them; if one seems to, add it to not-applied.txt. Never edit anything under k8s/data/postgres/init/, src/main/resources/db/migration/, or a file a configMapGenerator or secretGenerator lists.
9. **Formatting.** If `./gradlew spotlessCheck` fails on a file this plan edited, run `./gradlew spotlessApply -PspotlessIdeHook=<abs path>,<abs path>` naming only those files. Never run a bare spotlessApply. If it fails only on a user file, stop and report BLOCKED with the output.
10. **Commit protocol.** Write the message to Q/commit-msg.txt with the Write tool: a conventional subject such as `docs(comments): ...` that says why, a short body, and the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Confirm `git diff --cached --name-only` is empty, then run `git commit -F Q/commit-msg.txt -- $(fish Q/worklist.fish paths N)`. Every commit runs the full pre-commit hook: never `--no-verify`, because all three commits contain Java or k8s manifests, which project CLAUDE.md excludes from that allowance. Never amend, never push, and never commit PLAN.md, SUMMARY.md, STATE.md or anything under Q. If the hook fails in a file this plan edited, fix it and commit again. If it fails anywhere else (a user file, an unrelated test, Docker down), stop and report BLOCKED with the hook output. That is an acceptable outcome.
11. **Context budget.** If context runs short partway through a task, stop at a file boundary, commit the finished files under the same gates (pass only those paths to `git commit --`), and list the untouched files in SUMMARY.md. An exact remaining-files list is an acceptable terminal state. Do not rush the remaining files.

<tasks>

<task type="tracer">
  <name>Task 1: Fix the six factual-error comments end to end, through every gate and the commit</name>
  <files>.env.nonprod.example, .github/dependabot.yml, k8s/flux-system/apps-prod.yaml, src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogCleanupIsolationTest.java, src/test/java/com/vrudenko/kanban_board/config/FlywaySchemaProvenanceTest.java, src/test/java/com/vrudenko/kanban_board/controller/BoardFullReadTest.java</files>
  <precondition>Docker is running (`docker info` exits 0), the cwd is the primary checkout on main, and `./gradlew spotlessCheck compileTestJava` passes on the tree as found; if it fails because of a user file, report BLOCKED before editing anything.</precondition>
  <read_first>
    - src/main/resources/db/migration/ (list only: nine migrations, V1 to V9)
    - .github/workflows/deploy.yml (the image_tag step: main-run_number-sha7)
    - src/main/java/com/vrudenko/kanban_board/repository/ColumnRepository.java and TaskRepository.java (both finders order by position, then id)
    - src/main/java/com/vrudenko/kanban_board/entity/BaseEntity.java (RandFlakeId on the id) and the OrderBy("id") collections in BoardEntity.java and ColumnEntity.java
  </read_first>
  <action>
Step 0. Record the base with `git rev-parse HEAD > Q/base.sha`, note the start time (`date -Is`), and record `git status --short` in your notes so SUMMARY.md can show the user's files unchanged at the end. Run `fish Q/worklist.fish files 1`; it lists 6 files and 10 findings. This task owns every finding in those 6 files, the four non-factual ones included, so each file is opened by one task only.

Step 1. Work each file with shared rules 2 to 7. The six factual rows (shown as FACTUAL by `show`) need a comment that is true of the code as it is now:
- FlywaySchemaProvenanceTest class Javadoc: replace the four-migration version range with "Flyway migrations", as the replacement says. Nine migrations exist, and the test counts versions 1 to 6. Leave the test itself alone and note that count in SUMMARY.md as an observation.
- .env.nonprod.example, the IMAGE_TAG comment: say that CI publishes sortable tags named main-<run_number>-<sha7> (deploy.yml's image_tag step). Keep the fact that Docker Hub publishes no latest tag, and keep the docker inspect hint for the tag production is running. The IMAGE_TAG value line stays byte-identical; it uses the older bare-hash form, so note that in SUMMARY.md. Apply this file's other two rewrites from the worklist.
- .github/dependabot.yml, the github-actions entry: the line claiming parity with the gradle entry's limit is false (gradle 2, github-actions 5). Write a true one, for example "Cap on open PRs from an automated actor. These updates are not grouped, so each action bump opens its own PR." Confirm first that the entry has no groups key.
- BoardFullReadTest, the order-agnostic assert block: write a bare `// assert`, a blank `//`, then a short why. The flat endpoints return rows in (position, id) order (ColumnRepository.findAllByBoardId, TaskRepository.findAllByColumnId), while the nested document orders each collection by id (the OrderBy("id") entity collections). So the test compares ids as sets and checks the nested order separately. The paragraph claiming the flat repositories leave row order to the planner is false and goes. The other two assert rows in this file become bare `// assert` (rule 5).
- ActivityLogCleanupIsolationTest class Javadoc: apply the replacement. Board ids are RandFlake (BaseEntity's id carries RandFlakeId), so the last sentence names RandFlake board ids.
- k8s/flux-system/apps-prod.yaml: delete the two-line header about the Compose cutover precondition (the file already says suspend: false). Keep the `---` line that follows.

Step 2. Run the gates in the verify block, then commit with shared rule 10 (subject such as `docs(comments): correct six comments that contradict the code`), then run `fish Q/worklist.fish check 1`. This task is the tracer: it is the first run of the whole chain on this repo (worklist, Edit, lint, equiv, Spotless, hook, verify rescan, check). If any link misbehaves, fix the chain here before tasks 2 and 3 scale it to 185 more findings.
  </action>
  <verify>
    <automated>Run each line from the repo root; every one must exit 0.
! rg -q 'V1-V4' src/test/java/com/vrudenko/kanban_board/config/FlywaySchemaProvenanceTest.java
! rg -q 'short SHA' .env.nonprod.example && rg -q 'main-<run_number>-<sha7>' .env.nonprod.example
! rg -q 'Same cap as' .github/dependabot.yml
! rg -q 'explicit ORDER BY' src/test/java/com/vrudenko/kanban_board/controller/BoardFullReadTest.java
! rg -q 'ULID' src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogCleanupIsolationTest.java && rg -q 'RandFlake' src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogCleanupIsolationTest.java
! rg -q 'Unsuspend' k8s/flux-system/apps-prod.yaml && rg -q 'suspend: false' k8s/flux-system/apps-prod.yaml
python3 scripts/verify-comments.py check
python3 scripts/verify-comments.py equiv --base $(cat Q/base.sha) $(fish Q/worklist.fish paths 1)
./gradlew spotlessCheck
fish Q/worklist.fish check 1</automated>
  </verify>
  <done>All ten task-1 findings are applied or listed in not-applied.txt with a reason. The six factual comments match the code. equiv prints OK for the 6 files, with the arrange/act/assert count unchanged in BoardFullReadTest. One commit holds exactly the task-1 paths (check with `git show --name-only --format= HEAD`) and went through the full hook. `check 1` prints its OK line. The user's files show the same `git status --short` lines as in Step 0.</done>
</task>

<task type="auto">
  <name>Task 2: Apply the 110 findings in src/main and src/test Java files</name>
  <files>src/main/java/com/vrudenko/kanban_board/config/RandFlakeGenerator.java, src/main/java/com/vrudenko/kanban_board/constant/MetricNames.java, src/main/java/com/vrudenko/kanban_board/dto/board_dto/UpdateBoardRequestDTO.java, src/main/java/com/vrudenko/kanban_board/dto/column_dto/ColumnFullResponseDTO.java, src/main/java/com/vrudenko/kanban_board/entity/UserEntity.java, src/main/java/com/vrudenko/kanban_board/mapper/ColumnFullMapper.java, src/main/java/com/vrudenko/kanban_board/mapper/TaskFullMapper.java, src/main/java/com/vrudenko/kanban_board/security/AuthenticationController.java, src/main/java/com/vrudenko/kanban_board/security/ProblemDetailAuthenticationEntryPoint.java, src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java, src/main/java/com/vrudenko/kanban_board/service/BoardService.java, src/main/java/com/vrudenko/kanban_board/service/ColumnService.java, src/main/java/com/vrudenko/kanban_board/service/SubtaskService.java, src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogConsumerE2ETest.java, src/test/java/com/vrudenko/kanban_board/activitylog/HistoricalActivityEventReconstructor.java, src/test/java/com/vrudenko/kanban_board/activitylog/HistoricalSchemaRehearsalE2ETest.java, src/test/java/com/vrudenko/kanban_board/activitylog/SchemaCompatibilityE2ETest.java, src/test/java/com/vrudenko/kanban_board/activitylog/SchemaRegistryOutageE2ETest.java, src/test/java/com/vrudenko/kanban_board/config/ComposedConstraintPropertyCustomizerTest.java, src/test/java/com/vrudenko/kanban_board/config/OpenApiParameterCompletenessTest.java, src/test/java/com/vrudenko/kanban_board/controller/BoardControllerTest.java, src/test/java/com/vrudenko/kanban_board/controller/ColumnControllerTest.java, src/test/java/com/vrudenko/kanban_board/controller/ThemePersistenceTest.java, src/test/java/com/vrudenko/kanban_board/dto/OptionalNotBlankTest.java, src/test/java/com/vrudenko/kanban_board/e2e/board/BoardCreationE2ETest.java, src/test/java/com/vrudenko/kanban_board/e2e/column/ColumnLockingTest.java, src/test/java/com/vrudenko/kanban_board/e2e/observability/EagerMetricRegistrationTest.java, src/test/java/com/vrudenko/kanban_board/e2e/observability/ObservedAspectWiringTest.java, src/test/java/com/vrudenko/kanban_board/e2e/reset/ResetControllerE2ETest.java, src/test/java/com/vrudenko/kanban_board/e2e/subtask/SubtaskLockingTest.java, src/test/java/com/vrudenko/kanban_board/e2e/task/TaskMoveTest.java, src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java, src/test/java/com/vrudenko/kanban_board/security/AuthenticationTest.java, src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java, src/test/java/com/vrudenko/kanban_board/security/InjectionAttemptTest.java, src/test/java/com/vrudenko/kanban_board/security/ResetEndpointProfileGatingTest.java, src/test/java/com/vrudenko/kanban_board/security/SigninTimingEqualizationTest.java, src/test/java/com/vrudenko/kanban_board/service/BoardServiceTest.java, src/test/java/com/vrudenko/kanban_board/service/ColumnServiceTest.java, src/test/java/com/vrudenko/kanban_board/support/containers/AbstractPostgresContainerTest.java, src/test/java/com/vrudenko/kanban_board/support/fixtures/AbstractAppMockMvcTest.java, src/test/java/com/vrudenko/kanban_board/support/fixtures/AbstractAppTest.java</files>
  <read_first>
    - /home/andre/.claude/skills/review-comments/references/rubric.md (only when a replacement no longer fits the code)
  </read_first>
  <action>
Run `fish Q/worklist.fish files 2`; it lists 42 files and 110 findings. Do src/main first (13 files, 16 findings), then src/test (29 files, 94 findings). Go file by file with shared rules 2 to 8, finishing each file before the next.

What the findings are, so you can size each one:
- About 54 are AAA rows (planning-time count): a marker followed by narration, reduced to the bare marker under rule 5. Their worklist WHY reads "Marker carries narration" or "A marker with narration", or the TEXT simply starts with a marker. Four replacements keep text after the marker: SchemaCompatibilityE2ETest (grading line 107), ResetControllerE2ETest (320), and the two `act & assert` rows in ColumnControllerTest (467, 538). Treat them with rule 5: bare marker, then the kept why on the next line, except a combined marker, which stays.
- The rest are sibling cross-references (rule 7 in the rubric: drop the sibling name, keep the fact), restatements (deletes), route counts that rot (AuthorizationGatingTest: drop the number, keep the rest), and planning tokens (AuthenticationController's scan finding id, OpenApiParameterCompletenessTest's spike number: drop the token, keep the decision).
- Security files (AuthenticationController, ProblemDetailAuthenticationEntryPoint, SecurityConfiguration, SigninTimingEqualizationTest, AuthorizationGatingTest, InjectionAttemptTest): a rewrite keeps every decision and boundary the original states, and drops only the token or sibling the finding names. List each security-file edit in SUMMARY.md in one line.
- UserEntity's finding deletes a commented-out code line. That is a comment deletion, so do it; equiv confirms it.

After the last file, run the gates in the verify block, commit with shared rule 10 (subject such as `docs(comments): apply review-comments verdicts to Java comments`), then run `fish Q/worklist.fish check 2`.
  </action>
  <verify>
    <automated>Run each line from the repo root; every one must exit 0.
python3 scripts/verify-comments.py check
python3 scripts/verify-comments.py equiv --base $(cat Q/base.sha) $(fish Q/worklist.fish paths 2)
./gradlew spotlessCheck
fish Q/worklist.fish check 2</automated>
  </verify>
  <done>`check 2` exits 0, so every finding in tasks 1 and 2 is applied or listed in not-applied.txt with a reason. equiv prints OK for all 42 files, with no arrange/act/assert count change. The task-2 commit holds only task-2 paths and went through the full hook, fastTest included.</done>
</task>

<task type="auto">
  <name>Task 3: Apply the 75 non-Java findings, then run the final verify and clean up</name>
  <files>.github/workflows/api-fuzz.yml, .github/workflows/deploy.yml, .github/workflows/invariant-checks.yml, .github/workflows/security-scan.yml, .planning/milestones/v1.2-phases/05-infra-migration/scripts/verify-baseline-schema.sh, .planning/quick/260808-ku4-analyze-kanban-mock-up-pdf-and-produce-a/extract-mockup-text.py, .planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/migrate_docs.py, .planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/verify_migration.py, .planning/quick/261006-fby-ban-javadoc-html-tags-and-inline-tags-in/sweep-javadoc-markup.py, .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml, .serena/project.yml, infra/vm/k3s-host-firewall.sh, infra/vm/k3s/config.yaml, k8s/base/redpanda/redpanda.yaml, k8s/flux-system/data.yaml, k8s/monitoring/controllers/kube-prometheus-stack.yaml, k8s/monitoring/controllers/repositories.yaml, k8s/overlays/nonprod/ingressroute.yaml, k8s/platform/edge/ingressroute-monitoring.yaml, scripts/fuzz/run-fuzz-selftest.py, scripts/verify-comments-selftest.py, scripts/verify-diagrams.py, scripts/verify-k8s-invariants-selftest.py, scripts/verify-k8s-invariants.py, scripts/verify-k8s-manifests.sh, scripts/verify-postgres-init-quoting.sh, src/main/resources/application-test.properties, src/main/resources/application.properties, .planning/quick/261024-jvc-apply-the-198-review-comments-findings-t/261024-jvc-SUMMARY.md</files>
  <read_first>
    - k8s/data/postgres/kustomization.yaml and k8s/monitoring/configs/kustomization.yaml (the only configMapGenerators: the postgres init scripts, which this plan leaves alone, and the dashboard JSON, which no finding touches)
    - scripts/verify-k8s-invariants.py, the I10 docstring (shared rule 6)
  </read_first>
  <action>
Run `fish Q/worklist.fish files 3`; it lists 28 files and 75 findings. The 3 ROLLOUT_GATED rows (k8s/data/postgres/init/01-create-databases-and-roles.sh twice, 02-create-monitoring-role.sh once) are not in that list and are never edited. List each of them in SUMMARY.md with the reason: the postgres-init configMapGenerator hashes the file, so any edit renames the ConfigMap and restarts the Postgres StatefulSet.

Before editing a k8s/ or infra/ file, confirm the finding sits in a YAML or shell comment, not inside a block scalar (a `|` or `>` value) and not in a generator input; otherwise add it to not-applied.txt. Planning confirmed all 8 of this task's k8s/ and infra/ findings are plain comments. The infra/vm files are installed by hand (infra/vm/README.md, infra/vm/k3s/install.sh), so a comment edit triggers nothing.

Go file by file with shared rules 2 to 8. Groups, so you can pace them:
- .github/workflows/ (4 files, 9 findings): workflow comments outside and inside run blocks. Both are comments to the lint, and equiv checks both.
- scripts/ (7 files, 29 findings): mostly banner and case-label deletes in the selftests, plus planning tokens in verify-k8s-manifests.sh. For the "Task 3" token, name the real consumer script, found with rg.
- src/main/resources/*.properties (2 files, 12 findings): section-banner deletes. Keep one blank line between groups of keys.
- k8s/ (6 files) and infra/ (2 files): 8 findings, the ingressroute one under shared rule 6.
- .serena/project.yml (4 findings) and the six .planning/ files (13 findings): the comment lint excludes these paths, so equiv skips them. Prove them with yamleq for the two YAML files, `bash -n` for the .sh, `python3 -m py_compile` for the .py, and a read of `git diff -U0 $(cat Q/base.sha) -- <those paths>` confirming every changed line is a comment or docstring line.

Then run the gates in the verify block and commit with shared rule 10 (subject such as `docs(comments): apply review-comments verdicts to scripts, workflows and manifests`). This commit holds k8s manifests and properties files, so it goes through the full hook.

Then the final verify, which is the orchestrator's required step: `fish Q/worklist.fish check 3` runs `python3 /home/andre/.claude/skills/review-comments/scripts/whole_tree.py verify /home/andre/dev/kanban-board-backend --state-dir /home/andre/.claude/skills/review-comments/state` and saves its output to Q/verify-task3.log. From that log, record in SUMMARY.md:
- the last line (ledger counts)
- the UNAPPLIED count split by verdict: `awk '/^UNAPPLIED/{print $3}' Q/verify-task3.log | sort | uniq -c`
- the PENDING count: `rg -c '^PENDING' Q/verify-task3.log`

Expected, and not to be chased: about 400 UNAPPLIED deletes (the bare AAA markers kept on purpose), about 186 UNAPPLIED decision-record rows (the script reports every non-keep verdict as UNAPPLIED), 3 UNAPPLIED rewrites for the rollout-gated rows, plus one row per not-applied.txt entry. PENDING rows are the rewritten comments' new text and any neighbour whose anchor changed. Report the real numbers; the gate is check 3's exit code, not a match to these estimates.

Then check for orphaned Testcontainers left by the three hook runs: `docker ps -a --filter label=org.testcontainers=true --format '{{.ID}} {{.CreatedAt}} {{.Status}} {{.Image}}'`. Remove only stopped containers created after the start time Task 1 Step 0 noted, and report what you removed.

Write Q/261024-jvc-SUMMARY.md: the three commit SHAs; applied counts per task; every not-applied.txt line and the 3 rollout-gated rows with reasons; grading deviations (I10 and any others under rule 6); observations (the IMAGE_TAG value's older form, the Flyway test counting 6 of 9 migrations, anything else found); the security-file edit list; the verify counts above; the final `git status --short`, showing the user's files exactly as Task 1 Step 0 recorded them. Then delete the helpers with `trash-put Q/worklist.jq Q/worklist.jsonl Q/worklist.fish Q/check-applied.jq Q/not-applied.txt Q/base.sha Q/commit-msg.txt Q/verify-task*.log`, leaving only PLAN.md and SUMMARY.md in Q. Copy anything SUMMARY.md needs from them first.
  </action>
  <verify>
    <automated>Run each line from the repo root, before the cleanup step; every one must exit 0.
python3 scripts/verify-comments.py check
python3 scripts/verify-comments.py equiv --base $(cat Q/base.sha) $(fish Q/worklist.fish paths 3)
git diff --quiet $(cat Q/base.sha) -- k8s/data/postgres/init/ src/main/resources/db/migration/
fish Q/worklist.fish yamleq .serena/project.yml
fish Q/worklist.fish yamleq .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml
bash -n scripts/verify-k8s-manifests.sh && bash -n scripts/verify-postgres-init-quoting.sh && bash -n infra/vm/k3s-host-firewall.sh
bash -n .planning/milestones/v1.2-phases/05-infra-migration/scripts/verify-baseline-schema.sh
python3 -m py_compile .planning/quick/260808-ku4-analyze-kanban-mock-up-pdf-and-produce-a/extract-mockup-text.py
python3 -m py_compile .planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/migrate_docs.py .planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/verify_migration.py
python3 -m py_compile .planning/quick/261006-fby-ban-javadoc-html-tags-and-inline-tags-in/sweep-javadoc-markup.py
python3 scripts/verify-k8s-invariants-selftest.py
python3 scripts/verify-comments-selftest.py
python3 scripts/fuzz/run-fuzz-selftest.py
python3 scripts/verify-diagrams-selftest.py
fish Q/worklist.fish check 3</automated>
  </verify>
  <done>`check 3` exits 0, so all 195 applicable findings are applied or listed with a reason. The postgres init scripts and migrations are byte-identical to base.sha. Every selftest passes, as all four did at planning time. SUMMARY.md holds the final verify counts, the not-applied and rollout-gated list, deviations and observations. Q holds only PLAN.md and SUMMARY.md. Three commits exist on main, none pushed, none holding a user file.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| repo, then Flux on the next push | any committed byte under k8s/ reaches the production cluster once someone pushes main |
| working tree, then commit | the tree holds the user's uncommitted work and an untracked key file next to this plan's edits |
| comment text, then the reader's model of the code | a deleted or rewritten comment on security code changes what the next reader believes a guard is for |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261024-jvc-01 | Tampering | k8s/data/postgres/init/*.sh (configMapGenerator input) | high | mitigate | The worklist marks all 3 rows ROLLOUT_GATED and `paths 3` excludes them; Task 3's verify asserts the directory is byte-identical to base.sha; verify-comments.py's ROLLOUT_GATED_GLOBS records the same rule. |
| T-261024-jvc-02 | Tampering | code, config values and keys in the 76 edited files | high | mitigate | verify-comments.py equiv against base.sha on every task's paths (Java AST with comments stripped, parsed YAML, a quote-aware shell lexer, properties, Python AST without docstrings); yamleq, bash -n and py_compile for the .serena/ and .planning/ files equiv skips; fastTest through the hook on every commit. |
| T-261024-jvc-03 | Information disclosure | kanban-backup.agekey and the user's uncommitted files | critical | mitigate | Commits use only `git commit -F msg -- $(worklist.fish paths N)` after confirming the index is empty; never `git add -A` or `.`; the key file is never read; gitleaks scans every staged diff in the hook; never push. |
| T-261024-jvc-04 | Tampering | security-rationale comments (AuthenticationController, SecurityConfiguration, ProblemDetailAuthenticationEntryPoint, SigninTimingEqualizationTest, AuthorizationGatingTest, InjectionAttemptTest) | medium | mitigate | Shared rule 4 and Task 2: a rewrite keeps every decision and boundary and drops only the named token or sibling; each security-file edit is listed in SUMMARY.md for review. |
| T-261024-jvc-05 | Tampering | user's in-progress AsyncConfig.java and KafkaConsumerConfig.java | medium | mitigate | Shared rule 9: Spotless is applied only through `-PspotlessIdeHook` with this plan's paths; a bare spotlessApply is forbidden. |
| T-261024-jvc-SC | Tampering | npm/pip/cargo installs | low | accept | This plan installs no package; the only tools used (rg, jq, fish, python3 with PyYAML, gradle, docker) are already present and were exercised at planning time. |
</threat_model>

<verification>
- `fish Q/worklist.fish check 3` exits 0 (run inside Task 3, before the cleanup step).
- `python3 scripts/verify-comments.py check` exits 0, and equiv against base.sha prints OK for each task's paths.
- `git log --oneline $(cat Q/base.sha)..HEAD` shows three `docs(comments):` commits (more only if shared rule 11 split a task), none touching a user file.
- `git diff --quiet $(cat Q/base.sha) -- k8s/data/postgres/init/` exits 0.
</verification>

<success_criteria>
- The six factual-error comments are true of the current code.
- All 195 applicable findings are applied or individually listed with a reason in SUMMARY.md; the 3 rollout-gated findings are listed as left alone.
- Zero code, config-value or key changes, proven per file family by equiv, yamleq, bash -n and py_compile.
- The AAA-marker count is unchanged in every Java test file.
- The final whole_tree.py verify output is recorded, with UNAPPLIED split by verdict and the PENDING count.
- The user's files are untouched and uncommitted; nothing was pushed; the helper files are gone.
</success_criteria>

## Source coverage audit

| Source | Item | Covered by |
|--------|------|------------|
| GOAL | Apply the 198 review-comments findings to code comments, no code changes | Tasks 1 to 3 (10 + 110 + 78 rows = 198) |
| CONTEXT | Six factual errors first, each rewritten true against the real code | Task 1 Step 1 (one bullet per error) |
| CONTEXT | One plan, three tasks grouped by risk, Edit tool only, no line-number script | Plan shape; shared rules 2 and 3; Approaches table |
| CONTEXT | Comments only; tidy blank lines; spotlessApply if spotlessCheck complains | Shared rules 7 and 9; equiv gates (Spotless scoped to this plan's files so the user's files are never formatted) |
| CONTEXT | Never delete or reduce bare AAA markers; narrated marker becomes bare | Shared rule 5; equiv marker count; the 2 marker-dropping replacements named |
| CONTEXT | Plain text per CODE_STYLE rule 14; verify-comments.py passes | Shared rule 3; `verify-comments.py check` in every task |
| CONTEXT | Leave the rollout-triggering postgres init script alone and list it; check k8s/infra for rendered changes | Worklist skip field (02-create-monitoring-role.sh is in the same hashed generator, so it is left alone too); Task 3 block-scalar and generator check; T-261024-jvc-01 |
| CONTEXT | Unapplicable finding listed with reason, not forced | Shared rule 4; not-applied.txt; SUMMARY.md |
| CONTEXT | Hands off the user files; explicit-path commits | Shared rules 8 and 10; T-261024-jvc-03 |
| CONTEXT | One commit per task; hook on Java commits; no push or amend | Shared rule 10 (the hook runs on all three, since task 3 holds k8s manifests) |
| CONTEXT | Final whole_tree.py verify, record output, report UNAPPLIED and PENDING | Task 3 final-verify step |
| CONTEXT | Repo-root-relative paths in the body; do not commit PLAN/SUMMARY/STATE | Plan body; shared rule 10 |
| CONTEXT | About 40% context per task, file by file, each file read once | Shared rules 2 and 11; file-owned task grouping |
| REQ | No ROADMAP requirement ids for a quick task | requirements: QUICK-261024-jvc |
| RESEARCH | None for this quick task | n/a |

One regrouping against the orchestrator's sketch: four non-factual findings in two of the six factual-error files (.env.nonprod.example x2, BoardFullReadTest x2) moved into Task 1, so each file is opened by exactly one task. The six factual errors still come first.

<output>
Create `.planning/quick/261024-jvc-apply-the-198-review-comments-findings-t/261024-jvc-SUMMARY.md` when done (Task 3), and do not commit it.
</output>
