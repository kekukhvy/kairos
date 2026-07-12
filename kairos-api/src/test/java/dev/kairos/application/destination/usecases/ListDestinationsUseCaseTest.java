package dev.kairos.application.destination.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.destination.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.kairos.application.destination.usecases.DestinationBuilder.buildDefault;
import static dev.kairos.application.destination.usecases.DestinationBuilder.withId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListDestinationsUseCaseTest {

    private static final int PAGE_LIMIT = 10;
    private static final int PAGE_OFFSET = 5;

    private InMemoryDestinationRepository destinationRepository;
    private ListDestinationsUseCase useCase;

    @BeforeEach
    void setUp() {
        destinationRepository = new InMemoryDestinationRepository();
        useCase = new ListDestinationsUseCase(destinationRepository);
    }

    // --- happy path ---

    @Test
    void execute_withSeededDestination_returnsIt() {
        destinationRepository.seed(buildDefault());

        List<Destination> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(1, result.size());
    }

    @Test
    void execute_withMultipleDestinations_returnsAll() {
        destinationRepository.seed(buildDefault());
        destinationRepository.seed(withId("dest-sqs-2"));

        List<Destination> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(2, result.size());
    }

    @Test
    void execute_withEmptyRepository_returnsEmptyList() {
        List<Destination> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(0, result.size());
    }

    // --- pagination is forwarded ---

    @Test
    void execute_forwardsPaginationLimitToRepository() {
        useCase.execute(new Pagination(PAGE_LIMIT, PAGE_OFFSET));

        assertEquals(PAGE_LIMIT, destinationRepository.lastFindAllLimit);
    }

    @Test
    void execute_forwardsPaginationOffsetToRepository() {
        useCase.execute(new Pagination(PAGE_LIMIT, PAGE_OFFSET));

        assertEquals(PAGE_OFFSET, destinationRepository.lastFindAllOffset);
    }

    // --- null guard ---

    @Test
    void execute_withNullPagination_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
