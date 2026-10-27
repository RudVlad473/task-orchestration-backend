---
phase: "01"
slug: "foundations-edge-fix-and-honesty-gates"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-27"
---

# Phase 01 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| npm registry to `docs/architecture/node_modules` | The `yaml` parser is installed from the registry and runs inside the drift check and the pre-commit hook | Third-party code executed on the developer machine; pinned by exact version and lockfile integrity |
| `k8s/` manifests to `sync.mjs` | The check parses every manifest under `k8s/` as untrusted text and compares it with the LikeC4 model | Manifest YAML, read only; no secrets are read or printed |
| staged diff to `.githooks/pre-commit` | The hook decides whether a commit is checked | Commit contents; the trigger pathspec decides which paths reach the check |
| `.planning/` evidence files to the repository | Drill, selftest and diff output is committed | Tool output only, scanned by gitleaks |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-01-01 | Tampering | `yaml@2.9.1` install | high | mitigate | Blocking human gate before install (approved by the user); exact pin `"yaml": "2.9.1"` (`package.json:8`); integrity `sha512-3NxN8+78…Eb2yFw==` once in `pnpm-lock.yaml:629` and equal to `npm view`; no dependencies and no install scripts. Lockfile deviation (10 suffix-only re-keyed lines) accepted by the user as option A | closed |
| T-01-02 | Tampering | `k8sDocs()` on a malformed manifest | medium | mitigate | `doc.errors` checked before `toJS()`, exit 4 naming file and code (`sync.mjs:264-268`); drill D14c | closed |
| T-01-03 | Repudiation | `check()` exit status when tooling is missing | medium | mitigate | `yaml` loaded by dynamic import in `loadTooling()`, missing module exits 3 with the install command (`sync.mjs:32-45`); selftest cases 09, 10, 11 | closed |
| T-01-04 | Tampering | `placements()` resolving `resources` paths | low | mitigate | Entries that are absolute or leave `k8s/` after `posix.normalize` are skipped; only already-parsed documents are looked up (`sync.mjs:327-361`) | closed |
| T-01-05 | Repudiation | metadata keys on the model | medium | mitigate | `METADATA_KEYS` allowlist over elements, nodes, instances and relationships (`sync.mjs:53`, `:394-410`); drills D12, D12b | closed |
| T-01-06 | Information disclosure (integrity of the published view) | deployment instances absent from every view | medium | mitigate | `checkViewMembership()` (`sync.mjs:415-423`); drills D13, D13b | closed |
| T-01-07 | Repudiation | `check()` reading a scratch root that lost a k8s tree | medium | mitigate | `K8S_DIRS` guard exits 4 naming each missing tree (`sync.mjs:249`, `:498-500`); drills D14, D14b | closed |
| T-01-08 | Elevation of privilege (gate bypass) | hook trigger list | low | mitigate | Trigger pathspec widened to `k8s` (`.githooks/pre-commit:208-210`); selftest case 16 failed 15/16 against the old hook and passes 16/16 | closed |
| T-01-09 | Information disclosure | evidence files under `.planning` | low | mitigate | gitleaks found no leaks in the phase directory or `drill-output.txt`; evidence holds only tool output | closed |
| T-01-10 | Tampering | `--no-verify` on the Task 3 commit | low | accept | See Accepted Risks Log | closed |
| T-01-SC | Tampering | npm/pip/cargo installs | high | mitigate | The only dependency manifests changed in the phase are `package.json` and `pnpm-lock.yaml` (commit `0c6b615`); the only added package is `yaml@2.9.1` | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Residuals noted by the audit, not threats in the register: code-review WR-01 (an unreadable manifest, such as a dangling symlink, still refuses the commit but exits 1 instead of 4) and WR-02 (the hook gives install advice on exit 4). Both are open in `01-REVIEW-DISPOSITION.md`.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01-01 | T-01-10 | The Task 3 commit touches only Markdown (`README.md`, `docs/DIAGRAM_CONVENTIONS.md`), which project CLAUDE.md allows to skip the 4-minute hook; CI `secret-scan.yml` still scans every push with full-history gitleaks. The audit confirmed both claims, and the full hook ran on that commit anyway | Plan 01-03 (plan approved by the user) | 2026-10-27 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-27 | 11 | 11 | 0 | gsd-security-auditor (ASVS 1, block_on high) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-27
