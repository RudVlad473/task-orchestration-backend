# Phase 2: Model Completion - Pattern Map

**Mapped:** 2026-10-28
**Files analyzed:** 14 (4 new `.c4` families, 5 modified model/tooling files, 1 standard, 4 evidence files)
**Analogs found:** 12 / 14 (2 partial, see "No Analog Found")

All analog paths below were checked with `git ls-files` and are tracked. Scope: `docs/architecture/`, `docs/diagrams/`, `docs/DIAGRAM_CONVENTIONS.md`, Phase 1 artifacts. No mirror or runtime paths are used.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `docs/architecture/model/spec.c4` (modify: `provider`, `external`, `note`, `job` notation) | config (spec) | declarative | itself, `spec.c4:17-22`, `43-45` | exact |
| `docs/architecture/model/delivery.c4` (new: repo, ci, dockerhub, flux, provider nodes, `delivery` view) | model + deployment + view | declarative | `model.c4:6-11`, `deployment.c4:1-38`, `views.c4:33-38` | role-match |
| `docs/architecture/model/monitoring.c4` (new) | model + deployment + view | declarative | same trio | role-match |
| `docs/architecture/model/edge.c4` (new: hostfilter, firewall, svclb, certmanager, issuers) | model + deployment | declarative | `model.c4:6-11` (traefik), `deployment.c4:9-11,31-32` | role-match |
| `docs/architecture/model/deployment.c4` (modify: `extend`, new instances) | deployment | declarative | itself, `deployment.c4:13-26` | exact |
| `docs/architecture/model/views.c4` (modify: narrow `production`) | view | declarative | `views.c4:33-38` | exact |
| `docs/architecture/model/model.c4` (modify: PROC-01 elements, unify relationship titles, drop `PGHOST` label) | model | declarative | `model.c4:13-42`, `67-73`, `90-99` | exact |
| LOGI-01 note elements (`note` kind under `postgres` and an Avro-subjects note) | model (element) | declarative | `model.c4:51-57` (`spring_session` authored table with `source`) | role-match |
| `docs/architecture/model/scenarios*.c4` (SCEN-01..07, 9+ dynamic views) | view | request-response sequence | `views.c4:40-86` | exact |
| `docs/architecture/sync.mjs` (HelmRelease/ClusterIssuer/Flux readers, `host` key, label rule, participant rule, WR-03/04) | utility (checker) | transform / batch | `sync.mjs:233-243`, `368-389`, `394-410`, `415-423` | exact |
| `docs/architecture/drill.fish` (new drills D17+, `fresh()` widening) | test (drill harness) | batch | `drill.fish:16-24`, `120-186` | exact |
| `docs/architecture/drill-output.txt` (full re-record) | test fixture | file-I/O | itself | exact |
| `docs/DIAGRAM_CONVENTIONS.md` (Physical and Scenarios rows, stale sentences) | docs | n/a | itself, lines 100-106, 304-311, 338-343 | exact |
| Scenario evidence file (line-cited, one cite per step) | docs (evidence) | n/a | `01-01-edge-red.txt` style recordings; no cite-per-step analog | partial |

## Pattern Assignments

### `docs/architecture/model/spec.c4` (config, declarative)

**Analog:** itself. New kinds copy the closed-set declaration style.

Element kind with notation and style (`spec.c4:17-22`). D3 recommends generalising this notation in place rather than adding a kind:
```likec4
  element job {
    notation 'Scheduled job'
    style {
      color secondary
    }
  }
```
Deployment node kinds are bare declarations (`spec.c4:43-45`); `provider` copies this:
```likec4
  deploymentNode server
  deploymentNode cluster
  deploymentNode namespace
```
Style-bearing kind for `external` and `note` (`spec.c4:29-35`, shape + color + size):
```likec4
  element table {
    style {
      shape storage
      color green
      size small
    }
  }
```
Tag declarations are at `spec.c4:66-73`; no new tag is needed. Wave order: spec first (every other file depends on it).

