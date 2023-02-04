package ru.yandex.solomon.model.timeseries.decim;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayListViewIterator;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.stockpile.api.EDecimPolicy;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class DecimatingMetricTypePatametrizedTest {
    @Parameterized.Parameter
    public MetricType type;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return Stream.of(MetricType.values())
                .filter(k -> k != MetricType.METRIC_TYPE_UNSPECIFIED && k != MetricType.UNRECOGNIZED)
                .toArray();
    }

    @Test
    public void empty() {
        int mask = randomMask(type);
        AggrGraphDataArrayList source = new AggrGraphDataArrayList(mask, 100);
        AggrGraphDataArrayList result = decim(source, DecimPoliciesPredefined.policyFromProto(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS), System.currentTimeMillis());
        assertEquals(source, result);
    }

    @Test
    public void skipDecimWhenPolicyAbsent() {
        int mask = randomMask(type);
        AggrGraphDataArrayList source = new AggrGraphDataArrayList(mask, 100);
        for (int index = 0; index < 100; index++) {
            source.addRecord(randomPoint(mask));
        }

        AggrGraphDataArrayList result = decim(source, DecimPoliciesPredefined.policyFromProto(EDecimPolicy.POLICY_KEEP_FOREVER), System.currentTimeMillis());
        assertEquals(source, result);
    }

    @Test
    public void skipDecimWhenDataTooFresh() {
        DecimPolicy policy = DecimPolicy.newBuilder()
                .after(Duration.ofDays(7)).to(Duration.ofMinutes(5))
                .after(Duration.ofDays(30)).to(Duration.ofHours(1))
                .after(Duration.ofDays(60)).to(Duration.ofHours(5))
                .build();

        long now = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6);
        int mask = randomMask(type);
        AggrGraphDataArrayList source = new AggrGraphDataArrayList(mask, 100);
        for (int index = 0; index < 10; index++) {
            now += ThreadLocalRandom.current().nextLong(0, TimeUnit.DAYS.toMillis(1));
            AggrPoint point = randomPoint(mask);
            point.tsMillis = now;
            source.addRecord(point);
        }

        AggrGraphDataArrayList result = decim(source, policy, System.currentTimeMillis());
        assertEquals(source, result);
    }

    @Test
    public void pointAlwaysTimeAligned() {
        DecimPolicy policy = DecimPolicy.newBuilder()
                .after(Duration.ofDays(7)).to(Duration.ofMinutes(10))
                .build();

        long now = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(360);
        int mask = randomMask(type);
        AggrGraphDataArrayList source = new AggrGraphDataArrayList(mask, 100);
        for (int index = 0; index < 10; index++) {
            now += ThreadLocalRandom.current().nextLong(0, TimeUnit.DAYS.toMillis(1));
            AggrPoint point = randomPoint(type);
            point.tsMillis = now;
            source.addRecord(point);
        }

        AggrPoint temp = new AggrPoint();
        AggrGraphDataArrayList result = decim(source, policy, System.currentTimeMillis());
        AggrGraphDataArrayListViewIterator it = result.iterator();
        while (it.next(temp)) {
            assertEquals(0, temp.tsMillis % TimeUnit.MINUTES.toMillis(10));
        }
    }

    @Test
    public void decimByMultiplePolicy() {
        DecimPolicy policy = DecimPolicy.newBuilder()
                .after(Duration.ofDays(7)).to(Duration.ofMinutes(5))
                .after(Duration.ofDays(30)).to(Duration.ofHours(1))
                .after(Duration.ofDays(60)).to(Duration.ofDays(1))
                .build();

        long now = timeToMillis("2018-08-09T15:15:18.010Z");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // to 1 day group
                point("2018-05-01T12:00:00Z"),
                point("2018-05-01T13:00:00Z"),
                point("2018-05-01T16:00:00Z"),
                point("2018-05-01T17:00:00Z"),
                point("2018-05-02T18:00:00Z"),

                // to 1 hours group
                point("2018-07-01T14:15:00Z"),
                point("2018-07-01T14:20:00Z"),
                point("2018-07-01T14:25:00Z"),
                point("2018-07-01T15:15:00Z"),
                point("2018-07-01T15:20:00Z"),

                // to 5 min group
                point("2018-08-01T14:17:15Z"),
                point("2018-08-01T14:17:30Z"),
                point("2018-08-01T14:17:45Z"),
                point("2018-08-01T14:21:45Z"),
                point("2018-08-01T14:23:45Z"),

                // no decim group
                point("2018-08-09T14:17:15Z"),
                point("2018-08-09T14:17:30Z"),
                point("2018-08-09T14:17:45Z")
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // to 1 days group
                point("2018-05-01T00:00:00Z"),
                point("2018-05-02T00:00:00Z"),

                // to 1 hours group
                point("2018-07-01T14:00:00Z"),
                point("2018-07-01T15:00:00Z"),

                // to 5 min group
                point("2018-08-01T14:15:00Z"),
                point("2018-08-01T14:20:00Z"),

                // no decim group
                point("2018-08-09T14:17:15Z"),
                point("2018-08-09T14:17:30Z"),
                point("2018-08-09T14:17:45Z")
        ).cloneWithMask(StockpileColumn.TS.mask());

        AggrGraphDataArrayList result = decim(source, policy, now).cloneWithMask(StockpileColumn.TS.mask());
        assertEquals(expected, result);
    }

    private AggrPoint point(String time) {
        AggrPoint point = randomPoint(type);
        point.tsMillis = timeToMillis(time);
        return point;
    }

    private static long timeToMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }

    private AggrGraphDataArrayList decim(AggrGraphDataArrayList source, DecimPolicy policy, long now) {
        AggrGraphDataListIterator it = DecimatingAggrGraphDataIterator.of(type, source.iterator(), policy, now);
        AggrGraphDataArrayList result = new AggrGraphDataArrayList(source.columnSetMask(), 1);
        result.addAllFrom(it);
        return result;
    }

}
