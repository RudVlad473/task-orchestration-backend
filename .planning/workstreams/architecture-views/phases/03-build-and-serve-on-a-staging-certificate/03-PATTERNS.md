# Phase 3: Build and Serve on a Staging Certificate - Pattern Map

**Mapped:** 2026-10-28
**Files analyzed:** 22 new or modified
**Analogs found:** 20 / 22 (all analog paths confirmed git-tracked via `git ls-files`)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `.github/workflows/architecture-site.yml` | CI workflow | batch (build, publish) | `.github/workflows/backup-image.yml` (publish) + `invariant-checks.yml:138-167` (build steps) | role-match |
| `docs/architecture/site/Dockerfile` | config | file-I/O | none in repo for a static image; use RESEARCH "Code Examples" (backup Dockerfile `k8s/data/postgres/backup/Dockerfile` for comment style only) | partial |
| `docs/architecture/site/nginx.conf` | config | request-response | none; use RESEARCH final conf | none |
| `k8s/base/architecture-site/architecture-site.yaml` | manifest | request-response | `k8s/base/app/app.yaml` | exact |
| `k8s/base/architecture-site/kustomization.yaml` | config | n/a | `k8s/base/app/kustomization.yaml` | exact |
| `k8s/overlays/architecture/kustomization.yaml` | config | n/a | `k8s/overlays/nonprod/kustomization.yaml` | exact |
| `k8s/overlays/architecture/ingressroute.yaml` | route | request-response | `k8s/overlays/nonprod/ingressroute.yaml` | exact |
| `k8s/overlays/architecture/certificate.yaml` | manifest | event-driven | `k8s/overlays/nonprod/certificate.yaml` | exact |
| `k8s/platform/namespaces.yaml` (+Namespace) | manifest | n/a | same file | exact |
| `k8s/platform/edge/clusterissuers.yaml` (+serviceType) | manifest | n/a | same file | exact |
| `k8s/flux-system/apps-architecture.yaml` | Flux Kustomization | event-driven | `k8s/flux-system/apps-nonprod.yaml` | exact |
| `k8s/flux-system/kustomization.yaml` (+resource) | config | n/a | same file | exact |
| `k8s/flux-system/image-automation.yaml` (+repo, policy) | Flux image | event-driven | same file, nonprod pair | exact |
| `scripts/verify-k8s-invariants.py` (+I12) | gate | transform | `check_i4_source_file` (483-519), `check_i5_overlay_kustomization` (522-550) | exact |
| `scripts/verify-k8s-invariants-selftest.py` (+I12 cases) | test | transform | `run_cases` (166-) | exact |
| `docs/architecture/model/delivery.c4` (+site element, reword ci/dockerhub, edges) | model | n/a | same file | exact |
| `docs/architecture/model/model.c4` (+host in Traefik list) | model | n/a | same file lines 10-14 | exact |
| `docs/architecture/model/deployment.c4` (+namespace node) | model | n/a | same file lines 13-35 | exact |
| `docs/architecture/model/views.c4` (+membership) | model | n/a | same file | exact |
| `docs/architecture/model/scenarios-platform.c4` (+publish-the-site) | model | event-driven | `push-to-image`, `image-to-rollout` (lines 2-27) | exact |
| `03-SCENARIO-EVIDENCE.md` | doc | n/a | Phase 2 `02-SCENARIO-EVIDENCE.md` | exact |
| docs (`DIAGRAM_CONVENTIONS.md`, `INFRA_ARCHITECTURE.md`, `INFRA_RUNBOOK.md`) | doc | n/a | same files | exact |

## Pattern Assignments

### `.github/workflows/architecture-site.yml` (CI, batch)

**Analogs:** `backup-image.yml` for the publish job, `invariant-checks.yml:143-167` for the build job.

Header comment style (summary, then `Decisions:` marker; no planning ids, comment lint applies) - `backup-image.yml:1-13`.

Publish job pattern (`backup-image.yml:25-61`): SHA-pinned actions, token through `env:` and stdin only, `timeout-minutes`, `environment: production` only on the publish job.
```yaml
permissions:
  contents: read
env:
  DOCKERHUB_USER: rudenkovladimir
jobs:
  build-and-push:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    environment: production
    steps:
      - uses: actions/checkout@fbc6f3992d24b796d5a048ff273f7fcc4a7b6c09 # v5
      - uses: docker/setup-buildx-action@8d2750c68a42422c14e847fe6c8ac0403b4cbd6f # v3
      - name: Log in to Docker Hub
        env:
          DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
        run: echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
      - uses: docker/build-push-action@10e90e3645eae34f1e60eeb005ba3a3d33f178e8 # v6
        with:
          platforms: linux/amd64
          push: true
          tags: ${{ env.DOCKERHUB_USER }}/${{ env.DOCKERHUB_REPOSITORY }}:${{ steps.tag.outputs.image_tag }}
```
Tag step: copy line 49-51 shape, but `main-${{ github.run_number }}-${GITHUB_SHA::7}`.

