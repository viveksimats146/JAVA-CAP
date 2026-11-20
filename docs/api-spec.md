# API Specification: Federated Research & Diagnostics Platform

## Base URLs
- **Hospital Node Service**: `http://localhost:8080/`
- **Dataset Provider Service**: `http://localhost:8081/`

---

## 🔐 1. Authentication Endpoints

### `POST /api/auth/login`
Authenticates a user/client and generates a JWT access token.

#### Request Body
```json
{
  "username": "admin",
  "password": "password"
}
```

#### Response (`200 OK`)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "username": "admin"
}
```

---

## 🔍 2. Federated Query Endpoints

### `POST /api/query/federated`
Executes a federated SPARQL query across all registered hospital nodes.

#### Headers
`Authorization: Bearer <JWT_TOKEN>`

#### Request Body
```json
{
  "query": "SELECT ?condition ?symptom WHERE { ?s ?p ?o }",
  "maxResults": 100
}
```

#### Response (`200 OK`)
```json
[
  {
    "condition": "COVID-19",
    "symptom": "Fever",
    "score": 0.95,
    "contributingHospitals": ["Hospital-Alpha", "Hospital-Beta"],
    "totalCases": 1420
  }
]
```

---

## 🏥 3. Node & Health Telemetry Endpoints

### `GET /api/node/health`
Returns local hospital node status, RDF triple metrics, and uptime.

#### Response (`200 OK`)
```json
{
  "nodeId": "Hospital-Alpha-01",
  "status": "UP",
  "totalTriples": 50000,
  "uptimeSeconds": 123456
}
```

---

## 📁 4. Dataset Discovery Endpoints

### `GET /api/dataset/search`
Queries registered datasets and available node endpoints.

#### Response (`200 OK`)
```json
[
  {
    "datasetId": "dataset-oncology-01",
    "title": "Oncology Phenotype Dataset",
    "hospitalId": "Hospital-Alpha",
    "tripleCount": 25000
  }
]
```
