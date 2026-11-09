---
phase: quick-261109-vzu
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - scripts/verify-trivy-report-selftest.py
  - scripts/verify-trivy-report.py
  - scripts/trivy-scan.sh
  - .github/workflows/trivy-scan.yml
  - .gitignore
  - README.md
  - docs/INFRA_RUNBOOK.md
  - .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt
  - .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-proof.txt
autonomous: true
requirements: [261109-vzu]

estimate:
  tokens: 110000
  raw_tokens: 110000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "`bash scripts/trivy-scan.sh image` builds the image from ./Dockerfile, scans its operating-system packages with the digest-pinned Trivy container and exits 0 while findings exist (report-only)"
    - "`bash scripts/trivy-scan.sh config` renders the four workload-bearing kustomization roots, scans them and both Dockerfiles with Trivy's misconfiguration checks, and exits 0 while findings exist"
    - "`python3 -I scripts/verify-trivy-report.py` exits 1 on a report that scanned nothing or less than 95 percent of what the image or the staging directory held, and `python3 -I scripts/verify-trivy-report-selftest.py` exits 0 only while every such case fires"
    - "The selftest exits 1 against a copy of the guard whose floor comparison is neutralised (mutation proof, recorded in 261109-vzu-proof.txt)"
    - "A kustomization root under k8s/ that is neither scanned nor skipped by name makes `bash scripts/trivy-scan.sh config` exit non-zero and name it (proof recorded)"
    - ".github/workflows/trivy-scan.yml runs on schedule and workflow_dispatch only, uses only actions/checkout@v5 and actions/upload-artifact@v4, and runs each job's selftest before its scan"
    - "Trivy is referenced only as ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb, never through trivy-action or setup-trivy"
    - "README.md's gate table and docs/INFRA_RUNBOOK.md describe both scans, how to read a red run, the suppression rule and the pin-bump procedure"
  artifacts:
    - path: "scripts/trivy-scan.sh"
      provides: "The one place holding the Trivy pin, flags and the image and config invocations, run identically locally and in CI"
      contains: "ghcr.io/aquasecurity/trivy:0.76.2@sha256:"
    - path: "scripts/verify-trivy-report.py"
      provides: "Scan-coverage guard for both report kinds, with a severity table and --summary output"
    - path: "scripts/verify-trivy-report-selftest.py"
      provides: "Cases proving the guard fires, runnable against another copy with --guard"
    - path: ".github/workflows/trivy-scan.yml"
      provides: "Weekly and on-demand report-only image-scan and config-scan jobs"
      contains: "image-scan"
    - path: ".planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt"
      provides: "Tool versions, build and scan durations, and the measured image and config baselines"
    - path: ".planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-proof.txt"
      provides: "Selftest red-before-guard, mutation proof and root-drift proof with exit codes"
  key_links:
    - from: ".github/workflows/trivy-scan.yml"
      to: "scripts/trivy-scan.sh"
      via: "each job's scan step runs the script, so the CI command and the local command are the same"
      pattern: "scripts/trivy-scan.sh"
    - from: "scripts/trivy-scan.sh image"
      to: "scripts/verify-trivy-report.py --kind image"
      via: ".trivy-reports/image.json and .trivy-reports/image-packages.txt, the latter taken from dpkg inside the same built image"
      pattern: "image-packages.txt"
    - from: "scripts/trivy-scan.sh config"
      to: "scripts/verify-trivy-report.py --kind config"
      via: ".trivy-reports/config.json and .trivy-reports/config-expected.txt, the latter listing every staged file"
      pattern: "config-expected.txt"
---

<objective>
Add Trivy to this project as a report-only scan that complements the weekly OWASP dependency-check instead of repeating it:
the operating-system packages of the image built from ./Dockerfile (nothing scans eclipse-temurin:21-jre-jammy today), and
Trivy's misconfiguration checks on the two Dockerfiles and the rendered workload manifests under k8s/. Java libraries stay
with dependency-check and Dependabot, and secrets stay with gitleaks and TruffleHog.

Purpose: the repo has no scanner for the OS layer of what it ships or for container hardening of its manifests. Trivy is
adopted the way this repo adopts every scanner: pinned by digest, report-only until a triaged baseline exists, with a guard
that fails when the scan did not scan (the 2026-08-19 dependency-check run went green with 0 dependencies scanned).

Output: scripts/trivy-scan.sh, scripts/verify-trivy-report.py and its selftest, .github/workflows/trivy-scan.yml, a
.gitignore entry, a README gate-table row, a runbook triage section, a recorded measurement and a recorded proof, and three
commits that never push.

Core data flow (3 sentences): `scripts/trivy-scan.sh image` builds the image from ./Dockerfile, exports it with `docker save`,
lists the packages dpkg reports inside it, and runs the digest-pinned Trivy container over the tar for OS packages only, while
`scripts/trivy-scan.sh config` renders each workload-bearing kustomization root with the repo's pinned kubectl and runs Trivy's
misconfiguration checks over those files plus both Dockerfiles. Each mode writes a JSON report and an expected-inputs list
under .trivy-reports/. `scripts/verify-trivy-report.py` compares what the report says Trivy covered with that list and fails
the job when the scan covered too little, and findings alone never fail a job.
</objective>

## Design record (required by .claude/CLAUDE.md before a PLAN.md is approved)

The facts below were read or measured by the planner on 2026-11-09 and are the basis for the choices. The executor
re-verifies the pin before relying on it, and measures its own baseline; the planner's probe numbers are not a baseline.

### Approaches considered

