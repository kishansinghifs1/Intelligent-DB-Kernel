package database.kernel.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Schema defines the structure of a table — its columns, types, and constraints.
 */
public class Schema {

    public enum ColumnType {
        INT(4),
        FLOAT(4),
        VARCHAR(0); // variable size, stored with 4-byte length prefix

        private final int fixedSize;

        ColumnType(int fixedSize) {
            this.fixedSize = fixedSize;
        }

        public int getFixedSize() {
            return fixedSize;
        }
    }

    public static class Column {
        private final String name;
        private final ColumnType type;
        private final int size; // max size for VARCHAR, fixed for INT/FLOAT
        private final boolean nullable;

        public Column(String name, ColumnType type, int size, boolean nullable) {
            this.name = name;
            this.type = type;
            this.size = (type == ColumnType.VARCHAR) ? size : type.getFixedSize();
            this.nullable = nullable;
        }

        public Column(String name, ColumnType type) {
            this(name, type, type.getFixedSize(), false);
        }

        public Column(String name, ColumnType type, int size) {
            this(name, type, size, false);
        }

        public String getName() { return name; }
        public ColumnType getType() { return type; }
        public int getSize() { return size; }
        public boolean isNullable() { return nullable; }

        /**
         * Maximum bytes this column value can occupy when serialized.
         * VARCHAR: 4-byte length prefix + max size
         * INT/FLOAT: 4 bytes
         */
        public int getMaxSerializedSize() {
            if (type == ColumnType.VARCHAR) {
                return 4 + size; // length prefix + data
            }
            return type.getFixedSize();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Column column = (Column) o;
            return size == column.size && nullable == column.nullable
                    && Objects.equals(name, column.name) && type == column.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, size, nullable);
        }

        @Override
        public String toString() {
            if (type == ColumnType.VARCHAR) {
                return name + " VARCHAR(" + size + ")";
            }
            return name + " " + type.name();
        }
    }

    private final List<Column> columns;

    public Schema(List<Column> columns) {
        this.columns = new ArrayList<>(columns);
    }

    public List<Column> getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Column getColumn(int index) {
        return columns.get(index);
    }

    /**
     * Find the index of a column by name (case-insensitive).
     * Returns -1 if not found.
     */
    public int getColumnIndex(String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Maximum possible serialized size of a tuple with this schema.
     */
    public int getMaxTupleSize() {
        int total = 0;
        for (Column col : columns) {
            total += col.getMaxSerializedSize();
        }
        return total;
    }

    /**
     * Serialize the schema to bytes for catalog persistence.
     */
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(estimateSchemaSize());
        buf.putInt(columns.size());
        for (Column col : columns) {
            byte[] nameBytes = col.getName().getBytes(StandardCharsets.UTF_8);
            buf.putInt(nameBytes.length);
            buf.put(nameBytes);
            buf.putInt(col.getType().ordinal());
            buf.putInt(col.getSize());
            buf.put((byte) (col.isNullable() ? 1 : 0));
        }
        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    /**
     * Deserialize a schema from bytes.
     */
    public static Schema deserialize(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        int colCount = buf.getInt();
        List<Column> cols = new ArrayList<>();
        for (int i = 0; i < colCount; i++) {
            int nameLen = buf.getInt();
            byte[] nameBytes = new byte[nameLen];
            buf.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);
            ColumnType type = ColumnType.values()[buf.getInt()];
            int size = buf.getInt();
            boolean nullable = buf.get() == 1;
            cols.add(new Column(name, type, size, nullable));
        }
        return new Schema(cols);
    }

    private int estimateSchemaSize() {
        int size = 4; // column count
        for (Column col : columns) {
            size += 4 + col.getName().getBytes(StandardCharsets.UTF_8).length; // name
            size += 4 + 4 + 1; // type + size + nullable
        }
        return size;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Schema(");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}
