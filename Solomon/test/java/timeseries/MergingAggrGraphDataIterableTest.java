package ru.yandex.solomon.model.timeseries;

import java.util.List;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.LongValueColumn;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
public class MergingAggrGraphDataIterableTest {

    @Test
    public void emptyOne() {
        var empty = archive();
        var result = MergingAggrGraphDataIterable.of(List.of(empty));
        assertEquals(0, result.getRecordCount());
        assertEquals(empty, AggrGraphDataArrayList.of(result));
    }

    @Test
    public void emptyMore() {
        var empty = AggrGraphDataArrayList.empty();
        var result = MergingAggrGraphDataIterable.of(List.of(empty, empty, empty));
        assertEquals(0, result.getRecordCount());
        assertEquals(empty, AggrGraphDataArrayList.of(result));
    }

    @Test
    public void emptyAndNot() {
        int mask = TsColumn.mask | LongValueColumn.mask;
        var empty = archive(mask);
        var expected = archive(randomPoint(mask));
        var result = MergingAggrGraphDataIterable.of(List.of(empty, expected));
        assertEquals(1, result.getRecordCount());
        assertEquals(mask, result.columnSetMask());
        assertEquals(expected, AggrGraphDataArrayList.of(result));
    }

    @Test
    public void emptyInvalidMask() {
        int expectedMask = TsColumn.mask | LongValueColumn.mask;
        var empty = archive(TsColumn.mask | ValueColumn.mask);
        var expected = archive(randomPoint(expectedMask));
        var result = MergingAggrGraphDataIterable.of(List.of(empty, expected, empty));
        assertEquals(1, result.getRecordCount());
        assertEquals(expectedMask, result.columnSetMask());
        assertEquals(expected, AggrGraphDataArrayList.of(result));
    }

    private AggrGraphDataArrayList archive() {
        return AggrGraphDataArrayList.empty();
    }

    private AggrGraphDataArrayList archive(int mask) {
        return new AggrGraphDataArrayList(mask, 1);
    }

    private AggrGraphDataArrayList archive(AggrPoint... points) {
        return AggrGraphDataArrayList.of(points);
    }
}
