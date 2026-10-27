# Phase 1: Foundations, Edge Fix and Honesty Gates - Research

**Researched:** 2026-10-27
**Domain:** LikeC4 1.60.0 computed-model API, a Node ESM drift script (`docs/architecture/sync.mjs`), k8s/Flux YAML fact reading with the `yaml` package, fish drill harness, git pre-commit hook selftest
**Confidence:** HIGH for everything measured in this session (every number below was produced by a command run against the real repo or a scratch copy under `docs/architecture/out/`, since deleted); MEDIUM for the placement-rule design (a recommendation that was prototyped and run, not a locked decision)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Namespace source rule (GATE-04)
- **D-01:** A namespace is "defined" if it is a `Namespace` kind or appears in any `namespace:` field under `k8s/**` (including `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`). This covers the four in `k8s/platform/namespaces.yaml` plus `kube-system`, `flux-system` and `cert-manager`.
- **D-02:** The check runs both ways: a manifest namespace with no model node fails, and a model namespace node with no manifest fails. A typo'd `namespace:` in a manifest reads as an undefined namespace and fails; that is the intended catch. No hand-kept allowlist.
- **D-03:** The placement check reads `k8s/platform` as well as `k8s/base` and `k8s/data`.

### Parser
- **D-04:** Add the `yaml` dependency to `docs/architecture/package.json` and use it for the new readers. `workloads()` is ported to `yaml` in this phase too (user chose B over leaving it on its regex until GATE-01). That also widens `workloads()` to read `k8s/platform`. Cost accepted: Phase 1's diff touches an existing reader.
- **D-05:** Porting `workloads()` must not change its current output on the unmodified repo (the baseline tally still reports 4 workloads unless `k8s/platform` adds Deployments, StatefulSets or CronJobs, in which case the model needs elements for them or the check goes red). The planner measures this before committing the port. Existing drills D3 and D4 must still pass unchanged.
- **D-06:** Split `render()` here (ROADMAP planning note, ARCHITECTURE build order M-A).

### Namespaces with no instances (Open Question 6)
- **D-07:** `cert-manager`, `flux-system` and `monitoring` get namespace nodes in Phase 1 with no instances, checked against the `likec4 export json` model export only. They may not render until Phase 2 adds instances. No placeholder instances, because a placeholder draws a fact that does not exist.

### View-membership rule (GATE-03)
- **D-08:** Fail on any deployment instance absent from every computed deployment view, whatever the cause (symptom-based, not "has no relationship"). It reads the computed model that `check` already loads. The failure names the instance.

### Metadata-key allowlist (GATE-02)
- **D-09:** The allowlist is a constant in `sync.mjs`, alongside `VIEWPOINTS`. Keys in use today: `source` and `workload` (measured in `model.c4`). An unknown key fails and names the key and its element. Adding a key means editing the script, deliberately.

### Drills, hook and selftest (locked by roadmap criteria, not discussed)
- **D-10:** Each new rule gets the next numbered drill in `docs/architecture/drill.fish` (D10 and up), with recorded output showing the break not caught before the rule exists and caught after it.
- **D-11:** `fresh()` copies every directory the new readers read. A reader pointed at a missing directory exits non-zero rather than reporting clean.
- **D-12:** `.githooks/pre-commit` triggers on `k8s/platform`, and `scripts/pre-commit-likec4-selftest.py` passes with more than the 15 cases it had at research time.
- **D-13:** The first check added is the namespace check, run against the model as it stands, so it goes red on the real `edge` defect before Traefik is moved (success criterion 1).

### Claude's Discretion
- Exact drill numbering and wording, the failure-message text, and how the namespace and placement readers are factored inside `sync.mjs`.

### Deferred Ideas (OUT OF SCOPE)
None - discussion stayed within phase scope. Roadmap Open Questions 1 to 5, 7 and 8 belong to Phases 2 to 4.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PHYS-01 | The Physical view places Traefik in `kube-system` (not the nonexistent `edge` namespace) and every namespace the manifests define exists in the model. | Section "Measured facts" M1, M5, M8: the seven manifest namespaces, the post-fix `deployment.c4` that validates and passes, hyphenated node ids work, the export shape to check against. |
| GATE-02 | An unknown metadata key fails the check. | M3: `sourc` validates silently; metadata exists on four surfaces (elements, deployment nodes, instances, relationships), so the allowlist must walk all four. |
| GATE-03 | A view-membership check catches an element that silently vanishes from its view. | M2: `computed.views()` filtered by `isDeploymentView()`, node `kind === 'instance'`; measured with and without an unconnected instance; an empty namespace node is also undrawn and must NOT be required in a view. |
| GATE-04 | A namespace and placement check covers `k8s/platform`, not only `k8s/base` and `k8s/data`. | M5, M6, M9: the namespace census, the kustomization-resolving placement derivation (prototyped, with real red/green output), and the `cert-manager` trap (only reachable through `spec.targetNamespace`). |
</phase_requirements>

## Summary

The five things the planner most needs, all measured here. (1) The seven namespaces the manifests define are exactly `kanban-prod`, `kanban-nonprod`, `kanban-data`, `monitoring`, `flux-system`, `kube-system` and `cert-manager`, but `cert-manager` appears under no `namespace:` key anywhere: it is only `spec.targetNamespace: cert-manager` in `k8s/platform/cert-manager/cert-manager.yaml:38`. A reader that follows D-01's wording literally ("any `namespace:` field") finds six and then fails the model-to-manifest direction on the correct `cert-manager` node. The reader must also read `targetNamespace`. (2) There is no placement check today: `workloads()` compares the workload name set only. "Placement" must be built, and the cheapest correct design is a small kustomization resolver (a `kustomization.yaml` top-level `namespace:` reaches every file it lists, through nested kustomizations) compared by set equality per model element. I prototyped it; on the real repo it is green on the fixed model and red with the named finding on a moved instance. (3) A static `import 'yaml'` at the top of `sync.mjs` breaks the hook selftest: with the package unresolvable the script dies with an uncaught `ERR_MODULE_NOT_FOUND`, exit 1, no install message, where `loadLikeC4()` gives exit 3 plus the install command (measured). `yaml` must be loaded the same lazy way as `likec4`. (4) The metadata and view-membership rules are small and the API for both is documented and measured; relationship-level metadata IS supported in 1.60 (ROADMAP lists it as unverified), so an allowlist that only walks elements leaves a hole. (5) `fresh()` omits `k8s/overlays`, `k8s/monitoring` and `k8s/flux-system`. Removing the last two from a scratch root does not turn the prototype red (all seven namespaces are defined in `k8s/platform` alone), so D-11's "missing directory must not report clean" needs an explicit required-directory guard, not reliance on `readdirSync` throwing.

The `workloads()` port is safe: the regex reader and a `yaml` reader return the identical four workloads, and widening to `k8s/platform` (and even `k8s/monitoring`) adds none (M7). The baseline check exits 0 with the recorded tally, and the full drill output is byte-identical to `docs/architecture/drill-output.txt` (M10).

**Primary recommendation:** Land in this order, each step green before the next: (a) `yaml` dependency + lockfile + lazy loader + `workloads()` port + `render()` split, all behaviour-preserving and proven by a byte-identical `drill-output.txt` and a zero-diff `generated.c4`; (b) the namespace check, shown red on the real `edge` defect in the working tree, then landed in ONE commit with the model fix (a red commit is refused by the hook and fails CI); (c) placement; (d) metadata allowlist over four surfaces; (e) instance view-membership; (f) drills D10+ with before/after output, `fresh()` and required-directory guard, hook trigger, one new selftest case.

## Project Constraints (from CLAUDE.md)

Extracted from `./.claude/CLAUDE.md` and the user's global instructions. Treat as locked.

