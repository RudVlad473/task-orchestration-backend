# Phase 14: Production ownership: backend observability, alerting and recovery - Context

**Gathered:** 2026-10-15
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 14 is the first half of the production-ownership work: **the app reports on itself, someone is
told when it breaks, and production data is backed up off the VM.**

In scope for Phase 14:

- Prometheus app metrics (`micrometer-registry-prometheus`) on a separate management port that no
  IngressRoute routes, scraped by a ServiceMonitor or PodMonitor.
- Business metrics where the code already makes decisions (tasks created and moved, optimistic-lock
  conflicts, Kafka publish failures, DLT writes, auth failures) and `@Timed`/`Observation` on the
  3-5 hottest endpoints.
- Alertmanager enabled with a Telegram receiver and the first alert rules (app target down, 5xx
  ratio, p95 latency, DLT messages > 0, consumer lag rising, certificate expiry, VM disk).
- A dead-man's switch (`Watchdog` to healthchecks.io) and `uptime-check.yml` made to page.
- Structured JSON logs and OpenTelemetry tracing through Kafka into Tempo, with trace ids in the logs.
- Production post-deploy health verification (folded todo).
- Nightly encrypted `pg_dump` of the prod database to Backblaze B2, with a missing-backup alert.
- Measurement: fire each alert once on purpose and record time-to-notification.

Moved to **Phase 14.1** (to be inserted into ROADMAP.md, depends on 14): SLO recording rules and
multi-window burn-rate alerts, app dashboards (HTTP, JVM, Kafka) and the dashboard-grooming todo,
the weekly automated restore drill with RTO/RPO, the capacity test, the five game days, and one
runbook per scenario plus a postmortem.

Out of scope for both: HA, PDB, HPA (needs a second node); all frontend work (Next.js `traceparent`,
error tracking, Web Vitals, production Playwright smoke) belongs to `kanban-board-frontend`.

</domain>

<decisions>
## Implementation Decisions

### Phase split
- **D-01:** Split into Phase 14 (detection + backups) and Phase 14.1 (SLOs, dashboards, restore
  drill, capacity test, game days, runbooks). Backups are pulled forward from the source plan's
  week 4 into Phase 14 because production has had no backup since the 2026-09-26 cutover dump and
  the backup job depends on nothing else in the phase.

### Paging path
- **D-02:** Alertmanager's receiver is a Telegram bot. Token and chat ids come from a Kubernetes
  Secret via `bot_token_file` / `chat_id_file` (Alertmanager v0.34.1, shipped by chart 91.5.2), never
  inline in a manifest.
- **D-03:** Prod alerts page a primary Telegram chat. Nonprod alerts route to a second, muted
  Telegram chat. Note: Helm replaces lists, so a custom `routes` list replaces the chart's defaults.
- **D-04:** Dead-man's switch is both: (a) kube-prometheus-stack's always-firing `Watchdog` routed
  by an Alertmanager webhook receiver to a healthchecks.io check that notifies Telegram when pings
  stop, with `send_resolved: false`; (b) `.github/workflows/uptime-check.yml` posts to Telegram on
  failure and gets a keepalive against GitHub's 60-day scheduled-workflow auto-disable.
- **D-05:** Enable `alertmanager` and `defaultRules` in `k8s/monitoring/controllers/kube-prometheus-stack.yaml`
  (both currently `false`), with the default rule groups filtered to the relevant ones (`Watchdog`,
  node disk/memory, pod crash-looping and similar). Groups that misfire on k3s (etcd, scheduler,
  controller-manager, which k3s does not expose) stay off. The memory cost of Alertmanager is
  measured with the restart-ladder method before limits are set.
- **D-06:** Every external free tier was checked for actual suitability before being locked; the
  findings, caveats and sources are in `14-FREE-TIER-RESEARCH.md`. Caveats the planner must honour:
  Telegram group chats cap at 20 messages/minute (group alerts), an HTML message over 4096 chars is
  replaced by a fallback notice, healthchecks.io deletes accounts inactive for over a year and has
  no SLA, detection of a dead VM takes about 10-15 minutes.

