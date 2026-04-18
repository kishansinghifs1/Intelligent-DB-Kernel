import { Hono } from 'hono'
import { cors } from 'hono/cors'
import type { Context, MiddlewareHandler } from 'hono'

type GatewayBindings = {
  USER_SERVICE_URL?: string
  KERNEL_SERVICE_URL?: string
  OPTIMIZER_SERVICE_URL?: string
  GATEWAY_REQUEST_TIMEOUT_MS?: string
  GATEWAY_RATE_LIMIT_WINDOW_MS?: string
  GATEWAY_RATE_LIMIT_MAX?: string
  GATEWAY_RATE_LIMIT_AUTH_MAX?: string
  GATEWAY_RATE_LIMIT_USER_MAX?: string
  GATEWAY_RATE_LIMIT_DB_MAX?: string
  GATEWAY_RATE_LIMIT_OPTIMIZER_MAX?: string
  ALLOWED_ORIGINS?: string
}

type GatewayVariables = {
  requestId: string
}

type GatewayEnv = {
  Bindings: GatewayBindings
  Variables: GatewayVariables
}

type ServiceName = 'user-service' | 'kernel' | 'optimizer'

type ProxyRouteConfig = {
  serviceName: ServiceName
  stripPrefix: string
  targetPrefix: string
}

type RateLimitRule = {
  scope: string
  max: number
  windowMs: number
}

type RateLimitState = {
  count: number
  resetAt: number
}

type ProbeResult = {
  ok: boolean
  status: number
  latencyMs: number
  url: string
  error?: string
}

const DEFAULT_USER_SERVICE_URL = 'http://127.0.0.1:3001'
const DEFAULT_KERNEL_SERVICE_URL = 'http://127.0.0.1:8080'
const DEFAULT_OPTIMIZER_SERVICE_URL = 'http://127.0.0.1:8000'

const DEFAULT_REQUEST_TIMEOUT_MS = 15_000
const DEFAULT_RATE_LIMIT_WINDOW_MS = 60_000
const DEFAULT_RATE_LIMIT_MAX = 120
const DEFAULT_AUTH_RATE_LIMIT_MAX = 20
const DEFAULT_USER_RATE_LIMIT_MAX = 100
const DEFAULT_DB_RATE_LIMIT_MAX = 80
const DEFAULT_OPTIMIZER_RATE_LIMIT_MAX = 60

const rateLimitStore = new Map<string, RateLimitState>()
const gatewayStartedAt = Date.now()

const app = new Hono<GatewayEnv>()

