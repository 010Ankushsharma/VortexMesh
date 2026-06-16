package com.vortexmesh.common.dto;

import lombok.*;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstanceDTO {
    private String serviceId;
    private String instanceId;
    private String host;
    private int port;
    private String protocol;
    private String version;
    private String status;
    private Map<String, String> metadata;
    private Instant registeredAt;
    private Instant lastHeartbeat;
    private int healthScore;
}
