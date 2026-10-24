---
phase: quick-261024-nvj
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - scripts/verify-k8s-invariants.py
  - scripts/verify-k8s-invariants-selftest.py
  - k8s/platform/edge/ingressroute-monitoring.yaml
  - k8s/platform/edge/kustomization.yaml
  - k8s/monitoring/configs/ingressroute.yaml
  - docs/INFRA_RUNBOOK.md
  - docs/INFRA_ARCHITECTURE.md
  - .planning/todos/pending/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md
  - .planning/todos/completed/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md
autonomous: true
requirements: [QUICK-261024-nvj]

estimate:
  tokens: 150000
  raw_tokens: 150000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "On the unfixed manifests (origin/main ba8b5cf), `python3 scripts/verify-k8s-invariants.py --no-provisional` exits 1 with exactly two FAIL lines, both I11, one naming IngressRoute/grafana and one naming IngressRoute/grafana-http, each as rendered by both `edge` (k8s/platform/edge) and `monitoring` (k8s/monitoring/configs)"
    - "After the fix the same gate exits 0 and `python3 scripts/verify-k8s-invariants-selftest.py` exits 0 with I11 cases that fire on an engineered duplicate and stay quiet on near-misses (other namespace, other API group)"
    - "Exactly one Flux Kustomization, `monitoring`, renders IngressRoute grafana and grafana-http; k8s/platform/edge renders only the two ClusterIssuers and the `grafana` Certificate"
    - "The real routes keep the HTTPS redirect outside the ACME challenge path (I10 still green) and the /login rate limit, so removing the edge copies opens no plain-HTTP Grafana and breaks no HTTP-01 renewal"
    - "The SUMMARY states which test directions were checked, the worst-case outage of the rollout, and the exact post-merge commands (annotate-based, because the VM has no flux CLI) with curl checks expecting 200 on both README dashboard URLs"
    - "INFRA_RUNBOOK.md, INFRA_ARCHITECTURE.md and the moved todo name `monitoring` as the sole owner and point at I11; the original 2026-09-27 runbook paragraph stays as history"
  artifacts:
    - path: "scripts/verify-k8s-invariants.py"
      provides: "I11: no object identity (API group, kind, namespace, name) is rendered by two Flux Kustomizations; fails closed on unmodelled Flux specs"
      contains: "check_i11_single_flux_owner"
    - path: "scripts/verify-k8s-invariants-selftest.py"
      provides: "I11 fixtures: duplicate fires, version-only difference fires, namespace/group near-misses stay clean, fail-closed cases fire"
      contains: "check_i11_single_flux_owner"
    - path: "k8s/monitoring/configs/ingressroute.yaml"
      provides: "the only definition of IngressRoute grafana and grafana-http"
      contains: "kube-prometheus-stack-grafana"
    - path: ".planning/todos/completed/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md"
      provides: "closed todo with a Resolution section"
      contains: "## Resolution"
    - path: "/home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-SUMMARY.md"
      provides: "test directions checked, rollout worst case, post-merge production commands and verification"
      contains: "reconcile.fluxcd.io/requestedAt"
  key_links:
    - from: "scripts/verify-k8s-invariants.py main()"
      to: "check_i11_single_flux_owner"
      via: "the per-root render loop keeps each root's docs in a dict keyed by repo-relative root; Flux Kustomization CRs are read from those same rendered docs"
      pattern: "check_i11_single_flux_owner\\("
    - from: "k8s/flux-system/edge.yaml spec.path ./k8s/platform/edge"
      to: "k8s/platform/edge/kustomization.yaml resources"
      via: "Flux renders that kustomization.yaml; removing the resource line is what removes the routes from edge's inventory"
      pattern: "certificate-monitoring.yaml"
    - from: ".github/workflows/invariant-checks.yml job k8s-invariants"
      to: "scripts/verify-k8s-invariants.py --no-provisional"
      via: "CI already runs the gate and its selftest on pull_request, so I11 is enforced with no workflow edit"
      pattern: "verify-k8s-invariants"
---

<objective>
Make exactly one Flux Kustomization own `IngressRoute/grafana` and `IngressRoute/grafana-http` in
namespace `monitoring`, so the public Grafana dashboards stop flapping to 503, and pin the whole
class of defect with a new invariant, I11, that fails whenever two Flux Kustomizations render the
same object.

Purpose: since 2026-09-27 the `edge` Kustomization (k8s/platform/edge/ingressroute-monitoring.yaml,
a placeholder routing to a Service with no Endpoints) and the `monitoring` Kustomization
(k8s/monitoring/configs/ingressroute.yaml, the real routes to `kube-prometheus-stack-grafana`)
declare the same two objects. Whichever applies last wins. On 2026-10-24 `edge` won at 15:03:00Z and
both README dashboard links return 503 "no available server".

Output: I11 plus selftest cases (proven red on ba8b5cf, green on the fix), the edge placeholder
removed, docs and the todo updated, and a SUMMARY carrying the post-merge production procedure the
orchestrator runs only after the user approves the merge.

