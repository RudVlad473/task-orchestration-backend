---
created: 2026-10-31T23:10:00.000Z
title: Task delete leaves position gaps so a later create can duplicate a position
area: api
severity: major
files:
  - src/main/java/com/vrudenko/kanban_board/service/TaskService.java:322
  - src/main/java/com/vrudenko/kanban_board/service/TaskService.java:118
---

## Problem

Found by the second talos blind run (2026-10-31) and reproduced by all three review lanes (Claude, Gemini,
Codex). I re-read the code on 2026-10-31: `TaskService.deleteById` (line 322) deletes the task's subtasks and
then the task, publishes `TaskDeletedEvent`, and renumbers no sibling. Deleting a task from the middle of a
column therefore leaves a gap in the column's positions. Create then sets the new task's position to the
sibling count (`taskRepository.countByColumnId`, line 118) under the comment "Positions stay contiguous from
zero in every mutation here" (line 115): after a middle delete the count equals a position another task
already holds, so the new task duplicates it. The comment's invariant is what delete breaks. I did not
re-run the reproduction today.

## Solution

TBD. Either renumber the column's remaining tasks after a delete, or have create take max(position) + 1.
Pin it with a test that deletes a middle task, creates another, and asserts the positions are unique. Check
the same path for subtasks (`SubtaskService.deleteById`).
