package com.vortexmesh.controlplane.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String routeId;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String targetService;

    private String method;

    @Enumerated(EnumType.STRING)
    private RoutingStrategy strategy;

    @ElementCollection
    @CollectionTable(name = "route_weights")
    @MapKeyColumn(name = "target")
    @Column(name = "weight")
    private Map<String, Integer> weightedTargets;

    private boolean authRequired;
    private boolean rateLimited;
    private String tenantId;
    private int priority;
    private boolean enabled;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;

    public enum RoutingStrategy {
        ROUND_ROBIN, WEIGHTED, CANARY, BLUE_GREEN, HEADER_BASED, PATH_BASED
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.enabled = true;
        this.version = 1;
    }
}