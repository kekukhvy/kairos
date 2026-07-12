package dev.kairos.admin.feature.dashboard;

import dev.kairos.admin.feature.schedule.ScheduleService;
import dev.kairos.admin.feature.task.TaskService;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes {@link DashboardStats} from the tasks and schedules already exposed
 * by the API. Execution-derived metrics (throughput, success rate, next run,
 * failures) are not computed here — no execution data exists yet.
 */
@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final TaskService taskService;
    private final ScheduleService scheduleService;

    public DashboardService(TaskService taskService, ScheduleService scheduleService) {
        this.taskService = taskService;
        this.scheduleService = scheduleService;
    }

    /**
     * Loads tasks and their schedules and derives the dashboard metrics. On any
     * failure it logs and returns {@link DashboardStats#empty()} so the view can
     * still render.
     *
     * @return the computed metrics, never {@code null}
     */
    public DashboardStats load() {
        try {
            List<TaskDto> tasks = taskService.list();
            Map<UUID, String> taskLabels = tasks.stream()
                    .collect(Collectors.toMap(TaskDto::id, TaskDto::label, (a, b) -> a));
            List<ScheduleResponse> schedules = scheduleService.listForTasks(taskLabels.keySet());
            DashboardStats result = build(tasks, schedules, taskLabels);
            logger.debug("Dashboard stats loaded: tasks={}/{}, schedules={}/{}",
                    result.activeTasks(), result.totalTasks(),
                    result.activeSchedules(), result.totalSchedules());
            return result;
        } catch (RuntimeException ex) {
            logger.error("Failed to compute dashboard stats", ex);
            return DashboardStats.empty();
        }
    }

    private DashboardStats build(List<TaskDto> tasks, List<ScheduleResponse> schedules,
                                 Map<UUID, String> taskLabels) {
        int activeTasks = (int) tasks.stream().filter(TaskDto::active).count();
        int activeSchedules = (int) schedules.stream().filter(ScheduleResponse::active).count();
        return new DashboardStats(
                tasks.size(),
                activeTasks,
                schedules.size(),
                activeSchedules,
                rankBy(tasks, TaskDto::service),
                rankSchedulesByTask(schedules, taskLabels));
    }

    private List<DashboardStats.Ranked> rankBy(List<TaskDto> tasks,
                                               Function<TaskDto, String> classifier) {
        return topRanked(tasks.stream().collect(Collectors.groupingBy(classifier, Collectors.counting())));
    }

    private List<DashboardStats.Ranked> rankSchedulesByTask(List<ScheduleResponse> schedules,
                                                            Map<UUID, String> taskLabels) {
        Map<String, Long> counts = schedules.stream()
                .collect(Collectors.groupingBy(
                        s -> taskLabels.getOrDefault(s.taskId(), String.valueOf(s.taskId())),
                        Collectors.counting()));
        return topRanked(counts);
    }

    private List<DashboardStats.Ranked> topRanked(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(DashboardStats.MAX_LEADERBOARD_SIZE)
                .map(e -> new DashboardStats.Ranked(e.getKey(), e.getValue()))
                .toList();
    }
}
