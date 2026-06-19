package com.vortexmesh.gateway.loadbalancer;

import com.vortexmesh.common.dto.ServiceInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class WeightedLoadBalancer {

    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public ServiceInstanceDTO selectInstance(List<ServiceInstanceDTO> instances, String strategy) {
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("No available instances");
        }

        return switch (strategy.toUpperCase()) {
            case "ROUND_ROBIN" -> roundRobin(instances);
            case "WEIGHTED" -> weightedRandom(instances);
            case "LEAST_CONNECTIONS" -> leastConnections(instances);
            case "HEALTH_BASED" -> healthBased(instances);
            default -> roundRobin(instances);
        };
    }

    private ServiceInstanceDTO roundRobin(List<ServiceInstanceDTO> instances) {
        String serviceId = instances.get(0).getServiceId();
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(serviceId, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement()) % instances.size();
        return instances.get(index);
    }

    private ServiceInstanceDTO weightedRandom(List<ServiceInstanceDTO> instances) {
        int totalWeight = instances.stream()
                .mapToInt(i -> i.getHealthScore() > 0 ? i.getHealthScore() : 1)
                .sum();

        int randomValue = random.nextInt(totalWeight);
        int cumulative = 0;

        for (ServiceInstanceDTO instance : instances) {
            cumulative += instance.getHealthScore() > 0 ? instance.getHealthScore() : 1;
            if (randomValue < cumulative) {
                return instance;
            }
        }
        return instances.get(0);
    }

    private ServiceInstanceDTO leastConnections(List<ServiceInstanceDTO> instances) {
        // Simplified: use health score as proxy for load
        return instances.stream()
                .max(Comparator.comparingInt(ServiceInstanceDTO::getHealthScore))
                .orElse(instances.get(0));
    }

    private ServiceInstanceDTO healthBased(List<ServiceInstanceDTO> instances) {
        return instances.stream()
                .filter(i -> i.getHealthScore() >= 50)
                .max(Comparator.comparingInt(ServiceInstanceDTO::getHealthScore))
                .orElse(instances.get(0));
    }
}
