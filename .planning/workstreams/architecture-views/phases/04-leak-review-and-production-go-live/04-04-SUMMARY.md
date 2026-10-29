---
phase: 04-leak-review-and-production-go-live
plan: 04
subsystem: infra
tags: [cert-manager, letsencrypt, flux, certificate-flip, docs]

requires:
  - phase: 04-leak-review-and-production-go-live
    provides: APPROVED_TAG main-8-c3c366a and DECISION Q2 approve (plan 03)
provides:
  - the architecture site served on a Let's Encrypt production certificate (flip commit 26c0e3e, pushed in ca9d2e7)
  - the post-flip proof from the cluster and from outside, each recorded failing before the push
  - DECISION Q3 1 and Q4 1 recorded verbatim
  - the public address linked from README.md and docs/INFRA_ARCHITECTURE.md, in a local, unpushed docs commit (44a7976)
affects: [04-05]

plan_head_before: 9e5079e185dadf9039f59c944c5ab273de462ab0
plan_head_after: 44a7976d9ce992f84a20850f6ca88473db11c285

actuals:
  tokens: 1600
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "Flip a cert-manager issuer in place: same secretName, the old Secret serves until the new one is written"
    - "TAGCHECK three times (before the question, before the commit, after the pre-push fetch) so the flipped site is the reviewed tag"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-04-flip.txt
  modified:
    - k8s/overlays/architecture/certificate.yaml
    - README.md
    - docs/INFRA_ARCHITECTURE.md
    - docs/INFRA_RUNBOOK.md
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-DECISIONS.txt

key-decisions:
  - "Q3 1 flip now, reply \"Q3 1, Q4 1\""
  - "Q4 1 push and drill on GitHub, reply \"Q3 1, Q4 1\"; plan 05 acts on it, this plan pushed nothing beyond the flip"

requirements-completed: [PUB-03]

coverage:
  - id: D1
    description: "Certificate architecture is Ready on letsencrypt-production at a higher revision, the Secret's issuer-name annotation reads letsencrypt-production, the served issuer is Let's Encrypt without (STAGING), and curl without -k exits 0 with 200"
    requirement: PUB-03
    verification:
      - kind: integration
        ref: "04-04-flip.txt BASELINE CERT/TLS/CURL_STRICT (failing halves) against CERT revision=2, SECRET, TLS, CURL_STRICT exit=0 code=200"
        status: pass
    human_judgment: false
  - id: D2
    description: "The flip is one commit that changes only certificate.yaml, made after DECISION Q2 approve and Q3 1; git log on the file shows 8e531d0 then 26c0e3e"
    requirement: PUB-03
    verification:
      - kind: integration
        ref: "git log --format=%h -- k8s/overlays/architecture/certificate.yaml; git diff-tree on 26c0e3e"
        status: pass
    human_judgment: false
  - id: D3
    description: "README.md and docs/INFRA_ARCHITECTURE.md link the root address; every robots line says it is not access control; no doc says the site is on staging"
    requirement: PUB-06
    verification:
      - kind: integration
        ref: "rg checks of Task 3 (counts 1 and 1, robots count 1, guards empty)"
        status: pass
    human_judgment: false
    note: "The link is committed locally (44a7976) and not on origin; PUB-06 is complete only after plan 05 pushes it"

duration: about 35min of executor work in the resumed session (Task 1 resume through Task 3; the owner wait before it is excluded)
completed: 2026-10-29
status: complete
---

# Phase 4 Plan 04: Production certificate flip Summary

**The architecture site is served on a Let's Encrypt production certificate: one commit changed `issuerRef` to `letsencrypt-production`, cert-manager re-issued into the same Secret within 2.4 minutes of the push, and `curl` without `-k` now returns 200; the README and INFRA docs link the root address in a local commit that waits for plan 05's push.**

## Decisions, verbatim

- Q3: `DECISION Q3: 1 flip now; user reply: "Q3 1, Q4 1"`
- Q4: `DECISION Q4: 1 push and drill on GitHub; user reply: "Q3 1, Q4 1"`

Q4 is acted on by plan 05. This plan pushed the flip and nothing else.

## Tag checks (all equal)

- `TAGCHECK before-q3 2026-10-29 approved=main-8-c3c366a overlay=main-8-c3c366a pod=main-8-c3c366a site_runs_after_approval=0 verdict=equal` (written before the resume, commit 0e5ba30)
- `TAGCHECK before-commit ... verdict=equal` (same three tags, 0 runs)
- `TAGCHECK before-push ... verdict=equal`, run after the fetch that preceded the push (same three tags, 0 runs)

## Baseline and after

| Check | Before the push | After |
|---|---|---|
| Served issuer | `C=US, O=Let's Encrypt, CN=(STAGING) Dastardly Durum YR1` | `C=US, O=Let's Encrypt, CN=YR1` |
| `curl` without `-k` | exit 60, code 000 | exit 0, code 200, `id="likec4-root"` count 1 |
| Certificate | `letsencrypt-staging`, Ready True, revision 1 | `letsencrypt-production`, Ready True, revision 2 |
| Secret `issuer-name` annotation | `letsencrypt-staging` | `letsencrypt-production` |
| Other hosts | prod 200, nonprod 200, monitoring 302 | prod 200, nonprod 200, monitoring 302 |

prod and nonprod are read at `/api/healthz`, monitoring at `/` (as in plan 03).

