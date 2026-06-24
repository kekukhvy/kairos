package dev.kairos.domain.destination;


/**
 * Read port for destinations. In M1 the only need is an existence check, so the
 * task -> destination foreign key can be validated before insert/update and
 * surfaced as a clean domain error instead of a raw SQL failure.
 *
 * <p>M2 (full Destinations API) will grow this into a complete repository.
 */
public interface DestinationRepository {

    boolean existsById(DestinationId id);
}