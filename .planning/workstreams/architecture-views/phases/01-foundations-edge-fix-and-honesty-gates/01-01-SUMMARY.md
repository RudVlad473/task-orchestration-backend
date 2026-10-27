---
phase: 01-foundations-edge-fix-and-honesty-gates
plan: 01
subsystem: architecture-model
tags: [likec4, yaml, kubernetes, drift-check, pnpm]

requires: []
provides:
  - "sync.mjs namespace rule: every namespace a k8s manifest defines needs a namespace node, and every namespace node needs a defining manifest, both reported by name"
  - "yaml@2.9.1 pinned exactly in docs/architecture and loaded lazily beside likec4 (missing install still exits 3)"
  - "k8sDocs(): one parsed document list for k8s/**, exit 4 on a document with parse errors"
  - "deployment.c4 with Traefik in kube-system and empty flux-system, cert-manager, monitoring nodes (seven namespace nodes)"
  - "workloads() read through the parser over k8s/base, k8s/data, k8s/platform; render() split into four per-source functions"
affects: [01-02, 01-03, phase-2 physical view standard]

actuals:
  tokens: 6200
  tasks: 3
  commits: 2
plan_head_before: b38974865baecd175b2644fa009d1f1ad748f2ca
plan_head_after: ecf78b35a54c1bb17d2e6729eb264f4706a08361

tech-stack:
  added: ["yaml@2.9.1 (devDependency of docs/architecture, exact pin)"]
  patterns: ["parse k8s/** once per check and derive fact sets from the document list", "lazy dynamic import of tooling so a missing install exits 3, not 1", "render() composed from one function per fact source"]

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-yaml-lockfile.txt
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-edge-red.txt
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-edge-green.txt
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-port-parity.txt
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/model/deployment.c4
    - docs/architecture/package.json
    - docs/architecture/pnpm-lock.yaml

key-decisions:
  - "Accepted pnpm's peer re-key of vite/likec4 in the lockfile (user decision, option A) and replaced the plan's zero-deletion check with a mechanical suffix-only pairing check"
  - "Namespace node id equals the namespace name (kube-system, flux-system, cert-manager, monitoring); existing prod, nonprod, data ids kept"
  - "Red state on the real defect is an evidence file, never a commit"

requirements-completed: [PHYS-01, GATE-04]

coverage:
  - id: D1
    description: "Namespace rule goes red on the real edge defect and green after the model fix"
    requirement: "PHYS-01"
    verification:
      - kind: other
        ref: "node docs/architecture/sync.mjs check (01-01-edge-red.txt exit=1 naming netcup.k3s.edge; 01-01-edge-green.txt exit=0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Production export lists netcup.k3s.kube-system with traefik, no node id ends in .edge, and exactly the seven namespace titles exist"
    requirement: "PHYS-01"
    verification:
      - kind: other
        ref: "likec4 export json --skip-layout, output appended to 01-01-edge-green.txt"
        status: pass
    human_judgment: false
  - id: D3
    description: "Missing yaml install exits 3 with the install command; hook selftest 15/15"
    verification:
      - kind: integration
        ref: "python3 scripts/pre-commit-likec4-selftest.py (15/15 passed, run before and after Task 3); scratch copy of sync.mjs without yaml printed the install command and exited 3"
        status: pass
    human_judgment: false
  - id: D4
    description: "workloads() port and render() split change no output"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "01-01-port-parity.txt: drill-output.txt and generated.c4 byte-identical, OK tally unchanged (4 workloads)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Namespace rule covers k8s/platform (cert-manager reachable only via spec.targetNamespace)"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "edge-red.txt names cert-manager from k8s/platform/cert-manager/cert-manager.yaml; both-ways drills for a removed node and an undefined namespace are plan 01-03"
        status: pass
    human_judgment: true
    rationale: "The model-node-with-no-manifest and manifest-with-no-node directions were each seen red on the real repo, but the scratch-copy drills that pin them as regressions belong to plan 01-03; GATE-04 as a whole also needs placement (plan 01-02)."

duration: 25min
completed: 2026-10-27
status: complete
---

# Phase 01 Plan 01: Namespace rule, edge fix and YAML-parsed workloads Summary

**`sync.mjs check` now compares every namespace the k8s manifests define with the deployment model's namespace nodes in both directions, was red on the real `edge` defect, and landed green with Traefik moved to `kube-system` and the parser (yaml@2.9.1) pinned and lazily loaded.**

## Performance

- **Duration:** about 25 min for this executor (the Task 1 gate and the first install attempt ran in an earlier executor and are not counted)
- **Completed:** 2026-10-27T17:57Z
- **Tasks:** 3 of 3 (Task 1 approved by the user before this run)
- **Files modified:** 4 source files, 4 evidence files

## Accomplishments

- Namespace rule: `k8sDocs()` parses every YAML document under `k8s/` once; `manifestNamespaces()` collects `Namespace` names plus any string `namespace` or `targetNamespace` value at any depth (cert-manager appears only as `spec.targetNamespace`). Two problem-line shapes, sorted by namespace name, no allowlist.
- Red on the real defect, recorded before the fix: exit 1, `deployment node netcup.k3s.edge names namespace 'edge'` plus four `has no namespace node` lines (cert-manager, flux-system, kube-system, monitoring). Green after: exit 0 and the unchanged tally `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads)`.
- Export check: production view lists `netcup.k3s.kube-system` and `netcup.k3s.kube-system.traefik` with no `.edge` id; the deployment model holds exactly `cert-manager,flux-system,kanban-data,kanban-nonprod,kanban-prod,kube-system,monitoring`.
- `workloads()` reads `k8s/base`, `k8s/data` and `k8s/platform` through the parser (same four workloads); `render()` is `renderTables`, `renderForeignKeys`, `renderPackages`, `renderPackageEdges`. The re-recorded `drill-output.txt` and the regenerated `generated.c4` are byte-identical to the committed files.

