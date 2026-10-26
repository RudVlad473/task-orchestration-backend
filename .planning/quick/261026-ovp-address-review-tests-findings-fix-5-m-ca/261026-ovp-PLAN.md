---
phase: quick-261026-ovp
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-check.py
  - .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task1.tsv
  - .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task2.tsv
  - .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task3.tsv
  - src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java
  - src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/security/AuthenticationTest.java
  - docs/learning/06-security-and-sessions.md
  - src/test/java/com/vrudenko/kanban_board/controller/BoardControllerTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/task/TaskMoveTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/board/BoardCreationE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/service/UserServiceTest.java
  - src/test/java/com/vrudenko/kanban_board/service/BoardServiceTest.java
  - src/test/java/com/vrudenko/kanban_board/service/ColumnServiceTest.java
  - docs/learning/04-service-layer-and-access-control.md
  - src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogConsumerE2ETest.java
  - src/test/java/com/vrudenko/kanban_board/config/FlywaySchemaProvenanceTest.java
  - src/test/java/com/vrudenko/kanban_board/config/ComposedConstraintPropertyCustomizerTest.java
  - src/test/java/com/vrudenko/kanban_board/e2e/observability/ObservedAspectWiringTest.java
  - docs/ARCHITECTURE.md
  - docs/learning/01-domain-model-and-schema.md
  - docs/learning/07-events-and-activity-feed.md
  - docs/learning/08-testing-strategy.md
autonomous: true
requirements: [261026-ovp]

estimate:
  tokens: 220000
  raw_tokens: 220000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Each of the five cannot-fail M cases (BoardControllerTest delete, TaskMoveTest order, AuthorizationGatingTest create-board and update-theme, ActivityLogConsumerE2ETest two-event detail) has a harness row FAIL/FAIL under its named src/main mutation and a PASS row on the reverted code."
    - "Every new test (same-column move down, same-column append, same-column clamp, theme DARK then LIGHT, consumer BoardDeleted, ColumnUpdated, SubtaskUpdated, SubtaskDeleted, Flyway V1-V9 history, exact env status) has a FAIL/FAIL row under a named mutation and a PASS row on real code."
    - "BoardCreationE2ETest's two duplicate-name cases assert DUPLICATE_RESOURCE only where the measured code under the guard-deletion mutation differs; the measured status and code for both mutations are in the SUMMARY."
    - "The five D duplicates and the ObservedAspectWiringTest advisor-order pin are deleted, the signup exact field-set assertion is gone while its value assertions stay, and no stale test name survives in src/test/java, docs/ARCHITECTURE.md or docs/learning (baseline at HEAD 8d995db: 16 hits in 12 files)."
    - "git status --porcelain -- src/main prints nothing and git diff --stat START -- src/main is empty after the last task; ./gradlew fastTest exits 0 on the final tree."
  artifacts:
    - path: .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-check.py
      provides: "Proof harness: src/main dirty/clean guard, fresh JUnit XML, per-method verdict, TSV evidence row"
    - path: .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task1.tsv
      provides: "Both-direction evidence for the security group"
    - path: .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task2.tsv
      provides: "Both-direction evidence for the controller, service, e2e/task and e2e/board group"
    - path: .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task3.tsv
      provides: "Both-direction evidence for the activitylog, config and e2e/observability group"
  key_links:
    - from: "261026-ovp-mutation-check.py"
      to: "build/test-results/test/TEST-*.xml"
      via: "deletes the directory before each run, so an XML it reads was written by that run"
    - from: "a hand-applied src/main mutation"
      to: "the next PASS run"
      via: "git checkout -- src/main (plus the reverse mv for the V9 rename), then the harness refuses a PASS run while src/main is dirty"
---

<objective>
Act on the review-tests findings for src/test/java/com/vrudenko/kanban_board/: make the five cannot-fail
(M) tests fail under the production mutation each finding names, delete the five duplicate (D) cases and
two shape pins, and add the missing tests, each proven to FAIL under a hand-applied mutation and PASS on
the real code. Tests and their doc references only; src/main ends byte-identical.

Outcome and use: the suite catches the regressions the review showed it misses, and the SUMMARY gives the
user a per-test record of which mutation each test was proven against, in which direction, so the next
review can trust those rows without re-running them.

Output: edits to 15 test and doc files, one proof harness, three evidence logs, one SUMMARY.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@docs/CODE_STYLE.md
@docs/SESSION_LESSONS.md

Line numbers below come from the review run and from HEAD 8d995db; they may drift by a line or two, so
locate by method name. Base classes, the two-request `.with(user())` ceiling and the error envelope are in
docs/ARCHITECTURE.md, sections "Writing a new test: package, tier and base class" and "Scenario — how a
rejected request differs across 401 / 403 / 400 / 409".
</context>

