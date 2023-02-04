package ru.yandex.solomon.model.timeseries;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class ConcatAggrGraphDataIteratorTest {

    private static AggrGraphDataArrayList concat(AggrGraphDataIterable... source) {
        var iterators = Stream.of(source)
            .map(AggrGraphDataIterable::iterator)
            .collect(Collectors.toList());

        var it = ConcatAggrGraphDataIterator.of(iterators);
        return AggrGraphDataArrayList.of(it);
    }

    @Test
    public void empty() {
        var expected = new AggrGraphDataArrayList();
        AggrGraphDataArrayList empty = concat(expected, expected);
        assertEquals(new AggrGraphDataArrayList(), empty);
    }

    @Test
    public void one() {
        var expected = AggrGraphDataArrayList.of(
            point("2019-02-01T12:10:34Z", 1),
            point("2019-02-01T12:10:35Z", 2),
            point("2019-02-01T12:10:33Z", 3));

        var result = concat(expected);
        assertEquals(expected, result);
    }

    @Test
    public void sameSize() {
        var source = AggrGraphDataArrayList.of(
            point("2019-02-01T12:10:34Z", 1),
            point("2019-02-01T12:10:35Z", 2),
            point("2019-02-01T12:10:33Z", 3));

        var expected = new AggrGraphDataArrayList();
        expected.addAll(source);
        expected.addAll(source);
        expected.addAll(source);

        var result = concat(source, source, source);
        assertEquals(expected, result);
    }

    @Test
    public void diffSize() {
        var one = AggrGraphDataArrayList.of(
            point("2019-02-01T12:10:34Z", 1),
            point("2019-02-01T12:10:35Z", 2),
            point("2019-02-01T12:10:33Z", 3));

        var two = AggrGraphDataArrayList.of(
            point("2019-02-01T12:10:40Z", 4),
            point("2019-02-01T12:10:41Z", 5),
            point("2019-02-01T12:10:42Z", 6));

        var empty = new AggrGraphDataArrayList();

        var expected = new AggrGraphDataArrayList();
        expected.addAll(one);
        expected.addAll(two);


        var result = concat(empty, one, empty, two, empty);
        assertEquals(expected, result);
    }
}
