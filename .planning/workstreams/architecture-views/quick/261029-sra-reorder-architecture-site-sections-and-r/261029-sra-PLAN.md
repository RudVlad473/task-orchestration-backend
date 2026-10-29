---
phase: quick-261029-sra
plan: 01
type: quick
wave: 1
depends_on: []
autonomous: true
requirements: []
files_modified:
  - docs/architecture/sync.mjs
  - docs/architecture/model/views.c4
  - docs/architecture/model/monitoring.c4
  - docs/architecture/model/scenarios-app.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/drills.tsv
  - docs/architecture/drill-output.txt
  - scripts/pre-commit-likec4-selftest.py
  - docs/DIAGRAM_CONVENTIONS.md
  - .planning/workstreams/architecture-views/quick/261029-sra-reorder-architecture-site-sections-and-r/261029-sra-evidence.txt

estimate:
  tokens: 110000
  raw_tokens: 110000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "LikeC4's own folder order (computed.rootViewFolder.folders) reads 1 Physical, 2 Process, 3 Logical, 4 Application behaviour, 5 Platform and delivery, 6 Development, and index stays at the root"
    - "sync.mjs check refuses a folder set whose LikeC4 order differs from FOLDER_ORDER: the new script exits 1 on the HEAD model, and a scratch copy with two FOLDER_ORDER entries swapped exits 1 on the new model with only the order line"
    - "The development view draws all 14 packages inside four groups (Web: controller, handler, security; Application: service, activitylog, mapper, event, config; Persistence: repository, entity; Shared kernel: dto, base, constant, exception) and 12 edges, down from 14 nodes and 38 edges at HEAD"
    - "No edge into the shared kernel and no edge inside one layer is drawn; the four upward edges config->security, entity->config, base->entity and dto->entity are red and solid, each labelled with the cycle it closes; security->entity is labelled as a layer skip the ArchTest allows"
    - "A package left out of the development view is refused by sync.mjs check (drill D9b exit 1), and a scratch sync.mjs without that rule passes the same edit (exit 0)"
    - "Every drill and self-test that names a folder label still fails on its own edit: drill.fish output differs from the recorded drill-output.txt only on the intended lines, and the hook self-test prints 20/20 passed"
    - "The site built with CI's command passes the leak scan (exit 0), the leak-scan fixture tests pass, and generated.c4 is byte-identical to HEAD"
  artifacts:
    - path: docs/architecture/sync.mjs
      provides: "new folder labels, FOLDER_ORDER with checkFolderOrder, checkPackagesDrawn"
      contains: "rootViewFolder"
    - path: docs/architecture/model/views.c4
      provides: "relabelled folder blocks and the layered development view"
      contains: "group 'Shared kernel'"
    - path: docs/architecture/drills.tsv
      provides: "D29 and D29b retargeted to the new labels, new D9b"
      contains: "D9b"
    - path: docs/DIAGRAM_CONVENTIONS.md
      provides: "folder names, the layer key with evidence, the before and after measurements"
      contains: "6 Development"
  key_links:
    - from: "sync.mjs VIEW_FOLDERS and SCENARIO_FOLDERS"
      to: "the views '<label>' heads in views.c4, monitoring.c4, scenarios-app.c4, scenarios-platform.c4"
      via: "checkViewFolders compares view.folder.path to the label"
    - from: "sync.mjs FOLDER_ORDER"
      to: "LikeC4 computed.rootViewFolder.folders"
      via: "checkFolderOrder, the pin for the section order"
    - from: "views.c4 group includes (kanban.pkg_*)"
      to: "generated.c4 package components from sync.mjs packages()"
      via: "checkPackagesDrawn"
    - from: "drills.tsv D29, D29b and selftest test_18, test_19"
      to: "the new folder labels"
      via: "sed and replace_in anchors that silently no-op when a label changes"
---

<objective>
Reorder the architecture site's navigation sections to Physical, Process, Logical, Application behaviour, Platform and delivery, Development (titles numbered 1 to 6; the owner's "4a, 4b" read as the two scenario folders), and rebuild the Development view as a layered view that draws only the imports crossing layers, with cycle edges marked.

