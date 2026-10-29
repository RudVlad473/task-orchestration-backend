# Phase 4: Leak Review and Production Go-Live - Context

**Gathered:** 2026-10-29
**Status:** Ready for planning

<domain>
## Phase Boundary

The built LikeC4 site passes a leak scan and the owner's review. Only then does the site's Certificate move from `letsencrypt-staging` to `letsencrypt-production`, get an uptime probe, and get linked from the README and `docs/INFRA_ARCHITECTURE.md`. Requirements: PUB-03, PUB-04, PUB-05, PUB-06.

Out of scope: rewriting model text for style (Phase 3.1, done), CSP headers, a build stamp, Traefik's 64Mi limit under concurrent cold loads (Open Question 8, no requirement).

</domain>

<decisions>
## Implementation Decisions

The user chose "use my best judgment" for all four areas, so each decision below is the proposed default, not a stated preference.

### Leak scan (PUB-05)
- **D-01:** A script under `docs/architecture/` scans the built directory (every file, JS and HTML included), and the site workflow runs it before the push step. It fails on an empty or missing directory, as Phase 1's readers do. — **Reversibility:** reversible — a script and a workflow step.
- **D-02:** Targets: IPv4 and IPv6 literals, key paths, `ssh `, `kubectl`, Secret names, in-cluster hostnames, and the deny-list from PITFALLS 11. An allow-list passes only the four public hostnames (prod, nonprod, monitoring, architecture). — **Reversibility:** reversible.
- **D-03:** Red/green proof: a planted IP literal in a model description fails the scan, removing it passes. The scan gets a drill row in `drills.tsv`, a regenerated `drill-output.txt` and a hook self-test case, per the Phase 3.1 pattern.
- **D-04:** Whether the word `secret` stays a target is undecided: its false-positive count is not measured. The first plan task runs the scan once on the current build and reports the hits per target. Every description it flags is then rewritten, which is success criterion 1.
- **D-05:** `robots.txt` keeps `Disallow: /` and no doc calls it access control.

### Review gate (PUB-05)
- **D-06:** The owner reviews the scan report and the live staging site, which is the same image lineage that will go live. The approval is recorded in a phase evidence file with the reviewed image tag (`main-<run>-<sha7>`). — **Reversibility:** reversible.
- **D-07:** The flip plan refuses to proceed unless the live tag equals the approved tag, so a model change after review forces a re-review. The gate is a blocking-human checkpoint, not a script.

### Flip (PUB-03)
- **D-08:** The flip is one commit that changes `letsencrypt-staging` to `letsencrypt-production` in `k8s/overlays/architecture/certificate.yaml`, made after D-06 and separate from the commit that first set staging. — **Reversibility:** costly — Let's Encrypt allows 5 certificates per exact name per 7 days with no override, and a production certificate enters Certificate Transparency logs permanently.
- **D-09:** Verify with `Ready=True` on the production issuer and `curl https://<host>/` without `-k`. On issuance failure, revert to staging and diagnose before any retry, because the rate limit allows one careful attempt.

### Probe and links (PUB-04, PUB-06)
- **D-10:** `uptime-check.yml` probes the site root for the `likec4-root` marker. The base worst case is 5 URLs x 80s = 400s and the drill's sixth URL makes 480s, equal to the current 8-minute `timeout-minutes`, so the plan raises it to 10 and updates the header arithmetic.
- **D-11:** A drill against a wrong marker or URL fails the probe (red) and the restored entry passes (green). The runbook's list of probed URLs includes the site.
- **D-12:** The README and `docs/INFRA_ARCHITECTURE.md` link the root URL only, not a specific view. `rg` must find the address in both files.
- **D-13:** On the production hostname, a cold deep link to `/view/<id>/` and its refresh render the view in a headless browser. A missing `/assets/*.js` must not answer 200 `text/html`; it already answered 404 `text/html` on staging (03-06-live.txt:43), so this is a re-measurement on production.

### Claude's Discretion
Script language and file name for the scan, the allow-list file format, the layout of the scan report, and the wording of the approval evidence file.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Workstream
- `.planning/workstreams/architecture-views/ROADMAP.md` — Phase 4 goal, success criteria 1 to 4, planning notes
- `.planning/workstreams/architecture-views/REQUIREMENTS.md` — PUB-03 to PUB-06
- `.planning/workstreams/architecture-views/research/PITFALLS.md` §Pitfall 11 — what the published bundle exposes, the deny-list, the scan design
- `.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-06-live.txt` — staging measurements: issuer, deep link, missing asset, robots, CT
- `.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-DECISIONS.txt` — prior blocking-human decisions
- `.planning/workstreams/architecture-views/phases/03.1-model-description-pass/03.1-CONTEXT.md` — drill, self-test and wording-rule precedent

### Code and manifests
- `.github/workflows/architecture-site.yml` — where the scan step goes, before the push step
- `.github/workflows/uptime-check.yml` — probe URLs, header arithmetic, drill input
- `k8s/overlays/architecture/certificate.yaml`, `ingressroute.yaml`, `kustomization.yaml` — the flip target
- `k8s/base/architecture-site/architecture-site.yaml` — the served workload
- `docs/architecture/sync.mjs`, `drills.tsv`, `drill.fish`, `drill-output.txt` — drill mechanism for the new scan
- `scripts/pre-commit-likec4-selftest.py` — self-test cases
- `docs/INFRA_RUNBOOK.md` §"Uptime alert triage" and its probed-URL list; `docs/INFRA_ARCHITECTURE.md`; `README.md`
- `docs/DIAGRAM_CONVENTIONS.md` — the wording rule and standard

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `docs/architecture/drills.tsv` and `drill.fish`: red/green proof mechanism for any new check.
- `uptime-check.yml` drill input: adds one known-wrong URL, extend it for the site's marker.

### Established Patterns
- Fail closed on a missing input directory (Phase 1, exit 4).
- Decisions needing the owner are blocking-human checkpoints with numbered options (Phase 3).
- Live checks over SSH are read-only unless a plan says otherwise.

### Integration Points
- The scan step sits between `likec4 build` and the image push in `architecture-site.yml`; the PR-time build runs with no secrets.
- The Certificate edit is reconciled by Flux; the flip lands via a push to `main`.

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches.

</specifics>

<deferred>
## Deferred Ideas

- CSP and response headers (Report-Only first), a build stamp tying the live site to the model commit, and Traefik's 64Mi limit under concurrent cold loads: Open Question 8, no requirement; the owner decides whether any becomes one.
- V2-05: retention job for the third Docker Hub repo.

</deferred>

---

*Phase: 4-Leak Review and Production Go-Live*
*Context gathered: 2026-10-29*
