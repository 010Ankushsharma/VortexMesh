package com.vortexmesh.policy.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "traffic_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String serviceId;
    private String tenantId;

    @Enumerated(EnumType.STRING)
    private RuleType type;

    @Column(columnDefinition = "TEXT")
    private String conditionExpression; // SpEL or JSON-based condition

    @Column(columnDefinition = "TEXT")
    private String actionPayload; // JSON action definition

    private int priority;
    private boolean enabled;
    private Instant createdAt;
    private Instant expiresAt;

    public enum RuleType {
        RATE_LIMIT, CIRCUIT_BREAKER, RETRY, ROUTING, 
        HEADER_INJECTION, REQUEST_TRANSFORM, RESPONSE_TRANSFORM,
        ACCESS_CONTROL, THROTTLE
    }
}
