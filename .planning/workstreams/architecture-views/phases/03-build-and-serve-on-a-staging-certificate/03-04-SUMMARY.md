---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 04
subsystem: infra
tags: [k8s, kustomize, flux, traefik, cert-manager, likec4, sync-check]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: plan 02's published image main-1-5d44dac and plan 03's adopted memory values 16Mi request and 32Mi limit
provides:
  - namespace architecture, base/architecture-site and overlays/architecture rendering Deployment, Service, two IngressRoutes, Middleware and a staging Certificate
  - Flux Kustomization apps-architecture and the ImageRepository and ImagePolicy kanban-board-backend-architecture
  - model element archsite with its traefik, flux and dockerhub edges, the host claim, the namespace node and instance, drawn in production and delivery
  - the Maintenance Note's Kustomize roots naming base/architecture-site and overlays/{prod,nonprod,architecture}
  - recorded I5 red then green on the overlay and the sync check red on exactly the site's three facts
affects: [03-05, 03-06, 03-07]

actuals:
  tokens: 4900
  tasks: 2
  commits: 1
plan_head_before: 18bb910a5f76d15fae796b78715eeed54229c7ad
plan_head_after: 8e531d020293d55a1a52c6be17708dca3afd3ede

tech-stack:
  added: []
  patterns:
    - "one atomic commit holding manifests, Flux wiring and model claims, because sync.mjs checks workload, host and namespace in both directions"
    - "a single-instance namespace is drawn as a view node only when the view includes the namespace itself, not just its .** descendants"

key-files:
  created:
    - k8s/base/architecture-site/kustomization.yaml
    - k8s/base/architecture-site/architecture-site.yaml
    - k8s/overlays/architecture/kustomization.yaml
    - k8s/overlays/architecture/ingressroute.yaml
    - k8s/overlays/architecture/certificate.yaml
    - k8s/flux-system/apps-architecture.yaml
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-04-i5-red.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-04-i5-green.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-04-model-red.txt
  modified:
    - k8s/platform/namespaces.yaml
    - k8s/flux-system/kustomization.yaml
    - k8s/flux-system/image-automation.yaml
    - docs/architecture/model/delivery.c4
    - docs/architecture/model/model.c4
    - docs/architecture/model/deployment.c4
    - docs/architecture/model/views.c4
    - docs/INFRA_ARCHITECTURE.md

key-decisions:
  - "One 17-path commit through the full pre-commit hook: every two-way sync.mjs check is green at the commit, and the hook never needed --no-verify"
  - "The production view includes netcup.k3s.architecture explicitly, so the namespace node is drawn"
  - "Staging certificate only; the flip to letsencrypt-production stays a later, separate commit after the leak review"

requirements-completed: []  # PUB-02 stays Pending until plan 06 serves the site; SCEN-08's scenario view is plan 07

coverage:
  - id: D1
    description: "The site's overlay renders in namespace architecture and passes I1 to I12 and kubeconform; I5 fails on the overlay without its setter marker and passes with it"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-04-i5-red.txt (GATE exit=1, one FAIL: I5 line naming the overlay) and 03-04-i5-green.txt (GATE exit=0); verify-k8s-manifests.sh all 14 roots valid"
        status: pass
    human_judgment: false
  - id: D2
    description: "The sync check fails on exactly the site's workload, host and namespace before the model claims them, and passes after"
    requirement: "SCEN-08"
    verification:
      - kind: integration
        ref: "03-04-model-red.txt (exit=1, three problems); node docs/architecture/sync.mjs check exits 0 in the commit"
        status: pass
    human_judgment: false
  - id: D3
    description: "The export draws the site in production (namespace node, instance, traefik edge HTTP :8080) and delivery, with no merged labels"
    requirement: "SCEN-08"
    verification:
      - kind: integration
        ref: "export checks: production nodes netcup.k3s.architecture and netcup.k3s.architecture.archsite, edge label HTTP :8080, delivery holds archsite, physical views delivery,monitoring,production with 0 merged labels"
        status: pass
    human_judgment: false

duration: about 40min
completed: 2026-10-28
status: complete
---

# Phase 3 Plan 04: Serve manifests, Flux wiring and the model's claim of the site Summary

**The architecture site's Deployment, Service, routes and staging Certificate are written, owned by one Flux Kustomization, claimed by the model and drawn in the production and delivery views, all in one hook-checked local commit `8e531d0`; nothing is pushed.**

## Performance