Build job pattern (`invariant-checks.yml:143-167`), no environment, no secrets:
```yaml
- uses: actions/checkout@v5
- uses: pnpm/action-setup@v6
  with: { package_json_file: docs/architecture/package.json }
- uses: actions/setup-node@v5
  with: { node-version: '24.19.0' }
- run: pnpm --dir docs/architecture install --frozen-lockfile
- run: node docs/architecture/sync.mjs check
```
Then `pnpm --dir docs/architecture exec likec4 build --use-dot=false -o site/out model`, `docker build` with `push: false`, `upload-artifact@v4`. Publish job: `needs: build`, `if: github.event_name != 'pull_request' && github.ref == 'refs/heads/main'`, `download-artifact`. Paths list written twice (push and pull_request), per RESEARCH. Never put `likec4 start` outside a comment (I12).

### `k8s/base/architecture-site/architecture-site.yaml` (manifest)

**Analog:** `k8s/base/app/app.yaml`. Copy:
- Header summary comment (line 1), `replicas: 1`, `strategy: Recreate` with its comment (15-19), selector/labels `app.kubernetes.io/name` (20-26), `automountServiceAccountToken: false` + `seccompProfile: RuntimeDefault` (28-32), container securityContext (83-87), untagged image name (60), `Service` with `type: ClusterIP`, port `http`, `targetPort: http` (110-125).
- Differences: name `architecture-site`, port 8080 named `http`, add `runAsNonRoot: true`/`runAsUser: 101`, `readOnlyRootFilesystem: true`, `/tmp` emptyDir, probes on `/healthz`. Unlike app (resources `{}` patched by overlay), put resources directly in the base with a MEASURED comment within 25 lines above each `memory:` line (I4, `check_i4_source_file`).

### `k8s/base/architecture-site/kustomization.yaml`
**Analog:** `k8s/base/app/kustomization.yaml` (whole file, 4 lines): `resources: [architecture-site.yaml]`.

### `k8s/overlays/architecture/kustomization.yaml`
**Analog:** `k8s/overlays/nonprod/kustomization.yaml:1-23`.
```yaml
namespace: architecture
resources:
  - ../../base/architecture-site
  - ingressroute.yaml
  - certificate.yaml
images:
  - name: rudenkovladimir/kanban-board-backend-architecture
    newName: rudenkovladimir/kanban-board-backend-architecture
    newTag: main-<run>-<sha7> # {"$imagepolicy": "flux-system:kanban-board-backend-architecture:tag"}
```
No `patches` (no patches file). I5 requires newTag fullmatch `main-\d+-[0-9a-f]{7}` and the marker (verify script 522-550).

### `k8s/overlays/architecture/ingressroute.yaml`
**Analog:** `k8s/overlays/nonprod/ingressroute.yaml` (whole file). Keep the `Middleware redirect-https` (lines 2-9), `-https` route on `websecure` with `tls.secretName`, and the `-http` route whose match is exactly ``Host(`<host>`) && !PathPrefix(`/.well-known/acme-challenge/`)`` (line 37, I10) with the `redirect-https` middleware. Rename routes `architecture-https`/`architecture-http`, service `architecture-site` port 8080, secret `architecture-tls`.