## Approaches considered (CLAUDE.md, before approval)

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A. Hand-run every proof: edit src/main, run the scoped class, read the Gradle console, revert | Pro: no new code. Con: about 25 runs read by eye; a compile error, a context that failed to start or a stale XML from the previous run all look like "the test failed"; nothing checks the revert happened; the evidence is scrollback | Rejected |
| B. A small stdlib-Python harness in the quick dir that refuses a mutation run on clean src/main and a pass run on dirty src/main, clears build/test-results, runs one scoped Gradle invocation, judges each named method from the JUnit XML and appends a TSV row | Pro: each proof is mechanical, an invalid run cannot pass as a failure, the revert is checked before every pass run, the TSV is the SUMMARY's evidence. Con: about 100 throwaway lines that must themselves be proven (the tracer does that) | Picked |
| C. A mutation-testing plugin (PIT via gradle-pitest-plugin) | Pro: industry standard, finds survivors nobody named. Con: a new build dependency (package-legitimacy gate, gradle/verification-metadata.xml), reruns the Testcontainers suite per mutant, and its operators cannot express the named mutations (pass another user's id, swap LinkedHashMap for HashMap, turn V9 into a repeatable migration) | Rejected: out of scope for a tests-only change |

Per-item choices inside B, each with its rejected alternative:

- Flyway history: extend the IN list to '1'..'9' and expect 9, rather than asserting the exact version list
  or deriving it from the classpath. An exact list fails on every legitimate new migration (an inventory
  test); a classpath-derived list is more code for a check Hibernate validate mostly implies. Cost: V10
  stays uncovered until someone extends the list.
- Task order tie: name the tied pair's expected order with String.compareTo on two real ids, rather than
  persisting hand-built TaskEntity rows with literal ids. Java's String order and PostgreSQL's collation
  agree on lowercase base36, and the test keeps the production create path.
- Theme round trip at the service tier (UserServiceTest, JDBC read of the stored column, as its sibling
  does) rather than in ThemePersistenceTest over HTTP, where each extra write or read spends the
  two-request session ceiling.
- Same-column move cases in TaskMoveTest beside the cross-column append and clamp cases they mirror,
  rather than in TaskServiceTest.

## Non-obvious trade-offs

- Memory: every scoped run boots a Gradle JVM and a PostgreSQL container (test runs with maxParallelForks
  = 2, so a multi-class run can start two); ActivityLogConsumerE2ETest adds a broker. The host has 8.7 GiB.
  Run one Gradle command at a time and run the three tasks one after another, never in parallel worktrees
  (docs/SESSION_LESSONS.md lesson 9). The tasks touch disjoint files, so their order does not matter; the
  reason they cannot overlap is that src/main is shared mutable state during mutation runs.
- State invalidation: an unreverted mutation would leak into the next run or a commit. The harness
  refuses a pass run on dirty src/main; commits name only src/test, docs and the quick dir; the final
  check is git status on src/main. git checkout -- src/main does not delete an untracked file, which
  matters for the one rename mutation (V9 to R__).
- Stale results: Gradle can skip an up-to-date test task and leaves old XML behind on a compile error. The
  harness passes --rerun and deletes build/test-results/test first; no XML means INVALID, never FAIL.
- The task-order tie test is deterministic in its passing direction (ORDER BY decides). Its failing
  direction relies on PostgreSQL returning tied or unordered rows in heap order, which the write order
  controls; that is why both mutations are run, not assumed.
- Kafka mutation runs take at least 30 s per failing case (Awaitility's atMost), so the four new consumer
  cases are proven in one batched run (attribution argued in Task 3).
- Security: deleting the permitAll reachability class leaves signin and signup reachability to
  AuthenticationTest, whose signin and signup cases send no session and would get 401 if either route were
  gated. Dropping the signup exact field set leaves the hash-leak guard to the signin exact-set test, valid
  only while both endpoints return UserResponseDTO (open question 4).

## Data flow of a proof run

The executor edits src/main by hand; the harness checks src/main is dirty for a mutation run or clean for a
pass run, deletes build/test-results/test, runs one `./gradlew test --rerun --tests ...` and reads every
testcase from the fresh XML. A run counts only when each named method shows the expected outcome, a
failure counting only if it is an assertion failure, and one TSV row per testcase is appended to the task's
log. The revert (`git checkout -- src/main`, plus the reverse `mv` for the V9 rename) comes before the next
pass run, which the harness refuses while src/main is dirty.

## Mutation proof protocol (every task)

1. Make the test edits for one class, then one PASS run over it (catches a broken test before spending
   mutation runs).
2. For each named mutation: apply it to src/main by hand, run the harness with `--expect-fail` for the
   test(s) it targets, scoped to that class only, then revert and confirm `git status --porcelain --
   src/main` prints nothing.
3. After the task's last revert, one PASS run over all classes the task changed. A test edited after its
   PASS row needs both directions again.
4. A new or strengthened test whose mutation run does not FAIL is not accepted. Do not weaken the
   mutation or the expectation; fix the test or report "could not prove" with the harness row.

Every Gradle call goes through the harness, which adds `--rerun -x jacocoTestCoverageVerification` and
`< /dev/null` (the coverage ratchet fails any single-class run, and a hook-style stdin can hang Gradle).

<tasks>

<task type="tracer">
  <name>Task 1: proof harness, proven end-to-end on the update-theme case, then the security group</name>
  <files>.planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-check.py, .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task1.tsv, src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java, src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java, src/test/java/com/vrudenko/kanban_board/security/AuthenticationTest.java, docs/learning/06-security-and-sessions.md</files>
  <read_first>src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java (lines 50-150 and 386-560), src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java, src/test/java/com/vrudenko/kanban_board/security/AuthenticationTest.java (lines 220-250 and 430-475), src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java (lines 55-80), src/main/java/com/vrudenko/kanban_board/service/UserService.java (updateTheme), src/main/java/com/vrudenko/kanban_board/controller/BoardController.java (save)</read_first>
  <precondition>docker info succeeds; record START=$(git rev-parse HEAD) for the final src/main diff.</precondition>
  <action>
Tracer slice first, end to end, before anything else in this task.

(a) Write `.planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-check.py`, stdlib only, run as `python3 -I <path>` from the repo root. CLI: `--label TEXT`, `--log PATH`, `--tests PATTERN` (repeatable, passed through to Gradle), `--expect-fail METHOD` (repeatable), `--others-pass` (flag). In order: (1) read `git status --porcelain -- src/main`; with any `--expect-fail` it must be non-empty, without it must be empty, else print why and exit 3 before Gradle starts. (2) Delete `build/test-results/test`. (3) Run `./gradlew test --rerun <each --tests PATTERN> -x jacocoTestCoverageVerification` with stdin from /dev/null, output written to `build/261026-ovp-last-gradle.log`. (4) Parse every `build/test-results/test/TEST-*.xml` with xml.etree; if none exists, print the log's last 40 lines and exit 2 (INVALID: compile or context failure, never a FAIL). (5) Classify each testcase PASS, SKIP or FAIL (failure type plus first message line). A FAIL satisfies an expectation only when its type is java.lang.AssertionError, org.opentest4j.AssertionFailedError or org.awaitility.core.ConditionTimeoutException (Awaitility wraps the assertion). (6) Verdict: without `--expect-fail` every testcase must be PASS; with it, each named method (testcase name equal to METHOD followed by "()") must exist and FAIL that way, and with `--others-pass` every other testcase must be PASS. (7) Append one TSV row per testcase to `--log`: ISO time, label, classname, method, expected (FAIL for named methods, PASS in a pass run or under --others-pass, any otherwise), actual, failure type, message with tabs and newlines collapsed and cut at 200 chars, comma-joined dirty src/main paths. Print one verdict line; exit 0 only on a full match, 1 otherwise. Comments follow docs/CODE_STYLE.md rule 14 style even though the lint covers Java only.

(b) AuthorizationGatingTest, ScopedToCaller, update-theme case (~line 463): keep the status and id assertions, add that the response theme equals ThemePreference.DARK (the value UPDATE_THEME_BODY writes), then GET USER_THEME_URL as the owning user (one `.with(user(...))` call for that principal) and assert its theme is still ThemePreference.LIGHT.

(c) Prove the slice. PASS run labelled t1-tracer-pass over `*AuthorizationGatingTest`. Guard self-check: the same command with `--expect-fail` on that method and clean src/main must exit 3 without starting Gradle; record that exit code in the SUMMARY. Mutation M-T1: delete the line `user.setTheme(dto.getTheme());` in UserService.updateTheme; run with `--expect-fail` on the update-theme method (label t1-M-T1); revert. Read the log rows back and confirm they say what the console said. Only then continue.

(d) Same class, create-board case (~line 421): after the POST, read the created id from the response body, GET BOARDS_URL as the foreign user (its second and last `.with(user(...))` call in the method, docs/ARCHITECTURE.md two-request ceiling) and assert the returned ids contain it. Mutation M-T2: in BoardController.save pass the id of another existing user instead of userId (for example the first id from userService.findAll() that differs from userId); `--expect-fail` on the create-board method; revert.

(e) Same class: delete the nested class holding the two permitAll reachability tests (signin and signup, ~lines 489-546). Their imports become unused (ValidationConstants, SigninRequestDTO, SignupRequestDTO); spotlessApply's removeUnusedImports drops them, confirm they are gone. AuthenticationTest's signin and signup cases already send no session.

(f) ActuatorHealthE2ETest: delete the first PublicHealthTest case (the status-only 200 check on HEALTHZ_PATH; the no-component-detail case asserts the same 200). In the env case (~line 142) replace the at-least-300 range assertion with an exact HttpStatus. Predicted 401: managementSecurityFilterChain matches EndpointRequest.toAnyEndpoint(), which covers exposed endpoints only, so an unexposed /actuator/env falls through to the main chain and is refused unauthenticated. Write the prediction, run; if the run fails, the message carries the actual status. Adopt it only if it is 401 or 404; a 3xx or 5xx is a finding to report, not to pin. Rename the method after the measured status (rg docs/ARCHITECTURE.md docs/learning for the old name first and update any hit) and rewrite the assert comment to the measured mechanism. Mutation M-T3: append `,env` to management.endpoints.web.exposure.include in src/main/resources/application.properties (env then answers 200); `--expect-fail` on the env method; revert.

(g) AuthenticationTest, signup created-identity case (~line 437): delete the four lines that build the field-name set and assert it; keep the status, email, displayName, id and theme value assertions. Rewrite its Javadoc (~line 434) so it no longer promises an exact-set check. Leave the signin exact-set case (~line 225) and every other shape row alone.

(h) docs/learning/06-security-and-sessions.md ~line 120: replace the bullet naming the deleted nested class with one saying AuthenticationTest's signin and signup cases reach both routes with no session. Do not edit docs/wiki/ copies (.claude/CLAUDE.md: originals stay authoritative).

(i) `./gradlew spotlessApply < /dev/null`, `python3 scripts/verify-comments.py check < /dev/null`, final PASS run labelled t1-pass-final over the three classes. Commit: `git add` the harness and the task-1 log, then `git commit -m "<conventional message with the Co-Authored-By trailer>" -- <this task's paths> < /dev/null` with a Bash timeout of 600000 ms. Never --no-verify (Java changes run the 4-minute hook), never push, never amend.
  </action>
  <verify>
    <automated>Q=.planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca; test -z "$(git status --porcelain -- src/main)" && python3 -I $Q/261026-ovp-mutation-check.py --label t1-verify --log $Q/261026-ovp-mutation-log-task1.tsv --tests '*AuthorizationGatingTest' --tests '*ActuatorHealthE2ETest' --tests '*AuthenticationTest' && test "$(rg -c '\tFAIL\tFAIL\t' $Q/261026-ovp-mutation-log-task1.tsv)" -ge 3 && ! rg -q 'PermitAllExclusions|shouldReturnOk_whenHealthzCheckedWithoutSession|MULTIPLE_CHOICES' src/test/java docs/learning docs/ARCHITECTURE.md</automated>
  </verify>
  <done>The harness exits 3 on a mutation run with clean src/main, 2 on a run with no XML, and 0 only on a full match. The task-1 log holds FAIL/FAIL rows for M-T1 (update-theme), M-T2 (create-board) and M-T3 (env status) and PASS rows for every testcase of the three classes on the reverted code. The stale-name gate is 0 hits, down from 4 at HEAD (AuthorizationGatingTest 1, docs/learning/06 1, ActuatorHealthE2ETest 2). The commit exists and src/main is clean.</done>
</task>

<task type="auto">
  <name>Task 2: board delete, task order, same-column moves, theme round trip, duplicate-name codes, renames</name>
  <files>src/test/java/com/vrudenko/kanban_board/controller/BoardControllerTest.java, src/test/java/com/vrudenko/kanban_board/e2e/task/TaskMoveTest.java, src/test/java/com/vrudenko/kanban_board/e2e/board/BoardCreationE2ETest.java, src/test/java/com/vrudenko/kanban_board/service/UserServiceTest.java, src/test/java/com/vrudenko/kanban_board/service/BoardServiceTest.java, src/test/java/com/vrudenko/kanban_board/service/ColumnServiceTest.java, docs/learning/04-service-layer-and-access-control.md, .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task2.tsv</files>
  <read_first>src/test/java/com/vrudenko/kanban_board/e2e/task/TaskMoveTest.java (lines 1-480), src/main/java/com/vrudenko/kanban_board/service/TaskService.java (moveToColumn, lines 206-312), src/main/java/com/vrudenko/kanban_board/repository/TaskRepository.java, src/main/java/com/vrudenko/kanban_board/service/BoardService.java (deleteById, updateById, save and its Decisions comment), src/main/java/com/vrudenko/kanban_board/service/UserService.java (addBoardByUserId, updateTheme), src/test/java/com/vrudenko/kanban_board/e2e/board/BoardCreationE2ETest.java (lines 225-400), src/test/java/com/vrudenko/kanban_board/service/UserServiceTest.java</read_first>
  <precondition>Task 1's harness exists and git status --porcelain -- src/main prints nothing.</precondition>
  <action>
Follow the mutation proof protocol; log to `261026-ovp-mutation-log-task2.tsv`; label each run t2-...

(a) BoardControllerTest, DeleteById: after the delete, GET getBoardPrefix() as the same user (the method's second `.with(user(...))` call) and assert the returned ids equal exactly the ids of mockEmptyBoards (containsExactlyInAnyOrderElementsOf), a positive literal expectation that also shows the read worked. Mutation M-B1: delete `boardRepository.deleteById(deletedBoardId);` in BoardService.deleteById (columns still go, the board row stays).

(b) TaskMoveTest, MoveToColumn: replace the repeat-read test (~line 417, the one comparing two reads of the same query) with shouldOrderByPositionThenId_whenTwoTasksSharePosition. Arrange: a fresh column via createEmptyColumn and three tasks first, second, third via createTaskInColumn (positions 0, 1, 2). Name lowerId and higherId by String.compareTo on first's and second's ids. Through taskRepository (findById, set position, save; each save its own statement, no surrounding transaction), in this order: third to position 0, the higher-id task to 2, the lower-id task to 2. Every write changes the position, so each is a real UPDATE. Act: taskRepository.findAllByColumnId mapped to ids, with no test-side sort (orderedTaskIds re-sorts in Java and would hide the mutation). Assert containsExactly(third id, lowerId, higherId). One plain-text comment explains the write order: each UPDATE writes a new row version after the older ones, so heap order is third, higher, lower, which is what a missing ORDER BY or a missing id tiebreak would return; one more says why compareTo is a valid oracle (lowercase base36, same order in Java and PostgreSQL). Mutations, one run each: M-B2a drops the whole order-by clause from the findAllByColumnId @Query; M-B2b drops only the id tiebreak (keeps position ascending). If either does not FAIL, try third written last instead of first and re-prove both; if still not, report "could not prove" with the rows, do not weaken.

(c) TaskMoveTest, MoveToColumn: three same-column cases mirroring shouldAppendAtEnd_whenTargetPositionIsOmitted and shouldClampToEnd_whenTargetPositionExceedsDestinationSize, each on a fresh column with tasks A, B, C and asserting both orderedTaskIds (B, C, A) and positionsOf (0, 1, 2): move A to targetPosition 2 (move down; name like shouldMoveFirstTaskToEnd_andShiftOthersUp_whenTargetIsLastPositionInSameColumn); move A with targetPosition omitted (same-column append); move A with targetPosition 999 (same-column clamp). Mutation M-B3: delete the `else if (effectivePosition > oldPosition)` branch in TaskService.moveToColumn; expect the move-down case to FAIL. Mutation M-B4: make maxValidPosition equal destinationSiblingCount for both column cases (drop the same-column minus one, ~line 257); expect append and clamp to FAIL. Under M-B4 the order stays B, C, A and only the positions become 0, 1, 3, so the positions assertion is required, not optional.

(d) UserServiceTest, UpdateTheme: add shouldPersistLight_whenWritingLightAfterDark: updateTheme DARK, assert the stored column is "DARK" with the same jdbcTemplate query as the sibling, updateTheme LIGHT, assert "LIGHT". Mutation M-B5a: delete `user.setTheme(dto.getTheme());` (fails at the DARK step). Mutation M-B5b: replace it with a hard-coded `user.setTheme(ThemePreference.DARK);`; the new test must FAIL, and the same run's row for the existing DARK-only sibling should read PASS, which is the gap the review named (record it, do not pass --others-pass).

(e) BoardCreationE2ETest, DuplicateName (~line 227) and RenameBoard's conflict case (~line 335): add `Assertions.assertThat(response.body().jsonPath().getString("code"))` isEqualTo "DUPLICATE_RESOURCE", described with `.as(...)` carrying `response.statusCode()`, placed before the existing status assertion so one mutated run's failure message shows both status and code. PASS run first (the class is realSocket, outside fastTest, so the harness run is its only proof). Mutation M-B6: delete the existsByUserIdAndName guard in UserService.addBoardByUserId. Mutation M-B7: delete the duplicate-name guard block in BoardService.updateById (the `if (!isNoOpRename && ...)` throw; keep setName). Record status and code from each failure message. Predicted, to be confirmed or refuted: M-B6 gives 409 DATA_INTEGRITY_VIOLATION (the unique constraint fires at commit and JpaTransactionManager translates it, as BoardService.save's Decisions comment states), so today's status-only test passes under it; M-B7 likely gives 500, because the explicit entityManager.flush() raises the constraint violation outside Spring's exception translation, so the existing status assertion already catches that one. Decision rule: keep the code assertion for a case only if its measured mutated code differs from DUPLICATE_RESOURCE; if it is equal, remove that assertion and say so in the SUMMARY.

(f) BoardServiceTest: rename the two delete methods whose names promise a boolean that BoardService.deleteById (void) never returns: ~line 104 to testDeleteById_shouldThrowNotFound_whenBoardWasAlreadyDeleted, ~line 122 to testDeleteById_shouldThrowNotFoundOnReload_whenBoardFoundAndDeleted. ColumnServiceTest, shouldDeleteAllTasks_whenColumnHasTasks Javadoc (~line 395): drop the promise of a before/after count; say the clean return is the assertion (fk_tasks_column has no ON DELETE CASCADE) and the before-count only rules out a vacuous pass on an empty column. Add no after-count: once the column row is gone the FK makes zero tasks certain, so it would assert nothing the clean return does not. No mutation runs for renames or comments.

(g) docs/learning/04-service-layer-and-access-control.md ~lines 609-615: swap the replaced test's name for the new tie test's name and add the three same-column names.

(h) spotlessApply, verify-comments, final PASS run labelled t2-pass-final over all six classes, then commit as in Task 1 (paths of this task only, 600000 ms timeout, no --no-verify).
  </action>
  <verify>
    <automated>Q=.planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca; test -z "$(git status --porcelain -- src/main)" && python3 -I $Q/261026-ovp-mutation-check.py --label t2-verify --log $Q/261026-ovp-mutation-log-task2.tsv --tests '*BoardControllerTest' --tests '*TaskMoveTest' --tests '*BoardCreationE2ETest' --tests '*UserServiceTest' --tests '*BoardServiceTest' --tests '*ColumnServiceTest' && test "$(rg -c '\tFAIL\tFAIL\t' $Q/261026-ovp-mutation-log-task2.tsv)" -ge 9 && ! rg -q 'shouldReturnSameOrderTwice|shouldReturnFalse_whenBoardNotFound|shouldReturnTrue_whenBoardFoundAndDeleted' src/test/java docs/learning docs/ARCHITECTURE.md</automated>
  </verify>
  <done>The task-2 log has FAIL/FAIL rows for M-B1 (delete read-back), M-B2a and M-B2b (tie order), M-B3 (move down), M-B4 (append and clamp), M-B5a and M-B5b (theme), and for M-B6 and M-B7 wherever the decision rule kept the code assertion, plus PASS rows for every testcase of the six classes on reverted code. The SUMMARY states the measured status and code under M-B6 and M-B7. The stale-name gate is 0 hits, down from 4 at HEAD (TaskMoveTest 1, docs/learning/04 1, BoardServiceTest 2). The commit exists and src/main is clean.</done>
</task>

<task type="auto">
  <name>Task 3: literal consumer detail, four untested event types, V7-V9 history, duplicate and pin deletions</name>
  <files>src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogConsumerE2ETest.java, src/test/java/com/vrudenko/kanban_board/config/FlywaySchemaProvenanceTest.java, src/test/java/com/vrudenko/kanban_board/config/ComposedConstraintPropertyCustomizerTest.java, src/test/java/com/vrudenko/kanban_board/e2e/observability/ObservedAspectWiringTest.java, docs/ARCHITECTURE.md, docs/learning/01-domain-model-and-schema.md, docs/learning/07-events-and-activity-feed.md, docs/learning/08-testing-strategy.md, .planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-mutation-log-task3.tsv</files>
  <read_first>src/main/java/com/vrudenko/kanban_board/activitylog/ActivityLogConsumer.java, src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogConsumerE2ETest.java, src/test/java/com/vrudenko/kanban_board/config/FlywaySchemaProvenanceTest.java, src/test/java/com/vrudenko/kanban_board/config/ComposedConstraintPropertyCustomizerTest.java (lines 445-570), src/test/java/com/vrudenko/kanban_board/e2e/observability/ObservedAspectWiringTest.java (lines 1-160), docs/ARCHITECTURE.md (lines 196-212)</read_first>
  <precondition>Task 2 is committed and git status --porcelain -- src/main prints nothing.</precondition>
  <action>
Follow the mutation proof protocol; log to `261026-ovp-mutation-log-task3.tsv`; label each run t3-...

(a) ActivityLogConsumerE2ETest, shouldProduceIdenticalDetail_whenTwoTaskMovedEventsShareSameIds (~line 265): replace the row-to-row comparison with both rows' detail equal to the literal built the way the single TaskMoved literal test builds it (taskId, then sourceColumnId, then targetColumnId). Keep the name. Mutation M-A1: in ActivityLogConsumer, build a java.util.HashMap in the TaskMoved arm and change the ActionAndDetailIds record component to Map of String to String so it compiles. HashMap iterates these three keys as targetColumnId, sourceColumnId, taskId (String.hashCode bucket indexes 0, 13, 14 in a 16-slot table), so the detail differs deterministically. Do not use Map.of: its order changes per JVM, so it would fail only by chance.

(b) Same class, four new cases written like the existing ones (UUID eventId, randomId() ids, Instant.now(), sendAndAwaitAck, Awaitility 30 s, findByEventId, literal detail): BoardDeletedEvent, action BOARD_DELETED, detail "{}"; ColumnUpdatedEvent, COLUMN_UPDATED, columnId detail; SubtaskUpdatedEvent with isCompleted true, SUBTASK_UPDATED, detail taskId then subtaskId then isCompleted with the quoted string "true" (pins the String.valueOf stringification, not a JSON boolean); SubtaskDeletedEvent, SUBTASK_DELETED, detail taskId then subtaskId. Batched mutation M-A2, one run: BoardDeleted arm yields BOARD_UPDATED; ColumnUpdated arm yields COLUMN_CREATED; SubtaskUpdated arm loses its isCompleted put; SubtaskDeleted arm has its two put lines swapped. Each new case publishes one event type and each mutation sits in a different switch arm, so a failure is attributable to its own arm; run with `--expect-fail` on the four new methods and `--others-pass`, so every pre-existing case must still PASS. If any expectation misses, undo and prove the four as single-mutation runs.

(c) ComposedConstraintPropertyCustomizerTest: delete the single-case SaveSubtaskRequestDTO title minLength test directly after shouldPublishMostRestrictiveLength_whenComposedOrDirectAnnotationsPresent (~lines 549-565). Its row (SaveSubtaskRequestDTO, title, 2) stays in that table test.

(d) FlywaySchemaProvenanceTest: delete the nested class asserting the two Spring Session tables (~lines 215-231); SchemaShape's exact table set already names both. In the FlywayHistory success-count test, extend the IN list to '1' through '9', expect 9, and rename it to shouldRecordSuccessfulMigrationsV1ThroughV9_whenContextStarts (the IN-list choice and its cost are under "Approaches considered"). Mutation M-A3: `mv src/main/resources/db/migration/V9__add_columns_color.sql src/main/resources/db/migration/R__add_columns_color.sql`. The repeatable migration still adds columns.color, so Hibernate validate and the context start, but flyway_schema_history has no version 9. Expect the history test to FAIL by assertion; a context-start failure is INVALID and means the mutation is wrong. Revert with the reverse mv: git checkout -- src/main restores V9 but leaves the untracked R__ file behind. Confirm git status on src/main is empty.

(e) ObservedAspectWiringTest: delete the nested advisor-order class with its indexOf helper. In the class Javadoc, the sentence ending with the order cases pinning the commit placement now names the commit-coverage cases. spotlessApply drops the imports this frees (Predicate, Advisor, AbstractAspectJAdvice, Advised, BeanFactoryTransactionAttributeSourceAdvisor). No mutation: it is a deletion.

(f) Docs, originals only (not docs/wiki/): docs/ARCHITECTURE.md ~line 205, drop the advisor-order test reference and its "(advisor order) and", leaving the CommitCoverageTest case as the pin. docs/learning/01-domain-model-and-schema.md ~lines 955-962: three nested groups, the FlywayHistory row says versions 1 to 9, delete the session-coexistence row, and the SchemaShape row says the nine tables include both Spring Session tables. docs/learning/07-events-and-activity-feed.md ~lines 549-558: recount the event types the class covers (rg the event constructors in the test) and state the number, and list the four new names. docs/learning/08-testing-strategy.md ~lines 257-259: the real test count (rg -c '@Test' on the file) and three groups; ~line 386: point at FlywaySchemaProvenanceTest.SchemaShape, whose exact table set includes both session tables.

(g) spotlessApply, verify-comments, final PASS run labelled t3-pass-final over the four classes, then commit as in Task 1.
  </action>
  <verify>
    <automated>Q=.planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca; test -z "$(git status --porcelain -- src/main)" && python3 -I $Q/261026-ovp-mutation-check.py --label t3-verify --log $Q/261026-ovp-mutation-log-task3.tsv --tests '*ActivityLogConsumerE2ETest' --tests '*FlywaySchemaProvenanceTest' --tests '*ComposedConstraintPropertyCustomizerTest' --tests '*ObservedAspectWiringTest' && test "$(rg -c '\tFAIL\tFAIL\t' $Q/261026-ovp-mutation-log-task3.tsv)" -ge 6 && ! rg -q 'SpringSessionCoexistence|AspectOrderTest|shouldRaiseMinLengthAboveTheDirectNotBlank|shouldRecordSixSuccessfulMigrations' src/test/java docs/learning docs/ARCHITECTURE.md</automated>
  </verify>
  <done>The task-3 log has FAIL/FAIL rows for M-A1 (two-event literal), the four M-A2 methods (with every other consumer case PASS in that run) and M-A3 (history V1-V9), plus PASS rows for every testcase of the four classes on reverted code. The stale-name gate is 0 hits, down from 8 at HEAD (FlywaySchemaProvenanceTest 2, ComposedConstraintPropertyCustomizerTest 1, ObservedAspectWiringTest 1, docs/ARCHITECTURE.md 1, docs/learning/01 1, docs/learning/08 2). The commit exists and src/main is clean.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| working tree to commit | hand-applied src/main mutations exist in the tree during proof runs and must never reach a commit |
| test suite to production regressions | deleted tests must not remove the only guard on a security property |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-ovp-01 | Tampering | src/main during mutation runs | high | mitigate | The harness refuses a pass run on dirty src/main; every commit uses a pathspec limited to src/test, docs and the quick dir; final gate `git status --porcelain -- src/main` empty and `git diff --stat START -- src/main` empty; the V9 rename is reverted with mv because checkout leaves the untracked R__ file |
| T-ovp-02 | Elevation of privilege | deleted permitAll reachability class | medium | accept | AuthenticationTest's signin (200 plus cookie) and signup (201) cases send no session; a route moved behind auth turns them into 401 |
| T-ovp-03 | Information disclosure | signup exact field-set removal (AuthenticationTest ~436) | medium | accept | The signin exact-set case still fails on any new UserResponseDTO field, and signup returns the same DTO; residual risk only if signup changes DTO, recorded as open question 4 |
| T-ovp-04 | Spoofing | AuthorizationGatingTest ownership read-back | low | mitigate | Read-back stays within two `.with(user())` calls per principal, so a session-ceiling 401 cannot be mistaken for an ownership result |
| T-ovp-SC | Tampering | npm/pip/cargo installs | low | accept | This plan installs no package; the harness is stdlib-only Python |
</threat_model>

<verification>
After Task 3's commit, from the repo root:

1. `test -z "$(git status --porcelain -- src/main)" && test -z "$(git diff --stat START -- src/main)"` where START is the SHA recorded before Task 1.
2. `! rg -q 'shouldReturnSameOrderTwice|PermitAllExclusions|SpringSessionCoexistence|AspectOrderTest|shouldRaiseMinLengthAboveTheDirectNotBlank|shouldReturnOk_whenHealthzCheckedWithoutSession|shouldReturnFalse_whenBoardNotFound|shouldReturnTrue_whenBoardFoundAndDeleted|shouldRecordSixSuccessfulMigrations|MULTIPLE_CHOICES' src/test/java docs/ARCHITECTURE.md docs/learning` (16 hits in 12 files at HEAD 8d995db).
3. `./gradlew spotlessApply < /dev/null`, then `git status --porcelain -- src/test docs` prints nothing (formatting already committed).
4. `./gradlew fastTest < /dev/null` once, exit 0; record the test count Gradle reports. The kafka and realSocket classes are outside fastTest, so their proof is the harness rows from Tasks 2 and 3.
5. `python3 scripts/verify-comments.py check < /dev/null` exits 0.
6. `docker ps -a --filter label=org.testcontainers` after the last run: remove only containers this session created (compare CreatedAt with START's time) and report any others (memory note: no Ryuk reaper here).
</verification>

<success_criteria>
- Five M cases: each has a FAIL/FAIL row under its named mutation and a PASS row on real code.
- New tests: each has a FAIL/FAIL row under a named mutation and a PASS row on real code, or an explicit "could not prove" with its rows; none passes both ways.
- Duplicate-name cases: the code assertion exists exactly where the measured mutated code differs, and both measurements are in the SUMMARY.
- Deletions and renames done, the stale-name gate is 0, and src/main is identical to START.
- fastTest green once on the final tree; three commits, none with --no-verify, nothing pushed or amended.
</success_criteria>

## Open questions to record in the SUMMARY (report, do not fix)

1. The /full nested read orders columns by id (BoardEntity.java:46 `@OrderBy("id")`), though columns carry a
   position and have a reorder endpoint; BoardFullReadTest:479 asserts the nested task ids `isSorted()`,
   pinning id order rather than position order. Whether /full should order by position is a behaviour
   decision.
2. HistoricalSchemaRehearsalE2ETest:243-255: after the settle window, `untilAsserted` passes on the first
   500 ms poll that returns no dead-letter record, so a dead letter arriving after that poll is never seen;
   the check can pass on an empty poll.
3. After (a) in Task 3, shouldProduceIdenticalDetail_whenTwoTaskMovedEventsShareSameIds asserts the same
   literal as shouldPersistTaskMoved_withTaskIdSourceColumnIdTargetColumnIdDetail; the remaining difference
   is two events against one. A duplicate candidate for the next review, kept because the finding asked
   for the literal.
4. With the signup exact field set gone (as asked), a hash leak in the signup response is caught only
   through the signin exact-set case, which holds while both endpoints return UserResponseDTO.
5. docs/wiki/learning/ copies still name the deleted and renamed tests; .claude/CLAUDE.md says to edit the
   originals, not the wiki copies.
6. Out of scope, seen while planning: five `.claude/worktrees/*` directories hold stale copies of these
   test files and show up in repo-wide searches, and seven untracked 261024-* evidence files sit in
   `.planning/quick/`. Neither is touched; name both in the report.

<output>
Create `.planning/quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/261026-ovp-SUMMARY.md` with:
one row per proven test (class, method, mutation id and its one-line description, FAIL run label, PASS
run label, the failure message excerpt from the log); the guard self-check exit code; the measured
status and code under M-B6 and M-B7 and which code assertions were kept; the measured env status; any
"could not prove" items; the six open questions above; and the fastTest count from verification step 4.
</output>
