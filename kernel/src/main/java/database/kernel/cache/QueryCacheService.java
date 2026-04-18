package database.kernel.cache;

import com.github.benmanes.caffeine.cache.Cache;
import database.kernel.execution.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentMap;

/**
 * QueryCacheService provides query result caching with:
 * - TTL-based eviction (configured via CacheConfig)
 * - Write-through invalidation (INSERT/DROP invalidate related cache entries)
 * - Hot-key protection (Caffeine handles frequency-based eviction)
 *
 * Read replicas pattern: serve repeated reads from cache before hitting storage.
 */
@Service
public class QueryCacheService {

    private static final Logger log = LoggerFactory.getLogger(QueryCacheService.class);

    private final Cache<String, Object> cache;

    public QueryCacheService(Cache<String, Object> queryResultCache) {
        this.cache = queryResultCache;
        log.info("QueryCacheService initialized");
    }

    /**
     * Get a cached query result.
     *
     * @param key the cache key (tableName:predicate)
     * @return cached QueryResult or null if not found
     */
    public QueryResult get(String key) {
        Object value = cache.getIfPresent(key);
        if (value instanceof QueryResult qr) {
            return qr;
        }
        return null;
    }

    /**
     * Put a query result in the cache.
     */
    public void put(String key, QueryResult result) {
        if (result != null && !result.tuples().isEmpty()) {
            cache.put(key, result);
        }
    }

    /**
     * Invalidate all cache entries for a specific table.
     * Called on INSERT, UPDATE, DELETE, DROP TABLE operations.
     */
    public void invalidateTable(String tableName) {
        String prefix = tableName.toLowerCase() + ":";
        ConcurrentMap<String, Object> map = cache.asMap();
        long removed = map.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .peek(map::remove)
                .count();
        if (removed > 0) {
            log.debug("Invalidated {} cache entries for table '{}'", removed, tableName);
        }
    }

    /**
     * Invalidate the entire cache.
     */
    public void invalidateAll() {
        cache.invalidateAll();
        log.info("All cache entries invalidated");
    }

    /**
     * Get cache statistics.
     */
    public String stats() {
        return cache.stats().toString();
    }

    /**
     * Get the current cache size.
     */
    public long size() {
        return cache.estimatedSize();
    }
}
