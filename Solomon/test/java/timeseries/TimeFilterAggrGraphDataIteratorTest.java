package ru.yandex.solomon.model.timeseries;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList.of;
import static ru.yandex.solomon.model.timeseries.TimeFilterAggrGraphDataIterator.slice;
import static ru.yandex.solomon.model.timeseries.TimeFilterAggrGraphDataIterator.sliceFrom;
import static ru.yandex.solomon.model.timeseries.TimeFilterAggrGraphDataIterator.sliceTo;

/**
 * @author Vladimir Gordiychuk
 */
public class TimeFilterAggrGraphDataIteratorTest {

    @Test
    public void empty() {
        AggrGraphDataArrayList empty = AggrGraphDataArrayList.empty();
        assertEquals(empty, of(sliceFrom(empty.iterator(), System.currentTimeMillis())));
        assertEquals(empty, of(sliceTo(empty.iterator(), System.currentTimeMillis())));
        assertEquals(empty, of(slice(empty.iterator(), System.currentTimeMillis(), System.currentTimeMillis() + 15_000)));
    }

    @Test
    public void sliceFromIncluded() {
        var source = randomData(100);

        for (int index : List.of(0, 25, 50, 99)) {
            var expected = AggrGraphDataArrayList.of(source.slice(index, 100));
            var actual = of(sliceFrom(source.iterator(), source.getTsMillis(index)));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void sliceFromNear() {
        var source = randomData(100);

        for (int index : List.of(0, 25, 50, 99)) {
            var expected = AggrGraphDataArrayList.of(source.slice(index, 100));
            var actual = of(sliceFrom(source.iterator(), source.getTsMillis(index) - 1_000));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void sliceToIncluded() {
        var source = randomData(100);

        for (int index : List.of(0, 25, 50, 99)) {
            var expected = AggrGraphDataArrayList.of(source.slice(0, index + 1));
            var actual = of(sliceTo(source.iterator(), source.getTsMillis(index)));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void sliceToNear() {
        var source = randomData(100);

        for (int index : List.of(0, 25, 50, 99)) {
            var expected = AggrGraphDataArrayList.of(source.slice(0, index + 1));
            var actual = of(sliceTo(source.iterator(), source.getTsMillis(index) + 1_000));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void sliceFromTo() {
        var source = randomData(100);

        for (int from : List.of(0, 25, 50, 99)) {
            for (int to : List.of(0, 25, 50, 99)) {
                if (from > to) {
                    continue;
                }

                var expected = AggrGraphDataArrayList.of(source.slice(from, to + 1));
                var actual = of(slice(source.iterator(), source.getTsMillis(from), source.getTsMillis(to)));
                assertEquals(expected, actual);
            }
        }
    }

    private AggrGraphDataArrayList randomData(int size) {
        int mask = TsColumn.mask | ValueColumn.mask;
        var point = RecyclableAggrPoint.newInstance();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        AggrGraphDataArrayList result = new AggrGraphDataArrayList(mask, size);
        long ts0 = System.currentTimeMillis();
        for (int index = 0; index < size; index++) {
            randomPoint(point, mask, random);
            point.tsMillis = ts0 + index * 10_000;
            result.addRecord(point);
        }
        point.recycle();
        return result;
    }
}
