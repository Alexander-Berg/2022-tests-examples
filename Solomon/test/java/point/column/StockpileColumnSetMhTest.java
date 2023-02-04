package ru.yandex.solomon.model.point.column;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class StockpileColumnSetMhTest {

    private boolean isInColumnSetUsingMh(StockpileColumn column, StockpileColumnSet columnSet) {
        try {
            return (boolean) StockpileColumnSetMh.isInColumnSetImpl(column).invokeExact(columnSet.columnSetMask());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Test
    public void isInColumnSet() {
        Assert.assertTrue(isInColumnSetUsingMh(
            StockpileColumn.STEP,
            StockpileColumnSet.fromColumnsVa(StockpileColumn.STEP, StockpileColumn.MERGE)));
        Assert.assertFalse(isInColumnSetUsingMh(
            StockpileColumn.COUNT,
            StockpileColumnSet.fromColumnsVa(StockpileColumn.STEP, StockpileColumn.MERGE)));
    }

}
