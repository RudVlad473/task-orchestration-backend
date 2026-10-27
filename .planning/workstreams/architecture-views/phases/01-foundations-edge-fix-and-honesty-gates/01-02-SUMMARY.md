---
phase: 01-foundations-edge-fix-and-honesty-gates
plan: 02
subsystem: architecture-model
tags: [likec4, drift-check, kubernetes, kustomize, flux, drills]

requires:
  - phase: 01-01
    provides: "k8sDocs(), manifestNamespaces(), the namespace rule and the seven namespace nodes in deployment.c4"
provides:
  - "sync.mjs placement rule: per element, the namespaces its instances sit in must equal the namespaces its anchor documents land in"
  - "placements(docs): kustomization namespace followed through resources, HelmRelease in spec.targetNamespace, Flux source objects nowhere"
  - "METADATA_KEYS allowlist (source, workload) enforced on elements, deployment nodes, instances and relationships"
  - "instance view-membership rule: an instance drawn in no deployment view is named"
  - "drill.fish SYNC override and whole-k8s fresh(); drills D10, D10b, D11, D12, D12b, D13, D13b, D15, D16, D16b"
  - "01-drills-before.txt: every drill from D10 on recorded exit=0 under the pre-phase sync.mjs (68b8f3a)"
affects: [01-03, phase-2 physical view standard, phase-2 cert-manager and monitoring elements]

actuals:
  tokens: 6600
  tasks: 3
  commits: 3
plan_head_before: c3627b309197100eb62d728ce4a5ad475ccedfbf
plan_head_after: 4af2f0b7b812f45e7803101d8ef4a5c59fd4e045

tech-stack:
  added: []
  patterns: ["one check function per rule called from check()", "drill recorded twice: pre-phase script via SYNC override (not caught), current script (caught)", "placement resolved per document with a (file, document) function"]

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-drills-before.txt
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/drill.fish
    - docs/architecture/drill-output.txt

key-decisions:
  - "HelmRelease placement (A8): spec.targetNamespace when it is a string, else the inherited-then-metadata.namespace order every kind uses"
  - "Flux source objects (A9): a document whose apiVersion starts source.toolkit.fluxcd.io/ lands in no namespace"
  - "An outer namespaced kustomization wins over a nested one: nested kustomization directories reached from a namespaced root are not roots themselves"

requirements-completed: [GATE-04, GATE-02, GATE-03]

coverage:
  - id: D1
    description: "Placement rule: a moved instance (D11) and a HelmRelease-backed element drawn in flux-system (D16) are caught; the same element in its targetNamespace (D16b) and the real repo stay clean"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "docs/architecture/drill-output.txt D11 and D16 exit=1 with the named placement line, D16b exit=0; 01-drills-before.txt shows all three exit=0 under 68b8f3a; node docs/architecture/sync.mjs check exits 0 with the unchanged tally"
        status: pass
    human_judgment: false
  - id: D2
    description: "Namespace rule both-ways drills: deleted namespace node (D10) and misspelled manifest namespace (D10b) each exit 1 with named lines"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "drill-output.txt D10 (one line naming cert-manager) and D10b (three lines: kube-sytem has no node, kube-system defined by no manifest, traefik placement)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Widened workload scan: a Deployment added under k8s/platform is reported (D15)"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "drill-output.txt D15 exit=1 with `k8s Deployment/worker has no element carrying metadata workload`; before-run exit=0"
        status: pass
    human_judgment: false
  - id: D4
    description: "Metadata-key allowlist on all four surfaces"
    requirement: "GATE-02"
    verification:
      - kind: other
        ref: "drill-output.txt D12 (one element line) and D12b (node, instance, relationship lines), exactly 4 `carries metadata key` lines; before-run exit=0 for both"
        status: pass
    human_judgment: false
  - id: D5
    description: "Instance view-membership: an undrawn instance is named (D13) and the control with a relationship is clean (D13b)"
    requirement: "GATE-03"
    verification:
      - kind: other
        ref: "drill-output.txt D13 exit=1 naming netcup.k3s.data.lonely, D13b exit=0; D16 and D16b sections unchanged after the rule landed"
        status: pass
    human_judgment: false
  - id: D6
    description: "Pre-phase sections of drill-output.txt are untouched"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "git diff --numstat 68b8f3a -- docs/architecture/drill-output.txt prints 52 additions, 0 deletions"
        status: pass
    human_judgment: false

