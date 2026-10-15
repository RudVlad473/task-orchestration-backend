# Phase 14: Production ownership - Pattern Map

**Mapped:** 2026-10-15
**Files analyzed:** 29 (new or modified)
**Analogs found:** 26 / 29

All analog paths below were confirmed with `git ls-files`. Line numbers come from reads in this session.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `build.gradle` (+deps, comment at ~149) | config | build | itself (actuator dep block, `:96-119` verification cmd, `:149-153`) | exact |
| `gradle/verification-metadata.xml` | config | regenerate | itself, command at `build.gradle:96-119` | exact |
| `src/main/resources/application.properties` | config | config | itself, `# === actuator ===` block lines 19-27 | exact |
| `src/main/.../security/SecurityConfiguration.java` (+management chain, `/healthz`) | config | request-response | itself (`securityFilterChain`, lines 57-110) | exact |
| `src/main/.../constant/ApiPaths.java` (+`HEALTHZ`) | utility | constant | itself (`ACTUATOR_HEALTH`, line 38) | exact |
| `src/main/.../config/KafkaConsumerConfig.java` (template observation, DLT counter) | config | event-driven | itself (`kafkaTemplate` ~81-83, `recovered` ~175-185) | exact |
| `src/main/.../config/AsyncConfig.java` (TaskDecorator) | config | event-driven | itself (lines 16-23) | exact |
| `src/main/.../config/KafkaEventPublisher.java` (publish-failure counter) | service | event-driven | itself (`whenComplete`, lines 44-65) | exact |
| `src/main/.../handler/GlobalExceptionHandler.java` (conflict, ownership, auth counters, WARN log) | controller-advice | request-response | itself (arms at 150-156, 185-191, 89-95) | exact |
| `src/main/.../security/ProblemDetailAuthenticationEntryPoint.java` (unauthenticated counter) | middleware | request-response | itself (`commence`) | exact |
| `src/main/.../service/TaskService.java` (created/moved counters, optional Observation) | service | CRUD | itself (`save` 57-58, `moveToColumn` 172-173) | exact |
| `src/main/.../service/BoardService.java` (optional Observation on `findFullById`) | service | CRUD | `TaskService` | role-match |
| `src/test/.../security/ActuatorHealthE2ETest.java` (rewrite) | test | request-response | itself | exact |
| new metrics tests (counters per hook) | test | request-response | `activitylog/ActivityLogDeadLetterE2ETest.java` (+ `support/fixtures/AbstractAppTest.java`) | role-match |
| new trace-continuity test | test | event-driven | `activitylog/ActivityLogDeadLetterE2ETest.java` on `support/containers/AbstractKafkaContainerTest.java` | role-match |
| `k8s/base/app/app.yaml` (mgmt port, probes) | config | k8s | itself (lines 62-63, 87-103, 114-118) | exact |
| `k8s/overlays/{prod,nonprod}/patches.yaml` (OTLP env, resources) | config | k8s | itself | exact |
| `k8s/monitoring/configs/podmonitor-app.yaml` (or append to `podmonitors.yaml`) | config | scrape | `k8s/monitoring/configs/podmonitors.yaml` (Redpanda) | exact |
| `k8s/monitoring/configs/prometheusrules.yaml` (new) | config | alerting | none in repo for PrometheusRule; use RESEARCH Code Examples | no analog |
| `k8s/monitoring/configs/kustomization.yaml` (+resources) | config | k8s | itself | exact |
| `k8s/monitoring/configs/datasources.yaml` (+Tempo, derivedFields) | config | k8s | itself | exact |
| `k8s/monitoring/controllers/kube-prometheus-stack.yaml` (alertmanager, defaultRules, ruleSelector) | config | k8s | itself (lines 38-41, 84-85) | exact |
| `k8s/monitoring/controllers/tempo.yaml` (new HelmRelease) | config | k8s | `k8s/monitoring/controllers/alloy.yaml` (or `loki.yaml`) | role-match |
| `k8s/monitoring/controllers/repositories.yaml` (+grafana-community) | config | k8s | itself | exact |
| `k8s/monitoring/controllers/kustomization.yaml` (+tempo.yaml) | config | k8s | itself | exact |
| `k8s/flux-system/notifications.yaml` (Provider+Alert) + `kustomization.yaml` | config | event-driven | `k8s/flux-system/apps-prod.yaml`, `kustomization.yaml` resource list | partial |
| `k8s/data/postgres/backup-cronjob.yaml` (new) + `kustomization.yaml` | config | batch | `k8s/data/postgres/postgres.yaml` (probes, securityContext) and `k8s/base/app/app.yaml` (securityContext, secretRef) | role-match |
| `k8s/data/postgres/networkpolicy.yaml` (+`kanban-data` peer) | config | k8s | itself | exact |
| `docker/backup/Dockerfile` (new, optional) | config | build | none (no Dockerfile verified in scope) | no analog |
| `.github/workflows/uptime-check.yml` | config | request-response | itself | exact |
| `docs/INFRA_RUNBOOK.md`, `docs/INFRA_ARCHITECTURE.md`, `docs/diagrams/physical/monitoring-nodes.mmd`, `production-host.mmd` (+ re-render, `render-manifest.tsv`) | docs | - | themselves; `docs/DIAGRAM_CONVENTIONS.md` | exact |

