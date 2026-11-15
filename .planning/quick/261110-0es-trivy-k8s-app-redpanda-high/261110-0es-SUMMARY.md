---
phase: quick-261110-0es
plan: 01
subsystem: k8s-workloads
tags: [kubernetes, trivy, ksv-0014, ksv-0118, readonly-rootfs, redpanda, emptydir]
requires: []
provides:
  - "app, register-schemas and redpanda containers run with readOnlyRootFilesystem true"
affects: [k8s/base/app/app.yaml, k8s/base/redpanda/redpanda.yaml]
key-files:
  modified: [k8s/base/app/app.yaml, k8s/base/redpanda/redpanda.yaml]
  created: [.planning/quick/261110-0es-trivy-k8s-app-redpanda-high/261110-0es-proof.txt]
decisions:
  - "emptyDir at /etc/redpanda (the directory, not redpanda.yaml) because rpk replaces the file through a temp file in that directory"
  - "One disk-backed emptyDir tmp at /tmp shared by the app and register-schemas; no medium Memory, no sizeLimit"
  - "register-schemas mounts /tmp although it starts without it: the image B census names /tmp/hsperfdata_nobody"
  - "No securityContext field beyond readOnlyRootFilesystem on redpanda: the scan measured that a non-empty securityContext ends KSV-0118"
metrics:
  duration: "about 12 minutes (12:57 to 13:09 UTC after the first reads; image B build was cached)"
  completed: 2026-11-15
status: complete
commits: 1
plan_head_before: 0a5b86b568264e633d9f93183b92bf5b2c0275a1
plan_head_after: da5b92da45a38458ad86c44b1afa5ab193089582
actuals:
  tokens: 5233
  tasks: 3
  commits: 1
---

# Phase quick-261110-0es Plan 01: Trivy k8s app and redpanda HIGH Summary

Read-only root filesystem on the app, register-schemas and redpanda containers, with a Docker `--read-only` proof against two app images, takes the Trivy HIGH count on the Kubernetes roots from 12 to 4 (the 4 left are postgres) with no new finding of any severity.

## What changed

Commit `da5b92d` (two files, 39 insertions), made through the full pre-commit hook with no `--no-verify`. The Gradle part of the hook reported fastTest UP-TO-DATE because no Java input changed; gitleaks, the comment lint, the instruction budget, the LikeC4 drift check and spotless ran.

- `k8s/base/redpanda/redpanda.yaml`: container `redpanda` gets `securityContext.readOnlyRootFilesystem: true`, a `config` volume mount at `/etc/redpanda`, and the pod gets `volumes: config emptyDir {}`. Two comments carry the measurement and a falsifier (an image bump repeats the census).
- `k8s/base/app/app.yaml`: `app` and `register-schemas` get `readOnlyRootFilesystem: true` (between `allowPrivilegeEscalation` and `capabilities`), a `tmp` mount at `/tmp`, and the pod gets `volumes: tmp emptyDir {}`. Container and init container order, args and overlay patch indexes are unchanged.

## Before and after (full tables in 261110-0es-proof.txt)

| Measure | HEAD | After Task 1 | After Task 2 and commit |
|---|---|---|---|
| Guard `Failures, kubernetes, HIGH` | 12 | 8 | 4 (all k8s-data-postgres.yaml) |
| MEDIUM / LOW | 22 / 46 | 22 / 46 | 22 / 46 |
| HIGH rows in the two overlays | 8 | 4 | 0 |
| Failures in k8s-overlays-prod.yaml and nonprod | 28 / 28 | 26 / 26 | 24 / 24 |
| postgres / architecture | 19 / 5 | 19 / 5 | 19 / 5 |
| Files scanned | 6 of 6 | 6 of 6 | 6 of 6, Verdict PASS |

The line-to-container mapping was read from the rendered prod file before editing: lines 60 to 105 app, 107 to 129 register-schemas, 153 to 207 redpanda.

## Census and read-only runs (Docker, no cluster)

| Container | Writable census (`docker diff`, writable root) | Without the mount, `--read-only` | With the mount, `--read-only` |
|---|---|---|---|
| redpanda (v26.2.1) | only /etc/redpanda/redpanda.yaml | exit 1 in 2 s, "error writing to temporary file ... read-only file system" | Healthy true, topic create/produce/consume round trip, 0 read-only or permission lines, generated redpanda.yaml byte-identical to the writable run, docker diff empty, restart keeps health |
| app, image A (deployed tag main-165-ef59a17, root) | only /tmp: hsperfdata_root, 2 tomcat-docbase, 2 tomcat work trees | exit 1, "Unable to start web server" with the temp-dir read-only cause | health UP, /api/healthz 200, 0 read-only lines, real Postgres 16 and Redpanda |
| app, image B (HEAD build, uid 65534) | only /tmp: hsperfdata_nobody and the same tomcat dirs | exit 1, "Unable to create tempDir. java.io.tmpdir is set to /tmp", "Read-only file system" | health UP, uid and gid 65534 in /proc/1/status |
| register-schemas, image A | nothing at exit | exit 0 (does NOT fail), 14 subjects | exit 0, 14 subjects on a fresh registry |
| register-schemas, image B | /tmp/hsperfdata_nobody | exit 0 (does NOT fail), 14 subjects | init and app both read-only sharing one /tmp volume: init exit 0, 14 subjects, app health UP |

