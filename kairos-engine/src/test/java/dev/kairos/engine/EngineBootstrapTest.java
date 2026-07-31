package dev.kairos.engine;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the engine's real startup behavior end to end against an in-memory
 * H2 database (PostgreSQL mode): migrate, then flip health to ready only
 * after migration completes — never before.
 */
class EngineBootstrapTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:engine_bootstrap_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String FLYWAY_LOCATIONS = "classpath:test-migrations";
    private static final String MARKER_TABLE = "tasks";

    private HikariDataSource dataSource;

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void migratesSchemaAndMarksHealthReadyOnlyAfterMigrationCompletes(@TempDir Path tempDir) throws Exception {
        dataSource = buildDataSource();
        Path healthFile = tempDir.resolve("engine.health");
        EngineHealth health = new EngineHealth(healthFile);

        assertFalse(health.isReady(), "must not be ready before bootstrap runs");

        new EngineBootstrap(dataSource, FLYWAY_LOCATIONS, health).run();

        assertTrue(health.isReady(), "must be ready once migration has completed");
        assertTrue(schemaExists(dataSource), "migration must have created the schema");
    }

    /**
     * A marker left behind by a previous run (crash, or a restart against a
     * non-ephemeral path) must not make the engine look ready while this run
     * is still migrating. Bootstrap clears it before touching the database.
     */
    @Test
    void clearsAStaleHealthMarkerBeforeMigrating(@TempDir Path tempDir) throws Exception {
        dataSource = buildDataSource();
        Path healthFile = tempDir.resolve("engine.health");
        EngineHealth health = new EngineHealth(healthFile);
        health.markReady();
        assertTrue(health.isReady(), "precondition: a stale marker exists");

        RecordingHealth recorded = new RecordingHealth(healthFile);
        new EngineBootstrap(dataSource, FLYWAY_LOCATIONS, recorded).run();

        assertFalse(recorded.readyWhileMigrating,
                "stale marker must be gone while migration is still running");
        assertTrue(recorded.isReady(), "must be ready again once migration has completed");
    }

    /**
     * Samples the health signal at the moment migration begins — i.e. after
     * bootstrap has cleared any stale marker but before it marks ready.
     */
    private static final class RecordingHealth extends EngineHealth {

        private boolean readyWhileMigrating = true;

        private RecordingHealth(Path healthFilePath) {
            super(healthFilePath);
        }

        @Override
        public void clear() {
            super.clear();
            readyWhileMigrating = isReady();
        }
    }

    private static HikariDataSource buildDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(2);
        return new HikariDataSource(config);
    }

    private static boolean schemaExists(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, MARKER_TABLE, null)) {
            return tables.next();
        }
    }
}
