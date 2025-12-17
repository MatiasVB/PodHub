# Stage 1: Build with Gradle and JDK 21
FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app

# Copy Gradle wrapper and configuration files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src/ src/

# Build the JAR file (skip tests for faster Docker builds)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime with JRE 21 Alpine
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Health check using actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
