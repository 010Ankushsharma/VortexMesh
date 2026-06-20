package com.vortexmesh.sidecar.proxy;

import com.vortexmesh.sidecar.resilience.ResilienceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceProxy {

    private final ResilienceManager resilienceManager;
    private final RestTemplate restTemplate = new RestTemplate();

    public <T> ResponseEntity<T> forward(String targetService, String path, 
            HttpMethod method, Object body, Class<T> responseType, Map<String, String> headers) {

        return resilienceManager.executeWithResilience(targetService, () -> {
            String url = resolveServiceUrl(targetService) + path;

            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::add);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> entity = new HttpEntity<>(body, httpHeaders);

            log.debug("Proxying {} {} -> {}", method, path, url);
            return restTemplate.exchange(url, method, entity, responseType);
        });
    }

    private String resolveServiceUrl(String serviceId) {
        // TODO: Integrate with service registry for actual resolution
        return "http://" + serviceId + ":8080";
    }
}
