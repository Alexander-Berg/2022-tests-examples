package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.point.predicate.AggrPointPredicate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class FilteringAggrGraphDataIteratorTest {

    @Test
    public void filterAlwaysTrue() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-25T10:17:00Z", 1),
                point("2018-07-25T10:17:15Z", 2),
                point("2018-07-25T10:17:30Z", 3));

        AggrGraphDataArrayList result = AggrGraphDataArrayList.of(
                FilteringAggrGraphDataIterator.of(source.iterator(), AggrPointPredicate.TRUE));

        assertThat(result, equalTo(source));
    }

    @Test
    public void filterPoints() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-25T10:17:00Z", 1),
                point("2018-07-25T10:17:15Z", 20),
                point("2018-07-25T10:17:30Z", 3),
                point("2018-07-25T10:17:45Z", 4),
                point("2018-07-25T10:17:50Z", 44));

        AggrGraphDataArrayList result = AggrGraphDataArrayList.of(
                FilteringAggrGraphDataIterator.of(source.iterator(), (columnSetMask, p) -> p.getValueDivided() > 10));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-25T10:17:15Z", 20),
                point("2018-07-25T10:17:50Z", 44));
        assertThat(result, equalTo(expected));
    }
}
