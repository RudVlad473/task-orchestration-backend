# osv-scanner vs dependency-check (quick 261116-005)

- Date: 2026-11-16
- osv-scanner: v2.7.1 (osv-scalibr 0.5.3, commit 02f96a4, built 2026-11-09), linux amd64 release binary run from a `/tmp/osv-cmp-261116-005.*` directory, never from PATH
- SHA-256: `fb7dab19a8bd9bbe5c3189f09d4b7ee1820c9553a23bac202648a56efd2f6ee8`, equal to the release `osv-scanner_SHA256SUMS` line, to GitHub's asset digest and to the value recorded at planning time
- Flags: `scan source -L <input> --format json --all-packages --all-vulns --output-file <file>` (default data source deps.dev, default transitive resolution, no `--offline`)
- Trivy image: `ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb` (the pin in `scripts/trivy-scan.sh`), run as `rootfs --format cyclonedx --pkg-types library` over a copy of the boot jar built by `./gradlew --no-daemon bootJar` (42s build; Trivy about 5m45s by its log timestamps, almost all of it a 954 MiB Java DB download into an empty cache, 1.5 GB cache afterwards, trashed)
- Baseline: security-scan.yml run 42634765236 (2026-11-09T14:26:53Z), head commit 6c4bef8, NVD API Last Modified 2026-11-09T14:29:30Z
- Drift: `git diff --stat 6c4bef8 HEAD -- build.gradle settings.gradle gradle/` is empty, so the baseline describes HEAD's dependency set
- Run id check: no newer successful security-scan.yml run existed at execution time (next older run is 2026-10-24)

## Baseline: dependency-check run 42634765236

| Metric | Value |
| --- | --- |
| Expected runtime modules (runtimeClasspath tree, `parse_tree`) | 157 |
| Dependencies scanned (all entries / top-level jars) | 168 / 154 |
| Vulnerable entries | 35 |
| (entry, CVE) findings | 302 (NVD 290, RETIREJS 12) |
| Unique CVE ids | 48 (every one CVE-prefixed) |
| Distinct (group:artifact, CVE) pairs | 280 (the two swagger-ui DOMPurify bundles share one purl, so 44 entry rows are 22 pairs) |
| Severity (CRITICAL / HIGH / MEDIUM / LOW) | 83 / 88 / 114 / 17 |
| Engine, guard verdict | 12.2.2, PASS (`verify-dependency-check-report.py`) |

All pair counts below use the 280 distinct (group:artifact, CVE) pairs. The runtime set built from the tree has 157 rows, equal to the guard's expected count.

## Input 1: gradle/verification-metadata.xml

| Metric | Value |
| --- | --- |
| verification-metadata.xml components | 728 |
| Packages resolved | 728 |
| Runtime modules covered | 157 of 157 |
| Findings reported (package, advisory group) | 57 |
| Unique advisory groups | 47 |
| Findings inside runtime scope | 3 |
| Findings outside runtime scope (other version / not on runtimeClasspath) | 54 (43 other version of a runtime artifact, 11 artifact not on runtimeClasspath) |
| Overlap with dependency-check, (group:artifact, CVE) pairs | 2 |
| Overlap with dependency-check, unique CVE ids | 2 |
| dependency-check only, (group:artifact, CVE) pairs | 278 (48 unique CVE ids) |
| osv-scanner only inside runtime scope, (package, advisory group) | 1 |

Every runtime module resolved: osv-scanner took all 728 verification-metadata.xml components as packages and every one of the 157 exact group:artifact:version coordinates on the baseline's runtimeClasspath is among them. No module is missing. This settles the open point: with no Gradle lockfile, osv-scanner still gets the full transitive runtime set from the file, because Gradle verified every configuration's artifacts (728 components against 157 runtime modules). The cost is scope: 54 of the 57 findings (95 percent) sit on versions or artifacts that are not on runtimeClasspath.

## Input 2: CycloneDX SBOM of the boot jar

| Metric | Value |
| --- | --- |
| SBOM maven components / BOOT-INF/lib jars | 144 / 144 (every component has a `pkg:maven/` purl, none unidentified) |
| Packages resolved | 144 |
| Runtime modules covered | 142 of 157 |
| Findings reported (package, advisory group) | 3 |
| Unique advisory groups | 3 |
| Findings inside runtime scope | 3 |
| Findings outside runtime scope (other version / not on runtimeClasspath) | 0 (0 other version, 0 not on runtimeClasspath) |
| Overlap with dependency-check, (group:artifact, CVE) pairs | 2 |
| Overlap with dependency-check, unique CVE ids | 2 |
| dependency-check only, (group:artifact, CVE) pairs | 278 (48 unique CVE ids) |
| osv-scanner only inside runtime scope, (package, advisory group) | 1 |

Trivy found 144 of 144 jars on its first pass, above the 95 percent bar, so the unpacked-libs rerun was not needed. The SBOM is only the libraries: it has no component for the application's own jar. The 15 runtime modules not matched by exact coordinate are not missing code:

