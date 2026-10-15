# Phase 14 free-tier research

Read on 2026-10-15. Every "verified Y" row means the figure was read this session on the cited page. "UNVERIFIED" says what was tried.

## Verdicts

1. Telegram bot as Alertmanager receiver: SUITABLE WITH CAVEATS. `telegram_configs` and `bot_token_file` exist in the Alertmanager the chart pins (v0.34.1); caveats are that Alertmanager is currently disabled in this repo, group chats cap at 20 messages/minute, and the HTML-mode oversize fallback replaces the alert text.
2. healthchecks.io as dead-man's switch: SUITABLE WITH CAVEATS. Free Hobbyist plan covers 20 checks with a 60-second minimum period and a free Telegram integration; caveats are `send_resolved: false`, a one-shot alert, a ~10-15 minute detection delay, a one-person operator with no SLA, and deletion of accounts inactive for over a year.
3. GitHub Actions uptime workflow posting to Telegram: SUITABLE WITH CAVEATS. Minutes are free on public repos (the repo is public, checked with `gh api`), but scheduled workflows are auto-disabled after 60 days without repository activity, and this workflow already lists that as a known hole.
4. Off-VM backup storage: SUITABLE WITH CAVEATS. Recommended provider: **Backblaze B2, EU Central (Amsterdam) account**. It needs no credit card, offers write-only single-bucket keys, Object Lock and lifecycle rules at no extra charge in the docs read; Cloudflare R2 is the runner-up but cannot do write-only keys and needs a payment method on file.

Finding that changes the plan: `k8s/monitoring/controllers/kube-prometheus-stack.yaml` sets `alertmanager.enabled: false` and `defaultRules.create: false`. The `Watchdog` alert does not exist today. Enabling Alertmanager and the default rules (or defining a custom always-firing rule) is a prerequisite for decisions 1 and 2, and it costs memory on a 7.8 GiB VM (not measured here).

## Decision 1: Telegram as Alertmanager receiver

| Fact | Value | Source | Verified |
|---|---|---|---|
| Alertmanager version shipped by chart 91.5.2 | `quay.io/prometheus/alertmanager` tag `v0.34.1` (chart appVersion v0.94.1 is the Prometheus Operator, not Alertmanager) | https://raw.githubusercontent.com/prometheus-community/helm-charts/kube-prometheus-stack-91.5.2/charts/kube-prometheus-stack/values.yaml (line ~1033) and Chart.yaml | Y |
| `telegram_configs` receiver supported | Yes, with `bot_token`, `bot_token_file`, `chat_id`, `chat_id_file`, `message_thread_id`, `message`, `disable_notifications`, `parse_mode`, `send_resolved` | https://raw.githubusercontent.com/prometheus/alertmanager/v0.34.1/docs/configuration.md (section `<telegram_config>`) | Y |
| Token from a Kubernetes Secret | `bot_token` and `bot_token_file` are mutually exclusive; mount the Secret via `alertmanager.alertmanagerSpec.secrets: [name]`, which lands in `/etc/alertmanager/secrets/<name>/`, then point `bot_token_file` at the file. The chart default is `secrets: []`. | configuration.md above; chart values.yaml line ~1045 | Y |
| Token file is re-read on each send | `getBotToken()` calls `os.ReadFile` per notification, so rotating the Secret needs no restart | https://raw.githubusercontent.com/prometheus/alertmanager/v0.34.1/notify/telegram/telegram.go | Y |
| Chat id from file | `chat_id_file` exists; the value is parsed with `strconv.ParseInt`, so a group id like `-100123...` works | telegram.go | Y |
| Default `parse_mode` | `HTML`; accepted values MarkdownV2, Markdown, HTML, empty string for plain text | configuration.md | Y |
| 4096-character limit | Code constant `maxMessageLenRunes = 4096` (counted in runes). In non-HTML modes the text is truncated with a log warning. In HTML mode a longer message is not truncated: it is replaced by "Alertmanager notification could not be sent: message length exceeds Telegram limits." | telegram.go lines 33-100 | Y |
| HTML escaping | In HTML mode the template is rendered with `TmplHTML` (Go `html/template`), which escapes alert label/annotation values; a hand-written `message` template must use only Telegram-supported tags. Plain-text mode (`parse_mode: ""`) avoids the question. | telegram.go lines 83-96 | Y (code read; not exercised against the live Telegram API) |
| Default template | `telegram.default.message` lists firing and resolved alerts via `__text_alert_list` | https://raw.githubusercontent.com/prometheus/alertmanager/v0.34.1/template/default.tmpl | Y |
| Cost | Bots message users at no cost by default | https://core.telegram.org/bots/faq (read 2026-10-15) | Y |
| Rate limits | One message per second per chat (short bursts allowed, then 429); groups: no more than 20 messages per minute; bulk: about 30 messages per second | https://core.telegram.org/bots/faq | Y |
| Failure behaviour | The notifier returns `retry=true` on send errors, so Alertmanager retries on 429 or network failure | telegram.go lines 104-122 | Y |
| ToS | Bot Developer Terms: a bot "must not harass or spam users with unsolicited messages" and must comply with Telegram ToS. A bot that alerts the owner's own chat is not unsolicited. No clause found that bans automated alerting. | https://telegram.org/tos/bot-developers | Y (read via keyword excerpts, not the full 50 KB) |
| Two chats (prod loud, nonprod muted) | Two receivers with different `chat_id_file`; nonprod uses `disable_notifications: true`. Either two bots or one bot added to two chats works. | configuration.md | Y |
| Telegram uptime or SLA | None published on the pages read | n/a | UNVERIFIED (no SLA page found) |

