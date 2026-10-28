---
phase: 02-model-completion
reviewed: 2026-10-28T00:00:00Z
depth: standard
files_reviewed: 13
files_reviewed_list:
  - docs/DIAGRAM_CONVENTIONS.md
  - docs/architecture/drill-output.txt
  - docs/architecture/drill.fish
  - docs/architecture/model/delivery.c4
  - docs/architecture/model/edge.c4
  - docs/architecture/model/model.c4
  - docs/architecture/model/monitoring.c4
  - docs/architecture/model/scenarios-app.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/model/spec.c4
  - docs/architecture/model/views.c4
  - docs/architecture/sync.mjs
  - scripts/pre-commit-likec4-selftest.py
findings:
  critical: 0
  warning: 9
  info: 5
  total: 14
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-10-28
**Depth:** standard
**Files Reviewed:** 13
**Status:** issues_found

## Summary

What was run, so the verdict is grounded:

- `node docs/architecture/sync.mjs check` exits 0 (56 elements, 17 workloads).
- `python3 scripts/pre-commit-likec4-selftest.py` passes 16/16.
- `drill.fish` was replayed in full against the current tree. Its output is byte-identical to the committed `drill-output.txt` (the only diff is a trailer line my wrapper appended). The recorded drill evidence is therefore current, not stale.
- `02-drills-before.txt` shows `exit=0` for D17 to D24 (controls included) and `01-drills-before.txt` shows it for D10 to D16, so the "not caught before" claims in DIAGRAM_CONVENTIONS.md hold. Those runs used the old 33-element model, so they prove the old script did not look, not that the mutation was applied there.
- Scenario steps were checked against the cited source for `signin-*`, `signup-ok`, `error-403`, `error-409`, `activity-log-row`, `push-to-image`, `image-to-rollout`, `ingress-to-pod`, `nightly-backup` and `inbound-packet-path`. Most cites hold. The exceptions are WR-05 and WR-06.
- The self-test fixture change (`TRACKED_PATHS`, `test_08`) removes no assertion and strengthens `test_08`: the case now also has to satisfy placement and view membership. No finding.
- No secret, IP, age recipient, bucket name or Docker Hub user is in the model, drill or doc. The only environment-specific values are the three public duckdns hostnames, which are already committed in `k8s/` (IN-05).

No blocker found. The drift rules that exist are mostly sound and are exercised by drills. The defects are rules that can pass vacuously on input they claim to cover (WR-01, WR-07), a drill harness with no machine oracle (WR-02), and model or doc claims the code contradicts or does not enforce (WR-03 to WR-09).

## Warnings

### WR-01: The metadata-key allowlist is surface-agnostic, so keys that are allowed are silently ignored on most surfaces

**File:** `docs/architecture/sync.mjs:422-438` (also `605-608`, `599`, `263`)
**Issue:** `checkMetadataKeys` validates keys against one list (`source`, `workload`, `host`) on elements, deployment nodes, instances and relationships. Every reader only looks at elements: the `source` existence loop (605), the workload sets (599), `checkHosts` (263), and `checkPlacement` and `checkDeployedWorkloads` (via `element.getMetadata`). So `workload 'Deployment/x'` on an instance, `host [...]` on a relationship, or `source 'k8s/gone'` on a deployment node passes the allowlist and is then never compared with anything. This is the same silent no-op the allowlist was built to prevent (a misspelled key), reached with a correctly spelled key on the wrong surface. D12b only plants unknown key names, so it cannot see this.

Separately, `source` is read as a string. `existsSync(join(root, src))` throws `TypeError` if an author writes `source [...]`, the way `workload` and `host` are written. The result is a stack trace and exit 1, which the pre-commit hook reports as model drift (the case the header comment of `loadTooling` says it avoids).
**Fix:** Make the allowlist per surface.
```js
const KEYS_BY_SURFACE = {
  element: ['source', 'workload', 'host'],
  'deployment node': [],
  instance: [],
  relationship: [],
}
```
Look the surface up in `checkMetadataKeys`. In the source loop use `[e.getMetadata('source')].flat().filter(Boolean)` and check each entry. Add a drill that puts `workload` on an instance.

