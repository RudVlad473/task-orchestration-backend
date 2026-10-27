# Phase 14: Production ownership: backend observability, alerting and recovery - Research

**Researched:** 2026-10-15
**Domain:** Spring Boot 3.5.16 observability (Micrometer, OTel, structured logs), kube-prometheus-stack 91.5.2 alerting, Tempo, Flux notifications, Postgres backup to Backblaze B2, on one k3s node
**Confidence:** MEDIUM-HIGH (app-side behaviour was exercised in a scratch worktree; cluster-side and B2-side behaviour is from docs/chart source and was NOT exercised live)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Split into Phase 14 (detection + backups) and Phase 14.1 (SLOs, dashboards, restore drill, capacity test, game days, runbooks). Backups are pulled forward from the source plan's week 4 into Phase 14 because production has had no backup since the 2026-09-26 cutover dump and the backup job depends on nothing else in the phase.
- **D-02:** Alertmanager's receiver is a Telegram bot. Token and chat ids come from a Kubernetes Secret via `bot_token_file` / `chat_id_file` (Alertmanager v0.34.1, shipped by chart 91.5.2), never inline in a manifest.
- **D-03:** Prod alerts page a primary Telegram chat. Nonprod alerts route to a second, muted Telegram chat. Note: Helm replaces lists, so a custom `routes` list replaces the chart's defaults.
- **D-04:** Dead-man's switch is both: (a) kube-prometheus-stack's always-firing `Watchdog` routed by an Alertmanager webhook receiver to a healthchecks.io check that notifies Telegram when pings stop, with `send_resolved: false`; (b) `.github/workflows/uptime-check.yml` posts to Telegram on failure and gets a keepalive against GitHub's 60-day scheduled-workflow auto-disable.
- **D-05:** Enable `alertmanager` and `defaultRules` in `k8s/monitoring/controllers/kube-prometheus-stack.yaml` (both currently `false`), with the default rule groups filtered to the relevant ones (`Watchdog`, node disk/memory, pod crash-looping and similar). Groups that misfire on k3s (etcd, scheduler, controller-manager, which k3s does not expose) stay off. The memory cost of Alertmanager is measured with the restart-ladder method before limits are set.
- **D-06:** Every external free tier was checked for actual suitability before being locked; the findings, caveats and sources are in `14-FREE-TIER-RESEARCH.md`. Caveats the planner must honour: Telegram group chats cap at 20 messages/minute (group alerts), an HTML message over 4096 chars is replaced by a fallback notice, healthchecks.io deletes accounts inactive for over a year and has no SLA, detection of a dead VM takes about 10-15 minutes.
- **D-07:** Tempo stores traces on a `local-path` volume on the VM disk with 7-day retention.
- **D-08:** Trace sampling is 100% in both environments, and the sampling ratio is a config property so it can be lowered without a code change.
- **D-09:** If measured memory headroom cannot fit Tempo, trim in this order: nonprod tracing off, then Tempo retention to 3 days, then Prometheus retention below 30 days. Prod signals are protected longest.
- **D-10:** Back up the prod database (`kanban_prod`) only. Nonprod is resettable by design (Phase 8).
- **D-11:** Nightly `pg_dump -Fc` from a Flux-managed Kubernetes `CronJob`, uploaded to **Backblaze B2, EU Central (Amsterdam)**. Reversibility: costly - a B2 account's region is fixed at sign-up; moving region or provider means a new account, new keys and re-uploading history.
- **D-12:** The VM's B2 application key is write-only and scoped to the one backup bucket: the VM can upload but never read or delete backups. Object Lock protects the retained files.
- **D-13:** Retention is 7 daily + 4 weekly (about 11 files), enforced by the bucket's lifecycle rules where the S3 API supports it, otherwise by the job. (B2 lifecycle via the S3 API is marked UNVERIFIED in the research; the planner confirms it.)
- **D-14:** Dumps are encrypted with `age` before upload. The VM holds only the public key; the private key lives only in the user's password manager. How the 14.1 restore drill obtains the private key is deferred to 14.1.
- **D-15:** An alert fires when no successful backup has completed in about 26 hours.
- Carried forward: one shared kube-prometheus-stack / Loki / Alloy stack monitors both prod and nonprod (P12 D-04, P13 D-07/D-17); every `resources.limits` value is measured with the restart-ladder method, not computed (P12, P13 D-07); the three public Grafana dashboard shares keep working and `scripts/verify-public-dashboards.py` must pass (P13 D-18); all cluster changes ship through Flux from `k8s/` (P13 D-14/D-15).

### Claude's Discretion
- Whether Alloy (already deployed) or a separate collector receives OTLP.
- Exact default-rule groups to enable under D-05, alert thresholds, repeat intervals, grouping.
- ServiceMonitor versus PodMonitor; management port number.
- Business-metric names and which 3-5 endpoints get `@Timed`/`Observation`.
- The uptime-check keepalive mechanism.
- CronJob schedule time and the upload tool (`rclone` or `aws s3`).

### Deferred Ideas (OUT OF SCOPE)
- Where the capacity test and game days run (nonprod shares the node with prod) - Phase 14.1 discussion.
- How the restore drill obtains the `age` private key - Phase 14.1 discussion.
- HA (replicas, PDB, HPA) - needs a second node; name it in the README as the next step.
- Phase 14.1 scope (SLO/burn-rate rules, dashboards, restore drill, capacity test, game days, runbooks) and all frontend work: not researched here.
</user_constraints>

<phase_requirements>
## Phase Requirements

No requirement IDs are mapped (`phase_req_ids` is null); the CONTEXT.md decisions D-01..D-15 are the contract. Research support per decision is in the sections named below.

| Decision | Research support |
|----------|------------------|
| D-02..D-06 | Findings 3, 6, 8; Alertmanager config validated with amtool |
| D-07..D-09 | Finding 4 (Tempo chart choice, retention key, OTLP path), Finding 7 (memory) |
| D-10..D-15 | Finding 5 (B2 semantics, image, CronJob, alert) |
| Metrics / logs / traces | Findings 1, 2 (verified in a scratch worktree) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Source: `/home/andre/dev/kanban-board-backend/.claude/CLAUDE.md` (read this session) plus the global `~/.claude/CLAUDE.md`.
- Field `@Autowired` in services/controllers; `@Transactional` is `jakarta.transaction.Transactional`; match the neighbouring class.
- Every test (fastTest included) runs on Testcontainers PostgreSQL via the real Flyway migrations; Docker must run. Query counts via `AbstractAppTest.countQueries`.
- Code comments are plain text (CODE_STYLE rule 14, enforced by `scripts/verify-comments.py`): identifiers by bare name, blank comment line between paragraphs, `- ` list items.
- Every commit runs `.githooks/pre-commit` (~4 min: gitleaks on staged diff, comment lint, instruction-file budget, spotlessCheck, fastTest). `--no-verify` is allowed for docs/planning/config-only commits, not for feature-heavy ones (Java, migrations, k8s manifests).
- All cluster changes via Flux from `k8s/`; Secrets are created on the VPS, never committed (invariant I8 fails any rendered `kind: Secret`).
- Every `resources.limits` is measured with the restart ladder (invariant I4 needs a MEASURED comment within 25 lines above each `memory:` line; CI runs `--no-provisional`).
- BEFORE creating or approving any PLAN.md: document 2 alternate technical approaches, a 3-column trade-off matrix [Approach | Pros/Cons | Why Picked], and non-obvious performance/memory/security trade-offs. Pre-supplied below in "Trade-off Matrix"; the planner must carry it into the plan.
- Never auto-execute code blocks without explaining the core data-flow in 3 sentences or less.
- Update infra diagrams (`docs/diagrams/`, `docs/DIAGRAM_CONVENTIONS.md`) in the same change as the topology change (memory: infra diagrams go stale silently).
- Inspect live output by running the app on the host and `curl`ing it (docs/LOCAL_DEV.md "Inspecting live output"), not by throwaway test classes.

## Summary

