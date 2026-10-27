---
phase: 01-foundations-edge-fix-and-honesty-gates
reviewed: 2026-10-27T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - docs/architecture/sync.mjs
  - docs/architecture/drill.fish
  - docs/architecture/drill-output.txt
  - docs/architecture/model/deployment.c4
  - docs/architecture/package.json
  - docs/architecture/pnpm-lock.yaml
  - .githooks/pre-commit
  - scripts/pre-commit-likec4-selftest.py
  - docs/DIAGRAM_CONVENTIONS.md
  - README.md
findings:
  critical: 0
  warning: 6
  info: 3
  total: 9
status: issues_found
---

# Phase 01: Code Review Report

**Reviewed:** 2026-10-27
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Range reviewed: 76a66cb (exclusive) to HEAD. No structural findings (fallow) were provided.

Evidence base: every file in the list was read in full or as a diff. I ran `node docs/architecture/sync.mjs check` on the real repo (exit 0, unchanged tally). I ran the hook self-test against the real hook (16/16 pass) and against a copy of the hook with the old trigger paths (`k8s/base k8s/data docs/architecture`): only test_16 fails ("the commit succeeded; expected a refusal"), so the new self-test case does fail without the hook change. I drove `sync.mjs check` against scratch copies of the repo under `out/` (since trashed) for each claim below marked "reproduced". Items A10, A11 and A12 from the 01-02 SUMMARY are not reported as bugs.

Overall: the rules work for the cases the drills exercise, and the hook trigger is correct (`git diff --cached` pathspec `k8s` matches the whole tree and nothing beside it). The defects are in what the rules silently do not cover and in the error-handling contract: no BLOCKER found, six WARNINGs that are false negatives, false positives or a broken exit-code promise.

Not checked: Flux and kustomize behaviour beyond what `k8s/` contains today (kustomize semantics were reasoned from the code, not run against kustomize); the CI workflow beyond reading the `likec4-model-drift` job.

## Warnings

### WR-01: An unreadable manifest exits 1 with a stack trace, not 4, and the hook reports it as drift

**File:** `docs/architecture/sync.mjs:263` (contract stated at `:6-7`, and in `docs/DIAGRAM_CONVENTIONS.md` "fail-closed fact sources")
**Issue:** The header promises "4 a k8s fact source is missing or unreadable", but only YAML parse errors (`doc.errors`, line 264) reach `process.exit(4)`. `readFileSync(p, 'utf8')` on line 263 is outside any guard. Reproduced: a dangling `k8s/platform/dangling.yaml` symlink (`ln -s /nonexistent ...`) gives `exit=1` and a Node `ENOENT` stack trace; the same happens for a permission error (`EACCES`) or a `k8s/` entry that is a directory named `*.yaml`. The hook (`.githooks/pre-commit:224-228`) turns exit 1 into "LikeC4 model drift (see the DRIFT list above). Update docs/architecture/model ...", with no DRIFT list above it. That is exactly the misreport the exit-4 design was added to prevent. The same applies to any other uncaught exception in `check()` (for example `readdirSync` on a `k8s` that is a file).
**Fix:** Wrap the read and map failures to exit 4, and make the top level fail closed on unexpected errors:
```js
let text
try {
  text = readFileSync(p, 'utf8')
} catch (err) {
  console.error(`k8s fact source unreadable: ${file}: ${err.code ?? err.message}`)
  process.exit(4)
}
for (const doc of parseAllDocuments(text)) { ... }
```
Optionally wrap `await check()` in a `try/catch` that prints the error and exits with a code other than 1 (for example 5), so the hook never labels a crash as drift.

### WR-02: The hook gives install advice for exit 4, and no hook-level case covers exit 4 or a missing `yaml`