### `k8s/overlays/architecture/certificate.yaml`
**Analog:** `k8s/overlays/nonprod/certificate.yaml`. Change name `architecture`, secret `architecture-tls`, dnsNames the architecture host, and `issuerRef.name: letsencrypt-staging`. Fix the header comment (nonprod's says "Production certificate").

### `k8s/flux-system/apps-architecture.yaml`
**Analog:** `apps-nonprod.yaml:1-30`. Keep header comment style on `dependsOn`, `interval: 10m`, `prune: true`, `wait: true`, `suspend: false`, `sourceRef GitRepository flux-system`. `dependsOn`: `platform`, `edge` (drop `data`). Omit `healthChecks` (ignored when `wait: true`). Set none of targetNamespace/namePrefix/nameSuffix/patches/components/postBuild (I11).

### `k8s/flux-system/image-automation.yaml` additions
**Analog:** same file lines 12-20 (ImageRepository) and 37-50 (ImagePolicy), names `kanban-board-backend-architecture`; `ImageUpdateAutomation` (52-80) unchanged (`update.path: ./k8s/overlays`). Add `apps-architecture.yaml` to `k8s/flux-system/kustomization.yaml` resources (lines 3-14).

### `k8s/platform/namespaces.yaml` and `clusterissuers.yaml`
Append `apiVersion: v1 / kind: Namespace / metadata: name: architecture` after the `monitoring` block, with `---` separators; update the header comment's namespace list. ClusterIssuers: add `serviceType: ClusterIP` under `http01.ingress` (siblings of `ingressClassName: traefik`) in both issuers, as a separate commit.

### `scripts/verify-k8s-invariants.py` I12
**Analog:** `check_i4_source_file(path, text, ...)` (483-519): pure function of `(path, text)` returning a list of `"I12: {path}:{line}: ..."` strings, scanning `text.splitlines()`; I5-overlay (522-550) for the `[]`-on-parse-miss style. Wire it into `main()` (573-) next to the I4/I5 file loops (over `k8s/**/*.y?ml`, `Dockerfile*`, `.github/workflows/*.yml`). Strip full-line comments and trailing ` #...` before matching `likec4\s+(start|preview|serve)`, `\bvite\s+preview\b`, `--listen`. Add a docstring scope line and a "Known holes" line.

### `scripts/verify-k8s-invariants-selftest.py`
**Analog:** `run_cases` (166-): use the local `expect_violation(label, violations, "I12:")` / `expect_clean(label, violations)` closures and call `_gate.check_i12_no_dev_server(path, text)` with literal strings (planted line violates; same line inside a comment is clean).

### Model files
- `model.c4:10-14`: append the architecture host to Traefik `host [...]` list (two-way equality with IngressRoute hosts).
- `deployment.c4:13-35`: add `architecture = namespace 'architecture' { instanceOf <site> }` beside siblings under `netcup.k3s`.
- `delivery.c4:6-18`: reword `ci` description (line 7 says k8s/ and docs changes do not start it, true of deploy.yml only) and `dockerhub` description (line 14). Site element copies `flux` (20-32) metadata shape but with `workload 'Deployment/architecture-site'` and `source 'k8s/base/architecture-site/architecture-site.yaml'` as single strings. Relationship style (34-39): `traefik -[sync]-> site 'HTTP :8080'` in model.c4 next to line 141; no second `ci -> dockerhub` logical relationship.
- `scenarios-platform.c4:2-13` is the scenario template:
```
dynamic view publish-the-site {
  #scenarios
  variant sequence
  title 'Scenario: ...'
  description '...'
  repo -> ci '...'
  ci -> dockerhub '...'
```
At most 5 participants: repo, ci, dockerhub, flux, site.

## Shared Patterns

- **Comment lint** (`scripts/verify-comments.py`, covers yml/yaml/toml/Dockerfile/.conf/.sh/.py): plain text, a blank comment line between paragraphs, `Decisions:` marker for long narration, no planning ids. Every new file above is linted. Model: `backup-image.yml:1-13`, `app.yaml:1-6`.
- **Memory MEASURED comment** (I4): within 25 lines above each `memory:` line, from the ladder results.
- **Atomic commit**: base Deployment, IngressRoute host, Namespace, and the matching model element, host claim, namespace node and edge land in one commit or `sync.mjs check` goes red.
- **Setter marker**: `# {"$imagepolicy": "flux-system:<policy-name>:tag"}` on the `newTag` line (`nonprod/kustomization.yaml:23`).

## No Analog Found

| File | Role | Reason |
|---|---|---|
| `docs/architecture/site/nginx.conf` | config | No nginx config in repo; use RESEARCH final conf (measured). |
| `docs/architecture/site/Dockerfile` | config | No static-site image; use RESEARCH Dockerfile (three lines, digest pinned). |

## Metadata

**Analog search scope:** `.github/workflows/`, `k8s/base`, `k8s/overlays`, `k8s/flux-system`, `k8s/platform`, `scripts/verify-k8s-invariants*.py`, `docs/architecture/model/`
**Files read:** about 14; line numbers taken from this session's reads.
**Pattern extraction date:** 2026-10-28
