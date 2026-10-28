---
phase: 03-build-and-serve-on-a-staging-certificate
verified: 2026-10-28T22:25:00Z
status: human_needed
score: 5/6 must-haves verified
covered_files:
  - .github/workflows/architecture-site.yml
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
  - docs/DIAGRAM_CONVENTIONS.md
  - docs/INFRA_ARCHITECTURE.md
  - docs/INFRA_RUNBOOK.md
  - docs/architecture/drill-output.txt
  - docs/architecture/drills.tsv
  - docs/architecture/model/delivery.c4
  - docs/architecture/model/deployment.c4
  - docs/architecture/model/model.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/model/views.c4
  - docs/architecture/site/Dockerfile
  - docs/architecture/site/nginx.conf
  - k8s/base/architecture-site/architecture-site.yaml
  - k8s/base/architecture-site/kustomization.yaml
  - k8s/flux-system/apps-architecture.yaml
  - k8s/flux-system/image-automation.yaml
  - k8s/flux-system/kustomization.yaml
  - k8s/overlays/architecture/certificate.yaml
  - k8s/overlays/architecture/ingressroute.yaml
  - k8s/overlays/architecture/kustomization.yaml
  - k8s/platform/edge/clusterissuers.yaml
  - k8s/platform/namespaces.yaml
  - scripts/verify-k8s-invariants-selftest.py
  - scripts/verify-k8s-invariants.py
covered_digest: "v3:sha256:7f8fe8ce916c8d50d95f29e60772a256a08b6a2f8663d252ba8adc2c32367da2"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Decide how the src-only clause of ROADMAP criterion 1 is closed. Either accept the paths-filter evidence, or leave it pending until a real push that touches only src/ exists on origin/main, then run `gh run list --workflow=architecture-site.yml` and confirm no run for that SHA."
    expected: "No Architecture Site run for the src-only push (and no run for any Flux bump, already observed three times)."
    why_human: "DECISION Q4 1 chose passive observation. No src-only push exists on origin/main (checked: none of the 38 non-merge commits after 5d44dac touches only src/), so the literal clause has no observation. Pushing one is an outward action only the user can authorize."
  - test: "Decide on WR-05 of 03-REVIEW.md before Phase 4's review. Fix, or accept, two false statements in the published model: docs/architecture/model/scenarios-platform.c4:38 says the publish job is 'the only job in the production environment', and docs/architecture/model/delivery.c4:7 says GitHub Actions has 'Two workflows'."
    expected: "Each statement is true against .github/workflows/*.yml, or the user accepts it knowingly."
    why_human: "environment: production is also set at deploy.yml:94,161,307 and backup-image.yml:36, and backup-image.yml also pushes an image. The statements are false today, they are served live on the staging host, and no gate checks prose. Whether this blocks closing Phase 3 or is folded into Phase 4's leak review is the user's call."
---

# Phase 3: Build and Serve on a Staging Certificate Verification Report

**Phase Goal:** Every model change on `main` rebuilds the static site. Flux serves the static build at the architecture hostname over HTTPS on a Let's Encrypt staging certificate. The publish path is drawn in the model as it was built, not as planned.
**Verified:** 2026-10-28T22:25:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

The goal is achieved in the codebase and on the live host. Two items need a human decision: one clause of criterion 1 has no observation yet, and one review warning (WR-05) is a real factual defect in the published model.

## Goal Achievement

ROADMAP criterion 1 is split into 1a (everything observed) and 1b (the src-only clause), so the pending clause is scored on its own and cannot hide inside a pass.

### Observable Truths