Gotchas:

- Alertmanager is disabled in this repo today (`alertmanager.enabled: false`, `defaultRules.create: false`).
- Helm replaces lists, so supplying `alertmanager.config.route.routes` or `receivers` replaces the chart defaults, including the default Watchdog-to-`null` route (hyperping.com guide, see decision 2).
- Group chats: 20 messages/minute is a per-bot limit for a group. Alertmanager groups alerts into one message per group, so a burst of 50 alerts is a few messages, not 50.
- Group-id sign: supergroup ids are negative (`-100...`). A wrong id returns 400 "chat not found", which Alertmanager retries as an error.
- A bot cannot message a person who has not pressed Start, and must be added to a group before its id works.

## Decision 2: healthchecks.io as the dead-man's switch

| Fact | Value | Source | Verified |
|---|---|---|---|
| Hobbyist price | $0 / month | https://healthchecks.io/pricing/ (read 2026-10-15) | Y |
| Checks | 20 on Hobbyist and Supporter | pricing page | Y |
| Log retention | 100 log entries per job; at one ping per 5 minutes that is about 8 hours of history | pricing page | Y |
| SMS / WhatsApp / phone calls | Not on Hobbyist (0 credits; only Business has 50 SMS and 20 call credits) | pricing page; https://healthchecks.io/docs/healthchecks_cronitor_comparison/ marks SMS, WhatsApp, calls "paid plans only" | Y |
| Telegram integration | Listed as an integration without a "paid plans only" marker (SMS, WhatsApp, calls, PagerDuty, Splunk On-Call carry it) | https://healthchecks.io/docs/healthchecks_cronitor_comparison/ (read 2026-10-15) | Y (list read; free-plan Telegram not exercised) |
| Minimum period and grace | 60 seconds each (`IntegerField(min_value=60, ...)`); defaults 1 day period, 1 hour grace | https://raw.githubusercontent.com/healthchecks/healthchecks/master/hc/front/forms.py, models.py (master branch, not necessarily the deployed build) | Y (source code; not the live UI) |
| Ping rate limit | More than 5 pings per minute per check may be dropped | https://healthchecks.io/docs/http_api/ | Y |
| Ping methods | HEAD, GET, POST; POST body up to 100 kB stored | https://healthchecks.io/docs/http_api/ | Y |
| Alert style | One-shot; email reminders hourly or daily are optional | https://healthchecks.io/faq/ | Y |
| Non-commercial requirement | None found. ToS (last updated 2018-05-09) has no use restriction; FAQ's multiple-accounts rule says personal and company infrastructure may have separate Hobbyist accounts. | https://healthchecks.io/terms/ and /faq/ | Y |
| Inactivity / deletion | Account inactive (no log-in, no activity in logged-in sessions) for over a year gets an "Inactive Account Notification" email and is deleted after 30 days unless the user logs in. Pings from a system do not count as activity per the wording; a forgotten system "still sending pings" is named as a case. A Hobbyist account that exceeds limits is deleted after weekly warnings. | https://healthchecks.io/faq/ | Y |
| Operator and reliability | One-person company (SIA Monkey See Monkey Do, Latvia), Hetzner Germany, "multi-hour or even multi-day outages are possible", no SLAs | https://healthchecks.io/faq/ | Y |
| Data location / GDPR | Servers in Germany, company in Latvia, GDPR applies; no DPA signed with customers. Do not put personal data in ping bodies. | https://healthchecks.io/faq/ | Y |
| Open-source / nonprofit | Business plan free for open-source projects and nonprofits on request | pricing page | Y |

