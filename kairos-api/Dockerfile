# syntax=docker/dockerfile:1

# ---- Build stage: compile the API distribution -----------------------------
# JDK 26 to match the project toolchain. We build with the repo's Gradle
# wrapper, which downloads the right Gradle itself — no gradle image needed.
FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

# Copy the whole repo (kairos-api depends on :common and :kairos-persistence,
# and the Gradle wrapper + settings live at the root). .dockerignore keeps
# build/ and .git/ out.
COPY . .

# installDist lays out the app as bin/ + lib/ — the 'application' plugin's
# distribution format. No daemon in container builds.
RUN chmod +x gradlew \
    && ./gradlew --no-daemon :kairos-api:installDist

# ---- Runtime stage: slim JRE with just the distribution --------------------
FROM eclipse-temurin:26-jre AS runtime

# Run as a non-root user (least privilege).
RUN groupadd --system kairos && useradd --system --gid kairos kairos

WORKDIR /app

COPY --from=build /workspace/kairos-api/build/install/kairos-api/ ./

USER kairos

# Javalin listens on 8080 (server.port), kept distinct from kairos-admin's 8090
# so both can run side by side.
EXPOSE 8080

# Config comes from the environment (see AppConfig): DB_URL, DB_USERNAME,
# DB_PASSWORD, SERVER_PORT all override the packaged defaults. The API does not
# migrate — kairos-engine owns the schema, and SchemaReadinessCheck fails fast
# if it has not run yet.
ARG KAIROS_API_VERSION=dev
ENV KAIROS_API_VERSION=${KAIROS_API_VERSION}

ENTRYPOINT ["bin/kairos-api"]
