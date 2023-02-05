package com.yandex.mail;

import com.yandex.mail.developer_settings.DeveloperSettingsModule;
import com.yandex.mail.developer_settings.FlipperProxy;
import com.yandex.mail.developer_settings.LeakCanaryProxy;
import com.yandex.mail.developer_settings.TinyDancerProxy;
import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.storage.preferences.DeveloperSettings;
import com.yandex.xplat.common.StethoProxy;

import androidx.annotation.NonNull;
import dagger.Lazy;
import okhttp3.OkHttpClient;

class TestDeveloperSettingsModule extends DeveloperSettingsModule {

    @NonNull
    @Override
    public StethoProxy provideStethoProxy(@NonNull BaseMailApplication application) {
        return new StethoProxy() {
            @Override
            public void init() {
                // no-op
            }

            @Override
            public void patch(@NonNull OkHttpClient.Builder clientBuilder) {
                // no-op
            }
        };
    }

    @NonNull
    @Override
    public FlipperProxy provideFlipperProxy(@NonNull BaseMailApplication application) {
        return new FlipperProxy() {
            @Override
            public void init() {
                // no-op
            }

            @Override
            public void patch(@NonNull OkHttpClient.Builder clientBuilder) {
                // no-op
            }
        };
    }

    @NonNull
    @Override
    public DeveloperSettingsModel provideDeveloperSettingsModel(
            @NonNull BaseMailApplication app,
            @NonNull DeveloperSettings developerSettings,
            @NonNull Lazy<StethoProxy> stethoProxy,
            @NonNull Lazy<FlipperProxy> flipperProxy,
            @NonNull Lazy<TinyDancerProxy> tinyDancerProxy,
            @NonNull Lazy<LeakCanaryProxy> leakCanaryProxy
    ) {
        return new DeveloperSettingsModel(app, developerSettings, stethoProxy, flipperProxy, tinyDancerProxy, leakCanaryProxy) {
            @Override
            public void applyDeveloperSettings() {
                leakCanaryProxy.get().disable();
            }
        };
    }
}
