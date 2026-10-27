# Architecture Research

**Domain:** extending an existing LikeC4 model (5 Kruchten views) and publishing it as a static site on an existing single-node k3s + Flux + Traefik + cert-manager platform
**Milestone:** v1.5 "Architecture Views" (new features only)
**Researched:** 2026-10-27
**Confidence:** HIGH for every statement about what the repo does today (each cites the file it was read from). MEDIUM for the proposed designs (reasoned from those files, not built). LOW or UNVERIFIED items are labelled in place and collected in "Open items".

Baseline measured at research time: `node docs/architecture/sync.mjs check` exits 0 with
`33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads`.

## Standard Architecture

### The two seams this milestone plugs into

```
  CODE FACTS                      MODEL                               PUBLISH
  migrations, Java imports  -->   docs/architecture/model/*.c4  -->   likec4 build  -->  image  -->  Flux  -->  Traefik
  k8s/base, k8s/data              (generated.c4 | authored+checked)   (static site)      (Docker Hub)  (k8s/)   (cert-manager)
        ^                                  |
        +---- sync.mjs check  <------------+      gates: pre-commit hook (trigger paths) + CI job likec4-model-drift
```

Seam 1 (model honesty) is `docs/architecture/sync.mjs`: one `render()` that rewrites `generated.c4`
from code, and one `check()` that diffs, validates, and compares metadata with code
(`sync.mjs` lines 238-329). Seam 2 (publish) does not exist yet; every building block it needs
exists once, as the app's path: `deploy.yml` builds and pushes `main-<run>-<sha7>`, Flux
`ImageRepository` + `ImagePolicy` select the highest `<run>`, `ImageUpdateAutomation` commits the tag into
`k8s/overlays/*/kustomization.yaml`, `kustomize-controller` applies it (`k8s/flux-system/image-automation.yaml`,
`.github/workflows/deploy.yml` lines 109-126).

### Component map: new vs modified

| Component | Status | Path | Responsibility |
|---|---|---|---|
| `yaml` parser dependency | NEW | `docs/architecture/package.json` | Structured reads of Flux, HelmRelease, workflow, kustomization YAML. Not installed today: `pnpm-lock.yaml` mentions `yaml` only as an optional peer (line 614), and the existing readers are regexes. Registry latest 2.9.1 (`npm view yaml version`, 2026-10-27). |
| New fact readers in `sync.mjs` | MODIFIED | `docs/architecture/sync.mjs` | One function per source family, each feeding `render()` (generated) or `check()` (checked). |
| New drills | MODIFIED | `docs/architecture/drill.fish` | `fresh()` copies only `k8s/base k8s/data k8s/platform` plus migrations and Java (lines 12-20); it must also copy `.github/workflows`, `k8s/flux-system`, `k8s/overlays`, `k8s/monitoring`. |
| Hook trigger list | MODIFIED | `.githooks/pre-commit` lines 205-206 | Add the new source paths; extend the 15-case self-test `scripts/pre-commit-likec4-selftest.py`. |
| `deployment.c4`, `model.c4`, `views.c4`, `spec.c4` | MODIFIED | `docs/architecture/model/` | New nodes, elements, views, relationship kind, deployment-node kind. |
| Site image build | NEW | `docs/architecture/Dockerfile`, `docs/architecture/.dockerignore` | Multi-stage: pinned Node + pnpm frozen install + `likec4 build`, then a static server stage. |
| Site publish workflow | NEW | `.github/workflows/architecture-site.yml` | Drift gate, build, push `main-<run>-<sha7>`. |
| PR-time image build job | NEW (job) | `.github/workflows/invariant-checks.yml` | Build without push, no secrets. |
| Site manifests | NEW | `k8s/base/site/`, `k8s/overlays/site/` | Deployment + Service; IngressRoute, Certificate, image setter, measured memory. |
| Namespace | MODIFIED | `k8s/platform/namespaces.yaml` | Add one `Namespace`. |
| Flux wiring | MODIFIED | `k8s/flux-system/kustomization.yaml`, new `k8s/flux-system/site.yaml`, `k8s/flux-system/image-automation.yaml` | One Kustomization, one `ImageRepository`, one `ImagePolicy`. |
| Gate I5 overlay half | MODIFIED | `scripts/verify-k8s-invariants.py` line 538 and its selftest | Cover the site image (see Pattern 5). |
| Uptime probe | MODIFIED | `.github/workflows/uptime-check.yml` | Add the site URL; raise `timeout-minutes` (see Pitfall P6). |

## Part 1: What can be generated, checked or authored

The ladder is in `docs/DIAGRAM_CONVENTIONS.md` "## LikeC4 standard": generated (written from code),
checked (hand-written, `metadata` compared with code), authored (unverified). Today only five classes
of fact are above authored: tables, foreign keys, package components, package imports edges (generated),
and topics, workload set, `metadata.source` existence (checked) (`sync.mjs` lines 308-319).

### 1.1 Triage of the new facts

"Rung" is the target. "Recommend" is BUILD (in the milestone), OPTIONAL (cheap once the reader exists)
or NO (with reason).

