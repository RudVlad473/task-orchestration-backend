---
phase: 01-foundations-edge-fix-and-honesty-gates
plan: 03
subsystem: architecture-model
tags: [likec4, drift-check, pre-commit, kubernetes, drills, documentation]

requires:
  - phase: 01-02
    provides: "placement, metadata-key and view-membership rules, drills D10 to D16b, 01-drills-before.txt, the accepted limits A10 to A12"
provides:
  - "sync.mjs K8S_DIRS guard: a missing tree among six exits 4 naming it; a manifest that does not parse already exits 4 (plan 01)"
  - "drills D14, D14b, D14c, each recorded exit=0 under the pre-phase script and exit=4 under the current one"
  - "pre-commit LikeC4 trigger widened to the single pathspec k8s"
  - "selftest case 16 (k8s/platform namespace typo), seen failing against the pre-phase hook, 16/16 against the new one"
  - "docs/DIAGRAM_CONVENTIONS.md and README.md describing the checks as built, with the three placement limits stated before Phase 2 planning"
affects: [phase-2 physical view standard, phase-2 cert-manager and monitoring elements]

actuals:
  tokens: 2900
  tasks: 3
  commits: 3
plan_head_before: 943007e148b1ae95ffce8b63021dfcb1b8c88f25
plan_head_after: 0dbb556c1d7a19fa8e730c02d8168f67fb982130

tech-stack:
  added: []
  patterns: ["required-tree list checked with existsSync before any manifest is read", "hook trigger as a coarse pathspec so every reader's input is covered", "red direction shown by running the new selftest against a copy of the pre-phase hook through --hook"]

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-03-selftest-red.txt
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-03-selftest-green.txt
  modified:
    - docs/architecture/sync.mjs
    - docs/architecture/drill.fish
    - docs/architecture/drill-output.txt
    - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-drills-before.txt
    - .githooks/pre-commit
    - scripts/pre-commit-likec4-selftest.py
    - docs/DIAGRAM_CONVENTIONS.md
    - README.md

key-decisions:
  - "K8S_DIRS lists k8s/base, k8s/data, k8s/platform, k8s/overlays, k8s/monitoring, k8s/flux-system; any missing exits 4 (A3)"
  - "Hook trigger is the pathspec k8s, not k8s/platform alone, so overlays and monitoring typos are refused at commit time too (A2)"
  - "Docs fix only the statements this phase made false; the Physical and Scenarios row rewrite stays with Phase 2"

requirements-completed: [GATE-04]

coverage:
  - id: D1
    description: "A missing k8s tree or an unparseable manifest stops the check with exit 4 and a named path, where the pre-phase script exited 0"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "drill-output.txt D14 (k8s/overlays), D14b (k8s/monitoring), D14c (k8s/platform/broken.yaml BAD_INDENT) all exit=4; 01-drills-before.txt shows all three exit=0; node docs/architecture/sync.mjs check exits 0 with the unchanged tally"
        status: pass
    human_judgment: false
  - id: D2
    description: "A staged k8s/platform namespace typo runs the LikeC4 step and is refused as drift; the pre-phase hook lets it through"
    requirement: "GATE-04"
    verification:
      - kind: integration
        ref: "01-03-selftest-red.txt (FAIL test_16, 15/16, exit=1 with --hook on the 68b8f3a copy); 01-03-selftest-green.txt (16/16, exit=0)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The standard and the README state the checks as built and record limits A10, A11 and A12"
    requirement: "GATE-04"
    verification:
      - kind: other
        ref: "grep acceptance checks on docs/DIAGRAM_CONVENTIONS.md and README.md (stale phrases 0, new phrases 1 each)"
        status: pass
    human_judgment: true
    rationale: "Whether the prose reads clearly enough for Phase 2 planning is a reader judgment; the greps prove only that the stale statements are gone and the three quoted phrases exist."

duration: about 40min (start time not captured at executor start; estimate)
completed: 2026-10-27
status: complete
---

# Phase 01 Plan 03: Fail-closed fact sources, widened hook trigger and standard as built Summary

**`sync.mjs check` now exits 4 when any of six k8s trees is missing, the pre-commit hook runs the LikeC4 step for any staged path under `k8s` (selftest case 16 failed against the pre-phase hook and passes against the new one, 16/16), and the standard states the checks as built together with the three accepted placement limits.**

## Performance

- **Duration:** about 40 min (the start time was not captured; estimate)
- **Completed:** 2026-10-27
- **Tasks:** 3 of 3
- **Files modified:** 8 source and doc files, 2 new evidence files, 2 re-recorded evidence files

## Accomplishments

- Guard: `K8S_DIRS` plus a check in `check()` between `loadTooling()` and `k8sDocs()` printing `k8s fact source missing: <dir>` for each missing tree and exiting 4. The header comment already listed exit 4 (plan 01). The real repo still prints `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads)`.
- Drills D14, D14b, D14c appended; `drill-output.txt` now holds 27 sections (20 exit=1, 3 exit=4, 4 exit=0: baseline, D5, D13b, D16b). A fresh run is byte-identical to the committed file (`cmp` exit 0). `git diff --numstat 68b8f3a -- docs/architecture/drill-output.txt` is 61 additions, 0 deletions.
- Hook: the trigger pathspecs `k8s/base k8s/data` became `k8s`; the decisions comment now names migrations, any manifest under `k8s` and KafkaTopics.java and gives the reason and cost; the known-hole sentence names only the cited Java packages under `src/main/java`.
- Selftest: `TRAEFIK_CONFIG`, `test_16_misspelled_platform_namespace_is_refused`, docstring count (ten drift refusals), and a preflight that returns `yaml is not installed. Run: pnpm --dir docs/architecture install` (environment problem, exit 2) when the parser is absent.
- Docs: Physical row provenance cell, a new "Built in the architecture-views workstream, Phase 1" bullet (namespace, placement with A10 to A12 as a sub-list, metadata-key, view-membership, parser, fail-closed rules), the Drills, Wired and Key vocabulary bullets, the Proposed-not-built list (only the server-title check remains) and the README pre-commit paragraph.

