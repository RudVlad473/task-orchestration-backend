---
phase: 14-production-ownership-backend-observability-alerting-and-reco
plan: 04
subsystem: observability
tags: [micrometer, observed, ecs-logging, security-events, counters, spring-aop]

requires:
  - phase: 14-01
    provides: management port 9081, Prometheus registry, /api/healthz
provides:
  - counters kanban.auth.failures{reason}, kanban.ownership.denied, kanban.optimistic.lock.conflicts, kanban.tasks.created, kanban.tasks.moved{move}, all registered at startup
  - WARN security-event JSON lines (security_event, reason, http_method, path, user_id on access_denied) with no credentials
  - ECS JSON console logs with a UTC timestamp
  - service timers kanban.task.create, kanban.task.move, kanban.board.full.read through Observed, ordered outside the transaction
  - 405, 415 and 404 arms so scanner probes never answer 5xx
affects: [14-05 scrape contract and Kafka counters, 14-06 alert rules, 14-07 tracing, Phase 14.1 dashboards]

actuals:
  tokens: 19300
  tasks: 2
  commits: 2

tech-stack:
  added: [org.springframework.boot:spring-boot-starter-aop 3.5.16 (BOM-managed; aspectjweaver was already on the runtime classpath through spring-data-jpa)]
  patterns: [Observed(name = MetricNames.X) on service methods, aspect ordered outside the transaction advisor, eager counter registration, SLF4J key-value security events]

key-files:
  created:
    - src/main/java/com/vrudenko/kanban_board/constant/MetricNames.java
    - src/main/java/com/vrudenko/kanban_board/constant/SecurityEventLog.java
    - src/main/java/com/vrudenko/kanban_board/config/ObservedAspectConfig.java
    - src/test/java/com/vrudenko/kanban_board/e2e/observability/SecurityEventLogE2ETest.java
    - src/test/java/com/vrudenko/kanban_board/e2e/observability/BusinessMetricsE2ETest.java
    - src/test/java/com/vrudenko/kanban_board/e2e/observability/EagerMetricRegistrationTest.java
    - src/test/java/com/vrudenko/kanban_board/e2e/observability/ObservedAspectWiringTest.java
  modified:
    - src/main/java/com/vrudenko/kanban_board/constant/ErrorCode.java
    - src/main/java/com/vrudenko/kanban_board/handler/GlobalExceptionHandler.java
    - src/main/java/com/vrudenko/kanban_board/security/ProblemDetailAuthenticationEntryPoint.java
    - src/main/java/com/vrudenko/kanban_board/service/TaskService.java
    - src/main/java/com/vrudenko/kanban_board/service/BoardService.java
    - src/main/resources/application.properties
    - build.gradle
    - gradle/verification-metadata.xml
    - docs/ARCHITECTURE.md

key-decisions:
  - "Service-level timers and spans use Observed(name = MetricNames.X), superseding the plan's rejection of approach B for service methods only (user decision, evidence below). Controllers still get no Timed."
  - "Boot's ObservedAspect ties with the transaction advisor and lands inside it; ObservedAspectConfig replaces the bean with one ordered LOWEST_PRECEDENCE - 1."
  - "kanban.task.create does not cover its commit: ColumnService.addTaskByColumnId owns that transaction and TaskService.save joins it."

requirements-completed: ["2026-08-20-no-security-event-logging-on-auth-and-access-control", "2026-08-20-no-remote-log-shipping-structured-logging-or-alerting"]

commits: 2
plan_head_before: 2531727c4299fcf9ad43438baad12c4d5dfd1925
plan_head_after: f5cfc96a946dfd04f17409e9df7675d54b9e29e7

duration: 80min
completed: 2026-10-16
status: complete
---

# Phase 14 Plan 04: Request-path counters, security-event logs and service timers Summary

**Auth failures, ownership denials, optimistic-lock conflicts, task creates and moves are counted once per request from startup; failed signins and ownership denials write PII-free ECS JSON WARN lines; three hot service methods are timed through Observed outside the transaction; scanner probes answer 405, 415 or 404 instead of 500.**

