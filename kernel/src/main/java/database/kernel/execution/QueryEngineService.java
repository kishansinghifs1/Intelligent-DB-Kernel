package database.kernel.execution;

import database.kernel.advisor.ModelAdvisorService;
import database.kernel.cache.QueryCacheService;
import database.kernel.storage.CatalogService;
import database.kernel.storage.HeapFile;
import database.kernel.storage.IndexManagerService;
import database.kernel.storage.RecordId;
import database.kernel.storage.Schema;
import database.kernel.storage.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * QueryEngineService routes queries to the best execution strategy.
 * Uses RL-based ModelAdvisor (via gRPC) for intelligent scan selection,
 * with Caffeine cache for hot query results.
 *
 * Spring-managed service — the heart of query execution.
 */
@Service
public class QueryEngineService {

    private static final Logger log = LoggerFactory.getLogger(QueryEngineService.class);

    private final CatalogService catalog;
    private final IndexManagerService indexManager;
    private final SeqScanExecutor seqScan;
    private final IndexScanExecutor indexScan;
    private final ModelAdvisorService modelAdvisor;
    private final QueryCacheService queryCache;

    public QueryEngineService(CatalogService catalog,
                              IndexManagerService indexManager,
                              SeqScanExecutor seqScan,
                              IndexScanExecutor indexScan,
                              ModelAdvisorService modelAdvisor,
                              QueryCacheService queryCache) {
        this.catalog = catalog;
        this.indexManager = indexManager;
        this.seqScan = seqScan;
        this.indexScan = indexScan;
        this.modelAdvisor = modelAdvisor;
        this.queryCache = queryCache;
        log.info("QueryEngineService initialized");
    }

    /**
     * Execute a SELECT query with an optional predicate.
     * Automatically chooses between IndexScan and SeqScan using the RL advisor.
     */
    public QueryResult executeSelect(String tableName, Predicate predicate) {
        // Check cache first
        String cacheKey = buildCacheKey(tableName, predicate);
        QueryResult cached = queryCache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for query: {}", cacheKey);
            return new QueryResult(cached.tuples(), cached.strategy() + " [CACHED]", 0);
        }

        long startTime = System.nanoTime();
        List<Tuple> results;
        String strategy;
        int action = -1;
        ModelAdvisorService.QueryState rlState = null;

        if (predicate != null) {
            HeapFile table = catalog.getTable(tableName);
            rlState = new ModelAdvisorService.QueryState(
                    table.getPageCount(),
                    predicate.getOp() == Predicate.Op.BETWEEN,
                    catalog.hasIndex(tableName, predicate.getColumnName())
            );
            action = modelAdvisor.predict(rlState);
        }

        // Use RL action if available, otherwise fallback to heuristic
        boolean tryIndex = (action == 1 || (action == -1 && canUseIndex(tableName, predicate)))
                           && canUseIndex(tableName, predicate);

        if (tryIndex) {
            strategy = "IndexScan (RL)" + (action == -1 ? " [H]" : "");
            results = executeWithIndex(tableName, predicate);
            if (action == -1) action = 1;
        } else {
            strategy = "SeqScan (RL)" + (action == -1 ? " [H]" : "");
            results = seqScan.scan(tableName, predicate);
            if (action == -1) action = 0;
        }

        long elapsed = System.nanoTime() - startTime;

        // Report reward to RL agent (negative of elapsed time in ms)
        if (rlState != null) {
            modelAdvisor.reportReward(rlState, action, -1.0 * (elapsed / 1_000_000.0));
        }

        QueryResult result = new QueryResult(results, strategy, elapsed);

        // Cache the result (only for reads)
        queryCache.put(cacheKey, result);

        return result;
    }

    /**
     * Execute an INSERT into a table.
     */
    public RecordId executeInsert(String tableName, Tuple tuple) {
        HeapFile heapFile = catalog.getTable(tableName);
        RecordId rid = heapFile.insertTuple(tuple);

        // Update any indexes
        indexManager.insertIntoIndexes(tableName, tuple, rid);

        // Invalidate cache for this table
        queryCache.invalidateTable(tableName);

        return rid;
    }

    /**
     * Execute CREATE TABLE.
     */
    public void executeCreateTable(String name, Schema schema) {
        catalog.createTable(name, schema);
    }

    /**
     * Execute CREATE INDEX.
     */
    public void executeCreateIndex(String tableName, String columnName) {
        indexManager.createIndex(tableName, columnName);
        queryCache.invalidateTable(tableName);
    }

    /**
     * Execute DROP TABLE.
     */
    public void executeDropTable(String name) {
        catalog.dropTable(name);
        queryCache.invalidateTable(name);
    }

    /**
     * Check if we can use an index for the given predicate.
     */
    private boolean canUseIndex(String tableName, Predicate pred) {
        if (pred == null) return false;
        if (!catalog.hasIndex(tableName, pred.getColumnName())) {
            return false;
        }
        return pred.getOp() == Predicate.Op.EQUALS
                || pred.getOp() == Predicate.Op.BETWEEN;
    }

    /**
     * Execute a query using the index.
     */
    private List<Tuple> executeWithIndex(String tableName, Predicate pred) {
        String colName = pred.getColumnName();
        if (pred.getOp() == Predicate.Op.EQUALS) {
            return indexScan.equalitySearch(tableName, colName, pred.getValue());
        } else if (pred.getOp() == Predicate.Op.BETWEEN) {
            return indexScan.rangeSearch(tableName, colName,
                    pred.getValueLow(), pred.getValueHigh());
        }
        throw new RuntimeException("Unsupported index operation: " + pred.getOp());
    }

    /**
     * Build a cache key for a query.
     */
    private String buildCacheKey(String tableName, Predicate predicate) {
        if (predicate == null) {
            return tableName + ":*";
        }
        return tableName + ":" + predicate.toString();
    }
}
