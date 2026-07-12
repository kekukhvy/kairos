package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleWhenTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-07-09T12:00:00Z");

    @Test
    void describe_cron_returnsCronExpression() {
        ScheduleResponse schedule = schedule("CRON", null, "0 0 * * *", null);

        assertThat(ScheduleWhen.describe(schedule)).isEqualTo("0 0 * * *");
    }

    @Test
    void describe_fixed_returnsIntervalWithUnit() {
        ScheduleResponse schedule = schedule("FIXED", null, null, 60);

        assertThat(ScheduleWhen.describe(schedule)).isEqualTo("60 s");
    }

    @Test
    void describe_once_returnsFormattedRunAt() {
        ScheduleResponse schedule = schedule("ONCE", NOW, null, null);

        assertThat(ScheduleWhen.describe(schedule)).isNotBlank();
        assertThat(ScheduleWhen.describe(schedule)).isNotEqualTo(ScheduleText.WHEN_EMPTY);
    }

    @Test
    void describe_missingWhenField_returnsPlaceholder() {
        ScheduleResponse schedule = schedule("CRON", null, null, null);

        assertThat(ScheduleWhen.describe(schedule)).isEqualTo(ScheduleText.WHEN_EMPTY);
    }

    @Test
    void describe_unknownType_returnsPlaceholder() {
        ScheduleResponse schedule = schedule("WEEKLY", null, null, null);

        assertThat(ScheduleWhen.describe(schedule)).isEqualTo(ScheduleText.WHEN_EMPTY);
    }

    @Test
    void describe_nullType_returnsPlaceholder() {
        ScheduleResponse schedule = schedule(null, null, null, null);

        assertThat(ScheduleWhen.describe(schedule)).isEqualTo(ScheduleText.WHEN_EMPTY);
    }

    private static ScheduleResponse schedule(String type, Instant runAt, String cron, Integer intervalSeconds) {
        return new ScheduleResponse(ID, TASK_ID, type, "label", runAt, cron, intervalSeconds,
                "UTC", true, NOW, NOW);
    }
}
