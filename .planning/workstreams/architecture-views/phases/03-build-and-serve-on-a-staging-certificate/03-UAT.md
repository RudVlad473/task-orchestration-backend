---
status: complete
phase: 03-build-and-serve-on-a-staging-certificate
source: [03-VERIFICATION.md]
started: 2026-10-28T22:30:00Z
updated: 2026-10-29T10:15:01.416Z
---

## Current Test

[testing complete]

## Tests

### 1. Src-only clause of criterion 1 (DECISION Q4 1, passive)
expected: Accept the same-mechanism evidence with the literal case pending, or observe a real src-only push.
result: pass

### 2. WR-05: the published model states two things that are false
expected: scenarios-platform.c4:38 no longer says the publish job is "the only job in the production environment" (deploy.yml:94,161,307 and backup-image.yml:36 also use environment: production), and delivery.c4:7 no longer says "Two workflows" (backup-image.yml also pushes an image); the step 4 evidence row cites a source that supports what it says.
result: issue
reported: "issue"
severity: major

## Summary

total: 2
passed: 1
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- gap_id: G-03-2
  truth: "scenarios-platform.c4:38 no longer claims the publish job is the only job in the production environment, delivery.c4:7 no longer says Two workflows, and the step 4 evidence row cites a source that supports what it says"
  status: resolved
  resolved_by: 03-08-PLAN.md
  resolved_at: 2026-10-29
  reason: "User reported: issue (both phrases verified still present in the model on 2026-10-29)"
  severity: major
  test: 2
  root_cause: "Review finding WR-05 (03-REVIEW.md:152) was recorded but never fixed. The sync.mjs drift gate does not check prose claims, so the false sentences ship in the published model. environment: production is on architecture-site.yml:96, backup-image.yml:36 and deploy.yml:94,161,307."
  artifacts:
    - path: "docs/architecture/model/scenarios-platform.c4"
      issue: "line 38 says 'the only job in the production environment'"
    - path: "docs/architecture/model/delivery.c4"
      issue: "line 7 says 'Two workflows'; backup-image.yml also pushes an image"
  missing:
    - "delivery.c4:7: describe each image-pushing workflow without a count, or list deploy.yml, backup-image.yml and architecture-site.yml"
    - "scenarios-platform.c4:38: 'the only job of this workflow in the production environment, whose DOCKERHUB_TOKEN is shared with deploy.yml and backup-image.yml'"
    - "step 4 evidence row in 03-SCENARIO-EVIDENCE.md: re-cite a source that supports it (not yet checked)"
  debug_session: ""
