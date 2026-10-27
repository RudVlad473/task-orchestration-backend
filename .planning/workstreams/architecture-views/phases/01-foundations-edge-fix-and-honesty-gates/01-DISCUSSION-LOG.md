# Phase 1: Foundations, Edge Fix and Honesty Gates - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-27
**Phase:** 1-Foundations, Edge Fix and Honesty Gates
**Areas discussed:** Namespace source rule, Parser, Empty namespace nodes, View-membership rule, Metadata-key allowlist

---

## Namespace source rule

| Option | Description | Selected |
|--------|-------------|----------|
| A | `Namespace` kind plus every `namespace:` field under `k8s/**`, checked both ways | ✓ |
| B | `Namespace` kinds only, hand-kept allowlist for kube-system, flux-system, cert-manager | |

**User's choice:** A (default)

---

## Parser

| Option | Description | Selected |
|--------|-------------|----------|
| A | `yaml` for new readers only; `workloads()` stays on regex until GATE-01 | |
| B | Port `workloads()` to `yaml` now | ✓ |

**User's choice:** B (overrode the recommendation)
**Notes:** Widens Phase 1's diff to an existing reader; D-05 requires unchanged output on the unmodified repo.

---

## Empty namespace nodes

| Option | Description | Selected |
|--------|-------------|----------|
| A | Nodes with no instances, checked against the export only | ✓ |
| B | One placeholder instance each | |

**User's choice:** A (default)

---

## View-membership rule

| Option | Description | Selected |
|--------|-------------|----------|
| A | Fail on any instance with no relationship | |
| B | Fail on any instance absent from every computed deployment view | ✓ |

**User's choice:** B (default)

---

## Metadata-key allowlist

| Option | Description | Selected |
|--------|-------------|----------|
| A | Constant in `sync.mjs`, like `VIEWPOINTS` | ✓ |
| B | Derived from `spec.c4` | |

**User's choice:** A (default)

---

## Claude's Discretion

Drill numbering and wording, failure-message text, internal factoring of the new readers.

## Deferred Ideas

None.
