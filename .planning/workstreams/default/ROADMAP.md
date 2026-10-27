# Roadmap: Kanban Board Backend — Epic 2 Completion

## Milestones

- ✅ **v1.0 Optimistic Locking** — Phase 1 (shipped 2026-08-01)
- ✅ **v1.1 Kafka Activity Feed** — Phases 2-3 (shipped 2026-08-03)
- ✅ **v1.2 Infra Migration & Schema Registry** — Phases 4, 04.1, 04.2, 5, 6, 7, 07.1 (shipped 2026-08-17)
- ✅ **v1.3 Nonprod Environment & CI Hardening** — Phases 8-10 (shipped 2026-08-25)
- ✅ **v1.4 Self-Hosted Infra and Kubernetes** — Phases 11-13 (shipped 2026-09-30)

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

<details>
<summary>✅ v1.0 Optimistic Locking (Phase 1) — SHIPPED 2026-08-01</summary>

- [x] Phase 1: Optimistic Locking (3/3 plans) — completed 2026-08-01

Full details: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v1.1 Kafka Activity Feed (Phases 2-3) — SHIPPED 2026-08-03</summary>

- [x] Phase 2: Kafka Foundation, Domain Events & Move Endpoint (3/3 plans) — completed 2026-08-01
- [x] Phase 3: Activity Log Consumer, Reliability & Read API (3/3 plans) — completed 2026-08-02

Full details: [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md)

</details>

<details>
<summary>✅ v1.2 Infra Migration & Schema Registry (Phases 4, 04.1, 04.2, 5, 6, 7, 07.1) — SHIPPED 2026-08-17</summary>

- [x] Phase 4: Schema Registry (4/4 plans) — completed 2026-08-04
- [x] Phase 04.1: Flyway database migration implementation (INSERTED) (3/3 plans) — completed 2026-08-05
- [x] Phase 04.2: Testcontainers Postgres, drop H2 (INSERTED) (3/3 plans) — completed 2026-08-06
- [x] Phase 6: Mock-up Feature Gap Closure (7/7 plans) — completed 2026-08-09
- [x] Phase 7: Restructure test folder (7/7 plans) — completed 2026-08-09
- [x] Phase 07.1: Address hard blockers and inconsistencies from the frontend-integration-readiness audit (INSERTED) (9/9 plans) — completed 2026-08-17
- [x] Phase 5: Infra Migration (6/6 plans) — completed 2026-08-17

Full details: [milestones/v1.2-ROADMAP.md](milestones/v1.2-ROADMAP.md)

</details>

<details>
<summary>✅ v1.3 Nonprod Environment & CI Hardening (Phases 8-10) — SHIPPED 2026-08-25</summary>

- [x] Phase 8: Isolated Nonprod Environment, Live and Resettable (3/3 plans) — completed 2026-08-18
- [x] Phase 9: Nonprod Continuous Deploy & Scoped CI Credentials (4/4 plans) — completed 2026-08-19
- [x] Phase 10: CI & Deploy Hardening (6/6 plans) — completed 2026-08-19

Full details: [milestones/v1.3-ROADMAP.md](milestones/v1.3-ROADMAP.md)

</details>

<details>
<summary>✅ v1.4 Self-Hosted Infra and Kubernetes (Phases 11-13) — SHIPPED 2026-09-30</summary>

- [x] Phase 11: Migrate database from Neon to self-hosted Postgres (8/8 plans)
- [x] Phase 12: Self-hosted observability stack (6/6 plans)
- [x] Phase 13: Introduce Kubernetes (10/10 plans)

Full detail: `.planning/milestones/v1.4-ROADMAP.md`

</details>

## Progress

| Milestone | Phases | Plans | Status | Shipped |
|-----------|--------|-------|--------|---------|
| v1.0 Optimistic Locking | 1 | 3/3 | Complete | 2026-08-01 |
| v1.1 Kafka Activity Feed | 2 | 6/6 | Complete | 2026-08-03 |
| v1.2 Infra Migration & Schema Registry | 7 | 39/39 | Complete | 2026-08-17 |
| v1.3 Nonprod Environment & CI Hardening | 3 | 13/13 | Complete | 2026-08-25 |
| v1.4 Self-Hosted Infra and Kubernetes | 3 | 24/24 | Complete | 2026-09-30 |

Next milestone not yet scoped — run `/gsd-new-milestone` to define it.

