# syntax=docker/dockerfile:1

# ---- Build stage: compile the Vaadin production bootJar --------------------
# JDK 26 to match the project toolchain. We build with the repo's Gradle
# wrapper (9.5.1), which downloads the right Gradle itself — no gradle image
# needed. The Vaadin production build (-Pvaadin.productionMode=true) bundles the
# optimized frontend into the jar.
FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

# Copy the whole repo (kairos-admin depends on :common, and the Gradle wrapper
# + settings live at the root). .dockerignore keeps build/ and .git/ out.
COPY . .

# Build only the admin module and what it needs. No daemon in container builds.
RUN chmod +x gradlew \
    && ./gradlew --no-daemon -Pvaadin.productionMode=true :kairos-admin:bootJar

# ---- Runtime stage: slim JRE with just the jar ----------------------------
FROM eclipse-temurin:26-jre AS runtime

# Run as a non-root user (least privilege).
RUN groupadd --system kairos && useradd --system --gid kairos kairos

WORKDIR /app

# The bootJar lands in kairos-admin/build/libs/. Vaadin/Spring Boot produce a
# single runnable jar; copy it as app.jar.
COPY --from=build /workspace/kairos-admin/build/libs/*.jar app.jar

# Vaadin/Spring Boot default HTTP port.
EXPOSE 8080

USER kairos

# Image version, injected at build time, surfaced for debugging (docker inspect).
ARG KAIROS_ADMIN_VERSION=dev
ENV KAIROS_ADMIN_VERSION=${KAIROS_ADMIN_VERSION}

ENTRYPOINT ["java", "-jar", "app.jar"]