function parsePositiveInteger(rawValue: string | undefined, fallback: number): number {
  const parsed = Number.parseInt(rawValue ?? '', 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
}

function normalizeBaseUrl(url: string): string {
  return url.replace(/\/+$/, '')
}

function getServiceBaseUrl(serviceName: ServiceName, env: GatewayBindings): string {
  switch (serviceName) {
    case 'user-service':
      return normalizeBaseUrl(env.USER_SERVICE_URL ?? DEFAULT_USER_SERVICE_URL)
    case 'kernel':
      return normalizeBaseUrl(env.KERNEL_SERVICE_URL ?? DEFAULT_KERNEL_SERVICE_URL)
    case 'optimizer':
      return normalizeBaseUrl(env.OPTIMIZER_SERVICE_URL ?? DEFAULT_OPTIMIZER_SERVICE_URL)
  }
}

function parseAllowedOrigins(rawOrigins: string | undefined): string[] {
  if (!rawOrigins) {
    return ['*']
  }

  const parsed = rawOrigins
    .split(',')
    .map((origin) => origin.trim())
    .filter((origin) => origin.length > 0)

  return parsed.length > 0 ? parsed : ['*']
}

function selectCorsOrigin(origin: string, allowedOrigins: string[]): string | null {
  if (allowedOrigins.includes('*')) {
    return '*'
  }

  if (!origin) {
    return allowedOrigins[0] ?? null
  }

  return allowedOrigins.includes(origin) ? origin : null
}

function getClientIp(c: Context<GatewayEnv>): string {
  const cfIp = c.req.header('cf-connecting-ip')
  if (cfIp) {
    return cfIp
  }

  const forwardedFor = c.req.header('x-forwarded-for')
  if (forwardedFor) {
    const [first] = forwardedFor.split(',')
    if (first) {
      return first.trim()
    }
  }

  const realIp = c.req.header('x-real-ip')
  if (realIp) {
    return realIp
  }

  return 'unknown'
}

function joinPath(prefix: string, suffix: string): string {
  const normalizedPrefix = prefix === '/' ? '' : prefix.replace(/\/+$/, '')
  const normalizedSuffix = suffix.replace(/^\/+/, '')

  const combined = `${normalizedPrefix}/${normalizedSuffix}`.replace(/\/+/g, '/')
  const trimmed = combined.length > 1 ? combined.replace(/\/+$/, '') : combined

  return trimmed || '/'
}

function rewritePath(path: string, stripPrefix: string, targetPrefix: string): string {
  const suffix = path.startsWith(stripPrefix) ? path.slice(stripPrefix.length) : path
  return joinPath(targetPrefix, suffix)
}

function pruneRateLimitStore(now: number): void {
  if (rateLimitStore.size < 10_000) {
    return
  }

  for (const [key, value] of rateLimitStore) {
    if (value.resetAt <= now) {
      rateLimitStore.delete(key)
    }
  }
}

function getRateLimitRule(c: Context<GatewayEnv>): RateLimitRule {
  const windowMs = parsePositiveInteger(c.env.GATEWAY_RATE_LIMIT_WINDOW_MS, DEFAULT_RATE_LIMIT_WINDOW_MS)
  const globalMax = parsePositiveInteger(c.env.GATEWAY_RATE_LIMIT_MAX, DEFAULT_RATE_LIMIT_MAX)
  const authMax = parsePositiveInteger(c.env.GATEWAY_RATE_LIMIT_AUTH_MAX, DEFAULT_AUTH_RATE_LIMIT_MAX)
  const userMax = parsePositiveInteger(c.env.GATEWAY_RATE_LIMIT_USER_MAX, DEFAULT_USER_RATE_LIMIT_MAX)
  const dbMax = parsePositiveInteger(c.env.GATEWAY_RATE_LIMIT_DB_MAX, DEFAULT_DB_RATE_LIMIT_MAX)
  const optimizerMax = parsePositiveInteger(c.env.GATEWAY_RATE_LIMIT_OPTIMIZER_MAX, DEFAULT_OPTIMIZER_RATE_LIMIT_MAX)

  const path = c.req.path
  if (/^\/api\/(v1\/users\/(login|register|refresh)|users\/(login|register|refresh))$/.test(path)) {
    return { scope: 'auth', max: authMax, windowMs }
  }

  if (path.startsWith('/api/v1/users') || path.startsWith('/api/users')) {
    return { scope: 'user', max: userMax, windowMs }
  }

  if (
    path.startsWith('/api/db')
    || path.startsWith('/api/kernel')
    || /^\/api\/(execute|state|health|cache(\/.*)?)$/.test(path)
  ) {
    return { scope: 'kernel', max: dbMax, windowMs }
  }

  if (path.startsWith('/api/optimizer')) {
    return { scope: 'optimizer', max: optimizerMax, windowMs }
  }

  return { scope: 'global', max: globalMax, windowMs }
}

async function fetchWithTimeout(input: string | URL, init: RequestInit, timeoutMs: number): Promise<Response> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), timeoutMs)

  try {
    return await fetch(input, {
      ...init,
      signal: controller.signal,
    })
  } finally {
    clearTimeout(timeout)
  }
}

