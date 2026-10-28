---
phase: quick-261028-m6k
plan: 01
quick_id: 261028-m6k
subsystem: security
tags: [spring-security, spring-session, request-cache, session-hygiene, testing]

requires:
  - phase: 02-model-completion
    provides: "02-04-401-measure.txt, the measured cookie-less 401 behaviour (rows B, D, E)"
provides:
  - "securityFilterChain saves no refused request, so an anonymous 401 creates no spring_session row and sets no cookie"
  - "NoSessionSweep.shouldCreateNoSessionAndSetNoCookie_whenNoSessionIsPresent: 8 parameterized cases pinning that"
affects: [spring_session table growth, docs/architecture views describing anonymous 401 behaviour]

actuals:
  tokens: 3900   # chars/4 over the committed diff (15517 bytes total, 8767 bytes under src/); approximate, estimateTokens was not run
  tasks: 3
  commits: 1
plan_head_before: 178aff3189b0ba8d23684223601f46d08ca83f62
plan_head_after: 17a94cb92ad534eba5ac2d4d60c0a57c6b5f5f39

tech-stack:
  added: []
  patterns:
    - "NullRequestCache as the chain's shared RequestCache when nothing replays a saved request"
    - "Session-row assertions by PRIMARY_ID set difference, cookie NAMES only, one record assertion"

key-files:
  created:
    - .planning/workstreams/architecture-views/quick/261028-m6k-stop-anonymous-401-requests-from-creatin/261028-m6k-red.txt
    - .planning/workstreams/architecture-views/quick/261028-m6k-stop-anonymous-401-requests-from-creatin/261028-m6k-green.txt
  modified:
    - src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java
    - src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java

key-decisions:
  - "Approach A from the plan: http.requestCache(cache -> cache.requestCache(new NullRequestCache())) on securityFilterChain only"
  - "Evidence .txt files committed with the code, as the plan's Task 3 lists them among the four staged files"

requirements-completed: [quick-261028-m6k]

duration: ~1h (wall clock dominated by four Gradle runs of 3-6 minutes each plus the 4-minute commit hook)
completed: 2026-10-28
status: complete
---

# Quick 261028-m6k: Stop anonymous 401 requests from creating a session Summary

**A refused anonymous request now writes no spring_session row and sends no JSESSIONID: securityFilterChain uses a NullRequestCache, pinned by an 8-case test shown red on the unfixed code (6 of 8) and green on the fix.**

## Performance

- **Tasks:** 3 of 3
- **Files modified:** 2 source files (plus 2 evidence files)
- **Commit:** `17a94cb` (one commit, full pre-commit hook, no `--no-verify`)

## Direction checked

- **RED against the unfixed code:** Task 1 ran the new test with `git diff --stat -- src/main` empty. Result: `tests="30" failures="6"`.
- **GREEN against the fix:** Task 2 ran the same command with the one-line fix present. Result: `tests="30" failures="0"`, exit 0.

## Red and green per case

Each case asserts `RefusalSideEffects[status=401, newSessionRows=0, setCookieNames=[]]`.

| Case | Red (unfixed) | Green (fix) |
|---|---|---|
| GET /boards, Accept application/json | PASS | PASS |
| GET /boards, Accept wildcard | FAIL: 401, 1 new row, [JSESSIONID] | PASS |
| GET /boards, browser-navigation Accept | FAIL: 401, 1 new row, [JSESSIONID] | PASS |
| GET /boards, no Accept header | FAIL: 401, 1 new row, [JSESSIONID] | PASS |
| POST /boards, Accept application/json | PASS | PASS |
| POST /boards, Accept wildcard | FAIL: 401, 1 new row, [JSESSIONID] | PASS |
| POST /boards, browser-navigation Accept | FAIL: 401, 1 new row, [JSESSIONID] | PASS |
| POST /boards, no Accept header | FAIL: 401, 1 new row, [JSESSIONID] | PASS |

The 22 existing `shouldReturnUnauthorized_whenNoSessionIsPresent` cases passed in both runs. The MockMvc tier
reproduced the defect, so the plan's E2E-tier contingency was not needed. Evidence: `261028-m6k-red.txt`,
`261028-m6k-green.txt`.

## Task Commits

All three tasks landed in one commit, as the plan prescribes (the pre-commit `fastTest` refuses a red tree, so
the test and the fix cannot be committed separately):

1. **Task 1 (RED test), Task 2 (fix), Task 3 (non-regression and commit)** - `17a94cb` (fix)

## Committed files (the plan's four)

