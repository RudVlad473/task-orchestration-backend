---
phase: quick-261105-pay
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - build.gradle
  - gradle/verification-metadata.xml
  - .githooks/pre-commit
  - docs/CODE_STYLE.md
  - docs/CODE_REVIEW_RUBRIC.md
  - docs/ARCHITECTURE.md
  - docs/learning/09-build-quality-and-ci.md
  - .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-measure.txt
  - .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-proof.txt
  - .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-commit-hook.txt
autonomous: true
requirements: [261105-pay]

estimate:
  tokens: 130000
  raw_tokens: 130000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "`./gradlew pmdMain --rerun` passes on the committed tree under strict dependency verification, with zero processing errors in its report"
    - "Lowering any one of the three ceilings by one makes pmdMain fail and name that measure's worst offender from the measurement"
    - "A new main-code method whose cognitive complexity equals the ceiling fails pmdMain, and the same method one point lower passes"
    - "`./gradlew test` (CI) and `./gradlew fastTest` (pre-commit hook) both run pmdMain before any test, while `bootJar` (the Docker build) does not and runtimeClasspath is byte-identical to before"
    - "No task on the check, test or fastTest path analyzes test sources or generated sources"
    - "Each ceiling equals its measured worst offender plus one, and 261105-pay-measure.txt records the offenders, their values and the analyzed-file population"
    - "docs/CODE_STYLE.md rule 15 tells authors to name intermediate Stream and List locals instead of nesting Stream.concat and filter calls, and says the gate does not measure that nesting"
    - "The commit that lands the gate ran .githooks/pre-commit, and its captured output shows the pmdMain task in the fastTest graph"
  artifacts:
    - path: "build.gradle"
      provides: "pmd plugin, PMD 7.29.0 pin, inline two-rule ruleset with measured ceilings, main-only scope, test and fastTest wiring"
      contains: "category/java/design.xml/CognitiveComplexity"
    - path: "gradle/verification-metadata.xml"
      provides: "sha256 entries for net.sourceforge.pmd and its transitive artifacts"
      contains: "net.sourceforge.pmd"
    - path: ".planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-measure.txt"
      provides: "Measured worst offenders per measure, file population, generated-source contrast, chosen ceilings, new metadata components"
    - path: ".planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-proof.txt"
      provides: "Must-fail and must-pass runs P1 to P8 with exit codes, plus time and memory cost"
    - path: "docs/CODE_STYLE.md"
      provides: "Rule 15, enforced floor plus reviewed nesting guidance"
      contains: "### 15."
    - path: "docs/CODE_REVIEW_RUBRIC.md"
      provides: "Rule 15 rubric entry with the deciding test and the measured existing-site count"
      contains: "### 15."
  key_links:
    - from: "tasks.named('test')"
      to: "pmdMain"
      via: "dependsOn, because CI runs test and spotlessCheck and never check"
      pattern: "dependsOn 'pmdMain'"
    - from: "tasks.named('fastTest')"
      to: "pmdMain"
      via: "dependsOn, because .githooks/pre-commit runs fastTest"
      pattern: "dependsOn 'pmdMain'"
    - from: "pmd extension"
      to: "pmdMain only"
      via: "sourceSets = [sourceSets.main] keeps pmdTest off check; source restricted to src/main/java keeps Avro output out"
      pattern: "sourceSets = \\[sourceSets.main\\]"
---

<objective>
Add a build gate that fails when hand-written main code grows a method, constructor or type more complex
than the worst one in the tree today, measured by PMD's CognitiveComplexity and NcssCount rules. Add
CODE_STYLE rule 15 telling authors to name intermediate Stream and List locals instead of nesting
Stream.concat and filter calls.

Purpose: the user saw google-java-format (AOSP) staircase-indent the nested
Stream.concat(Stream.concat(mockTasks.stream().filter(...))) in TaskServiceTest. No current gate
(Spotless, Error Prone, ArchUnit, JaCoCo) measures complexity at all. The user chose PMD complexity
rules on main code, with the ceiling taken from the measured worst offenders so the gate is green on
day one, knowing these rules count branches and statements, not argument nesting depth.

Output: a measured, pinned, main-only PMD gate wired so CI and the pre-commit hook both run it,
regenerated dependency-verification metadata, a recorded measurement and a recorded red/green proof,
rule 15 in CODE_STYLE.md with its rubric entry, and one commit that ran the hook.