async function proxyToService(c: Context<GatewayEnv>, config: ProxyRouteConfig): Promise<Response> {
  const upstreamBaseUrl = getServiceBaseUrl(config.serviceName, c.env)
  const upstreamPath = rewritePath(c.req.path, config.stripPrefix, config.targetPrefix)

  const incomingUrl = new URL(c.req.url)
  const upstreamUrl = new URL(upstreamPath, upstreamBaseUrl)
  upstreamUrl.search = incomingUrl.search

  const headers = new Headers(c.req.raw.headers)
  headers.delete('host')
  headers.set('x-request-id', c.get('requestId'))
  headers.set('x-forwarded-for', getClientIp(c))
  headers.set('x-forwarded-host', incomingUrl.host)
  headers.set('x-forwarded-proto', incomingUrl.protocol.replace(':', ''))

  const timeoutMs = parsePositiveInteger(c.env.GATEWAY_REQUEST_TIMEOUT_MS, DEFAULT_REQUEST_TIMEOUT_MS)
  const method = c.req.method.toUpperCase()
  const hasBody = method !== 'GET' && method !== 'HEAD'

  let upstreamResponse: Response
  try {
    upstreamResponse = await fetchWithTimeout(
      upstreamUrl,
      {
        method,
        headers,
        body: hasBody ? c.req.raw.body : undefined,
        redirect: 'manual',
      },
      timeoutMs,
    )
  } catch (error) {
    const requestId = c.get('requestId')
    const isTimeout = error instanceof DOMException && error.name === 'AbortError'
    const status = isTimeout ? 504 : 502
    const message = isTimeout
      ? `Gateway timeout while calling ${config.serviceName}`
      : `Gateway failed to call ${config.serviceName}`

    return c.json(
      {
        ok: false,
        message,
        upstream: config.serviceName,
        requestId,
      },
      status,
    )
  }

  const responseHeaders = new Headers(upstreamResponse.headers)
  responseHeaders.set('x-gateway-upstream', config.serviceName)
  responseHeaders.set('x-request-id', c.get('requestId'))

  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    statusText: upstreamResponse.statusText,
    headers: responseHeaders,
  })
}

async function probeService(url: string, timeoutMs: number): Promise<ProbeResult> {
  const startedAt = Date.now()
  try {
    const response = await fetchWithTimeout(url, { method: 'GET' }, timeoutMs)
    return {
      ok: response.ok,
      status: response.status,
      latencyMs: Date.now() - startedAt,
      url,
    }
  } catch (error) {
    return {
      ok: false,
      status: 0,
      latencyMs: Date.now() - startedAt,
      url,
      error: error instanceof Error ? error.message : 'Unknown error',
    }
  }
}

const requestMetadataMiddleware: MiddlewareHandler<GatewayEnv> = async (c, next) => {
  const requestId = c.req.header('x-request-id') ?? crypto.randomUUID()
  c.set('requestId', requestId)

  const startedAt = Date.now()
  await next()

  c.header('x-request-id', requestId)
  c.header('x-gateway-service', 'db-engine-loadbalancer')
  c.header('x-gateway-latency-ms', String(Date.now() - startedAt))
  c.header('x-content-type-options', 'nosniff')
  c.header('x-frame-options', 'DENY')
  c.header('referrer-policy', 'no-referrer')
}

const rateLimitMiddleware: MiddlewareHandler<GatewayEnv> = async (c, next) => {
  if (c.req.method === 'OPTIONS') {
    await next()
    return
  }

  const now = Date.now()
  pruneRateLimitStore(now)

  const rule = getRateLimitRule(c)
  const key = `${rule.scope}:${getClientIp(c)}`
  const existing = rateLimitStore.get(key)

  let state: RateLimitState
  if (!existing || now >= existing.resetAt) {
    state = {
      count: 1,
      resetAt: now + rule.windowMs,
    }
    rateLimitStore.set(key, state)
  } else {
    state = {
      count: existing.count + 1,
      resetAt: existing.resetAt,
    }
    rateLimitStore.set(key, state)
  }

  const remaining = Math.max(0, rule.max - state.count)
  c.header('x-ratelimit-limit', String(rule.max))
  c.header('x-ratelimit-remaining', String(remaining))
  c.header('x-ratelimit-reset', String(Math.ceil(state.resetAt / 1000)))

  if (state.count > rule.max) {
    return c.json(
      {
        ok: false,
        message: 'Rate limit exceeded. Please retry later.',
        scope: rule.scope,
        requestId: c.get('requestId'),
      },
      429,
    )
  }

  await next()
}

app.use('*', requestMetadataMiddleware)
app.use('*', cors({
  origin: (origin, c) => {
    const allowedOrigins = parseAllowedOrigins(c.env.ALLOWED_ORIGINS)
    return selectCorsOrigin(origin, allowedOrigins)
  },
  allowMethods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'Authorization', 'X-Request-ID'],
  exposeHeaders: ['X-Request-ID', 'X-RateLimit-Limit', 'X-RateLimit-Remaining', 'X-RateLimit-Reset'],
  maxAge: 600,
}))
app.use('/api/*', rateLimitMiddleware)

