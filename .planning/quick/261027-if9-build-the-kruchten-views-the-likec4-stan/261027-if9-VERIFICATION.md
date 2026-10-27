---
phase: quick-261027-if9
verified: 2026-10-27T13:30:00Z
status: human_needed
score: 9/9 must-haves verified (3 items need a human look: see human_verification)
covered_files:
  - .githooks/pre-commit
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-PLAN.md
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T1-SUMMARY.md
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T2-SUMMARY.md
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T3-SUMMARY.md
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T4-SUMMARY.md
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T5-SUMMARY.md
  - .planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T6-SUMMARY.md
  - docs/DIAGRAM_CONVENTIONS.md
  - docs/architecture/drill-output.txt
  - docs/architecture/drill.fish
  - docs/architecture/likec4.fish
  - docs/architecture/model/generated.c4
  - docs/architecture/model/model.c4
  - docs/architecture/model/spec.c4
  - docs/architecture/model/views.c4
  - docs/architecture/sync.mjs
  - scripts/pre-commit-likec4-selftest.py
covered_digest: "v3:sha256:ada9259473e0cd0c9d0e894ec05d7688e502a40f447e60d27756bf6abe8ce4f5"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Open the development view (viewer, or docs/architecture/out/development.png after fish docs/architecture/likec4.fish png) at natural size (4475 x 2636)."
    expected: "All 14 package labels are legible, and a reader can tell the direction of the three arcs at the left of entity and the eight arrows into constant. Both are told apart only by arrowheads, since edges carry no label."
    why_human: "The executor's readability verdict and my own look were both made on an image downscaled to about 2000 px wide. Nobody has seen it at natural size. The standard's wording says so, which is honest, but 'readable' is a human judgement the code cannot hold."
  - test: "Push the branch (the user's call, nothing was pushed) and read the likec4-model-drift job in .github/workflows/invariant-checks.yml."
    expected: "The job passes on the final tree and would fail on a Java commit that adds a cross-package import."
    why_human: "The CI job was not run. The hook deliberately does not trigger on src/main/java, so cross-package import drift is gated by CI alone. I ran the same check locally (exit 0) but not the job."
  - test: "Read the three signin sequence views, the process view and the production view in the viewer or at natural size."
    expected: "Labels and arrows read cleanly, and the scenario widths (1838, 1500 and 1838 px) are acceptable to the user."
    why_human: "The standard leaves the LikeC4 legibility rule undefined (12 px floor not measured), so there is no gate to hold them. I read signin-ceiling, data, development and process downscaled. I did not read index, production, signin-ok or signin-bad-credentials images, only their dimensions."
---

# Quick 261027-if9: build the Kruchten views the LikeC4 standard describes - Verification Report

**Goal:** Build the Kruchten views the LikeC4 standard describes (todo "Solution" and "Acceptance").
**Verified:** 2026-10-27
**Status:** human_needed (no gaps; three human-only items)
**Re-verification:** No, initial verification.

All checks below were run by me against the tree at HEAD 106ed4a, not taken from the SUMMARY files.

## Commands run (exit codes read directly)

| Command | Result |
|---|---|
| `node docs/architecture/sync.mjs check` | exit 0. `OK (33 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 4 workloads)` and `package cycles (reported, not gated): base, config, dto, entity, mapper, repository, security, service` |
| `fish docs/architecture/drill.fish > /tmp/if9-drill-now.txt`, then `diff` against `drill-output.txt` | drill exit 0, diff empty (byte-identical). `git status --short docs/architecture` empty afterwards, so the drills did not touch the real model |
| `python3 scripts/pre-commit-likec4-selftest.py` | `15/15 passed`, exit 0, run against the real `.githooks/pre-commit` |
| `fish docs/architecture/likec4.fish validate` | `Valid (5 files)` |
| `likec4 export png --seq -o /tmp/if9-out docs/architecture/model` | 8 views exported: index, process, data, development, production, signin-ok, signin-bad-credentials, signin-ceiling. No `signin.png` |

`docs/architecture/node_modules` was present, so no install was run.

## Acceptance bullets (todo)

