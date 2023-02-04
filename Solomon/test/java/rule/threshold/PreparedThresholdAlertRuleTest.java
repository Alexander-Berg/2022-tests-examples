package ru.yandex.solomon.alert.rule.threshold;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.NoPointsPolicy;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.rule.AlertTimeSeries;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataIterable;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class PreparedThresholdAlertRuleTest {

    private static List<WindowCheckFunction> prepare(ThresholdAlert alert) {
        return alert.getPredicateRules().stream()
                .map(WindowCheckFunctionFactory::prepare)
                .collect(Collectors.toList());
    }

    private static ThresholdAlert.Builder newAlert() {
        return ThresholdAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId("junk")
                .setName("test alert")
                .setSelectors(Selectors.parse("project=java, cluster=unit, service=test, host=*"))
                .setPeriod(Duration.ofMinutes(5L));
    }

    @Test
    public void greatOrEqualAtLastOnce() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 51, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{53, 11, 12}, 15_000L, 53d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 51, 12}, 30_000L, 51d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 11, 52}, 45_000L, 52, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{-1, -2, 50}, 45_000L, 50, EvaluationStatus.Code.OK);
    }

    @Test
    public void lessOrEqualAtLastOnce() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.LTE, 10, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{-3, 11, 12}, 15_000L, -3, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 10, 12}, 30_000L, 10, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 11, -2}, 45_000L, -2, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{11.2}, 15_000L, 11.2, EvaluationStatus.Code.OK);
    }

    @Test
    public void greatOrEqualAtAllTime() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GTE, 15, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{15, 50, 25}, 45_000L, 25d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{15, 50, -1}, 45_000L, -1, EvaluationStatus.Code.OK);
        check(rule, new double[]{15, -2, 25}, 30_000L, -2, EvaluationStatus.Code.OK);
        check(rule, new double[]{-3, 50, 25}, 15_000L, -3, EvaluationStatus.Code.OK);
    }

    @Test
    public void equalSum() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.SUM, Compare.EQ, 50, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{25, 25, 0}, 45_000L, 50d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{25, 25, 1}, 45_000L, 51d, EvaluationStatus.Code.OK);
        check(rule, new double[]{25, 10, 1}, 45_000L, 36d, EvaluationStatus.Code.OK);
    }

    @Test
    public void notEqualSum() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.SUM, Compare.NE, 50, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{25, 25, 0}, 45_000L, 50d, EvaluationStatus.Code.OK);
        check(rule, new double[]{25, 25, 1}, 45_000L, 51d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{25, 10, 1}, 45_000L, 36d, EvaluationStatus.Code.ALARM);
    }

    @Test
    public void greatSum() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.SUM, Compare.GT, 10, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{1, 2, 3}, 45_000L, 6d, EvaluationStatus.Code.OK);
        check(rule, new double[]{5, 0, 5}, 45_000L, 10d, EvaluationStatus.Code.OK);
        check(rule, new double[]{5, 7, 8}, 45_000L, 20d, EvaluationStatus.Code.ALARM);
    }

    @Test
    public void lessMin() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.MIN, Compare.LT, 13, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{-1, 31, 10}, 45_000L, -1, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{55, -2, 10}, 45_000L, -2, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{55, 31, -3}, 45_000L, -3, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 15, 22}, 45_000L, 13, EvaluationStatus.Code.OK);
        check(rule, new double[]{}, 0, Double.NaN, EvaluationStatus.Code.NO_DATA);
    }

    @Test
    public void greatOrEqualMax() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.MAX, Compare.GTE, 55, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{85, 10, 12}, 45_000L, 85d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 55, 12}, 45_000L, 55d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{13, 10, 95}, 45_000L, 95d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{1, 2, 3}, 45_000L, 3d, EvaluationStatus.Code.OK);
        check(rule, new double[]{}, 0, Double.NaN, EvaluationStatus.Code.NO_DATA);
    }

    @Test
    public void greatOrEqualAvg() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.AVG, Compare.GTE, 5, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{2, 8, 8}, 45_000L, 6d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{10, 15, 2}, 45_000L, 9d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{1, 1, 1, 2, 1, 3}, 6 * 15_000L, 1.5, EvaluationStatus.Code.OK);
        check(rule, new double[]{1, 2, 3, 2}, 4 * 15_000L, 2d, EvaluationStatus.Code.OK);
        check(rule, new double[]{}, 0, Double.NaN, EvaluationStatus.Code.NO_DATA);
        check(rule, new double[]{Double.NaN}, 0, Double.NaN, EvaluationStatus.Code.NO_DATA);
    }

    @Test
    public void lastNoNan() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRule(PredicateRule.of(ThresholdType.LAST_NON_NAN, Compare.GTE, 5, TargetStatus.ALARM))
                .build());

        check(rule, new double[]{2, 8, 3}, 45_000L, 3d, EvaluationStatus.Code.OK);
        check(rule, new double[]{2, 8, 3, Double.NaN}, 45_000L, 3d, EvaluationStatus.Code.OK);
        check(rule, new double[]{2, 8, 7}, 45_000L, 7d, EvaluationStatus.Code.ALARM);
        check(rule, new double[]{2, 8, 7, Double.NaN}, 45_000L, 7d, EvaluationStatus.Code.ALARM);
    }

    @Test
    public void compositeRule() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GTE, 5, TargetStatus.ALARM),
                        PredicateRule.of(ThresholdType.AVG, Compare.GT, 10, TargetStatus.WARN),
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.LT, 100, TargetStatus.NO_DATA)
                ))
                .build());

        check(rule, new double[]{1, 5, 9, 13, 17, 21, 25}, 7 * 15_000L, 13d, EvaluationStatus.Code.WARN, 7);
        check(rule, new double[]{1, 5, 9, 13, 1, 2, 3}, 15_000L, 1d, EvaluationStatus.Code.NO_DATA, 7);
        check(rule, new double[]{}, 0L, Double.NaN, EvaluationStatus.Code.NO_DATA, 0);
    }

    @Test
    public void earlyDecidingCompositeRule() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 100, TargetStatus.ALARM),
                        PredicateRule.of(ThresholdType.AVG, Compare.GT, 10, TargetStatus.WARN)
                ))
                .build());

        check(rule, new double[]{1, 5, 9, 13, 171, 21, 25}, 5 * 15_000L, 171d, EvaluationStatus.Code.ALARM, 5);
        check(rule, new double[]{1, 5, 9, 13, 1, 2, 4}, 7 * 15_000L, 5d, EvaluationStatus.Code.OK, 7);
    }

    @Test
    public void skipAll() {
        List<WindowCheckFunction> rule = prepare(newAlert()
            .setPredicateRules(Stream.of(
                PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GT, 100, TargetStatus.ALARM),
                PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GT, 50, TargetStatus.WARN)
            ))
            .build());

        check(rule, new double[]{1, 5, 9, 13, 171, 21, 25}, 15_000L, 1d, EvaluationStatus.Code.OK, 1);
    }

    @Test
    public void dontFireTooEarly() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 100, TargetStatus.ALARM),
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GT, 10, TargetStatus.WARN)
                ))
                .build());

        check(rule, new double[]{1, 15, 9, 13, 171, 21, 25}, 5 * 15_000L, 171d, EvaluationStatus.Code.ALARM, 5);
        check(rule, new double[]{1, 15, 9, 13, 71, 21, 25}, 2 * 15_000L, 15d, EvaluationStatus.Code.WARN, 7);
        check(rule, new double[]{1, 5, 9, 3, 10, 2, 5}, 7 * 15_000L, 5d, EvaluationStatus.Code.OK, 7);
    }

    @Test
    public void noPointsPolicy() {
        List<WindowCheckFunction> rule = prepare(newAlert()
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.COUNT, Compare.EQ, 0, TargetStatus.WARN),
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 100, TargetStatus.ALARM),
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GT, 10, TargetStatus.WARN)
                ))
                .build());

        {
            double[] values = new double[]{};
            long ts = 0;

            check(NoPointsPolicy.DEFAULT, rule, values, ts, Double.NaN, EvaluationStatus.Code.NO_DATA, 0);
            check(NoPointsPolicy.OK, rule, values, ts, Double.NaN, EvaluationStatus.Code.OK, 0);
            check(NoPointsPolicy.WARN, rule, values, ts, Double.NaN, EvaluationStatus.Code.WARN, 0);
            check(NoPointsPolicy.ALARM, rule, values, ts, Double.NaN, EvaluationStatus.Code.ALARM, 0);
            check(NoPointsPolicy.NO_DATA, rule, values, ts, Double.NaN, EvaluationStatus.Code.NO_DATA, 0);
            check(NoPointsPolicy.MANUAL, rule, values, ts, 0, EvaluationStatus.Code.WARN, 0);
        }

        {
            double[] values = new double[]{Double.NaN};
            long ts = 0;

            check(NoPointsPolicy.DEFAULT, rule, values, ts, Double.NaN, EvaluationStatus.Code.NO_DATA, 1);
            check(NoPointsPolicy.OK, rule, values, ts, Double.NaN, EvaluationStatus.Code.OK, 1);
            check(NoPointsPolicy.WARN, rule, values, ts, Double.NaN, EvaluationStatus.Code.WARN, 1);
            check(NoPointsPolicy.ALARM, rule, values, ts, Double.NaN, EvaluationStatus.Code.ALARM, 1);
            check(NoPointsPolicy.NO_DATA, rule, values, ts, Double.NaN, EvaluationStatus.Code.NO_DATA, 1);
            check(NoPointsPolicy.MANUAL, rule, values, ts, 0, EvaluationStatus.Code.WARN, 1);
        }
    }

    private static class AccountingTimeseriesIterable implements AggrGraphDataIterable {
        final private AggrGraphDataArrayList storage;
        private int count = 0;

        int getCount() {
            return count;
        }

        private AccountingTimeseriesIterable(AggrGraphDataArrayList storage) {
            this.storage = storage;
        }

        public static AccountingTimeseriesIterable of(AggrGraphDataArrayList storage) {
            return new AccountingTimeseriesIterable(storage);
        }

        @ParametersAreNonnullByDefault
        private class Iterator extends AggrGraphDataListIterator {

            final AggrGraphDataListIterator nested;

            Iterator(AggrGraphDataListIterator nested) {
                super(nested.columnSetMask());
                this.nested = nested;
            }

            @Override
            public boolean next(AggrPoint target) {
                boolean ret = nested.next(target);
                if (ret) {
                    count++;
                }
                return ret;
            }
        }

        @Override
        public int getRecordCount() {
            return storage.getRecordCount();
        }

        @Override
        public int elapsedBytes() {
            return storage.elapsedBytes();
        }

        @Override
        public AggrGraphDataListIterator iterator() {
            return new Iterator(storage.iterator());
        }

        @Override
        public int columnSetMask() {
            return storage.columnSetMask();
        }
    }

    private void check(List<WindowCheckFunction> checkers, double[] values, long expectedTs, double expectedValue, EvaluationStatus.Code code) {
        check(NoPointsPolicy.DEFAULT, checkers, values, expectedTs, expectedValue, code);
    }

    private void check(NoPointsPolicy noPointsPolicy, List<WindowCheckFunction> checkers, double[] values, long expectedTs, double expectedValue, EvaluationStatus.Code code) {
        check(noPointsPolicy, checkers, values, expectedTs, expectedValue, code, null);
    }

    private void check(List<WindowCheckFunction> checkers, double[] values, long expectedTs, double expectedValue, EvaluationStatus.Code code, @Nullable Integer expectedCount) {
        check(NoPointsPolicy.DEFAULT, checkers, values, expectedTs, expectedValue, code, expectedCount);
    }

    private void check(NoPointsPolicy noPointsPolicy, List<WindowCheckFunction> checkers, double[] values, long expectedTs, double expectedValue, EvaluationStatus.Code code, @Nullable Integer expectedCount) {
        AggrGraphDataArrayList timeSeries = new AggrGraphDataArrayList(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), values.length);

        AggrPoint point = new AggrPoint();
        long now = 15_000;
        for (int index = 0; index < values.length; index++) {
            point.setTsMillis(now + (index * 15_000));
            point.setValue(values[index], ValueColumn.DEFAULT_DENOM);
            timeSeries.addRecord(point);
        }

        Labels labels = Labels.of("host", "test");

        AccountingTimeseriesIterable accountingTimeseries = AccountingTimeseriesIterable.of(timeSeries);

        MetricCheckResult result = MultiplePredicateChecker.checkMultipleFunctions(noPointsPolicy, checkers,
                new AlertTimeSeries(labels, MetricType.DGAUGE, accountingTimeseries));
        String message = timeSeries.toString();
        assertThat(message, result.getStatusCode(), equalTo(code));
        assertThat(message, result.getLabels().get(), equalTo(labels));
        assertThat(message, result.getTimeMillis(), equalTo(expectedTs));
        assertThat(message, result.getValue(), equalTo(expectedValue));
        if (expectedCount != null) {
            assertThat(message, accountingTimeseries.getCount(), equalTo(expectedCount));
        }
    }
}
