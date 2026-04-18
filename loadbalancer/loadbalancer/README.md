# DB-Engine API Gateway (Hono + Cloudflare Workers)

This service is the reverse proxy and API gateway for the DB-Engine microservices.

It provides:

- Centralized reverse proxy routing to all backend services
- Gateway-level rate limiting (path-aware)
- Request tracing with request IDs
- CORS and baseline security headers
- Aggregated health checks for end-to-end readiness

## Service Topology

- User Service (Express): `http://127.0.0.1:3001`
- Kernel Service (Spring Boot): `http://127.0.0.1:8080`
- Optimizer Service (FastAPI): `http://127.0.0.1:8000`

All URLs are configurable through `wrangler.jsonc` vars.

## End-to-End Flow Boundaries

- Client HTTP traffic enters through this gateway.
- Authentication and account APIs are routed to User Service.
- SQL and database state APIs are routed to Kernel Service.
- Optimizer HTTP APIs are routed to Optimizer Service.
- Kernel Service uses gRPC internally to talk to Optimizer on port `50051`.
- Kernel Service also exposes gRPC on port `9090` for direct gRPC clients.

The gateway currently focuses on HTTP traffic. Internal gRPC links remain service-to-service to avoid unnecessary protocol translation overhead.

## Gateway Route Map

| Incoming Route | Upstream Service | Rewritten Upstream Path |
|---|---|---|
| `/api/v1/users/*` | User Service | `/api/v1/users/*` |
| `/api/users/*` | User Service | `/api/v1/users/*` |
| `/api/kernel/*` | Kernel Service | `/api/*` |
| `/api/db/*` | Kernel Service | `/api/*` |
| `/api/execute` | Kernel Service | `/api/execute` |
| `/api/state` | Kernel Service | `/api/state` |
| `/api/health` | Kernel Service | `/api/health` |
| `/api/cache/*` | Kernel Service | `/api/cache/*` |
| `/api/optimizer/*` | Optimizer Service | `/*` |
| `/health` | Gateway | Aggregated health status |

## Rate Limiting Strategy

The gateway uses in-memory counters with separate limits by path scope:

- `auth`: login/register/refresh endpoints
- `user`: user-service APIs
- `kernel`: DB query and state APIs
- `optimizer`: RL optimizer APIs
- `global`: fallback for remaining gateway routes

Each response includes:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`

Note: in-memory limiting is per gateway instance. For multi-instance production deployments, move counters to a shared store (for example, Redis or Cloudflare Durable Objects/KV).

## Request Tracing and Gateway Headers

- Incoming `X-Request-ID` is reused, or a new UUID is generated
- `X-Request-ID` is forwarded upstream
- `X-Gateway-Upstream` marks the target service
- `X-Gateway-Latency-Ms` reports edge-side latency

## Configuration

Set values in `wrangler.jsonc` under `vars`:

- `USER_SERVICE_URL`
- `KERNEL_SERVICE_URL`
- `OPTIMIZER_SERVICE_URL`
- `GATEWAY_REQUEST_TIMEOUT_MS`
- `GATEWAY_RATE_LIMIT_WINDOW_MS`
- `GATEWAY_RATE_LIMIT_MAX`
- `GATEWAY_RATE_LIMIT_AUTH_MAX`
- `GATEWAY_RATE_LIMIT_USER_MAX`
- `GATEWAY_RATE_LIMIT_DB_MAX`
- `GATEWAY_RATE_LIMIT_OPTIMIZER_MAX`
- `ALLOWED_ORIGINS`

## Local Development

```bash
npm install
npm run dev
```

## Type Generation and Type Checking

```bash
npm run cf-typegen
npm run typecheck
```

## Deployment

```bash
npm run deploy
```

## Sustainability Notes

- Keep backend service contracts stable and route rewrites minimal.
- Use `/health` as the single readiness signal for orchestration.
- Tune scope-based rate limits by observing real traffic patterns.
- For scale-out, replace local in-memory limiter with distributed counters.