Purpose: the owner reads the site physical-first, and the current Development view (14 packages, 38 equal-weight edges, measured from `likec4 export json` at HEAD; the request said 42, the export and `sync.mjs check` both say 38) cannot be read.

Output: the edited model and checker, two new sync.mjs rules that pin the change, retargeted drills and self-tests, updated DIAGRAM_CONVENTIONS.md, one local commit. No push: a push rebuilds the production site through Flux, and the owner decides that separately.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.claude/CLAUDE.md
@docs/SESSION_LESSONS.md
@docs/DIAGRAM_CONVENTIONS.md
@docs/architecture/sync.mjs
@docs/architecture/model/views.c4
@docs/architecture/drills.tsv
@docs/architecture/drill.fish
@src/test/java/com/vrudenko/kanban_board/architecture/LayeringArchTest.java
@.planning/workstreams/architecture-views/quick/261029-kyd-group-the-14-scenario-views-into-two-fol/261029-kyd-SUMMARY.md

Facts the planner measured on 2026-10-29 (re-measure, do not trust):

- LikeC4 orders folders by label: `computed.rootViewFolder.folders` returns 1 Logical, 2 Process, 3 Development, 4 Physical, 5a Application behaviour, 5b Platform and delivery today. `order N` only orders views inside a folder, so relabelling is the only lever.
- Folder label sites (rg over docs, scripts, src, .githooks, .github, k8s, README): views.c4 (four heads), monitoring.c4 line 126 (`4 Physical`), scenarios-app.c4 line 1, scenarios-platform.c4 line 1, sync.mjs lines 56-61 and 70, drills.tsv D29 (`'5[^']*'`, goes silent under the new label) and D29b (`'4 Physical'`, goes silent), self-test test_18 and test_19, DIAGRAM_CONVENTIONS.md lines 88-95, drill-output.txt (D9, D29, D29b, D29c, D29d lines). No README or doc links a site view URL, and view ids do not change, so site URLs (`/view/<id>`) survive.
- A probe of the layered view on a scratch copy (groups plus the excludes below plus one `with` block) exported 18 nodes (14 packages, 4 `@group` nodes, every package parented by a group), 12 edges, bounds 3760 x 939, against 14 nodes, 38 edges, 4440 x 2580 at HEAD. `likec4 export png -f development --flat` rendered it in 7.5 s and it read legibly; the kernel group laid out to the right of the stack.
- Of the 38 generated edges under the layer key below: 19 point into the shared kernel (17 from other layers plus dto->base and dto->constant), 7 stay inside one layer (controller->security, service->mapper, service->event, service->config, activitylog->event, config->event, repository->entity), 12 cross layers.

Layer key, read from LayeringArchTest.java and the import lines in generated.c4:

- What the ArchTest enforces, all four rules: (1) classes in `..controller..` or annotated `@RestController` must not access `com.vrudenko.kanban_board.repository..` (the annotation half pulls AuthenticationController in the security package into scope); (2) classes in `service..` named `*Service`, except OwnershipVerifierService and UserService, must not call a project repository `findById`; (3) every `@RestController` carries `@Validated`; (4) every POST, PUT or PATCH handler binds each `*RequestDTO` parameter with `@RequestBody` and `@Valid`. Only rule (1) is a layer boundary: web never reaches repository. Every other direction in the view is convention, not a gate.
- security goes in Web: it holds AuthenticationController, which rule (1) treats as a controller; it imports service (security->service, AuthenticationController to UserService) as controller does; controller imports it only for the CurrentUserId annotation (intra-layer).
- config goes in Application: the four domain services import config.EventIdGenerator (service->config), and config.KafkaEventPublisher and AvroSchemaRegistrar import the event types (config->event). Any of the four layers leaves config with two upward edges, so the count does not decide; the users of its classes do. Shared kernel is rejected because it would hide service->config and entity->config under the kernel rule while the kernel depends upward on web and application.
- The four upward edges and the cycle each closes, every hop a generated edge: config->security (config, security, service), entity->config (entity, config, security), base->entity (base and entity import each other; entity->base is hidden as a kernel-inbound edge), dto->entity (dto, entity, config). The one layer skip: security->entity (web to persistence, AuthenticationController to UserEntity), allowed because rule (1) forbids only repository.
</context>

