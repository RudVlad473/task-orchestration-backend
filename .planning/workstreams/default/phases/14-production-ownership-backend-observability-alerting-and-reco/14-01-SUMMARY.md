---
phase: 14-production-ownership-backend-observability-alerting-and-reco
plan: 01
subsystem: observability
tags: [actuator, micrometer, prometheus, podmonitor, spring-security, k8s-probes, uptime-check]

requires:
  - phase: 13
    provides: k3s cluster with Flux, kube-prometheus-stack in namespace monitoring, PodMonitor CRD
provides:
  - Prometheus metrics on a management port (9081) that no Service or IngressRoute routes
  - Public health check at /api/healthz (db-only group, no details)
  - Kubernetes probes on the named container port management
  - PodMonitor app scraping kanban-prod and kanban-nonprod (not yet live: Task 3 not executed)
affects: [14-02 alert rules, uptime-check paging]

actuals:
  tokens: 8420
  tasks: 2
  commits: 2

tech-stack:
  added: [io.micrometer:micrometer-registry-prometheus 1.15.12 (BOM-managed)]
  patterns: [separate management port with its own stateless security chain, named container port referenced by a PodMonitor]

key-files:
  created: []
  modified:
    - build.gradle
    - gradle/verification-metadata.xml
    - src/main/resources/application.properties
    - src/main/resources/application-test.properties
    - src/main/java/com/vrudenko/kanban_board/constant/ApiPaths.java
    - src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java
    - src/test/java/com/vrudenko/kanban_board/security/ActuatorHealthE2ETest.java
    - src/test/java/com/vrudenko/kanban_board/security/ResetEndpointProfileGatingTest.java
    - k8s/base/app/app.yaml
    - k8s/monitoring/configs/podmonitors.yaml
    - .github/workflows/uptime-check.yml
    - scripts/verify-comments.py

key-decisions:
  - "Management port 9081 with public health at /api/healthz; the old /api/actuator/health path is retired (user decision, option-a)"
  - "managementSecurityFilterChain is order 0, matched by EndpointRequest.toAnyEndpoint(), stateless, permitAll"

requirements-completed: [D-01]

commits: 2
plan_head_before: acb0d86b10f694d2fbccb365de4fee7532f24fcd
plan_head_after: f3ec16b0167339570734c1523ca6f544534ed1de

duration: 35min
completed: 2026-10-16
status: partial
---

# Phase 14 Plan 01: Management-port metrics and /api/healthz Summary

**Code, tests, manifests and PodMonitor for Prometheus metrics on an unrouted management port (9081) and a db-only public health check at /api/healthz are committed and green locally; the gated live rollout (Task 3) was NOT executed.**

## Status: partial, Task 3 pending operator

Tasks 1 and 2 are done. Task 3 (Flux suspend, push, staged resume, live checks) needs `ssh netcup-prod`, Flux suspend/resume and a push to origin/main, which are production-affecting and outside a worktree executor's authority. Nothing was pushed, no Kustomization was suspended, and no live check was run. Every "live" truth in the plan's must_haves (Prometheus `up{container="app"}`, public 200/401 answers on the hostnames, uptime-check green, Flux resumed) is therefore UNVERIFIED.

`requirements-completed: [D-01]` is copied from the plan frontmatter as the template requires, but D-01 is only satisfied once Task 3 runs. The orchestrator should not mark it complete on this SUMMARY alone.

## Decisions

- **Task 1 (checkpoint:decision), resolved by the user in the live conversation: option-a.** Management port 9081, public health at /api/healthz, old path /api/actuator/health retired. The user named no external monitor outside this repo that polls the old URL.

## Tasks

| Task | Name | Commit | Status |
| ---- | ---- | ------ | ------ |
| 1 | Confirm management-port shape and new public health path | none (decision) | resolved: option-a |
| 2 | Tracer: metrics on the unrouted management port, /api/healthz, probes, PodMonitor | f3ec16b | done, full pre-commit hook passed |
| 3 | Gated rollout | none | NOT executed, see "Task 3 handoff" |

Extra commit (deviation, below): 41350d4 chore(14-01) exclude the Serena config tree from the comment lint.

## Task 2: TDD evidence

The plan commits once ("one commit holds ..."), and the pre-commit hook runs fastTest, which would reject a commit holding a failing test, so RED was run and recorded but not committed on its own. Format caveat: `gsd_run check tdd-red-evidence` was not run (the plan is `type: execute`, TDD_MODE was not passed); the semantic assessment below is mine.

**RED** (new ActuatorHealthE2ETest against unchanged production code; build output at the time, 21 tests incl. arch tests, 5 failed, 8 target tests ran, context loaded fine):

