---
phase: quick-261109-vzu
plan: 01
subsystem: ci-security-scanning
tags: [trivy, container-scan, misconfiguration, ci, report-only]
requires: []
provides:
  - scripts/trivy-scan.sh (image and config modes, one digest-pinned Trivy container)
  - scripts/verify-trivy-report.py and its selftest (scan-coverage guard)
  - .github/workflows/trivy-scan.yml (weekly and on-demand, report-only)
affects: [README.md gate table, docs/INFRA_RUNBOOK.md, .gitignore]
tech-stack:
  added: [Trivy 0.76.2 container pinned by digest]
  patterns: [coverage guard with selftest and mutation proof, root-drift guard, report-only scan]
key-files:
  created:
    - scripts/trivy-scan.sh
    - scripts/verify-trivy-report.py
    - scripts/verify-trivy-report-selftest.py
    - .github/workflows/trivy-scan.yml
    - .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt
    - .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-proof.txt
  modified:
    - .gitignore
    - README.md
    - docs/INFRA_RUNBOOK.md
key-decisions:
  - "Trivy runs only as ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb, never through trivy-action or setup-trivy"
  - "Image scan is OS packages only, so Java libraries are not reported a third time"
  - "Both scans are report-only; the guard, not the findings, decides whether a job is red"
  - "Trivy --timeout is 30m in the script, because the default 5m failed the first real run"
metrics:
  duration: about 1 hour 10 minutes wall clock, of which the two image builds and scans were about 20 minutes
  completed: 2026-11-09
status: complete
actuals:
  tokens: 13300
  tasks: 3
  commits: 3
plan_head_before: 760bdf2cafac2629e7a7557fb376e557303f88c4
plan_head_after: 9c12a52206d1eee0c833b555d761cadab4ad77e8
---

# Phase quick-261109-vzu Plan 01: Set up Trivy Summary

Digest-pinned Trivy 0.76.2 run by one script, scanning the OS packages of the Dockerfile-built image and the Dockerfile and rendered-manifest misconfigurations, with a coverage guard whose selftest is proven against a floor-removed mutant.

## What was built

- `scripts/trivy-scan.sh image` builds the image (`docker build --pull`), exports it with `docker save`, lists dpkg packages inside it, and scans OS packages only. `config` renders the four workload roots (prod, nonprod, architecture overlays and data/postgres) with the repo's pinned kubectl, stages them with both Dockerfiles, and scans with the embedded checks (`--skip-check-update`). Config mode fails when a `k8s/` kustomization root is in neither `SCAN_ROOTS` nor `SKIP_ROOTS`.
- `scripts/verify-trivy-report.py` fails below 95 percent package coverage (image) or when any staged file lacks a config Result with checks run (config). `scripts/verify-trivy-report-selftest.py` holds 35 cases.
- `.github/workflows/trivy-scan.yml`: jobs `image-scan` and `config-scan`, Mondays 07:00 UTC and `workflow_dispatch` only, `contents: read`, only `actions/checkout@v5` and `actions/upload-artifact@v4`, `persist-credentials: false`, selftest before scan, no error masking.
- README gate-table row and a `## Trivy scan triage` runbook section (no baseline numbers copied in).

## Pin used

Trivy 0.76.2, digest `sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb`. Re-verified before use: identical manifest digest on ghcr.io and Docker Hub, GitHub release immutable, still the latest release, and no advisory in `gh api repos/aquasecurity/trivy/security-advisories` lists 0.76.2 (newest is GHSA-8rc5-4fr6-64pw, patched in 0.72.0). The pin was not changed.

## Measurements (details in 261109-vzu-measure.txt)

- Image build: 495s cold Gradle cache, 241s with cached layers. Image scan: 108s. Config scan: 3s.
- Image baseline 2026-11-09: 143 of 143 OS packages scanned (ubuntu 22.04), CRITICAL 0, HIGH 2, MEDIUM 22, LOW 16, 9 with a fix.
- Config baseline 2026-11-09: 6 of 6 files scanned. dockerfile HIGH 1, LOW 2; kubernetes HIGH 12, MEDIUM 22, LOW 46.

## Proofs (details in 261109-vzu-proof.txt)