| # | Fact | Source read | Rung | Mechanism | Recommend |
|---|---|---|---|---|---|
| G1 | Jobs of the delivery workflows and their `needs` DAG | `.github/workflows/deploy.yml` (7 jobs: `setup`, `run-tests`, `build-and-push-docker-image`, `flyway-verify`, `flyway-verify-nonprod`, `cleanup-old-images`, `cleanup-old-images-nonprod`), `backup-image.yml`, new site workflow | generated | `extend actions { job_<id> = component ... }` plus a new structural edge kind `depends`, written by `generate`. Model only the three delivery workflows, not all nine files (`api-fuzz`, `uptime-check`, `security-scan`, `dependency-submission`, `verify-rate-limit`, `secret-scan` are not on the delivery path). A renamed job breaks the authored dynamic views that name it, so rename drift is caught by the model's own validation, the way D3/D4 are. | BUILD |
| G2 | Docker Hub repositories pushed vs repositories Flux scans | `env.DOCKERHUB_REPOSITORY*` in `deploy.yml` and `backup-image.yml` vs `ImageRepository.spec.image` in `k8s/flux-system/image-automation.yaml` | checked | Set check: every Flux-scanned repo is pushed by some workflow. The backup repo is pushed but deliberately not scanned (the CronJob pins tag plus digest, `backup-cronjob.yaml` line 58), so the rule is one-directional: scanned implies pushed. | BUILD |
| G3 | Flux Kustomizations: name, `path`, `dependsOn` | `k8s/flux-system/{platform,data,cert-manager,edge,apps-prod,apps-nonprod,monitoring-controllers,monitoring}.yaml` | generated | One component per Kustomization, `depends` edges from `dependsOn`. Same data `collect_flux_kustomizations` already reads in `scripts/verify-k8s-invariants.py` line 448. Gives a reconcile-order view (platform -> data, cert-manager -> edge -> apps). | BUILD (small once G1's `depends` kind exists) |
| G4 | Flux controllers | 6 `kind: Deployment` documents in `k8s/flux-system/gotk-components.yaml` (`source`, `kustomize`, `helm`, `notification`, `image-reflector`, `image-automation` controllers) | checked | Extend `workloads()` to this file. The current name regex `^metadata:\s*\n\s+name:` (sync.mjs line 225) cannot match: Flux's export puts `labels:` before `name:` (gotk-components.yaml lines 2570-2577). Needs the `yaml` parser, not a tweaked regex. Model all six as one-line containers of a `flux` system, or the two-way workload check fails. | BUILD |
| G5 | HelmReleases: name, chart, namespace | `k8s/monitoring/controllers/*.yaml` (`kube-prometheus-stack`, `loki`, `alloy`, `postgres-exporter`), `k8s/platform/cert-manager/cert-manager.yaml` | checked | New element metadata `helmrelease 'kube-prometheus-stack'`, two-way set check exactly like `workload`. Reason this is metadata and not generated elements: the running Deployments, StatefulSets and DaemonSets are rendered by the charts, invisible in repo YAML (the same known hole `verify-k8s-invariants.py` lines 16-18 records). The authored element says "Prometheus"; the check says "a HelmRelease that owns it exists". | BUILD |
| G6 | Prometheus scrape targets | `k8s/monitoring/configs/podmonitors.yaml` (`namespaceSelector` kanban-prod, kanban-nonprod; selectors `app.kubernetes.io/name: redpanda` and `app`; ports `admin` and `management`, paths `/public_metrics`, `/actuator/prometheus`) joined to the labels in `k8s/base/*` | generated | Generate `prometheus -> <element with matching workload>` edges tagged `#generated`, label from `port` and `path`. Needs the base Deployment's `app.kubernetes.io/name` label, which `app.yaml` carries (lines 11, 21, 26). No new relationship kind needed (provenance is the tag). | OPTIONAL |
| G7 | Grafana to Prometheus and Loki | `k8s/monitoring/configs/datasources.yaml` (`kps-prometheus.monitoring.svc:9090`, `loki.monitoring.svc:3100`) | checked | Element metadata on `grafana` holding the two hosts, compared with the ConfigMap. Relationship-level metadata in LikeC4 1.60 is UNVERIFIED, so put the metadata on the element. | BUILD |
| G8 | Alloy to Loki | `loki.write` URL inside `k8s/monitoring/controllers/alloy.yaml` `values.alloy.configMap.content` (line 81) | checked | Regex over that one string. | OPTIONAL |
| G9 | Public hostnames and the Service port behind them | `k8s/overlays/{prod,nonprod}/ingressroute.yaml`, `k8s/monitoring/configs/ingressroute.yaml`, new site route | checked | Metadata `host`, `port` on the target element; set equality with every `Host(...)` literal and `services[].port`. Also check each route host appears in a `Certificate.dnsNames` (a route with no certificate serves Traefik's default cert; nothing flags it today). | BUILD |
| G10 | Namespace nodes | `k8s/platform/namespaces.yaml` (4: `kanban-prod`, `kanban-nonprod`, `kanban-data`, `monitoring`) plus system namespaces `kube-system`, `flux-system`, `cert-manager` | checked | Node titles in `deployment.c4` vs that file plus a short allowlist of the three system namespaces (their source is `helmchartconfig.yaml` line 20, `gotk-components.yaml`, `cert-manager.yaml` line 23). | BUILD |
| G11 | Which workload runs in which namespace | each overlay's `kustomization.yaml` (`namespace:`, `resources:`) and `k8s/data/postgres/kustomization.yaml` (`namespace: kanban-data`) | checked | Resolve `resources:` recursively (dirs and files), map workload name to namespace, compare with `instanceOf` placement in `deployment.c4`. Today only the workload SET is checked; placement is authored (`DIAGRAM_CONVENTIONS.md` Viewpoints table, Physical row: "workloads checked ... nodes authored"). This is the largest honesty gain in Physical. | BUILD |
| G12 | Consumer group, executor thread prefix | `ActivityLogConsumer.GROUP_ID = "activity-log"`, `AsyncConfig` `setThreadNamePrefix("kafka-publish-")` | checked | Two regexes. Upgrades two authored Process facts; not strictly new-feature. | OPTIONAL |
| G13 | `METHOD /path` in scenario step titles | controllers use `ApiPaths` constants concatenated at class and method level (`ColumnController.java:33,45`), context path `/api` (`application.properties:3`) | checked | Needs constant resolution across `ApiPaths.java`. No committed OpenAPI document exists to read instead (searched `docs/`, `src/`, `scripts/`). | OPTIONAL, late |
| G14 | Postgres ClusterIP `10.43.54.32` | `deploy.yml` (4 places, two jobs), `k8s/data/postgres/postgres.yaml`, `k8s/data/postgres-bridge/postgres-bridge.yaml`, `infra/vm/sshd/kanban-ci-tunnel.conf` | checked | Equality of the literal across the four files. A silent-failure class: the runner's forward and the sshd `PermitOpen` must agree with the pinned Service IP. | OPTIONAL |
| A1 | External systems: GitHub, Docker Hub, DuckDNS, Let's Encrypt, Backblaze B2 | none in the repo | authored | Say so in the description. `metadata.source` can point at the file that names them. | n/a |
| A2 | Host firewall layers `[1]`..`[5]`, scenario step sequences | `infra/vm/k3s-host-firewall.sh` exists for `[2]`; Netcup Cloud Firewall `[1]` is outside the repo (`INFRA_ARCHITECTURE.md` lines 85-99) | authored (`source` existence checked for `[2]`) | | n/a |
| N1 | Image tag or digest in any model label or metadata | `k8s/overlays/*/kustomization.yaml` `newTag` | NO | Flux rewrites that field on every image (`image-automation.yaml` `update.path: ./k8s/overlays`). A generated fact derived from it makes `likec4-model-drift` red after every automated bump, and the bot cannot regenerate. Never read `images:` in overlays. |
| N2 | Poll intervals, retention counts, schedules in labels | `interval:` fields, "keep 5 newest", cron `17 2 * * *` | NO in labels | The standard's rule: "a retry count, a schedule or a ceiling is either covered by a check or left out of the label". If wanted, generate them into `metadata` from the YAML field, never into a title. The old Mermaid `delivery-nodes.mmd` carries "poll 1m" in a label; do not port that. |
| N3 | A `kubectl kustomize` render inside `sync.mjs` | | NO | Needs the pinned kubectl that `scripts/verify-k8s-manifests.sh --tool kubectl` downloads. The placement check (G11) needs only `kustomization.yaml` parsing. |

### 1.2 Authored shell, generated children

The existing generator already does this with `extend postgres { ... }` and `extend kanban { ... }`
(`sync.mjs` lines 244-271). Keep the pattern: authored `model.c4` holds the parent (`github`, `actions`,
`dockerhub`, `flux`, `monitoring`); `generated.c4` extends it with children (jobs, Kustomizations).
Split `render()` into one function per source family returning sorted line arrays, so output stays
deterministic and a diff names the family that moved.

### 1.3 Standard amendments the milestone needs

All three belong in `docs/DIAGRAM_CONVENTIONS.md` in the same change as the code (the standard says a
migration ends when its old home is gone, and `INFRA_ARCHITECTURE.md`'s Maintenance Note records that
unlisted facts go stale).

1. **Relationship kind `depends`** (structural, generated only): job `needs` and Flux `dependsOn`. Add to
   `spec.c4` beside `fk` and `imports`. Rationale: it is ordering, not a runtime interaction, so it should
   not borrow `sync`.
2. **Deployment-node kind `provider`** for external SaaS (GitHub, Docker Hub) with no CPU architecture.
   The standard requires every physical node to carry its architecture in the title because `technology`
   does not render on deployment nodes (spike 006); that rule suits `server` only. Keep the rule for
   `server` (`GitHub-hosted runner (x86_64)`), and when the proposed "every `server` title names an
   architecture" check is built, scope it to `server`. The GitHub runner's architecture is asserted in
   `docs/diagrams/physical/delivery-nodes.mmd`; the build is `linux/amd64` only (`deploy.yml` lines 128-129).
3. **Physical row text.** It says the monitoring stack is "excluded, as in `production-host.mmd`". That
   becomes false the moment a monitoring namespace node exists. Rewrite the row; also update "Does not
   represent: pods, replicas, secrets".

### 1.4 An existing inaccuracy to fix first

`deployment.c4` places Traefik in `edge = namespace 'edge'` (line 9). No namespace `edge` exists:
`k8s/platform/namespaces.yaml` has four, and Traefik's config is `namespace: kube-system`
(`k8s/platform/traefik/helmchartconfig.yaml` line 20; `INFRA_ARCHITECTURE.md` line 53 names
`kube-system/traefik`). `edge` is the name of a Flux Kustomization (`k8s/flux-system/edge.yaml`), which
holds ClusterIssuers and one Certificate. The only reference to the node is the `netcup.**` glob
(`views.c4` line 37), so renaming it to `kube_system` costs one line and G10 would have caught it.
Verified in the repo only; the live cluster was not queried.

## Part 2: How `deployment.c4` should grow

Target tree (ids illustrative; titles must pass G10):

```
deployment {
  github  = provider 'GitHub'                       // repo + Actions
    runner = server 'GitHub-hosted runner (x86_64)' { instanceOf actions }
  hub     = provider 'Docker Hub'                   { instanceOf dockerhub }
  netcup  = server 'Netcup VPS (x86_64)' {          // existing
    k3s = cluster 'k3s (single node)' {
      kube_system   = namespace 'kube-system'    { instanceOf traefik }          // was 'edge'
      flux_system   = namespace 'flux-system'    { instanceOf flux }
      cert_manager  = namespace 'cert-manager'   { instanceOf certmanager }
      monitoring    = namespace 'monitoring'     { instanceOf monitoring }
      prod, nonprod, data                                                           // existing
      architecture  = namespace 'architecture'   { instanceOf archsite }            // new
    }
  }
}
```

Rules that fall out of the existing standard:

- Instances inherit logical relationships, so the physical views draw no edges of their own
  (standard, "Deployment node and Instance"). Every delivery edge (`actions -> dockerhub` push,
  `flux -> dockerhub` tag scan, `flux -> github` poll and bump commit, `actions -> postgres` SSH forward)
  must therefore exist as an authored logical relationship in `model.c4` first.
- Do not grow the `production` deployment view. It is already 1965 px wide and the LikeC4 legibility rule
  is still undefined (`DIAGRAM_CONVENTIONS.md`, "Undefined"). Add `deployment view delivery` and
  `deployment view monitoring`, each tagged exactly `#physical` (the `sync.mjs` rule at lines 296-301
  applies to deployment views too).
- A workload name is the check key (`Deployment/<name>`, no namespace). Name the site Deployment
  `architecture-site`, never `app`, or the existing `Deployment/app` entry silently covers it.
- The `traefik -> <site>` edge is authored like `traefik -> kanban.api` (`model.c4` line 93); G9 upgrades
  its port to checked.

## Part 3: The publish path

### 3.1 Data flow

```
docs/architecture/** pushed to main
   |  .github/workflows/architecture-site.yml   (paths: docs/architecture/**, itself; NOT k8s/**)
   |    1. pnpm install --frozen-lockfile        (pnpm 11.20.0 from package.json, Node 24.19.0 as in invariant-checks.yml)
   |    2. node docs/architecture/sync.mjs check (no publish of a drifted model)
   |    3. docker build (Dockerfile: install, likec4 build, copy to static server)
   |    4. push rudenkovladimir/<site repo>:main-<run_number>-<sha7>          (environment: production, DOCKERHUB_TOKEN)
   v
Docker Hub  <---- ImageRepository (5m) / ImagePolicy numerical on <run> ---- Flux image-reflector
   |                                                                          |
   |                      ImageUpdateAutomation (k8s/overlays, Setters) commits "chore(flux): bump images"
   |                                                                          v
   |                                   k8s/overlays/site/kustomization.yaml  newTag  # {"$imagepolicy": ...}
   |                                                                          |
   +---- image pull <---- Deployment architecture-site (ns architecture) <---- Flux Kustomization `site` (kustomize-controller)
                               ^
          Traefik IngressRoute (websecure, Host(<site host>)) + cert-manager Certificate -> Secret
```

Loop guards that already exist and must not be broken: `deploy.yml` ignores `k8s/**` and `docs/**`
(lines 18-22), so neither the Flux bump commit nor a model edit rebuilds the app image. The new
workflow's `paths` must be `docs/architecture/**` and itself only; never `k8s/**`, or the bump
commit it causes would re-trigger it (the infinite loop `deploy.yml` lines 15-17 describe).

### 3.2 Where the manifests belong

Recommended layout, mirroring the app exactly so no gate and no Flux object needs a new rule:

```
k8s/base/site/kustomization.yaml       resources: [site.yaml]
k8s/base/site/site.yaml                Deployment architecture-site + Service (ClusterIP), resources: {}, image untagged
k8s/overlays/site/kustomization.yaml   namespace: architecture; resources: ../../base/site, ingressroute.yaml, certificate.yaml;
                                       patches: patches.yaml; images: newTag + $imagepolicy setter
k8s/overlays/site/patches.yaml         JSON patch for resources with a dated MEASURED comment
k8s/overlays/site/ingressroute.yaml    Middleware redirect-https, IngressRoute site (websecure), IngressRoute site-http (web)
k8s/overlays/site/certificate.yaml     Certificate, ClusterIssuer letsencrypt-production, secretName <site>-tls
k8s/platform/namespaces.yaml           + Namespace architecture
k8s/flux-system/site.yaml              Kustomization site: path ./k8s/overlays/site, prune, wait, healthChecks Deployment,
                                       dependsOn platform + edge (same shape as apps-nonprod.yaml)
k8s/flux-system/kustomization.yaml     + site.yaml in resources
k8s/flux-system/image-automation.yaml  + ImageRepository, ImagePolicy for the site repo (same filterTags pattern)
```

Why `base/` + `overlays/site/` and not one flat root such as `k8s/site/`:

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A. `base/site` + `overlays/site` (recommended) | Pro: `update.path: ./k8s/overlays` already covers the setter, so `image-automation.yaml`'s `ImageUpdateAutomation` is untouched; the I3/I4/I5 conventions (base `resources: {}`, overlay MEASURED patch, overlay `images:`) are already enforced for exactly this shape. Con: a base with a single overlay, two extra `kustomization.yaml` files. | Zero changes to the automation and the gates; the extra files are 10 lines. |
| B. Flat root `k8s/site/` (like `k8s/data/postgres`, `k8s/monitoring/configs`) | Pro: one fewer directory. Con: the setter marker would sit outside `./k8s/overlays`, so `update.path` must widen to `./k8s` (or a second `ImageUpdateAutomation` be added); two bots then push to `main`. | Rejected: widens a shared automation for a cosmetic saving. |
| C. Serve the site from a ConfigMap built by `configMapGenerator` (as the dashboards are) | Pro: no image, no registry. Con: the built site is about 6 MB on disk with a 2.7 MB `likec4-views.js` (a local build `docs/architecture/out/site-r1`, measured with `du`/`ls` 2026-10-27), and a ConfigMap is capped near 1 MiB; the repo already notes that node-exporter-full alone (470 KB) exceeds kubectl's client-side limit (`k8s/monitoring/configs/kustomization.yaml` lines 12-13). | Rejected on size. |

Namespace: a new `architecture` namespace, not `monitoring` (the `monitoring` Kustomization is the sole
owner of that namespace's routes, invariant I11) and not `kanban-prod` (prod isolation, rate-limit and
NetworkPolicy assumptions). Add it to `k8s/platform/namespaces.yaml`; every apps Kustomization already
`dependsOn: platform`.

Certificate and IngressRoute stay in the overlay, in the workload's namespace, as the app does
(`k8s/overlays/prod/{certificate,ingressroute}.yaml`): Traefik reads `tls.secretName` from the
IngressRoute's own namespace. The monitoring-in-`platform/edge` placement exists only so a hostname
could validate before Grafana existed (`certificate-monitoring.yaml` lines 1-2); no equivalent reason
exists here.

Gate consequences, each read from `scripts/verify-k8s-invariants.py`:

- I3 + I4: the site container needs `requests.memory` and `limits.memory`, and the overlay value needs a
  `MEASURED` comment within 25 lines above; CI runs the gate with `--no-provisional`
  (`invariant-checks.yml` line 124), so a PROVISIONAL guess fails the build. The repo's method is a
  restart ladder (`INFRA_RUNBOOK.md` "Iteration ladder"); a static server needs only a short one. The node
  has about 7945 MiB allocatable with about 3405 MiB requested (`INFRA_RUNBOOK.md` lines 713-715), so
  the pod is not a capacity risk, but it is not exempt.
- I10: the `web` route must be exactly `Host(`<host>`) && !PathPrefix(`/.well-known/acme-challenge/`)` with a
  redirect Middleware. Copy `k8s/overlays/nonprod/ingressroute.yaml` lines 37-40 shape.
- I11: the `site` Kustomization must be the only Flux owner of its objects; it sets none of the
  identity-rewriting fields (`targetNamespace`, `patches`, ...). Keep `sourceRef` = `GitRepository flux-system`.
- I7: new roots are auto-discovered; nothing to list. `verify-k8s-manifests.sh` validates the same kinds
  (`IngressRoute`, `Certificate`, `Middleware`) the app already uses, so no new CRD schema is expected;
  confirm on the first run.
- Edge pod hardening to copy from `base/app/app.yaml`: `automountServiceAccountToken: false`,
  `seccompProfile: RuntimeDefault`, `allowPrivilegeEscalation: false`, `capabilities.drop: [ALL]`, probes
  on the server's own port.

### 3.3 Image build and the tag bump

Build: `docs/architecture/Dockerfile`, next to the model it serves (the precedent is
`k8s/data/postgres/backup/Dockerfile` beside its script). A `docs/**` path is already ignored by
`deploy.yml`, so it never rebuilds the app. Stages: pinned Node 24.19.0 base (the version CI and the
model were verified with, `invariant-checks.yml` line 157), `pnpm install --frozen-lockfile`, `likec4 build`,
then a static-server stage that runs as non-root with a read-only root filesystem. Pin base images by
digest, as the backup image does (its `FROM alpine:3.22@sha256:...`), not by tag as the app's `Dockerfile` does.

Server choice, SPA fallback and hash-history are STACK-level decisions. Integration facts that constrain them:

- `likec4 build` has `--base`, `--use-hash-history`, `--title`, `--output-single-file`, `--public`
  (`likec4 build --help`, local 1.60.0). The built `404.html` is byte-identical to `index.html`
  (`cmp`, 2026-10-27), so a server that serves `404.html` for unknown paths already gives deep links
  without extra routing, at the cost of a 404 status.
- A probe by another researcher ran `joseluisq/static-web-server:2.44.1` read-only, non-root, 16 MiB cap
  against that build (`docs/architecture/out/probe-sws.fish`); its result is not in this file and was not
  re-run here.
- Keep build flags in one place: add a `package.json` script that both `likec4.fish site` (line 30) and
  the Dockerfile call. Today `likec4.fish site` hard-codes `likec4 build -o out/site model`; a second copy
  in a Dockerfile is a drift source nothing checks.

Tag and bump: copy the app's scheme, not the backup image's.

| Scheme | Used by | Fit for the site |
|---|---|---|
| `main-<run_number>-<sha7>` + `ImagePolicy` numerical + Setters | app and nonprod (`deploy.yml` lines 109-116, `image-automation.yaml`) | Chosen: every model merge should roll out with no human step. `run_number` is per workflow, so the new workflow starts its own counter and needs its own `ImageRepository`/`ImagePolicy`. |
| `<base>-<sha7>@sha256:<digest>` pinned by hand into the manifest | backup image (`backup-image.yml`, `backup-cronjob.yaml` line 58) | Rejected: it needs a manual manifest edit per rebuild, tolerable for an image that changes rarely, not for a site that changes with the model. |

Bootstrap order matters: the tag in `k8s/overlays/site/kustomization.yaml` must name an image that
exists, so the workflow is merged and run once (manual `workflow_dispatch`) before the manifests land. The
app followed the same order. The new Docker Hub repository must be public: the cluster has no
`imagePullSecrets` anywhere (`k8s/base/app/app.yaml`), and the cleanup job's comment relies on public-repo
tag listing (`deploy.yml` lines 314-316). Whether Docker Hub makes an auto-created repository public by
default is UNVERIFIED; the operator should create it explicitly.

Workflow conventions to copy from `backup-image.yml`, the newer of the two: `permissions: contents: read`,
`environment: production` only because that is where `DOCKERHUB_TOKEN` lives, token through env and stdin only,
actions pinned by digest, `timeout-minutes`, single platform `linux/amd64`. Add a PR-time job to
`invariant-checks.yml` that builds the image with `push: false` and no secrets: that file's header
explains why a gate living only in a push-to-main workflow fires too late (lines 4-9).

Tag retention: the app has two near-identical 50-line prune jobs (`cleanup-old-images*`); a third copy is
not recommended for a few-MB image. No prune is proposed; Docker Hub retention for a free public repository
is UNVERIFIED.

### 3.4 Edge, DNS, certificate

- Hostname: the repo uses DuckDNS subdomains, A record `159.195.114.230`, no updater running
  (`INFRA_RUNBOOK.md` "DNS - DuckDNS" lines 473-480). A new subdomain is an operator action outside the repo,
  and it must resolve before the `Certificate` is applied, or HTTP-01 fails and cert-manager retries
  against Let's Encrypt. Per-account DuckDNS subdomain limits are UNVERIFIED (the FAQ page states none).
  Three subdomains exist today (prod, nonprod, monitoring).
- Rate limiting: prod attaches `general-rate-limit` (60/min, burst 60) to its host route and nonprod
  attaches none (`overlays/prod/ingressroute.yaml` lines 23-40). A first page load fetches roughly 17 asset
  files from `assets/`, so a copy of the prod middleware will not trip on one visit. Judgement call, not a
  gate requirement.
- Uptime probe: add the site URL to `HEALTH_URLS` in `uptime-check.yml`. Its comment derives
  `timeout-minutes: 8` from "4 URLs x 3 attempts x 20s plus 8 sleeps of 10s = 320s; the drill's fifth URL
  makes it 400s" (lines 57-59). One more URL makes the drill case 6 URLs = 480 s = exactly the 8-minute
  limit, so the limit must rise or the drill times out. The runbook's "Uptime alert triage" lists the probed
  URLs and needs the same edit.
- Probe path: a static server has no `/api/healthz`; probe `/` and assert 200, since a route with no ready
  backend answers 503 (`uptime-check.yml` lines 12-13 make this point for Grafana).

## Part 4: Patterns to follow

### Pattern 1: Authored shell, checked metadata, generated children
**What:** the parent element is authored; every fact that exists in code is either a generated child or a
metadata key whose value `check()` compares. **When:** any new system (`flux`, `monitoring`, `actions`).
**Trade-off:** one more metadata key per fact; the DSL cannot declare allowed keys (V2), so typos in new
keys pass silently. Build the proposed unknown-metadata-key check first (Build order, phase A).

```js
// shape of a new set check, modelled on workloads() and the check at sync.mjs lines 312-314
const modelRepos = new Set(elements.flatMap((e) => [e.getMetadata('dockerRepo')].flat().filter(Boolean)))
for (const r of fluxScannedRepos()) if (!pushedRepos().has(r)) problems.push(`Flux scans ${r}, no workflow pushes it`)
```

### Pattern 2: Every new check is proven red then green
The standard requires it, and `drill.fish` is the mechanism: each drill edits a scratch copy and expects
exit 1; D5 is the negative control. Each BUILD row in 1.1 gets a numbered drill (D10 onward) seen NOT caught
before the rule exists, then caught after. The drills' `fresh()` must copy the new source directories or the
new reader runs against nothing and reports clean (an empty result and a clean result look identical).

### Pattern 3: One reader, two consumers
The Flux Kustomization list, HelmRelease list and workflow job list are read once and used by both
`render()` (G1, G3) and `check()` (G2, G5, G10, G11). Do not write a second YAML walker per check.

### Pattern 4: Gate the publish on the same check CI already runs
The site workflow runs `sync.mjs check` before building, so the published site is never built from a
drifted model, and a Java-only drift (which the hook does not trigger on, `DIAGRAM_CONVENTIONS.md`
"Wired") blocks the next model publish instead of being published.

### Pattern 5: Generalise the gate, do not special-case the new image
`check_i5_overlay_kustomization` only inspects images whose name contains `kanban-board-backend` (line 538).
A site image with another name passes without a setter marker, which is the thing that makes Flux bump it.
Change the rule to "every overlay `images[].newTag` must match `main-N-sha7` and the file must carry a
setter marker". The population today is the two app entries; the site adds a third. Add a selftest case:
red with the marker removed, green restored.

## Anti-Patterns

### Anti-Pattern 1: Model facts derived from files Flux rewrites
**What people do:** put the running image tag in element metadata so the model "shows what is deployed".
**Why it's wrong:** `image-automation.yaml` commits tag bumps to `k8s/overlays/**` on `main`; the drift job goes
red after each, and the bot cannot run `generate`. **Instead:** tags never enter the model (N1).

### Anti-Pattern 2: One giant `production` deployment view
**Why it's wrong:** 1965 px wide before monitoring and delivery are added; the legibility rule is undefined.
**Instead:** one deployment view per concern (`production`, `delivery`, `monitoring`).

### Anti-Pattern 3: Porting the Mermaid labels verbatim
`delivery-nodes.mmd` has "poll 1m"; `monitoring-nodes.mmd` has "30-day logs". Both are numbers the standard
forbids in labels unless a check covers them. Port the structure, drop or generate the numbers.

### Anti-Pattern 4: Parsing YAML with regexes for the new sources
The existing `workloads()` regex already fails on Flux's label-first export. Helm values embedded as a
string (`alloy.yaml` `configMap.content`, `helmchartconfig.yaml` `valuesContent`) need a real parser for the
outer document and a deliberate regex only for the inner string.

### Anti-Pattern 5: Publishing from the workflow that also holds the secrets, without a PR-time build
A Dockerfile typo then surfaces after merge. Build on PR without secrets; push only on `main`.

## Scaling Considerations

Not a user-scale problem. The relevant growth axes:

| Concern | Today | After v1.5 | Action |
|---|---|---|---|
| `sync.mjs check` wall time | about 1 s (median 958 ms per the standard) | adds a YAML parse of about 40 files | Measure after phase 3 so the hook stays cheap; the standard records the 1 s figure as the reason `information_schema` was not wired in. |
| Hook scope | 4 trigger path groups | plus `.github/workflows`, `k8s/flux-system`, `k8s/overlays`, `k8s/monitoring`, `k8s/platform` | k8s manifest commits are already the 4-minute "feature-heavy" kind per project `CLAUDE.md`, so Node is the only new prerequisite there. A workflow-only commit may use `--no-verify` (config-only), so CI's `likec4-model-drift` job is the backstop. Flux bot commits never run the hook. |
| View legibility | `development` 4475 x 2636 | more views | Define the LikeC4 legibility rule (currently undefined) before the delivery and monitoring views are accepted. |
| Cluster memory | about 43% requested | +1 small pod | Measured request, not a guess (I4). |

## Integration Points

### External services

| Service | Integration | Notes |
|---|---|---|
| Docker Hub | push from runner, tag scan and pull from cluster | New public repo; `DOCKERHUB_TOKEN` is in the `production` environment only. |
| GitHub | Flux polls `ssh://git@github.com/RudVlad473/kanban-board-backend` every 1m and commits bumps (`gotk-sync.yaml`) | Flux bump commits must stay outside the new workflow's `paths`. |
| DuckDNS | manual subdomain, A record to the VM | Outside the repo. |
| Let's Encrypt | HTTP-01 through the existing `ClusterIssuer letsencrypt-production` | I10 protects the challenge path on the `web` route. |

### Internal boundaries

| Boundary | Communication | Notes |
|---|---|---|
| `sync.mjs` <-> `k8s/` and `.github/workflows/` | file reads | New; both directories currently unread except `k8s/base`, `k8s/data`. |
| site workflow <-> `sync.mjs` | `node docs/architecture/sync.mjs check` as a step | Reuses the CI job's Node and pnpm setup. |
| `deployment.c4` <-> `namespaces.yaml` | G10 check | New. |
| site manifests <-> model | the `workloads()` scan of `k8s/base` fails until `Deployment/architecture-site` has a model element | This is a feature: the base manifest and its model element must land in one commit. |
| `likec4.fish site` <-> Dockerfile | one `package.json` script | New; avoids two copies of the build flags. |

## Suggested Build Order

Two tracks. Publish (P) is deliberately first: it makes every later model change flow through a finished
pipeline and gives a live URL to verify against. The delivery-path model (M-B) comes after P because it
must model the site pipeline as built, not as `#planned`.

| Phase | Track | Content | Depends on | Files touched |
|---|---|---|---|---|
| 1 | P | Dockerfile, `.dockerignore`, `package.json` build script, `architecture-site.yml`, PR-time build job. Operator: create the public Docker Hub repo and the DuckDNS subdomain. Prove red then green: a drifted model must fail the workflow before push. First run by `workflow_dispatch`. | none | `docs/architecture/`, `.github/workflows/` |
| 2 | P | Namespace, `base/site`, `overlays/site`, Flux `site` Kustomization, `ImageRepository`/`ImagePolicy`, I5 generalisation + selftest, MEASURED memory ladder, uptime probe URL + `timeout-minutes`, and the one model element + instance + `traefik ->` edge the workload check forces. Verify by merging a model edit and watching the tag bump, rollout, and a changed page; verify a deletion by removing a view and confirming it disappears. | 1 | `k8s/`, `scripts/verify-k8s-invariants*.py`, `docs/architecture/model/`, `uptime-check.yml`, docs |
| 3 | M-A | Foundations: add `yaml` dependency + lockfile; split `render()`; build the unknown-metadata-key check; extend drill `fresh()`, hook triggers and the hook self-test; amend the standard (`depends`, `provider`, Physical row); fix `edge` -> `kube-system`; G10 namespace check; G11 placement check. | none (parallel to 2, but both edit `deployment.c4`; run sequentially) | `sync.mjs`, `drill.fish`, `.githooks/pre-commit`, `scripts/pre-commit-likec4-selftest.py`, `model/`, standard |
| 4 | M-B | Delivery path: `github`, `actions`, `dockerhub`, `flux` systems; G1 jobs + `depends`, G2 repo set, G3 Kustomizations, G4 Flux controllers; `delivery` deployment view; scenarios `push-to-image`, `image-to-rollout`, `publish-architecture-site`. | 2, 3 | `sync.mjs`, `model/`, drills |
| 5 | M-C | Monitoring: `monitoring` system with authored Prometheus, Grafana, Loki, Alloy, node-exporter, kube-state-metrics, postgres-exporter; G5 HelmRelease check, G7 datasources, G9 hosts; optional G6, G8; `monitoring` deployment view. | 3 (4 for the namespace nodes) | `sync.mjs`, `model/`, drills |
| 6 | M-D | More scenarios and the migration end: `signup`, `error-status-split` (one view per outcome), `inbound-packet-path`, `ingress-to-pod`, nightly backup, uptime probe; optional G12-G14; then retire the Mermaid sources, `render-diagrams.sh`, `verify-diagrams.py`, `render-manifest.tsv` (11 entries today), repoint `INFRA_ARCHITECTURE.md` and the README embed, update the Maintenance Note file list. | 4, 5 | docs, scripts |

Parallelism rule applied (global `CLAUDE.md`): phases 2 and 3 both edit `deployment.c4` and the standard;
phases 4, 5 both edit `sync.mjs` and `deployment.c4`. They stay sequential. Phase 1 is disjoint from
phase 3 and can run beside it in a separate worktree.

## Pitfalls specific to integration

- **P1, the existing `edge` namespace is wrong** (section 1.4). Fixing it after more nodes hang off it is
  more edits.
- **P2, Flux rewrites `k8s/overlays/**`** (Anti-Pattern 1). Any reader that touches overlays must skip `images:`.
- **P3, the workload check keys on kind and name only** (`sync.mjs` lines 217-234): the name must be unique
  across `k8s/base` and `k8s/data`, and it ignores namespace, so it cannot tell a wrong placement. G11 fixes the latter.
- **P4, drills that read nothing**: `fresh()` does not copy `.github`, `k8s/flux-system`, `k8s/overlays`,
  `k8s/monitoring`. A new reader against a missing directory must fail loudly (non-zero), not return an empty set.
- **P5, hook trigger list vs CI**: a source outside the trigger list is caught in CI only
  (`DIAGRAM_CONVENTIONS.md` "Wired"). Extend both the list and its self-test in the same change as each reader.
- **P6, the uptime probe's timeout arithmetic** (section 3.4).
- **P7, Helm-rendered workloads are invisible**: the monitoring stack's real pod and container names cannot
  be generated; do not claim they are. The runbook's "34 containers across 7 namespaces" snapshot
  (`INFRA_ARCHITECTURE.md` line 119) goes to 8 namespaces; update it in the same change.
- **P8, public content**: the site publishes element descriptions, including the `KANBAN-INGRESS` filter and
  the Netcup firewall layering. The repository appears public (`uptime-check.yml` line 26 reasons about "a
  public repository"), so this adds discoverability, not new disclosure; confirm that before shipping.
- **P9, `likec4 build` in Docker is unproven here**: native-binary pnpm packages (the workspace sets
  `allowBuilds: esbuild: false`, `pnpm-workspace.yaml`) on a minimal image were not exercised. Prefer a glibc
  Node image first; test the build in phase 1 before writing the manifests.

## Open items (UNVERIFIED or deferred)

- Whether LikeC4 1.60 supports `metadata` on relationships (affects G7 shape).
- Docker Hub: default visibility of an auto-created repo; tag retention on a free public repo.
- DuckDNS: number of subdomains per account.
- `likec4 build` flags the site should use (`--use-hash-history`, `--title`, `--theme`) and the static
  server: STACK.md.
- The LikeC4 legibility rule (already "undefined" in the standard) gates acceptance of the new views.
- Live cluster state was not queried; everything about the cluster is read from committed manifests and docs.
- No new check proposed here has been built or run; the baseline `sync.mjs check` run is the only execution.

## Sources

- `/home/andre/dev/kanban-board-backend/docs/DIAGRAM_CONVENTIONS.md` "## LikeC4 standard" (lines 46-278)
- `/home/andre/dev/kanban-board-backend/docs/architecture/{sync.mjs,drill.fish,likec4.fish,package.json,pnpm-workspace.yaml,model/*.c4}`
- `/home/andre/dev/kanban-board-backend/k8s/**` (flux-system, platform, base, overlays, monitoring, data)
- `/home/andre/dev/kanban-board-backend/.github/workflows/{deploy,backup-image,invariant-checks,uptime-check}.yml`
- `/home/andre/dev/kanban-board-backend/scripts/verify-k8s-invariants.py` (I1-I11)
- `/home/andre/dev/kanban-board-backend/.githooks/pre-commit` lines 198-230
- `/home/andre/dev/kanban-board-backend/docs/INFRA_ARCHITECTURE.md`, `docs/INFRA_RUNBOOK.md`
- `/home/andre/dev/kanban-board-backend/docs/diagrams/**` (the Mermaid sources being migrated)
- Local `likec4 build --help` (1.60.0) and a local `likec4 build` output in `docs/architecture/out/site-r1`
- `npm view yaml version` (2.9.1), https://www.duckdns.org/faqs.jsp (no subdomain limit stated)

---
*Architecture research for: LikeC4 model growth and static-site publishing on k3s + Flux*
*Researched: 2026-10-27*
