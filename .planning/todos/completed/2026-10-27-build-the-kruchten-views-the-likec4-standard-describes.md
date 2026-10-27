---
created: 2026-10-27T12:40:00.000Z
title: Build the Kruchten views the LikeC4 standard describes
area: docs
severity: minor
files:
  - docs/architecture/model/views.c4
  - docs/architecture/model/spec.c4
  - docs/architecture/model/model.c4
  - docs/architecture/sync.mjs
  - docs/DIAGRAM_CONVENTIONS.md
  - scripts/pre-commit-likec4-selftest.py
---

## Problem

The LikeC4 standard and its drift gate exist, but the Kruchten views the standard describes were never
built. `docs/architecture/model/views.c4` holds only the five views moved unchanged from spike 006:
`index`, `process`, `data`, `production`, `signin`. A reader opening the viewer sees the spike, not the
4+1 set.

Filed 2026-10-27 at HEAD 5ca7d18 on main. Commits d39803e, c9e4e15 and 5ca7d18 (plus the commit that
adds this todo) are local and unpushed; everything before them is pushed.

### What exists (do not rebuild)

- The model lives in `docs/architecture/`: `model/*.c4`, `sync.mjs` (drift script, `generate` and
  `check`), `likec4.fish` (wrapper: `start|validate|png|site|check|sync`), `drill.fish` and
  `drill-output.txt`, `package.json` pinning likec4 1.60.0. Install once per checkout with
  `pnpm --dir docs/architecture install`.
- The standard is `docs/DIAGRAM_CONVENTIONS.md`, section "## LikeC4 standard": viewpoint table,
  provenance ladder (generated, checked, authored), V1 to V6 verification outcomes, and what is
  undefined or not adopted.
- The drift check runs in `.githooks/pre-commit` (fails closed, path-gated) and in the
  `likec4-model-drift` job of `.github/workflows/invariant-checks.yml`. It is proven red and green by
  `scripts/pre-commit-likec4-selftest.py`.
- Evidence and decisions: `.planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/`
  (`261027-f8q-BRIEF.md` is the approved design, `verify/` holds the experiments) and
  `.planning/quick/261027-fuj-move-the-likec4-model-and-drift-check-to/` (SUMMARY, VERIFICATION).
  Spikes 005 to 007 under `.planning/spikes/` are history.
- The viewer serves at http://localhost:5173/ (usually; it prints the address). Zed tasks with the
  prefix `architecture:` are in `.zed/tasks.json`.

### The gap, view by view

- **Logical (`data`):** six unconnected table boxes. The standard wants the tables plus the 4 FK
  ownership edges (boards to users, columns to boards, tasks to columns, subtasks to tasks). They were
  verified from a Flyway-migrated scratch Postgres, and a regex over `REFERENCES` gives the same 4
  today. It also wants Spring Session's tables as AUTHORED, labelled elements: the app creates them at
  startup (`spring.session.jdbc.initialize-schema=always`, `application.properties` around line 187),
  outside Flyway. The FK edges should be GENERATED into `model/generated.c4` by `sync.mjs` (extend
  `generate` and `check`). Keep the regex extraction in the gate; the scratch-Postgres route stays a
  documented option, not wired.
- **Process (`process`):** exists, but several descriptions were written from the old Mermaid labels
  and never checked against the Java: Tomcat request threads, `@TransactionalEventListener(AFTER_COMMIT)`,
  the Spring Kafka listener and group `activity-log`, the "session tables" JDBC label, and "shared by
  prod and nonprod databases". Verify them in `src/main/java` (`event/`, `activitylog/`,
  `config/KafkaConsumerConfig.java`, `constant/KafkaTopics.java`) or soften them. Numbers in labels
  (3 retries 1 s apart, the 02:17 schedule, ceiling 2) must be covered by a check or removed, per the
  standard's rule.
- **Development: MISSING.** The standard wants top-level packages as `component` of `kanban`, with
  dependency edges GENERATED from import statements. Measured: 38 edges over 14 top-level packages
  (base, service, dto, repository, handler, entity, controller, event, exception, security,
  activitylog, config, constant, mapper). The package graph is CYCLIC today (base and entity import
  each other; config to security to service to config; `tsort` reports it), so there is NO no-cycles
  gate (the standard marks that NOT ADOPTED). Report cycles, do not gate on them. Whether 38 edges is
  readable in one view is UNMEASURED; collapsing to layers is a possibility, not a result, so render it
  and look. The extraction used in the experiment is
  `.planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/pkg.fish` (fish, rg and
  tsort).
