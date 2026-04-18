package database.kernel.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;

/**
 * Cache configuration for the kernel.
 *
 * Uses Caffeine (in-memory) as the primary cache.
 * Ready for Redis swap via Spring Cache abstraction when needed.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${minipostgres.cache.max-size:10000}")
    private int maxSize;

    @Value("${minipostgres.cache.ttl-seconds:5}")
    private int ttlSeconds;

    /**
     * Spring CacheManager backed by Caffeine.
     * Can be swapped to RedisCacheManager via @Profile("redis") later.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("queryResults", "tableMetadata");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .recordStats());
        log.info("Caffeine CacheManager configured: maxSize={}, ttl={}s", maxSize, ttlSeconds);
        return cacheManager;
    }

    /**
     * Direct Caffeine cache for QueryCacheService (not via Spring Cache abstraction).
     * This gives us more fine-grained control (invalidation by table prefix).
     */
    @Bean
    public Cache<String, Object> queryResultCache() {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .recordStats()
                .build();
    }
}