## Pattern Assignments

### `SecurityConfiguration.java` (config, request-response)

**Analog:** itself, `src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java`

Current permit list (lines 66-79): `SIGNIN, SIGNUP, SWAGGER..., ApiPaths.ACTUATOR_HEALTH` then `.permitAll()`; `auth.anyRequest().authenticated()`. Replace the `ACTUATOR_HEALTH` entry with a new `/healthz` constant and keep the context-path-relative comment style ("the container strips /api before request matchers run").

Add a second bean beside `securityFilterChain` (research Pattern 1, verified):
```java
@Bean
@Order(1)
public SecurityFilterChain managementChain(HttpSecurity http) throws Exception {
    http.securityMatcher(EndpointRequest.toAnyEndpoint());
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
    http.csrf(c -> c.disable());
    return http.build();
}
```
Method style to copy: `http.csrf(AbstractHttpConfigurer::disable)` (line 62). Comments must follow CODE_STYLE rule 14 (plain text, enforced by `scripts/verify-comments.py`). Add a Javadoc on why permitAll is safe (port not routed).

### `application.properties` (config)

**Analog:** itself, lines 19-27. Keep the existing allowlist comment and write the reason for widening to `health,prometheus` beside it (CONTEXT: "write the reason beside the existing comment"). Property block to add is in RESEARCH Pattern 2. Existing lines:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```
Check `src/main/resources/application-test.properties` for test overrides before changing.

### `KafkaConsumerConfig.java`, `AsyncConfig.java`, `KafkaEventPublisher.java` (config/service, event-driven)

**Analog:** themselves.

`KafkaConsumerConfig.kafkaTemplate` (current body `return new KafkaTemplate<>(kafkaProducerFactory);`, javadoc ahead of it explains the `@Primary` back-off): change to build the template, call `template.setObservationEnabled(true)`, return it. Extend the javadoc paragraph; do not add `spring.kafka.template.observation-enabled`.

DLT counter hook, existing `RetryListener.recovered` (lines ~175-185):
```java
@Override
public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
    log.error("Dead-lettering record to {} after exhausting retries: sourceTopic={} partition={} offset={} cause={}", ...);
}
```
Increment `kanban_activity_dlt_written_total` here; `failedDelivery` stays a documented no-op. Inject `MeterRegistry` into the config class bean method or listener via the neighbouring style.

`AsyncConfig.kafkaPublishExecutor` (lines 16-23): add `executor.setTaskDecorator(new ContextPropagatingTaskDecorator());` before `initialize()`.

`KafkaEventPublisher` uses field injection, the project convention:
```java
@Autowired private KafkaTemplate<String, Object> kafkaTemplate;
@Autowired private ActivityEventAvroMapper activityEventAvroMapper;
```
Add `@Autowired private MeterRegistry meterRegistry;` and increment `kanban_kafka_publish_failures_total` inside `if (ex != null) { log.error(...) }` (lines ~52-61).

### `GlobalExceptionHandler.java` (controller-advice, request-response)

**Analog:** itself. All arms share one shape; copy it and add the counter plus a WARN `addKeyValue` log without email or password.
```java
@ExceptionHandler(OptimisticLockingFailureException.class)
public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setProperty(ErrorCode.CODE_PROPERTY, ErrorCode.OPTIMISTIC_LOCK_CONFLICT.name());
    return ResponseEntity.status(problem.getStatus()).body(problem);
}
```
Counter sites: `handleOptimisticLockingFailure` (`kanban_optimistic_lock_conflicts_total`), `handleBadCredentialsException` (`kanban_auth_failures_total`), `handleAppAccessDeniedException` at about lines 89-95 (`kanban_ownership_denied_total`). Do not change the byte-identical 401 envelope. The catch-all `Exception.class` arm (lines 72-78) is why main-port `/api/actuator/health` becomes 500.

### `ProblemDetailAuthenticationEntryPoint.java` (middleware)

**Analog:** itself. Uses `@Component @RequiredArgsConstructor` with a `private final ObjectMapper`: here constructor injection is in use, so add `private final MeterRegistry` and increment an unauthenticated tag in `commence`. This class and `GlobalExceptionHandler` are two producers of the 401 envelope (class Javadoc says to check drift).

### `TaskService.java` (service, CRUD)

**Analog:** itself. Hooks at `save` (`@Transactional` / `public TaskResponseDTO save(SaveTaskRequestDTO dto, ColumnEntity column)`, ~57-58) and `moveToColumn` (~172-173, `sameColumn` already computed, use as tag). Field `@Autowired`, `jakarta.transaction.Transactional` per CLAUDE.md. For spans: inject `ObservationRegistry` and wrap with `Observation.createNotStarted(...)` (no new dependency) rather than `@Observed` (needs `spring-boot-starter-aop`). Check `LayeringArchTest` in `src/test/java/com/vrudenko/kanban_board/architecture/` before adding fields.

### `ActuatorHealthE2ETest.java` (test, rewrite)

**Analog:** itself, `src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java` (note: under `security/`, not `e2e/`).
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ActuatorHealthE2ETest extends AbstractAppE2ETest {
    private static final String HEALTH_PATH = "/actuator/health";
    @Nested class HealthCheckTest {
        @Test void shouldReturnOk_whenHealthCheckedWithoutSession() {
            var statusCode = given().when().get(HEALTH_PATH).then().extract().statusCode();
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK.value());
        }
```
Conventions: `shouldX_whenY` names, `// arrange & act` / `// assert` comments, `@Nested`, AssertJ `Assertions.assertThat`, RestAssured `given()`. Switch `HEALTH_PATH` to `/healthz`; add management-port cases using `@LocalManagementPort` with `management.server.port=0` and `management.prometheus.metrics.export.enabled=true` in `@SpringBootTest(properties=...)`. Keep no `@Tag("realSocket")` (fastTest). Keep the "no components / no details" and `/actuator/env` allowlist assertions.

