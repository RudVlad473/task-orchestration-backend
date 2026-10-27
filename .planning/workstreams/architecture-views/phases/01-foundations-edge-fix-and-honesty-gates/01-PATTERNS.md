# Phase 1: Foundations, Edge Fix and Honesty Gates - Pattern Map

**Mapped:** 2026-10-27
**Files analyzed:** 9 (all modifications; none created except evidence files)
**Analogs found:** 9 / 9 (every file is its own closest analog: the phase extends an existing seam)

All analog paths below were confirmed git-tracked (`git ls-files`). No gitignored mirrors are cited.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `docs/architecture/sync.mjs` (new readers, rules, `render()` split, `workloads()` port) | utility / drift check | batch, transform | itself: `loadLikeC4`, `VIEWPOINTS` check, `workloads()`, `render()` | exact |
| `docs/architecture/drill.fish` (D10+, `SYNC` override, `fresh()` copies `k8s/`) | test (drill harness) | batch | itself: D3, D4, D6 | exact |
| `docs/architecture/drill-output.txt` | test evidence | batch | itself (recorded run) | exact |
| `docs/architecture/model/deployment.c4` | config (model DSL) | n/a | itself (29 lines) | exact |
| `docs/architecture/package.json` + `pnpm-lock.yaml` | config | n/a | itself (`likec4` pin) | exact |
| `.githooks/pre-commit` | config / middleware (gate) | request-response | itself, lines 181-232 | exact |
| `scripts/pre-commit-likec4-selftest.py` | test | request-response | itself: `test_03`, `test_04` | exact |
| `docs/DIAGRAM_CONVENTIONS.md` (Checks section only) | docs | n/a | itself, lines 156-185 | exact |
| Evidence files `*-red.txt` / `*-green.txt` in the phase dir | test evidence | n/a | `261027-if9-T1-red.txt` .. `T4-red.txt` (cited at `DIAGRAM_CONVENTIONS.md:172-173`) | role-match |

## Pattern Assignments

### `docs/architecture/sync.mjs` (utility, batch/transform)

**Analog:** itself. Rules are added by the "append a string to `problems`" pattern inside `check()`.

**Lazy tooling loader, exit 3** (`sync.mjs:28-36`). Copy this for `yaml`; a static `import 'yaml'` turns "tooling missing" into exit 1 and breaks selftest cases 09/11 (RESEARCH Pattern 1):
```js
async function loadLikeC4() {
  try {
    return await import('likec4')
  } catch (err) {
    if (err?.code !== 'ERR_MODULE_NOT_FOUND') throw err
    console.error('likec4 is not installed for docs/architecture. Run: pnpm --dir docs/architecture install')
    process.exit(3)
  }
}
```
`check()` calls it first (`sync.mjs:279-280`) so missing tooling wins over drift. Extend to load both modules; keep `generate` mode independent of `yaml`.

**Constant-next-to-rule pattern for the metadata allowlist** (`sync.mjs:38-40`, check at `296-306`):
```js
const VIEWPOINTS = ['logical', 'process', 'development', 'physical', 'scenarios']
...
for (const view of computed.views()) {
  if (view.id === 'index') continue
  const own = [...view.tags].filter((t) => VIEWPOINTS.includes(t))
  if (own.length === 0) problems.push(`view ${view.id} carries no viewpoint tag`)
```
Add `const METADATA_KEYS = ['source', 'workload']` directly below `VIEWPOINTS`. Walk four surfaces (`computed.elements()`, `computed.deployment.nodes()`, `.instances()`, `computed.relationships()`), each via `getMetadata()`; message names key and owner id.

**Both-ways set comparison pattern** (`sync.mjs:308-314`), the template for the namespace rule (manifest namespace with no model node; model node with no manifest):
```js
const modelWorkloads = new Set(elements.flatMap((e) => [e.getMetadata('workload')].flat().filter(Boolean)))
for (const w of workloads()) if (!modelWorkloads.has(w)) problems.push(`k8s ${w} has no element carrying metadata workload '${w}'`)
for (const w of modelWorkloads) if (!workloads().has(w)) problems.push(`model claims workload ${w} but k8s/ has no such manifest`)
```