- **Duration:** about 40 min (the plan's start time was not recorded, so this is an estimate)
- **Completed:** 2026-10-28
- **Tasks:** 2 of 2
- **Files modified:** 17 in one commit (14 source files, 3 records)

## Accomplishments

- Namespace `architecture` in `k8s/platform/namespaces.yaml`; `k8s/base/architecture-site` holds Deployment and Service `architecture-site` (nginx on 8080 as user 101, read-only root, `/tmp` emptyDir, all capabilities dropped, no service-account token, probes on `/healthz`).
- MEASURED values: `requests.memory: 16Mi`, `limits.memory: 32Mi`, copied from 03-03-ladder.txt's `ADOPTED: requests.memory=16Mi limits.memory=32Mi` under a comment dated 2026-10-28 on the VPS (rungs 64, 32, 16 pass; 8Mi OOM-killed).
- The overlay sets namespace `architecture`, the tag `main-1-5d44dac` (the only `main-` tag on Docker Hub) with the setter marker for `flux-system:kanban-board-backend-architecture:tag`, the Middleware, the routes `architecture-https` and `architecture-http` (the exact I10 match form) and Certificate `architecture` on `letsencrypt-staging`.
- `k8s/flux-system` gains Kustomization `apps-architecture` (path `./k8s/overlays/architecture`, wait true, dependsOn `platform` and `edge`, none of the I11 identity-rewriting fields) and the ImageRepository and ImagePolicy `kanban-board-backend-architecture`.
- Recorded red lines:
  - I5: `GATE exit=1`, `FAIL: I5: k8s/overlays/architecture/kustomization.yaml: image 'rudenkovladimir/kanban-board-backend-architecture' carries no Flux $imagepolicy setter-marker comment`; green: `GATE exit=0`.
  - Model: `exit=1` with `k8s Deployment/architecture-site has no element carrying metadata workload 'Deployment/architecture-site'`, `k8s IngressRoute routes host kanban-board-rud-vlad-473-architecture.duckdns.org, which no element claims in metadata host`, and `namespace architecture is defined in k8s/overlays/architecture/kustomization.yaml but has no namespace node in the deployment model`.
- Model: element `archsite` (workload and source metadata), edges `traefik -> archsite 'HTTP :8080'`, `flux -> archsite 'apply the architecture overlay'`, `archsite -> dockerhub 'image pull'`, the host in traefik's list, namespace node `architecture` with `instanceOf archsite`, includes in `production` and `delivery`, and the re-worded ci and dockerhub descriptions and `repo -> ci` and `ci -> dockerhub` titles. `sync.mjs check` reports `OK (62 elements, ..., 18 workloads)`.
- Production view node count is 26 (it was 25 before the explicit include, 24 before this plan: archsite, plus the namespace node `netcup.k3s.architecture`, are new). Its architecture nodes are `netcup.k3s.architecture` and `netcup.k3s.architecture.archsite`; the delivery view holds `netcup.k3s.architecture.archsite`; the physical views print `delivery,monitoring,production 0`; the leak-words check prints nothing.
- `docs/INFRA_ARCHITECTURE.md`'s Maintenance Note names `base/architecture-site` and `overlays/{prod,nonprod,architecture}` and says namespace `architecture` serves the site at the fourth DuckDNS hostname on a staging certificate until the leak review.
- Post-commit: `git show --name-only --format= HEAD` lists exactly the 17 paths, no deletions, `git status` is clean for `k8s`, `docs/architecture/model` and `docs/INFRA_ARCHITECTURE.md`, and `docs/architecture/out/export.json` is trashed.

## Task Commits

1. **Tasks 1 and 2 together (one atomic commit by plan design):** `8e531d0` (feat), through the full `.githooks/pre-commit` (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest, LikeC4 sync check)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The production view did not draw the namespace node**
- **Found during:** Task 2, the production-view verify command
- **Issue:** with only `netcup.k3s.architecture.**` in the include, the export held `netcup.k3s.architecture.archsite` (parent `netcup.k3s`) and no `netcup.k3s.architecture` node. LikeC4 collapses a namespace that holds a single instance into its parent, and `cert-manager` shows the same collapse. The plan's expected output and truth 6 need the node.
- **Fix:** `views.c4`'s production include reads `netcup.k3s.architecture, netcup.k3s.architecture.**,`. The verify command then prints `netcup.k3s.architecture,netcup.k3s.architecture.archsite HTTP :8080 true`. The executor first stopped and reported; the orchestrator chose this fix.
- **Files modified:** `docs/architecture/model/views.c4`
- **Commit:** `8e531d0`

**2. [Rule 1 - Bug] Comment lint rejected the first commit attempt**
- **Found during:** Task 2, the commit hook
- **Issue:** `apps-architecture.yaml`'s header was a four-line first paragraph, over the two-line summary limit (docs/CODE_STYLE.md rule 14). Task 1's lint run came before that file existed.
- **Fix:** a one-line summary, a blank comment line, then the dependsOn and healthChecks reasons. The hook refused the commit, so nothing landed before the fix; the second attempt passed the full hook.
- **Files modified:** `k8s/flux-system/apps-architecture.yaml`

### Plan expectation wrong

**3. The `name: kanban-board-backend-architecture` count is 3, not 2**
- **Found during:** Task 2 acceptance
- **Issue:** `grep -c 'name: kanban-board-backend-architecture' k8s/flux-system/image-automation.yaml` prints 3. The ImageRepository's name, the ImagePolicy's name and the policy's `imageRepositoryRef` name all match.
- **Fix:** none needed; the three objects are correct. The plan miscounted.

## Issues Encountered

- `docs/architecture/drill-output.txt` is stale on purpose: its DRIFT header carries element and workload counts this commit changes (62 elements and 18 workloads now). Plan 05 regenerates it; nothing in the hook or CI compares it.

## Known Stubs

None.

## Threat Flags

None: every new surface (route, Certificate, Flux Kustomization, container) is in the plan's threat model, with T-03-16 to T-03-20 mitigated as planned.

## Next Phase Readiness

- Plan 05 runs the drills on a scratch copy and regenerates drill-output.txt; plan 06 pushes the commit and reads the live certificate and the Flux label.
- `git rev-list --count origin/main..HEAD` is 3 now (2 from plan 03 plus this commit); nothing is pushed. PUB-02 stays Pending until plan 06.

## Self-Check: PASSED

- All 14 source files and the three records exist, and `8e531d0` is an ancestor of HEAD.
- The commit lists exactly the 17 paths of this plan's `files_modified`.
