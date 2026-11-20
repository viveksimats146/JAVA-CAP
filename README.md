# Federated Research & Diagnostics Platform

An enterprise-grade, privacy-preserving federated medical research platform. The system orchestrates SPARQL query decomposition, cost-based sub-query estimation, RMI-based node communication, and semantic result alignment across distributed hospital nodes, complemented by an Android client application.

---

## 🏛 Platform Architecture Overview

```
                          ┌──────────────────────────┐
                          │   Android Mobile App     │
                          │ (Compose + Retrofit + DB)│
                          └────────────┬─────────────┘
                                       │ REST / JWT
                                       ▼
                          ┌──────────────────────────┐
                          │  Hospital Node Service   │
                          │ (Spring Boot + Jena TDB2)│
                          └─────┬──────────────┬─────┘
                                │              │
                   RMI / Internal │              │ REST / Dataset Metadata
                                ▼              ▼
               ┌──────────────────────┐   ┌──────────────────────────┐
               │  Federation Common   │   │ Dataset Provider Service │
               │ (AST, Cost Estimator,│   │  (Dataset Metadata API)  │
               │ Cache, Decomposition)│   └──────────────────────────┘
               └──────────────────────┘
```

The system comprises four core modules:
1. **`federation-common`**: Core federation logic written in Kotlin JVM (SPARQL AST, cost estimation visitors, query decomposition, sub-query cache, RMI contracts).
2. **`hospital-node-service`**: Spring Boot backend serving as a hospital data node with Apache Jena TDB2 triple store, JWT authentication, RMI server orchestrator, and result alignment engine.
3. **`dataset-provider-service`**: Spring Boot microservice supplying dataset availability and hospital node registry metadata.
4. **`app`**: Native Android application utilizing Jetpack Compose, Retrofit with `AuthInterceptor`, Room database, Hilt Dependency Injection, and Repository pattern.

---

## 🚀 Quick Start & Prerequisites

### Prerequisites
- **Java JDK**: Version 17+
- **Android SDK**: API Level 35 (Android 15)
- **Docker & Docker Compose**: Version 20.10+
- **Gradle**: 8.14+ (managed via `./gradlew`)

---

## 🛠 Building & Testing

### 1. Build and Test Core Federation Common
```bash
./gradlew :federation-common:test
```

### 2. Build and Test Dataset Provider Service
```bash
./gradlew :dataset-provider-service:test
```

### 3. Build and Test Hospital Node Service
```bash
./gradlew :hospital-node-service:test
```

### 4. Build Android Application
```bash
./gradlew :app:assembleDebug
```

### 5. Run Full Build Pipeline
```bash
./scripts/build.sh
```

---

## 🐳 Docker Containerization

To launch the backend services in containerized environments:

```bash
# Copy example env
cp .env.example .env

# Start a 4-node cluster (3 hospital nodes + 1 dataset provider)
docker-compose up --build -d
```

Services will be accessible at:
- **Hospital Node A**: `http://localhost:8080` (Primary entry point for the Android app)
- **Hospital Node B**: Internal peer (RMI port 1099)
- **Hospital Node C**: Internal peer (RMI port 1099)
- **Dataset Provider Service**: `http://localhost:8081`

---

## 📱 Android App Setup

1. Open the project in **Android Studio**.
2. Select the `:app` run configuration.
3. Launch on an Android Emulator or physical device (Android 7.0+ / API 24+).
4. The Android app connects to `http://10.0.2.2:8080/` by default when running on the Android Emulator.

---

## 📁 Repository Structure

```
.
├── ARCHITECTURE.md                 # Detailed architecture documentation
├── README.md                       # Main repository overview
├── docker-compose.yml              # Docker compose service orchestrator
├── .env.example                    # Environment configuration template
├── app/                            # Android native application module
├── federated-rd-engine/
│   ├── federation-common/          # SPARQL decomposition & AST engine
│   ├── dataset-provider-service/   # Metadata & dataset discovery microservice
│   └── hospital-node-service/      # Spring Boot + Jena TDB2 hospital node
├── docs/                           # Documentation folder (API spec, SPARQL guide, Deployment)
├── scripts/                        # Automated build and testing scripts
└── .github/workflows/ci.yml        # CI/CD pipeline definition
```

---

## 📄 License
Privately developed for Federated Research & Diagnostics Platform.
