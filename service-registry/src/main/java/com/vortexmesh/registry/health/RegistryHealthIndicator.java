package com.vortexmesh.registry.health;

import com.vortexmesh.registry.repository.ServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistryHealthIndicator implements HealthIndicator {

    private final ServiceInstanceRepository repository;

    @Override
    public Health health() {
        long totalServices = repository.count();
        long activeServices = repository.findAllActiveServiceIds().size();

        return Health.up()
                .withDetail("totalInstances", totalServices)
                .withDetail("activeServices", activeServices)
                .build();
    }
}
