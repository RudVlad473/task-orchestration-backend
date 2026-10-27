# Technology Stack: v1.5 Architecture Views (static LikeC4 site on k3s)

**Project:** kanban-board-backend, milestone v1.5 "Architecture Views"
**Researched:** 2026-10-27
**Scope:** only what is NEW: build the LikeC4 model into a static site, package it, build it in CI, serve it from k3s behind Traefik. The model, the drift gate and the first views are not re-researched.
**Confidence:** HIGH for the build output, the server behaviour and the repo's own gates (all run or read this session); MEDIUM for Docker Hub and Flux behaviour not exercised here (marked UNVERIFIED where so).

## Verdict

Add exactly one runtime component and one CI workflow:

1. A **scratch-based `joseluisq/static-web-server:2.44.1` image** that carries the `likec4 build` output baked in at `/public`, run non-root and read-only.
2. A new Docker Hub repository **`rudenkovladimir/kanban-board-backend-architecture`**, tagged with the same `main-<run_number>-<sha7>` scheme the app uses.
3. A new workflow **`architecture-site.yml`**, path-filtered to the model, that runs the drift gate, builds the site with the already-pinned LikeC4, builds and pushes the image.
4. A new Flux wiring that copies the existing app pattern one-for-one: `ImageRepository` + `ImagePolicy` + a `Kustomization` over a new overlay carrying the `$imagepolicy` setter marker, plus `Certificate` + two `IngressRoute`s + `Middleware`s on the existing `letsencrypt-production` ClusterIssuer.

No new tool is needed at the edge: Traefik, cert-manager, Flux image automation and Docker Hub already do everything else.

## Recommended Stack

### Build (all already in the repo, reused)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| likec4 | 1.60.0 (already pinned; npm `latest` on 2026-10-27, published 2026-10-15) | `likec4 build` produces the static site | Same binary the drift gate and `likec4.fish site` use, so the published site is built from the toolchain the model was verified with. Do not use `likec4/actions@v1`: its sample takes `likec4-version: latest`, and I did not verify it can honour the lockfile. |
| pnpm | 11.20.0 (`packageManager` field) via `pnpm/action-setup@v6` | install from lockfile | Already how the `likec4-model-drift` job installs; copy its steps verbatim. |
| Node | 24.19.0 (already pinned in `invariant-checks.yml`) | runs LikeC4 and `sync.mjs` | Same pin as the gate. |
| Graphviz | none | layout | `likec4 build` uses a bundled WASM layout by default (`--use-dot` defaults false); my local build ran without a `dot` binary. Nothing to install on the runner. |

### Serving

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| static-web-server (SWS) | 2.44.1 (released 2026-10-07; Hub tag digest `sha256:94618e3d17802265a21926217e6f9e7832b311b6fbf5933684001739fafed06a` as reported by the Hub API, re-check it at pin time) | static file server, SPA fallback, health endpoint | Scratch image, 3.75 MB compressed (amd64), a single binary, no shell, no package manager. Runs as `65532:65532` with `--read-only --cap-drop ALL` (tested). Configured by one TOML file. See the comparison below. |
| Docker Hub repo `rudenkovladimir/kanban-board-backend-architecture` | n/a | image registry | Matches how both env repos are done (`rudenkovladimir/kanban-board-backend`, `...-nonprod`), uses the same `DOCKERHUB_TOKEN` secret and the same Flux scan. The name contains `kanban-board-backend` on purpose: invariant I5's overlay check keys on that substring, so the new overlay is forced into the `main-<n>-<sha7>` tag and setter-marker discipline for free. |
| Flux image automation | existing | picks up new tags | Add one `ImageRepository` and one `ImagePolicy` to `k8s/flux-system/image-automation.yaml`, with the same `filterTags` pattern `^main-(?P<num>[0-9]+)-[a-f0-9]{7}$`, `numerical asc`. |
| Traefik / cert-manager | existing | edge and TLS | New `Certificate` for `kanban-board-rud-vlad-473-architecture.duckdns.org` on `ClusterIssuer letsencrypt-production`; DNS already resolves to 159.195.114.230 (checked with `dig` against 8.8.8.8 today). |

### CI actions (all already used in this repo)

