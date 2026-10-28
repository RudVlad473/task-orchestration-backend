---
phase: "02"
slug: model-completion
status: verified
threats_open: 0
asvs_level: 1
created: "2026-10-28"
---

# Phase 02 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| model text to the published site | Phase 3 builds and serves every title, description and scenario step in docs/architecture/model | hostnames, descriptions (public) |
| drill scratch root to the real tree | drills edit only `docs/architecture/out/drill-root` | repo files (copies) |
| k8s YAML to sync.mjs | the reader parses repo manifests | manifests (parse errors exit 4) |
| IngressRoute match strings to sync.mjs | repo-authored strings read by a regex | host strings |
| local app and Postgres to the evidence file | live output copied into a committed file | headers, logs (cookie values must not cross) |
| workflow and manifest lines to the evidence file | quoted lines committed under .planning | CI/manifest lines |
| the standard to readers | DIAGRAM_CONVENTIONS.md states what is checked and what is authored | claims of coverage |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-02-01 | Information disclosure | model.c4 backup relationship title | medium | mitigate | Title now `pg_dump over the Postgres protocol` (model.c4:139); leak gate silent at HEAD, fired on 71637bd | closed |
| T-02-02 | Repudiation | new rules in sync.mjs | medium | mitigate | checkEdgeLabels/checkParticipants/checkDeployedWorkloads (sync.mjs:457,469,480); 02-01-label-red.txt; drills D17/D18/D19 red on replay, D17b clean | closed |
| T-02-03 | Tampering | drill.fish edits | low | mitigate | All mutations target `$scratch`; tracked tree clean after replay | closed |
| T-02-04 | Information disclosure | monitoring, backup, b2 descriptions | medium | mitigate | Scan of model finds no datasource URL, Secret name, bucket, endpoint or recipient | closed |
| T-02-05 | Repudiation | chart-installed elements on a HelmRelease | low | accept | Limit A11 stated in monitoring.c4:3 and DIAGRAM_CONVENTIONS.md:112,247-248 | closed |
| T-02-06 | Information disclosure | delivery and edge descriptions | medium | mitigate | Only three hostnames (model.c4:11-13); no IP, SSH target or key path | closed |
| T-02-07 | Repudiation | Flux workload reader | medium | mitigate | Single-file read of gotk-components.yaml (sync.mjs:236); D22 exit 0 before, 1 after | closed |
| T-02-08 | Repudiation | firewall, host filter, svclb | low | accept | Descriptions start `Authored.` (edge.c4:31,38,42); listed as not placement-checked in DIAGRAM_CONVENTIONS.md:244-246 | closed |
| T-02-09 | Information disclosure | 02-04-401-measure.txt, 02-SCENARIO-EVIDENCE.md | medium | mitigate | Cookie recorded as `<redacted>`/yes-no; no DB_PASS, jdbc, Authorization or Bearer | closed |
| T-02-10 | Repudiation | error-401 view, ROADMAP criterion 3 | medium | mitigate | View (scenarios-app.c4:19-29) matches the post-fix measure (401, no Set-Cookie, no session row, no logged statement) | closed |
| T-02-11 | Tampering | compose Postgres on 5433 | low | mitigate | Held listener left alone; measure ran on private project on 15433, cleaned up; no kbmeasure container remains | closed |
| T-02-12 | Information disclosure | push-to-image and backup evidence quotes | medium | mitigate | Quotes carry no secret value, forward target, bucket, endpoint or recipient; gitleaks over bc06665..HEAD clean (45 commits) | closed |
| T-02-13 | Repudiation | docs/DIAGRAM_CONVENTIONS.md | medium | mitigate | Stale phrases gone; unchecked elements named at :112 and :244-246 | closed |
| T-02-SC | Tampering | npm/pip/cargo installs (plans 01-05) | high | mitigate | No manifest or lockfile changed since bc06665; likec4 1.60.0 and yaml 2.9.1 locked | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above `high` count toward threats_open*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-02-01 | T-02-05 | Check proves the HelmRelease exists and lands in monitoring, not what the chart renders (limit A11) | plan 02-02 (disposition `accept`) | 2026-10-28 |
| AR-02-02 | T-02-08 | Firewall, host filter and svclb are authored, not placement-checked (limit A12) | plan 02-03 (disposition `accept`) | 2026-10-28 |

---

## Audit Notes (non-register, informational)

- The plan's "three README-linked hostnames" is imprecise: README.md links two. The prod hostname is already committed in docs/INFRA_RUNBOOK.md and k8s/overlays/prod, so the model adds no disclosure.
- The plan 04 user DECISION gate never fired; the 401 measurement led to code fix `17a94cb` (NullRequestCache in SecurityConfiguration.java). The Java change was not audited here, and who authorized it was not verified.
- `.githooks/pre-commit:208-210` does not run the LikeC4 drift step for `.github/workflows` or `infra/vm`; only the CI job `likec4-model-drift` covers them.
- Review finding WR-07 (sync.mjs:255 reads only the first Host argument) is open; 02-REVIEW-DISPOSITION.md predates later fixes.
- Not verified: drill.fish `sed` silent-miss (WR-02), CI runs of `likec4-model-drift` and `secret-scan`.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-28 | 14 | 14 | 0 | gsd-security-auditor (HEAD 91e9375) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-28
