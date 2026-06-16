package com.vortexmesh.common.exception;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String serviceId) {
        super("Service not found: " + serviceId);
    }
}
