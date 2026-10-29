---
phase: 04-leak-review-and-production-go-live
plan: 05
subsystem: infra
tags: [uptime-probe, github-actions, alert-issue, drill, production-url, robots]

requires:
  - phase: 04-leak-review-and-production-go-live
    provides: production certificate Ready, CURL_STRICT exit=0 code=200, DECISION Q4 1 (plan 04)
provides:
  - uptime-check.yml probes the architecture site root for id="likec4-root", timeout-minutes 10 (5 URLs x 80s = 400s, 6 URLs x 80s = 480s), known hole (f)
  - the runbook's Uptime alert triage and the CI learning doc name the site, plus a one-paragraph site triage
  - plan 04's docs commit and the planning commits pushed with the probe change (f4a11fb on origin/main)
  - a wrong-marker drill on GitHub: red for the site alone, alert issue 23 opened, a main dispatch green, issue 23 closed, drill branch deleted
  - production hostname measurements without -k: cold deep link and reload render, missing asset is 404 text/html, robots.txt Disallow
affects: [04-verification]

plan_head_before: a8afc0482aba21d52560e5acba2ec580eff15ebf
plan_head_after: 7e0f4c913ee1b1458d5f417d8a87ed8f09f326e5

actuals:
  tokens: 4300
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Drill an alert path with a throwaway branch carrying a wrong constant, dispatched on the branch then on main, instead of a new workflow input"
    - "Read the Flux image bump and the pod image tag before attributing a probe result: the push rolls both app environments after the deploy run ends"

key-files:
  created:
    - .planning/workstreams/architecture-views/phases/04-leak-review-and-production-go-live/04-05-live.txt
  modified:
    - .github/workflows/uptime-check.yml
    - docs/INFRA_RUNBOOK.md
    - docs/learning/09-build-quality-and-ci.md

key-decisions:
  - "Q4 1 acted on: pushed the probe change with plan 04's docs commit by explicit SHA f4a11fb, then drilled on GitHub"
  - "No new workflow input for the site drill: the wrong marker lives on a throwaway branch, so the worst case stays 6 URLs"

requirements-completed: [PUB-04, PUB-06, PUB-05]

coverage:
  - id: D1
    description: "The uptime probe covers the architecture site root with a body marker and a timeout that covers it; a main run after the push is green with the site OK"
    requirement: PUB-04
    verification:
      - kind: integration
        ref: "04-05-live.txt UPTIME baseline run=40936698753 conclusion=success (site_line OK, 200, marker present); LOCAL_PROBE result=pass"
        status: pass
    human_judgment: false
  - id: D2
    description: "A wrong site marker turns the probe red naming only the site URL as 200 without the expected marker, opens the alert issue, and the restored entry turns it green and closes the issue"
    requirement: PUB-04
    verification:
      - kind: integration
        ref: "04-05-live.txt DRILL run=40936850782 conclusion=failure, ISSUE opened=23, RECOVERY run=40937007797 conclusion=success, ISSUE 23 state=CLOSED"
        status: pass
    human_judgment: false
  - id: D3
    description: "On the production hostname a cold /view/signin-ok/ and its reload both answer 200 and render SVG nodes, a missing /assets/*.js is 404 text/html, robots.txt disallows everything, and the root address is linked on origin/main"
    requirement: PUB-06
    verification:
      - kind: integration
        ref: "04-05-live.txt DEEPLINK nav_status=200 svg_nodes=185, REFRESH nav_type=reload status=200 185/185, MISSING_ASSET status=404, ROBOTS disallow=yes, LINKS readme=1 infra_architecture=1"
        status: pass
    human_judgment: false
  - id: D4
    description: "robots.txt keeps Disallow: / on the production host"
    requirement: PUB-05
    verification:
      - kind: integration
        ref: "04-05-live.txt ROBOTS disallow=yes body=User-agent: * / Disallow: /"
        status: pass
    human_judgment: false

duration: 27min
completed: 2026-10-29
status: complete
---

# Phase 4 Plan 05: Uptime probe, drill and production URL checks Summary

**The standing uptime probe now checks the architecture site root for its LikeC4 shell marker; a wrong-marker drill on GitHub turned it red for the site alone and opened issue 23, the next main run closed it, and the trusted production hostname serves the deep link, its reload, a 404 for a missing asset and `Disallow: /`.**

## Performance

