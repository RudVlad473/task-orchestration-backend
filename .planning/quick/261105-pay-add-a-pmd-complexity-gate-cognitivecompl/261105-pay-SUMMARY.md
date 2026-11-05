---
phase: quick-261105-pay
plan: 01
subsystem: build-quality
tags: [pmd, complexity, gradle, dependency-verification, code-style]
requires: []
provides:
  - pmdMain complexity gate on hand-written main code (CognitiveComplexity, NcssCount)
  - CODE_STYLE and rubric rule 15
affects: [build.gradle, gradle/verification-metadata.xml, .githooks/pre-commit, docs]
tech-stack:
  added: [PMD 7.29.0 (Gradle core pmd plugin)]
  patterns: [measured ceiling = worst offender + 1, dependsOn wiring from test and fastTest]
key-files:
  created:
    - .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-measure.txt
    - .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-proof.txt
  modified:
    - build.gradle
    - gradle/verification-metadata.xml
    - .githooks/pre-commit
    - docs/CODE_STYLE.md
    - docs/CODE_REVIEW_RUBRIC.md
    - docs/ARCHITECTURE.md
    - docs/learning/09-build-quality-and-ci.md
decisions:
  - "PMD pinned to exactly 7.29.0; it ran against Gradle 8.11.1 with no step-down"
  - "Ceilings are measured worst + 1: cognitive 27, method NCSS 37, type NCSS 191"
  - "Main code only, source restricted to src/main/java so Avro output is never analyzed"
  - "Wired by dependsOn from test and fastTest, not into bootJar"
metrics:
  duration: about 70 minutes
  completed: 2026-11-05
status: complete
actuals:
  tokens: 60000
  tasks: 3
  commits: 1
plan_head_before: 845c764ba96e05190f51de47be6ea86766f41140
plan_head_after: 4b596d04faca3554e5bce66c6a521ac9fefb0140
---

# Phase quick-261105-pay Plan 01: PMD complexity gate Summary

PMD 7.29.0 CognitiveComplexity and NcssCount now gate hand-written main code through `pmdMain`, with each ceiling set to the measured worst offender plus one and wired into `test` (CI) and `fastTest` (pre-commit hook).

## Result

Commit: `4b596d0` build(pmd): gate main-code complexity at the measured worst offenders (9 files, hook ran, exit 0).

Ceilings and their offenders (full table in 261105-pay-measure.txt):

| Measure | Worst (measured) | Ceiling | Offender | PMD default |
|---|---|---|---|---|
| CognitiveComplexity | 26 | 27 | ComposedConstraintPropertyCustomizer.translateDotAndWhitespaceShorthand | 15 |
| NCSS per method or constructor | 36 | 37 | same method | 60 |
| NCSS per type | 190 | 191 | ComposedConstraintPropertyCustomizer | 1500 |

Population: 134 .java files under src/main/java, 120 reported at level 1. The other 14 are bodyless one-line event records (BoardCreatedEvent read directly; the other 13 inferred from names). Zero processing errors, zero /build/ files, exactly two distinct rules.

Generated-source contrast: without the source restriction the run analyzed 134 files, 14 of them under build/generated-main-avro-java, with a generated type NCSS of 292, which would exceed the 191 ceiling.

## Proof (direction checked: both)

Must-fail (exit 1, exactly one violation naming the expected offender): P2 cognitive 27 to 26, P3 method NCSS 37 to 36, P4 type NCSS 191 to 190, P5 a probe method whose cognitive complexity equals the ceiling exactly (27) fails with no NcssCount message. Must-pass (exit 0): P1 clean tree, P6 the probe one point lower. P7 shows `sourceSets = [sourceSets.main]` keeps :pmdTest off check. P8 shows test and fastTest list :pmdMain and bootJar lists no pmd task. P7 and P8 are dry runs (`-m`), so they prove task-graph membership, not execution. Details: 261105-pay-proof.txt.

## Cost

- Full analysis (cache deleted before each run), warm daemon: 5.86, 5.70, 5.84 s, median 5.84 s. With cache: 4.38, 4.29, 4.60 s, median 4.38 s. The 30 s stop rule did not trip. A cold-daemon pmdMain was not measured.
- Peak RSS of the PMD worker that appeared for the run: 283908 KB (about 277 MB). A second, older worker from earlier runs was also resident at about 253 MB, so the PMD path can hold two workers of 250 to 280 MB each on the 8 GB host.