| Case | Result before the change | Reason |
| ---- | ------------------------ | ------ |
| PublicHealthTest shouldReturnOk_whenHealthzCheckedWithoutSession | FAIL expected 200, was 401 | /healthz does not exist yet |
| PublicHealthTest shouldReportStatusUp_whenHealthzCheckedWithoutSession | FAIL expected "UP", was "401" | same |
| RetiredActuatorPathTest shouldReturnUnauthorized_whenRetiredHealthPathRequestedWithoutSession | FAIL expected 401, was 200 | old path still permitAll |
| ManagementPortTest shouldReportStatusUp_whenHealthCheckedOnManagementPort | FAIL expected 200, was 404 | no separate management port: LocalManagementPort resolves to the main port, where /actuator/health (no /api) does not exist |
| ManagementPortTest shouldExposeJvmMetrics_whenPrometheusScrapedOnManagementPort | FAIL expected 200, was 404 | prometheus not exposed |
| RetiredActuatorPathTest prometheus on routed port | PASS before and after | vacuous today (endpoint not exposed, so 401); guards the future |
| ManagementPortTest env endpoint | PASS before and after | same, guards the allowlist |
| PublicHealthTest no components / no details | PASS before the first edit | an error body satisfied the absence checks vacuously; fixed by also asserting 200 |

Semantic assessment: each failing case failed on the planned assertion (status code or body), not on setup, import or fixture. The three passing-before cases are regression guards, not RED evidence; I say so rather than count them.

**GREEN:** ActuatorHealthE2ETest 8/8 and ResetEndpointProfileGatingTest 2/2 pass after the change (JUnit XML read: tests=8, failures=0 across the three nested suites; tests=2, failures=0). The full pre-commit hook (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest) passed on f3ec16b.

**Failing direction of the two guard cases (mutation run, restored afterwards):** with the `@Bean` on managementSecurityFilterChain removed AND the old `/actuator/health` permitAll entry added back, one test run failed exactly these two cases:
- ManagementPortTest.shouldExposeJvmMetrics_whenPrometheusScrapedOnManagementPort: expected 200, was 401 (no management chain, the catch-all chain answers the management port).
- RetiredActuatorPathTest.shouldReturnUnauthorized_whenRetiredHealthPathRequestedWithoutSession: expected 401, was 500 (permitted path that no longer exists on the main port falls into the catch-all handler).

I ran the two mutations together, not one at a time. Each failure is attributable to one mutation by its message (401 on the management port is the missing chain; 500 on the routed port is the permit entry), but I did not isolate them in separate runs. SecurityConfiguration.java was restored from a byte copy and the committed diff was re-checked.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ResetEndpointProfileGatingTest hardcoded exactly one SecurityFilterChain**
- **Found during:** Task 2, the verification-metadata regeneration run (full suite, 555 tests, 1 failure)
- **Issue:** `should_registerNoResetSecurityChain_when_nonprodProfileIsInactive` asserted `hasSize(1)`; the new management chain makes it 2. Not listed in the plan's files.
- **Fix:** assert the exact bean names `securityFilterChain` and `managementSecurityFilterChain`, which keeps the intent (no reset chain outside nonprod).
- **Files modified:** src/test/java/com/vrudenko/kanban_board/security/ResetEndpointProfileGatingTest.java
- **Commit:** f3ec16b

