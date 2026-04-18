package database.kernel.gateway;

import database.kernel.execution.Predicate;
import database.kernel.execution.QueryEngineService;
import database.kernel.execution.QueryResult;
import database.kernel.storage.CatalogService;
import database.kernel.storage.Schema;
import database.kernel.storage.Schema.Column;
import database.kernel.storage.Schema.ColumnType;
import database.kernel.storage.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SqlParserService parses SQL text and routes commands to the query engine.
 *
 * Extracted from the old REPL — removes all I/O concerns (Scanner, PrintStream)
 * for clean separation between parsing logic and transport (REST/gRPC).
 */
@Service
public class SqlParserService {

    private static final Logger log = LoggerFactory.getLogger(SqlParserService.class);

    // ---- Regex patterns for SQL parsing ----
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+(\\w+)\\s*\\((.+)\\)\\s*;?");

    private static final Pattern INSERT = Pattern.compile(
            "(?i)INSERT\\s+INTO\\s+(\\w+)\\s+VALUES\\s*\\((.+)\\)\\s*;?");

    private static final Pattern SELECT_ALL = Pattern.compile(
            "(?i)SELECT\\s+\\*\\s+FROM\\s+(\\w+)\\s*;?");

    private static final Pattern SELECT_WHERE_EQ = Pattern.compile(
            "(?i)SELECT\\s+\\*\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(\\w+)\\s*=\\s*(.+?)\\s*;?");

    private static final Pattern SELECT_WHERE_BETWEEN = Pattern.compile(
            "(?i)SELECT\\s+\\*\\s+FROM\\s+(\\w+)\\s+WHERE\\s+(\\w+)\\s+BETWEEN\\s+(.+?)\\s+AND\\s+(.+?)\\s*;?");

    private static final Pattern CREATE_INDEX = Pattern.compile(
            "(?i)CREATE\\s+INDEX\\s+ON\\s+(\\w+)\\s*\\(\\s*(\\w+)\\s*\\)\\s*;?");

    private static final Pattern DROP_TABLE = Pattern.compile(
            "(?i)DROP\\s+TABLE\\s+(\\w+)\\s*;?");

    private final QueryEngineService engine;
    private final CatalogService catalog;

    public SqlParserService(QueryEngineService engine, CatalogService catalog) {
        this.engine = engine;
        this.catalog = catalog;
    }

    /**
     * Result of executing a single SQL command.
     */
    public record CommandOutput(
            String sql,
            String output,
            boolean isQuery,
            QueryResult queryResult,
            String tableName
    ) {}

    /**
     * Execute a single SQL/meta command and return structured output.
     */
    public CommandOutput execute(String line) {
        line = line.trim();
        if (line.isEmpty()) {
            return new CommandOutput(line, "", false, null, null);
        }

        // Meta commands
        if (line.startsWith("\\")) {
            return handleMetaCommand(line);
        }

        Matcher m;

        // CREATE TABLE
        m = CREATE_TABLE.matcher(line);
        if (m.matches()) {
            return handleCreateTable(line, m.group(1), m.group(2));
        }

        // INSERT INTO
        m = INSERT.matcher(line);
        if (m.matches()) {
            return handleInsert(line, m.group(1), m.group(2));
        }

        // SELECT ... WHERE ... BETWEEN (must check before WHERE =)
        m = SELECT_WHERE_BETWEEN.matcher(line);
        if (m.matches()) {
            return handleSelectBetween(line, m.group(1), m.group(2), m.group(3), m.group(4));
        }

        // SELECT ... WHERE =
        m = SELECT_WHERE_EQ.matcher(line);
        if (m.matches()) {
            return handleSelectEquals(line, m.group(1), m.group(2), m.group(3));
        }

        // SELECT * FROM
        m = SELECT_ALL.matcher(line);
        if (m.matches()) {
            return handleSelectAll(line, m.group(1));
        }

        // CREATE INDEX
        m = CREATE_INDEX.matcher(line);
        if (m.matches()) {
            return handleCreateIndex(line, m.group(1), m.group(2));
        }

        // DROP TABLE
        m = DROP_TABLE.matcher(line);
        if (m.matches()) {
            return handleDropTable(line, m.group(1));
        }

        return new CommandOutput(line, "Unrecognized command. Type \\help for usage.", false, null, null);
    }

