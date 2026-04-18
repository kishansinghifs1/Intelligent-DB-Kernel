package database.kernel.gateway;

import database.kernel.cache.QueryCacheService;
import database.kernel.storage.CatalogService;
import database.kernel.storage.DiskManager;
import database.kernel.storage.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

/**
 * REST controller for MiniPostgres kernel.
 *
 * Provides HTTP endpoints for SQL execution, state inspection,
 * health checks, and cache statistics.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SqlController {

    private static final Logger log = LoggerFactory.getLogger(SqlController.class);

    private final SqlParserService sqlParser;
    private final CatalogService catalog;
    private final DiskManager diskManager;
    private final QueryCacheService cacheService;
    private final Object executeLock = new Object();

    public SqlController(SqlParserService sqlParser,
                         CatalogService catalog,
                         DiskManager diskManager,
                         QueryCacheService cacheService) {
        this.sqlParser = sqlParser;
        this.catalog = catalog;
        this.diskManager = diskManager;
        this.cacheService = cacheService;
    }

    /**
     * Execute SQL commands.
     * Accepts a raw SQL script (may contain multiple statements).
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody String script) {
        script = script.trim();
        if (script.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "SQL cannot be empty"
            ));
        }

        List<String> commands = sqlParser.splitCommands(script);
        if (commands.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "No executable command found"
            ));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        StringBuilder transcript = new StringBuilder();

        synchronized (executeLock) {
            for (String cmd : commands) {
                try {
                    SqlParserService.CommandOutput output = sqlParser.execute(cmd);
                    transcript.append("minipostgres> ").append(cmd).append("\n");
                    transcript.append(output.output());
                    if (!output.output().endsWith("\n")) {
                        transcript.append("\n");
                    }

                    Map<String, Object> resultEntry = new LinkedHashMap<>();
                    resultEntry.put("sql", cmd);
                    resultEntry.put("output", output.output());
                    resultEntry.put("isQuery", output.isQuery());
                    if (output.queryResult() != null) {
                        resultEntry.put("strategy", output.queryResult().strategy());
                        resultEntry.put("elapsedMs", output.queryResult().elapsedMs());
                        resultEntry.put("rowCount", output.queryResult().rowCount());
                    }
                    results.add(resultEntry);
                } catch (Exception e) {
                    transcript.append("minipostgres> ").append(cmd).append("\n");
                    transcript.append("ERROR: ").append(e.getMessage()).append("\n");
                    results.add(Map.of(
                            "sql", cmd,
                            "output", "ERROR: " + e.getMessage(),
                            "isQuery", false
                    ));
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("output", transcript.toString());
        response.put("commandCount", commands.size());
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    /**
     * Get database state — tables, indexes, data files.
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> state() {
        Set<String> tableNames = catalog.getTableNames();
        Set<String> indexKeys = catalog.getIndexKeys();

        List<Map<String, Object>> tables = new ArrayList<>();
        for (String t : tableNames) {
            Schema schema = catalog.getSchema(t);
            List<Map<String, String>> columns = new ArrayList<>();
            for (Schema.Column col : schema.getColumns()) {
                columns.add(Map.of(
                        "name", col.getName(),
                        "type", col.getType().name(),
                        "size", String.valueOf(col.getSize())
                ));
            }
            tables.add(Map.of(
                    "name", t,
                    "schema", schema.toString(),
                    "columns", columns
            ));
        }

        List<String> files = listDataFiles();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("dataDirectory", diskManager.getDataDirectory());
        response.put("tableCount", tableNames.size());
        response.put("tables", tables);
        response.put("indexes", new ArrayList<>(indexKeys));
        response.put("dataFiles", files);

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "tables", catalog.getTableNames().size(),
                "indexes", catalog.getIndexKeys().size(),
                "cacheSize", cacheService.size(),
                "cacheStats", cacheService.stats()
        ));
    }

    /**
     * Cache statistics endpoint.
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        return ResponseEntity.ok(Map.of(
                "size", cacheService.size(),
                "stats", cacheService.stats()
        ));
    }

    /**
     * Clear all caches.
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> cacheClear() {
        cacheService.invalidateAll();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Cache cleared"
        ));
    }

    private List<String> listDataFiles() {
        File dir = new File(diskManager.getDataDirectory());
        List<String> files = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) {
            return files;
        }
        for (File f : children) {
            if (f.isFile()) {
                files.add(f.getName());
            }
        }
        files.sort(String::compareToIgnoreCase);
        return files;
    }
}