**Source-exists loop to leave alone** (`sync.mjs:316-319`); note it silently skips a misspelled key, which the allowlist closes:
```js
for (const e of elements) {
  const src = e.getMetadata('source')
  if (src && !existsSync(join(root, src))) problems.push(`${e.id}: metadata source '${src}' does not exist`)
}
```

**`workloads()` to port to `yaml`** (`sync.mjs:217-234`); output must stay `CronJob/postgres-backup, Deployment/app, StatefulSet/postgres, StatefulSet/redpanda`, and the walk list becomes `['k8s/base','k8s/data','k8s/platform']` (RESEARCH M5):
```js
for (const d of ['k8s/base', 'k8s/data']) walk(join(root, d))
```
Port detail: `parseAllDocuments`, throw on any `doc.errors.length`, skip null documents (`o && typeof o === 'object'`), keep `kind` filter `Deployment|StatefulSet|CronJob`. Preserve the D4 message pair verbatim.

**`render()` split** (`sync.mjs:238-274`): extract `renderTables`, `renderForeignKeys`, `renderPackages`, `renderPackageEdges`, each returning lines in current order; `render()` concatenates around the literals `'  extend postgres {'`, `'  }'`, `'  extend kanban {'`, `'  }'`, `'}'`. Proof: `check` exit 0 and zero diff in `generated.c4`.

**Tally and exit shape** (`sync.mjs:321-328`); do not change the tally string (it would rewrite all of `drill-output.txt`, and the selftest regex `OK_TALLY` expects `^OK \(\d+ elements(, \d+ [a-z ]+)+\)$`):
```js
if (problems.length) {
  console.error(`DRIFT (${tally}):\n- ${problems.join('\n- ')}\n${cycleLine(pkgs)}`)
  process.exit(1)
}
```

**Usage/exit codes header** (`sync.mjs:6`): update "Exit codes" comment if a missing-fact-source code is added (RESEARCH recommends exit 4, `existsSync` guard over a `K8S_DIRS` constant; the hook treats non-0/1 as "did not run to completion", `pre-commit:226-231`). `.mjs` is out of scope for `verify-comments.py`, but keep comments plain text.

**New rule code to copy from RESEARCH.md** (prototyped against the real repo, "Code Examples" lines 426-476): `manifestNamespaces(docs)` (must collect `Namespace.metadata.name` plus any string `namespace`/`targetNamespace` key at any depth; `cert-manager` is only reachable via `targetNamespace`; non-string values must be skipped), `placements(docs, root)` (kustomization namespace inherits through `resources`, inherited before own `metadata.namespace`), and the view-membership snippet (RESEARCH M2):
```js
const inView = new Set()
for (const v of computed.views()) if (v.isDeploymentView?.()) for (const n of v.nodes()) if (n.kind === 'instance') inView.add(n.id)
for (const i of computed.deployment.instances()) if (!inView.has(i.id)) problems.push(`instance ${i.id} appears in no deployment view`)
```
Instances only: empty namespace nodes (D-07) are never drawn.

---

### `docs/architecture/drill.fish` (test harness, batch)

**Analog:** itself. Drill shape, copy from D3 (`drill.fish:43-46`) and D6 (`59-62`):
```fish
echo "== D3: a new k8s Deployment named worker"
fresh
printf 'apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: worker\n' >$scratch/k8s/base/worker.yaml
run
```
```fish
echo "== D6: a view loses its viewpoint tag"
fresh
sed -i '/^    #process$/d' $scratch/model/views.c4
run
```
Deletion drill style, D4 (`drill.fish:48-51`): `trash-put $scratch/k8s/data/postgres/postgres.yaml`.

