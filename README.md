# SplitAudio Backend API (Orchestrator)

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![OpenFeign](https://img.shields.io/badge/Spring_Cloud-OpenFeign-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-S3_Storage-C72E49?style=for-the-badge&logo=minio&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)

The core orchestration backend for the **SplitAudio** ecosystem. This microservice coordinates audio ingestion (local uploads and YouTube downloads), asynchronous AI stem separation, metronome/BPM/key analysis, object storage management in MinIO, and relational metadata persistence in MySQL.

---

## 🏗️ Architecture & Tech Stack

- **Framework:** Java 21 & Spring Boot 4
- **Inter-Service Communication:** Spring Cloud OpenFeign with OkHttp
- **Database:** MySQL 8 (Spring Data JPA / Hibernate with `@EntityGraph` optimization)
- **Object Storage:** MinIO (S3-compatible) with dynamic presigned URLs
- **Job Queue & Caching:** Single-thread `ExecutorService` + Caffeine Cache
- **Build Tool:** Maven
- **Infrastructure:** Docker & Docker Compose (Multi-stage Alpine build)

### 🧩 Ecosystem Microservices

SplitAudio is designed as a distributed, polyglot architecture. Java/Spring Boot handles enterprise orchestration, concurrency, database state, and job queuing, while **Python** powers the heavy AI/ML audio processing models:

1. **[Split-Audio](https://github.com/JozRamirez10/Split-Audio):** Python AI worker responsible for separating audio tracks into base stems (bass, piano, drums, guitar, vocals, other) and sub-stems (individual drums, vocals, winds, and guitar roles/types).
2. **[Split-Metronome](https://github.com/JozRamirez10/Split-Metronome):** Python service that analyzes the original track to detect BPM, tuning frequency (Hz), and musical key, generating a synchronized metronome audio track.
3. **[Storage-MinIO](https://github.com/JozRamirez10/Storage-MinIO):** Lightweight Python helper running alongside the AI worker to download source audio and batch-upload separated stems directly to MinIO.
4. **MeTube (`alexta69/metube:latest`):** Self-hosted YouTube audio downloader configured to extract `.wav` files directly into a shared volume.
5. **MinIO:** Centralized S3-compatible object storage for serving separated tracks via time-limited presigned URLs.

### 🧠 Key Architectural Decisions

- **Host Co-Location Requirement (Backend + Metronome + MeTube):** `SplitAudio-Backend`, `Split-Metronome`, and `MeTube` **must run on the same host machine** sharing the same local directory (`ORCHESTRATOR_SHARED_DIR`, e.g., `/tmp/split_audio`). This allows the orchestrator to detect MeTube's downloaded `.wav` files, pass local file paths directly to `Split-Metronome`, and upload results straight to MinIO without redundant network file transfers.
- **Decoupled AI Worker Node:** `Split-Audio` and `Storage-MinIO` can run on a separate, dedicated machine (e.g., a GPU or Apple Silicon node) using their own local temporary directory (`SPLIT_AUDIO_TMP_DIR`).
- **Strategy & Template Method Patterns:** Sub-stem separation uses `StemProcessor` and `AbstractStemProcessor` to isolate instrument-specific API calls, while `BasePipeline` standardizes async execution, logging, and error handling.
- **Parallel Pipelines with Kill Switch:** Audio separation and metronome generation run concurrently via `CompletableFuture`. A shared `AtomicBoolean` kill switch immediately aborts sibling pipelines and triggers automatic MinIO/local rollbacks if any step fails.
- **Controlled Concurrency & Garbage Collection:** Incoming async jobs are queued through a single-threaded executor (`JobManagerService`) backed by Caffeine Cache to prevent overloading the AI workers. A scheduled task (`OrchestratorGarbageCollectorTask`) runs every 6 hours to purge orphan temporary files older than 2 hours.

---

## 📂 Repository Structure

```text
SplitAudio-Backend/
├── src/
│   └── main/
│       ├── java/com/app/splitaudio/
│       │   ├── constants/       # Centralized application constants
│       │   ├── exceptions/      # Global exception handler and custom exceptions
│       │   ├── orchestrator/    # Pipelines, Feign clients, stem processors, job queue, GC task, services and REST controllers
│       │   ├── songs/           # Entities, repositories, services and REST controllers
│       │   └── storage/         # MinIO client configuration and storage service
│       └── resources/
│           └── application.yml  # Environment-driven configuration (dev/prod profiles)
├── docker-compose.yml           # Self-contained stack (MySQL + Backend)
├── Dockerfile                   # Multi-stage Amazon Corretto 21 Alpine build
├── .env.example                 # Environment variables template
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose
- Running instances of **MinIO**, **Split-Audio**, **Split-Metronome**, **Storage-MinIO**, and **MeTube**

### Environment Setup

This project relies on a `.env` file for all runtime configurations.

1. Copy the template file:
   ```bash
   cp .env.example .env
   ```
2. Fill in your database credentials, MinIO keys, shared directories, and microservice URLs.
   - **Docker Note:** When running via `docker-compose.yml`, set `DB_HOST=mysql` and `DB_PORT=3306` so the backend connects to the internal MySQL container.

### Installation & Execution

**Option A: Running with Docker (Recommended)**

```bash
git clone https://github.com/JozRamirez10/SplitAudio-Backend.git
cd SplitAudio-Backend

# 1. Create your local environment file
cp .env.example .env

# STOP: Open .env and configure your Database, MinIO, Directories, and Feign URLs before continuing!

# 2. Build and start the containers (MySQL + Backend)
docker compose up -d --build
```

**Option B: Running locally with Maven**

```bash
git clone https://github.com/JozRamirez10/SplitAudio-Backend.git
cd SplitAudio-Backend

# 1. Create your local environment file
cp .env.example .env

# STOP: Open .env and configure your local Database, MinIO, Directories, and Feign URLs before continuing!

# 2. Compile and run the application
./mvnw clean install -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

1. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
2. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
3. Push to the Branch (`git push origin feature/AmazingFeature`)
4. Open a Pull Request