| Action | Version | Use |
|--------|---------|-----|
| `actions/checkout` | v5 | as everywhere |
| `pnpm/action-setup` | v6 | as in `likec4-model-drift` |
| `actions/setup-node` | v5, node 24.19.0 | as in `likec4-model-drift` |
| `docker/setup-buildx-action` | v3 | as in `deploy.yml` |
| `docker/build-push-action` | v6, `platforms: linux/amd64` | the VPS is x86_64; same single-platform rule as `deploy.yml` |

## `likec4 build`: what it emits and how it behaves (measured)

Built `docs/architecture/model` with 1.60.0 into a scratch dir, twice, then removed it.

- **Output:** 6.0 MB, `index.html`, `404.html`, `robots.txt`, `favicon.ico`, `likec4-views.js` (2.7 MB web component) and `assets/` (hashed JS and CSS, biggest `main-*.js` 1.24 MB). Build time about 1.5 s.
- **Base path:** default is `/`, and `index.html` references `/assets/...` absolutely. The docs say the site is "strictly bound" to its base. The new hostname serves it at the root, so build with **no `--base` flag**. `--base "./"` gives a relocatable site and is not needed.
- **Routing:** path routes of the form `/view/<viewId>/` and `/project/<id>/view/<viewId>/`. A deep link therefore needs a server-side fallback to `index.html`. `404.html` is byte-identical to `index.html` (the build emits it for GitHub Pages style hosts). `--use-hash-history` makes routes `/#/view/...` and removes the need for any fallback.
- **Self-contained:** no `https://` reference in `index.html` or the fonts CSS, so no third-party font or CDN request at runtime. A CSP can be strict about origins.
- **Not byte-reproducible:** two builds of the identical model gave different hashes (`main-B4rIxc-O.js` vs `main-BTvpgv24.js`, also `index-*`, `drawio-*`, `ProjectsOverview-*`). Cause not investigated (UNVERIFIED). Consequences: never rebuild on a schedule or on unrelated pushes, and never diff builds to detect a change. Trigger on source paths only.
- **`robots.txt` is `Disallow: /`**, emitted by LikeC4. The site will be public but ask crawlers to stay away. Whether that is wanted is a user decision (see Open decisions). Whether a file in `--public` overrides it is UNVERIFIED.
- **`--build-webcomponent` defaults true** and accounts for 2.7 of the 6.0 MB (`likec4-views.js`). It is only for embedding views into other pages. `--build-webcomponent=false` should drop it; I did not test that the site still works without it (UNVERIFIED), so verify in the phase before relying on it.

### Serving behaviour verified against the real build (SWS 2.44.1)

Run in Docker as `--user 65532:65532 --read-only --cap-drop ALL --security-opt no-new-privileges --memory 16m`, site mounted read-only at `/public`:

| Check | Result |
|-------|--------|
| `/`, `/view/data/` (deep link), `/no/such/page` | 200 `text/html` with `page-fallback = "/public/index.html"` |
| Headless browser (shot-scraper) on `/view/data/` | renders the Logical view: 43 SVG nodes and the table names (`user_id`, `activity_log`, `spring_session_attributes`) |
| `/assets/<hash>.js` | 200 `text/javascript`, gzip on the fly |
| `/health` | 200, no access-log line (with `health = true`) |
| TOML `[[advanced.headers]]` | `Cache-Control: no-cache` on everything, `public, max-age=31536000, immutable` on `/assets/**` (later entry wins), plus `nosniff` and `Referrer-Policy` |
| Memory | `RssAnon` 6.6 MB idle, 16.3 MB after serving every file once plus 200 extra requests for the 1.2 MB `main.js` (on-the-fly gzip). `docker stats` showed 15.4 of 16 MiB, but that figure includes page cache, which this repo already ruled out as an OOM signal (INFRA_RUNBOOK anon+shmem finding). |

Two traps found:

1. **A missing asset returns 200 HTML with the immutable header.** With a fallback page, `/assets/missing.js` answers 200 `text/html` and inherits `max-age=31536000, immutable`. SWS 2.44.1 rejects a `status = [...]` filter on a header rule ("unused configuration manifest key ... or unsupported"), although the master-branch docs show it. So Context7's SWS docs describe a newer tree than 2.44.1: **test every TOML key against the pinned tag**. Effect: a tab opened before a deploy that lazily loads a now-deleted chunk gets HTML and stays broken until reload. A fresh load is unaffected because `index.html` is `no-cache`. Acceptable for a docs site; hash history removes the whole class.
2. **Default `SERVER_CACHE_CONTROL_HEADERS=true` gives HTML `max-age=86400`.** After a deploy, a returning visitor holds an `index.html` pointing at deleted hashed assets for up to a day. Hence the explicit `no-cache` on HTML in the TOML rather than the env-var shortcut.

