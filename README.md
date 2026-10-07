# Devices Manager Service

A Java 21 Spring Boot REST service with PostgreSQL connection, Lombok support, SpringDoc OpenAPI (Swagger UI), and Gradle build tool.

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 4
- **Build Tool**: Gradle (with Gradle Wrapper)
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI
- **Database**: PostgreSQL (with Spring Data JPA / Hibernate)
- **Utilities**: Lombok (boilerplate reduction)
- **Testing**: JUnit 5, Spring Boot Test, MockMvc, H2 (in-memory test database)

---

## Getting Started

### 1. Prerequisites
- Java 21 JDK installed
- Docker

### 2. Start PostgreSQL (Docker)
A `docker-compose.yml` file is included for local development:
```bash
docker compose up -d
```
Default connection settings:
- **Host**: `localhost:5432`
- **Database**: `devices_db`
- **User**: `postgres`
- **Password**: `postgres`

These can be customized via environment variables:
- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/devices_db`)
- `SPRING_DATASOURCE_USERNAME` (default: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (default: `postgres`)

### 3. Build & Run the Application
Run locally using the Gradle wrapper:
```bash
./gradlew bootRun
```
Or build the executable JAR:
```bash
./gradlew bootJar
java -jar build/libs/devices-manager-0.0.1-SNAPSHOT.jar
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
