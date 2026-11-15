---
phase: quick-261110-0et
plan: 01
subsystem: k8s-workloads
tags: [kubernetes, trivy, ksv-0014, ksv-0118, ksv-0109, readonly-rootfs, postgres, seccomp, trivyignore]
requires: []
provides:
  - "postgres container runs with readOnlyRootFilesystem true, an emptyDir socket directory and a pod RuntimeDefault seccomp profile"
  - "path-scoped .trivyignore.yaml entry for the KSV-0109 false positive, loaded by config mode through --ignorefile"
affects: [k8s/data/postgres/postgres.yaml, .trivyignore.yaml, scripts/trivy-scan.sh, scripts/verify-trivy-report.py, docs/INFRA_RUNBOOK.md]
key-files:
  modified: [k8s/data/postgres/postgres.yaml, scripts/trivy-scan.sh, scripts/verify-trivy-report.py, docs/INFRA_RUNBOOK.md]
  created: [.trivyignore.yaml, .planning/quick/261110-0et-trivy-k8s-postgres-high/261110-0et-proof.txt]
decisions:
  - "emptyDir at /var/run/postgresql only: the writable-root census (first boot, restart, workload, spilling sort) showed the unix socket and its lock file as the only writes outside the data volume"
  - "Pod seccompProfile RuntimeDefault is the pod securityContext: a container field clears only the container row, and the profile matches app, architecture-site and the backup CronJob"
  - "No runAsUser, runAsGroup, runAsNonRoot or fsGroup: Postgres still starts as root and the entrypoint drops to uid 999, so the data volume's ownership is untouched"
  - "KSV-0109 is a keyword match on variable names (residue 0 lines, no Secret in the tree): suppressed for k8s-data-postgres.yaml only, through .trivyignore.yaml plus --ignorefile, never an ID-wide plain .trivyignore"
metrics:
  duration: "about 25 minutes (first Docker probe about 14:00 UTC, commit hook finished 14:11 UTC, plus reads and writing)"
  completed: 2026-11-15
status: complete
commits: 1
plan_head_before: b7d1a8fc29332a2d4fff7061abb10fe0e126e946
plan_head_after: 7e4458d4d270373fa8afed49b3ad05558597f186
actuals:
  tokens: 2011
  tasks: 3
  commits: 1
---

# Phase quick-261110-0et Plan 01: Trivy k8s postgres HIGH Summary

Read-only root filesystem on the postgres container (emptyDir at /var/run/postgresql, pod seccomp RuntimeDefault, no uid change) plus a path-scoped `.trivyignore.yaml` entry for the KSV-0109 false positive takes the Trivy HIGH count on the scanned roots from 4 to 0, with no new finding of any severity.

## What changed

Commit `7e4458d` (5 files, 57 insertions, 4 deletions, no deletions of files), made through the full pre-commit hook with no `--no-verify` (the hook ended with `BUILD SUCCESSFUL in 4m 1s`).

- `k8s/data/postgres/postgres.yaml`: pod `securityContext.seccompProfile.type: RuntimeDefault`; container `postgres` gets `securityContext.readOnlyRootFilesystem: true` and a `socket` mount at `/var/run/postgresql`; the pod gets `volumes: socket emptyDir {}`. Image, env, args, probe, resources and the volumeClaimTemplates are unchanged. Comments carry the measurement and a falsifier (an image change or a new extension repeats the census).
- `.trivyignore.yaml` (new): one entry, `KSV-0109`, `paths: [k8s-data-postgres.yaml]`, with a statement citing the evidence and the date, and a header with the falsifier.
- `scripts/trivy-scan.sh`: `scan_config` passes `--ignorefile .trivyignore.yaml`; one `Decisions` bullet and one `Known holes` bullet. `run_trivy`, `scan_image` and the root lists are unchanged.
- `docs/INFRA_RUNBOOK.md`: the Suppression paragraph now names `.trivyignore.yaml`, the flag, the one live entry and its falsifier (the old "None exists today" claim is gone).
- `scripts/verify-trivy-report.py`: the one Known holes line about suppressed findings names both ignore files.

## Before and after (full tables in 261110-0et-proof.txt)

| Measure | HEAD | After Task 1 | After Task 2 and commit |
|---|---|---|---|
| Guard `Failures, kubernetes, HIGH` | 4 | 1 (KSV-0109) | row absent (0 HIGH or CRITICAL by jq) |
| MEDIUM / LOW | 22 / 46 | 21 / 45 | 21 / 45 |
| Failures in k8s-data-postgres.yaml | 19 | 14 | 13 |
| prod / nonprod / architecture | 24 / 24 / 5 | 24 / 24 / 5 | 24 / 24 / 5 |
| dockerfile LOW | 2 | 2 | 2 |
| Files scanned | 6 of 6 | 6 of 6 | 6 of 6, Verdict PASS |

The one MEDIUM and one LOW that disappear are KSV-0104 and KSV-0030 on the postgres StatefulSet, cleared by the pod seccomp profile; the 14 rule IDs left on the postgres file contain neither.

## Census and read-only runs (Docker, local postgres:16, id c1b3783309b6, PG 16.15, Debian 13)

| Run | Result |
|---|---|
| Writable-root census (real init scripts, first boot, workload, spilling sort of 200000 rows, restart) | `docker diff` lists only `/run/postgresql` and `.s.PGSQL.5432` and its `.lock`, before and after the restart |
| Negative control, `--read-only`, no mount | exit 1 in seconds: "could not create lock file ... Read-only file system", "pg_ctl: could not start server" |
| `--read-only` plus tmpfs on `/var/run/postgresql`, fresh volume, first boot | init scripts ran, 4 roles present, pg_stat_statements loaded, sort returned 200000, `docker diff` empty, 0 read-only or permission lines, PID 1 is `postgres` as the postgres user; restart healthy, `docker diff` empty |
| Same form on the volume written by the writable run | "Database directory appears to contain a database; Skipping initialization", 4 roles, `docker diff` empty, 0 read-only or permission lines |

