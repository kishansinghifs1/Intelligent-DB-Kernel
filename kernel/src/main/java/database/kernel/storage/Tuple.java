package database.kernel.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Tuple represents a single row in a table.
 * Supports serialization/deserialization to/from byte arrays.
 */
public class Tuple {

    private final Schema schema;
    private final Object[] values;

    public Tuple(Schema schema, Object[] values) {
        if (values.length != schema.getColumnCount()) {
            throw new IllegalArgumentException(
                    "Expected " + schema.getColumnCount() + " values, got " + values.length);
        }
        this.schema = schema;
        this.values = Arrays.copyOf(values, values.length);
    }

    public Schema getSchema() {
        return schema;
    }

    public Object getValue(int index) {
        return values[index];
    }

    public Object getValue(String columnName) {
        int idx = schema.getColumnIndex(columnName);
        if (idx == -1) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return values[idx];
    }

    public Object[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    /**
     * Serialize this tuple to a byte array.
     *
     * Layout per field:
     *   INT:     4 bytes (big-endian int)
     *   FLOAT:   4 bytes (big-endian float)
     *   VARCHAR: 4-byte length prefix + UTF-8 bytes
     */
    public byte[] serialize() {
        int size = getSerializedSize();
        ByteBuffer buf = ByteBuffer.allocate(size);

        for (int i = 0; i < schema.getColumnCount(); i++) {
            Schema.Column col = schema.getColumn(i);
            Object val = values[i];

            switch (col.getType()) {
                case INT -> buf.putInt((Integer) val);
                case FLOAT -> buf.putFloat((Float) val);
                case VARCHAR -> {
                    String s = (String) val;
                    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                    buf.putInt(bytes.length);
                    buf.put(bytes);
                }
            }
        }
        return buf.array();
    }

    /**
     * Deserialize a tuple from bytes using the given schema.
     */
    public static Tuple deserialize(byte[] data, Schema schema) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        Object[] values = new Object[schema.getColumnCount()];

        for (int i = 0; i < schema.getColumnCount(); i++) {
            Schema.Column col = schema.getColumn(i);

            switch (col.getType()) {
                case INT -> values[i] = buf.getInt();
                case FLOAT -> values[i] = buf.getFloat();
                case VARCHAR -> {
                    int len = buf.getInt();
                    byte[] bytes = new byte[len];
                    buf.get(bytes);
                    values[i] = new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        return new Tuple(schema, values);
    }

    /**
     * Calculate the exact serialized size of this tuple in bytes.
     */
    public int getSerializedSize() {
        int size = 0;
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Schema.Column col = schema.getColumn(i);
            switch (col.getType()) {
                case INT -> size += 4;
                case FLOAT -> size += 4;
                case VARCHAR -> {
                    String s = (String) values[i];
                    size += 4 + s.getBytes(StandardCharsets.UTF_8).length;
                }
            }
        }
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple tuple = (Tuple) o;
        return Arrays.equals(values, tuple.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Tuple(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(schema.getColumn(i).getName()).append("=").append(values[i]);
        }
        sb.append(")");
        return sb.toString();
    }
}
