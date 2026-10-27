---
phase: 14-production-ownership-backend-observability-alerting-and-reco
plan: 03
subsystem: infra
tags: [kubernetes, cronjob, postgres, backblaze-b2, age, networkpolicy, flux]

requires:
  - phase: 14
    provides: postgres-backup.sh and the B2 bucket, write-only key and age recipient (14-02)
provides:
  - Flux-managed CronJob postgres-backup in kanban-data, nightly 02:17 UTC, kanban_prod only
  - pinned backup image built by the Backup Image workflow
  - NetworkPolicy peer admitting pods labelled kanban-board/postgres-client
  - measured memory limits and a rewritten runbook backup section
affects: [14 alerting (PostgresBackupMissing keys on this CronJob name), 14.1 restore drill]

key-files:
  created:
    - k8s/data/postgres/backup-cronjob.yaml
    - k8s/data/postgres/backup/Dockerfile
    - .github/workflows/backup-image.yml
    - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-03-b2-capability-check.yaml
  modified:
    - k8s/data/postgres/kustomization.yaml
    - k8s/data/postgres/networkpolicy.yaml
    - k8s/data/postgres/backup/postgres-backup.sh
    - scripts/smoke-postgres-backup.fish
    - docs/INFRA_RUNBOOK.md

key-decisions:
  - "Task 1: option-a, a pinned image built in CI (operator, 2026-10-26). Image k8s/data/postgres/backup/Dockerfile, pushed as rudenkovladimir/kanban-postgres-backup:alpine3.22-a75558e@sha256:472829cacfcb3fa10fe51f80206075105a57d8a22daf8d322012c197f2282b0a."
  - "The three third-party actions in backup-image.yml are pinned by commit SHA (a security review flagged the floating tags: the job holds DOCKERHUB_TOKEN). deploy.yml still uses floating tags for the same actions; not changed here."
  - "Commits f4968ed onward used --no-verify, authorised by the operator, because the host was in memory pressure (swap full) and fastTest was killed with exit 137 four times. gitleaks, the k8s gates, the comment lint and shellcheck ran by hand on each diff. The Java suite did not run locally on these commits; none touches Java."

requirements-completed: [D-10, D-11, D-12, D-13, D-14, D-15]

coverage:
  - id: C1
    description: "Manual run completes and the object it reports exists"
    requirement: D-11
    verification:
      - kind: other
        ref: "Job backup-final-20261026-122415 succeeded in 15s under the committed spec; log: uploaded daily/kanban_prod-20261026T122409Z.dump.age, encrypted_bytes=163079, sha256=9a894a0e83f85b1ba08d1b9f14088638eab9135fd10cf0a5d5a42f38294c076f"
        status: pass
    human_judgment: true
    rationale: "The object's existence in the bucket was not read back: the key cannot list, and no one has looked in the Backblaze console. The put-object call returned a VersionId in the probe and exited 0 in the Job."
  - id: C2
    description: "From inside the cluster the VM key's list and get calls are refused"
    requirement: D-12
    verification:
      - kind: other
        ref: "Job b2-capability-check log: list-objects-v2 exit=254 AccessDenied; get-object exit=254 AccessDenied"
        status: pass
    human_judgment: false
  - id: C3
    description: "Memory limit is a restart-ladder measurement with a dated MEASURED comment"
    requirement: D-14
    verification:
      - kind: other
        ref: "ladder 2026-10-26: passed 512Mi 256Mi 128Mi 64Mi, failed 48Mi (OOMKilled exit 137); cgroup peak 63.9MB on every passing rung; adopted 96Mi limit / 64Mi request; verify-k8s-invariants --no-provisional exit 0"
        status: pass
    human_judgment: false
  - id: C4
    description: "Pod reads only PROD_DB_* from postgres-init, no envFrom; NetworkPolicy peer limited to the label in kanban-data"
    requirement: D-13
    verification:
      - kind: other
        ref: "render assertion script exit 0 (no envFrom, hashed script ConfigMap, schedule/timezone/Forbid, three ingress peers); live iptables rule for policy postgres-ingress present"
        status: pass
    human_judgment: false
  - id: C5
    description: "Operator decrypted one downloaded backup and pg_restore -l listed kanban_prod's tables"
    requirement: D-14
    verification:
      - kind: other
        ref: "not done"
        status: unverified
    human_judgment: true
    rationale: "Backstop in the plan: needs the age private key from the operator's password manager. Outstanding."

duration: about 2h of work spread over an interrupted session
completed: 2026-10-26
status: complete
---

# Phase 14 Plan 03: Nightly backup CronJob Summary

**`kanban_prod` is backed up every night at 02:17 UTC to an off-VM, age-encrypted B2 bucket by a Flux-managed CronJob whose key provably cannot read, with measured memory limits; one backup was run by hand and succeeded, and no restore has been tested yet.**

## What was built

Dockerfile and `backup-image.yml` (commit 38bbc9a after rebase), the CronJob, script ConfigMap and NetworkPolicy peer, a SHA pin for the workflow's actions, the digest pin, the measured resources and the rewritten runbook section.

## Deviations found live (not in the plan)

1. **First backup pods were refused by Postgres** with `Connection refused`. A new pod is blocked by the NetworkPolicy controller for a few seconds. A labelled probe pod's first attempt was refused and its second, 10 seconds later, succeeded; a Job's own retries (10s and 20s backoff) all failed. `postgres-backup.sh` now polls `pg_isready` for up to 3 minutes before dumping (commit 4888cfe). The real fix for the controller delay was not investigated; the cause is inferred from that one probe.
2. **Uploads failed with `Connection was closed before we received a valid response`.** From a workstation the same key got `InvalidRequest: Content-MD5 OR x-amz-checksum- HTTP header is required for Put Object requests with Object Lock parameters`. The container sets `AWS_REQUEST_CHECKSUM_CALCULATION=when_required`, so no checksum was sent. `put-object` now passes `--checksum-algorithm CRC32` (commit d8acdf1); a probe pod's upload with it returned a VersionId. Smoke case 1 asserts the flag and was seen to fail against the old script and pass against the new one. The live refusal itself cannot be reproduced locally, so the live manual run is the proof.
3. **The smoke script's header named the wrong override variable** (`BACKUP_SCRIPT`; the script reads `SMOKE_BACKUP_SCRIPT`). Corrected. My first "red" run used the wrong name and tested the fixed script, which is how it was noticed.

## Verification run

- Static, on the final files: verify-k8s-manifests.sh, verify-k8s-invariants.py --no-provisional, verify-comments.py check, shellcheck, smoke 5 of 5, gitleaks on each diff: all exit 0.
- Live: manual Job succeeded under the committed spec; ladder results above; capability check refused both calls; no Kustomization left suspended; live CronJob shows 96Mi/64Mi and the original command.

## Left open

- **End-of-phase human check** (decrypt one real backup, `pg_restore -l`): not done, needs the age private key.
- **Objects left in the bucket by testing:** 6 backups under `daily/` from the manual run, the four passing ladder rungs and the final run (Governance lock, 7 days, expire by lifecycle), and one 1 KB object `test/crc-*` from the CRC32 probe (no lifecycle rule covers `test/`; it is locked 7 days and then needs deleting, which the VM key cannot do). The other probe uploads were refused and left nothing. Counts come from my own run log, not a bucket listing, which the key cannot produce.
- **PostgresBackupMissing alert** is not built yet (alerting plans).
- **Full CI run** for the pushed Java commit (14-07 task 2) was still running at the time of writing; its result is not recorded here.
- **deploy.yml** floating action tags: todo candidate.
