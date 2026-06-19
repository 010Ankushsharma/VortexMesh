package com.vortexmesh.gateway.routing;

import com.vortexmesh.common.dto.RouteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicRouteService {

    private final ApplicationEventPublisher publisher;
    private final List<RouteDTO> dynamicRoutes = new CopyOnWriteArrayList<>();

    public void addRoute(RouteDTO route) {
        dynamicRoutes.add(route);
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Dynamic route added: {} -> {}", route.getPath(), route.getTargetService());
    }

    public void removeRoute(String routeId) {
        dynamicRoutes.removeIf(r -> r.getRouteId().equals(routeId));
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Dynamic route removed: {}", routeId);
    }

    public void updateRoute(RouteDTO route) {
        removeRoute(route.getRouteId());
        addRoute(route);
    }

    public List<RouteDTO> getRoutes() {
        return List.copyOf(dynamicRoutes);
    }

    public void refreshRoutes(List<RouteDTO> routes) {
        dynamicRoutes.clear();
        dynamicRoutes.addAll(routes);
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Routes refreshed: {} routes loaded", routes.size());
    }
}
