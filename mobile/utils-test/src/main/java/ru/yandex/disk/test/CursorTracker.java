package ru.yandex.disk.test;

import android.database.Cursor;

public class CursorTracker extends ResourceTracker {

    public void add(final Cursor c) {
        add(new OpenInfo("cursor") {
            @Override
            protected boolean isClosed() {
                return c.isClosed();
            }
        });
    }

}
