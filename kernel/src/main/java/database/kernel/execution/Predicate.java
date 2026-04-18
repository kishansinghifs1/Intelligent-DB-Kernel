package database.kernel.execution;

import database.kernel.storage.Tuple;

/**
 * Predicate represents a filter condition for query evaluation.
 * Supports equality (=), less than (<), greater than (>),
 * less than or equal (<=), greater than or equal (>=),
 * not equal (!=), and BETWEEN operations.
 */
public class Predicate {

    public enum Op {
        EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL, BETWEEN
    }

    private final String columnName;
    private final Op op;
    private final Object value;       // for single-value ops
    private final Object valueLow;    // for BETWEEN
    private final Object valueHigh;   // for BETWEEN

    /**
     * Create a single-value predicate (e.g., col = 5).
     */
    public Predicate(String columnName, Op op, Object value) {
        this.columnName = columnName;
        this.op = op;
        this.value = value;
        this.valueLow = null;
        this.valueHigh = null;
    }

    /**
     * Create a BETWEEN predicate (col BETWEEN low AND high).
     */
    public Predicate(String columnName, Object low, Object high) {
        this.columnName = columnName;
        this.op = Op.BETWEEN;
        this.value = null;
        this.valueLow = low;
        this.valueHigh = high;
    }

    public String getColumnName() { return columnName; }
    public Op getOp() { return op; }
    public Object getValue() { return value; }
    public Object getValueLow() { return valueLow; }
    public Object getValueHigh() { return valueHigh; }

    /**
     * Evaluate this predicate against a tuple.
     *
     * @return true if the tuple satisfies the predicate
     */
    @SuppressWarnings("unchecked")
    public boolean evaluate(Tuple tuple) {
        Object tupleVal = tuple.getValue(columnName);
        if (tupleVal == null) {
            return false;
        }

        if (op == Op.BETWEEN) {
            Comparable<Object> tv = (Comparable<Object>) tupleVal;
            return tv.compareTo(valueLow) >= 0 && tv.compareTo(valueHigh) <= 0;
        }

        Comparable<Object> tv = (Comparable<Object>) tupleVal;
        int cmp = tv.compareTo(value);

        return switch (op) {
            case EQUALS -> cmp == 0;
            case NOT_EQUALS -> cmp != 0;
            case LESS_THAN -> cmp < 0;
            case GREATER_THAN -> cmp > 0;
            case LESS_EQUAL -> cmp <= 0;
            case GREATER_EQUAL -> cmp >= 0;
            default -> false;
        };
    }

    @Override
    public String toString() {
        if (op == Op.BETWEEN) {
            return columnName + " BETWEEN " + valueLow + " AND " + valueHigh;
        }
        String opStr = switch (op) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case LESS_EQUAL -> "<=";
            case GREATER_EQUAL -> ">=";
            default -> op.name();
        };
        return columnName + " " + opStr + " " + value;
    }
}
