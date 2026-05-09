# Architecture — account-gateway

> **TL;DR — no DB / no events / pass-through only.**
>
> This service does NOT follow the standard KRE microservice template. It has no Postgres,
> no Flyway, no outbox, no Kafka/Redpanda topic, no kre-events lib. That is intentional.
> See `docs/HLD.md` for the rationale and the plan file for full context.

## What it is

A thin Kotlin/Spring Boot HTTP wrapper over Monarch Money's GraphQL API. Exposes 8 REST
endpoints. Stateless except for a file-cached session token. Same conceptual shape as
`local-llm-service` (which is a thin pass-through over llama.cpp) but in Kotlin.

## Flat 6-package layout

```
api/src/main/kotlin/com/klaenerealestate/accountgateway/
├── Application.kt                   # Spring entry point
├── MonarchBootstrapMain.kt          # Interactive CLI — NOT called by the app server
│
├── controllers/
│   └── MonarchController.kt         # @RestController — the 8 pass-through endpoints
│
├── services/
│   ├── MonarchClient.kt             # RestClient wrapper; GraphQL POST; SSRF allowlist
│   ├── MonarchSessionService.kt     # Loads/saves JSON session file from disk
│   ├── MonarchAuth.kt               # login + MFA + TOTP (not called automatically)
│   ├── MonarchQueries.kt            # 7 GraphQL operation const vals
│   ├── MonarchFragments.kt          # 5 GraphQL fragment const vals
│   └── Totp.kt                      # RFC 6238 TOTP (HmacSHA1, no Bouncy Castle)
│
├── models/
│   ├── MonarchSession.kt            # Data class (token, email, lastVerifiedAt)
│   ├── HealthResponse.kt
│   ├── AuthStatusResponse.kt
│   └── ErrorResponse.kt             # Uniform error shape (code, message, details)
│
├── exceptions/
│   ├── MonarchSessionMissingException.kt   # → 503
│   ├── MonarchSessionExpiredException.kt   # → 401
│   ├── MonarchMfaRequiredException.kt      # → thrown during bootstrap only
│   ├── MonarchRateLimitedException.kt      # → 429 + Retry-After
│   └── MonarchRequestFailedException.kt    # → 502
│
└── config/
    ├── MonarchProperties.kt         # @ConfigurationProperties for monarch.*
    └── GlobalExceptionHandler.kt    # @RestControllerAdvice — maps exceptions to HTTP

# Note: MonarchClient builds its RestClient inline (per token); no separate @Bean.
```

No `persistence/` package — no JPA, no Flyway, no Postgres driver on the classpath.
No `kafka/` package — no Spring Kafka, no kre-events lib, no outbox relay.

## Why only 6 packages (not the standard 8)

The KRE template (`template-spring-next`) defines 8 top-level packages:
`controllers`, `services`, `models`, `exceptions`, `config`, `utils`,
`persistence` (JPA entities + repos), `kafka` (producers/consumers/outbox).

account-gateway drops `persistence` and `kafka` entirely. ArchUnit enforces
`noNestedSubpackages` over the 6 packages that remain.

## Key design constraints (enforced by ArchUnit)

- Controllers annotated `@RestController`, named `*Controller`.
- Models contain no Spring or JPA imports.
- All exceptions extend `RuntimeException`.
- No nested sub-packages under any of the 6 dirs.

## Session lifecycle

```
host filesystem: ~/.config/account-gateway/.mm-session.json
        │
        │  (written once by bootstrapMonarch CLI)
        ▼
MonarchSessionService.load()        ← called at startup + on POST /v1/auth/refresh
        │
        ▼  null → MonarchSessionMissingException → 503
MonarchClient  ─── Authorization: Token <t> ──►  api.monarch.com/graphql
```

The container never writes the session file. The host directory is bind-mounted
(read-only in production, read-write for bootstrap flow).

## SYSTEMS_ROADMAP reference

account-gateway appears in the kre-skills roadmap as a Phase 1.5 edge integration.
See `~/Desktop/kre-skills/docs/ARCHITECTURE.md` — and the plan file at
`~/.claude/plans/declarative-singing-yao.md` for V1 scope and deferred work.

## What's intentionally missing (do not add without a new plan)

| Capability | Why missing |
|---|---|
| Postgres / JPA | Monarch owns the data; no reason to duplicate it |
| Flyway migrations | No schema to migrate |
| Outbox / kre-events | No domain events to publish |
| Per-source supersession | Monarch handles dedup upstream |
| PII encryption at rest | Nothing persisted |
| Financial Command Center migration | Explicitly out of scope for V1 |
| MCP wrapper | Deferred — ticket `monarch-mcp-wrapper` |
| CLI wrapper | Deferred — ticket `monarch-cli-wrapper` |