| #  | Truth | Status | Evidence |
| -- | ----- | ------ | -------- |
| 1a | A model-only push runs the site workflow, which runs `sync.mjs check` before `likec4 build`, then pushes `main-<run>-<sha7>` to the site's own repo. A drifted model fails before the push (red), the fixed model pushes (green). The PR build holds no secret. No site run for a Flux bump. | VERIFIED | Workflow read: `architecture-site.yml:71-72` (check) precedes `:75` (build); publish job needs build, `if` excludes PRs, is the only job naming `environment: production`. `gh run view 40766223979`: build=failure at step "Verify the model matches the code", publish=skipped (red). Run 40766992838: build=success, publish=skipped (green PR, no push). Run 40774467920 (push a26694f): build and publish success. Docker Hub anonymous API lists tags `main-1-5d44dac`, `main-4-b1aa8c5`, `main-5-a26694f`. `gh run list`: Architecture Site has 5 runs total and none for Flux bumps 673238f, fe3466b, 6a82dde, whose Invariant Checks and Secret Scan runs do exist. |
| 1b | No site run for a push that touches only `src/`. | UNCERTAIN (PENDING by design) | `git log --no-merges --name-only 5d44dac..origin/main`: no commit touches only `src/`. The mechanism is the `paths:` filter (`architecture-site.yml:24-32`), the same one observed excluding the k8s-only push (03-03-solver.txt:36) and three Flux bumps. Not the literal case; reported as pending, not passed (DECISION Q4 1, 03-DECISIONS.txt:5). |
| 2  | Flux rolls the tag into the site namespace; `(STAGING)` issuer; Certificate `Ready=True` on `letsencrypt-staging`; `curl -k /` is 200 with `likec4-root`; other hostnames unchanged. | VERIFIED | Probed live today. `openssl s_client`: `issuer=C=US, O=Let's Encrypt, CN=(STAGING) Dastardly Durum YR1`. `curl -sk /`: 200, `text/html`, `likec4-root` count 1. `ssh netcup-prod k3s kubectl`: `certificate/architecture READY True ... letsencrypt-staging`; Deployment image `rudenkovladimir/kanban-board-backend-architecture:main-5-a26694f`, pod 1/1 0 restarts; ImagePolicy resolved `main-5-a26694f`; Kustomization `apps-architecture` Ready at `main@sha1:6a82dde`. Other hosts: prod `/api/healthz` 200, nonprod `/api/healthz` 200, monitoring `/` 302, as in the baseline (03-06-live.txt:6). HTTP on the new host answers 308 to https. |
| 3  | Only static output is reachable; `/@fs/etc/hostname` does not return the file; the dev-server gate is red on planted lines and green after; a cold deep link renders; the missing-asset response is recorded. | VERIFIED | `curl -sk /@fs/etc/hostname` returns the 816-byte app shell, byte-identical to `/` (`cmp` same), not the host's hostname. `Dockerfile`/`nginx.conf`/Deployment hold no `likec4 start`, `preview` or `--listen`; image is nginx-unprivileged by digest with `COPY out/`. Red/green: 03-01-i12-red.txt:17-20 (three `FAIL: I12`, exit=1, "GATE BEFORE I12 exit=0" with the same plants) and 03-01-i12-green.txt:2 (exit=0); `verify-k8s-invariants-selftest.py` re-run today: "I1-I12 each fire on an engineered violation". Cold deep link: I rendered `/view/publish-the-site/` in headless Chromium (cert errors ignored): status 200, 162 SVG nodes, no "does not exist" text, title "Scenario: a model change becomes the published architecture site". Missing asset: `curl -sk -D- /assets/missing.js` answers `404 text/html`, server nginx/1.30.6 (settles Open Question 1). |
| 4  | `verify-k8s-invariants.py --no-provisional` green; memory is MEASURED from a VPS ladder (I4); I5 red then green; route has the I10 shape; one owner under I11. | VERIFIED | Re-ran today: `invariants OK -- 14 kustomization root(s)`, exit 0; `verify-k8s-manifests.sh` all 14 roots valid. `architecture-site.yaml:56-65` carries `MEASURED 2026-10-28 on the VPS`; 03-03-ladder.txt has rungs 64/32/16Mi pass, 8Mi OOMKilled, ADOPTED 16Mi/32Mi. I5: 03-04-i5-red.txt exit=1 naming the missing setter marker, 03-04-i5-green.txt exit=0; overlay `kustomization.yaml:13` carries the `$imagepolicy` marker. I10: `architecture-http` match has `!PathPrefix('/.well-known/acme-challenge/')`; the gate checks it. I11 live: both IngressRoutes carry `kustomize.toolkit.fluxcd.io/name: apps-architecture`, read via ssh. CI: `Invariant Checks` success on a26694f and 6a82dde. |
| 5  | The model contains the site as built: a namespace node, the instance with a `traefik ->` edge, a publish-the-site scenario whose steps cite the workflow and manifest lines; Phase 1 and 2 checks are green and cover the site's workload. | VERIFIED (one clause false, see WR-05 below) | `deployment.c4:37-38` namespace `architecture` with `instanceOf archsite`; `model.c4:143` `traefik -[sync]-> archsite 'HTTP :8080'`. Fresh `likec4 export json`: `publish-the-site` is `#scenarios`, 5 nodes, 8 steps, all 8 titled; 14 scenario views, none above 5 participants; physical views `delivery`, `monitoring`, `production` with 0 `[...]` labels; export has `netcup.k3s.architecture.archsite`. `03-SCENARIO-EVIDENCE.md` has 8 rows for 8 steps; I spot-checked the cited lines of the workflow (71-72, 75, 81, 93, 96, 119, 128), `image-automation.yaml` (22-29, 69-74, 100-104), `apps-architecture.yaml:15` and `architecture-site.yaml:17-18`: all match the quotes. `sync.mjs check` exit 0 with "18 workloads"; drills D25-D27 each exit 1 (drill-output.txt:183-199), and `fish drill.fish | cmp - drill-output.txt` exits 0. |

