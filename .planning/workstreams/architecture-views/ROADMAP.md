# Roadmap: Kanban Board Backend

## Milestones

- 🚧 **v1.5 Architecture Views** — Phases 1-4 (in progress, workstream `architecture-views`)

## Overview

v1.5 has two halves. First it corrects the LikeC4 model and completes it across the Physical,
Process, Logical and Scenarios views, with every new fact behind a drift check that has been seen
red and then green. Then it publishes the `likec4 build` output as a static site at a fourth
DuckDNS hostname. The site is served on a Let's Encrypt staging certificate first and moves to
production only after the built artifact has passed a leak scan and the user's review. Phase
numbering restarts at 1 in this workstream. Requirements are in `REQUIREMENTS.md` and research in
`research/SUMMARY.md`, both beside this file.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundations, Edge Fix and Honesty Gates** - Traefik moves out of the nonexistent `edge` namespace, and the namespace, placement, metadata-key and view-membership checks are built and proven red then green (completed 2026-10-27)
- [ ] **Phase 2: Model Completion** - Delivery, monitoring, cert-manager and edge facts in Physical; Schema Registry, backup and Avro subjects in Process and Logical; seven scenarios; the YAML fact reader; the standard amended
- [ ] **Phase 3: Build and Serve on a Staging Certificate** - CI builds the site image on model changes, Flux serves the static build at the architecture hostname on a staging certificate, and the publish path is modelled as built
- [ ] **Phase 4: Leak Review and Production Go-Live** - Leak scan of the built artifact and the user's review, then the production certificate flip, the uptime probe and the public links

## Phase Details

### Phase 1: Foundations, Edge Fix and Honesty Gates

**Goal**: The published Physical view stops naming a namespace that does not exist. The drift check fails loudly when a namespace, a placement, a metadata key or an instance's view membership drifts, so every element Phase 2 adds is checked from the commit that adds it.
**Depends on**: Nothing (first phase)
**Requirements**: PHYS-01, GATE-02, GATE-03, GATE-04
**Success Criteria** (what must be TRUE):
  1. Against the model as it stands today, the new namespace check makes `node docs/architecture/sync.mjs check` exit 1 and name the `edge` node (red on the real defect). After Traefik moves to a `kube-system` node it exits 0, and the `production` view in `likec4 export json` has a `kube-system` node and no `edge` node.
  2. Every namespace the manifests define is a namespace node in `deployment.c4`: the four in `k8s/platform/namespaces.yaml`, plus `kube-system`, `flux-system` and `cert-manager` from their Helm and Flux sources. The placement check reads `k8s/platform` as well as `k8s/base` and `k8s/data`. On a scratch copy, deleting one namespace node and moving one instance into the wrong namespace each make `check` exit 1, and the unmodified copy exits 0.
  3. On a scratch copy, a misspelled metadata key (for example `sourc` for `source`) makes `check` exit 1 and name the key and its element. The unmodified model exits 0.
  4. On a scratch copy, a deployment instance with no relationship makes `check` exit 1 and name the instance. Such an instance appears in no computed deployment view (Pitfall 2, measured). Adding a relationship or removing the instance returns exit 0.
  5. Each new rule has a numbered drill in `docs/architecture/drill.fish`. The recorded output shows the break not caught before the rule exists and caught after it. `fresh()` copies every directory the new readers read, and a reader pointed at a missing directory exits non-zero instead of reporting clean. The pre-commit hook triggers on `k8s/platform`, and `scripts/pre-commit-likec4-selftest.py` passes with more than the 15 cases it had at research time.

**Plans**: 3/3 plans complete

Plans:
- [x] 01-01-PLAN.md — yaml@2.9.1 behind a human legitimacy gate; namespace rule red on the real `edge` defect and landed with the Traefik-to-kube-system fix; `workloads()` port and `render()` split, byte-identical
- [x] 01-02-PLAN.md — placement, metadata-key allowlist and instance view-membership rules, each with drills recorded not caught before and caught after
- [x] 01-03-PLAN.md — fail-closed k8s fact sources (exit 4), hook trigger widened to `k8s` with selftest case 16, standard and README updated