- **Physical (`production`):** done. The CPU architecture is in the title (Netcup VPS (x86_64)).
- **Scenarios (`signin`):** happy path only. The standard wants ONE dynamic view PER OUTCOME
  (`signin-ok`, `signin-bad-credentials`, `signin-ceiling`), because one trace is one Kruchten
  scenario. Working `variant sequence` examples are in
  `.planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/verify/lc4/exp.c4`; they render
  with `likec4 export png --seq`, and without `--seq` the PNG draws the ordinary layout. The source of
  truth for the steps is `docs/diagrams/scenarios/signin.mmd`: the 401 `BAD_CREDENTIALS` body is
  identical for an unknown email, a wrong password and the 2-live-sessions ceiling. Not yet modelled:
  signup, error-status-split, push-to-image, inbound-packet-path. They need new elements (CI runner,
  registry, Flux, network layers). Migrating the seven Mermaid diagrams and retiring
  `render-diagrams.sh`, `verify-diagrams.py`, `mermaid-config.json` and `render-manifest.tsv` is a LATER
  change, not this todo.
- **Cross-cutting:** every view needs a viewpoint tag (declare one tag per Kruchten view in `spec.c4`)
  and a check that fails on an untagged view. Two more checks are unbuilt: an unknown-metadata-key
  check (the DSL cannot declare allowed metadata keys, V2 NOT ADOPTED, so a typo key validates
  silently) and an "every `server` node title names an architecture" check. If built, add them to
  `sync.mjs check` and to the self-test, proven red then green.

### Decision to default unless the user says otherwise

Scenario layout. `variant sequence` draws a UML sequence diagram, matching the old `signin.mmd`, at
1599 px natural width, which exceeds the Mermaid-derived 1117 px bound. `autoLayout TopBottom` on the
diagram variant is 692 x 1252 and passes. The standard leaves it open because the legibility rule is
UNDEFINED for LikeC4 (the label font size was not measured). Recommendation: use the sequence layout
for scenarios and treat the width bound as not applicable to LikeC4 until a LikeC4 rule is defined.
`alt`/`else` inside ONE view is untested and not adopted.

### Open items, not part of this todo

- `.zed/tasks.json` still has Gradle tasks using `./gradlew.bat`. Zed may run in WSL, where those
  fail; the user is to confirm.
- 14 `.avsc` files exist while `application.properties` says "5 event types". Unresolved; it matters
  only if event types become elements.
- `scripts/verify-diagrams.py` exits 1 at HEAD on `docs/learning/kafka-architecture-overview.md:11`.
  Pre-existing and unrelated.

## Solution

Work it with `/gsd-quick`. It edits `sync.mjs`, the model and probably the hook self-test, so the
feature commit must run the REAL `.githooks/pre-commit`: run it in the background with a bounded wait,
never through the 30 s `gsd_run query commit` helper, which kills the hook mid-run and orphans it. Install
the tooling in the checkout first.

Verify by looking at the rendered views (`fish docs/architecture/likec4.fish png`, output in the
gitignored `docs/architecture/out/`), and with `node docs/architecture/sync.mjs check`,
`fish docs/architecture/drill.fish` (it must reproduce `docs/architecture/drill-output.txt`; extend the
drills if new checks are added) and `python3 scripts/pre-commit-likec4-selftest.py`. Keep
`docs/DIAGRAM_CONVENTIONS.md` in step: move items out of "Undefined, not adopted, and outside this
change" as they are built, and keep every number sourced.

Machine rules on this host: a Bash call over about 300 characters is refused (write a script file),
no `cd`, no `rm` (use `trash-put`), `rg` with explicit paths, `pnpm` not npm, and never push without
being told.

Acceptance:

- each Kruchten viewpoint has at least one view tagged with its viewpoint;
- the Logical view shows the 4 FK edges (generated) plus labelled session tables;
- a Development view exists (generated edges, readability judged from a render, cycles reported not
  gated);
- signin is split per outcome;
- the unverified descriptions in `process` are verified or softened;
- any check added is proven red, then green;
- the standard doc is updated to match what is built.
