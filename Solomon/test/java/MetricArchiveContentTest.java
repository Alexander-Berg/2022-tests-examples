package ru.yandex.solomon.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class MetricArchiveContentTest {

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

    private AggrPoint[] generateDataSet() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        AggrPoint[] result = new AggrPoint[100];
        long now = System.currentTimeMillis();
        int mask = randomMask(type);
        for (int index = 0; index < 100; index++) {
            AggrPoint point = randomPoint(mask);
            point.setTsMillis(now);
            now += random.nextLong(100, 15_000);
            result[index] = point;
        }
        return result;
    }

    @Test
    public void toMetricArchiveAndBack() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(generateDataSet());

        MetricArchiveImmutable content = MetricArchiveImmutable.of(type, source);
        assertThat(content.columnSetMask(), equalTo(source.columnSetMask()));

        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();
        assertThat(result, equalTo(source));
        close(content);
    }

    @Test
    public void sortAndMerge() {
        AggrPoint[] points = generateDataSet();
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(points);

        List<AggrPoint> unordered = Arrays.asList(points);
        Collections.shuffle(unordered);

        MetricArchiveMutable mContent = MetricArchiveMutable.of(type, source);
        MetricArchiveImmutable iContent = mContent.toImmutable();
        AggrGraphDataArrayList result = iContent.toAggrGraphDataArrayList();
        assertThat(result, equalTo(source));
        close(mContent, iContent);
    }
}
