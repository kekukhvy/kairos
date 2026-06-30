package dev.kairos.infrastructure.destination;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.destination.DestinationType;
import dev.kairos.infrastructure.generated.tables.records.DestinationsRecord;
import org.jooq.DSLContext;
import org.jooq.JSONB;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.kairos.infrastructure.generated.tables.Destinations.DESTINATIONS;

/**
 * jOOQ-backed implementation of {@link DestinationRepository}.
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
                        .from(DESTINATIONS)
                        .where(DESTINATIONS.ID.equal(id.value()))
        );
    }

    @Override
    public void save(Destination destination) {
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(destination.createdAt(), ZoneOffset.UTC);

        dslContext.insertInto(DESTINATIONS)
                .set(DESTINATIONS.ID, destination.destinationId().value())
                .set(DESTINATIONS.TYPE, destination.destinationType().name())
                .set(DESTINATIONS.CONFIG, JSONB.valueOf(destination.config()))
                .set(DESTINATIONS.CREATED_AT, createdAt)
                .onConflict(DESTINATIONS.ID)
                .doUpdate()
                .set(DESTINATIONS.TYPE, destination.destinationType().name())
                .set(DESTINATIONS.CONFIG, JSONB.valueOf(destination.config()))
                .execute();
    }

    @Override
    public Optional<Destination> findById(DestinationId id) {
        return dslContext.selectFrom(DESTINATIONS)
                .where(DESTINATIONS.ID.equal(id.value()))
                .fetchOptional()
                .map(this::toDomain);
    }

    @Override
    public List<Destination> findAll(int limit, int offset) {
        return dslContext.selectFrom(DESTINATIONS)
                .orderBy(DESTINATIONS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map(this::toDomain);
    }

    @Override
    public void deleteById(DestinationId id) {
        dslContext.delete(DESTINATIONS)
                .where(DESTINATIONS.ID.equal(id.value()))
                .execute();
    }

    private Destination toDomain(DestinationsRecord record) {
        return Destination.builder()
                .destinationId(DestinationId.of(record.getId()))
                .destinationType(DestinationType.valueOf(record.getType()))
                .destinationConfig(record.getConfig().data())
                .createdAt(record.getCreatedAt().toInstant())
                .build();
    }
}