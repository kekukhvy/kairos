package dev.kairos.admin.shared.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats {@link Instant} timestamps into human-readable strings for the admin
 * UI. Instants are rendered in the system default time zone so operators see
 * local wall-clock time instead of a raw UTC ISO string.
 */
public final class DateTimes {

    private static final String EMPTY = "";

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private DateTimes() {
    }

    /**
     * Formats {@code instant} as {@code yyyy-MM-dd HH:mm:ss} in the system time
     * zone, or {@code ""} when it is {@code null}.
     */
    public static String forDisplay(Instant instant) {
        if (instant == null) {
            return EMPTY;
        }
        return DISPLAY_FORMAT.format(instant);
    }
}
