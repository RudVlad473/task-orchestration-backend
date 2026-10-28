---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "The metadata-key allowlist is surface-agnostic, so keys that are allowed are silently ignored on most surfaces"
  - id: WR-02
    severity: warning
    disposition: open
    title: "drill.fish has no oracle, and a sed that misses its target is indistinguishable from \"not caught\""
  - id: WR-03
    severity: warning
    disposition: open
    title: "DIAGRAM_CONVENTIONS understates which model sources the commit hook does not guard"
  - id: WR-04
    severity: warning
    disposition: open
    title: "The Scenarios row claims every step is cited in the evidence file, but three scenario views have no section in it"
  - id: WR-05
    severity: warning
    disposition: open
    title: "`certmanager.staging` states a fact the manifests contradict"
  - id: WR-06
    severity: warning
    disposition: open
    title: "The host-filter step and element say the chain drops \"any other new connection\"; the script returns ICMP first"
  - id: WR-07
    severity: warning
    disposition: open
    title: "`routedHosts` reads only the first argument of `Host(...)` and ignores `HostRegexp`, so the hosts rule can pass vacuously"
  - id: WR-08
    severity: warning
    disposition: open
    title: "The Mermaid-parity claim overstates what was built"
  - id: WR-09
    severity: warning
    disposition: open
    title: "The public Grafana path is claimed but not drawn"
  - id: IN-01
    severity: info
    disposition: open
    title: "A comment in sync.mjs is now false"
  - id: IN-02
    severity: info
    disposition: open
    title: "A hand-copied table list in a process-view edge title duplicates generated facts"
  - id: IN-03
    severity: info
    disposition: open
    title: "Hand-written absence and exclusivity claims on notes are unchecked"
  - id: IN-04
    severity: info
    disposition: open
    title: "A \"provenance\" convention for relationships is not followed or checked"
  - id: IN-05
    severity: info
    disposition: open
    title: "Personal hostnames are committed in the model and in a drill"
open: 14
total: 14
recorded: 2026-10-28T14:33:11.657Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| WR-05 | warning | open | - |
| WR-06 | warning | open | - |
| WR-07 | warning | open | - |
| WR-08 | warning | open | - |
| WR-09 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
