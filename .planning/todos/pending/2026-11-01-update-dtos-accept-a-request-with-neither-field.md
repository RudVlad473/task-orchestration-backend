---
created: 2026-10-31T23:10:00.000Z
title: Update DTOs accept a request with neither field
area: api
severity: major
files:
  - src/main/java/com/vrudenko/kanban_board/dto/task_dto/UpdateTaskRequestDTO.java:30
  - src/main/java/com/vrudenko/kanban_board/dto/subtask_dto/UpdateSubtaskRequestDTO.java:32
---

## Problem

Found by the second talos blind run (2026-10-31) and reproduced by all three review lanes. An update request
with neither `title` nor `description` (task) or neither `title` nor `isCompleted` (subtask) is accepted. The
rule exists as `@AssertTrue(message = "Either 'title' or 'description' (or both) must be provided.")` on
`private boolean atLeastOneFieldPopulated()` in `UpdateTaskRequestDTO` (line 30) and in
`UpdateSubtaskRequestDTO` (line 32). Bean Validation applies `@AssertTrue` to a getter-style method
(`isX`/`getX`), and this one is private with a non-getter name, so the check never runs. I re-read both
files on 2026-10-31 and did not re-run the reproduction today.

## Solution

TBD. Rename each method to a getter form such as `isAtLeastOneFieldPopulated()` and make it visible to
validation (Bean Validation reads public getters), or move the rule to a class-level constraint. Pin it
with a test that PUTs only `version` and expects 400, for the task and the subtask.
