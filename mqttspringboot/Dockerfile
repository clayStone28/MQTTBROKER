# ==============================================================================
# Stage 1: Build & Package Application
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy everything from repository context
COPY .. .

# Adjust WORKDIR to your subfolder if pom.xml is inside a subfolder (e.g., mqttspringboot)
# Uncomment and update the line below if your pom.xml is inside a folder named 'mqttspringboot':
# WORKDIR /app/mqttspringboot

RUN mvn clean package -DskipTests

# ==============================================================================
# Stage 2: Minimal Runtime Container
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache alsa-lib

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Locate jar dynamically across root or subfolder targets
COPY --from=builder /app/**/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]