Mechanism (how an always-firing alert pings it):

- Alertmanager route for `alertname = "Watchdog"` goes to a `webhook_configs` receiver whose URL is the check's `https://hc-ping.com/<uuid>`. The webhook sends an HTTP POST with a JSON body, which the ping endpoint accepts.
- Use `url_file` (webhook config supports `url` or `url_file`, mutually exclusive, from the same Secret mount) so the ping URL, which is a secret, stays out of Git.
- Alertmanager only re-sends on a `group_interval` tick once `repeat_interval` has elapsed. Recommended: `group_wait: 0s`, `group_interval: 1m`, `repeat_interval: 5m`, `send_resolved: false`; healthchecks check Period 5 minutes (or 10), Grace 5 minutes. Pings arrive every 5-6 minutes with these settings.
- Detection delay: Prometheus marks each alert with an end time of 4x max(eval interval, resend delay) (about 4 minutes), so after Prometheus dies the Watchdog stays "firing" in Alertmanager a few minutes. Expect the Telegram alert about 10-15 minutes after the pipeline breaks (hyperping.com measured the same arithmetic).
- Sources for the mechanism and measured timings are third-party guides, not primary: https://hyperping.com/blog/how-to-monitor-prometheus-alertmanager (2026-10-08), https://blog.ediri.io/how-to-set-up-a-dead-mans-switch-in-prometheus, https://fractal-techware.github.io/runbooks/watchdog/. The `url_file`, `send_resolved` and `repeat_interval` semantics themselves are in the primary configuration.md (v0.34.1). Timings are UNVERIFIED on this cluster until tested.

Pitfalls:

- `send_resolved` defaults to true for webhooks: when Prometheus dies, Alertmanager sends a "resolved" POST, which the check counts as a ping and delays the alert by a cycle. Set it false.
- The chart's default route sends Watchdog to the `null` receiver; the new route must come before it and Helm list-replacement applies.
- One check per Alertmanager. nonprod and prod share one Alertmanager here, so one check; do not share a URL across clusters.
- The chart only emits `Watchdog` if `defaultRules.create` is true (and `defaultRules.disabled.Watchdog` is not set). Today it is false.
- A dead VM stops the pings too, which is exactly what the check is for. It does not catch "pipeline works but Telegram receiver broken": healthchecks.io alerts through its own Telegram channel, so that path is covered separately.
- The healthchecks.io Telegram integration is a bot run by healthchecks.io; it needs the human to authorise it in the chat.
- A paused or ignored check does not alert; keep the check active.

