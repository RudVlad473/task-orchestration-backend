# Diagram Conventions

Architecture diagrams for this project should aim to snap to **Kruchten's 4+1 architectural view
model** rather than an ad hoc mix of styles, so each diagram has one clear, singular concern instead
of quietly blending several.

- **Logical View** — key domain classes/objects and their relationships. Rarely needed as a separate
  diagram here; the entity/DTO structure already documents this in code.
- **Process View** — runtime concurrency and communication between processes (e.g. the app JVM's
  request thread, its `kafka-publish` executor and its Kafka consumer thread, the Redpanda broker,
  Traefik).
- **Development View** — module/subsystem organization from a builder's perspective. Roughly what a
  C4 container diagram captures.
- **Physical View (Deployment)** — mapping of software to physical/hardware nodes. **Always annotate
  each node with its platform/CPU architecture** (e.g. "GitHub Actions runner: x86_64" vs "Oracle A1
  Flex VM: ARM64/Ampere"), not just its name and IP. This is the view that would have caught the
  amd64-image-on-an-arm64-VM mismatch found during Phase 5 (Infra Migration) planning on 2026-08-04,
  before it became a runtime failure.
- **Scenarios (+1)** — one or two key end-to-end scenarios (e.g. "push to main → deploy", "signup →
  board creation") traced across the other views, to confirm they stay consistent with each other.

Not every diagramming task needs all five views. Pick whichever view(s) are relevant to what's being
communicated — the point is to be deliberate about which view a given diagram *is*, and to not
silently collapse a logical/development concern (what talks to what) with a physical/deployment one
(what runs where, on what hardware) in the same picture.

## What earns a diagram

The test: **does the diagram's truth survive a refactor?** These categories pass:

- runtime topology;
- process, thread and messaging boundaries;
- deployment (nodes, hardware, network layers);
- protocol-level scenarios (HTTP, session, Kafka, status-code semantics);
- module-level dependency direction.

A diagram fails if its lifelines or nodes are **classes calling classes**: a controller → service →
repository chain, a service-to-service call graph, a test base-class hierarchy. The reason is
upkeep, not taste. Class graphs churn on every refactor, and no check can catch the drift: a diagram
whose boxes are class names stays green while the code under it moves. Name lifelines and nodes by
runtime role instead (client, filter chain, a thread pool, a table, a topic, a pod, a node), put the
class names in the embedding doc's prose, and let the messages carry protocol facts: method and
path, status and `ProblemDetail` `code`, cookie or header, SQL-level action, topic, consumer group.
A fact a dropped class-level diagram carried moves into the prose that embedded it.

## LikeC4 standard

Approved 2026-10-27. The target is one LikeC4 model with one viewpoint per Kruchten view, in place of
a set of separate Mermaid files. That model lives in `docs/architecture/model/`, driven by
`docs/architecture/likec4.fish` (`start|validate|png|site|check|sync`). Its pinned tooling installs once
per checkout with `pnpm --dir docs/architecture install`. No diagram embedded in the docs uses it yet.

The survives-a-refactor test in "What earns a diagram" stays the inclusion test, so nothing
class-level is drawn in LikeC4 either.

Scope of the sections that follow. The sections from "Layout and naming" through "Inventory check"
govern the seven current Mermaid diagrams. They stay in force until those diagrams migrate, and this
change does not migrate any. This section governs new LikeC4 work and the model itself.

The approved design and the outcomes of its verification are in
`.planning/quick/261027-f8q-write-the-approved-likec4-diagramming-st/`, as `261027-f8q-BRIEF.md` and
the `verify/` folder.

### Entities and rules

- **Element.** A runtime thing with identity. The kinds are a closed set: `actor`, `system`,
  `container`, `component`, `thread`, `datastore`, `table`, `topic`, `job`.
- **Relationship.** A runtime interaction between two Elements, of kind `sync` or `async`, labelled
  with a protocol-level fact.
- **Deployment node and Instance.** The nodes are `server`, `cluster` and `namespace`. An Instance is
  an Element placed on one node through `instanceOf`. Prod and nonprod are two Instances of one
  Element, not two copies. Instances inherit all logical relationships, so the physical view re-draws
  no edges.
- **Viewpoint.** One of logical, process, development, physical, scenarios. Each View belongs to
  exactly one Viewpoint. A View projects Elements and never owns one.
- **Provenance.** Exactly one per fact, on a ladder of three rungs:
  - `generated`: written from code, never edited;
  - `checked`: hand-written, and carries `metadata` that a script compares with the code;
  - `authored`: hand-written and unverified.

  Prefer moving a fact up the ladder. A relationship that is authored is said to be authored.
- **Kinds versus tags.** Kinds say what a thing is. Tags carry only cross-cutting facts: `generated`,
  `planned`, and one tag per Viewpoint.
- **Numbers in labels.** A retry count, a schedule or a ceiling is either covered by a check or left
  out of the label.
- **CPU architecture.** Every physical node carries its CPU architecture in its title, because the
  `technology` field does not render on deployment nodes (measured in spike 006). This is the LikeC4
  form of the Physical View rule at the top of this document.
- **The `index` view.** An `index` context view is the landing page. It sits outside the 4+1.
- **Identifiers.** Hyphenated ids are allowed, under the rule `[a-zA-Z_][a-zA-Z0-9_-]*` (V3 below).

### Viewpoints

| Kruchten view | LikeC4 construct | Represents | Does not represent | Provenance |
|---|---|---|---|---|
| Logical | plain `view` over `table` elements | tables and the FK ownership chain | columns, entity classes, DTOs | generated (see V4 below); framework-owned tables authored |
| Process | `view ... of kanban` | request threads, named executors, topics, consumer groups, DB writes | method calls, class names | topics checked; executors and groups authored |
| Development | `view ... of kanban`, packages as `component` | top-level packages and their dependency direction | classes, third-party libraries | edges generated from imports (see V5 below); no cycle rule |
| Physical | `deployment` model and `deployment view` | servers (arch in title), cluster, namespaces, workloads | pods, replicas, secrets, the monitoring stack (excluded, as in `production-host.mmd`) | workloads checked against `k8s/base` and `k8s/data`; nodes authored |
| Scenarios | `dynamic view`, one per outcome | client-visible facts: method and path, status and `code`, cookie, SQL action, topic | internal calls | authored |

Three notes on the table.

**(a) The Logical row.** It bends the "rarely needed" bullet at the top of this document. It earns a
place because every fact in it is generated, so it costs no upkeep. The exception is the tables of
Spring Session. `spring.session.jdbc.initialize-schema=always` creates them at startup, outside
Flyway (`src/main/resources/application.properties` line 187), so they never appear in generated
output. They are authored and labelled as such.

**(b) Scenarios.** The old `signin` diagram drew its `alt` branches in one picture. The standard is
one dynamic view per outcome (`signin-ok`, `signin-bad-credentials`, `signin-ceiling`), because one
trace is one Kruchten scenario. The cost is several views instead of one. Only the first two were
rendered in verification. Branches inside one view were not tested and are not adopted.

**(c) Scenario layout.** The approved design adopts two layouts and this standard does not choose
between them:

- `variant sequence` draws a UML sequence diagram only when the PNG export is given `--seq`. Without
  it, the export draws the ordinary layout, a 4534 x 506 strip for `signin-ok`.
- `autoLayout TopBottom` takes `signin` from 4970 x 524 to 1384 x 2504.

The measurements against the legibility bounds are in the verification outcomes below.

### Checks

The drift script is `docs/architecture/sync.mjs`, built in spike 007.

- **Proven in spike 007:** validate; the generated file is up to date; the topic set matches
  `KafkaTopics.java`; the workload set matches `k8s/base` and `k8s/data`; every `metadata.source`
  path exists.
- **Drills:** 4 of 4 drift kinds were caught. A renamed controller class is not caught, by design.
- **Wired:** the pre-commit hook runs `node docs/architecture/sync.mjs check` when a staged path is
  under `src/main/resources/db/migration`, `k8s/base`, `k8s/data` or `docs/architecture`, or is
  `KafkaTopics.java`, and refuses the commit when Node or the install is missing. The
  `likec4-model-drift` job in `.github/workflows/invariant-checks.yml` runs the hook's self-test and the
  check. A changed metadata source outside those paths is caught in CI only.
- **Key vocabulary:** the DSL cannot declare allowed metadata keys (V2 below), so the drift script
  owns the key vocabulary.
- **Proposed by this standard, not built:**
  - an unknown-metadata-key check;
  - an "every view has a viewpoint tag" check;
  - an "every `server` node title names an architecture" check.

### Verification outcomes (2026-10-27)

Each outcome opens with its verdict. Evidence paths are relative to the `verify/` folder named
above. Every pixel size is recorded as text in `verify/png-dimensions.txt`, because the repo ignores
`out/` folders, so a render kept in one is not in git.

| # | Question | Outcome | Evidence |
|---|---|---|---|
| V1 | Does the `sequence` dynamic-view variant work, and can branches be separate views? | ADOPTED with a condition. `variant sequence` parses, but the PNG export draws the ordinary diagram layout unless `--seq` is passed (a 4534 x 506 strip without it). With `--seq` it draws a real UML sequence diagram, and `signin-ok` and `signin-bad-credentials` both render. An `alt`/`else` inside one view was not tested and is not adopted. | `verify/lc4/exp.c4`, `verify/out-seq/signin-ok.png`, `verify/out-seq/signin-bad-credentials.png`, `verify/png-dimensions.txt`, `likec4 export png --help` |
| V2 | Can the DSL declare allowed metadata keys so a typo fails? | NOT ADOPTED. A `metadata` block inside `specification` is a parse error (`Expecting token of type '}' but found metadata`). A typo key (`sorce`) validates silently. Metadata keys exist only in the TypeScript builder API. Consequence: the drift script must own the key vocabulary. | `verify/lc4-meta/meta.c4`, two validate runs |
| V3 | Are hyphenated ids allowed? | ADOPTED. Element `sub-el` and scoped view `process-activity` validate and render. The LikeC4 docs give the identifier rule `[a-zA-Z_][a-zA-Z0-9_-]*`. | `verify/lc4/exp.c4`, `verify/png-dimensions.txt` |
| V4 | Can tables and FKs come from `information_schema` of a Flyway-migrated scratch Postgres? | ADOPTED. Replaying V1 to V9 into a throwaway `postgres:16` and reading back through `information_schema` gives 6 tables and 4 FKs (boards to users, columns to boards, tasks to columns, subtasks to tasks). The container was removed afterwards. Today this gives the same 6 tables as the regex in spike 007, so the gain is only for FKs, or for tables added later by ALTER, and that gain is unmeasured. Gap: Spring Session JDBC creates its tables at startup (`spring.session.jdbc.initialize-schema=always`), outside Flyway, so they never appear. They are an authored, labelled exception. | `verify/fk.fish`, `verify/fk-result.txt`, `application.properties:187` |
| V5 | Can package edges come from imports, with a no-cycles rule? | PARTLY. Edge extraction is ADOPTED: 38 edges over 14 top-level packages. The no-cycles rule is NOT ADOPTED: `tsort` reports cycles in the current code (base and entity import each other; config, security and service form a loop). A gate would fail today. Whether 38 edges is readable in one view is unmeasured. Collapsing to layers is a possibility, not a result. | `verify/pkg.fish`, `verify/pkg-result.txt` |
| V6 | Does `autoLayout TopBottom` fit dynamic views to the legibility gate? | ADOPTED for scenarios, with the gate itself still undefined for LikeC4. `signin` goes from 4970 x 524 to 1384 x 2504 PNG. The export uses `deviceScaleFactor` 2, the same 2x as this repo, so natural size is the PNG size divided by 2. The measurements against the gate follow the table. The 12 px text floor cannot be checked, because LikeC4's label font size was not measured. `scripts/verify-diagrams.py` reads `.mmd` and PNG pairs and a Mermaid manifest, so it does not check `.c4` views. | `verify/png-dimensions.txt`, `verify/out-seq/signin-ok.png`, the CLI bundle's `deviceScaleFactor:2` |

V6, natural width against the Legibility test section's bounds (width 1117, displayed height 1600):

- `index`: 854 wide, passes.
- `process`: 1233 wide, fails. That is spike 006's render and the figure to use. The verify
  workspace's `process.png` is 2508 divided by 2, which is 1254, only because its experiment file adds
  a `sub-el` component to `kanban` and the view includes `kanban.*`. It is a different view, not a
  re-render.
- `production`: 2041 wide, fails.
- `signin` horizontal: 2485 wide, fails.
- `signin-ok` and `signin-bad-credentials` as sequence: 1599 wide each, fail.
- `signin` with `autoLayout TopBottom`: 692 x 1252, passes.

### Undefined, not adopted, and outside this change

**Undefined.** The legibility rule for LikeC4 is undefined.

- The 12 px text floor cannot be checked, because LikeC4's label font size was not measured.
- The 1117 bound was derived from the label size Mermaid draws, in the Legibility test section. It is
  not known to be the right bound for LikeC4.
- `scripts/verify-diagrams.py` reads `.mmd` and PNG pairs and the Mermaid manifest, so nothing checks
  a `.c4` view.

**Not adopted.**

- Declared metadata keys, because the DSL cannot declare them (V2).
- The no-cycles rule, because a gate would fail on today's code (V5).
- Branches inside one dynamic view, because they were not tested (V1).

**Unmeasured.**

- Whether 38 edges reads in one development view. Collapsing to layers is a possibility, not a result.
- The gain of `information_schema` over spike 007's regex, which today gives the same 6 tables. The
  gain applies only to FKs, or to tables added later by ALTER.

**Outside this change.** This change leaves out:

- migrating any of the seven Mermaid diagrams;
- the four diagrams with no LikeC4 counterpart yet: `signup`, `error-status-split`, `push-to-image`
  and `inbound-packet-path`;
- retiring `render-diagrams.sh`, `verify-diagrams.py`, `mermaid-config.json` and
  `render-manifest.tsv`, because a migration ends when its old home is gone, and that is a later
  change;
- a LikeC4 text-size legibility rule;
- the three unbuilt checks listed under Checks.

## Layout and naming

Every diagram is `docs/diagrams/<view>/<subject>.mmd` plus a rendered `<subject>.png` twin in the
same folder. `<view>` is one of `logical`, `process`, `development`, `physical`, `scenarios`
(Kruchten's names; Scenarios is the "+1"). A folder exists only while it holds a diagram. The
`<subject>` rules:

- kebab-case, matching `[a-z0-9]+(-[a-z0-9]+)*`;
- it names what is drawn, not the view (the folder carries that) and not the diagram type
  (flowchart or sequence is a rendering choice that can change while the subject stays the same);
- no old area prefix (`architecture-`, `auth-`, `infra-`).

The render manifest key is `<view>/<subject>`. Only two files sit at the top level of
`docs/diagrams/`: `render-manifest.tsv` and `mermaid-config.json`. A diagram that moves between
views is a `git mv` of both files plus a manifest key rename, as its own commit with no content
edit, so `git log --follow` keeps the history.

Current inventory:

| View | Path | Question it answers | Embedded in |
|---|---|---|---|
| Process | `process/activity-pipeline` | How does a committed mutation become an `activity_log` row, thread by thread? | `ARCHITECTURE.md` |
| Physical | `physical/production-host` | What runs where, on what hardware? | `INFRA_ARCHITECTURE.md` |
| Scenarios | `scenarios/signin` | What happens between a `POST` of credentials and a session cookie landing in Postgres? | `ARCHITECTURE.md`, `AUTH_FLOWS.md` |
| Scenarios | `scenarios/signup` | What do signup, its auto-authentication and its rollback look like to a client? | `AUTH_FLOWS.md` |
| Scenarios | `scenarios/error-status-split` | Which layer answers each of 401/403/400/409, and does the request reach application code? | `ARCHITECTURE.md` |
| Scenarios | `scenarios/push-to-image` | How does a push to `main` become a running deploy? | `INFRA_ARCHITECTURE.md` |
| Scenarios | `scenarios/inbound-packet-path` | Which network layers does one inbound packet cross on the VM? | `INFRA_ARCHITECTURE.md` |

`README.md` lists the same seven in its Diagrams table, and embeds an inline copy of the physical
view that is a simplified rendering of `physical/production-host.mmd`; the `.mmd` is canonical.

## Rendering

`scripts/render-diagrams.sh` renders every manifest entry through one digest-pinned `mermaid-cli`
container, so a PNG's pixels do not depend on which machine rendered it. `render-diagrams.sh --check
--all` renders into a scratch directory and compares each result with the committed PNG; it writes
nothing under `docs/diagrams/`.

- **`docs/diagrams/mermaid-config.json` turns `useMaxWidth` off** for flowcharts and sequence
  diagrams. Measured 2026-10-05: `mmdc` lays out into an 800 px viewport and, with the default
  `useMaxWidth: true`, shrinks every SVG to fit inside it (784 px) before rasterising. The `-s` flag
  multiplies pixels, not layout width, so 784 × 1, × 2 and × 4 were exactly the old committed widths
  of 784, 1568 and 3136, and a diagram that was 3600 px wide at its natural size came out with 4 px
  text. With the config the layout keeps its natural width.
- **The scale is uniformly 2.** Every manifest row says `2`, so a PNG is a 2× raster and its natural
  width is the PNG width divided by 2. A label is then 32 device px in the raster, which stays sharp
  on a devicePixelRatio-2 screen and on click-through.
- **`--check` compares the PNG's width exactly and its height within 2%.** Because width now tracks
  content, a label edit that changes the bounding box fails the check. A label edit that leaves the
  bounding box unchanged still passes: the check reads only the PNG header.

## Legibility test

The test is about text size as the reader sees it on GitHub. GitHub's `article.markdown-body` column
was measured on 2026-10-05 with headless Chromium, logged out:

| Viewport | Blob view of `docs/*.md` | Repo-home README |
|---|---|---|
| 1024 | 605 | 582 |
| 1280 | 861 | 838 |
| 1366 | 947 | 838 |
| 1440 / 1920 | 1012 | 838 |

`D = 838` px is the narrowest column at a viewport of 1280 px or wider. Narrower viewports are out
of target. GitHub keeps the `width` attribute on `<img>` and adds `max-width: 100%`.

Mermaid 11.17 draws every label at 16 px. For a diagram of natural width `W`, with `W` the PNG
width divided by 2:

- displayed text px = `16 × min(1, 838 / W)`;
- displayed height = `H × min(1, 838 / W)`, with `H` the PNG height divided by 2.

Two bounds follow, and **both are chosen, not standards**:

- **Text floor 12 CSS px**, three quarters of the 16 px body text. Hence `W_MAX = floor(16 × 838 /
  12) = 1117` px natural width.
- **Displayed height at most 1600 CSS px**, about two 800 px screens.

A diagram that fails gets these levers, in this order, and **a threshold is never raised to pass**:

1. Shorten labels and notes, moving rationale into the embedding doc's prose (notes at most two
   short lines).
2. Re-lay out: fewer lifelines by merging same-thread or same-node roles, rows of subgraphs aligned
   with `~~~`, an inner subgraph `direction LR` while the top level stays `TB`.
3. Split into two subjects in the same view.

**Embed format.** A diagram PNG is embedded as `<img src="<relative path>.png" width="<W>"
alt="<Kind>: <what it shows>">`, the standard idiom for a 2× raster at an explicit CSS width, with
the `<sub>[diagram source](…)</sub>` line after it. Markdown image syntax (`![…](….png)`) is not
allowed for a diagram PNG: it would display the 2× raster at up to twice its intended size. A plain
`[PNG](…)` link is fine.

## Layout rules for flowcharts

Applies to `flowchart` diagrams only — this repository's `sequenceDiagram`s follow Mermaid's own
top-to-bottom message ordering and are out of scope here. Four rules, each with the test that
decides whether a diagram satisfies it, so a reader can check without asking anyone.

**1. `flowchart TB`, never `TD`.** The two keywords are documented aliases producing identical
output, so this is purely about greppability: one spelling makes `rg -n 'flowchart TB'
docs/diagrams/` a complete inventory, and a mixed vocabulary makes it silently partial.
*Test:* `rg -n 'flowchart TD' docs/diagrams/` returns nothing.

**2. One vertical spine, outcomes as leaves.** A flowchart has exactly one primary top-to-bottom
chain of nodes carrying the main path, with terminal outcomes hanging off it as leaves rather than
threaded back into the chain.
*Test:* delete every leaf. What remains must still read as one unbroken sequence — if removing a
node breaks the chain, that node is spine, not leaf, and belongs in the trunk.

**3. `~~~` invisible links for rank alignment only.** When two nodes must render at the same
vertical rank but carry no semantic relationship, join them with `~~~` rather than reordering
declarations or inserting a styled-invisible dummy edge. `physical/production-host.mmd`'s
`netcup_spacer ~~~ traefik_box` is the existing precedent.
*Test:* removing the `~~~` must change only geometry, never meaning. If removing it also removes
information, it was carrying semantics and should have been a real edge instead.

**4. A shared `%%{init}%%` block.** Every flowchart opens with the same init block — currently
`subGraphTitleMargin` top/bottom 15 and `curve: linear` — so subgraph titles don't collide with
their first child and edges render straight rather than bezier. The line is byte-for-byte line 1 of
`physical/production-host.mmd`.
*Test:* two flowcharts in this repo placed side by side must not differ in edge curvature or
subgraph title spacing. If they do, one is missing the block or has diverged from it.

Rules 1 and 4 are checked mechanically by the inventory guard below (rule `flowchart-rules`, for
standalone diagrams and for inline `mermaid` fences alike). Rules 2 and 3 are judgment tests.

## Inline diagrams

A diagram may be written inline as a ```` ```mermaid ```` fence in a `docs/learning/` chapter, where
it explains something the chapter is teaching. Inline diagrams:

- pass the same keep test as standalone ones, and follow the flowchart rules above;
- are never a copy of a standalone diagram: if a chapter needs one, it embeds the PNG or links its
  `.mmd`. A hand-maintained duplicate drifts, and the README's old physical-view copy already did;
- are checked by the guard for the flowchart and class-label rules (not for width, because GitHub
  renders them itself).

## Inventory check

`scripts/verify-diagrams.py` is the mechanical check that this document's rules hold. It is
stdlib-only, enumerates files with `git ls-files` (so an untracked draft is invisible until
`git add`, and it never walks the tree and opens `.env` files), and scans tracked Markdown outside
the GSD, vendored-skill, `docs/wiki/` and `docs/raw/` trees.

```bash
python3 scripts/verify-diagrams.py check                          # exit 0 clean, 1 violations
python3 scripts/verify-diagrams.py check --diagram scenarios/signin   # one diagram and its references
python3 scripts/verify-diagrams.py report                         # legibility table: W, H, text px
python3 scripts/verify-diagrams-selftest.py                       # proves each rule fires on a plant
```

Exit code 2 means an empty scan or bad usage; an empty scan and a clean scan would otherwise look
the same. The rule ids are:

| Rule | Fails when |
|---|---|
| `outside-view-folder` | a diagram file is not directly inside a view folder |
| `missing-twin` | a `.mmd` has no `.png`, or the reverse |
| `manifest-mismatch` | a diagram has no manifest row, or a row has no source |
| `uniform-scale` | a manifest scale is not 2 |
| `bad-name` | a subject is not kebab-case, or carries an old area prefix or a type suffix |
| `bad-png` | a PNG has a bad signature or header |
| `dangling-reference` | a doc names a `diagrams/` path that does not resolve |
| `legibility-width` | natural width exceeds 1117 px |
| `legibility-height` | displayed height exceeds 1600 px |
| `embed-width` | a doc embeds a PNG without `<img width>`, or the width differs from the natural width |
| `flowchart-rules` | a flowchart lacks the shared init line or uses a keyword other than `flowchart TB` |
| `class-level-label` | a label names a class (a CamelCase name ending in a role suffix such as `Service`) |

The guard is **local-only**, like `render-diagrams.sh`: `.github/workflows/invariant-checks.yml`
ignores `docs/**`, and removing that ignore would pay a full deploy pipeline per documentation
commit. It trusts that a PNG came from the renderer, so `bash scripts/render-diagrams.sh --check
--all` is the other half of the proof; run both after touching a diagram.

**Why this convention exists:** raised during Phase 5 planning after discovering the CI pipeline's
Docker image build was x86_64-only while the deploy target (Oracle A1 Flex) is ARM64. A plain
C4-style container diagram of "what talks to what" would not have surfaced this — a Physical/
Deployment view with node-architecture annotations would have, because the mismatch lives entirely
in "what hardware is this node" rather than in the logical topology.
