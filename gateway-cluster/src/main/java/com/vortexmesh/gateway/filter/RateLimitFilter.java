package com.vortexmesh.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final int DEFAULT_RPS = 100;
    private static final String RATE_LIMIT_PREFIX = "vortex:ratelimit:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = resolveClientId(exchange);
        String key = RATE_LIMIT_PREFIX + clientId + ":" + currentSecond();

        ReactiveValueOperations<String, String> ops = redisTemplate.opsForValue();

        return ops.increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        redisTemplate.expire(key, Duration.ofSeconds(2)).subscribe();
                    }
                    if (count > DEFAULT_RPS) {
                        log.warn("Rate limit exceeded for client: {} (count: {})", clientId, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(DEFAULT_RPS));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                        exchange.getResponse().getHeaders().add("Retry-After", "1");
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(DEFAULT_RPS));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
                        String.valueOf(DEFAULT_RPS - count));
                    return chain.filter(exchange);
                });
    }

    private String resolveClientId(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey != null) return apiKey;

        String userId = exchange.getRequest().getHeaders().getFirst("X-Authenticated-User");
        if (userId != null) return userId;

        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private long currentSecond() {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
