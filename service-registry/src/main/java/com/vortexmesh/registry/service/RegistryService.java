package com.vortexmesh.registry.service;

import com.vortexmesh.common.dto.ServiceInstanceDTO;
import com.vortexmesh.common.exception.ServiceNotFoundException;
import com.vortexmesh.registry.model.ServiceInstance;
import com.vortexmesh.registry.repository.ServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistryService {

    private final ServiceInstanceRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REDIS_PREFIX = "vortex:registry:";
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);
    private static final String REGISTRY_EVENTS_TOPIC = "vortex.registry.events";

    @Transactional
    public ServiceInstanceDTO register(ServiceInstanceDTO dto) {
        String instanceId = dto.getInstanceId() != null ? 
            dto.getInstanceId() : UUID.randomUUID().toString();

        ServiceInstance instance = ServiceInstance.builder()
                .serviceId(dto.getServiceId())
                .instanceId(instanceId)
                .host(dto.getHost())
                .port(dto.getPort())
                .protocol(dto.getProtocol() != null ? dto.getProtocol() : "HTTP")
                .version(dto.getVersion() != null ? dto.getVersion() : "1.0.0")
                .metadata(dto.getMetadata())
                .build();

        ServiceInstance saved = repository.save(instance);

        // Cache in Redis for fast lookups
        String redisKey = REDIS_PREFIX + saved.getServiceId() + ":" + saved.getInstanceId();
        redisTemplate.opsForValue().set(redisKey, saved, Duration.ofMinutes(5));

        // Publish registration event
        kafkaTemplate.send(REGISTRY_EVENTS_TOPIC, saved.getServiceId(), 
            Map.of("action", "REGISTER", "instance", saved));

        log.info("Service registered: {} [{}] at {}:{}", 
            saved.getServiceId(), saved.getInstanceId(), saved.getHost(), saved.getPort());

        return toDTO(saved);
    }

    @Transactional
    public ServiceInstanceDTO heartbeat(String instanceId) {
        ServiceInstance instance = repository.findByInstanceId(instanceId)
                .orElseThrow(() -> new ServiceNotFoundException(instanceId));

        instance.setLastHeartbeat(Instant.now());
        instance.setStatus(ServiceInstance.ServiceStatus.UP);
        ServiceInstance saved = repository.save(instance);

        // Update Redis TTL
        String redisKey = REDIS_PREFIX + saved.getServiceId() + ":" + saved.getInstanceId();
        redisTemplate.opsForValue().set(redisKey, saved, Duration.ofMinutes(5));

        return toDTO(saved);
    }

    public List<ServiceInstanceDTO> getServiceInstances(String serviceId) {
        return repository.findByServiceId(serviceId).stream()
                .filter(i -> i.getStatus() == ServiceInstance.ServiceStatus.UP)
                .map(this::toDTO)
                .toList();
    }

    public List<ServiceInstanceDTO> getAllServices() {
        return repository.findByStatus(ServiceInstance.ServiceStatus.UP).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void deregister(String instanceId) {
        ServiceInstance instance = repository.findByInstanceId(instanceId)
                .orElseThrow(() -> new ServiceNotFoundException(instanceId));

        repository.deleteByInstanceId(instanceId);

        String redisKey = REDIS_PREFIX + instance.getServiceId() + ":" + instanceId;
        redisTemplate.delete(redisKey);

        kafkaTemplate.send(REGISTRY_EVENTS_TOPIC, instance.getServiceId(),
            Map.of("action", "DEREGISTER", "instanceId", instanceId));

        log.info("Service deregistered: {} [{}]", instance.getServiceId(), instanceId);
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void evictStaleInstances() {
        Instant threshold = Instant.now().minus(HEARTBEAT_TIMEOUT);
        List<ServiceInstance> stale = repository.findStaleInstances(threshold);

        for (ServiceInstance instance : stale) {
            instance.setStatus(ServiceInstance.ServiceStatus.DOWN);
            repository.save(instance);

            String redisKey = REDIS_PREFIX + instance.getServiceId() + ":" + instance.getInstanceId();
            redisTemplate.delete(redisKey);

            log.warn("Service marked DOWN (stale heartbeat): {} [{}]", 
                instance.getServiceId(), instance.getInstanceId());
        }
    }

    public int computeHealthScore(String instanceId) {
        ServiceInstance instance = repository.findByInstanceId(instanceId)
                .orElseThrow(() -> new ServiceNotFoundException(instanceId));

        long timeSinceHeartbeat = Duration.between(instance.getLastHeartbeat(), Instant.now()).toSeconds();
        int score = 100;

        if (timeSinceHeartbeat > 10) score -= 20;
        if (timeSinceHeartbeat > 20) score -= 30;
        if (instance.getStatus() == ServiceInstance.ServiceStatus.DEGRADED) score -= 25;
        if (instance.getStatus() == ServiceInstance.ServiceStatus.DOWN) score = 0;

        instance.setHealthScore(Math.max(0, score));
        repository.save(instance);
        return score;
    }

    private ServiceInstanceDTO toDTO(ServiceInstance instance) {
        return ServiceInstanceDTO.builder()
                .serviceId(instance.getServiceId())
                .instanceId(instance.getInstanceId())
                .host(instance.getHost())
                .port(instance.getPort())
                .protocol(instance.getProtocol())
                .version(instance.getVersion())
                .status(instance.getStatus().name())
                .metadata(instance.getMetadata())
                .registeredAt(instance.getRegisteredAt())
                .lastHeartbeat(instance.getLastHeartbeat())
                .healthScore(instance.getHealthScore())
                .build();
    }

    private java.util.Map<String, Object> Map_of(String... args) {
        return java.util.Map.of();
    }
}
