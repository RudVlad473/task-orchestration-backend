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
  `container`, `component`, `thread`, `datastore`, `table`, `topic`, `job`, `external`, `note`.
  `job` covers anything that runs to completion, a CronJob and an init container alike, so its
  notation reads "Run-to-completion job". `external` is a thing outside this repository or outside
  the cluster (a registry, a certificate authority, a provider firewall, an off-cluster bucket).
  `note` is documentation attached to an
  element, not a runtime thing: it carries a `source` and renders as a document.
- **Relationship.** A runtime interaction between two Elements, of kind `sync` or `async`, labelled
  with a protocol-level fact. Two structural kinds exist besides, and only `sync.mjs generate`
  writes them: `fk`, a foreign key from a child table to its parent, and `imports`, one top-level
  package importing another. The one authored `fk` edge, between the two Spring Session tables, is
  the exception.
- **Deployment node and Instance.** The nodes are `server`, `cluster`, `namespace` and `provider`
  (a SaaS or a network outside the server: GitHub, Docker Hub, Let's Encrypt, the provider
  firewall, the internet). An Instance is
  an Element placed on one node through `instanceOf`. Prod and nonprod are two Instances of one
  Element, not two copies. Instances inherit all logical relationships, so the physical view re-draws
  no edges.
- **Viewpoint.** One of logical, process, development, physical, scenarios. Each View belongs to
  exactly one Viewpoint. A View projects Elements and never owns one. The five tags are `#logical`,
  `#process`, `#development`, `#physical` and `#scenarios`, declared in `spec.c4`. The `index` view
  carries none. `sync.mjs check` refuses a view with no tag or with more than one, and refuses a
  viewpoint that no view carries.
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
| Logical | plain `view` over `table` elements | tables and the FK ownership chain | columns, entity classes, DTOs | tables and `fk` edges generated from the migrations by a statement-level regex (see V4 and note (a)); the two Spring Session tables authored; two `note` elements (on `activity_log` and on the Avro subjects) carry a `source` whose existence is checked, and their text is authored |
| Process | `view ... of kanban` | request threads, named executors, topics, consumer groups, DB writes; the Schema Registry, the schema-registering init job, the management server and the nightly backup flow to the off-cluster bucket | method calls, class names | topics checked; the backup job's CronJob checked as a workload; executors, groups, the registry, the init job and the bucket authored |
| Development | `view ... of kanban`, packages as `component` | top-level packages and their dependency direction | classes, third-party libraries | components and `imports` edges generated from import lines (see V5 and note (d)); cycles printed by the check, not gated |
| Physical | `deployment` model and `deployment view`: three views, `production`, `delivery` and `monitoring` | `production`: the server and the provider nodes (arch in the server titles), the cluster, namespaces, workloads, the cluster-scoped ClusterIssuers, and the inbound path with the provider firewall and the host filter as external elements. `delivery`: GitHub and its runner (arch in the title), Docker Hub, Flux and the image path into the cluster. `monitoring`: the monitoring workloads, the scrape targets the repo's monitor settings name, the default scrape targets that the kube-prometheus-stack chart at 91.5.2 leaves on (the kubelet with cAdvisor, the API server, CoreDNS, kube-state-metrics, the operator and Grafana), and the query edges; the self-scrape of Prometheus is stated, not drawn | pods, replicas, secrets, objects a chart renders (the chart workloads drawn under a HelmRelease claim are drawn, but the check does not render the chart) | workloads checked (`Deployment`, `StatefulSet`, `CronJob`, `HelmRelease` and `ClusterIssuer` under `k8s/base`, `k8s/data`, `k8s/platform` and `k8s/monitoring`, plus the Flux controller Deployments in `k8s/flux-system/gotk-components.yaml`); routed hosts checked against every `IngressRoute`; namespace nodes and instance placement checked against the manifests under `k8s`; the firewall, host filter, ServiceLB, the kubelet, the API server, CoreDNS, provider nodes, the runner and the scrape edges authored |
| Scenarios | `dynamic view`, one per outcome, at most 5 participants | client-visible facts: method and path, status and `code`, cookie, SQL action, topic; also the internal steps of `activity-log-row` (the AFTER_COMMIT hand-off, the publish executor, the topic, the consumer group, the idempotent insert) and of the delivery scenarios `push-to-image` and `image-to-rollout` (workflow jobs, Flux controllers, the init container); class-level calls and branches inside one view are still not drawn | class-level calls, branches inside one view | authored: every step of the ten Phase 2 scenario views is cited in `.planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md`, or marked authored with its reason; the three `signin-*` views were built before that file and have no section in it (the lines they were drawn from are listed, not per step, in `.planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/261027-if9-T4-SUMMARY.md` at 8b7e379); at most 5 participants checked; `error-401` draws no SQL step; measured before the fix, and after it a test checks session rows and cookies while the absence of SQL is inferred from the code (note (b2)) |

