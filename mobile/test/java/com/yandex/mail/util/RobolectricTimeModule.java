package com.yandex.mail.util;

import android.os.SystemClock;

import com.yandex.mail.di.TimeModule;

import androidx.annotation.NonNull;

public class RobolectricTimeModule extends TimeModule {

    @NonNull
    @Override
    public TimeProvider provideTimeProvider() {
        return SystemClock::currentThreadTimeMillis;
    }
}