**Score:** 5/6 truths verified (1b pending, 0 present-but-behavior-unverified).

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `.github/workflows/architecture-site.yml` | Two-job workflow, drift gate before build, token only in publish | VERIFIED | Read in full. No `pull_request_target`; build job has no `environment`/`secrets`; publish pins actions by SHA. |
| `docs/architecture/site/Dockerfile`, `nginx.conf` | Static image, extension rule, 404 on missing asset | VERIFIED | Live behaviour matches the config (404 on `/assets/missing.js`, shell on extensionless paths). |
| `k8s/base/architecture-site/architecture-site.yaml` | Hardened Deployment + Service, MEASURED memory | VERIFIED | readOnlyRootFilesystem, drop ALL, runAsNonRoot, no SA token, 16Mi/32Mi, Recreate. |
| `k8s/overlays/architecture/*` | Overlay with setter marker, staging Certificate, two IngressRoutes | VERIFIED | Live objects exist and are Ready. |
| `k8s/flux-system/apps-architecture.yaml`, `image-automation.yaml`, `kustomization.yaml` | Flux wiring, ImagePolicy, one owner | VERIFIED | Kustomization and ImagePolicy Ready live. |
| `docs/architecture/model/scenarios-platform.c4` | `dynamic view publish-the-site` | VERIFIED | Present in export; see WR-05 for a false clause in step 4. |
| `03-SCENARIO-EVIDENCE.md`, `03-07-gate.txt`, `03-07-model-only-push.txt` | Per-step citations, gate with PENDING line | VERIFIED | Gate ends `PHASE STATUS: criteria 1 to 5 mapped; pending: src-only push (criterion 1, DECISION Q4 1)`. |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `architecture-site.yml` publish | Docker Hub repo | `docker/build-push-action`, tag `main-RUN-SHA7` | WIRED | Three tags exist publicly. |
| ImagePolicy pattern | overlay setter marker | `^main-(?P<num>[0-9]+)-[a-f0-9]{7}$`, `path: ./k8s/overlays` | WIRED | Tag moved main-4 to main-5 on the live Deployment; bump commits exist. |
| `apps-architecture` Kustomization | `k8s/overlays/architecture` | `path: ./k8s/overlays/architecture` | WIRED | Ready live. |
| IngressRoute | Service `architecture-site:8080` | Host rule | WIRED | `curl -k /` returns the site. |
| Certificate `architecture` | `letsencrypt-staging` ClusterIssuer (ClusterIP solver) | issuerRef | WIRED | Ready True, notAfter 2026-12-31. |
| Model `archsite` | k8s Deployment | `metadata workload` claim read by `sync.mjs` | WIRED | D25 breaks it and `check` exits 1. |

### Data-Flow Trace (Level 4)

