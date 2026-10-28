---
phase: quick-261028-opl
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - docs/architecture/model/monitoring.c4
  - docs/architecture/drill-output.txt
  - .planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt
  - docs/DIAGRAM_CONVENTIONS.md
  - docs/architecture/model/scenarios-app.c4
  - .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md
  - .planning/workstreams/architecture-views/phases/02-model-completion/02-04-401-measure.txt
  - .planning/workstreams/architecture-views/phases/02-model-completion/02-VERIFICATION.md
autonomous: true
requirements: [PHYS-03, SCEN-02, STD-01]

estimate:
  tokens: 90000
  raw_tokens: 90000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "The monitoring view draws a Prometheus scrape edge to the kubelet with cAdvisor, the Kubernetes API server, kube-state-metrics and the Prometheus Operator, and also to CoreDNS and Grafana: every default monitor of kube-prometheus-stack 91.5.2 that k8s/monitoring/controllers/kube-prometheus-stack.yaml leaves on and LikeC4 can draw. Gate G3c prints 13 Prometheus edges in `monitoring`, the 7 earlier ones unchanged"
    - "Each new edge rests on a values.yaml line at tag kube-prometheus-stack-91.5.2, recorded with the line number and verbatim text in 261028-opl-chart-values.txt along with the byte count and sha256 of the file read. A title names a port or a protocol only where a values line states it (9153 for CoreDNS at line 2233, HTTPS for the kubelet at line 1900). The API server title names no protocol: its tlsConfig block (1741 to 1747) sets TLS client fields, but no values line in the kubeApiServer block states the scheme, and the templates that set it are not read"
    - "No edge is drawn for a monitor the repo turns off (controller-manager, scheduler, etcd and kube-proxy at kube-prometheus-stack.yaml lines 45 to 52, Alertmanager at lines 37 to 38) or one the chart leaves off (kubeDns, thanosRuler, windowsMonitoring). The record lists each of them with its reason"
    - "The Prometheus self-scrape (prometheus.serviceMonitor.selfMonitor true) is stated in the record, the view description and the standard, not drawn: LikeC4 1.60 rejects a relationship from an element to itself with `model does not validate: Invalid parent-child relationship`, measured at planning and again at execution"
    - "The monitoring view description, the prometheus element description, and in docs/DIAGRAM_CONVENTIONS.md the Physical row, the Checks paragraph and the `monitoring` table row say what is now drawn, and none of the earlier sentences saying the chart defaults are not drawn remains (gates N1 to N6)"
    - "`sync.mjs check` exits 0 with 61 elements and 17 workloads. The deployment views print monitoring 25 17, production 24 17 and delivery 14 8, with no `[...]` label. drill-output.txt is re-recorded so that only the element counts in its 33 tally lines change (29 at 61, 4 at 62). The replay is byte-identical and the self-test passes 16/16. No sync.mjs rule is added or weakened, and no drill is added"
    - "The error-401 view description, its section in 02-SCENARIO-EVIDENCE.md, note (b2), the Scenarios row and the `error-401` table row say that no Set-Cookie, no spring_session row and no statement in the capture windows were observed at 7bccf71 (commit 2fbe8ff). Each keeps the pre-fix measurement (82775fa, verdict b) and names what was not observed: only /api/boards GET and POST, one run on one machine, no proxy, no Redpanda, no stale session cookie, plain HTTP, and statements captured only in a 1-second window after each request plus one log-tail read after rows A to I, with the whole log not re-read, so a statement outside those windows is not ruled out (record Method lines 182 to 183 and limits lines 238 to 240, planning numbering). None of the five still carries the inference label, and each names the capture window (gates I1 to I5)"
    - "02-04-401-measure.txt loses no line and gains exactly one pointer line (M1 prints `1 0`). 02-VERIFICATION.md gains a dated addendum saying the user chose to draw, whose criterion 3 sentence names the capture windows (V3): its frontmatter is byte-identical, it has no override and no accepted_by entry, and its status is left for the next verifier run"
  prohibitions:
    - statement: "MUST NOT draw an edge for a monitor that the repo turns off or that the chart values at 91.5.2 do not enable, and MUST NOT put a port, path or protocol in an edge title unless a values line states it"
      category: transparency
      verification: judgment
    - statement: "MUST NOT claim that the live target list was read or that the chart was rendered (no cluster access, no helm binary)"
      category: transparency
      verification: judgment
    - statement: "MUST NOT claim more than the post-fix re-measure observed: other routes, other methods, a proxy, Redpanda, TLS and a stale or foreign cookie were not tried, and a statement outside the per-request capture windows and the one log-tail read is not ruled out"
      category: transparency
      verification: judgment
    - statement: "MUST NOT write an override, an accepted_by entry, a status or a score into 02-VERIFICATION.md frontmatter"
      category: scope
      verification: test
    - statement: "MUST NOT change Java, migrations, k8s manifests, scripts/, sync.mjs, drill.fish, views.c4 or ROADMAP.md, and MUST NOT add or weaken a drift rule"
      category: scope
      verification: test
    - statement: "MUST NOT write a cookie value, session id, password, IP literal, host name or local measurement port into model text, and MUST NOT copy the chart authorization or credentials lines into the record"
      category: privacy
      verification: test
  artifacts:
    - path: "docs/architecture/model/monitoring.c4"
      provides: "five new elements, six new scrape edges, their instances and the widened monitoring view"
      contains: "kube-state-metrics"
    - path: ".planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt"
      provides: "chart values at the pinned tag, line by line, with digest, repo overrides, the self-relationship probe and the limits"
      contains: "7d089fcf435e6904cda5ad246602e1c61005d08c68e3a9ab82770c249e395081"
    - path: "docs/architecture/drill-output.txt"
      provides: "drill replay re-recorded for the new element count"
      contains: "61 elements"
    - path: "docs/architecture/model/scenarios-app.c4"
      provides: "error-401 description stating the post-fix observation"
      contains: "observed at 7bccf71"
    - path: ".planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md"
      provides: "error-401 provenance with the post-fix observation and its limits"
      contains: "2fbe8ff"
    - path: "docs/DIAGRAM_CONVENTIONS.md"
      provides: "Physical, Checks, monitoring and 401 text matching what is drawn and observed"
      contains: "Five monitoring elements"
    - path: ".planning/workstreams/architecture-views/phases/02-model-completion/02-VERIFICATION.md"
      provides: "dated addendum answering human item 1"
      contains: "261028-opl-chart-values.txt"
  key_links:
    - from: "each new prometheus scrape edge in monitoring.c4"
      to: "its values.yaml line at tag kube-prometheus-stack-91.5.2"
      via: "the table in 261028-opl-chart-values.txt, named in the view description and the standard"
      pattern: "261028-opl-chart-values\\.txt"
    - from: "kube-state-metrics and prometheus-operator elements"
      to: "HelmRelease/kube-prometheus-stack in k8s/monitoring/controllers/kube-prometheus-stack.yaml"
      via: "metadata workload, held by the existing checkPlacement and checkDeployedWorkloads rules"
      pattern: "workload 'HelmRelease/kube-prometheus-stack'"
    - from: "error-401 description and its evidence section"
      to: "02-04-401-measure.txt section Post-fix re-measure (HEAD 7bccf71), committed in 2fbe8ff"
      via: "line cites of rows A to I, POST-FIX RESULT and the limits list"
      pattern: "POST-FIX RESULT"
    - from: "drill-output.txt"
      to: "the 518eb75 recording"
      via: "gate G7r: diff with the element counts normalized is empty"
      pattern: "elements"
---

<tradeoffs>
## Approaches considered (project directive: decided before this PLAN.md)

Which targets to draw (planning read the full values.yaml at the tag: 218,683 bytes, sha256 7d089fcf435e6904cda5ad246602e1c61005d08c68e3a9ab82770c249e395081):

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A: draw only the four named targets (kubelet with cAdvisor, API server, kube-state-metrics, operator) | Pro: the literal list in the scope. Con: the same file turns on two more default monitors that the repo does not touch: `coreDns.enabled: true` (line 2230, service port 9153 at 2233, ServiceMonitor on at 2243) and `grafana.serviceMonitor.enabled: true` (line 1700). Criterion 1 ("an edge to each thing they scrape") would still fail on two drawable targets, and the verifier would raise the same human item again. | Rejected. |
| B (picked): draw every default monitor the repo leaves on that LikeC4 can draw (the four, plus CoreDNS and Grafana); state the Prometheus self-scrape (line 4344) | Pro: meets the criterion as worded, with one stated exception that has a measured cause. The four were the list a read truncated at 62,574 characters could see; the user chose to draw the chart defaults. Con: two more edges than the scope named. The SUMMARY must say so plainly. | It is the outcome the user chose: the chart defaults drawn so criterion 1 holds. |

How to model the targets:

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A (picked): five separate elements of the existing kind `system`. `kube-state-metrics` and `prometheus-operator` carry `workload 'HelmRelease/kube-prometheus-stack'` and run in `monitoring`, as node-exporter and Grafana do. `kubelet` and `kube-apiserver` sit on the cluster node `netcup.k3s`, as the ClusterIssuers do. `coredns` sits in `kube-system`, as `svclb` does | Pro: one edge per thing scraped, each with its own title. Every placement follows a precedent already in the model, and nothing new enters spec.c4 or the standard. Measured at planning on a scratch copy: check exits 0 with 61 elements, monitoring becomes 25 nodes and 17 edges, production stays 24 and 17 (the CoreDNS instance has no edge there, so it is not drawn). Con: three authored elements that no check anchors. | Smallest change that draws each target, and every rule that fires is satisfied honestly. |
| B: one shared external element "k3s control plane" for the API server, kubelet and CoreDNS | Pro: two fewer elements. Con: checkEdgeLabels allows one title per instance pair (a differing title prints `[...]` and fails the check), so three scrapes collapse into one edge. It also merges a pod (CoreDNS) with the server process. | Rejected: the criterion counts things scraped, and B draws one where there are three. |
| C: a new spec kind (for example `controlplane`) | Pro: a distinct shape for cluster internals. Con: new vocabulary in spec.c4 and the standard, for which `svclb` already sets the precedent (`system`, "Authored."). | Rejected. |

