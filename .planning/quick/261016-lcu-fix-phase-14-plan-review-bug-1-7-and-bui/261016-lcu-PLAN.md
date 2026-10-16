---
phase: quick-261016-lcu
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-01-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-02-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-03-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-04-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-05-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-06-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-07-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-08-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-09-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-10-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-11-PLAN.md
  - .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-PATTERNS.md
  - .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish
  - .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml
  - .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/tests.yml
  - build.gradle
autonomous: true
requirements: [261016-lcu]

estimate:
  tokens: 130000
  raw_tokens: 130000
  tasks: 2
  confidence: low

must_haves:
  truths:
    - "BUG-3: `fish .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish` exits 0 and prints `checked 7 cleanup verifies, 0 failing case(s)`. Against the unedited plans it exited 1 with 7 failing cases (planner run, 2026-10-16). Each of the six suspension checks and the 14-10 deletion check now fails when ssh or kubectl fails."
    - "BUG-4: every scoped `./gradlew test --tests` command in the Phase 14 plans carries `-x jacocoTestCoverageVerification`, and no Gradle verify in them pipes into tail, so the executor reads Gradle's own exit status."
    - "BUG-1: 14-02 prescribes age with the plaintext dump as its input file. Its smoke harness must decrypt the uploaded object and compare the byte count with the dump size the script logged before encryption, so an empty ciphertext fails the encrypted case."
    - "BUG-2: 14-05 Task 1 counts a failed publish exactly once on both paths: a synchronous throw from the send call, and a returned future that completes exceptionally. Each path is pinned by its own test with its own failing direction: SchemaRegistryOutageE2ETest for the synchronous throw, the new KafkaPublishFailureE2ETest for the exceptional future."
    - "BUG-5: 14-06 prescribes the ActivityDeadLetterWritten expression the promtool fixture proves. It fires when a new pod's first sample is already 1 and on a 0 to 1 increase, and stays silent on flat 0 and after a single failed scrape. 14-08 requires those cases as CI unit tests, and 14-05's registration wording no longer claims registration alone covers the first dead letter."
    - "BUG-6: 14-10's substituted lag row relaxes both comparisons, so it fires on a caught-up consumer's flat zero lag. 14-06's production ActivityConsumerLagRising expression is byte-identical to before."
    - "BUG-7: 14-07 adds no ObservationPredicate. Its management-port no-span assertion is a characterisation of Boot, paired with a main-port SERVER-span positive control, and 14-05 and 14-09 no longer cite the predicate or its class."
    - "Every touched Phase 14 plan passes `frontmatter.validate --schema plan` and `verify.plan-structure`. Nothing under src/ changed, and 14-CONTEXT.md (D-01..D-15) and 14-RESEARCH.md are untouched."
    - "build.gradle's diff deletes or rewords only comment lines and the two task description strings, and `python3 scripts/verify-comments.py check` passes. Its commit ran the full pre-commit hook (no --no-verify) and is separate from Task 1's commit."
  artifacts:
    - path: ".planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish"
      provides: "BUG-3 red/green gate: extracts the seven cleanup verifies from the plans and runs each against stubbed ssh, k3s and python3"
      contains: "checked $checked cleanup verifies"
    - path: ".planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/tests.yml"
      provides: "promtool evidence for BUG-5 and BUG-6, with contrast rules that reproduce the old expressions' failures"
      contains: "stale"
    - path: ".planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-06-PLAN.md"
      provides: "the adopted ActivityDeadLetterWritten expression"
      contains: "max_over_time(kanban_activity_dlt_written_total[1h] offset 10m)"
    - path: ".planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-05-PLAN.md"
      provides: "two-path publish-failure counting with one test per path"
      contains: "KafkaPublishFailureE2ETest"
    - path: "build.gradle"
      provides: "comments trimmed to the comment rubric, no behavioural change"
  key_links:
    - from: ".planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml"
      to: ".planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-06-PLAN.md"
      via: "the ActivityDeadLetterWritten expression is the same string in both"
      pattern: "max_over_time\\(kanban_activity_dlt_written_total\\[1h\\] offset 10m\\)"
    - from: ".planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-06-PLAN.md"
      to: ".planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-08-PLAN.md"
      via: "14-08's promtool unit tests pin the new-series and stale-scrape cases of the 14-06 rule"
      pattern: "first sampled at 1"
    - from: ".planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish"
      to: "the seven cleanup verifies in 14-01, 14-03, 14-06, 14-09, 14-10 and 14-11"
      via: "rg extraction of automated lines that query netcup-prod for suspended objects or the 14-10 scratch objects"
      pattern: "suspend==true|measure-block-management-port"
---

<objective>
Fix the seven A- and B-band findings from the Phase 14 plan review (BUG-1..BUG-7) in the Phase 14 PLAN.md text. Separately, trim build.gradle's comments to the comment rubric. Nothing in Phase 14 has been executed: Task 1 edits plan text only, and Task 2 edits build.gradle comments only.

Purpose: as written, the Phase 14 plans would ship an empty encrypted backup, an alert that misses the dead letter a fresh deploy finds, cleanup gates that certify an unrecovered cluster, Java verifies that fail on green tests, a lag measurement that cannot fire, a publish-failure counter that misses the registry-outage case, and an untestable predicate. Both reviewers (Claude, Codex) reproduced most of these.

Output: eleven corrected Phase 14 plans plus 14-PATTERNS.md; two committed evidence artifacts that re-run the corrected strings offline (check-cleanup-verifies.fish, promtool-fixture/); a comment-only build.gradle change; and 261016-lcu-SUMMARY.md.
</objective>

<data_flow>
Task 1 rewrites prose and embedded shell and PromQL strings in the Phase 14 PLAN.md files, which later executors run verbatim. Two committed artifacts re-execute those strings offline: the cleanup-verify harness runs them against stubbed ssh and kubectl, and promtool runs the alert expressions against synthetic series, showing each fails and passes in the right cases. Task 2 deletes or rewords build.gradle comments only; the comment lint reads them, and the pre-commit hook builds and tests the project once, at commit time.
</data_flow>