### Single-node blast radius
- **D-07:** Tempo stores traces on a `local-path` volume on the VM disk with 7-day retention.
- **D-08:** Trace sampling is 100% in both environments, and the sampling ratio is a config property
  so it can be lowered without a code change.
- **D-09:** If measured memory headroom cannot fit Tempo, trim in this order: nonprod tracing off,
  then Tempo retention to 3 days, then Prometheus retention below 30 days. Prod signals are
  protected longest.

### Backups
- **D-10:** Back up the prod database (`kanban_prod`) only. Nonprod is resettable by design (Phase 8).
- **D-11:** Nightly `pg_dump -Fc` from a Flux-managed Kubernetes `CronJob`, uploaded to
  **Backblaze B2, EU Central (Amsterdam)**. — **Reversibility:** costly — a B2 account's region is
  fixed at sign-up; moving region or provider means a new account, new keys and re-uploading history.
- **D-12:** The VM's B2 application key is write-only and scoped to the one backup bucket: the VM can
  upload but never read or delete backups. Object Lock protects the retained files.
- **D-13:** Retention is 7 daily + 4 weekly (about 11 files), enforced by the bucket's lifecycle
  rules where the S3 API supports it, otherwise by the job. (B2 lifecycle via the S3 API is marked
  UNVERIFIED in the research; the planner confirms it.)
- **D-14:** Dumps are encrypted with `age` before upload. The VM holds only the public key; the
  private key lives only in the user's password manager. How the 14.1 restore drill obtains the
  private key is deferred to 14.1.
- **D-15:** An alert fires when no successful backup has completed in about 26 hours.

### Carried forward from prior phases (not re-asked)
- One shared kube-prometheus-stack / Loki / Alloy stack monitors both prod and nonprod (P12 D-04,
  P13 D-07/D-17).
- Every `resources.limits` value is measured with the restart-ladder method, not computed (P12, P13 D-07).
- The three public Grafana dashboard shares keep working; `scripts/verify-public-dashboards.py`
  must pass (P13 D-18).
- All cluster changes ship through Flux from `k8s/` (P13 D-14/D-15).

### Claude's Discretion
- Whether Alloy (already deployed) or a separate collector receives OTLP.
- Exact default-rule groups to enable under D-05, alert thresholds, repeat intervals, grouping.
- ServiceMonitor versus PodMonitor; management port number.
- Business-metric names and which 3-5 endpoints get `@Timed`/`Observation`.
- The uptime-check keepalive mechanism.
- CronJob schedule time and the upload tool (`rclone` or `aws s3`).

### Folded Todos
- **`2026-09-30-no-backups-of-current-production-data.md`** — the only dump is the 2026-09-26
  cutover archive on the VM itself. Closed by D-10..D-15.
- **`2026-08-20-no-documented-backup-restore-runbook-for-prod-db.md`** (ASVS V14.1.4) — the backup
  half is documented in `docs/INFRA_RUNBOOK.md` in this phase; the restore runbook lands with the
  14.1 restore drill.
- **`2026-08-20-no-prod-post-deploy-health-verification.md`** — prod deploys get the post-deploy
  health verification nonprod already has. Under Flux there is no CI deploy step to hang it on, so
  the planner picks the mechanism (for example a Flux health check or alert, or the uptime probe).
- **`2026-08-20-no-security-event-logging-on-auth-and-access-control.md`** — covered by the
  auth-failure metric plus structured JSON log events on the auth and ownership-check paths.
- **`2026-08-20-no-remote-log-shipping-structured-logging-or-alerting.md`** — P12 closed log
  shipping; this phase closes structured/UTC logging and alerting.
- **`2026-09-07-groom-grafana-dashboards-and-expose-public-links.md`** — folded into **Phase 14.1**
  alongside the app dashboards, not this phase.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Source plan and research
