# Pitfalls Research

**Domain:** extending a LikeC4 (1.60.0) architecture model to all five Kruchten views and publishing it as a static site, as a fourth public hostname on this repo's single-node k3s edge (Traefik, cert-manager, Let's Encrypt, DuckDNS), from a PUBLIC GitHub repo
**Milestone:** v1.5 "Architecture Views"
**Researched:** 2026-10-27
**Confidence:** MEDIUM-HIGH. Most system-specific findings below were measured on this repo's real model with the pinned likec4 1.60.0 (labelled MEASURED). Docs-derived facts are labelled DOCS. Anything I could not check is labelled UNVERIFIED.

## How to read the evidence labels

| Label | Meaning |
|---|---|
| MEASURED | I ran it on this repo's model or its build output on 2026-10-27. Reproduce with the command named. |
| SOURCE | Read in this repo's committed files or in the installed `node_modules/likec4` bundle. |
| DOCS | Stated by the cited official documentation page; not re-run here. |
| UNVERIFIED | Inference or third-party statement. Do not plan on it without a check. |

Phase labels used below are roadmap-agnostic. The roadmapper should map them onto real phase numbers:

- **Phase A, Model completion:** deployment, scenario and any further views.
- **Phase B, Honesty gates:** new drift checks, drills, hook self-test cases.
- **Phase C, Build and serve:** `likec4 build`, the static server, the image, its Flux wiring.
- **Phase D, Edge:** DuckDNS record, Certificate, IngressRoutes, uptime probe.
- **Phase E, Public-surface review and go-live:** leak scan of the built artifact, the staging-to-production flip, README link.

Suggested order is A, B, C, D (staging issuer only), E (leak review, then the production flip). The production certificate and any public link come last, so nothing is indexed before the review.

---

## Critical Pitfalls

### Pitfall 1: The dev server is exposed and serves arbitrary files

**What goes wrong:** `likec4 start` is a Vite dev server. On 1.60.0 it answers `GET /@fs/etc/hostname` with the real file bytes from outside the workspace, and answers a request carrying `Host: evil.example` with 200. Anyone who can reach its port can read any file the process user can read (`~/.ssh`, `.env`, kube config). MEASURED: bound to `127.0.0.1:5199` by default, `curl /@fs/etc/hostname` returned `DESKTOP-88EQU47`, identical to the file. `likec4 start --help` says `--allowed-host` "Defaults to allowing all hosts". It binds localhost by default, so the risk appears only when someone passes `--listen 0.0.0.0`, runs it in a container, port-forwards it, or puts it behind the new hostname "to get live reload".

**Why it happens:** the natural shortcut for "publish the site" is to run the thing that already works (`likec4.fish start`) on the VPS or in a pod. Two layers fail silently: no Host check, and the `/@fs` route is not restricted to the workspace.

**How to avoid:**
- Only the output of `likec4 build` is ever deployed: static files behind a static file server. Never `start`, never `preview` on the VPS (`preview` is a dev-grade server too).
- Add a gate that fails if any manifest under `k8s/`, any Dockerfile or any workflow runs `likec4 start`, passes `--listen`, or lists a likec4 binary as a container command. This is a text check in the style of `scripts/verify-compose-ports.py` and fits `scripts/verify-k8s-invariants.py`.
- Keep `likec4.fish start` as it is (localhost). Do not add a `--listen` option to the wrapper.
- Do not document "ssh -L" forwarding of the dev server in any file that feeds the site (see Pitfall 11).

**Warning signs:** a manifest with `command: ["likec4", ...]`; a port 5173 anywhere outside `likec4.fish`; a Service of type NodePort (already blocked by invariant I1).

**Phase to address:** C (gate and image design); E (re-check the artifact list).
**Confidence:** HIGH (measured).

---

### Pitfall 2: Deployed elements with no relationship vanish from the deployment view

**What goes wrong:** the existing view is `include netcup, netcup.k3s, netcup.**`. For `X.**`, LikeC4 only promotes descendants that end up connected to something visible; unconnected descendants stay implicit, i.e. not drawn (DOCS: likec4 `deploymentRefs.ts` as returned by context7; its own comment reads "only those that end up connected ... are promoted to explicit, the rest stay implicit"). MEASURED on this model: I added a system `docsite` with no relationships and an `instanceOf docsite` in a new namespace `kanban-docs`. The model export contained it (6 matches) but the computed `production` view had neither the namespace nor the instance. Adding `traefik -[sync]-> docsite 'HTTP :8080'` made both appear.

**Why it happens:** the model is the source of truth, the view is a projection, and nothing tells you a projection dropped something. `validate` passes (✓ Valid) and `sync.mjs check` passes, because check compares workload metadata with manifests, never view membership.

**Consequence for this milestone:** the static site is the fourth thing behind Traefik. If it is modelled as a deployed element but the `traefik -> site` relationship is forgotten, the published Physical view omits the site and nobody notices. Same for any future instance (cert-manager, monitoring, Flux).

**How to avoid:**
- Model the relationship `client -> traefik -> site` explicitly in `model.c4` (the logical edge is inherited by the instance).
- Add a `sync.mjs` rule: every deployment-model instance appears as a node in at least one deployment view (computed view nodes, same API `computedModel().views()` the viewpoint rule already uses). Prove it red with a drill (`drill.fish` D10: add an unconnected instance) and add a case to `scripts/pre-commit-likec4-selftest.py`.
- When an instance must appear without a relationship, `include` it explicitly by path.

**Warning signs:** `check` prints N instances in its tally but the PNG shows fewer boxes.

**Phase to address:** A (relationship and view), B (the gate and drill).
**Confidence:** HIGH (measured on 1.60.0; reproduce with a copy of `docs/architecture/model` plus one unconnected `instanceOf`).

---

### Pitfall 3: A new namespace-level fact is authored with no check, and the existing one is already wrong

**What goes wrong:** the deployment model puts Traefik in a namespace called `edge` (`deployment.c4` line 9, `instanceOf traefik`). `k8s/platform/namespaces.yaml` declares `kanban-prod`, `kanban-nonprod`, `kanban-data` and `monitoring` only; Traefik is the k3s-packaged chart in `kube-system` (`k8s/platform/traefik/helmchartconfig.yaml` line 20 `namespace: kube-system`; runbook "Task 3" names `kube-system/traefik`). `edge` is the name of a Flux Kustomization (`k8s/flux-system/edge.yaml`), not a namespace. The published Physical view prints "EDGE" as the namespace box (MEASURED render, `production.png`). The Traefik element also has no `workload` metadata, so `sync.mjs check` cannot notice.

