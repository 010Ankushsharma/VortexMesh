package com.vortexmesh.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        return ResponseEntity.ok(Map.of(
            "services", getServiceCount(),
            "routes", getRouteCount(),
            "requestsPerSecond", getAverageRps(),
            "status", "HEALTHY"
        ));
    }

    @GetMapping("/services")
    public ResponseEntity<?> getServices() {
        return restTemplate.getForEntity(
            "http://localhost:8761/api/v1/registry/services", Object.class);
    }

    @GetMapping("/routes")
    public ResponseEntity<?> getRoutes() {
        return restTemplate.getForEntity(
            "http://localhost:8750/api/v1/control-plane/routes", Object.class);
    }

    @GetMapping("/policies")
    public ResponseEntity<?> getPolicies() {
        return restTemplate.getForEntity(
            "http://localhost:8762/api/v1/policies", Object.class);
    }

    private int getServiceCount() { return 4; }
    private int getRouteCount() { return 10; }
    private double getAverageRps() { return 1500.0; }
}
