package com.vortexmesh.registry.repository;

import com.vortexmesh.registry.model.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, String> {

    List<ServiceInstance> findByServiceId(String serviceId);

    Optional<ServiceInstance> findByInstanceId(String instanceId);

    List<ServiceInstance> findByStatus(ServiceInstance.ServiceStatus status);

    @Query("SELECT s FROM ServiceInstance s WHERE s.lastHeartbeat < :threshold")
    List<ServiceInstance> findStaleInstances(Instant threshold);

    void deleteByInstanceId(String instanceId);

    @Query("SELECT DISTINCT s.serviceId FROM ServiceInstance s WHERE s.status = 'UP'")
    List<String> findAllActiveServiceIds();
}
