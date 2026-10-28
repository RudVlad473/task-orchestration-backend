---
phase: 03-build-and-serve-on-a-staging-certificate
reviewed: 2026-10-28T23:30:00Z
depth: standard
files_reviewed: 18
files_reviewed_list:
  - .github/workflows/architecture-site.yml
  - docs/architecture/site/Dockerfile
  - docs/architecture/site/nginx.conf
  - scripts/verify-k8s-invariants.py
  - scripts/verify-k8s-invariants-selftest.py
  - k8s/platform/edge/clusterissuers.yaml
  - k8s/platform/namespaces.yaml
  - k8s/base/architecture-site/architecture-site.yaml
  - k8s/base/architecture-site/kustomization.yaml
  - k8s/overlays/architecture/kustomization.yaml
  - k8s/overlays/architecture/certificate.yaml
  - k8s/overlays/architecture/ingressroute.yaml
  - k8s/flux-system/apps-architecture.yaml
  - k8s/flux-system/image-automation.yaml
  - k8s/flux-system/kustomization.yaml
  - docs/architecture/model/delivery.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/drills.tsv
findings:
  critical: 0
  warning: 6
  info: 6
  total: 12
status: issues_found
---

# Phase 03: Code Review Report

**Reviewed:** 2026-10-28
**Depth:** standard
**Files Reviewed:** 18
**Status:** issues_found

## Summary

Scope was the phase's commits `abd0265..HEAD` on main, code and config only. The Flux bot's image-bump commits and `.planning/` records were skipped. Also read for context: `deploy.yml`, `backup-image.yml`, `dependabot.yml`, and the model files `model.c4`, `deployment.c4` and `views.c4`. The model files were read only as far as the diff touched them.

Nothing in scope gives a secret to untrusted code. The review turned up no critical issues. The weaknesses are in what the new gates cannot see, in claims the model makes that are false, and in nginx hardening.

Secret-exposure checks that came back clean:
- The trigger is `pull_request`, not `pull_request_target`.
- The build job (`architecture-site.yml:50-89`) has no `environment:` and no `secrets.` reference. Its token is `contents: read` (line 42-43).
- The publish job is guarded by `if: ... github.ref == 'refs/heads/main'` (line 93).
- I read the `production` environment's deployment-branch policy with `gh api` (read-only). It holds exactly one policy, branch `main`, so a branch copy of this workflow that drops the guard cannot get `DOCKERHUB_TOKEN`.
- Artifact hand-over is within one run (no `run-id`), and nothing in the repo consumes a pull-request artifact.

The ClusterIP solver edit is correct. `serviceType` is a valid `http01.ingress` field and the cert-manager default is NodePort. It applies to both shared issuers, so every certificate renewal goes through it. The architecture staging certificate was issued live through it per `03-03-solver.txt` and `03-06-live.txt`; I did not re-read those records this run, and the production issuer's identical stanza has not been exercised (see WR-03).

**Probe method for WR-01 and WR-02.** I imported `check_i12_no_dev_server` from `scripts/verify-k8s-invariants.py` with `python3 -I`, using a script in `/tmp/i12probe/probe.py` outside the repo, and fed it 14 hand-written fixtures. The shipped gate and selftest both pass on the current tree: `invariants OK`, `selftest OK -- I1-I12`.

**Could not verify:**
- That the `main-*.js` files under `/assets/` are content-hashed. No built `site/out` exists locally. The runbook names `assets/main-*.js`, and Vite hashes by default.
- What the built site discloses, which the phase defers to a leak review.
- The commit SHAs behind the pinned actions at `architecture-site.yml:99,102,108,123`.
- The scope of `DOCKERHUB_TOKEN`: whether it can push to every repository under the account or only this one.

## Warnings

### WR-01: I12 line pass misses the ordinary ways to spell `likec4 start`

**File:** `scripts/verify-k8s-invariants.py:128,582,590,621`
**Issue:** I12 exists because the dev server answered `/@fs/etc/hostname` with file contents. `DEV_SERVER_RE` (line 128) needs the literal text `likec4<whitespace>start|serve|dev|preview` on one comment-stripped line. Probe results (MISSED means I12 reported nothing):