Alternatives:

| Service | Free tier | Source | Verified |
|---|---|---|---|
| Dead Man's Snitch | One snitch ("The Lone Snitch") free; paid from $5/month for 3 snitches. Fits exactly one check; Telegram not confirmed. | https://deadmanssnitch.com/plans | Y for plan sizes; Telegram UNVERIFIED |
| Cronitor | Hacker plan $0, 5 monitors, email and Slack alerts, no SMS. Telegram not listed on the pricing page text read. | https://cronitor.io/pricing | Y; Telegram UNVERIFIED |
| Better Stack | "Free for personal projects" $0: 10 monitors and heartbeats, Slack and e-mail alerts, 1 status page | https://betterstack.com/pricing | Y; Telegram on free UNVERIFIED |

healthchecks.io stays the pick: most checks (20), documented free Telegram, EU-hosted. Better Stack is the fallback if the one-person-operator risk matters.

## Decision 3: GitHub Actions uptime workflow

| Fact | Value | Source | Verified |
|---|---|---|---|
| Auto-disable | "In a public repository, scheduled workflows are automatically disabled when no repository activity has occurred in 60 days." Re-enable per "Disabling and enabling a workflow". The page does not list which events count as activity. | https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows (read 2026-10-15) | Y; which events count UNVERIFIED |
| Minimum interval | Once every 5 minutes; `*/15` is fine | same page | Y |
| Delays | Schedule events "can be delayed during periods of high loads"; high load includes the start of every hour; "some queued jobs may be dropped" under sufficiently high load. Use an off-peak minute (not `:00`) for the cron. | same page | Y |
| Runs on default branch only | Schedule only runs for a workflow file on the default branch, on the latest commit | same page | Y |
| Minutes on public repos | "GitHub Actions usage is free for self-hosted runners and for public repositories that use standard GitHub-hosted runners." Larger runners are always charged. | https://docs.github.com/en/billing/concepts/product-billing/github-actions (read 2026-10-15) | Y |
| Repo visibility | `gh api repos/<repo> --jq .private` returned `false` | local `gh` run | Y |
| Existing workflow | `.github/workflows/uptime-check.yml` already exists with `*/15 * * * *`, probes both health URLs, and its header lists the 60-day hole and "pages nobody" | repo file | Y |
| Telegram from Actions | `curl` to `https://api.telegram.org/bot<token>/sendMessage` with the token and chat id in Actions secrets; Bot FAQ limits are far above one message per 15 minutes | https://core.telegram.org/bots/faq | Y |

Caveats:

- Monthly volume: 96 runs/day (about 2,880/month) of well under a minute each, free on a public repo. Not a limit risk.
- 60-day disable is the real risk; the existing header already says it stops silently. Mitigations: a keepalive (a scheduled commit or a second workflow that re-enables via API) or an external check on the workflow's last-run time. Which of these counts as "activity" is UNVERIFIED, so test rather than assume.
- Runs from GitHub's network, so it sees the public URL as users do, which is the point; it does not see the VM's internal state.
- Secrets used from the workflow stay out of fork PRs; keep `permissions: {}` as it is.

## Decision 4: Off-VM backup storage

Our job's operation count (assumptions: one `pg_dump -Fc` per database per night, so up to two objects/night; encrypted file uploaded once; retention enforced by lifecycle rules, not by the job; weekly restore drill downloads one dump):

