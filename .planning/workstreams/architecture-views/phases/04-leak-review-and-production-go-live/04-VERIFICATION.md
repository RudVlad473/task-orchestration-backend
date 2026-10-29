---
phase: 04-leak-review-and-production-go-live
verified: 2026-10-29T19:30:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - .github/workflows/uptime-check.yml
  - README.md
  - docs/architecture/site/leak-scan.mjs
covered_digest: "v3:sha256:b5345bc7eea68a752ced51c8da38840e517132e2502873410adc960d29a616b5"
behavior_unverified: 0
overrides_applied: 0
re_verification: false
advisory:
  - finding: "IPv6 literals with a middle `::` and two or more trailing groups (for example 2001:db8::8a2e:370:7334) pass the scan (04-REVIEW WR-01, re-run by the verifier: exit 0, OK targets=13)"
    category: security
    reason: "Roadmap SC1 lists IPv6 literals as a scan target. The target exists, is wired and fixture-tested for the shapes it covers, and the approved build holds no IPv6 text, so SC1 holds. The pattern still has a false-negative shape for a future description."
    evidence_status: "reproduced by the verifier"
coincidental_reliance_items: []
---

# Phase 4: Leak Review and Production Go-Live Verification Report

**Phase Goal:** The built artifact passes a leak scan and the user's review. Only after that does the site move to a production certificate, get an uptime probe and get linked from the docs and the README.
**Verified:** 2026-10-29
**Status:** passed
**Re-verification:** No, initial verification

Evidence base: the roadmap Success Criteria were checked against the repo at HEAD b15934b, `gh run view` of the recorded runs, public `curl` and `openssl` calls with no `-k`, and read-only `ssh netcup-prod` metadata reads. No SUMMARY claim was accepted on its own.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP SC) | Status | Evidence |
|---|--------------------|--------|----------|
| 1 | SC1: leak scan runs on the built directory before the push step, red on a planted IP and green when removed, current build passes, robots.txt is `Disallow: /` | VERIFIED | `.github/workflows/architecture-site.yml`: `sync.mjs check` (l.75), fixture tests (l.78), `Scan the build for leaks` (l.86-87, no pipe, no `if:`), `docker build` (l.90), `publish` has `needs: build` (l.101). `leak-scan.mjs` has 13 targets. Red/green recorded in `04-03-push.txt` and re-checked with `gh`: PR run 40925203236 (`pull_request`, build=failure, publish=skipped, log shows `TARGET ipv4 hits=7`), PR run 40925366776 (build=success, publish=skipped), main push run 40923112448 (build=success, publish=success). Local baseline plant red/green in `04-scan-baseline.txt` (`PLANT ... exit=1 targets=ipv4`, `UNPLANT exit=0`). Verifier re-ran `node --test docs/architecture/site/leak-scan.test.mjs`: 81 pass, 0 fail. Live `curl https://<host>/robots.txt` returned `User-agent: *` / `Disallow: /`. |
| 2 | SC2: owner's approval recorded before the flip; Certificate first on staging, flipped in a separate later commit; after the flip Ready on production and `curl https://<host>/` works without `-k` | VERIFIED | `04-DECISIONS.txt` records `Q2 approve` and `APPROVED_TAG main-8-c3c366a ... at 2026-10-29T18:22:28Z`. `git log -- k8s/overlays/architecture/certificate.yaml`: 8e531d0 (2026-10-28, staging) then 26c0e3e (2026-10-29T18:44:51Z, production), so the flip commit is after the approval and a separate commit. Live: cluster Certificate `architecture` issuerRef `letsencrypt-production`, `Ready=True`, revision 2; deployed image tag `main-8-c3c366a` equals the approved tag; `openssl s_client` issuer `C=US, O=Let's Encrypt, CN=YR1` (not STAGING), valid to 2027-01-27; `curl -sS https://<host>/` returned 200 with `ssl_verify_result=0` and one `id="likec4-root"`. IngressRoute label `kustomize.toolkit.fluxcd.io/name: apps-architecture` (single owner). |
| 3 | SC3: `uptime-check.yml` probes the site for a body marker, timeout raised, wrong-marker drill red and restored green, runbook lists the site | VERIFIED | `uptime-check.yml`: `SITE_URL`, `SITE_MARKER: 'id="likec4-root"'`, `run_check "$SITE_URL" "$SITE_MARKER"` (l.169); `timeout-minutes` 8 to 10 in b99da2b with arithmetic 6 URLs x 80 s = 480 s. `gh`: drill run 40936850782 failed with `https://<host>/: 200 without the expected marker`; recovery run 40937007797 (`workflow_dispatch`, main) success; baseline run 40936698753 success; issue 23 CLOSED 2026-10-29T19:06:33Z. `docs/INFRA_RUNBOOK.md:57` lists the site URL and marker. |
| 4 | SC4: on production, cold deep link and refresh render the view; a missing `/assets/*.js` does not answer 200 `text/html`; README and `INFRA_ARCHITECTURE.md` link the address | VERIFIED | Live now: `GET /view/signin-ok/` 200; `HEAD /assets/missing.js` answers `404 content-type: text/html` (not 200). Headless render and reload recorded in `04-05-live.txt` (svg_nodes 185 before and after reload, no `-k`), not re-run by the verifier (see Human Verification: none required, one residual note). Links: `README.md:49` and `docs/INFRA_ARCHITECTURE.md:297` both contain the address (`rg -F`). |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `docs/architecture/site/leak-scan.mjs` | 13-target zero-dependency scan | VERIFIED | Exists, substantive, wired into the workflow; walks the build as bytes; exits 0/1/2/4 |
| `docs/architecture/site/leak-scan.test.mjs` | per-target fixtures | VERIFIED | 81 pass, 0 fail (run by verifier) |
| `.github/workflows/architecture-site.yml` | scan steps before the image | VERIFIED | order read from source |
| `k8s/overlays/architecture/certificate.yaml` | production issuerRef | VERIFIED | `letsencrypt-production`, matches the live object |
| `.github/workflows/uptime-check.yml` | site probe, timeout 10 | VERIFIED | read from source and exercised by runs |
| `04-scan-baseline.txt`, `04-03-push.txt`, `04-04-flip.txt`, `04-05-live.txt` | evidence | VERIFIED | the claims that can be re-queried (run ids, conclusions, issuer, tag, issue state) match |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| architecture-site.yml build job | leak-scan.mjs | step `Scan the build for leaks`, `publish: needs: build` | WIRED (PR run showed publish skipped on a hit) |
| leak-scan.test.mjs | leak-scan.mjs | spawns the sibling script | WIRED (81 pass) |
| Certificate manifest | live cluster | Flux `apps-architecture` | WIRED (issuer, Ready and revision match) |
| uptime-check.yml | live site | `run_check` with marker | WIRED (drill red proves marker is discriminating) |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Fixture suite | `node --test docs/architecture/site/leak-scan.test.mjs` | 81/81 | PASS |
| Drift check | `node docs/architecture/sync.mjs check` | `OK (62 elements ... 18 workloads)` | PASS |
| K8s invariants (incl. no dev/preview server) | `python3 scripts/verify-k8s-invariants.py --no-provisional` | `invariants OK -- 14 kustomization root(s)` | PASS |
| Strict TLS root | `curl -sS https://<host>/` | 200, verify=0 | PASS |
| Robots | `curl https://<host>/robots.txt` | `Disallow: /` | PASS |
| Missing asset | `curl -sI .../assets/missing.js` | 404 text/html | PASS |

