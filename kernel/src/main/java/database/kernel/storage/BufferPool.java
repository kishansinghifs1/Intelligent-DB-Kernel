package database.kernel.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.*;

/**
 * BufferPool caches pages in memory with LRU eviction.
 * All page access goes through the BufferPool — it sits between
 * the query engine and the DiskManager.
 *
 * Spring-managed component with configurable capacity.
 */
@Component
public class BufferPool {

    private static final Logger log = LoggerFactory.getLogger(BufferPool.class);

    private final int capacity;
    private final DiskManager diskManager;

    // LRU cache: access-order LinkedHashMap
    private final LinkedHashMap<PageKey, Page> cache;
    private final Set<PageKey> dirtyPages;

    /**
     * Composite key for cache: filename + pageId.
     */
    private record PageKey(String fileName, int pageId) {}

    public BufferPool(
            @Value("${minipostgres.buffer-pool-size:100}") int capacity,
            DiskManager diskManager) {
        this.capacity = capacity;
        this.diskManager = diskManager;
        this.dirtyPages = new HashSet<>();

        // access-order = true → LRU behavior
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<PageKey, Page> eldest) {
                // We handle eviction manually for dirty-page flushing
                return false;
            }
        };

        log.info("BufferPool initialized with capacity={} pages", capacity);
    }

    /**
     * Get a page from the buffer pool. If not cached, reads from disk.
     */
    public Page getPage(String fileName, int pageId) {
        PageKey key = new PageKey(fileName, pageId);

        Page page = cache.get(key);
        if (page != null) {
            return page; // cache hit
        }

        // Cache miss — read from disk
        if (cache.size() >= capacity) {
            evict();
        }

        page = diskManager.readPage(fileName, pageId);
        cache.put(key, page);
        return page;
    }

    /**
     * Mark a page as dirty (modified in memory, needs to be flushed).
     */
    public void markDirty(String fileName, int pageId) {
        dirtyPages.add(new PageKey(fileName, pageId));
    }

    /**
     * Flush a specific dirty page to disk.
     */
    public void flushPage(String fileName, int pageId) {
        PageKey key = new PageKey(fileName, pageId);
        Page page = cache.get(key);
        if (page != null && dirtyPages.contains(key)) {
            diskManager.writePage(fileName, pageId, page);
            dirtyPages.remove(key);
        }
    }

    /**
     * Flush all dirty pages to disk.
     */
    @PreDestroy
    public void flushAll() {
        log.info("Flushing {} dirty pages to disk...", dirtyPages.size());
        for (PageKey key : new ArrayList<>(dirtyPages)) {
            Page page = cache.get(key);
            if (page != null) {
                diskManager.writePage(key.fileName(), key.pageId(), page);
            }
        }
        dirtyPages.clear();
        log.info("Buffer pool flushed.");
    }

    /**
     * Allocate a new page on disk and bring it into the pool.
     *
     * @return the new page's ID
     */
    public int allocatePage(String fileName) {
        int newPageId = diskManager.allocatePage(fileName);

        PageKey key = new PageKey(fileName, newPageId);
        if (cache.size() >= capacity) {
            evict();
        }

        Page page = diskManager.readPage(fileName, newPageId);
        cache.put(key, page);
        return newPageId;
    }

    /**
     * Get the number of pages in a file.
     */
    public int getPageCount(String fileName) {
        return diskManager.getPageCount(fileName);
    }

    /**
     * Evict the least recently used page. If it's dirty, flush it first.
     */
    private void evict() {
        Iterator<Map.Entry<PageKey, Page>> it = cache.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<PageKey, Page> eldest = it.next();
            PageKey key = eldest.getKey();

            // Flush if dirty
            if (dirtyPages.contains(key)) {
                diskManager.writePage(key.fileName(), key.pageId(), eldest.getValue());
                dirtyPages.remove(key);
            }

            it.remove();
        }
    }

    /**
     * Get the current number of cached pages.
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Check if a page is in the cache.
     */
    public boolean isPageCached(String fileName, int pageId) {
        return cache.containsKey(new PageKey(fileName, pageId));
    }

    /**
     * Check if a page is dirty.
     */
    public boolean isPageDirty(String fileName, int pageId) {
        return dirtyPages.contains(new PageKey(fileName, pageId));
    }
}
