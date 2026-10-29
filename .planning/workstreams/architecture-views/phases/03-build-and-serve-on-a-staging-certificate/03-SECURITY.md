---
phase: "03"
slug: build-and-serve-on-a-staging-certificate
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-29"
---

# Phase 03 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail. Audited 2026-10-29 by gsd-security-auditor at ASVS level 1, `block_on: high`. The full evidence trail (file:line per threat) is in the auditor's report; this file records verdicts and the evidence anchors.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| GitHub Actions build job to publish job | Untrusted PR code builds; only the publish job on main holds the registry token | Site bundle artifact, registry credentials |
| Publish job to Docker Hub | Image push for the architecture site | Container image |
| Flux to the cluster | Image tag bump and overlay apply for the architecture namespace | Manifests, image tags |
| Public internet to the architecture host | Static site behind a staging certificate | Published model prose |
| Repo to origin/main | The first push and later pushes start workflows | Commits |

---

## Threat Register

The register was authored at plan time (34 numbered ids plus T-03-SC, repeated in all 8 plans). Evidence marked repo-only rests on the executor's committed record files, not a fresh observation of the live cluster.

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-03-01 | Information disclosure | dev or preview server exposing repo files | high | mitigate | I12 in verify-k8s-invariants.py, run in CI by invariant-checks.yml; red and green records 03-01-i12-*.txt | closed |
| T-03-02 | Elevation of privilege | registry token read by a non-main run | high | mitigate | publish job gated to main with `environment: production`; deployment branch policy is main only (read live 2026-10-29) | closed |
| T-03-03 | Tampering | base image and publish-job actions | medium | mitigate | Dockerfile base pinned by digest; publish-job actions SHA-pinned; frozen lockfile | closed |
| T-03-04 | Tampering | cached 404 for an asset | low | mitigate | nginx.conf `/assets/` location without `always`; local probe record | closed |
| T-03-05 | Elevation of privilege | nginx process | medium | mitigate | non-root, read-only root, drop ALL, seccomp in the Deployment | closed |
| T-03-06 | Tampering | unpatched Alpine userland | low | accept | Accepted risk AR-1; digest pin makes each base change a reviewed commit | closed |
| T-03-07 | Tampering | first push to origin/main | high | mitigate | DECISION Q1; per-commit classification in 03-02-publish.txt; deploy.yml tests gate | closed |
| T-03-08 | Tampering | pushing more than intended | medium | mitigate | explicit-SHA pushes; diff re-read; no force pushes in the activity feed | closed |
| T-03-09 | Denial of service | private or missing Docker Hub repository | medium | mitigate | anonymous GET returns 200 (re-checked 2026-10-29) | closed |
| T-03-10 | Information disclosure | credentials in a record or PR body | medium | mitigate | token-shape search over phase records empty; gitleaks over the phase range clean | closed |
| T-03-11 | Elevation of privilege | drift PR reaching the registry token | high | mitigate | publish skipped on pull_request; PR 21 closed, probe branch absent | closed |
| T-03-12 | Denial of service | ladder pod and cold-load driver | low | mitigate | one pod at a time, 64Mi top rung, namespace cleaned (repo-only) | closed |
| T-03-13 | Denial of service | production Certificate after the issuer edit | medium | mitigate | DECISION Q2; one-path k8s push; Certificate revision and notAfter unchanged (repo-only) | closed |
| T-03-14 | Information disclosure | NodePort per HTTP-01 challenge | low | mitigate | `serviceType: ClusterIP` in both ClusterIssuers | closed |
| T-03-15 | Information disclosure | pod and node IPs in records | low | mitigate | dotted-quad search over records and runbook empty | closed |
| T-03-16 | Tampering | two Flux Kustomizations on one object (I11) | high | mitigate | each object defined once; I11 green | closed |
| T-03-17 | Denial of service | redirect route catching the ACME path (I10) | medium | mitigate | `!PathPrefix` on the redirect route; I10 green | closed |
| T-03-18 | Elevation of privilege | the site container | medium | mitigate | no SA token, ClusterIP Service, hardened securityContext; I1 and I2 green | closed |
| T-03-19 | Information disclosure | model descriptions on the public site | medium | mitigate | leak-words search over the model empty; full leak scan is Phase 4 | closed |
| T-03-20 | Denial of service | production Let's Encrypt rate limits | medium | mitigate | Certificate uses letsencrypt-staging | closed |
| T-03-21 | Information disclosure | unreviewed model behind a staging certificate | medium | accept | Accepted risk AR-2 (DECISION Q5); production flip waits for the leak review | closed |
| T-03-22 | Information disclosure | file read via a dev-server path | high | mitigate | static nginx image, no command override, I12 in CI | closed |
| T-03-23 | Denial of service | existing hosts regressing | medium | mitigate | before and after status identical (repo-only) | closed |
| T-03-24 | Denial of service | Traefik at 64Mi under cold loads | low | mitigate | gzip in nginx, no compress Middleware; restarts unchanged (repo-only) | closed |
| T-03-25 | Tampering | acting on a failed live check without the user | medium | mitigate | prohibition in plan 03-06; no revert commits | closed |
| T-03-26 | Information disclosure | scenario text and evidence quotes | medium | mitigate | leak-words and token-shape searches empty | closed |
| T-03-27 | Repudiation | gate record and standard | medium | mitigate | file:line cites; PENDING written with a reason where unobserved | closed |
| T-03-28 | Tampering | model-only push carrying more | low | mitigate | one commit, one path (a26694f) | closed |
| T-03-29 | Tampering | a drill's sed reaching the real tree | medium | mitigate | `sed --sandbox`; clean `git status` | closed |
| T-03-30 | Repudiation | D25 to D27 presented as red-then-green | medium | mitigate | conventions text and real red record 03-04-model-red.txt | closed |
| T-03-31 | Elevation of privilege | branch workflow naming production or staging | high | mitigate | DECISION Q7; branch policy main-only on both environments; probe branch refused | closed |
| T-03-32 | Repudiation | published prose no gate reads | medium | mitigate | set diffs against .github/workflows recorded in 03-08-prose-proof.txt; evidence row 4; WR-01 fixed in 654c329; residual in AR-3 | closed |
| T-03-33 | Information disclosure | step 4 naming the registry token's users | low | mitigate | model says "registry token", no secret identifier; search empty | closed |
| T-03-34 | Tampering | `--no-verify` skipping the secret scan | low | mitigate | gitleaks run by hand before both 03-08 commits; no allowlist change | closed |
| T-03-SC | Tampering | npm, pip and cargo installs (plans 01 to 08) | high | mitigate | no dependency manifest or lockfile changed; CI installs with a frozen lockfile | closed |

