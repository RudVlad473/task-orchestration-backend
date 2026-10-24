---
created: 2026-09-27T00:00:00.000Z
title: "edge and monitoring Kustomizations both define IngressRoute grafana in namespace monitoring -- whichever reconciles last wins"
area: infra
severity: moderate
files:

  - k8s/platform/edge/ingressroute-monitoring.yaml
  - k8s/monitoring/configs/ingressroute.yaml

audit_acknowledged:
  milestone: v1.4
  at: 2026-09-30
---

## Problem

Found live during Plan 13-09's Task 2 (resuming Flux after the restart-ladder window):
`k8s/platform/edge/ingressroute-monitoring.yaml` (Plan 13-06) and
`k8s/monitoring/configs/ingressroute.yaml` (Plan 13-07) both define an `IngressRoute` object named
`grafana` in namespace `monitoring` -- the first a deliberate placeholder routing to
`grafana-placeholder` (a Service with no Endpoints, so Traefik answers a clean 503), the second
the real fix routing to `kube-prometheus-stack-grafana`.

Both objects are owned by different Flux Kustomizations (`edge` owns `k8s/platform/edge`,
`monitoring` owns `k8s/monitoring/configs`), and both Kustomizations are independently `Ready`.
Kubernetes only allows one object with a given name/namespace/kind to exist at a time, so
whichever Kustomization's server-side apply lands most recently silently overwrites the other's
version -- there is no error, no unready state, nothing that would show up in `kubectl get
kustomizations`.

Observed live 2026-09-27: after resuming Flux from a suspended state, `edge` reconciled after
`monitoring` (both triggered by the same event), so `edge`'s placeholder won -- the public
monitoring endpoint (`kanban-board-rud-vlad-473-monitoring.duckdns.org`) returned `503 no
available server` for a period until `monitoring` was forced to reconcile again. Nothing prevents
`edge` from winning the race again on its own next 10-minute reconcile interval, or after any
future cluster event that triggers both to reconcile close together (a node reboot, a `flux
reconcile --with-source`, etc.) -- this is a live, ongoing flakiness risk on the public monitoring
endpoint, not a one-time incident that self-resolved.

13-07's own commits (`bc8dc36`, `0b3551d`) explicitly repointed the Service reference in BOTH
files rather than removing either one, so this dual-object condition has existed, undetected,
since 13-07 landed -- it simply never manifested visibly until a forced reconcile raced the two
Kustomizations against each other for the first time.

## Solution

Remove the now-redundant placeholder Service/IngressRoute/IngressRoute-http objects from
`k8s/platform/edge/ingressroute-monitoring.yaml` (or its whole file, if nothing else in
`k8s/platform/edge/kustomization.yaml`'s resource list still needs it), leaving
`k8s/monitoring/configs/ingressroute.yaml` as the sole owner of the `grafana`/`grafana-http`
IngressRoute names. Verify: after the removal, `k3s kubectl get ingressroute grafana -n
monitoring -o jsonpath='{.metadata.labels}'` should show `kustomize.toolkit.fluxcd.io/name:
monitoring` and stay that way across multiple forced reconciles of both Kustomizations in
sequence (`edge` then `monitoring`, and the reverse order) -- proving the race is actually closed,
not just currently favoring the right winner. Consider adding a check to
`scripts/verify-k8s-invariants.py` (a new invariant) that fails when two rendered roots define an
object with the same GVK+namespace+name, closing this class of defect structurally rather than
relying on someone noticing the next flap.

## Resolution

Closed by **quick task 261024-nvj (2026-10-24)** on branch `fix/grafana-route-ownership`, in two
commits: `d5373d1` adds the invariant, `cb53636` removes the placeholder. The race recurred on
2026-10-24 (both README dashboard links answered 503, `edge` the last writer), which is why this was
fixed rather than left filed.

The fix followed this todo's own Solution: `k8s/platform/edge/ingressroute-monitoring.yaml` and its
resource line are gone, so `k8s/monitoring/configs/ingressroute.yaml` is the sole owner of the
`grafana` and `grafana-http` IngressRoute names. The invariant the todo suggested is I11 in
`scripts/verify-k8s-invariants.py`: it fails when any object identity (API group, kind, namespace,
name) is rendered by two Flux Kustomizations. It is red on the unfixed `ba8b5cf` naming both routes and
green on the fix.

Not closed by these commits: the live race-closure proof this todo asks for (forcing `edge` then
`monitoring`, and the reverse, and re-reading the owner label after each). That is the post-merge
procedure the operator runs at the merge gate, so no live result is claimed here.
