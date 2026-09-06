# syntax=docker/dockerfile:1

# ShopSphere notification-service - multi-stage build.
#
# Stage 1 builds the Spring Boot fat jar with Maven; stage 2 runs it on a
# minimal JRE-only image. Only this service's own pom.xml/source is copied
# in (each ShopSphere service is an independent Maven project - there is no
# shared parent module), so the build context for this Dockerfile should be
# this service's own directory (see docker-compose.yml: build.context).

# ---- Build stage ------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies in their own layer: only re-download when pom.xml changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -Dmaven.test.skip=true

# ---- Runtime stage ------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user rather than the image default (root).
RUN addgroup -S shopsphere && adduser -S shopsphere -G shopsphere
USER shopsphere

COPY --from=build /build/target/notification-service-*.jar app.jar

EXPOSE 4007

# Matches management.endpoints.web.exposure.include=health,info,metrics already
# configured in application.properties - no code change needed for this to work.
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD wget -qO- http://localhost:4007/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
