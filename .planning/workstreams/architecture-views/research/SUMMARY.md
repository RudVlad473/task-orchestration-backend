# Project Research Summary

**Project:** kanban-board-backend, milestone v1.5 "Architecture Views"
**Domain:** LikeC4 Kruchten 4+1 model completion, published as a static site on an existing single-node k3s edge (Traefik, cert-manager, Let's Encrypt, Flux, Docker Hub)
**Researched:** 2026-10-27
**Confidence:** MEDIUM-HIGH overall. HIGH for repo facts and measured build output; MEDIUM for proposed designs (reasoned, not built); several items UNVERIFIED and listed under Gaps.

## Scope decisions already made by the user

- **Mermaid migration and retirement is OUT of this milestone.** The user decided this. ARCHITECTURE.md's build order ends with a phase 6 step "retire the Mermaid sources". That step is dropped by the user's decision: no retiring `docs/diagrams/**`, `render-diagrams.sh`, `verify-diagrams.py`, `render-manifest.tsv`, and no repointing of the `INFRA_ARCHITECTURE.md` or README embeds for that reason. The Mermaid files stay as the parity baseline.
- The error-status-split decision (client-visible discriminators only, or add filter-chain components) is **OPEN** and is NOT decided here (see Open decisions).

## Executive Summary

The milestone has two halves. The first is to finish the existing LikeC4 model (baseline `sync.mjs check` exits 0 with 33 elements, 6 tables, 4 FKs, 14 packages, 38 package edges, 2 topics, 4 workloads) so that every important runtime, delivery and physical fact is present across the five Kruchten views, each new fact tied to a check or left out. The second is to publish the `likec4 build` output as a static site on the existing VPS, as a fourth public hostname behind Traefik.

The recommended publish approach reuses everything already in the repo. The runner builds the site with the already-pinned LikeC4 1.60.0 after the drift gate passes. The output is baked into a scratch `joseluisq/static-web-server:2.44.1` image (measured: 3.75 MB compressed, runs as 65532 with a read-only root and no capabilities, serves deep links through a page fallback). The image goes to a new Docker Hub repository tagged `main-<run>-<sha7>`, Flux picks it up through an `ImageRepository` and `ImagePolicy`, and the pod gets its own `architecture` namespace, a Certificate and two IngressRoutes. Only one Deployment is added. The model side adds the missing elements and edges by view, with a drill and a hook self-test for each new check.

The main risks are these. (1) One fact is already wrong in the published model: Traefik is drawn in a namespace `edge` that does not exist; three of four researchers confirmed it. (2) Two rows of the repo's own LikeC4 standard contradict the milestone goal (the Physical row excludes the monitoring stack; the Scenarios row bans internal calls). (3) A public site publishes whatever is in the model: measured bundle contents already include an in-cluster `PGHOST` name and the `KANBAN-INGRESS` and Netcup descriptions, so a leak review must happen before the production certificate issues. (4) ACME mistakes are costly (5 failures per hour, 5 certificates per week per exact name). (5) An SPA fallback hides missing assets and returns 200 for every typo. The researchers' proposed order differs; both orders are presented below and not picked.

## Key Findings

### Confirmed defect to fix first

`docs/architecture/model/deployment.c4` line 9 places Traefik in `edge = namespace 'edge'`. Three researchers independently report it:

- FEATURES.md, Finding 1 (H2), labelled [R] (read in the repo this run).
- ARCHITECTURE.md, section 1.4 and pitfall P1, labelled SOURCE-level repo evidence.
- PITFALLS.md, Pitfall 3, labelled SOURCE; the published Physical view prints "EDGE" as a namespace box (MEASURED render, `production.png`).

Repo evidence: `k8s/platform/namespaces.yaml` declares only `kanban-prod`, `kanban-nonprod`, `kanban-data`, `monitoring`. Traefik is configured with `namespace: kube-system` (`k8s/platform/traefik/helmchartconfig.yaml` line 20). `edge` is the name of a Flux Kustomization (`k8s/flux-system/edge.yaml`), not a namespace. `sync.mjs check` cannot see it, because its workload walk covers only `k8s/base` and `k8s/data` and only Deployment, StatefulSet and CronJob (`sync.mjs` lines 217-234).

Status: confirmed in the repo. The live cluster was not queried, so the live namespace is not verified. The fix is a one-line rename to `kube-system` (ARCHITECTURE: "costs one line"), but it is a prerequisite for every node that will be placed under a namespace.

### Recommended stack (STACK.md)

- **likec4 1.60.0** (already pinned, npm `latest` 2026-10-27). `likec4 build` makes the site. Same binary the drift gate uses. Do not use `likec4/actions@v1` (its sample uses `likec4-version: latest`). No Graphviz install is needed (bundled WASM layout). Build output is 6.0 MB; no `--base` flag, because the site is served at the hostname root.
- **static-web-server 2.44.1, scratch image.** Measured: deep links 200 `text/html` via `page-fallback`, headless render of `/view/data/` shows the Logical view, `/health` 200 with no access log, TOML `[[advanced.headers]]` split works (`no-cache` on HTML, `immutable` on `/assets/**`). RSS 6.6 MB idle, 16.3 MB after serving every file. Runner-up: `nginx-unprivileged 1.30.6-alpine-slim` (needs emptyDir mounts and an nginx.conf; UNVERIFIED beyond sizes). Rejected: Caddy (24.9 MB, TLS features Traefik already owns), busybox httpd (no header control).
- **Docker Hub repo `rudenkovladimir/kanban-board-backend-architecture`.** The name contains `kanban-board-backend` on purpose, so invariant I5 applies to the new overlay.
- **Flux**: one `ImageRepository` and `ImagePolicy` with the existing `filterTags` pattern; existing Traefik and cert-manager; new Certificate on `letsencrypt-production` (STACK's stated choice; see Disagreements).
- **CI**: `actions/checkout` v5, `pnpm/action-setup` v6, `actions/setup-node` v5 (node 24.19.0), `docker/setup-buildx-action` v3, `docker/build-push-action` v6 with `linux/amd64` only.

Measured traps the phase must handle (MEASURED, SWS 2.44.1):

- A missing asset returns **200 HTML with the immutable header** (fallback applies to asset paths too). SWS 2.44.1 rejects the `status = [...]` filter on header rules, which the master-branch docs show. Test every TOML key against the pinned tag.
- Default `SERVER_CACHE_CONTROL_HEADERS=true` gives HTML `max-age=86400`; set `no-cache` on HTML explicitly in TOML.
- Two builds of the identical model gave different hashes (not byte-reproducible; cause UNVERIFIED). Never schedule rebuilds; trigger on source paths only.
- `robots.txt` emitted by LikeC4 is `Disallow: /`.
- `--build-webcomponent` accounts for 2.7 MB (`likec4-views.js`); `=false` untested (UNVERIFIED).

### Expected features (FEATURES.md)

"Complete" is defined by the researchers as: every namespace in `namespaces.yaml` plus the Helm and Flux namespaces has a node; every workload kind under `k8s/` and every `.github/workflows` file is claimed by an element or an explicit exclusion; all seven Mermaid scenarios have a LikeC4 counterpart; the Physical and Scenarios rows of the standard match what is drawn.

**Table stakes (must have):**

- Fix `edge`; add `kube-system`, `cert-manager`, `flux-system`, `monitoring` nodes (H2, H3) [R]
- Delivery path: GitHub, runner, Docker Hub repos, Flux (H5, H11, D7) [R]
- Monitoring stack and its runtime edges: Prometheus, Grafana, Loki, Alloy, node-exporter, kube-state-metrics, postgres-exporter (H6, P8) [R]
- cert-manager, 2 ClusterIssuers, 3 Certificates, Let's Encrypt (H4) [R]
- Schema Registry, `register-schemas` init job, registry edges (P5, P6) [R]
- Edge routing: IngressRoutes and rate-limit Middlewares (H7) [R]
- Backup chain PG > age > B2 (P11, S15) [R for the script; the docs still say no backup]
- Management port :9081 and scrape edge (P7) [R]
- Host-level items `KANBAN-INGRESS`, sshd, Netcup Cloud Firewall as external (H10, H11); needs a spike [D for the doc-stated rules; Netcup ruleset not in repo]
- Scenarios S2, S5 (four outcome views), S6, S7, S8, S9, S11, S15
- Amend `docs/DIAGRAM_CONVENTIONS.md` (Physical row, Scenarios row, view table) in the same change

**Differentiators (should have, as capacity allows):** `chk` coverage for new elements (extend the `sync.mjs` walk to `k8s/platform`, `k8s/monitoring`, `.github/workflows`); signup race S3/S4 (S4 code path has no test, so its label must say so); S10 DROP scenario (counter attribution unproven [D]); layer overlay D3; per-event Avro subjects L5; state markers H9 and trust boundaries H12; planned backup alert H14 tagged `planned`; IPv6 note (`KANBAN-INGRESS` is IPv4-only [D]).

**Anti-features:** `postgres-bridge` (orphaned: no Flux Kustomization applies it, [R]); one element per Kustomization, object or Helm value; Pods, Secrets, memory limits, PVC sizes in labels; cAdvisor, Docker, Caddy, Compose, Promtail, DOCKER-USER; Alertmanager as an element (disabled in `kube-prometheus-stack.yaml`); a scenario per endpoint; counts in labels (for example "14 events"); the local `docker-compose.yml`; Netcup firewall rules as content; `alt`/`par` branches inside one dynamic view (untested, V1); migrating or deleting any Mermaid file (out of scope, see Scope decisions).

**Model finding not in the standard:** a nightly backup CronJob (02:17 UTC, age-encrypted dump to a Backblaze B2 bucket) is live in the repo ([R], `postgres-backup.sh`), while `INFRA_ARCHITECTURE.md` lines 78-83 still say "there is still no backup". The model has the `backup` job but no bucket, no encryption and no edge outside the cluster.

### Architecture approach (ARCHITECTURE.md)

Two seams: (1) model honesty, `docs/architecture/sync.mjs` with `render()` for generated facts and `check()` for checked facts; (2) publish, which does not exist yet and reuses the app's path (`deploy.yml` tag, Flux `ImageRepository` plus `ImagePolicy`, `ImageUpdateAutomation` committing into `k8s/overlays/**`). Governing rule from the standard: generated (written from code), checked (hand-written, `metadata` compared with code), authored (unverified).

**Major components:**

1. `sync.mjs` plus a `yaml` parser dependency (not installed today; latest 2.9.1). Reads Flux Kustomizations, HelmReleases, workflow jobs, the Flux controllers in `gotk-components.yaml` (its label-first export defeats the current regex). One reader, two consumers (render and check).
2. `deployment.c4` growth: namespace nodes under `k3s`, `provider`-kind nodes for GitHub and Docker Hub, separate deployment views (`delivery`, `monitoring`) tagged `#physical`. Do not grow `production`, which is already 1965 px wide.
3. Site image build: Dockerfile plus static-server stage (see Disagreements on where the build runs), tag `main-<run>-<sha7>`.
4. `architecture-site.yml` workflow: frozen-lockfile install, `sync.mjs check`, build, push with `environment: production`. A PR-time build job without secrets goes in `invariant-checks.yml`.
5. Site manifests: `base/site` plus `overlays/site` (the setter-marker path must sit under `./k8s/overlays`, which the existing `ImageUpdateAutomation` covers), namespace `architecture`, Flux Kustomization `site`, `dependsOn platform` and `edge`.
6. Edge objects: Certificate and IngressRoutes in the overlay, in the workload's namespace. The Certificate for the hostname must not live in a shared Kustomization with a placeholder route (invariant I11; the Grafana 503 race of 2026-09-28 and 2026-10-24).

Gate consequences (read from `scripts/verify-k8s-invariants.py`): I3 (requests and limits), I4 with `--no-provisional` in CI (a `MEASURED` comment needed; a PROVISIONAL label fails CI), I5 (tag shape and `$imagepolicy` marker), I10 (the web route must fullmatch the ACME-excluding rule), I11 (one owner per object). The memory numbers (request 16Mi, limit 32Mi) are a STACK hypothesis, not a VPS measurement.

Patterns: authored shell with checked metadata and generated children; every new check proven red then green with a `drill.fish` drill (`fresh()` must copy the new source directories, or a reader runs against nothing and reports clean); a deployment-model instance without a relationship is invisible in the computed view (MEASURED, PITFALLS 2).

### Critical pitfalls (PITFALLS.md)

1. **Dev server exposure (MEASURED).** `likec4 start` answers `/@fs/etc/hostname` with real bytes and accepts any `Host` header on 1.60.0. Deploy only `likec4 build` output as static files. Gate any manifest or workflow that runs `likec4 start`, `preview`, or passes `--listen`.
2. **Unconnected deployed elements vanish (MEASURED).** A deployment node with no relationship is not drawn in `production`. Model `client -> traefik -> site` explicitly; add a check that every instance appears in a computed deployment view.
3. **Merged deployment edges read `[...]` (MEASURED, 4 of 9 edges in `production`).** Give deployment-level relationships their own titles; make zero `[...]` a phase acceptance criterion.
4. **Public-surface leakage (MEASURED bundle contents).** `PGHOST postgres.kanban-data`, `KANBAN-INGRESS` and Netcup descriptions are present in the built JS. Write fresh one-sentence descriptions; scan the built artifact (not the source) against a deny-list of IP literals, key paths, `ssh`, `kubectl`, Secret names; review before the production certificate. Certificate Transparency publishes the hostname at issuance regardless of `robots.txt` (general knowledge, HIGH).
5. **ACME order (DOCS).** Staging first, DuckDNS record resolving on a public resolver before any Certificate, no AAAA record (DuckDNS cannot detect IPv6; `KANBAN-INGRESS` is IPv4-only), and `serviceType: ClusterIP` on the solver (the default NodePort creates a transient NodePort Service on every issuance and renewal, invisible to I1 and to the runtime inventory). Let's Encrypt limit: 5 certificates per exact identifier set per 7 days, with no override (DOCS, updated 2026-08-05).

Also important: 6 (blanket `try_files`/page fallback masks misses, MEASURED for SWS 200-on-miss), 7 (measured CSP policy loads cleanly on cold load, but interactive features are UNVERIFIED), 8 (placeholder-route race, SOURCE), 9 (Traefik 64Mi edge limit under concurrent cold loads, UNVERIFIED), 12 (numbers in labels, unchecked enumerations such as the `postgres` JDBC list that omits `activity_log`).

## Where the researchers disagree (not resolved here)

| Topic | Position A | Position B | Flag |
|---|---|---|---|
| Build location | STACK: runner builds with pinned pnpm and LikeC4, Dockerfile only `COPY`s the output | ARCHITECTURE: multi-stage Dockerfile does pnpm frozen install and `likec4 build` inside `docker build` | Affects drift-job reuse and the `--use-dot` question (PITFALLS 6: `--use-dot` defaults true inside a container; unproven) |
| Dockerfile and config location | STACK: `infra/architecture-site/` (Dockerfile, `sws.toml`), avoids `docs/architecture/` because the pre-commit hook gates it | ARCHITECTURE: `docs/architecture/Dockerfile` beside the model | Layout choice |
| Overlay and Flux names | STACK: `k8s/overlays/architecture/`, Flux Kustomization `apps-architecture` | ARCHITECTURE: `k8s/base/site/`, `k8s/overlays/site/`, Flux Kustomization `site` | Names only; pick one before manifests |
| Edge fix timing | User decision: fix first | ARCHITECTURE puts the fix in phase 3 (after publish); PITFALLS puts it in phase A | Follow the user's "fix first" |
| SPA fallback | STACK: `page-fallback` returns 200 for any path; accepts the stale-tab hazard | PITFALLS: fallback only for extensionless paths, real 404 for file misses; ARCHITECTURE: serve `404.html` (byte-identical to `index.html`) with a 404 status, or hash history | STACK measured that SWS 2.44.1 cannot filter the header by status, so PITFALLS' rule may need another server (nginx-unprivileged or hash mode). Not reconciled |
| Certificate issuer | STACK: new Certificate on `letsencrypt-production` directly | PITFALLS: staging first, separate commit to flip to production | Not picked; see Open decisions |
| Uptime probe | STACK: body marker `<div id="likec4-root">` | PITFALLS: marker plus one hashed asset URL; ARCHITECTURE: probe `/` and assert 200 | Use the marker at minimum |
| Docker Hub retention | STACK: copy the nonprod cleanup for the third repo (recommended) | ARCHITECTURE: no prune proposed for a few-MB image | Open decision |
| Public review | PITFALLS: a leak scan and an interactive CSP walk as an explicit phase before the production flip | ARCHITECTURE: no review phase; "confirm before shipping" (P8) | Needs a gate in the roadmap |

## Implications for Roadmap

### Two suggested phase orders (not chosen)

**Order 1, ARCHITECTURE.md (publish first, so every model change flows through a finished pipeline and gives a live URL):**

- **Phase 1 (P), publish skeleton.** Dockerfile (or runner build per STACK), `.dockerignore`, `package.json` build script shared with `likec4.fish site`, `architecture-site.yml`, PR-time build job. Operator creates the public Docker Hub repo and the DuckDNS subdomain. Prove red then green: a drifted model fails before push. First run by `workflow_dispatch`. Depends on: none.
- **Phase 2 (P), edge and serving.** Namespace `architecture`, `base`/`overlays` site manifests, Flux `site` Kustomization, `ImageRepository`/`ImagePolicy`, I5 generalisation with selftest, MEASURED memory ladder, uptime probe URL and `timeout-minutes` (480 s arithmetic, P6 in ARCHITECTURE), and the one model element, instance and `traefik ->` edge that the workload check forces. Verify by a model edit producing a tag bump and a changed page. Depends on: 1.
- **Phase 3 (M-A), foundations.** `yaml` dependency; split `render()`; unknown-metadata-key check; extend `drill.fish` `fresh()`, hook triggers and self-test; amend the standard (`depends`, `provider`, Physical row); fix `edge` to `kube-system`; G10 namespace check; G11 placement check. Depends on: none, but edits `deployment.c4` as phase 2, so run sequentially.
- **Phase 4 (M-B), delivery path.** `github`, `actions`, `dockerhub`, `flux` systems; G1 jobs, G2 repo set, G3 Kustomizations, G4 Flux controllers; `delivery` deployment view; scenarios `push-to-image`, `image-to-rollout`, `publish-architecture-site`. Depends on: 2, 3.
- **Phase 5 (M-C), monitoring.** Authored Prometheus, Grafana, Loki, Alloy, node-exporter, kube-state-metrics, postgres-exporter; G5 HelmRelease check, G7 datasources, G9 hosts; `monitoring` deployment view. Depends on: 3, 4.
- **Phase 6 (M-D), remaining scenarios.** `signup`, `error-status-split` (one view per outcome, after the open decision), `inbound-packet-path`, `ingress-to-pod`, nightly backup, uptime probe; optional G12-G14. The Mermaid retirement step is DROPPED by the user's decision. Depends on: 4, 5.

**Order 2, PITFALLS.md (model first, publish after gates, public review before the flip):**

- **A. Model completion:** deployment, scenario and any further views; fix `edge`; write the view-membership and `[...]` acceptance criteria; define the site legibility rule (currently "undefined" in the standard) before scenarios are judged.
- **B. Honesty gates:** new drift checks, drills, hook self-test cases, namespace check, unknown-metadata-key check.
- **C. Build and serve:** `likec4 build`, static server, image, Flux wiring, memory ladder, CSP in Report-Only, cache headers.
- **D. Edge (staging issuer only):** DuckDNS record, staging Certificate, IngressRoutes, `serviceType: ClusterIP`, uptime probe (only after production, Pitfall 15).
- **E. Public-surface review and go-live:** artifact leak scan, interactive CSP walk, then the staging-to-production flip, then any README link.

**Where the two disagree:** Order 1 puts the publish pipeline before model completion and has no public-review phase. Order 2 completes the model first, puts all gates before any build, and reserves a review before the production flip. Order 1 gives a live URL early to verify against; Order 2 avoids publishing a model that still carries the wrong `edge` node and the unreviewed descriptions. The roadmapper should choose, with the review gate written in whichever order is chosen.

### Research flags

Phases or items needing deeper research (spike or render experiment) during planning:

- **Host-level elements (H10: `KANBAN-INGRESS`, sshd, k3s secrets-encryption, Netcup firewall as external):** no current spec kind fits an iptables chain; High complexity; S8, S10 depend on it. Schedule last (FEATURES MVP step 5).
- **Dynamic views:** whether `par` can be expressed (FEATURES UNVERIFIED); parent plus child participants in one sequence view untested (PITFALLS 4); the 7-8 participant width is EXTRAPOLATED, not rendered.
- **Merged edges of mixed kinds** (sync plus async) styling: UNVERIFIED (PITFALLS 5).
- **Deployment-scoped non-Kubernetes elements** (an iptables chain as a deployment instance): UNVERIFIED (FEATURES).
- **LikeC4 1.60 metadata on relationships** (ARCHITECTURE G7 shape): UNVERIFIED; put metadata on elements instead until checked.
- **`--use-dot` in a container build**: unproven (ARCHITECTURE P9, PITFALLS 6). Test in the actual build step.
- **Static server choice and memory on the VPS**: SWS memory measured on this machine's CPU count; worker threads scale with cores, so the I4 ladder must be run on the VPS (STACK).
- **Traefik edge load under concurrent cold loads** (PITFALLS 9): UNVERIFIED; measure RestartCount and dmesg before and after.
- **Interactive CSP walk** (export menus, search, details, sequence walkthrough): UNVERIFIED (PITFALLS 7).
- **Error-status-split scenario form:** blocked on the open decision (FEATURES finding 3).

Phases with standard patterns (skip the research step):

- **Flux wiring** for the new image: a copy of the existing app pattern (`ImageRepository` plus `ImagePolicy` plus setter overlay), one-for-one (STACK, ARCHITECTURE).
- **Drill and self-test machinery** for new checks: 12 drills and 15 self-test cases already exist as the pattern (ARCHITECTURE Pattern 2; PITFALLS 12).
- **Route ownership (I10/I11)**: a worked precedent exists (the 2026-10-24 Grafana fix; PITFALLS 8).
- **Uptime probe addition**: a copy of the existing `uptime-check.yml` entry, with the timeout arithmetic (ARCHITECTURE section 3.4).

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH for build output and SWS serving (MEASURED locally on 2.44.1); MEDIUM for Docker Hub and Flux behaviour (UNVERIFIED here) | Versions and sizes from registry APIs; nginx-unprivileged and Caddy rows UNVERIFIED beyond sizes |
| Features | HIGH for the inventory (each row read in the repo this run, [R]); MEDIUM for the significance ranking (judgement); LOW for [U] items | Nothing checked against the running cluster; [D] items are doc claims |
| Architecture | HIGH for "what the repo does today" (each claim cites its file); MEDIUM for proposed designs | No new check built or run; baseline `sync.mjs check` run is the only execution |
| Pitfalls | MEDIUM-HIGH; MEASURED for the LikeC4 and SWS behaviour on this repo; DOCS for ACME, cert-manager, Let's Encrypt, DuckDNS | Live cluster not queried by design; DuckDNS subdomain cap LOW-MEDIUM (third-party only) |

**Overall confidence:** MEDIUM-HIGH. The repo facts are solid. The proposed designs and the phase order are not settled.

### Gaps to address

- **Live cluster state** (all four files): the namespace list as deployed, the `edge` node's live effect, Traefik load, the solver Service type during issuance. Read from manifests and docs only. Handle: verify with `k3s kubectl` at the relevant phase, not from memory.
- **Docker Hub default privacy and repository creation on first push** (UNVERIFIED; STACK read a search highlight, MEDIUM). Handle: create the repo explicitly and public before the first push, and check its visibility.
- **Docker Hub tag retention on a free public repo** (UNVERIFIED). Handle: do not rely on old tags persisting; see the retention decision.
- **DuckDNS subdomain limit** (third-party: 5 per account, LOW-MEDIUM; the repo already holds 3, so the fourth is fine and the fifth is last). Handle: check the account page before naming a fifth.
- **Let's Encrypt limit values** are DOCS (HIGH), but the refill cadence (about 34 hours per refill) comes from PITFALLS' recovery table and was not re-verified.
- **Non-reproducible builds** (cause not investigated). Handle: trigger on source paths only; never diff builds to detect change.
- **`--build-webcomponent=false`** site behaviour untested. Handle: verify in the build phase before dropping 2.7 MB.
- **SWS 2.44.1 cannot filter header rules by status** (MEASURED); the PITFALLS limited-fallback rule and the STACK missing-asset trap are unreconciled. Handle: decide the fallback and URL form (open decision 2) and test a cold deep link, a refresh, and a missing `.js` in the real image.
- **Netcup Cloud Firewall ruleset** is outside the repo (unverifiable). Handle: external element with description "not in this repo", no rules.
- **The legibility rule for LikeC4 views** is undefined in the standard. Handle: define it in a browser at 1440 x 900 before judging scenario and deployment views.
- **Build stamp and staleness:** nothing ties the live site to the model's last commit (PITFALLS 12). Handle: `--title` with the short SHA of the last model change.
- **Interactive CSP features and the `connect-src` consequences** (draw.io export, Kroki): UNVERIFIED. Handle: Report-Only first, then decide.
- **Time from push to a running pod** is not measured; the sum of polling intervals is an upper bound estimate.
- **Pre-commit hook scope**: `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`, `.github/workflows` are not hook triggers, so drift there is caught only by CI (`likec4-model-drift`). Handle: keep that CI job required, and extend trigger paths with each reader.

## Open decisions for the user

Each has a default if no answer, as the researchers proposed.

1. **Indexing (`robots.txt`).** LikeC4 emits `Disallow: /`. Keep it (public but asks crawlers to stay away) or index. Default if no answer: keep. Either way, `robots.txt` is not access control, and the docs must not say it is. Certificate Transparency reveals the hostname at issuance regardless.
2. **URL form.** History mode gives clean `/view/<id>/` URLs and needs a fallback (accepts the stale-tab hazard; SWS cannot filter the status). Hash mode gives `/#/view/...`, needs no server rules, and removes the missing-asset-200 hazard. Default if no answer: history mode with `no-cache` on HTML (STACK). The PITFALLS and ARCHITECTURE positions differ (see Disagreements).
3. **Retention for the third Docker Hub repository.** Copy the nonprod cleanup (about 45 lines of shell, STACK's recommendation) or defer; the image is about 4 MB plus the site. Default if no answer: defer, since ARCHITECTURE proposes no prune and the cost is small.
4. **Whether cert-manager, Flux and monitoring nodes belong in the Physical view.** FEATURES lists them as must-haves (H3, H4, H5, H6); PITFALLS leaves "a Grafana, cert-manager or Flux node in the Physical view" as not covered. Decide before the Physical view is judged complete. No default proposed by the researchers.
5. **Error-status split (OPEN, not decided here).** The `error-status-split` scenarios (S5, four views) need either (a) client-visible discriminators only: 401 with no SQL, 403 after the ownership SELECTs, 409 on the version check; or (b) add filter-chain components so the internal layer that answers is shown. The Scenarios row of the standard ("do not represent internal calls") contradicts (b). Blocks S5 only; everything else can proceed.
6. **Standard amendments.** FEATURES finds two rows of `docs/DIAGRAM_CONVENTIONS.md` contradicting the milestone: the Physical row (line 105: monitoring stack "excluded, as in `production-host.mmd`") and the Scenarios row (internal calls not represented). Both must be rewritten in the same change as the model work. The new wording is the user's to approve.
7. **Certificate issuer order** (see Disagreements): staging first, then a separate production flip commit (PITFALLS), or production directly (STACK). Default if no answer: staging first, because a failed production order spends the 5-per-week budget.
8. **Where the site Dockerfile lives and where the build runs** (see Disagreements).

## Sources

### Primary (HIGH confidence, measured or read this run)

- Local `likec4 build --help` (1.60.0) and scratch builds of `docs/architecture/model` (STACK, ARCHITECTURE, PITFALLS)
- SWS 2.44.1 containers on a read-only rootfs, non-root, no caps: headers, deep link, missing asset, health, RSS (STACK)
- `likec4 export json`, `export png --seq` and `start` on loopback (PITFALLS: deployment edges, sequence widths, `/@fs` read, `Host` header)
- Headless Chromium cold load of `/` and `/view/signin-ok/` with the CSP policy under test (PITFALLS 7)
- `scripts/verify-k8s-invariants.py` (I3, I4, I5, I10, I11), `sync.mjs` (lines 217-234, 238-329), `drill.fish`, `.githooks/pre-commit`
- Repo manifests: `k8s/platform/namespaces.yaml`, `k8s/platform/traefik/helmchartconfig.yaml`, `k8s/flux-system/*.yaml`, `k8s/overlays/{prod,nonprod}/*`, `k8s/monitoring/**`, `k8s/base/app/app.yaml`, `.github/workflows/{deploy,backup-image,invariant-checks,uptime-check}.yml`
- Repo docs: `docs/DIAGRAM_CONVENTIONS.md` ("LikeC4 standard"), `docs/INFRA_ARCHITECTURE.md`, `docs/INFRA_RUNBOOK.md`, `docs/AUTH_FLOWS.md`, `docs/ARCHITECTURE.md`
- Docker Hub API, npm registry, GitHub releases API (versions, sizes, dates; STACK)
- Context7 `/likec4/likec4` (CLI options, base path, hash history) and `/static-web-server/static-web-server` (master branch, ahead of 2.44.1: see trap)

### Secondary (MEDIUM confidence)

- docs.docker.com/docker-hub/settings (default repository privacy; read as a search highlight, STACK)
- likec4.dev deployment views and dynamic views (DOCS, via context7 and the site; PITFALLS)
- cert-manager HTTP-01 page and FAQ (DOCS); Let's Encrypt rate limits updated 2026-08-05 (DOCS, HIGH)
- DuckDNS FAQ (IPv6 not detected; MEDIUM via a summarising fetch)
- Kruchten's 1995 selection criteria (from memory, FEATURES; MEDIUM)

### Tertiary (LOW confidence, needs validation)

- DuckDNS five-subdomain cap (third-party add-on README and listings, PITFALLS 10)
- Certificate Transparency publishing behaviour (general knowledge, PITFALLS 11)
- likec4 issue 3456 "metadata schema" open status (not opened, PITFALLS 12)

---
*Research completed: 2026-10-27*
*Ready for roadmap: yes, once the open decisions above are answered or defaulted. The `edge` fix is a prerequisite, and the phase order is unresolved between the two proposals.*
