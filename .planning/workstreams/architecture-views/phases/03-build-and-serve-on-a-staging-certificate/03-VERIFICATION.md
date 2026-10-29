---
phase: 03-build-and-serve-on-a-staging-certificate
verified: 2026-10-29T00:00:00Z
status: human_needed
score: 5/6 must-haves verified
covered_files:
  - .github/workflows/architecture-site.yml
  - .github/workflows/backup-image.yml
  - .github/workflows/deploy.yml
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-01-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-01-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-02-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-02-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-03-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-03-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-04-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-04-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-05-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-06-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-06-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-07-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-07-SUMMARY.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-08-PLAN.md
  - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-08-SUMMARY.md
  - docs/architecture/model/delivery.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/model/views.c4
covered_digest: "v3:sha256:e14b88f2052c0911ed1abaafe0d8a16d3aa31bdafbda9b9aad372d71cc976b46"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 5/6
  gaps_closed:
    - "G-03-2 (WR-05): the published model no longer says the publish job is the only job in the production environment, no longer counts the workflows, and the step 4 evidence row cites sources for each clause"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Decide on WR-01 of the fresh 03-REVIEW.md. docs/architecture/model/views.c4:81, the description of the `delivery` deployment view, still reads 'A push to main starts the workflow on a GitHub-hosted runner, which pushes the app and site images to Docker Hub.' Either fix it now (a quick task: name the workflows and the backup image, as the review suggests) or leave it for Phase 3.1, the model description pass."
    expected: "The delivery view's caption agrees with its own edge labels (delivery.c4:42 names three workflows, delivery.c4:43 names the app, backup and site images), or the user knowingly defers it."
    why_human: "Which phase owns the fix is the user's call. It is a Phase 2 view description, outside G-03-2's three listed fixes, and no gate reads prose. Phase 3.1's goal (descriptions complete and consistent) covers it in principle, but no 3.1 plan exists yet."
  - test: "Push the 14 local commits ahead of origin/main when ready, then confirm the staging site serves the new sentences."
    expected: "A push touching docs/architecture/model starts architecture-site.yml (not deploy.yml), Flux rolls the new tag, and the served bundle holds 'the only job of this workflow in the production environment' and not 'the only job in the production environment'."
    why_human: "Pushing is an outward action only the user authorizes. Until then the live host serves the old sentences (the fixed text exists in the repo and in a fresh local build, not on the host)."
---

# Phase 3: Build and Serve on a Staging Certificate Verification Report

**Phase Goal:** Every model change on `main` rebuilds the static site. Flux serves the static build at the architecture hostname over HTTPS on a Let's Encrypt staging certificate. The publish path is drawn in the model as it was built, not as planned.
**Verified:** 2026-10-29T00:00:00Z
**Status:** human_needed
**Re-verification:** Yes, after gap closure plan 03-08 (G-03-2). The previous report had no `gaps:` block; G-03-2 came from UAT test 2. I verified G-03-2 in full and re-checked the rest as regressions.

G-03-2 is closed in the repository. No must-have failed. One review warning (WR-01) is open and is a user decision, and the fix is not on the live host until a push.

## Goal Achievement

### G-03-2 (the gap this run closes), verified against `.github/workflows`

UAT test 2 expected three things. All three hold on today's tree.

| UAT expectation | Evidence | Status |
| --------------- | -------- | ------ |
| `scenarios-platform.c4:38` no longer says the publish job is "the only job in the production environment" | Line 38 now reads "the publish job, the only job of this workflow in the production environment, whose registry token deploy.yml and backup-image.yml also use...". Source: `environment:` lines are `architecture-site.yml:96` (one only), `deploy.yml:94,161,307` (production) and `:236,380` (staging), `backup-image.yml:36`. The build job at `architecture-site.yml:50-53` has none. The claim is now scoped to its own workflow and is true. `diff` of `rg -l "environment: production"` (minus the site workflow) against the `.yml` names in step 4: exit 0. | VERIFIED |
| `delivery.c4:7` no longer says "Two workflows" | Line 7 now begins "The workflows that push images to Docker Hub are deploy.yml ...; backup-image.yml ...; and architecture-site.yml ...", with no count. `rg -l "push: true"` gives exactly `architecture-site.yml:128`, `deploy.yml:135`, `backup-image.yml:60`, the same three names as in the description (exit 0) and the repo-to-ci title at `:42` (exit 0). `rg -l "build-push-action\|docker push"` returns the same three files, so no image-pushing workflow is missing. Lines 14, 42 and 43 agree with line 7. | VERIFIED |
| Step 4 evidence row cites a source that supports it | Re-ran plan 03-08's row 4 gate on today's tree: `ROW4 OK`, exit 0 (step cell equals the model's step, 9 cites and 9 quote groups, every quote equals its cited line, and the clauses cite `build:`, `environment: production` and a `secrets.DOCKERHUB_TOKEN` line from each of the three workflows). The section still has 8 rows (03-07's check, rows unchanged in count). | VERIFIED |

