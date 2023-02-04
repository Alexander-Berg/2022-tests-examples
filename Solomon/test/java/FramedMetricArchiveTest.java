package ru.yandex.solomon.codec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.solomon.codec.archive.MetricArchiveGeneric;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.CountColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
public class FramedMetricArchiveTest {

    @Test
    public void emptyToMutable() {
        var source = allocate();
        var immutable = source.toImmutable();
        var target = immutable.toMutable();

        assertEquals(source, target);
        close(source, immutable, target);
    }

    @Test
    public void toMutableNoFrame() {
        var source = allocate();

        for (int i = 0; i < 100; i++) {
            source.addRecord(randomPoint(source.columnSetMask()));
        }

        var immutable = source.toImmutableNoCopy();
        assertEquals(source.toAggrGraphDataArrayList(), immutable.toAggrGraphDataArrayList());

        var target = immutable.toMutable();
        assertEquals(source, target);
        close(source, immutable, target);
    }

    @Test
    public void lastFrameContinueAfterConvertToMutable() {
        var source = allocate();
        writeUntilCloseFrame(source);

        assertEquals(0, source.frameRecordCount());
        assertEquals(0, source.frameBytesCount());

        var immutable = source.toImmutableNoCopy();
        assertEquals(source.toAggrGraphDataArrayList(), immutable.toAggrGraphDataArrayList());

        var target = immutable.toMutable();
        assertEquals(source.getRecordCount(), target.frameRecordCount());
        assertNotEquals(0, target.frameBytesCount());
        assertThat(target.frameBytesCount(), lessThan(source.bytesCount()));
        assertEquals(source, target);

        assertTrue(target.closeFrame());
        assertEquals(source.getCompressedDataRaw(), target.getCompressedDataRaw());
        close(source, immutable, target);
    }

    @Test
    public void toMutableLastFrameClosed() {
        var source = allocate();

        writeUntilCloseFrame(source);

        var immutable = source.toImmutableNoCopy();
        assertEquals(source.toAggrGraphDataArrayList(), immutable.toAggrGraphDataArrayList());

        var target = immutable.toMutable();
        assertEquals(source, target);
        close(source, immutable, target);
    }

    @Test
    public void toMutableLastFrameUnclosed() {
        var source = allocate();

        writeUntilCloseFrame(source);

        for (int index = 0; index < 10; index++) {
            var point = randomPoint(source.columnSetMask());
            point.tsMillis = source.getLastTsMillis() + 1_000;
            source.addRecord(point);
        }

        var immutable = source.toImmutableNoCopy();
        assertEquals(source.toAggrGraphDataArrayList(), immutable.toAggrGraphDataArrayList());

        var target = immutable.toMutable();
        assertEquals(source, target);
        close(source, immutable, target);
    }

    @Test
    public void immutableAlwaysSorted() {
        var source = allocate();

        List<AggrPoint> expected = writeUntilCloseFrame(source);

        var target = source.toImmutable().toMutable();
        assertRead(expected, target.toImmutable());

        AggrPoint point = new AggrPoint(expected.get(0));
        point.tsMillis -= 1_000;
        target.addRecord(point);

        assertRead(concat(List.of(point), expected), target.toImmutable());
        close(source, target);
    }

    @Test
    public void immutableDonNotContainDuplicates() {
        var source = allocate();

        List<AggrPoint> expected = writeUntilCloseFrame(source);

        var target = source.toImmutable().toMutable();
        assertRead(expected, target.toImmutable());

        AggrPoint duplicate = new AggrPoint(expected.get(expected.size() - 1));

        int records = target.getRecordCount();
        target.addRecord(duplicate);

        assertEquals(records + 1, target.getRecordCount());
        assertRead(expected, target.toImmutable());
        close(source, target);
    }

    @Test
    public void sortAndMergeLastFrame() {
        var source = allocate();

        var expected = writeUntilCloseFrame(source);
        assertRead(expected, source);

        long ts0 = source.getLastTsMillis();
        var unsorted = new ArrayList<AggrPoint>();
        for (int index = 0; index < 1_000; index++) {
            AggrPoint point = randomPoint(source.columnSetMask());
            point.tsMillis = ts0 + ThreadLocalRandom.current().nextLong(1, TimeUnit.DAYS.toMillis(1));

            unsorted.add(point);
            source.addRecord(point);
        }

        unsorted.sort(Comparator.comparingLong(o -> o.tsMillis));
        source.sortAndMerge();

        assertRead(concat(expected, unsorted), source);
        assertEquals(unsorted.size(), source.frameRecordCount());
        assertNotEquals(source.bytesCount(), source.frameBytesCount());
        close(source);
    }

    @Test
    public void mergeLastAllIntoPrev() {
        var source = allocate();

        var expected = writeUntilCloseFrame(source);
        assertRead(expected, source);

        var last = expected.get(expected.size() - 1);
        last.valueNum = 42;
        last.valueDenom = ValueColumn.DEFAULT_DENOM;

        source.addRecord(last);
        source.addRecord(last);
        source.addRecord(last);
        last.valueNum = 43;
        source.addRecord(last);
        last.valueNum = 44;
        source.addRecord(last);

        source.sortAndMerge();
        assertRead(expected, source);
        assertEquals(0, source.frameRecordCount());
        assertEquals(0, source.frameBytesCount());
        close(source);
    }