### New counter tests (test)

**Analog:** `src/test/java/com/vrudenko/kanban_board/activitylog/ActivityLogDeadLetterE2ETest.java` (`@SpringBootTest`, `@Tag("kafka")`, extends `AbstractKafkaContainerTest`, `@Nested` classes, `Awaitility`, `@Autowired` repositories, real wiring, no mocks) and `support/fixtures/AbstractAppTest.java` (`@Autowired` services, `countQueries`). Autowire `MeterRegistry` and assert counter deltas. Extend the DLT E2E for `kanban_activity_dlt_written_total`. Trace test: same Kafka base plus `@AutoConfigureObservability` and an `InMemorySpanExporter`.

### `k8s/base/app/app.yaml` (config)

**Analog:** itself. Container port and probes (lines 62-63, 87-103):
```yaml
ports:
  - name: http
    containerPort: 8080
...
startupProbe:
  httpGet:
    path: /api/actuator/health
    port: http
  periodSeconds: 5
  failureThreshold: 36
```
Add `- name: management` / `containerPort: 9081`, move all three probes to `path: /actuator/health`, `port: management`. Do NOT add the port to the Service (lines 114-118, only `http`). Pitfall 1: change atomically with image rollout, nonprod first. Env additions (`OTLP_TRACES_ENDPOINT`, sampling) follow the existing `env:` list shape (`name`/`value`, strings quoted for numbers). `resources: {}` stays in base; limits live in the overlay patches with MEASURED comments.