**File:** `.githooks/pre-commit:229-233`, `scripts/pre-commit-likec4-selftest.py` (no case)
**Issue:** The `elif` branch for any status other than 0 and 1 prints "If its tooling is not installed, run: pnpm --dir docs/architecture install". For exit 4 (a deleted `k8s/overlays`, a manifest that does not parse) that advice is wrong; the real cause is only on the line `sync.mjs` printed above it. The DIAGRAM_CONVENTIONS text says the hook "reports [exit 4] as not run to completion and never as drift"; that is true but untested. The self-test has 16 cases and none stages a broken or missing k8s tree, and none exercises the new `yaml` module being absent while `likec4` is present (the preflight only checks `yaml` exists in the real checkout). A regression that mapped exit 4 to drift, or dropped the `yaml` guard, would pass 16/16.
**Fix:** Branch on the status in the hook (`4)` prints "A k8s fact source is missing or unreadable (see above). Fix the manifest tree"). Add two self-test cases: stage a k8s change after writing `k8s/platform/broken.yaml` with `a: [1, 2` and assert the refusal contains "k8s fact source unreadable" and not "LikeC4 model drift"; remove `node_modules/yaml` from a symlinked copy (or point at a node_modules containing only `likec4`) and assert the install command is printed.

### WR-03: An anchored element with no deployment instance is never placement-checked

**File:** `docs/architecture/sync.mjs:371-376`
**Issue:** `checkPlacement` builds `byElement` only from `computed.deployment.instances()`, so an element that has zero instances is skipped entirely. Reproduced: deleting `instanceOf backup` (or `instanceOf traefik`) from `deployment.c4` leaves `check` at exit 0 with only unrelated noise from my scratch root; no finding names the missing instance. The workload rule still passes because the element keeps its `workload` metadata. So the physical view can silently lose a real workload (a StatefulSet, a CronJob, the Traefik config) and the check stays green. The limit is not among A10, A11 or A12.
**Fix:** Iterate the elements that have an anchor (`workload` metadata or a `k8s/` source), not only those with instances, and report `placement: <id> has anchor documents landing in [..] but no deployment instance`. If some anchored elements are deliberately undeployed, record that as an accepted limit in the standard instead.

### WR-04: A trailing slash on a `k8s/` source silently removes the element from the placement check

**File:** `docs/architecture/sync.mjs:379-381`
**Issue:** Anchor matching is `file === s || file.startsWith(`${s}/`)`. With `source 'k8s/platform/traefik/'` it becomes `k8s/platform/traefik//`, nothing matches, `anchors` is empty and line 383 `continue`s. The source-exists check (`:540`, `existsSync(join(root, src))`) still passes for the slashed path. Reproduced: with the trailing slash and traefik's instance moved into `kanban-prod`, `check` exits 0; without the slash it exits 1 with the placement line. This is a false negative that A12 does not describe (A12 covers an element with no anchor, not a mis-written one).
**Fix:** Normalise before matching: `const dir = s.replace(/\/+$/, '')` then `file === dir || file.startsWith(`${dir}/`)`. Better, report an element that has a `k8s/` source but zero anchors as a problem, so the silent skip cannot hide a typo.

### WR-05: A kustomization.yaml without `apiVersion` and `kind` is ignored, producing false placement drift

**File:** `docs/architecture/sync.mjs:319-322`
**Issue:** A document counts as a kustomization only when `apiVersion` starts with `kustomize.config.k8s.io` and `kind === 'Kustomization'`. Kustomize accepts a `kustomization.yaml` that has neither field (both are optional there, and many repos omit them). Every file in the repo today includes both, so nothing fails now. Reproduced: deleting the first two lines (`apiVersion`, `kind`) of `k8s/data/postgres/kustomization.yaml` keeps `namespace: kanban-data` and `resources:` intact and valid for kustomize, yet `check` exits 1 with `placement: postgres is deployed in [kanban-data] in the model, manifests place it in []` and the same for backup. A valid commit is blocked with a message pointing at the wrong thing.
**Fix:** Identify a kustomization by file name, not by its header: `/(^|\/)kustomization\.ya?ml$/` or `Kustomization` (the names kustomize looks for), and accept a missing `apiVersion`/`kind`; still skip Flux kustomizations by their `kustomize.toolkit.fluxcd.io` apiVersion.

