# Quick 261027-if9, Task 5 summary: process view claims verified or softened, numbers out, the job kind

Commit: `c381756` on main (not pushed), made through the real `.githooks/pre-commit`. Its log (`261027-if9-T5-commit.log`) ends
`exit=0`, shows the LikeC4 banner, then `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics,
4 workloads)` and the cycle line, before `Checking formatting...`. `git show --stat` lists exactly `model.c4` and `spec.c4`.
`spotlessCheck` and `fastTest` were both UP-TO-DATE (no Java changed). Testcontainers: 8 containers before and after, newest
2026-10-26, none new. Nothing else was staged.

## What changed

- `docs/architecture/model/spec.c4`: element kind `job` (notation 'Scheduled job', color secondary).
- `docs/architecture/model/model.c4`: backup is a `job`; publisher technology, description and source; consumer, topic, dlt,
  postgres and backup descriptions; three relationship labels (backup, JDBC, dlt). `views.c4` needed no change.
- No rule changed, so `drill-output.txt` is unchanged and no red/green pair applies (the plan marks T5 as not tdd).

## Per-claim verdicts (full rows with file:line in `261027-if9-T5-claims.txt`)

| Claim | Verdict | Evidence |
|---|---|---|
| api "Spring MVC, Tomcat request threads" | VERIFIED | build.gradle:173, :138, :516; no Tomcat exclusion; no virtual threads in application.properties, k8s/ or build.gradle |
| publisher technology | SOFTENED to the pool | KafkaEventPublisher.java:102-105 (listener only submits), AsyncConfig.java:19-29, prefix at :25 |
| publisher source | moved to AsyncConfig.java | same |
| "one Avro event per committed mutation" | SOFTENED, could not verify as stated | 14 publishEvent calls in board, column, task and subtask services; user mutations publish none (BoardService.java:70-73, :90-96) |
| "request thread never waits on Kafka" | VERIFIED | KafkaEventPublisher.java:29-31, :35-45 (comment; broker outage not run) |
| consumer "group activity-log" | VERIFIED | ActivityLogConsumer.java:45, :51 |
| retry count and spacing (3 labels) | REMOVED | real at KafkaConsumerConfig.java:203, :199-201 |
| "idempotent on event_id" | VERIFIED | V3__add_activity_log.sql:11; ActivityLogRecorder.java:42, :48-51 |
| "1 partition" | REMOVED | KafkaConsumerConfig.java:56 |
| "keyed by event id" | VERIFIED | KafkaEventPublisher.java:121-124 |
| JDBC label "session tables" | SOFTENED to table names | application.properties:175, :187 |
| postgres single instance, ns, prod+nonprod DBs, 16 | VERIFIED; "shared by" reworded to "holding" | postgres.yaml:43, :56; kustomization.yaml:3; 01-create-databases-and-roles.sh:59, :64 |
| backup schedule "daily 02:17" | REMOVED | backup-cronjob.yaml:29 |
| backup "as the prod DB role", PGHOST | VERIFIED | backup-cronjob.yaml:61, :63-64, :69-73 |
| api "401/403/400/409 by distinct layers" (not on the todo's list) | VERIFIED against docs/ARCHITECTURE.md, not re-derived from Java | ARCHITECTURE.md:20-24, :135-136 |

`rg -c "COULD NOT VERIFY"` over the claims file prints 1 (the "one event per mutation" row, which is false as written, so it was
softened rather than left).

## Render (looked at, downscaled to about 1610 px)

process.png shows client, REST API, kafka-publish executor, kanban.activity, activity-log consumer, the dlt topic and Postgres 16, with
no retry count, partition count or schedule in any label. production.png shows the backup in KANBAN-DATA in a distinct sky-blue
job style, labelled "PGHOST postgres.kanban-data", with no schedule. Dimension changes against T4: index 1708 -> 1672 wide, process
2466 -> 2472 wide, production 4082 -> 3930 wide, caused by shorter label text; data, development and the three signin views are
identical. See `261027-if9-T5-png.txt`.

## Deviations

- The api element's description is not on the todo's list; I checked it anyway and recorded the verdict (VERIFIED from the
  architecture doc only).
- No `cd` or other machine-rule violation this task, except that two Bash calls were refused for length and redone as script
  files.

## Not verified or not done

- postgres-backup.sh was not read; the backup description claims only the command and the role.
- The "request thread never waits on Kafka" claim rests on the code and its comments, not on a run against a broker outage.
- The `job` style colour (`secondary`) is my choice; the standard names the kind but no style.
- The development, data and three signin PNGs were not re-opened (unchanged dimensions).
- DIAGRAM_CONVENTIONS.md still describes the old state in places; Task 6 owns it. PLAN, SUMMARY and STATE were not committed.
  The untracked `.planning/quick/261024-*` files and the evidence files were left alone. Scratch and `docs/architecture/out` stay for Task 6.
