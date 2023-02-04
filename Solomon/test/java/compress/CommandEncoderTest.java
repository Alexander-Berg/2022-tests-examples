package ru.yandex.solomon.codec.compress;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.BitBufAllocator;
import ru.yandex.solomon.model.point.column.StockpileColumn;

/**
 * @author Stepan Koltsov
 */
public class CommandEncoderTest {

    @Test
    public void columnNumber() {
        for (StockpileColumn column : StockpileColumn.values()) {
            BitBuf os = BitBufAllocator.buffer(16);
            CommandEncoder.encodeColumn(os, column);

            StockpileColumn decoded = CommandEncoder.decodeColumn(os);
            Assert.assertEquals(0, os.readableBits());
            Assert.assertEquals(column, decoded);
            os.release();
        }
    }

}
