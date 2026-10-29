# Phase 4: Leak Review and Production Go-Live - Research

**Researched:** 2026-10-29
**Domain:** Build-artifact leak scanning in GitHub Actions, cert-manager issuer flip on Let's Encrypt, a body-marker uptime probe, public links for a static nginx site
**Confidence:** HIGH for repo facts and the scan baseline (read or measured this session); MEDIUM for cert-manager flip behaviour on a live cluster (documented, not exercised here)

<user_constraints>
## User Constraints (from CONTEXT.md)

The user chose "use my best judgment" for all four areas, so each decision below is the proposed default, not a stated preference.

### Locked Decisions

#### Leak scan (PUB-05)
- **D-01:** A script under `docs/architecture/` scans the built directory (every file, JS and HTML included), and the site workflow runs it before the push step. It fails on an empty or missing directory, as Phase 1's readers do. — **Reversibility:** reversible — a script and a workflow step.
- **D-02:** Targets: IPv4 and IPv6 literals, key paths, `ssh `, `kubectl`, Secret names, in-cluster hostnames, and the deny-list from PITFALLS 11. An allow-list passes only the four public hostnames (prod, nonprod, monitoring, architecture). — **Reversibility:** reversible.
- **D-03:** Red/green proof: a planted IP literal in a model description fails the scan, removing it passes. The scan gets a drill row in `drills.tsv`, a regenerated `drill-output.txt` and a hook self-test case, per the Phase 3.1 pattern.
- **D-04:** Whether the word `secret` stays a target is undecided: its false-positive count is not measured. The first plan task runs the scan once on the current build and reports the hits per target. Every description it flags is then rewritten, which is success criterion 1.
- **D-05:** `robots.txt` keeps `Disallow: /` and no doc calls it access control.

#### Review gate (PUB-05)
- **D-06:** The owner reviews the scan report and the live staging site, which is the same image lineage that will go live. The approval is recorded in a phase evidence file with the reviewed image tag (`main-<run>-<sha7>`). — **Reversibility:** reversible.
- **D-07:** The flip plan refuses to proceed unless the live tag equals the approved tag, so a model change after review forces a re-review. The gate is a blocking-human checkpoint, not a script.

#### Flip (PUB-03)
- **D-08:** The flip is one commit that changes `letsencrypt-staging` to `letsencrypt-production` in `k8s/overlays/architecture/certificate.yaml`, made after D-06 and separate from the commit that first set staging. — **Reversibility:** costly — Let's Encrypt allows 5 certificates per exact name per 7 days with no override, and a production certificate enters Certificate Transparency logs permanently.
- **D-09:** Verify with `Ready=True` on the production issuer and `curl https://<host>/` without `-k`. On issuance failure, revert to staging and diagnose before any retry, because the rate limit allows one careful attempt.

#### Probe and links (PUB-04, PUB-06)
- **D-10:** `uptime-check.yml` probes the site root for the `likec4-root` marker. The base worst case is 5 URLs x 80s = 400s and the drill's sixth URL makes 480s, equal to the current 8-minute `timeout-minutes`, so the plan raises it to 10 and updates the header arithmetic.
- **D-11:** A drill against a wrong marker or URL fails the probe (red) and the restored entry passes (green). The runbook's list of probed URLs includes the site.
- **D-12:** The README and `docs/INFRA_ARCHITECTURE.md` link the root URL only, not a specific view. `rg` must find the address in both files.
- **D-13:** On the production hostname, a cold deep link to `/view/<id>/` and its refresh render the view in a headless browser. A missing `/assets/*.js` must not answer 200 `text/html`; it already answered 404 `text/html` on staging (03-06-live.txt:43), so this is a re-measurement on production.

### Claude's Discretion
Script language and file name for the scan, the allow-list file format, the layout of the scan report, and the wording of the approval evidence file.

### Deferred Ideas (OUT OF SCOPE)
- CSP and response headers (Report-Only first), a build stamp tying the live site to the model commit, and Traefik's 64Mi limit under concurrent cold loads: Open Question 8, no requirement; the owner decides whether any becomes one.
- V2-05: retention job for the third Docker Hub repo.

Out of scope (Phase Boundary): rewriting model text for style (Phase 3.1, done), CSP headers, a build stamp, Traefik's 64Mi limit under concurrent cold loads.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PUB-03 | The certificate is issued by the staging issuer first, then flipped to production. | Staging half already in git (8e531d0, only commit touching `certificate.yaml`); flip procedure, re-issuance trigger and verification in "Flip" below. |
| PUB-04 | An uptime probe covers the site. | `uptime-check.yml` structure, timeout arithmetic with both drills, ordering after the flip. |
| PUB-05 | A leak scan of the built artifact and a review gate run before the production certificate. `robots.txt` keeps `Disallow: /`. | Measured scan baseline on a real build (zero hits on every target), regex set with measured false-positive traps, CI placement, review-gate tag mechanics. |
| PUB-06 | Docs and the README link to the public address; history-style URLs resolve and a missing asset does not return 200 HTML. | Baseline: address absent from README and INFRA_ARCHITECTURE; nginx.conf already returns 404 for a missing asset; deep-link probe recipe. |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Read from `/home/andre/dev/kanban-board-backend/.claude/CLAUDE.md` and the global `~/.claude/CLAUDE.md`:

- All repo edits start through a GSD command; no direct edits outside a GSD workflow.
- Before any PLAN.md is created or approved: document 2 alternate technical approaches, output a 3-column Trade-off Matrix `[Approach | Pros/Cons | Why Picked]`, and name non-obvious performance, memory or security trade-offs. A starter matrix is in "Trade-off Matrices for the planner" below.
- Never auto-execute code blocks without explaining the core data flow in 3 sentences or less.
- Source `.dev/gsd-run.sh` instead of re-pasting the resolver: `. ./.dev/gsd-run.sh && gsd_run query ...`.
- Read `docs/SESSION_LESSONS.md` before starting or resuming phase execution (git hygiene).
- Code comments are plain text: identifiers by bare name, a blank comment line between paragraphs, `- ` for list items (CODE_STYLE rule 14, `scripts/verify-comments.py`). Planning-system ids are banned in comments [VERIFIED: scripts/verify-comments.py:9-10] "planning-system ids are banned, a block of 4+ prose lines opens with a summary of at most 2 lines, narration past 8 prose lines sits behind a decision-record marker". So no `D-04`, `PUB-05` or `Pitfall 11` in any new script, workflow or manifest comment.
- Every commit runs `.githooks/pre-commit` (about 4 minutes, `fastTest` needs Docker). `--no-verify` is allowed for docs, planning and config-only commits, not for Java, migrations or k8s manifests. The flip commit edits a k8s manifest, so it runs the full hook.
- Gitleaks refuses a credential-shaped value anywhere, `.planning/` prose included. Test fixtures must assemble credential-shaped strings at runtime, not store them literally.
- Live checks over SSH are read-only unless a plan says otherwise. Ask before merging to main is a standing user rule (memory), and pushing is outside the commit-without-asking rule: every push to `main` in this phase is a blocking-human checkpoint, as in Phase 3 (03-DECISIONS.txt Q5, Q6).
- The wiki skill (`docs/wiki`) is not touched by this phase.