## Dependency verification

First regeneration with the build.gradle recipe added 120 lines. A strict cold `./gradlew --refresh-dependencies pmdMain --rerun` then failed on two POMs the recipe missed (checker-qual-3.55.1.pom, pcollections-4.0.2.pom). One extra pass, `--write-verification-metadata sha256 --refresh-dependencies pmdMain --rerun`, added them (126 added, 0 deleted, 15 new components, all PMD dependencies). Strict run then exit 0. runtimeClasspath before and after: identical.

## Deviations from Plan

**1. [Rule 3 - Blocking] Metadata regeneration under-captured two POMs**
- **Found during:** Task 1 step 9 (strict cold re-resolution)
- **Issue:** the verbatim recipe left checker-qual-3.55.1.pom and pcollections-4.0.2.pom without checksums, so strict resolution of the pmd configuration failed.
- **Fix:** one more `--write-verification-metadata sha256 --refresh-dependencies pmdMain --rerun` pass.
- **Files modified:** gradle/verification-metadata.xml
- **Commit:** 4b596d0
- Follow-up worth considering: the build.gradle recipe comment could name this second pass for a new build-time tool configuration; I did not change that comment (outside the plan).

**2. [Rule 3 - Blocking] First hooked commit attempt aborted on a Gradle daemon stop**
- The hook failed with "Gradle build daemon has been stopped: stop command received", about 1 s into fastTest. Per SESSION_LESSONS 9 I checked memory first (1.7 GiB free, 3.8 GiB available, no memory pressure). The daemon log recorded a stop command at 18:39:26 and nothing in my steps issues one. I did not identify who sent it. I re-ran the same hooked commit once, without --no-verify, and it passed. The first attempt's log is 261105-pay-commit-hook-attempt1.txt.

**3. [Rule 1 - Process] Comment lint adjustments**
- verify-comments.py rejected the first draft of the build.gradle section header (first paragraph over 2 lines). I shortened the summary and moved the measure-file path under Decisions. No behavior change.

**4. Commit trailer**
- The plan text names `Claude Opus 5.5` in the trailer; the executor instructions name `Claude Sonnet 5.5`. I used the instruction version.

## Not verified / caveats

- In the hooked commit's own log, `> Task :pmdMain UP-TO-DATE` and `> Task :fastTest UP-TO-DATE` appear: pmdMain was in the fastTest graph on the commit path, but it did not execute in that run (inputs unchanged since my previous run), and neither did the tests. Fresh execution of pmdMain is shown by P1 to P6 and by the `spotlessCheck test` regeneration run (5m 1s, BUILD SUCCESSFUL, pmdMain executed as test's dependency). I did not separately run fastTest to completion with pmdMain executing in it.
- The temporary build.gradle mutation for P7 used a one-off python replace and a file copy to restore, not Edit; the final sha256 equals the starting value (recorded in the proof file).
- Cold-daemon cost of pmdMain, and the CI-side `dependency-submission.yml` resolution of the new configuration, were not run.
- The cause of the first attempt's daemon stop is unknown.
- README.md build-gate rows (lines 97 and 237) were deliberately left unchanged (default for Q1). No ast-grep CI check was added (Q3).

## Known Stubs

None.

## Threat Flags

None. Mitigations T-261105-pay-SC (exact pin plus checksums, strict run passes), T-261105-pay-01 (runtimeClasspath identical, bootJar lists no pmd task) and T-261105-pay-03 (cost measured, under the stop rule) are covered above.

## Untracked files left for the orchestrator

- .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-commit-hook.txt (second, successful hook run; written after the commit)
- .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-commit-hook-attempt1.txt (aborted first attempt)
- 261105-pay-PLAN.md and this SUMMARY.md (not committed by me, per instructions)

Cleanup: scratch files under build/tmp deleted, `./gradlew --stop` run, no Testcontainers containers left (the freehire containers were untouched).

## Self-Check: PASSED

- Commit 4b596d0 exists on main and lists exactly the nine planned paths; `git rev-list --count 845c764..HEAD` is 1.
- Files exist: 261105-pay-measure.txt, 261105-pay-proof.txt, 261105-pay-commit-hook.txt (rg `> Task :pmdMain` hits).
- build.gradle sha256 matches its pre-mutation value; probe file deleted.