| Item | Per month | Basis |
|---|---|---|
| Uploads (PutObject) | about 60 (2 dumps x 30 nights) | single request while the file is under the SDK multipart threshold |
| Uploads if multipart (1 GB dump, 5 MiB parts) | about 200 parts x 60 = 12,000 | worst case, rclone default chunk size, UNVERIFIED for the tool settings chosen |
| List requests (existence check, drill) | about 100 | 1 per run + drill |
| Weekly drill GET/HEAD | about 10 | 4-5 drills x (List + Head + Get) |
| Stored objects | at most 22 | 7 daily + 4 weekly, per database |
| Stored bytes (tens of MB) | about 0.5-1 GB | far under 10 GB |
| Stored bytes (about 1 GB dump) | about 22 GB if both databases are 1 GB each, or about 11 GB for one | 11 objects x 1 GB per database; 12 GB over the free 10 GB would cost about $0.06/month at B2's $0.005/GB or about $0.18 at R2's $0.015/GB |
| Egress (drill) | about 5 x one dump | free on R2; B2 free up to 3x stored average |

Against free limits: Class A about 60 to 12,000 vs 1,000,000 (R2); Class B about 10 to 100 vs 10,000,000 (R2); B2 Class A, B and C calls are free for pay-as-you-go. Neither provider's operation limit is a constraint at either dump size.

| Fact | Cloudflare R2 | Backblaze B2 | Source | Verified |
|---|---|---|---|---|
| Free storage | 10 GB-month/month, Standard class only | First 10 GB always free | https://developers.cloudflare.com/r2/pricing/index.md (dated 2026-10-01); https://www.backblaze.com/cloud-storage/pricing | Y |
| Free operations | 1M Class A and 10M Class B per month | Class A, B, C free; Class D $0.004 per 10,000 with first 2,500/day free (our job uses none of the S3 calls listed under D on the transaction page read; Class D list not read in full) | R2 pricing page; https://www.backblaze.com/cloud-storage/transaction-pricing | Y; B2 Class D membership UNVERIFIED |
| Over-limit price | $0.015/GB-month; Class A $4.50/M; Class B $0.36/M | $0.005/GB-month (transaction page), pricing page headline "$6.95/TB" | same | Y (B2 page shows both $0.005/GB and $6.95/TB; they disagree slightly, rounding or a recent price change, UNVERIFIED which is current) |
| Egress | Free | Free up to 3x average monthly storage, then $0.01/GB | same | Y |
| Card required | Yes: R2 subscription checkout asks for billing info ("Complete the checkout flow to add an R2 subscription"); a Cloudflare community thread (2026-08-03) confirms the card dialog cannot be bypassed | No: sign-up page says "No credit card required." | https://developers.cloudflare.com/r2/get-started/; https://community.cloudflare.com/t/why-using-r2-free-tier-involves-giving-card-info/945179; https://www.backblaze.com/sign-up/s3 | Y (R2 via docs and community report; B2 via sign-up page text) |
| Free tier permanent | Free tier described as monthly allowance with no expiry on the pricing page; no end date stated | "always free" for the first 10 GB | pricing pages | Y for wording; no promise of future terms |
| EU region | `eu` jurisdiction guarantees EU storage (also `wnam`, `enam`, `weur`, `eeur`, `apac`, `oc` as location hints); endpoint `https://<account_id>.eu.r2.cloudflarestorage.com`; must be chosen when the bucket is created | EU Central = Amsterdam; chosen at account creation and cannot be changed afterwards | https://developers.cloudflare.com/r2/reference/data-location/index.md; https://www.backblaze.com/docs/cloud-storage-data-regions | Y |
| S3 API / rclone / aws cli | Yes; rclone and aws cli have official R2 pages | Yes (S3-compatible API); unsupported: ACLs, IAM roles, object tagging, website config, POST uploads, IPv6 | https://developers.cloudflare.com/r2/api/s3/api/index.md; https://www.backblaze.com/docs/cloud-storage-s3-compatible-api | Y (rclone against each not run) |
| Lifecycle rules | Yes: delete-after-N-days and transition to Infrequent Access; set via dashboard, Wrangler or S3 PutBucketLifecycleConfiguration; objects removed "within 24 hours" of expiry | Yes: `daysFromUploadingToHiding`, `daysFromHidingToDeleting`, prefix-scoped, per bucket. Deletion is two steps (hide, then delete), so "expire after 7 days" needs both fields | https://developers.cloudflare.com/r2/buckets/object-lifecycles/index.md; https://www.backblaze.com/docs/cloud-storage-lifecycle-rules | Y; B2 lifecycle via the S3 API UNVERIFIED (use console or native API) |
| Object lock / immutability | "Bucket locks" retention rules (dashboard, Wrangler, API; prefix + retention period) prevent deletion and overwrite; they take precedence over lifecycle. S3 `x-amz-object-lock-*` headers and `PutObjectLockConfiguration` are marked unsupported, so a client cannot request a lock via S3. Extra fee: none found on the pricing page. | Object Lock (S3-compatible API enabled; can be turned on for new or existing buckets; once on it cannot be turned off); governance and compliance modes not read in full | https://developers.cloudflare.com/r2/buckets/bucket-locks/index.md; https://www.backblaze.com/docs/cloud-storage-object-lock | Y; B2 modes and lock-on-free-account UNVERIFIED |
| Key scoping | Token types: Admin Read/Write, Admin Read only, Object Read/Write, Object Read only; the Object types can be scoped to chosen buckets. No write-only permission exists. Object scope works with the S3 API only. | Application keys: all buckets or one bucket (or a set via multi-bucket keys), with capabilities Read and Write, Read Only, or Write Only; can also be limited to a file-name prefix | https://developers.cloudflare.com/r2/api/tokens/index.md; https://www.backblaze.com/docs/cloud-storage-application-keys | Y |
| Delete protection from a stolen key | R2 Object Read/Write key can delete objects, but bucket lock blocks deletion until retention ends | Write-only key cannot delete or read; plus Object Lock | above | Y (design inference, not tested) |
| Account inactivity policy | Not found | Not found | n/a | UNVERIFIED (not searched beyond the pricing and sign-up pages) |