## Summary

The phase is mostly wiring and sequencing, not new technology. Measured on a fresh local build of the current model (`likec4 build --use-dot=false`, the workflow's own command), the built site is 20 files and 6.8 MB, and a strict scan finds **zero hits on every named target**: IPv4, IPv6, `ssh `, `kubectl`, `secret` (case-insensitive), key paths, in-cluster hostnames and the PGHOST string. The only duckdns hostnames in the bundle are exactly the four allowed ones. So success criterion 1's "once every description the scan flags has been rewritten" is already satisfied by the current model; the first task should record that zero-hit report rather than expect rewrites. The real design risk is the opposite: loose patterns produce false positives in minified vendor JavaScript (`\bssh\b` hits a bundled shell-command list; `token` hits 335 times in CSS design tokens; a dotted-quad regex without boundaries matches SVG path numbers), so the regex set and its boundaries are the decision that matters.

The flip is one cert-manager issuerRef change. cert-manager's documented re-issuance triggers include "cert-manager issuer annotations on the `Secret` do not match the issuer specified on the `Certificate`", so editing `issuerRef.name` in place re-issues with the same `secretName` and spends exactly one production order. The staging half of PUB-03 is already in git history. Two ordering traps need explicit plan steps: (1) any model or workflow-path edit after the owner's review creates a new image tag and invalidates the approval under D-07, so every model-text edit this phase needs (a scan step in the publish scenario, a stale "today" sentence in `edge.c4`) must land before the review; (2) the uptime probe, the README link and the doc edits must come after the production certificate is `Ready`, and the probe's wrong-marker drill must run after the probe has a TLS-clean baseline or its red is not attributable to the marker.

One conflict between D-03 and the existing harness needs a planner decision: `drill.fish` runs `sync.mjs check` on a scratch copy and never builds the site, and the pre-commit hook never builds either, so a drill row and a hook self-test case cannot exercise a scan of a built directory without new machinery. Recommended resolution is in Open Question 1.

**Primary recommendation:** Add a zero-dependency Node script `docs/architecture/leak-scan.mjs` (strict regexes below, reads every file as bytes, exits 4 on a missing or empty directory), call it in the `build` job between "Verify the build holds the app shell" and "Build the image without pushing", prove it red/green with a fixture test plus one real-build plant and a draft-PR run, then run review, flip, probe and links in that order with one blocking-human checkpoint before the push of the scan and one before the flip.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Leak scan of the built site | CI (build job on the runner) | Developer machine (same script, local) | The artifact exists only after `likec4 build`; the build job already holds the checkout, Node and pnpm install and no secrets, and `publish` pushes the very bytes `build` gated (architecture-site.yml header). |
| Review gate | Human (repo owner) | CI run summary as evidence | Only the owner can judge what is sensitive; the report must be tied to an image tag. |
| Production certificate | Cluster (cert-manager + Flux) | Git (the flip commit) | The Certificate object is reconciled by Flux from `k8s/overlays/architecture/`; cert-manager talks ACME. |
| TLS termination and routing | Edge (Traefik IngressRoute, `architecture-tls`) | — | Unchanged by the flip; same `secretName`. |
| Static serving, SPA fallback, 404 for missing assets | Pod (nginx-unprivileged, `nginx.conf`) | — | Already built and measured in Phase 3; Phase 4 only re-measures on production. |
| Uptime probe | CI (scheduled GitHub Actions workflow) | Issue tracker (alert job) | `uptime-check.yml` is an off-box scheduled probe; site is one more URL/marker pair. |
| Public links | Docs (README, INFRA_ARCHITECTURE.md) | — | Static markdown. |

## Standard Stack

### Core
No external package is installed in this phase. Everything uses what is already pinned.

| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Node | 24.19.0 (workflow `node-version`, local `v24.19.0`) | Scan script runtime and `node:test` | Already installed by the build job; `sync.mjs` is Node ESM. [VERIFIED: .github/workflows/architecture-site.yml:63-66 `node-version: '24.19.0'`] |
| LikeC4 | 1.60.0 | `likec4 build` produces the artifact | Pinned in `docs/architecture/package.json` [VERIFIED: package.json:7 `"likec4": "1.60.0"`] |
| cert-manager | v1.21.1 (per PITFALLS 10, pinned in `k8s/platform/cert-manager/cert-manager.yaml`) | issuerRef flip re-issues | Already running; not re-read this session. [CITED: research/PITFALLS.md Pitfall 10] |
| nginx-unprivileged | 1.30.6-alpine-slim by digest | Serves the build | [VERIFIED: docs/architecture/site/Dockerfile:6] |
| shot-scraper | 1.12 (`~/.local/bin/shot-scraper`) | Headless deep-link probe | Phase 3's recipe (03-06-PLAN.md:111). Present on this machine. |

### Supporting
| Tool | Purpose | When to Use |
|------|---------|-------------|
| `node:test` (built into Node 24) | Fixture tests for the scan | Standing red/green for every target without a 26 s build per case. |
| `gh` 2.98.0 | `gh workflow run uptime-check.yml -f ...`, `gh run view`, PR proof | Drills and CI evidence. Logged in as RudVlad473. |
| `dig`, `openssl s_client`, `curl` | Flip verification | Issuer CN lacks `(STAGING)`; no `-k`. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Node `.mjs` scan | Python `scripts/verify-site-leaks.py` + `-selftest.py` pair (the repo's gate shape, e.g. `verify-public-dashboards.py`) | Matches the gate convention and the comment-lint/selftest wiring, but D-01 says the script lives under `docs/architecture/`, and the build job's toolchain there is Node. Either works; Node keeps one language in that directory. |
| `gitleaks` or `trufflehog` on `site/out` | — | They target credential shapes, not internal hostnames or IPs; PITFALLS 11 already says gitleaks "will not catch these". Do not add a dependency. |

**Installation:** none. **Version verification:** no package is added, so `npm view` / `pip index` does not apply.

## Package Legitimacy Audit

No external packages are installed by this phase. Scan script uses `node:fs`, `node:path`, `node:test` only. Audit not applicable.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 model/*.c4 edit ──► PR / push to main
                        │ (paths: model/**, site/**, package.json, lockfile, workflow)
                        ▼
              ┌─────────── build job (no secrets) ───────────┐
              │ sync.mjs check ─► likec4 build ─► app-shell grep │
              │                                  │               │
              │                     NEW: leak-scan.mjs <site/out>│── exit 1 (hit) / 4 (empty dir) ──► run red, publish skipped
              │                                  │ pass          │
              │      docker build (no push) ─► upload-artifact   │
              └──────────────────────────────────┬───────────────┘
                                                 ▼ (push to main only, environment production)
                                   publish job: push main-<run>-<sha7>
                                                 ▼
                      Flux image policy ─► bump commit on main ─► apps-architecture (10m)
                                                 ▼
                         nginx pod, architecture namespace  ◄── staging cert today
                                                 │
        owner reviews: scan report (run summary) + live staging site   [blocking-human, records tag]
                                                 ▼
             live tag == approved tag ? ──no──► stop, re-review
                                   │yes
                                   ▼
        flip commit: certificate.yaml issuerRef letsencrypt-staging ─► letsencrypt-production
                                   ▼
        cert-manager re-issues (issuer annotation mismatch) ─► Ready on production issuer
                                   ▼
        curl without -k ─► uptime-check.yml gains site URL + likec4-root marker ─► drill red/green
                                   ▼
        README + INFRA_ARCHITECTURE link; runbook list; deep link / refresh / missing asset on production
```

### Recommended Project Structure
```
docs/architecture/
├── leak-scan.mjs          # new: scan of a built directory (name is discretion)
├── leak-scan.test.mjs     # new: node:test fixtures, one case per target plus clean and empty-dir
├── sync.mjs, drill.fish, drills.tsv, drill-output.txt   # unchanged unless Open Question 1 goes the drill way
└── site/                  # Dockerfile, nginx.conf; COPY out/ only, so a script here would not ship
.github/workflows/
├── architecture-site.yml  # +1 step in build job
├── uptime-check.yml       # +SITE entry, timeout, drill input, header arithmetic
└── invariant-checks.yml   # likec4-model-drift job gains `node --test docs/architecture/leak-scan.test.mjs`
.planning/workstreams/architecture-views/phases/04-.../
├── 04-scan-baseline.txt, 04-DECISIONS.txt (approval), 04-flip-*.txt, 04-uptime-*.txt, 04-live-*.txt
```

### Pattern 1: Strict regexes (measured against the real build)
**What:** Every pattern below was run with ripgrep PCRE over `likec4 build` output of the current model and, for IPv4, over a build with a planted `10.20.30.40` in a description.
**When to use:** The scan's target table. Patterns marked TRAP are the loose forms that produce false positives.

| Target | Pattern (PCRE) | Hits on current build | Note |
|--------|----------------|----------------------|------|
| IPv4 literal | `(?<![0-9.])(?:25[0-5]\|2[0-4][0-9]\|1[0-9]{2}\|[1-9]?[0-9])(?:\.(?:25[0-5]\|2[0-4][0-9]\|1[0-9]{2}\|[1-9]?[0-9])){3}(?![0-9])` | 0 | Planted `10.20.30.40` found 57 times in 4 files: `likec4-views.js`, `assets/model-*.js`, `assets/dot-*.js`, `assets/puml-*.js`. |
| IPv4 TRAP | `\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b` | 3 files with hits (e.g. `58.06.87.06`, `64.17.81.76`) | SVG path coordinates and leading-zero numbers in minified JS. Do not use. |
| IPv6 | `(?<![0-9A-Fa-f:])(?:[0-9A-Fa-f]{1,4}:){3,7}[0-9A-Fa-f]{1,4}(?![0-9A-Fa-f:])` and `(?<![0-9A-Za-z:])(?:[0-9A-Fa-f]{1,4}:){1,6}:(?:[0-9A-Fa-f]{1,4})?(?![0-9A-Za-z:])` | 0 and 0 | Clock-like `10:20:30:40` would match the first form; none present. |
| `ssh ` | `ssh ` (literal, trailing space) | 0 | TRAP: `\bssh\b` hits 2 times, both in a bundled shell-command word list (`...\|split\|ssh\|stat\|...`) in `likec4-views.js` and `assets/internal-*.js`. |
| `kubectl` | `kubectl` | 0 | |
| Secret word | `(?i)secret` | 0 | Answers D-04: zero false positives today, so keep it as a target. |
| Key paths | `authorized_keys`, `id_ed25519`, `id_rsa`, `~/\.ssh`, `/home/`, `/opt/deploy`, `BEGIN [A-Z ]*PRIVATE`, `pass show`, `netcup-prod` | 0 each | |
| Credential shapes | `ghp_[A-Za-z0-9]{20}`, `AKIA[0-9A-Z]{16}`, `dckr_pat_`, `-----BEGIN` | 0 | |
| In-cluster host | `\.svc\b`, `cluster\.local`, `postgres\.kanban`, `PGHOST` | 0 each | TRAP: a generic `<x>.<namespace>` regex matches `k3s.monitoring` (181), `k3s.kube-system` (75), `k3s.architecture` (37), `k3s.cert-manager` (35), `k3s.flux-system` (34). Those are LikeC4 deployment FQNs, not hostnames. Exclude a `k3s.` prefix or match only `.svc` forms plus named strings. |
| Namespace title alone | `kanban-data` | 38 | Namespace titles are drawn on purpose. Never a target by itself. |
| Hostname allow-list | `[A-Za-z0-9.-]*duckdns\.org` | 88 = 4 hostnames x 22 | Exactly the prod, nonprod, monitoring and architecture hosts; anything else fails. |
| TRAP: `token` | `(?i)token` | 335 | CSS design tokens and bundled code. Never a target. |
| `password` | `(?i)password` | 44 | Vendored bundles plus one authored edge label `POST /api/signin {email, password}`. Not a named target; a field name, not a credential. |

[VERIFIED: measured this session with `rg -P` over `docs/architecture/out/leak-probe` (deleted after) built by `likec4 build --use-dot=false -o ... model`; planted-IP case built from a scratch copy of `model/` in `docs/architecture/out/leak-probe-planted` (deleted after).] Caveat: `rg` skips binary files; the Node scan should read every file as bytes so `favicon.ico` is covered too. CI-built bytes were not measured (builds are not byte-reproducible per the workflow header), only the local build with the same command.

### Pattern 2: Scan script contract
**What:** `node docs/architecture/leak-scan.mjs <dir>`; exit 0 clean, 1 leak found, 2 usage, 4 directory missing, unreadable or holding no files (mirrors `sync.mjs`: [VERIFIED: docs/architecture/sync.mjs:6-7] "Exit codes: 0 clean, 1 drift, 2 usage, 3 tooling missing (likec4 or yaml not installed), 4 a k8s fact source is missing or unreadable.").
**Report layout (discretion):** one line per finding: `<target> <file> count=<n> first=<matched text only, no surrounding line>`; a totals line per target including zero counts so a clean report is a table of zeros, not silence; the hostname allow-list verdict; and a positive check that `robots.txt` exists and contains `Disallow: /`. Write the same text to `$GITHUB_STEP_SUMMARY` from inside the script when that variable is set, so no `| tee` is needed.
**Why not print surrounding context:** bundles are single multi-hundred-KB lines, and a CI log of a public repository is public.

### Pattern 3: Workflow step placement
Place between "Verify the build holds the app shell" (line 77-78) and "Build the image without pushing" (line 80-81). It then runs on pull requests (the PR-time build has no secrets) and gates `publish` through `needs: build`. [VERIFIED: .github/workflows/architecture-site.yml:93] `if: github.event_name != 'pull_request' && github.ref == 'refs/heads/main'`.
```yaml
      - name: Scan the build for leaks
        run: node docs/architecture/leak-scan.mjs docs/architecture/site/out
```
Do not write `node ... | tee -a "$GITHUB_STEP_SUMMARY"`: with `shell` unspecified GitHub runs `bash -e {0}` and only an explicit `bash` adds `-eo pipefail`, so tee would mask the scan's exit code. [CITED: docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax, shell table]
Add `docs/architecture/leak-scan.mjs` to both `paths:` lists (push and pull_request, lines 27-32 and 34-39). The header deliberately excludes `sync.mjs` because the site bytes do not depend on it; the scan script is different, since changing it changes whether the same bytes pass.

### Anti-Patterns to Avoid
- **Scanning source `.c4` instead of the artifact.** `sync.mjs` generates strings from metadata the source scan cannot see (PITFALLS 11).
- **Quoting the deny-list in model prose.** A publish-scenario step that says "the scan looks for kubectl and ssh" fails its own scan. Describe the step as "reads every file of the build and fails on a hit".
- **A generic `\w+\.<namespace>` host rule** (matches LikeC4 FQNs, 5 shapes above).
- **Deriving Secret names from manifests.** Names referenced as Secrets (`letsencrypt-staging`, `letsencrypt-production`, `flux-system`, `postgres-backup`, `app-env`, `grafana-admin`, `grafana-tls`) overlap names the model draws on purpose: the model source has `flux-system` x5, `postgres-backup` x3, `letsencrypt-staging` x2, `letsencrypt-production` x2 (measured with `rg -w` on `docs/architecture/model`). Use a curated static list of names that are only Secrets, and keep the case-insensitive word `secret` as the generic net. Whether `postgres-backup` is also a Secret name was not checked.
- **Piping the scan through `tee`** (see Pattern 3).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Re-issuing the certificate | A `cmctl renew` script or Secret deletion | Edit `issuerRef.name` in place | Documented trigger; deleting the Secret or namespace spends an order too and is not recommended. [CITED: cert-manager.io FAQ "When do certs get re-issued?"] |
| Deep-link rendering proof | A new Playwright harness | `shot-scraper javascript` recipe from 03-06-PLAN.md:111 | Already used and recorded in Phase 3 (`DEEPLINK ... svg_nodes=184`). |
| Red/green CI proof | A new mechanism | Draft PR on a branch, "restore plus a comment" for green | 03-02-pr-proof.txt: a pure revert leaves an empty PR diff and starts no path-filtered run. |
| Uptime alert path | A second workflow or a new alert job | One more `run_check` in the existing script | The workflow's design (one verdict, one issue) already covers any URL/marker pair. |
| Approval evidence format | A new template | `DECISION Qn: <n> <label>; user reply: "<text>"` lines as in 03-DECISIONS.txt | Established format. |
| Fail-closed on empty input | Treating "no files" as clean | Exit 4 | "An empty scan and a clean scan look the same" (verify-comments.py states the same rule). |

**Key insight:** every mechanism this phase needs already exists in the repo; the work is a script, a workflow step, five edits and an ordering discipline.

## Runtime State Inventory

Not a rename or migration phase. Omitted. (State that does matter is covered under Flip: the live Secret `architecture-tls` keeps its name; the Certificate and Secret are cluster state not in git.)

## Flip (PUB-03) details

**Current git state:** [VERIFIED: k8s/overlays/architecture/certificate.yaml:12-14]
```
  issuerRef:
    name: letsencrypt-staging
    kind: ClusterIssuer
```
and `git log` shows one commit touching the file, `8e531d0 feat(architecture-site): serve the LikeC4 build ... on a staging certificate`, so "first committed on staging" is already true. The flip commit changes line 13 to `name: letsencrypt-production` and updates the header comment (lines 1-3, which say it "moves to letsencrypt-production in a later, separate commit"). Both issuers exist and already set `serviceType: ClusterIP` [VERIFIED: k8s/platform/edge/clusterissuers.yaml lines `name: letsencrypt-staging` / `name: letsencrypt-production`, each with `serviceType: ClusterIP`].

**Live state (read-only, 2026-10-29 14:40Z):** issuer `C=US, O=Let's Encrypt, CN=(STAGING) Dastardly Durum YR1`, `notBefore=Oct 28 20:31:07 2026 GMT`, `notAfter=Dec 31 2026`; `GET /` 200 `text/html`; `/robots.txt` is `User-agent: *` / `Disallow: /`; `HEAD /assets/missing.js` is `HTTP/2 404` `content-type: text/html` `server: nginx/1.30.6` `content-length: 153`; A record resolves via 8.8.8.8, no AAAA answer. [VERIFIED: curl/openssl/dig this session]

**Tag and divergence:** local `main` is 2 ahead and 2 behind `origin/main` (`git rev-list --left-right --count HEAD...origin/main` printed `2 2`). `origin/main` holds Flux bump commits; its overlay says `newTag: main-7-560fc77` [VERIFIED: `git show origin/main:k8s/overlays/architecture/kustomization.yaml`], local file says `main-6-f05e7ff` [VERIFIED: k8s/overlays/architecture/kustomization.yaml:13]. The first push in this phase needs a fetch and rebase (or merge) over Flux's commits; Flux bumps land again after every site image, so re-fetch immediately before the flip push too. Pushes use explicit refs; under zsh write `${SHA}:refs/heads/main` (STATE.md Phase 03 decision).

**Re-issuance:** the issuer annotation on the Secret will not match the new `issuerRef`, which is a documented trigger [CITED: cert-manager.io/docs/faq "When do certs get re-issued?"]. Same `secretName: architecture-tls`, so Traefik's `tls.secretName` needs no change. Flux `apps-architecture` has `interval: 10m` [VERIFIED: k8s/flux-system/apps-architecture.yaml:14]; Phase 3 measured push to Certificate Ready in 1.6 min (03-06-live.txt:34), so budget minutes, not seconds, and consider `flux reconcile` over SSH only if the plan authorizes a non-read-only call.

**Verification set (do not rely on `Ready=True` alone):** Ready is also true on the old staging Secret before re-issuance starts [ASSUMED]. Record all of: Certificate `status.revision` incremented; Secret annotation `cert-manager.io/issuer-name` equals `letsencrypt-production` [ASSUMED annotation key, verify by reading the Secret metadata, never its data]; `openssl s_client` issuer lacks `(STAGING)`; `curl -sS https://<host>/` exits 0 without `-k`.

**Failure and rate limit:** Let's Encrypt: "Up to 5 certificates can be issued per exact same set of identifiers every 7 days." with no override; "Up to 5 authorization failures per identifier can be incurred by one account every hour." Page updated August 5, 2026. [CITED: letsencrypt.org/docs/rate-limits] D-09 says "one careful attempt"; the accurate numbers are 5 successful issuances per week, so a revert-and-retry after a failure is possible, but every success spends one and a validation failure loop burns the hourly 5. A failed production issuance leaves the old staging Secret in place so the site keeps serving [ASSUMED]; revert the commit to return `issuerRef` to staging, which re-issues from staging at no production cost. Do not delete the Secret or Certificate.

**Stale text the flip makes false (find with the `rg` below, update in a commit AFTER Ready, not in the flip commit, so D-08's "one commit" stays one commit):**
- `k8s/overlays/architecture/certificate.yaml:1-3` header (same file, same commit as the flip is fine).
- `docs/INFRA_ARCHITECTURE.md:296-297` "on a Let's Encrypt staging certificate until the built site has passed the leak review".
- `docs/INFRA_RUNBOOK.md:485-488` "It is served on a Let's Encrypt staging certificate until the built site has passed the leak review, so a browser warns about the certificate on purpose."
- `docs/architecture/model/edge.c4:10` [VERIFIED] `description 'A staging issuer, defined in the repo and referenced today only by the Certificate of the architecture site. The prod, nonprod and monitoring Certificates all name the production issuer.'` After the flip "referenced today only by the Certificate of the architecture site" is false. Editing it after the review changes the image tag and breaks D-07, so reword it BEFORE the review to something true on both sides of the flip, for example "A staging issuer, defined in the repo for trial issuance before a Certificate moves to production." and drop the clause that names a current referrer.
- `docs/architecture/model/edge.c4:18` production issuer description lists prod, nonprod and monitoring only; add the architecture site in the same pre-review edit, worded so it is also true before the flip (for example "Issuer for the trusted certificates." without a referrer list).

## Probe (PUB-04) details

**Current file facts:** [VERIFIED: .github/workflows/uptime-check.yml:57-59] `# Runaway guard against a hung curl: worst case is 4 URLs x 3 attempts x 20s curl timeout plus 8 sleeps of`, `# 10s, which is 320s; the drill's fifth URL makes it 400s. 8 minutes leaves the rest for runner setup.`, `timeout-minutes: 8`. Per URL worst case is 3 x 20 + 2 x 10 = 80 s [VERIFIED by arithmetic from `ATTEMPTS: 3`, `SLEEP_SECONDS: 10`, `CURL_MAX_TIME: 20` at lines 79-81]. The drill is a boolean input `drill` (lines 45-48) that adds `DRILL_URL`, a dashboard share id of 32 zeros.

**Arithmetic:** base with the site = 5 URLs = 400 s; with the existing dashboard drill = 480 s = 8 min, so 8 does not fit (D-10 is right). If the site gets its own drill input and both drills can be set together, worst case is 7 URLs = 560 s, which leaves 40 s of a 10-minute cap for runner setup; either make the two drills mutually exclusive in the script, or set 12. The header text, the step name ("Probe both health endpoints and both dashboards"), the file's first line, the alert job's `DRILL_LINE` sentence ("one known-wrong dashboard URL was probed on purpose") and `docs/INFRA_RUNBOOK.md:55-62` all name the current set and need the site added. `docs/learning/09-build-quality-and-ci.md:987` also describes the drill input (grep it).

**Marker:** `id="likec4-root"` is in the built `index.html` [VERIFIED: `<div id="likec4-root"></div>` read from the build] and the workflow already greps that exact string at architecture-site.yml:78. The probe's `check_url` does substring matching in a `case` on the body with a 200 status; the env-var style is `UP_MARKER: '"status":"UP"'` (line 82), so `SITE_MARKER: 'id="likec4-root"'`. Because `nginx.conf` serves `index.html` for every extensionless path (location `/` with `try_files $uri /index.html`, lines 35-38), the root probe proves the image is the LikeC4 shell, not that the assets load; PITFALLS 15 suggested also probing one hashed asset, D-10 chose root only, and hashed names change per build so they cannot be a constant.

**Drill design:** a wrong URL is already covered in kind by `DRILL_URL`. For the site, the cleanest "wrong marker" drill is a second boolean input (for example `drill_site`) that probes the real site URL with a marker string that cannot occur. That keeps the TLS result clean so a red is attributable to the marker. Run order, each as `gh workflow run uptime-check.yml` then `gh run view`: baseline green with the site entry, drill red (alert job opens the issue, as in 261024-q91-ci-proof.txt), restored run green (the next passing run closes the issue). Both drills open and close the one issue and email the owner; say so at the checkpoint. Do not add the site entry before the production certificate is Ready: the probe never follows redirects and a staging chain gives `status 000`.

**Scheduled-run latency:** scheduled runs arrive 2.6 to 8.2 hours apart (header hole (a)); the evidence must use `workflow_dispatch` runs, not wait for cron.

## Links and URL behaviour (PUB-06) details

- **Baseline:** [VERIFIED: `rg -c -F "rud-vlad-473-architecture.duckdns.org"`] hits only in `docs/INFRA_RUNBOOK.md` (1), `docs/architecture/model/model.c4` (1), `k8s/overlays/architecture/ingressroute.yaml` (2), `certificate.yaml` (1). README.md and `docs/INFRA_ARCHITECTURE.md` have none, so the success-criterion `rg` is currently red for both. Add `https://kanban-board-rud-vlad-473-architecture.duckdns.org/` (root only, D-12) to each. README has a dashboards link block at lines 49-53; INFRA_ARCHITECTURE's paragraph to extend is lines 293-297.
- **Server behaviour:** [VERIFIED: docs/architecture/site/nginx.conf:25-38] `location ^~ /assets/ { try_files $uri =404; ... }`, `location ~ \.[A-Za-z0-9]+$ { try_files $uri =404; ...}`, `location / { try_files $uri /index.html; ... }`. A missing `/assets/*.js` therefore gives 404, measured on staging today (`HTTP/2 404`, `content-type: text/html`, 153 bytes) and in 03-06-live.txt:43. The criterion is "does not answer 200 with text/html", so 404 text/html satisfies it. Re-measure the same `curl -I` on the production host after the flip; nginx is unchanged by the flip, so a difference would point at the edge.
- **Deep link and refresh:** use the Phase 3 recipe (a fresh `shot-scraper` browser per run is a cold load) against `/view/signin-ok/` [VERIFIED: 03-06-live.txt:41 `DEEPLINK /view/signin-ok/ nav_status=200 svg_nodes=184`], with the certificate error flag removed since production is trusted. A refresh of a history URL is a fresh GET of the same path, so a second cold run is the equivalent evidence; an in-page `location.reload()` would need a Playwright script and is not required by the criterion [ASSUMED equivalence].

## Common Pitfalls

### Pitfall 1: The review approval is invalidated by a later model edit
**What goes wrong:** the owner approves tag `main-N-sha`; a doc-looking model edit (the stale `edge.c4` sentence) lands afterwards, builds `main-N+1`, Flux rolls it, and D-07 refuses the flip (correctly).
**How to avoid:** list every model-text and workflow-path change this phase needs, land all of them in the push that carries the scan, then review the tag that push produces. Only `docs/*.md`, `README.md`, `k8s/**` and `uptime-check.yml` edits are safe after review, because the site workflow's `paths:` do not list them [VERIFIED: architecture-site.yml:27-32].
**Warning signs:** `gh run list --workflow architecture-site.yml` shows a run after the approval timestamp.

### Pitfall 2: The scan fails its own model prose
**What goes wrong:** the publish-the-site scenario step added for the scan names its targets; the step text contains `ssh ` or `kubectl`.
**How to avoid:** describe the behaviour, not the list (Anti-Patterns). Run the scan on the build that includes the new step before pushing.

### Pitfall 3: Publish-the-site becomes "not as built"
**What goes wrong:** `scenarios-platform.c4:39-40` says sync check "runs first" and then `likec4 build ... the image is built once without pushing`. Adding a scan step to the workflow without a matching step makes the scenario incomplete [VERIFIED: scenarios-platform.c4:36-41]. Add `ci -> ci '<scan step>'` between lines 39 and 40, update the view description (line 36, which says "The drift check gates the build"), and re-cite lines in `03-SCENARIO-EVIDENCE.md` or a Phase 4 evidence file, because inserting a workflow step shifts every later line number the evidence cites.

### Pitfall 4: A loose regex turns the gate into noise
See the TRAP rows. A gate that fails on vendor code gets an allow-list added until it checks nothing. Pin the strict forms and have the test file include a fixture of each TRAP string that must pass.

### Pitfall 5: A red proof that is red for the wrong reason
**What goes wrong:** the uptime drill run while the site is still on a staging chain fails with `status 000` and looks like a marker failure. Likewise a "planted IP" proof where the build itself broke.
**How to avoid:** the baseline green run immediately before each drill; for the scan, assert the failing output names the IPv4 target and the planted literal.

### Pitfall 6: Pipes mask the exit code (see Pattern 3)

### Pitfall 7: Flux bump and flip racing on `main`
Flux commits `chore(flux): bump images` to `main` after each site image (four such commits exist on `origin/main` already). A flip pushed from a stale local main is rejected; a force-push would drop Flux's commit and must never be used. Fetch, rebase, push by explicit refspec, then re-read the overlay tag to confirm the approved tag was not superseded in between.

### Pitfall 8: Public log contents
CI logs of this public repository are public. A report that prints the surrounding line of a hit (a whole bundle line) is both unreadable and a disclosure. Print matched text only, with file and count.

### Pitfall 9: Hook coverage gap for the new files
`.githooks/pre-commit` runs the drift check when a staged path is under `docs/architecture` [VERIFIED: .githooks/pre-commit:209 `src/main/resources/db/migration k8s docs/architecture \`], so the scan files trigger it, but nothing in the hook builds the site (and PITFALLS 12 says never wire a build into the hook). The scan is therefore gated by CI only; do not claim a hook gate.

### Pitfall 10: Credential-shaped fixtures
A test fixture holding a literal `AKIA...` or a PEM header trips gitleaks on commit. Build such strings at runtime (`'AK' + 'IA' + ...`). A literal IPv4 such as `10.20.30.40` is not a gitleaks credential shape [ASSUMED: gitleaks default rules; run the hook to see].

## Code Examples

### Strict scan core (illustrative, not final)
```javascript
// Source: patterns measured in this research; structure follows docs/architecture/sync.mjs conventions
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'

const IPV4 = /(?<![0-9.])(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])(?:\.(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}(?![0-9])/g
const TARGETS = [
  ['ipv4', IPV4],
  ['ssh', /ssh /g],
  ['kubectl', /kubectl/g],
  ['secret', /secret/gi],
]
// Read bytes as latin1 so a binary file cannot throw and every byte is matched.
function* files(dir) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, e.name)
    if (e.isDirectory()) yield* files(p)
    else yield p
  }
}
```
Hostname allow-list: collect `/[A-Za-z0-9.-]*duckdns\.org/g` matches and fail on any not in the four-host set.

### Deep-link probe (Phase 3 recipe, certificate flag removed)
`shot-scraper javascript https://<host>/view/signin-ok/ "<Promise polling document.querySelectorAll('svg *').length every 250 ms up to 15 s, resolving {count, status: performance.getEntriesByType('navigation')[0].responseStatus}>"` [CITED: 03-06-PLAN.md:111]. Expected: status 200 and count above 0 (184 on staging).

### Flip verification
```fish
set h kanban-board-rud-vlad-473-architecture.duckdns.org
echo | openssl s_client -connect $h:443 -servername $h 2>/dev/null | openssl x509 -noout -issuer -dates
curl -sS -o /dev/null -w '%{http_code}\n' https://$h/        # no -k
curl -sI https://$h/assets/missing.js | head -3                # expect 404, not 200
```
The cluster-side reads (Certificate `status.conditions`, `status.revision`, pod image) go through the repo's read-only SSH pattern, `ssh netcup-prod 'k3s kubectl ...'` [CITED: docs/INFRA_RUNBOOK.md:65].

## Trade-off Matrices for the planner

CLAUDE.md requires a 2-alternative matrix before any PLAN.md.

| Approach | Pros/Cons | Why Picked |
|----------|-----------|-----------|
| Scan the built directory in the build job with a Node script (chosen) | Sees generated strings; runs on PRs with no secrets; gates publish via `needs`. Cost: ~0.1 s of CPU on 6.8 MB; regex set must be curated. | Matches D-01; only artifact scanning can see `sync.mjs`-generated text. |
| Scan `.c4` source in the pre-commit hook | Fast local feedback. Blind to generated strings and to what LikeC4 embeds; rejected by PITFALLS 11. | Rejected; could be a later cheap hint, not a gate. |
| Standing fixture tests (`node:test`) + one real-build plant + draft-PR proof (recommended for D-03) | Fast, covers every target, shows the real path once. | Fits "delete first": no new harness kind. |
| New `drill.fish` kind that builds the site per drill | Literal reading of D-03, one ledger for all drills. Each drill costs a ~26 s build here; `run` is hard-wired to `sync.mjs check`, so it needs a second runner. | Offer to owner as an option (Open Question 1). |
| Second boolean drill input `drill_site` for uptime (recommended) | Keeps existing `drill=true` semantics and docs; wrong-marker red is isolated. 7 URLs worst case if both set. | Pick with mutual exclusion or `timeout-minutes: 12`. |
| Extend the existing `drill` boolean to also add a site URL | One input; worst case 6 URLs, 480 s, fits 10 min (D-10 arithmetic). Changes what an existing documented input does and cannot isolate a marker failure from a URL failure. | Fallback if the owner wants D-10's number unchanged. |

Performance, memory, security: the scan is linear in bytes read and holds one file at a time; no secret enters the build job; the scan output is public so it prints matched text only; the flip's only irreversible effect is a production certificate entering CT logs (hostname already public in the repo; `robots.txt` is not access control).

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Leak review by eye on the source | Scan the built artifact | PITFALLS 11 (measured bundle contents) | Generated and embedded strings are caught. |
| Delete the Secret to force re-issue | Change `issuerRef` in place | cert-manager FAQ | One order, no downtime on the old Secret. |
| Let's Encrypt limits | 5 certs / exact set / 7 days, no override (page updated 2026-08-05) | current | The flip is a one-shot with a few spare. |

**Deprecated/outdated:** the CONTEXT claim "the rate limit allows one careful attempt" is stricter than the documented limit (see Flip); keep the cautious procedure anyway.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Ready=True` can still read true on the old staging Secret right after the issuerRef edit, so Ready alone is not proof | Flip | If cert-manager flips Ready to False immediately, the extra checks are redundant but harmless. |
| A2 | The Secret annotation key is `cert-manager.io/issuer-name` | Flip | A wrong key makes one verification line fail; fall back to issuer CN and revision. |
| A3 | A failed production issuance leaves the old staging Secret serving | Flip | If wrong, the site could lose TLS during a failed flip; the revert step still restores staging. |
| A4 | A second cold `shot-scraper` run is equivalent evidence for "refresh" | Links | If the owner wants an in-page reload, add a Playwright step. |
| A5 | gitleaks default rules do not flag a literal dotted-quad fixture | Pitfalls | The commit hook would refuse; replace with a runtime-assembled string. |
| A6 | CI-built bytes give the same zero-hit baseline as the local build | Pattern 1 | A CI-only hit would show up in the first PR run and need a regex or allow-list decision. |
| A7 | `postgres-backup` may be a Secret name as well as a CronJob/element name | Anti-Patterns | If it is a Secret, a curated list must exclude it from the model-visible names; unverified. |

## Open Questions

1. **How does D-03's drill row and hook self-test case fit a scan of a built directory?**
   - What we know: `drill.fish` `run` calls `node $sync check --root $scratch --model $scratch/model` only [VERIFIED: drill.fish:40-45]; `drills.tsv` has 51 lines (header + 50 rows) of kinds sed/write/delete/mv/none and three helpers; the hook runs `sync.mjs check` and never builds (hook line 222, PITFALLS 12 forbids a build in the hook); the self-test copies the hook and runs commit shapes against it.
   - What's unclear: whether the owner wants a literal drill row (needs a second runner that builds, ~26 s per drill) or accepts the equivalent proof.
   - Recommendation: the CONTEXT says these are best-judgment defaults. Use fixture tests for each target, one recorded real-build plant (red then green, in the phase evidence), and a draft-PR run showing the workflow step red then green. Present the literal-drill alternative as option 2 at the plan checkpoint with its cost, and do not add a hook case (the hook cannot see the artifact).

2. **Which uptime drill shape?** (`drill_site` second input vs extending `drill`; timeout 10 vs 12.) Recommendation above; the owner's D-10 number (10) holds only if the two drills cannot both be on.

3. **Where does the approved tag come from at flip time?** The overlay file on `origin/main` shows the tag Flux last bumped; the cluster shows what runs. Recommendation: record both at approval, and at flip compare the overlay `newTag` on freshly fetched `origin/main`, the pod image via the read-only SSH call, and the approved tag; stop on any mismatch. Not measured this session: the live pod image (SSH not used).

4. **Is `secret` kept as a target?** Measured answer for D-04: 0 hits today with `(?i)secret`. Recommend keep. The owner confirms at the plan checkpoint.

5. **Scan script placement and workflow trigger.** `docs/architecture/leak-scan.mjs` needs adding to the workflow `paths:`; alternatively place it under `docs/architecture/site/`, which is already a trigger and is not copied into the image (Dockerfile copies only `nginx.conf` and `out/`). Recommendation: keep it beside `sync.mjs` and add the path.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | scan, `node:test`, build | yes | v24.19.0 | — |
| `docs/architecture/node_modules` (likec4) | local build for the plant proof | yes | likec4 1.60.0 | `pnpm --dir docs/architecture install` |
| ripgrep | measuring and verification | yes | — | — |
| fish | helper scripts | yes | 4.2.1 | — |
| shot-scraper | deep-link probe | yes | `~/.local/bin/shot-scraper` (1.12 per tooling notes) | Playwright script |
| gh | dispatch, run evidence, PR proof | yes | 2.98.0, logged in | — |
| Docker | pre-commit `fastTest` on the flip commit | yes | client 29.1.3 (daemon reachability not probed beyond `docker info` printing the client header) | start Docker before the flip commit |
| SSH alias `netcup-prod` | read-only cluster checks | alias resolves (`ssh -G` printed a hostname); not connected | — | none; blocking if the owner is unavailable |
| DNS A record for the host | ACME HTTP-01 | yes (resolves via 8.8.8.8, no AAAA) | — | — |

**Missing dependencies with no fallback:** none found.

## Security Domain

`security_enforcement` is enabled in `.planning/config.json` (`"security_enforcement": true`, `"security_asvs_level": 1`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | static public site, no login |
| V3 Session Management | no | none |
| V4 Access Control | partly | `production`/`staging` environments fenced to `main` (03-DECISIONS Q7); only the `publish` job holds the registry token |
| V5 Input Validation | yes | scan reads untrusted model text from PRs as data only; report prints matched text, not lines; no `${{ }}` interpolation of PR-controlled strings |
| V6 Cryptography | yes | cert-manager and Let's Encrypt; never hand-roll |
| V9 Communications | yes | production certificate, `curl` without `-k` as the check |
| V14 Config | yes | `robots.txt` `Disallow: /` kept, documented as not access control (D-05) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Internal detail embedded in the shipped model bundle | Information disclosure | Artifact scan before publish, review gate before the production cert |
| Scan output leaks the matched context into a public CI log | Information disclosure | Print matched text and count only |
| Scan weakened by an allow-list that grows | Tampering | Empty allow-list except the four hostnames; any exception needs a recorded reason in the script |
| PR-time run publishes or holds a token | Elevation of privilege | Scan lives in the `build` job, which has no secrets (existing design) |
| Rate-limit exhaustion by retry loops | Denial of service | One flip; revert before diagnosing; no Secret deletion |
| Hostname in CT logs after the flip | Information disclosure | Accepted: hostname already public in the repo; `Disallow: /` is not access control |
| Wrong marker/URL drill leaves an open alert issue | Repudiation/noise | Run a recovery dispatch; confirm the issue closed |

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `.github/workflows/architecture-site.yml`, `.github/workflows/uptime-check.yml`, `.github/workflows/invariant-checks.yml` (134-168), `k8s/overlays/architecture/{certificate,ingressroute,kustomization}.yaml`, `k8s/base/architecture-site/architecture-site.yaml`, `k8s/platform/edge/clusterissuers.yaml`, `docs/architecture/{sync.mjs (1-80, 300-340, 620-723), drill.fish, drills.tsv, drill-output.txt (head/tail), likec4.fish, package.json, site/Dockerfile, site/nginx.conf}`, `docs/architecture/model/{edge.c4 (1-30), scenarios-platform.c4 (25-65)}`, `scripts/verify-comments.py` (1-70), `scripts/pre-commit-likec4-selftest.py` (case list), `scripts/verify-k8s-invariants.py` (1-60), `.githooks/pre-commit` (grep), `docs/INFRA_RUNBOOK.md` (50-100, 478-497), `docs/INFRA_ARCHITECTURE.md` (286-305), `README.md` (40-65), `.planning/config.json`, workstream CONTEXT/REQUIREMENTS/STATE/ROADMAP, PITFALLS 10, 11, 12, 15, 03-06-live.txt, 03-DECISIONS.txt, 03-02-pr-proof.txt, 261024-q91-ci-proof.txt.
- Local measurements: build, scan counts, planted-IP build, live curl/openssl/dig (all this session).
- cert-manager docs via Context7 `/cert-manager/website`, FAQ "When do certs get re-issued?".
- letsencrypt.org/docs/rate-limits (fetched, updated August 5, 2026).
- docs.github.com workflow syntax, shell table (unspecified shell is `bash -e {0}`, explicit `bash` adds `-eo pipefail`).

### Secondary (MEDIUM confidence)
- PITFALLS.md claims about cert-manager v1.21.1 pin and the PSL measurement (research file; the pin file was not re-read).

### Tertiary (LOW confidence)
- None beyond the Assumptions Log.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing new is installed.
- Architecture (placement, ordering): HIGH, from the workflow and manifests read.
- Scan baseline: HIGH for the local build; MEDIUM that CI bytes match (A6).
- Flip behaviour on a live cluster: MEDIUM, documented not exercised (A1-A3).
- Pitfalls: HIGH for the ones measured (regex traps, FQN host false positive, tag invalidation); MEDIUM for gitleaks (A5).

**Research date:** 2026-10-29
**Valid until:** 2026-11-05 for the live-state facts (tag, divergence from origin); 30 days for the rest, unless LikeC4 is upgraded (vendor bundles, the `robots.txt` default and the regex baseline all change with it).