## Static server choice

| Candidate | Version | Size (amd64, compressed, Hub API) | Non-root | SPA fallback | Verdict |
|-----------|---------|-----------------------------------|----------|--------------|---------|
| **joseluisq/static-web-server (scratch)** | 2.44.1 | 3.75 MB | yes, tested as 65532 with a read-only rootfs and no caps | `page-fallback` returns 200 | **Use.** Smallest surface: no shell, no `apk`, nothing writable needed, port 8080 by config, probes need no exec. Weakness: single-maintainer project (a risk of slower fixes, not measured), and `trivy image` returned zero findings with no result targets at all, which is a blank scan of a static Rust binary, not evidence of zero CVEs. |
| nginxinc/nginx-unprivileged `1.30.6-alpine-slim` (stable) | 1.30.6 (mainline 1.31.7) | 8.2 MB | yes, uid 101, port 8080 | `try_files` | **Runner-up.** Choose it if you prefer the most widely understood server or want `location` rules that give a real 404 for `/assets/*` misses. Costs: needs `emptyDir` mounts for `/tmp`, `/var/cache/nginx` and `/var/run` under a read-only rootfs, Alpine userland to patch, an `nginx.conf` ConfigMap. Lags the official `nginx` image (official 1.31.8 / 1.30.6 slim were published 2026-10-24, unprivileged 1.31.7 on 2026-10-19). |
| caddy | 2.11.7-alpine | 24.9 MB | possible with `setcap` care | `try_files` | Not recommended: 6x the size for features (auto-TLS, reverse proxy) this pod must not use, because Traefik owns TLS. |
| busybox httpd / lipanski static-website | n/a | about 1 MB | yes | error-page only, 404 status | Not recommended: no compression, no header control, so no `no-cache`/`immutable` split. |

I did not test nginx-unprivileged or Caddy here; their rows come from the Hub API and general knowledge (UNVERIFIED beyond sizes).

## Packaging

- **Build the site on the runner, then `COPY` it in.** Do not run pnpm and LikeC4 inside `docker build`: it would duplicate the toolchain pin and the lockfile install that the drift job already has.
- Dockerfile shape: `FROM joseluisq/static-web-server:2.44.1@sha256:<digest>`, `COPY site /public`, `COPY sws.toml /etc/sws.toml`, `ENTRYPOINT ["/static-web-server", "--config-file", "/etc/sws.toml"]`. Set the user in the pod spec, not the Dockerfile: `securityContext.runAsUser: 65532`, `runAsNonRoot: true`, `readOnlyRootFilesystem: true`. I tested these through `docker run --user 65532:65532 --read-only`, not through a Kubernetes pod, and the Dockerfile itself was not built here. Put the Dockerfile and `sws.toml` in a new directory such as `infra/architecture-site/` (not under `docs/architecture/`, whose changes the pre-commit hook gates).
- SWS config (`sws.toml`) keys verified on 2.44.1: `host`, `port = 8080`, `root = "/public"`, `page-fallback = "/public/index.html"`, `health = true`, `compression = true`, `cache-control-headers = false`, `log-level`, and `[[advanced.headers]]` with `source` and `headers`. `security-headers = true` only matters with TLS and HTTP/2 inside the server, which this pod does not do, so set response headers through `[[advanced.headers]]` instead.
- Tag: `main-${{ github.run_number }}-${GITHUB_SHA::7}`, exactly as `deploy.yml` computes it. The run number is the NEW workflow's own counter, which is fine: the new `ImagePolicy` reads only the new repository.

## CI: how a rebuild is triggered

A new `.github/workflows/architecture-site.yml`:

