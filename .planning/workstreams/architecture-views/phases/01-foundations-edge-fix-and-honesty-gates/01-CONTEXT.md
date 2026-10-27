# Phase 1: Foundations, Edge Fix and Honesty Gates - Context

**Gathered:** 2026-10-27
**Status:** Ready for planning

<domain>
## Phase Boundary

The published Physical view stops naming the nonexistent `edge` namespace: Traefik moves to a `kube-system` node. `docs/architecture/sync.mjs check` then fails loudly when a namespace, a placement, a metadata key or an instance's view membership drifts, so every element Phase 2 adds is checked from the commit that adds it. Requirements: PHYS-01, GATE-02, GATE-03, GATE-04. Success criteria are in ROADMAP.md Phase 1 and are not restated here.

</domain>

<decisions>
## Implementation Decisions

### Namespace source rule (GATE-04)
- **D-01:** A namespace is "defined" if it is a `Namespace` kind or appears in any string `namespace:` or `spec.targetNamespace` value under `k8s/**` (clarified during planning: `cert-manager` appears only as a `targetNamespace`, at `k8s/platform/cert-manager/cert-manager.yaml:38`; same intent; including `k8s/platform`, `k8s/monitoring`, `k8s/flux-system`). This covers the four in `k8s/platform/namespaces.yaml` plus `kube-system`, `flux-system` and `cert-manager`.
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Workstream
- `.planning/workstreams/architecture-views/ROADMAP.md` — Phase 1 goal, success criteria, planning notes, Open Question 6
- `.planning/workstreams/architecture-views/REQUIREMENTS.md` — PHYS-01, GATE-02, GATE-03, GATE-04
- `.planning/workstreams/architecture-views/STATE.md` — current position and decisions
- `.planning/workstreams/architecture-views/research/SUMMARY.md` — research summary
- `.planning/workstreams/architecture-views/research/ARCHITECTURE.md` — build order (M-A), `render()` split
- `.planning/workstreams/architecture-views/research/PITFALLS.md` — Pitfall 2 (unconnected instances are not drawn)

### Code and standards
- `docs/architecture/sync.mjs` — the drift check; `workloads()`, `render()`, `check()`, `VIEWPOINTS`
- `docs/architecture/drill.fish` — drill harness; `fresh()`, `run()`, drills D1 to D9
- `docs/architecture/drill-output.txt` — recorded drill output
- `docs/architecture/model/deployment.c4` — the `edge` namespace defect (29 lines)
- `docs/architecture/model/model.c4` — metadata keys in use
- `docs/architecture/package.json` — likec4 1.60.0 pinned; `yaml` to be added
- `k8s/platform/namespaces.yaml`, `k8s/platform/traefik/helmchartconfig.yaml` (namespace `kube-system`), `k8s/platform/cert-manager/cert-manager.yaml` (`flux-system`)
- `.githooks/pre-commit` — LikeC4 drift step, trigger path list
- `scripts/pre-commit-likec4-selftest.py` — hook selftest cases
- `docs/DIAGRAM_CONVENTIONS.md` — the diagramming standard

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `VIEWPOINTS` constant and the viewpoint-tag check in `check()`: the pattern the metadata-key allowlist follows.
- `computed.views()` and `computed.elements()` already loaded in `check()`: the view-membership check reuses them.
- `drill.fish` `fresh()` already copies `k8s/platform`; `run()` prints exit code and output.

### Established Patterns
- `--root` and `--model` flags point the check at a scratch copy so drills never edit the real model.
- Exit codes: 0 clean, 1 drift, 2 usage, 3 tooling missing.
- Each check appends a string to `problems`; the tally line reports counts.
- Checks land in the same commit as the elements they check.

### Integration Points
- `workloads()` currently walks only `k8s/base` and `k8s/data`.
- `.githooks/pre-commit` trigger path list (line ~206) lacks `k8s/platform`.
- CI job `likec4-model-drift` also runs the check.

</code_context>

<specifics>
## Specific Ideas

No specific requirements beyond the roadmap's red-then-green discipline: every rule is shown failing before it exists and passing after.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. Roadmap Open Questions 1 to 5, 7 and 8 belong to Phases 2 to 4.

</deferred>

---

*Phase: 1-Foundations, Edge Fix and Honesty Gates*
*Context gathered: 2026-10-27*
