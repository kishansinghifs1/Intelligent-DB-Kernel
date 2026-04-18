package database.kernel.advisor;

import database.kernel.grpc.optimizer.v1.HealthCheckRequest;
import database.kernel.grpc.optimizer.v1.HealthCheckResponse;
import database.kernel.grpc.optimizer.v1.PredictRequest;
import database.kernel.grpc.optimizer.v1.PredictResponse;
import database.kernel.grpc.optimizer.v1.QueryOptimizerServiceGrpc;
import database.kernel.grpc.optimizer.v1.QueryState;
import database.kernel.grpc.optimizer.v1.RewardRequest;
import database.kernel.grpc.optimizer.v1.RewardResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * ModelAdvisorService communicates with the Python RL optimizer via gRPC.
 *
 * Features:
 * - Circuit breaking: falls back to heuristic when optimizer is down
 * - Retry with jitter: automatic retries on transient failures
 * - Timeouts: 200ms for predict, 500ms for reward
 * - Idempotency keys: safe to retry reward writes
 * - Async reward reporting: fire-and-forget with best-effort delivery
 */
@Service
public class ModelAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(ModelAdvisorService.class);

    private final ManagedChannel channel;
    private final CircuitBreaker circuitBreaker;
    private final Retry predictRetry;
    private final Retry rewardRetry;

    private QueryOptimizerServiceGrpc.QueryOptimizerServiceBlockingStub blockingStub;

    /**
     * Simplified query state for the RL agent.
     */
    public record QueryState(int numRows, boolean isRange, boolean hasIndex) {}

    public ModelAdvisorService(ManagedChannel optimizerChannel,
                                CircuitBreaker optimizerCircuitBreaker,
                                Retry predictRetry,
                                Retry rewardRetry) {
        this.channel = optimizerChannel;
        this.circuitBreaker = optimizerCircuitBreaker;
        this.predictRetry = predictRetry;
        this.rewardRetry = rewardRetry;
    }

    @PostConstruct
    public void init() {
        this.blockingStub = QueryOptimizerServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(200, TimeUnit.MILLISECONDS);
        log.info("ModelAdvisorService initialized — gRPC stub ready");

        // Probe health asynchronously
        CompletableFuture.runAsync(this::probeHealth);
    }

    /**
     * Ask the RL agent for the best action (0 for SeqScan, 1 for IndexScan).
     * Falls back to -1 (heuristic) if the optimizer is unavailable.
     */
    public int predict(QueryState state) {
        Supplier<Integer> decorated = CircuitBreaker.decorateSupplier(circuitBreaker,
                Retry.decorateSupplier(predictRetry, () -> {
                    PredictRequest request = PredictRequest.newBuilder()
                            .setNumRows(state.numRows())
                            .setIsRange(state.isRange())
                            .setHasIndex(state.hasIndex())
                            .setRequestId(UUID.randomUUID().toString())
                            .build();

                    PredictResponse response = blockingStub
                            .withDeadlineAfter(200, TimeUnit.MILLISECONDS)
                            .predict(request);

                    return response.getAction();
                }));

        try {
            return decorated.get();
        } catch (Exception e) {
            log.debug("RL Optimizer unavailable, falling back to heuristic: {}", e.getMessage());
            return -1; // fallback to heuristic
        }
    }

    /**
     * Report the result of an action to the RL agent for learning.
     * Fire-and-forget with idempotency key for safe retries.
     */
    public void reportReward(QueryState state, int action, double reward) {
        String idempotencyKey = UUID.randomUUID().toString();

        CompletableFuture.runAsync(() -> {
            Runnable decorated = CircuitBreaker.decorateRunnable(circuitBreaker,
                    Retry.decorateRunnable(rewardRetry, () -> {
                        database.kernel.grpc.optimizer.v1.QueryState grpcState =
                                database.kernel.grpc.optimizer.v1.QueryState.newBuilder()
                                        .setNumRows(state.numRows())
                                        .setIsRange(state.isRange())
                                        .setHasIndex(state.hasIndex())
                                        .build();

                        RewardRequest request = RewardRequest.newBuilder()
                                .setState(grpcState)
                                .setAction(action)
                                .setReward(reward)
                                .setIdempotencyKey(idempotencyKey)
                                .build();

                        blockingStub
                                .withDeadlineAfter(500, TimeUnit.MILLISECONDS)
                                .reportReward(request);
                    }));

            try {
                decorated.run();
            } catch (Exception e) {
                log.warn("Failed to report reward to RL Optimizer: {}", e.getMessage());
            }
        });
    }

    /**
     * Probe the optimizer health on startup.
     */
    private void probeHealth() {
        try {
            HealthCheckResponse response = blockingStub
                    .withDeadlineAfter(2000, TimeUnit.MILLISECONDS)
                    .healthCheck(HealthCheckRequest.getDefaultInstance());
            log.info("RL Optimizer health: status={}, ready={}, q_table_states={}, version={}",
                    response.getStatus(), response.getReady(),
                    response.getQTableStates(), response.getVersion());
        } catch (StatusRuntimeException e) {
            log.warn("RL Optimizer not available at startup (will use heuristic fallback): {}", e.getMessage());
        } catch (Exception e) {
            log.warn("RL Optimizer health probe failed: {}", e.getMessage());
        }
    }
}