## Approaches considered (project directive)

1. **View-only layering (picked).** In views.c4 only: four `group` blocks list the generated package components, `exclude` drops every edge into a shared-kernel package (by target) and the seven intra-layer pairs, and `include <edge> with { color, line, title }` marks the upward edges. generated.c4 and every element id stay as they are.
2. **Generated layer containers.** sync.mjs nests each `pkg_*` inside a generated layer element, so LikeC4 derives one aggregated edge per layer pair.
3. **Authored layer elements.** An authored .c4 adds four layer components and hand-written `imports` relations between them; the view draws only those.

| Approach | Pros / Cons | Why picked |
|---|---|---|
| 1 View-only groups and excludes | + facts stay generated, ids unchanged, package names stay visible so each red edge names the import that closes a cycle; + a new cross-layer import appears on its own because excludes are targeted. - group membership is hand-listed (a new package would be missing: held by the new checkPackagesDrawn); - a new upward import appears unmarked until someone adds its `with` block | Picked: the only option that changes no generated fact, and the probe proved it renders (12 edges, 4 groups) |
| 2 Generated layer containers | + one edge per layer pair, smallest picture. - rewrites generated.c4 and every `kanban.pkg_*` id; puts the layer map inside the generator; aggregation hides which package import closes each cycle | Rejected: changes generated facts the owner put out of scope and loses the honest cycle detail |
| 3 Authored layer elements | + cleanest picture. - a second, hand-kept copy of the import facts that no check holds, which goes stale silently (the failure the Neon-era infra diagrams showed) | Rejected: authored copies of generated facts drift |

Non-obvious trade-offs:

- State invalidation: hiding kernel-inbound edges also hides entity->base, one half of the base and entity cycle. The upward half is drawn red and the view description says the other half is hidden. A future upward import shows as a plain dotted edge until marked; the cycle line `sync.mjs check` prints on every run is what announces a new cycle.
- Folder relabel: view ids are unchanged, so site URLs keep working; the folder paths change, which is what the order rule reads.
- Security: view titles, descriptions and edge labels ship to a public site. They name only packages, layers and LayeringArchTest: no host, IP, namespace, credential or user name, and no word the leak scan targets (`secret`, `kubectl`, `ssh `, `/home/`). The built output is scanned.
- Performance: the two new checks iterate one view's nodes and the root folders, negligible next to the model load.
- Scope note: the brief said only authored .c4 files change. The folder labels are also held by sync.mjs (checkViewFolders), the drills and the self-test, so those authored files change too; leaving them would fail the check or turn D29, D29b, test_18 and test_19 into silent no-ops. generated.c4 and spec.c4 do not change.

Core data flow (3 sentences): sync.mjs reads the Java import lines and writes the 14 package components and 38 `imports` edges into generated.c4. views.c4 projects them into the development view, grouping packages into layers, dropping kernel-inbound and intra-layer edges and restyling the upward ones, while the `views '<label>'` heads place every view in a numbered folder. LikeC4 computes the model and builds the static site, and `sync.mjs check` reads that computed model back to hold each view's folder, the folder order and that every package is drawn.

<tasks>

<task type="tracer">
  <name>Task 1: Reorder the sections end to end and pin the order with a sync.mjs rule</name>
  <files>docs/architecture/sync.mjs, docs/architecture/model/views.c4, docs/architecture/model/monitoring.c4, docs/architecture/model/scenarios-app.c4, docs/architecture/model/scenarios-platform.c4, docs/architecture/drills.tsv, scripts/pre-commit-likec4-selftest.py, docs/DIAGRAM_CONVENTIONS.md, .planning/workstreams/architecture-views/quick/261029-sra-reorder-architecture-site-sections-and-r/261029-sra-evidence.txt</files>
  <action>
