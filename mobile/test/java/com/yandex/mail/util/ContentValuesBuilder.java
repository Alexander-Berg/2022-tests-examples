package com.yandex.mail.util;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ContentValuesBuilder {

    @NonNull
    private final ContentValues values;

    public ContentValuesBuilder() {
        values = new ContentValues();
    }

    @NonNull
    public ContentValuesBuilder put(@NonNull String key, long value) {
        values.put(key, value);
        return this;
    }

    @NonNull
    public ContentValuesBuilder put(@NonNull String key, int value) {
        values.put(key, value);
        return this;
    }

    @NonNull
    public ContentValuesBuilder put(@NonNull String key, @Nullable String value) {
        values.put(key, value);
        return this;
    }

    @NonNull
    public ContentValues get() {
        return values;
    }
}
