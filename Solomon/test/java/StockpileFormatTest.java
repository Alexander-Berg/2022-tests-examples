package ru.yandex.solomon.codec;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.codec.serializer.StockpileFormat;

/**
 * @author Stepan Koltsov
 */
public class StockpileFormatTest {

    @Test
    public void byNumber() {
        Assert.assertSame(StockpileFormat.CURRENT, StockpileFormat.byNumber(StockpileFormat.CURRENT.getFormat()));
        try {
            StockpileFormat.byNumber(99999);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
        try {
            StockpileFormat.byNumber(2);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void byNumberOrCurrent() {
        Assert.assertSame(StockpileFormat.CURRENT, StockpileFormat.byNumberOrCurrent(StockpileFormat.CURRENT.getFormat()));
        Assert.assertSame(StockpileFormat.CURRENT, StockpileFormat.byNumberOrCurrent(99999));
        Assert.assertSame(StockpileFormat.MIN, StockpileFormat.byNumberOrCurrent(StockpileFormat.MIN.getFormat()));
        Assert.assertSame(StockpileFormat.MIN, StockpileFormat.byNumberOrCurrent(2));
    }
}