### WR-02: drill.fish has no oracle, and a sed that misses its target is indistinguishable from "not caught"

**File:** `docs/architecture/drill.fish:36-40, 64-246`
**Issue:** `run` prints the exit code and output, and nothing compares them with an expectation. The header says every drill except D5, D13b, D16b and D17b "is expected to be caught", but that expectation lives only in a comment and in a human diff against `drill-output.txt`. Nothing in CI or the hook runs the drills at all (`rg drill` over `invariant-checks.yml` and `.githooks/pre-commit` is empty). Many mutations are literal `sed` patterns (D6, D6b, D9, D10, D12, D12b, D18, D20 to D24). If the target text changes (a renamed view, a re-indented model line, a new host), `sed` exits 0 having changed nothing, the check reports a clean run, and the drill reads as "not caught" with no error. D24 hard-codes `kanban-board-rud-vlad-473-nonprod`, so a host change breaks that drill the same way. The guarantee "each rule is seen failing before it is trusted" is only as good as the reviewer reading 183 lines of output.
**Fix:** Give each drill an expected exit and fail the script on mismatch, and assert the mutation took effect.
```fish
function expect --argument-names want
    # `run` stores its exit code in $last_code
    test $last_code = $want; or begin; echo "  UNEXPECTED exit $last_code, wanted $want"; set -g failed 1; end
end
```
Snapshot the scratch tree before the mutation and `diff -rq` it afterwards. Exit non-zero if nothing changed. Finish with `exit $failed`.

### WR-03: DIAGRAM_CONVENTIONS understates which model sources the commit hook does not guard

**File:** `docs/DIAGRAM_CONVENTIONS.md:259-266` (the hook itself is `.githooks/pre-commit:186-210`)
**Issue:** The doc says "A changed metadata source under `src/main/java` is caught in CI only." The hook triggers only on `src/main/resources/db/migration`, `k8s`, `docs/architecture` and `KafkaTopics.java`. Phase 2 added `source` citations outside those paths: `.github/workflows/deploy.yml` (`ci`, `dockerhub`), `infra/vm/k3s-host-firewall.sh` (`hostfilter`), `src/main/avro` (`avro_subjects`) and `src/main/resources/application.properties` (`management`, the two Spring Session tables). Deleting or renaming any of them commits cleanly and fails only in CI after the push. The self-test change that copies `.github/workflows` and `infra/vm` into the scratch repo shows the authors know these paths are sources. The doc, and the hook's own "Known holes" comment, still describe only the Java case.
**Fix:** Either add the paths to the hook trigger (`.github/workflows/deploy.yml`, `infra/vm`, `src/main/avro`, `application.properties`) and add a self-test case, or rewrite the doc sentence to list them.

### WR-04: The Scenarios row claims every step is cited in the evidence file, but three scenario views have no section in it

**File:** `docs/DIAGRAM_CONVENTIONS.md:113`; `.planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md` (no `signin-ok`, `signin-bad-credentials` or `signin-ceiling` section)
**Issue:** The Provenance cell for the Scenarios viewpoint says "authored: every step is cited in `02-SCENARIO-EVIDENCE.md`, or marked authored with its reason". The viewpoint has 12 views. The evidence file covers 9 of the Phase 2 views and none of the three `signin-*` views in `views.c4:90-136`. I traced `signin-ok` and `signin-ceiling` against `AuthenticationController` and `UserAuthenticationProvider` and the steps are right, so this is an unbacked claim, not a wrong diagram. As written, the doc promises a traceability that does not exist for a quarter of the views.
**Fix:** Add the three `signin-*` sections to the evidence file, or narrow the doc sentence to "the Phase 2 scenario views".

### WR-05: `certmanager.staging` states a fact the manifests contradict