Drift rule or drawing only:

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A (picked): drawing only, with no new rule and no new drill | Pro: each new element falls under a rule that was already drilled red. The two HelmRelease-backed elements fall under checkDeployedWorkloads (D19) and checkPlacement (D11, D16, D21); a planning probe that added a second kube-state-metrics instance in kube-system got `placement: kube-state-metrics is deployed in [kube-system,monitoring] ... manifests place it in [monitoring]`, exit 1. Every new instance falls under view membership (D13), every new edge under checkEdgeLabels (D17), every `source` under the exists check (D4). "Every new rule seen red first" is vacuous because there is no new rule. Con: no check reads the chart defaults, so a chart bump can stale the drawn set silently. | The scope is text and drawing, and the residual risk is stated, not hidden. |
| B: a sync.mjs rule that derives the expected scrape edges from the HelmRelease values | Pro: a chart default switched off would fail the check. Con: the defaults are not in the repo. The rule would either hardcode the chart monitor list, which mirrors the model and goes stale on a bump without failing, or vendor a 218 KB values file. It would also need its own red drill. | Rejected; the SUMMARY files the chart-bump seam as a candidate tracked item. |

The 401 statements (item 2):

| Approach | Pros / Cons | Why picked |
|---|---|---|
| A (picked): rewrite the five current-claim sites (view description, evidence section, note (b2), Scenarios row, `error-401` row) to "observed at 7bccf71" with the limits, keeping the pre-fix facts. In 02-04-401-measure.txt, insert one dated pointer line under the 261028-n5v follow-up heading | Pro: every reader-facing claim matches the record. No dated sentence is rewritten, and the record loses no line (M1 `1 0`). Con: the follow-up note keeps its inference sentence as history, flagged by the pointer. | True everywhere and keeps the history. |
| B: rewrite the follow-up note itself to say observed | Pro: no superseded sentence remains anywhere. Con: it would make a note dated before the re-measure claim what it could not have known, and it would delete lines from an append-only record. | Rejected. |
| C: leave the measure record untouched | Pro: the post-fix section already follows the note. Con: a reader who lands on the note through a cite (the n5v SUMMARY cites it) sees the inference with no pointer, and the scope asks for that note to be handled. | Rejected for one pointer line. |

## Non-obvious trade-offs

- **State invalidation.** The drawn defaults are pinned to chart 91.5.2 (kube-prometheus-stack.yaml line 25). A version bump fails no check, so the record keeps the digest of the file that was read. The repo-side line cites (45 to 52, 37 to 38) are positional and can drift the way the SecurityConfiguration.java cites did in 17a94cb.
- **Drill record.** The drill prints the check tally, so five new elements change all 33 tally lines (29 from 56 to 61, 4 from 57 to 62). Planning replayed the drills against a scratch model with the same five elements: with the element counts normalized the output was identical, so re-recording is required and is the only change. G7r proves it against 518eb75, the last commit that touched the file.
- **Legibility.** `monitoring` grows from 19 nodes and 11 edges to 25 and 17. The LikeC4 legibility rule is undefined, so the new PNG size is recorded without a verdict.
- **Fetch route.** Research rule W3 sends web pages through the web-access MCP, and rule W4 says to prefer a specialized tool. The previous pass's web-access fetch stopped at 62,574 of 218,683 characters. `gh api` (the authenticated GitHub CLI) returned the whole file in one call at planning. The download is untrusted data: it goes in its own new directory under the gitignored docs/architecture/out, is read only with sed and rg, and is trashed afterwards.
- **Security and publication.** The .c4 text is published in Phase 3. The new text names cluster components and one well-known port (CoreDNS 9153): no host, IP, credential or local measurement port (the 18080, 19081 and 15433 of the re-measure stay in the record only). The record does not copy the API server authorization block of values.yaml.
- **Commit hook.** Every path is under docs/ or .planning/, so `--no-verify` is allowed (project CLAUDE.md). This skips gitleaks and the LikeC4 step. L5 runs gitleaks 8.30.1 over each changed path, and G1 and G8 replace the LikeC4 step.
- **Performance.** Drill replay about 1 min 49 s (run in Task 1 to re-record and in Task 3 to replay), self-test about 43 s (twice), PNG export of every view about 3 min (once, in Task 3). Check and export take about 3 s each.
</tradeoffs>

<objective>
Close the last phase 02 gaps in text and drawing only, with no Java, migration, k8s manifest or app change.

(1) Criterion 1 / PHYS-03: the user chose to draw the kube-prometheus-stack default scrape targets instead of accepting the narrower scope. The monitoring Physical view gets Prometheus edges to the kubelet with cAdvisor, the API server, kube-state-metrics and the operator, which the scope named. It also gets edges to CoreDNS and Grafana, which the full values file shows the repo also leaves on (trade-off matrix: Approach B of "which targets"). The Prometheus self-scrape is stated, because LikeC4 cannot draw it. The record, the view description and the standard say what is drawn, what is turned off and what is not read. 02-VERIFICATION.md records the user's answer without an override.

(2) Criterion 3 / SCEN-02 / STD-01: the post-fix live re-measure (2fbe8ff) observed the cookie-less 401 at 7bccf71. The error-401 view, its evidence section, note (b2) and the two standard rows change from an inference to that observation. Each keeps the pre-fix history and names what was not observed, the limit of the statement capture included (a 1-second window per request plus one log-tail read after rows A to I).

Data flow in three sentences: the chart values at the pinned tag decide which edges `monitoring.c4` may draw, and the record ties each edge to its values line. LikeC4 parses the model, and `sync.mjs check` holds the new elements to the existing workload, placement, view-membership and edge-label rules, while the drill and the self-test re-run those rules on scratch copies. The prose files (descriptions, evidence, standard, records) are checked by the line and region gates below, because no tool validates wording.

Output: monitoring.c4, a chart-values record, a re-recorded drill-output.txt, scenarios-app.c4, the evidence file, one pointer line in the measure record, the standard, and an addendum in 02-VERIFICATION.md, in three commits.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/workstreams/architecture-views/phases/02-model-completion/02-VERIFICATION.md
@.planning/workstreams/architecture-views/quick/261028-n5v-close-phase-02-verification-gaps-error-4/261028-n5v-SUMMARY.md

Conventions binding every task:

- Paths are repo-root-relative and every command assumes cwd at the checkout root. Redirect stdin (`< /dev/null`) on the check, the self-test, the drill and the exports (docs/SESSION_LESSONS.md lesson 3). Run in the main checkout on main, not in a worktree.
- `$P` stands for `.planning/workstreams/architecture-views/phases/02-model-completion` and `$R` for `.planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt`. Set each in the same Bash call that uses it, because shell state does not persist between calls. This machine's Bash hook refuses an inline loop or a command over 300 characters unless it ends in `# long-ok: plan gate`.
- `$BASE` is what `git rev-parse --short HEAD` prints before Task 1; at planning it was 2fbe8ff. Record it in the SUMMARY. No automated command depends on it: the gates use the fixed revisions 2fbe8ff and 518eb75.
- The required-reading path `k8s/monitoring/kube-prometheus-stack.yaml` does not exist; the file is `k8s/monitoring/controllers/kube-prometheus-stack.yaml`.
- .c4 strings are single-quoted. Write no apostrophe inside one, and no wildcard Accept literal (a parse error fails G1). Markdown may use both. Keep each positive gate literal (`observed at 7bccf71`, `2fbe8ff`, `Five monitoring elements`, `capture window`) unbroken; the region gates squeeze line breaks, the row gates do not.
- A provenance line in an evidence section never starts with `| <digit> |`, so steps still equal rows (G5).
- Commit each task with `git diff --cached --name-only` empty first. The untracked files under `.planning/quick/`, `milestone.lock` and this folder's PLAN.md are left alone. The commit form is `git commit --no-verify -m "<subject>" -m "<why>" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>" -- FILE1 FILE2 ...`, with the task's paths listed one by one. Do not push or amend. If a task ever needs a path under src/, scripts/ or k8s/, stop and report.
- Records not rewritten: 02-0N-SUMMARY.md, 02-REVIEW.md, 02-REVIEW-DISPOSITION.md, STATE.md, ROADMAP.md and the m6k and n5v files. 02-VERIFICATION.md is append-only here: the scope asks for the open question to be answered there, and its frontmatter belongs to the verifier.
- Out of scope, untouched: WR-01, WR-02, WR-07, WR-09, the info findings, the stale header of k8s/platform/edge/clusterissuers.yaml, any Java, the Mermaid diagrams and docs/INFRA_ARCHITECTURE.md, the docs/wiki copies, and the Alloy chart defaults (stated as not read, not fetched).

Chart values at tag kube-prometheus-stack-91.5.2, read in full at planning (Task 1 re-reads and confirms each line). The repo-side lines are in k8s/monitoring/controllers/kube-prometheus-stack.yaml.

