# Architectural Specification: Federated Research & Diagnostics Platform

## 1. System High-Level Architecture

The Federated Research & Diagnostics Platform is an enterprise-scale distributed medical intelligence system designed for privacy-preserving federated clinical queries across decentralized hospital databases.

```
+-----------------------------------------------------------------------------------+
|                                   ANDROID APP                                     |
|  [Compose UI] -> [ViewModels] -> [Repositories] -> [Room DB / DataStore]          |
|                                         | (Retrofit + AuthInterceptor)            |
+-----------------------------------------|-----------------------------------------+
                                          |
                                          v  HTTPS / REST
+-----------------------------------------------------------------------------------+
|                              HOSPITAL NODE SERVICE                                |
|  [Security / JWT] -> [Query/Auth Controllers] -> [Federation Orchestrator]        |
|                                         |                                         |
|                       +-----------------+-----------------+                       |
|                       | (RMI / Local)                     |                       |
|                       v                                   v                       |
|         +---------------------------+           +-------------------+             |
|         |    FEDERATION COMMON      |           |   JENA TDB2 STORE |             |
|         | - SPARQL AST & Parser     |           | - RDF Triples     |             |
|         | - Query Decomposition     |           | - Local Graph     |             |
|         | - Cost Estimation Visitor |           +-------------------+             |
|         | - Sub-Query LRU Cache     |                                             |
|         +---------------------------+                                             |
+-----------------------------------------------------------------------------------+
                                          |
                                          | REST (Metadata Lookup)
                                          v
+-----------------------------------------------------------------------------------+
|                            DATASET PROVIDER SERVICE                               |
|  [DatasetController] -> [DatasetService] -> [Node Registry & Medical Datasets]    |
+-----------------------------------------------------------------------------------+
```

---

## 2. Core Modules Specification

### 2.1 `federation-common` (Kotlin JVM Engine)
- **AST (`SparqlAst.kt`)**: Represents SPARQL Abstract Syntax Trees including SELECT queries, Triple Patterns, Joins, Union, Filter, and Limit nodes.
- **Query Decomposition (`DecompositionVisitor.kt`)**: Splits global SPARQL queries into targeted sub-queries bound to specific hospital predicate domains.
- **Cost Estimation (`CostEstimationVisitor.kt`)**: Implements cost-based optimization heuristics using node graph size, triple selectively, and network latency estimates.
- **Sub-Query Cache (`SubQueryCache.kt`)**: High-performance thread-safe LRU cache for query results and sub-tree executions.
- **RMI Interface (`RemoteQueryService.kt`, `RmiModels.kt`)**: Java RMI contract for fast inter-node communication.

### 2.2 `hospital-node-service` (Spring Boot + Jena TDB2)
- **Graph Store (`LocalGraphManager.kt`)**: Embedded Apache Jena TDB2 dataset manager storing RDF triples (e.g. patients, diagnoses, symptoms, treatments).
- **JWT Authentication (`SecurityConfig.kt`, `JwtTokenProvider.kt`)**: Stateless security filter chain enforcing Bearer token verification.
- **RMI Server (`RmiServerManager.kt`, `RemoteQueryServiceImpl.kt`)**: Exposes RMI endpoints for sub-query execution across node networks.
- **Result Alignment (`ResultAlignmentEngine.kt`)**: Normalizes and scores multi-hospital clinical data into unified statistical representations (`RankedResultDto`).

### 2.3 `dataset-provider-service` (Spring Boot Microservice)
- **Dataset Registry (`DatasetService.kt`, `DatasetController.kt`)**: REST endpoints serving dataset availability, medical taxonomy schemas, and node connectivity states.

### 2.4 `app` (Android Client Application)
- **Architecture**: MVVM + Clean Architecture + Hilt DI.
- **Network Layer**: Retrofit 2 + OkHttp + `AuthInterceptor` for automatic JWT attachment.
- **Data Layer**: Room Database (`AppDatabase`, `QueryResultDao`, `NodeHealthDao`) for offline caching of federated results and node telemetry.
- **Token Storage**: `TokenManager` backed by Jetpack DataStore Preferences.
- **UI Framework**: Jetpack Compose with Material Design 3.

---

## 3. Data Flow & Federated Execution Sequence

```
User (Android)         Hospital Node Service         Federation Common         Jena TDB2 / Remote Nodes
     |                           |                           |                           |
     |--- POST /api/query ------>|                           |                           |
     |                           |--- Decompose Query ------>|                           |
     |                           |<-- AST Sub-queries -------|                           |
     |                           |                           |                           |
     |                           |--- Execute Sub-queries ------------------------------>|
     |                           |<-- Sub-query Results ---------------------------------|
     |                           |                           |                           |
     |                           |--- Align & Score Results -|                           |
     |                           |                           |                           |
     |<-- 200 OK (RankedResults)-|                           |                           |
```

---

## 4. Security & Compliance
- **Authentication**: JWT authentication with HMAC-SHA256 signature verification.
- **Privacy Enforcement**: Aggregate statistics and k-anonymity filtering prevent direct extraction of protected health information (PHI).
- **Transport Security**: TLS 1.3 enforced for HTTPS endpoints; encrypted RMI sockets for peer-to-peer node communication.