duration: about 30min
completed: 2026-10-27
status: complete
---

# Phase 01 Plan 02: Placement, metadata-key and view-membership rules Summary

**`sync.mjs check` now fails when an instance sits in a namespace the manifests do not place it in (kustomize namespaces, HelmRelease `spec.targetNamespace`, Flux source objects placing nothing), when a metadata key falls outside `source`/`workload` on any of four surfaces, and when an instance appears in no deployment view; ten drills are recorded not caught before and caught after.**

## Performance

- **Duration:** about 30 min (start time was not captured at executor start; the figure is an estimate)
- **Completed:** 2026-10-27
- **Tasks:** 3 of 3
- **Files modified:** 3 source files, 1 new evidence file (`01-drills-before.txt`)

## Accomplishments

- Placement rule (`placements()` and `checkPlacement()`): the resolver follows `resources` from every kustomization with a top-level `namespace`, skipping non-string, absolute and out-of-`k8s/` entries and guarding recursion with a visited set. A document lands, in order, nowhere (Flux source object), in `spec.targetNamespace` (HelmRelease that sets one), in the inherited namespaces, in its own `metadata.namespace`, or nowhere (cluster-scoped). Measured on the real repo: traefik [kube-system], kanban and redpanda [kanban-nonprod,kanban-prod], postgres and backup [kanban-data], all equal to the model, so the check still prints the unchanged `OK (33 elements ... 4 workloads)`.
- Metadata allowlist (`checkMetadataKeys()`): `METADATA_KEYS = ['source', 'workload']` below `VIEWPOINTS`; walks elements, deployment nodes, instances, relationships in that order, ids sorted, keys sorted; a relationship is written `<source> -> <target>`.
- View membership (`checkViewMembership()`): instance node ids in any view where `isDeploymentView()` is true; every other instance is named.
- Harness: `SYNC` override, `cp -r $repo/k8s $scratch/`, helper functions `certmgr_into` and `lonely_into` so the D16/D16b and D13/D13b pairs differ only in their one parameter.

## Task Commits

1. **Task 1: Placement rule, SYNC override, whole-tree fresh(), drills D10, D10b, D11, D15, D16, D16b** - `66a3626` (feat); full hook passed
2. **Task 2: Metadata-key allowlist, drills D12, D12b** - `5319f8c` (feat); full hook passed
3. **Task 3: Instance view-membership rule, drills D13, D13b** - `4af2f0b` (feat); full hook passed

**Plan metadata:** committed separately after this file (docs).

## Drill outcomes (as recorded)

| Drill | Before (68b8f3a) | After (current) |
|---|---|---|
| D10 deleted cert-manager node | exit=0 | exit=1, `namespace cert-manager is defined in k8s/platform/cert-manager/cert-manager.yaml but has no namespace node ...` |
| D10b kube-sytem | exit=0 | exit=1, three lines (no node for kube-sytem, kube-system defined by no manifest, traefik placement) |
| D11 backup in kanban-prod | exit=0 | exit=1, `placement: backup is deployed in [kanban-prod] in the model, manifests place it in [kanban-data]` |
| D12 `sourc` | exit=0 | exit=1, `element traefik carries metadata key 'sourc', not in the allowlist (source, workload)` |
| D12b node, instance, relationship keys | exit=0 | exit=1, three lines naming node, instance and `client -> traefik` |
| D13 lonely instance | exit=0 | exit=1, `instance netcup.k3s.data.lonely appears in no deployment view` |
| D13b lonely + relationship (control) | exit=0 | **exit=0** (OK tally, 34 elements) |
| D15 Deployment under k8s/platform | exit=0 | exit=1, `k8s Deployment/worker has no element carrying metadata workload 'Deployment/worker'` |
| D16 certmgr in flux-system | exit=0 | exit=1, exactly `placement: certmgr is deployed in [flux-system] in the model, manifests place it in [cert-manager]` |
| D16b certmgr in cert-manager (control) | exit=0 | **exit=0** (OK tally, 34 elements) |

