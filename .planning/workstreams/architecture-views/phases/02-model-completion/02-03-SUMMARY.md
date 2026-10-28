---
phase: 02-model-completion
plan: 03
subsystem: architecture-views
tags: [likec4, sync-check, flux, cert-manager, ingressroute, delivery-view, drills]

requires:
  - phase: 02-model-completion
    provides: plan 02-01 label, participant and workload-instance rules and the per-namespace production include; plan 02-02 HelmRelease reader, cert-manager controller element and the monitoring view
provides:
  - sync.mjs reads the Flux controller Deployments from gotk-components.yaml, ClusterIssuers, and every Host literal in IngressRoute matches
  - delivery.c4 with repo, ci, dockerhub and flux elements, GitHub and Docker Hub provider nodes, and the `delivery` Physical view
  - edge.c4 with both ClusterIssuers, Let's Encrypt, the Netcup Cloud Firewall, the host filter and ServiceLB, and `production` extended with the inbound path
  - drills D22, D23 and D24, recorded before and after
affects: [02-04, 02-05]

actuals:
  tokens: 2083  # chars/4 over the added lines of sync.mjs, the model folder and drill.fish (8332 chars); recordings excluded
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "One element claims several workloads with a metadata array (flux claims six controller Deployments)"
    - "A reader scans one named file of a directory (gotk-components.yaml) because a sibling patch file repeats the names"
    - "Hosts are claimed on one element (traefik) and compared both ways with every Host() literal in IngressRoute matches"

key-files:
  created:
    - docs/architecture/model/delivery.c4
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-03-red.txt
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/model/model.c4
    - docs/architecture/model/edge.c4
    - docs/architecture/model/views.c4
    - docs/architecture/drill.fish
    - docs/architecture/drill-output.txt
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-drills-before.txt

key-decisions:
  - "Flux controllers: read the single file gotk-components.yaml, not the directory, so D22 is not masked by the patch file"
  - "The `production` view excludes the logical client to Traefik edge; the inbound path is drawn through the provider firewall and host filter"

requirements-completed: [PHYS-02, PHYS-04, GATE-01]

duration: not measured
completed: 2026-10-28
status: complete
plan_head_before: 7a8e2a94fe564d215c999812fa19c6bc79ee48b1
plan_head_after: 518eb75705cf54e939ef04d3ec82cc6ac9514e5a
---

# Phase 02 Plan 03: Flux controllers, ClusterIssuers and hosts read; the delivery view and the inbound path drawn Summary

**sync.mjs now compares Flux controller Deployments, ClusterIssuers and every routed hostname with the model, each seen red on the real model and drilled exit 0 before and exit 1 after; the model has exactly three Physical views (`delivery`, `monitoring`, `production`) with no `[...]` edge.**

## Commits

| Task | Commit | What |
|---|---|---|
| 1 (tracer) | 5073e86 | gotk-components.yaml in WORKLOAD_DIRS, delivery.c4, `delivery` view, 02-03-red.txt |
| 2 | e6a0ce3 | ClusterIssuer kind, `host` key, routedHosts and checkHosts, traefik host array, issuers and inbound path, `production` extended |
| 3 | 518eb75 | drills D22, D23, D24 and both recordings |

`commits: 3` is measured: `git rev-list --count 7a8e2a9..HEAD`.

## Red lines (02-03-red.txt)

- Task 1, reader changed and no element added: `exit=1`, six lines `k8s Deployment/<name> has no element carrying metadata workload 'Deployment/<name>'` for source-controller, kustomize-controller, helm-controller, notification-controller, image-reflector-controller and image-automation-controller (tally 46 elements, 15 workloads). After the `flux` element: `OK (50 elements ... 15 workloads)`, `exit=0`.
- Task 2, readers changed and no element or host added: `exit=1`, two ClusterIssuer lines (letsencrypt-staging, letsencrypt-production) and three `k8s IngressRoute routes host <h>, which no element claims in metadata host` lines (prod, nonprod and monitoring hostnames); tally 50 elements, 17 workloads. After the elements: `OK (56 elements ... 17 workloads)`, `exit=0`.

## Runner architecture evidence (assumption A1)

`gh auth status` exited 0. The newest deploy.yml run was 40499205436; its log (`gh run view --log`) matches `x64`, for example `Path: /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/21.0.12-1/x64` and `setup-java-Linux-x64-gradle-...` cache keys. The runner node's description cites that. This shows a 64-bit x86 Java toolchain on the runner; it is indirect evidence of the architecture, not a statement from the runner image itself.

