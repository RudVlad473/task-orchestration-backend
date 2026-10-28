---
phase: 03-build-and-serve-on-a-staging-certificate
plan: 06
subsystem: infra
tags: [flux, traefik, cert-manager, letsencrypt-staging, likec4, nginx, live-verification]

requires:
  - phase: 03-build-and-serve-on-a-staging-certificate
    provides: plan 04's manifests and Flux wiring, plan 05's drills, plan 03's ClusterIP solver, plan 02's published image and push procedure
provides:
  - the architecture site served at its hostname over HTTPS on a Let's Encrypt staging certificate, owned by Flux Kustomization apps-architecture
  - DECISION Q5 1 and Q6 1 in 03-DECISIONS.txt, written before the push
  - 03-06-live.txt, the baseline, push, Flux, certificate, TLS, marker, other-host, ownership, bump, CI and static-only record
  - the first observed site bump: Flux rolled main-4-b1aa8c5 through a bump commit that started no site run
affects: [03-07]

actuals:
  tokens: 1800
  tasks: 3
  commits: 2
plan_head_before: c2e0be449a27fb3af24327a940e0c3f040bbb8bf
plan_head_after: c789fed96d7f75a5817da6aa5090f9da6c0905c7

tech-stack:
  added: []
  patterns:
    - "a live check is shown red on the host before the push (BASELINE) and green after, per probe"
    - "waiting on the cluster with `kubectl wait` (condition and jsonpath) instead of a sleep loop"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-06-live.txt
  modified:
    - .planning/workstreams/architecture-views/phases/03-build-and-serve-on-a-staging-certificate/03-DECISIONS.txt

key-decisions:
  - "Q5 1 and Q6 1 were authorized by the user's reply `Q5 1 Q6 1`; Q6 lets plan 07 push its model-only commit alone after a planning-only flush"
  - "Pushed 8 commits by explicit SHA b1aa8c5: 4 class (a), 4 class (b), no Flux merge needed (origin was 0 ahead of local)"

requirements-completed: [PUB-01, PUB-02]

coverage:
  - id: D1
    description: "The architecture host failed each probe before the push (Traefik default certificate, 404 with no marker, deep link 404 with 0 SVG nodes) and passes after: staging issuer, 200 with the likec4-root marker, deep link 200 with 184 SVG nodes"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-06-live.txt BASELINE lines (4) against TLS issuer, GET /, DEEPLINK and DEEPLINK_CURL lines"
        status: pass
    human_judgment: false
  - id: D2
    description: "Flux Kustomization apps-architecture Ready, Deployment 1/1 with 0 restarts, Certificate Ready with issuerRef letsencrypt-staging, both IngressRoutes carry the apps-architecture label, and only kube-system/traefik is non-ClusterIP"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-06-live.txt KUSTOMIZATION, ROLLOUT, CERT, I11 and NONCLUSTERIP lines"
        status: pass
    human_judgment: false
  - id: D3
    description: "The prod and nonprod /api/healthz answer 200 and the monitoring host answers 302, before and after"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-06-live.txt BASELINE OTHERS and OTHERS lines (identical)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The push changed the model, the site workflow published main-4-b1aa8c5, Flux committed the bump, the pod rolled to it, and the bump commit has no architecture-site run"
    requirement: "PUB-01"
    verification:
      - kind: integration
        ref: "03-06-live.txt SITE RUN, FLUX BUMP, ROLLED and CONTROL flux-bump lines (witness Invariant Checks 40772915022)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Only static output is reachable: /@fs/etc/hostname returns the app shell, a cold deep link renders, the missing-asset response is recorded"
    requirement: "PUB-02"
    verification:
      - kind: integration
        ref: "03-06-live.txt FSREAD, DEEPLINK, MISSING_ASSET lines"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-10-28
status: complete
---

# Phase 3 Plan 06: Serve the site live on a staging certificate Summary

**The LikeC4 site is live at the architecture hostname behind a Let's Encrypt staging certificate, applied by Flux from one push, with every probe shown red before and green after, the other three hosts unchanged, and Flux's own image bump rolling a newer tag without starting a site run.**

## Performance

- **Duration:** about 25 min for this continuation (the Task 1 checkpoint wait is not counted)
- **Completed:** 2026-10-28
- **Tasks:** 3 of 3
- **Files:** 1 record created, 1 decisions file appended

## Decisions

User reply `Q5 1 Q6 1`, recorded as `DECISION Q5: 1 Push now` and `DECISION Q6: 1 Authorize plan 07's closing model-only push` (b1aa8c5), before anything was pushed. Neither line came from a "use my best judgment" reply.

## Baseline (before the push, 03-06-live.txt)

- `TLS issuer=CN=TRAEFIK DEFAULT CERT`
- `GET / status=404 marker=0`
- `DEEPLINK /view/signin-ok/ nav_status=404 svg_nodes=0`, `DEEPLINK_CURL /view/signin-ok/ status=404`
- `OTHERS prod=200 nonprod=200 monitoring=302`, `TRAEFIK restarts=2`

## The push