*Status: open · closed · open — below high threshold (non-blocking)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-1 | T-03-06 | Alpine userland debt in the base image; the digest pin makes each change a reviewed commit (03-01-PLAN.md register) | plan 03-01 | 2026-10-27 |
| AR-2 | T-03-21 | The model is readable on a staging certificate before the leak review; production flip is a later commit (DECISION Q5, 03-DECISIONS.txt) | user, DECISION Q5 option 1 | 2026-10-28 |
| AR-3 | T-03-32 (residual) | A later workflow change can make the published prose false without failing a gate; no standing prose test by design (03-08-SUMMARY.md) | plan 03-08 | 2026-10-29 |

*Accepted risks do not resurface in future audit runs.*

---

## Unregistered Observations (no threat id, non-blocking)

- The build job's actions are tag-pinned, not SHA-pinned, while the publish job's are SHA-pinned. T-03-03 scopes pinning to the publish job, so it is closed as declared. No threat id covers integrity of the build-to-publish artifact handoff. Candidate for a later phase.
- GitHub reports two critical Dependabot alerts on every push. Out of scope for this phase (03-02-SUMMARY.md, 03-06-SUMMARY.md).

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-29 | 35 | 35 | 0 | gsd-security-auditor (sonnet), repo evidence plus read-only GitHub and Docker Hub reads; no ssh |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-29
