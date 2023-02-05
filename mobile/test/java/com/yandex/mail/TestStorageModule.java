package com.yandex.mail;

import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.settings.SimpleStorage;
import com.yandex.mail.settings.SimpleStorageImpl;
import com.yandex.mail.storage.StorageModule;
import com.yandex.mail.storage.StubSharedPreferences;

import androidx.annotation.NonNull;

class TestStorageModule extends StorageModule {

    @Override
    @NonNull
    public SimpleStorage provideSimpleStorage(@NonNull BaseMailApplication application, @NonNull YandexMailMetrica metrica) {
        return new SimpleStorageImpl(new StubSharedPreferences(), metrica);
    }
}
