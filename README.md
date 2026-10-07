# Devices Manager Service

A Java 21 Spring Boot REST service with PostgreSQL connection, Lombok support, SpringDoc OpenAPI (Swagger UI), and Gradle build tool.

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 4
- **Build Tool**: Gradle (with Gradle Wrapper)
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI
- **Database**: PostgreSQL (with Spring Data JPA / Hibernate)
- **Database Migrations**: Flyway
- **Utilities**: Lombok (boilerplate reduction)
- **Testing**: JUnit 5, Spring Boot Test, MockMvc, H2 (in-memory test database)

---

## Getting Started

### 1. Prerequisites
- Java 21 JDK installed
- Docker

### 2. Run Containerized with Docker Compose
To run both the application and PostgreSQL database fully containerized:
```bash
docker compose up --build -d
```
The application will be available at [http://localhost:8080](http://localhost:8080).

Alternatively, to start only the PostgreSQL container for local development:
```bash
docker compose up postgres -d
```
Default connection settings:
- **Host**: `localhost:5432`
- **Database**: `devices_db`
- **User**: `postgres`
- **Password**: `postgres`

### 3. Build & Run the Application Locally
Run locally using the Gradle wrapper:
```bash
./gradlew bootRun
```
Or build the executable JAR / Docker image:
```bash
# Build executable JAR
./gradlew bootJar
java -jar build/libs/devices-manager-0.0.1-SNAPSHOT.jar

# Build standalone Docker image
docker build -t devices-manager .
```

### 4. Running Tests
Run the test suite (uses embedded in-memory database for testing without requiring a live PostgreSQL instance):
```bash
./gradlew test
```

---

## API Documentation & Swagger UI

Once the application is running, the interactive Swagger UI and OpenAPI specifications can be accessed in your browser:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (or `/swagger-ui/index.html`)
- **OpenAPI Specification (YAML)**: [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)
- **Specification Source File**: `src/main/resources/static/openapi.yaml`

---

## Testing with Postman

A pre-configured Postman Collection is provided in the repository:
- **Collection File**: [`postman/devices-manager.postman_collection.json`](file:///Users/osama/workspace/devices-manager/postman/devices-manager.postman_collection.json)

### How to use:
1. Open **Postman** -> click **Import** -> select `postman/devices-manager.postman_collection.json`.
2. The collection includes the following folders:
   - **Devices**: Fetch all, filter by state, filter by brand, combined filters, fetch single, create, update, delete.
   - **Brands**: Fetch all, create, fetch single, update, delete.
   - **Documentation**: Raw OpenAPI spec.
3. **Automated Variables**: The collection defines `baseUrl` (default `http://localhost:8080`), `brandId`, and `deviceId`. Creating or fetching resources automatically updates `brandId` and `deviceId` for subsequent requests.

---

## Available REST Endpoints

### 1. Device Management (`/api/v1/devices`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/devices` | **Create Device**: Creates a new device associated with an existing brand (`name`, `brandId`, optional `state`). |
| `GET` | `/api/v1/devices` | **Fetch Devices**: Retrieves all devices. Optional, combinable query filters: `?brandId=`, `?brandName=`, `?state=` (e.g. `?state=available&brandId=...`). |
| `GET` | `/api/v1/devices/{id}` | **Fetch Single Device**: Retrieves details of a device by its ID. |
| `PUT` | `/api/v1/devices/{id}` | **Full Update**: Replaces all attributes (`name`, `brandId`, `state`) of an existing device. |
| `PATCH` | `/api/v1/devices/{id}` | **Partial Update**: Updates only the provided fields of an existing device. |
| `DELETE` | `/api/v1/devices/{id}` | **Delete Device**: Deletes a device by its ID (`204 No Content`). |

#### Device Domain Model
- **`id`**: Unique identifier (UUID)
- **`name`**: Device name/model
- **`brand`**: Associated Brand entity (`id`, `name`)
- **`state`**: Operational state (`available`, `in-use`, `inactive`)
- **`createdAt`**: Creation timestamp (ISO-8601)

#### Domain Validations
- **Creation Time Immutability**: Creation time (`createdAt`) is auto-generated upon creation and cannot be updated.
- **In-Use Device Protection (Update)**: Name and brand properties cannot be updated while the device state is `in-use`.
- **In-Use Device Protection (Delete)**: A device cannot be deleted while its state is `in-use`.

---

### 2. Brand Management (`/api/v1/brands`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/brands` | **Fetch All Brands**: Retrieves list of all registered brands. |
| `POST` | `/api/v1/brands` | **Create Brand**: Registers a new brand entity (`name`). |
| `GET` | `/api/v1/brands/{id}` | **Fetch Single Brand**: Retrieves brand details by ID. |
| `PUT` | `/api/v1/brands/{id}` | **Update Brand**: Updates brand name. |
| `DELETE` | `/api/v1/brands/{id}` | **Delete Brand**: Deletes a brand entity (when no devices are associated). |

To list a brand's devices, use `GET /api/v1/devices?brandId={id}`.

---

## Future Improvements & Production Roadmap

Below are recommended architectural, operational, and feature enhancements to take this service from MVP to an enterprise-grade production platform:

### 1. Security & Access Governance
- **OAuth2 / OIDC Authentication**: Integrate with an identity provider (e.g., Keycloak, Auth0, Okta, or AWS Cognito) using Spring Security OAuth2 Resource Server with JWT validation.
- **Role-Based Access Control (RBAC)**: Enforce granular scopes and permissions (e.g., `devices:read`, `devices:write`, `devices:delete`, `admin`) to protect sensitive administrative actions.
- **Audit Logging & Entity Versioning**: Implement Spring Data Envers or database triggers to maintain an immutable audit trail of who modified or deleted devices, capturing timestamps, caller identity, and state diffs.

### 2. Observability & Reliability
- **Spring Boot Actuator**: Add `/actuator/health/liveness` and `/actuator/health/readiness` probes for Kubernetes orchestration, alongside `/actuator/metrics`.
- **OpenTelemetry (OTel) & Distributed Tracing**: Export distributed traces and span contexts to APM backends (such as New Relic, Datadog, Jaeger, or Grafana Tempo) for end-to-end request visibility.
- **Prometheus Metrics**: Expose application and JVM metrics via Micrometer for alerting and monitoring dashboards.
- **Structured JSON Logging**: Standardize application logs to structured JSON (e.g., Logstash Logback Encoder) with correlation IDs (`traceId`, `spanId`) for ingestion by ELK, Loki, or Datadog.

### 3. API Scalability & Concurrency Control
- **Pagination & Sorting**: Transition `GET /api/v1/devices` to support Spring Data `Pageable` (`page`, `size`, `sort`) to avoid high memory consumption and slow queries when device counts scale into thousands or millions.
- **HTTP `ETag` / `If-Match` Support**: Entity versioning (`@Version`) is already in place to reject lost updates with a `409 Conflict`; expose that version via HTTP `ETag` and `If-Match` headers for standards-based conditional requests.
- **Rate Limiting & Throttling**: Protect public and high-throughput endpoints using Token Bucket algorithms (e.g., Bucket4j or Redis-backed rate limiters).

### 4. Data Architecture & Performance
- **Caching Layer**: Cache frequent read operations (such as brand lookups or device queries by ID) using Redis and Spring Cache, with cache eviction on state changes.
- **Soft Deletes**: In enterprise asset management, replace physical deletions with soft deletion (`deleted_at` timestamp or `@SQLRestriction`) to retain compliance and historical audit data.
- **Composite Database Indexes**: Add composite indexes (e.g., on `(brand_id, state)`) to optimize multi-criteria filtering queries at scale.

### 5. Event-Driven Architecture
- **Domain Event Publishing**: Publish lifecycle events (`DeviceCreatedEvent`, `DeviceStateChangedEvent`, `DeviceDeletedEvent`) to an event broker like Apache Kafka or RabbitMQ to decouple downstream consumers (inventory, billing, reporting).
- **Transactional Outbox Pattern**: Ensure reliable message delivery by persisting domain events within the same database transaction as business entities.

### 6. Cloud-Native Delivery & CI/CD
- **Kubernetes Helm Chart**: Package deployment manifests (Deployment, Service, Ingress, Horizontal Pod Autoscaler, PodDisruptionBudget, ConfigMaps, and Secrets) into a reusable Helm chart.
- **SonarQube Code Quality Gate**: Integrate SonarQube/SonarCloud into the CI pipeline for deeper static analysis than SpotBugs alone (code smells, duplication, test coverage, security hotspots), enforced as a merge-blocking quality gate. Requires a SonarQube server/account and a `SONAR_TOKEN` secret.
- **Aikido Security Scanning**: Add Aikido Security to the CI pipeline for unified SAST, open-source dependency (SCA) scanning, container image scanning, and secrets detection, with findings surfaced directly on pull requests. Requires an Aikido account and API key.

