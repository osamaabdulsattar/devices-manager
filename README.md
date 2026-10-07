# Devices Manager Service

A Java 21 Spring Boot REST service with PostgreSQL connection, Lombok support, and Gradle build tool.

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 4
- **Build Tool**: Gradle (with Gradle Wrapper)
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

## API Endpoints

### Hello World REST Endpoint
- **URL**: `GET /api/hello`
- **Optional Query Parameter**: `name` (default: `World`)

#### Example Requests & Responses:
```bash
curl http://localhost:8080/api/hello
```
```json
{
  "message": "Hello, World!",
  "timestamp": "2026-10-07T06:40:00.000000"
}
```

```bash
curl "http://localhost:8080/api/hello?name=Alice"
```
```json
{
  "message": "Hello, Alice!",
  "timestamp": "2026-10-07T06:40:05.000000"
}
```
