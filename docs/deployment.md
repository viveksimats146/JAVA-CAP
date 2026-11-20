# Deployment & Node Onboarding Guide

## Production Deployment

### Hospital Node Deployment Requirements
- JDK 17 JRE
- Minimum 4 GB RAM (8 GB recommended for Jena TDB2 dataset indexes)
- Persistent SSD volume mounted for Jena TDB2 (`/var/data/tdb2`)

### Environment Variables Setup
Create `.env` file from `.env.example`:
```env
HOSPITAL_NODE_PORT=8080
RMI_PORT=1099
JWT_SECRET=production_super_secret_jwt_key_here_32_bytes
JENA_TDB2_LOCATION=/var/data/tdb2
```

### Running with Docker Compose
```bash
docker-compose up --build -d
```

### Health Check Verification
```bash
curl http://localhost:8080/api/node/health
```
Expected response:
`{"nodeId":"...","status":"UP","totalTriples":...,"uptimeSeconds":...}`
