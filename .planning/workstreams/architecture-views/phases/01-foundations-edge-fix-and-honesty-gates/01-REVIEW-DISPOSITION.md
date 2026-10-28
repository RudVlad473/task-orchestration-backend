# Phase 01 review disposition

Source: 01-REVIEW.md (standard depth, 0 critical, 6 warning, 3 info). Advisory; no finding blocks the phase.
Every row is `open` until someone triages it. `open` means seen and not yet decided, not accepted.

| ID | Severity | Location | Summary | Disposition |
|----|----------|----------|---------|-------------|
| WR-01 | warning | docs/architecture/sync.mjs:263 | unreadable manifest (dangling symlink, EACCES) exits 1 with a stack trace instead of 4 | open |
| WR-02 | warning | .githooks/pre-commit:229-233 | exit 4 prints "run pnpm install"; no selftest case covers exit 4 or a missing yaml | open |
| WR-03 | warning | docs/architecture/sync.mjs:371-376 | anchored element with zero deployment instances is never checked | fixed in 02-01 (checkDeployedWorkloads, drill D19) |
| WR-04 | warning | docs/architecture/sync.mjs:379-381 | trailing slash on a k8s/ source leaves the element with no anchors, so a moved instance passes | open: no Phase 2 element anchors on a k8s/ directory source (02-01 trade-offs) |
| WR-05 | warning | docs/architecture/sync.mjs:319-322 | kustomization.yaml without apiVersion/kind is ignored, giving a false placement failure | open |
| WR-06 | warning | docs/architecture/sync.mjs:293 | any string `namespace` value counts as a definition, e.g. a RoleBinding subject | open |
| IN-01 | info | .githooks/pre-commit:201 | "Known holes" comment understates CI-only model sources | open |
| IN-02 | info | scripts/pre-commit-likec4-selftest.py:437 | trigger test pins only k8s/platform | open |
| IN-03 | info | docs/architecture/sync.mjs:230-239 | WORKLOAD_DIRS excludes k8s/monitoring and k8s/overlays with no reason given | fixed in 02-02 and 02-03 (WORKLOAD_DIRS comment) |
