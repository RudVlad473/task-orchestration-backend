---
phase: quick-261024-nvj
plan: 01
subsystem: infra
tags: [flux, kustomize, traefik, grafana, invariants]
requires: []
provides:
  - invariant I11 (no object rendered by two Flux Kustomizations)
  - monitoring as the sole Flux owner of IngressRoute grafana and grafana-http
key-files:
  created: []
  modified:
    - scripts/verify-k8s-invariants.py
    - scripts/verify-k8s-invariants-selftest.py
    - k8s/platform/edge/kustomization.yaml
    - k8s/monitoring/configs/ingressroute.yaml
    - docs/INFRA_RUNBOOK.md
    - docs/INFRA_ARCHITECTURE.md
  deleted:
    - k8s/platform/edge/ingressroute-monitoring.yaml
key-decisions:
  - "Approach A: delete the edge placeholder; monitoring owns the routes (alternatives B keep-with-priority and C move-to-edge rejected in the plan)"
  - "Rollout T1: one merge, then force monitoring to reconcile once edge reports the merge revision"
  - "I11 reads the Flux Kustomization CRs from the rendered roots and fails closed on unmodelled Flux specs"
status: complete
actuals:
  tokens: 60000
  tasks: 3
  commits: 3
plan_head_before: ba8b5cf
plan_head_after: ece1146
---

# Phase quick-261024-nvj Plan 01: Grafana IngressRoute ownership Summary

I11 in the k8s gate now rejects any object rendered by two Flux Kustomizations, and the `edge`
placeholder routes are removed so `monitoring` is the only owner of `IngressRoute` `grafana` and
`grafana-http`. Nothing is on main and nothing was written to production.

## Commits (branch fix/grafana-route-ownership, from ba8b5cf, pushed by name, no upstream set)

| # | SHA | Message | Hook |
|---|---|---|---|
| 1 | d5373d1 | test(k8s): fail when two Flux Kustomizations render the same object | full pre-commit, BUILD SUCCESSFUL |
| 2 | cb53636 | fix(k8s): make the monitoring Kustomization the only owner of the Grafana routes | full pre-commit |
| 3 | ece1146 | docs(infra): record monitoring as the sole owner of the Grafana routes | full pre-commit |

`git log --oneline ba8b5cf..HEAD` shows exactly these three. Commit 1 is red on the gate by design.
Push: `git push origin fix/grafana-route-ownership` only; PR URL offered by GitHub is
https://github.com/RudVlad473/task-orchestration-backend/pull/new/fix/grafana-route-ownership
(not opened).

## Test directions checked

| Direction | Result | Evidence |
|---|---|---|
| Selftest RED before implementation | `AttributeError: module 'verify_k8s_invariants' has no attribute 'check_i11_single_flux_owner'` | run in the terminal before the function existed |
| Selftest GREEN with I11 | `selftest OK -- I1-I11 each fire on an engineered violation; clean fixtures report nothing` (also green on the fix tree) | terminal |
| Gate RED on the unfixed tree (worktree at ba8b5cf + commit 1) | exit 1, exactly two FAIL lines | `261024-nvj-red-worktree.txt` |
| Gate RED on ba8b5cf with the final script (detached checkout, script copied in, checkout removed afterwards) | exit 1, exactly two FAIL lines | `261024-nvj-red-ba8b5cf.txt` |
| Gate GREEN on the fix (cb53636 tree) | exit 0 | `261024-nvj-green.txt` |

Exact red lines on ba8b5cf (both files carry the same two):

```
FAIL: I11: IngressRoute/grafana (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations: edge (k8s/platform/edge), monitoring (k8s/monitoring/configs) -- the last one to apply overwrites the others on every reconcile
FAIL: I11: IngressRoute/grafana-http (traefik.io, namespace monitoring) is rendered by 2 Flux Kustomizations: edge (k8s/platform/edge), monitoring (k8s/monitoring/configs) -- the last one to apply overwrites the others on every reconcile
```

Green line: `invariants OK -- 12 kustomization root(s) checked; ... and no object is rendered by two Flux Kustomizations`.

