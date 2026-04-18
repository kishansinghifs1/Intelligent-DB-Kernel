package database.kernel.advisor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Model Advisor gRPC client.
 * Sets up the gRPC channel, circuit breaker, and retry policies.
 */
@Configuration
public class ModelAdvisorConfig {

    private static final Logger log = LoggerFactory.getLogger(ModelAdvisorConfig.class);

    @Value("${minipostgres.optimizer.grpc.host:localhost}")
    private String optimizerHost;

    @Value("${minipostgres.optimizer.grpc.port:50051}")
    private int optimizerPort;

    private ManagedChannel channel;

    /**
     * gRPC channel to the Python RL optimizer.
     */
    @Bean
    public ManagedChannel optimizerChannel() {
        channel = ManagedChannelBuilder
                .forAddress(optimizerHost, optimizerPort)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                .build();
        log.info("gRPC channel configured: {}:{}", optimizerHost, optimizerPort);
        return channel;
    }

    /**
     * Circuit breaker for the optimizer client.
     *
     * Opens after 5 failures in a sliding window of 10 calls.
     * Stays open for 30 seconds before attempting half-open.
     */
    @Bean
    public CircuitBreaker optimizerCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)               // 50% failure rate triggers OPEN
                .slidingWindowSize(10)                   // last 10 calls
                .minimumNumberOfCalls(5)                 // need at least 5 calls before evaluating
                .waitDurationInOpenState(Duration.ofSeconds(30)) // wait 30s before half-open
                .permittedNumberOfCallsInHalfOpenState(3)        // allow 3 test calls in half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker cb = CircuitBreaker.of("optimizer", config);
        cb.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit breaker state transition: {}", event.getStateTransition()));
        return cb;
    }

    /**
     * Retry policy for predict calls.
     *
     * 1 retry with 100ms base + jitter for predict (latency-sensitive).
     */
    @Bean
    public Retry predictRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)                                  // 1 initial + 1 retry
                .waitDuration(Duration.ofMillis(100))            // base wait
                .retryExceptions(io.grpc.StatusRuntimeException.class)
                .build();

        return Retry.of("predict", config);
    }

    /**
     * Retry policy for reward reporting calls.
     *
     * 3 retries with exponential backoff + jitter for reward (less latency-sensitive).
     */
    @Bean
    public Retry rewardRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)                                  // 1 initial + 3 retries
                .waitDuration(Duration.ofMillis(200))            // base wait
                .retryExceptions(io.grpc.StatusRuntimeException.class)
                .build();

        return Retry.of("reward", config);
    }

    @PreDestroy
    public void shutdownChannel() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel...");
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
