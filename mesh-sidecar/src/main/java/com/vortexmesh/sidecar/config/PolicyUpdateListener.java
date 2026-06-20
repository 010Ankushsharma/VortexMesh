package com.vortexmesh.sidecar.config;

import com.vortexmesh.common.event.PolicyUpdateEvent;
import com.vortexmesh.sidecar.resilience.ResilienceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyUpdateListener {

    private final ResilienceManager resilienceManager;

    @KafkaListener(topics = "vortex.policy.updates", groupId = "${spring.application.name}")
    public void onPolicyUpdate(PolicyUpdateEvent event) {
        log.info("Received policy update for service: {} (action: {})", 
            event.getServiceId(), event.getAction());

        // Recreate resilience components with new config
        if (event.getPolicy() != null && event.getPolicy().getCircuitBreaker() != null) {
            var cb = event.getPolicy().getCircuitBreaker();
            resilienceManager.getOrCreateCircuitBreaker(
                event.getServiceId(),
                cb.getFailureRateThreshold(),
                cb.getWaitDurationInOpenState()
            );
        }
    }
}
