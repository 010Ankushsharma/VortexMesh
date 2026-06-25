#!/bin/bash
echo "🛑 Stopping VortexMesh Platform..."
pkill -f "spring-boot:run" || true
echo "✅ All services stopped."
