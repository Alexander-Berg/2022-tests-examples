package ru.yandex.solomon.model.timeseries.decim;

import java.util.function.Consumer;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointDataTestSupport;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.aggregation.collectors.PointValueCollector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimPointValueCollectorTest {
    @Test
    public void falseWhenAbsentCollectedPoints() {
        forEachType(type -> {
            PointValueCollector collector = DecimPointValueCollector.of(type);
            AggrPoint result = new AggrPoint(StockpileColumns.minColumnSet(type));
            assertFalse(type.name(), collector.compute(result));
        });
    }

    @Test
    public void multipleDecimMinMask() {
        forEachType(type -> {
            int mask = StockpileColumns.minColumnSet(type);
            PointValueCollector collector = DecimPointValueCollector.of(type);

            for (int index = 0; index < 100; index++) {
                collector.append(randomPoint(mask));
            }

            AggrPoint result = new AggrPoint(mask);
            assertTrue(type.name(), collector.compute(result));
            assertNotEquals(new AggrPoint(mask), result);
        });
    }

    @Test
    public void multipleDecimMaxMask() {
        forEachType(type -> {
            int mask = StockpileColumns.maxColumnSet(type);
            PointValueCollector collector = DecimPointValueCollector.of(type);

            for (int index = 0; index < 100; index++) {
                collector.append(randomPoint(mask));
            }

            AggrPoint result = new AggrPoint(mask);
            assertTrue(type.name(), collector.compute(result));
            assertNotEquals(new AggrPoint(mask), result);
        });
    }

    @Test
    public void decimOnePointMinMask() {
        forEachType(type -> {
            int mask = StockpileColumns.minColumnSet(type) & ~StockpileColumn.TS.mask();
            PointValueCollector collector = DecimPointValueCollector.of(type);
            AggrPoint source = randomPoint(mask);
            collector.append(source);

            AggrPoint result = new AggrPoint(mask);
            assertTrue(type.name(), collector.compute(result));
            assertEquals(type.name(), source, result);
        });
    }

    @Test
    public void decimOnePointMaxMask() {
        forEachType(type -> {
            int mask = StockpileColumns.maxColumnSet(type) & ~StockpileColumn.TS.mask();
            PointValueCollector collector = DecimPointValueCollector.of(type);
            AggrPoint source = randomPoint(mask);
            source.merge = false;
            source.count = 1;
            collector.append(source);

            AggrPoint result = new AggrPoint(mask);
            assertTrue(type.name(), collector.compute(result));
            assertEquals(type.name(), source, result);
        });
    }

    private void forEachType(Consumer<MetricType> consumer) {
        for (MetricType type : MetricType.values()) {
            if (type == MetricType.UNRECOGNIZED || type == MetricType.METRIC_TYPE_UNSPECIFIED ||
            type == MetricType.HIST || type == MetricType.HIST_RATE) {
                continue;
            }

            consumer.accept(type);
        }
    }

    private AggrPoint randomPoint(int mask) {
        AggrPoint point;
        do {
            point = AggrPointDataTestSupport.randomPoint(mask);
        } while (!isValid(point));
        return point;
    }

    private boolean isValid(AggrPoint point) {
        AggrPoint absent = new AggrPoint(point.columnSet);
        absent.tsMillis = point.tsMillis;
        if (absent.equals(point)) {
            return false;
        }

        return Double.isFinite(point.valueNum);
    }
}
