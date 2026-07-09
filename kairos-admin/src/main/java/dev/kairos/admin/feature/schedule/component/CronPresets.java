package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.CronText;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable "Quick Start" table mapping a human-friendly preset name to a
 * 6-field Spring cron expression ({@code seconds minutes hours day-of-month
 * month day-of-week}). Consumed by {@link CronBuilderDialog}'s preset picker;
 * selecting a name populates the six builder fields from the mapped expression.
 * Pure data, no UI — unit-testable on its own.
 */
public final class CronPresets {

    private static final Map<String, String> TABLE = buildTable();

    private CronPresets() {
    }

    private static Map<String, String> buildTable() {
        Map<String, String> table = new LinkedHashMap<>();
        table.put(CronText.PRESET_EVERY_MINUTE, "0 * * * * ?");
        table.put(CronText.PRESET_EVERY_5_MINUTES, "0 */5 * * * ?");
        table.put(CronText.PRESET_EVERY_15_MINUTES, "0 */15 * * * ?");
        table.put(CronText.PRESET_EVERY_HOUR, "0 0 * * * ?");
        table.put(CronText.PRESET_DAILY_MIDNIGHT, "0 0 0 * * ?");
        table.put(CronText.PRESET_DAILY_2AM, "0 0 2 * * ?");
        table.put(CronText.PRESET_DAILY_6AM, "0 0 6 * * ?");
        table.put(CronText.PRESET_WEEKDAYS_9AM, "0 0 9 * * MON-FRI");
        table.put(CronText.PRESET_WEEKLY_SUNDAY, "0 0 0 * * SUN");
        table.put(CronText.PRESET_MONTHLY_FIRST, "0 0 0 1 * ?");
        return table;
    }

    /** The preset names in display order. */
    public static java.util.List<String> names() {
        return java.util.List.copyOf(TABLE.keySet());
    }

    /**
     * The 6-field expression mapped to {@code name}, or {@code null} when the
     * name is not a known preset.
     */
    public static String expressionFor(String name) {
        return TABLE.get(name);
    }
}