**File:** `docs/architecture/model/edge.c4:9-15` (and the edge at `edge.c4:45`)
**Issue:** The description says "Issuer that every certificate starts on, so a misconfiguration costs no production rate limit." It comes from the header comment of `k8s/platform/edge/clusterissuers.yaml`, which describes the original rollout plan. `rg letsencrypt- k8s` shows all three Certificates (`overlays/prod/certificate.yaml:11`, `overlays/nonprod/certificate.yaml:11`, `platform/edge/certificate-monitoring.yaml:13`) reference `letsencrypt-production`. No Certificate uses staging. The model draws an active `certmanager.staging -> acme 'ACME over HTTPS'` edge and a "starts on staging" description for an issuer nothing uses. `sync.mjs` can only prove the issuer exists (`workload ClusterIssuer/letsencrypt-staging`), so the check stays green while the claim is false.
**Fix:** Reword to what is true: "Staging issuer, defined but referenced by no Certificate today." Or drop the edge if it is not meant to say that an issuer is live.

### WR-06: The host-filter step and element say the chain drops "any other new connection"; the script returns ICMP first

**File:** `docs/architecture/model/scenarios-platform.c4:37`; `docs/architecture/model/edge.c4:31`; evidence `02-SCENARIO-EVIDENCE.md` inbound row 3
**Issue:** The step reads "new TCP on an allow-listed port return from the chain, any other new connection is dropped". `edge.c4:31` says the filter "admits new TCP connections only on allow-listed ports". In `infra/vm/k3s-host-firewall.sh:63` the chain carries `-A ${CHAIN} -p icmp -j RETURN` ahead of the `NEW ... DROP` at line 64, so new ICMP is admitted. The evidence row cites lines 61, 62 and 64 and skips 63, which is the line that contradicts the sentence. Port 22 is also in `ALLOWED_TCP_PORTS` (line 45), but the step's next hop is to ServiceLB, so SSH is simply not drawn.
**Fix:** "...new TCP on an allow-listed port and ICMP return from the chain, any other new connection is dropped". Make the same change in the element description and add line 63 to the evidence row.

### WR-07: `routedHosts` reads only the first argument of `Host(...)` and ignores `HostRegexp`, so the hosts rule can pass vacuously

