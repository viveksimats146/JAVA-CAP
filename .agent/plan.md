# Project Plan

Federated Graph Query Federation Engine for Distributed Biomedical Knowledge Enabling Cross-Hospital Rare Disease Correlation without Data Sharing. Three independent hospital servers each hold a private RDF graph of anonymized rare-disease cases. A clinician submits a SPARQL query from an Android app; the query is decomposed and federated to peer hospitals over real Java RMI; each hospital runs the sub-query ONLY on its own local Jena TDB2 graph; only result-level data comes back; results are merged, deduplicated, and ranked by cross-hospital corroboration. Monorepo structure with federation-common, dataset-provider-service, hospital-node-service, android-app, docker-compose, scripts, docs, and infra.

## Project Brief

# Project Brief: Federated Rare Disease Query App

## Features
- **SPARQL Query Builder & Templates**: Simple editor and preset selection for clinicians to compose SPARQL queries targeting rare disease case correlations.
- **Hospital Node Network Status**: Visual dashboard indicating connection status and health across independent hospital federation nodes.
- **Federated Query Execution**: Triggers query submission, tracking live progress across distributed hospital nodes in real time.
- **Results Aggregation & Corroboration Ranking**: Merges and deduplicates returned dataset results, ranking cases by cross-hospital corroboration scores.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Jetpack Navigation 3 (State-driven)
- **Adaptive Strategy**: Compose Material Adaptive library (`ListDetailPaneScaffold`, adaptive window size classes)
- **Asynchronous & Concurrent Execution**: Kotlin Coroutines & StateFlow
- **Architecture**: MVVM pattern with Unidirectional Data Flow (UDF)
- **Networking**: OkHttp / Retrofit for API gateway communication with federation engine nodes

## Implementation Steps
**Total Duration:** 63h 56m 48s

### Task_1_DataAndNetworkSetup: Set up shared data models, Retrofit API service for backend gateway interaction, Room database for query logs and local caching, and repository layer with Hilt dependency injection.
- **Status:** COMPLETED
- **Updates:** Implemented federation-common, dataset-provider-service, hospital-node-service, and android-app data/network layer (DTOs, Retrofit, AuthInterceptor, Room DB, DAOs, Hilt modules, Repositories). Verified all unit tests and builds.
- **Acceptance Criteria:**
  - Retrofit API client configured with JWT token interceptor
  - Room database and DAOs created for query history and cached results
  - Hilt modules provided for network and database dependencies
  - Repository layer implemented for authentication and federated query execution
- **Duration:** 67h 12m 38s

### Task_2_AuthAndDashboardScreens: Implement Login screen and Network Status Dashboard showing real-time connection status and health across independent hospital federation nodes.
- **Status:** COMPLETED
- **Updates:** Implemented LoginScreen, LoginViewModel, DashboardScreen, DashboardViewModel, Navigation Compose routes, and unit tests. Verified build and tests.
- **Acceptance Criteria:**
  - Login screen built with Jetpack Compose, handling JWT authentication state
  - Dashboard screen displaying federation node connection status and health indicators
  - UDF architecture with ViewModel and StateFlow for screen state management

### Task_3_QueryBuilderAndExecution: Implement SPARQL Query Builder with preset templates and live query execution tracking screen with real-time progress and logs.
- **Status:** COMPLETED
- **Updates:** Implemented QueryScreen, QueryViewModel, SPARQL syntax highlighting, preset query chips, node execution tracking, LogsScreen, LogsViewModel, AST decomposition debug view, cache stats card, navigation wiring, and unit tests. Verified build and unit tests.
- **Acceptance Criteria:**
  - SPARQL query editor with template selection for clinicians
  - Query submission triggering federated execution across hospital nodes
  - Live execution tracking UI with status updates and execution logs

### Task_4_ResultsAggregationAndDetailPane: Implement Results screen using Compose Material Adaptive ListDetailPaneScaffold to display aggregated and ranked rare disease case results.
- **Status:** COMPLETED
- **Updates:** Implemented ResultsScreen, ResultsViewModel, ListDetailPaneScaffold, corroboration ranking badges, raw triple details, offline Room persistence, and unit tests. Verified build and tests.
- **Acceptance Criteria:**
  - Results screen displaying merged and deduplicated rare disease cases
  - Corroboration ranking score visualization for cross-hospital results
  - Adaptive ListDetailPaneScaffold implementation for dual-pane tablet/phone layouts
- **Duration:** N/A

### Task_5_RunAndVerify: Build, run, and perform complete verification of the application stability, functionality, and alignment with user requirements.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies application stability (no crashes), confirms alignment with user requirements, and reports any critical UI issues
- **StartTime:** 2026-09-06 16:00:11 IST