The token claim holds too: `secrets.DOCKERHUB_TOKEN` is read only by `architecture-site.yml:112`, `deploy.yml:107,319,391` and `backup-image.yml:46` (diff against step 4's workflow list: exit 0). The review notes that `deploy.yml:391` sits under `environment: staging` and uses the staging copy of the token, so "also use" is loose for that one job. I agree and do not treat it as false.

All six retired phrases are absent from `docs/architecture/model` (`rg -ni -F`, exit 1). I rebuilt the site with the workflow's own command (`likec4 build --use-dot=false`, `docs/architecture/site/out` absent before, trashed after) and every changed sentence is in the built bundle and no old wording is:

```
BUILT L38 new_files=3 old_files=0
BUILT L33 new_files=2 old_files=0
BUILT L7 new_files=4 old_files=0
BUILT L14 new_files=4 old_files=0
BUILT L42 new_files=5 old_files=0
BUILT L43 new_files=6 old_files=0
```

`node docs/architecture/sync.mjs check`: `OK (62 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 18 workloads)`, exit 0.

The commits touch only what the plan said. `git diff 6386b5f..HEAD -- docs/ scripts/ k8s/ .github/` shows two files: `delivery.c4` (4 lines changed) and `scenarios-platform.c4` (2 lines changed). No code, manifest or workflow moved, so the earlier gates have no reason to change.

### Observable Truths

ROADMAP criterion 1 stays split into 1a and 1b so the pending clause is scored on its own.

| #  | Truth | Status | Evidence |
| -- | ----- | ------ | -------- |
| 1a | A model-only push runs the site workflow: `sync.mjs check` before `likec4 build`, then a push of `main-<run>-<sha7>` to the site's own repo; drifted model red, fixed model green; PR build has no secrets; no site run for a Flux bump | VERIFIED (regression check) | No change to `architecture-site.yml` since the previous report (`git diff` empty for `.github/`). Workflow still has one `environment:` line (`:96`, publish only) and `push: true` only at `:128`. Previous report's run ids (40766223979 red, 40774467920 green) and the live tag `main-5-a26694f` are unchanged; the live Deployment tag was read over ssh then, not re-read now. |
| 1b | No site run for a push that touches only `src/` | UNCERTAIN, accepted: UAT test 1 recorded `pass` for "accept the same-mechanism evidence with the literal case pending" | No src-only push exists on origin/main, so the literal case has no observation. The `paths:` filter (`architecture-site.yml:24-32`) is the mechanism, observed excluding a k8s-only push and three Flux bumps. The user accepted this in UAT; I do not count it as verified because nothing observed it. No override was written. |
| 2  | Flux rolls the tag into the site namespace; `(STAGING)` issuer; Certificate `Ready=True` on `letsencrypt-staging`; `curl -k /` is 200 with `likec4-root`; other hostnames unchanged | VERIFIED (re-probed today) | `openssl s_client`: `issuer=C=US, O=Let's Encrypt, CN=(STAGING) Dastardly Durum YR1`. `curl -sk /`: 200, `likec4-root` count 1. Certificate Ready and the other hostnames were read over ssh and curl in the previous run; I did not repeat those today. |
| 3  | Only static output reachable; dev-server gate red then green; deep link renders; missing asset recorded | VERIFIED (re-probed in part) | `curl -sk /assets/missing.js`: `404 text/html` today, as recorded. `verify-k8s-invariants.py --no-provisional` still runs I12 and exits 0. The I12 red/green files and the `/@fs` and deep-link checks are from the previous run and unchanged inputs. |
| 4  | `verify-k8s-invariants.py --no-provisional` green; MEASURED memory; I5 red then green; I10 shape; one owner under I11 | VERIFIED (re-ran) | `invariants OK -- 14 kustomization root(s) checked ...`, exit 0 today. No `k8s/` or `scripts/` file changed since `6386b5f`. |
| 5  | The model contains the site as built: namespace node, instance with a `traefik ->` edge, a publish-the-site scenario with cited steps; Phase 1 and 2 checks green and cover the site's workload | VERIFIED | The WR-05 clause that made it "VERIFIED (one clause false)" last time is fixed (G-03-2 above). `sync.mjs check` exit 0 with 18 workloads. The 8-step scenario, `deployment.c4`, `model.c4` and `03-SCENARIO-EVIDENCE.md` were not touched except row 4. One adjacent caption (WR-01) is still stale; see below. |

**Score:** 5/6 truths verified (1b accepted but unobserved, 0 present-but-behavior-unverified).

### Required Artifacts (this round)

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `docs/architecture/model/scenarios-platform.c4` | Line 33 and step 4 scoped to this workflow | VERIFIED | Line 33: "the publish job is the only job of this workflow that holds the registry token". Step 4 as above. Numstat 2/2. |
| `docs/architecture/model/delivery.c4` | `ci` description, `dockerhub` description and two edge titles name every image-pushing workflow and image | VERIFIED | Lines 7, 14, 42, 43 read as the plan specified. Numstat 4/4. Metadata blocks unchanged. |
| `03-SCENARIO-EVIDENCE.md` | Row 4 re-cited | VERIFIED | `ROW4 OK`, exit 0 against the committed tree. |
| `03-08-prose-proof.txt` | Source assertions recorded | VERIFIED as a record only | 16 lines, `TREE 74e50d4`, all diffs exit 0, `OLD-PHRASES model_lines=0`, `LEAKWORDS lines=0`. It is the executor's own report; I relied on my own re-runs above, which gave the same numbers. |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| Step 4 workflow names | `.github/workflows/*.yml` `environment: production` users | set diff | WIRED | exit 0 |
| Step 4 workflow names | `secrets.DOCKERHUB_TOKEN` users | set diff | WIRED | exit 0 |
| `delivery.c4:7` and `:42` workflow names | `.github/workflows/*.yml` `push: true` | set diff | WIRED | both exit 0 |
| Evidence row 4 | `architecture-site.yml`, `deploy.yml`, `backup-image.yml` lines | verbatim quotes | WIRED | `ROW4 OK` |

### Data-Flow Trace (Level 4)

Not applicable: static prose in a model that is built into a static site. The trace that matters is model text to built bundle, covered by the six BUILT lines above.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Drift check | `node docs/architecture/sync.mjs check` | OK, 18 workloads, exit 0 | PASS |
| Set diffs (4) | `diff <(rg -l ...) <(rg ... \| rg -o "[a-z-]+\.yml")` | exit 0 each | PASS |
| Retired phrases absent | `rg -ni -F` six phrases over `docs/architecture/model` | exit 1 (no match) | PASS |
| Row 4 gate | plan 03-08 Task 1 python gate | `ROW4 OK`, exit 0 | PASS |
| Built site holds new wording, not old | plan 03-08 Task 2 build gate | six lines above, exit 0, output dir trashed | PASS |
| Invariants | `python3 scripts/verify-k8s-invariants.py --no-provisional` | OK, exit 0 | PASS |
| Live TLS, marker, missing asset | `openssl`, `curl -sk` | STAGING issuer, 200, marker 1, 404 text/html | PASS |

Not re-run: the drill replay (`fish docs/architecture/drill.fish`), selftests, `verify-comments.py`, `gitleaks`, Java `fastTest` and `spotlessCheck`. The two commits that matter changed two `.c4` files; `rg` over the 03-08 plan found no drill row, selftest or gate that reads those sentences. I did not re-confirm that by running the drills.

### Probe Execution

Step 7c: no `probe-*.sh` is declared by this phase's plans. SKIPPED.

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
| ----------- | ------------ | ----------- | ------ | -------- |
| PUB-01 | 03-01, 03-02, 03-06, 03-07 | CI builds the static site into an image pushed to its own Docker Hub repo, rebuilt only on source-path changes | SATISFIED, src-only observation pending (truth 1b) | Truth 1a. REQUIREMENTS.md ticks it. |
| PUB-02 | 03-01, 03-03, 03-04, 03-06 | Manifests and Flux wiring serve the image at the DuckDNS architecture hostname; only static output exposed | SATISFIED | Truths 2, 3, 4. |
| SCEN-08 | 03-04, 03-05, 03-07, 03-08 | Publish-the-site scenario | SATISFIED | Truth 5. The WR-05 false clause is gone. |

All three IDs appear in PLAN `requirements:` frontmatter (`rg` over the eight plans: PUB-01 in 03-01, -02, -06, -07; PUB-02 in 03-01, -03, -04, -06; SCEN-08 in 03-04, -05, -07, -08) and in REQUIREMENTS.md lines 31, 48, 49 and its traceability table (rows 94, 100, 101: Phase 3, Complete). No requirement maps to Phase 3 without a plan, so there is no orphan.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `docs/architecture/model/views.c4` | 81 | `delivery` view description says "starts the workflow ... which pushes the app and site images" while the same view's edges (delivery.c4:42-43) now name three workflows and the backup image (review WR-01, confirmed by reading both files) | Warning | Published caption and edge labels disagree. It is incomplete more than false (the app and site images are pushed), though "the workflow" in the singular no longer matches the view's own `repo -> ci` label. Not in G-03-2's three listed fixes; the plan's absence gate searched the exact retired string "push the app and site images", which cannot match "pushes the app and site images". |
| `docs/architecture/model/delivery.c4` | 7, 14, 43 | IN-01, IN-02, IN-03 and `ci` `source` naming only `deploy.yml` (review info items) | Info | Trigger clauses in line 7 are partial (backup-image.yml also runs on workflow edits and manual dispatch); the backup image has no consumer edge in the delivery view; the repo-to-ci list reads as exhaustive. None is false in the WR-05 sense. |
| `docs/architecture/model/scenarios-platform.c4` | 38 | IN-04: "whose registry token ... also use" is ambiguous and omits the staging copy of the token | Info | Wording, not truth. |
| `scripts/verify-k8s-invariants.py` | 128 | I12 regex narrower than its purpose (carried from the earlier review, WR-01 there) | Warning (carried, not re-evaluated) | Unchanged by this round. |

No `TBD`, `FIXME` or `XXX` marker in the files changed by 03-08 (`rg` over the two model files and the plan outputs: none).

### 03-REVIEW.md (gap-closure review)

0 critical, 1 warning, 4 info. I did not take it on trust: I re-derived its WR-05 conclusions from the workflow files (environment, token and `push: true` sets) and confirmed WR-01 by reading `views.c4:81` and `delivery.c4:42-43`. Its statement that WR-05 is closed for the files in scope holds. I did not independently verify its claim that `deploy.yml` pushes both the production and nonprod repositories (`:135-139`) or its reading of `k8s/data/postgres/backup-cronjob.yaml:58` for IN-02; they bear on info items only.

### Could not verify

- The live host's current served text. It cannot hold the new sentences: no push has happened (14 commits ahead of origin/main, `git rev-list --count origin/main..HEAD`), and the previous report's live tag is `main-5-a26694f`. I did not fetch the served bundle to confirm the old sentences are still there.
- The environment branch policy fencing the registry token, the pinned action SHAs against their tags, and the token's scope. Carried from the previous report; nothing in 03-08 touches them.
- Whether the drill harness or any selftest reads the six edited sentences (judged no from the plan's `rg`, not re-run).

### Human Verification Required

#### 1. Decide on WR-01 (`views.c4:81`)

**Test:** Read `docs/architecture/model/views.c4:81` beside `delivery.c4:42-43`. Fix it now or defer it to Phase 3.1.
**Expected:** The caption names the workflows and the backup image as the edges do, or the deferral is deliberate.
**Why human:** Ownership of the fix (this phase, a quick task, or 3.1) is the user's decision. 3.1's goal covers description defects in principle, but it has no plan yet.

#### 2. Push when ready, then confirm the site

**Test:** Push the local commits to origin/main, wait for `architecture-site.yml` and the Flux bump, then `curl -sk` the served bundle for the new step 4 wording.
**Expected:** New sentences served, old ones gone. No `deploy.yml` run (the changed paths are under `docs/`, which its `paths-ignore` excludes).
**Why human:** Pushing is outward; the user decides when.

### Gaps Summary

No failed truth and no blocker. G-03-2 is closed: the three false or incomplete statements are fixed in the model, every workflow-name claim equals the workflow files by set diff, the evidence row passes its gate, and a fresh build carries the new wording and none of the old. The status is `human_needed` rather than `passed` because WR-01 is an open, unowned prose defect in a sibling caption that the user should place, because the live site still serves the old sentences until a push, and because the literal src-only observation (1b) remains absent although the user accepted the evidence in UAT.

---

_Verified: 2026-10-29T00:00:00Z_
_Verifier: Claude (gsd-verifier)_