Before editing, copy the HEAD model for the red half: `git archive HEAD docs/architecture/model | tar -x -C docs/architecture/out/head` after `mkdir -p docs/architecture/out/head` (out/ is gitignored; delete it in Task 3).

Model: relabel each `views '<label>' {` head in place, without moving blocks: views.c4 `1 Logical` to `3 Logical`, `3 Development` to `6 Development`, `4 Physical` to `1 Physical`; monitoring.c4 `4 Physical` to `1 Physical`; scenarios-app.c4 `5a Application behaviour` to `4 Application behaviour`; scenarios-platform.c4 `5b Platform and delivery` to `5 Platform and delivery`. `2 Process` keeps its label. Leave every `order N` line, view id and tag line untouched (drills D6, D9, D28 and self-tests 12, 15, 17 anchor on those lines).

sync.mjs: set VIEW_FOLDERS to the new labels and SCENARIO_FOLDERS to `4 Application behaviour` and `5 Platform and delivery`. Add FOLDER_ORDER, the six labels in reading order taken by key from those two maps (physical, process, logical, application, platform, development), so each label is written once. Add checkFolderOrder(computed, problems): read `computed.rootViewFolder.folders.map((f) => f.path)`, and when it differs from FOLDER_ORDER push one problem of the form `view folders sort as [<have joined by ', '>], expected [<want>]`; call it from check() right after checkViewFolders. Rewrite the VIEW_FOLDERS comment: drop "give Kruchten order", say the numbers make LikeC4's alphabetical folder sort give the reading order in FOLDER_ORDER, and that checkFolderOrder holds it. Comments follow docs/CODE_STYLE.md rule 14 and must not contain a planning id such as a quick-task id (verify-comments.py bans them).

drills.tsv: D29 sed becomes label-agnostic, `s|^views '[^']*' {$|views {|` (scenarios-app.c4 has one block). D29b sed becomes `s|^views '1 Physical' {$|views '3 Logical' {|`. scripts/pre-commit-likec4-selftest.py: test_18 replaces `views '4 Application behaviour' {\n` and expects `view signin-ok is in folder '', expected '4 Application behaviour'`; test_19 replaces `views '5 Platform and delivery' {\n` with `views '4 Application behaviour' {\n` and expects `view push-to-image is in folder '4 Application behaviour', expected '5 Platform and delivery'`.

DIAGRAM_CONVENTIONS.md lines 88-95: name the six new labels in reading order, keep the participant heuristic for the two scenario folders, and add that `sync.mjs check` refuses a folder set whose LikeC4 order differs from FOLDER_ORDER. Add the order rule to the folder-rule bullet under "Built in the architecture-views workstream, Phase 3.1" or a new "Built in quick task 261029-sra" bullet beside it.

Red and green, both recorded in the evidence file with exit codes: (a) `node docs/architecture/sync.mjs check --model docs/architecture/out/head/docs/architecture/model` exits 1 and lists the folder lines and the order line; (b) the same command without `--model` exits 0; (c) copy sync.mjs to docs/architecture/out/sync-order-mutant.mjs, swap the physical and process entries of FOLDER_ORDER in the copy with Edit, run `node docs/architecture/out/sync-order-mutant.mjs check --root . --model docs/architecture/model`: exit 1 with the order line as the only problem. That proves the order rule, not the folder rule, holds the order.
  </action>
  <verify>
    <automated>node docs/architecture/sync.mjs check && fish docs/architecture/likec4.fish validate && python3 scripts/pre-commit-likec4-selftest.py</automated>
  </verify>
  <done>sync.mjs check prints OK with the 62-element tally; the self-test prints 20/20 passed; the evidence file holds (a) exit 1, (b) exit 0, (c) exit 1 with only the order line.</done>
</task>