1. **Digest-pinned `ghcr.io/aquasecurity/trivy` container run by one repo script, called by a weekly report-only workflow;
   image scan limited to OS packages of a fresh build; config scan over rendered roots and Dockerfiles; coverage guard plus
   selftest.** Picked.
2. **`aquasecurity/trivy-action` pinned by full commit SHA.** Not picked.
3. **A sha256-pinned release tarball downloaded on demand, the way scripts/verify-k8s-manifests.sh handles kubectl and
   kubeconform.** Not picked.
4. **Scope variants for what the image scan reads:** the runtime base image alone with no build, the image already deployed
   (tag read from k8s/overlays/prod/kustomization.yaml), and library scanning of the jar inside the image. Not picked.

### Trade-off matrix

| Approach | Pros / Cons | Why Picked |
|---|---|---|
| 1. Digest-pinned container, one script, weekly report-only workflow, OS-only image scan, guard plus selftest | + Same pinning mechanism as the repo's gitleaks and TruffleHog images, and the same command runs locally and in CI. + The digest was read from ghcr.io and Docker Hub and matched. + No third-party action in the job. + The guard turns "ran and found nothing" into a failure, which is the 2026-08-19 failure class. - Adds two Python files and one shell script that must be kept. - Weekly cadence means a new OS CVE waits up to a week. | Smallest trust surface and full local/CI parity; the maintenance cost is the price of a guard this repo has already needed once. |
| 2. `trivy-action` pinned by SHA | + Built-in database caching and SARIF upload. + Less code in the repo. - Advisory history: in March 2026, 76 of 77 `trivy-action` version tags and all `setup-trivy` tags were force-pushed to credential-stealing code (GHSA-69fq-xp46-6x23, critical), and an earlier command-injection advisory (GHSA-9p44-j4g5-cfx5, versions 0.31.0 through 0.34.0) exists. - SHA pinning protects the action itself, but the action still downloads the Trivy binary at run time and goes through `setup-trivy`, so the chain to audit is longer than one digest. - The action cannot run on a developer machine, so local and CI would diverge. | The advisory history plus the runtime fetch chain outweigh the caching convenience. |
| 3. sha256-pinned tarball | + Same pattern as kubectl here, and no Docker needed to run Trivy. - Needs per-platform extraction code and cache management. - The checksum sources are the release's checksums file (with a Sigstore bundle) and GitHub's asset digest, all served by GitHub, while the image digest was cross-checked on two registries. | A tie on safety; the image path reuses the repo's scanner convention and needs no extraction code. |
| 4a. Scan the base image alone | + No Gradle build, so cheaper and quicker. - Silently misses any OS package a later Dockerfile edit adds. | Faithfulness to the Dockerfile at the commit is worth the weekly build. |
| 4b. Scan the deployed image tag | + Answers "what runs in prod" exactly. - Needs tag parsing and registry reads, and depends on the deploy having succeeded. | Not chosen now. The Known holes section and the runbook state the gap: a fresh build shows what a rebuild would ship, not the OS layer already running. |
| 4c. Add `library` package scanning of the jar | + A GitHub-advisory-based second opinion on Java libraries. - Reports the libraries dependency-check and Dependabot already cover, so triage would see them three times. | The brief is to complement, not duplicate, so the scan passes `--pkg-types os`. |

### Non-obvious trade-offs

- **Fresh build versus running image.** The scan builds the Dockerfile now (`docker build --pull`, so a cached local base layer cannot make a stale scan look current). The prod pod runs the OS layer from its own build time. A finding fixed in a newer jammy layer will therefore show as absent here while still present in prod until the next deploy. The runbook must say this.
- **Cost of the weekly build.** The CI job runs a full cold Gradle `bootJar` inside `docker build`. The planner measured only the scan phase, on the base-image tar: 3m29s wall on the maintainer's machine with a cold vulnerability database, download not separated out. Task 1 measures build and scan separately, and Task 2 sizes `timeout-minutes` from those numbers as a runaway guard, not a forecast.
- **Security of the scanner process.** The Trivy container runs as the invoking uid:gid (not root), gets no Docker socket (the image goes in as a `docker save` tar through `--input`), and bind-mounts the checkout read-write only because it writes reports under .trivy-reports/. The workflow token is `contents: read`, both jobs hold no secrets and log in to no registry, and each checkout sets `persist-credentials: false` so no token sits in `.git/config` inside the mount.
- **The vulnerability database is not pinned and cannot be.** A pinned database is a scanner that stops finding new CVEs. The script never passes `--db-repository` or `--java-db-repository`, so Trivy uses its defaults (mirror.gcr.io then ghcr.io); the pinned version is above the release that fixed the path-traversal advisory on those flags (GHSA-mcj4-mphf-j9ff, fixed in 0.71.1).
- **Which verdicts drift.** The image verdict changes with the database, so it is weekly and never per-push, like security-scan.yml. The config verdict is a pure function of the rendered manifests and the pinned binary because the script passes `--skip-check-update` (embedded checks only, no download). That is the property that would let it join invariant-checks.yml later; the checks then age with the pin, and bumping the pin refreshes them.
- **Time and memory of the guard.** Linear in the report size (a 660 KB JSON in the probe), stdlib only, no measurable cost.

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@.github/workflows/security-scan.yml
@.github/workflows/invariant-checks.yml
@scripts/verify-k8s-manifests.sh
@scripts/verify-dependency-check-report.py
@scripts/verify-dependency-check-report-selftest.py

Facts established by the planner (2026-11-09), each read or run, not remembered:

