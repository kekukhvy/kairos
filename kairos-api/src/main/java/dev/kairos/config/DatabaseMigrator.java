package dev.kairos.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class DatabaseMigrator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);

    public static void migrate(DataSource dataSource, AppConfig config) {
        log.info("Starting migration.");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(config.getProperty("flyway.locations"))
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();

        var result = flyway.migrate();
        log.info("Flyway applied {} migration(s), current version: {}",
                result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
