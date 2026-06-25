package com.vortexmesh.telemetry.collector;

import com.vortexmesh.common.event.TelemetryEvent;
import com.vortexmesh.telemetry.model.MetricRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryIngestionService {

    private final MeterRegistry meterRegistry;
    @PersistenceContext
    private EntityManager entityManager;

    private final ConcurrentHashMap<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();

    @KafkaListener(topics = "vortex.telemetry.events", groupId = "telemetry-service")
    @Transactional
    public void ingestTelemetry(TelemetryEvent event) {
        // Record metrics
        Timer.builder("vortex.request.duration")
                .tag("service", event.getServiceId() != null ? event.getServiceId() : "unknown")
                .tag("method", event.getMethod() != null ? event.getMethod() : "unknown")
                .tag("status", String.valueOf(event.getStatusCode()))
                .register(meterRegistry)
                .record(Duration.ofMillis(event.getDurationMs()));

        // Count requests
        String serviceKey = event.getServiceId() != null ? event.getServiceId() : "unknown";
        requestCounters.computeIfAbsent(serviceKey, k -> new AtomicLong(0)).incrementAndGet();

        if (event.getStatusCode() >= 400) {
            errorCounters.computeIfAbsent(serviceKey, k -> new AtomicLong(0)).incrementAndGet();
        }

        // Persist for analytics
        MetricRecord record = MetricRecord.builder()
                .traceId(event.getTraceId())
                .serviceId(event.getServiceId())
                .operation(event.getOperation())
                .method(event.getMethod())
                .path(event.getPath())
                .statusCode(event.getStatusCode())
                .durationMs(event.getDurationMs())
                .timestamp(event.getTimestamp())
                .eventType(event.getType() != null ? event.getType().name() : "REQUEST")
                .build();

        entityManager.persist(record);

        if (event.getDurationMs() > 5000) {
            log.warn("[ALERT] Slow request detected: {} {} took {}ms (service: {})",
                event.getMethod(), event.getPath(), event.getDurationMs(), event.getServiceId());
        }
    }

    public long getRequestCount(String serviceId) {
        AtomicLong counter = requestCounters.get(serviceId);
        return counter != null ? counter.get() : 0;
    }

    public double getErrorRate(String serviceId) {
        long requests = getRequestCount(serviceId);
        if (requests == 0) return 0.0;
        AtomicLong errors = errorCounters.get(serviceId);
        return errors != null ? (double) errors.get() / requests * 100 : 0.0;
    }
}
