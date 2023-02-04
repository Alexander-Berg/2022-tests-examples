package ru.yandex.solomon.model.timeseries.decim;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.stockpile.api.EDecimPolicy;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimatingAggrGraphDataIteratorTest {

    @Test
    public void emptyIteratorDecim() {
        AggrGraphDataArrayList source = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, 100);

        AggrGraphDataArrayList result = decim(source, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS, System.currentTimeMillis());
        assertThat(result, equalTo(source));
    }

    @Test
    public void skipDecim() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-04-23T11:51:58.912Z", 42),
                point("2018-04-23T11:51:59.512Z", 24));

        AggrGraphDataArrayList result = decim(source, EDecimPolicy.POLICY_KEEP_FOREVER, System.currentTimeMillis());
        assertThat(result, equalTo(source));
    }

    @Test
    public void skipDecimForNewData() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-04-23T11:51:58.912Z", 42),
                point("2018-04-23T11:51:59.512Z", 24));

        long now = timeToMillis("2018-04-23T15:00:00Z");
        AggrGraphDataArrayList result = decim(source, EDecimPolicy.POLICY_1_MIN_AFTER_1_MONTH_5_MIN_AFTER_3_MONTHS, now);
        assertThat(result, equalTo(source));
    }

    @Test
    public void decimRoundTsToLeft() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-04-23T11:51:58.912Z", 42),
                point("2018-04-23T11:51:59.512Z", 24));

        long now = timeToMillis("2018-05-10T15:00:00Z");
        AggrGraphDataArrayList result = decim(source, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS, now);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-04-23T11:50:00Z", 33));

        assertThat(result, equalTo(expected));
    }

    @Test
    public void leftIncludedRightExcluded() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-04-23T11:50:00Z", 42),
                point("2018-04-23T11:51:52Z", 24),
                point("2018-04-23T11:55:00Z", 10)
        );

        long now = timeToMillis("2018-05-10T15:00:00Z");
        AggrGraphDataArrayList result = decim(source, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS, now);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-04-23T11:50:00Z", 33),
                point("2018-04-23T11:55:00Z", 10));

        assertThat(result, equalTo(expected));
    }

    @Test
    public void repeatDecim() {
        long now = timeToMillis("2018-04-23T11:50:00Z");
        AggrGraphDataArrayList source = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, 100);
        for (int index = 0; index < 100; index++) {
            source.addRecord(AggrPoint.builder()
                    .time(now)
                    .doubleValue(index)
                    .build());

            now += TimeUnit.MINUTES.toMillis(1L);
        }

        now += TimeUnit.DAYS.toMillis(10);
        AggrGraphDataArrayList v1 = decim(source, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS, now);

        AggrGraphDataArrayList v2 = decim(v1, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS, now);
        assertThat(v1, equalTo(v2));

        AggrGraphDataArrayList v3 = decim(v2, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS, now);
        assertThat(v2, equalTo(v3));
    }

    private long timeToMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }

    private AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    private AggrGraphDataArrayList decim(AggrGraphDataArrayList source, EDecimPolicy policy, long now) {
        MetricType type = StockpileColumns.typeByMask(source.columnSetMask());
        return decim(type, source, DecimPoliciesPredefined.policyFromProto(policy), now);
    }

    private AggrGraphDataArrayList decim(MetricType type, AggrGraphDataArrayList source, DecimPolicy policy, long now) {
        AggrGraphDataListIterator it = DecimatingAggrGraphDataIterator.of(type, source.iterator(), policy, now);
        AggrGraphDataArrayList result = new AggrGraphDataArrayList(source.columnSetMask(), 1);
        result.addAllFrom(it);
        return result;
    }
}
