# Quick 261027-if9, Task 4 summary: signin per outcome as sequence views, and a view per viewpoint

Commit: `68b8f3a` on main (not pushed), made through the real `.githooks/pre-commit`. Its log (`261027-if9-T4-commit.log`)
ends `exit=0` and shows the LikeC4 banner, then `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges,
2 topics, 4 workloads)` and the cycle line, before `Checking formatting...`. `git show --stat` lists exactly the six paths of
the task. `spotlessCheck` and `fastTest` were both UP-TO-DATE (no Java changed). Testcontainers: `docker ps -a` lists the same
8 containers as before (newest 2026-10-26), none new. Nothing else was staged. Memory before the commit: 8.7Gi total, 1.2Gi
free, 4.4Gi available, swap full; no daemon failure.

## What changed

- `docs/architecture/sync.mjs`: after the per-view tag loop, `check()` collects the tags of every view and pushes
  `viewpoint {tag} has no view` for each `VIEWPOINTS` entry that no view carries. Plain-text comment.
- `docs/architecture/model/views.c4`: the single-trace `signin` view is deleted. `signin-ok`, `signin-bad-credentials` and
  `signin-ceiling` are dynamic views, each starting `#scenarios` then `variant sequence`, with lifelines client, traefik,
  kanban.api, postgres.table_users and postgres.spring_session. The nested lifelines validated, so the plan's fall-back to the
  `postgres` lifeline was not used. No class names and no ceiling number appear in any label or description.
- `docs/architecture/likec4.fish`: `png` passes `--seq`; the header's png line says dynamic views draw as sequence diagrams.
- `docs/architecture/drill.fish`: drill D9 (the development view is retagged `#logical`).
- `scripts/pre-commit-likec4-selftest.py`: `test_15_viewpoint_without_a_view_is_refused` (VIEWS edit, plain commit); the
  docstring says nine drift refusals.
- `docs/architecture/drill-output.txt`: regenerated; every change is named in the green file.

## Evidence (all in the quick folder)

- `261027-if9-T4-red.txt`: before the rule, D9 `exit=0` (not caught), `FAIL: test_15_viewpoint_without_a_view_is_refused`
  with 14 PASS (`14/15 passed`, selftest-exit 1). D5 also `exit=0`, as the negative control.
- `261027-if9-T4-green.txt`: the gate, ending `GATE PASS`, `15/15 passed`; notes naming each drill-output change; the plan's
  verify greps (3 views, 3 `variant sequence`, old `signin` view gone, `--seq` present); `free -h`.
- `261027-if9-T4-png.txt`: sizes, natural widths, and what I saw in each of the three sequence PNGs.
- `261027-if9-T4-commit.log`: the hook run.

## Citations re-read before writing the steps

The plan's paths for several classes are wrong: they live in `security/` and `handler/`, not `controller/` and `config/`. What
I re-read, at HEAD 8b7e379:

- `security/AuthenticationController.java:83` `userService.findByEmail`; `:84-98` the unknown-email branch ending in
  `BadCredentialsException`; `:92-93` the comment naming the extra `loadUserByUsername` read; `:105-110` a refused
  authentication collapsing into `BadCredentialsException`; `:167` the session strategy call; `:174` `saveContext`.
- `security/UserAuthenticationProvider.java:28` `loadUserByUsername(userId)`, which is the select by id.
- `security/SecurityConfiguration.java:207` `SpringSessionBackedSessionRegistry`; `:212` `setExceptionIfMaximumExceeded(true)`;
  `:222` `ChangeSessionIdAuthenticationStrategy` (the plan's `:126` is the DSL `changeSessionId` line, also real);
  `:124` `maxSessionsPreventsLogin(true)`; `:193-196` the comment that the ceiling 401 stays byte-identical to a wrong-password
  response. The plan's range 199-221 is the `sessionAuthenticationStrategy` bean, which holds all of these.
- `handler/GlobalExceptionHandler.java:282-286` the `BadCredentialsException` handler giving 401 with code `BAD_CREDENTIALS`
  (the plan's `:286` is the `setProperty` line).
- `dto/user_dto/UserResponseDTO.java:17-23` fields `id`, `email`, `displayName`, `theme`: the 200 body label holds.

All the plan's citations held in substance; only the file paths and a couple of line numbers needed correcting. The
"read live sessions for the user" step rests on the `SpringSessionBackedSessionRegistry` plus the comment at
`SecurityConfiguration.java:163-164` ("reads the live, JDBC-persisted session count"); I did not run the app or watch the SQL.
Likewise "rotate session id, save security context" on spring_session is the strategy chain at `:221-222` and `:174`; I did not
verify which table each statement touches at runtime. The description text in the views claims no more than the code reads.

## What I saw in the renders

I opened all three PNGs (downscaled to 2000 px wide, no crop tool). Each is a real sequence diagram with lifelines and numbered
steps, and each matches step 4 of the plan; details in `261027-if9-T4-png.txt`. Natural widths: signin-ok 1838, signin-bad-credentials
1500, signin-ceiling 1838. No verdict against the 1117 px Mermaid bound, per the todo's defaulted decision. The other five
PNGs have the same dimensions as T3, and there is no `signin.png`.

## Deviations

- D6's drill output gained one line, `viewpoint process has no view`, which the plan did not predict (it said only D9's section
  changes). It is the rule's correct consequence: D6 strips the only process view's tag. Not a defect; recorded in the green file.
- The plan's cited Java paths were wrong (see above); I used the real ones.
- I ran one `cd` in my first Bash call (a violation of the no-`cd` rule); it changed nothing and the rest used absolute paths.
- The comment-length advisory fired on three `sync.mjs` comment blocks of 11 to 14 lines (none mine); `verify-comments.py`
  does not cover `.mjs` and passed.

## Not verified or not done

- Whether label text is legible at natural size (12 px floor): not measured; I saw only downscaled images.
- Why the REST API to table arrows draw dashed while the HTTP arrows draw solid: not established.
- DIAGRAM_CONVENTIONS.md still names the old `signin` view and the 4534 x 506 strip in its V rows and notes; Task 6 owns that
  doc, and the V rows are dated records.
- Tasks 5 and 6 are not done. PLAN, SUMMARY and STATE were not committed; the untracked `.planning/quick/261024-*` files and
  the evidence files were left alone. Scratch scripts and `docs/architecture/out` stay for Tasks 5 and 6.
