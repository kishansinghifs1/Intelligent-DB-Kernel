const { createClient } = require('redis');
const logger = require('./logger.config');
const {
    REDIS_ENABLED,
    REDIS_URL,
    REDIS_HOST,
    REDIS_PORT,
    REDIS_PASSWORD,
    REDIS_DB,
    REDIS_KEY_PREFIX
} = require('./server.config');

class RedisConnection {
    constructor() {
        if (RedisConnection.instance) {
            return RedisConnection.instance;
        }

        this.client = null;
        this.isConnected = false;
        RedisConnection.instance = this;
    }

    buildClientOptions() {
        if (REDIS_URL) {
            return { url: REDIS_URL };
        }

        return {
            socket: {
                host: REDIS_HOST,
                port: REDIS_PORT
            },
            password: REDIS_PASSWORD || undefined,
            database: REDIS_DB
        };
    }

    buildKey(key) {
        return REDIS_KEY_PREFIX ? `${REDIS_KEY_PREFIX}:${key}` : key;
    }

    isReady() {
        return Boolean(this.client && this.client.isReady && this.isConnected);
    }

    async connect() {
        if (!REDIS_ENABLED) {
            logger.info('Redis is disabled (REDIS_ENABLED=false). Falling back to in-memory behavior.');
            return;
        }

        if (this.client && this.client.isOpen) {
            return;
        }

        this.client = createClient(this.buildClientOptions());

        this.client.on('ready', () => {
            this.isConnected = true;
            logger.info('Redis connection is ready');
        });

        this.client.on('end', () => {
            this.isConnected = false;
            logger.warn('Redis connection closed');
        });

        this.client.on('error', (error) => {
            this.isConnected = false;
            logger.error(`Redis connection error: ${error.message}`);
        });

        try {
            await this.client.connect();
            this.isConnected = true;
            logger.info('Redis connected successfully');
        } catch (error) {
            this.isConnected = false;
            logger.warn(`Redis unavailable. Falling back to in-memory mode. Reason: ${error.message}`);
        }
    }

    async disconnect() {
        if (!this.client || !this.client.isOpen) {
            return;
        }

        try {
            await this.client.quit();
        } catch (error) {
            logger.warn(`Redis graceful shutdown failed: ${error.message}`);
        } finally {
            this.isConnected = false;
        }
    }

    async getJson(key) {
        if (!this.isReady()) {
            return null;
        }

        const value = await this.client.get(this.buildKey(key));
        if (!value) {
            return null;
        }

        try {
            return JSON.parse(value);
        } catch (error) {
            logger.warn(`Redis JSON parse failed for key ${key}: ${error.message}`);
            return null;
        }
    }

    async setJson(key, data, ttlSeconds) {
        if (!this.isReady()) {
            return false;
        }

        const payload = JSON.stringify(data);
        const redisKey = this.buildKey(key);

        if (Number.isFinite(ttlSeconds) && ttlSeconds > 0) {
            await this.client.set(redisKey, payload, { EX: ttlSeconds });
            return true;
        }

        await this.client.set(redisKey, payload);
        return true;
    }

    async deleteKey(key) {
        if (!this.isReady()) {
            return false;
        }

        await this.client.del(this.buildKey(key));
        return true;
    }

    async incrementWithWindow(key, windowMs) {
        if (!this.isReady()) {
            return null;
        }

        const redisKey = this.buildKey(key);
        const currentCount = await this.client.incr(redisKey);

        if (currentCount === 1) {
            await this.client.pExpire(redisKey, windowMs);
        }

        let ttlMs = await this.client.pTTL(redisKey);
        if (ttlMs < 0) {
            await this.client.pExpire(redisKey, windowMs);
            ttlMs = windowMs;
        }

        return {
            currentCount,
            ttlMs
        };
    }
}

const redisConnection = new RedisConnection();
module.exports = redisConnection;