- `src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java` - import plus one `http.requestCache(...)` call and a 7-line comment (+11)
- `src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java` - `AnonymousRefusalCase` and `RefusalSideEffects` records, `BROWSER_NAVIGATION_ACCEPT`, `anonymousRefusalCases()`, the new test (+98)
- `.planning/workstreams/architecture-views/quick/261028-m6k-stop-anonymous-401-requests-from-creatin/261028-m6k-red.txt`
- `.planning/workstreams/architecture-views/quick/261028-m6k-stop-anonymous-401-requests-from-creatin/261028-m6k-green.txt`

## Non-regression exit codes

| Check | Result |
|---|---|
| `fastTest --tests 'com.vrudenko.kanban_board.security.*'` | exit 0 (130 tests in the results dir, 0 failures) |
| `test` on SessionCookieAttributesE2ETest and ConcurrentSigninCeilingE2ETest, `-x jacocoTestCoverageVerification` | exit 0 (2 tests, 0 failures) |
| `git diff --quiet -- src/main/resources` | exit 0 (cookie Max-Age 600 and spring.session.timeout 180m unmoved) |
| `python3 scripts/verify-comments.py check` | exit 0 (330 files, 1286 comment blocks) |
| `./gradlew spotlessCheck` | exit 0 |
| `./gradlew fastTest` (full), first run | exit 1, 541 tests, 1 failed (the unrelated flake below) |
| `./gradlew fastTest` (full), rerun | exit 0, 541 tests, 0 failures |
| Pre-commit hook (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest) | passed, commit created without `--no-verify` |

## Decisions Made

- Followed approach A of the plan's trade-off matrix unchanged: the shared `RequestCache` set on the catch-all
  chain only. No `@Bean RequestCache`, no `RequestCacheConfigurer::disable`, no session-policy change.
- Cookie-name-only assertion (no values) kept session ids out of failure output and out of the evidence files.

## Deviations from Plan

None - plan executed exactly as written, with two small implementation choices inside the plan's latitude:
`Arrays.asList(...)` (imported) carries the null Accept value, and the source comment is 7 lines (summary,
blank line, 5 reason lines) against the plan's "about six lines".

## Issues Encountered

**Flaky, unrelated test seen in the full `fastTest` run (deferred, not fixed).**
`KafkaEventPublisherTest > OnActivityEventTest > shouldCountOneFailureAndNotThrow_whenThePublishExecutorRejects`
failed once (`KafkaEventPublisherTest.java:74`, the `baseline + 1` publish-failure counter assertion on the async
Kafka publish executor). It passed in isolation and on a full rerun. It touches no security path. Not run against
the unfixed code, so "pre-existing" is inferred, not measured. Candidate for its own ticket: its assertion looks
sensitive to async publishes that earlier tests leave in flight (the run's logs show repeated
"Topic kanban.activity not present in metadata" timeouts).

## Not done

- Only `/boards` (GET and POST) was driven. The fix is chain-wide, because every route on the catch-all chain
  passes the same `ExceptionTranslationFilter`, but no other route was exercised for this behaviour.
- No live `curl` re-measure against a running app was run; the evidence is the MockMvc tier plus the realSocket
  non-regression classes.
- Pre-deploy anonymous `spring_session` rows are left to expire through Spring Session's cleanup (no later than
  10800 s after last access). No migration and no DELETE.
- The Kafka flake above was not investigated further.
- STATE.md and ROADMAP.md were not updated and no docs commit was made, per the orchestrator's instructions.

## Housekeeping

- `./gradlew --stop` run at the end.
- `docker ps -a --filter label=org.testcontainers=true` was empty after the final run. During the runs a Ryuk
  container and a Postgres container were visible and were gone afterwards, so nothing was orphaned.
- Untracked files from other tasks and `milestone.lock` were left alone; nothing was pushed.

## Known Stubs

None.

## Threat Flags

None - no new network endpoint, auth path, file access pattern or schema change. T-m6k-01 and T-m6k-02 are
mitigated as planned; T-m6k-05 (pre-deploy rows) is accepted as planned.

## Self-Check: PASSED

- `SecurityConfiguration.java` contains `NullRequestCache` (import at line 30, call at line 129): FOUND
- `AuthorizationGatingTest.java` contains `shouldCreateNoSessionAndSetNoCookie_whenNoSessionIsPresent`: FOUND
- `261028-m6k-red.txt`, `261028-m6k-green.txt`: FOUND
- Commit `17a94cb` is an ancestor of HEAD: FOUND
