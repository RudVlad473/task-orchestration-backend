# Requirements: Kanban Board Backend — v1.5 Architecture Views

**Defined:** 2026-10-27
**Core Value:** A reader of the public repository can see how the system is built from a model that cannot drift from the code, not from screenshots.

Research: `.planning/workstreams/architecture-views/research/SUMMARY.md`. Phase numbering restarts at Phase 1 in this workstream.

## v1 Requirements

### Physical view (PHYS)

- [x] **PHYS-01**: The Physical view places Traefik in `kube-system` (not the nonexistent `edge` namespace) and every namespace the manifests define exists in the model.
- [ ] **PHYS-02**: A delivery view shows GitHub, the CI runner, Docker Hub and Flux, and how an image reaches the cluster.
- [ ] **PHYS-03**: A monitoring view shows the monitoring nodes and what they scrape.
- [ ] **PHYS-04**: The Physical view shows cert-manager, its issuers and the edge routing, with the host filter as an external element.

### Process and logical views (PROC, LOGI)

- [ ] **PROC-01**: The Process view shows Schema Registry with its init job and management port, and the backup flow.
- [ ] **LOGI-01**: The Logical view carries the `activity_log` table and the Avro subjects as notes tied to their sources.

### Scenarios (SCEN)

- [ ] **SCEN-01**: Signup scenario.
- [ ] **SCEN-02**: Error-status scenario with the client-visible discriminators only: 401 with no SQL, 403 after the ownership selects, 409 on the version check.
- [ ] **SCEN-03**: Push-to-image scenario.
- [ ] **SCEN-04**: Image-to-rollout scenario.
- [ ] **SCEN-05**: Inbound-packet-path and ingress-to-pod scenarios.
- [ ] **SCEN-06**: A mutation to `activity_log` row scenario.
- [ ] **SCEN-07**: Nightly backup scenario.
- [ ] **SCEN-08**: Publish-the-site scenario.

### Honesty gates (GATE)

Every gate is proven red on a deliberate break, then green, before it counts.

- [ ] **GATE-01**: `sync.mjs` reads the k8s yaml facts the new elements claim (namespaces, workloads, hosts) and fails on drift.
- [ ] **GATE-02**: An unknown metadata key fails the check.
- [ ] **GATE-03**: A view-membership check catches an element that silently vanishes from its view (an instance with no relationship).
- [ ] **GATE-04**: A namespace and placement check covers `k8s/platform`, not only `k8s/base` and `k8s/data`.

### Standard (STD)

- [ ] **STD-01**: The Physical and Scenarios rows of `docs/DIAGRAM_CONVENTIONS.md` are amended to match what is built.

### Publish (PUB)

- [ ] **PUB-01**: CI builds the static site (`likec4 build`) into an image pushed to its own Docker Hub repo, rebuilt only on source-path changes.
- [ ] **PUB-02**: k8s manifests and Flux wiring serve the image at the DuckDNS architecture hostname. Only static build output is exposed, never `likec4 start`.
- [ ] **PUB-03**: The certificate is issued by the staging issuer first, then flipped to production.
- [ ] **PUB-04**: An uptime probe covers the site.
- [ ] **PUB-05**: A leak scan of the built artifact and a review gate run before the production certificate. `robots.txt` keeps `Disallow: /`.
- [ ] **PUB-06**: Docs and the README link to the public address; history-style URLs resolve and a missing asset does not return 200 HTML.

## v2 Requirements

Deferred. Not in this roadmap.

- **V2-01**: The signup race scenario.
- **V2-02**: The DROP TABLE scenario.
- **V2-03**: A layer overlay.
- **V2-04**: State markers.
- **V2-05**: A retention job for the third Docker Hub repo.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Mermaid migration and retirement of `docs/diagrams/` | User decision 2026-10-27: skip retirement in this milestone. |
| Exposing `likec4 start` | Served `/@fs/etc/hostname` in measurement; a file-read hole. |
| Filter-chain components in the error-status scenario | Needs a rewrite of the Scenarios row; client-visible discriminators chosen instead. |
| Indexing by search engines | `Disallow: /` kept; not access control, CT logs reveal the hostname regardless. |
| Gating Java package cycles | Reported by `sync.mjs check`, never gated (decision in quick 261027-if9). |

## Traceability

Populated by the roadmapper.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PHYS-01 | Phase 1 | Complete |
| PHYS-02 | Phase 2 | Pending |
| PHYS-03 | Phase 2 | Pending |
| PHYS-04 | Phase 2 | Pending |
| PROC-01 | Phase 2 | Pending |
| LOGI-01 | Phase 2 | Pending |
| SCEN-01 | Phase 2 | Pending |
| SCEN-02 | Phase 2 | Pending |
| SCEN-03 | Phase 2 | Pending |
| SCEN-04 | Phase 2 | Pending |
| SCEN-05 | Phase 2 | Pending |
| SCEN-06 | Phase 2 | Pending |
| SCEN-07 | Phase 2 | Pending |
| SCEN-08 | Phase 3 | Pending |
| GATE-01 | Phase 2 | Pending |
| GATE-02 | Phase 1 | Pending |
| GATE-03 | Phase 1 | Pending |
| GATE-04 | Phase 1 | Pending |
| STD-01 | Phase 2 | Pending |
| PUB-01 | Phase 3 | Pending |
| PUB-02 | Phase 3 | Pending |
| PUB-03 | Phase 4 | Pending |
| PUB-04 | Phase 4 | Pending |
| PUB-05 | Phase 4 | Pending |
| PUB-06 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 25 total
- Mapped to phases: 25
- Unmapped: 0

---
*Requirements defined: 2026-10-27*