| Fixture | Result |
|---|---|
| `Dockerfile: RUN pnpm exec likec4 start` | caught |
| `Dockerfile: RUN pnpm exec likec4 \` followed by `    start` (shell line continuation) | MISSED |
| Same continuation inside a workflow `run: \|` block | MISSED |
| `CMD npx likec4@1.60.0 start` (the version-pinned form) | MISSED |
| `CMD pnpm dlx likec4@latest dev` | MISSED |
| `CMD node node_modules/likec4/bin/likec4.mjs start` | MISSED |
| `CMD pnpm run dev` (a `package.json` script; `docs/architecture/package.json` has no `scripts` today) | MISSED |
| `CMD npx vite --host 0.0.0.0` (only `vite preview` is listed) | MISSED |
| `run: echo "x #y" && pnpm exec likec4 start` | MISSED |
| k8s `command: [npx]`, `args: ['likec4@latest', start]` | MISSED |
| Dockerfile JSON array broken across lines with `\` | MISSED |

- **Quoted `#`.** `line.split(" #", 1)[0]` (line 582) treats a `#` inside a quoted string as a comment. In `sh -c "..."` the rest of the line still executes, but I12 discards it.
- **Argument spelling.** The container `command` and `args` pass (line 621) joins the arguments but still needs `likec4` and `start` separated by whitespace. `likec4@latest` fails that.
- **Exposure today.** None of these appears in the repo, so nothing is exposed now. The gate's docstring lists some holes (scripts, shell variables, HelmRelease values, `likec4.fish`) but not these. The version-pinned `npx` or `dlx` form is the likeliest accident, because the repo pins likec4 elsewhere.

**Fix:** Make the match tolerant and join before matching.
```python
# normalise: join backslash continuations, strip quotes and an optional @version after the binary
DEV_SERVER_RE = re.compile(
    r"\blikec4(?:@\S+)?(?:\.mjs)?[\s'\",\[\]]+(?:start|serve|dev|preview)\b"
    r"|\bvite\s+(?:preview|dev)\b|--listen(?![\w-])"
)
```
- Join each `\` continuation before matching. Pre-process `text` with `re.sub(r"\\\n\s*", " ", text)`.
- Drop only a comment that starts a shell word, such as `(^|\s)#`, and only outside quotes.
- In the container pass, join `command` and `args` with a space and also test the string with quotes stripped.
- Add selftest cases for each MISSED row above.
- Read `package.json` `scripts`, or fail on any script whose body matches.

### WR-02: I12 scans source text only, so kustomize inline patches and several file shapes are never read

**File:** `scripts/verify-k8s-invariants.py:628-646` and `:603-622`
**Issue:**
- **Kustomization patches.** The container check walks `iter_pod_templates(doc)` on each parsed document. An overlay `kustomization.yaml` that adds `args` through an inline `patches: - patch: |-` is a string, not a pod template, and the line pass sees only `- start` on its own line. Probe fixture `flow-list-patch` came back MISSED. The same gate already renders every overlay for I1-I6, but I12 does not reuse the rendered output, which is where a patched `command` or `args` appears in full. The selftest case "JSON6902 patch list is skipped" (selftest, I12 block) asserts that such patch files are ignored.
- **Dockerfile names.** `name.startswith("Dockerfile")` (line 643) skips `site.Dockerfile`, `app.Dockerfile` and `Containerfile`.
- **Workflows.** The workflow match is `posixpath.dirname(rel) == ".github/workflows"` (line 642). Composite actions under `.github/actions/*/action.yml` are skipped.
- **Compose.** `docker-compose.yml` is not in scope (line 641-643).
- **Excluded directories.** `os.walk` prunes every directory named `out` at any depth (line 635), not only `docs/architecture/site/out`.

**Fix:**
- Run the container check over the rendered documents that `main()` already produces for each root, as well as over source.
- Match Dockerfiles with `name == "Dockerfile" or name.endswith(".Dockerfile") or name == "Containerfile"`.
- Include `.github/actions/**`.
- Prune `out` only when `rel_dir` is `docs/architecture/site/out`.

### WR-03: The ClusterIP solver fix has no regression guard

**File:** `k8s/platform/edge/clusterissuers.yaml:21-24,36-39`
**Issue:** The new comment (lines 8-10) says the cert-manager NodePort default is invisible to every rendered-manifest check. That also means nothing fails if `serviceType: ClusterIP` is deleted from either issuer, or if a third issuer or solver is added without it. `rg "serviceType|http01" scripts/` returns nothing, and drill D23 (`drills.tsv`) only renames an issuer. A silent revert re-opens a NodePort on the host. The edge-filter documentation in `INFRA_RUNBOOK.md` lines 243-364 is the reason I1 exists. The fix is the very case the gate comment calls invisible, and it is not pinned. The change's blast radius is every certificate renewal in prod, nonprod and monitoring, but only the staging issuer's path has live evidence.

