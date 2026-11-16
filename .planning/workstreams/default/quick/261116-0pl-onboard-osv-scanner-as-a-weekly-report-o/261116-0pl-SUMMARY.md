---
phase: quick-261116-0pl
plan: 01
subsystem: ci-security
tags: [osv-scanner, sbom, trivy, github-actions, supply-chain]
requires:
  - phase: quick-261116-005
    provides: osv-scanner against dependency-check comparison (144 of 144 SBOM components, 3 in-scope findings)
provides:
  - scripts/osv-scan.sh, the one command for the verified osv-scanner scan of the boot jar's SBOM
  - scripts/verify-osv-report.py and its selftest, the coverage guard
  - .github/workflows/osv-scan.yml, weekly plus on demand, report-only
  - scripts/trivy-scan.sh image-ref mode, so the Trivy digest exists once
affects: [README CI gates table, docs/INFRA_RUNBOOK.md, scripts/trivy-scan.sh, .github/workflows/trivy-scan.yml]
tech-stack:
  added: [osv-scanner v2.7.1 release binary pinned by SHA-256]
  patterns: [count-based coverage guard with selftest and mutant proof, digest-verified binary download, single-sourced scanner image pin]
key-files:
  created:
    - scripts/osv-scan.sh
    - scripts/verify-osv-report.py
    - scripts/verify-osv-report-selftest.py
    - .github/workflows/osv-scan.yml
    - .planning/workstreams/default/quick/261116-0pl-onboard-osv-scanner-as-a-weekly-report-o/261116-0pl-measure.txt
  modified:
    - scripts/trivy-scan.sh
    - .gitignore
    - .github/workflows/trivy-scan.yml
    - README.md
    - docs/INFRA_RUNBOOK.md
decisions:
  - "Runner-built boot jar, Trivy rootfs SBOM via the trivy-scan.sh pin, digest-verified osv-scanner binary, Python guard (over the CycloneDX Gradle plugin and a Trivy image SBOM)"
  - "Gradle runs with no flag that relaxes dependency verification; a cold Docker bootJar already passes it on every deploy"
  - "Driver is bash, not fish, because the runner has no fish and the linters read .sh"
  - "Step timeout 20 minutes and job timeout 30 minutes, from 3 x 356s rounded up to 5 minutes plus 10"
metrics:
  duration: about 25 minutes of wall time, of which the pre-commit hook took about 5 minutes and the local scan about 6
  completed: 2026-11-16
status: complete
commits: 1
plan_head_before: 1cf0849962379538744d013804bf91aef920c275
plan_head_after: c33fb6c8e79995003b15e5418f9f4f75b94df30b
actuals:
  tokens: 40000
  tasks: 3
  commits: 1
---

# Phase quick-261116-0pl Plan 01: osv-scanner as a weekly report-only scan Summary

A weekly and on-demand report-only osv-scanner job that scans a Trivy CycloneDX SBOM of the Spring Boot jar, with a SHA-256-pinned binary, the Trivy image taken from the single existing pin, and a stdlib coverage guard whose selftest was proven RED against an absent guard and three mutants.

## What was built

- `scripts/osv-scan.sh`: downloads osv-scanner v2.7.1 into a trap-removed `mktemp -d`, checks its SHA-256 before chmod and before Gradle, builds the boot jar, writes the BOOT-INF/lib listing and the runtimeClasspath tree, makes the SBOM with the Trivy image from `bash scripts/trivy-scan.sh image-ref` (mount is `.osv-reports` only), then runs osv-scanner. Exit 0 and 1 continue, 127, 128, 129 and anything else fail.
- `scripts/trivy-scan.sh`: new `image-ref` mode printing `TRIVY_IMAGE`; header names osv-scanner next to dependency-check and Dependabot. No other behaviour change.
- `scripts/verify-osv-report.py`: checks osv-scanner against the SBOM and the SBOM against the runtime modules that ship a jar (95 percent floors, `parse_tree` loaded from `verify-dependency-check-report.py`), prints a table with one row per finding, never fails on findings.
- `scripts/verify-osv-report-selftest.py`: 38 checks (cases plus summary-file content checks) with a `--guard` flag.
- `.github/workflows/osv-scan.yml`: workflow_dispatch plus Monday 08:00 UTC cron, `contents: read`, no secrets, only the sibling actions, selftest first, guard with `if: always()`, uploads exactly four files as `osv-scan-report`.
- `.gitignore`, README CI-gates row and Trivy-row wording, `docs/INFRA_RUNBOOK.md` section `## osv-scanner scan triage` plus the Trivy section's two edits, `trivy-scan.yml` Known holes line.

## Measured results (all local, 2026-11-16, full numbers in 261116-0pl-measure.txt)

