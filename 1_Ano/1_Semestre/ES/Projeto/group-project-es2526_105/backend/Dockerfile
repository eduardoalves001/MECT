FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Install development tools
RUN apk add --no-cache curl wget

# Copy jar file
COPY --from=build /app/target/*.jar app.jar

# Expose debug port for development
EXPOSE 8080 5005

# Development JVM settings with debug support
ENTRYPOINT ["java", \
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=80.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=dev", \
    "-jar", "app.jar"]