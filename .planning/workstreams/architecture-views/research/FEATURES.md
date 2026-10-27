# Feature Landscape: what a COMPLETE LikeC4 4+1 model contains for this system

**Domain:** Architecture documentation (Kruchten 4+1) of one Spring Boot / k3s / Flux system, in LikeC4
**Researched:** 2026-10-27
**Overall confidence:** HIGH for the inventory (every row was read in the repo this run), MEDIUM for the
significance ranking (a judgment, not a measurement), LOW for anything marked [U] below.

## How to read this file

- **Evidence tags.** `[R]` = I opened the cited file this run and the claim is in it. `[D]` = the claim is
  in a doc (INFRA_ARCHITECTURE.md, INFRA_RUNBOOK.md) and I did not re-check it against the live VM. `[U]` =
  could not be confirmed from the repo. Nothing below was checked against the running cluster.
- **Status vs the model** (docs/architecture/model/*.c4 as of commit be9db1c): `IN`, `PARTIAL`, `MISSING`,
  `WRONG` (present but contradicted by the repo).
- **Rung** = the provenance rung the item can reach with the machinery that exists today: `gen`
  (sync.mjs writes it), `chk` (hand-written plus a `metadata` field a script compares), `auth` (hand-written,
  unverified). "Reach" means feasible, not built.
- Complexity: Low = under an hour of `.c4` text, Med = needs a new element kind, a new check, or a render
  experiment, High = needs a spike first.
- I did not re-derive the prior review's gap list. Where an inventory row happens to be on it, the row says
  what the repo gives for it, which is the new information.

## Findings that change the plan (read first)

1. **The existing physical view has a wrong node.** `deployment.c4` puts Traefik in a namespace called
   `edge`. No such namespace exists: `k8s/platform/namespaces.yaml` declares four (`kanban-prod`,
   `kanban-nonprod`, `kanban-data`, `monitoring`), Traefik is configured in `kube-system`
   (`k8s/platform/traefik/helmchartconfig.yaml:19-20`), and cert-manager lands in `cert-manager`
   (`k8s/platform/cert-manager/cert-manager.yaml:38`). `edge` is a Flux Kustomization name
   (`k8s/flux-system/edge.yaml`), not a namespace. `sync.mjs` cannot catch this: its workload check only
   walks `k8s/base` and `k8s/data` for Deployment, StatefulSet and CronJob (`sync.mjs:217-234`). [R]
2. **The LikeC4 standard contradicts the milestone goal on the monitoring stack.** The Physical row of the
   viewpoint table says the view does not represent "the monitoring stack (excluded, as in
   `production-host.mmd`)" (DIAGRAM_CONVENTIONS.md, line 105). The Mermaid baseline drew it in a separate
   file, `monitoring-nodes.mmd`. So "complete" needs the row amended and a second `deployment view`, not a
   bigger `production`. [R]
3. **The standard's Scenarios row contradicts the error-status-split baseline.** Scenarios "do not represent
   internal calls", but `error-status-split.mmd` is about which internal layer (filter chain, MVC dispatch,
   exception handler) answers. Parity needs either client-visible discriminators only (401: no SQL; 403: the
   ownership SELECTs then refusal; 409: version check) or a decision to add filter-chain components. Decide
   before building those four views.
4. **A live data-safety mechanism is undocumented in the model and stale in the docs.**
   `k8s/data/postgres/backup-cronjob.yaml` runs nightly at 02:17 UTC, age-encrypts a `kanban_prod` dump and
   uploads it to a Backblaze B2 bucket (`postgres-backup.sh:8-9,65,76`). `INFRA_ARCHITECTURE.md:78-83` still
   says "there is still no backup". The model has the `backup` job but no bucket, no encryption and no
   edge to anything outside the cluster. [R]
5. **`k8s/data/postgres-bridge/` is orphaned.** No Flux Kustomization points at it (`data.yaml` path is
   `./k8s/data/postgres`; `rg postgres-bridge` over k8s/, scripts/ and INFRA_RUNBOOK.md finds only its own
   files and one comment). It describes a Compose-era Postgres on `172.17.0.1`. Do not model it. Live
   state not checked. [R]
6. **Other doc drift found while inventorying** (feeds the Maintenance Note, not the model):
   `INFRA_ARCHITECTURE.md:277-279` says `invariant-checks.yml` has five jobs, the file has seven
   (`pre-commit-secret-scan` and `likec4-model-drift` added). The same doc says the Postgres NetworkPolicy
   limits access to pods carrying the `postgres-client` label; for the two app namespaces it admits the whole
   namespace, and the label gates only the exporter and backup peers (`networkpolicy.yaml:19-42`). [R]
7. **`sync.mjs` checks are narrow by construction.** It checks tables, FKs, packages and imports (generated),
   the two topic names, and the workload set under `k8s/base` and `k8s/data`. Everything on the delivery path,
   the monitoring stack, cert-manager, Flux, the workflow set and the external systems has no check at all
   today. That decides which items can reach `chk` (see "Dependencies").

## Inventory by viewpoint

### Logical (viewpoint tag `#logical`, view `data`)

The standard scopes Logical to tables and FK ownership (DIAGRAM_CONVENTIONS.md:102). Within that scope the
view is already near complete.

| # | Must appear | Repo evidence | Status | Rung | Cx |
|---|---|---|---|---|---|
| L1 | 6 Flyway tables: users, boards, columns, tasks, subtasks, activity_log | `src/main/resources/db/migration/V1..V9`; `generated.c4` | IN | gen | - |
| L2 | 4 FKs: boards>users, columns>boards, tasks>columns, subtasks>tasks (the ownership chain `OwnershipVerifierService` walks) | V1__init.sql; ARCHITECTURE.md:12-14 | IN | gen | - |
| L3 | `spring_session`, `spring_session_attributes` and their one FK | `application.properties:187` (`initialize-schema=always`) | IN | auth | - |
| L4 | `activity_log` is a soft reference: `board_id varchar(255) NOT NULL` with no REFERENCES (V3:7); it joins boards only by value | V3__add_activity_log.sql:5-19 | MISSING (table drawn, no note that it has no FK) | auth (description text) | Low |
| L5 | The 14 domain events as Schema Registry subjects (RecordNameStrategy, one per event record) | `src/main/avro/*.avsc` (14 files), `event/*Event.java` (14 records), `application.properties:223` | MISSING | gen or chk (file list vs `.avsc`) | Med (new element kind `subject`, or one registry element with the count left out of the label) |
| L6 | Per-env databases `kanban_prod`, `kanban_nonprod`, the `monitoring` role | `k8s/data/postgres/init/01-create-databases-and-roles.sh`, `02-create-monitoring-role.sh` | PARTIAL (Postgres description mentions both DBs) | auth | Low |

Open question for the user, not decidable from the repo: whether "Logical complete" may stay table-only. A
layered-architecture overlay (controller > service > repository, enforced by `LayeringArchTest`) is covered
under Development (D3), which is where the standard puts packages.

### Process (viewpoint tag `#process`, view `process`)

| # | Must appear | Repo evidence | Status | Rung | Cx |
|---|---|---|---|---|---|
| P1 | Request thread / REST API | `controller/*`, `security/AuthenticationController.java` | IN (`kanban.api`) | auth | - |
| P2 | `kafkaPublishExecutor` pool, AFTER_COMMIT hand-off | `config/AsyncConfig.java:19`, `KafkaEventPublisher.java:102` | IN | auth | - |
| P3 | Consumer thread, group `activity-log`, DLT path | `activitylog/ActivityLogConsumer.java:51`, `KafkaConsumerConfig.java:186-203` | IN | auth (topics chk) | - |
| P4 | Topics `kanban.activity`, `kanban.activity.dlt` | `constant/KafkaTopics.java` | IN | chk | - |
| P5 | **Schema Registry** (Redpanda :8081) and its three relationships: publisher looks up (never registers), consumer looks up, init job registers | `application.properties:210,217,256`; `k8s/base/app/app.yaml:36-49`; `redpanda.yaml` port 8081; `activity-pipeline.mmd` already draws it | MISSING | chk (port and init command greppable) | Low |
| P6 | **`register-schemas` init job** (runs `AvroSchemaRegistrar` before every app start) | `app.yaml:34-57`, `config/AvroSchemaRegistrar.java` | MISSING | chk (initContainer name) | Low-Med (sync.mjs reads only `kind` and `metadata.name` today) |
| P7 | **Management server :9081** serving `/actuator/prometheus` and `/actuator/health`, unrouted; Prometheus scrapes it, kubelet probes it. Plus the public `/api/healthz` (db indicator only) on :8080 | `application.properties:30-36`; `app.yaml:64-67,92-108`; `podmonitors.yaml:24-44`; `SecurityConfiguration.java:74` | MISSING | chk (port, path) | Low |
| P8 | Log shipping and metric scraping as runtime edges: Alloy>Loki push, Prometheus>app/redpanda/node-exporter/postgres-exporter, postgres-exporter>Postgres, Grafana>Prometheus/Loki | `alloy.yaml:74-81`, `podmonitors.yaml`, `postgres-exporter.yaml`, `datasources.yaml` | MISSING (needs the elements first, see Physical) | auth | Med |
| P9 | Nonprod-only reset path: stops the listener containers, trims both topics with `AdminClient.deleteRecords`, truncates Postgres, restarts listeners; separate STATELESS filter chain | `service/ResetService.java:51-87`, `NonprodResetSecurityConfiguration.java:26-39`, `ApiPaths.java:41` | MISSING | auth | Low-Med (profile-specific, so a description plus a `nonprod` note, since Instances inherit all relationships) |
| P10 | Flux reconciliation loops with their intervals (GitRepository 1m, ImageRepository 5m, ImageUpdateAutomation 5m, Kustomization 10m) and `dependsOn` order | `k8s/flux-system/*.yaml` | MISSING | chk possible (interval numbers are in YAML) | Med |
| P11 | CronJob backup flow: PG > age > B2 | `postgres-backup.sh`, `backup-cronjob.yaml` | PARTIAL (`backup`>`postgres` only) | auth | Low |

Noise for Process: the HikariCP pool (`application.properties:156`), Tomcat internals, the three Spring
Security filter chains as separate elements (one description line on `kanban.api` is enough), `NegativeLookupCache`
(`config/NegativeLookupCache.java`, a send-path optimisation, not a process boundary).

### Development (viewpoint tag `#development`, view `development`)

| # | Must appear | Repo evidence | Status | Rung | Cx |
|---|---|---|---|---|---|
| D1 | 14 top-level packages and 38 import edges | `src/main/java/com/vrudenko/kanban_board/*`; `generated.c4` | IN | gen | - |
| D2 | Cycle report (one SCC of 8: base, config, dto, entity, mapper, repository, security, service), printed not gated | `sync.mjs:171-210`; DIAGRAM_CONVENTIONS.md V5 | IN (outside the model, in the check output) | gen | - |
| D3 | A layer overlay: controller > service > repository enforced by ArchUnit, with the cycle visible as a group | `src/test/.../architecture/LayeringArchTest.java:46-101`; V5 says "collapsing to layers is possible, not a result" | MISSING | auth | Med (readability of 38 edges at 4475 x 2636 px is itself unmeasured) |
| D4 | Source sets that produce code: `src/main/avro` (14 `.avsc`) > Gradle Avro plugin > SpecificRecord; MapStruct mappers; Flyway migrations as a deployed artifact | `build.gradle:20` (Avro plugin), `mapper/*`, `db/migration` | MISSING | chk (file counts) or auth | Med |
| D5 | The pipeline that builds and gates a commit as workflow elements: the 9 files in `.github/workflows/` | `deploy.yml`, `backup-image.yml`, `uptime-check.yml`, `verify-rate-limit.yml`, `invariant-checks.yml`, `secret-scan.yml`, `security-scan.yml`, `api-fuzz.yml`, `dependency-submission.yml` | MISSING | chk (workflow file set vs model, same shape as the workload check) | Low-Med |
| D6 | Local gate: `.githooks/pre-commit` (gitleaks, comment lint, instruction budget, spotless, fastTest, likec4 check) | `.githooks/pre-commit`; `scripts/pre-commit-likec4-selftest.py` | MISSING | auth | Low |
| D7 | Image build: Dockerfile (gradle:8.7-jdk21 > eclipse-temurin:21-jre-jammy), tag scheme `main-<run>-<sha7>`, 3 Docker Hub repos (`kanban-board-backend`, `-nonprod`, `kanban-postgres-backup`) | `Dockerfile:1,9,14`; `deploy.yml:24-29,114-116`; `backup-image.yml:28-30` | MISSING | chk (repo names appear in `deploy.yml` env and `image-automation.yaml`) | Low |

Noise for Development: the test package tree (ARCHITECTURE.md "Testing" owns it), per-class structure, third
party libraries (the standard already excludes them), every Gradle task.

### Physical (viewpoint tag `#physical`, views `production` plus two more)

Existing: Netcup server (arch in title), k3s cluster, namespaces `edge` (WRONG, finding 1), `kanban-prod`,
`kanban-nonprod`, `kanban-data`; Instances of traefik, kanban, redpanda, postgres, backup.

| # | Must appear | Repo evidence | Status | Rung | Cx |
|---|---|---|---|---|---|
| H1 | Server node, x86_64, single k3s node | `infra/vm/k3s/config.yaml`, `deploy.yml:128-129` (amd64 only) | IN | auth | - |
| H2 | **Fix `edge` namespace**: Traefik and `svclb-traefik` (hostPort 80/443, the only Pod with a hostPort) belong in `kube-system` | `helmchartconfig.yaml:19-20`; `INFRA_ARCHITECTURE.md:51-58` [D] | WRONG | chk (HelmChartConfig name and namespace) | Low |
| H3 | Namespaces `cert-manager`, `flux-system`, `monitoring` | `cert-manager.yaml:38`, `gotk-sync.yaml`, `namespaces.yaml:19-22` | MISSING | chk (namespace list from `namespaces.yaml` plus Helm `targetNamespace`) | Low |
| H4 | cert-manager (HelmRelease), 2 ClusterIssuers (staging, production; HTTP-01 via ingressClass traefik), 3 Certificates (prod, nonprod, monitoring) | `platform/cert-manager/cert-manager.yaml`, `platform/edge/clusterissuers.yaml`, `overlays/*/certificate.yaml`, `platform/edge/certificate-monitoring.yaml` | MISSING | chk (HelmRelease and Certificate names) | Low-Med |
| H5 | Flux: 6 controllers (source, kustomize, helm, notification, image-reflector, image-automation), the GitRepository, 2 ImageRepository, 2 ImagePolicy, 1 ImageUpdateAutomation, 9 Kustomizations | `k8s/flux-system/controller-resources.yaml`, `image-automation.yaml`, `gotk-sync.yaml`, `apps-*.yaml`, `data.yaml`, `edge.yaml`, `cert-manager.yaml`, `platform.yaml`, `monitoring*.yaml` | MISSING | chk (Kustomization file set) | Med (group the six controllers as one element; model Kustomizations as one element with the dependency chain in text, not 9 elements) |
| H6 | Monitoring stack in `monitoring`: Prometheus + Operator, Grafana, kube-state-metrics, node-exporter (DaemonSet), Loki (SingleBinary), Alloy (DaemonSet), postgres-exporter; 2 PodMonitors; 2 public dashboards | `controllers/{kube-prometheus-stack,loki,alloy,postgres-exporter}.yaml`, `configs/podmonitors.yaml`, `README.md:52-53` | MISSING | chk (HelmRelease names; needs a new walk, since the workload check skips `k8s/monitoring`) | Med |
| H7 | Edge routing objects: 6 IngressRoutes (prod 2, nonprod 2, monitoring 2), 6 Middlewares (prod auth-rate-limit, general-rate-limit, redirect-https; nonprod redirect-https; monitoring grafana-login-rate-limit, redirect-https), rate limit keyed on direct IP | `overlays/*/ingressroute.yaml`, `monitoring/configs/ingressroute.yaml`, `verify-rate-limit.yml:5-14` | MISSING | chk | Med |
| H8 | Postgres NetworkPolicy `postgres-ingress` (the `[4]` boundary) | `k8s/data/postgres/networkpolicy.yaml` | MISSING | chk | Low |
| H9 | State markers: which things hold state (Postgres PVC, Redpanda PVC per env, Prometheus, Loki, Grafana PVCs, local-path) | `postgres.yaml:126`, `redpanda.yaml:106-112`, `kube-prometheus-stack.yaml:75-82`, `loki.yaml:59-61` | MISSING | auth; sizes left out of labels | Low |
| H10 | Host-level items outside Kubernetes, installed by hand per `infra/vm/README.md`: `KANBAN-INGRESS` (mangle PREROUTING, IPv4 only), sshd :22 with `PermitOpen 10.43.54.32:5432`, k3s itself with `secrets-encryption` | `infra/vm/k3s-host-firewall.sh`/`.service`, `infra/vm/sshd/kanban-ci-tunnel.conf`, `infra/vm/k3s/config.yaml:3` | PARTIAL (one sentence in the server description) | auth | Med; the inbound-packet-path scenario needs these as addressable elements, which no current spec kind fits cleanly. Needs a spike |
| H11 | External systems: GitHub (repo, Actions runner ubuntu-latest x86_64, Environments production/staging), Docker Hub, Let's Encrypt (staging and production ACME), Backblaze B2 bucket, DuckDNS (3 hostnames), Netcup Cloud Firewall (`[1]`, not in this repo, ruleset unverifiable) | `deploy.yml`, `clusterissuers.yaml:13,25`, `backup-cronjob.yaml`/`postgres-backup.sh`, `ingressroute.yaml` hosts, `INFRA_ARCHITECTURE.md:85-90` [D] | MISSING | auth (no check can see them) | Low |
| H12 | Numbered trust boundaries `[1]`-`[5]` (firewall, KANBAN-INGRESS, Traefik edge ETP Local, NetworkPolicy, CI SSH forward) | `INFRA_ARCHITECTURE.md:85-99` [D] | MISSING | auth | Low |
| H13 | The Mermaid delivery-nodes picture as its own physical view: GitHub, runner, Docker Hub, Flux, app, Postgres, with the `[5]` SSH forward | `delivery-nodes.mmd` | MISSING | auth | Low once H5, H11 exist |
| H14 | Planned alert "no successful backup in 26 hours" (`planned` tag exists in spec.c4, used nowhere) | `INFRA_RUNBOOK.md:463-464` [D] says not live as of 2026-10-26; `kube-prometheus-stack.yaml` has Alertmanager disabled | MISSING | auth, tagged `planned` | Low |

Do not model in Physical: Pods and replicas (the standard excludes them), Secrets, memory limits, PVC sizes,
`postgres-bridge`, cAdvisor (the only hits are a dashboard ConfigMap in `monitoring/configs/kustomization.yaml:22-24`;
no cAdvisor workload exists in the manifests), Docker, Caddy, Compose, `DOCKER-USER`, Promtail.

### Scenarios (viewpoint tag `#scenarios`)

See the next section. Existing: `signin-ok`, `signin-bad-credentials`, `signin-ceiling`.

## Which scenarios are the "+1" and which are noise

**Test used** (from Kruchten's 1995 paper as I remember it: scenarios are a few important use cases chosen
for criticality or risk, each tracing a path through the other four views to show they fit together. MEDIUM,
from memory, not re-fetched). A scenario is architecturally significant here if all hold:

- (a) it crosses at least two of the other four viewpoints' elements in a way no single other view shows end to end;
- (b) the repo records a design decision or failure mode on it (an ADR-like comment, a doc section, a test, an
  invariant script), so the trace explains something a reader would otherwise have to dig for;
- (c) it is not the same trace as another scenario plus one extra arrow.

| ID | Scenario (outcome) | In model | Significant? | Views crossed | Repo anchor | Cx |
|---|---|---|---|---|---|---|
| S1 | signin ok, bad credentials (unknown email), ceiling | IN (3) | Yes | L, P, Ph | `AUTH_FLOWS.md:79-141`; two-session ceiling | built |
| S2 | signup 201 | MISSING | Yes | L, P | `AUTH_FLOWS.md:23-77` | Low |
| S3 | signup lost race on unique email: 409 `DATA_INTEGRITY_VIOLATION` | MISSING | Yes (DB constraint as backstop to the checked guard) | L, P | `AUTH_FLOWS.md:44,57-58`; `uk_users_email` V1 | Low |
| S4 | signup then auto-signin fails: compensating DELETE, 401 | MISSING | Yes, but flag: `AUTH_FLOWS.md:67` says "No test drives this arm", so the trace is authored from code only | L, P | `AUTH_FLOWS.md:64-68` | Low |
| S5 | 401 / 403 / 400 / 409 (four views, one per outcome) | MISSING | Yes; ARCHITECTURE.md:119 calls it "the single most important behavior" of phase 07.1 | P, L | `error-status-split.mmd`, `GlobalExceptionHandler`, `ProblemDetailAuthenticationEntryPoint` | Low each; blocked on finding 3 |
| S6 | push to main > tests > image push, and Flyway verify over the SSH forward | MISSING | Yes (gate ordering, tag scheme `main-N-sha7`, `[5]` boundary) | D, Ph | `push-to-image.mmd`, `deploy.yml` | Med: `par` in a dynamic view is untested, so expect two views or a serial trace |
| S7 | image tag > Flux > rollout, including `register-schemas` and the `k8s/**` path-ignore loop guard | MISSING | Yes (the only place the loop-guard decision is visible) | D, Ph, P | `image-to-rollout.mmd`, `deploy.yml:10-22`, `image-automation.yaml` | Med |
| S8 | request enters the host: allowed path (firewall > KANBAN-INGRESS > hostPort DNAT > svclb > Traefik > Middleware > Service > Pod) | MISSING | Yes | Ph, P | `inbound-packet-path.mmd`, `ingress-to-pod.mmd`, `INFRA_ARCHITECTURE.md:195-262` | Med-High, blocked on H10 |
| S9 | same entry, outcome 429 (rate limit exceeded on `/api/signin`, `/api/signup`) | MISSING | Yes (limiter key is the unforgeable peer IP, a decision recorded in `verify-rate-limit.yml:5-14`) | Ph, P | `overlays/prod/ingressroute.yaml:15-40` | Low once S8 exists |
| S10 | same entry, outcome DROP at KANBAN-INGRESS (new TCP to a port other than 22/80/443); the client sees a timeout | MISSING | Yes (it is the control the INFRA doc spends 40 lines proving) | Ph | `k3s-host-firewall.sh`, `INFRA_ARCHITECTURE.md:223-262` | Low once S8 exists. Counter attribution after Docker's removal is unproven per the doc [D] |
| S11 | mutation to activity_log row (e.g. `PATCH /tasks/{id}/move` 200, then AFTER_COMMIT, Avro send, consume, INSERT) | MISSING; only the static `process` view | Yes, and the most important one: it is the system's defining flow | L, P, Ph | `ARCHITECTURE.md:246-255`, `activity-pipeline.mmd` | Low: every element already exists |
| S12 | broker down: mutation still 200, failure logged, never swallowed (D-01) | MISSING | Yes (the decision that justifies the executor) | P | `ARCHITECTURE.md:259-264`, `application.properties:229-231` | Low |
| S13 | poison message: 3 retries, then `kanban.activity.dlt`, bytes intact | MISSING | Yes | P | `ARCHITECTURE.md:281-284`, `KafkaConsumerConfig.java:196-203` | Low |
| S14 | nonprod reset | MISSING | Yes, but nonprod-only | P, Ph, L | `ResetService.java:27-87` | Low-Med |
| S15 | nightly backup: CronJob > PG > age > B2 | MISSING | Yes (only durability path; the docs still deny it exists) | Ph, L | `backup-cronjob.yaml:1-21` | Low-Med |
| S16 | certificate issue and renewal over HTTP-01 through Traefik, with the ACME path excluded from the redirect (invariant I10) | MISSING | Yes (a quiet failure mode a CI invariant guards) | Ph | `overlays/prod/ingressroute.yaml:87-91`, `clusterissuers.yaml` | Med |
| S17 | uptime probe fails > one GitHub issue opened | MISSING | Yes (the only outage detector; Alertmanager is disabled) | D, Ph | `uptime-check.yml:1-34` | Low-Med |
| N1 | `GET /boards/{id}/activity` pagination | - | Noise (a plain paginated read, no Kafka) | - | `ARCHITECTURE.md:286-298` | skip |
| N2 | `GET /boards/{id}/full` single round trip | - | Noise | - | - | skip |
| N3 | CRUD for boards, columns, tasks, subtasks, theme, logout | - | Noise as scenarios; they are covered by S5 (error split) and S11 (event publish) | - | `controller/*` | skip |
| N4 | duplicate delivery absorbed by `existsByEventId` | - | Noise: S11 plus one `SELECT`; the claim lives in `ActivityLogRecorder` and its tests | - | `ARCHITECTURE.md:268-272` | skip |
| N5 | schema registration at pod start | - | Fold into S7; a standalone view repeats it | - | `app.yaml:34-57` | fold |
| N6 | Flyway verify over SSH | - | Fold into S6 | - | `deploy.yml:155-300` | fold |
| N7 | `verify-rate-limit` workflow run | - | Noise; it tests S9, it is not a runtime path of the product | - | `verify-rate-limit.yml` | skip |
| N8 | Grafana login, dashboard view, log search | - | Noise; the login rate limit belongs in H7 | - | `monitoring/configs/ingressroute.yaml` | skip |

## Table Stakes

Missing any of these and the claim "no important part of the system is missing" is false.

| Feature | Why expected | Cx | Dependencies |
|---|---|---|---|
| Fix the `edge` namespace and add `kube-system`, `cert-manager`, `flux-system`, `monitoring` (H2, H3) | The current physical view is wrong about where Traefik runs | Low | `deployment.c4`; a namespace check (new) |
| Delivery path elements: GitHub, runner, Docker Hub x3, Flux (H5, H11, D7) | Half the system's moving parts; the Mermaid baseline has two diagrams for it | Med | none existing |
| Monitoring stack: Prometheus, Grafana, Loki, Alloy, node-exporter, kube-state-metrics, postgres-exporter (H6) and its runtime edges (P8) | Whole namespace missing; `monitoring-nodes.mmd` parity | Med | amend the Physical row of the standard (finding 2); a HelmRelease walk in `sync.mjs` for `chk` |
| cert-manager, 2 ClusterIssuers, 3 Certificates, Let's Encrypt (H4) | The only reason HTTPS works; one scenario depends on it | Low-Med | Traefik element exists |
| Schema Registry, the init job and the three registry edges (P5, P6) | `activity-pipeline.mmd` already draws it; Avro governance is a headline of the project | Low | `redpanda`, `kanban.publisher`, `kanban.consumer` exist |
| Edge routing: IngressRoutes and rate-limit Middlewares (H7) | Rate limiting is the only abuse control at the edge | Med | Traefik element |
| Backup chain with B2 and age (H11, P11, S15) | Only data durability; docs currently wrong | Low-Med | `backup` job exists |
| Management port and scrape edge (P7) | Without it Prometheus has no source in the model | Low | H6 |
| Host-level filter `KANBAN-INGRESS`, sshd, Netcup Cloud Firewall as external (H10, H11) | The firewall/filter notes the milestone names; S8-S10 need them | Med-High (spike) | none |
| Scenarios S2, S5 (x4), S6, S7, S8, S9, S11, S15 | Parity with all seven Mermaid scenario diagrams, plus the two flows the Mermaid set never drew (S11, S15) | Low-Med each | their elements |
| Amend DIAGRAM_CONVENTIONS.md (Physical row, Scenarios row, view table) in the same change | The standard is the contract; two rows now contradict the goal | Low | none |

## Differentiators

Not required for "complete", but they turn the model into something the Mermaid set could not be.

| Feature | Value proposition | Cx | Notes |
|---|---|---|---|
| `chk` coverage for the new elements: extend `sync.mjs` to walk `k8s/platform` and `k8s/monitoring` for HelmRelease, HelmChartConfig, Certificate, ClusterIssuer; compare namespaces; compare the `.github/workflows` file set; compare Docker Hub repo names | Turns "complete" into a gate: a new workload or workflow with no element fails CI. Today only Deployment/StatefulSet/CronJob under `k8s/base` and `k8s/data` are covered | Med | Pre-commit trigger paths must grow (`k8s/platform`, `k8s/monitoring`, `.github/workflows`) or drift is CI-only, as already documented for Java imports. Add a drill per new rule, as the 12 existing drills do |
| S3, S4 signup race and rollback | Show a DB constraint as backstop and a compensating action | Low | S4 is untested code: label authored and say so in the view description |
| S12, S13, S14, S16, S17 | Each carries a recorded decision (D-01, byte-fidelity DLT, two-store reset, ACME path exclusion, no-Alertmanager alerting) | Low-Med | S14 and S16 are profile- or install-specific |
| S10 (DROP at KANBAN-INGRESS) | The one scenario with no client response, only a timeout | Low | State the unproven counter-attribution in the description |
| Layer overlay D3 | Makes the 8-package cycle and the ArchUnit-enforced direction visible | Med | Unmeasured readability; the 4475 x 2636 px development view is the problem it would solve |
| Per-event `subject` elements generated from `src/main/avro` (L5) | Gives Schema Registry real content and a drift check | Med | New element kind in `spec.c4` |
| State markers (H9) and trust boundaries (H12) | Answers "what loses data if it dies" and "where does trust change" | Low | Authored; sizes and limits stay out |
| Planned-alert element tagged `planned` (H14) | Uses the existing tag; shows the gap honestly | Low | Remove the tag when the rule goes live |
| IPv6 note: `KANBAN-INGRESS` is IPv4-only, `ip6tables` INPUT policy ACCEPT | A known open exposure, not in any diagram | Low | [D] `INFRA_ARCHITECTURE.md:248-252`; not verified on the VM |
| Workflow `job` elements for `invariant-checks` incl. `likec4-model-drift`, `pre-commit` hook (D5, D6) | The model gates itself, closing the loop | Low | Do not model all 23 jobs |

## Anti-Features

| Anti-feature | Why avoid | Do instead |
|---|---|---|
| `postgres-bridge` Service and EndpointSlice | Orphaned: no Flux Kustomization applies it; refers to a removed Docker bridge | Leave out; optionally add a line to the Maintenance Note that the directory is dead (live state unchecked) |
| One element per Flux Kustomization, per Kubernetes object, per Helm value | 9 Kustomizations, 6 IngressRoutes, 6 Middlewares, 3 Certificates would swamp every view | Group: one `flux` element, one `routing` description, one `certificates` description |
| Pods, replicas, Secrets, memory limits, PVC sizes, Service ports as labels | The standard excludes them; numbers drift unless a check covers them | Leave out, or add the check first |
| cAdvisor, Docker, Caddy, Compose, Promtail, DOCKER-USER, `docker0` | No workload or live path; only a dashboard ConfigMap or deleted history remains | Skip |
| Alertmanager as an element | Disabled in `kube-prometheus-stack.yaml:37-38`; modelling it invents a component | The planned backup alert (H14) only |
| A scenario per endpoint or per CRUD verb | About 28 routes; none adds a trace S5 and S11 do not | Cover by outcome class |
| The test package tree, ArchUnit classes, Gradle tasks as elements | Class-level; ARCHITECTURE.md "Testing" owns it; fails the survives-a-refactor test | Reference from the Development view description |
| All 23 workflow jobs as elements | Job names change often and a job is not a boundary | 9 workflow elements at most |
| Hand-written counts in labels ("34 containers", "14 events", "38 edges") | Rule: a number in a label is covered by a check or left out | Generate or omit |
| The local `docker-compose.yml` | Not a deployment artifact (INFRA_ARCHITECTURE.md:266-269) | Omit; one sentence in the index description |
| Netcup Cloud Firewall rules as content | Not in this repo; unverifiable | External element, description "not in this repo", no rules |
| Branches (`alt`/`par`) inside one dynamic view | Not tested (standard V1); not adopted | One view per outcome |
| Migrating or deleting any Mermaid file | Out of scope for v1.5 | Leave as the parity baseline |

## Feature Dependencies

```
Fix edge namespace + add 4 namespaces (H2,H3)
  -> every new Physical element (placement needs the namespace)
Amend DIAGRAM_CONVENTIONS.md Physical row
  -> Monitoring view (H6, P8)
External systems (H11) -> delivery view (H13), backup (S15), S6, S7, S16, S17
Flux element (H5) + Docker Hub (D7) -> S6, S7
Traefik + Middlewares (H7) -> S8, S9
Host-level elements (H10, spike) -> S8, S10
Schema Registry (P5) + init job (P6) -> S7 (fold), S11 (lookup arrows)
Existing elements (api, publisher, consumer, topics, table_activity_log) -> S11, S12, S13 with no new element
Decide client-visible-only vs filter-chain components -> S5
sync.mjs walk extension (differentiator) -> chk rung for H4, H5, H6, H7, D5, D7
```

## Dependencies on what already exists

| Existing piece | What it gives | What it does not give |
|---|---|---|
| `sync.mjs generate` | tables, FKs, packages, imports | nothing for k8s, workflows, Helm |
| `sync.mjs check`: topics, workloads (`k8s/base`, `k8s/data`; Deployment, StatefulSet, CronJob), `metadata.source` paths exist, viewpoint tags, coverage | existing `chk` rung for `redpanda`, `postgres`, `backup`, `kanban` | Helm-managed workloads, DaemonSets, namespaces, Flux, Certificates, workflows |
| `metadata source` check | any new element with a real path gets existence-checked for free | does not check content |
| `metadata workload` check | passes only for kinds it scans; a new element carrying `workload 'DaemonSet/...'` would fail as "k8s has no such manifest" because DaemonSets are Helm-rendered, not in the repo | use `source` for Helm elements, or extend the walk |
| Pre-commit trigger paths | `k8s/base`, `k8s/data`, `db/migration`, `docs/architecture`, `KafkaTopics.java` | `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`, `.github/workflows` are not triggers; drift there is CI-only |
| `drill.fish` (12 drills, run on a scratch copy) | the pattern for proving a new rule fires | one drill per new rule must be written |
| Spec kinds: actor, system, container, component, thread, datastore, table, topic, job; nodes server, cluster, namespace | enough for GitHub, Flux, monitoring pieces (`system`/`container`), backup (`job`) | no kind for an iptables chain or a ClusterIssuer; use `component` under a `system`, or add a kind |
| Tags `planned`, `generated`, five viewpoint tags | `planned` is unused today and fits H14 | - |

## MVP Recommendation (ordering for the roadmap)

1. **Correct and extend the physical skeleton.** Fix `edge` (H2), add the four namespaces (H3), amend the
   standard's Physical and Scenarios rows. Nothing else is placeable before this. Low.
2. **Elements and edges for what is already runtime but unmodelled:** Schema Registry, init job, management
   port (P5-P7); cert-manager, issuers, Let's Encrypt (H4); GitHub, runner, Docker Hub, Flux (H5, H11, D7);
   B2 and age (P11). Med.
3. **Monitoring** as a second physical view, plus its runtime edges (H6, P8). Med.
4. **Scenarios that need only existing or step-2 elements:** S11, S12, S13, S2, S3, S5 (after the decision in
   finding 3), S6, S7, S15. Low-Med.
5. **Edge and host scenarios** (S8, S9, S10, S16) after a spike on how to place iptables chains and the
   Netcup firewall (H10). High risk, schedule last.
6. **Checks for the new elements** (the `sync.mjs` walk extension, trigger paths, one drill per rule). Do this
   per step as each element lands, not at the end: an element added without its check starts at `auth` and
   is rarely promoted later.
7. **Differentiators** (S14, S17, D3, L5, H9, H12, H14) as capacity allows.

Defer: per-event subject generation (L5) unless a registry element is wanted anyway; layer overlay (D3) until
the development view's natural-size readability is measured.

**What "complete" can truthfully mean once steps 1-6 are done.** Every namespace in
`k8s/platform/namespaces.yaml` plus the three Helm and Flux namespaces has a node; every workload kind
under `k8s/` and every file in `.github/workflows/` is claimed by an element or an explicit exclusion; all
seven Mermaid scenarios have a LikeC4 counterpart; the Physical and Scenarios rows of the standard match
what is drawn. It cannot mean the Netcup firewall ruleset or live cluster state is verified: those stay
`auth` and external.

## Unverified, so do not state as fact

- Everything about live cluster state (container counts, namespace list as deployed, the `KANBAN-INGRESS`
  counter after Docker's removal, the IPv6 policy). All from INFRA docs [D].
- The Netcup Cloud Firewall policy (not in the repo).
- The Backblaze bucket name `kanban-prod-backups-16-09-26` and lifecycle (INFRA_RUNBOOK.md:420-424 [D];
  `infra/b2/backup-bucket-lifecycle.json` exists but I did not open it).
- The Mermaid `signin.mmd` (not read; the three LikeC4 signin views already supersede it by the standard).
- Whether LikeC4 dynamic views can express `par`, and whether `deployment`-scoped instances can hold
  non-Kubernetes things like an iptables chain. Both need a render experiment.
- Kruchten's selection criteria as quoted above (from memory).
- That the `postgres-bridge` directory is absent from the live cluster (the repo shows no applier only).

## Sources

All repo-relative to /home/andre/dev/kanban-board-backend, read this run.

- `docs/architecture/model/{model,generated,views,spec,deployment}.c4`; `docs/architecture/sync.mjs`
- `docs/DIAGRAM_CONVENTIONS.md` ("LikeC4 standard", lines 46-278)
- `docs/INFRA_ARCHITECTURE.md`, `docs/ARCHITECTURE.md`, `docs/AUTH_FLOWS.md` (1-80), `docs/INFRA_RUNBOOK.md` (backup section only)
- `docs/diagrams/**/*.mmd` (all except `signin.mmd`), `docs/diagrams/render-manifest.tsv`
- `k8s/**` (kinds and names for every file; full reads of `base/app/app.yaml`, `data/postgres/{networkpolicy,backup-cronjob}.yaml`,
  `data/postgres-bridge/*`, `flux-system/{gotk-sync,image-automation,data,kustomization}.yaml`, `platform/**`,
  `monitoring/configs/{ingressroute,podmonitors}.yaml`, `monitoring/controllers/kube-prometheus-stack.yaml`,
  `overlays/prod/ingressroute.yaml`)
- `.github/workflows/*.yml` (job lists for all nine; headers of five)
- `src/main/java/**` (constant, config, service/ResetService, security, controller listings), `src/main/resources/application.properties`,
  `src/main/avro/*.avsc` (file list), `src/test/.../architecture/LayeringArchTest.java`
- `infra/vm/sshd/kanban-ci-tunnel.conf`, `infra/vm/k3s/config.yaml`, `Dockerfile`, `build.gradle` (plugin and task list), `.githooks/pre-commit` (header)
- `.planning/PROJECT.md`; `/home/andre/dev/custom-ai-skills/shared/general-rules/RULES.md`