**Why it happens:** `check` verifies workload names found in `k8s/base` and `k8s/data` (the `workloads()` function walks only those two directories and only `Deployment|StatefulSet|CronJob`) and `source` paths. It never reads namespaces, hostnames, ports or the Flux layout. Deployment nodes are fully authored.

**Consequence:** the new site gets a namespace box too. If it is authored from memory it repeats this class of error, now on a public page.

**How to avoid:**
- Fix `edge` now: rename the box to what is true (`kube-system`) or drop the namespace layer for Traefik.
- Add a namespace check: every `namespace` deployment node's title must be a `kind: Namespace` in `k8s/` (or on an explicit allow-list with a reason, e.g. `kube-system`). Cheap: `namespaces.yaml` plus an allow-list.
- Decide where the site's manifests live, then extend `workloads()` to that directory (see Pitfall 8) so the site's own `Deployment/<name>` is checked both ways.

**Phase to address:** A (fix the label while editing `deployment.c4`), B (namespace check).
**Confidence:** HIGH (SOURCE for both sides of the contradiction).

---

### Pitfall 4: Sequence and process scenarios become unreadably wide

**What goes wrong:** the scenario views use `variant sequence`. Each participant costs roughly 330 to 400 px of natural width. Recorded in the standard: `signin-ok` 1838 px with 5 participants; MEASURED here, a 4-participant view rendered 3162 px at 2x, so 1581 px natural. A scenario that follows the real write path (client, traefik, api, table, publisher executor, topic, consumer, table) has 7 to 8 participants, about 2600 to 3200 px natural (EXTRAPOLATED, not rendered). The old Mermaid legibility bound was 1117 px and the LikeC4 equivalent is still "undefined" in `docs/DIAGRAM_CONVENTIONS.md`.

**Related quirks:**
- DOCS (likec4.dev dynamic views): the sequence variant "supports only connections with leaf elements". MEASURED: one step ending on a non-leaf element (`postgres`, which has table children) validated and rendered as a participant. The case where a parent and its own child both take part in one view was NOT tested. Treat it as a risk until tried.
- DOCS: flow-control blocks (`alt`, `opt`, `loop`, `par`, `try`) are marked "Experimental ... syntax and rendering may still change", and nested `parallel` is not possible. The standard already bans branches inside one view (V1); keep it that way for the new scenarios.
- DOCS: participant order is derived from step order unless an `include a, b, c` list fixes it. A re-ordered step silently re-orders the lanes.
- DOCS: a step with no title takes its title from the model relationship (`api -> auth // title is derived from the model`). Editing a model label then changes scenario text with no diff in `views.c4`. Always give scenario steps their own title.
- DOCS/SOURCE: dynamic steps are not required to exist as model relationships, so a scenario step can describe an interaction the model does not contain, and nothing compares them.

**How to avoid:**
- Cap a scenario at 5 participants, the proven width. Split a long flow into two views and link them with `navigateTo` (DOCS: supported on dynamic steps).
- Define the legibility rule for the site once, in a browser at 1440 x 900 fit-to-view, record the number, and write it into the "Undefined" paragraph of the standard before building scenarios. The site viewer pans and zooms, so the PNG rule (1117 px) and the site rule differ.
- Give every step an explicit title; put the participant order in an `include` list.

**Warning signs:** a `png` render wider than the widest existing one (1838 px natural); a step title identical to a model relationship label.

**Phase to address:** A.
**Confidence:** MEDIUM-HIGH (widths measured, 7-8 participant figure extrapolated).

---

### Pitfall 5: Aggregated deployment edges lose their labels

**What goes wrong:** in a deployment view the edge between two instances merges all logical relationships between them, and when they carry different labels the edge is labelled `[...]`. MEASURED (`production` view of the current model, JSON and PNG): 4 of the 9 edges read `[...]` (prod and nonprod kanban to redpanda, prod and nonprod kanban to postgres). The standard requires every relationship to carry "a protocol-level fact", and these four carry none on the page.

**Why it happens:** DOCS (likec4.dev deployment views): `with { ... }` overrides do not work in deployment views ("In development": `include * -> * with { color: red } // does not work`), so you cannot relabel an inherited edge. Only a deployment-specific relationship (`a.b -> c.d 'title'` inside `deployment {}`) carries its own title.

**Consequence for this milestone:** adding the site and its edges, or cert-manager and Flux, adds more merged edges. Kind styling (solid, dashed, dotted) of a merged edge is UNVERIFIED when the merged relationships have different kinds (sync plus async).

**How to avoid:**
- Decide per edge: either define a deployment-level relationship with its own title (`traefik -> kanban 'HTTP :8080'` already works because it is a single relationship), or split the logical relationship so each pair has one.
- Check the final PNG for `[...]` and make zero a Phase A acceptance criterion.
- Do not rely on `fk` and `imports` being told apart by line style: both are dotted (`spec.c4`). They differ only by head (diamond vs vee) and colour, so never place both kinds in one view.

**Phase to address:** A.
**Confidence:** HIGH (measured).

---

### Pitfall 6: `likec4 build` assumptions break at the first non-root or non-fallback server

