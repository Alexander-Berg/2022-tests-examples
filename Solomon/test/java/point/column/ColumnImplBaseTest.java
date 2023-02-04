package ru.yandex.solomon.model.point.column;

import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class ColumnImplBaseTest {

    @Test
    public void instances() throws Exception {
        for (StockpileColumn column : StockpileColumn.values()) {
            checkStaticField(column, "DEFAULT_VALUE");
            checkStaticField(column, "mask");
            checkStaticField(column, "C");
        }
    }

    private void checkStaticField(StockpileColumn column, String fieldName) throws Exception {
        try {
            column.columnClass.getDeclaredField(fieldName);
        } catch (Exception e) {
            throw new Exception("class " + column.columnClass + " has no field " + fieldName);
        }
    }

}
