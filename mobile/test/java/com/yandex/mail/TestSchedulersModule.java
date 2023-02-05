package com.yandex.mail;

import com.yandex.mail.di.SchedulersModule;

import androidx.annotation.NonNull;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class TestSchedulersModule extends SchedulersModule {

    @Override
    @NonNull
    public Scheduler provideMainThreadScheduler() {
        return Schedulers.trampoline();
    }

    @Override
    @NonNull
    public Scheduler provideIoScheduler() {
        return Schedulers.trampoline();
    }
}
