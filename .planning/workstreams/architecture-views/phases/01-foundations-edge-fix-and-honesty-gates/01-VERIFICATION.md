---
phase: 01-foundations-edge-fix-and-honesty-gates
verified: 2026-10-27T19:00:00Z
status: passed
score: 25/25 must-haves verified
covered_files:
  - .githooks/pre-commit
  - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-PLAN.md
  - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-02-PLAN.md
  - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-02-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-03-PLAN.md
  - .planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-03-SUMMARY.md
  - README.md
  - docs/DIAGRAM_CONVENTIONS.md
  - docs/architecture/drill-output.txt
  - docs/architecture/drill.fish
  - docs/architecture/model/deployment.c4
  - docs/architecture/package.json
  - docs/architecture/pnpm-lock.yaml
  - docs/architecture/sync.mjs
  - scripts/pre-commit-likec4-selftest.py
covered_digest: "v3:sha256:b0cd420bacc81444d9e235e7268d15e2506cb67a438eefa9da69ef37190107d3"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 24/25
  gaps_closed:
    - "No sentence in docs/DIAGRAM_CONVENTIONS.md or README.md still lists the unknown-metadata-key check as not built (01-03 must_have)"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Open the Physical (production) view in a browser, for example with `docs/architecture/likec4.fish start`"
    expected: "Traefik sits inside a kube-system node, and no node labelled edge appears. The empty flux-system, cert-manager and monitoring namespaces are not drawn in this view."
    why_human: "The export JSON proves the node set. How the view renders and reads is a visual judgement that grep cannot make, and the verifier has no browser."
---

# Phase 1: Foundations, Edge Fix and Honesty Gates Verification Report

**Phase Goal:** The published Physical view stops naming a namespace that does not exist. The drift check fails loudly when a namespace, a placement, a metadata key or an instance's view membership drifts, so every element Phase 2 adds is checked from the commit that adds it.
**Verified:** 2026-10-27
**Status:** human_needed (every automated truth holds; one browser-render check remains)
**Re-verification:** Yes, after gap closure (quick task 261027-r62, commit 53c0415)

## Goal Achievement

The one gap from the first verification is closed against the file, not the commit message. All five ROADMAP success criteria and all 20 plan-level truths hold on the current tree (HEAD 4554944). The only open item is a visual render check I cannot perform.

### Gap closure check

`docs/DIAGRAM_CONVENTIONS.md:313-314` now reads: "the server-title check, the one check still proposed (the unknown-metadata-key check is built, see the metadata-key rule under Checks)". The metadata-key rule it points to exists at lines 194-195 (the "metadata-key rule: `METADATA_KEYS` in `sync.mjs`" bullet) and `METADATA_KEYS` is in `sync.mjs`. The diff `022e35f..HEAD` touches `docs/DIAGRAM_CONVENTIONS.md` (4 lines) and `.planning` files only; no code, hook, model or drill moved.

Sweep for any other sentence that lists a built check as not built (the 01-03 truth), over `docs/DIAGRAM_CONVENTIONS.md`, `README.md` and `.githooks/pre-commit`:

| Search | Hits | Judgement |
| --- | --- | --- |
| `proposed`, `not built`, `not yet`, `planned`, `future`, `to be built`, `a later` | `DIAGRAM_CONVENTIONS.md:89` (`planned` is a tag name), `:223` ("Proposed by this standard, not built": only the server-title check), `:310` (retiring the Mermaid toolchain, unrelated), `:313` (fixed) | No stale sentence |
| `unknown-metadata`, `metadata-key`, `metadata key` | Lines 194-195 and 313-314 of the standard, both stating the check is built | Consistent |
| `k8s/base`, `k8s/data` | `:105` (Physical row, lists `k8s/platform` and `k8s` too), `:161` (labelled "Proven in spike 007" and followed by "widened to `k8s/platform` in the bullet below"), `:199` (fail-closed list, all six trees) | Dated record with a pointer, not a current claim |
| `outside the trigger`, `not a trigger` | `.githooks/pre-commit:201-204` and `DIAGRAM_CONVENTIONS.md:218`, both about `src/main/java`, which is accurate (`k8s` is in the trigger pathspec at pre-commit:208-210) | Accurate |
| Counts: "16 cases", "27 sections", "23 drills" | `DIAGRAM_CONVENTIONS.md:202-212` | Selftest is 16/16 (re-run). Drill-output exit lines: baseline plus 26 drill/control sections; exit-1 count and exit-4 count agree with the prose (D14, D14b, D14c exit 4) |
| `README.md` | Hook description at line 206-208 says "only when a staged path is under a migration, `k8s`, `docs/architecture` or is `KafkaTopics.java`" | Matches the hook |