### `k8s/monitoring/configs/podmonitor-app.yaml` (config, scrape)

**Analog:** `k8s/monitoring/configs/podmonitors.yaml` (whole file, 17 lines):
```yaml
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: redpanda
  namespace: monitoring
spec:
  namespaceSelector:
    matchNames: [kanban-prod, kanban-nonprod]   # written as a block list in the file
  selector:
    matchLabels:
      app.kubernetes.io/name: redpanda
  podMetricsEndpoints:
    - port: admin
      path: /public_metrics
      relabelings:
        - sourceLabels: [__meta_kubernetes_namespace]
          regex: kanban-(.+)
          targetLabel: env
```
Copy with `name: app`, `selector app.kubernetes.io/name: app`, `port: management`, `path: /actuator/prometheus`. The pod label `app.kubernetes.io/name: app` must exist on the pod template (Service selector uses it, so it does). Add to `k8s/monitoring/configs/kustomization.yaml` `resources:` (currently `datasources.yaml, podmonitors.yaml, ingressroute.yaml`; this Kustomization sets `namespace: monitoring`).

### `k8s/monitoring/controllers/kube-prometheus-stack.yaml` (config)

**Analog:** itself. Replace (lines 36-41):
```yaml
# Alerting is out of scope; Alertmanager ships in the chart but is not configured.
alertmanager:
  enabled: false
defaultRules:
  create: false
```
with the RESEARCH Finding 3 blocks, and update that stale comment. Add `ruleSelectorNilUsesHelmValues: false` beside lines 84-85 (`serviceMonitorSelectorNilUsesHelmValues: false`, `podMonitorSelectorNilUsesHelmValues: false`). MEASURED comment style to copy for `alertmanagerSpec.resources` (the Prometheus one, lines ~62-72):
```yaml
# MEASURED (restart ladder, 2026-09-27): passed 512Mi, ...; failed at 256Mi (dmesg
# OOMKilled). Adopted 640Mi limit (peak x1.5) / 384Mi request.
#
# Decisions:
# ...
# Falsifier: any OOMKilled on this container inside the 24h zero-restart window ...
resources:
  requests: { memory: 384Mi, cpu: 50m }
  limits: { memory: 640Mi }
```
I4 needs `MEASURED` (or `PROVISIONAL` outside `--no-provisional`) within 25 lines above each `memory:` line. Prometheus memory may need a re-ladder after new series.

### `k8s/monitoring/controllers/tempo.yaml` (new HelmRelease)

**Analog:** `k8s/monitoring/controllers/alloy.yaml`. Copy the skeleton:
```yaml
apiVersion: helm.toolkit.fluxcd.io/v2
kind: HelmRelease
metadata:
  name: alloy
  namespace: monitoring
spec:
  interval: 1h
  chart:
    spec:
      chart: alloy
      version: "1.12.1"
      sourceRef:
        kind: HelmRepository
        name: grafana
        namespace: flux-system
  install:
    crds: CreateReplace
  upgrade:
    crds: CreateReplace
  values:
    ...
    # MEASURED (restart ladder, 2026-09-27): passed 75Mi, failed at 40Mi (...). Adopted 75Mi limit / 40Mi request.
    resources:
      requests: { memory: 40Mi, cpu: 10m }
      limits: { memory: 75Mi }
```
Use `chart: tempo`, pinned `version: "3.2.0"`, `sourceRef.name: grafana-community`, header comment with a `Decisions:` section as in `alloy.yaml`/`kube-prometheus-stack.yaml`. Values skeleton in RESEARCH "Tempo HelmRelease values skeleton". Add `tempo.yaml` to `controllers/kustomization.yaml` (resources: `repositories.yaml, kube-prometheus-stack.yaml, loki.yaml, alloy.yaml, postgres-exporter.yaml`). Add the second HelmRepository to `repositories.yaml` by copying the existing document (`apiVersion source.toolkit.fluxcd.io/v1`, `namespace: flux-system`, `interval: 1h`, `url: https://grafana-community.github.io/helm-charts`), separated by `---`. Do not use Alloy for OTLP (at its measured floor).

