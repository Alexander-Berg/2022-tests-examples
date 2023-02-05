package com.yandex.mail.shadows;

import org.robolectric.annotation.Implements;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;

@Implements(AsyncDifferConfig.class)
public class ShadowAsyncDifferConfig {

    private static Executor backgroundThreadExecutor;

    public static void setBackgroundThreadExecutor(@NonNull Executor executor) {
        backgroundThreadExecutor = executor;
    }

    @NonNull
    public Executor getBackgroundThreadExecutor() {
        return backgroundThreadExecutor;
    }
}
