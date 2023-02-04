package ru.yandex.solomon.model.timeseries;

import java.time.Instant;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class AggrGraphDataArrayListMergeAdjacentTest {

    private void testMergeAdjacentIdentical(AggrGraphDataArrayList a) {
        AggrGraphDataArrayList expected = a.clone();
        a.mergeAdjacent();
        Assert.assertEquals(expected, a);
    }

    @Test
    public void empty() {
        testMergeAdjacentIdentical(new AggrGraphDataArrayList());
    }

    @Test
    public void one() {
        long ts0 = Instant.parse("2015-10-11T12:13:14Z").toEpochMilli();
        AggrGraphDataArrayList a = new AggrGraphDataArrayList();
        a.addRecordFullForTest(ts0, 10, false, 1);
        testMergeAdjacentIdentical(a);
    }

    @Test
    public void unique() {
        long ts0 = Instant.parse("2015-10-11T12:13:14Z").toEpochMilli();
        AggrGraphDataArrayList a = new AggrGraphDataArrayList();
        a.addRecordFullForTest(ts0, 10, false, 1);
        a.addRecordFullForTest(ts0 + 1000, 12, false, 2);
        testMergeAdjacentIdentical(a);
    }

    @Test
    public void overwrite() {
        long ts0 = Instant.parse("2015-10-11T12:13:14Z").toEpochMilli();
        AggrGraphDataArrayList a = new AggrGraphDataArrayList();
        a.addRecordFullForTest(ts0, 10, false, 1);
        a.addRecordFullForTest(ts0, 12, false, 2);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordFullForTest(ts0, 12, false, 2);

        a.mergeAdjacent();

        Assert.assertEquals(expected, a);
    }

    @Test
    public void lotsIntoTwo() {
        long ts0 = Instant.parse("2015-10-11T12:13:14Z").toEpochMilli();

        AggrGraphDataArrayList a = new AggrGraphDataArrayList();

        for (int i = 0; i < 3; ++i) {
            a.addRecordShort(ts0 - 1000, 17);
        }

        for (int i = 0; i < 3; ++i) {
            a.addRecordShort(ts0 + 1000, 19);
        }

        a.mergeAdjacent();

        Assert.assertEquals(GraphDataArrayList.of(ts0 - 1000, 17, ts0 + 1000, 19), AggrGraphDataLists.toGraphDataArrayList(a));
    }

    @Test
    public void sum() {
        long ts0 = Instant.parse("2015-10-11T12:13:14Z").toEpochMilli();
        AggrGraphDataArrayList a = new AggrGraphDataArrayList();
        a.addRecordFullForTest(ts0, 10, true, 1);
        a.addRecordFullForTest(ts0, 12, true, 2);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordFullForTest(ts0, 22, true, 3);

        a.mergeAdjacent();

        Assert.assertEquals(expected, a);
    }

    @Test
    public void random() {
        long ts0 = Instant.parse("2015-10-11T12:13:10Z").toEpochMilli();
        Random r = new Random(17);

        for (int i = 0; i < 100; ++i) {
            int length = r.nextInt(4);

            AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
            AggrGraphDataArrayList unmerged = new AggrGraphDataArrayList();

            for (int j = 0; j < length; ++j) {
                long ts = ts0 + j * 1000;

                int stride = 1 + r.nextInt(4);
                for (int k = 0; k < stride; ++k) {
                    double value = 10 + j + k;
                    unmerged.addRecordShort(ts, value);
                    if (k == stride - 1) {
                        expected.addRecordShort(ts, value);
                    }
                }
            }

            AggrGraphDataArrayList merged = unmerged.clone();
            merged.mergeAdjacent();

            Assert.assertEquals(unmerged.toString(), expected, merged);
        }
    }

}