Work location: the worktree /home/andre/dev/kanban-board-backend/.claude/worktrees/grafana-route on
branch `fix/grafana-route-ownership` (from origin/main ba8b5cf, upstream unset on purpose). Every
command below runs with that worktree as its working directory, and every repo path is relative to
it. The primary checkout at /home/andre/dev/kanban-board-backend holds the user's uncommitted work
and is never edited, except that evidence files and the SUMMARY go into this plan's own quick
directory, /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r
(called the quick dir below), which the orchestrator copies into the branch. Never commit the
PLAN, the SUMMARY, the evidence files or STATE.md.

## Facts measured during planning (2026-10-24, read-only)

- Flux layout (k8s/flux-system): nine Kustomizations, all `prune: true`, all sourced from
  GitRepository `flux-system` (branch main, interval 1m), none using targetNamespace, namePrefix,
  nameSuffix, patches, components or postBuild. `edge` path ./k8s/platform/edge (dependsOn
  cert-manager, interval 10m); `monitoring` path ./k8s/monitoring/configs (dependsOn
  monitoring-controllers, interval 10m). `platform` (./k8s/platform) has its own kustomization.yaml
  listing only namespaces.yaml and traefik/, so it does not re-render edge.
- A planner probe rendered every Flux path with the pinned kubectl and indexed objects by (group,
  kind, namespace, name). The only identities rendered by more than one Kustomization are
  traefik.io/IngressRoute monitoring/grafana and monitoring/grafana-http, both by edge and
  monitoring. No other duplicate exists today.
- Baseline on ba8b5cf: the current gate prints "invariants OK -- 12 kustomization root(s) checked",
  the selftest prints "selftest OK -- I1-I10", `python3 scripts/verify-comments.py check` prints
  "OK: 316 files". The pre-commit hook does not run the k8s gate; CI runs it (invariant-checks.yml,
  job k8s-invariants) on push to main and on pull_request only.
- Live (ssh netcup-prod, read-only): IngressRoutes grafana and grafana-http route to the placeholder
  Service; grafana carries labels kustomize.toolkit.fluxcd.io/name=edge and
  kustomize.toolkit.fluxcd.io/namespace=flux-system; both Kustomizations Ready at
  main@sha1:ba8b5cf8e1e0644c2aaa67b2b8dd7761c62d57cb. `command -v flux` on the VM finds nothing, so
  post-merge reconciles use the `reconcile.fluxcd.io/requestedAt` annotation, which is what `flux
  reconcile` itself sets. Flux v2.9.5, kustomize-controller v1.9.5.
- HTTP-01: both ClusterIssuers solve with `http01.ingress.ingressClassName: traefik`, so
  cert-manager creates its own solver Ingress; neither grafana-http copy is needed for issuance.
- No diagram source under docs/diagrams draws Kustomization ownership or the placeholder:
  monitoring-nodes.mmd and ingress-to-pod.mmd mention Grafana only as a node and a Service.
- Every workflow's push trigger is main-only; pull_request runs invariant-checks and secret-scan.
  Pushing the branch by name triggers no workflow and Flux tracks main only.

## Approaches considered (project directive: two alternatives and a trade-off matrix)

Ownership fix:

| Approach | Pros/Cons | Why picked |
|---|---|---|
| A. Delete k8s/platform/edge/ingressroute-monitoring.yaml and its resource line; `monitoring` is the sole owner | Pro: one definition at every moment, two lines of change, no extra routers. Con: on a cold bootstrap the monitoring hostname serves Traefik's default certificate until `monitoring` applies its routes and Grafana's Service exists (the placeholder's original reason, observed 2026-09-26); con: edge's garbage collection may delete the live routes once during rollout (see Transition) | Picked: the only option with one owner by construction. The bootstrap gap lasts minutes, only on a rebuild, and HTTP-01 issuance never depended on the placeholder |
| B. Keep the placeholder under different names with an explicit low `priority` | Pro: keeps the bootstrap certificate behaviour. Con: two routers per hostname forever; correctness moves from Flux ordering to Traefik priority (the placeholder's Host rule and the real Host rule are the same length, so without an explicit priority the tie-break decides); keeps a dead Service and Middleware; I10 classifies two redirect routes | Rejected: trades a Flux race for a Traefik-priority dependency |
| C. Move the real routes into edge, delete them from monitoring | Pro: edge already owns the Certificate. Con: the rate-limit Middleware and the subchart-derived Service name move away from the HelmRelease that defines that name; the same rollout risk, mirrored onto monitoring's garbage collection | Rejected: wrong layer, no gain over A |
| (dependsOn or ordering only) | Does not stop the periodic 10-minute race | Rejected by the brief |

Rollout (transition):

| Approach | Pros/Cons | Why picked |
|---|---|---|
| T1. One merge, then force `monitoring` to reconcile once `edge` reports the merged revision | Pro: one production rollout; correct whether or not Flux skips deleting an object relabelled by another Kustomization. Con: if `edge` collects first while the live labels still say `edge`, both routes are deleted until `monitoring` applies the same revision (normally seconds to about two minutes; ceiling is monitoring's 10m interval if its reconcile errors), and the forced reconcile cuts that to operator reaction time | Picked: the endpoint already answers 503, so the worst case swaps 503 for 404 for minutes on a route that is down anyway |
| T2. The Flux FAQ two-step: set `prune: false` on `edge`, merge, move, re-enable | Pro: no deletion. Con: two merges to main and two rollouts; the placeholder Service and Middleware are orphaned while prune is off and need a manual delete or a third change; the race stays live through step one | Rejected: two production changes to protect a route that is already down |
| T3. Add `kustomize.toolkit.fluxcd.io/prune: disabled` to edge's copies for one release, remove them in the next | Pro: no deletion in release two whichever side won last. Con: two merges; both Kustomizations apply through the same kustomize-controller field manager, so monitoring's apply strips the annotation and edge's re-adds it on every flap; the race stays live through release one | Rejected: same two-release cost as T2 |

Invariant:

| Approach | Pros/Cons | Why picked |
|---|---|---|
| I-A. I11 inside scripts/verify-k8s-invariants.py, reusing its render loop and reading the Flux Kustomization CRs from the rendered k8s/flux-system root | Pro: no new tool, no extra render, CI already runs it, pure function the selftest can drive. Con: models plain kustomize rendering, not Flux-level patches or substitutions | Picked, with I11 failing closed on any Flux-level field that can rewrite identity, so the fidelity gap cannot read as clean |
| I-B. `flux build kustomization` per Kustomization | Pro: Flux-faithful, applies Flux-level patches and postBuild. Con: a new binary to pin by sha256 in verify-k8s-manifests.sh plus a dry-run kustomization-file mode per object; more CI moving parts for fields no Kustomization uses today | Rejected |
| I-C. A live check comparing every Kustomization's `.status.inventory` | Pro: also sees Helm-created and hand-applied objects. Con: runs after merge, needs cluster credentials in CI | Rejected as the gate; recorded as a known hole |

Non-obvious trade-offs:
- Performance and memory: I11 adds no render. It keeps every root's parsed docs in memory at once
  (a few MB; the largest single object is the node-exporter-full dashboard ConfigMap, about 470 KB)
  and does one dict pass, O(total objects).
- State invalidation: Flux keeps its own inventory per Kustomization and both Kustomizations write
  through one field manager, so server-side apply silently replaces whichever spec landed last. This
  is why the bug never surfaced as an error or a not-Ready condition, and why the rollout needs the
  ordering in T1.
- Security: with edge's copies gone, port 80 for the hostname is either monitoring's redirect route
  (I10 keeps its ACME-path negation) or, during a deletion gap, no router at all (404). Neither state
  serves Grafana over plain HTTP. The login rate limit lives only on monitoring's route and is
  unaffected.

Data flow of I11, in three sentences: main() renders each kustomize root once, as today, and keeps
the parsed docs per root; I11 picks the `kustomize.toolkit.fluxcd.io` Kustomization CRs out of those
docs and maps each `spec.path` to the root rendered for it. It then indexes every object those roots
render by (API group, kind, namespace, name) to the Kustomizations that render it, and any identity
with two or more owners becomes one FAIL line naming them. A Flux path with no rendered root, a
source other than the repo's GitRepository, or a spec field that can rewrite identity is a FAIL too,
so an unmodelled case never passes as clean.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@docs/SESSION_LESSONS.md
@docs/CODE_STYLE.md
@docs/INFRA_RUNBOOK.md
@docs/INFRA_ARCHITECTURE.md
@scripts/verify-k8s-invariants.py
@scripts/verify-k8s-invariants-selftest.py
@scripts/verify-k8s-manifests.sh
@k8s/flux-system/edge.yaml
@k8s/flux-system/monitoring.yaml
@k8s/flux-system/kustomization.yaml
@k8s/platform/edge/kustomization.yaml
@k8s/platform/edge/ingressroute-monitoring.yaml
@k8s/platform/edge/certificate-monitoring.yaml
@k8s/monitoring/configs/ingressroute.yaml
@.planning/todos/pending/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Add invariant I11 and its selftest cases, and prove I11 red on the unfixed tree</name>
  <files>scripts/verify-k8s-invariants.py, scripts/verify-k8s-invariants-selftest.py</files>
  <precondition>The worktree HEAD is ba8b5cf with a clean `git status --short`, and `docker info` succeeds (the pre-commit hook's gitleaks and fastTest need Docker).</precondition>
  <read_first>
    - scripts/verify-k8s-invariants.py (whole file: the docstring's invariant list and Known holes, check_i10_redirect_acme_exclusion as the model for a cross-object pure function, and main()'s render loop)
    - scripts/verify-k8s-invariants-selftest.py (expect_violation, expect_clean, and the I10 fixtures)
    - docs/CODE_STYLE.md rule 14 and the header of scripts/verify-comments.py (planning ids such as a two-digit plan pair and the word Task followed by a number are banned in comments)
    - k8s/flux-system/edge.yaml and k8s/flux-system/monitoring.yaml (the spec shape I11 reads)
  </read_first>
  <behavior>
    - Two Kustomizations, edge at k8s/platform/edge and monitoring at k8s/monitoring/configs, each rendering traefik.io/v1alpha1 IngressRoute grafana in namespace monitoring: one violation that contains "I11: IngressRoute/grafana (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations: edge (k8s/platform/edge), monitoring (k8s/monitoring/configs)"
    - The same object rendered as traefik.io/v1alpha1 by one Kustomization and traefik.io/v1 by the other: still a violation, because object identity ignores the API version
    - The same kind and name in two different namespaces: clean
    - The same kind, name and namespace under two different API groups (traefik.io and traefik.containo.us): clean
    - A cluster-scoped kind (ClusterIssuer) rendered by two Kustomizations: a violation whose text contains "no namespace"
    - A Kustomization whose spec.path has no rendered root: a violation that names the Kustomization and the normalized path
    - A Kustomization whose spec carries targetNamespace: a violation that names the field
    - A Kustomization whose sourceRef is not GitRepository flux-system: a violation containing "sourceRef"
    - Two Kustomizations rendering disjoint objects (edge renders a Certificate and a ClusterIssuer, monitoring renders the IngressRoutes): clean
  </behavior>
  <action>
    In scripts/verify-k8s-invariants.py add a pure function check_i11_single_flux_owner(flux_kustomizations, rendered_by_root) that returns a list of violation strings and touches no disk or subprocess, so the selftest can drive it with hand-built dicts, matching how check_i10_redirect_acme_exclusion is built.

    flux_kustomizations is a list of rendered docs whose apiVersion starts with kustomize.toolkit.fluxcd.io/ and whose kind is Kustomization. rendered_by_root maps a repo-relative root using forward slashes (for example k8s/platform/edge) to that root's list of parsed docs. For each Kustomization, in this order, fail closed with an I11 violation that names the Kustomization and continue to the next one when any of these holds: spec.sourceRef is not kind GitRepository named flux-system with namespace absent or flux-system (include the word sourceRef in the message); spec carries any of targetNamespace, namePrefix, nameSuffix, patches, components or postBuild (name the field; these can rewrite the identity I11 compares, and none is used today); posixpath.normpath of spec.path is not a key of rendered_by_root (name the normalized path; an absent spec.path normalizes to "." and fails the same way). Keep the six field names in one module-level tuple and the source in one module-level constant.

    Otherwise index every dict doc of that root by the identity tuple (API group, kind, metadata.namespace or None, metadata.name), where the group is the part of apiVersion before the slash and the empty string for a core apiVersion such as v1. The version is deliberately left out: Kubernetes stores one object per group, kind, namespace and name. Record (kustomization name, normalized path) as an owner of each identity. For every identity with two or more owners, emit exactly one line, owners sorted by name, in this exact shape: "I11: {kind}/{name} ({group or 'core'}, {namespace text}) is rendered by {N} Flux Kustomizations: {name} ({path}), {name} ({path}) -- the last one to apply overwrites the others on every reconcile", where the namespace text is "namespace {ns}" or "no namespace". Emit violations in a deterministic order (sorted by identity).

    In main(), keep each root's parsed docs in a dict keyed by the root with os.sep replaced by a forward slash, in the existing render loop, with no second render. After the loop, collect the Flux Kustomization docs from all rendered roots, de-duplicated by (namespace, name). If none is found, append the fail-closed violation "I11: no kustomize.toolkit.fluxcd.io Kustomization found in any rendered root -- cannot check single ownership". Otherwise append the output of check_i11_single_flux_owner. A root that failed to render is simply absent from the dict, so a Flux path pointing at it fails closed through the path rule, next to the existing "failed to render" line.

    Update the module docstring in the existing layout, which the comment lint passes today: add I11 to the numbered invariant list with a one-paragraph WHY (two Kustomizations applying the same object through one field manager means the last applier silently wins on every reconcile, with no error and no not-Ready condition; found live as the edge/monitoring Grafana route race), add I11 to the first-line summary and the Scope paragraph, and add Known holes: objects created by a HelmRelease or by a controller, and objects applied by hand, are invisible to I11 (the runtime check is comparing Kustomization inventories on the cluster); the fail-closed field list must be revisited before any Kustomization adopts one of those fields. Extend the success message with ", and no object is rendered by two Flux Kustomizations". Write no planning ids in comments or docstrings; name dates, files and invariants instead.

    In scripts/verify-k8s-invariants-selftest.py add the cases listed in the behavior block using expect_violation and expect_clean, with the full message prefix from the first case as that case's must_contain string, and change the final print to say I1-I11. Write the selftest cases first and run the selftest to watch it fail on the missing function before implementing.

    Then run the gate on this still-unfixed tree, write its output to the quick dir as 261024-nvj-red-worktree.txt, and confirm it exits 1 with exactly two FAIL lines, the grafana line and the grafana-http line. If any other FAIL line appears, stop and report it: it is either a defect in I11 or a duplicate the planner's probe missed, and the brief forbids allow-listing it.

    Commit only the two script paths with git commit -- scripts/verify-k8s-invariants.py scripts/verify-k8s-invariants-selftest.py and a conventional message (for example test(k8s): fail when two Flux Kustomizations render the same object) whose body says the gate is red on this commit by design and the next commit fixes the manifests, ending with the Co-Authored-By trailer from the session reminder. Use the full pre-commit hook, never the no-verify flag. If the hook's fastTest dies from host memory pressure (docs/SESSION_LESSONS.md lesson 9), stop and report BLOCKED; you cannot authorize skipping the hook yourself.
  </action>
  <verify>
    <automated>
python3 scripts/verify-k8s-invariants-selftest.py
python3 scripts/verify-k8s-invariants.py --no-provisional > /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-worktree.txt; test $? -eq 1
diff <(grep '^FAIL: ' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-worktree.txt | cut -d' ' -f1-3) <(printf 'FAIL: I11: IngressRoute/grafana\nFAIL: I11: IngressRoute/grafana-http\n')
grep -qF 'I11: IngressRoute/grafana (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations: edge (k8s/platform/edge), monitoring' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-worktree.txt
grep -qF 'I11: IngressRoute/grafana-http (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations: edge (k8s/platform/edge), monitoring' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-worktree.txt
python3 scripts/verify-comments.py check
git diff --quiet HEAD -- scripts/
    </automated>
    Run each line as its own call; every line must exit 0. Before I11 exists the second line fails (the gate exits 0), so this verify cannot pass without the work.
  </verify>
  <done>The selftest passes with the I11 cases, the gate on the unfixed tree exits 1 with exactly the two I11 lines (evidence saved as 261024-nvj-red-worktree.txt in the quick dir), the comment lint passes, and the two scripts are committed on fix/grafana-route-ownership with the full hook.</done>
</task>

<task type="auto">
  <name>Task 2: Remove the edge copies so monitoring is the sole owner, and prove the gate green</name>
  <files>k8s/platform/edge/ingressroute-monitoring.yaml, k8s/platform/edge/kustomization.yaml, k8s/monitoring/configs/ingressroute.yaml</files>
  <read_first>
    - k8s/platform/edge/ingressroute-monitoring.yaml (what is being removed: a Service, a redirect Middleware and two IngressRoutes, all only there for the pre-Grafana state)
    - k8s/platform/edge/kustomization.yaml and k8s/platform/edge/certificate-monitoring.yaml (what edge keeps)
    - k8s/monitoring/configs/ingressroute.yaml (the real routes, which stay byte-identical below the header comment)
  </read_first>
  <reversibility rating="reversible">git revert restores the edge copies; after merge a revert would also bring back the race, which I11 would then flag in CI.</reversibility>
  <action>
    Per Approach A and rollout T1 in the objective: delete k8s/platform/edge/ingressroute-monitoring.yaml with git rm and remove its line from the resources list of k8s/platform/edge/kustomization.yaml, leaving clusterissuers.yaml and certificate-monitoring.yaml. Its placeholder Service and its redirect Middleware go with it: nothing else in any rendered root references them (I10 fails closed on an undefined web Middleware, so a stray reference would show up as red), and monitoring's grafana-http uses its own redirect-https Middleware. Edge's own garbage collection removes the two live objects after merge, since only edge ever applied them.

    Do not change any route, Middleware or Service reference in k8s/monitoring/configs/ingressroute.yaml. Only extend its header comment so a reader learns these are the only definitions of these names: a second Flux Kustomization rendering them makes the last applier win on every reconcile, which invariant I11 in scripts/verify-k8s-invariants.py rejects. Keep the existing first line as the summary, add a blank comment line before the new paragraph, keep it to two or three lines, plain text, no planning ids (docs/CODE_STYLE.md rule 14).

    Do not touch k8s/platform/edge/certificate-monitoring.yaml: its comment (the Certificate lives in edge so the hostname can validate before Grafana exists) stays true.

    Run the selftest, the gate (saving its output to the quick dir as 261024-nvj-green.txt), scripts/verify-k8s-manifests.sh (schema validation of every root, pinned kubeconform; it fetches schemas over the network) and the comment lint. Then commit only the three k8s paths with git commit -- k8s/platform/edge/ingressroute-monitoring.yaml k8s/platform/edge/kustomization.yaml k8s/monitoring/configs/ingressroute.yaml and a conventional message (for example fix(k8s): make the monitoring Kustomization the only owner of the Grafana routes) whose body names the race, the I11 gate turning green, and that edge's prune removes the live copies after merge. End it with the Co-Authored-By trailer. Use the full pre-commit hook, never the no-verify flag; report BLOCKED on a memory-pressure hook failure as in Task 1.
  </action>
  <verify>
    <automated>
python3 scripts/verify-k8s-invariants-selftest.py
python3 scripts/verify-k8s-invariants.py --no-provisional > /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-green.txt
grep -q '^invariants OK' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-green.txt
bash scripts/verify-k8s-manifests.sh
test ! -e k8s/platform/edge/ingressroute-monitoring.yaml
diff <("$(bash scripts/verify-k8s-manifests.sh --tool kubectl)" kustomize k8s/platform/edge | grep '^kind: ' | sort) <(printf 'kind: Certificate\nkind: ClusterIssuer\nkind: ClusterIssuer\n')
"$(bash scripts/verify-k8s-manifests.sh --tool kubectl)" kustomize k8s/monitoring/configs | grep -A2 '^kind: IngressRoute$' | grep -qE '^  name: grafana-http$'
python3 scripts/verify-comments.py check
git diff --quiet HEAD -- k8s/ scripts/
    </automated>
    Run each line as its own call; every line must exit 0. The edge root renders exactly the two ClusterIssuers and the Certificate (a failed render yields an empty list, so the diff fails closed); the monitoring root still renders grafana-http.
  </verify>
  <done>The gate prints invariants OK with I11 included, the selftest and the manifest schema gate pass, edge renders three objects and monitoring both IngressRoutes, and the three k8s paths are committed with the full hook.</done>
</task>

<task type="auto">
  <name>Task 3: Re-prove red on ba8b5cf with the final gate, update docs and the todo, write the SUMMARY with the post-merge procedure</name>
  <files>docs/INFRA_RUNBOOK.md, docs/INFRA_ARCHITECTURE.md, .planning/todos/pending/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md, .planning/todos/completed/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md, /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-SUMMARY.md</files>
  <read_first>
    - docs/INFRA_RUNBOOK.md lines 453-575 (Observability on k3s) and 726-741 (the 2026-09-27 paragraph on the race)
    - docs/INFRA_ARCHITECTURE.md lines 101-113 (observability paragraph) and 262-288 (Maintenance Note)
    - .planning/todos/completed/2026-09-05-docker-user-chain-empty-on-the-vm.md (the closed-todo convention: the file moves to completed/ and gains a Resolution section)
    - ~/.claude/gsd-core/templates/summary.md
  </read_first>
  <action>
    Red re-proof with the final gate (the Task 1 run used the gate as it stood then; this pins the committed version). Create a detached checkout at the unfixed commit with git worktree add --detach /home/andre/dev/kanban-board-backend/.claude/worktrees/grafana-route-red ba8b5cf (never git stash). Copy this branch's scripts/verify-k8s-invariants.py over the copy in that checkout's scripts/, run it there with --no-provisional, and save the output to the quick dir as 261024-nvj-red-ba8b5cf.txt. It must exit 1 with exactly the two I11 lines. Then remove the checkout with git worktree remove --force on that path and confirm git worktree list no longer shows it.

    Flux garbage-collection semantics. Confirm from upstream source, read-only via gh api, whether kustomize-controller v1.9.5 skips deleting a stale object whose kustomize.toolkit.fluxcd.io/name label now names a different Kustomization: find the fluxcd/pkg/ssa version in kustomize-controller's go.mod at tag v1.9.5, then read that ssa version's delete path (the DeleteOptions inclusions check) and the controller's prune call. Quote the deciding lines with file and tag in the SUMMARY. If you cannot establish it, say so plainly; the post-merge procedure below is correct either way, and only the best-case claim (no gap at all when the live label already says monitoring) depends on it.

    Pre-merge baseline (read-only): record in the SUMMARY the HTTP status of both README dashboard URLs, https://kanban-board-rud-vlad-473-monitoring.duckdns.org/public-dashboards/9b5bf7c8d3c743e1868eed84c9d3e5df and https://kanban-board-rud-vlad-473-monitoring.duckdns.org/public-dashboards/cb7a5fc49fcb43108ba637fee7639b6d, via curl -s -o /dev/null -w with the http_code variable, and the live owner label and backend Service of IngressRoutes grafana and grafana-http via ssh netcup-prod with k3s kubectl get and a jsonpath. Production is read-only for you: get and describe only, never apply, patch, annotate, delete, reconcile or rollout.

    Docs, per Approach A. In docs/INFRA_RUNBOOK.md keep the 2026-09-27 paragraph under "A pre-existing, unrelated bug surfaced by this session" verbatim as history and append after it a paragraph that opens with a bold "Resolved 2026-10-24 (quick task 261024-nvj, branch fix/grafana-route-ownership)" lead. It states that the race recurred on 2026-10-24 (README dashboards 503, edge the last writer at 15:03:00Z against monitoring at 15:01:03Z, both at ba8b5cf), that the placeholder file and its resource line were removed from platform/edge so k8s/monitoring/configs/ingressroute.yaml is the only definition, that edge keeps the grafana Certificate and the ClusterIssuers, and that I11 in scripts/verify-k8s-invariants.py now fails any second Flux Kustomization rendering the same object (red on ba8b5cf naming both routes, green on the fix). It records the accepted consequence: on a cold bootstrap the hostname serves Traefik's default certificate until monitoring applies its routes, while HTTP-01 issuance uses cert-manager's own solver Ingress and needs neither route. It points at the live ownership check, k3s kubectl get ingressroute -n monitoring grafana with a jsonpath over metadata.labels, expecting kustomize.toolkit.fluxcd.io/name monitoring. Also add a short dated subsection "7. Route ownership (2026-10-24)" at the end of "Observability on k3s" naming monitoring as the sole owner of both IngressRoutes and both Middlewares, with the same live check and a pointer to I11. Do not claim any live post-merge result; those come from the orchestrator after merge. In docs/INFRA_ARCHITECTURE.md extend the sentence on Grafana's own IngressRoute in the observability paragraph to say the routes are declared only in k8s/monitoring/configs/ingressroute.yaml and applied only by the monitoring Flux Kustomization, that platform/edge holds just the hostname's Certificate and the ClusterIssuers, and that I11 enforces single ownership. Edit the originals only, not the docs/wiki copies (project CLAUDE.md). Re-run an rg over docs/diagrams for Kustomization ownership or the placeholder; it is expected to find nothing, in which case no diagram or Maintenance Note file list changes, and the SUMMARY says so. Neither Maintenance Note file list changes: no k8s root is added or removed.

    Todo. git mv the todo from .planning/todos/pending/ to .planning/todos/completed/ and append a "## Resolution" section in the repo's convention: closed by quick task 261024-nvj on 2026-10-24, branch fix/grafana-route-ownership, with the SHAs of the Task 1 and Task 2 commits; the fix followed this todo's own Solution and added the invariant it suggested as I11; the live race-closure proof it asks for (forced reconciles of edge then monitoring, and the reverse, with the owner label staying monitoring) is the post-merge procedure in the SUMMARY, run by the operator at the merge gate, not by this commit. Gitleaks scans .planning prose, so write no credential-shaped values.

    Commit docs/INFRA_RUNBOOK.md, docs/INFRA_ARCHITECTURE.md and both todo paths with git commit -- followed by those four paths, a conventional docs message, the Co-Authored-By trailer and the full hook (the brief forbids the no-verify flag for this task). Then push the branch by name only, git push origin fix/grafana-route-ownership, without setting an upstream: no workflow triggers on a non-main push and Flux tracks main. Never push or merge to main.

    SUMMARY (in the quick dir, not committed by you). Include:
    - the test directions checked: selftest red before implementation; gate red on the unfixed worktree; gate red on ba8b5cf with the final script; gate green on the fix. Name the evidence files and say which duplicates the red runs found (expected: only the two routes).
    - the three commits with SHAs, and the push.
    - the Flux GC finding with quotes, or the statement that it could not be verified.
    - the pre-merge baseline.
    - the worst-case outage: the endpoint is already 503; if the live grafana label reads edge when edge reconciles the merge commit, both routes may be deleted and the hostname answers 404 or Traefik's default certificate until monitoring applies the same revision (normally seconds to about two minutes; ceiling about ten minutes plus dependency requeue if monitoring's reconcile errors), cut to operator reaction time by step 4 below; if it reads monitoring and the GC skip holds, there is no gap.
    - the post-merge procedure for the orchestrator, every write marked as a production write needing the user's approval at the merge gate:
      - (1) read the owner label first;
      - (2) optional write: annotate gitrepository/flux-system in namespace flux-system with reconcile.fluxcd.io/requestedAt set to the current epoch, via ssh netcup-prod with k3s kubectl annotate --overwrite;
      - (3) read: poll k3s kubectl get kustomization -n flux-system edge monitoring until both show Applied revision main@sha1 of the merge commit;
      - (4) write: annotate kustomization/monitoring the same way and wait for it to be Ready again;
      - (5) read: both IngressRoutes labelled monitoring and routing to kube-prometheus-stack-grafana, and the placeholder Service grafana-placeholder and Middleware grafana-redirect-https in namespace monitoring both NotFound;
      - (6) read, from the workstation: curl of both README dashboard URLs expecting 200, curl of http://kanban-board-rud-vlad-473-monitoring.duckdns.org/ expecting 301 to the https URL, and curl of the http ACME challenge path /.well-known/acme-challenge/probe expecting a non-301 status;
      - (7) writes: the todo's race-closure proof, annotating edge then monitoring and then monitoring then edge, re-reading the owner label after each, expecting monitoring every time.
    - what you could not verify (every live post-merge result), the untouched docs/wiki copy, and the cleanup state: no scratch left, the red worktree removed, git status clean.
  </action>
  <verify>
    <automated>
diff <(grep '^FAIL: ' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-ba8b5cf.txt | cut -d' ' -f1-3) <(printf 'FAIL: I11: IngressRoute/grafana\nFAIL: I11: IngressRoute/grafana-http\n')
grep -qF 'I11: IngressRoute/grafana (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-ba8b5cf.txt
grep -qF 'I11: IngressRoute/grafana-http (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-red-ba8b5cf.txt
wt="$(git worktree list --porcelain)" && ! printf '%s\n' "$wt" | grep -q 'grafana-route-red'
test ! -e /home/andre/dev/kanban-board-backend/.claude/worktrees/grafana-route-red
test ! -e .planning/todos/pending/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md
grep -q '^## Resolution' .planning/todos/completed/2026-09-27-two-kustomizations-race-for-the-grafana-ingressroute-name.md
grep -q 'I11' docs/INFRA_RUNBOOK.md && grep -q 'I11' docs/INFRA_ARCHITECTURE.md
python3 scripts/verify-comments.py check
st="$(git status --porcelain)" && test -z "$st"
grep -q 'reconcile.fluxcd.io/requestedAt' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-SUMMARY.md
grep -q 'public-dashboards/9b5bf7c8d3c743e1868eed84c9d3e5df' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-SUMMARY.md
grep -q 'public-dashboards/cb7a5fc49fcb43108ba637fee7639b6d' /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-SUMMARY.md
    </automated>
    Run each line as its own call; every line must exit 0.
  </verify>
  <done>The final gate is proven red on ba8b5cf naming both routes and green on the branch; the red checkout is gone; the runbook, architecture doc and moved todo record the fix and point at I11; the docs commit is made with the full hook and the branch is pushed by name; the SUMMARY carries the test directions, the rollout worst case, the GC finding and the exact post-merge procedure with its curl checks.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| repo main to cluster (Flux) | Anything merged to main is applied to production by kustomize-controller within minutes; the branch is not |
| internet to Traefik web/websecure | Public HTTP and HTTPS requests for the monitoring hostname cross here; Grafana's login is the only authentication gate |
| ACME CA to Traefik web | Let's Encrypt's HTTP-01 validation requests must reach cert-manager's solver, not a redirect |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261024-nvj-01 | Denial of service | IngressRoute grafana and grafana-http during rollout | medium | mitigate | Rollout T1: wait for edge to report the merge revision, then force monitoring to reconcile (Task 3 SUMMARY steps 3-4); worst case stated in the SUMMARY; the endpoint is already 503 before the change |
| T-261024-nvj-02 | Tampering | scripts/verify-k8s-invariants.py I11 (false green) | medium | mitigate | I11 fails closed on an unrendered Flux path, a foreign sourceRef, any of six identity-rewriting spec fields, and zero Kustomizations found; selftest cases pin each, plus the version-insensitive identity and the namespace and group near-misses |
| T-261024-nvj-03 | Information disclosure | port 80 on the monitoring hostname after the edge copies go | low | mitigate | monitoring's grafana-http keeps the HTTPS redirect (I10 green in Task 2); during a deletion gap no router matches, so the answer is 404, never plain-HTTP Grafana |
| T-261024-nvj-04 | Denial of service | grafana-tls renewal (HTTP-01) | low | accept | Both ClusterIssuers use cert-manager's own solver Ingress (ingressClassName traefik), independent of either grafana-http copy; I10 still enforces the challenge-path negation on the remaining redirect route |
| T-261024-nvj-05 | Elevation of privilege | production writes | high | mitigate | The executor is read-only on netcup-prod (get and describe only) and never pushes or merges main; every production write is listed in the SUMMARY for the user to approve at the orchestrator's merge gate |
| T-261024-nvj-SC | Tampering | npm/pip/cargo installs | low | accept | No package install in this plan: PyYAML is already required by the gate, and kubectl and kubeconform come from verify-k8s-manifests.sh pinned by sha256 |
</threat_model>

<verification>
- `python3 scripts/verify-k8s-invariants-selftest.py` exits 0 and reports I1-I11.
- `python3 scripts/verify-k8s-invariants.py --no-provisional` exits 1 on ba8b5cf (exactly the two I11 lines, evidence in the quick dir twice: worktree run and detached-checkout run with the final script) and 0 on the branch head.
- `bash scripts/verify-k8s-manifests.sh` and `python3 scripts/verify-comments.py check` exit 0 on the branch head.
- `git log --oneline ba8b5cf..HEAD` on fix/grafana-route-ownership shows three commits (gate, manifests, docs and todo), each made through the full pre-commit hook; main is untouched.
- Live production verification happens only after the user approves the merge, using the SUMMARY's procedure.
</verification>

<success_criteria>
- One Flux Kustomization (`monitoring`) renders IngressRoute grafana and grafana-http; no identity is rendered by two Flux Kustomizations anywhere in k8s/.
- I11 is in the gate CI already runs, proven to fail without the fix and pass with it, with directions stated in the SUMMARY.
- Docs and the todo name the fix and I11 without rewriting history.
- The SUMMARY lets the orchestrator run the rollout and its curl checks (200 on both README dashboard URLs) without re-deriving anything.
- The worktree is clean, the red checkout is removed, nothing was written to production, and nothing reached main.
</success_criteria>

<output>
Create /home/andre/dev/kanban-board-backend/.planning/quick/261024-nvj-fix-the-grafana-ingressroute-ownership-r/261024-nvj-SUMMARY.md when done (do not commit it; the orchestrator copies the quick dir into the branch).
</output>
