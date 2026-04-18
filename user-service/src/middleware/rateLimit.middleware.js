const { StatusCodes } = require('http-status-codes');

const redisClient = require('../config/redis.config');

const logger = require('../config/logger.config');

function resolveClientIp(req) {
    const forwardedFor = req.headers['x-forwarded-for'];
    if (typeof forwardedFor === 'string' && forwardedFor.trim()) {
        return forwardedFor.split(',')[0].trim();
    }

    return req.ip || req.socket?.remoteAddress || 'unknown';
}

function setRateLimitHeaders(res, max, remaining, resetAtMs) {
    res.setHeader('X-RateLimit-Limit', max);
    res.setHeader('X-RateLimit-Remaining', remaining);
    res.setHeader('X-RateLimit-Reset', Math.ceil(resetAtMs / 1000));
}

function tooManyRequestsResponse(res, message, retryAfterSeconds) {
    if (retryAfterSeconds > 0) {
        res.setHeader('Retry-After', retryAfterSeconds);
    }

    return res.status(StatusCodes.TOO_MANY_REQUESTS).json({
        success: false,
        message,
        data: {}
    });
}

function applyInMemoryLimit({ req, res, next, store, windowMs, max, message, keyPrefix }) {
    const now = Date.now();
    const key = `${keyPrefix}:${resolveClientIp(req)}`;
    const existing = store.get(key);

    if (!existing || now > existing.resetAt) {
        const resetAt = now + windowMs;
        store.set(key, { count: 1, resetAt });
        setRateLimitHeaders(res, max, Math.max(0, max - 1), resetAt);
        return next();
    }

    existing.count += 1;
    store.set(key, existing);

    const remaining = Math.max(0, max - existing.count);
    setRateLimitHeaders(res, max, remaining, existing.resetAt);

    if (existing.count > max) {
        const retryAfter = Math.max(1, Math.ceil((existing.resetAt - now) / 1000));
        return tooManyRequestsResponse(res, message, retryAfter);
    }

    return next();
}

/**
 * Redis-backed rate limiter with in-memory fallback.
 */
function createRateLimiter({ windowMs, max, message, keyPrefix = 'global' }) {
    const store = new Map();
    let fallbackRequestCounter = 0;

    return async function rateLimiter(req, res, next) {
        if (redisClient.isReady()) {
            try {
                const ip = resolveClientIp(req);
                const key = `ratelimit:${keyPrefix}:${ip}`;
                const rateLimitState = await redisClient.incrementWithWindow(key, windowMs);

                if (rateLimitState) {
                    const now = Date.now();
                    const resetAt = now + Math.max(0, rateLimitState.ttlMs);
                    const remaining = Math.max(0, max - rateLimitState.currentCount);
                    setRateLimitHeaders(res, max, remaining, resetAt);

                    if (rateLimitState.currentCount > max) {
                        const retryAfter = Math.max(1, Math.ceil(Math.max(0, rateLimitState.ttlMs) / 1000));
                        return tooManyRequestsResponse(res, message, retryAfter);
                    }

                    return next();
                }
            } catch (error) {
                logger.warn(`Redis rate limiter fallback (${keyPrefix}): ${error.message}`);
            }
        }

        fallbackRequestCounter += 1;
        if (fallbackRequestCounter % 500 === 0 && store.size > 0) {
            const now = Date.now();
            for (const [key, state] of store.entries()) {
                if (now > state.resetAt) {
                    store.delete(key);
                }
            }
        }

        return applyInMemoryLimit({
            req,
            res,
            next,
            store,
            windowMs,
            max,
            message,
            keyPrefix
        });
    };
}

module.exports = {
    createRateLimiter
};
