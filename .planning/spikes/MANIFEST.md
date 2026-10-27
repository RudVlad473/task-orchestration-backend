# Spike Manifest

## Ideas

### openapi-error-coverage
Audit OpenAPI error-response coverage for the kanban-board-backend REST API. The API documents
errors via one global `ProblemDetailOpenApiCustomizer` bean that stamps the same six generic
status-code descriptions (400/401/403/404/409/500) onto every operation — a deliberate design
(D-08) that trades per-operation specificity for "no controller method can ever forget to declare
its error responses." The idea: prove out a per-operation override mechanism that layers a
concrete example on top of the generic bucket without replacing it (using POST /api/boards'
duplicate-board-name 409 as the concrete case), then survey the rest of the API for other places
where the generic bucket is too vague to be useful.

**Requirements:**
- Never propose abandoning the global-customizer architecture — work within it (D-08 is a
  deliberate decision, not an oversight).
- Any per-operation override must be additive to `ProblemDetailOpenApiCustomizer`'s generic
  bucket, never a replacement — the customizer's own conditional insert
  (`if (!responses.containsKey(statusCode))`) already supports this.

### web-fuzzing
Feasibility of adding OpenAPI-driven web fuzzing (Schemathesis) to this project: how much setup it
needs, what it finds against the live app, and what blocks it from being a trustworthy gate.

**Requirements:**
- Fuzzing writes data and publishes Kafka events: never point it at production or the shared nonprod
  broker; run only against a throwaway local/CI stack.
- No new framework in the Java build (project constraint); an external tool driven from the
  published OpenAPI document is in scope.

### likec4-diagram-sync
Feasibility of adopting LikeC4 (architecture-as-code, https://github.com/likec4/likec4) so diagrams
stay in sync with the real codebase: does the toolchain run here, can this app's topology be one
model with several views, and what does "sync" actually mean given LikeC4 reads no Java. Answer so
far: sync is a seam we write (a script that generates model fragments from code and diffs the
model's metadata against code facts), and it covers tables, topics, workloads and cited paths, not
relationships.

**Requirements:**
- Model by runtime role (thread pool, topic, table, pod, node), never classes, per
  docs/DIAGRAM_CONVENTIONS.md "does the diagram's truth survive a refactor".
- Every physical-view node carries its CPU architecture (DIAGRAM_CONVENTIONS physical view rule).
- A fact in the model that the code can contradict carries `metadata` (`source`, `workload`) so the
  drift check can test it; hand-drawn relationships are unverified and must be labelled as such.
- Pin the likec4 version and the Playwright Chromium revision it needs; PNG export is version-coupled.

## Spikes

| # | Idea | Name | Type | Validates | Verdict | Tags |
|---|------|------|------|-----------|---------|------|
| 001 | openapi-error-coverage | board-duplicate-name-409-override | standard | Per-operation `@ApiResponse` override layers on top of the global customizer's generic bucket without replacing it | VALIDATED (mechanism + non-regression proven; JSON capture pending a gradle-collision rerun) | openapi, springdoc, error-documentation |
| 002 | openapi-error-coverage | error-code-coverage-survey | standard | Every `ErrorCode` is named in the generic buckets; 3 operations found where the generic bucket is materially insufficient | PARTIAL (survey complete; live JSON confirmation pending the same rerun) | openapi, error-documentation, audit |
| 003 | web-fuzzing | web-fuzzing-schemathesis | standard | Schemathesis runs end to end against the live app via /api/docs + session cookie (24 ops, 51s) | PARTIAL (works; spec omits path params on 11 nested ops so they are unfuzzed; run-1 500 unconfirmed) | fuzzing, schemathesis, openapi |
| 004 | web-fuzzing | jqwik-web-fuzzing | comparison | jqwik + jqwik-spring run inside the existing Testcontainers/MockMvc harness; fails and shrinks correctly (200 checks on /signup, 0 5xx); costs 14 dependency-verification entries | VALIDATED (one endpoint, one property; broader coverage unmeasured) | fuzzing, jqwik, property-based-testing |
| 005 | likec4-diagram-sync | likec4-toolchain-and-views | standard | likec4 1.60.0 validates, exports PNG and builds a static site offline on this box (needs a one-time Playwright Chromium download) | VALIDATED | likec4, toolchain, pnpm |
| 006 | likec4-diagram-sync | likec4-model-of-this-app | standard | One model renders context, process, data, deployment and dynamic signin views; dynamic views cannot branch | VALIDATED (collapsed edges print `[...]`; signin shows the happy path only) | likec4, diagrams, c4 |
| 007 | likec4-diagram-sync | likec4-code-sync-seam | standard | 125-line sync.mjs generates tables and checks topics, workloads, source paths; 4 of 4 drift drills caught, renamed class not caught by design; found a real missing CronJob on first run | VALIDATED (covers 4 fact kinds, not relationships) | likec4, drift, ci |
