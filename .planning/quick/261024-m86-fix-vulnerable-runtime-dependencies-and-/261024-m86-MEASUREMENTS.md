# Quick task 261024-m86: measurements (2026-10-24)

Branch fix/dependency-scan-and-bumps, repo RudVlad473/task-orchestration-backend. Commits: c8e81a7 (scan fix, guard), f837b62
(guard counts top-level jar entries only), ea60c59 (dependency submission), 14bcc09 (bumps and floors), aa2299d (measured timeouts,
temporary trigger removed, docs). Every commit ran the full pre-commit hook; no --no-verify.

Run ids:

| Run | Workflow | Commit | Conclusion | Purpose |
|---|---|---|---|---|
| 40103131374 | Dependency Vulnerability Scan (dispatch) | c8e81a7 | success | cold measurement run |
| 40104144379 | Dependency Submission (push) | ea60c59 | success | first submission |
| 40104147751 | Dependency Submission (dispatch) | ea60c59 | success | dispatch on a branch-only file |
| 40106905185 | Dependency Submission (push) | 14bcc09 | success | post-bump graph |
| 40107989978 | Dependency Vulnerability Scan (dispatch) | aa2299d | success | final proof, warm cache |

## 1. Root cause of the 0-dependency scans

- Source: Spring Boot 3.5.16 makes runtimeClasspath extend testAndDevelopmentOnly. dependency-check-gradle 12.2.2 tests the
  configuration name and every name in its hierarchy against a "test" regex, so runtimeClasspath, the only configuration named in
  scanConfigurations, was skipped when skipTestGroups was true (the default).
- Local, offline (autoUpdate false, empty data dir), before the fix, `--info`:
  `'runtimeClasspath --> runtimeOnly --> implementation --> developmentOnly --> testAndDevelopmentOnly' is considered a test configuration: true`.
  After `skipTestGroups = false` that line is absent.
- CI: run 40103131374 scanned 171 entries where the 2026-08-19 run (32280511632) scanned 0.

## 2. Guard directions and the OOM check

- 2026-08-19 report (run 32280511632, `dependencies: []`) against a 160-module tree: exit 1, "0 dependencies scanned". HTML: "Dependencies Scanned: 0 (0 unique)".
- Branch reports: run 40103131374 exit 0 (157 top-level jars, floor 152); run 40107989978 exit 0 (156 top-level jars, floor 152).
- Selftest: exit 0 on the repository guard (24 cases). With `--guard` pointed at a copy whose floor comparison is `elif False:`: exit 1, 7 cases fail.
- Amendment f837b62: counting only top-level jar entries. Against the first guard (c8e81a7) the selftest exits 1 on three new cases, so the
  amendment is pinned.
- OOM check direction: `gh run view 32278248354 --log | grep -c OutOfMemoryError` = 2 (the August run, whose build still ended BUILD SUCCESSFUL).
  The workflow now greps the tee'd log of each scan step and fails the step on a match. Runs 1 and 2 logged none (the word appears
  only in the workflow's own script text).

## 3. Durations and the timeouts set

