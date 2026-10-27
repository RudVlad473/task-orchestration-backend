---
gsd_state_version: "1.0"
milestone: v1.3
current_phase: 14
current_phase_name: "Production ownership: backend observability, alerting and recovery"
status: executing
stopped_at: Phase 14 context gathered
last_updated: "2026-10-27T13:21:06.499Z"
last_activity: 2026-10-27
last_activity_desc: Phase 14 execution started
state_head: 106ed4a243dad05240a68cc89f83205982e7a511
progress:
  total_phases: 1
  completed_phases: 2
  total_plans: 13
  completed_plans: 24
milestone_name: Nonprod Environment & CI Hardening
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-08)

**Core value:** The backend is feature-complete against its own mock-ups and live in production; the differentiator now is proving the whole system — including a real frontend against a real deploy — is reliable.
**Current focus:** Phase 14 — Production ownership: backend observability, alerting and recovery

## Current Position

Phase: 14 (Production ownership: backend observability, alerting and recovery) — EXECUTING
Plan: 6 of 13 summarized (14-01 to 14-05, 14-07); next ready is 14-06 (needs human setup)
Status: Executing Phase 14
Last activity: 2026-10-27 - Completed quick task 261027-if9: Kruchten views built (tags, logical FKs, development imports, signin per outcome, process claims verified); verification human_needed (readability at natural size, CI drift job not run); earlier: Fast tasks: how to view the LikeC4 diagrams (LOCAL_DEV.md, viewer port corrected to 5173) and Zed tasks for the architecture model; earlier: quick task 261027-fuj (LikeC4 model moved to docs/architecture, drift gated in hook and CI)

## Performance Metrics

v1.0–v1.2 velocity/per-plan detail archived at milestone close — see `.planning/RETROSPECTIVE.md`'s Cost Observations per milestone and `.planning/milestones/v1.2-phases/` for individual plan SUMMARY.md files (each carries an `actuals` frontmatter block with tokens/tasks/commits). This section resets per milestone rather than accumulating a project-lifetime table.

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 11 P01 | 25min | 3 tasks | 6 files |
| Phase 11 P02 | 55min | 3 tasks | 1 files |
| Phase 11 P03 | 90min | 3 tasks | 3 files |
| Phase 11 P04 | 30min | 2 tasks | 1 files |
| Phase 11 P05 | 41min | 3 tasks | 2 files |
| Phase 11 P06 | 40min | 3 tasks | 2 files |
| Phase quick P260902-vjo | ~40min | 3 tasks | 2 files |
| Phase 12 P01 | 94min | 3 tasks | 6 files |
| Phase 12 P02 | 70min | 3 tasks | 6 files |
| Phase 12 P03 | 130min | 3 tasks | 5 files |
| Phase 12 P04 | 123min | 2 tasks | 5 files |
| Phase 12 P05 | 60min | 3 tasks | 2 files |
| Phase 12 P06 | 55min | 3 tasks | 7 files |
| Phase 13 P06 | ~59min window + ~30min post-window fixes/docs | 3 tasks | 23 files |
| Phase 13 P07 | ~2h40m | 2 tasks | 15 files |
| Phase 13 P08 | ~2h across two sessions | 3 tasks | 10 files |
| Phase 13 P09 | ~2.5h, one session | 3 tasks | 26 files |

## Accumulated Context

### Roadmap Evolution

- Phase 11 added: Migrate database from Neon to self-hosted Postgres
- Phase 12 added: Self-hosted observability stack
- Phase 13 added: Introduce Kubernetes
- Phase 14 added: Production ownership: backend observability, alerting and recovery

### Decisions

Decisions are logged in PROJECT.md's Key Decisions table and `.planning/RETROSPECTIVE.md`. The full per-plan decision log for v1.0–v1.3 was cleared at v1.3's close — preserved in `git log` and `.planning/milestones/v1.3-phases/`.

No active-milestone decisions pending — next milestone not yet scoped.

- [Phase null]: 260825-h7m: MapStruct name-based auto-mapping was sufficient to carry createdAt onto BoardFullResponseDTO with no explicit @Mapping needed
- [Phase 11]: Phase 11 plan 01: postgres service placed in docker-compose.prod.yml (not a dedicated third file), following the kanban-edge cross-project precedent
- [Phase 11]: Phase 11 plan 01: DB_JDBC_PARAMS dropped entirely (not softened) from both app services -- self-hosted target has no TLS listener
- [Phase 11]: Phase 11 plan 01: postgres mem_limit: 512m is an explicitly-labeled PROVISIONAL Iteration 0 baseline; plan 11-03 owns the measured floor
- [Phase 11]: Phase 11 plan 04: re-derived HikariCP pool for a same-host Postgres engine (min-idle 0->2, connection-timeout 30000->10000, idle-timeout/max-lifetime raised to Hikari defaults, keepalive-time 0->120000), dropped the DB_JDBC_PARAMS query-string override, and proved prepareThreshold=0 removal safe with 1028 observed server-side prepared-statement executions against a real instance
- [Phase 11]: Phase 11 plan 04: pg_prepared_statements is session-scoped and unusable for cross-session verification from a separate docker exec psql session -- used Postgres's own statement log (log_statement=all) instead, confirmed empirically before adopting
- [Phase 11]: Phase 11 plan 05: D-13 Task 1 resolved as Option A (one-off Flyway container over SSH), no D-03 exception taken
- [Phase 11]: Phase 11 plan 05: DB_HOST/DB_NAME moved from GitHub Environment secrets to variables so the resolved database name is provably visible, unmasked, in CI logs (DB_USER/DB_PASS remain secrets)
- [Phase 11]: Phase 11 plan 05: gate-still-bites evidence gathered via a live VM-local reproduction against a disposable scratch database rather than a throwaway-branch CI push
- [Phase 11]: 260829-ii3: ResetController's params-based two-route split (fullReset=true vs fullReset!=true) routed exactly as predicted; the plan's accepted-async-race assumption for activity_log/topic-offset assertions did not hold empirically and required bounding, not tightening
- [Phase 11]: 260902-vjo: Netcup SCP console triage guidance added to INFRA_RUNBOOK.md between Access and Firewall sections; new scheduled uptime-check.yml workflow (cron */15, workflow_dispatch, permissions: {}) probes both public health endpoints, proven to bite on non-200/unreachable/wrong-body branches locally
- [Phase 12]: D-03 amended: a login-path-scoped Caddy rate_limit zone is permitted (and required) on the Grafana hostname, distinct from an authentication gate
- [Phase 12]: node-exporter uses a host-root read-only bind mount + --path.rootfs/procfs/sysfs instead of network_mode:host/pid:host (forbidden by verify-compose-ports.py I2); node_network_* still leaks host interface topology via the sysfs bind, corrected in the compose comment post-deploy
- [Phase 12]: Phase 12 plan 02: general lesson for any future Loki deploy against an already-populated Docker host -- schema_config.from and reject_old_samples_max_age must both predate the oldest on-disk log backlog, not the deploy date; a third distinct too_far_behind per-stream ordering guard is an accepted operational characteristic, not something to configure around
- [Phase 12]: Phase 12 plan 02: .planning/config.json gained git.allow_default_branch_commits: true, making explicit this project's already-established branching_strategy: none convention
- [Phase 12]: Phase 12 plan 04: two real bugs found in a vendored postgres-exporter Grafana dashboard by diffing its PromQL against the live exporter's own /metrics output and against 12-03's actual scrape job name -- two metric renames (pg_replication_lag/pg_database_size) and one template-variable job-label mismatch; reusable playbook for any future vendored dashboard
- [Phase 12]: Phase 12 plan 04: 2 of 13 postgres-exporter dashboard panels (Max Connections, Shared Buffers) diagnosed as a Grafana 13.2.1 legacy singlestat panel-type rendering bug -- underlying data confirmed correct via direct /api/ds/query call -- accepted as a documented finding rather than fixed (would need a singlestat->stat panel-type migration, out of scope)
- [Phase 12]: Phase 12 plan 04: fixed an unrelated CI gap found mid-plan -- .github/workflows/deploy.yml's Caddyfile validation step was missing APP_DOMAIN_MONITORING (needed by 12-01's third Caddyfile block), silently failing production deploys since 12-01 merged
- [Phase 12]: Phase 12 plan 05: user explicitly authorized the agent to drive the restart-ladder memory

