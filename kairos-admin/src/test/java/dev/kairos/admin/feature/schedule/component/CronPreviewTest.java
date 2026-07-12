package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.CronText;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CronPreviewTest {

    // --- validate ---

    @Test
    void validate_wellFormedExpression_isValid() {
        assertThat(CronPreview.validate("0 0 2 * * ?").valid()).isTrue();
    }

    @Test
    void validate_fiveFieldExpression_isInvalid() {
        CronPreview.Validation result = CronPreview.validate("0 0 2 * *");

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotBlank();
    }

    @Test
    void validate_garbage_isInvalid() {
        assertThat(CronPreview.validate("not a cron").valid()).isFalse();
    }

    // --- nextExecutions ---

    @Test
    void nextExecutions_everyMinute_returnsRequestedCountInOrder() {
        List<LocalDateTime> times = CronPreview.nextExecutions("0 * * * * ?", 5);

        assertThat(times).hasSize(5);
        assertThat(times).isSorted();
        assertThat(times).allMatch(t -> t.isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void nextExecutions_invalidExpression_returnsEmpty() {
        assertThat(CronPreview.nextExecutions("bad", 10)).isEmpty();
    }

    // --- describe ---

    @Test
    void describe_dailyAt2Am_readsAtTimeEveryDay() {
        CronFields fields = CronFields.parse("0 0 2 * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("At 02:00 every day");
    }

    @Test
    void describe_weekdays_mentionsDayOfWeek() {
        CronFields fields = CronFields.parse("0 0 9 * * MON-FRI");

        assertThat(CronPreview.describe(fields)).isEqualTo("At 09:00 on MON-FRI");
    }

    @Test
    void describe_dayOfMonth_mentionsDay() {
        CronFields fields = CronFields.parse("0 0 0 15 * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("At 00:00 on day 15");
    }

    @Test
    void describe_everyFiveMinutes_readsFrequency() {
        CronFields fields = CronFields.parse("0 */5 * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 5 minutes");
    }

    @Test
    void describe_everyMinute_readsFrequency() {
        CronFields fields = CronFields.parse("0 * * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every minute");
    }

    @Test
    void describe_everyHour_readsFrequency() {
        CronFields fields = CronFields.parse("0 0 * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every hour");
    }

    @Test
    void describe_minutePastEveryHour_readsFrequency() {
        CronFields fields = CronFields.parse("0 15 * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("At minute 15 past every hour");
    }

    @Test
    void describe_everyThreeHours_readsFrequency() {
        CronFields fields = CronFields.parse("0 0 */3 * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 3 hours");
    }

    @Test
    void describe_literalMonth_readsMonthName() {
        CronFields fields = CronFields.parse("0 0 0 1 6 ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("At 00:00 on day 1, in June");
    }

    // --- describe: every-N-seconds branch ---

    @Test
    void describe_everyTenSeconds_readsFrequency() {
        CronFields fields = CronFields.parse("*/10 * * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 10 seconds");
    }

    @Test
    void describe_everyFiveSeconds_readsFrequency() {
        CronFields fields = CronFields.parse("*/5 * * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 5 seconds");
    }

    // --- describe: every-second branch ---

    @Test
    void describe_everySecond_readsFrequency() {
        CronFields fields = CronFields.parse("* * * * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every second");
    }

    // --- describe: sub-hourly with day constraint omits "every day" ---

    @Test
    void describe_everyFiveMinutesWeekdays_includesDayOfWeek() {
        CronFields fields = CronFields.parse("0 */5 * * * MON-FRI");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 5 minutes on MON-FRI");
    }

    @Test
    void describe_everyFiveMinutesDayOfMonth_includesDayOfMonth() {
        CronFields fields = CronFields.parse("0 */5 * 15 * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 5 minutes on day 15");
    }

    // --- describe: "every hour" not appended with "every day" for sub-hourly at :00 ---

    @Test
    void describe_everyHourOnWeekdays_includesDayOfWeek() {
        CronFields fields = CronFields.parse("0 0 * * * MON-FRI");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every hour on MON-FRI");
    }

    // --- describe: at-minute-N with day constraints ---

    @Test
    void describe_minutePastEveryHourOnDayOfMonth_includesDayOfMonth() {
        CronFields fields = CronFields.parse("0 30 * 10 * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("At minute 30 past every hour on day 10");
    }

    // --- describe: month name for all 12 months ---

    @Test
    void describe_allMonths_readCorrectNames() {
        String[] expectedNames = CronText.MONTH_NAMES;
        for (int month = 1; month <= 12; month++) {
            CronFields fields = CronFields.parse("0 0 0 1 " + month + " ?");
            assertThat(CronPreview.describe(fields))
                    .as("month %d", month)
                    .endsWith(CronText.DESCRIBE_IN_MONTH_PREFIX + expectedNames[month]);
        }
    }

    // --- describe: out-of-range month produces no month suffix ---

    @Test
    void describe_monthZero_producesNoMonthSuffix() {
        CronFields fields = new CronFields("0", "0", "0", "1", "0", "?");

        assertThat(CronPreview.describe(fields)).doesNotContain(CronText.DESCRIBE_IN_MONTH_PREFIX);
    }

    @Test
    void describe_wildcardMonth_producesNoMonthSuffix() {
        CronFields fields = CronFields.parse("0 0 2 * * ?");

        assertThat(CronPreview.describe(fields)).doesNotContain(CronText.DESCRIBE_IN_MONTH_PREFIX);
    }

    // --- describe: at time on day-of-week with month ---

    @Test
    void describe_atTimeOnDayOfWeekInMonth_includesBothDayAndMonth() {
        CronFields fields = CronFields.parse("0 0 9 ? 3 MON");

        assertThat(CronPreview.describe(fields)).isEqualTo("At 09:00 on MON, in March");
    }

    // --- describe: every-N-hours with non-zero minute field ---

    @Test
    void describe_everyTwoHours_readsFrequency() {
        CronFields fields = CronFields.parse("0 0 */2 * * ?");

        assertThat(CronPreview.describe(fields)).isEqualTo("Every 2 hours");
    }

    // --- warnings: all four 30-day months trigger short-month warning for day 31 ---

    @Test
    void warnings_day31InJune_warnsShortMonth() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 31 6 ?")))
                .contains(CronText.WARN_SHORT_MONTH_DAY);
    }

    @Test
    void warnings_day31InSeptember_warnsShortMonth() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 31 9 ?")))
                .contains(CronText.WARN_SHORT_MONTH_DAY);
    }

    @Test
    void warnings_day31InNovember_warnsShortMonth() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 31 11 ?")))
                .contains(CronText.WARN_SHORT_MONTH_DAY);
    }

    @Test
    void warnings_day30InThirtyDayMonth_hasNoShortMonthWarning() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 30 4 ?")))
                .doesNotContain(CronText.WARN_SHORT_MONTH_DAY);
    }

    // --- warnings: February boundary ---

    @Test
    void warnings_feb29_hasNoFebruaryWarning() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 29 2 ?")))
                .doesNotContain(CronText.WARN_FEB_DAY);
    }

    @Test
    void warnings_feb30_warnsFebruaryAndNotShortMonth() {
        List<String> result = CronPreview.warnings(CronFields.parse("0 0 0 30 2 ?"));

        assertThat(result).contains(CronText.WARN_FEB_DAY);
        assertThat(result).doesNotContain(CronText.WARN_SHORT_MONTH_DAY);
    }

    // --- warnings: wildcard day-of-month does not trigger impossible-day check ---

    @Test
    void warnings_wildcardDayOfMonth_hasNoDayWarning() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 * 2 ?")))
                .doesNotContain(CronText.WARN_FEB_DAY, CronText.WARN_SHORT_MONTH_DAY);
    }

    // --- warnings: both day fields constrained triggers conflict only, not day warning ---

    @Test
    void warnings_bothDayFieldsConstrained_doesNotProduceDayImpossibilityWarning() {
        List<String> result = CronPreview.warnings(CronFields.parse("0 0 0 15 6 MON"));

        assertThat(result).contains(CronText.WARN_DAY_CONFLICT);
        assertThat(result).doesNotContain(CronText.WARN_SHORT_MONTH_DAY);
    }

    // --- warnings: only dayOfWeek constrained, no conflict ---

    @Test
    void warnings_onlyDayOfWeekConstrained_hasNoConflict() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 * * MON")))
                .doesNotContain(CronText.WARN_DAY_CONFLICT);
    }

    // --- warnings ---

    @Test
    void warnings_cleanExpression_hasNone() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 2 * * ?"))).isEmpty();
    }

    @Test
    void warnings_bothDayFieldsConstrained_warnsConflict() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 15 * MON")))
                .contains(CronText.WARN_DAY_CONFLICT);
    }

    @Test
    void warnings_feb30_warnsImpossibleFebruaryDay() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 30 2 ?")))
                .contains(CronText.WARN_FEB_DAY);
    }

    @Test
    void warnings_day31InThirtyDayMonth_warnsShortMonth() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 31 4 ?")))
                .contains(CronText.WARN_SHORT_MONTH_DAY);
    }

    @Test
    void warnings_day31InLongMonth_hasNoShortMonthWarning() {
        assertThat(CronPreview.warnings(CronFields.parse("0 0 0 31 1 ?")))
                .doesNotContain(CronText.WARN_SHORT_MONTH_DAY, CronText.WARN_FEB_DAY);
    }
}