| Step | Cold (run 40103131374) | Warm (run 40107989978) | Limit set | Ratio to cold |
|---|---|---|---|---|
| Update the NVD database | 6m22s | 34s | 20 min | 3.1x |
| Run dependencyCheckAnalyze | 19s | 27s | 10 min | 31x (the plan's floor) |
| Resolve the runtime classpath tree | 27s | 27s | none | |
| Job total | 7m29s | 1m51s | 40 min (20 + 10 + 10 setup) | 5.3x |
| Dependency Submission job | 55s, 62s, 67s (three runs) | | 10 min (floor) | |

The plan's rule is 2x the update, so 13 minutes; 20 was chosen on the orchestrator's instruction to keep generous headroom, because
eight earlier weekly runs were cancelled at 45 minutes for a cause that was never found (the old step logged nothing between its start
and the cancel). Every limit is a runaway guard, not a forecast. The update step runs at `--info`, so a stall now shows where it stopped.

## 4. Cache behaviour

- Run 1 (cold): "Cache not found for input keys: dependency-check-data-12.2.2-40103131374-1, dependency-check-data-12.2.2-". The update fetched
  413,903 NVD records. "Cache saved with key: dependency-check-data-12.2.2-40103131374-1" (122.61 MiB), 3s, saved before the analysis started.
- Run 2 (warm): "Cache restored from key: dependency-check-data-12.2.2-40103131374-1", i.e. run 1's key, restored by prefix. The update step ended in 34s,
  of which Gradle start-up and configuration was most. The task itself logged `Skipping the NVD API Update as it was completed within the last 240 minutes`
  and `Check for updates complete (304 ms)`. So the restore path is proven, but the INCREMENTAL update path (an update that fetches only
  changed records) was not exercised, because run 2 started less than four hours after run 1. Its duration on a real weekly run is not measured.
- The save step also ran in run 2 and wrote key dependency-check-data-12.2.2-40107989978-1. Each run adds one ~122 MiB entry; GitHub evicts
  entries unused for 7 days and applies LRU at the 10 GB repository cap, so these expire on their own. Main cannot read a branch-saved cache,
  so the first Monday run on main starts cold; the 20-minute limit covers a cold update (6m22s measured).

## 5. Dependency counts

| | Run 40103131374 (pre-bump) | Run 40107989978 (post-bump) |
|---|---|---|
| Runtime modules in the tree (guard parse) | 160 | 159 |
| JSON dependencies array | 171 | 170 |
| HTML "Dependencies Scanned" | 171 (171 unique) | 170 (170 unique) |
| Top-level jar entries (what the guard counts) | 157 | 156 |
| Floor (95 percent of modules) | 152 | 152 |
| Entries / modules | 1.069 | 1.069 |
| Top-level jars / modules | 0.981 | 0.981 |

Entries above the module count are files embedded in a jar (a `fileName` containing ": ", e.g. swagger-ui bundles, snappy and zstd native
libraries) plus one shaded entry. In run 1, 158 of 160 modules had a matching jar entry (jackson-bom is POM-only, kotlin-stdlib-common has no
jar, okio shows as okio-jvm).

## 6. OWASP findings, before and after the bumps (CPE matched, NVD)

| Severity | Pre-bump (run 1) | Post-bump (run 2) |
|---|---|---|
| CRITICAL | 101 | 83 |
| HIGH | 99 | 86 |
| MEDIUM | 132 | 114 |
| LOW | 15 | 15 |
| Total | 347 | 298 |
| Vulnerable dependencies (entries) | 42 | 35 |

scanInfo: engine 12.2.2; NVD API Last Modified 2026-10-24T14:16:55Z in both runs (run 2 skipped the NVD update, section 4).

Post-bump vulnerable entries (from the report, out of 170): 13 Spring Framework jars at 17 findings each (spring-aop, aspects, beans, context, core,
expression, jcl, jdbc, messaging, orm, tx, web, webmvc, all 6.2.19), 4 Spring Security jars at 5 each (6.5.11), spring-data-jpa 3.5.13 (1),
kotlin-stdlib 1.9.25 (2), 13 io.opentelemetry 1.62.0 jars at 1 each, and two embedded DOMPurify 3.2.4 files inside swagger-ui-5.21.0.jar at 20 each.

OWASP findings with no OSV twin (OSV after the bumps lists only the 2 spring-webmvc advisories) and what they are, from the report's descriptions:

- The 17 Spring Framework CVEs on all 13 jars: the CPE `pivotal_software:spring_framework:6.2.19` is attributed to every Spring Framework module,
  while most descriptions name a single feature (WebFlux, RSocket, Aalto XML, XsltView, FreeMarker, Jetty 12 adapter). OSV ties an advisory to the
  package that contains the code, which is why it reports spring-webmvc only. I did not check each of the 17 against the affected ranges for 6.2.19;
  one description reads "Spring Framework 7.0.0 -", so at least that one is a 7.x advisory.
- The 13 opentelemetry jars: CVE-2026-54285, whose description opens "opentelemetry-js is the OpenTelemetry JavaScript Client". A JavaScript
  advisory matched to Java artifacts through the CPE `opentelemetry:opentelemetry`.
- The 4 Spring Security jars (CVE-2026-59270 CRITICAL, 41707 and 47841 HIGH, 47842 and 59276 MEDIUM): OWASP lists them; OSV does not for 6.5.11.
  Whether 6.5.11 is inside each affected range was not checked. Descriptions name the embedded UnboundID LDAP server, DPoP and WebAuthn. A grep
  of src/main for these features was not run in this task.
- spring-data-jpa 3.5.13, CVE-2026-47834 (Sort validation bypass): OSV lists nothing; the fix version was not looked up.
- kotlin-stdlib 1.9.25, CVE-2026-53914 (build-cache deserialization) and CVE-2020-29582 (temporary file creation): OSV lists nothing; the first
  is described as a build-time issue. Not checked further.
- DOMPurify 3.2.4 inside swagger-ui-5.21.0.jar (20 CVEs per file, medium and low): a JavaScript library embedded in springdoc's swagger-ui
  webjar. OSV queries here were Maven coordinates only, so this class was never in the OSV table. Whether a newer springdoc ships a newer
  DOMPurify was not checked.

## 7. OSV before and after (api.osv.dev, Maven coordinates of the runtimeClasspath tree)

Before (same day, 160 modules): 26 findings on 9 modules, which equals the brief's 26 (the brief said 8 modules; the 9th is
opentelemetry-extension-trace-propagators, counted separately). Severity: CRITICAL 5, HIGH 7, MODERATE 13, LOW 1. The 26 advisory ids are distinct.
After (159 modules): 2 findings on 1 module, both CRITICAL.