## Suppression negative control (pinned Trivy 0.76.2, scratch copy of the rendered file)

| `--ignorefile` entry | KSV-0109 rows |
|---|---|
| paths `some-other-file.yaml` | 1 (row stays) |
| paths `k8s-data-postgres.yaml` | 0 |
| no flag, with and without the real `.trivyignore.yaml` in the working directory | 1 (the YAML form is not auto-loaded) |

## KSV-0109 evidence in two lines

The ConfigMap `postgres-init-62tc4bg5h4` (2 scripts, 112 lines) has 11 lines matching `pass`: 2 comments, 3 `${VAR:?}` guards, 3 `-v name="$VAR"` arguments and 3 psql `:'name'` references, so the residue is 0 lines. No `kind: Secret` is rendered or committed under `k8s/`, so the rule matched variable names and nothing moved to a Secret.

## Not verified without a cluster

No cluster was available; everything above is Docker-only.

- kubelet emptyDir semantics (a root-owned 0777 directory that the entrypoint chowns) versus Docker tmpfs.
- containerd's RuntimeDefault seccomp profile versus Docker's default profile.
- The node's cached `postgres:16` digest (the census used id c1b3783309b6) and a data directory written by an older 16.x image.
- The real rollout of the single-replica StatefulSet, and how the two apps' connection pools recover after the restart.
- Behaviour under load beyond the spilling sort.
- The weekly workflow `.github/workflows/trivy-scan.yml` running the new `--ignorefile` flag on a runner. Image mode (`trivy-scan.sh image`) was not run: its invocation is unchanged (a region check shows the flag is absent from `scan_image`).
- The YAML ignore feature is experimental in the pinned Trivy; a pin bump must compare the rows, and a reappearing row is the signal.

## What a push would do

A push of `7e4458d` lets Flux re-apply `k8s/data/postgres`, which replaces `postgres-0`. One instance serves both prod and nonprod, so both databases are down for the restart, and for as long as a failed start lasts until a revert reconciles. The StatefulSet has no liveness probe and the data PVC is untouched (an existing data directory is detected and init is skipped). Nothing was pushed: `git log origin/main..HEAD` lists this commit on top of earlier local ones.

## Deviations from Plan

None - plan executed exactly as written. No Rule 1, 2, 3 or 4 fixes, no auth gates, no package installs, and rules R2 (read-only run failure), R3 (suppression control) and R4 (real secret) were not triggered.

Execution notes, not deviations:

- A first workload command in the census used a zsh variable as a command and failed with "command not found"; it was rerun through `bash -c` and the recorded results come from that rerun. The pg_stat_statements row counts differ by one between runs (15 and 14); both are loaded and answering.
- After the hook, `docker ps -a --filter label=org.testcontainers` listed a `postgres:16` container and a `testcontainers/ryuk` container (none before); both were already gone when `docker rm -f` ran (the reaper removed them), so nothing was left over.
- The comment lint reports 344 files before and 345 after the commit: `.trivyignore.yaml` is only counted once git tracks it.

## Left alone in the tree

Unrelated pending changes, not staged or committed: the two deleted files `docs/data-engineering-extension/source-research/README.md` and `sources.jsonl`; the untracked `.planning/quick/261024-nvj-*` and `261024-q91-*` evidence files; `.planning/quick-batches/`; `docs/architecture/record-demo.mjs`. The plan directory (PLAN.md, proof file, this SUMMARY) is uncommitted by instruction; the orchestrator commits it. `k8s/base/app`, `k8s/base/redpanda`, `k8s/data/postgres/kustomization.yaml` and the init scripts are untouched. The docs search for statements the change falsifies hit only the Suppression paragraph, the guard docstring and the script header (all edited).

## Cleanup

Removed: containers `k0et-pg`, `k0et-pg-neg`, `k0et-pg-ro`, `k0et-pg-pop`; volumes `k0et-pg-data-w`, `k0et-pg-data-neg`, `k0et-pg-data-ro`; the scratch directory `.trivy-reports/k0et-ignore-neg`; the commit message file; the `/tmp/k0et-*` scratch files (all with `docker rm`, `docker volume rm` and `trash-put`). No image was pulled or built. `s0es-app`, `s0es-rp`, `s0es-pg` and `probe-0et-a` were not touched. Gitignored `.trivy-reports/` files stay, holding the post-change reports.

## Known Stubs

None.

## Self-Check: PASSED

- `git merge-base --is-ancestor 7e4458d HEAD` holds; the commit lists exactly `.trivyignore.yaml`, `docs/INFRA_RUNBOOK.md`, `k8s/data/postgres/postgres.yaml`, `scripts/trivy-scan.sh`, `scripts/verify-trivy-report.py` and carries the Co-Authored-By line (message file written with it; hook ran in full).
- `git diff --quiet HEAD` over those paths passes; no `k0et-` Docker container, volume or scratch directory remains; comment lint and invariants gate pass after the commit.
- Proof file has the sections Pre-flight, Baseline, Postgres census (writable root), Postgres negative control, Postgres read-only run (fresh and populated volume), Render and validate, After Task 1, KSV-0109 evidence, Suppression negative control, After Task 2 and Gates, plus the not-verified list.
