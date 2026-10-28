---
phase: quick-261028-rlq
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - docs/architecture/drill.fish
  - docs/architecture/sed-drills.tsv
  - .planning/workstreams/architecture-views/quick/261028-rlq-refactor-drill-fish-tsv-for-one-sed-edit/261028-rlq-mutation.txt
autonomous: true
requirements: [GATE-01]

estimate:
  tokens: 45000
  raw_tokens: 45000
  tasks: 2
  confidence: low

must_haves:
  truths:
    - "`fish docs/architecture/drill.fish < /dev/null > docs/architecture/out/rlq-run.txt 2>&1` exits 0 and `cmp` finds the file identical to docs/architecture/drill-output.txt, which is not edited: same 36 sections in the same order, the same `exit=N` line in each"
    - "The 15 drills that are one `sed -i` on one file (D2, D6, D6b, D8, D9, D10, D10b, D12, D18, D19, D20, D21, D22, D23, D24) are the 15 data rows of docs/architecture/sed-drills.tsv, header `id title file sed` tab-separated, and none of them is still written out in drill.fish"
    - "drill.fish runs them through 6 sed_drills calls placed where their blocks were (D2 | D6 D6b | D8 | D9 D10 D10b | D12 | D18 to D24), and the ids called equal the TSV ids in the same order, with none missing, duplicated or uncalled"
    - "The other 21 sections (baseline, D1, D3, D4, D5, D7, D7b, D7c, D8b, D11, D15, D16, D16b, D12b, D13, D13b, D14, D14b, D14c, D17, D17b) are unchanged, and so are fresh, run, certmgr_into, lonely_into and labeldrill"
    - "sed_drills hands the expression to `sed -i --sandbox -e` as one argument from a variable, never re-parsed by fish; an id with no row prints a message naming it on stderr and exits 2 instead of reaching `string split` with no argument"
    - "The oracle is shown to read the TSV: a no-op mutation of the D24 row makes the replay differ (D24 goes from `exit=1` to `exit=0`), recorded in 261028-rlq-mutation.txt, and the TSV is restored from git afterwards"
    - "`fish -n` and `fish_indent --check` pass on drill.fish; the header comment says which drills live in sed-drills.tsv and stays true; `python3 scripts/verify-comments.py check` passes with the TSV tracked (.tsv is in its scope)"
  artifacts:
    - path: docs/architecture/sed-drills.tsv
      provides: "id, title, file relative to the scratch root, sed expression; one row per one-sed drill"
    - path: docs/architecture/drill.fish
      provides: "sed_drills runner plus the 21 drills that need printf, find, mv, trash-put, several seds or a helper"
  key_links:
    - from: docs/architecture/drill.fish
      to: docs/architecture/sed-drills.tsv
      via: "sed_drills reads $here/sed-drills.tsv by id with string match, splits with string split -m3 on a tab"
    - from: docs/architecture/drill.fish
      to: docs/architecture/drill-output.txt
      via: "byte-for-byte cmp of the replay, stdout and stderr together"
  prohibitions:
    - statement: "MUST NOT edit docs/architecture/drill-output.txt, sync.mjs or anything under docs/architecture/model, and MUST NOT reorder, add, remove or retitle a drill"
      category: scope
      verification: "git diff --stat 7321bd1 -- docs/architecture lists only drill.fish and sed-drills.tsv"
    - statement: "MUST NOT pass a TSV field through the fish eval builtin or any other re-parse"
      category: security
      verification: judgment
---

<objective>
Move every drill in docs/architecture/drill.fish that is a single `sed -i` edit on one file into a
tab-separated data file, docs/architecture/sed-drills.tsv (id, title, file, sed expression), read by
a sed_drills loop that splits each row with `string split`. Drills needing printf, find, mv,
trash-put, several commands or a helper stay written out. The replay must stay byte-identical to
docs/architecture/drill-output.txt.

Purpose: 15 near-identical four-line blocks (echo, fresh, sed, run) become 15 data rows, so adding a
one-sed drill is one TSV line, while the drill harness that evidences GATE-01 keeps proving the same
thing.

Output: docs/architecture/sed-drills.tsv, a shorter docs/architecture/drill.fish, and the mutation
record 261028-rlq-mutation.txt in this quick directory.
</objective>

## Corrections to the brief, checked against drill.fish at 7321bd1

