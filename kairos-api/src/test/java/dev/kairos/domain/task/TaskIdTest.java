package dev.kairos.domain.task;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskIdTest {

    private static final UUID VALID_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void constructor_withValidUuid_createsTaskId() {
        TaskId taskId = new TaskId(VALID_UUID);

        assertNotNull(taskId);
        assertEquals(VALID_UUID, taskId.value());
    }

    @Test
    void constructor_withNullUuid_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TaskId(null));
    }

    @Test
    void equality_twoTaskIdsWithSameUuid_areEqual() {
        TaskId first = new TaskId(VALID_UUID);
        TaskId second = new TaskId(VALID_UUID);

        assertEquals(first, second);
    }
}
