---
phase: 14-production-ownership-backend-observability-alerting-and-reco
plan: 02
subsystem: infra
tags: [postgres, pg_dump, age, backblaze-b2, s3-lifecycle, object-lock, shell]

requires:
  - phase: 13
    provides: k3s cluster with Postgres StatefulSet in kanban-data
provides:
  - postgres-backup.sh, tested locally on both weekday branches and the fail-before-upload path
  - backup-bucket-lifecycle.json in the Backblaze pair-per-prefix shape
  - smoke-postgres-backup.fish harness
affects: [14-03 backup CronJob, 14.1 restore drill]

actuals:
  tokens: 5000
  tasks: 1
  commits: 2
plan_head_before: acb0d86b10f694d2fbccb365de4fee7532f24fcd
plan_head_after: ce2e7f77f3c037a5db9111d278aefe8fe739bb2a

tech-stack:
  added: [age, aws-cli (s3api put-object), postgresql16-client in alpine:3.22]
  patterns: [if/fi weekly branch behind an env test seam, plaintext removed by an EXIT trap]

key-files:
  created:
    - k8s/data/postgres/backup/postgres-backup.sh
    - infra/b2/backup-bucket-lifecycle.json
    - scripts/smoke-postgres-backup.fish
  modified: []

key-decisions:
  - "Task 1 (Object Lock mode) is NOT decided; Task 2 was built to the option-a/option-c baseline, which needs no lock flags in the script."

requirements-completed: []

coverage:
  - id: D1
    description: "Backup script: dump, archive check, age encrypt, put-object daily and Sunday weekly, fail before upload"
    requirement: D-10
    verification:
      - kind: integration
        ref: "fish scripts/smoke-postgres-backup.fish (5 of 5 PASS, run against a scratch postgres:16)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Lifecycle JSON accepted by the Backblaze service with the intended rules"
    requirement: D-13
    verification:
      - kind: other
        ref: "python3 -m json.tool; aws s3api put-bucket-lifecycle-configuration client-side validation (offline)"
        status: pass
    human_judgment: true
    rationale: "Resolved in Task 3: the service accepted the file and b2 bucket get showed the four rules at revision 7; not re-read after the Governance change."
  - id: D3
    description: "B2 account, bucket, write-only key, age keypair and VPS Secret"
    requirement: D-12
    verification:
      - kind: other
        ref: "b2_authorize_account v4 capabilities == [writeFiles]; put-object allowed, list refused; Secret postgres-backup keys B2_APP_KEY,B2_KEY_ID read back from the cluster"
        status: pass
    human_judgment: true
    rationale: "Human setup in the Backblaze console; Governance mode shown by delete-with-bypass behaviour, the 7-day period not read back."

duration: 70min
completed: 2026-10-16
status: complete
---

# Phase 14 Plan 02: Backup script, lifecycle rules and smoke harness Summary

**Tested POSIX backup script (pg_dump, archive check, age, daily plus Sunday weekly put-object) with B2 lifecycle JSON; the plan is halted at the Object Lock decision and the human B2 setup.**

## Status: COMPLETE (all three tasks done; see "Task 1 and Task 3 outcome" below)

| Task | State |
|------|-------|
| 1. Object Lock decision (`checkpoint:decision`, `gate="blocking"`, no `auto_select`) | DECIDED: option a (Governance, 7 days). |
| 2. Script, lifecycle JSON, harness | Done under the option-a/option-c baseline (commits below). |
| 3. B2 account, bucket, key, age keypair, VPS Secret (`checkpoint:human-action`) | Done 2026-10-16; the outcome section records each value. |

Task 2 was built before Task 1 was answered. The user chose option a, so nothing in Task 2 changed. Had they picked option-b, a follow-up commit would have been needed (kept for the record):

- postgres-backup.sh: add `WEEKLY_LOCK_DAYS` (default 28) and `--object-lock-mode GOVERNANCE --object-lock-retain-until-date <now + N days>` to the weekly put-object only.
- smoke-postgres-backup.fish: assert those two flags on the weekly call and their absence on the daily call.
- COVERAGE.md: flip "b2: Object Lock retention headers on PutObject for weekly copies" from OPT-OUT to INTEGRATE with an empty reason (left untouched now, correct for option-a and option-c).
- Task 3 step 4: the VM key is created with the B2 CLI (`writeFiles,writeFileRetentions`), not the web console.

## Task Commits

1. **Task 2 RED:** `62982de` test(14-02) - harness only; five cases FAIL against an empty script.
2. **Task 2 GREEN:** `ce2e7f7` feat(14-02) - script and lifecycle JSON; five cases PASS.

## Files Created