- **GSD workflow only**: file edits start through a GSD command; this research writes only `01-RESEARCH.md`.
- **Before creating or approving any PLAN.md**: document 2 alternate technical approaches, a 3-column Trade-off Matrix `[Approach | Pros/Cons | Why Picked]`, and non-obvious performance/state-invalidation trade-offs. Never auto-execute code blocks without explaining the core data-flow in 3 sentences or less. Source `.dev/gsd-run.sh` instead of pasting the resolver.
- **Every commit runs `.githooks/pre-commit` (about 4 minutes)**: gitleaks on the staged diff, comment lint, instruction-file budget, `spotlessCheck`, `fastTest`. `git commit --no-verify` is allowed for docs, planning and config-only commits, NOT for feature-heavy commits (Java code, migrations, k8s manifests). Phase 1 touches no Java, migration or k8s manifest, so `--no-verify` is permitted, but it also skips the LikeC4 step; do not use it to land a rule whose proof is the hook.
- **Comment lint (`scripts/verify-comments.py`) rejects planning-system ids** (D-xx, GATE-xx, PHYS-xx, "Phase 1", "Pitfall 2") in comments of in-scope files. `.mjs` and `.fish` are out of its scope (`classify()` returns None for them, `scripts/verify-comments.py:127-166`), but `.githooks/pre-commit` (shell) and `scripts/*.py` are in scope: no planning ids in the hook comment or the selftest. Comments are plain text (`docs/CODE_STYLE.md` rule 14).
- **Wiki skill**: `docs/` other than `docs/wiki` stays authoritative; edit `docs/DIAGRAM_CONVENTIONS.md`, not wiki copies.
- **Global**: a bug fix or new rule is pinned by a test that fails without it (run it against unfixed code and say which direction was checked); shell scripts are fish; use `Edit` not heredoc scripts; commit finished work on the current branch with `git commit -- <paths>`; never push; leave scratch only inside the repo and delete it when done; memory note "Infra diagrams go stale silently" (see Pitfall 8).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Namespace / placement / metadata / view-membership rules | Drift script (`docs/architecture/sync.mjs`, Node ESM) | Model files (`model/*.c4`) | The script reads facts from k8s YAML and the computed LikeC4 model and compares; the DSL cannot declare allowed metadata keys (`docs/DIAGRAM_CONVENTIONS.md:199`, row V2). |
| Fact sources (what is "defined") | Repo files (`k8s/**` YAML) | — | Facts are read at check time from the working tree (`--root`), never cached. |
| Proof that each rule fires | Drill harness (`docs/architecture/drill.fish`) on a scratch copy | Hook selftest (`scripts/pre-commit-likec4-selftest.py`) | Drills prove rules on scratch copies; the selftest proves the hook triggers on a path. |
| Enforcement points | Pre-commit hook (`.githooks/pre-commit`) | CI job `likec4-model-drift` (`.github/workflows/invariant-checks.yml:137-167`) | Hook is fast local refusal on trigger paths; CI is the backstop on the committed tree. |
| Published Physical view | LikeC4 model (`model/deployment.c4`, `model/views.c4`) | — | The `edge` defect is a model-text error. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| likec4 | 1.60.0 (already pinned, exact) | Model parse, `computedModel()`, `export json` | Already the project's tool; `docs/architecture/package.json` pins it exactly. [VERIFIED: docs/architecture/package.json] |
| yaml (eemeli/yaml) | 2.9.1 (latest; `next` is 3.0.0-2, do not use) | Multi-document k8s YAML parsing with errors collected per document | User-locked D-04. `npm view yaml version` returned `2.9.1`, `time.modified` 2026-09-11, `engines.node >= 14.6`, repository `git+https://github.com/eemeli/yaml.git`, no dependencies, no `postinstall`. [VERIFIED: npm registry, run this session] |
| Node | 24.19.0 locally and in CI | Runtime | `node --version` printed `v24.19.0`; CI pins `node-version: '24.19.0'` (`invariant-checks.yml:157`). [VERIFIED] |
| pnpm | 11.20.0 | Install | `pnpm --version` printed `11.20.0`; `packageManager: "pnpm@11.20.0"`. [VERIFIED] |

### Supporting
None. Do not add a kustomize library or `kubectl` (ARCHITECTURE N3 already rejects a `kubectl kustomize` render inside `sync.mjs`).

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `yaml` parse of kustomization files | Shell out to `kubectl kustomize` | Needs a pinned kubectl download; rejected in ARCHITECTURE N3. |
| Per-element set-equality placement | Only "instance parent title is in the defined set" | Cannot catch "move one instance into the wrong namespace" (success criterion 2); the defined-set check alone passes a moved instance. |