measurement directly over SSH to netcup-prod, rather than a human operator running it manually — Matches a documented prior precedent in docs/INFRA_RUNBOOK.md of agent-driven SSH sessions
authorized mid-session; the plan's own checkpoint:human-action was presented to the user as a
decision before proceeding, not bypassed

- [Phase 12]: Phase 12 plan 06: user authorized the agent to drive the caddy restart-ladder measurement over

SSH too, with explicit acknowledgment this ladder briefly interrupts live public production
traffic on every rung -- a materially higher-stakes decision than plan 12-05's authorization — Presented as its own fresh checkpoint decision rather than assumed to extend 12-05's blanket
authorization, given the real-traffic-interruption and Let's Encrypt cert-safety risk profile

- [Phase 13]: 13-05 resume session: fixed deploy.yml's caddy-reload-inode-bug (replaced `caddy reload` inside the running container with `docker compose up -d --force-recreate caddy`) before continuing 13-05 Task 3 rather than deferring it -- appleboy/scp-action's rename-based file replacement gives the new Caddyfile a new inode, but Docker's bind mount is resolved once at container start and kept pointing at the old one, so the old mechanism was fail-closed-looking but actually silently stale; force-recreate sidesteps the inode problem structurally and trades that for a fail-loud failure mode instead. Live-verified via a real production deploy (host/container Caddyfile md5sum match, ~1min container uptime confirming genuine recreation), not just a green CI job -- the same md5sum-comparison discipline that caught the bug in the first place.
- [Phase 13]: 13-05 Task 3's interrupted-executor resume: the prior session's worktree (`agent-a4f43b113ff7a0069`) had no live agent to SendMessage-resume, but was clean and un-reaped, so it was rebased onto current main and then discarded in favor of a fresh `isolation="worktree"` dispatch once the project's own dispatch-isolation guard rejected manually pointing an Agent() call at a pre-existing worktree path -- no work was lost since the manual worktree's commits were already on main.
- [Phase 13]: 13-06: production cutover approved and executed; a mid-window push-by-branch-tip incident caused ~11min public 502 with zero data loss, operator directed push-forward-not-rollback recovery
- [Phase 13]: 13-06: 5 real pre-existing gaps found and fixed live against the cluster for the first time (postgres readinessProbe syntax, cert-manager HelmRepository/HelmRelease namespace, missing monitoring IngressRoute, SSH fingerprint comparison, a CNI-readiness race) -- none introduced by the incident
- [Phase 13]: 13-07: operator-authorized git commit --no-verify used 5 of 8 times after confirmed, sustained host memory pressure killed every local Gradle JVM fork regardless of heap size; the one push that also touched scripts/ triggered CI's real Java build/test workflow, which passed green
- [Phase 13]: 13-07: a live-database GRANT CONNECT fix was applied directly against running Postgres, not through a manifest change -- the committed init script is correct for any future first boot; 13-06's incident recovery (drop/recreate kanban_prod/kanban_nonprod) silently reverted the grant it had already applied
- [Phase 13]: 13-08 Task 1 (checkpoint:decision): proceed -- both the rate-limit re-derivation and the KANBAN-INGRESS host firewall install, not the rate-limit-only fallback -- after reviewing the live INPUT allow-list and confirming Traefik as the cluster's only non-ClusterIP Service
- [Phase 13]: 13-08 Task 3: the off-box-probe-vs-counter-attribution proof method hit the identical structural gap quick task 260906-feq already solved for DOCKER-USER -- Netcup's console-only outer Cloud Firewall blocked the probed NodePort before packets reached the VM's own iptables, so an unmodified probe could never move the Layer 3 counter under test. Resolved the same way: operator opened a temporary, scoped console rule (this workstation's IP only, destination port 30104) so the probe reaches Layer 3 without bypassing the rule under test; DROP counter measurably moved 5 -> 10 packets, the probed NodePort itself still timed out
- [Phase 13]: 13-08 Task 3's commit: operator-authorized git commit --no-verify (fresh, this commit only) after the local pre-commit hook's fastTest step killed its own Gradle daemon twice under confirmed host-wide memory contention (~400Mi free, three concurrent Claude Code sessions plus unrelated processes) -- gitleaks had already scanned the exact staged diff clean on both failed attempts
- [Phase 13]: 13-09 Task 1 (checkpoint:decision): "all" -- pre-authorized by the operator before this session's dispatch, recorded rather than re-asked
- [Phase 13]: 13-09 Task 2: Prometheus adopted at 384Mi/288Mi (not the bare-passing 288Mi) -- the real floor sits between confirmed-passing 288Mi and confirmed-failing 256Mi (live OOM observed at session start, before any ladder action), and the whole-cluster scrape target (123,106 series vs. Plan 12-05's 18,523 app-only figure) means the Compose-era predecessor's cap was never a valid basis for this successor
- [Phase 13]: 13-09 Task 2: Grafana adopted at 768Mi/400Mi directly, bypassing its own ladder's bare-passing 384Mi -- repeated the exact under-measurement pattern Plan 12-05's own addendum already documented for this binary (synthetic burst passes at a value that OOMs under real sustained load), so the already-known corrected figure was adopted rather than re-discovering it live
- [Phase 13]: 13-09 Task 2: nonprod redpanda's request (320Mi) deliberately NOT lowered to match its calmer 24h peak (179.9MiB) -- 320Mi is anchored to a documented ballooned-backlog crash-loop incident (quick task 260911-gkz) that normal-load data cannot supersede
- [Phase 13]: 13-09 Task 2: found and filed (not fixed) a real, pre-existing bug the Flux resume surfaced -- edge and monitoring Kustomizations both define IngressRoute grafana, racing for ownership on reconcile; out of this plan's files_modified scope and a real architectural decision, so filed as a todo with full reproduction instead
- [Phase 13]: 13-09: two git-branching conflicts (commits needing to reach main for Flux's GitRepository to see them, this session prohibited from merging/pushing main itself) resolved by the operator merging cd2e98d and 06cf6c1 directly, once also reconciling a concurrent fluxcdbot image-bump commit that had landed on origin/main at the same time
- [Phase 13]: 13-09: operator-authorized git commit --no-verify used 3 times (once per task's commit -- cd2e98d, b764148, 06cf6c1), each freshly requested in-session under confirmed host-wide memory contention (~320-536Mi free, swap active); gitleaks scanned each exact staged diff clean before every use

### Pending Todos

- [2026-08-01] [tooling] Bump Java version from 21 to 25 (current LTS) — [todo file](.planning/todos/pending/2026-08-01-bump-java-version-from-21-to-25-current-lts.md)
- [2026-08-02] [backend] Enable virtual threads in Spring Boot config — [todo file](.planning/todos/pending/2026-08-02-enable-virtual-threads-in-spring-boot-config.md)
- [2026-08-03] [tooling] Evaluate NullAway for null-safety dataflow analysis — [todo file](.planning/todos/pending/2026-08-03-evaluate-nullaway-for-null-safety-dataflow.md)
- [2026-08-04] [backend] Explore an alert-service integration as a separate microservice — [todo file](.planning/todos/pending/2026-08-04-explore-alert-service-as-a-separate-microservice-for-tech-ex.md)
- [2026-08-06] [backend] D-02's replay-from-zero rationale is not delivered by non-transitive BACKWARD — [todo file](.planning/todos/pending/2026-08-06-d-02-backward-non-transitive-vs-replay-from-zero.md)
- [2026-08-10] [testing] Investigate refactoring existing near-duplicate tests to @ParameterizedTest — [todo file](.planning/todos/pending/2026-08-10-investigate-refactoring-existing-tests-to-parameterized.md)
- [2026-08-10] [backend] Kafka-sourced ActivityEvent fields are persisted with no re-validation and no producer-authentication bou… — [todo file](.planning/todos/pending/2026-08-10-kafka-activity-consumer-trusts-unvalidated-event-fields.md)
- [2026-08-11] [backend] ActivityAction enum lives in entity/ but is not a JPA entity — relocate to match its actual role — [todo file](.planning/todos/pending/2026-08-11-activityaction-enum-misplaced-in-entity-package.md)
- [2026-08-11] [backend] Add OpenAPI breaking-change detection to CI (schema diffing against a checked-in baseline) — [todo file](.planning/todos/pending/2026-08-11-add-openapi-breaking-change-detection-to-ci.md)
- [2026-08-11] [docs] Revisit Java code comment and JavaDoc verbosity policy — [todo file](.planning/todos/pending/2026-08-11-revisit-java-code-comment-and-javadoc-verbosity-policy.md)
- [2026-08-11] [backend] TaskMovedEvent carries no position, unlike the new ColumnReorderedEvent (fork D-E, resolved E1) — [todo file](.planning/todos/pending/2026-08-11-taskmovedevent-position-asymmetry-not-fixed-in-s5e-fork-d-e.md)
- [2026-08-11] [backend] activity_log has no retention policy, and its growth rate just materially accelerated — [todo file](.planning/todos/pending/2026-08-11-unbounded-activity-log-growth-rate-materially-accelerated-by-s.md)
- [2026-08-11] [backend] UpdateBoardRequestDTO.name's optionality rests on the same unexamined assumption D-02 declined to accep… — [todo file](.planning/todos/pending/2026-08-11-updateboardrequestdto-name-optionality-rests-on-same-unex.md)
- [2026-08-12] [infra] Add a nonprod/staging environment and wire Playwright E2E tests against real (non-mocked) deploys — [todo file](.planning/todos/pending/2026-08-12-add-nonprod-staging-environment-and-playwright-e2e-ci-gate.md)
- [2026-08-12] [backend] Four remaining ResponseEntity.created(URI.create(request.getRequestURI())) sites now diverge from s… — [todo file](.planning/todos/pending/2026-08-12-four-remaining-created-location-sites-now-diverge-from-signup.md)
- [2026-08-12] [backend] POST /signup's Location header names a URI with no GET handler (GET /users/me does not exist) — [todo file](.planning/todos/pending/2026-08-12-signup-location-header-points-at-a-uri-with-no-get-handler.md)
- [2026-08-13] [tooling] Ratchet dependencyCheckAnalyze's failBuildOnCVSS after a real baseline exists — [todo file](.planning/todos/pending/2026-08-13-ratchet-failbuildoncvss-after-a-real-dependency-check-baseline.md)
- [2026-08-13] [security] "Two independent session-concurrency enforcers coexist (filter-held, in-memory vs. bean, JDBC-backed)" — [todo file](.planning/todos/pending/2026-08-13-two-independent-session-ceiling-enforcers-coexist.md)
- [2026-08-16] [tooling] gsd-tools' state.record-metric (or equivalent) writes the wrong progress percent -- reproduced twice,… — [todo file](.planning/todos/pending/2026-08-16-gsd-tools-state-record-metric-writes-wrong-progress-percent.md)
- [2026-08-18] [security] 500 ProblemDetail response's `detail` carries the raw exception message — [todo file](.planning/todos/pending/2026-08-18-500-problemdetail-detail-carries-raw-exception-message.md)
- [2026-08-19] [ci] deploy.yml has no path filter, so docs-only pushes trigger a full production+nonprod deploy — [todo file](.planning/todos/pending/2026-08-19-deploy-yml-has-no-path-filter-so-docs-only-pushes-trigger-a-full-deploy.md)
- [2026-08-20] [security] "No rate limiting / volumetric brute-force guard on POST /signin" — [todo file](.planning/todos/pending/2026-08-20-add-rate-limiting-to-signin-to-bound-brute-force-volume.md)
- [2026-08-20] [security] "Container runs as root — no USER directive in the Dockerfile" — [todo file](.planning/todos/pending/2026-08-20-dockerfile-runs-as-root-no-user-directive.md)
- [2026-08-20] [security] "IDOR chain consistency: nested path segments (boardId, columnId) are never cross-checked against the le… — [todo file](.planning/todos/pending/2026-08-20-idor-same-user-chain-consistency-boardid-columnid-not-c.md)
- [2026-08-20] [security] "Internal Kafka / schema-registry hop has neither SASL auth nor TLS" — [todo file](.planning/todos/pending/2026-08-20-internal-kafka-hop-has-no-sasl-auth-or-tls.md)
- [2026-08-20] [ci] "No branch protection on master" — [todo file](.planning/todos/pending/2026-08-20-no-branch-protection-on-master.md) — Needs Enable GitHub branch protection on `master` (require PR review and/or the existing test/build/.
- [2026-08-20] [security] "No breached-password check or strength meter on signup" — [todo file](.planning/todos/pending/2026-08-20-no-breached-password-check-or-strength-meter-on-signup.md)
- [2026-08-20] [tooling] No CI check for stale Gradle dependency-verification metadata — [todo file](.planning/todos/pending/2026-08-20-no-ci-check-for-stale-gradle-verification-metadata.md)
- [2026-08-20] [backend] "No explicit Content-Type validation on REST endpoints" — [todo file](.planning/todos/pending/2026-08-20-no-content-type-validation-on-rest-endpoints.md)
- [2026-08-20] [security] "No formal data classification; no self-service data export/delete/consent capture" — [todo file](.planning/todos/pending/2026-08-20-no-data-classification-or-self-service-export-delete.md)
- [2026-08-20] [infra] "No documented backup / restore runbook for the production database" — [todo file](.planning/todos/pending/2026-08-20-no-documented-backup-restore-runbook-for-prod-db.md)
- [2026-08-20] [security] "No MFA / second-factor enrollment path" — [todo file](.planning/todos/pending/2026-08-20-no-mfa-second-factor-enrollment-path.md)
- [2026-08-20] [security] "No notification on auth-detail changes (credential reset, new-device login)" — [todo file](.planning/todos/pending/2026-08-20-no-notification-on-auth-detail-changes-credential-rese.md)
- [2026-08-20] [security] "No password-change capability exists anywhere in the API" — [todo file](.planning/todos/pending/2026-08-20-no-password-change-capability-exists-anywhere-in-api.md)
- [2026-08-20] [ci] "Production deploys get no automated post-deploy health verification (nonprod does)" — [todo file](.planning/todos/pending/2026-08-20-no-prod-post-deploy-health-verification.md)
- [2026-08-20] [infra] "No remote log shipping, no structured/UTC logging standard, no alerting on unusual activity" — [todo file](.planning/todos/pending/2026-08-20-no-remote-log-shipping-structured-logging-or-alerting.md)
- [2026-08-20] [security] "No secret pepper on top of BCrypt's own per-hash salt" — [todo file](.planning/todos/pending/2026-08-20-no-secret-pepper-on-top-of-bcrypt-salt.md)
- [2026-08-20] [security] "No secrets-vault for runtime production secrets; no stated key/secret rotation cadence" — [todo file](.planning/todos/pending/2026-08-20-no-secrets-vault-for-runtime-prod-secrets-no-rotation.md)
- [2026-08-20] [security] "No security-relevant event logging on the authentication / access-control paths" — [todo file](.planning/todos/pending/2026-08-20-no-security-event-logging-on-auth-and-access-control.md)
- [2026-08-20] [security] "No self-service session revocation, and no re-authentication before destructive actions" — [todo file](.planning/todos/pending/2026-08-20-no-session-revocation-or-reauth-before-destructive-act.md)
- [2026-08-20] [security] "Password composition regex blocks Unicode-only passwords and violates ASVS's no-composition-rules guidance" — [todo file](.planning/todos/pending/2026-08-20-password-composition-regex-blocks-unicode-only-pass.md)
- [2026-08-20] [security] "Password minimum/maximum length undersized against ASVS's bar" — [todo file](.planning/todos/pending/2026-08-20-password-min-max-length-undersized-against-asvs.md)
- [2026-08-20] [security] "No CSP anywhere; HSTS likely never emitted behind Caddy (no forwarded-headers-strategy); baseline head… — [todo file](.planning/todos/pending/2026-08-20-security-response-headers-csp-and-unreliable-hsts-behind.md)
- [2026-08-20] [security] "Swagger / OpenAPI docs are reachable in production with no profile gate" — [todo file](.planning/todos/pending/2026-08-20-swagger-openapi-docs-reachable-in-prod-no-profile-gate.md)
- [2026-08-20] [security] "CSRF defense reasoning is sound but only half-verified: no test proves an actual cross-origin request … — [todo file](.planning/todos/pending/2026-08-20-verify-csrf-defense-with-a-real-cross-origin-rejection-t.md)
- [2026-08-26] [tooling] Add mutation testing with PITest — [todo file](.planning/todos/pending/2026-08-26-add-mutation-testing-with-pitest.md) — Needs TBD — likely: add the `info.solidsoft.pitest` Gradle plugin to `build.gradle`,.
- [2026-09-03] [api] "A 429 from the edge rate limiter does not carry the RFC 7807 ProblemDetail envelope" — [todo file](.planning/todos/pending/2026-09-03-429-response-is-not-an-rfc-7807-problemdetail.md)
- [2026-09-04] [api] Allow editing a column's color after creation — [todo file](.planning/todos/pending/2026-09-04-allow-editing-a-columns-color-after-creation.md)
- [2026-09-04] [testing] Audit the codebase for places property-based testing (jqwik) would pay off — [todo file](.planning/todos/pending/2026-09-04-audit-codebase-for-property-based-testing-with-jqwik.md)
- [2026-09-05] [api] ecmaEquivalentOf does not fail closed on Java-only regex constructs — [todo file](.planning/todos/pending/2026-09-05-ecmaequivalentof-does-not-fail-closed-on-java-only-regex-constructs.md)
- [2026-09-06] [security] "IPv6 published ports have no host-level firewall — DOCKER-USER's new policy cannot reach this path" — [todo file](.planning/todos/pending/2026-09-06-ipv6-published-ports-have-no-host-level-firewall.md)
- [2026-09-07] [api] "Fix direct-@Pattern bypass and UTF-16 length-oracle blind spot in constraint customizer" — [todo file](.planning/todos/pending/2026-09-07-fix-direct-pattern-bypass-and-utf-16-length-oracle-blind-spo.md)
- [2026-09-07] [infra] "Groom the three phase-12 Grafana dashboards and expose selected ones as public links" — [todo file](.planning/todos/pending/2026-09-07-groom-grafana-dashboards-and-expose-public-links.md)
- [2026-09-08] [infra] "SCP'd config changes do not reach the running container without a restart (Grafana datas… — [todo file](.planning/todos/pending/2026-09-08-scp-d-config-changes-do-not-reach-the-running-container-without-a-restart.md)
- [2026-09-24] [infra] "Netcup VPS shows intermittent TCP retransmits and a bitrate dip under sustained iperf3 throughp… — [todo file](.planning/todos/pending/2026-09-24-netcup-vps-intermittent-tcp-retransmits-under-sustained-throughput.md)
- [unknown] [general] Untitled — [todo file](.planning/todos/pending/2026-09-30-no-backups-of-current-production-data.md)
- [unknown] [general] Untitled — [todo file](.planning/todos/pending/2026-09-30-phase-13-review-warnings-wr01-wr06.md)
- [unknown] [general] Untitled — [todo file](.planning/todos/pending/2026-09-30-prove-kanban-ingress-drop-counter-after-docker-stop.md)
- [unknown] [general] Untitled — [todo file](.planning/todos/pending/2026-09-30-reproduce-two-source-ip-rate-limit-proof.md)
- [2026-10-06] [docs] "Learning chapters 00, 05, 06, 07, 08, 09, 10 and 11 still describe the Docker Compose / Caddy / Promtail deploy… — [todo file](.planning/todos/pending/2026-10-05-learning-guide-describes-the-compose-era-deployment.md)
- [2026-10-06] [api] "A multipart/form-data Content-Type with no boundary returns 500 INTERNAL_ERROR" — [todo file](.planning/todos/pending/2026-10-06-malformed-multipart-content-type-returns-500.md)
- [2026-10-06] [api-documentation] "ColumnResponseDTO.color is documented as a non-null string, but a column can come back with… — [todo file](.planning/todos/pending/2026-10-06-openapi-column-color-documented-non-null-but-returned-null.md)
- [2026-10-06] [api-documentation] "OpenAPI documents 200 for create operations that return 201" — [todo file](.planning/todos/pending/2026-10-06-openapi-does-not-list-the-201-created-responses.md)
- [2026-10-06] [api-documentation] "OpenAPI declares ProblemDetail.instance as format: uri, but the app returns a path" — [todo file](.planning/todos/pending/2026-10-06-openapi-error-instance-declared-format-uri-but-app-returns-a-path.md)
- [2026-10-06] [api-documentation] "OpenAPI request schemas omit the Password, AppEmail and name/title constraints, so schema-vali… — [todo file](.planning/todos/pending/2026-10-06-openapi-request-schemas-omit-the-validation-constraints.md)
- [2026-10-27] [docs] Build the Kruchten views the LikeC4 standard describes — [todo file](.planning/todos/pending/2026-10-27-build-the-kruchten-views-the-likec4-standard-describes.md)

### Blockers/Concerns

None open. All three carried into v1.3 (Redpanda memory floor, nonprod/production identity
confusion risk, `NVD_API_KEY` resolution failure) were resolved during Phases 8–10 — see
`.planning/milestones/v1.3-ROADMAP.md` and PROJECT.md's Key Decisions for detail.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260818-ied | Add CI-05 requirement to Phase 9: automate Avro schema registry registration for nonprod as part of the CI deploy pipeline, extending deploy.yml with a schema-registration step (mirroring CI-01's flyway-verify-nonprod/deploy-to-nonprod pattern) so production and nonprod schema registries stay in sync on every deploy without a manual step | 2026-08-18 | b985989 | [260818-ied-add-ci-05-requirement-to-phase-9-automat](./quick/260818-ied-add-ci-05-requirement-to-phase-9-automat/) |
| 260820-ecm | Resolve WINDOWS.md ledger items 3 and 7: record already-completed live verification proof in INFRA_RUNBOOK.md and mark both fixed | 2026-08-20 | b5a265a | [260820-ecm-resolve-windows-md-ledger-items-3-and-7-](./quick/260820-ecm-resolve-windows-md-ledger-items-3-and-7-/) |
| 260820-euc | Add paths-ignore filter to deploy.yml so docs-only pushes don't trigger a full production+nonprod redeploy; also close 7 stale pending todos already resolved by Phase 10 | 2026-08-20 | 95f61cc | [260820-euc-add-paths-ignore-filter-to-deploy-yml-so](./quick/260820-euc-add-paths-ignore-filter-to-deploy-yml-so/) |
| 260820-g3u | Iterate on 4 easy pending todos: docs/CODE_STYLE.md rule 4 correction, tag HistoricalActivityEventReconstructorTest as kafka, qualify bare print() static import in 4 controller tests, fix ResetServiceE2ETest flaky race; also found and fixed a real gap in gradle/verification-metadata.xml along the way | 2026-08-20 | df68443 | [260820-g3u-iterate-on-4-easy-pending-todos-docs-cod](./quick/260820-g3u-iterate-on-4-easy-pending-todos-docs-cod/) |
| 260820-giz | Audit penetration-testing/security coverage against OWASP API Security Top 10 (2023) — cited verdict for all 10 categories, 4 new gap todos filed (IDOR chain consistency, signin rate-limiting, security response headers, CSRF cross-origin test), originating todo closed with Resolution | 2026-08-20 | 6a77e54 | [260820-giz-audit-penetration-testing-and-security-c](./quick/260820-giz-audit-penetration-testing-and-security-c/) |
| 260820-iwo | File 19 new pending todos + enrich 6 already-filed todos from the ASVS 4.0.3 Level 2 audit (33-agent workflow, 253 requirements traced) — 12 moderate + 7 minor new todos filed, STATE.md Pending Todos refreshed (~26 → ~45 items) | 2026-08-20 | cdaf0b9 | [260820-iwo-file-19-new-pending-todos-and-enrich-6-a](./quick/260820-iwo-file-19-new-pending-todos-and-enrich-6-a/) |
| 260825-dfd | Add a createdAt timestamp to BoardEntity and expose it on BoardResponseDTO, populated once in BoardService.save() and shared with BoardCreatedEvent's timestamp | 2026-08-25 | 6aadda1 | [260825-dfd-add-a-createdat-timestamp-to-boardentity](./quick/260825-dfd-add-a-createdat-timestamp-to-boardentity/) |
| 260825-h7m | Add createdAt to BoardFullResponseDTO, carried through the nested GET /boards/{boardId}/full read via MapStruct name-based auto-mapping; extended the flat-equivalence test to cover board-level fields (name/version/createdAt), closing the untested gap that let createdAt reach production missing from the nested document | 2026-08-25 | 1dd30ea | [260825-h7m-add-createdat-to-boardfullresponsedto-ge](./quick/260825-h7m-add-createdat-to-boardfullresponsedto-ge/) |
| 260829-ii3 | In the nonprod reset endpoint, add a REQUIRED targeted-delete mode selected by a fullReset query param: POST /api/admin/reset?fullReset=true keeps the unconditional full reset unchanged, a bare POST with {"userIds": [...]} cascade-deletes only those users' boards/columns/tasks/subtasks and their own activity_log rows via a new ResetService.deleteUsers, reusing UserService.deleteById's existing cascade | 2026-08-29 | c29a32d | [260829-ii3-in-the-nonprod-reset-endpoint-add-a-requ](./quick/260829-ii3-in-the-nonprod-reset-endpoint-add-a-requ/) |
| 260902-vjo | Document Netcup console staleness triage in docs/INFRA_RUNBOOK.md and add a GitHub Actions cron uptime check for both public health endpoints | 2026-09-02 | 4613746 | [260902-vjo-document-netcup-console-staleness-triage](./quick/260902-vjo-document-netcup-console-staleness-triage/) |
| 260904-obv | add 'color' field to column and accept it on column creation | 2026-09-04 | 50d4aca | [260904-obv-add-color-field-to-column-and-accept-it-](./quick/260904-obv-add-color-field-to-column-and-accept-it-/) |
| 260905-qxi | add a CI invariant (scripts/verify-compose-ports.py + self-test) that only caddy may publish host ports 80/443 in the two deployed compose files, wire it into invariant-checks.yml, and correct docs/INFRA_RUNBOOK.md's false "both layers enforce identical policy" firewall claim | 2026-09-05 | 566e725 | [260905-qxi-add-ci-invariant-that-only-caddy-may-pub](./quick/260905-qxi-add-ci-invariant-that-only-caddy-may-pub/) |
| 260905-tw0 | Infra diagrams: pin a digest-pinned mermaid-cli renderer (scripts/render-diagrams.sh + render-manifest.tsv) and measure drift on all nine committed diagrams (7 pre-existing drift 6-11% in height, filed as a todo); add four flowchart layout rules with deciding tests to docs/DIAGRAM_CONVENTIONS.md; add an inbound packet-path Scenario to docs/INFRA_ARCHITECTURE.md with DOCKER-USER drawn empty and dated 2026-09-05; number trust boundaries [1]-[5] on the Physical view incl. the external Netcup Cloud Firewall; extend the Maintenance Note file list | 2026-09-05 | 2b0bd7b | [260905-tw0-infra-diagrams-pin-a-committed-mmd-png-r](./quick/260905-tw0-infra-diagrams-pin-a-committed-mmd-png-r/) |
| 260906-feq | Fill the empty DOCKER-USER iptables chain with a version-controlled default-drop policy for Docker-published ports (infra/vm/docker-user-firewall.sh + systemd unit, PartOf=docker.service), reviewed three ways (Claude/Gemini/Codex) pre-execution which caught a checkpoint that silently defaulted to rebooting production and other real gaps, proved via off-box canary probe + packet-counter attribution + a genuine VM reboot, and corrected the runbook/architecture-doc/diagram that had described the chain as empty since 2026-09-05; IPv6 gap left open and filed as a new todo per operator decision | 2026-09-06 | f47f912 | [260906-feq-docker-user-iptables-rules-on-the-vm-sys](./quick/260906-feq-docker-user-iptables-rules-on-the-vm-sys/) |
| 260908-dl3 | Let POST /api/boards optionally accept a caller-supplied board id, validated against RandFlakeGenerator's real base36 format, uniqueness-checked before insert, and rejected 409 when taken; corrected `.claude/CLAUDE.md`'s stale ULID claim to the actual base36 scheme | 2026-09-08 | cb38532 | [260908-dl3-create-board-endpoint-optionally-accepts](./quick/260908-dl3-create-board-endpoint-optionally-accepts/) |
| 260908-mtl | Rename the three phase-12 vendored Grafana dashboards to human-readable titles (VM Host Metrics, Per-Container Resource Usage, Postgres Internals) and re-scope the grooming todo to piece 2 only; plan fan-out-reviewed (Claude+agy+Codex) pre-execution, which caught and fixed a false claim about Grafana rewriting stale URL slugs (it doesn't — verified live) and a mis-citation of one of two precedent todos | 2026-09-08 | ba259cb | [260908-mtl-groom-the-three-phase-12-grafana-dashboa](./quick/260908-mtl-groom-the-three-phase-12-grafana-dashboa/) |
| 260908-r16 | Restructure the Postgres Internals dashboard's 23 panels from one crammed "Global Statistics" row into five expanded topic rows (Health & Availability, Connections, Query Performance, Storage & I/O, Locks) via a reviewable Python codemod, rename cAdvisor's title again to "CPU/Memory & Network Usage - cAdvisor", and supersede the now-stale title claim on the originating todo; verified against a real disposable Grafana container plus a live browser screenshot confirming the visual fix | 2026-09-08 | ac12fc3 | [260908-r16-restructure-the-postgres-internals-grafa](./quick/260908-r16-restructure-the-postgres-internals-grafa/) |
| 260908-sj9 | Discovered live (checking why a fresh public Grafana link showed stale content): deploy-to-netcup's SCP step never transferred 4 of 6 repo-relative bind mounts docker-compose.prod.yml declares (grafana provisioning, prometheus.yml, loki-config.yaml, promtail-config.yaml) — production had served Sep-7 hand-copied config behind 14 green deploys. Fixed the source list, added scripts/verify-deploy-scp-coverage.py (proven to fail against the pre-fix string, naming all four paths, and pass against the fix) wired into invariant-checks.yml, recorded the evidence in INFRA_RUNBOOK.md, and filed a todo for the separate remaining gap (3 of 4 consumers need a restart to apply transferred config, not just a copy) | 2026-09-08 | d433360 | [260908-sj9-fix-deploy-yml-add-docker-grafana-provis](./quick/260908-sj9-fix-deploy-yml-add-docker-grafana-provis/) |
| 260927-ryo | Onboarded the karpathy-llm-wiki skill (vendored by pinned upstream SHA at `.claude/skills/karpathy-llm-wiki/`, not the deprecated npm installer) and migrated docs/ into its raw/wiki model, rooted under docs/: 20 living-reference docs became docs/wiki/ articles, 51 dated/point-in-time docs (history, incidents, backend-modernization plans, the mockup gap snapshot) became docs/raw/ sources. An independent verify_migration.py checks completeness, header shape, link equivalence (1,717 rebased links) and originals-untouched; three deliberate corruptions proved the gate catches real regressions before recovering. Originals under docs/ are unmodified and stay authoritative until an operator-reviewed follow-on deletes them; nothing was pushed (this repo is inside 13-09's D-08 24h observation window, ending 2026-09-28T10:44:46Z, and a push would trigger a mid-window Flux rollout). Full account in `.planning/quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/260927-ryo-SUMMARY.md` | 2026-09-27 | 173614b | [260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith](./quick/260927-ryo-onboard-the-karpathy-llm-wiki-skill-gith/) |
| 260911-gkz | Root-caused all 7 dead "Postgres Internals" dashboard tiles via a live 4-layer probe (SSH to netcup-prod + Grafana's /api/ds/query with a viewer credential, substituting for an unavailable browser MCP tool): 2 tiles had disabled exporter collectors, 2 hit an already-documented Grafana 13.2.1 singlestat rendering bug, 2 (really 9 panels) hit a collapsed-row exact-match datname matcher. User approved all 4 fixes at the plan's checkpoint; landed as docker-compose.prod.yml + dashboard-JSON changes verified against disposable local containers only — the pg_stat_statements fix's production Postgres restart is explicitly deferred to a separate, later, gated deploy. A second, independent exporter-version/dashboard metric-name mismatch was found and fixed along the way | 2026-09-11 | 2ad3656 | [260911-gkz-debug-why-several-grafana-postgres-inter](./quick/260911-gkz-debug-why-several-grafana-postgres-inter/) |
| 261005-o6t | Trim all comments to the code-comments rubric and add a comment lint | 2026-10-05 | 7f4d52d | [261005-o6t-trim-and-format-all-comments-per-fan-out](./quick/261005-o6t-trim-and-format-all-comments-per-fan-out/) |
| 22 | 261005-sun | 2026-10-06 | 2f460ed | — |
| 261006-dpq | Make the OpenAPI doc Schemathesis-fuzzable: declare all path-template params, hide @CurrentUserId, add completeness test | 2026-10-06 | bf7323e | [261006-dpq-fix-openapi-doc-so-it-is-schemathesis-fu](./quick/261006-dpq-fix-openapi-doc-so-it-is-schemathesis-fu/) |
| 261006-fby | Ban Javadoc HTML tags and {@ inline tags in Java comments: lint rule, formatJavadoc(false), comment-only sweep | 2026-10-06 | 629a0d6 | [261006-fby-ban-javadoc-html-tags-and-inline-tags-in](./quick/261006-fby-ban-javadoc-html-tags-and-inline-tags-in/) |
| 261006-guz | Fix 500 on PUT /boards/{boardId} with a version-only body: null name means no rename | 2026-10-06 | 38881c9 | [261006-guz-fix-500-on-put-boards-boardid-with-a-ver](./quick/261006-guz-fix-500-on-put-boards-boardid-with-a-ver/) |
| 261006-i3k | Add a report-only Schemathesis fuzz script and weekly/dispatch workflow with a measured baseline | 2026-10-06 | c5079e8 | [261006-i3k-add-a-report-only-schemathesis-fuzzing-s](./quick/261006-i3k-add-a-report-only-schemathesis-fuzzing-s/) |
| 261006-jh9 | Trim .claude/CLAUDE.md from 36.8 KB to 4.6 KB and add a byte-ceiling gate on always-loaded instruction files | 2026-10-06 | 7b23313 | [261006-jh9-trim-claude-claude-md-and-add-a-size-cei](./quick/261006-jh9-trim-claude-claude-md-and-add-a-size-cei/) |
| 261006-kpj | Split docs/CODE_STYLE.md (43,974 to 9,345 bytes) into a numbered index, docs/CODE_REVIEW_RUBRIC.md and an ARCHITECTURE test section; nine new ArchUnit rules (MainCodeStyleArchTest, TestCodeStyleArchTest) | 2026-10-06 | fc86152 | [261006-kpj-split-docs-code-style-md-enforce-rules-w](./quick/261006-kpj-split-docs-code-style-md-enforce-rules-w/) |
| 261016-ekm | Investigate WSL memory crash; cap Gradle daemon heap | 2026-10-16 | 8f804bd | [261016-ekm-investigate-wsl-memory-crash-are-tests-t](./quick/261016-ekm-investigate-wsl-memory-crash-are-tests-t/) |
| 261016-lcu | Fix Phase 14 plan review BUG-1..7 and build.gradle comment violations | 2026-10-16 | 388fcc6 | [261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui](./quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/) |
| 261026-nr7 | Fix five verified fan-out review findings for phase 14: count a rejected publish, cache a failed cluster-id lookup (4s to 2s per send), bound the test admin client, count task creates and moves after commit, make the k8s invariants gate check CronJobs; unpushed | 2026-10-26 | 20bb56b | [261026-nr7-fix-five-fan-out-review-findings-for-pha](./quick/261026-nr7-fix-five-fan-out-review-findings-for-pha/) |
| 261026-ovp | Address review-tests findings: fix 5 cannot-fail tests, delete 5 duplicates, drop 2 shape pins, add mutation-proven tests (tests only) | 2026-10-26 | ef63a29 | [261026-ovp-address-review-tests-findings-fix-5-m-ca](./quick/261026-ovp-address-review-tests-findings-fix-5-m-ca/) |
| 261027-f8q | Write the approved LikeC4 diagramming standard into docs/DIAGRAM_CONVENTIONS.md after verifying six unverified dependencies | 2026-10-27 | 3abab0a | [261027-f8q-write-the-approved-likec4-diagramming-st](./quick/261027-f8q-write-the-approved-likec4-diagramming-st/) |
| 261027-fuj | Move the LikeC4 model and drift check to docs/architecture and wire it into pre-commit and invariant-checks CI | 2026-10-27 | de30039 | [261027-fuj-move-the-likec4-model-and-drift-check-to](./quick/261027-fuj-move-the-likec4-model-and-drift-check-to/) |
| 35 | Document how to view the LikeC4 diagrams in LOCAL_DEV.md and correct the viewer port (5173, not 61000) | 2026-10-27 | d39803e | — |
| 36 | Add Zed tasks to view, export, check and sync the LikeC4 architecture model (.zed/tasks.json) | 2026-10-27 | c9e4e15 | — |
| 261027-if9 | Build the Kruchten views the LikeC4 standard describes | 2026-10-27 | 106ed4a | [261027-if9-build-the-kruchten-views-the-likec4-stan](./quick/261027-if9-build-the-kruchten-views-the-likec4-stan/) |

## Deferred Items

Items acknowledged and carried forward (full v1.2-close table in `.planning/milestones/v1.2-ROADMAP.md`):

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| Epics | Modernization Epics 3 (OpenAPI half), 4, 6, 7 (Redis, Observability, K8s) | Deferred to future milestones | 2026-07-31 | v1.2 |
| Kafka | Cursor/keyset pagination on activity feed (PAGE-V2-01) | Deferred to v2 | 2026-08-01 | v1.2 |
| Infra Polish | INFRA-V2-01..03 (observability stack, blue-green deploys, multi-broker Redpanda HA) | Deferred to v2 | 2026-08-03 | v1.2 |
| Schema Registry Polish | SCHEMA-V2-01..02 (pre-merge schema-compatibility CI check, compatibility-mode rationale doc) | Deferred to v2 | 2026-08-03 | v1.2 |
| Frontend coupling | FRONTEND-DISPATCH-V2, FRONTEND-COUPLING-V2 | Deferred — hard-blocked on the frontend repo existing | 2026-08-17 | v1.2 |
| Nonprod | Per-PR ephemeral environments, ephemeral Neon branch per E2E run, second Neon project | Rejected for v1.3 with written rationale (REQUIREMENTS.md Out of Scope) | 2026-08-17 | v1.2 |
| Quick task | 260801-p03, 260802-rq5, 260802-ryf (missing/research-only summaries) | Open since v1.1 close | 2026-08-17 | v1.2 |
| quick_tasks | 260801-p03-add-explicit-comments-to-taskservice-upd | missing | 2026-08-25 | v1.3 |
| quick_tasks | 260802-rq5-bump-java-version-from-21-to-25-current- | missing | 2026-08-25 | v1.3 |
| quick_tasks | 260802-ryf-enable-virtual-threads-in-spring-boot-co | missing | 2026-08-25 | v1.3 |
| quick_tasks | 260816-hn1-wire-up-secret-scanning-gitleaks-truffle | unknown | 2026-08-25 | v1.3 |
| quick_tasks | 260824-v7n-install-docker-natively-inside-this-wsl2 | missing | 2026-08-25 | v1.3 |
| todos | 45 pending todos (full list: `.planning/todos/pending/`) | (presence-only) | 2026-08-25 | v1.3 |

**Known verification overrides: 14 total (recorded at v1.2 close); 50 newly acknowledged, 0 carried forward from a prior close, at v1.3 close (see above)**

**v1.3 close notes on the 5 flagged quick tasks:**

- `260801-p03`, `260802-rq5`, `260802-ryf` — pre-existing gap, unchanged since v1.1/v1.2 close (see rows above).
- `260816-hn1` (secret scanning) — false positive: fully complete with both PLAN.md and SUMMARY.md; flagged only because its `duration` frontmatter field is free text ("unknown (resumed mid-execution...)") rather than a parseable value.
- `260824-v7n` (install Docker natively inside WSL2) — genuinely empty, abandoned directory (scaffolded 2026-08-24, never executed); unrelated local dev-environment tooling, not a v1.3 requirement.

The 46 pending todos are individually listed and categorized in this document's own Pending Todos section above (security/CI/minor backlog, all explicitly out of v1.3 scope per PROJECT.md).

## Session Continuity

Last session: 2026-10-15T15:33:44.910Z
Stopped at: Phase 14 context gathered
Resume file: .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-CONTEXT.md

## Operator Next Steps

- Start the next milestone with /gsd-new-milestone