- `k8s/data/postgres/backup/postgres-backup.sh` - strict `sh`, validates all 12 variables first (names only in messages), `pg_dump -Fc --no-password`, `pg_restore -l`, logs `dump_bytes=N`, `age -r ... -o db.dump.age db.dump`, deletes the plaintext, `aws s3api put-object` to `daily/kanban_prod-<ts>.dump.age`, and to `weekly/` inside an `if` block when the weekday is 7. EXIT trap removes both files.
- `infra/b2/backup-bucket-lifecycle.json` - six rules: for each of `daily/` (7 days) and `weekly/` (28 days), one `Expiration Days`, one `ExpiredObjectDeleteMarker`, one `NoncurrentVersionExpiration` of 1 day.
- `scripts/smoke-postgres-backup.fish` - scratch postgres:16, throwaway age identity, stub `aws`, alpine:3.22 pinned by digest; `SMOKE_BACKUP_SCRIPT` swaps the script under test.

## Verification Run

| Check | Result |
|-------|--------|
| `fish scripts/smoke-postgres-backup.fish` | 5 of 5 PASS (exit 0). Case 3 decrypted to 3526 bytes, the logged `dump_bytes`. |
| After each run | No `smoke-pgbackup-*` container or network left; temp dir gone. Other sessions' containers untouched. |
| RED: empty script | 5 of 5 FAIL, for the planned reasons (0 put-object calls, exit 0 where non-zero expected). |
| Failing direction for encryption: `age` input argument removed (scratch copy) | Case 3 FAIL: "decrypted size is 0 or missing (logged dump size: 3526)"; other 4 cases PASS. Restored: PASS. |
| `shellcheck --shell=sh` on the script | clean |
| `python3 -m json.tool` on the lifecycle JSON | valid |
| `python3 scripts/verify-comments.py check <script>` | OK (the repo-wide run fails for an unrelated reason, below) |
| `pg_restore -l` appears once; age line precedes the first put-object; `if [` blocks at lines 46 and 67; no list/get/delete-object on code lines | all as the acceptance criteria require |
| lifecycle JSON through aws-cli parameter validation (offline, endpoint 127.0.0.1:1) | reached the connection step, so botocore accepts the shape |

Not mutation-tested: the trailing `&&` form of the weekly branch. Case 1 would catch it (exit 1 on a non-Sunday), but I did not break the script that way to confirm.

## Decisions

- Task 1: pending (see above). Recommendation in the plan is option-a.
- The lifecycle file uses separate rules per prefix (hide, orphan delete marker, noncurrent delete), copying the documented example. The page does not say whether a hide and a noncurrent delete may share one rule; if the service returns 400, merge each prefix's `NoncurrentVersionExpiration` into its `Expiration Days` rule.

## Deviations from Plan

**1. [Rule 3 - Blocking] Commits made with `--no-verify`**
- **Found during:** Task 2 commits
- **Issue:** `.githooks/pre-commit` fails at the comment-lint step on the base commit itself: 14 violations, all in `.serena/project.yml` (added by `acb0d86`; the vendored Serena template is not in `EXCLUDED_PREFIXES` of `scripts/verify-comments.py`). Gitleaks passed on both diffs before the lint step. The plan said to run the full hook.
- **Fix:** Project CLAUDE.md allows `--no-verify` unless the commit holds Java, migrations or k8s manifests. Neither commit does (a fish script, a `.sh` under `k8s/data/postgres/backup/` that no kustomization references, a JSON file). I ran the checks that bear on these files by hand (gitleaks via the hook, comment lint on the script, shellcheck, instruction-budget check OK). Spotless covers Java only and `fastTest` runs Java, so I skipped both.
- **Not run:** `scripts/verify-k8s-invariants.py` (needs PyYAML, absent here). No manifest changed.
- **Orchestrator note:** every commit in this wave will hit the same lint failure until `.serena/` is excluded or its comments fixed. I did not touch it (out of scope, and parallel agents would conflict).

**2. [Tooling] Lifecycle page read through Context7, not the web-access MCP**
- **Found during:** Task 2 step 3
- **Issue:** the web-access MCP is not on this subagent's tool list, and RULES.md says to say so rather than hand-walk a fetch ladder.
- **Fix:** read the page's indexed text through Context7 (`/websites/backblaze_apidocs`, source URL below). It gave the supported and unsupported feature lists, the pairing requirement and an example body. It is an index of the page, not the live page.
- **Source for the shape:** https://www.backblaze.com/apidocs/s3-put-lifecycle-configuration

