# Spike Conventions

Patterns and stack choices established across spike sessions. New spikes follow these unless the
question requires otherwise.

## Stack

Backend-only project (Spring Boot / Java 21 / Gradle) — no separate spike stack. Spikes that touch
source code work directly in the main tree (no scratch scaffolding needed), verified via the
project's own test harnesses rather than a bespoke spike runner.

## Patterns

**OpenAPI documentation overrides.** This API documents errors via one global
`ProblemDetailOpenApiCustomizer` bean stamping generic per-status descriptions onto every
operation (a deliberate decision, D-08, that rejects per-endpoint annotations by default). A
*specific* operation can still get a concrete example on top of that generic bucket — add a plain
`io.swagger.v3.oas.annotations.responses.ApiResponse` (with `@Content`/`@ExampleObject`) directly
on the controller method. springdoc's annotation-based generation runs before
`GlobalOpenApiCustomizer` beans (confirmed via springdoc's own docs, spike 001), and the
customizer's conditional insert (`if (!responses.containsKey(statusCode))`) leaves any
already-populated status code alone — so the two mechanisms compose without conflict. Never
replace or bypass the global customizer to do this.

**Verifying a generated OpenAPI document.** Use the harness `ProblemDetailOpenApiCustomizerTest`
already establishes: `@SpringBootTest @AutoConfigureMockMvc`, `@Value("${springdoc.api-docs.path}")`,
`mockMvc.perform(get(apiDocsPath))`, parse the response body with Jackson `ObjectMapper`. Don't
autowire the `OpenAPI` bean directly — springdoc caches the built document, so a test holding the
live instance risks mutating state shared with other assertions in the same Spring context.

**LikeC4 models (spikes 005-007).** One model under `docs/architecture/model/*.c4`, driven through
`docs/architecture/likec4.fish` (`start|validate|png|site|check|sync`). Elements are runtime roles,
never classes. A fact the code can contradict carries `metadata { source '<repo path>' workload
'<Kind/name>' }`; `docs/architecture/sync.mjs check` fails on drift and `sync` regenerates
`generated.c4` from the Flyway migrations. A drift drill is only valid against a green baseline
(spike 007 trail).

## Tools & Libraries

`likec4` 1.60.0 (pnpm, `docs/architecture/package.json`, installed with
`pnpm --dir docs/architecture install`); PNG export additionally needs Playwright
Chromium revision 1248 (`chromium-headless-shell`) in `~/.cache/ms-playwright`. `computedModel()` is
async despite the docs. `node_modules/` is not gitignored repo-wide: a folder that installs packages
ships its own `.gitignore` (`docs/architecture/.gitignore` does).

`io.swagger.v3.oas.annotations.*` (springdoc-openapi-starter-webmvc-ui's transitive
swagger-annotations dependency) — already on the classpath, no new dependency needed for
per-operation overrides.
