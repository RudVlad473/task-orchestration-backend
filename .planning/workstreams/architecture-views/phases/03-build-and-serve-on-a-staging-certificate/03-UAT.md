---
status: testing
phase: 03-build-and-serve-on-a-staging-certificate
source: [03-VERIFICATION.md]
started: 2026-10-28T22:30:00Z
updated: 2026-10-28T22:30:00Z
---

## Current Test

number: 1
name: Accept the pending src-only clause of criterion 1, or push a real src-only change
expected: |
  Either the user accepts that the paths filter, a k8s-only push, three Flux bump commits and a flush
  push (none started an architecture-site run) stand for "a push that touches only src/ starts no
  site run", with the literal case still listed as pending; or a src-only commit is pushed later and
  observed to start no site run.
awaiting: user response

## Tests

### 1. Src-only clause of criterion 1 (DECISION Q4 1, passive)
expected: Accept the same-mechanism evidence with the literal case pending, or observe a real src-only push.
result: [pending]

### 2. WR-05: the published model states two things that are false
expected: scenarios-platform.c4:38 no longer says the publish job is "the only job in the production environment" (deploy.yml:94,161,307 and backup-image.yml:36 also use environment: production), and delivery.c4:7 no longer says "Two workflows" (backup-image.yml also pushes an image); the step 4 evidence row cites a source that supports what it says.
result: [pending]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
