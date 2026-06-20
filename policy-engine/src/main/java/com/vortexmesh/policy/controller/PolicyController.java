package com.vortexmesh.policy.controller;

import com.vortexmesh.common.dto.PolicyDTO;
import com.vortexmesh.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    public ResponseEntity<PolicyDTO> createPolicy(@RequestBody PolicyDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(dto));
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<PolicyDTO> updatePolicy(
            @PathVariable String policyId,
            @RequestBody PolicyDTO dto) {
        return ResponseEntity.ok(policyService.updatePolicy(policyId, dto));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<PolicyDTO> getPolicy(
            @PathVariable String serviceId,
            @RequestParam(required = false) String tenantId) {
        return ResponseEntity.ok(policyService.getPolicy(serviceId, tenantId));
    }

    @GetMapping
    public ResponseEntity<List<PolicyDTO>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String policyId) {
        policyService.deletePolicy(policyId);
        return ResponseEntity.noContent().build();
    }
}