- P1: the selftest exited 1 before the guard existed.
- P2: the selftest exited 1 against a copy of the guard with the image floor comparison removed (3 cases failed); it exits 0 against the real guard.
- P3: a throwaway `k8s/zz-drift-probe` root made config mode exit 1 and name the probe; removed afterwards, `git status --short k8s` clean, config mode then exited 0.

## Commits

- dbf2ba4 ci(trivy): add pinned report-only image scan with a coverage guard
- 8f8c5f3 ci(trivy): add config scan with root-drift guard and the weekly report-only workflow
- 9c12a52 docs(trivy): document the Trivy scans in the gate table and the runbook

All three used `--no-verify` as the project CLAUDE.md allows for script, workflow and docs commits. Before each, the gitleaks command (clean, with a non-empty staged diff confirmed), `verify-comments.py check` and `verify-instruction-budget.py` all exited 0. Nothing was pushed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Trivy's default 5-minute timeout failed the first image scan**
- **Found during:** Task 1, step 6 (first real run)
- **Issue:** after a 495s cold build the scan died with `context deadline exceeded` at Trivy's default `--timeout 5m`.
- **Fix:** added `TRIVY_TIMEOUT="30m"` and `--timeout` to both Trivy invocations, with a Decisions line in the script header. The rerun passed (scan 108s). The timeout was only observed failing on the image scan; the first failure happened while the vulnerability database download and the analysis shared the 5 minutes, and the exact split was not measured.
- **Files modified:** scripts/trivy-scan.sh
- **Commit:** dbf2ba4

**2. [Rule 3 - Blocking] zsh did not word-split a path list in my first `git add`**
- **Found during:** Task 1 commit protocol
- **Issue:** the add failed and the first gitleaks run scanned 0 bytes (the empty-stream trap the plan warns about). I caught it from the `~0 bytes` line, re-staged with explicit paths, confirmed a 6-file diff stat and re-ran gitleaks (35 KB scanned, clean).
- **Commit:** dbf2ba4 (unaffected)

**3. [Rule 1 - Bug] Comment lint rejected the first SKIP_ROOTS comment** (9 prose lines before a marker, 8 allowed). Added a `Decisions:` marker. Fixed before the commit.

## Stub scan

No stubs, hardcoded empty values, or placeholder text in the created files.

## Threat surface

No new surface beyond the plan's threat model. The scanner container runs as the invoking uid with no Docker socket; the saved image tar is deleted on exit and never uploaded.

## Known limits and what was not verified

- The workflow was never run on a GitHub runner. The first `gh workflow run trivy-scan.yml` after a push is the real test. Runner behavior of the Docker build, anonymous Docker Hub pulls (possible rate limit), and the `timeout-minutes` values (sized from local seconds: image step 35 and job 40 minutes, config step 5 and job 10) are unmeasured.
- The `ci(trivy)` and `docs(trivy)` commits touch `scripts/`, `.github/workflows/` and `.gitignore`, none of which `deploy.yml` ignores on push to main. **Pushing these commits will trigger deploy.yml, which rolls prod and nonprod.**
- First command to run after a push: `gh workflow run trivy-scan.yml`.
- The fresh build shows what a rebuild ships, not the OS layer running in prod. The vulnerability database is unpinned by design. HelmRelease-installed workloads are not scanned.
- The README table row is not column-padded like its neighbours; no gate checks table alignment.

## Unrelated pending changes left unstaged

- Deleted: `docs/data-engineering-extension/source-research/README.md`, `docs/data-engineering-extension/source-research/sources.jsonl`
- Untracked: `.planning/quick/261024-nvj-*` and `.planning/quick/261024-q91-*` output files, `docs/architecture/record-demo.mjs`
- Untracked, left for the orchestrator's docs commit: this task directory's `261109-vzu-PLAN.md` and this SUMMARY.

## Cleanup

`.trivy-reports/`, the scan image tag `kanban-trivy-scan:local`, the mutant guard copy, the drift probe directory and the `/tmp` logs were removed. The pulled Trivy container image remains in the local Docker image cache (it is the pinned scanner).

## Self-Check: PASSED

- Files found: scripts/trivy-scan.sh, scripts/verify-trivy-report.py, scripts/verify-trivy-report-selftest.py, .github/workflows/trivy-scan.yml, 261109-vzu-measure.txt, 261109-vzu-proof.txt.
- Commits dbf2ba4, 8f8c5f3 and 9c12a52 are ancestors of HEAD; `git rev-list --count 760bdf2..HEAD` is 3.