- 12 have no jar at all: `spring-boot-starter`, `-actuator`, `-aop`, `-data-jpa`, `-jdbc`, `-json`, `-logging`, `-security`, `-tomcat`, `-validation` and `-web` (3.5.16) are POM-only, and so is `com.fasterxml.jackson:jackson-bom:2.21.7`.
- 2 are the same jar under a renamed coordinate: `com.squareup.okhttp3:okhttp:5.3.2` ships as `okhttp-jvm`, `com.squareup.okio:okio:3.16.4` as `okio-jvm` (the jars in BOOT-INF/lib are named `okhttp-jvm` and `okio-jvm`; why Gradle resolves them that way was not investigated).
- 1 is the same jar under the legacy group: `org.hibernate.orm:hibernate-core:6.6.53.Final` appears as `org.hibernate:hibernate-core:6.6.53.Final`.

The SBOM also holds one jar the tree does not list, `spring-boot-jarmode-tools:3.5.16`, which `bootJar` adds. osv-scanner reported nothing on it.

## Differences

The two inputs give the same dependency-check-only set (278 pairs, 48 unique CVE ids) and the same three runtime-scope findings, because Input 1's extra 54 findings all sit outside runtime scope and Input 2 has none of them. Every class below therefore holds for both inputs. Rule (b), package not extracted, matched nothing: all 35 vulnerable dependency-check entries are present in both osv package lists.

### dependency-check only (278 pairs, 48 CVE ids), one class per pair

| Class | Pairs | CVE ids | What it is |
| --- | ---: | ---: | --- |
| (a) embedded JavaScript | 22 | 22 | DOMPurify 3.2.4 in `swagger-ui-bundle.js` and `swagger-ui-es-bundle.js` inside `swagger-ui-5.21.0.jar` (44 of the 302 entry rows; the two bundles share one purl). Neither input models JavaScript inside a jar. |
| (b) package not extracted | 0 | 0 | none |
| (c) CPE fan-out | 24 | 2 | CVE-2026-47884 and CVE-2026-47890: osv-scanner reports both on `spring-webmvc` 6.2.19 only (those are the 2 overlap pairs), dependency-check also puts each on 12 other `spring-*` jars. |
| (d1) OSV Maven record excludes this package or version | 29 | 4 | CVE-2020-29582 on `kotlin-stdlib` 1.9.25 (GHSA-cqj8-47ch-rvvq: fixed in 1.4.21). CVE-2026-53914 on `kotlin-stdlib` (GHSA-r937-wjx7-w2jp lists only `kotlin-gradle-plugin`). CVE-2026-54285 on 14 `opentelemetry-*` 1.62.0 jars (GHSA-8988-4f7v-96qf lists only the npm package `@opentelemetry/core`). CVE-2026-47892 on 13 `spring-*` jars (GHSA-9qf2-26p9-2q2q lists only `spring-webflux`, which is not on the classpath). On OSV's evidence these are dependency-check false positives. |
| (d2) OSV record has no Maven package | 138 | 15 | OSV holds only a git-range import of the NVD record, so osv-scanner has nothing to match by coordinate. Spring Framework, 13 `spring-*` jars at 6.2.19: CVE-2026-47886, -47887, -47891, -47893, -59280, -59281, -59282, -59283, -59313 (9 CVEs, 117 pairs). Spring Security, 4 jars at 6.5.11: CVE-2026-41707, -47841, -47842, -59270, -59276 (5 CVEs, 20 pairs). Spring Data JPA 3.5.13: CVE-2026-47834 (1 pair). This is not evidence that dependency-check is wrong: it is a blind spot of the OSV data model for these advisories. |
| (e) OSV lists the version, osv-scanner did not report it | 13 | 1 | CVE-2026-59314 (Spring Framework `ContentDisposition` response splitting) on 13 `spring-*` jars at 6.2.19. Its OSV record lists tag `v6.2.19` as affected, by git tag and not by Maven coordinate, so osv-scanner cannot report it. Flagged as the one case where OSV's own data names the version in use and the scanner still stays silent. |
| (f) no OSV record | 52 | 4 | CVE-2026-47883, -47885, -47888, -47889, each on 13 `spring-*` jars. `api.osv.dev` answers 404 with no alias, so neither side could be verified. |

Published dates for every (d2), (e) and (f) CVE that OSV returns are 2026-08-25 to 2026-08-27, before the baseline's NVD timestamp, so time skew does not explain them.

### osv-scanner only, inside runtime scope (1 finding per input)

| Class | Findings | Detail |
| --- | ---: | --- |
| (g) published after the baseline's NVD timestamp | 0 | none |
| (h) no CVE alias | 0 | none |
| (i) dependency-check miss | 1 | GHSA-ccrj-gw8r-854m = CVE-2026-118259 on `io.opentelemetry:opentelemetry-api` 1.62.0 (fixed in 1.67.0), published 2026-10-28, 12 days before the NVD timestamp. The baseline report does not contain CVE-2026-118259 anywhere. Whether NVD had analysed it by 2026-11-09 was not checked. |

### osv-scanner only, outside runtime scope