<task type="auto">
  <name>Task 2: Rebuild the development view as layers and hold its package coverage</name>
  <files>docs/architecture/model/views.c4, docs/architecture/sync.mjs, docs/architecture/drills.tsv, docs/DIAGRAM_CONVENTIONS.md, .planning/workstreams/architecture-views/quick/261029-sra-reorder-architecture-site-sections-and-r/261029-sra-evidence.txt</files>
  <action>
Before: `docs/architecture/node_modules/.bin/likec4 export json -o docs/architecture/out/before.json docs/architecture/out/head/docs/architecture/model` and `docs/architecture/node_modules/.bin/likec4 export png -f development --flat -o docs/architecture/out/png-before docs/architecture/out/head/docs/architecture/model`. Record node count, edge count and bounds with `jq -c '.views.development | {nodes: (.nodes|length), edges: (.edges|length), w: .bounds.width, h: .bounds.height}'`; expect 14, 38, 4440 x 2580.

views.c4, `view development of kanban` (keep the id, the `#development` line exactly as `    #development`, and the folder `6 Development`): replace `include kanban.* where kind is component` with, in this order:
- four groups, each one include line with packages comma-separated: `group 'Web'` controller, handler, security (the include line exactly `      include kanban.pkg_controller, kanban.pkg_handler, kanban.pkg_security`, which drill D9b anchors on); `group 'Application'` service, activitylog, mapper, event, config; `group 'Persistence'` repository, entity; `group 'Shared kernel'` dto, base, constant, exception. Names are `kanban.pkg_<name>`.
- one `exclude` with `kanban.* -> kanban.pkg_dto`, `-> kanban.pkg_base`, `-> kanban.pkg_constant`, `-> kanban.pkg_exception`, then the seven intra-layer pairs: controller->security, service->mapper, service->event, service->config, activitylog->event, config->event, repository->entity.
- one `include` with a `with` block per marked edge: config->security, entity->config, base->entity and dto->entity each get `color red`, `line solid` and a short title naming the cycle (for example `upward, cycle with security and service` for config->security; base->entity says base and entity import each other); security->entity gets `color amber` and a title saying it skips application and the ArchTest forbids only repository. Before writing each cycle title, confirm every hop is a line in generated.c4 and list those lines in the evidence file.
- title: says the packages are grouped into layers and only imports that cross layers are drawn. Description, plain text ending with a period and no counts (counts go stale when imports change): packages and edges are generated by sync.mjs from the import lines and the four groups are authored; edges into the shared kernel and edges inside a layer are hidden, the other half of the base and entity cycle among them; red edges point up the stack and each closes a package cycle `sync.mjs check` reports; the one boundary a test enforces is LayeringArchTest's rule that no controller, AuthenticationController included, touches repository; security sits in Web because it holds AuthenticationController, and config in Application because services import it and it publishes their events. Use no em dash and none of the leak-scan words listed in the trade-offs.

sync.mjs: add checkPackagesDrawn(computed, pkgs, problems). Find the view with id `development` in computed.views(); if absent push `view development is missing`; otherwise for each name in pkgs.names (from packages(), the fact source) push `view development does not draw package kanban.pkg_<name>` when that id is not among the view's node ids. Call it from check(), computing `packages()` before the call. Its comment names the failure it prevents: group membership is hand-listed in views.c4, so a new package would otherwise be missing from the view while every rule stays green.

drills.tsv: insert after the D9 row `D9b	a package is left out of the development view (handler)	sed	model/views.c4	s|^      include kanban.pkg_controller, kanban.pkg_handler, kanban.pkg_security$|      include kanban.pkg_controller, kanban.pkg_security|` (tab-separated, five fields).

Red and green for the coverage rule, recorded with exit codes: copy the model to docs/architecture/out/cov-model, apply the D9b sed there, run `node docs/architecture/sync.mjs check --model docs/architecture/out/cov-model` (exit 1, the handler line); copy sync.mjs to docs/architecture/out/sync-no-coverage.mjs, delete only the checkPackagesDrawn call with Edit, run `node docs/architecture/out/sync-no-coverage.mjs check --root . --model docs/architecture/out/cov-model` (exit 0).

