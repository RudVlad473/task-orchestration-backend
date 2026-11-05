---
created: 2026-11-01T12:30:00.000Z
title: Task delete tests read no state back and miss the rejection cases
area: tests
severity: minor
files:
  - src/test/java/com/vrudenko/kanban_board/controller/TaskControllerTest.java:87
  - src/test/java/com/vrudenko/kanban_board/security/AuthorizationGatingTest.java:461
  - src/test/java/com/vrudenko/kanban_board/security/InjectionAttemptTest.java:833
  - src/main/java/com/vrudenko/kanban_board/service/TaskService.java:322
---

## Problem

Found on 2026-11-01 by running the review-tests skill with its `--cases` input on five test files, with the
case facts read from the talos spec of this backend (`talos_check.py cases`, spec `board_tree.qnt` from the
second blind run, `delete_task`). Files read: `TaskControllerTest`, `ActivityEventPublicationTest`,
`AuthorizationGatingTest`, `InjectionAttemptTest`, `ActivityLogConsumerE2ETest`. Light depth, applied by
hand; other test files were not read and may cover a row. No mutation run, so `PARTIAL` rows are reading
judgments.

`TaskService.deleteById` has 1 of 6 required cases fully covered (updated 2026-11-05: the position-gap
fix added one covered row and removed one that no longer applies):

| Required case | Status | Where |
| --- | --- | --- |
| Success: the task is gone on read-back | PARTIAL | `TaskControllerTest.java:87-101` asserts HTTP 200 only; it would pass if `deleteById` did nothing |
| Success: its subtasks are gone (no orphans) | MISSING | no test reads subtasks after a task delete |
| Success: siblings keep their relative order and close up to contiguous positions | MISSING | none for the HTTP delete; the renumber after a middle delete is read back only through the create in `TaskServiceTest.DeleteByIdTest` |
| Rejected `ACCESS_DENIED`, state unchanged | PARTIAL | `AuthorizationGatingTest.java:461` checks 403 and the code, never reads state back |
| Rejected `ENTITY_NOT_FOUND`, state unchanged | PARTIAL | `InjectionAttemptTest.java:833-847` accepts 400 or 404 for malformed ids only; no well-formed unknown id |
| After a middle delete, a later create keeps positions unique | COVERED | `TaskServiceTest.DeleteByIdTest.shouldKeepPositionsContiguousAndUnique_whenCreatingAfterDeletingAMiddleTask` (quick 261105-oq1) |

The event side is covered: `ActivityEventPublicationTest.java:80` pins `TaskDeletedEvent` and its four ids.

## Solution

TBD. In `TaskControllerTest.DeleteById`, after the 200: read the task back and expect not found, read its
subtasks and expect none, read the column's other tasks and expect them unchanged. Add a well-formed unknown
task id (expect 404, nothing changed) and make the foreign-user case read the task back (expect it still
there). The position-gap todo was decided on 2026-11-05 (renumber after delete), so the middle-delete-then-create
row is covered and the "positions are not closed up" row no longer applies. Re-run review-tests with
`--cases` afterwards; each row should move to `COVERED`.
