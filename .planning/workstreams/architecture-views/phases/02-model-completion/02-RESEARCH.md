# Phase 2: Model Completion - Research

**Researched:** 2026-10-28
**Domain:** LikeC4 1.60.0 Kruchten 4+1 model (deployment views, dynamic views, element notes) plus a Node drift checker (`docs/architecture/sync.mjs`) reading k8s YAML
**Confidence:** HIGH for repo facts and for the LikeC4 behaviours measured in this session; MEDIUM for the proposed element design; the items in the Assumptions Log are unverified.

No `02-CONTEXT.md` exists for this phase, so there are no locked decisions. Everything below is a recommendation the planner may override. The project's GSD directive (`.claude/CLAUDE.md`, "GSD Execution Directives") still applies: before a PLAN.md is created or approved it must document 2 alternate approaches and a 3-column trade-off matrix [Approach | Pros/Cons | Why Picked]. Candidate alternates are tabulated in "Decision points with trade-off matrices" below so the planner can copy them.

<user_constraints>
## User Constraints (from CONTEXT.md)

No CONTEXT.md exists for Phase 2 (`/gsd-discuss-phase` was not run). The constraints that do apply come from REQUIREMENTS.md, ROADMAP.md Phase 2 and the "Defaults accepted" line in STATE.md:

### Locked (from STATE.md "Defaults accepted" and REQUIREMENTS.md "Out of Scope")
- Monitoring, cert-manager and Flux appear in the Physical view.
- The error-status scenario shows client-visible discriminators only (401 with no SQL, 403 after the ownership SELECTs, 409 on the version check); no filter-chain components.
- Mermaid migration and retirement of `docs/diagrams/` are out of scope; the Mermaid files stay as the parity baseline.
- Package cycles are reported, never gated.
- Put metadata on elements, not relationships (ROADMAP Phase 2 planning note).
- Write each description fresh, one sentence, no runbook text pasted in (ROADMAP Pitfall 11 note).

### Claude's Discretion
- Everything else: element kinds, view membership, file layout of the `.c4` files, which reader or rule form GATE-01 takes, how the `[...]` labels are removed.

### Deferred Ideas (OUT OF SCOPE)
- V2-01 signup race scenario, V2-02 DROP TABLE scenario, V2-03 layer overlay, V2-04 state markers, V2-05 Docker Hub retention.
- SCEN-08 and every PUB requirement (Phases 3 and 4).
- A LikeC4 legibility number in the standard (Open Question 7: default is cap scenarios at 5 participants and record natural widths, no number written into the standard).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PHYS-02 | Delivery view: GitHub, CI runner, Docker Hub, Flux, image path into the cluster | "Facts: delivery" table; `provider` deployment-node kind and `extend` verified by spike; Flux controller anchors and the `controller-resources.yaml` trap |
| PHYS-03 | Monitoring view: monitoring nodes and what they scrape | "Facts: monitoring" table; HelmRelease anchoring; scrape evidence per target |
| PHYS-04 | cert-manager, issuers, edge routing, host filter as external element | "Facts: edge and cert-manager" table; ClusterIssuer-on-cluster-node placement trick; non-Kubernetes instance under the `server` node verified by spike |
| PROC-01 | Process view: Schema Registry, init job, management port, backup flow | "Facts: process" table; `job` kind notation caveat |
| LOGI-01 | `activity_log` table and Avro subjects as notes tied to sources | `note` element kind with `shape document` verified; source files and stale-comment warning |
| SCEN-01 | Signup scenario | Participants and line-cited steps |
| SCEN-02 | Error-status scenario, client-visible discriminators | Three views, line-cited; "no SQL" caveat (cookie absent) |
| SCEN-03 | Push-to-image scenario | `deploy.yml` line cites |
| SCEN-04 | Image-to-rollout scenario | Flux and overlay line cites |
| SCEN-05 | Inbound-packet-path and ingress-to-pod scenarios | Open Question 5 spike answers; host-level elements as logical elements |
| SCEN-06 | Mutation to `activity_log` row scenario | 5-participant composition; line cites; stale-comment warning |
| SCEN-07 | Nightly backup scenario | Script line cites |
| GATE-01 | `sync.mjs` reads yaml facts the new elements claim (namespaces, workloads, hosts), fails on drift | Reader design, drill design, ordering constraint, the `controller-resources.yaml` duplicate-name trap |
| STD-01 | Physical and Scenarios rows of `docs/DIAGRAM_CONVENTIONS.md` amended | Exact sentences to change and stale-sentence sweep list |
</phase_requirements>

## Summary

Phase 2 edits only the LikeC4 model (`docs/architecture/model/*.c4`), the drift checker (`sync.mjs`), the drill harness, and `docs/DIAGRAM_CONVENTIONS.md`. It does not touch `k8s/`, `src/` or `.github/`. That fact drives the plan shape: every new element's honesty comes from `sync.mjs` reading existing manifests, and the file-sharing hot spots are `sync.mjs`, `drill.fish`, `drill-output.txt`, `deployment.c4`, `model.c4`, `views.c4`, `spec.c4` and the standard.

Four findings change the plan and are not in the roadmap. (1) The existing `production` view already violates the Phase 2 criterion "no edge label reads `[...]`": 4 of its 9 edges do, measured today [VERIFIED: `likec4 export json` on the real model]. A deployment-level relationship, a direct logical relationship and a view-level `with { title }` were each tried and none removes the label; only making every merged relationship share one identical title does, or tagging one relationship per pair and filtering the view with `exclude * -> * where tag is not #physical` [VERIFIED: scratch spike, deleted]. (2) `production` is `include netcup, netcup.k3s, netcup.**`, so every new instance on the server is pulled into it automatically; the include must be narrowed before monitoring or Flux instances land [VERIFIED: spike]. (3) The six Flux controller Deployments appear twice under `k8s/flux-system`: in `gotk-components.yaml` and again in `controller-resources.yaml` (a strategic-merge patch with `kind: Deployment`). A reader that scans the whole directory cannot catch a rename in `gotk-components.yaml`, so the GATE-01 "rename a Flux controller" drill would silently pass [VERIFIED: parsed with `yaml`]. (4) `drill.fish` `fresh()` copies only migrations, `application.properties`, Java sources, `k8s` and the model, so any new element whose `source` metadata points at `.github/workflows`, `infra/vm` or `src/main/avro` makes every drill, the baseline included, fail with "metadata source does not exist".

Open Question 5 (the render spike) is answered here by running it: a non-Kubernetes element can sit directly under the `server` deployment node; a `provider` deployment-node kind works at top level; a sequence view silently drops a step to a parent when its child is also a participant, so participants must be leaves; dynamic steps need no matching relationship (so scenarios are unchecked prose unless evidenced); `par` was not rendered and the standard bans branches anyway.

**Primary recommendation:** Build in this order, with each reader widening landing in the same commit as the elements it forces (the hook refuses a red tree): (1) spec kinds, `production` include narrowing, a "no `[...]` edge label" rule and a "dynamic view has at most 5 participants" rule, red on today's 4 bad edges; (2) HelmRelease/ClusterIssuer reader plus monitoring and cert-manager elements; (3) Flux controller reader (gotk-components only) plus delivery elements; (4) `host` metadata plus edge elements; (5) Process and Logical additions; (6) scenarios (SCEN-01, 02, 06 need no new elements and can start in wave 1); (7) STD-01 and the full re-recording of `drill-output.txt`.

## Project Constraints (from CLAUDE.md)

From `/home/andre/dev/kanban-board-backend/.claude/CLAUDE.md` and `~/.claude/CLAUDE.md`:
- Source `.dev/gsd-run.sh` rather than pasting the resolver: `. ./.dev/gsd-run.sh && gsd_run query ...`.
- Before creating or approving any PLAN.md: document 2 alternate approaches, a 3-column trade-off matrix [Approach | Pros/Cons | Why Picked], and any non-obvious performance, memory or security trade-off.
- Never auto-execute code blocks without explaining the core data flow in 3 sentences or less.
- Read `docs/SESSION_LESSONS.md` before starting or resuming a phase-execution session (git hygiene).
- Every commit runs `.githooks/pre-commit` (about 4 minutes: gitleaks, comment lint, instruction budget, spotless, fastTest). `--no-verify` is allowed for docs, planning and config-only commits, but not for Java, migrations or k8s manifests. Phase 2 commits touch `docs/architecture`, `.planning` and `docs/DIAGRAM_CONVENTIONS.md` only, so they qualify; the LikeC4 check inside the hook is then skipped, so run `node docs/architecture/sync.mjs check` by hand before each commit and rely on CI `likec4-model-drift`.
- Gitleaks refuses credential-shaped values anywhere including `.planning/` prose. Do not paste the age recipient string, the B2 bucket name or endpoint, share ids or IP literals into planning files.
- Code comments are plain text: bare identifiers, blank comment line between paragraphs, `- ` for list items (enforced by `scripts/verify-comments.py`).
- Prefer the repo's existing tool over new code: LikeC4's computed-model API instead of parsing `.c4` text; the already-installed `yaml` package instead of a regex over YAML.
- Pin every bug fix with a failing test; here the equivalent is the drill rule: each new check is seen red then green (global CLAUDE.md "Pin every bug fix", and GATE definition in REQUIREMENTS.md).
- Commit finished work without being asked, only the touched paths, never push, never amend (global CLAUDE.md). Merging to main waits for the user (memory: "Ask before merging to main").

