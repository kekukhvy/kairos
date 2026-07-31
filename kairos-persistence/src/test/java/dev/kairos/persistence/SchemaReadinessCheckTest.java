package dev.kairos.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SchemaReadinessCheck}: the API's fail-fast guard that
 * distinguishes "engine hasn't migrated yet" from an opaque SQL error.
 */
class SchemaReadinessCheckTest {

    private static final String JDBC_URL_TEMPLATE =
            "jdbc:h2:mem:%s;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final int POOL_SIZE = 1;
    private static final String MARKER_TABLE_DDL = "CREATE TABLE tasks (id VARCHAR(36) PRIMARY KEY)";

    private HikariDataSource dataSource;

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void doesNotThrowWhenSchemaAlreadyMigrated() throws SQLException {
        dataSource = buildDataSource("schema_present");
        createMarkerTable(dataSource);

        assertDoesNotThrow(() -> SchemaReadinessCheck.verify(dataSource));
    }

    @Test
    void throwsExplicitErrorWhenSchemaIsMissing() {
        dataSource = buildDataSource("schema_missing");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> SchemaReadinessCheck.verify(dataSource));

        assertTrue(exception.getMessage().contains("kairos-engine"),
                "expected message to name the engine as the fix, was: " + exception.getMessage());
    }

    private static HikariDataSource buildDataSource(String databaseName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL_TEMPLATE.formatted(databaseName));
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(POOL_SIZE);
        return new HikariDataSource(config);
    }

    private static void createMarkerTable(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(MARKER_TABLE_DDL);
        }
    }
}
