package database.kernel.execution;

import database.kernel.storage.Tuple;

import java.util.List;

/**
 * QueryResult holds the result of a query execution with metadata.
 *
 * @param tuples       the matching tuples
 * @param strategy     the execution strategy used (e.g., "SeqScan", "IndexScan (RL)")
 * @param elapsedNanos execution time in nanoseconds
 */
public record QueryResult(
        List<Tuple> tuples,
        String strategy,
        long elapsedNanos
) {
    /**
     * Elapsed time in milliseconds.
     */
    public double elapsedMs() {
        return elapsedNanos / 1_000_000.0;
    }

    /**
     * Number of matching rows.
     */
    public int rowCount() {
        return tuples.size();
    }
}