| # | Acceptance | Status | Evidence |
|---|---|---|---|
| 1 | Each Kruchten viewpoint has at least one view tagged with it | VERIFIED | `views.c4`: process `#process` (l.9), data `#logical` (l.20), development `#development` (l.27), production `#physical` (l.34), signin-ok, signin-bad-credentials and signin-ceiling `#scenarios`. `index` has none. `spec.c4` l.69-73 declares the five tags. `sync.mjs` l.296-306 enforces exactly one tag per non-index view and a view for each viewpoint. D6, D6b and D9 are caught in the drill output I reproduced, and self-tests 12 and 15 pass |
| 2 | Logical view shows the 4 FK edges (generated) plus labelled session tables | VERIFIED | `generated.c4` l.31-42 holds 4 `-[fk]->` edges: boards to users, columns to boards, tasks to columns, subtasks to tasks. An independent `rg -i references` over the migrations returns exactly 4 lines, all in V1__init.sql (l.18, 25, 33, 41), the same set. `model.c4` l.51-64 has `spring_session` and `spring_session_attributes`, each described "Authored. ...", and l.102 has the authored fk edge. I read `data.png` (2028 x 2604): 8 tables, 5 labelled edges, "Authored" visible on both session tables. I checked the FK clause (`SPRING_SESSION_ATTRIBUTES_FK ... REFERENCES SPRING_SESSION(PRIMARY_ID)`) directly in the spring-session-jdbc 3.5.7 jar. application.properties l.175 and l.187 match the claims |
| 3 | Development view exists, generated edges, readability judged from a render, cycles reported not gated | VERIFIED (readability verdict is the human item 1) | `generated.c4` holds 14 `pkg_*` components and 38 `-[imports]->` edges. I recomputed the edge set independently from the Java import lines (own fish script, rg): 38 edges, byte-equal to the generated set. The 14 package folders match. `check` prints the cycle line and exits 0. D8 and D8b are caught, and D8b lists `exception, handler` as a second group. The cycle group is 8 packages and I traced it by hand from the edge list. I read `development.png` (8950 x 5272). The T3-png.txt verdict is recorded with three layouts tried, and the "downscaled only" limit is stated in the file and in the standard |
| 4 | Signin split per outcome | VERIFIED | `views.c4` l.40-86: `signin-ok`, `signin-bad-credentials`, `signin-ceiling`, each `#scenarios` and `variant sequence`. The old `signin` view is gone. `likec4.fish` l.28 passes `--seq`. My export draws sequence diagrams (I read `signin-ceiling.png`: lifelines, numbered steps, matches the view source). The steps agree with AuthenticationController.java l.83-110 (SELECT by email, 401 on unknown email, identical BadCredentialsException for the ceiling and wrong password) and with UserResponseDTO fields (`displayName`, `theme`) |
| 5 | Unverified descriptions in `process` verified or softened | VERIFIED | `T5-claims.txt` has a row per claim, including one honest COULD NOT VERIFY ("one Avro event per committed mutation", false for user mutations, so softened). I re-checked cited lines: KafkaEventPublisher.java l.102 (`@TransactionalEventListener(AFTER_COMMIT)`), l.105 (`kafkaPublishExecutor.execute`); AsyncConfig.java l.25 (`kafka-publish-`); ActivityLogConsumer.java l.45, l.51; KafkaConsumerConfig.java l.56 and l.203. All hold. `rg "3 retries\|1 s apart\|02:17\|1 partition\|ceiling 2\|daily\|[0-9]+ s \|retries 3"` over `docs/architecture/model` returns nothing. I read `process.png`: no number in a label. `backup` is a `job` (spec.c4 l.17, model.c4 l.67) |
| 6 | Any check added is proven red, then green | VERIFIED | Red files exist for T1-T4 and show what the plan required: T1 D6 and D6b `exit=0` and test_12 FAIL (11/12); T2 D7, D7b, D7c `exit=0` and test_13 FAIL (12/13); T3 D8 `exit=0`, test_14 FAIL (13/14), no `package cycles` line, 0 generated edges; T4 D9 `exit=0` and test_15 FAIL (14/15). Green is the state I reproduced: all of those now exit 1 with the named finding, 15/15. D5 stays `exit=0` as the negative control |
| 7 | Standard doc updated to match what is built | VERIFIED | See next section |

## Must-haves from PLAN frontmatter