Only the two routes were found as duplicates, matching the planner's probe; no other root pair
collides today. Selftest cases (all in `run_i11_cases`): duplicate fires, version-only difference
fires, other namespace clean, other API group clean, cluster-scoped duplicate says "no namespace",
unrendered path, absent `spec.path`, `targetNamespace`, foreign `sourceRef`, disjoint objects clean.

Also green on the fix: `bash scripts/verify-k8s-manifests.sh` (all 12 roots valid, edge renders 3
resources), `python3 scripts/verify-comments.py check` (OK: 315 files), edge renders exactly
Certificate plus two ClusterIssuers, monitoring renders `grafana` and `grafana-http`.

Not checked: I11 is a unit-and-real-tree gate; I did not mutation-test it beyond the red run on
ba8b5cf (the red run is the mutation: the pre-fix tree).

## Flux garbage-collection finding (read via gh api, read-only)

kustomize-controller `v1.9.5` go.mod pins `github.com/fluxcd/pkg/ssa v0.76.2`.

kustomize-controller v1.9.5, `internal/controller/kustomization_controller.go`, `deleteObjects`
(lines 1407-1409): `opts := ssa.DeleteOptions{ PropagationPolicy: ..., Inclusions: manager.GetOwnerLabels(obj.Name, obj.Namespace),` with Exclusions on the `prune: disabled` and `reconcile: disabled` annotations.

fluxcd/pkg tag ssa/v0.76.2, `ssa/manager_delete.go`, `Delete`:

```
sel, err := metav1.LabelSelectorAsSelector(&metav1.LabelSelector{MatchLabels: opts.Inclusions})
...
if !sel.Matches(labels.Set(existingObject.GetLabels())) {
    return m.changeSetEntry(object, SkippedAction), nil
}
```

So a pruning Kustomization skips a stale object whose live `kustomize.toolkit.fluxcd.io/name` label
names a different Kustomization. Consequence: if `monitoring` has already re-applied the routes
(label `monitoring`) when `edge` prunes, there is no gap. If the label still reads `edge` when `edge`
prunes, `edge` deletes both routes.

## Pre-merge baseline (read-only, measured 2026-10-24 about 15:40Z)

- `curl -s -o /dev/null -w '%{http_code}'` on
  `https://kanban-board-rud-vlad-473-monitoring.duckdns.org/public-dashboards/9b5bf7c8d3c743e1868eed84c9d3e5df` returned 503.
- Same for `.../public-dashboards/cb7a5fc49fcb43108ba637fee7639b6d` returned 503.
- `ssh netcup-prod`, `k3s kubectl get ingressroute -n monitoring grafana grafana-http` jsonpath:
  `grafana owner=edge svc=grafana-placeholder` and `grafana-http owner=edge svc=grafana-placeholder`.
- Kustomizations `edge` and `monitoring`: Ready True, both Applied revision `main@sha1:ba8b5cf8e1e0644c2aaa67b2b8dd7761c62d57cb`.

## Worst-case outage of the rollout

