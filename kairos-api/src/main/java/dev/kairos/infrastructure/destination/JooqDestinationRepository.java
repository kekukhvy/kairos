package dev.kairos.infrastructure.destination;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.infrastructure.generated.Tables;
import org.jooq.DSLContext;

import java.util.Objects;

/**
 * jOOQ-backed implementation of {@link DestinationRepository}.
 *
 * <p>M1 only needs an existence check to enforce the task -> destination FK at
 * the application level before a SQL constraint fires. A full CRUD
 * implementation arrives in M2 (Destinations API).
 */
public final class JooqDestinationRepository implements DestinationRepository {

    private final DSLContext dslContext;

    public JooqDestinationRepository(DSLContext dslContext) {
        this.dslContext = Objects.requireNonNull(dslContext, "dslContext cannot be null");
    }

    @Override
    public boolean existsById(DestinationId id) {

        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(Tables.DESTINATIONS)
                        .where(Tables.DESTINATIONS.ID.equal(id.value()))
        );
    }
}