    @Test
    public void mergeLast() {
        var source = allocate();

        var expected = writeUntilCloseFrame(source);
        assertRead(expected, source);

        var last = expected.get(expected.size() - 1);
        last.valueNum = 42;
        last.valueDenom = ValueColumn.DEFAULT_DENOM;

        source.addRecord(last);
        source.addRecord(last);

        var additional = randomPoint(source.columnSetMask());
        additional.tsMillis = last.tsMillis + 10_000;
        source.addRecord(additional);

        assertEquals(3, source.frameRecordCount());
        source.sortAndMerge();

        assertRead(concat(expected, List.of(additional)), source);
        assertEquals(1, source.frameRecordCount());
        close(source);
    }

    @Test
    public void mergeDuplicateFromOneOfPrevFrame() {
        var source = allocate();

        var one = writeUntilCloseFrame(source);
        var two = writeUntilCloseFrame(source);
        var three = writeUntilCloseFrame(source);

        for (var target : List.of(one, two, three)) {
            var last = target.get(target.size() - 1);
            System.out.println("l: " + last);
            last.valueNum = 42;
            last.valueDenom = ValueColumn.DEFAULT_DENOM;
            System.out.println("e: " + last);
            source.addRecord(last);
            assertEquals(1, source.frameRecordCount());

            source.sortAndMerge();
            assertRead(concat(one, two, three), source);
            assertEquals(0, source.frameRecordCount());
        }
        close(source);
    }

    @Test
    public void addPointToOneOfFrame() {
        var source = allocate();

        var one = writeUntilCloseFrame(source);
        var two = writeUntilCloseFrame(source);
        var three = writeUntilCloseFrame(source);

        for (var target : List.of(one, two, three)) {
            var last = target.get(target.size() - 1);

            var prev = randomPoint(source.columnSetMask());
            prev.tsMillis = last.tsMillis - 200;
            target.set(target.size() - 1, prev);
            target.add(last);

            source.addRecord(prev);
            assertEquals(1, source.frameRecordCount());

            source.sortAndMerge();

            assertEquals(one.size() + two.size() + three.size(), source.getRecordCount());
            assertRead(concat(one, two, three), source);
            assertEquals(0, source.frameRecordCount());
        }
        close(source);
    }

    @Test
    public void addPointToFewFrames() {
        var source = allocate();

        var one = writeUntilCloseFrame(source);
        var two = writeUntilCloseFrame(source);
        var three = writeUntilCloseFrame(source);

        for (var target : List.of(one, two, three)) {
            var last = target.get(target.size() - 1);

            var prev = randomPoint(source.columnSetMask());
            prev.tsMillis = last.tsMillis - 200;
            target.set(target.size() - 1, prev);
            target.add(last);

            source.addRecord(prev);
        }
        assertEquals(3, source.frameRecordCount());

        source.sortAndMerge();
        assertRead(concat(one, two, three), source);
        assertEquals(0, source.frameRecordCount());
        close(source);
    }

    @Test
    public void addPointToFewFrameAndLast() {
        var source = allocate();

        var one = writeUntilCloseFrame(source);
        var two = writeUntilCloseFrame(source);
        var three = writeUntilCloseFrame(source);

        for (var target : List.of(two, three)) {
            var last = target.get(target.size() - 1);

            var prev = randomPoint(source.columnSetMask());
            prev.tsMillis = last.tsMillis - 1;
            target.set(target.size() - 1, prev);
            target.add(last);

            source.addRecord(prev);
        }

        var additional = randomPoint(source.columnSetMask());
        additional.tsMillis = source.getLastTsMillis() + 10_000;
        source.addRecord(additional);
        assertEquals(3, source.frameRecordCount());

        source.sortAndMerge();
        assertRead(concat(one, two, three, List.of(additional)), source);
        assertEquals(1, source.frameRecordCount());
        close(source);
    }

    @Test
    public void repackSaveFrames() {
        var source = allocate();
        var expected = writeUntilCloseFrame(source);

        var expectedMask = source.columnSetMask() | CountColumn.mask;
        var additional = randomPoint(expectedMask);
        additional.tsMillis = source.getLastTsMillis() + 1_000;
        source.addRecord(additional);

        assertEquals(source.columnSetMask(), expectedMask);
        assertEquals(1, source.frameRecordCount());
        assertRead(concat(expected, List.of(additional)), source);
        close(source);
    }

    private List<AggrPoint> writeUntilCloseFrame(MetricArchiveMutable archive) {
        List<AggrPoint> points = new ArrayList<>();
        if (archive.getLastTsMillis() == 0) {
            var point = randomPoint(archive.columnSetMask());
            points.add(point);
            archive.addRecord(point);
        }

        while (!archive.closeFrame()) {
            var point = randomPoint(archive.columnSetMask());
            point.tsMillis = archive.getLastTsMillis() + ThreadLocalRandom.current().nextLong(TimeUnit.SECONDS.toMillis(1), TimeUnit.HOURS.toMillis(1));
            points.add(point);
            archive.addRecord(point);
        }
        return points;
    }

    private MetricArchiveMutable allocate() {
        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        return archive;
    }

    private void assertRead(List<AggrPoint> expectedPoints, MetricArchiveGeneric archive) {
        var it = archive.iterator();
        var point = RecyclableAggrPoint.newInstance();
        for (var expected : expectedPoints) {
            assertTrue(expected + " but latest point " + point, it.next(point));
            assertEquals(expected, point);
        }
        boolean hasNext = it.next(point);
        assertFalse(point.toString(), hasNext);
        point.recycle();
    }

    private List<AggrPoint> concat(List<AggrPoint> points, List<AggrPoint>... additional) {
        List<AggrPoint> result = new ArrayList<>(points.size() + Stream.of(additional).mapToInt(List::size).sum());
        result.addAll(points);
        for (var add : additional) {
            result.addAll(add);
        }
        return result;
    }
}
