---
created: 2026-10-31T23:10:00.000Z
title: Documented POST /api/logout is 404 and only /api/api/logout works
area: auth
severity: minor
files:
  - src/main/java/com/vrudenko/kanban_board/security/SecurityConfiguration.java:143
  - docs/AUTH_FLOWS.md:155
  - src/main/resources/application.properties:3
---

## Problem

Found by the second talos blind run (2026-10-31) and reproduced by all three review lanes: the documented
`POST /api/logout` returns 404 and only `POST /api/api/logout` logs out. `SecurityConfiguration` line 143
sets `logout.logoutUrl(CONTEXT_PATH + ApiPaths.LOGOUT)`, and `application.properties` line 3 already sets
`server.servlet.context-path=/api`. The Spring Security filter matches the path inside the servlet
context, so prefixing the context path again gives `/api/api/logout`. This is consistent with the
reported behavior; I re-read the code on 2026-10-31 and did not re-run the request today.
`docs/AUTH_FLOWS.md` (line 155 mentions `POST /api/logout`) documents the unprefixed form. Severity is
minor because a working URL exists.

## Solution

TBD. Set `logoutUrl(ApiPaths.LOGOUT)` without the context path, and keep the docs as they are. Pin it with
a test that POSTs `/api/logout` with a session cookie and expects 200 and the cookie cleared; check other
`CONTEXT_PATH +` uses in the security config for the same doubling.
