package dev.kairos.engine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-light health signal for {@code kairos-engine}.
 *
 * <p>The engine has no web framework, so instead of exposing an HTTP health
 * endpoint it marks readiness with a plain marker file on disk, written only
 * once Flyway has finished migrating. Orchestration (Docker Compose's
 * {@code depends_on: condition: service_healthy}) probes for it with a
 * {@code HEALTHCHECK CMD-SHELL "test -f <path>"} — no extra process, no extra
 * dependency, and it can never report healthy before {@link #markReady()} is
 * called at the end of a successful migration.
 *
 * <p>A marker file outlives the process that wrote it. If the engine crashes,
 * or restarts against a path that is not on the container's ephemeral
 * filesystem, the previous run's marker would otherwise still be there and
 * report "ready" while this run is only just starting to migrate — exactly the
 * race the signal exists to prevent. {@link #clear()} removes it, and
 * {@link EngineBootstrap} calls it before migrating.
 */
public class EngineHealth {

    private final Path healthFilePath;

    public EngineHealth(Path healthFilePath) {
        this.healthFilePath = healthFilePath;
    }

    public boolean isReady() {
        return Files.exists(healthFilePath);
    }

    /** Drops any marker left behind by a previous run. */
    public void clear() {
        try {
            Files.deleteIfExists(healthFilePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clear engine health marker " + healthFilePath, e);
        }
    }

    public void markReady() {
        try {
            Path parent = healthFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(healthFilePath, Long.toString(System.currentTimeMillis()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write engine health marker " + healthFilePath, e);
        }
    }
}