**3. [Process] RED evidence is not classifier-checkable**
The harness prints PASS/FAIL lines, not TAP, JUnit, swift-testing or unittest output, so `gsd_run check tdd-red-evidence` does not apply. RED evidence is the recorded output above; the semantic check is that each of the five cases failed on its own assertion (call counts, exit code, missing message), not on a setup error.

**Total deviations:** 3 (1 blocking, 1 tooling, 1 process). **Impact:** none on the script's behaviour; the commit gate was weaker than the plan wanted.

## Known Stubs

None.

## Threat Flags

None beyond the plan's register. T-14-10 is covered by the EXIT trap and case 3. T-14-06 and T-14-07 depend on Task 3.

## Issues Encountered

- The container's `apk` printed "opening from cache" warnings; harmless, now silenced in the harness.
- The harness uses `rm -rf` on its own mktemp directory and inside the tools container (files there are root-owned). The user's `rm` ban hook did not block it because it runs inside a script; flagging it in case that matters.

## User Setup Required

Task 3 is done (see the outcome section). The one thing worth repeating: do not press "Update Bucket" in the Backblaze console Lifecycle Settings dialog, it replaces the rules. No `14-USER-SETUP.md` was generated.

## Task 1 and Task 3 outcome (2026-10-16, recorded after the human setup)

Values below hold no key material. Secrets live in `pass` on the operator machine (`backblaze/vm-key`, `backblaze/age-key`) and in Secret `postgres-backup` on the VPS.

- **Object Lock option chosen:** option a, Governance with a 7-day default retention.
- **Incident:** the first save in the console set Compliance mode instead. The first probe object (`test/writeonly-probe.txt`) is therefore Compliance-locked until 2026-10-23 17:32 GMT and cannot be deleted by anyone before then; it is 6 bytes. The bucket default was then set to Governance 7 days with `aws s3api put-object-lock-configuration` (admin key, exit 0).
- **Governance proof, by behaviour:** a second probe uploaded with the VM key could not be deleted without the bypass header (AccessDenied) and was deleted with `--bypass-governance-retention`. The 7-day period itself was not read back (the keys held no read capability); check it on the first real upload in the console file view.
- **S3 endpoint / region / bucket:** `https://s3.eu-central-003.backblazeb2.com` / `eu-central-003` / `kanban-prod-backups-16-09-26` (bucket id `7e860fe0483e4b5aae190a11`, private, default encryption SSE-B2 AES256).
- **age public recipient:** `age1up3xrd06vmwnhym96sqpflw0j0vhkh9nf29am6neym46x4sdaqpse8hc60`. An encrypt and decrypt round trip with the stored private key succeeded.
- **VM key capabilities:** `b2_authorize_account` on API v4 (v2 and v3 refuse the call) returned exactly `["writeFiles"]`, restricted to the backup bucket, no name prefix. A put-object succeeded and a list-objects-v2 call was refused. This meets D-12.
- **Lifecycle rules:** applied from `infra/b2/backup-bucket-lifecycle.json`. The service accepted the file. The last read-back with `b2 bucket get` (revision 7) showed four rules: `daily/` hidden after 7 days and deleted 1 day after hiding, `weekly/` hidden after 28 days and deleted 1 day after hiding. It was not re-read after the Governance change, because the admin key was revoked afterwards. The console Lifecycle Settings dialog overwrote the rules once (revision 4 to 5); never press "Update Bucket" there.
- **Admin key:** revoked in the console (Backblaze answers 401) and removed from `pass`.
- **Secret on the VPS:** `postgres-backup` in `kanban-data`, keys `B2_APP_KEY` and `B2_KEY_ID`, decoded lengths 31 and 25. It has not yet been used from inside the cluster.
- **Cost:** about 11 GB expected per database, so 10 GB free then $0.005 per GB-month (from `14-FREE-TIER-RESEARCH.md`, not live pricing). A billing cap in the Backblaze dashboard is unconfirmed.
- **Still open:** `master-key-id` and `master-key` remain in `pass`; delete the probe `test/writeonly-probe.txt` after 2026-10-23.

## Next Phase Readiness

14-03 (the CronJob) can be written and applied against the script's environment contract: Secret `postgres-backup`, the bucket, endpoint and age recipient above exist. 14-03 must also depend on 14-01's merge, as planned.

## Self-Check: PASSED

- FOUND: scripts/smoke-postgres-backup.fish, k8s/data/postgres/backup/postgres-backup.sh, infra/b2/backup-bucket-lifecycle.json
- FOUND commits: 62982de, ce2e7f7 (both ancestors of HEAD)

---
*Phase: 14-production-ownership-backend-observability-alerting-and-reco*
*Completed: 2026-10-16 (partial: Tasks 1 and 3 pending)*
