# Phase 4: Leak Review and Production Go-Live - Pattern Map

**Mapped:** 2026-10-29
**Files analyzed:** 15 new or modified
**Analogs found:** 13 / 15 (2 partial)

All analog paths below were checked as tracked source (they live in `docs/`, `.github/`, `k8s/`, `.planning/`, none under a synced mirror). Line numbers were read this session.

Comment rule for every new script, workflow and manifest: no planning ids (`D-04`, `PUB-05`, `Pitfall 11`) in comments (`scripts/verify-comments.py`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `docs/architecture/leak-scan.mjs` (new) | utility (gate script) | batch / file-I/O | `docs/architecture/sync.mjs` | role-match (exit codes, header, arg parsing) |
| `docs/architecture/leak-scan.test.mjs` (new) | test | transform | none tracked (no `*.test.mjs` exists) | no analog |
| `.github/workflows/architecture-site.yml` (mod) | config (CI) | batch | itself, lines 71-81 | exact |
| `.github/workflows/invariant-checks.yml` (mod) | config (CI) | batch | `likec4-model-drift` job, lines 137-167 | exact |
| `.github/workflows/uptime-check.yml` (mod) | config (CI) | request-response | itself, lines 57-83, 155-167, 256-259 | exact |
| `k8s/overlays/architecture/certificate.yaml` (mod) | config (k8s) | declarative | itself, lines 1-14 | exact |
| `docs/architecture/model/edge.c4` (mod) | model | n/a | itself, lines 9-18 | exact |
| `docs/architecture/model/scenarios-platform.c4` (mod) | model | sequence | itself, lines 36-45 | exact |
| `docs/architecture/drills.tsv`, `drill.fish`, `drill-output.txt` (optional, Open Question 1) | test / drill | batch | itself | exact (if chosen) |
| `README.md` (mod) | doc | n/a | itself, lines 49-53 | exact |
| `docs/INFRA_ARCHITECTURE.md` (mod) | doc | n/a | itself, lines 293-297 | exact |
| `docs/INFRA_RUNBOOK.md` (mod) | doc | n/a | itself, lines 53-62 and 483-490 | exact |
| `04-scan-baseline.txt`, `04-flip-*.txt`, `04-uptime-*.txt`, `04-live-*.txt` (new evidence) | evidence | n/a | `03-06-live.txt`, `03-02-pr-proof.txt`, `261024-q91-ci-proof.txt` | exact |
| `04-DECISIONS.txt` (new approval record) | evidence | n/a | `03-DECISIONS.txt` | exact |
| `scripts/pre-commit-likec4-selftest.py` | test | n/a | not needed per research (hook cannot see the artifact) | skip |

## Pattern Assignments

### `docs/architecture/leak-scan.mjs` (utility, batch / file-I/O)

**Analog:** `docs/architecture/sync.mjs`

**Header and exit-code contract** (sync.mjs lines 1-8): copy the shape, with `0 clean, 1 leak found, 2 usage, 4 directory missing, unreadable or holding no files`.
```javascript
// The seam between the codebase and the LikeC4 model.
//
//   node docs/architecture/sync.mjs generate [--root DIR] [--model DIR]   rewrite generated.c4 from the code
//   node docs/architecture/sync.mjs check    [--root DIR] [--model DIR]   exit 1 when the model and the code disagree
//
// Exit codes: 0 clean, 1 drift, 2 usage, 3 tooling missing (likec4 or yaml not installed),
// 4 a k8s fact source is missing or unreadable.
```

**Imports and path setup** (sync.mjs lines 15-27): `node:` ESM imports, `fileURLToPath`, `process.argv.slice(2)`. Scan needs only `node:fs` and `node:path` (no loadTooling guard, zero dependencies).
```javascript
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, posix, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
const args = process.argv.slice(2)
```

**Usage and fail-closed exits** (sync.mjs lines 710-723, 703-707): usage to stderr then `process.exit(2)`; findings to stderr then `process.exit(1)`; clean prints `OK (<tally>)` to stdout.
```javascript
if ((mode === 'generate' || mode === 'check') && !existsSync(modelDir)) {
  console.error(`model folder ${modelDir} does not exist\nusage: ...`)
  process.exit(2)
}
...
  if (problems.length) {
    console.error(`DRIFT (${tally}):\n- ${problems.join('\n- ')}\n...`)
    process.exit(1)
  }
  console.log(`OK (${tally})\n...`)
```
Apply: exit 4 on missing or empty directory (sync.mjs line 317 and 635 use `process.exit(4)` for a missing fact source).

**Core pattern to write** (no analog for the scan body; use RESEARCH.md "Strict scan core" and Pattern 1 regex table): recursive `readdirSync(..., { withFileTypes: true })` generator, read each file as bytes with latin1 decoding, one `[name, regex]` table, print matched text only plus file and count, print a zero row per target on a clean run, hostname allow-list of exactly the four duckdns hosts, positive `robots.txt` check for `Disallow: /`. Write the report to `$GITHUB_STEP_SUMMARY` from inside the script when set (no `| tee`).

---

### `docs/architecture/leak-scan.test.mjs` (test, transform)

**No tracked analog.** `git ls-files` shows no `*.test.mjs` under `docs/architecture`. Use `node:test` (built in, Node 24.19.0) per RESEARCH.md: one case per target, one per TRAP string that must pass (`\bssh\b` shell-word list, SVG-style `58.06.87.06`, `k3s.monitoring`, `kanban-data`, `token`), clean and empty-directory (exit 4) cases. Build credential-shaped fixtures at runtime (`'AK' + 'IA' + ...`) so gitleaks does not refuse the commit. Closest structural reference for "failing output names the target": the drill rows in `docs/architecture/drills.tsv` (each row asserts a named check fires).

---

### `.github/workflows/architecture-site.yml` (CI, batch)

**Analog:** itself. Insert the new step between the app-shell check and the image build.

**Step shape to copy** (lines 71-81):
```yaml
      - name: Verify the model matches the code
        run: node docs/architecture/sync.mjs check

      - name: Build the static site
        run: pnpm --dir docs/architecture exec likec4 build --use-dot=false -o site/out model

      - name: Verify the build holds the app shell
        run: grep -q 'id="likec4-root"' docs/architecture/site/out/index.html

      - name: Build the image without pushing
        run: docker build docs/architecture/site
```
New step goes after line 78:
```yaml
      - name: Scan the build for leaks
        run: node docs/architecture/leak-scan.mjs docs/architecture/site/out
```
No `tee` (unspecified shell is `bash -e`, a pipe masks the exit code).

**Paths lists** (lines 27-32 push and 34-39 pull_request): add `docs/architecture/leak-scan.mjs` to both. Update the header comment at lines 7-9, which explains why `sync.mjs` is excluded; add a line saying the scan script is a trigger because changing it changes whether the same bytes pass.

---

### `.github/workflows/invariant-checks.yml` (CI, batch)

**Analog:** job `likec4-model-drift`, lines 137-167. Add one step after "Verify the model matches the code" (line 166-167), same indentation, no install change (the test needs no packages):
```yaml
      - name: Verify the model matches the code
        run: node docs/architecture/sync.mjs check
```
New: `- name: Verify the leak scan catches each target` / `run: node --test docs/architecture/leak-scan.test.mjs`. The job already installs Node 24.19.0 (lines 154-157).

---

### `.github/workflows/uptime-check.yml` (CI, request-response)

**Analog:** itself. Edits:

**Env block** (lines 64-83): add the site entry beside the folded scalars and the marker beside `UP_MARKER`:
```yaml
      HEALTH_URLS: >-
        https://kanban-board-rud-vlad-473.duckdns.org/api/healthz
        https://kanban-board-rud-vlad-473-nonprod.duckdns.org/api/healthz
      ...
      UP_MARKER: '"status":"UP"'
      DASHBOARD_MARKER: '"publicDashboardEnabled":true'
```
Add `SITE_URL: https://kanban-board-rud-vlad-473-architecture.duckdns.org/` and `SITE_MARKER: 'id="likec4-root"'`.

**Probe loop** (lines 155-167): add after the dashboard loop, before the drill block:
```bash
          for URL in $DASHBOARD_URLS; do
            run_check "$URL" "$DASHBOARD_MARKER"
          done
          ...
          if [ "$DRILL" = "true" ]; then
            DRILL_ACTIVE=true
            echo "Drill input is true: probing the known-wrong dashboard URL as well"
            run_check "$DRILL_URL" "$DASHBOARD_MARKER"
          fi
```
New: `run_check "$SITE_URL" "$SITE_MARKER"`. For the wrong-marker drill, a second boolean input (`drill_site`, mirrors `drill` at lines 44-48) probing `SITE_URL` with an impossible marker; env var wired like `DRILL: ${{ inputs.drill }}` (line 76); only `env:` interpolation, never `${{ }}` in the script.

**Timeout and arithmetic comment** (lines 57-59): `timeout-minutes: 8` becomes 10 (or 12 if both drills can be set; or make them mutually exclusive). Rewrite the comment: base 5 URLs x 80 s = 400 s (per URL 3 x 20 + 2 x 10), drill adds a sixth = 480 s.

**Text that names the current URL set and must be updated:** line 1 comment, step name at line 90 ("Probe both health endpoints and both dashboards"), the header "What is probed" bullet (lines 8-13), the `DRILL_LINE` sentence (line 258).

Order constraint: add the site entry only after the production certificate is Ready, since the probe never follows redirects and a staging chain gives status 000.

---

### `k8s/overlays/architecture/certificate.yaml` (k8s, declarative)

**Analog:** itself (the whole 14-line file):
```yaml
# Staging certificate for the architecture hostname; it moves to letsencrypt-production in a later,
# separate commit, after the built site has passed the leak review. ClusterIssuers are in
# k8s/platform/edge/clusterissuers.yaml.
...
  issuerRef:
    name: letsencrypt-staging
    kind: ClusterIssuer
```
Flip commit: change `name:` to `letsencrypt-production` and rewrite lines 1-3 in the same commit. Commit runs the full pre-commit hook (k8s manifest is feature-heavy, no `--no-verify`). Fetch and rebase over Flux bump commits before pushing; explicit refspec; no force.

---

### `docs/architecture/model/edge.c4` (model)

**Analog:** itself. Pre-review edits only (an edit after review changes the image tag and invalidates the approval):

Line 10 (`staging`): currently `'A staging issuer, defined in the repo and referenced today only by the Certificate of the architecture site. The prod, nonprod and monitoring Certificates all name the production issuer.'`. Reword so it is true before and after the flip, no referrer list.

Line 18 (`production`): currently `'Issuer for the trusted certificates, which the prod, nonprod and monitoring Certificates all reference.'`. Drop the referrer list.

Keep wording rules from `docs/DIAGRAM_CONVENTIONS.md`; the model prose must not name scan targets.

---

### `docs/architecture/model/scenarios-platform.c4` (model, sequence)

**Analog:** itself, lines 36-45. Add a `ci -> ci` step between lines 39 and 40 and update the view description at line 36 ("The drift check gates the build"):
```
    ci -> ci 'sync.mjs check runs first, and a drifted model stops the run before anything is built'
    ci -> ci 'likec4 build with the dot layout off writes the static site, and the image is built once without pushing'
```
New step text describes behaviour only, for example "the scan reads every file of the build and a hit stops the run before the image is built". It must not contain `ssh `, `kubectl`, `secret` or an IP. Inserting a step shifts cited line numbers in `03-SCENARIO-EVIDENCE.md`; re-cite.

---

### `drills.tsv` / `drill.fish` (optional, only if the owner picks the literal-drill option)

**Analog:** `docs/architecture/drills.tsv` lines 1-6 (tab-separated `drill title kind file value`, kinds sed/write/delete/mv/none) and `drill.fish` `run` (lines 40-45), hard-wired to `sync.mjs check`:
```fish
function run --inherit-variable sync --inherit-variable scratch
    set out (node $sync check --root $scratch --model $scratch/model 2>&1)
    set code $status
    echo "  exit=$code"
    printf '  %s\n' $out
end
```
A scan drill needs a second runner that builds (about 26 s per drill). Default recommendation is fixture tests plus one recorded real-build plant instead.

---

### `README.md`, `docs/INFRA_ARCHITECTURE.md`, `docs/INFRA_RUNBOOK.md` (docs)

**README** (lines 49-53), add one bullet in the same link style, root URL only:
```markdown
- [Network & OS](https://kanban-board-rud-vlad-473-monitoring.duckdns.org/public-dashboards/9b5bf7c8d3c743e1868eed84c9d3e5df)
```
New: `https://kanban-board-rud-vlad-473-architecture.duckdns.org/`.

**INFRA_ARCHITECTURE.md** lines 293-297: sentence "on a Let's Encrypt staging certificate until the built site has passed the leak review" becomes false after the flip; replace with the public URL and the production issuer. Edit after the certificate is Ready, in a separate commit from the flip. Docs-only, so `--no-verify` is allowed.

**INFRA_RUNBOOK.md** lines 55-62 (probed-URL list and drill sentence) add the site and the second drill; lines 483-488 contain the same stale staging sentence.

Verification: `rg -F "rud-vlad-473-architecture.duckdns.org" README.md docs/INFRA_ARCHITECTURE.md` finds both.

---

### Evidence files

**Approval record, analog `03-DECISIONS.txt`** (lines 1-8):
```
# Phase 3 decisions, recorded from the user's replies
DECISION Q1: 1 Create the repository, the executor pushes; user reply: "Q1 1"
DECISION Q5: 1 Push now; user reply: "Q5 1 Q6 1"
```
Copy as `04-DECISIONS.txt`, plus an `APPROVED_TAG main-<run>-<sha7>` line; the flip plan compares it to the overlay `newTag` on fresh `origin/main` and the pod image.

**Live measurements, analog `03-06-live.txt` lines 38-44** (key=value lines):
```
DEEPLINK /view/signin-ok/ nav_status=200 svg_nodes=184
MISSING_ASSET status=404 content_type=text/html immutable=no (...)
ROBOTS disallow=yes
```
Reuse these labels for the production re-measurement. Also reference `03-02-pr-proof.txt` (draft-PR red/green; a pure revert starts no path-filtered run, so green is "restore plus a comment") and `261024-q91-ci-proof.txt` (uptime drill red/green proof).

## Shared Patterns

### Fail closed on empty input
**Source:** `docs/architecture/sync.mjs` lines 6-7, 635 (`if (missing.length) process.exit(4)`). **Apply to:** `leak-scan.mjs`. An empty scan and a clean scan must not look the same.

### Values through env, never expression interpolation
**Source:** `.github/workflows/uptime-check.yml` lines 88-89 and 76. **Apply to:** every new workflow input or env value.

### One step per check, build job holds no secrets
**Source:** `architecture-site.yml` lines 10-12, 71-81. **Apply to:** the scan step; PR runs exercise it.

### Print matched text and count only
CI logs of this public repo are public; bundles are single huge lines. **Apply to:** scan report. Compare uptime-check.yml lines 137-138 ("The report gets the URL and the observed status only, never a response body").

### Blocking-human checkpoints with numbered options
**Source:** `03-DECISIONS.txt`. **Apply to:** push of the scan commit, the owner review, the flip push, and the uptime drills (they open and close the alert issue and email the owner).

### Ordering discipline
All model-text and site-workflow-path edits (edge.c4, scenarios-platform.c4, leak-scan.mjs path entry) land before the review; after review only `docs/*.md`, `README.md`, `k8s/**` and `uptime-check.yml` edits are safe (the site workflow `paths:` do not list them).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `docs/architecture/leak-scan.test.mjs` | test | transform | No tracked `node:test` file exists; use Node's built-in runner and RESEARCH.md Pattern 1 table |
| Scan core regex body in `leak-scan.mjs` | utility | batch | No existing artifact scanner; take regexes and traps from RESEARCH.md Pattern 1 |

## Metadata

**Analog search scope:** `docs/architecture/`, `.github/workflows/`, `k8s/overlays/architecture/`, `docs/*.md`, `README.md`, phase 03 evidence files.
**Files scanned:** about 16 read (several by targeted range).
**Pattern extraction date:** 2026-10-29
