package database.kernel.storage;

import database.kernel.storage.index.BTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CatalogService tracks all tables, schemas, and indexes in the database.
 * Acts as the system metadata registry.
 *
 * Spring-managed singleton service.
 */
@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final BufferPool bufferPool;

    // tableName → HeapFile
    private final Map<String, HeapFile> tables;

    // tableName → Schema
    private final Map<String, Schema> schemas;

    // "tableName.columnName" → BTree
    private final Map<String, BTree<? extends Comparable<?>>> indexes;

    public CatalogService(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
        this.tables = new LinkedHashMap<>();
        this.schemas = new LinkedHashMap<>();
        this.indexes = new LinkedHashMap<>();
        log.info("CatalogService initialized");
    }

    /**
     * Create a new table with the given schema.
     */
    public HeapFile createTable(String name, Schema schema) {
        String lower = name.toLowerCase();
        if (tables.containsKey(lower)) {
            throw new RuntimeException("Table already exists: " + name);
        }
        HeapFile heapFile = new HeapFile(lower, schema, bufferPool);
        tables.put(lower, heapFile);
        schemas.put(lower, schema);
        log.info("Table '{}' created with schema: {}", lower, schema);
        return heapFile;
    }

    /**
     * Get a table by name.
     */
    public HeapFile getTable(String name) {
        HeapFile hf = tables.get(name.toLowerCase());
        if (hf == null) {
            throw new RuntimeException("Table not found: " + name);
        }
        return hf;
    }

    /**
     * Get a table's schema by name.
     */
    public Schema getSchema(String name) {
        Schema s = schemas.get(name.toLowerCase());
        if (s == null) {
            throw new RuntimeException("Schema not found for table: " + name);
        }
        return s;
    }

    /**
     * Check if a table exists.
     */
    public boolean tableExists(String name) {
        return tables.containsKey(name.toLowerCase());
    }

    /**
     * Drop a table and its associated indexes.
     */
    public void dropTable(String name) {
        String lower = name.toLowerCase();
        if (!tables.containsKey(lower)) {
            throw new RuntimeException("Table not found: " + name);
        }
        tables.remove(lower);
        schemas.remove(lower);

        // Remove all indexes for this table
        indexes.entrySet().removeIf(e -> e.getKey().startsWith(lower + "."));
        log.info("Table '{}' dropped", lower);
    }

    /**
     * Register an index.
     */
    public void registerIndex(String tableName, String columnName, BTree<? extends Comparable<?>> tree) {
        String key = indexKey(tableName, columnName);
        indexes.put(key, tree);
        log.info("Index registered: {}", key);
    }

    /**
     * Get an index for a specific table and column.
     *
     * @return the BTree, or null if no index exists
     */
    @SuppressWarnings("unchecked")
    public <K extends Comparable<K>> BTree<K> getIndex(String tableName, String columnName) {
        return (BTree<K>) indexes.get(indexKey(tableName, columnName));
    }

    /**
     * Check if an index exists on a table/column.
     */
    public boolean hasIndex(String tableName, String columnName) {
        return indexes.containsKey(indexKey(tableName, columnName));
    }

    /**
     * Get all table names.
     */
    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    /**
     * Get all index keys ("tableName.columnName").
     */
    public Set<String> getIndexKeys() {
        return Collections.unmodifiableSet(indexes.keySet());
    }

    private String indexKey(String tableName, String columnName) {
        return tableName.toLowerCase() + "." + columnName.toLowerCase();
    }
}