**File:** `docs/architecture/sync.mjs:255`
**Issue:** `/Host\(\s*`([^`]+)`/g` captures the first backticked string after `Host(`. Traefik accepts `Host(\`a.example\`, \`b.example\`)`, and then `b.example` is never read. A route that serves a second host passes the "every routed host is claimed" half, and a claimed second host is reported as "no IngressRoute routes it" (a false positive). `HostRegexp(...)` does not match at all. The doc states the rule without this limit ("every `Host(...)` literal in the `match` of an `IngressRoute` is claimed"). Today's manifests use only the single-argument form, so no drill or baseline exposes it.
**Fix:** Capture the whole argument list and split it.
```js
for (const m of route.match.matchAll(/Host\(([^)]*)\)/g)) {
  for (const h of m[1].matchAll(/`([^`]+)`/g)) out.add(h[1])
}
```
Document that `HostRegexp` and `HostSNI` are not read, or fail on them.

### WR-08: The Mermaid-parity claim overstates what was built

**File:** `docs/DIAGRAM_CONVENTIONS.md:379-382` (and the `signup` row at `414`)
**Issue:** The text says the LikeC4 counterparts "are now built", mapping `error-status-split` to `error-401`, `error-403` and `error-409`. `docs/diagrams/scenarios/error-status-split.mmd:22,26` also draws the 400 `VALIDATION_FAILED` branch, which no LikeC4 view draws. The inventory row for `signup` says the Mermaid diagram shows "its auto-authentication and its rollback". `signup-ok` draws no rollback. `AuthenticationController.signup` deletes the user when authentication fails. The doc says nothing about either missing branch, and these Mermaid files are what the doc says will be retired later.
**Fix:** State the gap ("no 400 view; signup rollback not drawn"), or build `error-400` and a signup-rollback view before the Mermaid originals are retired.

### WR-09: The public Grafana path is claimed but not drawn

**File:** `docs/architecture/model/model.c4:6-16`; `docs/architecture/model/views.c4:59-75`; `docs/architecture/model/monitoring.c4`
**Issue:** `traefik` claims `kanban-board-rud-vlad-473-monitoring.duckdns.org`. `k8s/monitoring/configs/ingressroute.yaml:46-60` routes that host to `kube-prometheus-stack-grafana:80`. The model has no `traefik -> grafana` relationship. The `production` view does not include `netcup.k3s.monitoring`, and the `monitoring` view does not include Traefik. The only internet-facing path to Grafana, the one routed host that serves something other than the app, therefore appears in no view. The hosts rule only proves the string exists (set equality), and the doc admits that "which instance serves which host stays authored". Here the authored half was left out.
**Fix:** Add `traefik -[sync]-> grafana 'HTTP :80, Host rule for the monitoring hostname'` and include Traefik and Grafana in the `monitoring` view. Mind the shared-title rule for the merged edge label.

## Info

### IN-01: A comment in sync.mjs is now false

**File:** `docs/architecture/sync.mjs:273-277`
**Issue:** It says removing `k8s/monitoring` or `k8s/flux-system` "changes no finding". Since this phase both trees are workload sources, so removing them would raise workload findings. The `K8S_DIRS` guard is still needed, and D14b still discriminates because its exit is 4, not 1. The stated reason no longer holds.
**Fix:** Reword to say the guard keeps a missing tree an exit 4 rather than a different drift message.

### IN-02: A hand-copied table list in a process-view edge title duplicates generated facts

**File:** `docs/architecture/model/views.c4:19-21`
**Issue:** The `kanban.api -> postgres` override title lists seven table names by hand ("JDBC: users, boards, columns, tasks, subtasks, spring_session, spring_session_attributes"). The tables come from generated or authored elements. A new table, or one the API stops touching, leaves the title wrong with no check. DIAGRAM_CONVENTIONS says to prefer moving a fact up the provenance ladder.
**Fix:** Use a protocol-level title ("SQL over JDBC; reads and writes the application tables") and leave the table list to the `data` view.

### IN-03: Hand-written absence and exclusivity claims on notes are unchecked

**File:** `docs/architecture/model/model.c4:87-100`
**Issue:** `activity_log_note` asserts "no foreign key". `sync.mjs` already computes `foreignKeys()`, so it could verify that claim. `avro_subjects` says the subjects are "only looked up by the producer", but the same view family draws the consumer fetching schemas from the registry (`views.c4:37`). Only the `source` path existence is checked for both notes.
**Fix:** Add a one-line check that no generated FK has child `activity_log`, or soften the wording. For the Avro note say "looked up by the producer and fetched by id by the consumer".

### IN-04: A "provenance" convention for relationships is not followed or checked

**File:** `docs/DIAGRAM_CONVENTIONS.md:94`; `docs/architecture/model/edge.c4:45-51`, `docs/architecture/model/monitoring.c4:50-58`
**Issue:** The standard says "A relationship that is authored is said to be authored." Elements say "Authored." in their descriptions, but the authored relationships (`client -> firewall`, `firewall -> hostfilter`, `backup -> b2`, every scrape and query edge) carry no marker, and no relationship is on the `checked` rung. `prometheus` says its scrape edges are authored, which is the one place this is stated. Nothing enforces the convention.
**Fix:** Either tag authored relationships (a planned `authored` tag is cheap) or soften the sentence to what is actually done.

### IN-05: Personal hostnames are committed in the model and in a drill

**File:** `docs/architecture/model/model.c4:11-13`; `docs/architecture/drill.fish:245`
**Issue:** Three `kanban-board-rud-vlad-473*.duckdns.org` names appear in `model.c4`, and one is a literal in D24. They are public, already in `k8s/overlays` and `k8s/monitoring`, and the hosts rule needs them, so this is not a leak. The cost is duplication: a hostname change requires editing the model and the drill, and the drill fails silently (WR-02).
**Fix:** Derive D24's pattern from the manifest at run time (read the first `Host(` literal from the ingress file) instead of repeating it.

---

_Reviewed: 2026-10-28_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
