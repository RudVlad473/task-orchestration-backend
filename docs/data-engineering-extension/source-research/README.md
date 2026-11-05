# Source research

`sources.jsonl` logs every real-time data source considered for the data-engineering extension (see `../DESIGN.md`), one
JSON object per line, ordered by `volume.events_per_sec` descending with unmeasured sources last. A low-volume source
stays in the file so it is not researched twice; it is not discussed further.

Query it in place, for example `jq -c 'select(.verdict=="shortlist") | {id, e: .volume.events_per_sec}' sources.jsonl`.

## Record shape (entity `source`)

| Field | Values |
| --- | --- |
| `id` | kebab-case slug, unique |
| `name`, `publisher`, `url` | strings |
| `protocol` | `sse`, `ndjson-http`, `websocket`, `mqtt`, `rest-poll`, `batch-files`, `tcp` |
| `auth` | `none`, `free-key`, `account`, `unknown` |
| `access_verified` | `payload-read` (data arrived), `connected` (handshake ok, no data), `handshake-only`, `not-reached`, `not-tried` |
| `volume.events_per_sec` | number, or `null` when not measured |
| `volume.bytes_per_sec`, `window_s`, `measured_on`, `method`, `note` | what was measured and how |
| `volume_class` | `high` (100 per second or more), `medium` (5 to 100), `low` (under 5), `bursty-or-unmeasured` |
| `verdict` | `shortlist`, `log-only-low-volume`, `rejected`, `unverified` |
| `reason`, `project_fit` | one sentence each |

`auth` is `none` only when a connection or payload was obtained without credentials this session. `unknown` means it was not
established, not that it is open.

## How the volumes were taken

All on 2026-11-05 from one WSL machine, over one evening, in windows of 15 to 60 seconds (a USGS count over a day, a GDELT file
over its 15-minute slot). Each is a point sample, not a distribution: daily and weekly swings are not captured, and
polling-based sources have no push rate at all. Message counts can understate record counts where one message is an array
(see `binance-all-miniticker`).

Entries with `access_verified` of `not-tried`, and the claims in their `note`, come from a web search summary or a curated
GitHub list, not from a fetch. Treat them as leads.

## Crowded space

A web search for open-source geopolitical dashboards listed World Monitor, OSIRIS, SignalTrace and a world-intel MCP server
as existing aggregators over similar free sources. They were not opened, so what they actually cover is unverified.
