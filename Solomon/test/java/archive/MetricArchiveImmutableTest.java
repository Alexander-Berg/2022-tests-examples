package ru.yandex.solomon.codec.archive;

import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

import ru.yandex.solomon.codec.archive.header.MetricHeader;
import ru.yandex.solomon.codec.bits.ReadOnlyHeapBitBuf;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricArchiveImmutableTest {

    @Test(expected = IllegalArgumentException.class)
    public void failWithInvalidMaskOnNewFormatVersion() {
        int invalidMask = StockpileColumn.TS.mask()
                | StockpileColumn.VALUE.mask()
                | StockpileColumn.HISTOGRAM.mask()
                | StockpileColumn.LONG_VALUE.mask();

        MetricArchiveImmutable archive = new MetricArchiveImmutable(
                MetricHeader.defaultValue.withKind(MetricType.DGAUGE),
                StockpileFormat.CURRENT,
                invalidMask,
                ReadOnlyHeapBitBuf.EMPTY,
                1);

        fail("Archive should failed if mask wrote in invalid format: " + archive.columnSetMask());
    }

    @Test
    public void emptyArchiveDoesNotHaveMask() {
        MetricArchiveImmutable archive = new MetricArchiveImmutable(
                MetricHeader.defaultValue,
                StockpileFormat.CURRENT,
                StockpileColumnSet.empty.columnSetMask(),
                ReadOnlyHeapBitBuf.EMPTY,
                0);
        assertThat(archive.columnSetMask(), equalTo(StockpileColumnSet.empty.columnSetMask()));
        close(archive);
    }

    @Test
    public void objectSizeEmpty() {
        MetricArchiveImmutable archive = MetricArchiveImmutable.empty;
        GraphLayout gl = GraphLayout.parseInstance(archive);

        // Exclude enums because exists into single instance into app
        long enumSize = GraphLayout.parseInstance(archive.getType(), archive.getFormat()).totalSize();
        assertEquals(gl.totalSize() - enumSize, archive.memorySizeIncludingSelf());
        close(archive);
    }

    @Test
    public void objectSizeNoEmpty() {
        final int mask = randomMask(MetricType.DGAUGE);

        MetricArchiveMutable mutable = new MetricArchiveMutable();
        mutable.setType(MetricType.DGAUGE);
        mutable.ensureCapacity(mask, 100);

        for (int index = 0; index < 100; index++) {
            mutable.addRecordData(mask, randomPoint(mask));
        }

        MetricArchiveImmutable archive = mutable.toImmutable();
        assertThat(archive.memorySizeIncludingSelfInt(), greaterThan(archive.bytesCount()));
        close(mutable, archive);
    }
}