**Fix:**
- Add an invariant: every `ClusterIssuer` and `Issuer` solver with an `http01.ingress` block must set `serviceType: ClusterIP`. Add a red selftest case with the field absent and a clean one with it present.
- Optionally add a drill that deletes the field.
- Run it as a test that fails on the unfixed code, as the global rules require.

### WR-04: nginx sends no security headers, and `server_tokens` is on

**File:** `docs/architecture/site/nginx.conf:11-39`
**Issue:**
- **Missing headers.** No `X-Content-Type-Options: nosniff`, no `Content-Security-Policy` (`frame-ancestors` at minimum), no `Referrer-Policy`. Traefik has no Middleware for them either (`k8s/overlays/architecture/ingressroute.yaml:28-37` only redirects).
- **Version leak.** The `Server: nginx/<version>` header and the default 404 body disclose the nginx version.
- **Header-inheritance trap.** Every `add_header` is inside a `location` (lines 27, 32, 37). nginx drops inherited `add_header` directives from `server` level whenever a `location` declares its own. Adding a server-level security header later would silently not apply to `/assets/`, extension files and `/`, and only `/healthz` would carry it.
- **Gzip.** `gzip on` has no `gzip_vary on`, so a shared cache between Traefik and a client can serve gzip bytes to a client that did not ask for them.
- **Path handling.** I traced these cases and none escapes the root:
  - `/@fs/etc/hostname` has no extension, so it gets `index.html` with 200.
  - `/@fs/etc/hostname.txt` and `/..%2f` forms are normalised and answer 404.
  - Dotfiles hit the extension regex and answer 404.

**Fix:**
```nginx
server_tokens off;
gzip_vary on;
# repeat in every location that sets its own add_header, or move them to an include:
add_header X-Content-Type-Options "nosniff" always;
add_header Content-Security-Policy "frame-ancestors 'none'" always;
add_header Referrer-Policy "no-referrer" always;
```
A full CSP needs the built bundle checked first, because the LikeC4 app may inline scripts or styles. I did not verify what it needs.

### WR-05: The published model asserts two things that are false

**File:** `docs/architecture/model/delivery.c4:7` and `docs/architecture/model/scenarios-platform.c4:38`
**Issue:**
- **"Two workflows".** `delivery.c4:7` says the `ci` system is "Two workflows: deploy.yml ... architecture-site.yml". The repo has ten workflow files. `backup-image.yml` also pushes an image to Docker Hub. It builds `kanban-postgres-backup` and runs in `environment: production`.
- **"The only job in the production environment".** `scenarios-platform.c4:38` says "the publish job, the only job in the production environment". `rg "environment:"` shows `environment: production` on `deploy.yml:94,161,307` and `backup-image.yml`, and `environment: staging` on `deploy.yml:236,380`. The sentence was meant to be about this workflow, and as written it is false. It also misstates where the registry token's trust boundary sits, since `DOCKERHUB_TOKEN` is shared with those jobs. This model is published at a public hostname, and the drift gate (`sync.mjs check`) does not check prose claims.

**Fix:**
- `delivery.c4:7`: say what each workflow does without a count, or list all that push images (deploy.yml, backup-image.yml, architecture-site.yml).
- `scenarios-platform.c4:38`: "the publish job, the only job of this workflow in the production environment, whose DOCKERHUB_TOKEN is shared with deploy.yml and backup-image.yml".

### WR-06: Dependabot does not cover the digest-pinned nginx base or the LikeC4 lockfile

**File:** `docs/architecture/site/Dockerfile:6`, `.github/dependabot.yml`
**Issue:** The base image is pinned by tag and digest (`1.30.6-alpine-slim@sha256:5d8bb94...`), and the pin carries no update path. `dependabot.yml` has only the `gradle` and `github-actions` ecosystems. Nothing opens a PR for the `docker` ecosystem, so the digest freezes while the container serves unauthenticated internet traffic. Nothing covers the `npm` ecosystem for `docs/architecture/pnpm-lock.yaml` either, so likec4 and its transitive dependencies also freeze. A digest pin without an updater is a decision to run the same bytes indefinitely.

**Fix:** Add a `docker` entry for `/docs/architecture/site` and an `npm` entry for `/docs/architecture` to `.github/dependabot.yml`. Alternatively, record the accepted risk in the Dockerfile header and re-pin on a schedule.

## Info

### IN-01: `--listen\b` also matches `--listen-address` and similar

