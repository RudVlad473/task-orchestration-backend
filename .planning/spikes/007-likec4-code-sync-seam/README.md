---
spike: 007
idea: likec4-diagram-sync
name: likec4-code-sync-seam
type: standard
validates: "Given code facts (Flyway tables, Kafka topic constants, k8s workloads, cited source paths), when sync.mjs generates .c4 from them and checks the model, then a code change without a model change fails the check"
verdict: VALIDATED
related: [005, 006]
tags: [likec4, drift, ci, model-api, flyway, kubernetes]
---

# Spike 007: the code-to-model seam

## What This Validates

LikeC4 reads no Java. "Syncing with the codebase" therefore needs a seam written here. This spike builds the smallest one and tries to break it.

## Research

LikeC4's Model API (`LikeC4.fromWorkspace`, `computedModel().elements()`, `element.getMetadata(key)`) exposes every element's `metadata`, so a script can compare what the model claims against what the code contains. No LikeC4 feature does that itself; `likec4 validate` checks the model's own consistency only.

## How to Run

```
node .planning/spikes/007-likec4-code-sync-seam/sync.mjs check      # 1.1 s, exit 1 on drift
node .planning/spikes/007-likec4-code-sync-seam/sync.mjs generate   # rewrites model/generated.c4
fish .planning/spikes/007-likec4-code-sync-seam/drill.fish          # the drift drills
```

## What to Expect

`check` prints `OK (17 elements, 6 tables, 2 topics, 4 workloads)`. `drill-output.txt` holds the recorded drill run.

## Observability

None needed beyond the exit code and the itemised DRIFT list.

## Investigation Trail

1. **First `check` against my freshly written model failed on a real gap**: `k8s CronJob/postgres-backup` existed in `k8s/data/postgres/backup-cronjob.yaml` and my model omitted it. I had seen the file in a directory listing and still skipped it. This is the failure the seam exists to catch, and it fired on its first run.
2. `computedModel()` is async in 1.60 (docs say sync); one `await` fixed a crash.
3. **First drill run was invalid**: the baseline (untouched scratch copy) exited 1 because the scratch copy lacked `k8s/platform`, so every drill looked "caught" partly for an unrelated reason. Fixed the harness; a drill counts only against a green baseline.
4. Rerun: baseline OK; each drill produced exactly its own finding.

## Results

| Drill | Code change | Outcome |
|---|---|---|
| baseline | none | exit 0 |
| D1 | new migration creates `attachments` | exit 1, generated.c4 stale |
| D2 | new `KafkaTopics` constant `kanban.audit` | exit 1, topic not in model |
| D3 | new Deployment `worker` | exit 1, workload has no element |
| D4 | cited manifest deleted | exit 1, claim and source path both flagged |
| D5 negative control | a controller class renamed | exit 0, NOT caught, by design |

Direction of the proof: each of D1-D4 FAILS on the drifted code and the baseline PASSES on the unchanged code, so the checks discriminate. This is a spike proving the mechanism, not a shipped test, so there is no unit test over `sync.mjs`.

**What it does not catch** (and so what the diagram must not claim):

- Anything below the level of tables, topics, workloads and cited paths: relationships (who calls whom, FKs, which service talks to Postgres), thread pools, ports, annotations. A hand-drawn edge can be wrong forever. The `process` view's edges are authored, not derived.
- A metadata `source` that exists but no longer relates to the element (a path check, not a meaning check).
- The regexes are shallow: `CREATE TABLE` in migrations, `String X = "..."` constants, `kind:`/`metadata.name` in YAML. A table made by an ORM, a topic named in config, or a Helm-rendered workload is invisible. Kustomize overlays and HelmReleases were excluded from the workload scan.
- Whether the count 6 tables is the real schema: it ignores `ALTER TABLE ... RENAME` and tables created by Spring Session's own DDL.

Cost: about 125 lines of Node, run time 1.1 s, one dev dependency (`likec4`) already needed for rendering. Generated output is committed so a PR diff shows the table change.
