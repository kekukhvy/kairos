package dev.kairos.api.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.application.destination.commands.CreateDestinationCommand;
import dev.kairos.application.destination.usecases.*;
import dev.kairos.common.dto.PageResponse;
import dev.kairos.common.dto.destination.CreateDestinationRequest;
import dev.kairos.common.dto.destination.DestinationResponse;
import dev.kairos.common.dto.destination.UpdateDestinationRequest;
import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static dev.kairos.api.destination.DestinationDtoMapper.toResponse;
import static dev.kairos.common.util.helpers.JsonConverter.jsonToString;

/**
 * Javalin request handler for the {@code /api/v1/destinations} resource.
 *
 * <p>Each method maps to one HTTP endpoint and delegates all domain logic to
 * the corresponding use case. Exception-to-status mapping is centralised in
 * {@link dev.kairos.api.GlobalExceptionHandler}:
 * <ul>
 *   <li>400 — {@link dev.kairos.common.exceptions.ValidationException},
 *             {@link IllegalArgumentException} (malformed id),
 *             {@link dev.kairos.domain.destination.exceptions.InvalidDestinationTypeException}</li>
 *   <li>404 — {@link dev.kairos.domain.destination.exceptions.DestinationNotFoundException}</li>
 *   <li>409 — {@link dev.kairos.domain.destination.exceptions.DestinationAlreadyExistsException},
 *             {@link dev.kairos.domain.destination.exceptions.DestinationInUseException}</li>
 * </ul>
 */
public final class DestinationHandler {

    private static final Logger logger = LoggerFactory.getLogger(DestinationHandler.class);

    private final CreateDestinationUseCase createDestinationUseCase;
    private final UpdateDestinationUseCase updateDestinationUseCase;
    private final DeleteDestinationUseCase deleteDestinationUseCase;
    private final ListDestinationsUseCase listDestinationsUseCase;
    private final GetDestinationByIdUseCase getDestinationByIdUseCase;
    private final ObjectMapper objectMapper;

    public DestinationHandler(CreateDestinationUseCase createDestinationUseCase, UpdateDestinationUseCase updateDestinationUseCase, DeleteDestinationUseCase deleteDestinationUseCase, ListDestinationsUseCase listDestinationsUseCase, GetDestinationByIdUseCase getDestinationByIdUseCase, ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.createDestinationUseCase = Objects.requireNonNull(createDestinationUseCase);
        this.updateDestinationUseCase = Objects.requireNonNull(updateDestinationUseCase);
        this.deleteDestinationUseCase = Objects.requireNonNull(deleteDestinationUseCase);
        this.listDestinationsUseCase = Objects.requireNonNull(listDestinationsUseCase);
        this.getDestinationByIdUseCase = Objects.requireNonNull(getDestinationByIdUseCase);
    }

    /**
     * POST /api/v1/destinations → 201 + DestinationResponse
     *
     * @throws dev.kairos.domain.destination.exceptions.DestinationAlreadyExistsException if a destination with the same id already exists (→ 409)
     * @throws dev.kairos.domain.destination.exceptions.InvalidDestinationTypeException   if {@code destinationType} is not a known value (→ 400)
     */
    public void create(Context ctx) {
        CreateDestinationRequest request = ctx.bodyAsClass(CreateDestinationRequest.class);
        logger.debug("Creating destination with id {}, type {}", request.destinationId(), request.destinationType());

        CreateDestinationCommand command = new CreateDestinationCommand(
                request.destinationId(),
                request.destinationType(),
                jsonToString(request.config(), objectMapper)
        );

        Destination result = this.createDestinationUseCase.execute(command);
        logger.info("Destination {} created with type {}", result.destinationId().value(), result.destinationType());
        ctx.status(HttpStatus.CREATED).json(toResponse(result, objectMapper));
    }

    /**
     * PUT /api/v1/destinations/{id} → 200 + DestinationResponse
     *
     * <p>Only {@code config} may be changed; {@code type} and {@code createdAt} are
     * immutable after creation.
     *
     * @throws dev.kairos.domain.destination.exceptions.DestinationNotFoundException if no destination exists for {@code id} (→ 404)
     */
    public void update(Context ctx) {
        DestinationId id = DestinationId.of(ctx.pathParam("id"));
        logger.debug("Updating destination {}", id.value());

        UpdateDestinationRequest request = ctx.bodyAsClass(UpdateDestinationRequest.class);

        Destination destination = this.updateDestinationUseCase.execute(id,
                jsonToString(request.config(), objectMapper));

        logger.info("Destination {} updated", destination.destinationId().value());
        ctx.status(HttpStatus.OK).json(toResponse(destination, objectMapper));
    }

    /**
     * DELETE /api/v1/destinations/{id} → 204
     *
     * <p>Idempotent: deleting an already-absent destination is not an error.
     * Deletion is blocked while any task still references this destination.
     *
     * @throws dev.kairos.domain.destination.exceptions.DestinationInUseException if at least one task still references this destination (→ 409)
     */
    public void delete(Context ctx) {
        DestinationId id = DestinationId.of(ctx.pathParam("id"));
        logger.debug("Deleting destination {}", id.value());

        this.deleteDestinationUseCase.execute(id);

        logger.info("Destination {} deleted", id.value());
        ctx.status(HttpStatus.NO_CONTENT);
    }

    /**
     * GET /api/v1/destinations?limit=20&offset=0 → 200 + PageResponse
     *
     * <p>Fetches {@code limit + 1} rows to determine whether a next page exists,
     * then trims the result back to {@code limit} items. The {@code hasNext} flag
     * in the response lets clients page forward without an extra count query.
     */
    public void list(Context ctx) {
        Pagination pagination = Pagination.of(
                ctx.queryParamAsClass("limit", Integer.class).allowNullable().get(),
                ctx.queryParamAsClass("offset", Integer.class).allowNullable().get()
        );
        logger.debug("Listing destinations with limit {} offset {}", pagination.limit(), pagination.offset());

        Pagination fetchPagination = Pagination.of(pagination.limit() + 1, pagination.offset());

        List<Destination> destinations = this.listDestinationsUseCase.execute(fetchPagination);
        boolean hasNext = destinations.size() > pagination.limit();
        List<Destination> pageItems = hasNext ? destinations.subList(0, pagination.limit()) : destinations;

        List<DestinationResponse> list = pageItems.stream()
                .map(destination -> DestinationDtoMapper.toResponse(destination, objectMapper))
                .toList();

        ctx.status(HttpStatus.OK)
                .json(new PageResponse<>(list, pagination.limit(), pagination.offset(), hasNext));
    }

    /**
     * GET /api/v1/destinations/{id} → 200 + DestinationResponse
     *
     * @throws dev.kairos.domain.destination.exceptions.DestinationNotFoundException if no destination exists for {@code id} (→ 404)
     */
    public void getById(Context ctx) {
        DestinationId id = DestinationId.of(ctx.pathParam("id"));
        logger.debug("Fetching destination {}", id.value());

        Destination destination = this.getDestinationByIdUseCase.execute(id);

        ctx.status(HttpStatus.OK).json(toResponse(destination, objectMapper));
    }
}
