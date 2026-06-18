package com.vortexmesh.controlplane.controller;

import com.vortexmesh.common.dto.RouteDTO;
import com.vortexmesh.controlplane.service.ControlPlaneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/control-plane")
@RequiredArgsConstructor
public class ControlPlaneController {

    private final ControlPlaneService controlPlaneService;

    @PostMapping("/routes")
    public ResponseEntity<RouteDTO> createRoute(@RequestBody RouteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(controlPlaneService.createRoute(dto));
    }

    @PutMapping("/routes/{routeId}")
    public ResponseEntity<RouteDTO> updateRoute(
            @PathVariable String routeId,
            @RequestBody RouteDTO dto) {
        return ResponseEntity.ok(controlPlaneService.updateRoute(routeId, dto));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteDTO>> getAllRoutes(
            @RequestParam(required = false) String tenantId) {
        if (tenantId != null) {
            return ResponseEntity.ok(controlPlaneService.getRoutesByTenant(tenantId));
        }
        return ResponseEntity.ok(controlPlaneService.getAllRoutes());
    }

    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<Void> deleteRoute(@PathVariable String routeId) {
        controlPlaneService.deleteRoute(routeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/routes/{routeId}/canary")
    public ResponseEntity<Map<String, String>> enableCanary(
            @PathVariable String routeId,
            @RequestParam int percentage,
            @RequestParam String canaryTarget) {
        controlPlaneService.toggleCanary(routeId, percentage, canaryTarget);
        return ResponseEntity.ok(Map.of(
            "status", "canary_enabled",
            "routeId", routeId,
            "percentage", String.valueOf(percentage),
            "target", canaryTarget
        ));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> triggerSync() {
        controlPlaneService.syncRoutesToGateways();
        return ResponseEntity.ok(Map.of("status", "sync_triggered"));
    }
}