**`fresh()` to change** (`drill.fish:12-20`): line 18 copies only three dirs; replace with a whole-tree copy:
```fish
cp -r $repo/k8s/base $repo/k8s/data $repo/k8s/platform $scratch/k8s/
```
becomes `cp -r $repo/k8s $scratch/` (drop the `mkdir ... $scratch/k8s` component or keep it harmlessly). **`SYNC` override** after line 9: `set -q SYNC; and set sync $SYNC` (the old script copy must live under `docs/architecture/` so `likec4` resolves, e.g. gitignored `docs/architecture/out/`).

**`run()`** (`drill.fish:22-27`) prints `exit=` then each output line indented two spaces; new drills only append `echo "== D10: ..."; fresh; <edit>; run` blocks before the final `trash-put $scratch` (line 100). Drill specs (D10, D10b, D11, D12, D12b, D13, D13b, D14) with scratch edits and expected findings are tabulated in RESEARCH "Drill specification". Edits must target the post-fix model. D13b is the green half (exit 0).

Header comment (`drill.fish:2-5`) says "Every drill ... expected to be caught except D5"; keep true, and mark D13b/green drills as controls.

---

### `docs/architecture/drill-output.txt` (evidence)

Recorded run, format (`drill-output.txt:1-9`): `== title`, `  exit=N`, indented output lines. Regenerate with `fish docs/architecture/drill.fish > docs/architecture/drill-output.txt`. Gate: sections baseline through D9 byte-identical after the `workloads()` port (RESEARCH M7), D3 and D4 unchanged. "Before" half of each new drill is recorded by running with `SYNC=` pointing at the pre-rule script, saved as a separate evidence file, not in this file.

---

### `docs/architecture/model/deployment.c4` (model DSL)

**Analog:** itself (`deployment.c4:9-11`), the defect:
```
      edge = namespace 'edge' {
        instanceOf traefik
      }
```
Target (RESEARCH M6): id and title both `kube-system` with `instanceOf traefik`; add empty nodes `flux-system`, `cert-manager`, `monitoring` in the same `namespace '...' { }` form as the existing `prod`/`nonprod`/`data` blocks (lines 13-26). `views.c4:37` (`include netcup, netcup.**`) needs no edit. Land this edit in the same commit as the namespace rule (a red commit is refused by the hook and CI).

---

### `docs/architecture/package.json` + `pnpm-lock.yaml` (config)

**Analog:** `package.json` itself:
```json
"devDependencies": {
  "likec4": "1.60.0"
}
```
Add `"yaml": "2.9.1"` exact. Command: `pnpm --dir docs/architecture add -D --save-exact yaml@2.9.1`, run against `docs/architecture` itself (not a subdirectory: the `out/` importer trap). Commit lockfile together; lock diff must show only the `yaml` lines (about 13).

---

### `.githooks/pre-commit` (gate)

**Analog:** itself (`pre-commit:205-210`), the trigger list:
```sh
LIKEC4_TRIGGERS=$(git diff --cached --name-only --no-renames -- \
  src/main/resources/db/migration k8s/base k8s/data docs/architecture \
  ':(glob)src/main/java/**/constant/KafkaTopics.java') || {
```
Add `k8s/platform` (or the whole `k8s`, RESEARCH A2/Open Question 1). Rewrite the now-false comment at `pre-commit:198-199` ("Metadata sources outside the trigger paths (k8s/platform and the cited Java packages) are caught in CI only"). This file is in scope of `scripts/verify-comments.py`: no planning ids (D-xx, GATE-xx, "Phase 1") in comments.

Status handling to leave untouched (`pre-commit:219-231`): 1 = drift refusal, other non-zero = "did not run to completion".

---

### `scripts/pre-commit-likec4-selftest.py` (test, request-response)

