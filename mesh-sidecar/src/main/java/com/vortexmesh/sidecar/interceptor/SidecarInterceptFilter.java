package com.vortexmesh.sidecar.interceptor;

import com.vortexmesh.common.event.TelemetryEvent;
import com.vortexmesh.sidecar.telemetry.TelemetryCollector;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SidecarInterceptFilter implements Filter {

    private final TelemetryCollector telemetryCollector;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        long startTime = System.currentTimeMillis();

        // Add trace context
        httpResponse.addHeader("X-Trace-Id", traceId);
        httpResponse.addHeader("X-Span-Id", spanId);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            TelemetryEvent event = TelemetryEvent.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .parentSpanId(httpRequest.getHeader("X-Parent-Span-Id"))
                    .operation(httpRequest.getMethod() + " " + httpRequest.getRequestURI())
                    .method(httpRequest.getMethod())
                    .path(httpRequest.getRequestURI())
                    .statusCode(httpResponse.getStatus())
                    .durationMs(duration)
                    .timestamp(Instant.now())
                    .type(httpResponse.getStatus() >= 400 ? 
                        TelemetryEvent.EventType.ERROR : TelemetryEvent.EventType.RESPONSE)
                    .tags(Map.of(
                        "method", httpRequest.getMethod(),
                        "path", httpRequest.getRequestURI(),
                        "status", String.valueOf(httpResponse.getStatus())
                    ))
                    .build();

            telemetryCollector.collect(event);

            if (duration > 1000) {
                log.warn("[SLOW] {} {} took {}ms", httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), duration);
            }
        }
    }
}
