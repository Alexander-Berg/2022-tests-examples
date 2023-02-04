package ru.yandex.solomon.model.point.column;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class StockpileColumnSetTest {

    @Test
    public void hasColumn() {
        StockpileColumnSet cs = StockpileColumnSet.fromColumnsVa(StockpileColumn.TS, StockpileColumn.VALUE);
        Assert.assertTrue(cs.hasColumn(StockpileColumn.TS));
        Assert.assertTrue(cs.hasColumn(StockpileColumn.VALUE));
        Assert.assertFalse(cs.hasColumn(StockpileColumn.COUNT));
        Assert.assertFalse(cs.hasColumn(StockpileColumn.MERGE));
    }

    @Test
    public void testToString() {
        Assert.assertEquals("{}", StockpileColumnSet.empty.toString());
        Assert.assertEquals("{TS}", StockpileColumnSet.fromColumnsVa(StockpileColumn.TS).toString());
        Assert.assertEquals("{TS, COUNT}",
            StockpileColumnSet.fromColumnsVa(StockpileColumn.TS, StockpileColumn.COUNT).toString());
    }

    @Test
    public void maxMask() {
        for (StockpileColumn column : StockpileColumn.values()) {
            Assert.assertTrue(column.mask() < StockpileColumnSet.maxMask);
        }
    }

    @Test
    public void needExpansion() {
        Assert.assertTrue(StockpileColumnSet.needToAddAtLeastOneColumn(
            StockpileColumnSet.empty, StockpileColumnSet.fromColumnsVa(StockpileColumn.TS)));

        for (int mask = 0; mask <= StockpileColumnSet.maxMask; ++mask) {
            Assert.assertFalse(StockpileColumnSet.needToAddAtLeastOneColumn(mask, mask));
            if (mask != 0) {
                Assert.assertTrue(StockpileColumnSet.needToAddAtLeastOneColumn(0, mask));
                Assert.assertFalse(StockpileColumnSet.needToAddAtLeastOneColumn(mask, 0));
            }
        }

        Assert.assertTrue(StockpileColumnSet.needToAddAtLeastOneColumn(
            StockpileColumnSet.fromColumnsVa(StockpileColumn.COUNT),
            StockpileColumnSet.fromColumnsVa(StockpileColumn.VALUE)));
        Assert.assertTrue(StockpileColumnSet.needToAddAtLeastOneColumn(
            StockpileColumnSet.fromColumnsVa(StockpileColumn.TS, StockpileColumn.COUNT),
            StockpileColumnSet.fromColumnsVa(StockpileColumn.TS, StockpileColumn.VALUE)));
    }
}
