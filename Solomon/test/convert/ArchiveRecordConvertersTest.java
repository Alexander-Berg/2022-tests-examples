package ru.yandex.stockpile.ser.test.convert;

import org.junit.Test;

import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.point.column.TsColumn;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.stockpile.ser.test.convert.ArchiveRecordConverters.fromProto;
import static ru.yandex.stockpile.ser.test.convert.ArchiveRecordConverters.toProto;

/**
 * @author Vladimir Gordiychuk
 */
public class ArchiveRecordConvertersTest {
    @Test
    public void fullTwoWayConvert() {
        AggrPoint source = randomPoint(StockpileColumnSet.maxMask);
        StockpileCanonicalProto.ArchiveRecord proto = toProto(source);
        AggrPoint restore = fromProto(proto);
        restore.columnSet = source.columnSet;
        assertEquals(source, restore);
    }

    @Test
    public void byColumnTwoWayConvert() {
        for (StockpileColumn column : StockpileColumn.values()) {
            if (column == StockpileColumn.TS) {
                continue;
            }

            AggrPoint source = randomPoint(TsColumn.mask | column.mask());
            StockpileCanonicalProto.ArchiveRecord proto = toProto(source);
            AggrPoint restore = fromProto(proto);
            restore.columnSet = source.columnSet;
            assertEquals(column.name(), source, restore);
        }
    }
}
