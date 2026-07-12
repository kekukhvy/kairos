package dev.kairos.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Shared base for repository integration tests, backed by an in-memory H2
 * database in PostgreSQL compatibility mode (no Docker / Testcontainers).
 *
 * <p>A single in-memory database per test class is created once and the
 * {@code h2-schema.sql} test resource (an H2-compatible mirror of the
 * destinations + tasks Flyway migrations) is applied to it. The {@link org.jooq.DSLContext}
 * is built on the POSTGRES dialect to match production SQL (the repositories
 * render Postgres-flavoured statements such as {@code INSERT ... ON CONFLICT});
 * H2's PostgreSQL mode accepts them.
 */
public abstract class H2DatabaseBase {

    private static final String SCHEMA_RESOURCE = "/h2-schema.sql";
    private static final int POOL_SIZE = 2;

    protected static DSLContext dslContext;

    @BeforeAll
    static void initDatabase() {
        DataSource dataSource = buildDataSource();
        applySchema(dataSource);
        // Reuse the production factory: it renders the POSTGRES dialect with
        // renderSchema=false / unquoted names, which is exactly the SQL shape H2
        // (MODE=PostgreSQL) accepts — and it exercises the real configuration.
        dslContext = DSLContextFactory.create(dataSource);
    }

    private static DataSource buildDataSource() {
        // One shared in-memory DB per JVM, kept alive by DB_CLOSE_DELAY=-1.
        // Class name in the URL keeps separate IT classes isolated.
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:kairos_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(POOL_SIZE);
        return new HikariDataSource(cfg);
    }

    private static void applySchema(DataSource dataSource) {
        String ddl = readResource();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            // Drop anything left from a previous class sharing the in-mem DB,
            // then recreate from the schema script.
            st.execute("DROP ALL OBJECTS");
            st.execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to apply H2 test schema", e);
        }
    }

    private static String readResource() {
        try (var in = H2DatabaseBase.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + SCHEMA_RESOURCE, e);
        }
    }
}
