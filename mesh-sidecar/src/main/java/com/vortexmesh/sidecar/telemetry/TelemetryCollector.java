package com.vortexmesh.sidecar.telemetry;

import com.vortexmesh.common.event.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryCollector {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BlockingQueue<TelemetryEvent> eventQueue = new LinkedBlockingQueue<>(10000);
    private static final String TELEMETRY_TOPIC = "vortex.telemetry.events";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @jakarta.annotation.PostConstruct
    public void init() {
        // Batch flush every 5 seconds
        scheduler.scheduleAtFixedRate(this::flushEvents, 5, 5, TimeUnit.SECONDS);
    }

    public void collect(TelemetryEvent event) {
        if (!eventQueue.offer(event)) {
            log.warn("Telemetry queue full, dropping event: {}", event.getTraceId());
        }
    }

    private void flushEvents() {
        List<TelemetryEvent> batch = new ArrayList<>();
        eventQueue.drainTo(batch, 100);

        for (TelemetryEvent event : batch) {
            try {
                kafkaTemplate.send(TELEMETRY_TOPIC, event.getServiceId(), event);
            } catch (Exception e) {
                log.error("Failed to send telemetry event: {}", e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            log.debug("Flushed {} telemetry events", batch.size());
        }
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        flushEvents();
        scheduler.shutdown();
    }
}
