---
phase: 04-leak-review-and-production-go-live
reviewed: 2026-10-29T21:00:00Z
depth: standard
files_reviewed: 14
files_reviewed_list:
  - docs/architecture/site/leak-scan.mjs
  - docs/architecture/site/leak-scan.test.mjs
  - .github/workflows/architecture-site.yml
  - .github/workflows/uptime-check.yml
  - docs/architecture/drill.fish
  - docs/architecture/drills.tsv
  - .githooks/pre-commit
  - scripts/pre-commit-likec4-selftest.py
  - k8s/overlays/architecture/certificate.yaml
  - docs/architecture/model/edge.c4
  - docs/architecture/model/scenarios-platform.c4
  - docs/INFRA_ARCHITECTURE.md
  - docs/INFRA_RUNBOOK.md
  - README.md
findings:
  critical: 0
  warning: 5
  info: 7
  total: 12
status: issues_found
---

# Phase 4: Code Review Report

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 14
**Status:** issues_found

## Summary

The phase diff (`git diff fc88613..HEAD`) adds the leak scan and its 81 fixtures, two workflow steps, the site probe, the hook step, selftest case 20, the production Certificate and the doc changes. No structural findings block was supplied, so there is no fallow section.

What was run, not just read:

- `node --test docs/architecture/site/leak-scan.test.mjs`: 81 pass, 0 fail, 4.07 s.
- `node docs/architecture/site/leak-scan.mjs docs/architecture/site/out` on the local build: `OK targets=13`.
- 55 hand-written inputs piped through the scan in scratch directories under `/tmp/leakprobe` (outside the repo, nothing in the repo was edited), and 7 mutants of a scratch copy of the scan run against the unchanged fixture suite.
- `python3 scripts/verify-comments.py check`: OK (340 files). That lint classifies only `.java .gradle .sh .py .toml .yml .yaml .properties .sql .conf .service .tsv`, Dockerfile and the hook, so `.mjs` and `.fish` files are outside it. I read their comments by hand against rule 14 and found no violation.
- `shellcheck -s sh .githooks/pre-commit`: only the pre-existing SC1007 and SC2181 style notes; the new step at line 241 repeats the existing `$?` idiom. `fish -n drill.fish`: clean. Both workflows parse as YAML.

Claims checked against source and found true: the workflow step order (sync check, fixture tests, build, shell grep, scan, `docker build`, upload; the scan has no `|| true` and no `if:`); the 400 s and 480 s timeout arithmetic and the URL counts; the 20 selftest cases and the docstring tally (13 + 3 + 2 + 1 + 1); the evidence-file line cites for `publish-the-site` (workflow lines 86-87 and 90); `Disallow: /` in the build output; the production Certificate facts against `04-04-flip.txt` (issuer production, Ready, `revision=2`); the runbook's namespace and Certificate name `architecture`; the D30 and D30b rows against `drill-output.txt`; the `sed` anchor of D30 against `edge.c4:27`.

The scan is sound for the strings it was built around. The defects are false negatives in forms the fixtures never exercise, a report line that can echo part of a credential into a public log, and a fixture suite that cannot see the scan skipping files.

## Warnings

### WR-01: IPv6 literals with `::` followed by two or more groups pass the scan

**File:** `docs/architecture/site/leak-scan.mjs:36-37` (patterns), `:56` (target)
**Issue:** `IPV6_SHORT` accepts `::` only when at most one group follows it, and `IPV6_LONG` needs four or more groups with no `::`. Verified by running the scan on one-file directories, all printing `OK targets=13` (exit 0):
- `2001:db8::8a2e:370:7334`
- `fe80::1ff:fe23:4567:890a`
- `fd00::a:b:c`
- `via ::1 here`

