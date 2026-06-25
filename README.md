# 🌀 VortexMesh

## Distributed API Gateway & Service Mesh Platform

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## 📋 Overview

**VortexMesh** is an enterprise-grade, cloud-native distributed system that combines API Gateway and Service Mesh capabilities into a unified platform. Inspired by Istio, Envoy, Kong, and Spring Cloud Gateway.

## 🎯 Problem Statement

Modern microservice architectures face challenges in:
- **Traffic Management**: Routing, load balancing, and canary deployments across services
- **Resilience**: Circuit breaking, retries, and fault tolerance at scale
- **Observability**: Distributed tracing, metrics collection, and service dependency mapping
- **Security**: Consistent authentication/authorization across all services
- **Configuration**: Dynamic policy updates without service restarts

VortexMesh solves these by providing a centralized control plane with distributed data plane execution.

## 🏗️ Architecture

```
                        +----------------------+
                        |   Admin Dashboard    |
                        +----------+-----------+
                                   |
                                   v
+--------------------------------------------------------+
|                CONTROL PLANE (Spring Boot)              |
|  Service Registry | Policy Engine | Route Manager      |
+--------------------------------------------------------+
                       |
                       v
+--------------------------------------------------------+
|              DISTRIBUTED GATEWAY CLUSTER               |
|  Gateway Node 1  |  Gateway Node 2  |  Gateway Node N  |
+--------------------------------------------------------+
                       |
                       v
+--------------------------------------------------------+
|              SERVICE MESH SIDECARS                      |
|  Interceptor | Circuit Breaker | Telemetry | Proxy     |
+--------------------------------------------------------+
                       |
                       v
+--------------------------------------------------------+
|               BUSINESS MICROSERVICES                   |
|  User | Order | Payment | Inventory                    |
+--------------------------------------------------------+
```

## 🧱 Tech Stack

| Layer | Technology |
|-------|-----------|
| Core | Java 21, Spring Boot 3.x, Spring WebFlux |
| Gateway | Spring Cloud Gateway, Reactor Netty |
| Discovery | Consul / Eureka |
| Config | Spring Cloud Config Server |
| Messaging | Apache Kafka |
| Database | PostgreSQL |
| Cache | Redis |
| Security | Spring Security, JWT, API Keys |
| Resilience | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| Observability | Micrometer, Prometheus, Grafana, Jaeger |
| Deployment | Docker, Kubernetes |

## 📦 Module Breakdown

| Module | Port | Description |
|--------|------|-------------|
| `config-server` | 8888 | Centralized configuration |
| `service-registry` | 8761 | Service registration & discovery |
| `control-plane` | 8750 | Route management & orchestration |
| `policy-engine` | 8762 | Traffic policies & rules |
| `auth-service` | 8770 | JWT/API Key authentication |
| `gateway-cluster` | 8080 | API Gateway (edge routing) |
| `mesh-sidecar` | 8090 | Per-service sidecar proxy |
| `telemetry-service` | 8780 | Metrics ingestion & analytics |
| `admin-dashboard` | 8790 | Management UI |

## 🔌 API Documentation

### Service Registry
```
POST   /api/v1/registry/register          - Register service instance
POST   /api/v1/registry/heartbeat/{id}    - Send heartbeat
GET    /api/v1/registry/services           - List all services
GET    /api/v1/registry/services/{id}      - Get service instances
DELETE /api/v1/registry/services/{id}      - Deregister instance
```

### Control Plane
```
POST   /api/v1/control-plane/routes        - Create route
PUT    /api/v1/control-plane/routes/{id}   - Update route
GET    /api/v1/control-plane/routes        - List routes
DELETE /api/v1/control-plane/routes/{id}   - Delete route
POST   /api/v1/control-plane/routes/{id}/canary - Enable canary
```

### Policy Engine
```
POST   /api/v1/policies                    - Create policy
PUT    /api/v1/policies/{id}               - Update policy
GET    /api/v1/policies/{serviceId}        - Get policy
GET    /api/v1/policies                    - List all policies
DELETE /api/v1/policies/{id}               - Delete policy
```

### Auth Service
```
POST   /api/v1/auth/login                  - Authenticate
POST   /api/v1/auth/register               - Register user
POST   /api/v1/auth/api-keys               - Generate API key
POST   /api/v1/auth/validate               - Validate token
```

## 🔒 Security Model

1. **JWT Authentication** - Bearer tokens for user authentication
2. **API Keys** - Service-to-service authentication (`X-API-Key` header)
3. **mTLS Simulation** - Logical layer for inter-service trust
4. **RBAC** - Role-based access control per tenant
5. **Rate Limiting** - Redis-based token bucket algorithm

## 📊 Observability Setup

- **Metrics**: Micrometer → Prometheus (port 9090) → Grafana (port 3000)
- **Tracing**: OpenTelemetry → Jaeger (port 16686)
- **Logging**: Structured JSON logs with trace correlation
- **Health**: Spring Boot Actuator endpoints per service

## 🚀 Local Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Quick Start
```bash
# Clone repository
git clone https://github.com/your-org/vortexmesh.git
cd vortexmesh

# Start infrastructure
docker-compose up -d postgres redis kafka prometheus grafana jaeger

# Build all modules
mvn clean install -DskipTests

# Start services (in order)
./scripts/start-all.sh
```

### Individual Service
```bash
cd service-registry
mvn spring-boot:run
```

## 🐳 Docker Setup

```bash
# Build and run everything
docker-compose up --build

# Scale gateway
docker-compose up --scale gateway=3
```

## ☸️ Kubernetes Deployment

```bash
# Create namespace
kubectl apply -f kubernetes/base/configmap.yml

# Deploy services
kubectl apply -f kubernetes/base/

# Check status
kubectl get pods -n vortexmesh

# Access gateway
kubectl port-forward svc/vortex-gateway 8080:80 -n vortexmesh
```

## 🔄 CI/CD Pipeline

GitHub Actions workflow:
1. **Build** → Compile all modules
2. **Test** → Unit + Integration tests
3. **Analyze** → Static analysis (SpotBugs)
4. **Docker** → Build & push images (parallel per service)
5. **Deploy** → Rolling update to Kubernetes

## 🔮 Future Enhancements

- [ ] WebAssembly (Wasm) plugin support for custom filters
- [ ] GraphQL gateway support
- [ ] gRPC service mesh support
- [ ] AI-driven auto-scaling based on traffic patterns
- [ ] Multi-cluster federation
- [ ] Service mesh data plane in Rust (performance layer)
- [ ] Chaos engineering automation (Litmus integration)
- [ ] Traffic replay from production to staging

## 📄 License

Apache License 2.0
