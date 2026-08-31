package dev.kairos.domain.destination;


import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link Destination}. Implemented in the infrastructure
 * layer (JooqDestinationRepository). No SQL/JDBC leaks across this boundary.
 *
 * <p>{@code existsById} predates the rest of this interface — it was the only
 * method needed while Destinations were a FK-existence stub (M1), used to
 * validate the task -> destination reference before insert/update and surface
 * a clean domain error instead of a raw SQL failure. The remaining methods
 * were added for the full Destinations CRUD API (M2).
 */
public interface DestinationRepository {

    /**
     * Checks whether a destination with the given id exists, regardless of its
     * type or config. Used to validate the task -> destination foreign key
     * before a task is inserted or updated.
     */
    boolean existsById(DestinationId id);

    /**
     * Persists the current state of the destination: inserts if the
     * {@link DestinationId} is new, otherwise updates the existing row. The
     * entity is the source of truth for every column except {@code createdAt},
     * which is set only on insert.
     */
    void save(Destination destination);

    /**
     * Finds a destination by id. Empty if no row exists for the id.
     */
    Optional<Destination> findById(DestinationId id);

    /**
     * Lists destinations, newest first, with pagination. Destinations have no
     * soft-delete state, so this always reflects the full live set.
     *
     * @param limit  max number of rows to return
     * @param offset number of rows to skip
     */
    List<Destination> findAll(int limit, int offset);

    /**
     * Permanently removes the destination row (hard delete — destinations have
     * no {@code deleted_at} column). Callers are responsible for checking that
     * no task still references this destination before invoking this method;
     * the repository itself does not enforce that rule.
     */
    void deleteById(DestinationId id);
}