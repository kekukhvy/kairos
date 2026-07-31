package dev.kairos.engine;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EngineHealth}: a dependency-free, file-based health
 * signal. Compose/orchestration probes for the marker file's existence
 * (e.g. {@code test -f <path>}) to gate {@code depends_on:
 * condition: service_healthy} without adding a web framework to the engine.
 */
class EngineHealthTest {

    @Test
    void isNotReadyBeforeMarking(@org.junit.jupiter.api.io.TempDir Path tempDir) {
        Path healthFile = tempDir.resolve("engine.health");
        EngineHealth health = new EngineHealth(healthFile);

        assertFalse(health.isReady());
        assertFalse(Files.exists(healthFile));
    }

    @Test
    void isReadyAfterMarkingReady(@org.junit.jupiter.api.io.TempDir Path tempDir) {
        Path healthFile = tempDir.resolve("engine.health");
        EngineHealth health = new EngineHealth(healthFile);

        health.markReady();

        assertTrue(health.isReady());
        assertTrue(Files.exists(healthFile));
    }

    @Test
    void clearRemovesAnExistingMarker(@org.junit.jupiter.api.io.TempDir Path tempDir) {
        Path healthFile = tempDir.resolve("engine.health");
        EngineHealth health = new EngineHealth(healthFile);
        health.markReady();

        health.clear();

        assertFalse(health.isReady());
        assertFalse(Files.exists(healthFile));
    }

    /** Clearing on a first-ever start must be a no-op, not a failure. */
    @Test
    void clearOnAMissingMarkerDoesNotThrow(@org.junit.jupiter.api.io.TempDir Path tempDir) {
        Path healthFile = tempDir.resolve("engine.health");
        EngineHealth health = new EngineHealth(healthFile);

        assertDoesNotThrow(health::clear);
        assertFalse(health.isReady());
    }
}
