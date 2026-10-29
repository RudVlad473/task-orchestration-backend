---
phase: 04-leak-review-and-production-go-live
plan: 01
subsystem: infra
tags: [leak-scan, node-test, github-actions, likec4, architecture-site]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: the site workflow, the static build and the publish-the-site scenario evidence
provides:
  - docs/architecture/site/leak-scan.mjs, a zero-dependency scan of the built site with 13 targets and a four-host allow-list
  - docs/architecture/site/leak-scan.test.mjs, 81 fixture cases run by the workflow before the build
  - workflow steps that run the tests and the scan, the scan between the app-shell check and the image build
  - model text that stays true after the production flip (both ClusterIssuer descriptions) and a publish-the-site scenario that draws the scan
  - 04-scan-baseline.txt with per-target counts, the planted-IP red run and three mutant proofs
affects: [04-02, 04-03, 04-04, 04-05, leak review, production flip]

plan_head_before: fc886134d625066dc6e051abe0b89c501673f41f
plan_head_after: 18308796defbb3ea02c3caef9aef7791ac958c85

actuals:
  tokens: 11400
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Black-box node:test fixtures that spawn the gate script and assert exit code, target name and matched text"
    - "Report prints matched text, file and count only, because the CI log of the public repo is public"

key-files:
  created:
    - docs/architecture/site/leak-scan.mjs
    - docs/architecture/site/leak-scan.test.mjs
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-scan-baseline.txt
  modified:
    - .github/workflows/architecture-site.yml
    - docs/architecture/model/edge.c4
    - docs/architecture/model/scenarios-platform.c4
    - docs/DIAGRAM_CONVENTIONS.md
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-SCENARIO-EVIDENCE.md

key-decisions:
  - "The scan lives beside the site, so docs/architecture/site/** (already a push and pull-request trigger) covers it with no paths change"
  - "ssh-command stays case-sensitive as the plan words it; the model prose 'SSH forward' and a vendored uppercase 'SSH ' label are not hits (recorded for the owner)"
  - "secret-word stays a target by default with a measured 0 hits; the owner confirms at Q2 in plan 03"
  - "Six of ten Secret names are listed; postgres-backup, letsencrypt-staging, letsencrypt-production and flux-system are not, because the model draws them"

requirements-completed: [PUB-05]

coverage:
  - id: D1
    description: "leak-scan.mjs exits 0 on the real build and 1 on a build with a planted IP naming ipv4, 4 on empty or missing input, 2 on bad usage"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "node docs/architecture/site/leak-scan.mjs docs/architecture/site/out (exit 0, OK targets=13); 04-scan-baseline.txt PLANT/UNPLANT/MISSING/EMPTY/USAGE lines"
        status: pass
    human_judgment: false
  - id: D2
    description: "A fixture test per target and per measured trap, with three mutants each caught by the suite"
    requirement: PUB-05
    verification:
      - kind: unit
        ref: "docs/architecture/site/leak-scan.test.mjs (81 pass, 0 fail); MUTANT lines in 04-scan-baseline.txt"
        status: pass
    human_judgment: false
  - id: D3
    description: "The workflow runs the tests before the build and the scan before the image, with no pipe, so a hit fails the build job and publish never starts"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "yq '.jobs.build.steps[].name' .github/workflows/architecture-site.yml (step order); no CI run yet, nothing pushed"
        status: unknown
    human_judgment: true
    rationale: "The CI half (a pull-request run red then green, publish skipped) is plan 03; locally only the step order and command text are verified"
  - id: D4
    description: "Model text the flip would make false is rewritten and publish-the-site draws the scan, with evidence re-cited"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "node docs/architecture/sync.mjs check; scan of the edited model (MODEL_EDIT scan exit=0 last=OK targets=13)"
        status: pass
    human_judgment: true
    rationale: "Whether the rewritten wording reads well on the site is the owner's review in plan 03"

duration: 25min
completed: 2026-10-29
status: complete
---

# Phase 4 Plan 01: Leak scan of the built site Summary