---

### `delivery.c4`, `monitoring.c4`, `edge.c4` (model + deployment + view, declarative)

**Analogs:** `model.c4` (elements), `deployment.c4` (nodes/instances), `views.c4:33-38` (deployment view). No existing file combines all three, so each new file holds a `model { }`, an `extend`/`deployment { }` and a `views { }` block (research: separate files validate, spike-verified).

**Element with a checked anchor** (`model.c4:44-49`, copy for `certmanager` issuers via `workload`, and for HelmRelease-backed elements):
```likec4
  postgres = datastore 'Postgres 16' {
    description 'Single instance in ns kanban-data, holding the prod and nonprod databases'
    metadata {
      source 'k8s/data/postgres/postgres.yaml'
      workload 'StatefulSet/postgres'
    }
```
**Element with a directory source only** (`model.c4:6-11`, copy for `hostfilter` with `source 'infra/vm/k3s-host-firewall.sh'`, existence-checked only):
```likec4
  traefik = system 'Traefik' {
    description 'Public edge: TLS termination, routes to the app by host'
    metadata {
      source 'k8s/platform/traefik'
    }
  }
```
**Instances in namespace nodes** (`deployment.c4:23-35`; new monitoring and cert-manager instances go inside the existing empty nodes at `28-35`, via `extend` from the new file so plans touch disjoint files):
```likec4
      data = namespace 'kanban-data' {
        instanceOf postgres
        instanceOf backup
      }

      flux-system = namespace 'flux-system' {
      }
```
**Server/cluster node with architecture in the title** (`deployment.c4:2-6`; the runner node copies this shape because the standard requires arch in a server title):
```likec4
  netcup = server 'Netcup VPS (x86_64)' {
    description 'x86_64. KANBAN-INGRESS filter behind the Netcup Cloud Firewall.'
    technology 'x86_64'
    k3s = cluster 'k3s (single node)' {
```
**Deployment view** (`views.c4:33-38`). The `include netcup.**` glob here is the thing to narrow (Pitfall 2); each new view uses an explicit include list instead:
```likec4
  deployment view production {
    #physical
    title 'Physical view: what runs where'
    description 'Mapping of software to nodes. The VPS is x86_64.'
    include netcup, netcup.k3s, netcup.**
  }
```
Rules every new instance must satisfy: it needs at least one relationship or `checkViewMembership` fails it (`sync.mjs:415-423`); it carries exactly one `#physical`-style viewpoint tag per view (`sync.mjs:515-522`).

Cluster-scoped objects (ClusterIssuer): put the instance directly under `k3s`, anchor by `workload 'ClusterIssuer/<name>'`, never by `source 'k8s/platform/edge'` (research Pattern 3). No analog exists in `deployment.c4` for a cluster-level instance; `extend netcup.k3s { ... }` is spike-verified only.

---

### `docs/architecture/model/model.c4` (PROC-01, relationship title unification)

**Analog:** itself.

Child elements under a system (`model.c4:19-25`, copy for `management`, `registry`):
```likec4
    api = container 'REST API' {
      technology 'Spring MVC, Tomcat request threads'
      description '...'
      metadata {
        source 'src/main/java/com/vrudenko/kanban_board/controller'
      }
    }
```
Job element (`model.c4:67-73`, copy for the init job `register-schemas`, which also needs the `job` notation generalised, D3):
```likec4
  backup = job 'Postgres backup' {
    description 'CronJob that runs postgres-backup.sh as the prod DB role'
    metadata {
      source 'k8s/data/postgres/backup-cronjob.yaml'
      workload 'CronJob/postgres-backup'
    }
  }
```
Relationships block (`model.c4:90-99`). Edit sites for D1-A (identical title per instance pair so the deployment edge no longer reads `[...]`): lines 91, 94, 98, 99. Line 91 also carries the leaked in-cluster name to replace:
```likec4
  backup -[sync]-> postgres 'PGHOST postgres.kanban-db'   // actual: 'PGHOST postgres.kanban-data'
  kanban.api -[sync]-> postgres 'JDBC: users, boards, ...'
  kanban.consumer -[sync]-> postgres 'INSERT activity_log, idempotent on event_id'
  kanban.consumer -[async]-> redpanda.dlt 'after retries'
```
(Line 91 reads `'PGHOST postgres.kanban-data'` in the repo; do not copy it.) Kinds: `-[sync]->` and `-[async]->` only; no relationship metadata (roadmap note).

