package com.vortexmesh.sidecar.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@Slf4j
public class ResilienceManager {

    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Retry> retries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

    public CircuitBreaker getOrCreateCircuitBreaker(String serviceId, int failureRate, int waitDuration) {
        return circuitBreakers.computeIfAbsent(serviceId, id -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(failureRate)
                    .waitDurationInOpenState(Duration.ofMillis(waitDuration))
                    .slidingWindowSize(10)
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build();

            CircuitBreaker cb = CircuitBreakerRegistry.of(config).circuitBreaker(id);
            
            cb.getEventPublisher()
                    .onStateTransition(event -> 
                        log.warn("Circuit breaker [{}] state: {} -> {}", 
                            id, event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()));

            return cb;
        });
    }

    public Retry getOrCreateRetry(String serviceId, int maxAttempts, long waitDuration) {
        return retries.computeIfAbsent(serviceId, id -> {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(maxAttempts)
                    .waitDuration(Duration.ofMillis(waitDuration))
                    .retryExceptions(Exception.class)
                    .build();
            return RetryRegistry.of(config).retry(id);
        });
    }

    public Bulkhead getOrCreateBulkhead(String serviceId, int maxConcurrent) {
        return bulkheads.computeIfAbsent(serviceId, id -> {
            BulkheadConfig config = BulkheadConfig.custom()
                    .maxConcurrentCalls(maxConcurrent)
                    .maxWaitDuration(Duration.ofMillis(500))
                    .build();
            return BulkheadRegistry.of(config).bulkhead(id);
        });
    }

    public <T> T executeWithResilience(String serviceId, Supplier<T> supplier) {
        CircuitBreaker cb = getOrCreateCircuitBreaker(serviceId, 50, 10000);
        Retry retry = getOrCreateRetry(serviceId, 3, 500);
        Bulkhead bulkhead = getOrCreateBulkhead(serviceId, 25);

        Supplier<T> decorated = CircuitBreaker.decorateSupplier(cb,
                Retry.decorateSupplier(retry,
                        Bulkhead.decorateSupplier(bulkhead, supplier)));

        return decorated.get();
    }
}
