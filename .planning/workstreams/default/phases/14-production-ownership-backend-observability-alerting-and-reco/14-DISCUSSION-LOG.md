# Phase 14: Production ownership: backend observability, alerting and recovery - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-15
**Phase:** 14-production-ownership-backend-observability-alerting-and-reco
**Areas discussed:** Phase split, Paging path, Single-node blast radius, Backups

---

## Phase split

| Option | Description | Selected |
|--------|-------------|----------|
| 14 then 14.1, backups pulled forward into 14 | Data-loss exposure closes in the first wave; ~8-9 deliverables per phase | ✓ |
| 14 then 14.1, strict week order | Backups wait for 14.1 | |
| One phase, unsplit | Planner likely returns a split recommendation anyway | |

**User's choice:** 14 then 14.1 with backups in 14.
**Notes:** The user asked for an explanation of backups first (new topic for them): pg_dump vs WAL archiving, CronJob, off-VM object storage, retention, encryption, "did it run" alert vs restore drill, RPO/RTO.

---

## Paging path

| Option | Description | Selected |
|--------|-------------|----------|
| Telegram bot | Free, native Alertmanager support | ✓ |
| Email over SMTP | Needs SMTP credentials | |
| ntfy.sh | Topic name is the secret | |

| Option | Description | Selected |
|--------|-------------|----------|
| Prod pages, nonprod to muted chat | Record kept without noise | ✓ |
| Both page the same | Trains alert fatigue | |
| Prod only | Nonprod alerts only in UIs | |

| Option | Description | Selected |
|--------|-------------|----------|
| Both Watchdog to healthchecks.io and uptime-check to Telegram | Covers VM-dead and unreachable-from-internet | ✓ |
| healthchecks.io only | Uptime check still pages nobody | |
| Uptime check only, made to page | Cron jitter, 60-day auto-disable | |

**User's choice:** 1a 2a 3a.
**Notes:** User required that free tiers be researched for actual suitability before locking. A research subagent produced `14-FREE-TIER-RESEARCH.md`: all SUITABLE WITH CAVEATS; it found Alertmanager and defaultRules disabled in the repo, so `Watchdog` does not yet exist. Follow-up question: enable default rules filtered to relevant groups (selected) vs a custom Watchdog rule only.

---

## Single-node blast radius

| Option | Description | Selected |
|--------|-------------|----------|
| Tempo on local-path, 7d | No new external dependency | ✓ |
| Tempo on object storage | Survives VM loss, another credential | |

| Option | Description | Selected |
|--------|-------------|----------|
| 100% sampling, configurable | Keeps the rare failing trace | ✓ |
| 10% head sampling | Most single failures lose their trace | |

| Option | Description | Selected |
|--------|-------------|----------|
| Trim in order: nonprod tracing, Tempo 3d, Prometheus retention | Protects prod signals longest | ✓ |
| Drop Tempo, trace ids in logs only | No span waterfall | |
| Resize the VPS | Monthly cost | |

**User's choice:** defaults.

---

## Backups

| Option | Description | Selected |
|--------|-------------|----------|
| Prod only | Nonprod is resettable | ✓ |
| Both databases | Doubles storage for disposable data | |

| Option | Description | Selected |
|--------|-------------|----------|
| 7 daily + 4 weekly | Survives a month of detection lag | ✓ |
| 7 daily only | Week-2 discovery has no clean copy | |
| 7 daily + 4 weekly + 3 monthly | Three months of history | |

| Option | Description | Selected |
|--------|-------------|----------|
| age, private key off-VM | VM can encrypt but never decrypt | ✓ |
| Provider server-side encryption | Bucket key reads all PII | |

| Option | Description | Selected |
|--------|-------------|----------|
| Backblaze B2 EU Central | No card, write-only single-bucket key, Object Lock, lifecycle | ✓ |
| Cloudflare R2 | Needs payment method, no write-only key | |

**User's choice:** defaults.

---

## Claude's Discretion

OTLP receiver choice, default-rule group selection, thresholds and intervals, ServiceMonitor vs PodMonitor, metric names, uptime keepalive mechanism, CronJob schedule and upload tool.

## Deferred Ideas

- Capacity test and game-day location — Phase 14.1.
- Restore-drill access to the age private key — Phase 14.1.
- HA (replicas, PDB, HPA) — needs a second node.