D16 and D16b were recorded after Task 1 and again after Task 3; the sections are unchanged, so the relationship to traefik keeps certmgr drawn and the view-membership rule stays silent on it.

## Rules as built (for Phase 2 and plan 01-03)

- **A8:** a HelmRelease (`kind: HelmRelease`, apiVersion `helm.toolkit.fluxcd.io/`) with a string `spec.targetNamespace` lands there; without one it follows the inherited-then-`metadata.namespace` order every kind uses.
- **A9:** a document whose apiVersion starts `source.toolkit.fluxcd.io/` lands in no namespace.
- **A10 (accepted, unchecked):** a Flux Kustomization's `spec.targetNamespace` is not modelled; only a `kustomize.config.k8s.io` Kustomization's `namespace` is followed through `resources`.
- **A11 (accepted, unchecked):** a HelmRelease is checked by its declared `spec.targetNamespace`, never by the objects its chart renders.
- **A12 (accepted, unchecked):** an element with no anchor document (no `workload` metadata and no `k8s/` `source` that matches a parsed document) is not placement-checked, so a moved instance of it passes. Phase 2 elements carry a `workload` or `k8s/` `source` anchor, or the standard records them as not placement-checked.

## Decisions Made

- Nested kustomization directories reached from a namespaced kustomization are not treated as roots, so the outer namespace wins as in kustomize. A nested kustomization reached only from a non-namespaced parent is its own root.
- `checkPlacement` takes the `workload` anchor when the element has one and ignores its `source`; an element with only `source` uses the `k8s/` paths among them.
- The `lonely_into`/`certmgr_into` helpers keep each drill pair to one parameter instead of copying the scratch edit twice.

## Deviations from Plan

None - plan executed exactly as written. One internal correction before the first commit: the first draft of the resolver left an unused set and noted both an inner and an outer namespace for a nested kustomization; it was rewritten to the nested-roots form above before anything was recorded, so no recorded output reflects the draft.

## Issues Encountered

- The acceptance command `grep -c 'cp -r $repo/k8s $scratch/' docs/architecture/drill.fish` printed 0 in this environment (cause not investigated); the same text with `grep -cF` prints 1, and `grep -n 'cp -r' docs/architecture/drill.fish` shows the line at 22. Treated as a quoting artefact of the check, not a missing edit.
- `fastTest` and `spotlessCheck` printed `UP-TO-DATE` in all three hook runs: no Java or resource input changed, so Gradle reused the earlier result rather than re-running Testcontainers. The hook ran in full each time.
- The tally text still counts 33 elements on the real model; drills that add an element show 34.

## Known Stubs

None.

## Threat Flags

None. T-01-04 is mitigated in `placements()` (absolute entries and targets outside `k8s/` are skipped, only already-parsed documents are read), T-01-05 by the allowlist over four surfaces (D12, D12b), T-01-06 by the view-membership rule (D13, control D13b).

## Next Phase Readiness

Ready for 01-03: the standard's Checks section can state A10, A11 and A12 from the wording above, and the check now holds namespace, placement, metadata vocabulary and visibility for every element Phase 2 adds.

## Self-Check

- Key files exist: `docs/architecture/sync.mjs`, `docs/architecture/drill.fish`, `docs/architecture/drill-output.txt`, `01-drills-before.txt`, this file.
- Commits `66a3626`, `5319f8c` and `4af2f0b` are ancestors of HEAD.
- `git rev-list --count c3627b3..HEAD` from the ledger base: 3.
- Scratch under `docs/architecture/out/` removed with `trash-put`.

## Self-Check: PASSED