The brief listed D6, D6b, D9, D10b, D12 and D20 to D24 as the one-sed drills and called D8 and D10
multi-command. Read line by line, five more are a single `sed -i` on one file: D2 (line 75, on
KafkaTopics.java), D8 (line 121, `1a import ...` on KafkaTopics.java), D10 (line 137, a range delete
on deployment.c4), D18 (line 215, on views.c4) and D19 (line 220, on deployment.c4). Counted: 36
`echo "== ` headers and 21 top-level `sed -i` lines in the file; 15 one-sed drills use 15 of them,
D8b uses 1 (after a find), D11 uses 2 and D12b uses 3, total 21. The brief's order list also skipped
D14b and D17 to D19; the file order is authoritative.

## Approach and trade-offs (CLAUDE.md GSD directive)

Two alternatives were weighed against the chosen one.

| Approach | Pros/Cons | Why Picked |
|---|---|---|
| Function per drill, called in order | Pro: one shape for all 35 drills. Con: about 35 wrappers of 3 lines each, and the one-sed drills stay code, so nothing becomes data | Not picked: the user chose the TSV, and it adds lines without adding behaviour |
| TSV rows run by sed_drills, called by id where each block stood (picked) | Pro: 15 blocks of 4 to 5 lines become 15 rows and 6 call lines, the drill order and output are kept, and a new one-sed drill is one row. Con: a reader follows a call into a data file, and the expressions are no longer fish-quoted in place | Picked: the user's decision, and the only shape that moves all 15 without reordering |
| Leave drill.fish as it is | Pro: zero risk. Con: 60 lines of repeated echo/fresh/sed/run | Not picked: the user asked for the refactor |

Keeping position: one loop over the whole TSV at the end would run the 15 after D17b and reorder the
output, so the oracle forbids it. Moving only the trailing consecutive run D18 to D24 keeps a single
loop but leaves 8 one-sed drills inline. So sed_drills takes ids and is called at each run of
consecutive one-sed drills; it looks each id up rather than looping over all rows, so the call line
decides what runs and in which order.

Non-obvious costs: sed_drills re-reads a 16-line file once per id (15 reads), which is noise beside
36 `node sync.mjs` runs (the baseline replay took 69 s on 2026-10-28). Security: a TSV row reaches GNU
sed, whose `e`, `r` and `w` commands could run a shell command or write a file, and a data row is
skimmed in review more easily than code; `--sandbox` closes that at no cost (measured at planning:
sed 4.9 rejects `s|a|b|w /dev/stdout` with "e/r/w commands disabled in sandbox mode", exit 1, and
accepts the D6b expression with `-i`). An id with no row would leave `string split` with no argument,
and it then reads stdin, which hangs on a terminal; the status check before the split prevents that.

Data flow, in three sentences: drill.fish calls sed_drills with drill ids at the place those drills
always ran. For each id, `string match` picks the one TSV line that starts with the id and a tab,
`string split -m3` cuts it into id, title, file and expression, and the expression reaches sed as a
single argument from a variable. sed edits the scratch copy, and run prints the check result exactly
as the written-out block did.

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/workstreams/architecture-views/STATE.md
@.claude/CLAUDE.md
@docs/architecture/drill.fish
@docs/architecture/drill-output.txt

Facts measured at planning (fish 4.2.1, GNU sed 4.9, HEAD 7321bd1; drill.fish and drill-output.txt
last changed in 1d6c40e):
- Baseline replay is identical to the oracle (cmp exit 0) and takes about 69 s.
- `fish -n` and `fish_indent --check` both exit 0 on drill.fish at HEAD.
- In fish, `set row (string match -- $id\t'*' <file)` sets status 1 when no line matches, and the
  pattern D6 plus a tab does not match a D6b row.
- `string split -m3 \t -- $row` on a probe row yields exactly 4 fields with `\n`, `$`, `|` and `#`
  kept literally.
- What sed received for the two double-quoted drills, printed by fish itself:
  - D12: s|^      source 'k8s/platform/traefik'|      sourc 'k8s/platform/traefik'|
  - D18: s|^\(    client <- traefik '200 {id, email, displayName, theme}'\)$|\1\n    kanban.api -> redpanda.activity 'probe'|
- scripts/verify-comments.py lints `.tsv` files line-wise: only a line whose first non-blank
  character is `#` counts as a comment. TSV rows start with an id, so the file must hold no `#`
  comment line.
- Spotless covers Java only; docs/architecture/out/ is gitignored.
- Dry run at planning, in gitignored probe files since trashed: the sed_drills shape described in
  Task 1 passed `fish_indent --check`; a printf generator fed the original quoted D18 and D12
  arguments produced rows whose edits on copies of views.c4 and model.c4 were byte-identical to the
  inline seds, and changed the files; the Task 1 guard command printed the D99 message and `exit=2`.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Tracer, D18 end to end through sed-drills.tsv and sed_drills</name>
  <files>docs/architecture/sed-drills.tsv, docs/architecture/drill.fish</files>
  <read_first>docs/architecture/drill.fish (whole file, 248 lines), docs/architecture/drill-output.txt lines 144 to 150</read_first>
  <action>
