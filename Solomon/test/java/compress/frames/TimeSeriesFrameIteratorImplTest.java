package ru.yandex.solomon.codec.compress.frames;

import org.junit.Test;

import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.ReadOnlyHeapBitBuf;
import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.compress.TimeSeriesOutputStream;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
public class TimeSeriesFrameIteratorImplTest {
    private StockpileFormat format = StockpileFormat.CURRENT;
    private MetricType type = MetricType.DGAUGE;
    private int mask = TsColumn.mask | ValueColumn.mask;

    @Test
    public void empty() {
        var it = iterator(ReadOnlyHeapBitBuf.EMPTY);
        assertFalse(it.next(new Frame()));
        assertFalse(it.next(new Frame()));
    }

    @Test
    public void oneClosed() {
        long ts0 = System.currentTimeMillis();
        long ts1 = ts0 + 10_000;
        var point = randomPoint(mask);

        var ts = out();
        point.tsMillis = ts0;
        ts.writePoint(mask, point);
        point.tsMillis = ts1;
        ts.writePoint(mask, point);
        ts.forceCloseFrame();

        BitBuf buffer = ts.getCompressedData();

        var it = iterator(buffer);
        {
            var frame = it.nextFrame();
            assertNotNull(frame);
            assertEquals(ts0, frame.firstTsMillis);
            assertEquals(ts1, frame.lastTsMillis);
            assertEquals(2, frame.records);
            assertEquals(0, frame.pos);
            assertEquals(buffer.readableBits(), frame.size);
            assertTrue(frame.closed);
        }
        assertNull(it.nextFrame());
    }

    @Test
    public void oneUnclosed() {
        long ts0 = System.currentTimeMillis();
        long ts1 = ts0 + 10_000;
        var point = randomPoint(mask);

        var ts = out();
        point.tsMillis = ts0;
        ts.writePoint(mask, point);
        point.tsMillis = ts1;
        ts.writePoint(mask, point);

        BitBuf buffer = ts.getCompressedData();

        var it = iterator(buffer);
        {
            var frame = it.nextFrame();
            assertNotNull(frame);
            assertEquals(ts0, frame.firstTsMillis);
            assertEquals(0, frame.lastTsMillis);
            assertEquals(0, frame.records);
            assertEquals(0, frame.pos);
            assertEquals(buffer.readableBits(), frame.size);
            assertFalse(frame.closed);
        }
        assertNull(it.nextFrame());
    }

    @Test
    public void oneClosedOneUnclosed() {
        long ts0 = System.currentTimeMillis();
        long ts1 = ts0 + 10_000;
        long ts2 = ts1 + 10_000;
        var point = randomPoint(mask);

        var ts = out();
        point.tsMillis = ts0;
        ts.writePoint(mask, point);
        point.tsMillis = ts1;
        ts.writePoint(mask, point);
        ts.forceCloseFrame();
        long secondFramePos = ts.getCompressedData().writerIndex();
        point.tsMillis = ts2;
        ts.writePoint(mask, point);

        BitBuf buffer = ts.getCompressedData();

        var it = iterator(buffer);
        {
            var frame = it.nextFrame();
            assertNotNull(frame);
            assertEquals(ts0, frame.firstTsMillis);
            assertEquals(ts1, frame.lastTsMillis);
            assertEquals(2, frame.records);
            assertEquals(0, frame.pos);
            assertEquals(secondFramePos, frame.size);
            assertTrue(frame.closed);
        }
        {
            var frame = it.nextFrame();
            assertNotNull(frame);
            assertEquals(ts2, frame.firstTsMillis);
            assertEquals(0, frame.lastTsMillis);
            assertEquals(0, frame.records);
            assertEquals(secondFramePos, frame.pos);
            assertEquals(buffer.writerIndex() - secondFramePos, frame.size);
            assertFalse(frame.closed);
        }
        assertNull(it.nextFrame());
    }

    private TimeSeriesFrameIterator iterator(BitBuf buffer) {
        return new TimeSeriesFrameIteratorImpl(buffer.asReadOnly());
    }

    private TimeSeriesOutputStream out() {
        return CompressStreamFactory.createOutputStream(type, mask);
    }
}