Local main was 8 ahead and 0 behind after `git fetch origin`, so no Flux merge. Classes: (a) b1aa8c5 decisions, 0728e51 and 5377803 drills, 8e531d0 site commit; (b) planning-only c2e0be4, 03ca15a, 18bb910, 68999af; (c) none. Pushed `b1aa8c581f2380c70f911fb18a70833f6ee355b2` at 2026-10-28T21:28:02Z with `${SHA}:refs/heads/main` (origin tip was 8553595).

## Observed after the push

- Solver Service `cm-acme-http-solver-749lj` was seen once, `type=ClusterIP`; the Certificate went Ready at about 21:29:39Z, 1.6 minutes after the push, `issuerRef letsencrypt-staging`.
- Kustomization apps-architecture Ready at `b1aa8c5`; Deployment architecture-site 1/1, 0 restarts.
- `TLS issuer=C=US, O=Let's Encrypt, CN=(STAGING) Dastardly Durum YR1`; `GET / status=200 marker=1`; `OTHERS` unchanged (200, 200, 302).
- Both IngressRoutes carry `kustomize.toolkit.fluxcd.io/name: apps-architecture`; the only non-ClusterIP Service is kube-system/traefik.
- CI for the pushed SHA: Invariant Checks 40772183789, Architecture Site 40772184020 (build and publish success), Secret Scan and Dependency Submission all success; the five invariant jobs listed in the record, k8s-invariants (run with `--no-provisional`, workflow line 124), manifests-valid, likec4 drift and comment-lint included, all success.
- Bump: the site run (number 4) published `main-4-b1aa8c5` at 21:28:59Z; fluxcdbot committed `fe3466b` (`chore(flux): bump images`, only k8s/overlays/architecture/kustomization.yaml, newTag `main-4-b1aa8c5`) at 21:33:09Z; the pod rolled at 21:35:08Z with 0 restarts. Invariant Checks run 40772915022 exists for fe3466b and Architecture Site lists 0 runs, so the control is valid. Latency: push to Ready 1.6 min, tag to rollout 6.2 min (research A6 estimated about 12 total).
- Static-only: `/@fs/etc/hostname` returns the same 816-byte HTML as `/` (same sha256); a cold `/view/signin-ok/` has navigation status 200 and 184 SVG nodes in headless Chromium (curl 200); `robots.txt` is `Disallow: /`.

## Findings for Phase 4

- **MISSING_ASSET:** `/assets/missing.js` answers 404 `text/html`, no cache-control, so no `immutable` on a miss (Open Question 1 settled as expected, no correction needed). The status is 404, not a finding against the plan.
- **TRAEFIK restarts:** 2 before and 2 after 22 files (6.9 MB) loaded 10 at a time, so no restart under this load; Traefik's 2 earlier restarts are older than this phase.
- **CT:** crt.sh returned HTTP 200 and `[]` for the name about 8 minutes after issuance (0 entries). No CT claim is written into any doc; absence on one query is not proof staging certificates never reach CT.
- **SVG node count:** 184 on the live build against 38 in the plan's note on the research's local build of the same view. The check is "above 0" and passes; the difference was not investigated, so no cause is claimed.
- The first rollout (main-1-5d44dac, the tag in the overlay when the Kustomization applied) preceded the bump to main-4; the live record's `ROLLOUT` line names main-1, `ROLLED` names main-4.

## Task Commits

1. **Task 1: decisions** - `b1aa8c5` (docs, pushed as the tip of the authorized range)
2. **Tasks 2 and 3: live record** - `c789fed` (docs, local; plan 07's flush pushes it)

## Deviations from Plan

None - plan executed as written. Two mechanical adaptations, neither a plan change: the 10-second solver poll ran as a background script under /tmp (read-only ssh calls), and the waits for the certificate and the image rollout used `kubectl wait` (condition and jsonpath) instead of a poll loop, because this runtime has no Monitor tool and foreground sleeps are blocked. The solver Service was seen on two consecutive polls (12 seconds apart) and gone on the next.

## Authentication gates

None.

## Known Stubs

None.

## Threat Flags

None beyond the plan's register: T-03-21 accepted by Q5 (the unreviewed model is now readable behind a staging-certificate warning until Phase 4's review), T-03-22 mitigated (`/@fs` returns the shell), T-03-23 mitigated (other hosts unchanged), T-03-24 mitigated (no Traefik restart).

## Open items carried forward

- Local main is 2 ahead of origin/main (c789fed and the docs commit for this summary) and 1 behind (Flux bump fe3466b); plan 07's push procedure merges the bump and flushes the planning-only commits first.
- The site is live and unreviewed until Phase 4: the leak scan, the review and the production certificate flip.
- Two critical Dependabot alerts reported by GitHub on every push are outside this plan.

## Self-Check: PASSED

Verified: 03-06-live.txt and the two DECISION lines exist; `b1aa8c5` is an ancestor of origin/main (the push) and `c789fed` an ancestor of HEAD; the record's verify greps print 4 BASELINE failing halves, 0 baselines already green, 4 after-lines, 2 I11 lines, 1 CONTROL line, 3 Task 3 lines; the record holds no `dckr_pat_` or private-key string.
