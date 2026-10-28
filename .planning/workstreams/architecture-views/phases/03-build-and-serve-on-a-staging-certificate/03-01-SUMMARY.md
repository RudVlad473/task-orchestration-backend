---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 01
subsystem: infra
tags: [github-actions, nginx, likec4, docker, invariants, ci]

requires:
  - phase: 02-architecture-views
    provides: docs/architecture LikeC4 model, sync.mjs drift check and the pnpm lockfile the build installs from
provides:
  - .github/workflows/architecture-site.yml with a secret-free build job and a publish job that alone names the production environment
  - docs/architecture/site Dockerfile (nginx-unprivileged by digest) and the measured nginx.conf
  - invariant I12 in scripts/verify-k8s-invariants.py with selftest cases, red then green on planted lines
affects: [03-02, 03-03, 03-04, 03-05, 03-06, 03-07]

actuals:
  tokens: 6745
  tasks: 2
  commits: 2
plan_head_before: abd0265c811edd358c628b863f19fa505d3f1884
plan_head_after: 76c843a1bfd074ca308a29b76a9fceea550bbb07

tech-stack:
  added: []
  patterns:
    - "two-job workflow: build holds no secret and uploads the static output, publish pushes those same bytes"
    - "source-text invariant over three file kinds (YAML, Dockerfile, workflow) as a pure function plus selftest"

key-files:
  created:
    - .github/workflows/architecture-site.yml
    - docs/architecture/site/Dockerfile
    - docs/architecture/site/nginx.conf
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-01-local-probe.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-01-i12-red.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-01-i12-green.txt
  modified:
    - docs/INFRA_ARCHITECTURE.md
    - scripts/verify-k8s-invariants.py
    - scripts/verify-k8s-invariants-selftest.py

key-decisions:
  - "download-artifact pinned at d3f86a106a0bac45b974a628896c90dbdf5c8093 (v4), the value gh api printed at execution, unchanged from planning time"
  - "I12's scope sentence lives in the I12 docstring entry, not the Scope paragraph, because the comment lint caps prose before the first Known holes marker at 8 lines"

requirements-completed: [PUB-01, PUB-02]

coverage:
  - id: D1
    description: "architecture-site.yml: build job gated by sync.mjs check before likec4 build, no environment and no secret; publish job needs build, main-only, SHA-pinned, tags main-<run_number>-<sha7>"
    requirement: "PUB-01"
    verification:
      - kind: other
        ref: "yq checks on .github/workflows/architecture-site.yml (build.environment null, publish.environment production, push and pull_request paths equal, sync.mjs check numbered before likec4 build, 0 secrets. in build, 0 unpinned uses in publish)"
        status: pass
    human_judgment: true
    rationale: "The workflow has not run on GitHub; actions/download-artifact v4 pairing with upload-artifact v4 and the real run are settled by the next plan's first push."
  - id: D2
    description: "Site image serves history URLs with a real asset 404 and no file read, under user 101, read-only root, all caps dropped"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-01-local-probe.txt (4 matching PROBE lines)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Invariant I12 refuses a LikeC4 dev or preview server in a workflow, Dockerfile or manifest"
    requirement: "PUB-02"
    verification:
      - kind: unit
        ref: "python3 scripts/verify-k8s-invariants-selftest.py (I12 cases); 03-01-i12-red.txt and 03-01-i12-green.txt"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-28
status: complete
---

# Phase 3 Plan 01: Site image, workflow and invariant I12 Summary

**A two-job workflow builds the LikeC4 static site behind the drift gate into an nginx-unprivileged image that serves deep links with 200 and a missing asset with a real 404, and invariant I12 now fails CI on any dev or preview server in a manifest, Dockerfile or workflow.**

## Performance

- **Duration:** 8 min (2026-10-28T20:13Z to 20:21Z)
- **Tasks:** 2 of 2
- **Files modified:** 9 (6 created, 3 modified)
- **Commits:** 2 (measured from the plan ledger)

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 | 1046e0c | ci(architecture-site): build the LikeC4 site into an nginx image, gated by the drift check |
| 2 | 76c843a | test(k8s-invariants): I12 refuses a LikeC4 dev or preview server in a manifest, Dockerfile or workflow |

Both went through the full `.githooks/pre-commit`. Nothing was pushed.

## Accomplishments

- **Workflow.** `.github/workflows/architecture-site.yml`: `build` has no environment and no `secrets.` reference (0 string hits); `publish` needs `build`, runs only when the event is not a pull request and the ref is `refs/heads/main`, names `environment: production`, and pins every `uses` by a 40-character SHA. Push and pull_request path lists are deep-equal (five entries, none under k8s). download-artifact SHA used: `d3f86a106a0bac45b974a628896c90dbdf5c8093`.
- **Image.** From the local run of the workflow's own build commands: image ID `sha256:10146ec6075f55186e6055d4d1a3969d8ad1da7090600f2c1e25793df159442e`, size 10,233,192 bytes, run as `--user 101:101 --read-only --cap-drop ALL --security-opt no-new-privileges --memory 32m --tmpfs /tmp`.
- **Gate half of the dev-server criterion.** I12 plus selftest, shown not catching before and catching after on the real tree (below).

## The four probe lines (03-01-local-probe.txt)

