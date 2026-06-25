package com.vortexmesh.registry.controller;

import com.vortexmesh.common.dto.ServiceInstanceDTO;
import com.vortexmesh.registry.service.RegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final RegistryService registryService;

    @PostMapping("/register")
    public ResponseEntity<ServiceInstanceDTO> register(@RequestBody ServiceInstanceDTO dto) {
        ServiceInstanceDTO registered = registryService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @PostMapping("/heartbeat/{instanceId}")
    public ResponseEntity<ServiceInstanceDTO> heartbeat(@PathVariable String instanceId) {
        ServiceInstanceDTO updated = registryService.heartbeat(instanceId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceInstanceDTO>> getAllServices() {
        return ResponseEntity.ok(registryService.getAllServices());
    }

    @GetMapping("/services/{serviceId}")
    public ResponseEntity<List<ServiceInstanceDTO>> getServiceInstances(@PathVariable String serviceId) {
        return ResponseEntity.ok(registryService.getServiceInstances(serviceId));
    }

    @DeleteMapping("/services/{instanceId}")
    public ResponseEntity<Void> deregister(@PathVariable String instanceId) {
        registryService.deregister(instanceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health-score/{instanceId}")
    public ResponseEntity<Map<String, Object>> getHealthScore(@PathVariable String instanceId) {
        int score = registryService.computeHealthScore(instanceId);
        return ResponseEntity.ok(Map.of("instanceId", instanceId, "healthScore", score));
    }
}
