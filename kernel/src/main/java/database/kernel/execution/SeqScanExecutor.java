package database.kernel.execution;

import database.kernel.storage.CatalogService;
import database.kernel.storage.HeapFile;
import database.kernel.storage.Tuple;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SeqScanExecutor performs a full sequential scan over a table,
 * applying an optional predicate filter.
 *
 * Time complexity: O(n) where n = total tuples.
 */
@Component
public class SeqScanExecutor {

    private final CatalogService catalog;

    public SeqScanExecutor(CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Scan all tuples in the table, returning those matching the predicate.
     *
     * @param tableName  the table to scan
     * @param predicate  filter to apply (null = return all)
     * @return matching tuples
     */
    public List<Tuple> scan(String tableName, Predicate predicate) {
        HeapFile heapFile = catalog.getTable(tableName);
        List<Tuple> results = new ArrayList<>();

        Iterator<HeapFile.TupleWithId> it = heapFile.iterator();
        while (it.hasNext()) {
            Tuple tuple = it.next().getTuple();
            if (predicate == null || predicate.evaluate(tuple)) {
                results.add(tuple);
            }
        }

        return results;
    }

    /**
     * Scan all tuples with no filter.
     */
    public List<Tuple> scanAll(String tableName) {
        return scan(tableName, null);
    }
}