Not applicable: static content, no dynamic data variables. The built site is the data; its bytes reach the browser unmodified (live `/` marker, 162 nodes on the deep link).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Drift check green | `node docs/architecture/sync.mjs check` | OK, 18 workloads, exit 0 | PASS |
| Drill replay matches record | `fish docs/architecture/drill.fish \| cmp - docs/architecture/drill-output.txt` | exit 0 | PASS |
| Invariants | `python3 scripts/verify-k8s-invariants.py --no-provisional` | OK, exit 0 | PASS |
| Invariants selftest | `python3 scripts/verify-k8s-invariants-selftest.py` | "I1-I12 each fire", pass | PASS |
| Hook selftest | `python3 scripts/pre-commit-likec4-selftest.py` | 16/16 passed | PASS |
| Manifests render | `bash scripts/verify-k8s-manifests.sh` | all 14 roots valid, exit 0 | PASS |
| Comment lint | `python3 scripts/verify-comments.py check` | OK, 340 files, exit 0 | PASS |
| Instruction budget | `python3 scripts/verify-instruction-budget.py` | OK, 4886 of 5100 bytes | PASS |
| Secrets in the `--no-verify` range | `gitleaks git --log-opts=5d44dac..HEAD -c .gitleaks.toml` | 22 commits scanned, no leaks | PASS |
| Live TLS, marker, `/@fs`, missing asset, deep link | curl, openssl, headless Chromium | see truths 2 and 3 | PASS |
| Cluster state | `ssh netcup-prod k3s kubectl get` (read-only) | Certificate, Deployment, ImagePolicy, Kustomizations Ready; 38 containers, 11 namespaces; Traefik restarts 2 | PASS |

Exit codes for the first three Python/node rows were read after the command, not through a pipe, except the two selftests, where the printed success line was read instead (the pipe hid the exit code). The scripts print their own failure text, and none appeared.

### Probe Execution

Step 7c: no `probe-*.sh` is declared by this phase's plans. SKIPPED (the phase uses named gates and live probes, covered above).

### The `--no-verify` commits

The executor ran the hook's checks by hand for docs-only commits (03-05's drills commits among them). I re-ran the same checks on today's tree: `sync.mjs check` exit 0, `verify-comments.py check` OK, `pre-commit-likec4-selftest.py` 16/16, drill replay identical to the committed `drill-output.txt`, instruction budget OK, gitleaks clean over `5d44dac..HEAD`. `drills.tsv` rows D25 to D27 are sed rows as 03-05 planned, and each drill exits 1 with a DRIFT or placement line naming the break. Nothing the skipped hook would have refused is present. spotlessCheck and `fastTest` are Java-only and `git diff --name-only 5d44dac HEAD` lists no `src/` path (not re-run).

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
| ----------- | ------------ | ----------- | ------ | -------- |
| PUB-01 | 03-01, 03-02, 03-06, 03-07 | CI builds the static site into an image pushed to its own Docker Hub repo, rebuilt only on source-path changes | SATISFIED, with the src-only observation pending | Truth 1a. The "only on source-path changes" half rests on the `paths:` filter and four excluded pushes (k8s-only, three Flux bumps) plus the flush push; no literal src-only push exists (truth 1b). |
| PUB-02 | 03-01, 03-03, 03-04, 03-06 | k8s manifests and Flux wiring serve the image at the DuckDNS architecture hostname; only static output exposed, never `likec4 start` | SATISFIED (confirmed, not contested) | Truths 2, 3, 4: the executor's tick holds. Live host answers 200 with the marker on a staging certificate, the Deployment runs the Flux-selected tag, and `/@fs` returns the shell. |
| SCEN-08 | 03-04, 03-05, 03-07 | Publish-the-site scenario | SATISFIED | Truth 5. Caveat WR-05: step 4 contains a false clause. |