Residual note: the pre-existing sentence at `DIAGRAM_CONVENTIONS.md:161` ("the workload set matches `k8s/base` and `k8s/data`") is under a dated "Proven in spike 007" heading and is immediately qualified. I judged it a historical record, not a false present-tense claim.

### Proofs re-run by the verifier (this session, not reused)

| Proof | Command | Result |
| --- | --- | --- |
| Drift check on the real tree | `node docs/architecture/sync.mjs check` | exit 0, `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads)` |
| Drill replay | `fish docs/architecture/drill.fish \| cmp - docs/architecture/drill-output.txt` | `cmp` exit 0, byte-identical |
| Hook self-test | `python3 scripts/pre-commit-likec4-selftest.py` | `16/16 passed`, exit 0 (confirmed with the exit code read on its own, not through a pipe) |
| Model export | `likec4 export json --skip-layout` | `production` nodes: `netcup.k3s.kube-system` and `netcup.k3s.kube-system.traefik` present; no deployment element id has an `edge` segment; namespace titles are exactly cert-manager, flux-system, kanban-data, kanban-nonprod, kanban-prod, kube-system, monitoring |
| Debt markers | `rg "TBD\|FIXME\|XXX\|TODO\|HACK"` over `sync.mjs`, `drill.fish`, the hook, the selftest, `DIAGRAM_CONVENTIONS.md` | no match |
| Scope | `git diff 0c6b615^ HEAD --stat -- k8s src docs/architecture/model/generated.c4` | empty: the phase edited no manifest, no source, and left `generated.c4` untouched |

The replay and the export wrote only to scratch locations; the working tree outside this report is unchanged by me.

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Namespace check goes red on the real `edge` defect, then green after Traefik moves to `kube-system`; the `production` view has `kube-system` and no `edge` | VERIFIED | `01-01-edge-red.txt`: exit=1 with `deployment node netcup.k3s.edge names namespace 'edge', which no manifest under k8s/ defines`, recorded with the rule present and the model unfixed. `01-01-edge-green.txt`: exit=0. My own export (above) confirms the node set. |
| 2 | Every manifest-defined namespace is a model node (seven); placement reads `k8s/platform`; on a scratch copy a deleted namespace node and a moved instance each exit 1; unmodified exits 0 | VERIFIED | `deployment.c4` lines 9-34 hold seven namespace nodes. Replayed `drill-output.txt`: baseline exit 0; D10 exit 1 (cert-manager); D10b exit 1; D11 exit 1; D15 exit 1; D16 exit 1; D16b exit 0. |
| 3 | A misspelled metadata key exits 1 naming key and element; unmodified exits 0 | VERIFIED | D12 exit 1, D12b exit 1 (replayed byte-identical), baseline exit 0. |
| 4 | A deployment instance with no relationship exits 1 naming the instance; adding a relationship returns 0 | VERIFIED | D13 exit 1, D13b exit 0. |
| 5 | Each rule has a numbered drill recorded not-caught-before and caught-after; `fresh()` copies every directory the readers read; a missing directory exits non-zero; hook triggers on `k8s/platform`; selftest passes with more than 15 cases | VERIFIED | `01-drills-before.txt`: D10, D10b, D11, D15, D16, D16b, D12, D12b, D13, D13b, D14, D14b, D14c all `exit=0` (not caught before). After: D14, D14b, D14c exit 4. `K8S_DIRS` (sync.mjs:249) lists six trees, checked with `existsSync` at sync.mjs:498. Hook pathspec includes `k8s` (pre-commit:208-210). Selftest red `15/16` (test_16 fails: "the commit succeeded; expected a refusal"), green `16/16`, and my re-run `16/16`. |

### Plan-level truths

