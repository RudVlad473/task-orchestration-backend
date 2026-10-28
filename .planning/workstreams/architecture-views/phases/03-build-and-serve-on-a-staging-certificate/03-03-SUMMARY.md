---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 03
subsystem: infra
tags: [k3s, memory-ladder, cert-manager, clusterissuer, http01, flux, nginx]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: plan 02's published image main-1-5d44dac, the push procedure and the Q1 to Q4 and Q7 decisions
provides:
  - the architecture site image's memory floor measured on the VPS, with the adopted values 16Mi request and 32Mi limit
  - an architecture-site entry in the runbook's k3s resource measurement section
  - serviceType ClusterIP on both ClusterIssuers, applied live, with the three live Certificates unchanged
  - a k8s-only push shown to start no architecture-site run
affects: [03-04, 03-06]

actuals:
  tokens: 2000
  tasks: 2
  commits: 5
plan_head_before: d6fe95efff04ba3b7a4c029654184e8dfa5dded5
plan_head_after: 68999af05afbac74fe1fb4d48be3951b7fa0ea91

tech-stack:
  added: []
  patterns:
    - "memory ladder in a scratch pod, driven from the VPS host at the pod IP, never through Traefik"
    - "a cgroup memory.events oom_kill count and an all-200 response check beside restarts, termination reason and dmesg, because nginx's master respawns an OOM-killed worker without restarting the container"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-03-ladder.txt
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-03-solver.txt
  modified:
    - docs/INFRA_RUNBOOK.md
    - k8s/platform/edge/clusterissuers.yaml

key-decisions:
  - "Adopt requests.memory=16Mi limits.memory=32Mi: 16Mi is the lowest passing rung, 8Mi was OOM-killed, and the limit is twice the request as in Traefik's row"
  - "Q2 1 applied: both issuers ask for a ClusterIP challenge Service; the live Certificates kept READY, revision and notAfter"

requirements-completed: []  # PUB-02 is contributed to, not completed: no Deployment, route or hostname exists until plans 04 to 06

coverage:
  - id: D1
    description: "The site image's memory floor is measured on the VPS and the adopted values re-verify clean from a fresh pod"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-03-ladder.txt RUNG lines (64Mi, 32Mi, 16Mi pass; 8Mi OOMKilled), ADOPTED, REVERIFY limits=32Mi result=pass, CLEANUP"
        status: pass
    human_judgment: false
  - id: D2
    description: "Both ClusterIssuers carry serviceType ClusterIP live and the three live Certificates did not re-issue"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-03-solver.txt AFTER live solvers, AFTER certificates and A2: unchanged"
        status: pass
    human_judgment: false
  - id: D3
    description: "The k8s-only push started no architecture-site run and no deploy run"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-03-solver.txt CONTROL lines, witnessed by Invariant Checks run 40768818477 on the same SHA"
        status: pass
    human_judgment: false

duration: 70min
completed: 2026-10-28
status: complete
---

# Phase 3 Plan 03: Measure the site's memory floor and switch the issuers to a ClusterIP solver Summary

**The site image runs clean under 16Mi on the VPS and is OOM-killed at 8Mi, so plan 04 adopts 16Mi/32Mi; both ClusterIssuers now ask for a ClusterIP challenge Service, applied live by Flux with the three production Certificates untouched.**

## Performance

- **Duration:** about 70 min
- **Completed:** 2026-10-28
- **Tasks:** 2 of 2
- **Files modified:** 2 records created, the runbook and one manifest changed

## Accomplishments

- Ran the ladder on the VPS (4 vCPU, 7945 MiB) in namespace ladder-scratch with the published tag `main-1-5d44dac` and the real Deployment's security settings. Each rung drove 442 requests from the host at the pod (every file once, 200 gzip fetches of the main script, 10 concurrent cold loads) and 20 s of settle.
- Rungs, copied from the record:
  - `RUNG 64Mi restarts=0 lastState=none oom_dmesg=0 anon_kib=4064 shmem_kib=4 ready=yes result=pass`
  - `RUNG 32Mi restarts=0 lastState=none oom_dmesg=0 anon_kib=4060 shmem_kib=4 ready=yes result=pass`
  - `RUNG 16Mi restarts=0 lastState=none oom_dmesg=0 anon_kib=4040 shmem_kib=4 ready=yes result=pass`
  - `RUNG 8Mi restarts=1 lastState=OOMKilled oom_dmesg=24 anon_kib=unreadable shmem_kib=unreadable ready=yes result=fail`
