const dotenv = require('dotenv');

dotenv.config();

function toPositiveInteger(value, fallback) {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function toNonNegativeInteger(value, fallback) {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}

function toBoolean(value, fallback) {
    if (value === undefined || value === null || value === '') {
        return fallback;
    }

    const normalized = String(value).trim().toLowerCase();
    if (['true', '1', 'yes', 'on'].includes(normalized)) {
        return true;
    }

    if (['false', '0', 'no', 'off'].includes(normalized)) {
        return false;
    }

    return fallback;
}

module.exports = {
    PORT: process.env.PORT || 3001,
    ATLAS_DB_URL: process.env.ATLAS_DB_URL,
    NODE_ENV: process.env.NODE_ENV || 'development',
    JWT_SECRET: process.env.JWT_SECRET,
    JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '24h',
    JWT_REFRESH_SECRET: process.env.JWT_REFRESH_SECRET,
    JWT_REFRESH_EXPIRES_IN: process.env.JWT_REFRESH_EXPIRES_IN || '7d',
    REDIS_ENABLED: toBoolean(process.env.REDIS_ENABLED, true),
    REDIS_URL: process.env.REDIS_URL,
    REDIS_HOST: process.env.REDIS_HOST || '127.0.0.1',
    REDIS_PORT: toPositiveInteger(process.env.REDIS_PORT, 6379),
    REDIS_PASSWORD: process.env.REDIS_PASSWORD || '',
    REDIS_DB: toNonNegativeInteger(process.env.REDIS_DB, 0),
    REDIS_KEY_PREFIX: process.env.REDIS_KEY_PREFIX || 'user-service',
    USER_PROFILE_CACHE_TTL_SECONDS: toPositiveInteger(process.env.USER_PROFILE_CACHE_TTL_SECONDS, 120),
    AUTH_RATE_LIMIT_WINDOW_MS: toPositiveInteger(process.env.AUTH_RATE_LIMIT_WINDOW_MS, 15 * 60 * 1000),
    AUTH_RATE_LIMIT_MAX: toPositiveInteger(process.env.AUTH_RATE_LIMIT_MAX, 10),
    USER_API_RATE_LIMIT_WINDOW_MS: toPositiveInteger(process.env.USER_API_RATE_LIMIT_WINDOW_MS, 60 * 1000),
    USER_API_RATE_LIMIT_MAX: toPositiveInteger(process.env.USER_API_RATE_LIMIT_MAX, 120)
};