Three notes on the table.

**(a) The Logical row.** It bends the "rarely needed" bullet at the top of this document. It earns a
place because every fact in it is generated, so it costs no upkeep. The exception is the tables of
Spring Session. `spring.session.jdbc.initialize-schema=always` creates them at startup, outside
Flyway (`src/main/resources/application.properties` line 187), so they never appear in generated
output. They are authored and labelled as such, and the one `fk` edge between them is authored too
(`session_primary_id`, read from the `schema-postgresql.sql` inside spring-session-jdbc 3.5.7, in
`261027-if9-T2-session-schema.txt`).

The `fk` edges are generated by a regex over the migrations, which `sync.mjs` reads statement by
statement. It follows `CREATE TABLE`, `ALTER TABLE ... ADD ... REFERENCES`, `DROP CONSTRAINT` and
`DROP COLUMN`. It does not follow:

- an FK made inside a DO block or a function;
- a table or column rename;
- `DROP TABLE`, which is expected to fail through the model's own validation, not observed.

On today's migrations the regex gives the same 4 FKs as `information_schema` (boards to users,
columns to boards, tasks to columns, subtasks to tasks; the side-by-side list is in
`261027-if9-T2-green.txt`, against `verify/fk-result.txt`). The `information_schema` route of V4
stays documented, and is not wired into the gate: it needs Docker and a scratch Postgres inside a
check that today runs in about a second (median 958 ms, `261027-if9-T3-green.txt`).

**(b) Scenarios.** The old `signin` diagram drew its `alt` branches in one picture. The standard is
one dynamic view per outcome (`signin-ok`, `signin-bad-credentials`, `signin-ceiling`), because one
trace is one Kruchten scenario, and all three are built. The cost is several views instead of one.
`signin-bad-credentials` traces the unknown email. A wrong password has no view of its own: it
returns the identical 401 body right after the `SELECT` by id that `signin-ceiling` draws, so its
SQL actions are a prefix of that view and nothing would be undrawn. Branches inside one view were
not tested and are not adopted.

**(b2) The 401 view.** `error-401` draws no SQL step. On 2026-10-28 one live run (verdict b, recorded
in `02-04-401-measure.txt` in the Phase 2 folder) found that a request with no session cookie got a
clean 401 when its Accept header named `application/json`, but a bare wildcard or a browser
navigation header had the refused request saved in a new session: one `spring_session` row and a
`JSESSIONID` cookie per request. Commit `17a94cb` set a request cache that saves nothing, and
`AuthorizationGatingTest` (GET and POST `/boards`, four Accept variants) failed 6 of 8 cases before
it and passes after. The limits: no live re-measurement followed the fix, the test runs in process
and checks session rows and cookie names, not statements, and that no SQL is sent after the fix is
inferred from the code (the removed request cache was the only statement source measured), not
observed.

