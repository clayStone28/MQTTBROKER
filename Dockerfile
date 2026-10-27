# ==============================================================================
# Stage 1: Build & Package Application
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 1. Copy full project context (pom.xml + src)
COPY . .

# 2. Package the Spring Boot JAR (skipping unit tests)
RUN mvn clean package -DskipTests

# ==============================================================================
# Stage 2: Minimal Runtime Container
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install FreeTTS runtime audio support dependencies (ALSA sound library for Linux)
RUN apk add --no-cache alsa-lib

# Create a non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Ensure proper ownership of application files
RUN chown -R appuser:appgroup /app

USER appuser

# Expose Spring Boot web port
EXPOSE 8000

# Configure Java memory parameters for container environment
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]