- Input 1: 54 findings, 43 on another version of a runtime artifact and 11 on artifacts not on runtimeClasspath. Top artifacts: `jackson-databind` 9, `tomcat-embed-core` 7 (10.1.55), `lz4-java` 6, `jackson-core` 5, `guava` 5, `snappy-java` 3. One has no CVE alias (GHSA-mhm7-754m-9p8w, `jackson-databind` 2.21.4) and one was published after the NVD timestamp (GHSA-ghc9-756g-vrcv, `org.json:json` 20251224, not on runtimeClasspath).
- Input 2: 0. Trivy's SBOM holds only what ships, so the 54 never appear.

## Verdict

As a report-only replacement for dependency-check's role, osv-scanner is not enough on its own today: on the same 157 runtime modules it reports 3 findings against dependency-check's 280 pairs and 48 CVE ids, and 151 of the 278 pairs it does not report (16 Spring CVEs, classes d2 and e) are advisories that OSV holds only as git ranges, which osv-scanner cannot map to Maven coordinates, with another 52 (class f) that OSV has no record of. What it does cleanly is remove over-attribution, since 53 pairs (classes c and d1) are CPE fan-out or are excluded by OSV's own Maven records, and it found one in-scope advisory dependency-check lacks (CVE-2026-118259 on opentelemetry-api 1.62.0). It would lose the embedded-JavaScript class entirely, 22 pairs and 22 CVE ids in swagger-ui's DOMPurify (44 of 302 entry rows), because neither input sees inside a jar. On input, the SBOM matches what ships (144 of 144 jars, 3 findings, none out of scope) but costs a 42s boot jar build and a Trivy run (about 5m45s on an empty cache, 1.5 GB cache), while verification-metadata.xml needs no build, covers all 157 runtime modules, and returns the same 3 in-scope findings plus 54 out-of-scope ones. The data supports running osv-scanner next to dependency-check as a second, exact-coordinate view, and does not support retiring dependency-check.

## Could not verify

- SLSA provenance of the osv-scanner binary: `slsa-verifier` and `cosign` are not installed. The binary was checked against three digests that share GitHub as their root of trust, which is integrity and not independent authenticity.
- Classes (d2) and (f) leave open who is right: OSV carries no Maven mapping or no record, so 15 + 4 CVE ids (138 + 52 pairs) are unresolved, not cleared. The 404s may fill in later, because the baseline is frozen at 2026-11-09 and `api.osv.dev` was queried live on 2026-11-16.
- Whether NVD had analysed CVE-2026-118259 by the baseline's NVD timestamp: NVD was not queried, so class (i) says dependency-check lacks it, not why.
- Whether the results change without `--all-vulns`: every run used it, and the default run was not measured.
- Why Gradle resolves okhttp and okio to the `-jvm` artifacts in the boot jar: not investigated.
- Exploitability and severity of any finding: not assessed. Both tools were compared on identifiers only.
- Baseline age: the dependency-check artifact is 7 days old. A same-day dependency-check run was not made (it needs an NVD update and a 3 GB heap).

## Reproduce

1. `D=$(mktemp -d /tmp/osv-cmp-261116-005.XXXXXX)`, then in it `gh release download v2.7.1 --repo google/osv-scanner -p osv-scanner_linux_amd64 -p osv-scanner_SHA256SUMS`. Run `sha256sum -c --ignore-missing osv-scanner_SHA256SUMS` and compare with `gh release view v2.7.1 --repo google/osv-scanner --json assets`.
2. `gh run list --workflow security-scan.yml --status success --limit 1`, then `gh run download <id> -n dependency-check-report`. Check `git diff --stat <headSha> HEAD -- build.gradle settings.gradle gradle/` and `python3 -I scripts/verify-dependency-check-report.py --report <json> --tree <tree>`.
3. Load `parse_tree` from `scripts/verify-dependency-check-report.py` with `importlib.util.spec_from_file_location`, run it on `runtime-classpath-tree.txt` and write the 157 `group,artifact,version` rows.
4. Input 1: `osv-scanner scan source -L gradle/verification-metadata.xml --format json --all-packages --all-vulns --output-file osv-vm.json`.
5. Input 2: `./gradlew --no-daemon bootJar`, copy the jar into its own directory, then `docker run --rm --user "$(id -u):$(id -g)" -e TRIVY_CACHE_DIR=/cache -v <cache>:/cache -v <workdir>:/work -w /work <TRIVY_IMAGE from scripts/trivy-scan.sh> rootfs --format cyclonedx --pkg-types library --skip-version-check --timeout 30m --output bootjar.cdx.json jar`, then `osv-scanner scan source -L bootjar.cdx.json --format json --all-packages --all-vulns --output-file osv-sbom.json`. The SBOM filename must end `.cdx.json`.
6. Join in duckdb on (group:artifact, CVE id or OSV alias): dependency-check `dependencies[].vulnerabilities[]` against osv `results[].packages[].groups[]` (ids plus aliases). Runtime scope is exact group:artifact:version membership in the step 3 set.
7. Classify each dependency-check-only CVE with `curl https://api.osv.dev/v1/vulns/<CVE>`, following one alias hop to the GHSA record when the CVE record lists no Maven package.