---

### LOGI-01 note elements

**Analog:** `model.c4:51-57`, the authored table with a `source` and an "Authored." description:
```likec4
    spring_session = table 'spring_session' {
      technology 'Spring Session JDBC'
      description 'Authored. Spring Session creates this table at startup (initialize-schema=always), outside Flyway.'
      metadata {
        source 'src/main/resources/application.properties'
      }
    }
```
Copy the shape for a `note` child of `postgres` (source: `src/main/resources/db/migration/V3__add_activity_log.sql`) and an Avro-subjects note (source: `src/main/avro`, a directory; `existsSync` accepts it, `sync.mjs:538-541`). The data view `include postgres.*` (`views.c4:23`) picks up the child with no relationship. `generated.c4` is generated; do not hand-edit it.

---

### `docs/architecture/model/scenarios*.c4` (SCEN-01..07, view, sequence)

**Analog:** `views.c4:40-86` (`signin-ok`, `signin-bad-credentials`, `signin-ceiling`). SCEN-01 and SCEN-06 reuse its participants.

Full shape (`views.c4:40-55`):
```likec4
  dynamic view signin-ok {
    #scenarios
    variant sequence
    title 'Scenario: signin succeeds'
    description '...'

    client -> traefik 'POST /api/signin {email, password}'
    traefik -> kanban.api 'forward'
    kanban.api -> postgres.table_users 'SELECT user by email'
    kanban.api <- postgres.table_users 'row'
    kanban.api -> postgres.spring_session 'rotate session id, save security context'
    traefik <- kanban.api '200 Set-Cookie JSESSIONID'
    client <- traefik '200 {id, email, displayName, theme}'
  }
```
Conventions visible in the analog and required to copy:
- exactly one viewpoint tag `#scenarios`, `variant sequence`, `title 'Scenario: ...'`;
- every step has an explicit string title (Pitfall 6);
- leaf participants only: `postgres.table_users`, never `postgres` alongside a child (Pitfall 5); signup and the error views use `postgres.table_users` the same way;
- one outcome per view (`signin-bad-credentials` description at `views.c4:61` explains why a wrong-password view is not duplicated);
- at most 5 participants (the new rule). `signin-ok` has exactly 5, the ceiling case.

Outlines only, not evidence: `docs/diagrams/scenarios/{signup,error-status-split,push-to-image,image-to-rollout,inbound-packet-path,ingress-to-pod}.mmd` and `docs/diagrams/process/activity-pipeline.mmd`, `docs/diagrams/physical/{delivery-nodes,monitoring-nodes,production-host}.mmd`. Mermaid files stay (out of scope to retire).

---

### `docs/architecture/sync.mjs` (utility, transform)

**Analog:** itself. Each new rule copies the shape of an existing one: a function taking `(computed, docs, problems)`, deterministic sorted output, `problems.push(...)`, called from `check()`.