- **Trigger:** `push` to `main` with `paths:` limited to `docs/architecture/model/**`, `docs/architecture/package.json`, `docs/architecture/pnpm-lock.yaml`, `infra/architecture-site/**` and the workflow file itself, plus `workflow_dispatch`. This works because `generated.c4` is committed and the drift gate forces it up to date in the same commit as a migration, a `k8s/base` change or a Java import change, so every code change that alters the model shows up as a `docs/architecture/model/**` diff.
- **No loop, no overlap:** `deploy.yml` already ignores `docs/**` and `k8s/**`, so the two workflows cannot trigger each other. Flux's tag-bump commits touch only `k8s/overlays/**`, which the new filter does not list. Do not add `k8s/**` to the new filter.
- **Order inside the job:** install from the frozen lockfile, run `node docs/architecture/sync.mjs check` (publish nothing the drift gate would reject), `pnpm exec likec4 build -o <ctx>/site docs/architecture/model`, then log in, build, push. Use `environment: production` for `DOCKERHUB_TOKEN`, as `build-and-push-docker-image` and the cleanup jobs do.
- **Permissions:** `contents: read` only; the job holds a Docker Hub secret, so pin nothing less than the same tag-trusted actions the repo already accepts for `docker/*`.
- **Retention:** copy `cleanup-old-images-nonprod` with the repository axis changed (keep 5 newest `main-<n>-<sha7>` tags), as the nonprod repo did. This is the third copy of about 45 lines of shell; extracting it is out of scope here. The image is about 4 MB plus the site, so skipping retention is cheap; Docker Hub inactivity expiry for free repositories is UNVERIFIED, so do not rely on old tags staying forever.
- **Repository creation:** Docker Hub creates a missing repository on first `docker push` with the namespace's default privacy (Docker docs, docs.docker.com/docker-hub/settings). The existing repos are public, so check the default is Public before the first push, or create the repository by hand once. Flux scans anonymously, so the repository must be public.
- **Optional:** add `docker` to `.github/dependabot.yml` for the new directory so the SWS tag bumps by PR. Today Dependabot covers only `gradle` and `github-actions`.

## How Flux picks the new image

1. `ImageRepository kanban-board-backend-architecture` (`image: docker.io/rudenkovladimir/kanban-board-backend-architecture`, `interval: 5m`) and `ImagePolicy` as above.
2. A new overlay **under `k8s/overlays/architecture/`**, not under a new top-level folder: the existing `ImageUpdateAutomation kanban-images` has `update.path: ./k8s/overlays`, so a setter marker anywhere else is never rewritten. It holds `kustomization.yaml` with `namespace: architecture` and `images: [{name: rudenkovladimir/kanban-board-backend-architecture, newName: ..., newTag: main-N-sha7 # {"$imagepolicy": "flux-system:kanban-board-backend-architecture:tag"}}]`, the Deployment, Service, Certificate, Middlewares and IngressRoutes.
3. A new `k8s/flux-system/apps-architecture.yaml` Kustomization (`path: ./k8s/overlays/architecture`, `dependsOn: [edge]`, `prune: true`, `sourceRef: GitRepository flux-system`), listed in `k8s/flux-system/kustomization.yaml`. Use `namespace:` in the overlay, never `targetNamespace:` on the Flux Kustomization, because invariant I11 fails closed on `targetNamespace`.
4. Add `kind: Namespace architecture` to `k8s/platform/namespaces.yaml` (the namespace is owned by the `platform` Kustomization, as `kanban-prod` is).
5. **Bootstrap order:** the first tag must exist in Docker Hub before the overlay is committed, because I5 requires a well-formed `newTag` and `wait: true` would hold on a pull failure. Run the new workflow by `workflow_dispatch`, then commit the overlay with that tag. After that, every model change yields: push, new image, `ImagePolicy` picks the highest `<num>`, `fluxcdbot` commits `chore(flux): bump images` to `main`, `apps-architecture` reconciles, pod rolls. Latency is the 5 m scan plus the 5 m automation interval plus the Kustomization interval.
6. Rollout strategy `Recreate`, one replica, like the app (memory envelope). A brief gap at each deploy is acceptable for a docs site; a RollingUpdate would add a second pod's request for no benefit.

## Edge objects to copy (do not invent new ones)

Mirror `k8s/overlays/prod/ingressroute.yaml` and `certificate.yaml`:

- `Certificate architecture` in namespace `architecture`, `secretName: architecture-tls`, issuer `letsencrypt-production`. The secret and the IngressRoute must share a namespace.
- `IngressRoute architecture-https` on `websecure` with `tls.secretName`, and a SEPARATE `IngressRoute architecture-http` on `web` with the `redirect-https` Middleware and the route text `Host(...) && !PathPrefix(`/.well-known/acme-challenge/`)`. Invariant I10 fullmatches that exact shape, and I11 forbids two Kustomizations from rendering the same object, so define each name once.
- Optional `rateLimit` Middleware like `general-rate-limit`. First load of the site is roughly 20 requests, so use a burst of at least 120 and not prod's 60.
- Do not add a Traefik `compress` Middleware: SWS already compresses and the two would double-encode.

## Repo gates the new manifests must pass (read from `scripts/verify-k8s-invariants.py`)

| Gate | What it means for the new Deployment |
|------|--------------------------------------|
| I3 | the container declares BOTH `resources.requests.memory` and `limits.memory`, request <= limit |
| I4, run in CI as `--no-provisional` | every `memory:` line needs a `MEASURED` comment within 25 lines above it. A `PROVISIONAL` label FAILS CI. So the memory ladder must be run and written down in the same change that adds the Deployment. Starting hypothesis from my probe: request 16Mi, limit 32Mi (anon 16.3 MB under a worst-case serve, on a 4-CPU machine; SWS worker threads scale with CPU count, so re-measure on the VPS). This is a hypothesis, not a measurement for the VPS. |
| I5 | image tagged and not `:latest`; overlay `newTag` matches `^main-\d+-[0-9a-f]{7}$` and carries the `$imagepolicy` marker (applies because the image name contains `kanban-board-backend`) |
| I7 | the new `kustomization.yaml` root is auto-rendered (roots are discovered under `k8s/**`); nothing to register |
| I10, I11 | see edge objects above |
| kubeconform | validates the new IngressRoute and Certificate against the pinned CRD schemas through `verify-k8s-manifests.sh`; needs no change |

Also follow the app pod's existing hardening: `automountServiceAccountToken: false`, `seccompProfile: RuntimeDefault`, `allowPrivilegeEscalation: false`, `capabilities.drop: [ALL]`, plus `runAsNonRoot`, `runAsUser: 65532` and `readOnlyRootFilesystem: true` (tested working). Probes: `httpGet /health` on the container port; no exec probe is possible on a scratch image.

## Uptime and docs integration

- Extend `uptime-check.yml`: add the site URL to a probe list with a body marker. `<div id="likec4-root">` appears in `index.html`. A bare 200 is weak evidence because the fallback answers 200 for any path, so the marker is what proves the right content.
- Update `docs/INFRA_ARCHITECTURE.md` (its Maintenance Note lists the files to touch) and the LikeC4 `deployment.c4`/physical view: the new namespace and workload are exactly the kind of change the memory note "infra diagrams go stale silently" warns about, and the workload set is checked against `k8s/base` and `k8s/data` only, so `k8s/overlays/architecture` is NOT covered by the drift check unless the check is extended. Flag for the roadmap.

## Alternatives Considered

| Category | Recommended | Alternative | Why not |
|----------|-------------|-------------|---------|
| Delivery | Baked image + Flux image automation | ConfigMap holding the site | 6.0 MB exceeds the 1 MiB object limit |
| Delivery | Baked image | CI rsync/scp to the VM or a PVC | breaks pull-based GitOps and needs a new SSH path to the VM; the repo removed its CI deploy-over-SSH |
| Delivery | Baked image | GitHub Pages | the requirement is the DuckDNS hostname on the existing VPS behind Traefik |
| Registry | Docker Hub | GHCR | a second registry, a second token and Flux scan credentials for no gain; the repo's retention and tag-scheme logic is Hub-specific |
| Build location | runner builds site, `COPY` | multi-stage Dockerfile with pnpm | duplicates the toolchain pin outside the lockfile-gated job |
| Routing | history mode + SWS fallback | `--use-hash-history`, no fallback | hash mode is the zero-config fallback if the fallback traps above bite; it gives `/#/view/...` URLs and removes the missing-asset-200 hazard |
| Server | SWS scratch | nginx-unprivileged | see the table above |
| Ownership | new namespace `architecture` | reuse `monitoring` or `kanban-prod` | keeps blast radius, rate-limit and RBAC separate; costs one Namespace object |

## What NOT to add