- **Duration:** 27 min
- **Tasks:** 3 of 3
- **Q4 branch taken:** 1 (push and drill on GitHub), owner reply "Q3 1, Q4 1"

## Accomplishments

- Added `SITE_URL`, `SITE_MARKER`, `run_check "$SITE_URL" "$SITE_MARKER"`, `timeout-minutes: 10` with the 400 s and 480 s arithmetic, the new step name and known hole (f) to uptime-check.yml (b99da2b). The old `fifth URL` wording is gone. A local run of the probe step printed `OK (200, marker present)` for the site and `result=pass` before anything was pushed.
- Runbook "Uptime alert triage" lists the site and gains an "Architecture site failure" paragraph; the CI learning doc names the site in both places (f4a11fb).
- Pushed 5 commits by explicit SHA f4a11fb (class (a): b99da2b, f4a11fb, 44a7976; class (b): adc5042, a8afc04; no merge needed because origin had no new commits at push time). Deploy run 40935047275, Invariant Checks 40935047328 and Secret Scan 40935047415 all concluded success.
- Flux then bumped both overlays to main-163-f4a11fb (fluxcdbot 918f8bf nonprod, 2efee74 prod). The Recreate rollout showed both health endpoints at 503 at 19:03:34Z and 200 at 19:04:06Z. The baseline was dispatched only after that, so the green is attributable.
- Baseline UPTIME run 40936698753: success, site `OK (200, marker present)`.
- Drill: branch uptime-site-drill (47899c8, marker `id="likec4-root-drill"`). Run 40936850782 concluded failure; the four other URLs logged OK, the report held one line (the site, `200 without the expected marker`), issue 23 `Uptime check failing` opened at 19:05:54Z. Recovery run 40937007797 on main concluded success and closed issue 23 at 19:06:33Z. Branch deleted on origin and locally; open `uptime-alert` issues: 0.
- Production hostname (no `-k`): deep link and reload figures below.

| Measure | Staging (03-06-live.txt) | Production (this plan) |
|---|---|---|
| Cold deep link `/view/signin-ok/` | nav_status=200, svg_nodes=184 | nav_status=200, svg_nodes=185 |
| Reload of the deep link | not measured | nav_type=reload, status=200, svg nodes 185 before and 185 after |
| Missing `/assets/missing.js` | 404 text/html | 404 text/html |
| robots.txt | Disallow yes | `User-agent: *` / `Disallow: /` |
| Links on origin/main | n/a | README.md 1, docs/INFRA_ARCHITECTURE.md 1 |

## Task Commits

1. **Task 1: probe change, docs, push, baseline** - b99da2b (feat, full hook passed), f4a11fb (docs), 910f28f (record)
2. **Task 2: drill** - 47899c8 on the throwaway branch only (never merged, branch deleted); the record is in 7e0f4c9
3. **Task 3: production URL measurements** - 7e0f4c9 (record)

## Deviations from Plan

None in behavior. Two things worth knowing:

- The plan's `grep -c 'run_check "$SITE_URL" "$SITE_MARKER"'` printed 0 here while `grep -c -F` printed 1 for the same text on line 169; in this shell `grep` is not a plain POSIX BRE grep, so the `$` anchored. The fixed-string form is the one recorded as passing.
- The refresh ran as `out/refresh.py` through shot-scraper's interpreter, as the plan says; scratch files stay under the ignored `out/`.

## Authentication Gates

None.

## Issues Encountered

- A heredoc append to the record was blocked by the repo's tooling hook; the record was rewritten with the Edit and Write tools instead.

## Known Stubs

None.

## Threat Flags

None. The only new probed surface is the public site root; the report carries URL and status only (T-04-21).

## Unverified

- The two alert emails (issue open and close) were not observed; only the GitHub issue state was read.
- The probe still proves only that the root serves the LikeC4 shell, not that hashed assets load (known hole (f), documented in the workflow header).
- `.planning/` untracked leftovers from other work (quick tasks, milestone.lock) were left alone.

## Self-Check: PASSED

Checked: uptime-check.yml timeout 10, `SITE_MARKER` `id="likec4-root"`, 400 s and 480 s literals 1 each, `fifth URL` 0, `run_check` line present once, `verify-comments.py check` OK, plan's record greps 3, 1 and 2 matches, branch gone locally and on origin, 0 open `uptime-alert` issues, commits b99da2b f4a11fb 910f28f 7e0f4c9 on main.