**Workload reader** (`sync.mjs:230-243`). GATE-01 widening changes `WORKLOAD_DIRS` / `WORKLOAD_KINDS`; the comment on 230-232 ("left out on purpose") becomes the IN-03 explanation for excluding `controller-resources.yaml`:
```js
const WORKLOAD_DIRS = ['k8s/base/', 'k8s/data/', 'k8s/platform/']
const WORKLOAD_KINDS = new Set(['Deployment', 'StatefulSet', 'CronJob'])

function workloads(docs) {
  const out = new Set()
  for (const { file, o } of docs) {
    if (!WORKLOAD_DIRS.some((d) => file.startsWith(d))) continue
    if (WORKLOAD_KINDS.has(o.kind) && typeof o.metadata?.name === 'string') out.add(`${o.kind}/${o.metadata.name}`)
  }
  return out
}
```
`file.startsWith(d)` accepts a single-file path, so adding `'k8s/flux-system/gotk-components.yaml'` works, and `'k8s/monitoring/'` is a plain addition. Add `HelmRelease` and `ClusterIssuer` to the kind set.

**Both-ways comparison** (`sync.mjs:533-536`), already consumes `workload` arrays via `.flat()`:
```js
const modelWorkloads = new Set(elements.flatMap((e) => [e.getMetadata('workload')].flat().filter(Boolean)))
...
for (const w of manifestWorkloads) if (!modelWorkloads.has(w)) problems.push(`k8s ${w} has no element carrying metadata workload '${w}'`)
for (const w of modelWorkloads) if (!manifestWorkloads.has(w)) problems.push(`model claims workload ${w} but k8s/ has no such manifest`)
```
The hosts reader copies this two-way pair, with a new `routedHosts(docs)` over `IngressRoute` documents (research "Host reader" excerpt) and `host` added to `METADATA_KEYS` at `sync.mjs:53`.

**New rules (label rule, participant rule): analog is `checkViewMembership`** (`sync.mjs:415-423`), same view loop and accessor style:
```js
function checkViewMembership(computed, problems) {
  const drawn = new Set()
  for (const view of computed.views()) {
    if (!view.isDeploymentView?.()) continue
    for (const n of view.nodes()) if (n.kind === 'instance') drawn.add(n.id)
  }
  const ids = [...computed.deployment.instances()].map((i) => i.id).sort()
  for (const id of ids) if (!drawn.has(id)) problems.push(`instance ${id} appears in no deployment view`)
}
```
`isDeploymentView?.()` is proven here; the dynamic-view accessor (`isDynamicView`) is NOT proven in the repo (research A6), so confirm it or use the `_type` approach before relying on it.

**Placement rule** (`sync.mjs:368-389`) is where WR-03 and WR-04 land: the early `if (!anchors.length) continue` at line 383 is the silent skip; and `file.startsWith(\`${s}/\`)` at line 381 is the trailing-slash trap:
```js
    const anchors = docs.filter(({ file, o }) =>
      workloads.length ? workloads.includes(`${o.kind}/${o.metadata?.name}`) : sources.some((s) => file === s || file.startsWith(`${s}/`)),
    )
    if (!anchors.length) continue
```
HelmRelease placement is already handled by `sync.mjs:356-358` (targetNamespace) and source objects by `355`; ClusterIssuer has no namespace so `placeOf` returns `[]` (line 360).

**Metadata key rule** (`sync.mjs:394-410`) covers elements, deployment nodes, instances and relationships; no change besides the allowlist.

**Fact-source guard and exit 4** (`sync.mjs:249-277`): `K8S_DIRS` plus `k8sDocs()` parse error exit; reuse `k8sDocs`, do not add a second walk.

---

### `docs/architecture/drill.fish` (test, batch)

**Analog:** itself. Each drill: `echo "== Dn: ..."`, `fresh`, one `sed`/`printf` edit on `$scratch`, `run`. Last drill is D14c; new ones continue from D17 (research note), though existing numbering already inserted D16/D16b before D12 so check order in the file.