**(c) Scenario layout.** Sequence is adopted. Every scenario view carries `variant sequence`,
and `likec4.fish png` passes `--seq`, without which the export draws the ordinary layout (V1). The
natural widths, from `261027-if9-T4-png.txt`, are 1838 px for `signin-ok`, 1500 for
`signin-bad-credentials` and 1838 for `signin-ceiling` (the Phase 2 scenario widths are in the
table at the end of this section). The Mermaid width bound of 1117 px is not
applied to them, because the LikeC4 legibility rule is still undefined (see below). Those widths
are recorded, not judged.

**(d) Development.** The 14 components and 38 `imports` edges are generated from the
`import com.vrudenko.kanban_board.<package>` lines under `src/main/java`, and equal a fresh run of
`verify/pkg.fish` (38 = 38, `261027-if9-T3-green.txt`). An import of a root-level class gives no
edge, but no source file imports one today, so that exclusion is unexercised by the cross-check and
by every drill. A fully qualified class name used without an import line is not followed. `sync.mjs
check` prints the strongly connected components on every run, labelled "reported, not gated", and
they never change its exit code. Today it lists one group of 8 packages: base, config, dto, entity,
mapper, repository, security and service.

### Checks

The drift script is `docs/architecture/sync.mjs`, built in spike 007.

- **Proven in spike 007:** validate; the generated file is up to date; the topic set matches
  `KafkaTopics.java`; the workload set matches `k8s/base` and `k8s/data`; every `metadata.source`
  path exists. The workload reader was widened to `k8s/platform` in the bullet below.
- **Built in 261027-if9:**
  - the viewpoint-tag rule: every view except `index` carries exactly one of the five tags;
  - the coverage rule: every viewpoint has at least one view;
  - the generated file now also holds the FK edges, the package components and the import edges, so
    a migration that adds or drops an FK, or a Java import that adds or removes a cross-package
    edge, makes it stale;
  - the cycle report: `sync.mjs check` prints the package cycles and never gates on them.
- **Built in the architecture-views workstream, Phase 1:**
  - the namespace rule: every namespace a manifest under `k8s` defines, from a `Namespace` object or
    any string `namespace` or `targetNamespace` value, is a namespace node, and every namespace node
    is defined by a manifest;
  - the placement rule: each element with instances sits in exactly the namespaces its manifests
    land in, following a kustomization's namespace through its `resources`. A Flux `HelmRelease`
    places in its `spec.targetNamespace` when set, else its `metadata.namespace`, and Flux source
    objects such as a `HelmRepository` place nothing, proven on the real cert-manager manifest by
    D16 and D16b. Three limits are accepted rather than checked:
    - Flux Kustomization spec.targetNamespace is not modelled: the rule follows only a kustomize
      kustomization's namespace through its resources, so a directory that a Flux Kustomization
      applies with spec.targetNamespace is placed by its own kustomization and `metadata.namespace`
      values. Measured on 2026-10-27, 9 Flux Kustomizations sit under `k8s/flux-system`
      (`rg -l "^apiVersion: kustomize.toolkit.fluxcd.io/" k8s/flux-system`) and none sets a
      string-valued one (`rg -n "^\s+targetNamespace:\s*\S" k8s/flux-system` prints nothing). Before
      an element is anchored on a directory applied by one that sets it, `sync.mjs` models the field
      or the element is recorded in this section as not placement-checked.
    - A HelmRelease is placed by its declared targetNamespace and the check never renders the chart,
      so it checks the declared targetNamespace, not the rendered chart
      (an object a chart creates outside its targetNamespace is invisible to the check).
    - An element with no `workload` metadata and no `source` under `k8s` has no anchor document and is
      skipped, so a moved instance of it passes. Every element added to the Physical view, starting
      with the architecture-views Phase 2 elements, must carry a workload or k8s source anchor,
      or be recorded here as not placement-checked. The Phase 1 model had no such element; the
      Phase 2 ones are listed in the Phase 2 bullet below;
  - the metadata-key rule: `METADATA_KEYS` in `sync.mjs` (`source`, `workload` and, since Phase 2,
    `host`) is checked on elements, deployment nodes, instances and relationships;
  - the view-membership rule: every deployment instance is drawn in at least one deployment view
    (namespace nodes with no instances are not required to be drawn);
  - workloads are read with the `yaml` parser, not a regex;
  - fail-closed fact sources: a missing tree among `k8s/base`, `k8s/data`, `k8s/platform`,
    `k8s/overlays`, `k8s/monitoring` and `k8s/flux-system`, or a manifest that does not parse, exits
    4, which the hook reports as not run to completion and never as drift.
