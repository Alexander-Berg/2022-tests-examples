package ru.yandex.solomon.codec;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.model.timeseries.AggrGraphDataLists.toAggrListUnsorted;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class MetricArchiveMutableKindParametrizedTest {
    @Parameterized.Parameter
    public MetricType type;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return Stream.of(MetricType.values())
                .filter(k -> k != MetricType.METRIC_TYPE_UNSPECIFIED && k != MetricType.UNRECOGNIZED)
                .toArray();
    }

    @Before
    public void setUp() throws Exception {
        assumeTrue("Unsupported format " + StockpileFormat.CURRENT + " for type " + type, CompressStreamFactory.isSupported(type));
    }

    @Test
    public void randomShortFull() {
        var tmpPoint = RecyclableAggrPoint.newInstance();
        var random = ThreadLocalRandom.current();
        for (int i = 0; i < 100; ++i) {
            // shorter are easier to debug
            int maxLength = Math.min(i, 100);

            MetricArchiveMutable b = new MetricArchiveMutable();
            b.setType(type);

            AggrGraphDataArrayList l = new AggrGraphDataArrayList();

            int length = random.nextInt(maxLength + 1);
            for (int j = 0; j < length; ++j) {
                int mask = randomMask(type);
                AggrPoint point = randomPoint(tmpPoint, mask, random);
                l.addRecordData(mask, point);
                b.addRecordData(mask, point);
            }

            l.sortAndMerge();
            b.sortAndMerge();

            assertEquals(l, toAggrListUnsorted(b));
            close(b);
        }
        tmpPoint.recycle();
    }

    @Test
    public void memorySizeArchive() {
        final int mask = randomMask(type);
        StockpileColumnSet.validate(mask);

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(type);
        archive.ensureCapacity(mask, 100);

        for (int index = 0; index < 100; index++) {
            archive.addRecordData(mask, randomPoint(mask));
        }

        assertThat(archive.memorySizeIncludingSelfInt(), greaterThan(archive.getCompressedDataRaw().bytesSize()));
        close(archive);
    }
}
