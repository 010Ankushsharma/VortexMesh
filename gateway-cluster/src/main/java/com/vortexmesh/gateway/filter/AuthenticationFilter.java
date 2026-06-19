package com.vortexmesh.gateway.filter;

import com.vortexmesh.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/actuator/health",
        "/actuator/prometheus"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip auth for public paths
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // Check for API Key
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        if (apiKey != null && validateApiKey(apiKey)) {
            return chain.filter(exchange);
        }

        // Check for JWT Bearer token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.isTokenValid(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Add user info to headers for downstream services
        String subject = jwtTokenProvider.extractSubject(token);
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Authenticated-User", subject)
                .header("X-Trace-Id", exchange.getRequest().getId())
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean validateApiKey(String apiKey) {
        // TODO: Validate against Redis/DB stored API keys
        return apiKey.startsWith("vortex_");
    }

    @Override
    public int getOrder() {
        return -100; // High priority
    }
}