Recommendation for #4: **Backblaze B2, EU Central.** Reasons, in order: no card needed, a write-only single-bucket key lets the VM upload but never read or delete (a compromised VM key cannot wipe backups; R2 has no write-only role), Object Lock plus lifecycle rules give immutable 7-daily + 4-weekly retention, and the operation count is free. R2 is the fallback if a Cloudflare account already exists; there the nightly job's key would be Object Read & Write scoped to one bucket, with a bucket lock rule as the only delete protection.

Design points that follow from the facts:

- The restore drill needs a separate read-only key (B2 Read Only on the bucket); keep it off the VM, for example in GitHub Actions secrets.
- Retention via lifecycle: prefix `daily/` expires after 7 days, `weekly/` after 28 days. On B2 a locked file cannot be deleted until its retention date, so set the Object Lock default retention at or below the lifecycle age.
- A B2 account's region is fixed at creation (Amsterdam for EU); the EU choice must be made at sign-up.
- Encryption: dumps are already encrypted client-side (as stated), so B2 server-side encryption is not required. Do not rely on SSE-C or provider-side keys for the only copy of the key.
- At 1 GB per dump the free 10 GB is exceeded (about 11-22 GB stored); the cost is cents per month on B2, so "free" becomes "nearly free". Compress, or keep fewer weeklies, to stay under.

## Setup steps only the human can do

Telegram:

