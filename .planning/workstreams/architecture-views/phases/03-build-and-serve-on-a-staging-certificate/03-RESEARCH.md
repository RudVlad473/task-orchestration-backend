# Phase 3: Build and Serve on a Staging Certificate - Research

**Researched:** 2026-10-28
**Domain:** static-site publishing on an existing k3s + Flux + Traefik + cert-manager platform (LikeC4 1.60.0 build output, Docker Hub, GitHub Actions, Let's Encrypt staging)
**Confidence:** HIGH for the server measurement and every repo fact (each run or read this session); MEDIUM for the Flux/Docker Hub behaviour not exercised end to end; the VPS memory numbers do not exist yet (the ladder is a plan task).

## Summary

Phase 3 adds one workflow, one small image, one namespace, one Flux Kustomization and the model elements that the existing honesty checks force. Nothing new is invented: the app's own path (`main-<run>-<sha7>` tag, `ImageRepository` + `ImagePolicy`, a setter marker under `k8s/overlays`, Certificate + two IngressRoutes) is copied one-for-one. Two findings from this session change the earlier research. First, `infra/` is not in `deploy.yml`'s `paths-ignore`, so STACK's `infra/architecture-site/` would start the full app pipeline (tests, prod and nonprod image push, Flyway over SSH) on every Dockerfile edit. Second, `sync.mjs` reads Deployments only under `k8s/base/`, `k8s/data/`, `k8s/platform/` and `k8s/monitoring/`, so a Deployment living only in an overlay is invisible to the workload check and criterion 5 ("cover the site's workload") would fail.

Open Question 1 is settled by measurement on the real `likec4 build` output. SWS 2.44.1 serving `404.html` (candidate b) renders deep links in a browser but answers them with status 404. nginx-unprivileged with the fallback limited to extensionless paths (candidate a) answers deep links 200 and a missing `/assets/*.js` with a real 404, and uses about half the memory. A third candidate, SWS with two `rewrites` (c), works for `/view/**` and `/project/**` only and cannot stop `immutable` being stamped on a 404 in 2.44.1. Recommendation: nginx-unprivileged `1.30.6-alpine-slim` pinned by digest, config baked into the image, no ConfigMap.

Two things need a human: the public Docker Hub repository (currently answers 404 to an anonymous API call) and an authorization checkpoint for the memory ladder, which must create a scratch pod on the VPS (a cluster write, so not covered by "live checks are read-only"). The DuckDNS record already resolves to the VPS.

**Primary recommendation:** Build with `likec4 build --use-dot=false` on the runner after `sync.mjs check`, bake the output into `nginx-unprivileged` (dir `docs/architecture/site/`), publish `main-<run>-<sha7>` to `rudenkovladimir/kanban-board-backend-architecture`, wire it as `k8s/base/architecture-site/` + `k8s/overlays/architecture/` + Flux `apps-architecture`, issue the Certificate on `letsencrypt-staging` only, and land the model element, host claim, namespace node and `traefik ->` edge in the same commit as the base manifest.

<user_constraints>
## User Constraints (from CONTEXT.md)

No CONTEXT.md exists for this phase (`/gsd-discuss-phase` was not run). The binding scope is the ROADMAP Phase 3 section, STATE.md decisions and the user's instruction for this research ("research first, measure OQ1"). Copied verbatim from those sources:

### Locked Decisions
- Goal (ROADMAP): "Every model change on `main` rebuilds the static site. Flux serves the static build at the architecture hostname over HTTPS on a Let's Encrypt staging certificate. The publish path is drawn in the model as it was built, not as planned."
- Requirements: PUB-01, PUB-02, SCEN-08. PUB-03 (flip), PUB-04 (uptime probe), PUB-05 (leak scan and review), PUB-06 (links, URL behaviour) are Phase 4.
- STATE.md: "Mermaid migration and the retirement of `docs/diagrams/` are out of scope. Docker Hub tag cleanup for the site repository is deferred to v2 (V2-05)."
- STATE.md: "[Defaults accepted]: Monitoring, cert-manager and Flux appear in the Physical view. The error-status scenario shows client-visible discriminators only. URLs are history-style. `robots.txt` keeps `Disallow: /`."
- ROADMAP planning note: "No uptime probe yet, because a staging certificate fails its TLS check. Live checks over SSH are read-only unless the plan says otherwise (PROJECT.md key decision)."
- REQUIREMENTS out of scope: "Exposing `likec4 start` ... Served `/@fs/etc/hostname` in measurement; a file-read hole."

### Claude's Discretion
- The server (Open Question 1), the file layout and names (Open Question 2), the HTTP-01 solver `serviceType` (Open Question 8), the workflow shape, the ladder method and how Open Question 8's other items fold in are all open for recommendation. Nothing is locked on them.

### Deferred Ideas (OUT OF SCOPE)
- Phase 4: leak scan, user review, production flip, uptime probe, README and docs links.
- V2-05 retention job for the third Docker Hub repo. V2-01 to V2-04.
- A CSP and response-header policy (no v1 requirement; the cold-load policy is measured in PITFALLS 7, the interactive walk is not).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PUB-01 | CI builds the static site (`likec4 build`) into an image pushed to its own Docker Hub repo, rebuilt only on source-path changes. | Workflow shape, path filters verified against the Flux bump commits, `--use-dot=false` finding, PR-time build with no secrets, bootstrap order, Docker Hub repo state. |
| PUB-02 | k8s manifests and Flux wiring serve the image at the DuckDNS architecture hostname. Only static build output is exposed, never `likec4 start`. | Measured server comparison, layout reconciliation, gate I12 design, I3/I4/I5/I10/I11 consequences, solver decision, Certificate on staging, ladder. |
| SCEN-08 | Publish-the-site scenario. | Model changes forced by the existing checks, the 5-participant cap, the `[...]` edge-label trap, evidence-file format, `source` metadata traps. |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

From `.claude/CLAUDE.md` and the user's global `~/.claude/CLAUDE.md`:

- Work enters through a GSD command; no direct repo edits outside a GSD workflow.
- **Before creating or approving any PLAN.md: document 2 alternate technical approaches, a 3-column Trade-off Matrix [Approach | Pros/Cons | Why Picked], and non-obvious performance, memory or security trade-offs.** This file supplies the matrices for the server (OQ1) and layout (OQ2) decisions; the planner copies them.
- Never auto-execute code blocks without explaining the core data flow in 3 sentences or less.
- Read `docs/SESSION_LESSONS.md` before a phase-execution session (git hygiene).
- Comments are plain text (CODE_STYLE rule 14, enforced by `scripts/verify-comments.py`). That lint covers `.yml`, `.yaml`, `.toml`, `Dockerfile`, `.conf`, `.sh` and `.py` [VERIFIED: scripts/verify-comments.py:159-171], so the new workflow, Dockerfile, `nginx.conf` and k8s YAML are linted. It bans planning ids in comments (`Phase 3`, `PUB-02`-style ids for listed prefixes, quick-task ids, `D-NN`, plan numbers) [VERIFIED: scripts/verify-comments.py:80-87], a block of 4+ prose lines must open with a summary of at most 2 lines, and narration past 8 lines sits behind a `Decisions:` marker.
- Every commit runs `.githooks/pre-commit` (about 4 minutes). `--no-verify` is allowed for docs, planning and config-only commits, but not for feature-heavy ones (Java, migrations, k8s manifests). The manifest commits here are feature-heavy; the workflow-only and docs-only commits are not.
- Services use field `@Autowired`, Testcontainers etc.: not relevant to this phase.
- Global: commit finished work on the current branch with a conventional message that says why, only the paths touched, never push, never amend. Pin every fix with a test that fails without it (here: the red-then-green drills). Temporary work stays inside the repo and is deleted afterwards. "Ask before merging to main" (MEMORY.md): the bootstrap needs commits on `main`, so the plan must stop at that gate.

## Decision Register (what the planner should treat as settled)

| # | Question | Decision | Basis |
|---|----------|----------|-------|
| OQ1 | Server | **nginx-unprivileged `1.30.6-alpine-slim@sha256:5d8bb9437d74a600ec5dbf90c035451c724dc33eac8d3a71078f6f8612c345f6`**, extensionless-only fallback, config baked into the image | Measured below. (b)'s deep link answers 404; it renders, but it does not count as "resolving". |
| OQ2 | Layout | Dockerfile + `nginx.conf` in `docs/architecture/site/`; `k8s/base/architecture-site/`; `k8s/overlays/architecture/`; Flux `apps-architecture`; namespace `architecture`; Deployment `architecture-site` | Reconciliation below. |
| OQ3 | Operator actions | DNS: none (resolves today). Docker Hub: create the repo public, by hand, before the first push | Live checks below. |
| OQ8 | Solver | Set `serviceType: ClusterIP` on both ClusterIssuers, in a separate commit before the Certificate, as a user-confirmed decision | cert-manager docs; baseline verified live. |
| Boot | Order | image first (dispatch), ladder, then one atomic manifest+model commit | Bootstrap section. |
| Gate | `likec4 start/preview/--listen` | New invariant I12 inside `scripts/verify-k8s-invariants.py` (not a new script) | Design below. |

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Model drift gate before publish | CI runner | Pre-commit hook | `sync.mjs check` is the existing gate; the hook is the fast local copy, CI the backstop. |
| Static site generation | CI runner (build job) | — | Uses the lockfile-pinned LikeC4; no toolchain inside `docker build`. |
| Packaging and registry | CI runner (publish job) | Docker Hub | Same `DOCKERHUB_TOKEN` and `production` environment as the app. |
| Tag selection and rollout | Flux (image-reflector, image-automation, kustomize) | GitHub (bump commit) | Pull-based GitOps; nothing external pushes into the cluster. |
| TLS termination and routing | Traefik | cert-manager | The pod speaks plain HTTP on 8080. |
| Certificate lifecycle | cert-manager + Let's Encrypt staging | DuckDNS (DNS) | HTTP-01 through the `web` entry point. |
| Static file serving and history fallback | nginx pod | — | The server owns the extensionless-only fallback and the real 404 for assets. |
| Client-side routing | Browser (LikeC4 SPA, TanStack Router) | — | Routes are path-based with `trailingSlash: always`. |
| Public exposure control | Gate I12 + route shape (I10) | Leak scan (Phase 4) | Only static output is ever deployed. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| likec4 | 1.60.0 (already pinned) | `likec4 build` | The binary the drift gate uses [VERIFIED: docs/architecture/package.json, read this session]. |
| yaml | 2.9.1 (already pinned) | `sync.mjs` k8s readers | Existing dependency. |
| pnpm | 11.20.0 | frozen-lockfile install | `packageManager` field; local `pnpm --version` printed 11.20.0. |
| Node | 24.19.0 | runs LikeC4 and `sync.mjs` | Same pin as the drift job [VERIFIED: .github/workflows/invariant-checks.yml:155-157]. |
| nginxinc/nginx-unprivileged | `1.30.6-alpine-slim`, digest `sha256:5d8bb9437d74a600ec5dbf90c035451c724dc33eac8d3a71078f6f8612c345f6` | static server | Hub API tag digest equals the digest the measured image was built from; `USER 101`, listens 8080 [VERIFIED: docker image inspect, Docker Hub API]. |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `actions/checkout`, `docker/setup-buildx-action`, `docker/build-push-action` | SHAs already used in `backup-image.yml` | publish job (holds the Docker Hub secret) | Reuse the exact pins: `fbc6f3992d24b796d5a048ff273f7fcc4a7b6c09 # v5`, `8d2750c68a42422c14e847fe6c8ac0403b4cbd6f # v3`, `10e90e3645eae34f1e60eeb005ba3a3d33f178e8 # v6` [VERIFIED: .github/workflows/backup-image.yml:39,42,55]. |
| `pnpm/action-setup@v6`, `actions/setup-node@v5` | tag | build job (no secrets) | As in the drift job [VERIFIED: invariant-checks.yml:150,155]. |
| `actions/upload-artifact@v4` (+ matching `download-artifact`) | tag | hand the built `site/out` to the publish job | upload@v4 is already used [VERIFIED: rg of .github/workflows]; download-artifact is not used yet (tag choice is [ASSUMED]). |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| nginx-unprivileged | SWS 2.44.1 (scratch, 12.1 MB local) | Smaller surface, but no status filter on header rules and no way to get 200 deep links without a hard-coded route list. |
| Extensionless fallback | `--use-hash-history` | Removes the problem but contradicts the accepted history-style default. |
| Two jobs with an artifact hand-off | one job with `environment: production` | A PR job that names an environment can reach its secrets: `production` has `protection_rules: []` and `deployment_branch_policy: null` on a public repo [VERIFIED: gh api, this session]. |

**Installation:** none. `pnpm --dir docs/architecture install --frozen-lockfile` only; no `package.json` or lockfile change.

**Version verification:** `likec4` 1.60.0 and `yaml` 2.9.1 come from the committed lockfile. The nginx tag was checked against the Docker Hub API on 2026-10-28: `1.30.6-alpine-slim` and `stable-alpine-slim` both point at `sha256:5d8bb9437d74...`, last pushed 2026-10-19; mainline `1.31.7-alpine-slim` is `sha256:a8cb0f33d51f...`.

## Package Legitimacy Audit

Phase 3 installs no new npm, PyPI or crates package. The seam was run on the two npm dependencies it relies on:

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| likec4 | npm | 1.60.0 published 2026-10-15 (13 days) | 135,303/wk | github.com/likec4/likec4 | SUS (reason: `too-new`) | Pre-existing pinned dependency, lockfile-gated, accepted in Phases 1 and 2. No new install in this phase, so no `checkpoint:human-verify`. Do not bump it here. |
| yaml | npm | 2.9.1 published 2026-09-11 | 253,824,254/wk | github.com/eemeli/yaml | OK | Approved (existing). |
| nginxinc/nginx-unprivileged | Docker Hub | registered 2018-06-07 | 1,308,202,006 pulls | publisher `nginxinc`, not private | not covered by the npm/PyPI/crates seam | Approved by Hub metadata; pin by digest. [VERIFIED: Hub API] |
| actions/download-artifact | GitHub Actions (first party) | — | — | github.com/actions | not covered by the seam | Version choice is [ASSUMED]; pair with `upload-artifact@v4`. |

**Packages removed due to [SLOP] verdict:** none.
**Packages flagged as suspicious [SUS]:** `likec4` (pre-existing, see above).

## Architecture Patterns

### System Architecture Diagram

```
 push / PR / dispatch
   |  .github/workflows/architecture-site.yml
   |  paths: docs/architecture/model/**, docs/architecture/site/**, package.json, pnpm-lock.yaml, the workflow
   v
 [build job: no environment, no secrets, runs on PR too]
   checkout -> pnpm install --frozen-lockfile -> node sync.mjs check --(drift)--> RED, stop
        -> pnpm exec likec4 build --use-dot=false -o site/out model
        -> docker build (push: false)  -- validates the Dockerfile
        -> upload-artifact site/out
   |  (needs: build; only push or dispatch on refs/heads/main)
   v
 [publish job: environment production -> DOCKERHUB_TOKEN]
   download-artifact -> docker login -> build-push main-<run>-<sha7> -> Docker Hub
   v
 Docker Hub  rudenkovladimir/kanban-board-backend-architecture   (must be public)
   ^  scan 5m                                          (ImageRepository)
 Flux image-reflector -> ImagePolicy (numerical on <num>) -> image-automation commits
   "chore(flux): bump images" into k8s/overlays/architecture/kustomization.yaml  (touches nothing in the site filter)
   v  GitRepository poll 1m -> Kustomization apps-architecture (dependsOn platform, edge)
 namespace architecture: Deployment architecture-site (nginx, :8080, /tmp emptyDir) + Service
   ^                                   ^
 Certificate architecture --letsencrypt-staging--> Secret architecture-tls   (cert-manager, HTTP-01 via solver Ingress)
   ^
 client --HTTPS--> Traefik (IngressRoute architecture-https, websecure, tls architecture-tls)
                 \--HTTP-->  IngressRoute architecture-http (web, redirect-https, !PathPrefix(acme-challenge))
```

### Recommended Project Structure
```
.github/workflows/architecture-site.yml          build + publish
docs/architecture/site/Dockerfile                FROM nginx-unprivileged@digest; COPY nginx.conf; COPY out/ /srv/site/
docs/architecture/site/nginx.conf                the measured server config
docs/architecture/site/out/                      build output, gitignored by the existing `out/` rule
k8s/base/architecture-site/{kustomization.yaml,architecture-site.yaml}   Deployment + Service (read by the workload check)
k8s/overlays/architecture/{kustomization.yaml,ingressroute.yaml,certificate.yaml}
k8s/platform/namespaces.yaml                     + Namespace architecture
k8s/platform/edge/clusterissuers.yaml            + serviceType: ClusterIP (separate commit)
k8s/flux-system/{apps-architecture.yaml,kustomization.yaml,image-automation.yaml}
docs/architecture/model/{model.c4 or a new site.c4,deployment.c4,views.c4,delivery.c4}
scripts/verify-k8s-invariants.py (+ selftest)    I12
```

### Pattern 1: build once on the runner, hand the bytes to the publisher
**What:** the `build` job runs the gate, builds the site and validates the Dockerfile with `push: false`; the `publish` job downloads the same `site/out` and is the only job that names `environment: production`.
**When to use:** always, because the PR-time run must hold no secrets and what was tested is what is pushed.

### Pattern 2: authored shell, checked metadata (existing)
The site is one element with `workload 'Deployment/architecture-site'`, placed by `instanceOf` in a namespace node titled `architecture`; the host goes in the `host` list on Traefik; one authored `traefik -[sync]-> <site> 'HTTP :8080'` relationship keeps the instance drawn.

### Anti-Patterns to Avoid
- **Dockerfile under `infra/`:** every edit starts the app pipeline (see Pitfall 1).
- **Deployment only in the overlay:** invisible to `workloads()` (see OQ2).
- **`k8s/**` in the site workflow's `paths`:** Flux's bump commit would retrigger it. Verified bump commits touch only `k8s/overlays/*/kustomization.yaml` (`git log --author=fluxcdbot --stat`: bb1d043, 5f9537b, 70eb7ea).
- **A placeholder route or a Certificate in `edge` for this host:** the Grafana 503 race (I11).
- **A second logical `ci -> dockerhub` relationship with a different title:** the deployment edge becomes `[...]`.

## Open Question 1: server measurement (done)

**Method.** `likec4 build -o <scratch> model` of the model at HEAD (5c9c9d8) with likec4 1.60.0: 22 files (17 in `assets/`), 6.7 MB, `404.html` byte-identical to `index.html` (`cmp`), `robots.txt` = `User-agent: *` / `Disallow: /`, `index.html` contains `id="likec4-root"`. Four containers on loopback ports, each `--read-only --cap-drop ALL --security-opt no-new-privileges --memory 32m`, nginx also `--user 101:101 --tmpfs /tmp`, SWS `--user 65532:65532`. Host: WSL2, 4 CPUs, Docker 29.1.3 (the VPS also reports `nproc` = 4). Scratch lived in `docs/architecture/out/oq1/` (gitignored), was deleted afterwards; the containers were removed and `ss -ltn` showed no bound port in 18081-18089. The three images pulled for the test (nginx, node slim) were removed too.

**Result (status and content-type; all bodies of a 200/404 on a non-asset path were the app shell, 816 bytes, except where noted).**

| Request | (a) nginx, extensionless fallback | (b) SWS 2.44.1, default `404.html` | (c) SWS + rewrites `/view/**`, `/project/**` |
|---|---|---|---|
| `/` | 200 `text/html` | 200 `text/html; charset=utf-8` | 200 |
| `/view/signin-ok/` (cold deep link) | **200** `text/html` | **404** `text/html; charset=utf-8` | 200 |
| `/view/signin-ok` (no slash) | 200 | 404 | 200 |
| `/project/default/view/production/` | 200 | 404 | 200 |
| `/embed/production/`, `/export/production/` | 200 | 404 (not probed on (b); every path that is not a file answered 404) | **404** |
| `/assets/missing.js` | **404** `text/html`, no Cache-Control (final conf) | **404** `text/html`, `Cache-Control: public, max-age=31536000, immutable` | same as (b) |
| `/missing.png` | 404 | 404 | 404 |
| `/@fs/etc/hostname` | 200 `text/html`, body sha256 equals `index.html` (not the file) | 404 `text/html` (the shell) | 404 |
| `/no/such/page` | 200 (shell) | 404 | 404 |
| `/assets/main-*.js` | 200 `application/javascript`, immutable | 200 `text/javascript`, immutable | same |

Headless Chromium (`shot-scraper`) on `/view/signin-ok/`, cold: all three render 38 SVG nodes; seven subresource requests, all 200, one host; the navigation entry's `responseStatus` is 404 for (b) and 200 for (a) and (c). A nginx config where the `/assets/` location used `add_header ... always` stamped `immutable` on the 404 as well; dropping `always` there removed it (the final conf below).

**Memory** (cgroup `memory.stat` `anon`, after every file once plus 200 gzip requests for the 1.2 MB `main-*.js`): nginx (4 workers) 4.0 MiB idle, 4.0 MiB after; SWS 7.3 MiB idle, 8.5 MiB after. STACK recorded 16.3 MB for SWS under a 16 MiB cap; this session did not reproduce that figure (different run, same binary). The VPS ladder decides the real numbers.

**Container build finding (settles the `--use-dot` doubt).** `likec4 build` inside `node:24.19.0-slim` without a flag exits 1 with `not found: dot` errors and `Error: no views found`; with `--use-dot=false` it builds in 2.1 s. A GitHub-hosted runner is not a container, but pass `--use-dot=false` explicitly in the workflow anyway. The host build and the container build produced different hashed names (`main-BdZFwx7C.js` vs `main-B7gpOCNZ.js`): builds are not byte-reproducible, as STACK found.

**Does (b)'s 404 on a deep link count as "resolving"?** No. A browser renders it, so the letter of Phase 4 criterion 4 (renders in a headless browser, missing asset not 200 HTML) would pass. PUB-06 says history-style URLs *resolve*, and a valid page answering 404 is wrong for every client that reads the status: `curl -f`, link checkers, unfurlers, and this repo's own uptime probe pattern (any non-200 is a failure). Recommend (a). Choose (b) only if the user accepts "renders but answers 404" in writing.

**Final nginx conf (measured as `nginx-v2` and, with `root /srv/site`, in the built image):**
```nginx
server {
    listen 8080;
    server_name _;
    root /srv/site;
    index index.html;
    gzip on;
    gzip_types text/css application/javascript text/javascript application/json image/svg+xml;

    location = /healthz {
        access_log off;
        default_type text/plain;
        return 200 "ok\n";
    }
    location ^~ /assets/ {
        try_files $uri =404;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }
    location ~ \.[A-Za-z0-9]+$ {
        try_files $uri =404;
        add_header Cache-Control "no-cache" always;
    }
    location / {
        try_files $uri /index.html;
        add_header Cache-Control "no-cache" always;
    }
}
```
Built image (`FROM nginx-unprivileged@sha256:5d8bb...`, `COPY nginx.conf /etc/nginx/conf.d/default.conf`, `COPY out/ /srv/site/`): 10,232,945 bytes, `USER 101`, port 8080. Run with the pod flags above it served `/`, deep links and `/healthz` 200, `/assets/missing.js` and `/missing.png` 404, `/50x.html` 404 (the stock page is not in the web root), `/@fs/etc/hostname` = the shell. `root /srv/site` is deliberate: `COPY` into the stock web root would leave the image's own `50x.html` next to the site.

Limits of the measurement: one machine, one run per cell, no TLS (Traefik terminates it), no Traefik in the path, a `.` in a view id with no trailing slash would hit the extension rule and 404 (LikeC4 routes carry a trailing slash, and no current view id has a dot). SWS candidates were not tuned beyond the three TOML shapes above.

**Trade-off matrix (for the PLAN):**

| Approach | Pros / Cons | Why Picked |
|----------|-------------|-----------|
| (a) nginx-unprivileged, extensionless-only fallback | 200 deep links, real 404 for assets, no route list to maintain, 4 MiB anon. Alpine userland to patch, `emptyDir` for `/tmp`, no Dependabot docker ecosystem configured. | **Picked.** Only candidate that meets both halves of PUB-06 with correct statuses. |
| (b) SWS default `404.html` | Scratch image, trivial config. Deep links answer 404; `immutable` on 404s. | Fails "resolve". |
| (c) SWS + `rewrites` | 200 for the listed prefixes. `/embed`, `/export`, `/d2`, `/dot`, `/mmd`, `/puml`, `/projects` exist as routes in the bundle and 404; the list rots on a LikeC4 upgrade; `immutable` on 404 cannot be filtered in 2.44.1. | Fragile. |

Route list for reference [VERIFIED: bundle `main-*.js` path literals]: `/`, `/view/$viewId`, `/project/$projectId`, `/embed/$viewId`, `/export/$viewId`, `/d2`, `/dot`, `/mmd`, `/puml`, `/projects`, `/single-index`, `/webcomponent/$`; `trailingSlash:` is `always`.

## Open Question 2: layout and names (decided)

**Facts that decide it:**
- `deploy.yml` ignores only `docs/**`, `**/*.md`, `.planning/**`, `k8s/**` [VERIFIED: .github/workflows/deploy.yml:18-22, quote: `- "docs/**"` / `- "**/*.md"` / `- ".planning/**"` / `- "k8s/**"`]. A file under `infra/` (or `.github/**`, which it keeps on purpose, lines 13-14) is therefore not ignored.
- The Setters automation rewrites only under `./k8s/overlays` [VERIFIED: k8s/flux-system/image-automation.yaml:78-80, quote: `update:` / `path: ./k8s/overlays` / `strategy: Setters`].
- `workloads()` reads `const WORKLOAD_DIRS = ['k8s/base/', 'k8s/data/', 'k8s/platform/', 'k8s/monitoring/', 'k8s/flux-system/gotk-components.yaml']` [VERIFIED: docs/architecture/sync.mjs:236]. `k8s/overlays/` is not in it.
- The placement check follows an overlay's `namespace:` through `resources: ../../base/...` to the base file [VERIFIED: sync.mjs:345-390, read], so a base Deployment is placed in `architecture` by the overlay.
- Invariant I5's overlay half applies to any image whose name contains `kanban-board-backend` [VERIFIED: scripts/verify-k8s-invariants.py:538, quote: `if "kanban-board-backend" not in name:`]; the Hub repo name below contains it.
- The Flux Kustomization naming follows `apps-prod`, `apps-nonprod` [VERIFIED: k8s/flux-system/apps-prod.yaml:5].

| Approach | Pros / Cons | Why Picked |
|----------|-------------|-----------|
| STACK: all in `k8s/overlays/architecture/`, Dockerfile in `infra/architecture-site/` | Fewest files. Deployment unread by the workload check; `infra/` edits trigger the app pipeline. | Rejected on both counts. |
| ARCHITECTURE: `docs/architecture/Dockerfile`, `k8s/base/site/` + `k8s/overlays/site/`, Kustomization `site` | Base is workload-checked, setter under overlays. Name `site` says nothing; Dockerfile at the `docs/architecture/` root needs a `.dockerignore` for `node_modules`. | Layout kept, names and Dockerfile directory changed. |
| **Picked:** `docs/architecture/site/` (Dockerfile, nginx.conf, gitignored `out/`), `k8s/base/architecture-site/`, `k8s/overlays/architecture/`, `apps-architecture` | Build context is only those three entries, so no `.dockerignore`; `docs/**` is ignored by `deploy.yml`; names share one root (`architecture`) with the namespace, host, Hub repo and ImagePolicy. A base with one overlay costs two small files. | Meets criteria 4 and 5 with no change to `sync.mjs`, the automation or the gates. |

Names: namespace `architecture`; Deployment and Service `architecture-site` (workload key `Deployment/architecture-site`; the key carries no namespace, so it must not be `app`); Hub repo `rudenkovladimir/kanban-board-backend-architecture`; `ImageRepository` and `ImagePolicy` `kanban-board-backend-architecture`; Certificate `architecture`, secret `architecture-tls`; IngressRoutes `architecture-https` and `architecture-http`; Middleware `redirect-https` in namespace `architecture` (one per namespace, as in prod, nonprod and monitoring).

Put the measured `resources` directly in the base Deployment with the MEASURED comment: with a single overlay a patch file adds nothing, and I3 applies to the rendered overlay root while the base root is exempt from I3 and the rendered I5 check [VERIFIED: verify-k8s-invariants.py:475-477, `if not is_base_root:`].

`likec4.fish site` keeps writing `docs/architecture/out/site` (documented in `docs/LOCAL_DEV.md:139`); the workflow passes its own `-o site/out --use-dot=false`. Two copies of one flag are cheaper than rewriting the local wrapper. Verified command (run from the repo root, output ignored by git, then deleted): `pnpm --dir docs/architecture exec likec4 build --use-dot=false -o site/out model`.

## Open Question 3: operator actions and read-only checks

| Item | State on 2026-10-28 | Operator must | Read-only check |
|------|--------------------|---------------|-----------------|
| DuckDNS A record for `kanban-board-rud-vlad-473-architecture.duckdns.org` | `dig +short @8.8.8.8 ... A` = `159.195.114.230`, the VPS IPv4 [VERIFIED: dig; INFRA_RUNBOOK.md:20 lists the same address] | Nothing now. No updater runs, so if the VM IP ever changes the record is manual (INFRA_RUNBOOK.md:480). | `dig +short @8.8.8.8 <host> A` before the Certificate commit. |
| AAAA record | `dig +short @8.8.8.8 <host> AAAA` returned nothing | Add none (`KANBAN-INGRESS` is IPv4-only; DuckDNS cannot detect IPv6, per PITFALLS 10, not re-fetched). | same, AAAA |
| Hostname behaviour today | `curl -k https://<host>/` = 404, `http://<host>/` = 404, issuer `CN=TRAEFIK DEFAULT CERT` | — | Baseline for the "before" half of criterion 2. |
| Docker Hub repo `rudenkovladimir/kanban-board-backend-architecture` | `GET https://hub.docker.com/v2/repositories/rudenkovladimir/kanban-board-backend-architecture/` = **404** (an anonymous call returns 404 for both a missing and a private repo); the control `.../kanban-board-backend/` = 200 | Create it by hand, **Public**, in the `rudenkovladimir` namespace, before the first push. Docker documents that `docker push` to a missing repo creates it "with the default repository privacy for that namespace" [CITED: docs.docker.com/docker-hub/settings], and that default is not visible from here. | Same GET must return 200 after creation; `.../tags/?page_size=5` shows the first tag after the dispatch run. |
| `DOCKERHUB_TOKEN` | environment `production` has no protection rules and no branch policy; the secret is described as "production's copy of the account-wide token" [VERIFIED: deploy.yml:92-93] | Nothing. Whether a token may create a repository on push is unverified, which is why the repo is created by hand. | `gh api repos/RudVlad473/kanban-board-backend/environments` |
| Repo visibility | public [VERIFIED: gh api] | — | — |

Do not perform the DuckDNS or Docker Hub actions from the plan; they are `checkpoint:human-action` tasks.

## Open Question 8: HTTP-01 solver `serviceType`

**Decision: do it** (`serviceType: ClusterIP` under `http01.ingress` on both ClusterIssuers), as its own commit before the first Certificate, behind a user-confirmed `checkpoint:decision` because it edits a shared issuer.

- Today neither issuer sets it: `solvers:` / `- http01:` / `ingress:` / `ingressClassName: traefik` [VERIFIED: k8s/platform/edge/clusterissuers.yaml:16-19 and 30-33]. The live staging issuer shows `[{"http01":{"ingress":{"ingressClassName":"traefik"}}}]` [VERIFIED: ssh read-only].
- Unset means NodePort: "By default, type `NodePort` will be used when you don't set HTTP01 or when you set `serviceType` to an empty string. Normally there's no need to change this." [CITED: cert-manager.io/docs/configuration/acme/http01, via Context7 /cert-manager/website]. So every issuance and renewal creates a transient NodePort Service that invariant I1 (rendered manifests) cannot see.
- Live baseline before the change: the only non-ClusterIP Service in the cluster is `kube-system/traefik` (LoadBalancer) [VERIFIED: ssh `k3s kubectl get svc -A`, this session].
- Existing Certificates are not re-issued by an issuer edit: the FAQ's list of triggers names the Secret, the Certificate spec, the Secret's issuer annotation, renewal time and `cmctl renew`, not the ClusterIssuer's solver spec [CITED: cert-manager FAQ "When do certs get re-issued"]. That the edit is a no-op for the three live Certificates is an inference from that list ([ASSUMED] until observed; verify `k3s kubectl get certificate -A` is unchanged after the edit).
- Why before the first Certificate: the new staging issuance is the first use of a ClusterIP solver on this cluster, and staging absorbs the risk. The three production Certificates are 32 days old (`k3s kubectl get certificate -A`) and will renew through the same solver config at about two thirds of a 90-day lifetime ([ASSUMED] default renewal point), so the first renewals fall in a few weeks. The production issuer carries the same config, so the staging run is the only rehearsal.
- Verification: while the staging challenge is pending, `k3s kubectl get svc -n architecture` must show type ClusterIP, never NodePort; afterwards the cluster inventory still lists only `kube-system/traefik` as non-ClusterIP.

## Bootstrap order

Every push, PR, merge and `workflow_dispatch` below is an outward action and every VPS write is a cluster change. The global rules say never push and "ask before merging to main", so each is a `checkpoint:human-action` or an explicit authorization in the plan, not an executor default.

1. **Operator:** create the public Docker Hub repo; `dig @8.8.8.8` A (already green).
2. **Gate first:** add I12 to `scripts/verify-k8s-invariants.py` and its selftest cases, red on a planted line, then green (also put the gate in the PR-time path; it already runs on every push and PR because `invariant-checks.yml` matches `**`).
3. **Workflow + site dir commit** (`.github/workflows/architecture-site.yml`, `docs/architecture/site/*`): config-only, so `--no-verify` is permitted. It starts `deploy.yml` once (the app pipeline) because `.github/**` is not ignored; expect that run.
4. **PR-time proof (red, then green):** on a branch, a deliberately drifted model (for example rename a claimed workload in the model) opened as a PR fails the `build` job at the `sync.mjs check` step with `publish` skipped; the fixed model passes. This is the only way to show "red before the push step" without publishing from a branch.
5. **Merge to main and `workflow_dispatch`** (the user's merge gate applies): first tag `main-<run>-<sha7>` appears on Hub. Record the run number actually produced; do not assume 1.
6. **Ladder on the VPS** with that image (authorization checkpoint, below). The MEASURED comment values come from here.
7. **Solver commit:** `serviceType: ClusterIP` on both issuers; confirm the three live Certificates stay `Ready`.
8. **One atomic manifest + model commit** (feature-heavy, runs the 4-minute hook): `Namespace`, `k8s/base/architecture-site/`, `k8s/overlays/architecture/` (tag = the step 5 tag), Flux `apps-architecture`, `ImageRepository`/`ImagePolicy`, and the model element, `host` claim, namespace node, instance and `traefik ->` edge. They must be one commit: a base Deployment with no `workload` element, an IngressRoute host with no `host` claim, or a `Namespace` with no namespace node each fail `sync.mjs check` in the hook and in the workflow's own gate [VERIFIED: sync.mjs:599-603, 610-620, 262-271].
9. Observe Flux: Certificate `Ready` on staging, rollout, then run the verification recipes. The model push of step 8 itself triggers the site workflow (a second tag), which is the first natural Flux bump.
10. **SCEN-08 last**, from the files as built, with a `03-SCENARIO-EVIDENCE.md` in the Phase 2 format (file:line plus verbatim quote per step).

## Workflow design and path filters

```yaml
# Source of the shape: backup-image.yml (publish job) and invariant-checks.yml (build steps).
on:
  push:
    branches: [main]
    paths:
      - docs/architecture/model/**
      - docs/architecture/site/**
      - docs/architecture/package.json
      - docs/architecture/pnpm-lock.yaml
      - .github/workflows/architecture-site.yml
  pull_request:
    paths:              # the same five entries as the push list
      - docs/architecture/model/**
      - docs/architecture/site/**
      - docs/architecture/package.json
      - docs/architecture/pnpm-lock.yaml
      - .github/workflows/architecture-site.yml
  workflow_dispatch: {}
permissions:
  contents: read
```
Write the list out twice; do not rely on YAML anchors in a workflow file ([ASSUMED] unsupported, not tested here).

- "If at least one path matches a pattern in the `paths` filter, the workflow runs" [CITED: docs.github.com workflow-syntax]. A push of more than 1,000 commits always runs the workflow, and a diff over 3,000 files can miss a match; neither applies here. Whether `workflow_dispatch` is filtered is not stated by that page (dispatch has no paths).
- Flux bump commits touch only `k8s/overlays/*/kustomization.yaml` [VERIFIED: git log --author=fluxcdbot --stat -3]. They match no entry above. Never add `k8s/**` or `k8s/overlays/**`.
- A push that touches only `src/`: no listed path matches. The literal criterion needs a src-only push, which also starts the whole app pipeline. A docs-only or `docs/architecture/sync.mjs`-only push is the same filter mechanism at no cost; decide which the plan accepts. Passive evidence also works: the next real src-only push on `main` should show no site run in `gh run list --workflow architecture-site.yml --json headSha,event,createdAt`.
- `github.run_number` is per workflow and counts PR, dispatch and failed runs, so tags skip numbers but stay monotonic, which is all the `numerical` policy needs. Re-running an old run keeps its number.
- The publish job guard: `if: github.event_name != 'pull_request' && github.ref == 'refs/heads/main'`. A dispatch from a branch builds but does not publish, so a branch can never roll the cluster.
- Token handling as in `backup-image.yml`: `DOCKERHUB_TOKEN` through `env:` and stdin only, `timeout-minutes`, `linux/amd64` only.
- No `cleanup` job (deferred to V2-05).

## Memory-limit ladder (invariant I4)

I4 needs a `MEASURED` comment within 25 lines above each `memory:` line and CI runs it with `--no-provisional` [VERIFIED: invariant-checks.yml:124, quote: `run: python3 scripts/verify-k8s-invariants.py --no-provisional`; verify-k8s-invariants.py:502-518].

**Repo method** [VERIFIED: docs/INFRA_RUNBOOK.md:619-626]: halve from the starting value, require `RestartCount=0` plus a clean `dmesg` and `lastState` over a full workload cycle at each rung, adopt with headroom, re-verify from a fresh recreate. Traefik's row is the model: `256Mi✓ → 128Mi✓ → 64Mi✓ → 32Mi✓ → 16Mi✗ ... **64Mi/32Mi**` (runbook line 651), i.e. limit about twice the lowest passing rung.

**Where:** the VPS (4 vCPU and 7945 MiB, same CPU count as the measuring host, so nginx's four workers behave alike). It is a cluster write, so it needs a `checkpoint:human-action` authorization step. Precedent for the scratch namespace: "scratch namespace `ladder-scratch`" [VERIFIED: INFRA_RUNBOOK.md:633-634]. Do not use the real Deployment: each rung would be a commit and a Flux round trip.

**Plan:** pod in `ladder-scratch` with the exact securityContext and `/tmp` emptyDir of the real Deployment, image = the step 5 tag; rungs 64Mi → 32Mi → 16Mi → 8Mi limit; per rung drive from the VPS against the pod IP: every file of the site once, 200 gzip requests for `main-*.js`, 10 concurrent full cold loads, 20 s settle; read `RestartCount`, `lastState.terminated.reason`, `dmesg`, and anon+shmem from the cgroup (the repo already ruled page cache out as an OOM signal, runbook "anon+shmem" finding cited in STACK). Delete the pod and the namespace after. Starting hypothesis only: request 16Mi, limit 32Mi (local anon 4.0 MiB). Record the real figures; do not copy this hypothesis into the manifest.

Optional, cheap, read-only: Traefik is 64Mi/32Mi and had `restarts=2` before this phase [VERIFIED: ssh, this session]. Compare the restart count after the concurrent cold loads (OQ8 item, unmeasured today).

## Gates and invariants for the new objects

| Gate | Consequence | Status |
|---|---|---|
| I3 | `requests.memory <= limits.memory` on the container, rendered at the overlay root | write both in the base Deployment |
| I4 | MEASURED comment above both `memory:` lines (one comment covers both, within 25 lines) | ladder output |
| I5 | overlay `images[].newTag` fullmatch `^main-\d+-[0-9a-f]{7}$` and `SETTER_MARKER_RE = r'"\$imagepolicy":\s*"[^"]+:tag"'` anywhere in the file [VERIFIED: verify-k8s-invariants.py:115-116, 541-549] | red: delete the marker comment; green: restore it. A drill on the real overlay file, recorded like the earlier drills |
| I10 | the `web` route must fullmatch ``Host(`<host>`) && !PathPrefix(`/.well-known/acme-challenge/`)`` and name a `redirect-https` Middleware defined in the same namespace [VERIFIED: verify-k8s-invariants.py:301-304, 345-357] | copy `k8s/overlays/nonprod/ingressroute.yaml` with host, route names and service name changed |
| I11 | `apps-architecture` must set none of `targetNamespace`, `namePrefix`, `nameSuffix`, `patches`, `components`, `postBuild`, and `sourceRef` must be `GitRepository flux-system`; the Certificate, both routes, the Middleware live only in the overlay [VERIFIED: verify-k8s-invariants.py:365-372, 406-420] | live proof: `k3s kubectl get ingressroute -n architecture architecture-https -o jsonpath='{.metadata.labels}'` shows `kustomize.toolkit.fluxcd.io/name` = `apps-architecture` |
| I7 | new roots are discovered under `k8s/**` | nothing to register |
| kubeconform | validates `IngressRoute`, `Certificate`, `Middleware`, Flux kinds already in use | expected green; confirm on the first run |

### New invariant I12: no dev server from a manifest, Dockerfile or workflow
Add it to `verify-k8s-invariants.py` as a pure function `check_i12_no_dev_server(path, text)` plus selftest cases, not as a new script. Reasons: the file already hosts source-text gates (I4 scans hand-written YAML, I5-overlay parses text), its CI job runs on every push and PR (`invariant-checks.yml` matches `**`), and its selftest harness exists. Scope: `k8s/**/*.y?ml`, every `Dockerfile*`, `.github/workflows/*.yml`. Match (after dropping full-line comments and trailing ` #...`): `likec4\s+(start|preview|serve)`, `\bvite\s+preview\b`, and `--listen`. Today's tree has no hit [VERIFIED: rg of `.github/workflows`, `k8s`, `infra`, the app `Dockerfile` and `k8s/data/postgres/backup/Dockerfile` for these patterns]. The new workflow and Dockerfile must therefore state "never `likec4 start`" only in comments, which the gate strips; the docstring's scope line and the "Known holes" list need one line each. Red/green: plant `run: likec4 start` in a scratch copy of the workflow, expect `FAIL: I12`, remove it, expect exit 0. A `--listen` in `docs/architecture/likec4.fish` is out of scope (never deployed).

## Model changes SCEN-08 forces (read from sync.mjs and the model)

- Element for the site, `workload 'Deployment/architecture-site'` and a `source` that is a **single string** naming a file that exists in the drill scratch tree. `check` runs `join(root, src)` on `getMetadata('source')` [VERIFIED: sync.mjs:606-607, quote: `const src = e.getMetadata('source')` / `if (src && !existsSync(join(root, src)))`]; an array throws `ERR_INVALID_ARG_TYPE` (verified with `node -e`). Use `k8s/base/architecture-site/architecture-site.yaml`. The drills copy `k8s`, `.github/workflows` and `infra/vm` only [VERIFIED: drill.fish:14-30 `fresh()`], so a `source` of `docs/architecture/site/Dockerfile` would fail every drill at the baseline.
- Namespace node `architecture = namespace 'architecture' { instanceOf <site> }` under `netcup.k3s` in `deployment.c4`, next to the existing nodes [VERIFIED: deployment.c4:9-35]. Its title must equal a name the manifests define (`namespaces.yaml` will).
- Host: add `'kanban-board-rud-vlad-473-architecture.duckdns.org'` to Traefik's `host` list [VERIFIED: model.c4:10-14]; the check is two-way set equality with every `Host(...)` literal in any IngressRoute under `k8s/` [VERIFIED: sync.mjs:249-271].
- Edge: `traefik -[sync]-> <site> 'HTTP :8080'`. One logical relationship per pair keeps the deployment edge title unmerged; a second one with a different title produces `[...]` and fails `checkEdgeLabels` [VERIFIED: sync.mjs:457-465].
- View membership: include `netcup.k3s.architecture.**` in `production` and the site instance in `delivery` (for `flux -> site`). Every instance must appear in some deployment view or `check` fails [VERIFIED: sync.mjs:443-451]. Record each changed view's natural width in the evidence (OQ7 default).
- Scenario view `publish-the-site`, `#scenarios`, `variant sequence`, at most 5 participants [VERIFIED: sync.mjs:469-475, `> 5`]: `repo`, `ci`, `dockerhub`, `flux`, the site element is exactly 5. Dynamic steps are not required to be relationships, so `ci -> dockerhub 'build and push main-<run>-<sha7>'` as a step needs no new logical relationship; do not add one.
- Reword, in the same change, the `ci` description ("changes under k8s/ and the docs do not start it" is true of `deploy.yml` only) and the `dockerhub` description (it names "the app image repositories") [VERIFIED: delivery.c4, read this session]. Both would otherwise state something false about the second workflow.
- Update `docs/DIAGRAM_CONVENTIONS.md` Scenarios row (it lists the ten Phase 2 views) and the Physical row's workload/namespace coverage text, `docs/INFRA_ARCHITECTURE.md` (Maintenance Note lists the roots, the workflows and the namespace snapshot "34 containers across 7 namespaces"; a new `k8s/` root and an eighth namespace make it stale), and the runbook's DNS section (fourth hostname). The memory note "infra diagrams go stale silently" is the reason.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Tag bump and rollout | a CI deploy step or SSH push | `ImageRepository` + `ImagePolicy` + `ImageUpdateAutomation` Setters | Already deployed and gated by I5. |
| History fallback | a Node server or SPA proxy | nginx `try_files` with an extension rule | Measured. |
| TLS | in-pod TLS or Caddy | Traefik + cert-manager | Existing edge. |
| Compression | a Traefik `compress` Middleware | nginx `gzip` | Avoid double encoding and extra Traefik memory. |
| Dev-server prevention | a code review habit | I12 | Fails closed in CI. |
| Memory sizing | a guess | the ladder | I4 refuses PROVISIONAL in CI. |

## Common Pitfalls

### Pitfall 1: a file outside `docs/**`, `k8s/**` starts the app pipeline
**What goes wrong:** `infra/**` and `.github/**` edits run tests, push prod and nonprod images and open an SSH forward to Postgres. **Why:** `deploy.yml` uses `paths-ignore` with four entries only. **Avoid:** keep the Dockerfile and `nginx.conf` under `docs/architecture/site/`. Accept that creating or editing `.github/workflows/architecture-site.yml` starts one app pipeline run, exactly as `backup-image.yml` edits already do. Changing `deploy.yml`'s ignore list to spare that is a decision for the user, not a default.

### Pitfall 2: the atomic commit
Base Deployment, IngressRoute, Namespace and model facts each have a two-way check (see Bootstrap step 8). Splitting them makes the hook and the workflow's own `sync.mjs check` red.

### Pitfall 3: `source` metadata
Single string only; path must exist in the drill scratch tree. See the model section.

### Pitfall 4: PR job that names an environment
With no protection rules on a public repo, a same-repo PR run naming `production` can read `DOCKERHUB_TOKEN`. Keep `environment` on the publish job only.

### Pitfall 5: nginx `add_header` inheritance
A location-level `add_header` replaces the server-level ones, and without `always` it applies to 2xx/3xx only. The measured conf sets headers per location on purpose. If a CSP or `nosniff` is added later, repeat it in each location or `include` one snippet.

### Pitfall 6: `likec4 build` in a container
Fails without `--use-dot=false` (measured). Keep the build on the runner and pass the flag.

### Pitfall 7: `wait: true` hides `healthChecks`
Flux ignores `.spec.healthChecks` when `.spec.wait` is true [CITED: fluxcd.io kustomizations docs]. `apps-nonprod.yaml` sets both. Use `wait: true` alone for `apps-architecture`; the Certificate then counts toward readiness, so the Kustomization stays not-Ready until the staging cert issues. Nothing depends on it, so that is acceptable.

### Pitfall 8: stale counts in docs
An eighth namespace, a fourth hostname and a second delivery workflow make three docs sentences false. See the model section.

### Pitfall 9: comment lint on new files
`Phase 3`, `PUB-02`-style and plan ids in comments of the workflow, Dockerfile, `nginx.conf` or YAML fail the pre-commit hook and CI `comment-lint`. Write rationale in plain words.

### Pitfall 10: the exposure window
From the first staging rollout to the Phase 4 review the unreviewed model is reachable by anyone who clicks through the TLS warning and guesses the DuckDNS name. The ROADMAP default keeps this order (Open Question 4). Mitigations already true: `robots.txt` is `Disallow: /`, nothing links to the host, the repo is public. Whether Let's Encrypt staging certificates reach public CT logs is [ASSUMED] not to be the case; do not state it in docs.

### Pitfall 11: Docker Hub repo missing or private
Flux's `ImageRepository` scans anonymously. A private or absent repo leaves the `ImagePolicy` unresolved and the setter untouched; the first symptom is a not-ready `ImageRepository`, not a failed workflow.

### Pitfall 12: I5 is a file-wide regex
`SETTER_MARKER_RE.search(text)` looks anywhere in the file. The red drill must delete the marker comment itself, and any prose that happens to contain a marker-shaped string would hide the break.

## Code Examples

### Dockerfile (built and run this session)
```dockerfile
FROM nginxinc/nginx-unprivileged:1.30.6-alpine-slim@sha256:5d8bb9437d74a600ec5dbf90c035451c724dc33eac8d3a71078f6f8612c345f6
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY out/ /srv/site/
```

### Deployment hardening (copy of `base/app` plus the nginx needs)
```yaml
# Source of the settings: k8s/base/app/app.yaml (read this session) and docker run flags that worked.
spec:
  replicas: 1
  strategy: { type: Recreate }
  template:
    spec:
      automountServiceAccountToken: false
      securityContext: { seccompProfile: { type: RuntimeDefault }, runAsNonRoot: true, runAsUser: 101 }
      containers:
        - name: architecture-site
          image: rudenkovladimir/kanban-board-backend-architecture   # untagged in the base, as the app
          ports: [{ name: http, containerPort: 8080 }]
          securityContext: { allowPrivilegeEscalation: false, readOnlyRootFilesystem: true, capabilities: { drop: [ALL] } }
          volumeMounts: [{ name: tmp, mountPath: /tmp }]
          readinessProbe: { httpGet: { path: /healthz, port: http } }
          livenessProbe: { httpGet: { path: /healthz, port: http } }
          resources:   # MEASURED comment from the ladder goes directly above
            requests: { memory: <ladder> }
            limits: { memory: <ladder> }
      volumes: [{ name: tmp, emptyDir: {} }]
```

### Overlay kustomization (shape of `k8s/overlays/nonprod/kustomization.yaml:18-23`)
```yaml
namespace: architecture
resources: [../../base/architecture-site, ingressroute.yaml, certificate.yaml]
images:
  - name: rudenkovladimir/kanban-board-backend-architecture
    newName: rudenkovladimir/kanban-board-backend-architecture
    # Flux's ImagePolicy flux-system/kanban-board-backend-architecture rewrites this value in place.
    newTag: main-<run>-<sha7> # {"$imagepolicy": "flux-system:kanban-board-backend-architecture:tag"}
```
The nonprod original reads `newTag: main-158-80831b3 # {"$imagepolicy": "flux-system:kanban-board-backend-nonprod:tag"}` [VERIFIED: nonprod/kustomization.yaml:23].

### Certificate on staging (changes only `issuerRef.name` from `k8s/overlays/nonprod/certificate.yaml:2-12`)
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata: { name: architecture }
spec:
  secretName: architecture-tls
  dnsNames: [kanban-board-rud-vlad-473-architecture.duckdns.org]
  issuerRef: { name: letsencrypt-staging, kind: ClusterIssuer }
```
The staging issuer exists and is Ready [VERIFIED: ssh `k3s kubectl get clusterissuer`; clusterissuers.yaml:10]. The file's header already says every Certificate starts on staging (clusterissuers.yaml:1-2), yet all three live Certificates are on production.

### Flux wiring
```yaml
# k8s/flux-system/apps-architecture.yaml (shape of apps-nonprod.yaml:9-30, without the ignored healthChecks)
spec:
  interval: 10m
  path: ./k8s/overlays/architecture
  prune: true
  wait: true
  suspend: false
  dependsOn: [{ name: platform }, { name: edge }]
  sourceRef: { kind: GitRepository, name: flux-system }
# image-automation.yaml additions, same shape as the nonprod pair (lines 14-20, 37-50):
#   ImageRepository kanban-board-backend-architecture: image docker.io/rudenkovladimir/kanban-board-backend-architecture, interval: 5m
#   ImagePolicy kanban-board-backend-architecture: filterTags pattern '^main-(?P<num>[0-9]+)-[a-f0-9]{7}$', extract: $num, numerical asc
```
`update.path: ./k8s/overlays` needs no change. The Kustomization list is `k8s/flux-system/kustomization.yaml:3-14`; add `apps-architecture.yaml` there. The Flux source polls every minute (`interval: 1m`, gotk-sync.yaml:10), the scans and automation every 5 minutes. A push-to-rollout upper bound of about 12 minutes is arithmetic, not an observation; time one real run and record it.

## Success-Criterion Verification Recipes

(Validation Architecture section omitted: `workflow.nyquist_validation` is `false` in `.planning/config.json`.)

| Criterion | Command / observation | Verified here |
|---|---|---|
| 1 model-only push runs, bump and src-only do not | `gh run list --workflow architecture-site.yml --json headSha,event,createdAt,conclusion` against `git log --author=fluxcdbot` shas | filter reasoning; paths verified |
| 1 red then green | PR with a drifted model: job `build` fails at the check step, `publish` skipped (`gh run view <id> --json jobs`); fixed model passes | design |
| 1 PR build has no secrets | the `build` job has no `environment`; job log shows no `DOCKERHUB_TOKEN` use | design |
| 2 staging issuer | `H=<host>; openssl s_client -connect $H:443 -servername $H </dev/null 2>/dev/null \| openssl x509 -noout -issuer` must contain `(STAGING)`; today it prints `issuer=CN=TRAEFIK DEFAULT CERT` | command run on the live host |
| 2 Certificate | `k3s kubectl get certificate -n architecture` Ready, `issuerRef` `letsencrypt-staging` | ssh read-only |
| 2 marker | `curl -sk https://<host>/` is 200 and its body contains `id="likec4-root"` (pipe to `rg -c 'id="likec4-root"'`) | marker exists in the build |
| 2 others unchanged | baseline 2026-10-28: prod `/api/healthz` 200, nonprod `/api/healthz` 200, monitoring `/` 302 | measured |
| 3 no file read | `curl -k https://<host>/@fs/etc/hostname` must not equal `/etc/hostname`; expect the shell | measured on the image |
| 3 deep link | `shot-scraper javascript --browser-arg=--ignore-certificate-errors -i render.js https://<host>/view/signin-ok/`: 38 SVGs on the local build; without the flag the staging cert fails the load | flag verified against the live Traefik default cert (load fails without it, returns `404 page not found` with it) |
| 3 missing asset | `curl -skI https://<host>/assets/missing.js` = 404, content-type `text/html` | measured locally |
| 3 I12 | planted line red, removed green | design |
| 4 invariants | `python3 scripts/verify-k8s-invariants.py --no-provisional` and the selftest | existing commands |
| 4 I11 live | see the table above | design |
| 5 model | `node docs/architecture/sync.mjs check` exit 0 and the drills still caught | existing |

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| STACK: SWS with `page-fallback` (200 for every miss) | nginx extension rule | A missing asset is a real 404. |
| STACK/ARCHITECTURE: `infra/architecture-site/` or `docs/architecture/Dockerfile` | `docs/architecture/site/` | No app pipeline trigger, no `node_modules` in the context. |
| Production-first Certificate in STACK | staging first (ClusterIssuer already exists) | Matches PUB-03 and the file's own header. |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `actions/download-artifact@v4` is the right pair for `upload-artifact@v4` and tag-trust is acceptable in a job without secrets | Standard Stack | Wrong major would fail the workflow; the publish job's actions are SHA-pinned either way |
| A2 | An edit to a ClusterIssuer's solver spec does not re-issue existing Certificates | OQ8 | Three live certificates re-issue at once; check `Ready` after the edit |
| A3 | Staging certificates do not appear in public CT logs | Pitfall 10 | Hostname is discoverable earlier than assumed (it already resolves publicly) |
| A4 | A ClusterIP solver Service is reachable by Traefik's Ingress provider on this k3s | OQ8 | Staging issuance stalls; no production rate-limit cost |
| A5 | The Docker Hub namespace default privacy is unknown, so the repo is created by hand | OQ3 | A private repo leaves Flux unable to scan |
| A6 | Push-to-rollout is about 12 minutes at most | Flux wiring | Expectation only; measure once |
| A7 | VPS memory floor is near the local anon figures | Ladder | The ladder replaces it |
| A8 | Dependabot not tracking the nginx base image is acceptable debt (the backup image has the same) | OQ1 | Unpatched base image; add a `docker` ecosystem entry later |
| A9 | LikeC4 upgrades may add routes; the extension rule needs no change | OQ1 | An extensionful route would 404 |

## Open Questions

1. **Does the user accept a src-only push as the criterion 1 negative control, or a docs-only proxy?**
   - Known: both use the same filter mechanism; a real src-only push also runs the app pipeline.
   - Recommendation: use passive evidence from the next natural src-only push, plus a docs/`sync.mjs`-only push now.
2. **Wait for the user's choice on the shared-issuer edit (OQ8).** Recommendation: yes.
3. **Build stamp (OQ8 list).** Not folded in. The image tag already carries the sha7; revisit in Phase 4.
4. **Traefik under concurrent cold loads.** Optional read-only before/after restart count during the ladder; baseline `restarts=2`.
5. **`--build-webcomponent=false`** (drops 2.7 MB). Untested; leave the default.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (local) | image build and probe | yes | 29.1.3 | CI builds it |
| Node / pnpm | `sync.mjs`, likec4 | yes | 24.19.0 / 11.20.0 | — |
| fish, dig, openssl, curl, gh | recipes | yes | — | — |
| `shot-scraper` + Chromium | headless deep-link render | yes | 1.12 | Playwright |
| SSH to `netcup-prod` | read-only live checks, ladder | yes (BatchMode worked) | 4 vCPU, 7945 MiB | — |
| Docker Hub repo (public) | first push, Flux scan | **no** (404) | — | operator creates it |
| DuckDNS A record | HTTP-01 | yes | `159.195.114.230` | — |
| `letsencrypt-staging` ClusterIssuer | PUB-03 first half | yes (Ready, 32 d) | — | — |
| Docker on the VPS | ladder | not checked | — | `k3s kubectl` scratch pod (planned) |

**Missing dependencies with no fallback:** the public Docker Hub repo (operator).

## Security Domain

### Applicable ASVS Categories (level 1, `security_enforcement` true)

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture | yes | Static output only; I12 gate; no dev server |
| V5 Input Validation | no (no user input; static files) | — |
| V9 Communications | yes | Traefik TLS, `redirect-https`, staging then production issuer |
| V10 Malicious Code / supply chain | yes | image pinned by digest, publish-job actions pinned by SHA, frozen lockfile |
| V14 Configuration | yes | non-root, read-only rootfs, drop ALL, seccomp RuntimeDefault, no service-account token |
| V2/V3/V4 | no | public read-only site, no sessions |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| `likec4 start` exposed (arbitrary file read, Host rebinding) | Information disclosure | build output only; I12 |
| Secret reachable from a PR build | Elevation | `environment` only on publish; artifact hand-off |
| Transient NodePort from the ACME solver | Information disclosure / Tampering | `serviceType: ClusterIP` |
| Unreviewed model public on staging | Information disclosure | accepted per ROADMAP; Phase 4 leak scan before the flip |
| Wrong-tag or poisoned base image | Tampering | digest pin, publish-job SHA pins |
| `immutable` on a 404 | Tampering (cache) | no `always` on the assets location |
| Edge exhaustion (Traefik 64Mi) | Denial of service | optional restart-count check; compression in nginx |

## Sources

### Primary (HIGH)
- Local runs 2026-10-28: `likec4 build`, four server containers, a built image, headless Chromium, container build with and without `--use-dot=false`, `dig`, `curl`, `openssl`, `gh api` (repo visibility, environments), read-only `ssh netcup-prod` (`k3s kubectl get ns/clusterissuer/certificate/svc/pod`), `git log --author=fluxcdbot --stat`.
- Repo files read in full or in range: `.github/workflows/{deploy,backup-image,invariant-checks}.yml`, `k8s/flux-system/{image-automation,apps-prod,apps-nonprod,kustomization,edge,gotk-sync,platform,data,monitoring,cert-manager}.yaml`, `k8s/overlays/{prod,nonprod}/*`, `k8s/platform/{namespaces,edge/*,kustomization}.yaml`, `k8s/base/app/app.yaml`, `scripts/verify-k8s-invariants.py`, `scripts/verify-k8s-invariants-selftest.py` (outline), `scripts/verify-comments.py` (scope), `docs/architecture/{sync.mjs,drill.fish,likec4.fish,package.json,model/*.c4}`, `.githooks/pre-commit`, `docs/INFRA_RUNBOOK.md` (access, DNS, ladder), `docs/INFRA_ARCHITECTURE.md` (delivery, maintenance), `.planning/.../02-SCENARIO-EVIDENCE.md` (format).
- Docker Hub API (nginx-unprivileged metadata and tags), `joseluisq/static-web-server:2.44.1 --help` (`--page404` default `./404.html`).
- Context7 `/cert-manager/website` (HTTP-01 `serviceType`, re-issue triggers), `/websites/fluxcd_io_flux` (`wait` ignores `healthChecks`, setter marker format).

### Secondary (MEDIUM)
- docs.docker.com/docker-hub/settings (default privacy for repos created by `docker push`) and docs.github.com workflow-syntax (path filter semantics), both through a summarising fetch.

### Tertiary (LOW)
- Anything tagged [ASSUMED] above.

**Not verified:** the VPS memory floor; Flux end-to-end latency; behaviour of the real overlay under Flux (nothing was applied); Docker Hub namespace default privacy; whether the `DOCKERHUB_TOKEN` can create a repo; interactive CSP behaviour; the site under Traefik with TLS. The `research-plan`/`research-store` cache seam was not used (questions were answered by measurement and repo reads).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - versions, digests and the built image were checked this session.
- Architecture: HIGH for layout and gates (read from the checking code); MEDIUM for the Flux round trip (not exercised).
- Pitfalls: HIGH for the measured and read ones; MEDIUM for exposure and CT assumptions.

**Research date:** 2026-10-28
**Valid until:** 2026-11-11 (fast-moving: LikeC4 releases, nginx tags, the model changes under Phase 3 itself)
