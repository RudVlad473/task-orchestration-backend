---
spike: 005
idea: likec4-diagram-sync
name: likec4-toolchain-and-views
type: standard
validates: "Given this repo's Node 24 / WSL box with no Graphviz installed, when likec4 1.60.0 validates, exports PNG and builds a static site, then all three work offline and the output is legible"
verdict: VALIDATED
related: [006, 007]
tags: [likec4, toolchain, pnpm, playwright]
---

# Spike 005: LikeC4 toolchain and views

The pinned tooling from this spike (`package.json`, `pnpm-lock.yaml`, `pnpm-workspace.yaml`) moved to `docs/architecture/` on 2026-10-27, and `docs/architecture/.gitignore` replaces this spike's. Only this README and the `smoke/` model stay here as evidence. The policy lives in docs/DIAGRAM_CONVENTIONS.md, section "LikeC4 standard".

## What This Validates

Given this repo's Node 24 / WSL box with no Graphviz installed, when `likec4` 1.60.0 validates, exports PNG and builds a static site, then all three work offline and the output is legible.

## Research

- Docs checked through context7 (`/likec4/likec4`): CLI (`validate`, `export png`, `build`, `start`), Model API (`LikeC4.fromWorkspace`), MCP server (`@likec4/mcp`, 19 read-only tools).
- Alternatives already in the repo: Mermaid `.mmd` rendered by a digest-pinned `mermaid-cli` container (`scripts/render-diagrams.sh`, docs/DIAGRAM_CONVENTIONS.md). Not compared head to head; this spike asks whether LikeC4 runs here, not whether it beats Mermaid.
- Chosen approach: pin `likec4` 1.60.0 in a spike-local `package.json`, install with pnpm (machine policy).

## How to Run

```
pnpm --dir docs/architecture install
docs/architecture/node_modules/.bin/likec4 validate .planning/spikes/005-likec4-toolchain-and-views/smoke
```

Everything past this smoke model goes through `docs/architecture/likec4.fish`.

## What to Expect

`validate` prints `Valid (1 files)` and exits 0. `export png` writes `out/smoke/index.png`. `build` writes a 5.7 MB static site.

## Investigation Trail

1. `pnpm install`: 54 packages, 78 s cold. pnpm reported `ERR_PNPM_IGNORED_BUILDS: esbuild`. Suspected this would break `build`; it did not (build 1.05 s).
2. `validate`: 1.2 s, offline. Graphviz comes bundled as WASM, so no `dot` binary is needed.
3. `export png` failed first: it drives Playwright, which wanted Chromium revision 1248. The box had 1234 and 1243 cached, so no match. Fix: `node node_modules/.pnpm/playwright@1.64.0/node_modules/playwright/cli.js install chromium-headless-shell` (one-time, into the shared `~/.cache/ms-playwright`). After that the export succeeded on retry 2 of 3, in 22 s for one view and about 12-20 s for five.
4. `.gitignore` check: `node_modules/` is NOT ignored repo-wide here, so the spike carries its own `.gitignore`.

## Results

VALIDATED. All three commands work offline. Costs to carry into a real adoption:

- A Playwright Chromium download is a hidden prerequisite for PNG export, version-coupled to the likec4 release. In CI, cache `~/.cache/ms-playwright` keyed by the likec4 version or the PNG step will download on every run.
- The `likec4` docs claim `computedModel()` is synchronous; in 1.60.0 it returns a Promise (found in spike 007).
- PNG export is nondeterministic in the way the existing pipeline is not: it uses a live browser, not a digest-pinned container. Whether pixels are stable across machines is unmeasured.
