package com.vortexmesh.common.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDTO {
    private String policyId;
    private String serviceId;
    private String tenantId;
    private RateLimitConfig rateLimit;
    private CircuitBreakerConfig circuitBreaker;
    private RetryConfig retry;
    private AuthConfig auth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        private int requestsPerSecond;
        private int burstCapacity;
        private String algorithm; // TOKEN_BUCKET, SLIDING_WINDOW
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitBreakerConfig {
        private int failureRateThreshold;
        private int slowCallRateThreshold;
        private int waitDurationInOpenState;
        private int slidingWindowSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfig {
        private int maxAttempts;
        private long waitDuration;
        private double exponentialBackoffMultiplier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthConfig {
        private boolean jwtEnabled;
        private boolean apiKeyEnabled;
        private boolean mtlsEnabled;
        private String[] requiredRoles;
    }
}