- `/home/andre/dev/custom-ai-skills/skills/thoth/state/20261013-production-ownership-plan.md` §2 Weeks 1, 2 (backend half), 4 — the build list and measurements; its library specifics are from memory and must be checked against `build.gradle` (Spring Boot 3.5.16).
- `.planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-FREE-TIER-RESEARCH.md` — Telegram, healthchecks.io, GitHub Actions and B2/R2 limits, caveats, sources and human setup steps.

### Infrastructure
- `docs/INFRA_ARCHITECTURE.md` — current k3s topology and monitoring nodes.
- `docs/INFRA_RUNBOOK.md` — VM spec (4 vCPU / 7.8 GiB), existing `pg_dump` commands at lines ~404-434, cutover archive.
- `k8s/monitoring/controllers/kube-prometheus-stack.yaml` — chart 91.5.2; `alertmanager.enabled: false`, `defaultRules.create: false`.
- `k8s/monitoring/controllers/alloy.yaml`, `k8s/monitoring/controllers/loki.yaml` — existing log pipeline.
- `k8s/monitoring/configs/podmonitors.yaml` — existing scrape config pattern.
- `k8s/data/postgres/postgres.yaml` — Postgres 16 StatefulSet, the backup source.
- `.github/workflows/uptime-check.yml` — the probe made to page under D-04; its comment lists known holes.

### Project conventions
- `docs/ARCHITECTURE.md`, `docs/CODE_STYLE.md`, `docs/CODE_REVIEW_RUBRIC.md` — Java changes and tests.
- `docs/DIAGRAM_CONVENTIONS.md` — infra diagrams must be updated with the topology (see memory: infra diagrams go stale silently).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `spring-boot-starter-actuator` is already a dependency; only the Prometheus registry is missing.
- `KafkaConsumerConfig` already wires `errorHandler.setRetryListeners(...)` with a `RetryListener`: the hook for DLT-write metrics.
- Alloy already runs on the node and can receive OTLP.
- `scripts/verify-public-dashboards.py` checks the public Grafana shares.

### Established Patterns
- `application.properties` exposes only `health` publicly (`management.endpoints.web.exposure.include=health`, `show-details=never`): the deliberate "public actuator = health only" decision. The management port keeps it intact; write the reason beside the existing comment.
- Third-party charts are Flux `HelmRelease`s under `k8s/monitoring/controllers/`; own workloads are Kustomize under `k8s/base/` with overlays.
- Secrets are plain Kubernetes Secrets created on the VPS (P13 D-10), never committed.

### Integration Points
- `src/main/resources/application.properties` — management port, structured logging, tracing properties.
- `build.gradle` — `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, OTLP exporter.
- Spring Kafka template and listener observation for trace propagation through Redpanda.
- `k8s/monitoring/` — Alertmanager config, rules, Tempo, monitors.
- A new backup CronJob near `k8s/data/postgres/`.

</code_context>

<specifics>
## Specific Ideas

- Every stage ends in a measurement (time-to-notification per alert) because the résumé bullets are written from those numbers, not from the plan.
- Human-only setup steps (Telegram bot via @BotFather, healthchecks.io account, B2 account created in EU Central, write-only key, `age` keypair with the private key stored in the password manager) are listed in `14-FREE-TIER-RESEARCH.md` and must appear as explicit human checkpoints in the plans.

</specifics>

<deferred>
## Deferred Ideas

- Where the capacity test and game days run (nonprod shares the node with prod) — Phase 14.1 discussion.
- How the restore drill obtains the `age` private key — Phase 14.1 discussion.
- HA (replicas, PDB, HPA) — needs a second node; name it in the README as the next step.

### Reviewed Todos (not folded)
The 60 other todos matched by keyword were unrelated to observability or recovery (API contract, security hardening, test tooling) and were not folded.

</deferred>

---

*Phase: 14-production-ownership-backend-observability-alerting-and-reco*
*Context gathered: 2026-10-15*