    /**
     * Split a script into individual executable commands.
     */
    public List<String> splitCommands(String script) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                current.append(c);
                continue;
            }

            if (!inSingle && !inDouble && c == ';') {
                current.append(c);
                addIfNonBlank(commands, current.toString());
                current.setLength(0);
                continue;
            }

            if (!inSingle && !inDouble && c == '\n') {
                String trimmed = current.toString().trim();
                if (trimmed.startsWith("\\")) {
                    addIfNonBlank(commands, trimmed);
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        addIfNonBlank(commands, current.toString());
        return commands;
    }

    /**
     * Format a QueryResult into human-readable table output.
     */
    public String formatResults(String tableName, QueryResult result) {
        List<Tuple> tuples = result.tuples();
        Schema schema = catalog.getSchema(tableName);
        StringBuilder sb = new StringBuilder();

        if (tuples.isEmpty()) {
            sb.append("(0 rows)\n");
            sb.append(String.format("Strategy: %s | Time: %.3f ms%n", result.strategy(), result.elapsedMs()));
            return sb.toString();
        }

        int cols = schema.getColumnCount();
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = schema.getColumn(i).getName().length();
        }
        for (Tuple t : tuples) {
            for (int i = 0; i < cols; i++) {
                widths[i] = Math.max(widths[i], String.valueOf(t.getValue(i)).length());
            }
        }

        // Header
        sb.append(formatSeparator(widths));
        sb.append(formatRow(widths, i -> schema.getColumn(i).getName(), cols));
        sb.append(formatSeparator(widths));

        // Rows
        for (Tuple t : tuples) {
            sb.append(formatRow(widths, i -> String.valueOf(t.getValue(i)), cols));
        }
        sb.append(formatSeparator(widths));

        sb.append(String.format("(%d rows)%n", tuples.size()));
        sb.append(String.format("Strategy: %s | Time: %.3f ms%n", result.strategy(), result.elapsedMs()));

        return sb.toString();
    }

    // ======== Command Handlers ========

    private CommandOutput handleCreateTable(String sql, String tableName, String columnsDef) {
        List<Column> columns = parseColumns(columnsDef);
        Schema schema = new Schema(columns);
        engine.executeCreateTable(tableName, schema);
        return new CommandOutput(sql, "Table '" + tableName + "' created.", false, null, tableName);
    }

    private CommandOutput handleInsert(String sql, String tableName, String valuesDef) {
        Schema schema = catalog.getSchema(tableName);
        Object[] values = parseValues(valuesDef, schema);
        Tuple tuple = new Tuple(schema, values);
        engine.executeInsert(tableName, tuple);
        return new CommandOutput(sql, "1 row inserted.", false, null, tableName);
    }

    private CommandOutput handleSelectAll(String sql, String tableName) {
        QueryResult result = engine.executeSelect(tableName, null);
        String output = formatResults(tableName, result);
        return new CommandOutput(sql, output, true, result, tableName);
    }

    private CommandOutput handleSelectEquals(String sql, String tableName, String colName, String rawValue) {
        Schema schema = catalog.getSchema(tableName);
        int colIdx = schema.getColumnIndex(colName);
        if (colIdx == -1) {
            throw new RuntimeException("Column not found: " + colName);
        }
        Object value = parseTypedValue(rawValue.trim(), schema.getColumn(colIdx).getType());
        Predicate pred = new Predicate(colName, Predicate.Op.EQUALS, value);
        QueryResult result = engine.executeSelect(tableName, pred);
        String output = formatResults(tableName, result);
        return new CommandOutput(sql, output, true, result, tableName);
    }

    private CommandOutput handleSelectBetween(String sql, String tableName, String colName,
                                               String rawLow, String rawHigh) {
        Schema schema = catalog.getSchema(tableName);
        int colIdx = schema.getColumnIndex(colName);
        if (colIdx == -1) {
            throw new RuntimeException("Column not found: " + colName);
        }
        ColumnType type = schema.getColumn(colIdx).getType();
        Object low = parseTypedValue(rawLow.trim(), type);
        Object high = parseTypedValue(rawHigh.trim(), type);
        Predicate pred = new Predicate(colName, low, high);
        QueryResult result = engine.executeSelect(tableName, pred);
        String output = formatResults(tableName, result);
        return new CommandOutput(sql, output, true, result, tableName);
    }

    private CommandOutput handleCreateIndex(String sql, String tableName, String colName) {
        engine.executeCreateIndex(tableName, colName);
        return new CommandOutput(sql, "Index created on " + tableName + "(" + colName + ").", false, null, tableName);
    }

    private CommandOutput handleDropTable(String sql, String tableName) {
        engine.executeDropTable(tableName);
        return new CommandOutput(sql, "Table '" + tableName + "' dropped.", false, null, tableName);
    }

    private CommandOutput handleMetaCommand(String line) {
        return switch (line.toLowerCase()) {
            case "\\tables" -> {
                var names = catalog.getTableNames();
                if (names.isEmpty()) {
                    yield new CommandOutput(line, "No tables.", false, null, null);
                }
                StringBuilder sb = new StringBuilder("Tables:\n");
                for (String n : names) {
                    Schema s = catalog.getSchema(n);
                    sb.append("  ").append(n).append(" ").append(s).append("\n");
                }
                yield new CommandOutput(line, sb.toString(), false, null, null);
            }
            case "\\indexes" -> {
                var keys = catalog.getIndexKeys();
                if (keys.isEmpty()) {
                    yield new CommandOutput(line, "No indexes.", false, null, null);
                }
                StringBuilder sb = new StringBuilder("Indexes:\n");
                for (String k : keys) {
                    sb.append("  ").append(k).append("\n");
                }
                yield new CommandOutput(line, sb.toString(), false, null, null);
            }
            case "\\help" -> new CommandOutput(line, getHelpText(), false, null, null);
            default -> new CommandOutput(line, "Unknown command: " + line + ". Type \\help for usage.", false, null, null);
        };
    }

    // ======== Parsing Helpers ========

    private List<Column> parseColumns(String def) {
        List<Column> columns = new ArrayList<>();
        String[] parts = def.split(",");
        for (String part : parts) {
            part = part.trim();
            Matcher varcharMatch = Pattern.compile("(?i)(\\w+)\\s+VARCHAR\\s*\\(\\s*(\\d+)\\s*\\)").matcher(part);
            if (varcharMatch.matches()) {
                columns.add(new Column(varcharMatch.group(1), ColumnType.VARCHAR,
                        Integer.parseInt(varcharMatch.group(2))));
                continue;
            }
            String[] tokens = part.split("\\s+");
            if (tokens.length < 2) {
                throw new RuntimeException("Invalid column definition: " + part);
            }
            String name = tokens[0];
            ColumnType type = parseType(tokens[1]);
            columns.add(new Column(name, type));
        }
        return columns;
    }

    private ColumnType parseType(String typeStr) {
        return switch (typeStr.toUpperCase()) {
            case "INT", "INTEGER" -> ColumnType.INT;
            case "FLOAT", "REAL" -> ColumnType.FLOAT;
            case "VARCHAR", "TEXT", "STRING" -> ColumnType.VARCHAR;
            default -> throw new RuntimeException("Unknown type: " + typeStr);
        };
    }

    private Object[] parseValues(String def, Schema schema) {
        List<String> tokens = tokenizeValues(def);
        if (tokens.size() != schema.getColumnCount()) {
            throw new RuntimeException(
                    "Expected " + schema.getColumnCount() + " values, got " + tokens.size());
        }
        Object[] values = new Object[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            values[i] = parseTypedValue(tokens.get(i), schema.getColumn(i).getType());
        }
        return values;
    }

    private List<String> tokenizeValues(String def) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < def.length(); i++) {
            char c = def.charAt(i);
            if (c == '\'' || c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString().trim());
        return tokens;
    }

    private Object parseTypedValue(String raw, ColumnType type) {
        raw = raw.trim();
        if ((raw.startsWith("'") && raw.endsWith("'")) ||
                (raw.startsWith("\"") && raw.endsWith("\""))) {
            raw = raw.substring(1, raw.length() - 1);
        }
        return switch (type) {
            case INT -> Integer.parseInt(raw);
            case FLOAT -> Float.parseFloat(raw);
            case VARCHAR -> raw;
        };
    }

    @FunctionalInterface
    private interface CellProvider {
        String get(int index);
    }

    private String formatRow(int[] widths, CellProvider provider, int cols) {
        StringBuilder sb = new StringBuilder("| ");
        for (int i = 0; i < cols; i++) {
            sb.append(String.format("%-" + widths[i] + "s", provider.get(i)));
            if (i < cols - 1) sb.append(" | ");
        }
        sb.append(" |\n");
        return sb.toString();
    }

    private String formatSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    private void addIfNonBlank(List<String> commands, String raw) {
        String cmd = raw.trim();
        if (!cmd.isEmpty()) {
            commands.add(cmd);
        }
    }

    private String getHelpText() {
        return """
                Supported commands:
                  CREATE TABLE name (col1 TYPE, col2 TYPE, ...);
                  INSERT INTO name VALUES (v1, v2, ...);
                  SELECT * FROM name;
                  SELECT * FROM name WHERE col = value;
                  SELECT * FROM name WHERE col BETWEEN v1 AND v2;
                  CREATE INDEX ON name(col);
                  DROP TABLE name;
                  \\tables   - list all tables
                  \\indexes  - list all indexes
                  \\help     - show this help

                Types: INT, FLOAT, VARCHAR(n)
                """;
    }
}
