# Devices Manager Service

[![Latest Release](https://img.shields.io/github/v/release/osamaabdulsattar/devices-manager)](https://github.com/osamaabdulsattar/devices-manager/releases)

A Java 21 Spring Boot REST service for managing devices and their brands, backed by PostgreSQL.

Tagged releases (with release notes and a matching versioned Docker image published to GHCR) are available on the [Releases page](https://github.com/osamaabdulsattar/devices-manager/releases).

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 4
- **Build Tool**: Gradle (with Gradle Wrapper)
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI
- **Database**: PostgreSQL (with Spring Data JPA / Hibernate)
- **Database Migrations**: Flyway
- **Utilities**: Lombok (boilerplate reduction)
- **Observability**: Spring Boot Actuator (health, liveness & readiness probes)
- **Testing**: JUnit 5, Spring Boot Test, MockMvc, Testcontainers (PostgreSQL), JaCoCo coverage

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
Run locally using the Gradle wrapper (the `dev` profile logs SQL statements and enables debug logging):
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
Or build the executable JAR / Docker image:
```bash
# Build executable JAR
./gradlew bootJar
java -jar build/libs/devices-manager.jar

# Build standalone Docker image
docker build -t devices-manager .
```

Health checks are exposed at `/actuator/health`, `/actuator/health/liveness` and `/actuator/health/readiness`.

### 4. Running Tests
Integration tests run against a real PostgreSQL 16 container started by Testcontainers, so **Docker must be running**:
```bash
./gradlew test
```
A coverage report is generated at `build/reports/jacoco/test/html/index.html`.

The OpenAPI specification is written by hand (contract first). `OpenApiSpecConsistencyTest` fails the build if a documented
endpoint is not implemented (or the other way round), or if a documented schema's fields differ from its DTO.

---

## API Documentation & Swagger UI

Once the application is running, the interactive Swagger UI and OpenAPI specifications can be accessed in your browser:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (or `/swagger-ui/index.html`)
- **OpenAPI Specification (YAML)**: [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)
- **Specification Source File**: `src/main/resources/static/openapi.yaml`

---

## Testing with Postman

A pre-configured Postman Collection is provided in the repository:
- **Collection File**: [`postman/devices-manager.postman_collection.json`](postman/devices-manager.postman_collection.json)

### How to use:
1. Open **Postman** -> click **Import** -> select `postman/devices-manager.postman_collection.json`.
2. The folders are ordered so the whole collection can be run top to bottom on an empty database:
   - **Brands**: Fetch all, create, fetch single, update.
   - **Devices**: Fetch all, filter by state, filter by brand, combined filters, fetch single, create, update, delete.
   - **Cleanup**: Delete the brand (only possible once it has no devices).
   - **Documentation**: Raw OpenAPI spec.
3. **Automated Variables**: The collection defines `baseUrl` (default `http://localhost:8080`), `brandId`, `deviceId` and `deviceETag`. Creating or fetching resources automatically updates them for subsequent requests; `deviceETag` is sent as `If-Match` on device updates and deletes.

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
- **`version`**: Incremented on every change; also returned as the `ETag` header

#### Domain Validations
- **Creation Time Immutability**: Creation time (`createdAt`) is auto-generated upon creation and cannot be updated.
- **In-Use Device Protection (Update)**: Name and brand properties cannot be updated while the device state is `in-use` (`409 Conflict`).
- **In-Use Device Protection (Delete)**: A device cannot be deleted while its state is `in-use` (`409 Conflict`).
- **Conditional Updates**: `PUT`, `PATCH` and `DELETE` accept an optional `If-Match` header with the device's `ETag`; a stale value returns `412 Precondition Failed`.

---

### 2. Brand Management (`/api/v1/brands`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/brands` | **Fetch All Brands**: Retrieves all registered brands, sorted by name. |
| `POST` | `/api/v1/brands` | **Create Brand**: Registers a new brand (`name`, unique case-insensitively; `409` if taken). |
| `GET` | `/api/v1/brands/{id}` | **Fetch Single Brand**: Retrieves brand details by ID. |
| `PUT` | `/api/v1/brands/{id}` | **Update Brand**: Renames a brand. |
| `DELETE` | `/api/v1/brands/{id}` | **Delete Brand**: Deletes a brand; `409` while devices are still associated with it. |

To list a brand's devices, use `GET /api/v1/devices?brandId={id}`.

---

## Design Decisions & Assumptions

- **Brand is a separate entity** rather than a free-text field, so brand names stay consistent and devices can be filtered by brand reliably. Brand names are unique case-insensitively and trimmed.
- **Only name and brand are locked while a device is `in-use`.** Changing the state is always allowed, otherwise an in-use device could never be released. A request that repeats the current name/brand unchanged is accepted, so a full `PUT` that only changes the state works.
- **`PUT` replaces all fields, `PATCH` updates only the fields provided.** `null`/omitted fields in a `PATCH` are left unchanged.
- **Status codes:** `400` for malformed input or validation errors, `404` for unknown IDs (including a referenced brand), `409` when a valid request breaks a business rule (in-use device, brand still referenced, duplicate brand name, concurrent write), `412` for a stale `If-Match`.
- **Optimistic locking** (`@Version`) prevents lost updates. The version is exposed as an `ETag` so clients can opt in to conditional writes via `If-Match`; without the header the last write wins.
- **Contract-first API:** `openapi.yaml` is the source of truth for clients and is checked against the code by a test, instead of being generated from annotations.
- **Database:** the schema is owned by Flyway migrations (`ddl-auto=validate`), and tests run on the same PostgreSQL version as production.

## Future Improvements

- **Pagination & sorting** on `GET /api/v1/devices` (Spring Data `Pageable`) before device counts grow large.
- **Authentication & authorization**: OAuth2 resource server with JWTs and scopes such as `devices:read` / `devices:write`.
- **Audit trail / soft deletes**: keep a history of who changed or removed a device (e.g. Hibernate Envers or a `deleted_at` column).
- **Metrics & tracing**: Micrometer/Prometheus metrics and OpenTelemetry traces with correlation IDs in structured logs.
- **Domain events**: publish device state changes (e.g. via a transactional outbox) for downstream consumers.
