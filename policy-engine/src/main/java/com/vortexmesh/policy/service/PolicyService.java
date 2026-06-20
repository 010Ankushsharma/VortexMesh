package com.vortexmesh.policy.service;

import com.vortexmesh.common.dto.PolicyDTO;
import com.vortexmesh.common.event.PolicyUpdateEvent;
import com.vortexmesh.policy.model.Policy;
import com.vortexmesh.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String POLICY_CACHE_PREFIX = "vortex:policy:";
    private static final String POLICY_EVENTS_TOPIC = "vortex.policy.updates";

    @Transactional
    public PolicyDTO createPolicy(PolicyDTO dto) {
        Policy policy = mapToEntity(dto);
        Policy saved = policyRepository.save(policy);

        // Cache policy
        cachePolicy(saved);

        // Broadcast policy update
        publishPolicyEvent(saved, PolicyUpdateEvent.ActionType.CREATE);

        log.info("Policy created for service: {} (tenant: {})", dto.getServiceId(), dto.getTenantId());
        return mapToDTO(saved);
    }

    @Transactional
    public PolicyDTO updatePolicy(String policyId, PolicyDTO dto) {
        Policy existing = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

        updateEntity(existing, dto);
        Policy saved = policyRepository.save(existing);

        cachePolicy(saved);
        publishPolicyEvent(saved, PolicyUpdateEvent.ActionType.UPDATE);

        log.info("Policy updated: {} for service: {}", policyId, dto.getServiceId());
        return mapToDTO(saved);
    }

    public PolicyDTO getPolicy(String serviceId, String tenantId) {
        // Try cache first
        String cacheKey = POLICY_CACHE_PREFIX + serviceId + ":" + (tenantId != null ? tenantId : "default");
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Policy policy) {
            return mapToDTO(policy);
        }

        // Fallback to DB
        Policy policy = tenantId != null ?
                policyRepository.findByServiceIdAndTenantId(serviceId, tenantId)
                        .orElse(policyRepository.findByServiceIdAndTenantIdIsNull(serviceId).orElse(null)) :
                policyRepository.findByServiceIdAndTenantIdIsNull(serviceId).orElse(null);

        if (policy != null) {
            cachePolicy(policy);
            return mapToDTO(policy);
        }
        return getDefaultPolicy(serviceId);
    }

    public List<PolicyDTO> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void deletePolicy(String policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));
        policyRepository.delete(policy);

        String cacheKey = POLICY_CACHE_PREFIX + policy.getServiceId() + ":" + 
            (policy.getTenantId() != null ? policy.getTenantId() : "default");
        redisTemplate.delete(cacheKey);

        publishPolicyEvent(policy, PolicyUpdateEvent.ActionType.DELETE);
        log.info("Policy deleted: {}", policyId);
    }

    private PolicyDTO getDefaultPolicy(String serviceId) {
        return PolicyDTO.builder()
                .serviceId(serviceId)
                .rateLimit(PolicyDTO.RateLimitConfig.builder()
                        .requestsPerSecond(100)
                        .burstCapacity(150)
                        .algorithm("TOKEN_BUCKET")
                        .build())
                .circuitBreaker(PolicyDTO.CircuitBreakerConfig.builder()
                        .failureRateThreshold(50)
                        .slowCallRateThreshold(80)
                        .waitDurationInOpenState(10000)
                        .slidingWindowSize(10)
                        .build())
                .retry(PolicyDTO.RetryConfig.builder()
                        .maxAttempts(3)
                        .waitDuration(500)
                        .exponentialBackoffMultiplier(2.0)
                        .build())
                .auth(PolicyDTO.AuthConfig.builder()
                        .jwtEnabled(true)
                        .apiKeyEnabled(true)
                        .mtlsEnabled(false)
                        .build())
                .build();
    }

    private void cachePolicy(Policy policy) {
        String cacheKey = POLICY_CACHE_PREFIX + policy.getServiceId() + ":" + 
            (policy.getTenantId() != null ? policy.getTenantId() : "default");
        redisTemplate.opsForValue().set(cacheKey, policy, Duration.ofMinutes(10));
    }

    private void publishPolicyEvent(Policy policy, PolicyUpdateEvent.ActionType action) {
        PolicyUpdateEvent event = PolicyUpdateEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .serviceId(policy.getServiceId())
                .policy(mapToDTO(policy))
                .action(action)
                .timestamp(Instant.now())
                .build();
        kafkaTemplate.send(POLICY_EVENTS_TOPIC, policy.getServiceId(), event);
    }

    private Policy mapToEntity(PolicyDTO dto) {
        return Policy.builder()
                .serviceId(dto.getServiceId())
                .tenantId(dto.getTenantId())
                .rateLimitRps(dto.getRateLimit() != null ? dto.getRateLimit().getRequestsPerSecond() : 100)
                .rateLimitBurst(dto.getRateLimit() != null ? dto.getRateLimit().getBurstCapacity() : 150)
                .rateLimitAlgorithm(dto.getRateLimit() != null ? dto.getRateLimit().getAlgorithm() : "TOKEN_BUCKET")
                .cbFailureRateThreshold(dto.getCircuitBreaker() != null ? dto.getCircuitBreaker().getFailureRateThreshold() : 50)
                .cbSlowCallRateThreshold(dto.getCircuitBreaker() != null ? dto.getCircuitBreaker().getSlowCallRateThreshold() : 80)
                .cbWaitDurationMs(dto.getCircuitBreaker() != null ? dto.getCircuitBreaker().getWaitDurationInOpenState() : 10000)
                .cbSlidingWindowSize(dto.getCircuitBreaker() != null ? dto.getCircuitBreaker().getSlidingWindowSize() : 10)
                .retryMaxAttempts(dto.getRetry() != null ? dto.getRetry().getMaxAttempts() : 3)
                .retryWaitDurationMs(dto.getRetry() != null ? dto.getRetry().getWaitDuration() : 500)
                .retryBackoffMultiplier(dto.getRetry() != null ? dto.getRetry().getExponentialBackoffMultiplier() : 2.0)
                .authJwtEnabled(dto.getAuth() != null && dto.getAuth().isJwtEnabled())
                .authApiKeyEnabled(dto.getAuth() != null && dto.getAuth().isApiKeyEnabled())
                .authMtlsEnabled(dto.getAuth() != null && dto.getAuth().isMtlsEnabled())
                .build();
    }

    private void updateEntity(Policy entity, PolicyDTO dto) {
        if (dto.getRateLimit() != null) {
            entity.setRateLimitRps(dto.getRateLimit().getRequestsPerSecond());
            entity.setRateLimitBurst(dto.getRateLimit().getBurstCapacity());
        }
        if (dto.getCircuitBreaker() != null) {
            entity.setCbFailureRateThreshold(dto.getCircuitBreaker().getFailureRateThreshold());
            entity.setCbSlidingWindowSize(dto.getCircuitBreaker().getSlidingWindowSize());
        }
        if (dto.getRetry() != null) {
            entity.setRetryMaxAttempts(dto.getRetry().getMaxAttempts());
        }
    }

    private PolicyDTO mapToDTO(Policy policy) {
        return PolicyDTO.builder()
                .policyId(policy.getId())
                .serviceId(policy.getServiceId())
                .tenantId(policy.getTenantId())
                .rateLimit(PolicyDTO.RateLimitConfig.builder()
                        .requestsPerSecond(policy.getRateLimitRps())
                        .burstCapacity(policy.getRateLimitBurst())
                        .algorithm(policy.getRateLimitAlgorithm())
                        .build())
                .circuitBreaker(PolicyDTO.CircuitBreakerConfig.builder()
                        .failureRateThreshold(policy.getCbFailureRateThreshold())
                        .slowCallRateThreshold(policy.getCbSlowCallRateThreshold())
                        .waitDurationInOpenState(policy.getCbWaitDurationMs())
                        .slidingWindowSize(policy.getCbSlidingWindowSize())
                        .build())
                .retry(PolicyDTO.RetryConfig.builder()
                        .maxAttempts(policy.getRetryMaxAttempts())
                        .waitDuration(policy.getRetryWaitDurationMs())
                        .exponentialBackoffMultiplier(policy.getRetryBackoffMultiplier())
                        .build())
                .auth(PolicyDTO.AuthConfig.builder()
                        .jwtEnabled(policy.isAuthJwtEnabled())
                        .apiKeyEnabled(policy.isAuthApiKeyEnabled())
                        .mtlsEnabled(policy.isAuthMtlsEnabled())
                        .build())
                .build();
    }
}