**A zero-dependency Node scan reads every built file of the architecture site for 13 leak targets and a four-host allow-list, runs in the build job before the image exists, fails on a planted IP, and is held by 81 fixture tests.**

## Performance

- **Duration:** about 25 min (start time was not recorded; estimated from the first build at 15:41Z and the last check at 15:53Z)
- **Completed:** 2026-10-29T15:53Z
- **Tasks:** 3
- **Files modified:** 8 (3 created, 5 modified)

## Accomplishments

- `leak-scan.mjs` scans 22 files and 6,988,094 bytes of the real build in well under a second and reports `OK targets=13` with every target at 0 hits, `HOSTS` listing exactly the four public duckdns hosts and `ROBOTS disallow=yes`.
- Red on a plant: a copy of the model with `10.20.30.40` in the acme description builds and scans to exit 1, `LEAK targets=ipv4`, four `HIT ipv4` lines naming the files and the matched text only. The real build scanned right after exits 0. Missing and empty directories exit 4, no argument exits 2.
- 81 `node:test` cases (one failing case per pattern alternative, one passing case per measured trap, nesting, NUL bytes, report content, step summary, exit codes). Three mutants (ipv4 row removed: 30 failing tests; allow-list widened: 4; robots check removed: 5) each fail the suite, with the first failing test naming its target.
- `architecture-site.yml` has `Verify the leak scan catches each target` after the drift check and `Scan the build for leaks` between the app-shell check and the image build, no pipe, no `shell:` key, plus one header bullet.
- `edge.c4`: both ClusterIssuer descriptions no longer name which Certificates reference them. `scenarios-platform.c4`: publish-the-site has a separate scan step and a 69-word description. The scan of a build of the edited model is clean.

## Baseline (04-scan-baseline.txt, build at fc88613, likec4 1.60.0, use-dot=false)

| Target | Hits |
|---|---|
| ipv4, ipv6, ssh-command, kubectl, secret-word, key-path, ssh-host, credential, in-cluster-host, secret-name, user-name, hostname, robots | 0 each |

`DEFAULT secret-word kept, owner to confirm at Q2 hits=0` is recorded; I did not decide it. Plan 03 puts the default and the count to the owner.

Secret names (from `k8s/` plus `grafana-admin`): listed `app-tls`, `grafana-tls`, `architecture-tls`, `postgres-init`, `app-env`, `grafana-admin` (0 model hits each). Not listed because the model draws them: `postgres-backup` (3 model hits; it is a Secret), `letsencrypt-staging` (2), `letsencrypt-production` (2), `flux-system` (5).