**Analog:** `test_03` (`selftest.py:304-310`):
```python
def test_03_new_deployment_commit_all_is_refused():
    case_dir, repo = make_repo("new-deployment", True)
    add_worker_manifest(repo)
    git(repo, "add", WORKER_MANIFEST)
    before = head_of(repo)
    code, out = commit(case_dir, repo, "-a")
    refused_for_drift(code, out, repo, before, "k8s Deployment/selftest-worker has no element")
```
New `test_16_...` (cases are discovered by `test_` name; last existing is `test_15` at line 427): `replace_in(repo, "k8s/platform/traefik/helmchartconfig.yaml", "namespace: kube-system", "namespace: kube-sytem")`, `git add`, `commit(case_dir, repo)`, `refused_for_drift(..., "namespace kube-sytem is defined in")`. Helpers: `replace_in` (line 122, asserts the old text exists), `refused_for_drift` (240-246, asserts `BANNER`, `DRIFT_REFUSED_TEXT`, finding, HEAD unmoved), constants at lines 33-48. Update the module docstring counts (lines 4-5). Python rules: deliberate lint exceptions go on the line with a reason. No planning ids in comments. Also direction check with `--hook` pointed at a copy lacking the trigger path. Preflight checks `node_modules/likec4` only; a missing `yaml` surfaces as exit 3 inside the case.

---

### `docs/DIAGRAM_CONVENTIONS.md` (docs, Checks section lines 156-185)

**Analog:** itself. Stale statements to update in the same change (Pitfall 8): line 161 ("workload set matches `k8s/base` and `k8s/data`"), 171-173 (drill count and list), 175 ("runs 15 cases, 9 of them drift refusals, and passes 15/15"), 176-183 (trigger list; "caught in CI only" sentence), and add a "Built in <this phase>" bullet list mirroring the existing "Built in 261027-if9" bullet style (`:163-169`). Leave the Physical-row text (`:105`, `:230`) to STD-01 in Phase 2.

---

## Shared Patterns

### Problems list, loud failure
**Source:** `sync.mjs:281`, `285`, `299-300`, `313`
**Apply to:** every new rule. Push one human-readable string naming the offender; never throw for drift. Exit 0/1/2/3 contract; a malformed manifest (`doc.errors`) fails loudly, never silently half-reads.

### Scratch-root isolation
**Source:** `sync.mjs:20-26` (`--root`, `--model`) and `drill.fish:10-20`
**Apply to:** all drills and any prototyping. Never edit the real repo; scratch goes under `docs/architecture/out/` (gitignored) and is trashed after.

### Red-then-green evidence
**Source:** `261027-if9-T1-red.txt` .. `T4-red.txt`, referenced at `docs/DIAGRAM_CONVENTIONS.md:172-173`
**Apply to:** each rule. Capture the red from the working tree (rule present, model unfixed), then land rule and model fix in ONE commit. Evidence prose must not contain credential-shaped text (gitleaks); do not paste Helm values or raw `kubectl get -o yaml`.

### Behaviour-preserving port gate
**Apply to:** `workloads()` port and `render()` split. Full `fish docs/architecture/drill.fish` output byte-identical to the committed `drill-output.txt`, and `generated.c4` unchanged, before any new rule is added.

## No Analog Found

| File / piece | Role | Data Flow | Reason |
|--------------|------|-----------|--------|
| Placement rule (kustomization namespace resolver) | utility | transform | No placement check exists today; `workloads()` compares names only. Use the RESEARCH prototype (Pattern 4, Code Examples). |
| Required-directory guard (`K8S_DIRS`, exit 4) | utility | batch | Existing readers rely on `readdirSync` ENOENT (exit 1, misleading hook text). Use RESEARCH M9 design. |
| Manifest namespace reader (`targetNamespace`, string filter) | utility | transform | Nothing parses YAML in the repo's JS yet; use RESEARCH Pattern 3. |

## Metadata

**Analog search scope:** `docs/architecture/`, `.githooks/`, `scripts/`, `docs/DIAGRAM_CONVENTIONS.md`, `k8s/platform`
**Files read:** sync.mjs (full), drill.fish (full), deployment.c4, package.json, drill-output.txt (first 25 lines), pre-commit (190-234), selftest (225-319 plus constants), DIAGRAM_CONVENTIONS.md (156-185)
**Pattern extraction date:** 2026-10-27