### Phase 14: Production ownership: backend observability, alerting and recovery

**Goal:** The app reports on itself, someone is told when it breaks, and production data is backed up off the VM: Prometheus app metrics on a separate management port, Alertmanager paging Telegram with a healthchecks.io dead-man's switch, structured JSON logs and OpenTelemetry tracing through Kafka into Tempo, production post-deploy health verification, and a nightly encrypted `pg_dump` to Backblaze B2 with a missing-backup alert. Ends in a measurement: time-to-notification per alert, fired once on purpose. Backend half only: the console work (Next.js `traceparent`, error tracking, Web Vitals, production Playwright smoke) belongs to `kanban-board-frontend`. Source plan: `/home/andre/dev/custom-ai-skills/skills/thoth/state/20261013-production-ownership-plan.md` (library specifics in it are from memory and must be checked against the versions in `build.gradle`). Decisions: `14-CONTEXT.md`. Out of scope: HA, PDB, HPA (needs a second node).
**Requirements**: TBD (CONTEXT.md decisions D-01..D-15 are the contract)
**Depends on:** Phase 13
**Plans:** 5/13 plans executed

Plans:
**Wave 1**
- [x] 14-01-PLAN.md — Tracer: metrics on an unrouted management port scraped by Prometheus, public health at /api/healthz, probes and PodMonitor, gated rollout (wave 1)
- [x] 14-02-PLAN.md — Backup prep: Object Lock decision, tested backup script and lifecycle JSON, B2 EU Central account/key/age keypair (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 14-03-PLAN.md — Nightly encrypted pg_dump CronJob to B2: image decision, NetworkPolicy peer, first run, key capability proof, measured limits, runbook (wave 2)
- [x] 14-04-PLAN.md — Request-path counters, ECS JSON logs and security events, four hottest endpoints timed (three with service Observations) (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 14-05-PLAN.md — Kafka publish-failure and dead-letter counters, latency buckets, exact-name Prometheus scrape contract (wave 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 14-06-PLAN.md — Alertmanager to Telegram (nonprod muted), Watchdog dead-man to healthchecks.io, filtered default rules, kanban-alerts rules, measured limits (wave 4)
- [ ] 14-07-PLAN.md — OpenTelemetry tracing in the app: one trace through Kafka, trace ids in logs, 100% sampling as a property (wave 4)

**Wave 5** *(blocked on Wave 4 completion)*
- [ ] 14-08-PLAN.md — Alerting config CI gate with promtool unit tests, Flux deploy-failure notifications, paging uptime workflow with heartbeat (wave 5)

**Wave 6** *(blocked on Wave 5 completion)*
- [ ] 14-09-PLAN.md — Tempo 3.2.0 with 7-day retention, Grafana log-to-trace links, per-environment export, measured limits and headroom check (wave 6)

**Wave 7** *(blocked on Wave 6 completion)*
- [ ] 14-10-PLAN.md — Authorise the injection scope, fire every detection alert once and record time-to-notification (wave 7)

**Wave 8** *(blocked on Wave 7 completion)*
- [ ] 14-11-PLAN.md — Measure the dead-man, uptime and Flux pages; app memory re-measure; runbook measured-paging section (wave 8)
- [ ] 14-12-PLAN.md — Diagrams and INFRA_ARCHITECTURE views and Maintenance Note from live state (wave 8)

**Wave 9** *(blocked on Wave 8 completion)*
- [ ] 14-13-PLAN.md — README, LOCAL_DEV and the folded todos closed on evidence (wave 9)

### Phase 14.1: Production ownership: SLOs, recovery drills and game days (INSERTED)

**Goal:** Prove the system survives and write down how: SLO recording rules and multi-window burn-rate alerts replacing Phase 14's raw latency/error alerts, app dashboards (HTTP, JVM, Kafka) plus grooming the existing dashboards, an automated weekly restore drill of the Phase 14 backups recording RTO/RPO, a capacity test that finds the knee and the first resource to saturate, five game-day scenarios that each page via Phase 14's alerts, and one runbook per scenario in `docs/runbooks/` plus a postmortem. Open questions carried from Phase 14's discussion: where the capacity test and game days run on the shared node, and how the restore drill obtains the `age` private key.
**Requirements**: TBD
**Depends on:** Phase 14
**Plans:** 0 plans

Plans:
- [ ] TBD (run /gsd-discuss-phase 14.1 first)