Wire one drill, D18, through every layer: data row, runner, call site, oracle. D18 is the tracer
because its original argument is the hardest to carry: double-quoted, with escaped parens, an
escaped dollar sign, a back-reference, a newline escape, single quotes and braces.

1. Build the TSV mechanically so fish does the unquoting exactly as it did inline, and the tabs are
real. Write a throwaway generator at docs/architecture/out/rlq-gen.fish (gitignored) whose lines are
printf calls with the format '%s\t%s\t%s\t%s\n': first one with the four words id, title, file and
sed; then one for D18 whose arguments are D18, the title in single quotes (a scenario view gets a
sixth participant), model/views.c4, and the sed argument copied verbatim from drill.fish line 215
with its surrounding double quotes. Run fish docs/architecture/out/rlq-gen.fish redirected to
docs/architecture/sed-drills.tsv. A generator is used instead of Edit because Edit cannot guarantee
tab bytes, and copying the original quoted argument makes fish produce the same bytes sed received.
Keep the generator for Task 2. Do not put a comment line in the TSV.

2. In drill.fish, after the labeldrill function and before the baseline echo, add a function
sed_drills with --inherit-variable here and --inherit-variable scratch. For each id in argv: set row
to the output of string match with the pattern made of the id, a tab and a star, reading stdin
redirected from $here/sed-drills.tsv; on the next line, `or begin`, echo "sed_drills: no row for
$id in sed-drills.tsv" to stderr, exit 2, end. The check must come before the split, because string
split with no argument reads stdin and hangs on a terminal. Then set f to string split -m3 on a tab
over row (the -m3 keeps any later tab inside the expression), echo "== $id: $f[2]", call fresh, run
sed -i --sandbox -e $f[4] $scratch/$f[3] (per the security note in the plan), then call run. Do not
use the eval builtin or any construct that re-parses a field.

3. Above the function put a plain-text comment (CODE_STYLE.md rule 14: bare identifier names, no
backticks) in three lines or fewer, saying: it runs the named drills from their rows in
sed-drills.tsv, whose columns are id, title, the file to edit relative to the scratch root, and the
sed expression; and the expression is passed to sed as one argument that fish never parses again,
so the quotes, dollar signs and backslashes in it reach sed as written.

4. Replace the D18 block (its echo line, fresh, sed and run, lines 213 to 216 at HEAD) with the single
line sed_drills D18, keeping one blank line on each side. Anchor the edit on the echo text, not on
line numbers. Leave the header comment for Task 2: after this task only D18 is in the TSV, so a
sentence saying the one-sed drills live there would be false.

5. Run fish_indent -w docs/architecture/drill.fish if the indent check fails, then the verify
commands. Commit only the two files with git commit -m "refactor(drill): run D18 from sed-drills.tsv
through sed_drills" -- docs/architecture/drill.fish docs/architecture/sed-drills.tsv. `--no-verify` is
permitted per CLAUDE.md (no Java, migration or k8s manifest, and every TSV value already passed
gitleaks inside drill.fish), but first git add both paths and run python3 scripts/verify-comments.py
check, since the lint reads tracked files only.
  </action>
  <verify>
    <automated>fish -n docs/architecture/drill.fish && fish_indent --check docs/architecture/drill.fish</automated>
    <fails_when>non-zero exit from either command: a syntax error, or formatting fish_indent would change</fails_when>
    <automated>awk -F'\t' '{print NR, NF, $1}' docs/architecture/sed-drills.tsv && awk -F'\t' '$1=="D18" {print $4}' docs/architecture/sed-drills.tsv</automated>
    <fails_when>the first lines are not exactly `1 4 id` and `2 4 D18`, or the last line differs by any byte from the D18 literal in the plan's context section (an escaped dollar left as backslash-dollar, a lost backslash before the parens, or a space where a tab should be)</fails_when>
    <automated>fish -c 'function fresh; end; function run; end; set here docs/architecture; set scratch docs/architecture/out/none; source (sed -n "/^function sed_drills/,/^end/p" docs/architecture/drill.fish | psub); sed_drills D99' < /dev/null; echo "exit=$?"</automated>
    <fails_when>the last line is not `exit=2`, no stderr line names D99, or the command hangs (the status check sits after the split)</fails_when>
    <automated>fish docs/architecture/drill.fish < /dev/null > docs/architecture/out/rlq-run.txt 2>&1 && cmp docs/architecture/out/rlq-run.txt docs/architecture/drill-output.txt</automated>
    <fails_when>non-zero exit: the script failed, or cmp reports a first differing byte (look at the D18 section first, oracle line 144)</fails_when>
  </verify>
  <done>sed-drills.tsv holds the header and the D18 row; drill.fish has sed_drills with its comment and one call, sed_drills D18, in D18's old place; the replay is byte-identical; a missing id exits 2 without hanging; the two files are committed.</done>