- **Built in the architecture-views workstream, Phase 2:**
  - the label rule: no deployment view has an edge whose label is the merged `[...]`. A deployment
    edge merges every logical relationship between its instance pair, so every relationship on one
    pair carries one shared protocol-level title, and the views that want a specific label
    override it. A merged edge that mixes `sync` and `async` relationships under one title has no
    kind and is drawn with LikeC4's default dashed line (measured while planning Phase 2);
  - the participant rule: a dynamic view has at most 5 participants;
  - the workload-instance rule (Phase 1 review WR-03): an element that claims a workload must have
    at least one instance, so deleting its `instanceOf` is caught;
  - the readers: `HelmRelease` and `ClusterIssuer` objects are read as workloads, `k8s/monitoring` is
    a workload tree, and the six Flux controller Deployments are read from
    `k8s/flux-system/gotk-components.yaml` alone, because `controller-resources.yaml` repeats their
    names as a patch. A rename made only in that patch file is therefore not caught;
  - the hosts rule: every `Host(...)` literal in the `match` of an `IngressRoute` is claimed in the
    `host` metadata of an element, and every claimed host is routed. Which instance serves which
    host stays authored;
  - `METADATA_KEYS` in `sync.mjs` is the one place the key vocabulary lives: `source`, `workload`
    and `host`.

  Not placement-checked, so a moved instance of any of them passes: `svclb`, `coredns`, `kubelet` and
  `kube-apiserver` (no manifest declares them), the host filter (its `source` is a script outside `k8s`), and the instances on provider nodes
  or on the server: `repo`, `ci`, `dockerhub`, `acme`, `firewall` and `client`. Five monitoring elements (Prometheus, Grafana,
  node-exporter, kube-state-metrics and the operator) share one `HelmRelease/kube-prometheus-stack`
  claim, so the check proves that release exists and where it lands, not what the chart renders.
  The scrape edges in `monitoring` are authored from the repo's PodMonitor and ServiceMonitor
  settings and from the default monitors of the kube-prometheus-stack chart at 91.5.2 that the
  values file leaves on: the kubelet with cAdvisor, the API server, CoreDNS, kube-state-metrics, the
  operator and Grafana. Each was read from the chart's `values.yaml` at that tag and is recorded,
  with its line number, its quoted text, the byte count and the sha256 of the file read, in
  `.planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt`.
  A title names a port or protocol only where a values line states it. No check reads the chart
  defaults, so a chart bump can leave the drawn set stale without failing. The values file turns off
  the controller-manager, scheduler, etcd and kube-proxy monitors (lines 45 to 52) and Alertmanager
  (lines 37 to 38), and none of them is drawn. Prometheus also scrapes itself, which is not drawn
  because LikeC4 rejects a relationship from an element to itself. Of the other charts, the Loki
  ServiceMonitor is off (`loki.yaml` lines 102 to 103) and the Alloy chart defaults are not read; the
  live target list is not read either. Scenario
  steps are authored prose that LikeC4 does not validate against the model; only the participant cap
  is checked, and each step is tied to a line, or marked authored, in the evidence file.
