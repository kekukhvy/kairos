package dev.kairos.admin.shared.demo;

import dev.kairos.admin.feature.destination.DestinationService;
import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.schedule.ScheduleService;
import dev.kairos.admin.feature.task.TaskService;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Populates the running Kairos instance with a handful of demo tasks and
 * schedules on startup, so the admin UI has representative data to explore
 * (one task with two different schedulers, plus tasks covering the ONCE, CRON
 * and FIXED schedule types).
 *
 * <p>Runs only when {@code kairos.demo.seed=true} — the "create demo or not"
 * switch — and stays off by default so it never touches a real environment.
 * It talks to the same REST API as the rest of the admin (via the feature
 * services), so seeding goes through the exact create paths a user would.
 * Failures are logged and swallowed: a demo seed must never block startup.
 */
@Component
@ConditionalOnProperty(prefix = "kairos.demo", name = "seed", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_DESTINATION_ID = "demo-webhook";
    private static final String DEMO_DESTINATION_TYPE = "WEBHOOK";
    private static final Map<String, Object> DEMO_DESTINATION_CONFIG =
            Map.of("url", "https://example.com/hooks/demo");

    private static final String SERVICE_NAME = "demo-service";
    private static final int DEFAULT_TIMEOUT_MS = 30_000;
    private static final String UTC = "UTC";

    private final DestinationService destinations;
    private final TaskService tasks;
    private final ScheduleService schedules;

    public DemoDataSeeder(DestinationService destinations, TaskService tasks, ScheduleService schedules) {
        this.destinations = destinations;
        this.tasks = tasks;
        this.schedules = schedules;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Seeding demo data (kairos.demo.seed=true)");
        try {
            seed();
            logger.info("Demo data seeded successfully");
        } catch (RuntimeException ex) {
            logger.warn("Demo data seeding failed — continuing startup", ex);
        }
    }

    private void seed() {
        String destinationId = ensureDestination();
        seedTaskWithTwoSchedules(destinationId);
        seedOnceTask(destinationId);
        seedWeekdayCronTask(destinationId);
    }

    /** One destination all demo tasks deliver to; reused if it already exists. */
    private String ensureDestination() {
        return destinations.list().stream()
                .map(DestinationDTO::destinationId)
                .filter(DEMO_DESTINATION_ID::equals)
                .findFirst()
                .orElseGet(this::createDestination);
    }

    private String createDestination() {
        DestinationDTO created = destinations.create(new CreateDestinationRequest(
                DEMO_DESTINATION_ID, DEMO_DESTINATION_TYPE, DEMO_DESTINATION_CONFIG));
        return created.destinationId();
    }

    /** Scenario: a single task driven by two different schedulers (CRON + FIXED). */
    private void seedTaskWithTwoSchedules(String destinationId) {
        UUID taskId = createTask(destinationId, "nightly-report", "reports.generated");
        schedules.create(taskId, cron("Every night at 02:00", "0 0 2 * * ?"));
        schedules.create(taskId, fixed("Every 5 minutes", 300));
    }

    /** Scenario: a task that fires exactly once, an hour from startup. */
    private void seedOnceTask(String destinationId) {
        UUID taskId = createTask(destinationId, "one-off-migration", "migration.run");
        Instant runAt = Instant.now().plus(1, ChronoUnit.HOURS);
        schedules.create(taskId, new CreateScheduleRequest(
                "ONCE", "One hour from startup", runAt, null, null, UTC));
    }

    /** Scenario: a task on a weekday-business-hours CRON schedule. */
    private void seedWeekdayCronTask(String destinationId) {
        UUID taskId = createTask(destinationId, "business-hours-sync", "sync.triggered");
        schedules.create(taskId, cron("Weekdays at 09:00", "0 0 9 * * MON-FRI"));
    }

    private UUID createTask(String destinationId, String name, String eventName) {
        TaskDto task = tasks.create(new CreateTaskRequest(
                SERVICE_NAME, name, "Demo task: " + name, true,
                destinationId, eventName, Map.of("demo", true),
                DEFAULT_TIMEOUT_MS, false));
        return task.id();
    }

    private static CreateScheduleRequest cron(String label, String expression) {
        return new CreateScheduleRequest("CRON", label, null, expression, null, UTC);
    }

    private static CreateScheduleRequest fixed(String label, int intervalSeconds) {
        return new CreateScheduleRequest("FIXED", label, null, null, intervalSeconds, null);
    }
}
