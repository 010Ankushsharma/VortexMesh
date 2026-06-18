package com.vortexmesh.controlplane.service;

import com.vortexmesh.common.dto.RouteDTO;
import com.vortexmesh.common.dto.ServiceInstanceDTO;
import com.vortexmesh.controlplane.model.Route;
import com.vortexmesh.controlplane.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ControlPlaneService {

    private final RouteRepository routeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ROUTE_EVENTS_TOPIC = "vortex.route.updates";
    private static final String ROUTE_CACHE_PREFIX = "vortex:routes:";

    @Transactional
    public RouteDTO createRoute(RouteDTO dto) {
        Route route = Route.builder()
                .routeId(dto.getRouteId() != null ? dto.getRouteId() : UUID.randomUUID().toString())
                .path(dto.getPath())
                .targetService(dto.getTargetService())
                .method(dto.getMethod())
                .strategy(Route.RoutingStrategy.valueOf(dto.getStrategy().name()))
                .weightedTargets(dto.getWeightedTargets())
                .authRequired(dto.isAuthRequired())
                .rateLimited(dto.isRateLimited())
                .tenantId(dto.getTenantId())
                .priority(dto.getPriority())
                .build();

        Route saved = routeRepository.save(route);
        cacheRoute(saved);
        broadcastRouteUpdate("CREATE", saved);

        log.info("Route created: {} -> {}", saved.getPath(), saved.getTargetService());
        return toDTO(saved);
    }

    @Transactional
    public RouteDTO updateRoute(String routeId, RouteDTO dto) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));

        route.setPath(dto.getPath());
        route.setTargetService(dto.getTargetService());
        route.setStrategy(Route.RoutingStrategy.valueOf(dto.getStrategy().name()));
        route.setWeightedTargets(dto.getWeightedTargets());
        route.setAuthRequired(dto.isAuthRequired());
        route.setRateLimited(dto.isRateLimited());
        route.setPriority(dto.getPriority());
        route.setVersion(route.getVersion() + 1);

        Route saved = routeRepository.save(route);
        cacheRoute(saved);
        broadcastRouteUpdate("UPDATE", saved);

        return toDTO(saved);
    }

    public List<RouteDTO> getAllRoutes() {
        return routeRepository.findByEnabledOrderByPriorityDesc(true).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<RouteDTO> getRoutesByTenant(String tenantId) {
        return routeRepository.findByTenantIdAndEnabled(tenantId, true).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void deleteRoute(String routeId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
        routeRepository.delete(route);
        redisTemplate.delete(ROUTE_CACHE_PREFIX + routeId);
        broadcastRouteUpdate("DELETE", route);
        log.info("Route deleted: {}", routeId);
    }

    @Transactional
    public void toggleCanary(String routeId, int canaryPercentage, String canaryTarget) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));

        route.setStrategy(Route.RoutingStrategy.CANARY);
        Map<String, Integer> weights = new HashMap<>();
        weights.put(route.getTargetService(), 100 - canaryPercentage);
        weights.put(canaryTarget, canaryPercentage);
        route.setWeightedTargets(weights);

        Route saved = routeRepository.save(route);
        cacheRoute(saved);
        broadcastRouteUpdate("CANARY_UPDATE", saved);

        log.info("Canary enabled for route {}: {}% -> {}", routeId, canaryPercentage, canaryTarget);
    }

    @Scheduled(fixedRate = 30000)
    public void syncRoutesToGateways() {
        List<Route> activeRoutes = routeRepository.findByEnabledOrderByPriorityDesc(true);
        kafkaTemplate.send(ROUTE_EVENTS_TOPIC, "SYNC", Map.of(
            "action", "FULL_SYNC",
            "routes", activeRoutes.stream().map(this::toDTO).toList(),
            "timestamp", System.currentTimeMillis()
        ));
        log.debug("Route sync broadcast: {} routes", activeRoutes.size());
    }

    private void cacheRoute(Route route) {
        redisTemplate.opsForValue().set(
            ROUTE_CACHE_PREFIX + route.getRouteId(), route, Duration.ofMinutes(10));
    }

    private void broadcastRouteUpdate(String action, Route route) {
        kafkaTemplate.send(ROUTE_EVENTS_TOPIC, route.getRouteId(), Map.of(
            "action", action,
            "route", toDTO(route),
            "timestamp", System.currentTimeMillis()
        ));
    }

    private RouteDTO toDTO(Route route) {
        return RouteDTO.builder()
                .routeId(route.getRouteId())
                .path(route.getPath())
                .targetService(route.getTargetService())
                .method(route.getMethod())
                .strategy(RouteDTO.RoutingStrategy.valueOf(route.getStrategy().name()))
                .weightedTargets(route.getWeightedTargets())
                .authRequired(route.isAuthRequired())
                .rateLimited(route.isRateLimited())
                .tenantId(route.getTenantId())
                .priority(route.getPriority())
                .enabled(route.isEnabled())
                .build();
    }
}
