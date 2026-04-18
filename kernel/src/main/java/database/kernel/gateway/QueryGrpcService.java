package database.kernel.gateway;

import database.kernel.execution.QueryResult;
import database.kernel.grpc.query.v1.*;
import database.kernel.storage.CatalogService;
import database.kernel.storage.DiskManager;
import database.kernel.storage.Schema;
import database.kernel.storage.Tuple;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * gRPC server service for frontend query execution.
 *
 * Clients can send SQL queries via gRPC instead of REST.
 * This is the high-performance path for programmatic access.
 */
@GrpcService
public class QueryGrpcService extends QueryServiceGrpc.QueryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(QueryGrpcService.class);

    private final SqlParserService sqlParser;
    private final CatalogService catalog;
    private final DiskManager diskManager;
    private final Object executeLock = new Object();

    public QueryGrpcService(SqlParserService sqlParser,
                            CatalogService catalog,
                            DiskManager diskManager) {
        this.sqlParser = sqlParser;
        this.catalog = catalog;
        this.diskManager = diskManager;
        log.info("QueryGrpcService initialized — gRPC query server ready");
    }

    @Override
    public void execute(ExecuteRequest request, StreamObserver<ExecuteResponse> responseObserver) {
        String sql = request.getSql().trim();

        if (sql.isEmpty()) {
            responseObserver.onNext(ExecuteResponse.newBuilder()
                    .setOk(false)
                    .setError("SQL cannot be empty")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        List<String> commands = sqlParser.splitCommands(sql);
        if (commands.isEmpty()) {
            responseObserver.onNext(ExecuteResponse.newBuilder()
                    .setOk(false)
                    .setError("No executable command found")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        long totalStart = System.nanoTime();
        List<CommandResult> results = new ArrayList<>();

        synchronized (executeLock) {
            for (String cmd : commands) {
                try {
                    SqlParserService.CommandOutput output = sqlParser.execute(cmd);

                    CommandResult.Builder resultBuilder = CommandResult.newBuilder()
                            .setSql(cmd)
                            .setOutput(output.output())
                            .setIsQuery(output.isQuery());

                    if (output.queryResult() != null) {
                        resultBuilder.setStrategy(output.queryResult().strategy());
                        resultBuilder.setElapsedNanos(output.queryResult().elapsedNanos());

                        // Build structured ResultSet for SELECT queries
                        if (output.isQuery() && output.tableName() != null) {
                            resultBuilder.setResultSet(buildResultSet(output));
                        }
                    }

                    results.add(resultBuilder.build());
                } catch (Exception e) {
                    results.add(CommandResult.newBuilder()
                            .setSql(cmd)
                            .setOutput("ERROR: " + e.getMessage())
                            .setIsQuery(false)
                            .build());
                }
            }
        }

        long totalElapsed = System.nanoTime() - totalStart;

        responseObserver.onNext(ExecuteResponse.newBuilder()
                .setOk(true)
                .addAllResults(results)
                .setCommandCount(commands.size())
                .setTotalElapsedNanos(totalElapsed)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getState(StateRequest request, StreamObserver<StateResponse> responseObserver) {
        try {
            Set<String> tableNames = catalog.getTableNames();
            Set<String> indexKeys = catalog.getIndexKeys();

            List<TableInfo> tables = new ArrayList<>();
            for (String t : tableNames) {
                Schema schema = catalog.getSchema(t);
                List<ColumnMeta> columns = new ArrayList<>();
                for (Schema.Column col : schema.getColumns()) {
                    columns.add(ColumnMeta.newBuilder()
                            .setName(col.getName())
                            .setType(col.getType().name())
                            .setSize(col.getSize())
                            .build());
                }
                tables.add(TableInfo.newBuilder()
                        .setName(t)
                        .setSchema(schema.toString())
                        .addAllColumns(columns)
                        .build());
            }

            List<String> files = listDataFiles();

            responseObserver.onNext(StateResponse.newBuilder()
                    .setOk(true)
                    .setDataDirectory(diskManager.getDataDirectory())
                    .setTableCount(tableNames.size())
                    .addAllTables(tables)
                    .addAllIndexes(new ArrayList<>(indexKeys))
                    .addAllDataFiles(files)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void executeStream(ExecuteRequest request, StreamObserver<QueryResultRow> responseObserver) {
        String sql = request.getSql().trim();

        try {
            List<String> commands = sqlParser.splitCommands(sql);
            for (String cmd : commands) {
                SqlParserService.CommandOutput output;
                synchronized (executeLock) {
                    output = sqlParser.execute(cmd);
                }

                if (output.isQuery() && output.queryResult() != null && output.tableName() != null) {
                    Schema schema = catalog.getSchema(output.tableName());
                    List<Tuple> tuples = output.queryResult().tuples();

                    // Build column metadata
                    List<ColumnMeta> columns = new ArrayList<>();
                    for (Schema.Column col : schema.getColumns()) {
                        columns.add(ColumnMeta.newBuilder()
                                .setName(col.getName())
                                .setType(col.getType().name())
                                .setSize(col.getSize())
                                .build());
                    }

                    // Stream each row
                    for (int i = 0; i < tuples.size(); i++) {
                        Tuple tuple = tuples.get(i);
                        Row row = buildRow(tuple, schema);

                        QueryResultRow.Builder rowBuilder = QueryResultRow.newBuilder()
                                .setRow(row)
                                .setIsLast(i == tuples.size() - 1);

                        // Include meta on first row
                        if (i == 0) {
                            rowBuilder.setMeta(ResultSetMeta.newBuilder()
                                    .addAllColumns(columns)
                                    .setStrategy(output.queryResult().strategy())
                                    .setElapsedNanos(output.queryResult().elapsedNanos())
                                    .setTotalRows(tuples.size())
                                    .build());
                        }

                        responseObserver.onNext(rowBuilder.build());
                    }
                }
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private ResultSet buildResultSet(SqlParserService.CommandOutput output) {
        Schema schema = catalog.getSchema(output.tableName());
        List<Tuple> tuples = output.queryResult().tuples();

        List<ColumnMeta> columns = new ArrayList<>();
        for (Schema.Column col : schema.getColumns()) {
            columns.add(ColumnMeta.newBuilder()
                    .setName(col.getName())
                    .setType(col.getType().name())
                    .setSize(col.getSize())
                    .build());
        }

        List<Row> rows = new ArrayList<>();
        for (Tuple tuple : tuples) {
            rows.add(buildRow(tuple, schema));
        }

        return ResultSet.newBuilder()
                .addAllColumns(columns)
                .addAllRows(rows)
                .setRowCount(tuples.size())
                .build();
    }

    private Row buildRow(Tuple tuple, Schema schema) {
        Row.Builder rowBuilder = Row.newBuilder();
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Object val = tuple.getValue(i);
            Value.Builder valBuilder = Value.newBuilder();
            if (val == null) {
                valBuilder.setIsNull(true);
            } else {
                switch (schema.getColumn(i).getType()) {
                    case INT -> valBuilder.setIntVal((Integer) val);
                    case FLOAT -> valBuilder.setFloatVal((Float) val);
                    case VARCHAR -> valBuilder.setStringVal((String) val);
                }
            }
            rowBuilder.addValues(valBuilder.build());
        }
        return rowBuilder.build();
    }

    private List<String> listDataFiles() {
        File dir = new File(diskManager.getDataDirectory());
        List<String> files = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) return files;
        for (File f : children) {
            if (f.isFile()) files.add(f.getName());
        }
        files.sort(String::compareToIgnoreCase);
        return files;
    }
}
