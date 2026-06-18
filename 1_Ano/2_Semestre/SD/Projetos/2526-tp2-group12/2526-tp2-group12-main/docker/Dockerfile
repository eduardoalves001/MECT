# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM and source code
COPY pom.xml .
COPY src ./src

# Use BuildKit cache to speed up dependency downloads
# This mounts a cache directory for Maven, making rebuilds incredibly fast
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests && \
    mvn dependency:copy-dependencies -DoutputDirectory=build/lib

# Stage 2: Runtime
# Use the nonroot tag for better security
FROM gcr.io/distroless/java21-debian12:latest
WORKDIR /app

# Distroless runs as the 'nonroot' user; ensure we copy files with correct ownership
COPY --from=build /app/build/*.jar app.jar
COPY --from=build /app/build/lib ./lib

# This allows the 'command' in docker-compose to pass the Main class