**Planning notes**: Add the `yaml` parser dependency and split `render()` here (ARCHITECTURE build order, M-A). See Open Question 6 on namespace nodes that have no instances until Phase 2.

### Phase 2: Model Completion

**Goal**: Every runtime, delivery and physical fact that matters appears in the Physical, Process, Logical and Scenarios views. Each new fact is tied to a check or labelled as authored, and the LikeC4 standard describes what is drawn.
**Depends on**: Phase 1
**Requirements**: PHYS-02, PHYS-03, PHYS-04, PROC-01, LOGI-01, SCEN-01, SCEN-02, SCEN-03, SCEN-04, SCEN-05, SCEN-06, SCEN-07, GATE-01, STD-01
**Success Criteria** (what must be TRUE):
  1. `likec4 export json` lists three `#physical` deployment views. `production` is the existing one. A delivery view shows GitHub, the CI runner, Docker Hub, Flux and the path an app image takes into the cluster. A monitoring view shows the monitoring nodes, with an edge to each thing they scrape. The Physical views also show cert-manager with its staging and production ClusterIssuers, the edge routing, and the host filter as an external element. The Phase 1 view-membership check stays green, and no edge label in any deployment view's export reads `[...]`.
  2. The `process` view shows Schema Registry with the `register-schemas` init job and the management port, and the backup flow from Postgres through encryption to the off-cluster bucket. The `data` view has notes for `activity_log` and the Avro subjects. Each note's `metadata.source` names a file that `check` confirms exists.
  3. Scenario views tagged `#scenarios` exist for signup; for error status, as one view per outcome with client-visible discriminators only (401 with no SQL step, 403 after the ownership SELECTs, 409 on the version check); for push-to-image; for image-to-rollout; for inbound-packet-path and ingress-to-pod; for a mutation reaching an `activity_log` row; and for the nightly backup. Every step has its own title, and no view has more than 5 participants. The phase evidence file cites the code or manifest line behind each step.
  4. `sync.mjs` reads the workloads, namespaces and hosts that the new elements claim from the k8s YAML, including `k8s/platform`, `k8s/monitoring` and `k8s/flux-system`. `check` exits 0 on the completed model. On a scratch copy, three breaks each make `check` exit 1, and each is recorded by a drill: renaming a claimed workload (a HelmRelease or Flux controller included), changing a claimed host, and changing a claimed namespace. The unmodified copy exits 0.
  5. In `docs/DIAGRAM_CONVENTIONS.md`, the Physical row names the deployment views as built and no longer says the monitoring stack is excluded. The Scenarios row describes the scenarios as built, including the internal steps of the activity-log and delivery scenarios. Searching that file for "excluded, as in `production-host.mmd`" finds nothing.

**Plans**: 1/5 plans executed

