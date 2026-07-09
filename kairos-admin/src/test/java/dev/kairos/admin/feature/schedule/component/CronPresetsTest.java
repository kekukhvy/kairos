package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.CronText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CronPresetsTest {

    @Test
    void names_areOrderedAndComplete() {
        assertThat(CronPresets.names())
                .startsWith(CronText.PRESET_EVERY_MINUTE)
                .endsWith(CronText.PRESET_MONTHLY_FIRST)
                .hasSize(10);
    }

    @Test
    void expressionFor_everyMinute_mapsToExpression() {
        assertThat(CronPresets.expressionFor(CronText.PRESET_EVERY_MINUTE)).isEqualTo("0 * * * * ?");
    }

    @Test
    void expressionFor_daily2Am_matchesDefault() {
        assertThat(CronPresets.expressionFor(CronText.PRESET_DAILY_2AM))
                .isEqualTo(CronText.DEFAULT_EXPRESSION);
    }

    @Test
    void expressionFor_weekdays9Am_mapsToWeekdayRange() {
        assertThat(CronPresets.expressionFor(CronText.PRESET_WEEKDAYS_9AM)).isEqualTo("0 0 9 * * MON-FRI");
    }

    @Test
    void expressionFor_monthlyFirst_mapsToDayOne() {
        assertThat(CronPresets.expressionFor(CronText.PRESET_MONTHLY_FIRST)).isEqualTo("0 0 0 1 * ?");
    }

    @Test
    void expressionFor_unknownName_returnsNull() {
        assertThat(CronPresets.expressionFor("Never")).isNull();
    }

    @Test
    void everyPreset_isAValidExpression() {
        CronPresets.names().forEach(name ->
                assertThat(CronPreview.validate(CronPresets.expressionFor(name)).valid())
                        .as("preset '%s'", name)
                        .isTrue());
    }
}