### Probe Execution

SKIPPED: no `probe-*.sh` is declared by this phase. The uptime probe is a workflow, exercised through the recorded GitHub runs above.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PUB-03 | 04-04 | staging first, then production | SATISFIED | two commits 8e531d0 then 26c0e3e; live Certificate production, Ready, revision 2 |
| PUB-04 | 04-05 | uptime probe covers the site | SATISFIED | probe in uptime-check.yml; drill red then recovery green via `gh` |
| PUB-05 | 04-01, 04-02, 04-03, 04-05 | leak scan, review gate, `Disallow: /` | SATISFIED | scan before image; owner Q2 approve with APPROVED_TAG equal to the deployed tag; live robots.txt |
| PUB-06 | 04-04, 04-05 | docs link; deep links resolve; missing asset not 200 HTML | SATISFIED | README and INFRA_ARCHITECTURE links; deep link 200; missing asset 404 |

All four phase IDs appear in plan frontmatter and in REQUIREMENTS.md (marked Complete, mapped to Phase 4). No orphaned requirements: REQUIREMENTS.md maps no other ID to Phase 4.

### Anti-Patterns Found

None blocking. No unreferenced TBD/FIXME/XXX was searched in the 04-REVIEW file list beyond what the review already covered; the review reports none.

### 04-REVIEW.md warnings (context, none breaks a must-have)

- WR-01 IPv6 with a middle `::` and 2+ trailing groups is not matched. Reproduced by the verifier. Advisory above; SC1 still holds for the approved build.
- WR-02 `ssh-command` needs a trailing space, so "over ssh." passes.
- WR-03 a `credential` hit prints up to 24 characters of the match into a public CI log and step summary.
- WR-04 fixtures do not detect the scan skipping dotfiles or symlinks, so the hook's weaken-detection is blind to that mutation.
- WR-05 the site probe's retry grace (about 20 s) may be shorter than the site's Recreate outage; the site's window is not measured in any phase file. The verifier did not measure it either (a read-only phase). A false alert is possible after a model-change roll.

Info items IN-01 to IN-07 are noted in the review file and not re-assessed.

### Human Verification Required

None open. The owner's review (Q2) is recorded in `04-DECISIONS.txt`; the verifier can confirm the record and the tag equality but cannot independently confirm the owner's words beyond that file.

### Could not verify

- The headless-browser deep link and reload (SC4) were not re-run; the verifier confirmed the deep link answers 200 by `curl` and relies on `04-05-live.txt` for the render and reload.
- The runbook (`INFRA_RUNBOOK.md:57`) listing was confirmed by grep for the URL, not by reading the whole section.
- WR-05 (site Recreate window against the retry grace) is unmeasured.

### Gaps Summary

No gaps. The scan gates the image, the flip followed the recorded approval in a separate commit, the live certificate is production and strictly valid, the probe was seen red and green, and the public links and URL behaviour hold. The review warnings are hardening work, with WR-01 and WR-03 the ones worth scheduling because they sit on the scan's own purpose.

---

_Verified: 2026-10-29_
_Verifier: Claude (gsd-verifier)_