| Plan | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 01-01 | Rule in tree, model unfixed: check exits 1 naming `netcup.k3s.edge` | VERIFIED | `01-01-edge-red.txt` |
| 01-01 | After fix, real repo exits 0 with unchanged tally | VERIFIED | Re-run, same tally |
| 01-01 | Export lists `kube-system` in production, no `.edge`, seven namespace nodes | VERIFIED | My export |
| 01-01 | Missing node and phantom node each give a named DRIFT line | VERIFIED | D10, D10b |
| 01-01 | Missing tooling exits 3 with the install command; selftest still passes | VERIFIED | Selftest passes 16/16 including the missing-tooling cases; `likec4.fish` prints the install command and exits 3 |
| 01-01 | `workloads()` through the YAML parser reports the same 4 workloads; `generated.c4` byte-identical; baseline-through-D9 unchanged | VERIFIED | Tally still says 4 workloads; `generated.c4` has no diff since `0c6b615^`; `drill-output.txt` diff is +61/-0 |
| 01-02 | D10, D10b, D11, D16, D16b, D15, D12, D12b, D13, D13b named lines and exit codes | VERIFIED | Replayed output byte-identical |
| 01-02 | Every drill from D10 on is `exit=0` in `01-drills-before.txt` | VERIFIED | 13 sections read, all exit=0 |
| 01-02 | Sections baseline through D9 unchanged | VERIFIED | 0 deleted lines |
| 01-03 | D14, D14b exit 4 naming the directory; D14c exit 4 naming file and parser code | VERIFIED | `drill-output.txt` |
| 01-03 | Selftest test_16 refuses a `k8s/platform` typo; pre-phase hook lets it through | VERIFIED | red/green files and my re-run |
| 01-03 | Selftest passes 16/16 | VERIFIED | Re-run |
| 01-03 | No sentence lists `k8s/platform` as outside the trigger, `k8s/base`/`k8s/data` as the only sources, or the metadata-key check as not built | VERIFIED | Sweep table above; line 313 fixed |
| 01-03 | Checks section states the three placement limits | VERIFIED | `DIAGRAM_CONVENTIONS.md:178-193` |

**Score:** 25/25 (5 roadmap criteria, 20 plan-level truths). 0 behavior-unverified: every rule truth is backed by a drill whose red and green runs I replayed byte-for-byte.

### Required Artifacts

| Artifact | Status | Details |
| --- | --- | --- |
| `docs/architecture/sync.mjs` | VERIFIED | `K8S_DIRS` guard, namespace/placement/metadata/membership rules, wired from `check()` and exercised by the replay |
| `docs/architecture/model/deployment.c4` | VERIFIED | Traefik in `kube-system`; three empty namespace nodes |
| `docs/architecture/package.json`, `pnpm-lock.yaml` | VERIFIED | `yaml` pinned (lockfile reviewed in the first verification; unchanged since) |
| `docs/architecture/drill.fish`, `drill-output.txt` | VERIFIED | Replay reproduces the recording |
| `.githooks/pre-commit` | VERIFIED | Trigger covers `k8s` |
| `scripts/pre-commit-likec4-selftest.py` | VERIFIED | Case 16 present, 16/16 |
| `docs/DIAGRAM_CONVENTIONS.md` | VERIFIED | Inventory matches what is built; the stale sentence is gone |

### Key Link Verification

| From | To | Status | Details |
| --- | --- | --- | --- |
| `check()` | `yaml` package | WIRED | Dynamic import; selftest covers the missing-tooling path |
| namespace rule | `deployment.c4` namespace nodes | WIRED | D10, D10b |
| hook trigger | `k8s/platform/traefik/helmchartconfig.yaml` | WIRED | selftest test_16 red then green |
| `check()` exit 4 | hook not-run-to-completion branch | WIRED | pre-commit handles any status other than 0 and 1 |
| `drill.fish run()` | `out/sync-before.mjs` | WIRED | `01-drills-before.txt` produced through the override |

### Requirements Coverage

| Requirement | Source Plans | Status | Evidence |
| --- | --- | --- | --- |
| PHYS-01 | 01-01 | SATISFIED | Seven namespace nodes, no `edge`, Traefik under `kube-system` |
| GATE-02 | 01-02 | SATISFIED | D12, D12b |
| GATE-03 | 01-02 | SATISFIED | D13, D13b |
| GATE-04 | 01-01, 01-02, 01-03 | SATISFIED | D10, D10b, D11, D15, D16, D16b, hook test_16 |

No orphaned requirements: REQUIREMENTS.md maps exactly these four IDs to Phase 1 and all four are marked Complete and claimed by a plan.

### Anti-Patterns Found

None. The debt-marker search is empty over every modified code and standard file.

### Review Findings (01-REVIEW.md, advisory, open)

Unchanged from the first verification: WR-01 to WR-06 are latent loopholes or wrong exit classes, none defeats a success criterion. WR-03, WR-04 and WR-06 should be closed or recorded before Phase 2 relies on the placement rule for new elements. Dispositions are in `01-REVIEW-DISPOSITION.md`. I did not re-review them.

### Human Verification Required

#### 1. Physical view renders without `edge`

**Test:** Run `docs/architecture/likec4.fish start` and open the `production` deployment view in a browser.
**Expected:** Traefik sits inside a kube-system node, and no node labelled edge appears. The empty flux-system, cert-manager and monitoring namespaces are not drawn in this view.
**Why human:** The export JSON proves the node set, not how the view renders and reads. I have no browser, so I am not claiming it.

### Gaps Summary

No gaps. The documentation gap is closed in the file. Status is `human_needed` only because the production-view render check cannot be done by the verifier.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
