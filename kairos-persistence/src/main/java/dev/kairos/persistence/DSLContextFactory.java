package dev.kairos.persistence;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Builds a shared {@link org.jooq.DSLContext} from an existing {@link javax.sql.DataSource}. A
 * single instance is created at startup and injected into every repository.
 *
 * <p>Settings:
 * <ul>
 *   <li>{@code renderSchema = false} — avoids prefixing every query with
 *       {@code "public."}, which is the default schema anyway.</li>
 *   <li>{@code renderQuotedNames = NEVER} — keeps generated SQL readable and
 *       consistent with the lowercase-identifier conventions in migrations.</li>
 * </ul>
 */
public class DSLContextFactory {

    private DSLContextFactory() {

    }

    public static DSLContext create(DataSource dataSource) {

        Objects.requireNonNull(dataSource, "dataSource cannot be null");

        Settings settings = new Settings()
                .withRenderSchema(false)
                .withRenderQuotedNames(RenderQuotedNames.NEVER);


        return DSL.using(dataSource, SQLDialect.POSTGRES, settings);
    }
}
