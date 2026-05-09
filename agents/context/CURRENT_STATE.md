---
last_human_verified: 2026-05-09
freshness_horizon: 14d
---

# Current State — account-gateway

## Done

- **V1 implementation complete.** Stages 1–5 land in `feat/v1-pass-through-wrapper`.
  - Stage 1: repo scaffold (Kotlin/Spring Boot, minimal deps, no JPA/Kafka).
  - Stage 2: Monarch GraphQL client, queries, fragments, auth, session service.
  - Stage 3: HTTP pass-through controller + ExceptionHandler.
  - Stage 4: Tests (MockMvc + WireMock) + ArchUnit conventions.
  - Stage 5: Dockerfile, docker-compose, HLD, ARCHITECTURE, tickets. *(this commit)*
- **Auth bootstrap deferred.** `./gradlew :api:bootstrapMonarch` compiled but not run.
  Service returns 503 from data endpoints until session JSON is present.
  See ticket `monarch-bootstrap-auth`.

## How to run (local, no Docker)

```bash
cd ~/Desktop/account-gateway
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :api:bootRun
# → listening on http://localhost:8084
curl http://localhost:8084/healthz
# → {"status":"UP","authenticated":false,...}  (expected until bootstrap)
```

## How to run (Docker)

```bash
docker compose up -d
curl http://localhost:8084/healthz
```

## Port

**8084** (host and container). Defined in `application.yaml` (`API_PORT` env override available).

## Auth bootstrap (one-time, interactive)

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :api:bootstrapMonarch
# prompts for email / password / MFA code or TOTP secret
# writes ~/.config/account-gateway/.mm-session.json
```

After writing the session, either restart the service or:

```bash
curl -X POST http://localhost:8084/v1/auth/refresh
```

## Endpoints

| Path | Notes |
|---|---|
| `GET /healthz` | 200 always. `authenticated: bool` reflects session presence. |
| `GET /v1/accounts` | All linked accounts + balances. 503 if no session. |
| `GET /v1/account/{id}/history` | Daily balance history. |
| `GET /v1/transactions` | Params: `start`, `end`, `limit`, `offset`, `accountId`. |
| `GET /v1/cashflow` | Params: `start`, `end`. |
| `GET /v1/categories` | All transaction categories. |
| `GET /v1/tags` | All tags + colors + usage counts. |
| `POST /v1/refresh` | Pokes Monarch to re-poll Plaid. Returns 202. |
| `GET /v1/auth/status` | `{authenticated, email, lastVerifiedAt}`. |
| `POST /v1/auth/refresh` | Reloads session from disk (post-bootstrap). |

## Next agent: what to do

1. Open ticket `monarch-bootstrap-auth` — run the interactive bootstrap, verify `curl /v1/accounts` returns real Monarch data.
2. After bootstrap: open `monarch-auth-payload-verify` — reconcile `// VERIFY-AT-BOOTSTRAP` comments in `MonarchAuth.kt`.
3. If tests fail in CI: check WireMock stubs in `MonarchControllerTest` and `MonarchAuthTest` — they contain `// VERIFY-AT-BOOTSTRAP` markers where payload shapes were assumed.
