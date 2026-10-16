---
phase: quick-261016-lcu
plan: 01
subsystem: planning, build
tags: [phase-14-plan-review, promtool, cleanup-verifies, build.gradle-comments]
status: complete
requirements-completed: [261016-lcu]
key-files:
  modified:
    - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-01-PLAN.md to 14-11-PLAN.md
    - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-PATTERNS.md
    - build.gradle
actuals:
  tokens: 40000
  tasks: 2
  commits: 2
plan_head_before: 73cd948a48d5d743513fd168fdd73246a8b29380
plan_head_after: 388fcc62562f07a5348f66d443c8d0ee71e68dd5
---

# Phase quick-261016-lcu Plan 01: Phase 14 plan review fixes and build.gradle comment trim Summary

BUG-1..BUG-7 are fixed in the Phase 14 plan text with offline evidence (cleanup-verify harness, promtool fixture), and build.gradle's comments are trimmed to the rubric through the full pre-commit hook.

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 | 0884063 | docs(14): fix plan review findings BUG-1..BUG-7 (12 files, `--no-verify`, allowed for planning docs) |
| 2 | 388fcc6 | docs(build): trim build.gradle comments to the comment rubric (full hook, no `--no-verify`) |

`commits: 2` is measured with `git rev-list --count` from the base 73cd948.

## Per-BUG changes and evidence

- **BUG-1 (14-02).** The age step now reads `age -r "$AGE_RECIPIENT" -o "$WORK_DIR/db.dump.age" "$WORK_DIR/db.dump"`, and the script logs the dump size in bytes before encrypting. The behaviour bullet, the must_haves truth and the smoke-harness description now require the decrypted object to equal the logged size and be larger than 0. Two acceptance items were added: a grep for the input argument, and SUMMARY evidence of the failing direction. Evidence: text-only. The script and harness do not exist yet, so the harness's FAIL/PASS directions could not be run here; the 14-02 executor must record them.
- **BUG-2 (14-05).** Task 1 step 3 now prescribes `countPublishFailure(ActivityEvent)` with two call sites: the whenComplete failure branch and a rethrowing catch around `toAvro` and `send`. A new `KafkaPublishFailureE2ETest` is specified for the asynchronous path, with the paused Redpanda container, lowered producer timeouts and an explicit stop-and-report path. Step 5 lists the separate failing directions: catch increment removed turns SchemaRegistryOutageE2ETest red; whenComplete increment removed turns KafkaPublishFailureE2ETest red. Also updated: files_modified, Task 1 files and read_first, the behaviour list, truths, data_flow, the artifacts row, the acceptance items, T-14-47, and the instruction to rewrite KafkaEventPublisher's and SchemaRegistryOutageE2ETest's Javadoc. 14-PATTERNS.md lines 20 and 97 were updated to match.
- **BUG-3 (six suspension checks and the 14-10 deletion check).** Each is now a checked assignment followed by the test: `suspended=$(ssh ...) && test -z "$suspended"`. The 14-10 deletion check is three assignments with `--ignore-not-found`, then the assertions. The `<fails_when>` lines now mention an unreadable state. Evidence: the planner's red run printed 7 FAIL lines and exited 1. My red baseline run before editing printed `checked 7 cleanup verifies, 7 failing case(s)`. After the rewrite, `fish check-cleanup-verifies.fish` printed `checked 7 cleanup verifies, 0 failing case(s)` and exited 0.
- **BUG-4.** `-x jacocoTestCoverageVerification` was added to all 9 `./gradlew test --tests` sites (14-01 x2, 14-04 x3, 14-05 x2, 14-07 x2), and every `| tail` suffix on a Gradle command was removed. Gate checks: no `--tests` line lacks the flag, 9 sites exist, and `gradlew.*| *tail` has no match in any plan.
- **BUG-5.** 14-06 now prescribes `(kanban_activity_dlt_written_total unless max_over_time(kanban_activity_dlt_written_total[1h] offset 10m)) > 0 or increase(kanban_activity_dlt_written_total[10m]) > 0`, with the required YAML comment content, the fixture in read_first and the acceptance grep. 14-08 requires the four unit-test cases. 14-05's truth, trade-off and T-14-47 no longer claim registration alone covers the first dead letter. 14-10 row 3 notes which branch the live injection exercises. Evidence: promtool v3.14.0 `test rules` on the fixture printed SUCCESS, re-run in this session.
- **BUG-6.** 14-10's substituted lag row relaxes both comparisons to `>= 0` (context line and step 4). 14-06's production ActivityConsumerLagRising expression is unchanged (the `> 0` grep still prints one match in 14-06). The fixture shows the relaxed row fires on flat zero lag and the old row does not.
- **BUG-7.** 14-07 no longer adds an ObservationPredicate or a config class: the frontmatter, truths, artifacts, objective, trade-offs, artifacts table, Task 1 name and files, behaviour, step 4, step 5, acceptance, done and T-14-28 were all rewritten. The management-port no-span test is now a characterisation of Boot, paired with a main-port SERVER-span positive control, with a stop-and-report path if it shows a span at RED. 14-05's trade-off no longer cites the predicate, and 14-09's precondition now checks for ContextPropagatingTaskDecorator in AsyncConfig.java. The 14-07 characterisation was not run (it needs a JVM); the claim rests on Claude finding 04.