app.get('/', (c) => {
  return c.json({
    service: 'db-engine-api-gateway',
    status: 'running',
    uptimeSeconds: Math.floor((Date.now() - gatewayStartedAt) / 1000),
    docs: {
      health: '/health',
      userApi: '/api/v1/users/*',
      userApiAlias: '/api/users/*',
      kernelApi: '/api/kernel/* or /api/db/*',
      kernelLegacy: '/api/execute, /api/state, /api/health, /api/cache/*',
      optimizerApi: '/api/optimizer/*',
    },
  })
})

app.get('/health', async (c) => {
  const timeoutMs = parsePositiveInteger(c.env.GATEWAY_REQUEST_TIMEOUT_MS, DEFAULT_REQUEST_TIMEOUT_MS)

  const userHealthUrl = `${getServiceBaseUrl('user-service', c.env)}/ping`
  const kernelHealthUrl = `${getServiceBaseUrl('kernel', c.env)}/api/health`
  const optimizerHealthUrl = `${getServiceBaseUrl('optimizer', c.env)}/health`

  const [userService, kernel, optimizer] = await Promise.all([
    probeService(userHealthUrl, timeoutMs),
    probeService(kernelHealthUrl, timeoutMs),
    probeService(optimizerHealthUrl, timeoutMs),
  ])

  const allHealthy = userService.ok && kernel.ok && optimizer.ok

  return c.json(
    {
      ok: allHealthy,
      service: 'db-engine-api-gateway',
      timestamp: new Date().toISOString(),
      requestId: c.get('requestId'),
      upstreams: {
        userService,
        kernel,
        optimizer,
      },
    },
    allHealthy ? 200 : 503,
  )
})

app.get('/api/gateway/health', (c) => c.redirect('/health', 307))

const userServiceRoute: ProxyRouteConfig = {
  serviceName: 'user-service',
  stripPrefix: '/api/v1/users',
  targetPrefix: '/api/v1/users',
}

const userServiceAliasRoute: ProxyRouteConfig = {
  serviceName: 'user-service',
  stripPrefix: '/api/users',
  targetPrefix: '/api/v1/users',
}

const kernelRoute: ProxyRouteConfig = {
  serviceName: 'kernel',
  stripPrefix: '/api/kernel',
  targetPrefix: '/api',
}

const kernelAliasRoute: ProxyRouteConfig = {
  serviceName: 'kernel',
  stripPrefix: '/api/db',
  targetPrefix: '/api',
}

const kernelLegacyRoute: ProxyRouteConfig = {
  serviceName: 'kernel',
  stripPrefix: '/api',
  targetPrefix: '/api',
}

const optimizerRoute: ProxyRouteConfig = {
  serviceName: 'optimizer',
  stripPrefix: '/api/optimizer',
  targetPrefix: '/',
}

app.all('/api/v1/users', (c) => proxyToService(c, userServiceRoute))
app.all('/api/v1/users/*', (c) => proxyToService(c, userServiceRoute))
app.all('/api/users', (c) => proxyToService(c, userServiceAliasRoute))
app.all('/api/users/*', (c) => proxyToService(c, userServiceAliasRoute))

app.all('/api/kernel', (c) => proxyToService(c, kernelRoute))
app.all('/api/kernel/*', (c) => proxyToService(c, kernelRoute))
app.all('/api/db', (c) => proxyToService(c, kernelAliasRoute))
app.all('/api/db/*', (c) => proxyToService(c, kernelAliasRoute))

for (const path of ['/api/execute', '/api/state', '/api/health', '/api/cache/stats', '/api/cache/clear']) {
  app.all(path, (c) => proxyToService(c, kernelLegacyRoute))
}

app.all('/api/optimizer', (c) => proxyToService(c, optimizerRoute))
app.all('/api/optimizer/*', (c) => proxyToService(c, optimizerRoute))

app.notFound((c) => {
  return c.json(
    {
      ok: false,
      message: 'Route not found in API gateway',
      requestId: c.get('requestId'),
    },
    404,
  )
})

app.onError((error, c) => {
  console.error('Unhandled gateway error:', error)
  return c.json(
    {
      ok: false,
      message: 'Internal gateway error',
      requestId: c.get('requestId'),
    },
    500,
  )
})

export default app
