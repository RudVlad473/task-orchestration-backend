---
phase: quick-261110-0er
plan: 01
subsystem: container-image
tags: [docker, trivy, non-root, ds-0002, cve-2026-109518]
requires: []
provides:
  - "Runtime image runs the JVM as uid and gid 65534 (USER 65534:65534)"
affects: [k8s/base/app/app.yaml (sibling item 261110-0es)]
key-files:
  modified: [Dockerfile]
  created: [.planning/quick/261110-0er-trivy-dockerfile-high/261110-0er-proof.txt]
decisions:
  - "No apt upgrade layer: the freshly pulled eclipse-temurin:21-jre-jammy already ships apt and libapt-pkg6.0 2.4.15ubuntu0.1 (plan step 2 branch)"
  - "Numeric USER 65534:65534, jar left root-owned, no chown or chmod"
metrics:
  duration: "roughly 30 minutes, not measured (no start time was recorded)"
  completed: 2026-11-15
status: complete
commits: 1
plan_head_before: 7d3ba3daf90296df191471a6fbf90995c8e82f3c
plan_head_after: f0ecee2b80b90a8a6e9b93345a8f29e5da1c6837
---

# Phase quick-261110-0er Plan 01: Trivy Dockerfile HIGH Summary

The runtime image now ends in `USER 65534:65534`, which removes DS-0002; the two image-scan HIGH findings (CVE-2026-109518) are gone too, but because the refreshed base already carries the fixed apt, not because of any layer added here.

## What changed

- `Dockerfile`, runtime stage only: one 3-line plain-text comment and `USER 65534:65534` between the jar COPY and ENTRYPOINT (5 lines added). Commit `f0ecee2`, made through the full pre-commit hook (gitleaks, comment lint, spotless, fastTest; BUILD SUCCESSFUL in 5m 53s), no `--no-verify`.
- The planned apt layer was NOT added. Plan step 2 says to skip it if the base has caught up, and it had: after `docker pull eclipse-temurin:21-jre-jammy` (digest `sha256:dff6eb46...`) the base reports `apt 2.4.15ubuntu0.1` and `libapt-pkg6.0 2.4.15ubuntu0.1`. The planner's cached base held 2.4.14, so the baseline HIGH finding came from a stale local base cache.

## Before and after (full tables in 261110-0er-proof.txt)

| Measure | Before (HEAD) | After |
|---|---|---|
| Image scan HIGH | 2 (CVE-2026-109518 on apt and libapt-pkg6.0) | 0 |
| Image scan lines for CVE-2026-109518 | 2 | 0 |
| Config scan `Failures, dockerfile, HIGH` | 1 | row gone |
| `Failures in Dockerfile` | 2 | 1 (LOW DS-0026, no HEALTHCHECK, untouched) |
| DS-0002 in config.json | 1 | 0 |
| Kubernetes failure rows | HIGH 12, MEDIUM 22, LOW 46 | identical |
| Packages scanned | 143 of 143 | 143 of 143 |
| Image scan MEDIUM / LOW / with a fix | 22 / 16 / 9 | 35 / 16 / 18 |

Image MEDIUM rose from 22 to 35 and with-a-fix from 9 to 18 (the plan expected 22 and 7). The cause is not proven: the scan pulled a newer base and Trivy's vulnerability database is not pinned. The USER line adds no package. Out of scope for this HIGH-only item and not investigated.

## Uid proof (built image, probes in the proof file)

- `Config.User` is `65534:65534`; `id` prints `uid=65534(nobody) gid=65534(nogroup)`.
- The JVM running as PID 1 reports `Uid: 65534` and `Gid: 65534` in /proc/1/status, and the startup log says `started by nobody in /app`.
- /app/app.jar is owner 0, mode rw-r--r--; `touch /app/probe` is denied; `touch /tmp/probe` works; /var/lib/apt/lists is empty.
- With placeholder DB env the first ERROR is a refused connection to 127.0.0.1:5432 (Tomcat had initialised); the manifest's initContainer command fails with a refused connection to 127.0.0.1:1, not a permission error.
- Layer size: the USER instruction adds 0 B; there is no apt layer.

## Handoff for 261110-0es

- If it pins a uid, use 65534.
- `docker run --read-only` with no /tmp mount FAILS at startup: "Unable to create tempDir. java.io.tmpdir is set to /tmp", "Read-only file system". With `--read-only --tmpfs /tmp` startup reaches the same database refusal as the writable run. A `readOnlyRootFilesystem: true` therefore needs an emptyDir at /tmp on the app container. Measured with docker `--tmpfs`, not a kubelet emptyDir; the register-schemas initContainer was not run read-only.

## What a push would do

A push of `f0ecee2` triggers `.github/workflows/deploy.yml` (Dockerfile is not in `paths-ignore`), which rebuilds and pushes the production and nonprod image tags; Flux then rolls both environments to an image whose process runs as uid 65534. The pod strategy is Recreate, so a crash on start is an outage until a rollback. Nothing was pushed.

## Not verified without a cluster

- Volume ownership and fsGroup (the manifest mounts no volume; a statement about the manifest, not a run).
- That kubelet accepts `runAsNonRoot: true` against the numeric `65534:65534` form.
- Interplay with any runAsUser, fsGroup or readOnlyRootFilesystem that sibling items add.
- A full start against real Postgres and Redpanda (the probe stops at the first refused database connection).
- The rollout under the Recreate strategy.
- That the image CI builds equals the local one (same Dockerfile and `--pull`, but CI was not run and the base digest can drift).
- Also not done: HEAD's Dockerfile was not rebuilt against the new base, so "the HIGH removal does not depend on this commit" rests on the base's dpkg versions, not on a before/after build.

## Deviations from Plan

**1. [Plan branch taken, not a failure] apt layer skipped.** Found during Task 1 step 2: the base already ships the fixed apt. The plan's must_have "apt and libapt-pkg6.0 at 2.4.15ubuntu0.1 or later" holds with the observed version; the "Design record" claim that a layer is needed was based on the stale cached base. The 2.4.14 baseline is explained, not reproduced.

**2. [Measurement gap] MEDIUM 35 and with-a-fix 18** differ from the plan's expected 22 and 7 (see above).

No Rule 1 to 4 fixes were needed. No auth gates.

## Left alone in the tree

Unrelated pending changes, not staged or committed: the two deleted files under `docs/data-engineering-extension/source-research/` (README.md, sources.jsonl); the untracked `.planning/quick/261024-nvj-*` and `261024-q91-*` evidence files; `.planning/quick-batches/`; `docs/architecture/record-demo.mjs`. The plan directory itself (PLAN.md, the proof file, this SUMMARY) is uncommitted by instruction; the orchestrator commits it.

## Cleanup

Probe containers and `kanban-0er-probe:local` removed; `.trivy-reports/image.tar` gone; no Testcontainers containers before or after the hook; scratch logs and the commit message file trashed. Gitignored `.trivy-reports/` files left in place, now holding the post-change reports.

## Known Stubs

None.

## Self-Check: PASSED

- Dockerfile contains `USER 65534:65534`; commit `f0ecee2` is an ancestor of HEAD and lists only Dockerfile; message carries the Co-Authored-By line.
- `git diff --quiet HEAD -- k8s` passes; nothing pushed.
- Proof file exists with baseline, probes, both after-tables, handoff and not-verified sections.
