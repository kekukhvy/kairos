package dev.kairos.api;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.task.TaskId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory fake {@link ScheduleRepository} for API-layer tests. Mirrors the
 * approach of {@link InMemoryTaskRepositoryForApi}: insertion-order store,
 * seed() bypass for pre-populated data.
 */
final class InMemoryScheduleRepositoryForApi implements ScheduleRepository {

    private final Map<ScheduleId, Schedule> store = new LinkedHashMap<>();

    @Override
    public void save(Schedule schedule) {
        store.put(schedule.id(), schedule);
    }

    @Override
    public Optional<Schedule> findById(ScheduleId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Schedule> findByTaskId(TaskId taskId, int limit, int offset) {
        List<Schedule> matching = store.values().stream()
                .filter(s -> s.taskId().equals(taskId))
                .toList();
        int from = Math.min(offset, matching.size());
        int to = Math.min(from + limit, matching.size());
        return new ArrayList<>(matching.subList(from, to));
    }

    @Override
    public void deleteById(ScheduleId id) {
        store.remove(id);
    }

    /** Seeds a schedule directly, bypassing save(). */
    void seed(Schedule schedule) {
        store.put(schedule.id(), schedule);
    }
}