- `ADOPTED: requests.memory=16Mi limits.memory=32Mi`, then `REVERIFY limits=32Mi result=pass` from a fresh pod at request 16Mi and limit 32Mi (0 restarts, no OOM, all 442 responses 200). Plan 04's MEASURED comment copies the ADOPTED line.
- Deleted the namespace and read `NotFound` twice (after the ladder and again at SUMMARY time); the driver script and captures are trashed and `docs/architecture/out/ladder-pod.yaml` was never written (the manifest lived inside the driver script).
- Added the architecture-site subsection and table row to `docs/INFRA_RUNBOOK.md` (no IPs).
- Set `serviceType: ClusterIP` on both issuers in `6a023fe`, through the full pre-commit hook, and pushed `855359585fdb6822b99dc8e05324d4e2a9d50723` by explicit SHA. Flux's edge Kustomization applied it; the live issuers show `"serviceType":"ClusterIP"`; the three Certificates kept READY True, revision 1 and their notAfter values (A2 observed, no longer an inference); the only non-ClusterIP Service is still `kube-system/traefik`.
- Control: the push has an Invariant Checks run (success) and zero architecture-site and zero deploy.yml runs.

## Task Commits

1. **Task 1: ladder record and runbook** - `bc82d9b` (docs)
2. **Task 2: issuer edit** - `6a023fe` (fix); push merge `8553595`; record `68999af` (docs)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Stricter rung failure signals**
- **Found during:** Task 1, driver design
- **Issue:** the plan's pass rule (restarts, termination reason, dmesg, Ready) misses an OOM-killed nginx worker, because nginx's master process respawns workers and the container never restarts.
- **Fix:** a rung also failed on a non-zero `oom_kill` in the container's `memory.events` and on any response other than 200. Neither tripped at a passing rung (cgroup_oom_kill=0, non200=0 in all four passing cycles), so the adopted values would be the same under the plan's rule.
- **Files modified:** none (the driver script was scratch and is trashed; the rule is stated in the ladder record and the runbook)

**2. [Plan expectation wrong] the k8s diff acceptance lists two more paths**
- **Found during:** Task 2 acceptance
- **Issue:** `git diff --name-only bc82d9b..HEAD -- k8s` prints `clusterissuers.yaml` plus `k8s/overlays/nonprod/kustomization.yaml` and `k8s/overlays/prod/kustomization.yaml`, because HEAD contains the Flux merge.
- **Resolution:** those two paths are fluxcdbot's `673238f`, already on origin; `git diff --name-only 673238f 855359585fdb6822b99dc8e05324d4e2a9d50723 -- k8s` prints only `clusterissuers.yaml`, so the pushed diff matches the intent. Recorded as a NOTE in 03-03-solver.txt.

**3. [Observation] the 8Mi rung's evidence is thin**
- The 8Mi pod was OOM-killed while the cycle ran, so the file list read failed (`command terminated with exit code 137`), only the 200 gzip fetches ran (108 not 200), and anon and shmem were unreadable. The failure is established by `lastState=OOMKilled`, one restart and 24 new dmesg OOM lines; the cgroup figures at 8Mi are absent, not zero.

**4. [Observation] Flux transiently showed the edge Kustomization not ready**
- After the push, edge read `dependency 'flux-system/cert-manager' is not ready` for a few minutes while the upstream Kustomizations re-applied the new revision, then applied `8553595` without intervention. Applied revision confirmed with `kubectl wait --for=jsonpath` rather than a sleep loop.

**5. [Process] PUB-02 left Pending**
- The plan frontmatter lists PUB-02 and `requirements.mark-complete` ticked it. I reverted that one file change (`git checkout` of REQUIREMENTS.md), because PUB-02 says the manifests serve the image at the hostname and plans 04 to 06 have not built that yet. This plan delivers its memory and solver inputs only.

## Authentication gates

None.

## Known Stubs

None.

## Threat Flags

None. The solver Service type change is the plan's own T-03-14 mitigation; plan 06 observes the live challenge Service.

## Open items carried forward

- Plan 04 writes the Deployment's `requests.memory: 16Mi` and `limits.memory: 32Mi` under a MEASURED comment citing the runbook entry dated 2026-10-28.
- Plan 06 still has to observe the staging challenge Service as type ClusterIP and the staging Certificate turning Ready (research A4).
- Traefik's restart count after the cold loads (research optional item) was not measured here; the cold loads never passed through Traefik.
- Two critical Dependabot alerts were again reported on push; outside this plan.

## Self-Check: PASSED

Verified: `03-03-ladder.txt` has one ADOPTED line and four RUNG lines; `03-03-solver.txt` has the SOLVER line and both A2 and CONTROL lines; `serviceType: ClusterIP` appears twice in `k8s/platform/edge/clusterissuers.yaml`; `bc82d9b`, `6a023fe`, `8553595` and `68999af` are ancestors of HEAD and `855359585fdb6822b99dc8e05324d4e2a9d50723` is an ancestor of origin/main; `verify-k8s-invariants.py --no-provisional`, `verify-k8s-manifests.sh`, `sync.mjs check` and `verify-comments.py check` exit 0; `ladder-scratch` answers NotFound; the ladder record and the runbook additions hold no dotted quad.