Nothing was pushed or deployed. The changes reach production with the next push that rebuilds the app image (plan 14-06's rollout or a later push).

## Tasks

| Task | Name | Commit | Status |
| ---- | ---- | ------ | ------ |
| 1 | Auth, ownership and conflict counters, JSON security events, ECS logs, no 5xx for scanner probes | c65629b | done, full pre-commit hook passed (BUILD SUCCESSFUL in 4m 50s) |
| 2 | Task counters, service Observations, hot endpoints timed | f5cfc96 | done, full pre-commit hook passed (BUILD SUCCESSFUL in 5m 57s) |

## Verification run

| Command | Result |
| ------- | ------ |
| `./gradlew test --tests '*SecurityEventLogE2ETest' --tests '*BusinessMetricsE2ETest' --tests '*EagerMetricRegistrationTest' --tests '*ArchTest' --tests '*ProblemDetailOpenApiCustomizerTest' --tests '*GlobalExceptionHandlerTest' -x jacocoTestCoverageVerification` (Task 1, after the fix) | BUILD SUCCESSFUL, 46 tests, 0 failures |
| `./gradlew test --tests '*ObservedAspectWiringTest' --tests '*BusinessMetricsE2ETest' --tests '*EagerMetricRegistrationTest' --tests '*SecurityEventLogE2ETest' --tests '*ArchTest' -x jacocoTestCoverageVerification` (Task 2, final) | BUILD SUCCESSFUL, 46 tests, 0 failures |
| `./gradlew test` (whole suite, kafka-tagged included, JaCoCo verification ran) on the tree committed as f5cfc96 | BUILD SUCCESSFUL in 6m 33s, 587 tests, 0 failures, 0 skipped (counted from the JUnit XML) |
| `python3 scripts/verify-comments.py check` | OK: 309 files, 1214 comment blocks (run before the last small build.gradle comment edit; the commit hook re-ran it and passed) |
| `./gradlew spotlessCheck` | passed in the hook for both commits |
| `./gradlew --write-verification-metadata sha256 --refresh-dependencies spotlessCheck test` | the documented regeneration command (build.gradle comment), used for the new starter; added spring-boot-starter-aop 3.5.16 jar, module and pom (11 lines), and I restored the order of the two `<trust>` entries Gradle swapped, as 14-01 did |

Task 1 acceptance greps: `logging.structured.format.console=ecs` prints one line (application.properties:49); `grep -c meterRegistry GlobalExceptionHandler.java` printed 7 (at least 4 required). Task 2 acceptance: the endpoint-template grep in BusinessMetricsE2ETest printed 8 (at least 4); the `Observation.createNotStarted` counts (2 and 1) are intentionally NOT met, see Deviation 1.

## TDD evidence (red direction, then green)

### Task 1 (against the old production code, MetricNames added first so the tests compile)

15 new cases, 15 failed, each on the planned assertion (read from the JUnit XML, not the build summary):

- counter-delta cases: `expected: 1.0 but was: 0.0` (counter absent reads as zero), 4 cases
- eager-registration cases: `Expecting actual not to be null`, 3 cases
- log cases: `Expected size: 1 but was: 0 in: []` (no JSON lines at all) x5 and `Expecting actual not to be empty` for the UTC timestamp case
- scanner probes: `GET /api/signin` answered 500 and authenticated `GET /no-such-path` answered 500, so plan step 6 applied

The 13 existing ArchUnit tests in the same run passed. Semantic assessment: every failure was on the planned assertion, none on setup or compilation.

Failing direction per counter (mutation, restored from a byte copy afterwards, then `diff`-checked by a clean `git diff`):

| Mutation | Run | Result |
| -------- | --- | ------ |
| the four `.increment()` calls in GlobalExceptionHandler and the entry point replaced by `.count()` | BusinessMetricsE2ETest | exactly the 4 counter cases failed (bad_credentials, unauthenticated, ownership denied, optimistic lock); 19 tests, 4 failed |
| both `@PostConstruct` registration methods disabled | EagerMetricRegistrationTest + BusinessMetricsE2ETest | exactly the 3 eager cases failed; every Business case passed (lazy registration still counts, which is why the fresh-context class exists); 22 tests, 3 failed |

The log lines' failing direction is the RED run itself (no security_event line existed before the change). The 405/404/415 arms' failing direction is also the RED run (500 before the arms).

### Task 2, first implementation (explicit Observation wrapper, later replaced)

RED against the unchanged TaskService/BoardService: 6 failed (`TaskCreatedTest`, 2 x `TaskMovedTest`, `BoardFullReadTest`, 2 eager cases for tasks created and moved). Four per-uri timer cases and the stale-move case passed before any change: the per-uri cases are regression pins (Boot already records `http.server.requests` under the templated uri for `GET /boards/{boardId}/full`, `POST /boards/{boardId}/columns/{columnId}`, `PATCH /tasks/{taskId}/move`, `POST /signin`); the stale-move case is a negative guard.

Mutations on that implementation: increments removed -> the 3 created/moved cases failed (29 tests, 3 failed); the three wrappers removed -> the 4 timer cases failed, the per-uri cases stayed green (29 tests, 4 failed); an extra cross_column increment before the version check -> `shouldRaiseNeitherMoveCounter_whenMoveVersionIsStale` and both success cases failed (29 tests, 3 failed).

That code was then replaced (next section). The counter mutations still describe the committed code, whose counter lines did not change. The wrapper mutation no longer applies.

### Task 2, Observed (after the user's mid-plan decision)

RED = the wrapper-free service code with no annotation and no aspect: `BusinessMetricsE2ETest` 4 failed (`TaskCreatedTest`, 2 x `TaskMovedTest`, `BoardFullReadTest`: `expected: 1L but was: 0L`), `ObservedAspectWiringTest` 3 failed (advisor index -1; commit case `0L` below 300; probe `observed()` through the proxy `expected: 1L but was: 0L`), `EagerMetricRegistrationTest` unaffected.

GREEN step 1 (annotation, starter and property added, Boot's default aspect bean): the full suite run showed 587 tests, 2 failed, both in `ObservedAspectWiringTest`:

- `AspectOrderTest.shouldPlaceObservationAdvisorBeforeTransactionAdvisor_onTaskService`: observation advisor index 2, transaction advisor index 1 (`Expecting actual: 2 to be less than: 1`)
- `CommitCoverageTest.shouldIncludeCommitInMoveObservation_whenMoveOwnsItsTransaction`: observed time grew by 23 ms for a move whose commit was held 300 ms

So the default order puts the observation inside the transaction, and the timer does not include the commit.

GREEN step 2 (`ObservedAspectConfig`): first attempt returned `ObservedAspect` from the `@Bean` method and changed nothing (9 ms), because Spring reads the aspect's order from the declared bean type before instantiation and the return type hid `Ordered`. After declaring the subclass as the return type: 46 tests, 0 failures, then 587/0 on the whole suite.

The other limits: `SelfInvocationTest.shouldNotObserve_whenAnnotatedMethodIsCalledOnThisFromInsideItsClass` passes, and passes vacuously in the RED state, so its contrast is the paired `shouldObserve_whenAnnotatedMethodIsCalledThroughTheProxy`, which failed in RED and passes in GREEN. `CommitCoverageTest.shouldNotCoverCommit_whenCallerOwnsTheTransactionOfTaskCreate` also passed in RED (no timer, 0 ms) and is a documentation pin of a limit, not a goal.

## Decision: Observed supersedes approach B for service methods (for the PLAN matrix)

Plan 14-04 rejected `@Observed` because it needs `spring-boot-starter-aop`. The user reversed that for service methods only. Evidence:

- It works: the three timers are recorded through the real wiring (`BusinessMetricsE2ETest.TaskCreatedTest`, `TaskMovedTest`, `BoardFullReadTest`, all green with the annotation and no wrapper code in the services).
- The dependency cost is one BOM-managed starter whose only jar (aspectjweaver) was already on the runtime classpath through spring-data-jpa's spring-aspects, plus 11 lines of verification metadata.
- Cost the plan did not foresee: the default aspect order is wrong for a service that owns its transaction (measured above), which needed `ObservedAspectConfig` (43 lines) and three tests.
- `@Timed` on controllers stays rejected: `http.server.requests` already times every endpoint under its templated uri.

Documented in `docs/ARCHITECTURE.md`, section "Observability: service timers and request-path counters" (committed in f5cfc96): the rule, the reason, a table of the four alternatives, and the limits, each pointing at the test class and method that pins it. `docs/` has no ADR folder, so I used the architecture document. I added nothing to `.claude/CLAUDE.md`. If you want a one-line pointer there, say so; I would suggest "Service timers: Observed(name = MetricNames.X), see docs/ARCHITECTURE.md". I did not touch docs/wiki, so the wiki lint was not needed.

## Deviations from Plan

**1. [User decision] Observed instead of Observation wrappers.** `Observation.createNotStarted` appears 0 times in TaskService and BoardService, so the Task 2 acceptance greps (2 and 1) are not met by design. Added `spring-boot-starter-aop`, `management.observations.annotations.enabled=true`, `ObservedAspectConfig`, `ObservedAspectWiringTest`, verification-metadata entries. Commit f5cfc96.

**2. [Rule 2 - scanner probes] A third arm, 415.** Besides the planned 405 and 404 arms, `POST /api/signin` with `text/plain` answered 500 (`HttpMediaTypeNotSupportedException` fell to the catch-all). Same class of defect as the two named probes, so I added `UNSUPPORTED_MEDIA_TYPE` and an arm, with a test case. Commit c65629b.

**3. [Rule 1 - flaky test] A random "wrong" password could equal the real one.** `generateValidPassword()` draws from a small word list; one run got `401 expected, was 200`. The wrong password is now `getOwningUserPassword() + "x"` in the registered-email cases. Commit c65629b.

**4. [Plan wording] Files added beyond the plan's list:** `SecurityEventLog.java` (one place for the log field names shared by two producers), `EagerMetricRegistrationTest.java` (a shared test context cannot prove eager registration because another class may already have incremented; this class gets its own context and extends AbstractPostgresContainerTest so fixtures create no tasks), `ObservedAspectConfig.java`, `ObservedAspectWiringTest.java`, and the build.gradle and verification-metadata edits.

**5. [Plan wording] Counter registration sits in the class that increments.** The plan put both auth-failure reasons in GlobalExceptionHandler's `@PostConstruct`; the entry point registers its own `unauthenticated` series instead. Same result, no cross-wiring.

**6. [Research correction] ECS nests `log.level`.** The research said `log.level` is a flat key; Boot 3.5.16 writes `"log":{"level":"WARN",...}`. The test reads `log.level` as a nested path. The security fields (`security_event`, `reason`, `http_method`, `path`, `user_id`) are top-level, as planned.

**7. [Plan wording] "Four hot service methods" is three.** Three methods carry `@Observed`; `POST /api/signin` has no service observation by the plan's own decision (BCrypt cost is isolated by its per-uri timer). All four endpoints are pinned by `BusinessMetricsE2ETest.PerEndpointTimerTest`.

**8. [Tooling] Serena MCP was not available.** The coordinator asked for Serena edits; `ToolSearch` returned no Serena tools (queries `select:mcp__serena__...` and `serena`), so `activate_project` and the symbol edits could not run. All Java edits used Read and Edit (the Serena-first hook asked for a retry on the first edit of each file, which I did and note here). Edits were confirmed in the worktree by `git status` and the commits, not by Serena.

**9. [Process lapses] I ran one Bash call with a `cd` inside it and two throwaway Python scripts through heredocs (one to split a snippet, one to read JUnit XML), against the working preferences. No effect on the tree.

**Total deviations:** 9 (1 user decision, 2 added fixes, 6 plan-shape or tooling notes). **Impact:** none on the committed behaviour beyond the added 415 arm and the Observed change.

## Findings and limits (read these before relying on the series)

- `kanban.task.create` does not cover its commit. `ColumnService.addTaskByColumnId` is `@Transactional` and `TaskService.save` joins it, so the commit runs after `save` returns. If the commit should be in that timer, move the annotation to `ColumnService.addTaskByColumnId` (same name constant, one-line change plus its test). Not done: the user asked for the annotation on the service methods named in the plan.
- A counter increments before the commit, so a commit that rolls back would leave `kanban.tasks.created` and `kanban.tasks.moved` one too high. Not measured; judged rare.
- The OpenAPI customizer (`ProblemDetailOpenApiCustomizer`) lists the `code` enum from `ErrorCode.values()`, so the three new codes appear in the published schema, but its per-status descriptions still name only the old codes and declare no 405 or 415 response. Not changed (outside the plan's files); `ProblemDetailOpenApiCustomizerTest` passes.
- Metrics in the tests run on Boot's test registry, not the Prometheus one, so the Prometheus names (`kanban_tasks_created_total`, `kanban_task_move_seconds_*`) are not asserted here. Plan 14-05's scrape test owns that.

## Not verified

- No tracing span for the three observations: tracing arrives with 14-07; I verified timers only.
- The behaviour under real Prometheus scraping, the live app, and the k3s rollout.
- The property switched off on the real services: I did not run "property removed, annotation present". The probe in the RED run (aspect absent) and the advisor-order test (`index -1` without the aspect) cover it indirectly.
- The wrapper mutation evidence applies to the replaced implementation only (see above).
- `verify-comments.py` was not re-run after the very last build.gradle comment edit outside the commit hook (the hook did run it and passed).

## Threat Flags

None beyond the plan's threat model. T-14-17 (no PII in the log line): `SecurityEventLogE2ETest.shouldNotLogSubmittedCredentials_whenSigninFails` asserts the registered email and the wrong password are absent from the captured output after proving one event exists. T-14-18 (cardinality): tags come only from the fixed sets in `MetricNames`; the per-uri timers use Boot's templated uri. T-14-20: the three arms and `ScannerProbeTest`.

## Known Stubs

None.

## Self-Check: PASSED

- All 16 key-files listed above exist in the worktree (checked with the commits' file lists: `git show --stat` for c65629b and f5cfc96).
- Both commits are ancestors of HEAD, and `git rev-list --count 2531727c..HEAD` printed 2 before this SUMMARY commit.
- STATE.md and ROADMAP.md were not touched.