- Script wall time 356s (bootJar 13s, tree 11s, SBOM 323s, osv-scanner 1s). Equals the comparison: 144 SBOM components, 144 osv packages, 144 BOOT-INF/lib jars, 157 runtime modules, 3 findings (opentelemetry-api 1.62.0 with CVE-2026-118259, spring-webmvc 6.2.19 with CVE-2026-47890 and CVE-2026-47884).
- Guard on the real files: exit 0, 143 runtime modules ship a jar, 142 matched, `org.hibernate.orm:hibernate-core:6.6.53.Final` unmatched under its legacy group, as the plan predicted.
- Negative digest proof: a copy with the last hex digit changed exited 1 with the digest message and no Gradle output.
- Cold bootJar under strict verification: no local cold run; evidence is deploy.yml run 43723457306 (2026-11-15), job `build-and-push-docker-image`, success, whose Dockerfile runs `./gradlew bootJar` with no flag.
- Selftest direction checked: RED first (guard absent, exit 1), then GREEN against the guard (exit 0), control copy outside `scripts/` exit 0, and RED (exit 1) against each mutant: SBOM floor comparison removed (4 cases failed), runtime floor comparison removed (2 cases failed), empty-input failures removed (8 cases failed). Case names are in the measure file.

## Verification

- Task 1, Task 2 and Task 3 automated verify commands each passed (TASK1, TASK2 and TASK3 verify run to completion).
- The single commit `c33fb6c` went through the full `.githooks/pre-commit` (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest) with exit 0, never `--no-verify`. It holds exactly the ten `files_modified` paths. The pre-existing deletions under `docs/data-engineering-extension/source-research/`, the untracked `.planning/quick/261024-*` files and `docs/architecture/record-demo.mjs` are untouched and unstaged.
- `docs/ARCHITECTURE.md`, `.claude/CLAUDE.md`, `build.gradle`, `settings.gradle`, `gradle/` and `Dockerfile` are identical to HEAD.

## Not verified

- The workflow was NOT run on GitHub and nothing was pushed. The first dispatch after a push has to prove: the binary download and digest match on a runner; a runner-side `./gradlew bootJar` under strict verification; Trivy's Java DB download speed on a runner against the 20 and 30 minute timeouts (sized from a local run, not a runner run); osv-scanner reaching api.osv.dev from a runner; the guard table in the job summary and the four-file artifact.
- actionlint is not installed and was not run; the workflow was only parsed with `yq`.
- SLSA provenance of the osv-scanner binary is not verified (slsa-verifier is not installed).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] A shellcheck directive misparse in the script header**
- **Found during:** Task 1
- **Issue:** a header comment line starting with the word `shellcheck` was parsed by shellcheck as a directive (SC1073).
- **Fix:** reworded the line to "the linters here (shellcheck, the comment lint) ...".
- **Files modified:** scripts/osv-scan.sh
- **Commit:** c33fb6c

**2. [Rule 3 - Blocking] Comment lint summary-first violation in the workflow**
- **Found during:** Task 3
- **Issue:** the timeout comment's first paragraph was 4 lines (docs/CODE_STYLE.md rule 14).
- **Fix:** split a one-line summary from the detail with a blank comment line.
- **Files modified:** .github/workflows/osv-scan.yml
- **Commit:** c33fb6c

**3. [Plan wording] Commit trailer**
- The plan text names `Claude Opus 5.5` in the trailer. The attribution line given for this session is `Claude Sonnet 5.5`, the model that actually ran, so that line was used.

**4. [Plan wording] Index state before the commit**
- The plan asks for an empty `git diff --cached --name-only` first. `git update-index --chmod=+x` on the intent-to-add `scripts/osv-scan.sh` staged that one file, which is one of the ten commit paths, so the index held nothing outside the plan. The commit used `git commit -- <ten paths>`.

Task 1 is a tracer task; its feedback gate was the automated verify, re-run end to end before expanding (passed), with no human-verify checkpoint under the default end-of-phase mode.

## Known Stubs

None.

## Threat Flags

None beyond the plan's register: the new network surface (osv-scanner download, ghcr.io pull, api.osv.dev) is covered by T-261116-0pl-01 to -05.

## Housekeeping

- Scratch (mutant copies, run log, bad-digest copy) was trashed with `trash-put`; `git status` shows only the pre-existing pending changes plus this task's untracked PLAN.md.
- `.osv-reports/` (about 240 KB, gitignored) from the local run is left in the checkout so the plan's verify commands can be re-run. Trivy's 1.5 GB cache lived in a `--rm` container and is gone.

## Self-Check: PASSED

- FOUND: scripts/osv-scan.sh, scripts/verify-osv-report.py, scripts/verify-osv-report-selftest.py, .github/workflows/osv-scan.yml, 261116-0pl-measure.txt
- FOUND: commit c33fb6c8e79995003b15e5418f9f4f75b94df30b is an ancestor of HEAD (it is HEAD)