**2. [Rule 3 - Blocking] verify-comments.py failed on pre-existing .serena/project.yml, blocking every hooked commit**
- **Found during:** Task 2, running the comment lint before commit
- **Issue:** the scaffold commit acb0d86 added `.serena/project.yml`, which carries 14 summary-first/narration violations. The pre-commit hook scans all tracked files, so it would refuse f3ec16b, and the project forbids `--no-verify` on Java/k8s commits.
- **Fix:** added `.serena/` to EXCLUDED_PREFIXES beside `.claude/`, `.agents/`, `.dev/` (tool-owned trees). `verify-comments.py check` now prints "OK: 303 files, 1191 comment blocks"; the selftest passes 44/44. Committed separately with `--no-verify` as a config-only commit (permitted by the project instructions).
- **Files modified:** scripts/verify-comments.py
- **Commit:** 41350d4
- **Note for the orchestrator:** this is outside the plan's file list and you said to report outside findings. I made it because it blocked the plan's own required commit, and kept it isolated so it can be dropped (then `.serena/project.yml` itself needs fixing, or main's hook stays red for everyone). Your call.

**3. [Rule 1 - Plan expectation] verification-metadata diff is not only micrometer and prometheus-metrics-***
- **Found during:** Task 2, diff review
- **Issue:** the plan allowed only micrometer-registry-prometheus and io.prometheus prometheus-metrics-*. The regeneration also added `io.prometheus:client_java:1.3.10` (parent POM) and four `io.opentelemetry` BOM entries (opentelemetry-bom, -alpha, opentelemetry-instrumentation-bom, -alpha; each a `.pom` and `.module`, no jar).
- **Assessment:** verified benign. client_java-1.3.10.pom imports opentelemetry-instrumentation-bom-alpha (pom line 347) and Gradle must hash the BOM chain to resolve the POM. All jars added are micrometer-registry-prometheus-1.15.12 and the six prometheus-metrics-* 1.3.10 jars. Version 1.15.12 matches the plan's note.
- **Also:** Gradle swapped the order of the two `<trust>` entries; I restored the original order so the javadoc entry's "the sources-jar entry above" stays true. Net diff is 98 added lines, 0 removed.

**Total deviations:** 3 (1 bug in an existing test, 1 blocking lint config, 1 plan-expectation mismatch). **Impact:** none on behaviour; deviation 2 widens the diff by one config commit.

## Verification run

| Check | Result |
| ----- | ------ |
| `./gradlew test --tests '*ActuatorHealthE2ETest' -x jacocoTestCoverageVerification` (plus ResetEndpointProfileGatingTest) | BUILD SUCCESSFUL, 8 + 2 tests, 0 failures |
| `bash scripts/verify-k8s-manifests.sh` | all 12 kustomization roots valid |
| `python3 scripts/verify-k8s-invariants.py --no-provisional` | "invariants OK -- 12 kustomization root(s) checked" (run with /usr/bin/python3: the mise python3 on PATH has no PyYAML; I did not install anything) |
| `python3 scripts/verify-comments.py check` | OK |
| `./gradlew spotlessCheck` | passed inside the pre-commit hook |
| Pre-commit hook on f3ec16b (gitleaks, comment lint, budget, spotless, fastTest) | passed, no `--no-verify` |
| Acceptance greps (9081, exposure list, additional-path, test port 0, EndpointRequest, @Order(0), HEALTHZ, containerPort, `port: management` count 3, no `/api/actuator/health` in k8s or workflows) | all as specified; `@Order(0)` initially matched two lines because of a Javadoc mention, reworded so it matches one |
| Rendered prod overlay (`kubectl kustomize k8s/overlays/prod`) | Service `app` lists ports `['http']` only; container ports http 8080 and management 9081; all three probes on `management` at /actuator/health |
| verification-metadata diff | micrometer-registry-prometheus + prometheus-metrics-* jars; extra POM-only entries explained in deviation 3 |

Not checked: the real runtime behaviour on k3s (kubelet probes on 9081, PodMonitor discovery, the `up` series labels). Those belong to Task 3.

## Task 3 handoff (NOT EXECUTED)

Nothing below ran. The operator or orchestrator must run it; it is quoted from the plan with the concrete SHAs.

**Preconditions (all must hold):**
1. The operator has approved pushing exactly these SHAs (project memory: ask before merging to main): `41350d4` and `f3ec16b`. `git log --oneline origin/main..HEAD` on this worktree branch lists exactly those two, with `acb0d86` already on origin/main. Present `git diff --stat origin/main..HEAD` for the approval.
2. Fast-forward local main to this branch: `git merge --ff-only worktree-agent-a58180f83aa01a75c` on main. The push must come from main's history. Then `git fetch origin` and re-check `git log --oneline origin/main..HEAD` lists only the approved commits.
3. `ssh netcup-prod` works (docs/INFRA_RUNBOOK.md, Access section).

**Why gated:** Flux applies the new base manifest (probes on 9081) as soon as it lands, but the image that listens on 9081 arrives only after CI builds it and fluxcdbot commits the tag. With Recreate, an old image under new probes would CrashLoop for that whole window.

**Steps:**
1. Suspend Flux over SSH in this order, recording the UTC time of each: Kustomization `flux-system` (root sync first, otherwise it re-applies `suspend: false` from git), then `apps-nonprod`, then `apps-prod`:
   `ssh netcup-prod "k3s kubectl -n flux-system patch kustomization <name> --type merge -p '{\"spec\":{\"suspend\":true}}'"`
2. Push by explicit SHA: `git push origin f3ec16b0167339570734c1523ca6f544534ed1de:main`. Watch the deploy workflow for that SHA (`gh run watch`) until build-and-push-docker-image succeeds. Wait for fluxcdbot's tag commit (`main-<run>-<sha7>`) on origin/main for both overlays, then `git fetch && git merge --ff-only origin/main` locally.
3. Resume `apps-nonprod` only. Wait for `k3s kubectl -n kanban-nonprod rollout status deploy/app --timeout=8m`. Check on the nonprod hostname: `/api/healthz` is 200 with `"status":"UP"`; `/api/actuator/health` and `/api/actuator/prometheus` are 401; `k3s kubectl -n kanban-nonprod get svc app -o jsonpath='{.spec.ports[*].name}'` prints only `http`; and
   `k3s kubectl get --raw '/api/v1/namespaces/monitoring/services/kps-prometheus:9090/proxy/api/v1/query?query=up%7Bcontainer%3D%22app%22%7D'` shows up=1 for kanban-nonprod. Record the full label set of that series (job, namespace, pod, container, env, instance): the later alert rules key on it.
4. If any nonprod check fails: leave `apps-prod` suspended, revert the commit, push the revert by SHA, resume `apps-nonprod`, confirm recovery, resume the rest, and stop with a report.
5. Resume `apps-prod` and repeat step 3's checks for kanban-prod and the prod hostname.
6. Resume the root `flux-system` Kustomization last. Confirm no Kustomization is suspended.
7. Dispatch the uptime workflow (`gh workflow run uptime-check.yml`) and confirm green (`gh run list --workflow uptime-check.yml -L 1 --json conclusion -q '.[0].conclusion'` prints `success`). Runs between the push and step 5 may be red (the old image answers 401 on /api/healthz); note them.

**Plan-level verify commands (from Task 3), to run at the end:**
- Public hosts: `for h in kanban-board-rud-vlad-473 kanban-board-rud-vlad-473-nonprod; do curl -fsS --max-time 20 "https://$h.duckdns.org/api/healthz" | grep -q '"status":"UP"' || exit 1; for p in actuator/health actuator/prometheus; do test "$(curl -s -o /dev/null --max-time 20 -w '%{http_code}' "https://$h.duckdns.org/api/$p")" = 401 || exit 1; done; done`
- Prometheus: `ssh netcup-prod "k3s kubectl get --raw '/api/v1/namespaces/monitoring/services/kps-prometheus:9090/proxy/api/v1/query?query=up%7Bcontainer%3D%22app%22%7D'" | jq -e '[.data.result[] | select(.value[1]=="1") | .metric.namespace] | sort == ["kanban-nonprod","kanban-prod"]'`
- Flux fully resumed: `ssh netcup-prod "k3s kubectl get kustomizations -A -o jsonpath='{range .items[?(@.spec.suspend==true)]}{.metadata.name}{\"\n\"}{end}'"` prints nothing.

**Record in the amended SUMMARY after the run:** suspend and resume timestamps (UTC), pushed SHAs, rolled-out image tags, each check's output, the label set of the `up` series, and that prod resumed only after nonprod passed.

## Threat Flags

None beyond the plan's threat model. The management port is reachable by any pod in the cluster (T-14-04, accepted in the plan: no NetworkPolicy in the app namespaces). T-14-01 is mitigated in code and tests (main-port prometheus 401, exposure allowlist, Service lists http only in the rendered overlay); its live half is Task 3.

## Findings outside this plan (not acted on)

- docs/AUTH_FLOWS.md (line 26, "the actuator health check are also permitAll") and docs/learning/06-security-and-sessions.md, 09-build-quality-and-ci.md, 11-observability.md still describe `/api/actuator/health` as the public check and say only `health` is exposed. docs/learning/11-observability.md also lists "No application metrics" as a gap. They are now stale.
- docs/INFRA_RUNBOOK.md and the docs wiki were not grepped for the old path beyond the rg listing at the start of Task 2; a follow-up should check them.
- The mise-managed `python3` on PATH has no PyYAML, so `python3 scripts/verify-k8s-invariants.py` (as written in the plan's verify) fails with "PyYAML is required" in this environment; `/usr/bin/python3` works.
- Gradle daemons were stopped twice mid-session ("stop command received"), consistent with a sibling worktree (`agent-a86a503cb1796e5d0`) running concurrently on a 8.7 GiB host (docs/SESSION_LESSONS.md lesson 9). A retry succeeded each time; no work was lost.

## Known Stubs

None.

## Self-Check: PASSED

- All 12 listed key-files exist in the worktree (`ls` returned every path).
- Both commits are ancestors of HEAD (`git merge-base --is-ancestor`), and `git rev-list --count acb0d86..HEAD` was 2 before this SUMMARY commit.
- STATE.md and ROADMAP.md do not appear in `git diff --name-only acb0d86..HEAD`.
- Not self-checked, because it did not run: everything under Task 3.