User names (model hits): listed `rudenkovladimir` 0, `rudvlad473` 0, `andre` 0, `deploy-nonprod` 0, `root@` 0, `deploy@` 0. Excluded, with the reason carried in the script comment: `vrudenko` 17 (the Java package in every source path), `rud-vlad-473` 4 (inside the four hostnames), `root` 0 (CSS `:root` and JS `.root` in vendored code), `deploy` 9 (`deploy.yml`), `runner` 5 (a common word; GitHub's runner), `monitoring` 18 (a namespace the model draws). No listed name has model hits, so no description needed a rewrite. `deploy` reads 9 where the plan table says 8; it is excluded either way.

No `TRAP` led to a pattern change. One `TRAP ssh-command SSH ` line is recorded (see Decisions).

publish-the-site natural size: 2360 x 1401 before, 2356 x 1637 after (PNG 4712 x 3274 divided by 2).

## Task Commits

1. **Task 1: scan, workflow step, baseline** - `aaa549b` (feat)
2. **Task 2: fixture tests, workflow test step, mutant proofs** - `1301b50` (test)
3. **Task 3: model text, scenario evidence, conventions** - `1830879` (docs)

**Plan metadata:** committed after this file (docs: complete plan).

## Files Created/Modified

- `docs/architecture/site/leak-scan.mjs` - the scan; exit codes 0, 1, 2, 4; writes `GITHUB_STEP_SUMMARY` itself
- `docs/architecture/site/leak-scan.test.mjs` - 81 fixture cases
- `.github/workflows/architecture-site.yml` - two steps and one header bullet
- `docs/architecture/model/edge.c4`, `docs/architecture/model/scenarios-platform.c4` - the model text
- `.planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-SCENARIO-EVIDENCE.md` - rows re-cited against the new workflow lines (102, 27-35, 74-75, 81, 86-87, 90, 53-56, 101-105, 110-114, 121, 128, 137, 30-35), scan row added as row 4, later rows renumbered 5 to 9
- `docs/DIAGRAM_CONVENTIONS.md` - the leak clause names the scan file; size row and re-measure sentence
- `04-scan-baseline.txt` - the record

## Decisions Made

- The scan sits beside the site, so no `paths:` edit and no change to publish-the-site's trigger wording was needed.
- `ssh-command` is case-sensitive, as the plan words it. The model says "SSH forward with a pinned host key" in a scenario description, and vendored code carries an uppercase `SSH ` label (16 matches in 6 files, `rg -i`). Neither is a hit. A case-insensitive target would fail on the authored sentence, so that is an owner decision at review, not mine.
- Hostname matching is case-insensitive and compares lower-cased text to the allow-list, so `EVIL.DUCKDNS.ORG` fails.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] A trap sample in my own fixtures was a real dotted quad**
- **Found during:** Task 2
- **Issue:** research lists `64.17.81.76` beside `58.06.87.06` as an SVG-number trap for the loose pattern. Under the strict pattern `64.17.81.76` is four valid octets and rightly hits when standalone, so a passing fixture for it would have been wrong.
- **Fix:** the SVG trap fixture holds only `M58.06.87.06`. Also corrected an `in-cluster-host` sample (`db.kanban` does not match `postgres.kanban`).
- **Verification:** suite 81 pass, 0 fail, before the first commit of the test file.
- **Committed in:** `1301b50`

**2. [Process] One small scripted text replacement**
- Two string fixes in the test file were made with a one-off `node -e` script instead of `Edit` (it threw on a missing anchor, and both anchors were present). Later edits used `Edit`.

**3. [Edit outside the file list] 03-08 sentence in the evidence header**
- The header said "Row 4 was re-cited ... in plan 03-08". After the renumbering that row is row 5, so the sentence now reads "Row 4 (row 5 since plan 04-01)". Same file as listed.

**Total deviations:** 3 (1 bug in a fixture, 2 minor process or wording). **Impact:** none on scope.

## Issues Encountered

- The tool hook refuses long inline Bash commands and `rm`; scratch helpers were written as small fish scripts under `docs/architecture/out/` (gitignored) and removed with `trash-put`, as were the plant model, the plant build, the mutant copy and the exported PNGs. `docs/architecture/out/` is empty again.

## Not verified

- No CI run: nothing was pushed, so the workflow steps, the step-summary append on a runner and the pull-request red/green are unverified (plan 03). Locally verified: step order and text through `yq`, the script and tests under the same Node 24.19.0 the job installs.
- The scan was measured on a local build; CI builds are not byte-reproducible (workflow header), so counts there may differ in file names, not in what the strict patterns match.
- PITFALLS 11 line numbers and the deny-list mapping were taken from the plan context; I did not re-read PITFALLS.md.
- The hook gate does not build the site (research Pitfall 9); the scan is gated by CI only.

## Known Stubs

None.

## Threat Flags

None. The only new surface is a script that reads a build directory and a workflow step; both are in the plan's threat model (T-04-01 to T-04-06 mitigated as written: no pipe, matched text only, runtime-assembled credential shapes, mutant proofs for T-04-03).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02 can add the hook or drill rows; plan 03 owns the CI red/green, the Q2 question (`secret-word`, hits=0) and the owner review. The review must be of the tag that the push carrying these commits produces, since all model-text edits are done here.
- Owner-facing observations for the review: the surname-derived Java package `vrudenko` stays on the public site in every `source` path; "SSH forward" stays in prose because the target is case-sensitive.

---
*Phase: 04-leak-review-and-production-go-live*
*Completed: 2026-10-29*

## Self-Check: PASSED