Plans:
**Wave 1**
- [x] 02-01-PLAN.md — `[...]` label rule red on the real defect and fixed with one title per instance pair; participant and workload-instance rules; spec kinds; `production` narrowed; MODEL override and drills D17 to D19 (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [ ] 02-02-PLAN.md — HelmRelease reader with the monitoring stack, cert-manager and the `monitoring` view; Schema Registry, init job, management port, backup flow and notes in `process` and `data`; drills D20, D21 (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 02-03-PLAN.md — Flux, ClusterIssuer and hosts readers with the `delivery` view, the issuers and the inbound path in `production`; drills D22 to D24 (wave 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 02-04-PLAN.md — signup, error 401/403/409 and activity-log scenarios with cited steps; one live measurement of the 401 path's SQL, stopping for the user's decision if it contradicts criterion 3 (wave 4, after 02-03)

**Wave 5** *(blocked on Wave 4 completion)*
- [ ] 02-05-PLAN.md — push-to-image, image-to-rollout, inbound-packet-path, ingress-to-pod and nightly-backup scenarios; STD-01 standard amendment and the phase gate (wave 5)

**Planning notes**: Build elements and their checks first and scenarios after, because the scenarios use the elements. Write each description fresh, in one sentence, with no runbook text pasted in (Pitfall 11). The Phase 4 scan catches anything that slips through. Before SCEN-05 is planned, run the render spike in Open Question 5. Put metadata on elements, not relationships: whether LikeC4 1.60 supports metadata on relationships is unverified.

### Phase 3: Build and Serve on a Staging Certificate

**Goal**: Every model change on `main` rebuilds the static site. Flux serves the static build at the architecture hostname over HTTPS on a Let's Encrypt staging certificate. The publish path is drawn in the model as it was built, not as planned.
**Depends on**: Phase 2
**Requirements**: PUB-01, PUB-02, SCEN-08
**Success Criteria** (what must be TRUE):
  1. A push to `main` that changes only the model runs the site workflow. The workflow runs `sync.mjs check` before `likec4 build`, then pushes an image tagged `main-<run>-<sha7>` to the site's own Docker Hub repository. A model deliberately drifted on a branch fails the workflow before the push step (red), and the fixed model pushes (green). The PR-time build runs with no secrets. `gh run list` shows no site run for Flux's `chore(flux): bump images` commit or for a push that touches only `src/`.
  2. Flux rolls the new tag into the site's namespace. `openssl s_client` against the hostname shows an issuer containing `(STAGING)`, and the Certificate is `Ready=True` on `letsencrypt-staging`. `curl -k https://<host>/` returns 200 with the `likec4-root` marker. The prod, nonprod and monitoring hostnames answer as they did before the change.
  3. Only static build output is reachable. `curl -k https://<host>/@fs/etc/hostname` does not return the file's bytes. A gate fails on a planted line that runs `likec4 start`, `preview` or `--listen` from a manifest, a Dockerfile or a workflow (red) and passes once the line is removed (green). A cold deep link to `/view/<id>/` renders that view in a headless browser. The response to a missing `/assets/*.js` is measured and recorded, which settles Open Question 1 before Phase 4.
  4. `scripts/verify-k8s-invariants.py --no-provisional` is green in CI for the new manifests. The memory value carries a MEASURED comment from a ladder run on the VPS (I4). I5 applies to the site overlay, failing with the setter marker removed (red) and passing once it is restored (green). The route has the I10 shape, and the site objects have one owner under I11: the live IngressRoute's `kustomize.toolkit.fluxcd.io/name` label names a single Kustomization.
  5. The model contains the site as built: a namespace node for it, the site instance with a `traefik ->` edge, and a publish-the-site scenario whose steps cite the workflow and manifest lines. The Phase 1 and Phase 2 checks are green and cover the site's workload.

**Plans**: TBD
**Planning notes**: Open Questions 1, 2 and 3 block planning. Bootstrap order: run the workflow once with `workflow_dispatch` so the first tag exists before the overlay that names it is committed. Before this Certificate is committed, decide on `serviceType: ClusterIP` for the HTTP-01 solver (Pitfall 10, Open Question 8). No uptime probe yet, because a staging certificate fails its TLS check. Live checks over SSH are read-only unless the plan says otherwise (PROJECT.md key decision).

### Phase 4: Leak Review and Production Go-Live

**Goal**: The built artifact passes a leak scan and the user's review. Only after that does the site move to a production certificate, get an uptime probe and get linked from the docs and the README.
**Depends on**: Phase 3
**Requirements**: PUB-03, PUB-04, PUB-05, PUB-06
**Success Criteria** (what must be TRUE):
  1. A leak-scan step in the site workflow runs on the built directory before the push step. It scans for IPv4 and IPv6 literals, key paths, `ssh `, `kubectl`, Secret names, in-cluster hostnames and the deny-list. A planted IP literal in a model description fails it (red), and removing the IP makes it pass (green). The current build passes once every description the scan flags has been rewritten. `curl https://<host>/robots.txt` returns `Disallow: /`.
  2. The user's approval of the scan report and the built artifact is recorded in the phase evidence before the flip. `git log` shows the Certificate first committed on `letsencrypt-staging` and flipped to `letsencrypt-production` in a separate, later commit. After the flip the Certificate is `Ready=True` on the production issuer, and `curl https://<host>/` succeeds without `-k`.
  3. `uptime-check.yml` probes the site for a body marker, and `timeout-minutes` is raised to cover the extra URL (ARCHITECTURE 3.4 arithmetic). A drill against a wrong marker or URL fails the probe (red), and the restored entry passes (green). The runbook's list of probed URLs includes the site.
  4. On the production hostname, a cold deep link to `/view/<id>/` and a refresh of it both render the view in a headless browser. `curl -I` of a missing `/assets/*.js` does not answer 200 with `text/html`. The README and `docs/INFRA_ARCHITECTURE.md` link the public address, which `rg` finds in both files.

**Plans**: TBD
**Planning notes**: The "review gate" in PUB-05 is read as a human checkpoint, because only the repo owner can judge what counts as sensitive (PITFALLS 11). Let's Encrypt allows 5 certificates per exact name per 7 days with no override, so the flip happens once, after staging is `Ready`.

## Structure Decisions

These follow the user's four-phase order. Where the requirements differ from that order, the reason is given here.

- **SCEN-08 (publish-the-site scenario) is in Phase 3, not Phase 2.** The site pipeline does not exist until Phase 3. Drawing it in Phase 2 would model it as planned rather than as built, which ARCHITECTURE warns against.
- **GATE-01 is in Phase 2, not Phase 1.** Its workload and host readers force a model element for every Flux controller, HelmRelease and routed host they find. Those elements are Phase 2 work. Checks land in the same commit as the elements they check (FEATURES MVP step 6). The namespace half of the check is already delivered by GATE-04 in Phase 1.
- **PUB-03 (staging, then production) is in Phase 4.** The requirement is complete only when the flip happens. Phase 3 delivers the staging half as part of PUB-02's HTTPS serving (Phase 3 criterion 2). Phase 4 verifies the whole sequence from `git log` and live state.
- **PUB-04 (uptime probe) and PUB-06 (public links, URL behaviour) are in Phase 4.** A probe against a staging certificate fails its TLS check and opens a GitHub issue. A README link before the flip points visitors at a TLS warning. PUB-06's routing half depends on a server choice made in Phase 3, so Phase 3 measures it (criterion 3) and Phase 4 verifies it on the production hostname.
- **Phase 2 stays one phase despite 14 requirements.** Granularity is `coarse` (2 to 4 phases). Most of its work is LikeC4 text rated Low to Med in FEATURES, and the plans can run in waves: elements and their checks, then scenarios. ARCHITECTURE splits the same work into three phases (delivery, monitoring, scenarios). That split would give 6 phases, and all three would edit `deployment.c4` and `sync.mjs`, so they would run one after another anyway.

## Open Questions

Each question records what the research leaves unclear. A default is given only where the research or the user already gave one.

1. **Routing and server (blocks the Phase 3 server choice; Phase 4 verifies it).** PUB-06 asks for history-style URLs and for a missing asset that does not return 200 HTML. STACK measured that SWS 2.44.1 with `page-fallback` answers `/assets/missing.js` with 200 `text/html`. It also measured that 2.44.1 cannot filter header rules by status. Three candidates, none verified for this requirement:
   - (a) nginx-unprivileged with the fallback limited to extensionless paths. Not tested; it needs `emptyDir` mounts and an `nginx.conf`.
   - (b) SWS serving the byte-identical `404.html` with status 404 for every miss. Deep links would render but answer 404, and whether that counts as "resolve" is not settled.
   - (c) Hash history. It removes the hazard but is not history-style, so it contradicts the accepted default.

   Measure (a) and (b) on the real build when planning Phase 3.
2. **Build location, file layout and names (Phase 3).** The two research files disagree:
   - STACK: the runner builds the site, and a Dockerfile in `infra/architecture-site/` only `COPY`s it. Overlay `k8s/overlays/architecture/`, Kustomization `apps-architecture`.
   - ARCHITECTURE: a multi-stage Dockerfile in `docs/architecture/`. Directories `k8s/base/site/` and `k8s/overlays/site/`, Kustomization `site`.

   The docs say `--use-dot` defaults to true inside a container, which is untested here. Either way the setter marker must sit under `./k8s/overlays`.
3. **Operator actions outside the repo (Phase 3).** STACK reports that `kanban-board-rud-vlad-473-architecture.duckdns.org` already resolved to the VPS via 8.8.8.8 on 2026-10-27, while ARCHITECTURE lists creating the subdomain as an operator action. Re-check with `dig @8.8.8.8` before the Certificate commit, and add no AAAA record. The Docker Hub repository must exist and be public before the first push. Docker Hub's default privacy for an auto-created repository is unverified, so create it by hand.
4. **Exposure during the staging window (Phases 3 to 4).** From the first staging rollout until the Phase 4 review, anyone who clicks through the TLS warning sees the unreviewed model. The bundle already contains `PGHOST postgres.kanban-data` and the `KANBAN-INGRESS` and Netcup descriptions (PITFALLS 11, measured). The user's rule covers the production certificate only, and the repository is public, so this is not new disclosure. Default: keep the order. Alternative: land the leak-scan step in Phase 3, before the first push, and leave only the human review and the flip in Phase 4.
5. **Host-level elements (Phase 2: PHYS-04, SCEN-05).** No spec kind fits an iptables chain. Four things are unverified:
   - whether a non-Kubernetes element can sit in the deployment model
   - whether a parent and its child can both be participants in one sequence view
   - whether `par` works (standard V1 bans branches anyway)
   - how merged edges of mixed kinds are styled

   FEATURES rates this Med-High. A render spike is needed when planning Phase 2, before SCEN-05.
6. **Namespace nodes with no instances (Phase 1: PHYS-01).** `cert-manager`, `flux-system` and `monitoring` get nodes in Phase 1 but no instances until Phase 2. Pitfall 2 measured that unconnected instances are not drawn, and an empty namespace node is probably not drawn either (unverified). So Phase 1 checks PHYS-01 against the model export, and those namespaces appear in rendered views only from Phase 2.
7. **Legibility rule.** The standard says the LikeC4 legibility rule is undefined. The research says to define it in a browser at 1440 x 900 before the new views are judged, but no v1 requirement owns it. Default: Phase 2 caps scenarios at 5 participants (the width proven so far) and records each new view's natural width in the evidence, without writing a number into the standard. Adding the rule to STD-01's scope is the user's call.
8. **Research recommendations with no v1 requirement.** These are listed so they are not dropped silently:
   - CSP and response headers: the cold-load policy is measured, the interactive walk is not, and Report-Only comes first
   - `serviceType: ClusterIP` on both HTTP-01 solvers, which changes a shared ClusterIssuer
   - Traefik's 64Mi edge limit under concurrent cold loads (unverified)
   - a build stamp that ties the live site to the model commit

   Planning may fold these into PUB-02, PUB-03 or PUB-05 as implementation detail. Making any of them a requirement is the user's call.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundations, Edge Fix and Honesty Gates | 3/3 | Complete    | 2026-10-27 |
| 2. Model Completion | 1/5 | In Progress | - |
| 3. Build and Serve on a Staging Certificate | 0/TBD | Not started | - |
| 4. Leak Review and Production Go-Live | 0/TBD | Not started | - |
