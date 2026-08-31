package dev.kairos.domain.schedule;

import dev.kairos.common.exceptions.DomainException;

/**
 * Raised when a schedule cannot be found for the given id. The API maps this to
 * HTTP 404.
 */
public class ScheduleNotFoundException extends DomainException {

    private final ScheduleId scheduleId;

    public ScheduleNotFoundException(ScheduleId scheduleId) {
        super("Schedule " + scheduleId.value() + " not found");
        this.scheduleId = scheduleId;
    }

    public ScheduleId scheduleId() {
        return scheduleId;
    }
}
