---
last_human_verified: 2026-05-25
freshness_horizon: 14d
---

# Current State — monarch-proxy

## Done

- **V1 implementation complete.** Squash-merged to `main` as commit `eb55f78` on 2026-05-09.
  - Stage 1: repo scaffold (Kotlin/Spring Boot, minimal deps, no JPA/Kafka).
  - Stage 2: Monarch GraphQL client, queries, fragments, auth, session service.
  - Stage 3: HTTP pass-through controller + ExceptionHandler.
  - Stage 4: Tests (MockMvc + WireMock) + ArchUnit conventions.
  - Stage 5: Dockerfile, docker-compose, HLD, ARCHITECTURE, tickets.
- **2026-05-17 — moved from `klaene-real-estate/account-gateway` to `nicholasklaene/monarch-proxy`.**
  Package renamed `com.klaenerealestate.accountgateway` → `com.nicholasklaene.monarchproxy`.
  Port `8084` → `9084`. Session path `~/.config/account-gateway/` → `~/.config/monarch-proxy/`.
  See `~/.claude/plans/where-did-i-get-merry-harp.md`.
- **2026-05-17 — KRE-side `account-gateway` rebuilt as the consumer.**
  `klaene-real-estate/account-gateway` was rebuilt from scratch as a generic provider-agnostic
  service (port 8084) that calls THIS service via HTTP. It applies scope filtering so KRE
  business consumers see only authorized accounts. The mesh:
  ```
  KRE account-gateway (:8084)  ─HTTP─▶  monarch-proxy (:9084, this repo)  ─GraphQL─▶  api.monarch.com
  ```
  See PRs:
  - [klaene-real-estate/account-gateway#2](https://github.com/klaene-real-estate/account-gateway/pull/2)
  - [klaene-real-estate/kre-stack#3](https://github.com/klaene-real-estate/kre-stack/pull/3)
- **Auth bootstrap deferred.** `./gradlew :api:bootstrapMonarch` compiled but not run.
  Service returns 503 from data endpoints until session JSON is present.
  See ticket `monarch-bootstrap-auth`.

## How to run (local, no Docker)

```bash
cd ~/Desktop/monarch-proxy
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :api:bootRun
# → listening on http://localhost:9084
curl http://localhost:9084/healthz
# → {"status":"UP","authenticated":false,...}  (expected until bootstrap)
```

## How to run (Docker)

```bash
docker compose up -d
curl http://localhost:9084/healthz
```

## Port

**9084** (host and container). Defined in `application.yaml` (`API_PORT` env override available).

## Auth bootstrap (one-time, interactive)

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :api:bootstrapMonarch
# prompts for email / password / MFA code or TOTP secret
# writes ~/.config/monarch-proxy/.mm-session.json
```

After writing the session, either restart the service or:

```bash
curl -X POST http://localhost:9084/v1/auth/refresh
```

## Endpoints

| Path | Notes |
|---|---|
| `GET /healthz` | 200 always. `authenticated: bool` reflects session presence. |
| `GET /v1/accounts` | All linked accounts + balances. 503 if no session. |
| `GET /v1/account/{id}/history` | Per-account daily balance snapshots. |
| `GET /v1/account/{id}/holdings` | Per-account investment positions (Web_GetHoldings). |
| `GET /v1/transactions` | Params: `start`, `end`, `limit`, `offset`, `accountId`. |
| `GET /v1/cashflow` | Params: `start`, `end`. |
| `GET /v1/networth` | Aggregate net-worth-over-time. Params: `start`, `end`, `accountType`. |
| `GET /v1/networth/recent` | Per-account recent balance fluctuations. Params: `start`. |
| `GET /v1/networth/by-type` | Balance history grouped by account type. Params: `start`, `timeframe`. |
| `GET /v1/categories` | All transaction categories. |
| `GET /v1/tags` | All tags + colors + usage counts. |
| `POST /v1/refresh` | Pokes Monarch to re-poll Plaid. Returns 202. |
| `GET /v1/auth/status` | `{authenticated, email, lastVerifiedAt}`. |
| `POST /v1/auth/refresh` | Reloads session from disk (post-bootstrap). |

## Consumer

The primary consumer is `klaene-real-estate/account-gateway` (port 8084). It calls this
service over HTTP (default `http://localhost:9084`; in docker via `host.docker.internal:9084`).
A future MCP wrapper / CLI wrapper could also consume directly.

## 2026-05-25 — networth + holdings endpoints + monarch CLI

Added 4 new endpoints from upstream `monarchmoney`: `aggregateSnapshots` (net worth over
time), `recentAccountBalances`, `snapshotsByAccountType`, and `Web_GetHoldings`. All four
verified against live Monarch (115 daily snapshots from 2026-01-31, 30 accounts, 11 account
types, 23 holdings on tested brokerage). Pure pass-through; no DTO translation.

Also shipped `cli/monarch` — bash + curl + jq wrapper, symlinked to `~/bin/monarch`.
Subcommands cover all 13 endpoints. `--raw` flag for piping. Bash 3.2 compatible.

## Next agent: what to do

1. Open ticket `monarch-bootstrap-auth` — run the interactive bootstrap, verify `curl /v1/accounts` returns real Monarch data.
2. After bootstrap: open `monarch-auth-payload-verify` — reconcile `// VERIFY-AT-BOOTSTRAP` comments in `MonarchAuth.kt`.
3. Verify account-gateway end-to-end: start account-gateway on :8084 and confirm `curl http://localhost:8084/v1/accounts` returns canonical DTOs sourced from this service.
4. If tests fail in CI: check WireMock stubs in `MonarchControllerTest` and `MonarchAuthTest` — they contain `// VERIFY-AT-BOOTSTRAP` markers where payload shapes were assumed.
