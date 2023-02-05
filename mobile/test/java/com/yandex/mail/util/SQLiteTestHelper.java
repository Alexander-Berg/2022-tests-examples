package com.yandex.mail.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.yandex.mail.provider.SQLUtils.DataType;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static com.yandex.mail.asserts.CursorConditions.totalCount;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;
import static org.assertj.core.api.Assertions.assertThat;

public final class SQLiteTestHelper {

    @NonNull
    public SQLiteDatabase database;

    public SQLiteTestHelper() {
        this(SQLiteDatabase.create(null));
    }

    public SQLiteTestHelper(@NonNull SQLiteDatabase database) {
        this.database = database;
    }

    @NonNull
    public SQLiteDatabase getDatabase() {
        return database;
    }

    public void addTable(@NonNull String table, @NonNull String primary, @NonNull DataType primaryType, @NonNull Object... namesAndTypes) {
        if (namesAndTypes.length % 2 == 1) {
            throw new IllegalArgumentException();
        }
        int colCount = namesAndTypes.length / 2;
        for (int i = 0; i < colCount; i++) {
            if (!(namesAndTypes[2 * i] instanceof String)) {
                throw new IllegalArgumentException();
            }
            if (!(namesAndTypes[2 * i + 1] instanceof DataType)) {
                throw new IllegalArgumentException();
            }
        }

        final List<String> columns = new ArrayList<>();
        columns.add(primary + " " + primaryType.toString() + " PRIMARY KEY");
        for (int i = 0; i < colCount; i++) {
            String name = (String) namesAndTypes[2 * i];
            DataType type = (DataType) namesAndTypes[2 * i + 1];
            columns.add(name + " " + type.toString());
        }
        database.execSQL(format("CREATE TABLE %s (%s)", table, join(columns, ",")));
    }

    @NonNull
    public TableHandle getTable(@NonNull String tableName) {
        return new TableHandle(tableName);
    }

    public class TableHandle {

        @NonNull
        private final String tableName;

        public TableHandle(@NonNull String tableName) {
            this.tableName = tableName;
        }

        @NonNull
        public TableHandle addRow(@NonNull ContentValues values) {
            database.insert(tableName, null, values);
            return this;
        }

        public void assertHasRow(@NonNull String where, @NonNull String... whereArgs) {
            try (Cursor cursor = database.rawQuery("SELECT * FROM " + tableName + " WHERE " + where, whereArgs)) {
                assertThat(cursor).has(totalCount(1));
            }
        }

        public void assertColumnIsNull(@NonNull String column) {
            try (Cursor cursor = database.rawQuery("SELECT * FROM " + tableName + " WHERE " + column + " IS NOT NULL", null)) {
                assertThat(cursor).has(totalCount(0));
            }
        }

        public int getRowsCount() {
            try (Cursor cursor = database.rawQuery("SELECT * FROM " + tableName, null)) {
                return cursor.getCount();
            }
        }
    }

}