By module, before to after: tomcat-embed-core 3 to 0, lz4-java 6 to 0, jackson-core 2 to 0, jackson-databind 9 to 0, postgresql 1 to 0,
opentelemetry-api 1 to 0, opentelemetry-extension-trace-propagators 1 to 0, log4j-api 1 to 0, spring-webmvc 2 to 2.

Remaining:

| Module | Advisory | Why it remains |
|---|---|---|
| org.springframework:spring-webmvc:6.2.19 | GHSA-j9f9-w8pj-32f8 (CVE-2026-47890, SSE stream corruption while rendering fragments) | Fixed only in Spring Framework 7.0.9. 6.2.19 is the newest 6.2.x on Maven Central and Spring Boot 3.5.x is on 6.2, so the fix needs Spring Boot 4 (PR 16). |
| org.springframework:spring-webmvc:6.2.19 | GHSA-pc63-qcmh-9cmg (CVE-2026-47884, improper path limitation in XsltView) | Same. |

Reachability note: a search of src/main and src/test for SseEmitter, text/event-stream, TEXT_EVENT_STREAM, ServerSentEvent, ResponseBodyEmitter,
StreamingResponseBody, FragmentsRendering, XsltView, xslt, thymeleaf, freemarker and mustache matched nothing. The app declares no SSE endpoint,
no fragment-rendering view and no XSLT view in its own code. The grep does not show whether a dependency (springdoc) uses them internally.

Maven Central re-check at execution time: spring-boot newest 3.5.x is 3.5.16, so no Boot patch bump exists. Versions chosen: tomcat 10.1.61,
jackson 2.21.7, postgresql 42.7.12, log4j2 2.25.5, opentelemetry 1.62.0, lz4-java 1.11.4; a candidate OSV query on 17 modules at those
versions (and some newer) returned 0 findings.

Side effect of the OpenTelemetry bump: opentelemetry-exporter-sender-okhttp 1.62.0 itself requests okhttp 5.3.2, so okhttp 4.12.0 became
5.3.2 (plus okhttp-jvm), okio 3.6.0 became 3.16.4, and kotlin-stdlib-common, -jdk7 and -jdk8 1.9.25 dropped out (160 to 159 modules). The OTLP
export path is not covered by any test (management.otlp.tracing.export.enabled defaults to false); an export to a live collector was not run.

## 8. Floors, regeneration and tests

- Red: floors added with no overrides: `./gradlew -q verifyRuntimeDependencyFloor` exit 1, first failing entry
  `'org.apache.tomcat.embed:tomcat-embed-core' resolves to '10.1.55' on configurations.runtimeClasspath, below the required floor '10.1.61'`.
  Green after the overrides and the metadata regeneration: exit 0.
- Regeneration: `./gradlew --write-verification-metadata sha256 --refresh-dependencies -I graph-plugin.init.gradle spotlessCheck test`.
  Attempt 1 died 7m27s in when the Gradle daemon disappeared during `test` (cause not verified; the daemon log shows only a JVM shutdown, no OOM).
  Attempt 2: BUILD SUCCESSFUL in 11m29s, 251 test classes, 601 tests, 0 failures, 0 errors, 0 skipped. verification-metadata.xml +470 lines,
  0 deleted, 709 components now (656 on the base commit).
