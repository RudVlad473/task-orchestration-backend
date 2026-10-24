---
phase: quick-261024-jvc
plan: 01
subsystem: code-comments
tags: [review-comments, comments-only, docs]
status: complete
requirements: [QUICK-261024-jvc]
commits: 3
plan_head_before: 6762827f546a1704e60a3d501619ca21457e714b
plan_head_after: c22827a148e68aae32032c4b7c593a4b891c596f
key-files:
  modified: 76 files across src/main, src/test, scripts, .github/workflows, k8s, infra, .serena, .planning (comments only)
actuals:
  tasks: 3
  commits: 3
---

# Quick 261024-jvc: Apply the 198 review-comments findings

195 of 198 findings applied as comment-only edits in three commits; the other 3 are the rollout-gated postgres init findings, left alone on purpose. `not-applied.txt` stayed empty: no other finding was skipped.

## Commits

| Task | Commit | Scope |
| ---- | ------ | ----- |
| 1 | 3c08a62 | six factual-error comments plus the 4 non-factual findings in the same files (10 findings, 6 files) |
| 2 | c47fa48 | 110 findings in 42 src/main and src/test Java files |
| 3 | c22827a | 75 findings in 28 non-Java files (28 files changed, 35 insertions, 185 deletions) |

Tasks 1 and 2 were executed by earlier runs; their facts here come from their commit messages and the `check 1` / `check 2` gates, not from my own re-reading.

## Applied counts

Task 1: 10. Task 2: 110. Task 3: 75. Total 195, matching `gone 195` in the final ledger line.

## Not applied or left alone

Skipped findings from `not-applied.txt`: none (file is empty).

Rollout-gated, never edited (3 rows). The postgres-init configMapGenerator hashes its input files, so any byte change renames the ConfigMap and restarts the Postgres StatefulSet on the next Flux reconcile.

- k8s/data/postgres/init/01-create-databases-and-roles.sh, 2 findings
- k8s/data/postgres/init/02-create-monitoring-role.sh, 1 finding

`git diff --quiet 6762827 -- k8s/data/postgres/init/ src/main/resources/db/migration/` exits 0.

## Deviations and grading notes