### `k8s/monitoring/configs/datasources.yaml` (config)

**Analog:** itself. Keep the two literal uids (`PBFA97CFB590B2093` Prometheus, `P8E80F9AEF21F6940` Loki), extend the header "Decisions:" comment, add a Tempo entry with a new literal uid (url `http://tempo.monitoring.svc:3200`) and `jsonData.derivedFields` on Loki. Entry shape:
```yaml
- name: Loki
  uid: P8E80F9AEF21F6940
  type: loki
  access: proxy
  url: http://loki.monitoring.svc:3100
```
Gate: `python3 scripts/verify-public-dashboards.py` must still pass.

### `k8s/flux-system/notifications.yaml` (new) and `k8s/flux-system/kustomization.yaml`

**Analog:** `k8s/flux-system/kustomization.yaml` resource list (`gotk-components.yaml, gotk-sync.yaml, platform.yaml, data.yaml, apps-nonprod.yaml, cert-manager.yaml, edge.yaml, apps-prod.yaml, monitoring-controllers.yaml, monitoring.yaml, image-automation.yaml`) plus the patch note about generator-owned `gotk-components.yaml`. Add the file name to that list. Provider/Alert YAML is in RESEARCH "Flux notification". Existing Flux CR shape: `k8s/flux-system/monitoring.yaml` (`kustomize.toolkit.fluxcd.io/v1`, `namespace: flux-system`, `dependsOn`, `sourceRef GitRepository flux-system`). The `telegram-token` Secret is created on the VPS, never committed (I8).

### `k8s/data/postgres/backup-cronjob.yaml` (new) and `kustomization.yaml`

**Analog:** `k8s/data/postgres/postgres.yaml` for namespace/labels/Secret-ref style and `k8s/base/app/app.yaml` for the container security context:
```yaml
securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
```
`k8s/data/postgres/kustomization.yaml` sets `namespace: kanban-data` and lists `resources: postgres.yaml, networkpolicy.yaml`; append `backup-cronjob.yaml`. Use `secretKeyRef` for `PROD_DB_USER/PROD_DB_PASS/PROD_DB_NAME` from `postgres-init` (never `envFrom`, that Secret holds the superuser password). DB host `postgres.kanban-data.svc.cluster.local`. New Secret `postgres-backup` (VPS-created). Script shape: RESEARCH Finding 5. MEASURED comment above any `memory:` line (I4 applies; I3 skips CronJob templates but the ladder still applies). Pod template label `kanban-board/postgres-client: "true"`.

### `k8s/data/postgres/networkpolicy.yaml` (config)

**Analog:** itself (whole file). The `from:` list has two entries; the second shows the AND-combined namespace plus pod selector shape to copy:
```yaml
- namespaceSelector:
    matchLabels:
      kubernetes.io/metadata.name: monitoring
  podSelector:
    matchLabels:
      kanban-board/postgres-client: "true"
```
Add a third entry with `kubernetes.io/metadata.name: kanban-data` and the same podSelector, as a separate list item.

### `.github/workflows/uptime-check.yml` (config)