**What goes wrong (MEASURED on a fresh `likec4 build -o` of this model, and SOURCE in the installed CLI):**
- 22 files, 6.0 MB, no `.map` files (no sourcemap exposure by default).
- `index.html` references `/assets/...` as absolute paths: the base is baked at build time (DOCS: "The resulting website is strictly bound to the given base path"). It works at a hostname root, which is the plan. It breaks if anyone serves it under a path (for example behind a `PathPrefix(`/architecture`)` route on an existing host) unless built with `--base`.
- The router is path-based with `trailingSlash: 'always'` (bundle). A deep link such as `/view/signin-ok/` is not a file. A static server with no fallback answers 404. The CLI copies `index.html` to `404.html` (SOURCE: `dist/cli/index.mjs`), which only helps hosts that serve `404.html` for misses (GitHub Pages style). `--use-hash-history` removes the need for a fallback (DOCS).
- The page title is `default` (the project name) on the root view (MEASURED); use `--title`.
- A bundled `robots.txt` says `Disallow: /` (SOURCE: `node_modules/likec4/__app__/robots.txt`, copied into the build). That is a hint to crawlers, not access control. It will also stop an intended public portfolio page from being indexed; make that an explicit decision.
- `likec4-views.js` sits at the site root with an unhashed name (2.7 MB). Every other chunk is content-hashed.
- DOCS (cli.mdx): `--use-dot` is "false by default, true when inside container". Building inside a Docker stage switches layout from bundled WASM Graphviz to a system `dot` binary: a missing `dot` fails the build, and a different `dot` version can lay out differently. UNVERIFIED which; test in the actual build container.

**Why it happens:** the build works on the first try locally, where the dev server and a default static server hide each assumption.

**How to avoid (static server and cache policy):**
- Serve at the hostname root, pass `--base /` explicitly in `likec4.fish site`, and write down that a path prefix is not supported.
- Pick one routing strategy and test both a cold deep link and a refresh on it: either a fallback for extensionless paths only (`try_files $uri $uri/ /index.html`, with a real 404 for any path that has a file extension) or `--use-hash-history`. Do not apply a blanket catch-all, or a stale hashed chunk name after a redeploy returns `index.html` with status 200 as JavaScript and the open tab fails with a MIME error instead of a clean 404 (UNVERIFIED for this app; general Vite behaviour).
- Cache headers: `/assets/*` immutable, one year (hashed names). `index.html`, `404.html`, `likec4-views.js`, `robots.txt`, `favicon.ico`: `no-cache`. A stale `likec4-views.js` next to a new `index.html` is a real mix-up risk because of its unhashed name.
- Compress with the static server, not with a Traefik `compress` middleware: Traefik has a measured 64Mi limit for the whole edge (Pitfall 9) and the site's JavaScript files (1.2 MB and 2.7 MB uncompressed on disk) are far larger than any API response it carries today.
- Make `likec4.fish site` write to a directory in `.gitignore` (already `out/`); the build output must never be committed.

**Warning signs:** 404 on reload of a view URL; blank page with a MIME-type console error after a deploy; the browser shows `LikeC4` or `default` as the tab title.

**Phase to address:** C.
**Confidence:** HIGH for the measured facts, MEDIUM for the container `--use-dot` effect.

---

### Pitfall 7: Content-Security-Policy written from guesswork either breaks the site or is never added

**What goes wrong:** with no CSP the site has no policy at all; with a copy-pasted strict one it renders blank. MEASURED (headless Chromium via the pinned Playwright 1.64.0, cold load of `/` and `/view/signin-ok/`, 8 requests each, all same-origin, zero third-party hosts, zero failures):
- This policy loads cleanly, zero violations: `default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'`.
- The same policy with `style-src 'self'` produces inline-style violations (the UI injects style attributes). `'unsafe-inline'` for styles is required.
- The index has no inline script, and the bundle contains no `WebAssembly`, `new Worker`, `eval(` or `new Function(`, so `script-src 'self'` is enough.
- Fonts are inlined as data URIs in `fonts-*.css`, hence `font-src data:`.

**Not covered (UNVERIFIED):** interactive features. The bundle contains strings for `app.diagrams.net`, `kroki.io`, `icons.like-c4.dev`, `cdn.jsdelivr.net`, `bit.ly` and `github.com`. None was requested on cold load, but export menus, search, details dialogs and the sequence walkthrough were not exercised. A strict `connect-src 'self'` may disable export-to-draw.io or Kroki; that is acceptable, but it must be a decision.

