package ru.yandex.solomon.model.timeseries;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricTypeTransfersCommonTest {

    @Test
    public void isTransferAvailable() {
        forEachKind(from -> {
            forEachKind(to -> {
                if (MetricTypeTransfers.isAvailableTransfer(from, to)) {
                    expectSuccessTransfer(from, to);
                } else {
                    expectFailTransfer(from, to);
                }
            });
        });
    }

    private void expectSuccessTransfer(MetricType from, MetricType to) {
        AggrGraphDataArrayList source = listOf(IntStream.range(0, 100)
                .mapToObj(ignore -> randomPoint(from))
                .toArray(AggrPoint[]::new));
        source.sortAndMerge();

        AggrGraphDataArrayList result = listOf(MetricTypeTransfers.of(from, to, source.iterator()));
        assertFalse(from + " -> " + to, result.isEmpty());
        assertTrue(from + " -> " + to, (StockpileColumns.minColumnSet(to) & result.columnSetMask()) != 0);
    }

    private void expectFailTransfer(MetricType from, MetricType to) {
        AggrGraphDataArrayList source = listOf(IntStream.range(0, 100)
                .mapToObj(ignore -> randomPoint(from))
                .toArray(AggrPoint[]::new));
        source.sortAndMerge();
        try {
            listOf(MetricTypeTransfers.of(from, to, source.iterator()));
            fail("Expected failed transfer: "+ from + " -> " + to);
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }

    private AggrGraphDataArrayList listOf(AggrPoint... points) {
        return AggrGraphDataArrayList.of(points);
    }

    private AggrGraphDataArrayList listOf(AggrGraphDataListIterator iterator) {
        return AggrGraphDataArrayList.of(iterator);
    }

    private void forEachKind(Consumer<MetricType> consumer) {
        for (MetricType type : MetricType.values()) {
            if (type == MetricType.UNRECOGNIZED || type == MetricType.METRIC_TYPE_UNSPECIFIED) {
                continue;
            }

            consumer.accept(type);
        }
    }
}