## Accomplishments

- **Flip commit.** `26c0e3e0caa27404f65f245937e3fa9b441faaa2` changes only `k8s/overlays/architecture/certificate.yaml` (issuerRef line and header comment), ran the full pre-commit hook (no `--no-verify`), after `verify-k8s-invariants.py --no-provisional` and `verify-comments.py check` passed. `git log` on the file prints `26c0e3e`, then `8e531d0`.
- **Push.** The pre-push fetch showed 2 commits ahead on origin, both `fluxcdbot chore(flux): bump images` (`ea1855b`, `efeb3fd`), merged without conflict as `ca9d2e7`. The range was 7 commits: (a) the flip `26c0e3e`; (b) planning-only `e77fee6 0e5ba30 9e5079e 9354a94 e81c839`; (c) the merge. `PUSHED ca9d2e7b49355a80aff4abb0fb91aa089932f2e1 at 2026-10-29T18:46:09Z` by explicit SHA, no force.
- **Issuance.** At 18:46:23Z the Certificate was still on staging; at 18:48:23Z Flux had applied the new issuerRef (`letsencrypt-production`, Ready False) with CertificateRequest `architecture-2` and solver Service `cm-acme-http-solver-jqjq5` of type ClusterIP; at 18:48:49Z the Certificate read Ready True, revision 2. The Ready condition's `lastTransitionTime` is 18:48:30Z, so `LATENCY push_to_ready=2.4` minutes (staging in Phase 3: 1.6). Most of that was Flux's reconcile interval before it applied the edit. The new certificate is valid 2026-10-29T17:50:09Z to 2027-01-27T17:50:08Z.
- **CI for the flip push.** Invariant Checks run 40933917779 (all 7 jobs success), Secret Scan 40933917815 and Dependency Submission 40933917750 succeeded. With the Invariant Checks witness present for the same SHA, `architecture-site runs=0 deploy runs=0`, as expected for a k8s-plus-planning push.
- **Docs (Task 3).** `44a7976` (docs-only, `--no-verify`): README `## Live` gains a bold "Architecture views" link to the root address above the dashboards paragraph; INFRA_ARCHITECTURE.md names the public address, the production certificate, the leak scan before the image build, and `robots.txt` as not access control on one line; INFRA_RUNBOOK.md says the site has been on a production certificate since 2026-10-29 with the staging Certificate in git history. All Task 3 checks pass.

## Task Commits

1. **Task 1 (resume): record DECISION Q3 and Q4** - `e77fee6` (docs). The `TAGCHECK before-q3` line was committed earlier as `0e5ba30`.
2. **Task 2: flip, push, issuance, outside checks** - flip `26c0e3e`, Flux merge `ca9d2e7`, record `adc5042` (docs).
3. **Task 3: link the public address** - `44a7976` (docs).

`commits: 6` is measured with `git rev-list --first-parent --count 9e5079e..44a7976`, so it excludes the two Flux commits beneath the merge. The measurement base is the HEAD before `0e5ba30`, the first commit of this plan.

## Unpushed

`44a7976` (the docs commit) and the close-out commit for this SUMMARY and the state files are local only. Under DECISION Q4 1 plan 05 pushes them together with `uptime-check.yml`. Nothing else was pushed after `ca9d2e7`; no drill branch exists.

## Deviations from Plan

None to the plan's outcomes. Notes:

- `git grep -h newTag origin/main -- k8s/...` returned nothing the first time because the pathspec is relative to the shell's cwd (the phase directory). Re-running it with `':/k8s/overlays/architecture/kustomization.yaml'` read `main-8-c3c366a`. That empty output was not recorded as a tag.
- The Flux merge changed the local architecture overlay from `main-7-560fc77` to `main-8-c3c366a`. That is not a new tag: origin and the pod already ran `main-8-c3c366a` (bump `ea1855b`, 17:40Z) at the approval, which my local branch had not yet merged. The TAGCHECK compares origin's overlay, which stayed equal.
- `04-04-watch.sh`, a scratch poller written because the tool hook refuses inline loops, was deleted before the record commit.

## Issues Encountered

None that changed an outcome. The failure branch (revert to staging) was not needed.

## Not verified

- Research A3 (the old staging Secret keeps serving while the production order is in flight, no TLS gap) was not measured: I polled cluster state every 10 seconds but did not probe the served certificate during the roughly 2-minute window.
- No Certificate Transparency lookup after the flip (crt.sh was not queried); that the production entry is logged is expected, not observed.
- The `id="likec4-root"` marker count and 200 status were read once after Ready, not over time. Whether browsers show no warning was not tested in a browser; `curl` against the system trust store and the `openssl` issuer are the evidence.
- `actuals.tokens` is an estimate (chars/4 over the changed files, about 6 KB), not a precise count.

## Known Stubs

None. No stub patterns in the files changed.

## Threat Flags

None. The flip adds no endpoint, auth path or file access; the hostname's entry in Certificate Transparency logs was accepted as T-04-18.

## Self-Check: PASSED

Found: `k8s/overlays/architecture/certificate.yaml` (issuerRef `letsencrypt-production`), `04-04-flip.txt`, `04-DECISIONS.txt` with two Q3/Q4 lines. Commits `e77fee6`, `26c0e3e`, `ca9d2e7`, `adc5042`, `44a7976` verified as ancestors of HEAD; `ca9d2e7` is on origin/main.
