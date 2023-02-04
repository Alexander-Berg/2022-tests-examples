package ru.yandex.solomon.model.timeseries;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class GraphDataArrayListTest {

    @Test
    public void sorted() {
        long ts0 = Instant.parse("2016-01-10T12:13:14.002Z").toEpochMilli();
        Assert.assertTrue(new GraphDataArrayList().sorted());
        Assert.assertTrue(GraphDataArrayList.of(ts0, 10).sorted());
        Assert.assertTrue(GraphDataArrayList.of(ts0, 10, ts0 + 1000, 20).sorted());
        Assert.assertFalse(GraphDataArrayList.of(ts0, 10, ts0 - 1000, 20).sorted());
    }

    @Test
    public void mergeAdjacent() {
        long ts0 = Instant.parse("2016-03-15T12:13:14.002Z").toEpochMilli();
        GraphDataArrayList a = new GraphDataArrayList();
        a.add(ts0 + 1000, 10);
        a.add(ts0 + 2000, 20);
        a.add(ts0 + 2000, 30);
        a.add(ts0 + 3000, 40);
        a.add(ts0 + 3000, 50);
        a.add(ts0 + 3000, 60);
        a.add(ts0 + 4000, 70);
        a.add(ts0 + 4000, 80);
        a.mergeAdjacent();

        GraphDataArrayList expected = new GraphDataArrayList();
        expected.add(ts0 + 1000, 10);
        expected.add(ts0 + 2000, 30);
        expected.add(ts0 + 3000, 60);
        expected.add(ts0 + 4000, 80);

        Assert.assertEquals(expected, a);
    }

    @Test
    public void retainIf() {
        {
            GraphDataArrayList a = new GraphDataArrayList();
            a.retainIf((ts, v) -> true);

            Assert.assertEquals(new GraphDataArrayList(), a);
        }

        {
            long ts0 = Instant.parse("2016-03-15T12:51:53Z").toEpochMilli();
            GraphDataArrayList a = new GraphDataArrayList();
            a.add(ts0 + 1000, 10);
            a.add(ts0 + 2000, 20);
            a.add(ts0 + 3000, 30);
            a.add(ts0 + 4000, 40);
            a.add(ts0 + 5000, 50);
            a.add(ts0 + 6000, 60);

            a.retainIf((ts, v) -> ts == ts0 + 2000 || v >= 40);

            GraphDataArrayList e = new GraphDataArrayList();
            e.add(ts0 + 2000, 20);
            e.add(ts0 + 4000, 40);
            e.add(ts0 + 5000, 50);
            e.add(ts0 + 6000, 60);

            Assert.assertEquals(e, a);
        }
    }

    @Test
    public void toGraphData() {
        long ts0 = Instant.parse("2016-04-02T03:54:00Z").toEpochMilli();

        GraphDataArrayList l = new GraphDataArrayList();
        l.add(ts0, 30);
        l.add(ts0, 50);

        Assert.assertEquals(GraphData.graphData(ts0, 50), l.buildGraphData());
    }

}