## Task Commits

1. **Task 1: Approve yaml@2.9.1** - no commit (blocking-human gate, approved by the user)
2. **Task 2: Namespace rule, edge fix, parser pin (tracer)** - `0c615bd` (feat); full pre-commit hook passed
3. **Task 3: Parser-based workloads() and split render()** - `ecf78b3` (refactor); full pre-commit hook passed

**Plan metadata:** committed separately after this file (docs).

## Lockfile evidence

- `yaml@2.9.1` integrity in `pnpm-lock.yaml` is `sha512-3NxN8+78OdzbT7C/WjGsyfPAtJaN3FNDsWxv7Y7mcDsT/oOmgW8BpyQQFFBnvZE3j9Y2Sdz1ULFLezL7Eb2yFw==`, matching the approved string (1 occurrence); `npm view` shows `dependencies` empty and no preinstall, install or postinstall script.
- Lockfile diff: 21 lines added, 10 deleted (`pnpm-lock.yaml`), plus 2 added, 1 deleted in `package.json`. Mechanical check: all 10 deleted lines pair with an added line that differs only by the `(yaml@2.9.1)` suffix; the remaining 11 added lines are exactly the new importer entry (3), the `yaml@2.9.1` package entry (4 plus a blank), the `yaml: 2.9.1` peer line under vite, and the `yaml@2.9.1: {}` snapshot (plus a blank). No version moved.

## Decisions Made

- Node ids equal the namespace name (plan assumption A4).
- The OK tally text was not changed (plan assumption A5).
- No separate red commit (A7): the red state lives in `01-01-edge-red.txt`.

## Deviations from Plan

**1. [User-approved - lockfile check relaxed] pnpm re-keys vite's optional yaml peer**
- **Found during:** Task 2 (install), by the earlier executor; user chose option A.
- **Issue:** the plan's check (zero deleted lines, only `yaml@2.9.1:` and `yaml@2.9.1: {}` as new two-space keys) cannot hold: pnpm adds a `(yaml@2.9.1)` suffix to the vite, likec4, `@vitejs/plugin-react` and `vite-plugin-singlefile` snapshot keys and importer version.
- **Fix:** enforced the relaxed check instead (integrity matches; one new package, one new importer; every other changed line is suffix-only or the `yaml: 2.9.1` peer line; no version moves). The header of `01-01-yaml-lockfile.txt` states the acceptance and the reason.
- **Commit:** `0c615bd`

**2. [Rule 3 - minor] Installed-module name taken from the Node error**
- **Found during:** Task 2 (`loadTooling`).
- **Issue:** one guard now covers two modules, so a fixed message could name the wrong one.
- **Fix:** the message uses the module name from the `ERR_MODULE_NOT_FOUND` text (falls back to "a required module"). Checked by hiding `yaml` in a scratch copy: it printed `yaml is not installed for docs/architecture. Run: pnpm --dir docs/architecture install` and exited 3.
- **Commit:** `0c615bd`

**3. [Plan check not run as written] The automated verify for the lockfile and `git log -1` checks**
- The `git diff 68b8f3a` lockfile commands were replaced by the relaxed check above. The "one commit" check was confirmed with `git log -1 --format=%s -- docs/architecture/sync.mjs` returning the Task 3 subject after Task 3, and `git show --stat 0c615bd` listing the rule, model fix and lockfile together.

**Total deviations:** 1 user-approved, 1 auto-fixed (Rule 3), 1 verification substitution. **Impact:** none on behaviour; the lockfile diff is larger than planned but fully accounted for.

## Issues Encountered

- `fastTest` and `spotlessCheck` printed `UP-TO-DATE` in both commits' hook runs: no Java or resource input changed, so Gradle reused the earlier result rather than re-running Testcontainers. The hook still ran in full.
- A missing `k8s/` directory is not guarded yet: `readdirSync` would throw (exit 1 with a stack). The `k8s fact source missing` exit 4 is plan 01-03 by the plan's own interface list.

## Known Stubs

None.

## Threat Flags

None. No new network, auth or trust-boundary surface beyond the threat register (T-01-01 mitigated by the gate, exact pin and integrity check; T-01-02 by exit 4 on parse errors; T-01-03 by the lazy import and selftest 09/11).

## Next Phase Readiness

Ready for 01-02: `k8sDocs()` and `manifestNamespaces()` are the shared fact source for the placement rule, and `workloads(docs)` takes the same list.

## Self-Check

- Key files exist: `docs/architecture/sync.mjs`, `docs/architecture/model/deployment.c4`, `docs/architecture/package.json`, `docs/architecture/pnpm-lock.yaml`, the four evidence files, this file.
- Commits `0c615bd` and `ecf78b3` are ancestors of HEAD.
- `git rev-list --count b389748..HEAD` as measured from the ledger base: 2.

## Self-Check: PASSED
