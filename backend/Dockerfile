# =============================================================================
# Finbrain API — Production Dockerfile (multi-stage)
# Location: backend/Dockerfile
#
# Stage 1 (builder): Compiles the Spring Boot JAR using Maven.
# Stage 2 (runtime):  Runs only the JAR on a minimal JRE — small attack surface.
# =============================================================================

# --- Stage 1: Build ---
# Use official Maven + Temurin 17 on Alpine for a smaller build image.
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# All subsequent commands run from /app inside the container.
WORKDIR /app

# Copy dependency descriptors FIRST so Docker layer cache is reused when only
# source code changes (dependencies are downloaded in a separate layer).
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Make the Maven wrapper executable (required on Linux).
RUN chmod +x mvnw

# Pre-download all Maven dependencies offline — speeds up rebuilds.
RUN ./mvnw dependency:go-offline -B

# Copy application source after dependencies are cached.
COPY src ./src

# Build the executable JAR; skip tests here (CI runs tests separately).
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: Runtime ---
# Minimal JRE-only image — no Maven, no source code, no compiler.
FROM eclipse-temurin:17-jre-alpine AS runtime

# curl is used only for the optional Docker HEALTHCHECK below.
RUN apk add --no-cache curl

# Create a dedicated non-root user for security (principle of least privilege).
RUN addgroup -S finbrain && adduser -S finbrain -G finbrain

WORKDIR /app

# Copy ONLY the built JAR from the builder stage — keeps the image small.
COPY --from=builder /app/target/finance-tracker-*.jar app.jar

# Ensure the non-root user owns the JAR.
RUN chown finbrain:finbrain app.jar

# Drop root privileges before starting the app.
USER finbrain

# Default to production Spring profile; override via Render env if needed.
ENV SPRING_PROFILES_ACTIVE=prod

# JVM tuning for containers: respect cgroup memory limits, faster startup entropy.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Document the port; Render injects PORT at runtime (Spring reads server.port=${PORT}).
EXPOSE 8080

# Optional in-container health probe (Render also probes /actuator/health externally).
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -f "http://localhost:${PORT:-8080}/actuator/health" || exit 1

# Shell form allows JAVA_OPTS and PORT env expansion at runtime.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