- Trivy v0.76.2 was published 2026-11-05T12:50:25Z and the GitHub release reports `isImmutable: true`. The manifest digest of `ghcr.io/aquasecurity/trivy:0.76.2` and of `aquasec/trivy:0.76.2` is the same value, sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb. The pulled container prints `Version: 0.76.2`. The GitHub tag has a leading `v`; the registry tag does not.
- Advisory history, from `gh api repos/aquasecurity/trivy/security-advisories`: GHSA-69fq-xp46-6x23 (critical, 2026-03-21) covers a malicious v0.69.4 binary and image, hijacked `trivy-action` and `setup-trivy` tags, and Docker Hub images 0.69.5 and 0.69.6; it says images referenced by digest were not affected. Fixes that bound the minimum safe pin: Helm tar bomb in `trivy config` fixed in 0.71.0, path traversal through a crafted database fixed in 0.71.1, plugin path traversal fixed in 0.72.0. Any pin at 0.72.0 or later covers all three; 0.76.2 does.
- Flags confirmed in the pinned container's `--help`: image takes `--input`, `--pkg-types` (values os, library), `--scanners`, `--list-all-pkgs` (default true), `--ignorefile` (default `.trivyignore`), `--exit-code` (default 0), `--skip-version-check`, `--quiet`, `-f`, `-o`. config takes `--skip-check-update`, `--skip-version-check`, `--quiet`, `-f`, `-o`. The default database repositories are mirror.gcr.io/aquasec/trivy-db:2 then ghcr.io/aquasecurity/trivy-db:2.
- `docker buildx` is not installed on the maintainer's machine (Docker 29.1.3), so the script uses plain `docker build`.
- Image report shape, from a scan of `eclipse-temurin:21-jre-jammy` as a `docker save` tar run with `--user "$(id -u):$(id -g)" -e TRIVY_CACHE_DIR=/tmp/trivy-cache`: top-level keys include SchemaVersion, ArtifactName, Metadata, Results. `Metadata.OS` is an object with Family and Name (ubuntu, 22.04). `Results[0]` has Target, Class `os-pkgs`, Type `ubuntu`, Packages (each with Name) and Vulnerabilities (each with VulnerabilityID, PkgName, Severity, Status, FixedVersion). The report listed 143 packages and dpkg listed 143 installed packages in the same image (`dpkg-query -W -f '${db:Status-Abbrev}${binary:Package}\n'` filtered to the `ii` status), so dpkg is a valid denominator. The probe found 2 HIGH, 22 MEDIUM and 16 LOW findings and no CRITICAL on the base image; that is a probe of the base, not the baseline for the built image.
- Config report shape, from a scan of the rendered prod, nonprod and data/postgres roots plus both Dockerfiles copied into one directory: one Result per file with Target (a path relative to the scanned directory), Class `config`, Type `dockerfile` or `kubernetes`, and MisconfSummary (Successes, Failures). A clean file still appears, with Failures 0 and no Misconfigurations key. The findings included HIGH severity ones, so the baseline will not be empty.
- Kustomization roots under k8s/ and what each renders, measured with the pinned kubectl: base/app (Deployment, Service), base/architecture-site (Deployment, Service), base/redpanda (Service, StatefulSet), data/postgres (ConfigMap, CronJob, NetworkPolicy, Service, StatefulSet), data/postgres-bridge (EndpointSlice, Service), flux-system (Flux's own export: CRDs, controllers, RBAC), monitoring/configs (ConfigMap, IngressRoute, Middleware, PodMonitor), monitoring/controllers (HelmRelease, HelmRepository), overlays/architecture, overlays/nonprod and overlays/prod (each renders a Deployment, and nonprod and prod also a StatefulSet), platform (HelmChartConfig, Namespace), platform/cert-manager (HelmRelease, HelmRepository), platform/edge (Certificate, ClusterIssuer). The workload-bearing, repo-authored roots are the three overlays and data/postgres.
- `bash scripts/verify-k8s-manifests.sh --tool kubectl` prints the path of the pinned, sha256-verified kubectl on its last stdout line.
- `.github/workflows/deploy.yml` ignores only docs/**, **/*.md, .planning/** and k8s/** on push to main, so the scripts, workflow and .gitignore commits made here will trigger the full deploy pipeline when the user pushes them. Say so in the SUMMARY; never push.
- The pre-commit hook scans the staged diff with: `git diff --cached | docker run --rm -i ghcr.io/gitleaks/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f stdin --redact --no-banner --verbose --exit-code 2` (exit 0 clean, 2 leaks found). The yq expressions used in the verify blocks were tested on a sample workflow.
- The working tree already holds unrelated changes (deleted files under docs/data-engineering-extension/source-research/, untracked task output under .planning/quick/261024-*, and docs/architecture/record-demo.mjs). Never stage them; name them in the SUMMARY.

Commit protocol (every task ends with it): stage exactly that task's paths with `git add -- <paths>`, confirm `git diff --cached --stat` is non-empty (gitleaks on an empty stream reads clean), run the gitleaks command above in a shell with `set -o pipefail` so a failing `git diff` fails the scan instead of being swallowed (must exit 0), run `python3 scripts/verify-comments.py check` and `python3 scripts/verify-instruction-budget.py` (both must exit 0), then `git commit --no-verify -m "<conventional message saying why>" -- <paths>`. `--no-verify` is allowed here because these commits hold only scripts, a workflow, docs and planning files, and the three gates just run are the hook's non-Java gates; spotlessCheck and fastTest cover Java, which these commits do not touch. End the message with the attribution line the executor's own instructions specify. Never push, never amend.

Comment rules for every new file (docs/CODE_STYLE.md rule 14, enforced by scripts/verify-comments.py): plain text, identifiers by bare name, a blank comment line between paragraphs, `- ` for list items, a block of four or more prose lines opens with a summary of at most two lines, long records sit behind `Decisions:` or `Known holes:` markers, and no quick-task, plan or decision id appears in any comment.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Image scan end to end locally: guard and selftest first, then scripts/trivy-scan.sh image</name>
  <files>scripts/verify-trivy-report-selftest.py, scripts/verify-trivy-report.py, scripts/trivy-scan.sh, .gitignore, .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt, .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-proof.txt</files>
  <read_first>
    scripts/verify-dependency-check-report.py (docstring style, exit codes, --summary handling)
    scripts/verify-dependency-check-report-selftest.py (subprocess cases, the --guard option)
    scripts/verify-k8s-manifests.sh (header comment style, log and fail helpers, pinned-tool notes)
    .gitignore (the gitleaks report entry near line 49)
  </read_first>
  <behavior>
    - Image kind passes when the report names an OS family and lists at least ceil(expected x 0.95) unique OS package names against the expected list; exit 0, and the output states the scanned and expected counts.
    - Image kind at exactly the floor passes and one below it fails (exit 1, message names both counts).
    - Image kind fails on a report with no Results (the nothing-scanned case), on Results but no Metadata.OS family, and on an empty expected list.
    - Image summary counts vulnerabilities by severity as the report wrote them plus a count with a non-empty FixedVersion; a fully listed report with no vulnerabilities passes with zero counts.
    - Config kind passes when every name in the expected list is the Target of a Class config Result whose MisconfSummary Successes plus Failures is above zero; a file with no Misconfigurations key counts as scanned.
    - Config kind fails when an expected name is absent (message names it) or present with Successes plus Failures equal to zero.
    - Config summary counts failures by severity, and per Type, from Misconfigurations.
    - `--summary` appends the markdown table to the named file on pass and on fail; an unreadable or non-JSON report exits 2.
  </behavior>
  <action>
Work red to green, then wire the one path through every layer (build, export, denominator, pinned scan, guard).

1. Selftest first (red). Create scripts/verify-trivy-report-selftest.py in the shape of the dependency-check selftest: each case plants a report JSON and an expected-list file in a temporary directory, runs the guard as a `python3 -I` subprocess, and asserts the exit code plus a fragment of its output. Support `--guard <path>` to aim the same cases at another copy of the guard; the default is scripts/verify-trivy-report.py beside it. Build the fixtures as dicts in Python using the shapes in the context block (Metadata.OS.Family, Results[].Class, Type, Packages[].Name, Vulnerabilities[].Severity and FixedVersion; for config, Results[].Target, Class, Type, MisconfSummary, optional Misconfigurations[].ID and Severity). Implement every case in the behavior block, and compute the image floor in the selftest with the same ceil(expected x 0.95) formula so the boundary cases move with the margin. Run it before the guard exists and capture the failing exit code and first lines into 261109-vzu-proof.txt as "P1 selftest red without the guard".
2. Guard (green). Create scripts/verify-trivy-report.py, stdlib only. Arguments: `--kind` (image or config, required), `--report`, `--expected` (a text file with one name per line: dpkg package names for image, staged file names for config; blank lines ignored, duplicates collapsed), `--tolerance` (default 0.05, image kind only), `--summary` (optional file to append to). Exit 0 pass, 1 guard failure, 2 usage or input error (unreadable file, invalid JSON). Always print the markdown table before the verdict line, and append it to --summary when given, on pass and on fail. Image rules: the report names Metadata.OS.Family, the expected list is non-empty, and the count of unique Packages[].Name across Results of Class os-pkgs is at least ceil(expected x (1 - tolerance)); a missing Results key counts as zero scanned. Image table: OS family and name, expected and scanned counts, vulnerabilities by severity (CRITICAL, HIGH, MEDIUM, LOW, UNKNOWN first, any other word after, upper-cased and otherwise as written), and the number with a non-empty FixedVersion. Config rules: every expected name appears as the Target of a Class config Result with Successes plus Failures above zero; a missing Misconfigurations key means no failures. Config table: files per Type and failures by severity per Type, plus one line per Target with its failure count. Write the docstring in the style of the dependency-check guard: a two-line summary, then `Decisions:` and `Known holes:`. Decisions to record: count-based because the failure guarded against was a count (a scan reported green after covering nothing on 2026-08-19); the denominator comes from the same run, never a fixed number; 95 percent is a chosen margin, and the measured parity on the base image was 143 of 143 on 2026-11-09, so a shortfall is a signal to diagnose rather than to loosen; every severity word is reported as written. Known holes to record: counts are compared, not identities; the report carries no database date, so a stale database does not fail the guard; findings suppressed through a .trivyignore are absent from the table; library packages are out of scope by the scan's flag; an image without dpkg gives an empty expected list and fails loudly, so a non-Debian base needs a new denominator source. Re-run the selftest: it must exit 0.
3. Mutation proof. Copy the guard to 261109-vzu-guard-mutant.py in the task directory, use Edit on the copy to neutralise the image floor comparison so it always passes, and run the selftest with `--guard` pointing at the copy: it must exit 1. Record the exit code and the failing case name in 261109-vzu-proof.txt as "P2 mutation: floor removed", then remove the copy with trash-put. A selftest that exits 0 against the mutant covers nothing adjacent to the floor and must be fixed before continuing.
4. Script. Create scripts/trivy-scan.sh (bash, `set -euo pipefail`, repo-root derived from BASH_SOURCE like verify-k8s-manifests.sh, `log` and `fail` helpers; bash and not fish because the CI runner has no fish and this repo's other gate scripts are bash). Hold the pin once as `TRIVY_IMAGE`, set to ghcr.io/aquasecurity/trivy:0.76.2 with the digest sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb, after re-reading the digest from the registry (an anonymous manifest HEAD against ghcr.io and Docker Hub, as the planner did) and confirming the release is still immutable and that no advisory newer than GHSA-8rc5-4fr6-64pw lists 0.76.2 as affected (`gh api repos/aquasecurity/trivy/security-advisories`); if a newer release exists the pin stays at 0.76.2 unless an advisory forces a bump, in which case record the new value and the reason in 261109-vzu-measure.txt and use it everywhere. Define `REPORT_DIR` as .trivy-reports and `SCAN_TAG` as kanban-trivy-scan:local. Accept a mode argument; in this task only `image` exists, and any other value prints usage and exits 2. Image mode: remove this mode's old outputs first (image.json, image-packages.txt, image.tar) so a stale report cannot satisfy the guard, and install a trap that removes image.tar and the SCAN_TAG image on exit; run `docker build --pull -t "$SCAN_TAG"` on the repo root; run `docker save -o` into REPORT_DIR; write image-packages.txt from `docker run --rm --entrypoint dpkg-query "$SCAN_TAG" -W -f '${db:Status-Abbrev}${binary:Package}\n'` keeping only lines whose status field is `ii` and printing the package name; then run Trivy through one `run_trivy` helper that wraps `docker run --rm --user "$(id -u):$(id -g)" -e TRIVY_CACHE_DIR=/tmp/trivy-cache -v "$REPO_ROOT:/repo" -w /repo "$TRIVY_IMAGE"`. The scan arguments are `image --input` on the saved tar, `--pkg-types os`, `--scanners vuln`, `--list-all-pkgs`, `--quiet`, `--skip-version-check`, and `-f json -o` into REPORT_DIR as image.json; do not pass `--exit-code`, so findings leave the exit status 0, and never pass `--db-repository` or `--java-db-repository`. The working directory is the repo root so a future `.trivyignore` there is loaded with no extra wiring. Print the guard command to run next as the last log line. Write the header comment as a two-line summary, then `Decisions:` (pin by digest from two registries and why not trivy-action, citing the March 2026 advisory by its GHSA id; OS packages only so Java libraries are not reported a third time; `docker save` plus `--input` so no Docker socket reaches the scanner; the invoking uid and TRIVY_CACHE_DIR so report files are not root-owned; `--pull` so a cached base layer cannot make a stale scan look current; report-only because no triaged baseline exists) and `Known holes:` (database not pinned by design; a fresh build shows what a rebuild would ship, not the OS layer running in prod; anonymous Docker Hub pulls of the Gradle and Temurin base images may be rate limited on a shared runner and this was not measured; library and secret scanning are covered by other gates; a base image without dpkg breaks the denominator).
5. Ignore rule. Add `.trivy-reports/` to .gitignore beside the gitleaks report entry with a short comment saying it is scratch scan output that holds the saved image tar.
6. Run it for real and measure. Run `bash scripts/trivy-scan.sh image` with a tool timeout that fits a cold Gradle build (run it in the background and read its exit code; the scan phase alone took 3m29s in the planner's base-image probe), timing the build step and the scan step separately. Then run the guard exactly as the script's last line prints. Write 261109-vzu-measure.txt with: the Docker, Trivy and pinned-digest values actually used, build seconds, scan seconds, expected and scanned package counts, vulnerabilities by severity, how many have a fix, and the OS family and name reported. Confirm .trivy-reports/image.tar is gone afterwards and that `docker images` no longer lists kanban-trivy-scan.
7. Commit per the Commit protocol with the paths in this task's files list. Subject starts with `ci(trivy)`.
  </action>
  <verify>
    <automated>python3 -I scripts/verify-trivy-report-selftest.py && bash scripts/trivy-scan.sh image && python3 -I scripts/verify-trivy-report.py --kind image --report .trivy-reports/image.json --expected .trivy-reports/image-packages.txt && grep -q -F 'ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb' scripts/trivy-scan.sh && ! (grep -v '^[[:space:]]*#' scripts/trivy-scan.sh | grep -E 'aquasec(urity)?/trivy:' | grep -v '@sha256:') && git check-ignore -q .trivy-reports/image.json && test ! -e .trivy-reports/image.tar</automated>
  </verify>
  <done>The selftest exits 0, and exited 1 against the mutated copy and non-zero before the guard existed (both recorded in 261109-vzu-proof.txt). `bash scripts/trivy-scan.sh image` exits 0 end to end, the guard prints a table whose scanned count meets its floor, and 261109-vzu-measure.txt holds the measured build time, scan time and image baseline. The only Trivy reference outside comments carries a digest, .trivy-reports/ is ignored, the saved tar and the scan image tag are gone, and the commit holds only this task's paths. The end-to-end command takes minutes, not seconds, because it builds the image.</done>
</task>

<task type="auto">
  <name>Task 2: Config mode with root-drift guard, and the weekly report-only workflow</name>
  <files>scripts/trivy-scan.sh, .github/workflows/trivy-scan.yml, .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt, .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-proof.txt</files>
  <read_first>
    scripts/trivy-scan.sh (the run_trivy helper and header from Task 1)
    scripts/verify-k8s-manifests.sh (the --tool kubectl contract and its KUBECONFORM_SKIP_KINDS naming stance)
    .github/workflows/security-scan.yml (header style, selftest-first step, guard step condition, artifact upload)
    .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt (build and scan seconds measured in Task 1)
  </read_first>
  <action>
1. Config mode in scripts/trivy-scan.sh. Add `config` to the mode dispatch. Define two named arrays near the top, each entry with a short reason in a comment: `SCAN_ROOTS` holding k8s/overlays/prod, k8s/overlays/nonprod, k8s/overlays/architecture and k8s/data/postgres (the repo-authored roots that render pod-bearing kinds); `SKIP_ROOTS` holding k8s/base/app, k8s/base/architecture-site and k8s/base/redpanda (rendered inside the overlays, so scanning them alone double-counts), k8s/flux-system (Flux's own unmodified export, the same stance as the CustomResourceDefinition skip in verify-k8s-manifests.sh), and k8s/data/postgres-bridge, k8s/monitoring/configs, k8s/monitoring/controllers, k8s/platform, k8s/platform/cert-manager and k8s/platform/edge (they render no pod-bearing kind; the workloads their HelmReleases install come from upstream charts that are rendered in the cluster, which is a Known hole). Drift guard: discover every directory under k8s/ that holds a kustomization.yaml with `find`, sort it, and compare it symmetrically (for example `comm -3`) with the sorted union of both arrays; any difference fails with a message naming each unclassified or stale root and telling the reader to add it to SCAN_ROOTS or SKIP_ROOTS with a reason. Then clear this mode's old outputs (config.json, config-expected.txt, config-input/), create config-input under REPORT_DIR, get the kubectl path from `bash scripts/verify-k8s-manifests.sh --tool kubectl` (last stdout line), render each SCAN_ROOT with `kustomize` into config-input as a file named after the root with slashes turned into dashes plus `.yaml`, copy ./Dockerfile to config-input/Dockerfile and k8s/data/postgres/backup/Dockerfile to config-input/backup.Dockerfile, and write config-expected.txt with the name of every staged file, one per line. Run Trivy through run_trivy with `config --skip-check-update --quiet --skip-version-check -f json -o` into REPORT_DIR as config.json over the config-input directory, with no `--exit-code`. Add to the header Decisions: why those four roots and the rendered form (raw patch files would be scanned as partial manifests and report findings on fragments); why `--skip-check-update` (embedded checks only, so the verdict is a pure function of the manifests and the pin, and the checks age with the pin); why the same script and helper as image mode. Add to Known holes: HelmRelease-installed workloads are not scanned; the embedded checks are as old as the pinned binary; the scan reads rendered output only, so a base edited without a rendering root is invisible until a root is listed.
2. Root-drift proof. Create the throwaway directory k8s/zz-drift-probe holding a minimal kustomization.yaml, run `bash scripts/trivy-scan.sh config` and capture its exit code and the stderr line naming the probe directory into 261109-vzu-proof.txt as "P3 root drift fires", remove the directory with trash-put, and confirm `git status --short k8s` prints nothing for it. Never stage it. Then run config mode on the clean tree: it must exit 0.
3. Run config mode for real and run the guard as `python3 -I scripts/verify-trivy-report.py --kind config --report .trivy-reports/config.json --expected .trivy-reports/config-expected.txt`. Append to 261109-vzu-measure.txt: config scan seconds, the number of staged files, failures by severity per Type, and per-Target failure counts, labelled as the config baseline with the date.
4. Workflow. Create .github/workflows/trivy-scan.yml named `Trivy Scan`, triggered by `workflow_dispatch` and a weekly `schedule` (Mondays 07:00 UTC, an hour after the dependency-check run so both reports arrive together), with no push or pull_request trigger, and `permissions: contents: read`. Two jobs, `image-scan` and `config-scan`, each on ubuntu-latest, each with these steps in this order: `actions/checkout@v5` with `persist-credentials: false`; a selftest step running `python3 -I scripts/verify-trivy-report-selftest.py`; a scan step with an `id`, running `bash scripts/trivy-scan.sh image` or `bash scripts/trivy-scan.sh config` with its own `timeout-minutes`; a guard step with `if: always() && steps.<scan id>.outcome != 'skipped'` running the guard with `--summary "$GITHUB_STEP_SUMMARY"` and the same `--report` and `--expected` paths as locally; and an `actions/upload-artifact@v4` step with `if: always()` and `if-no-files-found: warn` that names files, never the directory. The image job uploads image.json and image-packages.txt as artifact `trivy-image-report` and never the saved tar; the config job uploads config.json and config-expected.txt as `trivy-config-report`. Size each scan step's `timeout-minutes` and each job's `timeout-minutes` at about three times the matching measured local seconds in 261109-vzu-measure.txt, rounded up to a multiple of five minutes, and say in a comment that these are runaway guards sized from a local measurement, not a runner measurement. No third-party action, no secret, no registry login, no `continue-on-error`. Write the header as a two-line summary, then `Decisions:` (no push or pull_request trigger because the image verdict drifts with the vulnerability database and a report-only per-push run has no gating power; the config verdict is deterministic, so graduating it to invariant-checks.yml is the later step once a baseline is triaged and a gate is wanted; the Trivy container instead of trivy-action with the March 2026 advisory id; selftest first, as every gate here; findings never fail a job, so a red job means the scan or the guard failed; `persist-credentials: false` because the scanner container mounts the checkout) and `Known holes:` (the workflow was not run on a GitHub runner when committed, since the executor never pushes, so the first `gh workflow run` after a push is the real test; anonymous Docker Hub pulls may be rate limited; fresh build versus running image; database unpinned; HelmRelease workloads and Java libraries are scanned elsewhere or not at all).
5. Commit per the Commit protocol with this task's repo paths (scripts/trivy-scan.sh, .github/workflows/trivy-scan.yml and the two .planning files). Subject starts with `ci(trivy)`.
  </action>
  <verify>
    <automated>bash scripts/trivy-scan.sh config && python3 -I scripts/verify-trivy-report.py --kind config --report .trivy-reports/config.json --expected .trivy-reports/config-expected.txt && test "$(yq -r '(.jobs | keys | sort | join(","))' .github/workflows/trivy-scan.yml)" = "config-scan,image-scan" && test "$(yq -r '(.on | keys | sort | join(","))' .github/workflows/trivy-scan.yml)" = "schedule,workflow_dispatch" && test "$(yq -r '[.jobs[].steps[].uses | select(. != null)] | unique | join(",")' .github/workflows/trivy-scan.yml)" = "actions/checkout@v5,actions/upload-artifact@v4" && test "$(yq -r '[.jobs[].steps[] | select(.uses == "actions/checkout@v5") | .with."persist-credentials"] | join(",")' .github/workflows/trivy-scan.yml)" = "false,false" && test "$(yq -r '.permissions.contents' .github/workflows/trivy-scan.yml)" = "read" && git status --short k8s | wc -l | grep -q '^0$'</automated>
  </verify>
  <done>Config mode exits 0 on the clean tree and the guard confirms every staged file (two Dockerfiles and four rendered roots) appears in the report. A kustomization root that is neither scanned nor skipped makes the script exit non-zero and name it (P3 recorded, probe directory removed, k8s/ untouched). The workflow has exactly the two jobs, schedule and dispatch triggers, only the two first-party actions, read-only permissions, credential-free checkouts, selftest before scan in each job, and timeouts derived from the recorded measurements. 261109-vzu-measure.txt now holds the config baseline. The commit holds only this task's paths.</done>
</task>

<task type="auto">
  <name>Task 3: Document both scans in the README gate table and the runbook</name>
  <files>README.md, docs/INFRA_RUNBOOK.md</files>
  <read_first>
    README.md (the "Quality & security gates" table and the closing See-also paragraph, lines 212 to 245)
    docs/INFRA_RUNBOOK.md (the "Uptime alert triage" section, lines 53 to 83, as the format model)
    .planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt
  </read_first>
  <action>
1. README.md. Add one row to the CI gate table directly after the OWASP dependency-check row: gate `Trivy` (image OS packages and Dockerfile/manifest misconfiguration), posture `Report-only, weekly + on demand`, and a why cell saying it scans the OS layer of the image built from the Dockerfile and Trivy's misconfiguration checks on the Dockerfiles and rendered workload manifests; that Java libraries stay with dependency-check and Dependabot; that findings never fail it but a guard fails the job when the scan covered too little; and that the image is pinned by digest, with no third-party action. Keep the table's existing column widths as closely as the other rows. Extend the closing See-also paragraph with a pointer to the new runbook section.
2. docs/INFRA_RUNBOOK.md. Insert a section `## Trivy scan triage` between the end of "Uptime alert triage" and `## Firewall — three layers`, in the same terse bold-lead-in format. It must state, in this order: (a) what runs, naming `.github/workflows/trivy-scan.yml`, its jobs `image-scan` and `config-scan`, the artifacts `trivy-image-report` and `trivy-config-report`, and the local commands `bash scripts/trivy-scan.sh image` and `bash scripts/trivy-scan.sh config` followed by the guard command; (b) a small table mapping which step is red to what it means: selftest red means the guard was edited into something that cannot fire, scan step red means Trivy or the build could not run (network, database download, build), guard red means the scan covered less than the image or the staged files held, and findings alone never turn a run red; (c) acting on an image finding: most are cleared by a rebuild that pulls a newer base layer, an empty fixed version means nothing to apply yet, and the prod pod runs the OS layer from its own build (the tag is in `k8s/overlays/prod/kustomization.yaml`) while the scan builds fresh, so a finding can be gone in the scan and still live in prod; (d) suppression: a `.trivyignore` at the repo root is loaded automatically because the scanner's working directory is the repo root, none exists today, and every entry must carry a comment citing the advisory and the evidence for ignoring it; (e) bumping the pin, as numbered steps: read the release page and confirm the release is immutable, read the manifest digest from ghcr.io and Docker Hub and require them equal, list advisories with `gh api repos/aquasecurity/trivy/security-advisories` and require the new version above every patched threshold, edit the one `TRIVY_IMAGE` value in scripts/trivy-scan.sh, rerun both local modes and compare with the baseline in the quick-task measurement file, with one sentence on why (March 2026 compromise of the binary, the action tags and two Docker Hub tags, with images referenced by digest unaffected); (f) status: not a gate yet, the dated baseline lives in `.planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-measure.txt`, and moving a mode to a gate means triaging that baseline first and then choosing an `--exit-code` and `--severity` policy as a separate decision. Copy no baseline numbers into the runbook; point at the measurement file so the figures cannot go stale in two places.
3. Final checks and commit. Run the selftest, `python3 scripts/verify-comments.py check`, `python3 scripts/verify-instruction-budget.py`, and `yq '.' .github/workflows/trivy-scan.yml` (it must parse). Confirm `git log --stat -3` lists only the paths the three tasks name and none of the unrelated pending changes. Commit README.md and docs/INFRA_RUNBOOK.md per the Commit protocol with a subject starting `docs(trivy)`.
4. Write the SUMMARY with: the pinned version and digest actually used, the build, scan and config durations, the image and config baselines, the three commit SHAs, a statement that the workflow was never run on a runner, a statement that pushing these commits triggers deploy.yml (the workflow, scripts and .gitignore are not in its ignore list) and so rolls prod and nonprod, the first command to run after a push (`gh workflow run trivy-scan.yml`), and the unrelated pending changes left unstaged.
  </action>
  <verify>
    <automated>python3 -I scripts/verify-trivy-report-selftest.py && python3 scripts/verify-comments.py check && python3 scripts/verify-instruction-budget.py && yq '.' .github/workflows/trivy-scan.yml > /dev/null && rg -q -F 'Trivy' README.md && rg -q '^## Trivy scan triage' docs/INFRA_RUNBOOK.md && rg -q -F 'TRIVY_IMAGE' docs/INFRA_RUNBOOK.md && rg -q -F 'trivy-scan.yml' docs/INFRA_RUNBOOK.md</automated>
  </verify>
  <done>README.md's gate table has a Trivy row and the runbook has a `## Trivy scan triage` section covering what runs, how to read a red step, how to act on an image finding, the suppression rule, the pin-bump procedure and the not-yet-a-gate status, with no baseline numbers copied into it. The selftest, comment lint and instruction-budget gate pass, the workflow parses, and the docs commit holds only README.md and docs/INFRA_RUNBOOK.md.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Registries to the runner | The Trivy container image is pulled and executed on developer machines and CI runners, and it downloads a vulnerability database at run time |
| Scanner process to the checkout | The container bind-mounts the repository and writes reports into it |
| Scan to verdict | A report is the only evidence a scan covered the image and the manifests |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261109-vzu-SC | Tampering | The Trivy image and its vulnerability database | high | mitigate | Pin by digest, read from ghcr.io and Docker Hub and required equal, on a release that is immutable on GitHub and above every patched threshold in the advisory list (0.72.0 and later). Never use `trivy-action` or `setup-trivy`, whose tags were hijacked in March 2026. Never override the database repositories. The pin-bump procedure is in the runbook. The npm/pip/cargo package-legitimacy gate does not apply: no package-manager install is added. |
| T-261109-vzu-01 | Information Disclosure | Checkout mounted into the scanner container | medium | mitigate | The container runs as the invoking uid:gid with no Docker socket. Each checkout sets `persist-credentials: false`, so no token is in `.git/config` inside the mount. The workflow token is `contents: read`, and neither job holds a secret or logs in to a registry. |
| T-261109-vzu-02 | Repudiation | A scan that covered nothing and reports clean | high | mitigate | The guard compares the report with a denominator taken from the same run (dpkg inside the built image, the list of staged files) and fails below 95 percent; the selftest runs first in each job and exited 1 against a floor-removed mutant; each mode deletes its previous outputs before running so a stale report cannot pass. |
| T-261109-vzu-03 | Tampering | A new kustomization root silently left unscanned | medium | mitigate | Config mode fails when a discovered root is in neither SCAN_ROOTS nor SKIP_ROOTS, in either direction, and the failure is proven with a throwaway root. |
| T-261109-vzu-04 | Information Disclosure | Uploaded report artifacts | low | accept | The image scan runs the `vuln` scanner only and the config scan reads committed, rendered manifests that hold no Secret objects, so the reports hold package names, advisory ids and finding ids that repository readers could derive anyway. The saved image tar is never uploaded. |
| T-261109-vzu-05 | Denial of Service | A hung build, database download or scan on a runner | low | mitigate | Step and job timeouts sized from the recorded local measurement, labelled as runaway guards. |
</threat_model>

<verification>
- `python3 -I scripts/verify-trivy-report-selftest.py` exits 0, and 261109-vzu-proof.txt records the red run before the guard, the exit 1 against the floor-removed mutant, and the root-drift failure with its exit code.
- `bash scripts/trivy-scan.sh image` and `bash scripts/trivy-scan.sh config` each exit 0, and each guard run prints a table with scanned counts at or above its floor.
- The workflow parses with yq and passes the five structural checks in Task 2's verify command.
- 261109-vzu-measure.txt holds the tool versions, the measured durations and both baselines, and the runbook points at it instead of copying numbers.
- `python3 scripts/verify-comments.py check` and `python3 scripts/verify-instruction-budget.py` exit 0.
- `git log --stat -3` shows three commits touching only this plan's paths, with the unrelated pending changes still unstaged.
</verification>

<success_criteria>
- Trivy runs from one digest-pinned container through one script, locally and in a weekly report-only workflow, and the OS packages of the Dockerfile-built image and the Dockerfile and rendered-manifest misconfigurations are scanned.
- A scan that covered less than the image or the staged files holds is a failed job, proven by a selftest that itself fails when the guard is weakened.
- Java libraries and secrets are left to the gates that already cover them, so nothing is reported twice.
- A new k8s root cannot slip past the config scan unnoticed.
- The README and runbook tell a maintainer how to read a run, act on a finding, suppress with evidence and bump the pin, and the baselines are recorded once, with a date, in the measurement file.
</success_criteria>

<output>
Create `.planning/quick/261109-vzu-setup-trivy-for-this-project/261109-vzu-SUMMARY.md` when done. Include the pinned version and digest, the build, scan and config durations, both baselines, the three commit SHAs, that the workflow was not run on a runner, that pushing triggers deploy.yml, the first command to run after a push, and the unrelated pending changes left unstaged.
</output>