| Monitor | values.yaml lines (planning) | Repo values | This plan |
|---|---|---|---|
| kubernetesServiceMonitors (master switch) | 1730-1731 `enabled: true` | not set | holds the component monitors on |
| kubeApiServer | 1735-1736 `enabled: true`; 1741-1742 tlsConfig `serverName: kubernetes`; 1748-1749 serviceMonitor `enabled: true` | not set | edge `scrape API server metrics` (no protocol: no values line in 1735 to 1749 states the scheme; the only https there is a URL in the comment at 1739, re-checked in this revision) |
| kubelet | 1838-1839 `enabled: true`; 1846-1847 serviceMonitor `enabled: true`; 1849 `kubelet: true`; 1900 `https: true`; 1929 `probes: true`; 1934 `resource: false`; 1944 `cAdvisor: true` | not set | edge `scrape kubelet, cAdvisor and probe metrics over HTTPS` |
| coreDns | 2229-2230 `enabled: true`; 2233 service `port: 9153`; 2242-2243 serviceMonitor `enabled: true` | not set | edge `scrape DNS metrics on port 9153` |
| kubeStateMetrics | 2835-2836 `enabled: true`; 2849-2853 kube-state-metrics prometheus monitor `enabled: true` | resources only, 169-177 | edge `scrape cluster object state metrics` |
| prometheusOperator | 2981-2982 `enabled: true`; 2997-2998 tls `enabled: true`; 3525-3528 serviceMonitor `selfMonitor: true` | resources only, 179-187 | edge `scrape operator metrics` |
| grafana | 1696-1700 serviceMonitor `enabled: true` | the grafana block 86-137 sets no serviceMonitor | edge `scrape Grafana server metrics` |
| nodeExporter | 2867-2868 `enabled: true` | monitor on, 160-162 | already drawn |
| prometheus self | 4341-4344 serviceMonitor `selfMonitor: true` | not set | stated, not drawn (LikeC4 rejects a self-relationship) |
| alertmanager | 398, 402 `enabled: true` | `enabled: false`, 37-38 | not drawn |
| kubeControllerManager, kubeScheduler, kubeEtcd, kubeProxy | 2102, 2563, 2431, 2718 | `enabled: false`, 45-52 | not drawn |
| kubeDns, thanosRuler, windowsMonitoring | 2322-2323, 5515 and 5519, 367 and 369, each `enabled: false` | not set | not drawn |

Also read at planning: kube-prometheus-stack.yaml:25 `version: "91.5.2"`, :83-84 both selector-nil flags `false` (Prometheus selects every ServiceMonitor and PodMonitor), and :1 and :13 (the release brings the operator and kube-state-metrics). infra/vm/k3s/config.yaml `disable:` names only `metrics-server`, so the k3s-bundled CoreDNS stays on.

Shared gates. Each was run at HEAD 2fbe8ff during planning, and the baseline is shown. Gates marked CHANGED differ from the 261028-n5v gates of the same name, and the reason is given.

