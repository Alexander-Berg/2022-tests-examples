package ru.yandex.solomon.alert.rule.expression;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.rule.SingleTimeSeries;
import ru.yandex.solomon.model.MetricKey;
import ru.yandex.solomon.model.StockpileKey;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.protobuf.MetricTypeConverter;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.stockpile.client.shard.StockpileLocalId;
import ru.yandex.stockpile.client.shard.StockpileShardId;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class SingleTimeSeriesTest {

    @Test
    public void aggrToGraphDataNoTransfers() {
        final long ts0 = Instant.parse("2018-09-11T09:31:35.916Z").toEpochMilli();
        final long step = 10_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts0 + step * 2, 2.0),
                point(ts0 + step * 3, 3.0),
                point(ts0 + step * 4, 4.0),
                point(ts0 + step * 5, 5.0));

        GraphData expected = GraphData.of(
                DataPoint.point(ts0 + step * 2, 2.0),
                DataPoint.point(ts0 + step * 3, 3.0),
                DataPoint.point(ts0 + step * 4, 4.0),
                DataPoint.point(ts0 + step * 5, 5.0));

        SingleTimeSeries ts = of(MetricType.DGAUGE, source);
        GraphData gd = ts.asNamedGraphData().getGraphData();
        assertEquals(expected, gd);
    }

    @Test
    public void aggrToGraphDataTransfer() {
        final long ts0 = Instant.parse("2018-09-11T09:31:35.916Z").toEpochMilli();
        final long step = 10_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint(ts0 + step * 2, 2),
                lpoint(ts0 + step * 3, 3),
                lpoint(ts0 + step * 4, 4),
                lpoint(ts0 + step * 5, 5));

        GraphData expected = GraphData.of(
                DataPoint.point(ts0 + step * 2, 2.0),
                DataPoint.point(ts0 + step * 3, 3.0),
                DataPoint.point(ts0 + step * 4, 4.0),
                DataPoint.point(ts0 + step * 5, 5.0));

        SingleTimeSeries ts = of(MetricType.COUNTER, source);
        GraphData gd = ts.asNamedGraphData().getGraphData();
        assertEquals(expected, gd);
    }

    @Test
    public void numDenomFold() {
        final long ts0 = Instant.parse("2018-09-11T09:31:35.916Z").toEpochMilli();
        final long step = 10_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder().time(ts0 + step * 2).doubleValue(100, 10_000).build(),
                AggrPoint.builder().time(ts0 + step * 3).doubleValue(250, 10_000).build(),
                AggrPoint.builder().time(ts0 + step * 4).doubleValue(110, 10_000).build(),
                AggrPoint.builder().time(ts0 + step * 5).doubleValue(140, 10_000).build());

        GraphData expected = GraphData.of(
                DataPoint.point(ts0 + step * 2, 10.0),
                DataPoint.point(ts0 + step * 3, 25.0),
                DataPoint.point(ts0 + step * 4, 11.0),
                DataPoint.point(ts0 + step * 5, 14.0));

        SingleTimeSeries ts = of(MetricType.DGAUGE, source);
        GraphData gd = ts.asNamedGraphData().getGraphData();
        assertEquals(expected, gd);
    }

    private SingleTimeSeries of(MetricType type, AggrGraphDataArrayList source) {
        MetricKey key = new MetricKey(type, Labels.of("type", type.name()), randomStockpileKey());
        return new SingleTimeSeries(key, MetricTypeConverter.toNotNullProto(type), source);
    }

    private static StockpileKey randomStockpileKey() {
        return new StockpileKey("myt", StockpileShardId.random(), StockpileLocalId.random());
    }
}
