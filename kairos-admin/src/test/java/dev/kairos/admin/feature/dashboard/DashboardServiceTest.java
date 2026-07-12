package dev.kairos.admin.feature.dashboard;

import dev.kairos.admin.feature.schedule.ScheduleService;
import dev.kairos.admin.feature.task.TaskService;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final String SERVICE_BILLING = "billing";
    private static final String SERVICE_EMAIL = "email";
    private static final String SERVICE_PAYMENTS = "payments";

    @Mock
    private TaskService taskService;

    @Mock
    private ScheduleService scheduleService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(taskService, scheduleService);
    }

    // --- totalTasks / activeTasks ---

    @Test
    void load_allTasksActive_totalAndActiveCountsMatch() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<TaskDto> tasks = List.of(
                task(id1, SERVICE_BILLING, "invoice", true),
                task(id2, SERVICE_EMAIL, "digest", true));
        stubTasksAndSchedules(tasks, List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.totalTasks()).isEqualTo(2);
        assertThat(stats.activeTasks()).isEqualTo(2);
    }

    @Test
    void load_mixedActiveInactiveTasks_activeCountReflectsActiveFlagOnly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        List<TaskDto> tasks = List.of(
                task(id1, SERVICE_BILLING, "invoice", true),
                task(id2, SERVICE_EMAIL, "digest", false),
                task(id3, SERVICE_PAYMENTS, "charge", false));
        stubTasksAndSchedules(tasks, List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.totalTasks()).isEqualTo(3);
        assertThat(stats.activeTasks()).isEqualTo(1);
    }

    @Test
    void load_noTasks_countsAreZero() {
        stubTasksAndSchedules(List.of(), List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.totalTasks()).isZero();
        assertThat(stats.activeTasks()).isZero();
    }

    // --- totalSchedules / activeSchedules ---

    @Test
    void load_mixedActiveInactiveSchedules_activeCountReflectsActiveFlagOnly() {
        UUID taskId = UUID.randomUUID();
        List<TaskDto> tasks = List.of(task(taskId, SERVICE_BILLING, "invoice", true));
        List<ScheduleResponse> schedules = List.of(
                schedule(taskId, true),
                schedule(taskId, false),
                schedule(taskId, true));
        stubTasksAndSchedules(tasks, schedules);

        DashboardStats stats = dashboardService.load();

        assertThat(stats.totalSchedules()).isEqualTo(3);
        assertThat(stats.activeSchedules()).isEqualTo(2);
    }

    @Test
    void load_noSchedules_scheduleCountsAreZero() {
        UUID taskId = UUID.randomUUID();
        List<TaskDto> tasks = List.of(task(taskId, SERVICE_BILLING, "invoice", true));
        stubTasksAndSchedules(tasks, List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.totalSchedules()).isZero();
        assertThat(stats.activeSchedules()).isZero();
    }

    // --- serviceRanking ---

    @Test
    void load_multipleServices_serviceRankingOrderedByTaskCountDescending() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        List<TaskDto> tasks = List.of(
                task(id1, SERVICE_EMAIL, "digest", true),
                task(id2, SERVICE_BILLING, "invoice", true),
                task(id3, SERVICE_BILLING, "refund", true));
        stubTasksAndSchedules(tasks, List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.serviceRanking()).extracting(DashboardStats.Ranked::label)
                .containsExactly(SERVICE_BILLING, SERVICE_EMAIL);
        assertThat(stats.serviceRanking()).extracting(DashboardStats.Ranked::count)
                .containsExactly(2L, 1L);
    }

    @Test
    void load_moreThanFiveServices_serviceRankingLimitedToFive() {
        List<TaskDto> tasks = List.of(
                task(UUID.randomUUID(), "svc-a", "t1", true),
                task(UUID.randomUUID(), "svc-b", "t2", true),
                task(UUID.randomUUID(), "svc-c", "t3", true),
                task(UUID.randomUUID(), "svc-d", "t4", true),
                task(UUID.randomUUID(), "svc-e", "t5", true),
                task(UUID.randomUUID(), "svc-f", "t6", true));
        stubTasksAndSchedules(tasks, List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.serviceRanking()).hasSize(DashboardStats.MAX_LEADERBOARD_SIZE);
    }

    @Test
    void load_singleService_serviceRankingHasOneEntry() {
        UUID taskId = UUID.randomUUID();
        List<TaskDto> tasks = List.of(task(taskId, SERVICE_BILLING, "invoice", true));
        stubTasksAndSchedules(tasks, List.of());

        DashboardStats stats = dashboardService.load();

        assertThat(stats.serviceRanking()).hasSize(1);
        assertThat(stats.serviceRanking().getFirst().label()).isEqualTo(SERVICE_BILLING);
        assertThat(stats.serviceRanking().getFirst().count()).isEqualTo(1L);
    }

    // --- scheduleRanking ---

    @Test
    void load_scheduleRankingGroupedByTaskLabel_sortedDescending() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();
        List<TaskDto> tasks = List.of(
                task(taskId1, SERVICE_BILLING, "invoice", true),
                task(taskId2, SERVICE_EMAIL, "digest", true));
        List<ScheduleResponse> schedules = List.of(
                schedule(taskId1, true),
                schedule(taskId2, true),
                schedule(taskId2, true));
        stubTasksAndSchedules(tasks, schedules);

        DashboardStats stats = dashboardService.load();

        assertThat(stats.scheduleRanking()).extracting(DashboardStats.Ranked::label)
                .containsExactly(
                        SERVICE_EMAIL + " / digest",
                        SERVICE_BILLING + " / invoice");
        assertThat(stats.scheduleRanking()).extracting(DashboardStats.Ranked::count)
                .containsExactly(2L, 1L);
    }

    @Test
    void load_scheduleWithUnknownTaskId_labelFallsBackToTaskIdString() {
        UUID knownTaskId = UUID.randomUUID();
        UUID unknownTaskId = UUID.randomUUID();
        List<TaskDto> tasks = List.of(task(knownTaskId, SERVICE_BILLING, "invoice", true));
        List<ScheduleResponse> schedules = List.of(schedule(unknownTaskId, true));
        stubTasksAndSchedules(tasks, schedules);

        DashboardStats stats = dashboardService.load();

        assertThat(stats.scheduleRanking()).hasSize(1);
        assertThat(stats.scheduleRanking().getFirst().label())
                .isEqualTo(unknownTaskId.toString());
    }

    @Test
    void load_moreThanFiveTasks_scheduleRankingLimitedToFive() {
        List<TaskDto> tasks = List.of(
                task(UUID.randomUUID(), "svc-a", "t1", true),
                task(UUID.randomUUID(), "svc-b", "t2", true),
                task(UUID.randomUUID(), "svc-c", "t3", true),
                task(UUID.randomUUID(), "svc-d", "t4", true),
                task(UUID.randomUUID(), "svc-e", "t5", true),
                task(UUID.randomUUID(), "svc-f", "t6", true));
        List<ScheduleResponse> schedules = tasks.stream()
                .map(t -> schedule(t.id(), true))
                .toList();
        stubTasksAndSchedules(tasks, schedules);

        DashboardStats stats = dashboardService.load();

        assertThat(stats.scheduleRanking()).hasSize(DashboardStats.MAX_LEADERBOARD_SIZE);
    }

    // --- error resilience ---

    @Test
    void load_taskServiceThrows_returnsEmptyStats() {
        when(taskService.list()).thenThrow(new RuntimeException("API down"));

        DashboardStats stats = dashboardService.load();

        assertThat(stats).isEqualTo(DashboardStats.empty());
    }

    @Test
    void load_scheduleServiceThrows_returnsEmptyStats() {
        UUID taskId = UUID.randomUUID();
        when(taskService.list()).thenReturn(List.of(task(taskId, SERVICE_BILLING, "invoice", true)));
        when(scheduleService.listForTasks(anyCollection())).thenThrow(new RuntimeException("API down"));

        DashboardStats stats = dashboardService.load();

        assertThat(stats).isEqualTo(DashboardStats.empty());
    }

    // --- helpers ---

    private void stubTasksAndSchedules(List<TaskDto> tasks, List<ScheduleResponse> schedules) {
        when(taskService.list()).thenReturn(tasks);
        Set<UUID> taskIds = tasks.stream().map(TaskDto::id).collect(java.util.stream.Collectors.toSet());
        when(scheduleService.listForTasks(taskIds)).thenReturn(schedules);
    }

    private static TaskDto task(UUID id, String service, String name, boolean active) {
        return new TaskDto(id, service, name, null, active,
                "dest-1", "Event", null, 5000, false, NOW, NOW);
    }

    private static ScheduleResponse schedule(UUID taskId, boolean active) {
        return new ScheduleResponse(UUID.randomUUID(), taskId, "CRON", "label",
                null, "0 0 * * *", null, "UTC", active, NOW, NOW);
    }
}