</task>

<task type="auto">
  <name>Task 2: Move the other 14 one-sed drills, update the header, prove the oracle reads the TSV</name>
  <files>docs/architecture/sed-drills.tsv, docs/architecture/drill.fish, .planning/workstreams/architecture-views/quick/261028-rlq-refactor-drill-fish-tsv-for-one-sed-edit/261028-rlq-mutation.txt</files>
  <read_first>docs/architecture/drill.fish (as left by Task 1), docs/architecture/out/rlq-gen.fish</read_first>
  <action>
1. Extend docs/architecture/out/rlq-gen.fish so that, after the header, it prints one row per drill
in drill order: D2, D6, D6b, D8, D9, D10, D10b, D12, D18, D19, D20, D21, D22, D23, D24. For each row
the id, the title in single quotes copied from that drill's echo line (the text after "== Dn: "), the
file as the path after $scratch/ on its sed line, and the sed argument copied verbatim with its
original quotes (single for all but D12 and D18, which are double). HEAD line numbers of the sed
lines for orientation: D2 75, D6 96, D6b 101, D8 121, D9 132, D10 137, D10b 142, D12 168, D19 220,
D20 225, D21 230, D22 235, D23 240, D24 245; D18's argument is already in the generator. Regenerate
docs/architecture/sed-drills.tsv from it, then trash-put the generator.

2. Replace each run of consecutive one-sed blocks in drill.fish with one call line, anchored on the
echo texts, keeping one blank line between sections: the D2 block becomes sed_drills D2; the D6 and
D6b blocks become sed_drills D6 D6b; D8 becomes sed_drills D8; D9, D10 and D10b become sed_drills D9
D10 D10b; D12 becomes sed_drills D12; and the existing sed_drills D18 line plus the D19 to D24 blocks
become sed_drills D18 D19 D20 D21 D22 D23 D24. Do not touch D8b, D11, D12b or any other section, and
do not change any helper.

3. Keep the header comment true. After the line ending "changes the scratch copy." add a blank
comment line, then a paragraph in plain text (rule 14) saying: a drill that is one sed edit on one
file is a row of sed-drills.tsv, run by a sed_drills call at its place below, so the drills run in
the order drill-output.txt records; drills that write or delete a file, edit more than once or call
a helper are written out in full. Then a blank comment line before the SYNC paragraph. Change no
other header sentence.

4. Run fish_indent -w if needed, then the verify commands up to and including the comment lint. Then
commit both paths as in Task 1 with the message "refactor(drill): move the one-sed drills into
sed-drills.tsv", after git add and the comment lint, using git commit -m ... -- with the two paths.

5. Prove the oracle reads the TSV, after the commit so git restores it. Apply a no-op mutation to
the D24 row: sed -i 's/nonprodx|g$/nonprod|g/' docs/architecture/sed-drills.tsv, which makes it
replace the host with itself. Replay into docs/architecture/out/rlq-mut.txt the same way as the
oracle verify, then write 261028-rlq-mutation.txt in this quick directory with three parts: the
mutation command, the line "cmp exit: N" from cmp -s of the mutant replay against drill-output.txt,
and the output of diff docs/architecture/out/rlq-mut.txt docs/architecture/drill-output.txt. Restore
with git checkout -- docs/architecture/sed-drills.tsv. Expected: cmp exit 1 and a diff confined to
the D24 section, where the mutant side shows exit=0. If cmp exits 0, stop and report it: the oracle
would not be reading the TSV.

