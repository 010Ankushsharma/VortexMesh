package com.vortexmesh.common.dto;

import lombok.*;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {
    private String routeId;
    private String path;
    private String targetService;
    private String method;
    private RoutingStrategy strategy;
    private Map<String, Integer> weightedTargets;
    private List<String> headers;
    private boolean authRequired;
    private boolean rateLimited;
    private String tenantId;
    private int priority;
    private boolean enabled;

    public enum RoutingStrategy {
        ROUND_ROBIN, WEIGHTED, CANARY, BLUE_GREEN, HEADER_BASED, PATH_BASED
    }
}