**File:** `scripts/verify-k8s-invariants.py:128`
**Issue:** `\b` after `--listen` matches before a hyphen. The probe returned a hit for `args: [--listen-address=:9090]`, so a future exporter or proxy flag of that shape fails I12 against any k8s YAML or workflow, even though the target is not LikeC4. A false positive in a gate teaches people to bypass it.
**Fix:** Use `--listen(?![\w-])`, as in the WR-01 snippet, and add a clean selftest case.

### IN-02: A re-run of the whole workflow reuses the image tag with different bytes

**File:** `.github/workflows/architecture-site.yml:115-119,129`
**Issue:** The tag is `main-${RUN_NUMBER}-${GITHUB_SHA::7}`. `run_number` stays the same when "Re-run all jobs" starts a new attempt, and the workflow header says the builds are not byte-reproducible (lines 11-12). A full re-run therefore overwrites the registry tag with new content. Flux sees the same tag and does no bump, so a node restart or a pull could then fetch bytes nobody gated. "Re-run failed jobs" reuses the artifact and is safe. The flow was never run, so I could not test this. It follows from the GitHub run semantics, the tag expression and the header comment.
**Fix:** Pin the deployment by digest as the backup image does, or add `${{ github.run_attempt }}` to the tag and widen the ImagePolicy pattern and the `IMAGE_TAG_PATTERN` accordingly. The less invasive option is to note the limit in the header.

### IN-03: A pnpm setting that changes the build is outside the trigger paths

**File:** `.github/workflows/architecture-site.yml:27-32,35-39`
**Issue:** `docs/architecture/pnpm-workspace.yaml` (which sets `allowBuilds: esbuild: false`) changes what `pnpm install` runs but is not in either `paths:` list. A change to it alone starts no build or publish. The header (lines 7-9) justifies leaving out `sync.mjs` but says nothing about this file. Separately, `actions/checkout@v5` in the build job (line 55) keeps the read-only `GITHUB_TOKEN` in `.git/config` while the job runs pnpm lifecycle scripts and `likec4 build` from the dependency tree. That is low risk, since the token is `contents: read` and the job holds nothing else, but `persist-credentials: false` removes it.
**Fix:** Add `docs/architecture/pnpm-workspace.yaml` to both `paths:` lists. Add `with: persist-credentials: false` to the build job's checkout.

### IN-04: An unbounded `/tmp` emptyDir and a dead `add_header`

**File:** `k8s/base/architecture-site/architecture-site.yaml:68-70`, `docs/architecture/site/nginx.conf:35-38`
**Issue:**
- **emptyDir.** The `/tmp` emptyDir has no `sizeLimit`. `k8s/data/postgres/backup-cronjob.yaml:143-144` sets `sizeLimit: 1Gi` on its scratch volume, so this one deviates from a precedent in the repo. nginx writes only its pid and temp files here, so the realistic exposure is small, but it is unbounded node disk.
- **Dead `add_header`.** In `location /`, `try_files $uri /index.html` makes an internal redirect that lands in the extension-regex location, which sets its own `Cache-Control`. The `add_header` at line 37 therefore applies only to an existing extensionless file, and the site has none.
**Fix:** Add `sizeLimit: 16Mi` to the `tmp` volume. Remove the dead header line, or say in the comment why it is kept.

### IN-05: The nginx dotted-route limit is documented, not guarded

**File:** `docs/architecture/site/nginx.conf:9-10`
**Issue:** A view id with a dot and no trailing slash would answer 404 on a direct load. No check enforces "no view id has a dot". I checked the 21 current view ids with `rg` and none has one, but a later `dynamic view v1.2` would fail only in the browser.
**Fix:** In `sync.mjs check`, or the build job, reject a view id that matches `\.`. Alternatively, in the build job, curl one route per view against the built image.

### IN-06: The site is unauthenticated and live while the leak review is pending

**File:** `k8s/overlays/architecture/ingressroute.yaml:39-53`, `k8s/overlays/architecture/certificate.yaml:1-3`
**Issue:** This is not a defect in the diff. The site serves the full infrastructure model at a public hostname over an untrusted staging certificate, and the model includes the nonprod and monitoring hostnames (`model.c4:10-15`). The phase defers the leak review and the production certificate to a later commit. There is no HTTP-level access control in the meantime. I did not request the live site, nor the built bundle, so I cannot say what it contains.
**Fix:** Record the leak review as a blocking prerequisite for the production-certificate commit. The `letsencrypt-staging` value in `certificate.yaml` is a guard only against browser trust, not exposure.

---

_Reviewed: 2026-10-28_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