## Task Commits

1. **Task 1: K8S_DIRS guard, drills D14, D14b, D14c** - `eed8138` (feat); full hook passed
2. **Task 2: hook trigger widened to k8s, selftest case 16** - `955a006` (feat); full hook passed (the changed hook ran on its own commit)
3. **Task 3: standard and README as built** - `0dbb556` (docs); full hook passed (the plan allows `--no-verify` for this docs-only commit; the dispatch forbade it, so the hook ran)

**Plan metadata:** committed separately after this file (docs).

## Drill outcomes D10 to D16b (as recorded)

| Drill | Before (68b8f3a, 01-drills-before.txt) | After (drill-output.txt) |
|---|---|---|
| D10 deleted cert-manager node | exit=0 | exit=1 |
| D10b kube-sytem | exit=0 | exit=1 |
| D11 backup in kanban-prod | exit=0 | exit=1 |
| D12 `sourc` | exit=0 | exit=1 |
| D12b node, instance, relationship keys | exit=0 | exit=1 |
| D13 lonely instance | exit=0 | exit=1 |
| D13b lonely + relationship (control) | exit=0 | exit=0 |
| D14 k8s/overlays missing | exit=0 | exit=4, `k8s fact source missing: k8s/overlays` |
| D14b k8s/monitoring missing | exit=0 | exit=4, `k8s fact source missing: k8s/monitoring` |
| D14c broken manifest | exit=0 | exit=4, `k8s fact source unreadable: k8s/platform/broken.yaml: BAD_INDENT ...` |
| D15 Deployment under k8s/platform | exit=0 | exit=1 |
| D16 certmgr in flux-system | exit=0 | exit=1 |
| D16b certmgr in cert-manager (control) | exit=0 | exit=0 |

The re-recorded `01-drills-before.txt` differs from plan 02's version only by the 12 appended lines for D14, D14b and D14c; every earlier section reproduced unchanged.

## Selftest lines

- Red (`01-03-selftest-red.txt`, `--hook` on a copy of `git show 68b8f3a:.githooks/pre-commit`): `FAIL: test_16_misspelled_platform_namespace_is_refused: the commit succeeded; expected a refusal.`, `15/16 passed`, `exit=1`. No other case failed.
- Green (`01-03-selftest-green.txt`, the real hook): `PASS: test_16_misspelled_platform_namespace_is_refused`, `16/16 passed`, `exit=0`.

## Decisions Made

- Trigger scope `k8s` (A2) over `k8s/platform`: the readers read all of `k8s/**`, so overlays and monitoring typos are refused locally too.
- The `existsSync` guard (A3) over relying on `readdirSync`: removing `k8s/monitoring` or `k8s/flux-system` changes no finding, so only an explicit list can see it.
- A10 measured when the task ran: 9 Flux Kustomizations under `k8s/flux-system`, none with a string-valued `targetNamespace`.

## Deviations from Plan

None - plan executed exactly as written, with one choice inside the plan's latitude: Task 3's commit ran the full hook instead of `--no-verify` because the dispatch instruction forbade `--no-verify`.

## Issues Encountered

- A tool-side command-density hook rejected three long Bash commands before they ran; they were re-issued as short steps with the same content. No file was touched by a rejected call.
- `fastTest` and `spotlessCheck` printed `UP-TO-DATE` in all three hook runs (no Java or resource input changed), so Gradle reused earlier results. The hook ran in full each time.
- The red-direction evidence file contains the scratch repo's commit line (`[master d798dbd] x`); it is a scratch repo hash and not a repository commit.

## Known Stubs

None.

## Threat Flags

None. T-01-07 is mitigated by the K8S_DIRS guard (D14, D14b), T-01-08 by the widened trigger and case 16, T-01-09 by evidence files holding only sync.mjs, drill and selftest output (gitleaks passed on all three commits). T-01-10 did not arise because the docs-only commit ran the hook.

## Next Phase Readiness

Phase 1 plans are complete. Phase 2 elements must carry a `workload` or `k8s/` `source` anchor, or be recorded in the Checks section of `docs/DIAGRAM_CONVENTIONS.md` as not placement-checked; an element anchored on a directory applied by a Flux Kustomization that sets `spec.targetNamespace` needs `sync.mjs` to model the field first.

## Self-Check

- Key files exist: `docs/architecture/sync.mjs`, `docs/architecture/drill.fish`, `docs/architecture/drill-output.txt`, `.githooks/pre-commit`, `scripts/pre-commit-likec4-selftest.py`, `docs/DIAGRAM_CONVENTIONS.md`, `README.md`, `01-drills-before.txt`, `01-03-selftest-red.txt`, `01-03-selftest-green.txt`, this file.
- Commits `eed8138`, `955a006` and `0dbb556` are ancestors of HEAD.
- `git rev-list --count 943007e..HEAD` from the ledger base: 3.
- Plan verification re-run after Task 3: `node docs/architecture/sync.mjs check` exit 0, `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt` exit 0, selftest `16/16 passed`, `scripts/verify-comments.py check` exit 0.
- Scratch under `docs/architecture/out/` removed with `trash-put`.

## Self-Check: PASSED