D-02 and PITFALLS 11 name "IPv6 literals" and the public runbook states the VPS IPv6 prefix, so this is a listed target with a hole. The fixtures pin only `2001:db8:0:0:0:0:0:1` and `fe80::1`; removing `IPV6_SHORT` fails exactly one test, so nothing pins the middle-compression shape. Common prefix forms such as `2a03:4000:1:2::` and `2a03:4000:1:2::1` are caught (verified), which is why this is a WARNING and not a BLOCKER; the current build is clean.
**Fix:** Add a third alternative for a compressed address with a tail, and one fixture per shape:
```js
const IPV6_COMPRESSED =
  '(?<![0-9A-Za-z:])(?:[0-9A-Fa-f]{1,4}:){0,6}:(?::?[0-9A-Fa-f]{1,4}){1,6}(?![0-9A-Za-z:])'
// ...
['ipv6', new RegExp(`${IPV6_LONG}|${IPV6_SHORT}|${IPV6_COMPRESSED}`, 'g')],
```
Then add LEAKS rows for `2001:db8::8a2e:370:7334`, `fd00::a:b:c` and `::1`, and a TRAPS row for the clock-time and `::` in CSS cases already measured.

### WR-02: `ssh-command` requires a trailing space, so a description that ends with "ssh." passes

**File:** `docs/architecture/site/leak-scan.mjs:57`
**Issue:** The pattern is `/ssh /g`. Verified `OK targets=13` for `reached over ssh.`, `ssh, then`, `"ssh"` and `use ssh<newline>to go`. DIAGRAM_CONVENTIONS requires element and view descriptions to end with a period (drill D28d), so a prose description whose last word is "ssh" is the likeliest shape of this leak, and it is exactly the shape the pattern misses. Uppercase `SSH` is also not a hit by design (trap row, `leak-scan.test.mjs:166`), so `SSH to the host` passes too. The header decision (`leak-scan.mjs:19-22`) says looser patterns hit vendored code, which holds for a bare `ssh`, but a punctuation-terminated form was not measured.
**Fix:** Keep the shell-word-list trap (`|split|ssh|stat|`) green by requiring whitespace or sentence punctuation after, and no `|` or word character before:
```js
['ssh-command', /(?<![|\w])ssh(?=[\s.,;:)\]"'])/g],
```
Add LEAKS rows for `over ssh.`, `ssh,` and `"ssh"`; keep the existing trap rows. Re-run the 1.60.0 build to confirm zero new hits in vendored code before committing (the baseline file measured only the spaced form).

### WR-03: A credential hit prints part of the credential into a public CI log and the step summary

**File:** `docs/architecture/site/leak-scan.mjs:88-92` (`printable`), `:126` (HIT line), `:62` (credential pattern), `:155` (summary)
**Issue:** The header says the report never prints the line around a match "because the CI log of a public repository is public", but it prints the match itself. For the `credential` target the match is the secret's own text. Verified with a fabricated 40-character `ghp_` token: the report line is `HIT credential a.js count=1 first=ghp_AbCdEfGhIjKlMnOpQrSt`, 24 of 40 characters, and the same text is appended to `$GITHUB_STEP_SUMMARY`. A pull-request run from a fork is public too. GitHub's log masking only covers registered secrets, not a token that happened to be in a model description. The same applies to the `AKIA` pattern (the whole key id is printed). The IP, host and user-name targets echo what was already in the public repository, so the exposure is specific to `credential`.
**Fix:** Print a length and a class for that target instead of the text:
```js
const REDACT = new Set(['credential'])
// in the loop
const shown = REDACT.has(name) ? `<${counted[0].length} chars>` : printable(counted[0])
hitLines.push(`HIT ${name} ${rel} count=${counted.length} first=${shown}`)
```
Update the `credential` rows in `leak-scan.test.mjs:95-99`, which assert `first=<the text>`, to expect the redacted form.

### WR-04: The fixtures cannot see the scan skipping files, so the hook's weaken-detection is blind to the failure the header forbids

**File:** `docs/architecture/site/leak-scan.test.mjs:201-212` (walk tests), `.githooks/pre-commit:240-246`, `scripts/pre-commit-likec4-selftest.py:496-504`
**Issue:** `leak-scan.mjs:16` states that no file is skipped, and the hook claims "a commit that weakens a target is refused here" (`pre-commit:199`). I applied mutants to scratch copies of the scan and ran the unchanged 81 fixtures:
- skip every entry whose name starts with `.` (`if (entry.name.startsWith('.')) continue` in `walk`): 81 pass, 0 fail.
- skip anything that is not a regular file (`else if (entry.isFile()) yield path`): 81 pass, 0 fail.
- accept a nested `robots.txt` (`rel.endsWith('robots.txt')`): 1 fail (good).
- drop `IPV6_SHORT`: 1 fail (thin but caught).

