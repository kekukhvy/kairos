package dev.kairos.engine;

import dev.kairos.persistence.DatabaseMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * The engine's startup sequence: migrate the schema, then flip the health
 * signal to ready. The engine owns the schema (see
 * {@code doc/specs/persistence-module.md}), so this is the single place
 * schema ownership is exercised for this component.
 *
 * <p>Order matters: any marker left by a previous run is cleared first, and
 * {@link EngineHealth#markReady()} runs strictly after
 * {@link DatabaseMigrator#migrate} returns, so orchestration polling the
 * health signal never observes "ready" while migrations are still running —
 * neither on a first start nor on a restart after a crash.
 */
public final class EngineBootstrap {

    private static final Logger log = LoggerFactory.getLogger(EngineBootstrap.class);

    private final DataSource dataSource;
    private final String flywayLocations;
    private final EngineHealth health;

    public EngineBootstrap(DataSource dataSource, String flywayLocations, EngineHealth health) {
        this.dataSource = dataSource;
        this.flywayLocations = flywayLocations;
        this.health = health;
    }

    public void run() {
        health.clear();
        DatabaseMigrator.migrate(dataSource, flywayLocations);
        health.markReady();
        log.info("Schema ready, engine health signal is up.");
    }
}