### WR-06: Any string `namespace` value, including a reference to a built-in namespace, demands a fake namespace node

**File:** `docs/architecture/sync.mjs:293` (rule text in `docs/DIAGRAM_CONVENTIONS.md`, namespace rule)
**Issue:** `manifestNamespaces` treats every string under a `namespace` or `targetNamespace` key at any depth as a defined namespace. A RoleBinding subject, a ServiceMonitor or IngressRoute service reference, or a Flux `sourceRef.namespace` that points at `default`, `kube-public` or `kube-node-lease` is a reference, not a definition. Reproduced: a RoleBinding in `kube-system` with a subject `namespace: default` makes `check` exit 1 with `namespace default is defined in k8s/platform/rb.yaml but has no namespace node in the deployment model`. Phase 2 will add monitoring and cert-manager manifests, which commonly carry exactly such references, and the only way to clear the refusal is to add a namespace node that models nothing deployed there.
**Fix:** Count only definitions: the name of a `Namespace` object, a kustomization `namespace`, a HelmRelease `spec.targetNamespace`, and `metadata.namespace` of a namespaced object; skip the subject/ref positions (`subjects[].namespace`, `*Ref.namespace`, `namespaceSelector`). Alternatively add a small allowlist of cluster built-ins (`default`, `kube-public`, `kube-node-lease`) and document it next to `METADATA_KEYS`.

## Info

### IN-01: The hook's "Known holes" comment now understates which metadata sources are CI-only

**File:** `.githooks/pre-commit:201-202`
**Issue:** The comment says metadata sources "under src/main/java (the cited Java packages)" are outside the trigger paths. The model also cites `src/main/resources/application.properties` as a `source` (`docs/architecture/model/model.c4:55` and `:62`, plus generated spring_session elements), and that path is not a trigger either. Deleting or renaming it fails the `metadata source ... does not exist` rule in CI only. `docs/DIAGRAM_CONVENTIONS.md` repeats the same narrowing ("under `src/main/java`").
**Fix:** Reword both to "metadata sources outside the trigger paths (the cited Java packages and `src/main/resources/application.properties`)".

### IN-02: The trigger self-test pins only `k8s/platform`, not the rest of the widened tree

**File:** `scripts/pre-commit-likec4-selftest.py:437-443`
**Issue:** I confirmed test_16 fails against the old trigger, so it earns its place. But it would also fail against a trigger of `k8s/base k8s/data k8s/platform`. The hook comment justifies the whole tree by naming `k8s/overlays` and `k8s/monitoring`; neither is exercised, and `k8s/flux-system` is also unpinned. A later "tidy" narrowing to the three workload trees would pass 16/16.
**Fix:** Parametrise test_16 (or add a second case) over a file in `k8s/overlays`, `k8s/monitoring` and `k8s/flux-system`, each with a namespace typo.

### IN-03: `WORKLOAD_DIRS` silently excludes `k8s/monitoring` and `k8s/overlays`

**File:** `docs/architecture/sync.mjs:230-239`
**Issue:** The comment explains leaving out `k8s/flux-system`, but `k8s/monitoring` and `k8s/overlays` are also unscanned and unexplained, while the hook comment now says the namespace and placement readers "read every manifest tree". A raw `Deployment` added under `k8s/monitoring` is therefore not reported by the workload rule even though the same file is parsed for namespaces. Today `k8s/monitoring` holds only HelmReleases, so nothing is wrong yet; it will matter when Phase 2 adds monitoring elements.
**Fix:** State in the comment whether the exclusion is deliberate (Helm-managed workloads) and add `k8s/monitoring/` to `WORKLOAD_DIRS` when Phase 2 models it, with a drill like D15 for that tree.

---

_Reviewed: 2026-10-27_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
