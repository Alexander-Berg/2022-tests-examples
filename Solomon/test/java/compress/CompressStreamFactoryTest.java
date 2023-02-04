package ru.yandex.solomon.codec.compress;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;

import ru.yandex.solomon.codec.bits.ReadOnlyHeapBitBuf;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricType;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.createInputStream;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.createOutputStream;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.emptyInputStream;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.emptyOutputStream;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
public class CompressStreamFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void notAbleCreateStreamWithoutTs() {
        try (var out = createOutputStream(MetricType.DGAUGE, StockpileColumn.VALUE.mask())) {
            Assert.fail("Each time series should have TS column");
        }
    }

    @Test
    public void compressDecompressSameDefaultValues() {
        forEachType(type -> {
            int mask = StockpileColumns.minColumnSet(type);
            try (TimeSeriesOutputStream out = createOutputStream(type, mask)) {
                assertThat(type.toString(), out, notNullValue());

                List<AggrPoint> source = IntStream.of(0, 10)
                        .mapToObj(ignore -> randomPoint(mask))
                        .collect(toList());

                for (AggrPoint point : source) {
                    out.writePoint(mask, point);
                }

                assertThat(out.recordCount(), equalTo(source.size()));
                try (TimeSeriesInputStream in = createInputStream(type, mask, out.getCompressedData())) {
                    for (AggrPoint expected : source) {
                        AggrPoint temp = new AggrPoint();
                        assertThat(type.toString(), in.hasNext(), equalTo(true));
                        in.readPoint(mask, temp);
                        assertThat(type.toString(), temp, equalTo(expected));
                    }
                }
            }
        });
    }

    @Test
    public void createEmptyOutputStreamByZeroMask() {
        forEachType(type -> {
            try (var stream = createOutputStream(type, 0)) {
                assertThat(type.toString(), stream, sameInstance(emptyOutputStream()));
            }
        });
    }

    @Test
    public void createEmptyInputStreamByZeroMask() {
        forEachType(type -> {
            try (var in = createInputStream(type, 0, ReadOnlyHeapBitBuf.EMPTY)) {
                assertThat(type.toString(), in, sameInstance(emptyInputStream()));
            }
        });
    }

    @Test
    public void objectSizeEmptyOutputStream() {
        forEachType(type -> {
            int mask = randomMask(type);
            try (var out = createOutputStream(type, mask)) {
                assertThat(type.name(), out.memorySizeIncludingSelf(), greaterThanOrEqualTo(expectedLayout(out)));
            }
        });
    }

    @Test
    public void objectSizeOutputStream() {
        forEachType(type -> {
            int mask = randomMask(type);
            try (var out = createOutputStream(type, mask)) {
                for (int index = 0; index < 100; index++) {
                    out.writePoint(mask, randomPoint(mask));
                }

                assertThat(type.name(), out.memorySizeIncludingSelf(), greaterThanOrEqualTo(expectedLayout(out)));
            }
        });
    }

    @Test
    public void countOverflow() {
        forEachType(type -> {
            int mask = StockpileColumns.minColumnSet(type) | StockpileColumn.COUNT.mask();
            try (var out = createOutputStream(type, mask)) {
                List<AggrPoint> source = IntStream.of(0, 10)
                    .mapToObj(index -> {
                        AggrPoint point = randomPoint(mask);
                        point.setCount(ThreadLocalRandom.current().nextLong(Integer.MAX_VALUE, Long.MAX_VALUE));
                        return point;
                    })
                    .collect(toList());

                for (AggrPoint point : source) {
                    out.writePoint(mask, point);
                }

                assertEquals(source.size(), out.recordCount());
                try (TimeSeriesInputStream in = createInputStream(type, mask, out.getCompressedData())) {
                    AggrPoint temp = new AggrPoint();
                    for (AggrPoint expected : source) {
                        assertTrue(type.toString(), in.hasNext());
                        in.readPoint(mask, temp);
                        assertEquals(type.toString(), expected, temp);
                    }
                }
            }
        });
    }

    private long expectedLayout(TimeSeriesOutputStream source) {
        var instanceSize = ClassLayout.parseClass(source.getClass()).instanceSize();
        var bytes = source.getCompressedData().bytesSize();
        return instanceSize + bytes;
    }

    private void forEachType(Consumer<MetricType> consumer) {
        for (MetricType type : MetricType.values()) {
            if (type == MetricType.UNRECOGNIZED || type == MetricType.METRIC_TYPE_UNSPECIFIED) {
                continue;
            }

            consumer.accept(type);
        }
    }
}
