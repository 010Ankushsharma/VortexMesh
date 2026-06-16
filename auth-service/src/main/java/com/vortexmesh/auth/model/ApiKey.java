package com.vortexmesh.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String keyValue;

    private String name;
    private String userId;
    private String tenantId;

    @Column(columnDefinition = "TEXT")
    private String permissions; // JSON array of permissions

    private boolean enabled;
    private Instant createdAt;
    private Instant expiresAt;
    private long requestCount;
}
