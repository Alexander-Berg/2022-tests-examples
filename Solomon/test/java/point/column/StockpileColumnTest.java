package ru.yandex.solomon.model.point.column;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class StockpileColumnTest {

    @Test
    public void byNumberOrNull() {
        for (StockpileColumn column : StockpileColumn.values()) {
            StockpileColumn found = StockpileColumn.byNumberOrNull(column.index());
            assertEquals(column.name(), column, found);
        }
    }

    @Test
    public void byNumberOrThrow() {
        for (StockpileColumn column : StockpileColumn.values()) {
            StockpileColumn found = StockpileColumn.byNumberOrThrow(column.index());
            assertEquals(column.name(), column, found);
        }
    }

    @Test
    public void maskUnique() {
        Stream.of(StockpileColumn.values())
                .collect(Collectors.groupingBy(StockpileColumn::mask))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> {
                    fail("Not unique mask " + entry.getKey() + " at columns: " + entry.getValue());
                });
    }

    @Test
    public void isInSetTrue() {
        int maxMask = Stream.of(StockpileColumn.values())
                .mapToInt(StockpileColumn::mask)
                .reduce(0, (left, right) -> left | right);

        for (StockpileColumn column : StockpileColumn.values()) {
            assertTrue(column.name(), column.isInSet(maxMask));
        }
    }

    @Test
    public void isInSetFalse() {
        for (StockpileColumn column : StockpileColumn.values()) {
            int maxMask = Stream.of(StockpileColumn.values())
                    .filter(c -> c != column)
                    .mapToInt(StockpileColumn::mask)
                    .reduce(0, (left, right) -> left | right);

            assertFalse(column.name(), column.isInSet(maxMask));
        }
    }
}
