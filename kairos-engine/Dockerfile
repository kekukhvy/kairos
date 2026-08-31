# syntax=docker/dockerfile:1

# ---- Build stage: compile the engine distribution --------------------------
# JDK 26 to match the project toolchain. We build with the repo's Gradle
# wrapper, which downloads the right Gradle itself — no gradle image needed.
FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

# Copy the whole repo (kairos-engine depends on :kairos-persistence, and the
# Gradle wrapper + settings live at the root). .dockerignore keeps build/ and
# .git/ out.
COPY . .

# installDist lays out the app as bin/ + lib/ — the 'application' plugin's
# distribution format. No daemon in container builds.
RUN chmod +x gradlew \
    && ./gradlew --no-daemon :kairos-engine:installDist

# ---- Runtime stage: slim JRE with just the distribution --------------------
FROM eclipse-temurin:26-jre AS runtime

# Run as a non-root user (least privilege).
RUN groupadd --system kairos && useradd --system --gid kairos kairos

WORKDIR /app

COPY --from=build /workspace/kairos-engine/build/install/kairos-engine/ ./

# The engine writes its readiness marker here (engine.health.file). Owned by
# the runtime user so the non-root process can create it, and kept out of the
# app directory so a volume can expose it to other components.
RUN mkdir -p /var/run/kairos && chown kairos:kairos /var/run/kairos

USER kairos

# The engine serves no HTTP port today — it migrates the schema and stays up.
# Config comes from the environment (see AppConfig): DB_URL, DB_USERNAME,
# DB_PASSWORD, ENGINE_HEALTH_FILE all override the packaged defaults.
ENV ENGINE_HEALTH_FILE=/var/run/kairos/engine.health

# Image version, injected at build time, surfaced for debugging (docker inspect).
ARG KAIROS_ENGINE_VERSION=dev
ENV KAIROS_ENGINE_VERSION=${KAIROS_ENGINE_VERSION}

ENTRYPOINT ["bin/kairos-engine"]
