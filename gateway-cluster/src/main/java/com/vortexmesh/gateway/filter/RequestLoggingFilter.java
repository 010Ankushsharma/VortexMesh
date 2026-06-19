package com.vortexmesh.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long startTime = System.currentTimeMillis();

        exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .header("X-Request-Start", String.valueOf(startTime));

        log.info("[{}] → {} {} from {}", 
            traceId,
            exchange.getRequest().getMethod(),
            exchange.getRequest().getURI().getPath(),
            exchange.getRequest().getRemoteAddress());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] ← {} {} - {}ms", 
                traceId,
                exchange.getResponse().getStatusCode(),
                exchange.getRequest().getURI().getPath(),
                duration);
        }));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
