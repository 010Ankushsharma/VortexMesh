package com.vortexmesh.policy.repository;

import com.vortexmesh.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {
    Optional<Policy> findByServiceIdAndTenantId(String serviceId, String tenantId);
    Optional<Policy> findByServiceIdAndTenantIdIsNull(String serviceId);
    List<Policy> findByServiceId(String serviceId);
    List<Policy> findByTenantId(String tenantId);
    List<Policy> findByEnabled(boolean enabled);
}
