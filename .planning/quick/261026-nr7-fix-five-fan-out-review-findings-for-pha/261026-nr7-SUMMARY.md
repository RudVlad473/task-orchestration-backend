---
quick_id: 261026-nr7
status: complete
description: Fix five verified fan-out review findings for phase 14 (BUG-1, 2, 3, 4, 5)
commits: [b97dae7, 7163fe7, 20bb56b]
pushed: false
---

# Quick task 261026-nr7: summary

Source: fan-out review run 496870, range 29f3a9c..80831b3. Every finding was verified before it was
fixed. Three units ran one after another through the full pre-commit hook.

## What changed

| Bug | Fix | Commit |
|---|---|---|
| BUG-1 a full publish pool dropped events uncounted | `onActivityEvent` submits to the executor itself and catches `TaskRejectedException` with the event in hand, counting and logging `eventId` and `boardId`; `@Async` and `@EnableAsync` removed | b97dae7 |
| BUG-3 a failed send took about 4s | `NegativeLookupCache` (30s) behind an override of the template's `clusterId()` | b97dae7 |
| BUG-5 test profile left the admin client at 2s | 50ms admin timeouts in `application-test.properties`; the one test that needs 2s restores it | b97dae7 |
| BUG-2 task counters ticked before commit | `incrementAfterCommit` registers an afterCommit callback; the moved counter had the same ordering and got the same fix | 7163fe7 |
| BUG-4 invariants gate skipped CronJobs | `iter_pod_templates` yields a CronJob's pod spec; selftest cases added | 20bb56b |

## Evidence

- BUG-3 end to end (orchestrator, throwaway test, deleted): broker down, first send 4150 ms, the next
  four 2002 to 2003 ms. Before the fix: 4139, 4016, 4015 ms.
- BUG-4: the same reproduction that returned zero violations now yields the CronJob template and fires
  I2, I3 and I5. The real backup CronJob passes all three.
- Red then green, per the executors: BUG-1 test (counter 4, expected 5), cache test, BUG-2 create
  (105 to 106) and move (2 to 3), BUG-4 selftest (6 failures before).
- Wider run after unit A: `activitylog.*` and `e2e.*` together, 158 tests, 0 failures, no
  "Could not create admin".

## Left open

- The tightened 4s ceiling in `KafkaClusterIdLookupBoundE2ETest` was run only green.
- 9 of 89 result files still contain `TaskRejectedException`, from fixture-heavy test classes; those drops
  are now counted and logged.
- `BusinessMetricsE2ETest` passed all 18 cases in my own run, but Gradle exited 143 (test JVM killed at
  teardown, unexplained).
- Not touched: `docs/learning/*` still describes `@Async`; I5 splits an image on its last `:` so
  `name:latest@sha256:...` would escape the `:latest` check (the real image is unaffected).
- Not pushed. A push redeploys production through CI and Flux.
