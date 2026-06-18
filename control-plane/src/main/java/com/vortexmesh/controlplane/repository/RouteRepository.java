package com.vortexmesh.controlplane.repository;

import com.vortexmesh.controlplane.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {
    Optional<Route> findByRouteId(String routeId);
    List<Route> findByEnabledOrderByPriorityDesc(boolean enabled);
    List<Route> findByTenantIdAndEnabled(String tenantId, boolean enabled);
    List<Route> findByTargetService(String targetService);
}
