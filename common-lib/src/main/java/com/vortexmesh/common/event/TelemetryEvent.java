package com.vortexmesh.common.event;

import lombok.*;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryEvent {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceId;
    private String instanceId;
    private String operation;
    private String method;
    private String path;
    private int statusCode;
    private long durationMs;
    private Instant timestamp;
    private Map<String, String> tags;
    private EventType type;

    public enum EventType {
        REQUEST, RESPONSE, ERROR, CIRCUIT_BREAKER_OPEN, 
        RATE_LIMITED, RETRY, TIMEOUT
    }
}