<approaches>
Required by .claude/CLAUDE.md before approval: two alternatives and a trade-off matrix.

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A. One quick task, two ordered plan tasks, two commits (Task 1 a planning-docs commit with --no-verify; Task 2 a build.gradle commit through the full hook) | Pro: one executor holds the cross-plan context that the BUG-2, BUG-5, BUG-6 and BUG-7 rewrites share (14-05, 14-06, 14-08, 14-10 and 14-PATTERNS restate each other). The build.gradle commit stays isolated, with its own hook run. Con: Task 1 touches 14 files, above the 5-file-per-task guideline | Picked. The edits are coupled through restated claims, so splitting them invites wording drift between plans. The orchestrator fixed this two-task shape |
| B. Two parallel quick tasks in separate worktrees | Pro: wall-clock parallelism. Con: worktree base-ref trap (the harness forks from origin/HEAD); a second executor re-reads the same plans; the halves share no files, so there is little to gain | Rejected: ceremony for no measurable gain |
| C. Hand edits outside GSD | Forbidden by .claude/CLAUDE.md (GSD Workflow Enforcement) | Rejected |

Per-finding decisions (sub-matrices):

| BUG-5 expression | Measured (promtool v3.14.0, fixture in this directory, 2026-10-16) | Verdict |
|---|---|---|
| E0 `increase(kanban_activity_dlt_written_total[10m]) > 0` (14-06 as planned) | New pod first sampled at 1: no alert (reproduces Claude finding 05) | Rejected |
| E1 instant offset: series `unless` the same series `offset 10m`, or the increase (the brief's example) | Catches the new pod. But one failed scrape writes a stale marker, and 10 minutes later the offset lookup reads that as absence: a pod whose counter is already 2 pages with no new dead letter (fires at 30m in the fixture) | Rejected: a false page after any transient scrape failure |
| E2 range offset: series `unless max_over_time(series[1h] offset 10m)`, or the increase | Catches the new pod. Range selections skip stale markers, so a failed scrape is not absence. Silent on flat 0 and after the stale scrape; fires on 0 to 1 and on a same-pod restart. No new series to pin | Picked |
| E3 new-pod branch joined on process_start_time_seconds | No gap false positive. Adds a second series and a label join that PrometheusScrapeE2ETest would also have to pin | Rejected: more coupling for the same coverage |

| BUG-2 design | Pros / Cons | Verdict |
|---|---|---|
| One counting method called from the whenComplete failure branch and from a catch around the send, which rethrows | Additive. The @Async uncaught handler keeps logging the synchronous failure exactly as today, so SchemaRegistryOutageE2ETest's existing "onActivityEvent" log assertion stays valid | Picked |
| Funnel: turn the synchronous throw into a failed future handled by the single whenComplete | One code path, and the log line names the event. But it changes the log message the outage test pins and removes the async handler's log | Rejected: wider change than the finding needs |

| BUG-7 | Pros / Cons | Verdict |
|---|---|---|
| Drop the predicate; keep the management-port assertion as a characterisation of Boot, with a main-port positive control | Honest; no dead bean; the assertion still catches a Boot upgrade that starts observing management requests | Picked |
| Re-scope the predicate to main-port /api/actuator paths | Those 401s are the scanner signal 14-04 counts as auth failures; hiding them loses data | Rejected |
| Keep the predicate as defence in depth | Untestable dead code; its failing-direction evidence cannot be produced | Rejected |

| BUG-6 | Pros / Cons | Verdict |
|---|---|---|
| Relax both comparisons (lag >= 0 and deriv >= 0) | Stays inside the "only the threshold changed" substitution rule and inside Task 1's authorised injection list; promtool shows it fires on flat zero | Picked |
| A real backlog injection (pause the nonprod listener, produce records) | A genuine rising lag, but it is a new injection that 14-10 Task 1's authorisation list does not cover | Rejected |

| BUG-3 | Pros / Cons | Verdict |
|---|---|---|
| Checked assignment, then test; the deletion check uses three ssh calls, each with `--ignore-not-found` | Each query's status is checked; flat quoting inside an XML-embedded command | Picked |
| One ssh running a remote set -e script | One round trip, but nested escaping inside XML is error-prone | Rejected |

Non-obvious trade-offs:
- Memory: the only Gradle, JVM or Testcontainers run is the pre-commit hook on Task 2's commit. Its fastTest adds about 2 GB (261016-ekm measurement), so the commit is gated on at least 3000 MB available. Promtool runs one container at a time, with --memory 256m and --network none.
- Security: Task 1's commit skips gitleaks, which .claude/CLAUDE.md allows for planning docs. Its content is prose, shell strings and synthetic YAML series with no credential-shaped value. Task 2's commit runs gitleaks.
- Plan-text state drift: a rewritten claim left stale in a sibling plan would mislead its executor. Step 1h's rg cross-check covers every changed identifier, and the GSD validators run on every touched plan.
- Alert cost: E2 adds one 1-hour range read over a handful of series per evaluation, which is negligible. Its known hole is a scrape gap longer than an hour on a pod whose counter is above 0: that pages once when scraping resumes, after AppTargetDown has already paged.
- 14-05's new async-path test pauses that Gradle fork's Redpanda container for a few seconds. It must unpause in @AfterEach, and this host has no Ryuk reaper (project memory), so the 14-05 executor checks `docker ps -a` afterwards. The mechanism is a plan prescription the planner could not run (no JVM allowed); the rewritten task carries an explicit stop-and-report path.
- Two adjacent extensions, each the same defect class as the named finding, are flagged in the SUMMARY:
  - BUG-4: the three full-suite `./gradlew test` verifies lose their tail pipe too, since it masks their exit status the same way.
  - build.gradle: the file has 8 bare URL comment lines; the brief listed 6, and the other two (datafactory, archunit) go too.
</approaches>

<execution_context>
@$HOME/.claude/gsd-core/workflows/execute-plan.md
@$HOME/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@/home/andre/dev/kanban-board-backend/.worktrees/review-claude-527919/.review-claude.md
@/home/andre/dev/kanban-board-backend/.worktrees/review-codex-527919/.review-codex.md
@/home/andre/.claude/skills/review-comments/references/rubric.md
@docs/CODE_STYLE.md

PHASE_DIR below means .planning/phases/14-production-ownership-backend-observability-alerting-and-reco and QUICK_DIR means .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui. Line numbers are those of the unedited files on 2026-10-16. Edit by text anchor with the Edit tool, never by script, so a shifted line does not matter.

Already in QUICK_DIR, written and run by the planner:
- check-cleanup-verifies.fish: against the unedited plans it printed 7 FAIL lines and exited 1. Against a scratch copy carrying the Step 1c rewrite it printed `checked 7 cleanup verifies, 0 failing case(s)` and exited 0.
- promtool-fixture/rules.yml and tests.yml: `promtool check rules` SUCCESS (5 rules) and `promtool test rules` SUCCESS. A flipped copy failed exactly the two key expectations (exit 1), so the assertions bite.

Hard limits for this run:
- Do not run ./gradlew, bootRun, java, Testcontainers or docker compose, except through the pre-commit hook on Task 2's commit; the VM has about 8 GB and froze earlier.
- Do not delete with rm; use trash-put.
- System python3 has no PyYAML; nothing here needs it.
</context>

<!-- planner-discipline-allow: ObservabilityConfig -->

<tasks>

<task type="auto">
  <name>Task 1: Fix BUG-1..BUG-7 in the Phase 14 plan text, cross-check restated claims, validate, commit</name>
  <files>.planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-01-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-02-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-03-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-04-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-05-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-06-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-07-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-08-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-09-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-10-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-11-PLAN.md, .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-PATTERNS.md, .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish, .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml, .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/tests.yml</files>
  <read_first>
    - /home/andre/dev/kanban-board-backend/.worktrees/review-claude-527919/.review-claude.md, plus findings/finding-01.md, finding-04.md and finding-05.md in that worktree
    - /home/andre/dev/kanban-board-backend/.worktrees/review-codex-527919/.review-codex.md (findings 01-04, with reproductions)
    - .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture/rules.yml and tests.yml
    - .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish
    - src/main/java/com/vrudenko/kanban_board/config/KafkaEventPublisher.java (whole file, 68 lines; its Javadoc's Decisions paragraph is wrong about registry failures)
    - src/test/java/com/vrudenko/kanban_board/activitylog/SchemaRegistryOutageE2ETest.java lines 35-80 and 127-131 (the synchronous-throw account and the root-logger appender)
    - src/test/java/com/vrudenko/kanban_board/support/containers/AbstractKafkaContainerTest.java lines 21-120 (shared per-fork Redpanda container, 30-second producer bounds, TestPropertySource precedence)
    - only the line ranges named below in each Phase 14 plan; do not read whole plans
  </read_first>
  <action>
Edit Phase 14 plan TEXT only, one finding at a time, with the Edit tool. Nothing under src/ changes. Do not touch 14-CONTEXT.md or 14-RESEARCH.md: the research record keeps its superseded candidates, and the plans now state what to use. If any fix would contradict a D-01..D-15 decision, stop and report it; the planner found no such conflict. Phase-plan text may cite review findings and this quick id; the "no decision ids in code comments" rule binds only the code those plans will later produce.

1a. BUG-1, the age step has no input file (PHASE_DIR/14-02-PLAN.md, Task 2).
- Line 203: make the encryption step `age -r "$AGE_RECIPIENT" -o "$WORK_DIR/db.dump.age" "$WORK_DIR/db.dump"`, adding the input file argument as 14-RESEARCH.md:401 already does. In the same step, after the `pg_restore -l` check, the script logs the dump's size in bytes (a byte count, no content). Leave `set -o pipefail` and the shellcheck verify as they are: Claude finding 3 is out of scope here.
- Line 194, the behaviour bullet: the uploaded body decrypts with the throwaway identity to exactly the logged dump size, and more than 0 bytes. pg_restore -l on the result lists smoke_backup, so an empty ciphertext fails this case.
- Line 43, the must_haves truth: append the same byte-count equality.
- Acceptance criteria: add two items.
  - `grep -nF '"$WORK_DIR/db.dump.age" "$WORK_DIR/db.dump"' k8s/data/postgres/backup/postgres-backup.sh` prints one line.
  - SUMMARY.md records the failing direction: with the input argument removed, the harness's encrypted case prints FAIL (0 decrypted bytes against the logged dump size); restored, it prints PASS.

1b. BUG-2, synchronous send failures are not counted (PHASE_DIR/14-05-PLAN.md).

Rewrite Task 1 step 3 so the publish-failure counter increments exactly once per failed publish on both paths:
- KafkaEventPublisher keeps the field-injected MeterRegistry and the @PostConstruct pre-registration per event class name.
- One private method, countPublishFailure(ActivityEvent), increments kanban.kafka.publish.failures tagged event=<simple class name>. It is called from exactly two places:
  - (i) the whenComplete `ex != null` branch, beside the existing log.error. This is a failure after the record reached the producer, such as a delivery timeout.
  - (ii) a catch of RuntimeException around the toAvro call and the kafkaTemplate.send call, which calls countPublishFailure and then rethrows. This is a synchronous failure: Avro serialization or a registry lookup, or an immediate producer failure that KafkaTemplate.doSend rethrows before returning a future (SchemaRegistryOutageE2ETest Javadoc lines 65-78; Spring Kafka 3.3.16 KafkaTemplate source, as cited in Codex finding 01).
- Rethrow so the @Async uncaught-exception handler keeps logging the failure as today, and SchemaRegistryOutageE2ETest's existing "onActivityEvent" log assertion stays valid.
- The two paths are exclusive per publish (a synchronous throw returns no future, so whenComplete is never attached), which is what makes the count exactly once.
- The step also tells the 14-05 executor to rewrite KafkaEventPublisher's Javadoc Decisions paragraph. It says no try/catch wraps the send and that a registry failure becomes a failed future, and the outage test's Javadoc documents why that is wrong. Replace it with the two-path account and why the catch rethrows. Also update SchemaRegistryOutageE2ETest's Javadoc bullet that quotes the old claim, so it describes the new state.

Behaviour list (lines 124-128): replace the registry bullet with two bullets.
- Synchronous path: a mutation made while the producer's schema registry is unreachable raises kanban.kafka.publish.failures{event=<the mutation's event class>} by exactly 1. The test reads the baseline after the arrange step's own publishes settle and holds for a few seconds with Awaitility during(...) to show no second increment. The ERROR naming onActivityEvent is still logged.
- Asynchronous path: a publish whose send returned a future that later completes exceptionally raises the counter by exactly 1 (held the same way), and the publisher's "Failed to publish" ERROR line is logged.

Step 1 (RED): add a new class, KafkaPublishFailureE2ETest, in package activitylog.
- Shape: @Tag("kafka"), extends AbstractKafkaContainerTest. A class-level @TestPropertySource lowers spring.kafka.producer.properties.request.timeout.ms and delivery.timeout.ms (for example 1000 and 2000; delivery.timeout.ms must be at least request.timeout.ms plus linger.ms or the producer refuses to start). Subclass TestPropertySource values override the superclass's, and the class gets its own context.
- Test: first do one successful publish of the same event type, so producer metadata and the serializer's schema id are cached. Then pause the Redpanda container through Testcontainers' Docker client (pauseContainerCmd on the shared container's id), make the mutation, wait for the failure, and assert the delta. Unpause in @AfterEach, always. Afterwards, check `docker ps -a` (this host has no Ryuk reaper).
- Why a new class: neither existing class reaches the asynchronous path. SchemaRegistryOutageE2ETest's context fails every send synchronously at serialization, and ActivityLogDeadLetterE2ETest shares the cached context, with 30-second producer bounds, with every other Kafka class.
- No mock and no hand-completed future (CODE_STYLE rule 4). If no real mechanism yields an exceptional completion reliably, stop and report the asynchronous path as not verified rather than substituting one.

Step 5 (GREEN), failing directions:
- catch's increment removed: SchemaRegistryOutageE2ETest red, KafkaPublishFailureE2ETest green;
- whenComplete increment removed: KafkaPublishFailureE2ETest red, SchemaRegistryOutageE2ETest green;
- plus the existing startup-registration direction.

Propagate:
- add the new test path to frontmatter files_modified, Task 1 `<files>`, `<read_first>` (with AbstractKafkaContainerTest) and the artifacts table row "Tests" (line 91);
- truth line 27: both paths, each proven by its own test;
- data_flow line 69: both hooks;
- acceptance: KafkaPublishFailureE2ETest passes, still tagged kafka; `grep -n 'countPublishFailure' src/main/java/com/vrudenko/kanban_board/config/KafkaEventPublisher.java` shows one definition and two call sites; SUMMARY.md records both path-specific failing directions;
- PHASE_DIR/14-PATTERNS.md line 20 (hook cell) and line 97 (increment site) describe both call sites and the rethrowing catch.

1c. BUG-3, cleanup verifies pass when ssh or kubectl fails. Seven `<automated>` lines:
- the six suspension checks: 14-01:289, 14-03:237, 14-06:249, 14-09:216, 14-10:173, 14-11:120;
- the deletion check: 14-10:169.

Rewrite each suspension check as a checked assignment followed by the emptiness test: `suspended=$(ssh netcup-prod "<the same remote kubectl command, unchanged>") && test -z "$suspended"`. In 14-06 and 14-09 keep the gates that precede it, joined with `&&`. Keep each remote jsonpath byte-identical.

Rewrite the 14-10:169 deletion check as three checked assignments, then the assertions:
`rule=$(ssh netcup-prod "k3s kubectl -n monitoring get prometheusrule kanban-measure-scratch --ignore-not-found -o name") && pol=$(ssh netcup-prod "k3s kubectl -n kanban-nonprod get networkpolicy measure-block-management-port --ignore-not-found -o name") && replicas=$(ssh netcup-prod "k3s kubectl -n kanban-nonprod get deploy app -o jsonpath='{.spec.replicas}'") && test -z "$rule" && test -z "$pol" && test "$replicas" = 1`
NotFound then reads as empty output with exit 0, while Forbidden or a transport error exits non-zero.

Update each adjacent `<fails_when>` to say: non-zero exit means something is still suspended (or present, or replicas are not 1), or that ssh or kubectl failed so the state could not be read.

1d. BUG-4, scoped Gradle verifies exit 1 on green tests, and the tail pipe masks it.
- Every scoped command gets `-x jacocoTestCoverageVerification` appended after its last `--tests` argument, and every Gradle `2>&1 | tail -N` suffix in an `<automated>` line is dropped (build.gradle has no testLogging override, so the output stays short).
- Scoped sites: 14-01:236 and the 14-01:327 verification bullet; the 14-04:163 action text (`--tests '*ArchTest'`), 14-04:168 and 14-04:213; 14-05:145 (with the new class from 1b) and 14-05:181; 14-07:146 and 14-07:188.
- Full-suite sites (14-04:215, 14-05:183, 14-07:190): drop only the tail suffix and keep the JaCoCo ratchet, which is the point of a full run. This extends the finding to the same exit-masking mechanism; say so in SUMMARY.md.
- Find every site with `rg -n 'gradlew test' PHASE_DIR/*-PLAN.md`; do not trust this list alone. 14-RESEARCH.md:624 and :626 stay unchanged: research record, not a verify.

1e. BUG-5, ActivityDeadLetterWritten misses a dead letter written before a new pod's first scrape.
- PHASE_DIR/14-06-PLAN.md line 197: replace the ActivityDeadLetterWritten expression with the fixture's adopted one, byte-identical: `(kanban_activity_dlt_written_total unless max_over_time(kanban_activity_dlt_written_total[1h] offset 10m)) > 0 or increase(kanban_activity_dlt_written_total[10m]) > 0`. It stays warning, with no `for`.
- In the same bullet, say it supersedes RESEARCH's candidate, and specify the rule's plain-text YAML comment:
  - the first branch catches a pod whose first scraped sample is already above 0 (a poison record dead-lettered before the first scrape after a Recreate rollout or restart), which increase() cannot see;
  - it looks back an hour, so one failed scrape's stale marker does not make an old series look new;
  - the second branch catches increments on a series already seen;
  - known hole: a scrape gap longer than an hour, on a pod whose counter is above 0, pages once when scraping resumes.
- Add QUICK_DIR/promtool-fixture/tests.yml to 14-06 Task 2's `<read_first>`.
- Add acceptance: `grep -nF 'max_over_time(kanban_activity_dlt_written_total[1h] offset 10m)' k8s/monitoring/configs/prometheusrules.yaml` prints one line.
- PHASE_DIR/14-08-PLAN.md line 144: the ActivityDeadLetterWritten unit-test cases become:
  - fires after a 0 to 1 increase;
  - does not fire while flat at 0, including a new series that starts at 0;
  - fires when a new pod's series is first sampled at 1 after the old pod's series ends;
  - does not fire 10 minutes after a single failed scrape (stale marker) on a pod whose counter is already 2.
  Point at the fixture's series notation, and add the fixture to 14-08 Task 1's `<read_first>`.
- PHASE_DIR/14-05-PLAN.md truth line 26, trade-off line 80 and threat row T-14-47 (line 210): eager registration lets increase() see every failure after a pod's first scrape. A dead letter written before that scrape is caught by the 14-06 rule's new-series branch, not by registration.
- PHASE_DIR/14-10-PLAN.md row 3 (line 161): note that the live injection exercises the increase branch on a running pod, and the new-series branch is covered by 14-08's promtool unit test.

1f. BUG-6, the substituted lag row cannot fire on a caught-up consumer (PHASE_DIR/14-10-PLAN.md).
- Line 115, the Task 1 context list: the ActivityConsumerLagRising substitution relaxes both comparisons, lag and its 10-minute deriv, each to >= 0. Reason: a caught-up consumer shows flat zero lag, so lowering only the lag threshold never fires.
- Line 162, step 4: the scratch expression becomes `kafka_consumer_fetch_manager_records_lag_max{namespace="kanban-nonprod"} >= 0 and deriv(kafka_consumer_fetch_manager_records_lag_max{namespace="kanban-nonprod"}[10m]) >= 0`, citing the promtool fixture. Keep the SUBSTITUTED label and the "only the threshold changed" framing; both changes are thresholds.
- Confirm 14-06's original expression is untouched: `rg -nF 'deriv(kafka_consumer_fetch_manager_records_lag_max{namespace=~"kanban-(prod|nonprod)"}[10m]) > 0' PHASE_DIR/14-06-PLAN.md` still prints the step 5 line.

1g. BUG-7, the ObservationPredicate filters nothing. Claude finding 04: on a live bootRun, more than 15 management-port requests produced zero http_server_requests observations, so a predicate cannot change span output.

In PHASE_DIR/14-07-PLAN.md:
- frontmatter: remove the config-class line from files_modified (line 11). Truth line 32 becomes: management-port requests produce no spans because Boot does not observe the management child context (pinned by TraceLogCorrelationE2ETest against a main-port positive control), and Spring Security observations are off by property.
- Artifacts (lines 39-41): replace the predicate entry with one for TraceLogCorrelationE2ETest (contains "LocalManagementPort").
- Objective Output (line 66): drop the predicate. Trade-off line 82: rewrite to the observed fact and why no predicate or metric filter is needed. Artifacts-table line 93: delete the Spring bean row.
- Task 1: drop the config class from `<name>` and `<files>` (lines 117-118). Behaviour line 128 becomes: GET /actuator/health on the management port exports no span, while GET /api/boards in the same test exports a SERVER span (positive control). This characterises Boot rather than app code: it passes at RED with no app change, and fails if a Boot upgrade starts observing management requests.
- Action step 4: replace it with an instruction NOT to add an ObservationPredicate or the config class, citing the finding. If the management case shows a span at RED, Boot does observe it in this setup: stop and report, because that contradicts the review and reopens the question.
- Step 5's failing-direction list: security property removed (security spans appear); tracing bridge absent (no traceId); management case recorded as passing at RED, beside the positive control.
- Acceptance: replace line 155's predicate grep and line 157's "three failing-direction checks" to match. Update done (line 159) and threat row T-14-28 (line 217): management-port requests are unobserved by Boot, pinned by the test.
- Elsewhere: PHASE_DIR/14-05-PLAN.md trade-off line 81 says management-port requests are unobserved and the p95 rule's /actuator matcher is a guard against a Boot change. PHASE_DIR/14-09-PLAN.md precondition line 198 checks that the deployed tag's commit contains ContextPropagatingTaskDecorator in src/main/java/com/vrudenko/kanban_board/config/AsyncConfig.java, instead of naming the dropped class.

1h. Cross-check pass. Run rg over PHASE_DIR for each changed identifier and claim, and fix every stale restatement in a plan, 14-PATTERNS.md or COVERAGE.md. COVERAGE.md and 14-13-PLAN.md held none in the planner's scan; confirm and say so in SUMMARY.md. Patterns:
- `ObservationPredicate|ObservabilityConfig|predicate`
- `KafkaPublishFailureE2ETest|countPublishFailure|whenComplete`
- `ActivityDeadLetterWritten|dlt_written|increase\(`
- `ActivityConsumerLagRising|deriv\(`
- `suspend==true|measure-block-management-port`
- `gradlew test`
- `age -r|db\.dump`

1i. Validate every touched plan (14-01 to 14-11) with `. ./.dev/gsd-run.sh && gsd_run query frontmatter.validate <plan> --schema plan` and `gsd_run query verify.plan-structure <plan>`. Both must report valid with no errors. If verify.plan-structure flags comment-text discipline on a rewritten action, rephrase by concept rather than adding an allowlist marker to the phase plan.

1j. Commit:
- Check `git diff --cached --name-only` is empty.
- `git add` the three untracked QUICK_DIR evidence files (check-cleanup-verifies.fish, promtool-fixture/rules.yml, promtool-fixture/tests.yml).
- Run `git commit --no-verify -m "docs(14): fix plan review findings BUG-1..BUG-7" -- <the touched PHASE_DIR files> <the three QUICK_DIR files>`. A planning-docs commit; .claude/CLAUDE.md allows --no-verify for it.
- Leave 261016-lcu-PLAN.md and the SUMMARY for the quick workflow's own docs commit. Do not sweep in .planning/config.json, .serena/, docs/learning/ or PHASE_DIR/.gitkeep.
  </action>
  <verify>
    <automated>fish .planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/check-cleanup-verifies.fish</automated>
    <fails_when>non-zero exit, a FAIL line, or a count other than "checked 7 cleanup verifies, 0 failing case(s)"</fails_when>
    <automated>P=.planning/phases/14-production-ownership-backend-observability-alerting-and-reco; { rg -q 'test -z.*ssh' $P/*-PLAN.md; test $? -eq 1; } && { rg -q 'gradlew.*\| *tail' $P/*-PLAN.md; test $? -eq 1; } && { rg -n -- '--tests' $P/*-PLAN.md | rg -q -v -- '-x jacocoTestCoverageVerification'; test $? -eq 1; } && test "$(rg -c -- '-x jacocoTestCoverageVerification' $P/*-PLAN.md | awk -F: '{s+=$NF} END {print s+0}')" -ge 9</automated>
    <fails_when>non-zero exit: an old suspension check remains, a Gradle verify still pipes into tail, a scoped --tests line lacks the -x flag, or fewer than 9 -x sites exist</fails_when>
    <automated>P=.planning/phases/14-production-ownership-backend-observability-alerting-and-reco; test "$(grep -cF 'max_over_time(kanban_activity_dlt_written_total[1h] offset 10m)' $P/14-06-PLAN.md)" -ge 1 && test "$(grep -cF 'deriv(kafka_consumer_fetch_manager_records_lag_max{namespace=~"kanban-(prod|nonprod)"}[10m]) > 0' $P/14-06-PLAN.md)" -ge 1 && test "$(grep -cF 'deriv(kafka_consumer_fetch_manager_records_lag_max{namespace="kanban-nonprod"}[10m]) >= 0' $P/14-10-PLAN.md)" -ge 1 && test "$(grep -cF -e '"$WORK_DIR/db.dump.age" "$WORK_DIR/db.dump"' $P/14-02-PLAN.md)" -ge 1 && test "$(grep -c 'KafkaPublishFailureE2ETest' $P/14-05-PLAN.md)" -ge 4 && test "$(grep -c 'first sampled at 1' $P/14-08-PLAN.md)" -ge 1 && { rg -q 'ObservabilityConfig' $P/*-PLAN.md; test $? -eq 1; }</automated>
    <fails_when>non-zero exit: a fix is missing from its plan, 14-06's production lag expression changed, or the dropped config class is still named in a plan</fails_when>
    <automated>docker run --rm --network none --memory 256m -v "$PWD/.planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/promtool-fixture":/c:ro -w /c --entrypoint promtool prom/prometheus:v3.14.0 test rules /c/tests.yml</automated>
    <fails_when>non-zero exit or output without SUCCESS</fails_when>
    <automated>. ./.dev/gsd-run.sh && P=.planning/phases/14-production-ownership-backend-observability-alerting-and-reco && for n in 01 02 03 04 05 06 07 08 09 10 11; do gsd_run query frontmatter.validate $P/14-$n-PLAN.md --schema plan | jq -e '.valid == true' >/dev/null && gsd_run query verify.plan-structure $P/14-$n-PLAN.md | jq -e '.valid == true and (.errors | length == 0)' >/dev/null || { echo "FAIL 14-$n"; exit 1; }; done</automated>
    <fails_when>non-zero exit or a "FAIL 14-NN" line</fails_when>
    <automated>git diff --quiet HEAD -- src/ .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-CONTEXT.md .planning/phases/14-production-ownership-backend-observability-alerting-and-reco/14-RESEARCH.md && test -z "$(git status --porcelain -- src/)"</automated>
    <fails_when>non-zero exit: source, CONTEXT or RESEARCH changed</fails_when>
  </verify>
  <acceptance_criteria>
    - The harness reports 7 checked and 0 failing; SUMMARY.md quotes the planner's red run (7 FAIL lines, exit 1) and the executor's green run.
    - Every rg gate in the verify block passes, and promtool reports SUCCESS on the fixture.
    - All eleven touched plans validate (frontmatter and structure).
    - SUMMARY.md lists, per BUG, the plan lines changed and the evidence. It also records the two adjacent extensions, why the brief's instant-offset DLT expression was not adopted (the fixture's stale-scrape case), and what was not verifiable here: the 14-05 asynchronous-path mechanism and the 14-07 characterisation need a JVM, and the live ssh commands were exercised only against stubs.
    - The commit contains only the touched PHASE_DIR files and the three QUICK_DIR evidence files.
  </acceptance_criteria>
  <done>BUG-1..BUG-7 are fixed in the plan text, every restatement agrees, the offline evidence passes, and one planning-docs commit holds it.</done>
</task>

<task type="auto">
  <name>Task 2: Trim build.gradle comments to the comment rubric, then commit through the full pre-commit hook</name>
  <files>build.gradle</files>
  <read_first>
    - build.gradle lines 1-50, 140-260, 265-345, 346-465 and 575-615 (each range once)
    - /home/andre/.claude/skills/review-comments/references/rubric.md (rule 0 on planning tokens, rule 7 on sibling names)
    - docs/CODE_STYLE.md rule 14 (plain text, blank comment line between paragraphs, `- ` list items, summary line on blocks of 4 or more lines)
  </read_first>
  <action>
Change comments and the two task description strings only; no task, dependency, version or setting changes. Line numbers are pre-edit; anchor every Edit on text. Keep paragraph breaks and summary lines valid under scripts/verify-comments.py. Steps:

a. Task description strings (lines 584-585 and 608-609; not comments, but the same rule-0 fix):
- registerSchemas: drop the hard-coded schema count and the parenthesised planning-token list. Result: `'Registers the Avro schemas against the Schema Registry with BACKWARD ' + 'compatibility.'`
- rehearseHistoricalSchemas: drop the parenthesised planning token that opens line 609, and move the sentence's full stop onto line 608, so the description reads "... historical activity_log rows. Requires DB_HOST/...".

b. Lines 247-251: delete the orphan Avro logical-types comment block, which sits above no dependency, together with its trailing blank line 252, leaving a single blank line before the archunit dependency.

c. Sibling-similarity names (rubric rule 7): cut the name, keep the reason.
- Lines 11-12: replace the pointer to the report task with the reason alone (generated code is excluded from the coverage denominator so the numbers stay honest).
- Lines 28-29: replace the parenthesised list of other pins with the policy alone (pin a reviewed, settled version rather than the newest).
- Line 373: delete the first sentence, which names two other pins, and keep the sentence about 0.8.12 being Gradle's bundled default.
- Line 434: keep only the reason, that the ratchet and the coverage report must share one denominator.
- Line 600: delete the clause naming fastTest as precedent, and keep "Filtered by tag, not class name:" plus the Gradle reason.

d. Lines 315-328, the duplicate fork-count decision in fastTest: the full record stays in the `test` block (lines 269-279). Replace these 14 lines with a comment of at most three lines carrying only fastTest's own facts, and no pointer to the test block:
- `./gradlew fastTest --rerun-tasks` averaged 285.0s at 1 fork and 242.5s (233s, 252s) at 2;
- a 2-fork run holds 2 PostgreSQL containers and at most 1 Redpanda container (one Kafka-container test class carries no kafka tag), inside this machine's 7.728 GiB Docker budget.
The `maxParallelForks = 2` line and the forkEvery comment below it stay.

e. Bare URL comment lines: delete each comment line whose whole text is a Maven repository URL. There are 8, at 145, 161, 167, 188, 195, 200, 203 and 253 (the brief listed six; 188 and 253 are the same case, so record that in SUMMARY.md).
- Line 167 heads a larger block. Delete only that line, so line 168 becomes the block's summary line.
- Line 457: delete the first sentence (the one about the map holding one entry today) and keep the next sentence, about a module absent from runtimeClasspath failing loudly.
- Lines 357-359: delete the blank comment line and the two-line sentence about every Mapper interface generating an implementation class. Keep line 356's first sentence.

f. Lines 25-27: delete the relative-time phrase that dates 13.0.0 against when the line was added, and keep both absolute publication dates. Lines 42-46: delete the measured-figures sentence (the finding counts by severity, before and after the bump), and keep a short sentence pointing at the already-cited .planning/quick/260813-q1i-add-a-dependency-vulnerability-scan-owas/260813-q1i-MEASUREMENTS.md.

g. Do NOT touch lines 17-18, the claim that the avro plugin's project was archived on a date that lies after today (2026-10-16). It is an open question for the user; record it in SUMMARY.md. Also record, without changing them, two adjacent relative-time phrases left in place: "has three months of field exposure" (line 26) and "report-only today (see the dependencyCheck block below)" (line 42).

h. Before committing, run the first and second verify commands (comment lint, phrase gates) and read `git diff -U0 -- build.gradle` by eye. The diff-shape and commit-shape gates (the other two) read the commit, so run them after it. Then commit:
- Run `free -m`. If the available column is below 3000 MB, stop and report, without committing and without --no-verify.
- Check that Docker is up (`docker info` exits 0).
- Check `git diff --cached --name-only` is empty.
- Run `git commit -m "docs(build): trim build.gradle comments to the comment rubric" -- build.gradle` in the foreground with a 600000 ms timeout. The full hook runs gitleaks, the comment lint, the instruction budget, spotlessCheck and fastTest, about 4 minutes. Do not pass --no-verify: the orchestrator requires the hook for this file.
- If the hook fails, read its output. Fix a comment-lint or Spotless failure. If a Gradle JVM is killed or memory runs out, stop and report, with `free -m` and `cat /proc/pressure/memory` readings.
- Afterwards, check `docker ps -a` for containers the hook left behind (no Ryuk on this host).
  </action>
  <verify>
    <automated>python3 scripts/verify-comments.py check</automated>
    <fails_when>non-zero exit or any violation line</fails_when>
    <automated>sha=$(git log -1 --format=%H -F --grep='docs(build): trim build.gradle comments to the comment rubric' -- build.gradle) && test -n "$sha" && gradle_diff=$(git diff -U0 "$sha^" "$sha" -- build.gradle) && test -n "$gradle_diff" && test "$(printf '%s\n' "$gradle_diff" | grep -E '^[-+]' | grep -Ev '^(\+\+\+|---)' | grep -Ev '^[-+][[:space:]]*(//.*)?$' | grep -Evc "^[-+][[:space:]]*(description = )?'")" = 0</automated>
    <fails_when>non-zero exit: the commit's build.gradle diff is empty, or a changed line is neither a comment, a blank line nor a description string line</fails_when>
    <automated>for phrase in mvnrepository SCHEMA-0 'ten days before' 'all 14' 'findings across' 'One entry today' 'generates a sibling' 'Mirrors the pinning' "fastTest's precedent" 'See jacocoTestReport' "This file's precedent" 'Same generated-code exclusions'; do test "$(grep -cF "$phrase" build.gradle)" = 0 || { echo "FAIL still present: $phrase"; exit 1; }; done; test "$(grep -cF 'archived by its owner on 2026-12-28' build.gradle)" -ge 1 && test "$(grep -cE 'maxParallelForks[[:space:]]=[[:space:]]2' build.gradle)" -ge 2 && test "$(grep -cF '242.5s' build.gradle)" -ge 1 && test "$(grep -cF '7.728 GiB' build.gradle)" -ge 1</automated>
    <fails_when>non-zero exit or a "FAIL still present" line: a targeted comment survived, the line-17 claim was touched, or a fork setting or fastTest fact was lost</fails_when>
    <automated>sha=$(git log -1 --format=%H -F --grep='docs(build): trim build.gradle comments to the comment rubric' -- build.gradle) && test -n "$sha" && changed=$(git diff-tree --no-commit-id --name-only -r "$sha") && test "$changed" = "build.gradle"</automated>
    <fails_when>non-zero exit: no build.gradle commit with that subject exists, or it carries other files</fails_when>
  </verify>
  <acceptance_criteria>
    - The comment lint passes, and the diff-shape gate shows only comment, blank and description-string lines changed.
    - None of the twelve targeted phrases remain; the line-17 claim, both maxParallelForks settings and the fastTest facts remain.
    - The commit holds build.gradle alone, and the pre-commit hook ran to completion without --no-verify. SUMMARY.md records the hook's wall time and `free -m` before it.
    - SUMMARY.md lists the open question (the line-17 archive date lies after 2026-10-16), the two relative-time phrases left in place, and the two URL lines deleted beyond the brief's six.
  </acceptance_criteria>
  <done>build.gradle carries only rubric-clean comments, behaviour is unchanged, and the change is committed through the full hook in its own commit.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Phase 14 plan text -> future executors -> production cluster | Shell and PromQL strings in the plans are executed verbatim against netcup-prod and Prometheus later |
| build.gradle -> pre-commit hook and CI | A non-comment change would alter the build; the hook builds and tests at commit time |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261016lcu-01 | Denial of service | Cleanup verifies certifying an unrecovered cluster (Flux left suspended) | high | mitigate | Checked-assignment rewrite; check-cleanup-verifies.fish proven red on the old text (7 failures) and green on the rewrite, re-run as Task 1's first verify |
| T-261016lcu-02 | Repudiation | Dead letters going unpaged, or false pages eroding trust in paging | medium | mitigate | Range-offset expression adopted on promtool evidence that includes the stale-scrape no-fire case; 14-08 turns the cases into CI unit tests |
| T-261016lcu-03 | Information disclosure | Backup upload of an empty or unverified ciphertext | high | mitigate | age input argument added; 14-02's harness must compare decrypted bytes with the logged dump size |
| T-261016lcu-04 | Tampering | build.gradle behaviour changed under a "comments only" label | medium | mitigate | Diff-shape gate allows only comment, blank and description-string lines; full hook (spotlessCheck, fastTest) on the commit |
| T-261016lcu-05 | Denial of service | Host memory exhaustion during the hook (8 GB WSL VM froze earlier) | medium | mitigate | free -m gate at 3000 MB available before committing; promtool one container at a time with --memory 256m; no other Gradle or JVM run |
| T-261016lcu-06 | Information disclosure | Task 1 commit skips gitleaks (--no-verify) | low | accept | Content is prose, shell strings and synthetic series with no credential-shaped value; .claude/CLAUDE.md permits --no-verify for planning docs |
| T-261016lcu-SC | Tampering | npm/pip/cargo installs | low | accept | No package install in this plan; the only image used, prom/prometheus:v3.14.0, is the one Phase 14 already pins and was already present locally |
</threat_model>

<verification>
- Task 1: the harness is green (7 checked, 0 failing), every rg gate passes, the promtool fixture reports SUCCESS, all eleven touched plans validate, and src/, 14-CONTEXT.md and 14-RESEARCH.md are unchanged.
- Task 2: the comment lint is green, the diff is comment-only apart from the two description strings, the targeted phrases are gone, and the commit went through the full hook alone.
- Two commits, in order: the planning-docs commit (Task 1), then the build.gradle commit (Task 2). The unrelated pending changes are untouched.
</verification>

<success_criteria>
The Phase 14 plans can be executed as written without the seven defects the reviewers reproduced, every restated claim agrees across plans, the offline evidence for the rewritten verifies and alert expressions is committed beside this plan, and build.gradle's comments pass the rubric with no behavioural change.
</success_criteria>

<output>
Create `.planning/quick/261016-lcu-fix-phase-14-plan-review-bug-1-7-and-bui/261016-lcu-SUMMARY.md` when done. Include:
- per-BUG changes and evidence, the two adjacent extensions, and the DLT-expression deviation from the brief with its promtool reason;
- what could not be verified;
- reviewer findings deliberately left out of scope: Claude 3 (pipefail against `shellcheck -s sh` in 14-02), Claude 4 (14-03 depends_on 14-01), Claude 6 (14-01's verification-metadata criterion against the OTel BOM poms), Claude 7 (bare python3 without PyYAML), and the 9081 /healthz 500 and empty-database-backup notes;
- the build.gradle open question and the two untouched relative-time phrases.

If a distillable pattern surfaced, record it with `node ~/.claude/hooks/record-gotcha.js`. Candidates: a command substitution inside an emptiness test discards the command's exit status; an instant `offset` lookup in a "new series" check is defeated by stale markers, while a range lookup is not.
</output>