**Scratch copy (`fresh()`, lines 16-24)** is what Pitfall 4 targets. Widen it in the same plan that adds the first `source` outside the copied trees:
```fish
function fresh --inherit-variable repo --inherit-variable scratch --inherit-variable here
    test -e $scratch; and trash-put $scratch
    mkdir -p $scratch/src/main/resources/db $scratch/src/main/java/com/vrudenko/kanban_board
    cp -r $repo/src/main/resources/db/migration $scratch/src/main/resources/db/
    cp $repo/src/main/resources/application.properties $scratch/src/main/resources/
    cp -r $repo/src/main/java/com/vrudenko/kanban_board/. $scratch/src/main/java/com/vrudenko/kanban_board/
    cp -r $repo/k8s $scratch/
    cp -r $here/model $scratch/model
end
```
Add `cp -r $repo/.github $scratch/`, `cp -r $repo/infra $scratch/`, and `cp -r $repo/src/main/avro $scratch/src/main/` (`mkdir` first).

**Rename-in-k8s drill** (copy `drill.fish:125-128`, D10b) for HelmRelease, Flux controller and namespace drills:
```fish
echo "== D10b: a manifest namespace is misspelled (kube-sytem)"
fresh
sed -i 's|namespace: kube-system|namespace: kube-sytem|' $scratch/k8s/platform/traefik/helmchartconfig.yaml
run
```
The Flux drill must edit `$scratch/k8s/flux-system/gotk-components.yaml`, not `controller-resources.yaml` (Pitfall 3).

**Drill that adds an element** (helper functions `certmgr_into` / `lonely_into`, `drill.fish:33-47`) is the pattern for a "deleted instance" drill (WR-03) and a "claimed host" drill:
```fish
function lonely_into --inherit-variable scratch
    set body "  lonely = system 'Lonely'"
    test "$argv[1]" = linked; and set body $body "  traefik -[sync]-> lonely 'probe'"
    printf '%s\n' 'model {' $body '}' >$scratch/model/lonely.c4
    sed -i "s|^\(        instanceOf backup\)\$|\1\n        instanceOf lonely|" $scratch/model/deployment.c4
end
```
**Control drills** (`D13b`, `D16b`, lines 146-171) are the model for the green counterpart of each new red drill. **View edits**: `sed -i '/^    #process$/d' ... views.c4` (D6, line 81) is the model for the `[...]` and 6-participant breaks (edit a copy of the model, never the real one). `SYNC=<old script>` at lines 12-13 supports the "not caught before" recording, as in Phase 1's `01-drills-before.txt`. Header comment lines 2-8 list the controls and must be updated when controls are added.

---

### `docs/architecture/drill-output.txt` (fixture)

**Analog:** itself (131 lines, header format `== name`, `  exit=N`, `  OK|DRIFT (tally)`). Every section contains the tally line (`33 elements ... 4 workloads`, line 3), so any new element or workload changes every section; re-record the whole file once, last. Verify with `fish docs/architecture/drill.fish | cmp - docs/architecture/drill-output.txt`.

Red-recording format to copy for per-reader "red on the real defect" evidence is `.planning/workstreams/architecture-views/phases/01-foundations-edge-fix-and-honesty-gates/01-01-edge-red.txt` (the raw `DRIFT (...)` block followed by `exit=1`). Also `01-drills-before.txt` for the before-state recording.

---

### `docs/DIAGRAM_CONVENTIONS.md` (docs, STD-01)

**Analog:** itself. Edit sites (verified by line):
- Physical row, line 105: contains `the monitoring stack (excluded, as in \`production-host.mmd\`)` and the provenance text; success criterion 5 requires no remaining hit for `excluded, as in`.
- Scenarios row, line 106: "internal calls" / "authored".
- Lines 304-311: "the four diagrams with no LikeC4 counterpart yet: `signup`, `error-status-split`, `push-to-image` and `inbound-packet-path`" becomes stale once SCEN-01/02/03/05 land.
- Table at lines 338-343 lists scenario questions per diagram (rows exist for signup, error-status-split, push-to-image, inbound-packet-path; add or confirm rows for image-to-rollout and ingress-to-pod).
- Per research Pitfall 9 also line 51 ("No diagram embedded in the docs uses it yet"), lines 64-66 (closed kind list, only if a kind is added) and the Drills prose counts.