```
PROBE / status=200 marker=1 sha256=a3ac3daa6e0438078c582ad9333142e144b3a5718fab1e9749f60a56c65759b5
PROBE /view/signin-ok/ status=200 content-type=text/html
PROBE /assets/missing.js status=404 immutable=no cache-control=[]
PROBE /@fs/etc/hostname status=200 sha256=a3ac3daa6e0438078c582ad9333142e144b3a5718fab1e9749f60a56c65759b5 same_as_root=yes
```

Also recorded: `/healthz` 200 `ok`, `/robots.txt` 200 with `Disallow: /`.

## The dev-server control line

```
CONTROL devserver /@fs/etc/hostname status=200 same_as_root=no same_as_file=no
```

The plan expected `same_as_file=yes`; the recorded value is `no`. See Deviations: the body is the file, but with a sourcemap comment appended, so the byte hash differs.

## The three I12 lines (03-01-i12-red.txt)

```
SELFTEST BEFORE exit=1          (AttributeError: no attribute 'check_i12_no_dev_server')
GATE BEFORE I12 exit=0          (three plants in place, hole exists)
GATE AFTER I12 exit=1
FAIL: I12: .github/workflows/architecture-site.yml:78: 'run: pnpm --dir docs/architecture exec likec4 start' starts a dev or preview server; ...
FAIL: I12: docs/architecture/site/Dockerfile:9: 'CMD ["npx", "likec4", "preview"]' starts a dev or preview server; ...
FAIL: I12: k8s/base/app/app.yaml: Deployment/app container app runs 'likec4 start'
```

`03-01-i12-green.txt`: `GATE GREEN exit=0` with the plants removed. Selftest after: `selftest OK -- I1-I12 ...`, exit 0.

Plant discipline: `CLEAN BEFORE PLANT worktree=0 index=0` was written before the first gated run; the plants were added and removed with the Edit tool only, never `git checkout`, `git restore` or `git stash`; `git diff --exit-code` on the three paths exited 0 afterwards.

## Deviations from Plan

**1. [Rule 3 - Blocking] I12 scope sentence moved from the Scope paragraph into the I12 docstring entry**
- **Found during:** Task 2, `python3 scripts/verify-comments.py check`
- **Issue:** Extending the module docstring's first paragraph and Scope paragraph, as the plan said, made the first paragraph 3 lines (limit 2) and put 11 prose lines before the first marker (limit 8), failing `summary-first` and `segregated-narration`.
- **Fix:** First paragraph kept to 2 lines ("..., no object has two Flux owners, no dev server."); the Scope paragraph left as it was; the scope statement (source text of every k8s YAML file plus every Dockerfile and workflow) written inside the I12 entry, which sits after the marker. The Known holes bullet and the I12 entry are as planned.
- **Files modified:** scripts/verify-k8s-invariants.py
- **Commit:** 76c843a

**2. [Measurement differed from plan expectation, recorded as the plan directs] Dev server control line reads `same_as_file=no`**
- **Found during:** Task 1, dev-server control
- **Issue:** The plan expected `same_as_file=yes`. The dev server returned 200, `Content-Type: text/javascript`, 223 bytes for a 16 byte file: the file's exact text (`DESKTOP-88EQU47`) followed by a `//# sourceMappingURL=data:...` comment. The hole is present (the file is read through `/@fs`); the byte hash differs only because of the appended sourcemap.
- **Action:** Recorded as measured with a NOTE line; not retried with other flags. The plan's own text makes a different outcome a recorded result and not a stop. The line regex still matches (count 1).
- **For the phase gate:** a live FSREAD check that compares a sha256 against the file will read `no` on a dev server too. Compare a prefix or the first line of the body instead if the clause is kept; the static image's answer (the app shell, hash equal to `/`) is unaffected.

**Total deviations:** 1 auto-fixed (Rule 3), 1 measurement differing from expectation (recorded as the plan directs). **Impact:** none on the committed artifacts.

## Issues Encountered

- The first `docker rmi` of the nginx base by tag answered "No such image" although `docker images` listed it; removing by image ID worked. All probe containers and images are gone.
- Scratch probe scripts and a dev server log under `docs/architecture/out/` were created and trashed with `trash-put`; the directory itself was left alone.

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None. The publish-job exposure of `DOCKERHUB_TOKEN` to any same-repo branch (the environment has no deployment-branch policy) is the residual risk already recorded in the plan's threat register as T-03-02 and fenced by the next plan's environment restriction; this plan adds no new surface beyond it.

## Verification results

- Plan-level: workflow yq checks all as expected (`null`, `production`, `true`, sync.mjs check line 2 before likec4 build line 3); probe record greps 4 and 1; I12 red record `FAIL: I12:` count 3, header greps 3, green 1, first line `CLEAN BEFORE PLANT worktree=0 index=0`.
- Final tree: `python3 scripts/verify-comments.py check` OK; `node docs/architecture/sync.mjs check` OK; gate with `--no-provisional` OK (12 roots); selftest OK (I1-I12).
- Not run here: the workflow itself on GitHub (A1, download-artifact v4 with upload-artifact v4, is settled by the first real run).

## Next Phase Readiness

Ready for plan 02, which owns the first push, the Docker Hub repository and the environment deployment-branch policy. The first push of this plan's commits starts one run of the whole app pipeline, because `.github/**` is not in deploy.yml's `paths-ignore`.

## Self-Check: PASSED