- **Drills:** `docs/architecture/drill.fish` runs on a scratch copy (`--model DIR`), never on the
  real model; the `SYNC` and `MODEL` environment variables point it at another script and another
  model folder, which is how a drill is recorded against the code that preceded its rule. Its
  sections are the baseline, the drills that introduce a break and are caught (exit 1 for drift,
  exit 4 for a fact source in D14, D14b and D14c), and the controls D5, D13b, D16b and D17b, which
  stay clean. D6 to D9 were each seen not caught before their rule existed
  (`261027-if9-T1-red.txt` to `261027-if9-T4-red.txt`). Each drill from D10 on was recorded not
  caught by the script and model that preceded it: Phase 1's in
  `.planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-drills-before.txt`
  and Phase 2's in
  `.planning/workstreams/architecture-views/phases/02-model-completion/02-drills-before.txt`
  (D17 to D24, controls included, show `exit=0` there). D5, a renamed controller class, is the negative control: it
  is not caught, by design. The result is `docs/architecture/drill-output.txt`. The hook's
  self-test `scripts/pre-commit-likec4-selftest.py` runs the hook's refusal and acceptance cases and
  passes them all.
- **Wired:** the pre-commit hook runs `node docs/architecture/sync.mjs check` when a staged path is
  under `src/main/resources/db/migration`, `k8s` or `docs/architecture`, or is `KafkaTopics.java`,
  and refuses the commit when Node or the install is missing. The
  `likec4-model-drift` job in `.github/workflows/invariant-checks.yml` runs the hook's self-test and the
  check. A changed metadata source under `src/main/java` is caught in CI only. So is a Java change that
  adds or removes a cross-package import: `src/main/java` is not a trigger path, because a trigger
  would make Node and a per-worktree install a prerequisite of every Java commit. Such a commit
  passes the hook and fails CI.
- **Key vocabulary:** the DSL cannot declare allowed metadata keys (V2 below), so the drift script
  owns the key vocabulary: `METADATA_KEYS` in `sync.mjs`, which holds three keys, `source`,
  `workload` and `host`.
- **Proposed by this standard, not built:**
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

### Built in 261027-if9 (2026-10-27)

The V rows above stay as the dated records they are. This table is the state after quick task
261027-if9. Sizes are the PNG size divided by 2 (V6), from the final render in
`261027-if9-T5-png.txt`, and are recorded without a verdict against the Mermaid bounds. Evidence
files are in `.planning/quick/261027-if9-build-the-kruchten-views-the-likec4-stan/`.

| View id | Tag | Provenance | Natural size |
|---|---|---|---|
| `index` | none | authored | 836 x 1204 |
| `data` | `#logical` | 6 tables and 4 `fk` edges generated; 2 Spring Session tables and their edge authored | 1014 x 1302 |
| `process` | `#process` | topics checked; executors and groups authored; claims verified or softened in `261027-if9-T5-claims.txt`, with no retry count, schedule or ceiling number left in a label | 1236 x 1536 |
| `development` | `#development` | 14 components and 38 `imports` edges generated | 4475 x 2636 |
| `production` | `#physical` | workloads checked; nodes authored | 1965 x 1241 |
| `signin-ok` | `#scenarios` | authored | 1838 x 964 |
| `signin-bad-credentials` | `#scenarios` | authored | 1500 x 574 |
| `signin-ceiling` | `#scenarios` | authored | 1838 x 880 |

Readability of `development` (`261027-if9-T3-png.txt`). The default layout meets the rule the plan
set: all 14 package labels legible and five traced edges unambiguous. That was judged from images
downscaled to about 2000 px wide, never at natural size. It does not show the 8-package cycle as a
group, and the three arcs at the left of `entity` and the eight arrows into `constant` are told apart
only by their arrowheads, since edges carry no label. Two other layouts were tried and rejected:
`autoLayout LeftRight 120 110` (7792 x 8938, worse: long looping arcs) and `autoLayout TopBottom
200 160` (12500 x 6552, the same topology as the default, only farther apart). Grouping the
packages into layers was not tried.

### Built in the architecture-views workstream, Phase 2

