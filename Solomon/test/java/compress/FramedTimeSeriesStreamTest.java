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

import ru.yandex.solomon.codec.archive.FramedTimeSeries;
import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.NettyBitBuf;
import ru.yandex.solomon.codec.compress.frames.Frame;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.createInputStream;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.createOutputStream;
import static ru.yandex.solomon.codec.compress.CompressStreamFactory.restoreOutputStream;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class FramedTimeSeriesStreamTest {
    @Parameterized.Parameter
    public StockpileFormat format;
    @Parameterized.Parameter(1)
    public MetricType type;
    private int mask;

    @Parameterized.Parameters(name = "{1}: {0}")
    public static List<Object[]> data() {
        Object[] types = Stream.of(MetricType.values())
            .filter(k -> k != MetricType.METRIC_TYPE_UNSPECIFIED && k != MetricType.UNRECOGNIZED)
            .toArray();

        List<Object[]> pairs = new ArrayList<>();
        for (StockpileFormat format : StockpileFormat.values()) {
            for (Object type : types) {
                pairs.add(new Object[]{format, type});
            }
        }
        return pairs;
    }

    @Before
    public void setUp() throws Exception {
        assumeTrue("Unsupported format " + format + " for type " + type, CompressStreamFactory.isSupported(type));
        mask = randomMask(type);
    }

    @Test
    public void frameRecordCount() {
        var point = new AggrPoint();

        var out = createOutputStream(type, mask);

        assertEquals(0, out.recordCount());
        assertEquals(0, out.frameRecordCount());

        out.writePoint(mask, randomPoint(point, mask));

        assertEquals(1, out.recordCount());
        assertEquals(1, out.frameRecordCount());

        int expectedRecords = 1;
        for (int index = 0; index < 3; index++) {
            while (!out.closeFrame()) {
                out.writePoint(mask, randomPoint(point, mask));
                expectedRecords++;
            }

            assertEquals(expectedRecords, out.recordCount());
            assertEquals(0, out.frameRecordCount());

            expectedRecords++;
            out.writePoint(mask, randomPoint(point, mask));
            assertEquals(expectedRecords, out.recordCount());
            assertEquals(1, out.frameRecordCount());
        }
        close(out);
    }

    @Test
    public void frameBytesCount() {
        var point = new AggrPoint();
        var out = createOutputStream(type, mask);

        assertEquals(0, out.bytesCount());
        assertEquals(0, out.frameBytesCount());

        out.writePoint(mask, randomPoint(point, mask));

        assertThat(out.bytesCount(), greaterThan(0));
        assertEquals(out.bytesCount(), out.frameBytesCount());

        int prevBytesCount;
        for (int index = 0; index < 3; index++) {
            prevBytesCount = out.bytesCount();
            while (!out.closeFrame()) {
                out.writePoint(mask, randomPoint(point, mask));
            }

            assertThat(out.bytesCount(), greaterThan(prevBytesCount));
            assertEquals(0, out.frameBytesCount());

            prevBytesCount = out.bytesCount();

            out.writePoint(mask, randomPoint(point, mask));
            assertThat(out.bytesCount(), greaterThan(prevBytesCount));
            assertEquals(out.bytesCount() - prevBytesCount, out.frameBytesCount());
        }
        close(out);
    }

    @Test
    public void copy() {
        var point = new AggrPoint();
        var out = createOutputStream(type, mask);

        for (int index = 0; index < 2; index++) {
            while (!out.closeFrame()) {
                out.writePoint(mask, randomPoint(point, mask));
            }
        }
        out.writePoint(mask, randomPoint(point, mask));

        TimeSeriesOutputStream copy = out.copy();

        assertEquals(out.recordCount(), copy.recordCount());
        assertEquals(out.frameRecordCount(), copy.frameRecordCount());
        assertEquals(out.bytesCount(), copy.bytesCount());
        assertEquals(out.frameRecordCount(), copy.frameRecordCount());
        assertEquals(out.getCompressedData(), copy.getCompressedData());

        randomPoint(point, mask);

        out.writePoint(mask, point);
        assertNotEquals(out.getCompressedData(), copy.getCompressedData());

        copy.writePoint(mask, point);
        assertEquals(out.getCompressedData(), copy.getCompressedData());
        close(out, copy);
    }

    @Test
    public void copyAndCompleteFrame() {
        var out = createOutputStream(type, mask);
        var expectedPoints = new ArrayList<AggrPoint>();

        // copy empty
        {
            var copy = out.copy();
            assertEquals(out.getCompressedData(), copy.getCompressedData());
            assertRead(expectedPoints, copy.getCompressedData());

            var additional = writePoints(copy, 1);
            assertRead(concat(expectedPoints, additional), copy.getCompressedData());
            close(copy);
        }

        expectedPoints.addAll(writePoints(out, 2));

        // copy last frame closed
        {
            var copy = out.copy();
            assertEquals(out.getCompressedData(), copy.getCompressedData());
            assertRead(expectedPoints, copy.getCompressedData());

            var additional = writePoints(copy, 1);
            assertRead(concat(expectedPoints, additional), copy.getCompressedData());
            close(copy);
        }

        var point = randomPoint(mask);
        expectedPoints.add(point);
        out.writePoint(mask, point);

        // last frame not closed
        {
            var copy = out.copy();
            assertEquals(out.getCompressedData(), copy.getCompressedData());
            assertRead(expectedPoints, copy.getCompressedData());

            var additional = writePoints(copy, 1);
            assertRead(concat(expectedPoints, additional), copy.getCompressedData());
            close(copy);
        }

        TimeSeriesOutputStream copy = out.copy();

        assertEquals(out.recordCount(), copy.recordCount());
        assertEquals(out.frameRecordCount(), copy.frameRecordCount());
        assertEquals(out.bytesCount(), copy.bytesCount());
        assertEquals(out.frameRecordCount(), copy.frameRecordCount());
        assertEquals(out.getCompressedData(), copy.getCompressedData());
        close(out, copy);
    }

    @Test
    public void read() {
        var out = createOutputStream(type, mask);
        var expectedPoints = new ArrayList<AggrPoint>();

        assertRead(expectedPoints, out.getCompressedData());
        // first frame unclosed
        {
            var point = randomPoint(mask);
            expectedPoints.add(point);
            out.writePoint(mask, point);
            assertRead(expectedPoints, out.getCompressedData());
        }

        for (int index = 0; index < 2; index++) {
            while (!out.closeFrame()) {
                var point = randomPoint(mask);
                expectedPoints.add(point);
                out.writePoint(mask, point);
            }
        }

        // last frame closed
        {
            assertEquals(0, out.frameRecordCount());
            assertRead(expectedPoints, out.getCompressedData());
        }

        for (int index = 0; index < ThreadLocalRandom.current().nextInt(1, 5); index++) {
            var point = randomPoint(mask);
            expectedPoints.add(point);
            out.writePoint(mask, point);
        }

        // last frame unclosed
        {
            assertNotEquals(0, out.frameRecordCount());
            assertRead(expectedPoints, out.getCompressedData());
        }
        close(out);
    }

    @Test
    public void restore() {
        var out = createOutputStream(type, mask);
        var expectedPoints = new ArrayList<AggrPoint>();

        var restored = restoreOutputStream(type, mask, out.getCompressedData());
        assertRead(expectedPoints, restored.getCompressedData());
        close(restored);
        // first frame unclosed
        {
            var point = randomPoint(mask);
            expectedPoints.add(point);
            out.writePoint(mask, point);

            restored = restoreOutputStream(type, mask, out.getCompressedData());
            point = randomPoint(mask);

            restored.writePoint(mask, point);
            assertRead(concat(expectedPoints, point), restored.getCompressedData());
            close(restored);
        }

        for (int index = 0; index < 2; index++) {
            while (!out.closeFrame()) {
                var point = randomPoint(mask);
                expectedPoints.add(point);
                out.writePoint(mask, point);
            }
        }

        // last frame closed
        {
            assertEquals(0, out.frameRecordCount());
            restored = restoreOutputStream(type, mask, out.getCompressedData());
            var point = randomPoint(mask);
            restored.writePoint(mask, point);
            assertRead(concat(expectedPoints, point), restored.getCompressedData());
            close(restored);
        }

        for (int index = 0; index < ThreadLocalRandom.current().nextInt(1, 5); index++) {
            var point = randomPoint(mask);
            expectedPoints.add(point);
            out.writePoint(mask, point);
        }

        // last frame unclosed
        {
            assertNotEquals(0, out.frameRecordCount());
            restored = restoreOutputStream(type, mask, out.getCompressedData());
            var point = randomPoint(mask);
            restored.writePoint(mask, point);
            assertRead(concat(expectedPoints, point), restored.getCompressedData());
            close(restored);
        }
        close(out);
    }

    @Test
    public void framesEmpty() {
        var out = createOutputStream(type, mask);

        var frames = frames(out);
        assertEquals(0, frames.size());
        close(out);
    }

    @Test
    public void framesOneUnclosed() {
        var out = createOutputStream(type, mask);

        long lastSize = 0;
        for (int index = 0; index < 5; index++) {
            var point = randomPoint(mask);
            out.writePoint(mask, point);

            var frames = frames(out);
            assertEquals(1, frames.size());

            var frame = frames.get(0);
            assertEquals(0, frame.pos);
            assertNotEquals(lastSize, frame.size);
            assertFalse(frame.closed);

            lastSize = frame.size;
        }
        close(out);
    }

    @Test
    public void framesAllClosed() {
        var out = createOutputStream(type, mask);

        var pointsAtFrameOne = writePoints(out, 1);
        {
            var frames = frames(out);
            assertEquals(1, frames.size());

            var frame = frames.get(0);
            assertTrue(frame.closed);
            assertEquals(pointsAtFrameOne.get(pointsAtFrameOne.size() - 1).tsMillis, frame.lastTsMillis);
            assertRead(pointsAtFrameOne, out.getCompressedData().slice(frame.pos, frame.size));
        }

        var pointAtFrameTwo = writePoints(out, 1);
        {
            var frames = frames(out);
            assertEquals(2, frames.size());

            var first = frames.get(0);
            var last = frames.get(1);
            assertTrue(last.closed);
            assertEquals(pointAtFrameTwo.get(pointAtFrameTwo.size() - 1).tsMillis, last.lastTsMillis);

            var compressed = out.getCompressedData();
            assertRead(concat(pointsAtFrameOne, pointAtFrameTwo), compressed);
            assertRead(pointsAtFrameOne, compressed.slice(first.pos, first.size));
            assertRead(pointAtFrameTwo, compressed.slice(last.pos, last.size));
        }

        var pointsAtFrameThree = writePoints(out, 1);
        {
            var frames = frames(out);
            var compressed = out.getCompressedData();
            var expected = List.of(pointsAtFrameOne, pointAtFrameTwo, pointsAtFrameThree);

            assertEquals(expected.size(), frames.size());
            for (int index = 0; index < expected.size(); index++) {
                var frame = frames.get(index);
                var expectPoints = expected.get(index);

                var expectedLastTsMillis = expectPoints.get(expectPoints.size() - 1).tsMillis;
                assertTrue(frame.closed);
                assertEquals(expectedLastTsMillis, frame.lastTsMillis);
                assertRead(expectPoints, compressed.slice(frame.pos, frame.size));
                if (index > 0) {
                    var prevFrame = frames.get(index - 1);
                    var compressedTwoFrame = compressed.slice(prevFrame.pos, prevFrame.size + frame.size);
                    assertRead(concat(expected.get(index - 1), expected.get(index)), compressedTwoFrame);
                }
            }
        }
        close(out);
    }

    @Test
    public void repackSafeBitBufType() {
        var buffer = new NettyBitBuf(ByteBufAllocator.DEFAULT.buffer(), 0);
        try {
            var out = createOutputStream(type, mask, buffer, 0);
            out.writePoint(mask, randomPoint(mask));
            out.writePoint(mask, randomPoint(mask));
            out.writePoint(mask, randomPoint(mask));
            TimeSeriesOutputStream repacked = FramedTimeSeries.repack(out.getCompressedData(), format, type, mask, mask);
            var compressed = repacked.getCompressedData();
            assertNotSame(buffer, compressed);
            assertEquals(buffer.asReadOnly(), compressed.duplicate());
            assertThat(compressed, instanceOf(NettyBitBuf.class));
            assertEquals(buffer.isDirect(), compressed.isDirect());
            repacked.close();
        } finally {
            buffer.release();
        }
    }

    @Test
    public void sortAndMergeSafeBitBufType() {
        var buffer = new NettyBitBuf(ByteBufAllocator.DEFAULT.buffer(1024), 0);
        var out = createOutputStream(type, mask, buffer, 0);
        try {
            writePoints(out, 3);
            TimeSeriesOutputStream merged = FramedTimeSeries.merge(out, format, type, mask, false);
            var compressed = merged.getCompressedData();
            assertNotSame(buffer, compressed);
            assertThat(compressed, instanceOf(NettyBitBuf.class));
            assertEquals(buffer.isDirect(), compressed.isDirect());
            merged.close();
        } finally {
            out.close();
        }
    }

    private List<AggrPoint> writePoints(TimeSeriesOutputStream out, int frameCount) {
        List<AggrPoint> points = new ArrayList<>();
        for (int index = 0; index < frameCount; index++) {
            while (!out.closeFrame()) {
                var point = randomPoint(mask);
                points.add(point);
                out.writePoint(mask, point);
            }
        }
        return points;
    }

    private void assertRead(List<AggrPoint> expectedPoints, BitBuf buffer) {
        try (var in = createInputStream(type, mask, buffer)) {
            var point = RecyclableAggrPoint.newInstance();
            for (var expected : expectedPoints) {
                assertTrue(in.hasNext());

                in.readPoint(mask, point);
                assertEquals(expected, point);
            }
            assertFalse(in.hasNext());
            point.recycle();
        }
    }

    private List<AggrPoint> concat(List<AggrPoint> points, AggrPoint... additional) {
        return concat(points, List.of(additional));
    }

    private List<AggrPoint> concat(List<AggrPoint> points, List<AggrPoint> additional) {
        List<AggrPoint> result = new ArrayList<>(points.size() + additional.size());
        result.addAll(points);
        result.addAll(additional);
        return result;
    }

    private List<Frame> frames(TimeSeriesOutputStream out) {
        var it = CompressStreamFactory.createFrameIterator(out.getCompressedData());
        Frame frame = new Frame();
        List<Frame> result = new ArrayList<>();
        while (it.next(frame)) {
            result.add(frame);
            frame = new Frame();
        }
        return result;
    }
}
