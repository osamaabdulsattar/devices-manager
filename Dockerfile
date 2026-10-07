# Multi-stage build for Devices Manager Service

# Stage 1: Build the application
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

# Copy gradle wrapper and configuration files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and build executable JAR
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

COPY --from=builder /workspace/build/libs/devices-manager-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