- **I10 kept (rule 6).** `I10` is the IngressRoute redirect invariant in scripts/verify-k8s-invariants.py (its FAIL lines start with `I10:`). In k8s/overlays/nonprod/ingressroute.yaml only the trailing "as in prod" was dropped, so "(I10)" stays. In k8s/platform/edge/ingressroute-monitoring.yaml the replacement text was applied as given, which also keeps "(I10)".
- **Truncated replacement, rewritten true.** The worklist replacement for scripts/verify-k8s-manifests.sh (the `--tool` comment) was the fragment `--tool <kubectl`. I wrote the full comment and named the real consumer, found with rg: verify-k8s-invariants.py (it runs `bash verify-k8s-manifests.sh --tool kubectl`).
- **Wrapped replacements.** Long single-line replacements (deploy.yml, security-scan.yml, infra/vm/k3s-host-firewall.sh, .serena/project.yml, extract-mockup-text.py) were re-wrapped to the neighbouring width. The infra/vm/k3s-host-firewall.sh rewrite also drops the "found live (Task 3)" aside, as the finding asked.
- **Edits that went further than a pure shorten, for review:** invariant-checks.yml header drops the whole "Jobs:" list (the finding's replacement does this) and keeps the Decisions block; .serena/project.yml collapses the 37-line language-server table into two lines plus the upstream enum link.
- **Task 3 touches no Java and no AAA markers.**

## Security-file edits

None in Task 3 (the security-file list belongs to Task 2; see c47fa48's message: AuthenticationController, SecurityConfiguration, ProblemDetailAuthenticationEntryPoint and the three security tests had only the scan finding id and sibling pointers removed).

## Final gate output (Task 3)

- `python3 scripts/verify-comments.py check`: `OK: 315 files, 1170 comment blocks`
- `verify-comments.py equiv --base 6762827 <task 3 paths>`: `OK: ... 21 changed in-scope files equivalent, 0 new, 0 allow-listed`
- `git diff --quiet base -- k8s/data/postgres/init/ src/main/resources/db/migration/`: exit 0
- `worklist.fish yamleq` on .serena/project.yml and promtool-fixture/rules.yml: SAME, SAME
- `bash -n` on scripts/verify-k8s-manifests.sh, scripts/verify-postgres-init-quoting.sh, infra/vm/k3s-host-firewall.sh and the 05-infra-migration verify-baseline-schema.sh: ok
- `python3 -m py_compile` on the four .planning Python files: ok. `git diff -U0` of those files read in full: every changed line is a comment, docstring or banner line, plus the trailing comment on one `return 8`.
- Selftests: verify-k8s-invariants-selftest.py exit 0 (I1-I10 fire); verify-comments-selftest.py 45/45; fuzz/run-fuzz-selftest.py 19/19; verify-diagrams-selftest.py 38/38.
- `python3 scripts/verify-k8s-invariants.py` on the real tree: exit 0, 12 kustomization roots.
- `fish worklist.fish check 3`: exit 0. `ledger: pending 115, graded 2487, gone 195` / `OK: every worklist finding for tasks 1..3 is applied, skipped or listed in not-applied.txt`.
- Pre-commit hook for c22827a: passed (gitleaks, comment lint, instruction budget, spotlessCheck, fastTest).

## Final `whole_tree.py verify` counts

Last line: `ledger: pending 115, graded 2487, gone 195`.

UNAPPLIED 589 in total, split by verdict (all expected): 400 delete (the bare arrange/act/assert markers kept on purpose, rule 5), 186 decision-record, 3 rewrite (the rollout-gated rows). PENDING 115 (the rewritten comments' new text and neighbours whose anchor changed).

## Observations

- The IMAGE_TAG value in .env.nonprod.example is `6755c84`, the older bare-hash form; CI now publishes main-<run_number>-<sha7>. Only the comment was corrected, the value is unchanged.
- FlywaySchemaProvenanceTest counts migration versions 1 to 6 while nine migrations exist (V1 to V9). Per the plan this is recorded, not fixed here; I recounted the migration files (9) but did not re-read the test.
- The hook's gitleaks step printed `0 commits scanned, scanned ~0 bytes` for the Task 3 commit, which was made with `git commit -- <paths>`. It reported "no leaks found", but the staged-diff scan may not have seen the diff under a pathspec commit. I could not verify this; worth a look at `.githooks/pre-commit`.
- fastTest was `UP-TO-DATE` in the Task 3 hook run (Java sources unchanged since Task 2's run), so no Testcontainers started in it.
- Comment-length advisories (long header blocks in verify-k8s-manifests.sh, verify-k8s-invariants.py, project.yml, k3s-host-firewall.sh) fired on blocks this task did not own; not acted on.

## Containers

`docker ps -a --filter label=org.testcontainers=true` showed one container created after 2026-10-24T14:37:28+02:00: 8b181d5329e0 (postgres:16, created 14:55:10 +0200, still Up, Testcontainers labels, inside Task 2's hook window, no gradle test process running afterwards). Stopped and removed it with `docker stop` and `docker rm -v`.

Left alone (older, all exited, created before this task): e8785121a7f5 (2026-10-16 22:36, postgres:16), 4f7231f255b6 and 14d8d935d192 (2026-10-16 10:49, postgres:16), ea9697796f2a (2026-10-05, postgres:16), 09d0fce6d520 (2026-09-25, redpanda:v26.2.1), a8e45073579a (2026-09-25, postgres:16).

## User files

Final `git status --short` shows the user's files exactly as at the start and in no commit: modified .planning/STATE.md, AsyncConfig.java, KafkaConsumerConfig.java; untracked kanban-backup.agekey and ActivityTraceContinuityE2ETest.java (plus this quick directory, uncommitted by instruction).

## Self-Check: PASSED

- c22827a, c47fa48 and 3c08a62 are ancestors of HEAD; `git show --name-only HEAD` lists the 28 task-3 paths and no user file.
- `check 3` exits 0 with `gone 195`.