Core data flow (3 sentences): Gradle's built-in pmd plugin registers pmdMain, which parses every file
under src/main/java with PMD 7.29.0 in a forked worker JVM and evaluates two rules from an inline
ruleset whose report levels are the measured worst values plus one. The test task (what CI runs) and
the fastTest task (what the pre-commit hook runs) both depend on pmdMain, so a method or type at or
above a ceiling fails the build before any test starts and prints the offender to the console. Test
sources, Avro-generated sources and the Docker bootJar path never reach PMD.
</objective>

## Design record (required by .claude/CLAUDE.md before a PLAN.md is approved)

The user chose PMD with a ceiling taken from measurement. This section records what else was weighed.

### Approaches considered

1. **PMD through Gradle's core `pmd` plugin, CognitiveComplexity plus NcssCount, ceiling = measured
   worst + 1, wired by `dependsOn` from test and fastTest.** Picked.
2. **Checkstyle through Gradle's core `checkstyle` plugin** (CyclomaticComplexity, NPathComplexity,
   JavaNCSS, NestedIfDepth). Not picked.
3. **PMD at its published default levels (CognitiveComplexity 15, NcssCount 60 per method and 1500
   per type), with today's offenders suppressed per site** (`@SuppressWarnings("PMD.CognitiveComplexity")`
   or `// NOPMD`). Not picked.
4. Wiring variants for approach 1: hang pmdMain on spotlessCheck, or name `pmdMain` explicitly in
   .github/workflows/deploy.yml and .githooks/pre-commit. Not picked.

### Trade-off matrix

| Approach | Pros / Cons | Why Picked |
|---|---|---|
| 1. PMD, two rules, measured ceilings, test and fastTest dependsOn | + Has a cognitive-complexity metric, which increments for nesting and nested lambdas, the closest signal to the user's complaint. + Core plugin, so no plugin-portal coordinate. + Ceilings come from the tree, so it is green on day one. + `dependsOn` from test mirrors verifyRuntimeDependencyFloor, so the CI command enforces it alone. − Adds a forked worker JVM and roughly 20 new build-time artifacts. − A ceiling at the worst offender stops new outliers, not drift below it. | Picked: the user's decision, and the only option with a nesting-aware metric. |
| 2. Checkstyle complexity checks | + Same core-plugin cost profile. + NestedIfDepth sees block nesting. − No cognitive-complexity metric, so lambdas nested inside stream calls add nothing. − NPathComplexity grows exponentially on sequential ifs, which makes a measured ceiling noisy. − Needs its own XML config and version pin. | Not picked: it misses the lambda-nesting signal and still does not measure argument depth. |
| 3. PMD defaults plus per-site suppression | + New code is held to PMD's published levels at once. − Every existing offender gets a suppression annotation or NOPMD marker in main code, and each needs a rule-14 comment. − A suppression silences the method for good, so the worst offenders can grow unchecked. | Not picked: the user set the ceiling from the worst offenders, and suppressions would mean editing main code just to land a build gate. |
| 4. spotlessCheck dependsOn pmdMain, or explicit task names in deploy.yml and the hook | + One wiring point, or visible in the workflow. − Gives the formatting task a compile dependency it does not have today. − Explicit names are two edits that drift apart, and a direct `./gradlew test` would skip the gate. | Not picked: the repo already wires gates this way (`dependsOn 'verifyRuntimeDependencyFloor'`, `finalizedBy jacocoTestCoverageVerification`), because CI never runs check. |

### Non-obvious trade-offs

- **Pre-commit and CI time.** pmdMain runs in a process-isolated worker JVM (Gradle 8.11.1
  `Pmd.java` line 92, `processIsolation`), so every hook run pays a worker start plus a parse of
  about 134 files. PMD's incremental cache, at build/tmp/pmdMain/incremental.cache, is keyed on the
  aux classpath, which includes main's compiled classes. So any commit that changes Java pays for a
  full analysis, and the cache helps only when nothing changed. Task 2 measures both paths. The stop
  rule: if the median full-analysis run exceeds 30 seconds (about 12% of the roughly 4-minute hook),
  do not wire it into fastTest, and report this instead.
- **Memory.** gradle.properties caps the daemon at 768 MB because of WSL memory incidents
  (docs/SESSION_LESSONS.md, 13-07). The PMD worker is one more JVM, and an idle worker daemon can
  stay alive while fastTest's two test forks run. Task 2 records its peak RSS.
- **Dependency verification.** gradle/verification-metadata.xml needs sha256 entries for the `pmd`
  configuration: pmd-java and pmd-ant (Gradle adds pmd-ant for PMD 7, `PmdPlugin.java` lines
  198-204) and their transitive dependencies. They are regenerated with the recipe in build.gradle's
  comment (`--refresh-dependencies` is mandatory). These artifacts sit on the build classpath only and
  never reach runtimeClasspath or the bootJar image. That also puts them outside the OWASP scan, which
  scans runtimeClasspath only, an accepted gap for build-time tooling.
  .github/workflows/dependency-submission.yml resolves every configuration, `pmd` included, so the
  metadata must cover it fully.
