package com.yandex.mail.tools;

import android.database.Cursor;
import android.net.Uri;

import com.pushtorefresh.storio3.contentresolver.StorIOContentResolver;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.robolectric.shadows.ShadowApplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.robolectric.Shadows.shadowOf;

public class TestContext {

    private TestContext() { }

    @SuppressWarnings("NullableProblems") // init
    @NonNull
    public static ShadowApplication shapp;

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    public static StorIOContentResolver resolver;

    /**
     * Should be called before each test
     */
    public static void init() {
        /*
            Application is recreated for every test, so we need to reinitialize these
         */
        shapp = shadowOf(IntegrationTestRunner.app());

        resolver = IntegrationTestRunner.app().getStorIOContentResolver().get();
    }

    /**
     * Should be called after each test
     */
    public static void reset() {
        shapp = null;
        resolver = null;
    }

    /**
     * Should be called only after {@link #init()}
     */
    @Nullable
    public static Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder
    ) {
        return IntegrationTestRunner.app().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }
}