- **No new CI orchestration tool, no Argo, no Helm chart.** One Deployment, one Service and the existing Flux patterns suffice.
- **No `likec4/actions@v1`**: unpinned by its own sample.
- **No `likec4` Docker image or `--use-dot`** on the runner: the pnpm-installed CLI with bundled WASM layout already works.
- **No Traefik `compress` middleware, no in-pod TLS, no HTTP/2 setting in SWS**: Traefik terminates TLS.
- **No server-side rendering, no Node server in the pod**: the output is static files.
- **No scheduled rebuild** (the build is not reproducible, so it would churn images and tags).
- **No `latest` tag, no digest-less base image** (I5 and repo habit: tag plus digest).
- **No second `Certificate`/`ClusterIssuer`**: `letsencrypt-production` already exists; a staging issuer is unnecessary for one extra hostname, mind only the Let's Encrypt duplicate-certificate limit if the Certificate is deleted and recreated repeatedly (limit value UNVERIFIED here).

## Open decisions for the user

1. **Indexing.** LikeC4 emits `robots.txt` with `Disallow: /`. Keep (public but unindexed) or override. Default if no answer: keep.
2. **URL form.** History mode (clean `/view/signin-ok/`, needs the fallback and accepts the stale-tab hazard) or hash mode (`/#/view/...`, no server rules). Default if no answer: history mode with the `no-cache` HTML header.
3. **Retention job** for the third repository: copy the nonprod cleanup (recommended) or defer.

## Unverified, to confirm in the phase

- Docker Hub default privacy on the namespace and first-push repository creation with the existing token.
- Time from push to a running new pod end to end (not measured; the sum of three polling intervals is an upper bound estimate, not an observation).
- Whether the site works with `--build-webcomponent=false`.
- Why two builds of the same model differ in hashes.
- SWS memory and CPU-thread behaviour on the VPS's actual core count; the I4 ladder must be run there.
- Whether nginx-unprivileged or Caddy behave as tabled (not run).

## Sources

- Local, run this session: `likec4 build --help` (1.60.0); two scratch builds of `docs/architecture/model`; SWS 2.44.1 containers on a read-only rootfs with a non-root user and no caps (headers, deep link, missing asset, health, RSS); a headless browser render of a deep link; `dig` for the architecture hostname; `trivy image` (empty result). Scratch files and builds were deleted afterwards.
- Repo, read in full or in the relevant ranges: `.planning/PROJECT.md`, `docs/DIAGRAM_CONVENTIONS.md` (LikeC4 standard), `docs/architecture/likec4.fish`, `package.json`, `pnpm-workspace.yaml`, `.github/workflows/deploy.yml`, `invariant-checks.yml` (likec4 job), `uptime-check.yml`, `.github/dependabot.yml` (ecosystem lines), `k8s/flux-system/{image-automation,apps-prod,edge,kustomization,monitoring}.yaml`, `k8s/overlays/prod/*`, `k8s/monitoring/configs/ingressroute.yaml`, `k8s/platform/{namespaces.yaml,edge/*,traefik/helmchartconfig.yaml}`, `k8s/base/app/app.yaml`, `scripts/verify-k8s-invariants.py` (docstring, I3, I4, I5), `docs/INFRA_RUNBOOK.md` DNS section.
- Context7 `/likec4/likec4` (HIGH): CLI `build` options, "strictly bound to base path", `--base "./"` relocatable, `--use-hash-history`.
- Context7 `/static-web-server/static-web-server` (HIGH, master branch, ahead of 2.44.1, see trap 1): environment variables, health endpoint, advanced headers.
- `joseluisq/static-web-server:2.44.1 --help` (HIGH, the pinned binary): `--page-fallback` serves the fallback "with a 200 status code, useful when using client routers".
- npm registry (likec4 dist-tags and publish times), Docker Hub API (tag sizes, dates, digest for SWS, nginx-unprivileged, official nginx, caddy), GitHub releases API (SWS latest v2.44.1, 2026-10-07): HIGH for versions and dates.
- docs.docker.com/docker-hub/settings (default repository privacy for repositories created by `docker push`): MEDIUM, read as a search highlight, not the full page.
- I did not run the `research-plan`/`research-store` cache seam; sources were fetched directly through Context7, the Docker Hub, npm and GitHub APIs, and local runs.