- **Failing on existing code.** Both rules report at greater-or-equal (PMD 7.29.0
  `CognitiveComplexityRule` line 58 `cognitive >= reportLevel`, `NcssCountRule` lines 90 and 108).
  So ceiling = worst + 1 is the tightest level that passes today. The worst offender cannot grow by
  even one point, and every other method can still grow up to that level.
- **The nesting gap.** Neither metric counts how deeply call arguments nest. The motivating site is in
  test code, which this gate excludes by the user's decision. Its shape is argument nesting, not
  branching, so the gate would not flag it even in main code. Rule 15's nesting half is held by
  review (rubric rule 15), not by this gate.
- **Version drift.** The rule definitions are versioned by the PMD release (how lambdas or switch arms
  count). So PMD is pinned exactly, for the same reason error_prone_core is, and the gate stays a pure
  function of the commit (docs/learning/09 CI-01). Gradle 8.11.1's bundled default is PMD 6.55.0
  (`PmdPlugin.DEFAULT_PMD_VERSION`), which is too old to rely on for this codebase. 7.29.0 is the
  newest 7.x on Maven Central (maven-metadata lastUpdated 2026-10-30, read 2026-11-05).
- **Silent skips.** A file PMD cannot parse produces a processing error, not a violation, and its
  rules never run. A green run with processing errors would be a false clean. Every recorded run
  therefore checks the report for zero `<error` elements.

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@build.gradle
@docs/CODE_STYLE.md
@docs/CODE_REVIEW_RUBRIC.md
@.githooks/pre-commit
@docs/SESSION_LESSONS.md

STATE.md and ROADMAP.md do not exist (between milestones), so ignore them.
docs/ARCHITECTURE.md: read only the "## Build quality gates" section (about lines 504-527).
docs/learning/09-build-quality-and-ci.md: read only lines 60-80 (the hard-gate list).

Facts verified while planning (2026-11-05), so do not re-derive them:
- CI (.github/workflows/deploy.yml run-tests job) runs `./gradlew test` and `./gradlew spotlessCheck`
  only. The Dockerfile runs `./gradlew bootJar` only. The hook runs `./gradlew spotlessCheck`, then
  `./gradlew fastTest`.
- Gradle wrapper 8.11.1. The Pmd task runs PMD in a processIsolation worker. The PmdExtension `ruleSets`
  convention becomes empty when `ruleSetConfig` is set. The `sourceSets` property is the set of
  source sets analyzed as part of check and build. incrementalAnalysis defaults to true.
- The pmdMain aux classpath includes `sourceSet.output`, so pmdMain depends on compileJava.
- gradle-avro-plugin adds build/generated-main-avro-java to sourceSets.main.java, so pmdMain would
  analyze generated Avro records unless its source is restricted.
- PMD 7.29.0 messages: CognitiveComplexity "The {method|constructor} 'sig' has a cognitive complexity
  of N, current threshold is T". NcssCount on a method "The method 'sig' has a NCSS line count of N."
  NcssCount on a type "The class 'Name' has a NCSS line count of N (Highest = M)." The kind word can
  also be record, enum or interface. Only the type form contains "(Highest =".
- Both rules require positive integer levels, so 1 is the lowest valid measurement level.
- Baselines at HEAD 845c764: `rg -c "id 'pmd'" build.gradle` finds nothing. No `### 15.` heading
  exists in docs/CODE_STYLE.md or docs/CODE_REVIEW_RUBRIC.md. No NOPMD or "PMD" suppression exists in
  src/main/java or src/test/java. 134 .java files sit under src/main/java. The ast-grep pattern
  `Stream.concat(Stream.concat($$$A), $$$B)` matches 1 site in src/, in
  src/test/java/com/vrudenko/kanban_board/service/TaskServiceTest.java.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Wire the PMD complexity gate end-to-end, with ceilings set from a measured run</name>
  <files>build.gradle, gradle/verification-metadata.xml, .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-measure.txt</files>
  <action>
Work in the main checkout on main (git.allow_default_branch_commits is true). Leave the pre-existing
untracked files listed by git status alone, and commit nothing in this task: Task 3 makes the single
commit. Every file deletion in this plan (the probe file, the PMD cache file, the scratch files) uses
`trash-put <exact path>`: `rm` is denied on this machine, and retrying it in another spelling is not
allowed.