## Edges as exported

`delivery` (eight edges, no `[...]`):
- github.repo -> github.runner.ci : push to main starts deploy.yml
- github.runner.ci -> docker-hub.dockerhub : push the app image
- netcup.k3s.flux-system.flux -> github.repo : Git over SSH: pull, commit tag bumps
- netcup.k3s.flux-system.flux -> docker-hub.dockerhub : scan image tags
- flux -> netcup.k3s.prod.kanban and netcup.k3s.nonprod.kanban : apply the app overlays
- prod.kanban and nonprod.kanban -> docker-hub.dockerhub : image pull

`production` (17 edges, no `[...]`), the new ones: internet.client -> netcup-cloud.firewall : HTTPS; firewall -> netcup.hostfilter : allowed traffic; hostfilter -> kube-system.svclb : new TCP on allow-listed ports; svclb -> kube-system.traefik : hostPort to Traefik; issuer-staging, issuer-production and cert-manager.certmanager -> letsencrypt.acme : ACME over HTTPS; letsencrypt.acme -> traefik : HTTP-01 challenge; traefik -> prod.kanban and nonprod.kanban : HTTP :8080. No internet.client -> traefik edge is drawn. Physical views exported: `delivery,monitoring,production`, 0 `[...]` edges.

## Drills D22, D23, D24

| Drill | Before (bc06665 script and model) | After (current) |
|---|---|---|
| D22 source-controller renamed in gotk-components.yaml | exit=0 | exit=1: `k8s Deployment/source-controllerx has no element carrying metadata workload 'Deployment/source-controllerx'` and `model claims workload Deployment/source-controller but k8s/ has no such manifest` |
| D23 letsencrypt-staging renamed | exit=0 | exit=1: the same two-line pair for `ClusterIssuer/letsencrypt-stagingx` and `ClusterIssuer/letsencrypt-staging` |
| D24 nonprod host changed | exit=0 | exit=1: `k8s IngressRoute routes host kanban-board-rud-vlad-473-nonprodx.duckdns.org, which no element claims in metadata host` and `model claims host kanban-board-rud-vlad-473-nonprod.duckdns.org but no IngressRoute routes it` |

D22 renames only in the generator file while controller-resources.yaml keeps the old name, and the check still reports the old name as missing, so the reader skips the patch file. Verified: D17 to D24 all exit=0 in 02-drills-before.txt; exit-code invariant against bc06665 prints 0; drill.fish and drill-output.txt each hold 36 sections; D12 and D12b list `(source, workload, host)`; `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` exits 0; leak gate over docs/architecture/model prints nothing. Tally in the recording reads 56 elements and 17 workloads except sections where the drill itself changes the model or manifests (D3 and D15 read 18 workloads, D4 reads 16, and D13, D13b, D16 and D16b read 57 elements).

## Deviations from Plan

None - plan executed exactly as written. Two small additions the plan did not spell out: a `\'` escape in the `production` description (the parser accepted it, and the export shows the apostrophe intact), and the `delivery` include list as `github, github.**, docker-hub, docker-hub.**` plus the plan's cluster entries.

## Not verified

- The Netcup Cloud Firewall, the host filter and ServiceLB are authored. Only the host filter script's existence is checked; the firewall ruleset and ServiceLB's pod are not readable from the repo (threat T-02-08, accepted).
- Which instance serves which host stays authored: hosts are claimed on traefik only (plan trade-off).
- A rename only in controller-resources.yaml is not caught by the workload rule (plan option A limit).
- The runner architecture is inferred from toolchain paths in one run log, as stated above.
- The delivery edges (Git over SSH, tag scan, image pull) come from reading image-automation.yaml, gotk-sync.yaml and deploy.yml by hand; the check does not verify them.
- Natural width of `production` after adding about eight nodes was not measured here; plan 05 records it.
- Duration was not measured.

## Known Stubs

None.

## Threat Flags

None. T-02-06 mitigated (only the three README-linked hostnames written; leak gate prints nothing), T-02-07 mitigated (D22 caught while the patch file holds the old name), T-02-08 accepted as stated, T-02-SC no package installed.

## Self-Check: PASSED

Commits 5073e86, e6a0ce3 and 518eb75 are ancestors of HEAD (verified below); delivery.c4 and 02-03-red.txt exist.