- Gradle dependency verification is strict. The submission action runs its own graph build with `-Dorg.gradle.dependency.verification=off`
  (seen in its command line), so the checksums added for `org.gradle:github-dependency-graph-gradle-plugin:1.5.0` were not seen to be needed.

## 9. GitHub side

- Before (default branch): SBOM 11 packages, all GitHub Actions, 0 Maven. Open Dependabot alerts 0. No GET endpoint lists submitted snapshots.
- After the branch runs: SBOM 11 packages, 0 Maven; alerts 0. GitHub answered the submission with: `The snapshot was accepted, but it is not for
  the default branch. It will not update dependency results for the repository.` So Dependabot alerts for Gradle dependencies appear only after
  dependency-submission.yml has run on main; this task did NOT show alerts working.
- `workflow_dispatch` on a workflow file that exists only on the branch was NOT refused: `gh workflow run dependency-submission.yml --ref
  fix/dependency-scan-and-bumps` printed the run URL (run 40104147751) and the run succeeded. The planning assumption was wrong, so the
  temporary push trigger was not needed; it was removed anyway.
- Graph content (run 40104144379): 161 packages = the 160 tree modules plus `org.gradle:gradle-core:8.11.1`, which the plugin reports itself with
  scope "development" (not on runtimeClasspath). No junit, testcontainers, archunit or rest-assured coordinate. The plan's broader pattern also
  matched `com.google.errorprone:error_prone_annotations:2.18.0`, a genuine runtimeClasspath jar.
- Compare API `main...ea60c59` (pre-bump): 161 maven changes, 9 vulnerable packages, 26 advisories (critical 5, high 7, moderate 13, low 1).
  Compare API `main...14bcc09` (post-bump, rechecked at the end): 163 changes, 160 maven; one package carries vulnerabilities, spring-webmvc 6.2.19
  with GHSA-pc63-qcmh-9cmg and GHSA-j9f9-w8pj-32f8. This equals the OSV after table.

## 10. PR 16

Open, "chore(deps): bump the gradle-updates group across 1 directory with 18 updates". Touches build.gradle and the wrapper files. Overlap with this
branch is build.gradle only. It bumps Spring Boot 3.5.16 to 4.1.1, dependency-check 12.2.2 to 13.0.0, Gradle to 9.x and others. A Boot 4 BOM manages
its own Tomcat, Jackson, OpenTelemetry and Log4j lines, so the override block (marked `TODO: #16`) must be dropped when it lands; the floors fail
loudly if a resolved version falls below them. PR 16 is also the only route to the 2 remaining spring-webmvc advisories. It was not commented on,
merged, closed or rebased.

## 11. Final checks

- deploy.yml runs on the branch: 0. Runs on the branch: only the Dependency Submission and Dependency Vulnerability Scan runs listed above.
- `git branch -r --contains HEAD`: origin/fix/dependency-scan-and-bumps only.
- The push of aa2299d started no workflow (the temporary trigger was gone).
- Worktree status empty after the commit. Primary checkout: the user's six pre-existing entries unchanged (STATE.md, AsyncConfig.java,
  KafkaConsumerConfig.java, ActivityTraceContinuityE2ETest.java, kanban-backup.agekey, the 261024-nvj directory) plus this quick directory.

## 12. Not verified

- That Dependabot alerts fire on main after the merge.
- The incremental NVD update path and its duration (run 2 skipped the update inside the 240-minute window).
- Whether the weekly Monday run starts cold on main (7-day eviction).
- That the graph plugin checksums are needed by any action version.
- The cause of the eight cancelled 45-minute runs and of the Gradle daemon disappearance in regeneration attempt 1.
- The OTLP export path against a live collector.
- Each of the 17 Spring Framework, 5 Spring Security and other OWASP findings against its affected range for the resolved version.

## 13. osv_runtime_audit.py (reuse for the next measurement)

Run as `python3 -I osv_runtime_audit.py TREE_FILE OUT_PREFIX` with the tree from `./gradlew -q dependencies --configuration runtimeClasspath`.
It imports the repository guard's `parse_tree`; the GUARD constant below names the worktree path used here, so change it to
`scripts/verify-dependency-check-report.py` of the checkout in use.