**Installation (real change, run by the executor):**
```bash
pnpm --dir docs/architecture add -D --save-exact yaml@2.9.1
```
Commit `docs/architecture/package.json` and `docs/architecture/pnpm-lock.yaml` together: CI runs `pnpm --dir docs/architecture install --frozen-lockfile` (`invariant-checks.yml:160`) and fails on a lockfile that disagrees with the manifest. Measured lockfile delta for the add (13 lines): an importer entry `yaml: specifier ^2.9.1 / version 2.9.1`, a `packages:` entry `yaml@2.9.1` with `resolution: {integrity: sha512-3NxN8+78OdzbT7C/WjGsyfPAtJaN3FNDsWxv7Y7mcDsT/oOmgW8BpyQQFFBnvZE3j9Y2Sdz1ULFLezL7Eb2yFw==}`, `engines: {node: '>= 14.6'}`, `hasBin: true`, and a `snapshots:` entry `yaml@2.9.1: {}`. `pnpm-lock.yaml:590` and `:614` already mention `yaml` only as an optional peer of another package, so today `import 'yaml'` from `sync.mjs` does not resolve (pnpm's strict layout). [VERIFIED: pnpm-lock.yaml:584-615 read this session; lock diff produced by a real `pnpm add`, then reverted]

**Pnpm trap (measured):** running `pnpm --dir <subdirectory of docs/architecture> add yaml` makes pnpm treat `docs/architecture/pnpm-workspace.yaml` as the workspace root and writes an `out/...` importer into the real `pnpm-lock.yaml`. I reverted it with `git checkout -- docs/architecture/pnpm-lock.yaml` (tree clean afterwards). Run the add against `docs/architecture` itself, and `git diff --stat` the lockfile afterwards: it must show only the `yaml` lines.

**Version verification:** `npm view yaml version` -> `2.9.1`; `npm view yaml time.created` -> `2011-04-15T16:25:52.216Z`; `https://api.npmjs.org/downloads/point/last-week/yaml` -> `179058770` for the window 2026-06-25 to 2026-07-01 (the API returned that window; the figure is a weekly order of magnitude, not a current count).

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| yaml | npm | created 2011-04-15 (about 15 yrs) | about 179M/wk (API window 2026-06-25..07-01) | github.com/eemeli/yaml (from `npm view yaml repository.url`) | `SUS` from the seam, see note | Approved by the user (D-04); planner adds a cheap `checkpoint:human-verify` for the lockfile diff |

**Seam output (verbatim verdict):** `gsd_run query package-legitimacy check --ecosystem npm yaml` returned `verdict: "SUS"` with every signal `null` (`exists`, `publishedAt`, `weeklyDownloads`, `repoUrl`) and reasons `unknown-age`, `unknown-downloads`, `no-repository`. All-null signals mean the seam made no observation (its lookup returned nothing); they do not describe the package. My own registry queries (table above) contradict each reason. `npm view yaml scripts.postinstall` printed nothing and the package has no dependencies. Per the protocol a `SUS` verdict still requires a human-verify checkpoint before install; the install is cheap and user-chosen, so the checkpoint is: after `pnpm add`, the lockfile diff shows exactly `yaml@2.9.1` with the integrity quoted above and nothing else.

**Packages removed due to SLOP verdict:** none
**Packages flagged SUS:** yaml (no-observation SUS, see above)

## Architecture Patterns

### System Architecture Diagram

```
  repo files (working tree)                         docs/architecture/model/*.c4
  --------------------------                        ----------------------------
  k8s/** *.yaml  (7 subtrees)                       spec.c4 model.c4 deployment.c4
  src/main/... (existing readers)                   views.c4 generated.c4
        |                                                   |
        v                                                   v
  required-dir guard  --missing--> exit non-zero    LikeC4.fromWorkspace(modelDir)
        |                                              |  getErrors()  (validates syntax only)
        v                                              v
  yaml.parseAllDocuments (per file)               computedModel()
   doc.errors? -> fail loudly                        .elements()  .relationships()
        |                                            .deployment.nodes()/instances()
        v                                            .views() -> deployment views -> nodes()
  manifest facts:                                         |
   - defined namespaces (Namespace.name, any string      |
     `namespace`/`targetNamespace` key)                  |
   - placement: file -> namespaces                       |
     (kustomization.namespace through resources)         |
   - workloads Deployment/StatefulSet/CronJob            |
        \_____________________  ____________________/
                              \/
                  check(): problems[] strings
      namespace(both ways) | placement(set equality per element)
      metadata keys (4 surfaces) | instance in some deployment view
      + existing: generated.c4 stale, viewpoints, topics, workloads, source exists
                              |
                    exit 0 OK(tally) / exit 1 DRIFT(list) / exit 3 tooling
                              |
        pre-commit hook (trigger paths)      CI job likec4-model-drift
        drill.fish (scratch copies)          scripts/pre-commit-likec4-selftest.py
```

### Recommended Project Structure
No new files are required by the rules. Keep everything in `docs/architecture/sync.mjs` (it is the established single seam) and factor by function: `needDir`, `k8sDocs`, `manifestNamespaces`, `placements`, `deployedNamespaces`, one check function per rule, called from `check()`. `render()` becomes a composition of per-source functions returning sorted line arrays (ARCHITECTURE.md:88-89).

### Pattern 1: Load `yaml` the way `likec4` is loaded (lazy, exit 3)
**What:** `loadLikeC4()` (`sync.mjs:28-36`) turns `ERR_MODULE_NOT_FOUND` into exit 3 plus the install command. `check()` calls it first so "missing tooling wins over drift" (`sync.mjs:279`). Do the same for `yaml`.
**Why it matters (measured):** with a top-level `import { parseAllDocuments } from 'yaml'` in a copy of `sync.mjs` where `yaml` is not resolvable, `node sync.mjs check` printed `Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'yaml'` with a stack and exited 1. The unmodified script in the same bare directory printed `likec4 is not installed for docs/architecture. Run: pnpm --dir docs/architecture install` and exited 3. Selftest cases 09 and 11 build scratch repos with no `node_modules` and `probe_no_tooling` (`pre-commit-likec4-selftest.py:174-182`) requires exit 3 plus the install command, otherwise it raises `EnvironmentProblem` and the whole selftest returns 2. A static import would break the selftest and make the hook report a missing install as drift.
**Example:**
```js
// Source: pattern of sync.mjs:28-36, extended
async function loadTooling() {
  try {
    const { LikeC4 } = await import('likec4')
    const yaml = await import('yaml')
    return { LikeC4, yaml }
  } catch (err) {
    if (err?.code !== 'ERR_MODULE_NOT_FOUND') throw err
    console.error('likec4 or yaml is not installed for docs/architecture. Run: pnpm --dir docs/architecture install')
    process.exit(3)
  }
}
```
`generate` mode also needs `yaml` only if `render()` starts reading YAML (it should not in Phase 1), so `generate` can keep running without it.

### Pattern 2: Read every manifest through one document iterator
Walk `k8s/` once; for each file call `parseAllDocuments`; throw on `doc.errors.length` (measured: a malformed file does not throw, it fills `doc.errors`, e.g. `BAD_INDENT`; a repeated key gives `DUPLICATE_KEY`); skip null/non-object documents (a comment-only document `toJS()` is `null`). Both `workloads()`, `manifestNamespaces()` and `placements()` take this iterator's output, so the tree is parsed once per check.

### Pattern 3: Defined namespaces (D-01, D-02) - what the reader must collect
Collect a string from all of: `kind: Namespace` -> `metadata.name`; any key named `namespace` anywhere in the document whose value is a **string**; any key named `targetNamespace` whose value is a string. Measured on the real repo: 98 hits for those two keys, 18 of them non-string (the CRD OpenAPI schema nodes `...properties.namespace` in `k8s/flux-system/gotk-components.yaml`), so the string filter is mandatory. Distinct string values: exactly seven.

| Namespace | Where it comes from | Reached by |
|-----------|---------------------|------------|
| kanban-prod | `k8s/platform/namespaces.yaml` (`kind: Namespace`), `k8s/overlays/prod/kustomization.yaml:3` | Namespace kind, top-level kustomization `namespace` |
| kanban-nonprod | `k8s/platform/namespaces.yaml`, `k8s/overlays/nonprod/kustomization.yaml:3`, `k8s/flux-system/apps-nonprod.yaml:23,27` (healthChecks) | same + nested |
| kanban-data | `k8s/platform/namespaces.yaml`, `k8s/data/postgres/kustomization.yaml:3`, `k8s/data/postgres-bridge/postgres-bridge.yaml:14,26` | same + `metadata.namespace` |
| monitoring | `k8s/platform/namespaces.yaml`, `k8s/monitoring/**` (12 `metadata.namespace`), `k8s/platform/edge/certificate-monitoring.yaml:7` | Namespace kind, `metadata.namespace` |
| flux-system | `kind: Namespace` in `k8s/flux-system/gotk-components.yaml`, 44 `metadata.namespace`, `sourceRef.namespace` in HelmReleases, `subjects[].namespace` | all |
| kube-system | `k8s/platform/traefik/helmchartconfig.yaml:20` (`namespace: kube-system`) | `metadata.namespace` |
| cert-manager | `k8s/platform/cert-manager/cert-manager.yaml:38` (`targetNamespace: cert-manager`) and `:37` (`createNamespace: true`); **never** under a `namespace:` key | `spec.targetNamespace` only |

Count of `metadata.namespace` values measured: `kanban-data` 2, `flux-system` 44, `monitoring` 12, `kube-system` 1, none 61. Count of `spec.targetNamespace`: `cert-manager` 1. Top-level `namespace` in kustomize `Kustomization` files: `k8s/data/postgres/kustomization.yaml=kanban-data`, `k8s/monitoring/configs/kustomization.yaml=monitoring`, `k8s/overlays/nonprod/kustomization.yaml=kanban-nonprod`, `k8s/overlays/prod/kustomization.yaml=kanban-prod`. `kind: Namespace` appears five times: four in `k8s/platform/namespaces.yaml` (`kanban-prod`, `kanban-nonprod`, `kanban-data`, `monitoring`) and one in `gotk-components.yaml` (`flux-system`).

The comment text `-n kube-system` in `helmchartconfig.yaml:15` and the Helm `valuesContent: |-` string are why a regex over lines is wrong and a parse is right: the block scalar is one string value to the parser, so nothing inside it is read as a `namespace` key.

### Pattern 4: Placement (D-03) - a new rule, not an extension
`workloads()` compares only `kind/name` strings (`sync.mjs:312-314`); nothing compares a namespace. GATE-04's "placement check covers k8s/platform, not only base and data" therefore describes a check that must be created. Design (prototyped and run):

1. For every `kustomize.config.k8s.io` `Kustomization` that has a top-level `namespace`, walk its `resources` (a path is a file, or a directory whose own `kustomization.yaml` is followed), and assign that namespace to every file reached. A nested kustomization without its own `namespace` inherits the caller's. This is how `k8s/base/app/app.yaml` (no namespace anywhere in the file) gets `kanban-prod` and `kanban-nonprod`: both overlays list `../../base/app` and `../../base/redpanda` (`k8s/overlays/prod/kustomization.yaml:4-8`).
2. A document with no inherited namespace uses its own `metadata.namespace` (Traefik's HelmChartConfig -> `kube-system`; cluster-scoped kinds such as `ClusterIssuer` have none and are skipped).
3. For each model element that has at least one deployment instance: its anchors are the manifest documents matching its `metadata.workload` (`kind/name`), else, if `metadata.source` starts with `k8s/`, every document under that file or directory. Expected namespace set = union of the anchors' namespaces. Compare, as a sorted set, with the set of `parent.title` of the element's instances. Unequal -> one problem line naming the element, the model set and the manifest set.
4. Skip an element with no anchor documents. This is deliberate: when D4 deletes `k8s/data/postgres/postgres.yaml`, the existing lines `model claims workload StatefulSet/postgres but k8s/ has no such manifest` and `postgres: metadata source ... does not exist` already say so; a third placement line would change D4's recorded output, which D-05 says must pass unchanged.

Measured placements on the real repo: `traefik` -> `[kube-system]`; `kanban` and `redpanda` -> `[kanban-nonprod,kanban-prod]` (via the overlays); `postgres` and `backup` -> `[kanban-data]` (via `k8s/data/postgres/kustomization.yaml`). All five match the fixed model.

Kustomize semantics note: a kustomization's `namespace:` overrides a resource's own `metadata.namespace`, so check inherited first, own second (this ordering is used in the code example). Phase 2 will meet `HelmRelease` objects, where the object lives in `metadata.namespace` (flux-system) but its chart objects land in `spec.targetNamespace` (cert-manager, per the comment at `cert-manager.yaml:8-9`). The prototype excludes a HelmRelease's own `metadata.namespace` when `targetNamespace` is set; deciding which of the two is "placement" for a HelmRelease-backed element is Phase 2 (GATE-01) work and should be logged as an Open Question there, not decided now.

### Pattern 5: View membership (D-08)
Computed deployment views list every drawn node in `view.nodes()`; an instance node has `kind === 'instance'` and the same id as the deployment-model instance. The rule: every `computed.deployment.instances()` id must be in the union of those ids across all views where `view.isDeploymentView()` is true. Do not require namespace nodes to be drawn (see M2: an empty namespace node is also undrawn, and D-07 creates three on purpose).

### Pattern 6: Metadata allowlist (D-09)
`const METADATA_KEYS = ['source', 'workload']` next to `VIEWPOINTS` (`sync.mjs:40`). Iterate **four** surfaces: `computed.elements()`, `computed.deployment.nodes()`, `computed.deployment.instances()`, `computed.relationships()`. Each exposes `getMetadata()` returning a plain object keyed by metadata key (measured). The existing source-exists loop (`sync.mjs:316-319`) only walks elements and silently skips an element whose key is misspelled, so the allowlist also closes that gap.

### Pattern 7: Split `render()` (D-06)
Four families today: tables (`sync.mjs:246-253`), foreign keys (`255-257`), packages (`258-268`), package edges (`269-271`). Extract `renderTables()`, `renderForeignKeys()`, `renderPackages()`, `renderPackageEdges()` each returning an array of lines in the current order; `render()` concatenates the header, `extend postgres {`, tables, `}`, FK lines, `extend kanban {`, packages, `}`, edges, `}`. The split is correct only if `check` still exits 0 (it compares `render()` to `generated.c4` byte for byte, `sync.mjs:283-285`) and `sync.mjs generate --model <scratch copy>` leaves `generated.c4` identical. Do not change ordering or whitespace.

### Anti-Patterns to Avoid
- **Regex over k8s YAML for namespaces:** matches comments (`-n kube-system`) and misses `targetNamespace`; cannot see the inheritance through `kustomization.yaml`.
- **Treating `readdirSync` ENOENT as the missing-directory guard:** it exits 1, which the hook prints as "LikeC4 model drift ... Update docs/architecture/model" (`pre-commit:221-225`) - wrong advice for a broken fact source - and it only fires for directories the reader names explicitly (M9).
- **Requiring every deployment node in a view:** breaks D-07's three empty namespace nodes.
- **A hand-kept namespace allowlist:** rejected by D-02.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Multi-document YAML | `split(/^---/m)` plus regex (current `workloads()`) | `yaml.parseAllDocuments` | The regex cannot see keys after `labels:` (ARCHITECTURE G4 records that Flux's export defeats it), comments, or block scalars. |
| Which instances are drawn | Re-implement include/exclude resolution | `computed.views()` nodes | LikeC4 already decides (and its implicit-node rule is the source of the silent vanish, PITFALLS Pitfall 2). |
| Metadata key vocabulary in the DSL | A `specification { metadata }` block | A constant in `sync.mjs` | The DSL rejects it (`docs/DIAGRAM_CONVENTIONS.md:199`, row V2). |
| Kustomize rendering | A bundled kustomize/kubectl | Parse `kustomization.yaml` `namespace` and `resources` | Only those two fields matter; ARCHITECTURE N3. |

**Key insight:** every rule here is "compare a set read from the repo with a set read from LikeC4's computed model". The only genuinely new logic is the kustomization namespace walk (about 30 lines).

## Measured facts (the seven questions)

All commands were run in this session. Scratch lived in `docs/architecture/out/r1/` (gitignored via `docs/architecture/.gitignore:2`) and has been deleted; `git status` for `docs`, `scripts`, `.githooks`, `k8s` is clean.

### M1. Deployment model API in 1.60.0 [VERIFIED: ran on the current model]
`LikeC4.fromWorkspace(dir, { printErrors: false, throwIfInvalid: false, logger: false })` then `await likec4.computedModel()`; the deployment model is `computed.deployment` with methods `node`, `findNode`, `instance`, `findInstance`, `roots`, `elements`, `nodes`, `nodesOfKind`, `instances`, `instancesOf`, `deploymentRef`, `relationships`, `views`, `children`, ... (printed from the prototype chain). The documented API is the same: `model.deployment.nodes()`, `model.deployment.instancesOf(...)`, `element.getMetadata()` [CITED: github.com/likec4/likec4 apps/docs/src/content/docs/tooling/model-api.mdx via Context7]. A namespace node's name is its **title**; its id is the DSL identifier.

Runnable (placed anywhere under `docs/architecture/` so `likec4` resolves):
```js
import { LikeC4 } from 'likec4'
const likec4 = await LikeC4.fromWorkspace(process.argv[2], { printErrors: false, throwIfInvalid: false, logger: false })
const dep = (await likec4.computedModel()).deployment
for (const n of dep.nodes()) console.log('NODE', n.id, n.kind, n.title, n.parent?.id ?? null)
for (const i of dep.instances()) console.log('INSTANCE', i.id, i.element.id, i.parent.id, JSON.stringify(i.getMetadata()), JSON.stringify(i.element.getMetadata()))
```
Real output on the current model:
```
NODE netcup server  Netcup VPS (x86_64) null
NODE netcup.k3s cluster  k3s (single node) netcup
NODE netcup.k3s.edge namespace  edge netcup.k3s
NODE netcup.k3s.prod namespace  kanban-prod netcup.k3s
NODE netcup.k3s.nonprod namespace  kanban-nonprod netcup.k3s
NODE netcup.k3s.data namespace  kanban-data netcup.k3s
INSTANCE netcup.k3s.edge.traefik of traefik, parent netcup.k3s.edge, instance meta {}, element meta {"source":"k8s/platform/traefik"}
INSTANCE netcup.k3s.prod.kanban        of kanban   ... element meta {"workload":"Deployment/app"}
INSTANCE netcup.k3s.prod.redpanda      of redpanda ... {"source":"k8s/base/redpanda/redpanda.yaml","workload":"StatefulSet/redpanda"}
INSTANCE netcup.k3s.nonprod.kanban     (same element metadata as prod)
INSTANCE netcup.k3s.nonprod.redpanda   (same)
INSTANCE netcup.k3s.data.postgres      {"source":"k8s/data/postgres/postgres.yaml","workload":"StatefulSet/postgres"}
INSTANCE netcup.k3s.data.backup        {"source":"k8s/data/postgres/backup-cronjob.yaml","workload":"CronJob/postgres-backup"}
```
Instances carry the element's metadata only through `i.element.getMetadata()`; `i.getMetadata()` is the instance's own (empty here).

`likec4 export json --skip-layout -o FILE DIR` (the form named in success criterion 1) writes top-level keys `_stage, projectId, project, specification, elements, relations, globals, views, deployments, imports, manualLayouts`. `deployments.elements` is an object keyed by id holding ALL deployment nodes and instances (16 on the fixed model: 1 server, 1 cluster, 7 namespaces, 7 instances), each with `id, kind, title, style`; `deployments.relations` was empty. `views.production.nodes` lists only drawn nodes with `id, kind, title`. Consequence for criterion 1: "`production` view has a `kube-system` node and no `edge` node" is checkable in `views.production.nodes`; "every namespace is a node" must be checked in `deployments.elements`, not in a view, because the empty namespace nodes are not drawn (M2).

### M2. Instance view membership, with and without an unconnected instance [VERIFIED: ran]
Detection snippet (instance node ids equal deployment instance ids):
```js
const inView = new Set()
for (const v of computed.views()) if (v.isDeploymentView?.()) for (const n of v.nodes()) if (n.kind === 'instance') inView.add(n.id)
for (const i of computed.deployment.instances()) if (!inView.has(i.id)) problems.push(`instance ${i.id} appears in no deployment view`)
```
(`n.isInstance()` is NOT a method; `n.kind === 'instance'` is the test. My first probe used `isInstance` and wrongly reported all seven missing.)

- Current model: view `production`, 13 nodes (`netcup, netcup.k3s, netcup.k3s.edge, netcup.k3s.edge.traefik, ... netcup.k3s.data.backup, netcup.k3s.data.postgres`); 7 instances, 7 in a view, missing `[]`.
- Scratch with a new `lonely = system 'Lonely'` (no relationship) and `instanceOf lonely` in `kanban-data`: `errors: []` (it validates), the `production` node list is **unchanged** (same 13 nodes, `netcup.k3s.data.lonely` absent), `ALL instances: 8 | in some view: 7`, `MISSING from every deployment view: [ 'netcup.k3s.data.lonely' ]`. Current `sync.mjs check` on that scratch model: `OK (34 elements ...)`, **exit 0** - not caught before the rule exists.
- Scratch with an empty `certmgr = namespace 'cert-manager' { }`: `errors: []`, `production` node list unchanged, instances 7/7 in view, and `ALL nodes not drawn in any view: [ 'netcup.k3s.certmgr' ]`. Current `check`: exit 0. This answers Open Question 6: an empty namespace node is validated, exported, and never drawn.
- Post-fix model (below) with three empty namespace nodes: `nodes not drawn: [ 'netcup.k3s.flux', 'netcup.k3s.certmgr', 'netcup.k3s.mon' ]`, instances 7/7 drawn.

### M3. Metadata surfaces and the silent typo [VERIFIED: ran]
Scratch: `sourc 'k8s/platform/traefik'` on `traefik` in `model.c4`; `metadata { nodekey 'x' }` on the `edge` namespace node; `metadata { inst-level 'y' }` inside `instanceOf traefik { ... }`; `metadata { relkey 'z' }` on the `client -[sync]-> traefik` relationship. Output of the key census:
```
errors: []
KEY source x28 ...      KEY workload x4 ...
KEY sourc x1 element:traefik
KEY nodekey x1 depnode:netcup.k3s.edge
KEY inst-level x1 instance:netcup.k3s.edge.traefik
KEY relkey x1 relationship:184uwhf
relationship has getMetadata: function
```
So (a) a misspelled key validates with `errors: []`, (b) metadata lives on elements, deployment nodes, instances AND relationships in 1.60 (ROADMAP.md says relationship metadata is unverified; it is supported), (c) the real model uses exactly `source` (29 elements before the typo) and `workload` (4), matching D-09. Current `sync.mjs check` on the typo model: `OK`, **exit 0**; because `sourc` is not read by the source-exists loop, the typo also silently disables that file-exists check for the element. Values may be arrays (docs example `tags ['backend', ...]`); `Object.keys(getMetadata())` is unaffected.

### M4. `yaml` package [VERIFIED: ran under pnpm 11.20.0 with `"type": "module"`]
- Version 2.9.1. ESM import works: `import { parse, parseAllDocuments, parseDocument } from 'yaml'` ran in a `"type": "module"` package installed by pnpm 11.20.0.
- `parse(multiDocString)` **throws** `MULTIPLE_DOCS` ("Source contains multiple documents; please use YAML.parseAllDocuments()"). Use `parseAllDocuments(str)`; `k8s/platform/namespaces.yaml` returned 4 documents (the leading comment block before the first `---` does not create a null document).
- Errors are not thrown: malformed input `a: [1, 2\nb: 3\n` gives `docs[0].errors.length === 1` with code `BAD_INDENT`; `a: 1\na: 2\n` gives `DUPLICATE_KEY`. A reader that skips `doc.errors` silently half-reads a broken manifest. Fail the check on any document error.
- All 100+ real documents under `k8s/` parsed with zero errors (`parse errors: []`), including the large Flux `gotk-components.yaml` and the Helm-values HelmReleases. Helm `values:` are nested objects (HelmRelease) or a `|-` block scalar string (`helmchartconfig.yaml` `valuesContent`); the latter is an opaque string to the parser, which is the safe behaviour for namespace reading.
- `toJS()` of a comment-only document is `null`; guard with `o && typeof o === 'object'`.

### M5. Namespaces and workloads under `k8s/**` [VERIFIED: ran, see Pattern 3 for the namespace table]
Workloads (Deployment, StatefulSet, CronJob) by directory, from parsed documents:
- `k8s/base`: `Deployment/app`, `StatefulSet/redpanda`.
- `k8s/data`: `CronJob/postgres-backup`, `StatefulSet/postgres`.
- `k8s/platform`: none (kinds present: Certificate 1, ClusterIssuer 2, HelmChartConfig 1, HelmRelease 1, HelmRepository 1, Kustomization 3, Namespace 4).
- `k8s/monitoring`: none (ConfigMap 1, HelmRelease 4, HelmRepository 2, IngressRoute 2, Kustomization 2, Middleware 2, PodMonitor 2).
- `k8s/overlays`: none (patch documents have no `kind`: 4 documents with `kind` undefined).
- `k8s/flux-system`: 12 Deployment documents (6 controllers, each twice in `gotk-components.yaml`) - must stay OUT of `workloads()` until Phase 2 (G4) models the Flux controllers; widening to `k8s/**` would turn the check red.

**Does the baseline tally change if `workloads()` is widened to `k8s/platform`?** No. Regex reader on `k8s/base`+`k8s/data`: `CronJob/postgres-backup, Deployment/app, StatefulSet/postgres, StatefulSet/redpanda` (4). `yaml` reader on the same dirs: identical (4, set equality printed `true`). `yaml` on `base+data+platform`: the same four. On `base+data+platform+monitoring`: the same four. Walk exactly `['k8s/base', 'k8s/data', 'k8s/platform']`.

### M6. Baseline and the `edge` defect [VERIFIED: ran]
`node docs/architecture/sync.mjs check` -> `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads)` plus `package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service`, exit 0.

`docs/architecture/model/deployment.c4:9` is `      edge = namespace 'edge' {` containing `instanceOf traefik`. Prototype namespace/placement run on the **unmodified** model against the real repo:
```
namespaces defined: cert-manager, flux-system, kanban-data, kanban-nonprod, kanban-prod, kube-system, monitoring
model namespace nodes: edge, kanban-data, kanban-nonprod, kanban-prod
DRIFT
- namespace flux-system is defined in k8s/flux-system/apps-nonprod.yaml but has no namespace node in the deployment model
- namespace monitoring is defined in k8s/monitoring/configs/datasources.yaml but has no namespace node in the deployment model
- namespace cert-manager is defined in k8s/platform/cert-manager/cert-manager.yaml but has no namespace node in the deployment model
- namespace kube-system is defined in k8s/platform/traefik/helmchartconfig.yaml but has no namespace node in the deployment model
- deployment node netcup.k3s.edge names namespace 'edge', which no manifest under k8s/ defines
- placement: traefik is deployed in [edge] in the model, manifests place it in [kube-system]
exit=1
```
So `edge` is the only **wrong** namespace (it is the name of a Flux Kustomization, `k8s/flux-system/edge.yaml:5`, path `./k8s/platform/edge`). It is not the only red line: success criterion 1 says the check "exits 1 and names the `edge` node", and it does; but four other lines (the four missing nodes `flux-system`, `monitoring`, `cert-manager`, `kube-system`) are also red until the model gains them. That is correct and intended, not a second defect. Note the rejected-name finding also appears twice (namespace rule and placement rule); fine.

Post-fix model (the target `deployment.c4` namespace block; `kube-system` renamed from `edge`, three empty nodes added):
```
      kube-system = namespace 'kube-system' {
        instanceOf traefik
      }
      flux-system = namespace 'flux-system' {
      }
      cert-manager = namespace 'cert-manager' {
      }
      monitoring = namespace 'monitoring' {
      }
```
Hyphenated node ids work (`docs/DIAGRAM_CONVENTIONS.md:200` row V3: identifier rule `[a-zA-Z_][a-zA-Z0-9_-]*`), measured: `errors: []`, and `production` lists `netcup.k3s.kube-system` and `netcup.k3s.kube-system.traefik`. Use id = namespace name so the id and the title both satisfy criterion 1's literal wording. `views.c4:37` (`include netcup, netcup.k3s, netcup.**`) needs no edit. With this model `sync.mjs check` (existing rules) exits 0 with `33 elements ... 4 workloads` (elements() does not count deployment nodes, so the element count stays 33), and the prototype's four rules exit 0.

### M7. Drill harness baseline [VERIFIED: ran]
`fish docs/architecture/drill.fish` exits 0 and its stdout is **byte-identical** to the committed `docs/architecture/drill-output.txt` (14 `==` sections: baseline, D1, D2, D3, D4, D5, D6, D6b, D7, D7b, D7c, D8, D8b, D9). Existing recorded D3 and D4 results:
```
== D3 ...  exit=1  - k8s Deployment/worker has no element carrying metadata workload 'Deployment/worker'   (5 workloads)
== D4 ...  exit=1  - model claims workload StatefulSet/postgres but k8s/ has no such manifest
                    - postgres: metadata source 'k8s/data/postgres/postgres.yaml' does not exist           (3 workloads)
```
`fresh()` (`drill.fish:12-20`) copies `src/main/resources/db/migration`, `application.properties`, the Java tree, and `k8s/base k8s/data k8s/platform` only, plus `model/`. It does NOT copy `k8s/overlays`, `k8s/monitoring`, `k8s/flux-system`. `run()` (`22-27`) calls `node $sync check --root $scratch --model $scratch/model` and prints `exit=` and each output line. The harness has no mechanism to run a drill against an older `sync.mjs`; `sync` is fixed at `$here/sync.mjs` (`drill.fish:9`).

### M8. Hook and selftest [VERIFIED: read and ran]
- Trigger list: `.githooks/pre-commit:205-207`:
  ```
  LIKEC4_TRIGGERS=$(git diff --cached --name-only --no-renames -- \
    src/main/resources/db/migration k8s/base k8s/data docs/architecture \
    ':(glob)src/main/java/**/constant/KafkaTopics.java') || {
  ```
  The comment above it (`pre-commit:198-199`) records `k8s/platform` as a known hole: "Metadata sources outside the trigger paths (k8s/platform and the cited Java packages) are caught in CI only." That sentence becomes false and must be rewritten with the change; also line 204 "Falsified if scripts/pre-commit-likec4-selftest.py passes with --hook pointed at a copy without this step."
- Selftest: `rg -c '^def test_'` -> **15** (`test_01` ... `test_15`); `python3 scripts/pre-commit-likec4-selftest.py` printed `15/15 passed` in about 15 s. The docstring (`:4-5`) says "nine refusals for drift, three matching commits that pass the check, two refusals for missing tooling and one commit of an unrelated path". Test functions are discovered by name (`main()`, `sorted(globals().items())`, `:460`), so adding `test_16_...` is all that is needed; the count printed becomes `16/16`.
- Pattern for a case that proves a trigger path: `make_repo(name, True)` copies every git-tracked file under `src/main`, `k8s`, `docs/architecture` into a scratch repo (`tracked_files()`, `:148-164`), symlinks the real `node_modules` in, stubs the later gates; the case edits a file, `git add`s it, calls `commit(...)`, and asserts with `refused_for_drift(code, out, repo, before, finding)` that the banner `Checking the LikeC4 model against the code it describes...` printed, the commit failed with `LikeC4 model drift`, the finding string is in the list, and HEAD did not move (`:240-246`). Existing model for a path-only trigger: `test_03` (new manifest under `k8s/base`, `-a` commit, `:304-310`).
  Proposed `test_16`: `replace_in(repo, "k8s/platform/traefik/helmchartconfig.yaml", "namespace: kube-system", "namespace: kube-sytem")`, `git add` it, commit, `refused_for_drift(..., "namespace kube-sytem is defined in")`. Red without the trigger: with the old trigger list this commit succeeds with no banner, so the `BANNER in plain` assertion fails; green once the path is added. Check the other direction with the existing `--hook PATH` option against a copy whose trigger list lacks the path (the mechanism `:204` names).
- Only one new case is needed to meet "more than 15" and prove the new trigger. The rules themselves are proven by drills, not by selftest cases; do not add cases per rule.
- Caveats: `copy_tracked()` copies **tracked** files only; the case runs with `yaml` resolvable because the selftest symlinks the real `docs/architecture/node_modules` (`:221`), so `pnpm --dir docs/architecture install` must have run (preflight checks `node_modules/likec4` only, `:443`; add a `yaml` check or accept that a missing yaml surfaces as exit 3 inside the case).
- CI: `likec4-model-drift` runs `pnpm install --frozen-lockfile`, then the selftest, then `node docs/architecture/sync.mjs check` (`invariant-checks.yml:159-167`). The workflow `paths` include `**` and `docs/architecture/**` (`:22-26`), so k8s edits already trigger CI; only the local hook is path-limited.

### M9. Missing-directory behaviour [VERIFIED: ran]
Today: deleting `k8s/data` from a scratch root makes `workloads()` throw `Error: ENOENT: no such file or directory, scandir '.../k8s/data'` with a stack trace from `sync.mjs:220` and the process exits **1** (same code as drift; the hook would print "LikeC4 model drift ... Update docs/architecture/model").

With a reader that walks `k8s/` as a whole, a missing sub-tree does NOT fail: in a scratch root lacking `k8s/overlays`, the prototype reported two `placement: kanban/redpanda ... manifests place it in []` lines (loud but misleading); in a root also lacking `k8s/monitoring` and `k8s/flux-system` the output was identical - **no new finding**, because `k8s/platform` alone defines all seven namespaces. So `fresh()` forgetting those two directories would silently shrink what the namespace reader reads without any drill going red. Therefore D-11 needs an explicit guard: a constant `K8S_DIRS = ['k8s/base','k8s/data','k8s/platform','k8s/overlays','k8s/monitoring','k8s/flux-system']`, checked with `existsSync` before reading, producing a message naming the missing directory and a distinct exit (recommended: add exit 4 "a fact source is missing", documented in the header comment at `sync.mjs:6`; the hook already treats any status other than 0/1 as "did not run to completion", `pre-commit:226-231`). Exit 1 is also acceptable if the planner prefers fewer codes, but the message must say a directory is missing, not "drift".

### M10. `render()` and tally [VERIFIED: read]
`generated.c4` is rewritten by `render()` (`sync.mjs:238-274`); `check` compares it byte for byte (`:283-285`). The tally string is built at `sync.mjs:322`; the selftest's `OK_TALLY` regex is `^OK \(\d+ elements(, \d+ [a-z ]+)+\)$` (`pre-commit-likec4-selftest.py:41`), so appending `, 7 namespaces` stays valid. But changing the tally line rewrites every line of `drill-output.txt`, destroying the evidence that the `workloads()` port changed nothing. Recommendation: do not touch the tally in the port or the render split; if a namespace count is wanted, add it in a later, separate commit and re-record.

## Common Pitfalls

### Pitfall 1: `cert-manager` is invisible to a literal `namespace:` scan
**What goes wrong:** The seventh namespace is only `spec.targetNamespace: cert-manager` (`cert-manager.yaml:38`); the file's own `metadata.namespace` is `flux-system`.
**Why it happens:** D-01 says "any `namespace:` field" but also lists `cert-manager` as covered, so the intent is clear and the wording is short by one key.
**How to avoid:** collect `targetNamespace` too. Add it to the Namespace reader and to the drill that removes the `cert-manager` node.
**Warning signs:** the namespace check names `cert-manager` as "defined nowhere" or the model node `cert-manager` as having no manifest.

### Pitfall 2: Static `import 'yaml'` converts "tooling missing" into "drift"
**What goes wrong:** exit 1 with a Node stack where exit 3 and the install command are expected; selftest cases 09/11 raise `EnvironmentProblem` and the whole selftest exits 2 (measured above).
**How to avoid:** Pattern 1.

### Pitfall 3: Committing the red state
**What goes wrong:** D-13 wants the namespace check shown red on the real defect. Committing the rule before the fix is refused by the hook (`docs/architecture` is a trigger path) and, if `--no-verify` is used, fails CI's `likec4-model-drift`.
**How to avoid:** capture the red as evidence from the working tree (rule present, model unfixed: `node docs/architecture/sync.mjs check; echo exit=$?`), save it to a file in the phase directory (precedent: `261027-if9-T1-red.txt` .. `T4-red.txt` referenced at `docs/DIAGRAM_CONVENTIONS.md:172-173`), then commit rule plus model fix together. The recorded green is the same command after the model edit.

### Pitfall 4: Partial `fresh()`
**What goes wrong:** M9. **How to avoid:** copy the whole `k8s/` tree (`cp -r $repo/k8s $scratch/`), and keep the `K8S_DIRS` guard in the script so a future reader dir cannot be forgotten.

### Pitfall 5: Requiring every deployment node to be drawn
**What goes wrong:** the three D-07 namespace nodes (and any node with no connected instance) are undrawn by design (M2). **How to avoid:** the membership rule is over **instances** only.

### Pitfall 6: Non-string `namespace` keys
**What goes wrong:** the Flux CRD schemas contain 18 `properties.namespace` object nodes; treating them as namespace names throws or yields `[object Object]`. **How to avoid:** `typeof v === 'string'`.

### Pitfall 7: Placement noise from cluster-scoped and HelmRelease kinds
**What goes wrong:** `ClusterIssuer` has no namespace; a `HelmRelease` is in `flux-system` while its objects land in `targetNamespace`. **How to avoid:** placement only evaluates elements that have an anchor document; skip documents with no resolvable namespace; defer HelmRelease semantics to Phase 2.

### Pitfall 8: Stale documentation of the checks
**What goes wrong:** the memory note "Infra diagrams go stale silently" is exactly this. After Phase 1, `docs/DIAGRAM_CONVENTIONS.md` still says: "the workload set matches `k8s/base` and `k8s/data`" (`:161`); "12 of 12 drills" and the list D1..D9 (`:171-173`); "runs 15 cases, 9 of them drift refusals, and passes 15/15" (`:175`); the hook trigger list (`:177-178`); "A changed metadata source outside those paths is caught in CI only" (`:180`); and in the Physical row/table "workloads checked against `k8s/base` and `k8s/data`; nodes authored" (`:105`, `:230`). The Physical row text is owned by STD-01 in Phase 2; the Checks section (`:156-185`) is the check inventory and should be updated in the same Phase 1 change. Put the nodes-are-now-checked fact in the Checks section and leave the row rewrite to STD-01, noting the gap.
**Also stale in code comments:** `pre-commit:198-199` (see M8) and the header of `sync.mjs` (rules and exit codes).

### Pitfall 9: Hook trigger narrower than the readers
**What goes wrong:** D-12 says the hook triggers on `k8s/platform`. The namespace reader reads all of `k8s/**` and placement reads `k8s/overlays` too, so a typo in `k8s/monitoring` or `k8s/overlays` passes the local hook and fails only in CI. **How to avoid / recommendation:** use `k8s` as the pathspec (it includes `k8s/platform`, so D-12 holds literally) and prove `k8s/platform` with the new selftest case. This is a deviation in breadth, not direction; log it as an assumption (A2). Cost: the hook needs Node and the install for any commit touching k8s (about 1 s for the check, measured 0.9 s).

### Pitfall 10: Two-sided drills need a "green" half
Success criterion 4 requires that adding a relationship or removing the instance returns exit 0. A drill that only shows red does not prove the rule is precise. Each membership drill should also run the fixed variant (see Validation Architecture D13).

## Code Examples

Prototype of all four rules, run against the real repo with real results quoted above (condensed; the real implementation lives in `sync.mjs` and uses the lazily-loaded `yaml`).

```js
// Defined namespaces: Namespace names + any string `namespace`/`targetNamespace` key at any depth.
function manifestNamespaces(docs) {
  const found = new Map()
  const add = (ns, file) => found.set(ns, [...(found.get(ns) ?? []), file])
  const visit = (n, file) => {
    if (Array.isArray(n)) n.forEach((v) => visit(v, file))
    else if (n && typeof n === 'object') {
      for (const [k, v] of Object.entries(n)) {
        if ((k === 'namespace' || k === 'targetNamespace') && typeof v === 'string') add(v, file)
        visit(v, file)
      }
    }
  }
  for (const { file, o } of docs) {
    if (o.kind === 'Namespace') add(o.metadata.name, file)
    visit(o, file)
  }
  return found
}

// Placement: a kustomization's top-level namespace reaches every file it lists, through nested kustomizations.
function placements(docs, root) {
  const kusts = docs.filter((d) => d.o.kind === 'Kustomization' && String(d.o.apiVersion).startsWith('kustomize.config'))
  const inherited = new Map() // absolute file -> Set(namespace)
  const reach = (kustFile, ns, seen = new Set()) => {
    const k = kusts.find((d) => d.file === kustFile)
    if (!k || seen.has(kustFile)) return
    seen.add(kustFile)
    for (const r of k.o.resources ?? []) {
      const target = resolve(dirname(kustFile), r)
      const nested = join(target, 'kustomization.yaml')
      if (existsSync(nested)) reach(nested, ns ?? k.o.namespace, seen)
      else if (ns ?? k.o.namespace) inherited.set(target, (inherited.get(target) ?? new Set()).add(ns ?? k.o.namespace))
    }
  }
  for (const k of kusts) if (k.o.namespace) reach(k.file, k.o.namespace)
  return (file, o) => {
    const fromKust = inherited.get(file)
    if (fromKust) return [...fromKust]
    const own = o.kind === 'HelmRelease' && o.spec?.targetNamespace ? undefined : o.metadata?.namespace
    return own ? [own] : []
  }
}

// Model side: namespace nodes are matched by title; instances grouped per element.
const modelNs = new Map([...dep.nodes()].filter((n) => n.kind === 'namespace').map((n) => [n.title, n.id]))
```
The element-anchor comparison, view-membership snippet (M2) and the four-surface metadata loop are as described in Patterns 4-6; the prototype produced the quoted outputs with them.

A real-repo prototype run on the six scratch variants (scratch models derived from the current model; all exit codes as printed):
- fixed model: `OK`, exit 0.
- unmodified model: DRIFT naming `edge` and the four missing namespaces (M6).
- `instanceOf backup` moved from `kanban-data` to `kanban-prod`: `- placement: backup is deployed in [kanban-prod] in the model, manifests place it in [kanban-data]`, exit 1.
- `cert-manager` namespace node deleted from the fixed model: `- namespace cert-manager is defined in k8s/platform/cert-manager/cert-manager.yaml but has no namespace node in the deployment model`, exit 1.
- manifest `namespace: kube-system` typo'd to `kube-sytem` (scratch root): three lines: `namespace kube-sytem is defined in k8s/platform/traefik/helmchartconfig.yaml but has no namespace node ...`, `deployment node netcup.k3s.kube-system names namespace 'kube-system', which no manifest under k8s/ defines`, `placement: traefik is deployed in [kube-system] in the model, manifests place it in [kube-sytem]`; exit 1.
- unconnected instance: `- instance netcup.k3s.data.lonely appears in no deployment view`; typo key: `- element traefik carries metadata key 'sourc', not in the allowlist (source, workload)` plus lines for the node, instance and relationship keys; exit 1 each.
- Cost: the whole prototype (likec4 load, parse of all of `k8s/**`) ran in 1.08 s wall; current `check` takes 0.90 s.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Regex over `---`-split YAML for workloads | `yaml.parseAllDocuments` | This phase (D-04) | Also fixes the labels-before-name blind spot (ARCHITECTURE G4). |
| Metadata keys unconstrained | Constant allowlist in `sync.mjs` | This phase | DSL cannot declare keys (row V2). |
| Workload set only | Namespace sets + per-element placement | This phase | ARCHITECTURE G10 + G11 ("the largest honesty gain in Physical"). |

**Deprecated/outdated:** the ARCHITECTURE G10 idea of a "short allowlist of the three system namespaces" is superseded by D-02 (no allowlist; the manifests are the source).

## Runtime State Inventory

Not a rename/refactor of runtime state in the stored-data sense, but the `edge` -> `kube-system` rename touches the published model and docs:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None - verified: the model is source text; no datastore holds the string `edge` as a namespace | none |
| Live service config | None - verified by reading `k8s/`: no cluster object is named `edge` as a namespace; `edge` is a Flux Kustomization name (`k8s/flux-system/edge.yaml:5`) and a directory `k8s/platform/edge`, both unchanged and correct | none; do NOT rename them |
| OS-registered state | None - no scheduler or service names it | none |
| Secrets/env vars | None | none |
| Build artifacts | `docs/architecture/out/` (gitignored) holds stale renders after the rename | regenerate only if a render is needed; the PNG/site outputs are not committed |

The only references to the model node are the `netcup.**` glob (`views.c4:37`, unaffected) and, outside the model, text mentioning the nonexistent `edge` namespace: `rg -n 'edge' docs/DIAGRAM_CONVENTIONS.md docs/INFRA_ARCHITECTURE.md` shows only prose about the "edge" concept and `platform/edge` paths, none naming a namespace `edge`. [VERIFIED: rg this session]

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | sync.mjs, drills, hook | yes | v24.19.0 | — |
| pnpm | `yaml` install | yes | 11.20.0 | — |
| likec4 (installed) | check, export | yes | 1.60.0 (`docs/architecture/node_modules/likec4`) | — |
| fish | drill.fish, scratch tooling | yes | in use this session | — |
| python3 | selftest | yes | ran `15/15 passed` | — |
| trash-put | drill.fish `fresh()` and cleanup | yes | in use | — |
| Docker | `fastTest` in the pre-commit hook (Testcontainers) | not probed | — | needed only for the full 4-minute hook, not for the LikeC4 step or the selftest (the selftest stubs docker) |
| Network to npm | `pnpm add yaml` | yes | registry reachable | — |

**Note on node_modules state:** my probe `pnpm add` (against the workspace root by accident, then lockfile reverted) left `docs/architecture/node_modules/.pnpm` with an extra `yaml@2.9.1` store entry and an updated `.modules.yaml`. These files are gitignored; `sync.mjs check` and the selftest still pass. The next `pnpm --dir docs/architecture install` reconciles them. A fresh worktree needs its own install anyway (`~/.claude/rules/js-ts.md`).

## Validation Architecture

> `workflow.nyquist_validation` is `false` in `.planning/config.json`, but the invocation asked for this section explicitly, so it is included. The proof discipline here is the roadmap's red-then-green drill, not unit tests.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | fish drill harness (`docs/architecture/drill.fish`) + Python selftest (`scripts/pre-commit-likec4-selftest.py`) |
| Config file | none |
| Quick run command | `node docs/architecture/sync.mjs check` (0.9 s) |
| Full suite command | `fish docs/architecture/drill.fish > /tmp/out.txt; diff /tmp/out.txt docs/architecture/drill-output.txt` plus `python3 scripts/pre-commit-likec4-selftest.py` (about 15 s) |

### Making "not caught before the rule exists" reproducible
Add one line to `drill.fish` after `set sync $here/sync.mjs` (`:9`): `set -q SYNC; and set sync $SYNC`. To record the "before" half for a new drill, copy the pre-rule `sync.mjs` (`git show HEAD:docs/architecture/sync.mjs`) to a path **under `docs/architecture/`** (for example `docs/architecture/out/sync-before.mjs`, gitignored) so `likec4` still resolves, run `SYNC=... fish docs/architecture/drill.fish`, and save the output as the evidence file for that rule; then run the normal `fish docs/architecture/drill.fish` after the rule lands and append to `drill-output.txt`. Order inside a rule's task: add the drill, run it against the old script (expect `exit=0` for D10+ drills), add the rule, run it again (expect `exit=1` with the named finding). Remember the old script ignores the extra `k8s` dirs and passes `--root/--model` as before, so it runs unchanged.

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PHYS-01 | Real defect red, then fix green; `production` has `kube-system`, no `edge`; every manifest namespace is a model node | evidence + export check | `node docs/architecture/sync.mjs check; echo exit=$?` (red then green), `likec4 export json --skip-layout -o out/x.json docs/architecture/model` then read `views.production.nodes` and `deployments.elements` | evidence files new |
| GATE-04 (namespace) | Missing node and manifest typo both red | drill | D10, D10b below | drill new |
| GATE-04 (placement) | Instance moved to wrong namespace red | drill | D11 | drill new |
| GATE-02 | `sourc` red; key on node/instance/relationship red | drill | D12, D12b | drill new |
| GATE-03 | Unconnected instance red; with a relationship green | drill | D13, D13b | drill new |
| D-05 | `workloads()` port output unchanged | regression | `diff` of `drill-output.txt` is empty after the port | exists |
| D-11 | Missing required directory exits non-zero with a naming message | drill | D14 | drill new |
| D-12 | k8s/platform commit triggers the step | selftest | `test_16` (+ `--hook` mutant) | new case |

### Drill specification (scratch edits; all on the post-fix model; expected findings from the prototype)
| Drill | Scratch edit | Expected before rule | Expected after rule |
|-------|--------------|----------------------|---------------------|
| D10 | delete the `cert-manager` namespace block from `$scratch/model/deployment.c4` | exit 0 (not caught) | exit 1, `namespace cert-manager is defined in k8s/platform/cert-manager/cert-manager.yaml but has no namespace node in the deployment model` |
| D10b | `sed -i 's/namespace: kube-system/namespace: kube-sytem/' $scratch/k8s/platform/traefik/helmchartconfig.yaml` | exit 0 | exit 1, three lines (namespace undefined in model, model node undefined in manifests, placement) |
| D11 | move `instanceOf backup` from the `data` block to the `prod` block | exit 0 | exit 1, `placement: backup is deployed in [kanban-prod] in the model, manifests place it in [kanban-data]` |
| D12 | `sed -i "s/^      source 'k8s\/platform\/traefik'/      sourc 'k8s\/platform\/traefik'/" $scratch/model/model.c4` | exit 0 | exit 1, names `sourc` and `traefik` |
| D12b | add `metadata { nodekey 'x' }` to a namespace node (and, as a sibling run, to an instance and a relationship) | exit 0 | exit 1, names the key and the node/instance/relationship id |
| D13 | append `lonely = system 'Lonely'` to `model.c4` and `instanceOf lonely` to the `data` namespace | exit 0 | exit 1, `instance netcup.k3s.data.lonely appears in no deployment view` |
| D13b (green half) | D13 plus `traefik -[sync]-> lonely 'probe'`, or the lonely lines removed | n/a | exit 0 (confirms the rule is symptom-based and precise) |
| D14 | `trash-put $scratch/k8s/overlays` (and separately `$scratch/k8s/monitoring`) | exit 1 as ENOENT or placement noise, or exit 0 for `k8s/monitoring` | non-zero with `k8s/<dir>` named and not reported as clean |
| D3, D4 | unchanged | recorded output byte-identical | recorded output byte-identical |

Real-defect red (D-13 / criterion 1): not a drill; the evidence file is the output of `node docs/architecture/sync.mjs check; echo exit=$?` with the namespace rule in the working tree and the model unfixed (exit 1, `edge` named), followed by the same command after the model edit (exit 0), and the export check: `views.production.nodes` contains `netcup.k3s.kube-system` (title `kube-system`) and no `edge`.

### Sampling Rate
- **Per task commit:** `node docs/architecture/sync.mjs check` and, for drill changes, the affected drill section.
- **Per wave merge:** full `fish docs/architecture/drill.fish` plus `python3 scripts/pre-commit-likec4-selftest.py`.
- **Phase gate:** drill output committed to `drill-output.txt`; the first section and D1..D9 unchanged except where a deliberate change is explained; selftest `16/16`; CI `likec4-model-drift` green.

### Wave 0 Gaps
- [ ] `yaml@2.9.1` in `package.json` + lock (checkpoint: lockfile diff is the `yaml` lines only)
- [ ] `drill.fish`: `SYNC` override; `fresh()` copies all of `k8s/`
- [ ] No other framework to install

## Security Domain

`security_enforcement` is `true` in `.planning/config.json:56`; included in short form because the phase is a local, read-only developer tool.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes (repo YAML is the input) | `yaml.parseAllDocuments` with `doc.errors` checked; never `eval`; typeof guards on every field read |
| V6 Cryptography | no | — |
| V14 Config / supply chain | yes | exact-pinned `yaml@2.9.1`, committed lockfile, `--frozen-lockfile` in CI, `allowBuilds` policy unchanged (`pnpm-workspace.yaml`: `esbuild: false`) |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Malicious or typosquatted package | Tampering | The seam returned a no-observation `SUS`; verify the lockfile diff and integrity (Package Legitimacy Audit). `yaml` has no `postinstall` and no dependencies. |
| Secrets in evidence files | Information disclosure | Gitleaks refuses credential-shaped values anywhere in a staged diff, `.planning/` prose included (CLAUDE.md). The k8s manifests read here contain no secret values, but do not paste Helm `valuesContent` or raw `kubectl get -o yaml` output into evidence. |
| Path traversal via `resources:` in a kustomization | Tampering | The resolver follows only relative paths from files under `--root`; keep resolved paths constrained to `root` (assert `target.startsWith(root)`). |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Placement by per-element set equality, skipping elements with no k8s anchor, is the intended meaning of "placement check" (D-03, criterion 2). The codebase has no placement check today, so this is a design, prototyped and run, not a locked decision. | Pattern 4 | A different intended rule (for example "instance parent title is in the defined set") would pass a moved instance, contradicting criterion 2; a stricter rule would force Phase 2 elements to carry anchors. |
| A2 | Widening the hook trigger to `k8s` (rather than only `k8s/platform`) is acceptable; it satisfies D-12 literally. | Pitfall 9 | User wanted a narrower trigger to keep non-platform k8s commits free of Node/install; costs about 1 s and a Node prerequisite per k8s commit. |
| A3 | Exit 4 (or exit 1 with a "missing directory" message) is acceptable for a missing fact-source directory. | M9 | Any non-zero satisfies D-11; a new code only changes hook wording. |
| A4 | Node id equal to the namespace name (`kube-system`) is preferred over `kubesystem`/`edge`-style ids. | M6 | Cosmetic; criterion 1's wording works either way if checked by title. |
| A5 | The tally line should stay unchanged in this phase. | M10 | Adding a namespace count later requires re-recording `drill-output.txt`. |
| A6 | The "weekly downloads" figure for `yaml` is from `api.npmjs.org` for a window ending 2026-07-01, not the current week. | Package Legitimacy Audit | None for the decision; recency of the number only. |
| A7 | Whether the user wants the red-state evidence in a separate (`--no-verify`) commit rather than only as an evidence file. | Pitfall 3 | A separate red commit fails CI; the evidence-file route avoids it. |

## Open Questions

1. **Hook trigger breadth (A2).**
   - What we know: D-12 names `k8s/platform`; the readers read all of `k8s/**`; CI already covers every path.
   - What's unclear: whether the user wants non-platform k8s commits to stay free of the Node prerequisite.
   - Recommendation: use `k8s`; if the user prefers the literal path, record the remaining hole in the hook comment and the Checks section (caught in CI only for `k8s/monitoring`, `k8s/flux-system`, `k8s/overlays`).

2. **HelmRelease placement semantics (Phase 2).**
   - What we know: a HelmRelease lives in `flux-system` and installs into `targetNamespace`; Phase 1 elements have no HelmRelease anchors.
   - What's unclear: which namespace the model's element for cert-manager/Prometheus should be placed under for GATE-01.
   - Recommendation: do not decide in Phase 1; leave the HelmRelease branch in `placements()` returning no own namespace and note it for Phase 2 planning.

3. **Does the Physical row need a Phase 1 touch?** Lines `:105` and `:230` of `docs/DIAGRAM_CONVENTIONS.md` say "nodes authored", false once namespaces are checked. STD-01 (Phase 2) owns the row. Recommendation: update the Checks section now and leave the rows, with a one-line pointer to STD-01.

## Sources

### Primary (HIGH confidence)
- Commands run this session against `docs/architecture/` (likec4 1.60.0), `k8s/**`, the hook and the selftest; outputs quoted above.
- `docs/architecture/sync.mjs` (lines 14-344), `docs/architecture/drill.fish`, `docs/architecture/drill-output.txt`, `docs/architecture/model/*.c4`, `.githooks/pre-commit:181-232`, `scripts/pre-commit-likec4-selftest.py` (all 501 lines), `.github/workflows/invariant-checks.yml:12-47,137-167`, `docs/DIAGRAM_CONVENTIONS.md:100-233`, `k8s/**` files named inline.
- npm registry: `npm view yaml version time.created time.modified dist-tags repository.url engines dependencies scripts.postinstall`.
- Context7 `/likec4/likec4`: Model API (`model.deployment.nodes()`, `instancesOf`, `getMetadata`) from `apps/docs/src/content/docs/tooling/model-api.mdx`, `packages/core/README.md`.

### Secondary (MEDIUM confidence)
- `.planning/workstreams/architecture-views/research/ARCHITECTURE.md` (G10, G11, N3, render split, Pitfall mapping) and `PITFALLS.md` Pitfall 2 (its "MEASURED" claim is independently reproduced here).

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - versions read from the registry and the lockfile; one seam `SUS` explained as no-observation.
- Architecture: HIGH for the API facts and the namespace/membership/metadata rules (all run); MEDIUM for the placement design (prototype, an assumption A1).
- Pitfalls: HIGH - each pitfall was triggered (static import, ENOENT, missing dirs, typo'd key, undrawn nodes) or read from the cited line.

**Research date:** 2026-10-27
**Valid until:** 2026-11-26 (likec4 is pinned exactly; a bump to 1.61+ would require re-running M1-M3; `yaml` 3.0 is in `next` only)
