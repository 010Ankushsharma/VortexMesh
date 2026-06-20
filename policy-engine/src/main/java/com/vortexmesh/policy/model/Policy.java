package com.vortexmesh.policy.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String serviceId;

    private String tenantId;

    // Rate Limit
    private int rateLimitRps;
    private int rateLimitBurst;
    private String rateLimitAlgorithm;

    // Circuit Breaker
    private int cbFailureRateThreshold;
    private int cbSlowCallRateThreshold;
    private int cbWaitDurationMs;
    private int cbSlidingWindowSize;

    // Retry
    private int retryMaxAttempts;
    private long retryWaitDurationMs;
    private double retryBackoffMultiplier;

    // Auth
    private boolean authJwtEnabled;
    private boolean authApiKeyEnabled;
    private boolean authMtlsEnabled;

    // Routing
    private String routingStrategy;

    // Metadata
    private boolean enabled;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.enabled = true;
        this.version = 1;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        this.version++;
    }
}
