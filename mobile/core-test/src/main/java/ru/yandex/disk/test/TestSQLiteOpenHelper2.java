package ru.yandex.disk.test;

import android.content.Context;

import ru.yandex.disk.sql.SQLiteOpenHelper2;

public class TestSQLiteOpenHelper2 extends SQLiteOpenHelper2 {

    public TestSQLiteOpenHelper2(final Context context, final String name, final int version) {
        super(context, name, version);
    }

    @Override
    public void close() {
        checkTransactionsState();
        super.close();
    }

    private void checkTransactionsState() {
        if (getReadableDatabase().inTransaction()) {
            throw new IllegalStateException("open transaction");
        }
    }
}