1. Baseline, before any edit: write the output of `./gradlew -q dependencies --configuration runtimeClasspath`
   to build/tmp/261105-pay-runtime-before.txt (build/ is gitignored and inside the repo).

2. Plugin: in the plugins block, add `id 'pmd'` directly after the `id 'jacoco'` entry. Give it a
   plain-text comment (CODE_STYLE rule 14): one summary line saying it is the main-code complexity gate
   configured in the PMD section further down, and that it ships inside Gradle core like jacoco, so no
   plugin coordinate resolves, only the PMD artifacts its toolVersion pins.

3. Gate section: add a new section headed `// --- PMD complexity gate ---` after the compileTestJava
   Error Prone block and before the git hooks bootstrap section. It holds:
   - Three Groovy variables: pmdCognitiveComplexityCeiling, pmdMethodNcssCeiling and pmdTypeNcssCeiling,
     all set to 1 for now (measurement levels).
   - A `pmd` block. toolVersion = '7.29.0'. sourceSets = [sourceSets.main], which keeps pmdTest off
     check (test code excluded by user decision). ruleSets = [], explicit even though the convention
     already empties it when ruleSetConfig is set. consoleOutput = true. ruleSetConfig =
     resources.text.fromString of a PMD ruleset 2.0.0 document. The document uses the
     http://pmd.sourceforge.net/ruleset/2.0.0 namespace and has a description element. It holds one
     rule ref to category/java/design.xml/CognitiveComplexity, with property reportLevel set to the
     cognitive ceiling variable, and one rule ref to category/java/design.xml/NcssCount, with
     properties methodReportLevel and classReportLevel set to the two NCSS ceiling variables. Use a
     triple-double-quoted GString so the variables interpolate.
   - `tasks.named('pmdMain')`, which replaces the task's source with the hand-written tree only
     (`source = fileTree('src/main/java')`), so the Avro output directory that gradle-avro-plugin adds
     to sourceSets.main.java is never analyzed.
   - `tasks.named('test') { dependsOn 'pmdMain' }` and `tasks.named('fastTest') { dependsOn 'pmdMain' }`.
     Keep both in this section, beside the gate they wire. dependsOn rather than finalizedBy: the gate
     needs nothing test produces, and it should fail in seconds, before the suite.

4. Measurement run, expected to FAIL because every level is 1. Run
   `./gradlew --dependency-verification lenient pmdMain --rerun --console=plain`. Lenient mode is
   needed because the PMD artifacts have no checksums yet. Confirm that build/reports/pmd/main.xml
   exists. If Gradle wrote no XML report, add `reports { xml.required = true }` to the pmdMain
   configuration and rerun.
   If the task fails for a reason other than rule violations (an invoker or class-loading error from
   Gradle 8.11.1 against PMD 7.29.0), record the error. Then step down to the newest 7.x release that
   runs, record why, and use that version everywhere this plan says 7.29.0.

5. Record the measurement in 261105-pay-measure.txt, plain text:
   - Header: date, `git rev-parse HEAD`, Gradle 8.11.1, the PMD version actually used, and the command.
   - Population: the count of .java files under src/main/java (`rg --files src/main/java -g '*.java'`,
     134 at planning time) against the count of distinct file elements in the report. With levels at
     1, nearly every file reports at least its type's NCSS, so the two counts should be close. Name
     any file that is absent.
   - Zero file names containing /build/ in the report.
   - The count of `<error` elements, which must be 0. If it is not, stop and report: an unparsed file
     is a silent skip.
   - The distinct rule attributes in the report, which must be exactly CognitiveComplexity and
     NcssCount.
   - For each of the three measures (cognitive complexity per method or constructor, NCSS per method
     or constructor from messages without "(Highest =", NCSS per type from messages with
     "(Highest ="): the maximum value, the top 5 offenders by value with file path and quoted name,
     and PMD's default level beside it (15, 60, 1500) for reference.
   - The chosen ceilings, each exactly max + 1, with the reason: both rules report at greater-or-equal
     (PMD 7.29.0 CognitiveComplexityRule line 58, NcssCountRule lines 90 and 108). Do not pick any
     other number. If a measured max falls below PMD's default, still use max + 1 (user decision) and
     note the gap in the file.

6. Generated-source contrast, which shows the source restriction earns its place. Temporarily delete
   the `source = fileTree('src/main/java')` line and rerun the step 4 command. Append to the measure
   file the count of report files whose path contains /build/, and the maximum of each measure among
   them. Restore the line with Edit and confirm it is back (`rg -n "fileTree\('src/main/java'\)" build.gradle`).