Every view added or changed in Phase 2. Sizes are the PNG size divided by 2 (V6), from a fresh
`likec4.fish png` export on 2026-10-28, and are recorded without a verdict against any bound: the
LikeC4 legibility rule is still undefined. `production`, `process` and `data` are changed views, so
their sizes replace the Phase 1 ones above. The other views in the Phase 1 table are unchanged.

| View id | Tag | Provenance | Natural size |
|---|---|---|---|
| `data` | `#logical` | as before, plus two `note` elements whose `source` is checked and whose text is authored | 1148 x 1392 |
| `process` | `#process` | topics checked; the backup CronJob checked as a workload; executors, groups, the registry, the init job and the bucket authored | 3280 x 1536 |
| `production` | `#physical` | workloads, ClusterIssuers, routed hosts, namespaces and placement checked; nodes, provider firewall, host filter and ServiceLB authored | 3427 x 2566 |
| `delivery` | `#physical` | Flux controller Deployments checked as workloads; repository, CI, Docker Hub and the runner authored | 1862 x 1556 |
| `monitoring` | `#physical` | HelmReleases checked as workloads; scrape and query edges authored from the repo's monitor settings and the chart values at 91.5.2; the Prometheus self-scrape is stated, not drawn | 3490 x 1574 |
| `signup-ok` | `#scenarios` | authored, steps cited | 1838 x 960 |
| `error-401` | `#scenarios` | authored, steps cited; no SQL step, measured before the fix; after it a test checks rows and cookies and the absence of SQL is inferred from the code (note (b2)) | 1180 x 530 |
| `error-403` | `#scenarios` | authored, steps cited | 1820 x 656 |
| `error-409` | `#scenarios` | authored, steps cited | 1533 x 582 |
| `activity-log-row` | `#scenarios` | authored, steps cited | 1926 x 567 |
| `push-to-image` | `#scenarios` | authored, steps cited | 1780 x 933 |
| `image-to-rollout` | `#scenarios` | authored, steps cited | 2205 x 960 |
| `inbound-packet-path` | `#scenarios` | authored, steps cited; the provider firewall and ServiceLB steps rest on a doc, not on a repo line | 2148 x 665 |
| `ingress-to-pod` | `#scenarios` | authored, steps cited; the response step authored | 1346 x 884 |
| `nightly-backup` | `#scenarios` | authored, steps cited | 1344 x 920 |

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

- Whether the development view reads at natural size. The verdict is in the table above, judged
  from downscaled images; collapsing to layers is still a possibility, not a result.
- The gain of `information_schema` over the regex. On today's migrations the regex gives the same 4
  FKs (`261027-if9-T2-green.txt`), so what remains is the forms the regex does not follow: an FK
  made in a DO block or a function, a rename, and `DROP TABLE`.

**Outside this change.** This change leaves out:

- migrating any of the seven Mermaid diagrams;
- the Mermaid files themselves. A LikeC4 view on the same subject is now built for each of `signup`
  (as `signup-ok`), `error-status-split` (as `error-401`, `error-403` and `error-409`),
  `push-to-image`, `image-to-rollout`, `inbound-packet-path`, `ingress-to-pod`, `delivery-nodes` (as
  `delivery`) and `monitoring-nodes` (as `monitoring`). Not every Mermaid branch is drawn: no view
  draws the 400 `VALIDATION_FAILED` section of `error-status-split`, and `signup-ok` draws the
  success path only, not the Mermaid `signup` branches for a body that fails validation, a taken
  email, a lost race on the unique email, or a failed authentication with its user DELETE
  rollback. Migrating the Mermaid files and retiring them stay outside;
- retiring `render-diagrams.sh`, `verify-diagrams.py`, `mermaid-config.json` and
  `render-manifest.tsv`, because a migration ends when its old home is gone, and that is a later
  change;
- a LikeC4 text-size legibility rule;
- the server-title check, the one check still proposed (the unknown-metadata-key check is built, see
  the metadata-key rule under Checks).

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