Table row format to keep (line 105):
```
| Physical | `deployment` model and `deployment view` | servers (arch in title), cluster, namespaces, workloads | pods, replicas, secrets, ... | workloads checked against ...; the server and cluster nodes authored |
```

---

## Shared Patterns

### Provenance ladder (generated, checked, authored)
**Source:** `model.c4` metadata blocks, `sync.mjs:538-541`
**Apply to:** every new element. `source` is existence-checked on `--root`; `workload` is two-way checked. `METADATA_KEYS` (`sync.mjs:53`) is a closed allowlist, applied to elements, nodes, instances and relationships (`sync.mjs:394-410`). Descriptions of authored elements begin "Authored." (`model.c4:53`).

### One sentence descriptions, no leakage
**Source:** `model.c4` descriptions
**Apply to:** all new `description` / title strings. No numbers in labels the checks cannot verify (existing `:443`/`:8080` stay untouched), no in-cluster DNS names, bucket name, endpoint, age recipient, secret names (Pitfall 8; gitleaks also scans `.planning/`).

### Red then green, plus control
**Source:** `drill.fish` D13/D13b, D16/D16b; Phase 1 red/green files `01-01-edge-red.txt`, `01-01-edge-green.txt`, `01-03-selftest-red.txt`
**Apply to:** every new reader or rule. A widening and the elements it forces land in one commit (the hook refuses a red tree); record the red output from the uncommitted intermediate state first.

### Commit hygiene
**Source:** `.claude/CLAUDE.md`
**Apply to:** all plans. Phase 2 touches docs only, so `--no-verify` is permitted, which skips the LikeC4 check in the hook; run `node docs/architecture/sync.mjs check` by hand before each commit.

---

## No Analog Found

| File / Construct | Role | Data Flow | Reason |
|---|---|---|---|
| `provider` deployment-node top-level (GitHub, Docker Hub, Internet) | deployment | declarative | `deployment.c4` has a single top-level `server`; the `provider` kind and `extend` usage are spike-verified only (research Pattern 1), nothing in the repo to copy |
| Non-Kubernetes instance under the server node (`extend netcup { hf = instanceOf hostfilter }`) and cluster-level instance for ClusterIssuer | deployment | declarative | all existing instances sit in namespace nodes (`deployment.c4:9-35`) |
| `[...]` edge-label rule and dynamic-view participant rule | checker rule | transform | nearest shape is `checkViewMembership` (`sync.mjs:415-423`), but no existing rule reads `view.edges()` labels or counts dynamic-view nodes; the accessor `isDynamicView` is unproven (A6) |
| Hosts reader | checker reader | transform | no existing reader inspects `IngressRoute` `spec.routes[].match`; use `k8sDocs()` plus the regex in research "Host reader" |
| Scenario evidence file (one cited line per step) | evidence | n/a | no earlier file maps scenario steps to code lines; Phase 1 evidence files record command output only. Planner should define the format (table: step, file:line, quote) |
| `note` element kind with `shape document` | spec | declarative | no note-like element exists today; spike-verified only |

## Metadata

**Analog search scope:** `docs/architecture/` (model, sync.mjs, drill.fish, drill-output.txt), `docs/DIAGRAM_CONVENTIONS.md`, `docs/diagrams/`, `.planning/workstreams/architecture-views/phases/01-*/`.
**Files scanned:** about 16 read in full or in the relevant range; tracked status confirmed with `git ls-files` for `docs/architecture`, `docs/diagrams`, `.github/workflows`, `infra/vm`, `k8s/flux-system`.
**Not read in this pass:** k8s manifests and Java sources (their facts are taken from 02-RESEARCH.md citations, unverified here); `gotk-components.yaml`.
**Pattern extraction date:** 2026-10-28