7. Set the three ceiling variables to the step-5 ceilings. Then write the section's comments (CODE_STYLE
   rule 14: plain text, a one- or two-line summary, a blank comment line, then `Decisions:` and
   `Known holes:` blocks):
   - Summary: complexity gate on hand-written main code, CognitiveComplexity and NcssCount, with each
     ceiling one above the worst offender measured on the date, citing the measure file path.
   - Decisions: the exact PMD pin and why (rule definitions are versioned by the PMD release, so a
     floating version could red an unchanged commit; Gradle 8.11.1's own default is 6.55.0). Ceiling =
     worst + 1 because both rules report at greater-or-equal. Main only (user decision; sourceSets
     keeps pmdTest off check). Source restricted to src/main/java because the Avro plugin adds its
     output to the main source set (quote the step-6 counts). Wired through test and fastTest because
     CI runs test and spotlessCheck and never check, and the hook runs fastTest. Not wired into bootJar,
     so the Docker build is unchanged. Ratchet direction: lower a ceiling when its worst offender is
     refactored, never raise it to admit new code.
   - Known holes: neither metric counts nested call-argument depth (CODE_STYLE rule 15, held by
     review). A ceiling at the worst offender stops new outliers, not growth below it. Test code is
     unchecked. NOPMD and SuppressWarnings PMD markers would bypass it (zero in the tree on the date).
   Also add pmdMain to the list of pure-function gates in the OWASP plugin comment (the line naming
   "Spotless, Error Prone, JaCoCo, ArchUnit, verifyRuntimeDependencyFloor"), because a pinned PMD
   yields the same verdict for the same commit forever.

8. Regenerate the verification metadata with the build.gradle recipe, verbatim:
   `./gradlew --write-verification-metadata sha256 --refresh-dependencies spotlessCheck test`. It runs
   the full suite (about 5 minutes) with pmdMain as test's dependency, which must pass at the final
   ceilings. Run it in the background and bound the wait: no new log output for 15 minutes means it is
   hung. Then read `git diff --numstat gradle/verification-metadata.xml` (added and deleted line
   counts, with no pipeline that could hide a git failure) and the diff itself. Expect a deleted count
   of 0, and new components only for net.sourceforge.pmd and its transitive dependencies. Explain any other change
   in the measure file. Append the list of newly added component groups and names.

9. Strict, cold re-resolution (the check that once caught under-captured metadata):
   `./gradlew --refresh-dependencies pmdMain --rerun` must exit 0 under default strict verification.
   Then write runtimeClasspath again to build/tmp/261105-pay-runtime-after.txt and `diff` it against
   the step-1 file. It must be identical. Record both results in the measure file, the second as the
   exact line "runtimeClasspath before/after: identical" (or the diff, if it is not).

10. `./gradlew spotlessCheck` and `python3 scripts/verify-comments.py check` must both exit 0.
  </action>
  <verify>
    <automated>./gradlew pmdMain --rerun && ./gradlew -m test | rg ':pmdMain' && ./gradlew -m fastTest | rg ':pmdMain' && python3 scripts/verify-comments.py check && rg -n "category/java/design.xml/CognitiveComplexity" build.gradle && rg -n "net.sourceforge.pmd" gradle/verification-metadata.xml && rg -q '^runtimeClasspath before/after: identical$' .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-measure.txt</automated>
  </verify>
  <done>pmdMain passes under strict verification at ceilings equal to measured max + 1. 261105-pay-measure.txt records the population, zero processing errors, zero build/ files, the top offenders, the generated-source contrast and the new metadata components. test and fastTest list pmdMain in their task graphs. runtimeClasspath is unchanged. The comment lint and spotlessCheck pass.</done>
</task>

<task type="auto">
  <name>Task 2: Prove the gate fails on violations and passes on the tree, and measure what it costs the hook</name>
  <files>.planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-proof.txt</files>
  <precondition>Task 1's verify command exits 0, so pmdMain passes under strict verification at the final ceilings.</precondition>
  <action>
Every mutation below is temporary: make it with Edit, run, then undo it with Edit. Start by recording
`sha256sum build.gradle` at the top of 261105-pay-proof.txt. Write one line per check, starting with
its ID (P1 to P8, then a space), the direction (must-pass or must-fail), the mutation, the command,
the exit code, and the deciding output line (the violation message, or the task outcome). Read the
exit code, not just the console: a crashed run and a clean run can print alike. Use `--rerun` on
pmdMain so a pass is a real execution, not UP-TO-DATE, and confirm the console shows the pmdMain task
executing.

- P1 must-pass: the clean tree. `./gradlew pmdMain --rerun` exits 0, and the report has zero
  violation and zero `<error` elements.
- P2 must-fail: pmdCognitiveComplexityCeiling lowered by one. The failure names the worst
  cognitive-complexity offender from the measure file. Restore.
- P3 must-fail: pmdMethodNcssCeiling lowered by one. The failure names the worst method-NCSS offender.
  Restore.
- P4 must-fail: pmdTypeNcssCeiling lowered by one. The failure names the worst type-NCSS offender (a
  message with "(Highest ="). Restore.
- P5 must-fail: create src/main/java/com/vrudenko/kanban_board/PmdGateProbe.java, a final class with
  one static int method. Its cognitive complexity must equal the cognitive ceiling exactly, and its
  NCSS must stay below the method NCSS ceiling. Nested ifs add 1 plus their nesting depth, and
  sequential top-level ifs add 1 each. So reach the value with a few nested ifs, then fill the
  remainder with sequential top-level ifs, keeping at least one top-level if. It must compile under
  Error Prone. The only violation must be a CognitiveComplexity message naming PmdGateProbe that
  reports exactly the ceiling value, and no NcssCount message may name the probe.
- P6 must-pass: remove one top-level if from the probe with Edit. pmdMain exits 0. Then delete the
  probe file and confirm `test ! -e` on its path.
- P7 must-fail then must-pass: delete the `sourceSets = [sourceSets.main]` line. `./gradlew -m check`
  then lists :pmdTest (proving the line keeps test code off check). Restore it, and `./gradlew -m check`
  lists no :pmdTest. Record both runs on the P7 line.
- P8 wiring: `./gradlew -m test` and `./gradlew -m fastTest` list :pmdMain and do not list :pmdTest.
  `./gradlew -m bootJar` lists neither (the Docker build path is unchanged).

Cost, appended under a "Cost" heading:
- Make compileJava up to date first (`./gradlew compileJava`). Then time three full-analysis runs of
  `./gradlew pmdMain --rerun`, deleting the incremental cache file before each one (confirm its path
  with ls; Gradle's default is build/tmp/pmdMain/incremental.cache). This is the cost on any commit
  that changes Java. Then time three runs with the cache present. Record each wall time and the median
  of each set.
- During one full-analysis run, sample `ps -eo pid,rss,args` every half second for processes whose
  args contain GradleWorkerMain. Record the peak RSS in MB of the worker that appeared for the run.
- Stop rule (from the design record): if the full-analysis median exceeds 30 seconds, stop. Report it
  as a blocker in the SUMMARY with the numbers, and do not proceed to Task 3's commit. Wiring it into
  fastTest would then be the user's call.

Finish: `sha256sum build.gradle` must equal the value recorded at the top. `git status --short` must
show no probe file and no path outside this plan's files_modified, other than the pre-existing
untracked files.
  </action>
  <verify>
    <automated>test "$(rg -c '^P[1-8] ' .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-proof.txt)" -ge 8 && test ! -e src/main/java/com/vrudenko/kanban_board/PmdGateProbe.java && ./gradlew pmdMain --rerun && M=$(./gradlew -m check) && printf '%s\n' "$M" | rg -q ':pmdMain' && ! printf '%s\n' "$M" | rg -q ':pmdTest'</automated>
  </verify>
  <done>261105-pay-proof.txt holds P1 to P8 with exit codes. Every must-fail run failed for the named rule and offender, and every must-pass run exited 0 with zero processing errors. The cost section records full-analysis and cached medians and the worker's peak RSS. build.gradle is byte-identical to its state at the start of the task, and the probe file is gone.</done>
</task>

<task type="auto">
  <name>Task 3: Add CODE_STYLE rule 15 and its rubric entry, list the gate in the docs, correct the hook message, and commit through the hook</name>
  <files>docs/CODE_STYLE.md, docs/CODE_REVIEW_RUBRIC.md, docs/ARCHITECTURE.md, docs/learning/09-build-quality-and-ci.md, .githooks/pre-commit</files>
  <action>
1. docs/CODE_STYLE.md: append `### 15.` after rule 14, following "Adding a rule" (the next integer,
   one statement line, then Enforced and Reviewed lines). Heading: "15. Name intermediate Stream and List
   values instead of nesting calls; keep methods under the complexity ceiling". Content:
   - One statement: name each intermediate Stream or List as a local and then combine the locals; do not
     pass one Stream.concat, filter or map pipeline as an argument of another.
   - Why, in one sentence: google-java-format (AOSP) indents each nested argument a further step, so a
     two-deep Stream.concat reads as a staircase that has to be unwound from the inside out.
   - "Enforced, as a floor:" pmdMain (build.gradle) fails on a main-code method or constructor whose
     PMD CognitiveComplexity or NCSS reaches the ceiling measured on 2026-11-05, and on a type whose
     NCSS does. Test code is not checked.
   - "Reviewed:" rubric rule 15, for nested call arguments, which no enforced check sees. ArchUnit
     reads bytecode, where the nesting is gone (the same reason as rule 9). PMD's metrics count
     branches and statements, not argument depth.

2. docs/CODE_REVIEW_RUBRIC.md: add `### 15.` with the identical heading, after the rule 14 entry.
   Shape it like the existing entries:
   - The rule.
   - "**Why:**" with the staircase reason.
   - The preferred shapes: a named local per intermediate stream or list, or Stream.of(a, b, c)
     flattened once, for three or more parts.
   - The existing example: the expectedIds assignment in TaskServiceTest's delete test is the shape to
     avoid. Re-measure the count with `ast-grep --lang java -p 'Stream.concat(Stream.concat($$$A), $$$B)' src/main/java src/test/java`
     (1 site at planning time) and write it as "Measured on <date>: N site(s)". Grade new lines only,
     as the rubric header says.
   - "**Deciding test:**" Does a changed expression pass the result of a stream-producing call
     (Stream.concat, Stream.of, a .stream() pipeline) directly as an argument of another
     stream-producing call, two or more levels deep? That is a finding. A named local per inner value
     passes. The rubric header's sentence listing fully-enforced rules (3, 6, 7, 10, 11) stays as it
     is, because rule 15 is only partly enforced.

3. docs/ARCHITECTURE.md, "## Build quality gates": add a bullet after the ErrorProne bullet. It covers
   PMD complexity (pmdMain): CognitiveComplexity and NcssCount on hand-written main code, with test and
   Avro-generated sources excluded. Its ceilings are one above the worst offenders measured on
   2026-11-05, and PMD is pinned exactly. test and fastTest depend on it, so CI and the pre-commit hook
   run it and the Docker build does not. It does not measure nested call arguments (CODE_STYLE rule 15).

4. docs/learning/09-build-quality-and-ci.md, around line 68: add PMD complexity (`pmdMain`) to the
   sentence listing the hard gates. Change nothing else in that file.

5. .githooks/pre-commit, last section only: extend the comment above the fastTest call to say that
   fastTest also pulls in pmdMain, the complexity gate, through build.gradle. Change the failure echo to
   "Compile, complexity gate (pmdMain) or test failure. Commit aborted." Change no other line. Confirm
   that `sh -n .githooks/pre-commit` exits 0, that `git ls-files -s .githooks/pre-commit` still shows
   mode 100755, and that `test -x .githooks/pre-commit` passes.

6. Run `python3 scripts/verify-comments.py check` and `python3 scripts/verify-instruction-budget.py`;
   both must exit 0.

7. Commit once, through the hook. This commit is the end-to-end proof that the pre-commit path runs the
   gate, so never use --no-verify here, even though CLAUDE.md allows it for docs commits.
   - Write the message with the Write tool to build/tmp/261105-pay-commit-msg.txt: subject
     "build(pmd): gate main-code complexity at the measured worst offenders". The body says why: no gate
     measured complexity; the ceilings come from the measurement; test and fastTest wiring because CI
     never runs check; rule 15 covers the nesting the metrics cannot see. End with the line
     "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>".
   - `git add` exactly these paths: build.gradle, gradle/verification-metadata.xml,
     .githooks/pre-commit, docs/CODE_STYLE.md, docs/CODE_REVIEW_RUBRIC.md, docs/ARCHITECTURE.md,
     docs/learning/09-build-quality-and-ci.md, and the task directory's 261105-pay-measure.txt and
     261105-pay-proof.txt. Then run `git commit -F build/tmp/261105-pay-commit-msg.txt -- <the same paths>`
     directly, not through gsd-tools, which kills the commit at 30 seconds. Run it in the background with
     stdout and stderr redirected to the task directory's 261105-pay-commit-hook.txt. Expect about
     5 minutes. Treat no log growth for 15 minutes as hung.
   - If the hook fails with Gradle daemon deaths, diagnose memory per docs/SESSION_LESSONS.md 13-07
     before anything else, and do not bypass the hook.
   - After it exits 0: `rg -n '> Task :pmdMain' <task-dir>/261105-pay-commit-hook.txt` must hit, and
     `git log -1 --stat` must list exactly the paths above. The hook log itself is written after the
     commit, so leave it for the SUMMARY docs commit and name it in the SUMMARY.

8. Clean up: delete the build/tmp/261105-pay-* scratch files and run `./gradlew --stop`. Check
   `docker ps -a --filter label=org.testcontainers` for containers the runs left behind and remove only
   those.
  </action>
  <verify>
    <automated>test "$(rg -c '^### 15\. ' docs/CODE_STYLE.md)" -eq 1 && test "$(rg -c '^### 15\. ' docs/CODE_REVIEW_RUBRIC.md)" -eq 1 && test "$(rg -N '^### 15\. .*' docs/CODE_STYLE.md)" = "$(rg -N '^### 15\. .*' docs/CODE_REVIEW_RUBRIC.md)" && rg -n 'pmdMain' docs/ARCHITECTURE.md docs/learning/09-build-quality-and-ci.md .githooks/pre-commit && sh -n .githooks/pre-commit && python3 scripts/verify-comments.py check && rg -n '> Task :pmdMain' .planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-commit-hook.txt && test "$(git log -1 --format=%s)" = "build(pmd): gate main-code complexity at the measured worst offenders"</automated>
  </verify>
  <done>Rule 15 exists in CODE_STYLE.md and the rubric under identical headings, with the measured site count. ARCHITECTURE.md and learning/09 list the gate. The hook names pmdMain in its comment and failure message and keeps mode 100755. One commit holds the gate, the metadata, the docs and the two evidence files, and its captured hook output shows :pmdMain running in the pre-commit path.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Maven Central to the build | New build-time artifacts (pmd-java, pmd-ant and their transitive dependencies) are downloaded and executed in a worker JVM on developer machines and CI runners |
| Author to the gate | A committer can mark code to be skipped by the analyzer |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261105-pay-SC | Tampering | `pmd` configuration artifacts from Maven Central | high | mitigate | Exact pin to 7.29.0, plus sha256 entries generated by the documented `--write-verification-metadata sha256 --refresh-dependencies spotlessCheck test` recipe. The diff is reviewed to contain only net.sourceforge.pmd and its transitive dependencies, and a strict `--refresh-dependencies pmdMain` run passes (Task 1 steps 8 and 9). The npm, pip and cargo legitimacy gate does not apply; the coordinates were confirmed on repo1.maven.org on 2026-11-05. |
| T-261105-pay-01 | Elevation of Privilege | Production image contents | medium | mitigate | PMD stays build-time only. The Task 1 step 9 runtimeClasspath diff is identical, and Task 2 P8 shows bootJar does not run pmdMain. |
| T-261105-pay-02 | Tampering | The gate's verdict | low | accept | NOPMD comments and SuppressWarnings PMD markers bypass a rule. There are zero in the tree on 2026-11-05, any new one shows in the diff a reviewer reads, and the bypass is listed under Known holes in build.gradle. |
| T-261105-pay-03 | Denial of Service | Pre-commit hook on the memory-capped WSL host | low | mitigate | Task 2 measures full-analysis time and the worker's peak RSS, with a 30-second stop rule before the gate is wired into the commit path. |
</threat_model>

<verification>
- `./gradlew pmdMain --rerun` exits 0 under strict verification, with zero violation and zero error elements in build/reports/pmd/main.xml.
- `./gradlew -m test` and `./gradlew -m fastTest` list :pmdMain. `./gradlew -m check` lists no :pmdTest. `./gradlew -m bootJar` lists no pmd task.
- 261105-pay-measure.txt and 261105-pay-proof.txt exist with the contents Tasks 1 and 2 specify. Each must-fail line records a non-zero exit, and each must-pass line records exit 0.
- `python3 scripts/verify-comments.py check`, `python3 scripts/verify-instruction-budget.py`, `./gradlew spotlessCheck` and `sh -n .githooks/pre-commit` all exit 0.
- The commit-hook log shows `> Task :pmdMain`, and HEAD's subject starts with `build(pmd)`.
</verification>

<success_criteria>
- A main-code method, constructor or type more complex than today's worst fails CI (`./gradlew test`) and the pre-commit hook (`./gradlew fastTest`), proven by P2 to P5. The current tree passes, proven by P1, P6 and the hooked commit.
- The ceilings trace to a recorded measurement (max + 1), not to chosen numbers.
- Test code and generated code are outside the gate, proven by P7 and the Task 1 step 6 contrast.
- The added hook cost is measured and under the 30-second stop rule.
- Authors have rule 15 for the nesting the metrics cannot see, and reviewers have its deciding test.
</success_criteria>

<output>
Create `.planning/quick/261105-pay-add-a-pmd-complexity-gate-cognitivecompl/261105-pay-SUMMARY.md` when done. Include the three ceilings with their offenders, the PMD version actually pinned, the full-analysis and cached medians, the worker peak RSS, the commit SHA, and the untracked hook log path. Note that README.md's build-gate rows (lines 97 and 237) were deliberately left unchanged.
</output>
