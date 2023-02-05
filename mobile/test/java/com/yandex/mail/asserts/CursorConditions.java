package com.yandex.mail.asserts;

import android.database.Cursor;

import org.assertj.core.api.Condition;

import androidx.annotation.NonNull;

public class CursorConditions {

    private CursorConditions() { }

    @NonNull
    public static Condition<Cursor> totalCount(int count) {
        return new Condition<Cursor>() {
            @Override
            public boolean matches(Cursor cursor) {
                return cursor.getCount() == count;
            }
        };
    }
}
