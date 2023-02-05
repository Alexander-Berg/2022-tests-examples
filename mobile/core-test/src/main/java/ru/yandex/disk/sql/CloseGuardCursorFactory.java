package ru.yandex.disk.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;
import ru.yandex.disk.test.CursorTracker;

public class CloseGuardCursorFactory implements CursorFactory {
    private final CursorTracker cursorTracker = new CursorTracker();

    @SuppressWarnings("deprecation")
    @Override
    public Cursor newCursor(SQLiteDatabase db,
            SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
        SQLiteCursor c = new SQLiteCursor(db, masterQuery, editTable, query);
        cursorTracker.add(c);
        return c;
    }

    public void tearDown() {
        cursorTracker.checkState();
    }

}
