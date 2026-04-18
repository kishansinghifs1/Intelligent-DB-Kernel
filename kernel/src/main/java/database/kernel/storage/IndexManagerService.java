package database.kernel.storage;

import database.kernel.storage.index.BTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Iterator;

/**
 * IndexManagerService handles index lifecycle: creation, insertion on tuple insert,
 * removal on tuple delete, and index lookups.
 *
 * Spring-managed service.
 */
@Service
public class IndexManagerService {

    private static final Logger log = LoggerFactory.getLogger(IndexManagerService.class);

    private final CatalogService catalog;

    public IndexManagerService(CatalogService catalog) {
        this.catalog = catalog;
        log.info("IndexManagerService initialized");
    }

    /**
     * Create a new B-Tree index on a column.
     * Scans all existing tuples to populate the index.
     */
    @SuppressWarnings("unchecked")
    public void createIndex(String tableName, String columnName) {
        HeapFile heapFile = catalog.getTable(tableName);
        Schema schema = heapFile.getSchema();
        int colIdx = schema.getColumnIndex(columnName);

        if (colIdx == -1) {
            throw new RuntimeException("Column not found: " + columnName + " in table " + tableName);
        }

        Schema.Column col = schema.getColumn(colIdx);

        // Create the appropriate B-Tree based on column type
        BTree<? extends Comparable<?>> tree = switch (col.getType()) {
            case INT -> createIntIndex(heapFile, colIdx);
            case FLOAT -> createFloatIndex(heapFile, colIdx);
            case VARCHAR -> createStringIndex(heapFile, colIdx);
        };

        catalog.registerIndex(tableName, columnName, tree);
        log.info("Index created on {}.{} ({} type)", tableName, columnName, col.getType());
    }

    private BTree<Integer> createIntIndex(HeapFile heapFile, int colIdx) {
        BTree<Integer> tree = new BTree<>(3);
        Iterator<HeapFile.TupleWithId> it = heapFile.iterator();
        while (it.hasNext()) {
            HeapFile.TupleWithId twid = it.next();
            Integer key = (Integer) twid.getTuple().getValue(colIdx);
            tree.insert(key, twid.getRecordId());
        }
        return tree;
    }

    private BTree<Float> createFloatIndex(HeapFile heapFile, int colIdx) {
        BTree<Float> tree = new BTree<>(3);
        Iterator<HeapFile.TupleWithId> it = heapFile.iterator();
        while (it.hasNext()) {
            HeapFile.TupleWithId twid = it.next();
            Float key = (Float) twid.getTuple().getValue(colIdx);
            tree.insert(key, twid.getRecordId());
        }
        return tree;
    }

    private BTree<String> createStringIndex(HeapFile heapFile, int colIdx) {
        BTree<String> tree = new BTree<>(3);
        Iterator<HeapFile.TupleWithId> it = heapFile.iterator();
        while (it.hasNext()) {
            HeapFile.TupleWithId twid = it.next();
            String key = (String) twid.getTuple().getValue(colIdx);
            tree.insert(key, twid.getRecordId());
        }
        return tree;
    }

    /**
     * Insert a key into the appropriate index when a tuple is inserted.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void insertIntoIndexes(String tableName, Tuple tuple, RecordId rid) {
        Schema schema = tuple.getSchema();
        for (int i = 0; i < schema.getColumnCount(); i++) {
            String colName = schema.getColumn(i).getName();
            if (catalog.hasIndex(tableName, colName)) {
                BTree tree = catalog.getIndex(tableName, colName);
                Comparable key = (Comparable) tuple.getValue(i);
                tree.insert(key, rid);
            }
        }
    }

    /**
     * Remove a key from the appropriate index when a tuple is deleted.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void removeFromIndexes(String tableName, Tuple tuple) {
        Schema schema = tuple.getSchema();
        for (int i = 0; i < schema.getColumnCount(); i++) {
            String colName = schema.getColumn(i).getName();
            if (catalog.hasIndex(tableName, colName)) {
                BTree tree = catalog.getIndex(tableName, colName);
                Comparable key = (Comparable) tuple.getValue(i);
                tree.delete(key);
            }
        }
    }

    /**
     * Get an index for a specific column, or null if none exists.
     */
    @SuppressWarnings("unchecked")
    public <K extends Comparable<K>> BTree<K> getIndex(String tableName, String columnName) {
        return catalog.getIndex(tableName, columnName);
    }

    /**
     * Check if an index exists.
     */
    public boolean hasIndex(String tableName, String columnName) {
        return catalog.hasIndex(tableName, columnName);
    }
}
