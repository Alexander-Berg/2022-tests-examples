package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointData;
import ru.yandex.solomon.model.point.column.StockpileColumn;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Vladimir Gordiychuk
 */
public class AggrGraphDataArrayListLongValueTest {
    private static AggrPoint point(String time, long value) {
        return AggrPoint.builder()
                .time(time)
                .longValue(value)
                .build();
    }

    private static AggrGraphDataArrayList listOf(AggrPoint... points) {
        return AggrGraphDataArrayList.of(points);
    }

    @Test
    public void setAndGet() {
        AggrPoint expectedPoint = randomPoint(StockpileColumn.TS.mask() | StockpileColumn.LONG_VALUE.mask());

        AggrGraphDataArrayList list = listOf(expectedPoint);
        AggrPoint result = list.getAnyPoint(0);

        assertEquals(expectedPoint, result);
        assertEquals(expectedPoint.tsMillis, list.getTsMillis(0));
    }

    @Test
    public void listEqualWhenAllEqual() {
        AggrGraphDataArrayList first = listOf(
                point("2017-05-10T09:00:00Z", 10L),
                point("2017-05-10T10:00:00Z", 42L)
        );

        AggrGraphDataArrayList second = listOf(
                point("2017-05-10T09:00:00Z", 10L),
                point("2017-05-10T10:00:00Z", 42L)
        );

        assertEquals(first, second);
    }

    @Test
    public void listDiffNotEqual() {
        AggrGraphDataArrayList first = listOf(
                point("2017-05-10T09:00:00Z", 2L),
                point("2017-05-10T10:00:00Z", 9L)
        );

        AggrGraphDataArrayList second = listOf(
                point("2017-05-10T09:00:00Z", 34L),
                point("2017-05-10T10:00:00Z", 93L)
        );

        assertNotEquals(first, second);
    }

    @Test
    public void mask() {
        AggrGraphDataArrayList list = listOf(point("2017-05-10T09:00:00Z", 4L));

        int expectMask = StockpileColumn.LONG_VALUE.mask() | StockpileColumn.TS.mask();
        assertEquals(expectMask, list.columnSetMask());
    }

    @Test
    public void sortAndMergeSkipMerge() {
        AggrGraphDataArrayList source = listOf(
                point("2017-05-10T10:00:00Z", 1),
                point("2017-05-10T03:10:00Z", 3),
                point("2017-05-10T13:00:00Z", 5)
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = listOf(
                point("2017-05-10T03:10:00Z", 3),
                point("2017-05-10T10:00:00Z", 1),
                point("2017-05-10T13:00:00Z", 5)
        );

        assertEquals(expected, source);
    }


    @Test
    public void sortAndMerge() {
        AggrGraphDataArrayList source = listOf(
                point("2017-05-10T03:10:00Z", 1),
                point("2017-05-10T10:10:00Z", 3),
                point("2017-05-10T03:10:00Z", 5),
                point("2017-05-10T03:10:00Z", 7)
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = listOf(
                point("2017-05-10T03:10:00Z", 7), // latest win
                point("2017-05-10T10:10:00Z", 3));

        assertEquals(expected, source);
    }

    @Test
    public void toView() {
        AggrGraphDataArrayList source = listOf(
                point("2017-05-10T01:00:00Z", 2),
                point("2017-05-10T02:00:00Z", 5),
                point("2017-05-10T03:10:00Z", 8)
        );

        AggrGraphDataArrayListView view = source.view();
        assertThat(view.columnSetMask(), equalTo(source.columnSetMask()));
        for (int index = 0; index < source.length(); index++) {
            AggrPointData sPoint = new AggrPointData();
            source.getDataTo(index, sPoint);

            AggrPointData vPoint = new AggrPointData();
            view.getDataTo(index, vPoint);

            assertEquals(sPoint, vPoint);
        }
    }

    @Test
    public void toViewAndBack() {
        AggrGraphDataArrayList source = listOf(
                point("2017-05-10T01:00:00Z", 1),
                point("2017-05-10T02:00:00Z", 4),
                point("2017-05-10T03:10:00Z", 7)
        );

        AggrGraphDataArrayListView view = source.view();

        AggrGraphDataArrayList result = new AggrGraphDataArrayList(view.columnSetMask(), view.length());
        result.addAllFrom(view.iterator());
        assertThat(result, equalTo(source));
    }
}
