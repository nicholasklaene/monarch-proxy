# monarch CLI

Thin bash wrapper over the monarch-proxy HTTP service. Calls `curl` against
`http://localhost:9084` (override with `MONARCH_GATEWAY`) and formats responses
with `jq`. Same shape as `~/bin/{gws,gh,git,gcloud}`.

## Install

```bash
ln -sf ~/Desktop/monarch-proxy/cli/monarch ~/bin/monarch
monarch health
```

## Usage

```bash
monarch                          # show usage
monarch health                   # service + auth status
monarch accounts                 # linked accounts table
monarch networth --start 2026-01-01
monarch transactions --start 2026-05-01 --limit 5
monarch holdings <account-id>
```

All commands accept `--raw` to bypass jq formatting (raw JSON for piping).

## Environment

- `MONARCH_GATEWAY` (default `http://localhost:9084`) — point at the service.

## Requirements

`curl`, `jq` (both standard on macOS / brew).
