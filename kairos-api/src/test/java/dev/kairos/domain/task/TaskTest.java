package dev.kairos.domain.task;

import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.DestinationId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static dev.kairos.domain.task.TaskBuilder.DEFAULT_CREATED_AT;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_DESTINATION_ID;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_ID;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_EVENT_NAME;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_NAME;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_SERVICE;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_TIMEOUT_MS;
import static dev.kairos.domain.task.TaskBuilder.DEFAULT_UPDATED_AT;
import static dev.kairos.domain.task.TaskBuilder.buildDefault;
import static dev.kairos.domain.task.TaskBuilder.defaultEdit;
import static dev.kairos.domain.task.TaskBuilder.defaults;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    private static final Instant UPDATE_TIME = Instant.parse("2026-06-01T12:00:00Z");
    private static final Instant DELETE_TIME = Instant.parse("2026-01-02T00:00:00Z");
    private static final String TOO_LONG_SERVICE = "a".repeat(Task.MAX_SERVICE_LENGTH + 1);
    private static final String TOO_LONG_NAME = "a".repeat(Task.MAX_NAME_LENGTH + 1);
    private static final String TOO_LONG_EVENT_NAME = "a".repeat(Task.MAX_EVENT_NAME_LENGTH + 1);
    private static final int ZERO_TIMEOUT_MS = 0;
    private static final int NEGATIVE_TIMEOUT_MS = -1;

    // --- builder happy path ---

    @Test
    void build_withAllValidFields_succeeds() {
        assertDoesNotThrow(() -> buildDefault());
    }

    @Test
    void build_withNullableFieldsOmitted_succeeds() {
        assertDoesNotThrow(() -> defaults()
                .description(null)
                .payload(null)
                .deletedAt(null)
                .build());
    }

    @Test
    void build_withDeletedAtSet_isDeletedReturnsTrue() {
        Task task = defaults().deletedAt(DELETE_TIME).build();

        assertTrue(task.isDeleted());
    }

    @Test
    void build_withoutDeletedAt_isDeletedReturnsFalse() {
        Task task = buildDefault();

        assertFalse(task.isDeleted());
    }

    // --- builder null-guard rejections ---

    @Test
    void build_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaults().id(null).build());
    }

    @Test
    void build_withNullDestinationId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaults().destinationId(null).build());
    }

    @Test
    void build_withNullCreatedAt_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaults().createdAt(null).build());
    }

    @Test
    void build_withNullUpdatedAt_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaults().updatedAt(null).build());
    }

    // --- builder validation failures ---

    @Test
    void build_withNullService_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().service(null).build());
    }

    @Test
    void build_withBlankService_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().service("   ").build());
    }

    @Test
    void build_withServiceExceedingMaxLength_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().service(TOO_LONG_SERVICE).build());
    }

    @Test
    void build_withNullName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().name(null).build());
    }

    @Test
    void build_withBlankName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().name("   ").build());
    }

    @Test
    void build_withNameExceedingMaxLength_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().name(TOO_LONG_NAME).build());
    }

    @Test
    void build_withNullEventName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().eventName(null).build());
    }

    @Test
    void build_withBlankEventName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().eventName("   ").build());
    }

    @Test
    void build_withEventNameExceedingMaxLength_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().eventName(TOO_LONG_EVENT_NAME).build());
    }

    @Test
    void build_withZeroTimeoutMs_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().timeoutMs(ZERO_TIMEOUT_MS).build());
    }

    @Test
    void build_withNegativeTimeoutMs_throwsValidationException() {
        assertThrows(ValidationException.class, () -> defaults().timeoutMs(NEGATIVE_TIMEOUT_MS).build());
    }

    // --- update happy path ---

    @Test
    void update_withValidEdit_doesNotThrow() {
        Task task = buildDefault();

        assertDoesNotThrow(() -> task.update(defaultEdit(), UPDATE_TIME));
    }

    @Test
    void update_withValidEdit_taskIsNotDeleted() {
        Task task = buildDefault();

        task.update(defaultEdit(), UPDATE_TIME);

        assertFalse(task.isDeleted());
    }

    @Test
    void update_withValidEdit_changesAllEditableFields() {
        Task task = buildDefault();
        TaskEdit edit = defaultEdit();

        task.update(edit, UPDATE_TIME);

        assertEquals(edit.name(), task.name());
        assertEquals(edit.description(), task.description());
        assertEquals(edit.active(), task.active());
        assertEquals(edit.destinationId(), task.destinationId());
        assertEquals(edit.eventName(), task.eventName());
        assertEquals(edit.payload(), task.payload());
        assertEquals(edit.timeoutMs(), task.timeoutMs());
        assertEquals(edit.supportsRetry(), task.supportsRetry());
    }

    @Test
    void update_withValidEdit_bumpsUpdatedAt() {
        Task task = buildDefault();

        task.update(defaultEdit(), UPDATE_TIME);

        assertEquals(UPDATE_TIME, task.updatedAt());
    }

    @Test
    void update_withValidEdit_doesNotChangeImmutableFields() {
        Task task = buildDefault();

        task.update(defaultEdit(), UPDATE_TIME);

        assertEquals(DEFAULT_ID, task.id());
        assertEquals(DEFAULT_SERVICE, task.service());
        assertEquals(DEFAULT_CREATED_AT, task.createdAt());
    }

    // --- update validation failures ---

    @Test
    void update_withBlankName_throwsValidationException() {
        Task task = buildDefault();
        TaskEdit editWithBlankName = new TaskEdit(
                "   ",
                null,
                true,
                DEFAULT_DESTINATION_ID,
                DEFAULT_EVENT_NAME,
                null,
                DEFAULT_TIMEOUT_MS,
                false
        );

        assertThrows(ValidationException.class, () -> task.update(editWithBlankName, UPDATE_TIME));
    }

    @Test
    void update_withNameExceedingMaxLength_throwsValidationException() {
        Task task = buildDefault();
        TaskEdit editWithLongName = new TaskEdit(
                TOO_LONG_NAME,
                null,
                true,
                DEFAULT_DESTINATION_ID,
                DEFAULT_EVENT_NAME,
                null,
                DEFAULT_TIMEOUT_MS,
                false
        );

        assertThrows(ValidationException.class, () -> task.update(editWithLongName, UPDATE_TIME));
    }

    @Test
    void update_withBlankEventName_throwsValidationException() {
        Task task = buildDefault();
        TaskEdit editWithBlankMessageType = new TaskEdit(
                DEFAULT_NAME,
                null,
                true,
                DEFAULT_DESTINATION_ID,
                "   ",
                null,
                DEFAULT_TIMEOUT_MS,
                false
        );

        assertThrows(ValidationException.class, () -> task.update(editWithBlankMessageType, UPDATE_TIME));
    }

    @Test
    void update_withZeroTimeoutMs_throwsValidationException() {
        Task task = buildDefault();
        TaskEdit editWithZeroTimeout = new TaskEdit(
                DEFAULT_NAME,
                null,
                true,
                DEFAULT_DESTINATION_ID,
                DEFAULT_EVENT_NAME,
                null,
                ZERO_TIMEOUT_MS,
                false
        );

        assertThrows(ValidationException.class, () -> task.update(editWithZeroTimeout, UPDATE_TIME));
    }

    @Test
    void update_withNullDestinationId_throwsNullPointerException() {
        Task task = buildDefault();
        TaskEdit editWithNullDest = new TaskEdit(
                DEFAULT_NAME,
                null,
                true,
                null,
                DEFAULT_EVENT_NAME,
                null,
                DEFAULT_TIMEOUT_MS,
                false
        );

        assertThrows(NullPointerException.class, () -> task.update(editWithNullDest, UPDATE_TIME));
    }

    @Test
    void update_withNullEdit_throwsNullPointerException() {
        Task task = buildDefault();

        assertThrows(NullPointerException.class, () -> task.update(null, UPDATE_TIME));
    }

    @Test
    void update_withNullNow_throwsNullPointerException() {
        Task task = buildDefault();

        assertThrows(NullPointerException.class, () -> task.update(defaultEdit(), null));
    }

    // --- soft-delete invariant ---

    @Test
    void update_onAlreadyDeletedTask_throwsTaskAlreadyDeletedException() {
        Task deletedTask = defaults().deletedAt(DELETE_TIME).build();

        assertThrows(TaskAlreadyDeletedException.class,
                () -> deletedTask.update(defaultEdit(), UPDATE_TIME));
    }

    @Test
    void update_onAlreadyDeletedTask_exceptionCarriesCorrectTaskId() {
        Task deletedTask = defaults().deletedAt(DELETE_TIME).build();

        TaskAlreadyDeletedException thrown = assertThrows(TaskAlreadyDeletedException.class,
                () -> deletedTask.update(defaultEdit(), UPDATE_TIME));

        assertEquals(DEFAULT_ID, thrown.taskId());
    }

    @Test
    void isDeleted_onFreshTask_returnsFalse() {
        Task task = buildDefault();

        assertFalse(task.isDeleted());
    }

    @Test
    void isDeleted_onTaskBuiltWithDeletedAt_returnsTrue() {
        Task task = defaults().deletedAt(DELETE_TIME).build();

        assertTrue(task.isDeleted());
    }

    // --- softDelete() ---

    @Test
    void softDelete_onLiveTask_setsDeletedAt() {
        Task task = buildDefault();

        task.softDelete(DELETE_TIME);

        assertEquals(DELETE_TIME, task.deletedAt());
    }

    @Test
    void softDelete_onLiveTask_setsUpdatedAt() {
        Task task = buildDefault();

        task.softDelete(DELETE_TIME);

        assertEquals(DELETE_TIME, task.updatedAt());
    }

    @Test
    void softDelete_onLiveTask_isDeletedReturnsTrue() {
        Task task = buildDefault();

        task.softDelete(DELETE_TIME);

        assertTrue(task.isDeleted());
    }

    @Test
    void softDelete_calledTwice_throwsTaskAlreadyDeletedException() {
        Task task = buildDefault();
        task.softDelete(DELETE_TIME);

        assertThrows(TaskAlreadyDeletedException.class, () -> task.softDelete(UPDATE_TIME));
    }

    @Test
    void softDelete_withNullNow_throwsNullPointerException() {
        Task task = buildDefault();

        assertThrows(NullPointerException.class, () -> task.softDelete(null));
    }

    @Test
    void update_afterSoftDelete_throwsTaskAlreadyDeletedException() {
        Task task = buildDefault();
        task.softDelete(DELETE_TIME);

        assertThrows(TaskAlreadyDeletedException.class,
                () -> task.update(defaultEdit(), UPDATE_TIME));
    }

    // --- TaskAlreadyDeletedException ---

    @Test
    void taskAlreadyDeletedException_messageContainsTaskIdValue() {
        TaskId taskId = new TaskId(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"));

        TaskAlreadyDeletedException exception = new TaskAlreadyDeletedException(taskId);

        assertTrue(exception.getMessage().contains(taskId.value().toString()));
    }

    @Test
    void taskAlreadyDeletedException_taskIdAccessorReturnsOriginalId() {
        TaskAlreadyDeletedException exception = new TaskAlreadyDeletedException(DEFAULT_ID);

        assertEquals(DEFAULT_ID, exception.taskId());
    }

    // --- TaskId factory methods ---

    @Test
    void taskId_newId_returnsNonNullUniqueId() {
        TaskId first = TaskId.newId();
        TaskId second = TaskId.newId();

        assertNotNull(first);
        assertNotNull(second);
        assertFalse(first.equals(second));
    }

    @Test
    void taskId_of_wrapsProvidedUuid() {
        UUID uuid = UUID.fromString("11111111-0000-0000-0000-000000000001");

        TaskId taskId = TaskId.of(uuid);

        assertEquals(uuid, taskId.value());
    }

    // --- TaskNotFoundException ---

    @Test
    void taskNotFoundException_messageContainsTaskIdValue() {
        TaskId taskId = new TaskId(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002"));

        TaskNotFoundException exception = new TaskNotFoundException(taskId);

        assertTrue(exception.getMessage().contains(taskId.value().toString()));
    }

    @Test
    void taskNotFoundException_taskIdAccessorReturnsOriginalId() {
        TaskNotFoundException exception = new TaskNotFoundException(DEFAULT_ID);

        assertEquals(DEFAULT_ID, exception.taskId());
    }
}