The walk tests cover a nested directory and a NUL-byte file only. The scan does read dotfiles and symlink targets today (verified: a `.hidden` file and a symlink to an outside file both produce `LEAK targets=ipv4`; a symlink to a directory or a dangling link exits 4, fail closed), but nothing pins it. Selftest case 20 proves only that removing the whole `ipv4` row is refused (`scripts/pre-commit-likec4-selftest.py:497-498`), not that a loosening of the walk or of a pattern is.
**Fix:** Add three fixtures to `leak-scan.test.mjs`: a leak in a dotfile, a leak behind a symlink to a regular file, and a dangling symlink expecting exit 4. Then add one more selftest mutation of a different kind (for example the dotfile skip) beside case 20 so the hook step is shown to refuse more than a deleted row. Run the mutants above against the suite again and record that each now fails.

### WR-05: The site probe's retry grace may be shorter than the site's Recreate outage (not measured)

**File:** `.github/workflows/uptime-check.yml:82-85` (ATTEMPTS, SLEEP_SECONDS), `:117-142` (loop); `k8s/base/architecture-site/architecture-site.yaml:17-18,50-54`
**Issue:** The new URL is the first probe whose backend is a single-replica `strategy: Recreate` Deployment that Flux rolls on every model change (each push to the model rebuilds the image and bumps the tag). A 503 or Traefik 404 returns fast, so the retry window is about `(ATTEMPTS - 1) x SLEEP_SECONDS = 20 s`. The readiness probe has `periodSeconds: 10` and no initial delay, so a new pod becomes Ready up to 10 s after its container starts, after the image pull. Plan 04-05 measured the app's Recreate window at 32 s (503 at 19:03:34Z, 200 at 19:04:06Z), longer than 20 s, and no phase file measures the site's window. A cron tick that lands inside a roll opens the `Uptime check failing` issue and emails the owner. Probability per roll is low (a cron every 15 minutes against a window of tens of seconds), but the false alarm is the thing the header says the file exists to prevent. I could not verify the site's actual window from the files; this is marked unverified.
**Fix:** Measure one site roll (time from 503 to 200 on the site URL during a Flux bump) and either note the measured window in the header beside hole (e), or give the site its own longer retry (for example a per-URL attempts argument of 5). If the measured window is under 20 s, record that in the header and close this finding.

## Info

### IN-01: The robots check is not tied to `User-agent: *`

**File:** `docs/architecture/site/leak-scan.mjs:134`
**Issue:** Any line equal to `Disallow: /` counts. Verified `OK` for `User-agent: Googlebot` plus `Disallow: /`, and for `Allow: /` followed by `Disallow: /`. The report then prints `ROBOTS disallow=yes`, which the docs repeat as "answers `Disallow: /`". Harmless for the LikeC4 default file (`User-agent: *` / `Disallow: /`, verified in the local build), and robots is not access control.
**Fix:** Require the group: match `/^user-agent:\s*\*\s*$/im` and a `Disallow: /` line after it with no intervening `Allow`, or soften the report label to `any Disallow: / line`.

### IN-02: Grafana share ids in the API form or bare are not targets

**File:** `docs/architecture/site/leak-scan.mjs:62`
**Issue:** Only `public-dashboards/` is listed. Verified `OK` for `/api/public/dashboards/9b5bf7c8d3c743e1868eed84c9d3e5df` and for the bare 32-hex id. PITFALLS 11 says to keep Grafana public-share URLs out of the site. Both README ids are public by design (README, `uptime-check.yml`), so there is no exposure today; the gap matters only if a different, private share id is pasted in.
**Fix:** Add `public\/dashboards\/` to the credential alternation (one pattern covers both forms) and a LEAKS row for the API form.

