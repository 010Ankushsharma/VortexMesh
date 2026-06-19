package com.vortexmesh.gateway.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class CanaryRoutingFilter implements GlobalFilter, Ordered {

    private final Random random = new Random();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String canaryHeader = exchange.getRequest().getHeaders().getFirst("X-Canary");
        
        if ("true".equalsIgnoreCase(canaryHeader)) {
            // Force canary route
            exchange.getAttributes().put("canary", true);
            log.debug("Request routed to canary (header-based)");
        } else {
            // Percentage-based canary (configurable, default 10%)
            int canaryPercentage = getCanaryPercentage(exchange);
            if (random.nextInt(100) < canaryPercentage) {
                exchange.getAttributes().put("canary", true);
                log.debug("Request routed to canary (percentage-based: {}%)", canaryPercentage);
            }
        }

        return chain.filter(exchange);
    }

    private int getCanaryPercentage(ServerWebExchange exchange) {
        // TODO: Fetch from policy engine
        return 10;
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