| # | Truth | Status | Evidence |
|---|---|---|---|
| 1 | One viewpoint tag per Kruchten view, none on index | VERIFIED | Acceptance 1 |
| 2 | `check` refuses an untagged or double-tagged non-index view and a viewpoint with no view | VERIFIED | `sync.mjs` l.296-306; drill D6, D6b, D9 and self-tests 12 and 15 |
| 3 | Logical view: generated tables, 4 generated FKs, 2 authored session tables | VERIFIED | Acceptance 2 |
| 4 | A migration FK change or a new Java cross-package import makes `generated.c4` stale | VERIFIED | D7, D7b, D7c, D8 are all reported stale in my reproduction. Self-tests 13 and 14 refuse through the real hook |
| 5 | Development view generated, readability a recorded verdict, cycles printed and never gated | VERIFIED | Acceptance 3. The exit code stays 0 with the cycle line present |
| 6 | Signin is three sequence views, `png` draws them as sequence, old view gone | VERIFIED | Acceptance 4 |
| 7 | Process claims verified or softened, no retry, schedule or ceiling number in the model | VERIFIED | Acceptance 5 |
| 8 | Every rule seen red before green, with evidence files | VERIFIED | Acceptance 6 |
| 9 | Standard describes what is built, numbers cited | VERIFIED | Next section |

## DIAGRAM_CONVENTIONS.md spot-check against evidence

- Sizes in the "Built in 261027-if9" table equal my fresh render halved: index 836 x 1204 (1672 x 2408), data 1014 x 1302, process 1236 x 1536, development 4475 x 2636, production 1965 x 1241, signin-ok 1838 x 964, signin-bad-credentials 1500 x 574, signin-ceiling 1838 x 880. All match.
- "12 of 12 drills that introduce drift are caught (D1 to D4, D6, D6b, D7, D7b, D7c, D8, D8b, D9)": 4 + 8 = 12, matches the drill output. D5 is named as the negative control.
- "15 cases, 9 of them drift refusals": cases 1-5 and 12-15 = 9. Matches the self-test output.
- "median 958 ms" cites T3-green.txt l.51 (910, 958, 1039; ratio 1.03). Matches.
- "38 = 38" and the 8-package group match my independent recomputation.
- The Wired paragraph names the CI-only hole, and `.githooks/pre-commit` (commit 8b7e379) carries the same comment and an updated refusal message.
- The unknown-metadata-key and server-title checks stay listed as proposed. `sync.mjs` has neither, consistent with the doc.
- The old text the plan said to remove ("three unbuilt checks", "does not choose") is gone. The V rows were left as dated records, as planned.

## Anti-patterns and debris

- No TBD, FIXME or XXX in the changed files (checked by reading `sync.mjs`, the models and the doc section).
- Scratch cleanup: `QDIR/scratch` and `docs/architecture/out` are absent. `git status --short -uno` is clean. The untracked files are the pre-existing 261024 ones plus this task's evidence.
- Six commits, each touching only its task's paths (`git show --stat`). T5 and T6 correctly touch no LikeC4 banner-triggering Java or migration paths.
- Two Testcontainers containers exist from 2026-10-26 10:50 (`recursing_blackwell`, `brave_mclaren`), before this task began, so they are not from these commits. Older ones exist too (the known no-Ryuk issue in memory). Reported, not acted on.

## Observations (not gaps)

- Standing limitation, recorded in `sync.mjs` and the doc: the FK regex does not follow DO blocks, renames or `DROP TABLE`, and a root-level class import exclusion is exercised by no fixture. These are stated limitations, not hidden ones.
- In the process view the `client` box has no edge (traefik is outside that view). Pre-existing from spike 006, and not in this task's scope.
- The Mermaid-derived width bound is deliberately not applied to LikeC4 views (the todo's defaulted decision). No LikeC4 legibility rule exists, so nothing machine-checks view readability.

## Human verification required

1. **Development view at natural size.** Test: open it at 4475 x 2636. Expected: labels legible, arc directions tellable. Why human: every readability judgement so far, mine included, was made on a downscaled image.
2. **CI job `likec4-model-drift`.** Test: run it on a pushed branch. Expected: green on this tree. Why human: not run here, and it is the only gate for Java import drift.
3. **Scenario, process and production readability at natural size.** Why human: there is no LikeC4 legibility rule. I read only some of the PNGs, downscaled.

## Gaps Summary

None. Every acceptance bullet and every PLAN must-have holds in the tree, and the three commands the task named reproduce cleanly. The status is human_needed only because readability and the CI run cannot be settled by code.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
