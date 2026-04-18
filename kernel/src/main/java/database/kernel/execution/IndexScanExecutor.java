package database.kernel.execution;

import database.kernel.storage.CatalogService;
import database.kernel.storage.HeapFile;
import database.kernel.storage.RecordId;
import database.kernel.storage.Tuple;
import database.kernel.storage.index.BTree;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * IndexScanExecutor uses a B-Tree index for efficient lookups.
 *
 * Supports equality lookups O(log n) and range scans O(log n + k).
 */
@Component
public class IndexScanExecutor {

    private final CatalogService catalog;

    public IndexScanExecutor(CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Equality lookup: find all tuples where column = value.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Tuple> equalitySearch(String tableName, String columnName, Object value) {
        BTree tree = catalog.getIndex(tableName, columnName);
        if (tree == null) {
            throw new RuntimeException("No index on " + tableName + "." + columnName);
        }

        Comparable key = (Comparable) value;
        List<RecordId> rids = tree.searchAll(key);

        HeapFile heapFile = catalog.getTable(tableName);
        List<Tuple> results = new ArrayList<>();
        for (RecordId rid : rids) {
            Tuple tuple = heapFile.getTuple(rid);
            if (tuple != null) {
                results.add(tuple);
            }
        }
        return results;
    }

    /**
     * Range lookup: find all tuples where low <= column <= high.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Tuple> rangeSearch(String tableName, String columnName,
                                   Object low, Object high) {
        BTree tree = catalog.getIndex(tableName, columnName);
        if (tree == null) {
            throw new RuntimeException("No index on " + tableName + "." + columnName);
        }

        Comparable lowKey = (Comparable) low;
        Comparable highKey = (Comparable) high;
        List<RecordId> rids = tree.rangeSearch(lowKey, highKey);

        HeapFile heapFile = catalog.getTable(tableName);
        List<Tuple> results = new ArrayList<>();
        for (RecordId rid : rids) {
            Tuple tuple = heapFile.getTuple(rid);
            if (tuple != null) {
                results.add(tuple);
            }
        }
        return results;
    }
}
