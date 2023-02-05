package ru.yandex.market.db;

import org.junit.Test;

import ru.yandex.market.base.database.DatabaseColumn;
import ru.yandex.market.base.database.DatabaseHelper;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.base.database.ColumnType.INTEGER;
import static ru.yandex.market.base.database.ColumnType.TEXT;

public class DatabaseHelperTest {

    private static final String TABLE_NAME = "tableName";

    private static final String COLUMN_1 = "column1";
    private static final String COLUMN_2 = "column2";

    @Test
    public void createTable() {
        final DatabaseColumn[] columns = new DatabaseColumn[]{
                DatabaseColumn.builder(COLUMN_1, TEXT).build(),
                DatabaseColumn.builder(COLUMN_2, INTEGER).build()
        };
        String result = DatabaseHelper.createTable(TABLE_NAME, columns);
        assertEquals(result,
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_1 + " TEXT, " +
                        COLUMN_2 + " INTEGER);");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableIfColumnsEmpty() {
        DatabaseHelper.createTable(TABLE_NAME, new DatabaseColumn[]{});
    }

    @Test
    public void testToStringOnlyNameAndType() {
        String result = DatabaseHelper.toString(DatabaseColumn.builder(COLUMN_1, TEXT).build());
        assertEquals(result, COLUMN_1 + " TEXT");
    }

    @Test
    public void testToStringWithPrimaryKey() {
        String result = DatabaseHelper.toString(DatabaseColumn.builder(COLUMN_1, TEXT).primaryKey().build());
        assertEquals(result, COLUMN_1 + " TEXT PRIMARY KEY");
    }

    @Test
    public void testToStringWithAutoIncrement() {
        String result = DatabaseHelper.toString(DatabaseColumn.builder(COLUMN_1, TEXT).autoIncrement().build());
        assertEquals(result, COLUMN_1 + " TEXT AUTOINCREMENT");
    }

    @Test
    public void testToStringWithNotNull() {
        String result = DatabaseHelper.toString(DatabaseColumn.builder(COLUMN_1, TEXT).notNull().build());
        assertEquals(result, COLUMN_1 + " TEXT NOT NULL");
    }

    @Test
    public void testToStringWithDefaultValue() {
        String result = DatabaseHelper.toString(DatabaseColumn.builder(COLUMN_1, TEXT).defaultValue("20")
                .build());
        assertEquals(result, COLUMN_1 + " TEXT DEFAULT 20");
    }
}