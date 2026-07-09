package dev.kairos.admin.feature.schedule.component;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CronFieldsTest {

    @Test
    void parse_sixParts_splitsInOrder() {
        CronFields fields = CronFields.parse("0 30 2 15 6 ?");

        assertThat(fields.seconds()).isEqualTo("0");
        assertThat(fields.minutes()).isEqualTo("30");
        assertThat(fields.hours()).isEqualTo("2");
        assertThat(fields.dayOfMonth()).isEqualTo("15");
        assertThat(fields.month()).isEqualTo("6");
        assertThat(fields.dayOfWeek()).isEqualTo("?");
    }

    @Test
    void parse_collapsesExtraWhitespace() {
        assertThat(CronFields.parse("0   0  2 * * ?").expression()).isEqualTo("0 0 2 * * ?");
    }

    @Test
    void parse_blank_returnsNull() {
        assertThat(CronFields.parse("  ")).isNull();
        assertThat(CronFields.parse(null)).isNull();
    }

    @Test
    void parse_wrongPartCount_returnsNull() {
        assertThat(CronFields.parse("0 0 2 * *")).isNull();
        assertThat(CronFields.parse("0 0 2 * * ? extra")).isNull();
    }

    @Test
    void constructor_blankPart_fallsBackToAny() {
        CronFields fields = new CronFields("0", "", null, "*", " ", "?");

        assertThat(fields.expression()).isEqualTo("0 * * * * ?");
    }

    @Test
    void expression_roundTripsThroughParse() {
        assertThat(CronFields.parse("0 0 9 * * MON-FRI").expression()).isEqualTo("0 0 9 * * MON-FRI");
    }
}