After: export json to docs/architecture/out/after.json and run `jq -c '.views.development | {nodes: (.nodes|length), edges: (.edges|length), w: .bounds.width, h: .bounds.height, groups: [.nodes[] | select(.kind=="@group") | .title], parents: ([.nodes[] | select(.kind!="@group") | .parent] | unique), e: [.edges[] | "\(.source)>\(.target) \(.color) \(.line) \(.label)"]}'`. Expect 18 nodes, 12 edges, four group titles, no null parent, the four red solid edges and the amber one, no edge whose target is dto, base, constant or exception, and no edge from controller, handler or security to repository. Export the PNG to docs/architecture/out/png-after, Read before and after, and record whether all 14 package names, the four group titles and every edge label are legible. If the layout is unreadable, one try with an `autoLayout` line is allowed; record what was tried.

DIAGRAM_CONVENTIONS.md: rewrite the Development row of the viewpoint table (line 137) for groups, hidden edges and marks; extend note (d) with the layer key, the config and security placement with its evidence, the four upward edges with their cycles, the security->entity skip, and the ArchTest reading key (which of its four rules is a layer boundary); add checkPackagesDrawn beside the order rule in the Checks list; replace the "Unmeasured" bullet on development readability (lines 451-452) with the result; add a dated record for the rebuilt view (before 14 nodes and 38 edges, after 14 packages in 4 groups and 12 edges, and the natural size as PNG size divided by 2, read with `file` on the after PNG). Leave the dated Phase 1 rows and the V5 row as the history they are.
  </action>
  <verify>
    <automated>node docs/architecture/sync.mjs check && fish docs/architecture/likec4.fish validate && python3 scripts/verify-comments.py check</automated>
  </verify>
  <done>sync.mjs check exits 0; the after JSON shows 18 nodes, 12 edges, 4 groups and the five marked edges; the coverage rule's red (exit 1) and mutant (exit 0) are in the evidence file; both PNGs were Read and the legibility verdict recorded.</done>
</task>

<task type="auto">
  <name>Task 3: Full gate on the built site, drills and self-test, then one local commit</name>
  <files>docs/architecture/drill-output.txt, .planning/workstreams/architecture-views/quick/261029-sra-reorder-architecture-site-sections-and-r/261029-sra-evidence.txt</files>
  <action>
Drills: `fish docs/architecture/drill.fish > docs/architecture/out/drill-new.txt 2>&1`, then `diff docs/architecture/drill-output.txt docs/architecture/out/drill-new.txt`. The only differences allowed are: D9's folder line (`'6 Development', expected '3 Logical'`), D29's eight view lines with the new label and its added order line, D29b reading `folder '3 Logical', expected '1 Physical'`, D29c and D29d with the new labels (plus an order line wherever a folder empties), and the new D9b block with exit=1 and the handler line. Every other drill, baseline and the scan drills D30 and D30b must match byte for byte; any other difference is a regression to explain before going on. Then copy drill-new.txt over drill-output.txt and record the diff summary in the evidence file.

Site: `trash-put docs/architecture/site/out` (a stale build from 16:47 holds the old labels, and a rebuild over it leaves stale hashed assets), run CI's exact command `pnpm --dir docs/architecture exec likec4 build --use-dot=false -o site/out model`, then `grep -q 'id="likec4-root"' docs/architecture/site/out/index.html`, `node docs/architecture/site/leak-scan.mjs docs/architecture/site/out` (exit 0, record its last line) and `node --test docs/architecture/site/leak-scan.test.mjs`. Count each new label in the bundle with `rg -o -F '<label>' docs/architecture/site/out/likec4-views.js | wc -l` and confirm `3 Development`, `5a Application behaviour`, `5b Platform and delivery` and `4 Physical` give 0.