## Architectural Responsibility Map

The tiers here are the model's own layers, not web tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Fact checking against k8s YAML (workloads, namespaces, hosts) | `sync.mjs` `check()` | `drill.fish` proves it, CI job `likec4-model-drift` runs it | One reader feeds the check; the hook runs it only for staged paths under `k8s`, `docs/architecture`, migrations, `KafkaTopics.java` |
| Where a thing runs (namespace, host, provider) | `deployment.c4` nodes and instances | placement rule in `sync.mjs` | Instances inherit logical relationships; the placement rule compares instance namespace with manifest namespace |
| What a thing is and talks to | `model.c4` (authored) and `generated.c4` (generated) | `metadata.source` existence check | Provenance ladder: generated, checked, authored |
| What is drawn | `views.c4` and new per-family view files | view-membership, viewpoint-tag, new `[...]` and participant rules | A view is a projection; deployment instances vanish from a view if unconnected |
| Scenario narrative | `dynamic view` blocks | line-cited evidence file (not machine-checked) | Dynamic steps are validated against nothing in the model [VERIFIED: spike] |
| Standard text | `docs/DIAGRAM_CONVENTIONS.md` | verifier sweep for stale sentences | Phase 1 needed a gap-closure quick task for one stale sentence (261027-r62) |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| likec4 | 1.60.0 | Model parse, computed views, `export json`, `export png --seq` | Pinned in `docs/architecture/package.json`; npm `latest`, modified 2026-10-15 [VERIFIED: `npm view likec4 version time.modified`] |
| yaml | 2.9.1 | Structured reads of k8s manifests in `sync.mjs` | Added in Phase 1 behind a legitimacy gate; npm `latest` 2.9.1 [VERIFIED: `npm view yaml version`; `docs/architecture/package.json`] |

### Supporting
| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| Node | 24.19.0 (CI) | Runs `sync.mjs` | `invariant-checks.yml` job `likec4-model-drift` pins it |
| fish | 4.2.1 | `drill.fish`, `likec4.fish` | Repo preference for new scripts |
| Playwright Chromium (bundled with likec4) | n/a | `likec4 export png --seq` | Worked in this session: 10 views in 1 m 23 s [VERIFIED: ran it] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Reading `kustomize build` output | The current `yaml` reader over source files | A render needs the pinned kubectl download; Phase 1 rejected it (ARCHITECTURE N3). Source reading cannot see chart-rendered objects (accepted limit) |
| A new reader per fact family | One `k8sDocs()` walk reused by every rule | Already built in Phase 1 (`sync.mjs:254-277`); reuse it |

**Installation:** none. No package is added in Phase 2.

**Version verification:** `likec4` 1.60.0 and `yaml` 2.9.1 confirmed against the npm registry on 2026-10-28.

## Package Legitimacy Audit

