package com.yandex.mail.network;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.fakeserver.MockWebServerHelper;
import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.proxy.BlockManager;
import com.yandex.mail.proxy.TimePreferences;
import com.yandex.mail.test.TestBlockManager;
import com.yandex.mail.test.TestNetworkBlockResolver;
import com.yandex.mail.test.TestTimeProvider;

import androidx.annotation.NonNull;
import io.reactivex.Scheduler;

public class TestNetworkCommonModule extends NetworkCommonModule {

    @NonNull
    private final MockWebServerHelper mockWebServerHelper;

    public TestNetworkCommonModule(@NonNull MockWebServerHelper mockWebServerHelper) {
        this.mockWebServerHelper = mockWebServerHelper;
    }

    @Override
    @NonNull
    public YandexMailHosts provideHosts(
            @NonNull BaseMailApplication context,
            @NonNull DeveloperSettingsModel developerSettingsModel
    ) {
        return new FakeHosts(mockWebServerHelper.getServer().url("/").toString());
    }

    @Override
    @NonNull
    public BlockManager provideBlockManager(
            @NonNull BaseMailApplication context,
            @NonNull TimePreferences.TimeProvider timeProvider,
            @NonNull BlockManager.NetworkBlockResolver networkBlockResolver,
            @NonNull DeveloperSettingsModel developerSettings,
            @NonNull Scheduler backgroundScheduler,
            @NonNull YandexMailMetrica metrica
    ) {
        final BlockManager original =
                super.provideBlockManager(context, timeProvider, networkBlockResolver, developerSettings, backgroundScheduler, metrica);
        return new TestBlockManager(original);
    }

    @Override
    @NonNull
    public BlockManager.NetworkBlockResolver provideNetworkBlockResolver() {
        return new TestNetworkBlockResolver(false);
    }

    @Override
    @NonNull
    public TimePreferences.TimeProvider provideTimeProvider() {
        return new TestTimeProvider();
    }
}
