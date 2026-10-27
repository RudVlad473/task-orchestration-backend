---
phase: 14-production-ownership-backend-observability-alerting-and-reco
plan: 07
subsystem: observability
tags: [opentelemetry, micrometer-tracing, kafka, trace-propagation, json-logs]

requires:
  - phase: 14-04
    provides: ECS JSON logging, MetricNames
  - phase: 14-05
    provides: AbstractKafkaContainerTest patterns
provides:
  - tracing bridge and OTLP exporter on the classpath, sampling and export gated by env (TRACING_SAMPLING_PROBABILITY, TRACING_EXPORT_ENABLED=false by default)
  - traceId and spanId on every JSON log line written inside a traced request
  - Spring Security observations switched off, management-port requests pinned as unobserved
  - one trace from the HTTP request through the async publish and Redpanda to the activity consumer
  - bounded Kafka cluster-id lookup, so an observed send cannot block on an unreachable broker
affects: [14-09 Tempo export, 14-08 rollout]

key-files:
  created:
    - src/test/java/com/vrudenko/kanban_board/support/observability/InMemorySpanExporterConfiguration.java
    - src/test/java/com/vrudenko/kanban_board/e2e/observability/TraceLogCorrelationE2ETest.java
    - src/test/java/com/vrudenko/kanban_board/activitylog/ActivityTraceContinuityE2ETest.java
    - src/test/java/com/vrudenko/kanban_board/activitylog/KafkaClusterIdLookupBoundE2ETest.java
  modified:
    - build.gradle
    - gradle/verification-metadata.xml
    - src/main/resources/application.properties
    - src/main/java/com/vrudenko/kanban_board/config/AsyncConfig.java
    - src/main/java/com/vrudenko/kanban_board/config/KafkaConsumerConfig.java
    - src/test/java/com/vrudenko/kanban_board/support/containers/AbstractKafkaContainerTest.java

key-decisions:
  - "The producer-side traceparent is written by setObservationEnabled(true) on the KafkaTemplate bean; the spring.kafka.template.observation-enabled property would be inert here and is absent."
  - "The async publish pool carries a ContextPropagatingTaskDecorator, so the send span is a child of the request trace instead of a new root."
  - "Observed sends fetch the Kafka cluster id through an admin call; it is bounded to 2s in application.properties so a down broker cannot stall a publish."

commits: 5
plan_head_before: 5c9762b8c9f62f66048a9e7c3109fed501bb0dfe
plan_head_after: 80831b3b25e350a3e7983bdb9789cba2c1802e92

completed: 2026-10-26
status: complete
---

# Phase 14 Plan 07: Tracing from HTTP through Kafka to the consumer Summary

**Task creation is now one trace from the HTTP request to the activity consumer, JSON logs carry trace ids, and export to a collector stays off until the Tempo plan switches it on.**

Nothing was pushed or deployed. Export is disabled by default (`TRACING_EXPORT_ENABLED=false`), so the code is inert in production until plan 14-09 sets the endpoint.

## Tasks

| Task | Name | Commits | Status |
| ---- | ---- | ------- | ------ |
| 1 | Tracing dependencies and properties, security spans suppressed, management-port requests pinned as unobserved, trace ids in JSON logs | d8f497b | done |
| 2 | One trace from HTTP through the async publish and Redpanda to the activity consumer | 8e29f14, 1afc0c2, a07a3df, 80831b3 | done |

The last three commits are fixes found while running task 2: the continuity test needed its own consumer group, and the observed send's Kafka admin call needed a 2s bound (pinned by `KafkaClusterIdLookupBoundE2ETest`).

## Verification run

This SUMMARY was written in a close-out session (2026-10-26), after the commits, not by the original executor.

- `./gradlew test --tests '*TraceLogCorrelationE2ETest' --tests '*ActivityTraceContinuityE2ETest' --tests '*KafkaClusterIdLookupBoundE2ETest' --tests '*ArchTest' -x jacocoTestCoverageVerification`: BUILD SUCCESSFUL in 1m 47s. XML reports show 1 test each, 0 failures, for the continuity test, the cluster-id test and the four nested `TraceLogCorrelationE2ETest` classes (log correlation, server span, security span, management port).
- `python3 scripts/verify-comments.py check`: OK, 326 files. `./gradlew spotlessCheck`: clean.
- Acceptance greps: sampling, export-enabled and spring.security properties each print one line; `ObservationPredicate` absent from `src/main/java`; `setObservationEnabled(true)` and `ContextPropagatingTaskDecorator` present; the inert `spring.kafka.template.observation-enabled` property absent.
- **Failing direction, checked:** with the `ContextPropagatingTaskDecorator` line removed from `AsyncConfig`, `ActivityTraceContinuityE2ETest` fails (AssertionError at line 240); restored afterwards, tree clean.

## Not verified

- Failing direction not re-run for: the security property, the tracing bridge (task 1), and `setObservationEnabled(true)` (task 2). The tests exist and pass; whether each breaks without its fix was not checked in the close-out session, and the original executor left no record.
- The plan's second task 2 verify command, the full `./gradlew test`, was not run here. Each of the five commits went through the repository's pre-commit hook (spotless, comment lint, `fastTest`), which is narrower than the full suite.
- Nothing exercised an actual OTLP export; that belongs to plan 14-09.
