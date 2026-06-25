package com.vortexmesh.telemetry.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "metrics", indexes = {
    @Index(name = "idx_metrics_service", columnList = "serviceId"),
    @Index(name = "idx_metrics_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String traceId;
    private String serviceId;
    private String operation;
    private String method;
    private String path;
    private int statusCode;
    private long durationMs;
    private Instant timestamp;
    private String eventType;
}
