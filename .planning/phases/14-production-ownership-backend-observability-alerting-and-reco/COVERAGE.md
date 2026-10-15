# API Coverage — Phase 14 external services

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Services: Telegram Bot API (through Alertmanager, Flux notification-controller and the uptime workflow),
> healthchecks.io (ping and read-only management API), Backblaze B2 S3-compatible API, GitHub REST API.
> Capability names carry the service prefix so they stay unique across the file.

| capability | decision | reason |
|---|---|---|
| telegram: sendMessage to the prod chat | INTEGRATE | |
| telegram: sendMessage to the muted nonprod chat with disable_notification | INTEGRATE | |
| telegram: bot token read from a mounted file (bot_token_file) | INTEGRATE | |
| telegram: chat id read from a mounted file (chat_id_file) | INTEGRATE | |
| telegram: getUpdates to discover chat ids | INTEGRATE | |
| telegram: parse_mode HTML or MarkdownV2 formatting | OPT-OUT | not needed: plain text avoids the HTML oversize replacement past 4096 characters (D-06) |
| telegram: message_thread_id forum topics | OPT-OUT | not needed: two plain group chats carry all paging |
| telegram: sendPhoto, sendDocument and other media | OPT-OUT | not needed: paging only |
| telegram: inline keyboards, callback queries, bot commands | OPT-OUT | not needed: the bot only sends, it never takes input |
| telegram: setWebhook and inbound updates | OPT-OUT | not needed: no inbound traffic to the cluster |
| telegram: editMessageText and deleteMessage | OPT-OUT | not needed: alerts are append-only notifications |
| telegram: payments, games, stickers, inline mode | OPT-OUT | explicitly out of scope: unrelated to alerting |
| healthchecks: ping success URL (POST from Alertmanager webhook) | INTEGRATE | |
| healthchecks: ping success URL from the uptime workflow heartbeat | INTEGRATE | |
| healthchecks: Telegram integration on both checks | INTEGRATE | |
| healthchecks: read-only management API (check status and flips) for measurement | INTEGRATE | |
| healthchecks: /fail and /start signals | OPT-OUT | not needed: the uptime workflow pages Telegram directly on failure, and run-time tracking is not measured |
| healthchecks: ping body logging | OPT-OUT | not needed, and no data should leave the cluster in ping bodies (GDPR note in free-tier research) |
| healthchecks: write management API (create or pause checks) | OPT-OUT | not needed: two checks created once by the operator in the UI |
| healthchecks: SMS, WhatsApp, phone call integrations | OPT-OUT | paid plans only; Telegram is the paging channel |
| healthchecks: badges and status pages | OPT-OUT | not needed yet: public status pages belong with Phase 14.1 SLO work |
| b2: PutObject of the encrypted dump (single request) | INTEGRATE | |
| b2: PutBucketLifecycleConfiguration (once, admin key, operator) | INTEGRATE | |
| b2: GetBucketLifecycleConfiguration (operator verification) | INTEGRATE | |
| b2: bucket default Object Lock retention | INTEGRATE | |
| b2: Object Lock retention headers on PutObject for weekly copies | OPT-OUT | conditional on 14-02 Task 1: unused under option-a/c (bucket default covers it); option-b flips this to INTEGRATE in 14-02 Task 2, with writeFileRetentions on the key |
| b2: PutObjectRetention on an existing object | OPT-OUT | not needed under any 14-02 option: retention is set at upload, by the bucket default or the option-b PutObject headers |
| b2: GetObject, HeadObject, ListObjectsV2 with the VM key | OPT-OUT | the VM key is write-only by D-12; the restore drill's read path is Phase 14.1 |
| b2: DeleteObject with the VM key | OPT-OUT | forbidden by D-12; retention is enforced by lifecycle rules |
| b2: multipart upload | OPT-OUT | not needed: the dump is about 100 KB, one PutObject |
| b2: server-side encryption (SSE-B2, SSE-C) | OPT-OUT | not needed: dumps are encrypted client-side with age (D-14) |
| b2: presigned URLs, CORS, event notifications, legal hold | OPT-OUT | not needed: no browser or event consumer touches the bucket |
| github: PUT actions/workflows/uptime-check.yml/enable (best-effort keepalive) | INTEGRATE | |
| github: repository secrets read by the uptime workflow through env | INTEGRATE | |
| github: other Actions REST endpoints | OPT-OUT | not needed: the heartbeat check detects a disabled schedule independently |
