package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code TaskForm}'s service+name uniqueness check: on blur of the
 * {@code name} (or {@code service}) field, and again in {@link
 * TaskForm}'s submit-path {@code validate()}, an entered pair that matches an
 * already-loaded task is rejected with {@link TaskText#VALIDATION_DUPLICATE_SERVICE_NAME}.
 */
class TaskFormTest {

    private static final UUID EXISTING_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String SERVICE = "billing";
    private static final String NAME = "invoice-sync";
    private static final String OTHER_SERVICE = "shipping";
    private static final String OTHER_NAME = "label-print";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void forCreate_blurOnDuplicatePair_marksNameFieldInvalidWithDuplicateMessage() {
        TaskForm form = TaskForm.forCreate(jsonMapper, List.of(), List.of(existingTask()), noopCreate());

        fillPair(form, SERVICE, NAME);
        blur(form.name());

        assertThat(form.name().isInvalid()).isTrue();
        assertThat(form.name().getErrorMessage()).isEqualTo(TaskText.VALIDATION_DUPLICATE_SERVICE_NAME);
    }

    @Test
    void forCreate_blurOnUniquePair_leavesNameFieldValid() {
        TaskForm form = TaskForm.forCreate(jsonMapper, List.of(), List.of(existingTask()), noopCreate());

        fillPair(form, OTHER_SERVICE, OTHER_NAME);
        blur(form.name());

        assertThat(form.name().isInvalid()).isFalse();
    }

    @Test
    void forCreate_blurOnServiceFieldAfterEditingService_alsoDetectsDuplicate() {
        TaskForm form = TaskForm.forCreate(jsonMapper, List.of(), List.of(existingTask()), noopCreate());

        fillPair(form, OTHER_SERVICE, NAME);
        blur(form.name());
        assertThat(form.name().isInvalid()).isFalse();

        form.service().setValue(SERVICE);
        blur(form.service());

        assertThat(form.name().isInvalid()).isTrue();
    }

    @Test
    void forEdit_blurWithSameTaskOwnServiceAndName_notFlaggedAsDuplicate() {
        TaskDto editing = existingTask();
        TaskForm form = TaskForm.forEdit(jsonMapper, List.of(), List.of(editing, otherTask()), editing, noopUpdate());

        blur(form.name());

        assertThat(form.name().isInvalid()).isFalse();
    }

    @Test
    void forEdit_blurWithAnotherTasksServiceAndName_flaggedAsDuplicate() {
        TaskDto editing = existingTask();
        TaskForm form = TaskForm.forEdit(jsonMapper, List.of(), List.of(editing, otherTask()), editing, noopUpdate());

        form.name().setValue(OTHER_NAME);
        blur(form.name());

        assertThat(form.name().isInvalid()).isTrue();
        assertThat(form.name().getErrorMessage()).isEqualTo(TaskText.VALIDATION_DUPLICATE_SERVICE_NAME);
    }

    @Test
    void validate_duplicatePairOnSubmitPath_returnsFalse() {
        TaskForm form = TaskForm.forCreate(jsonMapper, List.of(), List.of(existingTask()), noopCreate());
        fillPair(form, SERVICE, NAME);
        form.eventName().setValue("evt");
        form.destinationId().setValue("dest-1");
        form.timeoutMs().setValue(1000);

        assertThat(form.validate()).isFalse();
    }

    @Test
    void validate_uniquePairOnSubmitPath_returnsTrue() {
        TaskForm form = TaskForm.forCreate(jsonMapper, List.of(), List.of(existingTask()), noopCreate());
        fillPair(form, OTHER_SERVICE, OTHER_NAME);
        form.eventName().setValue("evt");
        form.destinationId().setValue("dest-1");
        form.timeoutMs().setValue(1000);

        assertThat(form.validate()).isTrue();
    }

    // --- helpers ---

    private static void fillPair(TaskForm form, String service, String name) {
        form.service().setValue(service);
        form.name().setValue(name);
    }

    private static void blur(TextField field) {
        ComponentUtil.fireEvent(field, new BlurNotifier.BlurEvent<>(field, false));
    }

    private static TaskDto existingTask() {
        return new TaskDto(EXISTING_ID, SERVICE, NAME, null, true,
                "dest-1", "InvoiceReady", null, 5000, false, NOW, NOW, 0);
    }

    private static TaskDto otherTask() {
        return new TaskDto(OTHER_ID, SERVICE, OTHER_NAME, null, true,
                "dest-1", "LabelPrinted", null, 5000, false, NOW, NOW, 0);
    }

    private static Consumer<CreateTaskRequest> noopCreate() {
        return request -> { };
    }

    private static Consumer<UpdateTaskRequest> noopUpdate() {
        return request -> { };
    }
}