## Cross-check (step 1h)

I ran rg for each changed identifier over the plans, 14-PATTERNS.md and COVERAGE.md. Remaining hits agree with the new text. COVERAGE.md, 14-12 and 14-13 held no stale restatement. `ObservabilityConfig` appears in no plan, and `ObservationPredicate` appears only in 14-07's "do not add" text.

## Validation

All of 14-01 to 14-11 pass `frontmatter.validate --schema plan` and `verify.plan-structure` with no errors or warnings (run via `node ~/.claude/gsd-core/bin/gsd-tools.cjs query ...` because the sourced-wrapper form was refused in this worktree). `git diff HEAD` over src/, 14-CONTEXT.md and 14-RESEARCH.md is empty.

## Task 2: build.gradle

- Free memory was 3157 MB before the commit (the 3000 MB gate passed). Docker was up.
- The full hook ran to completion: gitleaks, comment lint, instruction budget, spotlessCheck and fastTest. It ended `BUILD SUCCESSFUL in 5m 3s`; wall time 13:51:27Z to 13:56:58Z (5 min 31 s). Memory afterwards: 2326 MB available.
- Post-commit gates: the commit holds build.gradle alone, and a diff-shape count of non-comment, non-blank, non-description lines is 0. `python3 scripts/verify-comments.py check` printed OK. All 12 targeted phrases are gone, and the line-17 claim, both fork settings, `242.5s` and `7.728 GiB` remain.
- The hook left one Testcontainers PostgreSQL container running (no Ryuk on this host). It was created during my run (13:52:36Z), so I removed it with `docker rm -f`. Other exited containers in `docker ps -a` pre-date this run and were left alone.
- Gradle daemon stopped with `./gradlew --stop`.

## Deviations

- **DLT expression differs from the brief.** The brief's instant-offset form (`series unless series offset 10m`) was not adopted. In the fixture, one failed scrape writes a stale marker, and ten minutes later the instant lookup reads it as absence, so a pod whose counter is already 2 pages with no new dead letter (fires at 30m). The range-offset form stays silent in that case and still catches the new-pod and 0-to-1 cases.
- **Two adjacent extensions.** (1) BUG-4: the three full-suite `./gradlew test` verifies (14-04, 14-05, 14-07) also lost their tail pipe, since it masks the exit status the same way; they keep the JaCoCo ratchet. (2) build.gradle: 8 bare URL comment lines went, not the brief's 6; the datafactory and archunit ones are the same case.
- **Evidence files were already tracked.** check-cleanup-verifies.fish and promtool-fixture/ were committed in the base (73cd948) and unchanged, so Task 1's commit holds only the 12 Phase 14 files.
- **Task 1 commit used an explicit staged list.** I staged the 12 touched files and committed with the same pathspec.
- **Wording choices for the plan text.** I cited quick id 261016-lcu and the review findings in plan prose; these are not code comments.

## Not verifiable here

- 14-05's asynchronous-path mechanism (pausing Redpanda to get an exceptionally completing future) was not run; the rewritten task carries a stop-and-report path.
- The 14-07 management-port characterisation needs a JVM and a live app.
- The live ssh cleanup commands were exercised only against stubs.
- 14-02's smoke harness does not exist yet, so BUG-1's red/green directions are unrun.

## Open question for the user: build.gradle lines 17-18

The comment says the avro plugin's project "was archived by its owner on 2026-12-28". Today is 2026-10-16, so that date lies in the future. I left it untouched as instructed. Either the date is a typo or the claim is wrong; someone should check the plugin's repository.

## Left in place in build.gradle (relative-time phrases)

- "has three months of field exposure" (line 26 of the post-edit file).
- "report-only today (see the dependencyCheck block below)" (line 42).

## Reviewer findings deliberately out of scope

Claude 3 (pipefail against `shellcheck -s sh` in 14-02), Claude 4 (14-03 depends_on 14-01), Claude 6 (14-01's verification-metadata criterion against the OTel BOM poms), Claude 7 (bare python3 without PyYAML), and the 9081 /healthz 500 and empty-database-backup notes.

## Gotcha candidates (not recorded)

- A command substitution inside an emptiness test discards the command's exit status.
- An instant `offset` lookup in a "new series" check is defeated by stale markers, while a range lookup is not.

I did not run `record-gotcha.js`; both are documented in the plan text and the fixture.

## Self-Check: PASSED

Commits 0884063 and 388fcc6 are reachable from HEAD; the SUMMARY, harness, fixture and build.gradle exist; the harness reports 0 failing cases; promtool reports SUCCESS.
