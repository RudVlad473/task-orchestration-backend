---
spike: 006
idea: likec4-diagram-sync
name: likec4-model-of-this-app
type: standard
validates: "Given the real topology (Traefik, app JVM thread pools, Postgres, Redpanda, k3s node, backup CronJob), when modelled once in LikeC4, then a context view, a process view, a data view, a deployment view and a dynamic signin view all render from that one source"
verdict: VALIDATED
related: [005, 007]
tags: [likec4, diagrams, c4, deployment, dynamic-view]
---

# Spike 006: LikeC4 model of this app

## What This Validates

Given the real topology, when modelled once, then five views render from one source: `index` (context), `process`, `data`, `production` (deployment) and `signin` (dynamic).

## How to Run

```
fish .planning/spikes/006-likec4-model-of-this-app/likec4.fish start    # live viewer, http://localhost:61000
fish .planning/spikes/006-likec4-model-of-this-app/likec4.fish png      # out/*.png (not committed: the repo's root `out/` ignore rule covers them; regenerate)
fish .planning/spikes/006-likec4-model-of-this-app/likec4.fish site     # static site in out/site
```

## What to Expect

- `out/index.png`: client, Traefik, backend, Postgres, Redpanda, backup job.
- `out/process.png`: request thread, `kafka-publish` executor, `kanban.activity`, consumer, `activity_log`, DLT.
- `out/production.png`: Netcup VPS (x86_64) containing k3s containing four namespaces, with the same elements placed inside them.
- `out/signin.png`: numbered steps 1 to 7 from `POST /api/signin` to the cookie.
- `out/data.png`: six table boxes, written by spike 007's generator.

In `start`, clicking an element opens its metadata (`source`, `workload`) and the views that contain it.

## Investigation Trail

1. First deployment syntax was wrong (`node netcup = server`); the validator named the exact line. Correct form is `netcup = server 'title' { ... }`.
2. Rendered all four views and read them. Two authoring defects found by looking, not by the validator: (a) `include netcup.**` drew the children but not the VPS wrapper, so the physical view lacked the architecture annotation DIAGRAM_CONVENTIONS requires; fixed with `include netcup, netcup.k3s, netcup.**`. (b) the deployment node's `technology 'x86_64'` field does not render on the box, so the architecture moved into the title.
3. The process view leaves `client` unconnected (Traefik is not in that view). Left as is; shows that a view only draws edges between elements it includes.
4. I wrote "pg_dump on a schedule" for the backup job from the filename alone. The manifest says it runs `postgres-backup.sh` daily at 02:17; corrected before it shipped. Model facts need the same read-the-source discipline as prose.

## Results

VALIDATED, with these limits:

- **Collapsed edges print `[...]`** when several relationships merge into one line (context and production views). Readable in the interactive viewer, ugly in a PNG.
- **Dynamic views are linear.** The existing `scenarios/signin.mmd` has `alt`/`else` branches (400, 401 identical body, ceiling); the LikeC4 version shows only the happy path. A scenario with branching stays in Mermaid.
- **Layout is automatic.** Few knobs; the production view was acceptable without tuning, the process view is a tall single column.
- **One model, many views** is the real gain: the backup CronJob added once appeared in the context view, the process-less deployment view and the data namespace without re-drawing anything.
- No class-level elements were modelled, on purpose (docs/DIAGRAM_CONVENTIONS.md "does the diagram's truth survive a refactor").