6. Clean up: trash-put docs/architecture/out/rlq-run.txt and docs/architecture/out/rlq-mut.txt.
Leave the mutation record for the quick task's own docs commit.
  </action>
  <verify>
    <automated>fish -n docs/architecture/drill.fish && fish_indent --check docs/architecture/drill.fish</automated>
    <fails_when>non-zero exit from either command</fails_when>
    <automated>fish docs/architecture/drill.fish < /dev/null > docs/architecture/out/rlq-run.txt 2>&1 && cmp docs/architecture/out/rlq-run.txt docs/architecture/drill-output.txt</automated>
    <fails_when>non-zero exit: the script failed or exited 2 on a missing row, or cmp reports a differing byte (a wrongly unquoted expression, a wrong file column, or a drill out of order)</fails_when>
    <automated>rg -c '^echo "== ' docs/architecture/drill.fish; rg -c '^sed -i' docs/architecture/drill.fish; rg -c '^sed_drills ' docs/architecture/drill.fish</automated>
    <fails_when>the three counts are not 21, 6 and 6 (36, 21 and 0 at HEAD 7321bd1, 35, 20 and 1 after Task 1)</fails_when>
    <automated>awk -F'\t' 'NR>1 {n++; if (NF!=4) bad++} END {print n, bad+0}' docs/architecture/sed-drills.tsv</automated>
    <fails_when>prints anything but `15 0`: a row is missing or extra, or a row does not have exactly four tab-separated fields</fails_when>
    <automated>diff <(cut -f1 docs/architecture/sed-drills.tsv | tail -n +2) <(rg -N '^sed_drills ' -r '' docs/architecture/drill.fish | tr ' ' '\n')</automated>
    <fails_when>any output or non-zero exit: a TSV row nobody calls, a called id with no row, a duplicate id, or the two orders differ</fails_when>
    <automated>sed -n '1,20p' docs/architecture/drill.fish | rg -c 'sed-drills.tsv' && python3 scripts/verify-comments.py check</automated>
    <fails_when>the count is 0 (the header does not name the TSV), or the lint exits non-zero or prints anything other than an `OK:` line</fails_when>
    <automated>R=.planning/workstreams/architecture-views/quick/261028-rlq-refactor-drill-fish-tsv-for-one-sed-edit/261028-rlq-mutation.txt; rg -c '^cmp exit: 1$' $R && rg -c '^< +exit=0$' $R && git status --porcelain -- docs/architecture/sed-drills.tsv docs/architecture/drill.fish</automated>
    <fails_when>either count is not 1 or rg exits non-zero (the mutation went unseen or the record is missing), or git status prints a line (the TSV was not restored after the mutation, or the refactor is uncommitted)</fails_when>
  </verify>
  <done>All 15 one-sed drills are TSV rows run by 6 sed_drills calls in their original places; the other 21 sections are untouched; the header names the TSV; the replay is byte-identical and exits 0; the mutation record shows the oracle catching a no-op D24 row; the TSV is restored and both files are committed; the scratch files in docs/architecture/out are gone.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| sed-drills.tsv to sed | A repo data file's fields become a sed program and a path under the scratch copy, run on the developer's machine |
| sed-drills.tsv to fish | A field must never be parsed as fish code |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-rlq-01 | Elevation of privilege | sed_drills sed call | low | mitigate | `sed -i --sandbox -e` rejects the e, r and w commands, so a row cannot run a shell command or write a file other than the one edited in place (measured at planning) |
| T-rlq-02 | Tampering | sed_drills field handling | low | mitigate | Fields pass as variable expansions, which fish does not re-parse; no eval builtin; the D18 row with parens, dollar, back-reference and quotes is the tracer that proves it byte for byte |
| T-rlq-03 | Denial of service | sed_drills lookup | low | mitigate | A missing id fails the string match status check and exits 2 before string split can read a terminal; Task 1 tests it with D99 under stdin from /dev/null |
| T-rlq-04 | Tampering | TSV file column | low | accept | A path with `..` could reach the real model, exactly as a hand-written `$scratch/..` path could before; the row is reviewed in the same diff as the script, and git status after a replay would show the edit |
| T-rlq-SC | Tampering | npm/pip/cargo installs | low | accept | No package is installed by this plan |
</threat_model>

<verification>
- `fish docs/architecture/drill.fish < /dev/null > docs/architecture/out/rlq-run.txt 2>&1 && cmp docs/architecture/out/rlq-run.txt docs/architecture/drill-output.txt` exits 0 (about 70 s).
- `git diff --stat 7321bd1 -- docs/architecture` lists only drill.fish and sed-drills.tsv.
- 261028-rlq-mutation.txt shows `cmp exit: 1` and a diff confined to the D24 section.
</verification>

<success_criteria>
- Byte-identical replay, same drill order, same per-drill exit codes, script exit 0.
- 15 TSV rows, 6 sed_drills calls, 21 written-out sections, ids cross-checked in both directions.
- fish -n, fish_indent --check and the comment lint pass; the header and the sed_drills comment are true.
- The oracle is proven to read the TSV (mutation red), then restored (green).
</success_criteria>

<output>
Create `.planning/workstreams/architecture-views/quick/261028-rlq-refactor-drill-fish-tsv-for-one-sed-edit/261028-rlq-SUMMARY.md` when done
</output>