Four findings change the plan and the source plan's library specifics were wrong or incomplete on all four. **(1)** Moving actuator to a separate management port changes more than metrics: the app's single `SecurityFilterChain` (`anyRequest().authenticated()`) also guards the management port, so `/actuator/prometheus` returns 401 until a dedicated chain permits it, and `/api/actuator/health` on the main port stops existing (returns 500 through `GlobalExceptionHandler`'s catch-all). The public health URL used by `uptime-check.yml` and all three k8s probes must move; Boot's `additional-path` only accepts one path segment, so the public URL becomes `/api/healthz`, not `/api/actuator/health`. **(2)** Trace propagation through Kafka does not work with the properties the source plan names: `KafkaConsumerConfig` builds its own `@Primary KafkaTemplate`, which bypasses `spring.kafka.template.observation-enabled`, and `KafkaEventPublisher` hops to a `@Async` pool that drops the trace context. Both were fixed and proven in a scratch worktree (HTTP root, producer span, consumer span in one trace). **(3)** The `grafana/tempo` chart is flagged `deprecated: true` in the Grafana index (Tempo 2.9.0, chart 1.24.4); the maintained chart is `tempo` 3.2.0 (Tempo 3.1.0) in the `grafana-community` repo, and chart 3.2.0 is the first one that enforces `tempo.retention` (3.0.0-3.1.0 silently used 14 days). **(4)** The B2 lifecycle-via-S3-API item resolves to supported (since 2025-12) but must be set once with an admin key (a bucket-restricted write-only key cannot), and `gautamkrishnar/keepalive-workflow` is blocked on GitHub for ToS reasons, so the keepalive must be API-based or replaced by a heartbeat check.

Everything else is conventional and well-supported: Micrometer Prometheus registry plus a PodMonitor in the established pattern, ECS structured logs (Boot 3.5 emits `traceId`/`spanId` as top-level JSON keys, verified), `Watchdog` to healthchecks.io via `url_file`, a CronJob with a pinned Alpine-based image (`age` 1.2.1, `aws-cli`, `postgresql16-client` all present in alpine:3.22), and `kube_cronjob_status_last_successful_time` for the 26-hour alert (kube-state-metrics is enabled by chart default and collects cronjobs). The prod dump is tiny (103,928 bytes at cutover), so single-PutObject uploads, no multipart, and free-tier volume limits are irrelevant.

Live memory headroom was not measurable this session (no cluster access). The last recorded figure is 2286 MiB available (2026-09-27, before the Compose decommission); all new memory limits must go through the restart ladder in Wave 0/1 plans.

**Primary recommendation:** Build in this order: management port + security chain + probes + public `/api/healthz` (one atomic change, because each piece breaks another), then Prometheus registry/PodMonitor/business metrics, then enable Alertmanager + rules + dead-man's switch, then tracing/logs/Tempo, then the backup CronJob; end each stage with the time-to-notification measurement.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Business metrics, spans, JSON logs | App (API/Backend) | - | The app owns the decisions being counted (optimistic-lock conflict, DLT write) |
| Metrics exposure | App management port | Prometheus (scrape) | Separate port, not in the Service or any IngressRoute |
| Public health | App main port (`/api/healthz`) | Traefik IngressRoute | Unauthenticated, db-only group, no details |
| Probes (liveness/readiness/startup) | App management port | kubelet | Same aggregate semantics as today, new port |
| Alert evaluation / routing | Prometheus + Alertmanager (cluster) | - | One shared stack for both envs (P12 D-04) |
| Dead-man's switch | healthchecks.io (external) | Alertmanager webhook | Must live outside the VM it watches |
| Public uptime probe | GitHub Actions (external) | Telegram API | Sees the public URL as users do |
| Deploy verification | Flux kustomize-controller `wait: true` | Flux notification-controller, Prometheus rule | Independent of Prometheus for the Flux path |
| Trace storage | Tempo on `local-path` PVC | Grafana (query) | D-07 |
| Trace transport | App OTLP exporter direct to Tempo | (Alloy rejected, see matrix) | Alloy is at its measured memory floor |
| Backup job | k8s CronJob in `kanban-data` | B2 (off-VM) | Needs network path to Postgres (NetworkPolicy) |
| Backup freshness signal | kube-state-metrics | Prometheus rule | No Pushgateway installed |

## Standard Stack

### Core (Java, all BOM-managed, versions read from `spring-boot-dependencies-3.5.16.pom`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `io.micrometer:micrometer-registry-prometheus` | 1.15.12 | Prometheus scrape registry | The Boot-documented registry; resolved cleanly in a scratch build |
| `io.micrometer:micrometer-tracing-bridge-otel` | 1.5.12 | Micrometer Observation to OTel bridge | Boot docs name it with the exporter below [CITED: docs.spring.io/spring-boot/3.5/reference/actuator/tracing.html] |
| `io.opentelemetry:opentelemetry-exporter-otlp` | 1.49.0 | OTLP span export (HTTP default) | Same page |
| `io.micrometer:context-propagation` | 1.1.4 | Lets `ContextPropagatingTaskDecorator` carry the trace across `@Async` | Managed by `micrometer-bom` 1.15.12, not by Boot's own BOM block; optional dep of micrometer-core |
| `org.springframework.boot:spring-boot-starter-aop` | 3.5.16 | Only if `@Observed`/`@Timed` annotations are used (needs aspectjweaver) | [CITED: Boot observability docs "management.observations.annotations.enabled ... also requires aspectjweaver"] |
| `io.opentelemetry:opentelemetry-sdk-testing` (test) | 1.49.0 | `InMemorySpanExporter` for trace tests | Used in the scratch proof |

[VERIFIED: Gradle resolution in scratch worktree, versions present under `~/.gradle/caches/modules-2/files-2.1/io.micrometer/*` and `io.opentelemetry/*`.] Installing these requires regenerating `gradle/verification-metadata.xml` per the command at `build.gradle:96-119` (see Pitfall 11).

### Cluster components

| Component | Version | Source | Note |
|-----------|---------|--------|------|
| kube-prometheus-stack | 91.5.2 (existing) | `k8s/monitoring/controllers/kube-prometheus-stack.yaml` | Alertmanager image `v0.34.1` [VERIFIED: values.yaml:1033 of chart tag `kube-prometheus-stack-91.5.2`], Prometheus `v3.14.0-distroless` [VERIFIED: values.yaml:4540], KSM subchart 8.5.0 [VERIFIED: Chart.yaml] |
| Tempo chart | `tempo` **3.2.0** (appVersion 3.1.0, published 2026-10-14) from `grafana-community` | `https://grafana-community.github.io/helm-charts` (HTTP) or `oci://ghcr.io/grafana-community/helm-charts` | [VERIFIED: `index.yaml` entries 3.2.0/3.1.0/3.0.0; Chart.yaml in repo] |
| Backup image | pinned custom image from `alpine:3.22` (digest `sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8`) | packages `age` 1.2.1-r10, `aws-cli` 2.27.25-r0, `postgresql16-client` 16.15-r0 | [VERIFIED: `apk search` inside alpine:3.22.6 this session] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Tempo chart 3.2.0 (Tempo 3.1) | `grafana/tempo` 1.24.4 (Tempo 2.9.0) | Chart is `deprecated: true` [VERIFIED: grafana index.yaml]; do not start a new install on it |
| Tempo chart 3.2.0 | 3.1.0 | 3.1.0 ignores `tempo.retention` in the worker (14d default) [CITED: chart README "From Chart versions < 3.2.0"]; 3.2.0 is a day old, so check it installs before relying on it |
| `aws s3api put-object` | `rclone` | rclone's file-existence probe (HEAD/list) is untested against a write-only key; aws-cli `put-object` is exactly one PutObject call [ASSUMED for B2 write-only] |
| Direct app-to-Tempo OTLP | via Alloy | See Trade-off Matrix |

**Installation (Gradle, inside the `dependencies` block near `spring-boot-starter-actuator`):**
```groovy
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
implementation 'io.micrometer:context-propagation'
testImplementation 'io.opentelemetry:opentelemetry-sdk-testing'
```
`build.gradle:149-153` currently says the actuator dependency is "Not for general monitoring/metrics"; update that comment in the same change.

## Package Legitimacy Audit

`gsd_run query package-legitimacy check` supports only `npm|pypi|crates` (verified: its usage line), so it cannot audit Maven coordinates; it was not run.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `io.micrometer:micrometer-registry-prometheus` | Maven Central | years | n/a | github.com/micrometer-metrics/micrometer | not seam-checkable | Approved: named by Boot docs and managed in `spring-boot-dependencies-3.5.16.pom` / micrometer-bom |
| `io.micrometer:micrometer-tracing-bridge-otel` | Maven Central | years | n/a | github.com/micrometer-metrics/tracing | not seam-checkable | Approved: Boot docs |
| `io.opentelemetry:opentelemetry-exporter-otlp` | Maven Central | years | n/a | github.com/open-telemetry/opentelemetry-java | not seam-checkable | Approved: Boot docs; version from the opentelemetry-bom Boot imports |
| `io.micrometer:context-propagation` | Maven Central | years | n/a | github.com/micrometer-metrics/context-propagation | not seam-checkable | Approved: listed in micrometer-bom 1.15.12 (`<version>1.1.4</version>`) |
| `io.opentelemetry:opentelemetry-sdk-testing` | Maven Central | years | n/a | github.com/open-telemetry/opentelemetry-java | not seam-checkable | Approved (test scope), OTel BOM |

**Packages removed due to [SLOP]:** none. **Packages flagged [SUS]:** none. The existing dependency-verification metadata (`gradle/verification-metadata.xml`, SHA-256 per artifact) is the supply-chain control; it must be regenerated and the diff reviewed. The backup image's Alpine packages come from Alpine's signed index; the base image is pinned by digest.

## Architecture Patterns

### System Architecture Diagram

```
                    Internet
                       |
     GitHub Actions    |   Traefik (websecure)  -- IngressRoute routes only :8080 (http) --
     uptime-check.yml  |        |
     (15 min) --------->  /api/healthz (public, db-only group)       /api/** (session auth)
          | fail -> Telegram (curl)                 |
          | ok   -> healthchecks.io heartbeat       v
                                         +----------------- app pod (kanban-prod / kanban-nonprod) ------+
                                         | :8080 http   (Service port; routed)                           |
                                         | :9081 management (NOT in Service, NOT in any IngressRoute)    |
                                         |   /actuator/health  (probes)   /actuator/prometheus           |
                                         | JSON (ECS) logs to stdout, traceId/spanId top-level keys      |
                                         | OTLP http/protobuf spans --------------------------------+    |
                                         +--|--------------------|-------------------------------|---+
          Kafka (traceparent header)         | PodMonitor scrape    | logs (Alloy, k8s API)         |
   app -> Redpanda -> @KafkaListener         v                      v                               v
   (send span, receive span, one trace)   Prometheus (monitoring) Loki (derived field TraceID)   Tempo (monolithic, local-path 7d)
                                            | rules (PrometheusRule CRs + chart defaultRules)         ^
                                            v                                                        |
                                       Alertmanager ---- route: Watchdog --> webhook --> healthchecks.io --(no ping 10-15 min)--> Telegram
                                            |  \ namespace=kanban-nonprod --> muted chat
                                            |   \ default --> prod chat (Telegram)
                                            v    Grafana: Tempo datasource <-> Loki datasource (trace<->logs links)
   Flux (flux-system): Kustomization apps-prod wait:true --failure--> notification-controller --> Telegram
   kanban-data: CronJob postgres-backup (02:xx UTC): pg_dump -Fc -> pg_restore -l check -> age -r <pubkey> -> aws s3api put-object -> B2 (EU Central)
   kube-state-metrics: kube_cronjob_status_last_successful_time --> PrometheusRule PostgresBackupMissing (>26h)
```

### Recommended Project Structure

```
src/main/java/com/vrudenko/kanban_board/
  config/        # KafkaConsumerConfig (template observation, DLT counter), AsyncConfig (TaskDecorator)
  security/      # SecurityConfiguration (+ management chain, /healthz permit), auth-failure metric hook
  handler/       # GlobalExceptionHandler (counters at the existing arms)
k8s/base/app/app.yaml                    # containerPort "management", probes -> management port
k8s/overlays/{prod,nonprod}/             # env: OTLP endpoint/export toggle, resource attributes
k8s/monitoring/controllers/              # kube-prometheus-stack.yaml (alertmanager, defaultRules), tempo.yaml, repositories.yaml (+grafana-community)
k8s/monitoring/configs/                  # podmonitor-app.yaml, prometheusrules.yaml, datasources.yaml (+Tempo, Loki derived field)
k8s/flux-system/                         # notification Provider + Alert (telegram)
k8s/data/postgres/                       # backup-cronjob.yaml, networkpolicy.yaml (+peer)
.github/workflows/uptime-check.yml       # Telegram on failure, heartbeat/keepalive
docker/backup/Dockerfile (or equivalent) # only if the custom-image route is chosen
```

### Pattern 1: Management port with its own SecurityFilterChain (verified)

**What:** On a separate management port the app's existing chain still applies. Add a high-precedence chain matched with `EndpointRequest.toAnyEndpoint()`; this matcher is context-aware (it only matches on the management server when the ports differ), so the main port is unaffected.
**Verified behaviour (scratch worktree, `management.server.port=0`, Spring Boot 3.5.16):**
- Without a dedicated chain: mgmt `/actuator/health` 200, mgmt `/actuator/prometheus` 401 (ProblemDetail body), mgmt `/actuator/health/liveness` 401.
- With the chain below at `@Order(1)`: mgmt `/actuator/prometheus` 200, `/actuator/health/liveness` 200, main `/api/actuator/prometheus` still 401, `/actuator/env` still 401 (not exposed).
- Main `/api/actuator/health` after separating the port: **500** (`uri="/**"` 500 in `http_server_requests`), because the permitted-but-unmapped path falls into `GlobalExceptionHandler`'s `Exception.class` arm.
- A health group with `additional-path=server:/healthz` serves `GET /api/healthz` -> 200 `{"status":"UP"}` on the main port once `/healthz` is permitted. `additional-path=server:/actuator/health` fails at startup: `'value' must contain only one segment.`

```java
// Source: Spring Boot 3.5 actuator security docs (EndpointRequest.toAnyEndpoint) + scratch verification
@Bean
@Order(1)
public SecurityFilterChain managementChain(HttpSecurity http) throws Exception {
    http.securityMatcher(EndpointRequest.toAnyEndpoint());
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
    http.csrf(c -> c.disable());
    return http.build();
}
```
In `SecurityConfiguration`, replace `ApiPaths.ACTUATOR_HEALTH` in the `permitAll()` list (`SecurityConfiguration.java:79` is `ApiPaths.ACTUATOR_HEALTH)` and `ApiPaths.java:38` is `public static final String ACTUATOR_HEALTH = "/actuator/health";`) with a new `/healthz` constant, otherwise bots hitting `/api/actuator/health` get a 500 that feeds the 5xx-ratio alert.

### Pattern 2: Properties block (names verified against `spring-configuration-metadata.json` in the 3.5.16 jar)

```properties
management.server.port=9081
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.health.show-details=never
management.endpoint.health.group.public.include=db
management.endpoint.health.group.public.additional-path=server:/healthz
management.prometheus.metrics.export.enabled=true
management.metrics.distribution.slo.http.server.requests=50ms,100ms,250ms,500ms,1s,2s,5s
management.tracing.sampling.probability=${TRACING_SAMPLING_PROBABILITY:1.0}
management.otlp.tracing.export.enabled=${TRACING_EXPORT_ENABLED:false}
management.otlp.tracing.endpoint=${OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}
management.observations.enable.spring.security=false
spring.kafka.listener.observation-enabled=true
logging.structured.format.console=ecs
```
`management.tracing.sampling.probability` default is `0.1` (verified in metadata); 100% per D-08 needs the explicit value. `management.otlp.tracing.transport` default `http`. Metrics exposure widening of `exposure.include` is safe only because the port is not routed: write the reason beside the existing comment block at `application.properties:19-27` (the existing text says: `` `health` is the ONLY permitted value: the include list is an allowlist, not a default to widen. ``).

### Pattern 3: Kafka trace continuity (verified end to end)

Two code changes are needed; both were proven necessary by removal (without the `TaskDecorator` the producer span became a separate root trace; without `spring.kafka.listener.observation-enabled` no receive span exists; without `setObservationEnabled(true)` no producer span exists at all and the consumer span is an unlinked root).

```java
// KafkaConsumerConfig.kafkaTemplate (currently lines 81-83: `return new KafkaTemplate<>(kafkaProducerFactory);`)
var template = new KafkaTemplate<>(kafkaProducerFactory);
template.setObservationEnabled(true);   // spring.kafka.template.observation-enabled is bypassed by this hand-built @Primary bean
return template;

// AsyncConfig.kafkaPublishExecutor (lines 16-23): the @Async hop in KafkaEventPublisher.onActivityEvent drops the context
executor.setTaskDecorator(new org.springframework.core.task.support.ContextPropagatingTaskDecorator());
```
Result in the scratch test (`AutoConfigureObservability`, `InMemorySpanExporter`): `probe.root` -> `kanban.activity send` (PRODUCER, parent = root, same trace) -> `kanban.activity receive` (CONSUMER, parent = send span, same trace); the record carried header `traceparent=00-<traceId>-<spanId>-01`. The `deadLetterKafkaTemplate` needs no observation.

### Anti-Patterns to Avoid
- **Putting `/actuator/**` into the main chain's `permitAll()`:** it would also open it on the routed port if the port is ever merged back. Use the context-aware `EndpointRequest` chain.
- **Using `@SpringBootTest` defaults to test metrics/tracing:** Boot disables metric export and tracing propagation in tests (Pitfall 3).
- **A second Kafka observation switch:** do not add `spring.kafka.template.observation-enabled`; it does nothing for this app.
- **Alertmanager routes by `env` label:** only the PodMonitor-scraped app metrics can carry a custom `env`; kube-state-metrics and node alerts carry `namespace` or nothing. Route on `namespace`.

## Business metric and endpoint candidates (Finding 2, files read this session)

| Metric (suggested name) | Hook point | Evidence |
|---|---|---|
| tasks created `kanban_tasks_created_total` | `TaskService.save` | `TaskService.java:57-58` `@Transactional` / `public TaskResponseDTO save(SaveTaskRequestDTO dto, ColumnEntity column) {`; publishes `TaskCreatedEvent` |
| tasks moved `kanban_tasks_moved_total` (tag same/cross column) | `TaskService.moveToColumn` | `TaskService.java:172-173`; `sourceColumnId.equals(targetColumnId)` computed as `sameColumn`; publishes `TaskMovedEvent` |
| optimistic-lock conflicts `kanban_optimistic_lock_conflicts_total` (tag entity) | single choke point `GlobalExceptionHandler.handleOptimisticLockingFailure` | `GlobalExceptionHandler.java:150-156`; thrown from `TaskService.java:119,188`, `SubtaskService.java:103`, `BoardService.java:150`, `ColumnService.java:143,185` |
| Kafka publish failures `kanban_kafka_publish_failures_total` | `KafkaEventPublisher.onActivityEvent` `whenComplete` branch `if (ex != null)` | `KafkaEventPublisher.java:44-65`; also `kafka_producer_record_error_total` exists for free |
| DLT writes `kanban_activity_dlt_written_total` | `RetryListener.recovered` in `activityErrorHandler` | `KafkaConsumerConfig.java:175-185` (`failedDelivery` is the documented no-op at :168-173). Free alternative present: `spring_kafka_template_seconds_count{name="deadLetterKafkaTemplate",result="success"}` (seen in the scratch scrape) |
| auth failures `kanban_auth_failures_total` (tag signin/signup/unauthenticated) | `GlobalExceptionHandler.handleBadCredentialsException` (`:185-191`; `AuthenticationController` rethrows `BadCredentialsException` at `:99` and `:110` for signin and `:139` for signup) and `ProblemDetailAuthenticationEntryPoint.commence` (unauthenticated 401) | files read |
| ownership denials `kanban_ownership_denied_total` | `GlobalExceptionHandler.handleAppAccessDeniedException` (`:89-95`); thrown only at `OwnershipVerifierService.java:55` `throw new AppAccessDeniedException("Board");` | other `verifyOwnershipOf*` delegate to it |

Security-event JSON log lines belong in the same arms: log at WARN with SLF4J `addKeyValue` (ECS format adds MDC and key-values as JSON keys [CITED: docs.spring.io/spring-boot/3.5/reference/features/logging.html]). Do not log the email or password from a failed signin (log injection and PII).

**Hottest endpoints (3-5):** `GET /boards/{boardId}/full` (`BoardController.findFullById`, `@GetMapping(ApiPaths.BOARD_ID + ApiPaths.FULL)`, one nested document), `POST /boards/{boardId}/columns/{columnId}` (`ColumnController.addTaskByColumnId`, task create), `PATCH /tasks/{taskId}/move` (`TaskMoveController.moveToColumn`, ~9 DB statements), `POST /signin` (BCrypt cost dominates). Note Boot already times every request as `http_server_requests_seconds` with templated `uri` (verified: `uri="/signin"`, `"/boards"`); `@Timed` adds nothing there. The value of `@Observed` is a span plus timer around the **service method** (`TaskService.moveToColumn`, `BoardService.findFullById`, `TaskService.save`) and needs `spring-boot-starter-aop` and `management.observations.annotations.enabled=true`. Alternative with no new dependency: inject `ObservationRegistry` and wrap the body with `Observation.createNotStarted(...)`. The layering ArchUnit rules (`LayeringArchTest`) govern controller-to-repository and service rules only; a `MeterRegistry` field in a service or handler is not prohibited by any rule read.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Prometheus exposition | A custom `/metrics` endpoint | `micrometer-registry-prometheus` + actuator | Verified working on the management port |
| Trace id in logs | MDC filter | Boot structured logging + Micrometer tracing | `traceId`/`spanId` appear as top-level JSON keys automatically (verified in ECS output) |
| Trace context across `@Async` | Manual `Span` capture/restore | `ContextPropagatingTaskDecorator` | Verified: send span becomes child of the request trace |
| Kafka trace headers | Custom `ProducerInterceptor` | `KafkaTemplate.setObservationEnabled(true)` | Verified: `traceparent` header written |
| Dead-man's switch | A cron + curl sidecar | Alertmanager `Watchdog` -> webhook `url_file` | Primary source for `url_file`/`send_resolved` is `docs/configuration.md` v0.34.1 |
| Dump encryption | `openssl enc` or custom envelope | `age -r <public key>` | D-14; age is in Alpine |
| Retention enforcement | A delete loop in the job | B2 lifecycle rules (set once) | The write-only key cannot delete (D-12) |
| Backup freshness | A custom exporter / Pushgateway | `kube_cronjob_status_last_successful_time` | Already scraped; metric listed in KSM docs |
| Rule syntax checks | Eyeballing | `promtool check rules`, `amtool check-config` | Both verified runnable via docker images |

**Key insight:** every piece of this phase is configuration of existing components; the only new code is a handful of counters, one security chain, two lines for trace continuity, and one shell script.

## Findings by topic

### Finding 1: Spring Boot 3.5.16 facts (verified)

- Versions: micrometer 1.15.12, micrometer-tracing 1.5.12, opentelemetry 1.49.0, spring-kafka 3.3.16, kafka-clients 3.9.2, logback 1.5.34 [VERIFIED: `spring-boot-dependencies-3.5.16.pom` lines 117, 124, 145-146, 155, 195].
- Probe paths and ports: separating the port changes the k8s probes. They are at `k8s/base/app/app.yaml:87-103`: `startupProbe`/`readinessProbe`/`livenessProbe` all `httpGet: path: /api/actuator/health, port: http` (name `http`, `containerPort: 8080`, `app.yaml:62-63`). The management context has no `/api` prefix: use `path: /actuator/health`, `port: management` (add `- name: management` / `containerPort: 9081` to the container). The Service (`app.yaml:114-118`, only port `http`) must not gain the port.
- `register-schemas` initContainer (`app.yaml:44-59`) runs `AvroSchemaRegistrar` through `PropertiesLauncher`, not the Boot app: unaffected.
- Native structured logging: `logging.structured.format.console=ecs` produced single-line JSON with `@timestamp` in UTC (`...Z`), `log.level`, `log.logger`, `process.pid`, `service.name=kanban-board` and **`"traceId":"...","spanId":"..."` as top-level keys** whenever a span is current [VERIFIED: scratch run output]. The first "Securing GET ..." line of a request has no traceId (before the observation starts); later lines do. The Loki derived-field regex for this format is `"traceId":"(\w+)"` [CITED: grafana.com configure-trace-to-logs table].
- Test-time traps: `@SpringBootTest` turns metric export and tracing propagation off. With `management.prometheus.metrics.export.enabled=true` the scrape works; for tracing the `TextMapPropagator` is a no-op (record headers stay empty) until the test class carries `@AutoConfigureObservability` [VERIFIED: probe with headers=[] before, `[traceparent]` after]. A `@LocalManagementPort` injection needs `webEnvironment = RANDOM_PORT` (verified: `Could not resolve placeholder 'local.management.port'` otherwise).
- Spring Security emits three observations per request (`security filterchain before/after`, `authorize request`) as spans [VERIFIED: seen in exported spans]. Set `management.observations.enable.spring.security=false` [CITED: Boot docs, not exercised] or the trace store fills with noise at 100% sampling.
- Metrics present out of the box [VERIFIED: scraped in scratch]: `http_server_requests_seconds_*{uri,status,outcome}`, `kafka_consumer_fetch_manager_records_lag_max{client_id="consumer-activity-log-1",...}`, `kafka_producer_record_error_total`, `spring_kafka_listener_seconds_count{error,messaging_kafka_consumer_group="activity-log"}`, `spring_kafka_template_seconds_count{name,result,exception}`, `executor_queued_tasks{name="kafkaPublishExecutor"}`, `hikaricp_connections_active`, `jvm_memory_used_bytes`. About 563 of 893 exposition lines are Kafka client metrics per pod (series volume is small against Prometheus's 123,106 head series noted at `kube-prometheus-stack.yaml` comment "headStats: 123,106 series").

### Finding 3: kube-prometheus-stack 91.5.2 values (chart source read)

Existing file facts: `alertmanager:` / `enabled: false` (`kube-prometheus-stack.yaml:38-39`), `defaultRules:` / `create: false` (`:40-41`), `serviceMonitorSelectorNilUsesHelmValues: false` and `podMonitorSelectorNilUsesHelmValues: false` (`:84-85`). `ruleSelectorNilUsesHelmValues` is **not** set, and the chart default is `true`, which renders `ruleSelector: matchLabels: release: "<Release.Name>"` [VERIFIED: templates/prometheus/prometheus.yaml lines 287-296]. Either label every PrometheusRule `release: kube-prometheus-stack` (the Release name, confirmed by the Grafana service name comment `kube-prometheus-stack-grafana`) or add `ruleSelectorNilUsesHelmValues: false` beside the two existing lines (consistent with the established pattern). Recommendation: add the flag.

Alertmanager keys (all verified in chart `values.yaml`): `alertmanager.enabled` (:402), `alertmanager.config` (:553, a map written verbatim to Secret `alertmanager-kps-alertmanager` by `templates/alertmanager/secret.yaml`), `alertmanager.alertmanagerSpec.secrets` (:1045, "mounted into /etc/alertmanager/secrets/"), `alertmanagerSpec.resources` (:1177), `alertmanagerSpec.storage` (:1138, default none = emptyDir, silences lost on restart; acceptable).

Alertmanager config below passes `amtool check-config` from `prom/alertmanager:v0.34.1` (SUCCESS, 4 receivers). `bot_token_file`, `chat_id_file` (int64 parsed after `TrimSpace`) and webhook `url_file` (`TrimSpace`) confirmed in `notify/telegram/telegram.go` and `notify/webhook/webhook.go` at v0.34.1:

```yaml
alertmanager:
  enabled: true
  alertmanagerSpec:
    secrets: [alertmanager-secrets]     # keys: telegram-token, telegram-chat-prod, telegram-chat-nonprod, healthchecks-url
    # MEASURED (restart ladder, <date>): ... (required by invariant I4, <=25 lines above each memory: line)
    resources: { requests: { memory: <measured> }, limits: { memory: <measured> } }
  config:
    global: { resolve_timeout: 5m }
    route:
      receiver: telegram-prod            # default: infra-wide alerts page the prod chat
      group_by: [alertname, namespace]
      group_wait: 30s
      group_interval: 5m
      repeat_interval: 4h
      routes:
        - receiver: deadman
          matchers: ['alertname = "Watchdog"']
          group_wait: 0s
          group_interval: 1m
          repeat_interval: 5m
        - receiver: telegram-nonprod
          matchers: ['namespace = "kanban-nonprod"']
    receivers:
      - name: deadman
        webhook_configs:
          - url_file: /etc/alertmanager/secrets/alertmanager-secrets/healthchecks-url
            send_resolved: false
      - name: telegram-prod
        telegram_configs:
          - bot_token_file: /etc/alertmanager/secrets/alertmanager-secrets/telegram-token
            chat_id_file: /etc/alertmanager/secrets/alertmanager-secrets/telegram-chat-prod
            parse_mode: ""               # plain text: avoids the HTML 4096-char replacement (D-06)
            send_resolved: true
      - name: telegram-nonprod
        telegram_configs:
          - bot_token_file: /etc/alertmanager/secrets/alertmanager-secrets/telegram-token
            chat_id_file: /etc/alertmanager/secrets/alertmanager-secrets/telegram-chat-nonprod
            parse_mode: ""
            disable_notifications: true
```
Because Helm replaces lists, this `routes`/`receivers` replaces the chart default (which sends `Watchdog` to receiver `null`, `values.yaml` `alertmanager.config.route.routes`), and `templates: ['/etc/alertmanager/config/*.tmpl']` plus the chart's `inhibit_rules` are also dropped unless re-declared; re-add the inhibit rules if wanted. The nonprod route must sit before any catch-all.

`defaultRules.rules.<group>` toggles that exist (verified `values.yaml:176-210`): `alertmanager, etcd, configReloaders, general, k8sContainerCpuUsageSecondsTotal, k8sContainerMemoryCache, k8sContainerMemoryRss, k8sContainerMemorySwap, k8sContainerResource, k8sContainerMemoryWorkingSetBytes, k8sPodOwner, kubeApiserverAvailability, kubeApiserverBurnrate, kubeApiserverHistogram, kubeApiserverSlos, kubeControllerManager, kubelet, kubeProxy, kubePrometheusGeneral, kubePrometheusNodeRecording, kubernetesApps, kubernetesResources, kubernetesStorage, kubernetesSystem, kubeSchedulerAlerting, kubeSchedulerRecording, kubeStateMetrics, network, node, nodeExporterAlerting, nodeExporterRecording, prometheus, prometheusOperator, windows`. Per-alert opt-out is `defaultRules.disabled.<AlertName>: true`.

Recommended `defaultRules` (group -> alerts it renders, from the template files):
- **ON:** `general` (`TargetDown`, `Watchdog`, `InfoInhibitor`), `kubernetesApps` (`KubePodCrashLooping`, `KubePodNotReady`, `KubeDeploymentReplicasMismatch`, `KubeDeploymentRolloutStuck`, `KubeStatefulSetReplicasMismatch`, `KubeJobFailed`, `KubeContainerWaiting`, ...), `kubernetesStorage` (`KubePersistentVolumeFillingUp`, ...), `kubernetesSystem` (`KubeNodeNotReady`, `KubeletDown`, `KubeAPIDown`, kubelet/apiserver cert expiry), `nodeExporterAlerting` (`NodeFilesystemSpaceFillingUp`, `NodeFilesystemAlmostOutOfSpace`, `NodeMemoryHighUtilization`, `NodeDiskIOSaturation`, ...), `kubeStateMetrics`, `prometheus`, `prometheusOperator`, `alertmanager`, `configReloaders`, and `kubelet` (a tiny recording group, required because `KubeletPlegDurationHigh` reads the recording rule `node_quantile:kubelet_pleg_relist_duration_seconds:histogram_quantile`).
- **OFF:** `etcd`, `kubeControllerManager`, `kubeSchedulerAlerting`, `kubeSchedulerRecording`, `kubeProxy` (the templates additionally require `kubeEtcd/kubeProxy/kubeControllerManager/kubeScheduler.enabled`, which this file already sets false at `:46-53`, so they would not render anyway; set them false explicitly for clarity), `kubeApiserverAvailability`, `kubeApiserverBurnrate`, `kubeApiserverHistogram`, `kubeApiserverSlos` (heavy recording/SLO rules plus `KubeAPIErrorBudgetBurn`, Prometheus is already at 640Mi), `kubernetesResources` (overcommit/quota/`CPUThrottlingHigh` noise on a one-node cluster), `windows`, `network`, all `k8sContainer*` and `k8sPodOwner`, `kubePrometheusGeneral`, `kubePrometheusNodeRecording`, `node`, `nodeExporterRecording` (recording only; the three repo dashboards use raw metrics, verified by grepping `k8s/monitoring/configs/dashboards/*.json` for the recording-rule names: zero hits).
- Which groups "misfire on k3s": the ones whose targets k3s does not expose (etcd, controller-manager, scheduler, proxy: already excluded). `KubeAPIDown`/`KubeletDown` live in `kubernetesSystem`; the runbook records that the apiserver and kubelet are scraped, but the live `up` of those jobs was **not** checked (no cluster access): UNVERIFIED, check `/api/v1/targets` before enabling `kubernetesSystem`, and add `defaultRules.disabled.KubeAPIDown: true` if the job is absent.

Custom rules: put one `PrometheusRule` CR in `k8s/monitoring/configs/` (applied by Kustomization `monitoring`, which `dependsOn: monitoring-controllers`, so the CRD exists). The candidate expressions in Code Examples pass `promtool check rules` from `prom/prometheus:v3.14.0` (7 rules). kubeconform has a PrometheusRule v1 schema at the pinned catalog commit (HTTP 200 checked) so `scripts/verify-k8s-manifests.sh` validates it, but `PodMonitor` v1 and `ServiceMonitor` v1 already do.

PodMonitor vs ServiceMonitor: use a **PodMonitor**. The app Service exposes only `http` (`app.yaml:114-118`); a ServiceMonitor would need the management port on the Service (which risks routing). The established pattern is `k8s/monitoring/configs/podmonitors.yaml` (Redpanda): `namespaceSelector.matchNames: [kanban-prod, kanban-nonprod]`, `selector.matchLabels app.kubernetes.io/name`, `podMetricsEndpoints[].port` (a **named** containerPort), and a relabeling that derives `env` from `__meta_kubernetes_namespace` with `regex: kanban-(.+)`.

### Finding 4: Tempo, Grafana, OTLP path

- Chart choice: see Standard Stack. Values keys (chart 3.2.0 `values.yaml`, read this session): `fullnameOverride`, `tempo.retention` (default `24h`; set `168h`), `tempo.resources`, `persistence.enabled` (default false) / `persistence.size` (10Gi) / `persistence.storageClassName` (commented out; set `local-path`), `tempo.receivers.otlp.protocols.{grpc 4317, http 4318}` on by default, `tempo.server.http_listen_port: 3200`, `tempo.storage.trace.backend: local` with `/var/tempo/traces` and `/var/tempo/wal`, `securityContext` runs as 10001 with `fsGroup`, `serviceMonitor.enabled` (default false; Prometheus can scrape Tempo on 3200 if enabled). Tempo 3.x monolithic mode runs in one process and needs no Kafka [CITED: Tempo 3.0 release notes]. The chart's Service is named by `tempo.fullname` (with `fullnameOverride: tempo`: `tempo.monitoring.svc`), OTLP/HTTP on 4318, query on 3200.
- Add a second HelmRepository to `k8s/monitoring/controllers/repositories.yaml` (existing ones are plain HTTP `https://grafana.github.io/helm-charts` and `https://prometheus-community.github.io/helm-charts`): `https://grafana-community.github.io/helm-charts` (or `type: oci` with `oci://ghcr.io/grafana-community/helm-charts`). Note the kps chart's own Grafana subchart already moved to `oci://ghcr.io/grafana-community/helm-charts` (`Chart.yaml`), so this registry is the maintained home.
- Memory: **UNVERIFIED**. No published figure was found for Tempo 3.1 monolithic at this ingest volume. Treat as unknown; ladder it (Finding 7). The 7-day retention at 100% sampling of a low-traffic app is small on disk; the 10Gi default PVC is generous.
- Grafana datasource: extend `k8s/monitoring/configs/datasources.yaml` (ConfigMap `grafana-datasources`, label `grafana_datasource: "1"`). Keep the two existing literal UIDs (`PBFA97CFB590B2093`, `P8E80F9AEF21F6940`; the file header says public dashboards resolve datasources by uid only). Add a Tempo datasource with a new literal uid and `jsonData.tracesToLogsV2.datasourceUid: P8E80F9AEF21F6940`, and add `jsonData.derivedFields` to the Loki datasource with `matcherRegex: '"traceId":"(\w+)"'`, `datasourceUid: <tempo uid>`, `url: '$${__value.raw}'` [CITED: grafana.com provisioning examples; the `$$` escape is required in provisioning YAML]. The trace-to-logs custom query (Loki labels here are `namespace`, `pod`, `container`, `app`, from Alloy relabeling) and whether `${__trace.traceId}` is a valid variable were not verified: UNVERIFIED, confirm in the Grafana UI. `scripts/verify-public-dashboards.py` must still pass after the edit.
- OTLP into Alloy versus direct: Alloy is a DaemonSet whose measured floor is `MEASURED ... passed 75Mi, failed at 40Mi (dmesg OOMKilled, anon-rss ~39.6MiB + file-rss ~108.2MiB). Adopted 75Mi limit / 40Mi request` (`alloy.yaml`). Adding `otelcol.receiver.otlp` -> `otelcol.processor.batch` -> `otelcol.exporter.otlp` [CITED: grafana.com/docs/alloy collect/opentelemetry-data] means exposing 4317/4318 through the chart (`alloy.extraPorts`, not verified) and re-running the Alloy ladder. **Recommendation: direct app -> `http://tempo.monitoring.svc.cluster.local:4318/v1/traces`.** The Boot OTLP exporter has its own batching/queue (`management.tracing...` / `management.otlp.tracing.timeout` 10s). Switching later is a one-line endpoint change.
- Per-env switch for D-09 step 1: `MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=false` in the nonprod overlay env (relaxed binding of `management.otlp.tracing.export.enabled`). Env tag for traces: `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=<env>` [ASSUMED: Boot reads `OTEL_RESOURCE_ATTRIBUTES`; the class `OpenTelemetryResourceAttributes` exists in the jar; not exercised], or the property `management.opentelemetry.resource-attributes[...]` [VERIFIED name in metadata].

### Finding 5: Backups to B2 (resolves the UNVERIFIED items)

**B2 facts** (all `[CITED]` from backblaze.com docs fetched this session; nothing exercised against a live bucket):
- **Lifecycle via S3 API: supported** since 2025-12-11 (`PutBucketLifecycleConfiguration`, `GetBucketLifecycleConfiguration`, `DeleteBucketLifecycleConfiguration`). Mapping: S3 `Expiration` -> B2 *hide*; `NoncurrentVersionExpiration` -> B2 *delete*; `AbortIncompleteMultipartUpload` -> cancel unfinished large files. Supported: `Expiration Days`, `ExpiredObjectDeleteMarker=true`, `NoncurrentDays`, `DaysAfterInitiation`, `Filter Prefix`, `Status Enabled`. Unsupported (400): `Transition`, date-based expiry, `NewerNoncurrentVersions`, tag/size filters, **overlapping prefixes**, Disabled rules. Each `Expiration` rule must be paired with an `ExpiredObjectDeleteMarker=true` rule on the same prefix. Manage rules through one interface only: editing in the web console or `b2_update_bucket` regenerates rule IDs. [Source: backblaze.com/apidocs/s3-put-lifecycle-configuration, blog 2025-12-12]
- Rule setting needs `writeBuckets`, which "is not allowed for app keys that are restricted to a bucket" [CITED: application-key-capabilities page]. So lifecycle is set **once by the human with an admin-capable key**, never by the VM key. This also satisfies D-12.
- **Write-only key + Object Lock:** a key created in the web UI and restricted to a single bucket "is not assigned any" Object Lock capabilities, and uploads through it "receive the default retention mode and period for the bucket" [CITED: backblaze.com/blog/digging-deeper-into-object-lock/]. So a plain write-only bucket key uploads into a locked bucket and the default retention applies automatically. `writeFiles` covers regular and large-file upload APIs, including `b2_list_parts`, `b2_start_large_file`, `b2_finish_large_file` and also **`b2_hide_file`** [CITED: capabilities page]: a write-only key can hide (not delete) the current version; versions persist and locked versions cannot be deleted. The key cannot do HEAD/GET/list (`readFiles`/`listFiles`), which is why the job must not rely on a post-upload read.
- Object Lock: default retention is bucket-wide, not per-prefix; applies only to files uploaded after it is set; `days` 1-3000; **governance** can be overridden with `bypassGovernance`, **compliance** cannot be removed by anyone, and once Object Lock is enabled on a bucket it cannot be disabled [CITED: backblaze.com/docs/cloud-storage-object-lock]. "When a file is locked, lifecycle settings ... attempting to change or delete the file will fail" [same page]. Object Lock is enabled at bucket creation or later; default retention for a bucket created with it needs `writeBucketRetentions`.
- Endpoint: `https://s3.<region>.backblazeb2.com` [CITED: call-the-s3-compatible-api page]. For EU Central the region string `eu-central-003` appears only in a third-party guide (docs.polydoc.tech); treat as [ASSUMED] and have the human copy the exact endpoint from the bucket details page (human checkpoint).
- Sizing: `docs/INFRA_RUNBOOK.md:763` records `kanban_prod.dump 103,928 bytes`; 11 files is about 1 MB, so single PutObject, no multipart, no free-tier concern.
- aws-cli compatibility: AWS CLI >= 2.23 added default CRC64NVME checksums that B2 rejected ("Unsupported header 'x-amz-sdk-checksum-algorithm'") [CITED: aws/aws-cli#9214]; Backblaze later reported it implemented the checksum functionality in July 2025 [CITED: nocodb issue comment, secondary]. The Alpine package is 2.27.25 (post-change). Pin `AWS_REQUEST_CHECKSUM_CALCULATION=when_required` and `AWS_RESPONSE_CHECKSUM_VALIDATION=when_required` anyway (harmless, and `s3api put-object` honours them per the same thread). Test once with the real key (human checkpoint).

**Retention design the planner must put to the user** (see Open Question 1): with a bucket default Object Lock of N days, a daily object is deletable only after day N. Recommended: governance mode, default retention 7 days; lifecycle (S3 API, applied once): `daily/` `Expiration Days: 7` + `NoncurrentVersionExpiration NoncurrentDays: 1` (gone at day ~8, after the lock expires), `weekly/` `Expiration Days: 28` + `NoncurrentDays: 1`. Weeklies are then lock-protected for 7 days only; locking them for 28 days needs per-object retention, i.e. the `writeFileRetentions` capability on the VM key (create the key with `b2_create_key`/CLI, not the web UI).

**CronJob design.** Where: `k8s/data/postgres/backup-cronjob.yaml`, added to `k8s/data/postgres/kustomization.yaml` (Flux `data` Kustomization, `wait: true`, namespace `kanban-data`). Connectivity: `k8s/data/postgres/networkpolicy.yaml` admits 5432 only from namespaces `kanban-prod`/`kanban-nonprod` and from pods in `monitoring` labelled `kanban-board/postgres-client: "true"`; a pod in `kanban-data` is **not** admitted, so add a third, separate `from` entry (namespaceSelector `kanban-data` AND podSelector `kanban-board/postgres-client: "true"`) and label the Job's pod template (separate list entry, so the selectors AND-combine, as the file's own comment explains). Credentials: reference only `PROD_DB_USER`, `PROD_DB_PASS`, `PROD_DB_NAME` from Secret `postgres-init` with `secretKeyRef` (that Secret also holds the superuser password; do not `envFrom` it; keys listed in `postgres.yaml` comment: `POSTGRES_USER POSTGRES_PASSWORD PROD_DB_NAME PROD_DB_USER PROD_DB_PASS NONPROD_DB_NAME ...`). The prod role owns `kanban_prod` (`CREATE DATABASE :"prod_db" OWNER :"prod_user"`), so `pg_dump` works without the superuser. New Secret `postgres-backup` (created on the VPS, never committed; I8 fails a committed Secret): `B2_KEY_ID`, `B2_APP_KEY`. The `age` public recipient (`age1...`) is public and can be a plain env value or ConfigMap; the private key never touches the cluster (D-14).

Script shape (strict mode, validates before encrypting, never reads back from B2):
```sh
set -euo pipefail
ts=$(date -u +%Y%m%dT%H%M%SZ)
PGPASSWORD="$PROD_DB_PASS" pg_dump -h postgres.kanban-data.svc.cluster.local -U "$PROD_DB_USER" -d "$PROD_DB_NAME" -Fc -f /work/db.dump
pg_restore -l /work/db.dump > /dev/null          # archive TOC readable
age -r "$AGE_RECIPIENT" -o /work/db.dump.age /work/db.dump
aws s3api put-object --endpoint-url "$B2_ENDPOINT" --bucket "$B2_BUCKET" --key "daily/kanban_prod-$ts.dump.age" --body /work/db.dump.age
[ "$(date -u +%u)" = 7 ] && aws s3api put-object ... --key "weekly/kanban_prod-$ts.dump.age" --body /work/db.dump.age
```
Spec: `schedule` an off-peak minute (e.g. `17 2 * * *`), `timeZone: Etc/UTC`, `concurrencyPolicy: Forbid`, `startingDeadlineSeconds`, `backoffLimit` small, `activeDeadlineSeconds`, `successfulJobsHistoryLimit`/`failedJobsHistoryLimit` > 0, `emptyDir` for `/work`, `automountServiceAccountToken: false`, `securityContext` non-root + `allowPrivilegeEscalation: false` + `capabilities.drop: [ALL]` (match `app.yaml`). Memory: invariant I3 iterates `Deployment, StatefulSet, DaemonSet, Job, ReplicaSet, Pod` only (`scripts/verify-k8s-invariants.py:130`), so a CronJob's nested template is not checked by I3, but I4 still scans every hand-written `memory:` line for a MEASURED comment, and D-05/P13 require a ladder anyway (aws-cli is Python; its footprint was not measured). The retired-memory comment rule applies to the CronJob.

**Image decision:** no stock image combines `pg_dump` 16, `age`, and an S3 client [ASSUMED after searching; third-party "postgres-backup-s3" images are not recommended with superuser-adjacent credentials]. Recommended: a small pinned image (`FROM alpine:3.22@sha256:5291449c...` + `apk add --no-cache age aws-cli postgresql16-client`), built in CI, tag pinned in the manifest. Fallback if the planner wants to avoid a new image pipeline: stock `alpine:3.22` with `apk add` at job start (runtime network dependency on Alpine mirrors and unpinned package versions; a failed run is caught by the 26h alert). See the matrix.

**Missing-backup alert** (D-15): `kube-state-metrics` is enabled by chart default (`kubeStateMetrics: enabled: true`, `values.yaml:2835-2836`; this repo only sets resources) and its subchart collects `cronjobs` (kube-state-metrics 8.5.0 default `collectors` list). `kube_cronjob_status_last_successful_time` is listed (STABLE) in the KSM CronJob metrics doc; labels `cronjob`, `namespace`. The metric is absent until the first success, so the rule needs `or absent(...)` as in Code Examples. It reports "pod exited 0", so the script's `set -e` and put-object exit status are the integrity signal. Also covered for free by the default `KubeJobFailed` (15m) once `kubernetesApps` is on.

### Finding 6: Production post-deploy health verification under Flux

Existing facts: `k8s/flux-system/apps-prod.yaml` has `interval: 10m`, `prune: true`, `wait: true`, `dependsOn: data, edge`; `apps-nonprod.yaml` additionally lists `healthChecks` for `Deployment app` and `StatefulSet redpanda`. `wait: true` makes kustomize-controller wait for every applied resource to become ready and report failure otherwise; notification-controller is installed (`gotk-components.yaml` header: `Components: source-controller,kustomize-controller,helm-controller,notification-controller,image-reflector-controller,image-automation-controller`, Flux v2.9.5) and `Provider`/`Alert` CRDs are served at `v1beta3` (type `telegram` present in the CRD enum).

Recommended layers (each independent of the next):
1. **Flux `Provider` (type `telegram`) + `Alert`** in `k8s/flux-system/`: `Provider.spec.channel` is the numeric chat id (`"-1557265138"` form or `@name`), `secretRef` points to a Secret with key `token` in the **same namespace** (create `telegram-token` in `flux-system` on the VPS); `address` is ignored for telegram [CITED: fluxcd.io Providers page]. `Alert` `eventSeverity: error`, `eventSources`: `Kustomization/apps-prod`, `Kustomization/apps-nonprod`, `HelmRelease/*`, `ImageUpdateAutomation/*`. This pages on a failed `wait` even if Prometheus/Alertmanager are down. Detection latency is bounded by the Kustomization timeout (defaults to the 10m interval) [ASSUMED default].
2. A `PrometheusRule` `AppDeploymentUnavailable`: `kube_deployment_status_replicas_available{namespace="kanban-prod",deployment="app"} < 1` for `8m` (startupProbe allows 36 x 5s = 180s, `app.yaml:87-92`, and `Recreate` leaves zero pods briefly during every deploy, so a shorter `for` pages on healthy deploys). Default `KubeDeploymentReplicasMismatch`/`KubePodCrashLooping` fire after 15m.
3. `AppTargetDown` (scrape) and the GitHub uptime probe.
4. Optional positive signal: a second `Alert` with `eventSeverity: info` for `Kustomization/apps-prod` gives a "reconciled and healthy" message after each deploy; volume of info events was **not** verified (UNVERIFIED), so trial it before keeping it.
The Flux `alertmanager` provider type also exists (labels `alertname=Flux<Kind><reason>`), but events carry no end time and need `global.resolve_timeout` tuning [CITED: same page]; do not use it.

### Finding 7: VM memory headroom (no live measurement possible this session)

- Spec: 4 vCPU, 7.8 GiB, 251 GB disk (`docs/INFRA_RUNBOOK.md:24`).
- Last recorded `free -m` after the full ladder with all workloads (including the then-running Compose stack): `Mem:            7945        5659         398          42        2255        2286` (total/used/free/shared/buff-cache/available MiB; `INFRA_RUNBOOK.md:664-673`, 2026-09-27). Sum of container memory *requests*: **3405Mi (~3.33Gi)** of 8136004Ki allocatable (same section). Compose was decommissioned 2026-09-30 (INFRA_ARCHITECTURE.md header), so current headroom is likely higher: UNVERIFIED, re-measure with `free -m` and `max_over_time(container_memory_working_set_bytes[3d])` at Wave 0.
- Live peaks to protect: Prometheus 431 MiB against a 640Mi limit; app prod 532.9MiB peak against a 576Mi request (`overlays/prod/patches.yaml`, limit 3Gi); the Prometheus 2-hourly head compaction is where past OOM kills landed (comment in `kube-prometheus-stack.yaml`).
- Expected adds, all **[ASSUMED]** estimates to be replaced by ladder results: Alertmanager + its config-reloader sidecar tens of MiB; Tempo 3.1 monolithic unknown, plan for low hundreds of MiB; OTel SDK + Prometheus registry in each app pod tens of MiB of heap/threads, so **re-measure the app peak** and probably raise the prod request above 576Mi; Prometheus gains a few thousand series (about 900 exposition lines per pod x 2 envs plus histogram buckets) against a 123k baseline; the backup Job is a transient pod (Python aws-cli) once a night.
- Method (from `docs/INFRA_RUNBOOK.md:574-600` "k3s resource measurement"): start from a provisional value, halve per rung, at each rung run the standard workload cycle (54-request API burst on both envs, Grafana dashboards, wide Loki query, issue/delete a scratch Certificate, 20s settle), require `RestartCount=0` plus clean `dmesg`/`lastState`, adopt with headroom, re-verify from a fresh recreate, suspend Flux for the window and resume in reverse order, then hold a 24h zero-restart window. Lesson recorded at `kube-prometheus-stack.yaml`: minutes-per-rung ladders miss the 2-hour compaction spike, so also read the live 3-day peak before adopting.
- D-09 trim order is already encoded: nonprod tracing off (`MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=false` in the nonprod overlay), then `tempo.retention: 72h`, then Prometheus `retention` below 30d.

### Finding 8: `uptime-check.yml`

Current file (`.github/workflows/uptime-check.yml`): cron `'*/15 * * * *'`, `permissions: {}`, header holes (b) 60-day disable and (c) "pages nobody"; script exits 1 on failure. The repo is public (`gh api` -> `visibility: public`); the last five runs (2026-10-14/15) were `schedule` and `success`; git history has 711, 260 and 65 commits in 2026-08/09/10 including Flux bot commits, so the 60-day rule is unlikely to trigger from commits alone, but "activity" is undefined in the docs.
- **Telegram on failure:** a final step `if: failure()` doing `curl --max-time 20 -sS https://api.telegram.org/bot${TG_TOKEN}/sendMessage -d chat_id=${TG_CHAT} --data-urlencode text=...` with both values from `env:` (repo secrets), per the file's own no-`${{ }}`-in-`run` rule. Telegram's per-chat limits are far above one message per 15 minutes [CITED: 14-FREE-TIER-RESEARCH.md].
- **Keepalive:** `gautamkrishnar/keepalive-workflow` cannot be used or inspected: `gh api repos/gautamkrishnar/keepalive-workflow` returns `Repository access blocked ... reason: tos`. Do not recommend a blocked action. Options: (a) **heartbeat check (recommended):** the workflow's success path pings a second healthchecks.io check (period 30m, grace 30m; the free plan allows 20 checks); if GitHub silently disables the schedule, or runs stop, healthchecks.io pages Telegram. This detects any cause, independent of GitHub's undefined "activity" rule. (b) best-effort prevention in the same run: `gh api -X PUT repos/$GITHUB_REPOSITORY/actions/workflows/uptime-check.yml/enable` with `permissions: actions: write`; that this resets the 60-day timer is [ASSUMED] (it is the mechanism the blocked action documents, unreadable now). (c) rely on repo commits: unverifiable.
- Cron minute: GitHub warns schedule events can be delayed or dropped at the top of the hour [CITED: free-tier research]; `*/15` hits `:00`; use e.g. `7,22,37,52`.
- Probe URL change: replace `/api/actuator/health` with `/api/healthz` in both `HEALTH_URLS` lines (`uptime-check.yml:39-40`); the body marker `"status":"UP"` still matches (verified body `{"status":"UP"}`).

## Trade-off Matrix (required by project CLAUDE.md; carry into PLAN.md)

| Decision | Approach A | Approach B | Why picked |
|---|---|---|---|
| Public health + probes | A: management port for probes/metrics, public `/api/healthz` db-only group (verified) | B: keep actuator on the main port, block `/api/actuator/prometheus` in Traefik | A. Matches the locked "separate management port" intent; B leaves the metrics route one IngressRoute edit from public. Cost of A: touches probes, uptime-check, `SecurityConfiguration`, `ActuatorHealthE2ETest` together |
| OTLP path | A: app direct to Tempo `:4318` | B: via Alloy OTLP receiver | A. Alloy is at a measured 40-75Mi floor and its chart port exposure is unverified; the exporter already batches. B buys buffering/tail-sampling later at the price of a re-ladder |
| Backup image | A: pinned custom image from alpine:3.22 (needs CI job + registry) | B: stock `alpine:3.22` + `apk add` at run time | A. Reproducible and no nightly package download with DB credentials in the pod; B is acceptable as a first cut and the 26h alert covers its failure mode |
| Tempo chart | A: `grafana-community/tempo` 3.2.0 (Tempo 3.1) | B: deprecated `grafana/tempo` 1.24.4 (Tempo 2.9) | A. B is deprecated and ages out; A's retention fix is exactly D-07 |
| Keepalive | A: heartbeat to healthchecks.io + best-effort `enable` call | B: third-party keepalive action | A. B's repository is blocked for ToS reasons |
| Deploy verification | A: Flux telegram Alert on `wait` failure + KSM rule | B: only the KSM rule | A. A pages even when Prometheus/Alertmanager is the thing broken |

Non-obvious trade-offs: 100% sampling x Spring Security spans x scrapes of actuator multiplies span volume (suppress `spring.security` observations; exclude `/actuator/**` http server spans if the exporter volume is high [ASSUMED to need a predicate; not verified]); `management.metrics.distribution.slo.http.server.requests` adds 7 buckets per `uri x status x method` series, much cheaper than `percentiles-histogram` (about 70+ buckets) and enough for p95; Phase 14.1 burn-rate rules will want the same bucket boundaries, so choose them now (include the future SLO latency threshold). Backup key is write-only but can still `hide` objects; the real delete protection is Object Lock plus versioning, not the key capability alone.

## Common Pitfalls

### Pitfall 1: Moving the management port breaks four things at once
**What goes wrong:** main `/api/actuator/health` returns 500 (permitted but unmapped, swallowed by the catch-all `GlobalExceptionHandler.java:72-78`), so `uptime-check.yml` goes red, all three k8s probes fail (CrashLoop on rollout), and `ActuatorHealthE2ETest` fails.
**How to avoid:** one change: properties + chain + `/healthz` permit + probes (`app.yaml:87-103`) + uptime URL + test rewrite. Deploy order: because Flux applies the new image and the new manifest together (image tag and base manifest are separate commits: image automation commits only the tag), land the manifest change and the image in a state where both old and new paths work, or accept one failed rollout window. Recommend keeping `/api/actuator/health` served until the probes move (extra step) or doing the rollout on nonprod first.
**Warning signs:** `uri="/**"` 500s in `http_server_requests`; probes failing with connection refused on 8080 path.

### Pitfall 2: `/actuator/prometheus` is 401 on the management port
The app's `anyRequest().authenticated()` also guards the child management context (verified). Needs the `EndpointRequest` chain (Pattern 1).

### Pitfall 3: Boot tests silently disable what you are testing
`@SpringBootTest` disables metric export and tracing propagation. Use `management.prometheus.metrics.export.enabled=true` for scrape tests and `@AutoConfigureObservability` for tracing tests, otherwise `traceparent` is never written (verified: empty headers) and you chase a ghost.

### Pitfall 4: Spring Kafka observation properties do not reach the hand-built template
`KafkaConsumerConfig` defines `@Primary kafkaTemplate`, so `KafkaAutoConfiguration.kafkaTemplate` backs off and `spring.kafka.template.observation-enabled` is ignored (Pattern 3). The class Javadoc at `KafkaConsumerConfig.java:61-76` already explains the same back-off mechanism for the factory.

### Pitfall 5: `@Async` drops the trace
The AFTER_COMMIT listener (`KafkaEventPublisher.java:44-47`) runs on `kafkaPublishExecutor`; without `ContextPropagatingTaskDecorator` the producer span is a new root (verified).

### Pitfall 6: Chart default `ruleSelectorNilUsesHelmValues: true`
A PrometheusRule without `release: kube-prometheus-stack` is silently ignored. Add the flag or the label; verify with `promtool`-independent live check (Prometheus `/rules`).

### Pitfall 7: Alertmanager pod will not start if the Secret is missing
A `secrets:` entry mounts the Secret as a volume; a missing Secret leaves the pod `ContainerCreating` [ASSUMED: standard Kubernetes behaviour for non-optional secret volumes]. Create `alertmanager-secrets` in `monitoring` (human step) before the Flux commit that enables Alertmanager. Same for Flux's `telegram-token` in `flux-system` and `postgres-backup` in `kanban-data`.

### Pitfall 8: Custom `routes` replaces the default Watchdog-to-null route and inhibit rules
Declare the Watchdog route explicitly (above). Without `send_resolved: false` a resolved POST counts as a ping and delays the dead-man alert (free-tier research).

### Pitfall 9: The backup job cannot reach Postgres
`postgres-ingress` NetworkPolicy has no peer for `kanban-data` pods (Finding 5). Symptom: `pg_dump` connect timeout. The alert still fires after 26h, but the first night is lost; test with a manual `kubectl create job --from=cronjob/postgres-backup` before relying on the schedule.

### Pitfall 10: Lifecycle vs Object Lock timing
A rule that hides at day 7 and deletes at day 8 only succeeds if the default retention is <= 7 days; locked versions are skipped, not queued by this doc's wording [CITED: "deletion does not occur"]. Whether Backblaze retries after the lock expires is not documented: UNVERIFIED, observe after the first full cycle.

### Pitfall 11: Dependency verification metadata
New artifacts fail resolution until `gradle/verification-metadata.xml` is regenerated: `./gradlew --write-verification-metadata sha256 --refresh-dependencies spotlessCheck test` (`build.gradle:96-119`). The scratch probes used `--dependency-verification off` only because they were disposable.

### Pitfall 12: k8s gates
`scripts/verify-k8s-invariants.py --no-provisional` (CI) requires: a MEASURED comment within 25 lines above every `memory:` value (I4, includes HelmRelease values), no committed `kind: Secret` (I8), every new directory with a `kustomization.yaml` rendered or excluded (I7). `scripts/verify-k8s-manifests.sh` validates CRs against a pinned CRD catalog; Flux `Provider`/`Alert` v1beta3 and PrometheusRule v1 schemas exist there (HTTP 200 checked at the pinned commit `ad3b08c5...`). A Tempo HelmRelease under `controllers/` needs no new kustomization root.

### Pitfall 13: Telegram plain text vs HTML and group rate limits
Set `parse_mode: ""` to dodge the 4096-char HTML replacement; keep `group_by` coarse so a burst is a few messages (D-06).

## Code Examples

### PodMonitor for the app (pattern from `k8s/monitoring/configs/podmonitors.yaml`)
```yaml
# Source: established Redpanda PodMonitor in this repo; label names to be confirmed against live /api/v1/targets
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata: { name: app, namespace: monitoring }
spec:
  namespaceSelector: { matchNames: [kanban-prod, kanban-nonprod] }
  selector: { matchLabels: { app.kubernetes.io/name: app } }
  podMetricsEndpoints:
    - port: management          # needs `ports: - name: management, containerPort: 9081` in app.yaml
      path: /actuator/prometheus
      relabelings:
        - sourceLabels: [__meta_kubernetes_namespace]
          regex: kanban-(.+)
          targetLabel: env
```

### Candidate alert rules (syntax checked with `promtool check rules`, prom/prometheus:v3.14.0: SUCCESS, 7 rules)
```yaml
- alert: AppTargetDown           # up{...,container="app"}: confirm labels live
  expr: up{namespace=~"kanban-(prod|nonprod)", container="app"} == 0
  for: 2m
- alert: App5xxRatioHigh
  expr: sum by (namespace) (rate(http_server_requests_seconds_count{namespace=~"kanban-(prod|nonprod)", status=~"5.."}[5m])) / sum by (namespace) (rate(http_server_requests_seconds_count{namespace=~"kanban-(prod|nonprod)"}[5m])) > 0.05
  for: 5m
- alert: AppP95LatencyHigh       # needs the slo buckets above
  expr: histogram_quantile(0.95, sum by (namespace, le) (rate(http_server_requests_seconds_bucket{namespace=~"kanban-(prod|nonprod)", uri!~"/actuator.*"}[5m]))) > 1
  for: 10m
- alert: ActivityDeadLetterWritten
  expr: increase(kanban_activity_dlt_written_total[10m]) > 0
- alert: ActivityConsumerLagRising
  expr: kafka_consumer_fetch_manager_records_lag_max{namespace=~"kanban-(prod|nonprod)"} > 100 and deriv(kafka_consumer_fetch_manager_records_lag_max[10m]) > 0
  for: 10m
- alert: PostgresBackupMissing
  expr: (time() - kube_cronjob_status_last_successful_time{namespace="kanban-data", cronjob="postgres-backup"} > 26 * 3600) or absent(kube_cronjob_status_last_successful_time{namespace="kanban-data", cronjob="postgres-backup"})
  for: 15m
- alert: CertificateExpiringSoon # cert-manager ServiceMonitor is enabled at k8s/platform/cert-manager/cert-manager.yaml
  expr: (certmanager_certificate_expiration_timestamp_seconds - time()) < 14 * 86400
  for: 1h
```
VM disk is covered by the default `NodeFilesystemAlmostOutOfSpace`/`NodeFilesystemSpaceFillingUp` once `nodeExporterAlerting` is on (node-exporter mounts the host root: `hostRootFsMount.enabled: true`). Redpanda's own lag metric names (for a broker-side lag alert) were not verified: UNVERIFIED (`curl <pod>:9644/public_metrics | grep -i lag`); the app-side `kafka_consumer_fetch_manager_records_lag_max` is verified present.

### kube-prometheus-stack additions (next to existing lines 38-41 and 84-85)
```yaml
alertmanager:
  enabled: true          # see Finding 3 for alertmanagerSpec and config
defaultRules:
  create: true
  rules:
    general: true
    kubernetesApps: true
    kubernetesStorage: true
    kubernetesSystem: true
    nodeExporterAlerting: true
    kubeStateMetrics: true
    prometheus: true
    prometheusOperator: true
    alertmanager: true
    configReloaders: true
    kubelet: true
    # all other groups: false (list in Finding 3)
prometheus:
  prometheusSpec:
    ruleSelectorNilUsesHelmValues: false
```

### Tempo HelmRelease values skeleton
```yaml
values:
  fullnameOverride: tempo
  tempo:
    retention: 168h
    # MEASURED (restart ladder, <date>) ...
    resources: { requests: { memory: <measured> }, limits: { memory: <measured> } }
  persistence: { enabled: true, storageClassName: local-path, size: 10Gi }
  serviceMonitor: { enabled: true }
```

### Flux notification (same-namespace Secret with key `token`)
```yaml
apiVersion: notification.toolkit.fluxcd.io/v1beta3
kind: Provider
metadata: { name: telegram, namespace: flux-system }
spec: { type: telegram, channel: "<prod chat id>", secretRef: { name: telegram-token } }
---
apiVersion: notification.toolkit.fluxcd.io/v1beta3
kind: Alert
metadata: { name: deploy-failures, namespace: flux-system }
spec:
  providerRef: { name: telegram }
  eventSeverity: error
  eventSources:
    - { kind: Kustomization, name: apps-prod }
    - { kind: Kustomization, name: apps-nonprod }
    - { kind: HelmRelease, name: "*" }
```
(Flux CRs in `flux-system` are in the `k8s/flux-system/kustomization.yaml` resource list, reconciled by the root sync; add the new file there.)

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (Testcontainers, promtool/amtool images) | all tests, rule/config checks | yes | 29.1.3 | - |
| JDK 21 | build/tests | yes | temurin 21.0.12 | - |
| `kubectl`, `helm` on PATH | live checks | no | - | `scripts/verify-k8s-manifests.sh` downloads pinned kubectl/helm; live cluster commands need the operator's VM access |
| `promtool` / `amtool` | rule/config validation | via docker | prom/prometheus:v3.14.0 (go1.26.6), prom/alertmanager:v0.34.1 | - |
| Cluster / VM access | live measurements, firing alerts | not tested this session | - | plans must carry explicit operator tasks |
| `gh` | repo/workflow facts | yes | - | - |
| Telegram bot, healthchecks.io, B2 account, `age` keypair | D-02, D-04, D-11, D-14 | human-only | - | human checkpoints from `14-FREE-TIER-RESEARCH.md` "Setup steps only the human can do" |

**Missing with no fallback:** live VM/cluster access in this session (affects Finding 7 numbers and label confirmations). **Missing with fallback:** kubectl/helm (pinned downloads).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test 3.5.16, Testcontainers (Postgres, Redpanda), RestAssured 5.5.5, AssertJ, Awaitility, ArchUnit 1.4.2 |
| Config file | `build.gradle` (`test`, `fastTest` tasks), `src/main/resources/application-test.properties` |
| Quick run command | `./gradlew fastTest` (excludes `@Tag("kafka")`/`@Tag("realSocket")`; ~4 min, pre-commit gate) |
| Full suite command | `./gradlew test` (adds kafka/realSocket; finalizedBy `jacocoTestCoverageVerification` 0.90/0.90/0.75) |
| Static infra checks | `bash scripts/verify-k8s-manifests.sh`; `python3 scripts/verify-k8s-invariants.py --no-provisional`; `python3 scripts/verify-public-dashboards.py` |

### Phase Deliverables -> Verification Map
| Deliverable | Behaviour | Layer | Command / procedure | Exists? |
|-------------|-----------|-------|---------------------|---------|
| Management port | `/actuator/prometheus` 200 on mgmt port, 401 on `/api/actuator/prometheus` main port, `/api/healthz` 200 `{"status":"UP"}`, no `components`/`details` | E2E (`@SpringBootTest` RANDOM_PORT, `@LocalManagementPort`, props `management.server.port=0`, `management.prometheus.metrics.export.enabled=true`) | `./gradlew test --tests '*ActuatorHealthE2ETest'` (rewrite existing test; today it asserts `/actuator/health` on the main port) | rewrite (Wave 0) |
| Business counters | each counter increments at its hook (conflict, ownership denial, bad credentials, publish failure, DLT) | integration via real wiring (`@Autowired MeterRegistry`, no mocks, rule 4) | extend `ActivityLogDeadLetterE2ETest` for DLT; new tests per arm | new |
| Trace continuity | root -> send -> receive in one trace; `traceparent` header on the record | `AbstractKafkaContainerTest` + `@AutoConfigureObservability` + `InMemorySpanExporter` `SimpleSpanProcessor` bean + `opentelemetry-sdk-testing` | `./gradlew test --tests '*Trace*'` (tagged `kafka`, not in fastTest) | new (scratch proof exists, deleted) |
| JSON logs with traceId | request log line contains top-level `traceId` | `OutputCaptureExtension` + `logging.structured.format.console=ecs` | unit/E2E | new |
| Probes/port | probes on `management` port | `scripts/verify-k8s-manifests.sh` (schema) + manual rollout on nonprod | `kubectl -n kanban-nonprod rollout status deploy/app` | manual |
| Alert/Alertmanager config | syntax valid | static | `docker run --rm -v $PWD/<dir>:/c:ro --entrypoint amtool prom/alertmanager:v0.34.1 check-config /c/alertmanager.yaml`; rules: extract `.spec` with `yq` and `promtool check rules` (prom/prometheus:v3.14.0) | scriptable |
| Rule behaviour | expressions fire on synthetic series | `promtool test rules` unit tests | add `tests/*.yaml` beside rules (optional CI step) | new (optional) |
| Each alert end to end | time-to-notification | live, once on purpose | see measurement table | manual with operator |
| Watchdog dead-man | pings stop -> Telegram | live | scale `alertmanager` to 0 / delete the Secret key; record minutes to healthchecks.io Telegram message (free-tier research expects 10-15 min) | manual |
| Backup | dump uploaded encrypted, alert fires when stale | live + static | `kubectl -n kanban-data create job --from=cronjob/postgres-backup t1`; decrypt a downloaded object with the private key (human) and `pg_restore -l`; temporary rule with a short threshold to prove the alert path | manual |
| k8s invariants | MEASURED comments, no Secret, roots covered | static | the three scripts above | existing |

### Live measurement table (fire each alert once, record t(Telegram message) - t(fault))
| Alert | Fault injection | Note |
|-------|-----------------|------|
| `AppTargetDown` / `AppDeploymentUnavailable` | scale `app` to 0 in nonprod (and once in prod) | `for:` dominates (2m / 8m) |
| `App5xxRatioHigh` | temporarily point the app at a dead Postgres host or hit an endpoint that 500s | nonprod |
| `ActivityDeadLetterWritten` | publish a poison record to `kanban.activity` (exists in `ActivityLogAvroDeadLetterE2ETest` shape) | proves the counter + route |
| `ActivityConsumerLagRising` | pause the listener container (ResetService pauses containers; reuse that mechanism) | nonprod |
| `PostgresBackupMissing` | cannot wait 26h: temporarily apply a rule copy with a 1m threshold on a scratch CronJob | document the substitution honestly |
| `CertificateExpiringSoon`, disk | temporarily lower the threshold in a scratch rule | same |
| `Watchdog` dead-man | stop Alertmanager | measures the 10-15 min figure on this cluster |
| `uptime-check.yml` | `workflow_dispatch` with a bad URL | Telegram step + heartbeat |
| Flux failure | push a bad image tag to nonprod | `wait` timeout path |
Measure with timestamps: Prometheus `ALERTS` `activeAt`, `alertmanager_notifications_total`/`alertmanager_notification_latency_seconds` (Alertmanager self-metrics), and the Telegram message time in the client.

### Sampling Rate
- **Per task commit:** the scoped test class plus `./gradlew spotlessApply` (pre-commit runs fastTest ~4 min); for k8s files run the three static scripts.
- **Per wave merge:** `./gradlew test` (full, includes kafka tests) and all static checks.
- **Phase gate:** full suite green and `scripts/verify-public-dashboards.py` green before `/gsd-verify-work`; live measurement table filled.

### Wave 0 Gaps
- [ ] Rewrite `ActuatorHealthE2ETest` for `/api/healthz` + management-port assertions (old assertions target a path that stops existing).
- [ ] Test support for management port + trace assertions (`opentelemetry-sdk-testing`, a reusable `@AutoConfigureObservability` base or annotation).
- [ ] Regenerate `gradle/verification-metadata.xml` after the dependency change.
- [ ] Decide and script the `yq`+`promtool`/`amtool` check (optionally add to `invariant-checks.yml`).
- [ ] Live baseline: `free -m`, `max_over_time(container_memory_working_set_bytes[3d])`, `/api/v1/targets` labels, `kubectl get pods -A` restart counts (operator task).

## Security Domain

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (auth-failure telemetry only) | counter + JSON event; keep the byte-identical 401 (do not add a distinguishable status, see `SecurityConfiguration` Javadoc) |
| V3 Session Management | no change | - |
| V4 Access Control | yes | ownership-denial counter/log at `handleAppAccessDeniedException`; management port reachable only inside the cluster |
| V5 Input Validation | low | do not log raw request bodies, emails or passwords; JSON logging escapes control characters |
| V6 Cryptography | yes | `age` for dumps (never hand-rolled); private key off-cluster; B2 TLS only |
| V7 Error handling and logging | yes | structured UTC JSON logs, security events; traceId only, no PII in span attributes |
| V8/V14 Data protection and config | yes | Secrets created on the VPS only (I8), tokens via mounted files, write-only bucket key, Object Lock, no secret in manifests (gitleaks) |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Metrics endpoint exposed publicly | Information disclosure | separate port, not in Service/IngressRoute, `EndpointRequest` chain; optional NetworkPolicy allowing only `monitoring` |
| Health details leak | Information disclosure | `show-details=never`; the management `/actuator/health` body lists `groups` (verified), acceptable only on the unrouted port |
| Compromised VM key deletes backups | Tampering | write-only key, Object Lock, versioning; remember `hide` is allowed by `writeFiles` |
| Telegram/healthchecks/B2 secrets in git | Information disclosure | mounted Secrets (`*_file` options), `.gitleaks.toml` stays strict |
| Backup contains all user data | Information disclosure | age encryption before upload; public key only on the VM |
| Log/trace injection via user input | Tampering/Repudiation | structured logging; avoid user strings as span names |
| 5xx alert poisoned by unauthenticated scanners | DoS of alerting | remove `ACTUATOR_HEALTH` from main-port `permitAll`; unauthenticated unknown paths hit 401 before the 500 catch-all |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `grafana/tempo` chart 1.x (Tempo 2.x, ingester + compactor) | `grafana-community/tempo` 3.x, monolithic Tempo 3 (no Kafka) | chart 3.0.0, 2026-09-11 | New install should start on 3.2.0 |
| Spring Boot custom JSON logging libs (logstash-encoder) | `logging.structured.format.console=ecs|logstash|gelf` | Boot 3.4 | No dependency |
| S3 lifecycle via console/native API only on B2 | `PutBucketLifecycleConfiguration` supported | 2025-12-11 | D-13's "where the S3 API supports it" resolves to yes |
| AWS CLI MD5/no checksum | CRC64NVME default | aws-cli 2.23.0 (2025-01) | B2 initially rejected it; fixed provider-side July 2025 (secondary source) |

**Deprecated/outdated:** `grafana/tempo` chart (flagged deprecated), `gautamkrishnar/keepalive-workflow` on GitHub (blocked), `spring.kafka.template.observation-enabled` for this app (bypassed).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Alertmanager, Tempo, OTel-in-app, aws-cli job memory estimates | Finding 7 | Wrong limits cause OOM; mitigated by mandatory ladder |
| A2 | `aws s3api put-object` works with a B2 single-bucket write-only key into an Object Lock bucket, with the EU Central endpoint/region string `eu-central-003` | Finding 5 | First upload fails; found by the human smoke test |
| A3 | `writeFiles` also permits `b2_hide_file` effects the user did not expect from "write-only" | Finding 5 | Overstates what D-12 protects; mitigated by Object Lock |
| A4 | A missing `secrets:` Secret leaves the Alertmanager pod `ContainerCreating` | Pitfall 7 | Ordering of human steps |
| A5 | `OTEL_RESOURCE_ATTRIBUTES` is honoured by Boot's OTel resource config | Finding 4 | No env tag on traces |
| A6 | `PUT .../workflows/.../enable` resets the 60-day timer; Flux `info` event volume; Kustomization timeout default = interval | Finding 6, 8 | Keepalive ineffective / noisy deploy messages; heartbeat check is the real control |
| A7 | PodMonitor series carry `container="app"`, `namespace`, `pod` as in the candidate rules | Code Examples | `AppTargetDown` never fires; verify with `/api/v1/targets` |
| A8 | No stock image has pg_dump 16 + age + S3 client | Finding 5 | A simpler image may exist |
| A9 | `kubernetesSystem` group's `KubeAPIDown`/`KubeletDown` have live targets on this k3s | Finding 3 | False alerts; add `defaultRules.disabled` |
| A10 | Trace-to-logs custom query variable `${__trace.traceId}`; Loki labels used in the link | Finding 4 | Link opens empty logs; fix in Grafana UI |
| A11 | Boot can drop actuator/security spans via predicates if volume is a problem | Trade-offs | Trace volume at 100% sampling |
| A12 | Chart 3.2.0 (one day old) installs cleanly on k3s with `local-path` | Standard Stack | Fall back to 3.1.0 plus explicit `backendWorker.compaction.block_retention` |

## Open Questions

1. **Object Lock mode and weekly protection (decision with a costly branch).**
   - What we know: governance is overridable with `bypassGovernance`; compliance cannot be undone by anyone, and Object Lock cannot be turned off once enabled; default retention is bucket-wide; the single-bucket web-UI write-only key cannot set per-object retention.
   - What's unclear: whether the user wants weeklies locked 28 days (needs a key created via API with `writeFileRetentions`) or accepts 7-day locks on weeklies.
   - Recommendation: governance mode, default retention 7 days; ask the user before choosing compliance because it is irreversible.
2. **New image pipeline or runtime `apk add`?** Recommendation: pinned custom image; confirm the planner accepts a small CI job and a manual tag bump.
3. **Does the user accept `/api/healthz` as the new public health path?** Boot cannot keep `/api/actuator/health` once the port is separated; the uptime workflow, probes and any external monitor change.
4. **Prod request for the app after OTel + registry** and **Tempo/Alertmanager limits**: require the ladder (operator-driven, as in prior phases).
5. **Redpanda broker-side lag metric** for a second lag signal: unverified metric names.

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `build.gradle`, `src/main/resources/application.properties`, `application-test.properties`, `KafkaConsumerConfig`, `KafkaEventPublisher`, `AsyncConfig`, `GlobalExceptionHandler`, `SecurityConfiguration`, `AuthenticationController`, `OwnershipVerifierService`, `TaskService`, controllers, `k8s/**` (app, monitoring, data, flux-system, overlays), `.github/workflows/uptime-check.yml`, `scripts/verify-k8s-invariants.py`, `docs/INFRA_*.md`, `docs/CODE_STYLE.md`.
- `spring-boot-dependencies-3.5.16.pom`, `spring-boot-actuator-autoconfigure-3.5.16` jar (`spring-configuration-metadata.json`) and sources, spring-kafka 3.3.16 sources (`KafkaTemplate.observeSend`, `KafkaRecordSenderContext`), micrometer-tracing v1.5.12 `PropagatingSenderTracingObservationHandler` source.
- kube-prometheus-stack tag `kube-prometheus-stack-91.5.2`: `values.yaml`, `Chart.yaml`, `templates/prometheus/prometheus.yaml`, `templates/alertmanager/secret.yaml`, `templates/prometheus/rules-1.14/*.yaml`.
- Alertmanager v0.34.1 `notify/telegram/telegram.go`, `notify/webhook/webhook.go` (via `14-FREE-TIER-RESEARCH.md` and re-read); `amtool check-config` run.
- `grafana-community/helm-charts` tempo chart 3.2.0 (`Chart.yaml`, `values.yaml`, `README.md` upgrade notes, templates) and both Helm indexes.
- Context7: `/websites/spring_io_spring-boot_3_5`, `/grafana/tempo`, `/websites/grafana_grafana`, `/websites/grafana_alloy`.
- Scratch-worktree experiments (removed afterwards; worktree and branch deleted, `git status` clean): management-port security matrix, ECS log shape, metric names, Kafka trace propagation with and without each fix.

### Secondary (MEDIUM confidence)
- docs.spring.io Spring Boot 3.5 reference (tracing, observability, endpoints, logging).
- Backblaze docs: application keys, application-key capabilities, Object Lock, lifecycle rules, S3 lifecycle API page and blog posts (2025-12-12, 2026-02-05), "digging deeper into object lock" (2023), call-the-s3-compatible-api, data regions.
- Flux docs: providers page (telegram, alertmanager types).
- aws/aws-cli#9214, boto/s3transfer#327 (checksum change); Grafana provisioning docs.
- kube-state-metrics docs (CronJob metrics) and kube-state-metrics 8.5.0 chart `collectors`.

### Tertiary (LOW confidence)
- docs.polydoc.tech (B2 EU Central region string), nocodb issue comment (Backblaze statement about July 2025 checksum support).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH for Java deps and chart versions (resolved/read); MEDIUM for Tempo 3.2.0 (one day old, memory unknown)
- Architecture: HIGH for the app side (exercised), MEDIUM for cluster side (docs/chart source, not live)
- Pitfalls: HIGH where marked verified; MEDIUM for B2 and Flux behaviours (documentation only)

**Research date:** 2026-10-15
**Valid until:** 2026-11-14 for the app side; 2026-10-22 for Tempo chart (fast-moving, 3.2.0 published 2026-10-14) and B2 doc claims