- G1 check: `node docs/architecture/sync.mjs check < /dev/null`. Baseline `OK (56 elements, 6 tables, 4 foreign keys, 14 packages, 38 package edges, 2 topics, 17 workloads)`. After Task 1: the same with `61 elements`, still `17 workloads`. CHANGED: n5v expected 56; this plan adds five elements.
- G2 export: `docs/architecture/node_modules/.bin/likec4 export json --skip-layout -o docs/architecture/out/export.json docs/architecture/model < /dev/null`. Exit 0, a line containing `generated`.
- G3 physical: `node -e "const v=Object.values(require('./docs/architecture/out/export.json').views).filter(x=>x._type==='deployment');console.log(v.filter(x=>(x.tags||[]).includes('physical')).map(x=>x.id).sort().join(','),v.flatMap(x=>Object.values(x.edges)).filter(e=>e.label==='[...]').length)"`. Prints exactly `delivery,monitoring,production 0` (baseline and after).
- G3b view sizes (new): `node -e "for(const v of Object.values(require('./docs/architecture/out/export.json').views))if(v._type==='deployment')console.log(v.id,Object.keys(v.nodes).length,Object.keys(v.edges).length)"`. Baseline `monitoring 19 11`, `production 24 17`, `delivery 14 8`. After Task 1: `monitoring 25 17`, `production 24 17`, `delivery 14 8` (measured at planning with the same five elements). Line order is not significant.
- G3c scrape list (new): `node -e "const v=require('./docs/architecture/out/export.json').views.monitoring;console.log(Object.values(v.edges).filter(e=>e.source==='netcup.k3s.monitoring.prometheus').map(e=>e.target+' : '+e.label).sort().join('\n'))"`. Baseline 7 lines, each `: scrape metrics`, targets cert-manager.certmanager, monitoring.node-exporter, monitoring.postgres-exporter, nonprod.kanban, nonprod.redpanda, prod.kanban, prod.redpanda. After Task 1: those 7 unchanged plus `netcup.k3s.kube-apiserver`, `netcup.k3s.kube-system.coredns`, `netcup.k3s.kubelet`, `netcup.k3s.monitoring.grafana`, `netcup.k3s.monitoring.kube-state-metrics` and `netcup.k3s.monitoring.prometheus-operator`, 13 lines, each with the title from the table above.
- G4 scenarios: the n5v command, unchanged. Baseline and after, 13 lines: signup-ok scenarios 5 9 0; error-401 scenarios 3 4 0; error-403 scenarios 5 6 0; error-409 scenarios 4 5 0; activity-log-row scenarios 5 5 0; push-to-image scenarios 4 5 0; image-to-rollout scenarios 5 6 0; inbound-packet-path scenarios 5 5 0; ingress-to-pod scenarios 3 5 0; nightly-backup scenarios 3 5 0; signin-ok scenarios 5 9 0; signin-bad-credentials scenarios 4 6 0; signin-ceiling scenarios 5 9 0.
- G5 steps-to-rows: the n5v command, unchanged. Baseline and after: ten equal pairs (error-401 `4 4`), and the three signin views print `none`.
- G6 titles: `rg -n -e ' -> ' -e ' <- ' docs/architecture/model/scenarios-app.c4 docs/architecture/model/scenarios-platform.c4 | rg -v "'\s*$"`. Prints nothing.
- G7 drill replay: `fish docs/architecture/drill.fish < /dev/null 2>/dev/null | cmp - docs/architecture/drill-output.txt`. Exit 0 at baseline; exit 0 again after the Task 1 re-record. CHANGED: n5v forbade a re-record; this plan requires one, because the tally lines print the element count.
- G7r re-record proof (new): `old=$(git show 518eb75:docs/architecture/drill-output.txt) || exit 1; diff <(printf '%s\n' "$old" | sed -E 's/\(([0-9]+) elements/(N elements/') <(sed -E 's/\(([0-9]+) elements/(N elements/' docs/architecture/drill-output.txt)`. Exit 0 with no output, at baseline and after. git's status is captured before the pipe, so a failing `git show` fails the gate. Plus `rg -o '\([0-9]+ elements' docs/architecture/drill-output.txt | sort | uniq -c`: baseline `29 (56 elements` and `4 (57 elements`; after Task 1, `29 (61 elements` and `4 (62 elements`.
- G8 self-test: `python3 scripts/pre-commit-likec4-selftest.py < /dev/null`. Last line `16/16 passed`. Its tally regex is `\d+ elements`, so the count change does not affect it.
- L1 model leak: `rg -n -e 'svc\.cluster\.local' -e 'postgres\.kanban-' -e 'age1[0-9a-z]{20,}' -e 'backblazeb2' -e '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' -e '\.ssh/' docs/architecture/model; test $? -eq 1`. Prints nothing, exit 0.
- L2 evidence, standard, record and report leak: `P=.planning/workstreams/architecture-views/phases/02-model-completion; R=.planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt; rg -n -e 'age1[0-9a-z]{20,}' -e 'backblazeb2' -e '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' $P/02-SCENARIO-EVIDENCE.md docs/DIAGRAM_CONVENTIONS.md $P/02-VERIFICATION.md $R; test $? -eq 1`. CHANGED: adds the record and the report. Baseline: the old three-file form prints nothing (exit 0); the new form exits 1 until Task 1 creates the record (rg exits 2 on a missing file).
- L3 cookie and credential: `P=.planning/workstreams/architecture-views/phases/02-model-completion; rg -c -e 'JSESSIONID=[A-Za-z0-9-]{8,}' -e 'SESSION=[A-Za-z0-9-]{8,}' -e 'DB_PASS=' $P/02-04-401-measure.txt $P/02-SCENARIO-EVIDENCE.md $P/02-VERIFICATION.md docs/architecture/model; test $? -eq 1`. Prints nothing, exit 0 (baseline the same; adding the report was checked clean).
- L4 measure-record IP count: `P=.planning/workstreams/architecture-views/phases/02-model-completion; n=$(rg -c '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' $P/02-04-401-measure.txt); echo "$n"; test "$n" = 3`. Prints `3`. CHANGED: n5v expected 2; 2fbe8ff added a third loopback line (now 16, 52, 168). The n5v form fails at HEAD.
- L5 gitleaks over the changed paths (new; replaces the hook step that `--no-verify` skips). Each of the following exits 0 and prints `no leaks found`. Baseline exit 0 for every path that exists at HEAD.
  - `gitleaks dir docs/architecture/model --no-banner --redact -c .gitleaks.toml`
  - `gitleaks dir docs/DIAGRAM_CONVENTIONS.md --no-banner --redact -c .gitleaks.toml`
  - `gitleaks dir docs/architecture/drill-output.txt --no-banner --redact -c .gitleaks.toml`
  - `gitleaks dir .planning/workstreams/architecture-views/phases/02-model-completion --no-banner --redact -c .gitleaks.toml`
  - `gitleaks dir .planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet --no-banner --redact -c .gitleaks.toml`
- M1 measure record (CHANGED: the base is now 2fbe8ff, the last commit to touch the file, not 178aff3, and exactly one added line is required): `P=.planning/workstreams/architecture-views/phases/02-model-completion; out=$(git diff --numstat 2fbe8ff -- $P/02-04-401-measure.txt) || exit 1; printf '%s\n' "$out" | awk '{a+=$1; d+=$2} END {print a+0, d+0; exit !(a==1 && d==0)}'`. Baseline `0 0`, exit 1. After Task 2: `1 0`, exit 0.
- V1 report append-only (new): `P=.planning/workstreams/architecture-views/phases/02-model-completion; out=$(git diff --numstat 2fbe8ff -- $P/02-VERIFICATION.md) || exit 1; printf '%s\n' "$out" | awk '{a+=$1; d+=$2} END {print a+0, d+0; exit !(a>0 && d==0)}'`. Baseline `0 0`, exit 1. After Task 3: a positive count and `0`, exit 0.
- V2 report frontmatter unchanged (new): `P=.planning/workstreams/architecture-views/phases/02-model-completion; old=$(git show 2fbe8ff:$P/02-VERIFICATION.md) || exit 1; diff <(printf '%s\n' "$old" | awk 'NR==1{next} /^---$/{exit} 1') <(awk 'NR==1{next} /^---$/{exit} 1' $P/02-VERIFICATION.md)`. Exit 0 with no output, at baseline and after.
- V3 addendum names the capture limit (new in revision): `P=.planning/workstreams/architecture-views/phases/02-model-completion; awk '/^## Addendum, 2026-10-28 \(quick task 261028-opl\)/,0' $P/02-VERIFICATION.md | grep -c 'capture window'`. Baseline 0 (no addendum yet; the literal is also absent file-wide). After Task 3: at least 1.

Wording gates. Each has its baseline at HEAD; "after" is the required state. `S` = docs/DIAGRAM_CONVENTIONS.md. CK is the Checks paragraph squeezed to one line: `awk '/^  Not placement-checked/,/^- \*\*Drills/' docs/DIAGRAM_CONVENTIONS.md | tr '\n' ' ' | tr -s ' '`. B2 is note (b2) squeezed: `awk '/^\*\*\(b2\)/,/^$/' docs/DIAGRAM_CONVENTIONS.md | tr '\n' ' '`. EV is the evidence section squeezed: `awk '/^## view error-401$/,/^## view error-403$/' FILE | tr '\n' ' '`. C4 is the view region: `awk '/dynamic view error-401/,/^  }$/' docs/architecture/model/scenarios-app.c4`.

- N1 Physical row: `grep -E '^\| Physical \| .deployment. model' docs/DIAGRAM_CONVENTIONS.md | grep -c 'among them the kubelet'`, baseline 1, after 0. The same row piped to `grep -c CoreDNS`, baseline 0, after 1.
- N2 to N4, CK piped to `grep -c` for each literal. `chart documented defaults`: baseline 1, after 0. `line 1735`: baseline 1, after 0. `Three monitoring elements`: baseline 1, after 0. `Five monitoring elements`, `CoreDNS`, `261028-opl-chart-values.txt`, `itself` and `Alloy`: each baseline 0, after 1.
- N5 monitoring row: `grep -E '^\| .monitoring. \|' docs/DIAGRAM_CONVENTIONS.md | grep -c 'chart default scrape targets are not drawn'`, baseline 1, after 0. The same row piped to `grep -c '91.5.2'`, baseline 0, after 1.
- N6 model: `rg -c -e 'chart documented defaults' -e 'only the API server was confirmed' docs/architecture/model/monitoring.c4`, baseline 1, after nothing (exit 1). `rg -c -e '261028-opl-chart-values.txt' docs/architecture/model/monitoring.c4`, baseline nothing, after at least 1. `rg -c 'itself' docs/architecture/model/monitoring.c4`, baseline nothing, after at least 1.
- I1, C4 piped to `grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e '2fbe8ff' -e '82775fa' -e 'proxy' -e 'stale' -e 'capture window' | sort | uniq -c`. Baseline: one line, `1 inferred from the code` (`capture window` measured absent). After: `2fbe8ff`, `82775fa`, `observed at 7bccf71`, `proxy`, `stale` and `capture window` each at least once, and no `inferred from the code` line.
- I2, EV on $P/02-SCENARIO-EVIDENCE.md piped to `grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e '2fbe8ff' -e '82775fa' -e 'verdict b' -e 'proxy' -e 'stale' -e 'Redpanda' -e 'capture window' | sort | uniq -c`. Baseline: `1 82775fa`, `2 inferred from the code`, `1 proxy`, `1 verdict b` (re-measured in revision: the two hits are the inference bullet, evidence line 31, and the closing paragraph, line 40; `capture window` measured absent). After: all of `2fbe8ff`, `82775fa`, `observed at 7bccf71`, `verdict b`, `proxy`, `stale`, `Redpanda` and `capture window` present, and no `inferred from the code` line. Also `rg -c -F 'Measured before the fix: 02-04-401-measure.txt in this folder, 2026-10-28T14:44Z, host commit 82775fa, verdict b' $P/02-SCENARIO-EVIDENCE.md` prints 1, at baseline and after (the pre-fix bullet is kept verbatim).
- I3, B2 piped to `grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e '2fbe8ff' -e 'verdict b' -e '17a94cb' -e 'proxy' -e 'stale' -e 'capture window' | sort | uniq -c`. Baseline `1 17a94cb`, `1 inferred from the code`, `1 verdict b` (`capture window` measured absent). After: `2fbe8ff`, `observed at 7bccf71`, `verdict b`, `17a94cb`, `proxy`, `stale` and `capture window` present, and no `inferred from the code` line. The n5v gate stays: B2 piped to `grep -c -e 'not measured' -e 'did not run'` prints 0.
- I4 Scenarios row: `grep -E '^\| Scenarios \| .dynamic view' docs/DIAGRAM_CONVENTIONS.md | grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e 'capture window' | sort | uniq -c`. Baseline `1 inferred from the code` (`capture window` measured absent). After: `1 observed at 7bccf71`, `capture window` at least once, and no `inferred from the code` line.
- I5 `error-401` row: `grep -E '^\| .error-401. \|' docs/DIAGRAM_CONVENTIONS.md | grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e 'capture window' | sort | uniq -c`. Baseline and after the same as I4.
- I6 standard file-wide: `rg -c 'inferred from the code' docs/DIAGRAM_CONVENTIONS.md`, baseline 3, after nothing (exit 1).
- I7 measure record, history kept: `P=...; rg -c 'inferred from the code' $P/02-04-401-measure.txt` prints 1 at baseline and after. `P=...; rg -c 'observed at 7bccf71' $P/02-04-401-measure.txt`, baseline nothing, after 1. `P=...; grep -c -e '^VERDICT:' -e '^POST-FIX RESULT:' $P/02-04-401-measure.txt` prints 2 at baseline and after.

Planning probes (scratch copies under the gitignored docs/architecture/out, trashed afterwards; nothing tracked was touched):
- A relationship `prometheus -[sync]-> prometheus` made the check print `model does not validate: Invalid parent-child relationship` and exit 1.
- The five elements, instances and include lines of Task 1 gave `OK (61 elements, ..., 17 workloads)`, the G3b and G3c results above, and a drill replay identical to drill-output.txt once element counts were normalized.
- A second kube-state-metrics instance in kube-system gave the placement drift quoted in the trade-off matrix, exit 1.
</context>

<tasks>

<task type="auto">
  <name>Task 1: End-to-end: the monitoring view draws the chart-default scrape targets, tied to the chart values, with the drill record and the standard matching</name>
  <precondition>`git status --short -- docs/architecture docs/DIAGRAM_CONVENTIONS.md .planning/workstreams/architecture-views/phases/02-model-completion` prints nothing; `git merge-base --is-ancestor 2fbe8ff HEAD` exits 0; `gh auth status` exits 0.</precondition>
  <files>docs/architecture/model/monitoring.c4, .planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt, docs/architecture/drill-output.txt, docs/DIAGRAM_CONVENTIONS.md</files>
  <read_first>
    - docs/architecture/model/monitoring.c4 (whole file, 90 lines), docs/architecture/model/edge.c4 lines 41-43 and 67-83 (the svclb and ClusterIssuer placement precedents), docs/architecture/model/deployment.c4
    - k8s/monitoring/controllers/kube-prometheus-stack.yaml (whole file, 188 lines), k8s/monitoring/controllers/loki.yaml lines 95-105, infra/vm/k3s/config.yaml
    - docs/architecture/sync.mjs lines 392-489 (checkPlacement, checkViewMembership, checkEdgeLabels, checkDeployedWorkloads)
    - docs/DIAGRAM_CONVENTIONS.md lines 107-113 and 236-256 and 339-352
  </read_first>
  <action>
    Record `$BASE` first. Per the trade-off matrices: draw every target in the context table marked "edge" (approach B of "which targets"), model them as five `system` elements (approach A of "how to model"), and add no rule (approach A of "drift rule").

    Step 1, read the chart values at the tag; every edge claim rests on this. `mkdir -p docs/architecture/out`, then make a new empty directory with `mktemp -d -p docs/architecture/out` and call it VALUES_DIR. Run `gh api -H 'Accept: application/vnd.github.raw' 'repos/prometheus-community/helm-charts/contents/charts/kube-prometheus-stack/values.yaml?ref=kube-prometheus-stack-91.5.2'` with its output redirected to VALUES_DIR/values.yaml, then run `wc -c` and `sha256sum` on it. Planning got 218683 bytes and sha256 7d089fcf435e6904cda5ad246602e1c61005d08c68e3a9ab82770c249e395081. If either differs, record what was measured, compare every line in the context table against it, and say so in the SUMMARY; do not edit the numbers to match. If gh cannot fetch it, fall back to the web-access MCP `fetch` in pieces until all 218,683 characters are read. If neither works, draw no edge whose line was not read, and say in the record, the view and the standard which target is not drawn and why. Read the file only with `sed -n` and `rg -n`, never execute anything from that directory, and confirm every line of the context table with `sed -n` on its range. Also re-read the repo side: `sed -n '1p;13p;25p;37,52p;83,84p;86,137p;160,187p' k8s/monitoring/controllers/kube-prometheus-stack.yaml`, `sed -n '95,105p' k8s/monitoring/controllers/loki.yaml` and `sed -n '/^disable:/,$p' infra/vm/k3s/config.yaml`. If any line contradicts the context table (a monitor turned off, a port that differs), follow the source, not this plan, and record the difference.

    Step 2, the self-relationship claim, measured again because the text will state it. Copy docs/architecture/model to docs/architecture/out/probe-opl/model. Write a file selfloop.c4 there that holds one model block with the relationship `prometheus -[sync]-> prometheus 'probe'`, run `node docs/architecture/sync.mjs check --model docs/architecture/out/probe-opl/model < /dev/null`, and expect exit 1 with `Invalid parent-child relationship`. Then trash docs/architecture/out/probe-opl.

    Step 3, write `$R` (the record named in files_modified) with the Write tool, in plain text. It holds: the time checked (UTC) and `$BASE`; the gh command, why it was used instead of a web fetch (the previous pass's fetch stopped at 62,574 of 218,683 characters), and the byte count and sha256 measured. Then a table with one row per monitor in the context table: the key, each values line number with its text quoted verbatim (only `enabled`, `port`, `https`, `cAdvisor`, `probes`, `resource`, `selfMonitor` and `serverName` lines; never the authorization or credentials lines), the repo setting with its kube-prometheus-stack.yaml line, and the outcome (the drawn edge title, or not drawn and why). The kubeApiServer outcome cell says why its title names no protocol: the tlsConfig block (1741 to 1747) sets TLS client fields only, no values line in the kubeApiServer block (1735 to 1749) states the scheme, and the templates that would set it are not read; the kubelet title says HTTPS because line 1900 states `https: true`. After the table: the selector-nil lines 83 to 84; the k3s disable list; Loki's ServiceMonitor setting with its loki.yaml lines; the Step 2 command and its output line. Close with a "Not read" list: the live target list (no cluster access); the chart templates (no helm binary, so ports and paths appear only where a values line states them); the Alloy chart defaults. Finish with the cleanup line, naming VALUES_DIR as trashed.

    Step 4, docs/architecture/model/monitoring.c4. Write no apostrophe in any string.
    - Five new elements after postgres-exporter, each a `system`, with these ids and titles: `kubelet` 'kubelet (with cAdvisor)', `kube-apiserver` 'Kubernetes API server', `coredns` 'CoreDNS', `kube-state-metrics` 'kube-state-metrics', `prometheus-operator` 'Prometheus Operator'.
    - kube-state-metrics and prometheus-operator take the metadata node-exporter uses: `source 'k8s/monitoring/controllers/kube-prometheus-stack.yaml'` and `workload 'HelmRelease/kube-prometheus-stack'`. Their descriptions, in substance: kube-state-metrics exposes the state of Kubernetes objects as metrics, a subchart of the kube-prometheus-stack release; the operator reconciles the Prometheus, ServiceMonitor and PodMonitor objects for that release.
    - kubelet, kube-apiserver and coredns take `source 'infra/vm/k3s/config.yaml'` and no workload. Each description starts `Authored.`, as svclb does. The kubelet and the API server run inside the k3s server process that infra/vm/k3s/install.sh installs (the kubelet has cAdvisor built in), and no manifest in the repo declares them. CoreDNS is the cluster DNS that k3s deploys in kube-system; config.yaml leaves it on (its disable list names only metrics-server), and no manifest in the repo declares it.
    - Six relationships after the existing prometheus edges, each `prometheus -[sync]-> TARGET` with the title from the context table: kubelet, kube-apiserver, coredns, kube-state-metrics, prometheus-operator, grafana.
    - In the deployment block: kube-state-metrics and prometheus-operator join `extend netcup.k3s.monitoring`. Add `extend netcup.k3s` with `instanceOf kubelet` and `instanceOf kube-apiserver`, and `extend netcup.k3s.kube-system` with `instanceOf coredns`. Do not edit deployment.c4: drill sed anchors live there.
    - The monitoring view include gains one more line after the redpanda line, `netcup.k3s.kubelet, netcup.k3s.kube-apiserver, netcup.k3s.kube-system.coredns`; add a comma to the end of the redpanda line. Leave the exclude block and the title unchanged.
    - The prometheus element description, in substance: metrics store and scraper; its scrape edges are drawn from the PodMonitor and ServiceMonitor settings in the repo and from the default monitors of the kube-prometheus-stack chart that the values file leaves on, not from live targets. It also scrapes itself, which is not drawn.
    - The monitoring view description, replacing the whole string. In substance: the scrape and query edges come from the PodMonitor, ServiceMonitor and datasource settings in the repo, and from the default monitors of kube-prometheus-stack at the pinned version 91.5.2 that the values file leaves on: the kubelet with cAdvisor, the API server, CoreDNS, kube-state-metrics, the operator and Grafana, each read from the chart values in 261028-opl-chart-values.txt. Not drawn: the controller-manager, scheduler, etcd, kube-proxy and Alertmanager monitors, which the values file turns off, and the scrape Prometheus makes of itself, because LikeC4 rejects a relationship from an element to itself. The check proves each HelmRelease exists and lands in monitoring, not what its chart renders; the kubelet, the API server and CoreDNS are authored, and the live target list is not read. This text must not reuse either N6 literal.
    Then run G1, G2, G3, G3b, G3c, G6 and L1. G1 must print 61 elements and 17 workloads. If a rule fires, satisfy it in the model honestly (an instance, an include, a title); never edit sync.mjs.

    Step 5, re-record the drill: `fish docs/architecture/drill.fish < /dev/null 2>/dev/null > docs/architecture/drill-output.txt`, then G7r, which must show exit 0 and the `29 (61 elements` and `4 (62 elements` tallies, then G7 (byte-identical). Any diff that remains after normalization is a regression: investigate it, and do not commit over it.

    Step 6, docs/DIAGRAM_CONVENTIONS.md, the monitoring text only (Task 2 owns the 401 text).
    - Physical row (line 112). In the Represents cell, the `monitoring` clause becomes, in substance: the monitoring workloads; the scrape targets the repo monitor settings name; the default scrape targets the kube-prometheus-stack chart at 91.5.2 leaves on (the kubelet with cAdvisor, the API server, CoreDNS, kube-state-metrics, the operator and Grafana); the query edges; the self-scrape of Prometheus stated, not drawn. This replaces the N1 sentence and must not reuse the N1 literal. In the Does-not-represent cell, qualify "objects a chart renders": the chart workloads drawn under a HelmRelease claim are drawn, but the check does not render the chart. In the Provenance cell, add the kubelet, the API server and CoreDNS to the authored list.
    - Checks paragraph (lines 241-255). The "Not placement-checked" sentence adds `coredns`, `kubelet` and `kube-apiserver` (no manifest declares them) beside `svclb`. The sentence counting the elements that share one `HelmRelease/kube-prometheus-stack` claim now counts five and names them (Prometheus, Grafana, node-exporter, kube-state-metrics, the operator); write `Five monitoring elements` unbroken. Replace the scrape-edge sentences with what Step 4 wrote, in the standard voice, including the record path `$R` in full. The values file turns off the controller-manager, scheduler, etcd and kube-proxy monitors (lines 45 to 52) and Alertmanager (lines 37 to 38). Prometheus also scrapes itself, which is not drawn because LikeC4 rejects a relationship from an element to itself. Of the other charts, the Loki ServiceMonitor is off (cite the loki.yaml lines that Step 1 printed) and the Alloy chart defaults are not read; the live target list is not read either. The old API server line cite and the N2 literal go away, along with the sentence that held them.
    - The `monitoring` row of the Phase 2 table (line 352), Provenance cell: HelmReleases checked as workloads; scrape and query edges authored from the repo monitor settings and the chart values at 91.5.2; the Prometheus self-scrape stated, not drawn. Leave the size cell for Task 3.

    Run N1 to N6, L2, L5 (model, standard, drill-output, the quick folder), then commit monitoring.c4, `$R`, drill-output.txt and DIAGRAM_CONVENTIONS.md. Subject along the lines of `docs(architecture): draw the kube-prometheus-stack default scrape targets in the monitoring view`; the body names the six edges, the self-scrape exception and the re-recorded drill. Trash VALUES_DIR and docs/architecture/out/export.json.
  </action>
  <verify>
    <automated>node docs/architecture/sync.mjs check < /dev/null</automated>
    <fails_when>non-zero exit, or the tally is not `61 elements` with `17 workloads`</fails_when>
    <automated>docs/architecture/node_modules/.bin/likec4 export json --skip-layout -o docs/architecture/out/export.json docs/architecture/model < /dev/null && node -e "const v=require('./docs/architecture/out/export.json').views.monitoring;console.log(Object.values(v.edges).filter(e=>e.source==='netcup.k3s.monitoring.prometheus').map(e=>e.target+' : '+e.label).sort().join('\n'))"</automated>
    <fails_when>non-zero exit, fewer or more than 13 lines, any of the six new targets in G3c missing, the kubelet line without `cAdvisor`, the coredns line without `9153`, the kube-apiserver line naming a protocol (no values line states its scheme), or one of the 7 earlier lines changed</fails_when>
    <automated>node -e "for(const v of Object.values(require('./docs/architecture/out/export.json').views))if(v._type==='deployment')console.log(v.id,Object.keys(v.nodes).length,Object.keys(v.edges).length)"</automated>
    <fails_when>the lines are not exactly `monitoring 25 17`, `production 24 17` and `delivery 14 8` (any order)</fails_when>
    <automated>old=$(git show 518eb75:docs/architecture/drill-output.txt) || exit 1; diff <(printf '%s\n' "$old" | sed -E 's/\(([0-9]+) elements/(N elements/') <(sed -E 's/\(([0-9]+) elements/(N elements/' docs/architecture/drill-output.txt) && rg -o '\([0-9]+ elements' docs/architecture/drill-output.txt | sort | uniq -c</automated>
    <fails_when>diff prints anything or exits non-zero, or the counts are not `29 (61 elements` and `4 (62 elements`</fails_when>
    <automated>R=.planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt; rg -o -e 'kube-prometheus-stack-91.5.2' -e 'Invalid parent-child relationship' -e 'kubeApiServer' -e 'coreDns' -e 'kubeStateMetrics' -e 'prometheusOperator' -e 'selfMonitor' -e 'kubeControllerManager' -e 'alertmanager' $R | sort -u | wc -l</automated>
    <fails_when>prints anything but 9, or rg errors (the record is missing, or lacks the tag, the probe result or a monitor)</fails_when>
    <automated>awk '/^  Not placement-checked/,/^- \*\*Drills/' docs/DIAGRAM_CONVENTIONS.md | tr '\n' ' ' | tr -s ' ' | grep -o -e 'chart documented defaults' -e 'line 1735' -e 'Three monitoring elements' -e 'Five monitoring elements' -e 'CoreDNS' -e 'itself' -e 'Alloy' -e '261028-opl-chart-values.txt' | sort | uniq -c</automated>
    <fails_when>any of the first three literals is printed, or any of `Five monitoring elements`, `CoreDNS`, `itself`, `Alloy`, `261028-opl-chart-values.txt` is missing</fails_when>
  </verify>
  <acceptance_criteria>
    - N1: the Physical row piped to `grep -c 'among them the kubelet'` prints 0 (baseline 1), and piped to `grep -c CoreDNS` prints 1 (baseline 0).
    - N5: the `monitoring` row piped to `grep -c 'chart default scrape targets are not drawn'` prints 0 (baseline 1).
    - N6: `rg -c -e 'chart documented defaults' -e 'only the API server was confirmed' docs/architecture/model/monitoring.c4` prints nothing (baseline 1); `rg -c '261028-opl-chart-values.txt' docs/architecture/model/monitoring.c4` and `rg -c 'itself' docs/architecture/model/monitoring.c4` each print at least 1 (baseline nothing).
    - `R=...; rg -c '7d089fcf435e6904cda5ad246602e1c61005d08c68e3a9ab82770c249e395081' $R` prints 1, or the SUMMARY states the different digest measured and the gate is read against it; `R=...; rg -c -i -e 'credentials' -e 'Bearer' $R` prints nothing.
    - `rg -c "workload 'HelmRelease/kube-prometheus-stack'" docs/architecture/model/monitoring.c4` prints 5 (baseline 3).
    - G3 prints `delivery,monitoring,production 0`; G6 prints nothing; L1 and L2 print nothing and exit 0; each L5 command exits 0.
    - `git diff --stat 2fbe8ff -- docs/architecture/sync.mjs docs/architecture/drill.fish docs/architecture/model/views.c4 docs/architecture/model/deployment.c4 docs/architecture/model/spec.c4 k8s src scripts` prints nothing.
    - `ls docs/architecture/out` lists no VALUES_DIR and no probe-opl; after the commit, `git status --short` on the four files prints nothing.
  </acceptance_criteria>
  <done>The monitoring view draws an edge from Prometheus to each chart-default target that the values at 91.5.2 leave on and that LikeC4 can draw. The record ties each edge to its values line, the self-scrape is stated with its measured cause, every existing rule still passes, the drill record differs only in its tally counts, and the standard describes what is drawn.</done>
</task>

<task type="auto">
  <name>Task 2: The 401 path stated as observed at 7bccf71 after the fix, in the view, the evidence, the record and the standard</name>
  <precondition>Task 1 is committed (`git status --short -- docs/architecture/model docs/DIAGRAM_CONVENTIONS.md` prints nothing); `git diff --quiet 17a94cb HEAD -- src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java` exits 0, so the n5v SecurityConfiguration cites still hold.</precondition>
  <files>docs/architecture/model/scenarios-app.c4, .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md, .planning/workstreams/architecture-views/phases/02-model-completion/02-04-401-measure.txt, docs/DIAGRAM_CONVENTIONS.md</files>
  <read_first>
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-04-401-measure.txt lines 134-248 (the n5v follow-up and the whole Post-fix re-measure section)
    - docs/architecture/model/scenarios-app.c4 lines 19-29
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md lines 23-40
    - docs/DIAGRAM_CONVENTIONS.md line 113, lines 147-156 and the `error-401` row of the Phase 2 table
  </read_first>
  <action>
    Per approach A of the 401 trade-off matrix. One rule binds every sentence this task writes: claim only what the post-fix section of the measure record observed, cite it, and name what it did not try. Every reader-facing site keeps the pre-fix facts (82775fa, verdict b, the JSON-versus-wildcard split, 17a94cb and the in-process test). The inference label that the I gates grep for is removed from the five reader-facing sites; it survives only in the dated n5v note of the measure record, which the pointer below supersedes.

    Claim checks, run first and quoted from their output. `P=...; rg -n -e '^=== ' -e '^\| [A-I] \|' -e '^POST-FIX RESULT' -e '^Unverified / limits' -e '^Burst' -e '^Signin check' $P/02-04-401-measure.txt` (planning: post-fix heading 160, rows A to I 190-198, Burst 206, Signin check 211, POST-FIX RESULT 222, limits 229). Then read the limits list in full: /api/boards GET and POST only, with PUT, PATCH, DELETE and the other routes not tried; one machine, one run, no proxy, a local port other than 8080; no Redpanda; plain HTTP, with the Secure attribute not exercised; a one-second capture window per request plus one log-tail read after rows A to I; no stale or foreign session cookie. Also read the Method lines on the capture (`sed -n '177,183p'`, planning numbering): the whole log was not re-read at the end, so a statement outside every request window is not ruled out. That limit is the statement-capture limit, and every sentence below that says no statement was logged names it with the literal `capture window` (singular or plural). Confirm `git log --oneline -1 2fbe8ff` names the re-measure, and `git merge-base --is-ancestor 17a94cb 7bccf71` exits 0.

    Edit 02-04-401-measure.txt first (it shifts the post-fix lines by one). With the Edit tool, insert exactly one line directly below the heading `=== Follow-up (2026-10-28, quick task 261028-n5v): fixed in 17a94cb ===`, before its blank line. In substance: superseded in part on 2026-10-28 (quick task 261028-opl): the post-fix absence of SQL that this note calls inferred was observed at 7bccf71 in the Post-fix re-measure section below (commit 2fbe8ff); the rest of this note stands as written. The line must not start with `VERDICT:` or `POST-FIX RESULT:`, must not repeat the I gates' label phrase, and must change no other line. Run M1 (`1 0`), L4 (`3`) and I7, then re-run the `rg -n` claim check above and use those line numbers (planning plus one) in every cite below.

    docs/architecture/model/scenarios-app.c4, the error-401 description only; the title, the participants and the four steps stay as they are. Keep the first two sentences (the entry point, and that no SQL step is drawn). The rest, in substance:
    - Measured before the fix at 82775fa (02-04-401-measure.txt, GET and POST /api/boards): a client asking for JSON got this 401 with no SQL, while a client whose Accept header did not name application/json (a bare wildcard, or a browser navigation header) had its refused request saved in a new session, one spring_session row and a JSESSIONID cookie per request.
    - Since commit 17a94cb the main security chain saves no refused request, and AuthorizationGatingTest pins it in process.
    - Observed at 7bccf71 (the same record, commit 2fbe8ff): with JSON, wildcard, browser navigation, axios-style and absent Accept headers, on GET and POST /api/boards and in a 10-request burst, the 401 set no cookie, created no spring_session row and no statement appeared in the Postgres capture windows, and a real signin still created exactly one session row.
    - Not observed: other routes and methods, a proxy in the path, Redpanda, a stale session cookie, and TLS; one run on one machine; a statement outside the capture windows (one second after each request, plus one log-tail read after rows A to I; the whole log was not re-read).
    Write no apostrophe and no wildcard Accept literal, and keep the phrase `observed at 7bccf71` unbroken.

    .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md, the error-401 section only:
    - The provenance line names both quick tasks, 261028-n5v and 261028-opl.
    - Keep the "Measured before the fix", "Fixed" and "Pinned" bullets verbatim (I2's fixed-string gate holds the first).
    - Replace the "Not done" bullet and the inference bullet with two bullets. "Observed after the fix: 02-04-401-measure.txt, section Post-fix re-measure (HEAD 7bccf71), committed in 2fbe8ff" cites the heading line, rows A to I, the burst and the signin check, and quotes POST-FIX RESULT with its line. Its first sentence carries `observed at 7bccf71`, and its statement claim says no statement appeared in the capture windows. "Not observed" lists the limits with the limits line range, and includes the statement-capture limit with both cites (the Method lines on the capture and the limits lines on it): a 1-second window after each request plus one log-tail read after rows A to I, the whole log not re-read, so a statement outside those windows is not ruled out.
    - In the closing paragraph after the table (line 40), the last two sentences, from `What a cookie-less request wrote` to the end of the paragraph, become one sentence: what a cookie-less request wrote under IF_REQUIRED was measured before the fix, and after the fix no row, no cookie and no statement in the capture windows (one second after each request, plus one log-tail read after rows A to I; the whole log was not re-read) were observed at 7bccf71 for the cases above the table.
    - The four numbered rows stay unchanged (G5 `error-401 4 4`).

    docs/DIAGRAM_CONVENTIONS.md, the three 401 statements only:
    - Scenarios row (line 113), the final clause after `error-401` draws no SQL step: measured before the fix at 82775fa, and observed at 7bccf71 after it, with no row, no cookie and no statement in the capture windows (a one-second window after each request, plus one log-tail read after rows A to I; the whole log was not re-read; note (b2) lists the other limits).
    - Note (b2): keep the opening sentence, the pre-fix measurement with verdict b, and the 17a94cb and AuthorizationGatingTest sentence. Replace the closing limits sentence with two or three sentences. A second live run, observed at 7bccf71 and recorded in the same file (commit 2fbe8ff), saw no Set-Cookie, no spring_session row and no statement in the capture windows for JSON, wildcard, browser navigation, axios-style and absent Accept headers, on GET and POST /api/boards and across a 10-request burst, while a real signin still created exactly one session row. Not observed: other routes and methods, a proxy in the path, Redpanda, a stale session cookie and TLS, in one run on one machine, and a statement outside the capture windows (one second after each request, plus one log-tail read after rows A to I; the whole log was not re-read).
    - `error-401` row of the Phase 2 table, Provenance cell: authored, steps cited; no SQL step, measured before the fix at 82775fa, observed at 7bccf71 after it, with no statement in the capture windows (one second after each request, plus one log-tail read; the whole log was not re-read; note (b2) lists the other limits). Leave the size cell for Task 3.

    ROADMAP.md: no edit. Its criterion 3 clause "401 with no SQL step" is still true of the view.

    Run G1 (61 elements), G2, G4, G5, G6, I1 to I7, L1, L2, L3, L4, M1 and the L5 commands for the model, the standard and the phase folder. Then commit the four files. Subject along the lines of `docs(architecture): state the 401 path as observed at 7bccf71 after the fix`.
  </action>
  <verify>
    <automated>docs/architecture/node_modules/.bin/likec4 export json --skip-layout -o docs/architecture/out/export.json docs/architecture/model < /dev/null && node -e "for(const v of Object.values(require('./docs/architecture/out/export.json').views))if(v._type==='dynamic')console.log(v.id,(v.tags||[]).join('+'),Object.keys(v.nodes).length,Object.keys(v.edges).length,Object.values(v.edges).filter(e=>!e.label).length)"</automated>
    <fails_when>non-zero exit, or the 13 lines differ from G4 (error-401 must stay `error-401 scenarios 3 4 0`)</fails_when>
    <automated>awk '/dynamic view error-401/,/^  }$/' docs/architecture/model/scenarios-app.c4 | grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e '2fbe8ff' -e '82775fa' -e 'proxy' -e 'stale' -e 'capture window' | sort | uniq -c</automated>
    <fails_when>an `inferred from the code` line is printed, or any of `2fbe8ff`, `82775fa`, `observed at 7bccf71`, `proxy`, `stale`, `capture window` is missing</fails_when>
    <automated>awk '/^## view error-401$/,/^## view error-403$/' .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md | tr '\n' ' ' | grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e '2fbe8ff' -e '82775fa' -e 'verdict b' -e 'proxy' -e 'stale' -e 'Redpanda' -e 'capture window' | sort | uniq -c</automated>
    <fails_when>an `inferred from the code` line is printed, or any of the other eight literals is missing</fails_when>
    <automated>awk '/^\*\*\(b2\)/,/^$/' docs/DIAGRAM_CONVENTIONS.md | tr '\n' ' ' | grep -o -e 'inferred from the code' -e 'observed at 7bccf71' -e '2fbe8ff' -e 'verdict b' -e '17a94cb' -e 'proxy' -e 'stale' -e 'capture window' -e 'not measured' -e 'did not run' | sort | uniq -c</automated>
    <fails_when>any of `inferred from the code`, `not measured`, `did not run` is printed, or any of `observed at 7bccf71`, `2fbe8ff`, `verdict b`, `17a94cb`, `proxy`, `stale`, `capture window` is missing</fails_when>
    <automated>P=.planning/workstreams/architecture-views/phases/02-model-completion; out=$(git diff --numstat 2fbe8ff -- $P/02-04-401-measure.txt) || exit 1; printf '%s\n' "$out" | awk '{a+=$1; d+=$2} END {print a+0, d+0; exit !(a==1 && d==0)}'</automated>
    <fails_when>non-zero exit, or the output is not `1 0`</fails_when>
  </verify>
  <acceptance_criteria>
    - I4: the Scenarios row prints `1 observed at 7bccf71` and at least one `capture window`, and no `inferred from the code` line (baseline `1 inferred from the code` only). I5: the `error-401` row, the same as I4. I6: `rg -c 'inferred from the code' docs/DIAGRAM_CONVENTIONS.md` prints nothing (baseline 3).
    - I2 fixed string: `P=...; rg -c -F 'Measured before the fix: 02-04-401-measure.txt in this folder, 2026-10-28T14:44Z, host commit 82775fa, verdict b' $P/02-SCENARIO-EVIDENCE.md` prints 1.
    - I7: `P=...; rg -c 'inferred from the code' $P/02-04-401-measure.txt` prints 1; `P=...; rg -c 'observed at 7bccf71' $P/02-04-401-measure.txt` prints 1 (baseline nothing); `P=...; grep -c -e '^VERDICT:' -e '^POST-FIX RESULT:' $P/02-04-401-measure.txt` prints 2.
    - L4 prints 3; L1, L2 and L3 print nothing and exit 0; the three L5 commands exit 0.
    - Each measure-record line number cited in the new evidence bullets shows the cited text: for every cited number, `sed -n` on the record contains it. The pairs are recorded in the SUMMARY.
    - `rg -c 'did not run' docs/architecture/model/scenarios-app.c4` prints nothing; `git status --short -- .planning/workstreams/architecture-views/ROADMAP.md` prints nothing.
    - After the commit, `git status --short` on the four files prints nothing.
  </acceptance_criteria>
  <done>The error-401 view, its evidence, note (b2) and both standard rows state that no row, no cookie and no statement in the capture windows were observed at 7bccf71 (2fbe8ff), with the pre-fix history, the untried cases and the statement-capture limit named. The measure record keeps every line and points from its dated inference to the observation.</done>
</task>

<task type="auto">
  <name>Task 3: Record the re-exported view sizes, answer the override question in 02-VERIFICATION.md, then the full gate</name>
  <precondition>Tasks 1 and 2 are committed: `git status --short -- docs .planning/workstreams/architecture-views/phases/02-model-completion` prints nothing.</precondition>
  <files>docs/DIAGRAM_CONVENTIONS.md, .planning/workstreams/architecture-views/phases/02-model-completion/02-VERIFICATION.md</files>
  <read_first>
    - docs/DIAGRAM_CONVENTIONS.md lines 339-362 (the Phase 2 table and its intro)
    - docs/architecture/likec4.fish lines 1-36
    - .planning/workstreams/architecture-views/phases/02-model-completion/02-VERIFICATION.md lines 47-51 and 220-248
    - The Task 1 and Task 2 commit SHAs (`git log --oneline -3`)
  </read_first>
  <action>
    Sizes (the Phase 2 table records natural sizes, so the monitoring size is re-exported). Run `fish docs/architecture/likec4.fish png < /dev/null` (about 3 minutes), then `file docs/architecture/out/*.png`. The natural size is the PNG width and height divided by 2 (standard V6). Replace the `monitoring` row size cell with the new size; the old size must not remain. Compare every other Phase 2 row with its PNG, update each row whose size changed, and list the changed rows in the SUMMARY (expected: monitoring; any other change is reported, not explained away). Add one sentence to the table intro: the `monitoring` size (and any other row updated) was re-exported on 2026-10-28 after quick task 261028-opl drew the chart-default scrape targets. If the export cannot run, write `not re-exported` with the reason in the monitoring size cell instead, and say so. Afterwards trash the PNGs the export wrote.

    02-VERIFICATION.md, per the scope (do not write an override; the user chose to draw instead). Append at the end of the file, changing no existing line, a section headed `## Addendum, 2026-10-28 (quick task 261028-opl): human item 1 answered`. In substance:
    - The user chose option (b): draw the chart-default scrape targets.
    - No override was written. The frontmatter is unchanged, so its status and score stay for the next verifier run, which re-checks criterion 1 and PHYS-03.
    - The Task 1 commit draws Prometheus edges to the kubelet with cAdvisor, the API server, kube-state-metrics and the operator, plus CoreDNS and Grafana, which the full values file showed are also left on. The self-scrape is stated, not drawn, with its measured cause. The turned-off monitors are not drawn.
    - The record is `.planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-chart-values.txt` (byte count and digest).
    - Criterion 3: the statement-level absence of SQL after 17a94cb, which this report carried as an inference, now rests on the post-fix re-measure at 7bccf71 (02-04-401-measure.txt, Post-fix re-measure, commit 2fbe8ff): no statement appeared in its capture windows (one second after each request, plus one log-tail read after rows A to I). The whole log was not re-read, so a statement outside those windows is not ruled out. The Task 2 commit carries that, with the limit, into the view, the evidence and the standard.
    Name both commit SHAs. The addendum states what changed and does not set a verdict.

    Then the full gate on the final tree: G1, G2, G3, G3b, G3c, G4, G5, G6, G7, G8, L1, L2, L3, L4, L5 (all five), M1, V1, V2 and V3, plus N1 to N6 and I1 to I7. Commit DIAGRAM_CONVENTIONS.md and 02-VERIFICATION.md. Subject along the lines of `docs(02): record the monitoring view size and answer the phase 02 override question`. Trash docs/architecture/out/export.json.
  </action>
  <verify>
    <automated>node docs/architecture/sync.mjs check < /dev/null</automated>
    <fails_when>non-zero exit, or the tally is not `61 elements` with `17 workloads`</fails_when>
    <automated>python3 scripts/pre-commit-likec4-selftest.py < /dev/null</automated>
    <fails_when>non-zero exit, or the last line is not `16/16 passed`</fails_when>
    <automated>fish docs/architecture/drill.fish < /dev/null 2>/dev/null | cmp - docs/architecture/drill-output.txt</automated>
    <fails_when>non-zero exit: the replay differs from the record that Task 1 re-recorded</fails_when>
    <automated>docs/architecture/node_modules/.bin/likec4 export json --skip-layout -o docs/architecture/out/export.json docs/architecture/model < /dev/null && node -e "const v=Object.values(require('./docs/architecture/out/export.json').views).filter(x=>x._type==='deployment');console.log(v.filter(x=>(x.tags||[]).includes('physical')).map(x=>x.id).sort().join(','),v.flatMap(x=>Object.values(x.edges)).filter(e=>e.label==='[...]').length)"</automated>
    <fails_when>non-zero exit, or the output is not exactly `delivery,monitoring,production 0`</fails_when>
    <automated>node -e "const t=require('fs').readFileSync(process.argv[1],'utf8');for(const v of Object.values(require('./docs/architecture/out/export.json').views))if(v._type==='dynamic'){const s=t.split('\n## view '+v.id+'\n')[1];console.log(v.id,Object.keys(v.edges).length,s?s.split('\n## ')[0].split('\n').filter(l=>/^\| \d+ \|/.test(l)).length:'none')}" .planning/workstreams/architecture-views/phases/02-model-completion/02-SCENARIO-EVIDENCE.md # long-ok: plan gate</automated>
    <fails_when>any of the ten Phase 2 views prints two unequal numbers or `none`, or a signin view prints anything but `none`</fails_when>
    <automated>P=.planning/workstreams/architecture-views/phases/02-model-completion; old=$(git show 2fbe8ff:$P/02-VERIFICATION.md) || exit 1; diff <(printf '%s\n' "$old" | awk 'NR==1{next} /^---$/{exit} 1') <(awk 'NR==1{next} /^---$/{exit} 1' $P/02-VERIFICATION.md)</automated>
    <fails_when>any output or non-zero exit: the frontmatter changed</fails_when>
    <automated>P=.planning/workstreams/architecture-views/phases/02-model-completion; out=$(git diff --numstat 2fbe8ff -- $P/02-VERIFICATION.md) || exit 1; printf '%s\n' "$out" | awk '{a+=$1; d+=$2} END {print a+0, d+0; exit !(a>0 && d==0)}'</automated>
    <fails_when>non-zero exit: nothing was appended, or an existing line was changed or deleted</fails_when>
    <automated>P=.planning/workstreams/architecture-views/phases/02-model-completion; awk '/^## Addendum, 2026-10-28 \(quick task 261028-opl\)/,0' $P/02-VERIFICATION.md | grep -c 'capture window'</automated>
    <fails_when>prints 0 or exits non-zero: the addendum is missing, or its criterion 3 sentence does not name the statement-capture limit</fails_when>
  </verify>
  <acceptance_criteria>
    - `grep -E '^\| .monitoring. \|' docs/DIAGRAM_CONVENTIONS.md | grep -c '3490 x 1574'` prints 0 (baseline 1), and the cell holds the size `file` printed, halved, or `not re-exported` with a reason.
    - `P=...; awk 'NR==1{next} /^---$/{exit} 1' $P/02-VERIFICATION.md | grep -c '^overrides:'` prints 0; `P=...; rg -c -e '261028-opl-chart-values.txt' -e '2fbe8ff' $P/02-VERIFICATION.md` prints at least 1 (baseline nothing); V3 prints at least 1 (baseline 0).
    - G1, G3b, G3c, G4, G6, G7r, L1 to L4, all five L5 commands, M1, and N1 to N6 and I1 to I7 give their "after" values from the context block.
    - `git diff --stat 2fbe8ff..HEAD` lists exactly the eight files in files_modified; `git log --oneline 2fbe8ff..HEAD` shows three commits; `git diff --stat 2fbe8ff..HEAD -- k8s src scripts docs/architecture/sync.mjs docs/architecture/drill.fish docs/architecture/model/views.c4 .planning/workstreams/architecture-views/ROADMAP.md` prints nothing.
    - `git status --short -- docs .planning/workstreams/architecture-views/phases/02-model-completion` prints nothing; docs/architecture/out holds no export.json and none of the PNGs this task wrote.
  </acceptance_criteria>
  <done>The standard records the size of the monitoring view as now drawn, 02-VERIFICATION.md records the user's answer to human item 1 without an override, and every gate passes on the final tree with the rules, the scenario views and the drill outcomes unchanged.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| upstream chart file to the repo | values.yaml is downloaded from GitHub at a tag and read into a committed record |
| measurement record to committed text | post-fix results are copied into the model, the evidence and the standard |
| model descriptions to the published site | .c4 text becomes public in Phase 3 |
| shared git index | other tasks' untracked files sit in the same checkout |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-opl-01 | Tampering | downloaded values.yaml | medium | mitigate | Fetched with the authenticated gh CLI at an immutable tag into its own new directory, read only with sed and rg, never executed, and trashed after. The byte count and sha256 are compared with the planning digest and recorded, and a mismatch is reported rather than edited away |
| T-opl-02 | Repudiation (a model or standard that overclaims) | monitoring.c4, DIAGRAM_CONVENTIONS.md, scenarios-app.c4, evidence | medium | mitigate | Each edge is tied to a values line in the record, and ports and protocols appear only where a values line states them. The live target list, the chart templates and the Alloy defaults are stated as not read. The 401 text claims only the post-fix record and lists its limits, the statement-capture windows included (I1 to I5, V3). The N and I gates hold the old claims out and the new ones in |
| T-opl-03 | Information disclosure | record, descriptions, evidence | medium | mitigate | The record copies no authorization or credentials lines. The model names no host, IP or local port. L1 to L4 grep for IP literals, cookie values and credentials, and L5 runs gitleaks 8.30.1 over each changed path |
| T-opl-04 | Tampering (commit scope) | git index | low | mitigate | `git diff --cached --name-only` is empty before each commit, and `git commit --no-verify -- FILE1 FILE2` takes explicit paths. Task 3 checks that the diff lists exactly the eight files |
| T-opl-05 | Tampering (skipped hook) | `--no-verify` commits | low | accept | The project rule allows it for docs and planning commits. G1, G8 and L5 replace the hook steps, and CI still runs gitleaks on a push |
| T-opl-SC | Tampering | npm/pip/cargo installs | high | mitigate | This plan installs no package |
</threat_model>

<verification>
- G1 exit 0 with 61 elements and 17 workloads; G8 16/16; G7 byte-identical to the re-recorded file, and G7r shows only tally counts changed.
- G3 `delivery,monitoring,production 0`; G3b monitoring 25 17, production 24 17, delivery 14 8; G3c 13 Prometheus edges; G4 the same 13 lines; G5 ten equal pairs and three `none`; G6 nothing.
- N1 to N6 and I1 to I7 at their "after" values; L1 to L3 nothing; L4 3; L5 clean; M1 `1 0`; V1 exit 0; V2 no output; V3 at least 1.
- No change since 2fbe8ff to Java, migrations, k8s, scripts, sync.mjs, drill.fish, views.c4, deployment.c4, spec.c4 or ROADMAP.md.
</verification>

<success_criteria>
- Criterion 1 / PHYS-03: the monitoring view draws an edge to each thing Prometheus scrapes under the repo settings and the chart defaults at 91.5.2, except its own self-scrape, which is stated with a measured cause. Every edge is sourced, and nothing turned off is drawn.
- Criterion 3 / SCEN-02: the 401 view and its evidence state the post-fix observation at 7bccf71 with its limits, and keep the pre-fix history.
- STD-01: the standard's Physical, Checks, monitoring, Scenarios, (b2) and `error-401` text says what is drawn and observed, with no leftover sentence from the previous pass that is now false.
- Human item 1 in 02-VERIFICATION.md is answered by an addendum, without an override. Re-verification is the orchestrator's next step.
</success_criteria>

<output>
Create `.planning/workstreams/architecture-views/quick/261028-opl-phase-02-final-gap-pass-draw-kube-promet/261028-opl-SUMMARY.md`. Record:

- `$BASE` and the three commit SHAs.
- The values download: command, bytes and sha256, and any line that differed from the plan's table.
- The six drawn edges with their titles. Say plainly that CoreDNS and Grafana go beyond the four targets the scope named, and why. Say that the API server title names no protocol (changed in plan revision): no values line in the kubeApiServer block states the scheme, so only the kubelet title says HTTPS (line 1900).
- The 25 nodes of `monitoring`, named: list the node ids the export prints for the view and name the one that is neither an earlier node nor one of the five new instances (expected: the implicit `netcup.k3s.kube-system` ancestor that the CoreDNS instance brings in). If it is something else, name it and say so.
- The Prometheus self-scrape exception and its probe output, and the targets not drawn, each with its reason.
- That no rule and no drill was added, and why. Quote the G7r output.
- The PNG sizes old and new, and every row changed.
- The measure-record lines cited and their `sed -n` confirmations, the statement-capture lines (Method and limits) included.
- That the Task 1 record gate (the `rg -o ... | sort -u | wc -l` count of 9) is a presence check only: it proves the tag, the probe result and the monitor keys appear in the record, not that each cited line number and quote is right. The citation check rests on the executor reading each line with `sed -n` against the downloaded file in Step 1; say that this is what was done, row by row, or say which rows were not checked.
- Each gate changed from the n5v plan, and why: G1 tally, G7 re-record, L2 scope, L4 count, M1 base and count, the inference-label gates I1 to I6, which now require its absence, and I1 to I5 plus the new V3, which require `capture window` so every no-statement claim names the capture limit (plan revision).
- What was not done: live targets, chart templates, Alloy defaults, ROADMAP unchanged, STATE.md left to the orchestrator.
- The chart-bump seam as a candidate tracked item: no check reads the chart defaults.
</output>
