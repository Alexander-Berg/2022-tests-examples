package ru.yandex.solomon.codec.compress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import io.netty.buffer.ByteBufAllocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.NettyBitBuf;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayListViewIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.createInputStream;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.createOutputStream;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class CompressUncompressRandomTest {
    @Parameterized.Parameter(0)
    public StockpileFormat format;
    @Parameterized.Parameter(1)
    public MetricType type;

    @Parameterized.Parameters(name = "{1}: {0}")
    public static List<Object[]> data() {
        Object[] kinds = Stream.of(MetricType.values())
                .filter(k -> k != MetricType.METRIC_TYPE_UNSPECIFIED && k != MetricType.UNRECOGNIZED)
                .toArray();

        List<Object[]> pairs = new ArrayList<>();
        for (StockpileFormat format : StockpileFormat.values()) {
            for (int index = 0; index < kinds.length; index++) {
                pairs.add(new Object[]{format, kinds[index]});
            }
        }
        return pairs;
    }

    @Before
    public void setUp() throws Exception {
        assumeTrue("Unsupported format " + format + " for type " + type, CompressStreamFactory.isSupported(type));
    }

    @Test
    public void random() {
        for (int i = 0; i < 100; ++i) {
            AggrGraphDataArrayList source = generateDataSet();
            BitBuf compressData = encode(source);
            AggrGraphDataArrayList result = decode(source.columnSetMask(), compressData);
            assertEquals(source, result);
            source.clear();
            result.clear();
        }
    }

    @Test
    public void randomWriteAndRead() {
        var tmpOne = RecyclableAggrPoint.newInstance();
        var tmpTwo = RecyclableAggrPoint.newInstance();
        var random = ThreadLocalRandom.current();
        for (int index = 0; index < 100; index++) {
            int mask = randomMask(type);

            TimeSeriesOutputStream output = CompressStreamFactory.createOutputStream(type, mask, 100);
            AggrGraphDataArrayList expectedList = new AggrGraphDataArrayList(mask, 100);
            for (int pointIndex = 0; pointIndex < 100; pointIndex++) {
                AggrPoint point = randomPoint(tmpOne, mask, random);
                output.writePoint(mask, point);
                expectedList.addRecordData(mask, point);
            }

            TimeSeriesInputStream input = CompressStreamFactory.createInputStream(type, mask, output.getCompressedData());

            tmpTwo.columnSet = mask;
            for (int pointIndex = 0; pointIndex < 100; pointIndex++) {
                expectedList.getDataTo(pointIndex, tmpTwo);

                tmpOne.columnSet = mask;
                input.readPoint(mask, tmpOne);

                assertEquals(tmpTwo, tmpOne);
            }

            output.close();
            input.close();
        }
        tmpOne.recycle();
        tmpTwo.recycle();
    }

    @Test
    public void onCloseReleaseRefCount() {
        int mask = randomMask(type);
        var sourceBuffer = new NettyBitBuf(ByteBufAllocator.DEFAULT.heapBuffer(8), 0);
        var output = CompressStreamFactory.createOutputStream(type, mask, sourceBuffer, 0);
        output.writePoint(mask, randomPoint(mask));

        var compressed = output.getCompressedData();
        assertEquals(1, compressed.refCnt());
        assertEquals(1, sourceBuffer.refCnt());
        output.close();

        assertEquals(0, compressed.refCnt());
        assertEquals(0, sourceBuffer.refCnt());
    }

    @Test
    public void copyOutputStream() {
        int mask = randomMask(type);
        TimeSeriesOutputStream source, copy;
        source = createOutputStream(type, mask);
        for (int index = 0; index < 3; index++) {
            source.writePoint(mask, randomPoint(mask));
        }

        copy = source.copy();
        for (int index = 0; index < 3; index++) {
            var point = randomPoint(mask);
            source.writePoint(mask, point);
            copy.writePoint(mask, point);
        }

        assertEquals(type.name(), source.getCompressedData(), copy.getCompressedData());
        source.close();
        copy.close();
    }

    private AggrGraphDataArrayList generateDataSet() {
        var random = ThreadLocalRandom.current();
        int mask = randomMask(type);
        int len = 1 + random.nextInt(100);

        AggrGraphDataArrayList al = new AggrGraphDataArrayList(mask, len);
        var point = RecyclableAggrPoint.newInstance();
        for (int j = 0; j < len; ++j) {
            al.addRecordData(mask, randomPoint(point, mask, random));
        }
        point.recycle();

        return al;
    }

    private BitBuf encode(AggrGraphDataArrayList dataSet) {
        try (TimeSeriesOutputStream out = createOutputStream(type, dataSet.columnSetMask())) {
            AggrPoint tempPoint = new AggrPoint();
            AggrGraphDataArrayListViewIterator it = dataSet.iterator();
            while (it.next(tempPoint)) {
                out.writePoint(dataSet.columnSetMask(), tempPoint);
            }

            return out.getCompressedData().retain();
        }
    }

    private AggrGraphDataArrayList decode(int mask, BitBuf compressedData) {
        try (TimeSeriesInputStream input = createInputStream(type, mask, compressedData)) {
            AggrPoint tempPoint = new AggrPoint();
            tempPoint.columnSet = mask;

            AggrGraphDataArrayList result = new AggrGraphDataArrayList(mask, 10);
            while (input.hasNext()) {
                input.readPoint(mask, tempPoint);
                result.addRecordData(mask, tempPoint);
            }

            return result;
        } finally {
            compressedData.release();
        }
    }
}