Every register-schemas run used a freshly recreated registry, so the 14 subjects were registered by that run.

Deviation from the plan's expectation: the register-schemas negative control did not fail. The plan allowed a mount there "if its census or its negative control demands it"; the image B census names `/tmp/hsperfdata_nobody`, so it got the mount (rule R1: the census names the path). It is not needed for correctness today and is documented as such in the manifest comment. If you prefer the strict minimum, deleting its 5 lines is safe on both measured images.

## Not verified without a cluster

No cluster was available; everything above is Docker-only.

- kubelet emptyDir semantics (mode 0777, fsGroup 101 group ownership) versus Docker tmpfs and named volumes.
- seccomp RuntimeDefault on top of the dropped capabilities: the Docker runs mirror cap-drop ALL and no-new-privileges, not the seccomp profile.
- The real rollout: the app Deployment under Recreate and the single-replica redpanda StatefulSet restart, in kanban-prod and kanban-nonprod.
- A live PVC already written by the previous (writable-root) container: the read-only runs used fresh data volumes; only a restart was exercised.
- A crash writing to the now read-only `/var/lib/redpanda/coredump`.
- Behaviour under real traffic (the app's Kafka producer path was not driven; only /actuator/health and /api/healthz).
- The schema registrar when Redpanda is still starting (the runs waited for a healthy broker).
- Advertise addresses in the Docker runs were `redpanda:19092` and `redpanda:33145`, not the StatefulSet DNS names.

## What a push would do

A push of `da5b92d` lets Flux re-apply `k8s/`: the app Deployment (strategy Recreate) loses its only pod before the new one starts, so there is a gap, and the redpanda StatefulSet restarts `redpanda-0`, in both kanban-prod and kanban-nonprod. The app's register-schemas init container waits on the schema registry, so if both roll together the app comes up after the broker. A container that crashes on start is an outage until a rollback. Nothing was pushed (`git log origin/main..HEAD` lists this commit on top of earlier local ones).

## Deviations from Plan

1. **[Plan branch, not a failure] register-schemas mount added on census evidence although its negative control passed** (above).
2. **[Measurement note]** Task 1's readiness loop and the restart check both passed on the first probe; the broker was up within about 5 seconds, and the follow-up topic round trip confirms it was healthy, not a loop artifact. The topic checked after the restart is `k0es-t2` (the read-only run's own volume), not `k0es-t` as the plan wording says.
3. **[Rule 3 - blocking]** An initial manifest comment draft failed `verify-comments.py` summary-first (first paragraph over 2 lines); fixed in the comments, lint untouched.

No Rule 1, 2 or 4 fixes, no auth gates, no package installs.

## Left alone in the tree

Unrelated pending changes, not staged or committed: the two deleted files under `docs/data-engineering-extension/source-research/` (README.md, sources.jsonl); the untracked `.planning/quick/261024-nvj-*` and `261024-q91-*` evidence files; `.planning/quick-batches/`; `docs/architecture/record-demo.mjs`. The plan directory (PLAN.md, proof file, this SUMMARY) is uncommitted by instruction; the orchestrator commits it. `k8s/data/postgres`, the overlays and scripts are untouched. No doc statement became false (the one rg hit, INFRA_RUNBOOK.md line 861, is the unrelated architecture-site emptyDir).

## Cleanup

All `k0es-` containers, the volumes `k0es-rp-data`, `k0es-rp-data-ro` to `ro6` and `k0es-tmp`, the network `k0es-net`, the image `kanban-0es-probe:local` and the pulled `main-165-ef59a17` image (it was not local before) were removed. `s0es-app`, `s0es-rp`, `s0es-pg`, `probe-0et-a` and `scratch-0es-net` were not touched. No Testcontainers containers before or after the hook. The commit message file and `/tmp/k0es-*` scratch files were trashed. Gitignored `.trivy-reports/` files stay, holding the post-change reports.

## Known Stubs

None.

## Self-Check: PASSED

- `git merge-base --is-ancestor da5b92d HEAD` holds; the commit lists exactly `k8s/base/app/app.yaml` and `k8s/base/redpanda/redpanda.yaml` and carries the Co-Authored-By line.
- `git diff --quiet HEAD -- k8s` passes; the Task 3 verify chain exited 0 (no `k0es-` Docker object, comment lint, invariants gate, single commit for both files).
- Proof file exists with every named section marker, baseline, census, negative controls, both scan tables, gates and the not-verified list.