UI order, best effort: serve the build with `python3 -m http.server 8099 --bind 127.0.0.1 -d docs/architecture/site/out` in the background, capture the page with `shot-scraper http://127.0.0.1:8099/ -o docs/architecture/out/nav.png --wait 3000` (or the chrome-devtools MCP if the navigation panel must be clicked open), Read the image and record the folder order seen, then stop the server. If the panel cannot be reached headless, write "UI order not observed; held by checkFolderOrder, which reads LikeC4's computed folder order" rather than claiming it.

Final checks: `node docs/architecture/sync.mjs check`, `python3 scripts/pre-commit-likec4-selftest.py` (20/20), `python3 scripts/verify-comments.py check`, `git diff --quiet HEAD -- docs/architecture/model/generated.c4 docs/architecture/model/spec.c4` (exit 0). `git diff --stat` must list exactly the nine repo files in files_modified besides the evidence file.

Commit once, on the branch the quick workflow runs on, with the hook (no `--no-verify`: the commit changes checker code and the model, and the hook's LikeC4 step is the gate that holds them; it needs Docker and takes about 4 minutes): `git commit -m "<conventional message saying why>" -- <the nine paths>`, with the Co-Authored-By trailer from the session. If the hook refuses, fix the cause and commit again. Never push. Leave the unrelated untracked files under .planning/quick/261024-* alone and name them in the report. Delete docs/architecture/out/* and docs/architecture/site/out after the scan results are recorded.
  </action>
  <verify>
    <automated>node docs/architecture/sync.mjs check && node --test docs/architecture/site/leak-scan.test.mjs && git diff --quiet HEAD -- docs/architecture/model/generated.c4</automated>
  </verify>
  <done>drill-output.txt regenerated with only the listed differences; the CI-built site scanned clean (exit 0) with the new labels present and the old ones absent; the self-test 20/20; one local commit made through the hook; nothing pushed; out/ and site/out cleaned.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| model text to public site | titles, descriptions and edge labels in .c4 files are published at the architecture hostname |
| repo to production | a push to main rebuilds and deploys the site through Flux |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-261029-sra-01 | Information disclosure | development view title, description, edge labels | medium | mitigate | text names packages, layers and LayeringArchTest only; Task 3 builds with CI's command and runs leak-scan.mjs on the output (exit 0) |
| T-261029-sra-02 | Tampering | drills D29, D29b and self-tests 18, 19 | medium | mitigate | anchors retargeted in Task 1; Task 3 diffs the drill output so a drill that stops failing shows as a difference, and the self-test must print 20/20 |
| T-261029-sra-03 | Elevation of privilege | production site deploy | medium | mitigate | local commit only, no push; the owner decides the push |
| T-261029-sra-SC | Tampering | npm/pip/cargo installs | low | accept | no package is installed; the existing docs/architecture install is used as is |
</threat_model>

<verification>
- `node docs/architecture/sync.mjs check` exits 0 and its cycle line is unchanged.
- `fish docs/architecture/likec4.fish validate` passes.
- Before and after JSON measurements of the development view (14/38 to 18/12) and both PNGs read.
- Order rule: exit 1 on the HEAD model, exit 1 with only the order line on the swapped mutant, exit 0 on the new model.
- Coverage rule: D9b exit 1 with the new script, exit 0 with the rule removed.
- `python3 scripts/pre-commit-likec4-selftest.py` prints 20/20 passed.
- drill.fish output differs from the recorded one only on the listed lines.
- `node docs/architecture/site/leak-scan.mjs docs/architecture/site/out` exits 0 on a clean CI-command build; `node --test docs/architecture/site/leak-scan.test.mjs` passes.
- generated.c4 and spec.c4 unchanged.
</verification>

<success_criteria>
The site's sections read 1 Physical to 6 Development in LikeC4's own order, held by a rule that fails on the old labels; the development view shows four layers and the 12 imports that cross them, with the four cycle-closing upward edges in red; every existing drill and self-test still bites; one local commit, not pushed.
</success_criteria>

<output>
Create `.planning/workstreams/architecture-views/quick/261029-sra-reorder-architecture-site-sections-and-r/261029-sra-SUMMARY.md` when done
</output>
