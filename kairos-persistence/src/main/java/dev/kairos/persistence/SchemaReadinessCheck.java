package dev.kairos.persistence;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Fail-fast startup guard: {@code kairos-api} no longer runs Flyway
 * (schema ownership moved to {@code kairos-engine}), so against a fresh,
 * un-migrated database it must not surface a raw "relation does not exist"
 * SQL error. Checking for one well-known table is enough to tell "engine
 * hasn't migrated yet" apart from any other startup failure.
 */
public final class SchemaReadinessCheck {

    private static final String MARKER_TABLE = "tasks";
    private static final String NOT_INITIALIZED_MESSAGE =
            "Database schema not initialized (missing table '" + MARKER_TABLE
                    + "') — start kairos-engine first, it owns and applies the schema.";

    private SchemaReadinessCheck() {
    }

    public static void verify(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData()
                     .getTables(null, null, MARKER_TABLE, null)) {

            if (!tables.next()) {
                throw new IllegalStateException(NOT_INITIALIZED_MESSAGE);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(NOT_INITIALIZED_MESSAGE, e);
        }
    }
}
