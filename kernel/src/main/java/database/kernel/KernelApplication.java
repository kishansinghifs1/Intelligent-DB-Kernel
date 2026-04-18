package database.kernel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * MiniPostgres Kernel — Spring Boot Application.
 *
 * Architecture (ownership boundaries):
 *   Gateway        → REST Controller + gRPC Server (query execution)
 *   Query Engine   → SeqScan, IndexScan, Predicate evaluation
 *   Storage Engine → BufferPool, DiskManager, Catalog, HeapFile, B+Tree
 *   Model Advisor  → gRPC Client → Python RL Optimizer
 *   Cache          → Caffeine (in-memory), Redis-ready
 */
@SpringBootApplication
@EnableCaching
public class KernelApplication {

    private static final Logger log = LoggerFactory.getLogger(KernelApplication.class);

    public static void main(String[] args) {
        printBanner();
        SpringApplication.run(KernelApplication.class, args);
    }

    private static void printBanner() {
        System.out.println("""
                ╔══════════════════════════════════════════════════════╗
                ║          🐘 MiniPostgres Kernel v2.0                ║
                ║    Spring Boot + gRPC Database Engine                ║
                ║                                                      ║
                ║    Boundaries:                                       ║
                ║      🌐 Gateway     (REST + gRPC)                   ║
                ║      ⚡ Query Engine (RL-optimized scans)            ║
                ║      💾 Storage     (BufferPool + B+Tree)            ║
                ║      🧠 Advisor     (gRPC → Python RL)              ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
