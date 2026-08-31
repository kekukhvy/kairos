package dev.kairos.persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class DatabaseMigrator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);
    private static final String FLYWAY_LOCATIONS_KEY = "flyway.locations";

    public static void migrate(DataSource dataSource, AppConfig config) {
        migrate(dataSource, config.getProperty(FLYWAY_LOCATIONS_KEY));
    }

    public static void migrate(DataSource dataSource, String flywayLocations) {
        log.info("Starting migration.");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();

        var result = flyway.migrate();
        log.info("Flyway applied {} migration(s), current version: {}",
                result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
