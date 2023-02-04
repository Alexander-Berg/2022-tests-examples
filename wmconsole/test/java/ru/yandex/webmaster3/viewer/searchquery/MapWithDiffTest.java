package ru.yandex.webmaster3.viewer.searchquery;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.storage.searchquery.MapWithDiff;

/**
 * @author aherman
 */
public class MapWithDiffTest {
    private static final LocalDate LD_2016_01_10 = new LocalDate("2016-01-10");
    private static final LocalDate LD_2016_01_11 = new LocalDate("2016-01-11");
    private static final LocalDate LD_2016_01_12 = new LocalDate("2016-01-12");
    private static final LocalDate LD_2016_01_13 = new LocalDate("2016-01-13");
    private static final LocalDate LD_2016_01_14 = new LocalDate("2016-01-14");
    private static final LocalDate LD_2016_01_15 = new LocalDate("2016-01-15");

    @Test
    public void testWithoutGaps() throws Exception {
        List<Pair<Range<LocalDate>, Double>> data = Lists.newArrayList(
                Pair.of(Range.closed(LD_2016_01_10, LD_2016_01_10), 2.0),
                Pair.of(Range.closed(LD_2016_01_11, LD_2016_01_11), 0.0),
                Pair.of(Range.closed(LD_2016_01_12, LD_2016_01_12), 4.0),
                Pair.of(Range.closed(LD_2016_01_13, LD_2016_01_13), 0.0),
                Pair.of(Range.closed(LD_2016_01_14, LD_2016_01_14), 8.0),
                Pair.of(Range.closed(LD_2016_01_15, LD_2016_01_15), 8.0)
        );
        List<Double> values = new ArrayList<>();
        List<Double> diffs = new ArrayList<>();
        MapWithDiff.map(data.iterator(), (r, v, d) -> {
            values.add(v);
            diffs.add(d);
        });

        Assert.assertEquals(Lists.newArrayList(0.0, 4.0, 0.0, 8.0, 8.0), values);
        Assert.assertEquals(Lists.newArrayList(-2.0, +4.0, -4.0, +8.0, 0.0), diffs);
    }

    @Test
    public void testWithGaps() throws Exception {
        List<Pair<Range<LocalDate>, Double>> data = Lists.newArrayList(
                Pair.of(Range.closed(LD_2016_01_11, LD_2016_01_11), null),
                Pair.of(Range.closed(LD_2016_01_12, LD_2016_01_12), null),
                Pair.of(Range.closed(LD_2016_01_13, LD_2016_01_13), 2.0),
                Pair.of(Range.closed(LD_2016_01_14, LD_2016_01_14), 4.0),
                Pair.of(Range.closed(LD_2016_01_15, LD_2016_01_15), null)
        );

        List<Double> values = new ArrayList<>();
        List<Double> diffs = new ArrayList<>();
        MapWithDiff.map(data.iterator(), (r, v, d) -> {
            values.add(v);
            diffs.add(d);
        });

        Assert.assertEquals(Lists.newArrayList(null, 2.0, 4.0, null), values);
        Assert.assertEquals(Lists.newArrayList(null, null, 2.0, null), diffs);
    }
}