The endpoint already answers 503. The live owner label reads `edge` right now, so if `edge`
reconciles the merge commit before `monitoring` does, `edge` prunes both routes and the hostname
answers 404 (and Traefik's default certificate) until `monitoring` applies the same revision:
normally seconds to about two minutes; ceiling about ten minutes plus dependency requeue if
`monitoring`'s reconcile errors. Step 4 below cuts that to operator reaction time. If `monitoring`
reconciles first, its server-side apply relabels the objects to `monitoring` and, per the GC finding
above, `edge` then skips them: no gap. Plain HTTP Grafana is never served in any state: the only
`web` router for the host is `grafana-http`, which keeps the HTTPS redirect (I10 green), and during a
deletion gap no router matches.

## Prune-transition decision (from the plan)

T1: one merge, then force `monitoring` to reconcile once `edge` reports the merge revision. The
two-merge alternatives (T2 `prune: false` on edge, T3 `prune: disabled` annotation on edge's copies)
were rejected because they cost two production changes to protect a route that is already down. The
orphaned placeholder Service `grafana-placeholder` and Middleware `grafana-redirect-https` in
namespace `monitoring` are pruned by `edge` too (their live labels also read `edge`).

## Post-merge procedure (for the orchestrator; every WRITE needs the user's approval at the merge gate)

The VM has no flux CLI, so reconciles use the `reconcile.fluxcd.io/requestedAt` annotation, which is
what `flux reconcile` sets. Let `$SHA` be the merge commit and `$KC="ssh netcup-prod k3s kubectl"`.

1. READ, owner label first: `$KC get ingressroute -n monitoring grafana grafana-http -o jsonpath='{range .items[*]}{.metadata.name}{" "}{.metadata.labels.kustomize\.toolkit\.fluxcd\.io/name}{"\n"}{end}'`
2. WRITE (optional, production), speed up source pickup:
   `$KC annotate gitrepository/flux-system -n flux-system reconcile.fluxcd.io/requestedAt="$(date +%s)" --overwrite`
3. READ, poll `$KC get kustomization -n flux-system edge monitoring` until both show Applied revision `main@sha1:$SHA`.
4. WRITE (production): `$KC annotate kustomization/monitoring -n flux-system reconcile.fluxcd.io/requestedAt="$(date +%s)" --overwrite`, then wait until `monitoring` is Ready again.
5. READ: both IngressRoutes labelled `monitoring` and routing to `kube-prometheus-stack-grafana`
   (same jsonpath as step 1 plus `{.spec.routes[*].services[*].name}`); `$KC get service grafana-placeholder -n monitoring` and `$KC get middleware grafana-redirect-https -n monitoring` both NotFound.
6. READ, from the workstation:
   - `curl -s -o /dev/null -w '%{http_code}\n' https://kanban-board-rud-vlad-473-monitoring.duckdns.org/public-dashboards/9b5bf7c8d3c743e1868eed84c9d3e5df` expecting 200
   - `curl -s -o /dev/null -w '%{http_code}\n' https://kanban-board-rud-vlad-473-monitoring.duckdns.org/public-dashboards/cb7a5fc49fcb43108ba637fee7639b6d` expecting 200
   - `curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' http://kanban-board-rud-vlad-473-monitoring.duckdns.org/` expecting 301 to the https URL
   - `curl -s -o /dev/null -w '%{http_code}\n' http://kanban-board-rud-vlad-473-monitoring.duckdns.org/.well-known/acme-challenge/probe` expecting a non-301 status
7. WRITES (production), the todo's race-closure proof: annotate `edge`, then `monitoring`, then
   `monitoring`, then `edge` (same annotate form as step 4, waiting for Ready between), re-reading the
   owner label after each; expect `monitoring` every time.

## Deviations from Plan

None - plan executed as written. Two notes:
- Two Bash tool hooks (too-dense commands, heredoc writes) forced splitting commands and using `Edit`; no effect on the result.
- The selftest helper `run_i11_cases` is a separate function called from `run_cases`, to keep `run_cases` readable; the plan did not prescribe the structure.

## What I could not verify

- Every live post-merge result (labels after rollout, the 200s on both dashboard URLs, the 301 and
  ACME probes, the race-closure proof). The 503 baseline above is the only live HTTP measurement.
- I11 against a Flux-level field it fails closed on (`patches`, `postBuild`, and so on) beyond the
  fixtures for `targetNamespace` and `sourceRef`; the other four field names share the same code path.
- Helm-created and controller-created objects are invisible to I11 (documented as a known hole).

## Cleanup state

Red checkout `.claude/worktrees/grafana-route-red` removed (`git worktree list` no longer shows it).
Worktree `grafana-route` `git status --short` is empty. Scratch commit-message file trashed. No
Testcontainers left by this task. docs/wiki copies untouched (project CLAUDE.md). docs/diagrams has no
Kustomization-ownership or placeholder content (rg found nothing), so no diagram and no Maintenance
Note file list changed. PLAN, SUMMARY, STATE and the evidence txt files are uncommitted in the
primary checkout's quick directory for the orchestrator to copy.

## Self-Check: PASSED

Commits d5373d1, cb53636, ece1146 are ancestors of HEAD on fix/grafana-route-ownership; the three
evidence files exist in the quick directory; the deleted edge file is absent.