All three IDs in the phase brief appear in at least one PLAN's `requirements:` frontmatter and in REQUIREMENTS.md. No orphaned requirement: REQUIREMENTS.md maps only PUB-01, PUB-02 and SCEN-08 to Phase 3 (PUB-03 to PUB-06 are Phase 4 and stay unticked).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `docs/architecture/model/scenarios-platform.c4` | 38 | Published statement "the only job in the production environment" is false (`deploy.yml:94,161,307`, `backup-image.yml:36`) | Warning | Public model overstates the token boundary; the evidence row cites only `architecture-site.yml:96`, which cannot prove "only". |
| `docs/architecture/model/delivery.c4` | 7 | "Two workflows" while `backup-image.yml` also builds and pushes an image | Warning | Same class: prose no gate checks. |
| `scripts/verify-k8s-invariants.py` | 128 | I12 regex needs `likec4<whitespace>start`; `npx likec4@1.60.0 start`, line continuations and `pnpm run dev` pass unseen (WR-01, confirmed by reading the regex) | Warning | The criterion's literal red/green holds; the gate is narrower than its purpose. Nothing exposed today. |
| `k8s/platform/edge/clusterissuers.yaml` | 21-24, 36-39 | `serviceType: ClusterIP` on both shared issuers has no regression guard (WR-03) | Warning | The production issuer's stanza is unexercised live; a silent revert reopens a NodePort. |
| `docs/architecture/site/nginx.conf` | whole | No security headers, `server_tokens` on (WR-04) | Warning | Not a Phase 3 criterion; Roadmap lists CSP/headers as an unowned recommendation (Open Question 8). |

No `TBD`, `FIXME` or `XXX` marker in any phase-touched file (rg over the workflow, site, k8s, model and gate script: none).

### 03-REVIEW.md

0 critical, 6 warnings, 6 info. Read in full. Judgment against the must-haves:

- WR-05 touches truth 5 and PUB/SCEN-08 fidelity ("as built"). I confirmed both statements are false against `.github/workflows/`. Raised as human item 2.
- WR-01 does not contradict criterion 3, whose wording is a gate that fails on planted `likec4 start`, `preview` or `--listen` lines and passes after. It does mean the gate is weaker than the claim "a gate refuses a dev server". Advisory.
- WR-02, WR-03, WR-04, WR-06 and the info items have no must-have behind them. IN-06 (the site is public while the leak review is pending) is already the Phase 4 ordering and the user's accepted default (ROADMAP Open Question 4).

### Findings the executors flagged, as I weighed them

- 184 SVG nodes against the plan's 38: the criterion is "renders the view", and my own render of a different view (162 nodes, correct title, no error text) agrees that the view renders. The 184 versus 38 difference is uninvestigated and unexplained; the build differs (`--use-dot=false` in CI) but I did not test that as the cause.
- Missing `/assets` file answers `404 text/html`: confirmed live. This already satisfies the Phase 4 wording ("does not answer 200 with text/html").
- crt.sh had no entry 8 minutes after issuance: not re-queried; the gate makes no CT claim, and I make none.

### Could not verify

- Whether the `production` environment's branch policy still fences `DOCKERHUB_TOKEN` (the 03-02 probe run 40767150747 is the evidence; I did not re-read the environment settings).
- Whether the pinned action SHAs at `architecture-site.yml:99,102,108,123` match their stated tags (WR review also could not).
- The `DOCKERHUB_TOKEN` scope (all repositories or one).
- Why `kanban-data` shows 4 containers against 1 in the earlier INFRA_ARCHITECTURE snapshot (the doc itself says it was not investigated).
- Not re-run: spotlessCheck and `fastTest` (no Java under `src/` changed in `5d44dac..HEAD`).

### Human Verification Required

#### 1. Close or keep pending the src-only clause

**Test:** Choose between accepting the paths-filter evidence or leaving the clause pending until a real src-only push exists, then check `gh run list --workflow=architecture-site.yml` for that SHA.
**Expected:** No Architecture Site run.
**Why human:** No such push exists, and making one is an outward action (DECISION Q4 1 chose passive observation).

#### 2. Fix or accept WR-05

**Test:** Compare `scenarios-platform.c4:38` and `delivery.c4:7` with `rg -n "environment:" .github/workflows`.
**Expected:** Both statements true, or knowingly accepted.
**Why human:** The model is served publicly now, and no drift check reads prose.

### Gaps Summary

No failed truth and no blocker. The phase delivers its goal: the model-only push runs the workflow after the drift check and publishes a tag, Flux rolled that tag, the live host serves static output on a staging certificate, the invariant gates and I12 and I5 reds are on record and re-run green, and the site is in the model with a cited scenario. The status is `human_needed` rather than `passed` because the src-only clause of criterion 1 was never observed and is reported as pending, and because the published model carries two statements that are false today.

---

_Verified: 2026-10-28T22:25:00Z_
_Verifier: Claude (gsd-verifier)_
