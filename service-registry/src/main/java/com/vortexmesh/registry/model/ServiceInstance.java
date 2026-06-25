package com.vortexmesh.registry.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false, unique = true)
    private String instanceId;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    private String protocol;
    private String version;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    @ElementCollection
    @CollectionTable(name = "service_metadata")
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata;

    private Instant registeredAt;
    private Instant lastHeartbeat;
    private int healthScore;

    @PrePersist
    public void prePersist() {
        this.registeredAt = Instant.now();
        this.lastHeartbeat = Instant.now();
        this.status = ServiceStatus.UP;
        this.healthScore = 100;
    }

    public enum ServiceStatus {
        UP, DOWN, DEGRADED, STARTING, DRAINING
    }
}
