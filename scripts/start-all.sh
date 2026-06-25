#!/bin/bash
set -e

echo "🌀 Starting VortexMesh Platform..."

# Colors
GREEN='\033[0;32m'
NC='\033[0m'

start_service() {
    local service=$1
    local port=$2
    echo -e "${GREEN}Starting $service on port $port...${NC}"
    cd $service
    mvn spring-boot:run -Dspring-boot.run.profiles=local &
    cd ..
    sleep 5
}

# Start in dependency order
start_service "config-server" 8888
start_service "service-registry" 8761
start_service "auth-service" 8770
start_service "policy-engine" 8762
start_service "control-plane" 8750
start_service "telemetry-service" 8780
start_service "gateway-cluster" 8080

echo ""
echo -e "${GREEN}✅ VortexMesh Platform started successfully!${NC}"
echo ""
echo "Services:"
echo "  Gateway:          http://localhost:8080"
echo "  Service Registry: http://localhost:8761"
echo "  Control Plane:    http://localhost:8750"
echo "  Policy Engine:    http://localhost:8762"
echo "  Auth Service:     http://localhost:8770"
echo "  Telemetry:        http://localhost:8780"
echo "  Config Server:    http://localhost:8888"
echo ""
echo "Observability:"
echo "  Prometheus:       http://localhost:9090"
echo "  Grafana:          http://localhost:3000"
echo "  Jaeger:           http://localhost:16686"
