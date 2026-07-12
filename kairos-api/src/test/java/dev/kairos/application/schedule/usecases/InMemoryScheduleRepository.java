package dev.kairos.application.schedule.usecases;

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
 * In-memory fake for {@link ScheduleRepository}. Stores schedules by id in
 * insertion order. {@link #save} is tracked so tests can assert it was (or was
 * not) called. {@link #seed} inserts a row without incrementing the save counter.
 */
final class InMemoryScheduleRepository implements ScheduleRepository {

    private final Map<ScheduleId, Schedule> store = new LinkedHashMap<>();
    private int saveCallCount = 0;

    @Override
    public void save(Schedule schedule) {
        saveCallCount++;
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

    // ── test helpers ──────────────────────────────────────────────────────────

    int saveCallCount() {
        return saveCallCount;
    }

    /** Seeds a schedule without counting it as a save(). */
    void seed(Schedule schedule) {
        store.put(schedule.id(), schedule);
    }

    boolean contains(ScheduleId id) {
        return store.containsKey(id);
    }
}