**Analog:** itself. Keep: `permissions: {}`, `env:` for every value (no `${{ }}` in `run`), `set -eu`, `HEALTH_URLS` folded scalar (lines 39-40), `UP_MARKER: '"status":"UP"'`. Changes: URLs to `/api/healthz`; cron from `'*/15 * * * *'` to offset minutes such as `7,22,37,52`; add a final `if: failure()` curl step to the Telegram API using `env:` secrets; on success ping a healthchecks.io heartbeat URL from a secret; rewrite the "Known holes" header items (b) and (c). `permissions` becomes `actions: write` only if the `enable` call is used.

### Docs and diagrams

**Analogs:** `docs/INFRA_RUNBOOK.md` (existing `pg_dump` commands ~404-434, ladder method ~574-600, memory table ~664-673), `docs/INFRA_ARCHITECTURE.md`, `docs/diagrams/physical/monitoring-nodes.mmd` and `production-host.mmd`, `docs/diagrams/process/activity-pipeline.mmd` (Kafka trace), `docs/diagrams/render-manifest.tsv`. Procedure: `docs/DIAGRAM_CONVENTIONS.md`, `scripts/render-diagrams.sh`, gate `scripts/verify-diagrams.py`. Update the Maintenance Note file list in the same change (memory: infra diagrams go stale silently). `docs/wiki/` copies are not authoritative.

## Shared Patterns

### Secrets never committed
**Apply to:** Alertmanager, Flux Provider, backup CronJob, uptime workflow. Secrets are created on the VPS (invariant I8 fails any rendered `kind: Secret`). Alertmanager reads them as mounted files (`bot_token_file`, `chat_id_file`, `url_file`); the workflow reads repo secrets only through `env:`. Gitleaks runs on every commit.

### MEASURED memory comment
**Source:** `k8s/monitoring/controllers/alloy.yaml` and `kube-prometheus-stack.yaml`. **Apply to:** Alertmanager, Tempo, backup CronJob, any overlay patch whose app request changes. Required by `scripts/verify-k8s-invariants.py` I4 within 25 lines above each `memory:` line.

### Static gates for k8s files
`bash scripts/verify-k8s-manifests.sh`, `python3 scripts/verify-k8s-invariants.py --no-provisional`, `python3 scripts/verify-public-dashboards.py`. Validate Alertmanager config with `amtool check-config` and rules with `promtool check rules` via docker, as in RESEARCH.

### Java conventions
Field `@Autowired`, `jakarta.transaction.Transactional`, plain-text comments (rule 14), `./gradlew spotlessApply`, tests `shouldX_whenY` with `@Nested`. Regenerate `gradle/verification-metadata.xml` after dependency changes (`build.gradle:96-119`).

### Flux layering
Third-party charts: HelmRelease under `k8s/monitoring/controllers/` (Kustomization `monitoring-controllers`); CRs depending on their CRDs (PodMonitor, PrometheusRule) go under `k8s/monitoring/configs/` (Kustomization `monitoring`, `dependsOn: monitoring-controllers`). Own workloads: Kustomize base plus overlays.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `k8s/monitoring/configs/prometheusrules.yaml` | config | alerting | No PrometheusRule exists in the repo; use RESEARCH "Candidate alert rules" (promtool-checked) and label or selector per Pitfall 6 |
| Backup image `docker/backup/Dockerfile` (optional) | config | build | No in-scope Dockerfile verified; RESEARCH Finding 5 gives the Alpine recipe. Search `.github/workflows/deploy.yml` for the existing image build job to extend |
| Alertmanager `config` block | config | alerting | Alertmanager is currently disabled; use RESEARCH Finding 3 (amtool-validated) |

## Metadata

**Analog search scope:** `src/main/java`, `src/test/java`, `k8s/`, `.github/workflows/`, `scripts/`, `docs/` (tracked listings only)
**Files read in full or in part:** about 20
**Not re-verified here:** `k8s/overlays/*/patches.yaml`, `k8s/data/postgres/postgres.yaml` and `docs/INFRA_RUNBOOK.md` line numbers are taken from RESEARCH.md, not re-read this session.
**Pattern extraction date:** 2026-10-15