**How to avoid:**
- Ship the measured policy as the starting point through a Traefik `headers` Middleware or the static server, first as `Content-Security-Policy-Report-Only`, then walk every view and every menu in a headless browser, then enforce.
- Also send `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `frame-ancestors 'none'` (embedding is not planned).
- HSTS on a `duckdns.org` host: `max-age` only; never `includeSubDomains` or `preload` (the PSL lists `duckdns.org` as a public suffix, MEASURED against publicsuffix.org, so those flags are meaningless or harmful for a shared suffix).

**Phase to address:** C (policy and headers), E (interactive walk before enforcing).
**Confidence:** HIGH for the cold-load result, LOW-MEDIUM for interactive features.

---

### Pitfall 8: The fourth hostname repeats the placeholder-route race that already took Grafana down twice

**What goes wrong:** `docs/INFRA_RUNBOOK.md` ("A pre-existing, unrelated bug surfaced by this session", resolved 2026-10-24) records that `k8s/platform/edge` carried a placeholder IngressRoute `grafana` so the certificate could validate before the real service existed, while `k8s/monitoring/configs/ingressroute.yaml` defined the real one. Two Flux Kustomizations rendered the same object, the last applier won on every reconcile, and the public dashboards answered 503 twice. The fix, and invariant I11 in `scripts/verify-k8s-invariants.py`, leave exactly one owner per object.

**Why it happens:** the certificate has to exist before the first HTTPS request, so the hostname's Certificate is tempting to place in the always-ready `edge` Kustomization (`dependsOn: cert-manager`), with its route placed next to it "for now".

**How to avoid:**
- Follow the post-fix pattern: the Certificate and nothing else for the hostname may live in `k8s/platform/edge` (like `certificate-monitoring.yaml`); the namespace, Deployment, Service, Middlewares and both IngressRoutes live in the site's own Kustomization root. Never a placeholder route.
- Accepted consequence, already documented: until the site's Kustomization applies, the hostname serves Traefik's default certificate while HTTP-01 issuance works through cert-manager's own solver Ingress.
- A new Kustomization root must be rendered by the gate (I7 fails for an unrendered root with a `kustomization.yaml`), and if it sets `targetNamespace`, `namePrefix`, `nameSuffix`, `patches`, `components` or `postBuild`, I11 fails closed. Prefer a namespace declared in the manifests over `targetNamespace`.
- The runtime proof is the existing live command pattern: `k3s kubectl get ingressroute -n <ns> <name> -o jsonpath='{.metadata.labels}'` names one Kustomization.

**Warning signs:** `no available server` 503 on the new hostname; an `I11` FAIL line; two Kustomizations listing the same object in their inventories.

**Phase to address:** D.
**Confidence:** HIGH (SOURCE: runbook and invariant docstring).

---

### Pitfall 9: A new workload fails the existing gates, or quietly changes the edge budget

**What goes wrong:** the existing gate `scripts/verify-k8s-invariants.py` will reject, in CI and not in the pre-commit hook, anything that was reasoned for the app only:
- I3: every container needs both a memory request and limit.
- I4 with `--no-provisional` (CI runs it so): a `PROVISIONAL` memory comment fails; only `MEASURED` passes. A static server needs its own restart-ladder measurement (the repo method: halve until OOM, adopt with headroom, re-verify from a fresh recreate). The host is not roomy: after the full 13-09 ladder `free -m` showed 2286 MB available of 7945 MB, and requests sum to 3.33 GiB.
- I5: no `:latest` and no untagged image, for any container. A new image repository also needs its own Flux image policy if tags are to be automated; the `main-N-sha7` tag-pattern check covers the app image only.
- I8: no committed `kind: Secret`. I1: no NodePort/LoadBalancer. I2: no hostPort, hostPath or hostNetwork.
- I10: the web-entrypoint redirect route must fullmatch ``Host(`<host>`) && !PathPrefix(`/.well-known/acme-challenge/`)``. Copy the `app-http` route from `k8s/overlays/prod/ingressroute.yaml` byte for byte and change only the host.
- Middlewares are namespaced and cross-namespace references are not used in this repo (`redirect-https` is defined once per namespace in prod, nonprod and monitoring). A site in its own namespace needs its own `redirect-https`.

**The Traefik edge has a measured 64Mi limit** (32Mi request; `helmchartconfig.yaml`, restart ladder failed at 16Mi), sized for API traffic. A 6 MB site is a different traffic shape. Cold load measured at 8 requests (transferred bytes were not measured; on disk `main-*.js` is 1.24 MB and `likec4-views.js` is 2.7 MB, and whether the cold load fetches the latter is unknown). Re-measure Traefik under N concurrent cold loads before and after (RestartCount, dmesg), using the repo's own ladder method, not an assumption.

**Do not copy `general-rate-limit` to the site by default.** Its bucket (burst 60, 60 per minute, per /56 client) is sized for API clients. At 8 requests per cold load the budget is not the problem today, so an unlimited route is simpler, but a shared office NAT is one bucket; decide on purpose, derive it in a comment like the existing Middlewares, and test with the existing `verify-rate-limit` pattern if one is added.

**Phase to address:** C (image, memory ladder), D (route and Middlewares).
**Confidence:** HIGH (SOURCE for invariants and limits); MEDIUM for the Traefik load effect (UNVERIFIED, not measured).

---

### Pitfall 10: ACME issuance for the new hostname: staging-first, DNS-first, and a NodePort side effect

**Rate limits (DOCS: letsencrypt.org/docs/rate-limits, updated 2026-08-05):** 5 certificates per exact identifier set per 7 days (no override); 5 authorization failures per identifier per account per hour; 300 new orders per account per 3 hours; 50 certificates per registered domain per week. MEASURED against the Public Suffix List: `duckdns.org` is itself on the PSL, so each DuckDNS subdomain is its own registered domain, and the 50-per-week limit is not shared with other DuckDNS users. The 5-per-week per exact name is the limit that bites.

**What goes wrong, in the order it usually happens:**
1. The Certificate is committed on `letsencrypt-production` directly. The header of `k8s/platform/edge/clusterissuers.yaml` says every Certificate starts on staging, but the two committed Certificates (`app`, `grafana`) are both already on production, so a copy-paste starts at the flipped state.
2. The DuckDNS A record does not exist yet (or points elsewhere), so HTTP-01 fails and each retry spends one of 5 failures per hour.
3. The flip from staging to production changes `issuerRef`. DOCS (cert-manager FAQ, "When do certs get re-issued"): a changed issuer annotation on the Secret triggers re-issuance, so the flip works in place with the same `secretName`, and it spends a new production order. Deleting the Secret or the namespace to "start clean" does the same and is not recommended by the docs; use `cmctl renew` sparingly.
4. While on staging, browsers and curl fail the TLS check; an uptime probe added too early opens a GitHub issue.

**Precise precondition order:** DuckDNS record exists and resolves to `159.195.114.230` on a public resolver (the runbook's DNS section used `8.8.8.8`) -> commit the Certificate on `letsencrypt-staging` -> `Ready` with the staging chain -> separate commit flipping to `letsencrypt-production` -> `Ready` -> only then the uptime probe and any public link. Do not add an `AAAA` record: DuckDNS cannot detect IPv6 (DOCS: duckdns.org FAQ), and `KANBAN-INGRESS` is IPv4-only, so a manual AAAA would send Let's Encrypt validation over a path with no host-level filter and an unproven route.

**Transient NodePort (DOCS, cert-manager HTTP-01 page, v1.21.1 is pinned in `k8s/platform/cert-manager/cert-manager.yaml`):** the solver Service defaults to type `NodePort` ("By default, type `NodePort` will be used ... Normally there's no need to change this"), and neither ClusterIssuer sets `serviceType`. So every issuance and every renewal creates a short-lived NodePort Service in the Certificate's namespace. Neither invariant I1 (rendered manifests only) nor the runtime inventory ("the only non-ClusterIP Service in the cluster is `kube-system/traefik`", recorded 2026-09-27) sees it. Traefik reaches the solver through the Ingress either way, so `serviceType: ClusterIP` on both solvers loses nothing. This is a change to the shared ClusterIssuers; make it deliberately and once, before the new Certificate. It also matters because the `KANBAN-INGRESS` DROP counter after Docker's removal is still listed as unproven in PROJECT.md, so the filter is not yet an evidenced backstop for a stray NodePort.

**Firewall:** a fourth hostname on the same IP needs no firewall change (`KANBAN-INGRESS` and the Netcup Cloud Firewall already allow NEW TCP 22/80/443). The pitfall is the opposite one: a site served on any other port (5173, 8080, a NodePort) is dropped, and the tempting "temporary console rule" for testing is a Netcup-side, out-of-repo change. If one is ever opened, record its removal as an explicit step, as the runbook did for the 30104 probe.

**DuckDNS account limit:** third-party documentation says a free account holds 5 subdomains (Home Assistant add-on README, dehydrated-duckdns listing); I found no official statement (LOW-MEDIUM). The repo already holds 3 (`kanban-board-rud-vlad-473`, `-nonprod`, `-monitoring`), so the fourth is fine and the fifth is the last. Check the account page before naming a fifth.

**Phase to address:** D.
**Confidence:** HIGH for the ACME and cert-manager facts (DOCS), MEDIUM for the DuckDNS cap.

---

### Pitfall 11: The published site exposes more than the diagrams show

**What goes wrong:** `likec4 build` ships the whole model as JavaScript. MEASURED: `PGHOST postgres.kanban-data` and `k8s/platform/traefik` (a metadata `source` value on an element that appears in no view's metadata display) are present in `likec4-views.js` and `assets/model-*.js`; so are the description strings (`KANBAN-INGRESS`, `Netcup`). Scoping a view with `include` does not hide an element; element search, details panels and direct view URLs read the model, not the view. The repo is already public, so these strings are not secrets, but the site changes who sees them: a search-friendly, indexed-by-CT hostname with a polished summary. Certificate Transparency publishes the hostname as soon as the production certificate issues, regardless of `robots.txt` (general knowledge, HIGH).

**What is already in the public repo and must not be amplified into the model or site (SOURCE: `docs/INFRA_RUNBOOK.md`):** the VPS public IPv4 and IPv6 prefix, the SSH key file path and `Host netcup-prod`, the operator workstation IP and a CI runner /24 used as probe sources, the Netcup console temporary-rule source IP, in-cluster DNS names, Secret names (`monitoring/grafana-admin`), Grafana public-dashboard addresses. The SSH-forwarding and dev-server notes the brief mentions belong in that group.

**How to avoid:**
- Write each description fresh, one sentence, about what the thing is. Never paste a runbook or history paragraph into a `.c4` file.
- Define an allow-list of fact classes the site may state (kind, name, protocol and port class, namespace, the existence of a filter) and a deny-list (any IP literal, hostname other than the three already linked from the README, key paths, Secret names, user names, `ssh`, `kubectl`, tokens).
- Scan the artifact, not the source: a CI step greps the built directory (JS, HTML, any file) for IPv4/IPv6 literals, `BEGIN .* PRIVATE`, `authorized_keys`, `id_ed25519`, `ssh `, `kubectl`, `pass show`, `secret`, and the deny-list. The source scan cannot see strings that sync.mjs generates from `metadata`. The existing pre-commit gitleaks looks for credential shapes, not for an internal hostname, so it will not catch these.
- Replace `PGHOST postgres.kanban-data` with the protocol-level fact (`SQL over TCP`) unless the in-cluster name is deliberately public.
- Keep Grafana public-share URLs out of `link` properties.
- Treat the site as public from the first production certificate; the review (Phase E) happens before the flip.
- `robots.txt` stays `Disallow: /` unless indexing is wanted; either way it is not an access control and the doc must not call it one.

**Warning signs:** an IP literal or a path starting `/home/`, `~/.ssh` or `/opt/deploy` in the built artifact; a description longer than two sentences.

**Phase to address:** A (writing rule), E (artifact scan and review before the flip).
**Confidence:** HIGH (measured bundle contents), MEDIUM on what counts as sensitive (a judgement for the repo owner).

---

### Pitfall 12: The model drifts again, now with more authored facts and a published copy

**What goes wrong, with evidence already in the current model:**
- **Unchecked enumerations.** `kanban.api -[sync]-> postgres 'JDBC: users, boards, columns, tasks, subtasks, spring_session, spring_session_attributes'` (`model.c4` line 94) is a hand list of tables. `generated.c4` has 6 generated tables including `activity_log`, which the list omits, and PROJECT.md records `GET /boards/{boardId}/activity` as a read endpoint. The next migration adds a table and the label stays. (The PROJECT.md reading is a document claim; the Java call was not traced.)
- **Unchecked numbers.** `Postgres 16` (element title; `k8s/data/postgres/postgres.yaml` has `image: postgres:16`), `x86_64` (title, description, technology), `:443` and `:8080` in labels, `401/403/400/409` in the API description. The standard's "Numbers in labels" rule says each is covered by a check or left out; none is covered. A one-line comparison check each (image tag, `Service` port, deploy platform) turns them into `checked` facts; otherwise delete the number.
- **The workload check does not reach the new site.** `workloads()` walks only `k8s/base` and `k8s/data`. A `Deployment/<site>` elsewhere is not compared; and a model element carrying `workload 'Deployment/<site>'` produces "model claims workload ... no such manifest", which tempts someone to delete the metadata rather than extend the walker. Extend the walker to the site directory in the same change that adds the element.
- **Hook trigger gap, by design.** The pre-commit trigger list is `src/main/resources/db/migration`, `k8s/base`, `k8s/data`, `docs/architecture`, `KafkaTopics.java`. An edit to `k8s/platform/**` or to the site's own manifests passes the hook and is caught only by CI (`likec4-model-drift`). Acceptable, but the CI job must stay required.
- **View membership is unchecked** (Pitfall 2) and **namespaces are unchecked** (Pitfall 3).
- **Scenario steps are authored.** Each new scenario multiplies unchecked claims (a SQL action per step). The `if9` work verified the signin steps against code once and softened what it could not; repeat that per new scenario, cite the code line per step in the evidence file, and prefer the repo's query-count tests (`AbstractAppTest.countQueries`) as the source for "N statements" claims; otherwise leave the count out.
- **Published copy staleness.** Nothing ties the live site to `main`. A site rebuilt only on a manual command can show last month's model next to this month's code.
- **Version coupling.** All the measured behaviour here (edge labels `[...]`, `--seq` flag, sequence widths, layout) is for 1.60.0, released 2026-10-15 (GitHub releases). 1.52.0 turned implicit views off by default (`implicitViews`), 1.59.4 and 1.60.0 changed dynamic-view and deployment behaviour. An upgrade changes layout and computed edges; the lockfile and `--frozen-lockfile` in CI hold the pin. Upgrade as its own change with re-measurement.
- **Metadata keys are still not declared by the DSL.** A typo key validates silently (V2 in the standard). The upstream request "Metadata schema and validation" (likec4/likec4 issue 3456, listed as open on the issues page; I did not open the issue) shows it is still missing. The drift script owning the vocabulary stays necessary.

**How to avoid:**
- Move facts up the provenance ladder, or delete them. For every number, enumeration and name added in Phase A, record its provenance (`generated`, `checked`, `authored`) in the commit and keep the authored count visible in the `check` tally line.
- Stamp the build: `--title 'Kanban architecture (<short sha>)'` or a text line in the index description written by the build script, and make the publish step compare the stamp with `git rev-parse HEAD` of the model directory's last change.
- Publish from the same workflow that runs `likec4-model-drift`, with `needs:`, so a drifting model never publishes. `deploy.yml` triggers on push to `main` only and cannot take a `pull_request:` trigger (its own header), so it cannot be the gate; a site built there must depend on the drift job's result or re-run `sync.mjs check` itself.
- Prove every new check red then green with a drill and a hook self-test case, as the existing 12 drills and 15 self-test cases were; keep D5 (renamed controller class) documented as the negative control.
- Never wire `likec4 build` or `png` into the pre-commit hook: it is already about 4 minutes, and PNG export took 22 s for 9 views here (MEASURED), needs Chromium and is not run in CI today.

**Phase to address:** B (checks, drills, self-test), C (stamp, needs-gating).
**Confidence:** HIGH (SOURCE for every drift hole cited).

---

## Moderate Pitfalls

### Pitfall 13: The development view does not read at natural size, and more nodes make deployment worse

**What goes wrong:** `development` is 4475 x 2636 natural (14 packages, 38 imports edges; recorded), judged only from downscaled images, with edges told apart "only by their arrowheads". `production` is 1965 x 1241 natural with 4 namespaces; the rendered image (MEASURED) shows the prod-kanban to Postgres edge curving across the `kanban-nonprod` box, which reads as if it passed through that namespace. A fifth and sixth namespace (the site, and cert-manager or monitoring if added) multiplies these crossings.
**Prevention:** define the site legibility rule first (Pitfall 4); try layers or grouping for `development` (explicitly "not tried" in the standard) instead of more layouts; try `routing ortho` (new in 1.60.0, DOCS release notes, unmeasured here) on `production` only after the rule exists; order namespaces so the shared one (`kanban-data`) sits between prod and nonprod; use `rank` constraints sparingly, since 1.60.0 itself fixed "rank constraints having no effect when changed in an existing view" (issue 3211).
**Phase:** A. **Confidence:** MEDIUM.

### Pitfall 14: View sprawl, folders and `include` patterns

**What goes wrong:** every new view needs one viewpoint tag, or `sync.mjs check` refuses it; that is intended, but it also fires on any auto-generated view. `implicitViews` has been `false` by default since 1.52.0 (DOCS: likec4 PR 2705); enabling it would generate views with no tag (UNVERIFIED that check would flag them all). A `/` in a view or element title creates a sidebar folder (CHANGELOG, "Escaping `/` in Titles", fix 3215: write `\/` to avoid it). `include kanban.* where kind is component` on `development` will also pick up any hand-written `component` added later, not only the generated `pkg_*` ones.
**Prevention:** keep `implicitViews` unset; no `/` in titles (or escape it); give new generated vs authored components different kinds or a tag, and add `where tag is #generated` to the include if authored components are ever added.
**Phase:** A. **Confidence:** MEDIUM.

### Pitfall 15: The uptime probe cannot see a broken site behind a fallback

**What goes wrong:** with an SPA fallback every extensionless URL returns 200. A probe of `/` passes against a stale or wrong deploy. The existing `uptime-check.yml` probes a body marker for Grafana shares for exactly this reason (status table in the runbook: "200 without the expected marker").
**Prevention:** probe `/` for a build marker string and one hashed asset URL taken from the live `index.html` (200 with a JavaScript content type); add it only after the production flip (Pitfall 10), as one more entry, not a new workflow. Scheduled runs here arrive hours apart (2.6 to 8.2 h measured), so it detects, it does not page.
**Phase:** D. **Confidence:** HIGH.

### Pitfall 16: A ConfigMap, a git-clone init container or a node-local build looks like the cheapest way to ship 6 MB

**What goes wrong:** a Kubernetes ConfigMap holds at most 1 MiB (general Kubernetes limit, HIGH), and the build is 6.0 MB; an init container that clones the repo and builds needs Node, pnpm and egress on a host with about 2.3 GB available; a build on the VM breaks the repo's rule that CI builds and Flux pulls.
**Prevention:** build in CI from the committed lockfile, bake the output into a small static-server image with a pinned tag, push it, let Flux roll it; or serve from a GitHub-hosted artifact only if the hostname requirement is dropped. Because the image is the unit of rollback, tag it with the model commit.
**Phase:** C. **Confidence:** MEDIUM (design guidance).

### Pitfall 17: Rendering PNGs for docs and publishing the site are two pipelines that can disagree

**What goes wrong:** `png` runs with `--seq` and needs Playwright's Chromium (an earlier likec4 Docker image failed on a Chromium revision mismatch, PR 3032); `site` does neither. A diagram embedded in a doc as PNG can differ from the site's live view of the same model.
**Prevention:** keep the PNGs for the docs that cite them, regenerate them from the same commit as the site build (one script), and never edit one without the other; record the pinned likec4 version in the PNG evidence file.
**Phase:** C. **Confidence:** MEDIUM.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Paste a runbook paragraph into a `description` | Quick, rich text | Leaks operator IPs and key paths into an indexed site; goes stale unchecked | Never |
| Production issuer on the first commit | One step less | One bad DNS or route burns the 5 failures per hour and the 5 per week budget | Never |
| `PROVISIONAL` memory comment on the site container | Passes locally | CI runs `--no-provisional`, so the PR is red; the real fix is a restart ladder | Never (CI refuses it) |
| Number or table list written into a label | Reads well | Stale on the next migration, no check | Only with a check or a dated "authored" note |
| Blanket `try_files ... /index.html` | Deep links work | Stale-chunk MIME errors; 200 for every typo; masks probe failures | Never; limit the fallback to extensionless paths |
| Placeholder IngressRoute in `platform/edge` | Hostname answers early | The exact 503 race of 2026-09-28 and 2026-10-24 | Never (invariant I11) |
| Skip the `[...]` check on edges | Faster Phase A | Published diagram with 4+ unlabelled edges | Only until Phase A acceptance |
| Serve through `likec4 start` "just for now" | Live reload | Arbitrary file read (Pitfall 1) | Never off localhost |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| cert-manager | Same `secretName` reused across staging and production without checking the issuer annotation changed | Flip `issuerRef` in its own commit; confirm re-issue (`Ready`, issuer name on the Secret) before moving on |
| cert-manager | Leaving the solver `serviceType` unset | Set `ClusterIP` on both ClusterIssuers once |
| Traefik IngressRoute | Referencing a Middleware from another namespace | One `redirect-https` per namespace, as prod, nonprod and monitoring do |
| Traefik IngressRoute | Listing `web` and `websecure` in one object | Two objects, as the existing routes do; the `web` route must fullmatch the I10 rule |
| Flux | A second Kustomization rendering the same object | One owner per identity (I11); Certificate in `edge`, everything else in the site root |
| DuckDNS | Creating the Certificate before the A record resolves | Resolve on a public resolver first; no AAAA record |
| GitHub Actions | Building the site in `deploy.yml` with no dependency on the drift job | Same workflow as `likec4-model-drift`, or `needs:` it |
| LikeC4 CLI | Building inside a container and assuming WASM layout | Test the container build; `--use-dot` defaults to true there (DOCS) |
| LikeC4 export | Expecting a sequence PNG without `--seq` | Keep the `--seq` flag in `likec4.fish png` (V1) |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| 6 MB site through a 64Mi Traefik | Edge restarts under concurrent loads (UNVERIFIED) | Serve compression from the static server; measure Traefik RestartCount and dmesg with N parallel cold loads | When several visitors load cold at once; threshold not measured |
| Merged-edge label `[...]` | Unlabelled arrows | Deployment-level relationships with own titles | Every view with 2+ relationships between the same pair |
| Sequence width per participant | Horizontal scroll, tiny text | 5 participants per scenario | Beyond about 5 (1838 px natural at 5) |
| 22 s of Chromium for 9 PNGs | Slow doc builds | Keep PNG out of hooks and CI gates | About 2.5 s per view, linear in view count |
| Unhashed `likec4-views.js` (2.7 MB) cached forever | Old model next to a new index | `no-cache` for it | Every redeploy |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Dev server reachable (`--listen`, forward, container) | Arbitrary file read, DNS-rebinding (measured) | Deploy build output only; gate on `likec4 start` in manifests |
| Internal detail in model descriptions or metadata | IPs, key paths, Secret names on an indexed site | Fresh one-line descriptions; artifact scan with a deny-list |
| Treating `robots.txt` as protection | Page is public regardless; hostname is in CT logs | Say so in docs; review before the production flip |
| CSP omitted, or strict `style-src 'self'` | No policy, or a blank page | Measured policy in Pitfall 7, Report-Only first |
| HSTS `includeSubDomains` or `preload` on `duckdns.org` | Affects a shared public suffix | `max-age` only |
| Console firewall rule left open after a probe | Unversioned exposure | Record removal as a plan step |
| Transient NodePort from the HTTP-01 solver | A non-ClusterIP Service during every issuance and renewal | `serviceType: ClusterIP` |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Tab title `default` | Looks unfinished | `--title` |
| Opening on `index` with no way to find the four other viewpoints | Visitor sees one diagram | Link the five viewpoints from the `index` view (`navigateTo`) |
| Deep link 404 | Shared link dead | Test a cold deep link and refresh |
| Scenario views wider than the screen | Unreadable on a laptop | 5 participants, rule from Pitfall 4 |
| Staging certificate left in place | Browser warning | Flip, then verify with a trusted client |

## "Looks Done But Isn't" Checklist

- [ ] **Deployment view:** every instance in `deployment.c4` is a node in some deployment view. Verify by counting nodes in the computed view, not by looking at the PNG.
- [ ] **Edge labels:** zero `[...]` on any published view. Verify in the final PNG and the exported JSON.
- [ ] **Namespace boxes:** each box title is a real `Namespace` in the cluster (or `kube-system`). Verify against `k8s/platform/namespaces.yaml`.
- [ ] **Site routing:** a cold deep link and a refresh on `/view/<id>/` both render; a missing `/assets/x.js` returns a real 404.
- [ ] **Cache headers:** `curl -I` of `/`, `/likec4-views.js`, `/assets/<hashed>.js` shows `no-cache`, `no-cache`, immutable.
- [ ] **CSP:** zero violations walking every view and every menu, in a headless browser.
- [ ] **Artifact scan:** no IP literal, key path, `ssh`, `kubectl`, Secret name or deny-list hostname in the built directory.
- [ ] **Certificate:** staging `Ready` first, production `Ready` after a separate commit; browser trusts it; no AAAA record.
- [ ] **Solver Service:** during an issuance, `kubectl get svc -n <ns>` shows ClusterIP.
- [ ] **Route ownership:** the new IngressRoute's `kustomize.toolkit.fluxcd.io/name` label names exactly one Kustomization.
- [ ] **Gates:** the invariant script and the drift check are green in CI, each new check was red first (drill output kept), and the hook self-test case count was raised.
- [ ] **Staleness:** the live site shows the build stamp of the commit that last changed `docs/architecture/model`.
- [ ] **Edge budget:** Traefik RestartCount unchanged after concurrent cold loads.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Dev server exposed | HIGH | Take it down at once; treat every file readable by that user as disclosed; rotate the SSH key and any `.env` secrets (`pass`, deploy key); review shell history |
| Leak found in the live site | MEDIUM | Redeploy the cleaned build; the old text stays in caches and in any crawler copy; rotate anything credential-like |
| Rate limit hit | MEDIUM | Wait (token bucket; no reset); run on staging while waiting; 5 per exact set refills 1 per 34 hours |
| Two Kustomizations own a route | LOW | Remove the duplicate; confirm the owner label; I11 prevents recurrence |
| Stale chunk errors after a redeploy | LOW | Hard refresh; fix the fallback to 404 for file-extension paths; keep the previous image's assets briefly |
| Drifted model published | LOW | Rebuild from `main`; add the missing check with a drill |
| Traefik OOM from the new traffic | MEDIUM | Raise the measured limit via a new ladder; move compression to the static server |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 Dev server exposed | C, E | Gate fails a manifest with `likec4 start`; `ss -ltn` on the host shows no 5173 |
| 2 Unconnected instances vanish | A, B | New drill D10 red then green; node-count check |
| 3 Namespace label wrong, unchecked | A, B | Namespace check red on `edge`, green after the fix |
| 4 Scenario width, sequence quirks | A | Widest natural width recorded; participants at most 5 |
| 5 `[...]` merged-edge labels | A | Zero `[...]` in the final render |
| 6 Build, base, routing, cache | C | Deep link, refresh, header and 404 tests on the real hostname |
| 7 CSP | C, E | Report-Only walk with zero reports, then enforced |
| 8 Placeholder-route race | D | I11 green; ownership label |
| 9 Gates and edge budget | C, D | CI green with `--no-provisional`; Traefik ladder |
| 10 ACME order, NodePort | D | Staging `Ready`, then production `Ready`; solver Service type |
| 11 Leakage | A, E | Artifact scan job red on a planted IP, then green |
| 12 Drift and staleness | B, C | Drill outputs, self-test count, build stamp equals commit |
| 13 Readability | A | Rule written; judged in a browser |
| 14 View sprawl | A | `check` tally and view list reviewed |
| 15 Probe blind spot | D | Probe fails against a deliberately wrong deploy (drill) |
| 16 Delivery shortcut | C | Image built in CI, pinned tag, no ConfigMap |
| 17 Two render pipelines | C | One script, one commit for PNG and site |

## Research flags for the roadmap

- **Needs its own phase-level research or a spike:** Pitfall 7 interactive CSP walk (UNVERIFIED), Pitfall 6 `--use-dot` in the build container, Pitfall 9 Traefik load effect, Pitfall 4 parent-plus-child participants in one sequence view, Pitfall 5 merged edges of mixed kinds.
- **Standard patterns, low research risk:** Pitfall 8 (a worked precedent exists), Pitfall 10 order of operations, Pitfall 12 check additions (the drill and self-test machinery exists).
- **Not covered by this research:** the live cluster state (no SSH was used, by design), Flux image automation for a new image repository, the Netcup Cloud Firewall's live ruleset (outside version control per the runbook), and a Grafana, cert-manager or Flux node in the Physical view.

## Sources

Measured on this repo, 2026-10-27, likec4 1.60.0 (Playwright 1.64.0, Node 24):
- `likec4 export json --skip-layout` of `docs/architecture/model`, plus three scratch copies with an unconnected instance, a connected instance, and a non-leaf sequence step (scratch deleted after the run).
- `likec4 export png --seq` render of `production` and a non-leaf sequence view.
- `likec4 build` output: file list, size, absence of `.map`, outbound hosts, bundle strings; headless Chromium cold-load capture and two CSP policies.
- `likec4 start --port` on loopback: bind address, `/@fs` file read, foreign `Host` header.
- `curl https://publicsuffix.org/list/public_suffix_list.dat` (the `duckdns.org` entry).

Repository files read: `.planning/PROJECT.md`, `docs/DIAGRAM_CONVENTIONS.md` (LikeC4 standard), `docs/INFRA_RUNBOOK.md`, `docs/architecture/{sync.mjs,likec4.fish,package.json,model/*.c4}`, `k8s/overlays/{prod,nonprod}/ingressroute.yaml`, `k8s/overlays/prod/certificate.yaml`, `k8s/platform/edge/*`, `k8s/platform/traefik/helmchartconfig.yaml`, `k8s/platform/cert-manager/cert-manager.yaml`, `k8s/monitoring/configs/ingressroute.yaml`, `scripts/verify-k8s-invariants.py` (docstring and I10/I11), `.githooks/pre-commit` (LikeC4 step), `.github/workflows/invariant-checks.yml` (header).

External documentation:
- LikeC4 static website guide: https://likec4.dev/guides/static-website/ (HIGH)
- LikeC4 deployment views: https://likec4.dev/dsl/deployment/views/ (HIGH); deployment model and predicates via context7 `/likec4/likec4` (HIGH)
- LikeC4 dynamic views: https://likec4.dev/dsl/views/dynamic/ (HIGH)
- LikeC4 CLI page (build options, `--use-dot` in containers), via context7 `/likec4/likec4` (HIGH)
- LikeC4 releases and changelog: https://github.com/likec4/likec4/releases and https://github.com/likec4/likec4/blob/main/CHANGELOG.md (1.60.0 dated 2026-10-15; 1.59.4, 1.52.0) (HIGH)
- LikeC4 PR 2705 (implicit views off by default): https://github.com/likec4/likec4/pull/2705 (HIGH)
- LikeC4 PR 3032 (Docker image Playwright/Chromium mismatch): https://github.com/likec4/likec4/pull/3032 (MEDIUM, search result text)
- Let's Encrypt rate limits (updated 2026-08-05): https://letsencrypt.org/docs/rate-limits/ (HIGH)
- cert-manager HTTP-01 page: https://cert-manager.io/docs/configuration/acme/http01/ (HIGH); FAQ "When do certs get re-issued" and certificate usage page via context7 `/cert-manager/website` (HIGH)
- DuckDNS FAQ (IPv6 not detected): https://www.duckdns.org/faqs.jsp (MEDIUM, via a summarising fetch). Five-subdomain cap: Home Assistant duckdns add-on README and dehydrated-duckdns Docker Hub listing, found by search (LOW-MEDIUM, third-party).
- Public Suffix List: https://publicsuffix.org/list/public_suffix_list.dat (HIGH)

Not used: `web-access` MCP (not on this session's tool list; I fetched with Exa fetch, context7, WebFetch and `curl` for the PSL data file). The research-plan cache seam (`gsd_run query research-plan`) was not run: the questions were answered mostly from the repo and by measurement, so there were no cacheable digests worth storing.

---
*Pitfalls research for: LikeC4 Kruchten views, static publication on k3s/Traefik/cert-manager, public repo*
*Researched: 2026-10-27*