### IN-03: `drill.fish` header does not list D30b among the controls; `scan_build` exits mid-run

**File:** `docs/architecture/drill.fish:3-5` and `:79-82`
**Issue:** The header says the drills expected to stay clean are D5, D13b, D16b and D17b. `drills.tsv:54` adds D30b (control), which must read `scan exit=0`. Separately `scan_build` calls `exit 3` when likec4 is not installed, which aborts the whole harness after D1 to D29 have printed and leaves `out/drill-root` behind (the final `trash-put $scratch` at line 169 never runs). The harness also asserts nothing: a `sed` that stops matching (for example if `edge.c4:27` is reworded) prints `scan exit=0` for D30 with no failure, and only a manual diff against `drill-output.txt` shows it.
**Fix:** Add D30b to the header sentence. Replace `exit 3` with `return 3` plus a skipped marker line so the table finishes and the scratch is trashed. Optionally have the `scan` kind check that the sed row before it changed the file.

### IN-04: Selftest does not pin where the leak step runs, and its `finding` assertion does not discriminate

**File:** `scripts/pre-commit-likec4-selftest.py:262-264`, `:279-291`, `:415-421`
**Issue:** `committed_with_check` (cases 6 to 8) and `committed_without_step` (case 11) never test for `LEAK_BANNER`. A regression that moves the leak step out of `if [ -n "$LIKEC4_TRIGGERS" ]` would make Node a prerequisite of every commit (the thing the hook header at `pre-commit:207-209` says must not happen) and case 11 would still pass, because it only checks that the drift banner is absent. In `refused_for_leak_tests`, `finding in plain` (`"ipv4"`) is also satisfied by a passing test name, because `node --test` prints passing names (one line, `ipv4: host 10.20.30.40 here fails the scan`, appears in a clean run); the earlier `LEAK_REFUSED_TEXT` and exit-code asserts do the real work.
**Fix:** In `committed_with_check` assert `LEAK_BANNER in plain`; in `committed_without_step` assert `LEAK_BANNER not in plain`. In `refused_for_leak_tests` assert on a failure marker, for example `"✖ ipv4:"` or `"not ok"` followed by the target name.

### IN-05: The hook comment says the fixture run takes "about a second"; it measured 4.07 s

**File:** `.githooks/pre-commit:197-198`
**Issue:** `node --test` reported `duration_ms 4074.04` for 81 tests (each spawns the scan). The comment understates the cost of an extra gate on every model, k8s and migration commit.
**Fix:** Say "about 4 seconds".

### IN-06: Encodings and spellings the scan does not read

**File:** `docs/architecture/site/leak-scan.mjs:55` (ipv4), `:117` (latin1 decode)
**Issue:** Verified `OK` for a UTF-16LE file containing `10.20.30.40`, for `010.020.030.040`, for `x.10.20.30.40` (a dot-prefixed quad is excluded by the `(?<![0-9.])` guard that exists for SVG numbers), for `10.20.30.40`, and for a base64 string of `ssh root@10.20.30.40`. LikeC4 emits UTF-8 with literal ASCII, so none of these is a realistic route today; recorded so the header's "no file is skipped" is not read as "every encoding is decoded".
**Fix:** Add one sentence to the header decisions (`leak-scan.mjs:15-23`) naming that only ASCII spellings in UTF-8 or Latin-1 bytes are matched.

### IN-07: Local `site/out` mixes two builds

**File:** `docs/architecture/site/out` (untracked build directory)
**Issue:** `likec4 build -o site/out` does not clean an existing directory. The local `out` holds 33 files with pairs of hashed assets from two builds (for example `AIChat-BaZgjqqN.js` at 16:41 and `AIChat-D0h1V84L.js` at 16:47), against the 22 files in the baseline (`04-scan-baseline.txt:43`). A local scan result therefore describes both builds. CI is unaffected (fresh checkout), and the drill builds into a fresh scratch directory.
**Fix:** Say in the leak-scan header or the runbook that a local scan needs `trash-put docs/architecture/site/out` first, or have the local recipe build to a fresh directory.

---

_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
