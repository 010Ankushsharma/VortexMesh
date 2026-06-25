-- VortexMesh Database Initialization
CREATE DATABASE vortexmesh_registry;
CREATE DATABASE vortexmesh_control;
CREATE DATABASE vortexmesh_policy;
CREATE DATABASE vortexmesh_auth;
CREATE DATABASE vortexmesh_telemetry;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE vortexmesh_registry TO vortex;
GRANT ALL PRIVILEGES ON DATABASE vortexmesh_control TO vortex;
GRANT ALL PRIVILEGES ON DATABASE vortexmesh_policy TO vortex;
GRANT ALL PRIVILEGES ON DATABASE vortexmesh_auth TO vortex;
GRANT ALL PRIVILEGES ON DATABASE vortexmesh_telemetry TO vortex;