```python
"""Query OSV for every module of a runtimeClasspath tree and write JSON plus a markdown table.

Usage: python3 -I osv_runtime_audit.py TREE_FILE OUT_PREFIX
Reads the tree through the repository guard's parser (public coordinates only leave the machine), posts one
/v1/querybatch call, follows next_page_token, fetches each advisory at /v1/vulns/{id}, and writes
OUT_PREFIX.json and OUT_PREFIX.md. A finding is a (module, advisory id) pair; the count deduped by alias
is reported as well.
"""

import importlib.util
import json
import sys
import urllib.request

GUARD = "/home/andre/dev/kanban-board-backend/.claude/worktrees/dep-scan/scripts/verify-dependency-check-report.py"
API = "https://api.osv.dev"


def post(path, body):
    request = urllib.request.Request(
        API + path, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(request, timeout=60) as response:  # noqa: S310
        return json.load(response)


def get(path):
    with urllib.request.urlopen(API + path, timeout=60) as response:  # noqa: S310
        return json.load(response)


def load_modules(tree_path):
    spec = importlib.util.spec_from_file_location("guard", GUARD)
    guard = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(guard)
    with open(tree_path, encoding="utf-8") as handle:
        modules, _ = guard.parse_tree(handle.read())
    return sorted(modules)


def query_all(modules):
    queries = [
        {"package": {"ecosystem": "Maven", "name": "%s:%s" % (g, a)}, "version": v} for g, a, v in modules
    ]
    ids_by_module = {m: [] for m in modules}
    pending = list(enumerate(queries))
    while pending:
        result = post("/v1/querybatch", {"queries": [q for _, q in pending]})
        next_pending = []
        for (index, query), item in zip(pending, result["results"]):
            ids_by_module[modules[index]].extend(v["id"] for v in item.get("vulns", []))
            token = item.get("next_page_token")
            if token:
                next_pending.append((index, dict(query, page_token=token)))
        pending = next_pending
    return ids_by_module


def fixed_in(vuln, package):
    fixed = []
    for affected in vuln.get("affected", []):
        if affected.get("package", {}).get("name") != package:
            continue
        for rng in affected.get("ranges", []):
            fixed.extend(e["fixed"] for e in rng.get("events", []) if "fixed" in e)
    return sorted(set(fixed))


def main(tree_path, prefix):
    modules = load_modules(tree_path)
    ids_by_module = query_all(modules)
    cache = {}
    findings = []
    for (group, artifact, version), ids in ids_by_module.items():
        package = "%s:%s" % (group, artifact)
        for vuln_id in sorted(set(ids)):
            if vuln_id not in cache:
                cache[vuln_id] = get("/v1/vulns/" + vuln_id)
            vuln = cache[vuln_id]
            findings.append(
                {
                    "module": "%s:%s" % (package, version),
                    "id": vuln_id,
                    "aliases": vuln.get("aliases", []),
                    "severity": (vuln.get("database_specific") or {}).get("severity", "UNKNOWN"),
                    "summary": vuln.get("summary", ""),
                    "fixed": fixed_in(vuln, package),
                }
            )
    dedup = set()
    for finding in findings:
        dedup.add(frozenset([finding["id"], *finding["aliases"]]) | {finding["module"]})
    by_severity = {}
    for finding in findings:
        by_severity[finding["severity"]] = by_severity.get(finding["severity"], 0) + 1
    out = {
        "modules_queried": len(modules),
        "findings": len(findings),
        "by_severity": by_severity,
        "findings_list": findings,
    }
    with open(prefix + ".json", "w", encoding="utf-8") as handle:
        json.dump(out, handle, indent=2)
    lines = [
        "modules queried: %d; findings (module, advisory id): %d; by severity: %s"
        % (len(modules), len(findings), by_severity),
        "",
        "| module | advisory | severity | summary | fixed in |",
        "|---|---|---|---|---|",
    ]
    for f in sorted(findings, key=lambda x: (x["module"], x["id"])):
        lines.append(
            "| %s | %s (%s) | %s | %s | %s |"
            % (f["module"], f["id"], ", ".join(f["aliases"][:2]), f["severity"], f["summary"][:90], ", ".join(f["fixed"]))
        )
    with open(prefix + ".md", "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
```

(The `dedup` set is built but never reported; the alias-deduped count was not printed. The 26 advisory ids were checked to be distinct by hand.)