- Create a bot with @BotFather; save the token.
- Create the prod chat (and the nonprod chat), add the bot, send a message, read `getUpdates` to find each chat id; mute the nonprod chat in the client.
- Put the token and chat ids into Kubernetes Secrets (via the repo's secret mechanism; do not commit them).

healthchecks.io:

- Sign up (email magic link); create one check, Period 5 minutes, Grace 5 minutes.
- Add the Telegram integration (authorise the healthchecks bot in a chat) and attach it to the check.
- Copy the ping URL into a Kubernetes Secret as the file Alertmanager reads with `url_file`.
- Log in at least once a year to keep the account.

GitHub Actions:

- Add repository secrets for the bot token and chat id used by the failure step.
- Pick a keepalive approach for the 60-day rule and re-enable the workflow if it is ever disabled.

Backblaze B2:

- Create an account and choose the EU Central (Amsterdam) region at sign-up.
- Create a private bucket with Object Lock enabled and lifecycle rules; create a Write Only application key restricted to that bucket for the VM, and a Read Only key for the drill.
- Store the keys as Kubernetes Secret and GitHub Actions secret respectively; store the dump encryption key off the VM too (a password manager).
- Optionally set a billing cap or alert in the B2 dashboard.

## What this rests on

Pages read in full or by targeted excerpt on 2026-10-15 (fetched with `curl` and parsed locally, or with the web-access `fetch` tool for the first three):

- https://prometheus.io/docs/alerting/latest/configuration/ (the "latest" page did not contain the `<telegram_config>` section; the v0.34.1 file from GitHub did)
- https://raw.githubusercontent.com/prometheus/alertmanager/v0.34.1/docs/configuration.md, notify/telegram/telegram.go, template/default.tmpl
- https://raw.githubusercontent.com/prometheus-community/helm-charts/kube-prometheus-stack-91.5.2/charts/kube-prometheus-stack/Chart.yaml, values.yaml, templates/prometheus/rules-1.14/general.rules.yaml
- https://core.telegram.org/bots/faq, https://telegram.org/tos/bot-developers (keyword excerpts only), https://core.telegram.org/bots/api (searched for 4096; no hit, the limit comes from the Alertmanager code constant)
- https://healthchecks.io/pricing/, /faq/, /terms/, /docs/http_api/, /docs/configuring_checks/, /docs/configuring_notifications/, /docs/healthchecks_cronitor_comparison/; healthchecks source `hc/front/forms.py` and `hc/api/models.py` on the master branch
- https://deadmanssnitch.com/plans, https://cronitor.io/pricing, https://betterstack.com/pricing
- https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows, https://docs.github.com/en/billing/concepts/product-billing/github-actions
- https://developers.cloudflare.com/r2/pricing/, /r2/buckets/object-lifecycles/, /r2/buckets/bucket-locks/, /r2/api/tokens/, /r2/reference/data-location/, /r2/api/s3/api/, /r2/get-started/
- https://www.backblaze.com/cloud-storage/pricing, /cloud-storage/transaction-pricing, /sign-up/s3, and docs pages for application keys, lifecycle rules, Object Lock, data regions, S3-compatible API
- Search results (Exa): hyperping.com and ediri.io and fractal-techware guides on Watchdog wiring; Cloudflare community thread on R2 payment method; Backblaze blog "Free Isn't Always Free" (2022, used only as a secondary note on card requirement).
- Repo files: `k8s/monitoring/controllers/kube-prometheus-stack.yaml`, `.github/workflows/uptime-check.yml`, `docs/INFRA_RUNBOOK.md` (line 383: no production backup exists today).

Not verified, and what was tried:

- Nothing was exercised against live services: no message sent to Telegram, no ping to healthchecks.io, no `rclone` run against R2 or B2. Behaviour is from docs and source.
- Dump size: not measured; both sizes are shown above.
- Which events count as "repository activity" for the 60-day rule: the docs page does not say.
- Whether the free healthchecks.io plan lets the Telegram integration be used with no hidden limit: inferred from the absence of a "paid plans only" marker.
- Telegram on the free tier of Dead Man's Snitch, Cronitor and Better Stack: not read.
- B2 lifecycle through the S3 API, B2 Object Lock modes on a free account, B2 Class D membership, and either provider's inactivity or deletion policy for idle free accounts: not read.
- B2 price: the pricing page shows $6.95/TB while the transaction page shows $0.005/GB; I did not reconcile them.
- Alertmanager and Prometheus memory cost on the 7.8 GiB VM: not measured.
- Telegram's formal uptime record: no primary source looked up.
- Scratch downloads live in `/tmp/ktr` (outside the repo, deletable); nothing was committed.
