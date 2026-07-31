package dev.kairos.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DatabaseMigrator}'s location-only overload, the seam
 * {@code kairos-engine} uses so it can run a migration without depending on
 * the {@code AppConfig}-shaped call the API uses.
 */
class DatabaseMigratorTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:database_migrator_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String FLYWAY_LOCATIONS = "classpath:test-migrations";
    private static final String MARKER_TABLE = "marker";
    private static final int POOL_SIZE = 2;

    private HikariDataSource dataSource;

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void migratesUsingRawFlywayLocationsWithoutAnAppConfig() throws Exception {
        dataSource = buildDataSource();

        DatabaseMigrator.migrate(dataSource, FLYWAY_LOCATIONS);

        assertTrue(markerTableExists(dataSource));
    }

    private static HikariDataSource buildDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(POOL_SIZE);
        return new HikariDataSource(config);
    }

    private static boolean markerTableExists(HikariDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, MARKER_TABLE, null)) {
            return tables.next();
        }
    }
}
