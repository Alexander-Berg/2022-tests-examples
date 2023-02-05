package ru.yandex.yandexmaps.app;

import android.annotation.SuppressLint;

import timber.log.Timber;

public class TestMapsApplication extends MapsApplication {

    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate() {
        appContext = this;
        Timber.plant(new Timber.DebugTree());
    }
}