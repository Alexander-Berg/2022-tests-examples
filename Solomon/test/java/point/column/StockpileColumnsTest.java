package ru.yandex.solomon.model.point.column;

import java.util.EnumSet;

import org.junit.Test;

import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.column.StockpileColumn.COUNT;
import static ru.yandex.solomon.model.point.column.StockpileColumn.DSUMMARY;
import static ru.yandex.solomon.model.point.column.StockpileColumn.HISTOGRAM;
import static ru.yandex.solomon.model.point.column.StockpileColumn.ISUMMARY;
import static ru.yandex.solomon.model.point.column.StockpileColumn.LOG_HISTOGRAM;
import static ru.yandex.solomon.model.point.column.StockpileColumn.LONG_VALUE;
import static ru.yandex.solomon.model.point.column.StockpileColumn.MERGE;
import static ru.yandex.solomon.model.point.column.StockpileColumn.STEP;
import static ru.yandex.solomon.model.point.column.StockpileColumn.TS;
import static ru.yandex.solomon.model.point.column.StockpileColumn.VALUE;
import static ru.yandex.solomon.model.point.column.StockpileColumns.minColumnSet;
import static ru.yandex.solomon.model.point.column.StockpileColumns.typeByMask;

/**
 * @author Vladimir Gordiychuk
 */
public class StockpileColumnsTest {

    @Test
    public void byMaskTest() {
        assertThat(typeByMask(TS.mask() | VALUE.mask()), equalTo(MetricType.DGAUGE));
        assertThat(typeByMask(TS.mask() | VALUE.mask() | StockpileColumn.STEP.mask()), equalTo(MetricType.DGAUGE));
        assertThat(typeByMask(TS.mask() | ISUMMARY.mask()), equalTo(MetricType.ISUMMARY));
        assertThat(typeByMask(TS.mask() | DSUMMARY.mask()), equalTo(MetricType.DSUMMARY));
        assertThat(typeByMask(TS.mask() | HISTOGRAM.mask()), equalTo(MetricType.HIST));
        assertThat(typeByMask(TS.mask() | LOG_HISTOGRAM.mask()), equalTo(MetricType.LOG_HISTOGRAM));
        assertThat(typeByMask(TS.mask() | LONG_VALUE.mask()), equalTo(MetricType.IGAUGE));
    }

    @Test
    public void tsRequiredColumn() {
        for (MetricType type : MetricType.values()) {
            if (EnumSet.of(MetricType.UNRECOGNIZED, MetricType.METRIC_TYPE_UNSPECIFIED).contains(type)) {
                continue;
            }

            int mask = minColumnSet(type);
            assertTrue(TS.isInSet(mask));
        }
    }

    @Test
    public void minColumnSetTest() {
        assertTrue(HISTOGRAM.isInSet(minColumnSet(MetricType.HIST)));
        assertTrue(HISTOGRAM.isInSet(minColumnSet(MetricType.HIST)));
        assertTrue(VALUE.isInSet(minColumnSet(MetricType.DGAUGE)));
        assertTrue(ISUMMARY.isInSet(minColumnSet(MetricType.ISUMMARY)));
        assertTrue(DSUMMARY.isInSet(minColumnSet(MetricType.DSUMMARY)));
        assertTrue(LOG_HISTOGRAM.isInSet(minColumnSet(MetricType.LOG_HISTOGRAM)));
        assertTrue(LONG_VALUE.isInSet(minColumnSet(MetricType.COUNTER)));
        assertTrue(LONG_VALUE.isInSet(minColumnSet(MetricType.RATE)));
        assertTrue(LONG_VALUE.isInSet(minColumnSet(MetricType.IGAUGE)));
    }

    @Test
    public void tsRequired() {
        // valid
        StockpileColumns.ensureColumnSetValid(MetricType.DGAUGE, TS.mask() | VALUE.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.DGAUGE, TS.mask() | VALUE.mask() | COUNT.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.DGAUGE, TS.mask() | VALUE.mask() | COUNT.mask() | MERGE.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.DGAUGE, TS.mask() | VALUE.mask() | COUNT.mask() | MERGE.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.DGAUGE, TS.mask() | VALUE.mask() | COUNT.mask() | MERGE.mask() | STEP.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.DSUMMARY, TS.mask() | DSUMMARY.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.ISUMMARY, TS.mask() | ISUMMARY.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.LOG_HISTOGRAM, TS.mask() | LOG_HISTOGRAM.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.HIST, TS.mask() | HISTOGRAM.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.COUNTER, TS.mask() | LONG_VALUE.mask());
        StockpileColumns.ensureColumnSetValid(MetricType.RATE, TS.mask() | LONG_VALUE.mask());

        // invalid
        checkInvalidColumnSet(MetricType.DGAUGE, 0);
        checkInvalidColumnSet(MetricType.DGAUGE, TS.mask());
        checkInvalidColumnSet(MetricType.DGAUGE, VALUE.mask());
        checkInvalidColumnSet(MetricType.DGAUGE, TS.mask() | VALUE.mask() | HISTOGRAM.mask());
        checkInvalidColumnSet(MetricType.LOG_HISTOGRAM, TS.mask() | HISTOGRAM.mask());
        checkInvalidColumnSet(MetricType.ISUMMARY, TS.mask() | VALUE.mask());
        checkInvalidColumnSet(MetricType.DSUMMARY, TS.mask() | VALUE.mask());
        checkInvalidColumnSet(MetricType.COUNTER, TS.mask() | VALUE.mask());
        checkInvalidColumnSet(MetricType.RATE, TS.mask() | VALUE.mask());
    }

    @Test
    public void tsRequiredRandom() {
        for (MetricType type : MetricType.values()) {
            if (type == MetricType.METRIC_TYPE_UNSPECIFIED || type == MetricType.UNRECOGNIZED) {
                continue;
            }

            StockpileColumns.ensureColumnSetValid(type, randomMask(type));
        }
    }

    @Test
    public void typeByMaskConvertersCompatibility() {
        // Should be compatible with https://nda.ya.ru/3UVKCA otherwise failed during decode
        assertThat(typeByMask(
                TS.mask()
                        | VALUE.mask()
                        | HISTOGRAM.mask()
                        | LOG_HISTOGRAM.mask()
                        | ISUMMARY.mask()
                        | DSUMMARY.mask()),
                equalTo(MetricType.DGAUGE));

        assertThat(typeByMask(
                TS.mask()
                        | HISTOGRAM.mask()
                        | LOG_HISTOGRAM.mask()
                        | ISUMMARY.mask()
                        | DSUMMARY.mask()),
                equalTo(MetricType.LOG_HISTOGRAM));

        assertThat(typeByMask(
                TS.mask()
                        | HISTOGRAM.mask()
                        | ISUMMARY.mask()
                        | DSUMMARY.mask()),
                equalTo(MetricType.HIST));

        assertThat(typeByMask(
                TS.mask()
                        | ISUMMARY.mask()
                        | DSUMMARY.mask()),
                equalTo(MetricType.ISUMMARY));

        assertThat(typeByMask(
                TS.mask()
                        | DSUMMARY.mask()),
                equalTo(MetricType.DSUMMARY));
    }

    private void checkInvalidColumnSet(MetricType type, int mask) {
        try {
            StockpileColumns.ensureColumnSetValid(type, mask);
            fail("Expected invalid mask " + StockpileColumnSet.toString(mask) + " for type " + type);
        } catch (IllegalArgumentException e) {
            // ok
        }
    }
}
