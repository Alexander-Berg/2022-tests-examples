package com.yandex.mail.asserts;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import org.assertj.core.api.AbstractIntegerAssert;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseAssertions {

    private DatabaseAssertions() {
    }

    public static void assertTableDoesNotExist(@NonNull SQLiteDatabase db, @NonNull String tableName) {
        try (final Cursor info = db.rawQuery(String.format("PRAGMA table_info(%s)", tableName), null)) {
            assertThat(info.getCount()).isEqualTo(0);
        }
    }

    @CheckResult
    @NonNull
    public static AbstractIntegerAssert<?> assertTableSize(@NonNull SQLiteDatabase db, @NonNull String tableName) {
        try (Cursor cursor = db.query(tableName, null, null, null, null, null, "ROWID ASC")) {
            return assertThat(cursor.getCount());
        }
    }

    public static void assertContainsSameData(@NonNull SQLiteDatabase db1, @NonNull SQLiteDatabase db2, @NonNull String tableName) {
        try (
                Cursor cursor1 = db1.query(tableName, null, null, null, null, null, "ROWID ASC");
                Cursor cursor2 = db2.query(tableName, null, null, null, null, null, "ROWID ASC")
        ) {
            assertCursorsSame(cursor1, cursor2);
        }
    }

    public static void assertCursorsSame(@NonNull Cursor a, @NonNull Cursor b) {
        assertThat(a.getCount()).isEqualTo(b.getCount());
        while (a.moveToNext() && b.moveToNext()) {
            assertThat(DatabaseUtils.dumpCurrentRowToString(a)).isEqualTo(DatabaseUtils.dumpCurrentRowToString(b));
        }
    }
}