Phase 2 installs no external package. `yaml` 2.9.1 and `likec4` 1.60.0 are already in `docs/architecture/package.json` and `pnpm-lock.yaml` from Phase 1 (which passed its own legitimacy gate). If a plan adds one anyway, run `gsd_run query package-legitimacy check --ecosystem npm <pkg>` first and insert a `checkpoint:human-verify` task.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 k8s/**  (platform, monitoring, flux-system, overlays, base, data)
 .github/workflows, infra/vm, src/main/avro, migrations      (cited by metadata.source)
        |
        |  k8sDocs(): parseAllDocuments over every *.yaml under k8s  (exit 4 on unreadable)
        v
 sync.mjs check ----------------------------------------------------------------------+
   |  A. generated.c4 up to date                (migrations, Java imports)              |
   |  B. model validates, viewpoint tags, topics                                        |
   |  C. workloads two-way        <- WIDEN: HelmRelease, ClusterIssuer, flux gotk file  |
   |  D. namespaces both ways, placement, metadata keys (+ host), view membership       |
   |  E. hosts two-way            <- NEW: Host(`..`) in IngressRoute vs metadata host   |
   |  F. NEW: no deployment-view edge label "[...]"; dynamic view has <= 5 nodes        |
   v                                                                                    |
 exit 0 / 1 (drift) / 4 (fact source) <--- hook (staged k8s, docs/architecture) + CI ---+
        ^
        |  drill.fish: scratch copy (--root, --model), SYNC=old script for "not caught before"
 model/*.c4:  spec.c4 (kinds) | model.c4 + new per-family files | deployment.c4 (+ extend) | views
```

### Recommended Project Structure
```
docs/architecture/model/
  spec.c4          # new kinds: deploymentNode provider; element external, note; job notation
  model.c4         # existing authored model; keep edits here minimal (title unification)
  generated.c4     # untouched in Phase 2 unless a generated fact is added
  deployment.c4    # existing nodes; extend nodes from per-family files instead of editing
  views.c4         # narrow `production`; keep signin views
  delivery.c4      # NEW: github, ci, dockerhub, flux elements + provider nodes + delivery view
  monitoring.c4    # NEW: prometheus, grafana, loki, alloy, exporters + instances + monitoring view
  edge.c4          # NEW: hostfilter, firewall, svclb, certmanager, issuers + instances
  scenarios.c4     # NEW (or one file per scenario family): dynamic views only
```
Separate files are a recommendation, not a requirement: LikeC4 loads every `.c4` in the folder, `extend <node>` works for nested deployment nodes, and a second `views { }` block in another file validates [VERIFIED: spike]. They exist to let plans edit disjoint files.

### Pattern 1: Provider nodes and a non-Kubernetes instance (answers Open Question 5a)
**What:** External SaaS and host-level things are deployment nodes or instances outside any namespace.
**Verified:** `deploymentNode provider` in `specification`; `github = provider 'GitHub (SaaS)' { instanceOf gh }` at top level of `deployment`; `extend netcup { hf = instanceOf hostfilter }` places a non-Kubernetes element directly under the server node; `extend netcup.k3s.flux-system { instanceOf fluxsys }` extends a nested node. The exported node list contained `github<provider>`, `github.gh<instance>`, `netcup.hf<instance>`, and `edge github.gh -> dockerhub.hub label="push image"` [VERIFIED: spike on a scratch copy, since deleted].
**Example (illustrative new ids, not repo values):**
```likec4
specification {
  deploymentNode provider
  element external { style { color muted } }
}
deployment {
  github = provider 'GitHub (SaaS)' { instanceOf repo }
  extend netcup { hf = instanceOf hostfilter }
  extend netcup.k3s { issuer_staging = instanceOf certmanager.staging }  // cluster-scoped, no namespace
}
```
Source: spike, plus likec4 docs "Deploy to any level, not only leaf nodes" [CITED: https://likec4.dev/dsl/deployment/model/, via context7 `/likec4/likec4`].

### Pattern 2: A drawn `client` needs an instance
`client` is not in the deployment model, so `client -> hostfilter` is not drawn in a deployment view (the spike drew only `hostfilter -> traefik`). If the Physical view must show the inbound edge, add an `internet = provider 'Internet'` node with `instanceOf client` [VERIFIED by absence in spike; fix not rendered].

### Pattern 3: Cluster-scoped objects sit on the cluster node
A `ClusterIssuer` has no namespace. `placements()` returns `[]` for it (`k8s/platform/edge/kustomization.yaml` sets no `namespace`), and an instance directly under `k3s` has no namespace ancestor, so `have === want === ''` and the placement rule passes [VERIFIED by reading `sync.mjs:353-361`, `368-388` and `k8s/platform/edge/kustomization.yaml`; not run]. Anchor such elements by `workload 'ClusterIssuer/<name>'`, never by `source 'k8s/platform/edge'`: that directory also holds `Certificate/grafana` in namespace `monitoring`, which would make the expected namespace set `{monitoring}`.

### Pattern 4: Metadata arrays
`workload ['Deployment/source-controller', 'Deployment/kustomize-controller']` validates and exports as an array; `getMetadata('workload')` is already consumed through `.flat()` in `sync.mjs:378` and `533` [VERIFIED: spike + read].

### Pattern 5: Notes as elements
`element note { style { shape document } }` validates; a `note` child of `postgres` is included by `include postgres.*` with no relationship [VERIFIED: spike]. `metadata.source` on the note is then existence-checked by the current rule (`sync.mjs:538-541`).

### Anti-Patterns to Avoid
- **Scanning `k8s/flux-system` for workloads without excluding `controller-resources.yaml`.** The patch repeats the six Deployment names; see Pitfall 3.
- **Relying on `include netcup.**` once monitoring and Flux instances exist.** Narrow it (Pitfall 2).
- **One element per k8s object** (every IngressRoute, Middleware, Kustomization, ImageRepository). FEATURES anti-feature list; keep to one element per runtime role.
- **A hand-written count or port in a label** (`:8081`, `9081`, "14 subjects", a cron time). The standard's "Numbers in labels" rule requires a check or omission. Say "management port", "Schema Registry".
- **Parent and child as participants in one sequence view** (Pitfall 5).
- **Pasting runbook text or in-cluster DNS names into descriptions.** Phase 4's scan will fail the build on them; avoid the rework.

## Facts for each new view (repo evidence)

Every row below was read in this session. "Quote" is verbatim.

### Facts: delivery (PHYS-02, SCEN-03, SCEN-04)

| Fact | Evidence | Quote |
|---|---|---|
| Image build runs on `ubuntu-latest`, amd64 only | `.github/workflows/deploy.yml:130-134` | `- name: Build and push Docker image (linux/amd64)` ... `platforms: linux/amd64` |
| Trigger and loop guard | `deploy.yml:7-22` | `paths-ignore:` ... `- "k8s/**"` (comment: "an infinite loop") |
| `run-tests` gates the other jobs | `deploy.yml:55-58`, `90`, `156` | `needs: [ setup, run-tests ]` |
| Tag scheme | `deploy.yml:114-116` | `echo "image_tag=main-${{ github.run_number }}-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"` |
| Two Docker Hub repos pushed by one build | `deploy.yml:24-29`, `136-138` | `DOCKERHUB_REPOSITORY: kanban-board-backend`, `DOCKERHUB_REPOSITORY_NONPROD: kanban-board-backend-nonprod` |
| Third repo (backup image) pushed by its own workflow | `.github/workflows/backup-image.yml:28-30` | `DOCKERHUB_REPOSITORY: kanban-postgres-backup` |
| Flyway verify over an SSH forward to Postgres, host key pinned | `deploy.yml:155-228`, `166-188`, `193-202` | `- name: Open SSH forward to in-cluster Postgres` (never quote the forward target in a label) |
| Flux scans two repos, picks highest run number | `k8s/flux-system/image-automation.yaml:4-34` | `interval: 5m`, `pattern: '^main-(?P<num>[0-9]+)-[a-f0-9]{7}$'` |
| Flux commits tag bumps to main under `./k8s/overlays` | `image-automation.yaml:52-80` | `chore(flux): bump images`, `path: ./k8s/overlays`, `strategy: Setters` |
| Flux polls Git over SSH every minute | `k8s/flux-system/gotk-sync.yaml:4-15` | `interval: 1m`, `url: ssh://git@github.com/RudVlad473/kanban-board-backend` |
| Setter marker in the overlay | `k8s/overlays/prod/kustomization.yaml:23` | `newTag: main-158-80831b3 # {"$imagepolicy": "flux-system:kanban-board-backend-prod:tag"}` (do not copy the tag value; Flux rewrites it) |
| Rollout is Recreate, with an init container first | `k8s/base/app/app.yaml:18-19`, `33-57` | `type: Recreate`; `- name: register-schemas` |
| Six Flux controllers | `k8s/flux-system/gotk-components.yaml` (parsed) | Deployments: source-controller, kustomize-controller, helm-controller, notification-controller, image-reflector-controller, image-automation-controller, all `namespace: flux-system` |
| Nine Flux Kustomizations | `k8s/flux-system/{platform,data,cert-manager,edge,apps-prod,apps-nonprod,monitoring-controllers,monitoring}.yaml` plus `gotk-sync.yaml:17-28` | e.g. `dependsOn: - name: platform` |

Elements to add (recommended): `repo` (system, "Git repository, main"), `ci` (system, "GitHub Actions workflows"), `dockerhub` (`external` or `system`), `flux` (system, claims the six Deployments by array). Deployment: `github = provider` holding `repo`; a runner `server` node titled with its architecture holding `ci`; `dockerhub = provider`; `flux` instance in the existing `flux-system` namespace node. The runner's x86_64 claim is only in the repo's own Mermaid (`docs/diagrams/physical/delivery-nodes.mmd:7`, `subgraph gha["GitHub Actions runner — x86_64"]`); it is authored [ASSUMED for GitHub's hosted image; see A1].

Provenance for delivery edges: workflow job names and repo names are not read by any check. Tie them to existence with `source '.github/workflows/deploy.yml'` and `source 'k8s/flux-system/image-automation.yaml'` and label the rest authored. This forces `fresh()` to copy `.github` (Pitfall 4).

SCEN-03 `push-to-image`, 4 participants: `repo`, `ci`, `dockerhub`, `postgres` (a 5th, a developer actor, is allowed). Steps and cites: push to main (`deploy.yml:7-9`); tests gate (`55-86`); derive tag (`114-116`); build and push both repos (`130-138`); Flyway verify through an SSH forward, host key pinned (`155-228`).

SCEN-04 `image-to-rollout`, 4 to 5 participants: `flux`, `dockerhub`, `repo`, `kanban` (plus the init job). Steps and cites: Flux lists tags (`image-automation.yaml:4-11`); policy picks highest (`22-34`); Flux commits the bump to `k8s/overlays` (`52-80`); `k8s/**` is ignored by CI (`deploy.yml:22`) so no build starts; Flux polls Git (`gotk-sync.yaml:10`); apply with Recreate (`app.yaml:18-19`); init job runs before the app container (`app.yaml:33-57`). The Mermaid `image-to-rollout.mmd` is a starting outline, not evidence.

### Facts: monitoring (PHYS-03)

| Fact | Evidence | Quote |
|---|---|---|
| Four HelmReleases in namespace `monitoring`, no `targetNamespace` | `k8s/monitoring/controllers/{kube-prometheus-stack,loki,alloy,postgres-exporter}.yaml` | `kind: HelmRelease`, `namespace: monitoring` |
| Chart bundles Prometheus Operator, Prometheus, Grafana, kube-state-metrics, node-exporter | `kube-prometheus-stack.yaml:1`, `86-187` | header: `kube-prometheus-stack HelmRelease: Prometheus Operator, Prometheus, Grafana, kube-state-metrics.` |
| Alertmanager disabled | `kube-prometheus-stack.yaml:37-38` | `alertmanager:` / `enabled: false` |
| Prometheus scrapes the app on its management port | `k8s/monitoring/configs/podmonitors.yaml:37-40` | `- port: management` / `path: /actuator/prometheus` |
| Prometheus scrapes Redpanda | `podmonitors.yaml:15-17` | `- port: admin` / `path: /public_metrics` |
| Both PodMonitors select the two app namespaces | `podmonitors.yaml:8-11` | `- kanban-prod` / `- kanban-nonprod` |
| node-exporter has a ServiceMonitor | `kube-prometheus-stack.yaml:160-168` | `prometheus:` / `monitor:` / `enabled: true` |
| postgres-exporter has a ServiceMonitor and reads Postgres | `postgres-exporter.yaml:34-40`, `62-67` | `serviceMonitor:` / `enabled: true` |
| cert-manager exposes a ServiceMonitor | `k8s/platform/cert-manager/cert-manager.yaml:43-47` | `servicemonitor:` / `enabled: true` |
| Alloy tails pod logs through the Kubernetes API and pushes to Loki | `alloy.yaml:74-83` | `loki.source.kubernetes "pods"` ... `loki.write "default"` |
| Grafana reads two datasources: Prometheus and Loki | `k8s/monitoring/configs/datasources.yaml:19-32` | `type: prometheus`, `type: loki` (the URLs are in-cluster names; do not copy them) |
| Grafana is the only publicly routed monitoring piece | `k8s/monitoring/configs/ingressroute.yaml:37-64` | `kind: IngressRoute` `name: grafana` |

Scrape targets that the repo evidences, and so the monitoring view may draw an edge to: the app (management port), Redpanda, node-exporter, postgres-exporter, cert-manager. kube-state-metrics and the chart's own kubelet and API-server monitors are chart defaults not visible in repo YAML [ASSUMED, A2]; state in the element description that the scrape edges are authored from the manifests, not from live targets (`cadvisor.json` is only a dashboard; no cAdvisor workload exists, FEATURES anti-feature).

Anchoring: each monitoring element carries `workload 'HelmRelease/<name>'`. Prometheus, Grafana, kube-state-metrics and node-exporter all carry the same `HelmRelease/kube-prometheus-stack`, which the set-based two-way check tolerates (`sync.mjs:533-536`). The check then proves the HelmRelease exists and sits in `monitoring`; it does not prove Prometheus runs in it (accepted limit already in the standard: the chart is never rendered).

### Facts: edge and cert-manager (PHYS-04, SCEN-05)

| Fact | Evidence | Quote |
|---|---|---|
| Traefik is the k3s-packaged chart, configured by a HelmChartConfig in `kube-system` | `k8s/platform/traefik/helmchartconfig.yaml:16-21` | `kind: HelmChartConfig` / `name: traefik` / `namespace: kube-system` |
| Traefik edge is a LoadBalancer with `externalTrafficPolicy: Local` | `helmchartconfig.yaml:22-30` | `type: LoadBalancer` ... `externalTrafficPolicy: Local` |
| cert-manager is a HelmRelease in `flux-system` installing into `cert-manager` | `cert-manager.yaml:19-23`, `38` | `namespace: flux-system` ... `targetNamespace: cert-manager` |
| Two ClusterIssuers, HTTP-01 via Traefik's ingress class | `k8s/platform/edge/clusterissuers.yaml:10`, `13`, `24`, `27`, `16-19`, `30-33` | `name: letsencrypt-staging`, `name: letsencrypt-production`, `ingressClassName: traefik`; ACME hosts `acme-staging-v02.api.letsencrypt.org` and `acme-v02.api.letsencrypt.org` |
| Header says every Certificate starts on staging | `clusterissuers.yaml:1-2` | `every Certificate starts on letsencrypt-staging` (the two committed app Certificates are on production; `overlays/prod/certificate.yaml:11`) |
| Three public hostnames, each in an IngressRoute `Host(...)` and a Certificate | `k8s/overlays/prod/ingressroute.yaml:62-90`, `overlays/nonprod/ingressroute.yaml:20,37`, `monitoring/configs/ingressroute.yaml:46,58,79`; `overlays/{prod,nonprod}/certificate.yaml`, `platform/edge/certificate-monitoring.yaml` | the three `*.duckdns.org` names (hostnames are already linked from the README; Pitfall 11 allows them) |
| HTTP routes exclude the ACME path | `overlays/prod/ingressroute.yaml:90` | ``Host(`...`) && !PathPrefix(`/.well-known/acme-challenge/`)`` |
| Rate-limit Middlewares per namespace | `overlays/prod/ingressroute.yaml:4-49`, `monitoring/configs/ingressroute.yaml:7-34` | `kind: Middleware`, `rateLimit:` |
| Host filter is a mangle PREROUTING chain at position 1 on `eth0`, IPv4 only | `infra/vm/k3s-host-firewall.sh:40-45`, `30-31`, `64` | `readonly EXT_IF="eth0"`; `readonly PREROUTING_JUMP="-I PREROUTING 1 -i ${EXT_IF} -j ${CHAIN}"`; `readonly ALLOWED_TCP_PORTS="22 80 443"`; `-A ${CHAIN} -m conntrack --ctstate NEW -j DROP` |
| Netcup Cloud Firewall is outside the repo | `docs/INFRA_ARCHITECTURE.md:85-88` | "`[1]` is deliberately marked `(external — not in this repo)`" |
| `svclb-traefik` hostPort pod is created by k3s ServiceLB | `docs/INFRA_ARCHITECTURE.md:51-54` | "`svclb-traefik-*` is the ONLY Pod in the cluster carrying a `hostPort` (80, 443)" (doc claim, confirmed live in the doc only; no manifest in the repo) |

Recommended elements: `certmanager` (system) with child components `staging` and `production` for the issuers, deployed as instances on the `k3s` cluster node for the issuers and in the `cert-manager` namespace for the controller; `hostfilter` (system, "KANBAN-INGRESS") instantiated under `netcup`; `firewall` (`external`, description says "not in this repo", no rules); `svclb` (component or system). Anchors: `certmanager` `workload 'HelmRelease/cert-manager'`; each issuer `workload 'ClusterIssuer/letsencrypt-staging'` and `'.../letsencrypt-production'`; `hostfilter` `source 'infra/vm/k3s-host-firewall.sh'` (existence only; non-`k8s` source is not placement-checked and must be recorded as such in the standard, which already says so at `docs/DIAGRAM_CONVENTIONS.md:190-193`).

Hosts claim (GATE-01 hosts): put `host ['<the three names>']` on the `traefik` element and add `host` to `METADATA_KEYS`. Reader: every `Host(\`...\`)` literal in `spec.routes[].match` of every `IngressRoute`, compared two-way with the union of `host` metadata. Per-instance host mapping stays authored.

SCEN-05, two views (from `inbound-packet-path.mmd` and `ingress-to-pod.mmd`, used only as outlines):
- `inbound-packet-path`, 5 participants: `client`, `firewall`, `hostfilter`, `svclb`, `traefik`. Steps: HTTPS to the VPS; Cloud Firewall (external, unverifiable); mangle PREROUTING jump to the filter (`k3s-host-firewall.sh:43`); NEW tcp on the allow-list returns, anything else NEW drops (`:45`, `:64`); hostPort DNAT to the svclb pod (doc claim, INFRA_ARCHITECTURE:51-54); proxy to Traefik. Describe the ruleset by role ("allow-listed ports"), not by listing ports, per "Numbers in labels".
- `ingress-to-pod`, 3 to 4 participants: `svclb`, `traefik`, `kanban.api` (or `grafana`). Steps: route match; rate-limit Middleware (`overlays/prod/ingressroute.yaml:15-21`); forward to the ClusterIP Service port (`:67-68`). Do not name the Middleware's numbers.
Host-level facts are authored; only the existence of `infra/vm/k3s-host-firewall.sh` is checked.

### Facts: process (PROC-01, SCEN-07)

| Fact | Evidence | Quote |
|---|---|---|
| Schema Registry is Redpanda's built-in registry | `k8s/base/redpanda/redpanda.yaml:22-23`, `73-74` | `- name: schema-registry`; `- --schema-registry-addr` / `- 0.0.0.0:8081` |
| `register-schemas` is an initContainer, not a Job | `k8s/base/app/app.yaml:3-6`, `36` | "register-schemas is an initContainer, not a Job" |
| It runs the registrar with the registry URL | `app.yaml:40-49` | `- -Dloader.main=com.vrudenko.kanban_board.config.AvroSchemaRegistrar` |
| Registrar registers each record under its full name and sets BACKWARD first | `src/main/java/.../config/AvroSchemaRegistrar.java:93-101` | `String subject = schema.getFullName();` ... `client.updateCompatibility(subject, BACKWARD_COMPATIBILITY);` |
| Producer only looks schemas up | `src/main/resources/application.properties:217` | `spring.kafka.producer.properties.auto.register.schemas=false` |
| Subject strategy on both sides | `application.properties:223`, `259` | `...value.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy` |
| Management port is unrouted, probed and scraped | `app.yaml:62-67`, `91-108`, `120-125`; `application.properties:30` | `- name: management` / `containerPort: 9081`; `management.server.port=9081`; "Never add the management port here" |
| Backup CronJob runs in `kanban-data`, carries the Postgres-client label | `k8s/data/postgres/backup-cronjob.yaml:41-44`, `24-27` | `kanban-board/postgres-client: "true"` |
| Backup steps: wait for Postgres, dump, check archive, encrypt, upload daily, upload weekly on Sunday | `k8s/data/postgres/backup/postgres-backup.sh:46-53`, `56-57`, `65`, `72-78`, `82-89` | `pg_dump -Fc --no-password -f "$WORK_DIR/db.dump"`; `age -r "$AGE_RECIPIENT" -o ...`; `aws s3api put-object ... --checksum-algorithm CRC32` |
| Bucket key is write-only, private key never in the cluster | `backup-cronjob.yaml:13-17` | "The bucket key has write capability only." |
| `INFRA_ARCHITECTURE.md` is stale on backup | `docs/INFRA_ARCHITECTURE.md:78-83` | "there is still no backup" (contradicted by the CronJob; the standard is out of scope for that doc fix, but do not copy the sentence) |

Elements to add: under `kanban`, a `management` container ("Management server", unrouted) and an `init` job ("register-schemas"); under `redpanda`, a `registry` component; a top-level external `b2` datastore ("off-cluster object bucket"). Relationships (one title per pair): `kanban.init -> redpanda.registry` register schemas; `kanban.publisher -> redpanda.registry` and `kanban.consumer -> redpanda.registry` schema lookup; `<monitoring> -> kanban.management` scrape; `backup -> postgres` dump; `backup -> b2` "age-encrypted dump, put". Encryption is a label on the upload, not an element: age runs inside the backup container (`postgres-backup.sh:65`).

Kind caveat for the init job: the existing `job` kind has `notation 'Scheduled job'` (`spec.c4:17-22`), which is wrong for an init container. Either generalise the notation to a run-to-completion wording (one line in `spec.c4`, no new kind, keeps the closed-set rule untouched) or add a kind and amend the "Element" bullet in the standard. See the decision matrix.

SCEN-07 `nightly-backup`, 3 to 4 participants: `backup`, `postgres`, `b2` (CronJob controller is not an element). Steps and cites: wait until Postgres accepts connections through the NetworkPolicy peer (`postgres-backup.sh:46-53`, `backup-cronjob.yaml:41-44`); dump and check the archive (`56-57`); encrypt (`65`); upload under the daily prefix (`72-78`); upload a weekly copy on Sunday (`82-89`, an `if`, not a branch in the view; leave it to one step title or a description sentence). Leave the schedule, the bucket name, the endpoint and the age recipient out.

### Facts: logical notes (LOGI-01)

| Fact | Evidence | Quote |
|---|---|---|
| `activity_log` has no foreign key; `board_id` is a plain column | `src/main/resources/db/migration/V3__add_activity_log.sql:5-12` | `board_id varchar(255) NOT NULL,` (no `REFERENCES`) |
| Idempotency key | `V3__add_activity_log.sql:11` | `event_id uuid NOT NULL CONSTRAINT uk_activity_log_event_id UNIQUE,` |
| 14 Avro records | `src/main/avro/*.avsc` (14 files listed); `AvroSchemaRegistrar.java:56-70` | 14 `getClassSchema()` entries |
| Subject name is the record full name | `src/main/avro/AvroTaskMovedEvent.avsc` | `"name": "AvroTaskMovedEvent"`, `"namespace": "com.vrudenko.kanban_board.event.avro"` |

Notes: one `note` element on `activity_log` (source: the V3 migration file) and one on the Avro subjects (source: `src/main/avro`, a directory; `existsSync` accepts it). Leave the number 14 out of the text. The `activity_log` note makes a negative claim ("no foreign key"); the generated FK edges already cover it if it is false, but a 3-line rule "no `fk` edge has child `activity_log`" plus a drill would make it checked rather than authored. The Avro note stays authored unless a rule compares the `.avsc` set with the registrar list (optional).

Stale comments to avoid copying: `application.properties:218-219` says "all 5 event types ... 5 independently-versioned subjects" while the registrar registers 14; `application.properties:224-225` says the publish "runs synchronously on the request thread" while `KafkaEventPublisher.java:35-42` and `:102-105` submit to an executor. The Java is right in both cases. Filing these comments as a todo is out of scope here.

### Facts: scenarios on existing elements (SCEN-01, SCEN-02, SCEN-06)

**SCEN-01 `signup-ok`.** Participants (5, same set as `signin-ok`): `client`, `traefik`, `kanban.api`, `postgres.table_users`, `postgres.spring_session`. Steps and cites: `POST /api/signup` body validated before the handler (`AuthenticationController.java:118-122`, `@Valid`); duplicate-email check `UserService.java:75` (`existsByEmail`); insert `:80-81`; authenticate by user id (`AuthenticationController.java:129-130`, helper `150-179`; token `155`); live-session read and session-id rotation inside `sessionAuthenticationStrategy.onAuthentication` (`167-168`); context saved (`174`); `201` with `Location` (`146-147`). The SELECT-by-id before the session read comes from the authentication provider (`UserAuthenticationProvider.java:28`, `loadUserByUsername`), as in `signin-ok`. The `Location` header names a route with no GET handler (`AuthenticationController.java:141-146`); do not print the path as if it were live. Each step needs its own title (Pitfall 6).

**SCEN-02 three views** (one participant set each):
- `error-401`: `client`, `traefik`, `kanban.api`. "No SQL" holds for a request with no session cookie. `ProblemDetailAuthenticationEntryPoint.java:20-27` ("fires inside Spring Security's ExceptionTranslationFilter, before DispatcherServlet runs"); body `code` is `UNAUTHENTICATED` (`ErrorCode.java`, member list: `ENTITY_NOT_FOUND, ILLEGAL_ARGUMENT, ... UNAUTHENTICATED, ...`). A stale or invalid cookie would hit `spring_session`; the view title must say "no session cookie". That no SQL is issued for a cookie-less request is training knowledge about Spring Session, not read in this repo [ASSUMED, A3]; verify with a query-count test (`AbstractAppTest.countQueries`) or label the step authored.
- `error-403`: `client`, `traefik`, `kanban.api`, `postgres.table_users`, `postgres.table_boards`. Route family `/api/boards/{boardId}/columns` (`ColumnController.java:33`, `@RequestMapping(ApiPaths.BOARDS + ApiPaths.BOARD_ID + ApiPaths.COLUMNS)`); ownership SELECTs `OwnershipVerifierService.java:40` (`userRepository.findById`) and `46` (`boardRepository.findById`), then `:54-55` `throw new AppAccessDeniedException("Board")`; handler returns 403 with `code` `ACCESS_DENIED` (`GlobalExceptionHandler.java:176-180`).
- `error-409`: `client`, `traefik`, `kanban.api`, plus the tables the ownership chain reads (task, column, board, user are four tables: choose at most two to stay within 5 and describe the rest in the step title, or use `postgres.table_tasks` only). Route `PUT` on `.../tasks/{taskId}` (`TaskController.java:24-29`, `51`); `TaskService.updateById` loads and verifies ownership (`:169`) then compares versions (`:173-176`: `if (!task.getVersion().equals(dto.getVersion())) { throw new OptimisticLockingFailureException(`); handler maps to `409` and `OPTIMISTIC_LOCK_CONFLICT` (`GlobalExceptionHandler.java:245-249`). Nothing is written and no event is published (comment at `TaskService.java:191-193`).
The `400` outcome is not in SCEN-02 as written.

**SCEN-06 `activity-log-row`.** Choose a 5-participant composition: `kanban.api`, `kanban.publisher`, `redpanda.activity`, `kanban.consumer`, `postgres.table_activity_log`, and open with the committed mutation as a step title, since a sixth participant (`client`) would break the cap. Fallback that keeps the client: two views joined with `navigateTo` (commit half and event half). Steps and cites: mutation transaction commits and publishes a domain event (`TaskService.java:194-201`, `eventPublisher.publishEvent(new TaskUpdatedEvent(`); AFTER_COMMIT listener submits to the pool, request does not wait on Kafka (`KafkaEventPublisher.java:102-105`, `kafkaPublishExecutor.execute(() -> send(event));`; rationale `28-31`); send keyed by event id (`:121-124`); consumer group `activity-log` polls (`ActivityLogConsumer.java:45`, `51`); idempotent insert: `existsByEventId`, then `saveAndFlush` (`ActivityLogRecorder.java:42-47`); unique `event_id` constraint as the race backstop (`V3__add_activity_log.sql:11`). The `redpanda.dlt` branch stays out (standard bans branches).

### Existing `sync.mjs` surface (what Phase 1 left)

| Concern | Where | Behaviour |
|---|---|---|
| Fact sources | `sync.mjs:249-277` | `K8S_DIRS` six trees must exist (exit 4); `k8sDocs()` parses every yaml once; parse errors exit 4 |
| Workloads | `230-243`, `533-536` | `WORKLOAD_DIRS = ['k8s/base/', 'k8s/data/', 'k8s/platform/']`, `WORKLOAD_KINDS = Deployment, StatefulSet, CronJob`; two-way with element `workload` metadata |
| Namespaces | `279-303`, `543-553` | every manifest namespace needs a namespace node and vice versa |
| Placement | `305-389` | instance namespaces must equal the anchor documents' landing namespaces; anchors are `workload` docs else `k8s/` source docs; no anchor means skipped |
| Metadata keys | `53`, `391-410` | `METADATA_KEYS = ['source', 'workload']`, checked on elements, nodes, instances, relationships |
| View membership | `412-423` | every instance drawn in some deployment view |
| `source` existence | `538-541` | `existsSync(join(root, src))`, on `--root` |

The tally line prints `33 elements ... 4 workloads` today and is part of every drill's recorded output.

## GATE-01 design

**What to read, and how it is proven.**

| Claim | Reader change | Drill (break on scratch copy) | Where caught |
|---|---|---|---|
| Workload names, now including HelmReleases | Add `HelmRelease` and `ClusterIssuer` to `WORKLOAD_KINDS`; add `k8s/monitoring/` to `WORKLOAD_DIRS` (`k8s/platform/` already there, so `HelmRelease/cert-manager` is found at once) | rename `metadata.name: loki` in `k8s/monitoring/controllers/loki.yaml` | `model claims workload HelmRelease/loki but k8s/ has no such manifest` and the reverse line |
| Flux controllers | Add the single file `k8s/flux-system/gotk-components.yaml` to the scan (a path prefix match works for a file); do not scan `controller-resources.yaml` | rename `source-controller` in `gotk-components.yaml` | same message for `Deployment/source-controller` |
| Namespace | no reader change; the Phase 1 placement rule applies once the new elements have `workload` anchors | change `namespace: monitoring` in `loki.yaml` to `kanban-data` | `placement: <loki element> is deployed in [monitoring] in the model, manifests place it in [kanban-data]` |
| Hosts | new `hosts()` reader (regex ``/Host\(`([^`]+)`\)/g`` over `IngressRoute` `spec.routes[].match`), new `host` key in `METADATA_KEYS` | change the host literal in `k8s/overlays/prod/ingressroute.yaml` | two lines: a claimed host with no route, a routed host with no claim |

**Ordering constraint (important).** Each widening makes the baseline red until the elements it forces exist, and the pre-commit hook refuses a red tree. So a widening and its elements land in one commit. Phase 1 did the same for the `edge` fix. Record the "red on the real defect" output from the uncommitted intermediate state (reader landed, model not yet updated), as `01-01-edge-red.txt` did, then commit both together.

**"Not caught before" protocol.** Phase 1 used `SYNC=<old sync.mjs>` through `drill.fish` and recorded `01-drills-before.txt` (all 13 D10 to D16b sections `exit=0`). Run the new drills the same way against the Phase 1 `sync.mjs` and the Phase 1 model, before changing either. Against the new model, the old script reports false drift on the baseline (the new model claims `HelmRelease/...` that the old reader cannot see), so "before" must be recorded with the old model too. Write the drills first, record before, then build.

**Re-recording.** Every drill output line contains the tally (`N elements ... M workloads`), so adding elements changes every section of `drill-output.txt`. Re-record the whole file in the last plan and verify with `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` as Phase 1 did. The standard's prose counts ("27 sections", "23 drills", "16 cases") change too; prefer wording without counts, or recount.

**Two additional rules the success criteria need** (cheap, recommended, each with a drill):
1. No deployment-view edge label equals `[...]`. `view.edges()` on a computed deployment view exposes `label`; the real model today returns `[...]` for 4 edges [VERIFIED: ran against the real model]. This is a real red-then-green: the rule fails on the real defect until the titles are fixed.
2. Every dynamic view has at most 5 participants. `view.nodes()` on a dynamic view returns the participants (5 for `signin-ok`, 4 for `signin-bad-credentials`) [VERIFIED: ran].
Both implement ROADMAP success criteria 1 and 3, which otherwise rest on a manual look.

**Phase 1 review items worth closing inside this phase** (they sit in code this phase edits; dispositions are all still `open` in `01-REVIEW-DISPOSITION.md`):
- WR-03: an element with anchors but no instance is never placement-checked. Phase 2 adds many anchored elements; deleting an `instanceOf` would pass. Fix with one rule and one drill.
- WR-04: a trailing slash on a `k8s/` source empties the anchors silently. Normalise or report "k8s source with zero anchors".
- IN-03: `WORKLOAD_DIRS` exclusions are unexplained; the GATE-01 widening resolves it, so say why `controller-resources.yaml` is excluded in a comment.
- WR-06 does not bite, since Phase 2 adds no manifests.

## Decision points with trade-off matrices

For the planner to copy into PLAN.md (CLAUDE.md directive).

**D1. Removing `[...]` edge labels.**

| Approach | Pros/Cons | Why Picked |
|---|---|---|
| A. Give every relationship that merges onto one instance pair the same protocol-level title (for example `SQL over TCP` for `kanban.api -> postgres` and `kanban.consumer -> postgres`; one Kafka title for the publisher and DLT edges) | Pro: no new construct, no standard change; verified: edges became `SQL over TCP`, `Avro over Kafka protocol` [VERIFIED: spike]. Con: the `process` view loses the specific labels (`INSERT activity_log, idempotent on event_id`, `after retries`) unless moved into relationship descriptions | Recommended default |
| B. Add one extra relationship per pair tagged `#physical` and filter each deployment view with `exclude * -> * where tag is not #physical` | Pro: logical labels stay specific; verified the filter works and yields clean labels [VERIFIED: spike]. Con: a second authored relationship set that duplicates logical facts, every drawn edge needs the tag, and relationship tags go beyond "tags carry only cross-cutting facts" in the standard | Use only if the `process` view labels are judged too valuable to lose |
| C. Deployment-level relationship, direct logical relationship, or `include a -> b with { title }` | Con: each still printed `[...]` [VERIFIED: three spikes]. Documented as unsupported for deployment views | Rejected |

**D2. Where cert-manager, issuers and the host filter are drawn.** Keep exactly three `#physical` views (the criterion says "lists three"). Recommended: `production` (Traefik, apps, data, cert-manager with issuers, host filter, internet), `delivery`, `monitoring`. Alternative: a fourth `edge` physical view, cheaper on width but contradicts "three". Mixed-kind merged-edge styling is unverified (A5), so avoid placing sync and async relationships on one instance pair where the title also differs.

**D3. Init job kind.** (a) Reuse `job` and generalise its notation in `spec.c4`. (b) Add `initjob` and amend the closed kind list in the standard (`DIAGRAM_CONVENTIONS.md:64-66`). (a) recommended: one line, no closed-set change.

**D4. Notes.** (a) `note` element kind (verified). (b) description text only on the `activity_log` table. (a) satisfies "notes with `metadata.source`" as ROADMAP criterion 2 reads; (b) cannot carry its own source.

**D5. SCEN-06 composition.** (a) One 5-participant view without `client`. (b) Two views with `navigateTo`. (a) recommended; (b) if the planner wants the HTTP request drawn.

## Parallelism and file ownership

Shared and serial by nature: `sync.mjs` (every GATE-01 widening, the two new rules), `drill.fish`, `drill-output.txt` (final re-record), `spec.c4` (kinds, do first), `DIAGRAM_CONVENTIONS.md` (STD-01, last), and `deployment.c4` plus `views.c4` if edited directly. ROADMAP already says plans run one after another for this reason.

Can run in parallel, because they touch only new files and need no new element:
- SCEN-01, SCEN-02, SCEN-06 (existing elements only) in `scenarios-*.c4` files, once `spec.c4` is settled. They can start in wave 1.

Must wait for their elements:
- SCEN-03 and SCEN-04 after delivery elements; SCEN-05 after edge elements; SCEN-07 after the `b2` element and the `backup` relationships; PROC-01 and LOGI-01 need only `spec.c4` kinds and can run beside the physical work if they edit `model.c4` / the `process` and `data` views while the physical plans use per-family files.

Suggested plan slicing: P1 spec kinds, `production` include narrowing, `[...]` and participant rules, title unification (red on the real defect). P2 monitoring and cert-manager elements with the HelmRelease/ClusterIssuer reader (PHYS-03, PHYS-04 part, GATE-01 part). P3 Flux elements with the gotk-only reader and delivery view (PHYS-02, GATE-01 part). P4 host metadata, edge elements, host filter and internet (PHYS-04, GATE-01 hosts). P5 Process and Logical (PROC-01, LOGI-01). P6a SCEN-01/02/06; P6b SCEN-03/04 after P3; P6c SCEN-05 after P4; P6d SCEN-07 after P5. P7 STD-01 plus full `drill-output.txt` replay. P2 to P4 are serial because of `sync.mjs`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Parsing k8s YAML | regex over manifests | `parseAllDocuments` from `yaml`, via the existing `k8sDocs()` | Flux's exported manifests put `labels:` before `name:`, defeating the old regex (ARCHITECTURE G4) |
| Reading model structure | text parsing of `.c4` | `LikeC4.fromWorkspace` and `computedModel()` (`view.edges()`, `view.nodes()`, `instances()`) | already used by `check()` |
| Rendering charts or kustomize | `kustomize build` inside `check` | source reading | Phase 1 decision; needs a pinned kubectl download |
| Host extraction | a YAML-path walker per route shape | one regex over `match` strings | Traefik rules are strings; two shapes exist and both are `Host(...)` |
| Legibility judgement | a pixel rule in the standard | record natural widths in the evidence file | Open Question 7 default |

**Key insight:** the value of a check is that a deliberate break is seen red; spend effort on drills, not on clever readers.

## Common Pitfalls

### Pitfall 1: The existing `production` view already fails the `[...]` criterion
**What goes wrong:** ROADMAP criterion 1 says no label in any deployment view reads `[...]`; 4 of 9 `production` edges do today (prod and nonprod `kanban -> redpanda` and `kanban -> postgres`).
**Why:** a deployment edge merges every logical relationship between the instance pair (3 relations in the measured edge); different titles give `[...]`.
**Avoid:** decision D1; add the label rule; fix before adding more edges.
**Warning signs:** `view.edges()` labels include `"[...]"`; a rendered tag reading `[...]`.

### Pitfall 2: `include netcup.**` swallows new instances
**What goes wrong:** every instance added under `netcup` appears in `production` (measured: a Flux instance showed up there without being asked), ballooning the 1965 px view.
**Avoid:** replace the glob with an explicit list of namespace nodes in `production`; give `delivery` and `monitoring` their own lists. GATE-03 is satisfied by an instance appearing in any view.

### Pitfall 3: Duplicate workload names in `k8s/flux-system`
**What goes wrong:** `controller-resources.yaml` repeats the six Deployment names as a strategic-merge patch (`k8s/flux-system/kustomization.yaml:17-18`, `patches:`). Scanning the directory lets a rename in `gotk-components.yaml` pass because the patch still supplies the old name.
**Avoid:** scan the single file `gotk-components.yaml`, and make the drill rename it there.
**Warning signs:** a "rename a Flux controller" drill that exits 0 after the rule exists.

### Pitfall 4: `fresh()` does not copy new `source` paths
**What goes wrong:** `drill.fish:16-24` copies migrations, `application.properties`, Java, `k8s` and the model. A new element with `source '.github/workflows/deploy.yml'`, `infra/vm/k3s-host-firewall.sh` or `src/main/avro` makes every drill fail the `source ... does not exist` rule, baseline included, with a message unrelated to the drill.
**Avoid:** extend `fresh()` in the same plan that adds the first such `source`; check the baseline section stays `exit=0`. Phase 1's rule, restated: `fresh()` copies every directory a reader or `source` reads.

### Pitfall 5: Parent and child in one sequence view
**What goes wrong:** with `postgres` and `postgres.table_users` both participants, validation passes (`✓ Valid`) but prints "Sequence view does not support nested actors", and in the render the step to the parent is not drawn at all [VERIFIED: PNG inspected]. A direct step between parent and child is an error: "Invalid parent-child relationship".
**Avoid:** leaf participants only.

### Pitfall 6: Scenario steps are unchecked prose
**What goes wrong:** a dynamic step to an element with no relationship validates and renders [VERIFIED: spike: `kanban.api -> hostfilter` step with no relationship]. A step with no title takes the relationship's title, so editing a model label silently edits the scenario.
**Avoid:** explicit title on every step, an `include` list fixing lane order, and the line-cited evidence file the roadmap requires.

### Pitfall 7: Red-before needs the old model
See GATE-01 design. Recording the old script against the new model reports baseline drift, which is not "not caught".

### Pitfall 8: Public-surface leakage written into descriptions
Phase 4 scans the built artifact. Do not write: the VPS address, SSH key paths, in-cluster DNS names (they are in `app.yaml`, `backup-cronjob.yaml`, `postgres-exporter.yaml`, `datasources.yaml`), Secret names (`postgres-init`, `postgres-backup`, `grafana-admin`), the bucket name or endpoint, the age recipient, share ids. The existing label `PGHOST postgres.kanban-data` (`model.c4:91`) is one; replace it while editing the backup relationship (D1 also needs a single clean title there).

### Pitfall 9: Standard sentences that go stale
Phase 1 shipped with one (fixed by quick task 261027-r62). Sweep `docs/DIAGRAM_CONVENTIONS.md` for: line 105 "the monitoring stack (excluded, as in `production-host.mmd`)"; line 106 "Does not represent: internal calls"; lines 64-66 closed kind list (if kinds change); line 51 "No diagram embedded in the docs uses it yet"; lines 304-309 "the four diagrams with no LikeC4 counterpart yet: signup, error-status-split, push-to-image and inbound-packet-path" (all four are built in this phase); the Physical row provenance text; the counts in "Drills". Success criterion 5 requires that searching for `excluded, as in` finds nothing.

### Pitfall 10: Unverified numbers in labels
Existing labels carry `:443`, `:8080`, `Postgres 16`, `x86_64`, `401/403/400/409`. Do not add more (`:8081`, `9081`, "14", a cron time, a rate). Phase 2 may leave the existing ones alone; say so in the evidence file.

## Code Examples

### The label rule (shape only, uses the API verified in this session)
```js
// Source: computed-model API exercised in this session (view.edges(), edge.label)
function checkEdgeLabels(computed, problems) {
  for (const view of computed.views()) {
    if (!view.isDeploymentView?.()) continue
    for (const e of view.edges()) {
      if (e.label === '[...]') problems.push(`view ${view.id}: edge ${e.source.id} -> ${e.target.id} has merged label '[...]'`)
    }
  }
}
```
Sorting the output as the existing rules do keeps drill output byte-stable.

### The participant rule
```js
// Source: view.nodes() on a dynamic view returned the participants (signin-ok: 5)
for (const view of computed.views()) {
  if (view.isDynamicView?.() && [...view.nodes()].length > 5) problems.push(`dynamic view ${view.id} has more than 5 participants`)
}
```
The `isDynamicView` method name was not exercised; the session used `view._type`. Confirm the accessor before relying on it [ASSUMED, A6].

### Host reader
```js
// Source: Traefik IngressRoute match strings, e.g. k8s/overlays/prod/ingressroute.yaml:62
const HOST = /Host\(`([^`]+)`\)/g
function routedHosts(docs) {
  const out = new Set()
  for (const { o } of docs) {
    if (o.kind !== 'IngressRoute') continue
    for (const r of o.spec?.routes ?? []) for (const m of String(r.match ?? '').matchAll(HOST)) out.add(m[1])
  }
  return out
}
```

### Drill scaffolding for the host break
```fish
# Source: pattern of docs/architecture/drill.fish D12
echo "== D17: a claimed host is changed in an IngressRoute"
fresh
sed -i 's|kanban-board-rud-vlad-473-nonprod|kanban-board-rud-vlad-473-nonprod-x|' $scratch/k8s/overlays/nonprod/ingressroute.yaml
run
```
(Numbering continues from D16b; the last existing drill is D14c at `drill.fish:183`.)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Mermaid per diagram | One LikeC4 model, five viewpoints | Standard approved 2026-10-27 | Mermaid stays as parity baseline this milestone |
| `implicitViews` on | off by default | LikeC4 1.52.0 | A new view needs an explicit tag |
| `include a -> b with {}` in deployment views | not supported | still in 1.60.0 [VERIFIED: `with { title }` validated but changed nothing] | Cannot relabel a merged edge in the view |
| Sequence flow-control blocks | `parallel`/`par`, `alt`, `opt`, `loop`, `try` exist, experimental, `parallel` cannot nest | docs, 1.60.0 | Standard V1 still bans branches inside one view; not exercised here |

**Deprecated/outdated:** none relevant.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | GitHub-hosted `ubuntu-latest` runners are x86_64 | Facts: delivery | The runner node title (which must name an architecture) would be wrong; the only repo source is the Mermaid file |
| A2 | kube-state-metrics, kubelet and API-server monitors are chart defaults (not overridden in `kube-prometheus-stack.yaml`, which disables only controllerManager, scheduler, etcd, proxy at `:45-52`) | Facts: monitoring | The monitoring view may omit or over-claim scrape edges |
| A3 | A request with no session cookie issues no SQL under Spring Session JDBC | Facts: scenarios (error-401) | The "401 with no SQL" view would be false; verify with a query-count test or label authored |
| A4 | `svclb-traefik` hostPort pod and the DNAT chain behave as `INFRA_ARCHITECTURE.md:51-54` states (doc claim confirmed live there, not in this repo) | Facts: edge | The inbound path would show a layer that is not there |
| A5 | Mixed sync and async relationships merged into one deployment edge render acceptably | D2 | Edge style or label ambiguity in `production` |
| A6 | `isDynamicView` exists on the computed view class (the session used `view._type` and `view.isDeploymentView`) | Code Examples | Rule throws at runtime; check the accessor first |
| A7 | LikeC4 metadata on relationships works in 1.60 | Locked note | Not used; the roadmap says put metadata on elements |
| A8 | Natural width of a 5-participant sequence view stays near 1838 px | Scenarios | Evidence file records widths; no pass/fail number is set |

## Open Questions (RESOLVED)

Each question below carries its outcome as planned (plans 02-01 to 02-05, revision a2bcd24).

1. **Does the user want the planned `[...]` fix via identical titles (D1-A)?** (RESOLVED) Planned as D1-A+ in 02-01: one shared protocol title per merging instance pair, and the `process` view restores the specific labels with `include … with { title }`. Measured on a scratch copy: `production` has no `[...]` edge and `process` keeps its six labels.
2. **Is a developer actor wanted in `push-to-image`?** (RESOLVED) No. 02-05 draws push-to-image with repo, ci, dockerhub and postgres participants and no developer actor.
3. **Does `kanban.management` belong in the same view as Prometheus (Process) or only in Physical?** (RESOLVED) Same view, via a single scrape relationship from the Prometheus element (02-02). No split of the `process` view; its natural width is recorded in the 02-05 evidence file, not judged.
4. **Does the user accept closing WR-03 and WR-04 inside Phase 2?** (RESOLVED) WR-03 is folded into 02-01 (drill D19). WR-04 stays open: no Phase 2 element anchors on a `k8s/` directory source, and 02-05 records that disposition in `01-REVIEW-DISPOSITION.md`.
5. **Mixed-kind merged edges and `par`** (RESOLVED, not used) Unrendered and unneeded: no plan uses `par`, a parent-and-child participant pair, or a mixed-kind merged edge as a checked design. A5 and the Pitfall 5 note stay as unmeasured assumptions.
6. **PNG legibility judgement.** (RESOLVED) No rule exists (Open Question 7). Natural widths are recorded in the 02-05 evidence file only.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | `sync.mjs`, spikes | ✓ | v24.19.0 (nvm) | none needed |
| `docs/architecture/node_modules` (likec4, yaml) | all checks | ✓ | likec4 1.60.0, yaml 2.9.1 | `pnpm --dir docs/architecture install` |
| Chromium via likec4 (PNG export) | width recording, view inspection | ✓ | exported 10 views in 1 m 23 s | skip PNGs; read JSON only |
| fish | `drill.fish` | ✓ (used by Phase 1) | 4.2.1 per tooling inventory | none |
| Docker | pre-commit `fastTest` (Testcontainers) | not probed | n/a | `--no-verify` allowed for docs/config-only commits |
| Live cluster / SSH | not needed | not used | n/a | Phase 2 is repo-only |

**Missing dependencies with no fallback:** none.

## Validation Architecture

`workflow.nyquist_validation` is `false` in `.planning/config.json`, so this section would normally be skipped. It is included because the invoking request asked for it.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | none in the JS sense; the gates are `sync.mjs check`, `drill.fish`, `scripts/pre-commit-likec4-selftest.py`, `likec4 validate` |
| Config file | none |
| Quick run command | `node docs/architecture/sync.mjs check` (about 1.4 s measured) |
| Full suite command | `fish docs/architecture/drill.fish \| cmp - docs/architecture/drill-output.txt` and `python3 scripts/pre-commit-likec4-selftest.py` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PHYS-02/03/04 | three `#physical` deployment views; no `[...]`; all instances drawn | check + export | `node docs/architecture/sync.mjs check`; `likec4 export json --skip-layout` and read `views` | check yes; label rule Wave 0 |
| PROC-01, LOGI-01 | elements exist; `metadata.source` exists | check | `sync.mjs check` (source rule exists at `sync.mjs:538-541`) | yes |
| SCEN-01 to SCEN-07 | views exist, tagged `#scenarios`, at most 5 participants | check | `sync.mjs check`; participant rule Wave 0 | tag rule yes; participant rule Wave 0 |
| SCEN steps match code | prose cites code lines | manual evidence file | none | Wave 0 (evidence file) |
| GATE-01 | each break exits 1 | drill | `fish docs/architecture/drill.fish` | new drills Wave 0 |
| STD-01 | no `excluded, as in` sentence; rows match what is built | grep | `rg -n "excluded, as in" docs/DIAGRAM_CONVENTIONS.md` (expects no match) | n/a |

### Sampling Rate
- **Per task commit:** `node docs/architecture/sync.mjs check` and `likec4 validate docs/architecture/model`.
- **Per wave merge:** full drill replay and the hook self-test.
- **Phase gate:** drill replay byte-identical, self-test green, `likec4 export json` shows three `#physical` deployment views and no `[...]`, then `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] New drills D17 and up in `docs/architecture/drill.fish` (rename HelmRelease, rename Flux controller in `gotk-components.yaml`, change a claimed namespace, change a claimed host, a `[...]` break, a 6-participant break) and a "before" recording against the Phase 1 script and model.
- [ ] `fresh()` copies `.github`, `infra`, `src/main/avro` (and any other `source` path) before the first such `source` lands.
- [ ] Scenario evidence file with one cited line per step.
- [ ] Recount or reword the standard's prose counts.

## Security Domain

`security_enforcement` is true (ASVS level 1). Phase 2 adds no service, endpoint, credential or dependency; the live concern is disclosure through a model that Phase 3 will publish.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | none touched |
| V3 Session Management | no | scenarios describe existing behaviour only |
| V4 Access Control | no | none touched |
| V5 Input Validation | yes (drift checker) | `yaml` parser errors exit 4; never `eval` a manifest value; reader handles non-string `namespace` values |
| V6 Cryptography | no | the backup scenario only names age encryption |
| V8 Data Protection | yes | keep secrets, bucket name, endpoint, key material and in-cluster names out of model text and planning files |

### Known Threat Patterns for this stack
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Sensitive detail published through model descriptions or metadata | Information disclosure | fresh one-sentence descriptions; allowed host list is the three README hostnames; Phase 4 artifact scan |
| Credential-shaped strings in `.planning/` | Information disclosure | gitleaks in the pre-commit hook; no age recipient, bucket name or ids in prose |
| Checker reads attacker-shaped YAML | Tampering | existing fail-closed exit 4 for unreadable docs; do not widen to `eval` |
| Scratch directories outside the repo | n/a | keep drill scratch under `docs/architecture/out/` (gitignored) |

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `docs/architecture/sync.mjs`, `drill.fish`, `package.json`, `likec4.fish`, `model/{model,deployment,views,spec}.c4`; `docs/DIAGRAM_CONVENTIONS.md`; `.github/workflows/{deploy,backup-image,invariant-checks,uptime-check}.yml`; `k8s/flux-system/*` (all but `gotk-components.yaml`, which was parsed); `k8s/platform/**`; `k8s/monitoring/**` (YAML); `k8s/overlays/{prod,nonprod}/*`; `k8s/base/{app,redpanda}/*.yaml`; `k8s/data/postgres/{backup-cronjob.yaml,kustomization.yaml,backup/postgres-backup.sh}`; `infra/vm/k3s-host-firewall.sh`; Java: `AuthenticationController`, `UserService`, `OwnershipVerifierService`, `TaskService`, `KafkaEventPublisher`, `ActivityLogConsumer`, `ActivityLogRecorder`, `ProblemDetailAuthenticationEntryPoint`, `ErrorCode`, `GlobalExceptionHandler` (handler lines), `AvroSchemaRegistrar`, `ApiPaths`; `application.properties`; migration `V3`; `src/main/avro/AvroTaskMovedEvent.avsc`.
- Planning: ROADMAP.md, REQUIREMENTS.md, STATE.md, Phase 1 VERIFICATION, CONTEXT, REVIEW, REVIEW-DISPOSITION; workstream research SUMMARY, FEATURES, PITFALLS, ARCHITECTURE (first 260 lines).
- Measured here with the repo's own likec4 1.60.0 on scratch copies of the model (deleted after): label merge experiments, provider node, `extend`, non-Kubernetes instance, nested-actor sequence view, metadata array, `note` element, PNG renders, `view.edges()`/`view.nodes()` API.
- npm registry: `npm view likec4 version time.modified`, `npm view yaml version`.

### Secondary (MEDIUM confidence)
- Context7 `/likec4/likec4`: deployment model (`instanceOf` at any level, deployment relationships), deployment view predicates, dynamic-view syntax including `parallel`, `alt`, `navigateTo` and the "sequence supports only leaf participants" note.

### Tertiary (LOW confidence)
- Training knowledge: Spring Session behaviour for a cookie-less request (A3); GitHub-hosted runner architecture (A1); chart-default ServiceMonitors (A2).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing new, versions verified.
- Architecture (model and check design): MEDIUM-HIGH; the building blocks were each exercised, the full design was not built.
- Repo facts and line cites: HIGH, each read this session; the host-level layers beyond the filter script are doc claims.
- Pitfalls: HIGH for 1 to 5 (measured or parsed), MEDIUM for the rest.

**Research date:** 2026-10-28
**Valid until:** 2026-11-28 for repo facts; re-check if LikeC4 is upgraded past 1.60.0 (edge-label and sequence behaviours measured here are version-specific).
