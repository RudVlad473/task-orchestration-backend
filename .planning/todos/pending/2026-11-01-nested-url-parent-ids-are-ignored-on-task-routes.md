---
created: 2026-10-31T23:10:00.000Z
title: Nested URL parent ids are ignored on task routes
area: api
severity: minor
files:
  - src/main/java/com/vrudenko/kanban_board/controller/TaskController.java:24
  - src/main/java/com/vrudenko/kanban_board/controller/TaskController.java:43
---

## Problem

Found by the second talos blind run (2026-10-31) and reproduced by all three review lanes:
`DELETE /boards/zz/columns/yy/tasks/<id>` returns 200 with made-up board and column ids, because the parent
ids in the URL are not checked. `TaskController` is mapped under
`/boards/{boardId}/columns/{columnId}/tasks` (lines 24 to 29), but `deleteById` (line 43) reads only
`taskId` and calls `taskService.deleteById(userId, taskId)`; ownership is checked through the user id and
the task id alone. I re-read the controller on 2026-10-31 and did not re-run the request today. I did not
check the other task and subtask routes. It is a contract gap, not an access leak, as far as I could see:
the task is still looked up by the current user.

## Solution

TBD. Either verify that the task belongs to the board and column in the URL (404 when it does not), or
drop the unused path variables from the mapping. Pin it with a test that deletes a task under a wrong
board id and expects 404. Check the sibling routes (subtasks, columns) for the same pattern first.
